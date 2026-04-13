package com.baidu.tv.player.ui.filebrowser

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.baidu.tv.player.R
import com.baidu.tv.player.ui.MainActivity
import com.baidu.tv.player.ui.filebrowser.FileBrowserFragment.newInstance

/**
 * 文件浏览Activity
 *
 * 负责文件浏览模块的主界面，包含文件浏览Fragment和导航控制
 * 支持遥控器D-pad导航，处理方向键事件
 *
 * 该Activity主要用于在电视端提供文件浏览功能
 */
class FileBrowserActivity : AppCompatActivity() {

    private lateinit var fileBrowserFragment: FileBrowserFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_browser)

        // 获取选择模式参数（从Intent传递）
        val selectionMode = intent.getIntExtra("selection_mode", SelectionMode.SINGLE.ordinal)
        val mode = SelectionMode.values()[selectionMode]

        // 创建并添加Fragment
        fileBrowserFragment = newInstance(mode)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fileBrowserFragment)
            .commit()
    }

    /**
     * 处理遥控器D-pad导航事件
     *
     * @param keyCode 按键代码
     * @param event 键盘事件
     * @return 是否处理了事件
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                // 上方向键：移动到上一个文件
                // 这里由RecyclerView的焦点管理自动处理，不需要额外实现
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // 下方向键：移动到下一个文件
                // 这里由RecyclerView的焦点管理自动处理，不需要额外实现
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // 左方向键：返回上一级目录
                if (fileBrowserFragment.viewModel.navigateBack()) {
                    return true
                } else {
                    Toast.makeText(this, "已经是根目录", Toast.LENGTH_SHORT).show()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // 右方向键：进入选中文件夹或播放文件
                val selectedFiles = fileBrowserFragment.viewModel.getSelectedFileList()
                if (selectedFiles.isNotEmpty()) {
                    val file = selectedFiles.first()
                    if (file.isDir) {
                        fileBrowserFragment.viewModel.navigateToFolder(file)
                    } else if (file.isVideo) {
                        playFile(file)
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                // 中心键：确认选择
                val selectedFiles = fileBrowserFragment.viewModel.getSelectedFileList()
                if (selectedFiles.isNotEmpty()) {
                    val file = selectedFiles.first()
                    if (file.isDir) {
                        fileBrowserFragment.viewModel.navigateToFolder(file)
                    } else if (file.isVideo) {
                        playFile(file)
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                // 返回键：返回到上一个Activity
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * 播放指定文件
     *
     * @param file 要播放的文件
     */
    private fun playFile(file: FileInfo) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("play_file", file.path)
        intent.putExtra("play_type", "single")
        startActivity(intent)
    }

    /**
     * 播放选中的多个文件
     *
     * @param files 要播放的文件列表
     */
    fun playSelectedFiles(files: List<FileInfo>) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("play_files", files.map { it.path }.toTypedArray())
        intent.putExtra("play_type", "multi")
        startActivity(intent)
    }
}