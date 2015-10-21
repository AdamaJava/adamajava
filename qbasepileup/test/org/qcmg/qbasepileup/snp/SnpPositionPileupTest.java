package org.qcmg.qbasepileup.snp;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupException;

public class SnpPositionPileupTest {
	static final String SAM = "testbam.sam";
	String samFile;
	String bamFile;
	IndexedFastaSequenceFile indexedFasta;
	private Options options;
	SnpPosition p;
	InputBAM input;
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	
	@Before
	public void setUp() {
		samFile = testFolder.getRoot().toString() + "/" + SAM;
		bamFile = samFile.replace(".sam", ".bam");
		options = createMockOptions(false, false);
		indexedFasta = createMockIndexedFasta();
		input = new InputBAM(1, "test", new File(bamFile), Options.INPUT_BAM);
	}
	
	@After
	public void tearDown() {
		samFile = null;
		bamFile = null;
		options = null;
		indexedFasta = null;
		input = null;
		p = null;
	}
	
	@Test 
	public void testPileup() throws Exception {		
		createBamFile();
		p = new SnpPosition("test","chr7", 140188962, 140188962);

		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
				
		pp.pileup(input.getSAMFileReader());
		String expected = "\t\t"+bamFile+"\ttest\tchr7\t140188962\t140188962\tC\t0\t2\t0\t0\t0\t2\t0\t2";
		assertEquals(expected, pp.toString());
	}
	
	@Test
	public void testCoverageMaps() throws QBasePileupException {				
		p = new SnpPosition("test","chr7", 140188962, 140188962);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertEquals(5, pp.getCoverageMap().size());
		assertTrue(null == pp.getForCoverageMap());
		assertTrue(null == pp.getForNovelStartMap());
		
		//strand is true
		options = createMockOptions(false, true);
		pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertEquals(5, pp.getForCoverageMap().size());
		assertEquals(5, pp.getRevCoverageMap().size());
		assertEquals(5, pp.getCoverageMap().size());
		assertTrue(null == pp.getForNovelStartMap());
	}
	
	@Test
	public void testIsSingleBasePosition() throws QBasePileupException {
		p = new SnpPosition("test","chr7", 140188962, 140188962);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertTrue(pp.isSingleBasePosition());
		 options = createMockOptions(false, false);
		p = new SnpPosition("test","chr7", 140188962, 140188963);
		pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertFalse(pp.isSingleBasePosition());
	}	
	
	@Test
	public void testNovelStartMaps() throws Exception {
		Options options = createMockOptions(true, false);		
		p = new SnpPosition("test","chr7", 140188962, 140188962);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
//		pp.pileup();
		assertEquals(5, pp.getForNovelStartMap().size());
		assertEquals(5, pp.getRevNovelStartMap().size());
		assertTrue(null == pp.getCoverageMap());
		
		String s = pp.getNovelStartMapString(pp.getForNovelStartMap());
	}
	
	@Test
	public void testIncrementCoverage() throws QBasePileupException {
		p = new SnpPosition("test","chr7", 140188962, 140188962);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertEquals(0, pp.getCoverageMap().get("C").intValue());
		
		pp.incrementCoverage("C", false);
		assertEquals(1, pp.getCoverageMap().get("C").intValue());
	
		options = createMockOptions(false, true);		
		pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertEquals(0, pp.getRevCoverageMap().get("C").intValue());
		pp.incrementCoverage("C", true);
		assertEquals(1, pp.getRevCoverageMap().get("C").intValue());
	}
	
	@Test
	public void testParseRecordIsUsed() throws QBasePileupException {
		SAMRecord r = createSAMRecord();	
		p = new SnpPosition("test","chr7", 89700206, 89700206);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);	
		assertParseRecord(1, 1, pp, r);		
	}
	
	@Test
	public void testParseRecordIsReadNotMapped() throws QBasePileupException {
		SAMRecord r = createSAMRecord();	
		p = new SnpPosition("test","chr7", 89700205, 89700205);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertParseRecord(1, 0, pp, r);	
	}
	
	@Test
	public void testParseRecordDontIncludeIndel() throws QBasePileupException {
		Options options = createMock(Options.class);
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(10));
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(10));
		expect(options.getMappingQuality()).andReturn(Integer.valueOf(0));
		expect(options.isNovelstarts()).andReturn(false);
		expect(options.isStrandSpecific()).andReturn(false);
		expect(options.includeIndel()).andReturn(false);
		replay(options);
		
		SAMRecord r = createSAMRecord();	
		r.setCigarString("93M1D7S");		
		p = new SnpPosition("test","chr7", 89700206, 89700206);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertParseRecord(1, 0, pp, r);	
	}
	
	@Test
	public void testParseRecordDontIncludeIntron() throws QBasePileupException {
		Options options = createMock(Options.class);
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(10));
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(10));
		expect(options.getMappingQuality()).andReturn(Integer.valueOf(0));
		expect(options.isNovelstarts()).andReturn(false);
		expect(options.isStrandSpecific()).andReturn(false);
		expect(options.includeIndel()).andReturn(false);
		expect(options.includeIntron()).andReturn(false);
		replay(options);
		
		SAMRecord r = createSAMRecord();	
		r.setCigarString("101N");		
		p = new SnpPosition("test","chr7", 89700206, 89700206);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertParseRecord(1, 0, pp, r);	
	}
	
	@Test
	public void testParseRecordFilterByMappingQuality() throws QBasePileupException {
		Options options = createMock(Options.class);
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(10));
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(10));
		expect(options.getMappingQuality()).andReturn(Integer.valueOf(30));
		expect(options.isNovelstarts()).andReturn(false);
		expect(options.isStrandSpecific()).andReturn(false);
		expect(options.getMode()).andReturn("snp");
		expect(options.includeIndel()).andReturn(false);
		expect(options.includeIntron()).andReturn(false);
		replay(options);
		
		SAMRecord r = createSAMRecord();	
		r.setCigarString("94M7S");		
		p = new SnpPosition("test","chr7", 89700206, 89700206);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertParseRecord(1, 0, pp, r);	
	}
	
	@Test
	public void testParseRecordFilterByBaseQuality() throws QBasePileupException {
		Options options = createMock(Options.class);
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(70));
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(70));
		expect(options.getMappingQuality()).andReturn(Integer.valueOf(0));
		expect(options.includeIntron()).andReturn(false);
		expect(options.isNovelstarts()).andReturn(false);
		expect(options.isStrandSpecific()).andReturn(false);
		expect(options.getMode()).andReturn("snp");
		expect(options.includeIndel()).andReturn(false);
		expect(options.includeIntron()).andReturn(false);
		replay(options);
		
		SAMRecord r = createSAMRecord();	
		r.setCigarString("94M7S");		
		p = new SnpPosition("test","chr7", 89700206, 89700206);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		assertParseRecord(1, 0, pp, r);	
	}

	@Test
	public void testBasesAreMapped() throws QBasePileupException {
		p = new SnpPosition("test","chr7", 140188962, 140188962);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);		
		char[] readBases = new char[1];
		pp.setReadBases(readBases);
		assertFalse(pp.basesAreMapped());
		readBases[0] = 'C';
		pp.setReadBases(readBases);
		assertTrue(pp.basesAreMapped());
	}
	
	@Test
	public void testDecovoluteReadSequence() throws QBasePileupException {
		SAMRecord r = createSAMRecord();		
		p = new SnpPosition("test","chr7", 89700206, 89700206);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);
		pp.deconvoluteReadSequence(r);
		assertEquals('A', pp.getReadBases()[0]);
		
	}
	
	@Test
	public void testDecovoluteReadSequenceIsNull() throws QBasePileupException {
		SAMRecord r = createSAMRecord();		
		p = new SnpPosition("test","chr7", 89700205, 89700205);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);
		pp.deconvoluteReadSequence(r);
		assertEquals('\u0000', pp.getReadBases()[0]);		
	}
	
	@Test
	public void testAddToNovelStartsMap() throws QBasePileupException {
		p = new SnpPosition("test","chr7", 89700205, 89700205);
		SnpPositionPileup pp = new SnpPositionPileup(input, p, options,indexedFasta);
		Map<String, Set<Integer>> novelStartMap = new HashMap<String, Set<Integer>>();
		assertFalse(novelStartMap.containsKey("A"));		
		pp.addToNovelStartMap("A", novelStartMap, 89700205);
		assertTrue(novelStartMap.containsKey("A"));
		assertEquals(1, novelStartMap.get("A").size());
		//only increment with a novel start
		pp.addToNovelStartMap("A", novelStartMap, 89700205);
		assertEquals(1, novelStartMap.get("A").size());
	}
	
	
	private void assertParseRecord(int afterTotal, int afterPassFilter, SnpPositionPileup pp, SAMRecord r) throws QBasePileupException {
		assertEquals(0, pp.getTotalExamined());
		assertEquals(0, pp.getPassFiltersCount());
		pp.parseRecord(r);
		assertEquals(afterTotal, pp.getTotalExamined());
		assertEquals(afterPassFilter, pp.getPassFiltersCount());		
		assertEquals(afterPassFilter, pp.getCountNegativeStrand());
	}
	
	
	

	private SAMRecord createSAMRecord() {
		String samString = "HWI-ST1240:47:D12NAACXX:2:2315:11796:5777	83	chr10	89700206	29	94M7S	=	89699999	-301	ACTTCAGATAGGCAACAGATCGAGACCTTGTTTCACAAAACGAACAGATCTGCAAAGATCAACCTGTCCTAAGTCATATAATCTCTTTGTGTAAGAGATTA	DCC=EDDDDDEDEEEEEFFFFFHHHFHJJJJIHIHJIIJJJJIIJJJIGIJIJJJIJJIJJJJJJJJJJIIJJJJJJJJJIIJJJJJJHHHHHFFFFFCCC	ZC:i:10	MD:Z:94	RG:Z:2012060803293054	XG:i:0	NH:i:1	AM:i:29	NM:i:0	SM:i:29	XM:i:0	XO:i:0	ZP:Z:AAA	XT:A:M";
		String[] values = samString.split("\t");
		SAMRecord r = new SAMRecord(null);
		r.setReadName(values[0]);
		r.setCigarString(values[5]);
		r.setReadString(values[9]);
		r.setFlags(83);
		r.setAlignmentStart(Integer.valueOf(values[3]));
		r.setBaseQualities(values[10].getBytes());
		return r;
	}

	private void createBamFile() throws IOException {
		createSAMFile();
		
        SAMFileReader reader = new SAMFileReader(new File(samFile));
        SAMFileHeader header = reader.getFileHeader();
        
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		factory.setCreateIndex(true);
		SAMFileWriter writer = factory.makeBAMWriter(header, false, new File(bamFile));
		
		for (SAMRecord r: reader) {
			writer.addAlignment(r);
		}
		reader.close();
		writer.close(); 

	}

	private void createSAMFile() throws IOException {
		final List<String> data = new ArrayList<String>();
		data.addAll(createSamHeader(SortOrder.coordinate));
		data.addAll(createSamBody());

		BufferedWriter out;
		out = new BufferedWriter(new FileWriter(samFile));
		for (final String line : data) {
			out.write(line + "" + "\n");
		}
		out.close();

	}

	private IndexedFastaSequenceFile createMockIndexedFasta() {
		IndexedFastaSequenceFile indexedFasta = createMock(IndexedFastaSequenceFile.class);
		byte[] bytes = new byte[1];
		bytes[0] = 1;
		expect(indexedFasta.getSubsequenceAt("chr7", 140188962, 140188962)).andReturn(new ReferenceSequence("test", 1234, new String("C").getBytes()));
		replay(indexedFasta);
		return indexedFasta;
	}

	private Options createMockOptions(boolean isNovelStarts, boolean isStrand) {
		Options options = createMock(Options.class);
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(10));
		expect(options.includeIndel()).andReturn(false);
		expect(options.includeIndel()).andReturn(false);
		expect(options.includeIntron()).andReturn(false);
		expect(options.includeIntron()).andReturn(false);
		expect(options.getMode()).andReturn("snp");
		expect(options.getMode()).andReturn("snp");
		expect(options.getBaseQuality()).andReturn(Integer.valueOf(10));
		expect(options.getMappingQuality()).andReturn(Integer.valueOf(0));
		expect(options.isNovelstarts()).andReturn(isNovelStarts);
		expect(options.isStrandSpecific()).andReturn(isStrand);
		replay(options);
		return options;
	}
	
	
	private static Collection<String> createSamHeader(
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
	
	public static List<String> createSamBody() {
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
	

}
