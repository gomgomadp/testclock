package com.gomgom.testclock

import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.DatePicker
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private val handler = Handler()
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val updateClock = object : Runnable {
        override fun run() {
            val cDate = Date()
            val currentTime = timeFormat.format(cDate)
            val currentDate = dateFormat.format(cDate)
            clockView.text = currentTime
            clockView.gravity = Gravity.CENTER  // 수직 + 수평 가운데 정렬
            //gravity = Gravity.TOP or Gravity.START  // 왼쪽 상단

            dateView.text = currentDate
            dateView.gravity = Gravity.TOP
            //dateView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
            //clockView.gravity = Gravity.BOTTOM or Gravity.END // 오른쪽 하단

            handler.postDelayed(this, 1000) // 1초마다 갱신
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = FrameLayout(this)

        dateView = TextView(this).apply {
            textSize = 24f
            setTextColor(0xFF000000.toInt())
            text = "20250720"
            gravity = Gravity.START
        }

        val dateParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = 20
            leftMargin = 20
        }

        clockView = TextView(this).apply {
            textSize = 48f
            setTextColor(0xFF000000.toInt())
            gravity = Gravity.CENTER
            text = "21:00:00"
        }

        val clockParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }

        layout.addView(dateView, dateParams)
        layout.addView(clockView, clockParams)

        setContentView(layout)
    }
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        clockView = TextView(this).apply {
//            textSize = 48f
//            setTextColor(0xFF000000.toInt())  // 검정색 텍스트
//            setBackgroundColor(0xFFFFFFFF.toInt()) // 흰 배경
//            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
//            gravity = Gravity.CENTER                        // 수직 + 수평 가운데 정렬
//
//        }
//
//        setContentView(clockView)
//    }

    override fun onResume() {
        super.onResume()
        handler.post(updateClock)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateClock)
    }
}