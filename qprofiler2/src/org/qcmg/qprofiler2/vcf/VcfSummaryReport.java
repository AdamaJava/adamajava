package org.qcmg.qprofiler2.vcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.tuple.Pair;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.summarise.SampleSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;


public class VcfSummaryReport  extends SummaryReport {
	
	public static final String NodeHeader = "vcfHeader";
	public static final String NodeHeaderMeta = "MetaInformation" ;
	public static final String NodeHeaderMetaLine = "MetaInformationLine";
	public static final String NodeHeaderStructured = "StructuredMetaInformation";
	public static final String NodeHeaderStructuredType = "StructuredMetaInformationType";
	public static final String NodeHeaderStructuredLine = "StructuredMetaInformationLine";
	public static final String NodeHeaderFinal =  "HeaderLine";		
	public static final String NodeSummary = "vcfMetrics";
	public static final String NodeCategory = "ReportingCategory";
	public static final String Sample = "Sample";	
	public static final String id = "id";
	
	static final String Seperator = ",:,";
	
	private final VcfHeader vcfHeader;	
	private final String[] sampleIds; 
 
	//it allows the format field value eg. --formart FT=PASS, then it seperate value to PASS the others
	private final String[] formatCategories;
	Map< String, SampleSummary > summaries = new HashMap<>();
	Map< String, AtomicLong > counts = new HashMap<>();
	
	public VcfSummaryReport(VcfHeader header, String[] formats){	     
		this.vcfHeader = header;
		this.formatCategories = formats; 
		this.sampleIds = header.getSampleId(); 	
	}
	
	public void toXml(Element parent) {
		logger.info("preparing output...");
		Element parentElement = init(parent, ProfileType.VCF);	
		logger.info("outputing vcf header to xml...");
		//vcfHeaderToXml(parentElement);  //Vcf header	
		XmlUtils.vcfHeaderToXml(parentElement, vcfHeader);
		logger.info("outputing sample information to xml...");
		summaryToXml( parentElement  );		
	}
	
	void parseRecord( VcfRecord  vcf ) {
		updateRecordsParsed();
		
		List<String> formats = vcf.getFormatFields();
		if(sampleIds == null || formats.size() != sampleIds.length + 1)
			logger.warn("missing/redundant sample column exists in vcf record: " + vcf.toSimpleString());
		
		//for each sample column
		for(int i = 1; i < formats.size(); i ++){
			String key = sampleIds[i-1]; 
			VcfFormatFieldRecord re = new VcfFormatFieldRecord(formats.get(0), formats.get(i));
			for(String cate : formatCategories ){
				//new
				int pos = cate.indexOf("="); 
				if(pos > 0){
					String formatKey = cate.substring(0, pos).trim();
					String formatValue = cate.substring(pos+1).trim();
					key += Seperator +  ( re.getField(formatKey) == null ? null :
							  re.getField(formatKey).equalsIgnoreCase(formatValue) ? formatValue : "Other" );
					//key += Seperator +  ( re.getField(formatKey) != null && re.getField(formatKey).equalsIgnoreCase(formatValue) ? formatValue : "Other" );
						
				} else {
					//sample id may be various: http://purl.org/..   so make the Seperator longer and unique
					key += Seperator + re.getField(cate);
					//key += Seperator + (re.getField(cate) != null? re.getField(cate) : "Other" ) ;
				}
				counts.computeIfAbsent(key, k ->  new AtomicLong() ).getAndIncrement();
			}	 
			summaries.computeIfAbsent( key, (k) -> new SampleSummary() ).parseRecord( vcf, i );
		}				
	}

	/**
	 * modifying now
	 * @param parent
	 */
	void summaryToXml(Element parent){	
		Element summaryElement =  QprofilerXmlUtils.createSubElement(parent, NodeSummary);			
		
		//get list of types eg. FT:INF:CONF
		List<String>  formatsTypes = new ArrayList<>();
		for(int i = 0; i < formatCategories.length; i ++) {
			int pos = formatCategories[i].indexOf("=");
			formatsTypes.add( pos > 0 ? formatCategories[i].substring(0, pos) : formatCategories[i] );			
		}
		
		for( String key : summaries.keySet() ) {			
			List<String> cats = new ArrayList<>(Arrays.asList(key.split(Seperator)));
			String sample = cats.remove(0);						
			Element ele = getSampleElement(summaryElement, sample);
			if(formatsTypes.isEmpty())
				summaries.get(key).toXML(ele, null, null);
			else
				summaries.get(key).toXML(ele, String.join(":", formatsTypes), String.join(":", cats));				
		}
	}
	
	
	
	
	void vcfHeaderToXmlOld(Element parent){
		Element headerElement =  QprofilerXmlUtils.createSubElement( parent, NodeHeader);
		Element metaElement =  QprofilerXmlUtils.createSubElement( headerElement, NodeHeaderMeta);
		Element structuredElement =  QprofilerXmlUtils.createSubElement( headerElement, NodeHeaderStructured);
		
		HashMap<String, Element> struElements = new HashMap<String, Element>();
				
		for(final VcfHeaderRecord record: vcfHeader ){			
			String key = record.getMetaKey().replace("##", "");
			if(key.startsWith( VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE )){
				Element ele =  QprofilerXmlUtils.createSubElement( headerElement, NodeHeaderFinal );
				//ele.setAttribute("columns", key);
 				ele.appendChild(ele.getOwnerDocument().createCDATASection(key)  );
			}
			else if( record.getId() == null ){
				Element ele =   QprofilerXmlUtils.createSubElement( metaElement, NodeHeaderMetaLine );
				ele.setAttribute("key", record.getMetaKey().replace("##", "") );
				String v = record.getMetaValue();
				 if(v.startsWith("\"") && v.endsWith("\"")) 
					 v = v.substring( 1, v.length()-1 ).trim();				  				
				 ele.setAttribute("value", v);				
			}else{				
				 if(!struElements.containsKey(key)){
					 Element ele =  QprofilerXmlUtils.createSubElement( structuredElement, NodeHeaderStructuredType );
					 ele.setAttribute("type", key);
					 struElements.put(key, ele );
				 }
				 Element ele = struElements.get(key);
				 ele =   QprofilerXmlUtils.createSubElement( ele, NodeHeaderStructuredLine );
				 //ele.setAttribute("id", record.getId());
				 for( Pair<String, String> p: record.getSubFields() ) {
					 String v = (String) p.getRight().trim();
					 if( v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length()-1).trim();					 
					 ele.setAttribute(p.getLeft(), v);						  
				 }		
			} 			
		}  				
	}

	private Element getSampleElement(Element parent, String sampleId) {
		 
		List<Element> Esamples =  QprofilerXmlUtils.getOffspringElementByTagName(parent, Sample);
		for(Element ele : Esamples)  
			if( ele.getAttribute(id).equals(sampleId))
				return ele; 
		 
		Element ele =  QprofilerXmlUtils.createSubElement( parent, Sample);
		ele.setAttribute(id, sampleId);
		
		return ele;
	}
	
}
