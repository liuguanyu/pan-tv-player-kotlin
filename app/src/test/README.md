# Pan TV Player Kotlin - 测试文档

## 测试概述

本项目实现了全面的单元测试和集成测试，覆盖率目标 >85%。

## 测试框架

- **JUnit 4**: 基础测试框架
- **MockK**: Kotlin专用Mock框架（替代Mockito）
- **Kotlin Coroutines Test**: 协程测试支持
- **AndroidX Test**: Android集成测试框架
- **Google Truth**: 断言库
- **Room Testing**: 数据库测试支持

## 测试目录结构

```
app/src/
├── test/                          # 单元测试
│   └── java/com/baidu/tv/player/
│       ├── auth/                  # 认证模块测试
│       │   ├── AuthRepositoryTest.kt
│       │   └── AuthServiceTest.kt
│       ├── repository/            # 数据仓库测试
│       │   ├── PlaylistRepositoryTest.kt
│       │   └── PlaybackHistoryRepositoryTest.kt
│       ├── ui/
│       │   ├── filebrowser/       # 文件浏览测试
│       │   │   └── FileRepositoryTest.kt
│       │   ├── playback/          # 播放器测试
│       │   │   └── PlaybackViewModelTest.kt
│       │   └── settings/          # 设置模块测试
│       │       └── SettingsRepositoryTest.kt
│       ├── geocoding/             # 地点识别测试
│       │   ├── GeocodingFactoryTest.kt
│       │   └── LocationCacheTest.kt
│       └── utils/                 # 工具类测试
│           ├── LocationUtilsTest.kt
│           ├── QRCodeUtilsTest.kt
│           └── RandomUtilsTest.kt
│
└── androidTest/                   # 集成测试
    └── java/com/baidu/tv/player/
        ├── auth/                  # 认证模块集成测试
        │   └── AuthRepositoryIntegrationTest.kt
        └── repository/            # 数据仓库集成测试
            └── PlaylistRepositoryIntegrationTest.kt
```

## 测试覆盖模块

### 1. 认证模块 (Authentication)

#### 单元测试
- **AuthRepositoryTest**: 测试登录状态管理、token存储/读取
  - 检查认证状态（已认证/未认证）
  - 获取设备码（成功/失败）
  - 轮询设备码状态（成功/失败）
  - 刷新token（成功/失败）
  - 退出登录
  - 获取认证信息

- **AuthServiceTest**: 测试设备码流程、token刷新
  - 获取设备ID（新设备/已有设备）
  - 检查是否已认证（已认证/未认证/token过期）
  - 获取访问令牌
  - 获取设备码（成功/失败）
  - 轮询设备码状态（授权成功/授权中）
  - 刷新token（成功/失败）

#### 集成测试
- **AuthRepositoryIntegrationTest**: 真实环境下的认证流程测试

### 2. 数据库模块 (Database)

#### 单元测试
- **PlaylistRepositoryTest**: 测试播放列表CRUD操作
  - 获取所有播放列表
  - 获取单个播放列表
  - 插入或更新播放列表
  - 更新播放列表
  - 删除播放列表
  - 删除所有播放列表
  - 获取不存在的播放列表
  - 插入重复的播放列表

- **PlaybackHistoryRepositoryTest**: 测试播放记录存储/查询
  - 获取所有播放历史记录
  - 获取最近的播放历史记录
  - 获取最新的4条播放历史记录
  - 获取单个播放历史记录
  - 根据文件路径获取播放历史记录
  - 插入或更新播放历史记录
  - 更新播放历史记录
  - 删除播放历史记录
  - 删除所有播放历史记录

#### 集成测试
- **PlaylistRepositoryIntegrationTest**: 真实数据库环境下的CRUD操作

### 3. 文件浏览模块 (File Browser)

#### 单元测试
- **FileRepositoryTest**: 测试文件列表获取、过滤
  - 获取文件列表（成功/失败）
  - 获取文件列表（带路径参数）
  - 搜索文件（成功/失败）
  - 获取视频播放信息（成功/失败）
  - 文件过滤（仅包含支持的媒体类型）
  - 文件过滤（不过滤）
  - 递归获取文件列表
  - MIME类型推断

### 4. 播放器模块 (Playback)

#### 单元测试
- **PlaybackViewModelTest**: 测试播放状态管理
  - 播放状态
  - 设置播放列表
  - 播放指定索引的文件
  - 播放下一个文件（顺序/随机/单曲循环模式）
  - 播放上一个文件（顺序/随机/单曲循环模式）
  - 设置播放模式
  - 获取当前播放文件
  - 获取当前播放模式
  - 获取播放设置
  - 获取播放列表
  - 获取当前播放索引
  - 检查是否有下一个/上一个文件
  - 获取下一个/上一个文件
  - 预加载下一个文件
  - 保存和获取播放进度
  - 播放器类型指示器（视频/图片）

### 5. 地点识别模块 (Geocoding)

#### 单元测试
- **GeocodingFactoryTest**: 测试策略优先级选择
  - 获取地址（高德地图成功）
  - 获取地址（高德地图失败，Android Geocoder成功）
  - 获取地址（高德地图和Android Geocoder失败，Nominatim成功）
  - 获取地址（所有策略都失败）
  - 获取地址（高德地图抛出异常，Android Geocoder成功）
  - 获取所有策略名称
  - 测试所有策略的可用性

- **LocationCacheTest**: 测试双层缓存
  - 从内存缓存获取地址
  - 从内存缓存获取过期地址
  - 从本地缓存获取地址
  - 从本地缓存获取过期地址
  - 保存地址到双层缓存
  - 内存缓存达到最大容量时的LRU清理
  - 清除所有缓存
  - 获取缓存统计信息

### 6. 设置模块 (Settings)

#### 单元测试
- **SettingsRepositoryTest**: 测试设置存储/读取
  - 获取设置（默认值）
  - 获取设置（自定义值）
  - 保存设置
  - 保存图片特效设置
  - 保存背景模式
  - 保存地点显示设置
  - 保存H.265设置

### 7. 工具类 (Utils)

#### 单元测试
- **LocationUtilsTest**: 测试GPS坐标提取
  - 从图片获取地点（成功/失败）
  - 从视频获取地点（MediaMetadataRetriever成功/文件头提取成功/文件尾部提取成功/所有方法都失败）
  - 解析位置字符串（成功/格式错误）
  - 从坐标获取地点（内存缓存命中/本地缓存命中/缓存未命中/所有策略都失败）

- **QRCodeUtilsTest**: 测试二维码生成
  - 生成二维码（成功）
  - 生成二维码（空内容）
  - 生成二维码（非法尺寸）
  - 生成二维码（大尺寸）

- **RandomUtilsTest**: 测试Fisher-Yates算法
  - Fisher-Yates洗牌算法（整数数组）
  - Fisher-Yates洗牌算法（字符串数组）
  - Fisher-Yates洗牌算法（单个元素数组）
  - Fisher-Yates洗牌算法（空数组）
  - 生成指定范围内的随机整数
  - 生成0到1之间的随机浮点数

## 运行测试

### 运行所有单元测试
```bash
./gradlew test
```

### 运行所有集成测试
```bash
./gradlew connectedAndroidTest
```

### 运行特定模块的测试
```bash
# 运行认证模块测试
./gradlew test --tests "com.baidu.tv.player.auth.*"

# 运行数据库模块测试
./gradlew test --tests "com.baidu.tv.player.repository.*"

# 运行文件浏览模块测试
./gradlew test --tests "com.baidu.tv.player.ui.filebrowser.*"

# 运行播放器模块测试
./gradlew test --tests "com.baidu.tv.player.ui.playback.*"

# 运行地点识别模块测试
./gradlew test --tests "com.baidu.tv.player.geocoding.*"

# 运行工具类测试
./gradlew test --tests "com.baidu.tv.player.utils.*"
```

### 生成测试覆盖率报告
```bash
./gradlew testDebugUnitTestCoverage
```

## 测试原则

1. **单元测试**: 使用MockK模拟依赖项，测试单一职责
2. **集成测试**: 使用真实组件，测试模块间交互
3. **协程测试**: 使用`runTest`和`TestDispatcher`进行协程测试
4. **异步测试**: 使用`InstantTaskExecutorRule`确保LiveData同步执行
5. **断言**: 使用Google Truth提供清晰的断言语法
6. **测试隔离**: 每个测试独立运行，不依赖其他测试
7. **测试命名**: 使用`test<方法名>_<场景>`命名规范
8. **测试注释**: 每个测试包含Given-When-Then注释

## 测试覆盖率目标

- 总体覆盖率: **>85%**
- 单元测试覆盖率: **>80%**
- 集成测试覆盖率: **>60%**
- 关键业务逻辑覆盖率: **>90%**

## 测试最佳实践

1. 使用`@Before`初始化测试环境
2. 使用`@After`清理测试资源
3. 使用`@Rule`配置测试规则
4. 使用MockK的`relaxUnitFun`简化mock配置
5. 使用协程的`runTest`进行异步测试
6. 使用Room的内存数据库进行数据库测试
7. 避免使用真实的网络请求
8. 避免使用真实的文件系统操作
9. 每个测试只测试一个功能点
10. 测试应该快速、可靠、独立

## 注意事项

1. 所有单元测试使用MockK，不使用Mockito
2. 所有协程测试使用kotlinx-coroutines-test
3. 所有数据库测试使用Room内存数据库
4. 所有网络请求使用MockWebServer或mock
5. 所有文件操作使用临时文件
6. 所有异步操作使用TestDispatcher
7. 所有LiveData测试使用InstantTaskExecutorRule
8. 所有Flow测试使用`first()`或`toList()`

## 测试依赖

```gradle
// Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito:mockito-core:5.7.0")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("com.google.truth:truth:1.1.5")

androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.test:runner:1.5.2")
androidTestImplementation("androidx.test:rules:1.5.0")
androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
```

## 常见问题

### Q: 如何测试协程?
A: 使用`runTest`和`TestDispatcher`，示例：
```kotlin
@Test
fun testCoroutine() = runTest {
    // Given
    val result = someSuspendFunction()

    // Then
    assertThat(result).isTrue()
}
```

### Q: 如何测试LiveData?
A: 使用`InstantTaskExecutorRule`和`observeForever`，示例：
```kotlin
@get:Rule
val instantTaskExecutorRule = InstantTaskExecutorRule()

@Test
fun testLiveData() {
    val observer = mockk<Observer<String>>()
    liveData.observeForever(observer)

    viewModel.someFunction()

    verify { observer.onChanged("expected value") }
}
```

### Q: 如何测试Flow?
A: 使用`first()`或`toList()`，示例：
```kotlin
@Test
fun testFlow() = runTest {
    val flow = repository.getData()
    val result = flow.first()

    assertThat(result).isEqualTo(expectedValue)
}
```

### Q: 如何测试Room数据库?
A: 使用内存数据库，示例：
```kotlin
@Before
fun setup() {
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}

@After
fun tearDown() {
    database.close()
}
```

## 总结

本测试套件提供了全面的测试覆盖，确保代码质量和稳定性。所有测试遵循最佳实践，使用现代化的测试框架和工具，为项目的持续发展提供了坚实的保障。
