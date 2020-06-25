package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.event.MouseEvent;

import net.schwarzbaer.java.tools.alphachareditor.LineForm.Arc;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Line;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.PolyLine;

abstract class LineFormEditing<FormType extends LineForm> {
	
	FormType form;
	LineFormEditing(FormType form) {
		this.form = form;
	}
	
	public abstract void onEntered (MouseEvent e);
	public abstract void onExited  (MouseEvent e);
	public abstract void onClicked (MouseEvent e);
	public abstract void onPressed (MouseEvent e);
	public abstract void onReleased(MouseEvent e);
	public abstract void onMoved   (MouseEvent e);
	public abstract void onDragged (MouseEvent e);

	static LineFormEditing<?> create(LineForm form) {
		if (form instanceof PolyLine) return new PolyLineEditing((PolyLine) form);
		if (form instanceof Line    ) return new     LineEditing((Line    ) form);
		if (form instanceof Arc     ) return new      ArcEditing((Arc     ) form);
		return null;
	}

	static class LineEditing extends LineFormEditing<Line    > {
		
		public LineEditing(Line form) {
			super(form);
		}
		
		@Override public void onExited  (MouseEvent e) {}
		@Override public void onMoved   (MouseEvent e) {}
		@Override public void onEntered (MouseEvent e) {}
		@Override public void onClicked (MouseEvent e) {}
		@Override public void onPressed (MouseEvent e) {}
		@Override public void onReleased(MouseEvent e) {}
		@Override public void onDragged (MouseEvent e) {}
	}
	
	static class PolyLineEditing extends LineFormEditing<PolyLine> {
		public PolyLineEditing(PolyLine form) {
			super(form);
		}
		@Override public void onExited  (MouseEvent e) {}
		@Override public void onMoved   (MouseEvent e) {}
		@Override public void onEntered (MouseEvent e) {}
		@Override public void onClicked (MouseEvent e) {}
		@Override public void onPressed (MouseEvent e) {}
		@Override public void onReleased(MouseEvent e) {}
		@Override public void onDragged (MouseEvent e) {}
	}
	
	static class ArcEditing extends LineFormEditing<Arc> {
		public ArcEditing(Arc form) {
			super(form);
		}
		@Override public void onExited  (MouseEvent e) {}
		@Override public void onMoved   (MouseEvent e) {}
		@Override public void onEntered (MouseEvent e) {}
		@Override public void onClicked (MouseEvent e) {}
		@Override public void onPressed (MouseEvent e) {}
		@Override public void onReleased(MouseEvent e) {}
		@Override public void onDragged (MouseEvent e) {}
	}

}