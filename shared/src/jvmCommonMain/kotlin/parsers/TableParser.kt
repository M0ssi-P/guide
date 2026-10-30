package parsers

import parsers.bible.models.ILanguage

abstract class TableParser: BaseParser() {
    abstract suspend fun getAllLanguages(): MutableList<ILanguage>
}
