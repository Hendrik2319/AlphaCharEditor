package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.StandardMainWindow;

class MainWindow extends StandardMainWindow {
	private static final long serialVersionUID = 313126168052969131L;
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}

	MainWindow(String title) {
		super(title);
		
		EditorView editorView = new EditorView();
		editorView.setPreferredSize(500, 300);
		
		CharRaster charRaster = new CharRaster(ch->System.out.printf("Selected Char: %s%n", ch));
		charRaster.setPreferredSize(200, 300);
		
		JPanel leftPanel = new JPanel(new BorderLayout(3,3));
		leftPanel.add(charRaster,BorderLayout.CENTER);
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.add(leftPanel,BorderLayout.WEST);
		contentPane.add(editorView,BorderLayout.CENTER);
		
		JMenuBar menuBar = createMenuBar();
		
		startGUI(contentPane, menuBar);
		editorView.reset();
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fontMenu = menuBar.add(new JMenu("Font"));
		fontMenu.add(createMenuItem("Load Font ...",e->{}));
		fontMenu.add(createMenuItem("Load Default Font",e->loadDefaultFont()));
		fontMenu.add(createMenuItem("Save Font",e->{}));
		fontMenu.add(createMenuItem("Save Font As ...",e->{}));
		
		return menuBar;
	}

	private void loadDefaultFont() {
		// TODO Auto-generated method stub
	}

	private JMenuItem createMenuItem(String title, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	
	private static class CharRaster extends Canvas {
		private static final Color COLOR_TEXT = Color.BLACK;
		private static final Color COLOR_BACKGROUND = Color.WHITE;
		private static final Color COLOR_FIELD_BACKGROUND  = new Color(0xf0f0f0);
		private static final Color COLOR_FIELD_HIGHLIGHTED = Color.CYAN;
		private static final Color COLOR_FIELD_SELECTED = Color.GREEN;

		private static final long serialVersionUID = 6444819062135187504L;
		
		private final char[][] chars;
		private final int fieldWidth;
		private final int fieldHeight;

		private Rectangle view = null;
		private Point highlightedField = null;
		private Point selectedField = null;
		private Character selectedChar = null;
		private int[] rows;
		private SelectionListener listener;

		CharRaster(SelectionListener listener) {
			this.listener = listener;
			Assert(this.listener!=null);
			this.fieldWidth = 20;
			this.fieldHeight = 16;
			chars = new char[][] {
				createCharArray('A','Z'),
				createCharArray('a','z'),
				createCharArray('0','9'),
				new char[] { 'ä','ö','ü', 'Ä','Ö','Ü', 'ß' }
			};
			rows = new int[chars.length];
			Arrays.fill(rows,0);
			
			MouseAdapter m = new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) { setSelectedField   (e.getPoint()); }
				@Override public void mouseEntered(MouseEvent e) { setHighlightedField(e.getPoint()); }
				@Override public void mouseMoved  (MouseEvent e) { setHighlightedField(e.getPoint()); }
				@Override public void mouseExited (MouseEvent e) { setHighlightedField(null); }
			};
			addMouseListener(m);
			addMouseMotionListener(m);
		}
		
		interface SelectionListener {
			void selectedCharChanged(Character selectedChar);
		}
		
		private void setSelectedField(Point p) {
			Point field = getField(p);
			boolean repaint = false;
			if (field==null) {
				repaint = selectedField!=null;
				selectedField=null;
			} else if (selectedField==null || !selectedField.equals(field)) {
				repaint = true;
				selectedField = field; 
			}
			updateSelectedChar();
			if (repaint) repaint();
		}
		
		private void setHighlightedField(Point p) {
			Point field = getField(p);
			boolean repaint = false;
			if (field==null) {
				repaint = highlightedField!=null;
				highlightedField=null;
			} else if (highlightedField==null || !highlightedField.equals(field)) {
				repaint = true;
				highlightedField = field; 
			}
			if (repaint) repaint();
		}

		private Point getField(Point p) {
			if (p==null || view==null) return null;
			int x = p.x-view.x;
			int y = p.y-view.y;
			if (x<0 || x>=view.width ) return null;
			if (y<0 || y>=view.height) return null;
			int ix = x/fieldWidth;
			int iy = y/fieldHeight;
			return new Point(ix, iy); 
		}

		private void updateSelectedChar() {
			if (selectedField != null && view != null) {
				int block = -1;
				for (int i=0; i<rows.length; i++) {
					if (selectedField.y>=rows[i] && (i+1>=rows.length || selectedField.y<rows[i+1])) {
						block  = i;
						break;
					}
				}
				if (block>=0) {
					int y = selectedField.y-rows[block];
					int x = selectedField.x;
					int iWidth = view.width/fieldWidth;
					int i = x + iWidth*y;
					if (i<chars[block].length) {
						char ch = chars[block][i];
						if (selectedChar==null || !selectedChar.equals(ch)) {
							selectedChar = ch;
							listener.selectedCharChanged(selectedChar);
						}
						return;
					}
				}
			}
			if (selectedChar!=null) {
				selectedChar = null;
				listener.selectedCharChanged(selectedChar);
			}
		}

		private char[] createCharArray(char first, char last) {
			char[] chars = new char[last-first+1];
			for (int i=0; i<chars.length; ++i) chars[i] = (char) (first+i);
			return chars;
		}

		@Override
		protected void paintCanvas(Graphics g, int x, int y, int width, int height) {
			if (view==null) view = new Rectangle(x, y, width, height);
			else view.setBounds(x, y, width, height);
			g.setColor(COLOR_BACKGROUND);
			g.fillRect(x, y, width, height);
			
			if (g instanceof Graphics2D) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setClip(x, y, width, height);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				
				int iy = 0;
				for (int b=0; b<chars.length; b++) {
					char[] block = chars[b];
					int ix = 0;
					rows[b] = iy;
					for (int i=0; i<block.length; i++) {
						if ((ix+1)*fieldWidth>width) { ++iy; ix=0; }
						char ch = block[i];
						Color color;
						if (selectedField!=null && selectedField.x==ix && selectedField.y==iy)
							color = COLOR_FIELD_SELECTED;
						else if (highlightedField!=null && highlightedField.x==ix && highlightedField.y==iy)
							color = COLOR_FIELD_HIGHLIGHTED;
						else
							color = COLOR_FIELD_BACKGROUND;
						g2.setPaint(color);
						g2.fillRect(ix*fieldWidth+1, iy*fieldHeight+1, fieldWidth-2, fieldHeight-2);
						g2.setPaint(COLOR_TEXT);
						g2.drawString(Character.toString(ch), ix*fieldWidth+7, iy*fieldHeight+13);
						++ix;
					}
					++iy;
				}
				
			}
		}
		
	}
}
