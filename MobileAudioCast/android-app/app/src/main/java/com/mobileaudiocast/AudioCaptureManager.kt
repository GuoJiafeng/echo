package com.mobileaudiocast

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build

class AudioCaptureManager(
    private val mediaProjection: MediaProjection,
    private val onAudioData: (ByteArray, Int) -> Unit,
    private val onCaptureError: (String) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile
    private var running = false

    fun startCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onCaptureError("AudioPlaybackCapture 仅支持 Android 10+")
            return
        }
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val sampleRate = 48000
        val channelMask = AudioFormat.CHANNEL_IN_STEREO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(encoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelMask)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        val record = audioRecord ?: return
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            onCaptureError("AudioRecord 初始化失败")
            return
        }

        running = true
        record.startRecording()
        captureThread = Thread {
            val buffer = ByteArray(bufferSize)
            while (running) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    onAudioData(buffer.copyOf(read), read)
                } else if (read < 0) {
                    onCaptureError("当前音源 App 可能禁止被系统捕获，请更换音源 App 或检查系统限制。")
                    break
                }
            }
        }.apply { start() }
    }

    fun stopCapture() {
        running = false
        captureThread?.join(500)
        captureThread = null
        audioRecord?.runCatching {
            stop()
            release()
        }
        audioRecord = null
        mediaProjection.stop()
    }
}
