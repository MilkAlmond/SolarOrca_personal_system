package com.yujing.tool.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.yujing.tool.BuildConfig
import com.yujing.tool.reminder.ReminderScheduler

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    notificationsGranted: Boolean,
    requestNotificationPermission: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val exactAllowed = remember(tick) { ReminderScheduler.canScheduleExact(context) }
    val ignoringBattery = remember(tick) {
        val power = context.getSystemService(PowerManager::class.java)
        power.isIgnoringBatteryOptimizations(context.packageName)
    }
    var serverUrl by remember { mutableStateOf(viewModel.serverUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    viewModel.session?.displayName ?: viewModel.session?.username ?: "未登录",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "账号 ${viewModel.session?.username.orEmpty()} · " +
                        if (viewModel.session?.role == "admin") "管理员" else "普通用户",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("服务器", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("服务地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.updateServerUrl(serverUrl) }) { Text("保存") }
                    OutlinedButton(onClick = { viewModel.refreshAll() }) { Text("立即同步") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("提醒权限", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (notificationsGranted) "通知权限：已允许" else "通知权限：未允许，收不到提醒",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!notificationsGranted) {
                    Button(onClick = requestNotificationPermission) { Text("允许通知") }
                }
                Text(
                    if (exactAllowed) "精确提醒：已开启" else "精确提醒：未开启，提醒可能延迟",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }) { Text("开启精确提醒") }
                }
                Text(
                    if (ignoringBattery) "后台运行：已忽略电池优化" else "后台运行：未加入电池优化白名单",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!ignoringBattery) {
                    OutlinedButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }) { Text("忽略电池优化") }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("提醒总开关", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "关闭后所有提醒暂停发送",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = viewModel.settings?.remindersEnabled ?: true,
                        onCheckedChange = { viewModel.setRemindersEnabled(it) },
                        enabled = !viewModel.readOnly,
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("版本与更新", style = MaterialTheme.typography.titleSmall)
                Text("当前版本 ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                val info = viewModel.versionInfo
                if (info == null) {
                    Text(
                        "尚未获取到服务端版本信息（管理员可在后台配置 android 版本）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "最新 ${info.latestVersion} · 最低支持 ${info.minSupportedVersion}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (!info.notes.isNullOrBlank()) {
                        Text(info.notes, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!info.downloadUrl.isNullOrBlank()) {
                        Text(
                            "下载地址：${info.downloadUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (info.readOnly) {
                        Text(
                            "当前版本低于最低支持版本，仅可查看。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                OutlinedButton(onClick = { viewModel.checkVersion() }) { Text("检查更新") }
            }
        }

        HorizontalDivider()
        TextButton(onClick = { viewModel.logout() }) {
            Text("退出登录", color = MaterialTheme.colorScheme.error)
        }
        Text(
            "卸载应用只清除本机凭据与缓存，服务器数据保留；删除数据请使用 Web 端的注销入口。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
