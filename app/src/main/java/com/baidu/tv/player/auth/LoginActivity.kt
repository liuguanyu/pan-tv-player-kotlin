package com.baidu.tv.player.auth

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.baidu.tv.player.R
import com.baidu.tv.player.model.DeviceCodeResponse
import com.baidu.tv.player.ui.main.MainActivity
import com.baidu.tv.player.utils.QRCodeUtils

/**
 * 登录界面
 *
 * 负责显示二维码、处理登录流程
 * 与AuthViewModel交互，更新UI状态
 */
class LoginActivity : FragmentActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var ivQrCode: ImageView
    private lateinit var pbLoading: ProgressBar
    private lateinit var llError: LinearLayout
    private lateinit var btnRetry: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        initViewModel()
    }

    private fun initViews() {
        ivQrCode = findViewById(R.id.iv_qr_code)
        pbLoading = findViewById(R.id.pb_loading)
        llError = findViewById(R.id.ll_error)
        btnRetry = findViewById(R.id.btn_retry)
        tvStatus = findViewById(R.id.tv_status)

        btnRetry.setOnClickListener { viewModel.startLogin() }
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        viewModel.getAuthState().observe(this) { authState ->
            when (authState.status) {
                AuthRepository.AuthState.Status.LOADING -> {
                    showLoading()
                    tvStatus.text = authState.message
                }

                AuthRepository.AuthState.Status.DEVICE_CODE_RECEIVED -> {
                    val response = authState.data as DeviceCodeResponse
                    showQRCode(response)
                    // 开始轮询
                    viewModel.startPolling(response.deviceCode!!)
                    tvStatus.text = "请使用手机百度网盘APP扫描二维码登录"
                }

                AuthRepository.AuthState.Status.POLLING -> {
                    // 保持二维码显示，状态不变
                }

                AuthRepository.AuthState.Status.AUTHENTICATED -> {
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                    startMainActivity()
                }

                AuthRepository.AuthState.Status.UNAUTHENTICATED -> {
                    // 如果是刷新失败或其他原因导致的未认证，重新开始登录流程
                    viewModel.startLogin()
                }

                AuthRepository.AuthState.Status.ERROR -> {
                    showError()
                    tvStatus.text = authState.message
                }
            }
        }

        // 开始登录流程
        viewModel.startLogin()
    }

    private fun showLoading() {
        pbLoading.visibility = View.VISIBLE
        llError.visibility = View.GONE
        ivQrCode.setImageBitmap(null)
    }

    private fun showQRCode(response: DeviceCodeResponse) {
        pbLoading.visibility = View.GONE
        llError.visibility = View.GONE

        val url = response.fullVerificationUrl
        if (url != null) {
            val qrCode = QRCodeUtils.createQRCodeBitmap(url, 250, 250)
            ivQrCode.setImageBitmap(qrCode)
        }
    }

    private fun showError() {
        pbLoading.visibility = View.GONE
        llError.visibility = View.VISIBLE
        ivQrCode.setImageBitmap(null)
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}