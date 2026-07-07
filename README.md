# 中国象棋 Chinese Chess

一款 Android 中国象棋应用，支持人机对战、本地双人对战和残局挑战。

## 功能特性

- **人机对战** - 4个难度等级的AI对手，基于Alpha-Beta剪枝算法
- **双人对战** - 同一台设备上轮流下棋
- **残局挑战** - 10个经典残局，从简单到大师级
- **完整规则** - 支持蹩马腿、塞象眼、炮翻山、将帅对面等所有象棋规则

## 技术栈

- Kotlin
- Jetpack Compose + Canvas 自定义绘制
- MVVM 架构
- Alpha-Beta 剪枝 AI 引擎

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
- 双人对战界面
- 残局选择列表
- 残局游戏界面

## 许可证

MIT License
