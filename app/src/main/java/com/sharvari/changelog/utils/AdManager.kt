package com.sharvari.changelog.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

object AdManager {

    // Replace with real ad unit ID before release
    private const val AD_UNIT_ID = "ca-app-pub-5701767605641071~1696951661" // test ID

    private var interstitialAd: InterstitialAd? = null
    private val _isAdReady = MutableStateFlow(false)
    val isAdReady: StateFlow<Boolean> = _isAdReady

    fun loadAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    _isAdReady.value = true
                    Timber.d("Interstitial ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    _isAdReady.value = false
                    Timber.w("Ad failed to load: %s", error.message)
                }
            }
        )
    }

    fun showAd(activity: Activity, onDismiss: () -> Unit = {}) {
        val ad = interstitialAd ?: run { onDismiss(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                _isAdReady.value = false
                onDismiss()
                loadAd(activity) // pre-load next
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                interstitialAd = null
                _isAdReady.value = false
                onDismiss()
            }
        }
        ad.show(activity)
    }
}
