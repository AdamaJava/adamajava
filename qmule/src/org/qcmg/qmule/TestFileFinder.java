/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;

import org.qcmg.common.util.FileUtils;

public class TestFileFinder {
	public static void main(String[] args) {
		File [] files = FileUtils.findDirectories(args[0], "seq_final", true);
		System.out.println("no of files: " + files.length);
		for (File f : files) {
			System.out.println("file found: " + f.getAbsolutePath());
		}
//		File [] files = FileUtils.findFiles(args[0], "java", true);
//		System.out.println("no of files: " + files.length);
//		for (File f : files) {
//			System.out.println("file found: " + f.getAbsolutePath());
//		}
	}
}
