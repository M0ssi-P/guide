package parsers.vgr

/**
 * The language of a VGR-named audio file for [dateCode]: its ISO3 prefix ("SWA62-1014E A Guide VGR.opus" → SWA), or ENG
 * with no prefix ("62-1014E A Guide VGR.opus"). Null when the name isn't that sermon's VGR opus or m4a.
 */
fun vgrFileLanguage(name: String, dateCode: String): String? =
    Regex("^([A-Z]{3})?" + Regex.escape(dateCode) + " .* VGR\\.(opus|m4a)$", RegexOption.IGNORE_CASE).find(name)?.let { m ->
        m.groupValues[1].uppercase().ifEmpty { "ENG" }
    }

/**
 * A title as it is written into file names, used both to save and to look up so the two never disagree: Unicode
 * normalised, everything that isn't a letter, digit, space or hyphen removed (Windows can't store ? * : " < > | / \
 * at all, and VGR also drops # ! ' , . ;), spaces collapsed. "What Is The Holy Ghost?" → "What Is The Holy Ghost".
 */
fun vgrSafeTitle(title: String): String =
    nfc(title)
        .replace(Regex("[^\\p{L}\\p{N}\\s-]"), "").replace(Regex("\\s+"), " ").trim()

/** Unicode NFC, so an "é" typed as e + accent matches a precomposed "é". */
internal expect fun nfc(s: String): String
