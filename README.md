PS template-mod-template-1.21.11> git push -u origin main --force
Enumerating objects: 58, done.
Counting objects: 100% (58/58), done.
Delta compression using up to 32 threads
Compressing objects: 100% (47/47), done.
Writing objects: 100% (58/58), 77.84 KiB | 5.56 MiB/s, done.
Total 58 (delta 1), reused 0 (delta 0), pack-reused 0 (from 0)
remote: Resolving deltas: 100% (1/1), done.
To https://github.com/UnderTheCity/jukebox-promax.git
 + 72a9e74...cd60045 main -> main (forced update)
branch 'main' set up to track 'origin/main'.
PS template-mod-template-1.21.11> 


# Jukebox Global Sound（唱片机全局音效 + 自定义音乐/歌单）

Fabric 1.21.11 模组。Mojang 官方映射、Java 21、Fabric API 0.141.6+1.21.11。
兼容 Fabric Loader ≥ 0.17.3。

## 功能

### 1. 唱片机声音全局化（无视距离 + 双耳等响）
无论是否开启「立体音效（方向性音频）」，唱片机播放时左右耳音量始终相等（声音固定在听者正中）。
且**不受距离衰减**——只要区块加载，任何位置都能正常收听。

### 2. 自定义音乐目录（版本文件夹级，自动创建）
音乐放在（首次启动自动创建，无需手动建）：
```
<minecraft>/versions/<你的版本>/jukebox/
```
（实际由启动器指定的 gameDirectory/jukebox 决定。）

### 3. 铁砧重命名 → 单曲 / 歌单
给原版唱片铁砧重命名为 `X` 后放进唱片机，按下面顺序匹配：

| 情况 | 行为 |
|---|---|
| `jukebox/X/` 是含 .ogg 的文件夹 | 播放整个文件夹（歌单） |
| `jukebox/X.ogg` 存在 | 播放该单曲 |
| 都没有 | 播放原版音乐 |

### 4. 歌单标签（重命名名 X 后接空格标签）
标准格式：`X 模式 [模式] [定时]`（标签可组合，顺序不限，定时只能一个且放最后）。

| 标签 | 含义 |
|---|---|
| `顺序` / `列表` | 按文件名顺序播放（默认） |
| `随机` | 随机顺序播放 |
| `单次` | 播完一遍即止（默认循环） |
| 定时，如 `1h30m`、`90s`、`5m` | 到时后播完正在播放的音乐即停止 |
| 音量，如 `[10%]` | 整体音量降到对应百分比（0~100） |

例：
- `我的歌单` → 顺序 + 循环
- `我的歌单 随机` → 随机 + 循环
- `我的歌单 随机 单次 30m` → 随机 + 播一遍 + 30 分钟到点播完即止
- `我的歌单 5h38m` → 顺序 + 循环 + 5 小时 38 分定时
- `我的歌单 单次 1h [10%]` → 单次 + 定时 1 小时 + 整体音量降到 10%（歌单/单曲都适用）

### 5. 红石暂停开关
播放**非原版内容**（单曲/歌单）时，唱片机方块被**红石充能**（任意信号）→ 暂停播放；
取消充能 → 恢复播放（从当前曲开头重播）。红石不参与音量控制。

### 6. 多唱片机
同一时间只播放**最后触发的那个**非原版会话；新的会把旧的顶掉。

### 7. 游戏内引导
- 在铁砧给唱片改名成功后，聊天栏自动打印一次格式说明（目录、单曲/歌单、标签、示例）。
- 聊天输入 `td` 可永久关闭该提示（存配置）；输入 `/template-mod hints on` 重新开启。

## 说明
- 音乐仅识别 **.ogg**（Vorbis，建议单声道）。
- 歌单只扫描文件夹**顶层** .ogg，文件名按字典序排序。
- 自定义名不能含 `/`、`\`。
- 读取唱片名走单机集成服务端；纯联机客户端自动回退原版。

## 实现
- 需求 1：`RecordSoundGlobalizerMixin` 改写 RECORDS 声音 getter（相对听者、居中、无衰减）。
- 自定义播放：`CustomDiscJukeboxMixin` 在 1010 事件编排；
  `JukeboxSongPlayerMixin` 在服务端线程读取唱片名入 `DiscNameCache`；
  `DiscPlaybackManager` 全局单实例托管单曲/歌单（切曲、循环/单次、定时、红石暂停）；
  `DiscNameParser` 解析标签；`CustomDiscPlayer` 用 fabric-sound 自定义流播磁盘 OGG。
