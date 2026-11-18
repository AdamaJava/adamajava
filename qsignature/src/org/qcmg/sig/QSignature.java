/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2026.
 * <p>
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.sig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Entry point for <code>java -jar qsignature.jar</code> and the <code>qsignature</code> start script.
 * <p>
 * The first argument names the tool to run, and the remaining arguments are passed to it unchanged, e.g.
 * <code>qsignature Generate -snpPositions positions.txt -input sample.bam -log generate.log</code>
 * is the same as <code>java -cp ... org.qcmg.sig.Generate -snpPositions positions.txt -input sample.bam -log generate.log</code>
 */
public final class QSignature {

	static final List<String> TOOLS = List.of("Generate", "Compare", "CompareRG", "CompareGenotypeBespoke",
			"CompareIlluminaData", "VcfProfiler", "PositionsProfiler", "VcfAddGeneModel");

	private QSignature() {}

	/**
	 * @return the main method of the named tool
	 * @throws IllegalArgumentException if the name is not one of {@link #TOOLS}
	 */
	static Method getMainMethod(String tool) throws ClassNotFoundException, NoSuchMethodException {
		if ( ! TOOLS.contains(tool)) {
			throw new IllegalArgumentException("Unknown qsignature tool: " + tool + ". Valid tools are: " + String.join(", ", TOOLS));
		}
		return Class.forName(QSignature.class.getPackageName() + "." + tool).getMethod("main", String[].class);
	}

	static String usage() {
		return "usage: qsignature <tool> [tool options]\n"
				+ "where <tool> is one of: " + String.join(", ", TOOLS) + "\n"
				+ "Run a tool with no options (or -help) to see its usage.";
	}

	public static void main(String[] args) throws Throwable {
		if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0]) || "-help".equals(args[0])) {
			System.err.println(usage());
			System.exit(args.length == 0 ? 1 : 0);
		}
		Method main;
		try {
			main = getMainMethod(args[0]);
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.err.println(usage());
			System.exit(1);
			return;
		}
		try {
			main.invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}
}
