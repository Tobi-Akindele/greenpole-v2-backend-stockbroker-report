package com.ap.greenpole.stockbroker.dto;

import java.util.List;

import com.google.gson.Gson;

public class RequestAuthorization {

	private List<Long> requestIds;
	private String action;
	private String rejectionReason;
	
	public List<Long> getRequestIds() {
		return requestIds;
	}
	public void setRequestIds(List<Long> requestIds) {
		this.requestIds = requestIds;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	
	public String getRejectionReason() {
		return rejectionReason;
	}
	public void setRejectionReason(String rejectionReason) {
		this.rejectionReason = rejectionReason;
	}
	@Override
	public String toString() {
		try {
			return new Gson().toJson(this);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
