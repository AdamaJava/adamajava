package org.qcmg.qbamfilter.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import htsjdk.samtools.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMWriterFactory;
import org.qcmg.qbamfilter.filter.TestFile;

public class QueryExecutorTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    private static final String INPUT_FILE_NAME = "input.sam";

    /**
     * Create an testing input sam file
     */
    @BeforeClass
    public static void before()throws Exception{
        createSamFile();
    }
    
    @AfterClass
    public static void after1(){
          new File(TestFile.INPUT_FILE_NAME).delete();
     }
	
    @Test
    public void testValid() throws Exception{
        //there are two reads satisfied below query
        String query = "or (option_ZM == 1 , OPTION_ZM >= 100)";
        int r = checkRecord(query);
        assertEquals(2, r);

        //there are no reads satisfied below query
        query = "and (mapq > 16 ,cigar_M >= 40 , or (option_ZM == 0 , option_ZM > 200))";
        r = checkRecord(query);
        assertEquals(0, r);

        //there are two reads satisfy below query
        query = "opTIOn_ZM < 200";
        r = checkRecord(query);
        assertEquals(1, r);
        
        query = "Flag_DuplicateRead == false";
        r = checkRecord(query);
        assertEquals(3, r );
       
    }

    /**
     * picard won't treat a read with cigar "42M8S" but it is 8 base hardclip, as invalid read
     * So here we only test invalid flag and Mate information
     */
    @Test
    public void InvalidFlag() throws Exception{
        String  query = "and (mapq > 16,cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
        QueryExecutor myExecutor = new QueryExecutor(query);
        try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME))) {

            int i = 0;
            for (SAMRecord record : reader) {
                if (i == 2) {
                    record.setFlags(7);
                    assertFalse(myExecutor.Execute(record));
                    break;
                }
                i++;
            }
        }
    }
    
    @Test
    public void nullOrEmptyQuery() throws Exception {
    		try {
    			new QueryExecutor(null);
    			Assert.fail("Should have thrown an exception");
    		} catch (IllegalArgumentException aie) {}
    		try {
    			new QueryExecutor("");
    			Assert.fail("Should have thrown an exception");
    		} catch (IllegalArgumentException aie) {}
    }
    
    @Test
    public void queryContainsQuotes() throws Exception {
    		// if the query passed to QueryExecutor contains quotes, then antlr with eventually give an OOM error
    	
        String  query = "\"and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))\"";
  		try {
  			QueryExecutor myExecutor = new QueryExecutor(query);
    			Assert.fail("Should have thrown an exception");
    		} catch (IllegalArgumentException aie) {}
  		 query = "and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))\"";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
     			Assert.fail("Should have thrown an exception");
     		} catch (IllegalArgumentException aie) {}
   		query = "\"and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		query = "and (mapq > 16 , cigar_M >= \"40\" , or (option_ZM == 0, option_ZM > 200))";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		
   		
   		query = "'and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))'";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		query = "and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))'";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		query = "'and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
   		query = "and (mapq > 16 , cigar_M >= '40' , or (option_ZM == 0, option_ZM > 200))";
   		try {
   			QueryExecutor myExecutor = new QueryExecutor(query);
   			Assert.fail("Should have thrown an exception");
   		} catch (IllegalArgumentException aie) {}
    	
    }
    @Test
    public void queryContainsDodgyCharacters() throws Exception {
    	// if the query passed to QueryExecutor contains quotes, then antlr with eventually give an OOM error
    	
        String  query = "\\ and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
        try {
            QueryExecutor myExecutor = new QueryExecutor(query);
            Assert.fail("Should have thrown an exception");
        } catch (IllegalArgumentException aie) {}

        query = "and mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
        try {
            QueryExecutor myExecutor = new QueryExecutor(query);
            Assert.fail("Should have thrown an exception");
        } catch (RuntimeException aie) {}
    	
    }

    /**
     * test invalid read: Flag is 0 but it's mate mapping positon is 100
     */
    @Test
    public void invalidMate() throws Exception{
        String  query = "and (mapq > 16 , cigar_M >= 40 , or (option_ZM == 0, option_ZM > 200))";
        QueryExecutor myExecutor = new QueryExecutor(query);
        try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME))) {

            int i = 0;
            for (SAMRecord record : reader) {
                if (i == 1) {
                    record.setFlags(0);
                    record.setMateAlignmentStart(100);
                    assertFalse(myExecutor.Execute(record));
                    break;
                }
                i++;
            }
        }
    }

    @Test
    public void mrnmRNAME() throws Exception{
        String  query = " and (rname != mrnm, mrnm != *)";
        QueryExecutor myExecutor = new QueryExecutor(query);
        try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME))) {

            int i = 0;
            for (SAMRecord record : reader) {
                if (i == 1) {
                    //origial return False since chr1 equal '='
                    assertFalse(myExecutor.Execute(record));

                    //return True since chr1 unequal chr11
                    record.setMateReferenceName("chr11");
                    assertTrue(myExecutor.Execute(record));

                    //create a valid read with unmapped Mate; return False since mrnm equal '*'
                    record.setMateReferenceName("*");
                    record.setMateAlignmentStart(0);
                    record.setFlags(123);
                    assertFalse(myExecutor.Execute(record));
                }
                i++;
            }
        }
    }

    @Test
    public void mdTagInCram() throws Exception {
        File cramFile = testFolder.newFile("mdTagInCram.cram");
        File cramIndexFile = testFolder.newFile("mdTagInCram.cram.bai");
        File refFile = testFolder.newFile("ref.fa");
        File refIndexFile = testFolder.newFile("ref.fa.fai");
        setupReferenceFile(refFile, refIndexFile);

        /*
        setup CRAM file
         */
        getCramFile(cramFile, true, true, true, testFolder.newFolder(), refFile);

        String  query = "MD_mismatch <= 3";
        System.setProperty("samjdk.reference_fasta", refFile.getAbsolutePath());
        QueryExecutor myExecutor = new QueryExecutor(query);
        try (SamReader reader = SAMFileReaderFactory.createSAMFileReaderAsStream(cramFile, cramIndexFile, refFile,
                ValidationStringency.STRICT, null )) {

            int passesFilterCount = 0;
            for (SAMRecord record : reader) {
                if (myExecutor.Execute(record)) {
                    passesFilterCount++;
                }
            }
            assertEquals(4, passesFilterCount);
        }
    }

    static SAMFileHeader getHeader(boolean valid) {
        SAMFileHeader header = new SAMFileHeader();
        header.setTextHeader(VALID_HEADER);
        header.setSortOrder(SAMFileHeader.SortOrder.coordinate);

        SAMProgramRecord bwaPG = new SAMProgramRecord("bwa");
        bwaPG.setProgramName("bwa");
        bwaPG.setProgramVersion("0.6.1-r104");
        header.addProgramRecord(bwaPG);
//		"@PG	ID:bwa	PN:bwa	zc:6	VN:0.6.1-r104"+

        if ( ! valid) {
            SAMProgramRecord invalidPG = new SAMProgramRecord("blah");
            invalidPG.setAttribute("CL", "");
            header.addProgramRecord(invalidPG);

            SAMReadGroupRecord rgRec = new SAMReadGroupRecord("ID");
            rgRec.setAttribute("PG", "tmap");
            header.addReadGroup(rgRec);
        }

        // looks like we need this to be specifically defined
        SAMSequenceDictionary seqDict = new SAMSequenceDictionary();
        SAMSequenceRecord seqRec1 = new SAMSequenceRecord("chr1", 249250621);
        SAMSequenceRecord seqRec2 = new SAMSequenceRecord("chr2", 243199373);
        SAMSequenceRecord seqRec3 = new SAMSequenceRecord("chr3", 198022430);
        SAMSequenceRecord seqRec4 = new SAMSequenceRecord("chr4", 191154276);
        SAMSequenceRecord seqRec5 = new SAMSequenceRecord("chr5", 180915260);
        seqDict.addSequence(seqRec1);
        seqDict.addSequence(seqRec2);
        seqDict.addSequence(seqRec3);
        seqDict.addSequence(seqRec4);
        seqDict.addSequence(seqRec5);
        header.setSequenceDictionary(seqDict);

        return header;
    }

    static void getCramFile(File bamFile, boolean validHeader, boolean validRecords, boolean createIndex, File tmpDir, File ref) {
        SAMFileHeader header = getHeader(validHeader);
        SAMWriterFactory factory = new SAMWriterFactory(header, false, bamFile, tmpDir, 10000, createIndex, false, 10000, ref);
        try (SAMFileWriter writer = factory.getWriter()) {
            for (SAMRecord s : getRecords(validRecords,header)) {
                writer.addAlignment(s);
            }
        }
    }

    static List<SAMRecord> getRecords(boolean valid, SAMFileHeader header) {
        List<SAMRecord> records = new ArrayList<>();
//		records.add("HS2000-152_756:1:1316:11602:65138	89	chr1	9993	25	100M	=	9993	0	TCTTCCGATCTCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?=7D9@@8.C;B8=.HGDBBBCCD::*GGD:?*FDGFCA>EIHEEBEAEFDFFC=+?DD@@@	X0:i:1	X1:i:0	ZC:i:9	MD:Z:0C0T0G6A0A89	PG:Z:MarkDuplicates	RG:Z:20130325103517169	XG:i:0	AM:i:0	NM:i:5	SM:i:25	XM:i:5	XN:i:8	XO:i:0	XT:A:U");
        SAMRecord sam = new SAMRecord(header);
        sam.setAlignmentStart(9993);
        sam.setReferenceName("chr1");
        sam.setReadName("HS2000-152_756:1:1316:11602:65138");
        sam.setReadString("TCTTCCGATCTCCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA");
        sam.setBaseQualityString("B@??BBCCB<>BCBB?:BAA?9-A;?2;@ECA=;7BEE?=7D9@@8.C;B8=.HGDBBBCCD::*GGD:?*FDGFCA>EIHEEBEAEFDFFC=+?DD@@@");
        sam.setCigarString("100M");
        sam.setAttribute("MD", "100");
        records.add(sam);

//		records.add("HS2000-152_756:2:1212:5424:43221	99	chr1	10001	29	45M1I14M4D9M2D21M10S	=	10101	199	TAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCCCACCCCCTACCCCACACTCACCCACCCCCTAACCTCAGCACCCC	CCCFFFFFFHHHHGGIJJIIJJJJJJJJJJGEHIJJ9)?)?D))?(?BFB;CD@C#############################################	ZC:i:7	MD:Z:54T0A3^CTAA5A3^TA2C2A4T0A0A8	PG:Z:MarkDuplicates	RG:Z:20130325112045146	XG:i:7	AM:i:29	NM:i:15	SM:i:29	XM:i:8	XO:i:3	XT:A:M");
        sam = new SAMRecord(header);
        sam.setAlignmentStart(10001);
        sam.setReferenceName("chr1");
        sam.setReadName("HS2000-152_756:2:1212:5424:43221");
        sam.setReadString("TAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCCCACCCCCTACCCCACACTCACCCACCCCCTAACCTCAGCACCCC");
        sam.setBaseQualityString("CCCFFFFFFHHHHGGIJJIIJJJJJJJJJJGEHIJJ9)?)?D))?(?BFB;CD@C#############################################");
        sam.setCigarString("45M1I14M4D9M2D21M10S");
        sam.setAttribute("MD", "100");
        records.add(sam);

//		records.add("HS2000-152_757:7:1311:15321:98529	163	chr1	10002	0	100M	=	10020	118	AACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACACTAACCCTAACCCTAACCCTAACCCTAACCCTAACC	@@@FFDFFB;DFHIHIIIIJGICGGGGGGF?9CF;@?DD;BDGG2DEFGC9EDHHI@CCEEFE)=?33;6;6;@AA;=A?2<?((59(9<<((28?<?B8	X0:i:362	ZC:i:8	MD:Z:63C36	PG:Z:MarkDuplicates	RG:Z:20130325084856212	XG:i:0	AM:i:0	NM:i:1	SM:i:0	XM:i:1	XO:i:0	XT:A:R");
        sam = new SAMRecord(header);
        sam.setAlignmentStart(10002);
        sam.setReferenceName("chr1");
        sam.setReadName("HS2000-152_757:7:1311:15321:98529");
        sam.setReadString("AACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACACTAACCCTAACCCTAACCCTAACCCTAACCCTAACC");
        sam.setBaseQualityString("@@@FFDFFB;DFHIHIIIIJGICGGGGGGF?9CF;@?DD;BDGG2DEFGC9EDHHI@CCEEFE)=?33;6;6;@AA;=A?2<?((59(9<<((28?<?B8");
        sam.setCigarString("100M");
        sam.setAttribute("MD", "100");
        records.add(sam);

//		records.add("HS2000-152_756:2:2306:7001:4421	99	chr1	10003	29	2S53M1I44M	=	10330	426	TGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA	CCCFFFFFHHHHHIJJJJEHGIIJIHGIIJIFGIJJJGGHHIIGCDGIHI>GIIEAFGJJI@EGFDFCE@DDDE@CA=A;3;?BDB?CD@DB9ADDBA9?	ZC:i:7	MD:Z:5A5A5A5A5A5A5A5A49	PG:Z:MarkDuplicates	RG:Z:20130325112045146	XG:i:1	AM:i:29	NM:i:9	SM:i:29	XM:i:8	XO:i:1	XT:A:M");
        sam = new SAMRecord(header);
        sam.setAlignmentStart(10003);
        sam.setReferenceName("chr1");
        sam.setReadName("HS2000-152_756:2:2306:7001:4421");
        sam.setReadString("TGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTGACCCTAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA");
        sam.setBaseQualityString("CCCFFFFFHHHHHIJJJJEHGIIJIHGIIJIFGIJJJGGHHIIGCDGIHI>GIIEAFGJJI@EGFDFCE@DDDE@CA=A;3;?BDB?CD@DB9ADDBA9?");
        sam.setCigarString("2S53M1I44M");
        sam.setAttribute("MD", "100");
        records.add(sam);

//		records.add("HS2000-152_756:1:1215:14830:88102	99	chr1	10004	29	24M4D76M	=	10441	537	CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC	CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB	ZC:i:9	MD:Z:5A5A5A6^CCCT54A11A9	PG:Z:MarkDuplicates	RG:Z:20130325103517169	XG:i:4	AM:i:29	NM:i:9	SM:i:29	XM:i:5	XO:i:1	XT:A:M");
        sam = new SAMRecord(header);
        sam.setAlignmentStart(10004);
        sam.setReferenceName("chr1");
        sam.setReadName("HS2000-152_756:1:1215:14830:88102");
        sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
        sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
        sam.setCigarString("24M4D76M");
        sam.setAttribute("MD", "10A10C10G10T");
        records.add(sam);

        if ( ! valid) {
            sam = new SAMRecord(header);
            sam.setAlignmentStart(10005);
            sam.setReferenceName("chr1");
            sam.setReadName("HS2000-152_756:1:1215:14830:88103");
            sam.setReadString("CCCTACCCCTACCCCTACCCCTAAAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTTACCCTAACCCTTACCCTAACC");
            sam.setBaseQualityString("CCCFFFFFHHHHHJJIHFHJIGIHGIIJJJJIJJIIIIJJJJJIIJJJJIJJJJJJJJHHGHHFFEFCEEDD9?BABDCDDDDDDDDDDDDCDDDDDDDB");
            sam.setCigarString("24M4D76M");
            sam.setReadUnmappedFlag(true);
            sam.setMappingQuality(1);
            records.add(sam);
        }

        return records;
    }



    private int checkRecord(String query) throws Exception {
        int r = 0;
        QueryExecutor myExecutor = new QueryExecutor(query);
        try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(TestFile.INPUT_FILE_NAME))) {

            for (SAMRecord record : reader) {
                if (myExecutor.Execute(record)) {
                    r++;
                }
            }
        }
        return r;
    }
    
    private static void createSamFile() throws Exception{
        //create sam header and records
        List<String> data = new ArrayList<>(createSamHeader());
        data.add("@CO	create by TagValueFilterTest::CreateSamHeader");
        data.addAll(createSamBody());

        //add above contend into a new sam file

        BufferedWriter out;
        out = new BufferedWriter(new FileWriter(INPUT_FILE_NAME));
        for (String line : data) {
            out.write(line + "\n");
        }
        out.close();
   }

    private static List<String> createSamHeader(){
        List<String> data = new ArrayList<>();
        data.add("@HD	VN:1.0	SO:coordinate");
        data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
        data.add("@PG	ID:SOLID-GffToSam	VN:1.4.3");
        data.add("@SQ	SN:chr1	LN:249250621");
        data.add("@SQ	SN:chr11	LN:243199373");
        return data;
    }

    private static List<String> createSamBody(){
    		List<String> data = new ArrayList<>();
		data.add("970_1290_1068	163	chr1	10176	3	42M8H	=	10167	-59	" +
				"AACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	I&&HII%%IIII4CII=4?IIF0B((!!7F@+129G))I>.6	RG:Z:1959T	" +
				"CS:Z:G202023020023010023010023000.2301002302002330000000	CQ:Z:@A&*?=9%;?:A-(<?8&/1@?():(9!,,;&&,'35)69&)./?11)&=	ZM:i:1");
		data.add("681_1482_392	115	chr1	10236	20	10H40M	=	10242	56	" +
				"AACCCTAACCCTAAACCCTAAACCCTAACCCTAACCCTAA	IIIIIIIIEBIIIIFFIIIIIIIIIIIIIIIIIIIIIIII	RG:Z:1959T	" +
				"CS:Z:T00320010320010320010032001003200103200100320000320	CQ:Z::<=>:<==8;<;<==9=9?;5>8:<+<;795.89>2;;8<:.78<)1=5;	ZM:i:200");
		data.add("1997_1173_1256	177	chr1	10242	100	22H28M	chr1	10236	0	" +
				"AACCCTAAACCCTAAACCCTAACCCTAA	IIII27IICHIIIIHIIIHIIIII$$II	RG:Z:1959T	" +
				"CS:Z:G10300010320010032001003000100320000032000020001220	CQ:Z:5?8$2;>;:458=27597:/5;7:2973:3/9;18;6/:5+4,/85-,'(");
		return data;
    }

    private static final String VALID_HEADER = "@HD	VN:1.4	GO:none	SO:coordinate"+
            "@SQ	SN:chr1	LN:249250621"+
            "@SQ	SN:chr2	LN:243199373"+
            "@SQ	SN:chr3	LN:198022430"+
            "@SQ	SN:chr4	LN:191154276"+
            "@SQ	SN:chr5	LN:180915260"+
            "@SQ	SN:chr6	LN:171115067"+
            "@SQ	SN:chr7	LN:159138663"+
            "@SQ	SN:chr8	LN:146364022"+
            "@SQ	SN:chr9	LN:141213431"+
            "@SQ	SN:chr10	LN:135534747"+
            "@SQ	SN:chr11	LN:135006516"+
            "@SQ	SN:chr12	LN:133851895"+
            "@SQ	SN:chr13	LN:115169878"+
            "@SQ	SN:chr14	LN:107349540"+
            "@SQ	SN:chr15	LN:102531392"+
            "@SQ	SN:chr16	LN:90354753"+
            "@SQ	SN:chr17	LN:81195210"+
            "@SQ	SN:chr18	LN:78077248"+
            "@SQ	SN:chr19	LN:59128983"+
            "@SQ	SN:chr20	LN:63025520"+
            "@SQ	SN:chr21	LN:48129895"+
            "@SQ	SN:chr22	LN:51304566"+
            "@RG	ID:20130325090048212	PL:ILLUMINA	PU:lane_8.nobc	LB:LP6005273-DNA_G04	zc:6:/path/project2/SSSS_076/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.bam	SM:SSSS_076	PG:qbamfix	CN:QCMG"+
            "@RG	ID:20130325112045146	PL:ILLUMINA	PU:lane_2.nobc	LB:LP6005273-DNA_G04	zc:7:/path/project2/SSSS_076/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.bam	SM:SSSS_076	PG:qbamfix	CN:QCMG"+
            "@RG	ID:20130325084856212	PL:ILLUMINA	PU:lane_7.nobc	LB:LP6005273-DNA_G04	zc:8:/path/project2/SSSS_076/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.bam	SM:SSSS_076	PG:qbamfix	CN:QCMG"+
            "@RG	ID:20130325103517169	PL:ILLUMINA	PU:lane_1.nobc	LB:LP6005273-DNA_G04	zc:9:/path/project2/SSSS_076/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.bam	SM:SSSS_076	PG:qbamfix	CN:QCMG"+
            "@PG	ID:0ee948df-cf28-4456-be94-15b3aa9ed7b4	PN:qbammerge	zc:10	VN:0.6pre (6216)	CL:qbammerge --output /scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam --log /scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam.qmrg.log --input /path/project2/SSSS_076/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.bam --input /path/project2/SSSS_076/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.bam --input /path/project2/SSSS_076/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.bam --input /path/project2/SSSS_076/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.bam"+
            "@PG	ID:7022afd6-d0fb-47b9-90d0-c6f3ef0f98e6	PN:qbamfix	zc:9	VN:qbamfix, version 0.2pre	CL:qbamfix --input /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.sam --output /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.fixrg.bam --log /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_1.nobc.fix.log --RGLB LP6005273-DNA_G04 --RGSM SSSS_076 --tmpdir /scratch/329416.machine"+
            "@PG	ID:9b400d65-3b8d-4f98-b246-ba4280b916ae	PN:qbamfix	zc:7	VN:qbamfix, version 0.2pre	CL:qbamfix --input /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.sam --output /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.fixrg.bam --log /path/130207_SN152_0756_AC1PJYACXX/130207_SN152_0756_AC1PJYACXX.lane_2.nobc.fix.log --RGLB LP6005273-DNA_G04 --RGSM SSSS_076 --tmpdir /scratch/329421.machine"+
            "@PG	ID:9c662b98-b27f-4a94-bd76-d1937d33ab9c	PN:qbamfix	zc:8	VN:qbamfix, version 0.2pre	CL:qbamfix --input /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.sam --output /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.fixrg.bam --log /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_7.nobc.fix.log --RGLB LP6005273-DNA_G04 --RGSM SSSS_076 --tmpdir /scratch/329449.machine"+
            "@PG	ID:MarkDuplicates	PN:MarkDuplicates	zc:5	VN:1.88(1394)	QN:picard_markdups	CL:htsjdk.samtools.sam.MarkDuplicates INPUT=[/scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.withdups.bam] OUTPUT=/scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam METRICS_FILE=/scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam.dedup_metrics COMMENT=[CN:QCMG"+
            "@PG	ID:ad74545e-b36f-4587-aa46-812baa87f467	PN:qbamfix	zc:6	VN:qbamfix, version 0.2pre	CL:qbamfix --input /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.sam --output /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.fixrg.bam --log /path/130207_SN152_0757_BD1U57ACXX/130207_SN152_0757_BD1U57ACXX.lane_8.nobc.fix.log --RGLB LP6005273-DNA_G04 --RGSM SSSS_076 --tmpdir /scratch/329452.machine"+
            "@PG	ID:bwa	PN:bwa	zc:6	VN:0.6.1-r104"+
            "@PG	ID:bwa.1	PN:bwa	zc:7	VN:0.6.1-r104"+
            "@PG	ID:bwa.2	PN:bwa	zc:8	VN:0.6.1-r104"+
            "@PG	ID:bwa.3	PN:bwa	zc:9	VN:0.6.1-r104"+
            "@PG	ID:d0cac0d3-3318-42bc-91d2-1d5abc92ab80	PN:qbammerge	zc:11	VN:0.6pre (6239)	CL:qbammerge --output /path/project2/SSSS_076/folder/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.bam --log /path/project2/SSSS_076/folder/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.bam.log --co CN:QCMG QN:qbammaker CL:-v --auto /path/project2/SSSS_076 --logfile SSSS_076.auto.log --co CN:QCMG QN:qlimsmeta Aligner=bwa Capture Kit=kit1 Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=HiSeq Species Reference Genome=Homo sapiens (reference) --co CN:QCMG QN:qmapset Aligner=bwa Capture Kit= Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Mapset=130207_SN152_0756_AC1PJYACXX.lane_1.nobc Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=Plat1 Species Reference Genome=Homo sapiens (reference) --co CN:QCMG QN:qmapset Aligner=bwa Capture Kit= Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Mapset=130207_SN152_0757_BD1U57ACXX.lane_8.nobc Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=Plat1 Species Reference Genome=Homo sapiens (reference) --co CN:QCMG QN:qmapset Aligner=bwa Capture Kit= Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Mapset=130207_SN152_0757_BD1U57ACXX.lane_7.nobc Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=Plat1 Species Reference Genome=Homo sapiens (reference) --co CN:QCMG QN:qmapset Aligner=bwa Capture Kit= Donor=SSSS_076 Failed QC=0 Library Protocol=protocol1 Mapset=130207_SN152_0756_AC1PJYACXX.lane_2.nobc Material=1:DNA Project=project2 Reference Genome File=/path/REF.fa Sample=SAMPLE-20130205-035 Sample Code=7:Primary tumour Sequencing Platform=Plat1 Species Reference Genome=Homo sapiens (reference) --input /scratch/332212.machine/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam"+
            "@CO	CN:QCMG	QN:picard_markdups	CL:OPTICAL_DUPLICATE_PIXEL_DISTANCE=10 VALIDATION_STRINGENCY=SILENT INPUT=${SCRATCH_DIR}/DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.withdups.bam OUTPUT=${SCRATCH_DIR}/SSSS076_1DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam METRICS_FILE=${SCRATCH_DIR}/SSSS076_1DNA_7PrimaryTumour_IDBPC20130205035_out_kit1_Bwa_HiSeq.Library_NEW5709A939AR2.bam.dedup_metrics"+
            "@CO	CN:QCMG	QN:qbammaker	CL:-v --auto /path/project2/SSSS_076 --logfile SSSS_076.auto.log"+
            "@CO	CN:QCMG	QN:qlimsmeta	Aligner=bwa	Capture Kit=kit1	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=HiSeq	Species Reference Genome=Homo sapiens (reference)"+
            "@CO	CN:QCMG	QN:qmapset	Aligner=bwa	Capture Kit=	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Mapset=130207_SN152_0756_AC1PJYACXX.lane_1.nobc	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=Plat1	Species Reference Genome=Homo sapiens (reference)"+
            "@CO	CN:QCMG	QN:qmapset	Aligner=bwa	Capture Kit=	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Mapset=130207_SN152_0757_BD1U57ACXX.lane_8.nobc	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=Plat1	Species Reference Genome=Homo sapiens (reference)"+
            "@CO	CN:QCMG	QN:qmapset	Aligner=bwa	Capture Kit=	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Mapset=130207_SN152_0757_BD1U57ACXX.lane_7.nobc	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=Plat1	Species Reference Genome=Homo sapiens (reference)"+
            "@CO	CN:QCMG	QN:qmapset	Aligner=bwa	Capture Kit=	Donor=SSSS_076	Failed QC=0	Library Protocol=protocol1	Mapset=130207_SN152_0756_AC1PJYACXX.lane_2.nobc	Material=1:DNA	Project=project2	Reference Genome File=/path/REF.fa	Sample=SAMPLE-20130205-035	Sample Code=7:Primary tumour	Sequencing Platform=Plat1	Species Reference Genome=Homo sapiens (reference)";

    private void setupReferenceFile(File file, File indexFile) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file));) {
            writer.write(">chr1\n");
            writer.write("GATCACAGGTCTATCACCCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGG");
            writer.write("GTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTC");
            writer.write("CTGCCTCATCCTATTATTTATCGCACCTACGTTCAATATTACAGGCGAACATACTTACTAAAGTGTGTTA");
            writer.write("ATTAATTAATGCTTGTAGGACATAATAATAACAATTGAATGTCTGCACAGCCACTTTCCACACAGACATC");
            writer.write("ATAACAAAAAATTTCCACCAAACCCCCCCTCCCCCGCTTCTGGCCACAGCACTTAAACACATCTCTGCCA");
            writer.write("AACCCCAAAAACAAAGAACCCTAACACCAGCCTAACCAGATTTCAAATTTTATCTTTTGGCGGTATGCAC");
            writer.write("TTTTAACAGTCACCCCCCAACTAACACATTATTTTCCCCTCCCACTCCCATACTACTAATCTCATCAATA");
            writer.write("CAACCCCCGCCCATCCTACCCAGCACACACACACCGCTGCTAACCCCATACCCCGAACCAACCAAACCCC");
            writer.write("AAAGACACCCCCCACAGTTTATGTAGCTTACCTCCTCAAAGCAATACACTGAAAATGTTTAGACGGGCTC");
            writer.write("ACATCACCCCATAAACAAATAGGTTTGGTCCTAGCCTTTCTATTAGCTCTTAGTAAGATTACACATGCAA");
            writer.write("GCATCCCCGTTCCAGTGAGTTCACCCTCTAAATCACCACGATCAAAAGGAACAAGCATCAAGCACGCAGC");
            writer.write("AATGCAGCTCAAAACGCTTAGCCTAGCCACACCCCCACGGGAAACAGCAGTGATTAACCTTTAGCAATAA");
            writer.write("ACGAAAGTTTAACTAAGCTATACTAACCCCAGGGTTGGTCAATTTCGTGCCAGCCACCGCGGTCACACGA");
            writer.write("TTAACCCAAGTCAATAGAAGCCGGCGTAAAGAGTGTTTTAGATCACCCCCTCCCCAATAAAGCTAAAACT");
            writer.write("CACCTGAGTTGTAAAAAACTCCAGTTGACACAAAATAGACTACGAAAGTGGCTTTAACATATCTGAACAC");
            writer.write("ACAATAGCTAAGACCCAAACTGGGATTAGATACCCCACTATGCTTAGCCCTAAACCTCAACAGTTAAATC");
            writer.write("AACAAAACTGCTCGCCAGAACACTACGAGCCACAGCTTAAAACTCAAAGGACCTGGCGGTGCTTCATATC");
            writer.write("CCTCTAGAGGAGCCTGTTCTGTAATCGATAAACCCCGATCAACCTCACCACCTCTTGCTCAGCCTATATA");
            writer.write("CCGCCATCTTCAGCAAACCCTGATGAAGGCTACAAAGTAAGCGCAAGTACCCACGTAAAGACGTTAGGTC");
            writer.write("AAGGTGTAGCCCATGAGGTGGCAAGAAATGGGCTACATTTTCTACCCCAGAAAACTACGATAGCCCTTAT");
            writer.write("GAAACTTAAGGGTCGAAGGTGGATTTAGCAGTAAACTAAGAGTAGAGTGCTTAGTTGAACAGGGCCCTGA");
            writer.write("AGCGCGTACACACCGCCCGTCACCCTCCTCAAGTATACTTCAAAGGACATTTAACTAAAACCCCTACGCA");
            writer.write("TTTATATAGAGGAGACAAGTCGTAACATGGTAAGTGTACTGGAAAGTGCACTTGGACGAACCAGAGTGTA");
            writer.write("GCTTAACACAAAGCACCCAACTTACACTTAGGAGATTTCAACTTAACTTGACCGCTCTGAGCTAAACCTA");
            writer.write("GCCCCAAACCCACTCCACCTTACTACCAGACAACCTTAGCCAAACCATTTACCCAAATAAAGTATAGGCG");
            writer.write("ATAGAAATTGAAACCTGGCGCAATAGATATAGTACCGCAAGGGAAAGATGAAAAATTATAACCAAGCATA");
            writer.write("ATATAGCAAGGACTAACCCCTATACCTTCTGCATAATGAATTAACTAGAAATAACTTTGCAAGGAGAGCC");
            writer.write("AAAGCTAAGACCCCCGAAACCAGACGAGCTACCTAAGAACAGCTAAAAGAGCACACCCGTCTATGTAGCA");
            writer.write("AAATAGTGGGAAGATTTATAGGTAGAGGCGACAAACCTACCGAGCCTGGTGATAGCTGGTTGTCCAAGAT");
            writer.write("AGAATCTTAGTTCAACTTTAAATTTGCCCACAGAACCCTCTAAATCCCCTTGTAAATTTAACTGTTAGTC");
            writer.write("CAAAGAGGAACAGCTCTTTGGACACTAGGAAAAAACCTTGTAGAGAGAGTAAAAAATTTAACACCCATAG");
            writer.write("TAGGCCTAAAAGCAGCCACCAATTAAGAAAGCGTTCAAGCTCAACACCCACTACCTAAAAAATCCCAAAC");
            writer.write("ATATAACTGAACTCCTCACACCCAATTGGACCAATCTATCACCCTATAGAAGAACTAATGTTAGTATAAG");
            writer.write("TAACATGAAAACATTCTCCTCCGCATAAGCCTGCGTCAGATTAAAACACTGAACTGACAATTAACAGCCC");
            writer.write("AATATCTACAATCAACCAACAAGTCATTATTACCCTCACTGTCAACCCAACACAGGCATGCTCATAAGGA");
            writer.write("AAGGTTAAAAAAAGTAAAAGGAACTCGGCAAATCTTACCCCGCCTGTTTACCAAAAACATCACCTCTAGC");
            writer.write("ATCACCAGTATTAGAGGCACCGCCTGCCCAGTGACACATGTTTAACGGCCGCGGTACCCTAACCGTGCAA");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(indexFile));) {
            writer.write("chr1\t16571\t7\t50\t51\n");
        }
    }
}
