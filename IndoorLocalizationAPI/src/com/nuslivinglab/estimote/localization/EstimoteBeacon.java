package com.nuslivinglab.estimote.localization;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name ="beacons")
public class EstimoteBeacon {
	// date
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private int id;
	@Column(name="name")
	private String name;
	@Column(name="mac")
	private String mac;
	@Column(name="uuid")
	private String uuid;
	@Column(name="major")
	private int major;
	@Column(name="minor")
	private int minor;
//	@Column(name="measured_power")
//	private int measuredPower;
//	private int rssi;
	@Column(name="x")
	private double x;
	@Column(name="y")
	private double y;
//	@Column(name="lat")
//	private double latitude;
//	@Column(name="lon")
//	private double longitude;
	
	// constructor
	public EstimoteBeacon() {}
	
	public EstimoteBeacon(int id, String name, String mac, String uuid,
			int major, int minor,  double x, double y) {
		this.id = id;
		this.name = name;
		this.mac = mac;
		this.uuid = uuid;
		this.major = major;
		this.minor = minor;
//		this.measuredPower = measuredPower;
//		this.rssi = rssi;
		this.x = x;
		this.y = y;
//		this.latitude = latitude; 
//		this.longitude = longitude;
	}
	
	// methods
	// accessors
	public int getId() {
		return this.id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getMac() {
		return this.mac;
	}
	
	public String getUuid() {
		return this.uuid;
	}
	
	public int getMajor() {
		return this.major;
	}
	
	public int getMinor() {
		return this.minor;
	}
	
//	public int getMeasuredPower() {
//		return this.measuredPower;
//	}
	
//	public int getRssi() {
//		return this.rssi;
//	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
//	public double getLatitude() {
//		return this.latitude;
//	}
//	
//	public double getLongitude() {
//		return this.longitude;
//	}
	
	// mutator
	public void setId(int id) {
		this.id = id;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setMac(String mac) {
		this.mac = mac;
	}
	
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public void setMajor(int major) {
		this.major = major;
	}
	
	public void setMinor(int minor) {
		this.minor = minor;
	}
	
//	public void setMeasuredPower(int measuredPower) {
//		this.measuredPower = measuredPower;
//	}
	
//	public void setRssi(int rssi) {
//		this.rssi = rssi;
//	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
//	public void setLaitude(double latitude) {
//		this.latitude = latitude;
//	}
//	
//	public void setLongitude(double longitude) {
//		this.longitude = longitude;
//	}
	
	public String toString() {
		return "id: " + id + "\t name: " + name + "\t uuid: " + uuid
				+ "\t mac: " + mac + "\t major: " + major
				+ "\t minor: " + minor + "\t x: " + x + "\t y: " + y; 
	}
}
