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
import com.yujing.tool.data.CourseItem
import com.yujing.tool.data.Formats
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun CoursesScreen(viewModel: AppViewModel) {
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (viewModel.courses.isEmpty()) {
            EmptyHint("还没有课程。\n新增课程后会自动生成课前提醒。")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(viewModel.courses, key = { it.id }) { item ->
                    CourseCard(viewModel, item)
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新增课程")
        }
    }

    if (showAdd) {
        AddCourseDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, teacher, location, weekday, start, end, repeatType, date, remind ->
                viewModel.addCourse(
                    name, teacher, location, weekday, start, end, repeatType, date, remind,
                )
                showAdd = false
            },
        )
    }
}

@Composable
private fun CourseCard(viewModel: AppViewModel, item: CourseItem) {
    val repeatText = if (item.repeatType == "once") {
        "单次 ${Formats.date(item.courseDate)}"
    } else {
        "每周"
    }
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${item.name}  ${Formats.timeRange(item.startTime, item.endTime)}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "${Formats.weekday(item.weekday)} · $repeatText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!item.teacher.isNullOrBlank() || !item.location.isNullOrBlank()) {
                    Text(
                        listOfNotNull(item.teacher, item.location).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    if (item.remindMinutes > 0) "课前 ${item.remindMinutes} 分钟提醒" else "不提醒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { viewModel.deleteCourse(item) }, enabled = !viewModel.readOnly) {
                Icon(Icons.Filled.Delete, contentDescription = "删除")
            }
        }
    }
}

@Composable
private fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        String, String?, String?, Int, String, String, String, String?, Int,
    ) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var weekday by remember { mutableStateOf(1) }
    var start by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var end by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var repeatType by remember { mutableStateOf("weekly") }
    var courseDate by remember { mutableStateOf(LocalDate.now()) }
    var remindMinutes by remember { mutableStateOf("15") }
    var error by remember { mutableStateOf<String?>(null) }

    FormDialog(
        title = "新增课程",
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = {
            if (!end.isAfter(start)) {
                error = "结束时间必须晚于开始时间，跨天课程请拆成两条"
            } else {
                onConfirm(
                    name,
                    teacher.ifBlank { null },
                    location.ifBlank { null },
                    weekday,
                    "%02d:%02d".format(start.hour, start.minute),
                    "%02d:%02d".format(end.hour, end.minute),
                    repeatType,
                    if (repeatType == "once") courseDate.toString() else null,
                    remindMinutes.toIntOrNull() ?: 15,
                )
            }
        },
    ) {
        FormField(name, { name = it }, "课程名称")
        FormField(teacher, { teacher = it }, "教师（可选）")
        FormField(location, { location = it }, "地点（可选）")
        Text("重复方式", style = MaterialTheme.typography.labelLarge)
        OptionChips(
            options = listOf("weekly" to "每周", "once" to "单次"),
            selected = repeatType,
            onSelect = { repeatType = it },
        )
        if (repeatType == "weekly") {
            Text("星期", style = MaterialTheme.typography.labelLarge)
            OptionChips(
                options = (1..7).map { it.toString() to Formats.weekday(it) },
                selected = weekday.toString(),
                onSelect = { weekday = it.toInt() },
            )
        } else {
            OutlinedButton(onClick = {
                showDatePicker(context, courseDate) { picked ->
                    courseDate = picked
                    weekday = picked.dayOfWeek.value
                }
            }) {
                Text("上课日期 ${courseDate}（${Formats.weekday(weekday)}）")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                showTimePicker(context, start) { start = it }
            }) { Text("开始 ${"%02d:%02d".format(start.hour, start.minute)}") }
            OutlinedButton(onClick = {
                showTimePicker(context, end) { end = it }
            }) { Text("结束 ${"%02d:%02d".format(end.hour, end.minute)}") }
        }
        Text("课前提醒", style = MaterialTheme.typography.labelLarge)
        OptionChips(
            options = listOf("0" to "不提醒", "5" to "5 分钟", "10" to "10 分钟", "15" to "15 分钟", "30" to "30 分钟"),
            selected = remindMinutes,
            onSelect = { remindMinutes = it },
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
