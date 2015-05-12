package nez.main;

import nez.Grammar2;
import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeWriter;
import nez.util.ConsoleUtils;
import nez.util.UList;

class ParseCommand extends Command {
	@Override
	public String getDesc() {
		return "AST parser";
	}

	@Override
	public void exec(CommandConfigure config) {
		Recorder rec = config.getRecorder();
		Grammar2 p = config.getProduction();
		p.record(rec);
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			file.start(rec);
//			CommonTree.gcCount = 0;
			CommonTree node = p.parse(file);
			file.done(rec);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			file = null;
			if(rec != null) {
				record(rec, node);
				rec.log();
			}
			new CommonTreeWriter().transform(config.getOutputFileName(file), node);
		}
	}
	
	private void record(Recorder rec, CommonTree node) {
		rec.setCount("O.Size", node.count());
		System.gc();
		long t1 = System.nanoTime();
		CommonTree t = node.dup();
		long t2 = System.nanoTime();
		Recorder.recordLatencyMS(rec, "O.Overhead", t1, t2);
//		rec.setCount("O.GC", CommonTree.gcCount);
	}

}

class CheckCommand extends Command {
	@Override
	public String getDesc() {
		return "grammar validator";
	}

	@Override
	public
	void exec(CommandConfigure config) {
		UList<String> failedInput = new UList<String>(new String[4]);
		Recorder rec = config.getRecorder();
		Grammar2 product = config.getProduction();
		product.disable(Grammar2.ASTConstruction);
		product.record(rec);
		int totalCount = 0, failureCount = 0, unconsumedCount = 0;
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			totalCount++;
			file.start(rec);
			boolean result = product.match(file);
			file.done(rec);
			product.verboseMemo();
			if(!result) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				failedInput.add(file.getResourceName());
				failureCount++;
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
				unconsumedCount++;
			}
			if(rec != null) {
				rec.log();
			}
		}
		if(totalCount > 1){
			Verbose.println(
					totalCount + " files, " +
					failureCount + " failed, " +
					unconsumedCount + " uncosumed, " +
					(100 - 100.0 * (unconsumedCount+failureCount)/totalCount) + "% passed.");
		}
		if(failedInput.size() > 0) {
			ConsoleUtils.exit(1, "failed: " + failedInput);
		}
	}

}


