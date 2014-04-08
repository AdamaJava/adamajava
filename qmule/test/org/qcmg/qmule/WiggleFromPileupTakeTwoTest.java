package org.qcmg.qmule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.util.FileUtils;

public class WiggleFromPileupTakeTwoTest {
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	private File pileupFile;
	private File gff3File;
	private File wiggleFile;
	private File pileupFileGZIP;
	private File wiggleFileGZIP;
	
	@Before
	public final void before() {
		try {
			pileupFile = tempFolder.newFile("wigglePileupTest.pileup");
			wiggleFile = tempFolder.newFile("wigglePileupTest.wiggle");
			gff3File = tempFolder.newFile("wigglePileupTest.gff3");
			pileupFileGZIP = tempFolder.newFile("wigglePileupTest.pileup.gz");
			wiggleFileGZIP = tempFolder.newFile("wigglePileupTest.wiggle.gz");
			createPileupFile(pileupFile);
			createPileupFile(pileupFileGZIP);
			createGFF3File(gff3File);
			assertTrue(pileupFile.exists());
			assertTrue(gff3File.exists());
			assertTrue(pileupFileGZIP.exists());
		} catch (Exception e) {
			System.err.println("File creation error in test harness: " + e.getMessage());
		}
	}
	
	
//	@Test
//	public void testIsPositionInBaitSingleGff() {
//		GFF3Record gff = new GFF3Record();
//		gff.setSeqId("chr1");
//		gff.setStart(1);
//		gff.setEnd(10);
//		
//		List<GFF3Record> gffs = new ArrayList<GFF3Record>();
//		gffs.add(gff);
//		Iterator<GFF3Record> iter = gffs.iterator();
////		WiggleFromPileup.setGffRecord(gff);
//		
//		Assert.assertEquals(false, WiggleFromPileupTakeTwo.isPositionInBait("chr0", 0, iter, iter.next()));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 0, iter, gff));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 1, iter, gff));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 10, iter, gff));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 11, iter, gff));
//		
//		gff.setSeqId("chrX");
//		gff.setStart(1000123);
//		gff.setEnd(1000223);
//		
////		WiggleFromPileup.setGffRecord(gff);
//		
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chrX", 0, iter, gff));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chrX", 1, iter, gff));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chrx", 1000124, iter, gff));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chrx", 11, iter, gff));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chrX", 11, iter, gff));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chrX", 1000123, iter, gff));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chrX", 1000124, iter, gff));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chrX", 1000223, iter, gff));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chrX", 1000224, iter, gff));
//	}
	
//	@Test
//	public void testIsPositionInBaitMultipleGff() {
//		GFF3Record gff1 = new GFF3Record();
//		gff1.setSeqId("chr1");
//		gff1.setStart(1);
//		gff1.setEnd(10);
//		GFF3Record gff2 = new GFF3Record();
//		gff2.setSeqId("chr1");
//		gff2.setStart(11);
//		gff2.setEnd(20);
//		GFF3Record gff3 = new GFF3Record();
//		gff3.setSeqId("chr1");
//		gff3.setStart(31);
//		gff3.setEnd(40);
//		
//		List<GFF3Record> gffs = new ArrayList<GFF3Record>();
//		gffs.add(gff1);
//		gffs.add(gff2);
//		gffs.add(gff3);
//		Iterator<GFF3Record> iter = gffs.iterator();
//		
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 0, iter, iter.next()));
//		
////		Assert.assertEquals(gff1, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 1, iter, gff1));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 10, iter, gff1));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 11, iter, gff1));
//		// iterator should have been advanced
//		Assert.assertEquals(gff2, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 20, iter, gff2));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 21, iter, gff2));
//		// iterator should have been advanced
//		Assert.assertEquals(gff3, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 29, iter, gff3));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 31, iter, gff3));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 40, iter, gff3));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 41, iter, gff3));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 141, iter, gff3));
//	}
	
//	@Test
//	public void testIsPositionInBaitMultipleGffMultipleChromosomes() {
//		GFF3Record gff1 = new GFF3Record();
//		gff1.setSeqId("chr1");
//		gff1.setStart(1);
//		gff1.setEnd(10);
//		GFF3Record gff2 = new GFF3Record();
//		gff2.setSeqId("chr1");
//		gff2.setStart(11);
//		gff2.setEnd(20);
//		GFF3Record gff3 = new GFF3Record();
//		gff3.setSeqId("chr1");
//		gff3.setStart(31);
//		gff3.setEnd(40);
//		GFF3Record gff4 = new GFF3Record();
//		gff4.setSeqId("chr2");
//		gff4.setStart(15);
//		gff4.setEnd(25);
//		GFF3Record gff5 = new GFF3Record();
//		gff5.setSeqId("chr2");
//		gff5.setStart(26);
//		gff5.setEnd(40);
//		GFF3Record gff6 = new GFF3Record();
//		gff6.setSeqId("chrX");
//		gff6.setStart(100026);
//		gff6.setEnd(100040);
//		
//		List<GFF3Record> gffs = new ArrayList<GFF3Record>();
//		gffs.add(gff1);
//		gffs.add(gff2);
//		gffs.add(gff3);
//		gffs.add(gff4);
//		gffs.add(gff5);
//		gffs.add(gff6);
//		Iterator<GFF3Record> iter = gffs.iterator();
//		
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr0", 0, iter, iter.next()));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 0, iter, gff1));
//		
////		Assert.assertEquals(gff1, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 1, iter, gff1));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 10, iter, gff1));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 11, iter, gff1));
//		// iterator should have been advanced
//		Assert.assertEquals(gff2, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 20, iter, gff2));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 21, iter, gff2));
//		// iterator should have been advanced
//		Assert.assertEquals(gff3, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 29, iter, gff3));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 31, iter, gff3));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr1", 40, iter, gff3));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 41, iter, gff3));
//		// iterator should have been advanced
//		Assert.assertEquals(gff4, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 141, iter, gff4));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 142, iter, gff4));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr1", 1000142, iter, gff4));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr2", 1, iter, gff4));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr2", 2, iter, gff4));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr2", 15, iter, gff4));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr2", 25, iter, gff4));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr2", 26, iter, gff4));
//		// iterator should have been advanced
//		Assert.assertEquals(gff5, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chr2", 40, iter, gff5));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr2", 41, iter, gff5));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr3", 15, iter, gff5));
//		// iterator should have been advanced
//		Assert.assertEquals(gff6, WiggleFromPileup.getGffRecord());
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr3", 10015, iter, gff6));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr4", 10015, iter, gff6));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr5", 10015, iter, gff6));
//		Assert.assertEquals(false, WiggleFromPileup.isPositionInBait("chr15", 10015, iter, gff6));
//		Assert.assertEquals(true, WiggleFromPileup.isPositionInBait("chrX", 100026, iter, gff6));
//		
//	}
	
	@Test
	public final void callWithNoArgs() throws Exception {
		String command = "";
		Executor exec = new Executor(command, "org.qcmg.qmule.WiggleFromPileupTakeTwo");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}
	
	@Test
	public final void callWithNoInputFile() throws Exception {
		String command = "-log ./logfile -o " + tempFolder.getRoot().getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.qmule.WiggleFromPileupTakeTwo");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}
	
	@Test
	public final void callWithMissingArgs() throws Exception {
		String command = "-log ./logfile -o blah.wiggle -i " + pileupFile.getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.qmule.WiggleFromPileupTakeTwo");
		assertTrue(1 == exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		assertTrue(0 < exec.getErrorStreamConsumer().getLines().length);
	}
	
	@Test
	public final void callWithValidArguments() throws Exception {
		ExpectedException.none();
		String command = "-log ./logfile  -pileupFormat NNTT -normalCoverage 1 -tumourCoverage 1 -i " + pileupFile.getAbsolutePath() 
		+ " -i " + gff3File.getAbsolutePath()
		+ " -gffRegions exon"
		+ " -o " + wiggleFile.getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.qmule.WiggleFromPileupTakeTwo");
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		// check the wiggle file
		InputStream reader = new FileInputStream(wiggleFile);
		assertEquals(29, examineWiggle(reader));
	}
	
	@Test
	public final void callWithValidArgumentsLargeCoverage() throws Exception {
		ExpectedException.none();
		String command = "-log ./logfile  -pileupFormat NNTT -normalCoverage 50 -tumourCoverage 50 -i " + pileupFile.getAbsolutePath() 
		+ " -i " + gff3File.getAbsolutePath()
		+ " -gffRegions exon"
		+ " -o " + wiggleFile.getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.qmule.WiggleFromPileupTakeTwo");
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		// check the wiggle file
		InputStream reader = new FileInputStream(wiggleFile);
		assertEquals(0, examineWiggle(reader));
	}
	
	@Test
	public final void callWithZippedFiles() throws Exception {
		ExpectedException.none();
		String command = "-log ./logfile  -pileupFormat NNTT -normalCoverage 20 -tumourCoverage 20 -i " + pileupFileGZIP.getAbsolutePath() 
		+ " -i " + gff3File.getAbsolutePath()
		+ " -gffRegions exon"
		+ " -o " + wiggleFileGZIP.getAbsolutePath();
		Executor exec = new Executor(command, "org.qcmg.qmule.WiggleFromPileupTakeTwo");
		assertEquals(0, exec.getErrCode());
		assertTrue(0 == exec.getOutputStreamConsumer().getLines().length);
		
		// check the wiggle file
		InputStream reader = new GZIPInputStream(new FileInputStream(wiggleFileGZIP));
		assertEquals(14, examineWiggle(reader));
	}
	
	private int examineWiggle(InputStream reader) throws IOException {
		int count = 0;
		BufferedReader fr = new BufferedReader(new InputStreamReader(reader));
		String line = fr.readLine();	// first line has the header
		while ((line = fr.readLine()) != null) {
			if (line.startsWith("fixedStep")) continue;
			count += Integer.parseInt(line);
		}
		return count;
	}
	
	private void createPileupFile(File pileupFile) throws IOException {
		
		OutputStream os = FileUtils.isFileGZip(pileupFile) ? new GZIPOutputStream( new FileOutputStream(pileupFile)) 
		: new FileOutputStream(pileupFile);
		
//		OutputStream os = new FileOutputStream(pileupFile);
		PrintStream ps = new PrintStream(os);
		
		ps.println("chr1\t14923\tG\t8\t.......^!.\tIIIIIIIE\t7\t,.....^!.\t5IIIIIE\t10\t.........^T.\t0IIIIIIIIE\t7\t...,...\tIIIIIII");
		ps.println("chr1\t14924\tA\t9\t........^!.\tEI@III?IB\t7\t,......\t@IIIIII\t10\t..........\t-IIIIIIIII\t8\t...,...^!.\tIIII/IIB");
		ps.println("chr1\t14925\tA\t11\t.........^!.^P.\tIIDIIIHIEEE\t8\t,......^N.\tBIIIIIIE\t10\t..........\t)IIIIIIIII\t8\t...,....\tIII:4IIE");
		ps.println("chr1\t14926\tT\t11\t...........\tDIIIIIIIIII\t8\t,.......\t9IIIIIII\t10\t..........\t-IIIIIIIII\t8\t...,....\tIIH;DIII");
		ps.println("chr1\t14927\tT\t11\t...........\tDIIIIIIIIII\t8\t,.......\t8IIIIIII\t11\t..........^O.\t&FIIIIIIIIE\t8\t...,....\tII:>IIII");
		ps.println("chr1\t14928\tA\t11\t...........\tIIIIIIIIIII\t9\t,.......^(.\tGAIIIIIIE\t12\t...........^G.\t&CIBIIII9IIE\t8\t...,....\tII;0DIII");
		ps.println("chr1\t14929\tC\t11\t...........\t<HIIIIIIIII\t9\t,........\tA3IIIIIII\t12\t............\t'DI?IIIIIIII\t8\t...,....\tHIB%5III");
		ps.println("chr1\t14930\tA\t11\t..G..G.....\t8I7CI7GIIII\t9\t,.G..G...\t6,7%I7III\t12\t.G.GG.......\t(:I77IIIIIII\t8\tG..,..G.\t1I:%9I7I");
		ps.println("chr1\t14931\tA\t11\t...........\tBI7BI7>IIII\t9\t,........\tB37%I7III\t12\t............\t9FI77IIIIIII\t8\t...,....\t?I;>4I7I");
		ps.println("chr1\t14932\tG\t11\t...........\tI=IIIIIIIII\t9\t,........\t?@IIIIIII\t12\t............\t>IIIIIIIIIII\t8\t...,....\t?ICI@III");
		ps.println("chr1\t14933\tG\t11\t...........\tEAIIIIDIIII\t9\t,........\tD8III?III\t12\t............\t3EIIIIIIIIII\t9\t...,....^L.\t8I9HIIIIE");
		ps.println("chr1\t14934\tT\t11\t...........\t9I>III<IIII\t9\t,........\tIGIIIIIII\t12\t............\t+IIIIIIIIIII\t9\t...,.....\tIIAAGIGII");
		ps.println("chr1\t14935\tG\t12\t...........^!.\tIF>IIIFIIIIE\t9\t,........\tHCIIIIIII\t12\t............\t*IIIIIIIIIII\t9\t...,.....\tIII7IIIII");
		ps.println("chr1\t14936\tC\t12\t............\tI@IIIIIIIIII\t9\t,........\tBIIDIIIII\t12\t............\t8GIIIIIIIIII\t9\t...,.....\tIII,BIIII");
		ps.println("chr1\t14937\tT\t12\t............\tIIIIIIIIIIII\t9\t,........\t8IIIIFIII\t12\t............\t:IIIIIIIIIII\t9\t...,.....\tBII?)IIII");
		ps.println("chr1\t14938\tG\t12\t....$........\t%=I1II6IFIII\t9\t,........\tD%IIB/IHI\t12\t............\t3II>IIIIIIHI\t9\t...,.....\t0IAI/I?II");
		ps.println("chr1\t14939\tG\t11\t...........\t%@IHI:IIIHI\t9\t,........\tI%II@CIDI\t12\t............\t7IICIIIIII9A\t9\t...,.....\t1IAI;I9II");
		ps.println("chr1\t14940\tC\t11\t...........\t:IF?I-IIIII\t9\t,........\tF+II+IIII\t12\t......$......\t2%I%A>I>IIIA\t9\t...,.....\t3?)G:I<IIv");
		ps.println("chr1\t14941\tC\t11\t...........\t7IF?I-III=I\t9\t,........\tD,II+IIGI\t11\t.......$....\t7%E%1I/IIII\t9\t...,.....\tE1)B%ICIA");
		ps.println("chr1\t14942\tC\t11\t...........\tDFIIIIIIG=I\t9\t,........\tC4IIIEIII\t10\t..........\t7IIIIIIIII\t9\t...,.....\tIDII%IIII");
		ps.println("chr1\t14943\tA\t11\t...........\t)9EFI%IICFI\t9\t,........\tI@FII+I&G\t10\t.$.........\t'IIIIIEIII\t9\t.$..,.....\t@I@?=I>II");
		ps.println("chr1\t14944\tG\t11\t.....C.....\t(//AI%IIIFI\t9\t,$........\tI<BIB2I&F\t9\t.........\tE87EI?EI?\t8\t..,.....\t3.I&H:I:"); 
		ps.println("chr1\t14945\tG\t11\t.....C.....\t/>=II%ICIII\t8\t.$.......\t2II@6IBI\t9\t.........\t?:16IIB=,\t8\t..,.....\t9/%&>CI0");
		ps.println("chr1\t14946\tG\t11\t...........\t3I>II%I@I(I\t7\t.......\tIICIIII\t9\t.........\t4ID?II@GD\t8\t..,.....\tI@%;HIII");
		ps.println("chr1\t14947\tC\t11\t...$........\tDI?IIAIDI(I\t7\t.......\tIIIIIII\t9\t.$.....N$..\tEI58II!(B\t8\t..,.....\tI@C?IIII");
		ps.println("chr1\t14948\tG\t10\t.$.$........\t=;-%3I6I<I\t7\t.......\tEIC%IG<\t7\t.$......\t5%),A%,\t8\t..,.$....\t90I0BF;>");
		ps.println("chr1\t14949\tG\t8\t.......$.\t5%6I>I%D\t7\t.......\tBI:%I;B\t6\t......\t*1,:0%\t7\t.$.,....\t'1I59;'");
		ps.println("chr1\t14950\tG\t7\t.$......\t?H3B+B7\t7\t.$......\t:+%%D7@\t6\t......\t%-%50%\t6\t.,....\t-I3'C'");
		ps.println("chr1\t14951\tC\t6\t......\tG2=+95\t6\t......\t)%%A6C\t6\t......\t%9%C89\t6\t.,....\t8H6(=%");

		ps.close();
		os.close();
	}
	
	private void createGFF3File(File pileupFile) throws IOException {
		
		OutputStream os = FileUtils.isFileGZip(pileupFile) ? new GZIPOutputStream( new FileOutputStream(pileupFile)) 
		: new FileOutputStream(pileupFile);
		
//		OutputStream os = new FileOutputStream(pileupFile);
		PrintStream ps = new PrintStream(os);
		
		
		ps.println("##gff-version 3");
		ps.println("# Created by: simple_segmenter.pl[v2940]");
		ps.println("# Created on: Tue May 24 01:48:54 2011");
		ps.println("# Commandline: -v -g -l -i SureSelect_All_Exon_50mb_filtered_exons_1-200_20110524.gff3 -o SureSelect_All_Exon_50mb_filtered_exons_1-200_20110524_shoulders.gff3 -f exon,100,100,100 -f highexon,300 -f lowexon");
		ps.println("chr1	simple_segmenter.pl[v2940]	fill	1	14166	.	.	.	ID=gnl|fill");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_3_100	14167	14266	.	+	.	ID=gnl|exon_3_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_2_100	14267	14366	.	+	.	ID=gnl|exon_2_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	14367	14466	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	14467	14587	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	14588	14638	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	14639	14883	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon	14884	14942	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	14943	15064	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	15065	15164	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_2_100	15165	15264	.	+	.	ID=gnl|exon_2_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_3_100	15265	15364	.	+	.	ID=gnl|exon_3_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	fill	15365	15370	.	.	.	ID=gnl|fill");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_3_100	15371	15470	.	+	.	ID=gnl|exon_3_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_2_100	15471	15570	.	+	.	ID=gnl|exon_2_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	15571	15670	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	15671	15990	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	15991	16090	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_2_100	16091	16190	.	+	.	ID=gnl|exon_2_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_3_100	16191	16390	.	+	.	ID=gnl|exon_3_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_2_100	16391	16490	.	+	.	ID=gnl|exon_2_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	16491	16590	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	16591	16719	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	16720	16749	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	16750	17074	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	17075	17177	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	17178	17420	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	17421	17442	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	17443	18108	.	+	.	ID=ens|ENST00000423562,ens|ENST00000430492,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	18109	18202	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	exon	18203	18448	.	+	.	ID=ens|ENST00000423562,ens|ENST00000430492,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_1_100	18449	18548	.	+	.	ID=gnl|exon_1_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_2_100	18549	18648	.	+	.	ID=gnl|exon_2_100");
		ps.println("chr1	simple_segmenter.pl[v2940]	exon_3_100	18649	18848	.	+	.	ID=gnl|exon_3_100");
//		ps.println("##gff-version 3");
//		ps.println("# Created by: simple_segmenter.pl[v2940]");
//		ps.println("# Created on: Tue May 24 01:48:54 2011");
//		ps.println("# Commandline: -v -g -l -i SureSelect_All_Exon_50mb_filtered_baits_1-200_20110524.gff3 -o SureSelect_All_Exon_50mb_filtered_baits_1-200_20110524_shoulders.gff3 -f bait,100,100,100 -f highbait,300 -f lowbait");
//		ps.println("chr1	simple_segmenter.pl[v2940]	fill	1	14166	.	.	.	ID=gnl|fill");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_3_100	14167	14266	.	+	.	ID=gnl|bait_3_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_2_100	14267	14366	.	+	.	ID=gnl|bait_2_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	14367	14466	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	14467	14587	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	14588	14638	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	14639	14883	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	14884	14942	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	14943	15064	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	15065	15164	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_2_100	15165	15264	.	+	.	ID=gnl|bait_2_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_3_100	15265	15364	.	+	.	ID=gnl|bait_3_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	fill	15365	15370	.	.	.	ID=gnl|fill");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_3_100	15371	15470	.	+	.	ID=gnl|bait_3_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_2_100	15471	15570	.	+	.	ID=gnl|bait_2_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	15571	15670	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	15671	15990	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	15991	16090	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_2_100	16091	16190	.	+	.	ID=gnl|bait_2_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_3_100	16191	16390	.	+	.	ID=gnl|bait_3_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_2_100	16391	16490	.	+	.	ID=gnl|bait_2_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	16491	16590	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	16591	16719	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	16720	16749	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	16750	17074	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	17075	17177	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	17178	17420	.	+	.	ID=ens|ENST00000423562,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	17421	17442	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	17443	18108	.	+	.	ID=ens|ENST00000423562,ens|ENST00000430492,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	18109	18202	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	SureSelect_All_Exon_50mb_with_annotation.hg19.bed	bait	18203	18448	.	+	.	ID=ens|ENST00000423562,ens|ENST00000430492,ens|ENST00000438504,ens|ENST00000488147,ref|NR_024540,ref|WASH7P");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_1_100	18449	18548	.	+	.	ID=gnl|bait_1_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_2_100	18549	18648	.	+	.	ID=gnl|bait_2_100");
//		ps.println("chr1	simple_segmenter.pl[v2940]	bait_3_100	18649	18848	.	+	.	ID=gnl|bait_3_100");
		
		ps.close();
		os.close();
	}
}
