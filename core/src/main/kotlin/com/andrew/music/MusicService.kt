package com.andrew.music

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Shared by the Android and desktop apps. No ads: streams come straight from the sources. */
object MusicService {
    private var ready = false
    private fun init() { if (!ready) { NewPipe.init(HttpDownloader()); ready = true } }

    fun search(query: String): List<Track> {
        init()
        val out = mutableListOf<Track>()
        val yt = runCatching {
            ServiceList.YouTube.getSearchExtractor(query, listOf("music_songs"), "").also { it.fetchPage() }
                .initialPage.items.filterIsInstance<StreamInfoItem>().map { it.toTrack(Source.YOUTUBE) }
        }.getOrDefault(emptyList())
        val sc = runCatching {
            ServiceList.SoundCloud.getSearchExtractor(query, listOf("tracks"), "").also { it.fetchPage() }
                .initialPage.items.filterIsInstance<StreamInfoItem>().map { it.toTrack(Source.SOUNDCLOUD) }
        }.getOrDefault(emptyList())
        val n = maxOf(yt.size, sc.size)
        for (i in 0 until n) { yt.getOrNull(i)?.let(out::add); sc.getOrNull(i)?.let(out::add) }
        return out
    }

    /** Direct audio URL for playback. */
    fun streamUrl(track: Track): String {
        init()
        val service = if (track.source == Source.YOUTUBE) ServiceList.YouTube else ServiceList.SoundCloud
        val info = StreamInfo.getInfo(service, track.url)
        return info.audioStreams.maxByOrNull { it.averageBitrate }?.content
            ?: error("No audio stream found")
    }

    private fun StreamInfoItem.toTrack(s: Source) = Track(
        title = name, artist = uploaderName ?: "", url = url,
        thumbnail = thumbnails.firstOrNull()?.url, durationSec = duration, source = s,
    )
}
