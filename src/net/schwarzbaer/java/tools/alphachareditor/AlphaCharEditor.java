package net.schwarzbaer.java.tools.alphachareditor;

import java.io.File;
import java.util.HashMap;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.image.alphachar.AlphaCharIO;
import net.schwarzbaer.image.alphachar.Form;

public class AlphaCharEditor {
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		// TODO Auto-generated method stub
		//testAlphaCharIO();
		
		new AlphaCharEditor().createGUI();
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

	@SuppressWarnings("unused")
	private MainWindow mainwindow;

	private void createGUI() {
		mainwindow = new MainWindow("AlphaChar Editor");
	}
}
