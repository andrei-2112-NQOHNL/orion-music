package com.andrew.music

import com.grack.nanojson.JsonParser
import okhttp3.OkHttpClient
import java.net.URLEncoder

data class LyricWord(val text: String, val startMs: Long, val endMs: Long)
data class LyricLine(val startMs: Long, val endMs: Long, val text: String, val words: List<LyricWord>)

/** Timed lyrics from LRCLIB (free, no key). Word timing is estimated inside each line by word length. */
object Lyrics {
    private val client = OkHttpClient()
    private val stamp = Regex("""\[(\d+):(\d+(?:\.\d+)?)]""")

    private fun clean(t: String) = t.replace(Regex("""\(.*?\)|\[.*?]|official.*|lyrics?|audio|video""", RegexOption.IGNORE_CASE), "").trim(' ', '-')

    fun fetch(track: Track): List<LyricLine> {
        val title = URLEncoder.encode(clean(track.title), "UTF-8")
        val artist = URLEncoder.encode(clean(track.artist), "UTF-8")
        val url = "https://lrclib.net/api/search?track_name=$title&artist_name=$artist"
        val body = client.newCall(okhttp3.Request.Builder().url(url).header("User-Agent", "OrionMusic").build())
            .execute().use { it.body?.string() } ?: return emptyList()
        val arr = JsonParser.array().from(body)
        for (o in arr) {
            val s = (o as? com.grack.nanojson.JsonObject)?.getString("syncedLyrics") ?: continue
            if (s.isNotBlank()) return parse(s, track.durationSec * 1000)
        }
        return emptyList()
    }

    fun parse(lrc: String, totalMs: Long): List<LyricLine> {
        val raw = mutableListOf<Pair<Long, String>>()
        for (l in lrc.lines()) {
            val m = stamp.find(l) ?: continue
            val ms = (m.groupValues[1].toLong() * 60_000 + m.groupValues[2].toDouble() * 1000).toLong()
            raw.add(ms to l.substring(m.range.last + 1).trim())
        }
        raw.sortBy { it.first }
        return raw.mapIndexedNotNull { i, (start, text) ->
            if (text.isEmpty()) return@mapIndexedNotNull null
            val end = raw.getOrNull(i + 1)?.first ?: maxOf(totalMs, start + 4000)
            val span = minOf(end - start, 8000L).coerceAtLeast(500L)
            val parts = text.split(" ").filter { it.isNotEmpty() }
            val total = parts.sumOf { it.length + 1 }.toDouble()
            var acc = 0.0
            val words = parts.map { w ->
                val a = start + (span * acc / total).toLong()
                acc += w.length + 1
                LyricWord(w, a, start + (span * acc / total).toLong())
            }
            LyricLine(start, end, text, words)
        }
    }
}
