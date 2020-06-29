package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import net.schwarzbaer.image.alphachar.Form;
import net.schwarzbaer.java.tools.alphachareditor.EditorView.ViewState;

interface LineForm<HighlightPointType> extends LineFormEditing.EditableForm<HighlightPointType> {

	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}
	
	static final Stroke STROKE_HIGHLIGHTED = new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND);
	static final Stroke STROKE_STANDARD    = new BasicStroke(1f);
	static final Color COLOR_HIGHLIGHTED = Color.BLUE;
	static final Color COLOR_STANDARD    = Color.BLACK;

	public default void drawLines(Graphics2D g2, ViewState viewState, boolean isSelected, boolean isHighlighted) {
		Stroke prevStroke = g2.getStroke();
		if (isSelected||isHighlighted) {
			g2.setStroke(STROKE_HIGHLIGHTED);
			g2.setColor(COLOR_HIGHLIGHTED);
		} else {
			g2.setStroke(STROKE_STANDARD);
			g2.setColor(COLOR_STANDARD);
		}
		drawLines(g2, viewState);
		g2.setStroke(prevStroke);
	}
	
	void drawLines (Graphics2D g2, ViewState viewState);
	void drawPoints(Graphics2D g2, ViewState viewState);
	Double getDistance(float x, float y, float maxDist);
	LineForm<HighlightPointType> setValues(double[] values);
	
	static LineForm<?> convert(Form form) {
		Assert(form instanceof LineForm);
		return (LineForm<?>) form;
	}

	static Form convert(LineForm<?> form) {
		if (form instanceof PolyLine) return (PolyLine) form;
		if (form instanceof Line    ) return (Line    ) form;
		if (form instanceof Arc     ) return (Arc     ) form;
		Assert(false);
		return null;
	}

	static LineForm<?>[] convert(Form[] arr) {
		if (arr == null) return null;
		LineForm<?>[] newArr = new LineForm<?>[arr.length];
		for (int i=0; i<arr.length; i++) newArr[i] = convert(arr[i]);
		return newArr;
	}
	static Form[] convert(LineForm<?>[] arr) {
		if (arr == null) return null;
		Form[] newArr = new Form[arr.length];
		for (int i=0; i<arr.length; i++) newArr[i] = convert(arr[i]);
		return newArr;
	}

	static LineForm<?> createNew(FormType formType, Rectangle2D.Float viewRect) {
		switch (formType) {
		case PolyLine: return new PolyLine().setValues(createNewValues(formType,viewRect));
		case Line    : return new Line    ().setValues(createNewValues(formType,viewRect));
		case Arc     : return new Arc     ().setValues(createNewValues(formType,viewRect));
		}
		return null;
	}

	static double[] createNewValues(FormType formType, Rectangle2D.Float viewRect) {
		float x = viewRect.x;
		float y = viewRect.y;
		float w = viewRect.width;
		float h = viewRect.height;
		switch (formType) {
		case PolyLine: return new double[] { x+2*w/5,y+2*h/5, x+2.5*w/5,y+3*h/5, x+3*w/5,y+2*h/5 };
		case Line    : return new double[] { x+2*w/5,y+2*h/5, x+3*w/5,y+3*h/5 };
		case Arc     : return new double[] { x+w/2,y+h/2, Math.min(w,h)/6, 0,Math.PI };
		}
		return null;
	}

	static class Factory implements Form.Factory {
		@Override public PolyLine createPolyLine(double[] values) { return new PolyLine().setValues(values); }
		@Override public Line     createLine    (double[] values) { return new Line    ().setValues(values); }
		@Override public Arc      createArc     (double[] values) { return new Arc     ().setValues(values); }
	}
	
	enum FormType { PolyLine, Line, Arc }
	
	static class PolyLine extends Form.PolyLine implements LineForm<Integer> {
		
		interface HighlightListener {
			void highlightedPointChanged(Integer point);
		}
		
		private NextNewPoint nextNewPoint = null;
		private Integer highlightedPoint = null;
		private HighlightListener listener = null;
		
		@Override public void setHighlightedPoint(Integer point) { highlightedPoint = point; if (listener!=null) listener.highlightedPointChanged(highlightedPoint); }
		public void setHighlightListener(HighlightListener listener) { this.listener = listener; }

		@Override
		public String toString() {
			return String.format(Locale.ENGLISH, "PolyLine [ %d points ]", points.size());
		}

		public static String toString(Point p) {
			return String.format(Locale.ENGLISH, "Point ( %1.4f, %1.4f )", p.x, p.y);
		}

		@Override public LineForm.PolyLine setValues(double[] values) { super.setValues(values); return this; }
		
		@Override
		public Double getDistance(float x, float y, float maxDist) {
			Point p = points.get(0);
			double x1 = p.x;
			double y1 = p.y;
			Double minDist = null;
			for (int i=1; i<points.size(); i++) {
				p = points.get(i);
				Double dist = new LineForm.Line(x1,y1,p.x,p.y).getDistance(x,y, maxDist);
				if (dist!=null && (minDist==null || minDist>dist)) minDist = dist;
				x1 = p.x;
				y1 = p.y;
			}
			return minDist;
		}

		@Override public void drawPoints(Graphics2D g2, ViewState viewState) {
			for (int i=0; i<points.size(); i++) {
				Point p1 = points.get(i);
				int x = viewState.convertPos_AngleToScreen_LongX((float) p1.x);
				int y = viewState.convertPos_AngleToScreen_LatY ((float) p1.y);
				EditorView.drawPoint(g2,x,y,highlightedPoint!=null && i==highlightedPoint.intValue());
			}
			if (nextNewPoint!=null) {
				int x = viewState.convertPos_AngleToScreen_LongX((float) nextNewPoint.x);
				int y = viewState.convertPos_AngleToScreen_LatY ((float) nextNewPoint.y);
				EditorView.drawPoint(g2,x,y,true);
			}
		}

		@Override public void drawLines(Graphics2D g2, ViewState viewState) {
			Point p = points.get(0);
			int x1s = viewState.convertPos_AngleToScreen_LongX((float) p.x);
			int y1s = viewState.convertPos_AngleToScreen_LatY ((float) p.y);
			for (int i=1; i<points.size(); i++) {
				if (nextNewPoint!=null && nextNewPoint.pos==i) {
					int x2s = viewState.convertPos_AngleToScreen_LongX((float) nextNewPoint.x);
					int y2s = viewState.convertPos_AngleToScreen_LatY ((float) nextNewPoint.y);
					g2.drawLine(x1s,y1s,x2s,y2s);
					x1s = x2s;
					y1s = y2s;
				}
				p = points.get(i);
				int x2s = viewState.convertPos_AngleToScreen_LongX((float) p.x);
				int y2s = viewState.convertPos_AngleToScreen_LatY ((float) p.y);
				g2.drawLine(x1s,y1s,x2s,y2s);
				x1s = x2s;
				y1s = y2s;
			}
			if (nextNewPoint!=null && nextNewPoint.pos>=points.size()) {
				int x2s = viewState.convertPos_AngleToScreen_LongX((float) nextNewPoint.x);
				int y2s = viewState.convertPos_AngleToScreen_LatY ((float) nextNewPoint.y);
				g2.drawLine(x1s,y1s,x2s,y2s);
			}
		}
		
		private static class NextNewPoint extends Point {
			private int pos;
			public NextNewPoint(double x, double y, int pos) {
				super(x, y);
				this.pos = pos;
			}
			public void set(double x, double y, int pos) {
				set(x, y);
				this.pos = pos;
			}
		}
		
		boolean setNextNewPointOnLine(double x, double y, double maxDist) {
			Point p1 = points.get(0);
			Integer index = null;
			Point p = null;
			double minDist = 0;
			//Form.Line.LineDistance[] distArr = new Form.Line.LineDistance[points.size()-1];
			for (int i=1; i<points.size(); i++) {
				Point p2 = points.get(i);
				Form.Line line = new Form.Line(p1.x,p1.y,p2.x,p2.y);
				Form.Line.LineDistance dist = line.getDistance(x,y);
				//distArr[i-1] = dist;
				if (0<=dist.f && dist.f<=1 && dist.r<=maxDist && (index==null || dist.r<minDist)) {
					index = i;
					minDist = dist.r;
					p = line.computePoint(dist.f);
				}
				p1 = p2;
			}
			//System.out.println("setNextNewPointOnLine: distArr = "+Arrays.toString(distArr));
			
			if (index == null || p == null) return false;
			
			if (nextNewPoint==null) nextNewPoint = new NextNewPoint(p.x,p.y, index);
			else                    nextNewPoint.set(p.x,p.y, index);
			return true;
		}
		
		void setNextNewPoint(double x, double y) {
			if (nextNewPoint==null) nextNewPoint = new NextNewPoint(x,y,points.size());
			else                    nextNewPoint.set(x,y,points.size());
		}

		void clearNextNewPoint() {
			nextNewPoint = null;
		}

		int addNextNewPoint() {
			int index = 0;
			if (nextNewPoint!=null) {
				if (0<=nextNewPoint.pos && nextNewPoint.pos<points.size()) {
					points.insertElementAt(nextNewPoint, nextNewPoint.pos);
					index = nextNewPoint.pos;
				} else {
					points.add(nextNewPoint);
					index = points.size()-1;
				}
			}
			nextNewPoint = null;
			return index;
		}
		
		boolean hasNextNewPoint() {
			return nextNewPoint!=null;
		}
	}
	
	static class Line extends Form.Line implements LineForm<Line.LinePoint> {
		
		enum LinePoint { P1,P2 } 
		
		LinePoint highlightedPoint = null;
		
		private Line() { super(); }
		private Line(double x1, double y1, double x2, double y2) { super(x1, y1, x2, y2); }

		@Override public void setHighlightedPoint(LinePoint point) { highlightedPoint = point; }

		@Override public String toString() {
			return String.format(Locale.ENGLISH, "Line [ (%1.2f,%1.2f), (%1.2f,%1.2f) ]", x1, y1, x2, y2);
		}

		@Override public LineForm.Line setValues(double[] values) { super.setValues(values); return this; }

		@Override public void drawLines(Graphics2D g2, ViewState viewState) {
			int x1s = viewState.convertPos_AngleToScreen_LongX((float) x1);
			int y1s = viewState.convertPos_AngleToScreen_LatY ((float) y1);
			int x2s = viewState.convertPos_AngleToScreen_LongX((float) x2);
			int y2s = viewState.convertPos_AngleToScreen_LatY ((float) y2);
			g2.drawLine(x1s,y1s,x2s,y2s);
		}
		
		@Override
		public void drawPoints(Graphics2D g2, ViewState viewState) {
			int x1s = viewState.convertPos_AngleToScreen_LongX((float) x1);
			int y1s = viewState.convertPos_AngleToScreen_LatY ((float) y1);
			int x2s = viewState.convertPos_AngleToScreen_LongX((float) x2);
			int y2s = viewState.convertPos_AngleToScreen_LatY ((float) y2);
			EditorView.drawPoint(g2,x1s,y1s,highlightedPoint==LinePoint.P1);
			EditorView.drawPoint(g2,x2s,y2s,highlightedPoint==LinePoint.P2);
		}

		@Override
		public Double getDistance(float x, float y, float maxDist) {
			LineDistance dist = getDistance(x, y);
			if (dist.f>1) {
				// after (x2,y2)
				double d = Math2.dist(x2,y2,x,y);
				if (d<=maxDist) return d;
				return null;
			}
			if (dist.f<0) {
				// before (x1,y1)
				double d = Math2.dist(x1,y1,x,y);
				if (d<=maxDist) return d;
				return null;
			}
			if (dist.r<=maxDist) return dist.r;
			return null;
		}
	}
	
	static class Arc extends Form.Arc implements LineForm<Arc.ArcPoint> {

		private ArcPoint highlightedPoint = null;
		@Override public void setHighlightedPoint(ArcPoint point) { highlightedPoint = point; }

		static class ArcPoint {
			enum Type { Radius, Center, Start, End }
			final Type type;
			double x;
			double y;
			
			ArcPoint(Type type, double x, double y) {
				this.type = type;
				this.x = x;
				this.y = y;
				Assert(this.type!=null);
			}
		}
		
		@Override
		public String toString() {
			return String.format(Locale.ENGLISH, "Arc [ C:(%1.2f,%1.2f), R:%1.2f, Angle(%1.1f..%1.1f) ]", xC, yC, r, aStart*180/Math.PI, aEnd*180/Math.PI);
		}

		@Override public LineForm.Arc setValues(double[] values) { super.setValues(values); return this; }
		
		@Override
		public void drawLines(Graphics2D g2, ViewState viewState) {
			int xCs = viewState.convertPos_AngleToScreen_LongX((float) xC);
			int yCs = viewState.convertPos_AngleToScreen_LatY ((float) yC);
			int rs  = viewState.convertLength_LengthToScreen((float) r);
			int startAngle = (int) Math.round( -aEnd       *180/Math.PI);
			int arcAngle   = (int) Math.round((aEnd-aStart)*180/Math.PI);
			g2.drawArc(xCs-rs, yCs-rs, rs*2, rs*2, startAngle, arcAngle);
		}

		@Override
		public void drawPoints(Graphics2D g2, ViewState viewState) {
			int xCs = viewState.convertPos_AngleToScreen_LongX((float) xC);
			int yCs = viewState.convertPos_AngleToScreen_LatY ((float) yC);
			int xSs = viewState.convertPos_AngleToScreen_LongX((float) (xC+r*Math.cos(aStart)));
			int ySs = viewState.convertPos_AngleToScreen_LatY ((float) (yC+r*Math.sin(aStart)));
			int xEs = viewState.convertPos_AngleToScreen_LongX((float) (xC+r*Math.cos(aEnd  )));
			int yEs = viewState.convertPos_AngleToScreen_LatY ((float) (yC+r*Math.sin(aEnd  )));
			EditorView.drawPoint(g2,xSs,ySs,isType(highlightedPoint,ArcPoint.Type.Start ));
			EditorView.drawPoint(g2,xEs,yEs,isType(highlightedPoint,ArcPoint.Type.End   ));
			EditorView.drawPoint(g2,xCs,yCs,isType(highlightedPoint,ArcPoint.Type.Center));
			if (isType(highlightedPoint,ArcPoint.Type.Radius)) {
				int xRs = viewState.convertPos_AngleToScreen_LongX((float) highlightedPoint.x);
				int yRs = viewState.convertPos_AngleToScreen_LatY ((float) highlightedPoint.y);
				EditorView.drawPoint(g2,xRs,yRs,true);
			}
		}
		
		private boolean isType(ArcPoint p, ArcPoint.Type t) {
			return p!=null && p.type==t;
		}

		@Override
		public Double getDistance(float x, float y, float maxDist) {
			double dC = Math2.dist(xC,yC,x,y);
			if (Math.abs(dC-r)>maxDist) return null;
			
			double w = Math2.angle(xC,yC,x,y);
			if (Math2.isInsideAngleRange(aStart, aEnd, w)) {
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
