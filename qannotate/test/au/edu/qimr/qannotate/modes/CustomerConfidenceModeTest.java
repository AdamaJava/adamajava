package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;
import org.qcmg.common.model.MafConfidence;
import org.qcmg.common.vcf.VcfInfoFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;

import au.edu.qimr.qannotate.utils.SampleColumn;

public class CustomerConfidenceModeTest {
	
	 @AfterClass
	 public static void deleteIO(){
		 new File(DbsnpModeTest.inputName).delete();
	 }
	 
	 @Test
	 public void sampleidTest() throws Exception{
		createInputFile();
		final CustomerConfidenceMode mode = new CustomerConfidenceMode();		
		mode.loadVcfRecordsFromFile(new File(DbsnpModeTest.inputName),true);
			 
		SampleColumn column =  SampleColumn.getSampleColumn(null,null,  mode.header);
		mode.setSampleColumn(column.getTestSampleColumn(), column.getControlSampleColumn() );
		mode.addAnnotation();
		mode.reheader("testing run",   DbsnpModeTest.inputName );
		mode.writeVCF(new File(DbsnpModeTest.outputName));
		
		try(VCFFileReader reader = new VCFFileReader(DbsnpModeTest.outputName)){			
			for (final VcfRecord re : reader) {		
				final VcfInfoFieldRecord infoRecord = new VcfInfoFieldRecord(re.getInfo()); 				
				if(re.getPosition() == 41281388) 
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.HIGH.toString())); 
				else if(re.getPosition() == 41281389) 
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.ZERO.toString())); 
				else if(re.getPosition() == 41281390) 
					assertTrue(infoRecord.getField(VcfHeaderUtils.INFO_CONFIDENCE).equals(MafConfidence.ZERO.toString())); 
			}
		}		 
	 }
						
	public static void createInputFile() throws IOException{
        final List<String> data = new ArrayList<String>();
        data.add("##fileformat=VCFv4.2");
//        data.add("##");
        data.add("##qControlSample=null");
        data.add("##qTestSample=null");
//        data.add("##");
        data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tqTestSample\tqControlSample");
        data.add("chr3\t41281388\t.\tT\tG\t.\t5BP55\tFLANK=ATTTAGCAAAC;CONF=HIGH\tGT:GD:AC:MR:NNS\t0/1:G/T:A0[0],15[16.53],C1[38],0[0],G0[0],105[27.78],T112[37.92],106[22.47]:105:2\t0/1:G/T:A0[0],15[16.53],C1[38],0[0],G0[0],105[27.78],T112[37.92],106[22.47]:105:2");
        data.add("chr3\t41281389\t.\tT\tG\t.\t%5BP67\tFLANK=ATTTAGCAAAC;CONF=HIGH\tGT:GD:AC:MR:NNS\t0/1:G/T:A0[0],15[16.53],C1[38],0[0],G0[0],105[27.78],T112[37.92],106[22.47]:105:2\t0/1:G/T:A0[0],15[16.53],C1[38],0[0],G0[0],15[27.78],T112[37.92],106[22.47]:15:2");
        data.add("chr3\t41281390\t.\tT\tG\t.\tSBIAS;SBIASCOV\tFLANK=ATTTAGCAAAC;CONF=HIGH\tGT:GD:AC:MR:NNS\t0/1:G/T:A0[0],15[16.53],C1[38],0[0],G0[0],105[27.78],T112[37.92],106[22.47]:105:2\t0/1:G/T:A0[0],15[16.53],C1[38],0[0],G0[0],15[27.78],T112[37.92],106[22.47]:15:2");

        try(BufferedWriter out = new BufferedWriter(new FileWriter(DbsnpModeTest.inputName));){      
           for (final String line : data)  out.write(line + "\n");
        }
	          
	}
}
