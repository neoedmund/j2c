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
import neoe.j2c.JavaParser.ClassBodyDeclarationContext;
import neoe.j2c.JavaParser.ClassDeclarationContext;
import neoe.j2c.JavaParser.CompilationUnitContext;
import neoe.j2c.JavaParser.ConstructorDeclarationContext;
import neoe.j2c.JavaParser.CreatorContext;
import neoe.j2c.JavaParser.FieldDeclarationContext;
import neoe.j2c.JavaParser.FormalParameterContext;
import neoe.j2c.JavaParser.FormalParameterListContext;
import neoe.j2c.JavaParser.FormalParametersContext;
import neoe.j2c.JavaParser.ImportDeclarationContext;
import neoe.j2c.JavaParser.MemberDeclarationContext;
import neoe.j2c.JavaParser.MethodDeclarationContext;
import neoe.j2c.JavaParser.ModifierContext;
import neoe.j2c.JavaParser.PackageDeclarationContext;
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
public class J2CVarScanH extends JavaBaseListener {
	TokenStreamRewriter rewriter;
	private String pkg;
	/** support nested class */
	private Stack<String> clsNames = new Stack<String>();
	private TokenStream tokens;

	public J2CVarScanH(TokenStream tokens) {
		rewriter = new TokenStreamRewriter(tokens);
		this.tokens = tokens;
	}

	@Override
	public void enterPackageDeclaration(PackageDeclarationContext ctx) {
		pkg = ctx.qualifiedName().getText();
		rewriter.insertBefore(ctx.start, "// ");
		// System.out.println("package " + pkg);
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
	private String firstClassName;

	@Override
	public void exitTypeDeclaration(TypeDeclarationContext ctx) {
		ClassDeclarationContext cd = ctx.classDeclaration();
		if (cd != null) {
			String clsName = firstClassName;
			rewriter.insertBefore(ctx.start, "// ");
			rewriter.insertBefore(cd.stop, "// ");
			rewriter.insertAfter(cd.stop, "/*cls*/\n");
		}
		// rewriter.insertBefore(body.start, );

	}

	@Override
	public void exitCompilationUnit(CompilationUnitContext ctx) {
		String clsName = firstClassName;
		rewriter.insertBefore(ctx.start, String.format("#ifndef _%s_H\n#define _%s_H\n", clsName, clsName));
		rewriter.insertAfter(ctx.stop, String.format("#endif\n"));
	}

	@Override
	public void enterCreator(CreatorContext ctx) {
		int prev;
		String t = tokens.get(prev = ctx.start.getTokenIndex() - 2).getText();
		if ("new".equals(t)) {
			rewriter.replace(prev, "");
			rewriter.replace(ctx.createdName().start, ctx.createdName().getText() + "_Init");
		} else {
			System.out.println("warn: new!=" + t);
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
	
	static Map primMap;

	static Map getPrimMap() throws Exception {
		if (primMap == null)
			primMap = (Map) PyData.parseAll("{byte:uint8_t,char:uint16_t,short:uint16_t,int:uint32_t,long:uint64_t}");
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
		rewriter.replace(ctx.Identifier().getSymbol(), cls  + "_Init");

		//
		assert methodParams.isEmpty();
		FormalParametersContext fp = ctx.formalParameters();
		FormalParameterListContext fpl = fp.formalParameterList();
		if (fpl == null) {
			rewriter.insertAfter(fp.start, cls + "* self");
		} else {
			rewriter.insertAfter(fp.start, cls + "* self, ");
			getParamNames(fpl.formalParameter(), methodParams, fpl);
			// System.out.println("params of " + ctx.Identifier().getText() +
			// ":" + methodParams);
		}

	}

	@Override
	public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
		// System.out.println("enter class [" + ctx.Identifier() + "|" +
		// ctx.start + "," + ctx.stop + "]");
		clsNames.add(ctx.Identifier().getText());

		String clsName;
		System.out.println("cls=" + (clsName = getCurrentClassName()));
		if (firstClassName == null)
			firstClassName = clsName;
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
			// System.out.println("params of " + ctx.Identifier().getText() +
			// ":" + methodParams);
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
	public void exitBlock(BlockContext ctx) {
		rewriter.replace(ctx.start, ctx.stop, ";\n");
	}

}
