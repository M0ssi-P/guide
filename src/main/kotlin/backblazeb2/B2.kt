package backblazeb2

import client
import backblazeb2.actions.B2Credentials
import backblazeb2.actions.IAuthResponse
import backblazeb2.actions.authorization
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.nicehttp.Requests
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class RequestOptions(
    val maxRetries: Int? = null,
    val backoff: Int? = null,
)

class BackBlazeB2 {
    private var credentials: B2Credentials;
    private var auth: IAuthResponse? = null;
    private var _userSetPartSize: Long? = null;

    var partSize: Long
        get() {
            val authNonNull = auth ?: throw IllegalStateException("auth is not initialized")
            return _userSetPartSize?.coerceAtLeast(authNonNull.absoluteMinimumPartSize)
                ?: authNonNull.recommendedPartSize
        }
        set(value) {
            _userSetPartSize = value
        }

    val apiVersion: String = "v2";
    val userAgent: String = "b2-js/1.2.3+nodejs/v25.3.0 https://git.io/b2-js"

    constructor(credentials: B2Credentials) {
        this.credentials = credentials
    }

    suspend fun authorize() {
        this.auth = authorization(this, this.credentials.applicationKeyId, this.credentials.applicationKey);
    }

    suspend fun authorize(credentials: B2Credentials): BackBlazeB2 {
        val b2 = BackBlazeB2(credentials);
        b2.authorize();
        return b2;
    }

    fun uriEncodeString(decoded: String): String {
        return URLEncoder.encode(decoded, StandardCharsets.UTF_8.name())
            .replace("%2F", "/")
    }

    fun uriDecodeString(encoded: String): String {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
            .replace("+", " ")
    }

    private suspend fun request(
        url: String,
        request: Requests = client,
        _options: RequestOptions,
        retries: Int = 0
    ): NiceResponse {
        val options = _options.copy(maxRetries = 5, backoff = 150);
        var res: NiceResponse;

        try {
            res = client.custom(
                "GET",
                url,
                headers = request.defaultHeaders + mapOf<String, String>(
                    "Authorization" to auth?.authorizationToken!!,
                    "User-Agent" to this.userAgent
                ),
            )
        } catch (e: Exception) {
            val delayMs = (options.backoff!! * (0.5 + Math.random())).toLong()
            delay(delayMs)
            return request(url, request, options, retries + 1)
        }

        if(res.code == 200) {
            return res
        } else {
            val data = res.parsed<BackblazeErrorResponse>()
            when(data.code) {
                "bad_request" -> {
                    SecurityException("Bad Request")
                }
                "expired_auth_token" -> {
                    if (retries < options.maxRetries!!) {
                        this.authorize()
                        return request(url, request, options, retries + 1)
                    } else {
                        throw Exception("UnAuthorized")
                    }
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
                            return request(url, request, options.copy(backoff = options.backoff * 2), retries + 1)
                        }
                        else -> {
                            throw Exception("UnAuthorized")
                        }
                    }
                }
            }
        }

        return res
    }

    suspend fun callApi(
        operationName: String,
        request: Requests = client,
        opts: RequestOptions = RequestOptions()
    ): NiceResponse {
        val url = listOf<String>(
            this.auth?.downloadUrl!!,
            "b2api",
            this.apiVersion,
            operationName
        ).joinToString("/")

        return request(url, request, opts);
    }

    suspend fun requestFromDownloadFileByName(
        bucketName: String,
        fileName: String,
        request: Requests = client,
        opts: RequestOptions = RequestOptions()
    ): NiceResponse {
        val url = listOf<String>(this.auth?.downloadUrl!!, "file", bucketName, fileName).joinToString ("/");
        return this.request(url, request, opts);
    }

    suspend fun bucket(bucketId: String): Bucket {
        return Bucket(this, bucketId)
    }
}