import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/** Where [saveData] keeps its files: ~/guide/files on the desktop; Android points it at the app's files folder at start. */
var storeDir: File = File(System.getProperty("user.home"), "guide/files")

fun saveData(fileName: String, data: Any?) {
    tryWith {
        val filesDir = storeDir.apply { mkdirs() }
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
    val file = File(storeDir.apply { mkdirs() }, fileName)
    if (!file.isFile) return null
    return try {
        TolerantInput(FileInputStream(file).buffered()).use { it.readObject() as T }
    } catch (e: java.io.ObjectStreamException) {
        // saved by an older build in a shape this one can't read: keep it aside rather than lose it, and start fresh
        println("loadData: $fileName no longer fits (${e.message}); kept as $fileName.bak")
        File(file.parentFile, "$fileName.bak").let { bak -> if (!bak.exists()) file.renameTo(bak) else file.delete() }
        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Reads data saved before the models moved into the shared module: moving them changed their computed
 * serialVersionUID, so a class whose saved fields are exactly the current ones is read as the current class.
 */
internal class TolerantInput(input: java.io.InputStream) : ObjectInputStream(input) {
    override fun readClassDescriptor(): java.io.ObjectStreamClass {
        val saved = super.readClassDescriptor()
        val local = runCatching { java.io.ObjectStreamClass.lookup(Class.forName(saved.name, false, TolerantInput::class.java.classLoader)) }.getOrNull() ?: return saved
        return if (local.serialVersionUID != saved.serialVersionUID && fields(local) == fields(saved)) local else saved
    }

    private fun fields(c: java.io.ObjectStreamClass) = c.fields.map { "${it.name}:${it.typeCode}:${it.typeString}" }
}
