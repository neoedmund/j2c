package neoe.j2c;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class J2C {
	public static void main(String[] args) throws Exception {
		String inputFile = null;
		if (args.length > 0)
			inputFile = args[0];
		InputStream is;
		if (inputFile == null) {
			System.out.println("param javafilename");
			return;
		}
		{
			ANTLRInputStream input = new ANTLRInputStream(toReader(new FileInputStream(inputFile)));

			JavaLexer lexer = new JavaLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer, Token.DEFAULT_CHANNEL);
			JavaParser parser = new JavaParser(tokens);
			ParseTree tree = parser.compilationUnit(); // parse

			ParseTreeWalker walker = new ParseTreeWalker(); // create standard
															// walker
			J2CVarScan varScan = new J2CVarScan(tokens);
			walker.walk(varScan, tree); // initiate walk of tree with listener

			// print back ALTERED stream
			String cfn = new File(inputFile).getAbsolutePath() + ".j2.c";
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cfn), "utf8"));
			out.println(varScan.rewriter.getText());
			out.close();
			System.out.println("write to " + cfn);
		}
		{
			ANTLRInputStream input = new ANTLRInputStream(toReader(new FileInputStream(inputFile)));

			JavaLexer lexer = new JavaLexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer, Token.DEFAULT_CHANNEL);
			JavaParser parser = new JavaParser(tokens);
			ParseTree tree = parser.compilationUnit(); // parse

			ParseTreeWalker walker = new ParseTreeWalker(); // create standard
															// walker
			J2CVarScanH varScan = new J2CVarScanH(tokens);
			walker.walk(varScan, tree); // initiate walk of tree with listener

			// print back ALTERED stream
			String cfn = new File(inputFile).getAbsolutePath() + ".j2.h";
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cfn), "utf8"));
			out.println(varScan.rewriter.getText());
			out.close();
			System.out.println("write to " + cfn);
		}
	}

	private static Reader toReader(InputStream in) throws UnsupportedEncodingException {
		return new InputStreamReader(in, "UTF8");
	}
}
