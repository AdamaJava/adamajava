package org.qcmg.qprofiler.summarise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.qprofiler.report.SummaryReport;
import org.qcmg.qprofiler.vcf.VcfSummaryReport;
import org.w3c.dom.Element;

public class SampleSummary {

	// the ratio of transitions vs. transversions in SNPs.  
	public enum TRANS{
		AG(0), GA(1), CT(2), TC(3),			
		AC(4), CA(5),AT(6), TA(7),CG(8), GC(9), GT(10), TG(11), 
		NA(12),AN(13), NG(14), GN(15),NT(16),TN(17),NC(18),CN(19), Others(20); 
		
		final int order;
		TRANS(int od){  this.order = od;  }		
		public boolean isTranstion(){ return (order <= 3); } 
		public boolean isTransversion(){ return (order >=4 && order <= 11); } 
		@Override
		public String toString(){ return this.name().substring(0,1)  + ">" + this.name().substring(1); }
		
		public static TRANS getTrans(char ref, char alt){
			int ascRef =  Character.toUpperCase(ref) ;
			int ascii = ascRef * 2 + Character.toUpperCase(alt);	
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
	
	final String sampleType; 
	
	public SampleSummary(String value){
		this.sampleType = value; 
	}
	Set<String> gts = new HashSet<>(); //store possible genotype 0/1, 0/0, ./1 ...
	Map<String, AtomicLong> summary = new HashMap<>();
	
	AtomicLong counts = new AtomicLong();
	QCMGAtomicLongArray sampleTrans = new QCMGAtomicLongArray(  TRANS.values().length );
	
	public long getCounts(){ return counts.get();}

	private void increment(String key){
		summary.computeIfAbsent(key, v -> new AtomicLong()).incrementAndGet();
	}
	

	public void parseRecord( VcfRecord  vcf, VcfFormatFieldRecord format) {
		counts.incrementAndGet(); //total number
		SVTYPE type = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());
		boolean isdbSNP = !StringUtils.isNullOrEmptyOrMissingData(vcf.getId());
		
		String gt = format.getField("GT"); //GT
		gts.add(gt);
				
		String filter = "Other";
		if(format.getField("FT") != null  && format.getField("FT").equals("PASS") ) filter = "PASS";				
		increment(filter); //count to total
		String filterAndType = filter + type;
		increment(filterAndType ); //count svtype
		increment( filterAndType + gt);	//count genotyp	
				
		if(isdbSNP) increment( filterAndType + "dbSNP");	//dbsnp					
		if(type.equals(SVTYPE.SNP)){ 
			//get Ti Tv counts based on GT value
			List<TRANS> transTypes = new ArrayList<>();
			String salt = vcf.getAlt().replace(",", "");
			String sgt = gt.replace("|", "").replace("/", "").replace(".", "").replace("0", "");	
			
			Set<Character> uniqAlts = new HashSet<> (sgt.chars().mapToObj(e->(char)e).collect(Collectors.toList())  );												
			for(char c : uniqAlts) 				 
				transTypes.add(TRANS.getTrans( vcf.getRef().charAt(0), salt.charAt( c-'1' )) );		
 			
			for(TRANS transType : transTypes){
				String mark = transType.isTranstion() ? "Ti" : transType.isTransversion() ? "Tv" : "Other";	
				increment(filterAndType + mark  );
				increment(filterAndType + mark + transType.name() );			
			}
		}	 			
	}	
		
	public void toXML(Element parent){
		parent.setAttribute( "type", "FORMAT:INF");
		parent.setAttribute( "value" , sampleType);
		parent.setAttribute("count", counts.get()+""); //add total number
			
		for(String filter: new String[] {"PASS", "Other"}){
			if( ! summary.containsKey(filter)) continue; 
			Element filterE = SummaryReport.createSubElement(parent, VcfSummaryReport.NodeCategory);
			filterE.setAttribute( "type", "FORMAT:FT");			
			filterE.setAttribute("value", filter);
			filterE.setAttribute("count", summary.get(filter) + ""  );
			Element reportE = SummaryReport.createSubElement(filterE, "Report");			
			for(SVTYPE type : SVTYPE.values()){			
				//only output none zero value
				if( !summary.containsKey(filter + type.name())) continue;				
				Element svtypeE = SummaryReport.createSubElement(reportE, "VariationType");
				svtypeE.setAttribute("type", type.toVariantType() );
				svtypeE.setAttribute("count", summary.get(filter + type.name()) + "");
				String key = filter + type.name() + "dbSNP";
				svtypeE.setAttribute("inDBSNP", summary .containsKey(key)? summary.get(key).get()+"" : "0" ); 
				
				Element genotypeE = SummaryReport.createSubElement(svtypeE, "Genotypes");
				for(String gt : gts){
					key = filter + type.name() + gt;
					if(summary.containsKey(key) && summary.get(key).get() > 0){
						Element element = SummaryReport.createSubElement(genotypeE, "Genotype");
						element.setAttribute("type", gt);
						element.setAttribute("count", summary.containsKey(key)? summary.get(key)+"" : "0" ); 
					}
				}
				 
				//titv
				if(type.equals(SVTYPE.SNP)){
					Element titvE = SummaryReport.createSubElement(svtypeE, "Substitutions");
					long sum1 =  summary.getOrDefault(filter + type.name() + "Ti", new AtomicLong(0)).get();
					long sum2 =  summary.getOrDefault( filter + type.name() + "Tv", new AtomicLong(0)).get();	 
					titvE.setAttribute("TiTvRatio", String.format("%.2f", (double) sum1/sum2) );
					titvE.setAttribute("Transitions", summary.get(filter + type + "Ti") + "");
					
					titvE.setAttribute("Transitions", sum1 + "");
					titvE.setAttribute("Transversions", sum2 + "");
					
					for(TRANS tran: TRANS.values()){
						if( !tran.isTransversion() && !tran.isTranstion()) continue; 
						String mark = tran.isTranstion()? "Ti" :  "Tv" ;
						if( ! summary.containsKey(key = filter+ type + mark + tran.name())) continue;
						
						Element subE = SummaryReport.createSubElement(titvE, "Substitution");
						subE.setAttribute("change", tran.toString());
						subE.setAttribute("count", summary.get(key).get() + "" );
					}
										
				}	
			}
		}		
	}

}
