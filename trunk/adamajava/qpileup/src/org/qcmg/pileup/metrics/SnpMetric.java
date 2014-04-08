/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.VCFRecord;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.pileup.PileupConstants;
import org.qcmg.pileup.PileupUtil;
import org.qcmg.pileup.metrics.record.ResultRecord;
import org.qcmg.pileup.metrics.record.ResultSummary;
import org.qcmg.pileup.metrics.record.SnpRecord;
import org.qcmg.pileup.model.Chromosome;
import org.qcmg.pileup.model.QPileupRecord;
import org.qcmg.pileup.model.StrandEnum;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;
import org.qcmg.vcf.VCFFileReader;


public class SnpMetric extends Metric {
	
	private double snpPercentNonRef;
	private File snpComparisonFile;
	private String snpFileFormat;
	private File dccOutputFile;
	private BufferedWriter tabFileWriter;
	private BufferedWriter dccFileWriter;
	private int highSnpNonRefCount;
	private int snpNonRefCount;
	final static String TAB_DELIMITER = PileupConstants.TAB_DELIMITER;
	private File dbSnpFile;
	private String snpFileAnnotation;
	private Map<String,TreeMap<Integer,SnpRecord>> snpMap = new ConcurrentHashMap<String,TreeMap<Integer,SnpRecord>>();
	private QLogger logger = QLoggerFactory.getLogger(getClass());
	private File snpOutputFile;
	private File germlineDBFile;
	private String hdfName;
	private String uuid;
	private String snpTmpFileStem;
	private String hdfFilePath;
	private Integer minStrandBiasDifference = new Integer(10);
	private String snpLowConfStem;
	private double minSnpPercentNonRef = 0.1;
	private double minSnpNonRefCount = 5;
	private int minHighSnpNonRefCount = 0;


	public SnpMetric(String hdfFile, String hdfFilePath, String pileupDir, String snpDir, int percentNonRef, int nonRefCount, int highNonRefCount,
			File dbSnpFile, File germlineDBFile, File snpFile, String snpFileFormat, 
			String snpFileAnnotation, Integer minWindowCount, String tmpDir) {
		super(PileupConstants.METRIC_SNP, 1.0, minWindowCount, 0);		
		this.hdfName = hdfFile;
		this.hdfFilePath = hdfFilePath;
		this.snpTmpFileStem = tmpDir + PileupConstants.FILE_SEPARATOR + PileupConstants.FILE_SEPARATOR +  hdfFile + "."+PileupConstants.METRIC_SNP;
		this.snpLowConfStem = snpDir + PileupConstants.FILE_SEPARATOR + PileupConstants.FILE_SEPARATOR +  hdfFile + "."+PileupConstants.METRIC_SNP + ".lowconf.";
		this.snpOutputFile = new File(pileupDir + PileupConstants.FILE_SEPARATOR +  hdfFile + "."+PileupConstants.METRIC_SNP+".txt");
		this.dccOutputFile = new File(pileupDir + PileupConstants.FILE_SEPARATOR + hdfFile + ".dcc");		
		this.snpPercentNonRef = (double)percentNonRef/100.0;
		this.snpNonRefCount = nonRefCount;
		this.highSnpNonRefCount = highNonRefCount;
		this.dbSnpFile = dbSnpFile;
		this.germlineDBFile = germlineDBFile;
		if (snpFile != null) {
			this.snpComparisonFile = snpFile;
			this.snpFileFormat =  snpFileFormat;
			this.snpFileAnnotation =  snpFileAnnotation;
		}		
	}
	
	public void setMinStrandBiasDifference(Integer minStrandBiasDifference) {
		this.minStrandBiasDifference = minStrandBiasDifference;
	}
	
	public double getSnpPercentNonRef() {
		return snpPercentNonRef;
	}

	public void setSnpPercentNonRef(double snpPercentNonRef) {
		this.snpPercentNonRef = snpPercentNonRef;
	}

	public File getSnpFile() {
		return snpComparisonFile;
	}

	public void setSnpFile(File snpFile) {
		this.snpComparisonFile = snpFile;
	}

	public String getSnpFileFormat() {
		return snpFileFormat;
	}

	public void setSnpFileFormat(String snpFileFormat) {
		this.snpFileFormat = snpFileFormat;
	}

	public File getDccFile() {
		return dccOutputFile;
	}

	public void setDccFile(File dccFile) {
		this.dccOutputFile = dccFile;
	}

	public BufferedWriter getTabFileWriter() {
		return tabFileWriter;
	}

	public void setTabFileWriter(BufferedWriter tabFileWriter) {
		this.tabFileWriter = tabFileWriter;
	}

	public BufferedWriter getDccFileWriter() {
		return dccFileWriter;
	}

	public void setDccFileWriter(BufferedWriter dccFileWriter) {
		this.dccFileWriter = dccFileWriter;
	}

	public int getHighSnpNonRefCount() {
		return highSnpNonRefCount;
	}

	public void setHighSnpNonRefCount(int highSnpNonRefCount) {
		this.highSnpNonRefCount = highSnpNonRefCount;
	}

	public int getSnpNonRefCount() {
		return snpNonRefCount;
	}

	public void setSnpNonRefCount(int snpNonRefCount) {
		this.snpNonRefCount = snpNonRefCount;
	}

	public File getDbSnpFile() {
		return dbSnpFile;
	}

	public void setDbSnpFile(File dbSnpFile) {
		this.dbSnpFile = dbSnpFile;
	}

	public String getSnpFileAnnotation() {
		return snpFileAnnotation;
	}

	public void setSnpFileAnnotation(String snpFileAnnotation) {
		this.snpFileAnnotation = snpFileAnnotation;
	}
	
	public Map<String, TreeMap<Integer, SnpRecord>> getSnpMap() {
		return snpMap;
	}

	public void setSnpMap(Map<String, TreeMap<Integer, SnpRecord>> snpMap) {
		this.snpMap = snpMap;
	}
	
	public List<String> getOptionsSummary() {
		List<String> list = new ArrayList<String>();
		list.add("Metric type: " + type);
		list.add("Minimum count per window: " + windowCount);
		list.add("Output text file: " + snpOutputFile);
		list.add("Output dcc file: " + dccOutputFile);
		list.add("SNP non-reference count: " + snpNonRefCount);
		list.add("SNP percent non-reference: " + snpPercentNonRef);
		list.add("SNP high non-reference count: " + highSnpNonRefCount);
		list.add("dbSNP file: " + dbSnpFile);
		list.add("GermlineDB file: " + germlineDBFile);
		list.add("SNP comparison file: " + snpComparisonFile);
		list.add("SNP comparison file format: " + snpFileFormat);
		list.add("SNP comparison file annotation: " + snpFileAnnotation);
		return list;
	}
	
	@Override
	public void addChromosome(String chromosome) {
		if (!snpMap.containsKey(chromosome)) {
			TreeMap<Integer, SnpRecord> map = new TreeMap<Integer, SnpRecord>();
			snpMap.put(chromosome, map);
		}
		if (!summaryMap.containsKey(chromosome)) {
			TreeMap<Integer, ResultSummary> map = new TreeMap<Integer, ResultSummary>();
			summaryMap.put(chromosome, map);
		}
	}

	@Override
	public void processRecord(QPileupRecord record, int totalReads) throws Exception {
		SnpRecord r = getSNPRecord(record, totalReads, minSnpPercentNonRef, minSnpNonRefCount, minHighSnpNonRefCount);	
		if (r != null) {			
			addRecord(r, recordCount.incrementAndGet(), record.getChromosome());
		}
	}
	
	@Override
	public ResultRecord getWindow(String name, int start, int end, long total) {
		ResultRecord rr = null;
		if (snpMap.containsKey(name)) {
			
			NavigableMap<Integer, SnpRecord> subMap = snpMap.get(name).subMap(start, true, end, true);
			long size = subMap.size();			
			long totalCount = getTotalSnpCount(subMap);
				
			windowDistribution.addBaseCounts(total, totalCount);		
			
			if (size >= windowCount) {				
				rr = new ResultRecord(type, size, totalCount, 0);
			} 			
		}
		
		return rr;
	}
	
	public SnpRecord getSNPRecord(QPileupRecord record, int totalReads, double snpPercentNonRef, double snpNonRefCount, int highSnpNonRefCounts) {
		
		char refBase = record.getBase();
		String chr = record.getChromosome();
		int pos = (int) record.getBasePosition();
		long totalRefBases = record.getTotalElement(StrandEnum.referenceNo.toString());
		long totalNonRefBases = record.getTotalElement(StrandEnum.nonreferenceNo.toString());
		long totalBases = totalRefBases + totalNonRefBases;
		
		long highNonRef = record.getTotalElement("highNonreference");
		SnpRecord r = null;
		if (totalNonRefBases > 0) {
			double percent = 0;
			if (totalBases > 0) {
				percent = (double) totalNonRefBases / (double) totalBases;
			}			
			if (passesFilters(percent, (int)totalNonRefBases, (int)highNonRef, snpPercentNonRef, snpNonRefCount, highSnpNonRefCounts) ) {
				Character altBase = record.getAltBase(true, true);
				
				if (altBase != null) {	
					if (altBase != 'N') {
						
						r = new SnpRecord(chr, pos, refBase, record.getTotalBasesString(), altBase, uuid, totalReads,  (int) totalBases, totalPatients);
						r.setTotalBases(totalBases);
						r.setTotalReferenceBases(totalRefBases);
						r.setTotalNonReferenceBases(totalNonRefBases);
						r.setHighNonRef(highNonRef);
						r.setDCCBaseCountString(record.getDCCBaseCountString());	
						r.setNormalGenotype(record.getGenotypeEnum());
						r.setAltBaseCount(record.getTotalAltBases(altBase));
						
						if (record.getStrandBiasRecord(minStrandBiasDifference) != null) {
							r.setStrandBias(true);
						}
					}
				}
			} 
		}
		return r;
	}
	
	public boolean passesFilters(double percent, int totalNonRefBases,
			int highNonRef, double snpPercentNonRef, double snpNonRefCount, int highSnpNonRefCount) {
		
		if (percent >= snpPercentNonRef && totalNonRefBases>=snpNonRefCount && highNonRef >= highSnpNonRefCount) {
			return true;
		}
		return false;
	}	

	@Override
	public void writeHeader(String hdfHeader, String execHeader, String uuid, String wiggleDir) throws IOException {
		this.uuid = uuid;
		tabFileWriter = new BufferedWriter(new FileWriter(snpOutputFile));
		dccFileWriter = new BufferedWriter(new FileWriter(dccOutputFile));
		tabFileWriter.write("#" + hdfFilePath + "\n");
		tabFileWriter.write(getColumnHeaders());
		dccFileWriter.write("#" + hdfFilePath + "\n");
		dccFileWriter.write(hdfHeader);	
		dccFileWriter.write(execHeader);	
		dccFileWriter.write(getDccColumnHeaders());
		tabFileWriter.close();
		dccFileWriter.close();
		this.wiggleFile = new File(wiggleDir + PileupConstants.FILE_SEPARATOR +  type + ".wig");
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(wiggleFile, true));
		writer1.write("track type=wiggle_0 name="+ type + PileupConstants.NEWLINE);
		writer1.close();
	}

	@Override
	public String getColumnHeaders() {
		StringBuilder baseString = new StringBuilder();
		for (StrandEnum e: StrandEnum.getBaseCounts()) {
			baseString.append(e+ TAB_DELIMITER); 
		}
		return "Chr"+ TAB_DELIMITER +"Pos"+ TAB_DELIMITER +"RefBase"+ TAB_DELIMITER + "AltBase"+ TAB_DELIMITER +"RefCount"+ TAB_DELIMITER +"NonRefCount"+ TAB_DELIMITER +"AltBaseCount"+ TAB_DELIMITER
		+ baseString.toString() + baseString.toString() + "Genotype"  + TAB_DELIMITER+ "Notes\thighNonRefCount\tGMAF\tGMAFDifference\tAlleleFreq\tAFDifference\n"; 
	}
	

	public String getDccColumnHeaders() {
		return "analysis_id"+ TAB_DELIMITER +"tumour_sample_id"+ TAB_DELIMITER +"mutation_id"+ 
				TAB_DELIMITER +"mutation_type"+ TAB_DELIMITER +"chromosome"+ TAB_DELIMITER +"chromosome_start"+ 
				TAB_DELIMITER +"chromosome_end"+ TAB_DELIMITER +"chromosome_strand"+ TAB_DELIMITER +"refsnp_allele"+ 
				TAB_DELIMITER +"refsnp_strand"+ TAB_DELIMITER +"reference_genome_allele"+ TAB_DELIMITER +"control_genotype"+ 
				TAB_DELIMITER +"tumour_genotype"+ TAB_DELIMITER +"mutation"+ TAB_DELIMITER +"expressed_allele"+  TAB_DELIMITER +"quality_score"+  
				TAB_DELIMITER +"probability"+ TAB_DELIMITER +"read_count"+ TAB_DELIMITER +"is_annotated"+ 
				TAB_DELIMITER +"validation_status"+ TAB_DELIMITER +"validation_platform"+ 
				TAB_DELIMITER +"xref_ensembl_var_id"+ TAB_DELIMITER +"note"+ TAB_DELIMITER +"QCMGflag" + TAB_DELIMITER 
				+ "ND" + TAB_DELIMITER + "TD" + TAB_DELIMITER + "NNS" + TAB_DELIMITER + "FlankSeq" + TAB_DELIMITER + "HighNonRefCount\tGMAF\tGMAFDifference\tAlleleFreq\tAFDifference\n"; 
	}
	

	
	public void addRecord(SnpRecord snpRec, long count, String chromosome) throws Exception {	
		snpRec.setId(hdfName + "_" + count);
		if (snpMap.containsKey(snpRec.getChromosome())) {
			snpMap.get(snpRec.getChromosome()).put(snpRec.getPosition(), snpRec);
		} else {
			TreeMap<Integer,SnpRecord> map = new TreeMap<Integer, SnpRecord>();
			map.put(snpRec.getPosition(), snpRec);
			snpMap.put(snpRec.getChromosome(), map);
		}
	}	

	private void processSnpFileComparison() throws Exception {
		if (snpFileFormat.equals("vcf")) {
			processVCF(snpComparisonFile, snpFileAnnotation);
		}		
	}
	
	private void processGermlineDbSNP() throws Exception {		
		processVCF(germlineDBFile, "GERMDB");		
	}

	public void processVCF(File file, String annotation) throws Exception {
		VCFFileReader reader = new VCFFileReader(file);		
		Iterator<VCFRecord> iter = reader.iterator();
		int count = 0;
		while (iter.hasNext()) {
			VCFRecord vcfRecord = iter.next();
			
			if (++count % 10000000 == 0) {
				logger.info("Processed " + count  + " " + annotation + " records");
			}
			
			String chr = PileupUtil.getFullChromosome(vcfRecord.getChromosome());
			if (snpMap.containsKey(chr)) {
				SnpRecord snpRecord = snpMap.get(chr).get(vcfRecord.getPosition());
				if (null == snpRecord) continue;
				if (annotation.equals("GERMDB")) {
					snpRecord.setGermdb(annotation); 
				} else {
					snpRecord.setComparisonFileAnnotation(annotation);
				}
			}			
		}
		
		reader.close();		
	}

	public void processDbSnp() throws Exception {
		
		VCFFileReader reader = new VCFFileReader(dbSnpFile);		
		Iterator<VCFRecord> iter = reader.iterator();
		int count = 0;
		while (iter.hasNext()) {
			VCFRecord vcfRecord = iter.next();
			if (++count % 10000000 == 0) {
				logger.info("Processed " + count  + " dbSNP records");
			}
			//ChrPosition c = new ChrPosition(vcfRecord.getChromosome(), vcfRecord.getPosition());
			//System.out.println(c.toString());
			// vcf dbSNP record chromosome does not contain "chr", whereas the positionRecordMap does - add
			if (null == snpMap.get(vcfRecord.getChromosome())) continue;
			
			SnpRecord snpRecord = snpMap.get(vcfRecord.getChromosome()).get(vcfRecord.getPosition());			
			
			if (null == snpRecord) continue;
			
			// only proceed if we have a SNP variant record
			if ( ! StringUtils.doesStringContainSubString(vcfRecord.getInfo(), "VC=SNV", false)) continue;			
					
			// multiple dbSNP entries can exist for a position.
			// if we already have dbSNP info for this snp, check to see if the dbSNP alt is shorter than the existing dbSNP record
			// if so, proceed, and re-write dbSNP details (if applicable).
			int dbSNPAltLengh = vcfRecord.getAlt().length(); 
			
			if (snpRecord.getDbSnpAltLength() > 0 && dbSNPAltLengh > snpRecord.getDbSnpAltLength()) {
				continue;
			}
			
			// deal with multiple alt bases
			String [] alts = null;
			if (dbSNPAltLengh == 1) {
				alts = new String[] {vcfRecord.getAlt()};
			} else if (dbSNPAltLengh > 1){
				alts = TabTokenizer.tokenize(vcfRecord.getAlt(), ',');
			}
			
			if (null != alts) {
				
				for (String alt : alts) {		
					
					GenotypeEnum dbSnpGenotype = BaseUtils.getGenotypeEnum(vcfRecord.getRef() +  alt);
					if (null == dbSnpGenotype) {
						continue;
					}					

//					if (tumour == dbSnpGenotype || (tumour.isHomozygous() && dbSnpGenotype.containsAllele(tumour.getFirstAllele()))) {
						boolean reverseStrand = StringUtils.doesStringContainSubString(vcfRecord.getInfo(), "RV", false);
						snpRecord.setDbSnpStrand(reverseStrand ? '-' : '+');
						snpRecord.setDbSnpId(vcfRecord.getId());
						snpRecord.setDbSnpGenotype(dbSnpGenotype);
						snpRecord.setDbSnpAltLength(dbSNPAltLengh);
						if (vcfRecord.getInfo().contains("GMAF")) {
							String[] values = vcfRecord.getInfo().split(";");
							for (String s: values) {
								if (s.contains("GMAF=")) {
									snpRecord.setdbSNPMAF(s);
								}
							}
						}
						break;
//					}
				}
			}
			
		}
		reader.close();
	}
	
	@Override
	public void clear(Chromosome chromosome) {
		snpMap.get(chromosome.getName()).clear();
	}

	private void writeMap() throws IOException {
		for (Entry<String, TreeMap<Integer, SnpRecord>> entry: snpMap.entrySet()) {	
			TreeMap<Integer, SnpRecord> map = entry.getValue();
			if (map != null) {
				for (Entry<Integer, SnpRecord> currentEntry: map.entrySet()) {			
					writeRecord(currentEntry.getValue());
				}
				//clear values after writing
				map.clear();
			}		
		}
	}
	
	public int size(Chromosome chromosome) {
		return snpMap.get(chromosome.getName()).size();
	}
	
	public void writeTmpRecords(Chromosome chromosome) throws IOException {
		BufferedWriter tmpRecordWriter = new BufferedWriter(new FileWriter(new File(snpTmpFileStem + chromosome.getName()), true));
		TreeMap<Integer, SnpRecord> map = snpMap.get(chromosome.getName());
		if (map != null) {
			for (Entry<Integer, SnpRecord> currentEntry: map.entrySet()) {			
				tmpRecordWriter.write(currentEntry.getValue().toString());
			}
			//clear values after writing
			snpMap.get(chromosome.getName()).clear();
		}
		tmpRecordWriter.close();
	}

	public void writeRecord(SnpRecord record) throws IOException {				
		tabFileWriter.write(record.toTabString());		
		dccFileWriter.write(record.toDCCString());
	}	

	public synchronized void writeRecords(String gffName, StrandEnum[] strandEnums, List<Chromosome> chromosomes) throws Exception {
		snpMap.clear();
		tabFileWriter = new BufferedWriter(new FileWriter(snpOutputFile, true));
		dccFileWriter = new BufferedWriter(new FileWriter(dccOutputFile, true));
		Map<String, TreeMap<ChrPosition, TabbedRecord>> mismapMap = readGFFRecords(gffName);
		
		for (Chromosome c : chromosomes) {
			
			readTmpRecords(c);		
			
			if (snpMap.containsKey(c.getName()) && snpMap.get(c.getName()).size() > 0) {
				if (dbSnpFile != null) {
					processDbSnp();			
				}
				if (germlineDBFile != null) {
					processGermlineDbSNP();			
				}
				if (snpComparisonFile != null) {
					processSnpFileComparison();
				}
				
				processMisMapRegion(mismapMap);
				//compareToOtherHDFs(strandEnums);
				findNearestNeighbours();
				writeMap();
				snpMap.get(c.getName()).clear();
			}
				
		}
				
		tabFileWriter.close();
		dccFileWriter.close();
		snpMap.clear();
		
	}

	private void processMisMapRegion(Map<String, TreeMap<ChrPosition, TabbedRecord>> mismapMap) throws Exception {		
		
		for (Entry<String, TreeMap<Integer, SnpRecord>> entry : snpMap.entrySet()) {
			for (Entry<Integer, SnpRecord> currentEntry : entry.getValue().entrySet()) {
				SnpRecord snp = currentEntry.getValue();
				ChrPosition chrPos = new ChrPosition(snp.getChromosome(), snp.getPosition(), snp.getEndPosition());
				if (mismapMap.containsKey(snp.getChromosome())) {
					TreeMap<ChrPosition, TabbedRecord> compareMap = mismapMap.get(snp.getChromosome());					
					Entry<ChrPosition, TabbedRecord> floor = compareMap.floorEntry(chrPos);
					Entry<ChrPosition, TabbedRecord> ceiling = compareMap.ceilingEntry(chrPos);
					
					if (tabbedRecordFallsInCompareRecord(chrPos, floor) || tabbedRecordFallsInCompareRecord(chrPos, ceiling)) {
						snp.setInMismapRegion(true);
					}					
				}
			}
		}		
	}
	
	public boolean tabbedRecordFallsInCompareRecord(ChrPosition inputChrPos, Entry<ChrPosition, TabbedRecord> entry) {
		if (entry != null) {
			ChrPosition compareChrPos = entry.getKey();
			if ((inputChrPos.getPosition() >= compareChrPos.getPosition() && inputChrPos.getPosition() <= compareChrPos.getEndPosition()) ||
					(inputChrPos.getEndPosition() >= compareChrPos.getPosition() && inputChrPos.getEndPosition() <= compareChrPos.getEndPosition()) 
					|| (inputChrPos.getPosition() <= compareChrPos.getPosition() && inputChrPos.getEndPosition() >= compareChrPos.getEndPosition())) {
				return true;
			}		
		}
		return false;
	}

	private Map<String, TreeMap<ChrPosition, TabbedRecord>> readGFFRecords(String gffFile) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(gffFile));
				
		Map<String, TreeMap<ChrPosition, TabbedRecord>> map = new HashMap<String, TreeMap<ChrPosition, TabbedRecord>>();
		
		Iterator<TabbedRecord> iterator = reader.getRecordIterator();
			
		while (iterator.hasNext()) {
			
			TabbedRecord tab = iterator.next();
			
			if (tab.getData().startsWith("#") || tab.getData().startsWith("Hugo") || tab.getData().startsWith("analysis")) {					
				continue;
			}
			
			String[] values = tab.getData().split("\t");
			String key = values[0];
			ChrPosition chrPos = new ChrPosition(key, new Integer(values[3]), new Integer(values[4])); 
			if (map.containsKey(key)) {
				map.get(key).put(chrPos, tab);
			} else {				
				TreeMap<ChrPosition, TabbedRecord> tmap = new TreeMap<ChrPosition, TabbedRecord>();
				tmap.put(chrPos, tab);
				map.put(key, tmap);
			}
			
		}
		
		reader.close();
		return map;
		
	}

	private void readTmpRecords(Chromosome c) throws Exception {
		File tmpFile = new File(snpTmpFileStem + c.getName());
		if (tmpFile.exists()) {
			TabbedFileReader reader = new TabbedFileReader(tmpFile);
			BufferedWriter dccWriter = new BufferedWriter(new FileWriter(snpLowConfStem + c.getName() + ".dcc"));
			BufferedWriter txtWriter = new BufferedWriter(new FileWriter(snpLowConfStem + c.getName() + ".snp.txt"));
			dccWriter.write(getDccColumnHeaders());
			txtWriter.write(getColumnHeaders());
			Iterator<TabbedRecord> iterator = reader.getRecordIterator();
			int count = 0;
			while (iterator.hasNext()) {			
				TabbedRecord r = iterator.next();
				count++;
				String[] values = r.getData().split("\t");
				String baseString = "";
				for (int i=6; i<=15; i++) {
					baseString += values[i] + "\t";
				}
				
				SnpRecord snpRecord = new SnpRecord(values[0], Integer.parseInt(values[1]), values[2].charAt(0), baseString, values[3].charAt(0), uuid, 0, Integer.parseInt(values[4]) + Integer.parseInt(values[5]), totalPatients);
				snpRecord.setTotalReferenceBases((Integer.parseInt(values[4])));
				snpRecord.setTotalNonReferenceBases((Integer.parseInt(values[5])));
				snpRecord.setHighNonRef(Integer.parseInt(values[16]));
				snpRecord.setCount((long)count);
				snpRecord.setDccBaseCountString(values[17]);
				if (!values[18].equals("--")) {
					snpRecord.setNormalGenotype(GenotypeEnum.valueOf(values[18]));
				}
				snpRecord.setAltBaseCount(new Integer(values[19]));
				if (values[20].equals("true")) {
					snpRecord.setStrandBias(true);
				}
				//is a good snp, do more processing, otherwise write to the low conf file/s
				if (passesFilters(snpRecord.getAlleleFrequency(), snpRecord.getAltBaseCount(), (int) snpRecord.getHighNonRef(), snpPercentNonRef, snpNonRefCount, highSnpNonRefCount)) {
					addRecord(snpRecord, count, snpRecord.getChromosome());
				} else {
					dccWriter.write(snpRecord.toDCCString());
					txtWriter.write(snpRecord.toTabString());
				}
				
			}
			dccWriter.close();
			txtWriter.close();
			reader.close();
			new File(snpTmpFileStem + c.getName()).delete();
		}
	}

	private void findNearestNeighbours() {
		
		for (Entry<String, TreeMap<Integer, SnpRecord>> entry: snpMap.entrySet()) {		
			TreeMap<Integer, SnpRecord> map = entry.getValue();
			if (map != null) {
				for (Entry<Integer, SnpRecord> currentEntry: map.entrySet()) {			
					SnpRecord record = currentEntry.getValue();
					Integer higher = map.higherKey(currentEntry.getKey());
					Integer lower = map.lowerKey(currentEntry.getKey());
					if (higher != null && lower != null) {
						record.setNearestNeighbour(Math.min(higher, lower));
					} else if (higher != null) {
						record.setNearestNeighbour(higher);
					} else {
						if (lower != null) {
							record.setNearestNeighbour(lower);
						}
					}			
				}		
			}
		}
	}

	private int getTotalSnpCount(NavigableMap<Integer, SnpRecord> subMap) {
		int totalSizeCount = 0;
		for (Entry<Integer, SnpRecord> entry: subMap.entrySet()) {
			totalSizeCount += entry.getValue().getCount();
		}
		return totalSizeCount;
	}
	
	
}
