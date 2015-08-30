package nez.vm;

import nez.NezOption;
import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.MatchSymbol;
import nez.lang.MultiChar;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;

public class PlainCompiler extends NezCompiler {

	protected final Instruction commonFailure = new IFail(null);

	public PlainCompiler(NezOption option) {
		super(option);
	}

	// encoding

	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	public Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump) {
		return new IAny(p, next);
	}

	public Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump) {
		return new IByte(p, next);
	}

	public Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump) {
		return new ISet(p, next);
	}

	@Override
	public Instruction encodeMultiChar(MultiChar p, Instruction next, Instruction failjump) {
		return new IStr(p, next);
	}

	public Instruction encodeFail(Expression p) {
		return this.commonFailure;
	}

	public Instruction encodeOption(Option p, Instruction next) {
		Instruction pop = new ISucc(p, next);
		return new IAlt(p, next, encode(p.get(0), pop, next));
	}

	public Instruction encodeRepetition(Repetition p, Instruction next) {
		// Expression skip = p.possibleInfiniteLoop ? new ISkip(p) : new
		// ISkip(p);
		Instruction skip = new ISkip(p);
		Instruction start = encode(p.get(0), skip, next/* FIXME */);
		skip.next = start;
		return new IAlt(p, next, start);
	}

	public Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump) {
		return encode(p.get(0), this.encodeRepetition(p, next), failjump);
	}

	public Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
		Instruction inner = encode(p.get(0), new IBack(p, next), failjump);
		return new IPos(p, inner);
	}

	public Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		Instruction fail = new ISucc(p, new IFail(p));
		return new IAlt(p, next, encode(p.get(0), fail, failjump));
	}

	public Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump) {
		// return encode(p.get(0), encode(p.get(1), next, failjump), failjump);
		Instruction nextStart = next;
		for (int i = p.size() - 1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encode(e, nextStart, failjump);
		}
		return nextStart;
	}

	public Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		Instruction nextChoice = encode(p.get(p.size() - 1), next, failjump);
		for (int i = p.size() - 2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IAlt(e, nextChoice, encode(e, new ISucc(e, next), nextChoice));
		}
		return nextChoice;
	}

	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production r = p.getProduction();
		return new ICall(r, next);
	}

	// AST Construction

	public Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		if (this.option.enabledASTConstruction) {
			next = new ITPop(p, next);
			next = encode(p.get(0), next, failjump);
			return new ITPush(p, next);
		}
		return encode(p.get(0), next, failjump);
	}

	public Instruction encodeNew(New p, Instruction next) {
		if (this.option.enabledASTConstruction) {
			return p.leftFold ? new ITLeftFold(p, next) : new INew(p, next);
		}
		return next;
	}

	public Instruction encodeCapture(Capture p, Instruction next) {
		if (this.option.enabledASTConstruction) {
			return new ICapture(p, next);
		}
		return next;
	}

	public Instruction encodeTagging(Tagging p, Instruction next) {
		if (this.option.enabledASTConstruction) {
			return new ITag(p, next);
		}
		return next;
	}

	public Instruction encodeReplace(Replace p, Instruction next) {
		if (this.option.enabledASTConstruction) {
			return new IReplace(p, next);
		}
		return next;
	}

	public Instruction encodeBlock(Block p, Instruction next, Instruction failjump) {
		next = new IEndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new IBeginSymbolScope(p, next);
	}

	public Instruction encodeLocalTable(LocalTable p, Instruction next, Instruction failjump) {
		next = new IEndSymbolScope(p, next);
		next = encode(p.get(0), next, failjump);
		return new IBeginLocalScope(p, next);
	}

	public Instruction encodeDefSymbol(DefSymbol p, Instruction next, Instruction failjump) {
		return new IPos(p, encode(p.get(0), new IDefSymbol(p, next), failjump));
	}

	public Instruction encodeExistsSymbol(ExistsSymbol p, Instruction next, Instruction failjump) {
		String symbol = p.getSymbol();
		if (symbol == null) {
			return new IExists(p, next);
		} else {
			return new IExistsSymbol(p, next);
		}
	}

	public Instruction encodeMatchSymbol(MatchSymbol p, Instruction next, Instruction failjump) {
		return new IMatch(p, next);
	}

	public Instruction encodeIsSymbol(IsSymbol p, Instruction next, Instruction failjump) {
		if (p.is) {
			return new IPos(p, encode(p.getSymbolExpression(), new IIsSymbol(p, next), failjump));
		} else {
			return new IPos(p, encode(p.getSymbolExpression(), new IIsaSymbol(p, next), failjump));
		}
	}

	public Instruction encodeDefIndent(DefIndent p, Instruction next, Instruction failjump) {
		return new IDefIndent(p, next);
	}

	public Instruction encodeIsIndent(IsIndent p, Instruction next, Instruction failjump) {
		return new IIsIndent(p, next);
	}

	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		return next;
	}

}
