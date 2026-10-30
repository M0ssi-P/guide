package parsers

abstract class BaseProvider{
    //    Name of the provider
    abstract val name: String

    //    The main url of the provider
    abstract val hostUrl: String

    abstract val saveName: String

    // Languages that the provider supports
    open val isDiverse: Boolean = true
    open val language: String = "en"

    open val isWorking: Boolean = true

    data class IProviderStats(
        val name: String,
        val saveName: String,
        val hostUrl: String,
        val isDiverse: Boolean,
        val language: String,
        val isWorking: Boolean,
    )

    override fun toString(): String {
        return IProviderStats(
            name = this.name,
            saveName = this.saveName,
            hostUrl = this.hostUrl,
            isDiverse = this.isDiverse,
            language = this.language,
            isWorking = this.isWorking
        ).toString()
    }
}