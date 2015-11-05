package neoe.j2c;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import neoe.j2c.JavaParser.BlockContext;
import neoe.j2c.JavaParser.BlockStatementContext;
import neoe.j2c.JavaParser.ClassBodyDeclarationContext;
import neoe.j2c.JavaParser.ClassDeclarationContext;
import neoe.j2c.JavaParser.ConstructorDeclarationContext;
import neoe.j2c.JavaParser.CreatorContext;
import neoe.j2c.JavaParser.ExpressionContext;
import neoe.j2c.JavaParser.FieldDeclarationContext;
import neoe.j2c.JavaParser.FormalParameterContext;
import neoe.j2c.JavaParser.FormalParameterListContext;
import neoe.j2c.JavaParser.FormalParametersContext;
import neoe.j2c.JavaParser.ImportDeclarationContext;
import neoe.j2c.JavaParser.LiteralContext;
import neoe.j2c.JavaParser.LocalVariableDeclarationContext;
import neoe.j2c.JavaParser.LocalVariableDeclarationStatementContext;
import neoe.j2c.JavaParser.MemberDeclarationContext;
import neoe.j2c.JavaParser.MethodDeclarationContext;
import neoe.j2c.JavaParser.ModifierContext;
import neoe.j2c.JavaParser.PackageDeclarationContext;
import neoe.j2c.JavaParser.PrimaryContext;
import neoe.j2c.JavaParser.StatementContext;
import neoe.j2c.JavaParser.TypeContext;
import neoe.j2c.JavaParser.TypeDeclarationContext;
import neoe.j2c.JavaParser.VariableDeclaratorContext;
import neoe.util.PyData;

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
	private TokenStream tokens;

	public J2CVarScan(TokenStream tokens) {
		rewriter = new TokenStreamRewriter(tokens);
		this.tokens = tokens;
	}

	@Override
	public void enterPackageDeclaration(PackageDeclarationContext ctx) {
		pkg = ctx.qualifiedName().getText();
		rewriter.insertBefore(ctx.start, "// ");
		System.out.println("package " + pkg);
	}

	@Override
	public void exitClassDeclaration(ClassDeclarationContext ctx) {
		clsNames.pop(); // remove last one
		clsFieldList.pop();
		clsMethodList.pop();

	}

	@Override
	public void enterImportDeclaration(ImportDeclarationContext ctx) {
		rewriter.insertBefore(ctx.start, "// ");
	}

	Stack<Map<String, String>> clsFieldList = new Stack();
	Stack<Map<String, String>> clsMethodList = new Stack();
	Stack<Map<String, String>> blockVarList = new Stack();

	@Override
	public void enterTypeDeclaration(TypeDeclarationContext ctx) {
		ClassDeclarationContext cd = ctx.classDeclaration();
		if (cd != null) {
			rewriter.insertBefore(ctx.start, "// ");
			rewriter.insertBefore(cd.stop, "// ");
			rewriter.insertAfter(cd.stop, "/*cls*/\n");
		}
	}

	@Override
	public void enterCreator(CreatorContext ctx) {
		int p1 = ctx.start.getTokenIndex() - 2;
		String t1 = tokens.get(p1).getText();
		if ("new".equals(t1)) {
			rewriter.replace(p1, " _localvar_x; ");
			rewriter.replace(ctx.createdName().start.getTokenIndex(), ctx.createdName().start.getTokenIndex() + 1,
					ctx.createdName().getText() + "_Init(&_localvar_x, ");
		} else {
			System.out.println("warn: new!=" + t1);
		}
	}

	@Override
	public void enterType(TypeContext ctx) {
		String org = ctx.getText();
		String t = getTypeText(ctx);
		if (!isPrimitiveType(org)) {
			rewriter.insertAfter(ctx.start, "*");
		} else {
			rewriter.replace(ctx.start, ctx.stop, t);

		}
	}

	static Map primMap;

	static Map getPrimMap() throws Exception {
		if (primMap == null)
			primMap = (Map) PyData.parseAll("{byte:uint8_t,char:uint16_t,short:uint16_t,int:uint32_t,long:uint64_t,boolean:bool}");
		return primMap;
	}

	private String replacePrimitiveType(String t) {
		String s;
		try {
			int p0 = t.indexOf("[");
			if (p0 < 0) {
				p0 = t.indexOf("*");
			}
			if (p0 > 0) {
				s = (String) getPrimMap().get(t.substring(0, p0));
				if (s != null) {
					s = s + t.substring(p0);
				}
			} else {
				s = (String) getPrimMap().get(t);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return t;
		}
		if (s == null)
			s = t;
		return s;
	}

	@Override
	public void enterConstructorDeclaration(ConstructorDeclarationContext ctx) {
		String cls = getCurrentClassName();
		String text = ctx.Identifier().getText();
		rewriter.insertBefore(ctx.Identifier().getSymbol(), "void ");
		rewriter.replace(ctx.Identifier().getSymbol(), cls + "_Init");

		//
		assert methodParams.isEmpty();
		FormalParametersContext fp = ctx.formalParameters();
		FormalParameterListContext fpl = fp.formalParameterList();
		if (fpl == null) {
			rewriter.insertAfter(fp.start, cls + "* self");
		} else {
			rewriter.insertAfter(fp.start, cls + "* self, ");
			getParamNames(fpl.formalParameter(), methodParams, fpl);
			System.out.println("params of " + ctx.Identifier().getText() + ":" + methodParams);
		}

	}

	@Override
	public void exitConstructorDeclaration(ConstructorDeclarationContext ctx) {
		methodParams.clear();
	}

	@Override
	public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
		System.out.println("enter class [" + ctx.Identifier() + "|" + ctx.start + "," + ctx.stop + "]");
		clsNames.add(ctx.Identifier().getText());

		String clsName;
		System.out.println("cls=" + (clsName = getCurrentClassName()));
		JavaParser.ClassBodyContext body = ctx.classBody();
		Map<String, String> clsFields = new HashMap();
		clsFieldList.add(clsFields);

		HashMap ml;
		clsMethodList.add(ml = new HashMap());
		for (ClassBodyDeclarationContext cbd : body.classBodyDeclaration()) {
			MemberDeclarationContext md = cbd.memberDeclaration();
			if (md != null) {
				{
					FieldDeclarationContext fd = md.fieldDeclaration();
					if (fd != null) {

						getFieldName(fd.variableDeclarators().variableDeclarator(), clsFields, getTypeText(fd.type()));
						rewriter.insertBefore(fd.start, "// ");
					}

				}
				{
					MethodDeclarationContext med = md.methodDeclaration();
					if (med != null) {
						TerminalNode id = med.Identifier();
						ml.put(id.getText(), getTypeText(med.type()));
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

		String def = addClassDef(clsName, clsFields);
		rewriter.insertAfter(ctx.classBody().start, def);
		System.out.println("clsFieldList=" + clsFieldList);
	}

	private String addClassDef(String clsName, Map<String, String> clsFields) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n typedef struct { \n");
		for (String name : clsFields.keySet()) {
			sb.append("\t").append(clsFields.get(name)).append(" ").append(name).append(";\n");
		}
		sb.append(" } ");
		sb.append(clsName).append(";   \n");
		return sb.toString();
	}

	private String getTypeText(TypeContext type) {
		if (type == null)
			return "void";
		return replacePrimitiveType(type.getText());
	}

	Map<String, String> methodParams = new HashMap();

	@Override
	public void enterModifier(ModifierContext ctx) {
		rewriter.insertBefore(ctx.start, "/*");
		rewriter.insertAfter(ctx.stop, "*/\n");
	}

	@Override
	public void enterMethodDeclaration(MethodDeclarationContext ctx) {
		assert methodParams.isEmpty();
		FormalParametersContext fp = ctx.formalParameters();
		FormalParameterListContext fpl = fp.formalParameterList();
		if (fpl == null) {
		} else {
			getParamNames(fpl.formalParameter(), methodParams, fpl);
			System.out.println("params of " + ctx.Identifier().getText() + ":" + methodParams);
		}
	}

	private void getParamNames(List<FormalParameterContext> list, Map<String, String> map,
			FormalParameterListContext fpl) {
		for (FormalParameterContext fp : list) {
			map.put(fp.variableDeclaratorId().getText(), getTypeText(fp.type()));
		}
	}

	@Override
	public void exitMethodDeclaration(MethodDeclarationContext ctx) {
		methodParams.clear();
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

	private void getFieldName(List<VariableDeclaratorContext> list, Map<String, String> map, String type) {
		for (VariableDeclaratorContext vd : list) {
			String text = vd.variableDeclaratorId().getText();
			map.put(text, type);
		}
	}

	@Override
	public void enterLiteral(LiteralContext ctx) {

	}

	@Override
	public void enterPrimary(PrimaryContext ctx) {

		{
			// this. -> self->
			String text = ctx.getText();
			if ("this".equals(text)) {
				rewriter.replace(ctx.start, "self");
				int next = ctx.start.getTokenIndex() + 1;
				if (".".equals(tokens.get(next).getText())) {
					rewriter.replace(next, "->");
				}
				return;
			}

		}

		TerminalNode id = ctx.Identifier();
		String clsName = getCurrentClassName();

		if (id != null) {
			boolean isClassMethod = false;
			String text = id.getText();
			System.out.println("enterPrimary:id=" + text);
			String type[] = new String[1];
			if (containsFromList(blockVarList, text, type)) {
			} else if (methodParams.containsKey(text)) {
				type[0] = methodParams.get(text);
			} else if (containsFromList(clsFieldList, text, type)) {
				// type[0] = null;// for no translate below
				text = "self->" + text;
				System.out.println("enterPrimary:replaceField=" + text);
				rewriter.replace(id.getSymbol(), text);
				// rewriter.insertBefore(id.getSymbol(), );
			} else if (containsFromList(clsMethodList, text, type)) {
				text = clsName + "_" + text;
				rewriter.replace(id.getSymbol(), text);
				isClassMethod = true;
			} else {
				System.out.println("warn:unknow id:" + text);
			}
			int next = id.getSymbol().getTokenIndex() + 1;
			String t = tokens.get(next).getText();
			// System.out.println("[debug1105-enterPrimary]" + text + "|" + t);

			if (!isPrimitiveType(type[0])) {
				if (".".equals(t)) {
					if (type[0] != null) {
						// caller.push(text);

						int p3 = next + 1 + 1;
						String t3 = tokens.get(p3).getText();
						String t4 = tokens.get(p3 + 1).getText();
						// System.out.println("[debug1105-push]" + text + "|" +
						// t3 + "|" + t4);
						if (t3.equals("(")) {
							rewriter.replace(id.getSymbol(), type[0].toString());
							rewriter.replace(next, "_");
							rewriter.replace(p3, "(" + text + (t4.equals(")") ? "" : ", "));
						} else {
							rewriter.replace(next, "->");
						}
					} else {

					}

				} else if (isClassMethod && "(".equals(t)) { // (this.)method()
					int p3 = next;
					String t4 = tokens.get(p3 + 1).getText();
					rewriter.replace(p3, "(self" + (t4.equals(")") ? "" : ", "));
				}
			} else {

			}
		}
	}

	static Set<String> primitity;

	static {
		try {
			primitity = new HashSet((Collection) PyData.parseAll("[byte,short,int,long,float,double,char,boolean]"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isPrimitiveType(String s) {
		return primitity.contains(s);
	}

	private boolean containsFromList(Stack<Map<String, String>> list, String text, String[] type) {
		for (Map<String, String> lvl : list) {
			if (lvl.containsKey(text)) {
				type[0] = lvl.get(text);
				return true;
			}
		}
		return false;
	}

	@Override
	public void enterLocalVariableDeclaration(LocalVariableDeclarationContext ctx) {

		getFieldName(ctx.variableDeclarators().variableDeclarator(), blockVarList.peek(), getTypeText(ctx.type()));
	}

	@Override
	public void exitBlock(BlockContext ctx) {
		blockVarList.pop();
	}

	@Override
	public void enterBlock(JavaParser.BlockContext ctx) {
		// System.out.println("enter block [" + ctx.start + "," + ctx.stop);
		Map<String, String> map = new HashMap();
		for (BlockStatementContext bs : ctx.blockStatement()) {
			LocalVariableDeclarationStatementContext lvds = bs.localVariableDeclarationStatement();
			if (lvds != null) {
				getFieldName(lvds.localVariableDeclaration().variableDeclarators().variableDeclarator(), map,
						getTypeText(lvds.localVariableDeclaration().type()));
				// System.out.println("local:" + map);
			}
		}
		// System.out.println(" --- ]");
		blockVarList.add(map);
	}
}
