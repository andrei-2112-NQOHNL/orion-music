package com.andrew.music

import com.grack.nanojson.JsonParser
import okhttp3.OkHttpClient
import java.io.File

data class UpdateInfo(val version: String, val url: String, val notes: String)

/** Checks a small JSON file {"version":"0.5","url":"https://...","notes":"..."} for a newer version. */
object Updater {
    const val CURRENT = "0.5"
    private val client = OkHttpClient()

    /** The check address is stored in ~/.config/orionmusic/update_url so it can be changed without rebuilding. */
    const val DEFAULT_URL = "https://raw.githubusercontent.com/andrei-2112-NQOHNL/orion-music/main/version.json"
    fun checkUrl(dir: File): String? = File(dir, "update_url").takeIf { it.exists() }?.readText()?.trim()?.takeIf { it.startsWith("https://") } ?: DEFAULT_URL

    private fun newer(a: String, b: String): Boolean {
        val x = a.split(".").map { it.toIntOrNull() ?: 0 }
        val y = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    fun check(dir: File): UpdateInfo? {
        val url = checkUrl(dir) ?: return null
        val body = client.newCall(okhttp3.Request.Builder().url(url).header("User-Agent", "OrionMusic").build())
            .execute().use { it.body?.string() } ?: return null
        val o = JsonParser.`object`().from(body)
        val v = o.getString("version") ?: return null
        val u = o.getString("url") ?: return null
        return if (newer(v, CURRENT)) UpdateInfo(v, u, o.getString("notes") ?: "") else null
    }
}
