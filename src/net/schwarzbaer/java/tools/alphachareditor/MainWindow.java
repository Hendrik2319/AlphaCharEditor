package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;

import net.schwarzbaer.gui.Canvas;
import net.schwarzbaer.gui.Disabler;
import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.image.alphachar.AlphaCharIO;
import net.schwarzbaer.image.alphachar.Form;
import net.schwarzbaer.java.tools.alphachareditor.EditorView.GuideLine;
import net.schwarzbaer.java.tools.alphachareditor.LineForm.FormType;

class MainWindow extends StandardMainWindow {
	private static final long serialVersionUID = 313126168052969131L;
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}
	
	private LineForm<?>[] lineforms = null;
	private Character selectedChar;
	private JComponent valuePanel;
	
	private final AlphaCharEditor alphaCharEditor;
	private final CharRaster charRaster;
	private final EditorView editorView;
	private final GeneralOptionPanel generalOptionPanel;
	private final FileChooser projectFileChooser;
	private final FileChooser fontFileChooser;

	MainWindow(AlphaCharEditor alphaCharEditor_, String title) {
		super(title);
		this.alphaCharEditor = alphaCharEditor_;
		
		projectFileChooser = new FileChooser("Project-File", "project");
		fontFileChooser = new FileChooser("Font-File", AlphaCharIO.ALPHACHARFONT_EXTENSION);
		
		selectedChar = null;
		charRaster = new CharRaster(this::setSelectedChar);
		JPanel charRasterPanel = new JPanel(new BorderLayout(3,3));
		charRasterPanel.setBorder(BorderFactory.createTitledBorder("Characters"));
		charRasterPanel.add(charRaster,BorderLayout.CENTER);
		
		generalOptionPanel = new GeneralOptionPanel(new GeneralOptionPanel.Context() {
			@Override public void repaintView() { editorView.repaint(); }
			@Override public Rectangle2D.Float getViewRectangle() { return editorView.getViewRectangle(); }

			@Override public void changeHighlightedForms    (List<LineForm<?>> forms) { editorView.setHighlightedForms(forms); }
			@Override public void changeSelectedForm        (LineForm<?> form       ) { editorView.setSelectedForm    (form); }
			@Override public void changeHighlightedGuideLine(GuideLine guideLine    ) { editorView.setHighlightedGuideLine(guideLine); }
			
			@Override
			public void addGuideLine(GuideLine guideLine) {
				if (guideLine==null) return;
				alphaCharEditor.project.guideLines.add(guideLine);
				editorView        .setGuideLines(alphaCharEditor.project.guideLines);
				generalOptionPanel.setGuideLines(alphaCharEditor.project.guideLines);
			}

			@Override
			public void removeGuideLine(int index) {
				if (index<0 || index>=alphaCharEditor.project.guideLines.size()) return;
				alphaCharEditor.project.guideLines.remove(index);
				editorView        .setGuideLines(alphaCharEditor.project.guideLines);
				generalOptionPanel.setGuideLines(alphaCharEditor.project.guideLines);
			}

			@Override
			public void addForm(LineForm<?> form) {
				if (form==null) return;
				LineForm<?>[] newArr = lineforms==null ? new LineForm[1] : Arrays.copyOf(lineforms, lineforms.length+1);
				newArr[newArr.length-1] = form;
				setNewArray(newArr);
			}
			@Override
			public void addForms(Vector<LineForm<?>> forms) {
				if (forms==null || forms.isEmpty()) return;
				LineForm<?>[] newArr = lineforms==null ? new LineForm[forms.size()] : Arrays.copyOf(lineforms, lineforms.length+forms.size());
				int offset = lineforms==null ? 0 : lineforms.length;
				for (int i=0; i<forms.size(); i++)
					newArr[offset+i] = forms.get(i);
				setNewArray(newArr);
			}
			@Override
			public void removeForms(List<LineForm<?>> forms) {
				if (forms==null || forms.isEmpty()) return;
				Vector<LineForm<?>> vec = new Vector<>(Arrays.asList(lineforms));
				for (LineForm<?> rf:forms) vec.remove(rf);
				LineForm<?>[] newArr = vec.toArray(new LineForm<?>[vec.size()]);
				setNewArray(newArr);
			}

			private void setNewArray(LineForm<?>[] newArr) {
				lineforms = newArr;
				editorView.setForms(lineforms);
				generalOptionPanel.setForms(lineforms,selectedChar);
				if (selectedChar!=null) {
					alphaCharEditor.project.font.put(selectedChar, LineForm.convert(lineforms));
					charRaster.updateCharList(alphaCharEditor.project.font,selectedChar);
				}
			}
			
		});
		generalOptionPanel.setPreferredSize(new Dimension(200, 200));
		
		JPanel leftPanel = new JPanel(new BorderLayout(3,3));
		leftPanel.add(charRasterPanel,BorderLayout.NORTH);
		leftPanel.add(valuePanel = generalOptionPanel,BorderLayout.CENTER);
		
		editorView = new EditorView(new EditorView.Context() {
			@Override public void updateHighlightedForms(HashSet<LineForm<?>> forms) {
				if (lineforms==null)
					generalOptionPanel.setSelectedForms(new int[0]);
				else {
					Vector<Integer> indices = new Vector<>();
					for (int i=0; i<lineforms.length; i++) {
						LineForm<?> form = lineforms[i];
						if (forms.contains(form)) indices.add(i);
					}
					generalOptionPanel.setSelectedForms(indices.stream().mapToInt(v->v).toArray());
				}
			}
			@Override public void setValuePanel(JPanel panel) {
				if (valuePanel!=null) leftPanel.remove(valuePanel);
				if (panel!=null)
					valuePanel = panel;
				else {
					valuePanel = generalOptionPanel;
				}
				if (valuePanel!=null) leftPanel.add(valuePanel,BorderLayout.CENTER);
				leftPanel.revalidate();
				leftPanel.repaint();
			}
		});
		editorView.setPreferredSize(500, 500);
		
		JPanel editorViewPanel = new JPanel(new BorderLayout(3,3));
		editorViewPanel.setBorder(BorderFactory.createTitledBorder("Geometry"));
		editorViewPanel.add(editorView,BorderLayout.CENTER);
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.add(leftPanel,BorderLayout.WEST);
		contentPane.add(editorViewPanel,BorderLayout.CENTER);
		
		JMenuBar menuBar = createMenuBar();
		
		startGUI(contentPane, menuBar);
		editorView.reset();
		
		updateAfterProjectLoad();
	}
	
	private void setSelectedChar(Character ch) {
		selectedChar=ch;
		Form[] forms = alphaCharEditor.project.font==null ? null : alphaCharEditor.project.font.get(selectedChar);
		System.out.printf("SelectedChar: %s %s%n", selectedChar==null ? "none" : "'"+selectedChar+"'", forms==null ? "--" : "["+forms.length+"]");
		lineforms = LineForm.convert(forms);
		editorView.setForms(lineforms);
		generalOptionPanel.setForms(lineforms,selectedChar);
	}
	
	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu projectMenu = menuBar.add(new JMenu("Project"));
		projectMenu.add(createMenuItem("New Project"        ,e->alphaCharEditor.createNewProject()));
		projectMenu.add(createMenuItem("Reload Project"     ,e->alphaCharEditor.reloadProject(                            )));
		projectMenu.add(createMenuItem("Load Project ..."   ,e->alphaCharEditor.loadProject  (      getProjectFileToOpen())));
		projectMenu.add(createMenuItem("Save Project"       ,e->alphaCharEditor.saveProject  (this::getProjectFileToSave  )));
		projectMenu.add(createMenuItem("Save Project As ...",e->alphaCharEditor.saveProjectAs(      getProjectFileToSave())));
		
		JMenu fontMenu = menuBar.add(new JMenu("Font"));
		fontMenu.add(createMenuItem("Load Default Font",e->{ alphaCharEditor.project.loadDefaultFont();                     updateAfterFontLoad(); }));
		fontMenu.add(createMenuItem("Reload Font"      ,e->{ alphaCharEditor.project.reloadFont(                         ); updateAfterFontLoad(); }));
		fontMenu.add(createMenuItem("Load Font ..."    ,e->{ alphaCharEditor.project.loadFont  (      getFontFileToOpen()); updateAfterFontLoad(); }));
		fontMenu.add(createMenuItem("Save Font"        ,e->{ alphaCharEditor.project.saveFont  (this::getFontFileToSave  ); }));
		fontMenu.add(createMenuItem("Save Font As ..." ,e->{ alphaCharEditor.project.saveFontAs(      getFontFileToSave()); }));
		
		return menuBar;
	}

	static <A, C extends JComponent> C addToDisabler(Disabler<A> disabler, A disableTag, C component) {
		disabler.add(disableTag, component);
		return component;
	}

	static JToggleButton createToggleButton(String title, ButtonGroup bg, boolean selected, ActionListener al) {
		JToggleButton comp = new JToggleButton(title,selected);
		if (bg!=null) bg.add(comp);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}

	static JButton createButton(String title, ActionListener al) {
		return createButton(title, true, al);
	}
	static JButton createButton(String title, boolean enabled, ActionListener al) {
		JButton comp = new JButton(title);
		comp.setEnabled(enabled);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	
	static JMenuItem createMenuItem(String title, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}

	private File getFileToSave(FileChooser fileChooser) {
		if (fileChooser.showSaveDialog(this) != FileChooser.APPROVE_OPTION) return null;
		return fileChooser.getSelectedFile();
	}

	private File getFileToOpen(FileChooser fileChooser) {
		if (fileChooser.showOpenDialog(this) != FileChooser.APPROVE_OPTION) return null;
		return fileChooser.getSelectedFile();
	}

	private File getProjectFileToSave() { return getFileToSave(projectFileChooser); }
	private File getProjectFileToOpen() { return getFileToOpen(projectFileChooser); }
	private File getFontFileToSave() { return getFileToSave(fontFileChooser); }
	private File getFontFileToOpen() { return getFileToOpen(fontFileChooser); }

	void updateAfterProjectLoad() {
		editorView        .setGuideLines(alphaCharEditor.project.guideLines);
		generalOptionPanel.setGuideLines(alphaCharEditor.project.guideLines);
		updateAfterFontLoad();
	}

	private void updateAfterFontLoad() {
		charRaster.updateCharList(alphaCharEditor.project.font,null);
		setSelectedChar(null);
	}

	private static class GeneralOptionPanel extends JTabbedPane {
		private static final long serialVersionUID = -2024771038202756837L;
		
		private final FormsPanel formsPanel;
		private final GuideLinesPanel guideLinesPanel;

		private Context context;
		
		GeneralOptionPanel(Context context) {
			super();
			this.context = context;
			setBorder(BorderFactory.createTitledBorder("General"));
			addTab("Forms"      ,      formsPanel = new      FormsPanel());
			addTab("Guide Lines", guideLinesPanel = new GuideLinesPanel());
		}
		
		void setGuideLines(Vector<GuideLine> guideLines) {
			guideLinesPanel.setGuideLines(guideLines);
		}
	
		void setSelectedForms(int[] selectedIndices) {
			formsPanel.setSelected(selectedIndices);
		}
	
		void setForms(LineForm<?>[] forms, Character selectedChar) {
			formsPanel.setForms(forms,selectedChar);
		}

		private Float showFloatInputDialog(Component parentComp, String message, Float initialValue) {
			String newStr = JOptionPane.showInputDialog(parentComp, message, initialValue);
			if (newStr==null) return null;
			
			try {
				return Float.parseFloat(newStr);
			} catch (NumberFormatException e) {
				message = String.format("Can't parse \"%s\" as numeric value.", newStr);
				JOptionPane.showMessageDialog(parentComp, message, "Wrong input", JOptionPane.ERROR_MESSAGE);
			}
			
			return null;
		}
		
		private <V> V showMultipleChoiceDialog(Component parentComp, String message, String title, V[] selectionValues, V initialSelectionValue, Class<V> classObj) {
			Object result = JOptionPane.showInputDialog(parentComp, message, title, JOptionPane.QUESTION_MESSAGE, null, selectionValues, initialSelectionValue);
			if (result==null) return null;
			if (!classObj.isAssignableFrom(result.getClass())) return null;
			return classObj.cast(result);
		}
		
		interface Context {
			void addForm (LineForm<?> form);
			void addForms(Vector<LineForm<?>> forms);
			void removeForms(List<LineForm<?>> forms);
			void addGuideLine(GuideLine guideLine);
			void removeGuideLine(int index);
			void changeHighlightedForms(List<LineForm<?>> forms);
			void changeSelectedForm(LineForm<?> form);
			void changeHighlightedGuideLine(GuideLine guideLine);
			void repaintView();
			Rectangle2D.Float getViewRectangle();
			
		}
	
		enum FormsPanelButtons { New,Edit,Remove,Copy,Paste,Mirror,Translate }
		
		private class FormsPanel extends JPanel {
			private static final long serialVersionUID = 5266768936706086790L;
			private final JList<LineForm<?>> formList;
			private final Vector<LineForm<?>> localClipboard;
			private Disabler<FormsPanelButtons> disabler;
			private Character selectedChar;
			
			FormsPanel() {
				super(new BorderLayout(3,3));
				setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
				
				localClipboard = new Vector<>();
				
				disabler = new Disabler<FormsPanelButtons>();
				disabler.setCareFor(FormsPanelButtons.values());
				
				formList = new JList<>();
				formList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				formList.addListSelectionListener(e->{
					List<LineForm<?>> selectedValues = formList.getSelectedValuesList();
					context.changeHighlightedForms(selectedValues);
					setButtonsEnabled(selectedValues.size());
				});
				
				JScrollPane formListScrollPane = new JScrollPane(formList);
				
				JPanel buttonPanel1 = new JPanel(new GridBagLayout());
				buttonPanel1.add( addToDisabler( disabler, FormsPanelButtons.New   , createButton("New"   , false, e->context.addForm(createNewForm())                       ) ) );
				buttonPanel1.add( addToDisabler( disabler, FormsPanelButtons.Edit  , createButton("Edit"  , false, e->context.changeSelectedForm(formList.getSelectedValue())) ) );
				buttonPanel1.add( addToDisabler( disabler, FormsPanelButtons.Remove, createButton("Remove", false, e->context.removeForms(formList.getSelectedValuesList())  ) ) );
				
				JPanel buttonPanel2 = new JPanel(new GridBagLayout());
				buttonPanel2.add( addToDisabler( disabler, FormsPanelButtons.Copy  , createButton("Copy" , false, e->copyForms(formList.getSelectedValuesList())) ) );
				buttonPanel2.add( addToDisabler( disabler, FormsPanelButtons.Paste , createButton("Paste", false, e->pasteForms()                               ) ) );
				
				JPanel buttonPanel3 = new JPanel(new GridBagLayout());
				buttonPanel3.add( addToDisabler( disabler, FormsPanelButtons.Mirror   , createButton("Mirror"   , false, e->mirrorForms   (formList.getSelectedValuesList()) ) ) );
				buttonPanel3.add( addToDisabler( disabler, FormsPanelButtons.Translate, createButton("Translate", false, e->translateForms(formList.getSelectedValuesList()) ) ) );
				
				JPanel buttonGroupsPanel = new JPanel(new GridLayout(0,1));
				buttonGroupsPanel.add(buttonPanel1);
				buttonGroupsPanel.add(buttonPanel2);
				buttonGroupsPanel.add(buttonPanel3);
				
				add(formListScrollPane,BorderLayout.CENTER);
				add(buttonGroupsPanel,BorderLayout.SOUTH);
			}
	
			private void translateForms(List<LineForm<?>> forms) {
				Float x = showFloatInputDialog(this, "Set X translation value: ", null);
				if (x==null) return;
				Float y = showFloatInputDialog(this, "Set Y translation value: ", null);
				if (y==null) return;
				
				for (LineForm<?> form:forms)
					if (form!=null)
						form.translate(x,y);
				
				context.repaintView();
			}

			private void mirrorForms(List<LineForm<?>> forms) {
				LineForm.MirrorDirection dir = showMultipleChoiceDialog(this, "Select mirror direction:", "Mirror Direction", LineForm.MirrorDirection.values(), null, LineForm.MirrorDirection.class);
				if (dir==null) return;
				Float pos = showFloatInputDialog(this, String.format("Set position of %s mirror axis: ", dir.axisPos.toLowerCase()), null);
				if (pos==null) return;
				
				for (LineForm<?> form:forms)
					if (form!=null)
						form.mirror(dir,pos);
				
				context.repaintView();
			}

			private void pasteForms() {
				Vector<LineForm<?>> vec = new Vector<>();
				for (LineForm<?> form:localClipboard)
					if (form!=null)
						vec.add(LineForm.clone(form));
				context.addForms(vec);
			}

			private void copyForms(List<LineForm<?>> forms) {
				localClipboard.clear();
				for (LineForm<?> form:forms)
					if (form!=null)
						localClipboard.add(LineForm.clone(form));
				setButtonsEnabled(formList.getSelectedValuesList().size());
			}

			private void setButtonsEnabled(int selection) {
				disabler.setEnable(button->{
					switch (button) {
					case New: return selectedChar!=null;
					case Edit: return selection==1;
					case Copy:
					case Remove:
					case Mirror:
					case Translate: return selection>0;
					case Paste: return !localClipboard.isEmpty();
					}
					return false;
				});
			}

			private LineForm<?> createNewForm() {
				FormType formType = showMultipleChoiceDialog(this, "Select type of new form:", "Form Type", LineForm.FormType.values(), null, LineForm.FormType.class);
				if (formType==null) return null;
				return LineForm.createNew(formType, context.getViewRectangle());
			}

			void setSelected(int[] selectedIndices) {
				if (selectedIndices==null || selectedIndices.length==0) formList.clearSelection();
				else formList.setSelectedIndices(selectedIndices);
				setButtonsEnabled(selectedIndices==null ? 0 : selectedIndices.length);
			}
	
			void setForms(LineForm<?>[] forms, Character selectedChar) {
				this.selectedChar = selectedChar;
				formList.setModel(new FormListModel(forms));
				disabler.setEnable(FormsPanelButtons.New, this.selectedChar!=null);
			}
	
			private final class FormListModel implements ListModel<LineForm<?>> {
				private LineForm<?>[] forms;
				private Vector<ListDataListener> listDataListeners;
				
				public FormListModel(LineForm<?>[] forms) {
					this.forms = forms;
					listDataListeners = new Vector<>();
				}
	
				@Override public int getSize() { return forms==null ? 0 : forms.length; }
				@Override public LineForm<?> getElementAt(int index) {
					if (forms==null || index<0 || index>=forms.length) return null;
					return forms[index];
				}
			
				@Override public void    addListDataListener(ListDataListener l) { listDataListeners.   add(l); }
				@Override public void removeListDataListener(ListDataListener l) { listDataListeners.remove(l);}
			}
			
		}
		
		private class GuideLinesPanel extends JPanel {
			private static final long serialVersionUID = -2804258616332267816L;
			private final JList<GuideLine> guideLineList;
			private final JButton btnNew;
			private final JButton btnEdit;
			private final JButton btnRemove;
	
			GuideLinesPanel() {
				super(new BorderLayout(3,3));
				setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
				
				guideLineList = new JList<GuideLine>();
				guideLineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				guideLineList.addListSelectionListener(e->{
					GuideLine selected = guideLineList.getSelectedValue();
					setButtonsEnabled(selected!=null);
					context.changeHighlightedGuideLine(selected);
				});
				
				JScrollPane guideLineListScrollPane = new JScrollPane(guideLineList);
				
				JPanel buttonPanel = new JPanel(new GridBagLayout());
				buttonPanel.add(btnNew    = createButton("New"   , true , e->context.addGuideLine(createNewGuideLine())));
				buttonPanel.add(btnEdit   = createButton("Edit"  , false, e->editGuideLine(guideLineList.getSelectedValue())));
				buttonPanel.add(btnRemove = createButton("Remove", false, e->context.removeGuideLine(guideLineList.getSelectedIndex())));
				
				add(guideLineListScrollPane,BorderLayout.CENTER);
				add(buttonPanel,BorderLayout.SOUTH);
			}
	
			private void setButtonsEnabled(boolean enabled) {
				btnNew   .setEnabled(true);
				btnEdit  .setEnabled(enabled);
				btnRemove.setEnabled(enabled);
			}

			private GuideLine createNewGuideLine() {
				GuideLine.Type type = getGuideLineType();
				if (type==null) return null;
				
				Float pos = getPosOfGuideLine(type, null);
				if (pos==null) return null;
				
				return new GuideLine(type, pos);
			}
			
			private void editGuideLine(GuideLine selected) {
				if (selected==null) return;
				
				Float pos = getPosOfGuideLine(selected.type, selected.pos);
				if (pos==null) return;
				
				selected.pos = pos;
				context.repaintView();
				guideLineList.repaint();
			}
			
			private GuideLine.Type getGuideLineType() {
				return showMultipleChoiceDialog(this, "Select type of new GuideLine:", "GuideLine Type", GuideLine.Type.values(), null, GuideLine.Type.class);
			}
	
			private Float getPosOfGuideLine(GuideLine.Type type, Float initialPos) {
				return showFloatInputDialog(this,String.format("Set %s position of %s guideline:", type.axis, type.toString().toLowerCase()), initialPos);
			}

			void setGuideLines(Vector<GuideLine> guideLines) {
				guideLineList.setModel(new GuideLineListModel(guideLines));
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

		private char[] createCharArray(char first, char last) {
			char[] chars = new char[last-first+1];
			for (int i=0; i<chars.length; ++i) chars[i] = (char) (first+i);
			return chars;
		}

		public void updateCharList(HashMap<Character, Form[]> font, Character selectedChar) {
			for (int b=0; b<charExist.length; b++) {
				for (int ch=0; ch<charExist[b].length; ch++) {
					Form[] forms = font==null ? null : font.get(chars[b][ch]);
					charExist[b][ch] = forms!=null && forms.length>0;
				}
			}
			this.highlightedField = null;
			this.selectedField = getField(selectedChar);
			this.selectedChar = selectedChar;
			System.out.printf("SelectedChar: %s (%s)%n", this.selectedChar, this.selectedField);
			repaint();
		}

		interface SelectionListener {
			void selectedCharChanged(Character selectedChar);
		}
		
		private void setSelectedField(Point p) {
			Point field = getField(p);
			boolean repaint = false;
//			if (field==null) {
//				repaint = selectedField!=null;
//				selectedField=null;
//			} else if (selectedField==null || !selectedField.equals(field)) {
//				repaint = true;
//				selectedField = field; 
//			}
			repaint = true;
			selectedField = field; 
			updateSelectedChar();
			if (repaint) repaint();
		}
		
		private void setHighlightedField(Point p) {
			Point field = getField(p);
			boolean repaint = false;
//			if (field==null) {
//				repaint = highlightedField!=null;
//				highlightedField=null;
//			} else if (highlightedField==null || !highlightedField.equals(field)) {
//				repaint = true;
//				highlightedField = field; 
//			}
			repaint = true;
			highlightedField = field; 
			if (repaint) repaint();
		}

		private Point getField(Character ch) {
			if (ch==null || view==null) return null;
			int block = -1;
			int index = -1;
			for (int b=0; b<chars.length; b++) {
				for (int i=0; i<chars[b].length; i++) {
					if (chars[b][i]==ch) {
						block = b;
						index = i;
						break;
					}
				}
				if (block>=0) break;
			}
			
//			// from <getCharAt>
//			int y = field.y-rows[block];
//			int x = field.x;
			int iWidth = view.width/fieldWidth;
//			int i = x + iWidth*y;
			
			int x =  index % iWidth;
			int y = (index / iWidth)+rows[block];
			return new Point(x, y);
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

		private Character getCharAt(Point field) {
			if (field==null || view==null) return null;
			int block = -1;
			for (int b=0; b<rows.length; b++) {
				if (field.y>=rows[b] && (b+1>=rows.length || field.y<rows[b+1])) {
					block  = b;
					break;
				}
			}
			if (block>=0) {
				int y = field.y-rows[block];
				int x = field.x;
				int iWidth = view.width/fieldWidth;
				int i = x + iWidth*y;
				if (i<chars[block].length) {
					return chars[block][i];
				}
			}
			return null;
		}

		private void updateSelectedChar() {
			Character ch = getCharAt(selectedField);
			if (ch!=null && selectedChar != null && selectedChar.equals(ch)) return;
			if (ch==null && selectedChar == null) return; 
			selectedChar = ch;
			listener.selectedCharChanged(selectedChar);
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
