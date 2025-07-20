package com.gomgom.testclock

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var clockView: TextView
    private val handler = Handler()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val updateClock = object : Runnable {
        override fun run() {
            val currentTime = timeFormat.format(Date())
            clockView.text = currentTime
            handler.postDelayed(this, 1000) // 1초마다 갱신
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clockView = TextView(this).apply {
            textSize = 48f
            setTextColor(0xFF000000.toInt())  // 검정색 텍스트
            setBackgroundColor(0xFFFFFFFF.toInt()) // 흰 배경
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            gravity = Gravity.CENTER                        // 수직 + 수평 가운데 정렬

        }

        setContentView(clockView)
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateClock)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateClock)
    }
}