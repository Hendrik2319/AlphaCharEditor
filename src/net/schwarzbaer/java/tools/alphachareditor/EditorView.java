package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import net.schwarzbaer.gui.ZoomableCanvas;

class EditorView extends ZoomableCanvas<EditorView.ViewState> {
	private static final long serialVersionUID = -2936567438026759797L;
	
	EditorView() {
		Color color = new Color(0x70000000,true);
		activateMapScale(color, "px");
		activateAxes(color, true,true,true,true);
	}
	
	
	@Override
	protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
		g.setColor(Color.WHITE);
		g.fillRect(x, y, width, height);
		
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setClip(x, y, width, height);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			//g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			
			drawMapDecoration(g2, x, y, width, height);
			
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
}
