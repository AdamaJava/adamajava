package org.qcmg.qprofiler2.vcf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.summarise.SampleSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;


public class VcfSummaryReport  extends SummaryReport {
	public static final String seperator = Constants.COLON_STRING;		
	public static final String Sample = "sample";	
	private final VcfHeader vcfHeader;	
	private final String[] sampleIds; 
 
	//it allows the format field value eg. --formart FT=PASS, then it seperate value to PASS the others
	private final String[] formatCategories;
	Map< String, Map<String,SampleSummary> > summaries = new HashMap<>();
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
		XmlUtils.vcfHeaderToXml(parentElement, vcfHeader);
		logger.info("outputing sample information to xml...");
		summaryToXml( parentElement  );		
	}
	

	void parseRecord( VcfRecord  vcf ) {
		updateRecordsInputed();
		
		List<String> formats = vcf.getFormatFields();
		if(sampleIds == null || formats.size() != sampleIds.length + 1) {
			logger.warn("missing/redundant sample column exists in vcf record: " + vcf.toSimpleString());
		}
		//for each sample column
		for(int i = 1; i < formats.size(); i ++){
			VcfFormatFieldRecord re = new VcfFormatFieldRecord(formats.get(0), formats.get(i));
			 
			List<String> cates = new ArrayList<>();
			for(String cate : formatCategories ){
				//new
				int pos = cate.indexOf("="); 
				if(pos > 0){
					String formatKey = cate.substring(0, pos).trim();
					String formatValue = cate.substring(pos+1).trim();
					cates.add(  re.getField(formatKey) == null ? null :
							  re.getField(formatKey).equalsIgnoreCase(formatValue) ? formatValue : "Other"  );						
				} else {					 
					cates.add(   re.getField(cate) );					 
				}
			}	 
			Map<String, SampleSummary> map =  summaries.computeIfAbsent( sampleIds[i-1], (k) -> new HashMap<String, SampleSummary>() );
			map.computeIfAbsent( StringUtils.join( cates, seperator), (k) -> new SampleSummary() ).parseRecord( vcf, i ) ;
		}				
	}

	/**
	 * modifying now
	 * @param parent
	 */
	void summaryToXml(Element parent){			
		//get list of types eg. FT:INF:CONF
		List<String>  formatsTypes = new ArrayList<>();
		for(int i = 0; i < formatCategories.length; i ++) {
			int pos = formatCategories[i].indexOf("=");
			formatsTypes.add( pos > 0 ? formatCategories[i].substring(0, pos) : formatCategories[i] );			
		}	
		
		Element summaryElement =  XmlElementUtils.createSubElement(parent,  ProfileType.VCF.getReportName()+"Metrics" );		
		for( String sample : summaries.keySet() ) {	
			Element ele =  XmlElementUtils.createSubElement( summaryElement, Sample);
			ele.setAttribute(XmlUtils.Sname, sample);
						
			for(String cates : summaries.get(sample).keySet() ) {			
				if( formatsTypes.isEmpty() ) {
					summaries.get(sample).get(cates).toXML( ele, null, null );
				}else {
					summaries.get(sample).get(cates).toXML( ele, StringUtils.join( formatsTypes, seperator), cates );	
				}
			}			
		}		
	}
	
}
