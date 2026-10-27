package update

import APP_VERSION
import DesktopPlatform
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import loadData
import mvvm.GithubRelease
import mvvm.UpdateViewModel
import saveData
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.util.Properties
import java.util.concurrent.TimeUnit

enum class UpdateStage { IDLE, AVAILABLE, DOWNLOADING, CHECKING, READY, PREPARING }

/** Where the updater keeps its files. ~/TheGuide/update holds only the log, the installer and markers; user data is never touched. */
object UpdatePaths {
    private val home = System.getProperty("user.home")
    val dir: File get() = File(home, "TheGuide/update").apply { mkdirs() }
    val log get() = File(dir, "update.log")
    val job get() = File(dir, "job.properties")
    val updaterPid get() = File(dir, "updater.pid")
    fun marker(version: String) = File(dir, "launched-$version")
    val backupRoot: File get() = when (DesktopPlatform.Current) {
        DesktopPlatform.Windows -> File(System.getenv("LOCALAPPDATA") ?: "$home/AppData/Local", "TheGuide/backup")
        else -> File(home, "Library/Application Support/TheGuide/backup")
    }
}

object UpdateLog {
    @Synchronized
    fun log(line: String) {
        println("[update] $line")
        runCatching { UpdatePaths.log.appendText("${LocalDateTime.now().withNano(0)}  $line\n") }
    }
}

internal val currentVersion get() = APP_VERSION.removePrefix("v")
internal fun String.plainVersion() = removePrefix("v")

/** Runs a command and returns its exit code, logging the call and output. */
internal fun run(vararg cmd: String, timeoutMin: Long = 15): Int {
    UpdateLog.log("$ ${cmd.joinToString(" ")}")
    return runCatching {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        if (!p.waitFor(timeoutMin, TimeUnit.MINUTES)) { p.destroyForcibly(); UpdateLog.log("timed out"); return -1 }
        if (out.isNotBlank()) UpdateLog.log(out.trim().takeLast(2000))
        UpdateLog.log("exit ${p.exitValue()}")
        p.exitValue()
    }.getOrElse { UpdateLog.log("failed to start: ${it.message}"); -1 }
}

/** True when a file can be created in [dir] without asking for permission. */
internal fun canWrite(dir: File): Boolean = runCatching { File.createTempFile("guide", ".tmp", dir).delete() }.getOrDefault(false)

/** Copies a whole install (or backup) from [from] to [to], replacing what is there. */
internal fun copyTree(from: File, to: File, elevated: Boolean = false): Boolean = when (DesktopPlatform.Current) {
    DesktopPlatform.Windows -> {
        // robocopy exit codes below 8 mean success
        val args = "\"${from.path}\" \"${to.path}\" /MIR /R:2 /W:1 /NFL /NDL /NP"
        (if (elevated) runElevatedWin("robocopy", args) else run("robocopy", from.path, to.path, "/MIR", "/R:2", "/W:1", "/NFL", "/NDL", "/NP")) in 0..7
    }
    else -> {
        val script = "rm -rf ${shq(to.path)} && /usr/bin/ditto ${shq(from.path)} ${shq(to.path)}"
        (if (elevated) runAsAdminMac(script) else run("/bin/sh", "-c", script)) == 0
    }
}

internal fun shq(s: String) = "'" + s.replace("'", "'\\''") + "'"

/** Starts [exe] through UAC and waits; 1223 means the person declined the permission prompt. */
internal fun runElevatedWin(exe: String, args: String): Int {
    fun psq(s: String) = "'" + s.replace("'", "''") + "'"
    val ps = "try { \$p = Start-Process -FilePath ${psq(exe)} -ArgumentList ${psq(args)} -Verb RunAs -Wait -PassThru -WindowStyle Hidden; exit \$p.ExitCode } catch { exit 1223 }"
    return run("powershell", "-NoProfile", "-NonInteractive", "-Command", ps)
}

/** Runs a shell script with the macOS password prompt; -128 / 1 from osascript means it was cancelled. */
internal fun runAsAdminMac(script: String): Int =
    run("/usr/bin/osascript", "-e", "do shell script \"${script.replace("\\", "\\\\").replace("\"", "\\\"")}\" with administrator privileges")

/** The installed app this process is running from: the install folder on Windows, the .app bundle on macOS. Null when run from an IDE / plain java. */
internal fun runningInstall(): Pair<File, String>? {
    val cmd = ProcessHandle.current().info().command().orElse(null)?.let(::File) ?: return null
    val loc = when (DesktopPlatform.Current) {
        DesktopPlatform.Windows -> cmd.takeUnless { it.name.lowercase().startsWith("java") }?.parentFile
        DesktopPlatform.MacOS -> generateSequence(cmd) { it.parentFile }.firstOrNull { it.name.endsWith(".app") }
        else -> null
    } ?: return null
    return loc to cmd.relativeTo(loc).path
}

/** What the updater process is asked to do, written next to the log so it survives the app quitting. */
data class UpdateJob(
    val pid: Long, val installer: File, val install: File, val backup: File, val launcher: String,
    val oldVersion: String, val newVersion: String, val relaunch: Boolean, var result: String = "pending",
) {
    fun save() = Properties().apply {
        setProperty("pid", "$pid"); setProperty("installer", installer.path); setProperty("install", install.path); setProperty("backup", backup.path)
        setProperty("launcher", launcher); setProperty("old", oldVersion); setProperty("new", newVersion); setProperty("relaunch", "$relaunch"); setProperty("result", result)
    }.let { p -> UpdatePaths.job.outputStream().use { p.store(it, "The Guide update") } }

    companion object {
        fun load(f: File = UpdatePaths.job): UpdateJob? = runCatching {
            val p = Properties().apply { f.inputStream().use { load(it) } }
            UpdateJob(
                p.getProperty("pid").toLong(), File(p.getProperty("installer")), File(p.getProperty("install")), File(p.getProperty("backup")), p.getProperty("launcher"),
                p.getProperty("old"), p.getProperty("new"), p.getProperty("relaunch").toBoolean(), p.getProperty("result") ?: "pending"
            )
        }.getOrNull()
    }
}

/** Update state shared by every UpdateViewModel and the dialog, pill and cards. */
object Updates {
    var release by mutableStateOf<GithubRelease?>(null)
    var stage by mutableStateOf(UpdateStage.IDLE)
    var read by mutableStateOf(0L)
    var total by mutableStateOf(0L)
    var installOnQuit by mutableStateOf(false)
    var justUpdated by mutableStateOf<String?>(null)
    var toast by mutableStateOf<String?>(null)
    /** Notes of the version just installed, for "What's new" on the done card. */
    var notes by mutableStateOf<GithubRelease?>(null)
    /** Set by main(): closes the window the normal way (player released, state saved). */
    var quitApp: () -> Unit = { kotlin.system.exitProcess(0) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var download: Job? = null
    @Volatile private var cancelled = false
    private var shownThisRun = false

    val asset: GithubRelease.Asset? get() = release?.let(::pickAsset)
    val newVersion get() = release?.tag_name?.plainVersion() ?: ""
    val installerFile get() = asset?.let { File(UpdatePaths.dir, it.name) }

    fun say(text: String) { toast = text }

    /** The installer for this computer: .msi (or .exe) on Windows, the .dmg for this chip on macOS. */
    fun pickAsset(r: GithubRelease): GithubRelease.Asset? {
        val list = r.assets.orEmpty()
        return when (DesktopPlatform.Current) {
            DesktopPlatform.Windows -> list.firstOrNull { it.name.endsWith(".msi", true) } ?: list.firstOrNull { it.name.endsWith(".exe", true) }
            DesktopPlatform.MacOS -> {
                val dmgs = list.filter { it.name.endsWith(".dmg", true) }
                val arm = System.getProperty("os.arch").contains("aarch64") || System.getProperty("os.arch").contains("arm")
                val armWords = listOf("arm64", "aarch64", "apple")
                val intelWords = listOf("x64", "x86_64", "intel", "amd64")
                dmgs.firstOrNull { d -> (if (arm) armWords else intelWords).any { d.name.contains(it, true) } }
                    ?: dmgs.firstOrNull { d -> (if (arm) intelWords else armWords).none { d.name.contains(it, true) } } ?: dmgs.firstOrNull()
            }
            else -> null
        }
    }

    fun skipped() = loadData<String>("update_skipped")
    fun skip() { saveData("update_skipped", release?.tag_name); say("Skipped $newVersion. You’ll hear about the next version.") }
    fun remindTomorrow(quiet: Boolean = false) { saveData("update_remind_after", Instant.now().plusSeconds(86_400)); if (!quiet) say("OK, we’ll remind you tomorrow.") }

    /** Whether to open the dialog by itself this run. */
    fun shouldAutoShow(): Boolean {
        val r = release ?: return false
        if (shownThisRun || stage != UpdateStage.AVAILABLE || r.tag_name == skipped()) return false
        val after = loadData<Instant>("update_remind_after")
        if (after != null && Instant.now().isBefore(after)) return false
        shownThisRun = true
        return true
    }

    fun offer(r: GithubRelease) {
        release = r
        if (stage == UpdateStage.IDLE) stage = UpdateStage.AVAILABLE
    }

    fun startDownload(vm: UpdateViewModel) {
        val a = asset ?: run { say("There’s no installer for this computer in that release."); return }
        val target = installerFile ?: return
        cancelled = false
        stage = UpdateStage.DOWNLOADING
        read = 0; total = a.size
        download = scope.launch {
            UpdateLog.log("Downloading ${a.name} (${a.size} bytes) for $newVersion")
            // an installer kept from an earlier try is reused when it still checks out
            val ok = (target.isFile && verify(target, a)) || (vm.downloadAsset(a, target) { r, t ->
                if (cancelled) throw java.io.InterruptedIOException("cancelled")
                read = r; if (t > 0) total = t
            } && run { stage = UpdateStage.CHECKING; verify(target, a) })
            withContext(Dispatchers.Main) {
                when {
                    cancelled -> stage = UpdateStage.AVAILABLE
                    ok -> { stage = UpdateStage.READY; UpdateLog.log("Download ready: ${target.path}") }
                    else -> { stage = UpdateStage.AVAILABLE; target.delete(); say("The download didn’t complete. Try again in a moment.") }
                }
            }
        }
    }

    fun cancelDownload() { cancelled = true; download?.cancel(); stage = UpdateStage.AVAILABLE; say("Download cancelled.") }

    /** Checks the file against the sha256 GitHub publishes for the asset (and its size when there is no digest). */
    fun verify(f: File, a: GithubRelease.Asset): Boolean {
        val want = a.digest?.substringAfter("sha256:", "")?.lowercase()
        if (want.isNullOrBlank()) {
            UpdateLog.log("No sha256 digest published; checking size only")
            return f.length() == a.size
        }
        val md = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input -> val buf = ByteArray(1 shl 16); while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) } }
        val got = md.digest().joinToString("") { "%02x".format(it) }
        UpdateLog.log("sha256 ${if (got == want) "matches" else "MISMATCH got $got want $want"}")
        return got == want
    }

    /**
     * Backs up the running install, then starts the backup copy as the updater and quits.
     * The updater can then replace the install because nothing runs from it any more.
     */
    fun restartAndInstall(relaunch: Boolean = true) {
        val installer = installerFile?.takeIf { it.isFile } ?: return
        val (install, launcher) = runningInstall() ?: run { say("Updates install from the installed app, not from a development build."); return }
        stage = UpdateStage.PREPARING
        scope.launch {
            val job = prepare(installer, install, launcher, relaunch)
            withContext(Dispatchers.Main) {
                if (job == null) { stage = UpdateStage.READY; say("Couldn’t back up the current version, so nothing was changed.") }
                else quitApp()
            }
        }
    }

    /** Runs on quit when "Install when I quit" was chosen. Blocks briefly for the backup. */
    fun installOnQuitIfAsked() {
        if (!installOnQuit || stage != UpdateStage.READY) return
        val installer = installerFile?.takeIf { it.isFile } ?: return
        val (install, launcher) = runningInstall() ?: return
        prepare(installer, install, launcher, relaunch = false)
    }

    private fun prepare(installer: File, install: File, launcher: String, relaunch: Boolean): UpdateJob? {
        UpdateLog.log("---- Update $currentVersion -> $newVersion (${DesktopPlatform.Current}) ----")
        val backup = File(UpdatePaths.backupRoot, currentVersion).resolve(install.name)
        backup.parentFile.mkdirs()
        UpdateLog.log("Backing up ${install.path} to ${backup.path}")
        if (!copyTree(install, backup) || !File(backup, launcher).isFile) { UpdateLog.log("Backup failed"); return null }
        UpdatePaths.dir.listFiles { f -> f.name.startsWith("launched-") }?.forEach { it.delete() }
        val job = UpdateJob(ProcessHandle.current().pid(), installer, install, backup, launcher, currentVersion, newVersion, relaunch).apply { save() }
        return runCatching {
            ProcessBuilder(File(backup, launcher).path, "--updater", UpdatePaths.job.path)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start()
            UpdateLog.log("Updater started from the backup")
            job
        }.getOrElse { UpdateLog.log("Couldn't start the updater: ${it.message}"); null }
    }

    /**
     * Called by a normal start. Writes the "it opened" marker the updater waits for; on the new version it then removes the
     * backup and installer once the updater has closed, and shows the "You're on X" card.
     */
    fun onStartup() {
        runCatching { UpdatePaths.marker(currentVersion).writeText("${Instant.now()}") }
        val job = UpdateJob.load() ?: return
        scope.launch {
            // the updater may still be finishing; never clean up underneath it
            runCatching { UpdatePaths.updaterPid.readText().trim().toLong() }.getOrNull()
                ?.let { ProcessHandle.of(it).orElse(null) }?.onExit()?.get(2, TimeUnit.MINUTES)
            if (currentVersion != job.oldVersion) {
                UpdateLog.log("$currentVersion opened; removing backup ${job.backup.path}")
                job.backup.parentFile.deleteRecursively()
                job.installer.delete()
                UpdatePaths.job.delete()
                notes = loadData<String>("update_release")?.let { runCatching { kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<GithubRelease>(it) }.getOrNull() }
                    ?.takeIf { it.tag_name.plainVersion() == currentVersion }
                justUpdated = currentVersion
            } else if (job.result == "failed" || job.result == "restored") {
                UpdateLog.log("Back on $currentVersion after a failed update")
                job.backup.parentFile.deleteRecursively()
                UpdatePaths.job.delete()
                remindTomorrow(quiet = true)
                say("You’re on $currentVersion. The update will be offered again tomorrow.")
            }
        }
    }
}
