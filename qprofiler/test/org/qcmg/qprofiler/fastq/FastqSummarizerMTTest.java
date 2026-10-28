package org.qcmg.qprofiler.fastq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FastqSummarizerMTTest {

	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();

	@Test
	public void sameResultsAsSingleThreaded() throws Exception {
		File f = testFolder.newFile("test.fastq");
		// more than a few batches worth of records, so that all consumers get some
		writeFastq(f, 10 * FastqSummarizerMT.BATCH_SIZE + 17, false);

		FastqSummaryReport st = (FastqSummaryReport) new FastqSummarizer().summarize(f);
		for (int threads : new int[] {1, 2, 4, 8}) {
			FastqSummaryReport mt = (FastqSummaryReport) new FastqSummarizerMT(threads).summarize(f);
			assertEquals(st.getRecordsParsed(), mt.getRecordsParsed());
			assertEquals("threads: " + threads, toXml(st), toXml(mt));
		}
	}

	@Test
	public void gzippedInput() throws Exception {
		File f = testFolder.newFile("test.fastq");
		File gz = testFolder.newFile("test.fastq.gz");
		writeFastq(f, 5 * FastqSummarizerMT.BATCH_SIZE + 3, false);
		writeFastq(gz, 5 * FastqSummarizerMT.BATCH_SIZE + 3, true);

		FastqSummaryReport plain = (FastqSummaryReport) new FastqSummarizerMT(4).summarize(f);
		FastqSummaryReport gzipped = (FastqSummaryReport) new FastqSummarizerMT(4).summarize(gz);
		assertEquals(5 * FastqSummarizerMT.BATCH_SIZE + 3, gzipped.getRecordsParsed());
		// file name is part of the xml
		gzipped.setFileName(plain.getFileName());
		assertEquals(toXml(plain), toXml(gzipped));
	}

	@Test
	public void excludesArePassedOn() throws Exception {
		File f = testFolder.newFile("test.fastq");
		writeFastq(f, 100, false);

		FastqSummaryReport all = (FastqSummaryReport) new FastqSummarizerMT(4).summarize(f);
		FastqSummaryReport excludeAll = (FastqSummaryReport) new FastqSummarizerMT(4, new String[] {"all"}).summarize(f);
		assertEquals(100, excludeAll.getRecordsParsed());
		assertTrue(toXml(all).contains("ReadNameAnalysis"));
		assertFalse(toXml(excludeAll).contains("ReadNameAnalysis"));
	}

	@Test
	public void emptyFile() throws Exception {
		File f = testFolder.newFile("empty.fastq");
		FastqSummaryReport sr = (FastqSummaryReport) new FastqSummarizerMT(4).summarize(f);
		assertEquals(0, sr.getRecordsParsed());
	}

	@Test
	public void invalidRecordThrowsException() throws Exception {
		File f = testFolder.newFile("invalid.fastq");
		writeFastq(f, 3 * FastqSummarizerMT.BATCH_SIZE, false);
		try (PrintWriter pw = new PrintWriter(new FileOutputStream(f, true))) {
			pw.println("@read_missing_qual_header");
			pw.println("ACGT");
			pw.println("ACGT");
		}
		try {
			new FastqSummarizerMT(4).summarize(f);
			fail("Should have thrown an Exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().length() > 0);
		}
	}

	@Test
	public void consumerFailureThrowsException() throws Exception {
		File f = testFolder.newFile("badheader.fastq");
		writeFastq(f, 3 * FastqSummarizerMT.BATCH_SIZE, false);
		try (PrintWriter pw = new PrintWriter(new FileOutputStream(f, true))) {
			// 5 element header with a non-numeric tile number - fails when the record is summarised (by a consumer thread)
			pw.println("@INSTRUMENT:1:NOT_A_TILE:100:200/1");
			pw.println("ACGT");
			pw.println("+");
			pw.println("IIII");
		}
		try {
			new FastqSummarizerMT(4).summarize(f);
			fail("Should have thrown an Exception");
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof NumberFormatException);
		}
	}

	private static String toXml(FastqSummaryReport report) throws Exception {
		// times are part of the xml
		report.setStartTime("start");
		report.setFinishTime("finish");
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		doc.appendChild(root);
		report.toXml(root);
		StringWriter sw = new StringWriter();
		TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(sw));
		return sw.toString();
	}

	/*
	 * Writes reads of varying length, with some Ns, and Casava 1.8 style headers
	 */
	private static void writeFastq(File f, int noOfRecords, boolean gzip) throws IOException {
		Random random = new Random(42);
		char[] bases = {'A', 'C', 'G', 'T'};
		OutputStream os = new FileOutputStream(f);
		if (gzip) {
			os = new GZIPOutputStream(os);
		}
		try (Writer w = new OutputStreamWriter(os, StandardCharsets.US_ASCII)) {
			for (int i = 0 ; i < noOfRecords ; i++) {
				int length = 100 + random.nextInt(51);
				StringBuilder seq = new StringBuilder(length);
				StringBuilder qual = new StringBuilder(length);
				for (int j = 0 ; j < length ; j++) {
					seq.append(random.nextInt(100) == 0 ? 'N' : bases[random.nextInt(4)]);
					qual.append((char) (33 + 2 + random.nextInt(39)));
				}
				w.write("@A00123:45:HXXXXDSXY:" + (1 + i % 4) + ":" + (1101 + i % 50) + ":" + i + ":" + (i % 9000) + " " + (1 + i % 2) + ":N:0:ACGTACGT\n");
				w.write(seq + "\n+\n" + qual + "\n");
			}
		}
	}
}
