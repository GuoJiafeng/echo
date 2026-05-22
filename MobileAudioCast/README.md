# MobileAudioCast

## 1. 项目简介
MobileAudioCast 用于将 Android 手机系统媒体音频实时转发到电脑浏览器播放。

## 2. 功能说明
- Android 端采集系统音频（MediaProjection + AudioPlaybackCapture）
- 局域网 HTTP 网页播放
- WebSocket 信令 + WebRTC 音频传输
- 支持开始/停止/重连

## 3. 系统要求
- 手机和电脑在同一局域网
- Android 10+
- 现代浏览器（Chrome/Edge/Firefox）

## 4. Android 版本要求
- minSdk 29
- targetSdk 35
- compileSdk 35

## 5. 构建方式
```bash
cd android-app
./gradlew assembleDebug
```

## 6. 本地运行方式
1. 安装 debug APK
2. 打开 App，允许系统录屏/投屏授权
3. 点击“开始投放系统音频”

## 7. 电脑端访问方式
浏览器访问 App 显示的地址（例：`http://192.168.x.x:8080`），点击“连接手机音频”。

## 8. GitHub Actions 说明
- `android-build.yml`：main 分支 push / PR 自动构建 debug + lint + 上传 APK
- `release.yml`：tag（如 `v1.0.0`）触发 release APK 构建并上传 GitHub Release

> 若暂无正式签名密钥，当前 release 使用默认调试签名。生产发布前请在 `app/build.gradle.kts` 配置正式 `signingConfigs`。

## 9. 已知限制
- 不需要 Root。
- 手机端必须安装 Android App。
- 电脑端只需要浏览器。
- 受 Android 系统和音源 App 限制，部分音乐 App 可能无法被捕获。
- Android 会显示录屏/投屏授权弹窗，这是系统音频捕获必要权限。
- 本项目不会保存录音文件，只做实时转发。

## 10. 常见问题
**Q: 电脑听不到声音？**
A: 确认音源 App 支持被系统捕获；确保局域网可互通；点击网页重连。

**Q: 为什么会弹录屏权限？**
A: AudioPlaybackCapture 必须依赖 MediaProjection 系统授权。
