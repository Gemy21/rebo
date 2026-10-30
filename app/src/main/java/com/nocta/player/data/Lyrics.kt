package com.nocta.player.data

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
    val source: String
) {
    val isPlain: Boolean get() = !synced

    fun activeIndex(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        if (!synced) return -1
        var low = 0
        var high = lines.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].timeMs <= positionMs + 90L) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}

object LrcParser {

    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val wordTag = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")
    private val metaTag = Regex("""^\[(ar|ti|al|by|offset|length|re|ve):.*]$""", RegexOption.IGNORE_CASE)

    fun parse(raw: String, source: String): Lyrics? {
        if (raw.isBlank()) return null

        val timed = mutableListOf<LyricLine>()
        val plain = mutableListOf<String>()
        var offset = 0L

        raw.lineSequence().forEach { original ->
            val line = original.trim()
            if (line.isEmpty()) return@forEach

            if (line.startsWith("[offset:", ignoreCase = true)) {
                offset = Regex("""^\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull() ?: offset
                return@forEach
            }

            if (metaTag.matches(line)) return@forEach

            val matches = timeTag.findAll(line).toList()
            if (matches.isEmpty()) {
                plain += line
                return@forEach
            }

            val text = wordTag.replace(timeTag.replace(line, ""), "").trim()
            if (text.isBlank()) return@forEach
            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionRaw = match.groupValues[3]
                val fraction = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> (fractionRaw.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionRaw.toLongOrNull() ?: 0L) * 10L
                    else -> fractionRaw.take(3).toLongOrNull() ?: 0L
                }
                timed += LyricLine(
                    timeMs = minutes * 60_000L + seconds * 1_000L + fraction - offset,
                    text = text
                )
            }
        }

        if (timed.isNotEmpty()) {
            val lines = timed
                .filter { it.timeMs >= 0 }
                .sortedBy { it.timeMs }
                .distinctBy { it.timeMs to it.text }
            return Lyrics(lines = lines, synced = true, source = source)
        }

        if (plain.isNotEmpty()) {
            return Lyrics(
                lines = plain.map { LyricLine(-1L, it) },
                synced = false,
                source = source
            )
        }
        return null
    }
}
