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
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.image.alphachar.Form;

class MainWindow extends StandardMainWindow {
	private static final long serialVersionUID = 313126168052969131L;
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}
	
	private AlphaCharEditor alphaCharEditor;
	private CharRaster charRaster;
	private EditorView editorView;
	private Character selectedChar;
	private JComponent valuePanel = null;

	MainWindow(AlphaCharEditor alphaCharEditor, String title) {
		super(title);
		this.alphaCharEditor = alphaCharEditor;
		
		editorView = new EditorView();
		editorView.setPreferredSize(500, 300);
		JPanel editorViewPanel = new JPanel(new BorderLayout(3,3));
		editorViewPanel.setBorder(BorderFactory.createTitledBorder("Geometry"));
		editorViewPanel.add(editorView,BorderLayout.CENTER);
		
		selectedChar = null;
		charRaster = new CharRaster(ch->{
			selectedChar=ch;
			Form[] forms = this.alphaCharEditor.font==null ? null : this.alphaCharEditor.font.get(selectedChar);
			System.out.printf("SelectedChar: %s %s%n", selectedChar==null ? "none" : "'"+selectedChar+"'", forms==null ? "--" : "["+forms.length+"]");
			editorView.setForms(forms);
		});
		JPanel charRasterPanel = new JPanel(new BorderLayout(3,3));
		charRasterPanel.setBorder(BorderFactory.createTitledBorder("Characters"));
		charRasterPanel.add(charRaster,BorderLayout.CENTER);
		
		JPanel leftPanel = new JPanel(new BorderLayout(3,3));
		leftPanel.add(charRasterPanel,BorderLayout.NORTH);
		leftPanel.add(valuePanel = new JLabel(),BorderLayout.CENTER);
		editorView.setValuePanelChangeFcn(newPanel->{
			if (valuePanel!=null) leftPanel.remove(valuePanel);
			valuePanel = newPanel;
			if (valuePanel!=null) leftPanel.add(valuePanel,BorderLayout.CENTER);
			leftPanel.revalidate();
			leftPanel.repaint();
		});
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.add(leftPanel,BorderLayout.WEST);
		contentPane.add(editorViewPanel,BorderLayout.CENTER);
		
		JMenuBar menuBar = createMenuBar();
		
		startGUI(contentPane, menuBar);
		editorView.reset();
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fontMenu = menuBar.add(new JMenu("Font"));
		fontMenu.add(createMenuItem("Load Font ...",e->{}));
		fontMenu.add(createMenuItem("Load Default Font",e->{ alphaCharEditor.loadDefaultFont(editorView.getViewState()); updateAfterFontLoad(); }));
		fontMenu.add(createMenuItem("Save Font",e->{}));
		fontMenu.add(createMenuItem("Save Font As ...",e->{}));
		
		return menuBar;
	}

	private void updateAfterFontLoad() {
		charRaster.updateCharList(alphaCharEditor.font);
	}

	private JMenuItem createMenuItem(String title, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	
	private static class CharRaster extends Canvas {
		private static final Color COLOR_TEXT           = Color.BLACK;
		private static final Color COLOR_TEXT_NOTEXISTS = Color.LIGHT_GRAY;
		private static final Color COLOR_BACKGROUND     = Color.WHITE;
		private static final Color COLOR_CHAR_EXISTS      = new Color(0xf0f0f0);
		private static final Color COLOR_CHAR_HIGHLIGHTED = Color.CYAN;
		private static final Color COLOR_CHAR_SELECTED    = Color.GREEN;

		private static final long serialVersionUID = 6444819062135187504L;
		
		private final char[][] chars;
		private final boolean[][] charExist;
		private final int fieldWidth;
		private final int fieldHeight;
		private final int offsetX;
		private final int offsetY;
		private final int border;

		private Rectangle view = null;
		private Point highlightedField = null;
		private Point selectedField = null;
		private Character selectedChar = null;
		private int[] rows;
		private SelectionListener listener;

		CharRaster(SelectionListener listener) {
			this.listener = listener;
			Assert(this.listener!=null);
			this.fieldWidth  = 20;
			this.fieldHeight = 18;
			this.offsetX =  7;
			this.offsetY = 13;
			this.border = 3;
			chars = new char[][] {
				createCharArray('A','Z'),
				createCharArray('a','z'),
				createCharArray('0','9'),
				new char[] { 'ä','ö','ü', 'Ä','Ö','Ü', 'ß' }
			};
			
			rows = new int[chars.length];
			Arrays.fill(rows,0);
			
			charExist = new boolean[chars.length][];
			for (int i=0; i<charExist.length; i++) {
				charExist[i] = new boolean[chars[i].length];
				Arrays.fill(charExist[i],false);
			}
			
			MouseAdapter m = new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) { setSelectedField   (e.getPoint()); }
				@Override public void mouseEntered(MouseEvent e) { setHighlightedField(e.getPoint()); }
				@Override public void mouseMoved  (MouseEvent e) { setHighlightedField(e.getPoint()); }
				@Override public void mouseExited (MouseEvent e) { setHighlightedField(null); }
			};
			addMouseListener(m);
			addMouseMotionListener(m);
			
			setPreferredSize(2*border + 10*fieldWidth, 2*border +  8*fieldHeight);
		}

		public void updateCharList(HashMap<Character, Form[]> font) {
			for (int b=0; b<charExist.length; b++) {
				for (int ch=0; ch<charExist[b].length; ch++) {
					Form[] forms = font==null ? null : font.get(chars[b][ch]);
					charExist[b][ch] = forms!=null;
				}
			}
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
					int ix = 0;
					rows[b] = iy;
					for (int i=0; i<chars[b].length; i++) {
						if ((ix+1)*fieldWidth>width-2*border) { ++iy; ix=0; }
						char ch = chars[b][i];
						boolean exist = charExist[b][i];
						Color color;
						if (selectedField!=null && selectedField.x==ix && selectedField.y==iy)
							color = COLOR_CHAR_SELECTED;
						else if (highlightedField!=null && highlightedField.x==ix && highlightedField.y==iy)
							color = COLOR_CHAR_HIGHLIGHTED;
						else
							color = exist ? COLOR_CHAR_EXISTS : COLOR_BACKGROUND;
						g2.setPaint(color);
						g2.fillRect(border+ix*fieldWidth+1, border+iy*fieldHeight+1, fieldWidth-2, fieldHeight-2);
						g2.setPaint(exist ? COLOR_TEXT : COLOR_TEXT_NOTEXISTS);
						g2.drawString(Character.toString(ch), border+ix*fieldWidth+offsetX, border+iy*fieldHeight+offsetY);
						++ix;
					}
					++iy;
				}
				
			}
		}
		
	}
}
