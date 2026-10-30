import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.OkHttpClient
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

// The HTTP clients every parser uses. Each app sets them up once at start (the desktop's initialiseNetwork, Android's App).

val defaultHeaders = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36"
)

lateinit var okHttpClient: OkHttpClient
lateinit var client: Requests

object Mapper : ResponseParser {

    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @OptIn(InternalSerializationApi::class)
    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return json.decodeFromString(kClass.serializer(),text)
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> NiceResponse.parsedList(): List<T> {
        return Mapper.json.decodeFromString(ListSerializer(T::class.serializer()), this.text)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return try {
            parse(text, kClass)
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(InternalSerializationApi::class)
    override fun writeValueAsString(obj: Any): String {
        val kClass = obj::class
        @Suppress("UNCHECKED_CAST")
        val serializer = kClass.serializer() as KSerializer<Any>
        return json.encodeToString(serializer, obj)
    }

    inline fun <reified T> parse(text: String): T {
        return json.decodeFromString(text)
    }
}

fun logError(e: Exception) {
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    e.printStackTrace(pw)
    val stackTrace: String = sw.toString()

//    toastString(e.localizedMessage, null , stackTrace)
    e.printStackTrace()
}

fun <T> tryWith(call: () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Exception) {
        logError(e)
        null
    }
}

suspend fun <T> tryWithSuspend(call: suspend () -> T): T? {
    return try {
        call.invoke()
    } catch (e: Exception) {
        logError(e)
        null
    } catch (e: CancellationException) {
        null
    }
}
