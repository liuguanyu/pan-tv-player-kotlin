package com.baidu.tv.player.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baidu.tv.player.R
import com.baidu.tv.player.model.MediaType
import com.baidu.tv.player.model.PlaybackHistory
import com.baidu.tv.player.model.Playlist
import com.baidu.tv.player.repository.PlaylistRepository
import com.baidu.tv.player.utils.PreferenceUtils
import java.util.List

/**
 * 主界面Fragment
 *
 * 负责管理主界面的所有UI组件和交互逻辑
 * 包含播放列表、最近播放、快速操作区域
 * 支持D-pad导航和焦点管理
 * 使用ViewModel管理UI状态
 */
class MainFragment : Fragment() {

    private var viewModel: MainViewModel? = null
    private var recentTaskAdapter: RecentTaskAdapter? = null
    private var playlistAdapter: PlaylistAdapter? = null
    private var playlistRepository: PlaylistRepository? = null

    private var rvPlaylists: RecyclerView? = null
    private var rvRecentTasks: RecyclerView? = null
    private var tvRecentTitle: TextView? = null
    private var tvNoPlaylist: TextView? = null
    private var btnBrowseFiles: ImageView? = null
    private var btnCreatePlaylist: ImageView? = null

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)

        initViews(view)
        initViewModel()
        loadPlaylists()

        return view
    }

    private fun initViews(view: View) {
        rvPlaylists = view.findViewById(R.id.rv_playlists)
        rvRecentTasks = view.findViewById(R.id.rv_recent_tasks)
        tvRecentTitle = view.findViewById(R.id.tv_recent_title)
        tvNoPlaylist = view.findViewById(R.id.tv_no_playlist)
        btnBrowseFiles = view.findViewById(R.id.btn_browse_files)
        btnCreatePlaylist = view.findViewById(R.id.btn_create_playlist)

        // 设置播放列表RecyclerView为横向
        playlistAdapter = PlaylistAdapter(requireContext())
        rvPlaylists?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvPlaylists?.adapter = playlistAdapter
        // 启用RecyclerView的焦点搜索
        rvPlaylists?.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        rvPlaylists?.hasFixedSize = true

        // 设置最近任务RecyclerView为横向
        recentTaskAdapter = RecentTaskAdapter()
        rvRecentTasks?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvRecentTasks?.adapter = recentTaskAdapter
        // 启用RecyclerView的焦点搜索，但禁用RecyclerView本身的焦点（电视屏幕上可能不可见）
        rvRecentTasks?.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        rvRecentTasks?.isFocusable = false

        // 设置点击事件
        btnBrowseFiles?.setOnClickListener { openFileBrowser(MediaType.ALL) }
        btnCreatePlaylist?.setOnClickListener { openFileBrowserForPlaylist() }

        // 设置播放列表点击事件
        playlistAdapter?.setOnItemClickListener(::onPlaylistClick)
        playlistAdapter?.setOnItemLongClickListener(::onPlaylistLongClick)
        playlistAdapter?.setOnDeleteClickListener(::onPlaylistDelete)
        playlistAdapter?.setOnRefreshClickListener(::onPlaylistRefresh)

        // 设置最近任务点击事件
        recentTaskAdapter?.setOnItemClickListener(::onRecentTaskClick)
    }

    private fun initViewModel() {
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewModel?.getRecentHistory()?.observe(viewLifecycleOwner) { historyList ->
            if (historyList == null || historyList.isEmpty()) {
                rvRecentTasks?.visibility = View.GONE
            } else {
                tvRecentTitle?.visibility = View.VISIBLE
                rvRecentTasks?.visibility = View.VISIBLE
                recentTaskAdapter?.setHistoryList(historyList)
            }
        }
    }

    /**
     * 加载播放列表
     */
    private fun loadPlaylists() {
        playlistRepository = PlaylistRepository(requireContext())
        playlistRepository?.getAllPlaylists()?.observe(viewLifecycleOwner) { playlists ->
            // 播放列表标题始终显示
            if (playlists == null || playlists.isEmpty()) {
                rvPlaylists?.visibility = View.GONE
                tvNoPlaylist?.visibility = View.VISIBLE
                // 没有播放列表，默认聚焦到浏览文件按钮
                btnBrowseFiles?.post {
                    btnBrowseFiles?.requestFocus()
                }
            } else {
                rvPlaylists?.visibility = View.VISIBLE
                tvNoPlaylist?.visibility = View.GONE
                playlistAdapter?.setPlaylists(playlists)
                // 有播放列表，延迟请求第一个项的焦点
                rvPlaylists?.post {
                    val firstChild = rvPlaylists?.layoutManager?.findViewByPosition(0)
                    if (firstChild != null) {
                        firstChild.requestFocus()
                    }
                }
            }
        }
    }

    /**
     * 打开文件浏览器（普通模式）
     */
    private fun openFileBrowser(mediaType: MediaType) {
        val intent = Intent(requireContext(), com.baidu.tv.player.ui.filebrowser.FileBrowserActivity::class.java)
        intent.putExtra("mediaType", mediaType.value)
        intent.putExtra("initialPath", "/")
        startActivity(intent)
    }

    /**
     * 打开文件浏览器（创建播放列表模式）
     */
    private fun openFileBrowserForPlaylist() {
        val intent = Intent(requireContext(), com.baidu.tv.player.ui.filebrowser.FileBrowserActivity::class.java)
        intent.putExtra("mediaType", MediaType.ALL.value)
        intent.putExtra("initialPath", "/")
        intent.putExtra("multiSelectMode", true) // 多选模式
        startActivityForResult(intent, 1001) // 请求码用于标识创建播放列表操作
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 处理从FileBrowserActivity返回的结果
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK) {
            // 播放列表创建成功，刷新播放列表显示
            loadPlaylists()
        }
    }

    /**
     * 播放列表点击事件
     */
    private fun onPlaylistClick(playlist: Playlist) {
        // 如果处于编辑模式，点击无效
        if (playlistAdapter?.isEditMode() == true) {
            return
        }

        // 启动播放器，播放该播放列表
        val intent = Intent(requireContext(), com.baidu.tv.player.ui.playback.PlaybackActivity::class.java)
        intent.putExtra("playlistDatabaseId", playlist.id)
        startActivity(intent)
    }

    /**
     * 播放列表长按事件 - 切换编辑模式
     */
    private fun onPlaylistLongClick(playlist: Playlist) {
        // 切换编辑模式
        val newEditMode = !playlistAdapter?.isEditMode() ?: false
        playlistAdapter?.setEditMode(newEditMode)

        if (newEditMode) {
            android.widget.Toast.makeText(requireContext(),
                "点击删除按钮删除播放列表，再次长按退出编辑模式",
                android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 播放列表删除事件
     */
    private fun onPlaylistDelete(playlist: Playlist) {
        // 显示确认对话框
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("删除播放列表")
            .setMessage("确定要删除播放列表\"${playlist.name}\"吗？\n这将删除播放列表及其所有文件记录。")
            .setPositiveButton("删除") { dialog, which ->
                // 执行删除操作
                Thread {
                    try {
                        playlistRepository?.deletePlaylist(playlist,
                            {
                                // 删除成功
                                requireActivity().runOnUiThread {
                                    android.widget.Toast.makeText(requireContext(),
                                        "播放列表已删除",
                                        android.widget.Toast.LENGTH_SHORT).show()

                                    // 退出编辑模式
                                    playlistAdapter?.setEditMode(false)

                                    // 刷新播放列表
                                    loadPlaylists()
                                }
                            },
                            {
                                // 删除失败
                                requireActivity().runOnUiThread {
                                    android.widget.Toast.makeText(requireContext(),
                                        "删除播放列表失败",
                                        android.widget.Toast.LENGTH_SHORT).show()
                                }
                            })
                    } catch (e: Exception) {
                        android.util.Log.e("MainFragment", "删除播放列表失败", e)
                        requireActivity().runOnUiThread {
                            android.widget.Toast.makeText(requireContext(),
                                "删除播放列表失败: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 播放列表刷新事件
     */
    private fun onPlaylistRefresh(playlist: Playlist) {
        // 显示刷新提示
        android.widget.Toast.makeText(requireContext(),
            "正在刷新播放列表\"${playlist.name}\"...",
            android.widget.Toast.LENGTH_SHORT).show()

        // 执行刷新操作（BaiduAuthService会在PlaylistRepository内部处理认证）
        playlistRepository?.refreshPlaylist(playlist,
            {
                // 刷新成功
                requireActivity().runOnUiThread {
                    android.widget.Toast.makeText(requireContext(),
                        "播放列表刷新成功",
                        android.widget.Toast.LENGTH_SHORT).show()

                    // 刷新播放列表显示
                    loadPlaylists()
                }
            },
            {
                // 刷新失败
                requireActivity().runOnUiThread {
                    android.widget.Toast.makeText(requireContext(),
                        "刷新播放列表失败，请检查网络连接或重新登录",
                        android.widget.Toast.LENGTH_SHORT).show()
                }
            })
    }

    /**
     * 最近任务点击事件
     */
    private fun onRecentTaskClick(history: PlaybackHistory) {
        val intent = Intent(requireContext(), com.baidu.tv.player.ui.playback.PlaybackActivity::class.java)
        intent.putExtra("historyId", history.id)
        startActivity(intent)
    }

    /**
     * 处理遥控器按键事件
     */
    override fun onResume() {
        super.onResume()
        // 设置按键监听
        view?.isFocusableInTouchMode = true
        // 不让根视图获得焦点，而是让可见的子元素获得焦点
        // view?.requestFocus()
        // 确保焦点在可见的元素上
        requestFocusOnVisibleElement()
        view?.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                // 菜单键打开设置
                if (keyCode == KeyEvent.KEYCODE_MENU) {
                    openSettings()
                    return@setOnKeyListener true
                }
                // 返回键：如果处于编辑模式，退出编辑模式
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (playlistAdapter?.isEditMode() == true) {
                        playlistAdapter?.setEditMode(false)
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    /**
     * 打开设置界面
     */
    private fun openSettings() {
        val intent = Intent(requireContext(), com.baidu.tv.player.ui.settings.SettingsActivity::class.java)
        startActivity(intent)
    }

    /**
     * 请求焦点到可见的元素
     */
    private fun requestFocusOnVisibleElement() {
        // 延迟执行，确保布局已完成
        view?.post {
            // 优先聚焦到播放列表的第一个项
            if (rvPlaylists?.visibility == View.VISIBLE && rvPlaylists?.adapter != null
                && rvPlaylists?.adapter?.itemCount ?: 0 > 0) {
                val firstChild = rvPlaylists?.layoutManager?.findViewByPosition(0)
                if (firstChild != null && firstChild.requestFocus()) {
                    return@post
                }
            }
            // 如果没有播放列表，聚焦到浏览文件按钮
            if (btnBrowseFiles?.visibility == View.VISIBLE && btnBrowseFiles?.requestFocus() == true) {
                return@post
            }
            // 最后尝试聚焦到创建播放列表按钮
            if (btnCreatePlaylist?.visibility == View.VISIBLE) {
                btnCreatePlaylist?.requestFocus()
            }
        }
    }
}