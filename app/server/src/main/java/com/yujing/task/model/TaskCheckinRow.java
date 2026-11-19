package com.yujing.task.model;

import java.time.LocalDate;

public class TaskCheckinRow {

    private Long taskId;
    private LocalDate checkinDate;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public LocalDate getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(LocalDate checkinDate) {
        this.checkinDate = checkinDate;
    }
}
