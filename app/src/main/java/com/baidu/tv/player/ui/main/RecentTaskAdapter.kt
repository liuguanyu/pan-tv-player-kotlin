package com.baidu.tv.player.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.baidu.tv.player.R
import com.baidu.tv.player.model.MediaType
import com.baidu.tv.player.model.PlaybackHistory

import java.util.ArrayList
import java.util.List

/**
 * 最近播放适配器
 *
 * 负责管理最近播放记录的水平滚动列表显示
 *
 * @property historyList 最近播放记录数据源
 * @property listener 点击监听器
 */
class RecentTaskAdapter : RecyclerView.Adapter<RecentTaskAdapter.ViewHolder>() {

    private var historyList: List<PlaybackHistory> = ArrayList()
    private var listener: OnItemClickListener? = null

    /**
     * 设置点击监听器
     */
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener
    }

    /**
     * 设置最近播放记录列表
     *
     * @param historyList 最近播放记录列表，允许为null
     */
    fun setHistoryList(historyList: List<PlaybackHistory>?) {
        this.historyList = historyList ?: ArrayList()
        notifyDataSetChanged()
    }

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(@NonNull holder: ViewHolder, position: Int) {
        val history = historyList[position]
        holder.bind(history)
        holder.itemView.setOnClickListener {
            listener?.onItemClick(history)
        }
    }

    override fun getItemCount(): Int = historyList.size

    /**
     * 最近播放项点击监听器
     */
    interface OnItemClickListener {
        fun onItemClick(history: PlaybackHistory)
    }

    /**
     * ViewHolder类，负责管理最近播放项的视图
     *
     * @property tvFolderName 文件夹名称
     * @property tvFolderPath 文件夹路径
     * @property tvMediaType 媒体类型
     * @property tvFileCount 文件数量
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFolderName: TextView = itemView.findViewById(R.id.tv_folder_name)
        val tvFolderPath: TextView = itemView.findViewById(R.id.tv_folder_path)
        val tvMediaType: TextView = itemView.findViewById(R.id.tv_media_type)
        val tvFileCount: TextView = itemView.findViewById(R.id.tv_file_count)

        /**
         * 绑定数据到视图
         *
         * @param history 播放历史记录
         */
        fun bind(history: PlaybackHistory) {
            tvFolderName.text = history.folderName
            tvFolderPath.text = history.folderPath

            val mediaType = MediaType.fromCode(history.mediaType)
            tvMediaType.text = mediaType.name

            tvFileCount.text = "${history.fileCount}个文件"
        }
    }
}