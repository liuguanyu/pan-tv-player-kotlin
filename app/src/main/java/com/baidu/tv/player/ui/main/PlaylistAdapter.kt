package com.baidu.tv.player.ui.main

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.baidu.tv.player.R
import com.baidu.tv.player.model.Playlist

import java.util.ArrayList
import java.util.List

/**
 * 播放列表适配器
 *
 * 负责管理播放列表的水平滚动列表显示
 * 支持编辑模式，在编辑模式下显示删除和刷新按钮
 *
 * @property context 上下文对象
 * @property playlists 播放列表数据源
 * @property onItemClickListener 播放列表项点击监听器
 * @property onItemLongClickListener 播放列表项长按监听器
 * @property onDeleteClickListener 删除按钮点击监听器
 * @property onRefreshClickListener 刷新按钮点击监听器
 * @property isEditMode 是否处于编辑模式
 */
class PlaylistAdapter(private val context: Context) : RecyclerView.Adapter<PlaylistCardViewHolder>() {

    private var playlists: List<Playlist> = ArrayList()
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null
    private var onDeleteClickListener: OnDeleteClickListener? = null
    private var onRefreshClickListener: OnRefreshClickListener? = null
    private var isEditMode = false

    /**
     * 设置播放列表数据
     *
     * @param playlists 播放列表集合
     */
    fun setPlaylists(playlists: List<Playlist>) {
        this.playlists = playlists
        notifyDataSetChanged()
    }

    /**
     * 设置播放列表项点击监听器
     */
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.onItemClickListener = listener
    }

    /**
     * 设置播放列表项长按监听器
     */
    fun setOnItemLongClickListener(listener: OnItemLongClickListener?) {
        this.onItemLongClickListener = listener
    }

    /**
     * 设置删除按钮点击监听器
     */
    fun setOnDeleteClickListener(listener: OnDeleteClickListener?) {
        this.onDeleteClickListener = listener
    }

    /**
     * 设置刷新按钮点击监听器
     */
    fun setOnRefreshClickListener(listener: OnRefreshClickListener?) {
        this.onRefreshClickListener = listener
    }

    /**
     * 设置编辑模式
     *
     * @param editMode 是否开启编辑模式
     *
     * 编辑模式下，每个播放列表卡片会显示删除和刷新按钮
     */
    fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        notifyDataSetChanged()
    }

    /**
     * 检查是否处于编辑模式
     *
     * @return 如果处于编辑模式返回true，否则返回false
     */
    fun isEditMode(): Boolean = isEditMode

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): PlaylistCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_card, parent, false)
        return PlaylistCardViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: PlaylistCardViewHolder, position: Int) {
        val playlist = playlists[position]

        // 格式化统计信息
        val stats = if (playlist.totalDuration > 0) {
            val duration = DateUtils.formatElapsedTime(playlist.totalDuration / 1000)
            "${playlist.totalItems}个文件 · $duration"
        } else {
            "${playlist.totalItems}个文件"
        }

        holder.bind(playlist.coverImagePath, playlist.name, stats)

        // 根据编辑模式设置删除按钮和刷新按钮的可见性
        holder.ivDelete.visibility = if (isEditMode) View.VISIBLE else View.GONE
        holder.ivRefresh.visibility = if (isEditMode) View.VISIBLE else View.GONE

        // 设置点击事件
        holder.itemView.setOnClickListener {
            if (!isEditMode && onItemClickListener != null) {
                onItemClickListener!!.onItemClick(playlist)
            }
        }

        // 设置长按事件
        holder.itemView.setOnLongClickListener {
            if (onItemLongClickListener != null) {
                onItemLongClickListener!!.onItemLongClick(playlist)
                return@setOnLongClickListener true
            }
            false
        }

        // 设置删除按钮点击事件
        holder.ivDelete.setOnClickListener {
            if (onDeleteClickListener != null) {
                onDeleteClickListener!!.onDeleteClick(playlist)
            }
        }

        // 设置刷新按钮点击事件
        holder.ivRefresh.setOnClickListener {
            if (onRefreshClickListener != null) {
                onRefreshClickListener!!.onRefreshClick(playlist)
            }
        }
    }

    override fun getItemCount(): Int = playlists.size

    /**
     * 播放列表项点击监听器
     */
    interface OnItemClickListener {
        fun onItemClick(playlist: Playlist)
    }

    /**
     * 播放列表项长按监听器
     */
    interface OnItemLongClickListener {
        fun onItemLongClick(playlist: Playlist)
    }

    /**
     * 删除按钮点击监听器
     */
    interface OnDeleteClickListener {
        fun onDeleteClick(playlist: Playlist)
    }

    /**
     * 刷新按钮点击监听器
     */
    interface OnRefreshClickListener {
        fun onRefreshClick(playlist: Playlist)
    }
}