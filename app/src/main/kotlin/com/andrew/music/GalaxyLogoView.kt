package com.andrew.music

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/** Tiny rigid four-arm spiral galaxy for the top left of every Orion app. Arms rotate as one shape. */
class GalaxyLogoView(ctx: Context) : View(ctx) {
    private var spin = 0.0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pal = intArrayOf(Color.rgb(255, 214, 160), Color.rgb(255, 160, 130), Color.rgb(225, 130, 230), Color.rgb(150, 140, 255), Color.rgb(110, 190, 255))

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - 2f
        for (arm in 0 until 4) for (i in 0 until 22) {
            val t = i / 21.0
            val ang = spin + arm * Math.PI / 2 + t * 4.6
            val rad = r * (0.15 + 0.85 * t)
            paint.color = pal[minOf(4, (t * 5).toInt())]
            paint.alpha = (230 - 150 * t).toInt()
            c.drawCircle(cx + (cos(ang) * rad).toFloat(), cy + (sin(ang) * rad).toFloat(), (1.8 - 0.9 * t).toFloat() * resources.displayMetrics.density, paint)
        }
        paint.color = Color.rgb(255, 235, 200); paint.alpha = 90
        c.drawCircle(cx, cy, r * 0.3f, paint)
        paint.color = Color.rgb(255, 250, 235); paint.alpha = 255
        c.drawCircle(cx, cy, r * 0.12f, paint)
        spin += 0.02
        postInvalidateOnAnimation()
    }
}
