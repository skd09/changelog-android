package com.sharvari.changelog.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.sharvari.changelog.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

object AdManager {

    private val AD_UNIT_ID = BuildConfig.AD_UNIT_ID
    const val AD_FREQUENCY = 9

    private var interstitialAd: InterstitialAd? = null
    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady

    private var retryAttempt = 0
    private const val MAX_RETRIES = 3
    private val handler = Handler(Looper.getMainLooper())

    fun loadAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isAdReady.value = true
                    retryAttempt = 0
                    Timber.d("Interstitial ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _isAdReady.value = false
                    Timber.w("Ad failed to load: %s", error.message)
                    retryWithBackoff(context)
                }
            }
        )
    }

    fun showAd(activity: Activity, onDismiss: () -> Unit = {}) {
        val ad = interstitialAd ?: run {
            loadAd(activity)
            onDismiss()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _isAdReady.value = false
                retryAttempt = 0
                onDismiss()
                loadAd(activity)
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                Timber.w("Ad failed to show: %s", e.message)
                interstitialAd = null
                _isAdReady.value = false
                onDismiss()
                loadAd(activity)
            }
        }
        ad.show(activity)
    }

    // ── Retry with exponential backoff ──────────────────────────────────────

    private fun retryWithBackoff(context: Context) {
        if (retryAttempt >= MAX_RETRIES) {
            Timber.d("Ad max retries reached, will try on next show")
            return
        }
        retryAttempt++
        val delay = retryAttempt * retryAttempt * 5000L // 5s, 20s, 45s
        handler.postDelayed({ loadAd(context) }, delay)
    }
}
