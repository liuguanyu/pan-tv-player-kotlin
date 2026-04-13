package com.baidu.tv.player.ui.filebrowser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.baidu.tv.player.R
import com.baidu.tv.player.databinding.FragmentFileBrowserBinding
import com.baidu.tv.player.ui.MainActivity
import java.util.*

/**
 * 文件浏览Fragment
 *
 * 负责文件浏览的UI展示和用户交互，包括：
 * - 文件列表的显示
 * - 文件夹导航
 * - 搜索功能
 * - 多选/单选模式切换
 * - 进度反馈和错误提示
 *
 * 与FileBrowserViewModel协同工作，实现MVVM架构
 */
class FileBrowserFragment : Fragment() {

    private var _binding: FragmentFileBrowserBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FileBrowserViewModel
    private lateinit var adapter: FileAdapter
    private lateinit var recyclerView: RecyclerView

    private val selectionMode: SelectionMode by lazy {
        requireArguments().getInt("selection_mode", SelectionMode.SINGLE.ordinal)
            .let { SelectionMode.values()[it] }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化ViewModel
        val application = requireActivity().application
        viewModel = ViewModelProvider(
            this,
            FileBrowserViewModel.Factory(application)
        )[FileBrowserViewModel::class.java]

        // 初始化RecyclerView
        recyclerView = binding.rvFiles
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = FileAdapter(
            emptyList(),
            ::onFileClick,
            ::onFileLongClick,
            selectionMode,
            mutableSetOf()
        )
        recyclerView.adapter = adapter

        // 观察ViewModel状态
        observeViewModel()

        // 设置搜索按钮事件
        binding.btnSearch.setOnClickListener {
            val keyword = binding.etSearch.text.toString().trim()
            if (keyword.isNotEmpty()) {
                viewModel.searchFiles(keyword)
            }
        }

        // 设置刷新按钮事件
        binding.btnRefresh.setOnClickListener {
            viewModel.refreshCurrentDir()
        }

        // 设置返回按钮事件
        binding.btnBack.setOnClickListener {
            if (!viewModel.navigateBack()) {
                Toast.makeText(requireContext(), "已经是根目录", Toast.LENGTH_SHORT).show()
            }
        }

        // 设置选择模式切换按钮
        binding.btnSelectionMode.setOnClickListener {
            val currentMode = viewModel.selectionMode.value ?: SelectionMode.SINGLE
            val newMode = if (currentMode == SelectionMode.SINGLE) SelectionMode.MULTI else SelectionMode.SINGLE
            viewModel.setSelectionMode(newMode)
            updateSelectionButton()
        }

        // 设置全选按钮
        binding.btnSelectAll.setOnClickListener {
            viewModel.selectAll()
        }

        // 设置清除选择按钮
        binding.btnClearSelection.setOnClickListener {
            viewModel.clearSelection()
        }

        // 设置播放按钮（播放选中的视频）
        binding.btnPlaySelected.setOnClickListener {
            val selectedFiles = viewModel.getSelectedFileList()
            if (selectedFiles.isNotEmpty()) {
                val videoFiles = selectedFiles.filter { it.isVideo }
                if (videoFiles.isNotEmpty()) {
                    // 发送播放事件到MainActivity
                    (requireActivity() as MainActivity).playSelectedFiles(videoFiles)
                } else {
                    Toast.makeText(requireContext(), "请选择视频文件进行播放", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "请先选择文件", Toast.LENGTH_SHORT).show()
            }
        }

        // 初始化按钮状态
        updateSelectionButton()
    }

    private fun observeViewModel() {
        // 观察文件列表变化
        viewModel.fileList.observe(viewLifecycleOwner) { files ->
            adapter.updateList(files)
        }

        // 观察选择状态变化
        viewModel.selectedFiles.observe(viewLifecycleOwner) { selected ->
            adapter.selectedFiles.clear()
            adapter.selectedFiles.addAll(selected)
            adapter.refreshSelectionState()
        }

        // 观察选择模式变化
        viewModel.selectionMode.observe(viewLifecycleOwner) { mode ->
            adapter.selectionMode = mode
            adapter.refreshSelectionState()
            updateSelectionButton()
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // 观察错误信息
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                // 清除错误信息
                viewModel.clearError()
            }
        }

        // 观察搜索状态
        viewModel.isSearching.observe(viewLifecycleOwner) { isSearching ->
            binding.btnSearch.isEnabled = !isSearching
            binding.etSearch.isEnabled = !isSearching
            binding.btnRefresh.isEnabled = !isSearching
            binding.btnBack.isEnabled = !isSearching
            binding.btnSelectionMode.isEnabled = !isSearching
            binding.btnSelectAll.isEnabled = !isSearching
            binding.btnClearSelection.isEnabled = !isSearching
            binding.btnPlaySelected.isEnabled = !isSearching
        }
    }

    private fun onFileClick(file: FileInfo) {
        if (viewModel.selectionMode.value == SelectionMode.MULTI) {
            viewModel.toggleFileSelection(file)
        } else {
            if (file.isDir) {
                viewModel.navigateToFolder(file)
            } else {
                // 单选模式下，直接播放文件（如果是视频）
                if (file.isVideo) {
                    (requireActivity() as MainActivity).playFile(file)
                }
            }
        }
    }

    private fun onFileLongClick(file: FileInfo): Boolean {
        if (viewModel.selectionMode.value == SelectionMode.SINGLE) {
            // 在单选模式下，长按切换为多选模式
            viewModel.setSelectionMode(SelectionMode.MULTI)
            viewModel.toggleFileSelection(file)
            updateSelectionButton()
            return true
        } else {
            // 在多选模式下，长按切换选择状态
            viewModel.toggleFileSelection(file)
            return true
        }
    }

    private fun updateSelectionButton() {
        val mode = viewModel.selectionMode.value ?: SelectionMode.SINGLE
        val buttonText = if (mode == SelectionMode.SINGLE) "多选" else "单选"
        binding.btnSelectionMode.text = buttonText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * 创建文件浏览Fragment的工厂方法
         *
         * @param selectionMode 选择模式
         * @return 文件浏览Fragment实例
         */
        fun newInstance(selectionMode: SelectionMode = SelectionMode.SINGLE): FileBrowserFragment {
            val fragment = FileBrowserFragment()
            val args = Bundle().apply {
                putInt("selection_mode", selectionMode.ordinal)
            }
            fragment.arguments = args
            return fragment
        }
    }
}