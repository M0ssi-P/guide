import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

val HeadingList: List<String> = listOf("s", "s1", "is", "is1", "ms", "ms1")
val SubHeadList: List<String> = listOf("d")
val ParagraphList: List<String> = listOf("p", "q1", "q2", "pi", "li1", "m")
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