package com.mobileaudiocast

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var addressText: TextView

    private val projectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = AudioCaptureService.buildStartIntent(this, result.resultCode, result.data!!)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
        } else {
            Toast.makeText(this, "未授予系统捕获权限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ip = NetworkUtils.getLocalIpv4Address()
        addressText = TextView(this).apply { text = "电脑访问地址: http://$ip:8080" }
        val startBtn = Button(this).apply {
            text = "开始投放系统音频"
            setOnClickListener { requestProjection() }
        }
        val stopBtn = Button(this).apply {
            text = "停止投放"
            setOnClickListener { stopService(Intent(this@MainActivity, AudioCaptureService::class.java)) }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 60)
            addView(addressText)
            addView(startBtn)
            addView(stopBtn)
        }
        setContentView(layout)
    }

    private fun requestProjection() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(manager.createScreenCaptureIntent())
    }
}
