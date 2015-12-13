package nez.lang.expr;

import nez.ast.SourcePosition;
import nez.lang.Conditional;
import nez.lang.Expression;
import nez.lang.ExpressionVisitor;
import nez.lang.PossibleAcceptance;
import nez.lang.Typestate;
import nez.lang.Visa;
import nez.parser.AbstractGenerator;
import nez.parser.moz.MozInst;

public class Xif extends Term implements Conditional {
	boolean predicate;
	String flagName;

	Xif(SourcePosition s, boolean predicate, String flagName) {
		super(s);
		if (flagName.startsWith("!")) {
			predicate = false;
			flagName = flagName.substring(1);
		}
		this.predicate = predicate;
		this.flagName = flagName;
	}

	@Override
	public final boolean equalsExpression(Expression o) {
		if (o instanceof Xif) {
			Xif e = (Xif) o;
			return this.predicate == e.predicate && this.flagName.equals(e.flagName);
		}
		return false;
	}

	public final String getFlagName() {
		return this.flagName;
	}

	public boolean isPredicate() {
		return predicate;
	}

	@Override
	public Object visit(ExpressionVisitor v, Object a) {
		return v.visitXif(this, a);
	}

	@Override
	public boolean isConsumed() {
		return false;
	}

	@Override
	public int inferTypestate(Visa v) {
		return Typestate.BooleanType;
	}

	@Override
	public short acceptByte(int ch) {
		return PossibleAcceptance.Unconsumed;
	}

	@Override
	public MozInst encode(AbstractGenerator bc, MozInst next, MozInst failjump) {
		return bc.encodeXif(this, next, failjump);
	}

}