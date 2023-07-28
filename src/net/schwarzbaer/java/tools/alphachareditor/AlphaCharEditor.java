package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.java.lib.gui.Canvas;
import net.schwarzbaer.java.lib.gui.FileChooser;
import net.schwarzbaer.java.lib.gui.StandardMainWindow;
import net.schwarzbaer.java.lib.gui.ZoomableCanvas;
import net.schwarzbaer.java.lib.image.linegeometry.AlphaCharIO;
import net.schwarzbaer.java.lib.image.linegeometry.Form;
import net.schwarzbaer.java.lib.system.Settings.DefaultAppSettings;
import net.schwarzbaer.java.tools.lineeditor.EditorViewFeature;
import net.schwarzbaer.java.tools.lineeditor.LineEditor;
import net.schwarzbaer.java.tools.lineeditor.LineEditor.GuideLinesStorage;

public class AlphaCharEditor {
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		//testAlphaCharIO();
		
		new AlphaCharEditor().readLastProject();
	}

	@SuppressWarnings("unused")
	private static void testAlphaCharIO() {
		HashMap<Character,Form[]> alphabet = new HashMap<Character,Form[]>();
		alphabet.put('X',new Form[] { new Form.Line(0, 0,50,100), new Form.Line(50, 0,0,100) });
		alphabet.put('x',new Form[] { new Form.Line(0,40,50,100), new Form.Line(50,40,0,100) });
		alphabet.put('M',new Form[] { new Form.PolyLine(0,100).add(0,0).add(35,60).add(70,0).add(70,100) });
		alphabet.put('B',new Form[] { new Form.Line(0,0,0,100), new Form.Line(0,0,20,0), new Form.Line(0,40,20,40), new Form.Line(0,100,20,100), new Form.Arc(20,20,20,-Math.PI/2,Math.PI/2), new Form.Arc(20,70,30,-Math.PI/2,Math.PI/2) });
		
		File file1 = new File("alphabet1.txt");
		File file2 = new File("alphabet2.txt");
		AlphaCharIO.test(alphabet, file1, file2);
		
		File file3 = new File("alphabet3.txt");
		AlphaCharIO.rewriteDefaultAlphaCharFont(file3);
	}

	private final static AppSettings settings = new AppSettings();
	private final LineEditor lineEditor;
	private final StandardMainWindow mainWindow;
	private final FileChooser projectFileChooser;
	private final FileChooser fontFileChooser;
	private final JPanel leftPanel;
	private final CharRaster charRaster;
	
	private Project project;
	private JComponent valuePanel;
	private Character selectedChar;

	private AlphaCharEditor() {
		project = Project.createDefaultProject();
		
		projectFileChooser = new FileChooser("Project-File", "project");
		fontFileChooser = new FileChooser("Font-File", AlphaCharIO.ALPHACHARFONT_EXTENSION);
		
		mainWindow = new StandardMainWindow("AlphaChar Editor");
		leftPanel = new JPanel(new BorderLayout(3,3));
		
		lineEditor = new LineEditor(
				new LineEditor.Context() {
					@Override public void switchOptionsPanel(JComponent panel)
					{
						if (valuePanel!=null) leftPanel.remove(valuePanel);
						valuePanel = panel;
						if (valuePanel!=null) leftPanel.add(valuePanel,BorderLayout.CENTER);
						leftPanel.revalidate();
						leftPanel.repaint();
					}
					@Override public boolean canCreateNewForm()
					{
						return selectedChar!=null;
					}
					@Override public void replaceForms(Form[] forms)
					{
						project.font.put(selectedChar, forms);
						charRaster.updateCharList(project.font,selectedChar);
					}
				},
				new ThickLines()
		);
		valuePanel = lineEditor.getInitialOptionsPanel();
		
		selectedChar = null;
		charRaster = new CharRaster(this::setSelectedChar);
		JPanel charRasterPanel = new JPanel(new BorderLayout(3,3));
		charRasterPanel.setBorder(BorderFactory.createTitledBorder("Characters"));
		charRasterPanel.add(charRaster,BorderLayout.CENTER);
		
		leftPanel.add(charRasterPanel,BorderLayout.NORTH);
		leftPanel.add(valuePanel,BorderLayout.CENTER);
		
		JPanel editorViewPanel = new JPanel(new BorderLayout(3,3));
		editorViewPanel.setBorder(BorderFactory.createTitledBorder("Geometry"));
		editorViewPanel.add(lineEditor.getEditorView(),BorderLayout.CENTER);
		
		JPanel contentPane = new JPanel(new BorderLayout(3,3));
		contentPane.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
		contentPane.add(leftPanel,BorderLayout.WEST);
		contentPane.add(editorViewPanel,BorderLayout.CENTER);
		
		mainWindow.startGUI(contentPane, createMenuBar());
		settings.registerAppWindow(mainWindow);
		
		lineEditor.init();
		updateAfterProjectLoad();
	}

	private void setSelectedChar(Character ch) {
		selectedChar=ch;
		Form[] forms = project.font==null ? null : project.font.get(selectedChar);
		System.out.printf("SelectedChar: %s %s%n", selectedChar==null ? "none" : "'"+selectedChar+"'", forms==null ? "--" : "["+forms.length+"]");
		lineEditor.setForms(forms);
	}

	private AlphaCharEditor readLastProject() {
		File lastProjectFile = settings.getFile(AppSettings.ValueKey.Project, null);
		if (lastProjectFile!=null) project = Project.readFromFile(lastProjectFile);
		updateAfterProjectLoad();
		return this;
	}
	
	void createNewProject() {
		project = Project.createDefaultProject();
		settings.remove(AppSettings.ValueKey.Project);
		updateAfterProjectLoad();
	}
	
	void reloadProject() {
		if (project.projectFile!=null)
			loadProject(project.projectFile);
	}

	void loadProject(File file) {
		if (file==null) return;
		project = Project.readFromFile(file);
		settings.putFile(AppSettings.ValueKey.Project, file);
		updateAfterProjectLoad();
	}
	
	void saveProjectAs(File file) {
		if (file==null) return;
		project.writeToFile(file);
		settings.putFile(AppSettings.ValueKey.Project, file);
	}
	
	void saveProject(Supplier<File> getFile) {
		Assert(getFile!=null);
		if (project.projectFile!=null) {
			project.writeToFile(project.projectFile);
		} else {
			File file = getFile.get();
			if (file!=null) {
				project.writeToFile(file);
				settings.putFile(AppSettings.ValueKey.Project, file);
			}
		}
	}
	
	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu projectMenu = menuBar.add(new JMenu("Project"));
		projectMenu.add(createMenuItem("New Project"        ,e->createNewProject()));
		projectMenu.add(createMenuItem("Reload Project"     ,e->reloadProject(                            )));
		projectMenu.add(createMenuItem("Load Project ..."   ,e->loadProject  (      getProjectFileToOpen())));
		projectMenu.add(createMenuItem("Save Project"       ,e->saveProject  (this::getProjectFileToSave  )));
		projectMenu.add(createMenuItem("Save Project As ...",e->saveProjectAs(      getProjectFileToSave())));
		
		JMenu fontMenu = menuBar.add(new JMenu("Font"));
		fontMenu.add(createMenuItem("Load Default Font",e->{ project.loadDefaultFont();                     updateAfterFontLoad(); }));
		fontMenu.add(createMenuItem("Reload Font"      ,e->{ project.reloadFont(                         ); updateAfterFontLoad(); }));
		fontMenu.add(createMenuItem("Load Font ..."    ,e->{ project.loadFont  (      getFontFileToOpen()); updateAfterFontLoad(); }));
		fontMenu.add(createMenuItem("Save Font"        ,e->{ project.saveFont  (this::getFontFileToSave  ); }));
		fontMenu.add(createMenuItem("Save Font As ..." ,e->{ project.saveFontAs(      getFontFileToSave()); }));
		
		return menuBar;
	}

	private void updateAfterProjectLoad() {
		lineEditor.setGuideLines(project.guideLinesStorage);
		updateAfterFontLoad();
	}

	private void updateAfterFontLoad() {
		charRaster.updateCharList(project.font,null);
		setSelectedChar(null);
	}

	private File getFileToSave(FileChooser fileChooser) {
		if (fileChooser.showSaveDialog(mainWindow) != FileChooser.APPROVE_OPTION) return null;
		return fileChooser.getSelectedFile();
	}

	private File getFileToOpen(FileChooser fileChooser) {
		if (fileChooser.showOpenDialog(mainWindow) != FileChooser.APPROVE_OPTION) return null;
		return fileChooser.getSelectedFile();
	}

	private File getProjectFileToSave() { return getFileToSave(projectFileChooser); }
	private File getProjectFileToOpen() { return getFileToOpen(projectFileChooser); }
	private File getFontFileToSave() { return getFileToSave(fontFileChooser); }
	private File getFontFileToOpen() { return getFileToOpen(fontFileChooser); }
	
	static JMenuItem createMenuItem(String title, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		if (al!=null) comp.addActionListener(al);
		return comp;
	}
	static JCheckBoxMenuItem createCheckBoxMI(String title, boolean isSelected, Consumer<Boolean> setValue) {
		JCheckBoxMenuItem comp = new JCheckBoxMenuItem(title, isSelected);
		if (setValue!=null) comp.addActionListener(e->setValue.accept(comp.isSelected()));
		return comp;
	}

	private static class AppSettings extends DefaultAppSettings<AppSettings.ValueGroup,AppSettings.ValueKey> {
		
		enum ValueGroup implements DefaultAppSettings.GroupKeys<ValueKey> {
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		enum ValueKey {
			Project
		}

		AppSettings() { super(AlphaCharEditor.class, ValueKey.values()); }
	}
	
	private static class Project {
		private File projectFile;
		private final GuideLinesStorage guideLinesStorage = new GuideLinesStorage();
		private HashMap<Character, Form[]> font = null;
    	private File fontFile = null;
		private boolean fontIsDefault = false;
    	
    	Project(File projectFile) {
			this.projectFile = projectFile;
		}
    	
    	static Project createDefaultProject()
    	{
    		Project project = new Project(null);
    		project.guideLinesStorage.setDefaultGuideLines(new double[]{ 0 }, new double[]{ 0,40,100 });
    		return project;
    	}

		void writeToFile(File file) {
			Assert(file!=null);
			projectFile = file;
			
			System.out.printf("Write project to file \"%s\" ...%n", file);
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(projectFile), StandardCharsets.UTF_8))) {
    			
    			if (fontIsDefault)
    				out.printf("DefaultFont%n");
    			
    			if (fontFile!=null && !fontIsDefault)
    				out.printf("Font=%s%n", fontFile.getAbsolutePath());
    			
    			guideLinesStorage.writeToFile(out);
    			
    		} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			System.out.printf("... done%n");
		}
    	
    	static Project readFromFile(File file) {
    		Assert(file!=null);
			Project project = new Project(file);
			
			System.out.printf("Read project from file \"%s\" ...%n", file);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				
				String line;
				while( (line=in.readLine())!=null ) {
					
					if (line.equals("DefaultFont"))
						project.fontIsDefault = true;
					
					if (line.startsWith("Font=")) {
						String str = line.substring("Font=".length());
						project.fontFile = new File(str);
					}
					
					project.guideLinesStorage.parseLine(line);
					
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.printf("... done%n");
			
			project.reloadFont();
			return project;
    	}

    	void loadDefaultFont() {
    		this.font = AlphaCharIO.readDefaultAlphaCharFont(LineEditor.createFormFactory(), true);
    		this.fontFile = null;
    		this.fontIsDefault = true;
    	}

		void reloadFont() {
			if (fontIsDefault)
				loadDefaultFont();
			else if (fontFile!=null)
				loadFont(fontFile);
		}

		void loadFont(File fontFile) {
			if (fontFile==null) return;
			this.font = AlphaCharIO.readAlphaCharFontFromFile(fontFile, LineEditor.createFormFactory(), true);
    		this.fontFile = fontFile;
    		this.fontIsDefault = false;
    	}

		void saveFont(Supplier<File> getFontFile) {
			File file = fontFile;
			if (file==null) file=getFontFile.get();
			if (file!=null) saveFontAs(file);
    	}

		void saveFontAs(File fontFile) {
			if (fontFile==null) return;
			AlphaCharIO.writeAlphaCharToFile(fontFile, font, true);
    		this.fontFile = fontFile;
    		this.fontIsDefault = false;
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

		void updateCharList(HashMap<Character, Form[]> font, Character selectedChar) {
			for (int b=0; b<charExist.length; b++) {
				for (int ch=0; ch<charExist[b].length; ch++) {
					Form[] forms = font==null ? null : font.get(chars[b][ch]);
					charExist[b][ch] = forms!=null && forms.length>0;
				}
			}
			this.highlightedField = null;
			this.selectedField = getField(selectedChar);
			this.selectedChar = selectedChar;
			//System.out.printf("SelectedChar: %s (%s)%n", this.selectedChar, this.selectedField);
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
	
	private static class ThickLines implements EditorViewFeature
	{
		private static final Color COLOR_THICKLINES  = new Color(0xf0f0f0);
		private static final Color COLOR_THICKLINES2 = new Color(0xe0e0e0);
		private static final Color COLOR_THICKLINES3 = new Color(0xd0d0d0);
		
		private boolean showThickLines = true;
		private float thickLinesWidth = 20f;
		private JCheckBoxMenuItem miShowThickLines = null;
		private Component editorView = null;
		
		@Override
		public void setEditorView(Component editorView)
		{
			this.editorView = editorView;
		}
		
		private boolean isShowThickLines  () { return showThickLines ; }
		private float   getThickLinesWidth() { return thickLinesWidth; }
		private void setShowThickLines (boolean showThickLines ) { this.showThickLines  = showThickLines ; editorView.repaint(); }
		private void setThickLinesWidth(float   thickLinesWidth) { this.thickLinesWidth = thickLinesWidth; editorView.repaint(); }

		@Override
		public void addToEditorViewContextMenu(JPopupMenu contextMenu)
		{
			contextMenu.addSeparator();
			contextMenu.add(miShowThickLines = createCheckBoxMI("Show Thick Lines", isShowThickLines(), this::setShowThickLines   ));
			contextMenu.add(createMenuItem("Set line width ...", e->{
				float width = getThickLinesWidth();
				String result = JOptionPane.showInputDialog(editorView, "Set width of thick lines:", width);
				if (result!=null) {
					try { setThickLinesWidth(Float.parseFloat(result)); }
					catch (NumberFormatException e1) {
						String message = "Error: Can't convert input into numeric value.";
						JOptionPane.showMessageDialog(editorView, message, "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}));
		}
		
		@Override
		public void prepareContextMenuToShow()
		{
			miShowThickLines.setSelected(isShowThickLines());
		}
		
		@Override
		public void draw(Graphics2D g2, int x, int y, int width, int height, ZoomableCanvas.ViewState viewState, Iterable<? extends FeatureLineForm> forms)
		{
			if (forms!=null && showThickLines) {
				float lineWidth = viewState.convertLength_LengthToScreenF((double) thickLinesWidth).floatValue();
				
				g2.setColor(COLOR_THICKLINES);
				g2.setStroke(new BasicStroke(lineWidth,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
				for (FeatureLineForm form : forms) form.drawLines(g2,viewState);
				
				g2.setColor(COLOR_THICKLINES2);
				g2.setStroke(new BasicStroke(lineWidth/3*2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
				for (FeatureLineForm form : forms) form.drawLines(g2,viewState);
				
				g2.setColor(COLOR_THICKLINES3);
				g2.setStroke(new BasicStroke(lineWidth/3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
				for (FeatureLineForm form : forms) form.drawLines(g2,viewState);
				
				g2.setStroke(new BasicStroke(1f));
			}
		}
		
	}
}
