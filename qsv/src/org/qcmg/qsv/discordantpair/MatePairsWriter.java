/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qsv.discordantpair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.qcmg.qsv.QSVException;

public class MatePairsWriter {

    private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();
    private final PairClassification zp;
    private final Map<String, Map<String, MatePair>> matePairs; // Key: Chromosome, Value: Map Key: MatePair read name(qname+RG-Id) Map Value: MatePair (with both mates)
    private final String dirToWrite;
    private final String fileName;
	private final String pairType;

    public MatePairsWriter(PairClassification zp, String matePairFilePath, String type, String pairType) throws IOException, QSVException {
        this.zp = zp;
        this.matePairs = new TreeMap<String, Map<String, MatePair>>();
        this.dirToWrite = matePairFilePath + zp.getPairingClassification() + FILE_SEPARATOR;
        this.fileName = "_" + type + "_" + zp.getPairingClassification();  
        this.pairType = pairType;
    }

    public PairClassification getZp() {
        return zp;
    }

    public Map<String, Map<String, MatePair>> getMatePairs() {
        return matePairs;
    }
    
    public void addNewMatePair(MatePair matePair) {
        String refName = matePair.getLeftMate().getReferenceName();
        if (zp.getPairingClassification().equals("Cxx")) {
            refName += "-" + matePair.getRightMate().getReferenceName();
        }
        String cat = switch (pairType) {
            case "lmp" -> matePair.getSVCategoryForLMP();
            case "pe" -> matePair.getSVCategoryForPE();
            case "imp" -> matePair.getSVCategoryForIMP();
            default -> null;
        };

        if (cat != null) {
        	String key = refName += "-" + cat;
	        if (matePairs.containsKey(refName)) {
	            matePairs.get(key).put(matePair.getReadName(), matePair);
	        } else {
	            Map<String, MatePair> pairs = new HashMap<String, MatePair>();
	            pairs.put(matePair.getReadName(), matePair);
	            matePairs.put(key, pairs);
	        }
        }
    }
    
    private Map<String, File> createFilesToWrite() {
        Map<String, File> filesToWrite = new HashMap<>();

        for (Map.Entry<String, Map<String, MatePair>> entry : matePairs.entrySet()) {
            File file = new File(dirToWrite + entry.getKey() + fileName);
            filesToWrite.put(entry.getKey(), file);
        }
        return filesToWrite;
    }

    public String getDirToWrite() {
        return dirToWrite;
    }

    public String getFileName() {
        return fileName;
    }

    public synchronized void writeMatePairsToFile() throws IOException {
        // set to true to append rather than create
        Map<String, File> filesToWrite = createFilesToWrite();

        for (Map.Entry<String, Map<String, MatePair>> chrEntry : matePairs.entrySet()) {

            File file = filesToWrite.get(chrEntry.getKey());
            
            FileWriter fileWriter = new FileWriter(file, true);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            Map<String, MatePair> currentPairs = chrEntry.getValue();
            for (Map.Entry<String, MatePair> entry : currentPairs.entrySet()) {
                writer.write(entry.getValue().toString());
            }
            writer.flush();
            writer.close();
        }
        matePairs.clear();
    }
}
