package org.qcmg.qprofiler2.summarise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.SubsitutionEnum;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;

import org.w3c.dom.Element;

public class SampleSummary {
	public final static String variantType = "VariantType";
	public final static String genotypes = "Genotypes";
	public final static String genotype = "Genotype";
	public final static String substitutions = "Substitutions";	
	public final static String substitution = "Substitution";
	public final static String report = "Report";	
	public final static String VAF = "VariantAlleleFrequency";
	public final static String binTally = "BinTally";
	public final static String tiTvRatio = "TiTvRatio";
	public final static String transitions ="Transitions";
	public final static String transversions = "Transversions";
		
	List< String > gts = new ArrayList<>(); //store possible genotyp 0/1, 0/0, ./1 ...
	Map< String, AtomicLong > summary = new HashMap<>();
	Map<String, QCMGAtomicLongArray> summaryAD = new HashMap<>();
	
	AtomicLong counts = new AtomicLong();
	QCMGAtomicLongArray sampleTrans = new QCMGAtomicLongArray(  SubsitutionEnum.values().length );	

	private void increment(String key){		
		summary.computeIfAbsent(key, v -> new AtomicLong()).incrementAndGet();
	}
	
	/**
	 * Updates the supplied map with 
	 *  
	 * NOT SIDE-EFFECT FREE
	 * 
	 * @param type
	 * @param gt
	 * @param ad
	 * @param dp
	 * @param map
	 */
	public static void incrementGTAD(SVTYPE type,String gt, String ad, String dp, Map<String, QCMGAtomicLongArray> map){	
		
		if( ad == null || ad.contains(".") ||  gt == null || gt.contains(".")  ||  gt.equals("0/0") || gt.equals("0|0") ) 
				return;		
		
		int commaIndex = ad.indexOf(Constants.COMMA);
		int vaf = 0;
		
		/*
		 * vaf needs to equal the sum of all the numbers in the AD field, apart from the first number (which is the reference count)
		 */
		while (commaIndex > -1) {
			int nextConmmaIndex = ad.indexOf(Constants.COMMA, commaIndex + 1);
			if (nextConmmaIndex > -1) {
				vaf += Integer.parseInt(ad.substring(commaIndex + 1, nextConmmaIndex));
				commaIndex = nextConmmaIndex;
			} else {
				vaf += Integer.parseInt(ad.substring(commaIndex + 1));
				break;
			}
		}
		 
		int rate = (int) ( 0.5 + (double) ( vaf * 100 ) / Integer.parseInt(dp) );
		map.computeIfAbsent(type.name() , (k) -> new QCMGAtomicLongArray(51)).increment( rate );	
	}
	
//	private void incrementGTAD(SVTYPE type,String gt, String ad, String DP){	
//		
//		if( ad == null || ad.contains(".") ||  gt == null || gt.contains(".")  ||  gt.equals("0/0") || gt.equals("0|0") ) 
//			return;		
//		
//		summaryAD.computeIfAbsent(type.name() , (k) -> new QCMGAtomicLongArray(102));		
//		
//		//bf: vaf = f(1)/(f(0) + f(1) + f(2) ..)  now: vaf (f(1) + f(2) +...) /DP
//		String[] ads = ad.split(",");
//		int vaf = 0; 
//		for(int i = 1; i < ads.length; i ++) {
//			if (i > 0) {
//				vaf += Integer.parseInt(ads[i]);
//			}
//		}
//		
//		int dp = Integer.parseInt(DP);
//		int rate = (int) ( 0.5 + (double) ( vaf * 100 ) / dp );
//		summaryAD.get( type.name()  ).increment( rate );	
//	}
		

	public void parseRecord( VcfRecord  vcf, int formateOrder) {
		VcfFormatFieldRecord format = new VcfFormatFieldRecord(vcf.getFormatFields().get(0), vcf.getFormatFields().get(formateOrder));
		counts.incrementAndGet(); //total number
		SVTYPE type = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());
		boolean isdbSNP = !StringUtils.isNullOrEmptyOrMissingData(vcf.getId());
		
		String gt = format.getField("GT"); //GT
		if( !gts.contains(gt) ) gts.add(gt);
				
		increment( type + ""); //count svtype
		increment( type + gt);	//count genotyp	
						
		//variant allel frequence VAF	 
		incrementGTAD(type , gt, format.getField("AD"), format.getField("DP"), summaryAD);				
 								
		if(isdbSNP) increment(  type.name() + "dbSNP");	//dbsnp					
		if(type.equals(SVTYPE.SNP)){ 
			//get Ti Tv counts based on GT value			
			String salt = vcf.getAlt().replace(",", "");
			String sgt = gt.replace("|", "").replace("/", "").replace(".", "").replace("0", "");				
			List<SubsitutionEnum> transTypes = new ArrayList<SubsitutionEnum>();
			new HashSet<Character> (sgt.chars().mapToObj(e->(char)e).collect(Collectors.toList())).forEach(
				c ->  transTypes.add( SubsitutionEnum.getTrans( vcf.getRef().charAt(0), salt.charAt( c-'1' ) )) 
			);
						
			for(SubsitutionEnum transType : transTypes){
				String mark = transType.isTranstion() ? "Ti" : transType.isTransversion() ? "Tv" : "Other";	
				increment( type.name() + mark  );
				increment( type.name() + mark + transType.name() );					
			}				
		}		
	}	 			
		
	public void toXML(Element parent){
		Element reportE = QprofilerXmlUtils.createSubElement(parent, report);	
		for(SVTYPE type : SVTYPE.values()){	
			//only output none zero value
			if( !summary.containsKey( type.name())) continue;		
			final long total = summary.get( type.name()).get();
			Element svtypeE = QprofilerXmlUtils.createSubElement(reportE,  variantType);
			svtypeE.setAttribute("type", type.toVariantType() );
			svtypeE.setAttribute( QprofilerXmlUtils.count, total + "");
			String key =  type.name() + "dbSNP";
			svtypeE.setAttribute("inDBSNP", summary .containsKey(key)? summary.get(key).get()+"" : "0" ); 
			
			Element genotypeE = QprofilerXmlUtils.createSubElement(svtypeE, genotypes);
			for(String gt : gts){
				key =  type.name() + gt;
				if(summary.containsKey(key) && summary.get(key).get() > 0){
					Element element = QprofilerXmlUtils.createSubElement(genotypeE, genotype);
					element.setAttribute("type", gt);
					long count = summary.containsKey(key)? summary.get(key).get()   : 0;
					element.setAttribute( QprofilerXmlUtils.count, count+"" ); 
					QCMGAtomicLongArray array = summaryAD.get(key);
				}
			}
							
			//vaf
			Element ele = QprofilerXmlUtils.createSubElement( svtypeE, VAF );
			VAF2Xml( ele, type.name() );
			 			
			//titv
			if(type.equals(SVTYPE.SNP)){
				Element titvE = QprofilerXmlUtils.createSubElement( svtypeE, substitutions );
				Element titvMethodE = QprofilerXmlUtils.createSubElement( titvE, type.name() );

				long sum1 =  summary.getOrDefault( type.name() + "Ti", new AtomicLong(0)).get();
				long sum2 =  summary.getOrDefault( type.name() + "Tv", new AtomicLong(0)).get();	 
				titvMethodE.setAttribute( tiTvRatio, (sum2 == 0 ? "-" : String.format( "%.2f", (double)sum1/sum2)));							
				titvMethodE.setAttribute( transitions, sum1 + "");
				titvMethodE.setAttribute( transversions, sum2 + "");	
				for(SubsitutionEnum tran: SubsitutionEnum.values()){
					if( !tran.isTransversion() && !tran.isTranstion()) continue; 
					String mark = tran.isTranstion()? "Ti" :  "Tv" ;
					if( ! summary.containsKey(key = type.name() + mark + tran.name())) continue;	
					Element subE = QprofilerXmlUtils.createSubElement( titvMethodE, substitution );							
					subE.setAttribute( QprofilerXmlUtils.count, summary.get(key).get() + "" );
					subE.setAttribute("change", tran.toString());							
				}
			}
		}
	}
	
	private void VAF2Xml(Element parent, String tagName  ){
		 
		QCMGAtomicLongArray array = summaryAD.get(tagName);
		if(array == null) return;			
		long total = 0;
		for( int i = 0; i < array.length(); i ++  )
			total += array.get(i);
		
		Element ele = QprofilerXmlUtils.createSubElement(parent, tagName);
		ele.setAttribute( QprofilerXmlUtils.totalCount, total+"");
		int arrayLast = 101;
		for( int i = 0; i < array.length(); i ++  ){
			long count =  array.get(i) ;
			if(count <= 0) continue;
			Element subE = QprofilerXmlUtils.createSubElement(ele, binTally);	
			subE.setAttribute( QprofilerXmlUtils.count , count+"");
			if(i == arrayLast ) subE.setAttribute("bin", "null");
			else if(arrayLast == 101) 
				subE.setAttribute("bin", String.format("%.2f", (double) i / 100)  );
			else
				subE.setAttribute("bin", String.format("%.1f", (double) i / 10)  );
			
			//percent	
			int percent = (int)  (0.5 + (double) ( count * 100 ) / total );
			subE.setAttribute(QprofilerXmlUtils.percent ,percent+"%" );			 
		}		
	}


	
}
