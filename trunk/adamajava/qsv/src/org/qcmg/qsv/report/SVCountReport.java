/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.util.QSVUtil;

public class SVCountReport extends QSVReport {

    private Map<String, Integer> somatic;
	private Map<String, Integer> germline;
    private Map<String, Integer> normalGermline;
    private String sampleName;
    
    public SVCountReport(File countFile, String sampleName) {
        super(countFile);
        this.sampleName = sampleName;
        setUpClusterCountMap();
    } 
    
    public Map<String, Integer> getSomatic() {
		return somatic;
	}

	public void setSomatic(Map<String, Integer> somatic) {
		this.somatic = somatic;
	}

	public Map<String, Integer> getGermline() {
		return germline;
	}

	public void setGermline(Map<String, Integer> germline) {
		this.germline = germline;
	}

	public Map<String, Integer> getNormalGermline() {
		return normalGermline;
	}

	public void setNormalGermline(Map<String, Integer> normalGermline) {
		this.normalGermline = normalGermline;
	}

	public String getSampleName() {
		return sampleName;
	}

	public void setSampleName(String sampleName) {
		this.sampleName = sampleName;
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
    
    private void setUpClusterCountMap() {
        somatic = singleCountMap();
        germline = singleCountMap();
        normalGermline = singleCountMap();
    }
    
    private Map<String, Integer> singleCountMap() {
        Map<String, Integer> map = new TreeMap<String, Integer>();
        for (PairGroup zp : PairGroup.values()) {
            map.put(zp.toString(), Integer.valueOf(0));
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

    public String getCountString(String type, Map<String, Integer> mapOfCounts) {
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
        BufferedWriter writer = new BufferedWriter (new FileWriter(file, append));  
        writer.write(getHeader());
        writer.write(generateReport());        
        writer.close();        
    }

    private void addCounts(Map<String, Integer> map, String zp, int count) {
        Integer totalCount = map.get(zp);
        if (totalCount != null) {
            totalCount += count;
            map.put(zp, totalCount);
        }        
    }

	public synchronized void addCountsToMap(PairGroup zp, int somaticCount,
			int germlineCount, int normalgermlineCount) {		
			addCounts(somatic, zp.toString(), somaticCount);
			addCounts(germline, zp.toString(), germlineCount);  
			addCounts(normalGermline, zp.toString(), normalgermlineCount);    
	}

	public int getSomaticCounts() {
		int count = 0;
		for (Entry<String, Integer> entry: somatic.entrySet()) {
			count += entry.getValue();
		}
		return count;
	}

	public int getGermlineCounts() {
		int count = 0;
		for (Entry<String, Integer> entry: germline.entrySet()) {
			count += entry.getValue();
		}
		return count;
	}

	public int getNormalGermlineCounts() {
		int count = 0;
		for (Entry<String, Integer> entry: normalGermline.entrySet()) {
			count += entry.getValue();
		}
		return count;
	}

	@Override
	public void writeHeader() throws IOException {
		// TODO Auto-generated method stub
		
	}
}
