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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.yujing.tool.data.TaskItem
import java.time.LocalTime

@Composable
fun TasksScreen(viewModel: AppViewModel) {
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (viewModel.tasks.isEmpty()) {
            EmptyHint("今天还没有任务。\n点击右下角新建每日或每周任务。")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(viewModel.tasks, key = { it.id }) { task ->
                    TaskCard(viewModel, task)
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新建任务")
        }
    }

    if (showAdd) {
        AddTaskDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, period, weeklyTarget, remindTime ->
                viewModel.addTask(name, period, weeklyTarget, remindTime)
                showAdd = false
            },
        )
    }
}

@Composable
private fun TaskCard(viewModel: AppViewModel, task: TaskItem) {
    val periodText = if (task.period == "weekly") {
        "每周 · 本周 ${task.weekProgress}/${task.weeklyTarget ?: 0}"
    } else {
        "每日 · 连续 ${task.streakDays} 天"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(task.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    periodText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!task.remindTime.isNullOrBlank()) {
                    Text(
                        "提醒 ${Formats.time(task.remindTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (task.weekCompleted) {
                    Text(
                        "本周目标已完成",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    if (task.checkedToday) {
                        viewModel.undoCheckin(task)
                    } else {
                        viewModel.checkin(task)
                    }
                },
                enabled = !viewModel.readOnly,
            ) {
                Text(if (task.checkedToday) "撤销" else "打卡")
            }
            IconButton(onClick = { viewModel.deleteTask(task) }, enabled = !viewModel.readOnly) {
                Icon(Icons.Filled.Delete, contentDescription = "删除任务")
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int?, String?) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("daily") }
    var weeklyTarget by remember { mutableStateOf("3") }
    var remindTime by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = "新建任务",
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(
                name,
                period,
                if (period == "weekly") weeklyTarget.toIntOrNull()?.coerceIn(1, 7) else null,
                remindTime,
            )
        },
    ) {
        FormField(name, { name = it }, "任务名称")
        Text("周期", style = MaterialTheme.typography.labelLarge)
        OptionChips(
            options = listOf("daily" to "每日", "weekly" to "每周"),
            selected = period,
            onSelect = { period = it },
        )
        if (period == "weekly") {
            FormField(weeklyTarget, { weeklyTarget = it.filter(Char::isDigit) }, "每周目标（1–7 次）")
        }
        OutlinedButton(onClick = {
            showTimePicker(context, LocalTime.of(9, 0)) { picked ->
                remindTime = "%02d:%02d".format(picked.hour, picked.minute)
            }
        }) {
            Text(remindTime?.let { "提醒时间 $it" } ?: "设置提醒时间（可选）")
        }
        Text(
            "开始日期为今天，之后每天/每周在今日视图打卡。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
