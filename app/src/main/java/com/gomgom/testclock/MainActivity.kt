package com.gomgom.testclock

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private var clockJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd (E)", Locale.getDefault())
    lateinit var saveButton: Button
    lateinit var settingsIcon: ImageButton

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


//         saveButton = Button(this).apply {
//            text = "설정 저장"
//            setOnClickListener {
//                val prefs = getSharedPreferences("clock_prefs", MODE_PRIVATE)
//                with(prefs.edit()) {
//                    putInt("fontSize", 60)
//                    putBoolean("showSeconds", true)
//                    apply()
//                }
//                Toast.makeText(this@MainActivity, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
//            }
//        }
//
//        val params = FrameLayout.LayoutParams(
//            FrameLayout.LayoutParams.WRAP_CONTENT,
//            FrameLayout.LayoutParams.WRAP_CONTENT
//        ).apply {
//            gravity = Gravity.BOTTOM or Gravity.END
//            bottomMargin = 30
//            rightMargin = 30
//        }
//
//
//        saveButton.layoutParams = params
//        layout.addView(saveButton)

        settingsIcon = ImageButton(this).apply {
            setImageResource(R.drawable.ic_settings)  // Vector asset로 추가
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                showSettingsBottomSheet()
            }
        }
                val siconparams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = 30
            rightMargin = 30
        }
        settingsIcon.layoutParams = siconparams
        val prefs = getSharedPreferences("clock_prefs", MODE_PRIVATE)
        val fontSize = prefs.getInt("fontSize", 48)
        val showSeconds = prefs.getBoolean("showSeconds", true)
        layout.addView(dateView, dateParams)
        layout.addView(clockView, clockParams)
        layout.addView(settingsIcon)
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

    private fun showSettingsBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.setting_sheet, null)

        val fontSeekBar = view.findViewById<SeekBar>(R.id.seekFontSize)
        val showSecondsSwitch = view.findViewById<Switch>(R.id.switchSeconds)
        val nightModeSwitch = view.findViewById<Switch>(R.id.switchNight)

        val saveButton = view.findViewById<Button>(R.id.buttonSave)
        val savedLabel = view.findViewById<TextView>(R.id.labelSaved)

        saveButton.setOnClickListener {
            val prefs = getSharedPreferences("clock_prefs", MODE_PRIVATE)
            with(prefs.edit()) {
                putInt("fontSize", fontSeekBar.progress + 48)
                putBoolean("showSeconds", showSecondsSwitch.isChecked)
                putBoolean("nightMode", nightModeSwitch.isChecked)
                apply()
            }

            savedLabel.text = "✔ 저장됨"
            savedLabel.visibility = View.VISIBLE
            savedLabel.postDelayed({ savedLabel.visibility = View.GONE }, 2000)
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }
}