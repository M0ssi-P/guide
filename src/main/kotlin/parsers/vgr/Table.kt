package parsers.vgr

import Mapper.parsedList
import client
import com.google.gson.Gson
import kotlinx.serialization.json.Json
import logError
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import parsers.TableParser
import parsers.bible.QueryLanguage
import parsers.bible.models.ILanguage
import parsers.vgr.models.Calendar
import parsers.vgr.models.FormVars
import parsers.vgr.models.IHubResponse
import parsers.vgr.models.IHubSermonResponse
import parsers.vgr.models.IQotd
import parsers.vgr.models.IRepoSERMONS
import parsers.vgr.models.ISermonResponse
import parsers.vgr.models.LanguageRequest
import parsers.vgr.models.LanguageRoot
import parsers.vgr.models.Line
import parsers.vgr.models.Paragraph
import parsers.vgr.models.Section
import parsers.vgr.models.Segment
import parsers.vgr.models.Sermon
import parsers.vgr.models.SermonRequest
import parsers.vgr.models.Style
import parsers.vgr.utils.fromRoman
import parsers.vgr.utils.languageJson
import parsers.vgr.utils.toRoman
import parsers.vgr.utils.toUfsmCode
import java.time.LocalDate
import java.util.Locale
import java.util.Locale.getDefault

class Table: TableParser() {
    override val name: String = "The Table"
    override val saveName: String = "vgr"
    override val hostUrl: String = "https://branham.org"
    override val isDiverse: Boolean = true

    private val tUrl: String = "https://table.branham.org"
    private val messageBaseURL = "https://search.messagehub.info/api"

    private val header = mapOf("X-Requested-With" to "XMLHttpRequest", "referer" to hostUrl)

    override suspend fun getAllLanguages(): MutableList<ILanguage> {
        val promises = mutableListOf<ILanguage>();

        val unformatedLanguage: List<LanguageRoot> = Json.decodeFromString(languageJson)

        unformatedLanguage.map {
            promises.add(ILanguage(
                id = null,
                iso63901 = it.i,
                iso63903 = it.c,
                name = it.e,
                localName = it.f,
                tag = it.i,
                textDirection = "lt",
                hasAudio = true,
                hasText = true,
                totalVersions = 1,
                font = null
            ))
        }

        return promises
    }

    suspend fun getAllSermons(language: ILanguage): List<Sermon> {
        val promises: MutableList<Sermon> = mutableListOf();

        try {
            val reqBody = LanguageRequest("en")

            val res = client.post("$tUrl/rest/index/allSermons", header, json = reqBody).parsed<IRepoSERMONS>()
            val hub = client.get("${messageBaseURL}/languages/en/sermons").parsedList<IHubResponse>()

            res.Result.Sermons.map { table ->
                val isFoundHub = hub.find {
                    if (table.cab) {
                        val newDate = table.p.split("-")[0].trim().replace(" ", "-");

                        it.dateCode == newDate
                    } else {
                        it.dateCode == table.p
                    }
                }

                val index = res.Result.Sermons.indexOf(table);

                val hasNext = if(index != -1 && index < res.Result.Sermons.size - 1) res.Result.Sermons[index + 1] else null
                val hasPrev = if(index != -1 && index > 0) res.Result.Sermons[index - 1] else null

                if(isFoundHub != null) {
                    promises.add(Sermon(
                        id = isFoundHub.id,
                        date = isFoundHub.date,
                        dateCode = table.p,
                        language = isFoundHub.languageCode,
                        location = isFoundHub.location,
                        nextSermonDate = hasNext?.p ?: "",
                        prevSermonDate = hasPrev?.p ?: "",
                        title = table.t,
                        sortDate = isFoundHub.sortDate,
                        minutes = table.m,
                        isCab = table.cab,
                        c = table.c,
                        i = table.i,
                        ct = table.ct,
                        updatedAt = isFoundHub.updatedOn
                    ))
                } else {
                    promises.add(Sermon(
                        id = null,
                        date = "",
                        dateCode = table.p,
                        language = "",
                        location = "",
                        nextSermonDate = hasNext?.p ?: "",
                        prevSermonDate = hasPrev?.p ?: "",
                        title = table.t,
                        sortDate = "",
                        minutes = table.m,
                        isCab = table.cab,
                        c = table.c,
                        i = table.i,
                        ct = table.ct,
                        updatedAt = 12121212121212
                    ))
                }
            }
        } catch (e: Exception) {
            logError(e)
        }

        return promises
    }

    suspend fun getSermon(sermon: Sermon, language: String? = null, bibleV: String? = null): Sermon {
        val sections: MutableList<Section> = mutableListOf()

        val payload = SermonRequest(
            GetAllContent = true, null, SermonProductIdentityId = sermon.i, Language = language?: "en"
        )

        val res = client.post("$tUrl/rest/sermons/sermonRequest", header, json = payload).parsed<ISermonResponse>()
        val hub: IHubSermonResponse? = if(sermon.updatedAt != 12121212121212) {
            client.get("$messageBaseURL/languages/${language?: "en"}/sermons/${sermon.dateCode}/combined/${bibleV ?: "KJV"}").parsed<IHubSermonResponse>()
        } else {
            null
        }

        val totalSections = res.Result.TotalSections
        val location: String = Jsoup.parse(res.Result.Sections[0].Content).select(".place").text().trim()

        res.Result.Sections.map {
            val section: Section = Section(
                canHit = it.IsHit,
                isHeader = it.Paragraph == "header",
                key = it.Paragraph.ifEmpty { "whole" },
                paragraph = it.Paragraph.ifEmpty { "whole" },
                content = mutableListOf<Paragraph>()
            )

            val body = Jsoup.parse(it.Content);
            body.select("p").map { i ->
                val mainClasses = setOf("first_par", "normal_pn")
                val number = if(i.classNames().any {c -> c == "first_par"}) 1 else {
                    i.select("span.first span.pn").text().toIntOrNull()
                }
                val isScripture = i.classNames().contains("scr");

                if(i.classNames().any { com -> com in mainClasses }) {
                    section.content.add(
                        Paragraph(
                            id = hub?.sermon?.blocks?.find { bk -> bk.blockNumber == number }?.blockId,
                            number = number,
                            isFirst = number == 1,
                            text = "",
                            content = mutableListOf()
                        )
                    )
                }
                val targetP = section.content.last()
                targetP.content.add(
                    Line(
                        displayNumber = false,
                        number = targetP.number!!,
                        segments = mutableListOf()
                    )
                )

                if(i.classNames().none { toIgnore -> toIgnore in mainClasses } && targetP.text.isNotEmpty()) {
                    targetP.text += "\n"
                }

                i.select("span.st").map { x ->
                    val ignoredClasses = setOf("eagle", "se", "pn")

                    x.childNodes().map { node ->
                        when(node) {
                            is TextNode -> {
                                targetP.text += node.text()
                                targetP.content.last().segments.add(
                                    Segment(
                                        style = listOf(Style.Normal),
                                        content = node.text()
                                    )
                                )
                            }
                            is Element -> {
                                val classNames = node.classNames()
                                if (classNames.none { toIgnore -> toIgnore in ignoredClasses }) {
                                    when {
                                        classNames.contains("en") -> {
                                            val text = node.text()
                                            targetP.text += text
                                            targetP.content.last().segments.add(
                                                Segment(
                                                    style = listOf(Style.BlankTape, Style.Deemed),
                                                    content = text
                                                )
                                            )
                                        }
                                        classNames.contains("italic") -> {
                                            val text = node.text()
                                            targetP.text += text
                                            targetP.content.last().segments.add(
                                                Segment(
                                                    style = mutableListOf(Style.Italic, if(isScripture) Style.Scripture else Style.Nothing),
                                                    content = text
                                                )
                                            )
                                        }
                                        classNames.contains("ellquell") -> {
                                            val text = node.text()
                                            targetP.text += text
                                            targetP.content.last().segments.add(
                                                Segment(
                                                    style = listOf(Style.Normal),
                                                    content = text
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            sections.add(section)
        }

        return sermon.copy(totalSections = totalSections, sections = sections, location = location)
    }

    override fun search(q: String) {
        TODO("Not yet implemented")
    }

    suspend fun getCalendar(current: Boolean = true): Calendar {
        val promisses: MutableList<Calendar.ActiveDay> = mutableListOf();

        val payload = FormVars(
            formVars = listOf(
                FormVars.Fields(
                    name = "languagecode",
                    value = "en"
                )
            )
        )

        val res = client.post(
            "$hostUrl/branham/QOTD.aspx/${if(current) "wmCurrentCalendar" else "wmPrevCalendar"}",
            header,
            json = payload
        ).parsed<FormVars.FormRawResponse>()

        val htmlLoaded = Jsoup.parse(res.d.split("|")[0].trim());
        val calendarMonth = res.d.split("|")[1].trim().split(Regex("\\s+"))[0]
        val isCurrentMonth = calendarMonth.equals(LocalDate.now().month.name, ignoreCase = true)

        val selection = htmlLoaded.select(".columns[onClick*=\"toggleSelectedDay\"]")

        selection.map {
            val day = it.select(".dayactive").text().toIntOrNull()
            val regex = Regex("""'(\d+)'""")
            val href = it.selectFirst("a")?.attr("href")
            val match = regex.find(href!!)
            val id = match?.groupValues?.get(1)?.toIntOrNull()
            val isToday = if(isCurrentMonth) {
                val lastDay = selection.toList().last().select(".dayactive").text().toIntOrNull()
                day == lastDay && LocalDate.now().dayOfMonth == lastDay || day == lastDay && LocalDate.now().dayOfMonth == day!! + 1
            } else false

            promisses.add(Calendar.ActiveDay(
                isCurrent = isToday,
                day = day ?: 0,
                id = id ?: 0
            ))
        }

        return Calendar(
            currentMonth = isCurrentMonth,
            activeDays = promisses
        )
    }

    suspend fun qotd(activeDay: Calendar.ActiveDay): IQotd {
        val payload = FormVars(
            formVars = listOf(
                FormVars.Fields(
                    name = "pageid",
                    value = "${activeDay.id}"
                ),
                FormVars.Fields(
                    name = "languagecode",
                    value = "en"
                )
            )
        )

        val el = client.post(
            "$hostUrl/branham/QOTD.aspx/wmLoadQuoteArchive",
            header,
            json = payload
        ).parsed<FormVars.FormRawResponse>();

        val fullBibleReference = el.d.split("|")[0].split("\\s+".toRegex())

        // Bible
        val bookHuman = fullBibleReference.let {
            if(it.size == 3) "${it[0].fromRoman()} ${it[1]}" else it[0]
        }

        val bookUfsm = bookHuman.toUfsmCode()
        val chapter = fullBibleReference.let {
            if(it.size == 3) it[2].split(":").first().toInt() else it[1].split(":").first().toInt()
        }
        val verse = fullBibleReference.let {
            if(it.size == 3) it[2].split(":").last().toInt() else it[1].split(":").last().toInt()
        }
        val ufsm = "$bookUfsm.$chapter.$verse"
        val content = Jsoup.parse(el.d.split("|")[1]).text()


        // The Table
        val code = el.d.split("|")[3]
        val year = "19${code.split("-").first()}".toInt()
        val month = code.split("-").last().substring(0, 2).toInt()
        val day = code.split("-").last().substring(2, 4).toInt()
        val dayWhen = if (code.length > 7) code.last().toString() else null

        val title = Jsoup.parse(el.d.split("|")[4]).text()
        val doc = Jsoup.parse(el.d.split("|")[5])
        val cleanText = doc.select("p").html().replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")
        val text = Jsoup.parseBodyFragment(cleanText).body().wholeText()

        return IQotd(
            ufsm = ufsm,
            bookHuman = bookHuman,
            bookUfsm = bookUfsm!!,
            chapter = chapter,
            verseNumber = verse,
            content = content,
            TheTable = IQotd.ITheTable(
                title,
                code,
                year,
                month,
                day,
                dayWhen,
                text
            )
        )
    }
}