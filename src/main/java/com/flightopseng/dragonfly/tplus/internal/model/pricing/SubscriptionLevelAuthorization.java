package com.flightopseng.dragonfly.tplus.internal.model.pricing;

public class SubscriptionLevelAuthorization {

	private int id;
	
	private String resource;
	
	private String operation;
	
	private String subscription;
	
	private boolean authorized;
	
	public SubscriptionLevelAuthorization() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getSubscription() {
		return subscription;
	}

	public void setSubscription(String subscription) {
		this.subscription = subscription;
	}

	public boolean isAuthorized() {
		return authorized;
	}

	public void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}
	
}
