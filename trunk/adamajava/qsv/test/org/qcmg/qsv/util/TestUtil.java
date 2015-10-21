package org.qcmg.qsv.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import org.junit.rules.TemporaryFolder;
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

	public static String[] getValidOptions(final TemporaryFolder testFolder,
            final String normalBam, final String tumorBam, final String preprocessMode,
            final String analysisMode) throws IOException {
			String iniFile = setUpIniFile(testFolder, preprocessMode, analysisMode, normalBam, tumorBam, 3, 1, testFolder.getRoot().toString(), testFolder.getRoot().toString());
			
            return new String[] {"--ini", iniFile, "--tmp",  testFolder.getRoot().toString()};
    }

	public static String[] getInvalidOptions(final TemporaryFolder testFolder,
            final String normalBam, final String tumorBam,
            final String mode, Integer clusterSize, Integer filterSize, String tmpDir, String outputDir) throws IOException {
       
			String iniFile = setUpIniFile(testFolder, "both", mode, normalBam, tumorBam, clusterSize, filterSize, tmpDir, outputDir);
			
            return new String[] {"--ini", iniFile, "--tmp",  testFolder.getRoot().toString()};
    }
	
	private static String setUpIniFile(final TemporaryFolder testFolder, String preprocessMode,
            final String analysisMode, String normalBam, String tumorBam, Integer clusterSize, Integer filterSize, String tmpDir, String outputDir) throws IOException {
		
		File iniFile = testFolder.newFile("test.ini");
		File reference = testFolder.newFile("reference_file");
		if (iniFile.exists()) {
			iniFile.delete();
		}		
		
		BufferedWriter out = new BufferedWriter(new FileWriter(iniFile));
		out.write("[general]" + NEWLINE);
		out.write("log=test.log" + NEWLINE);
		out.write("loglevel=DEBUG" + NEWLINE);
		out.write("sample=test" + NEWLINE);
		out.write("platform=solid" + NEWLINE);		
		out.write("sv_analysis="+analysisMode+"" + NEWLINE);		
		out.write("output="+outputDir+"" + NEWLINE);
		out.write("reference=" + reference.getAbsolutePath() + NEWLINE);
		out.write("isize_records=all" + NEWLINE);
		out.write("qcmg=true" + NEWLINE);
		
		out.write("[pair]" + NEWLINE);
		out.write("pair_query=and(Cigar_M > 35,option_SM > 14,MD_mismatch < 3,Flag_DuplicateRead == false)" + NEWLINE);
		out.write("pairing_type=lmp" + NEWLINE);
		out.write("cluster_size="+clusterSize+"" + NEWLINE);
		out.write("filter_size="+filterSize+"" + NEWLINE);
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
		out.write("input_file="+tumorBam+"" + NEWLINE);
		if (preprocessMode.equals("none")) {
			out.write("discordantpair_file="+tumorBam+"" + NEWLINE);
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
		out.write("input_file="+normalBam+"" + NEWLINE);
		if (preprocessMode.equals("none")) {
			out.write("discordantpair_file="+normalBam+"" + NEWLINE);
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
    	out.close();
		return iniFile.getAbsolutePath();
	}

	public static DiscordantPairCluster setupSolidCluster(PairGroup zp, String clusterType, TemporaryFolder testfolder, String chr1, String chr2) throws IOException, Exception {
		  List<MatePair> pairs = setupMatePairs(testfolder, zp);
		  String query = "Cigar_M > 35 and option_SM > 14 and MD_mismatch < 3 and Flag_DuplicateRead == false ";
		  String tumourFile = testfolder.newFile("tumor.bam").getAbsolutePath();
		  createBamFile(tumourFile, zp, SortOrder.coordinate);
		  String normalFile = testfolder.newFile("normal.bam").getAbsolutePath();
		  createBamFile(normalFile, zp, SortOrder.coordinate);
	      QSVParameters tumor = TestUtil.getQSVParameters(testfolder, tumourFile, normalFile, true, "both", "both");
	      QSVParameters normal = TestUtil.getQSVParameters(testfolder, tumourFile, normalFile, false, "both", "both");
	      
	      DiscordantPairCluster cluster = new DiscordantPairCluster(chr1, chr2, zp.getPairGroup(), tumor, true);	      
	
	      for (MatePair p : pairs) {
	          cluster.getClusterMatePairs().add(p);
	      }
	      
	      if (!clusterType.equals("somatic")) {
	    	  cluster.getMatchedReadPairs().add(pairs.get(0));
	      }
	      
	      cluster.setClusterEnds();
	      cluster.setNormalRange(3000);
	      cluster.finalize(tumor, normal, clusterType, 1, query, "lmp", true);
	      return cluster;
	}

    
    public static List<MatePair> setupMatePairs(TemporaryFolder testFolder, PairGroup pg) throws IOException, QSVException {
    	List<SAMRecord> records = new ArrayList<SAMRecord>();
        List<MatePair> pairs = new ArrayList<MatePair>();
        String fileName = testFolder.newFile("test.bam").getCanonicalPath();
        
        TestUtil.createBamFile(fileName, pg, SortOrder.queryname);
        
        SAMFileReader read = new SAMFileReader(new File(fileName));
        
        for (SAMRecord r : read) {
            records.add(r);
        }
        
        pairs.add(new MatePair(records.get(0), records.get(1)));
        pairs.add(new MatePair(records.get(2), records.get(3)));
        pairs.add(new MatePair(records.get(4), records.get(5)));
        pairs.add(new MatePair(records.get(6), records.get(7)));
        pairs.add(new MatePair(records.get(8), records.get(9)));
        pairs.add(new MatePair(records.get(10), records.get(11)));
        read.close();
        return pairs;
	}
    
	public static QSVParameters getQSVParameters(final TemporaryFolder testFolder, final String normalBam, final String tumorBam,
            final boolean isTumor, final String analysisMode) throws Exception {
        final String FILE_SEPERATOR = System.getProperty("file.separator");
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
        final String FILE_SEPERATOR = System.getProperty("file.separator");
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
        final String seperator = System.getProperty("file.separator");
        final String fullname = file.getPath();

        final String name = fullname.substring(
                fullname.lastIndexOf(seperator) + 1, fullname.length());

        return name;
    }

    public static String getFilename(final String fullname) {
        final String seperator = System.getProperty("file.separator");
        final String name = fullname.substring(
                fullname.lastIndexOf(seperator) + 1, fullname.length());

        return name;
    }
    
	public static File createHiseqBamFile(final String inputFileName, PairGroup pg, SortOrder sort) throws IOException {
		String samFile = inputFileName.replace("bam", "sam");
    	if (pg != null) {
    		createSamFile(samFile, pg, sort, true);
    	} else {
    		createSamFile(samFile, sort, true);
    	}
    	
        SAMFileReader reader = new SAMFileReader(new File(samFile));
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
    
    public static File createBamFile(final String inputFileName, PairGroup pg, SortOrder sort)
            throws IOException {
    
    	String samFile = inputFileName.replace("bam", "sam");
    	if (pg != null) {
    		createSamFile(samFile, pg, sort, false);
    	} else {
    		createSamFile(samFile, sort, false);
    	}
    	
        SAMFileReader reader = new SAMFileReader(new File(samFile));
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
	
    public static File createSamFile(final String inputFileName, SortOrder sort, boolean isHiseq)
            throws IOException {
    
        final List<String> data = new ArrayList<String>();
        // create sam header and records
        if (!isHiseq) {
        	data.addAll(createSamHeader(sort));
        	data.addAll(createSamBody());
        } else {
        	data.addAll(createHiseqSamHeader(sort));
        	data.addAll(createHiseqSamBody());
        }
        BufferedWriter out;
        out = new BufferedWriter(new FileWriter(inputFileName));
        for (final String line : data) {
            out.write(line + "" + NEWLINE);
        }
        out.close();
		return new File(inputFileName);
    }
	 
	 private static Collection<? extends String> createHiseqSamBody() {
		List<String> data = new ArrayList<String>();

		data = createHiseqAACSamBody();
         
        
        return data;

	}

	public static File createSamFile(final String inputFileName, PairGroup pg, SortOrder sort, boolean isHiseq)
	            throws IOException {
	        final List<String> data = new ArrayList<String>();
	        // create sam header and records
	        if (!isHiseq) {
	        	data.addAll(createSamHeader(sort));
	        } else {
	        	data.addAll(createHiseqSamHeader(sort));
	        }
	        
	        if (!isHiseq) {
		        if (pg.equals(PairGroup.AAC)) {
		        	data.addAll(createAACSamBody());	    
		        } else if (pg.equals(PairGroup.Cxx)) {
		        	data.addAll(createCxxSamBody());
		        } else {
		        	//throw new QSVException();
		        }
	        } else {
	        	if (pg.equals(PairGroup.AAC)) {
		        	data.addAll(createHiseqAACSamBody());	    
		        } else if (pg.equals(PairGroup.Cxx)) {
		        	data.addAll(createCxxSamBody());
		        } else {
		        	//throw new QSVException();
		        }
	        }
	       
	        BufferedWriter out;
	        out = new BufferedWriter(new FileWriter(inputFileName));
	        for (final String line : data) {
	            out.write(line + "" + NEWLINE);
	        }
	        out.close();
	        return new File(inputFileName);
	 }
	 


	private static Collection<String> createHiseqSamHeader(
			SortOrder sort) {
		 final List<String> data = new ArrayList<String>();
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

	private static List<String> createCxxSamBody() {
		 final List<String> data = new ArrayList<String>();
         return data;
	}

	public static List<String> createAACSamBody() {
		final List<String> data = new ArrayList<String>();
		 
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
		 final List<String> data = new ArrayList<String>();			 																												 												
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
	 

	 public static List<String> createSamBody(PairGroup pg) {
	        final List<String> data = new ArrayList<String>();

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

		public static List<String> createSamHeader(SortOrder sort) {
	        final List<String> data = new ArrayList<String>();
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

	    public static List<String> createSamBody() {
	        final List<String> data = new ArrayList<String>();

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
	    
	    public static SAMFileHeader getSAMHeader(String file) throws IOException {
			final List<String> data = new ArrayList<String>();
	        // create sam header and records
	        data.addAll(createSamHeader(SortOrder.unsorted));	     
	       
	        try (BufferedWriter out = new BufferedWriter(new FileWriter(file));) {
		        for (final String line : data) {
		            out.write(line + "" + NEWLINE);
		        }
	        }
	        
	        SAMFileHeader header = null;
	        try (SAMFileReader sam = new SAMFileReader(new File(file));) {
	        		header =  sam.getFileHeader();
	        }
	        return header;
		}

		public static String createTmpClusterFile(String dirName, PairClassification pc, String fileName) throws IOException {
			
			File testDir = new File(dirName + pc.getPairingClassification());
			testDir.mkdir();
			String outFile = testDir + FILE_SEPERATOR + fileName;
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outFile)));) {
			
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
			//writer.write("1822_622_785:20110221052813657,chr7,140198994,140199037,AAC,113,R1R2,true,1822_622_784:20110221052813657,chr7,140201589,140120629,AAC,177,R2R1,true" + NEWLINE);
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
			List<MatePair> pairs = new ArrayList<MatePair>();
			BufferedReader reader = new BufferedReader(new FileReader(matePairsFile));
			
			String line = reader.readLine();
			
			while (line != null) {
				pairs.add(new MatePair(line));
				
				line = reader.readLine();
			}
			
			reader.close();
			Collections.sort(pairs, new MatePair.ReadMateLeftStartComparator());
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
			
			HashSet<Clip> clips;
			if (isLeft) {
				clips = getLeftClips(nCount);	
				breakpoint = new Breakpoint(89712341, "chr10", isLeft, consensus, 50);
								
			} else {
				clips=getRightClips(nCount);
				breakpoint = new Breakpoint(89700299, "chr10", isLeft, consensus, 50);
			}
			for (Clip c : clips) {
				breakpoint.addTumourClip(c);
			}
//			breakpoint.setTumourClips(clips);
			if (isGermline) {
				if (isLeft) {
					for (Clip c : getLeftClips(false)) {
						breakpoint.addNormalClip(c);
					}
//					breakpoint.setNormalClips(getLeftClips(false));
				} else {
					for (Clip c : getRightClips(false)) {
						breakpoint.addNormalClip(c);
					}
//					breakpoint.setNormalClips(getRightClips(false));
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


		private static HashSet<Clip> getRightClips(boolean nClips) {
			HashSet<Clip> clips = new HashSet<Clip>();
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10,89700299,-,right,GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG") + ",GCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:6:2301:10241:71660:20120608110941621,chr10,89700299,-,right,ATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTG," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTG") + ",ATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:1110:20608:86188:20120608092353631,chr10,89700299,-,right,CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC") + ",CAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:1:1204:3577:34360:20120608115535190,chr10,89700299,-,right,AGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGC," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGC") + ",AGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:5:2113:4661:50103:20120607102754932,chr10,89700299,-,right,GATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGT," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGT") + ",GATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:1114:3101:51165:20120608092353631,chr10,89700299,-,right,TCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGG," + (nClips ? "NNNNNNNNNNNNNNN" : "GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGG") + ",TCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAA"));

//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10,89700299,-,right,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAG"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:6:2301:10241:71660:20120608110941621,chr10,89700299,-,right,GAGATTATACTTTG"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:1110:20608:86188:20120608092353631,chr10,89700299,-,right,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGC"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:1:1204:3577:34360:20120608115535190,chr10,89700299,-,right,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGC"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:5:2113:4661:50103:20120607102754932,chr10,89700299,-,right,GAGATTATACTTTGTGTAAGAGGT"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:1114:3101:51165:20120608092353631,chr10,89700299,-,right,GAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGG"));
			
			return clips;
		}
		
		public static Clip getClip(String strand, String side) {
			
			String line = "HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10,89700299,"+strand+","+side+",ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAACTGN,ACTGN,ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACA";
			return new Clip(line);
		}
		
		public static Clip getClip(String strand, String side, int position) {
			
			String line = "HWI-ST1240:47:D12NAACXX:1:2307:8115:32717:20120608115535190,chr10," + position + ","+strand+","+side+",ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAACTGN,ACTGN,ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACA";
			return new Clip(line);
		}
		
		public static Clip getClip(String strand, String side, String name) {
			
			String line = name + ",chr10,89700299,"+strand+","+side+",ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAACTGN,ACTGN,ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACA";
			return new Clip(line);
		}

		private static HashSet<Clip> getLeftClips(boolean nClips) {
			HashSet<Clip> clips = new HashSet<Clip>();	
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:7:2210:12278:86346:20120608113919562,chr10,89712341,+,left,ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT," + (nClips ? "NNNNNNNNNNNNNNN" : "ACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:5:2311:7722:24906:20120607102754932,chr10,89712341,+,left,TCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA," + (nClips ? "NNNNNNNNNNNNNNN" : "TCTCTTTGTGTAAGAGATTATACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGA"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:2105:19785:71299:20120608092353631,chr10,89712341,+,left,AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGG," + (nClips ? "NNNNNNNNNNNNNNN" : "AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGG"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:7:2305:11547:56681:20120608113919562,chr10,89712341,+,left,ACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT," + (nClips ? "NNNNNNNNNNNNNNN" : "ACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGGTATTGACAGTAAT"));
			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:8:2107:14006:55890:20120608020343585,chr10,89712341,+,left,TGTAAGAGATTATACTTTGTGTAAGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGG," + (nClips ? "NNNNNNNNNNNNNNN" : "TGTAAGAGATTATACTTTGTGTA") + ",AGAGGTCCACCAGAGGAGTTCAGCAATTTGCTGCTCTTAGGGCAGGGATCAATTCCTTAATATCTTAGGAAGACTAGG"));

//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:7:2210:12278:86346:20120608113919562,chr10,89712341,+,left,ACTTTGTGTA"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:5:2311:7722:24906:20120607102754932,chr10,89712341,+,left,TCTCTTTGTGTAAGAGATTATACTTTGTGTA"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:4:2105:19785:71299:20120608092353631,chr10,89712341,+,left,AAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTATACTTTGTGTA"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:7:2305:11547:56681:20120608113919562,chr10,89712341,+,left,ACTTTGTGTA"));
//			clips.add(new Clip("HWI-ST1240:47:D12NAACXX:8:2107:14006:55890:20120608020343585,chr10,89712341,+,left,TGTAAGAGATTATACTTTGTGTA"));
			return clips;
		}

		public static DiscordantPairCluster setupHiseqCluster(String clusterType, TemporaryFolder testFolder, String qPrimerCategory) throws IOException, Exception {
			  List<MatePair> pairs = new ArrayList<MatePair>();
		      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:7:1112:14008:49131:20120608113919562,chr10,89700049,89700149,AAC,97,false,HWI-ST1240:47:D12NAACXX:7:1112:14008:49131:20120608113919562,chr10,89712348,89712448,AAC,145,true,F1R2"));
		      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:4:1304:9295:38861:20120608092353631,chr10,89700053,89700153,AAC,97,false,HWI-ST1240:47:D12NAACXX:4:1304:9295:38861:20120608092353631,chr10,89712340,89712440,AAC,145,true,F1R2"));
		      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:7:1309:3779:55661:20120608113919562,chr10,89700060,89700160,AAC,97,false,HWI-ST1240:47:D12NAACXX:7:1309:3779:55661:20120608113919562,chr10,89712340,89712440,AAC,145,true,F1R2"));
		      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:6:2109:5161:48848:20120608110941621,chr10,89700064,89700164,AAC,97,false,HWI-ST1240:47:D12NAACXX:6:2109:5161:48848:20120608110941621,chr10,89712346,89712446,AAC,145,true,F1R2"));
		      pairs.add(new MatePair("HWI-ST1240:47:D12NAACXX:8:1109:11792:69957:20120608020343585,chr10,89700200,89700300,AAC,161,false,HWI-ST1240:47:D12NAACXX:8:1109:11792:69957:20120608020343585,chr10,89712446,89712546,AAC,81,true,F2R1"));
		      String tumourFile = testFolder.newFile("tumor.bam").getAbsolutePath();
			  createHiseqBamFile(tumourFile, PairGroup.AAC, SortOrder.coordinate);
			  String normalFile = testFolder.newFile("normal.bam").getAbsolutePath();
			  createHiseqBamFile(normalFile, PairGroup.AAC, SortOrder.coordinate);
		      QSVParameters tumor = TestUtil.getQSVParameters(testFolder, tumourFile, normalFile, true, "both", "both");
		      QSVParameters normal = TestUtil.getQSVParameters(testFolder, tumourFile, normalFile, false, "both", "both");    
		      String query = "Cigar_M > 35 and option_SM > 14 and MD_mismatch < 3 and Flag_DuplicateRead == false ";
		      DiscordantPairCluster cluster = new DiscordantPairCluster("chr10", "chr10", "AAC", tumor, true);	      
		
		      for (MatePair p : pairs) {
		          cluster.getClusterMatePairs().add(p);
		      }
		      
		      if (!clusterType.equals("somatic")) {
		    	  cluster.getMatchedReadPairs().add(pairs.get(0));
		      }
//		      
		      cluster.setClusterEnds();
		      cluster.setNormalRange(3000);
		      cluster.finalize(tumor, normal, clusterType, 1, query, "pe", true);
		      cluster.getqPrimerCateory().setPrimaryCategoryNo(qPrimerCategory);
		      return cluster;
		}





}
