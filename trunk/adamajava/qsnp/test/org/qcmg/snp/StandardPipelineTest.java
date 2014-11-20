package org.qcmg.snp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.model.Accumulator;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

public class StandardPipelineTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testRunStandardMode() throws Exception{
		File logFile = testFolder.newFile("qsnp.log");
		File normalBam = createCoverageSam("normal.bam");
		File tumourBam = createTumourCoverageSam("tumour.bam");
		File reference = createRefFile("StandardPipelineTest.reference.fa");
		File ini = testFolder.newFile("ini.ini");
		File vcf = testFolder.newFile("output.vcf");
		IniFileGenerator.createRulesOnlyIni(ini);
		IniFileGenerator.addInputFiles(ini, false, "ref = " + reference.getAbsolutePath());
		IniFileGenerator.addInputFiles(ini, false, "controlBam = " + normalBam.getAbsolutePath());
		IniFileGenerator.addInputFiles(ini, false, "testBam = " + tumourBam.getAbsolutePath());
//		IniFileGenerator.addStringToIniFile(ini, "[filter]\nquery = \"Flag_DuplicateRead == false\"", true);
		IniFileGenerator.addOutputFiles(ini, false, "vcf = " + vcf.getAbsolutePath());
		IniFileGenerator.addStringToIniFile(ini, "[parameters]\nrunMode = standard", true);
//		IniFileGenerator.addStringToIniFile(ini, "\nannotateMode = dcc", true);
		
//		new StandardPipeline(new Ini(ini));
		
		String command = "-log " + logFile.getAbsolutePath() + " -i " + ini.getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.snp.Main");
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
	}
	
	private int noOfColumnsInDCCOutputFile(File dccFile) throws Exception {
		TabbedFileReader reader = new TabbedFileReader(dccFile);
		try {
			for (TabbedRecord vcf : reader) {
				if (vcf.getData().startsWith("analysis")) continue;	// header line
				String[] data = vcf.getData().split("\t");
				return data.length;
			}
		} finally {
			reader.close();
		}
		return -1;
	}
	
	@Ignore
	public void testContainsAndRemove() {
		ConcurrentMap<Integer, Accumulator> map = new ConcurrentHashMap<Integer, Accumulator>();
		int noOfLoops = 100000;
		long time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			map.remove(i);
		}
		System.out.println("EMPTY: remove time: " + (System.currentTimeMillis() - time));
		
		time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			if (map.containsKey(i))
				map.remove(i);
		}
		System.out.println("EMPTY: contains and remove time: " + (System.currentTimeMillis() - time));
		
		time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			map.remove(i);
		}
		System.out.println("EMPTY: remove time: " + (System.currentTimeMillis() - time));
	}
	
	@Ignore
	public void testContainsAndRemoveFullMap() {
		ConcurrentMap<Integer, Accumulator> map = new ConcurrentHashMap<Integer, Accumulator>();
		
		int noOfLoops = 100000;
		for (int i = 0 ; i < noOfLoops ; i ++) {
			map.put(i, new Accumulator(i));
		}
		
		
		long time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			map.remove(i);
		}
		System.out.println("FULL: remove time: " + (System.currentTimeMillis() - time));
		
		for (int i = 0 ; i < noOfLoops ; i ++) {
			map.put(i, new Accumulator(i));
		}
		
		time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			if (map.containsKey(i))
				map.remove(i);
		}
		System.out.println("FULL: contains and remove time: " + (System.currentTimeMillis() - time));
		
		for (int i = 0 ; i < noOfLoops ; i ++) {
			map.put(i, new Accumulator(i));
		}
		
		time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			map.remove(i);
		}
		System.out.println("FULL: remove time: " + (System.currentTimeMillis() - time));
		
		for (int i = 0 ; i < noOfLoops ; i ++) {
			map.put(i, new Accumulator(i));
		}
		
		time = System.currentTimeMillis();
		for (int i = 0 ; i < noOfLoops ; i++) {
			Accumulator acc = map.get(i);
			if (null != acc) map.remove(i, acc);
		}
		System.out.println("FULL: get and remove time: " + (System.currentTimeMillis() - time));
	}
	
	
	public final File createCoverageSam(final String fileName) throws Exception {
		File file = testFolder.newFile(fileName);
		OutputStream os = new FileOutputStream(file);
		PrintStream ps = new PrintStream(os);
		
		ps.println("@HD	VN:1.0	SO:coordinate");
		ps.println("@RG	ID:20100803052556101	SM:ES	DS:rl=50	");
		ps.println("@RG	ID:20100803052545338	SM:ES	DS:rl=50	");
		ps.println("@PG	ID:SOLID-GffToSam	VN:1.4.3");
		ps.println("@SQ	SN:chr1	LN:255000000");
		ps.println("@SQ	SN:chr2	LN:255000000");
		ps.println("@SQ	SN:chr3	LN:255000000");
		ps.println("@SQ	SN:chr7	LN:255000000");
		ps.println("@SQ	SN:chr17	LN:255000000");
		ps.println("@SQ	SN:chr18	LN:255000000");
		ps.println("@SQ	SN:chrX	LN:255000000");
		ps.println("841_1342_589	65	chr1	10177	18	50M	chr17	71070884	0	ATCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACC	IAIII1;CIIIIIIIIIIIIIIIII7D@AIGDDCI**IDIIII@%%BIG3	ZC:i:11	MD:Z:1C48	RG:Z:20100803052556101	NH:i:2	CM:i:4	NM:i:1	SM:i:16	ZP:Z:Z**	CQ:Z:59)B?+'5/=599>6:>;B>85?A;/)<%=>*;*:8*@+:1><9(%):53	CS:Z:T33223010023010023010023010023010023030023010033010");
		ps.println("710_1533_1074	115	chr1	10201	0	50M	=	11271	1120	CCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCTAACCCTAAA	=IIIIIIIIIIIIIIIIHIIIIICIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:10	MD:Z:50	RG:Z:20100803052545338	NH:i:2	CM:i:0	NM:i:0	SM:i:16	ZP:Z:AAA	CQ:Z:>BAAB@BAAB>BB>B>?@A<4;;<B<22<>A<72@2??7?38B=6995A=	CS:Z:T00032001032001032000103200103200103200103200103200");
		ps.println("1551_1209_1505	161	chr1	10308	3	45M5H	chrX	86086501	0	CCAACCCCAACCCCAACCCTAAACCCTAACCCTAACCCTAACCCT	IIIIIIIIIIIIIIIIIIIHIIIIIII==III@;++I=5--8IA1	ZC:i:11	MD:Z:22C22	RG:Z:20100803052556101	NH:i:2	CM:i:3	NM:i:1	SM:i:20	ZP:Z:Z**	CQ:Z:BBBBBBBBBB9BBBB;@BB:/B:<A5<///@<2/-+B//'''2;'+++@/	CS:Z:G30101000101000101002300100230100230000230100200100");
		ps.println("1989_1770_1230	65	chr1	10308	2	44M6H	=	231394359	231384101	CCCACCCCAACCCCAACCCTAACCCCTAACCCTAACCCTAACCC	IIIIIIIGIIIII?AIIIIIIIIII9133FD5979IE5@:-AI<	ZC:i:11	MD:Z:2A41	RG:Z:20100803052556101	NH:i:2	CM:i:2	NM:i:1	SM:i:18	ZP:Z:Z**	CQ:Z:ABBB:A@9/BB;?5+7:@<B;@;5?1))+)>'/+--=)-4'';6')7'4'	CS:Z:T20011000101000101002301000230100230100230100330102");
		ps.println("841_1342_589	65	chr2	10177	18	50M	chr17	71070884	0	ATCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACC	IAIII1;CIIIIIIIIIIIIIIIII7D@AIGDDCI**IDIIII@%%BIG3	ZC:i:11	MD:Z:1C48	RG:Z:20100803052556101	NH:i:2	CM:i:4	NM:i:1	SM:i:16	ZP:Z:Z**	CQ:Z:59)B?+'5/=599>6:>;B>85?A;/)<%=>*;*:8*@+:1><9(%):53	CS:Z:T33223010023010023010023010023010023030023010033010");
		ps.println("710_1533_1074	115	chr2	10201	0	50M	=	11271	1120	CCCTAACCCTAACCCTAACCCTAACCCTAACCCCTAACCCTAACCCTAAA	=IIIIIIIIIIIIIIIIHIIIIICIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:10	MD:Z:50	RG:Z:20100803052545338	NH:i:2	CM:i:0	NM:i:0	SM:i:16	ZP:Z:AAA	CQ:Z:>BAAB@BAAB>BB>B>?@A<4;;<B<22<>A<72@2??7?38B=6995A=	CS:Z:T00032001032001032000103200103200103200103200103200");
		ps.println("1551_1209_1505	161	chr2	10308	3	45M5H	chrX	86086501	0	CCAACCCCAACCCCAACCCTAAACCCTAACCCTAACCCTAACCCT	IIIIIIIIIIIIIIIIIIIHIIIIIII==III@;++I=5--8IA1	ZC:i:11	MD:Z:22C22	RG:Z:20100803052556101	NH:i:2	CM:i:3	NM:i:1	SM:i:20	ZP:Z:Z**	CQ:Z:BBBBBBBBBB9BBBB;@BB:/B:<A5<///@<2/-+B//'''2;'+++@/	CS:Z:G30101000101000101002300100230100230000230100200100");
		ps.println("1989_1770_1230	65	chr2	10308	2	44M6H	=	231394359	231384101	CCCACCCCAACCCCAACCCTAACCCCTAACCCTAACCCTAACCC	IIIIIIIGIIIII?AIIIIIIIIII9133FD5979IE5@:-AI<	ZC:i:11	MD:Z:2A41	RG:Z:20100803052556101	NH:i:2	CM:i:2	NM:i:1	SM:i:18	ZP:Z:Z**	CQ:Z:ABBB:A@9/BB;?5+7:@<B;@;5?1))+)>'/+--=)-4'';6')7'4'	CS:Z:T20011000101000101002301000230100230100230100330102");
		ps.close();
		os.close();
		return file;
	}
	
	public final File createTumourCoverageSam(final String fileName) throws Exception {
		File file = testFolder.newFile(fileName);
		OutputStream os = new FileOutputStream(file);
		PrintStream ps = new PrintStream(os);
		
		ps.println("@HD	VN:1.0	SO:coordinate");
		ps.println("@RG	ID:2010080607305580	SM:ES	DS:rl=50	");
		ps.println("@RG	ID:20100806073055880	SM:ES	DS:rl=50	");
		ps.println("@RG	ID:20100806061644214	SM:ES	DS:rl=50	");
		ps.println("@PG	ID:SOLID-GffToSam	VN:1.4.3");
		ps.println("@SQ	SN:chr1	LN:255000000");
		ps.println("@SQ	SN:chr2	LN:255000000");
		ps.println("@SQ	SN:chr3	LN:255000000");
		ps.println("@SQ	SN:chr7	LN:255000000");
		ps.println("@SQ	SN:chr17	LN:255000000");
		ps.println("@SQ	SN:chr18	LN:255000000");
		ps.println("@SQ	SN:chrX	LN:255000000");
		ps.println("1652_611_1413	65	chr1	10170	12	48M2H	chr18	98761	0	AACCCTAACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	IIIIIIIIIIIII''H<DB<CIEA;?IB;:7;;;5BD51//9''77A9	ZC:i:7	MD:Z:48	RG:Z:2010080607305580	NH:i:2	CM:i:2	NM:i:0	SM:i:18	ZP:Z:Z**	CQ:Z:@B>9B??59=-B=7'@)412+915-/1;(4'1+1++8-))')1'1'11))	CS:Z:T30100230102301102301002301002301002301002300002310");
		ps.println("2310_699_589	161	chr1	10176	2	50M	chr7	159127746	0	AACCTAACCCTAACCCTAAACCTAACCCTAACCCTAACCCTAACCCTAAC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIH+	ZC:i:7	MD:Z:19C30	RG:Z:20100806073055880	NH:i:3	CM:i:2	NM:i:1	SM:i:18	ZP:Z:Z**	CQ:Z:BBBBBBBBBBBBABBBABB@@<>?B?BB>@A<B<>=?;B=<7@<BB7A>+	CS:Z:G20102301002301002300102301002301002301002301002301");
		ps.println("1844_1231_1722	137	chr1	10304	2	2H48M	*	0	0	AACCCCAACCCCAACCCCAACCCTAACCCCTAACCCTAACCCTAACCC	IIIIICD++IIIEIIII))IIIIIIIIIII@I%%IIIG''?9==5?I<	ZC:i:8	MD:Z:48	RG:Z:20100806061644214	NH:i:0	CM:i:4	NM:i:0	SM:i:16	ZP:Z:Z**	CQ:Z::B+A2BB5/6+A8?9-?:</)2;7<9796=8=-4;%?<<17'5+///'9<	CS:Z:G30201000100000101000301002301000230000230000230100");
		ps.println("1679_1928_748	145	chr1	10304	2	3H47M	chrX	155260125	0	AACCCCAACCCCAACCCCAAACCTAACCCCTAACCCTAACCCTAACC	04IIIIB;AGA:;54>%%E<-9=2AIIII%%IHGF:=ADIIBGIIDG	ZC:i:7	MD:Z:20C26	RG:Z:20100806073055880	NH:i:4	CM:i:4	NM:i:1	SM:i:25	ZP:Z:Z**	CQ:Z:441;621<502,/8097%56;8:(+3''60%/0%1+026,03;8?0%,;5	CS:Z:G00103200103200103000010320100110010100010100010000");
		ps.println("1760_1943_1783	115	chr1	11770557	69	8H42M	=	11771284	782	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAA	IIIIIIIIIIIIIIEHIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:7	MD:Z:42	RG:Z:20100806073055880	NH:i:1	CM:i:0	NM:i:0	SM:i:90	ZP:Z:AAA	CQ:Z:AA@9AAB@=BBB4;B@>=9BBBAB@B?*<BB>9B?B;AAA??@B>6)4)-	CS:Z:T00130111300130332031200222220111300000000000002222");
		ps.println("2111_1305_1488	115	chr1	11770557	83	3H47M	=	11771427	923	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACA	BEIIIIIIIIIIIIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:47	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:@@;:?A>:;@<?<8@@?6>A<@/:??A8?@@A64AB?5@@=@=>AA%>@@	CS:Z:T01100001301113001303320312002222201113000000000000");
		ps.println("293_1672_1413	115	chr1	11770557	77	5H45M	=	11771674	1172	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:45	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:BB?AA?=;;=:?:?>7?@><<@@@A>B@=B:>::?5A@?B?BA;/A@=,8	CS:Z:T00000130111300130332031200222220111300000000000000");
		ps.println("2225_562_1361	67	chr1	11770558	88	50M	=	11768815	-1793	AAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACACTAC	IIIII@EIIIFHIII%%III?EIIICDIII2BIII>8III7CIIIH5II:	ZC:i:8	MD:Z:50	RG:Z:20100806061644214	NH:i:1	CM:i:1	NM:i:0	SM:i:86	ZP:Z:AAA	CQ:Z:84=<4:'?<==*?7=<%?>=:&@?;<(=>@,'<>@:%4=?-+9:>=,*@:	CS:Z:T30000000031110220220021302330310031110310000111231");
		ps.println("1652_611_1413	65	chr2	10170	12	48M2H	chr18	98761	0	AACCCTAACCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTA	IIIIIIIIIIIII''H<DB<CIEA;?IB;:7;;;5BD51//9''77A9	ZC:i:7	MD:Z:48	RG:Z:2010080607305580	NH:i:2	CM:i:2	NM:i:0	SM:i:18	ZP:Z:Z**	CQ:Z:@B>9B??59=-B=7'@)412+915-/1;(4'1+1++8-))')1'1'11))	CS:Z:T30100230102301102301002301002301002301002300002310");
		ps.println("2310_699_589	161	chr2	10176	2	50M	chr7	159127746	0	AACCTAACCCTAACCCTAAACCTAACCCTAACCCTAACCCTAACCCTAAC	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIH+	ZC:i:7	MD:Z:19C30	RG:Z:20100806073055880	NH:i:3	CM:i:2	NM:i:1	SM:i:18	ZP:Z:Z**	CQ:Z:BBBBBBBBBBBBABBBABB@@<>?B?BB>@A<B<>=?;B=<7@<BB7A>+	CS:Z:G20102301002301002300102301002301002301002301002301");
		ps.println("1844_1231_1722	137	chr2	10304	2	2H48M	*	0	0	AACCCCAACCCCAACCCCAACCCTAACCCCTAACCCTAACCCTAACCC	IIIIICD++IIIEIIII))IIIIIIIIIII@I%%IIIG''?9==5?I<	ZC:i:8	MD:Z:48	RG:Z:20100806061644214	NH:i:0	CM:i:4	NM:i:0	SM:i:16	ZP:Z:Z**	CQ:Z::B+A2BB5/6+A8?9-?:</)2;7<9796=8=-4;%?<<17'5+///'9<	CS:Z:G30201000100000101000301002301000230000230000230100");
		ps.println("1679_1928_748	145	chr2	10304	2	3H47M	chrX	155260125	0	AACCCCAACCCCAACCCCAAACCTAACCCCTAACCCTAACCCTAACC	04IIIIB;AGA:;54>%%E<-9=2AIIII%%IHGF:=ADIIBGIIDG	ZC:i:7	MD:Z:20C26	RG:Z:20100806073055880	NH:i:4	CM:i:4	NM:i:1	SM:i:25	ZP:Z:Z**	CQ:Z:441;621<502,/8097%56;8:(+3''60%/0%1+026,03;8?0%,;5	CS:Z:G00103200103200103000010320100110010100010100010000");
		ps.println("1760_1943_1783	115	chr2	11770557	69	8H42M	=	11771284	782	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAA	IIIIIIIIIIIIIIEHIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:7	MD:Z:42	RG:Z:20100806073055880	NH:i:1	CM:i:0	NM:i:0	SM:i:90	ZP:Z:AAA	CQ:Z:AA@9AAB@=BBB4;B@>=9BBBAB@B?*<BB>9B?B;AAA??@B>6)4)-	CS:Z:T00130111300130332031200222220111300000000000002222");
		ps.println("2111_1305_1488	115	chr2	11770557	83	3H47M	=	11771427	923	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACA	BEIIIIIIIIIIIIIIIIIIIIIIHIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:47	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:@@;:?A>:;@<?<8@@?6>A<@/:??A8?@@A64AB?5@@=@=>AA%>@@	CS:Z:T01100001301113001303320312002222201113000000000000");
		ps.println("293_1672_1413	115	chr2	11770557	77	5H45M	=	11771674	1172	AAAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAA	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	ZC:i:8	MD:Z:45	RG:Z:20100806061644214	NH:i:1	CM:i:0	NM:i:0	SM:i:100	ZP:Z:AAA	CQ:Z:BB?AA?=;;=:?:?>7?@><<@@@A>B@=B:>::?5A@?B?BA;/A@=,8	CS:Z:T00000130111300130332031200222220111300000000000000");
		ps.println("2225_562_1361	67	chr2	11770558	88	50M	=	11768815	-1793	AAAAAAAAATGTGGAGAGAAAGTAAGCGGCAAATGTGGCAAAAACACTAC	IIIII@EIIIFHIII%%III?EIIICDIII2BIII>8III7CIIIH5II:	ZC:i:8	MD:Z:50	RG:Z:20100806061644214	NH:i:1	CM:i:1	NM:i:0	SM:i:86	ZP:Z:AAA	CQ:Z:84=<4:'?<==*?7=<%?>=:&@?;<(=>@,'<>@:%4=?-+9:>=,*@:	CS:Z:T30000000031110220220021302330310031110310000111231");
		ps.close();
		os.close();
		return file;
	}
	
	public final File createRefFile(final String filename) throws IOException {
		File file = testFolder.newFile(filename);
		OutputStream os = new FileOutputStream(file);
		PrintStream ps = new PrintStream(os);
		ps.println(">chr1");
		char[] chr1 = new char[12000000];
		Arrays.fill(chr1, 'A');
		ps.println(chr1);
		ps.close();
		os.close();
		return file;
	}

}
