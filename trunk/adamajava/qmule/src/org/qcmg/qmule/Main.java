/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;



/**
 * The entry point for the command-line SAM/BAM merging tool.
 */
public final class Main {
	
//	enum Tool {
//		GetBamRecords("org.qcmg.qmule.GetBamRecords");
////		GetBamRecords("GetBamRecords", "org.qcmg.qmule.GetBamRecords"),
////		GetBamRecords("GetBamRecords", "org.qcmg.qmule.GetBamRecords"),
////		GetBamRecords("GetBamRecords", "org.qcmg.qmule.GetBamRecords");
//		
////		private final String name;
//		private final String fullyQualifiedName;
//		
//		private Tool(String fullyQualifiedName) {
////			this.name = name;
//			this.fullyQualifiedName = fullyQualifiedName;
//		}
//		
//		public String getFullyQualifiedName() {
//			return fullyQualifiedName;
//		}
//		public static Tool getTool(String name) {
//			for (Tool t : Tool.values()) {
//				if (name.equals(t.name())) return t;
//			}
//			throw new IllegalArgumentException("Tool not found: " + name);
//		}
//	}
	
	/**
	 * Performs a single merge based on the supplied arguments. Errors will
	 * terminate the merge and display error and usage messages.
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
				try {
					options.displayHelp();
				} catch (Exception e) {
					e.printStackTrace();
				}
				
//				String toolName = options.getToolName();
//				Tool t = Tool.getTool(toolName);
//				Class tool = Class.forName(t.getFullyQualifiedName());
//				System.out.println("Class: " + tool.getCanonicalName());
//				 // Create the array of Argument Types
//				Class[] argTypes = { args.getClass()}; // array is Object!
//				// Now find the method
//				Method m = null;
//				try {
//					m = tool.getMethod("main", argTypes);
//				} catch (SecurityException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (NoSuchMethodException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				System.out.println(m);
//	
//				// Create the actual argument array
//				Object passedArgv[] = { args };
//	
//				 // Now invoke the method.
//				try {
//					m.invoke(null, passedArgv);
//				} catch (IllegalArgumentException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (IllegalAccessException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				} catch (InvocationTargetException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
				
//)				Method m = tool.getMethod("main", Object.class);
//				m.iinvoke(args);
				System.exit(0);
	}
}
