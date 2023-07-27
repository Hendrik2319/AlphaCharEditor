package net.schwarzbaer.java.tools.lineeditor;

import net.schwarzbaer.java.lib.image.bumpmapping.BumpMapping;

class Math2 {

	static double dist(double xC, double yC, double x, double y) {
		double localX = x-xC;
		double localY = y-yC;
		return Math.sqrt(localX*localX+localY*localY);
	}
	
	static double angle(double xC, double yC, double x, double y) {
		double localX = x-xC;
		double localY = y-yC;
		return Math.atan2(localY, localX);
	}
	
	static double normalizeAngle(double minW, double w) {
		return BumpMapping.normalizeAngle(minW, w);
	}
	
	static boolean isInsideAngleRange(double minW, double maxW, double w) {
		return BumpMapping.isInsideAngleRange(minW, maxW, w);
	}

	static double computeAngleDist(double a1, double a2) {
		// result: -Math.PI ... Math.PI
		a2 = normalizeAngle(a1, a2); // a2 in a1...a1+2*Math.PI
		if (a2>a1+Math.PI) return a2-a1-2*Math.PI;
		return a2-a1;
	}
}