package com.baidu.tv.player

import android.app.Application
import android.content.Context
import timber.log.Timber

/**
 * 应用程序类
 * 初始化全局状态，如日志、缓存等
 */
class BaiduTVApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化日志（开发模式）
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 初始化其他全局组件
        initGlobalComponents()
    }

    private fun initGlobalComponents() {
        // 初始化缓存、数据库等
    }

    companion object {
        @Volatile
        private var instance: BaiduTVApplication? = null

        fun getContext(): Context {
            return instance ?: throw IllegalStateException("BaiduTVApplication not initialized")
        }
    }
}