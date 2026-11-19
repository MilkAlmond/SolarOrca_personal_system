# 羽鲸个人工具 · Android 客户端

Kotlin + Jetpack Compose 客户端，最低支持 Android 8.0（API 26），服务端契约与 Web/桌面端一致。

## 构建环境

- Android Studio Ladybug（或更新），JDK 17–21（Studio 自带 JBR 21 可用）
- Android SDK Platform 35、Build-Tools 35.x
- Gradle 8.11.1（`gradle/wrapper/gradle-wrapper.properties` 已配置，仓库内**未包含** `gradle-wrapper.jar`）

本仓库当前机器未安装 Android SDK / ADB / Gradle，因此代码未在此环境编译或运行，验收项均为“未验证”。

## 构建与运行

1. 用 Android Studio 打开 `app/android`，等待 Gradle 同步（Studio 会自动补全 wrapper）。
2. 或在本机安装 Gradle 8.11.1 后执行：

   ```powershell
   cd app/android
   gradle assembleDebug
   ```

3. 模拟器直接使用默认服务地址 `http://10.0.2.2:8080`。
   真机需在登录页把服务地址改为电脑的局域网 IP（例如 `http://192.168.1.20:8080`），并保证手机与电脑同一网段。
4. 服务端以 HTTP 明文暴露时依赖 `android:usesCleartextTraffic="true"`；正式部署改为 HTTPS 后可移除。

## 功能范围

- 登录/注册：原生平台使用 `Authorization: Bearer <token>`，token 保存在 EncryptedSharedPreferences（不支持时降级为普通私有存储）。
- 五个模块：今日任务（打卡/撤销/新建/删除）、生日纪念（公历/农历、提前提醒天数）、课程表（每周/单次、课前提醒）、笔记（2 秒自动保存、标题搜索、版本冲突自动恢复一次）、提醒（手动新建、启停、删除、全局开关）。
- 提醒：WorkManager 每 15 分钟对账 + AlarmManager 精确闹钟；启动、登录、数据变更后重建本地计划；通知只显示标题与时间；回执失败重试一次。
- 设置：服务地址、通知/精确提醒/电池优化引导、全局提醒开关、同步、版本检查、退出登录。
- 版本策略：启动时请求 `/api/v1/app/version?platform=android&current=0.1.0`，`readOnly=true` 时客户端只读。

## 代码结构

```
app/src/main/java/com/yujing/tool/
├── MainActivity.kt          # 入口、通知权限
├── YujingApplication.kt     # Prefs 初始化、通知渠道、周期任务
├── data/                    # ApiClient、数据模型、本地存储、格式化
├── reminder/                # 调度器、闹钟接收器、WorkManager、通知
└── ui/                      # Compose 根视图、五个模块、设置、主题
```

## 已知限制

- 未提供修改密码与头像/资料编辑入口（由 Web 端完成）。
- 课程为列表视图，不提供 Web 端的并排周视图。
- 离线时只缓存登录凭据，不做离线编辑；恢复网络后按服务端版本号提交，冲突时提示并刷新。
- 未包含 Gradle wrapper jar，需 Android Studio 或本机 Gradle 生成/补全。
