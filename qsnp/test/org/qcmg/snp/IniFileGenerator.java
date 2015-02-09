package org.qcmg.snp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IniFileGenerator {
	public static final char NL = '\n';
	
	public static void createRulesOnlyIni(File iniFile) throws IOException {
		FileWriter writer = new FileWriter(iniFile);
		try {
			// add rules
			for (String s : generateIniFileData()) {
				writer.write(s + NL);
			}
		} finally {
			writer.close();
		}
	}
	
	public static void addInputFiles(File iniFile, boolean newFile, String ... inputs) throws IOException {
		if (newFile) {
			createRulesOnlyIni(iniFile);
		}
		FileWriter writer = new FileWriter(iniFile, true);
		try {
			// add inputs
			writer.write("[inputFiles]\n");
			for (String s : inputs) {
				writer.write(s + NL);
			}
		} finally {
			writer.close();
		}
	}
	
	public static void addOutputFiles(File iniFile, boolean newFile, String ... outputs) throws IOException {
		if (newFile) {
			createRulesOnlyIni(iniFile);
		}
		FileWriter writer = new FileWriter(iniFile, true);
		try {
			// add inputs
			writer.write("[outputFiles]\n");
			for (String s : outputs) {
				writer.write(s + NL);
			}
		} finally {
			writer.close();
		}
	}
	
	public static void addStringToIniFile(File iniFile, String data, boolean append) throws IOException {
		FileWriter writer = new FileWriter(iniFile, append);
		try {
			writer.write(NL + data + NL);
		} finally {
			writer.close();
		}
	}
	
	public static List<String> generateIniFileData() {
		List<String> data  = new ArrayList<String>();
		data.add("[rules]");
		data.add("control1=0,20,3");
		data.add("control2=21,50,4");
		data.add("control3=51,,10");
		data.add("test1=0,20,3");
		data.add("test2=21,50,4");
		data.add("test3=51,,5");
		data.add("[parameters]");
		data.add("minimumBaseQuality = 10");
		data.add("pileupOrder=NT");
		return data;
	}
}
