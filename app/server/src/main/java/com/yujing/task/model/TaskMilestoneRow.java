package com.yujing.task.model;

public class TaskMilestoneRow {

    private Long taskId;
    private Integer milestoneDays;

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
}
