package com.wowwatchdog.app.data.api

import com.wowwatchdog.app.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ApiKeyInterceptor @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Skip API key for health endpoint
        if (original.url.encodedPath == "/api/v1/health") {
            return chain.proceed(original)
        }

        val apiKey = runBlocking {
            settingsDataStore.connectionConfig.first().apiKey
        }

        val request = original.newBuilder()
            .header("X-API-Key", apiKey)
            .build()

        return chain.proceed(request)
    }
}
