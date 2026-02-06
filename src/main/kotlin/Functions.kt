import io.github.cdimascio.dotenv.dotenv
import models.ISongDetails
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.zip.ZipInputStream
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

val HeadingList: List<String> = listOf("s", "s1", "is", "is1", "ms", "ms1")
val SubHeadList: List<String> = listOf("d")
val ParagraphList: List<String> = listOf("p", "q1", "q2", "pi", "li1", "m")
val ItalicList = mutableListOf("it", "add")

val dotenv = dotenv()

fun saveData(fileName: String, data: Any?) {
    tryWith {
        val filesDir = AppDir()
        val file = File(filesDir, fileName);

        val fos: FileOutputStream = FileOutputStream(file)
        val os: ObjectOutputStream = ObjectOutputStream(fos)
        os.writeObject(data)
        os.close()
        fos.close()
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> loadData(fileName: String): T? {
    try {
        val a = AppDir()
        if(a.list() != null) {
            if(fileName in a.list()) {
                val file = File(a, fileName)
                val fileIs: FileInputStream = FileInputStream(file)
                val objIs: ObjectInputStream = ObjectInputStream(fileIs)
                val data = objIs.readObject() as T
                objIs.close()
                fileIs.close()
                return data
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}

fun AppDir(): File {
    val getUser = System.getProperty("user.home")
    val dir = File(getUser, "guide/files")
    dir.mkdirs()
    return dir
}

fun dbExists(dbName: String): Boolean {
    val dir = File(System.getProperty("user.home"), "TheGuide/db")
    val dbFile = File(dir, dbName)
    return dbFile.exists()
}

fun <T : Any> T.disassemble(): List<Pair<String, KType>> {
    return this::class.memberProperties.map {
        Pair(it.name, it.returnType)
    }
}

fun extractZipToMemory(zipBytes: ByteArray): Map<String, String> {
    val extractedFiles = mutableMapOf<String, String>()

    ZipInputStream(zipBytes.inputStream()).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val bytes = zip.readBytes()
                val content = bytes.toString(Charsets.UTF_8) // for JSON files
                extractedFiles[entry.name] = content
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }

    return extractedFiles
}

fun String.Abbreviate(): String {
    return this.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString("") {
        it.first().uppercaseChar().toString()
    }
}

fun searchSongs(query: String, songs: List<ISongDetails>): List<ISongDetails> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return songs

    val isNumber = q.all { it.isDigit() }

    return songs.map { song ->
        var score = 0;

        if (isNumber) {
            if (song.songNumber.toString() == q) score += 100
            else if (song.songNumber.toString().startsWith(q)) score += 80
        }

        val title = song.titles.first().title.lowercase()
        if (title == q) score += 90
        else if (title.startsWith(q)) score += 70
        else if (title.contains(q)) score += 40

        if(song.lyrics.isNotEmpty()) {
            if (song.lyrics.any { it.lines.joinToString(" ").lowercase().contains(q) }) {
                score += 20
            }
        }

        song to score
    }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
}