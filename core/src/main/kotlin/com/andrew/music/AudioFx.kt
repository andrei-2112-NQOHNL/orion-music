package com.andrew.music

/** Shared audio settings. Builds an mpv/ffmpeg filter chain for speed, pitch, equalizer and reverb. */
class AudioFx {
    var speed = 1.0
    var pitch = 1.0
    var reverb = 0.0
    var volume = 100
    val bands = DoubleArray(10)

    companion object {
        val FREQS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
    }

    fun filterChain(): String {
        val f = mutableListOf<String>()
        if (speed != 1.0 || pitch != 1.0) f.add("rubberband=pitch-scale=$pitch:tempo=$speed")
        for (i in bands.indices) if (bands[i] != 0.0) f.add("equalizer=f=${FREQS[i]}:width_type=o:width=1:g=${bands[i]}")
        if (reverb > 0.0) {
            val d = 20 + reverb * 60
            val dec = 0.1 + reverb * 0.4
            f.add("aecho=0.8:0.8:${d.toInt()}|${(d * 2).toInt()}|${(d * 3).toInt()}:$dec|${dec * 0.7}|${dec * 0.5}")
        }
        return f.joinToString(",")
    }
}
