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
	
	@Test
	public void HeaderTest() throws Exception{
		
		File file = createVcfHeader( );	
		VcfHeader header;
		try (VCFFileReader reader = new VCFFileReader(file) ){ 	header = reader.getHeader(); }
		
		Node nreport = getXmlParentNode( file ) ;		
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
		file.delete();		
	}	
  
	@Test
	public void MetricsTest()throws Exception{
		File file = createVcfFile();			
		Node nreport = getXmlParentNode( file ) ;
		
//		QProfilerSummary sol = new QProfilerSummary();
//		sol.asXmlText( nreport.getOwnerDocument().getDocumentElement() , "output.xml");		
		 	
		Node child = nreport.getChildNodes().item(1);
		assertTrue(child.getNodeName().equals( VcfSummaryReport.NodeSummary));		 
		assertTrue(child.getChildNodes().getLength() == 4);
		
		//the common part of each sample level xml
		int snv0 = 0, dnv0 = 0, tnv0 = 0 ;
		int dbsnv0 = 0, dbdnv0 = 0, dbtnv0 = 0 ;
		for(int sampleNo = 0; sampleNo < 4; sampleNo ++){
			child = nreport.getChildNodes().item(1).getChildNodes().item(sampleNo); 
			assertTrue( child.getNodeName().equals( "Sample") );
			String value = ( sampleNo%2==0)? "control" + (sampleNo/2+1) : "test" + (sampleNo/2+1 );
			assertTrue( child.getAttributes().getNamedItem("value").getNodeValue().equals( value) );	
			
			if(sampleNo < 3) 
				assertTrue( child.getChildNodes().getLength() == 1 );			 
			else
				assertTrue( child.getChildNodes().getLength() == 2 );
						
			
			int vcfs = 0, snv = 0, dnv = 0, tnv = 0 ;
			int   dbsnv = 0, dbdnv = 0, dbtnv = 0 ;			
			for(int j = 0; j < child.getChildNodes().getLength() ; j++ ){
				Node  node = child.getChildNodes().item(j);
				assertTrue(node.getAttributes().getNamedItem("type").getNodeValue().equals("FORMAT:INF") );
				if( sampleNo%2==0 || j == 1) 
					assertTrue( node.getAttributes().getNamedItem("value").getNodeValue().equals("GERMLINE") );
				else 
					assertTrue( node.getAttributes().getNamedItem("value").getNodeValue() .equals("SOMATIC") );	
				
				int ftVcfs = 0;
				for(int ftNo = 0; ftNo < child.getChildNodes().item(j).getChildNodes().getLength(); ftNo ++ ){
					node = child.getChildNodes().item(j).getChildNodes().item(ftNo);
					assertTrue(node.getAttributes().getNamedItem("type").getNodeValue().equals("FORMAT:FT") );

					if(ftNo == 0 && !(sampleNo == 3 && j == 0))
						assertTrue(node.getAttributes().getNamedItem("value").getNodeValue().equals("PASS") );
					else 						 
						assertTrue(node.getAttributes().getNamedItem("value").getNodeValue().equals("Other") );					
						ftVcfs += Integer.parseInt( node.getAttributes().getNamedItem("count").getNodeValue());	
						
					//get node of eg. <VariationType count="1" inDBSNP="1" type="SNV">
					node = child.getChildNodes().item(j).getChildNodes().item(ftNo).getFirstChild();							
					for( int vtypeNo = 0; vtypeNo < node.getChildNodes().getLength(); vtypeNo ++){
						Node nodeNo = node.getChildNodes().item(vtypeNo);
						assertTrue( nodeNo.getNodeName().equals("VariationType") );
						
						int count = Integer.parseInt(nodeNo.getAttributes().getNamedItem("count").getNodeValue() );
						int dbCount = Integer.parseInt(nodeNo.getAttributes().getNamedItem("inDBSNP").getNodeValue() );
						String type = nodeNo.getAttributes().getNamedItem("type").getNodeValue();
						if( type.equals("SNV")  && sampleNo == 0 ){
							snv0 += count;  dbsnv0 += dbCount;
						}else if( type.equals("SNV")  && sampleNo > 0 ){
							snv += count;  dbsnv += dbCount;
						}else if( type.equals("DNV")  && sampleNo == 0 ){
							dnv0 += count;  dbdnv0 += dbCount;
						}else if( type.equals("DNV")  && sampleNo > 0 ){
							dnv += count;  dbdnv += dbCount;
						}else if( type.equals("TNV")  && sampleNo == 0 ){
							tnv0 += count;  dbtnv0 += dbCount;
						}else if( type.equals("TNV")  && sampleNo > 0 ){
							tnv += count;  dbtnv += dbCount;
						}
					}		
				}								
				int infVcfs = Integer.parseInt( child.getChildNodes().item(j).getAttributes().getNamedItem("count").getNodeValue() );	
				assertTrue(infVcfs == ftVcfs);
				vcfs += infVcfs; 					
			} //end of eg. <ReportingCategory count="4" type="FORMAT:INF" value="SOMATIC">
			
				
			//svtype and dbsnp information should be same cross all sample
			if(sampleNo > 0){					
				assertTrue(snv0 == snv);
				assertTrue(dbsnv0 == dbsnv);
				assertTrue(dnv0 == dnv);
				assertTrue(dbdnv0 == dbdnv);
				assertTrue(tnv0 == tnv);
				assertTrue(dbtnv0 == dbtnv);
			}
			
			assertTrue(vcfs == 40);
		}
			
		//check sample 2 titv for 1/2
		child = nreport.getChildNodes().item(1).getChildNodes().item(1) ; //VCFMetrics::sample2			
		child = child.getFirstChild().getFirstChild().getFirstChild().getFirstChild();// <VariationType count="1" inDBSNP="1" type="SNV">			
		secondSampleTest( child );	
		
		//check sample 4 titv for GT=. and GT=0/0
		child = nreport.getChildNodes().item(1).getChildNodes().item(3) ; //VCFMetrics::sample4		
		fourthSampleTest( child );	
				
		file.delete();			
	}
	
	private void fourthSampleTest(Node node){
		//SOMATIC Other <VariationType count="1" inDBSNP="0" type="TNV">	
		Node child = node.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
		assertTrue(child.getAttributes().getNamedItem("type").getNodeValue().equals("TNV"));
		assertTrue(child.getChildNodes().getLength() == 1);
		child = child.getFirstChild().getFirstChild();
		assertTrue(child.getNodeName().equals("Genotype") );
		assertTrue(child.getAttributes().getNamedItem("type").getNodeValue().equals("1/2"));
		assertTrue(child.getAttributes().getNamedItem("count").getNodeValue().equals("10"));
		
		//GERMLINE PASS <VariationType count="1" inDBSNP="1" type="SNV">	
		child = node.getLastChild().getFirstChild().getFirstChild().getFirstChild();
		assertTrue(child.getAttributes().getNamedItem("type").getNodeValue().equals("SNV"));
		assertTrue(child.getChildNodes().getLength() == 2);
		child = child.getFirstChild().getFirstChild();
		assertTrue(child.getNodeName().equals("Genotype") );
		assertTrue(child.getAttributes().getNamedItem("type").getNodeValue().equals("0/0"));
		assertTrue(child.getAttributes().getNamedItem("count").getNodeValue().equals("10"));
		
		child = node.getLastChild().getFirstChild().getFirstChild().getFirstChild().getLastChild();
		assertTrue(child.getNodeName().equals("Substitutions") );
		assertTrue(child.getAttributes().getNamedItem("Transitions").getNodeValue().equals("0"));
		assertTrue(child.getAttributes().getNamedItem("Transversions").getNodeValue().equals("0"));
		
		//GERMLINE Other <VariationType count="1" inDBSNP="1" type="SNV">
		child = node.getLastChild().getLastChild().getFirstChild().getFirstChild();
		assertTrue(child.getAttributes().getNamedItem("type").getNodeValue().equals("SNV"));
		child = child.getFirstChild().getFirstChild();
		assertTrue(child.getNodeName().equals("Genotype") );
		assertTrue(child.getAttributes().getNamedItem("type").getNodeValue().equals("."));
		assertTrue(child.getAttributes().getNamedItem("count").getNodeValue().equals("10"));
		
		child = node.getLastChild().getLastChild().getFirstChild().getFirstChild().getLastChild();
		assertTrue(child.getNodeName().equals("Substitutions") );
		assertTrue(child.getAttributes().getNamedItem("Transitions").getNodeValue().equals("0"));
		assertTrue(child.getAttributes().getNamedItem("Transversions").getNodeValue().equals("0"));
		
		
	}
	
	private void secondSampleTest(Node node){
		Node child = node.getFirstChild();
			 
		assertTrue(child.getNodeName().equals("Genotypes"));
		assertTrue(child.getFirstChild().getNodeName().equals("Genotype"));
		assertTrue(child.getFirstChild().getAttributes().getNamedItem("type").getNodeValue().equals("1/2"));
		assertTrue(child.getFirstChild().getAttributes().getNamedItem("count").getNodeValue().equals("10"));
		
		child = node.getLastChild();
		assertTrue(child.getNodeName().equals("Substitutions"));
		assertTrue(child.getFirstChild().getNodeName().equals("Substitution"));
		assertTrue(child.getFirstChild().getAttributes().getNamedItem("change").getNodeValue().equals("G>A"));
		assertTrue(child.getFirstChild().getAttributes().getNamedItem("count").getNodeValue().equals("10"));
		assertTrue(child.getLastChild().getAttributes().getNamedItem("change").getNodeValue().equals("G>T"));
		assertTrue(child.getLastChild().getAttributes().getNamedItem("count").getNodeValue().equals("10"));		
	}
	
	private Node getXmlParentNode( File input )   {
				
		try (  VCFFileReader reader = new VCFFileReader(input ) ){
			
			VcfSummaryReport vcfSummaryReport; 
			VcfHeader header;			
			header = reader.getHeader();
			vcfSummaryReport = new VcfSummaryReport( header );		
			for (final VcfRecord vcf : reader)  	
				vcfSummaryReport.parseRecord( vcf);			
						
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();			 
			Element root = builder.getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
			vcfSummaryReport.toXml(root);
			
			Node nreport = root.getChildNodes().item(0);
			assertTrue(nreport.getNodeName().equals( ProfileType.VCF.getReportName()  + "Report"));
			return nreport;
			
		}catch ( ParserConfigurationException | IOException e ) {				 
			e.printStackTrace();
			return null;
		}
				
	}
	
	private static File createVcfFile() throws IOException{
	  final List<String> data = new ArrayList<String>();
	  data.add("##fileformat=VCFv4.2");	  
	  data.add("##INFO=<ID=IN,Number=.,Type=Integer,Description=\"Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file\">");
	  data.add("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");	
	  data.add("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");	
	  data.add("##FORMAT=<ID=FT,Number=.,Type=String,Description=\"Filters that apply to this sample\">");	
	  data.add("##FORMAT=<ID=INF,Number=.,Type=String,Description=\"Sample genotype information indicating if this genotype was 'called' (similar in concept to the INFO field). A semi-colon seperated list of information pertaining to this sample. Use ‘.’ to indicate the absence of information. These values should be described in the meta-information in the same way as INFOs\">");  	  
	  data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tcontrol1\ttest1\tcontrol2\ttest2");	
	  
	  for(int i = 0; i < 10; i++){
	  data.add("chr1\t10177\trs201752861\tA\tC\t.\t.\tIN=1\tGT:DP:FT:INF\t0/0:25:MIN:.\t0/1:97:5BP=3:SOMATIC;GERM=42,185\t.:.:.:.\t.:.:.:.");	
	  data.add("chr1\t80930980\trs7354844\tG\tT,A\t.\t.\tIN=1\tGT:AD:DP:FT:GQ:INF\t1/1:.:29:PASS:.:.\t1/2:.:64:PASS:.:SOMATIC\t0/2:0,18:18:SBIASALT:26:.\t0/0:.:64:PASS:.:.");
	  data.add("chr21\t10725791\t.\tGG\tAA,TA\t.\t.\tIN=1\tGT:DP:FT:INF\t0/1:153:PASS:.\t0/2:143:PASS:SOMATIC\t.:.:.:.\t.:.:.:.");	 
	  data.add("chr22\t48574793\t.\tCGC\tTAA,AAA\t.\t.\tIN=1,2\tGT:DP:FT:INF\t1/1:36:PASS:.\t1/2:66:SBIASALT:SOMATIC\t1/1:36:PASS:.\t1/2:66:SBIASALT:SOMATIC");	
	  }
	  try( BufferedWriter out = new BufferedWriter( new FileWriter(inputfile) ); ){      
	     for ( final String line : data )  out.write( line + "\n");
	  }   
					  
	  return  new File( inputfile );			  
	}
	
	
	private static File  createVcfHeader( ) throws IOException{
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
		 
		  try(BufferedWriter out = new BufferedWriter(new FileWriter(inputfile));){      
		     for (final String line : data)  out.write(line + "\n");
		  } 
		  
		 return  new File(inputfile);
	}	
	 

}
