package com.baidu.tv.player.ui.filebrowser

import com.baidu.tv.player.network.BaiduPanApiService
import com.baidu.tv.player.network.model.BaiduFile
import com.baidu.tv.player.network.model.FileListResponse
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import com.google.common.truth.Truth.assertThat

/**
 * FileRepository 单元测试
 *
 * 测试文件列表获取、文件搜索、过滤等功能
 * 使用 MockK 进行网络API接口模拟
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FileRepositoryTest {

    private lateinit var fileRepository: FileRepository
    private lateinit var mockBaiduPanApiService: BaiduPanApiService

    @Before
    fun setup() {
        mockBaiduPanApiService = mockk()
        fileRepository = FileRepository(mockBaiduPanApiService)
    }

    /**
     * 测试：获取文件列表 - 成功
     */
    @Test
    fun testGetFileList_Success() = runTest {
        // Given: 模拟API响应
        val baiduFiles = listOf(
            BaiduFile(
                fsId = "1",
                path = "/video1.mp4",
                serverFilename = "video1.mp4",
                size = 1024L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 120
            ),
            BaiduFile(
                fsId = "2",
                path = "/folder1",
                serverFilename = "folder1",
                size = 0L,
                isDir = 1,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = null
            )
        )

        val mockResponse = FileListResponse(list = baiduFiles, errno = 0)
        val response = Response.success(mockResponse)
        every { mockBaiduPanApiService.getFileList(eq("/"), eq(1), eq(100), eq(false)) } returns response

        // When: 获取文件列表
        val result = fileRepository.getFileList()

        // Then: 验证返回成功结果
        assertThat(result.isSuccess).isTrue()
        val fileList = result.getOrNull()
        assertThat(fileList).isNotNull()
        assertThat(fileList!!.size).isEqualTo(2)
        assertThat(fileList[0].fileName).isEqualTo("video1.mp4")
        assertThat(fileList[0].isVideo()).isTrue()
        assertThat(fileList[1].fileName).isEqualTo("folder1")
        assertThat(fileList[1].isDir).isTrue()
    }

    /**
     * 测试：获取文件列表 - API失败
     */
    @Test
    fun testGetFileList_Failure() = runTest {
        // Given: 模拟API响应失败
        val errorBody = ResponseBody.create(MediaType.parse("text/plain"), "Error message")
        val response = Response.error(500, errorBody)
        every { mockBaiduPanApiService.getFileList(eq("/"), eq(1), eq(100), eq(false)) } returns response

        // When: 获取文件列表
        val result = fileRepository.getFileList()

        // Then: 验证返回失败结果
        assertThat(result.isFailure).isTrue()
        val exception = result.exceptionOrNull()
        assertThat(exception).isNotNull()
        assertThat(exception!!.message).contains("获取文件列表失败")
    }

    /**
     * 测试：获取文件列表 - 带路径参数
     */
    @Test
    fun testGetFileList_WithPath() = runTest {
        // Given: 模拟API响应
        val baiduFiles = listOf(
            BaiduFile(
                fsId = "1",
                path = "/folder/video.mp4",
                serverFilename = "video.mp4",
                size = 1024L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 120
            )
        )

        val mockResponse = FileListResponse(list = baiduFiles, errno = 0)
        val response = Response.success(mockResponse)
        every { mockBaiduPanApiService.getFileList(eq("/folder"), eq(1), eq(100), eq(false)) } returns response

        // When: 获取指定路径的文件列表
        val result = fileRepository.getFileList(path = "/folder")

        // Then: 验证返回正确结果
        assertThat(result.isSuccess).isTrue()
        val fileList = result.getOrNull()
        assertThat(fileList!!.size).isEqualTo(1)
        assertThat(fileList[0].fileName).isEqualTo("video.mp4")
        assertThat(fileList[0].filePath).isEqualTo("/folder/video.mp4")
    }

    /**
     * 测试：搜索文件 - 成功
     */
    @Test
    fun testSearchFiles_Success() = runTest {
        // Given: 模拟API响应
        val baiduFiles = listOf(
            BaiduFile(
                fsId = "1",
                path = "/video1.mp4",
                serverFilename = "video1.mp4",
                size = 1024L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 120
            ),
            BaiduFile(
                fsId = "2",
                path = "/video2.mp4",
                serverFilename = "video2.mp4",
                size = 2048L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 180
            )
        )

        val mockResponse = FileListResponse(list = baiduFiles, errno = 0)
        val response = Response.success(mockResponse)
        every {
            mockBaiduPanApiService.searchFiles(
                eq("video"),
                eq("/"),
                eq(true)
            )
        } returns response

        // When: 搜索文件
        val result = fileRepository.searchFiles(key = "video")

        // Then: 验证返回成功结果
        assertThat(result.isSuccess).isTrue()
        val fileList = result.getOrNull()
        assertThat(fileList!!.size).isEqualTo(2)
    }

    /**
     * 测试：搜索文件 - 失败
     */
    @Test
    fun testSearchFiles_Failure() = runTest {
        // Given: 模拟API响应失败
        val response = Response.error<FileListResponse>(404, ResponseBody.create(
            MediaType.parse("text/plain"), "Not found"
        ))
        every { mockBaiduPanApiService.searchFiles(any(), any(), any()) } returns response

        // When: 搜索文件
        val result = fileRepository.searchFiles(key = "nonexistent")

        // Then: 验证返回失败结果
        assertThat(result.isFailure).isTrue()
    }

    /**
     * 测试：获取视频播放信息 - 成功
     */
    @Test
    fun testGetVideoPlayInfo_Success() = runTest {
        // Given: 模拟API响应
        val mockResponse = FileListResponse(
            list = emptyList(),
            downloadLink = "https://example.com/video.mp4",
            errno = 0
        )
        val response = Response.success(mockResponse)
        every { mockBaiduPanApiService.getFileDownloadLink(eq("fs_id_123")) } returns response

        // When: 获取视频播放信息
        val result = fileRepository.getVideoPlayInfo(fsId = "fs_id_123")

        // Then: 验证返回播放链接
        assertThat(result.isSuccess).isTrue()
        val downloadLink = result.getOrNull()
        assertThat(downloadLink).isEqualTo("https://example.com/video.mp4")
    }

    /**
     * 测试：获取视频播放信息 - 失败
     */
    @Test
    fun testGetVideoPlayInfo_Failure() = runTest {
        // Given: 模拟API响应失败
        val response = Response.error<FileListResponse>(500, ResponseBody.create(
            MediaType.parse("text/plain"), "Server error"
        ))
        every { mockBaiduPanApiService.getFileDownloadLink(any()) } returns response

        // When: 获取视频播放信息
        val result = fileRepository.getVideoPlayInfo(fsId = "fs_id_123")

        // Then: 验证返回失败结果
        assertThat(result.isFailure).isTrue()
    }

    /**
     * 测试：文件过滤 - 仅包含支持的媒体类型
     */
    @Test
    fun testGetFileList_FilterSupportedOnly() = runTest {
        // Given: 模拟API响应包含多种文件类型
        val baiduFiles = listOf(
            BaiduFile(
                fsId = "1",
                path = "/video.mp4",
                serverFilename = "video.mp4",
                size = 1024L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 120
            ),
            BaiduFile(
                fsId = "2",
                path = "/image.jpg",
                serverFilename = "image.jpg",
                size = 512L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = null
            ),
            BaiduFile(
                fsId = "3",
                path = "/document.pdf",
                serverFilename = "document.pdf",
                size = 2048L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = null
            ),
            BaiduFile(
                fsId = "4",
                path = "/folder",
                serverFilename = "folder",
                size = 0L,
                isDir = 1,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = null
            )
        )

        val mockResponse = FileListResponse(list = baiduFiles, errno = 0)
        val response = Response.success(mockResponse)
        every { mockBaiduPanApiService.getFileList(any(), any(), any(), any()) } returns response

        // When: 获取文件列表（启用过滤）
        val result = fileRepository.getFileList(filterSupported = true)

        // Then: 验证只返回支持的媒体文件和文件夹
        assertThat(result.isSuccess).isTrue()
        val fileList = result.getOrNull()
        assertThat(fileList!!.size).isEqualTo(3) // video + image + folder
        assertThat(fileList.none { it.fileName.endsWith(".pdf") }).isTrue()
    }

    /**
     * 测试：文件过滤 - 不过滤
     */
    @Test
    fun testGetFileList_NoFilter() = runTest {
        // Given: 模拟API响应包含多种文件类型
        val baiduFiles = listOf(
            BaiduFile(
                fsId = "1",
                path = "/video.mp4",
                serverFilename = "video.mp4",
                size = 1024L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 120
            ),
            BaiduFile(
                fsId = "2",
                path = "/document.pdf",
                serverFilename = "document.pdf",
                size = 2048L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = null
            )
        )

        val mockResponse = FileListResponse(list = baiduFiles, errno = 0)
        val response = Response.success(mockResponse)
        every { mockBaiduPanApiService.getFileList(any(), any(), any(), any()) } returns response

        // When: 获取文件列表（禁用过滤）
        val result = fileRepository.getFileList(filterSupported = false)

        // Then: 验证返回所有文件
        assertThat(result.isSuccess).isTrue()
        val fileList = result.getOrNull()
        assertThat(fileList!!.size).isEqualTo(2)
        assertThat(fileList.any { it.fileName.endsWith(".pdf") }).isTrue()
    }

    /**
     * 测试：递归获取文件列表
     */
    @Test
    fun testGetFileList_Recursive() = runTest {
        // Given: 模拟根目录和子文件夹的文件
        val rootFiles = listOf(
            BaiduFile(
                fsId = "1",
                path = "/video.mp4",
                serverFilename = "video.mp4",
                size = 1024L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 120
            ),
            BaiduFile(
                fsId = "2",
                path = "/subfolder",
                serverFilename = "subfolder",
                size = 0L,
                isDir = 1,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = null
            )
        )

        val subfolderFiles = listOf(
            BaiduFile(
                fsId = "3",
                path = "/subfolder/video2.mp4",
                serverFilename = "video2.mp4",
                size = 2048L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 180
            )
        )

        val mockRootResponse = Response.success(FileListResponse(list = rootFiles, errno = 0))
        val mockSubfolderResponse = Response.success(FileListResponse(list = subfolderFiles, errno = 0))

        every { mockBaiduPanApiService.getFileList(eq("/"), eq(1), eq(100), eq(false)) } returns mockRootResponse
        every { mockBaiduPanApiService.getFileList(eq("/subfolder"), eq(1), eq(100), eq(false)) } returns mockSubfolderResponse

        // When: 递归获取文件列表
        val result = fileRepository.getFileList(recursion = true)

        // Then: 验证返回所有文件（包括子文件夹中的文件）
        assertThat(result.isSuccess).isTrue()
        val fileList = result.getOrNull()
        assertThat(fileList!!.size).isEqualTo(3)
        assertThat(fileList.map { it.fileName }).containsExactly(
            "video.mp4",
            "subfolder",
            "video2.mp4"
        ).inOrder()
    }

    /**
     * 测试：MIME类型推断
     */
    @Test
    fun testMimeTypeInference() = runTest {
        // Given: 包含各种扩展名的文件
        val baiduFiles = listOf(
            BaiduFile(
                fsId = "1",
                path = "/video.mp4",
                serverFilename = "video.mp4",
                size = 1024L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = 120
            ),
            BaiduFile(
                fsId = "2",
                path = "/image.jpg",
                serverFilename = "image.jpg",
                size = 512L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = null
            ),
            BaiduFile(
                fsId = "3",
                path = "/animation.gif",
                serverFilename = "animation.gif",
                size = 256L,
                isDir = 0,
                serverMtime = System.currentTimeMillis() / 1000,
                thumbs = null,
                duration = null
            )
        )

        val mockResponse = FileListResponse(list = baiduFiles, errno = 0)
        val response = Response.success(mockResponse)
        every { mockBaiduPanApiService.getFileList(any(), any(), any(), any()) } returns response

        // When: 获取文件列表
        val result = fileRepository.getFileList()

        // Then: 验证MIME类型正确推断
        assertThat(result.isSuccess).isTrue()
        val fileList = result.getOrNull()
        assertThat(fileList!![0].mimeType).isEqualTo("video/mp4")
        assertThat(fileList[1].mimeType).isEqualTo("image/jpeg")
        assertThat(fileList[2].mimeType).isEqualTo("image/gif")
    }
}
