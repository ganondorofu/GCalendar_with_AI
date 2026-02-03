package com.gcalendar.komeniki.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Event {
    private String title;
    @JsonProperty("is_all_day")
    private boolean allDay;
    @JsonProperty("start_datetime")
    private String startDatetime;
    @JsonProperty("end_datetime")
    private String endDatetime;
    private String description;

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isAllDay() {
        return allDay;
    }

    public void setAllDay(boolean allDay) {
        this.allDay = allDay;
    }

    public String getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(String startDatetime) {
        this.startDatetime = startDatetime;
    }

    public String getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(String endDatetime) {
        this.endDatetime = endDatetime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
