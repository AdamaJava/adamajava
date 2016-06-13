/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.annotate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.isize.CalculateISize;

public class RunTypeRecord {    
    
	public static final int MAX_I_SIZE = 50000;
	public static final int INITIAL_I_SIZE_BUCKET_SIZE = 10;
	public static final int FINAL_I_SIZE_BUCKET_SIZE = 5000;
	public static final int MAX_CURVE_ISIZE = 5000;
	
	private final String rgId;
	private Integer lower;
	private Integer upper;
	private final String seqMapped;
	private final ConcurrentHashMap<Integer, AtomicInteger> isizeMap;
	private final AtomicInteger count;
	

	 
    public RunTypeRecord(String rgId, Integer lower, Integer upper, String seqMapped) {
    	this.rgId = rgId;
        this.lower = lower;
        this.upper = upper;
        this.seqMapped = seqMapped;
        this.isizeMap = new ConcurrentHashMap<Integer, AtomicInteger>();
        this.count = new AtomicInteger(0);
    }    

    public String getRgId() {
        return rgId;
    }

    public int getLower() {
        return lower.intValue();
    }

    public int getUpper() {
        return upper.intValue();
    }
    
	public ConcurrentHashMap<Integer, AtomicInteger> getIsizeMap() {
		return isizeMap;
	}
    
    @Override
    public String toString() {
		return "RGID: " + rgId +  " | Sample: " + seqMapped + " |  Lower: " + lower + " | Upper:" + upper;    	
    }
    
	public void calculateISize() throws QSVException {	
        CalculateISize isize = new CalculateISize(isizeMap);
        isize.calculate();
        this.lower = isize.getISizeMin();
        this.upper = isize.getISizeMax();
        
        if (this.upper == 0) {
        	throw new QSVException("ZERO_UPPER", rgId);
        }
	}

	public synchronized int getCount() {
		return count.intValue();
	}
	
	public synchronized void addToMap(int iSize) {		
		
		int absISize = Math.abs(iSize);		
		
		int bucket = 0;
		if (absISize < MAX_I_SIZE) {
			bucket = (absISize / INITIAL_I_SIZE_BUCKET_SIZE)
					* INITIAL_I_SIZE_BUCKET_SIZE;
		} else {
			
			if (absISize < FINAL_I_SIZE_BUCKET_SIZE) {
				bucket = MAX_I_SIZE;
			} else {
				bucket = (absISize / FINAL_I_SIZE_BUCKET_SIZE)
						* FINAL_I_SIZE_BUCKET_SIZE;
			}
		}
		
		if (bucket < MAX_CURVE_ISIZE) {
			count.incrementAndGet();
			
			AtomicInteger current = isizeMap.get(bucket);
			if (null != current) {
				current.incrementAndGet();
			} else {
				isizeMap.put(bucket, new AtomicInteger(1));
			}
		}	
	}

	public String getReportDataString() {
		return seqMapped + "_" + rgId + "_" + lower + "_" + upper;
	}
}
