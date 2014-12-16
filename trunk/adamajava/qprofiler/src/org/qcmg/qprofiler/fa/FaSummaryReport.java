/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 * © Copyright QIMR Berghofer Medical Research Institute 2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler.fa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.ProfileType;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.util.SummaryReportUtils;
import org.w3c.dom.Element;

public class FaSummaryReport extends SummaryReport {
	
	ConcurrentMap<String, AtomicLong> kmers = new ConcurrentHashMap<>();
	
	
	List<ConcurrentMap<String, AtomicLong>> kmerArray =  new ArrayList<ConcurrentMap<String, AtomicLong>>();
	
	final int kmerArraySize = 256;
	
	
	public FaSummaryReport() {
		super();
		
		//populate kmerArray with empty maps
		for (int i = 0 ; i < kmerArraySize ; i++) {
			kmerArray.add(new ConcurrentHashMap<String, AtomicLong>());
		}
	}
	
	@Override
	public void toXml(Element parent) {
		
		Element element = init(parent, ProfileType.FA, null, null, null);
		logger.info("no of kmers: " + kmers.size());
		SummaryReportUtils.lengthMapToXml(element, "KMERS", kmers);
		
		
		// print some stats on the kmerArray
		for (int i = 0 ; i < kmerArraySize ; i++) {
			// we want position, and number of kmers at that position
			ConcurrentMap<String, AtomicLong> map = kmerArray.get(i);
			logger.info("position: " + i + ", no of kmers at this pos: " + map.size());
		}
	}
	
	/**
	 * Reads a row from the text file and returns it as a string
	 * 
	 * @return next row in file
	 */
	public void parseRecord(byte[] record) {
		if (null != record) {
			
			updateRecordsParsed();
				
			// split read into 6-mers and tally
			int kmerLength = 6;
			for (int i = 0, len = record.length - kmerLength ; i < len ; i++) {
				String kmer = new String(Arrays.copyOfRange(record, i, i+kmerLength));
				updateMap(kmers, kmer);
				updateMapAndPosition(kmerArray, kmer, i);
			}
		}
	}

	private <T> void updateMap(ConcurrentMap<T, AtomicLong> map , T key) {
		AtomicLong al = map.get(key);
		if (null == al) {
			al = new AtomicLong();
				AtomicLong existing = map.putIfAbsent(key, al);
				if (null != existing) {
					al = existing;
				}
		}
		al.incrementAndGet();
	}
	
	private <T> void updateMapAndPosition(List<ConcurrentMap<T, AtomicLong>> collection , T key, int position) {
		
		if (position > kmerArraySize) {
			throw new IllegalArgumentException("position is larger than size of array! position: " + position + ", array size: " + kmerArraySize);
		}
		// get map at this position
		ConcurrentMap<T, AtomicLong> map = collection.get(position);
		
		
		AtomicLong al = map.get(key);
		if (null == al) {
			al = new AtomicLong();
			AtomicLong existing = map.putIfAbsent(key, al);
			if (null != existing) {
				al = existing;
			}
		}
		al.incrementAndGet();
	}
	
	public ConcurrentMap<String, AtomicLong> getKmersMap() {
		return kmers;
	}
	
	public long getKmerCoverageCount() {
		// sum all the values in the kmer map
		long tally = 0;
		for (AtomicLong al : kmers.values()) {
			tally += al.get();
		}
		return tally;
	}
	
}
