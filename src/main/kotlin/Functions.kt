import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import models.ISongDetails
import org.commonmark.node.*
import org.commonmark.parser.Parser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

val HeadingList: List<String> = listOf("s", "s1", "is", "is1", "ms", "ms1")
val SubHeadList: List<String> = listOf("d")
val ParagraphList: List<String> = listOf("p", "q1", "q2", "pi", "li1", "m", "pc")
val ItalicList = mutableListOf("it", "add")

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

fun isNewerVersion(current: String, latest: String): Boolean {
    val c = current.removePrefix("v").split(".").map { it.toInt() }
    val l = latest.removePrefix("v").split(".").map { it.toInt() }

    for (i in 0 until maxOf(c.size, l.size)) {
        val cv = c.getOrElse(i) { 0 }
        val lv = l.getOrElse(i) { 0 }
        if (lv > cv) return true
        if (lv < cv) return false
    }
    return false
}

fun markdownToAnnotatedString(markdown: String): AnnotatedString {
    val parser = Parser.builder().build()
    val document = parser.parse(markdown)
    val builder = AnnotatedString.Builder()

    parseChildren(document, builder)
    return builder.toAnnotatedString()
}

fun parseChildren(parent: Node, builder:  AnnotatedString.Builder) {
    var child = parent.firstChild
    while (child != null) {
        parseNode(child, builder)
        child = child.next
    }
}

fun parseNode(node: Node, builder: AnnotatedString.Builder) {
    when (node) {
        is Text -> builder.append(node.literal)
        is StrongEmphasis -> {
            builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            parseChildren(node, builder)
            builder.pop()
        }
        is Emphasis -> {
            builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            parseChildren(node, builder)
            builder.pop()
        }
        is Paragraph -> {
            parseChildren(node, builder)
            builder.append("\n\n")
        }
        is BulletList -> parseChildren(node, builder)
        is ListItem -> {
            builder.append("• ")
            parseChildren(node, builder)
            builder.append("\n")
        }
        is Link -> {
            val start = builder.length
            parseChildren(node, builder)
            builder.addStringAnnotation(
                tag = "URL",
                annotation = node.destination,
                start = start,
                end = builder.length
            )
        }
        else -> parseChildren(node, builder)
    }
}

fun markdownBodyToAnnotatedString(body: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val lines = body.split("\r\n", "\n") // support both \r\n and \n

    for (line in lines) {
        when {
            line.startsWith("## ") -> {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                builder.append(line.removePrefix("## ").trim())
                builder.pop()
                builder.append("\n\n")
            }
            line.startsWith("- ") -> {
                builder.append("• ${line.removePrefix("- ").trim()}\n")
            }
            line.isBlank() -> {
                builder.append("\n")
            }
            else -> {
                builder.append(line)
            }
        }
    }
    return builder.toAnnotatedString()
}

fun formatAssetSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val units = arrayOf("KB", "MB", "GB", "TB")
    val exp = floor(log10(bytes.toDouble()) / log10(1024.0)).toInt()
    val size = bytes / 1024.0.pow(exp.toDouble())
    val unit = units.getOrElse(exp - 1) { "PB" }

    return String.format("%.2f %s", size, unit)
}