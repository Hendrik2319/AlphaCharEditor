package net.schwarzbaer.java.tools.alphachareditor;

import java.awt.Dimension;
import java.awt.Point;
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
import java.util.HashMap;
import java.util.Vector;
import java.util.function.Supplier;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.java.lib.image.linegeometry.AlphaCharIO;
import net.schwarzbaer.java.lib.image.linegeometry.Form;
import net.schwarzbaer.java.lib.system.Settings;
import net.schwarzbaer.java.tools.alphachareditor.EditorView.GuideLine;

public class AlphaCharEditor {
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		//testAlphaCharIO();
		
		new AlphaCharEditor().createGUI().readLastProject();
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

	MainWindow mainwindow = null;
	Project project;
	final AppSettings settings;

	private AlphaCharEditor() {
		settings = new AppSettings();
		createDefaultProject();
	}

	private void createDefaultProject() {
		project = new Project(null);
		project.createDefaultGuideLines();
	}
	
	private AlphaCharEditor readLastProject() {
		Assert(mainwindow!=null);
		String lastProjectPath = settings.getString(AppSettings.ValueKey.Project, null);
		if (lastProjectPath!=null) project = Project.readFromFile(new File(lastProjectPath));
		mainwindow.updateAfterProjectLoad();
		return this;
	}
	
	void createNewProject() {
		Assert(mainwindow!=null);
		createDefaultProject();
		settings.remove(AppSettings.ValueKey.Project);
		mainwindow.updateAfterProjectLoad();
	}
	
	void reloadProject() {
		if (project.projectFile!=null)
			loadProject(project.projectFile);
	}

	void loadProject(File file) {
		if (file==null) return;
		Assert(mainwindow!=null);
		project = Project.readFromFile(file);
		settings.putString(AppSettings.ValueKey.Project, file.getAbsolutePath());
		mainwindow.updateAfterProjectLoad();
	}
	
	void saveProjectAs(File file) {
		if (file==null) return;
		project.writeToFile(file);
		settings.putString(AppSettings.ValueKey.Project, file.getAbsolutePath());
	}
	
	void saveProject(Supplier<File> getFile) {
		Assert(getFile!=null);
		if (project.projectFile!=null) {
			project.writeToFile(project.projectFile);
		} else {
			File file = getFile.get();
			if (file!=null) {
				project.writeToFile(file);
				settings.putString(AppSettings.ValueKey.Project, file.getAbsolutePath());
			}
		}
	}
	

	private AlphaCharEditor createGUI() {
		mainwindow = new MainWindow(this,"AlphaChar Editor");
		return this;
	}
	
	static class AppSettings extends Settings<AppSettings.ValueGroup,AppSettings.ValueKey> {
		
		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			WindowPos (ValueKey.WindowX, ValueKey.WindowY),
			WindowSize(ValueKey.WindowWidth, ValueKey.WindowHeight);
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		enum ValueKey {
			WindowX, WindowY, WindowWidth, WindowHeight,
			Project
		}

		public AppSettings() { super(AlphaCharEditor.class); }

		public Point getWindowPos() {
			int x = getInt(ValueKey.WindowX);
			int y = getInt(ValueKey.WindowY);
			return new Point(x,y);
		}
		public void setWindowPos(Point location) {
			putInt(ValueKey.WindowX, location.x);
			putInt(ValueKey.WindowY, location.y);
		}

		public Dimension getWindowSize() {
			int w = getInt(ValueKey.WindowWidth );
			int h = getInt(ValueKey.WindowHeight);
			return new Dimension(w,h);
		}
		public void setWindowSize(Dimension size) {
			putInt(ValueKey.WindowWidth , size.width );
			putInt(ValueKey.WindowHeight, size.height);
		}
	}
	
	static class Project {
		private File projectFile;
        Vector<GuideLine> guideLines = new Vector<>();;
    	HashMap<Character, Form[]> font = null;
    	private File fontFile = null;
		private boolean fontIsDefault = false;
    	
    	public Project(File projectFile) {
			this.projectFile = projectFile;
		}

		private void createDefaultGuideLines() {
    		guideLines.clear();
    		guideLines.add(new GuideLine(GuideLine.Type.Vertical,0));
    		guideLines.add(new GuideLine(GuideLine.Type.Horizontal,0));
    		guideLines.add(new GuideLine(GuideLine.Type.Horizontal,40));
    		guideLines.add(new GuideLine(GuideLine.Type.Horizontal,100));
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
    			
    			for (GuideLine gl:guideLines)
					out.printf("GuideLine.%s=%s%n", gl.type.name(), Double.toString(gl.pos));
    			
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
					
					for (GuideLine.Type type:GuideLine.Type.values()) {
						// out.printf("GuideLine.%s=%s%n", gl.type.toString(), Float.toString(gl.pos));
						String prefix = String.format("GuideLine.%s=", type.name());
						if (line.startsWith(prefix)) {
							String str = line.substring(prefix.length());
							try {
								double pos = Double.parseDouble(str);
								project.guideLines.add(new GuideLine(type, pos));
							} catch (NumberFormatException e) {
								System.err.printf("Can't convert \"%s\" in line \"%s\" into a numeric value.", str, line);
							}
							break;
						}
					}
					
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
    		this.font = AlphaCharIO.readDefaultAlphaCharFont(new LineForm.Factory(), true);
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
			this.font = AlphaCharIO.readAlphaCharFontFromFile(fontFile, new LineForm.Factory(), true);
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
}
