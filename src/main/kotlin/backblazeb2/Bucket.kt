package backblazeb2

class Bucket {
    var b2: BackBlazeB2;
    var bucketId: String;

    constructor(b2: BackBlazeB2, bucketId: String) {
        this.b2 = b2
        this.bucketId = bucketId
    }

    fun getBucketName(): String {
        return bucketId
    }

    suspend fun file(fileName: String): File {
        return File(this, fileName);
    }
}