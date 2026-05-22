package com.mobileaudiocast

import android.content.Context
import fi.iki.elonen.NanoHTTPD

class LocalWebServer(private val context: Context) : NanoHTTPD(8080) {
    override fun serve(session: IHTTPSession): Response {
        val path = when (session.uri) {
            "/", "/index.html" -> "web/index.html"
            "/player.js" -> "web/player.js"
            "/style.css" -> "web/style.css"
            else -> return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
        val mime = when {
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            else -> "text/html"
        }
        val content = context.assets.open(path).bufferedReader().use { it.readText() }
        return newFixedLengthResponse(Response.Status.OK, mime, content)
    }
}
