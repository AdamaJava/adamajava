package org.qcmg.qsv.isize;

import static org.junit.Assert.assertEquals;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.samtools.SAMFileHeader.SortOrder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.annotate.RunTypeRecord;

public class SAMRecordCounterMTTest {
	
	private static final String END_OF_READ = "\tACTCCATTTCTAGAAAAAAATTAGAAAATTAACTGGAACCAGGAGAGGTG	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIHIIIIIII@	MD:Z:18C31	RG:Z:20110221052813657	NH:i:1	CM:i:2	NM:i:1	SM:i:97	ZP:Z:ABC	CQ:Z:BBBBBBBBABB>>7AA:@??B??;>@?>5>@@B@@?1:@?=81<::>=?@	CS:Z:T31220130022322000000303220003030121020101202222011\n";
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	
	@Test
	public void testCountReaderWorker() throws QSVException, IOException {
		File file = testFolder.newFile("test.bam");
		createBamFile(file);
		SAMRecordCounterMT worker = new SAMRecordCounterMT(file);
		List<RunTypeRecord> records = worker.getRunRecords();
		assertEquals(1, records.size());
		RunTypeRecord record = records.get(0);
		assertEquals("20110221052813657", record.getRgId());
		assertEquals(725, record.getUpper());
		assertEquals(85, record.getLower());
	}

	private void createBamFile(File file) throws IOException {
		List<String> data = new ArrayList<String>();
		data.addAll(createSamHeader(SortOrder.unsorted));

		try (BufferedWriter out = new BufferedWriter(new FileWriter(file));) {
	        for (final String line : data) {
	            out.write(line + "\n");
	        }
	        getSAMRecords(out);
		}
		
	}

	private Collection<? extends String> createSamHeader(SortOrder sort) {
		List<String> data = new ArrayList<String>();
		 data.add("@HD	VN:1.0	GO:none	SO:"+ sort.name());
	        data.add("@SQ	SN:chr1	LN:249250621	");
	        data.add("@SQ	SN:chr4	LN:191154276	");
	        data.add("@SQ	SN:chr7	LN:159138663	");
	        data.add("@SQ	SN:chrX	LN:155270560	");
	        data.add("@SQ	SN:chrY	LN:59373566	");
	        data.add("@SQ	SN:chr19	LN:59128983	");
	        data.add("@SQ	SN:GL000191.1	LN:106433	");
	        data.add("@SQ	SN:GL000211.1	LN:166566	");
	        data.add("@SQ	SN:chrMT	LN:16569	");
	        data.add("@RG	ID:20110221052813657	PL:SOLiD	PU:bioscope-pairing	LB:Library_20100702_A	PI:1355	DS:RUNTYPE{50x50MP}	DT:2011-02-21T15:28:13+1000	SM:S1	ZC:Z:1:S0049_20100000_1_LMP");
	        data.add("@PG	ID:1f71f335-5c8d-4b6c-a125-ecf9a0c6f7e6	PN:qbamannotate	VN:0.3pre (3663)	CL:qbamannotate --self -t pebc -l 350 -u 2360 /panfs/imb/seq_mapped/dot_20101130_1_LMP/20110218/pairing/dot_20101130_1_LMP_F3-R3-Paired.annotated.bam /panfs/imb/seq_mapped/dot_20101130_1_LMP/20110218/pairing/F3-R3-Paired.bam");
	       return data;
	}

	private void getSAMRecords(BufferedWriter out) throws IOException {
		
		createRecords(229,"140191020\t0", out);
		createRecords(2,"140191020\t40", out);
		createRecords(2,"140191020\t80", out);
		createRecords(376,"140191020\t90", out);
		createRecords(2820,"140191020\t100", out);
		createRecords(5772,"140191020\t110", out);
		createRecords(8896,"140191020\t120", out);
		createRecords(11358,"140191020\t130", out);
		createRecords(12626,"140191020\t140", out);
		createRecords(13437,"140191020\t150", out);
		createRecords(14032,"140191020\t160", out);
		createRecords(14451,"140191020\t170", out);
		createRecords(14537,"140191020\t180", out);
		createRecords(14615,"140191020\t190", out);
		createRecords(15806,"140191020\t200", out);
		createRecords(16309,"140191020\t210", out);
		createRecords(17540,"140191020\t220", out);
		createRecords(20509,"140191020\t230", out);
		createRecords(24656,"140191020\t240", out);
		createRecords(28513,"140191020\t250", out);
		createRecords(34720,"140191020\t260", out);
		createRecords(40961,"140191020\t270", out);
		createRecords(49129,"140191020\t280", out);
		createRecords(58384,"140191020\t290", out);
		createRecords(71585,"140191020\t300", out);
		createRecords(73491,"140191020\t310", out);
		createRecords(67551,"140191020\t320", out);
		createRecords(57469,"140191020\t330", out);
		createRecords(46505,"140191020\t340", out);
		createRecords(35617,"140191020\t350", out);
		createRecords(26385,"140191020\t360", out);
		createRecords(17762,"140191020\t370", out);
		createRecords(10119,"140191020\t380", out);
		createRecords(4459,"140191020\t390", out);
		createRecords(1704,"140191020\t400", out);
		createRecords(782,"140191020\t410", out);
		createRecords(536,"140191020\t420", out);
		createRecords(372,"140191020\t430", out);
		createRecords(268,"140191020\t440", out);
		createRecords(266,"140191020\t450", out);
		createRecords(234,"140191020\t460", out);
		createRecords(230,"140191020\t470", out);
		createRecords(254,"140191020\t480", out);
		createRecords(214,"140191020\t490", out);
		createRecords(180,"140191020\t500", out);
		createRecords(148,"140191020\t510", out);
		createRecords(178,"140191020\t520", out);
		createRecords(64,"140191020\t530", out);
		createRecords(44,"140191020\t540", out);
		createRecords(30,"140191020\t550", out);
		createRecords(44,"140191020\t560", out);
		createRecords(20,"140191020\t570", out);
		createRecords(30,"140191020\t580", out);
		createRecords(22,"140191020\t590", out);
		createRecords(32,"140191020\t600", out);
		createRecords(28,"140191020\t610", out);
		createRecords(42,"140191020\t620", out);
		createRecords(22,"140191020\t630", out);
		createRecords(18,"140191020\t640", out);
		createRecords(10,"140191020\t650", out);
		createRecords(2,"140191020\t660", out);
		createRecords(10,"140191020\t670", out);
		createRecords(4,"140191020\t680", out);
		createRecords(8,"140191020\t690", out);
		createRecords(32,"140191020\t700", out);
		createRecords(22,"140191020\t710", out);
		createRecords(18,"140191020\t720", out);
		createRecords(8,"140191020\t730", out);
		createRecords(4,"140191020\t780", out);
		createRecords(2,"140191020\t790", out);
		createRecords(8,"140191020\t800", out);
		createRecords(2,"140191020\t810", out);
		createRecords(4,"140191020\t820", out);
		createRecords(2,"140191020\t860", out);
	}

	private void createRecords(int size, String dataString, BufferedWriter out) throws IOException {
		String s = "	129	chr7	140191000	63	50M	chr7\t"+dataString+END_OF_READ;
		for (int i=0; i<= size; i++) {
			out.write("254_166_1407" + i + s);
		}
	}
}
