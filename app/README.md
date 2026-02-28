# The Changelog — Android (Kotlin)

Kotlin/Compose port of the iOS SwiftUI app. Feature-for-feature parity.

## Project Structure

```
app/src/main/java/app/thechangelog/
├── MainActivity.kt              # Entry point, root nav (mirrors changelogApp.swift)
├── ChangelogApp.kt              # Application class, store init
├── data/
│   ├── api/
│   │   └── APIService.kt        # Ktor HTTP client (mirrors iOS APIService.swift)
│   ├── model/
│   │   └── Models.kt            # Article, Category, Stats, Config data classes
│   └── store/
│       ├── DeviceTokenStore.kt  # Device registration + token (mirrors DeviceTokenManager)
│       ├── StatsStore.kt        # Read/skip/session tracking (mirrors StatsStore)
│       ├── CategoryStore.kt     # Selected categories (mirrors CategoryStore)
│       ├── ReadArticlesStore.kt # Read history (mirrors ReadArticlesStore)
│       └── AppConfigService.kt  # Maintenance/force-update config
└── ui/
    ├── theme/
    │   └── Theme.kt             # Dracula palette, typography, spacing (mirrors Theme.swift)
    ├── components/
    │   ├── CyberComponents.kt   # CyberButton, CyberBadge, CyberChip, CyberLoader
    │   ├── CyberBackground.kt   # Grid + atmospheric glow background
    │   └── AdManager.kt        # AdMob interstitial (mirrors iOS AdManager)
    ├── home/
    │   ├── HomeScreen.kt        # Card stack, states, toolbar (mirrors HomeView)
    │   └── HomeViewModel.kt     # Article loading, dismiss/open logic
    ├── article/
    │   └── ArticleCardView.kt   # Swipe card with image/content (mirrors ArticleCardView)
    ├── onboarding/
    │   └── OnboardingScreen.kt  # 5 feature pages + channel picker (mirrors OnboardingView)
    ├── settings/
    │   └── SettingsScreen.kt    # Stats section + settings rows (mirrors SettingsView)
    ├── splash/
    │   └── SplashScreen.kt      # Delta Δ logo animation (mirrors SplashView)
    └── others/
        └── OtherScreens.kt      # Maintenance + ForceUpdate screens
```

## Setup

### 1. Dependencies (gradle/libs.versions.toml)
All dependencies declared in the version catalog. Key choices:
- **Ktor** instead of Retrofit (better coroutine support, mirrors URLSession style)
- **Coil** for async image loading (replaces AsyncImage from iOS)
- **DataStore Preferences** instead of SharedPreferences (mirrors UserDefaults)
- **Compose Material3** for all UI

### 2. Before building
```
# In app/build.gradle.kts — already configured:
# debug → https://dev-api.thechangelog.app/api/v1
# release → https://api.thechangelog.app/api/v1
```

### 3. AdMob
Replace test IDs before release:
- `AndroidManifest.xml` → `APPLICATION_ID`  
- `AdManager.kt` → `AD_UNIT_ID`

### 4. Minimum SDK
API 26 (Android 8.0) — matches modern device targeting.

## iOS → Android equivalents

| iOS | Android |
|-----|---------|
| `UserDefaults` | `DataStore Preferences` |
| `@StateObject` / `@ObservedObject` | `StateFlow` + `collectAsState()` |
| `SwiftUI View` | `@Composable` function |
| `URLSession` | `Ktor HttpClient` |
| `AsyncImage` | `coil.compose.AsyncImage` |
| `NavigationStack` | `AnimatedContent` state machine |
| `NWPathMonitor` | `ConnectivityManager.NetworkCallback` |
| `UIApplication.open(url)` | `LocalUriHandler.openUri()` |
| `TabView(.page)` | `HorizontalPager` |
| `LazyVGrid` | `LazyVerticalGrid` |
| `withAnimation` | `animateFloatAsState` / `AnimatedVisibility` |

## Notes
- Onboarding uses `HorizontalPager` (Compose Foundation) — same page-swipe feel as iOS `TabView`
- Card swipe uses `detectHorizontalDragGestures` — mirrors iOS dual gesture system
- All Dracula colors match iOS `Theme.swift` exactly (hex values preserved)
- Stats sync timer runs every 30s matching iOS behavior
