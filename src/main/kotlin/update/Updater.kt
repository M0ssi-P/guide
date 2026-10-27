package update

import DesktopPlatform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.composables.updateArrowIcon
import com.composables.restoreIcon
import components.global.rise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ui.screens.thisweek.hand
import ui.theme.LocalTheme
import ui.theme.ThemeUI
import ui.theme.ZillaSlab
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private class UpdateFailure(val reason: String, val changed: Boolean) : Exception(reason)

private enum class StepState { WAIT, ACTIVE, DONE, FAIL }
private enum class Phase { RUNNING, RESTORING, FAILED, DONE }
private data class Step(val title: String, val sub: String, val state: StepState)

/**
 * The work of the updater process. It runs from the backup copy, so it can replace the install, check the new version opens,
 * and put the backup back if anything fails.
 */
private class UpdaterRun(val job: UpdateJob) {
    var phase by mutableStateOf(Phase.RUNNING)
    var at by mutableStateOf(0)
    var failedAt by mutableStateOf(-1)
    var restoreState by mutableStateOf(StepState.WAIT)
    var reason by mutableStateOf("")
    var attempt by mutableStateOf(0)
    val win = DesktopPlatform.Current == DesktopPlatform.Windows
    val old = job.oldVersion
    val new = job.newVersion

    val baseSteps get() = buildList {
        add("Closing The Guide" to "Saving your place so you come back to the same tab.")
        add("Backing up version $old" to job.backup.path)
        add("Installing $new" to job.install.path)
        if (job.relaunch) {
            add("Checking $new opens" to "If it doesn’t start within a minute, $old is put back.")
            add("Removing the backup" to "Only after $new has opened successfully.")
        } else add("Keeping the backup" to "It’s removed the first time you open $new.")
    }

    val steps: List<Step> get() {
        val base = baseSteps
        if (failedAt < 0) return base.mapIndexed { i, (t, s) -> Step(t, s, if (i < at || phase == Phase.DONE) StepState.DONE else if (i == at) StepState.ACTIVE else StepState.WAIT) }
        return base.take(failedAt + 1).mapIndexed { i, (t, s) ->
            if (i == failedAt) Step(t, "Stopped: see below.", StepState.FAIL) else Step(t, s, StepState.DONE)
        } + listOfNotNull(Step("Restoring $old", "Putting your backup back exactly as it was.", restoreState).takeIf { failedAt >= 2 })
    }

    val progress: Float get() = when {
        failedAt >= 0 -> if (restoreState == StepState.DONE) 1f else (failedAt + 1.5f) / (failedAt + 2f)
        phase == Phase.DONE -> 1f
        else -> (at + 0.5f) / baseSteps.size
    }

    suspend fun go(fromInstall: Boolean = false) = withContext(Dispatchers.IO) {
        failedAt = -1; restoreState = StepState.WAIT; phase = Phase.RUNNING; reason = ""
        try {
            if (!fromInstall) {
                at = 0
                UpdatePaths.updaterPid.writeText("${ProcessHandle.current().pid()}")
                UpdateLog.log("Updater running (pid ${ProcessHandle.current().pid()}); waiting for The Guide ${job.pid} to close")
                ProcessHandle.of(job.pid).orElse(null)?.let { h ->
                    runCatching { h.onExit().get(60, TimeUnit.SECONDS) }
                    if (h.isAlive) throw UpdateFailure("The Guide didn’t close, so the update couldn’t start. Nothing was changed.", false)
                }
                at = 1
                if (!File(job.backup, job.launcher).isFile) throw UpdateFailure("The backup of $old is missing, so nothing was changed.", false)
                UpdateLog.log("Backup in place at ${job.backup.path}")
                delay(400)
            }
            at = 2
            if (win) installWindows() else installMac()
            if (!File(job.install, job.launcher).isFile) throw UpdateFailure("$new installed somewhere other than ${job.install.path}.", true)
            if (job.relaunch) {
                at = 3
                launchAndWait()
                at = 4
            }
            job.result = "done"; job.save()
            phase = Phase.DONE
            UpdateLog.log("Update to $new finished")
            delay(1200)
            exitProcess(0)
        } catch (e: UpdateFailure) {
            fail(e)
        } catch (e: Exception) {
            UpdateLog.log("Unexpected: ${e.stackTraceToString().take(1500)}")
            fail(UpdateFailure("Something unexpected stopped the update (${e.message}).", true))
        }
    }

    private suspend fun fail(e: UpdateFailure) {
        UpdateLog.log("FAILED at step $at: ${e.reason}")
        failedAt = at
        reason = e.reason
        if (at >= 2) {
            phase = Phase.RESTORING
            restoreState = StepState.ACTIVE
            if (e.changed) {
                UpdateLog.log("Restoring $old from ${job.backup.path}")
                val dir = if (win) job.install else job.install.parentFile
                val ok = copyTree(job.backup, job.install, elevated = !canWrite(dir)) && File(job.install, job.launcher).isFile
                if (!ok) reason += " Putting $old back also failed: your backup is still at ${job.backup.path}."
                job.result = if (ok) "restored" else "failed"
            } else {
                delay(600)
                job.result = "restored"
            }
            restoreState = StepState.DONE
        } else job.result = "failed"
        job.save()
        phase = Phase.FAILED
    }

    private fun installWindows() {
        val path = job.installer.path
        val msi = job.installer.extension.equals("msi", true)
        val elevate = !canWrite(job.install)
        UpdateLog.log("Installing ${job.installer.name} (${if (elevate) "asking Windows for permission" else "no permission needed"})")
        val code = if (msi) {
            val msiLog = File(UpdatePaths.dir, "msi.log").path
            if (elevate) runElevatedWin("msiexec", "/i \"$path\" /qn /norestart /l*v \"$msiLog\"")
            else run("msiexec", "/i", path, "/qn", "/norestart", "/l*v", msiLog)
        } else {
            // shortcut: an .exe installer runs with its own window; switch to .msi releases for a silent install
            if (elevate) runElevatedWin(path, "") else run(path)
        }
        when (code) {
            0, 3010 -> UpdateLog.log("Installer finished")
            1223, 1602 -> throw UpdateFailure("Windows stopped the installer from writing to ${job.install.path} (permission was declined). Nothing was changed.", false)
            else -> throw UpdateFailure("The installer stopped with error $code. Details are in update.log.", true)
        }
    }

    private fun installMac() {
        val mount = Files.createTempDirectory("guide-update").toFile()
        if (run("/usr/bin/hdiutil", "attach", "-nobrowse", "-readonly", "-noautoopen", "-mountpoint", mount.path, job.installer.path) != 0)
            throw UpdateFailure("The downloaded disk image couldn’t be opened. Nothing was changed.", false)
        try {
            val app = mount.listFiles()?.firstOrNull { it.name.endsWith(".app") } ?: throw UpdateFailure("The disk image has no app in it. Nothing was changed.", false)
            // only insist on a signature when the version being replaced was signed
            val oldSigned = run("/usr/bin/codesign", "--verify", "--deep", "--strict", job.backup.path) == 0
            if (oldSigned && run("/usr/bin/codesign", "--verify", "--deep", "--strict", app.path) != 0)
                throw UpdateFailure("The new app didn’t pass the macOS signature check, so it wasn’t opened. Nothing was changed.", false)
            val elevate = !canWrite(job.install.parentFile)
            UpdateLog.log("Copying ${app.name} to ${job.install.path}${if (elevate) " (asking for your password)" else ""}")
            if (!copyTree(app, job.install, elevated = elevate)) {
                if (!job.install.exists() || elevate) throw UpdateFailure("macOS didn’t allow The Guide to be replaced in ${job.install.parentFile.path}.", true)
                throw UpdateFailure("Copying the new app failed. Details are in update.log.", true)
            }
        } finally {
            run("/usr/bin/hdiutil", "detach", mount.path, "-force")
            mount.delete()
        }
    }

    /** Starts the new version and waits for it to write its launched-* marker. */
    private suspend fun launchAndWait() {
        val started = System.currentTimeMillis()
        val exe = File(job.install, job.launcher)
        UpdateLog.log("Opening ${exe.path}")
        val p = ProcessBuilder(exe.path).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        while (System.currentTimeMillis() - started < 60_000) {
            val marker = UpdatePaths.dir.listFiles { f -> f.name.startsWith("launched-") && f.lastModified() >= started - 2000 }?.firstOrNull()
            if (marker != null) { UpdateLog.log("New version opened (${marker.name})"); return }
            if (!p.isAlive && p.exitValue() != 0) throw UpdateFailure("$new closed straight after opening (exit ${p.exitValue()}), so $old was put back.", true)
            delay(500)
        }
        p.descendants().forEach { it.destroyForcibly() }
        p.destroyForcibly()
        throw UpdateFailure("$new didn’t open within a minute, so $old was put back.", true)
    }

    fun openOld() {
        val restored = File(job.install, job.launcher)
        val exe = if (restored.isFile && job.result == "restored") restored else File(job.backup, job.launcher)
        UpdateLog.log("Opening $old from ${exe.path}")
        runCatching { ProcessBuilder(exe.path).redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start() }
        exitProcess(0)
    }
}

/** Entry point for `--updater <job file>`: only the small updater window, nothing else of the app. */
fun runUpdater(jobPath: String?) {
    val job = jobPath?.let { UpdateJob.load(File(it)) } ?: run { UpdateLog.log("Updater started without a job"); exitProcess(1) }
    val runner = UpdaterRun(job)
    application {
        val state = rememberWindowState(size = DpSize(560.dp, 640.dp))
        Window(
            onCloseRequest = { if (runner.phase == Phase.FAILED || runner.phase == Phase.DONE) exitApplication() },
            state = state, title = "The Guide Updater", resizable = false, icon = painterResource("icons/logo.png"),
        ) {
            ThemeUI { UpdaterScreen(runner) }
        }
    }
}

@Composable
private fun UpdaterScreen(r: UpdaterRun) {
    val theme = LocalTheme.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { r.go() }
    val failed = r.phase == Phase.FAILED
    val restoring = r.phase == Phase.RESTORING
    val shake = remember { Animatable(0f) }
    LaunchedEffect(failed) { if (failed) shake.animateTo(0f, keyframes { durationMillis = 450; -6f at 90; 5f at 180; -3f at 270; 2f at 360 }) }
    var copied by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(theme.colors.background).verticalScroll(rememberScrollState())
            .graphicsLayer { translationX = shake.value * density }.swapIn(r.attempt).padding(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.swapIn(failed || restoring)) {
            val bad = failed || restoring
            Box(Modifier.size(48.dp).background((if (bad) URed else theme.colors.primary).copy(alpha = 0.14f), R4), contentAlignment = Alignment.Center) {
                Icon(if (bad) restoreIcon(URedSoft) else updateArrowIcon(theme.colors.primaryBright), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(
                    when { failed -> "The update didn’t install"; restoring -> "Putting ${r.old} back"; else -> "Updating to ${r.new}" },
                    color = theme.colors.primaryText, fontFamily = ZillaSlab, fontWeight = FontWeight.Bold, fontSize = 22.sp
                )
                Text(
                    when {
                        failed -> "Nothing is lost. The Guide ${r.old} is back, just as it was."
                        restoring -> "Something went wrong, so your backup is being restored."
                        r.job.relaunch -> "The Guide will reopen by itself when it’s done."
                        else -> "This window closes by itself when it’s done."
                    },
                    color = theme.colors.text, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Column(Modifier.padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            r.steps.forEachIndexed { i, st -> StepRow(st, i) }
        }

        if (!failed) Column(Modifier.padding(top = 18.dp)) {
            ShimmerBar(r.progress, theme.colors.border, if (restoring) listOf(URed, URedSoft) else listOf(theme.colors.primary, theme.colors.primaryBright), height = 6.dp)
            Text("Please don’t turn off your computer. This usually takes under a minute.", color = theme.colors.text, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        } else Column(Modifier.padding(top = 18.dp).rise(0, 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = URedSoft, fontWeight = FontWeight.Bold)); append("What happened: "); pop()
                    append(r.reason)
                },
                color = Color(0xFFE8C2BC), fontSize = 12.5.sp, lineHeight = 19.sp,
                modifier = Modifier.fillMaxWidth().background(URed.copy(alpha = 0.08f), R4).border(1.dp, URed.copy(alpha = 0.3f), R4).padding(horizontal = 14.dp, vertical = 12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                UpdButton("Open The Guide ${r.old}", primary = true) { r.openOld() }
                UpdButton("Try again") { r.attempt++; scope.launch { r.go(fromInstall = true) } }
                Spacer(Modifier.weight(1f))
                UpdButton(if (copied) "✓ Copied" else "Copy details", ghost = true) {
                    runCatching {
                        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(java.awt.datatransfer.StringSelection(UpdatePaths.log.readText().takeLast(20_000)), null)
                        copied = true
                    }
                }
            }
            Text("Or download ${r.job.installer.name} from the website and run it yourself. Your backup stays until ${r.new} opens once.", color = theme.colors.text, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StepRow(st: Step, i: Int) {
    val theme = LocalTheme.current
    val ring = when (st.state) { StepState.DONE -> UGreen; StepState.ACTIVE -> theme.colors.primary; StepState.WAIT -> theme.colors.border; StepState.FAIL -> URed }
    val bg by animateColorAsState(
        when (st.state) { StepState.ACTIVE -> theme.colors.primary.copy(alpha = 0.08f); StepState.FAIL -> URed.copy(alpha = 0.08f); else -> Color.Transparent }, tween(300)
    )
    Row(Modifier.fillMaxWidth().rise(i * 40, 6.dp).background(bg, R4).padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val dot = Modifier.size(22.dp)
        when (st.state) {
            StepState.DONE, StepState.FAIL -> androidx.compose.runtime.key(st.state) {
                Box(dot.popOnce().background(ring, CircleShape), contentAlignment = Alignment.Center) {
                    Text(if (st.state == StepState.DONE) "✓" else "!", color = Color(0xFF1E1B1A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            StepState.ACTIVE -> Box(dot.pulse(theme.colors.primary, circle = true, periodMs = 1600).border(2.dp, ring, CircleShape))
            StepState.WAIT -> Box(dot.border(2.dp, ring, CircleShape))
        }
        Column {
            Text(st.title, color = when (st.state) { StepState.ACTIVE -> theme.colors.primaryText; StepState.WAIT -> theme.colors.text; StepState.FAIL -> URedSoft; else -> theme.colors.primaryText.copy(alpha = 0.85f) },
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(st.sub, color = theme.colors.text, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** Buttons of the update dialog and updater: filled orange, outlined, or text only. 38dp tall, 4dp corners. */
@Composable
internal fun UpdButton(label: String, primary: Boolean = false, ghost: Boolean = false, height: androidx.compose.ui.unit.Dp = 38.dp, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(150))
    val bg = when {
        primary -> if (hovered) theme.colors.primaryBright else theme.colors.primary
        ghost -> Color.Transparent
        else -> if (hovered) theme.colors.menuHoverColor else theme.colors.menu
    }
    Box(
        Modifier.height(height).graphicsLayer { scaleX = scale; scaleY = scale }.background(bg, R4)
            .then(if (!primary && !ghost) Modifier.border(1.dp, theme.colors.border, R4) else Modifier)
            .hoverable(source)
            .clickable(source, null, role = Role.Button, onClick = onClick).hand().padding(horizontal = if (primary) 16.dp else 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = when { primary -> UInk; ghost -> theme.colors.text; else -> theme.colors.primaryText }, fontSize = if (ghost) 12.5.sp else 13.sp,
            fontWeight = if (primary) FontWeight.Bold else if (ghost) FontWeight.Normal else FontWeight.SemiBold)
    }
}
