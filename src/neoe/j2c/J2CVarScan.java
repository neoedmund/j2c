package neoe.j2c;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/***
 * Excerpted from "The Definitive ANTLR 4 Reference",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/tpantlr2 for more book information.
***/
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.TerminalNode;

import neoe.j2c.JavaParser.BlockStatementContext;
import neoe.j2c.JavaParser.ClassBodyDeclarationContext;
import neoe.j2c.JavaParser.ClassDeclarationContext;
import neoe.j2c.JavaParser.FieldDeclarationContext;
import neoe.j2c.JavaParser.FormalParameterListContext;
import neoe.j2c.JavaParser.FormalParametersContext;
import neoe.j2c.JavaParser.LocalVariableDeclarationStatementContext;
import neoe.j2c.JavaParser.MemberDeclarationContext;
import neoe.j2c.JavaParser.MethodDeclarationContext;
import neoe.j2c.JavaParser.PackageDeclarationContext;
import neoe.j2c.JavaParser.VariableDeclaratorContext;

/**
 * Hope to solve 80% of problem of java to c. 1. change method signature(name,
 * parameters)<br/>
 * 2. change field variable<br/>
 * 3. change type define<br/>
 * 
 * @author neoe
 *
 */
public class J2CVarScan extends JavaBaseListener {
	TokenStreamRewriter rewriter;
	private String pkg;
	/** support nested class */
	private Stack<String> clsNames = new Stack<String>();

	public J2CVarScan(TokenStream tokens) {
		rewriter = new TokenStreamRewriter(tokens);
	}

	@Override
	public void enterPackageDeclaration(PackageDeclarationContext ctx) {
		pkg = ctx.qualifiedName().getText();
		System.out.println("package " + pkg);
	}

	@Override
	public void exitClassDeclaration(ClassDeclarationContext ctx) {
		clsNames.pop(); // remove last one
		
	}

	Stack<List<String>> clsFieldList = new Stack();

	@Override
	public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
		System.out.println("enter class [" + ctx.Identifier() + "|" + ctx.start + "," + ctx.stop + "]");
		clsNames.add(ctx.Identifier().getText());
		System.out.println("cls=" + getCurrentClassName());
		JavaParser.ClassBodyContext body = ctx.classBody();
		for (ClassBodyDeclarationContext cbd : body.classBodyDeclaration()) {
			MemberDeclarationContext md = cbd.memberDeclaration();
			if (md != null) {
				{
					FieldDeclarationContext fd = md.fieldDeclaration();
					if (fd != null) {
						List<String> clsFields = new ArrayList();
						clsFieldList.add(clsFields);
						List<String> fn;
						System.out.println(fn = getFieldName(fd.variableDeclarators().variableDeclarator()));
						clsFields.addAll(fn);
					}
				}
				{
					MethodDeclarationContext med = md.methodDeclaration();
					if (med != null) {
						TerminalNode id = med.Identifier();
						rewriter.replace(id.getSymbol(), getCurrentClassName() + "_" + id.getText());
						FormalParametersContext fp = med.formalParameters();
						FormalParameterListContext fpl = fp.formalParameterList();
						if (fpl == null) {
							rewriter.insertAfter(fp.start.getTokenIndex(), getCurrentClassName() + "* self");
						} else {
							rewriter.insertAfter(fp.start.getTokenIndex(), getCurrentClassName() + "* self, ");
						}
					}
				}
			}
		}
	}

	private String getCurrentClassName() {
		StringBuilder sb = new StringBuilder();
		sb.append(pkg).append('.');
		for (String clsName : clsNames) {
			sb.append(clsName).append('.');
		}
		sb.deleteCharAt(sb.length() - 1);
		String s = sb.toString();
		s = s.replace('.', '_');
		return s;
	}

	private List<String> getFieldName(List<VariableDeclaratorContext> list) {
		List<String> ret = new ArrayList<String>();
		for (VariableDeclaratorContext vd : list) {
			ret.add(vd.variableDeclaratorId().getText());
		}
		return ret;
	}

	@Override
	public void enterBlock(JavaParser.BlockContext ctx) {
		System.out.println("enter block [" + ctx.start + "," + ctx.stop + "]");
		for (BlockStatementContext bs : ctx.blockStatement()) {
			LocalVariableDeclarationStatementContext lvds = bs.localVariableDeclarationStatement();
			if (lvds != null) {
				System.out.println(
						getFieldName(lvds.localVariableDeclaration().variableDeclarators().variableDeclarator()));
			}
		}
	}
}
