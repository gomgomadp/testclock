package com.gomgom.testclock

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
    private var showSeconds = true
    private lateinit var settingsIcon: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }

        dateView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
        }

        val dateParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = 30
            leftMargin = 30
        }

        clockView = TextView(this).apply {
            typeface = Typeface.MONOSPACE
        }

        val clockParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }

        settingsIcon = ImageButton(this).apply {
            setImageResource(R.drawable.ic_settings)
            setBackgroundColor(Color.TRANSPARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener {
                // Calculate max font size in pixels that fits the screen
                val paint = Paint().apply {
                    typeface = clockView.typeface
                }
                val textToMeasure = "00:00:00"
                val screenWidth = resources.displayMetrics.widthPixels
                val targetWidth = screenWidth * 0.95f

                // Binary search for the best text size
                var high = 2000f
                var low = 0f
                while (high - low > 1) {
                    val mid = (high + low) / 2
                    paint.textSize = mid
                    if (paint.measureText(textToMeasure) > targetWidth) {
                        high = mid
                    } else {
                        low = mid
                    }
                }
                val maxFontSizePx = low

                val sheet = SettingsBottomSheetFragment().apply {
                    arguments = Bundle().apply {
                        putFloat("maxFontSizePx", maxFontSizePx)
                    }
                }
                sheet.show(supportFragmentManager, "SettingsBottomSheet")
            }
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val iconSize = screenWidth / 10 // 화면 너비의 1/10 크기로 설정

        val siconparams = FrameLayout.LayoutParams(
            iconSize,
            iconSize
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = 30
            rightMargin = 30
        }

        settingsIcon.layoutParams = siconparams
        layout.addView(dateView, dateParams)
        layout.addView(clockView, clockParams)
        layout.addView(settingsIcon)
        setContentView(layout)

        applyClockSettings() // 앱 시작 시 저장된 설정 적용
    }

    override fun onResume() {
        super.onResume()
        applyClockSettings() // 다른 화면에서 돌아왔을 때도 설정 적용
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

                val delayMs = if (showSeconds) {
                    // Refresh every second
                    val nextTick = now + 1000 - (now % 1000)
                    nextTick - System.currentTimeMillis()
                } else {
                    // Refresh every minute
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = now
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    calendar.add(Calendar.MINUTE, 1)
                    calendar.timeInMillis - System.currentTimeMillis()
                }
                delay(delayMs.coerceAtLeast(0))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        clockJob?.cancel()
    }

    fun applyClockSettings() {
        val prefs = getSharedPreferences("clock_prefs", MODE_PRIVATE)

        // Font Sizes (in pixels)
        val defaultFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 48f, resources.displayMetrics).toInt()
        val defaultDateFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics).toInt()
        val fontSize = prefs.getInt("fontSize", defaultFontSizePx)
        val dateFontSize = prefs.getInt("dateFontSize", defaultDateFontSizePx)

        // Other Settings
        this.showSeconds = prefs.getBoolean("showSeconds", true)
        val nightMode = prefs.getBoolean("nightMode", false)
        val iconWhite = prefs.getBoolean("iconWhite", false)

        // Apply font sizes
        clockView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateFontSize.toFloat())

        // Apply time format
        timeFormat.applyPattern(if (showSeconds) "HH:mm:ss" else "HH:mm")

        // Apply Date Format
        val dateFormatSetting = prefs.getString("dateFormat", "yyyy-MM-dd (E)")
        if (dateFormatSetting == "HIDE_DATE") {
            dateView.visibility = View.GONE
        } else {
            dateView.visibility = View.VISIBLE
            var pattern = dateFormatSetting
            if (pattern == "CUSTOM_FORMAT") {
                pattern = prefs.getString("customDateFormat", "yyyy-MM-dd (E)")
            }
            try {
                dateFormat.applyPattern(pattern)
            } catch (e: IllegalArgumentException) {
                // Revert to default if pattern is invalid
                dateFormat.applyPattern("yyyy-MM-dd (E)")
                Toast.makeText(this, "잘못된 날짜 형식입니다. 기본값으로 복원됩니다.", Toast.LENGTH_LONG).show()
            }
        }

        // Apply colors
        val bgColor = if (nightMode) Color.BLACK else Color.WHITE
        val textColor = if (nightMode) Color.WHITE else Color.BLACK
        val iconColor = if (iconWhite) Color.WHITE else Color.BLACK

        clockView.setTextColor(textColor)
        dateView.setTextColor(textColor)
        settingsIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        (clockView.parent as? View)?.setBackgroundColor(bgColor)
    }
}
