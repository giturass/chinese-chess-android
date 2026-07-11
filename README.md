# 中国象棋 Chinese Chess

一款 Android 中国象棋应用，支持人机对战、双人对战和残局挑战。

## 功能特性

- **人机对战** - 4个难度等级的AI对手，内置 Pikafish ARM64 原生引擎并通过 UCI 协议通信
- **双人对战** - 支持同设备轮流下棋和服务器房间联机
- **残局挑战** - 10个经典残局，从简单到大师级
- **完整规则** - 支持蹩马腿、塞象眼、炮翻山、将帅对面等所有象棋规则

## 技术栈

- Kotlin
- Jetpack Compose + Canvas 自定义绘制
- MVVM 架构
- Pikafish UCI AI 引擎（Android ARM64 原生二进制 + NNUE）

## 构建

```bash
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/`

## GitHub Actions

每次推送到 main 分支会自动构建并上传 APK artifact。

## 截图

应用包含以下界面：
- 主页菜单
- 人机对战界面（含难度选择）
- 双人对战模式选择、本地对战和联机对战界面
- 残局选择列表
- 残局游戏界面

## 第三方组件

APK 内置 `official-pikafish/Pikafish` 2026-01-02 Android ARM64 二进制文件和 NNUE 文件，相关 GPL v3 与 NNUE 许可证文本随 assets 一并打包。

## 许可证

MIT License
