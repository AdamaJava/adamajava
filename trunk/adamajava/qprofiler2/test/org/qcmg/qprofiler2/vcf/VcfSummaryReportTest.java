package org.qcmg.qprofiler2.vcf;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.junit.After;
import org.junit.Test;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.qprofiler2.QProfiler;
import org.qcmg.qprofiler2.summarise.SampleSummary;
import org.qcmg.qprofiler2.vcf.VcfSummaryReport;
import org.w3c.dom.*;
import java.io.*;

public class VcfSummaryReportTest {
//	@Rule
//	public  TemporaryFolder testFolder = new TemporaryFolder();	
	
	private final static String inputfile = "input.vcf";
	private final static String[] category = new String[] {"FT","INF"};
	
	@After
	public void cleanup(){		
		String dir = System.getProperty("user.dir");
		for(File f : new File(dir).listFiles( (d, name) -> name.startsWith("output.")  || name.startsWith("input.")) ) 
			f.delete();
	}
	
	@Test
	public void HeaderTest() throws Exception{
		File file =  new File( inputfile );
		createVcfHeader( file );	
		
		VcfHeader header;
		try (VCFFileReader reader = new VCFFileReader(file) ){ 	header = reader.getHeader(); }
		
		Node nreport = getXmlParentNode( file ) ;		
		Node child = nreport.getChildNodes().item(0);
		assertTrue( child.getNodeName().equals( VcfSummaryReport.NodeHeader) );
		assertTrue( child.getChildNodes().getLength() == 3 );
		
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
		assertTrue(child.getChildNodes().getLength() == 1 ); 
		child = child.getChildNodes().item(0);
		assertTrue( child.getNodeName().equals("#cdata-section") );	
		assertTrue( child.getNodeValue().equals( header.getChrom().toString()) );
		file.delete();		
	}	
  
	@Test
	public void MetricsTest() throws Exception{
		final String lastSample = "http://special";
		
		File file = new File("input.vcf");
		createVcfFile(lastSample, file);			
		Node nreport = getXmlParentNode( file ) ;
				 	
		Node child = nreport.getChildNodes().item(1);
		assertTrue(child.getNodeName().equals( VcfSummaryReport.NodeSummary));		 
		assertTrue(child.getChildNodes().getLength() == 4);
		
		//the common part of each sample level xml
		for(int sampleNo = 0; sampleNo < 4; sampleNo ++){
			child = nreport.getChildNodes().item(1).getChildNodes().item(sampleNo); 
			assertTrue( child.getNodeName().equals( VcfSummaryReport.Sample  ) );
			
			int counts = 0;
			for(int j = 0; j < child.getChildNodes().getLength(); j++){
				Node  node = child.getChildNodes().item(j);
				assertTrue( node.getNodeName().equals( VcfSummaryReport.NodeCategory) );
				assertTrue( node.getAttributes().getNamedItem("type").getNodeValue().equals("FORMAT:" + category[0]) );
				counts += Integer.parseInt(node.getAttributes().getNamedItem("count").getNodeValue() );
				
				for(int k = 0; k < node.getChildNodes().getLength(); k ++){
					Node cnode = node.getChildNodes().item(k);
					assertTrue( cnode.getNodeName().equals( VcfSummaryReport.NodeCategory) );
					assertTrue( cnode.getAttributes().getNamedItem("type").getNodeValue().equals("FORMAT:" + category[1]) );
					counts += Integer.parseInt(cnode.getAttributes().getNamedItem("count").getNodeValue() );
					
					NodeList nlists = cnode.getFirstChild().getChildNodes(); //VariationType
					for(int kk = 0; kk < nlists.getLength(); kk ++){
						cnode = nlists.item(kk);
						assertEquals(cnode.getNodeName(), SampleSummary.variantType);
						assertEquals(cnode.getFirstChild().getNodeName(), SampleSummary.genotypes);
						if( cnode.getAttributes().getNamedItem("type").getNodeValue().equals(SVTYPE.SNP.toVariantType() ) ){
							assertSame(cnode.getChildNodes().getLength(), 3);
							assertEquals(cnode.getChildNodes().item(2).getNodeName(), SampleSummary.substitutions);
						}else
							assertSame(cnode.getChildNodes().getLength(), 2); // now conatin VariantAlleleFrequency
					} 					
				}				
			}
			assertTrue(counts == 40*2);
			
			String sample = child.getAttributes().getNamedItem("value").getNodeValue();
			if(sample.equals(lastSample) || sample.equals("control2"))  checkLastSampleColumn(child);	
			else checkSNV(child);
		}
						
		file.delete();			
	}
	
	@Test
	public void SampleWithSpecial() {
				
		try {
			//sample contain special letter same to the seperator
			String lastSample = "http" + VcfSummaryReport.Seperator + "last";				
			File file = new File("input.vcf");
			createVcfFile(lastSample, file);	
			
			QProfiler qp = new QProfiler();
			int exitStatus = qp.setup("--nohtml --input input.vcf --output output.xml --log output.log".split(" "));
			assertTrue(1 == exitStatus);
			
			//normal sample name
			lastSample = lastSample.replace(VcfSummaryReport.Seperator, "");
			createVcfFile(lastSample, file);			
			qp = new QProfiler();
			exitStatus = qp.setup("--nohtml --input input.vcf --output output.xml --log output.log".split(" "));
			assertTrue(0 == exitStatus);
		} catch (Exception e) {
			fail("throw unexpected Exception");
		}			
				
	}
	
	private Node getXmlParentNode( File input ){
				
		try (  VCFFileReader reader = new VCFFileReader(input ) ){			 
			VcfSummaryReport vcfSummaryReport  = new VcfSummaryReport( reader.getHeader(), category );					 		
			for (final VcfRecord vcf : reader) vcfSummaryReport.parseRecord( vcf);			
						
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();			 
			Element root = builder.getDOMImplementation().createDocument(null, "qProfiler", null).getDocumentElement();
			vcfSummaryReport.toXml(root);
						
			Node nreport = root.getChildNodes().item(0);
			assertTrue(nreport.getNodeName().equals( ProfileType.VCF  + "Report"));
			
			//QprofilerXmlUtils.asXmlText(root, "/Users/christix/Documents/Eclipse/data/qprofiler/vcf/test.xml");			
			return nreport;
			
		}catch (Exception e ) {				 
			e.printStackTrace();
			return null;
		}
				
	}
	
	//also used by cohorSaummryReportTest
	/**
	 * 
	 * @param lastSampleName
	 * @param inputfile
	 * @throws IOException
	 */
	public static void createVcfFile(String lastSampleName, File inputfile) throws IOException{
	  final List<String> data = new ArrayList<String>();
	  data.add("##fileformat=VCFv4.2");	
	  data.add("##SpecialChar= < & \" \t > ' ");	
	  data.add("##INFO=<ID=IN,Number=.,Type=Integer,Description=\"Indicates which INput file this vcf record came from. Multiple values are allowed which indicate that the record has been merged from more than 1 input file\">");
	  data.add("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");	
	  data.add("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");	
	  data.add("##FORMAT=<ID=FT,Number=.,Type=String,Description=\"Filters that apply to this sample\">");	
	  data.add("##FORMAT=<ID=INF,Number=.,Type=String,Description=\"Sample genotype information indicating if this genotype was 'called' (similar in concept to the INFO field). A semi-colon seperated list of information pertaining to this sample. Use ‘.’ to indicate the absence of information. These values should be described in the meta-information in the same way as INFOs\">");  	  	  
	  data.add("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\tcontrol1\ttest1\tcontrol2\t"+ lastSampleName);	
	  
	  for(int i = 0; i < 10; i++){	
		  data.add("chr1\t10177\trs201752861\tA\tC\t.\t.\tIN=1\tGT:DP:FT:INF\t0/0:25:PASS:.\t0/1:97:5BP=3:SOMATIC;GERM=42,185\t.:.:.:.\t.:.:.:.");	
		  data.add("chr1\t80930980\trs7354844\tG\tT,A\t.\t.\tIN=1\tGT:AD:DP:FT:GQ:INF\t1/1:.:29:PASS:.:.\t1/2:.:64:PASS:.:SOMATIC\t.:.:.:.\t.:.:.:.");
		  data.add("chr21\t10725791\t.\tGG\tAA,TA\t.\t.\tIN=1\tGT:DP:FT:INF\t0/1:153:PASS:.\t0/2:143:PASS:SOMATIC\t.:.:.:.\t.:.:.:.");	 
		  data.add("chr22\t48574793\t.\tCGC\tTAA,AAA\t.\t.\tIN=1,2\tGT:DP:FT:INF\t1/1:36:PASS:.\t1/2:66:5BP=3:SOMATIC\t.:.:.:.\t.:.:.:.");	
	  }
	  try( BufferedWriter out = new BufferedWriter( new FileWriter(inputfile) ); ){      
	     for ( final String line : data )  out.write( line + "\n");
	  }   
					  
	 // return  new File( inputfile );			  
	}
	/**
	 * 	add header information into this file without appending. 
	 * @param inputfile: 
	 * @throws IOException
	 */
	private static void  createVcfHeader( File inputfile ) throws IOException{
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
		  
//		 return  new File(inputfile);
	}	
	 
	private void checkLastSampleColumn(Node node){
		
		Node cnode = node.getFirstChild(); // FORMAT:FT		 
		for(int i = 0; i < 2; i ++){
			assertTrue( cnode.getChildNodes().getLength() == 1 );  //single child until <report>
			assertEquals( cnode.getAttributes().getNamedItem("value").getNodeValue(),"." );
			assertEquals( cnode.getAttributes().getNamedItem("count").getNodeValue(),"40" );			
			cnode = cnode.getFirstChild(); // FORMAT:INF then <Report>
		}
		
		assertSame(cnode.getChildNodes().getLength(),3); //<Report>		
		for(int i = 0; i < 3; i ++){
			Node varNode = cnode.getChildNodes().item(i);
			Node genNode = varNode.getFirstChild().getFirstChild();
			assertEquals( genNode.getAttributes().getNamedItem("type").getNodeValue(), "." );
			if(varNode.getAttributes().getNamedItem("type").getNodeValue().equals(SVTYPE.SNP.toVariantType() ) ){
				assertEquals(varNode.getAttributes().getNamedItem("count").getNodeValue() , "20" );
				assertEquals(varNode.getAttributes().getNamedItem("inDBSNP").getNodeValue() , "20" );
				assertEquals(genNode.getAttributes().getNamedItem("count").getNodeValue(), "20" );
				assertEquals( varNode.getChildNodes().item(2).getNodeName(), SampleSummary.substitutions  );
				assertEquals( varNode.getChildNodes().item(2).getFirstChild().getFirstChild(), null );
				
			}else{
				assertEquals(varNode.getAttributes().getNamedItem("count").getNodeValue() , "10" );
				assertEquals(varNode.getAttributes().getNamedItem("inDBSNP").getNodeValue() , "0" );
				assertEquals(genNode.getAttributes().getNamedItem("count").getNodeValue(), "10" );
				String type = varNode.getAttributes().getNamedItem("type").getNodeValue();
				assertTrue( type.equals(SVTYPE.TNP.toVariantType())  || type.equals(SVTYPE.DNP.toVariantType())  );	
			}			
		}
		
	}

	private void checkSNV(Node sampleNode){
		
		String sample = sampleNode.getAttributes().getNamedItem("value").getNodeValue();
	
		//get <VariationType count="10" inDBSNP="10" type="SNV">
		Node svnNode = null;
		for(int i = 0; i < sampleNode.getChildNodes().getLength(); i ++){
			String str1 = sampleNode.getChildNodes().item(i).getAttributes().getNamedItem("value").getNodeValue();
			String str2 = sampleNode.getChildNodes().item(i).getAttributes().getNamedItem("type").getNodeValue();
			
			if(str1.equals("PASS") && str2.equals("FORMAT:FT")){
				Node node = sampleNode.getChildNodes().item(i).getFirstChild().getFirstChild();
				for(int ii = 0; ii < node.getChildNodes().getLength(); ii++) 
					if(node.getChildNodes().item(ii).getAttributes().getNamedItem("type")
							.getNodeValue().equals(SVTYPE.SNP.toVariantType() )){
						svnNode = node.getChildNodes().item(ii);
						break; //break ii loop
					}			
			}
			if(svnNode != null) break; // break i loop 
		}
		
		String str = svnNode.getParentNode().getParentNode().getAttributes().getNamedItem("type").getNodeValue();
		assertEquals( str, "FORMAT:INF" );
		str = svnNode.getParentNode().getParentNode().getAttributes().getNamedItem("value").getNodeValue();
		if(sample.equals("test1"))  assertEquals( str, "SOMATIC");
		else assertTrue( str.equals(".")     );				 
		
		for( int i = 0; i < svnNode.getChildNodes().getLength(); i++ ){
			Node node = svnNode.getChildNodes().item(i);
			//<Genotype count="10" type="1/2"/>
			if(node.getNodeName().equals(SampleSummary.genotypes))
				for(int ii = 0; ii < node.getChildNodes().getLength(); ii ++){
					assertEquals(node.getChildNodes().item(ii).getNodeName(), SampleSummary.genotype) ;
					str = node.getChildNodes().item(ii).getAttributes().getNamedItem("type").getNodeValue();					
					if(sample.equals("test1"))  assertEquals( str, "1/2");
					else assertTrue( str.equals("0/0")  || str.equals("1/1")   );				 
				}
			else if(node.getNodeName().equals(SampleSummary.substitutions)){
				//node =  QprofilerXmlUtils.getChildElement((Element)node, "SNP_uniqAlt", 0) //now  got child SNP_uniqAlt
				node = node.getChildNodes().item(0);								
				if(sample.equals("test1")){
					assertEquals(node.getAttributes().getNamedItem("TiTvRatio").getNodeValue(), "1.00");
					assertEquals(node.getAttributes().getNamedItem("Transitions").getNodeValue(), "10");
					assertEquals(node.getAttributes().getNamedItem("Transversions").getNodeValue(), "10");					
				}else{
					//sample: control1
					assertEquals(node.getAttributes().getNamedItem("TiTvRatio").getNodeValue(), "0.00");
					assertEquals(node.getAttributes().getNamedItem("Transitions").getNodeValue(), "0");
					assertEquals(node.getAttributes().getNamedItem("Transversions").getNodeValue(), "10");					
				}
				
				for(int ii = 0; ii < node.getChildNodes().getLength(); ii ++){
					str = node.getChildNodes().item(ii).getAttributes().getNamedItem("change").getNodeValue();
					if(sample.equals("control1"))  assertEquals( str, "G>T");
					else assertTrue( str.equals("G>A")  || str.equals("G>T")   );				 
				}				
			}			
		}				
	}
	
	@Test
	public void xuTest(){
		String xml = "<resp><status>good</status><msg>hi</msg></resp>";
		String path = "/resp/status";		
		
        double[] a = {2.3,4,3,6,8,9};
        double[] b = {3,6,6,8.4,7,10};
        PearsonsCorrelation correlation = new PearsonsCorrelation();
        double r2 = correlation.correlation(a, b);
        System.out.println(r2);
        
 /*       
        > a<-read.csv("/Users/christix/aa.tsv",sep="\t");
        > plot(a)
        */
	}
	
}
