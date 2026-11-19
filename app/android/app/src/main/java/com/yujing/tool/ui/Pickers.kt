package com.yujing.tool.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import java.time.LocalDate
import java.time.LocalTime

fun showDatePicker(
    context: Context,
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onPick(LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}

fun showTimePicker(
    context: Context,
    initial: LocalTime,
    onPick: (LocalTime) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPick(LocalTime.of(hour, minute)) },
        initial.hour,
        initial.minute,
        true,
    ).show()
}
