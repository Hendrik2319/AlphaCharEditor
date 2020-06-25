package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.event.MouseEvent;

import net.schwarzbaer.java.tools.alphachareditor.EditorView.ViewState;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Arc;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Line;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.Line.SelectedPoint;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.PolyLine;

abstract class LineFormEditing<FormType extends LineForm> {
	
	public final FormType form;
	protected final ViewState viewState;
	LineFormEditing(FormType form, ViewState viewState) {
		this.form = form;
		this.viewState = viewState;
	}
	
	public abstract void onEntered (MouseEvent e);
	public abstract void onExited  (MouseEvent e);
	public abstract void onClicked (MouseEvent e);
	public abstract void onMoved   (MouseEvent e);
	public abstract boolean onPressed (MouseEvent e);
	public abstract boolean onReleased(MouseEvent e);
	public abstract boolean onDragged (MouseEvent e);

	static LineFormEditing<?> create(LineForm form, ViewState viewState) {
		if (form instanceof PolyLine) return new PolyLineEditing((PolyLine) form, viewState);
		if (form instanceof Line    ) return new     LineEditing((Line    ) form, viewState);
		if (form instanceof Arc     ) return new      ArcEditing((Arc     ) form, viewState);
		return null;
	}

	static class LineEditing extends LineFormEditing<Line    > {
		
		private int x1s,y1s,x2s,y2s;
		private int moveOffsetX = -1;
		private int moveOffsetY = -1;
		public LineEditing(Line form, ViewState viewState) {
			super(form, viewState);
			x1s = this.viewState.convertPos_AngleToScreen_LongX((float) this.form.x1);
			y1s = this.viewState.convertPos_AngleToScreen_LatY ((float) this.form.y1);
			x2s = this.viewState.convertPos_AngleToScreen_LongX((float) this.form.x2);
			y2s = this.viewState.convertPos_AngleToScreen_LatY ((float) this.form.y2);
		}
		
		@Override public boolean onPressed (MouseEvent e) {
			if (e.getButton()!=MouseEvent.BUTTON1) return false;
			int x = e.getX();
			int y = e.getY();
			double d1 = Math2.dist(x1s, y1s, x, y);
			double d2 = Math2.dist(x1s, y1s, x, y);
			if (d1<d2 && d1<EditorView.MAX_NEAR_DISTANCE) {
				form.selectedPoint = SelectedPoint.P1;
				moveOffsetX = x1s-x;
				moveOffsetY = y1s-y;
				return true;
			}
			if (d2<d1 && d2<EditorView.MAX_NEAR_DISTANCE) {
				form.selectedPoint = SelectedPoint.P2;
				moveOffsetX = x2s-x;
				moveOffsetY = y2s-y;
				return true;
			}
			form.selectedPoint = null;
			moveOffsetX = -1;
			moveOffsetY = -1;
			return false;
		}
		@Override public boolean onReleased(MouseEvent e) {
			form.selectedPoint = null;
			moveOffsetX = -1;
			moveOffsetY = -1;
			return false;
		}
		@Override public void onClicked (MouseEvent e) {  }
		@Override public void onEntered (MouseEvent e) {  }
		@Override public void onExited  (MouseEvent e) {  }
		@Override public void onMoved   (MouseEvent e) {  }
		
		@Override public boolean onDragged (MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			if (form.selectedPoint!=null) {
				e.consume();
				switch (form.selectedPoint) {
				case P1:
					form.x1 = viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX);
					form.y1 = viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY);
					x1s = viewState.convertPos_AngleToScreen_LongX((float) form.x1);
					y1s = viewState.convertPos_AngleToScreen_LatY ((float) form.y1);
					break;
				case P2:
					form.x2 = viewState.convertPos_ScreenToAngle_LongX(x+moveOffsetX);
					form.y2 = viewState.convertPos_ScreenToAngle_LatY (y+moveOffsetY);
					x2s = viewState.convertPos_AngleToScreen_LongX((float) form.x2);
					y2s = viewState.convertPos_AngleToScreen_LatY ((float) form.y2);
					break;
				}
				return true;
			}
			return false;
		}
	}
	
	static class PolyLineEditing extends LineFormEditing<PolyLine> {
		public PolyLineEditing(PolyLine form, ViewState viewState) {
			super(form, viewState);
		}
		@Override public void onExited  (MouseEvent e) {}
		@Override public void onMoved   (MouseEvent e) {}
		@Override public void onEntered (MouseEvent e) {}
		@Override public void onClicked (MouseEvent e) {}
		@Override public boolean onPressed (MouseEvent e) { return false; }
		@Override public boolean onReleased(MouseEvent e) { return false; }
		@Override public boolean onDragged (MouseEvent e) { return false; }
	}
	
	static class ArcEditing extends LineFormEditing<Arc> {
		public ArcEditing(Arc form, ViewState viewState) {
			super(form, viewState);
		}
		@Override public void onExited  (MouseEvent e) {}
		@Override public void onMoved   (MouseEvent e) {}
		@Override public void onEntered (MouseEvent e) {}
		@Override public void onClicked (MouseEvent e) {}
		@Override public boolean onPressed (MouseEvent e) { return false; }
		@Override public boolean onReleased(MouseEvent e) { return false; }
		@Override public boolean onDragged (MouseEvent e) { return false; }
	}

}