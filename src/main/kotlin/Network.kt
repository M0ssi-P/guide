import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import com.lagradost.nicehttp.addGenericDns
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

lateinit var cache: Cache


fun initialiseNetwork() {
    val dns = loadData<Int>("settings_dns") ?: 1.apply {
        saveData("settings_dns", this)
    }

    val cacheDir = System.getProperty("java.io.tmpdir")
    cache = Cache(
        File(cacheDir, "http_cache"),
        50L * 1024L * 1024L
    )


    okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .cache(cache)
        .apply {
            when (dns) {
                1 -> addGoogleDns()
                2 -> addCloudFlareDns()
                3 -> addAdGuardDns()
            }
        }
//        .addNetworkInterceptor { chain ->
//            val response = chain.proceed(chain.request())
//
//            response.newBuilder()
//                .removeHeader("Pragma")
//                .removeHeader("Cache-Control")
//                .header(
//                    "Cache-Control",
//                    "public, max-age=${TimeUnit.HOURS.toSeconds(6)}"
//                )
//                .build()
//        }
        .build()

    client = Requests(
        okHttpClient,
        defaultHeaders,
        defaultCacheTime = 6,
        defaultCacheTimeUnit = TimeUnit.HOURS,
        responseParser = Mapper
    )
}

//fun <A, B> Collection<A>.asyncMap(f: suspend (A) -> B): List<B> = runBlocking {
//    map { async { f(it) } }.map { it.await() }
//}

//fun <A, B> Collection<A>.asyncMapNotNull(f: suspend (A) -> B?): List<B> = runBlocking {
//    map { async { f(it) } }.mapNotNull { it.await() }
//}

fun OkHttpClient.Builder.addGoogleDns() = (
        addGenericDns(
            "https://dns.google/dns-query",
            listOf(
                "8.8.4.4",
                "8.8.8.8"
            )
        ))

fun OkHttpClient.Builder.addCloudFlareDns() = (
        addGenericDns(
            "https://cloudflare-dns.com/dns-query",
            listOf(
                "1.1.1.1",
                "1.0.0.1",
                "2606:4700:4700::1111",
                "2606:4700:4700::1001"
            )
        ))

fun OkHttpClient.Builder.addAdGuardDns() = (
        addGenericDns(
            "https://dns.adguard.com/dns-query",
            listOf(
                // "Non-filtering"
                "94.140.14.140",
                "94.140.14.141",
            )
        ))