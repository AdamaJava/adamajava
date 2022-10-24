package org.qcmg.picard;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SAMRecordValidationTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void testRecord() throws IOException {
		File samFile = testFolder.newFile("testRecord.sam");
		OutputStream os = new FileOutputStream(samFile);
		PrintStream ps = new PrintStream(os);
		try {
			ps.println("@HD	VN:1.0	GO:none	SO:unsorted");
			ps.println("@SQ	SN:GL000246.1	LN:249250621	UR:file:/path/reference.fa");
			ps.println("@RG	ID:20130202070519571	PL:Plat1	PU:Unit" +
					"	LB:50x50MP	PI:1535	DT:2010-06-01T16:21:27+1000	SM:S1");
			ps.println("HWI-ST1243:135:D1HU9ACXX:4:1114:8509:18271	163	GL000246.1	3012	0	101M	=	3159	248	TTTTAATGTTGAAAATAGGCCACCAGTCTCTGGTTTCTGCTAAGAGGTCTGCTGCCATCCTGATGGTGTTCTCTCTATAAGGGACCTGCCACTTCTCTCTA	C@@FFFFFHHFHHGHCFHIIIJIJIIFHGIIJGFFFGGIGFHIJGIJBGHHG*?D@FGIIJEGIHI=CGGGJJJGEHHH>=);;BACCCD@>CD@ACCDC3	X0:i:2	X1:i:0	XA:Z:chr22,-17275221,101M,1;	ZC:i:3	MD:Z:81T19	RG:Z:20130202070519571	XG:i:0	AM:i:0	NM:i:1	SM:i:0	XM:i:1	XO:i:0	XT:A:R");
		} finally {
			try {
				ps.close();
			} finally {
				os.close();
			}
		}
	
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(samFile);
		SAMRecordIterator iter = reader.iterator();
		assertEquals(true, iter.hasNext());
		SAMRecord rec = iter.next();
		assertEquals(true, null != rec);
		assertEquals("HWI-ST1243:135:D1HU9ACXX:4:1114:8509:18271", rec.getReadName());
	}

}
