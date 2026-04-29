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
