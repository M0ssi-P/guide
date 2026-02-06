package backblazeb2.actions

import client
import backblazeb2.BackBlazeB2
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import logError
import java.util.Base64


@Serializable
enum class B2KeyCapability {
    @SerialName("readBucketLifecycleRules") ReadBucketLifecycleRules,
    @SerialName("writeBucketReplications") WriteBucketReplications,
    @SerialName("writeBucketLogging") WriteBucketLogging,
    @SerialName("listBuckets") ListBuckets,
    @SerialName("listFiles") ListFiles,
    @SerialName("deleteFiles") DeleteFiles,
    @SerialName("readBuckets") ReadBuckets,
    @SerialName("writeBuckets") WriteBuckets,
    @SerialName("readBucketNotifications") ReadBucketNotifications,
    @SerialName("listAllBucketNames") ListAllBucketNames,
    @SerialName("readBucketEncryption") ReadBucketEncryption,
    @SerialName("writeBucketLifecycleRules") WriteBucketLifecycleRules,
    @SerialName("readFiles") ReadFiles,
    @SerialName("writeBucketNotifications") WriteBucketNotifications,
    @SerialName("readBucketReplications") ReadBucketReplications,
    @SerialName("readBucketLogging") ReadBucketLogging,
    @SerialName("shareFiles") ShareFiles,
    @SerialName("writeFiles") WriteFiles,
    @SerialName("writeBucketEncryption") WriteBucketEncryption
}

@Serializable
data class KeyAllowedField(
    val capabilities: List<B2KeyCapability>,

    /** When present, access is restricted to one bucket. */
    val bucketId: String? = null,
    /**
     * When bucketId is set, and it is a valid bucket that has not been deleted,
     * this field is set to the name of the bucket. It's possible that bucketId
     * is set to a bucket that no longer exists, in which case this field will
     * be null. It's also null when bucketId is null. */
    val bucketName: String? = null,
    /** When present, access is restricted to files whose names start with the prefix */
    val namePrefix: String? =null,
    val s3ApiUrl: String = "https://s3.us-east-005.backblazeb2.com"
)

@Serializable
data class IAuthResponse(
    val accountId: String,
    val authorizationToken: String,
    val absoluteMinimumPartSize: Long,
    val allowed: KeyAllowedField,
    val apiUrl: String = "https://apiNNN.backblazeb2.com",
    val downloadUrl: String = "https://f002.backblazeb2.com",
    val recommendedPartSize: Long,
)

data class B2Credentials(
    val applicationKeyId: String,
    val applicationKey: String
)

suspend fun authorization(B2: BackBlazeB2, applicationKeyId: String, applicationKey: String): IAuthResponse {
    val combinedCredentials = Base64.getEncoder().encodeToString("$applicationKeyId:$applicationKey".toByteArray(Charsets.UTF_8))
    val header = mapOf("Authorization" to "Basic $combinedCredentials", "User-Agent" to B2.userAgent)

    try {
        val res = client.get(
            "https://api.backblazeb2.com/b2api/${B2.apiVersion}/b2_authorize_account",
            header,
        ).parsed<IAuthResponse>()

        println(res)

        return res
    } catch (err: Exception) {
        logError(err)
        return TODO("Provide the return value")
    }
}