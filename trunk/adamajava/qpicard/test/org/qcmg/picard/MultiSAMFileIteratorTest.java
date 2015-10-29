package org.qcmg.picard;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class MultiSAMFileIteratorTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();

	public void createFile(String name) {
		try {
			File file = new File(name);

			OutputStream os = new FileOutputStream(file);
			PrintStream ps = new PrintStream(os);

			ps.println("@HD	VN:1.0	SO:coordinate");
			ps.println("@RG	ID:ES	SM:ES	DS:rl=50	");
			ps.println("@RG	ID:EST	SM:ES	DS:rl=50	");
			ps.println("@PG	ID:SOLID-GffToSam	VN:1.4.3");
			ps.println("@SQ	SN:chr1	LN:249250621");
			ps.println("@SQ	SN:chr2	LN:243199373");
			ps.println("@SQ	SN:chr3	LN:198022430");
			ps.println("@SQ	SN:chr4	LN:191154276");
			ps.println("@SQ	SN:chr5	LN:180915260");
			ps.println("@SQ	SN:chr6	LN:171115067");
			ps.println("@SQ	SN:chr7	LN:159138663");
			ps.println("@SQ	SN:chr8	LN:146364022");
			ps.println("@SQ	SN:chr9	LN:141213431");
			ps.println("@SQ	SN:chr10	LN:135534747");
			ps.println("@SQ	SN:chr11	LN:135006516");
			ps.println("@SQ	SN:chr12	LN:133851895");
			ps.println("@SQ	SN:chr13	LN:115169878");
			ps.println("@SQ	SN:chr14	LN:107349540");
			ps.println("@SQ	SN:chr15	LN:102531392");
			ps.println("@SQ	SN:chr16	LN:90354753");
			ps.println("@SQ	SN:chr17	LN:81195210");
			ps.println("@SQ	SN:chr18	LN:78077248");
			ps.println("@SQ	SN:chr19	LN:59128983");
			ps.println("@SQ	SN:chr20	LN:63025520");
			ps.println("@SQ	SN:chr21	LN:48129895");
			ps.println("@SQ	SN:chr22	LN:51304566");
			ps.println("@SQ	SN:chrX	LN:155270560");
			ps.println("@SQ	SN:chrY	LN:59373566");
			ps.println("@SQ	SN:chrM	LN:16571");
			ps.println("1858_2026_1766	0	chr1	10148	255	40M10H	*	0	0	CCCTAACCCTAACCCTAACCCTAAAAATAACCTCCCCCTA	!DDDDD8DDDDCBDDDC2@DD0\"%%%%%6<C:'&&&D<)!	RG:Z:EST	CS:Z:T2002301002301002301002320003301022000023	CQ:Z:7733:>(180:9+8239+(9<)(&853%+,13()'&08%%");
			ps.println("229_1153_1818	16	chr1	10169	255	50M	*	0	0	NAACCCTAACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA	!1*>C\"\"\";><)6931*BD%%DA8C5>DDDDDDDDDDDDDDDDDDDDDD!	RG:Z:ES	CS:Z:T00320010320010320010320010320000320010320100000100	CQ:Z:5?@><<9=B5:<:>;+<>>:-:99&04%=,%<>%&,(2%%8'5(5*:%&,");
			ps.println("2173_1119_350	0	chr1	10223	255	35M15H	*	0	0	AACCCTAACCCCTAACCCTAACCCTAAACCCTAAA	!DDDDDDDDDDDDDDDDA-D++D<9DD4?DA+;D!	RG:Z:ES	CS:Z:T30100230100023010023000023000002300	CQ:Z:;=@A;<>B+AB@;6=9?;''A+A8%5:4A?=%'55");
			ps.println("610_1918_617	16	chr1	20108	255	5H45M	*	0	0	NTAAGCACTTAGAAAAAGCCGCGGTGAGTCCCAGGGGCCAGCACT	!3''D<%&=DB2*@D:+:DDDDDDDC?AA;>DBDDDD>9DDD>C!	RG:Z:ES	CS:Z:T321132103000210021221103330320000223022132232	CQ:Z:4<(7<:3'85=>.57(4.4,8</858<6%'4;&%.53+&%84'+)");
			ps.println("2100_823_972	0	chr1	30908	255	50M	*	0	0	ATTTCTCTCTCTCTCGCTATCTCATTTTTCTCTCTCTCTCTTTCTCTCCT	!DDDDDDCDDCDDDDDDDDD=ADDDDDDDDDA>DDD%%DD>6CDDDCDD!	RG:Z:ES	CS:Z:T33002222222222233233222130000222222220222002222202	CQ:Z::>>>528-7;22;>;2<4?@5)97;.=9<<:9)6=;9%975*-7;:22:5");
			ps.println("601_1983_251	16	chr1	53185	255	10H40M	*	0	0	TCCTATGCCATGTATATTTCTGTATGTGTAGCCTTTGCCT	!&&&:7D++DDD9<DDDA.5DDDDDDDDDDDDDDDDD''!	RG:Z:ES	CS:Z:T3213100203231111331122003333113123133202	CQ:Z:.9'94>9/9>::7;8=96750&)9<77&4=<-+73%6&/&");
			ps.println("1290_738_1025	0	chr1	54026	255	45M5H	*	0	0	AACATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTG	!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDD:<3B''DDD!	RG:Z:ES	CS:Z:T301130201000212101113201021003302230033233111	CQ:Z:BBB=B:@5?>B9A5?>B?'A49<475%@;6<+;9@'4)+8'1?:>");
			ps.println("340_760_182	16	chr1	54441	255	5H45M	*	0	0	TTTTTTCTCTTAATTGCTGCTAATCTCCCCCATCTTCAAATACTC	!D@6DDC>%%C515DD((**DDDD=>DDDDD>DD=DDDDDDC<D!	RG:Z:ES	CS:Z:T122133001202231000022230322103103030022200000	CQ:Z:4;5(<942463+:2-=;=32-1?79.*.(:50&,*:%+40=,+68");
			ps.println("823_901_1499	16	chr1	54441	255	5H45M	*	0	0	TTTTTTCCTTTAATTGCTGCTAATCTCCCCCATCTTCAAATACTC	!DDBD<%%%-''/)8C8-3BD@9B6CDDDDD;36DD>DDDDDCD!	RG:Z:ES	CS:Z:T122133001202231000022230323123103000020200000	CQ:Z:899+<82;2-92%/-==>=;).5%<4/%)04%%+'<2-%&7/475");
			ps.println("1185_1382_175	16	chr1	54446	255	10H40M	*	0	0	TCTCCCGATTGCTGCTAATCTCCCCCATCTTCAAATACTC	!@9%%%%%\"\"=B7(+DDDDDDDDDDDDDBDDDD@CDDDD!	RG:Z:ES	CS:Z:T1221330012022310000222303221231222300222	CQ:Z:2=37=8,5:37/4:+:2:7:3487.7+(03+'%4+9%'3.");

			ps.close();
			os.close();

		} catch (Exception e) {
			System.err.println("File creation error in test harness: "
					+ e.getMessage());
		}
	}

	@Before
	public final void before() {
		createFile("testfile1.sam");
		createFile("testfile2.sam");
		createFile("testfile3.sam");
	}

	public void deleteFile(final String name) {
		File file = new File(name);
		file.delete();
	}

	@After
	public final void after() {
		deleteFile("testfile1.sam");
		deleteFile("testfile2.sam");
		deleteFile("testfile3.sam");
	}

	@Test
	public final void singleFileIteration() throws Exception {
		HashSet<File> files = new HashSet<File>();

		File file1 = new File("testfile1.sam");
		files.add(file1);

		MultiSAMFileReader reader = new MultiSAMFileReader(files);

		int count = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			count++;
		}

		assertTrue(10 == count);
	}

	@Test
	public final void dualFileIteration() throws Exception {
		HashSet<File> files = new HashSet<File>();

		File file1 = new File("testfile1.sam");
		File file2 = new File("testfile2.sam");

		files.add(file1);
		files.add(file2);

		MultiSAMFileReader reader = new MultiSAMFileReader(files);

		int count = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			count++;
		}

		assertTrue(20 == count);
	}

	@Test
	public final void tripleFileIteration() throws Exception {
		HashSet<File> files = new HashSet<File>();

		File file1 = new File("testfile1.sam");
		File file2 = new File("testfile2.sam");
		File file3 = new File("testfile3.sam");

		files.add(file1);
		files.add(file2);
		files.add(file3);

		MultiSAMFileReader reader = new MultiSAMFileReader(files);

		int count = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			count++;
			//for debuging
			if(count % 6 == 0)
				System.out.println(record.getIntegerAttribute("zc"));
		}
		
		System.out.println("counts: " + count);
		
		assertTrue(30 == count);
	}
	
//	@Ignore
	@Test
	public void testListInsteadOfSet() throws IOException {
		int number = 1000000;
		
		File testInputFile = testFolder.newFile("testing123");
		
		HashSet<SAMRecordWrapper> set = new HashSet<SAMRecordWrapper>();
		long start = System.currentTimeMillis();
		SAMRecord rec = new SAMRecord(null);
		SamReader reader = SAMFileReaderFactory.createSAMFileReader( testInputFile ); //new SAMFileReader(testInputFile);
		Iterator<SAMRecord> iterator = reader.iterator();
		for (int i = 0 ; i < number ; i++) {
			SAMRecordWrapper temp = new SAMRecordWrapper(rec, iterator, reader);
			set.add(temp);
			set.remove(temp);
		}
		System.out.println("time taken for set: " + (System.currentTimeMillis() - start));
		
		List<SAMRecordWrapper> list = new ArrayList<SAMRecordWrapper>();
		start = System.currentTimeMillis();
//		SAMRecord rec = new SAMRecord(null);
//		SamReader reader = new SAMFileReader(testInputFile);
//		Iterator<SAMRecord> iterator = reader.iterator();
		for (int i = 0 ; i < number ; i++) {
			SAMRecordWrapper temp = new SAMRecordWrapper(rec, iterator, reader);
			list.add(temp);
			list.remove(temp);
		}
		System.out.println("time taken for list: " + (System.currentTimeMillis() - start));
		
		
		 set = new HashSet<SAMRecordWrapper>();
		start = System.currentTimeMillis();
		
		for (int i = 0 ; i < number ; i++) {
			SAMRecordWrapper temp = new SAMRecordWrapper(rec, iterator, reader);
			set.add(temp);
			set.remove(temp);
		}
		System.out.println("time taken for set: " + (System.currentTimeMillis() - start));
	}
}
