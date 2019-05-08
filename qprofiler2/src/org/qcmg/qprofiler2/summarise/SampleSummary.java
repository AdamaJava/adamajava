package org.qcmg.qprofiler2.summarise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.SubsitutionEnum;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class SampleSummary {	
	public final static String formats = "formats";
	public final static String values = "values";
	
	public final static String genotype = "Genotype";
	public final static String report = "report";	
	public final static String VAF = "variantAltFrequencyPercent";
	public final static String tiTvRatio = "TiTvRatio";
	public final static String transitions ="Transitions";
	public final static String transversions = "Transversions";
	public final static String dbSNP = "inDBSNP";
		
	//allele frequency value range. eg. altBinSize = 10, each bin contain counts for variants which mutation allele percent is [0, 0.1]
	public final static int altBinSize = 20; 
		
	Set< String > gts = new HashSet<>(); //store possible genotyp 0/1, 0/0, ./1 ...
	Map< String, AtomicLong > summary = new HashMap<>();
	Map<String, QCMGAtomicLongArray> summaryAD = new HashMap<>();
	
	AtomicLong counts = new AtomicLong();
	QCMGAtomicLongArray sampleTrans = new QCMGAtomicLongArray(  SubsitutionEnum.values().length );	

	private void increment(String key){	summary.computeIfAbsent(key, v -> new AtomicLong()).incrementAndGet(); }
	
	/**
	 * Updates the supplied map with 
	 * NOT SIDE-EFFECT FREE
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
		 *  vaf needs to equal the sum of all the numbers in the AD field, apart from the first number (which is the reference count)
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
		
		//java support modulus for float number; 
		//if reminding is zero, it count to previous bin	
		int idp = Integer.parseInt(dp);
		int total = vaf * altBinSize;
		int bin = total / idp;
		if(total % idp == 0 && bin > 0) {
			bin -= 1;
		}
		map.computeIfAbsent( type.name() , (k) -> new QCMGAtomicLongArray(altBinSize )).increment( bin );
		
	}
		

	public void parseRecord( VcfRecord  vcf, int formateOrder) {
		VcfFormatFieldRecord format = new VcfFormatFieldRecord(vcf.getFormatFields().get(0), vcf.getFormatFields().get(formateOrder));
		counts.incrementAndGet(); //total number
		SVTYPE type = IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt());
		boolean isdbSNP = !StringUtils.isNullOrEmptyOrMissingData(vcf.getId());
		
		String gt = format.getField(VcfHeaderUtils.FORMAT_GENOTYPE); //GT
		gts.add(gt);
				
		increment( type.name() ); //count svtype
		increment( type + gt );	  //count genotype	
						
		//variant allel frequence VAF	 
		incrementGTAD(type , gt, format.getField(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS), format.getField(VcfHeaderUtils.FORMAT_READ_DEPTH), summaryAD);				
 								
		if(isdbSNP) increment(  type.name() + "dbSNP");	//dbsnp					
		if(type.equals(SVTYPE.SNP)){ 
			//get Ti Tv counts based on GT value			
			String salt = vcf.getAlt().replace(",", "");
			String sgt = gt.replace("|", "").replace("/", "").replace(".", "").replace("0", "");				
			List<SubsitutionEnum> transTypes = new ArrayList<>();
			new HashSet<Character> (sgt.chars().mapToObj(e->(char)e).collect(Collectors.toList())).forEach(
				c ->  transTypes.add( SubsitutionEnum.getTrans( vcf.getRef().charAt(0), salt.charAt( c-'1' ) )) 
			);
						
			for(SubsitutionEnum transType : transTypes){
				increment( type.name() + transType.name() );					
			}				
		}		
	}	
	
		
	public void toXML(Element parent, String formats, String values ){
		
		Element reportE = XmlElementUtils.createSubElement(parent, report);
		if(formats != null) {
			reportE.setAttribute(SampleSummary.formats, formats);
			reportE.setAttribute(SampleSummary.values, values);
		}
		
		List<String> orderedGTs = new ArrayList<>( gts );
		//orderedGTs.sort(null);
		for(SVTYPE type : SVTYPE.values()){	
			//only output non zero value
			AtomicLong totalAL = summary.get( type.name());
			if( null == totalAL) continue;						
						
			Element reportE1  = XmlUtils.createMetricsNode( reportE,  type.toVariantType(),new Pair(XmlUtils.Scount, totalAL) );
			String key =  type.name() + "dbSNP";
			XmlUtils.outputValueNode(reportE1, "inDBSNP", summary .containsKey(key)? summary.get(key).get() : 0 ); 
						
			//titv
			if(type.equals(SVTYPE.SNP)){
				Map<String, AtomicLong> tiFreq = new HashMap<>();
				Map<String, AtomicLong> tvFreq = new HashMap<>();
				
				long sumTi = 0, sumTv = 0;
				for(SubsitutionEnum tran: SubsitutionEnum.values()) {
					if(tran.isTranstion() &&  summary.get(type.name()+tran.name()) != null) { 
						tiFreq.put(tran.toString(), summary.get(type.name()+tran.name()));
						sumTi += summary.get(type.name()+tran.name()).get();
					}else if( tran.isTransversion() &&  summary.get(type.name()+tran.name()) != null) {					 
						tvFreq.put(tran.toString(), summary.get(type.name()+tran.name()));	
						sumTv += summary.get(type.name()+tran.name()).get();
					}
				}
				double rate = sumTi * sumTv == 0 ? 0 : (double) sumTi/sumTv;			 
				XmlUtils.outputValueNode(reportE1, tiTvRatio,  rate );
				 
				XmlUtils.outputTallyGroup(reportE1 ,  transitions,  tiFreq, true);
				XmlUtils.outputTallyGroup(reportE1 ,  transversions,  tvFreq, true);
			}				
						
			Map<String, AtomicLong> gtvalues = new HashMap<>();
			for(String gt : orderedGTs) {
				AtomicLong gtv =  summary.get( type.name() + gt );
				if(gtv != null) gtvalues.put( gt, gtv );				
			}

			XmlUtils.outputTallyGroup( reportE1 , genotype, gtvalues, true );

			QCMGAtomicLongArray array = summaryAD.get(type.name());
			if(array != null ) {					
				Element cateEle = XmlUtils.outputBins(reportE1, VAF, array.toMap(), (100 / altBinSize));
				if(cateEle.hasChildNodes()) {
					XmlUtils.addCommentChild(cateEle, "Each closedBin list the variant number belonging to variant allele frequency region ( start%, end% ].");
				}				
//				Element cateEle = XmlUtils.createGroupNode(reportE1, VAF );
//				for( int i = 0; i <= altBinSize; i ++  ){
//					long count =  array.get(i) ;
//					if( count <= 0 ) continue;					
//						XmlUtils.outputBinNode( cateEle,  100*i / altBinSize, 100*(i+1) / altBinSize, new AtomicLong(count));							
//				}
//				
				
			}

		}
	}
}
