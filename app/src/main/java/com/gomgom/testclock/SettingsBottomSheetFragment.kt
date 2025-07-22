package com.gomgom.testclock

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsBottomSheetFragment : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.NoAnimationBottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.setting_sheet, container, false)

        val fontSeekBar = view.findViewById<SeekBar>(R.id.seekFontSize)
        val showSecondsSwitch = view.findViewById<Switch>(R.id.switchSeconds)
        val nightModeSwitch = view.findViewById<Switch>(R.id.switchNight)
        val saveButton = view.findViewById<Button>(R.id.buttonSave)
        val savedLabel = view.findViewById<TextView>(R.id.labelSaved)

        val prefs = requireActivity().getSharedPreferences("clock_prefs", Context.MODE_PRIVATE)
        fontSeekBar.progress = prefs.getInt("fontSize", 48) - 48
        showSecondsSwitch.isChecked = prefs.getBoolean("showSeconds", true)
        nightModeSwitch.isChecked = prefs.getBoolean("nightMode", false)

        saveButton.setOnClickListener {
            with(prefs.edit()) {
                putInt("fontSize", fontSeekBar.progress + 48)
                putBoolean("showSeconds", showSecondsSwitch.isChecked)
                putBoolean("nightMode", nightModeSwitch.isChecked)
                apply()
            }

            // UI에 즉시 반영 요청
            (activity as? MainActivity)?.applyClockSettings()

            // 저장됨 표시 후 닫기
            savedLabel.text = "✔ 저장됨"
            savedLabel.visibility = View.VISIBLE
            savedLabel.postDelayed({ dismiss() }, 1500)
        }

        return view
    }
}