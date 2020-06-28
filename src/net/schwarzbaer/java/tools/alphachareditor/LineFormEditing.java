package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
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

abstract class LineFormEditing<FormType extends LineForm> {
	
	public final FormType form;
	protected final ViewState viewState;
	protected final EditorView editorView;
	LineFormEditing(FormType form, ViewState viewState, EditorView editorView) {
		this.form = form;
		this.viewState = viewState;
		this.editorView = editorView;
	}
	
	public abstract JPanel createValuePanel();
	public abstract void onEntered (MouseEvent e);
	public abstract void onExited  (MouseEvent e);
	public abstract void onMoved   (MouseEvent e);
	public abstract boolean onPressed (MouseEvent e);
	public abstract boolean onReleased(MouseEvent e);
	public abstract boolean onDragged (MouseEvent e);

	static LineFormEditing<?> create(LineForm form, ViewState viewState, EditorView editorView, MouseEvent e) {
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

	static class LineEditing extends LineFormEditing<Line    > {
		
		private int moveOffsetX = -1;
		private int moveOffsetY = -1;
		private int x1s,y1s,x2s,y2s;
		private GenericTextField<Double> x1Field = null;
		private GenericTextField<Double> y1Field = null;
		private GenericTextField<Double> x2Field = null;
		private GenericTextField<Double> y2Field = null;
		private boolean isX1Fixed = false;
		private boolean isY1Fixed = false;
		private boolean isX2Fixed = false;
		private boolean isY2Fixed = false;
		
		public LineEditing(Line form, ViewState viewState, EditorView editorView, MouseEvent e) {
			super(form, viewState, editorView);
			this.form.highlightedPoint = e==null ? null : getNext(e.getX(),e.getY());
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
			c.gridy=i++; panel.add(x1Field=createDoubleInput(form.x1, v->form.x1=v),c);
			c.gridy=i++; panel.add(y1Field=createDoubleInput(form.y1, v->form.y1=v),c);
			c.gridy=i++; panel.add(x2Field=createDoubleInput(form.x2, v->form.x2=v),c);
			c.gridy=i++; panel.add(y2Field=createDoubleInput(form.y2, v->form.y2=v),c);
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

		private SelectedPoint getNext(int x, int y) {
			x1s = viewState.convertPos_AngleToScreen_LongX((float) form.x1);
			y1s = viewState.convertPos_AngleToScreen_LatY ((float) form.y1);
			x2s = viewState.convertPos_AngleToScreen_LongX((float) form.x2);
			y2s = viewState.convertPos_AngleToScreen_LatY ((float) form.y2);
			double d1 = Math2.dist(x1s, y1s, x, y);
			double d2 = Math2.dist(x2s, y2s, x, y);
			if (d1<d2 && d1<EditorView.MAX_NEAR_DISTANCE) return SelectedPoint.P1;
			if (d2<d1 && d2<EditorView.MAX_NEAR_DISTANCE) return SelectedPoint.P2;
			return null;
		}
		
		@Override public boolean onPressed (MouseEvent e) {
			if (e.getButton()!=MouseEvent.BUTTON1) return false;
			int x = e.getX();
			int y = e.getY();
			form.selectedPoint = getNext(x,y);
			if (form.selectedPoint!=null) {
				switch (form.selectedPoint) {
				case P1:
					moveOffsetX = x1s-x;
					moveOffsetY = y1s-y;
					break;
				case P2:
					moveOffsetX = x2s-x;
					moveOffsetY = y2s-y;
					break;
				}
				editorView.repaint();
				return true;
			}
			moveOffsetX = -1;
			moveOffsetY = -1;
			editorView.repaint();
			return false;
		}
		@Override public boolean onReleased(MouseEvent e) {
			form.selectedPoint = null;
			moveOffsetX = -1;
			moveOffsetY = -1;
			editorView.repaint();
			return false;
		}
		@Override public boolean onDragged (MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			if (form.selectedPoint!=null) {
				switch (form.selectedPoint) {
				case P1:
					if (!isX1Fixed) x1Field.setValue(form.x1 = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX)));
					if (!isY1Fixed) y1Field.setValue(form.y1 = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY)));
					break;
				case P2:
					if (!isX2Fixed) x2Field.setValue(form.x2 = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX)));
					if (!isY2Fixed) y2Field.setValue(form.y2 = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY)));
					break;
				}
				editorView.repaint();
				return true;
			}
			editorView.repaint();
			return false;
		}
		
		@Override public void onEntered (MouseEvent e) { form.highlightedPoint = getNext(e.getX(),e.getY()); editorView.repaint(); }
		@Override public void onMoved   (MouseEvent e) { form.highlightedPoint = getNext(e.getX(),e.getY()); editorView.repaint(); }
		@Override public void onExited  (MouseEvent e) { form.highlightedPoint = null; editorView.repaint(); }
	}
	
	static class ArcEditing extends LineFormEditing<Arc> {
		
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
		
		private int moveOffsetX = -1;
		private int moveOffsetY = -1;
		private ArcPoint selectedPoint = null;
		
		public ArcEditing(Arc form, ViewState viewState, EditorView editorView, MouseEvent e) {
			super(form, viewState, editorView);
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
			c.gridy=i++; panel.add(    cxField=createDoubleInput(form.xC    , v->form.xC    =v),c);
			c.gridy=i++; panel.add(    cyField=createDoubleInput(form.yC    , v->form.yC    =v),c);
			c.gridy=i++; panel.add(     rField=createDoubleInput(form.r     , v->form.r     =v, v->v>0),c);
			c.gridy=i++; panel.add(aStartField=createDoubleInput(form.aStart*180/Math.PI, v->form.aStart=v/180*Math.PI, v->v<=form.aEnd  *180/Math.PI),c);
			c.gridy=i++; panel.add(  aEndField=createDoubleInput(form.aEnd  *180/Math.PI, v->form.aEnd  =v/180*Math.PI, v->v>=form.aStart*180/Math.PI),c);
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

		private Arc.ArcPoint getNext(int x, int y) {
			float maxDist = viewState.convertLength_ScreenToLength(EditorView.MAX_NEAR_DISTANCE);
			float xM = viewState.convertPos_ScreenToAngle_LongX(x);
			float yM = viewState.convertPos_ScreenToAngle_LatY (y);
			
			double xC = form.xC;
			double yC = form.yC;
			double xS = xC+form.r*Math.cos(form.aStart);
			double yS = yC+form.r*Math.sin(form.aStart);
			double xE = xC+form.r*Math.cos(form.aEnd  );
			double yE = yC+form.r*Math.sin(form.aEnd  );
			
			double dC = Math2.dist(xC, yC, xM, yM);
			double dS = Math2.dist(xS, yS, xM, yM);
			double dE = Math2.dist(xE, yE, xM, yM);
			if (dC<dS && dC<dE && dC<maxDist) return new Arc.ArcPoint(Arc.ArcPoint.Type.Center, xC,yC);
			if (dS<dE && dS<dC && dS<maxDist) return new Arc.ArcPoint(Arc.ArcPoint.Type.Start , xS,yS);
			if (dE<dC && dE<dS && dE<maxDist) return new Arc.ArcPoint(Arc.ArcPoint.Type.End   , xE,yE);
			
			if (Math.abs(dC-form.r) < maxDist) {
				double angle = Math2.angle(xC, yC, xM, yM);
				if (Math2.isInsideAngleRange(form.aStart, form.aEnd, angle)) {
					double xR = xC+form.r*Math.cos(angle);
					double yR = yC+form.r*Math.sin(angle);
					return new Arc.ArcPoint(Arc.ArcPoint.Type.Radius,xR,yR);
				}
			}
			return null;
		}
		
		@Override public void onEntered (MouseEvent e) { form.highlightedPoint = getNext(e.getX(),e.getY()); editorView.repaint(); }
		@Override public void onMoved   (MouseEvent e) { form.highlightedPoint = getNext(e.getX(),e.getY()); editorView.repaint(); }
		@Override public void onExited  (MouseEvent e) { form.highlightedPoint = null; editorView.repaint(); }
		
		@Override public boolean onPressed (MouseEvent e) {
			if (e.getButton()!=MouseEvent.BUTTON1) return false;
			int x = e.getX();
			int y = e.getY();
			selectedPoint = getNext(x,y);
			if (selectedPoint!=null) {
				if (selectedPoint.type==Type.Start || selectedPoint.type==Type.End) {
					// TODO: compute intersection points with guide lines
				}
				int xs = viewState.convertPos_AngleToScreen_LongX((float) selectedPoint.x);
				int ys = viewState.convertPos_AngleToScreen_LatY ((float) selectedPoint.y);
				moveOffsetX = xs-x;
				moveOffsetY = ys-y;
				editorView.repaint();
				return true;
			}
			moveOffsetX = -1;
			moveOffsetY = -1;
			editorView.repaint();
			return false;
		}
		@Override public boolean onReleased(MouseEvent e) {
			selectedPoint = null;
			moveOffsetX = -1;
			moveOffsetY = -1;
			editorView.repaint();
			return false;
		}
		@Override public boolean onDragged (MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			if (selectedPoint!=null) {
				switch (selectedPoint.type) {
				case Radius: break; // TODO
				case Center:
					if (!isCxFixed) cxField.setValue(form.xC = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX)));
					if (!isCyFixed) cyField.setValue(form.yC = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY)));
					break;
				case End  : break; // TODO
				case Start: break; // TODO
//				case P1:
//					if (!isX1Fixed) x1Field.setValue(form.x1 = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX)));
//					if (!isY1Fixed) y1Field.setValue(form.y1 = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY)));
//					break;
//				case P2:
//					if (!isX2Fixed) x2Field.setValue(form.x2 = editorView.stickToGuideLineX(viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX)));
//					if (!isY2Fixed) y2Field.setValue(form.y2 = editorView.stickToGuideLineY(viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY)));
//					break;
				}
				editorView.repaint();
				return true;
			}
			editorView.repaint();
			return false;
		}
	}

	static class PolyLineEditing extends LineFormEditing<PolyLine> {
		public PolyLineEditing(PolyLine form, ViewState viewState, EditorView editorView, MouseEvent e) {
			super(form, viewState, editorView);
		}
		
		@Override public JPanel createValuePanel() {
			JPanel panel = new JPanel(new GridBagLayout());
			panel.setBorder(BorderFactory.createTitledBorder("PolyLine Values"));
			// TODO
			return panel;
		}
		
		@Override public void onExited  (MouseEvent e) {}
		@Override public void onMoved   (MouseEvent e) {}
		@Override public void onEntered (MouseEvent e) {}
		@Override public boolean onPressed (MouseEvent e) { return false; }
		@Override public boolean onReleased(MouseEvent e) { return false; }
		@Override public boolean onDragged (MouseEvent e) { return false; }
	}

}