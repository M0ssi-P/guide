package parsers.bible

import HeadingList
import ItalicList
import ParagraphList
import client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.devtools.DevTools
import org.openqa.selenium.devtools.v137.network.Network
import parsers.BibleParser
import parsers.bible.models.*
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class Bible: BibleParser() {
    override val name: String = "YouVersion"
    override val saveName: String = "youversion"
    override val hostUrl: String = "https://www.bible.com"
    override val isDiverse: Boolean = true

    private val header = mapOf("X-Requested-With" to "XMLHttpRequest", "referer" to hostUrl)

    override suspend fun getAllLanguages(): MutableList<ILanguage> {
        val promises: MutableList<ILanguage> = mutableListOf()

        val res = client.get("$hostUrl/api/bible/configuration", header).parsed<QueryLanguage>()

        res.response?.data?.defaultVersions?.forEach {
            promises.add(ILanguage(
                id = it.id!!,
                iso63901 = it.iso63901,
                iso63903 = it.iso63903,
                name = it.name,
                localName = it.localName,
                tag = it.tag,
                hasAudio = it.hasAudio,
                hasText = it.hasText,
                totalVersions = it.totalVersions,
                textDirection = it.textDirection,
                font = it.font
            ))
        }

        return promises
    }

    override suspend fun getVersionsFromLanguage(tag: String, type: String): MutableList<IBibleVersion> {
        val promises: MutableList<IBibleVersion> = mutableListOf()

        val res = client.get("$hostUrl/api/bible/versions", header, params = mapOf(
            "language_tag" to tag,
            "type" to type
        )).parsed<QueryVersion>()

        res.response?.data?.versions?.forEach {
            promises.add(IBibleVersion(
                id = it.id,
                abbreviation = it.abbreviation,
                localAbbreviation = it.localAbbreviation,
                title = it.title,
                localTitle = it.localTitle,
                audio = it.audio,
                audioCount = it.audioCount,
                text = it.text,
                language = it.language.toILanguage(),
            ))
        }

        return promises
    }

    suspend fun getFullVersion(version: IBibleVersion): IBibleVersion {
        val res = client.get("$hostUrl/api/bible/version/${version.id}", header).parsed<IBibleVersionResponse.IBibleV>()

        return IBibleVersion(
            id = res.id,
            abbreviation = res.abbreviation,
            localAbbreviation = res.localAbbreviation,
            title = res.title,
            localTitle = res.localTitle,
            audio = res.audio,
            audioCount = res.audioCount,
            text = res.text,
            language = res.language.toILanguage(),
            copyrightLong = res.copyrightLong.let {
                IBibleVersion.HTMLorText(it?.html, it?.text)
            },
            copyrightShort = res.copyrightShort.let {
                IBibleVersion.HTMLorText(it?.html, it?.text)
            },
            readerFooter = res.readerFooter.let {
                IBibleVersion.HTMLorText(it?.html, it?.text)
            },
            publisher = res.publisher?.let {
                IBibleVersion.Publisher(
                    id = it.id,
                    name = it.name,
                    localName = it.localName,
                    url = it.url,
                    it.description
                )
            },
            books = res.books?.map {
                it.toIBook()
            }
        )
    }

    suspend fun getChapterDoc(version: IBibleVersion, book: String, chapter: Int): IChapterContent {
        val promises: IChapterContent = IChapterContent(
            version = version,
        );

        if(version.books?.find { book == it.usfm } == null) {
            error("Invalid Book! Make sure that the usfm is correct.")
        }

        if(version.books.find { book == it.usfm }?.chapters?.size!! < chapter) {
            error("Chapter not found")
        }

        val key = getKey()

        if(key.isNullOrEmpty()) {
            error("Error retrieving the chapter! Please check your connection and try again")
        }

        val url = "$hostUrl/_next/data/$key/en/audio-bible/${version.id}/${book.uppercase()}.$chapter.${version.localAbbreviation}.json?versionId=${version.id}&usfm=${book.uppercase()}.$chapter.${version.localAbbreviation}"
        val res = client.get(url, header).parsed<PageProps>()

        val body = Jsoup.parse(res.pageProps.chapterInfo.content.trim())
        val element = body.select("div.version > div.book > div.chapter");

        element.select("> div").forEach { el ->
            if(HeadingList.any { it.equals(el.classNames().first(), ignoreCase = false)}) {
                val heading = el.select("span.heading").text()

                promises.chapter?.add(ChapterSection(
                    hasHeading = true,
                    hasSubheading = false,
                    heading = ChapterSection.HeadingObject(
                        heading = heading,
                        subHeading = null,
                        type = ""
                    ),
                    content = mutableListOf()
                ))
            }

            if(ParagraphList.any { it.equals(el.classNames().first(), ignoreCase = false) }) {
                if(promises.chapter.isNullOrEmpty()){
                    promises.chapter?.add(ChapterSection(
                        hasHeading = false,
                        hasSubheading = false,
                        heading = ChapterSection.HeadingObject(
                            heading = null,
                            subHeading = null,
                            type = null
                        ),
                        content = mutableListOf()
                    ))
                }

                if(el.classNames().first() == "p") {
                    val paragraph: ChapterSection.IParagraph = ChapterSection.IParagraph();

                    el.select("> span.verse").forEach { d ->
                        val number = d.select("> span.label").text().trim().toInt()

                        val verse = ChapterSection.IBibleVerse(
                            number = number,
                            content = mutableListOf()
                        )

                        d.select("> span").forEach { p ->
                            if(p.classNames().first() == "content") {
                                val text = p.text()
                                if(!text.isNullOrEmpty()) {
                                    verse.content.add(ChapterSection.IBibleVerseLine(
                                        type = VerseLineEnum.Word,
                                        isJesus = false,
                                        display = VerseLineDisplay.Normal,
                                        content = text,
                                    ))
                                }
                            }

                            if(p.classNames().first() == "note") {
                                val reference = p.select("> span.body").text()
                                verse.content.add(ChapterSection.IBibleVerseLine(
                                    type = VerseLineEnum.Reference,
                                    isJesus = false,
                                    display = VerseLineDisplay.Normal,
                                    reference = reference
                                ))
                            }

                            if(p.classNames().first() == "it") {
                                val text = p.select("> span.content").text()
                                verse.content.add(ChapterSection.IBibleVerseLine(
                                    type = VerseLineEnum.Word,
                                    isJesus = false,
                                    display = VerseLineDisplay.Italic,
                                    content = text
                                ))
                            }

                            if(p.classNames().first() == "wj") {
                                for (elem in p.select("> span")) {
                                    if(elem.classNames().first() == "content") {
                                        val text = elem.text()
                                        verse.content.add(ChapterSection.IBibleVerseLine(
                                            type = VerseLineEnum.Word,
                                            isJesus = true,
                                            display = VerseLineDisplay.Normal,
                                            content = text
                                        ))
                                    }

                                    if(elem.classNames().first() == "it") {
                                        val text = elem.select("> span.content").text()
                                        verse.content.add(ChapterSection.IBibleVerseLine(
                                            type = VerseLineEnum.Word,
                                            isJesus = true,
                                            display = VerseLineDisplay.Italic,
                                            content = text
                                        ))
                                    }

                                    if(elem.classNames().first() == "sc") {
                                        for(eleme in elem.select("> span")) {
                                            if(eleme.classNames().first() == "content") {
                                                val text = eleme.text()
                                                verse.content.add(ChapterSection.IBibleVerseLine(
                                                    type = VerseLineEnum.Word,
                                                    isJesus = true,
                                                    display = VerseLineDisplay.SmallCaps,
                                                    content = text
                                                ))
                                            }

                                            if(eleme.classNames().first() == "bd") {
                                                val text = eleme.select("> span.content").text()
                                                verse.content.add(ChapterSection.IBibleVerseLine(
                                                    type = VerseLineEnum.Word,
                                                    isJesus = true,
                                                    display = VerseLineDisplay.BdSmallCaps,
                                                    content = text
                                                ))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        paragraph.verses.add(verse)
                    }

                    promises.chapter?.last()?.content?.add(paragraph)
                }

                if(el.classNames().first() == "q1") {
                    val paragraph: ChapterSection.IParagraph = ChapterSection.IParagraph(
                        type = Paragraph.Q1
                    );

                    el.select("> span.verse").forEach { d ->
                        val number = d.select("> span.label").text().trim().toInt()

                        val verse = ChapterSection.IBibleVerse(
                            number = number,
                            content = mutableListOf()
                        )

                        d.select("> span").forEach { p ->
                            if(p.classNames().first() == "content") {
                                val text = p.text()
                                if(!text.isNullOrEmpty()) {
                                    verse.content.add(ChapterSection.IBibleVerseLine(
                                        type = VerseLineEnum.Word,
                                        isJesus = false,
                                        display = VerseLineDisplay.Normal,
                                        content = text,
                                    ))
                                }
                            }

                            if(p.classNames().first() == "note") {
                                val reference = p.select("> span.body").text()
                                verse.content.add(ChapterSection.IBibleVerseLine(
                                    type = VerseLineEnum.Reference,
                                    isJesus = false,
                                    display = VerseLineDisplay.Normal,
                                    reference = reference
                                ))
                            }

                            if(ItalicList.any { it == p.classNames().first()}) {
                                val text = p.select("> span.content").text()
                                verse.content.add(ChapterSection.IBibleVerseLine(
                                    type = VerseLineEnum.Word,
                                    isJesus = false,
                                    display = VerseLineDisplay.Italic,
                                    content = text
                                ))
                            }

                            if(p.classNames().first() == "wj") {
                                for (elem in p.select("> span")) {
                                    if(elem.classNames().first() == "content") {
                                        val text = elem.text()
                                        verse.content.add(ChapterSection.IBibleVerseLine(
                                            type = VerseLineEnum.Word,
                                            isJesus = true,
                                            display = VerseLineDisplay.Normal,
                                            content = text
                                        ))
                                    }

                                    if(ItalicList.any { it == elem.classNames().first()}) {
                                        val text = elem.select("> span.content").text()
                                        verse.content.add(ChapterSection.IBibleVerseLine(
                                            type = VerseLineEnum.Word,
                                            isJesus = true,
                                            display = VerseLineDisplay.Italic,
                                            content = text
                                        ))
                                    }

                                    if(elem.classNames().first() == "sc") {
                                        for(eleme in elem.select("> span")) {
                                            if(eleme.classNames().first() == "content") {
                                                val text = eleme.text()
                                                verse.content.add(ChapterSection.IBibleVerseLine(
                                                    type = VerseLineEnum.Word,
                                                    isJesus = true,
                                                    display = VerseLineDisplay.SmallCaps,
                                                    content = text
                                                ))
                                            }

                                            if(eleme.classNames().first() == "bd") {
                                                val text = eleme.select("> span.content").text()
                                                verse.content.add(ChapterSection.IBibleVerseLine(
                                                    type = VerseLineEnum.Word,
                                                    isJesus = true,
                                                    display = VerseLineDisplay.BdSmallCaps,
                                                    content = text
                                                ))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        paragraph.verses.add(verse)
                    }

                    promises.chapter?.last()?.content?.add(paragraph)
                }

                if(el.classNames().first() == "q2") {
                    val paragraph: ChapterSection.IParagraph = ChapterSection.IParagraph(
                        type = Paragraph.Q2
                    );

                    el.select("> span.verse").forEach { d ->
                        val number = d.select("> span.label").text().trim()
                        val currentVerse = d.attribute("data-usfm")!!.value.split(".")[2].toInt()

                        val hasNumber = number.isNotEmpty()

                        val verse = ChapterSection.IBibleVerse(
                            number = if (hasNumber) number.toInt() else currentVerse,
                            content = mutableListOf()
                        )

                        d.select("> span").forEach { p ->
                            if(p.classNames().first() == "content") {
                                val text = p.text()
                                if(!text.isNullOrEmpty()) {
                                    verse.content.add(ChapterSection.IBibleVerseLine(
                                        type = VerseLineEnum.Word,
                                        isJesus = false,
                                        display = VerseLineDisplay.Normal,
                                        content = text,
                                    ))
                                }
                            }

                            if(p.classNames().first() == "note") {
                                val reference = p.select("> span.body").text()
                                verse.content.add(ChapterSection.IBibleVerseLine(
                                    type = VerseLineEnum.Reference,
                                    isJesus = false,
                                    display = VerseLineDisplay.Normal,
                                    reference = reference
                                ))
                            }

                            if(ItalicList.any { it == p.classNames().first()}) {
                                val text = p.select("> span.content").text()
                                verse.content.add(ChapterSection.IBibleVerseLine(
                                    type = VerseLineEnum.Word,
                                    isJesus = false,
                                    display = VerseLineDisplay.Italic,
                                    content = text
                                ))
                            }

                            if(p.classNames().first() == "wj") {
                                for (elem in p.select("> span")) {
                                    if(elem.classNames().first() == "content") {
                                        val text = elem.text()
                                        verse.content.add(ChapterSection.IBibleVerseLine(
                                            type = VerseLineEnum.Word,
                                            isJesus = true,
                                            display = VerseLineDisplay.Normal,
                                            content = text
                                        ))
                                    }

                                    if(ItalicList.any { it == elem.classNames().first()}) {
                                        val text = elem.select("> span.content").text()
                                        verse.content.add(ChapterSection.IBibleVerseLine(
                                            type = VerseLineEnum.Word,
                                            isJesus = true,
                                            display = VerseLineDisplay.Italic,
                                            content = text
                                        ))
                                    }

                                    if(elem.classNames().first() == "sc") {
                                        for(eleme in elem.select("> span")) {
                                            if(eleme.classNames().first() == "content") {
                                                val text = eleme.text()
                                                verse.content.add(ChapterSection.IBibleVerseLine(
                                                    type = VerseLineEnum.Word,
                                                    isJesus = true,
                                                    display = VerseLineDisplay.SmallCaps,
                                                    content = text
                                                ))
                                            }

                                            if(eleme.classNames().first() == "bd") {
                                                val text = eleme.select("> span.content").text()
                                                verse.content.add(ChapterSection.IBibleVerseLine(
                                                    type = VerseLineEnum.Word,
                                                    isJesus = true,
                                                    display = VerseLineDisplay.BdSmallCaps,
                                                    content = text
                                                ))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if(verse.content.isNotEmpty()) paragraph.verses.add(verse)
                    }

                    promises.chapter?.last()?.content?.add(paragraph)
                }
                if(el.classNames().first() == "m") {
                    val paragraph: ChapterSection.IParagraph = ChapterSection.IParagraph(
                        type = Paragraph.M
                    );

                    el.select("> span.verse").forEach { d ->
                        val number = d.select("> span.label").text().trim()
                        val currentVerse = d.attribute("data-usfm")!!.value.split(".")[2].toInt()

                        val hasNumber = number.isNotEmpty()

                        val verse = ChapterSection.IBibleVerse(
                            number = if (hasNumber) number.toInt() else currentVerse,
                            content = mutableListOf()
                        )

                        d.select("> span").forEach { p ->
                            if(p.classNames().first() == "content") {
                                val text = p.text()
                                if(!text.isNullOrEmpty()) {
                                    verse.content.add(ChapterSection.IBibleVerseLine(
                                        type = VerseLineEnum.Word,
                                        isJesus = false,
                                        display = VerseLineDisplay.Normal,
                                        content = text,
                                    ))
                                }
                            }

                            if(p.classNames().first() == "note") {
                                val reference = p.select("> span.body").text()
                                verse.content.add(ChapterSection.IBibleVerseLine(
                                    type = VerseLineEnum.Reference,
                                    isJesus = false,
                                    display = VerseLineDisplay.Normal,
                                    reference = reference
                                ))
                            }

                            if(ItalicList.any { it == p.classNames().first()}) {
                                val text = p.select("> span.content").text()
                                verse.content.add(ChapterSection.IBibleVerseLine(
                                    type = VerseLineEnum.Word,
                                    isJesus = false,
                                    display = VerseLineDisplay.Italic,
                                    content = text
                                ))
                            }

                            if(p.classNames().first() == "wj") {
                                for (elem in p.select("> span")) {
                                    if(elem.classNames().first() == "content") {
                                        val text = elem.text()
                                        verse.content.add(ChapterSection.IBibleVerseLine(
                                            type = VerseLineEnum.Word,
                                            isJesus = true,
                                            display = VerseLineDisplay.Normal,
                                            content = text
                                        ))
                                    }

                                    if(ItalicList.any { it == elem.classNames().first()}) {
                                        val text = elem.select("> span.content").text()
                                        verse.content.add(ChapterSection.IBibleVerseLine(
                                            type = VerseLineEnum.Word,
                                            isJesus = true,
                                            display = VerseLineDisplay.Italic,
                                            content = text
                                        ))
                                    }

                                    if(elem.classNames().first() == "sc") {
                                        for(eleme in elem.select("> span")) {
                                            if(eleme.classNames().first() == "content") {
                                                val text = eleme.text()
                                                verse.content.add(ChapterSection.IBibleVerseLine(
                                                    type = VerseLineEnum.Word,
                                                    isJesus = true,
                                                    display = VerseLineDisplay.SmallCaps,
                                                    content = text
                                                ))
                                            }

                                            if(eleme.classNames().first() == "bd") {
                                                val text = eleme.select("> span.content").text()
                                                verse.content.add(ChapterSection.IBibleVerseLine(
                                                    type = VerseLineEnum.Word,
                                                    isJesus = true,
                                                    display = VerseLineDisplay.BdSmallCaps,
                                                    content = text
                                                ))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if(verse.content.isNotEmpty()) paragraph.verses.add(verse)
                    }

                    promises.chapter?.last()?.content?.add(paragraph)
                }
            }
        }

        return promises
    }

    override fun search(q: String) {
        TODO("Not yet implemented")
    }

    private fun getKey(): String? {
        var key: String? = null
        val latch = CountDownLatch(1)

        val options = ChromeOptions()
        options.addArguments("--headless=new") // Use `--headless=new` for Chrome 109+
        options.addArguments("--disable-gpu")  // Optional: improves stability
        options.addArguments("--window-size=1920,1080")

        val driver = ChromeDriver(options)
        val devTools: DevTools = driver.devTools
        devTools.createSession()

        devTools.send(
            Network.enable(
            Optional.of(1000000),
            Optional.of(500000),
            Optional.of(262144)
        ))

        devTools.addListener(Network.requestWillBeSent()) { fetch ->
            if(fetch.request.url.contains("_next/data")) {
                if(key.isNullOrEmpty()) {
                    val parts = fetch.request.url.split("/")
                    if(parts.size > 5) {
                        key = parts[5]
                        latch.countDown() // 🔑 Signal that we're done
                    }
                }
            }
        }

        driver.get("https://www.bible.com/bible/111/GEN.INTRO1.NIV")

        latch.await(15, TimeUnit.SECONDS)

        driver.quit()

        return key
    }
}

@Serializable
data class QueryVersion(
    @SerialName("response") var response: Response?,
) {
    @Serializable
    data class Response(
        @SerialName("data") var data: IBibleVersionResponse?
    )
}

@Serializable
data class QueryLanguage(
    @SerialName("response") var response: Response?,
) {
    @Serializable
    data class Response(
        @SerialName("data") var data: ILanguageResponse?
    )
}

@Serializable
data class PageProps(
    @SerialName("pageProps") val pageProps: Response
) {
    @Serializable
    data class Response(
        val chapterInfo: ChapterResponse
    )

    @Serializable
    data class ChapterResponse(
        @SerialName("audioChapterInfo") val chapterAudio: List<ChapterAudio>?,
        val content: String,
        val copyright: IBibleVersionResponse.HTMLorText,
        val next: PaginationResponse?,
        val previous: PaginationResponse?,
        val reference: PaginationResponse
    )

    @Serializable
    data class PaginationResponse(
        val canonical: Boolean?,
        val usfm: List<String>,
        val human: String,
        val toc: Boolean?,
        @SerialName("version_id") val versionId: Int
    )

    @Serializable
    data class ChapterAudio(
        val id: Int,
        @SerialName("version_id") val versionId: Int,
        val title: String,
        @SerialName("timing_available") val hasTiming: Boolean,
        val dramatized: Boolean,
        val default: Boolean,
        @SerialName("download_urls") val playOrDwnLoad: DownloadUrls,
    ) {
        @Serializable
        data class DownloadUrls(
            @SerialName("format_mp3_32k") val formattedMp3: String,
            @SerialName("format_hls") val formattedHls: String,
        )
    }
}

@Serializable
data class ILanguageResponse(
    @SerialName("default_versions") val defaultVersions: List<IVersionResponse>?
) {
    @Serializable
    data class IVersionResponse(
        val id: Int?,
        @SerialName("iso_639_1") val iso63901: String?,
        @SerialName("iso_639_3") val iso63903: String?,
        val name: String,
        @SerialName("local_name") val localName: String,
        @SerialName("language_tag") val tag: String,
        @SerialName("has_audio") val hasAudio: Boolean?,
        @SerialName("has_text") val hasText: Boolean?,
        @SerialName("total_versions") val totalVersions: Int?,
        @SerialName("text_direction") val textDirection: String,
        val font: String?
    )
}

@Serializable
data class IBibleVersionResponse(
    val versions: List<IBibleV>?
) {
    @Serializable
    data class IBibleV(
        val id: Int,
        val abbreviation: String,
        @SerialName("local_abbreviation") val localAbbreviation: String,
        val title: String,
        @SerialName("local_title") val localTitle: String,
        val audio: Boolean,
        @SerialName("audio_count") val audioCount: Int,
        val text: Boolean,
        val language: ILanguageResponse.IVersionResponse,
        @SerialName("copyright_short") val copyrightShort: HTMLorText?,
        @SerialName("copyright_long") val copyrightLong: HTMLorText?,
        @SerialName("reader_footer") val readerFooter: HTMLorText?,
        val publisher: Publisher?,
        val books: List<Book>?
    )

    @Serializable
    data class HTMLorText(
        val html: String?,
        val text: String?
    )

    @Serializable
    data class Book(
        @SerialName("text") val hasText: Boolean,
        @SerialName("audio") val hasAudio: Boolean,
        val usfm: String,
        val canon: String,
        val human: String,
        @SerialName("human_long") val humanLong: String,
        val abbreviation: String,
        val chapters: List<Chapter>,
    )

    @Serializable
    data class Chapter(
        val toc: Boolean,
        val usfm: String,
        val human: String,
        val canonical: Boolean,
    )

    @Serializable
    data class Publisher(
        val id: Int,
        val name: String,
        @SerialName("local_name") val localName: String?,
        val url: String,
        val description: String?,
    )
}

internal fun ILanguageResponse.IVersionResponse.toILanguage(): ILanguage {
    return ILanguage(
        id,
        dbId = null,
        iso63901,
        iso63903,
        name,
        localName,
        tag,
        hasAudio,
        hasAudio,
        totalVersions,
        textDirection,
        font
    )
}

internal fun IBibleVersionResponse.Book.toIBook(): IBibleVersion.Book {
    return IBibleVersion.Book(
        hasText,
        hasAudio,
        usfm,
        canon,
        human,
        humanLong,
        abbreviation,
        chapters = chapters.map {
            IBibleVersion.Chapter(
                toc = it.toc,
                usfm = it.usfm,
                human = it.human,
                canonical = it.canonical
            )
        }
    )
}