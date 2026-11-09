package com.andrew.music

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

/** OkHttp based downloader that NewPipe Extractor needs. */
class HttpDownloader(private val client: OkHttpClient = OkHttpClient()) : Downloader() {
    override fun execute(request: Request): Response {
        val body = request.dataToSend()?.toRequestBody()
        val b = okhttp3.Request.Builder().url(request.url())
            .method(request.httpMethod(), body ?: if (request.httpMethod() == "POST") ByteArray(0).toRequestBody() else null)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0")
        for ((k, v) in request.headers()) { b.removeHeader(k); v.forEach { b.addHeader(k, it) } }
        client.newCall(b.build()).execute().use { r ->
            if (r.code == 429) throw org.schabi.newpipe.extractor.exceptions.ReCaptchaException("Rate limited", request.url())
            return Response(r.code, r.message, r.headers.toMultimap(), r.body?.string(), r.request.url.toString())
        }
    }
}
