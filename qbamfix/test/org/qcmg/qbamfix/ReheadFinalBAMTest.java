package org.qcmg.qbamfix;


import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;


import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.qcmg.picard.SAMFileReaderFactory;

public class ReheadFinalBAMTest {
	public static final String Test_FILE_NAME = "test.bam";
	
    @Before
    public void before() throws Exception{ createBAM(); }
    
     @After
     public void after(){ new File(Test_FILE_NAME).delete(); }
    
    
	@Test
	public void noBadMateTest() throws Exception{
		String str ="@CO	CN:QCMG	QN:qlimsmeta	Aligner=bwa	Capture Kit=Human Rapid Exome (Nextera)	Failed QC=0	Library Protocol=Illumina Nextera Rapid Capture Exome Manual	Material=1:DNA	Project=project	Donor=AAAA_1111	Reference Genome File=/path/reference.fa	Sample=Sample-20121220-047	Sample Code=7:Primary tumour	Sequencing Platform=HiSeq	Species Reference Genome=Homo sapiens  ";
		String[] array = str.split("Donor=");
		array = array[1].split("\\s+");
		System.out.println(array[0]);
		
		str = "ok,";
		str = str.substring(0, str.length()-1);
		System.out.println(str +" length is " + str.length());
		

	}
	
	@Test
	public void mainTest() throws Exception{
		ReheadFinalBAM rehead = new ReheadFinalBAM();
		
		SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(new File(Test_FILE_NAME)).getFileHeader().clone();
		
		//get info from header
		Assert.assertEquals("SSSS_001",  rehead.getDonor(header.getComments()));
		Assert.assertEquals("SSSS_093,SSSS_093,SSSS_090", rehead.matchDonor("SSSS_001", header.getReadGroups()));
		 
		//correct the header to a new BAM
		String Corrected_BAM = "./Test.tmp.bam";
	    SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(Test_FILE_NAME));  	    	
	    rehead.refinalBAM(reader, "SSSS_001", new File(Corrected_BAM));
		reader.close();	
		
		//test on the corrected BAM
		header = SAMFileReaderFactory.createSAMFileReader(new File( Corrected_BAM)).getFileHeader().clone();
		Assert.assertEquals("SSSS_001",  rehead.getDonor(header.getComments()));
		Assert.assertEquals("", rehead.matchDonor("SSSS_001", header.getReadGroups()));
		
		//delete testing file
		new File(Corrected_BAM).delete();		 
	    
	}
	
	private void createBAM() throws Exception{

		String str = "@HD	VN:1.0	GO:none	SO:coordinate\n" +
						"@SQ	SN:chr1	LN:249250621\n" +
						"@SQ	SN:chrMT		LN:16569\n" +
						"@RG	ID:20130228061435683	PL:ILLUMINA	PU:lane_2.nobc	LB:LP6005273-DNA_D04	zc:6:/path/130102_SN1248_0149_BD1PJ6ACXX.lane_2.nobc.bam	SM:SSSS_093	PG:qbamfix	CN:QCMG\n" +
						"@RG	ID:20130228060423541	PL:ILLUMINA	PU:lane_1.nobc	LB:LP6005273-DNA_D04	zc:7:/path/130102_SN1248_0149_BD1PJ6ACXX.lane_1.nobc.bam	SM:SSSS_093	PG:qbamfix	CN:QCMG\n" +
						"@RG	ID:20130228054048683	PL:ILLUMINA	PU:lane_4.nobc	LB:LP6005273-DNA_D04	zc:9:/path/130102_SN1248_0149_BD1PJ6ACXX.lane_4.nobc.bam	SM:SSSS_090	PG:qbamfix	CN:QCMG\n" +
						"@PG	ID:b8d85b29-caaf-49fb-bed7-ce8424934a3b	PN:qbamfix	zc:7	VN:qbamfix,version0.2pre	CL:qbamfix --input --output  --log fix.log --RGLB LP6005273-DNA_D04 --RGSM SSSS_093 --tmpdir /scratch\n" +
						"@PG	ID:bwa	PN:bwazc:6	VN:0.6.1-r104\n" +
						"@PG	ID:bwa.1		PN:bwa	zc:7	VN:0.6.1-r104\n" +
						"@PG	ID:bwa.3		PN:bwa	zc:9	VN:0.6.1-r104\n" +
						"@PG	ID:fe12251e-2a94-4ced-9f7f-c293cd7c10f1	PN:qbammerge	zc:11	VN:0.6pre(6203)\n" +
						"@CO	CN:QCMG	QN:qlimsmeta	Aligner=bwa	Capture Kit=NoCapture	Donor=SSSS_001	FailedQC=0	Library Protocol=Illumina IGN Outsourcing	Material=1:DNA	Project=project2	Reference Genome File=/path/reference.fa Sample=SAMPLE-20130205-002	SampleCode=7:Primarytumour	SequencingPlatform=HiSeq	SpeciesReferenceGenome=Homosapiens \n"+
						"HS2000-1248_149:2:1105:6448:82712	117	chr1	9992	0	*	=	9992	0	TCTTCCGATCTGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTT	?58038CCA?BA<<959A@B<==BA;=3A?;?7)D?=(=;@AD@;>@=8>FCFB9D?>FDFB@D:??)?C?EDGIGGIIGHFIHBHDF>HHHDFFFFCC@	ZC:i:6RG:Z:20130228061435683\n"+
						"HS2000-1248_149:2:1105:6448:82712	153	chr1	9992	23	3M1D5M2I90M	=	9992	0	TCTTTCGATCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCC	######A>CA<2(255(,(5???=;BDB?666)?B6.EEEC=;HGC=8@8BB89FHFDDD?DF?8FFGFD?11B>GGA23D@FF?GFDFFFHFDDBD@@@	X0:i:1	X1:i:1	XA:Z:chr12,+95643,91M2I7M,5;	ZC:i:6MD:Z:3^G1C93	RG:Z:20130228061435683	XG:i:1	AM:i:0	NM:i:4SM:i:23	XM:i:4	XN:i:9	XO:i:1	XT:A:U\n";
		
		BufferedWriter out = new BufferedWriter(new FileWriter(Test_FILE_NAME));
		out.write(str );				
		out.close();				
	}
}
