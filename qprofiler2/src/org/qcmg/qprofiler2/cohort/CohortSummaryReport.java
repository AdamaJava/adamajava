package org.qcmg.qprofiler2.cohort;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.summarise.SampleSummary;
import org.qcmg.qprofiler2.vcf.VcfSummaryReport;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class CohortSummaryReport extends SummaryReport {
	public static String categorySeperate = ";";
	public static String outputSeperate = "\t";	
	public static String headerline = String.join( outputSeperate, new String[]{ "File","Sample" ,"ReportingCategory","VariantType","VariantCount","DbSnpProportion","TiTvRatio"});
	
	final String sampleId; 
	final String file;	
	List<Category> categories = new ArrayList<>();
	
	private long sum_ti = 0; 
	private long sum_tv = 0;
	private long sum_db = 0;
	private long sum_count = 0;
	
	CohortSummaryReport(File fxml, Element sampleNode) throws IOException{
		this.file = fxml.getCanonicalPath();
		this.sampleId = sampleNode.getAttribute("value");
		 
		NodeList reportNS =	 sampleNode.getElementsByTagName(SampleSummary.report);
		for(int j = 0; j < reportNS.getLength(); j ++){
			Element report = ( Element ) reportNS.item(j);	
			
			List<String> cats = new ArrayList<>();
			while((report = (Element) report.getParentNode()) != null){
				if(report.getNodeName().equalsIgnoreCase(VcfSummaryReport.Sample  )) break;				 					
				if(report.getNodeName().equals( VcfSummaryReport.NodeCategory  ) )
					cats.add(report.getAttribute("value"));
			}
			if(cats.size() > 0 ) Collections.reverse(cats);
			String key = String.join( categorySeperate, cats);
			if(key.length() == 0) key = "-";
			
			//for each report node
			Category cat = new Category(key, ( Element ) reportNS.item(j)); 
			categories.add(cat );
			
			//summary
			sum_ti += cat.ti;
			sum_tv += cat.tv;
			for(int db : cat.dbSnpCounts.values()) sum_db += db;
			for(int count : cat.variantsCounts.values()) sum_count += count;

		}		
	}	
	
	List<String> outputCounts(  ){		 
		List<String> output = new ArrayList<>(); 
		for(Category cat: categories)
			for(String str : cat.output())
				output.add(file + outputSeperate + sampleId + outputSeperate + str);
		
		return output;
	}	
	
	/**
	 * summary counts will output to log file
	 * @return an array of total counts of variantCounts , dbsnp , ti and tv
	 */
	public long[] getCountSum(){
		
	//	return new long[]{ sum_ti, sum_tv, sum_db, sum_count };
		return new long[]{ sum_count, sum_db, sum_ti, sum_tv  };
		
	}
	
	class Category{
		final String category; //eg. FORMAT:FT=PASS;FORMAT:INF=.
		HashMap<String, Integer> variantsCounts = new HashMap<>();
		HashMap<String, Integer> dbSnpCounts = new HashMap<>();
		
		//for snv only, there is only one svn for each report unde same category
		final String titvRate; 
		int ti, tv;
						
		Category(String name, Element report){
			
			this.category = name; 
			String titv = "-" ;
			for(Element ele :QprofilerXmlUtils.getOffspringElementByTagName(report, SampleSummary.variantType)){
				//record counts and dbsnp for all type variants
				String type = ele.getAttribute("type");				
				int db = Integer.parseInt( ele.getAttribute("inDBSNP"));
				int count = Integer.parseInt(ele.getAttribute("count"));
				variantsCounts.put(type, count );
				dbSnpCounts.put(type, db);
				if(type.equals(SVTYPE.SNP.toVariantType())){
					//"Substitutions"
					Element titvE =QprofilerXmlUtils.getChildElement(QprofilerXmlUtils.getChildElement(ele, SampleSummary.substitutions , 0),SVTYPE.SNP.name(), 0 );
					ti = Integer.parseInt( titvE.getAttribute(SampleSummary.transitions ));
					tv = Integer.parseInt( titvE.getAttribute(SampleSummary.transversions ));
					titv = titvE.getAttribute( SampleSummary.tiTvRatio );
				}
			}
			this.titvRate = titv; 
		}
		
		/**
		 * order: "ReportingCategory","VariantType","VariantCount","DbSnpProportion","TiTvRatio"
		 * @return a list of counts for each variant type
		 */
		List<String> output(){
			List<String> output = new ArrayList<>(); 
			for(String type :  variantsCounts.keySet() ){	
				StringBuilder sb = new StringBuilder(category);
				sb.append(outputSeperate).append(type)
				  .append(outputSeperate).append( variantsCounts.get(type) )
				  .append(outputSeperate).append( String.format( "%.3f",  (double) dbSnpCounts.get(type) / variantsCounts.get(type) ))
				  .append(outputSeperate).append( type.equals(SVTYPE.SNP.toVariantType()) ? titvRate : "-" );
				
				output.add( sb.toString() );
			}
			return output;
		}		
	}

	
	@Override @Deprecated
	public void toXml(Element parent) {
		// TODO Auto-generated method stub		
	}

}
