package com.andrew.music

import android.graphics.*
import android.graphics.drawable.Drawable

/** Drifting aurora glow with twinkling stars, drawn from the clock so any redraw animates it. */
class GlowBackground : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cols = intArrayOf(0x00B8D4, 0xB4BEFE, 0xCBA6F7, 0xF5C2E7, 0x94E2D5)
    private val rnd = java.util.Random(7)
    private val sx = FloatArray(60) { rnd.nextFloat() }
    private val sy = FloatArray(60) { rnd.nextFloat() }
    private val sp = FloatArray(60) { 0.02f + rnd.nextFloat() * 0.06f }

    override fun draw(c: Canvas) {
        val w = bounds.width().toFloat(); val h = bounds.height().toFloat()
        c.drawColor(0xFF0B0F1A.toInt())
        val t = System.currentTimeMillis() / 1000.0
        for (i in cols.indices) {
            val cx = (w * (0.15 + 0.7 * i / (cols.size - 1) + 0.2 * Math.sin(t * (0.3 + i * 0.1) + i * 2))).toFloat()
            val cy = (h * (0.2 + 0.6 * ((i * 0.37) % 1.0) + 0.15 * Math.cos(t * (0.25 + i * 0.08) + i))).toFloat()
            p.shader = RadialGradient(cx, cy, maxOf(w, h) * 0.6f, 0x45000000 or cols[i], cols[i], Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, p)
        }
        p.shader = null
        for (i in sx.indices) {
            val x = ((sx[i] + t * sp[i] * 0.3) % 1.0).toFloat() * w
            val y = ((sy[i] - t * sp[i] * 0.15 + 1.0) % 1.0).toFloat() * h
            val a = (60 + 150 * (0.5 + 0.5 * Math.sin(t * 3 * (1 + sp[i] * 10) + i))).toInt().coerceIn(0, 255)
            p.color = Color.argb(a, 255, 255, 255)
            c.drawCircle(x, y, if (i % 9 == 0) 2.5f else 1.5f, p)
        }
    }
    override fun setAlpha(a: Int) {}
    override fun setColorFilter(f: ColorFilter?) {}
    @Deprecated("required override") override fun getOpacity() = PixelFormat.OPAQUE
}
