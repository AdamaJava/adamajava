/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qsv.discordantpair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.qsv.util.QSVUtil;

public class MatePairsReader {

	private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

	private final PairGroup zp;
	private final String[] dirToRead;
	private final String type;
	private final List<String> categories;

	private Map<String, List<File>> filesToRead;
	private int mateCount;

	public MatePairsReader(PairGroup zp, String matepairFilePath,  String outName, String type) {
		this.zp = zp;
		// the file directory to read from resultsdir/matepair/ACC etc
		if (zp.getPairGroup().contains("_")) {
			String[] groups = zp.toString().split("_");
			dirToRead = new String[groups.length];

			for (int i = 0; i < groups.length; i++) {
				dirToRead[i] = matepairFilePath +  groups[i] + FILE_SEPARATOR;
			}

		} else {
			this.dirToRead = new String[1];
			dirToRead[0] = matepairFilePath + zp.getPairGroup() + FILE_SEPARATOR;
		}  

		this.mateCount = 0;
		this.type = type;
		setUpFilesToRead();
		this.categories = QSVUtil.getCategoryByPairGroup(zp);

	}


	public Map<String, List<File>> getFilesToRead() {
		return filesToRead;
	}

	public PairGroup getZp() {
		return zp;
	}

	public int getMateCount() {
		return mateCount;
	}

	public String getType() {
		return type;
	}

	private void setUpFilesToRead() {
		filesToRead = new HashMap<>();

		for (String dirString : dirToRead) {

			File dir = new File(dirString);
			String[] subFiles = dir.list();
			// example: chr7_test_ND_AAC  

			if (subFiles != null) {
				for (String s : subFiles) {

					//contains ND or TD
					if (s.contains(type)) {

						String key = s.substring(0, s.indexOf("_xxx_"));
						String value = dirString + s;

						File file = new File(value);
						if (filesToRead.containsKey(key)) {  
							filesToRead.get(key).add(file);
						} else {
							List<File> list = new ArrayList<>();
							list.add(file);
							filesToRead.put(key, list); // key: reference name | value: file
						}                     
					}
				}
			}
		}       
	}

	public List<MatePair> getMatePairsListByFiles(List<File> files, boolean isFindMethod) throws Exception {

		List<MatePair> readPairs = new ArrayList<>();

		for (File file : files) {
			try (FileReader fileReader = new FileReader(file);
					BufferedReader reader = new BufferedReader(fileReader)) {
				String line = reader.readLine();
				while (line != null) {
					if (isFindMethod) {
						mateCount++;
					}
					MatePair readPair = new MatePair(line);
					readPairs.add(readPair);
					line = reader.readLine();
				}
			}
		}

		readPairs.sort(new MatePair.ReadMateLeftStartComparator());
		return readPairs;
	}
}
