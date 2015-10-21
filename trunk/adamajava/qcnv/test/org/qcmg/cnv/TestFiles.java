package org.qcmg.cnv;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.*;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMRecord;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;


public class TestFiles {
	public static final String INPUT_NORMAL_SAM = "input.normal.sam";	
	public static final String INPUT_TUMOR_SAM = "input.tumor.sam";
	
	public static void CreateBAM(String INPUT_NORMAL_BAM, String INPUT_TUMOR_BAM){
		List<String> normaldata = new ArrayList<String>();
		List<String> tumordata = new ArrayList<String>();
		
		//common
		normaldata.add("@HD	VN:1.0");  
		normaldata.add("@SQ	SN:GL000195.1	LN:182896");	
		normaldata.add("@SQ	SN:GL000196.1	LN:38914");
		normaldata.add("@RG	ID:2010072264129530	LB:Library_20100413_C	DS:RUNTYPE{50F}	SM:S0414_20100607_2_FragBC_bcSample1_F3_bcA10_05");
		normaldata.add("@PG	ID:2010072264129500	PN:MANUAL");
		normaldata.add("1035_217_1202	272	GL000196.1	319	3	50M	*	0	0	GACATATACACAACACTGTACCCAACTATACGATACATATTCTTCTCAAG	!IIIIIFIIIGIIII:?IIF4CIII;7CI''''IIIIIII**IIGIIIIA	RG:Z:2010072264129530	CS:Z:T22012220200333113323133321010013112111011113333112	AS:i:34	CQ:Z:/3:8@62B-*46?-A+B;'A'<9+-/@@6.'@B4,/;@2=+@B)>/?B@A	XN:i:34	NH:i:2	IH:i:2	HI:i:2	MD:Z:26T3CG18");
		normaldata.add("1340_940_1222	1296	GL000196.1	437	3	50M	*	0	0	ACATACAATGGAATATTATTCAGCCTTTAAAAAGAAGGAAATTCTAGCCG	!IIII7IIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	RG:Z:2010072264129530	CS:Z:T23032322030020202200003002032120330333020130113311	AS:i:49	CQ:Z:BBBBBBBBBBABBBBBBBBAAB@BB6A@B8@B>?B@>A?;=6A@/)BB<;	XN:i:49	NH:i:10	IH:i:2	HI:i:2	MD:Z:50");
 		normaldata.add("1599_1110_902	16	GL000196.1	437	3	50M	*	0	0	ACATACAATGGAATATTATTCAGCCTTTAAAAAGAAGGAAATTCTAGCCG	!IIIICIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII	RG:Z:2010072264129530	CS:Z:T23032322030020202200003002032120330333020130113311	AS:i:49	CQ:Z:BBBB@ABBBBBBBBBBBBBA@BABB7BB?@@BB@@BB@??A<BB7-@@>/	XN:i:49	NH:i:10	IH:i:2	HI:i:2	MD:Z:50");
 		normaldata.add("603_1107_1232	0	GL000196.1	480	1	25M25H	*	0	0	AATCACTTGAACCCAGGAGGCGGAG	IIIIIIIIIIIIIIIIIIIIIIII:	RG:Z:2010072264129530	CS:Z:T30321120120100120220330223100133302310303131133123	AS:i:24	CQ:Z:BBBB@AAA>><>B@;9AA<:BB=@>:AB<<=@9@7'9<22>?921<:/'1	XN:i:24	NH:i:10	IH:i:2	HI:i:1	CC:Z:GL000247.1	CP:i:35405	MD:Z:25");
 		normaldata.add("828_1019_1921	0	GL000196.1	38525	3	37M13H	*	0	0	AGGCTGAGGTGGGCGGATCACTTGAGGTCCAGAGTTC	IIIIIIIIIIIIIIIII;?IIIB@IIIBAIIIIIIII	RG:Z:2010072264129530	CS:Z:T32032122011003302321120122012012221023222003301200	AS:i:30	CQ:Z:<?=?8?9<<=@?<@@?@5'96<:)8;?9*8@?:7B?@<:A@;<+?0-8:8	XN:i:28	NH:i:10	IH:i:2	HI:i:2	MD:Z:29C7");
		
		//tumor
		tumordata.add("2126_1421_1351	1280	GL000196.1	442	0	8H37M5H	*	0	0	CAATGGAATATTATTCAGCCTTTAAAAAGAAGGAAAT	@IIIIIIIIIIIIIIIIIIIIIIH?II((IIII@III	RG:Z:2010072264129530	CS:Z:T03331121210310203330330212302003000032020200320211	AS:i:33	CQ:Z:BBABBB@@BBBA;AB?>5BBA@<:@;A?@A@>+5@9(7>@B4->><6,9<	XN:i:33	NH:i:10	IH:i:2	HI:i:2	MD:Z:37");
		tumordata.add("274_555_1818	256	GL000196.1	442	0	8H37M5H	*	0	0	CAATGGAATATTATTCAGCCTTTAAAAAGAAGGAAAT	?IIIIIIIIIIIIIIIIIIIIIIIIIIC;?IIIIIII	RG:Z:2010072264129530	CS:Z:T03331121210310203330330212302003000022020200320211	AS:i:36	CQ:Z:BBBBABA?BBBBA<BB@B@BBB=@B7B=<@A@<>B;)3-?B:@=B5@1?>	XN:i:36	NH:i:10	IH:i:2	HI:i:2	MD:Z:37");
		tumordata.add("1000_1348_882	1280	GL000196.1	448	0	14H34M2H	*	0	0	AATATTATTCAGCCTTTAAAAAGAAGGAAATTCT	7IIIIIIIIIIIIII:(BIII;?IIIAEIIIIII	RG:Z:2010072264129530	CS:Z:T21230221110321003330330212302033000022020200302213	AS:i:30	CQ:Z:@A@BB@B=?ABB@7>B>?B<AB<>:5BB=:@(;<;7%;A@;'?@>=3???	XN:i:30	NH:i:10	IH:i:2	HI:i:2	MD:Z:34");
		tumordata.add("1196_1956_396	1280	GL000196.1	448	0	14H34M2H	*	0	0	AATATTATTCAGCCTTTAAAAAGAAGGAAATTCT	:IIIIIIIIIIIIII:*BIIC''III7AIIIIII	RG:Z:2010072264129530	CS:Z:T21230221110321003330330212302033000012020200302213	AS:i:27	CQ:Z:?8A@A?A?<@BBA<:@=?A:AA<=99B@?:>*9;;)';A92&<A></A@?	XN:i:27	NH:i:10	IH:i:2	HI:i:2	MD:Z:34");

 
		try {

			BufferedWriter outNormal = new BufferedWriter(new FileWriter(INPUT_NORMAL_SAM));
			BufferedWriter outTumor = new BufferedWriter(new FileWriter(INPUT_TUMOR_SAM));
			
			//create normal BAM
			for (String line : normaldata){ 
				outNormal.write(line + "\n");	
				outTumor.write(line + "\n");	
			}		 
			outNormal.close();
			
			//append extra to turmor BAM									
			for (String line : tumordata)  				
				outTumor.write(line + "\n");
			outTumor.close();
			
			//don't forgot index file for picard query
			Txt2BAM(new File(INPUT_NORMAL_SAM), new File(INPUT_NORMAL_BAM));
			Txt2BAM(new File(INPUT_TUMOR_SAM), new File(INPUT_TUMOR_BAM));
			new File(INPUT_NORMAL_SAM).delete();
			new File(INPUT_TUMOR_SAM).delete();
			
		} catch (IOException e) {
			System.err.println(e.toString() + "\n\t can't write to : " + INPUT_NORMAL_SAM + " or " + INPUT_TUMOR_SAM);
		}
		
	}
	
		
	private static void Txt2BAM(File SAM, File BAM){
		SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(SAM) ;
		SAMFileHeader header = reader.getFileHeader().clone();
		header.setSortOrder(SAMFileHeader.SortOrder.coordinate);
		
		SAMOrBAMWriterFactory factory = new SAMOrBAMWriterFactory(header, false, BAM, true);
		SAMFileWriter writer = factory.getWriter();
		for( SAMRecord record : reader)
			writer.addAlignment(record);	
			
		reader.close();
		factory.closeWriter();		
	}
}
