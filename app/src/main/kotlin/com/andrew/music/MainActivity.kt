package com.andrew.music

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ComponentName
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import java.io.File

class MainActivity : AppCompatActivity() {
    private var controller: MediaController? = null
    private lateinit var lib: Library
    private val fx = AudioFx()
    private val results = mutableListOf<Track>()
    private var lyrics: List<LyricLine> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var nowLbl: TextView
    private lateinit var seek: SeekBar
    private lateinit var listView: ListView
    private lateinit var soundView: ScrollView
    private var tab = 0
    private var plName: String? = null
    private var shown = listOf<Track>()
    private var dragging = false
    private var lyricDialog: Dialog? = null
    private var lyricText: TextView? = null
    private lateinit var settingsView: android.view.View

    private val ACC = Color.rgb(0, 184, 212)
    private val PANEL = Color.rgb(20, 27, 45)
    private val BGC = Color.rgb(11, 15, 26)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(t: String, size: Float = 14f, c: Int = Color.WHITE) = TextView(this).apply { text = t; textSize = size; setTextColor(c) }
    private fun btn(t: String, f: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; setTextColor(ACC); setBackgroundColor(PANEL); textSize = 12f; minWidth = 0; minimumWidth = 0
        setPadding(dp(8), 0, dp(8), 0); setOnClickListener { f() }
    }

    private fun items(): List<Track> = when (tab) {
        0 -> results; 1 -> lib.queue; 2 -> lib.liked
        3 -> plName?.let { lib.playlists[it] } ?: emptyList()
        else -> emptyList()
    }

    private fun refresh() {
        shown = items().toList()
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, shown.map { "${if (lib.isLiked(it)) "♥ " else ""}${it.title} - ${it.artist}" })
    }

    private fun showTab(i: Int) {
        tab = i
        soundView.visibility = if (i == 4) android.view.View.VISIBLE else android.view.View.GONE
        settingsView.visibility = if (i == 5) android.view.View.VISIBLE else android.view.View.GONE
        listView.visibility = if (i >= 4) android.view.View.GONE else android.view.View.VISIBLE
        if (i == 3) {
            val names = lib.playlists.keys.toTypedArray()
            if (names.isEmpty()) { Toast.makeText(this, "No playlists yet. Long press a song and add one.", Toast.LENGTH_SHORT).show() }
            else AlertDialog.Builder(this).setTitle("Playlist").setItems(names) { _, w -> plName = names[w]; refresh() }.show()
        }
        refresh()
    }

    private fun slider(label: String, min: Int, max: Int, init: Int, on: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(tv(label, 13f, ACC), LinearLayout.LayoutParams(dp(64), -2))
        val s = SeekBar(this).apply {
            this.max = max - min; progress = init - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, user: Boolean) { if (user) { on(p + min); applyFx() } }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }
        row.addView(s, LinearLayout.LayoutParams(0, -2, 1f))
        return row
    }

    private fun applyFx() {
        val c = controller ?: return
        c.playbackParameters = PlaybackParameters(fx.speed.toFloat(), fx.pitch.toFloat())
        c.volume = (fx.volume / 100f).coerceIn(0f, 1f)
        c.sendCustomCommand(SessionCommand(PlaybackService.CMD_EQ, Bundle.EMPTY), Bundle().apply { putIntArray("gains", IntArray(10) { fx.bands[it].toInt() }) })
        c.sendCustomCommand(SessionCommand(PlaybackService.CMD_REVERB, Bundle.EMPTY), Bundle().apply { putInt("level", (fx.reverb * 100).toInt()) })
    }

    private fun play(t: Track) {
        status.text = "Loading..."
        nowLbl.text = "${t.title} - ${t.artist}"
        lyrics = emptyList()
        Thread {
            val r = runCatching { MusicService.streamUrl(t) }
            runOnUiThread {
                val url = r.getOrNull()
                if (url == null) { status.text = "Can't play: ${r.exceptionOrNull()?.message}"; return@runOnUiThread }
                controller?.run {
                    setMediaItem(MediaItem.Builder().setUri(url).setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).build()).build())
                    prepare(); play(); applyFx()
                }
                status.text = "Playing"
            }
        }.start()
        Thread { lyrics = runCatching { Lyrics.fetch(t) }.getOrDefault(emptyList()) }.start()
    }

    private fun lyricHtml(ms: Long): String {
        if (lyrics.isEmpty()) return "<br><br><font color='#7c8599'>No synced lyrics found</font>"
        val i = lyrics.indexOfLast { it.startMs <= ms }.coerceAtLeast(0)
        val sb = StringBuilder()
        lyrics.getOrNull(i - 1)?.let { sb.append("<font color='#4a5166'>${it.text}</font><br><br>") }
        sb.append("<big><big><b>")
        for (w in lyrics[i].words) {
            val c = if (ms >= w.endMs) "#00b8d4" else if (ms >= w.startMs) "#ffffff" else "#6b7388"
            sb.append("<font color='$c'>${w.text}</font> ")
        }
        sb.append("</b></big></big><br><br>")
        lyrics.getOrNull(i + 1)?.let { sb.append("<font color='#8a92a8'>${it.text}</font><br><br>") }
        lyrics.getOrNull(i + 2)?.let { sb.append("<font color='#4a5166'>${it.text}</font>") }
        return sb.toString()
    }

    private fun openLyrics() {
        val d = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val t = TextView(this).apply { gravity = Gravity.CENTER; textSize = 20f; setTextColor(Color.WHITE); setPadding(dp(20), dp(20), dp(20), dp(20)); setBackgroundColor(Color.BLACK) }
        t.setOnClickListener { d.dismiss() }
        d.setContentView(t)
        lyricDialog = d; lyricText = t
        d.setOnDismissListener { lyricDialog = null; lyricText = null }
        d.show()
    }

    private val ticker = object : Runnable {
        override fun run() {
            controller?.let { c ->
                if (!dragging && c.duration > 0) { seek.max = (c.duration / 1000).toInt(); seek.progress = (c.currentPosition / 1000).toInt() }
                lyricText?.let { it.text = Html.fromHtml(lyricHtml(c.currentPosition), Html.FROM_HTML_MODE_LEGACY) }
            }
            handler.postDelayed(this, 120)
        }
    }

    private fun currentSel(): Track? = lib.current()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        lib = Library(File(filesDir, "orionmusic"))
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(28), dp(12), dp(8)); setBackgroundColor(BGC) }
        val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        head.addView(GalaxyLogoView(this), LinearLayout.LayoutParams(dp(40), dp(40)))
        head.addView(tv("ORION MUSIC", 20f, ACC).apply { letterSpacing = 0.2f; setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        val updateB = btn("Update") {}
        updateB.visibility = android.view.View.GONE
        head.addView(updateB)
        status = tv("Ready", 12f, Color.GRAY); head.addView(status)
        Thread {
            val u = runCatching { Updater.check(File(filesDir, "orionmusic")) }.getOrNull()
            if (u != null) runOnUiThread {
                updateB.text = "Update " + u.version
                updateB.setOnClickListener { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u.url))) }
                updateB.visibility = android.view.View.VISIBLE
            }
        }.start()
        col.addView(head)
        val box = EditText(this).apply { hint = "Search YouTube and SoundCloud"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); isSingleLine = true; imeOptions = EditorInfo.IME_ACTION_SEARCH }
        col.addView(box)

        val tabs = LinearLayout(this)
        listOf("Search", "Queue", "Liked", "Playlists", "Sound", "Settings").forEachIndexed { i, n -> tabs.addView(btn(n) { showTab(i) }, LinearLayout.LayoutParams(0, -2, 1f)) }
        col.addView(tabs)

        listView = ListView(this)
        soundView = ScrollView(this).apply { visibility = android.view.View.GONE }
        val sc = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(4), dp(8), dp(4), dp(8)) }
        sc.addView(slider("Speed", 50, 200, 100) { fx.speed = it / 100.0 })
        sc.addView(slider("Pitch", 50, 200, 100) { fx.pitch = it / 100.0 })
        sc.addView(slider("Reverb", 0, 100, 0) { fx.reverb = it / 100.0 })
        sc.addView(slider("Volume", 0, 100, 100) { fx.volume = it })
        sc.addView(tv("Equalizer", 14f, ACC).apply { setPadding(0, dp(12), 0, dp(4)) })
        for (i in 0 until 10) sc.addView(slider(AudioFx.FREQS[i].let { if (it >= 1000) "${it / 1000}k" else "$it" }, -12, 12, 0) { fx.bands[i] = it.toDouble() })
        soundView.addView(sc)
        val st = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(12), dp(8), dp(12)) }
        st.addView(tv("Settings", 16f, ACC))
        st.addView(tv("Version " + Updater.CURRENT, 14f).apply { setPadding(0, dp(8), 0, dp(8)) })
        val upStatus = tv("Tap to check for updates", 13f, Color.GRAY).apply { setPadding(0, dp(8), 0, dp(8)) }
        val dlB = btn("Download update") {}
        dlB.visibility = android.view.View.GONE
        val checkB = btn("Check for updates") {}
        checkB.setOnClickListener {
            upStatus.text = "Checking..."
            Thread {
                val r = runCatching { Updater.check(File(filesDir, "orionmusic")) }
                runOnUiThread {
                    val up = r.getOrNull()
                    if (r.isFailure) upStatus.text = "Check failed. Are you online?"
                    else if (up == null) upStatus.text = "You are up to date"
                    else {
                        upStatus.text = "Version " + up.version + " is available. " + up.notes
                        dlB.visibility = android.view.View.VISIBLE
                        dlB.setOnClickListener { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(up.url))) }
                    }
                }
            }.start()
        }
        st.addView(checkB); st.addView(upStatus); st.addView(dlB)
        settingsView = ScrollView(this).apply { visibility = android.view.View.GONE; addView(st) }
        val frame = FrameLayout(this); frame.addView(listView); frame.addView(soundView); frame.addView(settingsView)
        col.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))

        nowLbl = tv("Nothing playing", 14f); col.addView(nowLbl)
        seek = SeekBar(this).apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, user: Boolean) {}
                override fun onStartTrackingTouch(sb: SeekBar) { dragging = true }
                override fun onStopTrackingTouch(sb: SeekBar) { dragging = false; controller?.seekTo(sb.progress * 1000L) }
            })
        }
        col.addView(seek)

        val shuffleB = btn("Shuffle off") {}
        shuffleB.setOnClickListener { lib.shuffle = !lib.shuffle; shuffleB.text = "Shuffle ${if (lib.shuffle) "on" else "off"}" }
        val repeatB = btn("Repeat off") {}
        repeatB.setOnClickListener { lib.repeat = RepeatMode.values()[(lib.repeat.ordinal + 1) % 3]; repeatB.text = "Repeat ${lib.repeat.name.lowercase()}" }
        val r1 = LinearLayout(this)
        r1.addView(btn("Prev") { lib.previous()?.let { play(it); refresh() } }, LinearLayout.LayoutParams(0, -2, 1f))
        r1.addView(btn("Pause/Play") { controller?.let { if (it.isPlaying) it.pause() else it.play() } }, LinearLayout.LayoutParams(0, -2, 1f))
        r1.addView(btn("Next") { lib.next()?.let { play(it); refresh() } }, LinearLayout.LayoutParams(0, -2, 1f))
        r1.addView(btn("Lyrics") { openLyrics() }, LinearLayout.LayoutParams(0, -2, 1f))
        val r2 = LinearLayout(this)
        r2.addView(shuffleB, LinearLayout.LayoutParams(0, -2, 1f)); r2.addView(repeatB, LinearLayout.LayoutParams(0, -2, 1f))
        r2.addView(btn("♥ Like") { currentSel()?.let { lib.toggleLike(it); refresh() } }, LinearLayout.LayoutParams(0, -2, 1f))
        r2.addView(btn("Stop") { controller?.stop() }, LinearLayout.LayoutParams(0, -2, 1f))
        col.addView(r1); col.addView(r2)
        setContentView(col)

        box.setOnEditorActionListener { _, _, _ ->
            val q = box.text.toString().trim()
            if (q.isNotEmpty()) {
                status.text = "Searching..."
                Thread {
                    val r = runCatching { MusicService.search(q) }
                    runOnUiThread {
                        results.clear(); r.getOrNull()?.let { results.addAll(it) }
                        status.text = if (r.isFailure) "Search failed" else "${results.size} results"
                        showTab(0)
                    }
                }.start()
            }
            true
        }
        listView.setOnItemClickListener { _, _, i, _ ->
            val t = shown.getOrNull(i) ?: return@setOnItemClickListener
            if (tab == 1) lib.index = i else lib.playNow(t)
            play(t); refresh()
        }
        listView.setOnItemLongClickListener { _, _, i, _ ->
            val t = shown.getOrNull(i) ?: return@setOnItemLongClickListener true
            AlertDialog.Builder(this).setTitle(t.title).setItems(arrayOf("Add to queue", "Like / unlike", "Add to playlist")) { _, w ->
                when (w) {
                    0 -> { lib.enqueue(t); refresh() }
                    1 -> { lib.toggleLike(t); refresh() }
                    2 -> {
                        val input = EditText(this).apply { hint = "Playlist name" }
                        AlertDialog.Builder(this).setTitle("Add to playlist").setView(input)
                            .setPositiveButton("Add") { _, _ -> input.text.toString().trim().takeIf { it.isNotEmpty() }?.let { lib.addToPlaylist(it, t); plName = it } }
                            .setNegativeButton("Cancel", null).show()
                    }
                }
            }.show()
            true
        }

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val f = MediaController.Builder(this, token).buildAsync()
        f.addListener({
            controller = f.get().also { c ->
                c.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) lib.next()?.let { play(it); refresh() }
                    }
                })
            }
        }, ContextCompat.getMainExecutor(this))
        handler.post(ticker)
        refresh()
    }

    override fun onDestroy() { handler.removeCallbacks(ticker); super.onDestroy() }
}
