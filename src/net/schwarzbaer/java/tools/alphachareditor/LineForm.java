package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import net.schwarzbaer.image.alphachar.Form;
import net.schwarzbaer.image.bumpmapping.BumpMapping;
import net.schwarzbaer.java.tools.alphachareditor.EditorView.ViewState;

public interface LineForm {
	static final Stroke STROKE_HIGHLIGHTED = new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
	static final Stroke STROKE_STANDARD    = new BasicStroke(1f);
	static final Color COLOR_HIGHLIGHTED = Color.BLUE;
	static final Color COLOR_STANDARD    = Color.BLACK;

	public void draw(Graphics2D g2, ViewState viewState, boolean isSelected, boolean isHighlighted);
	public Double getDistance(float x, float y, float maxDist);
	
	public static class Math2 {

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
	
	public static class Factory implements Form.Factory {
		@Override public PolyLine createPolyLine(double[] values) { return new PolyLine().setValues(values); }
		@Override public Line     createLine    (double[] values) { return new Line    ().setValues(values); }
		@Override public Arc      createArc     (double[] values) { return new Arc     ().setValues(values); }
	}
	
	public class PolyLine extends Form.PolyLine implements LineForm {
		@Override public LineForm.PolyLine setValues(double[] values) { super.setValues(values); return this; }
		
		@Override
		public Double getDistance(float x, float y, float maxDist) {
			Point p = points.get(0);
			double x1 = p.x;
			double y1 = p.y;
			Double minDist = null;
			for (int i=1; i<points.size(); i++) {
				p = points.get(i);
				Double dist = LineForm.Line.getDistance( x1,y1,p.x,p.y, x,y, maxDist);
				if (dist!=null && (minDist==null || minDist>dist)) minDist = dist;
				x1 = p.x;
				y1 = p.y;
			}
			return minDist;
		}

		@Override
		public void draw(Graphics2D g2, ViewState viewState, boolean isSelected, boolean isHighlighted) {
			if (isSelected||isHighlighted) {
				g2.setStroke(STROKE_HIGHLIGHTED);
				g2.setColor(COLOR_HIGHLIGHTED);
			} else {
				g2.setStroke(STROKE_STANDARD);
				g2.setColor(COLOR_STANDARD);
			}
			Point p = points.get(0);
			int x1s = viewState.convertPos_AngleToScreen_LongX((float) p.x);
			int y1s = viewState.convertPos_AngleToScreen_LatY ((float) p.y);
			for (int i=1; i<points.size(); i++) {
				p = points.get(i);
				int x2s = viewState.convertPos_AngleToScreen_LongX((float) p.x);
				int y2s = viewState.convertPos_AngleToScreen_LatY ((float) p.y);
				g2.drawLine(x1s,y1s,x2s,y2s);
				x1s = x2s;
				y1s = y2s;
			}
		}
	}
	
	public class Line extends Form.Line implements LineForm {
		@Override public LineForm.Line setValues(double[] values) { super.setValues(values); return this; }

		@Override
		public void draw(Graphics2D g2, ViewState viewState, boolean isSelected, boolean isHighlighted) {
			if (isSelected||isHighlighted) {
				g2.setStroke(STROKE_HIGHLIGHTED);
				g2.setColor(COLOR_HIGHLIGHTED);
			} else {
				g2.setStroke(STROKE_STANDARD);
				g2.setColor(COLOR_STANDARD);
			}
			int x1s = viewState.convertPos_AngleToScreen_LongX((float) x1);
			int y1s = viewState.convertPos_AngleToScreen_LatY ((float) y1);
			int x2s = viewState.convertPos_AngleToScreen_LongX((float) x2);
			int y2s = viewState.convertPos_AngleToScreen_LatY ((float) y2);
			g2.drawLine(x1s,y1s,x2s,y2s);
		}
		
		@Override
		public Double getDistance(float x, float y, float maxDist) {
			return getDistance(x1, y1, x2, y2, x, y, maxDist);
		}
		
		public static Double getDistance(double x1, double y1, double x2, double y2, float x, float y, float maxDist) {
			double length = Math2.dist(x1,y1,x2,y2);
			double f = ((x2-x1)*(x-x1)+(y2-y1)*(y-y1))/length/length; // cos(a)*|x-x1,y-y1|*|x2-x1,y2-y1| / |x2-x1,y2-y1|² -> (x1,y1) ..f.. (x2,y2)
			if (f>1) {
				// after (x2,y2)
				double d = Math2.dist(x2,y2,x,y);
				if (d>maxDist) return null;
				return d;
			}
			if (f<0) {
				// before (x1,y1)
				double d = Math2.dist(x1,y1,x,y);
				if (d>maxDist) return null;
				return d;
			}
			// between (x1,y1) and (x2,y2)
			double d = Math.abs(((x2-x1)*(y-y1)-(y2-y1)*(x-x1))/length); // sin(a)*|x-x1,y-y1|*|x2-x1,y2-y1| / |x2-x1,y2-y1|  =  sin(a)*|x-x1,y-y1|  =  r
			if (d>maxDist) return null;
			return d;
		}
	}
	
	public class Arc extends Form.Arc implements LineForm {

		@Override public LineForm.Arc setValues(double[] values) { super.setValues(values); return this; }

		@Override
		public void draw(Graphics2D g2, ViewState viewState, boolean isSelected, boolean isHighlighted) {
			if (isSelected||isHighlighted) {
				g2.setStroke(STROKE_HIGHLIGHTED);
				g2.setColor(COLOR_HIGHLIGHTED);
			} else {
				g2.setStroke(STROKE_STANDARD);
				g2.setColor(COLOR_STANDARD);
			}
			int xCs = viewState.convertPos_AngleToScreen_LongX((float) xC);
			int yCs = viewState.convertPos_AngleToScreen_LatY ((float) yC);
			int rs  = viewState.convertLength_LengthToScreen((float) r);
			int startAngle = (int) Math.round( aStart      *180/Math.PI);
			int arcAngle   = (int) Math.round((aEnd-aStart)*180/Math.PI);
			g2.drawArc(xCs-rs, yCs-rs, rs*2, rs*2, startAngle, arcAngle);
		}
		
		@Override
		public Double getDistance(float x, float y, float maxDist) {
			double dC = Math2.dist(xC,yC,x,y);
			if (Math.abs(dC-r)>maxDist) return null;
			
			double w = Math2.angle(xC,yC,x,y);
			if (BumpMapping.isInsideAngleRange(aStart, aEnd, w)) {
				return Math.abs(dC-r);
			}
			double xS = r*Math.cos(aStart);
			double yS = r*Math.sin(aStart);
			double xE = r*Math.cos(aEnd);
			double yE = r*Math.sin(aEnd);
			double dS = Math2.dist(xS,yS,x,y);
			double dE = Math2.dist(xE,yE,x,y);
			if (dS<dE) {
				if (dS<=maxDist) return dS;
			} else {
				if (dE<=maxDist) return dE;
			}
			return null;
		}
	}

}
