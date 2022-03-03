/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2022.
 */

package au.edu.qimr.tiledaligner;



/**
 * The entry point for the q3tiledaligner project
 */
public final class Main {
	
	
	/**
	 * Lists to the user the different ways of invoking the applications within this project
	 * 
	 * @param args
	 *            the command-line arguments.
	 * @throws ClassNotFoundException 
	 */
	public static void main(final String[] args) throws ClassNotFoundException {
		Options options = null;
		try {
			options = new Options(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(Messages.USAGE);
//		try {
//			options.displayHelp();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		System.exit(0);
	}
}
