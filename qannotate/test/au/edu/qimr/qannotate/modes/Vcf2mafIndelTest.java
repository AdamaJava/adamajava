package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Test;
import org.qcmg.common.commandline.Executor;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.utils.MafElement;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;


public class Vcf2mafIndelTest {
 	static String inputName = DbsnpModeTest.inputName;	
	static String outputDir = new File(inputName).getAbsoluteFile().getParent() + "/output";
	static String outputMafName = "output.maf";
	static String logName = "output.log"; 
 
	@After
	public void deleteIO() throws IOException{
       
		File dir = new java.io.File( "." ).getCanonicalFile();
		File[] files = dir.listFiles();
		if (null != files) {
			for(File f: files){ 
			    if(    f.getName().endsWith(".vcf")  ||  f.getName().contains(".log") || f.getName().endsWith(".maf") ||  f.getName().contains("output")){		    
			        f.delete();	
			    }      
			}
		}
		Vcf2mafTest.deleteIO();		
	}
	
	@Test
	public void Frame_Shift_Test()  {
			String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",			
					VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
					VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL",
					VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST",				
					VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE,
					"chr1\t7127\t.\tTC\tC\t.\tMR;MIUN\tSOMATIC;EFF=frameshift_variant(HIGH||ggccaa/|p.Gly105fs/c.313_317delGGCCA|112|AC004824.2|protein_coding|CODING|ENST00000602074|5|1|WARNING_TRANSCRIPT_NO_START_CODON);CONF=ZERO",
					"chr2\t7127\t.\tT\tTCCC\t.\tMR;MIUN\tSOMATIC;EFF=frameshift_variant(HIGH||ggccaa/|p.Gly105fs/c.313_317delGGCCA|112|AC004824.2|protein_coding|CODING|ENST00000602074|5|1|WARNING_TRANSCRIPT_NO_START_CODON);CONF=ZERO"
			};
			
	        try{
	        	Vcf2mafTest.createVcf(str);                
	        	final File input = new File( DbsnpModeTest.inputName);
	        	final Vcf2maf v2m = new Vcf2maf(1,2, null, null);	
	        	try(VCFFileReader reader = new VCFFileReader(input); ){
			 		for (final VcfRecord vcf : reader){  		
			 			SnpEffMafRecord maf  = v2m.converter(vcf);
			 			//INS
			 			if( maf.getColumnValue(5).equals("1") ){
			 				assertTrue(maf.getColumnValue(10).equals(SVTYPE.DEL.name()));
			 				assertTrue(maf.getColumnValue(9).equals("Frame_Shift_DEL"));
			 				assertTrue(maf.getColumnValue(6).equals("7128"));
			 				assertTrue(maf.getColumnValue(7).equals("7128"));		 							 				
			 			}else{
			 				assertTrue(maf.getColumnValue(10).equals(SVTYPE.INS.name()));
			 				assertTrue(maf.getColumnValue(9).equals("Frame_Shift_INS"));
			 				assertTrue(maf.getColumnValue(6).equals("7127"));
			 				assertTrue(maf.getColumnValue(7).equals("7128"));
			 			}
			 		}	
		        }	
	        }catch(Exception e){	fail(e.getMessage()); }
	        
	        new File(inputName).delete();
	}
	

	//test record with some missing sample information, also test the uuid from vcf header to maf column 16,17,33 and 34
	@Test 
	public void EveryTest() throws Exception{
        String[] str = {
        		VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",			
				VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
				VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",
				VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample",	
				VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_bamID",
				VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_bamID",				
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tqControlSample\tqTestSample",
			 	 	"chr1\t72119\t.\tG\tGTATA\t.\tNNS;COVN12;REPEAT\tSOMATIC;NIOC=0;SVTYPE=INS;END=72120;CONF=ZERO;EFF=downstream_gene_variant(MODIFIER||2112||305|OR4F5|protein_coding|CODING|ENST00000335137||1),intergenic_region(MODIFIER||||||||||1)\t"
 	 	                  + "GT:GD:AD:DP:GQ:PL:ACINDEL\t.:.:.:.:.:.:.\t1/1:GTATA/GTATA:0,2:2:6:90,6,0:1,5,5,1[0,1],1[1],1,0,2", 	                  
	
 	  		        "chr3\t21816\t.\tCTTTTTT\tC\t.\tNNS;NPART\tSOMATIC;IN=2;HOM=28,CTTTTCTTTC______TTTTTTTTTT;NIOC=0.020;SVTYPE=DEL;END=21822;CONF=LOW;EFF=intergenic_region(MODIFIER||||||||||1)\t"
 	 		        +"GT:GD:AD:DP:GQ:PL:ACINDEL\t.:.:.:.:.:.:0,34,29,0[0,0],0[0],8,0,2\t0/1:CTTTTTT/C:10,10:20:99:297,0,351:1,51,41,1[0,1],1[1],4,1,4", 		        
 	 		      
 	 		        "chr4\t120614609\trs149427940\tCTT\tC\t731.73\tTPART;NPART\tAC=1,1;AF=0.500,0.500;AN=2;BaseQRankSum=-0.418;ClippingRankSum=1.614;DP=35;FS=3.522;MLEAC=1,1;MLEAF=0.500,0.500;MQ=59.37;MQ0=0;MQRankSum=-0.179;QD=20.91;ReadPosRankSum=-0.896;SOR=0.761;HOM=0,AATGTACCAC__TATTTTTTTT;NIOC=0;SVTYPE=DEL;END=120614611;DB;CONF=LOW;EFF=intergenic_region(MODIFIER||||||||||1)\t"
 	 		        + "GT:GD:AD:DP:GQ:PL:ACINDEL\t.:C/CT:1,14,17:32:99:769,368,360,287,0,223:9,33,31,9[2,7],8[]8,19,0,1\t.:C/CT:5,20,28:53:99:1150,541,675,387,0,331:23,70,65,23[8,15],23[23],29,0,2",

 	 		        "chr11\t1214822581\t.\tG\tGA\t31.731\tHCOVT1\tSOR=0.2331;TRF=3_14\tGT:GD:AD:DP:GQ:PL1\t0/1:G/GA:305,22:327:69:69,0,131721\t.:.:.:.:.:."
 	                  
        };
        	try {
				Vcf2mafTest.createVcf(str);
				final String[] command = {"--mode", "vcf2maf",  "--log", logName,  "-i", inputName , "-o" , outputMafName};
				au.edu.qimr.qannotate.Main.main(command);
			} catch ( Exception e) {
				e.printStackTrace(); 
	        	fail(); 
			}                
			
			try(BufferedReader br = new BufferedReader(new FileReader(outputMafName));) {
			    String line = null;
			    while ((line = br.readLine()) != null) {
				    	if(line.startsWith("#") || line.startsWith(MafElement.Hugo_Symbol.name())) continue; //skip vcf header
				    	
					SnpEffMafRecord maf =  toMafRecord(line);		
	 				assertTrue(maf.getColumnValue(16).equals("TEST_bamID"));
	 				assertTrue(maf.getColumnValue(17).equals("CONTROL_bamID"));     
	 				assertTrue(maf.getColumnValue(33).equals("TEST_sample"));
	 				assertTrue(maf.getColumnValue(34).equals("CONTROL_sample"));   
			 		assertTrue(maf.getColumnValue(MafElement.BAM_File).equals("TEST_bamID:CONTROL_bamID"));
			 		
		 			if(maf.getColumnValue(5).equals("3")){
		 				assertTrue(maf.getColumnValue(12).equals("TTTTTT")); //t_allel1
		 				assertTrue(maf.getColumnValue(13).equals("-"));		 				
		 				assertTrue(maf.getColumnValue(18).equals("TTTTTT")); //n_allel1 go to ref if no evidence
		 				assertTrue(maf.getColumnValue(19).equals("TTTTTT"));		 			 				
		 				assertTrue(maf.getColumnValue(36).equals("0,34,29,0[0,0],0[0],8,0,2"));    //ND
		 				assertTrue(maf.getColumnValue(37).equals("1,51,41,1[0,1],1[1],4,1,4"));		 				
		 				assertTrue(maf.getColumnValue(41).equals("-:ND0:TD1"));
		 		 		assertTrue(maf.getColumnValue(42).equals("CTTTTCTTTC______TTTTTTTTTT"  )); //Var_Plus_Flank or HOM	 
		 		 		assertTrue(maf.getColumnValue(43).equals(MafElement.dbSNP_AF.getDefaultValue())); //Var_Plus_Flank	 
		 				
		 		 		assertTrue(maf.getColumnValue(45).equals("41"));    //t_depth 1,51,41,1[0,1],1[1],4,1,4" 
		 				assertTrue(maf.getColumnValue(46).equals("36"));	//informative - support - partial indel 
		 				assertTrue(maf.getColumnValue(47).equals("1"));
		 				assertTrue(maf.getColumnValue(48).equals("29"));
		 				assertTrue(maf.getColumnValue(49).equals("21"));
		 				assertTrue(maf.getColumnValue(50).equals("0"));		 				
		 				assertTrue(maf.getColumnValue(MafElement.Notes).equals("HOM=28"));
		 				assertTrue(maf.getColumnValue(MafElement.Input).equals("2"));
		 				assertTrue(maf.getColumnValue(MafElement.Var_Plus_Flank).equals("CTTTTCTTTC______TTTTTTTTTT"));
	 				}else if(maf.getColumnValue(5).equals("11")){	 					
		 				assertTrue(maf.getColumnValue(12).equals("null")); //t_allel1
		 				assertTrue(maf.getColumnValue(13).equals("null"));
		 				assertTrue(maf.getColumnValue(18).equals("-")); //n_allel1
		 				assertTrue(maf.getColumnValue(19).equals("A"));		 				
		 				assertTrue(maf.getColumnValue(36).equals("null"));    //ND
		 				assertTrue(maf.getColumnValue(37).equals("null"));		 				
		 				assertTrue(maf.getColumnValue(41).equals("A:ND0:TD0"));
		 				assertTrue(maf.getColumnValue(45).equals("0"));    //t_depth 1,51,41,1[0,1],4,1,4" 
		 				assertTrue(maf.getColumnValue(46).equals("0"));	//informative - support - partial indel - nearbyindel
		 				assertTrue(maf.getColumnValue(47).equals("0"));
		 				assertTrue(maf.getColumnValue(48).equals("0"));
		 				assertTrue(maf.getColumnValue(49).equals("0"));
		 				assertTrue(maf.getColumnValue(50).equals("0"));	
		 				assertTrue(maf.getColumnValue(MafElement.Notes).equals("TRF=3_14"));
		 				assertTrue(maf.getColumnValue(MafElement.Input).equals(SnpEffMafRecord.Null));
	 				}else if(maf.getColumnValue(5).equals("4")){	
						assertTrue(maf.getColumnValue(11).equals("TT")); //reference
		 				assertTrue(maf.getColumnValue(12).equals("-")); //t_allel1
		 				assertTrue(maf.getColumnValue(13).equals("T"));		 				
		 				assertTrue(maf.getColumnValue(18).equals("-")); //n_allel1 go to ref if no evidence
		 				assertTrue(maf.getColumnValue(19).equals("T"));		 			 				
		 				assertTrue(maf.getColumnValue(62).equals("null"));	
		 			//	CTT\tC\t "GT:GD:AD:DP:GQ:PL:ACINDEL\t.:C/CT
	 				}		    	
			    }
			}
	}
	
	@Test
	public void indelOutDirTest() throws Exception{
        String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",			
				VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
				VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_bam",
				VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_bam",
				VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_bamID",
				VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_bamID",
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tqControlSample\tqTestSample",				
		        "chr1\t16864\t.\tGCA\tG\t154.73\tPASS\tAC=1;AF=0.500;AN=2;BaseQRankSum=-0.387;ClippingRankSum=-0.466;DP=12;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=53.00;MQ0=0;MQRankSum=0.143;QD=12.89;ReadPosRankSum=-0.143;SOR=0.495;NIOC=0;SVTYPE=DEL;END=16866;CONF=HIGH;EFF=intergenic_region(MODIFIER||||||||||1)\t"
		        + "GT:GD:AD:DP:GQ:PL:ACINDEL\t0/1:GCA/G:6,5:11:99:192,0,516:8,18,16,8[2,6],9[9],0,0,0\t.:GC/G:8,15:23:99:601,0,384:12,35,34,12[8,4],15[13],0,1,0",
 		        "chr2\t23114\t.\tT\tTAA\t129.73\t.\tAC=1;AF=0.500;AN=2;BaseQRankSum=0.103;ClippingRankSum=-0.470;DP=11;FS=7.782;MLEAC=1;MLEAF=0.500;MQ=27.00;MQ0=0;MQRankSum=0.470;QD=11.79;ReadPosRankSum=-1.453;SOR=3.599;HOMTXT=ATAATAAAATaaAAAAAAAGAC;NIOC=0;SVTYPE=INS;END=23115;CONF=LOW;EFF=intergenic_region(MODIFIER||||||||||1)\t"
 		        + "GT:AD:DP:GQ:PL:ACINDEL:NNS\t0/1:5,5:10:99:167,0,163:8,22,21,9[6,3],9[9],0,0,0:0\t.:6,4:10:99:149,0,210:13,38,37,13[8,5],15[15],0,0,1:0"
        };
		   

        	Vcf2mafTest.createVcf(str);             
        	Vcf2mafTest.createIO();
        	
			final String command = "--mode vcf2maf  --log " + outputDir + "/output.log  -i " + inputName + " --outdir " + outputDir;			
			final Executor exec = new Executor(command, "au.edu.qimr.qannotate.Main");        	
			assertEquals(0, exec.getErrCode());	
						
			 Path path = Paths.get(outputDir,"MELA_0264.CONTROL_bam.TEST_bam.maf" );
			    //The stream hence file will also be closed here
		    try(Stream<String> lines = Files.lines(path)){
		        Optional<String> delline = lines.filter(s ->s.contains("8,18,16,8[2,6],9[9],0,0,0")).filter(s->s.contains("DEL")).findFirst();
		        
 		        if(!delline.isPresent())
		        	Assert.fail("missing DEL variants");		        
		        //split string to maf record		       
				SnpEffMafRecord maf =  toMafRecord(delline.get());
				
 				//"chr1\t16864\t.\tGCA\tG
	            assertTrue(maf.getColumnValue(5).equals("1") );
 				assertTrue(maf.getColumnValue(6).equals("16865")); //start 				
 				assertTrue(maf.getColumnValue(7).equals("16866"));
 				assertTrue(maf.getColumnValue(8).equals("+"));
 				assertTrue(maf.getColumnValue(10).equals(SVTYPE.DEL.name()));
 				assertTrue(maf.getColumnValue(11).equals("CA"));
 				
 				//FORMAT\tqControlSample\tqTestSample",
 				//"GT:GD:AD:DP:GQ:PL:ACINDEL\t0/1:GCA/G:6,5:11:99:192,0,516:8,18,16,8[2,6],0,0,0\t.:GC/G:8,15:23:99:601,0,384:12,35,34,12[8,4],0,1,0",
 				// GT:GD:AD:DP:GQ:PL:ACINDEL\t0/1:GCA/G:6,5:11:99:192,0,516:8,18,16,8[2,6],9[9],0,0,0\t.:GC/G:8,15:23:99:601,0,384:12,35,34,12[8,4],15[13],0,1,0",
 				assertTrue(maf.getColumnValue(12).equals("C")); //t_allel1
 				assertTrue(maf.getColumnValue(13).equals("-"));
 				assertTrue(maf.getColumnValue(18).equals("CA")); //n_allel1
 				assertTrue(maf.getColumnValue(19).equals("-"));
 				assertTrue(maf.getColumnValue(33).equals("TEST_bam")); 	//t_sample_id
 				assertTrue(maf.getColumnValue(34).equals("CONTROL_bam"));	//n_sample_id
 				
 				assertTrue(maf.getColumnValue(35).equals("PASS")); //QFlag
 				assertTrue(maf.getColumnValue(36).equals("8,18,16,8[2,6],9[9],0,0,0"));    //ND
 				assertTrue(maf.getColumnValue(37).equals("12,35,34,12[8,4],15[13],0,1,0"));
 				assertTrue(maf.getColumnValue(38).equals("HIGH")); 
 				assertTrue(maf.getColumnValue(39).equals("MODIFIER"));
 				assertTrue(maf.getColumnValue(40).equals("100")); //??
 				
 				assertTrue(maf.getColumnValue(41).equals("-:ND8:TD12"));
 				assertTrue(maf.getColumnValue(45).equals("34")); 				
 				assertTrue(maf.getColumnValue(46).equals("19"));	//informative - support - partial indel  
 				assertTrue(maf.getColumnValue(47).equals("15"));
 				assertTrue(maf.getColumnValue(48).equals("16"));
 				assertTrue(maf.getColumnValue(49).equals("7"));
 				assertTrue(maf.getColumnValue(50).equals("9"));		
 				assertTrue(maf.getColumnValue(MafElement.Notes).equals(SnpEffMafRecord.Null)); 				
		    }	
 				
		    try(Stream<String> lines = Files.lines(path)){
 				Optional<String> insline = lines.filter(s -> s.contains("INS")).filter(s -> s.contains("23114")).findFirst();
		        if(!insline.isPresent())
		        	Assert.fail("missing INS variants");		        
		        SnpEffMafRecord maf =  toMafRecord(insline.get());
		           
 				//"chr1\t16864\t.\tGCA\tG
	            assertTrue(maf.getColumnValue(5).equals("2") );
 				assertTrue(maf.getColumnValue(6).equals("23114")); //start 				
 				assertTrue(maf.getColumnValue(7).equals("23115"));
 				assertTrue(maf.getColumnValue(8).equals("+"));
 				assertTrue(maf.getColumnValue(10).equals(SVTYPE.INS.name()));
 				assertTrue(maf.getColumnValue(11).equals("-"));
 				
 				//FORMAT\tqControlSample\tqTestSample",
 				//"GT:AD:DP:GQ:PL:ACINDEL:NNS\t0/1:5,5:10:99:167,0,163:8,22,21,9[6,3],0,0,0:0\t.:6,4:10:99:149,0,210:13,38,37,13[8,5],0,0,1:0" 
 				//"GT:AD:DP:GQ:PL:ACINDEL:NNS\t0/1:5,5:10:99:167,0,163:8,22,21,9[6,3],9[9],0,0,0:0\t.:6,4:10:99:149,0,210:13,38,37,13[8,5],15[15],0,0,1:0"
 				assertTrue(maf.getColumnValue(12).equals("-")); //t_allel1
 				assertTrue(maf.getColumnValue(13).equals("-"));
 				assertTrue(maf.getColumnValue(18).equals("-")); //n_allel1
 				assertTrue(maf.getColumnValue(19).equals("AA"));
 				assertTrue(maf.getColumnValue(33).equals("TEST_bam")); 	//t_sample_id
 				assertTrue(maf.getColumnValue(34).equals("CONTROL_bam"));	//n_sample_id
 				
 				assertTrue(StringUtils.isMissingDtaString( maf.getColumnValue(35) )); //QFlag
 				assertTrue(maf.getColumnValue(36).equals("8,22,21,9[6,3],9[9],0,0,0"));    //ND
 				assertTrue(maf.getColumnValue(37).equals("13,38,37,13[8,5],15[15],0,0,1"));
 				assertTrue(maf.getColumnValue(38).equals("LOW")); 
 				assertTrue(maf.getColumnValue(39).equals("MODIFIER"));
 				assertTrue(maf.getColumnValue(40).equals("100")); //??
 				
 				assertTrue(maf.getColumnValue(41).equals("AA:ND8:TD13"));
 				assertTrue(maf.getColumnValue(45).equals("37"));
 				assertTrue(maf.getColumnValue(46).equals("22"));	//informative - support - partial indel - nearbyindel
 				assertTrue(maf.getColumnValue(47).equals("15"));
 				assertTrue(maf.getColumnValue(48).equals("21"));
 				assertTrue(maf.getColumnValue(49).equals("12"));
 				assertTrue(maf.getColumnValue(50).equals("9"));				        	
		    }		   
	}
	
	@Test
	public void IdsTest() throws Exception{
        String[] str = {VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",			
        		VcfHeaderUtils.STANDARD_FILE_FORMAT + "=VCFv4.0",			
        		VcfHeaderUtils.STANDARD_DONOR_ID + "=MELA_0264",
        		VcfHeaderUtils.STANDARD_CONTROL_SAMPLE + "=CONTROL_sample",
        		VcfHeaderUtils.STANDARD_TEST_SAMPLE + "=TEST_sample" ,
        		VcfHeaderUtils.STANDARD_CONTROL_BAM + "=CONTROL_bam",
        		VcfHeaderUtils.STANDARD_TEST_BAM + "=TEST_bam",
        	//	VcfHeaderUtils.STANDARD_CONTROL_BAMID + "=CONTROL_bamID",
        		VcfHeaderUtils.STANDARD_TEST_BAMID + "=TEST_bamID",
				VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL_bam\tTEST_sample",				
		        "chr1\t16864\t.\tGCA\tG\t154.73\tPASS\tAC=1;AF=0.500;AN=2;BaseQRankSum=-0.387;ClippingRankSum=-0.466;DP=12;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=53.00;MQ0=0;MQRankSum=0.143;QD=12.89;ReadPosRankSum=-0.143;SOR=0.495;NIOC=0;SVTYPE=DEL;END=16866;CONF=HIGH;EFF=intergenic_region(MODIFIER||||||||||1)\t"
		        + "GT:GD:AD:DP:GQ:PL:ACINDEL\t0/1:GCA/G:6,5:11:99:192,0,516:8,18,16,8[2,6],9[9],0,0,0\t.:GC/G:8,15:23:99:601,0,384:12,35,34,12[8,4],15[13],0,1,0"
        };
		   
    	try {
			Vcf2mafTest.createVcf(str);
			final String[] command = {"--mode", "vcf2maf",  "--log", logName,  "-i", inputName , "-o" , outputMafName};
			au.edu.qimr.qannotate.Main.main(command);
		} catch ( Exception e) {
			e.printStackTrace(); 
        	fail(); 
		}                
		
		try(BufferedReader br = new BufferedReader(new FileReader(outputMafName));) {
		    String line = null;
		    while ((line = br.readLine()) != null) {
			    	if(line.startsWith("#") || line.startsWith(MafElement.Hugo_Symbol.name())) continue; //skip vcf header
			    	
				SnpEffMafRecord maf =  toMafRecord(line);		       
				assertTrue( maf.getColumnValue( MafElement.BAM_File).equals("TEST_bamID:CONTROL_bam")) ;
				assertTrue( maf.getColumnValue( MafElement.Tumor_Sample_Barcode).equals("TEST_bamID")) ;
				assertTrue( maf.getColumnValue( MafElement.Matched_Norm_Sample_Barcode).equals("CONTROL_bam")) ;
				assertTrue( maf.getColumnValue( MafElement.Tumor_Sample_UUID).equals("TEST_sample"));
				assertTrue( maf.getColumnValue( MafElement.Matched_Norm_Sample_UUID).equals("CONTROL_sample"));
 
		 		 
	        }	
        }catch(Exception e){	fail(e.getMessage()); }
        
        new File(inputName).delete();
		    
	}
	
	public static SnpEffMafRecord toMafRecord(String line) throws Exception{
        //split string to maf record
		SnpEffMafRecord maf = new SnpEffMafRecord();
        String[] eles = line.split("\\t");
        
        assertTrue(eles.length == MafElement.values().length);
             
        for(int i = 0; i < eles.length; i ++){
        //	maf.setColumnValue( MafElement.getByColumnNo( i+1), ""); //wipe off all default value
        	maf.setColumnValue( MafElement.getByColumnNo( i+1), eles[i]);       	
        }
		return maf; 		
	}	
	
}
