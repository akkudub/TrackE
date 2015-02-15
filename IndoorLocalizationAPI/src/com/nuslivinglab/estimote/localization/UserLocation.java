package com.nuslivinglab.estimote.localization;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="users_location")
public class UserLocation {
	// data
	@Id
	@Column(name="user_id")
	private String userId;
	@Column(name="x")
	private double x;
	@Column(name="y")
	private double y;
	@Column(name="accuracy")
	private double accuracy;
	@Column(name="timestamp")
	private long timestamp;
	
	// constructor
	public UserLocation() {}
	public UserLocation(String userId, double x, double y, double accuracy, long timestamp) {
		this.userId = userId;
		this.x = x;
		this.y = y;
		this.accuracy = accuracy;
		this.timestamp = timestamp;
	}
	
	// methods
	// accessor
	public String getUserId() {
		return this.userId;
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public double getAccuray() {
		return this.accuracy;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	// mutator
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public String toString() {
		return "userId: " + userId + "\t x: " + x + "\t y: " + y;
	}
}
