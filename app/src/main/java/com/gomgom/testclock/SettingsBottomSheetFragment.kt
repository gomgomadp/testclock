package com.gomgom.testclock

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsBottomSheetFragment : BottomSheetDialogFragment() {

    // Define special keys and format list
    private val HIDE_DATE_KEY = "HIDE_DATE"
    private val CUSTOM_FORMAT_KEY = "CUSTOM_FORMAT"
    private lateinit var dateFormats: List<Pair<String, String>>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.NoAnimationBottomSheetDialogTheme)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.peekHeight = 0
            }
        }
        dialog.dismissWithAnimation = false
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.setting_sheet, container, false)

        // Initialize format list
        dateFormats = listOf(
            "날짜 숨기기" to HIDE_DATE_KEY,
            "yyyy-MM-dd (E)" to "yyyy-MM-dd (E)",
            "yyyy. MM. dd." to "yyyy. MM. dd.",
            "MM/dd/yyyy" to "MM/dd/yyyy",
            "사용자 지정" to CUSTOM_FORMAT_KEY
        )

        // --- Get View References ---
        val fontSeekBar = view.findViewById<SeekBar>(R.id.seekFontSize)
        val dateFontSeekBar = view.findViewById<SeekBar>(R.id.seekDateFontSize)
        val showSecondsSwitch = view.findViewById<Switch>(R.id.switchSeconds)
        val nightModeSwitch = view.findViewById<Switch>(R.id.switchNight)
        val iconWhiteSwitch = view.findViewById<Switch>(R.id.switchIconWhite)
        val saveButton = view.findViewById<Button>(R.id.buttonSave)
        val savedLabel = view.findViewById<TextView>(R.id.labelSaved)
        val dateFormatSpinner = view.findViewById<Spinner>(R.id.spinnerDateFormat)
        val customDateFormatEdit = view.findViewById<EditText>(R.id.editCustomDateFormat)
        val dateFontSizeLabel = view.findViewById<TextView>(R.id.labelDateFontSize)

        // --- Setup Date Format Spinner ---
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dateFormats.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dateFormatSpinner.adapter = adapter

        // --- Load Preferences ---
        val res = requireContext().resources
        val prefs = requireActivity().getSharedPreferences("clock_prefs", Context.MODE_PRIVATE)

        // Clock Font Size
        val maxFontSizePx = arguments?.getFloat("maxFontSizePx") ?: 800f
        val minFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12f, res.displayMetrics)
        fontSeekBar.max = (maxFontSizePx - minFontSizePx).toInt()
        val defaultFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 48f, res.displayMetrics).toInt()
        val currentFontSizePx = prefs.getInt("fontSize", defaultFontSizePx).toFloat()
        fontSeekBar.progress = (currentFontSizePx - minFontSizePx).toInt().coerceAtLeast(0)

        // Date Font Size
        val minDateFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 8f, res.displayMetrics)
        val maxDateFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 40f, res.displayMetrics)
        dateFontSeekBar.max = (maxDateFontSizePx - minDateFontSizePx).toInt()
        val defaultDateFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, res.displayMetrics).toInt()
        val currentDateFontSizePx = prefs.getInt("dateFontSize", defaultDateFontSizePx).toFloat()
        dateFontSeekBar.progress = (currentDateFontSizePx - minDateFontSizePx).toInt().coerceAtLeast(0)

        // Date Format
        val savedFormatValue = prefs.getString("dateFormat", "yyyy-MM-dd (E)")
        val savedFormatIndex = dateFormats.indexOfFirst { it.second == savedFormatValue }.coerceAtLeast(0)
        dateFormatSpinner.setSelection(savedFormatIndex)
        if (savedFormatValue == CUSTOM_FORMAT_KEY) {
            customDateFormatEdit.setText(prefs.getString("customDateFormat", ""))
        }

        // Other settings
        showSecondsSwitch.isChecked = prefs.getBoolean("showSeconds", true)
        nightModeSwitch.isChecked = prefs.getBoolean("nightMode", false)
        iconWhiteSwitch.isChecked = prefs.getBoolean("iconWhite", false)

        // --- Spinner Item Selection Logic ---
        dateFormatSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedKey = dateFormats[position].second
                val isCustom = selectedKey == CUSTOM_FORMAT_KEY
                val isHidden = selectedKey == HIDE_DATE_KEY

                customDateFormatEdit.visibility = if (isCustom) View.VISIBLE else View.GONE
                dateFontSizeLabel.visibility = if (isHidden) View.GONE else View.VISIBLE
                dateFontSeekBar.visibility = if (isHidden) View.GONE else View.VISIBLE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // --- Save Button Logic ---
        saveButton.setOnClickListener {
            with(prefs.edit()) {
                putInt("fontSize", (minFontSizePx + fontSeekBar.progress).toInt())
                putBoolean("showSeconds", showSecondsSwitch.isChecked)
                putBoolean("nightMode", nightModeSwitch.isChecked)
                putBoolean("iconWhite", iconWhiteSwitch.isChecked)

                val selectedFormatKey = dateFormats[dateFormatSpinner.selectedItemPosition].second
                putString("dateFormat", selectedFormatKey)

                if (selectedFormatKey == CUSTOM_FORMAT_KEY) {
                    putString("customDateFormat", customDateFormatEdit.text.toString())
                }

                if (selectedFormatKey != HIDE_DATE_KEY) {
                    putInt("dateFontSize", (minDateFontSizePx + dateFontSeekBar.progress).toInt())
                }

                apply()
            }

            (activity as? MainActivity)?.applyClockSettings()

            savedLabel.text = "✔ 저장됨"
            savedLabel.visibility = View.VISIBLE
            savedLabel.postDelayed({ dismiss() }, 500)
        }

        return view
    }
}
