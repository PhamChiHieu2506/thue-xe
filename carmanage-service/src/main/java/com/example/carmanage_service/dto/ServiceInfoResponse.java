package com.example.carmanage_service.dto;

public class ServiceInfoResponse {
	private String service;
	private String version;
	private String status;
	/**
	 * 
	 */
	public ServiceInfoResponse() {
		super();
	}
	public ServiceInfoResponse(String service, String version, String status) {
		super();
		this.service = service;
		this.version = version;
		this.status = status;
	}
	public String getService() {
		return service;
	}
	public void setService(String service) {
		this.service = service;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
}
