package com.mobileaudiocast

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.InetSocketAddress

class SignalingServer(
    private val onMessage: (JSONObject) -> Unit,
    private val onPeerChanged: (Boolean) -> Unit
) {
    private var client: WebSocket? = null

    private val server = object : WebSocketServer(InetSocketAddress(8081)) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            client?.close(1000, "Only one client supported")
            client = conn
            onPeerChanged(true)
            sendToClient(JSONObject().put("type", "connected"))
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            if (client == conn) {
                client = null
                onPeerChanged(false)
            }
        }

        override fun onMessage(conn: WebSocket, message: String) {
            runCatching { JSONObject(message) }
                .onSuccess(onMessage)
                .onFailure {
                    sendToClient(JSONObject().put("type", "error").put("message", "invalid json"))
                }
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            ex.printStackTrace()
        }

        override fun onStart() = Unit
    }

    fun start() = server.start()

    fun stop() {
        server.stop()
    }

    fun sendToClient(json: JSONObject) {
        client?.send(json.toString())
    }
}
