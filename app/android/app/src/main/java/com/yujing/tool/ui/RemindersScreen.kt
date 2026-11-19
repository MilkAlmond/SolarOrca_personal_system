package com.yujing.tool.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yujing.tool.data.Formats
import com.yujing.tool.data.ReminderItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

@Composable
fun RemindersScreen(viewModel: AppViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    val globalEnabled = viewModel.settings?.remindersEnabled ?: true

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("提醒总开关", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (globalEnabled) "关闭后到点不会弹出任何提醒" else "当前所有提醒已暂停",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = globalEnabled,
                        onCheckedChange = { viewModel.setRemindersEnabled(it) },
                        enabled = !viewModel.readOnly,
                    )
                }
            }
            if (viewModel.reminders.isEmpty()) {
                EmptyHint("没有提醒。\n生日、课程与任务会自动生成提醒，也可以手动新建。")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(viewModel.reminders, key = { it.id }) { item ->
                        ReminderCard(viewModel, item)
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新建提醒")
        }
    }

    if (showAdd) {
        AddReminderDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, triggerAt ->
                viewModel.addReminder(title, triggerAt)
                showAdd = false
            },
        )
    }
}

@Composable
private fun ReminderCard(viewModel: AppViewModel, item: ReminderItem) {
    val source = when (item.source) {
        "birthday" -> "生日"
        "course" -> "课程"
        "task" -> "任务"
        else -> "手动"
    }
    val status = when (item.deliveryStatus) {
        "sent" -> "已送达"
        "failed" -> "发送失败"
        else -> "待发送"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    Formats.dateTime(item.triggerAt),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "$source · $status",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.deliveryStatus == "failed") {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Switch(
                checked = item.enabled,
                onCheckedChange = { viewModel.setReminderEnabled(item, it) },
                enabled = !viewModel.readOnly,
            )
            IconButton(onClick = { viewModel.deleteReminder(item) }, enabled = !viewModel.readOnly) {
                Icon(Icons.Filled.Delete, contentDescription = "删除提醒")
            }
        }
    }
}

@Composable
private fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, OffsetDateTime) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = "新建提醒",
        confirmEnabled = title.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = {
            val local = LocalDateTime.of(date, time)
            val trigger = OffsetDateTime.of(local, ZoneId.systemDefault().rules.getOffset(local))
            if (!trigger.isAfter(OffsetDateTime.now())) {
                error = "提醒时间必须晚于现在"
            } else {
                onConfirm(title, trigger)
            }
        },
    ) {
        FormField(title, { title = it }, "提醒标题")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                showDatePicker(context, date) { date = it }
            }) { Text("日期 $date") }
            OutlinedButton(onClick = {
                showTimePicker(context, time) { time = it }
            }) { Text("时间 ${"%02d:%02d".format(time.hour, time.minute)}") }
        }
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
