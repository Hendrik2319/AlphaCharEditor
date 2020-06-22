package net.schwarzbaer.java.tools.alphachareditor;

import java.io.File;
import java.util.HashMap;

import net.schwarzbaer.image.alphachar.AlphaCharIO;
import net.schwarzbaer.image.alphachar.Form;

public class AlphaCharEditor {
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}

	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
		HashMap<Character,Form[]> alphabet = new HashMap<Character,Form[]>();
		alphabet.put('X',new Form[] { new Form.Line(0, 0,50,100), new Form.Line(50, 0,0,100) });
		alphabet.put('x',new Form[] { new Form.Line(0,40,50,100), new Form.Line(50,40,0,100) });
		alphabet.put('M',new Form[] { new Form.PolyLine(0,100).add(0,0).add(35,60).add(70,0).add(70,100) });
		alphabet.put('B',new Form[] { new Form.Line(0,0,0,100), new Form.Line(0,0,20,0), new Form.Line(0,40,20,40), new Form.Line(0,100,20,100), new Form.Arc(20,20,20,-Math.PI/2,Math.PI/2), new Form.Arc(20,70,30,-Math.PI/2,Math.PI/2) });
		
		File file1 = new File("alphabet1.txt");
		File file2 = new File("alphabet2.txt");
		AlphaCharIO.test(alphabet, file1, file2);
		
		HashMap<Character, Form[]> alphabet2 = AlphaCharIO.readDefaultAlphaCharFont();
		if (alphabet2==null) {
			System.out.println("No \"default\" font.");
		} else {
			File file3 = new File("alphabet3.txt");
			AlphaCharIO.writeAlphaCharToFile(file3, alphabet2);
		}
	}
}
