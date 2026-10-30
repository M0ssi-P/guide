import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.nio.ByteBuffer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

data class Probe(val title: String, val n: Int) : java.io.Serializable

class StoreTest {
    private val dir = File.createTempFile("store", "").apply { delete(); mkdirs() }.also { storeDir = it }

    @AfterTest fun clean() { dir.deleteRecursively() }

    /** [value] serialized as if [Probe] had had [suid] (what an older build wrote). */
    private fun savedWith(value: Any, suid: Long, rename: Pair<String, String>? = null): ByteArray {
        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { o -> o.writeObject(value) } }.toByteArray()
        val now = ByteBuffer.allocate(8).putLong(ObjectStreamClass.lookup(Probe::class.java).serialVersionUID).array()
        val at = (0..bytes.size - 8).first { i -> (0 until 8).all { k -> bytes[i + k] == now[k] } }
        ByteBuffer.wrap(bytes, at, 8).putLong(suid)
        return rename?.let { (from, to) -> String(bytes, Charsets.ISO_8859_1).replace(from, to).toByteArray(Charsets.ISO_8859_1) } ?: bytes
    }

    @Test fun readsDataSavedBeforeTheClassMoved() {
        File(dir, "probe").writeBytes(savedWith(arrayListOf(Probe("Hear ye Him", 3)), 7791186461179744302L))
        assertEquals<List<Probe>?>(listOf(Probe("Hear ye Him", 3)), loadData<ArrayList<Probe>>("probe"))
    }

    @Test fun keepsDataThatNoLongerFitsAsBak() {
        // the old class had another field ("tytle"): it can't be read, so it is set aside, not deleted
        File(dir, "probe").writeBytes(savedWith(Probe("x", 1), 1L, "title" to "tytle"))
        assertNull(loadData<Probe>("probe"))
        assertFalse(File(dir, "probe").exists())
        assertTrue(File(dir, "probe.bak").exists())
    }
}
