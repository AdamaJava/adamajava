package org.qcmg.qsv.util;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordCoordinateComparator;
import htsjdk.samtools.SAMRecordQueryNameComparator;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.Constants;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.Options;
import org.qcmg.qsv.QSVCluster;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.blat.BLATRecord;
import org.qcmg.qsv.discordantpair.DiscordantPairCluster;
import org.qcmg.qsv.discordantpair.MatePair;
import org.qcmg.qsv.discordantpair.PairClassification;
import org.qcmg.qsv.discordantpair.PairGroup;
import org.qcmg.qsv.softclip.Breakpoint;
import org.qcmg.qsv.softclip.Clip;
import org.qcmg.qsv.softclip.SoftClipCluster;
import org.qcmg.qsv.tiledaligner.TiledAlignerLongMap;
import org.qcmg.qsv.tiledaligner.TiledAlignerUtil;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class TestUtil {
	
	private static final String FILE_SEPERATOR = System.getProperty("file.separator");
	private static String NEWLINE = System.getProperty("line.separator");
	
	public final static SAMFileHeader SOLID_SAM_FILE_HEADER_COORDINATE_SORTED = createSamHeaderObject(SortOrder.coordinate);
	public final static SAMFileHeader SOLID_SAM_FILE_HEADER_QUERY_NAME_SORTED = createSamHeaderObject(SortOrder.queryname);
	
	private static final Set<Clip> RIGHT_CLIPS_TRUE = getRightClips(true);
	private static final Set<Clip> RIGHT_CLIPS_FALSE = getRightClips(false);
	private static final Set<Clip> LEFT_CLIPS_TRUE = getLeftClips(true);
	private static final Set<Clip> LEFT_CLIPS_FALSE = getLeftClips(false);

	public static String[] getValidOptions(final TemporaryFolder testFolder,
            final String normalBam, final String tumorBam, final String preprocessMode,
            final String analysisMode) throws IOException {
		
		return getValidOptions(testFolder,normalBam,tumorBam, preprocessMode,analysisMode, true);
    }
	public static String[] getValidOptions(final TemporaryFolder testFolder,
			final String normalBam, final String tumorBam, final String preprocessMode,
			final String analysisMode, boolean goodOutput) throws IOException {
		String iniFile = setUpIniFile(testFolder, preprocessMode, analysisMode, normalBam, tumorBam, 3, 1, testFolder.getRoot().toString(), testFolder.getRoot().toString(), goodOutput);
		
		return new String[] {"--ini", iniFile, "--tmp",  testFolder.getRoot().toString()};
	}

	public static String[] getInvalidOptions(final TemporaryFolder testFolder,
            final String normalBam, final String tumorBam,
            final String mode, Integer clusterSize, Integer filterSize, String tmpDir, String outputDir) throws IOException {
       
			String iniFile = setUpIniFile(testFolder, "both", mode, normalBam, tumorBam, clusterSize, filterSize, tmpDir, outputDir);
			
            return new String[] {"--ini", iniFile, "--tmp",  testFolder.getRoot().toString()};
    }
	
	private static String setUpIniFile(final TemporaryFolder testFolder, String preprocessMode,
            final String analysisMode, String normalBam, String tumorBam, int clusterSize, int filterSize, String tmpDir, String outputDir) throws IOException {
		return setUpIniFile(testFolder,  preprocessMode, analysisMode,  normalBam,  tumorBam,  clusterSize,  filterSize,  tmpDir,  outputDir, true);
	}
	
	private static String setUpIniFile(final TemporaryFolder testFolder, String preprocessMode,
            final String analysisMode, String normalBam, String tumorBam, int clusterSize, int filterSize, String tmpDir, String outputDir, boolean goodOutput) throws IOException {
		
		File iniFile = testFolder.newFile("test.ini");
		File reference = testFolder.newFile("reference_file");
		if (iniFile.exists()) {
			iniFile.delete();
		}		
		
		try (BufferedWriter out = new BufferedWriter(new FileWriter(iniFile))) {
		out.write("[general]" + NEWLINE);
		out.write("log=test.log" + NEWLINE);
		out.write("loglevel=DEBUG" + NEWLINE);
		out.write("sample=test" + NEWLINE);
		out.write("platform=solid" + NEWLINE);		
		out.write("sv_analysis="+analysisMode + NEWLINE);
		if (goodOutput) {
			out.write("output="+outputDir + NEWLINE);
		}
		out.write("reference=" + reference.getAbsolutePath() + NEWLINE);
		out.write("isize_records=all" + NEWLINE);
		out.write("qcmg=true" + NEWLINE);
		
		out.write("[pair]" + NEWLINE);
		out.write("pair_query=and(Cigar_M > 35,option_SM > 14,MD_mismatch < 3,Flag_DuplicateRead == false)" + NEWLINE);
		out.write("pairing_type=lmp" + NEWLINE);
		out.write("cluster_size="+clusterSize + NEWLINE);
		out.write("filter_size="+filterSize + NEWLINE);
		out.write("primer_size=3" + NEWLINE);
		out.write("mapper=bioscope" + NEWLINE);
		out.write("[clip]" + NEWLINE);
		out.write("clip_query=and(Cigar_M > 35,option_SM > 14,MD_mismatch < 3,Flag_DuplicateRead == false)" + NEWLINE);
		out.write("clip_size=3" + NEWLINE);
		out.write("consensus_length=20" + NEWLINE);
		out.write("blatpath=/home/Software/BLAT" + NEWLINE);
		out.write("blatserver=localhost" + NEWLINE);
		out.write("blatport=50000" + NEWLINE);
		
		out.write("["+ QSVConstants.DISEASE_SAMPLE +"]" + NEWLINE);
		out.write("name=TD" + NEWLINE);
		out.write("sample_id=ICGC-DBLG-20110506-01-TD" + NEWLINE);
		out.write("input_file="+tumorBam + NEWLINE);
		if (preprocessMode.equals("none")) {
			out.write("discordantpair_file="+tumorBam + NEWLINE);
		}
		out.write("["+ QSVConstants.DISEASE_SAMPLE +"/size_1]" + NEWLINE);
    	out.write("rgid=20110221052813657" + NEWLINE);
    	out.write("lower=640" + NEWLINE);
    	out.write("upper=2360" + NEWLINE + NEWLINE);
    	out.write("name=seq_mapped_1" + NEWLINE);
    	out.write("["+ QSVConstants.DISEASE_SAMPLE +"/size_2]" + NEWLINE);
    	out.write("rgid=20110221052813667" + NEWLINE);
    	out.write("lower=640" + NEWLINE);
    	out.write("upper=2360" + NEWLINE + NEWLINE);
    	out.write("name=seq_mapped_1" + NEWLINE);
    	out.write("["+ QSVConstants.CONTROL_SAMPLE +"]" + NEWLINE);
    	out.write("name=ND" + NEWLINE);
    	out.write("sample_id=ICGC-DBLG-20110506-01-ND" + NEWLINE);
		out.write("input_file="+normalBam + NEWLINE);
		if (preprocessMode.equals("none")) {
			out.write("discordantpair_file="+normalBam + NEWLINE);
		}
    	out.write("["+ QSVConstants.CONTROL_SAMPLE +"/size_1]" + NEWLINE);
    	out.write("rgid=20110221052813657" + NEWLINE);
    	out.write("lower=640" + NEWLINE);
    	out.write("upper=2360" + NEWLINE + NEWLINE);
    	out.write("name=seq_mapped_1" + NEWLINE);
    	out.write("["+ QSVConstants.CONTROL_SAMPLE +"/size_2]" + NEWLINE);
    	out.write("type=ND" + NEWLINE);
    	out.write("rgid=20110221052813667" + NEWLINE);
    	out.write("lower=640" + NEWLINE);
    	out.write("upper=2360" + NEWLINE + NEWLINE);   
    	out.write("name=seq_mapped_1" + NEWLINE);
		}
		return iniFile.getAbsolutePath();
	}

	public static DiscordantPairCluster setupSolidCluster(PairGroup zp, String clusterType, TemporaryFolder testfolder, String chr1, String chr2) throws Exception {
		  List<MatePair> pairs = setupMatePairs(testfolder, zp);
		  String query = "Cigar_M > 35 and option_SM > 14 and MD_mismatch < 3 and Flag_DuplicateRead == false ";
		  String tumourFile = testfolder.newFile("tumor.bam").getAbsolutePath();
		  createBamFile(tumourFile, zp, SortOrder.coordinate);
//		  String normalFile = testfolder.newFile("normal.bam").getAbsolutePath();
//		  createBamFile(normalFile, zp, SortOrder.coordinate);
	      QSVParameters tumor = TestUtil.getQSVParameters(testfolder, tumourFile, tumourFile, true, "both", "both");
	      QSVParameters normal = TestUtil.getQSVParameters(testfolder, tumourFile, tumourFile, false, "both", "both");
//	      QSVParameters tumor = TestUtil.getQSVParameters(testfolder, tumourFile, normalFile, true, "both", "both");
//	      QSVParameters normal = TestUtil.getQSVParameters(testfolder, tumourFile, normalFile, false, "both", "both");
	      
	      DiscordantPairCluster cluster = new DiscordantPairCluster(chr1, chr2, zp.getPairGroup(), tumor, true);	      
	
	      for (MatePair p : pairs) {
	          cluster.getClusterMatePairs().add(p);
	      }
	      
	      if ( ! clusterType.equals("somatic")) {
	    	  	cluster.getMatchedReadPairs().add(pairs.get(0));
	      }
	      
	      cluster.setClusterEnds();
	      cluster.setNormalRange(3000);
	      cluster.finalize(tumor, normal, clusterType, 1, query, "lmp", true);
	      return cluster;
	}

    
    public static List<MatePair> setupMatePairs(TemporaryFolder testFolder, PairGroup pg) throws QSVException {
    		List<SAMRecord> records = getAACSAMRecords(SortOrder.queryname);
        
        return Arrays.asList(
        		new MatePair(records.get(0), records.get(1)),
        		new MatePair(records.get(2), records.get(3)),
        		new MatePair(records.get(4), records.get(5)),
        		new MatePair(records.get(6), records.get(7)),
        		new MatePair(records.get(8), records.get(9)),
        		new MatePair(records.get(10), records.get(11))
        		);
	}
    
	public static QSVParameters getQSVParameters(final TemporaryFolder testFolder, final String normalBam, final String tumorBam,
            final boolean isTumor, final String analysisMode) throws Exception {
        Options options = new Options(getValidOptions(testFolder, normalBam, tumorBam, "both", analysisMode));
        options.parseIniFile();
     
        String matepairsDir = testFolder.newFolder("matepair").toString() + FILE_SEPERATOR;
        for (PairClassification zp : PairClassification.values()) {
            File mateDir = new File(matepairsDir + zp.getPairingClassification() + FILE_SEPERATOR);
            mateDir.mkdir();
        }
        
        QSVParameters p = new QSVParameters(options, isTumor, testFolder.getRoot().toString() + FILE_SEPERATOR, matepairsDir, new Date(), "test");
        return p;
    }

	public static QSVParameters getQSVParameters(final TemporaryFolder testFolder, final String normalBam, final String tumorBam,
            final boolean isTumor, final String preprocessMode, String analysisMode) throws Exception {
        Options options = new Options(getValidOptions(testFolder, normalBam, tumorBam,preprocessMode, analysisMode));
        options.parseIniFile();

        String matepairsDir = testFolder.newFolder("matepair").toString() + FILE_SEPERATOR;
        for (PairClassification zp : PairClassification.values()) {
            File mateDir = new File(matepairsDir + zp.getPairingClassification() + FILE_SEPERATOR);
            mateDir.mkdir();
        }
        
        QSVParameters p = new QSVParameters(options, isTumor, testFolder.getRoot().toString() + FILE_SEPERATOR, matepairsDir, new Date(), "test");
        return p;
    }
    
    public static String getFilename(final File file) {
    		return file.getName();
    }

    public static String getFilename(final String fullname) {
    		return getFilename(new File(fullname));
    }
    
	public static File createHiseqBamFile(final String inputFileName, PairGroup pg, SortOrder sort) throws IOException {
		String samFile = inputFileName.replace("bam", "sam");
    	if (pg != null) {
    		createSamFile(samFile, pg, sort, true);
    	} else {
    		createSamFile(samFile, sort, true);
    	}
    	
        SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(samFile));
        SAMFileHeader header = reader.getFileHeader();
        
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		factory.setCreateIndex(true);
		SAMFileWriter writer = factory.makeBAMWriter(header, false, new File(inputFileName));
		
		for (SAMRecord r: reader) {
			writer.addAlignment(r);
		}
		reader.close();
		writer.close();
        
		return new File(inputFileName);
	}
    
    public static File createBamFile(final String inputFileName, PairGroup pg, SortOrder sort) throws IOException {
    
	    	if (pg != null) {
//	    		createSamFile(samFile, pg, sort, false);
	    		SAMFileHeader h = sort == SortOrder.coordinate ? SOLID_SAM_FILE_HEADER_COORDINATE_SORTED : SOLID_SAM_FILE_HEADER_QUERY_NAME_SORTED;
	    		List<SAMRecord> recs = getAACSAMRecords(sort);
	    		
	    		SAMFileWriterFactory factory = new SAMFileWriterFactory();
	    		factory.setCreateIndex(true);
	    		try (SAMFileWriter writer = factory.makeBAMWriter(h, false, new File(inputFileName))) {
	    		
		    		for (SAMRecord r: recs) {
//		    			System.out.println("r zp tag: " + r.getAttribute("ZP"));
//		    			System.out.println("mate ref: " + r.getMateReferenceName());
		    			writer.addAlignment(r);
		    		}
	    		}
	    	} else {
//	    		String samFile = inputFileName.replace("bam", "sam");
//	    		createSamFile(samFile, sort, false);
	    		List<SAMRecord> recs = createSamBodyRecords(sort);
	    		SAMFileHeader h = sort == SortOrder.coordinate ? SOLID_SAM_FILE_HEADER_COORDINATE_SORTED : SOLID_SAM_FILE_HEADER_QUERY_NAME_SORTED;
//	        try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(samFile))) {
//		        SAMFileHeader header = reader.getFileHeader();
				SAMFileWriterFactory factory = new SAMFileWriterFactory();
				factory.setCreateIndex(true);
				try (SAMFileWriter writer = factory.makeBAMWriter(h, false, new File(inputFileName))) {
					for (SAMRecord r: recs) {
						writer.addAlignment(r);
					}
				}
//	        }
	    }
        
		return new File(inputFileName);
    }
//    public static File createBamFile(final String inputFileName, PairGroup pg, SortOrder sort)
//    		throws IOException {
//    	
//    	String samFile = inputFileName.replace("bam", "sam");
//    	if (pg != null) {
//    		createSamFile(samFile, pg, sort, false);
//    	} else {
//    		createSamFile(samFile, sort, false);
//    	}
//    	
//    	SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(samFile));
//    	SAMFileHeader header = reader.getFileHeader();
//    	
//    	SAMFileWriterFactory factory = new SAMFileWriterFactory();
//    	factory.setCreateIndex(true);
//    	SAMFileWriter writer = factory.makeBAMWriter(header, false, new File(inputFileName));
//    	
//    	for (SAMRecord r: reader) {
//    		writer.addAlignment(r);
//    	}
//    	reader.close();
//    	writer.close();
//    	
//    	return new File(inputFileName);
//    }
	
    public static File createSamFile(final String inputFileName, SortOrder sort, boolean isHiseq)
            throws IOException {
    
        final List<String> data = new ArrayList<>();
        // create sam header and records
        if ( ! isHiseq) {
	        	data.addAll(createSamHeader(sort));
	        	data.addAll(createSamBody());
        } else {
	        	data.addAll(createHiseqSamHeader(sort));
	        	data.addAll(createHiseqSamBody());
        }
        try ( BufferedWriter out = new BufferedWriter(new FileWriter(inputFileName))) {
	        for (final String line : data) {
	            out.write(line + "" + NEWLINE);
	        }
        }
		return new File(inputFileName);
    }
	 
	 private static Collection<? extends String> createHiseqSamBody() {
        return createHiseqAACSamBody();
	}

	public static File createSamFile(final String inputFileName, PairGroup pg, SortOrder sort, boolean isHiseq)
	            throws IOException {
	        final List<String> data = new ArrayList<>();
	        // create sam header and records
	        if ( ! isHiseq) {
	        		data.addAll(createSamHeader(sort));
	        } else {
	        		data.addAll(createHiseqSamHeader(sort));
	        }
	        
	        if ( ! isHiseq) {
		        if (pg.equals(PairGroup.AAC)) {
		        		data.addAll(createAACSamBody());	    
		        }
	        } else {
		        	if (pg.equals(PairGroup.AAC)) {
			        	data.addAll(createHiseqAACSamBody());	    
			    }
	        }
	       
	        try (BufferedWriter out = new BufferedWriter(new FileWriter(inputFileName))) {
		        for (final String line : data) {
		            out.write(line + NEWLINE);
		        }
	        }
	        return new File(inputFileName);
	 }
	
	 public static SAMFileHeader createHiseqSamHeaderObject(SortOrder sort) {
		 SAMSequenceDictionary dict = new SAMSequenceDictionary(
				 Arrays.asList(new SAMSequenceRecord("chr1",249250621),
						 new SAMSequenceRecord("chr4",191154276),
						 new SAMSequenceRecord("chr7",159138663),
						 new SAMSequenceRecord("chrX",155270560),
						 new SAMSequenceRecord("chrY",59373566),
						 new SAMSequenceRecord("chr10",135534747),
						 new SAMSequenceRecord("chr19",59128983),
						 new SAMSequenceRecord("GL000191.1",106433),
						 new SAMSequenceRecord("SN:GL000211.1",166566),
						 new SAMSequenceRecord("chrMT",16569)));
		 SAMFileHeader h = new SAMFileHeader();
		 h.setSequenceDictionary(dict);
		 h.setSortOrder(sort);
		 
		 
		 SAMReadGroupRecord rg1 = new SAMReadGroupRecord("20110221052813657");
		 rg1.setPlatform("ILLUMINA");
		 rg1.setLibrary("Library_20120511_C	SM:Colo-829");
		 SAMReadGroupRecord rg2 = new SAMReadGroupRecord("20110221052813667");
		 rg2.setPlatform("ILLUMINA");
		 rg2.setLibrary("Library_20120511_C	SM:Colo-829");
		 
		 h.setReadGroups(Arrays.asList(rg1, rg2));
		 return h;
	 }

	private static Collection<String> createHiseqSamHeader(SortOrder sort) {
		final List<String> data = new ArrayList<>();
		data.add("@HD	VN:1.0	GO:none	SO:"+ sort.name());
		data.add("@SQ	SN:chr1	LN:249250621	");
		data.add("@SQ	SN:chr4	LN:191154276	");
		data.add("@SQ	SN:chr7	LN:159138663	");
		data.add("@SQ	SN:chrX	LN:155270560	");
		data.add("@SQ	SN:chrY	LN:59373566	");
		data.add("@SQ	SN:chr10	LN:135534747");	        
		data.add("@SQ	SN:chr19	LN:59128983	");
		data.add("@SQ	SN:GL000191.1	LN:106433	");
		data.add("@SQ	SN:GL000211.1	LN:166566	");
		data.add("@SQ	SN:chrMT	LN:16569	");
		data.add("@RG	ID:20110221052813657	PL:ILLUMINA	PU:lane_3	LB:Library_20120511_C	SM:Colo-829");
		data.add("@RG	ID:20120608103628549	PL:ILLUMINA	PU:lane_2	LB:Library_20120511_C	SM:Colo-829");
		return data;
	}
	
	
	public static List<SAMRecord> getAACSAMRecords(SortOrder so) {
		SAMFileHeader h = createSamHeaderObject(so); 
		String rg = "20110221052813657";
		String zp = "AAC";
		Integer one = Integer.valueOf(1);
		
		SAMRecord s1 = getSAM(h, "254_166_1407", 129, "chr7",140188379,  63, "50M", "chr7", 140191044,  2715, "ACGGCTCATGTCTCCTTAGAATGTATAAAAGCAAGCTGTGCTCTGACCAC", "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIA", "40A9", rg,zp, one);
		SAMRecord s2 = getSAM(h, "254_166_1407", 65, "chr7",140191044,  63, "50M", "chr7", 140188379,  -2715, "ACTCCATTTCTAGAAAAAAATTAGAAAATTAACTGGAACCAGGAGAGGTG", "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIHIIIIIII@", "18C31", rg,zp, one);
		SAMRecord s3 = getSAM(h, "1789_1456_806",65,  "chr7",140191179, 69,  "50M", "chr7", 140188227,  -3002, "ATGGCAAAACCCTGTCTCATTCCTTCAATCCTAGCACTTTGGGAGGCTGA", "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII@", "50", rg,zp, one);
		SAMRecord s4 = getSAM(h, "1789_1456_806", 129, "chr7",140188227, 69, "50M", "chr7", 140191179,  3002, "ATGGCAAAACCCTGTCTCATTCCTTCAATCCTAGCACTTTGGGAGGCTGA", "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII@", "50", rg,zp, one);
		SAMRecord s5 = getSAM(h,"515_451_1845",129,	"chr7",	140188449,	69,	"50M","chr7",	140191238,	2839,	"AACCTCCTGAGGCTGAGTACAGTGGCTTATGCCTGTAATCCCAGCACACT","IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIICGIIIIIADI4","50",rg,zp, one);
		SAMRecord s6 = getSAM(h,"515_451_1845",	65	,"chr7",	140191238,	69,	"50M","chr7",	140188449,	-2839	,"GTCACTTGAGGTCAGTTCAAGACCAGCCTGGCCAACATAGTGAAACCCCC","IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIB","50",rg,zp, one);
		SAMRecord s7 = getSAM(h,"1887_329_319",	113,	"chr7",	140188962,	61,	"2H48M","chr7",	140191372,	2462	,"TGGCCTTTAGAAGTAGGAGAAGTACAGAGTACTTTGCCATTTTAAGGC","IIIIIII72IICBIIII5AIIIDIII<28II''<IIIIIIIEDIIIII","48",rg,zp, one);
		SAMRecord s8 = getSAM(h,"1887_329_319",	177,	"chr7",	140191372,	61,	"50M","chr7",	140188962,	-2462	,"AAGAAGCACATGAGGAGGCTGAAGCCCAAAAGAAAGATGAGGCAGAGGTC",";III%%III''IIIGIIII%%IIIIIIIIIIIIIIIIIIIIIIIIIIIII","50",rg,zp, one);
		SAMRecord s9 = getSAM(h,"690_397_1054",	113,	"chr7",	140188962,	61,	"3H47M","chr7",	140191394,	2485	,"TGGCCTTTAGAAGTAGGAGAAGTACAGAGTACTTTGCCATTTTAAGG","IIIIIIII8IIIIIIIIIIIIIIIIIII<IIIE@IIIIIIIIIIIII","47",rg,zp, one);
		SAMRecord s10 = getSAM(h,"690_397_1054",	177,	"chr7",	140191394,	61	,"50M","chr7",	140188962,	-2485	,"AGCCCAAAAGAAAGATGAGGCAGAGGTCCAAGTAAACCACTAGCTTGTTG","2IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII","50",rg,zp, one);
		SAMRecord s11 = getSAM(h,"1822_622_784",	113,	"chr7",	140188994,	39	,"6H44M","chr7",	140191589,	2642	,"TTTGCCATTTTAAGGCCCGGAAAATGAGGTTGTCGAGTCATGCA","G@HIIIIIII>IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII","44",rg,zp, one);
		SAMRecord s12 = getSAM(h,"1822_622_784",	177,	"chr7",	140191589,	39,	"9H41M","chr7",	140188994,	-2642	,"GACCCAAATTGGTAATAACCAAAACTGTCCATGTTGGTCCT","?:9=I'&&&IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII","6C0C33",rg,zp, one);
		SAMRecord s13 = getSAM(h,"874_1001_370",	113,	"chr7",	140189005,	63,	"50M","chr7",	140191611,	2656	,"AAGGCCCGGAAAATGAGGTTGTCGAGTCATGCACAAATGTTGCCTGTAAT","BIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII","50",rg,zp, one);
		SAMRecord s14 = getSAM(h,"874_1001_370",	177,	"chr7",	140191611,	63,	"50M","chr7",	140189005,	-2656	,"AACTGTCCATGTTGGTCCTTTGTCCAGGATCTGTGACATTCTGAACTATT",">IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII","25T24",rg,zp, one);
		SAMRecord s15 = getSAM(h,"2134_481_267",	129,	"chr7",	140189059,	56,	"50M","chr7",	140191509,	2500	,"TAGCCCATAAGTGAGCTTGGAGCTTGAGGAATTTAAACTTCTGCTTTATT","IIIII%%<BI9:DIIIIIIIE5>II&&IIIIGIIII:CII20BIIICFI@","50",rg,zp, one);
		SAMRecord s16 = getSAM(h,"2134_481_267",	65,	"chr7",	140191509,	56,	"50M","chr7",	140189059,	-2500	,"TTGGACTGCATGCTGCTGTCTAGAGCTTTCTCAATGGACCTGGAACTTTA","IIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIDHIII((III**IIIIIA","14A35",rg,zp, one);
		
		
		List<SAMRecord> recs = Arrays.asList(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12, s13, s14, s15, s16);
		
		if (h.getSortOrder().equals(SortOrder.coordinate)) {
			recs.sort(new SAMRecordCoordinateComparator());
		} else if (h.getSortOrder().equals(SortOrder.queryname)) {
			recs.sort(new SAMRecordQueryNameComparator());
		}
		return recs;
	}
	
	private static SAMRecord getSAM(SAMFileHeader h, String readName,int flags, String chr, int pos, int mapQ, String cigar, String mRef, int mPos, int iSize, String bases, String quals, String md, String rg ,String zp, Integer nh) {
		SAMRecord s1 = new SAMRecord(h);
		s1.setAlignmentStart(pos);
		s1.setCigarString(cigar);
		s1.setBaseQualityString(quals);
		s1.setFlags(flags);
		s1.setMappingQuality(mapQ);
		s1.setInferredInsertSize(iSize);
		s1.setReadName(readName);
		s1.setReferenceName(chr);
		s1.setReadString(bases);
		s1.setAttribute("MD", md);
		s1.setAttribute("RG", rg);
		s1.setAttribute("ZP", zp);
		s1.setAttribute("NH", nh);
		s1.setMateReferenceName(mRef);
		s1.setMateAlignmentStart(mPos);
		return s1;
	}

	public static List<String> createAACSamBody() {
		final List<String> data = new ArrayList<>();
		 
	     data.add("254_166_1407	129	chr7	140188379	63	50M	=	140191044	2715	ACGGCTCATGTCTCCTTAGAATGTATAAAAGCAAGCTGTGCTCTGACCAC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIA	MD:Z:40A9	RG:Z:20110221052813657	NH:i:1	CM:i:2	NM:i:1	SM:i:97	ZP:Z:AAC	CQ:Z:@B3B?BBAAB@B<;@>B5B>AB@AAAB;@@BB:A@A<>B=B@66>B;;=A	CS:Z:G21303221311222020322031133300023102321113222121011");
		 data.add("254_166_1407	65	chr7	140191044	63	50M	=	140188379	-2715	ACTCCATTTCTAGAAAAAAATTAGAAAATTAACTGGAACCAGGAGAGGTG	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIHIIIIIII@	MD:Z:18C31	RG:Z:20110221052813657	NH:i:1	CM:i:2	NM:i:1	SM:i:97	ZP:Z:AAC	CQ:Z:BBBBBBBBABB>>7AA:@??B??;>@?>5>@@B@@?1:@?=81<::>=?@	CS:Z:T31220130022322000000303220003030121020101202222011");
		 data.add("1789_1456_806	65	chr7	140191179	69	50M	=	140188227	-3002	ATGGCAAAACCCTGTCTCATTCCTTCAATCCTAGCACTTTGGGAGGCTGA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII@	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAC	CQ:Z:BBABBBBAB@BBA?BBBA?BBBA@AB=AB@@BA;@BB<@@B??@B9B>B@	CS:Z:T33103100010021122213020202103202323112001002203212");
		 data.add("1789_1456_806	129	chr7	140188227	69	50M	=	140191179	3002	GGCCAATCAGAAACTCAAAAGAATGCAACCATTTGCCTGTTATCTACCTA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII@	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:43	ZP:Z:AAC	CQ:Z:BBBBBBBBBBBBABBBABABABABBBBBA?>BBBB<BABB<A@@<?7=@@	CS:Z:G00301032122001221000220313101013001302110332231023");
		 data.add("515_451_1845	129	chr7	140188449	69	50M	=	140191238	2839	AACCTCCTGAGGCTGAGTACAGTGGCTTATGCCTGTAATCCCAGCACACT	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIICGIIIIIADI4	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAC	CQ:Z:>B?BAAA?AA@?AAB?@ABB==@A?@@@BA=A><>:=6A408<688<&?4	CS:Z:G20102202122032122131121103203313021130320012311112");
		 data.add("515_451_1845	65	chr7	140191238	69	50M	=	140188449	-2839	GTCACTTGAGGTCAGTTCAAGACCAGCCTGGCCAACATAGTGAAACCCCC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIB	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:38	ZP:Z:AAC	CQ:Z:BBB>B>BB?BBABBB@BB>A5BB??BA??A>B?77;>?;?BB=<@@A>BB	CS:Z:T11211201220121210210221012302103010113321120010000");
		 data.add("1887_329_319	113	chr7	140188962	61	2H48M	=	140191372	2462	TGGCCTTTAGAAGTAGGAGAAGTACAGAGTACTTTGCCATTTTAAGGC	IIIIIII72IICBIIII5AIIIDIII<28II''<IIIIIIIEDIIIII	MD:Z:48	RG:Z:20110221052813657	NH:i:1	CM:i:1	NM:i:0	SM:i:100	ZP:Z:AAC	CQ:Z:ABAAA@%AAAA/=A0-'@A.+(5B;50@A8*,@B7<'=A+(0@A9@:9B-	CS:Z:T13020300031031000131222113120222023120223002030110");
		 data.add("1887_329_319	177	chr7	140191372	61	50M	=	140188962	-2462	AAGAAGCACATGAGGAGGCTGAAGCCCAAAAGAAAGATGAGGCAGAGGTC	;III%%III''IIIGIIII%%IIIIIIIIIIIIIIIIIIIIIIIIIIIII	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:3	NM:i:0	SM:i:75	ZP:Z:AAC	CQ:Z:>AB?@>?AA?>?@B@A>=>>0?>?A-BA=>%<<>@+=:>?'3>@<%A@>;	CS:Z:G02102221302213220022000100320222302202210111300220");
		 data.add("690_397_1054	113	chr7	140188962	61	3H47M	=	140191394	2485	TGGCCTTTAGAAGTAGGAGAAGTACAGAGTACTTTGCCATTTTAAGG	IIIIIIII8IIIIIIIIIIIIIIIIIII<IIIE@IIIIIIIIIIIII	MD:Z:47	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAC	CQ:Z:BBBB=<BBBA<AB106@B/.>@B;2A@B77:BB64=>B*/=AB::@?9%=	CS:Z:T20203000310310021312221131202220231202230020301121");
		 data.add("690_397_1054	177	chr7	140191394	61	50M	=	140188962	-2485	AGCCCAAAAGAAAGATGAGGCAGAGGTCCAAGTAAACCACTAGCTTGTTG	2IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:37	ZP:Z:AAC	CQ:Z:@>BAAA=B?A@@B@B@?AB@BA@B@@@@BBBB@BA@A<@A<@=><;;?@2	CS:Z:G31011023232110100312010210222130221322002200010032");
		 data.add("1822_622_784	113	chr7	140188994	39	6H44M	=	140191589	2642	TTTGCCATTTTAAGGCCCGGAAAATGAGGTTGTCGAGTCATGCA	G@HIIIIIII>IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	MD:Z:44	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:97	ZP:Z:AAC	CQ:Z:BB@BB=<?>BB@?@B@BA>BABAAAA@@>@A<;2-@B:?/?>+62;B&)(	CS:Z:T01313121223211010221300020300302030003103100013020");
		 data.add("1822_622_784	177	chr7	140191589	39	9H41M	=	140188994	-2642	GACCCAAATTGGTAATAACCAAAACTGTCCATGTTGGTCCT	?:9=I'&&&IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	MD:Z:6C0C33	RG:Z:20110221052813657	NH:i:1	CM:i:2	NM:i:2	SM:i:47	ZP:Z:AAC	CQ:Z:BBBABAAABBAAAAA?B?ABA@<<BBB=AB;B=&,'79%5&:;%4%4>-(	CS:Z:G22021010113102112100010103303101030010012101111000");
		 data.add("874_1001_370	113	chr7	140189005	63	50M	=	140191611	2656	AAGGCCCGGAAAATGAGGTTGTCGAGTCATGCACAAATGTTGCCTGTAAT	BIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAC	CQ:Z:BABBBBBABAAB:BBBBAABB@<BBAB6BBBB/BA>B2BBAA;?BBA?AB	CS:Z:T33031120310113001113131212232110102213000203003020");
		 data.add("874_1001_370	177	chr7	140191611	63	50M	=	140189005	-2656	AACTGTCCATGTTGGTCCTTTGTCCAGGATCTGTGACATTCTGAACTATT	>IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	MD:Z:25T24	RG:Z:20110221052813657	NH:i:1	CM:i:2	NM:i:1	SM:i:97	ZP:Z:AAC	CQ:Z:BBBBBBBBBBBBBBBABABABBBBB@BBB>@BBB@ABB@?B<B>>B5A@>	CS:Z:G20332102122031121112232021021100202101011310211210");
		 data.add("2134_481_267	129	chr7	140189059	56	50M	=	140191509	2500	TAGCCCATAAGTGAGCTTGGAGCTTGAGGAATTTAAACTTCTGCTTTATT	IIIII%%<BI9:DIIIIIIIE5>II&&IIIIGIIII:CII20BIIICFI@	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:3	NM:i:0	SM:i:86	ZP:Z:AAC	CQ:Z:?@@>?9%8%>1)23<?6:;@60&9@=&3<;@-;>=@2);@2303;=134@	CS:Z:G13230033302112232010223201020203003001202013200330");
		 data.add("2134_481_267	65	chr7	140191509	56	50M	=	140189059	-2500	TTGGACTGCATGCTGCTGTCTAGAGCTTTCTCAATGGACCTGGAACTTTA	IIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIDHIII((III**IIIIIA	MD:Z:14A35	RG:Z:20110221052813657	NH:i:1	CM:i:4	NM:i:1	SM:i:54	ZP:Z:AAC	CQ:Z:BB=7;==@884><-?B<@0<;>;78A>A1@;>;*?A?>(<@=@*@?8A6A	CS:Z:T00102121313132132112232223200222103102002100012003");

        return data;
    }
	
	 private static List<String>  createHiseqAACSamBody() {
		 final List<String> data = new ArrayList<>();			 																												 												
		 data.add("HWI-ST1240:47:D12NAACXX:7:1112:14008:49131	97	chr10	89700049	37	101M	=	89712348	12400	TAAAAAATAGCCGGGCATGGTGTCACGTGCCTGTAGTTCCAGCTGCTTGGGAGGCTGAGGTGGGAGGATTGCCAGAGCCTGGGAGGTTGAGGCTGCAGTGA	CCCFFFFFHHHHHJIJJJJJFHHIGIJIIIIIIGIJJJJJJJJJJIIJIIJIJJHHHHFF@DCDBDDDDDDDDDDDDDDDDDBBDD?BDDDDDDDBCA>AA	X0:i:1	X1:i:0	ZC:i:15	MD:Z:101	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:0	SM:i:37	XM:i:0	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:7:1112:14008:49131	145	chr10	89712348	37	101M	=	89700049	-12400	CACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAAGCAATGAA	CDDDEEECEEFFFFFFHHHGHJJJJJJIIJJJJJIJJJJJJJIJJJJIJJIIIGIIJJJJJJJJIIIJIJJJJIJIJJJJJJJJJJJIHHHHHFFFFFCCC	X0:i:1	X1:i:0	ZC:i:15	MD:Z:101	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:0	SM:i:37	XM:i:0	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:4:1304:9295:38861	97	chr10	89700053	37	101M	=	89712340	12388	AAATAGCCGGGCATGGTGTCACGTGCCTGTAGTTCCAGCTGCTTGGGAGGCTGAGGTGGGAGGATTGCCAGAGCCTGGGAGGTTGAGGCTGCAGTGAGCCA	CCCFFFFFHGHHHJJJGIJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJCHHFFFDDDEDDDDDDDCDDDDDDDD<ACDDDDDDDDDCDDDDDD	X0:i:1	X1:i:0	ZC:i:8	MD:Z:101	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:0	SM:i:37	XM:i:0	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:4:1304:9295:38861	145	chr10	89712340	37	101M	=	89700053	-12388	AAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAA	DDDDDDDDEEEFFFFFHHHHHHJJIJJJJJJJJIJJIJJJJJJJJJIJIJIJJJIJJJJJIJJJJJJJIJJJJJIIJJJJJJJJJJJJHHHHHFFFFFCCC	X0:i:1	X1:i:0	ZC:i:8	MD:Z:0C100	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:1	SM:i:37	XM:i:1	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:7:1309:3779:55661	97	chr10	89700060	37	101M	=	89712340	12381	CGGGCATGGTGTCACGTGCCTGTAGTTCCAGCTGCTTGGGAGGCTGAGGTGGGAGGATTGCCAGAGCCTGGGAGGTTGAGGCTGCAGTGAGCCATGATCAC	CCCFFFFFHHHHHJJJJJJJJJJJJJJJJJJJIJJJJJJJIJJJJJHJJEHIJIJIJHHHHHHFFFFEEEDDDDDDBDDDDDDDDCDCDDDDDDDDDDDED	X0:i:1	X1:i:0	ZC:i:15	MD:Z:101	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:0	SM:i:37	XM:i:0	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:6:2109:5161:48848	97	chr10	89700064	37	101M	=	89712346	12383	TATGGTGTCACGTGCCTGTAGTTCCAGCTGCTTGGGAGGCTGAGGTGGGAGGATTGCCAGAGCCTGGGAGGTTGAGGCTGCAGTGAGCCATGATCACACCA	CCCFFDFFHGHHHJIJJIJJJIJJJJIJJJJIJIIJJIJJJIIIF?FHIFHJIGJIIGIIHHGHFFFBABB1?CDDDDDDDDDCCC@ACCDDDCCDDDDD8	X0:i:1	X1:i:0	ZC:i:13	MD:Z:0C100	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:1	SM:i:37	XM:i:1	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:7:1309:3779:55661	145	chr10	89712340	37	101M	=	89700060	-12381	AAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAA	DDDDCDDDECCFFFFFHHHHHHJJJJIJJJJJIIJJJJJJJJIJJJJIJIIJJJIJJJJJJJJJIJJJJJJJJIJJJJJJJJJJJJJJHHHHHFFFFFCCC	X0:i:1	X1:i:0	ZC:i:15	MD:Z:0C100	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:1	SM:i:37	XM:i:1	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:8:2107:8195:5758	97	chr10	89700084	37	101M	=	89712340	12357	GTTCCAGCTGCTTGGGAGGCTGAGGTGGGAGGATTGCCAGAGCCTGGGAGGTTGAGGCTGCAGTGAGCCATGATCACACCACAGCACTCTAGCCTAGAGCC	@@@FFDFDFFFHHJJJHHJIJJBGI*@FH?FACGIGGGIJIIIIGJHH@CHHGGCHHHFFBEE;>>AAACAC@CA>:>A@BDDDBCDCCCDCDCDCD@CC?	X0:i:1	X1:i:0	ZC:i:12	MD:Z:101	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:0	SM:i:37	XM:i:0	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:8:2107:8195:5758	145	chr10	89712340	37	101M	=	89700084	-12357	AAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAA	CCDCCABDCCCDBBDDHHHEAHIGEIJIHEGHADGIGGHGIHEHDJJIGGGGGGHGGGFIIGIGGH>CJJIHHCIGHDFBIFIFCGIIHHHHGEDBDD?@@	X0:i:1	X1:i:0	ZC:i:12	MD:Z:0C100	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:1	SM:i:37	XM:i:1	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:6:2109:5161:48848	145	chr10	89712346	37	101M	=	89700064	-12383	TCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAATGGTGACAAAGCAATG	DDDCCEDEEEFFFFFFHHHHHHJIJJJIJIGJIJJJIIJIJJJIIIJJJIHJIJJJJJJJJIJJIIHGIJJJJIJJJJJJJJIJJJJJHHHHGFFFFFCCC	X0:i:1	X1:i:0	ZC:i:13	MD:Z:101	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:37	NM:i:0	SM:i:37	XM:i:0	XO:i:0	ZP:Z:AAC	XT:A:U");
	     data.add("HWI-ST1240:47:D12NAACXX:1:2307:8115:32717	147	chr10	89700257	29	43M58S	=	89700054	-246	GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG	CDDDEDCCCC@DDEFFFFFFHHHHHHHJIIHJJJJJJJJJIJJJJJJJIIGGJJJJJJJJJJJJJJJIGGJJJJJJJJJJIJJJJJJJHHHHHFFFFFCCC	ZC:i:9	MD:Z:43	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
	     data.add("HWI-ST1240:47:D12NAACXX:6:2301:10241:71660	147	chr10	89700213	29	87M14S	=	89699990	-310	ATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTG	CCCDDCDEDEEEEDFEC>FECHHHJJIHIJJHGIIJIIJJIJJJIIJJIHHJJHGJJIJJJJJJJIJJJJJJJJJJIJJJJJJJJJJJHHHHHFFFFFCCC	ZC:i:13	MD:Z:87	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
	     data.add("HWI-ST1240:47:D12NAACXX:4:1110:20608:86188	147	chr10	89700241	29	59M42S	=	89700052	-248	CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC	<BDC@DDEEEECECDFCFFFHHHHHFHJJIIIIGGIJJIJJIGIJJJJJIHJJJIJJJJJIIIJJIGHJJJJJJJJJJJJJJJJIHJJHHHHHFFFFFCCC	ZC:i:8	MD:Z:59	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
	     data.add("HWI-ST1240:47:D12NAACXX:7:2210:12278:86346	163	chr10	89712341	29	10S91M	=	89712530	290	ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT	BCCFFFDFFHHHHJJGHJIJJJJFIJJJHIJJJJJJJJJIJJJJJJJJJJHIJJJJJIJJJJJJJJJHHHHHHHFFFFFFFCEEEEDD@CDEDDDDDDEDD	ZC:i:15	MD:Z:91	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
	     data.add("HWI-ST1240:47:D12NAACXX:5:2311:7722:24906	163	chr10	89712341	29	31S70M	=	89712504	264	TCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA	BCCFFFFFHHHHGIIJJJJJJJJIJJJHIGIJIHIJCGHGHJIJGHHEGFHHFIIJIIJJJIJJIIFHIIJFIJJHHFFFCEEEEECDDDDDEEDDD>CCD	ZC:i:14	MD:Z:70	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
	     data.add("HWI-ST1240:47:D12NAACXX:4:2105:19785:71299	163	chr10	89712341	29	59S42M	=	89712539	299	AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGG	CCCFFFFFHHHHHIJJJJJJHGHJJJJJJJJJJJJJGHHIIJJJJJJJJIIIJJJIJIIJJJJIEGHIJJIJJIJIJHGFHHHFFFFFEEEEEDEDDDDDD	ZC:i:8	MD:Z:42	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
	     data.add("HWI-ST1240:47:D12NAACXX:4:2105:19785:71299	163	chr10	89712341	29	59S42M	chrX	12539	0	AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGG	CCCFFFFFHHHHHIJJJJJJHGHJJJJJJJJJJJJJGHHIIJJJJJJJJIIIJJJIJIIJJJJIEGHIJJIJJIJIJHGFHHHFFFFFEEEEEDEDDDDDD	ZC:i:8	MD:Z:42	RG:Z:20110221052813657	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M");
	     return data;
	}

	 public static SAMFileHeader createSamHeaderObject(SortOrder sort) {
		 SAMSequenceDictionary dict = new SAMSequenceDictionary(
				 Arrays.asList(new SAMSequenceRecord("chr1",249250621),
						 new SAMSequenceRecord("chr4",191154276),
						 new SAMSequenceRecord("chr7",159138663),
						 new SAMSequenceRecord("chr10",135534747),
						 new SAMSequenceRecord("chr19",59128983),
						 new SAMSequenceRecord("chrX",155270560),
						 new SAMSequenceRecord("chrY",59373566),
						 new SAMSequenceRecord("GL000191.1",106433),
						 new SAMSequenceRecord("GL000211.1",166566),
						 new SAMSequenceRecord("chrMT",16569)));
		 SAMFileHeader h = new SAMFileHeader();
		 h.setSequenceDictionary(dict);
		 h.setSortOrder(sort);
		 
		 SAMReadGroupRecord rg1 = new SAMReadGroupRecord("20110221052813657");
		 rg1.setPlatform("SOLiD");
		 rg1.setLibrary("Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}");
		 rg1.setAttribute("ZC", "Z:1:S0049_20100000_1_LMP");
		 SAMReadGroupRecord rg2 = new SAMReadGroupRecord("20110221052813667");
		 rg2.setPlatform("SOLiD");
		 rg2.setLibrary("Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}");
		 rg2.setAttribute("ZC", "Z:1:S0049_20100000_2_LMP");
		 
		 h.setReadGroups(Arrays.asList(rg1, rg2));
		 return h;
	 }

		public static List<String> createSamHeader(SortOrder sort) {
	        final List<String> data = new ArrayList<>();
	        data.add("@HD	VN:1.0	GO:none	SO:"+ sort.name());
	        data.add("@SQ	SN:chr1	LN:249250621	");
	        data.add("@SQ	SN:chr4	LN:191154276	");
	        data.add("@SQ	SN:chr7	LN:159138663	");
	        data.add("@SQ	SN:chrX	LN:155270560	");
	        data.add("@SQ	SN:chrY	LN:59373566	");
	        data.add("@SQ	SN:chr19	LN:59128983	");
	        data.add("@SQ	SN:GL000191.1	LN:106433	");
	        data.add("@SQ	SN:GL000211.1	LN:166566	");
	        data.add("@SQ	SN:chrMT	LN:16569	");
	        data.add("@RG	ID:20110221052813657	PL:SOLiD	PU:bioscope-pairing	LB:Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}	DT:2011-02-21T15:28:13+1000	SM:S1	ZC:Z:1:S0049_20100000_1_LMP");
	        data.add("@RG	ID:20110221052813667	PL:SOLiD	PU:bioscope-pairing	LB:Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}	DT:2011-02-21T15:28:13+1000	SM:S1	ZC:Z:1:S0049_20100000_2_LMP");
	        
	        return data;
	    }
		
		public static List<SAMRecord> createSamBodyRecords(SortOrder so) {
			SAMFileHeader h = createSamHeaderObject(so);
			String rg = "20110221052813657";
			SAMRecord s1 = getSAM(h, "254_166_1407", 129, "chr7",140191044,  63, "50M", "chr7", 140188379,  -2715, "ACTCCATTTCTAGAAAAAAATTAGAAAATTAACTGGAACCAGGAGAGGTG", "IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIHIIIIIII@", "18C31", rg,"ABC", 1);
			SAMRecord s2 = getSAM(h, "254_166_1407",	65	,"chr7",	140188379,	63,	"50M","chr7",140191044	,2715	, "ACGGCTCATGTCTCCTTAGAATGTATAAAAGCAAGCTGTGCTCTGACCAC","IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIA","40A9", rg, "ABC", 1);
			SAMRecord s3 = getSAM(h, "736_1100_1853",	97	,"chr7",	140188275,	69	,"50M","chr4",85925068	,0	, "TATGACCTGGAAAACTCCTCCCGGCTTGGAATCTTCCCTTTTTTGGCTTC","IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII7","50", rg, "C**", 1);
			SAMRecord s4 = getSAM(h, "736_1100_1853",	145	,"chr4",	85925068,	69,	"50M","chr7",	140188275,	0	, "TATGACCTGGAAAACTCCTCCCGGCTTGGAATCTTCCCTTTTTTGGCTTC","IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII7","50", rg, "C**", 1);
			SAMRecord s5 = getSAM(h, "204_1749_420",	65	,"chrX",	6448103,	2	,"47M3H","chr7",	140190996	,0	, "GATCGCTTGAAGCCAGGAGCTCAAGACCAGCCTGGGCAATATAGCAA","IIIIIIIHIIIIIIIIIBIIIIIIIII@IIIIBIIIII?FII7IIIF","47",rg,"C**",2);
			SAMRecord s6 = getSAM(h, "204_1749_420",	65	,"chr7",	140190996,	2	,"47M3H","chrX",	6448103	,0	, "GATCGCTTGAAGCCAGGAGCTCAAGACCAGCCTGGGCAATATAGCAA","IIIIIIIHIIIIIIIIIBIIIIIIIII@IIIIBIIIII?FII7IIIF","47",rg,"C**",2);
			SAMRecord s7 = getSAM(h, "204_1749_421",	65	,"chrY",	6448103,	2	,"47M3H","chrX",	130833637	,0	, "GATCGCTTGAAGCCAGGAGCTCAAGACCAGCCTGGGCAATATAGCAA","IIIIIIIHIIIIIIIIIBIIIIIIIII@IIIIBIIIII?FII7IIIF","47",rg,"C**",2);
			SAMRecord s8 = getSAM(h, "204_1749_421",	65	,"chrX",	130833637,	2,	"47M3H","chrY",	6448103	,0	, "GATCGCTTGAAGCCAGGAGCTCAAGACCAGCCTGGGCAATATAGCAA","IIIIIIIHIIIIIIIIIBIIIIIIIII@IIIIBIIIII?FII7IIIF","47",rg,"C**",2);
			SAMRecord s9 = getSAM(h, "1789_1456_806",	129	,"chr7",	140188227,	69,	"50M","chr7",	140191179,	3002, "GGCCAATCAGAAACTCAAAAGAATGCAACCATTTGCCTGTTATCTACCTA","IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII@","50",rg,"AAC",1);
			SAMRecord s10 = getSAM(h, "1789_1456_806",	65	,"chr7",	140191179,	69,	"50M","chr7",	140188227	,-3002	, "ATGGCAAAACCCTGTCTCATTCCTTCAATCCTAGCACTTTGGGAGGCTGA","IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII@","50",rg,"AAC",1);
			SAMRecord s11 = getSAM(h, "1911_1919_2005",	129	,"chr19",	12241065,	61,	"50M","chr19",	12241090,	75	, "AGTTAAGGTTTGGTCAGTCAGTGATTGCTTCTATGTCTGCTGGGACTGGG","=56EI=2:II=6?IIF<<IIE7;IIH8=D;<75DII86<BIIIIIIAHI2","50",rg,"AAB",1);
			SAMRecord s12 = getSAM(h, "1911_1919_2005",	65,"chr19",	12241090,	61	,"47M3H","chr19",	12241065,	-75	, "TGCTTCTATGTCTGCTGGGACTGGGATTTTTACTCTAGGGAGACTGA","IIIIIIIIIIIIFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII","47",rg,"AAB",1);
			
			List<SAMRecord> recs = Arrays.asList(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11, s12);
			
			
			if (so.equals(SortOrder.coordinate)) {
				recs.sort(new SAMRecordCoordinateComparator());
			} else if (so.equals(SortOrder.queryname)) {
				recs.sort(new SAMRecordQueryNameComparator());
			}
			return recs;
			
		}

	    public static List<String> createSamBody() {
	        final List<String> data = new ArrayList<>();

	        // mate pairs
	        data.add("254_166_1407	129	chr7	140191044	63	50M	chr7	140188379	-2715	ACTCCATTTCTAGAAAAAAATTAGAAAATTAACTGGAACCAGGAGAGGTG	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIHIIIIIII@	MD:Z:18C31	RG:Z:20110221052813657	NH:i:1	CM:i:2	NM:i:1	SM:i:97	ZP:Z:ABC	CQ:Z:BBBBBBBBABB>>7AA:@??B??;>@?>5>@@B@@?1:@?=81<::>=?@	CS:Z:T31220130022322000000303220003030121020101202222011");
	        data.add("254_166_1407	65	chr7	140188379	63	50M	chr7	140191044	2715	ACGGCTCATGTCTCCTTAGAATGTATAAAAGCAAGCTGTGCTCTGACCAC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIA	MD:Z:40A9	RG:Z:20110221052813657	NH:i:1	CM:i:2	NM:i:1	SM:i:97	ZP:Z:ABC	CQ:Z:@B3B?BBAAB@B<;@>B5B>AB@AAAB;@@BB:A@A<>B=B@66>B;;=A	CS:Z:G21303221311222020322031133300023102321113222121011");

	        // different chromosome for second mate
	        data.add("736_1100_1853	97	chr7	140188275	69	50M	chr4	85925068	0	TATGACCTGGAAAACTCCTCCCGGCTTGGAATCTTCCCTTTTTTGGCTTC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII7	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:C**	CQ:Z:BBBBABBAB?BAB?BBBB@@BBBBBBBBBAABBAAABBBA@BBB;5A??7	CS:Z:T03312102102000122022003032010203220200200000103202");
	        data.add("736_1100_1853	145	chr4	85925068	69	50M	chr7	140188275	0	TATGACCTGGAAAACTCCTCCCGGCTTGGAATCTTCCCTTTTTTGGCTTC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII7	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:C**	CQ:Z:BBBBABBAB?BAB?BBBB@@BBBBBBBBBAABBAAABBBA@BBB;5A??7	CS:Z:T03312102102000122022003032010203220200200000103202");

	        // X/Y chromosome for second mate
	        data.add("204_1749_420	65	chrX	6448103	2	47M3H	chr7	140190996	0	GATCGCTTGAAGCCAGGAGCTCAAGACCAGCCTGGGCAATATAGCAA	IIIIIIIHIIIIIIIIIBIIIIIIIII@IIIIBIIIII?FII7IIIF	MD:Z:47	RG:Z:20110221052813657	NH:i:2	CM:i:0	NM:i:0	SM:i:28	ZP:Z:C**	CQ:Z:<BBBB=B+>=BA=@0BB039>B0?=B>-46@B+84:B7;%BB*.>/B%63	CS:Z:T12323320120230120223221022101230210031033332310021");
	        data.add("204_1749_420	65	chr7	140190996	2	47M3H	chrX	6448103	0	GATCGCTTGAAGCCAGGAGCTCAAGACCAGCCTGGGCAATATAGCAA	IIIIIIIHIIIIIIIIIBIIIIIIIII@IIIIBIIIII?FII7IIIF	MD:Z:47	RG:Z:20110221052813657	NH:i:2	CM:i:0	NM:i:0	SM:i:28	ZP:Z:C**	CQ:Z:<BBBB=B+>=BA=@0BB039>B0?=B>-46@B+84:B7;%BB*.>/B%63	CS:Z:T12323320120230120223221022101230210031033332310021");

	        data.add("204_1749_421	65	chrY	6448103	2	47M3H	chrX	130833637	0	GATCGCTTGAAGCCAGGAGCTCAAGACCAGCCTGGGCAATATAGCAA	IIIIIIIHIIIIIIIIIBIIIIIIIII@IIIIBIIIII?FII7IIIF	MD:Z:47	RG:Z:20110221052813657	NH:i:2	CM:i:0	NM:i:0	SM:i:28	ZP:Z:C**	CQ:Z:<BBBB=B+>=BA=@0BB039>B0?=B>-46@B+84:B7;%BB*.>/B%63	CS:Z:T12323320120230120223221022101230210031033332310021");
	        data.add("204_1749_421	65	chrX	130833637	2	47M3H	chrY	6448103	0	GATCGCTTGAAGCCAGGAGCTCAAGACCAGCCTGGGCAATATAGCAA	IIIIIIIHIIIIIIIIIBIIIIIIIII@IIIIBIIIII?FII7IIIF	MD:Z:47	RG:Z:20110221052813657	NH:i:2	CM:i:0	NM:i:0	SM:i:28	ZP:Z:C**	CQ:Z:<BBBB=B+>=BA=@0BB039>B0?=B>-46@B+84:B7;%BB*.>/B%63	CS:Z:T12323320120230120223221022101230210031033332310021");

	        // first and second mate for AAC
	        data.add("1789_1456_806	129	chr7	140188227	69	50M	chr7	140191179	3002	GGCCAATCAGAAACTCAAAAGAATGCAACCATTTGCCTGTTATCTACCTA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII@	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:43	ZP:Z:AAC	CQ:Z:BBBBBBBBBBBBABBBABABABABBBBBA?>BBBB<BABB<A@@<?7=@@	CS:Z:G00301032122001221000220313101013001302110332231023");
	        data.add("1789_1456_806	65	chr7	140191179	69	50M	chr7	140188227	-3002	ATGGCAAAACCCTGTCTCATTCCTTCAATCCTAGCACTTTGGGAGGCTGA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII@	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAC	CQ:Z:BBABBBBAB@BBA?BBBA?BBBA@AB=AB@@BA;@BB<@@B??@B9B>B@	CS:Z:T33103100010021122213020202103202323112001002203212");

	        // mate pairs	        
	        data.add("1911_1919_2005	129	chr19	12241065	61	50M	=	12241090	75	AGTTAAGGTTTGGTCAGTCAGTGATTGCTTCTATGTCTGCTGGGACTGGG	=56EI=2:II=6?IIF<<IIE7;IIH8=D;<75DII86<BIIIIIIAHI2	MD:Z:50	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAB	CQ:Z:1-).84*)295).2=34)499-+1<81(6/-0(.774%2+88993;5-<2	CS:Z:G22103020100101212121211230132022331122132100212100");
	        data.add("1911_1919_2005	65	chr19	12241090	61	47M3H	=	12241065	-75	TGCTTCTATGTCTGCTGGGACTGGGATTTTTACTCTAGGGAGACTGA	IIIIIIIIIIIIFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	MD:Z:47	RG:Z:20110221052813657	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAB	CQ:Z:295:8@B>B<<B8/=@?7?>B<==57B=A=7<39@?A79=?</<A+B:%=	CS:Z:T01320223311221321002121002300003122232002221212203");
	        return data;
	    }
	    
		public static String createTmpClusterFile(String dirName, PairClassification pc, String fileName) throws IOException {
			
			File testDir = new File(dirName + pc.getPairingClassification());
			testDir.mkdir();
			String outFile = testDir + FILE_SEPERATOR + fileName;
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outFile)))) {
			
				if (pc.equals(PairClassification.AAC)) {
					writeAACPairs(writer);
				} 
				if (pc.equals(PairClassification.Cxx)) {
					writeCxxPairs(writer);
				}
			}
			
			return outFile;
		}
		
		public static void writeAACPairs(BufferedWriter writer) throws IOException {	
			
			writer.write("254_166_1407:20110221052813657,chr7,140188379,140188428,AAC,129,false,254_166_1407:20110221052813657,chr7,140191044,140191093,AAC,65,false,F2F1," + NEWLINE);
			writer.write("1789_1456_806:20110221052813657,chr7,140188227,140188276,AAC,129,false,1789_1456_806:20110221052813657,chr7,140191179,140191228,AAC,65,false,F2F1," + NEWLINE);
			writer.write("515_451_1845:20110221052813657,chr7,140188449,140188498,AAC,129,false,515_451_1845:20110221052813657,chr7,140191238,140191287,AAC,65,false,F2F1" + NEWLINE);
			writer.write("1887_329_319:20110221052813657,chr7,140188962,140189009,AAC,113,true,1887_329_319:20110221052813657,chr7,140191372,140191421,AAC,177,true,R1R2" + NEWLINE);
			writer.write("690_397_1054:20110221052813657,chr7,140188962,140189008,AAC,113,true,690_397_1054:20110221052813657,chr7,140191394,140191443,AAC,177,true,R1R2" + NEWLINE);
			writer.write("1822_622_784:20110221052813657,chr7,140188994,140189037,AAC,113,true,1822_622_784:20110221052813657,chr7,140191589,140191629,AAC,177,true,R1R2" + NEWLINE);
		}
		
		public static void writeCxxPairs(BufferedWriter writer) throws IOException {
			writer.write("722_126_792:20110412030837875,chr4,38243635,38243684,Cxx,129,false,722_126_792:20110412030837875,chr15,63771542,63771585,Cxx,65,false,F2F1" + NEWLINE);
			writer.write("1553_1092_632:20110412030837875,chr4,38243644,38243693,Cxx,129,false,1553_1092_632:20110412030837875,chr15,63771201,63771249,Cxx,65,false,F2F1" + NEWLINE);
			writer.write("1457_1817_310:20110412030837875,chr4,38243665,38243714,Cxx,129,false,1457_1817_310:20110412030837875,chr15,63770882,63770926,Cxx,65,false,F2F1" + NEWLINE);
			writer.write("1345_419_238:20110412030837875,chr4,38243701,38243744,Cxx,113,true,1345_419_238:20110412030837875,chr15,63770958,63771007,Cxx,177,true,R1R2" + NEWLINE);
			writer.write("1247_1808_1080:20110412030837875,chr4,38243702,38243751,Cxx,113,true,1247_1808_1080:20110412030837875,chr15,63771179,63771228,Cxx,177,true,R1R2" + NEWLINE);
			writer.write("1370_909_596:20110412030837875,chr4,38243723,38243764,Cxx,113,true,1370_909_596:20110412030837875,chr15,63771228,63771273,Cxx,177,true,R1R2" + NEWLINE);
			writer.write("1133_82_1886:20110412030837875,chr4,38243732,38243781,Cxx,129,false,1133_82_1886:20110412030837875,chr15,63771252,63771298,Cxx,65,false,F2F1" + NEWLINE);
		}

		public static List<MatePair> readInMatePairs(File matePairsFile) throws IOException {
			List<MatePair> pairs = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(new FileReader(matePairsFile))) {
				String line = reader.readLine();
				while (line != null) {
					pairs.add(new MatePair(line));
					
					line = reader.readLine();
				}
			}
			pairs.sort(new MatePair.ReadMateLeftStartComparator());
			return pairs;
		}

	public static QSVCluster setupQSVCluster(PairGroup zp, String clusterType,TemporaryFolder testFolder, String chr1, String chr2, boolean isGermline, boolean isSingleSide) throws IOException, Exception {
		DiscordantPairCluster cluster = setupSolidCluster(zp, clusterType, testFolder, chr1, chr2);
		SoftClipCluster clip = setUpClipRecord(chr1, chr2, isGermline, isSingleSide);
		
		if (isSingleSide) {
			QSVCluster record = new QSVCluster(clip, "test");
			record.addQSVClipRecord(clip);
			record.setGermline(isGermline);
			return record;
		} else {
			QSVCluster record = new QSVCluster(cluster, false, "test");
			record.addQSVClipRecord(clip);
			record.setGermline(isGermline);
			return record;
		}			
	}

		public static SoftClipCluster setUpClipRecord(String chr1, String chr2, boolean isGermline, boolean isSingleSide) throws Exception {
			if (isSingleSide) {
				Breakpoint b = getBreakpoint(false, isGermline, 20, false);	
		        String value2 = "46\t0\t0\t2\t0\t0\t0\t0\t-\tchr10-89700299-false-neg\t66\t0\t48\tchr10\t135534747\t89712340\t89712388\t1\t48,\t18,\t89712340,";
				b.setMateBreakpoint(89712341);
				b.setMateReference("chr10");
				b.setMateStrand(QSVUtil.MINUS);
				b.setBlatRecord(new BLATRecord(value2.split("\t")));
				return new SoftClipCluster(b);
			} else {
				return new SoftClipCluster(getBreakpoint(true, isGermline, 20, false), getBreakpoint(false, isGermline, 20, false));
			}		
		}

		
		public static Breakpoint getBreakpoint(boolean isLeft, boolean isGermline, int consensus, boolean nCount) throws Exception {
			
			Breakpoint breakpoint; 
			
			Set<Clip> clips;
			if (isLeft) {
				clips = nCount? LEFT_CLIPS_TRUE : LEFT_CLIPS_FALSE;	
				breakpoint = new Breakpoint(89712341, "chr10", isLeft, consensus, 50);
								
			} else {
				clips=nCount ? RIGHT_CLIPS_TRUE : RIGHT_CLIPS_FALSE;
				breakpoint = new Breakpoint(89700299, "chr10", isLeft, consensus, 50);
			}
			for (Clip c : clips) {
				breakpoint.addTumourClip(c);
			}
			if (isGermline) {
				if (isLeft) {
					for (Clip c : LEFT_CLIPS_FALSE) {
						breakpoint.addNormalClip(c);
					}
				} else {
					for (Clip c : RIGHT_CLIPS_FALSE) {
						breakpoint.addNormalClip(c);
					}
				}
			}	
			breakpoint.defineBreakpoint(3, false, null);
			if (isLeft) {
				String value2 = "46\t0\t0\t2\t0\t0\t0\t0\t-\tchr10-89700299-false-neg\t66\t0\t48\tchr10\t135534747\t89712340\t89712388\t1\t48,\t18,\t89712340,";
				breakpoint.setBlatRecord(new BLATRecord(value2.split("\t")));
			} else {
				String value = "48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10-89712341-true-pos\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
				breakpoint.setBlatRecord(new BLATRecord(value.split("\t")));
			}
			if (breakpoint.getStrand() == QSVUtil.PLUS) {
				breakpoint.setMateStrand(QSVUtil.PLUS);
			} else {
				breakpoint.setMateStrand(QSVUtil.MINUS);
			}
			
			return breakpoint;
		}
		
		public static Breakpoint getBreakpointNoChr(boolean isLeft, boolean isGermline, int consensus) throws Exception {
			
			Breakpoint breakpoint; 
			
			Set<Clip> clips;
			if (isLeft) {
				clips = LEFT_CLIPS_FALSE;
				breakpoint = new Breakpoint(89712341, "10", isLeft, consensus, 50);
				
			} else {
				clips=RIGHT_CLIPS_FALSE;
				breakpoint = new Breakpoint(89700299, "10", isLeft, consensus, 50);
			}
			for (Clip c : clips) {
				breakpoint.addTumourClip(c);
			}
			if (isGermline) {
				if (isLeft) {
					for (Clip c : LEFT_CLIPS_FALSE) {
						breakpoint.addNormalClip(c);
					}
				} else {
					for (Clip c : RIGHT_CLIPS_FALSE) {
						breakpoint.addNormalClip(c);
					}
				}
			}	
			breakpoint.defineBreakpoint(3, false, null);
			if (isLeft) {
				String value2 = "46\t0\t0\t2\t0\t0\t0\t0\t-\tchr10-89700299-false-neg\t66\t0\t48\tchr10\t135534747\t89712340\t89712388\t1\t48,\t18,\t89712340,";
				breakpoint.setBlatRecord(new BLATRecord(value2.split("\t")));
			} else {
				String value = "48\t1\t0\t0\t2\t0\t3\t0\t+\tchr10-89712341-true-pos\t66\t0\t48\tchr10\t135534747\t89700251\t89700299\t1\t48,\t0,\t89700251,";
				breakpoint.setBlatRecord(new BLATRecord(value.split("\t")));
			}
			if (breakpoint.getStrand() == QSVUtil.PLUS) {
				breakpoint.setMateStrand(QSVUtil.PLUS);
			} else {
				breakpoint.setMateStrand(QSVUtil.MINUS);
			}
			
			return breakpoint;
		}


		private static Set<Clip> getRightClips(boolean nClips) {
			Set<Clip> clips = new HashSet<>(12);
			try {
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10,89700299,-,right,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG") + ",GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:6:2301:10241:71660:20120608110941621,chr10,89700299,-,right,ATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTG," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTG") + ",ATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:1110:20608:86188:20120608092353631,chr10,89700299,-,right,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC") + ",CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:1:1204:3577:34360:20120608115535190,chr10,89700299,-,right,AGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGC," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGC") + ",AGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:5:2113:4661:50103:20120607102754932,chr10,89700299,-,right,GATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGT," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGT") + ",GATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:1114:3101:51165:20120608092353631,chr10,89700299,-,right,TCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGG," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGG") + ",TCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
			} catch (QSVException e) {
				//do nothing
			}
			return clips;
		}
		
		public static Clip getClip(String strand, String side, int position, String name)  {
			
			String line = name + ",chr10,"+position+","+strand+","+side+",ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAACTGN,ACTGN,ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACA";
			try {
				return new Clip(line);
			} catch (QSVException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		public static Clip getClip(String strand, String side)  {
			return getClip(strand, side, 89700299, "HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190");
		}
		
		public static Clip getClip(String strand, String side, int position)  {
			return getClip(strand, side, position, "HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190");
		}
		
		public static Clip getClip(String strand, String side, String name) {
			return getClip(strand, side, 89700299, name);
		}

		private static Set<Clip> getLeftClips(boolean nClips)  {
			Set<Clip> clips = new HashSet<>(10);
			try {
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:7:2210:12278:86346:20120608113919562,chr10,89712341,+,left,ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT," + (nClips ? "NNNNNNNNNNNNNNN" : "ACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:5:2311:7722:24906:20120607102754932,chr10,89712341,+,left,TCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA," + (nClips ? "NNNNNNNNNNNNNNN" : "TCTCTTTGTGTAAGAGATTATACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:2105:19785:71299:20120608092353631,chr10,89712341,+,left,AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGG," + (nClips ? "NNNNNNNNNNNNNNN" : "AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGG"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:7:2305:11547:56681:20120608113919562,chr10,89712341,+,left,ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT," + (nClips ? "NNNNNNNNNNNNNNN" : "ACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT"));
				clips.add(new Clip("HWI-ST1240:47:D12NAACXX:8:2107:14006:55890:20120608020343585,chr10,89712341,+,left,TGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGG," + (nClips ? "NNNNNNNNNNNNNNN" : "TGTAAGAGATTATACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGG"));
			} catch (QSVException e) {
				//do nothing
			}
			return clips;
		}

	public static DiscordantPairCluster setupHiseqCluster(String clusterType, TemporaryFolder testFolder, String qPrimerCategory) throws IOException, Exception {
		  List<MatePair> pairs = new ArrayList<>();
	      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:7:1112:14008:49131:20120608113919562,chr10,89700049,89700149,AAC,97,false,HWI-ST1240:47:D12NAACXX:7:1112:14008:49131:20120608113919562,chr10,89712348,89712448,AAC,145,true,F1R2"));
	      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:4:1304:9295:38861:20120608092353631,chr10,89700053,89700153,AAC,97,false,HWI-ST1240:47:D12NAACXX:4:1304:9295:38861:20120608092353631,chr10,89712340,89712440,AAC,145,true,F1R2"));
	      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:7:1309:3779:55661:20120608113919562,chr10,89700060,89700160,AAC,97,false,HWI-ST1240:47:D12NAACXX:7:1309:3779:55661:20120608113919562,chr10,89712340,89712440,AAC,145,true,F1R2"));
	      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:6:2109:5161:48848:20120608110941621,chr10,89700064,89700164,AAC,97,false,HWI-ST1240:47:D12NAACXX:6:2109:5161:48848:20120608110941621,chr10,89712346,89712446,AAC,145,true,F1R2"));
	      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:8:1109:11792:69957:20120608020343585,chr10,89700200,89700300,AAC,161,false,HWI-ST1240:47:D12NAACXX:8:1109:11792:69957:20120608020343585,chr10,89712446,89712546,AAC,81,true,F2R1"));
	      String tumourFile = testFolder.newFile("tumor.bam").getAbsolutePath();
		  createHiseqBamFile(tumourFile, PairGroup.AAC, SortOrder.coordinate);
//		  String normalFile = testFolder.newFile("normal.bam").getAbsolutePath();
//		  createHiseqBamFile(normalFile, PairGroup.AAC, SortOrder.coordinate);
	      QSVParameters tumor = TestUtil.getQSVParameters(testFolder, tumourFile, tumourFile, true, "both", "both");
	      QSVParameters normal = TestUtil.getQSVParameters(testFolder, tumourFile, tumourFile, false, "both", "both");    
//	      QSVParameters tumor = TestUtil.getQSVParameters(testFolder, tumourFile, normalFile, true, "both", "both");
//	      QSVParameters normal = TestUtil.getQSVParameters(testFolder, tumourFile, normalFile, false, "both", "both");    
	      String query = "Cigar_M > 35 and option_SM > 14 and MD_mismatch < 3 and Flag_DuplicateRead == false ";
	      DiscordantPairCluster cluster = new DiscordantPairCluster("chr10", "chr10", "AAC", tumor, true);	      
	
	      for (MatePair p : pairs) {
	          cluster.getClusterMatePairs().add(p);
	      }
	      
	      if ( ! clusterType.equals("somatic")) {
	    	  	cluster.getMatchedReadPairs().add(pairs.get(0));
	      }
//		      
	      cluster.setClusterEnds();
	      cluster.setNormalRange(3000);
	      cluster.finalize(tumor, normal, clusterType, 1, query, "pe", true);
	      cluster.getqPrimerCateory().setPrimaryCategoryNo(qPrimerCategory);
	      return cluster;
	}
	
	public static TIntObjectMap<int[]> getTiledAlignerCache(List<String> entriesForCache) {
		TIntObjectMap<int[]> cache = new TIntObjectHashMap<>();
		for (String s : entriesForCache) {
			int tabindex = s.indexOf(Constants.TAB);
			if (tabindex == -1) {
				System.out.println("no tab in " + s);
			}
			String tile = s.substring(0, tabindex);
			int tileInt = TiledAlignerUtil.convertTileToInt(tile);
			if (tileInt != -1) {
				int [] positions = TiledAlignerLongMap.convertStringToIntArray(s.substring(tabindex + 1));
				cache.put(tileInt, positions);
			}
		}
		return cache;
	}
	public static List<String> getStringsForTiledAlignerCache() {
		return Arrays.asList("AAAATGAGGTTGT\t83948024,110957239,115356742,177269879,238125246,244112828,261149626,269964274,283862001,287583202,303064775,306549321,307375352,331657371,349940270,354415404,382977066,390914572,411901783,4898" + 
				"44023,495481861,511409376,533991240,563262572,599849293,613835993,644065496,666219967,674421974,678554229,716369295,718579629,792337697,795797532,853603943,864394905,869739266,884532347,906953082,933947583,952" + 
				"451269,959727938,962536382,966262668,995543980,1014058635,1054868406,1111674797,1135962802,1136191759,1139213578,1149266001,1160514709,1162799185,1169787921,1204446225,1206748268,1208804926,1226349485,12541550" + 
				"04,1267170617,1273056558,1280761739,1284257102,1299118524,1310225179,1314650528,1348728273,1350326509,1373846041,1385306256,1432964550,1439921813,1463025292,1470513253,1488009321,1490046038,1495440764,14977112" + 
				"65,1517344639,1532418423,1552098798,1552693973,1582651305,1583808065,1605296002,1611320747,1622674667,1623191056,1661738293,1700422546,1700961598,1706483552,1707459367,1735789676,1759059510,1772667955,18116024" + 
				"94,1823393268,1833421779,1899292461,1904598576,1905032445,1917151525,2002534148,2044712920,2053780178,2061717731,2063016157,2077384506,2111678448,2133637926,2186294177,2187331296,2187397171,2231105590,22320660" + 
				"06,2235473900,2259070227,2276654992,2297829359,2303594015,2335519277,2355271035,2364405943,2397178832,2402564774,2417703326,2419478508,2426830783,2481510231,2493513543,2515758145,2537530987,2629583046,26369216" + 
				"36,2661678737,2716812205,2781023918,2807411293,2847628450,2849836298,2862088580,2885796447,2916319502,2923953406,2934703235,2966265210,2971766407,2978064328,2979185846,3031104533,3052776267",
				"AAAATGGCAAAGT\t21249747,39085264,44334162,62891501,101577875,102512988,102850697,117579881,118585789,168034003,171395236,174927149,178581696,189985943,195315563,196454151,201440651,235331165,235679881,2442543" + 
				"00,245155472,267112016,290107984,292350304,301351273,303277502,314138258,314175316,331335103,332413672,333129901,347802774,355419532,358063439,360319977,371157265,390131456,395593904,397909037,410059950,416109" + 
				"294,418328578,421141236,423136499,434378930,440237448,440659354,446782973,446931840,447591972,497540321,499867759,500124192,510205330,515043088,527908859,529494629,532602550,543909730,556807822,558914207,56006" + 
				"3971,587650081,594878365,596894336,616820059,617315253,630706654,632044675,635075127,651909494,673125616,679364841,681171155,714507985,717077126,727109112,735246321,746466390,756247517,756410210,757285185,7844" + 
				"45603,797181942,801883659,824645906,862667223,862955974,893903895,906302250,910492340,926007815,935746256,939153902,940238236,943000011,953322319,955721140,967247141,968503897,969025799,983079425,998965600,104" + 
				"0897443,1043448118,1047191446,1061164038,1063386262,1066507560,1072069255,1072686574,1108447825,1109859734,1112524191,1154662571,1155821639,1163630683,1181903001,1182051869,1201715634,1220446506,1222963069,126" + 
				"8922229,1278993841,1284797496,1287381901,1325418655,1330676699,1337108747,1356106246,1357345480,1357951712,1364326580,1365891454,1379841546,1387366153,1400040254,1401842077,1426069400,1434057014,1458549906,145" + 
				"8785535,1471626850,1487260739,1490242089,1496280833,1525471098,1547144613,1559571836,1564678076,1570058009,1611950083,1626636495,1654442049,1658727401,1672743974,1681880506,1682728456,1683473200,1695552780,170" + 
				"4117286,1707275530,1714476455,1727496177,1731504955,1731726609,1744039282,1776102816,1776104739,1790458832,1826376035,1853121692,1859422200,1875334542,1898442127,1919824717,1927274863,1946716310,1948285887,195" + 
				"7914534,1961265680,1972619032,1982981066,1992029493,2020972184,2023450222,2042861389,2043319992,2046127190,2046372950,2061776002,2070171556,2109736658,2112123658,2139259024,2142827957,2146712053,2148748123,215" + 
				"2730776,2162458982,2187096426,2187765775,2190505262,2190611401,2237409550,2243115770,2244416118,2246352254,2257158642,2276099008,2277744521,2279564242,2279988748,2284604916,2334288596,2335482888,2338999686,234" + 
				"2560124,2355010564,2365923051,2366502228,2369050250,2384455783,2389359587,2398852075,2404178209,2404630688,2405116901,2430528528,2430803890,2442039670,2459337482,2461220254,2477970984,2478980816,2493135350,254" + 
				"3581356,2568232710,2634416305,2636968214,2641478873,2651160603,2653311832,2657673745,2718093760,2719280630,2725170039,2733290409,2739609208,2777381180,2807714000,2813733496,2857828038,2868059140,2875199642,289" + 
				"5632787,2903406023,2917917741,2919126931,2928711816,2953712985,2974536904,2982213245,2990781288,2995193707,3004396906,3007862555,3013060161,3020789660,3027447916",
				"AAAGTACTCTGTA\t31402755,45556742,49004375,49593451,76214453,94694240,106097109,110103249,180376933,204461294,204570553,232949330,234241430,247486745,312241248,314360836,365882262,378694155,424594922,466125286" + 
				",513995995,529342181,556353275,639053734,680467730,745313498,759837365,772671247,775535412,788653471,800546977,812271810,842623165,854505841,858279083,877071094,908722013,932818125,981304487,985703536,10001567" + 
				"94,1043993766,1051756706,1051758177,1089063640,1114301883,1138686921,1160389185,1215131424,1246595124,1277200379,1311602833,1311867015,1364395952,1395205964,1521977292,1532322906,1571341348,1618432299,16518427" + 
				"11,1662690273,1726799443,1731481142,1731702911,1750509548,1778654156,1892616244,1938936662,1976820129,1978221959,1983564648,2010399122,2028497561,2044219714,2048490772,2116548553,2122455387,2132053629,21384606" + 
				"66,2164841646,2180967900,2224316365,2245926387,2372086416,2376428517,2483075081,2510743238,2534062607,2551439423,2575371073,2682179956,2712055427,2902060909,2924024394,2950498190,2982505370,3005940636,30190021" + 
				"19,3032715975", 
				"AAATGAGGTTGTC\t191870362,244112829,250710504,280421236,281949171,306549322,314968701,350680570,382977067,396345672,419393424,479327084,489844024,533991241,613835994,666219968,700156750,866593482,867343252,869" + 
				"739267,1025591662,1045783251,1096262995,1114340188,1160514710,1168339095,1205091471,1284257103,1301790786,1350326510,1373846042,1395560833,1473478782,1517344640,1532418424,1541946126,1611320748,1622674668,1904" + 
				"598577,1907468579,1923686609,1947473295,1999651984,2000715500,2013425043,2020365701,2067147543,2079554370,2138972721,2186294178,2236474413,2259070228,2327880617,2366452297,2402564775,2457951667,2629583047,2661" + 
				"678738,2724387331,2733223573,2861290425,2966265211,3031104534,3031894283",
				"AAATGGCAAAGTA\t36212954,62891502,79650725,94862412,121186680,162818084,168034004,171395237,174927150,209378015,235331166,245155473,257564993,262310577,292350305,301351274,332413673,333129902,355419533,3603199" + 
				"78,374312577,387363186,412839129,421141237,422342236,440237449,444086138,446782974,446931841,499867760,510205331,511763242,515043089,543909731,556807823,560063972,575928796,587650082,594878366,609478013,681171" + 
				"156,695113223,718032816,738770363,754563155,756247518,784445604,791290356,793346366,806252643,833793735,849053468,862667224,862955975,926007816,935746257,935919964,953186108,953515587,967247142,969025800,10092" + 
				"41237,1029719260,1046344903,1066507561,1072069256,1072686575,1086820402,1100968502,1108355993,1108447826,1145952384,1173893159,1175691279,1181903002,1182051870,1273764902,1284797497,1325418656,1330676700,13482" + 
				"69593,1348963857,1355802572,1357345481,1364326581,1365891455,1374401448,1384228347,1387366154,1401842078,1426069401,1429651689,1434057015,1454537110,1458549907,1475679018,1483586167,1484727541,1497284951,15001" + 
				"62756,1525471099,1547144614,1564678077,1570058010,1626636496,1628262116,1651391620,1695552781,1707617733,1707986191,1744039283,1776104740,1790458833,1793124447,1797615024,1801272525,1852453804,1853121693,18984" + 
				"42128,1908513911,1915391653,1936172679,1946716311,1948285888,1972619033,1992029494,2011090049,2014594359,2023450223,2028370532,2044031830,2047600366,2145887610,2156743062,2162458983,2163905622,2168621573,21718" + 
				"50675,2190611402,2199062948,2235641557,2237409551,2243115771,2278911153,2279988749,2280366243,2335482889,2338999687,2341453552,2365923052,2366502229,2403884027,2430803891,2442039671,2461220255,2476010763,24789" + 
				"80817,2544448774,2552075298,2568232711,2568887440,2623842967,2640007474,2641478874,2665019970,2732316350,2743045611,2767290458,2856014651,2917917742,2953712986,2980362289,3002176620,3004396907,3007862556,30130" + 
				"60162,3025188542",
				"AACCTCATTTTCC\t11717120,33394950,43823986,50787356,52350294,85401521,99834640,151370515,157347077,175565714,210888220,215760890,253338521,253428958,256014978,273043611,281758437,300258881,310775115,315244069," + 
				"368241020,372438250,383421854,388116868,394417624,403890600,412270240,423528205,433120746,460081938,479128772,479314363,497391293,499914654,506110253,506379243,559009676,562706381,597876260,622182896,630468626" + 
				",673424078,705773354,751277849,757672466,765210836,799173869,808310592,819715588,825417858,830055555,837423208,868261321,909718397,955554387,975351927,1002621766,1006543557,1023835642,1034874894,1077000187,108" + 
				"2833572,1138527172,1138738357,1173774672,1196186825,1203965243,1210531235,1222552465,1253492667,1264659919,1268468842,1276438448,1299160795,1309636587,1322489000,1330087565,1338097267,1352357688,1356779457,139" + 
				"7434259,1462704414,1500469601,1540431185,1557968356,1565680004,1568989969,1610677795,1629344840,1672212884,1683806826,1684065979,1687567433,1716650986,1718095219,1730219762,1746328677,1752420042,1758808445,179" + 
				"0001918,1800556362,1801155856,1805538987,1851716067,1861103184,1892617672,1894461526,1902409369,1915053392,1949716207,1964838916,1975250600,1999612678,2002351882,2009939717,2022473227,2029293825,2042427825,204" + 
				"4612833,2057927217,2058226819,2068556204,2070138977,2070416826,2082950770,2109571018,2117837643,2122064349,2268116181,2269981329,2276271949,2342939253,2360837799,2365198061,2380714286,2384632326,2399132678,241" + 
				"5353008,2415892489,2422240992,2440728049,2441889439,2442832703,2475022259,2475872641,2539895033,2554704831,2558740133,2569553884,2569729861,2573636995,2584545368,2592173814,2608808548,2615135267,2616006171,264" + 
				"3272368,2678616341,2689304648,2700703058,2727451653,2727725893,2727726677,2749917675,2753181846,2767544435,2809185394,2865674145,2888366117,2900799530,2901706712,2908637452,2913721291,2930758554,2949139911,295" + 
				"0749208,2965480671",
				"AAGGCCCGGAAAA\t895627130,1076585327,1184409504,1373846032,1387878874,1742435788,1884425048,2117766284,2274935069,2293502027",
				"AAGTACAGAGTAC\t112068774,112435839,254755978,272292527,381152454,407949458,598688899,689266886,697322367,703165762,769409645,771042120,781472501,814474570,841917686,871040569,959317740,980605008,986250752,108" + 
				"3382229,1178213568,1226411264,1277155564,1315149998,1373846008,1426855499,1578266448,1581473637,1609868865,1629743754,1635386363,1836234512,1923700670,1938697531,2009803689,2032735828,2035355852,2125662960,216" + 
				"6830934,2258136612,2268578632,2305565210,2355806921,2357318873,2484867974,2698776519,2725571599,2863478289,2890405556,2978566366",
				"AAGTACTCTGTAC\t31009841,49004376,480660815,556353276,578355503,692698627,850418528,861937264,868704629,877071095,897129844,958748488,968758204,1025173652,1089063641,1189059330,1273922614,1318469185,1323351799" + 
				",1364395953,1387189021,1471461002,1579472049,1583054994,1604796569,1651842712,1659344649,1662690274,1703910759,1892616245,1938936663,1944084454,1976820130,1983564649,2027023283,2048490773,2116548554,2376189030" + 
				",2379635475,2510743239,2632006259",
				"AAGTAGGAGAAGT\t223506895,233436761,317303936,319591991,345711286,368122519,572962595,580778156,660378366,667437040,855713944,862044114,909916521,916424633,972109377,975121357,997017363,1003260240,1009777173,1" + 
				"073706643,1156044643,1203815843,1258521653,1284139765,1306540389,1313603034,1373845999,1475337006,1527529786,1557785084,1568509388,1643713006,1689289429,1746649104,1823504357,1900283912,1915198632,1937324488,1" + 
				"979796047,1989849685,1992660034,1994429138,2030277871,2047400978,2119977469,2122550130,2137842790,2251549673,2289760202,2339478729,2553955860,2769810658,2907114402,2909713364,3005003439,3028747524,3060608174",
				"AATGAGGTTGTCG\t278118865,1373846043,1526741704,1947473296,2067147544,2138972722,2236474414,2661678739,2907998107,3031104535",
				"AATGGCAAAGTAC\t174927151,209378016,213462197,214644717,235331167,252469709,392328949,402241572,408539136,439990607,440237450,499867761,543909732,560063973,565511179,609478014,630090068,695113224,820454824,953" + 
				"186109,986232571,1020395053,1066507562,1100968503,1145952385,1247355120,1258320748,1284797498,1354475677,1365891456,1368281245,1394100464,1434057016,1508722198,1554783794,1557336532,1579296141,1583894498,16049" + 
				"25777,1780470045,1801272526,1802051966,1932796478,1946716312,1983949591,2231603626,2280366244,2286954230,2335482890,2377336829,2443751023,2476010764,2516632558,2800764108,2808114968,3002176621,3025188543,30463" + 
				"08857,3101047592,3101186257",
				"ACAACCTCATTTT\t17370812,29292088,33394948,61525141,83686248,92002216,97244409,150539953,163619146,164372109,184170139,201978276,228483390,228488534,238761761,253338519,256636038,368605595,414297681,415547695,463234698,479128770,492522296,494340733,508681273,516739121,529545566,574731177,575018363,587478145,591245306,636775861,644056460,680166816,684069500,690972108,717642495,752479794,768988248,780554375,781528953,786476686,790269427,793147188,797613401,837423206,857643662,868261319,949204149,1024239369,1027595870,1077000185,1084883786,1112830316,1114058421,1138183895,1168008171,1215559620,1226616385,1246111166,1270291748,1276994849,1297240488,1309971859,1326807730,1371527566,1381557094,1400969004,1403724640,1411500063,1418228881,1418604400,1422474612,1429736697,1434383205,1452228814,1465588188,1472593528,1496185871,1499968576,1546565083,1555274723,1578725840,1580053692,1580870874,1631881001,1638151955,1645955964,1647784642,1659195895,1692361091,1701855714,1718890808,1732423044,1734985431,1744121584,1760992683,1763595173,1773391973,1805538985,1821058775,1828210099,1842741621,1858345737,1874988546,1882068359,1947654503,1963786000,1984715823,1999612676,2004410947,2009939715,2012096318,2038945007,2057952345,2060908459,2068462501,2078526398,2117959328,2118726654,2133578710,2152866224,2165115268,2185313357,2247789724,2248133821,2263731981,2272739851,2275257679,2359413024,2366044906,2376390214,2386263121,2416425077,2430859120,2461414907,2518774509,2545560614,2552950422,2554704829,2603820023,2609272408,2641105535,2642059136,2644761243,2653544611,2681633240,2681837971,2682626161,2690947431,2717024362,2719132727,2768610354,2775097107,2777725447,2792038467,2808804917,2813645558,2850940709,2855670877,2895091811,2909525790,2910294307,2914137003,2920021402,2920857080,2946249344,2949139909,2990081149,2993326290,3008891080,3035211680", 
				"ACAGAGTACTTTG\t13378549,14703615,54291144,56447848,65657819,145666447,200051566,220828197,246378906,306152291,351622863,487507369,507259150,518103265,528133508,644374341,644516569,649413259,775614541,843737518,908089963,910766543,980385587,983523807,991604302,999522252,1139226812,1232104279,1260374942,1350918698,1373846012,1448843157,1493973329,1612695941,1793627220,1819428189,1828814431,1955819669,2039466792,2076602834,2133858882,2238938385,2302667095,2368057572,2554214714,2646982564,2727539648,2809798637,2905852937,2992355614,3010571148,3045957168",
				"ACCTCATTTTCCG\t52350295,157347078,288380255,506379244,1892617673,2461628795,2854831683",
				"ACTCGACAACCTC\t906816977,1203131636,1403200378",
				"ACTCTGTACTTCT\t599022,11174028,56303611,58503468,66056247,86830512,98366796,99306192,103832418,103972150,119892902,142872988,143456229,157071480,160712354,173214920,239336706,247029808,255166088,264981316,290393430,320975212,324086997,347980753,357477737,400988721,410697910,415955305,433426805,437338603,439418915,471626587,485617447,528502958,576544021,590288481,591316165,592992702,599584878,667897171,684130146,684477671,695757261,709194204,719060547,730805346,737973650,740092174,753029252,753616267,776483667,780143344,802587944,810863584,815997125,856608923,870247537,878271358,897129848,915187002,917044869,939198019,985294029,1000425441,1010932337,1018767877,1028152009,1030436043,1037088245,1062625819,1064003950,1102104531,1104138550,1140647765,1144942130,1158588308,1180485237,1212555442,1236911384,1246860330,1249505134,1250578763,1285231056,1298931535,1321253401,1331347487,1392887292,1396680728,1406403398,1450097789,1451646377,1483293984,1488749442,1491368801,1502684643,1529344232,1547006245,1547012299,1574943217,1578670852,1579998379,1580815867,1583047181,1604788787,1633242292,1673980724,1698000155,1709060802,1724486042,1732639095,1734878216,1743292079,1745755999,1746501222,1762989756,1786618273,1815973714,1826181769,1838108188,1859206232,1872942616,1873695591,1874553222,1874618505,1874645491,1901142142,1902721902,1914137211,1916856611,1921969304,1924398791,1944085889,1960342518,1969427602,1970607931,1985147901,1989138158,1989673689,1990756607,2038033044,2049923875,2066408345,2070753234,2107512187,2118089989,2120015492,2124692064,2134220814,2153730366,2166694255,2183672572,2189593789,2224644304,2260937834,2331623147,2335477145,2338500730,2340091314,2363615892,2369291947,2373167734,2379312819,2384686789,2389335450,2416676052,2436423708,2466012214,2475023295,2485544241,2498427147,2508720114,2514680032,2549721664,2556659737,2586254286,2595074724,2595242581,2614984410,2647303901,2654376607,2659579778,2710993120,2726875050,2759075827,2773320911,2791555976,2810140193,2810380000,2822789798,2828778248,2855665919,2907595606,2920577201,2923444724,2930657509,2968003584,2988392951,2991039279,2994375781,2997511528,3049663141,3096624740,3099371396,3099849586,3100725317,3101409726,3101704108", 
				"ACTTCTAAAGGCC\t27592938,40030071,56701266,66872127,72385861,196572788,314792361,445766492,454812940,760224962,901975990,910132596,1009215981,1020811706,1043144450,1081006946,1082376572,1142486825,1151789636,1152233021,1487027911,1606950492,1637826630,1649440833,1752105733,1831430701,1845280180,1975675515,2018652294,2222874071,2267823453,2372604685,2545330242,2565849113,2568169998,2718789733,2759246866,2860665967,2863682580,2964099241,3007927949", 
				"ACTTCTCCTACTT\t10335957,10979583,101903750,174038338,214843415,270382066,303512963,324241484,345946019,354502742,382697228,476023109,497212417,559785264,563785250,722499984,796769460,838340635,857086724,865330553,877845156,889115333,895366775,933625279,952945733,955745742,1113628844,1118510077,1128883824,1267666883,1287719945,1443266211,1481542922,1507082535,1560153885,1611618019,1614847886,1620815074,1663934384,1688848068,1700210905,1701914152,1736761840,1736823551,1823780776,1834193032,1846653727,1846950166,1894092997,2007609379,2012918824,2030570502,2048390819,2131993721,2137650629,2152822443,2340433908,2347275632,2357039430,2400393112,2422207631,2433296062,2542565571,2639777543,2645016306,2760048080,2797961153,2887573686,2887677417,2977921480,3057916233,3060878223", 
				"ACTTTGCCATTTT\t15799806,40966502,52202724,87455971,100963617,104168917,104263042,119428594,153738454,175771609,177553746,187404674,191578468,198291645,212323290,250901848,263164752,263249902,282838306,303586474,306091441,313502938,313679269,322276330,333011629,333180188,360067873,378929117,399161554,403154246,404837294,417009768,418111246,435611441,458783925,459453087,464385989,464513752,464754128,465667473,469241459,477942502,480314281,486956643,508410397,509709827,512914057,513624324,572006705,576951701,600820963,610274686,618375160,623180542,632106934,635302155,636771894,673985221,681451710,681550901,695183499,703009560,704640221,711082823,715267103,718742849,729664501,751837291,753157893,758284166,764167089,770874516,772056687,785811749,792337844,796054979,807712139,810409506,822394510,827237251,828592724,833504237,858124510,858586724,860757740,870949572,876829572,901693965,901869683,920832576,921939456,933863248,935987531,940049659,941554434,945102750,945103910,972433584,975252432,987075352,995083188,1001036564,1007341215,1012201962,1017452598,1018657109,1040626167,1061616784,1067604192,1068248101,1100621634,1105334023,1112554091,1118701014,1130319455,1136403766,1143776370,1147115549,1181012087,1183597294,1188946707,1204621486,1205538329,1249954771,1251203325,1253259940,1254796260,1258601203,1260354414,1260380844,1266776372,1267357312,1285512093,1285975149,1301070311,1302900972,1313506011,1322041739,1324111038,1334849538,1345060936,1349453930,1351997920,1362835801,1373846019,1377973707,1400578055,1410006946,1427722840,1428369401,1442740887,1470057051,1472627789,1478068923,1478122187,1483130790,1509767758,1544124532,1555661650,1565203033,1617908635,1619061267,1631125927,1645386541,1648987024,1669190569,1674158341,1684783743,1699258988,1701752560,1703582641,1707990848,1732014424,1739888234,1746668997,1783684961,1788437134,1796405964,1799749928,1819494068,1822103169,1829635880,1842032041,1843313577,1852405287,1853152649,1854935115,1879062035,1895186253,1908571683,1911018436,1915685596,1940469430,1946566449,1950126400,1962316127,1966898489,1968572478,1969890556,1977212028,1977467020,1979085925,1989889643,1993740735,2029767398,2032425175,2036266649,2039422163,2048329501,2063600349,2066992336,2067184638,2124389536,2143605990,2156632903,2157247751,2169376956,2186965915,2239349248,2248012245,2249625309,2254058244,2255047034,2255198085,2259541973,2267969301,2272642217,2282139551,2283320118,2335596499,2343538553,2356605103,2376511506,2391439228,2394421618,2401263522,2402700537,2419200880,2435660918,2463242616,2472303116,2491893220,2503596060,2512128496,2512864469,2532119481,2542079624,2566361771,2594010782,2606643486,2617955589,2618525043,2625568425,2629315580,2649923084,2667955370,2704700095,2727091308,2740861347,2761013154,2764669040,2802762901,2805701226,2806219181,2806851244,2807512671,2812103148,2813472728,2816496063,2857474019,2862308867,2886269128,2887613926,2889967935,2891148873,2891197921,2899792558,2912588754,2912724649,2919560156,2931463535,2961814469,2973706949,2995778351,3026059756,3059325405", 
				"AGAAGTACAGAGT\t391424,12796807,44291488,63405060,105589621,107085244,142636036,143134917,159863355,170263119,172675055,192577109,195960577,217978679,233300584,247877440,254755976,280108522,287370892,292025542,311788484,330587049,331306851,347858725,358337678,370147944,373476095,374007857,412427025,414632578,414749710,417785011,496244090,501822657,574228744,574879001,595094051,607647610,618367029,626418553,636551976,640398065,654249268,685513525,693530696,703282904,703498662,708421420,710397519,720702303,729190685,737836024,739653571,755042811,781472499,799246894,800584446,813279965,816602528,817043240,847020659,893904259,897369712,918385539,920670243,935432633,948563055,954159794,965712306,998274409,1014900644,1028643075,1036851256,1046058404,1048789870,1062444676,1068425931,1083382227,1099897634,1100959957,1101243305,1139721702,1178213566,1194262954,1211512605,1215519074,1217917650,1217917801,1233517552,1257446672,1259974667,1260130567,1275232307,1275766283,1302082710,1325209821,1325354343,1330695492,1336349464,1337038293,1337984366,1346309802,1354581241,1371705392,1373846006,1397680709,1403099978,1412846689,1413311980,1426814020,1479305112,1530960251,1536844906,1537094325,1544423868,1545890697,1547257881,1555376656,1570837860,1578266446,1580404271,1582707562,1605351280,1612785747,1613381491,1632594678,1635276501,1641046878,1646119727,1655765342,1700992693,1747742465,1777893358,1876395558,1889689408,1913758455,1923700668,2017292687,2022477375,2028807598,2029403619,2036947881,2047078302,2058080555,2128778680,2135279127,2142354879,2150081890,2156565941,2167871083,2182067856,2190924449,2253746833,2256768949,2285134679,2285326306,2288199102,2295433417,2338072225,2357318871,2378283217,2389644904,2398513826,2400985744,2409725291,2417487430,2469007297,2474407752,2478252096,2486876161,2493413786,2497098392,2513281963,2532252720,2553078380,2585652973,2607280030,2617683159,2689271254,2698776517,2699210051,2709670534,2711050793,2730812476,2754503436,2771644888,2774991921,2778149581,2778173759,2791264415,2791780694,2804264691,2808218963,2814955179,2816175154,2896313110,2923205396,2945984901,2954373352,2966601155,2978844712,3098679508",
				"AGAAGTAGGAGAA\t574323,48471903,50422646,65799888,87564198,89726938,95790524,145256983,159166012,169709655,191202710,197009309,206802025,227050937,233328110,254789650,261645776,293610144,293803598,307426494,334618112,345711284,349564246,354638814,362656164,381953623,400664551,431994956,433571147,456551006,467897119,472539136,491705483,493521473,517322378,526094090,534510071,563724594,571749140,573470646,604500289,612337707,622852280,623783464,636265109,649538198,660378364,660774028,660774904,676591347,680079434,709980154,717348895,728582817,728582852,728582896,728667714,731262885,737402957,771105005,772111947,776326939,792093272,855713951,864773195,875674968,893809014,900121977,923543978,935906679,956663795,971296185,979892867,982173516,987450205,1001942917,1009190244,1065285798,1127418365,1163638647,1166283040,1192202781,1193302838,1207867868,1259375101,1272004307,1290651309,1323905214,1335349795,1342968911,1350977705,1373845997,1381234765,1387278747,1408244202,1441866289,1445463101,1446165681,1465735711,1471006697,1471221473,1479712347,1484811755,1487744210,1492985057,1499868185,1520692116,1557785082,1576245093,1576245108,1615786953,1624314709,1632409226,1642070293,1643510031,1643713004,1657354344,1689289427,1696717019,1696937932,1723211967,1726180143,1762410353,1763286389,1763494584,1791852631,1797177547,1808000114,1811181043,1822862572,1837841992,1857321513,1872114388,1875833274,1889720673,1890561100,1898155757,1901581979,1910879893,1922243917,1923204965,1927073009,1935351181,1958004669,1965262335,1970171890,2006154806,2032415465,2036222676,2059443508,2063317474,2103815530,2106138870,2112483702,2115945881,2124764349,2125470812,2132615378,2154401647,2175794766,2248899846,2263079970,2265679370,2278841303,2283053458,2283534745,2363750365,2394079971,2398137975,2408460303,2426801937,2432136752,2461586173,2467587158,2472230828,2472530632,2472602802,2488482857,2551062662,2552742062,2554085370,2618981741,2628064315,2634389139,2642343100,2643734418,2655771614,2664033827,2672608874,2708003552,2715688588,2796074589,2825539926,2895932720,2900261889,2907114400,2914671958,2928525551,2954369715,2960936686,2968402305,2977367673,2977374188,3016459011,3021747537,3044909710,3045191455,3057221262,3060608172", 
				"AGAGTACTTTGCC\t18693903,63538596,77657877,545670206,700588102,923995356,947601996,1108800787,1131135162,1256417876,1271714862,1328293047,1373846014,1416348846,1698831644,1815683475,1951682707,2163553277,2173083957,2415426325,2442146743,2443144172,2646982566,2742668357,2921007038,2963118361", 
				"AGGAGAAGTACAG\t56344753,60817517,82468405,87398425,105659053,107085241,157668392,167513820,168181975,207491766,211508905,231095101,239940138,266192487,267021241,283300084,378617021,457525292,461179850,467623558,480568504,501572998,504257955,505008375,509924930,522576094,525283447,536801018,610319794,618573150,677085098,718559455,728490237,734758382,744281315,756607999,766068131,770293204,777336325,780595347,803386044,847020656,860408432,875386079,877890768,897369709,897916109,904407325,989433984,995720557,1048789867,1062100817,1097572402,1102814858,1135301418,1152289965,1181128542,1187510309,1206523908,1207412713,1224595772,1225412902,1229880411,1337813670,1373846003,1389979168,1403348588,1418432288,1436416581,1546468873,1547257878,1553932917,1556746366,1570974384,1573693516,1639367446,1641046875,1650819966,1671346207,1733022028,1765936715,1813054896,1863024548,1908240363,1922157580,1926951634,1967326807,2011685974,2030943751,2071195195,2105102578,2164348949,2178372622,2178720115,2182067853,2182401315,2256531737,2275746668,2343491743,2347700793,2347713599,2392834990,2399989631,2427106815,2427429023,2487327876,2492295757,2505161237,2531200987,2554473144,2566685505,2567120795,2575393400,2606997845,2728966051,2739514482,2755061137,2804182076,2818726292,2821271063,2853563862,2870086455,2890545523,2894699895,2978844709,2987354623,3027625740", 
				"AGGCCCGGAAAAT\t1373846033,1578392603,1578921824,1884425049,2368000418", 
				"AGGTTGTCGAGTC\t420063524,1373846047,2690498281", 
				"AGTACAGAGTACT\t112435840,288494418,381152455,557075935,560948967,589216557,598688900,771042121,781472502,814474571,841917687,842365202,893150364,973439090,1010003149,1027095110,1034629566,1178213569,1198533366,1327602980,1373846009,1578258640,1581473638,1609868866,1635386364,1836234513,1923700671,1938697532,2009803690,2029330707,2032735829,2035355853,2258136613,2268578633,2426886378,2434615046,2741285909,2756137275,2863478290,2950363831,3001009671", 
				"AGTACTCTGTACT\t40747102,90394211,146754078,150834626,178814291,288829406,295780287,319700098,381167539,399533754,430862789,451621055,456644497,556353277,558865058,578355504,692698628,714401278,737473407,743568630,824787993,877071096,897129845,1047314243,1089063642,1109366339,1129436063,1138167577,1189059331,1253161612,1324894861,1364395954,1444477397,1554432444,1565069176,1579472050,1582347595,1583054995,1583332558,1604796570,1611306824,1651842713,1659344650,1662690275,1733813592,1938936664,1976820131,2048490774,2116548555,2129128494,2297256443,2422869768,2632006260,2764194341,2803680377,2917116332", 
				"AGTACTTTGCCAT\t74049194,83159875,90047268,109638397,176722380,191907278,256168625,297685278,313502935,389946668,398910824,437642130,468387352,534967520,545670208,620515357,654203347,668950589,713498410,717211801,724909400,798989383,807712136,902820231,918752108,923217359,923995358,957990687,1022991481,1029401147,1054816953,1126453123,1147115546,1328293049,1373846016,1396378001,1425869312,1466596557,1523757345,1543746346,1578126075,1586353777,1617908632,1704273559,1786504403,1804936347,1812126562,1836972128,1881102175,1894790832,1916489639,1938340125,2150924897,2173083959,2227680291,2260459051,2389375935,2486825825,2491172908,2548371202,2578377416,2593126497,2702057492,2744742322,2752143614,2807512668,2995778348,3006323228", 
				"AGTAGGAGAAGTA\t190927370,319591992,572793475,581805073,667437041,817223158,855713945,972109378,997160274,1156044644,1175854169,1203815844,1244583178,1284139448,1284139766,1308258932,1352699504,1373846000,1413205734,1546232711,1568509389,1643713007,1757056126,1839632303,1900283913,1907899357,1907977861,1911998913,1942317613,1979796048,1992660035,2013672104,2047400979,2119977470,2137842791,2190005072,2283461204,2289760203,2427945631,2563114044,2757878944,2761739858,2863083127,2864296648,2909713365,2916501497,3060608175", 
				"ATGACTCGACAAC\t269275655,667663736", 
				"ATGAGGTTGTCGA\t278118866,328591615,1373846044,1526741705,2067147545,2147194499,2505023519,2757017310,3041926947", 
				"ATGGCAAAGTACT\t172349202,174927152,214644718,231846280,252469710,315729905,320963388,348983426,402241573,424784512,440237451,475357617,499867762,550099717,554052368,590722709,590722736,596577875,626414299,627535659,651542124,732502017,739399863,953186110,954539150,974504402,978704698,986232572,1020395054,1247355121,1258320749,1291240280,1350573622,1353830723,1374289803,1434057017,1451017677,1508722199,1547003022,1554783795,1557336533,1562030956,1579296142,1583894499,1604925778,1619559462,1802051967,1890544136,1933146912,1960155199,1968205454,1976869759,1983949592,1989855108,2030625653,2107632185,2158448038,2281512341,2281843443,2335482891,2345065234,2357094294,2369251126,2476010765,2516632559,2554281236,2725094977,2864586264,2896132573", 
				"ATTTTAAGGCCCG\t454184151,1276612209,1373846027,2501968385,2764247309", 
				"ATTTTCCGGGCCT\t1198466812,1582903294,1706303174,2069227269,2948795056", 
				"CAAAGTACTCTGT\t8806240,12988666,31402754,49004374,85390767,94694239,98627845,189829114,225784246,232949329,248642227,253553784,378694154,397566178,411354383,431813754,502076496,552561883,556353274,665025684,762939242,772671246,776890878,877071093,955052119,981304486,1026394244,1114301882,1215131423,1246595123,1277200378,1311867014,1456391817,1617220117,1662443165,1662690272,1702447781,1739601294,1797269958,1812793239,1820763090,1887342634,1895556498,2000294190,2007178550,2116548552,2152478600,2241875387,2257129201,2372232478,2398990797,2495851893,2534062606,2600401763,2601052936,2612038870,2682179955,2702743547,2703179399,2712055426,2917028671,2950498189,2984042009,3022826708,3042530890", 
				"CAACCTCATTTTC\t33394949,43823985,44828739,61525142,114262977,166927443,168447027,244721065,249418050,253338520,253428957,256014977,391032461,394417623,405297772,413418883,479128771,488066287,575018364,630468625,636775862,652963348,654681087,680871996,752479795,757672465,765210835,799769146,830055554,837423207,848961727,868261320,885139498,919550665,983986662,1006543556,1047624803,1075199853,1077000186,1112830317,1163718765,1172643053,1181768256,1192772625,1210531234,1218527769,1240399853,1264659918,1275678656,1297240489,1309636586,1321705534,1322488999,1372676521,1381557095,1388988082,1391529167,1418604401,1452480159,1454489996,1460243456,1473455883,1499968577,1500469600,1536503019,1547786105,1564640464,1567976129,1620277403,1631881002,1647977732,1718890809,1732423045,1746699371,1763595174,1764534690,1798806585,1800556361,1805538986,1865246253,1884840980,1920224349,1958805193,1999612677,2009939716,2011415572,2012096319,2022473226,2029293824,2037149389,2057107399,2060908460,2082950769,2118726655,2152866225,2227806122,2231935677,2248133822,2360837798,2380714285,2384632325,2434739911,2440728048,2464309626,2475872640,2542944360,2553700501,2554704830,2569553883,2643272367,2644119546,2651173708,2704416856,2756866164,2766838618,2805110775,2865674144,2888366116,2895091812,2913721290,2920857081,2924650939,2949139910,2950749207,2965480670,2986591730,2991308203,2993326291,3008777657,3008891081,3018550350,3021000836,3024036430,3062276813", 
				"CAGAGTACTTTGC\t54291145,65452863,86467000,117488687,304839412,306152292,441762118,507259151,597734887,706546865,775614542,892876868,910766544,938930346,947601995,965432932,1139226813,1209109369,1269599184,1363031583,1373317663,1373846013,1490976149,1494041058,1610830043,1626857462,1635794915,1805924683,1819428190,1820840897,1951682706,1956836847,1979417296,2133858883,2552020907,2589244837,2620513062,2646982565,2775503787,2776627638,2963118360", 
				"CATTTTAAGGCCC\t32608126,64806949,108305206,182873607,221857224,278631777,306699844,355832729,462694723,496587567,500774046,523809906,575907689,737952542,767029521,849559570,910538194,955007632,1072494626,1272803363,1373846026,1424370947,1501366269,1521201659,1528450734,1532094627,1557420998,1636770089,1663827870,1670000944,1700698673,1730875203,1792381928,1851290032,1884327193,1941696260,1946427713,2013353165,2036180693,2041411542,2077604112,2081694284,2181484395,2257147539,2438974499,2476470561,2497958595,2589926159,2595566555,2644710328,2730939421,2733853449,2758321531,2818768760,2912962587", 
				"CATTTTCCGGGCC\t1198466811,1361829385,1692723210,1706303173,2290911079,2560729019,2816514434",
				"CCATTTTAAGGCC\t32608125,63918948,81908500,87543062,182873606,355832728,387860752,462694722,546362446,634161316,672112487,690018361,692628058,836363784,910538193,913805260,938659154,969737433,1009552049,1039813746,1070427102,1178816290,1197660782,1238918144,1373846025,1453468736,1454745717,1499047544,1501366268,1532094626,1557057839,1577030088,1616966250,1702402894,1797002958,1797491473,1826256749,1925164978,1930539368,1946427712,1983667420,2005443341,2077604111,2081694283,2183766129,2367829080,2391967096,2424104777,2627630642,2687615158,2705359083,2756964223,2965918702", 
				"CCCGGAAAATGAG\t75301089,216053976,757170202,1089080719,1224499373,1368328526,1373846036,1797094591,2279854992,2504564850,2706307328,2849549385,2881093794,3036314354", 
				"CCGGAAAATGAGG\t66698602,75301090,711607433,1089080720,1224499374,1373846037,1710934794,2170309503,2400134802,2649460194,2706307329,2767179864,3019229282", 
				"CCGGGCCTTAAAA\t208280200,467196151,563357141,1181639345,2344191679,2537023731,2701456676,2709718210,2768786226,3019029467", 
				"CCTACTTCTAAAG\t177048043,220597828,426278187,510671471,518417439,577161617,594395704,656586561,682807626,766173318,796781878,841452572,894624629,912413301,1108886911,1156168674,1296767788,1381018487,1450309133,1519629895,1560872780,1615055800,1700304679,1790879546,1840538665,1855471773,1858778670,1887208384,1923743673,1926945525,1931274579,1949252149,2004988014,2018781366,2040763909,2046579533,2152685799,2241151764,2257584802,2297396192,2332799539,2353691474,2365764130,2544267601,2611569654,2614837953,2632655159,2659055330,2732331670,2819517339,2861799419,3017288684", 
				"CCTCATTTTCCGG\t157347079,304745376,506379245,984561937,1393345850,1432339138,1976561651,2715603375,2883695298,2912717017,2924063920,3038915858", 
				"CCTTAAAATGGCA\t408540,102942522,146127246,147669078,172518932,193204543,212419390,246124269,264158338,296669052,301824263,382675782,390307755,438417218,452208590,457315338,492554069,499737177,504746262,526640137,545896706,565278933,575063705,600257334,662526856,667863479,681171151,728658630,732963076,774643775,774718472,776821168,787115632,787824641,800674978,810063038,862588696,867937330,907930179,933064191,948467142,973440320,978699740,993434658,1019362544,1062461790,1101982123,1117808504,1118297132,1129702594,1159902336,1191535825,1206089295,1220263873,1220897501,1233534694,1262715953,1267468264,1346289028,1375170566,1402366292,1409807793,1429964080,1496204136,1519633085,1527597256,1547020168,1562728319,1569767672,1635739625,1658062694,1691238189,1739280392,1794108969,1799488487,1809759786,1821209403,1877947770,1906606359,1967960661,1976184885,1978256877,1981349336,2050639519,2140136624,2187035597,2196036314,2226655180,2235178905,2255724171,2258034249,2266173734,2383512232,2392718464,2415201487,2485979878,2487712256,2493458825,2585550061,2638552917,2704250750,2725170035,2734875762,2760855916,2762019562,2821353725,2823566859,2903406019,2911777354,2914258151,2914610202,2979671038,3000708127,3044871789,3055591341,3062767921,3096202262", 
				"CCTTTAGAAGTAG\t101532327,167480919,238022650,239908958,317357610,361303566,473998769,477929721,523602184,545293123,573904035,589905275,632856115,638766212,714514967,730533379,874578056,906311192,972059445,992403912,1006962301,1189207587,1210978855,1230458460,1262350107,1310650451,1332177896,1342261076,1373845992,1406990315,1500257311,1519594410,1549549589,1685868105,1693590886,1809686015,1960323522,2267598799,2381652441,2385496714,2396313414,2513510400,2690428881,2803566340,2965443702,2982269879", 
				"CGACAACCTCATT\t554464624,1052732843,2165115266,2708075107,2976096703", 
				"CGGAAAATGAGGT\t35367726,194310146,370280620,378982879,482724036,543664252,631603898,1148893443,1224499375,1226349482,1373846038,1767983020,1821559298,1951634008,1982242157,2170309504,2355845832,2491421795,2922584172", 
				"CGGGCCTTAAAAT\t467196152,563357142,681418743,975753019,1181639346", 
				"CTACTTCTAAAGG\t72385859,107885385,177048044,183751479,186211463,220597829,372498886,518417440,547749268,594395705,601193750,643191054,662196311,681678702,940617384,1086786401,1192749108,1267440980,1452609892,1516504719,1530850853,1637826628,1664644125,1670662247,1683093700,1684717954,1741971912,1788034133,1855471774,1917248800,1931274580,2121504828,2303436041,2332799540,2365764131,2366631465,2372604683,2382844773,2382869049,2544267602,2609269023,2741560275,2915696879", 
				"CTCATTTTCCGGG\t233171939,573856975,666978487,722980525,892858513,1512049080,1556276830,1932001807,2604809555,2615360299,2715603376,2883682981,3038903541", 
				"CTCCTACTTCTAA\t50091752,77666918,78785788,104636006,166039090,168502000,187691091,220597826,234781063,254084258,274463810,312426773,324861030,344978602,345118802,356439351,398595545,401302261,416344885,426278185,439155399,440021452,529987890,567322796,567682727,668150938,737981994,793895890,825148344,870202450,1059220914,1108175504,1120077364,1143382202,1200223500,1226639520,1258374945,1268063474,1296767786,1298114251,1320487595,1476329790,1482013413,1501709409,1517338362,1546635873,1569286706,1612963791,1615497839,1729917617,1744636208,1875124805,1895993463,1907804832,1983919875,1996596886,2026610741,2082246193,2123468790,2152121571,2283173973,2368289178,2401912147,2496339525,2550973122,2569983388,2596632119,2695313716,2702072117,2807843342,2861799417,2871027411,2888217574,2984655038", 
				"CTCGACAACCTCA\t1933093614,2190591756,2197553793", 
				"CTCTGTACTTCTC\t11174029,32227098,48247593,58255997,103832419,103972151,107736076,119560732,160712355,198778165,239289186,246709154,247448254,264908764,272317178,285142007,297085502,299899710,345948186,359233608,368571643,410697911,434051461,437338604,443590319,485617448,504338552,509396323,576371531,579682754,590288482,591316166,632517971,670676601,674909717,684477672,713262267,718202284,718872947,730805347,767273962,775017935,776483668,804415291,807380176,814067687,824575926,837996112,878820352,906093481,914499732,920587678,968482104,1023093841,1085436789,1102104532,1120286736,1120893882,1135314533,1147123165,1156902531,1158419232,1205793985,1222883873,1244014382,1248854416,1252191965,1260123171,1299716941,1331347488,1373028362,1384776326,1396680729,1402779223,1406403399,1445664468,1447623924,1483293985,1486321559,1486607469,1491368802,1516989746,1534814623,1545741086,1565576050,1567515350,1576562400,1583047182,1604788788,1612627068,1686442910,1712929400,1732639096,1734681756,1736142999,1773259375,1786618274,1793365106,1800657598,1822584159,1858269917,1861345543,1899555267,1901142143,1914137212,1984863624,1985147902,1996110006,2037015267,2040266092,2040502765,2114719464,2162194270,2187347415,2189593790,2224644305,2224787214,2231944573,2262408340,2268453005,2335477146,2371368715,2373907131,2404606657,2434604583,2475023296,2488149055,2508936539,2512841468,2549721665,2566765305,2619840985,2644104421,2654376608,2700909413,2711101047,2719427652,2810140194,2820745044,2822789799,2828778249,2853202458,2855665920,2926757979,2934734056,2946688381,2974901150,2976335188,2988392952,3036262158,3095635724", 
				"CTGTACTTCTCCT\t6202891,14519604,37138557,55561675,58255999,71101629,108077040,213337284,264908766,272317180,437338606,450781461,462954986,509396325,522851658,591316168,598165732,680417857,683950047,713262269,754454973,775017937,776483670,787975017,898851208,933625275,946533178,999578109,1004185628,1100744738,1106695654,1156902533,1158419234,1178847327,1196917895,1227576300,1248581147,1263643987,1287652214,1301866542,1308844535,1317392022,1373028364,1390606480,1393607080,1486321561,1486607471,1612627070,1695961579,1701308381,1743352373,1772563247,1802516433,1831506560,1859661617,1894657346,1896758263,1896758454,2079369193,2114719466,2189593792,2232935029,2242721759,2250059688,2269364830,2335002773,2411676935,2421257164,2431433352,2432527608,2475023298,2564706980,2566765307,2567121850,2605304495,2619840987,2695264390,2769442503,2776612385,2807045945,2810140196,2867330165,2869289411,2918646712,2936580321,2978502767,2984768187,3008606196,3013506589", 
				"CTTAAAATGGCAA\t77445,408541,26576290,42715232,44575887,47965513,59185944,63459500,65482606,72201175,160853081,190168994,225748505,246124270,264158339,296669053,301824264,316807346,325684404,370208967,374351308,382675783,390131453,390307756,392425436,399237370,399736118,413716187,417843600,422978722,429762156,481417661,499737178,512793051,520650616,532806715,545110824,555515987,556807819,561235334,562465381,575063706,589465831,594728662,595577795,600257335,632519389,643783601,650979239,653256633,675101311,681171152,724315269,727112015,746770074,751142670,759826992,766363883,774718473,778573377,800674979,810063039,827070333,834213028,836729000,862588697,862667220,897788922,906052187,907930180,913678698,933064192,957083231,958048460,962628276,978699741,986374093,992821966,1000791420,1019362545,1049505565,1062461791,1062609407,1069687556,1084185182,1101982124,1120822680,1129702595,1155636085,1159902337,1188412370,1190357333,1205550316,1220263874,1233534695,1241390562,1267468265,1282834854,1291269815,1327590511,1339569723,1346289029,1375987273,1376634400,1384640891,1402511574,1409807794,1411475718,1421451566,1424027884,1455578098,1465499119,1465723593,1486298473,1494105003,1519633086,1527597257,1534226054,1542418422,1583295104,1635807490,1650818817,1695552777,1704753336,1711502178,1731449086,1733756448,1739280393,1748506147,1776084972,1806772966,1809759787,1821209404,1825969029,1832489091,1841458797,1858290316,1863512104,1877947771,1955209462,1984779344,2022490549,2104897396,2112490935,2126093602,2142727213,2160540206,2162631083,2163684311,2193086550,2226655181,2235178906,2236746598,2258034250,2275290032,2281386486,2372706398,2384740946,2385980518,2424540219,2440922447,2493458826,2496550779,2550703830,2585550062,2611191530,2612243260,2618381910,2659563362,2704250751,2725170036,2798712864,2801492695,2882843661,2893263288,2893425751,2897019446,2903406020,2905072599,2912436369,2914610203,2927173903,2928711813,2960643339,2982213242,2983039129,3000708128,3021102884,3025611354,3029264960,3038064221,3055591342,3062767922,3101648254", 
				"CTTCTAAAGGCCA\t9455273,27592939,35452397,40030072,66872128,70914931,72385862,81180605,82899138,103709458,157640125,196572789,327822267,339239871,418015504,433072609,433613282,454812941,456315458,462202545,496897746,497125389,555257649,593357901,595914681,614736420,618584318,633352225,639727569,660243674,760224963,779784782,784105051,861294569,900379091,901975991,910132597,991296061,1043144451,1097081421,1151789637,1220962696,1330965845,1487027912,1516754251,1533759759,1649440834,1679228645,1681469585,1688641664,1746823774,1758670438,1772194435,1805883118,1820804650,1827729017,1845280181,1938291233,1992094467,2018652295,2041990388,2064371909,2109092437,2168548597,2199697796,2222874072,2279947847,2335826140,2385550914,2392441395,2493184778,2753133376,2762094957,2776705821,2860665968,3007927950",
				"CTTCTCCTACTTC\t10979584,101819547,101903751,150935285,153123644,198174181,202344574,213762652,220906257,246356998,251583323,290506713,299105668,324136147,331613805,345946020,354502743,374803532,421709667,469005960,476023110,478230114,515366704,518725173,534526486,560524744,600613618,644998780,653590347,673803265,727946821,745225159,791667423,807947595,813214245,840219670,855683552,865330554,873765102,889156434,889312260,894330686,948569907,1025148893,1090681350,1113628845,1118510078,1140202663,1163172287,1166195172,1181972898,1212378961,1247798732,1260679549,1296767783,1303249823,1326911484,1361649980,1365067539,1368154285,1423126429,1430011174,1481542923,1485504107,1499042760,1500944694,1514831197,1528660962,1531500090,1540045179,1688848069,1701914153,1781754810,1832087295,1834193033,1843448946,1900542759,1907804829,1932240474,1944758393,1977520462,2031152769,2041060949,2048390820,2064051029,2114415013,2118975177,2150185641,2152822444,2196174371,2238861790,2265882470,2266583517,2291524214,2295537908,2336939113,2340433909,2347275633,2401912144,2425864940,2433296063,2458119757,2507932800,2514722793,2531052532,2531833292,2542565572,2548534429,2555146647,2584527076,2654779788,2702525727,2852559153,2863542586,2878178371,2930913295,2976751733,3014155480,3027436904,3044637464,3060878224", 
				"CTTTAGAAGTAGG\t8672857,35913960,87849996,167480920,215560912,222815913,224339443,238022651,239161023,317357611,473998770,497179871,521845387,523602185,576465240,621557616,621663805,822880194,839389665,905877875,939693675,964554284,977963798,992403913,1004400565,1025002275,1085431877,1102088832,1203005081,1290651305,1373845993,1406990316,1418332222,1518613559,1545157724,1577014635,1645556993,1744710890,1809686016,1860965251,1864039950,1953037466,2015923531,2016061260,2281436572,2289749229,2396313415,2398598817,2416685361,2542684094,2618181757,2654877298,2808245370,2906194567,2965443703,2998229730", 
				"CTTTGCCATTTTA\t4540161,10415521,36615785,48113537,56506918,57469224,61773973,63256134,63760068,69009962,77941355,84366704,98240211,104168918,104263043,146956964,151648683,158337854,170996623,177553747,187404675,187893700,249784797,266906682,280302029,282336789,284625719,287276438,290359521,296900849,306091442,313502939,316467330,318185357,333180189,336541004,372740071,378012964,384468604,388271615,388477074,399161555,404521660,404837295,412599550,418804882,419271833,464385990,464754129,465667474,510537027,523709099,527466404,551318980,558023117,572006706,572924699,587474257,618375161,624211178,636771895,638759953,644253013,665992065,685237252,702944829,711082824,715772168,718368677,733969602,735697435,736112707,750573346,751502267,752091788,753157894,758356645,771575628,792337845,796054980,804256610,807712140,826991762,837719629,858124511,868718682,870949573,875448403,876829573,887252045,901496849,901693966,920832577,935987532,942912912,945102751,945103911,954713304,968239812,975252433,982052860,987075353,998032891,1009523866,1025905810,1034449105,1044687374,1065905354,1086012470,1100621635,1119501947,1119609707,1130817413,1147115550,1174758895,1177772032,1187985038,1189364844,1193296099,1204621487,1205438688,1211345341,1249954772,1251203326,1253259941,1254796261,1258601204,1274678381,1285512094,1289232858,1308279906,1322041740,1334849539,1346421176,1355926329,1373846020,1377973708,1388749376,1395437562,1396694533,1408909295,1412161189,1425605701,1427722841,1443712793,1444080502,1453512605,1469163930,1478068924,1500259376,1500410966,1509767759,1510821171,1517319106,1525085371,1534051132,1551156706,1552827194,1566765546,1579697055,1631125928,1639364183,1663961502,1669190570,1678058840,1678943771,1684783744,1688974161,1701752561,1703582642,1707990849,1717917565,1718050762,1739690309,1753134908,1809390919,1841990335,1842032042,1844725225,1852399038,1852405288,1853152650,1854935116,1855442216,1857786576,1865049989,1875077457,1875550489,1893261983,1895990136,1912181858,1943963670,1950126401,1956835563,1966898490,1966951743,1977467021,1979565090,2022515532,2032425176,2042513722,2065238646,2112234072,2117503794,2147026158,2150339268,2157247752,2159840997,2165152268,2176816405,2181845116,2182711075,2186965916,2242026830,2246194480,2248012246,2249625310,2256175473,2276723706,2283160329,2287628349,2290875645,2342087843,2343322612,2346617043,2352142342,2396291759,2401263523,2419200881,2432451058,2435660919,2471361448,2482618414,2486399188,2542079625,2552644267,2557541139,2563268365,2564900182,2565244695,2584270056,2589726582,2612495822,2618525044,2619994595,2621136067,2631774582,2642146925,2667861823,2667955371,2712276122,2729960444,2778300498,2796039549,2810451446,2814945013,2858621103,2891833328,2899653892,2904445435,2907538495,2919359401,2950351072,2953623036,2961814470,2990634550,2994720492,3007518289,3025020738", 
				"GAAAATGAGGTTG\t34231342,53763555,83948023,86555972,110957238,115258143,151907762,166405639,174804003,190127952,208824016,246587092,256440327,263703019,277103043,290518948,298585511,331657370,345614803,349940269,351039384,355395180,378317495,390914571,408442970,411901782,454247942,463451070,466477562,472665941,486702432,489844022,494273210,494435710,509823386,512473439,528083422,580888589,599697512,604264148,609318797,613835992,614386790,643943662,646030142,646906269,648817477,725422988,753195544,753231607,761826967,773194824,775221549,787123609,801244850,838198953,920105136,933947582,937342901,962536381,975816775,979131486,983640570,1011027857,1014058634,1047240244,1111674796,1116487164,1160202947,1160514708,1171231028,1209227078,1219112991,1226349484,1254155003,1267170616,1273056557,1281343991,1299118523,1310225178,1318477829,1358370757,1366294207,1373846040,1439921812,1497711264,1611320746,1618611737,1635819658,1640483586,1649281127,1708395253,1716133602,1749171404,1806023332,1812248600,1836048574,1862663184,1891143144,1905032444,1942407486,2002534147,2014065193,2070291754,2106739183,2110698872,2122278416,2139524573,2175220232,2190999763,2235693986,2303594014,2355845834,2364405942,2385033000,2419478507,2464141055,2486194279,2489437575,2491155654,2493513542,2514716495,2539075713,2566933184,2663833726,2669805170,2774851463,2849836297,2868694593,2886344392,2895745824,2897074048,2910400160,2917476239,2923953405,2926794300,2962963055,2966265209,3004888232,3005456819,3011463410,3052776266,3064293254", 
				"GAAGTACAGAGTA\t391425,15886877,25598710,44291489,159863356,170263120,217978680,231595914,247877441,254386429,254755977,417785012,456766175,457994090,473410911,521606955,556215173,561015900,574228745,669217515,703165761,703282905,708421421,729190686,771042119,781472500,795036201,813279966,816602529,893904260,918385540,920670244,954159795,965712307,972335846,1014900645,1022115648,1062444677,1083382228,1164105776,1178213567,1215519075,1226411263,1227283814,1233517553,1273634524,1275232308,1277155563,1328278571,1337038294,1346309803,1350365436,1369469423,1373846007,1432688912,1521560000,1530960252,1565660209,1578266447,1581473636,1609868864,1612785748,1635386362,1685781611,1747742466,1784303654,1897023883,1913758456,1923700669,1925614582,1952131538,1962237579,1996925866,2017333003,2028807599,2079412686,2128778681,2142978127,2247842981,2295433418,2298283715,2357318872,2379167911,2409725292,2484867973,2493413787,2532199818,2537711634,2609176923,2669389166,2689271255,2698776518,2760679062,2774991922,2814955180,2884458721,2890405555,2890406487,2890407531,2890408463,2890409393,2890410325,2890411256,2890412300,2890413232,2890415322,2945984902,2966601156,2998140962",
				"GAAGTAGGAGAAG\t574324,1230858,152955861,153497544,159166013,206802026,227050938,228040668,274481710,296043094,323330267,334618113,345711285,354638815,376826638,421941952,433571148,468087373,491705484,499485795,502200091,517322379,571749141,573470647,581145833,604261264,612200422,652469035,660378365,676591348,680079435,731262886,737402958,753424898,771105006,825219462,856954293,942732004,956663796,972109376,982173517,991877762,992009413,1007688531,1049495658,1057647824,1058567389,1065285799,1078441299,1128012813,1135022748,1164653343,1207867869,1213320306,1290651310,1301133558,1306540388,1327574893,1335349796,1373845998,1432731726,1445463102,1471221474,1474227349,1492985058,1520692117,1547356259,1557785083,1576245094,1613038807,1614630706,1643283135,1643713005,1664046934,1689289428,1703473375,1741234729,1797177548,1823504356,1824693787,1848730188,1885126677,1889720674,1917955600,1918270091,1927073010,1935351182,1972531305,1975435460,1983834738,1992660033,1994429137,2003307782,2005611803,2010104682,2030277870,2032415466,2047400977,2055512845,2221026215,2232235192,2265857788,2296619308,2366453830,2385399726,2394079972,2461586174,2472230829,2472530633,2472602803,2488482858,2495605237,2551062663,2575893560,2582894399,2584603098,2626870566,2628064316,2647541757,2716399992,2718476335,2752021292,2769810657,2777713080,2870560047,2907114401,2911475083,2928525552,2988540644,3005003438,3021747538,3060608173,3064845318", 
				"GACAACCTCATTT\t16561177,77947307,114238762,184170138,201978275,220084322,228485360,262800340,355339184,415547694,417827744,474301988,601996235,680166815,694436295,797613400,821704937,836897229,837423205,868261318,912269749,995355877,1024239368,1027595869,1044619460,1084883785,1101023666,1114058420,1169740552,1216371156,1220425046,1226616384,1322938943,1351332926,1357580004,1381557093,1425346390,1496185870,1504110265,1631881000,1697516135,1732423043,1734985430,1735030236,1740964688,1871624670,1882068358,1947654502,2004410946,2010460569,2053025233,2060908458,2068462500,2118726653,2119036673,2151503318,2152866223,2165115267,2171400065,2172930894,2184136379,2185313356,2276870064,2347145448,2359413023,2366044905,2386263120,2492310813,2545560613,2634629301,2642059135,2665020034,2711802651,2860226863,2951109944,2977353626",
				"GAGAAGTACAGAG\t12796806,20788800,39267770,62431323,63405059,107085243,170208674,195960576,198855892,225741389,228782713,239940140,243945931,267021243,303673235,345709119,347858724,352012175,358337677,358419109,402306181,428973010,516885783,677085100,709517692,717886010,718559457,729625726,737836023,761930397,767532191,780595349,781472498,799246893,816602527,845693518,847020658,889066817,897369711,904407327,917084289,989433986,990324304,991975397,998274408,1009208520,1039737071,1045331111,1048789869,1062100819,1062278517,1078619939,1078639120,1083382226,1099897633,1139052013,1152289967,1154916247,1170128470,1187510311,1224595774,1337813672,1337984365,1354581240,1359068874,1373846005,1384982998,1389475982,1389979170,1412150464,1418432290,1428069543,1436416583,1440118612,1494659904,1537094324,1542923196,1547257880,1556746368,1578266445,1616850633,1623305100,1641046877,1652548852,1655765341,1670020997,1694629384,1775517195,1786435002,1802298838,1876580050,1923700667,1926814931,1955639883,1971808425,1979229040,2028807597,2040071654,2072471598,2128778679,2135810901,2142354878,2149281252,2156565940,2164348951,2177582510,2178372624,2182067855,2199699570,2252764351,2253938994,2281538313,2346095339,2368437334,2382287127,2393815116,2398513825,2399989633,2400985743,2417487429,2434763585,2508062082,2566685507,2634186724,2730812475,2739514484,2740216951,2754503435,2778149580,2894699897,2895146830,2895685175,2896313109,2962155507,2977120740,2978778375,2978844711", 
				"GAGGTTGTCGAGT\t1373846046,1462479218,2690498280,2709991127", 
				"GAGTACTTTGCCA\t18693904,74049193,77657878,256168624,352717177,437642129,494664829,545670207,918757873,923995357,968572900,1108800788,1131135163,1271714863,1290214547,1328293048,1373846015,1385265506,1425869311,1659258585,1695025984,1881102174,2136170522,2173083958,2223099711,2379717792,2380681431,2389897724,2390195050,2415426326,2484926016,2718633795,2742086191,2742668358", 
				"GCAAAGTACTCTG\t79052880,85390766,98627844,232949328,319569706,378694153,500741414,605117645,641946454,665025683,759606648,822690316,877071092,969875414,1026394243,1116747728,1312335833,1410037773,1702447780,1764006304,1777479134,1813061229,1884726737,1887342633,1901695236,2007178549,2046383009,2072085606,2152478599,2359440376,2534062605,2702743546,2703179398,2769272512,2770372243,2878191075,2917028670", 
				"GCATGACTCGACA\t1637054587",
				"GCCATTTTAAGGC\t32608124,81362675,97203495,178049461,309178755,407026022,648133192,692628057,748826677,1009552048,1039813745,1081318727,1110848520,1133674922,1325455564,1361506664,1373846024,1504947796,1532094625,1546815059,1557057838,1557284446,1696555461,1764827879,1791639125,1828636818,1925164977,1932416330,1937041961,1946427711,1983667419,2041247546,2046618524,2076326491,2292584741,2643953031,2655500968,2677695258,2705359082", 
				"GCCCGGAAAATGA\t75301088,1140917411,1172180254,1207362395,1373846035,1668713892,2279854991,2349970464,2654540731,2881093793,3036314353", 
				"GCCTTAAAATGGC\t94190906,146127245,147669077,181304011,424124247,438417217,457315337,475467730,475468591,614789723,707959824,716593876,729670791,787115631,814896885,862588695,923074470,999051720,1026016958,1151139540,1188154904,1206089294,1220463760,1262774618,1402366291,1502344904,1519633084,1547020167,1610867668,1725170952,1739280391,1772491618,1821209402,1971770414,2055666161,2154900296,2155452295,2196036313,2255724170,2335448627,2760855915,2762019561,2823566858,2903406018,2911777353,3009895233,3096202261", 
				"GCCTTTAGAAGTA\t15577073,101532326,194417611,227979840,238022649,272950580,361303565,524594527,555997608,565530814,573904034,607545495,637039083,730533378,820635139,867529137,1013222409,1074360118,1125554413,1262237245,1311366638,1354494550,1373845991,1423741822,1484839510,1711030105,1766706919,1791018453,1800421340,1828758932,1899973539,1939455028,1944189546,1960323521,2134960144,2181217589,2267598798,2336236235,2337985084,2339974094,2340026449,2381652440,2385496713,2389922549,2390219872,2390298833,2392340571,2409601090,2583626287,2681437234,2682990309,2889634304,2947832089,2970396294,2982269878,3016944783,3039849978,3062662902,3063947356", 
				"GGAAAATGAGGTT\t34231341,41346695,45867313,51818838,86555971,88740390,107771302,115258142,151907761,156969041,170367888,174804002,202315758,208824015,240183203,241904527,254634693,256141382,272532105,274882882,284874907,288837810,290518947,295571511,298324626,298585510,332471100,351039383,355395179,390914570,408442969,424924032,452167961,459706448,473085841,486702431,494273209,509823385,511481292,550425631,550992927,552395106,555550006,566353903,569446307,580888588,592706077,593030995,608925879,618573509,648817476,664359606,673579903,725422987,736496628,746470972,753195543,754052817,787123608,789903756,801244849,806104254,838198952,855210518,863435450,864076578,870584219,882612332,920105135,933947581,934036928,937342900,962536380,979131485,1005133530,1007339085,1007833456,1018523507,1029125613,1036247386,1047240243,1075377466,1079083143,1079489240,1085315249,1088351214,1098166169,1117924930,1166638892,1193199395,1209227077,1226349483,1267170615,1268578831,1277535550,1291112347,1299242476,1312488080,1327882124,1343610051,1356397155,1373846039,1381080528,1394830066,1423521070,1431222623,1453892081,1489401784,1547479397,1557756389,1573996192,1611167706,1621798976,1624879712,1639233677,1660204525,1664348798,1693602933,1717279650,1741050785,1742255982,1749087518,1767983021,1800663330,1812248599,1820419086,1831436089,1842124793,1844830813,1859205751,1871420500,1881275664,1892029748,1907991281,1923018533,1929524897,1932090967,1942407485,1952344694,1971380328,1990722856,2005940987,2008677475,2014065192,2042579955,2049174146,2051538154,2116443896,2156185518,2157094963,2162865017,2170309505,2181816542,2194314618,2195888388,2234632876,2256644607,2263916255,2266671257,2346259917,2348212929,2355845833,2364405941,2364801083,2385032999,2422916420,2443469709,2474650420,2514716494,2532526914,2538410298,2539075712,2567346250,2570575818,2578473071,2603101427,2607583366,2645865110,2654956728,2665275779,2669805169,2709476911,2722719681,2749191598,2774409904,2774627483,2774851462,2863248068,2895068703,2917476238,2922224672,2923953404,2948165020,3004888231,3013778443,3029404190", 
				"GGAGAAGTACAGA\t56344754,107085242,120267228,168181976,228782712,239940139,267021242,294588338,327266592,345709118,352012174,397368804,446223416,467623559,469680866,504257956,535862371,600755724,659501626,677085099,715206627,718559456,734758383,737836022,744281316,747398211,756608000,766068132,766867057,780595348,847020657,897369710,904407326,962086227,974863126,976015552,989433985,990324303,995720558,998274407,1011929703,1045331110,1048789868,1062100818,1152289966,1187510310,1224595773,1299580469,1309423644,1337813671,1373846004,1389979169,1406136961,1418432289,1436416582,1476794989,1547257879,1556746367,1616850632,1641046876,1652548851,1704338761,1733022029,1822388010,1851611001,1876580049,1901111524,1908240364,1922157581,1967326808,1979229039,2028807596,2072471597,2104678164,2105102579,2164348950,2178372623,2178720116,2182067854,2251614677,2272179491,2280596415,2398513824,2399989632,2427429024,2492295758,2517056141,2566685506,2575393401,2592337408,2728966052,2739514483,2818726293,2853563863,2854164963,2894699896,2918963762,2920181906,2939050994,2978844710,3015315119", 
				"GGCAAAGTACTCT\t4053177,211981169,305041713,315729907,319569705,402241575,474193479,500741413,507270444,528714514,589370787,605117644,1026394242,1131981743,1322246883,1368917258,1410037772,1451017679,1702447779,1795982817,2046618157,2072085605,2190479493,2254724161,2275552608,2280105777,2562501116,2770372242,2894854096", 
				"GGCCCGGAAAATG\t482418999,1207362394,1213410348,1373846034,1573286251,2279854990,2909017522", 
				"GGCCTTAAAATGG\t94190905,313891776,358148872,424124246,459635164,532188436,621378776,643074079,716593875,729670790,787115630,814896884,821105885,821673141,923074469,962654492,975753021,1026016957,1159176681,1213211165,1262774617,1299807075,1323994560,1372545837,1444270548,1461441604,1465418261,1502344903,1519633083,1610867667,1644216355,1725170951,1839024238,1844247691,2018025863,2055666160,2117123869,2137019859,2154900295,2155688280,2335448626,2385287302,2572012551,2575969194,2695195812,2760855914,2863919623,2903406017,2911777352", 
				"GGCCTTTAGAAGT\t24421769,29461452,31150906,94620950,120298160,238022648,269187029,329557782,560688704,573904033,609069361,628834314,636722923,666684295,677082456,690747074,690923769,735154570,791644912,820635138,1027595328,1144371821,1168077480,1191482191,1201392225,1373845990,1528697949,1702481614,1775169509,1775805471,1814571238,2173942008,2183283512,2258740805,2267598797,2368228020,2482024188,2573704091,2722924869,2858390502,2891194773,2949637160,2970396293,3016944782,3039849977", 
				"GGGCCTTAAAATG\t69650066,85632954,94190904,267951917,279333613,297881688,361031999,390791869,444893287,522615346,563357143,635608747,716677981,802215559,965466956,975753020,994045871,1022940061,1026016956,1070703772,1078861104,1134831079,1323994559,1390463606,1461441603,1475080264,1519633082,1519874679,1534991992,1610867666,1725170950,1862129632,1872044336,1874019021,1874041124,2051704998,2185940274,2278651337,2334774633,2375302836,2484886338,2726884071,2733482969,2758956437,2796930963,2797669831,2810653367,2925194856", 
				"GGTTGTCGAGTCA\t1373846048,2690498282", 
				"GTACAGAGTACTT\t31820449,200051564,329549488,429471173,530408274,560948968,588451269,598688901,613543909,771042122,842365203,949221155,960323850,973609851,1010003150,1373846010,1417777443,1448843155,1578258641,1635386365,1782420208,1836234514,2029330708,2111913702,2258136614,2357013889,2373688115,2426886379,2434615047,2543580858,2705521074,2809798635,2819760993", 
				"GTACTCTGTACTT\t68877277,97990982,146754079,288829407,295780288,335276577,381167540,530949114,555168429,556353278,578355505,597999148,608844399,647456874,650294418,714401279,730935660,743568631,824787994,852148715,897129846,910888465,933376698,945784081,979074891,1028152007,1109366340,1138167578,1225750199,1284705064,1375532711,1554432445,1554871519,1565069177,1582347596,1583047179,1583332559,1604788785,1611306825,1651842714,1659344651,1663656049,1665438243,1733813593,1821666329,1848338298,1916667619,1935927989,1965617517,1976820132,2048490775,2049105365,2116548556,2129128495,2259427113,2297256444,2386876745,2516080343,2654376605,2803680378,2917116333,3051366137", 
				"GTACTTCTCCTAC\t74501634,272317182,274979154,762317758,857086722,888768870,933625277,1103631149,1178847329,1189656135,1417003618,1797305367,1834765946,2025449696,2422207629,2620010369", 
				"GTACTTTGCCATT\t58795239,74049195,176722381,279569871,285984397,297685279,313271915,313502936,418111244,431470034,438171273,470458647,597637457,654203348,668207799,685535465,717211802,805662857,807041564,807712137,812374653,902820232,918752109,1147115547,1284205090,1373846017,1377958895,1419777300,1425869313,1541518788,1578126076,1586353778,1617908633,1701501301,1718799946,1804936348,1809437339,1812126563,1812658771,1888623090,1902652825,2023706639,2227680292,2288720711,2357271761,2402700535,2486825826,2491172909,2514590363,2564085072,2578377417,2591327112,2593126498,2797953823,2807512669,2808918627,2862062835,2862226860,2899792556,2924058273,2995778349,3006323229", 
				"GTAGGAGAAGTAC\t154594384,667437042,675131027,806014740,886792429,972109379,1248434644,1373846001,1465762062,1465936436,1612955569,1778476525,1900283914,2504138206,2901368986", 
				"GTCGAGTCATGCA\t297122187,313453192,719346420,1107525859,1373846052", 
				"GTTGTCGAGTCAT\t783220304,1373846049",
				"TAAAATGGCAAAG\t21249746,34234816,34995385,37436811,47318496,50271984,55633014,62891500,65882532,95409897,100245756,115444201,115914953,169789044,183064970,187128953,196454150,196963491,198355454,204200555,230048348,230513436,245155471,246759740,264798267,267112015,274004168,292350303,303277501,307996021,314175315,331335102,333129900,334303167,336211206,374744884,382139663,390131455,404581335,408271156,421141235,430944177,434378929,440237447,447591971,454317603,460787218,467365676,470425952,471805281,503416572,522739787,525692280,532867420,553798840,556807821,564436632,587650080,594878364,595039389,598266328,611923046,613886979,617315252,623979234,635993106,637326401,637866918,653760166,681171154,706904628,714507984,756230473,757181655,759451395,761993524,763393533,764226160,775434442,790241865,797534509,808988890,846907206,847919993,862667222,887988020,888907174,899534370,901701640,902072265,906302249,924376131,939557766,955721139,957083233,959579718,969025798,971461121,1016078067,1029174342,1030800841,1034676944,1039985696,1047191445,1051654777,1061164037,1064235944,1072069254,1115203163,1119595572,1157006284,1171936075,1176511825,1181920567,1182051868,1191925694,1210691342,1218915337,1228866778,1250437338,1269748082,1272351666,1279315169,1284797495,1285011160,1306256571,1312508152,1325418654,1326295599,1331894984,1379841545,1400913786,1401842076,1403719175,1414034304,1418685165,1426069399,1433226883,1434057013,1451213484,1458549905,1481262726,1489232275,1490242088,1491734608,1496280832,1513562823,1534043659,1555397561,1557970076,1559571835,1562597695,1582942142,1605851555,1609034554,1626636494,1645970057,1654584492,1661899076,1665097154,1669544303,1681304847,1695552779,1724895672,1751038514,1751038833,1759031040,1764553479,1768681138,1772553710,1783359101,1796274970,1800398813,1805384862,1855314658,1871623920,1875334541,1898442126,1899611901,1903350459,1919824716,1929337658,1955687514,1957830399,1960249040,1961265679,1980211491,1982019059,1982981065,1991113900,1996297668,2020972183,2036846741,2040207606,2046372949,2061776001,2105836807,2107530092,2113709646,2128764374,2128829282,2136217663,2146712052,2148748122,2152730775,2187096425,2187220711,2187765774,2190505261,2192607722,2195185625,2227810019,2237409549,2242098196,2243115769,2252722101,2275290034,2277455239,2279564241,2293711953,2359122541,2360086172,2363063254,2365592879,2366502227,2369050249,2372706400,2384938876,2387692337,2389359586,2400107711,2404630687,2430528527,2437776575,2463727634,2470356848,2491474385,2492623045,2516550228,2531145747,2577405714,2596667185,2618246086,2651160602,2690141398,2714216555,2719211771,2719280629,2720968191,2725170038,2742186709,2742777480,2772316767,2804155168,2813650393,2857339760,2875199641,2890002906,2900719051,2903406022,2925323993,2928705289,2928711815,2938730026,2945588833,2962373233,2969367280,2971381129,2982213244,2982919864,2987756832,2997388429,3013060160,3040003969", 
				"TAAGGCCCGGAAA\t22470793,165825420,1174043395,1373846031,1999465518,2115455784,2117766283", 
				"TACAGAGTACTTT\t31820450,43867129,79712094,105257754,118371385,178267401,193152442,200051565,209690129,317052512,324333118,348904243,365828269,401923482,477024277,487507368,530408275,558345923,559849137,560948969,638951907,644374340,656521307,702761084,711554629,733505935,771042123,775614540,781210879,827557196,833587252,843737517,903463357,922726353,924276190,948401338,980385586,983523806,996486051,996698610,1031733533,1097523543,1142794680,1160994887,1161386544,1373846011,1444643974,1448843156,1490333816,1493352770,1511730271,1635386366,1713028537,1716434462,1723457998,1732038106,1738071948,1738922516,1757011172,1779951384,1807789009,1828814430,1836234515,1845283172,1908055448,1934402823,1955819668,1963705966,2029330709,2039466791,2081672842,2111913703,2133858881,2138360131,2150908200,2160362835,2180235745,2258035733,2258136615,2283161732,2298650403,2346119060,2357013890,2358280921,2434615048,2611728355,2622281654,2629689759,2741367620,2809798636,2828825320,2982758202", 
				"TACTCTGTACTTC\t599021,25747594,56303610,68877278,78089243,98366795,99306191,160712353,173214919,255166087,267460829,288829408,295780289,330647387,453187254,519793723,590288480,592992701,599584877,668429812,673298812,682783974,720911594,733612079,755117418,768784059,780143343,802587943,808974647,852148716,870247536,897129847,952800699,982935793,1000425440,1028152008,1039041692,1062625818,1085053524,1102688506,1109366341,1154306633,1199329742,1285231055,1331347486,1392887291,1488749441,1498529972,1554432446,1574943216,1582347597,1583047180,1583332560,1604788786,1648055539,1673980723,1707682571,1724486041,1733614912,1739385198,1745755998,1753088204,1757019578,1815973713,1902721901,1916667620,1934539826,1936948642,1970607930,2038033043,2049105366,2049923874,2055173164,2066408344,2070753233,2107512186,2118089988,2132848416,2153730365,2184397589,2192697831,2196598497,2220950328,2260344895,2297256445,2373167733,2386876746,2427270853,2498427146,2537322686,2556659736,2563998556,2573123192,2586254285,2595242580,2635667216,2654376606,2659579777,2726875049,2828778247,2855665918,2997511527,3000969348,3009959035,3011582975,3100725316", 
				"TACTTCTAAAGGC\t72385860,107885386,192101645,196572787,201170267,214607493,257400999,312669451,371969922,404758685,518417441,531822739,547749269,854459611,910132595,934538230,976948613,1020811705,1297823403,1387796235,1637826629,1682799774,1741971913,1752105732,1795883892,1831430700,1855471775,1917248801,2119755135,2141772413,2241326659,2330547839,2337667979,2337720081,2338136990,2338189045,2338376480,2340178464,2366631466,2372604684,2380240921,2382844774,2382869050,2383361510,2386313285,2390012622,2390094885,2390389147,2390479229,2392195354,2392232804,2393074423,2479642896,2489819857,2544267603,2568169997,2609269024,2645214810,2681009666,2681049726,2683559139,2912820214,3062622763,3063907214", 
				"TACTTCTCCTACT\t74501635,207467087,270382065,274979155,324241483,382697227,538963258,632687323,688253637,701767732,706889347,723106097,762317759,787295225,796769459,838340634,844279887,857086723,933625278,952945732,958488312,973758346,1178847330,1189656136,1287719944,1306277550,1307831236,1327188046,1348528277,1349237664,1471678415,1481542921,1516540429,1545154453,1625408433,1663934383,1736823550,1767610436,1861642928,1967008662,1993489519,2020251810,2137650628,2237288359,2279353531,2337152816,2357039429,2422207630,2639777542,2750565048,2818257612,2887677416,2899741487,2977921479,2998660528,3060878222", 
				"TACTTTGCCATTT\t14785670,55745102,65753268,74049196,94917383,119428593,172694512,191578467,196428375,215359875,263164751,284919217,287635997,290284790,306091440,313271916,313502937,313679268,333011628,348263215,360067872,374373583,385068324,399161553,414222989,418111245,435611440,443066501,458783924,462573477,464754127,465667472,470458648,477449417,477942501,508410396,525949562,559615408,586171713,590232297,596123647,597637458,618375159,636771893,641423096,656901078,681451709,696235063,700707437,704640220,711280529,717211803,735353115,753157892,764167088,772056686,773995096,785811748,795258674,805436363,807712138,833504236,858124509,858586723,860757739,861696331,888558683,891225985,901693964,901869682,902820233,938751136,944065601,958796143,971119605,1001036563,1007341214,1017453662,1036107949,1040626166,1058686531,1067604191,1068154408,1093004186,1100621633,1102310702,1112554090,1130319454,1136403765,1139256269,1147115548,1183597293,1209818178,1221628132,1249954770,1251203324,1253259939,1258601202,1260380843,1285975148,1324111037,1347911531,1349453929,1351997919,1373846018,1379001459,1432932071,1442740886,1470057050,1472627788,1478122186,1487395351,1500036028,1509767757,1523960179,1542704370,1544124531,1562339619,1617908634,1631125926,1679802889,1698124652,1714880650,1718799947,1739888233,1746668996,1783684960,1809437340,1829635879,1842032040,1852461185,1853152648,1861637169,1895186252,1903180809,1920408070,1946566448,1962316126,1969890555,1979085924,2014143720,2023706640,2032425174,2052308488,2066992335,2067184637,2068039062,2139382910,2157247750,2237239846,2239349247,2257811091,2259541972,2272642216,2278923527,2288720712,2343538552,2376511505,2396752162,2402700536,2419200879,2419809575,2463242615,2466672492,2472303115,2477258641,2491172910,2542079623,2566361770,2618525042,2625568424,2629315579,2634268703,2646311153,2649923083,2689367191,2704700094,2716554989,2740861346,2805701225,2806219180,2806851243,2807512670,2812103147,2816496062,2857474018,2862308866,2864331514,2879635194,2886269127,2887619118,2889967934,2899792557,2905815775,2931385059,2952562943,2953889662,2961814468,2973706948,2995778350,3026059755,3029576466,3059325404", 
				"TAGAAGTAGGAGA\t31801687,35913963,52027021,79171064,101919448,113400764,118422865,261645775,270711743,293803597,334618111,364850386,400664550,415831114,425137690,467897118,491705482,517322377,569174188,604500288,613059104,621771486,660378363,674808391,684016860,704195229,737402956,776326938,790855725,793576237,864773194,888984818,893687948,904882079,910632928,933793659,956663794,1039564511,1084038157,1110099295,1135879975,1137578140,1193302837,1207867867,1232929540,1232929672,1232929705,1232929738,1232929804,1290651308,1335349794,1342045667,1342968910,1350977704,1373845996,1407492102,1441866288,1464295310,1471006696,1577740155,1657039667,1723211966,1797177546,1835635745,1958004668,1970171889,2032251904,2056627748,2059443507,2063317473,2103815529,2124764348,2170552700,2219114632,2240403436,2249485857,2303607279,2334469407,2472230827,2476127712,2488482856,2514628074,2514789874,2528497665,2584653017,2618181760,2655771613,2719225645,2724946132,2732982369,2796074588,2810832439,2900261888,2907114399,2910443235,2935052562,2960936685,3028018150,3045004201,3057221261,3060608171", 
				"TAGGAGAAGTACA\t56344752,167258643,366859487,377839674,457525291,472256134,505008374,510357078,525283446,650554082,667437043,716058405,756607998,837974699,845192545,847020655,942245575,972109380,1097572401,1187510308,1206523907,1311670766,1373846002,1463671907,1465936437,1543733092,1546468872,1634674532,1772531815,1778476526,1840806627,1851363660,2231184293,2275746667,2533011488,2554473143,2634445283,2659712548,2804182075,2813490864", 
				"TCATTTTCCGGGC\t1361829384,2536913828,2560729018", 
				"TCCGGGCCTTAAA\t208280199,563357140,1886254173,3019029466", 
				"TCCTACTTCTAAA\t61296251,78785789,91003332,96065051,104372305,104636007,149589458,164489737,177048042,187691092,220597827,254501611,257715889,312426774,326316074,327863681,344978603,401302262,416344886,426278186,439155400,627914486,629790826,633573826,656586560,666589415,674400584,714935360,745054405,750655571,793895891,841452571,873484505,903674633,903676451,912413300,913619315,937260932,938737147,949019766,1021501833,1108175505,1112195033,1113251283,1118195835,1119932305,1158040904,1175795383,1188199956,1200223501,1208120633,1226639521,1249065879,1258374946,1261757730,1296767787,1314013738,1367631387,1450309132,1467569035,1546635874,1561145032,1562352169,1612963792,1663374895,1666947271,1685037751,1729917618,1736229654,1820265379,1820393274,1855471772,1908554857,1912970620,1931274578,2005816458,2011616555,2018781365,2035056290,2127100498,2185974654,2283649041,2353691473,2368289179,2414932609,2441377182,2518600439,2545240549,2569983389,2574368999,2582899688,2600784920,2611569653,2614837952,2679830403,2732331669,2734548475,2856527384,2861799418,2888217575,2896671488,2924326254,3017288683,3051828099,3057679604", 
				"TCGACAACCTCAT\t2893973231",
				"TCTCCTACTTCTA\t62916608,72532068,78785787,104636005,185890782,187691090,213762654,216858730,234781062,251583325,268415709,277205105,312426772,324861029,330189570,335753491,356439350,374363843,382105747,395512040,416344884,502964879,529987889,567322795,629836861,651746547,727946823,775429820,778645391,793895889,799232975,833249866,870202449,909094223,925140412,927077767,978389336,979838918,1028865953,1038421324,1081603353,1120077363,1143382201,1157246855,1159802516,1181972900,1195384645,1211185809,1212378963,1255713179,1258374944,1268461385,1296767785,1344577054,1433209267,1500565029,1533957661,1546635872,1569286705,1572723635,1682213421,1708466106,1744636207,1853635585,1875124804,1889127446,1907804831,1977520464,2028013436,2128792133,2140069731,2152121570,2221082839,2249870356,2344029010,2401912146,2550973121,2569983387,2596632118,2623069192,2691395648,2732280502,2750687967,2803871915,2806697417,2807843341,2846373880,2856037676,2978370939,3044724709,3044789060,3057030594,3060878226", 
				"TCTGTACTTCTCC\t32227099,58255998,71101628,84407803,118966462,168961802,230967768,264908765,272317179,299899711,330822413,345948187,351735008,434051462,437338605,441967055,462954985,484357499,509396324,573353089,579682755,591316167,592043264,598165731,680417856,687978908,713262268,753929275,754454972,775017936,776483669,837996113,933625274,943998009,1020336366,1023093842,1069027795,1084763117,1102104533,1120286737,1120893883,1156902532,1158419233,1172174919,1205109775,1205793986,1248581146,1274699766,1299888969,1304178440,1317392021,1372884057,1373028363,1379932392,1384776327,1402779224,1486321560,1486607470,1534814624,1545741087,1573729207,1612627069,1643973470,1651990288,1701308380,1732639097,1822584160,1831506559,1859661616,1894657345,1909196785,1942613852,1974203319,2037522352,2079369192,2109897023,2114719465,2181741618,2189593791,2231944574,2373907132,2421309375,2434604584,2475023297,2508936540,2512841469,2521734350,2564706979,2566765306,2605304494,2619840986,2701711815,2769442502,2807045944,2810140195,2862353854,2867330164,2918646711,2926757980,2936580320,2946688382,2984768186,2988392953,3009969369,3015391852,3036262159,3095635725", 
				"TGACTCGACAACC\t269275656", 
				"TGAGGTTGTCGAG\t278118867,299163610,390532522,405891202,613719810,739425921,1373846045,1526741706,2067147546,2075010564,2781364610", 
				"TGCATGACTCGAC\t168541864,844651816,2303446098", 
				"TGCCATTTTAAGG\t581917,81362674,89032557,98240214,182338667,203361223,220698988,224104072,243182432,264604905,309178754,323297460,412718577,440351497,449488809,492435524,492596262,494840869,514690654,567111534,632629400,633146260,648133191,658386942,672855353,689914486,734215057,737803603,748826676,769749432,827907043,831401430,894322836,897112050,898838722,916776351,968351209,1004000277,1009552047,1039813744,1064638730,1076569785,1081318726,1110848519,1132127479,1211370801,1232348861,1263541125,1264095916,1269049688,1273124143,1312455664,1325455563,1344111344,1353846325,1373846023,1378888715,1392870209,1413775232,1449880887,1476299561,1487952037,1497931071,1504947795,1517845337,1532094624,1565315801,1629523199,1661976684,1726512839,1727338402,1782896556,1791639124,1861899890,1937041960,1945283787,1946427710,1952423515,1956459852,2046618523,2075639370,2076326490,2174318029,2177302271,2183593864,2187217334,2220773916,2270946790,2276606588,2282504056,2539172820,2553570323,2582357659,2613301169,2639624090,2643953030,2650516231,2750374104,2757188389,2869892528,2902342717,2902753668,2913350669,2969636101,3004285904,3042855443,3042871218,3063802203", 
				"TGGCAAAGTACTC\t187999103,206248340,315729906,348983427,402241574,474193478,499867763,528714513,589370786,605117643,682590880,714808618,1026394241,1131981742,1211406208,1350573623,1403419038,1410037771,1451017678,1508722200,1562030957,1619559463,1727000686,1729187619,1729708131,1758364091,1875306083,1877650727,1928884836,2030625654,2037875273,2043673986,2061811975,2072085604,2419638755,2550452244", 
				"TGGCCTTTAGAAG\t109284633,175427709,178374732,188932586,213779066,238022647,293588460,307132343,338782097,403474702,499586068,534526174,548764220,627044321,629646345,649246227,718409144,754042653,767149881,802697547,820635137,843034367,887618445,889594013,909110862,912094861,1029956619,1035098446,1063593435,1065955066,1081190276,1083620210,1147245084,1150595788,1171530454,1172105854,1193139273,1288107005,1365812581,1373845989,1377885441,1456157409,1528697948,1635949282,1651880766,1702481613,1841857054,1877661483,1936104592,1943044313,1949091468,2039299978,2050505901,2075943375,2112248649,2173942007,2188735369,2242412910,2290280879,2345506596,2359553663,2368228019,2386358853,2391293349,2470968880,2482024187,2490293278,2551285251,2573704090,2581945378,2634999362,2656102137,2656219402,2699423457,2819536034,2858390501,2872423888,2891194772,2909633214", 
				"TGTACTTCTCCTA\t6202892,74501633,83351999,170851113,172525637,196507225,272317181,274979153,673686744,682864856,683950048,734094313,785519820,857086721,898851209,933625276,946080293,1166995344,1178847328,1219500090,1267230849,1317392023,1457114635,1488321712,1545559293,1802516434,1817302651,1913313653,2189593793,2190196235,2249828086,2254673006,2368599623,2436789316,2626864501,2632737609,2805389449", 
				"TGTCGAGTCATGC\t453384204,1373846051", 
				"TTAAAATGGCAAA\tC603", 
				"TTAAGGCCCGGAA\t1186497917,1373846030,2238902246", 
				"TTAGAAGTAGGAG\t35913962,52027020,111095969,174310608,193484313,240448920,250440496,270711742,349709203,349990190,351041774,401016079,409816233,415831113,425137689,470677489,498674442,544441286,572637905,604500287,640168265,645016305,660378362,684016859,737223230,790855724,793576236,888984817,955932144,959828939,963977131,964554286,975814264,1037521635,1082447747,1145135961,1164293810,1207867866,1290651307,1318301405,1335349793,1373845995,1407492101,1415499561,1463330585,1510935968,1636595425,1645969931,1705878986,1723211965,1797177545,1804308964,1809686018,1864039952,1911182867,2032251903,2082691038,2103815528,2124764347,2334469406,2416554326,2470874620,2472230826,2488482855,2514628073,2528497664,2572762888,2584653016,2618181759,2630316642,2636633544,2757689108,2796074587,2809514258,2868103146,2898440971,2910443234,2950675921,3013918303", 
				"TTCCGGGCCTTAA\t552164738,563357139,1886254172,3019029465", 
				"TTCTCCTACTTCT\t14210997,22855645,35840756,84268535,92184442,97061935,101819548,120564411,150935286,151206640,163033082,179479221,187691089,203019915,206802874,212974954,213762653,251583324,256027103,278286395,284728229,290998375,307585608,318012529,330189569,331613806,371137685,374363842,377119410,382105746,383678014,395512039,400122169,476023111,476673430,478230115,487600687,515366705,518725174,531239374,556335785,596307847,600613619,629836860,646870343,657287102,667412872,675612252,727946822,763173255,769174223,778344201,785028489,791667424,820560900,824757392,830428443,844735957,848791545,865330555,873765103,886879875,912853434,933482293,976633548,991621909,993085404,1025148894,1028865952,1046299664,1054785055,1116460179,1143382200,1156070858,1156730706,1157246854,1161624017,1164388204,1166195173,1181972899,1195384644,1206138482,1210962054,1211185808,1212378962,1242311445,1254208948,1255713178,1260679550,1261161026,1281200040,1296767784,1322309745,1326911485,1333260053,1361649981,1367500673,1409354202,1410921108,1433209266,1481542924,1485504108,1491482159,1496365971,1497157679,1499042761,1500565028,1500944695,1514831198,1527932921,1558633394,1564455008,1569286704,1701914154,1736188853,1741848185,1745046372,1804799371,1819600840,1821157529,1837619807,1843448947,1907804830,1927456137,1928596917,1961039164,1977520463,1979440743,2031152770,2031511027,2043774736,2046536279,2064051030,2068856299,2080413505,2114415014,2118975178,2125208996,2131319346,2142187595,2194805716,2196174372,2221082838,2238861791,2245064702,2297925349,2347275634,2349551041,2401912145,2420501126,2420906807,2465175483,2470471175,2476038424,2491611725,2507932801,2510407729,2514722794,2531052533,2531833293,2548534430,2550973120,2553428801,2582736750,2584527077,2596632117,2618141840,2623069191,2682879491,2736491014,2750687966,2808725721,2848237482,2852559154,2881381882,2889623997,2898254245,2935341300,2948359737,2976751734,2978370938,2990431755,3035231416,3036602442,3044637465,3044724708,3044789059,3044987054,3055726951,3057030593,3060878225", 
				"TTGCCATTTTAAG\t581916,73982633,76842205,98240213,169095356,177246767,199102177,200676758,203361222,209474252,216347292,224104071,230325417,243182431,244015200,252937044,257505061,278328814,304097829,309178753,311022202,323297459,349188725,364280864,376130484,378012966,388477076,394656081,397260094,397457900,399161557,404285051,419271835,446604985,449066525,461886739,464891536,492435523,494840868,505799520,511603066,514690653,518714031,523709101,544745600,551318982,552163434,559505375,567111533,578099305,600668590,602711321,606254368,619632813,632629399,633146259,636771897,648942994,663590725,665549376,701295694,710707919,714419255,748099200,748826675,753284531,764598114,771380840,783131564,789240859,799179300,808324817,818968347,824760532,836940732,846839392,861255082,861468893,900278281,907765619,916776350,944209984,980903885,982025357,989918925,1009552046,1055067174,1064638729,1083203988,1091782975,1110848518,1116071650,1121171083,1124631934,1125441322,1132127478,1156973676,1164280386,1175093282,1177990792,1242797202,1250155190,1258604705,1260029814,1260478250,1312455663,1314612796,1316521393,1344111343,1345591561,1351973690,1357787430,1373846022,1378549635,1387285427,1392870208,1402806726,1449880886,1452350584,1465068716,1476299560,1477563294,1478068926,1487869653,1497907621,1497931070,1500259378,1504947794,1506246160,1517845336,1519123596,1534673723,1545993543,1559147131,1612627176,1697884175,1726512838,1766129734,1767427323,1782896555,1793312707,1793489406,1809390921,1831413295,1897108533,1920558310,1929395350,1937041959,1940150502,1945283786,1951250739,1952423514,2018264241,2025362521,2032846731,2034074183,2034373793,2048525101,2106605083,2106740381,2120124872,2143887092,2146789308,2161315959,2167060141,2174318028,2183593863,2187217333,2219018940,2229005700,2241694952,2262816936,2283502786,2345099759,2352142344,2373796318,2395027378,2409740597,2425872690,2437346240,2463222945,2479545750,2482852446,2496916124,2511334708,2553570322,2561339191,2564900184,2568315212,2582357658,2584270058,2614532728,2630360616,2639624089,2649592053,2651512139,2735326678,2742018994,2764242149,2769076263,2775921674,2777339177,2797220431,2812953756,2819311602,2846994089,2877623031,2882601075,2893368423,2902482952,2926246332,2990318557,3004285903,3012227824,3037821635,3063802202", 
				"TTGTCGAGTCATG\t453384203,964358990,1113188012,1373846050,2133813245,2388028068", 
				"TTTAAGGCCCGGA\t535173947,1373846029,1612121348,1682855759,2121378737,2764247311",
				"TTTAGAAGTAGGA\t35913961,52027019,70340015,71941321,98267520,143731469,144327797,144508048,147918973,162078577,180316432,193484312,222815914,224339444,237193672,239161024,255011876,314930798,351041773,374816831,428710253,433198664,572637904,602102069,640168264,645016304,677941258,684016858,723670442,793576235,796927080,800458874,811626607,822241854,856820188,888984816,918577022,939693676,963977130,964554285,967857652,975814263,1004400566,1025002276,1033107710,1085431878,1102088833,1128546145,1135019739,1145135960,1152296850,1164293809,1165014447,1169432930,1190269283,1203005082,1207867865,1210975563,1226695653,1249363644,1290651306,1318301404,1335349792,1373845994,1444712518,1463330584,1498019841,1534749376,1545157725,1617274628,1647524010,1733112476,1736636283,1744710891,1790635719,1809686017,1820165382,1820210801,1842530992,1864039951,2016061261,2023938397,2119814216,2251156515,2281436573,2406742832,2413874445,2460654720,2470874619,2516865609,2566350296,2589448946,2618027147,2618181758,2636633543,2654877299,2679798632,2709767202,2808245371,2821450186,2884716116,2899622270,2906194568,2970454269,3039908132,3055607288", 
				"TTTCCGGGCCTTA\t1886254171,2252142122,2414194547", 
				"TTTGCCATTTTAA\tC658", 
				"TTTTAAGGCCCGG\t154145644,454184152,1373846028,1394565872,1682855758,2121378736,2764247310", 
				"TTTTCCGGGCCTT\t1195230672,1311880758,1415258475,1706303175,1815347693,2069227270,2136632579,2414194546,2588559461");
	}

}
