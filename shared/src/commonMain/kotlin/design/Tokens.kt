package design

/*
 * The approved boards (design/boards, one .dc.html per screen) as numbers, for every app: Android maps them to Compose, the iPhone and
 * Apple TV apps to SwiftUI. Colours are 0xAARRGGBB, sizes are board pixels (phone: 1 px = 1 dp/pt; TV: the board is
 * 1280 px wide, so 1 px = screen width / 1280), times are milliseconds.
 */

/** Every colour the boards use, by role. */
object Palette {
    // night surfaces
    const val Backdrop = 0xFF0E1322     // launch and TV screens
    const val Night = 0xFF121829        // phone screens
    const val Sheet = 0xFF1B2340        // bottom sheets, toolbars
    const val Card = 0xFF182039         // host cards, TV language cards
    const val DeckTop = 0xFF1E2642      // tape deck gradient
    const val DeckBottom = 0xFF151B30
    const val HomeTapeTop = 0xFF1C2440  // "keep listening" card gradient
    const val HomeTapeBottom = 0xFF161D33
    const val NavGlass = 0xC7161D33     // rgba(22,29,51,.78)
    const val DeckGlass = 0xD1161D33    // rgba(22,29,51,.82), the reading decks
    const val DeckGlassLight = 0xEB1E243A // rgba(30,36,58,.92), decks over paper and sepia
    const val LiveDock = 0xDB161D33     // rgba(22,29,51,.86)
    const val TvPanel = 0xEB181F36      // rgba(24,31,54,.92)
    const val TvToast = 0xF0181F36      // rgba(24,31,54,.94)
    const val Scrim = 0x80050810        // rgba(5,8,16,.5)
    const val ScrimHome = 0x8C050810    // rgba(5,8,16,.55)
    const val ScrimUpdate = 0x99050810  // rgba(5,8,16,.6)
    const val ScrimTv = 0xB3050810      // rgba(5,8,16,.7)
    const val TvOverlay = 0xF0090C16    // rgba(9,12,22,.94)
    const val TvOverlayStrong = 0xF2090C16 // rgba(9,12,22,.95)
    const val TvModal = 0xEB090C16      // rgba(9,12,22,.92)
    const val TvLeave = 0xE6090C16      // rgba(9,12,22,.9)

    // ink on night
    const val Ink = 0xFFFBF6EA
    const val Title = 0xFFFFF8E8
    const val Text = 0xFFECE6D8
    const val VerseText = 0xFFE4DED0
    const val Soft = 0xFFD9D1C0
    const val Stone = 0xFFC8C1B2
    const val WeekNum = 0xFFB5BBD0
    const val Sub = 0xFF9AA1B8
    const val Meta = 0xFF8189A6
    const val Faint = 0xFF6B7391
    const val Future = 0xFF4A5170
    const val TabIdle = 0xFF7C84A0
    const val Brass = 0xFFC9B98F        // small labels over the quote and verse
    const val QuoteRef = 0xFFB8AE98
    const val LineGold = 0xFFFFF6E0     // the spoken sentence in the player
    const val Glow = 0xFFFFF8E8

    // lamp gold
    const val Gold = 0xFFF3C969
    const val OnGold = 0xFF1A1608
    const val Amber = 0xFFE9A44C
    const val Flame = 0xFFFFE7A8
    const val FlameCore = 0xFFFFF6D8
    const val Honey = 0xFFD9953E
    const val Copper = 0xFFB9742E
    const val LampTop = 0xFFE8C27A
    const val LampBottom = 0xFF8C6A30
    const val LampHandle = 0xFFB88A3E
    const val Initials = 0xFF3A2208

    // other voices
    const val English = 0xFF8FB7FF      // English everywhere: VU bars, mix, verse kind
    const val EnglishDeep = 0xFF5D86D6
    const val Coral = 0xFFFF7A66        // live dot, offline, leave
    const val CoralSoft = 0xFFFF9E8E
    const val HostWarm = 0xFFFFB29F
    const val HostWarmDeep = 0xFFE2513F
    const val HostCool = 0xFFB5CCFF
    const val HostCoolDeep = 0xFF4F6FBF
    const val Sage = 0xFFC7E6B5

    // paper, letters and songs
    const val Paper = 0xFFF2E9D6
    const val PaperOpen = 0xFFE9DFC9    // the unfolded letter on the phone home
    const val PaperB = 0xFFEADFC6
    const val PaperC = 0xFFF5EEDD
    const val PaperD = 0xFFE6D9BD
    const val PaperInk = 0xFF2A2418
    const val PaperBody = 0xFF3A2E1E
    const val PaperDeep = 0xFF2C2216
    const val PaperSign = 0xFF4A3F2C
    const val PaperPreview = 0xFF5A4A34
    const val PaperMuted = 0xFF7D735F
    const val PaperDate = 0xFF8A7658
    const val PaperChipText = 0xFFF2E9D6
    const val PaperPill = 0xFFB8AE98
    const val Wax = 0xFF8E1F2C
    const val WaxLight = 0xFFB8394A
    const val WaxBright = 0xFFD2515E
    const val WaxDeep = 0xFF6A1420
    const val WaxText = 0xFFF7D9C9
    const val Ribbon = 0xFFC23A48
    const val RibbonTv = 0xFF9E2A36
    const val RibbonTvTop = 0xFF7A1F29

    // the tape deck
    const val ReelHub = 0xFF1B2238
    const val ReelTape = 0xFF4A2E14
    const val ReelTapeEdge = 0xFF6B4420
    const val ReelFace = 0xFF232B45
    const val ReelSpoke = 0xFFD9C9A3
    const val Tape = 0xFF5A3A1A
    const val VuWell = 0xFF0B0F1C
    const val CassetteTape = 0xFFC9783A
    const val CassetteDark = 0xFF2A3150
    const val CassetteLight = 0xFF3B4468

    // the song shelf
    const val ShelfTop = 0xFF5B4024
    const val ShelfBottom = 0xFF3A2814

    /** Book spines for Table languages, in list order (repeats every ten). */
    val Spines = longArrayOf(0xFF7A2E2E, 0xFF23365E, 0xFF2F4A36, 0xFF6B4A1E, 0xFF4A2E5E, 0xFF1E4250, 0xFF7A5A1E, 0xFF5E2E3A, 0xFF2E4A5E, 0xFF4A5A2A)
    /** Envelope papers in the stacks, in order. */
    val EnvelopePapers = longArrayOf(0xFFE6D9BD, 0xFFEADFC6, 0xFFF5EEDD, 0xFFF2E9D6)
    /** Archive letter papers, in order. */
    val LetterPapers = longArrayOf(0xFFF2E9D6, 0xFFEADFC6, 0xFFF5EEDD, 0xFFE6D9BD)
}

/** A reading theme: the four the Table, Bible and Live share. */
class ReadingTheme(val id: String, val name: String, val bg: Long, val ink: Long, val sub: Long, val sheet: Long, val fold: Long) {
    val light get() = id == "paper" || id == "sepia"
}

object ReadingThemes {
    val Night = ReadingTheme("night", "Night", 0xFF121829, 0xFFECE6D8, 0xFF9AA1B8, 0xFF1B2340, 0xFF0A0E19)
    val Dusk = ReadingTheme("dusk", "Dusk", 0xFF251C2E, 0xFFF1E4DA, 0xFFB4A1B3, 0xFF33283F, 0xFF120C18)
    val Paper = ReadingTheme("paper", "Paper", 0xFFFBF6EA, 0xFF2A2418, 0xFF7D735F, 0xFFFFFDF7, 0xFFDCD2BC)
    val Sepia = ReadingTheme("sepia", "Sepia", 0xFFE9D9BB, 0xFF3A2B19, 0xFF7B6547, 0xFFF3E6CC, 0xFFC9B48D)
    val all = listOf(Night, Dusk, Paper, Sepia)
    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: Night
}

/** The light tone that tints the spoken sentence: its rgb, and its accent on night themes. */
class Tone(val id: String, val name: String, val rgb: Long, val accent: Long)

object Tones {
    val Lamp = Tone("lamp", "Lamplight", 0xF3B75F, 0xFFF3C969)
    val Day = Tone("day", "Daylight", 0xECE1C8, 0xFFE6D7B0)
    val Moon = Tone("moon", "Moonlight", 0x8FB7FF, 0xFF8FB7FF)
    val all = listOf(Lamp, Day, Moon)
    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: Lamp
}

/**
 * Everything a reading screen derives from its theme and tone (the boards' `tokens(s)`). Alphas are 0..1 over black
 * (light themes) or white (night themes) as on the boards.
 */
class ReadingLook(val theme: ReadingTheme, val tone: Tone) {
    val light = theme.light
    /** Accent: gold on night, ink-brown on paper (deep blue with moonlight). */
    val accent: Long = if (light) (if (tone.id == "moon") 0xFF3E62A8 else 0xFF9A6417) else tone.accent
    val glowAlpha = if (light) 0.22f else 0.14f
    val lineAlpha = if (light) 0.14f else 0.18f
    val chipAlpha = if (light) 0.06f else 0.07f
    val hairAlpha = if (light) 0.08f else 0.07f
    val litStartAlpha = if (light) 0.35f else 0.24f
    val litEndAlpha = 0.06f
    val pillLight = 0xFFFFFFFF
    val pillNightAlpha = 0.14f
    val deck: Long = if (light) Palette.DeckGlassLight else Palette.DeckGlass
    val pageBg: Long = if (light) 0xFFFFFCF4 else 0xFFF2E9D6
    val pageEdge: Long = if (light) 0xFFE8DFC9 else 0xFFD9CDB2
    val pageEdge2: Long = if (light) 0xFFDCD1B8 else 0xFFBFB295
    val curl: Long = if (light) 0xFFE3D8BF else 0xFFCDBF9F
    /** Active choice backgrounds in the settings sheet (faces, gaps, tones). */
    val selectedAlpha = if (light) 0.08f else 0.10f
    val toneSelectedAlpha = if (light) 0.07f else 0.08f
    val trackAlpha = if (light) 0.12f else 0.14f
}

/** cubic-bezier(x1, y1, x2, y2); y outside 0..1 overshoots. */
class Bezier(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

object Easings {
    /** Screen and content swaps, rise, enter. */
    val Swap = Bezier(.2f, .8f, .2f, 1f)
    /** Sheets sliding up, side panels, panels opening. */
    val Sheet = Bezier(.2f, .9f, .1f, 1f)
    /** The press-down scale on every button. */
    val Press = Bezier(.3f, .7f, .2f, 1f)
    val Overshoot10 = Bezier(.2f, .9f, .1f, 1.1f)
    val Overshoot115 = Bezier(.2f, .9f, .1f, 1.15f)
    val Overshoot12 = Bezier(.2f, .9f, .1f, 1.2f)
    val Overshoot13 = Bezier(.2f, .9f, .1f, 1.3f)
    val Overshoot14 = Bezier(.2f, .9f, .1f, 1.4f)
    val Overshoot16 = Bezier(.2f, .9f, .1f, 1.6f)
    val CssEase = Bezier(.25f, .1f, .25f, 1f)
    val CssEaseInOut = Bezier(.42f, 0f, .58f, 1f)
    val CssEaseOut = Bezier(0f, 0f, .58f, 1f)
}

/** Durations from the boards. */
object Durations {
    const val Press = 180
    const val ColorSwap = 250
    const val Toggle = 300
    const val Fade = 300
    const val Screen = 500
    const val Enter = 550
    const val EnterSlow = 600
    const val Sheet = 500
    const val TvPanel = 550
    const val Unfold = 600
    const val UnfoldSlow = 800
    const val Pop = 400
    const val PopSlow = 450
    const val Rise = 450
    const val RiseSlow = 1000
    const val Ripple = 2600
    const val Halo = 2600
    const val Flicker = 1800
    const val Beat = 1600
    const val Swing = 6000
    const val Spin = 3000
    const val Reel = 2400
    const val Wave = 1000
    const val Shimmer = 3500
    const val Sheen = 3400
    const val TvFocus = 350
    const val Glow = 1200
    const val Toast = 2200
}

/** Shared sizes and radii (board px). */
object Metrics {
    const val PhoneStatus = 54f        // iOS status bar on the boards; Android uses the real inset
    const val PillHeight = 56f
    const val PillRadius = 28f
    const val IconButton = 40f
    const val SheetInset = 8f
    const val SheetRadius = 38f
    const val SheetHandleW = 40f
    const val SheetHandleH = 5f
    const val NavHeight = 64f
    const val NavSide = 14f
    const val NavBottom = 26f
    const val TvWidth = 1280f
    const val TvFocusScale = 1.05f
    const val TvFocusRing = 3f
    const val TvRail = 100f
    const val TvPanelWidth = 470f
    const val TvPanelInset = 18f
    const val TvPanelRadius = 30f
}
