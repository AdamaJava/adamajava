/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.maf.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.qcmg.common.dcc.DccConsequence;
import org.qcmg.common.dcc.MutationType;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.maf.MAFRecord;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.model.MafType;
import org.qcmg.common.model.PileupElement;
import org.qcmg.common.model.ReferenceNameComparator;
import org.qcmg.common.model.TorrentVerificationStatus;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.FileUtils;
import org.qcmg.common.util.SnpUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.maf.QMafException;
import org.qcmg.picard.util.PileupElementUtil;
import org.qcmg.picard.util.QDccMetaFactory;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedHeader;
import org.qcmg.tab.TabbedRecord;

public class MafUtils {
	private static final ReferenceNameComparator chrComp = new ReferenceNameComparator();
	
//	public static final String INS = "INS";
//	public static final String DEL = "DEL";
//	public static final String SNP = "SNP";
//	public static final String ONP = "ONP";
	
	public static final char NL = '\n';
	public static final char HASH = '#';
	
	private static final QLogger logger = QLoggerFactory.getLogger(MafUtils.class);
	
	public static final String CORE_HEADER = "#version 2.2\n" +
		"Hugo_Symbol\tEntrez_Gene_Id\tCenter\tNCBI_Build\tChromosome\tStart_Position\tEnd_Position\tStrand\tVariant_Classification\t" +
		"Variant_Type\tReference_Allele\tTumor_Seq_Allele1\tTumor_Seq_Allele2\tdbSNP_RS\tdbSNP_Val_Status\tTumor_Sample_Barcode\tMatched_Norm_Sample_Barcode\t" +
		"Match_Norm_Seq_Allele1\tMatch_Norm_Seq_Allele2\tTumor_Validation_Allele1\tTumor_Validation_Allele2\tMatch_Norm_Validation_Allele1\t" +
		"Match_Norm_Validation_Allele2\tVerification_Status\tValidation_Status\tMutation_Status\tSequencing_Phase\tSequence_Source\t" +
		"Validation_Method\tScore\tBAM_File\tSequencer";
	
	// QCMG specific fields follow..
	public static final String QCMG_FIELDS = "\tQCMG_Flag\tND\tTD\tCanonical_Transcript_Id\tCanonical_AA_Change\tCanonical_Base_Change\t" +
			"Alternate_Transcript_Id\tAlternate_AA_Change\tAlternate_Base_Change";

	public static final String HEADER = CORE_HEADER + QCMG_FIELDS + "\n";
	public static final String HEADER_NS = CORE_HEADER + QCMG_FIELDS + "\tNovel_Starts\n";
	public static final String  HEADER_WITH_CONFIDENCE = CORE_HEADER + QCMG_FIELDS + "\tConfidence\n";
	public static final String HEADER_WITH_CPG = CORE_HEADER + QCMG_FIELDS + "\tCPG\n";
	public static final String HEADER_WITH_CONFIDENCE_CPG = CORE_HEADER + QCMG_FIELDS + "\tConfidence\tCPG\tNovel_Starts\n";
	public static final String HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS = CORE_HEADER + QCMG_FIELDS + "\tConfidence\tCPG\tGff3_Bait\tNovel_Starts\n";
	public static final String HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS_COSMIC = HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS.substring(0, HEADER_WITH_CONFIDENCE_CPG_BAITS_NNS.length() -1) 
			+ "\tCosmic_id\tCosmic_id_freq\tCosmic_freq\tCosmic_gene\n";
	public static final String NON_QCMG_HEADER_WITH_CPG = CORE_HEADER + "\tCPG\n";
	
	public static final int HEADER_WITH_CONFIDENCE_COLUMN_COUNT =  TabTokenizer.tokenize(HEADER_WITH_CONFIDENCE).length;
	
	
	public static TabbedRecord addColumn(TabbedRecord tabbedRec, String data) {
		tabbedRec.setData(tabbedRec.getData() + "\t" + data);
		return tabbedRec;
	}
	
	public static void loadEntrezMapping(String fileName, Map<String, Set<Integer>> ensemblToEntrez) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(fileName));
		try {
			int count = 0;
			for (TabbedRecord rec : reader) {
				
				// header line
				if (count++ == 0) continue;
				
				String[] params = TabTokenizer.tokenize(rec.getData());
				// ensemble id is column 2, entrez id is column 3
				// need to deal with one to many mappings - keep them all
//				String ensembl = params[1];
//				String entrez = params[2];
				String ensembl = params[1];
				String entrez = params[5];	// now being taken from larger file
				
				if (StringUtils.isNullOrEmpty(entrez)) continue;
				
				if ("NULL".equals(entrez)) continue;
				
				Set<Integer> existingEntrez = ensemblToEntrez.get(ensembl);
				if (null == existingEntrez) {
					existingEntrez = new HashSet<Integer>();
					ensemblToEntrez.put(ensembl, existingEntrez);
				}
				existingEntrez.add(Integer.parseInt(entrez));
			}
			
		} finally {
			reader.close();
		}
	}
	
	public static void loadCanonicalTranscriptMapping(String fileName, Map<String, String> ensemblGeneToCanonicalTranscript) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(fileName));
		try {
			int count = 0;
			for (TabbedRecord rec : reader) {
				
				if (++count == 1) continue;	// header line
				
				String[] params = TabTokenizer.tokenize(rec.getData());
				// ensemble gene id is column 1, canonical transcript id is column 2
				// need to deal with one to many mappings - keep them all
				String geneId = params[0];
				String canonicalTranscriptId = params[1];
				
				if (StringUtils.isNullOrEmpty(canonicalTranscriptId)) continue;
				
				String existingCanonicalTransId = ensemblGeneToCanonicalTranscript.get(geneId);
				if (null == existingCanonicalTransId) {
					ensemblGeneToCanonicalTranscript.put(geneId, canonicalTranscriptId);
				} else if ( ! existingCanonicalTransId.equals(canonicalTranscriptId)) {
					logger.info("Got more than 1 canonical transcript for gene id:" + geneId);
					// add new entrez id to the existing one - pipe delimited
					ensemblGeneToCanonicalTranscript.put(geneId, existingCanonicalTransId + "|" + canonicalTranscriptId);
				}
			}
			
		} finally {
			reader.close();
		}
	}
	
	public static void getVerifiedData(String fileName, String patientId, Map<String, Map<ChrPosition, TorrentVerificationStatus>> verifiedData) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(fileName));
		try {
			int verifiedYes = 0, verifiedNo = 0;
			for (TabbedRecord rec : reader) {
				
				// seem to be some blank lines at the top of the verification data file
				// also skip past header line
				if (StringUtils.isNullOrEmpty(rec.getData()) || rec.getData().startsWith("PatientID")) continue;
				
				String[] params = TabTokenizer.tokenize(rec.getData());
				
				String patientFromFile = params[0];
				
				// same patient?
				if (null != patientId &&  ! patientId.equalsIgnoreCase(patientFromFile)) continue;
				
				Map<ChrPosition, TorrentVerificationStatus> patientSpecificVerifiedData = verifiedData.get(patientFromFile);
				if (null == patientSpecificVerifiedData) {
					patientSpecificVerifiedData = new HashMap<ChrPosition, TorrentVerificationStatus>();
					verifiedData.put(patientFromFile, patientSpecificVerifiedData);
				}
				
				ChrPosition position = StringUtils.getChrPositionFromString(params[2]);
				String verification = params[57];
				TorrentVerificationStatus tvs = TorrentVerificationStatus.getVerificationStatus(verification.trim());
				
				if (tvs.verified()) verifiedYes++;
				else verifiedNo++;
				
				patientSpecificVerifiedData.put(position, tvs);
			}
			logger.info("loaded verification data - verified yes: " + verifiedYes + ", verified no: " + verifiedNo);
		} finally {
			reader.close();
		}
	}
	
	public static void getDbSNPValDetails(String fileName, List<MAFRecord> mafs) throws Exception {
		// create a map of dbSnpId and maf record
		final Map<String, MAFRecord> snpIdMap = new HashMap<String, MAFRecord>(mafs.size());
		for (MAFRecord maf : mafs) {
			if ( ! maf.getDbSnpId().isEmpty())
				snpIdMap.put(maf.getDbSnpId(), maf);
		}
		
		TabbedFileReader reader = new TabbedFileReader(new File(fileName));
		try {
			
			for (TabbedRecord rec : reader) {
				String[] params = TabTokenizer.tokenize(rec.getData());
				// dbSnp id is column 5, val details is column 12
				// need to deal with one to many mappings - keep them all
				String dbSnpId = params[4];
				String snpVal = params[11];

				MAFRecord maf = snpIdMap.get(dbSnpId);
				if (null != maf) {
					// update the db snp val field
					if (StringUtils.isNullOrEmpty(maf.getDbSnpValStatus())) {
						maf.setDbSnpValStatus(snpVal);
					} else {
						maf.setDbSnpValStatus(maf.getDbSnpValStatus() + ";" + snpVal);
					}
				}
			}
		} finally {
			reader.close();
		}
	}
	
	/**
	 * Writes the maf records contained in the suppled list to file, along with the suppled header
	 * 
	 * @param fileName
	 * @param mafs
	 * @param header
	 * @throws IOException
	 */
	public static void writeMafOutput(String fileName, List<MAFRecord> mafs, String header, boolean includeExtraFields) throws IOException {
		File outputFile = new File(fileName);
		if ( ! FileUtils.canFileBeWrittenTo(outputFile))
			throw new IllegalArgumentException("Can't write to output file: " + fileName);
		
		// log some stats
		StringBuilder statsSB = new StringBuilder();
		List<String> variants = MafStatsUtils.getVariantClassifications(mafs);
		for (String s : variants) {
			logger.info(outputFile.getName() + MafStatsUtils.SEPARATOR + s);
			statsSB.append(HASH).append(s).append(NL);
		}
		List<String> mutations = MafStatsUtils.getMutationsAndTiTv(mafs);
		for (String s : mutations) {
			logger.info(outputFile.getName() + MafStatsUtils.SEPARATOR + s);
			statsSB.append(HASH).append(s).append(NL);
		}
		String rsRatio = MafStatsUtils.getRsRatio(mafs);
		logger.info(outputFile.getName() + MafStatsUtils.SEPARATOR + rsRatio);
		statsSB.append(HASH).append(rsRatio).append(NL);
		
		// sort the list by chromosome and then position
		Collections.sort(mafs, new Comparator<MAFRecord>(){
			@Override
			public int compare(MAFRecord o1, MAFRecord o2) {
				if (null != o1.getChromosome() && null != o2.getChromosome()) {
					int chrDiff = chrComp.compare(o1.getChromosome(), o2.getChromosome());
					if (0 != chrDiff) return chrDiff;
					return o1.getStartPosition() - o2.getStartPosition();
				} else return 0;
			}});
		
		// split header up so that stats can be inserted between meta info and maf header info
		String header1 = header.substring(0, header.indexOf(CORE_HEADER));
		String header2 = header.substring(header.indexOf(CORE_HEADER));
		
		try (FileWriter writer = new FileWriter(outputFile)) {
			// make sure header line has a new line at the end
			if ( ! header2.endsWith("\n")) header2 += "\n";
			
			writer.write(header1);
			writer.write(statsSB.toString());		// add stats to header
			writer.write(header2);
			
			for (MAFRecord record : mafs) {
				if (includeExtraFields) {
//					writer.write(record.toFormattedStringExtraFieldsCosmic() + "\n");
					writer.write(record.toFormattedStringExtraFields() + "\n");
				} else { 
					writer.write(record.toFormattedString() + "\n");
				}
			}
		}
	}
	public static void writeMafOutput(String fileName, List<MAFRecord> mafs, String header) throws IOException {
		writeMafOutput(fileName,mafs, header, false);
	}


	public static void setupStaticMafFields(MAFRecord maf,  String patientId, String controlSampleID, 
			String tumourSampleID, boolean somatic) {
		
		maf.setCenter("qcmg.uq.edu.au");
		maf.setNcbiBuild(37);
		maf.setMutationStatus(somatic ? "Somatic" : "Germline");
		maf.setSequencer("Unknown");
		maf.setSequencingSource("Unknown");
		maf.setTumourSampleBarcode("QCMG-66-" + patientId + "-" + tumourSampleID);
		maf.setNormalSampleBarcode("QCMG-66-" + patientId + "-" + controlSampleID);
		maf.setPatient(patientId);
	}
	
	public static String getEntrezId(String ensembleId, Map<String, Set<Integer>>ensemblToEntrez ) {
		
		String resultString = "";
		if (null != ensembleId && ensembleId.contains("|")) {
			String[] params = ensembleId.split("|");
			for (String param : params) {
				Set<Integer> entrezIds = ensemblToEntrez.get(param);
				if (null != entrezIds ) {
					for (Integer id : entrezIds) {
						resultString += (resultString.length() > 0 ? "|" : "") + id.intValue();
					}
				}
			}
		} else {
			Set<Integer> entrezIds = ensemblToEntrez.get(ensembleId);
			if (null != entrezIds) {
				for (Integer id : entrezIds) {
					resultString += (resultString.length() > 0 ? "|" : "") + id.intValue();
				}
			}
		}
		return resultString.length() > 0 ? resultString : "0";
	}
	
	public static String getHugoSymbol(String gene) {
		if (null == gene) throw new IllegalArgumentException("Null value passed to getHugoSymbol");
		
		return gene.replace("-888", "Unknown");
	}
	
	public static String getDbSnpId(String id) {
		if (StringUtils.isNullOrEmpty(id) || "-888".equals(id) || "-999".equals(id)) return "novel";
		return id;
	}
	
	public static void loadDCCFile(File fileName, String patientId, Map<ChrPosition, TorrentVerificationStatus> verifiedData, List<MAFRecord> mafs, Map<String, Set<Integer>> ensemblToEntrez, MutationType type) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(fileName);
		TabbedHeader header = reader.getHeader();
		
		// should be able to glean some useful info from the header
//		String patientIdFromFile = null;
		String controlSampleID = null;
		String tumourSampleID = null;
		String tool = null;
//		DccType type = null;
		
		for (String headerLine : header) {
//			if (headerLine.startsWith("#PatientID"))
//				patientIdFromFile = headerLine.substring(headerLine.indexOf(':') +2);
			if (headerLine.startsWith("#ControlSampleID"))
				controlSampleID = headerLine.substring(headerLine.indexOf(':') +2);
			if (headerLine.startsWith("#TumourSampleID"))
				tumourSampleID = headerLine.substring(headerLine.indexOf(':') +2);
			if (headerLine.startsWith("#Tool")) {
				tool = headerLine.substring(headerLine.indexOf(':') +2);
//				type = headerLine.endsWith("SNP") || headerLine.endsWith("GATK")  ? DccType.SNP : (headerLine.endsWith("small_indel_tool") ? DccType.INDEL : null);
			}
		}
		
		// default to snp if not in header
//		if (null == type) type = DccType.SNP;
		
		logger.info("patient: " + patientId + ", controlSampleID: "  + controlSampleID + ", tumourSampleID: " + tumourSampleID + ", tool: " + tool);
		
		try {
			int count = 0;
			boolean containsNS = false;
			boolean containsEA = false;
			boolean isGermline = false;
			for (TabbedRecord rec : reader) {
				if (++count == 1) {		// header line
					// If we have a NS column header, then set containsNS to true
					if (rec.getData().contains("NNS"))
						containsNS = true;
					if (rec.getData().contains("expressed_allele"))
						containsEA = true;
					if (rec.getData().contains("variation_id"))
						isGermline = true;
					continue;
				}
				
				if (isGermline) {
					convertGermlineDccToMaf(rec, patientId, controlSampleID, tumourSampleID, verifiedData, mafs, ensemblToEntrez);
				} else {
					convertDccToMaf(rec, patientId, controlSampleID, tumourSampleID, verifiedData, mafs, ensemblToEntrez, containsNS, containsEA);
				}
			}
			logger.info("count " + count + " dcc records");
		} finally {
			reader.close();
		}
	}
	
	public static void updateFlag(MAFRecord maf, String annotation) {
		if (null == maf) throw new IllegalArgumentException("Null maf record passed to updateFlag()");
		
		String existingFlag = maf.getFlag();
		if (SnpUtils.PASS.equals(existingFlag) || SnpUtils.PASS.equals(annotation) || StringUtils.isNullOrEmpty(existingFlag)) {
			maf.setFlag(annotation);
		} else {
			// append to flag
			if ( ! existingFlag.contains(annotation)) {
				maf.setFlag(existingFlag + ";" + annotation);
			}
		}
	}
	
	public static void loadDCCFile(String dccqFile, Map<ChrPosition, TorrentVerificationStatus> verifiedData,
			List<MAFRecord> mafs, Map<String, Set<Integer>> ensemblToEntrez, MafType mafType) throws IOException, Exception {
		
		
		try (TabbedFileReader reader = new TabbedFileReader(new File(dccqFile));) {
			TabbedHeader header = reader.getHeader();
			
			QDccMeta dccMeta = QDccMetaFactory.getDccMeta(header);
			
			String controlSampleId = dccMeta.getMatchedSampleId().getValue();
			String testSampleId = dccMeta.getAnalyzedSampleId().getValue();
			String donor = dccMeta.getDonorId().getValue();
			
			int count = 0;
			boolean containsNS = false;
			boolean containsEA = false;
			for (TabbedRecord rec : reader) {
				if (++count == 1) {		// header line
					// If we have a NS column header, then set containsNS to true
					if (rec.getData().contains("NNS"))
						containsNS = true;
					if (rec.getData().contains("expressed_allele"))
						containsEA = true;
					continue;
				}
				
				if (mafType.isGermline()) {
					convertGermlineDccToMaf(rec, donor, controlSampleId, testSampleId, verifiedData, mafs, ensemblToEntrez);
				} else {
					convertDccToMaf(rec, donor, controlSampleId, testSampleId, verifiedData, mafs, ensemblToEntrez, containsNS, containsEA);
				}
			}
			logger.info("count " + count + " dcc records");
		}
	}
	
	
	public static void convertDccToMaf(TabbedRecord tabbedRecord, String patientId, String controlSampleID, 
			String tumourSampleID, Map<ChrPosition, TorrentVerificationStatus> verifiedData, List<MAFRecord> mafs,
			Map<String, Set<Integer>> ensemblToEntrez) throws QMafException {
		convertDccToMaf(tabbedRecord, patientId, controlSampleID, tumourSampleID, verifiedData, mafs, ensemblToEntrez, false, false);
	}
	
	public static void convertDccToMaf(final TabbedRecord tabbedRecord, final String patientId, final String controlSampleID, 
			final String tumourSampleID, final Map<ChrPosition, TorrentVerificationStatus> verifiedData, final List<MAFRecord> mafs,
			final Map<String, Set<Integer>> ensemblToEntrez, final boolean containsNS, final boolean containsEA) throws QMafException {
		
		// novel starts position in file is 23 (1-based)
		// expressed_allele position in file is 13 (1-based)
		
		// anything over this position needs to check containsNS variable
		
		int offset = containsNS && containsEA ? 2 : (containsNS || containsEA ? 1 : 0);
		
		
		final int flagPosition = 35 + offset;
		final int flankingSequencePosition = 36 + offset;
		
		String[] params = TabTokenizer.tokenize(tabbedRecord.getData());
		
		// check if position verified
		String chromosome = params[2];
		int startPosition = Integer.parseInt(params[3]);
		int endPosition = Integer.parseInt(params[4]);
		TorrentVerificationStatus tvs = (null == verifiedData) ? null 
				: verifiedData.get(new ChrRangePosition("chr"+chromosome, startPosition, endPosition));
		
		// setup maf record with static and common fields
		MAFRecord maf = new MAFRecord();
		MafUtils.setupStaticMafFields(maf, patientId, controlSampleID, tumourSampleID, true);
		
		if (containsNS) {
			String nsString = params[containsEA ? 23 : 22];
			if ( ! StringUtils.isNullOrEmpty(nsString)) {
				// indels don't populate this field
				if ( ! nsString.contains("--"))
					maf.setNovelStartCount(Integer.parseInt(nsString));
			}
		}
		
		// get type
		int typeInt = Integer.parseInt(params[1]);
		
		MutationType type = null; 
		switch (typeInt) {
		case 1: type = MutationType.SNP; break;
		case 2: type = MutationType.INS; break;
		case 3: type = MutationType.DEL; break;
		case 4: 
			int polymorphismRange = (endPosition - startPosition) + 1;
			type = polymorphismRange == 2 ? MutationType.DNP : 
						polymorphismRange == 3 ? MutationType.TNP : MutationType.ONP;
			break;
		default: throw new QMafException("Unknown dcc type id: " + typeInt);
		}
		maf.setVariantType(type);
		
		
		// use M rather than MT
		// strip out "chr"
		int chrIndex = chromosome.indexOf("chr");
		String chr = (chrIndex > -1) ? chromosome.substring(chrIndex + 3) : chromosome;
		if ("MT".equals(chr)) chr = "M";
		
		maf.setChromosome(chr);
		maf.setStartPosition(startPosition);
		maf.setEndPosition(endPosition);
		maf.setStrand(Integer.parseInt(params[5]) == 1 ? '+' : '-');	// set this according to 1 or 0
		maf.setRef(params[8]);
		maf.setTumourAllele1(getAllele(true, params[10]));
		maf.setTumourAllele2(getAllele(false, params[10]));
//		maf.setTumourAllele1(getAllele(true, type, params[10]));
//		maf.setTumourAllele2(getAllele(false, type, params[10]));
		maf.setDbSnpId(MafUtils.getDbSnpId(params[containsEA ? 19 : 18]));
		
		if (null != tvs) {
			maf.setValidationStatus(tvs.getMafDisplayName());
		} else  {
			maf.setValidationStatus("Unknown");
		}
		
		// qcmg specific
		maf.setFlag(params[flagPosition]);		// QCMGFlag field
		maf.setNd(params[containsEA ? 21 : 20]);		// ND field
		maf.setTd(params[containsEA ? 22 : 21]);		// TD field
		maf.setCpg(params[flankingSequencePosition]);		// FlankSeq field
		
		// normal doesn't always exist for somatic...
		if ("--".equals(params[9]) || "-/-".equals(params[9])) {
			maf.setNormalAllele1("-");
			maf.setNormalAllele2("-");
		} else {
			maf.setNormalAllele1(getAllele(true, params[9]));
			maf.setNormalAllele2(getAllele(false, params[9]));
//			maf.setNormalAllele1(getAllele(true, type, params[9]));
//			maf.setNormalAllele2(getAllele(false, type, params[9]));
		}
		
		
//		if (canonicalMafMode) {
//			canonicalTranscript(type, params, genes, geneIds, transcriptIds, maf);
//		} else {
//			worstCaseTranscript(type, params, genes, geneIds, transcriptIds, maf, ensemblToEntrez, offset);
			
			worstCaseConsequence(type, params, maf, ensemblToEntrez, offset);
			
		
		// need to check that there is a valid gene set on the Maf object
		// if not - don't add to collection
		
		if (null != maf.getHugoSymbol())
			mafs.add(maf);
	}
	
	public static void convertGermlineDccToMaf(final TabbedRecord tabbedRecord, final String patientId, final String controlSampleID, 
			final String tumourSampleID, final Map<ChrPosition, TorrentVerificationStatus> verifiedData, final List<MAFRecord> mafs,
			final Map<String, Set<Integer>> ensemblToEntrez) throws QMafException {
		
		// novel starts position in file is 23 (1-based)
		// expressed_allele position in file is 13 (1-based)
		
		// anything over this position needs to check containsNS variable
		
		int offset = 1;
//		int offset = containsNS && containsEA ? 2 : (containsNS || containsEA ? 1 : 0);
		
		
		final int flagPosition = 35 + offset;
		final int flankingSequencePosition = 36 + offset;
		
		String[] params = TabTokenizer.tokenize(tabbedRecord.getData());
		
		// check if position verified
		String chromosome = params[2];
		int startPosition = Integer.parseInt(params[3]);
		int endPosition = Integer.parseInt(params[4]);
		TorrentVerificationStatus tvs = (null == verifiedData) ? null 
				: verifiedData.get(new ChrRangePosition("chr"+chromosome, startPosition, endPosition));
		
		// setup maf record with static and common fields
		MAFRecord maf = new MAFRecord();
		MafUtils.setupStaticMafFields(maf, patientId, controlSampleID, tumourSampleID, false);
		
		String nsString = params[22];
		if ( ! StringUtils.isNullOrEmpty(nsString)) {
			// indels don't populate this field
			if ( ! nsString.contains("--"))
				maf.setNovelStartCount(Integer.parseInt(nsString));
		}
		
		// get type
		int typeInt = Integer.parseInt(params[1]);
		
		MutationType type = null; 
		switch (typeInt) {
		case 1: type = MutationType.SNP; break;
		case 2: type = MutationType.INS; break;
		case 3: type = MutationType.DEL;  break;
		case 4: 	
			int polymorphismRange = (endPosition - startPosition) + 1;
			type = polymorphismRange == 2 ? MutationType.DNP : 
			polymorphismRange == 3 ? MutationType.TNP : MutationType.ONP;
			break;
		default: throw new QMafException("Unknown dcc type id: " + typeInt);
		}
		maf.setVariantType(type);
		
		
		// use M rather than MT
		// strip out "chr"
		int chrIndex = chromosome.indexOf("chr");
		String chr = (chrIndex > -1) ? chromosome.substring(chrIndex + 3) : chromosome;
		if ("MT".equals(chr)) chr = "M";
		
		maf.setChromosome(chr);
		maf.setStartPosition(startPosition);
		maf.setEndPosition(endPosition);
		maf.setStrand(Integer.parseInt(params[5]) == 1 ? '+' : '-');	// set this according to 1 or 0
		maf.setRef(params[8]);
		// tumour doesn't always exist for germline...
		if ("--".equals(params[10]) || "-/-".equals(params[10])) {
			maf.setTumourAllele1("-");
			maf.setTumourAllele2("-");
		} else {
			maf.setTumourAllele1(getAllele(true, params[10]));
			maf.setTumourAllele2(getAllele(false, params[10]));
//			maf.setTumourAllele1(getAllele(true, type, params[10]));
//			maf.setTumourAllele2(getAllele(false, type, params[10]));
		}
		maf.setDbSnpId(MafUtils.getDbSnpId(params[18]));
		
		if (null != tvs) {
			maf.setValidationStatus(tvs.getMafDisplayName());
		} else  {
			maf.setValidationStatus("Unknown");
		}
		
		// qcmg specific
		maf.setFlag(params[flagPosition]);		// QCMGFlag field
		maf.setNd(params[ 20 ]);		// ND field
		maf.setTd(params[ 21 ]);		// TD field
		maf.setCpg(params[flankingSequencePosition]);		// FlankSeq field
		
		// normal doesn't always exist for somatic...
		if ("--".equals(params[9]) || "-/-".equals(params[9])) {
			maf.setNormalAllele1("-");
			maf.setNormalAllele2("-");
		} else {
			maf.setNormalAllele1(getAllele(true, params[9]));
			maf.setNormalAllele2(getAllele(false, params[9]));
//			maf.setNormalAllele1(getAllele(true, type, params[9]));
//			maf.setNormalAllele2(getAllele(false, type, params[9]));
		}
		
		
//		if (canonicalMafMode) {
//			canonicalTranscript(type, params, genes, geneIds, transcriptIds, maf);
//		} else {
//			worstCaseTranscript(type, params, genes, geneIds, transcriptIds, maf, ensemblToEntrez, offset);
		
		worstCaseConsequence(type, params, maf, ensemblToEntrez, offset);
		
		
		// need to check that there is a valid gene set on the Maf object
		// if not - don't add to collection
		
		if (null != maf.getHugoSymbol())
			mafs.add(maf);
	}
	
	public static String getAllele(boolean firstAllele, String bases) throws QMafException {
//		public static String getAllele(boolean firstAllele, MutationType type, String bases) throws QMafException {
		int index = bases.indexOf('/');
		if (index >= 0)
			return firstAllele ? bases.substring(0, index) : bases.substring(index + 1);
		
		index = bases.indexOf('>');
		if (index >= 0)
			return firstAllele ? bases.substring(0, index) : bases.substring(index + 1);
			
		if ("-999".equals(bases)) 
			return "-";
		return bases;
	}
	
	
	public static void worstCaseTranscript(MutationType type, String[] params, String[] genes, String[] geneIds, 
			String[] transcriptIds, MAFRecord maf, Map<String, Set<Integer>> ensemblToEntrez, int offset) {
		
		final int allTranscriptsPosition = 27 + offset;
		final int consequencesPosition = 22 + offset;
		final int aaChangesPosition = 23 + offset;
		final int baseChangesPosition = 24 + offset;
		
		walkThroughGenes(type, params, genes, geneIds, transcriptIds, maf,
				ensemblToEntrez, allTranscriptsPosition,
				consequencesPosition, aaChangesPosition, baseChangesPosition);
		
		if (null == maf.getHugoSymbol()) {
			// didn't get a worst case gene - put in Unknown, as we want to keep them all 
			maf.addHugoSymbol("Unknown");
		}
	}
	
	static List<GenePositions> getGenePositions(String[] genes) {
		if (null == genes)
			throw new IllegalArgumentException("Null gene array passed to getGenePositions");
		
		List<GenePositions> genePositions = new ArrayList<GenePositions>();
		GenePositions currentGene = null;
		
		for (int i = 0 ; i < genes.length ; i++) {
			String gene = genes[i];
			
			if (i == 0) {
				currentGene = new GenePositions(gene, 0);
			} else {
				if (gene.equals(currentGene.getGene())) {
					currentGene.addPosition(i);
				} else {
					genePositions.add(currentGene);
					currentGene = new GenePositions(gene, i);
				}
			}
		}
		if (null != currentGene)
			genePositions.add(currentGene);
		
		return genePositions;
	}

	private static void walkThroughGenes(MutationType type, String[] params,
			String[] genes, String[] geneIds, String[] transcriptIds,
			MAFRecord maf, Map<String, Set<Integer>> ensemblToEntrez,
			final int allTranscriptsPosition, final int consequencesPosition,
			final int aaChangesPosition, final int baseChangesPosition) {
		
		// setup collection of GenePositions
		List<GenePositions> genePositions = getGenePositions(genes);
		
//		String [] allTranscripts = params[allTranscriptsPosition].split("[,|]");
		String [] consequences = params[consequencesPosition].split(",");
//		int i = 0;
		for (GenePositions genePosition : genePositions) {
			
			List<String> geneSpecificTranscriptIds = StringUtils.getChildArrayFromParentArray(transcriptIds, genePosition.getPositions());
//			String[] geneSpecificTranscriptIds =  transcriptIds[i].split(",");
			String geneId = geneIds[genePosition.getPositions()[0]];
			
			// need start and stop positions of transcripts belonging to this gene so that the relevant consequences can be retrieved
//			int startPosition = StringUtils.getPositionOfStringInArray(allTranscripts, geneSpecificTranscriptIds[0], true);
//			int endPosition = StringUtils.getPositionOfStringInArray(allTranscripts, geneSpecificTranscriptIds[geneSpecificTranscriptIds.length -1], true);
			
			
			String [] geneConsequences = new String[genePosition.getPositions().length];
			
//			logger.info("consequences: " + Arrays.deepToString(consequences));
//			logger.info("startPosition: " + startPosition + ", endPosition: " + endPosition);
			int j = 0;
			for (int position : genePosition.getPositions()) {
				geneConsequences[j++] = consequences[position];
			}
			
			String worstCaseConsequence = DccConsequence.getWorstCaseConsequence(type, geneConsequences);
			String dccConseq = DccConsequence.getMafName(worstCaseConsequence, type, Integer.parseInt(params[1]));
			if ( ! DccConsequence.passesMafNameFilter(dccConseq)) {
				logger.debug("Would normally skip this dccConseq: " + dccConseq + ", geneConsequences[] " + Arrays.deepToString(geneConsequences));
				continue;
			}
			
			maf.addVariantClassification(dccConseq);
			
			int currentPosition = 0;
			for (String c : geneConsequences) {
				if (c.equals(worstCaseConsequence)) break;
				currentPosition++;
			}
			
			if (currentPosition >= geneSpecificTranscriptIds.size()) {
				currentPosition = geneSpecificTranscriptIds.size() -1;
			}
			
			String [] aaChanges = params[aaChangesPosition].split(",");
			String [] baseChanges = params[baseChangesPosition].split(",");
			String worstCaseTranscriptId = geneSpecificTranscriptIds.get(currentPosition);
			maf.addCanonicalTranscriptId(worstCaseTranscriptId);
			maf.addCanonicalAAChange(aaChanges[currentPosition]);
			maf.addCanonicalBaseChange(baseChanges[currentPosition]);
			maf.addEntrezGeneId(getEntrezId(geneId, ensemblToEntrez));
			maf.addHugoSymbol("Unknown".equals(genePosition.getGene()) ? MafUtils.getHugoSymbol(geneId) : genePosition.getGene());
		}
	}
	
//	static void walkThroughConsequences(DccType type, String[] params,
//			String[] genes, String[] geneIds, String[] transcriptIds,
//			MAFRecord maf, Map<String, Set<Integer>> ensemblToEntrez,
//			final int allTranscriptsPosition, final int consequencesPosition,
//			final int aaChangesPosition, final int baseChangesPosition) {
//		
//		String [] consequences = params[consequencesPosition].split(",");
//		String worstCaseConsequence = DccConsequence.getWorstCaseConsequence(type, consequences);
//		
//		if (null == worstCaseConsequence) {
//			logger.debug("No worst case consequence found in: " + Arrays.deepToString(consequences));
//		} else {
//			logger.debug("Worst case consequence found: " + worstCaseConsequence);
//			String dccConseq = DccConsequence.getMafName(worstCaseConsequence, type, Integer.parseInt(params[1]));
//			if (DccConsequence.passesMafNameFilter(dccConseq)) {
//				logger.debug("dcc consequence found: " + dccConseq);
//				// need to get corresponding gene and transcript id
//				int arrayPosition = StringUtils.getPositionOfStringInArray(consequences, worstCaseConsequence, false);
//				String gene = (arrayPosition > -1 && arrayPosition < genes.length) ? genes[arrayPosition] : null;
//				String geneId = (arrayPosition > -1 && arrayPosition < geneIds.length) ? geneIds[arrayPosition] : null;
//				String transcriptId = (arrayPosition > -1 && arrayPosition < transcriptIds.length) ? transcriptIds[arrayPosition] : null;
//				
//				String [] aaChanges = params[aaChangesPosition].split(",");
//				String [] baseChanges = params[baseChangesPosition].split(",");
//				
//				String aaChange = (arrayPosition > -1 && arrayPosition < aaChanges.length) ? aaChanges[arrayPosition] : null;
//				String baseChange = (arrayPosition > -1 && arrayPosition < baseChanges.length) ? baseChanges[arrayPosition] : null;
//				
//				
//				maf.addVariantClassification(dccConseq);
//				
//				maf.addCanonicalTranscriptId(transcriptId);
//				maf.addCanonicalAAChange(aaChange);
//				maf.addCanonicalBaseChange(baseChange);
//				maf.addEntrezGeneId(getEntrezId(geneId, ensemblToEntrez));
//				maf.addHugoSymbol("Unknown".equals(gene) ? MafUtils.getHugoSymbol(geneId) : gene);
//				
//			} else {
//				logger.debug("Skipping this dccConseq: " + dccConseq + ", consequences[] " + Arrays.deepToString(consequences));
//			}
//		}
//	}
	
	public static void worstCaseConsequence(MutationType type, String[] params,
			MAFRecord maf, Map<String, Set<Integer>> ensemblToEntrez, int offset) {
		
//		final int allTranscriptsPosition = 27 + offset;
		final int consequencesPosition = 22 + offset;
		final int aaChangesPosition = 23 + offset;
		final int baseChangesPosition = 24 + offset;
		final int genesPosition = 30 + offset;
		final int genesIdsPosition = 26 + offset;
		final int transcriptIdsPosition = 27 + offset;
		
		// get worst case consequence
		String [] consequences = params[consequencesPosition].split(",");
		if (null == consequences || consequences.length == 0) return;
		
		String worstCaseConsequence = getWorstCaseConsequence(type, consequences);
		if ( ! StringUtils.isNullOrEmpty(worstCaseConsequence)) {
		
			int arrayPosition = StringUtils.getPositionOfStringInArray(consequences, worstCaseConsequence, false);
			String dccConseq = DccConsequence.getMafName(worstCaseConsequence, type, Integer.parseInt(params[1]));
				
			String [] genes = MafUtils.getHugoSymbol(params[genesPosition]).split(",");
			String [] geneIds = params[genesIdsPosition].split(",");
			String [] transcriptIds = params[transcriptIdsPosition].split(",");
			String [] aaChanges = params[aaChangesPosition].split(",");
			String [] baseChanges = params[baseChangesPosition].split(",");
			
			String gene = (arrayPosition > -1 && arrayPosition < genes.length) ? genes[arrayPosition] : null;
			String geneId = (arrayPosition > -1 && arrayPosition < geneIds.length) ? geneIds[arrayPosition] : null;
			String transcriptId = (arrayPosition > -1 && arrayPosition < transcriptIds.length) ? transcriptIds[arrayPosition] : null;
			String aaChange = (arrayPosition > -1 && arrayPosition < aaChanges.length) ? aaChanges[arrayPosition] : null;
			String baseChange = (arrayPosition > -1 && arrayPosition < baseChanges.length) ? baseChanges[arrayPosition] : null;
			
			// check that gene and geneId are not blank - if so, set to null so that they can be handled appropriately later on
			if (StringUtils.isNullOrEmpty(gene)) gene = null;
			
			maf.addVariantClassification(dccConseq);
			maf.addCanonicalTranscriptId(transcriptId);
			maf.addCanonicalAAChange(aaChange);
			maf.addCanonicalBaseChange(baseChange);
			maf.addEntrezGeneId(getEntrezId(geneId, ensemblToEntrez));
			maf.addHugoSymbol("Unknown".equals(gene) ? MafUtils.getHugoSymbol(geneId) : gene);
				
		}
		
		if (null == maf.getHugoSymbol()) {
			// didn't get a worst case gene - put in Unknown, as we want to keep them all 
			maf.addHugoSymbol("Unknown");
		}
	}
	
	static String getWorstCaseConsequence(MutationType type, String[] consequences) {
		String worstCaseConsequence = DccConsequence.getWorstCaseConsequence(type, consequences);
		return worstCaseConsequence;
	}
	
	
	public static void canonicalTranscript(MutationType type, String[] params,
			String[] genes, String[] geneIds, String[] transcriptIds,
			MAFRecord maf, Map<String, String> ensemblGeneToCanonicalTranscript, Map<String, Set<Integer>> ensemblToEntrez) {
		int i = 0, allTranscriptIdCount = 0;
		//TODO may need to up index positions if novel starts info is contained in dcc file
		for (String gene : genes) {
			String[] geneSpecificTranscriptIds =  transcriptIds[i].split(",");
			String geneId = geneIds[i++];
			
			// get canonical transcript id
			String canonicalTranscripId = ensemblGeneToCanonicalTranscript.get(geneId);
			maf.addCanonicalTranscriptId(canonicalTranscripId);
			if (null != canonicalTranscripId) {
				int positionInTranscripts = StringUtils.getPositionOfStringInArray(geneSpecificTranscriptIds, canonicalTranscripId, true);
				String [] consequences = params[22].split(",");
				String [] aaChanges = params[23].split(",");
				String [] baseChanges = params[24].split(",");
				
				//TODO what to do if canonical transcript id is not found!!
				
				if (positionInTranscripts > -1) {
					// we have a matching canonical transcript
					positionInTranscripts += allTranscriptIdCount;
					
					if (consequences.length > positionInTranscripts) {
						String dccConseq = DccConsequence.getMafName(consequences[positionInTranscripts], type, Integer.parseInt(params[1]));
						
						if ( ! DccConsequence.passesMafNameFilter(dccConseq)) {
							continue;
						}
						
						maf.addVariantClassification(dccConseq);
						maf.addCanonicalAAChange(aaChanges[positionInTranscripts]);
						maf.addCanonicalBaseChange(baseChanges[positionInTranscripts]);
					} else {
						logger.info("consequences.length is <= positionInTranscripts");
					}
				} else {
//					missingCanonicalTransId++;
					logger.debug("canonical transcript id not found in transcript id array");
					
					// don't want to record this gene 
					continue;
				}
				allTranscriptIdCount += geneSpecificTranscriptIds.length;
				
				// set the alternate transcriptId field to be all the other transcripts
				int position = 0;
				for (String transId : geneSpecificTranscriptIds) {
					if ( ! canonicalTranscripId.equalsIgnoreCase(transId)) {
						maf.setAlternateTranscriptId(StringUtils.isNullOrEmpty(maf.getAlternateTranscriptId()) 
								? transId : maf.getAlternateTranscriptId() + (position == 0 ? ";" : ", ") + transId);
						// also alternate aa change & base change
						maf.setAlternateAAChange(StringUtils.isNullOrEmpty(maf.getAlternateAAChange()) 
								? aaChanges[position] : maf.getAlternateAAChange() +  (position == 0 ? ";" : ", ") + aaChanges[position]);
						maf.setAlternateBaseChange(StringUtils.isNullOrEmpty(maf.getAlternateBaseChange()) 
								? baseChanges[position] : maf.getAlternateBaseChange() +  (position == 0 ? ";" : ", ") + baseChanges[position]);
					}
					position++;
				}
				
			} else {
				// still want to keep the transcript count up to date 
				allTranscriptIdCount += geneSpecificTranscriptIds.length;
				maf.addVariantClassification(DccConsequence.getMafName(params[22], type, Integer.parseInt(params[1])));
			}
			
			maf.addEntrezGeneId(MafUtils.getEntrezId(geneId, ensemblToEntrez));
			maf.addHugoSymbol("Unknown".equals(gene) ? MafUtils.getHugoSymbol(geneId) : gene);
		}
	}
	
	public static MAFRecord convertKRASToMaf(String[] params) {
		MAFRecord maf = new MAFRecord();
		
		maf.setHugoSymbol(params[0]);
		maf.setEntrezGeneId(params[1]);
		maf.setCenter(params[2]);
		maf.setNcbiBuild(Integer.parseInt(params[3]));
		maf.setChromosome(params[4]);
		maf.setStartPosition(Integer.parseInt(params[5]));
		maf.setEndPosition(Integer.parseInt(params[6]));
		maf.setStrand(params[7].charAt(0));
		maf.setVariantClassification(params[8]);
		maf.setVariantType(MutationType.getMutationType(params[9]));
		maf.setRef(params[10]);
		maf.setTumourAllele1(params[11]);
		maf.setTumourAllele2(params[12]);
		maf.setDbSnpId(params[13]);
		maf.setDbSnpValStatus(params[14]);
		maf.setTumourSampleBarcode(params[15]);
		maf.setNormalSampleBarcode(params[16]);
		maf.setNormalAllele1(params[17]);
		maf.setNormalAllele2(params[18]);
		maf.setTumourValidationAllele1(params[19]);
		maf.setTumourValidationAllele2(params[20]);		
		maf.setNormalValidationAllele1(params[21]);		
		maf.setNormalValidationAllele2(params[22]);		
		maf.setVerificationStatus(params[23]);		
		maf.setValidationStatus(params[24]);		
		maf.setMutationStatus(params[25]);		
		maf.setSequencingPhase(params[26]);		
		maf.setSequencingSource(params[27]);		
		maf.setValidationMethod(params[28]);		
		maf.setScore(params[29]);		
		maf.setBamFile(params[30]);		
		maf.setSequencer(params[31]);
		// QCMG
		if (params.length > 32)
			maf.setFlag(params[32]);
		if (params.length > 33)
			maf.setNd(params[33]);
		if (params.length > 34)
			maf.setTd(params[34]);
		if (params.length > 35)
			maf.setCanonicalTranscriptId(params[35]);
		if (params.length > 36)
			maf.setCanonicalAAChange(params[36]);
		if (params.length > 37)
			maf.setCanonicalBaseChange(params[37]);
		if (params.length > 38)
			maf.setAlternateTranscriptId(params[38]);
		if (params.length > 39)
			maf.setAlternateAAChange(params[39]);
		if (params.length > 40)
			maf.setAlternateBaseChange(params[40]);
		
		return maf;
	}
	
	
	public static boolean passesHighConfidenceFilter(String flag, MutationType type, String td, boolean dbSNPNovel, char alt) {
		// must be class A 
		return SnpUtils.isClassA(flag) 
		&& ((MutationType.isIndel(type) && dbSNPNovel)
				|| (MutationType.isSubstitution(type) && passesCountCheck(td, 5, alt)));
	}
	public static boolean passesLowerConfidenceFilter(String flag, MutationType type, String td, char alt) {
		// must be class A or B 
		return SnpUtils.isClassAorB(flag)
		&& (MutationType.isIndel(type) || (MutationType.isSubstitution(type) && passesCountCheck(td, 4, alt)));
	}
	
	/**
	 * 
	 * 
	 * 
	 * @param td
	 * @param count
	 * @param variant
	 * @return boolean indicating if the 
	 */
	static boolean passesCountCheck(String td, int count, char alt) {
		if ( '\u0000' != alt &&  ! StringUtils.isNullOrEmpty(td)) {
			List<PileupElement> pileups = PileupElementUtil.createPileupElementsFromString(td);
			for (PileupElement pe : pileups) {
				if (pe.getBase() == alt && pe.getTotalCount() >= count) return true;
			}
		}
		return false;
	}
	
	/**
	 * Retrieves the variant from the maf record based on the reference, and the two tumour alleles.
	 * <br>
	 * If tumour allele 1 is equal to the reference, then tumour allele 2 is returned and vice versa
	 * 
	 * @param maf
	 * @param mafType 
	 * @return  char representing the variant
	 * @throws IllegalArgumentException if the maf record is null, or if the reference, tumour allele 1 or 2 are null
	 */
	public static String getVariant(final MAFRecord maf) {
		if (null == maf || null == maf.getRef() || null == maf.getMafType())
			throw new IllegalArgumentException("Null maf object passed to getVariant");
		
		String allele1 = maf.getMafType().isSomatic() ? maf.getTumourAllele1() : maf.getNormalAllele1();
		String allele2 = maf.getMafType().isSomatic() ? maf.getTumourAllele2() : maf.getNormalAllele2();
		
		if (StringUtils.isNullOrEmpty(allele1) || StringUtils.isNullOrEmpty(allele2))
			throw new IllegalArgumentException("Maf object does not contain valid alleles in getVariant: " + maf.toString());
			
		String variant = maf.getRef().equals(allele1) ? allele2 : allele1;
		return variant;
	}
	
	public static String getFullChromosome(final MAFRecord maf) {
		if (null == maf)
			throw new IllegalArgumentException("Null maf object passed to getFullChromosome");
		
		return getFullChrFromMafChr(maf.getChromosome());
	}
	
	/**
	 * Converts a chromosome in maf format to DCC/picard/QCMG format
	 * <br>
	 * eg. 1,2,X,Y,M -> chr1, chr2, chrX, chrMT
	 * <p>
	 * Also converts lower case letters to upper case
	 * 
	 * @param chr String representing maf chromosome
	 * @return String containing QCMG equivalent
	 * @throws IllegalArgumentException if the input string is null or empty
	 */
	public static String getFullChrFromMafChr(final String chr) {
		if (StringUtils.isNullOrEmpty(chr))
			throw new IllegalArgumentException("Null or empty string passed to getFullChrFromMafChr");
		//getting lower case x instead of of the maf required upper case from some institutes
		char c = chr.charAt(0);
		String fullChr = "chr" + (Character.isLowerCase(c) ? Character.toUpperCase(c) : chr);
			
		if ("chrM".equals(fullChr)) fullChr = "chrMT";
		return fullChr;
	}
	
	
	public static void loadPositionsOfInterest(String mafFile, Collection<ChrPosition> positionsOfInterest ) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(new File(mafFile));
		try {
			
			int count = 0;
			
			for (TabbedRecord rec : reader) {
				if (count++ == 0 && (rec.getData().startsWith("Hugo_Symbol"))) continue;	// first line is header
				
				String[] params = TabTokenizer.tokenize(rec.getData());
				String chr = params[4];
				int startPos = Integer.parseInt(params[5]);
				int endPos = Integer.parseInt(params[6]);
				
				ChrPosition cp = new ChrRangePosition(chr, startPos, endPos);
				positionsOfInterest.add(cp);
			}
			logger.info("for file: " + mafFile + " no of records: " + count + ", no of entries in chrpos set: " + positionsOfInterest.size());
			
		} finally {
			reader.close();
		}
	}
	
	static class GenePositions {
		final String gene;
		final List<Integer> arrayPositions = new ArrayList<Integer>();
		GenePositions(String gene, int position) {
			this.gene = gene;
			arrayPositions.add(position);
		}
		public String getGene() {
			return gene;
		}
		public void addPosition(int position) {
			arrayPositions.add(position);
		}
		public Integer[] getPositions() {
			return arrayPositions.toArray(new Integer[] {});
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((gene == null) ? 0 : gene.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			GenePositions other = (GenePositions) obj;
			if (gene == null) {
				if (other.gene != null)
					return false;
			} else if (!gene.equals(other.gene))
				return false;
			return true;
		}
	}

	
	
}
