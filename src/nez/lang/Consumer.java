package nez.lang;

import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.lang.expr.Xsymbol;

public enum Consumer {
	Unconsumed, //
	Consumed, //
	Undecided;

	public final static class Analyzer extends Expression.Visitor {

		public Consumer quickCheck(Production p) {
			String uname = p.getUniqueName();
			if (this.isVisited(uname)) {
				return (Consumer) lookup(uname);
			}
			Consumer c = quickCheck(p.getExpression());
			if (c != Undecided) {
				this.memo(uname, c);
			}
			return c;
		}

		public Consumer deepCheck(Production p) {
			String uname = p.getUniqueName();
			if (this.isVisited(uname)) {
				return (Consumer) lookup(uname);
			}
			Consumer c = quickCheck(p.getExpression());
			this.memo(uname, c);
			if (c == Undecided) {
				c = deepCheck(p.getExpression());
				this.memo(uname, c);
			}
			return c;
		}

		public final Consumer quickCheck(Expression e) {
			return (Consumer) e.visit(this, true);
		}

		public final Consumer deepCheck(Expression e) {
			return (Consumer) e.visit(this, false);
		}

		public final boolean isConsumed(Production p) {
			Consumer c = deepCheck(p);
			return c == Consumed;
		}

		public final boolean isConsumed(Expression p) {
			Consumer c = deepCheck(p);
			return c == Consumed;
		}

		private Consumer check(Expression e, Object a) {
			return (Consumer) e.visit(this, a);
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			if ((Boolean) a) {
				return Undecided;
			}
			return this.deepCheck(e.getProduction());
		}

		@Override
		public Object visitPempty(Pempty e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitPfail(Pfail e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitCbyte(Cbyte e, Object a) {
			return Consumed;
		}

		@Override
		public Object visitCset(Cset e, Object a) {
			return Consumed;
		}

		@Override
		public Object visitCany(Cany e, Object a) {
			return Consumed;
		}

		@Override
		public Object visitCmulti(Cmulti e, Object a) {
			return Consumed;
		}

		@Override
		public Object visitPsequence(Psequence e, Object a) {
			if (check(e.getFirst(), a) == Consumed) {
				return Consumed;
			}
			return check(e.getNext(), a);
		}

		@Override
		public Object visitPchoice(Pchoice e, Object a) {
			boolean unconsumed = false;
			boolean undecided = false;
			for (Expression sub : e) {
				Consumer c = check(sub, a);
				if (c == Consumed) {
					continue;
				}
				unconsumed = true;
				if (c == Undecided) {
					undecided = true;
				}
			}
			if (!unconsumed) {
				return Consumed;
			}
			return undecided ? Undecided : Unconsumed;
		}

		@Override
		public Object visitPoption(Poption e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitPzero(Pzero e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitPone(Pone e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitPand(Pand e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitPnot(Pnot e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitTnew(Tnew e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitTlfold(Tlfold e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitTlink(Tlink e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitTtag(Ttag e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitTreplace(Treplace e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitTcapture(Tcapture e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitTdetree(Tdetree e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitXblock(Xblock e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitXlocal(Xlocal e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitXdef(Xsymbol e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitXmatch(Xmatch e, Object a) {
			return Undecided;
		}

		@Override
		public Object visitXis(Xis e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitXexists(Xexists e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitXindent(Xindent e, Object a) {
			return Undecided;
		}

		@Override
		public Object visitXif(Xif e, Object a) {
			return Unconsumed;
		}

		@Override
		public Object visitXon(Xon e, Object a) {
			return Unconsumed;
		}
	}
}