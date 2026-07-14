# 中国象棋

一款 内置了 pikafish 引擎的 Android 中国象棋应用，支持人机对战、双人对战和残局挑战。

## 注意

1.首次打开应用会后台加载pikafish的nnue文件 需要打开网络 加载完毕会提示重启应用生效。
2.如果觉得内置联机服务器不够稳定，可以 https://github.com/giturass/ChineseChessOnlineServer 自部署，然后应用设置替换默认联机服务器。

## 功能特性

- **人机对战** - 4个难度等级的AI对手，内置 Pikafish ARM64 原生引擎并通过 UCI 协议通信
- **双人对战** - 支持同设备轮流下棋和服务器房间联机
- **残局挑战** - 16个经典残局，从简单到大师级

## 技术栈

- Kotlin
- MVVM 架构
- Pikafish UCI AI 引擎（Android ARM64 原生二进制 + NNUE）

## 构建

```bash
./gradlew assembleDebug
```

## GitHub Actions

每次推送到 main 分支会自动构建并上传 APK artifact

## 第三方组件

APK 内置 `official-pikafish/Pikafish` 2026-01-02 Android ARM64 二进制文件和 NNUE 文件。

## 许可证

MIT License
