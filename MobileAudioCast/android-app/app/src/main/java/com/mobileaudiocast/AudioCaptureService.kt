package com.mobileaudiocast

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
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
        NotificationHelper.ensureChannel(this)
        startForeground(1001, NotificationHelper.buildForegroundNotification(this))

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "启动失败：缺少 MediaProjection 授权", Toast.LENGTH_LONG).show()
            stopSelf()
            return START_NOT_STICKY
        }

        webServer = LocalWebServer(this).also { it.start() }
        signalingServer = SignalingServer(onMessage = ::handleSignalMessage, onPeerChanged = { connected ->
            if (connected) {
                webRtcStreamer?.release()
                webRtcStreamer?.initPeerConnection()
                webRtcStreamer?.createOffer()
            }
        }).also { it.start() }

        webRtcStreamer = WebRtcStreamer(this, signalingServer!!)

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = projectionManager.getMediaProjection(resultCode, data)
        audioCaptureManager = AudioCaptureManager(projection,
            onAudioData = { _, _ -> },
            onCaptureError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() })
        audioCaptureManager?.startCapture()

        return START_STICKY
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

    override fun onDestroy() {
        audioCaptureManager?.stopCapture()
        webRtcStreamer?.release()
        signalingServer?.stop()
        webServer?.stop()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"

        fun buildStartIntent(context: Context, resultCode: Int, resultData: Intent): Intent {
            return Intent(context, AudioCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
        }
    }
}
