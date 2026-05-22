# Web 端源码说明

此目录是电脑浏览器端的**独立静态源码**：
- `index.html`
- `player.js`
- `style.css`

Android App 运行时实际对外提供的是 `android-app/app/src/main/assets/web/` 下的同名文件（由内置 HTTP Server 在 `:8080` 提供）。

保留此目录是为了：
1. 方便前端单独调试与代码审查；
2. 让仓库结构中能直观看到“Web 端”代码位置；
3. 后续可接入独立前端打包流程，再复制到 Android assets。
