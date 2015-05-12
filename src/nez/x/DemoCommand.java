package nez.x;

import nez.NameSpace;
import nez.Grammar2;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.expr.GrammarChecker;
import nez.expr.NezParser;
import nez.expr.Production;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.util.ConsoleUtils;

public class DemoCommand extends Command {
	@Override
	public String getDesc() {
		return "Don't try this";
	}

	@Override
	public void exec(CommandConfigure config) {
		Grammar2 product = config.getProduction();
		NameSpace peg = product.getStartRule().getNameSpace();
		String start = config.StartingPoint;
		NezParser parser = new NezParser();
		boolean grammarAdded = false;
		String line = null;
		int linenum = 0;
		while((line = readMultiLine(start + "? ", "")) != null) {
			linenum++;
			if(line.startsWith("\\")) {
				String s = line.substring(1);
				Grammar2 p = peg.newProduction(s);
				if(p == null) {
					ConsoleUtils.println("Undefined Rule: " + s);
					ConsoleUtils.println("Rules: " + peg.getDefinedRuleList());
					ConsoleUtils.println("To switch the rule, type \\Rule");
					continue;
				}
				for(Production r : p.getRuleList()) {
					ConsoleUtils.println(r.getLocalName() + " = " + r.getExpression());
				}
				start = line.substring(1);
				product = p;
				continue;
			}
			int loc = line.indexOf("=");
			if(loc > 0) {
				Production r = parser.parseRule(peg, "<stdin>", linenum, line);
				if(r != null) {
					grammarAdded = true;
					start = r.getLocalName();
					continue;
				}
			}
			if(grammarAdded) {
				new GrammarChecker().verify(peg);
				product = peg.newProduction(start);
				assert(product != null);
				grammarAdded = false;
			}
			SourceContext sc = SourceContext.newStringSourceContext("<stdio>", linenum, line);
			CommonTree node = product.parse(sc);
			if(node == null) {
				ConsoleUtils.println(sc.getSyntaxErrorMessage());
				continue;
			}
			ConsoleUtils.println(node + "\n");
		}
	}
	
}
