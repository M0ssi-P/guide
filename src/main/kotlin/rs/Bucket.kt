package rs

class Bucket {
    var r2: RS;

    constructor(r2: RS,) {
        this.r2 = r2
    }

    fun file(prefix: String): File {
        return File(this, prefix);
    }
}