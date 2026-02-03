package com.gcalendar.komeniki.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AiResponse {
	@JsonProperty("is_answerable")
	private boolean answerable;
	private String message;
	@JsonProperty("request_type")
	private String requestType;
	// For register - 複数イベント対応
	private List<Event> events;
	// For search
	@JsonProperty("start_date")
	private String startDate;
	@JsonProperty("end_date")
	private String endDate;	

	public boolean isAnswerable() {
		return answerable;
	}

	public String getMessage() {
		return message;
	}

	public String getRequestType() {
		return requestType;
	}

	public List<Event> getEvents() {
		return events;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setAnswerable(boolean answerable) {
		this.answerable = answerable;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
}
