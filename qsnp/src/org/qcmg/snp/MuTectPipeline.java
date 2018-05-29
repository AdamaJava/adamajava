/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

import org.ini4j.Ini;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.GenotypeEnum;
import org.qcmg.common.model.PileupElementLite;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.QDccMetaFactory;
import org.qcmg.picard.util.SAMUtils;
import org.qcmg.pileup.QSnpRecord;
import org.qcmg.common.model.Classification;
import org.qcmg.snp.util.HeaderUtil;
import org.qcmg.snp.util.IniFileUtil;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

/**
 */
public final class MuTectPipeline extends Pipeline {
	
	private final static ReferenceNameComparator COMPARATOR = new ReferenceNameComparator();
	
	private static final DecimalFormat NF = new DecimalFormat("#.##");
	
	private final static QLogger logger = QLoggerFactory.getLogger(MuTectPipeline.class);
	
	private final ConcurrentMap<ChrPosition, Accumulator> normalPileup = new ConcurrentHashMap<ChrPosition, Accumulator>();
	private final ConcurrentMap<ChrPosition, Accumulator> tumourPileup = new ConcurrentHashMap<ChrPosition, Accumulator>();

	private final ConcurrentMap<ChrPosition, String[]> map = new ConcurrentHashMap<ChrPosition, String[]>();
	
	//input Files
	private String muTectFile;
	private String tumourBam;

	/**
	 */
	public MuTectPipeline(final Ini iniFile, QExec qexec) throws SnpException, IOException, Exception {
		super(qexec, false);

		// load rules from ini file
		ingestIni(iniFile);
		
		// setup normalBam and tumourBam variables
		if (null != testBams && testBams.length > 0)
			tumourBam = testBams[0];
		else throw new SnpException("No tumour bam file specified");
		
		
		
		// load MuTect output file
		logger.info("loading MuTect data");
		loadMuTectOutput(muTectFile, map);
		if (map.isEmpty()) throw new SnpException("No MuTect data - empty file??");
		logger.info("number of mutect entries: " + map.size());
		
		for (Entry<ChrPosition,String[]> entry : map.entrySet()) {
			if ("KEEP".equals(entry.getValue()[34])) {
				positionRecordMap.put(entry.getKey(), getQSnpRecord(entry.getValue()));
			}
		}
		logger.info("number of mutect entries that are KEEP: " + positionRecordMap.size());
		
		// dump initial mutect data - save on memory
		map.clear();
		
		// add novel starts
		logger.info("about to get novel starts");
		addNovelStarts();
		logger.info("about to get novel starts - DONE");
		
		// time for post-processing
		classifyPileup();
		
		// write output
		writeVCF(vcfFile);
	}
	
	
	@Override
	String getDccMetaData() throws Exception {
		if (null == controlBams || controlBams.length == 0 || 
				StringUtils.isNullOrEmpty(controlBams[0]) 
				|| null == testBams || testBams.length == 0 
				|| StringUtils.isNullOrEmpty(testBams[0])) return null;
		
		SAMFileHeader controlHeader = SAMFileReaderFactory.createSAMFileReader(new File(controlBams[0])).getFileHeader();
		SAMFileHeader analysisHeader = SAMFileReaderFactory.createSAMFileReader(new File(testBams[0])).getFileHeader();
		
		QDccMeta dccMeta = QDccMetaFactory.getDccMeta(qexec, controlHeader, analysisHeader, "MuTect");
		
		return dccMeta.getDCCMetaDataToString();
	}
	
	private void addNovelStarts() throws IOException {
		
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(tumourBam))) {
			
			for (Entry<ChrPosition, QSnpRecord> entry : positionRecordMap.entrySet()) {
				
				ChrPosition cp = entry.getKey();
				QSnpRecord rec = entry.getValue();
				List<SAMRecord> sams = new ArrayList<>(); 
				
				SAMRecordIterator iter = reader.queryOverlapping(cp.getChromosome(), cp.getStartPosition(), cp.getEndPosition());
				while (iter.hasNext()) {
					SAMRecord sam = iter.next();
					if (SAMUtils.isSAMRecordValidForVariantCalling(sam)) sams.add(sam);
				}
				iter.close();
				
				// now get the novel starts
				Accumulator acc = SAMUtils.getAccumulatorFromReads(sams, cp.getStartPosition());
				
				String altString = rec.getAlt();
				if (altString.length() > 1) {
					logger.warn("alt string: " + altString + " in MuTectPipeline.addNovelStarts");
				}
				char alt = altString.charAt(0);
				String refString = rec.getRef();
				if (refString.length() > 1) {
					logger.warn("ref string: " + refString + " in MuTectPipeline.addNovelStarts");
				}
				char ref = refString.charAt(0);
				
				int nsCount = acc.getNovelStartsCountForBase(alt);
				rec.setTumourNovelStartCount(nsCount);
				
//				rec.setTumourNucleotides(acc.getPileupElementString());
				rec.setTumourOABS(acc.getObservedAllelesByStrand());
				
				// check for strand bias
				PileupElementLite pel = acc.getLargestVariant(ref);
				if (null != pel && ! pel.isFoundOnBothStrands()) {
					VcfUtils.updateFilter(rec.getVcfRecord(), SnpUtils.STRAND_BIAS_ALT);
				}
			}
		}
	}

	/**
	 * 
	 * ## muTector v1.0.47986
	1		contig   - the contig location of this candidate
	2		position    - the 1-based position of this candidate on the given contig    
	3		context 
	4		ref_allele      - the reference allele for this candidate
	5		alt_allele      - the mutant (alternate) allele for this candidate
	6		tumor_name      - name of the tumor as given on the command line, or extracted from the BAM
	7		normal_name     - name of the normal as given on the command line, or extracted from the BAM
	8		score   - for future development
	9		dbsnp_site      - is this a dbsnp site as defined by the dbsnp bitmask supplied to the caller
	10	covered - was the site powered to detect a mutation (80% power for a 0.3 allelic fraction mutation)
	11	power    - tumor_power * normal_power
	12	tumor_power     - given the tumor sequencing depth, what is the power to detect a mutation at 0.3 allelic fraction
	13	normal_power    - given the normal sequencing depth, what power did we have to detect (and reject) this as a germline variant
	14	total_pairs     - total tumor and normal read depth which come from paired reads
	15	improper_pairs  - number of reads which have abnormal pairing (orientation and distance)
	16	map_Q0_reads    - total number of mapping quality zero reads in the tumor and normal at this locus
	17	t_lod_fstar     - CORE STATISTIC: Log of (likelihood tumor event is real / likelihood event is sequencing error )
	18	tumor_f - allelic fraction of this candidated based on read counts
	19	contaminant_fraction    - estimate of contamination fraction used (supplied or defaulted)
	20	contaminant_lod - log likelihood of ( event is contamination / event is sequencing error )
	21	t_ref_count     - count of reference alleles in tumor
	22	t_alt_count     - count of alternate alleles in tumor
	23	t_ref_sum       - sum of quality scores of reference alleles in tumor
	24	t_alt_sum       - sum of quality scores of alternate alleles in tumor
	25	t_ref_max_mapq 
	26	t_alt_max_mapq  
	27	t_ins_count      - count of insertion events at this locus in tumor
	28	t_del_count     - count of deletion events at this locus in tumor
	29	normal_best_gt  - most likely genotype in the normal
	30	init_n_lod      - log likelihood of ( normal being reference / normal being altered )
	31	n_ref_count     - count of reference alleles in normal
	32	n_alt_count     - count of alternate alleles in normal
	33	n_ref_sum       - sum of quality scores of reference alleles in normal
	34	n_alt_sum       - sum of quality scores of alternate alleles in normal
	35	judgement		- final judgement of site KEEP or REJECT (not enough evidence or artifact)
	 * 
	 * @param muTectData
	 * @return
	 */
	public static QSnpRecord getQSnpRecord(String [] mtData) {
		if (null == mtData || mtData.length == 0) throw new IllegalArgumentException("Null or empty string array passed to getQSnpRecord");
		
		char ref = mtData[3].charAt(0);
		char alt = mtData[4].charAt(0);
		int normalRefCount = Integer.parseInt(mtData[30]);
		int normalAltCount = Integer.parseInt(mtData[31]);
		int tumourRefCount = Integer.parseInt(mtData[20]);
		int tumourAltCount = Integer.parseInt(mtData[21]);
		
		QSnpRecord rec = new QSnpRecord(mtData[0], Integer.parseInt(mtData[1]), mtData[3], mtData[4]);
//		rec.setRef(ref);
//		rec.setAlt(alt);
		rec.setMutation(ref + Constants.MUT_DELIM + alt);
		rec.setNormalCount(normalRefCount + normalAltCount);
		rec.setTumourCount(tumourRefCount + tumourAltCount);
		rec.setNormalGenotype(GenotypeEnum.getGenotypeEnum(mtData[28].charAt(0), mtData[28].charAt(1)));
		
		if (tumourRefCount > 0 && tumourAltCount > 0) {
			rec.setTumourGenotype(GenotypeEnum.getGenotypeEnum(ref, alt));
		} else if (tumourAltCount > 0) {
			rec.setTumourGenotype(GenotypeEnum.getGenotypeEnum(alt, alt));
		}
		
		// compose dummy normal nucleotides based on available data - don't want to hit up normal bam file if we can avoid it
		// all on the forward strand...
		// division by zero
		String ND = null;
		String normalPileup = null;
		if (normalRefCount > 0) {
			double normalRefQuality = Double.parseDouble(mtData[32]);
			double aveQual = normalRefQuality / normalRefCount;
			
			ND = ref + normalRefCount + "[" + NF.format(aveQual) + "]0[0]";
			normalPileup = "" + ref;
		}
		
		if (normalAltCount > 0) {
			double normalAltQuality = Double.parseDouble(mtData[33]);
			double aveQual = normalAltQuality / normalAltCount;
			
			if (null == ND) {
				ND = alt + normalAltCount + "[" + NF.format(aveQual) + "]0[0]";
			} else {
				ND += ";" + alt + normalAltCount + "[" + NF.format(aveQual) + "]0[0]";
			}
			
			normalPileup = null == normalPileup ? "" + alt : "" + ref + alt;
		}
		rec.setNormalOABS(ND);
		rec.setNormalPileup(normalPileup);
		
		// hard-coding all to somatic
		rec.setClassification(Classification.SOMATIC);
		
		return rec;
	}
	
//	private  QSnpRecord getQSnpRecord(QSnpGATKRecord normal, QSnpGATKRecord tumour) {
//		QSnpRecord qpr = new QSnpRecord();
//		qpr.setId(++mutationId);
//		
//		if (null != normal) {
//			qpr.setChromosome(normal.getChromosome());
//			qpr.setPosition(normal.getPosition());
//			qpr.setRef(normal.getRef());
//			qpr.setNormalGenotype(normal.getGenotypeEnum());
//			qpr.setAnnotation(normal.getAnnotation());
//			// tumour fields
//			qpr.setTumourGenotype(null == tumour ? null : tumour.getGenotypeEnum());
//			qpr.setTumourCount(null == tumour ? 0 :  VcfUtils.getDPFromFormatField(tumour.getGenotype()));
//			
//		} else if (null != tumour) {
//			qpr.setChromosome(tumour.getChromosome());
//			qpr.setPosition(tumour.getPosition());
//			qpr.setRef(tumour.getRef());
//			qpr.setTumourGenotype(tumour.getGenotypeEnum());
//			qpr.setTumourCount(VcfUtils.getDPFromFormatField(tumour.getGenotype()));
//		}
//		
//		return qpr;
//	}
	
	private static void loadMuTectOutput(String muTectOutput, Map<ChrPosition, String[]> map) {
		try (TabbedFileReader reader = new TabbedFileReader(new File(muTectOutput))) {
			int noOfRecords = 0;
			for (TabbedRecord rec : reader) {
				if (noOfRecords++ > 0) {		// header line in mutect output doesn't have '#'
					String [] params = TabTokenizer.tokenize(rec.getData());
					map.put(ChrPointPosition.valueOf(params[0], Integer.parseInt(params[1])), params);
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void ingestIni(Ini ini) throws SnpException {
		
		super.ingestIni(ini);

		// ADDITIONAL INPUT FILES
		muTectFile = IniFileUtil.getInputFile(ini, "mutect");
		
		// log values retrieved from in file
		logger.tool("**** ADDITIONAL INPUT FILES ****");
		logger.tool("MuTect output file: " + muTectFile);
		
		logger.tool("**** OTHER CONFIG ****");
		logger.tool("mutationIdPrefix: " + mutationIdPrefix);
	}

	@Override
	protected String getFormattedRecord(QSnpRecord record, final String ensemblChr) {
		return record.getDCCDataNSFlankingSeq(mutationIdPrefix, ensemblChr);
	}

	@Override
	protected String getOutputHeader(boolean isSomatic) {
		if (isSomatic) return HeaderUtil.DCC_SOMATIC_HEADER;
		else return HeaderUtil.DCC_GERMLINE_HEADER;
	}
	
	
	/**
	 * Class that reads SAMRecords from a Queue and after checking that they satisfy some criteria 
	 * (ie. not duplicates, reasonable mapping quality) attempts to match the record against the current vcf record.
	 * If there is a match (ie. the sam record encompasses the vcf position, then a record of the base and strand at that position is kept.
	 * 
	 */
	public class Pileup implements Runnable {
//		private final String bamFile;
		private final SamReader reader;
		private final boolean isNormal;
		private final ConcurrentMap<ChrPosition , Accumulator> pileupMap;
		private int arraySize;
		private int arrayPosition;
		private ChrPosition cp;
		private Comparator<String> chrComparator;
		private final List<ChrPosition> snps;
		private final CountDownLatch latch;
		
		public Pileup(final String bamFile, final CountDownLatch latch, final boolean isNormal) {
//			this.bamFile = bamFile;
			this.isNormal = isNormal;
			pileupMap = isNormal ? normalPileup : tumourPileup;
			reader = SAMFileReaderFactory.createSAMFileReader(new File(bamFile));
			snps = new ArrayList<ChrPosition>(positionRecordMap.keySet());
			this.latch = latch;
		}
		
		private void createComparatorFromSAMHeader(SamReader reader) {
			SAMFileHeader header = reader.getFileHeader();
			
			final List<String> sortedContigs = new ArrayList<String>();
			for (SAMSequenceRecord contig : header.getSequenceDictionary().getSequences()) {
				sortedContigs.add(contig.getSequenceName());
			}
			
			// try and sort according to the ordering of the bam file that is about to be processed
			// otherwise, resort to alphabetic ordering and cross fingers...
			if ( ! sortedContigs.isEmpty()) {
				
				chrComparator = new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						return sortedContigs.indexOf(o1) - sortedContigs.indexOf(o2);
					}
				};
				
				Collections.sort(snps, new Comparator<ChrPosition>() {
					@Override
					public int compare(ChrPosition o1, ChrPosition o2) {
						int diff = chrComparator.compare(o1.getChromosome(), o2.getChromosome());
						if (diff != 0) return diff;
						return o1.getStartPosition() - o2.getStartPosition();
					}
				});
				
			} else {
				chrComparator = COMPARATOR;
				Collections.sort(snps, new Comparator<ChrPosition>() {
					@Override
					public int compare(ChrPosition o1, ChrPosition o2) {
						int diff = COMPARATOR.compare(o1.getChromosome(), o2.getChromosome());
						if (diff != 0) return diff;
						return o1.getStartPosition() - o2.getStartPosition();
					}
				});
			}
			
			arraySize = snps.size();
		}
		
		private void advanceCPAndPosition() {
			if (arrayPosition >= arraySize) {
				// reached the end of the line
				cp = null;
				return;
			}
			if (null != cp) {
				// update QSnpRecord with our findings
				Accumulator acc = pileupMap.remove(cp);
				if (null != acc) {
					QSnpRecord rec = positionRecordMap.get(cp);
					
					String refString = rec.getRef();
					if (refString.length() > 1) {
						logger.warn("ref string: " + refString + " in MuTectPipeline.advanceCPAndPosition");
					}
					char ref = refString.charAt(0);
					
					PileupElementLite pel = acc.getLargestVariant(ref);
					if (isNormal) {
						rec.setNormalOABS(acc.getObservedAllelesByStrand());
//						rec.setNormalNucleotides(acc.getPileupElementString());
						rec.setNormalCount(acc.getCoverage());
						rec.setNormalPileup(acc.getPileup());
						rec.setNormalNovelStartCount(null != pel ? pel.getNovelStartCount() : 0);
					} else {
						// tumour fields
						rec.setTumourCount(acc.getCoverage());
						rec.setTumourOABS(acc.getObservedAllelesByStrand());
//						rec.setTumourNucleotides(acc.getPileupElementString());
						rec.setTumourNovelStartCount(null != pel ? pel.getNovelStartCount() : 0);
					}
				}
			}
			cp = snps.get(arrayPosition++);
		}
		
		private boolean match(SAMRecord rec, ChrPosition thisCPf, boolean updatePointer) {
			if (null == thisCPf) return false;
			if (rec.getReferenceName().equals(thisCPf.getChromosome())) {
				
				if (rec.getAlignmentEnd() < thisCPf.getStartPosition())
					return false;
				
				if (rec.getAlignmentStart() <= thisCPf.getStartPosition()) {
					return true;
				}
				
				// finished with this cp - update results and get a new cp
				if (updatePointer) {
					advanceCPAndPosition();
					return match(rec, cp, true);
				} else {
					return false;
				}
				
				
			} else if (chrComparator.compare(rec.getReferenceName(), thisCPf.getChromosome()) < 1){
				// keep iterating through bam file 
				return false;
			} else {
				if (updatePointer) {
					// need to get next ChrPos
					advanceCPAndPosition();
					return match(rec, cp, true);
				} else {
					return false;
				}
			}
		}
		private void updateResults(ChrPosition cp, SAMRecord sam, int readId) {
			// get read index
			final int indexInRead = SAMUtils.getIndexInReadFromPosition(sam, cp.getStartPosition());
			
			if (indexInRead > -1 && indexInRead < sam.getReadLength()) {
				
				// no longer do any filtering on base quality
				final byte baseQuality = sam.getBaseQualities()[indexInRead]; 
				
				Accumulator acc = pileupMap.get(cp);
				if (null == acc) {
					acc = new Accumulator(cp.getStartPosition());
					Accumulator oldAcc = pileupMap.putIfAbsent(cp, acc);
					if (null != oldAcc) acc = oldAcc;
				}
				acc.addBase(sam.getReadBases()[indexInRead], baseQuality, ! sam.getReadNegativeStrandFlag(), 
						sam.getAlignmentStart(), cp.getStartPosition(), sam.getAlignmentEnd(), readId);
			}
		}
		
		@Override
		public void run() {
			try {
				createComparatorFromSAMHeader(reader);
				// reset some key values
				arrayPosition = 0;
				cp = null;
				// load first VCFRecord
				advanceCPAndPosition();
				long recordCount = 0;
				int chrCount = 0;
				// take items off the queue and process
				for (SAMRecord sam : reader) {
					chrCount++;
					if (++recordCount % 1000000 == 0) {
						logger.info("processed " + recordCount/1000000 + "M records so far...");
					}
					
					// quality checks
					if ( ! SAMUtils.isSAMRecordValidForVariantCalling(sam)) continue;
					
					if (match(sam, cp, true)) {
						updateResults(cp, sam, chrCount);
						
						// get next cp and see if it matches
						int j = 0;
						if (arrayPosition < arraySize) {
							ChrPosition tmpCP = snps.get(arrayPosition + j++);
							while (match(sam, tmpCP, false)) {
 								updateResults(tmpCP, sam, chrCount);
								if (arrayPosition + j < arraySize)
									tmpCP = snps.get(arrayPosition + j++);
								else tmpCP = null;
							}
						}
					}
				}
				logger.info("processed " + recordCount + " records");
				
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			}
		}
	}
}
