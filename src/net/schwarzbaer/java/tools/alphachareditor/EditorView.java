package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import net.schwarzbaer.gui.ZoomableCanvas;
import net.schwarzbaer.image.alphachar.Form;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Arc;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Line;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.PolyLine;

class EditorView extends ZoomableCanvas<EditorView.ViewState> {
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}
	
	private static final long serialVersionUID = -2936567438026759797L;
	
	private static final Color COLOR_AXIS       = new Color(0x70000000,true);
	private static final Color COLOR_BACKGROUND = Color.WHITE;
	private static final Color COLOR_GUIDELINES      = new Color(0xf0f0f0);

	private LineForm[] forms;
	private LineForm highlighted;
	private LineFormEditing<?> editing;
	
	EditorView() {
		activateMapScale(COLOR_AXIS, "px");
		activateAxes(COLOR_AXIS, true,true,true,true);
		forms = null;
		highlighted = null;
		editing = null;
		
		MouseAdapter m = new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) { if (editing==null) setSelected   (e.getPoint()); else editing.onClicked(e); }
			@Override public void mouseEntered(MouseEvent e) { if (editing==null) setHighlighted(e.getPoint()); else editing.onEntered(e); }
			@Override public void mouseMoved  (MouseEvent e) { if (editing==null) setHighlighted(e.getPoint()); else editing.onMoved  (e); }
			@Override public void mouseExited (MouseEvent e) { if (editing==null) setHighlighted(null        ); else editing.onExited (e); }
		};
		addMouseListener(m);
		addMouseMotionListener(m);
	}
	
	protected void setSelected(Point p) {
		editing = LineFormEditing.create(getNext(p));
		highlighted = null;
		repaint();
	}

	protected void setHighlighted(Point p) {
		LineForm form = getNext(p);
		if (form!=highlighted) {
			highlighted = form;
			repaint();
		}
	}

	private LineForm getNext(Point p) {
		if (p==null || forms==null) return null;
		
		Double minDist = null;
		LineForm nearest = null;
		float maxDist = viewState.convertLength_ScreenToLength(20);
		float x = viewState.convertPos_ScreenToAngle_LongX(p.x);
		float y = viewState.convertPos_ScreenToAngle_LatY (p.y);
		//System.out.printf(Locale.ENGLISH, "getNext: %f,%f (max:%f)%n", x,y,maxDist);
		for (LineForm form:forms) {
			Double dist = form.getDistance(x,y,maxDist);
			//System.out.printf(Locale.ENGLISH, "Distance[%s]: %s%n", form.getClass().getSimpleName(), dist);
			if (dist!=null && (minDist==null || minDist>dist)) {
				minDist = dist;
				nearest = form;
			}
		}
		
		return nearest;
	}

	public void setForms(Form[] forms) {
		if (forms==null)
			this.forms=null;
		else {
			this.forms = new LineForm[forms.length];
			for (int i=0; i<forms.length; i++) {
				Form form = forms[i];
				Assert(form instanceof LineForm);
				this.forms[i] = (LineForm) form;
			}
		}
		editing = null;
		highlighted = null;
		repaint();
	}

	@Override
	protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
		g.setColor(COLOR_BACKGROUND);
		g.fillRect(x, y, width, height);
		
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setClip(x, y, width, height);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			//g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			
			int X0   = viewState.convertPos_AngleToScreen_LongX(  0);
			int Y0   = viewState.convertPos_AngleToScreen_LatY (  0);
			int Y40  = viewState.convertPos_AngleToScreen_LatY ( 40);
			int Y100 = viewState.convertPos_AngleToScreen_LatY (100);
			
			g2.setColor(COLOR_GUIDELINES);
			g2.drawLine(X0, y, X0, y+height);
			g2.drawLine(x, Y0  , x+width, Y0  );
			g2.drawLine(x, Y40 , x+width, Y40 );
			g2.drawLine(x, Y100, x+width, Y100);
			
			drawMapDecoration(g2, x, y, width, height);
			
			if (forms!=null)
				for (LineForm form:forms)
					form.draw(g2,viewState,editing!=null && form==editing.form,form==highlighted);
		}
		
	}
	
	
	@Override
	protected ViewState createViewState() {
		return new ViewState(this);
	}
	class ViewState extends ZoomableCanvas.ViewState {
		
		ViewState(ZoomableCanvas<?> canvas) {
			super(canvas,0.1f);
			setPlainMapSurface();
			setVertAxisDownPositive(true);
			//debug_showChanges_scalePixelPerLength = true;
		}

		@Override
		protected void determineMinMax(MapLatLong min, MapLatLong max) {
			min.latitude_y  = (float) 0;
			min.longitude_x = (float) 0;
			max.latitude_y  = (float) 200;
			max.longitude_x = (float) 400;
		}
	}
	
	private static class LineFormEditing<FormType extends LineForm> {
		
		FormType form;
		LineFormEditing(FormType form) {
			this.form = form;
		}
		
		public void onExited (MouseEvent e) {}
		public void onMoved  (MouseEvent e) {}
		public void onEntered(MouseEvent e) {}
		public void onClicked(MouseEvent e) {}

		static LineFormEditing<?> create(LineForm form) {
			if (form instanceof PolyLine) return new PolyLineEditing((PolyLine) form);
			if (form instanceof Line    ) return new     LineEditing((Line    ) form);
			if (form instanceof Arc     ) return new      ArcEditing((Arc     ) form);
			return null;
		}

		static class PolyLineEditing extends LineFormEditing<PolyLine> { public PolyLineEditing(PolyLine polyLine) { super(polyLine); } }
		static class     LineEditing extends LineFormEditing<Line    > { public     LineEditing(Line     line    ) { super(line    ); } }
		static class      ArcEditing extends LineFormEditing<Arc     > { public      ArcEditing(Arc      arc     ) { super(arc     ); } }
	}
}
