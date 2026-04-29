# Phase 2: Hilt Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace manual ViewModelFactory instantiation with Hilt dependency injection. All ViewModel creation moves from MainActivity to `@HiltViewModel` with `@Inject constructor`. Navigation.kt parameters drop from 15 to ~5.

**Architecture:** Hilt `@HiltAndroidApp` on QingApplication. Four DI modules provide singletons (PreferencesManager, Database, Repositories, Network). ViewModels use `@HiltViewModel` + `@Inject constructor`. Screens use `hiltViewModel()` internally.

**Tech Stack:** Hilt (Dagger), Hilt Navigation Compose, KSP

**Prerequisite:** Phase 1 (Room Data Layer) must be complete.

---

### Task 2.1: Add Hilt Dependencies and Plugin

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Update version catalog**

In `gradle/libs.versions.toml`, add:

```toml
# Add to [versions]
hilt = "2.56.2"
hiltNavigationCompose = "1.2.0"

# Add to [libraries]
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }

# Add to [plugins]
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 2: Update app/build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
```

Add to dependencies:

```kotlin
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
```

- [ ] **Step 3: Sync and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add Hilt dependencies and plugin"
```

---

### Task 2.2: Annotate Application and Create DI Modules

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/QingApplication.kt`
- Create: `app/src/main/java/com/onmi/qing/di/AppModule.kt`
- Create: `app/src/main/java/com/onmi/qing/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/onmi/qing/di/RepositoryModule.kt`
- Create: `app/src/main/java/com/onmi/qing/di/NetworkModule.kt`

- [ ] **Step 1: Annotate QingApplication**

```kotlin
package com.onmi.qing

import android.app.Application
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QingApplication : Application() {

    // These are now provided by Hilt modules, but keep for migration compatibility
    // They will be removed once all consumers use @Inject
    lateinit var dataStore: QingDataStore
        private set

    lateinit var demoModeManager: DemoModeManager
        private set

    override fun onCreate() {
        super.onCreate()
        dataStore = QingDataStore(this)
        demoModeManager = DemoModeManager(this)
    }
}
```

- [ ] **Step 2: Create AppModule**

```kotlin
package com.onmi.qing.di

import android.content.Context
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideQingDataStore(@ApplicationContext context: Context): QingDataStore {
        return QingDataStore(context)
    }

    @Provides
    @Singleton
    fun provideDemoModeManager(@ApplicationContext context: Context): DemoModeManager {
        return DemoModeManager(context)
    }
}
```

- [ ] **Step 3: Create DatabaseModule**

```kotlin
package com.onmi.qing.di

import android.content.Context
import androidx.room.Room
import com.onmi.qing.data.local.AppDatabase
import com.onmi.qing.data.local.dao.AchievementDao
import com.onmi.qing.data.local.dao.ChatDao
import com.onmi.qing.data.local.dao.MoodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "qing_database"
        ).build()
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    fun provideMoodDao(database: AppDatabase): MoodDao {
        return database.moodDao()
    }

    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao {
        return database.achievementDao()
    }
}
```

- [ ] **Step 4: Create RepositoryModule**

```kotlin
package com.onmi.qing.di

import com.onmi.qing.data.local.dao.AchievementDao
import com.onmi.qing.data.local.dao.ChatDao
import com.onmi.qing.data.local.dao.MoodDao
import com.onmi.qing.data.repository.AchievementRepository
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.data.repository.MoodRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideChatRepository(chatDao: ChatDao): ChatRepository {
        return ChatRepository(chatDao)
    }

    @Provides
    @Singleton
    fun provideMoodRepository(moodDao: MoodDao): MoodRepository {
        return MoodRepository(moodDao)
    }

    @Provides
    @Singleton
    fun provideAchievementRepository(achievementDao: AchievementDao): AchievementRepository {
        return AchievementRepository(achievementDao)
    }
}
```

- [ ] **Step 5: Create NetworkModule**

```kotlin
package com.onmi.qing.di

import com.onmi.qing.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }
        val anthropicInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(anthropicInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
}
```

- [ ] **Step 6: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/onmi/qing/QingApplication.kt \
       app/src/main/java/com/onmi/qing/di/
git commit -m "feat: add Hilt @HiltAndroidApp and DI modules"
```

---

### Task 2.3: Create ApiServiceFactory

**Files:**
- Create: `app/src/main/java/com/onmi/qing/data/remote/ApiServiceFactory.kt`
- Modify: `app/src/main/java/com/onmi/qing/di/NetworkModule.kt`

- [ ] **Step 1: Create ApiServiceFactory**

```kotlin
package com.onmi.qing.data.remote

import com.onmi.qing.data.datastore.QingDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiServiceFactory @Inject constructor(
    private val preferencesManager: QingDataStore,
    private val okHttpClient: OkHttpClient
) {
    private var currentBaseUrl: String? = null
    private var cachedRetrofit: Retrofit? = null

    fun <T> create(serviceClass: Class<T>): T {
        val baseUrl = runBlocking { preferencesManager.userPreferences.first().apiUrl }
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (normalizedUrl != currentBaseUrl || cachedRetrofit == null) {
            currentBaseUrl = normalizedUrl
            cachedRetrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return cachedRetrofit!!.create(serviceClass)
    }

    inline fun <reified T> create(): T = create(T::class.java)
}
```

- [ ] **Step 2: Add ApiServiceFactory to NetworkModule**

Add to `NetworkModule.kt`:

```kotlin
    @Provides
    @Singleton
    fun provideApiServiceFactory(
        preferencesManager: QingDataStore,
        okHttpClient: OkHttpClient
    ): ApiServiceFactory {
        return ApiServiceFactory(preferencesManager, okHttpClient)
    }
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/remote/ApiServiceFactory.kt \
       app/src/main/java/com/onmi/qing/di/NetworkModule.kt
git commit -m "feat: add ApiServiceFactory for shared Retrofit creation"
```

---

### Task 2.4: Create UsageStatsManager

**Files:**
- Create: `app/src/main/java/com/onmi/qing/data/UsageStatsManager.kt`
- Modify: `app/src/main/java/com/onmi/qing/di/AppModule.kt`

- [ ] **Step 1: Create UsageStatsManager**

```kotlin
package com.onmi.qing.data

import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.datastore.UsageStats
import com.onmi.qing.data.demo.DemoModeManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsManager @Inject constructor(
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager
) {
    val usageStats: Flow<UsageStats> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.usageStats,
        dataStore.usageStats
    ) { isDemo, demoStats, userStats ->
        if (isDemo) demoStats else userStats
    }

    suspend fun incrementChatCount() {
        if (demoModeManager.isDemoMode.value) return
        dataStore.incrementChatCount()
        dataStore.resetDailyActivitiesIfNewDay()
        dataStore.markTodayChat()
    }

    suspend fun incrementBreathingCount() {
        if (demoModeManager.isDemoMode.value) return
        dataStore.incrementBreathingCount()
        dataStore.resetDailyActivitiesIfNewDay()
        dataStore.markTodayBreathing()
    }

    suspend fun incrementCheckInCount(hour: Int = -1) {
        if (demoModeManager.isDemoMode.value) return
        dataStore.incrementCheckInCount()
        dataStore.resetDailyActivitiesIfNewDay()
        dataStore.markTodayCheckin()
        if (hour in 0..7) {
            dataStore.checkAndUnlockEarlyBird(hour)
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/UsageStatsManager.kt
git commit -m "feat: add UsageStatsManager singleton"
```

---

### Task 2.5: Migrate SettingsViewModel to @HiltViewModel

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/viewmodel/SettingsViewModel.kt`

- [ ] **Step 1: Rewrite SettingsViewModel**

Read the current `SettingsViewModel.kt` first, then replace the constructor and remove the Factory:

```kotlin
// Add imports at top:
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Change class declaration:
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: QingDataStore
) : ViewModel() {
    // ... keep all existing logic ...

    // DELETE the entire Factory companion object
}
```

- [ ] **Step 2: Update MainActivity.kt**

Remove the manual SettingsViewModel creation:
```kotlin
// DELETE these lines:
//    val settingsViewModel: SettingsViewModel = viewModel(
//        factory = SettingsViewModel.Factory(application.dataStore)
//    )
```

Update Navigation.kt to use `hiltViewModel()`:
```kotlin
        composable(Screen.Settings.route) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                // ... rest of params
            )
        }
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/viewmodel/SettingsViewModel.kt \
       app/src/main/java/com/onmi/qing/MainActivity.kt \
       app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt
git commit -m "refactor: migrate SettingsViewModel to @HiltViewModel"
```

---

### Task 2.6: Migrate Remaining ViewModels to @HiltViewModel

**Files:**
- Modify: All remaining ViewModels
- Modify: `app/src/main/java/com/onmi/qing/MainActivity.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt`

For each ViewModel: add `@HiltViewModel`, add `@Inject constructor`, remove `Factory` companion object.

- [ ] **Step 1: Migrate HomeViewModel**

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dataStore: QingDataStore
) : ViewModel() {
    // ... keep existing logic, delete Factory
}
```

- [ ] **Step 2: Migrate MoodViewModel**

```kotlin
@HiltViewModel
class MoodViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val moodRepository: MoodRepository,
    private val demoModeManager: DemoModeManager
) : ViewModel() {
    // Replace dataStore.moodEntries → moodRepository.getAllEntries()
    // Replace dataStore.latestMood → moodRepository.getLatestEntry()
    // Replace dataStore.addMoodEntry → moodRepository.addEntry
    // Replace dataStore.deleteMoodEntry → moodRepository.deleteEntry
    // Replace dataStore.updateMoodEntry → moodRepository.updateEntry
    // Delete Factory
}
```

- [ ] **Step 3: Migrate SbtiViewModel**

```kotlin
@HiltViewModel
class SbtiViewModel @Inject constructor() : ViewModel() {
    // Pure logic, no dependencies. Just delete Factory.
}
```

- [ ] **Step 4: Migrate MbtiViewModel**

```kotlin
@HiltViewModel
class MbtiViewModel @Inject constructor() : ViewModel() {
    // Pure logic, no dependencies. Just delete Factory.
}
```

- [ ] **Step 5: Migrate StressDetectionViewModel**

```kotlin
@HiltViewModel
class StressDetectionViewModel @Inject constructor(
    @android.app.Application private val application: android.app.Application,
    private val demoModeManager: DemoModeManager
) : ViewModel() {
    // Delete Factory
}
```

Note: This VM needs `Context` for BLE. Use `@ApplicationContext` if possible, or `@android.app.Application`.

- [ ] **Step 6: Migrate StateViewModel**

```kotlin
@HiltViewModel
class StateViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager,
    private val achievementRepository: AchievementRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {
    // Replace dataStore.allAchievements → achievementRepository.getAll()
    // Replace dataStore.unlockedCount → achievementRepository.getUnlockedCount()
    // Replace dataStore.unlockAchievement → achievementRepository.unlock
    // Replace dataStore.lockAchievement → achievementRepository.lock
    // Replace dataStore.getMessagesForSession → chatRepository.getMessagesForSessionOnce
    // Delete Factory
}
```

- [ ] **Step 7: Migrate ChatViewModel**

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val chatRepository: ChatRepository,
    private val apiServiceFactory: ApiServiceFactory,
    private val usageStatsManager: UsageStatsManager,
    private val demoModeManager: DemoModeManager
) : ViewModel() {
    // Replace initializeApiService() to use apiServiceFactory.create<ChatApiService>()
    // Replace dataStore.createSession → chatRepository.createSession
    // Replace dataStore.addMessage → chatRepository.addMessage
    // Replace stateViewModel?.incrementChatCount() → usageStatsManager.incrementChatCount()
    // Replace dataStore.getMessagesForSession → chatRepository.getMessagesForSession
    // Delete Factory
}
```

- [ ] **Step 8: Update MainActivity.kt — remove all manual ViewModel creation**

```kotlin
// DELETE all viewModel() calls (lines 181-204)
// The ViewModels are now created by Hilt inside each composable
```

- [ ] **Step 9: Update Navigation.kt — use hiltViewModel() in each composable**

```kotlin
@Composable
fun QingNavHost(
    navController: NavHostController,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(...) {
        composable(Screen.Home.route) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            val stateViewModel = hiltViewModel<StateViewModel>()
            val moodViewModel = hiltViewModel<MoodViewModel>()
            val demoModeManager = ... // Get from LocalContext or Hilt
            HomeScreen(
                viewModel = homeViewModel,
                stateViewModel = stateViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                // ... navigation callbacks
            )
        }
        // ... similar for all other routes
    }
}
```

For `DemoModeManager`, since it's a `@Singleton` provided by Hilt, access it via `hiltViewModel()` or inject it into a wrapper. The simplest approach: create a `@HiltViewModel` wrapper or access it from the Application context.

Actually, the cleanest approach is to pass `DemoModeManager` as a parameter to `QingNavHost` from `MainActivity` (where it's available from the Application), keeping it out of the NavHost's ViewModel dependencies.

- [ ] **Step 10: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "refactor: migrate all ViewModels to @HiltViewModel"
```

---

### Task 2.7: Clean Up MainActivity and Application

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/MainActivity.kt`
- Modify: `app/src/main/java/com/onmi/qing/QingApplication.kt`

- [ ] **Step 1: Remove legacy properties from QingApplication**

Now that all ViewModels use Hilt injection, remove:
- `lateinit var dataStore` (keep if still needed for migration in Phase 1)
- `lateinit var demoModeManager` (keep for now, used in QingApp composable)

Actually, `QingApplication` still needs `dataStore` and `demoModeManager` for the `QingApp` composable and the `DataMigration`. Keep them for now.

- [ ] **Step 2: Simplify QingApp composable**

Remove `application` parameter from `QingApp`. Get `DemoModeManager` from the Hilt-provided singleton instead. Since `QingApp` is a composable (not a ViewModel), we can't use `@Inject` directly. Options:
1. Keep passing `demoModeManager` from `MainActivity` (simplest)
2. Use `LocalContext.current.applicationContext as QingApplication` to access it

Option 1 is cleaner for now. Keep `demoModeManager` as a parameter.

- [ ] **Step 3: Verify build and test**

Run: `./gradlew assembleDebug`
Run: `./gradlew test`
Expected: All passes

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: Phase 2 complete — Hilt integration"
```
