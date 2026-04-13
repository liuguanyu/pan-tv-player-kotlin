package com.baidu.tv.player.ui.main

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.baidu.tv.player.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

/**
 * 播放列表卡片ViewHolder
 *
 * 负责管理单个播放列表卡片的视图和数据绑定
 *
 * @property ivCover 封面图片视图
 * @property tvPlaylistName 播放列表名称文本视图
 * @property tvStats 统计信息文本视图（文件数量和时长）
 * @property ivDelete 删除按钮图片视图
 * @property ivRefresh 刷新按钮图片视图
 */
class PlaylistCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
    val tvPlaylistName: TextView = itemView.findViewById(R.id.tv_playlist_name)
    val tvStats: TextView = itemView.findViewById(R.id.tv_stats)
    val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)
    val ivRefresh: ImageView = itemView.findViewById(R.id.iv_refresh)

    /**
     * 绑定数据到视图
     *
     * @param coverImagePath 封面图片路径，可能为null或空字符串
     * @param playlistName 播放列表名称
     * @param stats 统计信息字符串
     *
     * 使用Glide加载封面图片，如果封面路径无效则使用默认图片
     */
    fun bind(coverImagePath: String?, playlistName: String, stats: String) {
        // 加载封面图片
        if (!coverImagePath.isNullOrEmpty()) {
            Glide.with(ivCover.context)
                .load(coverImagePath)
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivCover)
        } else {
            // 使用默认图片
            ivCover.setImageResource(R.drawable.banner)
        }

        // 设置播放列表名称
        tvPlaylistName.text = playlistName

        // 设置统计信息
        tvStats.text = stats
    }
}