package com.yujing.tool.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yujing.tool.data.Prefs
import com.yujing.tool.reminder.ReminderScheduler

private data class WorkspaceTab(val label: String, val icon: ImageVector)

private val tabs = listOf(
    WorkspaceTab("今日", Icons.Filled.CheckCircle),
    WorkspaceTab("生日", Icons.Filled.Star),
    WorkspaceTab("课程", Icons.Filled.DateRange),
    WorkspaceTab("笔记", Icons.Filled.Edit),
    WorkspaceTab("提醒", Icons.Filled.Notifications),
)

@Composable
fun YujingApp(
    viewModel: AppViewModel,
    notificationsGranted: Boolean,
    requestNotificationPermission: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.message) {
        val text = viewModel.message
        if (text != null) {
            snackbar.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    if (viewModel.checkingSession) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = viewModel.session
    if (user == null) {
        LoginScreen(viewModel, snackbar)
    } else {
        Workspace(viewModel, snackbar, notificationsGranted, requestNotificationPermission)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(viewModel: AppViewModel, snackbar: SnackbarHostState) {
    var serverUrl by rememberSaveable { mutableStateOf(viewModel.serverUrl) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var registerMode by rememberSaveable { mutableStateOf(false) }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("羽鲸个人工具", style = MaterialTheme.typography.headlineMedium)
            Text(
                "生日、课程、任务、笔记与提醒，在手机和电脑之间保持同步。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("服务器地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "模拟器填 ${Prefs.DEFAULT_SERVER}，真机填电脑的局域网地址。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("账号名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (registerMode) {
                Text(
                    "账号名 3–20 位字母、数字或下划线；密码 8–64 位。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = {
                    if (registerMode) {
                        viewModel.register(username, password, serverUrl)
                    } else {
                        viewModel.login(username, password, serverUrl)
                    }
                },
                enabled = !viewModel.busy && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (viewModel.busy) "请稍候…" else if (registerMode) "注册并登录" else "登录")
            }
            TextButton(onClick = { registerMode = !registerMode }) {
                Text(if (registerMode) "已有账号？返回登录" else "没有账号？立即注册")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Workspace(
    viewModel: AppViewModel,
    snackbar: SnackbarHostState,
    notificationsGranted: Boolean,
    requestNotificationPermission: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSettings) { showSettings = false }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (showSettings) "设置" else tabs[selectedTab].label)
                },
                navigationIcon = {
                    if (showSettings) {
                        IconButton(onClick = { showSettings = false }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (!showSettings) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "设置")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!showSettings) {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (viewModel.readOnly) {
                ReadOnlyBanner()
            }
            if (showSettings) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    SettingsScreen(viewModel, notificationsGranted, requestNotificationPermission)
                }
            } else {
                if (selectedTab == 0) {
                    PermissionCard(
                        viewModel = viewModel,
                        notificationsGranted = notificationsGranted,
                        requestNotificationPermission = requestNotificationPermission,
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> TasksScreen(viewModel)
                        1 -> BirthdaysScreen(viewModel)
                        2 -> CoursesScreen(viewModel)
                        3 -> NotesScreen(viewModel)
                        else -> RemindersScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyBanner() {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Text(
            "当前客户端版本过低，只能查看，请安装新版本后再修改数据。",
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun PermissionCard(
    viewModel: AppViewModel,
    notificationsGranted: Boolean,
    requestNotificationPermission: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableStateOf(0) }
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
    if (viewModel.permissionCardDismissed || (notificationsGranted && exactAllowed)) {
        return
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("让提醒准时到达", style = MaterialTheme.typography.titleSmall)
            Text(
                "允许通知后才能在到点时收到提醒；开启\u201c闹钟与提醒\u201d可避免延迟。两项都可跳过。",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!notificationsGranted) {
                    Button(onClick = requestNotificationPermission) { Text("允许通知") }
                }
                if (!exactAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }) { Text("精确提醒") }
                }
                TextButton(onClick = { viewModel.dismissPermissionCard() }) { Text("跳过") }
            }
        }
    }
}
