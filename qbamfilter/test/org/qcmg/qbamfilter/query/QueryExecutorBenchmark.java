package org.qcmg.qbamfilter.query;

import htsjdk.samtools.BAMRecordCodec;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;

import org.qcmg.picard.SAMFileReaderFactory;

/**
 * Micro-benchmark for QueryExecutor. Not a unit test - run with:
 * <pre>./gradlew :qbamfilter:benchmark [-Pbam=/path/to.bam] [-Ppasses=N]</pre>
 *
 * The BAM is read once and each record re-encoded into an in-memory byte array.
 * Before every pass those bytes are decoded (untimed) into fresh, lazily decoded BAMRecords, exactly as
 * a SamReader would hand them to a caller. Only the QueryExecutor calls are timed, so the cost of
 * any cigar/tag decoding triggered by the filter is included.
 */
public class QueryExecutorBenchmark {

	static final String[] QUERIES = {
		// qsv DEFAULT_CLIP_QUERY / lifescope query
		"and (Cigar_M > 34, MD_mismatch < 3, MapQ > 0, flag_DuplicateRead == false)",
		// qsv pe query
		"and (Cigar_M > 34, MD_mismatch < 3, option_SM > 10, flag_DuplicateRead == false)",
		// qannotate OverlapMode
		"and (Flag_DuplicateRead == false, CIGAR_M > 34, MAPQ >10, MD_mismatch <= 3 )",
		// nested, with string tag comparisons
		"or (and (option_ZP =~ AA*, option_RG != foo, flag_ReadPaired == true), and (MAPQ > 59, flag_DuplicateRead == true))",
	};

	public static void main(String[] args) throws Exception {
		String bam = args.length > 0 ? args[0] : "../qsv/src/resources/example.test.bam";
		int passes = args.length > 1 ? Integer.parseInt(args[1]) : 200;
		int warmup = Math.max(passes / 4, 10);

		SAMFileHeader header;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int recordCount = 0;
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(bam))) {
			header = reader.getFileHeader();
			BAMRecordCodec encoder = new BAMRecordCodec(header);
			encoder.setOutputStream(baos);
			for (SAMRecord r : reader) {
				encoder.encode(r);
				recordCount++;
			}
		}
		byte[] data = baos.toByteArray();
		System.out.printf("%s: %d records, %d passes (+%d warmup)%n", bam, recordCount, passes, warmup);

		SAMRecord[] records = new SAMRecord[recordCount];
		for (String query : QUERIES) {
			QueryExecutor exec = new QueryExecutor(query);
			long[] filterNanos = new long[passes];
			long passed = 0;
			for (int i = -warmup; i < passes; i++) {
				// fresh records each pass, so that nothing decoded by a previous pass is cached
				decode(header, data, records);
				long t0 = System.nanoTime();
				long result = 0;
				for (SAMRecord r : records) {
					if (exec.Execute(r)) {
						result++;
					}
				}
				long t1 = System.nanoTime();
				if (i >= 0) {
					filterNanos[i] = t1 - t0;
					passed = result;
				}
			}
			Arrays.sort(filterNanos);
			double nsPerRead = (double) filterNanos[passes / 2] / recordCount;
			System.out.printf("%-120s passed=%6d  filter=%7.1f ns/read (median)%n", query, passed, nsPerRead);
		}
	}

	private static void decode(SAMFileHeader header, byte[] data, SAMRecord[] records) {
		BAMRecordCodec codec = new BAMRecordCodec(header);
		codec.setInputStream(new ByteArrayInputStream(data));
		for (int i = 0; i < records.length; i++) {
			records[i] = codec.decode();
		}
	}
}
