# Shared Music（MusicApplication）项目总结

> 整理日期：2026-10-14 ｜ 基于 `main` 分支最新提交 `0b8e75d`（另有 2 处未提交的小改动，见第 10 节）
>
> 下文源码路径默认省略公共前缀 `app/src/main/java/com/example/musicapplication/`

---

## 1. 项目概览

Shared Music 是一个用 **Kotlin + Jetpack Compose** 开发的 Android 音乐播放器，对接自建后端（`http://106.12.119.143/api/`）。

主要能力：账号登录 / 注册 / 找回密码、在线歌曲分页浏览与搜索、收藏、最近播放、下载后离线播放、从文件管理器打开本地音频、后台播放与系统媒体通知、歌词滚动高亮、可拖拽排序的播放队列、四种播放模式、6 套主题色、应用内公告。

| 项目 | 值 |
|---|---|
| 应用名 | Shared Music |
| applicationId / namespace | `com.sharedmusic.app` / `com.example.musicapplication` |
| 版本 | versionCode 3，versionName 1.0.2 |
| SDK | minSdk 31（Android 12）；compileSdk / targetSdk 36 |
| 屏幕方向 | 仅竖屏 |
| 代码规模 | 145 个 Kotlin 文件 + 1 个 Java 文件，约 1.45 万行 |
| 开发周期 | 30 次提交，2025-09-09 → 2026-06-20 |

---

## 2. 技术栈

| 类别 | 选型（版本） |
|---|---|
| 语言 / 构建 | Kotlin 2.0.21、AGP 8.12.1、Gradle 8.13、KSP 2.0.21-1.0.27、JVM target 11 |
| UI | Jetpack Compose（BOM 2024.09.00）、Material3、material-icons-extended、Navigation Compose 2.9.4 |
| 架构 | MVVM + Repository；Coroutines / Flow / StateFlow |
| 依赖注入 | Hilt 2.56.2、hilt-navigation-compose 1.2.0 |
| 网络 | Retrofit 2.9.0（Gson + Scalars 转换器）、OkHttp 4.12.0 + logging-interceptor |
| 本地存储 | Room 2.7.2、DataStore Preferences 1.2.1、SharedPreferences |
| 播放 | Media3 1.8.0（ExoPlayer、Session、datasource-okhttp、UI） |
| 图片 | Coil 2.6.0；Palette（提取封面主色） |
| 第三方组件 | `sh.calvin.reorderable` 2.5.1（队列拖拽排序）、`com.vanniktech:android-image-cropper` 4.7.0（头像裁剪） |

---

## 3. 目录结构

```text
app/src/main/java/com/example/musicapplication/
├── MusicApplication.kt          @HiltAndroidApp；给 Coil 提供带鉴权的 OkHttpClient
├── MainActivity.kt              单 Activity：沉浸式、申请通知权限、接收外部音频 Intent
├── config/RouterConfig.kt       顶层路由常量
├── playback/PlaybackService.kt  Media3 MediaSessionService（前台播放服务 + 系统媒体通知）
├── domain/
│   ├── model/                   Song、MusicSource、SongListKey、UserProfile、UserStat、AppNotification…
│   ├── mapper/                  Song → 最近播放实体
│   └── player/
│       ├── MediaControllerManager.kt   ★ 当前使用：连接 PlaybackService 的播放控制封装
│       └── MusicPlayerManager.kt       旧版直连 ExoPlayer 的封装（已不再使用）
├── data/
│   ├── common/                  RepositoryWorkResult（Success / Failure）
│   ├── session/                 SessionManager + AuthState（全局登录态）
│   ├── remote/                  api/、dto/、interceptor/、mapper/、di/NetworkModule
│   ├── local/                   song(Room)、token、player、profile、userstat、theme、search、
│   │                            notification、artwork(封面缓存)、download(下载工具)
│   └── repository/              Auth / Song / Profile / UserStat / AppNotification / MainPage
├── ui/
│   ├── appNaviagtion/           顶层 NavHost：Splash → Login → MainPage
│   ├── splash/  login/  session/
│   ├── mainPage/
│   │   ├── MainPage.kt          二级 NavHost + 底部导航栏 + 全局 PlayerSheet
│   │   ├── home/  search/
│   │   ├── audioPlayer/         PlayerSheet、PlayerViewModel、进度条、歌词/歌曲 UI 状态
│   │   └── profile/             个人中心 + account / download / favorite / recent / setting / about
│   ├── component/               通用组件（底部栏、输入框、列表脚手架、模糊背景等）
│   └── theme/                   6 套主题预设 + LocalMusicThemeColors
└── utils/                       LrcParser、MediaUrlUtils、LocalAudioMetaDataReader、时间格式化
```

---

## 4. 整体架构

### 4.1 分层与数据流

```text
Compose Screen ──collect──▶ ViewModel(StateFlow) ──▶ Repository ──┬──▶ Retrofit API（remote）
       ▲                                                          └──▶ Room / DataStore（local）
       └──────────────── Room Flow / DataStore Flow 自动推送 ◀────────────┘
```

- **歌曲列表以 Room 为唯一数据源**：`SongResponse → SongEntity → Room → Flow<List<Song>> → ViewModel → UI`。首页、喜欢、搜索、播放器监听同一份数据，收藏状态改一处、处处同步。
- Repository 统一返回 `RepositoryWorkResult`，ViewModel 不直接处理 Retrofit 异常和后端原始 `ApiResponse`。
- 一次性事件（Toast 提示）使用 `MutableSharedFlow`，或"可消费的 errorMsg 字段 + `consumeXxx()`"。
- Hilt 模块全部装在 `SingletonComponent`：`NetworkModule`、`LocalDatabaseModule`、`RepositoryModule`，以及 Token / Profile / UserStat / Theme / PlayerState / SearchHistory / AppNotification 各自的 `@Binds` 模块（接口 + 实现，便于替换和测试）。

### 4.2 播放架构

```text
PlayerSheet（UI）
   │
PlayerViewModel ─── 队列 / 播放模式 / 歌词 / 收藏 / 下载 / 状态快照（作用域：MainPage 导航条目）
   │
MediaControllerManager（@Singleton）─── MediaController；状态以 StateFlow / SharedFlow 暴露
   │  Media3 Session（跨组件通信）
PlaybackService（MediaSessionService）─── ExoPlayer + MediaSession + 系统媒体通知
   │
OkHttpDataSource（@Named("media") 客户端：自动带 Token、过期自动刷新）
```

播放器跑在前台服务中：退到后台、锁屏后继续播放，通知栏 / 耳机 / 蓝牙均可控制。

---

## 5. 功能模块详解

### 5.1 启动与登录态

- `SplashScreen`：等待约 800ms，直接读取 SharedPreferences `token_prefs` 判断是否有 token，跳转主页或登录页。
- `SessionManager`（单例）维护 `AuthState`：`Authenticated` / `Unauthenticated` / `SessionExpired`。`AppNavigation` 监听到 `SessionExpired` 时自动跳回登录页并清掉主页栈。
- `LoginScreen`：同一页面切换 **登录 / 注册 / 忘记密码** 三种模式（`LoginMode`），注册与重置密码使用邮箱验证码。

### 5.2 网络与认证

三个 OkHttpClient：

| 名称 | 用途 | 特点 |
|---|---|---|
| 默认 | 业务接口、Coil 图片 | Logging(BODY) + AuthInterceptor + TokenAuthenticator，超时 10s |
| `@Named("refresh")` | 仅用于 `refreshToken` 接口 | 不带鉴权组件，避免依赖循环 |
| `@Named("media")` | 音频流、封面缓存、下载 | 带鉴权但不打印 body（避免大文件 OOM），`readTimeout = 0` |

- `AuthInterceptor`：每次请求实时从 TokenStore 读取 access token，添加 `Authorization: Bearer …`。
- `TokenAuthenticator`（HTTP 401 时触发）：
  1. 请求头已有 `Already-Retried` → 判定会话过期；
  2. 加锁；若 token 已被其他线程刷新过 → 直接用最新 token 重试；
  3. 否则用 refresh token 调 `refreshToken`：成功则保存并重试，失败则 `onSessionExpired()` 回到登录页。
- 后端返回的封面 / 音频 / 歌词路径可以是相对路径，由 `MediaUrlUtils.buildMediaUrl()` 拼接 `BASE_URL`。

### 5.3 歌曲数据与分页缓存（Room：`song_database`，version 1）

| 表 | 作用 |
|---|---|
| `songs` | 歌曲本体：标题、歌手、封面 / 音频 / 歌词 URL、是否喜欢 |
| `song_list_items` | 列表与歌曲的关系，主键 (listKey, songId)，含 position、page |
| `song_list_meta` | 每个列表的分页元数据：current / pages / total / size |
| `recent_song_play` | 最近播放：自增 id（允许重复）、保留最近 100 条、区分 `remote` / `local_uri` |
| `downloadSongs` | 已下载歌曲：本地音频 / 封面 / 歌词路径、文件大小、下载时间 |

- `SongListKey`：`Hot("hot")`、`Loved("favorite")`、`Search("search_关键词")`、`Playlist("playlist_id")`（歌单接口尚未实现）。
- `refreshSongs`：请求第 1 页，清空该列表后写入；`loadMoreSongs`：按 meta 取下一页追加；`ensureSongAtPosition`：播放器按"列表位置"取歌，本地没有就按页补拉。
- 收藏 / 取消收藏是**乐观更新**：先改本地 `songs` 和 `recent_song_play`，接口失败再回滚；成功后插入 / 删除 `favorite` 列表关系。
- 列表页统一为"滚动到倒数第 3 项自动 loadMore + 下拉刷新 + 底部'已经到底了'"。

### 5.4 播放系统

**PlaybackService**
- ExoPlayer 使用 `DefaultDataSource` + `OkHttpDataSource`（media 客户端），网络 URL、`content://`、`file://` 都能播。
- 设置 `AudioAttributes(USAGE_MEDIA, MUSIC)` 并自动处理音频焦点；自定义通知渠道"音乐播放"和小图标。

**MediaControllerManager**
- 异步连接 Session；未连上时把 `playSong / playQueue / restoreQueue` 暂存为 pending，连上后再执行。
- `playbackCommandVersion` 版本号丢弃过期的异步命令（例如快速连点两首歌，前一首的封面下载晚完成也不会覆盖后一首）。
- 每 500ms 轮询进度；拖动进度条期间暂停轮询，避免拖动和刷新互相打架。
- **通知栏封面**：远程封面需要 token，系统 UI 无法直接加载。因此先把封面下载到 `cache/artwork/{songId}.{ext}`，再通过 FileProvider 生成 `content://` URI 并授权给 `com.android.systemui`；当前曲目同步获取，下一首提前预加载。

**PlayerViewModel**
- **队列**：`songQueue` + 一一对应的 `queueKeys`（自增 seed 生成）。允许同一首歌在队列里重复出现，key 用于 LazyColumn 和拖拽排序。
- **播放模式**：`Sequence` 顺序（播完最后一首暂停并回到开头）、`Repeat` 列表循环、`RepeatOne` 单曲循环、`Shuffle` 随机。前三种映射到 ExoPlayer 的 repeatMode；随机模式由 VM 自己打乱队列（当前歌曲固定在第一位），同时保留 `originalQueue`，切回其他模式时计算一组 `moveMediaItem` 操作，在不打断播放的情况下恢复原顺序。
- **两种队列来源**：
  - 完整列表（下载、最近播放、最近搜索）：`playQueueSong(songs, index)`；
  - 分页列表（热门、喜欢、搜索结果）：`playQueueSong(listKey, songs, index)`，记录 `playbackListKey / playbackListPosition`，上一首 / 下一首通过 `ensureSongAtPosition` 越过已加载范围继续取歌。
- **每首歌开始播放时**：更新 UI 状态、加载歌词、远程歌曲上报播放次数、用户播放统计 +1、写入最近播放。
- **歌词**：远程 URL 用 OkHttp 获取，已下载歌曲读本地文件；`LrcParser` 解析 `[mm:ss.xx]`，按当前秒数高亮并滚动。
- **背景色**：从封面缓存解码 Bitmap，用 Palette 取 dominant / vibrant 色，用于全屏播放器的模糊渐变背景。
- **状态持久化**：每 3 秒及每次队列 / 模式变化，把队列（Gson JSON）、当前下标、进度、模式写入 DataStore；下次启动恢复队列并暂停在上次的位置（兼容旧版只存 songId 的格式）。

### 5.5 播放器 UI：PlayerSheet

- 用一个 `Animatable` 进度 `p ∈ [0, 1]` 驱动"迷你播放条 → 全屏播放器"的连续形变（位置、宽高、圆角、封面尺寸、透明度插值），仿 Apple Music。
- 支持上下拖拽：按速度（上甩 < -1200、下甩 > 900）或位置（> 0.68）决定展开 / 收起；展开时返回键优先收起播放器；底部导航栏随展开渐隐下移。
- 全屏布局按屏幕比例划分（上 70% 封面 / 面板，下 30% 控制区），适配不同尺寸的手机。
- 三个面板：**封面 / 歌词 / 播放队列**（队列可拖拽排序、切换播放模式）。
- "更多操作"弹层：**下载**（已实现）、定时睡眠（占位）、分享歌曲（占位）。

### 5.6 页面与路由

| 路由 | 页面 | ViewModel | 功能 |
|---|---|---|---|
| `splashScreen` | SplashScreen | — | 启动页，判断登录态 |
| `login?redirect={redirect}` | LoginEntry | LoginViewModel | 登录 / 注册 / 忘记密码 |
| `mainPage` | MainPage | PlayerViewModel 等 | 二级导航容器 + 全局播放器 |
| `home` | HomeScreen | HomeScreenViewModel | 发现页：搜索入口、推荐歌单（敬请期待）、热门歌曲分页、公告铃铛 |
| `search` | SearchScreen | SearchViewModel | 350ms 防抖搜索、分页加载、最近搜索 |
| `profile` | ProfileScreen | ProfileViewModel、ThemeViewModel | 头像昵称、播放 / 收藏统计、主题色、功能入口 |
| `profile/account` | ProfileAccountScreen | ProfileAccountViewModel | 选图 + 裁剪上传头像、修改昵称 |
| `profile/download` | DownloadMusicScreen | DownloadMusicViewModel | 已下载列表、播放全部、长按多选删除 |
| `profile/favorite` | FavoriteMusicScreen | FavoriteMusicViewModel | 喜欢的歌曲（分页、下拉刷新） |
| `profile/history` | RecentMusicScreen | RecentMusicViewModel | 最近播放（本地，最多 100 条） |
| `profile/setting` | ProfileSettingScreen | ProfileSettingViewModel | 清理图片缓存、退出登录 |
| `profile/about` | AboutMusicScreen | — | 应用说明、官网地址、反馈邮箱 |

### 5.7 其他功能

- **下载与离线播放**：`DownloadUtils` 先把音频（必需）、封面和歌词（可选）下载到 `files/downloads/tmp/…`，全部成功后整体 rename 到 `files/downloads/songs/{songId}/`，再写入 `downloadSongs` 表；播放时作为 `MusicSource.Local(file://…)`，断网可播。
- **搜索**："最近搜索"保存的是从搜索结果中点击播放过的**歌曲 ID**（DataStore，最多 10 个），不是搜索关键词。
- **应用公告**：首页启动时请求 `notification/latest`，若公告 ID 与上次自动弹出的不同就弹窗（支持图片，即最新提交"通知可以传图片"）；点击铃铛可手动查看。
- **外部打开音频**：Manifest 声明了 `ACTION_VIEW` + `content://` + `audio/*`；从文件管理器等打开音频时，`LocalAudioMetaDataReader` 读取标题、歌手、内嵌封面后直接播放。
- **主题**：6 套预设（松石绿 / 湖蓝 / 玫瑰 / 琥珀 / 紫罗兰 / 石墨），保存在 DataStore，通过 `LocalMusicThemeColors` 下发到所有页面；关闭了 Android 12 动态取色。

---

## 6. 后端接口清单

所有接口返回 `ApiResponse { code, message, data }`，`code == 200` 视为成功；分页数据为 `PageResponse { records, total, size, current, pages }`。

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `login` | JSON `{email, password}` → `{accessToken, refreshToken}` |
| POST | `register` | 表单 `email, auCode, password` |
| POST | `sendCode` | 表单 `email`，发送邮箱验证码 |
| POST | `resetPassword` | 表单 `email, auCode, password` |
| POST | `refreshToken` | JSON `{refreshToken}` → 新的 access token |
| GET | `userProfile` | 获取个人资料 |
| PUT | `user/profile` | 更新个人资料（JSON） |
| POST | `user/avatar` | 上传头像（multipart，字段名 `file`） |
| GET | `songs/getHotSongs` | 热门歌曲，`pageNum, pageSize` |
| GET | `songs/favorites` | 喜欢的歌曲，分页 |
| GET | `songs/search` | 搜索，`keyword, pageNum, pageSize` |
| POST / DELETE | `songs/{songId}/favorite` | 收藏 / 取消收藏 |
| POST | `songs/{songId}/play` | 歌曲播放次数 +1 |
| GET | `userStat/get` | 用户播放数、收藏数 |
| POST | `userStat/increase` | 用户播放数 +1 |
| GET | `notification/latest` | 最新应用公告 |
| GET | `demo` | 测试接口（未使用） |

---

## 7. 本地存储一览

| 类型 | 名称 / 位置 | 内容 |
|---|---|---|
| SharedPreferences | `token_prefs` | access_token、refresh_token |
| Room | `song_database` | 见 5.3 |
| DataStore | `player_state_prefs` | 播放队列快照、当前下标、进度、播放模式 |
| DataStore | `profile_prefs` | 个人资料（含旧 Int 型 userId → Long 的兼容） |
| DataStore | `user_stat_prefs` | 播放数、收藏数 |
| DataStore | `theme_prefs` | 主题预设 |
| DataStore | `search_history_prefs` | 最近搜索点击的歌曲 ID |
| DataStore | `app_notification_prefs` | 上次自动弹出的公告 ID |
| 文件 | `cache/artwork/` | 封面缓存（通过 FileProvider 共享给通知栏） |
| 文件 | `cache/cover_*.jpg` | 外部打开音频的内嵌封面 |
| 文件 | `files/downloads/` | 下载的音频、封面、歌词 |

---

## 8. 构建与运行

1. 用 Android Studio 打开项目根目录（JDK 11+、Android SDK 36）。
2. 后端地址在 `app/build.gradle.kts` 的 `BASE_URL`，当前为 `http://106.12.119.143/api/`（README 里 `:8080` 的写法只是示例，以 gradle 配置为准）。
3. 编译调试包：

```bash
./gradlew :app:assembleDebug
```

4. 正式包用 `./gradlew :app:assembleRelease`，产物输出到 `app/release/`（目前未开启混淆）。

---

## 9. 开发历程

| 时间 | 里程碑 |
|---|---|
| 2025-09 | 项目初始化；网络请求、注册、双 Token + 自动刷新；主页导航栏 |
| 2025-10 ~ 11 | 播放器 UI（迷你 / 全屏）、进度条；接入 ExoPlayer；理清"外部打开 / 本地 / 网络"三种播放来源；Splash；首页静态页 |
| 2026-06-02 | 列表架构改为 Retrofit → Room → UI，解决多页面状态不一致；PlayerSheet 重点改进 |
| 2026-06-03 ~ 04 | 歌词面板；DataStore 持久化与主题色 |
| 2026-06-08 | 通知栏 + 后台 Service（MediaSession 架构） |
| 2026-06-11 ~ 14 | 搜索、播放状态存储、加载更多、下载管理，"完成前期开发" |
| 2026-06-15 ~ 20 | 修复下载歌曲断网无法播放；PlayerSheet 改为按比例布局；公告支持图片；版本 1.0.2 |

---

## 10. 当前完成度

### 已完成

- 登录 / 注册 / 找回密码，双 Token 自动刷新，会话过期自动回登录页
- 热门、喜欢、搜索的分页加载与 Room 缓存
- 收藏 / 取消收藏（乐观更新，多页面同步）
- 后台播放 + 系统媒体通知（含封面）
- 四种播放模式、可拖拽排序的播放队列、分页列表跨页上一首 / 下一首
- 歌词解析、高亮、自动滚动
- 播放状态持久化与恢复
- 下载、离线播放、下载管理
- 外部音频文件打开播放
- 个人资料、头像裁剪上传、播放 / 收藏统计
- 6 套主题色、应用公告弹窗

### 占位 / 未完成

- **定时睡眠、分享歌曲**：菜单项已有，点击事件为空（`ui/mainPage/audioPlayer/PlayerSheet.kt:1002`、`:1013`）
- **推荐歌单**：首页显示"敬请期待"；`SongListKey.Playlist` 对应接口未实现
- **清空最近播放**：设置页该项 `onClick = { }`（`ui/mainPage/profile/setting/ProfileSettingScreen.kt:125`）；`RecentMusicViewModel.clearRecentPlay()`、`SearchViewModel.clearSearchHistory()` 已实现但没有 UI 入口
- **深色模式**：Material 配色有暗色分支，但自定义的 `MusicThemeColors` 只有浅色值，实际仍是浅色界面
- **测试**：只有模板测试用例

### 遗留 / 未使用代码（可考虑清理）

- `domain/player/MusicPlayerManager.kt`：旧播放器封装，已被 PlaybackService + MediaControllerManager 取代
- `ui/mainPage/audioPlayer/FullScreenPlayer.kt`、`MiniPlayer.kt`：已被 PlayerSheet 取代
- `ui/component/MusicProgressBar.kt`（注释已说明不用）、`BottomNavigationBar.kt`（已换成 CustomBottomBar）、`SongSelectItem.kt`
- `ui/appNaviagtion/NavigationViewModel.kt`；空的 `MainPageViewModel` 及 `MainPageRepository` / `MainPageApi`（`GET demo`）
- `TokenApi.getRefreshed()`、空函数 `ProfileRemoteMapper.toUserProfile()`、`PageResult` 与 `PageResponse.map()`
- `ProfileRoute.PROFILE_THEME`、`PROFILE_LOGOUT` 两个常量
- `LocalAudioMetaDataReader.buildMediaUrl()` 与 `MediaUrlUtils.buildMediaUrl()` 重复

### 未提交的改动

- `playback/PlaybackService.kt`：新增 `onPlayerError` 日志
- `ui/mainPage/MainPage.kt`：一处空格格式调整
- 重新打包的 `app-release.apk` 与 baseline profile

---

## 11. 发现的问题与改进建议

> 以下均来自代码阅读，未做修改，也未运行验证。标注"需确认"的条目取决于后端的实际行为。

### 安全

1. **明文 HTTP**：`BASE_URL` 是 `http://`，`res/xml/network_security_config.xml` 又全局允许明文流量，登录密码和 Token 都以明文传输。建议后端启用 HTTPS。
2. **正式包也打印完整报文**：`data/remote/di/NetworkModule.kt:40` 的 `HttpLoggingInterceptor.Level.BODY` 不区分构建类型，登录请求体（含密码）和刷新接口返回的 Token 会出现在 Logcat。建议仅在 `BuildConfig.DEBUG` 时开启。
3. **测试账号残留**：`ui/login/LoginViewModel.kt:58`，用户名 `root1` + 密码 `1` 会跳过接口直接进入主页（拿不到 token，之后的请求会 401 被踢回登录页）。属于开发期代码，发版前应删除。
4. `allowBackup="true"` 且备份规则是空模板，`token_prefs` 可能随系统备份被导出。

### 逻辑问题

5. **分页播放的位置不同步**：`playbackListPosition` 只在点击列表歌曲和手动上 / 下一首时更新。ExoPlayer 自动切到下一首（`ui/mainPage/audioPlayer/PlayerViewModel.kt:614` `handleControllerMediaItemTransition`）和在队列面板点歌（同文件 `:624` `playQueueIndex`）都不会更新它。之后再按"下一首"会按旧位置计算，常见表现是**重新播放当前这首**。另外，分页来源下 ExoPlayer 自动切歌仍按列表原顺序，随机模式只对手动切歌生效。
6. **同一列表混用两种 pageSize**：列表页默认每页 5 条（`data/repository/SongRepository.kt:21`、`:26`），播放器跨页取歌用 20 条（`PlayerViewModel.kt:117`），两者共用 `song_list_meta`。播放器补页后 meta 的 current / size 被改写，首页之后的 loadMore 会重复请求已有数据，看起来像"加载不出新歌"。
7. **退出登录清理不完整**：`data/session/SessionManager.kt:33` 的 `logout()` 只清 token。个人资料、统计数据、Room 中的收藏状态、播放队列快照、搜索历史、最近播放都会保留（`clearProfile / clearUserStat / clearSnapshot` 从未被调用），换账号登录会看到上一个账号的数据。
8. **关于页版本号显示错误**：`ui/mainPage/profile/about/AboutMusicScreen.kt:221` 写的是 `R.string.version.toString()`，界面上显示的是资源 ID 数字，应改为 `stringResource(R.string.version)`。另外 `strings.xml` 中是"版本1.0.1"，与 versionName 1.0.2 不一致。
9. **起播要等封面下载完**：`domain/player/MediaControllerManager.kt:174` 等处先下载封面再 `setMediaItem`，而 media 客户端 `readTimeout = 0`（不超时，`NetworkModule.kt:121`）。封面服务慢时起播会明显变慢。可以先播放，封面就绪后再 `replaceMediaItem`（`ensureArtworkForIndex` 已有类似逻辑）。
10. **（需确认）Token 过期的判定方式**：OkHttp 的 Authenticator 只在 HTTP 状态码 401 时触发（`data/remote/interceptor/TokenAuthenticator.kt:20` 的注释写的是 401 或 403）。如果后端用 403，或用 HTTP 200 + body 里 `code=401` 表示过期，就不会自动刷新。
11. **（需确认）同时修改头像和昵称**：`ui/mainPage/profile/account/ProfileAccountViewModel.kt:122-129` 上传头像后立刻从 `_uiState` 读取 `avatarUrl` 组装资料更新请求。此时新头像可能还没从 DataStore 回流到 UI 状态，导致把旧头像 URL 发给 `user/profile`；如果后端会用这个字段覆盖头像，头像会被改回去。直接使用 `avatarResult.data.avatarUrl` 更稳妥。

### 工程化

12. **APK 偏大**：release 未开启 R8（`isMinifyEnabled = false`），APK 约 54MB，`material-icons-extended` 很可能是主要来源。开启混淆压缩通常能大幅缩小体积，但需要为 Gson 反射用到的 DTO 补充 keep 规则。
13. **构建产物进了 git**：`app/release/` 下的 APK 和 baseline profile 被提交，`.git` 目录已有 107MB。建议加入 `.gitignore`，改用 Release 附件分发。
14. **依赖版本不统一**：`converter-scalars` 2.11.0 搭配 Retrofit 2.9.0；`androidx.compose.ui:ui` 单独指定 1.9.4，覆盖了 BOM 2024.09.00；`room-compiler` 的 ksp 声明了两次；`libs.androidx.runtime` 在 toml 中声明但未使用。
15. **数据库迁移**：Room 为 version 1，没有 Migration，也没有配置 schema 导出目录。以后修改表结构前需要先补迁移，否则老用户升级会崩溃。
16. **小问题**：注入注解混用 `javax.inject` 与 `jakarta.inject`；包名 `appNaviagtion`、文件名 `ProfileAccoutScreen.kt` 有拼写错误。
