package org.qcmg.qprofiler2.summarise;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

public class KmersSummaryTest {
	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();
	private static File input ;
	
	@BeforeClass
	public static void setUp() throws Exception {
		input = createTestSamFile();
	}

	@Test
	public void incrementInt() {
		assertEquals(1, KmersSummary.incrementInt(0, (byte) 'A', 3));
		assertEquals(9, KmersSummary.incrementInt(1, (byte) 'A', 3));

		int kmerId = 0;
		for (byte b : new byte[]{'A','A','A','A','A','A'}) {
			kmerId = KmersSummary.incrementInt(kmerId, b, 3);
		}
		assertEquals(Integer.parseUnsignedInt("001001001001001001", 2), kmerId);

		kmerId = 0;
		for (byte b : new byte[]{'A','A','T','G','C','A'}) {
			kmerId = KmersSummary.incrementInt(kmerId, b, 3);
		}
		assertEquals(Integer.parseUnsignedInt("001001100011010001", 2), kmerId);
	}

	@Test
	public void getBitMaskValue() {
		assertEquals(262143, KmersSummary.BIT_MASK_VALUE);
	}

	@Test
	public void getEntry() {
		assertEquals(Integer.parseUnsignedInt("001001001001001001", 2), KmersSummary.getEntry(new byte[] {'A','A','A','A','A','A'}));
//		assertEquals(Integer.parseUnsignedInt("001010011100001001", 2), KmersSummary.getEntry(new byte[] {'A','A','T','G','C','A'}));
		assertEquals(Integer.parseUnsignedInt("001001100011010001", 2), KmersSummary.getEntry(new byte[] {'A','A','T','G','C','A'}));
	}
		 
	@Test
	public void producerTest() {
		assertEquals("A,T,G,C,N", KmersSummary.producer(1,"",true));
		assertEquals("A,T,G,C", KmersSummary.producer(1,"",false));
		assertEquals("AA,AT,AG,AC,TA,TT,TG,TC,GA,GT,GG,GC,CA,CT,CG,CC", KmersSummary.producer(2,"",false));
	}

	@Test
	// speed test should be ignore due to time consuming
	public void getPossibleKmerStringTest()  {
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		String [] kmers = summary.getPossibleKmerString(6, true);
		assertEquals((int)Math.pow(5,6), kmers.length);
		kmers = summary.getPossibleKmerString(6, false);
		assertEquals((int)Math.pow(4,6), kmers.length);

		// test speed between split and subString
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		System.out.println(dtf.format(now));

		// calling producer with split 101 times
		String[] mers1 = summary.getPossibleKmerString(6, false);
		for (int  i = 0; i < 100; i ++) {
			mers1 = summary.getPossibleKmerString(6, false);
		}
		assertEquals(4096, mers1.length);
		System.out.println("the finished producer with split 101 times ");

		now = LocalDateTime.now();
		System.out.println(dtf.format(now));
		System.out.println("calling producer with subString 101 times ");

		List<String> mers2 = new ArrayList<>();
		String str = KmersSummary.producer(6, "", false);
		while(str.contains(Constants.COMMA_STRING)) {
			int pos =  str.indexOf(Constants.COMMA_STRING);
			mers2.add(str.substring(0, pos));
			str = str.substring(pos+1);
		}
		mers2.add(str); // add last mer
		for (int  i = 0; i < 100; i ++) {
			str = KmersSummary.producer(6, "", false);
			mers2 = new ArrayList<>();
			while(str.contains(Constants.COMMA_STRING)) {
				int pos =  str.indexOf(Constants.COMMA_STRING);
				mers2.add(str.substring(0, pos));
				str = str.substring(pos+1);
			}
			mers2.add(str); // add last mer
		}
		assertEquals(4096, mers2.size());
		now = LocalDateTime.now();
		System.out.println(dtf.format(now));
		System.out.println("the finished producer with subString 100 times ");
	}

	@Test
	public void bothReversedTest() throws IOException {
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(input)) {
			for (SAMRecord record : reader) {
				final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;
				summary.parseKmers(record.getReadBases(), true, order);
//				summary.parseKmers(record.getReadBases(), true, order, false);
			}
		}

		// kmers3
		//  CAGNG TTAGG <= GTCNCAATCC <= CCTAACNCTG		 first  set to reverse
		String[] bases1 = new String[] {"CAG","AGN","GNG","NGT" ,"GTT"};
		//  AGNG TTAGG <= TCNCAATCC  <= CCTAACNCT		 second	 set to reverse
		String[] bases2 = new String[] {"AGN","GNG","NGT" ,"GTT"};
		for (int i = 0; i < bases2.length; i++) {
			assertEquals(1, summary.getCount(i, bases1[i], 1));
			assertEquals(0, summary.getCount(i, bases1[i], 2));
			assertEquals(1, summary.getCount(i, bases2[i], 2));
			assertEquals(0, summary.getCount(i, bases2[i], 1));
		}
		assertEquals(1, summary.getCount(4, bases1[4], 1));
	}

	@Test
	public void toXmlFastqTest() throws  ParserConfigurationException {

		final String base1 = "CAGNGTTAGGTTTTT";
		final String base2 = "CCCCGTTAGGTTTTTT";
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		// prepair data base only no strand and pair info
		summary.parseKmers(base1.getBytes() , false, 0);
		summary.parseKmers(base2.getBytes() , false, 0);

		Element root = XmlElementUtils.createRootElement("root", null);
		summary.toXml(root, 2, true);

		// only one <sequenceMetrics>
		List<Element> eles = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS);
		assertEquals(eles.size(), 1);
		assertEquals(eles.getFirst().getAttribute(XmlUtils.NAME), "2mers");
		assertEquals(1, eles.getFirst().getChildNodes().getLength());

		// check <variableGroup...>
		Element ele = (Element)eles.getFirst().getFirstChild();
		assertEquals(ele.getAttribute(XmlUtils.NAME), "2mers") ;
		// base.length -3
		// cycle number = base.length - KmersSummary.maxKmers = 16-6 that is [1,11]
		assertEquals(11, ele.getChildNodes().getLength());
		for (int i = 0; i < ele.getChildNodes().getLength(); i ++) {
			Element baseE = XmlElementUtils.getChildElement(ele, XmlUtils.BASE_CYCLE, i);
			assert baseE != null;
			assertEquals(baseE.getAttribute(XmlUtils.CYCLE), String.valueOf(i + 1));
		}

		// test unpaired bam reads
		root = XmlElementUtils.createRootElement("root", null);
		summary.toXml(root, 2, false);
		eles = XmlElementUtils.getChildElementByTagName(root, XmlUtils.SEQUENCE_METRICS);
		// check <variableGroup...>
		ele = (Element)eles.getFirst().getFirstChild();
		assertEquals(ele.getAttribute(XmlUtils.NAME), "unPaired") ;

	}

	@Test
	public void toXmlTest() throws ParserConfigurationException, IOException {
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(input)) {
			for (SAMRecord record : reader) {
				final int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;
				summary.parseKmers(record.getReadBases(), record.getReadNegativeStrandFlag(), order);
			}
		}

		Element root = XmlElementUtils.createRootElement("root", null);
		summary.toXml(root, 3, false);

		// the popular kmers are based on counts on middle, middle of first half, middle of second half
		// in this testing case it look at firt cyle, middle cycle and last cycle
		assertEquals("GTT,CAG", StringUtils.join(summary.getPopularKmerString(3, 1), ","));
		assertEquals("TAA,CCT", StringUtils.join(summary.getPopularKmerString(3, 2), ","));

		List<Element> tallysE = XmlElementUtils.getOffspringElementByTagName(root, XmlUtils.TALLY);
		assertEquals(4, tallysE.size());

		for (Element tE : tallysE) {
			assertEquals("1", tE.getAttribute(XmlUtils.COUNT));
			Element baseCycleEle = (Element) tE.getParentNode();
			Element groupEle =  (Element) baseCycleEle.getParentNode();
			Element metricEle = (Element) groupEle.getParentNode();
            switch (tE.getAttribute(XmlUtils.VALUE)) {
                case "GTT" -> {
                    assertEquals("5", baseCycleEle.getAttribute(XmlUtils.CYCLE));
                    assertEquals("3mers", metricEle.getAttribute(XmlUtils.NAME));
                    assertEquals("firstReadInPair", groupEle.getAttribute(XmlUtils.NAME));
                }
                case "TAA" -> {
                    assertEquals("3", baseCycleEle.getAttribute(XmlUtils.CYCLE));
                    assertEquals("3mers", metricEle.getAttribute(XmlUtils.NAME));
                    assertEquals("secondReadInPair", groupEle.getAttribute(XmlUtils.NAME));
                }
                case "CCT" -> assertEquals("1", baseCycleEle.getAttribute(XmlUtils.CYCLE));
                default -> assertEquals("CAG", tE.getAttribute(XmlUtils.VALUE));
            }
		}

		//  kmers3
		//  CAGNG TTAGG <= GTCNCAATCC <= CCTAACNCTG		 first reversed
		String[] bases1 = new String[] {"CAG","AGN","GNG","NGT" ,"GTT"};
		//   CCTAACNCT		 second	forwarded
		String[] bases2 = new String[] {"CCT","CTA","TAA" ,"AAC"};  // ,"ACN", "CNC", "NCT"};

		for (int i = 0; i < bases2.length; i++) {
			assertEquals(1, summary.getCount(i, bases1[i], 1));
			assertEquals(0, summary.getCount(i, bases1[i], 2));
			assertEquals(1, summary.getCount(i, bases2[i], 2));
			assertEquals(0, summary.getCount(i, bases2[i], 1));
		}
		assertEquals(1, summary.getCount(4, bases1[4], 1));
	}


	@Test
	public void bothForwardTest() throws  IOException {

		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(input)) {
			for (SAMRecord record : reader) {
				final int order = (!record.getReadPairedFlag()) ? 0: (record.getFirstOfPairFlag())? 1 : 2;
				summary.parseKmers(record.getReadBases(), false, order);
			}
		}
		// kmers1
		//CCTA A CNCTG first
		//CCTA A CNCT  second
		String[] bases = summary.getPossibleKmerString(1, true);
		for (int cycle = 0; cycle < 10; cycle ++) {
			for (String base : bases) {
				// second read
				if ((cycle == 0 || cycle == 1) && base.equals("C")) {
					assertEquals(1, summary.getCount(cycle, base, 2));
				} else if (cycle == 2 && base.equals("T")) {
					assertEquals(1, summary.getCount(cycle, base, 2));
				} else if (cycle == 3 && base.equals("A")) {
					assertEquals(1, summary.getCount(cycle, base, 2));
				} else {
					// short mers from second reads are discarded
					assertEquals(0, summary.getCount(cycle, base, 2));
				}
			}
		}
		// kmers2
		bases = summary.getPossibleKmerString(2, true);
		for (int cycle = 0; cycle < 10; cycle ++) {
			for (String base : bases) {
				if (cycle == 0 && base.equals("CC")) {
					assertEquals(1, summary.getCount(cycle, base, 1));
				} else if (cycle == 1 && base.equals("CT")) {
					assertEquals(1, summary.getCount(cycle, base, 1));
				} else if (cycle == 2 && base.equals("TA")) {
					assertEquals(1, summary.getCount(cycle, base, 1));
				} else if (cycle == 3 && base.equals("AA")) {
					assertEquals(1, summary.getCount(cycle, base, 1));
				} else if (cycle == 4 && base.equals("AC")) {
					assertEquals(1, summary.getCount(cycle, base, 1));
				} else {
					assertEquals(0, summary.getCount(cycle, base, 1));
				}
			}
		}
	}

	@Test
	/**
	 * the accuracy will drop down if the read base is short. it trim the last six base which the value of KmersSummary.maxKmers
	 * here we set the base to more the 15 base
	 * @throws ParserConfigurationException
	 */
	public void bothFirstTest() {

		final String base1 = "CAGNGTTAGGTTTTT";
		final String base2 = "CCCCGTTAGGTTTTTT";

		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		summary.parseKmers(base1.getBytes() , false, 1);
		summary.parseKmers(base2.getBytes() , false, 1);

		/**
		 * int midCycle = cycleNo / 2;
		 * int bfMidCycle = (midCycle > 20)? midCycle - 10 : (midCycle < kLength)? 0 : midCycle - kLength;
		 * int afMidCycle = (midCycle > 20)? midCycle + 10 : (midCycle < kLength)? cycleNo-1 : midCycle + kLength;
		 * according above code:
		 * popular kmers string is based on cycle:
		 * midCycle 5 = ("base2".length() - 6 + 1)/2
		 * 2 = 5 - mersNo  since 5>mersNo
		 * 8 = 5+ mersNo
		 */

		// CAGNG TTAGG TTTTT
		// CCCCG TTAGG TTTTT T
		// the mer with N won't belong to possible mer string
		assertEquals(1, summary.getCount(2, "GNG", 1));
		assertEquals(1, summary.getCount(2, "CCG", 1));
		// two same bases
		assertEquals(2, summary.getCount(5, "TTA", 1));
		assertEquals(2, summary.getCount(8, "GGT", 1));

		// mers are not counted that is zero unless "TTA,GGT,CCG"
		assertEquals("TTA,GGT,CCG", StringUtils.join(summary.getPopularKmerString(3, 1), ","));
		// nothing on second pair
		assertEquals("", StringUtils.join(summary.getPopularKmerString(3, 2), ","));

	}

	/*
	 * here this method only called once, otherwise exception throw due to file "input.sam" exists
	 */
	private static File createTestSamFile() throws IOException {
		List<String> data = new ArrayList<>();
		data.add("@HD	VN:1.0	SO:coordinate");
		data.add("@RG	ID:1959T	SM:eBeads_20091110_CD	DS:rl=50");
		data.add("@SQ	SN:chr1	LN:249250621");
		data.add("@CO	Test SAM file for use by KmersSummaryTest.java");

		// reverse first in pair
		data.add("642_1887_1862	83	chr1	10167	1	5H10M	=	10176	59	CCTAACNCTG	.(01(\"!\"	RG:Z:1959T");
		// forward second in pair
		data.add("970_1290_1068	163	chr1	10176	3	9M6H	=	10167	-59	CCTAACNCT	I&&HII%%I	RG:Z:1959T");

		File input = testFolder.newFile("input.sam");
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(input)))) {
			for (String line : data)   out.println(line);
		} catch (IOException e) {
			Logger.getLogger("KmersSummaryTest").log(
					Level.WARNING, "IOException caught whilst attempting to write to SAM test file: " + input.getAbsolutePath(), e);
		}

		return input;
	}

	@Test
	public void timingTest() {
		final String bases = "AAACCAGGAGGCTAAGTGGGGTGGAAGGGAGTGAGCTCTCGGACTCCCAGGAGTAAAAGCTTCCAAGTTGGGCTCTCACTTCAGCCCCTCCCACACAGGGAAGCCAGATGGGTTCCCCAGGACCGGGATTCCCCAAGGGGGCTGCTCCCA";
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		for (int i = 0 ; i < 100 ; i++) {
			summary.parseKmers(bases.getBytes(StandardCharsets.UTF_8), true, 1);
		}
	}
	@Test
	public void twoMerTest() {
		final String bases = "AAACCAGGAGGCTAAGTGGGGTGGAAGGGAGTGAGCTCTCGGACTCCCAGGAGTAAAAGCTTCCAAGTTGGGCTCTCACTTCAGCCCCTCCCACACAGGGAAGCCAGATGGGTTCCCCAGGACCGGGATTCCCCAAGGGGGCTGCTCCCA";
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);
		summary.parseKmers(bases.getBytes(StandardCharsets.UTF_8), false, 1);

		assertEquals(1, summary.getCount(0, "AA", 1));
		assertEquals(1, summary.getCount(0, "AAA", 1));
		assertEquals(1, summary.getCount(0, "AAACCA", 1));
		assertEquals(1, summary.getCount(1, "AA", 1));
		assertEquals(1, summary.getCount(1, "AAC", 1));
		assertEquals(1, summary.getCount(1, "AACCAG", 1));
		assertEquals(1, summary.getCount(144, "CTCCCA", 1));
		assertEquals(1, summary.getCount(144, "CT", 1));
		assertEquals(1, summary.getCount(144, "CTC", 1));

		/*
		and now for a reverse strand read
		 */
		summary = new KmersSummary(6);
		summary.parseKmers(bases.getBytes(StandardCharsets.UTF_8), true, 1);

		assertEquals(1, summary.getCount(0, "TGGGAG", 1));
		assertEquals(1, summary.getCount(0, "TGG", 1));
		assertEquals(1, summary.getCount(0, "TG", 1));
	}

	@Test
	public void maxLengthTest() {
		final String bases = "ATGC";
		KmersSummary summary = new KmersSummary(KmersSummary.maxKmers);

		// due to summary.toXml(). getPopularKmerString(...).getPossibleKmerString(kLength, false)
		// any seq include 'N' will not be reported, it is better to skip 'N' for testing
		StringBuilder sb=new StringBuilder();
        sb.append(bases.repeat(200));
		for (int i = 0; i < 100; i ++) {
			summary.parseKmers(sb.toString().getBytes(StandardCharsets.UTF_8), false, 1);
		}

		// reach maximum 1000 base
        sb.append(bases.repeat((KmersSummary.maxCycleNo / bases.length() - 200)));
		assertEquals(KmersSummary.maxCycleNo, sb.length());
		summary.parseKmers(sb.toString().getBytes(StandardCharsets.UTF_8), false, 1);

		assertEquals(101, summary.getCount(793, "TGCATG", 1));  // cycle + 1 = 794 to xml
		assertEquals(1, summary.getCount(797, "TGCATG", 1));  // cycle + 1 = 794 to xml

		// test oversize sequence
		try {
			sb.append("A");
			assertTrue(sb.length() > KmersSummary.maxCycleNo);
			summary.parseKmers(sb.toString().getBytes(StandardCharsets.UTF_8), false, 1);
			// must fail if no exception happen
			fail();
		}catch(IllegalArgumentException e) {
			// expected exception due to large seq
			assertTrue(true);
		}
	}
}

