/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.indel.pileup;

import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.indel.Q3IndelException;

public class IndelPosition {
	
	private final List<VcfRecord> vcfs ; 	
	private final ChrRangePosition position; 
	private final int indelStart;
	private final int indelEnd; 	
	private final SVTYPE mutationType;	
	private IndelPileup tumourPileup;
	private IndelPileup normalPileup;
	
	/**
	 * retrieve information from a vcf record
	 * 
	 * @param re a vcf record
	 * @param type of SNP MNP etc
	 */
	public IndelPosition(VcfRecord re, SVTYPE type) { 
		vcfs = new ArrayList<>(); 
		vcfs.add(re);		
		
		this.mutationType = type;
		String fullChromosome = IndelUtils.getFullChromosome(re.getChromosome());	 	
		
		if (isInsertion()) { 
			this.indelStart = re.getPosition();
			this.indelEnd = re.getPosition() + 1;
		} else if (isDeletion()) { 
			this.indelStart = re.getPosition() + 1; 
			this.indelEnd = re.getPosition() + re.getRef().length() - 1; 
		} else { 
			this.indelStart = -1; 
			this.indelEnd = -1; 
		}
		
		int start = re.getPosition();
		int end = re.getChrPosition().getEndPosition();
		position = new ChrRangePosition(fullChromosome, start, end);		
	}

	//next job: check all vcfs are the same type, start and end
	public IndelPosition(List<VcfRecord> res, SVTYPE type) { 
		this(res.getFirst(), type);
		//append all vcfs
		vcfs.clear();
		vcfs.addAll(res);
	}
	
	public IndelPosition(VcfRecord re) { 
		this( re, IndelUtils.getVariantType(re.getRef(), re.getAlt()) );		
	}
	
	public VcfRecord getIndelVcf(int index) { 	
		return vcfs.get(index); 
	}
	
	public List<VcfRecord> getIndelVcfs() { 
		return vcfs; 
	}
	
	public void addVcf(VcfRecord vcf) throws Q3IndelException {
		if ( ! IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt() ).equals(mutationType) ) {
			throw new Q3IndelException("Variant types do not match! Expected " + mutationType + " but was: " + IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt() ));			
		}
				
		vcfs.add(vcf);
	}
	
	public SVTYPE getIndelType() { 
		return mutationType; 
	}

	/**  
	 * convert chromosome to full name. eg return 
	 * ChrRangePosition( "chr1", 99, 99 ) for vcf "1 99 . A ATT"
	 * ChrRangePosition( "chr1", 99, 101 ) for vcf "1 99 . ATT A"
	 * 
	 * @return ChrRangePosition from vcfRecord 
	 * 
	 */
	public ChrRangePosition getChrRangePosition() {	
		return position; 
	}
	
	/**
	 * return indel start position, eg return 99 for INS "chr 99 . A ATT"; return 100 for DEL "chr 99 . ATT A"
	 * 
	 * @return Indel start.
	 *   
	 */
	public int getStart() {	
		return indelStart;
	}
	
	/**
	 * return indel end position, eg return 100 for INS "chr 99 . A ATT"; return 101 for DEL "chr 99 . ATT A"
	 * 
	 * @return Indel end positon.
	 *      
	 */
	public int getEnd() {		 
		return indelEnd; 
	}
	
	public String getFullChromosome() {		
		return position.getChromosome();	 
    }
	
	public boolean isInsertion() {		
		return  mutationType.equals(SVTYPE.INS); 
	}

	public boolean isDeletion() { 
		return  mutationType.equals(SVTYPE.DEL); 		 
	}		
	
	public String getMotif(int index) {	
		
		if (mutationType.equals(SVTYPE.INS)) {
			return vcfs.get(index).getAlt().substring(1);	
		} else if (mutationType.equals(SVTYPE.DEL)) { 
			return vcfs.get(index).getRef().substring(1);
		}
		
		return null; 
	}
		
	public List<String> getMotifs( ) {	
		List<String> motifs = new ArrayList<>(vcfs.size() + 1);
		for (int i = 0; i < vcfs.size(); i ++) {
			motifs.add(i, getMotif(i));
		}
		return motifs; 
	}
	
	
	@Override
    public boolean equals(final Object o) {
	       
        if (!(o instanceof IndelPosition other)) {
        	return false;
        }

        if (! this.mutationType .equals(other.mutationType)) {
    		return false;
    	}

        return this.position.equals(other.position);
    }
	    
    @Override
    public int hashCode() { 
		return position.hashCode();
    }
    
	@Override
	public String toString() {
		return this.position.toString() + ":" + mutationType + ": variants number: " + vcfs.size();
	}

	public void setPileup(boolean isTumour, IndelPileup pileup) { 
		if (isTumour) { 
			 this.tumourPileup = pileup;
		} else { 
			this.normalPileup = pileup;
		}
	}
	
	public VcfRecord getPileupedVcf(int index, final int gematicNNS, final float gematicSOI) { 
		VcfRecord re = vcfs.get(index);		
		
		//not interested in these indels since over coverage		
		if (tumourPileup != null  && tumourPileup.getTotalCount() > 1000) { 
			re.setFilter( IndelUtils.FILTER_HCOVT);
			return re; 
		}
	 	
		if (normalPileup != null  && normalPileup.getTotalCount() > 1000) { 
			re.setFilter( IndelUtils.FILTER_HCOVN);
			return re; 
		}		
		
		boolean somatic = re.getFilter().equals(ReadIndels.FILTER_SOMATIC);
 		if (normalPileup != null && somatic) { 
 			if ( normalPileup.getSupportReadCount(index) > gematicNNS ) { 
				somatic = false;
 			} else if (normalPileup.getInformativeCount() > 0) { 
				int scount = normalPileup.getSupportReadCount(index);
				int icount = normalPileup.getInformativeCount();
				if (((float) (100 * scount) / icount) >= (gematicSOI * 100)) {
					somatic = false; 
				}
			} 
		}
		
		if (somatic) { 
			re.appendInfo(VcfHeaderUtils.INFO_SOMATIC);
		}
		
		//set default filter as PASS
		re.setFilter(VcfHeaderUtils.FILTER_PASS);
		String td = ".", nd = ".";
		List<String> genotypeField =  re.getFormatFields();
		if (tumourPileup != null) { 		
			if (tumourPileup.getTotalCount() > 0) { 
				 td = String.format("%d,%d,%d,%d[%d,%d],%d[%d],%d,%d,%d", 
						 tumourPileup.getStrongSupportNovelStartReadCount(index),tumourPileup.getTotalCount(),
						 tumourPileup.getInformativeCount(),  tumourPileup.getStrongSupportReadCount(index),
						 tumourPileup.getForwardSupportReadCount(index),tumourPileup.getBackwardSupportReadCount(index),
						 tumourPileup.getSupportReadCount(index),tumourPileup.getSupportNovelStartReadCount(index), 
						 tumourPileup.getPartialReadCount(index),tumourPileup.getNearbyIndelCount(),tumourPileup.getNearybySoftclipCount());
			}
			
			if (!somatic && tumourPileup.getTotalCount() < 8) { 
				VcfUtils.updateFilter(re,  IndelUtils.FILTER_COVT);
			}
			
			if (tumourPileup.getPartialReadCount(index) >= 3 
					&&	(100 * tumourPileup.getPartialReadCount(index) / tumourPileup.getTotalCount()) > 10) { 
				VcfUtils.updateFilter(re,IndelUtils.FILTER_TPART);
			}
			
			if (somatic && tumourPileup.getSupportReadCount(index) >= 3 && tumourPileup.hasStrandBias(index, 0.1)) { 
				VcfUtils.updateFilter(re,IndelUtils.FILTER_TBIAS);
			}
		}	
		
		//String nd = "ND=0:0:0:0:0:0:0";
		if ( normalPileup != null) { 
			if ( normalPileup.getTotalCount() > 0) {
				nd = String.format("%d,%d,%d,%d[%d,%d],%d[%d],%d,%d,%d", 
						normalPileup.getStrongSupportNovelStartReadCount(index),normalPileup.getTotalCount(),
						normalPileup.getInformativeCount(), normalPileup.getStrongSupportReadCount(index),
						normalPileup.getForwardSupportReadCount(index),normalPileup.getBackwardSupportReadCount(index),
						normalPileup.getSupportReadCount(index), normalPileup.getSupportNovelStartReadCount(index),  
						normalPileup.getPartialReadCount(index),normalPileup.getNearbyIndelCount(),normalPileup.getNearybySoftclipCount());
			}
			
			//re.appendInfo("ND=" + nd);				
			if (somatic && normalPileup.getTotalCount() < 12) {
				VcfUtils.updateFilter(re,IndelUtils.FILTER_COVN12);
			}
			
			if (!somatic && normalPileup.getTotalCount() < 8) {
				VcfUtils.updateFilter(re,IndelUtils.FILTER_COVN8);
			}	
			
			if (somatic && normalPileup.getSupportNovelStartReadCount(index) > 0) {
				VcfUtils.updateFilter(re,IndelUtils.FILTER_MIN);
			}
			
			if (normalPileup.getPartialReadCount(index) >= 3 
				&& (100 * normalPileup.getPartialReadCount(index) / normalPileup.getTotalCount()) > 5) {
				VcfUtils.updateFilter(re,IndelUtils.FILTER_NPART);
			}
			
			if (!somatic && normalPileup.getSupportReadCount(index) >= 3 && normalPileup.hasStrandBias(index, 0.05)) {
				VcfUtils.updateFilter(re,IndelUtils.FILTER_NBIAS);	
			}
		}
					 
		//future job should check GT column	
		//control always on first column and then test
		List<String> field = new ArrayList<>();
		field.add( 0,  (!genotypeField.isEmpty()) ? genotypeField.get(0) + ":" + IndelUtils.FORMAT_ACINDEL : IndelUtils.FORMAT_ACINDEL );
		field.add( 1,  (genotypeField.size() > 1) ? genotypeField.get(1) + ":" + nd : nd);
		field.add( 2,  (genotypeField.size() > 2) ? genotypeField.get(2) + ":" + td : td);					
		re.setFormatFields( field ); 
				
					 
		IndelPileup pileup = (somatic) ? tumourPileup : normalPileup; 				
		
		if (pileup != null  && pileup.getStrongSupportNovelStartReadCount(index) < 4) { 
			VcfUtils.updateFilter(re,IndelUtils.FILTER_NNS);
		}
		
		float nn = (pileup == null || pileup.getTotalCount() == 0) ? 0 : (float) pileup.getNearbyIndelCount() / pileup.getTotalCount();
		float ss = (pileup == null || pileup.getInformativeCount() == 0) ? 0 : (float) pileup.getStrongSupportReadCount(index) / pileup.getInformativeCount();
		re.appendInfo(IndelUtils.INFO_NIOC + "=" + ((nn == 0 ) ? "0" : String.format("%.3f", nn)));		
		re.appendInfo(IndelUtils.INFO_SSOI + "=" + ((ss == 0 ) ? "0" : String.format("%.3f", ss)));	
				
		re.appendInfo("SVTYPE=" + this.mutationType.name());
		re.appendInfo("END=" + indelEnd);
		re.appendInfo(VcfHeaderUtils.INFO_MERGE_IN + "=1");
					
		return re;
	}

}
