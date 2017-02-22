package org.qcmg.qprofiler.summarise;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.concurrent.atomic.AtomicLong;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qprofiler.report.SummaryReport;
import org.w3c.dom.Element;

public class SampleSummary {

	// the ratio of transitions vs. transversions in SNPs.  
	public enum TRANS{
		AG(0), GA(1), CT(2), TC(3),			
		AC(4), CA(5),AT(6), TA(7),CG(8), GC(9), GT(10), TG(11), 
		NA(12),AN(13), NG(14), GN(15),NT(16),TN(17),NC(18),CN(19), Others(20); 
		

		final int order;
		TRANS(int od){  this.order = od;  }		
		public boolean isTranstion(){ return (order <= 3)? true: false; } 
		public boolean isTransversion(){ return (order >=4 && order <= 11)? true: false; } 
		
		public static TRANS getTrans(char ref, char alt){
			int ascRef =  (int) Character.toUpperCase(ref) ;
			int ascii = ascRef * 2 + (int) Character.toUpperCase(alt);	
			switch (ascii) {
				case 201 : return AG;
				case 207 : return GA;
				case 218 : return CT;
				case 235 : return TC;
				case 197 : return AC;
				case 199 : return CA;
				case 214 : return AT;
				case 233 : return TA;
				case 205 : return CG;
				case 209 : return GC;
				case 226 : return GT;
				case 239 : return TG;
				case 221 : return NA;
				case 208 : return AN;
				case 227 : return NG;
				case 220 : return GN;
				case 240 : return NT;
				case 246 : return TN;
				case 223 : return NC;
				case 212 : return CN;
			}
			
			return Others;
		}		
	} 	
		
	ArrayList<String> gts = new ArrayList<String>(); //store possible genotyp 0/1, 0/0, ./1 ...
	HashMap<String, AtomicLong> summary = new HashMap<String, AtomicLong>();
	
	AtomicLong counts = new AtomicLong();
	QCMGAtomicLongArray sampleTrans = new QCMGAtomicLongArray(  TRANS.values().length );	
	public long getCounts(){ return counts.get();}

	private void increment(String key){
		if(!summary.containsKey(key)) 
			summary.put(key, new AtomicLong());
		summary.get(key).getAndIncrement();						
	}
	
	public void parseRecord( VcfRecord  vcf, VcfFormatFieldRecord format) {
		counts.incrementAndGet(); //total number
		SVTYPE type = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());
		boolean isdbSNP = !StringUtils.isNullOrEmptyOrMissingData(vcf.getId());
		
		String gt = format.getField("GT"); //GT
		if( !gts.contains(gt) ) gts.add(gt);
		
		TRANS transType = (type.equals(SVTYPE.SNP))? TRANS.getTrans( vcf.getRef().charAt(0), vcf.getAlt().charAt(0)) : null;
		
		String filter = "Other";
		if(format.getField("FT") != null  && format.getField("FT").equals("PASS") )
			filter = "PASS";
				
		increment(filter); //count to total		
//		increment( filter + gt);	//count genotyp	
		increment( filter + type.name() ); //count svtype
		increment( filter + type.name() + gt);	//count genotyp	
		
		if(isdbSNP) increment( filter + type.name() + "dbSNP");	//dbsnp					
		if(type.equals(SVTYPE.SNP)){
			String mark = "Other";			
			if(transType.isTranstion()) mark = "Ti";
			else if(transType.isTransversion()) mark = "Tv";
			 
			increment(filter + type.name() + mark  );
			increment(filter + type.name() + mark + transType.name() );
		}	 			
	}	
		
	public void toXML(Element parent){
		 
		parent.setAttribute("count", counts.get()+""); //add total number
				
		for(String filter: new String[] {"PASS", "Other"}){
			Element filterE = SummaryReport.createSubElement(parent, "FILTER");
			filterE.setAttribute("value", filter);
			filterE.setAttribute("count",  (summary.get(filter) == null ? "0" : summary.get(filter) + "") );
						
			for(SVTYPE type : SVTYPE.values()){			
				//only output none zero value
				if( !summary.containsKey(filter + type.name())) continue;				
				Element svtypeE = SummaryReport.createSubElement(filterE, "VariationClass");
				svtypeE.setAttribute("type", type.name() );
				svtypeE.setAttribute("count", summary.get(filter + type.name()) + "");
				String key = filter + type.name() + "dbSNP";
				svtypeE.setAttribute("inDBSNP", summary .containsKey(key)? summary.get(key).get()+"" : "0" ); 
				
				Element genotypeE = SummaryReport.createSubElement(svtypeE, "Genotypes");
				for(String gt : gts){
					key = filter + type.name() + gt;					
					Element element = SummaryReport.createSubElement(genotypeE, "Genotype");
					element.setAttribute("type", gt);
					element.setAttribute("count", summary.containsKey(key)? summary.get(key)+"" : "0" ); 
				}
				 
				//titv
				if(type.equals(SVTYPE.SNP)){
					Element titvE = SummaryReport.createSubElement(svtypeE, "TiTv");
					double sum1 = (double) summary.get(filter + type.name() + "Ti").get();
					double sum2 = (double)summary.get(filter + type.name() + "Tv").get();				 
					titvE.setAttribute("ratio", String.format("%.2f", sum1/sum2) );
					
					Element[] eleTran = new Element[3];
					String pairs[][] = new String[][]{{"Transitions", "Ti"},{"Transversions", "Tv"},{"Others", "Other"}};
					for(int i = 0; i < pairs.length; i ++)														 
						if(summary.containsKey(key = filter + type + pairs[i][1]))
							( eleTran[i] = SummaryReport.createSubElement(titvE, pairs[i][0])).setAttribute("count", summary.get(filter + type + pairs[i][1]) + "");
										
 					for(TRANS tran: TRANS.values()){
 						Element element = eleTran[2];
 						String mark = "Other";						
  						if(tran.isTranstion()){	element = eleTran[0]; mark = "Ti";}
  						else if(tran.isTransversion()){ element = eleTran[1]; mark = "Tv";}
  						
  						if( ! summary.containsKey(key = filter+ type + mark + tran.name())) continue;
      					Element	childE = SummaryReport.createSubElement(element, mark);
  						childE.setAttribute("change", tran.name());
  						childE.setAttribute("count", summary.get(key).get()+"" );	 						  						
 					}				
				}	
			}
			
//			Element genotypeE = SummaryReport.createSubElement(filterE, "Genotype");
//			for(String gt : gts){
//				String key = filter + gt;					
//				Element element = SummaryReport.createSubElement(genotypeE, "GT");
//				element.setAttribute("value", gt);
//				element.setAttribute("count", summary.containsKey(key)? summary.get(key)+"" : "0" ); 
//			}
		}		
	}

}
