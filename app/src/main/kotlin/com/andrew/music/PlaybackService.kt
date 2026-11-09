package com.andrew.music

import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionCommands
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** Plays audio and applies live equalizer and reverb. The app sends them as custom commands. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var player: ExoPlayer? = null
    private var eq: Equalizer? = null
    private var rev: EnvironmentalReverb? = null

    companion object {
        const val CMD_EQ = "orion.eq"
        const val CMD_REVERB = "orion.reverb"
    }

    private fun attachEffects(id: Int) {
        runCatching { eq?.release(); rev?.release() }
        eq = runCatching { Equalizer(0, id).apply { enabled = true } }.getOrNull()
        rev = runCatching { EnvironmentalReverb(0, id).apply { enabled = true; roomLevel = -9000; reverbLevel = -9000 } }.getOrNull()
    }

    override fun onCreate() {
        super.onCreate()
        val p = ExoPlayer.Builder(this).build()
        player = p
        p.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) { if (audioSessionId != 0) attachEffects(audioSessionId) }
        })
        if (p.audioSessionId != 0) attachEffects(p.audioSessionId)
        val cb = object : MediaSession.Callback {
            override fun onConnect(s: MediaSession, c: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                val cmds = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(CMD_EQ, Bundle.EMPTY)).add(SessionCommand(CMD_REVERB, Bundle.EMPTY)).build()
                return MediaSession.ConnectionResult.accept(cmds, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
            }
            override fun onCustomCommand(s: MediaSession, c: MediaSession.ControllerInfo, cmd: SessionCommand, args: Bundle): ListenableFuture<SessionResult> {
                when (cmd.customAction) {
                    CMD_EQ -> eq?.let { e ->
                        val range = e.bandLevelRange
                        val gains = args.getIntArray("gains") ?: return@let
                        for (b in 0 until e.numberOfBands) {
                            val centre = e.getCenterFreq(b.toShort()) / 1000
                            val i = AudioFx.FREQS.indices.minByOrNull { Math.abs(AudioFx.FREQS[it] - centre) } ?: 0
                            e.setBandLevel(b.toShort(), (gains[i] * 100).coerceIn(range[0].toInt(), range[1].toInt()).toShort())
                        }
                    }
                    CMD_REVERB -> rev?.let { r ->
                        val v = args.getInt("level", 0)
                        r.roomLevel = (-9000 + v * 85).toShort().coerceAtMost(0)
                        r.reverbLevel = (-9000 + v * 85).toShort().coerceAtMost(0)
                        r.decayTime = 500 + v * 30
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
        }
        session = MediaSession.Builder(this, p).setCallback(cb).build()
    }

    override fun onGetSession(info: MediaSession.ControllerInfo) = session
    override fun onDestroy() {
        runCatching { eq?.release(); rev?.release() }
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
