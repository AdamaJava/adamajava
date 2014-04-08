/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.snp;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupConstants;
import org.qcmg.qbasepileup.QBasePileupException;




public class SnpPositionPileup {
	
	private static QLogger logger = QLoggerFactory.getLogger(SnpPositionPileup.class);
	private Integer baseQual;
	private final Integer mappingQual;
	private Character[] readBases;
	private int[] baseQuals;
	private Map<String, AtomicInteger> coverageMap;
	private Map<String, AtomicInteger> forCoverageMap;
	private Map<String, AtomicInteger> revCoverageMap;
	private Map<String, Set<Integer>> forNovelStartMap;
	private Map<String, Set<Integer>> revNovelStartMap;	
	private final SnpPosition position;
	private IndexedFastaSequenceFile indexedFasta;
	private byte[] referenceBases;
	private final Options options;
	int totalExamined = 0;
	int passFiltersCount = 0;
	int countPositiveStrand = 0;
	int countNegativeStrand = 0;
	int basesNotPassBaseQual = 0;
	int readsNotPassMapQual = 0;
	int doesntMapCount = 0;
	List<String> allStarts;
	private final boolean isNovelStarts;
	private final boolean isStrand;
	private InputBAM input;
	private int referenceBaseCount = 0;
	private int nonReferenceBaseCount = 0;
	private QueryExecutor exec;
	private int indelPresent = 0;
		
	public SnpPositionPileup(InputBAM input, SnpPosition p, Options options, IndexedFastaSequenceFile indexedFasta) throws QBasePileupException {
		this.input = input;
		this.position = p;
		this.indexedFasta = indexedFasta;
		this.isNovelStarts = options.isNovelstarts();
		this.isStrand = options.isStrandSpecific();
		this.options = options;
		
		if (options.getBaseQuality() != null) {
			this.baseQual = options.getBaseQuality();
		}
		this.mappingQual = options.getMappingQuality();
				
		if (isNovelStarts) {
			setUpNovelStartMaps();			
		} else {
			setUpCoverageMaps();
		}
	}
	
	public SnpPositionPileup(SnpPosition p, Options options, QueryExecutor exec) throws QBasePileupException {
		this.position = p;
		this.referenceBases = p.getReferenceBases();
		this.isNovelStarts = options.isNovelstarts();
		this.options = options;		
		this.isStrand = options.isStrandSpecific();
		if (options.getBaseQuality() != null) {
			this.baseQual = options.getBaseQuality();
		}
		this.mappingQual = options.getMappingQuality();
				
		if (isNovelStarts) {
			setUpNovelStartMaps();			
		} else {
			setUpCoverageMaps();
		}
		this.exec = exec;		
	}
	
	public SnpPositionPileup(InputBAM input, SnpPosition p, Options options, QueryExecutor exec) throws QBasePileupException {		
		this(p, options, exec);
		this.input = input;		
	}
	
	public Map<String, AtomicInteger> getCoverageMap() {
		return coverageMap;
	}

	public Map<String, AtomicInteger> getForCoverageMap() {
		return forCoverageMap;
	}

	public Map<String, AtomicInteger> getRevCoverageMap() {
		return revCoverageMap;
	}

	public Map<String, Set<Integer>> getForNovelStartMap() {
		return forNovelStartMap;
	}

	public Map<String, Set<Integer>> getRevNovelStartMap() {
		return revNovelStartMap;
	}

	void setUpCoverageMaps() {
		if (isSingleBasePosition()) {
			coverageMap = setUpStandardMap();			
			if (isStrand) {
				forCoverageMap = setUpStandardMap();
				revCoverageMap = setUpStandardMap();
			}
		} else {
			coverageMap = new TreeMap<String, AtomicInteger>();
			if (isStrand) {
				forCoverageMap = new TreeMap<String, AtomicInteger>();
				revCoverageMap = new TreeMap<String, AtomicInteger>();
			}
		}		
	}
	
	void setUpNovelStartMaps() {
		if (isSingleBasePosition()) {
			forNovelStartMap = setUpStandardNovelStartMap();
			revNovelStartMap = setUpStandardNovelStartMap();			
		} else {
			forNovelStartMap = new TreeMap<String, Set<Integer>>();
			revNovelStartMap = new TreeMap<String, Set<Integer>>();
		}		
	}
	
	private Map<String, AtomicInteger> setUpStandardMap() {
		Map<String, AtomicInteger> map = new TreeMap<String, AtomicInteger>();
		map.put("A", new AtomicInteger(0));
		map.put("C", new AtomicInteger(0));		
		map.put("G", new AtomicInteger(0));
		map.put("T", new AtomicInteger(0));
		map.put("N", new AtomicInteger(0));
		return map;
	}		

	private Map<String, Set<Integer>> setUpStandardNovelStartMap() {
		Map<String, Set<Integer>> map = new TreeMap<String, Set<Integer>>();
		map.put("A", new HashSet<Integer>());
		map.put("C", new HashSet<Integer>());		
		map.put("G", new HashSet<Integer>());
		map.put("T", new HashSet<Integer>());
		map.put("N", new HashSet<Integer>());
		return map;
	}

	private String getReferenceBases() {
		StringBuilder b = new StringBuilder();
		for (byte by: referenceBases) {
			b.append((char) by);
		}
		return b.toString();
//		return new String(referenceBases);
	}
	
	private String getCompoundReferenceBases() {
		StringBuilder b = new StringBuilder();
		for (byte by: position.getAltBases()) {
			b.append((char) by);
		}
		return b.toString();
//		return new String(position.getAltBases());
	}

	public boolean isSingleBasePosition() {
		if (position.getLength() == 1) {
			return true;
		} 
		return false;
	}

	public int getCountPositionStrand() {
		return countPositiveStrand;
	}

	public int getCountNegativeStrand() {
		return countNegativeStrand;
	}

	public int getBasesNotPassBaseQual() {
		return basesNotPassBaseQual;
	}

	public int getReadsNotPassMapQual() {
		return readsNotPassMapQual;
	}

	public int getDoesntMapCount() {
		return doesntMapCount;
	}

	public int getTotalExamined() {
		return totalExamined;
	}

	public Character[] getReadBases() {
		return readBases;
	}

	public void setReadBases(Character[] readBases) {
		this.readBases = readBases;
	}

	public int getPassFiltersCount() {
		return passFiltersCount;
	}

	public void pileup() throws Exception {
		//logger.info("Pileup for " + position.toString());			
		
		if (referenceBases == null && indexedFasta != null) {
			referenceBases = indexedFasta.getSubsequenceAt(position.getFullChromosome(), position.getStart(), position.getEnd()).getBases();
		}	
		
		pileupFile();		
		
		logger.debug("TOTAL COUNTS FOR THIS SNP:\t " + passFiltersCount+ " / " + totalExamined);
		logger.debug("READ DOES NOT MAP TO POS:\t"+doesntMapCount+"");
		logger.debug("BAD BASE/MAP QUALITY:\t\t" + basesNotPassBaseQual+ " / " + readsNotPassMapQual);		
	}
	
	public void addSAMRecord(SAMRecord record) throws Exception {
		if (passesReadFilters(record)) {
			parseRecord(record);
		}	
	}
	

	private void pileupFile() throws Exception {
		try (SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(input.getBamFile(), ValidationStringency.SILENT)) {
					
			SAMRecordIterator iter = reader.queryOverlapping(position.getFullChromosome(), position.getStart(), position.getEnd());
			
			while (iter.hasNext()) {
				SAMRecord r = iter.next();
				
				if (passesReadFilters(r)) {
					parseRecord(r);
				}					
			}
			iter.close();
		}
	}

	private boolean passesReadFilters(SAMRecord r) throws Exception {
		//make sure alignment contains the position/s of interest
		if (position.getStart() >= r.getAlignmentStart() && position.getEnd() >= r.getAlignmentStart() 
				&& position.getStart() <= r.getAlignmentEnd() && position.getEnd() <= r.getAlignmentEnd()) {
			//filters	
			
			if ((!r.getDuplicateReadFlag() || (r.getDuplicateReadFlag() && options.includeDuplicates())) && !r.getReadUnmappedFlag() && (exec == null || (exec !=null && exec.Execute(r)))) {
				return true;
			}				
		}
		return false;
	}

	public void parseRecord(SAMRecord r) throws QBasePileupException {
	
		totalExamined ++;		
		
		//skip indels
		if ( ! options.includeIndel() && (r.getCigarString().contains("I") || r.getCigarString().contains("D"))) {
			return;
		}
//		//skip introns
		if ( ! options.includeIntron() && r.getCigarString().contains("N")) {
			return;
		}			
				
		//get relevant bases, dealing with I,D
		deconvoluteReadSequence(r);
		
		if (options.getMode().equals(QBasePileupConstants.SNP_CHECK_MODE)) {
			
			if (indelPresent(r)) {				
				indelPresent ++;
			}			
		}
		
		//read doesn't pass base quality filtering
		String readBaseString = new String();
		if (basesAreMapped()) {
			
			for (int i=0; i<readBases.length; i++) {
				readBaseString += readBases[i];
				if (baseQual != null) {					
					if (baseQuals[i] < baseQual.intValue()) {
						basesNotPassBaseQual++;
						return;
					}
				}					
			}
			
			//read doesn't pass mapping quality filtering				
			if (mappingQual != null) {
				if (r.getMappingQuality() < mappingQual.intValue()) {
					readsNotPassMapQual++;
					return;
				}
			}			
			
			//increase count in coverage map
			if (isNovelStarts) {
				if (r.getReadNegativeStrandFlag()) {
					addToNovelStartMap(readBaseString, revNovelStartMap, r.getAlignmentStart());					
				} else {
					addToNovelStartMap(readBaseString, forNovelStartMap, r.getAlignmentEnd());
				}
					
			} else {
				incrementCoverage(readBaseString.toUpperCase(), r.getReadNegativeStrandFlag());
			}
			//final counts of reads passing filters
			passFiltersCount++;
			if (r.getReadNegativeStrandFlag()) {
				countNegativeStrand++;
			} else {
				countPositiveStrand++;
			}
		}				
	}	

	private boolean indelPresent(SAMRecord r) {
		final Cigar cigar = r.getCigar();
		String cigarString = cigar.toString();
		if ( ! cigarString.contains("I") && ! cigarString.contains("D")) {
			return false;
		}
		int readStart = r.getAlignmentStart();
		int offset = 0; 
		
		for (CigarElement ce : cigar.getCigarElements()) {
			int cigarLength = ce.getLength();
			
			
			if (CigarOperator.DELETION == ce.getOperator()) {
				if (position.getStart() == (readStart + offset -1) 
					|| position.getStart() == (readStart + offset + cigarLength)) {
					return true;
				}
			} else if (CigarOperator.INSERTION == ce.getOperator()) {
				if (position.getStart() == (readStart + offset) ) {
						return true;
				}
			}
			
			if (isCigarElementAdvancable(ce)) {
				offset += cigarLength;
			}
		}
		
		return false;
	}

	private boolean isCigarElementAdvancable(CigarElement ce) {
		return CigarOperator.M == ce.getOperator()
				|| CigarOperator.D == ce.getOperator()
				|| CigarOperator.EQ == ce.getOperator()
				|| CigarOperator.X == ce.getOperator();
	}

	public void addToNovelStartMap(String key,
			Map<String, Set<Integer>> novelStartMap, Integer alignmentStart) {
		if (novelStartMap.containsKey(key)) {			
			novelStartMap.get(key).add(alignmentStart);	
		} else {			
			Set<Integer> set = new HashSet<Integer>();
			set.add(alignmentStart);
			novelStartMap.put(key, set);
		}		
	}

	public void incrementCoverage(String readBaseString, boolean isReadStrandNegative) {
		//add to coverage maps for strand specific
		
		if (isStrand) {			
			if (isReadStrandNegative) {
				addToMap(readBaseString, revCoverageMap);					
			} else {
				addToMap(readBaseString, forCoverageMap);
			}								
		} else {		
			//coverage map for all reads
			addToMap(readBaseString, coverageMap);		
		}		
	}

	private void addToMap(String key, Map<String, AtomicInteger> map) {
		AtomicInteger ai = map.get(key);
		if (null == ai) {
			map.put(key, new AtomicInteger(1));	
		} else {
			ai.incrementAndGet();
		}
	}

	public boolean basesAreMapped() {
		if (readBases != null) {
			for (int i=0; i<readBases.length; i++) {
				if (readBases[i] == null) {
					doesntMapCount++;
					return false;
				}
			}
		} else {
			doesntMapCount++;
			return false;
		}
		return true;
	}

	public void deconvoluteReadSequence(SAMRecord r) throws QBasePileupException {
		int readIndex = 0;		
		int referenceStart = r.getUnclippedStart();
		int readStart = r.getAlignmentStart();
		int readEnd = r.getAlignmentEnd();
		int referencePos = referenceStart;
		//make the reference from the read sequence using picard					
		byte[] readBytes = r.getReadBases();
		byte[] baseQualities = r.getBaseQualities();
				
		readBases = new Character[position.getLength()];
		baseQuals = new int[position.getLength()];
		
		for (CigarElement element: r.getCigar().getCigarElements()) {
			CigarOperator operator = element.getOperator();

			if (referencePos < readStart || referencePos > readEnd) {
				
				if (operator == CigarOperator.H || operator == CigarOperator.S) {					
					referencePos += element.getLength();					
					if (operator == CigarOperator.S) {
						readIndex += element.getLength();						
					}
				} else if (operator == CigarOperator.I) {					
				} else {
					String error = "ReferencePos: " + referencePos + " ReadStart: " + readStart + " ReadEnd: " + readEnd + " CigarOperator: " + operator.name();				
					throw new QBasePileupException("BASE_RANGE_ERROR", ""  + error, r.getSAMString());
				}
			} else {
				//should be in the read
				if (referencePos >= readStart && referencePos <= readEnd) {
					if (operator == CigarOperator.M) {						
						for (int i=0; i<element.getLength(); i++) {
							char base = (char) readBytes[readIndex];	
							
							if (referencePos >= position.getStart() && referencePos <= position.getEnd()) {								
								int index = referencePos - position.getStart();
								readBases[index] = base;
								baseQuals[index] = baseQualities[readIndex];
							}
							
							readIndex++;
							referencePos++;
						}												
					} else if (operator ==  CigarOperator.D) {
						referencePos += element.getLength();						
					} else if (operator == CigarOperator.I) {			
						readIndex += element.getLength();
					} else if (operator == CigarOperator.N) {
						referencePos += element.getLength();																
					} else if (operator == CigarOperator.P) {
						throw new QBasePileupException("CIGAR_P_ERROR", r.getSAMString());
					} else {
						String error = "ReferencePos: " + referencePos + " ReadStart: " + readStart + " ReadEnd: " + readEnd;
						throw new QBasePileupException("BASE_ERROR", error, r.getSAMString());
					}						
				} else {
					String error = "ReferencePos: " + referencePos + " ReadStart" + readStart + " ReadEnd: " + readEnd + " CigarOperator: " + operator.name();
					throw new QBasePileupException("BASE_RANGE_ERROR", ""  + error, r.getSAMString());
				}
			}			
 		}
	}

	private String getMapString(Map<String, AtomicInteger> map) {
		StringBuilder b = new StringBuilder();
		if (isSingleBasePosition()) {
			b.append(map.get("A").intValue() + "\t");
			b.append(map.get("C").intValue() + "\t");			
			b.append(map.get("G").intValue() + "\t");
			b.append(map.get("T").intValue() + "\t");
			b.append(map.get("N").intValue() + "\t");
		} else {
			for (Entry<String, AtomicInteger> entry: map.entrySet()) {				
				b.append(entry.getValue().intValue() + "\t");
			}
		}		
		return b.toString();
	}

	private String getCountsString(Map<String, AtomicInteger> coverageMap,
			Map<String, Set<Integer>> novelStartMap) {
		if (isNovelStarts) {
			return getNovelStartMapString(novelStartMap); 
		} else {
			return getMapString(coverageMap); 
		}
	}
	
	public String getNovelStartMapString(Map<String, Set<Integer>> novelStartMap) {
		StringBuilder b = new StringBuilder();
		if (isSingleBasePosition()) {
			b.append(novelStartMap.get("A").size() + "\t");
			b.append(novelStartMap.get("C").size() + "\t");			
			b.append(novelStartMap.get("G").size() + "\t");
			b.append(novelStartMap.get("T").size() + "\t");
			b.append(novelStartMap.get("N").size() + "\t");
		} else {
			for ( Entry<String, Set<Integer>> entry: novelStartMap.entrySet()) {
				b.append(entry.getValue().size() + "\t");
			}
		}		
		return b.toString();
	}
	
	
	private int getNovelCounts(String base,
			Map<String, Set<Integer>> forNovelStartMap,
			Map<String, Set<Integer>> revNovelStartMap) {
		return forNovelStartMap.get(base).size() + revNovelStartMap.get(base).size();
	}
	
	private String getNovelStartMapString(
			Map<String, Set<Integer>> forNovelStartMap,
			Map<String, Set<Integer>> revNovelStartMap) {
		StringBuilder b = new StringBuilder();
		if (isSingleBasePosition()) {
			b.append(getNovelCounts("A",forNovelStartMap, revNovelStartMap)+ "\t");
			b.append(getNovelCounts("C",forNovelStartMap, revNovelStartMap) + "\t");			
			b.append(getNovelCounts("G",forNovelStartMap, revNovelStartMap) + "\t");
			b.append(getNovelCounts("T",forNovelStartMap, revNovelStartMap) + "\t");
			b.append(getNovelCounts("N",forNovelStartMap, revNovelStartMap) + "\t");
		} else {
			Set<String> keys = new TreeSet<String>(forNovelStartMap.keySet());
			keys.addAll(revNovelStartMap.keySet());

			for (String key: keys) {
				int count = 0;
				if (forNovelStartMap.containsKey(key)) {
					count += forNovelStartMap.get(key).size();
				}
				if (revNovelStartMap.containsKey(key)) {
					count += revNovelStartMap.get(key).size();
				}
				b.append(count + "\t");
			}
		}		
		return b.toString();
	}	

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		String refBases = getReferenceBases();
		calculateTotalReferenceBases(refBases);
		String start = input.toString() + "\t" + position.toTabString() + "\t" + refBases + "\t" 
		+ referenceBaseCount + "\t" + nonReferenceBaseCount + "\t";
		b.append(start);
		if (!isStrand) {			
			if (isNovelStarts) {
				b.append(getNovelStartMapString(forNovelStartMap, revNovelStartMap));	
			} else {
				b.append(getMapString(coverageMap));	
			}						
			b.append(passFiltersCount);
		} else {			
			b.append(getCountsString(forCoverageMap, forNovelStartMap));		
			b.append(countPositiveStrand);
			b.append("\t");
			b.append(getCountsString(revCoverageMap, revNovelStartMap));
			b.append(countNegativeStrand);			
		}
		return b.toString();
	}
	
	public String toColumnString() {
		StringBuilder b = new StringBuilder();
		b.append(position.toOutputColumnsString() +  "\t");
		b.append(input.getAbbreviatedBamFileName() + "\t" + getAllBaseCounts(forCoverageMap, revCoverageMap));
		return b.toString();
	}	
	
	private String getAllBaseCounts(Map<String, AtomicInteger> forCoverageMap, Map<String, AtomicInteger> revCoverageMap) {
		StringBuilder b = new StringBuilder();
		if (isSingleBasePosition()) {
			b.append(forCoverageMap.get("A").intValue() + revCoverageMap.get("A").intValue() + "\t");
			b.append(forCoverageMap.get("C").intValue() + revCoverageMap.get("C").intValue() + "\t");	
			b.append(forCoverageMap.get("G").intValue() + revCoverageMap.get("G").intValue() + "\t");
			b.append(forCoverageMap.get("T").intValue() + revCoverageMap.get("T").intValue() + "\t");
			b.append(forCoverageMap.get("N").intValue() + revCoverageMap.get("N").intValue() + "\t");
		} 
		return b.toString();
	}

	public String toCompoundString() {
		StringBuilder b = new StringBuilder();
		String refBases = getReferenceBases();
		String altBases = getCompoundReferenceBases();
		calculateTotalReferenceBases(refBases);

		b.append(input.toString() + "\t" + position.toTabString() + "\t" + refBases + "\t" + getCompoundReferenceBases() + "\t"
		+ countPositiveStrand + "\t" + getCompoundBaseCount(forCoverageMap, refBases) + "\t" + getCompoundBaseCount(forCoverageMap, altBases) + "\t" + getOtherCompoundBaseCount(forCoverageMap, refBases, altBases)
		+ "\t" + countNegativeStrand + "\t" + getCompoundBaseCount(revCoverageMap, refBases) +"\t"+ getCompoundBaseCount(revCoverageMap, altBases)  + "\t" + getOtherCompoundBaseCount(revCoverageMap, refBases, altBases));
		
		return b.toString();
	}

	private int getOtherCompoundBaseCount(Map<String, AtomicInteger> map, String refBases,
			String altBases) {
		int count = 0;
		for (Entry<String, AtomicInteger> entry: map.entrySet()) {
			if (!entry.getKey().equals(refBases) && !entry.getKey().equals(altBases)) {
				count += entry.getValue().intValue();
			}
		}
		return count;
	}

	private int getCompoundBaseCount(Map<String, AtomicInteger> coverageMap, String bases) {
		if (coverageMap.containsKey(bases)) {
			return coverageMap.get(bases).intValue();
		} else {
			return 0;
		}
	}

	private void calculateTotalReferenceBases(String refBases) {
		referenceBaseCount = 0;
		nonReferenceBaseCount = 0;
		if (isNovelStarts) {
			countNovelStartReferenceBases(refBases, forNovelStartMap);
			countNovelStartReferenceBases(refBases, revNovelStartMap);			
			
		} else {
			if (isStrand) {
				countCoverageReferenceBases(refBases, forCoverageMap);
				countCoverageReferenceBases(refBases, revCoverageMap);
			} else {
				countCoverageReferenceBases(refBases, coverageMap);
			}			
		}
	}

	private void countCoverageReferenceBases(String refBases,
			Map<String, AtomicInteger> map) {
		for (Entry<String, AtomicInteger> entry : map.entrySet()) {
			if (entry.getKey().equals(refBases)) {
				referenceBaseCount += entry.getValue().intValue();
			} else {
				nonReferenceBaseCount += entry.getValue().intValue();
			}
		}	
		
	}

	private void countNovelStartReferenceBases(String refBase, Map<String, Set<Integer>> map) {
		for (Entry<String, Set<Integer>> entry: map.entrySet()) {
			if (entry.getKey().equals(refBase)) {
				referenceBaseCount += entry.getValue().size();
			} else {
				nonReferenceBaseCount += entry.getValue().size();
			}
		}		
	}

	public String toMafString() {
		StringBuilder sb = new StringBuilder();
		String indelCheck = "";
		if (indelPresent > 0) {
			indelCheck = "yes";
		}
		sb.append(position.getInputLine());
		sb.append("\t" + indelCheck);
		return sb.toString();
	}

}
