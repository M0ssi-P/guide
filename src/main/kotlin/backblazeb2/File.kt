package backblazeb2

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FilterInputStream
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class File {
    private var _bucket: Bucket;
    private var _fileName: String = "";

    constructor(bucket: Bucket, fileData: String) {
        this._bucket = bucket;
        this._fileName = fileData;
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun createReadStream(onProgress: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null): InputStream {
        val pipeIn = PipedInputStream()
        val pipeOut = PipedOutputStream(pipeIn)

        val fileName = _fileName

        when {
            fileName.isNotEmpty() -> {
                GlobalScope.launch {
                    try {
                        val bucketName = _bucket.getBucketName()
                        val res = _bucket.b2.requestFromDownloadFileByName(bucketName, fileName)
                        val totalBytes = res.body.contentLength()
                        res.body.use { body ->
                            val inputStream = ProgressInputStream(body.byteStream(), totalBytes, onProgress)
                            inputStream.use { inputStream ->
                                inputStream.copyTo(pipeOut)
                            }
                        }
                    } catch (e: Exception) {
                        pipeOut.close()
                        pipeIn.close()
                    } finally {
                        pipeOut.close()
                    }
                }
            }
            else -> {
                throw Exception("To download a file, you must provide either its fileId or fileName.")
            }
        }

        return pipeIn
    }

}

fun interface ProgressListener {
    /**
     * @param bytesRead bytes downloaded so far
     * @param totalBytes total bytes to download (-1 if unknown)
     */
    fun onProgress(bytesRead: Long, totalBytes: Long)
}

class ProgressInputStream(
    private val original: InputStream,
    private val totalBytes: Long,
    private val listener: ProgressListener?
): FilterInputStream(original) {
    private var bytesReadSoFar = 0L

    override fun read(): Int {
        val byte = super.read()
        if (byte != -1) {
            bytesReadSoFar++
            listener?.onProgress(bytesReadSoFar, totalBytes)
        }
        return byte
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val n = super.read(b, off, len)
        if (n > 0) {
            bytesReadSoFar += n
            listener?.onProgress(bytesReadSoFar, totalBytes)
        }
        return n
    }
}