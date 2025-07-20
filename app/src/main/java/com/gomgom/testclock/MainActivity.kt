package com.gomgom.testclock

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private var clockJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd (E)", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }

        dateView = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.BLACK)
        }

        val dateParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = 30
            leftMargin = 30
        }

        clockView = TextView(this).apply {
            textSize = 48f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.BLACK)
        }

        val clockParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }

        layout.addView(dateView, dateParams)
        layout.addView(clockView, clockParams)
        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        clockJob = lifecycleScope.launch {
            var lastTime = ""
            var lastDate = ""

            while (isActive) {
                val now = System.currentTimeMillis()
                val currentTime = timeFormat.format(Date(now))
                val currentDate = dateFormat.format(Date(now))

                if (currentTime != lastTime) {
                    clockView.text = currentTime
                    lastTime = currentTime
                }

                if (currentDate != lastDate) {
                    dateView.text = currentDate
                    lastDate = currentDate
                }

                val nextTick = now + 1000 - (now % 1000)
                val delay = nextTick - System.currentTimeMillis()
                delay(delay)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        clockJob?.cancel()
    }
}