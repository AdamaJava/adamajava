package org.qcmg.qprofiler.vcf;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Test;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.qprofiler.QProfilerSummary;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.vcf.VcfSummaryReport;
import org.w3c.dom.*;

public class VcfSummaryReportTest {
	private final static String inputfile = "input.vcf";
	private boolean Iterator;
	
 
	
	@Test
	public void HeaderTest() throws Exception{
		
		File file = createVcfHeader(inputfile);	
		VcfSummaryReport vcfSummaryReport; 
		VcfHeader header;
		try (VCFFileReader reader = new VCFFileReader(file) ){
			header = reader.getHeader();
			vcfSummaryReport = new VcfSummaryReport(header);			
		}
		
		 
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Element root = builder.getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
		vcfSummaryReport.toXml(root);
		
		Node nreport = root.getChildNodes().item(0);
		assertTrue(nreport.getNodeName().equals( ProfileType.VCF  + "Report"));
		
		Node child = nreport.getChildNodes().item(0);
		assertTrue(child.getNodeName().equals( VcfSummaryReport.NodeHeader));		 
		assertTrue(child.getChildNodes().getLength() == 3);
		
		//check meta Information line
		child = nreport.getChildNodes().item(0).getFirstChild(); 
		assertTrue(child.getNodeName().equals(VcfSummaryReport.NodeHeaderMeta));		
		assertTrue(child.getChildNodes().getLength() == 12 ); 
		boolean hasInput = false; 
		for(int i = 0; i < 12; i++){
			child = nreport.getChildNodes().item(0).getFirstChild().getChildNodes().item(i);
			assertTrue(child.getNodeName().equals(VcfSummaryReport.NodeHeaderMetaLine)  );
			String key =   child.getAttributes().getNamedItem("key").getNodeValue();
			String value =   child.getAttributes().getNamedItem("value").getNodeValue();
			if(key.equals("SnpEffVersion")   ) value = "\"" + value + "\"";
			else if(key.equals("SnpEffCmd")) value = "\"" + value + " \"";
			
			if( key.equals("INPUT")  ){
				int index = hasInput ? 1 : 0;   
				assertTrue( header.getRecords(key).get(index) .getMetaValue().equals(value) );
				hasInput = true; 
			}else		
				assertTrue( header.firstMatchedRecord(key).getMetaValue().equals(value) );
		}
		 
		//check structured meta information line
		child = nreport.getChildNodes().item(0).getChildNodes().item(1); 
		assertTrue(child.getNodeName().equals(VcfSummaryReport.NodeHeaderStructured));		
		assertTrue(child.getChildNodes().getLength() == 4 ); 
		int pgIds = 0;
		for(int i = 0; i < 4; i++){
			Node node =  child.getChildNodes().item(i);
			assertTrue(node.getNodeName().equals( VcfSummaryReport.NodeHeaderStructuredType ));	
			String key = node.getAttributes().getNamedItem("type").getNodeValue();	
			for(int j = node.getChildNodes().getLength()-1; j >= 0; j --){
				node = child.getChildNodes().item(i).getChildNodes().item(j);
				assertTrue(node.getNodeName().equals( VcfSummaryReport.NodeHeaderStructuredLine ));	
				String id = node.getAttributes().getNamedItem("ID").getNodeValue();	
				if(key.equals("qPG")) 
					pgIds += Integer.parseInt(id);
				for(Pair p : header.getIDRecord(key, id).getSubFields()){					
					assertTrue(((String) p.getRight()).replace( "\"", "").equals(node.getAttributes().getNamedItem( (String) p.getLeft()).getNodeValue() ));
				}
			}
			
		}
		assertTrue(pgIds == 8);
		
		//final head line
		child = nreport.getChildNodes().item(0).getChildNodes().item(2); 
		assertTrue(child.getNodeName().equals(VcfSummaryReport.NodeHeaderFinal));	
		assertTrue(child.getChildNodes().getLength() == 0 ); 
		assertTrue(child.getAttributes().getNamedItem("columns").getNodeValue().replace(" ", "\t").equals(header.getChrom().toString())    );
		
//		QProfilerSummary sol = new QProfilerSummary();
//		sol.asXmlText(root, "output.xml");		
		file.delete();		
	}	
  
	
	
	
	public static void createVcfFile() throws IOException{
	  final List<String> data = new ArrayList<String>();
	  data.add("##fileformat=VCFv4.2");
	  data.add("##FORMAT=<ID=AC,Number=1,Type=String,Description=\"Allele Count: lists number of reads on forward strand [avg base quality], reverse strand [avg base quality]\">"); 
	  data.add("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");	  
	  data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tcontrol\ttest");	  
	  data.add("chr1\t10231\t.\tC\tA\t.\t5BP1\tFLANK=CCTAAACCCTA\tGT:GD:AC:MR:NNS\t0/1:A/C:A2[37],2[11],C18[36.06],6[42]:4:4\t1/1:C/C:A2[37],0[0],C18[30.39],6[36]:2:2"); 
	  data.add("chr1\t13302\t.\tC\tT,G\t.\tPASS\tFLANK=GGACATGCTGT\tGT:GD:AC:MR:NNS\t0/1:C/T:C7[42],7[36.14],T4[35.75],2[42]:6:6\t0/2:G/T:C11[40.64],6[37.83],T5[28.8],4[40.75]:9:7"); 
	  data.add("chr1\t1399617\trs142358585\tC\tT\t.\tPASS_1;PASS_2\tSOMATIC_1;\tGT:GD:AC:DP\t0/0&.:C/C&C/C:.:.:\t.&0/1:C/T&C/T:30&30:27&27"); 
 	         
	  try(BufferedWriter out = new BufferedWriter(new FileWriter(inputfile));){      
	     for (final String line : data)  out.write(line + "\n");
	  }    
	}
	
	
	public static File  createVcfHeader(String filename) throws IOException{
		  final List<String> data = new ArrayList<String>();
		  data.add("##fileformat=null");
		  data.add("##qUUID=612fd093-d806-43d2-9df1-0976ccd8d029");
		  data.add("##qSource=qannotate-2.0 (1534)");
		  data.add("##1:qControlSample=9a869714-00cf-434d-abb1-d90df7999e6a");
		  data.add("##1:qTestSample=ec22fee4-cd79-4190-bcc3-b986d5d8a95a");	  
		  data.add("##1:qDonorId=http://purl.org/net/grafli/donor#\"2422102e-d66e-4acc 91b8-35dba381232b\"");
		  data.add("##2:qControlSample=9a869714-00cf-434d-abb1-d90df7999e6a");
		  data.add("##2:qTestSample=ec22fee4-cd79-4190-bcc3-b986d5d8a95a");
		  data.add("##INPUT=1,FILE=/mnt/lustre/working/genomeinfo/share/temp/mock_analysis/5/7/57e19da8-f2bd-4044-891f-31d69def6f42/57e19da8-f2bd-4044-891f-31d69def6f42.vcf");
		  data.add("##SnpEffVersion=\"4.0e (build 2014-09-13), by Pablo Cingolani\"");
		  data.add("##SnpEffCmd=\"SnpEff  -o VCF -stats /mnt/lustre/working/genomeinfo/share/temp/mock_analysis/9/1/91a93414-acdd-4620-9c10-159ec89a753b/91a93414-acdd-4620-9" +
				  "c10-159ec89a753b.merged.dbsnp.germ.conf.snpeff.vcf.snpEff_summary.html GRCh37.75 /mnt/lustre/working/genomeinfo/share/temp/mock_analysis/9/1/91a93414-ac" +
				  "dd-4620-9c10-159ec89a753b/91a93414-acdd-4620-9c10-159ec89a753b.merged.dbsnp.germ.conf.vcf \"");
		  data.add("##qPG=<ID=3,Tool=qannotate,Version=2.0 (1534),Date=2017-02-02 20:09:28,CL=\"qannotate --mode snpeff -i /mnt/lustre/working/genomeinfo/share/temp/mock_analysis/9/1/91a93414-acdd-4620-9c10-159ec89a753b/91a93414-acdd-4620-9c10-159ec89a753b.merged.dbsnp.germ.conf.vcf -o /mnt/lustre/working/genomeinfo/share/temp/mock_analysis/9/1/91a93414-acdd-4620-9c10-159ec89a753b/91a93414-acdd-4620-9c10-159ec89a753b.merged.dbsnp.germ.conf.snpeff.vcf --log 91a93414-acdd-4620-9c10-159ec89a753b.merged.dbsnp.germ.conf.snpeff.vcf.log -d /mnt/lustre/reference/software/snpEff/GRCh37.75\">");
		  data.add("##qPG=<ID=5,Tool=qannotate,Version=2.0 (1534),Date=2017-02-02 20:09:28,CL=\"qannotate --mode snpeff -i /mnt/lustre/working/genomeinfo/share/temp/mock_analysis/9/1/91a93414-acdd-4620-9c10-159ec89a753b/91a93414-acdd-4620-9c10-159ec89a753b.merged.dbsnp.germ.conf.vcf -o /mnt/lustre/working/genomeinfo/share/temp/mock_analysis/9/1/91a93414-acdd-4620-9c10-159ec89a753b/91a93414-acdd-4620-9c10-159ec89a753b.merged.dbsnp.germ.conf.snpeff.vcf --log 91a93414-acdd-4620-9c10-159ec89a753b.merged.dbsnp.germ.conf.snpeff.vcf.log -d /mnt/lustre/reference/software/snpEff/GRCh37.75\">");
		  data.add("##INFO=<ID=EFF,Number=.,Type=String,Description=\"Predicted effects for this variant.Format: 'Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )' \">");
		  data.add("##INFO=<ID=EFF,Number=.,Type=String,Description=\"Predicted effects for this variant.Format: 'Effect ( Effect_Impact | Functional_Class | Codon_Change | Amino_Acid_Change| Amino_Acid_length | Gene_Name | Transcript_BioType | Gene_Coding | Transcript_ID | Exon_Rank  | Genotype_Number [ | ERRORS | WARNINGS ] )' \">");
		  data.add("##FILTER=<ID=SBIASALT,Description=\"Alternate allele on only one strand (or percentage alternate allele on other strand is less than 5%)\">");
		  data.add("##FORMAT=<ID=GD,Number=1,Type=String,Description=\"Genotype details: specific alleles (A,G,T or C)\">");
		  data.add("##FORMAT=<ID=FT,Number=.,Type=String,Description=\"Filters that apply to this sample\">");
		  data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t4a88db37-6b5a-430e-a318-1f82bb614447_1\t00b30292-ffcd-44f3-b7db-36749e3226ce_1\t4a88db37-6b5a-430e-a318-1f82bb614447_2\t00b30292-ffcd-44f3-b7db-36749e3226ce_2");
		  data.add("##INPUT=2,FILE=/mnt/lustre/working/genomeinfo/share/temp/mock_analysis/1/6/1601fab0-b7d9-4363-85c6-dbf5e4a45967/1601fab0-b7d9-4363-85c6-dbf5e4a45967.vcf");
		 
		  try(BufferedWriter out = new BufferedWriter(new FileWriter(filename));){      
		     for (final String line : data)  out.write(line + "\n");
		  } 
		  
		 return  new File(filename);
	}	
	 

}
