package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.schwarzbaer.java.tools.alphachareditor.EditorView.ViewState;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Arc;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Arc.ArcPoint;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Arc.ArcPoint.Type;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Line;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Line.SelectedPoint;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.PolyLine;

abstract class LineFormEditing<HighlightedPointType> {

	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}
	
	interface EditableForm<HPType> {
		void setHighlightedPoint(HPType point);
	}
	
	private final LineForm<HighlightedPointType> form;
	protected final ViewState viewState;
	protected final EditorView editorView;
	private Point pickOffset = null;
	private HighlightedPointType selectedPoint = null;
	
	LineFormEditing(LineForm<HighlightedPointType> form, ViewState viewState, EditorView editorView) {
		this.form = form;
		this.viewState = viewState;
		this.editorView = editorView;
	}
	
	public LineForm<HighlightedPointType> getForm() { return form; }

	protected abstract HighlightedPointType getNext(int x, int y);
	protected abstract Point computePickOffset(int x, int y, HighlightedPointType selectedPoint);
	protected abstract void modifySelectedPoint(HighlightedPointType selectedPoint, int x, int y, Point pickOffset);

	public abstract JPanel createValuePanel();
	public void onEntered (MouseEvent e) { form.setHighlightedPoint(getNext(e.getX(),e.getY())); editorView.repaint(); }
	public void onMoved   (MouseEvent e) { form.setHighlightedPoint(getNext(e.getX(),e.getY())); editorView.repaint(); }
	public void onExited  (MouseEvent e) { form.setHighlightedPoint(null                      ); editorView.repaint(); }
	
	public boolean onPressed (MouseEvent e) {
		if (e.getButton()!=MouseEvent.BUTTON1) return false;
		int x = e.getX();
		int y = e.getY();
		selectedPoint = getNext(x,y);
		form.setHighlightedPoint(selectedPoint);
		if (selectedPoint!=null) {
			pickOffset = computePickOffset(x,y,selectedPoint);
			Assert(pickOffset!=null);
			editorView.repaint();
			return true;
		}
		pickOffset = null;
		editorView.repaint();
		return false;
	}
	
	public boolean onReleased(MouseEvent e) {
		selectedPoint = null;
		form.setHighlightedPoint(null);
		pickOffset = null;
		editorView.repaint();
		return false;
	}
	
	public boolean onDragged (MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		if (selectedPoint!=null) {
			form.setHighlightedPoint(selectedPoint);
			modifySelectedPoint(selectedPoint,x,y,pickOffset);
			editorView.repaint();
			return true;
		}
		editorView.repaint();
		return false;
	}

	static LineFormEditing<?> create(LineForm<?> form, ViewState viewState, EditorView editorView, MouseEvent e) {
		if (form instanceof PolyLine) return new PolyLineEditing((PolyLine) form, viewState, editorView, e);
		if (form instanceof Line    ) return new     LineEditing((Line    ) form, viewState, editorView, e);
		if (form instanceof Arc     ) return new      ArcEditing((Arc     ) form, viewState, editorView, e);
		return null;
	}

	protected void setFixed(JTextField field, boolean isFixed) {
		field.setEditable(!isFixed);
		//field.setEnabled (!isFixed);
	}
	
	protected JCheckBox createCheckBox(String title, boolean isSelected, Consumer<Boolean> setValue) {
		JCheckBox comp = new JCheckBox(title,isSelected);
		if (setValue!=null)
			comp.addActionListener(e->{
				setValue.accept(comp.isSelected());
				editorView.repaint();
			});
		return comp;
	}
	
	protected GenericTextField<Double> createDoubleInput(double value, Consumer<Double> setValue) {
		return createDoubleInput(value, setValue, v->true);
	}

	protected GenericTextField<Double> createDoubleInput(double value, Consumer<Double> setValue, Predicate<Double> isOK) {
		Function<String,Double> parse = str->{ try { return Double.parseDouble(str); } catch (NumberFormatException e) { return Double.NaN; } };
		Predicate<Double> isOK2 = v->v!=null && !Double.isNaN(v) && isOK.test(v);
		Function<Double, String> toString = v->v==null ? "" : v.toString();
		return new GenericTextField<>(value, toString, parse, isOK2, setValue);
	}

	protected GenericTextField<String> createTextInput(String value, Consumer<String> setValue, Predicate<String> isOK) {
		return new GenericTextField<>(value, v->v, v->v, isOK, setValue, setValue);
	}
	
	protected class GenericTextField<V> extends JTextField {
		private static final long serialVersionUID = 1942488481760358728L;
		
		private final Function<V, String> toString;
		private final Color defaultBG;
		private final Function<String, V> parse;
		private final Predicate<V> isOK;

		GenericTextField(V value, Function<V,String> toString, Function<String,V> parse, Predicate<V> isOK, Consumer<V> setValue) {
			this(value, toString, parse, isOK, setValue, null);
		}
		GenericTextField(V value, Function<V,String> toString, Function<String,V> parse, Predicate<V> isOK, Consumer<V> setValue, Consumer<V> setValueWhileAdjusting) {
			super(toString.apply(value),5);
			this.toString = toString;
			this.parse = parse;
			this.isOK = isOK;
			defaultBG = getBackground();
			if (setValueWhileAdjusting!=null) {
				addCaretListener (e -> {
					readTextField(v -> {
						setValueWhileAdjusting.accept(v);
						editorView.repaint();
					});
				});
			}
			Consumer<V> modifiedSetValue = d -> {
				setValue.accept(d);
				editorView.repaint();
			};
			addActionListener(e->{ readTextField(modifiedSetValue); });
			addFocusListener(new FocusListener() {
				@Override public void focusLost  (FocusEvent e) { readTextField(modifiedSetValue); }
				@Override public void focusGained(FocusEvent e) {}
			});
		}
		
		void setValue(V value) { setText(toString.apply(value)); }
		
		private void readTextField(Consumer<V> setValue) {
			V d = parse.apply(getText());
			if (isOK.test(d)) {
				setBackground(defaultBG);
				setValue.accept(d);
			} else {
				setBackground(Color.RED);
			}
		}
	}

	static class LineEditing extends LineFormEditing<Line.SelectedPoint> {
		
		private int x1s,y1s,x2s,y2s;
		private GenericTextField<Double> x1Field = null;
		private GenericTextField<Double> y1Field = null;
		private GenericTextField<Double> x2Field = null;
		private GenericTextField<Double> y2Field = null;
		private boolean isX1Fixed = false;
		private boolean isY1Fixed = false;
		private boolean isX2Fixed = false;
		private boolean isY2Fixed = false;
		private Line line;
		
		public LineEditing(Line line, ViewState viewState, EditorView editorView, MouseEvent e) {
			super(line, viewState, editorView);
			this.line = line;
			this.line.setHighlightedPoint(e==null ? null : getNext(e.getX(),e.getY()));
		}
				
		@Override
		public JPanel createValuePanel() {
			int i=0;
			JPanel panel = new JPanel(new GridBagLayout());
			panel.setBorder(BorderFactory.createTitledBorder("Line Values"));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty=0;
			c.weightx=0; c.gridx=0; i=0;
			c.gridy=i++; panel.add(new JLabel("X1: "),c);
			c.gridy=i++; panel.add(new JLabel("Y1: "),c);
			c.gridy=i++; panel.add(new JLabel("X2: "),c);
			c.gridy=i++; panel.add(new JLabel("Y2: "),c);
			c.weightx=1; c.gridx=1; i=0;
			c.gridy=i++; panel.add(x1Field=createDoubleInput(line.x1, v->line.x1=v),c);
			c.gridy=i++; panel.add(y1Field=createDoubleInput(line.y1, v->line.y1=v),c);
			c.gridy=i++; panel.add(x2Field=createDoubleInput(line.x2, v->line.x2=v),c);
			c.gridy=i++; panel.add(y2Field=createDoubleInput(line.y2, v->line.y2=v),c);
			c.weightx=0; c.gridx=2; i=0;
			c.gridy=i++; panel.add(createCheckBox("fixed", isX1Fixed, b->setFixed(x1Field,isX1Fixed=b)),c);
			c.gridy=i++; panel.add(createCheckBox("fixed", isY1Fixed, b->setFixed(y1Field,isY1Fixed=b)),c);
			c.gridy=i++; panel.add(createCheckBox("fixed", isX2Fixed, b->setFixed(x2Field,isX2Fixed=b)),c);
			c.gridy=i++; panel.add(createCheckBox("fixed", isY2Fixed, b->setFixed(y2Field,isY2Fixed=b)),c);
			c.weighty=1; c.weightx=1;
			c.gridx=0; c.gridy=i; c.gridwidth=3;
			panel.add(new JLabel(),c);
			return panel;
		}

		@Override protected SelectedPoint getNext(int x, int y) {
			x1s = viewState.convertPos_AngleToScreen_LongX((float) line.x1);
			y1s = viewState.convertPos_AngleToScreen_LatY ((float) line.y1);
			x2s = viewState.convertPos_AngleToScreen_LongX((float) line.x2);
			y2s = viewState.convertPos_AngleToScreen_LatY ((float) line.y2);
			double d1 = Math2.dist(x1s, y1s, x, y);
			double d2 = Math2.dist(x2s, y2s, x, y);
			if (d1<d2 && d1<EditorView.MAX_NEAR_DISTANCE) return SelectedPoint.P1;
			if (d2<d1 && d2<EditorView.MAX_NEAR_DISTANCE) return SelectedPoint.P2;
			return null;
		}
		
		@Override protected Point computePickOffset(int x, int y, SelectedPoint selectedPoint) {
			switch (selectedPoint) {
			case P1: return new Point(x1s-x, y1s-y);
			case P2: return new Point(x2s-x, y2s-y);
			}
			return null;
		}

		@Override
		protected void modifySelectedPoint(SelectedPoint selectedPoint, int x, int y, Point pickOffset) {
			x+=pickOffset.x;
			y+=pickOffset.y;
			switch (selectedPoint) {
			case P1:
				if (!isX1Fixed) x1Field.setValue(line.x1 = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x)));
				if (!isY1Fixed) y1Field.setValue(line.y1 = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y)));
				break;
			case P2:
				if (!isX2Fixed) x2Field.setValue(line.x2 = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x)));
				if (!isY2Fixed) y2Field.setValue(line.y2 = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y)));
				break;
			}
		}
	}
	
	static class ArcEditing extends LineFormEditing<Arc.ArcPoint> {
		
		private GenericTextField<Double>     cxField = null;
		private GenericTextField<Double>     cyField = null;
		private GenericTextField<Double>      rField = null;
		private GenericTextField<Double> aStartField = null;
		private GenericTextField<Double>   aEndField = null;
		private boolean     isCxFixed = false;
		private boolean     isCyFixed = false;
		private boolean      isRFixed = false;
		private boolean isAStartFixed = false;
		private boolean   isAEndFixed = false;
		
		private double[] glAngles = null;
		private double maxGlAngle = Double.NaN;
		private final Arc arc;
		
		public ArcEditing(Arc arc, ViewState viewState, EditorView editorView, MouseEvent e) {
			super(arc, viewState, editorView);
			this.arc = arc;
			this.arc.setHighlightedPoint(e==null ? null : getNext(e.getX(),e.getY()));
		}
		
		@Override public JPanel createValuePanel() {
			JPanel panel = new JPanel(new GridBagLayout());
			panel.setBorder(BorderFactory.createTitledBorder("Arc Values"));
			GridBagConstraints c = new GridBagConstraints();
			int i=0;
			c.fill = GridBagConstraints.BOTH;
			c.weighty=0;
			c.weightx=0; c.gridx=0; i=0;
			c.gridy=i++; panel.add(new JLabel("Center X: "),c);
			c.gridy=i++; panel.add(new JLabel("Center Y: "),c);
			c.gridy=i++; panel.add(new JLabel("Radius: "),c);
			c.gridy=i++; panel.add(new JLabel("Start Angle (deg): "),c);
			c.gridy=i++; panel.add(new JLabel("End Angle (deg): "),c);
			c.weightx=1; c.gridx=1; i=0;
			c.gridy=i++; panel.add(    cxField=createDoubleInput(arc.xC    , v->arc.xC    =v),c);
			c.gridy=i++; panel.add(    cyField=createDoubleInput(arc.yC    , v->arc.yC    =v),c);
			c.gridy=i++; panel.add(     rField=createDoubleInput(arc.r     , v->arc.r     =v, v->v>0),c);
			c.gridy=i++; panel.add(aStartField=createDoubleInput(arc.aStart*180/Math.PI, v->arc.aStart=v/180*Math.PI, v->v<=arc.aEnd  *180/Math.PI),c);
			c.gridy=i++; panel.add(  aEndField=createDoubleInput(arc.aEnd  *180/Math.PI, v->arc.aEnd  =v/180*Math.PI, v->v>=arc.aStart*180/Math.PI),c);
			c.weightx=0; c.gridx=2; i=0;
			c.gridy=i++; panel.add(createCheckBox("fixed",     isCxFixed, b->setFixed(    cxField,    isCxFixed=b)),c);
			c.gridy=i++; panel.add(createCheckBox("fixed",     isCyFixed, b->setFixed(    cyField,    isCyFixed=b)),c);
			c.gridy=i++; panel.add(createCheckBox("fixed",      isRFixed, b->setFixed(     rField,     isRFixed=b)),c);
			c.gridy=i++; panel.add(createCheckBox("fixed", isAStartFixed, b->setFixed(aStartField,isAStartFixed=b)),c);
			c.gridy=i++; panel.add(createCheckBox("fixed",   isAEndFixed, b->setFixed(  aEndField,  isAEndFixed=b)),c);
			c.weighty=1; c.weightx=1;
			c.gridx=0; c.gridy=i; c.gridwidth=3;
			panel.add(new JLabel(),c);
			return panel;
		}

		@Override protected Arc.ArcPoint getNext(int x, int y) {
			float maxDist = viewState.convertLength_ScreenToLength(EditorView.MAX_NEAR_DISTANCE);
			float xM = viewState.convertPos_ScreenToAngle_LongX(x);
			float yM = viewState.convertPos_ScreenToAngle_LatY (y);
			
			double xC = arc.xC;
			double yC = arc.yC;
			double xS = xC+arc.r*Math.cos(arc.aStart);
			double yS = yC+arc.r*Math.sin(arc.aStart);
			double xE = xC+arc.r*Math.cos(arc.aEnd  );
			double yE = yC+arc.r*Math.sin(arc.aEnd  );
			
			double dC = Math2.dist(xC, yC, xM, yM);
			double dS = Math2.dist(xS, yS, xM, yM);
			double dE = Math2.dist(xE, yE, xM, yM);
			if (dC<dS && dC<dE && dC<maxDist && (!isCxFixed || !isCyFixed)) return new Arc.ArcPoint(Arc.ArcPoint.Type.Center, xC,yC);
			if (dS<dE && dS<dC && dS<maxDist &&  !isAStartFixed) return new Arc.ArcPoint(Arc.ArcPoint.Type.Start , xS,yS);
			if (dE<dC && dE<dS && dE<maxDist &&  !isAEndFixed  ) return new Arc.ArcPoint(Arc.ArcPoint.Type.End   , xE,yE);
			
			if (Math.abs(dC-arc.r) < maxDist && !isRFixed) {
				double angle = Math2.angle(xC, yC, xM, yM);
				if (Math2.isInsideAngleRange(arc.aStart, arc.aEnd, angle)) {
					double xR = xC+arc.r*Math.cos(angle);
					double yR = yC+arc.r*Math.sin(angle);
					return new Arc.ArcPoint(Arc.ArcPoint.Type.Radius,xR,yR);
				}
			}
			return null;
		}
		
		@Override protected Point computePickOffset(int x, int y, ArcPoint selectedPoint) {
			if (selectedPoint.type==Type.Start || selectedPoint.type==Type.End) {
				glAngles = computeIntersectionPointsWithGuideLines(arc.xC,arc.yC,arc.r);
				maxGlAngle = viewState.convertLength_ScreenToLength(EditorView.MAX_GUIDELINE_DISTANCE)/arc.r;
				//System.out.printf(Locale.ENGLISH, "maxGlAngle: %1.4f (%1.2f°)%n", maxGlAngle, maxGlAngle*180/Math.PI);
				//System.out.printf(Locale.ENGLISH, "glAngles:%n");
				//System.out.printf(Locale.ENGLISH, "   %s%n", toString(glAngles, d->String.format(Locale.ENGLISH, "%1.4f", d            )));
				//System.out.printf(Locale.ENGLISH, "   %s%n", toString(glAngles, d->String.format(Locale.ENGLISH, "%1.2f°", d*180/Math.PI)));
			}
			int xs = viewState.convertPos_AngleToScreen_LongX((float) selectedPoint.x);
			int ys = viewState.convertPos_AngleToScreen_LatY ((float) selectedPoint.y);
			return new Point(xs-x, ys-y);
		}

		private double[] computeIntersectionPointsWithGuideLines(double xC, double yC, double r) {
			Vector<Double> anglesVec = new Vector<>();
			editorView.forEachGuideLines((type,pos)->{
				switch (type) {
				case Horizontal:
					if (yC-r<pos && pos<yC+r) {
						double a = Math.asin((pos-yC)/r);
						anglesVec.add(a);
						anglesVec.add(Math.PI-a);
					}
					break;
				case Vertical:
					if (xC-r<pos && pos<xC+r) {
						double a = Math.acos((pos-xC)/r);
						anglesVec.add( a);
						anglesVec.add(-a);
					}
					break;
				}
			});
			double[] anglesArr = anglesVec.stream().mapToDouble(d->d).toArray();
			for (int i=0; i<anglesArr.length; i++) {
				while (anglesArr[i] <  0        ) anglesArr[i] += Math.PI*2;
				while (anglesArr[i] >= Math.PI*2) anglesArr[i] -= Math.PI*2;
			}
			Arrays.sort(anglesArr);
			return anglesArr;
		}

		@SuppressWarnings("unused")
		private String toString(double[] values, DoubleFunction<? extends String> mapper) {
			return Arrays.toString(toStringArr(values, mapper));
		}
		private String[] toStringArr(double[] values, DoubleFunction<? extends String> mapper) {
			return Arrays.stream(values).mapToObj(mapper).toArray(n->new String[n]);
		}
		
		@Override protected void modifySelectedPoint(ArcPoint selectedPoint, int x, int y, Point pickOffset) {
			x+=pickOffset.x;
			y+=pickOffset.y;
			switch (selectedPoint.type) {
			case Radius:
				selectedPoint.x = viewState.convertPos_ScreenToAngle_LongX(x);
				selectedPoint.y = viewState.convertPos_ScreenToAngle_LatY (y);
				arc.r = Math2.dist(arc.xC, arc.yC, selectedPoint.x, selectedPoint.y);
				rField.setValue(arc.r);
				break;
			case Center:
				if (!isCxFixed) cxField.setValue(selectedPoint.x = arc.xC = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x)));
				if (!isCyFixed) cyField.setValue(selectedPoint.y = arc.yC = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y)));
				break;
			case End  : {
				double a = computeAngle(x,y); 
				while (a<arc.aStart          ) a+=Math.PI*2;
				while (a>arc.aStart+Math.PI*2) a-=Math.PI*2;
				//if (form.aEnd != a) System.out.printf(Locale.ENGLISH, "form.aEnd: %1.4f%n", a);
				arc.aEnd = a;
				aEndField.setValue(arc.aEnd);
			} break;
			case Start: {
				double a = computeAngle(x,y); 
				while (a>arc.aEnd          ) a-=Math.PI*2;
				while (a<arc.aEnd-Math.PI*2) a+=Math.PI*2;
				//if (form.aStart != a) System.out.printf(Locale.ENGLISH, "form.aStart: %1.4f%n", a);
				arc.aStart = a;
				aStartField.setValue(arc.aStart);
			} break;
			}
		}

		private double computeAngle(int x, int y) {
			float xM = viewState.convertPos_ScreenToAngle_LongX(x);
			float yM = viewState.convertPos_ScreenToAngle_LatY (y);
			double aM = Math2.angle(arc.xC, arc.yC, xM, yM);
			double aMinDist = Math.PI*2;
			Double aMin = null;
			double[] aDistArr = new double[glAngles.length];
			for (int i=0; i<glAngles.length; i++) {
				double a = glAngles[i];
				double aDist = Math.abs(Math2.computeAngleDist(a,aM));
				aDistArr[i] = aDist;
				if (aDist<maxGlAngle && (aMin==null || aDist<aMinDist)) {
					aMinDist = aDist;
					aMin = a;
				}
			}
			//System.out.printf(Locale.ENGLISH, "AngleDistances: %s%n", toString(aDistArr, d->String.format(Locale.ENGLISH, "%1.2f°", d*180/Math.PI)));
			if (aMin!=null) return aMin;
			return aM;
		}
	}

	static class PolyLineEditing extends LineFormEditing<Integer> {
		
		private final PolyLine polyLine;
		private boolean isXFixed;
		private boolean isYFixed;
		public PolyLineEditing(PolyLine polyLine, ViewState viewState, EditorView editorView, MouseEvent e) {
			super(polyLine, viewState, editorView);
			this.polyLine = polyLine;
			this.polyLine.setHighlightedPoint(e==null ? null : getNext(e.getX(),e.getY()));
		}
		
		@Override public JPanel createValuePanel() {
			JPanel panel = new JPanel(new GridBagLayout());
			panel.setBorder(BorderFactory.createTitledBorder("PolyLine Values"));
			// TODO: PolyLineEditing.createValuePanel
			return panel;
		}

		private void updateLine(Integer selectedPoint) {
			// TODO Auto-generated method stub
			
		}

		@Override protected Integer getNext(int x, int y) {
			float xu = viewState.convertPos_ScreenToAngle_LongX(x);
			float yu = viewState.convertPos_ScreenToAngle_LatY (y);
			float maxDist = viewState.convertLength_ScreenToLength(EditorView.MAX_NEAR_DISTANCE);
			
			Integer index = null;
			double minDist = 0;
			for (int i=0; i<polyLine.points.size(); i++) {
				net.schwarzbaer.image.alphachar.Form.PolyLine.Point p = polyLine.points.get(i);
				double d = Math2.dist(p.x, p.y, xu, yu);
				if (d<maxDist && (index==null || d<minDist)) {
					minDist = d;
					index = i;
				}
			}
			return index;
		}

		@Override
		protected Point computePickOffset(int x, int y, Integer selectedPoint) {
			net.schwarzbaer.image.alphachar.Form.PolyLine.Point p = polyLine.points.get(selectedPoint);
			int xs = viewState.convertPos_AngleToScreen_LongX((float) p.x);
			int ys = viewState.convertPos_AngleToScreen_LatY ((float) p.y);
			return new Point(xs-x, ys-y);
		}

		@Override protected void modifySelectedPoint(Integer selectedPoint, int x, int y, Point pickOffset) {
			net.schwarzbaer.image.alphachar.Form.PolyLine.Point p = polyLine.points.get(selectedPoint);
			if (!isXFixed) p.x = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x+pickOffset.x));
			if (!isYFixed) p.y = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y+pickOffset.y));
			updateLine(selectedPoint);
		}
		
	}

}