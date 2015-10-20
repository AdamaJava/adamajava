package org.qcmg.qbamannotate;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.qcmg.common.commandline.StreamConsumer;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.testing.SamTestData;
import org.qcmg.testing.TestDataSolid0039_20091125_2_TD04_LMP_20100531;

public class AnnotatorTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Before
	public final void before() {
		try {
			TestDataSolid0039_20091125_2_TD04_LMP_20100531
					.createNaturalReadNameSortedMappedSamFile();
			TestDataSolid0039_20091125_2_TD04_LMP_20100531.createF3MaFile();
			TestDataSolid0039_20091125_2_TD04_LMP_20100531.createR3MaFile();
		} catch (Exception e) {
			System.err.println("File creation error in test harness: "
					+ e.getMessage());
		}
	}

	@After
	public final void after() {
		try {
			File inputFile = new File("test.sam");
			inputFile.delete();
			File f3File = new File("f3.ma");
			f3File.delete();
			File r3File = new File("r3.ma");
			r3File.delete();
			File outputFile = new File("output.sam");
			outputFile.delete();
		} catch (Exception e) {
			System.err.println("File deletion error in test harness: "
					+ e.getMessage());
		}
	}

	private StreamConsumer osc;
	private StreamConsumer errsc;

	public int exec(final String command) throws Exception {
		Process process = Runtime.getRuntime().exec(command);

		osc = new StreamConsumer(process.getInputStream());
		errsc = new StreamConsumer(process.getErrorStream());
		osc.run();
		errsc.run();

		int errCode = process.waitFor();

		return errCode;
	}

	@Ignore
	public final void constructWithValidArguments() throws Exception {
		ExpectedException.none();

		final String BASE_DIR = "/path/test_lmp/20100821";
		final String BAM_FILE_NAME = BASE_DIR + "/pairing/F3-R3-Paired.bam";
		String MA_FILE_NAME_F3 = BASE_DIR
				+ "/F3/s_mapping/S0433_20100208_LMP_1_Sample1_F3_million_records.csfasta.ma";
		String MA_FILE_NAME_R3 = BASE_DIR
				+ "/R3/s_mapping/S0433_20100208_LMP_1_Sample1_R3_million_records.csfasta.ma";
		String SORTED_BAM_FILE_PREFIX = "/path/test_lmp/naturally_sorted_qbamannotate";
		String SORTED_BAM_FILE_NAME = SORTED_BAM_FILE_PREFIX + ".bam";
        String OUTPUT_FILE_NAME = "/path/test_lmp/large_output.sam";

		// Perform the actual annotation
		new AdvancedAnnotator(OUTPUT_FILE_NAME, SORTED_BAM_FILE_NAME,
				MA_FILE_NAME_F3, MA_FILE_NAME_R3, new LongMatePair(580, 2400),
				"qbamannotate", "0.0", "not applicable");

	}

	@Test
	public final void constructWithValidArgumentsF3AndR3() throws Exception {
		ExpectedException.none();
		new AdvancedAnnotator("output.sam", "test.sam", "f3.ma", "r3.ma",
				new LongMatePair(580, 2400), "qbamannotate", "0.0",
				"not applicable");
		File fileC = new File("output.sam");
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		boolean allRecordsHaveZM = true;
		int count = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			String zm = record.getStringAttribute("ZM");
			if (null == zm) {
				allRecordsHaveZM = false;
			} else {
				Integer zmInteger = Integer.parseInt(zm);
				int zmInt = zmInteger.intValue();
				if (1 == zmInt) {
					count++;
				}
			}
		}
		assertTrue(allRecordsHaveZM);
		assertTrue(3 == count);

		reader.close();
		reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			if (record.getReadName().equals("2_22_285")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(1 == zmInt);
				} else {
					fail();
				}
			} else if (record.getReadName().equals("2_22_361")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(1 == zmInt);
				} else {
					fail();
				}
			} else if (record.getReadName().equals("2_22_541")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(1 == zmInt);
				} else {
					fail();
				}
			} else {
				String zm = record.getStringAttribute("ZM");
				assertTrue(null != zm);
			}
		}
	}

	@Test
	public final void constructWithValidArgumentsF3Only() throws Exception {
		ExpectedException.none();
		new Annotator("output.sam", "test.sam", "f3.ma", "qbamannotate", "0.0",
				"not applicable");
		File fileC = new File("output.sam");
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		boolean allRecordsHaveZM = true;
		int count = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			String zm = record.getStringAttribute("ZM");
			if (null == zm) {
				allRecordsHaveZM = false;
			} else {
				Integer zmInteger = Integer.parseInt(zm);
				int zmInt = zmInteger.intValue();
				if (1 == zmInt) {
					count++;
				}
			}
		}
		assertTrue(allRecordsHaveZM);
		assertTrue(3 == count);
		reader.close();
		reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			if (record.getReadName().equals("2_22_285")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(1 == zmInt);
				} else {
					fail();
				}
			} else if (record.getReadName().equals("2_22_361")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(1 == zmInt);
				} else {
					fail();
				}
			} else if (record.getReadName().equals("2_22_541")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(1 == zmInt);
				} else {
					fail();
				}
			} else {
				String zm = record.getStringAttribute("ZM");
				assertTrue(null != zm);
			}
		}
	}

	@Test
	public final void constructWithValidArgumentsR3Only() throws Exception {
		ExpectedException.none();
		new Annotator("output.sam", "test.sam", "r3.ma", new LongMatePair(0,
				10000), "qbamannotate", "0.0", "not applicable");
		File fileC = new File("output.sam");
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		boolean allRecordsHaveZM = true;
		int count = 0;
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			String zm = record.getStringAttribute("ZM");
			if (null == zm) {
				allRecordsHaveZM = false;
			} else {
				Integer zmInteger = Integer.parseInt(zm);
				int zmInt = zmInteger.intValue();
				if (1 == zmInt) {
					count++;
				}
			}
		}
		assertTrue(allRecordsHaveZM);
		assertTrue(0 == count);
		reader.close();
		reader = SAMFileReaderFactory.createSAMFileReader(fileC);
		for (@SuppressWarnings("unused")
		SAMRecord record : reader) {
			if (record.getReadName().equals("2_22_285")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(-1 == zmInt);
				} else {
					fail();
				}
			} else if (record.getReadName().equals("2_22_361")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(-1 == zmInt);
				} else {
					fail();
				}
			} else if (record.getReadName().equals("2_22_541")
					&& record.getReadPairedFlag()
					&& record.getFirstOfPairFlag()) {
				String zm = record.getStringAttribute("ZM");
				if (null != zm) {
					Integer zmInteger = Integer.parseInt(zm);
					int zmInt = zmInteger.intValue();
					assertTrue(-1 == zmInt);
				} else {
					fail();
				}
			} else {
				String zm = record.getStringAttribute("ZM");
				assertTrue(null != zm);
			}
		}
	}
}
