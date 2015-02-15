package com.nuslivinglab.localization;

public class ReceivedSignal {
	// data
	private String id;
	private double rss;
	
	// constructor
	public ReceivedSignal(String id, double rss) {
		this.id = id;
		this.rss = rss;
	}
	
	// methods
	// accessors
	public String getId() {
		return this.id;
	}
	public double getRss() {
		return this.rss;
	}
	
	// mutator
	public void setId(String id) {
		this.id = id;
	}
	public void setRss(double rss) {
		this.rss = rss;
	}
}
