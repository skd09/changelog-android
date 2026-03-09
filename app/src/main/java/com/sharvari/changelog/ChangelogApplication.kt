package com.sharvari.changelog

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import com.sharvari.changelog.data.store.CategoryStore
import com.sharvari.changelog.data.store.DeviceTokenStore
import com.sharvari.changelog.data.store.FcmTokenStore
import com.sharvari.changelog.data.store.ReadArticlesStore
import com.sharvari.changelog.data.store.StatsStore
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import java.util.concurrent.TimeUnit

class ChangelogApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // DeviceTokenStore and FcmTokenStore use SharedPreferences (synchronous).
        // Init just provides context — no async needed, no race condition.
        DeviceTokenStore.init(applicationContext)
        FcmTokenStore.init(applicationContext)

        // Remaining stores use DataStore (async) — init on IO thread.
        CoroutineScope(Dispatchers.IO).launch {
            StatsStore.init(applicationContext)
            CategoryStore.init(applicationContext)
            ReadArticlesStore.init(applicationContext)
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent",
                            "Mozilla/5.0 (Linux; Android 14; Pixel 8) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/120.0.0.0 Mobile Safari/537.36")
                        .header("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                        .header("Referer", "https://www.google.com/")
                        .build()
                )
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this)
                .components { add(OkHttpNetworkFetcherFactory(callFactory = okHttpClient)) }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizePercent(this@ChangelogApplication, 0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache").toOkioPath())
                        .maxSizeBytes(50L * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .build()
        }

        MobileAds.initialize(this)
    }
}