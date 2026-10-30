package parsers.vgr

import java.text.Normalizer

internal actual fun nfc(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)
