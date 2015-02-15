package com.nuslivinglab.estimote.localization;

public class ReceivedBeacon {
	// data
	private String uuid;
	private String macAddress;
	private int major;
	private int minor;
	private int rssi;
	
	// constructor
	public ReceivedBeacon(String uuid, String macAddress, int major, int minor, int rssi) {
		this.uuid = uuid;
		this.macAddress = macAddress;
		this.major = major;
		this.minor = minor;
		this.rssi = rssi;
	}
	
	// methods
	// accessor
	public String getUuid() {
		return this.uuid;
	}
	
	public String getMacAddress() {
		return this.macAddress;
	}
	
	public int getMajor() {
		return this.major;
	}
	
	public int getMinor() {
		return this.minor;
	}
	
	public int getRssi() {
		return this.rssi;
	}
	
	// mutator
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public void setMacAddress(String macAddress) {
		this.macAddress = macAddress;
	}
	
	public void setMinor(int minor) {
		this.minor = minor;
	}
	
	public void setMajor(int major) {
		this.major = major;
	}
	
	public void setRssi(int rssi) {
		this.rssi = rssi;
	}
	
	// compare to
	public int compareTo(ReceivedBeacon beaconTwo) {
		int rssiTwo = beaconTwo.getRssi();
		if(this.rssi == rssiTwo) {
			return 0;
		} else if(this.rssi > rssiTwo) {
			return 1;
		} else {
			return -1;
		}
	}
	
	// to string
	public String toString() {
		return "uuid: " + uuid + "\t mac: " + macAddress + "\t major: " + major
				+ "\t minor: " + minor + "\t rssi: " + rssi; 
	}
	
}
