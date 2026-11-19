package com.yujing.task.model;

import java.time.LocalDate;

public class TaskMilestoneRow {

    private Long taskId;
    private Integer milestoneDays;
    private LocalDate reachedOn;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getMilestoneDays() {
        return milestoneDays;
    }

    public void setMilestoneDays(Integer milestoneDays) {
        this.milestoneDays = milestoneDays;
    }

    public LocalDate getReachedOn() {
        return reachedOn;
    }

    public void setReachedOn(LocalDate reachedOn) {
        this.reachedOn = reachedOn;
    }
}
