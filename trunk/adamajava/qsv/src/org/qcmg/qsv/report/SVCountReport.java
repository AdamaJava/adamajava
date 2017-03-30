/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.util.Constants;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.QSVUtil;

public class SVCountReport extends QSVReport {

    private final ConcurrentMap<String, AtomicInteger> somatic;
	private final ConcurrentMap<String, AtomicInteger> germline;
    private final ConcurrentMap<String, AtomicInteger> normalGermline;
    private final String sampleName;
    
    public SVCountReport(File countFile, String sampleName) {
        super(countFile);
        this.sampleName = sampleName;
        
        this.somatic = singleCountMap();
        this.germline = singleCountMap();
        this.normalGermline =  singleCountMap();
    }
    
    public ConcurrentMap<String, AtomicInteger> getSomatic() {
		return somatic;
	}

	public ConcurrentMap<String, AtomicInteger> getGermline() {
		return germline;
	}

	public ConcurrentMap<String, AtomicInteger> getNormalGermline() {
		return normalGermline;
	}

	public String getSampleName() {
		return sampleName;
	}

    
    @Override
    public String getHeader() {
        StringBuilder headings = new StringBuilder();
        headings.append("Mutation Type\t");
        headings.append("Sample\t");
        for (PairGroup zp : PairGroup.values()) {
            headings.append(zp.toString()).append(Constants.TAB);
        }            
        headings.append("TOTAL" + QSVUtil.getNewLine());
        return headings.toString();
    }
    
    private synchronized ConcurrentMap<String, AtomicInteger> singleCountMap() {
        ConcurrentMap<String, AtomicInteger> map = new ConcurrentHashMap<>();
        for (PairGroup zp : PairGroup.values()) {
            map.put(zp.toString(), new AtomicInteger());
        }
        return map;
    }  

    private String generateReport() {
    		StringBuilder sb = new StringBuilder();
        sb.append(getCountString("somatic", somatic));
        sb.append(getCountString("germline", germline));
        sb.append(getCountString("normal-germline", normalGermline));
        return sb.toString();
    }

    public String getCountString(String type, ConcurrentMap<String, AtomicInteger> mapOfCounts) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(Constants.TAB);
        sb.append(sampleName).append(Constants.TAB);
        
        for (PairGroup zp : PairGroup.values()) {
            sb.append(mapOfCounts.get(zp.toString())).append(Constants.TAB);
        }        
        if (type.equals("somatic")) {
        		sb.append(getSomaticCounts() +QSVUtil.getNewLine());
        } else if (type.equals("germline")) {
        		sb.append(getGermlineCounts() +QSVUtil.getNewLine());
        } else {
        		sb.append(getNormalGermlineCounts() +QSVUtil.getNewLine());
        }
        
        return sb.toString();
    }

    @Override
    public void writeReport() throws Exception {
        try (BufferedWriter writer = new BufferedWriter (new FileWriter(file, append));) {  
	        writer.write(getHeader());
	        writer.write(generateReport());        
        }
    }

    private void addCounts(ConcurrentMap<String, AtomicInteger> map, String zp, int count) {
        map.get(zp).addAndGet(count);
    }

	public synchronized void addCountsToMap(PairGroup zp, int somaticCount,
			int germlineCount, int normalgermlineCount) {
			addCounts(somatic, zp.toString(), somaticCount);
			addCounts(germline, zp.toString(), germlineCount);
			addCounts(normalGermline, zp.toString(), normalgermlineCount);
	}

	public int getSomaticCounts() {
		return somatic.values().stream().mapToInt(ai -> ai.get()).sum();
	}

	public int getGermlineCounts() {
		return germline.values().stream().mapToInt(ai -> ai.get()).sum();
	}

	public int getNormalGermlineCounts() {
		return normalGermline.values().stream().mapToInt(ai -> ai.get()).sum();
	}

	@Override
	public void writeHeader() throws IOException {
		// TODO Auto-generated method stub
	}
}
