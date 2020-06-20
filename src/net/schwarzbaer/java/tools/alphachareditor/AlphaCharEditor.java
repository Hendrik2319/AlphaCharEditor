package net.schwarzbaer.java.tools.alphachareditor;

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
import java.util.Vector;

public class AlphaCharEditor {
	
	static void Assert(boolean condition) {
		if (!condition) throw new IllegalStateException();
	}

	public static void main(String[] args) {
		
		// TODO Auto-generated method stub
		HashMap<Character,Form[]> alphabet = new HashMap<Character,Form[]>();
		alphabet.put('X',new Form[] { new Line(0, 0,50,100), new Line(50, 0,0,100) });
		alphabet.put('x',new Form[] { new Line(0,40,50,100), new Line(50,40,0,100) });
		alphabet.put('M',new Form[] { new PolyLine(0,100).add(0,0).add(35,60).add(70,0).add(70,100) });
		alphabet.put('B',new Form[] { new Line(0,0,0,100), new Line(0,0,20,0), new Line(0,40,20,40), new Line(0,100,20,100), new Arc(20,20,20,-Math.PI/2,Math.PI/2), new Arc(20,70,30,-Math.PI/2,Math.PI/2) });
		
		File file1 = new File("alphabet1.txt");
		File file2 = new File("alphabet2.txt");
		FileIO.test(alphabet, file1, file2);
	}

	private static class FileIO {

		private static void test(HashMap<Character, Form[]> alphabet1, File file1, File file2) {
			writeAlphaCharToFile(file1, alphabet1);
			HashMap<Character, Form[]> alphabet2 = readAlphaCharFromFile(file1);
			writeAlphaCharToFile(file2, alphabet2);
		}

		private static HashMap<Character,Form[]> readAlphaCharFromFile(File file) {
			HashMap<Character, Form[]> alphabet = new HashMap<Character,Form[]>();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				
				String line, value;
				Character ch = null;
				Vector<Form> forms = new Vector<>();
				
				while ( (line=in.readLine())!=null ) {
					if ( (value=getValue(line,"[AlphaChar '","']"))!=null ) {
						addTo(alphabet,ch,forms);
						Assert(value.length()==1);
						ch = value.charAt(0);
					}
					if ( (value=getValue(line,"Line="))!=null ) forms.add(new Line().setValues(toArray(value)));
					if ( (value=getValue(line,"Arc=" ))!=null ) forms.add(new Arc ().setValues(toArray(value)));
					if (line.isEmpty()) { addTo(alphabet,ch,forms); ch = null; }
				}
				addTo(alphabet,ch,forms);
				
			}
			catch (FileNotFoundException e) {}
			catch (IOException e) { e.printStackTrace(); }
			
			return alphabet;
		}
		
		private static double[] toArray(String str) {
			String[] valueStrs = str.split(";");
			double[] values = new double[valueStrs.length];
			for (int i=0; i<values.length; i++) {
				try { values[i] = Double.parseDouble(valueStrs[i]); }
				catch (NumberFormatException e) { values[i] = Double.NaN; }
				if (Double.isNaN(values[i])) {
					System.err.printf("Can't parse Double value (\"%s\") in String \"%s\" at position %d.%n", valueStrs[i], str, i);
					return null;
				}
			}
			return values;
		}

		private static void addTo(HashMap<Character, Form[]> alphabet, Character ch, Vector<Form> forms) {
			if (ch!=null && forms!=null && !forms.isEmpty()) {
				alphabet.put(ch, forms.toArray(new Form[forms.size()]));
				forms.clear();
			}
		}

		private static String getValue(String line, String prefix) { return getValue(line, prefix, null); }
		private static String getValue(String line, String prefix, String suffix) {
			if (prefix!=null) { if (line.startsWith(prefix)) line = line.substring(prefix.length()                ); else return null; }
			if (suffix!=null) { if (line.endsWith  (suffix)) line = line.substring(0,line.length()-suffix.length()); else return null; }
			return line;
		}
		
		private static void writeAlphaCharToFile(File file, HashMap<Character,Form[]> alphabet) {
			try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
				
				Vector<Character> keys = new Vector<>(alphabet.keySet());
				keys.sort(null);
				for (Character ch:keys) {
					out.printf("[AlphaChar '%s']%n", ch);
					Form[] forms = alphabet.get(ch);
					writeForms(out,forms);
					out.printf("%n");
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		private static void writeForms(PrintWriter out, Form[] forms) {
			for (Form form:forms)
				if (form instanceof PolyLine)
					writeForms(out,((PolyLine)form).toLineArray());
				else
					writeForm(out,form);
		}

		private static void writeForm(PrintWriter out, Form form) {
			double[] values = form.getValues();
			String name = form.getClass().getSimpleName();
			String valuesStr = String.join(";", Arrays.stream(values).mapToObj(d->Double.toString(d)).toArray(String[]::new));
			out.printf("%s=%s%n", name, valuesStr);
		}
	}

	public interface Form {
		double[] getValues();
		Form setValues(double[] values);
	}
	
	public static class PolyLine implements Form {
		
		private final Vector<Point> points;
		public PolyLine(double xStart, double yStart) {
			points = new Vector<>();
			points.add(new Point(xStart,yStart));
		}
		
		public PolyLine add(double x, double y) {
			points.add(new Point(x,y));
			return this;
		}
		
		public Line[] toLineArray() {
			if (points.size()<=1) return new Line[0];
			Line[] lines = new Line[points.size()-1];
			for (int i=1; i<points.size(); ++i) {
				Point p1 = points.get(i-1);
				Point p2 = points.get(i);
				lines[i-1] = new Line(p1.x, p1.y, p2.x, p2.y);
			}
			return lines;
		}
		
		@Override public double[] getValues() { throw new UnsupportedOperationException(); }
		@Override public PolyLine setValues(double[] values) { throw new UnsupportedOperationException(); }

		private static class Point {
			double x,y;
			private Point(double x, double y) { this.x = x; this.y = y; }
		}
	}
	
	public static class Line implements Form {
		private double x1, y1, x2, y2;
		public Line() { this(0,0,0,0); }
		public Line(double x1, double y1, double x2, double y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
		@Override public double[] getValues() {
			return new double[] { x1, y1, x2, y2 };
		}
		@Override public Line setValues(double[] values) {
			Assert(values.length==4);
			this.x1 = values[0];
			this.y1 = values[1];
			this.x2 = values[2];
			this.y2 = values[3];
			return this;
		}
	}
	
	public static class Arc implements Form {
		private double xC,yC,r,aStart,aEnd;
		public Arc() { this(0,0,0,0,0); }
		public Arc(double xC, double yC, double r, double aStart, double aEnd) {
			this.xC     = xC;
			this.yC     = yC;
			this.r      = r;
			this.aStart = aStart;
			this.aEnd   = aEnd;
		}
		@Override public double[] getValues() {
			return new double[] { xC,yC,r,aStart,aEnd };
		}
		@Override public Arc setValues(double[] values) {
			Assert(values.length==5);
			this.xC     = values[0];
			this.yC     = values[1];
			this.r      = values[2];
			this.aStart = values[3];
			this.aEnd   = values[4];
			return this;
		}
	}
}
