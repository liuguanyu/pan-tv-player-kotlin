package com.baidu.tv.player.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.baidu.tv.player.R
import com.baidu.tv.player.auth.LoginActivity
import com.baidu.tv.player.auth.DataStoreManager

/**
 * 设置Activity
 *
 * 应用的主设置界面，包含所有设置项。
 * 使用Fragment方式实现，便于后续扩展和维护。
 *
 * 功能：
 * - 图片特效设置（支持多选和随机模式）
 * - 动画时长和展示时长设置
 * - 背景模式选择
 * - 地点显示开关
 * - H.265播放设置
 * - 退出登录功能
 *
 * 支持TV遥控器D-pad操作，所有UI元素都支持焦点导航。
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 设置标题
        supportActionBar?.apply {
            title = "设置"
            setDisplayHomeAsUpEnabled(true)
        }

        // 加载设置Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.settings_container, SettingsFragment())
                setReorderingAllowed(true)
            }
        }

        // 设置退出登录按钮
        setupLogoutButton()
    }

    /**
     * 设置退出登录按钮
     */
    private fun setupLogoutButton() {
        // 注意：退出登录按钮在XML布局中定义，这里设置点击事件
        // 如果使用PreferenceScreen方式，退出登录可以通过Preference实现
        findViewById<android.widget.Button>(R.id.btn_logout)?.setOnClickListener {
            performLogout()
        }
    }

    /**
     * 执行退出登录操作
     *
     * 清除认证信息并跳转到登录界面
     */
    private fun performLogout() {
        // 清除认证信息
        val dataStoreManager = DataStoreManager(this@SettingsActivity)
        dataStoreManager.clearAuthInfo()

        // 跳转到登录界面
        val intent = Intent(this@SettingsActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * 处理返回按钮
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        private const val TAG = "SettingsActivity"
    }
}