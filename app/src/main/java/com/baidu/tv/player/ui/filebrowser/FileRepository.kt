package com.baidu.tv.player.ui.filebrowser

import com.baidu.tv.player.network.BaiduPanApiService
import com.baidu.tv.player.network.model.FileListResponse
import com.baidu.tv.player.network.model.BaiduFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.lang.Exception

/**
 * 文件数据仓库
 *
 * 负责从百度网盘API获取文件列表数据，并提供数据过滤和处理功能
 *
 * @property apiService 百度网盘API服务
 */
class FileRepository(
    private val apiService: BaiduPanApiService
) {

    /**
     * 获取指定路径下的文件列表
     *
     * @param path 文件夹路径，默认为根目录
     * @param page 页码，从1开始
     * @param num 每页数量，默认100
     * @param recursion 是否递归获取子文件夹内容
     * @param filterSupported 是否只返回支持的媒体文件
     * @return 文件列表结果
     */
    suspend fun getFileList(
        path: String = "/",
        page: Int = 1,
        num: Int = 100,
        recursion: Boolean = false,
        filterSupported: Boolean = true
    ): Result<List<FileInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFileList(path, page, num, recursion)

                if (response.isSuccessful && response.body() != null) {
                    val fileList = response.body()!!.list.map { it.toFileInfo() }

                    // 如果需要过滤支持的文件
                    val filteredList = if (filterSupported) {
                        fileList.filter { it.isDir || it.isSupported }
                    } else {
                        fileList
                    }

                    // 如果需要递归获取子文件夹
                    val result = if (recursion) {
                        val allFiles = mutableListOf<FileInfo>()
                        allFiles.addAll(filteredList)

                        // 递归获取所有子文件夹的内容
                        for (file in filteredList) {
                            if (file.isDir) {
                                val subFiles = getFileList(file.path, page, num, true, filterSupported)
                                if (subFiles.isSuccess) {
                                    allFiles.addAll(subFiles.getOrDefault(emptyList()))
                                }
                            }
                        }
                        allFiles
                    } else {
                        filteredList
                    }

                    Result.success(result)
                } else {
                    Result.failure(Exception("获取文件列表失败: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("网络请求失败: ${e.message}", e))
            }
        }
    }

    /**
     * 搜索文件
     *
     * @param key 搜索关键词
     * @param dir 搜索目录，默认为根目录
     * @param recursion 是否递归搜索
     * @return 匹配的文件列表
     */
    suspend fun searchFiles(
        key: String,
        dir: String = "/",
        recursion: Boolean = true
    ): Result<List<FileInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchFiles(key, dir, recursion)

                if (response.isSuccessful && response.body() != null) {
                    val fileList = response.body()!!.list
                        .map { it.toFileInfo() }
                        .filter { it.isDir || it.isSupported }

                    Result.success(fileList)
                } else {
                    Result.failure(Exception("搜索失败: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("搜索请求失败: ${e.message}", e))
            }
        }
    }

    /**
     * 获取视频播放信息
     *
     * @param fsId 文件ID
     * @return 视频播放URL
     */
    suspend fun getVideoPlayInfo(fsId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getFileDownloadLink(fsId)

                if (response.isSuccessful && response.body() != null) {
                    val downloadLink = response.body()!!.link
                    Result.success(downloadLink)
                } else {
                    Result.failure(Exception("获取播放链接失败: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(Exception("获取播放链接失败: ${e.message}", e))
            }
        }
    }

    /**
     * 扩展函数：将BaiduFile转换为FileInfo
     */
    private fun BaiduFile.toFileInfo(): FileInfo {
        return FileInfo(
            id = fsId.toString(),
            name = serverFilename ?: filename ?: "",
            path = path,
            size = size,
            isDir = isDir == 1,
            modifiedTime = serverMtime * 1000, // 转换为毫秒
            thumbnailUrl = thumbs?.url2 ?: thumbs?.url1,
            videoDuration = duration,
            mimeType = if (isDir == 0) {
                // 根据扩展名推断MIME类型
                when (serverFilename?.substringAfterLast(".", "")?.lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "bmp" -> "image/bmp"
                    "webp" -> "image/webp"
                    "mp4" -> "video/mp4"
                    "mkv" -> "video/x-matroska"
                    "avi" -> "video/x-msvideo"
                    "mov" -> "video/quicktime"
                    "flv" -> "video/x-flv"
                    "ts" -> "video/mp2t"
                    "m3u8" -> "application/x-mpegURL"
                    "hevc", "h265" -> "video/hevc"
                    else -> null
                }
            } else null
        )
    }
}
