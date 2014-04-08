package org.qcmg.qprofiler.ma;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MaSummarizerTest {
	private static final String MA_INPUT_FILE = "testInputFile.ma";
	private static final String MA_DODGY_INPUT_FILE = "testInputFileDodgy.ma";

	@Before
	public void setup() {
		createTestMaFile(MA_INPUT_FILE, createValidMaData());
	}

	@After
	public void tearDown() {
		File outputFile = new File(MA_INPUT_FILE);
		Assert.assertTrue(outputFile.delete());
	}

	@Test
	public void testSummarize() throws Exception {
		MaSummarizer ms = new MaSummarizer();
		MaSummaryReport sr = (MaSummaryReport) ms.summarize(new File(
				MA_INPUT_FILE));

		Assert.assertNotNull(sr);
		Assert.assertEquals(6, sr.getColorByCycle().count(1, '0').get());
		Assert.assertEquals(2, sr.getColorByCycle().count(2, '2').get());
		
		Assert.assertEquals(4, sr.getBadReadsCount().size());
		
		// lets take a look at the MAMapings
		Assert.assertEquals(24, sr.getChromosomeCount().size());
		int chromoCount = 0;
		for (Entry<String, AtomicLong> mapEntry : sr.getChromosomeCount().entrySet()) {
			chromoCount += mapEntry.getValue().get();
		}
		Assert.assertTrue(chromoCount == 216);
		
		Assert.assertTrue(sr.getLocationCount().isEmpty());
		
//		Assert.assertEquals(3, sr.getMismatchCount().size());
//		int mismatchCount = 0;
//		for (Entry<Integer, Integer> mapEntry : sr.getMismatchCount().entrySet()) {
//			mismatchCount += mapEntry.getValue();
//		}
//		Assert.assertTrue(mismatchCount == 216);
		
		Assert.assertEquals(4, sr.getQualityCount().size());
		int qualityCount = 0;
		for (Entry<String, AtomicLong> mapEntry : sr.getQualityCount().entrySet()) {
			qualityCount += mapEntry.getValue().get();
		}
		Assert.assertTrue(qualityCount == 216);
	}

	@Test
	public void testSummarizeMissingData() throws Exception {
		createDodgyDataFile(createMaDataMissingData());

		MaSummarizer qs = new MaSummarizer();
		MaSummaryReport sr = (MaSummaryReport) qs.summarize(new File(MA_DODGY_INPUT_FILE));
		Assert.assertEquals(0, sr.getRecordsParsed());

		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeEmptyFile() throws Exception {
		createDodgyDataFile(new ArrayList<String>());

		MaSummarizer qs = new MaSummarizer();
		MaSummaryReport sr = (MaSummaryReport) qs.summarize(new File(MA_DODGY_INPUT_FILE));
		Assert.assertEquals(0, sr.getRecordsParsed());

		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeExtraData() throws Exception {
		createDodgyDataFile(createMaDataExtraData());

		MaSummarizer qs = new MaSummarizer();
		MaSummaryReport sr = (MaSummaryReport) qs.summarize(new File(MA_DODGY_INPUT_FILE));
		Assert.assertEquals(0, sr.getRecordsParsed());

		deleteDodgyDataFile();
	}

	@Test
	public void testSummarizeNoHeader() throws Exception {
		createDodgyDataFile(createMaDataBody());

		MaSummarizer qs = new MaSummarizer();
		MaSummaryReport sr = (MaSummaryReport) qs.summarize(new File(
				MA_DODGY_INPUT_FILE));
		Assert.assertEquals(6, sr.getRecordsParsed());

		deleteDodgyDataFile();
	}

	private void deleteDodgyDataFile() {
		File outputFile = new File(MA_DODGY_INPUT_FILE);
		Assert.assertTrue(outputFile.delete());
	}

	private void createDodgyDataFile(List<String> dodgyData) {
		createTestMaFile(MA_DODGY_INPUT_FILE, dodgyData);
	}

	private static List<String> createValidMaData() {
		List<String> data = new ArrayList<String>();
		
		for (String dataEntry : createMaDataHeader()) {
			data.add(dataEntry);
		}
		data.add("# called by createValidMaData()");
		
		for (String dataEntry : createMaDataBody()) {
			data.add(dataEntry);
		}
		
		return data;
	}

	private static List<String> createMaDataHeader() {
		List<String> data = new ArrayList<String>();
		data.add("# test ma file");
		data.add("# auto-generated from MaSummarizerTest.java");
		return data;
	}
	
	private static List<String> createMaDataBody() {
		List<String> data = new ArrayList<String>();
		
		data.add(">2_16_98_F3");
		data.add("T02.1.0223.310.031.0.3203331303.0.3002..303..2....2");
		data.add(">2_16_645_F3");
		data.add("T00.0.000..000.030.0.0000000000.3.0000..000..0....0");
		data.add(">2_20_70_F3,24_19386877.1:(29.1.15):q1,24_3902119.2:(26.2.13):q0,23_111953591.2:(37.5.7):q0,23_77138606.2:(25.2.14):q0,23_46244203.2:(28.2.13):q0,23_26103154.2:(26.2.13):q0,23_8989429.2:(33.3.6):q0,23_8989404.2:(26.2.13):q0,23_8989376.2:(26.2.13):q0,23_8989348.2:(26.2.13):q0,22_30850994.2:(24.2.15):q0,22_25001898.2:(32.4.7):q0,22_20656188.2:(26.2.13):q0,22_18718434.2:(25.2.15):q0,22_18718382.2:(30.3.9):q0,21_25009745.2:(26.2.13):q0,21_15460589.2:(28.2.13):q0,20_52485514.2:(31.2.13):q0,20_16129849.2:(26.2.13):q0,19_53111570.2:(29.2.15):q0,19_29963698.2:(27.2.13):q0,19_29963657.2:(27.2.13):q0,19_14326420.2:(28.2.12):q0,19_108002.2:(25.2.15):q0,19_107957.2:(25.2.15):q0,18_63580787.2:(30.3.9):q0,18_60492662.2:(31.3.13):q0,18_53288908.2:(33.4.6):q0,18_49798776.2:(26.2.13):q0,18_49798764.2:(24.2.15):q0,18_27175974.2:(26.2.13):q0,18_7021802.2:(25.2.14):q0,17_69639595.2:(26.2.13):q0,17_68814616.2:(33.4.7):q0,17_12345785.2:(36.4.8):q0,16_80983867.2:(30.3.9):q0,16_16872799.2:(30.3.13):q0,16_2492602.2:(27.2.13):q0,15_93174531.2:(33.3.6):q0,15_86954095.2:(32.3.7):q0,15_72717964.2:(31.3.8):q0,15_57982078.2:(32.3.7):q0,14_61056832.2:(26.2.13):q0,14_41816560.2:(29.3.15):q0,14_41816536.2:(29.3.15):q0,13_110347036.2:(31.3.13):q0,13_110346980.2:(31.3.13):q0,13_110346946.2:(26.2.13):q0,13_110346911.2:(31.3.13):q0,13_80943146.2:(30.3.9):q0,13_65324990.2:(26.2.13):q0,13_28508802.2:(27.2.12):q0,12_120203179.2:(31.3.8):q0,12_108432281.2:(26.2.15):q0,12_98366451.2:(26.2.13):q0,12_98366008.2:(26.2.13):q0,12_95543056.2:(25.2.14):q0,12_89096173.2:(24.2.15):q0,12_54043944.1:(26.1.13):q0,11_72962510.2:(26.2.13):q0,11_62129948.2:(29.2.15):q0,11_46183568.2:(34.4.7):q0,11_44572370.2:(29.3.15):q0,11_26550953.2:(30.3.9):q0,11_26550835.2:(26.2.13):q0,11_26550779.2:(26.2.13):q0,11_6732697.2:(26.2.13):q0,10_104397940.2:(26.2.13):q0,10_94284114.2:(29.3.15):q0,10_86217065.2:(26.2.13):q0,10_56382370.2:(36.4.8):q0,10_28174560.2:(31.3.8):q0,10_4957194.2:(26.2.13):q0,9_125441421.2:(25.2.14):q0,9_125441376.2:(25.2.14):q0,9_113443354.2:(26.2.13):q0,9_100018387.2:(27.2.12):q0,9_96502934.2:(31.3.13):q0,9_33714109.2:(26.2.13):q0,9_33714068.2:(32.4.7):q0,8_122949077.2:(26.2.13):q0,8_122949065.2:(24.2.15):q0,8_91006568.2:(26.2.13):q0,8_78255716.2:(33.4.7):q0,7_143557068.2:(26.2.13):q0,7_143557054.2:(26.2.13):q0,7_143557012.2:(26.2.13):q0,7_143299099.2:(26.2.13):q0,7_111959710.2:(27.2.13):q0,7_91386274.2:(25.2.15):q0,7_91386238.2:(25.2.15):q0,7_49589459.2:(26.2.13):q0,7_4447499.2:(26.2.13):q0,6_162431811.2:(33.3.6):q0,6_162431756.2:(24.2.15):q0,6_156028721.2:(33.3.6):q0,6_137520430.2:(24.2.15):q0,6_103549549.2:(33.4.6):q0,6_98835782.2:(33.4.6):q0,6_86876919.2:(32.3.9):q0");
		data.add("T000003103333313303303033303333333033333000333.3..0");
		data.add(">2_24_235_F3,4_-24551754.2:(37.5.0):q1,23_150183486.2:(24.2.0):q0,14_105303270.2:(24.2.0):q0,2_-208039139.2:(24.2.0):q0,9_-8349076.2:(27.2.0):q1,16_73135445.2:(27.2.0):q1,5_18953817.2:(27.2.0):q1,4_58398000.2:(24.2.0):q0,1_191324467.2:(24.2.0):q0,11_-110111494.2:(25.2.0):q0,2_-228835655.2:(25.2.0):q0,20_-7602311.2:(27.2.0):q1,23_-54645188.2:(24.2.0):q0,14_-65725821.2:(26.2.0):q0,7_-147038553.2:(27.2.0):q1");
		data.add("T00000310310000030030000000002003030000000000000000");
		data.add(">2_25_1128_F3,10_34842650.2:(24.2.15):q7");
		data.add("T02001333333300330030000130003303010000200003200002");
		data.add(">2_27_285_F3,12_124322288.0:(33.0.15):q3,23_26896083.2:(25.2.15):q0,23_11891820.2:(24.2.15):q0,22_43462686.0:(24.0.15):q0,22_41460705.1:(33.1.15):q0,22_41301071.2:(28.2.14):q0,21_41588194.2:(28.2.15):q0,20_48304412.1:(26.1.14):q0,20_34175881.2:(33.3.15):q0,20_32338434.2:(28.2.15):q0,20_693836.2:(24.2.15):q0,19_44137997.1:(31.1.14):q0,19_18161683.2:(29.2.14):q0,19_13391154.1:(30.1.14):q0,19_12701732.2:(26.2.15):q0,19_11326750.2:(27.2.15):q0,19_6851347.2:(25.2.15):q0,18_45386809.1:(24.1.15):q0,18_20682758.2:(27.2.15):q0,18_227308.2:(24.2.15):q0,17_62485461.2:(30.2.14):q0,17_42523472.1:(25.1.14):q0,17_36637380.2:(24.2.15):q0,17_20949980.2:(31.2.15):q0,16_70619588.0:(28.0.15):q0,16_50383297.2:(25.2.14):q0,16_23628884.1:(29.1.14):q0,16_15861776.1:(29.1.13):q0,16_5449731.0:(26.0.15):q0,16_5346556.1:(25.1.14):q0,15_56702655.2:(24.2.15):q0,14_90443632.2:(25.2.15):q0,14_61182963.2:(24.2.15):q0,13_81406012.2:(33.4.15):q0,13_41302623.2:(24.2.15):q0,12_57904921.2:(25.2.14):q0,12_56682597.1:(25.1.14):q0,12_54855035.2:(29.3.10):q0,12_54041430.2:(29.2.15):q0,11_110113165.2:(30.2.15):q0,10_93833359.2:(24.2.15):q0,10_79774465.2:(25.2.15):q0,10_54404024.2:(24.2.15):q0,10_28726393.2:(24.2.15):q0,10_12363623.1:(25.1.15):q0,10_7246833.1:(25.1.15):q0,9_116028788.2:(25.2.14):q0,9_103201781.2:(26.2.15):q0,9_81380279.2:(24.2.15):q0,9_18368061.2:(26.2.15):q0,9_6991241.2:(25.2.14):q0,8_141961991.1:(27.1.14):q0,8_42718580.2:(24.2.15):q0,8_35601104.2:(26.2.15):q0,8_33507747.2:(27.2.13):q0,8_22341518.2:(24.2.15):q0,8_16073591.2:(32.2.14):q0,8_15130367.1:(28.1.15):q0,8_4842886.2:(24.2.15):q0,7_44944299.0:(24.0.15):q0,7_29671270.2:(28.3.15):q0,6_134265078.1:(24.1.15):q0,6_111973884.1:(25.1.15):q0,6_26194958.2:(25.2.14):q0,6_22016141.1:(27.1.15):q0,5_168188910.2:(31.2.15):q0,5_132441778.0:(33.0.15):q3,5_112431380.1:(26.1.14):q0,5_75184152.1:(26.1.14):q0,5_69392254.1:(31.2.10):q0,4_186133442.0:(33.2.15):q0,4_170834250.2:(24.2.15):q0,4_120999729.2:(24.2.15):q0,4_89328715.2:(24.2.15):q0,4_20392378.1:(24.1.15):q0,3_183499687.0:(25.0.15):q0,3_183163585.2:(24.2.15):q0,3_178821365.2:(34.4.14):q0,3_143430405.2:(33.2.15):q0,3_128307456.2:(24.2.15):q0,3_122328540.2:(29.2.15):q0,3_113400555.2:(28.2.15):q0,3_4542701.2:(24.2.15):q0,2_231816637.2:(24.2.15):q0,2_220573686.2:(24.2.15):q0,2_179083117.2:(29.2.15):q0,2_121962565.2:(26.2.14):q0,2_86386198.1:(29.1.14):q0,2_33801858.1:(32.1.14):q0,1_234480163.1:(28.1.15):q0,1_212515364.2:(24.2.15):q0,1_166964910.2:(25.2.15):q0,1_150904695.1:(24.1.15):q0,1_144454445.2:(27.2.15):q0,1_93946138.2:(27.2.13):q0,1_53294094.2:(24.2.15):q0,1_48954150.2:(27.2.15):q0,1_29256832.1:(27.1.14):q0,1_8830358.2:(28.3.15):q0,24_59348995.2:(28.2.15):q0");
		data.add("T03000323302001202030000000000003000000000000000002");
		
		return data;
	}

	private static List<String> createMaDataMissingData() {
		List<String> data = new ArrayList<String>();
		for (String dataEntry : createMaDataHeader()) {
			data.add(dataEntry);
		}
		data.add("# called by createMaDataMissingData()");
		
		data.add(">id_123");
		data.add(">id_234");
		data.add(">id_345");
		data.add(">id_456");
		data.add(">id_567");
		return data;
	}

	private static List<String> createMaDataExtraData() {
		List<String> data = new ArrayList<String>();
		for (String dataEntry : createMaDataHeader()) {
			data.add(dataEntry);
		}
		data.add("# called by createMaDataExtraData()");
		
		data.add("T332.22112122.032100222.0021.2.020.3000.3203.030...");
		data.add("T322.2.10.202.000020302.0002...002.2002.0022.002...");
		data.add("T21213332221232222222222232232322222233333322333222");
		data.add("T21020020022300231002202003000002020022200202003220");
		
		for (String dataEntry : createMaDataBody()) {
			data.add(dataEntry);
		}
		
		return data;
	}

	private static void createTestMaFile(String name, List<String> data) {
		String fileName = MA_INPUT_FILE;
		if (null != name)
			fileName = name;

		PrintWriter out;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));

			for (String line : data) {
				out.println(line);
			}
			out.close();
		} catch (IOException e) {
			Logger.getLogger("MaSummarizerTest").log(
					Level.WARNING,
					"IOException caught whilst attempting to write to MA test file: "
							+ fileName, e);
		}
	}

}
