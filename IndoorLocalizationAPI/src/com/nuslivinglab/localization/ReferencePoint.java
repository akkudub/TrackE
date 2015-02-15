package com.nuslivinglab.localization;

public class ReferencePoint {
	// data
	private final double BETA = 2.5;
	private final double P0 = -72;
	
	private String id;
	private double x;
	private double y;
	
	// constructor
	public ReferencePoint(String id, double x, double y) {
		this.id = id;
		this.x = x;
		this.y = y;
	}
	
	// methods
	// accessor
	public String getId() {
		return this.id;
	}
	public double getX() {
		return this.x;
	}
	public double getY() {
		return this.y;
	}
	
	public double getK() {
		return x*x + y*y;
	}
	
	// mutator
	public void setId(String id) {
		this.id = id;
	}
	public void setX(double x) {
		this.x = x;
	}
	public void setY(double y) {
		this.y = y;
	}
	
	// compute distance, p is received signal strength
	public double computeDistance(double p) {
		if(p > P0) {
			return 1.0;
		}
		return Math.pow(10, (P0 - p)/(10*BETA));
	}
}
