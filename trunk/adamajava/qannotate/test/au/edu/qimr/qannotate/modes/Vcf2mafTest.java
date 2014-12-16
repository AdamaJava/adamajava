package au.edu.qimr.qannotate.modes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.utils.SnpEffMafRecord.mutation_status;

public class Vcf2mafTest {
	 @BeforeClass
	public static void createInput() throws IOException{
		createVcf();
	}
	 
	
	 @Test
	 public  void createMaf() throws IOException, Exception{
		 
		 
		 final String in = "\tFormat\tsample1\tsample2\n";
		 final String[] out = in.split(Constants.TAB+"");
		 final Map<String,String> field = new HashMap<>();	
		 System.out.println( "get " + field.get("ok"));
		 
		 for(int i  = 0; i < out.length; i++)
			 System.out.println("i: " + out[i] );
		 
			
		 
		 final String s = "MODIFIER|||n.376-566C>T||AC027612.3|processed_transcript|NON_CODING|ENST00000436174|4|1";
		 final String[] aa = s.split(Vcf2maf.bar);
		 System.out.println(mutation_status.PostTranscriptional.toString() + " " + mutation_status.LOH);
		 
		 final Vcf2maf v2m = new Vcf2maf();
		 
			try(VCFFileReader reader = new VCFFileReader(new File( DbsnpModeTest.inputName)); ){
				
	        	for (final VcfRecord vcf : reader){ 
	        	//	final SnpEffMafRecord maf = v2m.converter(vcf);
	        		System.out.println(vcf.toString());
	        	//	System.out.println(maf.toString()  );
	        		
	        	}
	        	
	        	
	        	
	        	
	        		//splitEFF(  vcf );
	        	
	        	//for(final Entry<String, String> set : effRanking.entrySet())
	        		//out.println(  set.getValue() + "\t" + set.getKey());
	  
			}
		 
	 }
	
	
	public static void createVcf() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.0");
        data.add("chr2\t91888700\trs78405093\tG\tA\t.\tPASS\tMR=1217;NNS=1;FS=TGAGCACCTAC;EFF=intron_variant(MODIFIER|||n.376-566C>T||AC027612.3|processed_transcript|NON_CODING|ENST00000436174|4|1),intron_variant(MODIFIER|||n.478-123C>T||AC027612.3|transcribed_unprocessed_pseudogene|NON_CODING|ENST00000445955|4|1)\tGT:GD:AC\t1/1:A/A:A0[0],1217[35.41],C0[0],5[26.8],G0[0],7[34],T0[0],7[31]1/1:A/A:A0[0],1217[35.41],C0[0],5[26.8],G0[0],7[34],T0[0],7[31]"); 
        data.add("chrY\t2675825\t.\tTTG\tTCA\t.\tMIN;MIUN\tSOMATIC;END=2675826\tACCS\tTTG,5,37,TCA,0,2\tTAA,1,1,TCA,4,1,TCT,3,1,TTA,11,76,TTG,2,2,_CA,0,3,TTG,0,1");
        data.add("chrY\t22012840\t.\tC\tA\t.\tSBIAS\tMR=15;NNS=13;FS=GTGATATTCCC\tGT:GD:AC\t0/1:C/A:A0[0],15[36.2],C11[36.82],9[33]\t0/1:C/A:A0[0],33[35.73],C6[30.5],2[34]"); 
        
        try(BufferedWriter out = new BufferedWriter(new FileWriter(DbsnpModeTest.inputName));) {          
            for (final String line : data)   out.write(line + "\n");                  
         }  
	}

}
