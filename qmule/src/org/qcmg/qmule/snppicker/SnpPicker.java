/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.snppicker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import htsjdk.samtools.SAMRecord;

import org.qcmg.chrconv.ChrConvFileReader;
import org.qcmg.chrconv.ChromosomeConversionRecord;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.Genotype;
import org.qcmg.common.util.BaseUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.dbsnp.Dbsnp130Record;
import org.qcmg.dbsnp.DbsnpFileReader;
import org.qcmg.gff3.GFF3FileReader;
import org.qcmg.gff3.GFF3Record;
import org.qcmg.illumina.IlluminaFileReader;
import org.qcmg.illumina.IlluminaRecord;
import org.qcmg.picard.QJumper;
import org.qcmg.pileup.PileupFileReader;
import org.qcmg.qmule.Messages;
import org.qcmg.qmule.Options;
import org.qcmg.qmule.QMuleException;
import org.qcmg.record.Record;
import org.qcmg.vcf.VCFFileReader;

public class SnpPicker {
	
	private static final char DEFAULT_CHAR = '\u0000';
	private static QLogger logger;
//	private static DecimalFormat df = new DecimalFormat("0.0000");
	
	private String logFile;
	private String[] cmdLineInputFiles;
	private String[] cmdLineOutputFiles;
	private int exitStatus;
	
	private static boolean isNormal;
	
//	private static final Pattern tabbedPattern = Pattern.compile("[\\t]");
	
	Map<String, IlluminaRecord> illuminaMap = new HashMap<String, IlluminaRecord>(1000000,0.99f);	// not expecting more than 1000000
	
	Map<ChrPosition, VariantRecord> variantMap = new HashMap<ChrPosition, VariantRecord>(2000000);
	
	// map to hold chromosome conversion data
	Map<String, String> gffToQCMG = new HashMap<String, String>(100, 0.99f);
	
//	List<IlluminaRecord> illuminaRecords = new ArrayList<IlluminaRecord>();
//	List<Dbsnp130Record> dbSNPRecords = new ArrayList<Dbsnp130Record>(13000000);
	
	private int engage() throws Exception {
		
		// populate the chromosome conversion map
		logger.info("about to load chromosome conversion data");
		loadChromosomeConversionData();
		logger.info("about to load chromosome conversion data - DONE");
		
		// we are working off the raw illumina data here - first convert it into filtered format, and use that as the input

		logger.info("About to load raw illumina data");
		loadRawIlluminaData();
//		logger.info("No of variant records: " + variantMap.size() + " in file: " + cmdLineInputFiles[0]);
		
		logger.info("About to load gff3 data");
		loadGff3Data();
		logger.info("No of variant records: " + variantMap.size());
		
//		logger.info("About to load vcf data");
//		loadVCFData();
//		logger.info("No of variant records: " + variantMap.size());
		
		logger.info("About to load qsnp data");
		loadQSnpData();
		logger.info("No of variant records: " + variantMap.size());
		
		
		
		
		logger.info("About to load dbSNP data");
		loadDbSnpData();
//		logger.info("No of variant records: " + variantMap.size());
		
		// update variantMap with details from illuminaMap
		logger.info("About to load filtered illumina data into variant map");
		convertIlluminaToVariant();
		logger.info("About to load filtered illumina data into variant map - DONE");
		
		// get some stats
		displayStats();
		
		// pileup
		logger.info("time for pileup...");
		getPileup();
		logger.info("time for pileup - DONE");
		
		// more stats
		displayStats2();
		
		logger.info("Will now attempt to write out variant data" );
		outputVariantData();
		logger.info("Will now attempt to write out variant data - DONE");
		
		return exitStatus;
	}

	private void getPileup() throws Exception {
		QJumper qj = new QJumper();
		qj.setupReader(cmdLineInputFiles[5], cmdLineInputFiles[6]);
		
		VariantRecord rec;
		StringBuilder pileup = new StringBuilder();
		List<SAMRecord> reads;
//		String chr;
		int position;
		int offset;
		
		int pileupCount = 0;
		for (Map.Entry<ChrPosition, VariantRecord> entry : variantMap.entrySet()) {
			// only want pileup if we have gff or vcf data
			rec = entry.getValue();
			if (DEFAULT_CHAR != rec.getGffRef() || null != rec.getVcfGenotype()) {
//				chr = ( ! entry.getKey().getChromosome().startsWith("GL") ? "chr" : "") + entry.getKey().getChromosome();
				
				reads = qj.getRecordsAtPosition(entry.getKey().getChromosome(), entry.getKey().getStartPosition());
				// do something with the reads
				position = entry.getKey().getStartPosition();
				for (SAMRecord sr : reads) {
					offset = position - sr.getAlignmentStart();
					pileup.append((char)sr.getReadBases()[offset]);
				}
				rec.setPileup(pileup.toString());
				
				// reset the StringBuilder
				pileup.setLength(0);
				
				if (++pileupCount % 1000 == 0)
					logger.info("Run " + pileupCount + " pileups so far, " + reads.size() + " sam records returned from picard");
			}
		}
	}

	private void loadChromosomeConversionData() {
		String chrConvFile = cmdLineInputFiles[4];
		ChrConvFileReader reader = null;
		try {
			reader = new ChrConvFileReader(new File(chrConvFile));
		} catch (Exception e) {
			logger.error("Exception caught whilst trying to instantiate ChrConvFileReader", e);
			exitStatus = -1;
		}
		
		if (null != reader) {
			for (ChromosomeConversionRecord record : reader) {
				// add extra map inserts here as required
				// diBayes field is no longer present in chr conv file
//				gffToQCMG.put(record.getDiBayes(), record.getQcmg());
				// guessing we want ensemble in here as the key
				gffToQCMG.put(record.getEnsembleV55(), record.getQcmg());
			}
			
			try {
				reader.close();
			} catch (IOException e) {
				logger.error("IOException caught whilst trying to close ChrConvFileReader", e);
				exitStatus = -1;
			}
		}
	}

	private void displayStats() {
		int illuminaOnly = 0;
		int gff3Only = 0;
		int vcfOnly = 0;
		int vcfANDgff = 0;
		int vcfANDillumina = 0;
		int gffANDillumina = 0;
		int allThree = 0;
		for (VariantRecord record : variantMap.values()) {
			
			boolean illuminaDataPresent = null != record.getIlluminaRef();
			boolean gffDataPresent = DEFAULT_CHAR != record.getGffRef();
			boolean vcfDataPresent = DEFAULT_CHAR != record.getVcfRef();
			
			if (illuminaDataPresent && gffDataPresent && vcfDataPresent) {
				allThree++;
				record.setPositionMatch("IGV");
			} else if (gffDataPresent && vcfDataPresent) {
				vcfANDgff++;
				record.setPositionMatch("GV");
			} else if (illuminaDataPresent && vcfDataPresent) {
				vcfANDillumina++;
				record.setPositionMatch("IV");
			} else if (illuminaDataPresent && gffDataPresent) {
				gffANDillumina++;
				record.setPositionMatch("IG");
			} else if ( gffDataPresent) {
				gff3Only++;
				record.setPositionMatch("G");
			}else if ( vcfDataPresent) {
				vcfOnly++;
				record.setPositionMatch("V");
			}else if ( illuminaDataPresent) {
				illuminaOnly++;
				record.setPositionMatch("I");
			}
			
			record.setGenotypeMatch(getGenotypeMatchInfo(record));
		}
		
		logger.info("allThree: " + allThree);
		logger.info("illuminaOnly: " + illuminaOnly);
		logger.info("gff3Only: " + gff3Only);
		logger.info("vcfANDgff: " + vcfANDgff);
		logger.info("vcfANDillumina: " + vcfANDillumina);
		logger.info("gffANDillumina: " + gffANDillumina);
		logger.info("vcfOnly: " + vcfOnly);
		
		int total = allThree + illuminaOnly + gff3Only + vcfANDgff + vcfANDillumina + gffANDillumina + vcfOnly;
		logger.info("Sum of above numbers: " + total);
		logger.info("No of records in map: " + variantMap.size());
		
	}
	
	private void displayStats2() {
		final String IGV = "IGV";
		final String IG = "IG";
		final String IV = "IV";
		final String GV = "GV";
		final String I = "I";
		final String G = "G";
		final String V = "V";
		
		int positionIGV=0, positionIG=0, positionIV=0, positionGV=0, positionI=0, positionG=0, positionV = 0;
		int pIGVgIGV=0, pIGVgIG=0, pIGVgIV=0, pIGVgGV=0; 
		int pIGgIG=0; 
		int pIVgIV=0; 
		int pGVgGV=0; 
		
		
		for (VariantRecord record : variantMap.values()) {
			
			String positionMatch = record.getPositionMatch();
			String genotypeMatch = record.getGenotypeMatch();
			
			if (IGV.equals(positionMatch)) {
				positionIGV++;
				if (IGV.equals(genotypeMatch)) pIGVgIGV++;
				else if (IG.equals(genotypeMatch)) pIGVgIG++;
				else if (IV.equals(genotypeMatch)) pIGVgIV++;
				else if (GV.equals(genotypeMatch)) pIGVgGV++;
				
			} else if (IG.equals(positionMatch)) {
				positionIG++;
				if (IG.equals(genotypeMatch)) pIGgIG++;
				
			} else if (IV.equals(positionMatch)) {
				positionIV++;
				if (IV.equals(genotypeMatch)) pIVgIV++;
				
			} else if (GV.equals(positionMatch)) {
				positionGV++;
				if (GV.equals(genotypeMatch)) pGVgGV++;
				
			} else if (I.equals(positionMatch)) positionI++;
			else if ( G.equals(positionMatch)) positionG++;
			else if ( V.equals(positionMatch)) positionV++;
		}
		
		logger.info("position IGV: " + positionIGV + ", genotype IGV: " + pIGVgIGV + ", genotype IG: " + pIGVgIG + ", genotype IV: " + pIGVgIV + ", genotype GV: " + pIGVgGV);
		logger.info("position IG: " + positionIG + ", genotype IG: " + pIGgIG);
		logger.info("position IV: " + positionIV + ", genotype IV: " + pIVgIV);
		logger.info("position GV: " + positionGV + ", genotype GV: " + pGVgGV);
		
		logger.info("position I: " + positionI);
		logger.info("position G: " + positionG);
		logger.info("position V: " + positionV);
		
		int total = positionIGV + positionIG + positionIV + positionGV + positionI + positionG + positionV;
		logger.info("Sum of above numbers: " + total);
		logger.info("No of records in map: " + variantMap.size());
		
	}

	private String getGenotypeMatchInfo(VariantRecord record) {
		Genotype illuminaGen = BaseUtils.getGenotype(record.getIllAllele1() , record.getIllAllele2());
//		String illuminaGen = record.getIlluminaRef();
		Genotype gffGen = BaseUtils.getGenotypeFromIUPACCode(record.getGffGenotype());
		Genotype vcfGen = null;
		if (DEFAULT_CHAR != record.getVcfAlt())
			vcfGen = BaseUtils.getGenotypeFromVcf(record.getVcfGenotype(), record.getVcfRef(), record.getVcfAlt());
		else
			vcfGen = BaseUtils.getGenotype(record.getVcfGenotype());
		
		String result = null;
		
		if (illuminaGen.equals( gffGen) && illuminaGen.equals(vcfGen)) result = "IGV";
		else if (illuminaGen.equals(gffGen)) result = "IG";
		else if (illuminaGen.equals(vcfGen)) result = "IV";
		else if (null != gffGen && gffGen.equals(vcfGen)) result = "GV";
//		if (doStringsMatch(illuminaGen, gffGen) && doStringsMatch(illuminaGen, vcfGen)) result = "IGV";
//		else if (doStringsMatch(illuminaGen, gffGen)) result = "IG";
//		else if (doStringsMatch(illuminaGen, vcfGen)) result = "IV";
//		else if (doStringsMatch(gffGen, vcfGen)) result = "GV";
		
		return result;
	}
	
	private boolean doStringsMatch(String a, String b) {
		return null == a ? false : a.equals(b);
	}

	private void loadDbSnpData() {
		// update records with dbsnp info
		// should be second of the input files
		String dbSNPFile = cmdLineInputFiles[3];
		DbsnpFileReader dbSNPReader = null;
		try {
			dbSNPReader = new DbsnpFileReader(new File(dbSNPFile));
		} catch (Exception e) {
			logger.error("Error caught whilst trying to instantiate DbsnpFileReader", e);
			exitStatus = -1;
		}

		int updateCount = 0;
		int noOfDbSnps = 0;
		if (null != dbSNPReader) {
			
			ChrPosition varId;
			VariantRecord varRec;
			IlluminaRecord illRec;
			int illuminaDbSnpCount = 0;
			
			for (Dbsnp130Record rec : dbSNPReader) {
				// update illumina array with dbSNP details
				illRec = illuminaMap.get(rec.getRefSnp());
				if (null != illRec) {
					if (null != illRec.getChr()) {
						logger.info("illumina rec: " + illRec.getChr() + ":" + illRec.getStart() + ":" + illRec.getSnpId() +" has already been updated - dbSNP: " + rec.getChromosome() + ":" + rec.getChromosomePosition() + ":" + rec.getRefSnp());
						// dbSNP id has more than 1 chr and position - create another IlluminaRecord in the variantMap
						//TODO deal with multiple dbSnps for same id here!!!
					} else {
						updateIlluminaRecord(illRec, rec);
					}
					illuminaDbSnpCount++;
				}
				
				varId = ChrPointPosition.valueOf(rec.getChromosome(), rec.getChromosomePosition());
				// lookup variant map to see if we have a matching record
				varRec = variantMap.get(varId);
				if (null == varRec && null != illRec && illRec.isSnp()) {
					// don't have an existing record at this position, but we want to put illumina data in here if its a snp
					varRec = new VariantRecord();
					variantMap.put(varId, varRec);
				}
				
				if (null != varRec) {
					// update required fields
					varRec.setDbSnpID(rec.getRefSnp());
					varRec.setDbSnpStrand(rec.getStrand().charAt(0));
					varRec.setDbSnpRef_Alt(rec.getRefGenome() + "__" + rec.getVariant());
					
					if (++updateCount % 100000 == 0)
						logger.info("updated " + updateCount + " variant records with dbSNP ids");
				}
				
//				dbSNPRecords.add(rec);
				if (++noOfDbSnps % 1000000 == 0)
					logger.info("hit " + noOfDbSnps + " dbSnp records");
			}
			
			logger.info("match count for dbSnp and Illumina: " + illuminaDbSnpCount);
			
			try {
				dbSNPReader.close();
			} catch (IOException e) {
				logger.error("IOException caught whilst trying to close DbsnpFileReader", e);
				exitStatus = -1;
			}
		}
		
		logger.info("No of dbSnp records: " + noOfDbSnps + " in file: " + dbSNPFile);
		logger.info("No of updated variant records: " + updateCount);
	}
	
	private void loadVCFData() {
		String vcfFile = cmdLineInputFiles[2];
		VCFFileReader reader = null;
		try {
			reader = new VCFFileReader(new File(vcfFile));
		} catch (Exception e) {
			logger.error("Error caught whilst trying to instantiate VCFFileReader", e);
			exitStatus = -1;
		}
		
		if (null != reader) {
			int vcfCount = 0;
			ChrPosition id;
			VariantRecord value;
			
			for (VcfRecord rec : reader) {
				
				id = ChrPointPosition.valueOf(rec.getChromosome(), rec.getPosition());
				
				value = variantMap.get(id);
				if (null == value) {
					value = new VariantRecord();
					variantMap.put(id, value);
				}
				value.setVcfRef(rec.getRefChar());
				value.setVcfAlt(rec.getAlt().charAt(0));
				value.setVcfGenotype(VcfUtils.getGenotypeFromGATKVCFRecord(rec));
				vcfCount++;
			}
			logger.info("there were " + vcfCount + " records in the vcf file");
			try {
				reader.close();
			} catch (IOException e) {
				logger.error("IOException caught whilst trying to close VCFFileReader", e);
				exitStatus = -1;
			}
		}
	}
	
	private void loadQSnpData() {
		String qSnpFile = cmdLineInputFiles[2];
		PileupFileReader reader = null;
		try {
			reader = new PileupFileReader(new File(qSnpFile));
		} catch (Exception e) {
			logger.error("Error caught whilst trying to instantiate PileupFileReader", e);
			exitStatus = -1;
		}
		
		if (null != reader) {
			int vcfCount = 0;
			ChrPosition id;
			VariantRecord value;
			
			for (String rec : reader) {
//				for (PileupRecord rec : reader) {
				// got some work to do here - need to split the pileup attribute to construct the object
				String [] params =  TabTokenizer.tokenize(rec);
//				String [] params =  tabbedPattern.split(rec.getPileup(), -1);
				
				// skip if the tumour genotype is null
				String genotype = params[params.length-(isNormal ? 2 : 1)];
				if (null != genotype && ! "null".equals(genotype)) {
				
					id = ChrPointPosition.valueOf(params[0], Integer.parseInt(params[1]));
					
					value = variantMap.get(id);
					if (null == value) {
						value = new VariantRecord();
						variantMap.put(id, value);
					}
					value.setVcfRef(params[2].charAt(0));
	//				value.setVcfAlt(rec.getAlt());
					value.setVcfGenotype(genotype);
					vcfCount++;
				}
			}
			logger.info("there were " + vcfCount + " records in the	qsnp file");
			try {
				reader.close();
			} catch (IOException e) {
				logger.error("IOException caught whilst trying to close PileupFileReader", e);
				exitStatus = -1;
			}
		}
	}

	private void loadGff3Data() {
		String gff3File = cmdLineInputFiles[1];
		GFF3FileReader reader = null;
		try {
			reader = new GFF3FileReader(new File(gff3File));
		} catch (Exception e) {
			logger.error("Exception caught whilst trying to instantiate GFF3FileReader", e);
			exitStatus = -1;
		}
		
		if (null != reader) {
			int gff3Count = 0;
			ChrPosition id;
			VariantRecord value;
			String chr;
			
			for (GFF3Record rec : reader) {
				// get QCMG chromosome from map
				chr = gffToQCMG.get(rec.getSeqId());
				
				id = ChrPointPosition.valueOf(chr, rec.getStart());
				
				value = variantMap.get(id);
				if (null == value) {
					value = new VariantRecord();
					variantMap.put(id, value);
				}
				String attributes = rec.getAttributes();
				char genotype = attributes.charAt(attributes.indexOf("genotype=")+9);
				char reference = attributes.charAt(attributes.indexOf("reference=")+10);
//				value.setGffAlt(genotype+"");
				value.setGffGenotype(genotype);
				value.setGffRef(reference);
				gff3Count++;
			}
			logger.info("there were " + gff3Count + " records in the gff3 file");
			try {
				reader.close();
			} catch (IOException e) {
				logger.error("IOException caught whilst trying to close GFF3FileReader", e);
				exitStatus = -1;
			}
		}
	}

	private void loadRawIlluminaData() {
		String illuminaFile = cmdLineInputFiles[0];
		
		isNormal = illuminaFile.contains("ND_");
		
		IlluminaFileReader reader = null;
		try {
			reader = new IlluminaFileReader(new File(illuminaFile));
		} catch (Exception e) {
			logger.error("Error caught whilst trying to instantiate IlluminaFileReader", e);
			exitStatus = -1;
		}
		
		if (null != reader) {
			IlluminaRecord tempRec;
			for (Record rec : reader) {
				tempRec = (IlluminaRecord) rec;
				illuminaMap.put(tempRec.getSnpId(), tempRec);
			}
			try {
				reader.close();
			} catch (IOException e) {
				logger.error("IOException caught whilst trying to close IlluminaFileReader", e);
				exitStatus = -1;
			}
		}
		logger.info("Loaded " + illuminaMap.size() + " entries into the illumina map");
	}
	
//	private void loadIlluminaData() {
//		String illuminaFile = cmdLineInputFiles[0];
//		IlluminaFileReader reader = null;
//		try {
//			reader = new IlluminaFileReader(new File(illuminaFile));
//		} catch (Exception e) {
//			logger.error("Error caught whilst trying to instantiate IlluminaFileReader", e);
//			exitStatus = -1;
//		}
//		
//		if (null != reader) {
//			VariantID id;
//			IlluminaRecord tempRec;
//			
//			for (Record rec : reader) {
//				tempRec = (IlluminaRecord) rec;
//				
//				id = new VariantID(tempRec.getChr(), tempRec.getStart());
//				
//				VariantRecord value = variantMap.get(id);
//				if (null == value) {
//					value = new VariantRecord();
//					variantMap.put(id, value);
//				}
//				value.setIlluminaSNP(tempRec.getSnp());
//			}
//			try {
//				reader.close();
//			} catch (IOException e) {
//				logger.error("IOException caught whilst trying to close IlluminaFileReader", e);
//				exitStatus = -1;
//			}
//		}
//	}

	private void convertIlluminaToVariant() {
		ChrPosition id;
		VariantRecord value;
		
		// loop through the illumina map converting all entries into the variantMap
		for (IlluminaRecord illuminaRec : illuminaMap.values()) {
			
			// TODO check this !!!
			// ignore records that did not have a dbSNP
			if (null != illuminaRec.getChr()) {
			
				id = ChrPointPosition.valueOf(illuminaRec.getChr(), illuminaRec.getStart());
				
				value = variantMap.get(id);
				if (null == value && illuminaRec.isSnp()) {
					// only want to populate our map with illumina data that does not have a corresponding gff or vcf record
					// if it contains a snp
					value = new VariantRecord();
					variantMap.put(id, value);
				}
				
				if (null != value) {
					value.setDbSnpID(illuminaRec.getSnpId());
//					value.setIlluminaAlt(illuminaRec.getRefGenomeRefSNPAllele());
					value.setIlluminaRef(illuminaRec.getSnp());
					value.setIllAllele1(illuminaRec.getFirstAllele());
					value.setIllAllele2(illuminaRec.getSecondAllele());
					value.setIllGCScore(illuminaRec.getGCScore());
					value.setIllTypeHom(illuminaRec.isHom());
				}
			}
		}
		
		// clear illuminaMap - no longer required
		illuminaMap.clear();
	}


	private void updateIlluminaRecord(IlluminaRecord illuminaRec, Dbsnp130Record dbSnpRec) {
		// standard value setting here...
		char dbSnpStrand = dbSnpRec.getStrand().charAt(0);
		illuminaRec.setChr(dbSnpRec.getChromosome());
		illuminaRec.setStart(dbSnpRec.getChromosomePosition());
//		illuminaRec.setRefGenomeRefSNPAllele(dbSnpRec.getRefGenome() + "__" + dbSnpRec.getVariant());
		
		// now gets a bit more interesting
		char strand;
		// if illumina alleles are equal to dbsnp alleles
		if (BaseUtils.areGenotypesEqual(dbSnpRec.getVariant(), illuminaRec.getSnp())) {
			strand = dbSnpStrand;
		} else strand = '+' == dbSnpStrand ? '-' : '+';
//		if (illuminaRec.getReference().charAt(1) == dbAlleles.charAt(0) && 
//				illuminaRec.getReference().charAt(3) == dbAlleles.charAt(2)) {
//			strand = dbSnpStrand;
//		} else strand = '+' == dbSnpStrand ? '-' : '+';
		
		// no longer switch the illumina snp call, but the actual allele data
//		if ('-' == strand)
//			illuminaRec.setReference(BaseUtils.getComplementFromString(illuminaRec.getReference()));
//		else
//			illuminaRec.setReference(illuminaRec.getReference().substring(1, illuminaRec.getReference().length()-1));
		if ('-' == strand) {
			illuminaRec.setFirstAllele(BaseUtils.getComplement(illuminaRec.getFirstAllele()));
			illuminaRec.setSecondAllele(BaseUtils.getComplement(illuminaRec.getSecondAllele()));
		}
		// trim illumina snp 
		illuminaRec.setSnp(illuminaRec.getSnp().substring(1, illuminaRec.getSnp().length()-1));
		
		// set snp 
		illuminaRec.setSnp(isSnp(dbSnpRec.getRefGenome(), illuminaRec.getFirstAllele(), illuminaRec.getSecondAllele()));
	}
	
	private boolean isSnp(String ref, char alleleOne, char alleleTwo) {
		if (null == ref || DEFAULT_CHAR == alleleOne || DEFAULT_CHAR == alleleTwo)
			return false;
		return ref.charAt(0) != alleleOne || ref.charAt(0) != alleleTwo;
	}
//	private boolean isSnp(String ref, String genotype) {
//		if (null == ref || null == genotype)
//			return false;
//		// assume ref is of type A
//		// assume genotype is of the form A/G
//		return ref.charAt(0) != genotype.charAt(0) || ref.charAt(0) != genotype.charAt(2); 
//	}
	
	
	private void outputVariantData() {
		FileWriter allRecordsWriter = null;
		FileWriter nonDbSnpwriter = null;
		try {
			allRecordsWriter = new FileWriter(new File(cmdLineOutputFiles[0]));	// should be the first output file supplied
			nonDbSnpwriter = new FileWriter(new File(cmdLineOutputFiles[1]));	// should be the second output file supplied
			allRecordsWriter.write("#chr\tstart\tdbSNP_id\tstrand\trg_rsa\t" +	//dbSNP
					"Ill_gc\ta1\ta2\ttype\tref\t" +	//illumina
					"gff3_ref\talt\tgen" +	//gff
					"\tvfc_ref\talt\tgen\t" +	//vcf
					"pileup\t" + 		//pileup
			"posMatch\tgenMatch\n");		//matching
			
			nonDbSnpwriter.write("#chr\tstart\tdbSNP_id\tstrand\trg_rsa\t" +	//dbSNP
					"Ill_gc\ta1\ta2\ttype\tref\t" +	//illumina
					"gff3_ref\talt\tgen" +	//gff
					"\tvfc_ref\talt\tgen\t" +	//vcf
					"pileup\n" +		//pileup
			"posMatch\tgenMatch\n");		//matching
		} catch (IOException ioe) {
			logger.error("IOException caught whilst outputting data", ioe);
		}
			
			//plonk the data into a TreeMap to bring some order to the proceedings..
			TreeMap<ChrPosition, VariantRecord> sortedVariantMap = new TreeMap<ChrPosition, VariantRecord>(variantMap);
			
			ChrPosition id;
			VariantRecord value;
//			String chr;
			
			for (Map.Entry<ChrPosition, VariantRecord> entry : sortedVariantMap.entrySet()) {
				id = entry.getKey();
				value = entry.getValue();
//				chr = ( ! id.getChromosome().startsWith("GL") ? "chr" : "") + id.getChromosome();
				
				try {
					allRecordsWriter.write(id.getChromosome() + "\t" +
							id.getStartPosition() + "\t" +
							value.formattedRecord() );
					// only want non dbSNP records
					if (null == value.getDbSnpID()) {
						nonDbSnpwriter.write(id.getChromosome() + "\t" +
								id.getStartPosition() + "\t" +
								value.formattedRecord() );
					}
				} catch (IOException e) {
					logger.error("IOException caught whilst outputting data", e);
				}
			}

			// close up
		try {
			allRecordsWriter.close();
			nonDbSnpwriter.close();
		} catch (IOException e) {
			logger.error("IOException caught whilst trying to close output files", e);
		}	
	}
	
	
	public static void main(String[] args) throws Exception {
		SnpPicker sp = new SnpPicker();
		int exitStatus = sp.setup(args);
		if (null != logger)
			logger.logFinalExecutionStats(exitStatus);
		
		System.exit(exitStatus);
	}
	
	protected int setup(String args[]) throws Exception{
		int returnStatus = -1;
		Options options = new Options(args);

		if (options.hasHelpOption()) {
			System.err.println(Messages.USAGE);
			options.displayHelp();
			returnStatus = 0;
		} else if (options.hasVersionOption()) {
			System.err.println(Messages.getVersionMessage());
			returnStatus = 0;
		} else if (options.getInputFileNames().length < 1) {
			System.err.println(Messages.USAGE);
		} else if ( ! options.hasLogOption()) {
			System.err.println(Messages.USAGE);
		} else {
			// configure logging
			logFile = options.getLogFile();
			logger = QLoggerFactory.getLogger(SnpPicker.class, logFile, options.getLogLevel());
			logger.logInitialExecutionStats("SnpPicker", SnpPicker.class.getPackage().getImplementationVersion());
			
			// get list of file names
			cmdLineInputFiles = options.getInputFileNames();
			if (cmdLineInputFiles.length < 1) {
				throw new QMuleException("INSUFFICIENT_ARGUMENTS");
			} else {
				// loop through supplied files - check they can be read
				for (int i = 0 ; i < cmdLineInputFiles.length ; i++ ) {
					if ( ! FileUtils.canFileBeRead(cmdLineInputFiles[i])) {
						throw new QMuleException("INPUT_FILE_READ_ERROR" , cmdLineInputFiles[i]);
					}
				}
			}
			
			// check supplied output files can be written to
			if (null != options.getOutputFileNames()) {
				cmdLineOutputFiles = options.getOutputFileNames();
				for (String outputFile : cmdLineOutputFiles) {
					if ( ! FileUtils.canFileBeWrittenTo(outputFile))
						throw new QMuleException("OUTPUT_FILE_WRITE_ERROR", outputFile);
				}
			}
			
			return engage();
		}
		return returnStatus;
	}
}
