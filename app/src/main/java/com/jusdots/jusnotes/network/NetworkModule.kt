package com.jusdots.jusnotes.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * NetworkModule — singleton Retrofit instance for OpenRouter API.
 * Extracted from NoteViewModel companion to keep network infra out of ViewModels.
 */
object NetworkModule {
    private const val BASE_URL = "https://openrouter.ai/api/v1/"

    private val client by lazy {
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply { level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: OpenRouterApi by lazy { retrofit.create(OpenRouterApi::class.java) }
}
