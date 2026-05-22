package com.mobileaudiocast

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import org.json.JSONObject

class AudioCaptureService : Service() {
    private var webServer: LocalWebServer? = null
    private var signalingServer: SignalingServer? = null
    private var webRtcStreamer: WebRtcStreamer? = null
    private var audioCaptureManager: AudioCaptureManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> startStreaming(intent)
            else -> stopSelf()
        }
        return START_STICKY
    }

    private fun startStreaming(intent: Intent) {
        NotificationHelper.ensureChannel(this)
        startForeground(1001, NotificationHelper.buildForegroundNotification(this))

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "启动失败：缺少 MediaProjection 授权", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        teardown()
        webServer = LocalWebServer(this).also { it.start() }
        signalingServer = SignalingServer(
            onMessage = ::handleSignalMessage,
            onPeerChanged = { connected ->
                if (connected) {
                    webRtcStreamer?.resetForNewPeer()
                    webRtcStreamer?.createOffer()
                } else {
                    webRtcStreamer?.releasePeerOnly()
                }
            },
            onServerError = { msg -> Toast.makeText(this, "信令异常: $msg", Toast.LENGTH_SHORT).show() }
        ).also { it.start() }

        webRtcStreamer = WebRtcStreamer(this, signalingServer!!).also { it.initPeerConnection() }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)
        audioCaptureManager = AudioCaptureManager(
            mediaProjection = projection,
            onAudioData = { _, _ -> /* Placeholder: PCM bridge to custom WebRTC ADM in full production build. */ },
            onCaptureError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        ).also { it.startCapture() }
    }

    private fun handleSignalMessage(json: JSONObject) {
        when (json.optString("type")) {
            "answer" -> webRtcStreamer?.handleAnswer(json.optString("sdp"))
            "ice-candidate" -> webRtcStreamer?.addIceCandidate(
                json.optString("candidate"),
                json.optString("sdpMid"),
                json.optInt("sdpMLineIndex")
            )
        }
    }

    private fun teardown() {
        audioCaptureManager?.stopCapture()
        audioCaptureManager = null
        webRtcStreamer?.release()
        webRtcStreamer = null
        signalingServer?.stop()
        signalingServer = null
        webServer?.stop()
        webServer = null
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"
        private const val ACTION_START = "com.mobileaudiocast.action.START"
        private const val ACTION_STOP = "com.mobileaudiocast.action.STOP"

        fun buildStartIntent(context: Context, resultCode: Int, resultData: Intent): Intent {
            return Intent(context, AudioCaptureService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
        }

        fun buildStopIntent(context: Context): Intent {
            return Intent(context, AudioCaptureService::class.java).apply { action = ACTION_STOP }
        }
    }
}
