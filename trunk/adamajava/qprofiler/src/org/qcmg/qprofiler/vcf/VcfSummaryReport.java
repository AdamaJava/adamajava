package org.qcmg.qprofiler.vcf;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.summarise.SampleSummary;
import org.w3c.dom.Element;

public class VcfSummaryReport  extends SummaryReport {
	
	static final String NodeHeader = "VCFHeader";
	static final String NodeHeaderMeta = "MetaInformation" ;
	static final String NodeHeaderMetaLine = "MetaInformationLine";
	static final String NodeHeaderStructured = "StructuredMetaInformation";
	static final String NodeHeaderStructuredType = "StructuredMetaInformationType";
	static final String NodeHeaderStructuredLine = "StructuredMetaInformationLine";
	static final String NodeHeaderFinal =  "HeaderLine";
	
	
	static final String NodeSummary = "VCFMetrics";
	public static final String NodeCategory = "ReportingCategory";
	
	//private  AtomicLong count = new  AtomicLong(); 
	private final VcfHeader vcfHeader;
	
	private final String[] sampleIds; 
	private final SampleSummary[] somaticSummaries;
	private final SampleSummary[] germlineSummaries;
	
	public VcfSummaryReport(VcfHeader header){
	     
		this.vcfHeader = header;

		//sample id
		this.sampleIds = header.getSampleId(); 	
		somaticSummaries = new SampleSummary[sampleIds.length];
		germlineSummaries = new SampleSummary[sampleIds.length];
		
		for(int i = 0; i < sampleIds.length; i ++){  
			somaticSummaries[i]  = new SampleSummary( "SOMATIC" );
			germlineSummaries[i]  = new SampleSummary("GERMLINE" );
		}
		
	}
	
	@Override
	public void toXml(Element parent) {
		Element parentElement = init(parent, ProfileType.VCF);				
		vcfHeaderToXml(parentElement);  //Vcf header		
		summaryToXml( parentElement  );		
	}
	
	void parseRecord( VcfRecord  vcf) {
		updateRecordsParsed();
		
		List<String> formats = vcf.getFormatFields();
		if(formats.size() != sampleIds.length + 1)
			logger.warn("missing/redundant sample column exists in vcf record: " + vcf.toSimpleString());
				
		for(int i = 1; i < formats.size(); i ++){
			VcfFormatFieldRecord re = new VcfFormatFieldRecord(formats.get(0), formats.get(i));
			if((re.getField("INF") != null) && re.getField("INF").contains("SOMATIC"))
				somaticSummaries[i-1].parseRecord(vcf, re);
			else
				germlineSummaries[i-1].parseRecord(vcf, re);			
		}		
		
	}

	void summaryToXml(Element parent){
		Element summaryElement = createSubElement(parent, NodeSummary);
		
		for(int i = 0; i < sampleIds.length; i ++){
			Element sampleE = createSubElement(summaryElement, "Sample");
			sampleE.setAttribute("value", sampleIds[i]);
			
			if( somaticSummaries[i].getCounts() > 0 ){			
				Element somaticE = createSubElement(sampleE, NodeCategory);
				somaticSummaries[i].toXML(somaticE);
			}
			
			if( germlineSummaries[i].getCounts() > 0 ){
				Element germline = createSubElement(sampleE, NodeCategory);
				germlineSummaries[i].toXML(germline);
			}
		}		
	}
	
	
	void vcfHeaderToXml(Element parent){
		Element headerElement = createSubElement( parent, NodeHeader);
		Element metaElement = createSubElement( headerElement, NodeHeaderMeta);
		Element structuredElement = createSubElement( headerElement, NodeHeaderStructured);
		
		HashMap<String, Element> struElements = new HashMap<String, Element>();
				
		for(final VcfHeaderRecord record: vcfHeader ){			
			String key = record.getMetaKey().replace("##", "");
			if(key.startsWith( VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE )){
				Element ele =  createSubElement( headerElement, NodeHeaderFinal );
				ele.setAttribute("columns", key.replace("\t", " "));				
			}
			else if(record.getId() == null ){
				Element ele =  createSubElement( metaElement, NodeHeaderMetaLine );
				ele.setAttribute("key", record.getMetaKey().replace("##", "") );
				String v = record.getMetaValue();
				 if(v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length()-1).trim();
				  				
				 ele.setAttribute("value", v);				
			}else{				
				 if(!struElements.containsKey(key)){
					 Element ele = createSubElement( structuredElement, NodeHeaderStructuredType );
					 ele.setAttribute("type", key);
					 struElements.put(key, ele );
				 }
				 Element ele = struElements.get(key);
				 ele =  createSubElement( ele, NodeHeaderStructuredLine );
				 //ele.setAttribute("id", record.getId());
				 for( Pair<String, String> p: record.getSubFields()) {
					 String v = p.getRight().trim();
					 if( v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length()-1).trim();
					 
					 ele.setAttribute(p.getLeft(), v);	
					  
				 }		
			} 
			
		}  
				
	}


}
