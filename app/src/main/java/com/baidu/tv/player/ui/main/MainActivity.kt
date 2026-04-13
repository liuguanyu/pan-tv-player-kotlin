package com.baidu.tv.player.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.baidu.tv.player.R
import com.baidu.tv.player.auth.AuthRepository
import com.baidu.tv.player.ui.settings.SettingsActivity

/**
 * 主Activity
 *
 * 负责初始化主界面，处理全局按键事件，管理登录状态
 * 使用沉浸式全屏模式隐藏状态栏和导航栏
 * 支持遥控器菜单键打开设置界面
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏和沉浸式模式，隐藏状态栏和导航栏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        setContentView(R.layout.activity_main)

        // 检查是否已登录
        val authRepository = AuthRepository(this)
        if (!authRepository.isAuthenticated()) {
            // 未登录，跳转到登录界面
            val intent = Intent(this, com.baidu.tv.player.auth.LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }
    }

    /**
     * 处理按键事件
     * 按遥控器菜单键（或键盘M键）打开设置
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 遥控器菜单键或键盘M键打开设置
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_M) {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}