package org.qcmg.qprofiler2.cohort;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.SummaryReport;
import org.qcmg.qprofiler2.summarise.SampleSummary;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class CohortSummaryReport extends SummaryReport { 
	public static final String outputSeperate = "\t";	
	public static final String headerline = String.join( outputSeperate, new String[] { "File","Sample" ,"ReportingCategory","VariantType","VariantCount","DbSnpProportion","TiTvRatio"});
	
	final String sampleId; 
	final String file;	
	List<Category> categories = new ArrayList<>();
	
	private long sum_ti = 0; 
	private long sum_tv = 0;
	private long sum_db = 0;
	private long sum_count = 0;
	
	CohortSummaryReport(File fxml, Element sampleNode) throws IOException { 
		this.file = fxml.getCanonicalPath();
		this.sampleId = sampleNode.getAttribute(XmlUtils.NAME);
		
		for( Element ele : XmlElementUtils.getChildElementByTagName( sampleNode, SampleSummary.report ) ) { 
			Category cat = new Category( ele.getAttribute(SampleSummary.values), ele ); 
			categories.add(cat );
			
			 // summary
			sum_ti += cat.ti;
			sum_tv += cat.tv;
			for(int db : cat.dbSnpCounts.values()) sum_db += db;
			for(int count : cat.variantsCounts.values()) sum_count += count;			
		}
				
	}	
	
	List<String> outputCounts(  ) { 		 
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
	public long[] getCountSum() { 
		return new long[] { sum_count, sum_db, sum_ti, sum_tv  };		
	}
	
	static class Category { 
		final String category;  // eg. FORMAT:FT=PASS;FORMAT:INF=.
		HashMap<String, Integer> variantsCounts = new HashMap<>();
		HashMap<String, Integer> dbSnpCounts = new HashMap<>();
		
		 // for snv only, there is only one svn for each report unde same category
		final String titvRate; 
		int ti = 0, tv = 0;
						
		Category(String name, Element report) { 
					 
			this.category = (name == null || name.isEmpty()) ? "-" : name; 
			String titv = "-" ;
			 
			 // for(Element ele :QprofilerXmlUtils.getOffspringElementByTagName(report, SampleSummary.variantType)) { 
			for(Element ele :XmlElementUtils.getOffspringElementByTagName(report, XmlUtils.SEQUENCE_METRICS)) { 
				 // record counts and dbsnp for all type variants
				String type = ele.getAttribute(XmlUtils.NAME);	
				int count = Integer.parseInt(ele.getAttribute("count"));
				variantsCounts.put(type, count );
				
				Element e1 = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.VALUE).stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals(SampleSummary.dbSNP)).findFirst().get();				
				int db = Integer.parseInt( e1.getTextContent() );
				dbSnpCounts.put(type, db);
				
				if(type.equals(SVTYPE.SNP.toVariantType())) { 
					e1 = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.VALUE).stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals(SampleSummary.tiTvRatio)).findFirst().get();				
					titv = e1.getTextContent();	
					
					 // ti
					Optional<Element> streams = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.VARIABLE_GROUP).stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals(SampleSummary.transitions)).findFirst();				
					if( streams.isPresent()   ) { 
						List<Integer> sums = new ArrayList<>();						
						XmlElementUtils.getChildElementByTagName(streams.get(), XmlUtils.TALLY).stream()
							.forEach( e ->   sums.add(  Integer.parseInt(e.getAttribute(XmlUtils.COUNT)) ) );
						ti = sums.stream().mapToInt(i -> i.intValue()).sum();
					} 
					
					 // tv
					streams = XmlElementUtils.getChildElementByTagName(ele, XmlUtils.VARIABLE_GROUP).stream().filter(e -> e.getAttribute(XmlUtils.NAME).equals(SampleSummary.transversions)).findFirst() ;				
					if( streams.isPresent() ) { 
						List<Integer> sums = new ArrayList<>();
						XmlElementUtils.getChildElementByTagName(streams.get(), XmlUtils.TALLY).stream()
							.forEach( e ->   sums.add(  Integer.parseInt(e.getAttribute(XmlUtils.COUNT)) ) );
						tv = sums.stream().mapToInt(i -> i.intValue()).sum();
					}
					
				}
			}
			 
			this.titvRate = titv;				 
		}
		
		/**
		 * order: "ReportingCategory","VariantType","VariantCount","DbSnpProportion","TiTvRatio"
		 * @return a list of counts for each variant type
		 */
		List<String> output() { 
			List<String> output = new ArrayList<>(); 
			for(Entry<String, Integer> entry :  variantsCounts.entrySet() ) { 	
				StringBuilder sb = new StringBuilder(category);
				sb.append(outputSeperate).append(entry.getKey())
				  .append(outputSeperate).append(entry.getValue() )
				  .append(outputSeperate).append( String.format( "%.3f",  (double) dbSnpCounts.get(entry.getKey()) / entry.getValue()  ))
				  .append(outputSeperate).append( entry.getKey().equals(SVTYPE.SNP.toVariantType()) ? titvRate : "-" );
				
				output.add( sb.toString() );
			}
			return output;
		}		
	}

	
	@Override @Deprecated
	public void toXml(Element parent) { }

}
