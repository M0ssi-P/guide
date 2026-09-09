package rs

import client
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.delay

data class RequestOptions(
    val maxRetries: Int? = null,
    val backoff: Int? = null,
)

class RS {
    private val r2_uri = "https://theguide-file-store.vrg-theguide.workers.dev"

    private suspend fun request(
        prefix: String,
        request: Requests = client,
        _options: RequestOptions,
        retries: Int = 0
    ): NiceResponse {
        val url = listOf<String>(
            r2_uri,
            prefix,
        ).joinToString("/")
        val options = _options.copy(maxRetries = 5, backoff = 150);
        var res: NiceResponse;

        try {
            res = client.custom(
                "GET",
                url,
                headers = request.defaultHeaders
            )
        } catch (e: Exception) {
            val delayMs = (options.backoff!! * (0.5 + Math.random())).toLong()
            delay(delayMs)
            return request(url.removePrefix("$r2_uri/"), request, options, retries + 1)
        }

        if(res.code == 200) {
            return res
        } else {
            val data = res.parsed<BackblazeErrorResponse>()
            when(data.code) {
                "bad_request" -> {
                    SecurityException("Bad Request")
                }
                else -> {
                    when(data.status) {
                        400 -> {
                            throw Exception("UnAuthorized")
                        }
                        403 -> {}
                        416 -> {}
                        500 -> {}
                        408 -> {}
                        503 -> {
                            if (retries >= options.maxRetries!!) throw Exception("UnAuthorized")

                            val delayMs = (options.backoff!! * (0.5 + Math.random())).toLong()
                            delay(delayMs)
                            return request(url.removePrefix("$r2_uri/"), request, options.copy(backoff = options.backoff * 2), retries + 1)
                        }
                        else -> {
                            throw Exception("Bad Request")
                        }
                    }
                }
            }
        }

        return res
    }

    suspend fun callApi(
        prefix: String,
        request: Requests = client,
        opts: RequestOptions = RequestOptions()
    ): NiceResponse {
        return request(prefix, request, opts)
    }

    suspend fun requestFromDownloadDb(
        prefix: String,
        request: Requests = client,
        opts: RequestOptions = RequestOptions()
    ): NiceResponse {
        return this.request(prefix, request, opts);
    }
}