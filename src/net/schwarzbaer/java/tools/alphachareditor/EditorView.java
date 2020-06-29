package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;

import javax.swing.JPanel;

import net.schwarzbaer.gui.ZoomableCanvas;
import net.schwarzbaer.java.tools.alphachareditor.EditorView.GuideLine.Type;

class EditorView extends ZoomableCanvas<EditorView.ViewState> {
	
	static final int MAX_NEAR_DISTANCE = 20;
	static final int MAX_GUIDELINE_DISTANCE = 2;

	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}
	
	private static final long serialVersionUID = -2936567438026759797L;
	
	private static final Color COLOR_AXIS       = new Color(0x70000000,true);
	private static final Color COLOR_BACKGROUND = Color.WHITE;
	private static final Color COLOR_GUIDELINES            = new Color(0xf0f0f0);
	private static final Color COLOR_GUIDELINES_HIGLIGHTED = new Color(0xb5f0b5);

	private LineForm<?>[] forms = null;
	private Vector<GuideLine> guideLines = null;
	private LineForm<?> highlightedForm = null;
	private LineFormEditing<?> formEditing = null;
	private GuideLine highlightedGuideLine;
	private final Context context;
	
	EditorView(Context context) {
		this.context = context;
		Assert(this.context!=null);
		activateMapScale(COLOR_AXIS, "px");
		activateAxes(COLOR_AXIS, true,true,true,true);
	}

	void setGuideLines(Vector<GuideLine> guideLines) {
		this.guideLines = guideLines;
		repaint();
	}

	void setForms(LineForm<?>[] forms) {
		this.forms = forms;
		highlightedForm = null;
		deselect();
		repaint();
	}

	ViewState getViewState() { return viewState; }
	
	interface Context {
		void setValuePanel(JPanel panel);
		void updateHighlightedForm(LineForm<?> form);
	}

	double stickToGuideLineX(float x) { return GuideLine.stickToGuideLines(x, Type.Vertical  , viewState.convertLength_ScreenToLength(MAX_GUIDELINE_DISTANCE), guideLines); }
	double stickToGuideLineY(float y) { return GuideLine.stickToGuideLines(y, Type.Horizontal, viewState.convertLength_ScreenToLength(MAX_GUIDELINE_DISTANCE), guideLines); }
	
	void forEachGuideLines(BiConsumer<GuideLine.Type,Float> action) {
		for (GuideLine gl:guideLines)
			action.accept(gl.type,gl.pos);
	}

	@Override public void mouseClicked (MouseEvent e) { if (formEditing!=null) deselect();            else setSelectedForm(e); }
	@Override public void mouseEntered (MouseEvent e) { if (formEditing!=null) formEditing.onEntered (e); else setHighlightedForm(e.getPoint()); setHighlightedGuideLine(null); }
	@Override public void mouseMoved   (MouseEvent e) { if (formEditing!=null) formEditing.onMoved   (e); else setHighlightedForm(e.getPoint()); }
	@Override public void mouseExited  (MouseEvent e) { if (formEditing!=null) formEditing.onExited  (e); else setHighlightedForm((Point)null ); }
	@Override public void mousePressed (MouseEvent e) { if (formEditing==null || !formEditing.onPressed (e)) super.mousePressed (e); }
	@Override public void mouseReleased(MouseEvent e) { if (formEditing==null || !formEditing.onReleased(e)) super.mouseReleased(e); }
	@Override public void mouseDragged (MouseEvent e) { if (formEditing==null || !formEditing.onDragged (e)) super.mouseDragged (e); }
	
	private void deselect() {
		if (formEditing!=null) formEditing.stopEditing();
		formEditing=null;
		context.setValuePanel(null);
	}
	
	private void setSelectedForm(MouseEvent e) {
		setSelectedForm(getNext(e.getPoint()), e);
	}
	void setSelectedForm(LineForm<?> selectedForm) {
		setSelectedForm(selectedForm, null);
	}
	private void setSelectedForm(LineForm<?> selectedForm, MouseEvent e) {
		if (formEditing!=null) formEditing.stopEditing();
		formEditing = LineFormEditing.create(selectedForm,viewState,this,e);
		if (formEditing!=null) context.setValuePanel(formEditing.createValuePanel());
		highlightedForm = null;
		repaint();
	}

	void setHighlightedGuideLine(GuideLine highlightedGuideLine) {
		this.highlightedGuideLine = highlightedGuideLine;
		repaint();
	}

	private void setHighlightedForm(Point p) {
		setHighlightedForm(getNext(p),true);
	}
	void setHighlightedForm(LineForm<?> highlightedForm) {
		setHighlightedForm(highlightedForm,false);
	}
	private void setHighlightedForm(LineForm<?> highlightedForm, boolean updateHighlightedInFormList) {
		if (highlightedForm!=this.highlightedForm) {
			this.highlightedForm = highlightedForm;
			repaint();
		}
		if (updateHighlightedInFormList)
			context.updateHighlightedForm(this.highlightedForm);
	}

	private LineForm<?> getNext(Point p) {
		if (p==null || forms==null) return null;
		
		Double minDist = null;
		LineForm<?> nearest = null;
		float maxDist = viewState.convertLength_ScreenToLength(MAX_NEAR_DISTANCE);
		float x = viewState.convertPos_ScreenToAngle_LongX(p.x);
		float y = viewState.convertPos_ScreenToAngle_LatY (p.y);
		//System.out.printf(Locale.ENGLISH, "getNext: %f,%f (max:%f)%n", x,y,maxDist);
		for (LineForm<?> form:forms) {
			Double dist = form.getDistance(x,y,maxDist);
			//System.out.printf(Locale.ENGLISH, "Distance[%s]: %s%n", form.getClass().getSimpleName(), dist);
			if (dist!=null && (minDist==null || minDist>dist)) {
				minDist = dist;
				nearest = form;
			}
		}
		
		return nearest;
	}

	public Rectangle2D.Float getViewRectangle() {
		Rectangle2D.Float rect = new Rectangle2D.Float();
		rect.x      = viewState.convertPos_ScreenToAngle_LongX(0);
		rect.y      = viewState.convertPos_ScreenToAngle_LatY (0);
		rect.width  = viewState.convertPos_ScreenToAngle_LongX(0+this.width)  - rect.x;
		rect.height = viewState.convertPos_ScreenToAngle_LatY (0+this.height) - rect.y;
		return rect;
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
			
			if (guideLines!=null)
				for (GuideLine gl:guideLines) {
					g2.setColor(gl==highlightedGuideLine ? COLOR_GUIDELINES_HIGLIGHTED : COLOR_GUIDELINES);
					gl.draw(viewState,g2,x,y,width,height);
				}
			
			drawMapDecoration(g2, x, y, width, height);
			
			LineForm<?> selectedForm = formEditing==null ? null : formEditing.getForm();
			if (forms!=null)
				for (LineForm<?> form:forms)
					if (form!=selectedForm && form!=highlightedForm) form.drawLines(g2,viewState,false,false);
			if (selectedForm!=null) { selectedForm.drawLines(g2,viewState,true ,false); selectedForm.drawPoints(g2, viewState); }
			if (highlightedForm !=null) { highlightedForm .drawLines(g2,viewState,false,true ); highlightedForm .drawPoints(g2, viewState); }
		}
		
	}
	
	
	public static void drawPoint(Graphics2D g2, int x, int y, boolean highlighted) {
		int radius = 3;
//		G2.SETCOLOR(COLOR.BLACK);
//		G2.SETXORMODE(COLOR.WHITE);
		g2.setColor(highlighted ? Color.GREEN : Color.WHITE);
		g2.fillOval(x-radius+1, y-radius+1, 2*radius-1, 2*radius-1);
		g2.setColor(Color.BLACK);
		g2.drawOval(x-radius, y-radius, 2*radius, 2*radius);
//		g2.setPaintMode();
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
			min.latitude_y  = (float) -50;
			min.longitude_x = (float) -100;
			max.latitude_y  = (float) 150;
			max.longitude_x = (float) 300;
		}
	}
	
	static class GuideLine {
		
		enum Type {
			Horizontal("Y"), Vertical("X");
			final String axis;
			Type(String axis) { this.axis = axis; }
		}

		final Type type;
		float pos;
		
		public GuideLine(Type type, float pos) {
			this.type = type;
			this.pos = pos;
			Assert(this.type!=null);
		}

		@Override
		public String toString() {
			return String.format(Locale.ENGLISH, "%s GuideLine @ %s:%1.2f", type, type.axis, pos);
		}

		static double stickToGuideLines(float val, Type type, float maxDist, Vector<GuideLine> lines) {
			Float dist = null;
			Float pos = null;
			if (lines!=null)
				for (GuideLine gl:lines)
					if (gl.type==type) {
						float d = Math.abs(gl.pos-val);
						if (d<=maxDist && (dist==null || dist.floatValue()>d)) {
							dist = d;
							pos = gl.pos;
						}
					}
			if (pos!=null) return pos;
			return val;
		}

		public void draw(ViewState viewState, Graphics2D g2, int x, int y, int width, int height) {
			switch (type) {
			case Horizontal:
				int yl = viewState.convertPos_AngleToScreen_LatY(pos);
				g2.drawLine(x, yl, x+width, yl);
				break;
			case Vertical:
				int xl = viewState.convertPos_AngleToScreen_LongX(pos);
				g2.drawLine(xl, y, xl, y+height);
				break;
			}
		}
		
	}
}
