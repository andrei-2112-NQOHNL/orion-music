package com.andrew.music

import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

private val BG = Color(0x0B0F1A)
private val PANEL = Color(0x141B2D)
private val ACC = Color(0x00B8D4)
private val DIM = Color(0x7C8599)

private fun <T : JComponent> T.dark(): T { background = PANEL; foreground = Color.WHITE; return this }

private fun btn(t: String, f: () -> Unit) = JButton(t).apply {
    background = PANEL; foreground = ACC; isFocusPainted = false
    border = BorderFactory.createLineBorder(ACC.darker())
    addActionListener { f() }
}

private fun fmt(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

private class TrackList(val items: () -> List<Track>) : JScrollPane() {
    val model = DefaultListModel<String>()
    val list = JList(model).dark()
    var cur = listOf<Track>()
    init { setViewportView(list); border = BorderFactory.createEmptyBorder(); refresh() }
    fun refresh() {
        cur = items(); model.clear()
        cur.forEach { model.addElement("${it.title} - ${it.artist}  [${if (it.source == Source.YOUTUBE) "YT" else "SC"}]") }
    }
    fun selected(): Track? = cur.getOrNull(list.selectedIndex)
}

fun main() {
    SwingUtilities.invokeLater {
        val lib = Library(File(System.getProperty("user.home"), ".config/orionmusic"))
        val fx = AudioFx()
        val results = mutableListOf<Track>()
        var lyrics: List<LyricLine> = emptyList()
        var dragging = false

        val status = JLabel("Ready").dark().apply { foreground = DIM }
        val nowLbl = JLabel("Nothing playing").dark().apply { font = font.deriveFont(Font.BOLD, 14f) }
        val timeLbl = JLabel("0:00").dark()
        val seek = JSlider(0, 100, 0).dark()

        // lyrics fullscreen window
        val lyricLbl = JLabel("", SwingConstants.CENTER).apply { foreground = Color.WHITE; verticalAlignment = SwingConstants.CENTER }
        val lyricWin = JFrame("Orion Lyrics").apply {
            isUndecorated = true; contentPane.background = Color.BLACK
            contentPane.add(lyricLbl)
            addKeyListener(object : KeyAdapter() { override fun keyPressed(e: KeyEvent) { if (e.keyCode == KeyEvent.VK_ESCAPE) isVisible = false } })
            addMouseListener(object : MouseAdapter() { override fun mouseClicked(e: MouseEvent) { isVisible = false } })
            extendedState = Frame.MAXIMIZED_BOTH
        }
        fun hex(c: Color) = "#%02x%02x%02x".format(c.red, c.green, c.blue)
        fun showLyrics(ms: Long) {
            if (lyrics.isEmpty()) { lyricLbl.text = "<html><div style='text-align:center;font-size:28px;color:#7c8599'>No synced lyrics found</div></html>"; return }
            var i = lyrics.indexOfLast { it.startMs <= ms }
            if (i < 0) i = 0
            val sb = StringBuilder("<html><div style='text-align:center;font-family:sans-serif'>")
            fun side(l: LyricLine?, op: String) { if (l != null) sb.append("<div style='font-size:24px;color:$op;margin:10px'>${l.text}</div>") }
            side(lyrics.getOrNull(i - 1), "#4a5166")
            sb.append("<div style='font-size:44px;font-weight:bold;margin:18px'>")
            for (w in lyrics[i].words) {
                val c = if (ms >= w.endMs) hex(ACC) else if (ms >= w.startMs) "#ffffff" else "#6b7388"
                sb.append("<span style='color:$c'>${w.text}</span> ")
            }
            sb.append("</div>")
            side(lyrics.getOrNull(i + 1), "#8a92a8"); side(lyrics.getOrNull(i + 2), "#4a5166")
            sb.append("</div></html>")
            lyricLbl.text = sb.toString()
        }

        lateinit var play: (Track) -> Unit
        lateinit var refreshAll: () -> Unit
        val mpv = MpvPlayer(
            onTime = { s -> SwingUtilities.invokeLater {
                if (!dragging) seek.value = s.toInt()
                timeLbl.text = fmt(s.toInt())
                if (lyricWin.isVisible) showLyrics((s * 1000).toLong())
            } },
            onEnd = { SwingUtilities.invokeLater { lib.next()?.let { play(it) } } },
        )

        play = { t ->
            status.text = "Loading ${t.title}..."
            nowLbl.text = "${t.title} - ${t.artist}"
            seek.maximum = maxOf(1, t.durationSec.toInt()); seek.value = 0
            lyrics = emptyList()
            Thread {
                val r = runCatching { MusicService.streamUrl(t) }
                SwingUtilities.invokeLater {
                    val url = r.getOrNull()
                    if (url == null) { status.text = "Can't play: ${r.exceptionOrNull()?.message}"; return@invokeLater }
                    runCatching { mpv.play(url, t.title, fx); status.text = "Playing" }
                        .onFailure { status.text = "Install mpv to play audio" }
                }
            }.start()
            Thread { lyrics = runCatching { Lyrics.fetch(t) }.getOrDefault(emptyList()) }.start()
        }

        val searchTab = TrackList { results }
        val queueTab = TrackList { lib.queue }
        val likedTab = TrackList { lib.liked }
        val plCombo = JComboBox<String>().dark()
        val plList = TrackList { lib.playlists[plCombo.selectedItem as? String] ?: emptyList() }
        refreshAll = {
            searchTab.refresh(); queueTab.refresh(); likedTab.refresh()
            val sel = plCombo.selectedItem
            plCombo.removeAllItems(); lib.playlists.keys.forEach { plCombo.addItem(it) }
            if (sel != null) plCombo.selectedItem = sel
            plList.refresh()
        }
        plCombo.addActionListener { plList.refresh() }

        val tabs = listOf("Search" to searchTab, "Queue" to queueTab, "Liked" to likedTab)
        for ((_, tl) in tabs) tl.list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) tl.selected()?.let { t ->
                    if (tl === queueTab) lib.index = tl.list.selectedIndex else lib.playNow(t)
                    play(t); refreshAll()
                }
            }
        })
        plList.list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) { if (e.clickCount == 2) plList.selected()?.let { lib.playNow(it); play(it); refreshAll() } }
        })

        fun currentSel(tp: JTabbedPane): Track? = when (tp.selectedIndex) {
            0 -> searchTab.selected(); 1 -> queueTab.selected(); 2 -> likedTab.selected(); 3 -> plList.selected(); else -> lib.current()
        }

        // sound tab
        fun slider(label: String, min: Int, max: Int, init: Int, vertical: Boolean, on: (Int) -> Unit): JPanel {
            val s = JSlider(if (vertical) JSlider.VERTICAL else JSlider.HORIZONTAL, min, max, init).dark()
            val l = JLabel(label, SwingConstants.CENTER).dark().apply { foreground = ACC }
            s.addChangeListener { on(s.value); mpv.applyFx(fx) }
            s.addMouseWheelListener { e -> s.value -= e.wheelRotation * maxOf(1, (max - min) / 40) }
            return JPanel(BorderLayout()).dark().apply { add(l, if (vertical) BorderLayout.SOUTH else BorderLayout.WEST); add(s) }
        }
        val soundTab = JPanel(BorderLayout(8, 8)).dark()
        val knobs = JPanel(GridLayout(0, 1, 4, 4)).dark()
        knobs.add(slider("Speed  ", 50, 200, 100, false) { fx.speed = it / 100.0 })
        knobs.add(slider("Pitch  ", 50, 200, 100, false) { fx.pitch = it / 100.0 })
        knobs.add(slider("Reverb ", 0, 100, 0, false) { fx.reverb = it / 100.0 })
        knobs.add(slider("Volume ", 0, 130, 100, false) { fx.volume = it })
        val eq = JPanel(GridLayout(1, 10, 2, 2)).dark()
        for (i in 0 until 10) eq.add(slider(AudioFx.FREQS[i].let { if (it >= 1000) "${it / 1000}k" else "$it" }, -12, 12, 0, true) { fx.bands[i] = it.toDouble() })
        soundTab.add(knobs, BorderLayout.NORTH); soundTab.add(eq, BorderLayout.CENTER)

        val tp = JTabbedPane().dark()
        tp.addTab("Search", searchTab); tp.addTab("Queue", queueTab); tp.addTab("Liked", likedTab)
        tp.addTab("Playlists", JPanel(BorderLayout()).dark().apply { add(plCombo, BorderLayout.NORTH); add(plList) })
        tp.addTab("Sound", soundTab)

        val box = JTextField().dark().apply { caretColor = ACC }
        box.addActionListener {
            val q = box.text.trim(); if (q.isEmpty()) return@addActionListener
            status.text = "Searching..."
            Thread {
                val r = runCatching { MusicService.search(q) }
                SwingUtilities.invokeLater {
                    results.clear(); r.getOrNull()?.let { results.addAll(it) }
                    status.text = if (r.isFailure) "Search failed: ${r.exceptionOrNull()?.message}" else "${results.size} results"
                    tp.selectedIndex = 0; refreshAll()
                }
            }.start()
        }

        val shuffleB = btn("Shuffle: off") {}
        shuffleB.addActionListener { lib.shuffle = !lib.shuffle; shuffleB.text = "Shuffle: ${if (lib.shuffle) "on" else "off"}" }
        val repeatB = btn("Repeat: off") {}
        repeatB.addActionListener {
            lib.repeat = RepeatMode.values()[(lib.repeat.ordinal + 1) % 3]
            repeatB.text = "Repeat: ${lib.repeat.name.lowercase()}"
        }
        val bar = JPanel(FlowLayout(FlowLayout.CENTER, 6, 4)).dark()
        bar.add(btn("Prev") { lib.previous()?.let { play(it); refreshAll() } })
        bar.add(btn("Pause/Play") { mpv.togglePause() })
        bar.add(btn("Next") { lib.next()?.let { play(it); refreshAll() } })
        bar.add(shuffleB); bar.add(repeatB)
        bar.add(btn("Like") { (currentSel(tp) ?: lib.current())?.let { lib.toggleLike(it); refreshAll() } })
        bar.add(btn("+ Queue") { currentSel(tp)?.let { lib.enqueue(it); refreshAll() } })
        bar.add(btn("+ Playlist") {
            currentSel(tp)?.let { t ->
                val name = JOptionPane.showInputDialog(null, "Playlist name:", "Add to playlist", JOptionPane.PLAIN_MESSAGE, null, lib.playlists.keys.toTypedArray(), null) as? String
                if (!name.isNullOrBlank()) { lib.addToPlaylist(name.trim(), t); refreshAll() }
            }
        })
        bar.add(btn("Lyrics") { lyricWin.isVisible = true; lyricWin.requestFocus() })
        bar.add(btn("Stop") { mpv.stop(); status.text = "Stopped" })

        seek.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { dragging = true }
            override fun mouseReleased(e: MouseEvent) { dragging = false; mpv.seek(seek.value.toDouble()) }
        })

        val updateB = btn("Update available") {}
        updateB.isVisible = false
        Thread {
            val u = runCatching { Updater.check(File(System.getProperty("user.home"), ".config/orionmusic")) }.getOrNull()
            if (u != null) SwingUtilities.invokeLater {
                updateB.text = "Update to ${u.version}"
                updateB.toolTipText = u.notes
                updateB.addActionListener { runCatching { Desktop.getDesktop().browse(java.net.URI(u.url)) } }
                updateB.isVisible = true
            }
        }.start()
        val head = JPanel(BorderLayout(10, 4)).dark().apply {
            add(GalaxyLogo(44), BorderLayout.WEST)
            add(JLabel("ORION MUSIC").dark().apply { font = Font(Font.SANS_SERIF, Font.BOLD, 20); foreground = ACC }, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).dark().apply { add(updateB); add(status) }, BorderLayout.EAST)
        }
        val north = JPanel(BorderLayout(4, 6)).dark().apply { add(head, BorderLayout.NORTH); add(box, BorderLayout.SOUTH) }
        val seekRow = JPanel(BorderLayout(6, 0)).dark().apply { add(seek); add(timeLbl, BorderLayout.EAST) }
        val south = JPanel(GridLayout(0, 1)).dark().apply { add(nowLbl); add(seekRow); add(bar) }

        JFrame("Orion Music").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            addWindowListener(object : java.awt.event.WindowAdapter() { override fun windowClosing(e: java.awt.event.WindowEvent) { mpv.stop() } })
            contentPane.background = PANEL
            contentPane.layout = BorderLayout(8, 8)
            (contentPane as JComponent).border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            contentPane.add(north, BorderLayout.NORTH); contentPane.add(tp, BorderLayout.CENTER); contentPane.add(south, BorderLayout.SOUTH)
            setSize(820, 700); setLocationRelativeTo(null); isVisible = true
        }
    }
}
