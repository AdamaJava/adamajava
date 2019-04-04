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
	      QSVParameters tumor = TestUtil.getQSVParameters(testfolder, tumourFile, tumourFile, true, "both", "both");
	      QSVParameters normal = TestUtil.getQSVParameters(testfolder, tumourFile, tumourFile, false, "both", "both");
	      
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
    		SAMFileHeader h = sort == SortOrder.coordinate ? SOLID_SAM_FILE_HEADER_COORDINATE_SORTED : SOLID_SAM_FILE_HEADER_QUERY_NAME_SORTED;
    		List<SAMRecord> recs = getAACSAMRecords(sort);
    		
    		SAMFileWriterFactory factory = new SAMFileWriterFactory();
    		factory.setCreateIndex(true);
    		try (SAMFileWriter writer = factory.makeBAMWriter(h, false, new File(inputFileName))) {
    		
	    		for (SAMRecord r: recs) {
	    			writer.addAlignment(r);
	    		}
    		}
    	} else {
    		List<SAMRecord> recs = createSamBodyRecords(sort);
    		SAMFileHeader h = sort == SortOrder.coordinate ? SOLID_SAM_FILE_HEADER_COORDINATE_SORTED : SOLID_SAM_FILE_HEADER_QUERY_NAME_SORTED;
			SAMFileWriterFactory factory = new SAMFileWriterFactory();
			factory.setCreateIndex(true);
			try (SAMFileWriter writer = factory.makeBAMWriter(h, false, new File(inputFileName))) {
				for (SAMRecord r: recs) {
					writer.addAlignment(r);
				}
			}
	    }
        
		return new File(inputFileName);
    }
	
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


	public static Set<Clip> getRightClips(boolean nClips) {
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

	public static Set<Clip> getLeftClips(boolean nClips)  {
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
	      QSVParameters tumor = TestUtil.getQSVParameters(testFolder, tumourFile, tumourFile, true, "both", "both");
	      QSVParameters normal = TestUtil.getQSVParameters(testFolder, tumourFile, tumourFile, false, "both", "both");    
	      String query = "Cigar_M > 35 and option_SM > 14 and MD_mismatch < 3 and Flag_DuplicateRead == false ";
	      DiscordantPairCluster cluster = new DiscordantPairCluster("chr10", "chr10", "AAC", tumor, true);	      
	
	      for (MatePair p : pairs) {
	          cluster.getClusterMatePairs().add(p);
	      }
	      
	      if ( ! clusterType.equals("somatic")) {
	    	  	cluster.getMatchedReadPairs().add(pairs.get(0));
	      }
	      
	      cluster.setClusterEnds();
	      cluster.setNormalRange(3000);
	      cluster.finalize(tumor, normal, clusterType, 1, query, "pe", true);
	      cluster.getqPrimerCateory().setPrimaryCategoryNo(qPrimerCategory);
	      return cluster;
	}

}
