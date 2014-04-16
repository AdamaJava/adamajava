package org.qcmg.qmule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class BamMismatchCountsTest {
	public static final String INPUT_SAM = "./input.sam";	
	public static final String OUTPUT_FILE_NAME = "./result.counts";	
 
	@After
	public void deleteFiles(){ 
 		new File(INPUT_SAM).delete();	
 		
		//delete output
		File[] files = new File("./").listFiles();
		for(File f :  files)
			if(f.toString().startsWith(OUTPUT_FILE_NAME))
				f.delete();
	}
	
	@Before
	public void before(){		 
		CreateBAM(INPUT_SAM, OUTPUT_FILE_NAME);		

	}
	
	@Test
	public void mainTest() throws Exception{
		final String[] args = {"-i" , INPUT_SAM , "-o" , OUTPUT_FILE_NAME };
		
//		System.out.println(args.toString());
		BamMismatchCounts.main(args);
		
	}
	
	
	public static void CreateBAM(String INPUT_NORMAL_BAM, String INPUT_TUMOR_BAM){
		List<String> normaldata = new ArrayList<String>();
		
		//common
		normaldata.add("@HD	VN:1.0");  
		normaldata.add("@SQ	SN:GL000196.1	LN:38914");
		normaldata.add("@RG	ID:2010072264129530	LB:Library_20100413_C	DS:RUNTYPE{50F}	SM:S0414_20100607_2_FragBC_bcSample1_F3_bcA10_05");
		normaldata.add("@PG	ID:2010072264129500	PN:MANUAL");
		normaldata.add("1035_217_1202	272	GL000196.1	319	3	50M	*	0	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530	CS:Z:T22012220200333113323133321010013112111011113333112	AS:i:34	CQ:Z:/3:8@62B-*46?-A+B;'A'<9+-/@@6.'@B4,/;@2=+@B)>/?B@A	XN:i:34	NH:i:2	IH:i:2	HI:i:2	MD:Z:26T3CG18");
 		normaldata.add("603_1107_1232	0	GL000196.1	480	1	25M25H	*	0	0	AATCACTTGAACCCAGGAGGCGGAG	IIIIIIIIIIIIIIIIIIIIIIII:	RG:Z:2010072264129530	CS:Z:T30321120120100120220330223100133302310303131133123	AS:i:24	CQ:Z:BBBB@AAA>><>B@;9AA<:BB=@>:AB<<=@9@7'9<22>?921<:/'1	XN:i:24	NH:i:10	IH:i:2	HI:i:1	CC:Z:GL000247.1	CP:i:35405	MD:Z:25");
 		normaldata.add("828_1019_1921	0	GL000196.1	38525	3	37M5D13H	*	0	0	AGGCTGAGGTGGGCGGATCACTTGAGGTCCAGAGTTC	IIIIIIIIIIIIIIIII;?IIIB@IIIBAIIIIIIII	RG:Z:2010072264129530	CS:Z:T32032122011003302321120122012012221023222003301200	AS:i:30	CQ:Z:<?=?8?9<<=@?<@@?@5'96<:)8;?9*8@?:7B?@<:A@;<+?0-8:8	XN:i:28	NH:i:10	IH:i:2	HI:i:2	MD:Z:29C7");
 
		try {

			BufferedWriter outNormal = new BufferedWriter(new FileWriter(INPUT_SAM));
			
			//create SAM
			for (String line : normaldata) 
				outNormal.write(line + "\n");			 
			outNormal.close();

 			
		} catch (IOException e) {
			System.err.println(e.toString() + "\n\t can't write to : " + INPUT_SAM   );
		}
		
	}
	
	 
}
