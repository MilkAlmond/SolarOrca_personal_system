package com.yujing.birthday.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BirthdayRow {

    private Long id;
    private Long ownerId;
    private String name;
    private String category;
    private String calendarType;
    private LocalDate solarDate;
    private Integer lunarMonth;
    private Integer lunarDay;
    private boolean lunarLeap;
    private String relationTag;
    private int advanceDays;
    private String note;
    private long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCalendarType() {
        return calendarType;
    }

    public void setCalendarType(String calendarType) {
        this.calendarType = calendarType;
    }

    public LocalDate getSolarDate() {
        return solarDate;
    }

    public void setSolarDate(LocalDate solarDate) {
        this.solarDate = solarDate;
    }

    public Integer getLunarMonth() {
        return lunarMonth;
    }

    public void setLunarMonth(Integer lunarMonth) {
        this.lunarMonth = lunarMonth;
    }

    public Integer getLunarDay() {
        return lunarDay;
    }

    public void setLunarDay(Integer lunarDay) {
        this.lunarDay = lunarDay;
    }

    public boolean isLunarLeap() {
        return lunarLeap;
    }

    public void setLunarLeap(boolean lunarLeap) {
        this.lunarLeap = lunarLeap;
    }

    public String getRelationTag() {
        return relationTag;
    }

    public void setRelationTag(String relationTag) {
        this.relationTag = relationTag;
    }

    public int getAdvanceDays() {
        return advanceDays;
    }

    public void setAdvanceDays(int advanceDays) {
        this.advanceDays = advanceDays;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
