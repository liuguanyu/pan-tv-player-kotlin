package com.baidu.tv.player.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit客户端工厂
 *
 * 提供创建Retrofit实例的工厂方法
 * 支持OAuth和普通API两种客户端
 */
object RetrofitClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
        .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    /**
     * 获取OAuth API的Retrofit实例
     * 用于设备码和token相关接口
     */
    fun getOAuthInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.OAUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    /**
     * 获取普通API的Retrofit实例
     * 用于文件操作等接口
     */
    fun getApiInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConstants.PAN_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}