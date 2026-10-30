package parsers

abstract class BaseParser: BaseProvider() {
    abstract fun search(q: String)
}