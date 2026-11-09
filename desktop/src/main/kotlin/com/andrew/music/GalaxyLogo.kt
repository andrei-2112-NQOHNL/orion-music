package com.andrew.music

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import javax.swing.Timer
import kotlin.math.cos
import kotlin.math.sin

/** Tiny rigid four-arm spiral galaxy for the top left of every Orion app. Arms rotate as one shape. */
class GalaxyLogo(private val size: Int = 44) : JComponent() {
    private var spin = 0.0
    private val pal = arrayOf(Color(255, 214, 160), Color(255, 160, 130), Color(225, 130, 230), Color(150, 140, 255), Color(110, 190, 255))
    private val timer = Timer(33) { spin += 0.02; repaint() }

    init { preferredSize = Dimension(size, size); isOpaque = false; timer.start() }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        val cx = width / 2.0
        val cy = height / 2.0
        val r = minOf(width, height) / 2.0 - 2
        for (arm in 0 until 4) {
            for (i in 0 until 22) {
                val t = i / 21.0
                val ang = spin + arm * Math.PI / 2 + t * 4.6
                val rad = r * (0.15 + 0.85 * t)
                val x = cx + cos(ang) * rad
                val y = cy + sin(ang) * rad
                val c = pal[minOf(4, (t * 5).toInt())]
                g2.color = Color(c.red, c.green, c.blue, (230 - 150 * t).toInt())
                val s = 2.6 - 1.4 * t
                g2.fillOval((x - s / 2).toInt(), (y - s / 2).toInt(), s.toInt().coerceAtLeast(1), s.toInt().coerceAtLeast(1))
            }
        }
        g2.color = Color(255, 235, 200, 90)
        g2.fillOval((cx - r * 0.3).toInt(), (cy - r * 0.3).toInt(), (r * 0.6).toInt(), (r * 0.6).toInt())
        g2.color = Color(255, 250, 235)
        g2.fillOval((cx - r * 0.12).toInt(), (cy - r * 0.12).toInt(), (r * 0.24).toInt().coerceAtLeast(2), (r * 0.24).toInt().coerceAtLeast(2))
    }
}
