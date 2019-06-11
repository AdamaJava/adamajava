package org.qcmg.qprofiler2.vcf;

import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.qprofiler2.QProfiler2;
import org.qcmg.qprofiler2.Summarizer;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.summarise.SampleSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.qcmg.qprofiler2.vcf.VcfSummaryReport;
import org.w3c.dom.*;
import java.io.*;

public class VcfSummaryReportTest {
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();	
	
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
		assertTrue( child.getNodeName().equals( "vcfHeader") );
		
		int[] mark = { 0, 0, 0, 0, 0, 0 };
		for( int i = 0; i < 6; i++ ) {
			Element headersEle =  (Element) child.getChildNodes().item(i);
			assertEquals( "headerRecords" , headersEle .getNodeName() );
			
			//check meta Information line
			if( headersEle.getAttribute( "FIELD" ).equals( "MetaInformation" ) ) {
				mark[1] = 1;
				for(int j = 0; j < 12; j ++) {
					Element recordEle = (Element) headersEle.getChildNodes().item(j);
					assertEquals( "record" , recordEle .getNodeName() );
					String key = recordEle.getAttribute(XmlUtils.NAME);
					if(header.getRecords(key).size() == 1)
						assertEquals( recordEle.getTextContent() , header.getRecords(key).get(0).toString() );
					else
						assertEquals( "INPUT" , key );
				}
			}
			
			//check structured meta information line	 
			else if( headersEle.getAttribute( "FIELD" ).equals( "qPG" ) ) {
				mark[2] = 1;
				for( Element ele : XmlElementUtils.getChildElementByTagName( headersEle, "record") )
					assertEquals( ele.getTextContent(), header.getIDRecord("qPG", ele.getAttribute(XmlUtils.NAME)).toString() );					
			}else if( headersEle.getAttribute( "FIELD" ).equals( "FILTER" ) ) {
				mark[3] = 1;
				assertEquals( 1, headersEle.getChildNodes().getLength() );
			}else if( headersEle.getAttribute( "FIELD" ).equals( "FORMAT" ) ) {
				mark[4] = 1;
				assertEquals( 2, headersEle.getChildNodes().getLength() );
			}else if( headersEle.getAttribute( "FIELD" ).equals( "INFO" ) ) {
				mark[5] = 1;
				assertEquals( 1, headersEle.getChildNodes().getLength() );
			}else {
				mark[0] = 1;
				assertEquals( "headerline", headersEle.getAttribute( "FIELD" ));
				assertEquals( header.getChrom().toString(), headersEle.getChildNodes().item(0).getTextContent());
			}			
		}
			 
		assertTrue(IntStream.of(mark).sum() == 6);
		
		file.delete();		
	}	
  
	@Test
	public void MetricsTest() throws Exception{
		final String lastSample = "http://special";
		
		File file = new File("input.vcf");
		createVcfFile(lastSample, file);			
		Node nreport = getXmlParentNode( file ) ;
				 	
		Node child = nreport.getChildNodes().item(1);
		assertTrue(child.getNodeName().equals( ProfileType.VCF.getReportName()+"Metrics" ));		 
		assertTrue(child.getChildNodes().getLength() == 4);
		
		//the common part of each sample level xml
		for(int sampleNo = 0; sampleNo < 4; sampleNo ++){
			child = nreport.getChildNodes().item(1).getChildNodes().item(sampleNo); 
			assertTrue( child.getNodeName().equals( VcfSummaryReport.Sample  ) );
						
			//check each report
			for(int j = 0; j < child.getChildNodes().getLength(); j++){
				Node  node = child.getChildNodes().item(j); //report node
				assertEquals("report", node.getNodeName() );	
				
				assertEquals( StringUtils.join( category, Constants.COLON ), node.getAttributes().getNamedItem("formats").getNodeValue()  );	
				//only allow one sequenceMetric under report
				assertTrue(node.getChildNodes().getLength() > 0 );				
				Element cnode = (Element) node.getChildNodes().item(0);
				assertEquals(XmlUtils.SEQUENCE_METRICS, cnode.getNodeName() );
				if(cnode.getAttribute(XmlUtils.NAME).equals(SVTYPE.SNP.toVariantType()))  
					assertEquals( 2, XmlElementUtils.getChildElementByTagName(cnode, XmlUtils.VALUE).size() );					
				 else  
					assertEquals( 1,  XmlElementUtils.getChildElementByTagName(cnode, XmlUtils.VALUE).size() );				
			}
			List<Element> eles = new ArrayList<>();		
			
 			XmlElementUtils.getOffspringElementByTagName((Element)child, XmlUtils.VARIABLE_GROUP).stream() 
				.filter( e -> e.getAttribute( XmlUtils.NAME ).equals( SampleSummary.genotype ))
				.forEach( e1 ->     eles.addAll(XmlElementUtils.getChildElementByTagName(e1, XmlUtils.TALLY)) );
					
			List<Integer> counts = new ArrayList<>();
			
			eles.stream().forEach(e -> 	counts.add(Integer.parseInt(e.getAttribute(XmlUtils.COUNT))) );			 			
		    assertEquals( 40, counts.stream().mapToInt(i -> i.intValue()).sum() );
						
			// assertTrue( counts == 40*2 );			
			String sample = child.getAttributes().getNamedItem( XmlUtils.NAME).getNodeValue();
			if( sample.equals(lastSample) || sample.equals( "control2" ) )  checkLastSampleColumn(child);	
			else if( sample.equals("test1"))
					checkTest1(child);
			else
				checkControl1(child);			
				 
		}
						
		file.delete();			
	}
	
	private void checkControl1(Node child) {
		
		/** 
		 * 0/0:20,5:25:PASS:.
		 * 1/1:.:29:PASS:.:.
		 * 0/1:153:PASS:.
		 * 1/1:5,30,1:36:PASS:.
		 */
				
		assertEquals( 1,  XmlElementUtils.getChildElementByTagName( (Element) child, "report").size() );
		Element ele = XmlElementUtils.getChildElementByTagName( (Element) child, "report").get(0);
		assertEquals( "PASS:." , ele.getAttribute("values"));
				
		//check genotype
		long no = XmlElementUtils.getOffspringElementByTagName( ele,  XmlUtils.TALLY ).stream()
			.filter( e -> e.getAttribute(XmlUtils.VALUE).equals("0/0") && e.getAttribute(XmlUtils.COUNT).equals("10")).count();		
		assertEquals( 1 , no );
		no = XmlElementUtils.getOffspringElementByTagName( ele,  XmlUtils.TALLY ).stream()
				.filter( e -> e.getAttribute(XmlUtils.VALUE).equals("1/1") && e.getAttribute(XmlUtils.COUNT).equals("10")).count();	
		assertEquals( 2 , no );
		no = XmlElementUtils.getOffspringElementByTagName( ele,  XmlUtils.TALLY ).stream()
				.filter( e -> e.getAttribute(XmlUtils.VALUE).equals("0/1") && e.getAttribute(XmlUtils.COUNT).equals("10")).count();	
		assertEquals( 1 , no );
		
		// ariantAltFrequencyPercent only from 1/1:5,30,1:36:PASS:.
		// 31/36 = 86%
		assertEquals( 1 , XmlElementUtils.getOffspringElementByTagName( ele,  XmlUtils.COLSED_BIN ).size() );
		no = XmlElementUtils.getOffspringElementByTagName( ele,  XmlUtils.COLSED_BIN ).stream()
				.filter( e -> e.getAttribute(XmlUtils.START).equals("85")  && e.getAttribute(XmlUtils.COUNT).equals("10")).count();			
//				.filter( e -> e.getAttribute(XmlUtils.Sstart).equals("85") && e.getAttribute(XmlUtils.Send).equals("90") && e.getAttribute(XmlUtils.Scount).equals("10")).count();			
		assertEquals( 1 , no );
				
	}

	/**
	 * 0/1:49,48:97:5BP=3:SOMATIC;GERM=42,185
	 * 1/2:.:64:PASS:.:SOMATIC (FT:GQ:INF )
	 * 0/2:143:PASS:SOMATIC
	 * 1/2:1,32,32:66:5BP=3:SOMATIC
	 */
	private void checkTest1(Node child) {
		
		//check FT:INF
		for(String value : new String[] {"5BP=3:SOMATIC;GERM=42,185",  "PASS:SOMATIC", "5BP=3:SOMATIC"  })
		assertEquals( 1, XmlElementUtils.getOffspringElementByTagName( (Element) child,  "report" ).stream()
				.filter( e -> e.getAttribute("values").equals(value) ).count() );		
		//check genotype
		long no = XmlElementUtils.getOffspringElementByTagName( (Element) child,  XmlUtils.TALLY ).stream()
			.filter( e -> e.getAttribute(XmlUtils.VALUE).equals("0/1") && e.getAttribute(XmlUtils.COUNT).equals("10")).count();		
		assertEquals( 1 , no );
		no = XmlElementUtils.getOffspringElementByTagName( (Element) child,  XmlUtils.TALLY ).stream()
				.filter( e -> e.getAttribute(XmlUtils.VALUE).equals("1/2") && e.getAttribute(XmlUtils.COUNT).equals("10")).count();	
		assertEquals( 2 , no );
		no = XmlElementUtils.getOffspringElementByTagName( (Element) child,  XmlUtils.TALLY ).stream()
				.filter( e -> e.getAttribute(XmlUtils.VALUE).equals("0/2") && e.getAttribute(XmlUtils.COUNT).equals("10")).count();	
		assertEquals( 1 , no );
								
		//titv will be done on sampleSummayTest		
		//ariantAltFrequencyPercent
		assertEquals( 2 , XmlElementUtils.getOffspringElementByTagName( (Element) child,  XmlUtils.COLSED_BIN ).size() );
		no = XmlElementUtils.getOffspringElementByTagName( (Element) child,  XmlUtils.COLSED_BIN ).stream()
				.filter( e -> e.getAttribute(XmlUtils.START).equals("45")  && e.getAttribute(XmlUtils.COUNT).equals("10")).count();			
		assertEquals( 1 , no );	
		no = XmlElementUtils.getOffspringElementByTagName( (Element) child,  XmlUtils.COLSED_BIN ).stream()
				.filter( e -> e.getAttribute(XmlUtils.START).equals("95")  && e.getAttribute(XmlUtils.COUNT).equals("10")).count();			
		assertEquals( 1 , no );			
	}

	@Test
	/**
	 * it can deal with special char in sample name since updated qprofiler2 use Map<Ssample, Map<categor,SampleSummary>>
	 * @throws IOException
	 */
	public void SampleWithSpecial() throws IOException {
		 
			//sample contain special letter same to the seperator
			final String lastSample = "http" + Constants.COMMA + "last";				
			File file = new File("input.vcf");
			createVcfFile(lastSample, file);				
			try {
				Summarizer summarizer  = new VcfSummarizer( new String[] {}  );
				SummaryReport sr = summarizer.summarize("input.vcf", null);				
				Element root = XmlElementUtils.createRootElement("qProfiler", null);					
				sr.toXml(root);				
				List<Element> samples = XmlElementUtils.getOffspringElementByTagName(root, "sample");
				assertEquals(4, samples.size());
				assertEquals(1, samples.stream().filter( e -> e.getAttribute(XmlUtils.NAME).equals(lastSample)).count()) ;	
				assertEquals(4, XmlElementUtils.getOffspringElementByTagName(root, "report").size());
				XmlElementUtils.getOffspringElementByTagName(root, "report").forEach(e -> assertEquals( 0, e.getAttributes().getLength()) );				 
			} catch (Exception e) { fail("unexpected error"); }
			
			
			try {
				Summarizer summarizer  = new VcfSummarizer(new String[] {"FT","INF"} );
				SummaryReport sr = summarizer.summarize("input.vcf", null);				
				Element root = XmlElementUtils.createRootElement("qProfiler", null);					
				sr.toXml(root);				
				List<Element> samples = XmlElementUtils.getOffspringElementByTagName(root, "sample");
				assertEquals(4, samples.size());
				assertEquals(1, samples.stream().filter( e -> e.getAttribute(XmlUtils.NAME).equals(lastSample)).count()) ;	
				assertEquals(6, XmlElementUtils.getOffspringElementByTagName(root, "report").size());				
				Element child =  samples.stream().filter( e -> e.getAttribute(XmlUtils.NAME).equals("test1")).findFirst().get();
				checkTest1(child);							 
			} catch (Exception e) { fail("unexpected error"); }
			
			
			 
			try {
				
				QProfiler2 qp = new QProfiler2();
				createVcfFile( lastSample.replace(Constants.COMMA_STRING, ""), file);				
				int exitStatus = qp.setup( "--input input.vcf --output output.xml --log output.log".split(" ") );
				assertTrue(0 == exitStatus);
			} catch (Exception e) {
				fail("unexpected error");
			}
			
	}
	
	private Node getXmlParentNode( File input ){
				
		try (  VCFFileReader reader = new VCFFileReader(input ) ){			 
			VcfSummaryReport vcfSummaryReport  = new VcfSummaryReport( reader.getHeader(), category );					 		
			for (final VcfRecord vcf : reader) vcfSummaryReport.parseRecord( vcf);			
								 
			Element root = XmlElementUtils.createRootElement("qProfiler", null);					
			vcfSummaryReport.toXml(root);
									
			Node nreport = root.getChildNodes().item(0);
			assertTrue(nreport.getNodeName().equals( ProfileType.VCF.getReportName()  + "Report"));
			
			return nreport;			
		}catch (Exception e ) {				 
			e.printStackTrace();
			fail("unexpected error during unit test");
			return null; 
		}
				
	}
	
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
		  data.add("chr1\t10177\trs201752861\tA\tC\t.\t.\tIN=1\tGT:AD:DP:FT:INF\t0/0:20,5:25:PASS:.\t0/1:49,48:97:5BP=3:SOMATIC;GERM=42,185\t.:.:.:.\t.:.:.:.");	
		  data.add("chr1\t80930980\trs7354844\tG\tT,A\t.\t.\tIN=1\tGT:AD:DP:FT:GQ:INF\t1/1:.:29:PASS:.:.\t1/2:.:64:PASS:.:SOMATIC\t.:.:.:.\t.:.:.:.");
		  data.add("chr21\t10725791\t.\tGG\tAA,TA\t.\t.\tIN=1\tGT:DP:FT:INF\t0/1:153:PASS:.\t0/2:143:PASS:SOMATIC\t.:.:.:.\t.:.:.:.");	 
		  data.add("chr22\t48574793\t.\tCGC\tTAA,AAA\t.\t.\tIN=1,2\tGT:AD:DP:FT:INF\t1/1:5,30,1:36:PASS:.\t1/2:1,32,32:66:5BP=3:SOMATIC\t.:.:.:.\t.:.:.:.");			  
	  }
	  
	  try( BufferedWriter out = new BufferedWriter( new FileWriter(inputfile) ); ){  
	     for ( final String line : data )  out.write( line + "\n");
	  }   
					  		  
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
		  
	}	
	 
	private void checkLastSampleColumn(Node node){
		
		assertEquals(1, node.getChildNodes().getLength()); //only one <report>
		
		Element ele = (Element) node.getFirstChild();
		assertEquals( "FT:INF", ele.getAttribute("formats") );
		assertEquals( ".:.", ele.getAttribute("values") );
		assertSame(ele.getChildNodes().getLength(),3); // <sequenceMetrics>	
		
		//check SNV, there is no gt so no titv
		Element snvE = (Element) ele.getFirstChild();		
		for( Element e : XmlElementUtils.getChildElementByTagName(snvE, XmlUtils.VALUE) )
			if(e.getAttribute(XmlUtils.NAME).equals("inDBSNP"))  
				assertEquals("20", e.getTextContent() );  //dbsnp always same for all sample
			else if(e.getAttribute(XmlUtils.NAME).equals("TiTvRatio"))  
				assertEquals("0.00", e.getTextContent() );
			 					
		for(int i = 0; i < 3; i ++){
			Element e = (Element) ele.getChildNodes().item(i);
			assertEquals(XmlUtils.SEQUENCE_METRICS, e.getNodeName() );						
			assertEquals(1, XmlElementUtils.getChildElementByTagName( e, XmlUtils.VARIABLE_GROUP ).size());
			Element e1 = XmlElementUtils.getChildElementByTagName( e, XmlUtils.VARIABLE_GROUP ).get(0);
			assertEquals(1, XmlElementUtils.getChildElementByTagName(e1, XmlUtils.TALLY).size());
			e1 = XmlElementUtils.getChildElement(e1,  XmlUtils.TALLY, 0);
			assertEquals(".", e1.getAttribute(XmlUtils.VALUE));
			
			if( e.getAttribute( XmlUtils.NAME).equals(SVTYPE.SNP.toVariantType() )  ) 				
				assertEquals("20",  e1.getAttribute( XmlUtils.COUNT));
			else 				
				assertEquals("10",  e1.getAttribute( XmlUtils.COUNT));
			
		}
		
	}

	
}
