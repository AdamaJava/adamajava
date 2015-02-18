/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

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
        StringBuffer headings = new StringBuffer();
        headings.append("Mutation Type\t");
        headings.append("Sample\t");
        for (PairGroup zp : PairGroup.values()) {
            headings.append(zp.toString() + "\t");
        }            
        headings.append("TOTAL" + QSVUtil.getNewLine());
        return headings.toString();
    }
    
    private synchronized ConcurrentMap<String, AtomicInteger> singleCountMap() {
        ConcurrentMap<String, AtomicInteger> map = new ConcurrentHashMap<String, AtomicInteger>();
        for (PairGroup zp : PairGroup.values()) {
            map.put(zp.toString(), new AtomicInteger());
        }
        return map;
    }  

    private String generateReport() {
        StringBuffer sb = new StringBuffer();
        sb.append(getCountString("somatic", somatic));
        sb.append(getCountString("germline", germline));
        sb.append(getCountString("normal-germline", normalGermline));
        return sb.toString();
    }

    public String getCountString(String type, ConcurrentMap<String, AtomicInteger> mapOfCounts) {
        String line = "";
        
        StringBuffer sb = new StringBuffer();
        sb.append(type + "\t");
        sb.append(sampleName + "\t");
        
        for (PairGroup zp : PairGroup.values()) {
            sb.append(mapOfCounts.get(zp.toString()) + "\t");
        }        
        if (type.equals("somatic")) {
        	sb.append(getSomaticCounts() +QSVUtil.getNewLine());
        } else if (type.equals("germline")) {
        	sb.append(getGermlineCounts() +QSVUtil.getNewLine());
        } else {
        	sb.append(getNormalGermlineCounts() +QSVUtil.getNewLine());
        }
        
        line += sb.toString();
        
        return line;
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
		int count = 0;
		List<String> keys = new ArrayList<>(somatic.keySet());
		Collections.sort(keys);
		for (String k : keys) {
			AtomicInteger value = somatic.get(k);
			count += value.intValue();
		}
		return count;
	}

	public int getGermlineCounts() {
		int count = 0;
		for (Entry<String, AtomicInteger> entry: germline.entrySet()) {
			count += entry.getValue().intValue();
		}
		return count;
	}

	public int getNormalGermlineCounts() {
		int count = 0;
		for (Entry<String, AtomicInteger> entry: normalGermline.entrySet()) {
			count += entry.getValue().intValue();
		}
		return count;
	}

	@Override
	public void writeHeader() throws IOException {
		// TODO Auto-generated method stub
		
	}
}
