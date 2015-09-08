package nez.lang;

import nez.lang.expr.And;
import nez.lang.expr.Capture;
import nez.lang.expr.Choice;
import nez.lang.expr.DefSymbol;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.IsSymbol;
import nez.lang.expr.Link;
import nez.lang.expr.Match;
import nez.lang.expr.New;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Not;
import nez.lang.expr.Option;
import nez.lang.expr.Repetition;
import nez.lang.expr.Repetition1;
import nez.lang.expr.Replace;
import nez.lang.expr.Tagging;
import nez.util.UList;

public class Typestate extends GrammarReshaper {
	public final static int Undefined = -1;
	public final static int BooleanType = 0;
	public final static int ObjectType = 1;
	public final static int OperationType = 2;

	private int required = BooleanType;
	private GrammarFile checker;

	Typestate(GrammarFile checker) {
		this.checker = checker;
	}

	void reportInserted(Expression e, String operator) {
		checker.reportWarning(e.s, "expected " + operator + " .. => inserted!!");
	}

	void reportRemoved(Expression e, String operator) {
		checker.reportWarning(e.s, "unexpected " + operator + " .. => removed!!");
	}

	@Override
	public Expression reshapeProduction(Production p) {
		int t = checkNamingConvention(p.name);
		this.required = p.inferTypestate(null);
		if (t != Typestate.Undefined && this.required != t) {
			checker.reportNotice(p.s, "invalid naming convention: " + p.name);
		}
		p.setExpression(p.getExpression().reshape(this));
		return p;
	}

	private static int checkNamingConvention(String ruleName) {
		int start = 0;
		if (ruleName.startsWith("~") || ruleName.startsWith("\"")) {
			return Typestate.BooleanType;
		}
		for (; ruleName.charAt(start) == '_'; start++) {
			if (start + 1 == ruleName.length()) {
				return Typestate.BooleanType;
			}
		}
		boolean firstUpperCase = Character.isUpperCase(ruleName.charAt(start));
		for (int i = start + 1; i < ruleName.length(); i++) {
			char ch = ruleName.charAt(i);
			if (ch == '!')
				break; // option
			if (Character.isUpperCase(ch) && !firstUpperCase) {
				return Typestate.OperationType;
			}
			if (Character.isLowerCase(ch) && firstUpperCase) {
				return Typestate.ObjectType;
			}
		}
		return firstUpperCase ? Typestate.BooleanType : Typestate.Undefined;
	}

	@Override
	public Expression reshapeNew(New p) {
		if (p.leftFold) {
			if (this.required != Typestate.OperationType) {
				this.reportRemoved(p, "{@");
				return p.reshape(GrammarReshaper.RemoveASTandRename);
			}
		} else {
			if (this.required != Typestate.ObjectType) {
				this.reportRemoved(p, "{");
				return empty(p);
			}
		}
		this.required = Typestate.OperationType;
		return p;
	}

	@Override
	public Expression reshapeLink(Link p) {
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "@");
			p.inner = p.inner.reshape(GrammarReshaper.RemoveASTandRename);
		}
		this.required = Typestate.ObjectType;
		Expression inn = p.inner.reshape(this);
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "@");
			this.required = Typestate.OperationType;
			return updateInner(p, inn);
		}
		this.required = Typestate.OperationType;
		return updateInner(p, inn);
	}

	@Override
	public Expression reshapeMatch(Match p) {
		return p.inner.reshape(GrammarReshaper.RemoveASTandRename);
	}

	@Override
	public Expression reshapeTagging(Tagging p) {
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "#" + p.tag.getName());
			return empty(p);
		}
		return p;
	}

	@Override
	public Expression reshapeReplace(Replace p) {
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "`" + p.value + "`");
			return empty(p);
		}
		return p;
	}

	@Override
	public Expression reshapeCapture(Capture p) {
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "}");
			return empty(p);
		}
		return p;
	}

	@Override
	public Expression reshapeNonTerminal(NonTerminal p) {
		Production r = p.getProduction();
		int t = r.inferTypestate();
		if (t == Typestate.BooleanType) {
			return p;
		}
		if (this.required == Typestate.ObjectType) {
			if (t == Typestate.OperationType) {
				reportRemoved(p, "AST operations");
				return p.reshape(GrammarReshaper.RemoveASTandRename);
			}
			this.required = Typestate.OperationType;
			return p;
		}
		if (this.required == Typestate.OperationType) {
			if (t == Typestate.ObjectType) {
				reportInserted(p, "@");
				return ExpressionCommons.newLink(p.getSourcePosition(), null, p);
			}
		}
		return p;
	}

	@Override
	public Expression reshapeChoice(Choice p) {
		int required = this.required;
		int next = this.required;
		UList<Expression> l = ExpressionCommons.newList(p.size());
		for (Expression e : p) {
			this.required = required;
			ExpressionCommons.addChoice(l, e.reshape(this));
			if (this.required != required && this.required != next) {
				next = this.required;
			}
		}
		this.required = next;
		return ExpressionCommons.newChoice(p.getSourcePosition(), l);
	}

	@Override
	public Expression reshapeRepetition(Repetition p) {
		int required = this.required;
		Expression inn = p.inner.reshape(this);
		if (required != Typestate.OperationType && this.required == Typestate.OperationType) {
			checker.reportWarning(p.getSourcePosition(), "unable to create objects in repetition => removed!!");
			inn = inn.reshape(GrammarReshaper.RemoveASTandRename);
			this.required = required;
		}
		return updateInner(p, inn);
	}

	@Override
	public Expression reshapeRepetition1(Repetition1 p) {
		int required = this.required;
		Expression inn = p.inner.reshape(this);
		if (required != Typestate.OperationType && this.required == Typestate.OperationType) {
			checker.reportWarning(p.getSourcePosition(), "unable to create objects in repetition => removed!!");
			inn = inn.reshape(GrammarReshaper.RemoveASTandRename);
			this.required = required;
		}
		return updateInner(p, inn);
	}

	@Override
	public Expression reshapeOption(Option p) {
		int required = this.required;
		Expression inn = p.inner.reshape(this);
		if (required != Typestate.OperationType && this.required == Typestate.OperationType) {
			checker.reportWarning(p.getSourcePosition(), "unable to create objects in repetition => removed!!");
			inn = inn.reshape(GrammarReshaper.RemoveASTandRename);
			this.required = required;
		}
		return updateInner(p, inn);
	}

	@Override
	public Expression reshapeAnd(And p) {
		if (this.required == Typestate.ObjectType) {
			this.required = Typestate.BooleanType;
			Expression inn = p.inner.reshape(this);
			this.required = Typestate.ObjectType;
			return updateInner(p, inn);
		}
		return updateInner(p, p.inner.reshape(this));
	}

	@Override
	public Expression reshapeNot(Not p) {
		int t = p.inner.inferTypestate(null);
		if (t == Typestate.ObjectType || t == Typestate.OperationType) {
			updateInner(p, p.inner.reshape(GrammarReshaper.RemoveASTandRename));
		}
		return p;
	}

	@Override
	public Expression reshapeDefSymbol(DefSymbol p) {
		int t = p.inner.inferTypestate(null);
		if (t != Typestate.BooleanType) {
			updateInner(p, p.inner.reshape(GrammarReshaper.RemoveASTandRename));
		}
		return p;
	}

	@Override
	public Expression reshapeIsSymbol(IsSymbol p) {
		Expression e = p.getSymbolExpression();
		if (e == null) {
			checker.reportError(p.getSourcePosition(), "undefined table: " + p.getTableName());
			return ExpressionCommons.newFailure(p.getSourcePosition());
		}
		return p;
	}

	// @Override
	// public Expression reshapeBlock(Block p) {
	// p.inner = p.inner.reshape(this);
	// return p;
	// }

}
