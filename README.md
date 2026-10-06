<!--suppress ALL -->

<p align="center">
  <img src="resources/logo.svg" width="100" alt="词幕 Logo"/>
</p>

<h1 align="center">词幕 · BaiCha Fork</h1>

<p align="center">
  <b>基于 Xposed 框架的 Android 状态栏歌词增强工具（完全由AI修改制作的个人分支）</b>
</p>

<p align="center">
  <a href="https://github.com/Baicha0108/lyricon/releases"><img src="https://img.shields.io/github/v/release/Baicha0108/lyricon?style=flat&color=blue" alt="Version"></a>
  <a href="https://github.com/Baicha0108/lyricon/releases"><img src="https://img.shields.io/github/downloads/Baicha0108/lyricon/total?style=flat&color=orange" alt="Downloads"></a>
  <a href="https://github.com/Baicha0108/lyricon/commits"><img src="https://img.shields.io/github/last-commit/Baicha0108/lyricon?style=flat" alt="Last Commit"></a>
  <a href="README-EN.md"><img src="https://img.shields.io/badge/Document-English-red.svg" alt="EN"></a>
</p>

<p align="center">
  <img src="resources/z.gif" alt="展示动画" width="539"/>
</p>

---

## ⚠ 关于本分支

本仓库是 [kifranei/lyricon](https://github.com/kifranei/lyricon) 的个人分支（原始项目为 [Lyricon](https://github.com/tomakino/Lyricon)），在保留状态栏歌词、歌词源插件、样式配置等全部能力的基础上，继续做个人向的功能补充与修复。

- **追随上游更新**：上游已原生提供的功能，以跟随上游实现为准。
- **应用包名**：`io.github.baicha.lyricon.fork`，与上游包名不同，不会覆盖安装原版。

> [!IMPORTANT]
> 如果你之前装的是旧包名 `io.github.kifranei.lyricon.fork`（1.0.40-rc2 及更早），升级前**必须先卸载旧版**：两个包名会同时存在，导致两套模块同时作用于状态栏。卸载后原有设置不会保留，需要重新配置。

---

## ✨ 功能特性

### 词幕本体能力

- 🎤 **歌词展示** — 逐字歌词、翻译显示、对唱模式。
- 🧩 **模块化设计** — 独立插件系统，可扩展不同播放器的歌词源。
- 🎨 **视觉自定义** — 字体样式、Logo 显示、坐标偏移、动画效果均可调。

### 继承自上游增强

- 🎚️ **提供者独立歌词控制** — 各插件可分别禁用全部、主行或副行歌词，并单独调整毫秒级同步偏移。详见 [提供者设置](docs/zh-cn/app/providers.md)。
- 📊 **听歌历史与海报墙** — 默认关闭；开启后在本机记录传到词幕的歌曲，按实际收听时长统计歌曲、歌手与专辑榜单，支持时间/播放器筛选及 PNG 海报墙保存和分享。详见 [听歌历史说明](docs/zh-cn/app/listening-history.md)。
- 🏝️ **小米超级岛（HyperOS 灵动岛）联动** — 显示歌词时自动隐藏灵动岛，歌词消失后无缝恢复；也可改为"岛出现时自动缩短歌词宽度"。
- 🌈 **彩虹歌词** — 内置一键彩虹渐变配色（亮 / 暗两套），无需手动配色，也可自定义覆盖。
- ✨ **拉长音发光** — 长音节高亮的呼吸发光效果，支持 HDR 增亮与彩虹渐变。
- 🔆 **HDR 高亮** — 在支持 HDR / 广色域的设备上让当前歌词高亮突破 SDR 亮度（可选，默认关闭）。
- 💧 **液态玻璃底栏** — App 主界面支持停靠底栏（背景高斯模糊）与液态玻璃悬浮底栏两种形态。
- 🪞 **全新关于页** — 着色器动态背景、磨砂玻璃卡片。
- 🧊 **libxposed API 101 / 102** — 适配 LSPosed 1.0.2。

### 本分支新增

- 📊 **状态栏播放进度条** — 状态栏歌词下方（可切换为上方）显示极细播放进度条，支持开关、高度、上下偏移、位置与颜色模式（跟随状态栏单色 / 彩虹渐变）；进度时长优先取系统媒体会话数据，避免第三方歌词源时长缺失或偏短导致进度异常。
- 🧊 **液态玻璃开关** — 悬浮底栏的液态玻璃效果可单独开关（Android 13 及以上），关闭后底栏退化为不透明药丸样式。
- ↔️ **标签页平滑翻页** — 主界面三个标签页支持点击底栏平滑切换，也支持左右滑动翻页。

---

## 🚀 快速上手

### 📋 环境要求

- **系统版本**：Android 10 (API 29) 及以上。
- **前置条件**：设备已 **Root**，并安装支持 **libxposed API 101 / 102** 的 **LSPosed**（如 LSPosed 1.0.2）或兼容 Xposed 框架。

> [!TIP]
> 建议使用明确支持 API 102 的框架版本（API 101 仍受支持）。不建议临时 Root，Zygote 进程脆弱可能引发未知问题。

### ⚙️ 安装与配置

1. **下载主体应用**：从 [Releases](https://github.com/Baicha0108/lyricon/releases) 下载并安装。
2. **激活模块**：在 LSPosed 中启用「词幕」模块（若同时装有其它词幕分支，请认准包名 `io.github.baicha.lyricon.fork`），勾选 **系统界面 (`com.android.systemui`)** 作用域；小米设备如需超级岛联动，请一并勾选 **`miui.systemui.plugin`**。
3. **重启生效**：重启系统界面完成 Hook 注入。
4. **安装插件**：根据播放器在 [LyricProvider](https://github.com/tomakino/LyricProvider) 下载对应插件。
5. **参数调节**：进入词幕，按屏幕情况调整位置锚点、宽度与视觉样式。为避免歌词与时间重叠，`clock` 默认在歌词显示时隐藏；若需同时显示，请把它的视图规则设为"默认"。
6. **运行测试**：播放音乐，检查状态栏显示。

---

## 🧩 生态与支持

| 类别       | 资源链接                                                          | 说明             |
|:---------|:--------------------------------------------------------------|:---------------|
| **插件库**  | [LyricProvider 仓库](https://github.com/tomakino/LyricProvider) | 主流音乐平台适配插件     |
| **开发文档** | [文档中心](https://tomakino.github.io/lyricon/)                   | App 与 Lyric 文档 |

### 💡 已原生适配的应用

- [**光锥音乐**](https://coneplayer.trantor.ink/)
- **Flamingo**
- [**BBPlayer**](https://bbplayer.roitium.com/)
- **MobiMusic**
- [**Kanade**](https://github.com/rcmiku/Kanade)
- **Sollin Player**
- [**QZ Music**](https://github.com/lqtmcstudio/QZMusic)
- [**棉花音乐**](https://github.com/pure-music/PureMusic)
- [**Smart Music Next**](https://qun.qq.com/universal-share/share?ac=1&authKey=k1hftnugk%2Bx5FZnOePE2RTS%2ByBftX2E87Trhz59sfxtVtvC3nw1MXnlxycVUIPZw&busi_data=eyJncm91cENvZGUiOiIzMzA0NzM2OTYiLCJ0b2tlbiI6IlB0NWpkSW0zWTA0UXBCTHFFdjZ0SDBsN014aUVnTitxMllFUnlMV0JpdTJEem1sdDBvRWZEM2p0RXJGVUFpZTgiLCJ1aW4iOiIyOTIwNTMzMzczIn0%3D&data=388N05tm4gkrgDLeoysN-LIYOHsCk5mUfrcBBVE9UW3WyoWG_DxkLZqDttvrptZWN5VOQWvYBwZ7d3MgKUDmTg&svctype=4&tempid=h5_group_info)
- [**LunaBeat**](https://github.com/2755337087/LunaBeat)
- [**Halcyon**](https://github.com/Kifranei/Halcyon)
- [**NeriPlayer**](https://github.com/cwuom/NeriPlayer)
- [**棱镜音乐**](https://github.com/Ryderwe/PrismMusic-Release)

#### 已适配了但没有你的播放器？请[提交 issue](https://github.com/Baicha0108/lyricon/issues)。

---

## 🙏 致谢

- [Lyricon](https://github.com/tomakino/lyricon) — 原始项目
- [kifranei/lyricon](https://github.com/kifranei/lyricon) — 本分支基于的上游项目
- [Halcyon](https://github.com/Kifranei/Halcyon) — 关于页设计
- [天道酬勤☆劉先生](https://www.coolapk.com/u/336057) — 状态栏歌词进度条来源

---

## 👥 贡献者

[![Contributors](https://contrib.rocks/image?repo=Baicha0108/lyricon)](https://github.com/Baicha0108/lyricon/graphs/contributors)

---

## ⭐ Star History

<p align="center">
  <img src="https://count.getloli.com/get/@baicha_lyricon?theme=moebooru" alt="Visitor Count" />
</p>
