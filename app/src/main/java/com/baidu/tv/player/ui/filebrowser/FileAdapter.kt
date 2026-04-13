package com.baidu.tv.player.ui.filebrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.baidu.tv.player.R

/**
 * 文件列表适配器
 *
 * 负责将文件列表数据绑定到RecyclerView的每个项上
 * 支持单选和多选模式，显示文件图标、名称、大小和修改时间
 *
 * @property fileList 文件列表
 * @property onFileClick 文件点击回调
 * @property onFileLongClick 文件长按回调
 * @property selectionMode 选择模式
 * @property selectedFiles 已选择的文件集合
 */
class FileAdapter(
    private val fileList: MutableList<FileInfo>,
    private val onFileClick: (FileInfo) -> Unit,
    private val onFileLongClick: (FileInfo) -> Boolean,
    private val selectionMode: SelectionMode,
    private val selectedFiles: MutableSet<FileInfo>
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    /**
     * 更新文件列表
     */
    fun updateList(newList: List<FileInfo>) {
        this.fileList.clear()
        this.fileList.addAll(newList)
        notifyDataSetChanged()
    }

    /**
     * 刷新选择状态
     */
    fun refreshSelectionState() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_browser, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = fileList[position]
        holder.bind(file)
    }

    override fun getItemCount(): Int = fileList.size

    /**
     * 文件列表项ViewHolder
     */
    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.iv_file_icon)
        private val nameTextView: TextView = itemView.findViewById(R.id.tv_file_name)
        private val sizeTextView: TextView = itemView.findViewById(R.id.tv_file_size)
        private val modifiedTextView: TextView = itemView.findViewById(R.id.tv_modified_time)
        private val selectionIndicator: ImageView = itemView.findViewById(R.id.iv_selection_indicator)

        init {
            // 设置点击事件
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFileClick(fileList[position])
                }
            }

            // 设置长按事件
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFileLongClick(fileList[position])
                }
                true
            }
        }

        /**
         * 绑定文件数据到视图
         */
        fun bind(file: FileInfo) {
            // 设置文件图标
            if (file.isDir) {
                iconImageView.setImageResource(R.drawable.ic_folder)
            } else if (file.isImage) {
                iconImageView.setImageResource(R.drawable.ic_image)
            } else if (file.isVideo) {
                iconImageView.setImageResource(R.drawable.ic_video)
            } else {
                iconImageView.setImageResource(R.drawable.ic_file)
            }

            // 设置文件名
            nameTextView.text = file.name

            // 设置文件大小
            sizeTextView.text = file.formatSize()

            // 设置修改时间
            modifiedTextView.text = file.formatModifiedTime()

            // 设置选择状态指示器
            if (selectionMode == SelectionMode.MULTI) {
                selectionIndicator.visibility = View.VISIBLE
                selectionIndicator.setImageResource(
                    if (selectedFiles.contains(file)) R.drawable.ic_check_selected else R.drawable.ic_check_unselected
                )
            } else {
                selectionIndicator.visibility = View.GONE
            }

            // 设置缩略图（仅图片和视频）
            if (file.isImage || file.isVideo) {
                if (file.thumbnailUrl != null) {
                    Glide.with(itemView.context)
                        .load(file.thumbnailUrl)
                        .placeholder(R.drawable.ic_file_placeholder)
                        .error(R.drawable.ic_file_placeholder)
                        .into(iconImageView)
                }
            }
        }
    }
}