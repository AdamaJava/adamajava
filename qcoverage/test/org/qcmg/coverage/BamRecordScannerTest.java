package org.qcmg.coverage;

import htsjdk.samtools.BAMFileReader;
import htsjdk.samtools.BAMFileSpan;
import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.CloseableIterator;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.picard.SAMFileReaderFactory;

/**
 * Tests that the fast BAM reader calculates the same coverage as htsjdk's BAM reader.
 */
public class BamRecordScannerTest {

	private static final int REF_LENGTH = 100000;
	private static Path tmpDir;
	private static File bamFile;

	@BeforeClass
	public static void setup() throws Exception {
		tmpDir = Files.createTempDirectory(null);
		bamFile = new File(tmpDir.toFile(), "scanner.bam");
		SequenceCoverageTest.createCoverageBam(bamFile.getAbsolutePath(), createRecords(),
				SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate));
	}

	@AfterClass
	public static void tearDown() {
		if (null != tmpDir) {
			tmpDir.toFile().delete();
		}
	}

	@Test
	public void testPhysicalCoverage() throws Exception {
		assertCoverageMatches(new PhysicalCoverageAlgorithm());
	}

	@Test
	public void testSequenceCoverage() throws Exception {
		assertCoverageMatches(new SequenceCoverageAlgorithm());
	}

	@Test
	public void testLowReadDepthCoverage() throws Exception {
		assertCoverageMatches(new LowReadDepthAlgorithm(8));
	}

	/**
	 * Calculates the coverage of each of the reference sequences using both the fast BAM
	 * reader and htsjdk's BAM reader, and checks that the results are the same.
	 */
	private void assertCoverageMatches(final Algorithm algorithm) throws Exception {
		final SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile);
		try {
			final BAMFileReader bamFileReader = SAMFileReaderFactory.getBAMFileReader(reader);
			Assert.assertNotNull(bamFileReader);
			final SAMFileHeader header = reader.getFileHeader();
			long totalCoverage = 0;
			for (final String refName : Arrays.asList("chr1", "chr2", "chr3")) {
				final int refIndex = header.getSequenceIndex(refName);
				final long[] filePointers = getFilePointers(bamFileReader, refIndex);
				Assert.assertNotNull(filePointers);

				final int[] expected = CoverageArrayUtils.toPerBaseCoverage(
						coverageUsingHtsjdk(reader, algorithm, refName));
				final int[] actual = CoverageArrayUtils.toPerBaseCoverage(
						coverageUsingFastReader(filePointers, refIndex, algorithm));

				Assert.assertArrayEquals("coverage of " + refName, expected, actual);

				totalCoverage += Arrays.stream(expected).sum();
			}

			/*
			 * Sanity check that the reads have actually contributed to the coverage.
			 * Note that this can only be checked in aggregate, as not every reference
			 * sequence is covered by every algorithm (e.g. the physical coverage algorithm
			 * ignores paired reads with a negative insert size).
			 */
			Assert.assertTrue("coverage should not be empty", totalCoverage > 0);
		} finally {
			reader.close();
		}
	}

	/**
	 * @return the file pointers of the reads of the given reference sequence
	 */
	private long[] getFilePointers(final BAMFileReader reader, final int refIndex) {
		final BAMFileSpan span = BAMFileReader.getFileSpan(
				new QueryInterval[] { new QueryInterval(refIndex, 0, 0) }, reader.getIndex());
		return (null == span) ? null : span.toCoordinateArray();
	}

	/**
	 * Calculates the coverage of the given reference sequence using htsjdk's BAM reader.
	 */
	private int[] coverageUsingHtsjdk(final SamReader reader, final Algorithm algorithm, final String refName) {
		final int[] coverage = new int[REF_LENGTH];
		try (CloseableIterator<SAMRecord> iter = reader.query(refName, 0, 0, false)) {
			while (iter.hasNext()) {
				algorithm.applyTo(iter.next(), coverage);
			}
		}
		return coverage;
	}

	/**
	 * Calculates the coverage of the given reference sequence using the fast BAM reader.
	 */
	private int[] coverageUsingFastReader(final long[] filePointers, final int refIndex, final Algorithm algorithm)
			throws Exception {
		final int[] coverage = new int[REF_LENGTH];
		try (BamRecordScanner scanner = new BamRecordScanner(bamFile, filePointers, refIndex)) {
			while (scanner.next()) {
				algorithm.applyTo(scanner.getAlignmentStart(), scanner.getAlignmentEnd(), scanner.getInsertSize(),
						scanner.isPaired(), coverage);
			}
		}
		return coverage;
	}

	/**
	 * @return a collection of reads that cover the types of read that the fast BAM reader
	 *         has to be able to process
	 */
	private static List<SAMRecord> createRecords() {
		final SAMFileHeader header = SequenceCoverageTest.createSamHeaderObject(SortOrder.coordinate);
		final List<SAMRecord> recs = new ArrayList<>();

		// paired reads, with a variety of CIGAR strings
		addRecord(recs, header, "chr1", 100, 0x1 | 0x2 | 0x20 | 0x40, "100M", 250, 250);
		addRecord(recs, header, "chr1", 200, 0x1 | 0x2 | 0x10 | 0x80, "10S90M", 300, -300);
		addRecord(recs, header, "chr1", 300, 0x1 | 0x2 | 0x20 | 0x40, "50M10D50M", 0, 0);
		addRecord(recs, header, "chr1", 400, 0x1 | 0x2 | 0x20 | 0x40, "50M100N50M", 0, 0);
		addRecord(recs, header, "chr1", 500, 0x1 | 0x2 | 0x20 | 0x40, "50=50X", 0, 0);
		addRecord(recs, header, "chr1", 600, 0x1 | 0x2 | 0x20 | 0x40, "5H95M", 0, 0);

		// single end reads
		addRecord(recs, header, "chr1", 700, 0, "100M", 0, 0);
		addRecord(recs, header, "chr1", 800, 0, "50M10I50M", 0, 0);

		// secondary and supplementary alignments
		addRecord(recs, header, "chr1", 900, 0x1 | 0x2 | 0x20 | 0x100, "100M", 0, 0);
		addRecord(recs, header, "chr1", 950, 0x1 | 0x2 | 0x20 | 0x800, "100M", 0, 0);

		// an unmapped read that is placed at the position of its mate
		addRecord(recs, header, "chr1", 1000, 0x1 | 0x4 | 0x8 | 0x20, "100M", 1100, 100);

		// the other reference sequences
		addRecord(recs, header, "chr2", 100, 0x1 | 0x2 | 0x20 | 0x40, "100M", 250, 250);
		addRecord(recs, header, "chr2", 200, 0, "80M", 0, 0);
		addRecord(recs, header, "chr3", 100, 0x1 | 0x2 | 0x10 | 0x80, "100M", 250, -250);

		return recs;
	}

	private static void addRecord(final List<SAMRecord> recs, final SAMFileHeader header, final String refName,
			final int position, final int flags, final String cigar, final int matePosition, final int insertSize) {
		final SAMRecord rec = new SAMRecord(header);
		rec.setReadName("read_" + refName + "_" + position);
		rec.setReferenceName(refName);
		rec.setAlignmentStart(position);
		rec.setCigarString(cigar);
		rec.setFlags(flags);
		rec.setMateReferenceName(refName);
		rec.setMateAlignmentStart(matePosition);
		rec.setInferredInsertSize(insertSize);
		rec.setReadString("A".repeat(100));
		rec.setBaseQualityString("I".repeat(100));
		rec.setMappingQuality(60);
		recs.add(rec);
	}
}
