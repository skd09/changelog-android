package com.sharvari.changelog.ui.activity.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sharvari.changelog.service.device.DeviceService
import com.sharvari.changelog.store.config.AppConfigService
import com.sharvari.changelog.store.bookmark.BookmarkStore
import com.sharvari.changelog.ui.activity.main.ChangelogApp
import com.sharvari.changelog.ui.theme.ChangelogTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            val appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
            CoroutineScope(Dispatchers.IO).launch {
                AppConfigService.fetch()
                DeviceService.shared.registerIfNeeded(appVersion)
            }
            BookmarkStore.init(this)
        }

        setContent { ChangelogTheme { ChangelogApp() } }
    }
}
