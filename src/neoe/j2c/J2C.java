package neoe.j2c;

/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class J2C {
	public static void main(String[] args) throws Exception {
		String inputFile = null;
		if (args.length > 0)
			inputFile = args[0];
		InputStream is = System.in;
		if (inputFile != null) {
			is = new FileInputStream(inputFile);
		}
		ANTLRInputStream input = new ANTLRInputStream(is);

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
}
