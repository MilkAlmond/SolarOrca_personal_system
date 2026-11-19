package com.yujing.tool.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Formats {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun date(iso: String?): String = iso?.take(10) ?: "—"

    fun dateTime(iso: String?): String {
        if (iso.isNullOrBlank()) {
            return "—"
        }
        return try {
            OffsetDateTime.parse(iso)
                .atZoneSameInstant(ZoneId.systemDefault())
                .format(dateTimeFormatter)
        } catch (ex: Exception) {
            iso
        }
    }

    fun time(value: String?): String {
        if (value.isNullOrBlank()) {
            return "—"
        }
        return try {
            LocalTime.parse(value).format(timeFormatter)
        } catch (ex: Exception) {
            value.take(5)
        }
    }

    fun timeRange(start: String?, end: String?): String = "${time(start)}–${time(end)}"

    fun weekday(value: Int): String {
        val names = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        return names.getOrElse(value - 1) { "周一" }
    }

    fun today(): String = LocalDate.now().toString()

    fun plusDays(days: Long): String = LocalDate.now().plusDays(days).toString()
}
