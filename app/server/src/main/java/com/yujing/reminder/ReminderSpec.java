package com.yujing.reminder;

import java.time.OffsetDateTime;

/**
 * A reminder the source module wants to exist. The dedupe key identifies the
 * same occurrence across regenerations (plan 4.2: no duplicate per day).
 */
public record ReminderSpec(String dedupeKey, String title, OffsetDateTime triggerAt, int advanceMinutes) {
}
