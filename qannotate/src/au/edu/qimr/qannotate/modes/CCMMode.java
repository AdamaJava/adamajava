/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;


import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.ContentType;
import org.qcmg.common.vcf.VcfFileMeta;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.CCM;

public class CCMMode extends AbstractMode{
	private final static QLogger logger = QLoggerFactory.getLogger(CCMMode.class);
	
	public static final String DOUBLE_ZERO = "0/0";
	
	private VcfFileMeta meta;
	private Map<String, short[]> callerPositionsMap = Collections.emptyMap();
	private final Map<String, AtomicInteger> statsMap = new HashMap<>();
	
	//for unit Test
	CCMMode(){}
	
	
	public CCMMode(Options options) throws IOException {	
		logger.tool("input: " + options.getInputFileName());
        logger.tool("output for annotated vcf records: " + options.getOutputFileName());
        logger.tool("logger file " + options.getLogFileName());
        logger.tool("logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel()));
		

        loadVcfRecordsFromFile(new File( options.getInputFileName()));
		
		/*
		 * don't have access to header until after records have been loaded...
		 */
		meta = new VcfFileMeta(header);
		
		/*
		 * only perform annotation if we have multiple samples
		 */
		if (meta.getType() == ContentType.MULTIPLE_CALLERS_MULTIPLE_SAMPLES || meta.getType() == ContentType.SINGLE_CALLER_MULTIPLE_SAMPLES) {
			/*
			 * get caller, sample positions map
			 */
			callerPositionsMap = meta.getCallerSamplePositions();
			addAnnotation( );
		}
		
		reheader(options.getCommandLine(),options.getInputFileName())	;
		writeVCF( new File(options.getOutputFileName()));	
	}
	
	public static boolean isIntInArray(int i, int[] array) {
		for (int j : array) {
			if (i == j) return true;
		}
		return false;
	}
	
	public static final int getCCM(String cGT, String tGT) {
		/*
		 * deal with instances where we have single dot rather than double dot
		 */
		if (Constants.MISSING_DATA_STRING.equals(cGT)) {
			cGT = Constants.MISSING_GT;
		}
		if (Constants.MISSING_DATA_STRING.equals(tGT)) {
			tGT = Constants.MISSING_GT;
		}
		
		if (Constants.MISSING_GT.equals(cGT)) {
			if (Constants.MISSING_GT.equals(tGT)) 						return 1;
			if (DOUBLE_ZERO.equals(tGT)) 						return 2;
			int[] testAlleles = getAlleles(tGT);
			if (testAlleles[0] == 0 || testAlleles[1] == 0)	return 3;
			if (testAlleles[0] == testAlleles[1] ) 				return 4;
			return 5;
		}
			
		if (DOUBLE_ZERO.equals(cGT)) {
			if (Constants.MISSING_GT.equals(tGT)) 						return 11;
			if (DOUBLE_ZERO.equals(tGT)) 						return 12;
			int[] testAlleles = getAlleles(tGT);
			if (testAlleles[0] == 0 || testAlleles[1] == 0)	return 13;
			if (testAlleles[0] == testAlleles[1] ) 				return 14;
			return 15;
		}
		
		int[] controlAlleles = getAlleles(cGT);
		if (controlAlleles[0] == 0 || controlAlleles[1] == 0) {
			if (Constants.MISSING_GT.equals(tGT)) 						return 21;
			if (DOUBLE_ZERO.equals(tGT)) 						return 22;
			int[] testAlleles = getAlleles(tGT);
			if (testAlleles[0] == 0 || testAlleles[1] == 0) {
				if (testAlleles[0] != 0 && isIntInArray(testAlleles[0], controlAlleles)
						|| testAlleles[1] != 0 && isIntInArray(testAlleles[1], controlAlleles)) return 23;
				return 26;
			}
			if (testAlleles[0] == testAlleles[1]) {
				if (isIntInArray(testAlleles[0], controlAlleles)) return 24;
				return 27;
			} else {
				if (isIntInArray(testAlleles[0], controlAlleles) 
						|| isIntInArray(testAlleles[1], controlAlleles)) 		return 25;
				return 28;
			}
		}
		
		if (controlAlleles[0] == controlAlleles[1]) {
			if (Constants.MISSING_GT.equals(tGT)) 						return 31;
			if (DOUBLE_ZERO.equals(tGT)) 						return 32;
			int[] testAlleles = getAlleles(tGT);
			if (testAlleles[0] == 0 || testAlleles[1] == 0) {
				if (testAlleles[0] != 0 && isIntInArray(testAlleles[0], controlAlleles)
						|| testAlleles[1] != 0 && isIntInArray(testAlleles[1], controlAlleles)) return 33;
				return 38;
			}
			if (testAlleles[0] == testAlleles[1]) {
				if (testAlleles[0] == controlAlleles[0])	return 34;
				return 37;
			}
			if (isIntInArray(testAlleles[0], controlAlleles) 
					|| isIntInArray(testAlleles[1], controlAlleles)) return 35;
			return 36;
		}
		
		/*
		 * control is x/y by this point
		 */
		if (Constants.MISSING_GT.equals(tGT)) 						return 41;
		if (DOUBLE_ZERO.equals(tGT)) 						return 42;
		int[] testAlleles = getAlleles(tGT);
		if (testAlleles[0] == 0 || testAlleles[1] == 0) {
			if ((testAlleles[0] != 0 && testAlleles[0] == controlAlleles[0])
					|| (testAlleles[1] != 0 && testAlleles[1] == controlAlleles[0])) return 43;
			if ((testAlleles[0] != 0 && testAlleles[0] == controlAlleles[1])
					|| (testAlleles[1] != 0 && testAlleles[1] == controlAlleles[1])) return 44;
			return 49;
		}
		if (testAlleles[0] == testAlleles[1]) {
			if (testAlleles[0] == controlAlleles[0])	return 45;
			if (testAlleles[0] == controlAlleles[1])	return 46;
			return 50;		// update with correct number once known
		}
		if (cGT.equals(tGT)) return 47;
		if ( ! isIntInArray(testAlleles[0], controlAlleles) &&  ! isIntInArray(testAlleles[1], controlAlleles)) return 48;
		if ((isIntInArray(testAlleles[0], controlAlleles) &&  ! isIntInArray(testAlleles[1], controlAlleles))
				|| ( ! isIntInArray(testAlleles[0], controlAlleles) &&  isIntInArray(testAlleles[1], controlAlleles))) return 51;
		
		logger.warn("Found a 99er! cGT: " + cGT + ", tGT: " + tGT);
		return 99;
			
	}
	public static int[] getAlleles(String gt) {
		if (StringUtils.isNullOrEmpty(gt)) {
			throw new IllegalArgumentException("Null or empty gt passed to CCMMode.getAlleles()");
		}
		
		int i = gt.indexOf(Constants.SLASH);
		if (i == -1) {
			throw new IllegalArgumentException("Null or empty gt passed to CCMMode.getAlleles()");
		}
		int a1 = Integer.parseInt(gt.substring(0, i));
		int a2 = Integer.parseInt(gt.substring( i + 1));
		return new int[]{a1,a2};
	}
	
	void updateVcfRecordWithCCM(VcfRecord v, Map<String, short[]> map) {
		updateVcfRecordWithCCM(v, map, statsMap);
	}
	
	public static void updateVcfRecordWithCCM(VcfRecord v, Map<String, short[]> map, Map<String, AtomicInteger> statsMap) {
		/*
		 * need to do this for all callers
		 */
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
		String[] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
		
		
		/*
		 * if null or single sample, skip
		 */
		if (null != gtArr && gtArr.length > 1) {
			
			if ( ! map.isEmpty()) {
				String [] ccmArray = new String[gtArr.length];
				String [] cccArray = new String[gtArr.length];
				/*
				 * do this for each caller
				 */
				String key = "";
				for (Map.Entry<String, short[]> entry : map.entrySet()) {
					String cGT = gtArr[entry.getValue()[0] -1];
					String tGT = gtArr[entry.getValue()[1] -1];
					int ccm = getCCM(cGT, tGT);
					key += key.length() > 0 ? ":" + ccm : ccm;
					
					ccmArray[entry.getValue()[0] -1] = ccm+"";
					ccmArray[entry.getValue()[1] -1] = ccm+"";
					CCM ccmE = CCM.getCCM(ccm);
					cccArray[entry.getValue()[0] -1] = CCM.getControl(ccmE);
					cccArray[entry.getValue()[1] -1] = CCM.getTest(ccmE);
				}
				if (null != statsMap) {
					statsMap.computeIfAbsent(key, vv -> new AtomicInteger()).incrementAndGet();
				}
				
				/*
				 * update the map with the new entries, and update the vcf record
				 */
				ffMap.put(VcfHeaderUtils.FORMAT_CCM, ccmArray);
				ffMap.put(VcfHeaderUtils.FORMAT_CCC, cccArray);
				v.setFormatFields(VcfUtils.convertFFMapToList(ffMap));
				
			}
		}
	}
	
	private void addAnnotation(){
		
		/*
		 * add header records for CCC and CCM
		 */
		header.addOrReplace(VcfHeaderUtils.FORMAT +"=<ID=" + VcfHeaderUtils.FORMAT_CCM + ",Number=.,Type=String,Description=\"" + VcfHeaderUtils.FORMAT_CCM_DESC + "\">" );
		header.addOrReplace(VcfHeaderUtils.FORMAT +"=<ID=" + VcfHeaderUtils.FORMAT_CCC + ",Number=.,Type=String,Description=\"" + VcfHeaderUtils.FORMAT_CCC_DESC + "\">" );
		/*
		 * apply annotation to all records
		 */
		positionRecordMap.values().stream().flatMap(List::stream).distinct().forEach(r -> updateVcfRecordWithCCM(r, callerPositionsMap));
		
		statsMap.entrySet().stream().sorted((e1, e2) -> {return e2.getValue().get() -  e1.getValue().get();}).forEach(e -> logger.info("key: " + e.getKey() + ", count: " + e.getValue().get()));
	}

	@Override
	void addAnnotation(String dbfile) throws IOException {
		// TODO Auto-generated method stub
	}
	
}
