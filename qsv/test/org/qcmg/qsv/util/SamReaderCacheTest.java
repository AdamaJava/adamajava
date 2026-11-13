package org.qcmg.qsv.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.picard.SAMFileReaderFactory;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;

public class SamReaderCacheTest {

	private static final File TEST_BAM = new File("src/resources/example.test.bam");
	private static final File CONTROL_BAM = new File("src/resources/example.control.bam");

	private static String contig;
	private static int start;
	private static int end;

	@BeforeClass
	public static void findQueryRegion() throws Exception {
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(TEST_BAM, "silent");
				SAMRecordIterator iter = reader.iterator()) {
			while (iter.hasNext()) {
				SAMRecord r = iter.next();
				if ( ! r.getReadUnmappedFlag()) {
					contig = r.getReferenceName();
					start = Math.max(1, r.getAlignmentStart() - 1000);
					end = r.getAlignmentStart() + 1000;
					break;
				}
			}
		}
		assertTrue("expected a mapped record in " + TEST_BAM, contig != null);
	}

	@Test
	public void sameFileReturnsSameReader() throws Exception {
		try (SamReaderCache cache = new SamReaderCache()) {
			SamReader a = cache.get(TEST_BAM);
			assertSame(a, cache.get(TEST_BAM));
			assertNotSame(a, cache.get(CONTROL_BAM));
		}
	}

	@Test
	public void repeatedQueriesOnCachedReaderMatchFreshReader() throws Exception {
		int expected = countFresh();
		assertTrue(expected > 1);

		try (SamReaderCache cache = new SamReaderCache()) {
			SamReader reader = cache.get(TEST_BAM);

			// early break, as the rescue code does when a region has too many reads
			try (SAMRecordIterator iter = reader.queryOverlapping(contig, start, end)) {
				iter.next();
			}
			// a second query on the shared reader must work and see every record
			for (int i = 0; i < 3; i++) {
				assertEquals(expected, count(cache.get(TEST_BAM)));
			}
		}
	}

	@Test
	public void reopensAfterClose() throws Exception {
		SamReaderCache cache = new SamReaderCache();
		SamReader first = cache.get(TEST_BAM);
		cache.close();
		SamReader second = cache.get(TEST_BAM);
		assertNotSame(first, second);
		assertEquals(countFresh(), count(second));
		cache.close();
	}

	private static int countFresh() throws Exception {
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(TEST_BAM, "silent")) {
			return count(reader);
		}
	}

	private static int count(SamReader reader) {
		int n = 0;
		try (SAMRecordIterator iter = reader.queryOverlapping(contig, start, end)) {
			while (iter.hasNext()) {
				iter.next();
				n++;
			}
		}
		return n;
	}
}
