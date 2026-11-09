package com.andrew.music

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path

/** Controls one mpv process over its IPC socket. Live filters, volume, seeking and song time. */
class MpvPlayer(private val onTime: (Double) -> Unit, private val onEnd: () -> Unit) {
    private val sock: Path = Path.of(System.getProperty("java.io.tmpdir"), "orionmusic-mpv.sock")
    private var proc: Process? = null
    private var ch: SocketChannel? = null
    @Volatile var paused = false

    private val timeRe = Regex("\"data\":([0-9.]+)")

    fun play(url: String, title: String, fx: AudioFx) {
        stop()
        paused = false
        val af = fx.filterChain()
        val cmd = mutableListOf("mpv", "--no-video", "--really-quiet", "--input-ipc-server=$sock",
            "--force-media-title=$title", "--volume=${fx.volume}")
        if (af.isNotEmpty()) cmd.add("--af=$af")
        cmd.add(url)
        val p = ProcessBuilder(cmd).start()
        proc = p
        Thread {
            var c: SocketChannel? = null
            for (i in 0 until 60) {
                if (Files.exists(sock)) {
                    c = runCatching { SocketChannel.open(StandardProtocolFamily.UNIX).also { it.connect(UnixDomainSocketAddress.of(sock)) } }.getOrNull()
                    if (c != null) break
                }
                Thread.sleep(100)
            }
            if (c == null || proc !== p) return@Thread
            ch = c
            send("""{"command":["observe_property",1,"time-pos"]}""")
            BufferedReader(InputStreamReader(Channels.newInputStream(c))).use { r ->
                while (true) {
                    val line = runCatching { r.readLine() }.getOrNull() ?: break
                    if (line.contains("\"name\":\"time-pos\"")) timeRe.find(line)?.let { onTime(it.groupValues[1].toDouble()) }
                    else if (line.contains("\"event\":\"end-file\"") && line.contains("\"reason\":\"eof\"")) { if (proc === p) onEnd() }
                }
            }
        }.start()
    }

    @Synchronized
    private fun send(json: String) {
        runCatching { ch?.write(ByteBuffer.wrap((json + "\n").toByteArray())) }
    }

    fun applyFx(fx: AudioFx) {
        send("""{"command":["set_property","af","${fx.filterChain()}"]}""")
        send("""{"command":["set_property","volume",${fx.volume}]}""")
    }

    fun togglePause() { paused = !paused; send("""{"command":["set_property","pause",$paused]}""") }
    fun seek(sec: Double) = send("""{"command":["seek",$sec,"absolute"]}""")

    fun stop() {
        runCatching { ch?.close() }
        ch = null
        proc?.destroy()
        proc = null
        runCatching { Files.deleteIfExists(sock) }
    }
}
