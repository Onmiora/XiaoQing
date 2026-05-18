package com.onmi.qing.data.remote

import com.onmi.qing.data.datastore.QingDataStore
import kotlinx.coroutines.flow.first
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
    @Volatile private var currentBaseUrl: String? = null
    @Volatile private var regularRetrofit: Retrofit? = null
    @Volatile private var streamingRetrofit: Retrofit? = null

    private suspend fun getBaseUrl(): String {
        val baseUrl = preferencesManager.userPreferences.first().apiUrl
        return if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    }

    private suspend fun getRegularRetrofit(): Retrofit {
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

    private suspend fun getStreamingRetrofit(): Retrofit {
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

    suspend fun createChatApiService(): ChatApiService {
        return getRegularRetrofit().create(ChatApiService::class.java)
    }

    suspend fun createStreamingChatApiService(): ChatApiService {
        return getStreamingRetrofit().create(ChatApiService::class.java)
    }

    suspend fun createAnalyzeApiService(): AnalyzeApiService {
        return getRegularRetrofit().create(AnalyzeApiService::class.java)
    }

    // Backward-compatible generic method (will be removed in Task 6)
    suspend fun <T> create(serviceClass: Class<T>): T {
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
    suspend inline fun <reified T> create(): T = create(T::class.java)
}
