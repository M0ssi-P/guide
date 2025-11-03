package parsers

import parsers.bible.models.IBibleVersion
import parsers.bible.models.ILanguage

abstract class BibleParser: BaseParser() {

    abstract suspend fun getAllLanguages(): MutableList<ILanguage>

    abstract suspend fun getVersionsFromLanguage(tag: String, type: String = "all"): MutableList<IBibleVersion>
}