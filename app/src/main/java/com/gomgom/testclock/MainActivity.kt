package com.gomgom.testclock

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var clockView: TextView
    private lateinit var dateView: TextView
    private var clockJob: Job? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd (E)", Locale.getDefault())
    private var showSeconds = true
    private lateinit var settingsIcon: ImageButton
    private lateinit var removeAdsIcon: ImageButton

    // Ad & Billing
    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null
    private var rewardedAd: RewardedAd? = null
    private lateinit var billingManager: BillingManager
    private var isAdFree = false
    private val TAG = "MainActivity"

    // TODO: AdMob에서 발급받은 실제 광고 단위 ID로 교체하세요.
    private val AD_UNIT_ID_BANNER = "ca-app-pub-3940256099942544/6300978111" // 테스트 배너 ID
    private val AD_UNIT_ID_REWARDED = "ca-app-pub-3940256099942544/5224354917" // 테스트 보상형 ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // 화면 꺼짐 방지
        // --- Ad & Billing Initialization ---
        MobileAds.initialize(this) {}
        billingManager = BillingManager(this)
        checkFirstLaunch()
        loadRewardedAd()

        // --- Layout Setup ---
        val rootLayout = RelativeLayout(this)
        val clockLayout = FrameLayout(this).apply { id = View.generateViewId() }
        adContainer = FrameLayout(this).apply { id = View.generateViewId() }

        // Clock & Date Views
        dateView = TextView(this).apply { typeface = Typeface.MONOSPACE }
        val dateParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = 30
            leftMargin = 30
        }
        clockView = TextView(this).apply { typeface = Typeface.MONOSPACE }
        val clockParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER
        }
        clockLayout.addView(dateView, dateParams)
        clockLayout.addView(clockView, clockParams)

        // Settings Icon
        settingsIcon = ImageButton(this).apply {
            setImageResource(R.drawable.ic_settings)
            setBackgroundColor(Color.TRANSPARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { showSettingsSheet() }
        }
        val iconSize = resources.displayMetrics.widthPixels / 10
        val siconparams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            bottomMargin = 30
            rightMargin = 30
        }
        clockLayout.addView(settingsIcon, siconparams)

        // Remove Ads Icon
        removeAdsIcon = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel) // Placeholder icon
            setBackgroundColor(Color.TRANSPARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnClickListener { showRemoveAdsDialog() }
        }
        val removeAdsParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            bottomMargin = 30
            leftMargin = 30
        }
        clockLayout.addView(removeAdsIcon, removeAdsParams)

        // --- Assemble Root Layout ---
        val adParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        }
        rootLayout.addView(adContainer, adParams)

        val clockLayoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
            addRule(RelativeLayout.ABOVE, adContainer.id)
        }
        rootLayout.addView(clockLayout, clockLayoutParams)

        setContentView(rootLayout)

        // Observe premium status
        lifecycleScope.launch {
            billingManager.isPremium.collect { premium ->
                isAdFree = premium
                updateAdVisibility()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyClockSettings()
        startClock()
        updateAdVisibility()
    }

    override fun onPause() {
        super.onPause()
        clockJob?.cancel()
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = lifecycleScope.launch {
            var lastTime = ""
            var lastDate = ""
            while (isActive) {
                val now = System.currentTimeMillis()
                if (lastTime != timeFormat.format(Date(now))) {
                    clockView.text = timeFormat.format(Date(now))
                    lastTime = timeFormat.format(Date(now))
                }
                if (lastDate != dateFormat.format(Date(now))) {
                    dateView.text = dateFormat.format(Date(now))
                    lastDate = dateFormat.format(Date(now))
                }
                val delayMs = if (showSeconds) 1000 - (now % 1000) else 60000 - (now % 60000)
                delay(delayMs)
            }
        }
    }

    fun applyClockSettings() {
        val prefs = getSharedPreferences("clock_prefs", MODE_PRIVATE)
        val defaultFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 48f, resources.displayMetrics).toInt()
        val defaultDateFontSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20f, resources.displayMetrics).toInt()
        val fontSize = prefs.getInt("fontSize", defaultFontSizePx)
        val dateFontSize = prefs.getInt("dateFontSize", defaultDateFontSizePx)
        this.showSeconds = prefs.getBoolean("showSeconds", true)
        val nightMode = prefs.getBoolean("nightMode", false)
        val iconWhite = prefs.getBoolean("iconWhite", false)
        val dateFormatSetting = prefs.getString("dateFormat", "yyyy-MM-dd (E)")

        clockView.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize.toFloat())
        dateView.setTextSize(TypedValue.COMPLEX_UNIT_PX, dateFontSize.toFloat())
        timeFormat.applyPattern(if (showSeconds) "HH:mm:ss" else "HH:mm")

        if (dateFormatSetting == "HIDE_DATE") {
            dateView.visibility = View.GONE
        } else {
            dateView.visibility = View.VISIBLE
            var pattern = if (dateFormatSetting == "CUSTOM_FORMAT") prefs.getString("customDateFormat", "yyyy-MM-dd (E)") else dateFormatSetting
            try {
                dateFormat.applyPattern(pattern)
            } catch (e: IllegalArgumentException) {
                dateFormat.applyPattern("yyyy-MM-dd (E)")
                Toast.makeText(this, getString(R.string.app_error_invalid_date_format), Toast.LENGTH_LONG).show()
            }
        }

        val bgColor = if (nightMode) Color.BLACK else Color.WHITE
        val textColor = if (nightMode) Color.WHITE else Color.BLACK
        val iconColor = if (iconWhite) Color.WHITE else Color.BLACK
        clockView.setTextColor(textColor)
        dateView.setTextColor(textColor)
        settingsIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        removeAdsIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
        (clockView.parent.parent as? View)?.setBackgroundColor(bgColor)
    }

    private fun showSettingsSheet() {
        val paint = Paint().apply { typeface = clockView.typeface }
        val textToMeasure = "00:00:00"
        val screenWidth = resources.displayMetrics.widthPixels
        val targetWidth = screenWidth * 0.95f
        var high = 2000f
        var low = 0f
        while (high - low > 1) {
            val mid = (high + low) / 2
            paint.textSize = mid
            if (paint.measureText(textToMeasure) > targetWidth) high = mid else low = mid
        }
        val sheet = SettingsBottomSheetFragment().apply {
            arguments = Bundle().apply { putFloat("maxFontSizePx", low) }
        }
        sheet.show(supportFragmentManager, "SettingsBottomSheet")
    }

    // --- Ad & Billing Logic ---

    private fun checkFirstLaunch() {
        val prefs = getSharedPreferences("clock_prefs", MODE_PRIVATE)
        var firstLaunchTime = prefs.getLong("firstLaunchTime", 0)
        if (firstLaunchTime == 0L) {
            firstLaunchTime = System.currentTimeMillis()
            prefs.edit().putLong("firstLaunchTime", firstLaunchTime).apply()
        }
    }

    private fun shouldShowAds(): Boolean {
        if (isAdFree) return false

        val prefs = getSharedPreferences("clock_prefs", MODE_PRIVATE)
        val firstLaunchTime = prefs.getLong("firstLaunchTime", 0)
        val adFreeUntil = prefs.getLong("adFreeUntil", 0)

        val isPastGracePeriod = System.currentTimeMillis() - firstLaunchTime > TimeUnit.DAYS.toMillis(1)
        val isRewardExpired = System.currentTimeMillis() > adFreeUntil

        return isPastGracePeriod && isRewardExpired
    }

    private fun updateAdVisibility() {
        if (shouldShowAds()) {
            removeAdsIcon.visibility = View.VISIBLE
            loadBannerAd()
        } else {
            removeAdsIcon.visibility = View.GONE
            adContainer.visibility = View.GONE
            adView?.destroy()
            adView = null
        }
    }

    private fun loadBannerAd() {
        adView = AdView(this)
        adView?.adUnitId = AD_UNIT_ID_BANNER
        adView?.setAdSize(AdSize.BANNER)
        adContainer.removeAllViews()
        adContainer.addView(adView)
        val adRequest = AdRequest.Builder().build()
        adView?.loadAd(adRequest)
        adContainer.visibility = View.VISIBLE
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, AD_UNIT_ID_REWARDED, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.toString())
                rewardedAd = null
            }
            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Ad was loaded.")
                rewardedAd = ad
            }
        })
    }

    private fun showRemoveAdsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.app_remove_ads_title))
            .setMessage(getString(R.string.app_remove_ads_message))
            .setPositiveButton(getString(R.string.app_remove_ads_watch_ad)) { _, _ ->
                showRewardedAd()
            }
            .setNegativeButton(getString(R.string.app_remove_ads_purchase)) { _, _ ->
                billingManager.launchPurchaseFlow(this)
            }
            .setNeutralButton(getString(R.string.app_remove_ads_cancel), null)
        builder.create().show()
    }

    private fun showRewardedAd() {
        rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewardedAd() // Preload next ad
            }
        }

        rewardedAd?.let { ad ->
            ad.show(this) { rewardItem ->
                val prefs = getSharedPreferences("clock_prefs", MODE_PRIVATE)
                val adFreeUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
                prefs.edit().putLong("adFreeUntil", adFreeUntil).apply()
                Toast.makeText(this, getString(R.string.app_reward_granted_message), Toast.LENGTH_SHORT).show()
                updateAdVisibility()
            }
        } ?: run {
            Toast.makeText(this, getString(R.string.app_reward_failed_message), Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }
}