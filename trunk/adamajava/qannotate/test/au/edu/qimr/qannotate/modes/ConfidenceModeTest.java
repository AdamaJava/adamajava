package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.modes.AbstractMode.SampleColumn;
import au.edu.qimr.qannotate.modes.ConfidenceMode.Confidence;

public class ConfidenceModeTest {
	
	final static String VerifiedFileName = "verify.vcf";
	final static String patient = "APGI_2001";
	
	@BeforeClass
	public static void createInput() throws IOException{
	

		createVerifiedFile();
	}
	
	 @AfterClass
	 public static void deleteIO(){

		 new File(DbsnpModeTest.inputName).delete();
		 new File(VerifiedFileName).delete();
		 new File(DbsnpModeTest.outputName).delete();
		 
	 }
	
	
	//@Ignore 
	 @Test (expected=Exception.class)
	public void SampleColumnTest() throws IOException, Exception{
		final ConfidenceMode mode = new ConfidenceMode(patient);		
		mode.inputRecord(new File(DbsnpModeTest.inputName));
		mode.addAnnotation(VerifiedFileName);
		
		//
		
		//new SampleColumn();
	}
	 @Test
	 public void SingleSampleTest() throws IOException, Exception{	
		 	DbsnpModeTest.createVcf();
			final ConfidenceMode mode = new ConfidenceMode(patient);		
			mode.inputRecord(new File(DbsnpModeTest.inputName));
			
			String Scontrol = "EXTERN-MELA-20140505-001";
			String Stest = "EXTERN-MELA-20140505-002";
			mode.header.parseHeaderLine("##qControlSample=" + Scontrol);
			mode.header.parseHeaderLine("##qTestSample="+ Stest);	
			mode.header.parseHeaderLine("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tEXTERN-MELA-20140505-001\tEXTERN-MELA-20140505-002");			
			
			//mode.retriveDefaultSampleColumn();
			SampleColumn column = new SampleColumn(Stest, Scontrol, mode.header);
			mode.setSampleColumn(column.getTestSampleColumn(), column.getControlSampleColumn() );
			
			mode.addAnnotation(VerifiedFileName);
			mode.reheader("unitTest", DbsnpModeTest.inputName);
			mode.writeVCF(new File(DbsnpModeTest.outputName)  );
			
  			try(VCFFileReader reader = new VCFFileReader(DbsnpModeTest.outputName)){				 				 
				for (final VcfRecord re : reader) {		
					final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(re.getInfo()); 				
					if(re.getPosition() == 2675826) 
						//compound SNPs
						assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENT).equals(Confidence.LOW.name())); 
					else if(re.getPosition() == 22012840)
						//isClassB
						assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENT).equals(Confidence.LOW.name())); 
					else if(re.getPosition() == 14923588)  
						//listed on verified file
						assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENT).equals(Confidence.HIGH.name())); 
				}
			 }		
	 }	 
	 
	 
	 @Test
	 public void ConfidenceTest() throws IOException, Exception{	
		 	DbsnpModeTest.createVcf();
			final ConfidenceMode mode = new ConfidenceMode(patient);		
			mode.inputRecord(new File(DbsnpModeTest.inputName));

			String Scontrol = "EXTERN-MELA-20140505-001";
			String Stest = "EXTERN-MELA-20140505-002";
			mode.header.parseHeaderLine("##qControlSample=" + Scontrol);
			mode.header.parseHeaderLine("##qTestSample="+ Stest);	
			mode.header.parseHeaderLine("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tEXTERN-MELA-20140505-001\tEXTERN-MELA-20140505-002");			
			
			SampleColumn column = new SampleColumn(Stest, Scontrol, mode.header);
			mode.setSampleColumn(column.getTestSampleColumn(), column.getControlSampleColumn() );
			
			mode.addAnnotation(VerifiedFileName);
			mode.reheader("unitTest", DbsnpModeTest.inputName);
			mode.writeVCF(new File(DbsnpModeTest.outputName)  );
			
  			try(VCFFileReader reader = new VCFFileReader(DbsnpModeTest.outputName)){				 				 
				for (final VcfRecord re : reader) {		
					final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(re.getInfo()); 				
					if(re.getPosition() == 2675826) 
						//compound SNPs
						assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENT).equals(Confidence.LOW.name())); 
					else if(re.getPosition() == 22012840)
						//isClassB
						assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENT).equals(Confidence.LOW.name())); 
					else if(re.getPosition() == 14923588)  
						//listed on verified file
						assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENT).equals(Confidence.HIGH.name())); 
				}
			 }		 	 	 
	 }

	public static void createVerifiedFile() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("#version 3.0 all SNP and indel verification (not including APGI_1830)");
        data.add("##dbSNP_BUILD_ID=135");  
        data.add("PatientID\tSampleID\tChrPos\tInputType\tGeneID\tMutationClass\tbase_change\tAmpliconStart\tAmpliconEnd\tAmpliconSize\tSeqDirection\tPrimerTM\tPrimerBarcode\tPrimerPlateID\tPlatePos\tPlateIDTf\tPlateIDTr\tIonTorrentRunID\tTool\tConseq\tQCMGflag\tTD\tND\tNumMutReads\tpatient\tChr\tPos\tref\tND_A\tND_C\tND_G\tND_T\tND_N\tND_TOTAL\tlocation\tpatient\tChr\tPos\tref\tTD_A\tTD_C\tTD_G\tTD_T\tTD_N\tTD_TOTAL\tlocation\tPatientID\tChrPos\tbase_change\tref\tmutant\tref_ND\tmut_ND\tref_TD\tmut_TD\tfreq_ND\tfreq_TD\tverification"); 
        data.add("APGI_1127\txxx\tchr10:70741311-70741311\tTD\tDDX21\tsomaticclassA\tC>T\txxx\txxx\t84\tF\t\"59.547,59.22\"\tACGCGAGTAT\txxx\txxx\txxx\txxx\txxx\tqSNP\tNON_SYNONYMOUS_CODING\t--\t\"C:14[38.53],30[30.86],T:0[0],6[40]\"\t\"C:26[39.58],25[36.13]\"\t6\tAPGI_1127\tchr10\t70741311\tC\tA:1\tC:1603\tG:0\tT:0\tN:0\t1604\tchr10:70741311-70741311\tAPGI_1127\tchr10\t70741311\tC\tA:0\tC:1264\tG:0\tT:0\tN:0\t1264\tchr10:70741311-70741311\tAPGI_1127\tchr10:70741311-70741311\tC>T\tC\tT\t1603\t0\t1264\t0\t0\t0\tno");
        data.add(patient +"\txxx\tchrY:14923588-14923588\tTD\tUSP9Y\tsomaticclassA\tG>A\txxx\txxx\t98\tF\t\"61.078,59.793\"\tTACTCTCGTG\txxx\txxx\txxx\txxx\txxx\tqSNP\tNON_SYNONYMOUS_CODING\t--\t\"A:23[40],0[0],G:12[36.84],10[36.91]\"\t\"G:29[34.2],11[35.19]\"\t23\tAPGI_2001\tchrY\t14923588\tG\tA:0\tC:1\tG:1283\tT:0\tN:0\t1284\tchrY:14923588-14923588\tAPGI_2001\tchrY\t14923588\tG\tA:0\tC:0\tG:33\tT:0\tN:0\t33\tchrY:14923588-14923588\tAPGI_2001\tchrY:14923588-14923588\tG>A\tG\tA\t1283\t0\t33\t0\t0\t0\tyes");
               
        try(BufferedWriter out = new BufferedWriter(new FileWriter(VerifiedFileName));){      
           for (final String line : data)  out.write(line + "\n");
        }
          
	}
}
