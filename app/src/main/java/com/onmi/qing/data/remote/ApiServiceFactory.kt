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
    private val regularClient: OkHttpClient,
    private val streamingClient: OkHttpClient
) {
    private var currentBaseUrl: String? = null
    private var regularRetrofit: Retrofit? = null
    private var streamingRetrofit: Retrofit? = null

    private fun getBaseUrl(): String {
        val baseUrl = runBlocking { preferencesManager.userPreferences.first().apiUrl }
        return if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }

    private fun getRegularRetrofit(): Retrofit {
        val baseUrl = getBaseUrl()
        if (regularRetrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            regularRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(regularClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return regularRetrofit!!
    }

    private fun getStreamingRetrofit(): Retrofit {
        val baseUrl = getBaseUrl()
        if (streamingRetrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            streamingRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(streamingClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return streamingRetrofit!!
    }

    fun createChatApiService(): ChatApiService {
        return getRegularRetrofit().create(ChatApiService::class.java)
    }

    fun createStreamingChatApiService(): ChatApiService {
        return getStreamingRetrofit().create(ChatApiService::class.java)
    }

    fun createAnalyzeApiService(): AnalyzeApiService {
        return getRegularRetrofit().create(AnalyzeApiService::class.java)
    }

    // Backward-compatible generic method (will be removed in Task 6)
    fun <T> create(serviceClass: Class<T>): T {
        val baseUrl = getBaseUrl()
        if (regularRetrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            regularRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(regularClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return regularRetrofit!!.create(serviceClass)
    }

    // Backward-compatible reified method (will be removed in Task 6)
    inline fun <reified T> create(): T = create(T::class.java)
}
