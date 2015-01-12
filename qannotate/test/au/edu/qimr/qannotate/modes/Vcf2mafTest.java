package au.edu.qimr.qannotate.modes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class Vcf2mafTest {
	 @BeforeClass
	public static void createInput() throws IOException{
		createVcf();
		
		
		

 
	}
 
	 


	
	 @Test
	 public  void createMaf() throws IOException, Exception{
		final Vcf2maf v2m = new Vcf2maf(3, 2);
		 
		try(VCFFileReader reader = new VCFFileReader(new File( DbsnpModeTest.inputName)); ){
			System.out.println(SnpEffMafRecord.getSnpEffMafHeaderline());
			final SnpEffMafRecord[] maf = new SnpEffMafRecord[3];
			int i=0;
        	for (final VcfRecord vcf : reader){ 
        		maf[i++] = v2m.converter(vcf);
        		System.out.println(maf[i-1].getMafLine()  );
        	}	
        }
        	
        	//	System.out.println(vcf.toString());
        	//	
        	 
		final String [] alts = {}; 	
		for (final String alt : alts)  
			System.out.println("null array!" + alt);
		
		 
	 }
	
	
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        data.add(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE + "\tFORMAT\tCONTROL\tTEST");
        data.add("chrY\t22012840\t.\tC\tA\t.\tSBIAS\tMR=15;NNS=13;FS=GTGATATTCCC;EFF=sequence_feature[compositionally_biased_region:Glu/Lys-rich](LOW|||c.1252G>C|591|CCDC148|protein_coding|CODING|ENST00000283233|10|1),splice_acceptor_variant(HIGH|||n.356G>C||CCDC148-AS1|antisense|NON_CODING|ENST00000412781|5|1)\tGT:GD:AC\t0/0:C/A:A1[5],0[0],C6[6.67],0[0],T1[6],21[32.81]\t0/1:C/A:C8[7.62],2[2],A2[8],28[31.18]");        
        data.add("chr2\t91888700\trs78405093\tG\tA\t.\tPASS\tMR=1217;NNS=1;FS=TGAGCACCTAC;GMAF=0.113802559414991;EFF=intron_variant(MODIFIER|||n.376-566C>T||AC027612.3|processed_transcript|NON_CODING|ENST00000436174|4|1),intron_variant(MODIFIER|||n.478-123C>T||AC027612.3|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000445955|4|1)\tGT:GD:AC\t1/1:G/T:G0[0],1217[35.41],T0[0],5[26.8],A0[0],7[34],C0[0],7[31]\t1/1:A/A:A0[0],1217[35.41],C0[0],5[26.8],G0[0],7[34],T0[0],7[31]"); 
        data.add("chrY\t2675825\t.\tTTG\tTCA\t.\tMIN;MIUN\tSOMATIC;END=2675826\tACCS\tTTG,5,37,TCA,0,2\tTAA,1,1,TCA,4,1,TCT,3,1,TTA,11,76,TTG,2,2,_CA,0,3,TTG,0,1");
 
        
        
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(DbsnpModeTest.inputName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
	}

}
