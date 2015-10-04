package nez.ext;

import java.io.IOException;

import nez.ast.script.EmptyResult;
import nez.ast.script.ScriptContext;
import nez.ast.script.ScriptRuntimeException;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;

public class Ckonoha extends Command {

	@Override
	public void exec(CommandContext config) throws IOException {
		ScriptContext sc = new ScriptContext(config.newParser());
		show();
		int linenum = 1;
		String command = null;
		while ((command = readLine()) != null) {
			if (command.trim().equals("")) {
				continue;
			}
			try {
				Object result = sc.eval2("<stdio>", linenum, command);
				if (!(result instanceof EmptyResult)) {
					ConsoleUtils.println("<<<");
					ConsoleUtils.println(result);
				}
			} catch (ScriptRuntimeException e) {
				ConsoleUtils.println(e.getMessage());
			} catch (RuntimeException e) {
				ConsoleUtils.println(e);
				e.printStackTrace();
			}
			linenum += (command.split("\n").length);
		}
	}

	public final static String KonohaVersion = "4.0";

	private static void show() {
		ConsoleUtils.println("Konoha 4E (" + "Zen" + ") on Nez " + Version);
		ConsoleUtils.println(Copyright);
	}

	private static String readLine() {
		ConsoleUtils.println(">>>");
		Object console = ConsoleUtils.getConsoleReader();
		StringBuilder sb = new StringBuilder();
		while (true) {
			String line = ConsoleUtils.readSingleLine(console, "");
			if (line == null) {
				return null;
			}
			if (line.equals("")) {
				return sb.toString();
			}
			ConsoleUtils.addHistory(console, line);
			sb.append(line);
			sb.append("\n");
		}
	}

	// @Override
	// public void exec(CommandContext config) throws IOException {
	// Class<?> c = null;
	// try {
	// c = Class.forName("nez.konoha.Konoha");
	// } catch (ClassNotFoundException e) {
	// ConsoleUtils.exit(1, "unsupported konoha");
	// }
	// try {
	// Object konoha = c.newInstance();
	// Method shell = c.getMethod("shell");
	// shell.invoke(konoha);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

}
