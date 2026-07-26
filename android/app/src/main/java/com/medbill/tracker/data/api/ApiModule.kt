package com.medbill.tracker.data.api

import com.medbill.tracker.BuildConfig
import com.medbill.tracker.auth.SessionStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiModule {
    fun createApi(): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val authInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val token = SessionStore.accessToken.value
            val updated = if (token.isNullOrBlank()) {
                request
            } else {
                request.newBuilder().addHeader("Authorization", "Bearer $token").build()
            }
            chain.proceed(updated)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
