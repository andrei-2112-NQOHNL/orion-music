package com.andrew.music

import java.io.File

enum class RepeatMode { OFF, ALL, ONE }

/** Shared queue, shuffle, repeat, likes and playlists. Saved as simple tab separated lines. */
class Library(private val dir: File) {
    val queue = mutableListOf<Track>()
    var index = -1
    var shuffle = false
    var repeat = RepeatMode.OFF
    val liked = mutableListOf<Track>()
    val playlists = linkedMapOf<String, MutableList<Track>>()

    init { dir.mkdirs(); load() }

    fun current(): Track? = queue.getOrNull(index)
    fun enqueue(t: Track) { queue.add(t) }
    fun playNow(t: Track) { queue.add(t); index = queue.size - 1 }

    fun next(): Track? {
        if (queue.isEmpty()) return null
        if (repeat == RepeatMode.ONE) return current()
        index = if (shuffle) queue.indices.random() else index + 1
        if (index >= queue.size) { if (repeat == RepeatMode.ALL) index = 0 else { index = queue.size - 1; return null } }
        return current()
    }

    fun previous(): Track? {
        if (queue.isEmpty()) return null
        index = if (index <= 0) 0 else index - 1
        return current()
    }

    fun isLiked(t: Track) = liked.any { it.url == t.url }
    fun toggleLike(t: Track) { if (!liked.removeAll { it.url == t.url }) liked.add(t); save() }
    fun addToPlaylist(name: String, t: Track) { playlists.getOrPut(name) { mutableListOf() }.add(t); save() }
    fun removePlaylist(name: String) { playlists.remove(name); save() }

    private fun enc(t: Track) = listOf(t.title, t.artist, t.url, t.thumbnail ?: "", t.durationSec.toString(), t.source.name)
        .joinToString("\t") { it.replace("\t", " ").replace("\n", " ") }
    private fun dec(s: String): Track? {
        val p = s.split("\t"); if (p.size < 6) return null
        return Track(p[0], p[1], p[2], p[3].ifEmpty { null }, p[4].toLongOrNull() ?: 0, runCatching { Source.valueOf(p[5]) }.getOrDefault(Source.YOUTUBE))
    }

    fun save() {
        File(dir, "liked.tsv").writeText(liked.joinToString("\n") { enc(it) })
        File(dir, "playlists.tsv").writeText(playlists.entries.joinToString("\n") { (n, l) -> l.joinToString("\n") { "$n\t${enc(it)}" } })
    }

    private fun load() {
        File(dir, "liked.tsv").takeIf { it.exists() }?.readLines()?.mapNotNull { dec(it) }?.let { liked.addAll(it) }
        File(dir, "playlists.tsv").takeIf { it.exists() }?.readLines()?.forEach { l ->
            val i = l.indexOf('\t'); if (i < 0) return@forEach
            dec(l.substring(i + 1))?.let { playlists.getOrPut(l.substring(0, i)) { mutableListOf() }.add(it) }
        }
    }
}
