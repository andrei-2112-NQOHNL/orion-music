package com.andrew.music

enum class Source { YOUTUBE, SOUNDCLOUD }

data class Track(
    val title: String,
    val artist: String,
    val url: String,
    val thumbnail: String?,
    val durationSec: Long,
    val source: Source,
)
