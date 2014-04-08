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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.QSVException;
import org.qcmg.qsv.annotate.RunTypeRecord;

public class SAMRecordCounterMTTest {
	
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
		data.addAll(getSAMRecords());

		BufferedWriter out = new BufferedWriter(new FileWriter(file));
        for (final String line : data) {
            out.write(line + "" + "\n");
        }
        out.close();
		
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

	private Collection<? extends String> getSAMRecords() {
		List<String> data = new ArrayList<String>();
		
		data.addAll(createRecords(229,"140191020\t0"));
		data.addAll(createRecords(2,"140191020\t40"));
		data.addAll(createRecords(2,"140191020\t80"));
		data.addAll(createRecords(376,"140191020\t90"));
		data.addAll(createRecords(2820,"140191020\t100"));
		data.addAll(createRecords(5772,"140191020\t110"));
		data.addAll(createRecords(8896,"140191020\t120"));
		data.addAll(createRecords(11358,"140191020\t130"));
		data.addAll(createRecords(12626,"140191020\t140"));
		data.addAll(createRecords(13437,"140191020\t150"));
		data.addAll(createRecords(14032,"140191020\t160"));
		data.addAll(createRecords(14451,"140191020\t170"));
		data.addAll(createRecords(14537,"140191020\t180"));
		data.addAll(createRecords(14615,"140191020\t190"));
		data.addAll(createRecords(15806,"140191020\t200"));
		data.addAll(createRecords(16309,"140191020\t210"));
		data.addAll(createRecords(17540,"140191020\t220"));
		data.addAll(createRecords(20509,"140191020\t230"));
		data.addAll(createRecords(24656,"140191020\t240"));
		data.addAll(createRecords(28513,"140191020\t250"));
		data.addAll(createRecords(34720,"140191020\t260"));
		data.addAll(createRecords(40961,"140191020\t270"));
		data.addAll(createRecords(49129,"140191020\t280"));
		data.addAll(createRecords(58384,"140191020\t290"));
		data.addAll(createRecords(71585,"140191020\t300"));
		data.addAll(createRecords(73491,"140191020\t310"));
		data.addAll(createRecords(67551,"140191020\t320"));
		data.addAll(createRecords(57469,"140191020\t330"));
		data.addAll(createRecords(46505,"140191020\t340"));
		data.addAll(createRecords(35617,"140191020\t350"));
		data.addAll(createRecords(26385,"140191020\t360"));
		data.addAll(createRecords(17762,"140191020\t370"));
		data.addAll(createRecords(10119,"140191020\t380"));
		data.addAll(createRecords(4459,"140191020\t390"));
		data.addAll(createRecords(1704,"140191020\t400"));
		data.addAll(createRecords(782,"140191020\t410"));
		data.addAll(createRecords(536,"140191020\t420"));
		data.addAll(createRecords(372,"140191020\t430"));
		data.addAll(createRecords(268,"140191020\t440"));
		data.addAll(createRecords(266,"140191020\t450"));
		data.addAll(createRecords(234,"140191020\t460"));
		data.addAll(createRecords(230,"140191020\t470"));
		data.addAll(createRecords(254,"140191020\t480"));
		data.addAll(createRecords(214,"140191020\t490"));
		data.addAll(createRecords(180,"140191020\t500"));
		data.addAll(createRecords(148,"140191020\t510"));
		data.addAll(createRecords(178,"140191020\t520"));
		data.addAll(createRecords(64,"140191020\t530"));
		data.addAll(createRecords(44,"140191020\t540"));
		data.addAll(createRecords(30,"140191020\t550"));
		data.addAll(createRecords(44,"140191020\t560"));
		data.addAll(createRecords(20,"140191020\t570"));
		data.addAll(createRecords(30,"140191020\t580"));
		data.addAll(createRecords(22,"140191020\t590"));
		data.addAll(createRecords(32,"140191020\t600"));
		data.addAll(createRecords(28,"140191020\t610"));
		data.addAll(createRecords(42,"140191020\t620"));
		data.addAll(createRecords(22,"140191020\t630"));
		data.addAll(createRecords(18,"140191020\t640"));
		data.addAll(createRecords(10,"140191020\t650"));
		data.addAll(createRecords(2,"140191020\t660"));
		data.addAll(createRecords(10,"140191020\t670"));
		data.addAll(createRecords(4,"140191020\t680"));
		data.addAll(createRecords(8,"140191020\t690"));
		data.addAll(createRecords(32,"140191020\t700"));
		data.addAll(createRecords(22,"140191020\t710"));
		data.addAll(createRecords(18,"140191020\t720"));
		data.addAll(createRecords(8,"140191020\t730"));
		data.addAll(createRecords(4,"140191020\t780"));
		data.addAll(createRecords(2,"140191020\t790"));
		data.addAll(createRecords(8,"140191020\t800"));
		data.addAll(createRecords(2,"140191020\t810"));
		data.addAll(createRecords(4,"140191020\t820"));
		data.addAll(createRecords(2,"140191020\t860"));
		return data;
	}

	private Collection<? extends String> createRecords(int size, String dataString) {
		List<String> data = new ArrayList<String>();
		for (int i=0; i<= size; i++) {
			data.add(getSAMString(i, dataString));		
		}
		
		return data;
	}

	private String getSAMString(int i, String isize) {
		return "254_166_1407"+i+"	129	chr7	140191000	63	50M	chr7\t"+isize+"\tACTCCATTTCTAGAAAAAAATTAGAAAATTAACTGGAACCAGGAGAGGTG	IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIHIIIIIII@	MD:Z:18C31	RG:Z:20110221052813657	NH:i:1	CM:i:2	NM:i:1	SM:i:97	ZP:Z:ABC	CQ:Z:BBBBBBBBABB>>7AA:@??B??;>@?>5>@@B@@?1:@?=81<::>=?@	CS:Z:T31220130022322000000303220003030121020101202222011";
	}
}
