package org.qcmg.qmule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;


public class AlignerCompareTest {
	public static final String INPUT_SAM1 = "./input1.sam";	
	public static final String INPUT_SAM2 = "./input2.sam";	
	public static final String OUTPUT_BAM = "./output.bam";	
	
	@After
	public void deleteFiles(){ 
		File in1 = new File(INPUT_SAM1);
		File in2 = new File(INPUT_SAM2);
		File out = new File(OUTPUT_BAM);
				
		in1.delete();
		in2.delete();
		out.delete();
		
	 
	}
	
	@Before
	public void before(){		 
		CreateSAMs( );	
 

	}
	
	
 
	@Test
	public void mainTest() throws Exception{
  
  		final String[] args1 = { INPUT_SAM1, INPUT_SAM2, OUTPUT_BAM };
		AlignerCompare.main(args1);
		
	}
	
	
	public static void CreateSAMs(){
		List<String> mydata = new ArrayList<String>();
		
		//common
		mydata.add("@HD	VN:1.4	SO:queryname");  
		mydata.add("@SQ	SN:GL000196.1 LN:38914");
		
		mydata.add("@RG	ID:2010072264129530	LB:Library_20100413_C	DS:RUNTYPE{50F}	SM:S0414_20100607_2_FragBC_bcSample1_F3_bcA10_05");
		mydata.add("@PG	ID:2010072264129500	PN:MANUAL");
		mydata.add("603_1107_1232	0	GL000196.1	480	1	25M25H	*	0	0	AATCACTTGAACCCAGGAGGCGGAG	IIIIIIIIIIIIIIIIIIIIIIII:	RG:Z:2010072264129530	CS:Z:T30321120120100120220330223100133302310303131133123	AS:i:24	CQ:Z:BBBB@AAA>><>B@;9AA<:BB=@>:AB<<=@9@7'9<22>?921<:/'1	XN:i:24	NH:i:10	IH:i:2	HI:i:1	CC:Z:GL000247.1	CP:i:35405	MD:Z:25");
 		mydata.add("603_1107_1233	163	GL000196.1	36008	29	75M	=	36083	142	GGATCTAGAATGCTGAAGGATCTAGTGTGTTGAGGGATCTAGCATGCTGAAGGATCTAGCATGTTAAGGGATCTA	BBBFFFFFFFFFFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFFFFI	X0:i:1	X1:i:0	ZC:i:5	MD:Z:8G66	PG:Z:MarkDuplicates	RG:Z:2010072264129530	XG:i:0	AM:i:29	NM:i:1	SM:i:29	XM:i:1	XO:i:0	XT:A:U");
 		mydata.add("603_1107_1233	87	GL000196.1	36083	29	4S67M4S	=	36008	-142	TCTAGCATGTCGAGAGATCTAGCATGCTGAAGGATCTAGCATGCTGAAGGATCTAGCATGTTGAGGGTTCTAGTG	FFIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIFFFFFFFFFFBBB	ZC:i:5	MD:Z:63A3	PG:Z:MarkDuplicates	RG:Z:2010072264129530	XG:i:0	AM:i:29	NM:i:1	SM:i:29	XM:i:1	XO:i:0	XT:A:M");
 		mydata.add("603_1108_0001	0	GL000196.1	38525	3	37M5D13H	*	0	0	AGGCTGAGGTGGGCGGATCACTTGAGGTCCAGAGTTC	IIIIIIIIIIIIIIIII;?IIIB@IIIBAIIIIIIII	RG:Z:2010072264129530	CS:Z:T32032122011003302321120122012012221023222003301200	AS:i:30	CQ:Z:<?=?8?9<<=@?<@@?@5'96<:)8;?9*8@?:7B?@<:A@;<+?0-8:8	XN:i:28	NH:i:10	IH:i:2	HI:i:2	MD:Z:29C7");

 		List<String> mydata1 = new ArrayList<String>();
 		mydata1.add("603_1108_0002	73	GL000196.1	319	3	50M	=	319	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA" + 	"	X0:i:1	X1:i:0	MD:Z:100	PG:Z:MarkDuplicates	RG:Z:2010072264129530	XG:i:0	AM:i:0	NM:i:0	SM:i:37	XM:i:0	XO:i:0	XT:A:U");
 		mydata1.add("603_1108_0002	133	GL000196.1	319	0	*	=	319	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530	CS:Z:T22012220200333113323133321010013112111011113333112	AS:i:34	CQ:Z:/3:8@62B-*46?-A+B;'A'<9+-/@@6.'@B4,/;@2=+@B)>/?B@A	XN:i:34	HI:i:2");

 		List<String> mydata2 = new ArrayList<String>();
 		mydata2.add("603_1108_0002	73	GL000196.1	319	3	50M	=	319	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530	CS:Z:T22012220200333113323133321010013112111011113333112	AS:i:34	CQ:Z:/3:8@62B-*46?-A+B;'A'<9+-/@@6.'@B4,/;@2=+@B)>/?B@A	XN:i:34	NH:i:2	IH:i:2	HI:i:2	MD:Z:26T3CG18");
		mydata2.add("603_1108_0002	133	GL000196.1	319	0	*	=	319	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530");
		
 		//add invalide record since mapq is not zero for unmapped reads
		
		try {

			BufferedWriter writer1 = new BufferedWriter(new FileWriter(INPUT_SAM1));
			BufferedWriter writer2 = new BufferedWriter(new FileWriter(INPUT_SAM2));
			
			//create SAM 
			for (String line : mydata){ 
				writer1.write(line + "\n");	
				writer2.write(line + "\n");
			}
			
			for (String line : mydata1) 
				writer1.write(line + "\n");	
			
			for (String line : mydata2) 
				writer2.write(line + "\n");	

			
			writer1.close();
			writer2.close();
//debug
//			System.out.println(new File(INPUT_SAM).getAbsolutePath() );
 			
		} catch (IOException e) {
			System.err.println(e.toString() + "\n\t can't write to : " + INPUT_SAM1 + " or " + INPUT_SAM2   );
		}
		
	}
	
	 
}
