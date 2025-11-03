package parsers.vgr.utils

val languageJson: String = """
   [
      { "c": "afr", "e": "Afrikaans", "f": "Afrikaans", "i": "af" },
      { "c": "ast", "e": "Asante Twi", "f": "Asante Twi", "i": "ak" },
      { "c": "bem", "e": "Bemba", "f": "Icibemba", "i": "bem" },
      { "c": "ceb", "e": "Cebuano", "f": "Cebuano", "i": "ceb" },
      { "c": "cha", "e": "Chichewa", "f": "Chichewa", "i": "ny" },
      { "c": "chn", "e": "Chinese Simplified", "f": "简体中文", "i": "zh" },
      { "c": "cnt", "e": "Chinese Traditional", "f": "繁體中文", "i": "cnt" },
      { "c": "cre", "e": "Haitian Creole", "f": "Kreyòl Ayisyen", "i": "ht" },
      { "c": "ctk", "e": "Chitumbuka", "f": "Chitumbuka", "i": "tum" },
      { "c": "czh", "e": "Czech", "f": "Čeština", "i": "cs" },
      { "c": "dut", "e": "Dutch", "f": "Nederlands", "i": "nl" },
      { "c": "emk", "e": "Emakhuwa", "f": "Emakhuwa", "i": "vmw" },
      { "c": "eng", "e": "English", "f": "English", "i": "en" },
      { "c": "ewe", "e": "Ewe", "f": "Eʋegbe", "i": "ee" },
      { "c": "fin", "e": "Finnish", "f": "Suomi", "i": "fi" },
      { "c": "fon", "e": "Fon", "f": "Fɔngbe", "i": "fon" },
      { "c": "frn", "e": "French", "f": "Français", "i": "fr" },
      { "c": "ger", "e": "German", "f": "Deutsch", "i": "de" },
      { "c": "hil", "e": "Hiligaynon", "f": "Hiligaynon", "i": "hil" },
      { "c": "hin", "e": "Hindi", "f": "हिंदी", "i": "hi" },
      { "c": "hun", "e": "Hungarian", "f": "Magyar", "i": "hu" },
      { "c": "ind", "e": "Indonesian", "f": "Bahasa Indonesia", "i": "id" },
      { "c": "itl", "e": "Italian", "f": "Italiano", "i": "it" },
      { "c": "khm", "e": "Khmer", "f": "ខ្មែរ", "i": "km" },
      { "c": "kng", "e": "Kikongo-kituba", "f": "Kikongo-Kituba", "i": "kon" },
      { "c": "kya", "e": "Kyangonde", "f": "Kyangonde", "i": "nyy" },
      { "c": "lat", "e": "Latvian", "f": "Latviešu", "i": "lv" },
      { "c": "lin", "e": "Lingala", "f": "Lingála", "i": "ln" },
      { "c": "lit", "e": "Lithuanian", "f": "Lietuvių", "i": "lt" },
      { "c": "loz", "e": "Lozi", "f": "Silozi", "i": "loz" },
      { "c": "lua", "e": "Ciluba", "f": "Ciluba", "i": "lua" },
      { "c": "lve", "e": "Luvale", "f": "Luvale", "i": "lue" },
      { "c": "mag", "e": "Malagasy", "f": "Malagasy", "i": "mg" },
      { "c": "mal", "e": "Malayalam", "f": "മലയാളം", "i": "ml" },
      { "c": "mar", "e": "Marathi", "f": "मराठी", "i": "mr" },
      { "c": "nde", "e": "Ndebele", "f": "Ndebele", "i": "nd" },
      { "c": "nep", "e": "Nepali", "f": "नेपाली", "i": "ne" },
      { "c": "nor", "e": "Norwegian", "f": "Norsk", "i": "no" },
      { "c": "nst", "e": "Northern Sotho", "f": "Northern Sotho", "i": "nso" },
      { "c": "osh", "e": "Oshikwanyama", "f": "Oshikwanyama", "i": "kj" },
      { "c": "pol", "e": "Polish", "f": "Polski", "i": "pl" },
      { "c": "por", "e": "Portuguese", "f": "Português", "i": "pt" },
      { "c": "pun", "e": "Punjabi", "f": "ਪੰਜਾਬੀ", "i": "pa" },
      { "c": "rom", "e": "Romanian", "f": "Română", "i": "ro" },
      { "c": "rus", "e": "Russian", "f": "Русский", "i": "ru" },
      { "c": "sho", "e": "Shona", "f": "ChiShona", "i": "sn" },
      { "c": "spn", "e": "Spanish", "f": "Español", "i": "es" },
      { "c": "sst", "e": "South Sotho", "f": "Southern Sesotho", "i": "st" },
      { "c": "ssw", "e": "Siswati", "f": "SiSwati", "i": "ss" },
      { "c": "swa", "e": "Kiswahili", "f": "Kiswahili", "i": "sw" },
      { "c": "swd", "e": "Swedish", "f": "Svenska", "i": "sv" },
      { "c": "tag", "e": "Tagalog", "f": "Tagalog", "i": "tl" },
      { "c": "tam", "e": "Tamil", "f": "தமிழ்", "i": "ta" },
      { "c": "tel", "e": "Telugu", "f": "తెలుగు", "i": "te" },
      { "c": "tng", "e": "Chitonga", "f": "Chitonga", "i": "tog" },
      { "c": "tso", "e": "Xitsonga", "f": "Xitsonga", "i": "ts" },
      { "c": "tsv", "e": "Tshivenda", "f": "Tshivenda", "i": "ve" },
      { "c": "tsw", "e": "Tswana", "f": "Setswana", "i": "tn" },
      { "c": "ukr", "e": "Ukrainian", "f": "Українська", "i": "uk" },
      { "c": "vie", "e": "Vietnamese", "f": "Tiếng Việt", "i": "vi" },
      { "c": "xho", "e": "Xhosa", "f": "isiXhosa", "i": "xh" },
      { "c": "zul", "e": "Zulu", "f": "isiZulu", "i": "zu" },
      { "c": "lug", "e": "Luganda", "f": "Luganda", "i": "lg" },
      { "c": "orm", "e": "Oromo", "f": "Afaan Oromoo", "i": "om" },
      { "c": "ctd", "e": "Tedim Chin", "f": "Tedim Chin", "i": "ctd" },
      { "c": "kir", "e": "Kirundi", "f": "Kirundi", "i": "rn" },
      { "c": "kin", "e": "Kinyarwanda", "f": "Kinyarwanda", "i": "rw" },
      { "c": "jpn", "e": "Japanese", "f": "日本語", "i": "ja" },
      { "c": "arm", "e": "Armenian", "f": "Հայերեն", "i": "hy" },
      { "c": "tur", "e": "Turkish", "f": "Türkçe", "i": "tr" },
      { "c": "urd", "e": "Urdu", "f": "اُردو", "i": "ur" }
   ] 
""";

fun String.toUfsmCode(): String? {
    val bookAbbreviations = mapOf(
        "Genesis" to "GEN", "Exodus" to "EXO", "Leviticus" to "LEV", "Numbers" to "NUM", "Deuteronomy" to "DEU",
        "Joshua" to "JOS", "Judges" to "JDG", "Ruth" to "RUT", "1 Samuel" to "1SA", "2 Samuel" to "2SA",
        "1 Kings" to "1KI", "2 Kings" to "2KI", "1 Chronicles" to "1CH", "2 Chronicles" to "2CH", "Ezra" to "EZR",
        "Nehemiah" to "NEH", "Esther" to "EST", "Job" to "JOB", "Psalm" to "PSA", "Psalms" to "PSA", "Proverbs" to "PRO",
        "Ecclesiastes" to "ECC", "Song of Solomon" to "SNG", "Isaiah" to "ISA", "Jeremiah" to "JER", "Lamentations" to "LAM",
        "Ezekiel" to "EZK", "Daniel" to "DAN", "Hosea" to "HOS", "Joel" to "JOL", "Amos" to "AMO",
        "Obadiah" to "OBA", "Jonah" to "JON", "Micah" to "MIC", "Nahum" to "NAM", "Habakkuk" to "HAB",
        "Zephaniah" to "ZEP", "Haggai" to "HAG", "Zechariah" to "ZEC", "Malachi" to "MAL",
        "Matthew" to "MAT", "Mark" to "MRK", "Luke" to "LUK", "John" to "JHN", "Acts" to "ACT",
        "Romans" to "ROM", "1 Corinthians" to "1CO", "2 Corinthians" to "2CO", "Galatians" to "GAL", "Ephesians" to "EPH",
        "Philippians" to "PHP", "Colossians" to "COL", "1 Thessalonians" to "1TH", "2 Thessalonians" to "2TH", "1 Timothy" to "1TI",
        "2 Timothy" to "2TI", "Titus" to "TIT", "Philemon" to "PHM", "Hebrews" to "HEB", "James" to "JAS",
        "1 Peter" to "1PE", "2 Peter" to "2PE", "1 John" to "1JN", "2 John" to "2JN", "3 John" to "3JN",
        "Jude" to "JUD", "Revelation" to "REV"
    )

    return bookAbbreviations[this]
}

fun Int.toRoman(): String {
    if (this !in 1..3999) return "Invalid number"

    val romanMap = listOf(
        1000 to "M",
        900 to "CM",
        500 to "D",
        400 to "CD",
        100 to "C",
        90 to "XC",
        50 to "L",
        40 to "XL",
        10 to "X",
        9 to "IX",
        5 to "V",
        4 to "IV",
        1 to "I"
    )

    var number = this
    val result = StringBuilder()

    for ((value, numeral) in romanMap) {
        while (number >= value) {
            result.append(numeral)
            number -= value
        }
    }

    return result.toString()
}

fun String.fromRoman(): Int {
    val romanMap = mapOf(
        'I' to 1,
        'V' to 5,
        'X' to 10,
        'L' to 50,
        'C' to 100,
        'D' to 500,
        'M' to 1000
    )

    var total = 0
    var prevValue = 0

    for (char in this.reversed()) {
        val value = romanMap[char] ?: return -1 // invalid character
        if (value < prevValue) {
            total -= value
        } else {
            total += value
        }
        prevValue = value
    }

    return total
}