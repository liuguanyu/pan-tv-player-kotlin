package com.baidu.tv.player.network

import com.baidu.tv.player.model.DeviceCodeResponse
import com.baidu.tv.player.model.FileInfo
import com.baidu.tv.player.model.FileListResponse
import com.baidu.tv.player.model.TokenResponse
import com.baidu.tv.player.model.UserInfoResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * 百度网盘API服务接口
 */
interface BaiduPanService {

    /**
     * 获取设备码
     *
     * @param clientId 应用ID
     * @param scope 授权范围
     * @param responseType 响应类型，固定为"device_code"
     * @return 设备码响应
     */
    @GET(ApiConstants.ENDPOINT_DEVICE_CODE)
    fun getDeviceCode(
        @Query("client_id") clientId: String,
        @Query("scope") scope: String,
        @Query("response_type") responseType: String
    ): Call<DeviceCodeResponse>

    /**
     * 轮询设备码状态获取token
     *
     * @param grantType 授权类型，固定为"device_token"
     * @param deviceCode 设备码
     * @param clientId 应用ID
     * @param clientSecret 应用密钥
     * @return Token响应
     */
    @GET(ApiConstants.ENDPOINT_TOKEN)
    fun getTokenByDeviceCode(
        @Query("grant_type") grantType: String,
        @Query("code") deviceCode: String,
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String
    ): Call<TokenResponse>

    /**
     * 刷新token
     *
     * @param grantType 授权类型，固定为"refresh_token"
     * @param refreshToken 刷新令牌
     * @param clientId 应用ID
     * @param clientSecret 应用密钥
     * @return Token响应
     */
    @GET(ApiConstants.ENDPOINT_TOKEN)
    fun refreshToken(
        @Query("grant_type") grantType: String,
        @Query("refresh_token") refreshToken: String,
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String
    ): Call<TokenResponse>

    /**
     * 获取用户信息
     *
     * @param method 方法名，固定为"get_user_info"
     * @param accessToken 访问令牌
     * @return 用户信息响应
     */
    @GET(ApiConstants.ENDPOINT_NAS)
    fun getUserInfo(
        @Query("method") method: String,
        @Query("access_token") accessToken: String
    ): Call<UserInfoResponse>

    /**
     * 获取文件列表
     *
     * @param method 方法名，固定为"list"
     * @param dir 目录路径
     * @param order 排序方式
     * @param desc 是否降序
     * @param start 起始位置
     * @param limit 限制数量
     * @param web 是否显示web信息
     * @param folder 是否只显示文件夹
     * @param accessToken 访问令牌
     * @return 文件列表响应
     */
    @GET(ApiConstants.ENDPOINT_FILE)
    fun getFileList(
        @Query("method") method: String,
        @Query("dir") dir: String,
        @Query("order") order: String,
        @Query("desc") desc: Int,
        @Query("start") start: Int,
        @Query("limit") limit: Int,
        @Query("web") web: Int,
        @Query("folder") folder: Int,
        @Query("access_token") accessToken: String
    ): Call<FileListResponse>

    /**
     * 递归获取文件列表
     *
     * @param method 方法名，固定为"list"
     * @param path 路径
     * @param order 排序方式
     * @param desc 是否降序
     * @param limit 限制数量
     * @param recursion 是否递归
     * @param accessToken 访问令牌
     * @return 文件列表响应
     */
    @GET(ApiConstants.ENDPOINT_MULTIMEDIA)
    fun getFileListRecursive(
        @Query("method") method: String,
        @Query("path") path: String,
        @Query("order") order: String,
        @Query("desc") desc: Int,
        @Query("limit") limit: Int,
        @Query("recursion") recursion: Int,
        @Query("access_token") accessToken: String
    ): Call<FileListResponse>

    /**
     * 获取文件信息（包括下载链接）
     *
     * @param method 方法名，固定为"meta"
     * @param fsids 文件ID列表
     * @param dlink 是否获取下载链接
     * @param accessToken 访问令牌
     * @return 文件信息响应
     */
    @GET(ApiConstants.ENDPOINT_MULTIMEDIA)
    fun getFileInfo(
        @Query("method") method: String,
        @Query("fsids") fsids: String,
        @Query("dlink") dlink: Int,
        @Query("access_token") accessToken: String
    ): Call<FileListResponse>
}