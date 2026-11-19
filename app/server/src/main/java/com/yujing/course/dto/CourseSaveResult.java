package com.yujing.course.dto;

import java.util.List;

public record CourseSaveResult(CourseView course, List<CourseView> conflicts, String reminderNotice) {
}
