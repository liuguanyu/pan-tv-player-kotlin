package com.baidu.tv.player.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.baidu.tv.player.geocoding.GeocodingFactory
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.InputStream
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Response
import com.google.common.truth.Truth.assertThat

/**
 * LocationUtils 单元测试
 *
 * 测试GPS坐标提取和地点识别功能
 * 使用 MockK 进行网络和文件操作模拟
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationUtilsTest {

    @MockK
    private lateinit var mockContext: Context

    @MockK
    private lateinit var mockDataStore: DataStore<Preferences>

    @MockK
    private lateinit var mockPreferences: Preferences

    @MockK
    private lateinit var mockGeocodingFactory: GeocodingFactory

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockContext = ApplicationProvider.getApplicationContext()

        // Mock DataStore
        every { mockDataStore.data } returns flowOf(mockPreferences)
        every { mockDataStore.edit(any()) } answers {
            val block = firstArg<(Preferences) -> Unit>()
            block(mockPreferences)
        }

        // Mock GeocodingFactory
        mockkObject(GeocodingFactory)
        every { GeocodingFactory.getAddress(any(), any(), any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkObject(GeocodingFactory)
        LocationUtils.memoryCache.clear()
    }

    /**
     * 测试：从图片获取地点 - 成功
     */
    @Test
    fun testGetLocationFromImage_Success() = runTest {
        // Given: 模拟图片下载和EXIF数据
        val imageUrl = "https://example.com/image.jpg"
        val latitude = 39.9042
        val longitude = 116.4074
        val address = "北京市朝阳区"

        // Mock HTTP connection
        val mockConnection = mockk<java.net.HttpURLConnection>()
        val mockInputStream = mockk<InputStream>()
        val mockFile = mockk<File>()

        every { mockContext.cacheDir } returns mockk()
        every { File.createTempFile(any(), any(), any()) } returns mockFile
        every { mockFile.absolutePath } returns "/tmp/location_exif_123.tmp"
        every { mockFile.delete() } returns true
        every { mockContext.openHttpConnection(any()) } returns mockConnection
        every { mockConnection.responseCode } returns 200
        every { mockConnection.inputStream } returns mockInputStream
        every { mockConnection.disconnect() } just Runs

        // Mock EXIF data
        val mockExifInterface = mockk<android.media.ExifInterface>()
        every { android.media.ExifInterface(any()) } returns mockExifInterface
        every { mockExifInterface.getLatLong(any()) } returns true
        every { mockExifInterface[android.media.ExifInterface.TAG_GPS_LATITUDE] } returns "39.9042"
        every { mockExifInterface[android.media.ExifInterface.TAG_GPS_LONGITUDE] } returns "116.4074"

        // Mock geocoding result
        every { GeocodingFactory.getAddress(any(), eq(latitude), eq(longitude)) } returns address

        // When: 获取图片地点
        val result = LocationUtils.getLocationFromImage(mockContext, imageUrl)

        // Then: 验证返回正确的地址
        assertThat(result).isEqualTo(address)
    }

    /**
     * 测试：从图片获取地点 - 失败
     */
    @Test
    fun testGetLocationFromImage_Failure() = runTest {
        // Given: 模拟图片下载失败
        val imageUrl = "https://example.com/image.jpg"

        // Mock HTTP connection
        val mockConnection = mockk<java.net.HttpURLConnection>()
        every { mockContext.openHttpConnection(any()) } returns mockConnection
        every { mockConnection.responseCode } returns 404

        // When: 获取图片地点
        val result = LocationUtils.getLocationFromImage(mockContext, imageUrl)

        // Then: 验证返回null
        assertThat(result).isNull()
    }

    /**
     * 测试：从视频获取地点 - MediaMetadataRetriever成功
     */
    @Test
    fun testGetLocationFromVideo_MediaMetadataRetrieverSuccess() = runTest {
        // Given: 模拟MediaMetadataRetriever成功获取GPS信息
        val videoUrl = "https://example.com/video.mp4"
        val locationString = "+39.9042-116.4074"
        val address = "北京市朝阳区"

        // Mock MediaMetadataRetriever
        val mockRetriever = mockk<android.media.MediaMetadataRetriever>()
        every { android.media.MediaMetadataRetriever() } returns mockRetriever
        every { mockRetriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_LOCATION) } returns locationString
        every { mockRetriever.release() } just Runs

        // Mock geocoding result
        every { GeocodingFactory.getAddress(any(), any(), any()) } returns address

        // When: 获取视频地点
        val result = LocationUtils.getLocationFromVideo(mockContext, videoUrl)

        // Then: 验证返回正确的地址
        assertThat(result).isEqualTo(address)
    }

    /**
     * 测试：从视频获取地点 - 文件头提取成功
     */
    @Test
    fun testGetLocationFromVideo_FileHeaderSuccess() = runTest {
        // Given: 模拟文件头提取成功
        val videoUrl = "https://example.com/video.mp4"
        val address = "北京市朝阳区"

        // Mock file header extraction
        every { LocationUtils.getLocationFromVideoHeader(any(), any()) } returns address
        every { LocationUtils.getLocationFromVideoTail(any(), any()) } returns null
        every { GeocodingFactory.getAddress(any(), any(), any()) } returns null

        // When: 获取视频地点
        val result = LocationUtils.getLocationFromVideo(mockContext, videoUrl)

        // Then: 验证返回正确的地址
        assertThat(result).isEqualTo(address)
    }

    /**
     * 测试：从视频获取地点 - 文件尾部提取成功
     */
    @Test
    fun testGetLocationFromVideo_FileTailSuccess() = runTest {
        // Given: 模拟文件尾部提取成功
        val videoUrl = "https://example.com/video.mp4"
        val address = "北京市朝阳区"

        // Mock file header extraction
        every { LocationUtils.getLocationFromVideoHeader(any(), any()) } returns null
        every { LocationUtils.getLocationFromVideoTail(any(), any()) } returns address
        every { GeocodingFactory.getAddress(any(), any(), any()) } returns null

        // When: 获取视频地点
        val result = LocationUtils.getLocationFromVideo(mockContext, videoUrl)

        // Then: 验证返回正确的地址
        assertThat(result).isEqualTo(address)
    }

    /**
     * 测试：从视频获取地点 - 所有方法都失败
     */
    @Test
    fun testGetLocationFromVideo_AllFail() = runTest {
        // Given: 所有提取方法都失败
        val videoUrl = "https://example.com/video.mp4"

        // Mock all extraction methods
        every { LocationUtils.getLocationFromVideoHeader(any(), any()) } returns null
        every { LocationUtils.getLocationFromVideoTail(any(), any()) } returns null
        every { GeocodingFactory.getAddress(any(), any(), any()) } returns null

        // When: 获取视频地点
        val result = LocationUtils.getLocationFromVideo(mockContext, videoUrl)

        // Then: 验证返回null
        assertThat(result).isNull()
    }

    /**
     * 测试：解析位置字符串
     */
    @Test
    fun testParseLocationString_Success() {
        // Given: ISO-6709格式的位置字符串
        val locationString = "+39.9042-116.4074"
        val expectedLatitude = 39.9042
        val expectedLongitude = 116.4074
        val address = "北京市朝阳区"

        // Mock geocoding result
        every { GeocodingFactory.getAddress(any(), eq(expectedLatitude), eq(expectedLongitude)) } returns address

        // When: 解析位置字符串
        val result = LocationUtils.parseLocationString(mockContext, locationString)

        // Then: 验证返回正确的地址
        assertThat(result).isEqualTo(address)
    }

    /**
     * 测试：解析位置字符串 - 格式错误
     */
    @Test
    fun testParseLocationString_InvalidFormat() {
        // Given: 无效的位置字符串
        val locationString = "invalid format"

        // When: 解析位置字符串
        val result = LocationUtils.parseLocationString(mockContext, locationString)

        // Then: 验证返回null
        assertThat(result).isNull()
    }

    /**
     * 测试：从坐标获取地点 - 内存缓存命中
     */
    @Test
    fun testGetLocationFromCoordinates_MemoryCacheHit() = runTest {
        // Given: 内存缓存中存在地址
        val latitude = 39.9042
        val longitude = 116.4074
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"
        LocationUtils.memoryCache[cacheKey] = address

        // When: 从坐标获取地点
        val result = LocationUtils.getLocationFromCoordinates(mockContext, latitude, longitude)

        // Then: 验证返回内存缓存的地址
        assertThat(result).isEqualTo(address)
        verify(exactly = 0) { GeocodingFactory.getAddress(any(), any(), any()) }
    }

    /**
     * 测试：从坐标获取地点 - 本地缓存命中
     */
    @Test
    fun testGetLocationFromCoordinates_DiskCacheHit() = runTest {
        // Given: 本地缓存中存在地址
        val latitude = 39.9042
        val longitude = 116.4074
        val cacheKey = "39.9042,116.4074"
        val fullKey = "loc_$cacheKey"
        val address = "北京市朝阳区"

        every { mockPreferences[stringPreferencesKey(fullKey)] } returns address
        every { mockPreferences[longPreferencesKey("${fullKey}_time")] } returns System.currentTimeMillis()

        // When: 从坐标获取地点
        val result = LocationUtils.getLocationFromCoordinates(mockContext, latitude, longitude)

        // Then: 验证返回本地缓存的地址
        assertThat(result).isEqualTo(address)
        verify(exactly = 1) { mockDataStore.data }
        verify(exactly = 0) { GeocodingFactory.getAddress(any(), any(), any()) }
    }

    /**
     * 测试：从坐标获取地点 - 缓存未命中，策略成功
     */
    @Test
    fun testGetLocationFromCoordinates_CacheMiss_StrategySuccess() = runTest {
        // Given: 缓存未命中，策略成功
        val latitude = 39.9042
        val longitude = 116.4074
        val cacheKey = "39.9042,116.4074"
        val address = "北京市朝阳区"

        every { GeocodingFactory.getAddress(any(), eq(latitude), eq(longitude)) } returns address

        // When: 从坐标获取地点
        val result = LocationUtils.getLocationFromCoordinates(mockContext, latitude, longitude)

        // Then: 验证返回策略的结果
        assertThat(result).isEqualTo(address)
        verify(exactly = 1) { GeocodingFactory.getAddress(any(), any(), any()) }
        // 验证地址已存入缓存
        assertThat(LocationUtils.memoryCache[cacheKey]).isEqualTo(address)
    }

    /**
     * 测试：从坐标获取地点 - 所有策略都失败
     */
    @Test
    fun testGetLocationFromCoordinates_AllStrategiesFail() = runTest {
        // Given: 所有策略都失败
        val latitude = 39.9042
        val longitude = 116.4074

        every { GeocodingFactory.getAddress(any(), any(), any()) } returns null

        // When: 从坐标获取地点
        val result = LocationUtils.getLocationFromCoordinates(mockContext, latitude, longitude)

        // Then: 验证返回null
        assertThat(result).isNull()
    }
}
