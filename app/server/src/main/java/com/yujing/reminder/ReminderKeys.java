package com.yujing.reminder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Stable per-occurrence keys so regeneration never duplicates a reminder. */
public final class ReminderKeys {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HHmm");

    private ReminderKeys() {
    }

    public static String birthday(Long birthdayId, LocalDate occurrence, boolean advanceSlot) {
        return "birthday:" + birthdayId + ":" + DATE.format(occurrence) + ":" + (advanceSlot ? "advance" : "day");
    }

    public static String course(Long courseId, LocalDate occurrence, LocalTime startTime) {
        return "course:" + courseId + ":" + DATE.format(occurrence) + ":" + TIME.format(startTime);
    }

    public static String task(Long taskId, LocalDate occurrence) {
        return "task:" + taskId + ":" + DATE.format(occurrence);
    }
}
