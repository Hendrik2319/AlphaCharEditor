package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.image.alphachar.Form;
import net.schwarzbaer.java.tools.alphachareditor.EditorView.GuideLine;

class MainWindow extends StandardMainWindow {
	private static final long serialVersionUID = 313126168052969131L;
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}
	
	private AlphaCharEditor alphaCharEditor;
	private CharRaster charRaster;
	private EditorView editorView;
	private Character selectedChar;
	private JComponent valuePanel;
	private GeneralOptionPanel generalOptionPanel;

	MainWindow(AlphaCharEditor alphaCharEditor, String title) {
		super(title);
		this.alphaCharEditor = alphaCharEditor;
		
		editorView = new EditorView();
		editorView.setPreferredSize(500, 300);
		editorView.setGuideLines(this.alphaCharEditor.project.guideLines);
		
		JPanel editorViewPanel = new JPanel(new BorderLayout(3,3));
		editorViewPanel.setBorder(BorderFactory.createTitledBorder("Geometry"));
		editorViewPanel.add(editorView,BorderLayout.CENTER);
		
		selectedChar = null;
		charRaster = new CharRaster(ch->{
			selectedChar=ch;
			Form[] forms = this.alphaCharEditor.project.font==null ? null : this.alphaCharEditor.project.font.get(selectedChar);
			System.out.printf("SelectedChar: %s %s%n", selectedChar==null ? "none" : "'"+selectedChar+"'", forms==null ? "--" : "["+forms.length+"]");
			LineForm[] lineforms = null;
			if (forms != null) {
				lineforms = new LineForm[forms.length];
				for (int i=0; i<forms.length; i++) {
					Form form = forms[i];
					Assert(form instanceof LineForm);
					lineforms[i] = (LineForm) form;
				}
			}
			editorView.setForms(lineforms);
			generalOptionPanel.setForms(lineforms);
		});
		JPanel charRasterPanel = new JPanel(new BorderLayout(3,3));
		charRasterPanel.setBorder(BorderFactory.createTitledBorder("Characters"));
		charRasterPanel.add(charRaster,BorderLayout.CENTER);
		
		generalOptionPanel = new GeneralOptionPanel();
		generalOptionPanel.setPreferredSize(new Dimension(200, 200));
		generalOptionPanel.setGuideLines(this.alphaCharEditor.project.guideLines);
		
		JPanel leftPanel = new JPanel(new BorderLayout(3,3));
		leftPanel.add(charRasterPanel,BorderLayout.NORTH);
		leftPanel.add(valuePanel = generalOptionPanel,BorderLayout.CENTER);
		
		editorView.setValuePanelChangeFcn(newPanel->{
			if (valuePanel!=null) leftPanel.remove(valuePanel);
			if (newPanel!=null)
				valuePanel = newPanel;
			else {
				valuePanel = generalOptionPanel;
				generalOptionPanel.updateContent();
			}
			if (valuePanel!=null) leftPanel.add(valuePanel,BorderLayout.CENTER);
			leftPanel.revalidate();
			leftPanel.repaint();
		});
		editorView.setUpdateSelectedInFormListFcn(generalOptionPanel::setSelectedForm);
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.add(leftPanel,BorderLayout.WEST);
		contentPane.add(editorViewPanel,BorderLayout.CENTER);
		
		JMenuBar menuBar = createMenuBar();
		
		startGUI(contentPane, menuBar);
		editorView.reset();
	}
	
	private class GeneralOptionPanel extends JPanel {

		private static final long serialVersionUID = -2024771038202756837L;
		
		private SubPanel subPanel;
		private FormsPanel formsPanel;
		private GuideLinesPanel guideLinesPanel;
		
		GeneralOptionPanel() {
			super(new BorderLayout(3,3));
			setBorder(BorderFactory.createTitledBorder("General"));
			
			formsPanel = new FormsPanel();
			guideLinesPanel = new GuideLinesPanel();
			
			ButtonGroup bg = new ButtonGroup();
			JPanel buttonPanel = new JPanel(new GridLayout(1,0,3,3));
			buttonPanel.add(createToggleButton("Forms"      , bg, true , e->setSubPanel(     formsPanel)));
			buttonPanel.add(createToggleButton("Guide Lines", bg, false, e->setSubPanel(guideLinesPanel)));
			
			add(buttonPanel,BorderLayout.NORTH);
			add(subPanel = formsPanel,BorderLayout.CENTER);
		}
		
		public void setGuideLines(Vector<GuideLine> guideLines) {
			guideLinesPanel.setGuideLines(guideLines);
		}

		void setSelectedForm(LineForm form) {
			formsPanel.setSelected(form);
		}

		void updateContent() {
			formsPanel.updateContent();
			guideLinesPanel.updateContent();
		}

		void setForms(LineForm[] forms) {
			formsPanel.setForms(forms);
		}

		private void setSubPanel(SubPanel newSubPanel) {
			if (subPanel!=null) remove(subPanel);
			subPanel = newSubPanel;
			if (subPanel!=null) {
				subPanel.updateContent();
				add(subPanel,BorderLayout.CENTER);
			}
			revalidate();
			repaint();
		}
		
		private abstract class SubPanel extends JPanel {
			private static final long serialVersionUID = 644740467923572485L;
			protected SubPanel(LayoutManager layout) { super(layout); }
			abstract void updateContent();
		}

		private class FormsPanel extends SubPanel {
			private static final long serialVersionUID = 5266768936706086790L;
			private final JList<LineForm> formList;
			@SuppressWarnings("unused")
			private final JButton btnNew;
			private final JButton btnEdit;
			@SuppressWarnings("unused")
			private final JButton btnRemove;

			FormsPanel() {
				super(new BorderLayout(3,3));
				
				formList = new JList<LineForm>();
				formList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				formList.addListSelectionListener(e->{
					LineForm selectedValue = formList.getSelectedValue();
					editorView.setHighlightedForm(selectedValue);
					setButtonsEnabled(selectedValue!=null);
				});
				
				JScrollPane formListScrollPane = new JScrollPane(formList);
				
				JPanel buttonPanel = new JPanel(new GridBagLayout());
				//GridBagConstraints c = new GridBagConstraints();
				buttonPanel.add(btnNew    = createButton("New"   , false, e->{}));
				buttonPanel.add(btnEdit   = createButton("Edit"  , false, e->editorView.setSelectedForm(formList.getSelectedValue())));
				buttonPanel.add(btnRemove = createButton("Remove", false, e->{}));
				
				add(formListScrollPane,BorderLayout.CENTER);
				add(buttonPanel,BorderLayout.SOUTH);
			}

			private void setButtonsEnabled(boolean enabled) {
				//btnNew   .setEnabled(enabled);
				btnEdit  .setEnabled(enabled);
				//btnRemove.setEnabled(enabled);
			}

			void setSelected(LineForm form) {
				if (form==null) formList.clearSelection();
				else formList.setSelectedValue(form, true);
				setButtonsEnabled(form!=null);
			}

			void setForms(LineForm[] forms) {
				formList.setModel(new FormListModel(forms));
			}

			@Override
			void updateContent() {
				// TODO Auto-generated method stub
			}

			private final class FormListModel implements ListModel<LineForm> {
				private LineForm[] forms;
				private Vector<ListDataListener> listDataListeners;
				
				public FormListModel(LineForm[] forms) {
					this.forms = forms;
					listDataListeners = new Vector<>();
				}

				@Override public int getSize() { return forms==null ? 0 : forms.length; }
				@Override public LineForm getElementAt(int index) {
					if (forms==null || index<0 || index>=forms.length) return null;
					return forms[index];
				}
			
				@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
				@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l);}
			}
			
		}
		
		private class GuideLinesPanel extends SubPanel {
			private static final long serialVersionUID = -2804258616332267816L;
			private final JList<GuideLine> guideLineList;
			@SuppressWarnings("unused")
			private final JButton btnNew;
			private final JButton btnEdit;
			@SuppressWarnings("unused")
			private final JButton btnRemove;

			GuideLinesPanel() {
				super(new BorderLayout(3,3));
				
				guideLineList = new JList<GuideLine>();
				guideLineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				guideLineList.addListSelectionListener(e->{
					GuideLine selected = guideLineList.getSelectedValue();
					setButtonsEnabled(selected!=null);
					editorView.setHighlightedGuideLine(selected);
				});
				
				JScrollPane guideLineListScrollPane = new JScrollPane(guideLineList);
				
				JPanel buttonPanel = new JPanel(new GridBagLayout());
				//GridBagConstraints c = new GridBagConstraints();
				buttonPanel.add(btnNew    = createButton("New"   , false, e->{}));
				buttonPanel.add(btnEdit   = createButton("Edit"  , false, e->editGuideLine(guideLineList.getSelectedValue())));
				buttonPanel.add(btnRemove = createButton("Remove", false, e->{}));
				
				add(guideLineListScrollPane,BorderLayout.CENTER);
				add(buttonPanel,BorderLayout.SOUTH);
			}

			private void editGuideLine(GuideLine selected) {
				if (selected==null) return;
				
				String message = String.format("Set %s position of %s guideline:", selected.type.axis, selected.type.toString().toLowerCase());
				String newStr = JOptionPane.showInputDialog(this, message, selected.pos);
				if (newStr==null) return;
				
				try {
					selected.pos = Float.parseFloat(newStr);
					editorView.repaint();
					guideLineList.repaint();
				} catch (NumberFormatException e) {
					message = String.format("Can't parse \"%s\" as numeric value.", newStr);
					JOptionPane.showMessageDialog(this, message, "Wrong input", JOptionPane.ERROR_MESSAGE);
				}
			}

			private void setButtonsEnabled(boolean enabled) {
				//btnNew   .setEnabled(enabled);
				btnEdit  .setEnabled(enabled);
				//btnRemove.setEnabled(enabled);
			}

			void setGuideLines(Vector<GuideLine> guideLines) {
				guideLineList.setModel(new GuideLineListModel(guideLines));
			}

			@Override
			void updateContent() {
				// TODO Auto-generated method stub
			}
			
			private final class GuideLineListModel implements ListModel<GuideLine> {
				private Vector<ListDataListener> listDataListeners;
				private Vector<GuideLine> guideLines;

				public GuideLineListModel(Vector<GuideLine> guideLines) {
					this.guideLines = guideLines;
					listDataListeners = new Vector<>();
				}

				@Override public int getSize() { return guideLines==null ? 0 : guideLines.size(); }
				@Override public GuideLine getElementAt(int index) {
					if (guideLines==null || index<0 || index>=guideLines.size()) return null;
					return guideLines.get(index);
				}
			
				@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
				@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l);}
			}
		}
	}

	private JToggleButton createToggleButton(String title, ButtonGroup bg, boolean selected, ActionListener al) {
		JToggleButton comp = new JToggleButton(title,selected);
		if (bg!=null) bg.add(comp);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}

	@SuppressWarnings("unused")
	private JButton createButton(String title, ActionListener al) {
		return createButton(title, true, al);
	}
	private JButton createButton(String title, boolean enabled, ActionListener al) {
		JButton comp = new JButton(title);
		comp.setEnabled(enabled);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu projectMenu = menuBar.add(new JMenu("Project"));
		projectMenu.add(createMenuItem("New Project",e->{}));
		projectMenu.add(createMenuItem("Load Project ...",e->{}));
		projectMenu.add(createMenuItem("Save Project",e->{}));
		projectMenu.add(createMenuItem("Save Project As ...",e->{}));
		
		JMenu fontMenu = menuBar.add(new JMenu("Font"));
		fontMenu.add(createMenuItem("Load Default Font",e->{ alphaCharEditor.project.loadDefaultFont(editorView.getViewState()); updateAfterFontLoad(); }));
		fontMenu.add(createMenuItem("Load Font ...",e->{}));
		fontMenu.add(createMenuItem("Save Font",e->{}));
		fontMenu.add(createMenuItem("Save Font As ...",e->{}));
		
		return menuBar;
	}

	private void updateAfterFontLoad() {
		charRaster.updateCharList(alphaCharEditor.project.font);
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
