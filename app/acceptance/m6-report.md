# M6 里程碑报告（Android 客户端与验收材料）

- 日期：2026-11-19
- 范围：Android 客户端（Kotlin + Jetpack Compose，minSdk 26）、提醒本地化、验收脚本与记录
- 结论：**代码与验收材料已交付；Android 端未编译、未安装、未运行，所有 Android UI/通知验收项为“未验证”。** 服务端契约预验证 14/14 通过，Web 构建通过。

## 1. 交付内容

| 交付物 | 路径 | 说明 |
| --- | --- | --- |
| Android 工程 | `app/android` | Gradle Kotlin DSL、AGP 8.7.3、Kotlin 2.0.21、Compose BOM 2024.10.01 |
| 构建说明 | `app/android/README.md` | 环境要求、模拟器/真机地址、签名与权限说明 |
| 客户端代码 | `app/android/app/src/main/java/com/yujing/tool` | 登录注册、五个模块、设置、版本只读、提醒调度与通知 |
| 验收脚本 | `app/scripts/verify-android.ps1` | 环境检查、服务端契约冒烟、ADB 安装/启动（可用时） |
| 验收记录 | `app/acceptance/m6-android-acceptance.md` | 10.4 / 10.5 逐条记录、执行命令与真机步骤 |

## 2. 实现摘要

- **认证**：`X-Client-Platform: android` 登录/注册返回 `data.token`，使用 `Authorization: Bearer`；token 存 EncryptedSharedPreferences，异常时降级为普通私有存储；启动时 `GET /auth/me` 恢复会话。
- **模块**：今日任务（新建、打卡/撤销、删除）、生日纪念（公历/农历、提前天数、闰月选项）、课程（每周/单次、星期、起止时间、课前提醒）、笔记（2 秒自动保存、标题搜索、删除、冲突重载重试）、提醒（手动新建、启停、删除、总开关）。
- **提醒链路**：WorkManager 每 15 分钟对账 + 启动/登录/数据变更后重建 AlarmManager 精确闹钟；通知渠道 `reminders`；通知内容仅标题与时间；去重键 `reminderId@triggerAt`；回执 `sent/failed`，失败重试一次；开机与升级后重新同步。
- **权限**：`POST_NOTIFICATIONS`（33+ 运行时申请）、`SCHEDULE_EXACT_ALARM`（31+ 跳转授权）、`RECEIVE_BOOT_COMPLETED`、电池优化引导。
- **版本策略**：`GET /app/version?platform=android&current=0.1.0`，服务端返回 `readOnly=true` 时客户端禁用写操作并显示横幅；4009 同样进入只读。
- **数据一致性**：所有更新携带 `version`；409/4002 时提示“数据已被其他设备修改”并刷新，笔记保存冲突自动重载版本重试一次。

## 3. 验证结果

| 项目 | 结果 | 证据 |
| --- | --- | --- |
| Web 生产构建（`npm run build`） | ✅ 通过 | tsc + vite 构建成功，53 个模块 |
| Web 任务打卡（B-03 单次路径） | ✅ 通过 | 新账号 `m6web19` 创建“验收任务”，打卡后显示“撤销今日 / 连续 1 天 · 今天已打卡” |
| 服务端契约冒烟（Android 头 + Bearer） | ✅ 14/14 | `app/scripts/verify-android.ps1 -SkipAdb` 输出 |
| Android 编译（`assembleDebug`） | ⚠️未执行 | 无 Android SDK / Gradle wrapper jar / Gradle CLI |
| APK 生成与安装 | ⚠️未执行 | 无 SDK、无 adb、无模拟器 |
| Android UI 与通知验收 | ⚠️未验证 | 见 `m6-android-acceptance.md` |

环境实测：`java 25.0.4.1`；`ANDROID_HOME` 为空；`adb` 不存在；`gradle` 不存在。

## 4. 本轮发现并修复的问题

1. **任务打卡缺少日期**：服务端 `POST /tasks/{id}/checkins` 要求请求体包含 `date`，Web 端原实现发送 `{}` 导致 400。已修复 Web `checkinTask` 与 Android `checkinTask`，均按客户端本地日期提交；契约检查 API-05 通过。
2. **Android 只读与错误码**：补充 4009 只读、4005 注销冻结提示、4002 冲突刷新，避免静默覆盖。

## 5. 计划矛盾（需所有者确认）

计划 §9.6 同时规定：

- “注销后立即登出所有端，账号进入 30 天冻结期，**期间无法登录**。”（`plan.txt` 1520 行）
- “**冻结期内用原账号与密码登录，可撤销注销**并恢复全部数据。”（`plan.txt` 1521 行）

两者不可同时成立：若冻结期禁止登录，就无法通过登录撤销注销。

M4 已采用的实现：

- 登录冻结账号返回 `ACCOUNT_FROZEN`（4005），提示“账号注销冻结中，无法登录”；
- 新增公开接口 `POST /api/v1/account/deletion/cancel`（账号名 + 密码），作为冻结期撤销注销的唯一入口；
- Android 端按此实现：登录被拒绝时展示冻结提示，撤销注销需走 Web/公开接口。

请所有者确认该解释是否作为最终规则；若要求“登录即撤销”，需修改登录流程与安全校验。

## 6. 已知缺口

- Android 未提供修改密码、资料/头像编辑入口（计划由 Web 端覆盖）。
- Android 课程为列表视图，Web 的并排周视图与拖动排序仍以 Web 端为准。
- 无离线编辑缓存；断网时写操作直接提示失败，不排队重放。
- 未生成 APK 与四端安装包；签名、渠道与发布流程未执行。
- Gradle wrapper jar 未纳入仓库，首次构建依赖 Android Studio 或本机 Gradle。

## 7. 下一步

1. 在具备 Android SDK（API 35）与 JDK 17–21 的机器上打开 `app/android`，执行 `assembleDebug` 并修复编译问题。
2. 按 `m6-android-acceptance.md` 第 4 节执行真机用例，回填设备型号、Android 版本、结果与证据。
3. 优先验证 R-02（应用未运行提醒）与 R-03（断网失败/重试），二者只能在真机确认。
4. 补充修改密码入口（如需要 Android 端覆盖 A-03）与 APK 签名发布流程。
