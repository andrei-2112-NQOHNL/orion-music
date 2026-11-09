package com.andrew.music

import java.awt.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

private val BG = Color(0x11111B)
private val PANEL = Color(0x20212C)
private val ACC = Color(0x89B4FA)
private val DIM = Color(0x787D92)

private fun <T : JComponent> T.dark(): T { background = PANEL; foreground = Color.WHITE; if (this is JPanel || this is JLabel) isOpaque = false; return this }

private class FadeTabs : JTabbedPane() {
    private var a = 1f
    init {
        addChangeListener {
            a = 0f
            javax.swing.Timer(16, null).apply {
                addActionListener { a += 0.09f; if (a >= 1f) { a = 1f; stop() }; repaint() }
                start()
            }
        }
    }
    override fun paint(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.composite = AlphaComposite.SrcOver.derive(a.coerceIn(0f, 1f))
        super.paint(g2)
        g2.dispose()
    }
}

private class ShimmerTitle(val txt: String) : JComponent() {
    private var t = 0f
    init {
        font = Font(Font.SANS_SERIF, Font.BOLD, 22)
        javax.swing.Timer(16) { if (isShowing) { t += 0.0064f; repaint() } }.start()
    }
    override fun getPreferredSize() = Dimension(240, 36)
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.font = font
        val w = width.toFloat()
        val off = (t % 1f) * w * 2f - w
        g2.paint = java.awt.LinearGradientPaint(
            java.awt.geom.Point2D.Float(off, 0f), java.awt.geom.Point2D.Float(off + w, 0f),
            floatArrayOf(0f, 0.5f, 1f), arrayOf(ACC, Color.WHITE, Color(0xCBA6F7)),
            java.awt.MultipleGradientPaint.CycleMethod.REPEAT)
        val fm = g2.fontMetrics
        g2.drawString(txt, 0, (height + fm.ascent - fm.descent) / 2)
        g2.dispose()
    }
}

private class NowBars : JComponent() {
    private var t = 0.0
    init { javax.swing.Timer(60) { if (isShowing) { t += 0.25; repaint() } }.start() }
    override fun getPreferredSize() = Dimension(44, 28)
    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val n = 6
        val bw = width / n
        for (i in 0 until n) {
            val h = ((0.35 + 0.65 * Math.abs(Math.sin(t * (0.6 + i * 0.17) + i * 1.3))) * height).toInt()
            g2.paint = GradientPaint(0f, (height - h).toFloat(), Color(0xCBA6F7), 0f, height.toFloat(), ACC)
            g2.fillRoundRect(i * bw + 1, height - h, bw - 3, h, 4, 4)
        }
    }
}

private class NowTitle : JLabel("Nothing playing") {
    private var t = 0f
    init {
        font = Font(Font.SANS_SERIF, Font.BOLD, 15); foreground = Color.WHITE; isOpaque = false
        javax.swing.Timer(16) { if (isShowing) { t += 0.01f; repaint() } }.start()
    }
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val w = width.toFloat()
        val x = (t % 1f) * w * 1.6f - w * 0.3f
        g2.paint = java.awt.LinearGradientPaint(x, 0f, x + w * 0.4f, 0f, floatArrayOf(0f, 0.5f, 1f), arrayOf(Color(ACC.red, ACC.green, ACC.blue, 0), Color(ACC.red, ACC.green, ACC.blue, 200), Color(ACC.red, ACC.green, ACC.blue, 0)))
        g2.fillRect(0, height - 3, width, 2)
        g2.dispose()
        super.paintComponent(g)
    }
}

private class PulseRing(val inner: JComponent) : JPanel(BorderLayout()) {
    private var t = 0f
    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        add(inner)
        javax.swing.Timer(16) { if (isShowing) { t += 0.0167f; repaint() } }.start()
    }
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val k = (0.5f + 0.5f * Math.sin(t.toDouble()).toFloat())
        g2.color = Color(ACC.red, ACC.green, ACC.blue, (30 + 80 * k).toInt())
        g2.stroke = BasicStroke(2f)
        val m = (1 + 2 * k).toInt()
        g2.drawOval(m, m, width - 2 * m - 1, height - 2 * m - 1)
        g2.dispose()
    }
}

private class RowRenderer : DefaultListCellRenderer() {
    private var sel = false
    init { isOpaque = false; border = BorderFactory.createEmptyBorder(9, 14, 9, 14); font = font.deriveFont(14f) }
    override fun getListCellRendererComponent(l: JList<*>, v: Any?, i: Int, s: Boolean, f: Boolean): Component {
        super.getListCellRendererComponent(l, v, i, s, f)
        sel = s; isOpaque = false
        foreground = if (s) BG else Color.WHITE
        return this
    }
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        if (sel) {
            g2.paint = GradientPaint(0f, 0f, ACC, width.toFloat(), 0f, Color(0xCBA6F7))
            g2.fillRoundRect(2, 1, width - 4, height - 2, 18, 18)
        } else {
            g2.color = Color(255, 255, 255, 10)
            g2.fillRoundRect(2, 1, width - 4, height - 2, 18, 18)
        }
        g2.dispose()
        super.paintComponent(g)
    }
}

private class Burst : JComponent() {
    private var t = 1f
    private val tm = javax.swing.Timer(25, null)
    init {
        isOpaque = false
        tm.addActionListener { t += 0.05f; if (t >= 1f) { t = 1f; tm.stop() }; repaint() }
    }
    fun fire() { t = 0f; tm.start() }
    override fun contains(x: Int, y: Int) = false
    override fun paintComponent(g: Graphics) {
        if (t >= 1f) return
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val cx = width / 2f; val cy = height / 2f
        for (i in 0 until 10) {
            val a = i * Math.PI / 5
            val d = t * 40f
            val al = ((1f - t) * 220).toInt().coerceIn(0, 255)
            g2.color = Color(0xF5, 0xC2, 0xE7, al)
            val r = (6 * (1f - t)).toInt() + 2
            g2.fillOval((cx + Math.cos(a) * d).toInt() - r / 2, (cy + Math.sin(a) * d).toInt() - r / 2, r, r)
        }
        g2.dispose()
    }
}

private class GlowSlider(min: Int, max: Int, v: Int, val vert: Boolean = false) : JSlider(if (vert) VERTICAL else HORIZONTAL, min, max, v) {
    init { isOpaque = false; cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) }
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val pad = 10
        val frac = if (maximum > minimum) (value - minimum).toFloat() / (maximum - minimum) else 0f
        val track = Color(255, 255, 255, 30)
        val halo = Color(ACC.red, ACC.green, ACC.blue, 70)
        if (!vert) {
            val w = width - pad * 2
            val cy = height / 2
            val x = pad + (w * frac).toInt()
            g2.color = track; g2.fillRoundRect(pad, cy - 3, w, 6, 6, 6)
            if (x > pad) {
                g2.paint = GradientPaint(pad.toFloat(), 0f, ACC, x.toFloat() + 1f, 0f, Color(0xCBA6F7))
                g2.fillRoundRect(pad, cy - 3, x - pad, 6, 6, 6)
            }
            g2.color = halo; g2.fillOval(x - 10, cy - 10, 20, 20)
            g2.color = Color.WHITE; g2.fillOval(x - 6, cy - 6, 12, 12)
        } else {
            val h = height - pad * 2
            val cx = width / 2
            val y = pad + (h * (1f - frac)).toInt()
            g2.color = track; g2.fillRoundRect(cx - 3, pad, 6, h, 6, 6)
            if (y < pad + h) {
                g2.paint = GradientPaint(0f, y.toFloat(), Color(0xCBA6F7), 0f, (pad + h).toFloat() + 1f, ACC)
                g2.fillRoundRect(cx - 3, y, 6, pad + h - y, 6, 6)
            }
            g2.color = halo; g2.fillOval(cx - 10, y - 10, 20, 20)
            g2.color = Color.WHITE; g2.fillOval(cx - 6, y - 6, 12, 12)
        }
        g2.dispose()
    }
}

private class WaveStrip : JComponent() {
    private var t = 0.0
    init { isOpaque = false; javax.swing.Timer(50) { if (isShowing) { t += 0.12; repaint() } }.start() }
    override fun getPreferredSize() = Dimension(100, 34)
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val cols = arrayOf(ACC, Color(0xB4BEFE), Color(0xCBA6F7))
        for (k in 0 until 3) {
            val path = java.awt.geom.Path2D.Float()
            var x = 0
            while (x <= width) {
                val y = height / 2.0 + Math.sin(x * 0.02 * (1 + k * 0.4) + t * (1 + k * 0.5)) * (height * (0.30 - k * 0.07))
                if (x == 0) path.moveTo(x.toDouble(), y) else path.lineTo(x.toDouble(), y)
                x += 6
            }
            val c = cols[k]
            g2.color = Color(c.red, c.green, c.blue, 150 - k * 30)
            g2.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2.draw(path)
        }
        g2.dispose()
    }
}

private class GlassCard(val inner: JComponent) : JPanel(BorderLayout()) {
    private var t = 0f
    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
        add(inner)
        javax.swing.Timer(16) { if (isShowing) { t += 0.0125f; repaint() } }.start()
    }
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val k = 0.5f + 0.5f * Math.sin(t.toDouble()).toFloat()
        g2.color = Color(255, 255, 255, 14)
        g2.fillRoundRect(0, 0, width - 1, height - 1, 26, 26)
        g2.paint = GradientPaint(0f, 0f, Color(ACC.red, ACC.green, ACC.blue, (50 + 110 * k).toInt()), width.toFloat(), height.toFloat(), Color(0xCB, 0xA6, 0xF7, (160 - 110 * k).toInt()))
        g2.stroke = BasicStroke(1.6f)
        g2.drawRoundRect(0, 0, width - 1, height - 1, 26, 26)
        g2.dispose()
    }
}

private class Vinyl : JComponent() {
    private var t = 0.0
    init { isOpaque = false; javax.swing.Timer(16) { if (isShowing) { t += 0.032; repaint() } }.start() }
    override fun getPreferredSize() = Dimension(46, 46)
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val d = minOf(width, height) - 6
        val cx = width / 2.0; val cy = height / 2.0
        g2.color = Color(ACC.red, ACC.green, ACC.blue, 60)
        g2.fillOval((cx - d / 2 - 3).toInt(), (cy - d / 2 - 3).toInt(), d + 6, d + 6)
        g2.color = Color(0x14, 0x14, 0x1E)
        g2.fillOval((cx - d / 2).toInt(), (cy - d / 2).toInt(), d, d)
        g2.stroke = BasicStroke(1f)
        for (r in 1..3) {
            g2.color = Color(255, 255, 255, 25)
            val rr = d / 2 - r * 4
            g2.drawOval((cx - rr).toInt(), (cy - rr).toInt(), rr * 2, rr * 2)
        }
        g2.stroke = BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g2.paint = GradientPaint(0f, 0f, ACC, width.toFloat(), height.toFloat(), Color(0xCBA6F7))
        val rr = d / 2 - 2
        g2.drawArc((cx - rr).toInt(), (cy - rr).toInt(), rr * 2, rr * 2, Math.toDegrees(-t).toInt(), 70)
        g2.drawArc((cx - rr).toInt(), (cy - rr).toInt(), rr * 2, rr * 2, Math.toDegrees(-t).toInt() + 180, 70)
        g2.paint = GradientPaint(0f, 0f, ACC, width.toFloat(), height.toFloat(), Color(0xF5C2E7))
        g2.fillOval((cx - 7).toInt(), (cy - 7).toInt(), 14, 14)
        g2.dispose()
    }
}

private class GlowPanel : JPanel() {
    private var t = 0.0
    private val rnd = java.util.Random(7)
    private val sx = DoubleArray(70) { rnd.nextDouble() }
    private val sy = DoubleArray(70) { rnd.nextDouble() }
    private val sp = DoubleArray(70) { 0.02 + rnd.nextDouble() * 0.06 }
    private val lo = java.awt.image.BufferedImage(360, 236, java.awt.image.BufferedImage.TYPE_INT_RGB)
    init { isOpaque = true; javax.swing.Timer(16) { if (isShowing) { t += 0.011; repaint() } }.start() }
    override fun paintComponent(g: Graphics) {
        val q = lo.createGraphics()
        q.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF)
        q.color = BG; q.fillRect(0, 0, lo.width, lo.height)
        val blobs = arrayOf(Triple(0.2, 0.3, ACC), Triple(0.8, 0.6, Color(0xB4BEFE)), Triple(0.5, 0.9, Color(0xCBA6F7)), Triple(0.85, 0.1, Color(0xF5C2E7)), Triple(0.1, 0.85, Color(0x94E2D5)), Triple(0.6, 0.35, Color(0x89DCEB)))
        for ((i, b) in blobs.withIndex()) {
            val cx = (lo.width * (b.first + 0.2 * Math.sin(t * (0.7 + i * 0.3) + i * 2))).toFloat()
            val cy = (lo.height * (b.second + 0.15 * Math.cos(t * (0.5 + i * 0.25) + i))).toFloat()
            val c = b.third
            q.paint = RadialGradientPaint(cx, cy, lo.width * 0.6f, floatArrayOf(0f, 1f), arrayOf(Color(c.red, c.green, c.blue, 85), Color(c.red, c.green, c.blue, 0)))
            q.fillRect(0, 0, lo.width, lo.height)
        }
        q.dispose()
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(lo, 0, 0, width, height, null)
        for (i in sx.indices) {
            val x = ((sx[i] + t * sp[i] * 0.3) % 1.0) * width
            val y = ((sy[i] - t * sp[i] * 0.15 + 1.0) % 1.0) * height
            val a = (60 + 150 * (0.5 + 0.5 * Math.sin(t * 3 * (1 + sp[i] * 10) + i))).toInt().coerceIn(0, 255)
            g2.color = Color(255, 255, 255, a)
            val r = if (i % 9 == 0) 3 else 2
            g2.fillOval(x.toInt(), y.toInt(), r, r)
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        for (k in 0 until 3) {
            val ph = (t * 0.22 + k * 0.37) % 1.0
            if (ph > 0.22) continue
            val p = ph / 0.22
            val x1 = width * (0.15 + 0.7 * ((k * 0.53) % 1.0)) + p * width * 0.25
            val y1 = height * 0.05 + p * height * 0.3 + k * 20
            val x0 = x1 - 90; val y0 = y1 - 40
            val al = (255 * Math.sin(p * Math.PI)).toInt().coerceIn(0, 255)
            g2.paint = GradientPaint(x0.toFloat(), y0.toFloat(), Color(255, 255, 255, 0), x1.toFloat(), y1.toFloat(), Color(0xCB, 0xA6, 0xF7, al))
            g2.stroke = BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g2.drawLine(x0.toInt(), y0.toInt(), x1.toInt(), y1.toInt())
        }
    }
}

private class PillButton(t: String, val pulse: Boolean = false, f: () -> Unit) : JButton(t) {
    private var amt = 0f
    private var target = 0f
    private var ph = 0.0
    private val tm = javax.swing.Timer(25, null)
    init {
        isContentAreaFilled = false; isFocusPainted = false; isBorderPainted = false
        foreground = BG; font = font.deriveFont(Font.BOLD, 13f)
        border = BorderFactory.createEmptyBorder(8, 18, 8, 18)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        tm.addActionListener {
            if (!isShowing) return@addActionListener
            amt += (target - amt) * 0.3f; ph += 0.12
            if (!pulse && Math.abs(target - amt) < 0.02f) { amt = target; tm.stop() }
            repaint()
        }
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) { target = 1f; tm.start() }
            override fun mouseExited(e: MouseEvent) { target = 0f; tm.start() }
            override fun mousePressed(e: MouseEvent) { amt = 1.4f; tm.start() }
        })
        if (pulse) tm.start()
        addActionListener { f() }
    }
    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val glow = (amt * 0.5f + (if (pulse) 0.25f * (1f + Math.sin(ph).toFloat()) / 2f else 0f)).coerceIn(0f, 1f)
        g2.color = Color(ACC.red, ACC.green, ACC.blue, (110 * glow).toInt().coerceIn(0, 255))
        g2.fillRoundRect(0, 0, width, height, height, height)
        fun lift(c: Color) = Color((c.red + (255 - c.red) * glow * 0.4f).toInt(), (c.green + (255 - c.green) * glow * 0.4f).toInt(), (c.blue + (255 - c.blue) * glow * 0.4f).toInt())
        g2.paint = GradientPaint(0f, 0f, lift(ACC), width.toFloat(), height.toFloat(), lift(Color(0xB4BEFE)))
        g2.fillRoundRect(1, 1, width - 2, height - 2, height, height)
        g2.dispose()
        super.paintComponent(g)
    }
}

private fun btn(t: String, f: () -> Unit): JButton = PillButton(t, false, f)

private fun fmt(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

private class TrackList(val items: () -> List<Track>) : JScrollPane() {
    val model = DefaultListModel<String>()
    val list = JList(model).dark().apply { cellRenderer = RowRenderer(); isOpaque = false }
    var cur = listOf<Track>()
    init { setViewportView(list); border = BorderFactory.createEmptyBorder(); refresh() }
    private var fa = 1f
    override fun paint(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.composite = AlphaComposite.SrcOver.derive(fa.coerceIn(0f, 1f))
        super.paint(g2)
        g2.dispose()
    }
    fun refresh() {
        fa = 0.3f
        javax.swing.Timer(25, null).apply { addActionListener { fa += 0.12f; if (fa >= 1f) { fa = 1f; stop() }; repaint() }; start() }
        cur = items(); model.clear()
        cur.forEach { model.addElement("${it.title} - ${it.artist}  [${if (it.source == Source.YOUTUBE) "YT" else "SC"}]") }
    }
    fun selected(): Track? = cur.getOrNull(list.selectedIndex)
}

fun main() {
    run {
        val dark = javax.swing.plaf.ColorUIResource(PANEL)
        val acc = javax.swing.plaf.ColorUIResource(ACC)
        val bg = javax.swing.plaf.ColorUIResource(BG)
        for (k in listOf("TabbedPane.selected", "TabbedPane.background", "TabbedPane.contentAreaColor", "TabbedPane.light", "TabbedPane.highlight", "TabbedPane.shadow", "TabbedPane.darkShadow", "TabbedPane.focus", "ScrollBar.track", "ScrollBar.background", "Slider.background", "List.background", "TextField.background", "ComboBox.background", "OptionPane.background", "Panel.background")) UIManager.put(k, dark)
        for (k in listOf("ScrollBar.thumb", "ScrollBar.thumbHighlight", "ScrollBar.thumbShadow", "ScrollBar.thumbDarkShadow", "Slider.thumb", "Slider.focus", "List.selectionBackground", "ComboBox.selectionBackground")) UIManager.put(k, acc)
        UIManager.put("List.selectionForeground", bg)
        UIManager.put("TabbedPane.foreground", Color.WHITE)
        UIManager.put("TabbedPane.tabInsets", Insets(8, 18, 8, 18))
        UIManager.put("TabbedPane.contentBorderInsets", Insets(0, 0, 0, 0))
        UIManager.put("ScrollBar.width", 10)
    }
    SwingUtilities.invokeLater {
        val lib = Library(File(System.getProperty("user.home"), ".config/orionmusic"))
        val fx = AudioFx()
        val results = mutableListOf<Track>()
        var lyrics: List<LyricLine> = emptyList()
        var dragging = false

        val status = JLabel("Ready").dark().apply { foreground = DIM }
        val nowLbl = NowTitle()
        val timeLbl = JLabel("0:00").dark()
        val seek = GlowSlider(0, 100, 0)

        // lyrics fullscreen window
        val lyricLbl = JLabel("", SwingConstants.CENTER).apply { foreground = Color.WHITE; verticalAlignment = SwingConstants.CENTER }
        val lyricWin = JFrame("Orion Lyrics").apply {
            isUndecorated = true; contentPane = GlowPanel().apply { layout = BorderLayout() }
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
            val s = GlowSlider(min, max, init, vertical)
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

        val tp = FadeTabs().dark()
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
        bar.add(PillButton("Pause/Play", true) { mpv.togglePause() })
        bar.add(btn("Next") { lib.next()?.let { play(it); refreshAll() } })
        bar.add(shuffleB); bar.add(repeatB)
        val likeBurst = Burst()
        val likeB = btn("Like") { (currentSel(tp) ?: lib.current())?.let { lib.toggleLike(it); refreshAll(); likeBurst.fire() } }
        bar.add(JPanel().apply { isOpaque = false; layout = OverlayLayout(this); border = BorderFactory.createEmptyBorder(0, 0, 0, 0); add(likeBurst); add(likeB) })
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
            add(PulseRing(GalaxyLogo(44)), BorderLayout.WEST)
            add(ShimmerTitle("ORION MUSIC"), BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).dark().apply { add(updateB); add(status) }, BorderLayout.EAST)
        }
        val north = JPanel(BorderLayout(4, 6)).dark().apply { add(head, BorderLayout.NORTH); add(box, BorderLayout.SOUTH) }
        val seekRow = JPanel(BorderLayout(6, 0)).dark().apply { add(Vinyl(), BorderLayout.WEST); add(seek); add(timeLbl, BorderLayout.EAST) }
        val south = JPanel(GridLayout(0, 1)).dark().apply { add(nowLbl); add(seekRow); add(bar) }

        JFrame("Orion Music").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            addWindowListener(object : java.awt.event.WindowAdapter() { override fun windowClosing(e: java.awt.event.WindowEvent) { mpv.stop() } })
            contentPane = GlowPanel().apply { layout = BorderLayout(8, 8) }
            (contentPane as JComponent).border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            contentPane.add(north, BorderLayout.NORTH); contentPane.add(GlassCard(tp), BorderLayout.CENTER); contentPane.add(south, BorderLayout.SOUTH)
            setSize(820, 700); setLocationRelativeTo(null); isVisible = true
        }
    }
}
