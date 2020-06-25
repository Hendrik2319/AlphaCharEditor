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
			super(toString.apply(value));
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
			this.form.highlightedPoint = getNext(e.getX(),e.getY());
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

		private void setFixed(GenericTextField<Double> field, boolean isFixed) {
			field.setEditable(!isFixed);
			//field.setEnabled (!isFixed);
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
					if (!isX1Fixed) x1Field.setValue(form.x1 = viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX));
					if (!isY1Fixed) y1Field.setValue(form.y1 = viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY));
					break;
				case P2:
					if (!isX2Fixed) x2Field.setValue(form.x2 = viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX));
					if (!isY2Fixed) y2Field.setValue(form.y2 = viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY));
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
	
	static class PolyLineEditing extends LineFormEditing<PolyLine> {
		public PolyLineEditing(PolyLine form, ViewState viewState, EditorView editorView, MouseEvent e) {
			super(form, viewState, editorView);
		}
		@Override public JPanel createValuePanel() { return null; }
		@Override public void onExited  (MouseEvent e) {}
		@Override public void onMoved   (MouseEvent e) {}
		@Override public void onEntered (MouseEvent e) {}
		@Override public boolean onPressed (MouseEvent e) { return false; }
		@Override public boolean onReleased(MouseEvent e) { return false; }
		@Override public boolean onDragged (MouseEvent e) { return false; }
	}
	
	static class ArcEditing extends LineFormEditing<Arc> {
		public ArcEditing(Arc form, ViewState viewState, EditorView editorView, MouseEvent e) {
			super(form, viewState, editorView);
		}
		@Override public JPanel createValuePanel() { return null; }
		@Override public void onExited  (MouseEvent e) {}
		@Override public void onMoved   (MouseEvent e) {}
		@Override public void onEntered (MouseEvent e) {}
		@Override public boolean onPressed (MouseEvent e) { return false; }
		@Override public boolean onReleased(MouseEvent e) { return false; }
		@Override public boolean onDragged (MouseEvent e) { return false; }
	}

}