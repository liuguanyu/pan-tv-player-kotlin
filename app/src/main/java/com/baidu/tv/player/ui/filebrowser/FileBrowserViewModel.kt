package com.baidu.tv.player.ui.filebrowser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Stack

/**
 * 文件浏览视图模型
 *
 * 负责管理文件浏览的UI状态和业务逻辑，包括：
 * - 文件列表的加载和刷新
 * - 文件夹导航历史
 * - 多选和单选模式管理
 * - 搜索功能
 * - 加载状态和错误处理
 *
 * @property application 应用上下文
 * @property repository 文件数据仓库
 */
class FileBrowserViewModel(
    application: Application,
    private val repository: FileRepository
) : AndroidViewModel(application) {

    // 当前文件列表
    private val _fileList = MutableLiveData<List<FileInfo>>()
    val fileList: LiveData<List<FileInfo>> = _fileList

    // 当前路径
    private val _currentPath = MutableLiveData<String>()
    val currentPath: LiveData<String> = _currentPath

    // 加载状态
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 错误信息
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 选择模式
    private val _selectionMode = MutableLiveData<SelectionMode>()
    val selectionMode: LiveData<SelectionMode> = _selectionMode

    // 已选择的文件
    private val _selectedFiles = MutableLiveData<MutableSet<FileInfo>>()
    val selectedFiles: LiveData<MutableSet<FileInfo>> = _selectedFiles

    // 路径导航栈
    private val pathStack = Stack<String>()

    // 搜索关键词
    private val _searchKeyword = MutableLiveData<String>()
    val searchKeyword: LiveData<String> = _searchKeyword

    // 是否在搜索模式
    private val _isSearching = MutableLiveData<Boolean>()
    val isSearching: LiveData<Boolean> = _isSearching

    // 搜索结果
    private val _searchResults = MutableLiveData<List<FileInfo>>()
    val searchResults: LiveData<List<FileInfo>> = _searchResults

    init {
        _selectionMode.value = SelectionMode.SINGLE
        _selectedFiles.value = mutableSetOf()
        _currentPath.value = "/"
        _isSearching.value = false
        pathStack.push("/")
        loadFileList("/")
    }

    /**
     * 加载文件列表
     *
     * @param path 要加载的路径
     * @param refresh 是否为刷新操作
     */
    fun loadFileList(path: String, refresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getFileList(path = path)

            _isLoading.value = false

            result.fold(
                onSuccess = { files ->
                    // 按文件夹优先、名称排序
                    val sortedFiles = files.sortedWith(
                        compareBy<FileInfo> { !it.isDir }
                            .thenBy { it.name.lowercase() }
                    )
                    _fileList.value = sortedFiles

                    if (!refresh) {
                        _currentPath.value = path
                        pathStack.push(path)
                    }
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                }
            )
        }
    }

    /**
     * 刷新当前目录
     */
    fun refreshCurrentDir() {
        _currentPath.value?.let { path ->
            loadFileList(path, refresh = true)
        }
    }

    /**
     * 导航到指定文件夹
     *
     * @param folder 文件夹信息
     */
    fun navigateToFolder(folder: FileInfo) {
        if (folder.isDir) {
            loadFileList(folder.path)
        }
    }

    /**
     * 返回上一级目录
     *
     * @return 是否成功返回
     */
    fun navigateBack(): Boolean {
        return if (pathStack.size > 1) {
            pathStack.pop()
            val previousPath = pathStack.peek()
            loadFileList(previousPath, refresh = true)
            true
        } else {
            false
        }
    }

    /**
     * 设置选择模式
     *
     * @param mode 选择模式
     */
    fun setSelectionMode(mode: SelectionMode) {
        _selectionMode.value = mode
        clearSelection()
    }

    /**
     * 切换文件选择状态
     *
     * @param file 文件信息
     */
    fun toggleFileSelection(file: FileInfo) {
        val selected = _selectedFiles.value ?: mutableSetOf()

        if (selected.contains(file)) {
            selected.remove(file)
        } else {
            // 单选模式下先清空之前的选择
            if (_selectionMode.value == SelectionMode.SINGLE) {
                selected.clear()
            }
            selected.add(file)
        }

        _selectedFiles.value = selected
    }

    /**
     * 清空选择
     */
    fun clearSelection() {
        _selectedFiles.value = mutableSetOf()
    }

    /**
     * 全选当前页面的文件
     */
    fun selectAll() {
        val files = _fileList.value ?: return
        val selected = _selectedFiles.value ?: mutableSetOf()

        // 只选择媒体文件，不选择文件夹
        files.filter { !it.isDir && it.isSupported }.forEach {
            selected.add(it)
        }

        _selectedFiles.value = selected
    }

    /**
     * 获取已选择的文件列表
     */
    fun getSelectedFileList(): List<FileInfo> {
        return _selectedFiles.value?.toList() ?: emptyList()
    }

    /**
     * 搜索文件
     *
     * @param keyword 搜索关键词
     */
    fun searchFiles(keyword: String) {
        if (keyword.isBlank()) {
            clearSearch()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _isSearching.value = true
            _searchKeyword.value = keyword
            _errorMessage.value = null

            val result = repository.searchFiles(keyword, _currentPath.value ?: "/")

            _isLoading.value = false

            result.fold(
                onSuccess = { files ->
                    val sortedFiles = files.sortedBy { it.name.lowercase() }
                    _searchResults.value = sortedFiles
                    _fileList.value = sortedFiles
                },
                onFailure = { error ->
                    _errorMessage.value = error.message
                }
            )
        }
    }

    /**
     * 清空搜索
     */
    fun clearSearch() {
        _isSearching.value = false
        _searchKeyword.value = ""
        _searchResults.value = emptyList()
        _currentPath.value?.let { loadFileList(it, refresh = true) }
    }

    /**
     * 获取视频播放链接
     *
     * @param file 视频文件
     * @return 播放链接结果
     */
    suspend fun getVideoPlayUrl(file: FileInfo): Result<String> {
        return repository.getVideoPlayInfo(file.id)
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 工厂类，用于创建FileBrowserViewModel实例
     */
    class Factory(private val application: Application, private val repository: FileRepository) : AndroidViewModel.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : AndroidViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FileBrowserViewModel::class.java)) {
                return FileBrowserViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * 选择模式枚举
 */
enum class SelectionMode {
    SINGLE,  // 单选模式
    MULTI    // 多选模式
}
