package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.remote.AnthropicMessage
import com.onmi.qing.data.remote.AnthropicRequest
import com.onmi.qing.data.remote.ChatApiService
import com.onmi.qing.data.repository.AchievementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.onmi.qing.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Settings ViewModel
class SettingsViewModel(
    private val dataStore: QingDataStore,
    private val achievementRepository: AchievementRepository
) : ViewModel() {

    companion object {
        const val DEFAULT_API_URL = "https://api.xiaoqing.com"
    }

    // Theme preference from DataStore
    private val _isDarkThemeInternal = MutableStateFlow(false)
    val isDarkThemeInternal: StateFlow<Boolean> = _isDarkThemeInternal.asStateFlow()

    private val _apiUrlInternal = MutableStateFlow(DEFAULT_API_URL)
    val apiUrlInternal: StateFlow<String> = _apiUrlInternal.asStateFlow()

    private val _userNameInternal = MutableStateFlow("同学")
    val userNameInternal: StateFlow<String> = _userNameInternal.asStateFlow()

    private val _userDescriptionInternal = MutableStateFlow("正在使用小晴心理健康助手")
    val userDescriptionInternal: StateFlow<String> = _userDescriptionInternal.asStateFlow()

    private val _chatCount = MutableStateFlow(0)
    val chatCount: StateFlow<Int> = _chatCount.asStateFlow()

    private val _breathingCount = MutableStateFlow(0)
    val breathingCount: StateFlow<Int> = _breathingCount.asStateFlow()

    private val _checkInCount = MutableStateFlow(0)
    val checkInCount: StateFlow<Int> = _checkInCount.asStateFlow()

    private val _achievementUnlockedInternal = MutableStateFlow(0)
    val achievementUnlockedInternal: StateFlow<Int> = _achievementUnlockedInternal.asStateFlow()

    val usageStats = dataStore.usageStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.onmi.qing.data.datastore.UsageStats())

    val achievementUnlocked = achievementRepository.getUnlockedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            dataStore.userPreferences.collect { prefs ->
                _isDarkThemeInternal.value = prefs.isDarkTheme
                _apiUrlInternal.value = prefs.apiUrl
                _userNameInternal.value = prefs.userName
                _userDescriptionInternal.value = prefs.userDescription
            }
        }
        viewModelScope.launch {
            dataStore.usageStats.collect { stats ->
                _chatCount.value = stats.chatCount
                _breathingCount.value = stats.breathingCount
                _checkInCount.value = stats.checkInCount
            }
        }
        viewModelScope.launch {
            achievementRepository.getUnlockedCount().collect { count ->
                _achievementUnlockedInternal.value = count
            }
        }
    }

// Toggle dark/light theme
    fun toggleTheme() {
        viewModelScope.launch {
            val newTheme = !_isDarkThemeInternal.value
            dataStore.updateTheme(newTheme)
            _isDarkThemeInternal.value = newTheme
        }
    }

// Set theme directly
    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            dataStore.updateTheme(isDark)
            _isDarkThemeInternal.value = isDark
        }
    }

// Update API URL
    fun updateApiUrl(url: String) {
        viewModelScope.launch {
            dataStore.updateApiUrl(url)
            _apiUrlInternal.value = url
        }
    }

    // Restore default API URL
    fun restoreDefaultApiUrl() {
        viewModelScope.launch {
            dataStore.updateApiUrl(DEFAULT_API_URL)
            _apiUrlInternal.value = DEFAULT_API_URL
        }
    }

// Update user name
    fun updateUserName(name: String) {
        viewModelScope.launch {
            dataStore.updateUserProfile(name, _userDescriptionInternal.value)
            _userNameInternal.value = name
        }
    }

    // Update user description
    fun updateUserDescription(description: String) {
        viewModelScope.launch {
            dataStore.updateUserProfile(_userNameInternal.value, description)
            _userDescriptionInternal.value = description
        }
    }

    // Update state counts from StateViewModel
    fun updateStateCounts(
        chatCount: Int,
        breathingCount: Int,
        checkInCount: Int,
        achievementUnlocked: Int
    ) {
        _chatCount.value = chatCount
        _breathingCount.value = breathingCount
        _checkInCount.value = checkInCount
        _achievementUnlockedInternal.value = achievementUnlocked
    }

    // Get total state count for display
    fun getTotalStateCount(): Int {
        return _chatCount.value + _breathingCount.value + _checkInCount.value + _achievementUnlockedInternal.value
    }

    // 重置心理学维度分
    fun resetDimensionScores(onReset: () -> Unit) {
        viewModelScope.launch {
            dataStore.resetPsychologyDimensions()
            onReset()
        }
    }

    // Clear all personal state data
    fun clearAllState(onCleared: () -> Unit) {
        viewModelScope.launch {
            // Clear DataStore
            dataStore.clearAllData()
            // Reset all local StateFlows to defaults
            _isDarkThemeInternal.value = false
            _apiUrlInternal.value = DEFAULT_API_URL
            _userNameInternal.value = "同学"
            _userDescriptionInternal.value = "正在使用小晴心理健康助手"
            _chatCount.value = 0
            _breathingCount.value = 0
            _checkInCount.value = 0
            _achievementUnlockedInternal.value = 0
            onCleared()
        }
    }

    // 测试 API 连接
    suspend fun testApiConnection(apiUrl: String): String = withContext(Dispatchers.IO) {
        try {
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
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(anthropicInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            // Ensure baseUrl ends with "/"
            val baseUrl = if (apiUrl.endsWith("/")) apiUrl else "$apiUrl/"
            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(ChatApiService::class.java)
            val testRequest = AnthropicRequest(
                model = "glm-4.5-air",
                messages = listOf(AnthropicMessage(role = "user", content = "Hi")),
                max_tokens = 10,
                stream = false
            )
            val response = service.chat(testRequest)
            if (response.isSuccessful) {
                val content = response.body()?.content?.firstOrNull()?.text
                if (!content.isNullOrEmpty()) {
                    "连接成功！AI回复: $content"
                } else {
                    "连接成功！但AI回复为空"
                }
            } else {
                "连接失败: ${response.code()} ${response.message()}"
            }
        } catch (e: Exception) {
            "连接异常: ${e.message}"
        }
    }

    class Factory(
        private val dataStore: QingDataStore,
        private val achievementRepository: AchievementRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(dataStore, achievementRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
