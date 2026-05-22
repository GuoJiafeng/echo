package com.mobileaudiocast

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.json.JSONObject
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * LAN signaling server. Supports a single active browser client and routes
 * structured JSON messages used by WebRTC offer/answer/ICE negotiation.
 */
class SignalingServer(
    private val onMessage: (JSONObject) -> Unit,
    private val onPeerChanged: (Boolean) -> Unit,
    private val onServerError: (String) -> Unit
) {
    private val client = AtomicReference<WebSocket?>(null)

    private val server = object : WebSocketServer(InetSocketAddress(8081)) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            if (handshake.resourceDescriptor != "/signaling") {
                conn.close(1008, "unsupported path")
                return
            }
            client.getAndSet(conn)?.close(1000, "replace old session")
            onPeerChanged(true)
            sendToClient(JSONObject().put("type", "connected"))
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            if (client.compareAndSet(conn, null)) {
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
            onServerError(ex.message ?: "unknown signaling error")
        }

        override fun onStart() = Unit
    }

    fun start() = server.start()

    fun stop() {
        runCatching { client.getAndSet(null)?.close(1001, "server stop") }
        runCatching { server.stop(500) }
    }

    fun sendToClient(json: JSONObject) {
        client.get()?.send(json.toString())
    }
}
