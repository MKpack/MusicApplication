# MusicApplication

MusicApplication 是一个基于 Kotlin 和 Jetpack Compose 开发的 Android 音乐播放应用。项目采用 MVVM + Repository 分层架构，结合 Hilt、Retrofit、OkHttp、Room、DataStore、Media3、Coil 等技术，实现登录认证、歌曲分页加载、本地缓存、喜欢歌曲、最近播放、播放队列、歌词解析和全屏播放器等功能。

## 技术栈

- Kotlin
- Jetpack Compose
- MVVM
- Hilt
- Retrofit
- OkHttp
- Room
- DataStore
- Media3 ExoPlayer
- Coil
- Kotlin Coroutines / Flow / StateFlow

## 核心功能

- 用户登录与登录状态维护
- AccessToken 自动携带与 401 Token 刷新
- 热门歌曲分页加载
- 喜欢歌曲与取消喜欢
- 最近播放记录
- 本地音频 Uri 播放
- 远程音频 URL 播放
- 播放队列管理
- 顺序播放、列表循环、单曲循环、随机播放
- 播放进度同步与拖动 seek
- 全屏播放器与迷你播放器联动
- 歌词 LRC 解析、当前歌词高亮和自动滚动
- 用户资料、头像、昵称等信息展示与更新

## 项目架构

项目整体采用分层架构：

```text
ui
  Compose 页面、组件、ViewModel

domain
  业务模型、播放器管理、通用业务类型

data
  repository
  remote
  local
```

主要职责：

- `ui`：负责页面展示、用户交互和 UI 状态收集。
- `domain`：负责业务模型定义和播放器核心管理。
- `data/remote`：负责 Retrofit API、DTO、Interceptor、Authenticator。
- `data/local`：负责 Room、DataStore、TokenStore、本地缓存。
- `data/repository`：负责协调远程数据、本地数据和业务结果封装。

## 数据层设计

歌曲模块引入 Room 作为本地缓存层，核心表包括：

```text
songs
  存储歌曲本体信息，如标题、歌手、封面、音频地址、歌词地址、喜欢状态

song_list_items
  存储歌曲和列表之间的关系，如热门列表、喜欢列表、歌单列表、搜索结果列表

song_list_meta
  存储列表分页信息，如 current、pages、total、size

recent_song_play
  存储最近播放记录，支持区分 remote 和 local_uri 来源
```

数据流设计：

```text
后端 API
  -> SongResponse
  -> SongEntity
  -> Room
  -> Flow<List<SongEntity>>
  -> Song
  -> ViewModel StateFlow
  -> Compose UI
```

通过 Room + Flow，首页、喜欢列表、最近播放、播放器等页面可以监听同一份本地数据，减少多页面状态不同步的问题。

## 网络与认证

网络层基于 Retrofit + OkHttp：

- `AuthInterceptor`：请求时自动附加 AccessToken。
- `TokenAuthenticator`：接口返回 401 时尝试刷新 Token，并重新发起请求。
- `RepositoryWorkResult`：统一封装 Repository 层成功、失败和错误信息。

登录态相关数据通过本地 TokenStore 保存，页面层不直接处理 Retrofit 异常和后端原始响应结构。

## 播放器设计

播放器基于 AndroidX Media3 ExoPlayer 封装：

- `MusicPlayerManager`：负责底层播放、暂停、恢复、seek、播放完成回调、进度同步。
- `PlayerViewModel`：负责当前歌曲、播放队列、播放模式、歌词状态和 UI 状态。
- `PlayerSheet`：负责迷你播放器、全屏播放器、歌词、队列和更多操作 UI。

播放来源通过 `MusicSource` 区分：

```kotlin
sealed class MusicSource {
    class Remote(...)
    class Local(...)
}
```

因此项目同时支持后端音乐 URL 和用户本地选择的音频 Uri。

## 歌词实现

后端返回 `lyricUrl` 后，客户端通过 OkHttp 请求 LRC 文件，并使用本地解析器解析时间轴歌词：

```text
[01:23.45] 歌词内容
```

解析后生成歌词行列表，根据播放器当前进度计算当前歌词行，并在全屏播放器中自动滚动，使当前歌词保持在中间区域。

## 页面模块

```text
login
  登录页面

mainPage/home
  发现页、热门歌曲

mainPage/profile
  个人中心、账号资料、喜欢歌曲、最近播放、下载管理、设置

mainPage/audioPlayer
  迷你播放器、全屏播放器、播放队列、歌词

component
  通用 Compose 组件
```

## 运行说明

1. 使用 Android Studio 打开项目。
2. 确认本地后端服务已启动。
3. 修改 `app/build.gradle.kts` 中的 `BASE_URL`：

```kotlin
buildConfigField("String", "BASE_URL", "\"http://你的后端地址:8080\"")
```

4. 编译运行：

```bash
./gradlew :app:assembleDebug
```

## 项目亮点

- 使用 MVVM + Repository 组织业务逻辑，降低 UI 与数据层耦合。
- 使用 Hilt 管理网络层、Repository、本地存储等依赖。
- 使用 Retrofit + OkHttp 实现 Token 自动携带与过期刷新。
- 使用 Room 设计歌曲缓存、列表关系和分页元数据，支持分页数据本地持久化。
- 使用 Flow / StateFlow 驱动 Compose UI 自动刷新。
- 使用 Media3 ExoPlayer 封装音乐播放能力，支持远程和本地音频。
- 实现播放队列、拖拽排序、多种播放模式和歌词自动滚动高亮。

## 后续计划

- 完善歌单详情与搜索模块
- 增加下载歌曲管理
- 增加本地音乐库
- 增加播放历史云同步
- 增加更完整的错误恢复和缓存过期策略
