package au.edu.qimr.qmito;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMWriterFactory;

public class TestFile {
	
	  public static void createFile(List<String> data, String fileName) throws IOException{
	      
	       try(BufferedWriter out = new BufferedWriter(new FileWriter(fileName));) {	         
	          for (String line : data) {
	        	  out.write(line + "\n");
	          }
	       }  
	 }
	  
	  public static void createRef(String output) throws IOException{
	 
		  List<String> data = new ArrayList<>();
		  data.add("\\>chrMT");	
		  
		  //get last 806 char from chrMT reference file
		  data.add("AGCTACCCTTTTACCATCATTGGACAAGTAGCATCCGTACTATAC"
				  + "TTCACAACAATCCTAATCCTAATACCAACTATCTCCCTAATTGAAAACAAAATACTCAAATGGGCCTGTC"
				  + "CTTGTAGTATAAACTAATACACCAGTCTTGTAAACCGGAGATGAAAACCTTTTTCCAAGGACAAATCAGA"
				  + "GAAAAAGTCTTTAACTCCACCATTAGCACCCAAAGCTAAGATTCTAATTTAAACTATTCTCTGTTCTTTC"
				  + "ATGGGGAAGCAGATTTGGGTACCACCCAAGTATTGACTCACCCATCAACAACCGCTATGTATTTCGTACA"
				  + "TTACTGCCAGCCACCATGAATATTGTACGGTACCATAAATACTTGACCACCTGTAGTACATAAAAACCCA"
				  + "ATCCACATCAAAACCCCCTCCCCATGCTTACAAGCAAGTACAGCAATCAACCCTCAACTATCACACATCA"
				  + "ACTGCAACTCCAAAGCCACCCCTCACCCACTAGGATACCAACAAACCTACCCACCCTTAACAGTACATAG"
				  + "TACATAAAGCCATTTACCGTACATAGCACATTACAGTCAAATCCCTTCTCGTCCCCATGGATGACCCCCC"
				  + "TCAGATAGGGGTCCCTTGACCACCATCCTCCGTGAAATCAATATCCCGCACAAGAGTGCTACTCTCCTCG"
				  + "CTCCGGGCCCATAACACTTGGGGGTAGCTAAAGTGAACTGTATCCGACATCTGGTTCCTACTTCAGGGTC"
				  + "ATAAAGCCTAAATAGCCCACACGTTCCCCTTAAATAAGACATCACGATG");	  
		  createFile(data, output);
		  
		  //index file
		  data = new ArrayList<>();
		  data.add("chrMT\t813\t7\t70\t71");		  
		  createFile(data, output + ".fai");	  
	  }
	 

		public static void createBam(String output) throws IOException{
			String sam = output+ ".sam";
			createFile(TestFile.createSam(), sam);
			
			try(SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(sam), null,  ValidationStringency.SILENT); ) { 
				SAMWriterFactory factory = new SAMWriterFactory(reader.getFileHeader(), true, new File(output),true );
				try( SAMFileWriter writer = factory.getWriter(); ){				
					for( SAMRecord record : reader) writer.addAlignment(record);				
				}
				factory.renameIndex(); //try already closed writer
			} 			
	    	new File(sam).delete();
			
		}
		
		/**
		* There are three reversed mapped and primary reads, two of them mapped on edge. These testing data are modified from real SAM records.
	    * original reads was
	    * 
	    * data.add("HWI-ST1240:137:H08VDADXX:1:1206:5690:25092	177	chrMT	16569	37	101M	=	39	-16530	CCATCGTGATGTCTTATTTAAGGGTAACGTGTGGGCTATTTAGGCTTTATGGCCCTGAAGTAGGAACCAGATGTCGGATACAGTTCACTTTAGCTACCCCC	BBBBB<0<BBBBB<BBBBBBBBBBBBBBBBFBBFFFBFFFFBB0<BFFFBBBFB7FBFBFBFFB0BF<<0IFBFFBBBFFFF7IFFBFBB<FBB07FFBBB	X0:i:1	X1:i:0	ZC:i:10	MD:Z:0G0A1A0A0A0A0A1A0A0A0A0A0A1A0A0A2A0A0A0A2A0A0A0A0A0A0A0A0A0A1A0A0A1A0A0A0A0A0A1A0A0A0A0A0A0A0A2A0A1A0A2A0A1A1A0A0A0A0A0A1A1A1A0A0A0A1A0A0A0A1A0A0A1A0A0A0A0A0	PG:Z:MarkDuplicates	RG:Z:20130123010211418	XG:i:0	AM:i:37	NM:i:78	SM:i:37	XM:i:3	XO:i:0	XT:A:U");
	    * data.add("HWI-ST1240:137:H08VDADXX:1:2105:6130:72695	177	chrMT	16568	37	101M	=	138	-16430	TCCATCGTGATGTCTTATTTAAGGGGAACGTGTGGGCTATTTAGGCTTTATGGCCCTGAAGTAGGAACCAGATGTCGGATACAGTTCACTTTAGCTACCCC	#####BBBBB<BBBBBBBB<<0<0<70'FFFFFFF<BBFFBBFFFFIIIFIFFBBBFFFFB7FFFBFFB7FF<BFIFBFFI<BFB<F<BBBFBB<BFF<BB	X0:i:1	X1:i:0	ZC:i:10	MD:Z:1G0A1A0A0A0A0A1A0A0A0A0A0A1A0A0A2A0A0A0A2A0A0A0A0A0A0A0A0A0A1A0A0A1A0A0A0A0A0A1A0A0A0A0A0A0A0A2A0A1A0A2A0A1A1A0A0A0A0A0A1A1A1A0A0A0A1A0A0A0A1A0A0A1A0A0A0A0	PG:Z:MarkDuplicates	RG:Z:20130123010211418	XG:i:0	AM:i:37	NM:i:77	SM:i:37	XM:i:2	XO:i:0	XT:A:U");	        
	    * data.add("HWI-ST1240:115:H03J8ADXX:2:1216:5509:54570	83	chrMT	16479	29	91M10S	=	16251	-319	TAAAGTGAACTGTATCCGACATCTGGTTCCTACTTCAGGGCCATAAAGCCTAAATAGCCCACACGTTCCCCTTAAATAAGACATCACGATGGATCACAGGT	4A:CCDC@>CCCBBBB?>5@C=CCEEEEEHHEECED@@GGDIGHIEGIGFACGHGGIIIHFDDFEGIIIIHFAEHHHHEC<FG>E:AGAHHFHDDDDD@@;	ZC:i:8	MD:Z:40T50	PG:Z:MarkDuplicates	RG:Z:20130123010211418	XG:i:0	AM:i:29	NM:i:1	SM:i:29	XM:i:1	XO:i:0	XT:A:M");	        
	    * 
	    * we shift 10,000 position to match the truncated chrMT reference file
	    * 
	    * @return a list of text SAMRecord 
	    */
	   public static List<String> createSam() {
	        List<String> data = new ArrayList<>();
	        data.add("@HD	VN:1.4	GO:none	SO:coordinate");	
	        data.add("@SQ	SN:chrMT	LN:813");	
	        data.add("@RG	ID:20130123010211418	PL:IONTORRENT	PU:PGM/316D/IonXpress_001	LB:ICGC_HG19/IonXpress_001	FO:TACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACGTCTGAGCATCGATCGATGTACAGCTACGTACG	zc:5:/mnt/seq_results/smgres_special/COLO_829/seq_mapped/T00002_20120206_140.nopd.IonXpress_001.bam	DS:Manual ePCR	DT:2012-12-30T02:33:23+1000	PG:tmap	SM:COLO_pool_29gene_Manual	KS:TCAGCTAAGGTAACGAT	CN:TorrentServer/T02");
	        data.add("@PG	ID:0bea213c-c8ff-489d-b4f4-5ce5b0ddb5f3	PN:qbammerge	zc:8	VN:0.6pre (6608)	CL:qbammerge --output /scratch/432375.qcmg-clustermk2.imb.uq.edu.au/SmgresSpecial_COLO829_1DNA_9CellLineDerivedFromTumour_ICGCMSSM2011051602CD_IonTorrentMultiplexedLibraryBuilder_29GeneCancerPanelTargetSEQ_Tmap_IonPGM.jpearson.Library_20120201_O_Library_20120202_A.bam --log /scratch/432375.qcmg-clustermk2.imb.uq.edu.au/SmgresSpecial_COLO829_1DNA_9CellLineDerivedFromTumour_ICGCMSSM2011051602CD_IonTorrentMultiplexedLibraryBuilder_29GeneCancerPanelTargetSEQ_Tmap_IonPGM.jpearson.Library_20120201_O_Library_20120202_A.bam.qmrg.log --input /mnt/seq_results/smgres_special/COLO_829/seq_mapped/T00002_20120206_140.nopd.IonXpress_001.bam --input /mnt/seq_results/smgres_special/COLO_829/seq_mapped/T00002_20120128_135.nopd.IonXpress_001.bam --input /mnt/seq_results/smgres_special/COLO_829/seq_mapped/T00002_20120308_149.nopd.IonXpress_001.bam");	        	        
	        data.add("HWI-ST1240:115:H03J8ADXX:2:1216:5509:54570	83	chrMT	479	29	91M10S	=	16251	-319	TAAAGTGAACTGTATCCGACATCTGGTTCCTACTTCAGGGCCATAAAGCCTAAATAGCCCACACGTTCCCCTTAAATAAGACATCACGATGGATCACAGGT	4A:CCDC@>CCCBBBB?>5@C=CCEEEEEHHEECED@@GGDIGHIEGIGFACGHGGIIIHFDDFEGIIIIHFAEHHHHEC<FG>E:AGAHHFHDDDDD@@;	ZC:i:8	MD:Z:40T50	PG:Z:MarkDuplicates	RG:Z:20130123010211418	XG:i:0	AM:i:29	NM:i:1	SM:i:29	XM:i:1	XO:i:0	XT:A:M");
	        data.add("HWI-ST1240:137:H08VDADXX:1:2105:6130:72695	177	chrMT	568	37	101M	=	138	-16430	TCCATCGTGATGTCTTATTTAAGGGGAACGTGTGGGCTATTTAGGCTTTATGGCCCTGAAGTAGGAACCAGATGTCGGATACAGTTCACTTTAGCTACCCC	#####BBBBB<BBBBBBBB<<0<0<70'FFFFFFF<BBFFBBFFFFIIIFIFFBBBFFFFB7FFFBFFB7FF<BFIFBFFI<BFB<F<BBBFBB<BFF<BB	X0:i:1	X1:i:0	ZC:i:10	MD:Z:1G0A1A0A0A0A0A1A0A0A0A0A0A1A0A0A2A0A0A0A2A0A0A0A0A0A0A0A0A0A1A0A0A1A0A0A0A0A0A1A0A0A0A0A0A0A0A2A0A1A0A2A0A1A1A0A0A0A0A0A1A1A1A0A0A0A1A0A0A0A1A0A0A1A0A0A0A0	PG:Z:MarkDuplicates	RG:Z:20130123010211418	XG:i:0	AM:i:37	NM:i:77	SM:i:37	XM:i:2	XO:i:0	XT:A:U");
	        data.add("HWI-ST1240:137:H08VDADXX:1:1206:5690:25092	177	chrMT	569	37	101M	=	39	-16530	CCATCGTGATGTCTTATTTAAGGGTAACGTGTGGGCTATTTAGGCTTTATGGCCCTGAAGTAGGAACCAGATGTCGGATACAGTTCACTTTAGCTACCCCC	BBBBB<0<BBBBB<BBBBBBBBBBBBBBBBFBBFFFBFFFFBB0<BFFFBBBFB7FBFBFBFFB0BF<<0IFBFFBBBFFFF7IFFBFBB<FBB07FFBBB	X0:i:1	X1:i:0	ZC:i:10	MD:Z:0G0A1A0A0A0A0A1A0A0A0A0A0A1A0A0A2A0A0A0A2A0A0A0A0A0A0A0A0A0A1A0A0A1A0A0A0A0A0A1A0A0A0A0A0A0A0A2A0A1A0A2A0A1A1A0A0A0A0A0A1A1A1A0A0A0A1A0A0A0A1A0A0A1A0A0A0A0A0	PG:Z:MarkDuplicates	RG:Z:20130123010211418	XG:i:0	AM:i:37	NM:i:78	SM:i:37	XM:i:3	XO:i:0	XT:A:U");

		  return data;
	    }
 
}
