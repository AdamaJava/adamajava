package org.qcmg.qprofiler2.vcf;

import java.util.ArrayList;
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
import org.w3c.dom.Element;


public class VcfSummaryReport  extends SummaryReport {
	
	public static final String NodeHeader = "VCFHeader";
	public static final String NodeHeaderMeta = "MetaInformation" ;
	public static final String NodeHeaderMetaLine = "MetaInformationLine";
	public static final String NodeHeaderStructured = "StructuredMetaInformation";
	public static final String NodeHeaderStructuredType = "StructuredMetaInformationType";
	public static final String NodeHeaderStructuredLine = "StructuredMetaInformationLine";
	public static final String NodeHeaderFinal =  "HeaderLine";		
	public static final String NodeSummary = "VCFMetrics";
	public static final String NodeCategory = "ReportingCategory";
	public static final String Sample = "Sample";
	
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
		vcfHeaderToXml(parentElement);  //Vcf header	
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
					key += Seperator +  ( re.getField(formatKey) != null && re.getField(formatKey).equalsIgnoreCase(formatValue) ? formatValue : "Other" );
						
				}else				
					//sample id may be various: http://purl.org/..   so make the Seperator longer and unique
					key += Seperator + (re.getField(cate) != null? re.getField(cate) : "Other" ) ;
				if(!counts.containsKey(key))  counts.put( key, new AtomicLong() );
				counts.get(key).getAndIncrement();			
			}	 
			SampleSummary summary = summaries.computeIfAbsent( key, (k) -> new SampleSummary() );
			summary.parseRecord( vcf, i );			
		}				
	}

	void summaryToXml(Element parent){
		Element summaryElement = QprofilerXmlUtils.createSubElement(parent, NodeSummary);
		
		List<HashMap<String, Element>> categories =  new ArrayList< HashMap<String, Element>>();
		for(int i = 0; i <= formatCategories.length; i ++) 
			categories.add(   new HashMap<String, Element>() ); 
				
		for( String key : summaries.keySet() ) {			
			String[] cats = key.split(Seperator);			
			
			String newKey = cats[0];			
			Element ele = categories.get(0).computeIfAbsent( newKey, (k) -> QprofilerXmlUtils.createSubElement( summaryElement, Sample) );
			ele.setAttribute( "value", cats[0] ); 
			
			for( int i = 1; i < cats.length; i ++ ) {
				newKey += Seperator + cats[i];	
				if(! categories.get(i).containsKey( newKey) )
					categories.get(i).put( newKey, QprofilerXmlUtils.createSubElement(ele,  NodeCategory ) );
				ele = categories.get(i).get(  newKey );	
				ele.setAttribute("value", cats[i]);
				int pos = formatCategories[i-1].indexOf("=");
				String type = "FORMAT:" + (pos > 0 ? formatCategories[i-1].substring(0,pos)  : formatCategories[i-1]);
				//ele.setAttribute("type", "FORMAT:" + formatCategories[i-1]);
				ele.setAttribute("type", type);
				ele.setAttribute("count", counts.get(newKey)+"");
			}
			summaries.get(key).toXML(ele);
		}		
	}
		
	void vcfHeaderToXml(Element parent){
		Element headerElement = QprofilerXmlUtils.createSubElement( parent, NodeHeader);
		Element metaElement = QprofilerXmlUtils.createSubElement( headerElement, NodeHeaderMeta);
		Element structuredElement = QprofilerXmlUtils.createSubElement( headerElement, NodeHeaderStructured);
		
		HashMap<String, Element> struElements = new HashMap<String, Element>();
				
		for(final VcfHeaderRecord record: vcfHeader ){			
			String key = record.getMetaKey().replace("##", "");
			if(key.startsWith( VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE )){
				Element ele = QprofilerXmlUtils.createSubElement( headerElement, NodeHeaderFinal );
				//ele.setAttribute("columns", key);
 				ele.appendChild(ele.getOwnerDocument().createCDATASection(key)  );
			}
			else if(record.getId() == null ){
				Element ele =  QprofilerXmlUtils.createSubElement( metaElement, NodeHeaderMetaLine );
				ele.setAttribute("key", record.getMetaKey().replace("##", "") );
				String v = record.getMetaValue();
				 if(v.startsWith("\"") && v.endsWith("\"")) 
					 v = v.substring(1, v.length()-1).trim();				  				
				 ele.setAttribute("value", v);				
			}else{				
				 if(!struElements.containsKey(key)){
					 Element ele = QprofilerXmlUtils.createSubElement( structuredElement, NodeHeaderStructuredType );
					 ele.setAttribute("type", key);
					 struElements.put(key, ele );
				 }
				 Element ele = struElements.get(key);
				 ele =  QprofilerXmlUtils.createSubElement( ele, NodeHeaderStructuredLine );
				 //ele.setAttribute("id", record.getId());
				 for( Pair<String, String> p: record.getSubFields() ) {
					 String v = (String) p.getRight().trim();
					 if( v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length()-1).trim();					 
					 ele.setAttribute(p.getLeft(), v);						  
				 }		
			} 			
		}  				
	}

}
