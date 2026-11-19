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
import com.yujing.tool.data.BirthdayItem
import com.yujing.tool.data.Formats
import java.time.LocalDate

@Composable
fun BirthdaysScreen(viewModel: AppViewModel) {
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (viewModel.birthdays.isEmpty()) {
            EmptyHint("还没有生日或纪念日。\n支持公历与农历，到点自动生成提醒。")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(viewModel.birthdays, key = { it.id }) { item ->
                    BirthdayCard(viewModel, item)
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "新增生日")
        }
    }

    if (showAdd) {
        AddBirthdayDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, category, calendarType, solarDate, month, day, leap, relationTag, advanceDays, note ->
                viewModel.addBirthday(
                    name, category, calendarType, solarDate, month, day, leap,
                    relationTag, advanceDays, note,
                )
                showAdd = false
            },
        )
    }
}

@Composable
private fun BirthdayCard(viewModel: AppViewModel, item: BirthdayItem) {
    val calendarText = if (item.calendarType == "lunar") {
        "农历 ${item.lunarMonth ?: 0} 月 ${item.lunarDay ?: 0} 日"
    } else {
        "公历 ${Formats.date(item.solarDate)}"
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
                    item.name + if (!item.relationTag.isNullOrBlank()) "（${item.relationTag}）" else "",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "$calendarText · ${if (item.category == "anniversary") "纪念日" else "生日"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "下次 ${Formats.date(item.nextDate)}，还有 ${item.daysUntil} 天",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    if (item.advanceDays > 0) "提前 ${item.advanceDays} 天提醒" else "当天提醒",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.leapAdjusted) {
                    Text(
                        "按 02-28 提醒",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (!item.note.isNullOrBlank()) {
                    Text(item.note, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { viewModel.deleteBirthday(item) }, enabled = !viewModel.readOnly) {
                Icon(Icons.Filled.Delete, contentDescription = "删除")
            }
        }
    }
}

@Composable
private fun AddBirthdayDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        String, String, String, String?, Int?, Int?, Boolean, String?, Int, String?,
    ) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("birthday") }
    var calendarType by remember { mutableStateOf("solar") }
    var solarDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var lunarMonth by remember { mutableStateOf("1") }
    var lunarDay by remember { mutableStateOf("1") }
    var lunarLeap by remember { mutableStateOf(false) }
    var relationTag by remember { mutableStateOf("") }
    var advanceDays by remember { mutableStateOf("1") }
    var note by remember { mutableStateOf("") }

    FormDialog(
        title = "新增生日/纪念日",
        confirmEnabled = name.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = {
            onConfirm(
                name,
                category,
                calendarType,
                if (calendarType == "solar") solarDate else null,
                lunarMonth.toIntOrNull(),
                lunarDay.toIntOrNull(),
                lunarLeap,
                relationTag.ifBlank { null },
                advanceDays.toIntOrNull()?.coerceIn(0, 30) ?: 0,
                note.ifBlank { null },
            )
        },
    ) {
        FormField(name, { name = it }, "名称")
        Text("类别", style = MaterialTheme.typography.labelLarge)
        OptionChips(
            options = listOf("birthday" to "生日", "anniversary" to "纪念日"),
            selected = category,
            onSelect = { category = it },
        )
        Text("历法", style = MaterialTheme.typography.labelLarge)
        OptionChips(
            options = listOf("solar" to "公历", "lunar" to "农历"),
            selected = calendarType,
            onSelect = { calendarType = it },
        )
        if (calendarType == "solar") {
            OutlinedButton(onClick = {
                showDatePicker(context, LocalDate.parse(solarDate)) { picked ->
                    solarDate = picked.toString()
                }
            }) {
                Text("公历日期 $solarDate")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(
                    lunarMonth,
                    { lunarMonth = it.filter(Char::isDigit) },
                    "农历月",
                    Modifier.weight(1f),
                )
                FormField(
                    lunarDay,
                    { lunarDay = it.filter(Char::isDigit) },
                    "农历日",
                    Modifier.weight(1f),
                )
            }
            OptionChips(
                options = listOf("false" to "非闰月", "true" to "闰月"),
                selected = lunarLeap.toString(),
                onSelect = { lunarLeap = it.toBoolean() },
            )
        }
        FormField(
            advanceDays,
            { advanceDays = it.filter(Char::isDigit) },
            "提前提醒天数（0–30）",
        )
        FormField(relationTag, { relationTag = it }, "关系标签（可选）")
        FormField(note, { note = it }, "备注（可选）")
        Text(
            "提醒默认在当天 09:00 触发，提前提醒另算一条。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
