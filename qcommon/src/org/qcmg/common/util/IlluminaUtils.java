package org.qcmg.common.util;

import org.qcmg.common.string.StringUtils;


public class IlluminaUtils {
	
	
	public static String convertIlluminaGenotype(String illuminaGenotype, String illuminaCall, char ref) {
		if ('\u0000' == ref) throw new IllegalArgumentException("invalid reference specified");
		if (StringUtils.isNullOrEmpty(illuminaGenotype)) throw new IllegalArgumentException("invalid illumina genotype specified");
		if (StringUtils.isNullOrEmpty(illuminaCall)) throw new IllegalArgumentException("invalid illumina call specified");
		
		char illGenChar1 = illuminaGenotype.charAt(0);
		char illGenChar2 = illuminaGenotype.charAt(1);
		if ('/' == illGenChar2) {
			if (illuminaGenotype.length() < 3)
				throw new IllegalArgumentException("invalid illumina genotype specified: " + illuminaGenotype);
			illGenChar2 = illuminaGenotype.charAt(2);
		}
		
		boolean complement = ref != illGenChar1 && ref != illGenChar2;
		
//		System.out.println("illGenChar1: " + illGenChar1 + ", illGenChar2: " + illGenChar2 + ", complement: " + complement);
		
		if ("AA".equals(illuminaCall)) {
			return complement ? 
					BaseUtils.getComplement(illGenChar1) + "/" + BaseUtils.getComplement(illGenChar1)
					: illGenChar1 + "/" + illGenChar1; 
					
		} else if ("AB".equals(illuminaCall)) {
			return complement ? 
					BaseUtils.getComplement(illGenChar1) + "/" + BaseUtils.getComplement(illGenChar2)
					: illGenChar1 + "/" + illGenChar2; 
			
		}else if ("BB".equals(illuminaCall)) {
			return complement ? 
					BaseUtils.getComplement(illGenChar2) + "/" + BaseUtils.getComplement(illGenChar2)
					: illGenChar2 + "/" + illGenChar2; 
			
		} else {
			throw new IllegalArgumentException("Illumina call was not in correct format: " + illuminaCall);
		}
		
	}
	
	public static int[] getAllelicCounts(final int totalCount, final double logRRatio, final double bAlleleFrequency) {
		double totalCoverage = Math.pow(2 , logRRatio) * totalCount;
		int bAlleleCount =  (int) (totalCoverage * bAlleleFrequency);
		int aAlleleCount = (int) (totalCoverage - bAlleleCount);
		if (bAlleleCount < 0 || aAlleleCount < 0)
			System.out.println("got a negative allele count from following values: logRRatio: " 
					+ logRRatio + ", bAlleleFrequency: " + bAlleleFrequency);
		return new int[] {aAlleleCount, bAlleleCount};
	}
	
	public static int[] getAllelicCounts(final int totalCount, final double logRRatio, final int rawXIntensity, final int rawYIntensity) {
		double totalCoverage = Math.pow(2 , logRRatio) * totalCount;
		int totalIntensity = rawXIntensity + rawYIntensity;
		int bAlleleCount =  (int) (totalCoverage * ((double)rawYIntensity / totalIntensity));
		int aAlleleCount = (int) (totalCoverage - bAlleleCount);
		if (bAlleleCount < 0 || aAlleleCount < 0)
			System.out.println("got a negative allele count from following values: logRRatio: " 
					+ logRRatio + ", rawXIntensity: " + rawXIntensity + ", rawYIntensity: " + rawYIntensity);
		return new int[] {aAlleleCount, bAlleleCount};
	}
	
	

}
