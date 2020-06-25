package net.schwarzbaer.java.tools.alphachareditor;

public class Math2 {

	public static double dist(double xC, double yC, double x, double y) {
		double localX = x-xC;
		double localY = y-yC;
		return Math.sqrt(localX*localX+localY*localY);
	}
	
	public static double angle(double xC, double yC, double x, double y) {
		double localX = x-xC;
		double localY = y-yC;
		return Math.atan2(localY, localX);
	}
	
}