/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.common.util;

import static org.qcmg.common.util.FileUtils.FILE_SEPARATOR;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LoadReferencedClasses {
	
	
	public static final <T> void loadClasses(final Class<T> clazz) throws URISyntaxException, IOException, ClassNotFoundException {
		final long start = System.currentTimeMillis();
		final long mem = Runtime.getRuntime().freeMemory();
		
		final File thisJarFile = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
		if (null != thisJarFile && FileUtils.isFileTypeValid(thisJarFile, "jar")) {
//			System.out.println("BEFORE: no of loaded packages: " + Package.getPackages().length);
		
			// create JarFile and extract info from manifest
			final JarFile jf = new JarFile(thisJarFile);
			
			// load entries from this far file first
			loadClassesInJar(jf, clazz, false);
			
			// now onto dependent jars
			try {
				final Attributes att = jf.getManifest().getMainAttributes();
				String classpath = att.getValue("Class-Path");
				
				if (null != classpath) {
					String [] jarArray = classpath.split("\\s");
					for (String jar : jarArray) {
						try {
							JarFile internalJarFile = new JarFile(thisJarFile.getParent() + FILE_SEPARATOR + jar);
							loadClassesInJar(internalJarFile, clazz, true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
//					System.out.println("time taken: " + (System.currentTimeMillis() - start));
//					System.out.println("mem used:  " + (mem - Runtime.getRuntime().freeMemory()));
//					System.out.println("AFTER: no of loaded packages: " + Package.getPackages().length);
				} else {
					System.err.println("couldn't locate Class-Path attribute in jar manifest file");
				}
			} finally {
				jf.close();
			}
		} else {
			// we could be running from eclipse in which case the supplied clazz would not have an associated jar file
//			System.out.println("Null jar file, or invalid file type");
		}
	}
	
	static final <T> void loadClassesInJar(JarFile jarFile, Class<T> clazz, boolean closeFile) throws IOException, ClassNotFoundException {
		if (null != jarFile) {
			
//			System.out.println("loading classes from jar file: " + jarFile.getName());
			
			try {
				Enumeration<JarEntry> enums = jarFile.entries();
				while (enums.hasMoreElements()) {
					JarEntry je = enums.nextElement();
					if (FileUtils.isFileTypeValid(je.getName(), "class")) {
						String classToLoad = je.getName().replace(".class", "").replaceAll(FILE_SEPARATOR, ".");
						try {
							//classToLoad = classToLoad.replaceAll(FILE_SEPARATOR, ".");
							//System.out.println("about to load class: " + classToLoad);
							clazz.getClassLoader().loadClass(classToLoad);
						} catch (NoClassDefFoundError e) {	
							//XXX catching errors is not recommended, but is necessary in this instance
							// Ignoring - have seen instances where classes refer to other classes not included in the jar file
//							System.err.println("could not load class: " + classToLoad);
						}
					}
				}
			} finally {
				if (closeFile) jarFile.close();
			}
		}
	}

}
