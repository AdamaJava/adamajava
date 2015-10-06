/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.indel.pileup;

 
import java.util.ArrayList;
import java.util.List;

import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.util.IndelUtils;
import org.qcmg.common.util.IndelUtils.SVTYPE;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import au.edu.qimr.indel.pileup.Homopolymer.HOMOTYPE;

public class IndelPosition {
	
	private final List<VcfRecord> vcfs ; 	
	private final ChrPosition position; 
	//indel start end is not eauql to positon.start and end
	private final int indelStart, indelEnd; 
	
	private final SVTYPE mutationType;	
	private IndelPileup tumourPileup;
	private IndelPileup normalPileup;
	private Homopolymer polymer;	

	/**
	 * retrive information from a vcf record
	 * @param re: a vcf record
	 * @param type
	 * @param indelFileType
	 * @param isGermline
	 */
	public IndelPosition(VcfRecord re, SVTYPE type){
		vcfs = new ArrayList<VcfRecord>(); 
		
		VcfRecord vcf = re; 				
		vcfs.add(re);		
		
		this.mutationType = type;
		String fullChromosome = IndelUtils.getFullChromosome(vcf.getChromosome());
	 	
		
		if (isInsertion()) {
			this.indelStart = vcf.getPosition();
			this.indelEnd = vcf.getPosition()+1;
		} else if (isDeletion()){
			this.indelStart = vcf.getPosition() + 1;
			this.indelEnd = vcf.getPosition() + vcf.getRef().length() - 1;
		} else {
			this.indelStart = -1;
			this.indelEnd = -1; 
		}
		
		int start = vcf.getPosition();
		int end = vcf.getChrPosition().getEndPosition();
		position = new ChrPosition(fullChromosome, start, end);
		
	}

	//next job: check all vcfs are same type, start and end
	public IndelPosition(List<VcfRecord> res, SVTYPE type) throws Exception{
		this(res.get(0), type);		
		//append all vcfs
		vcfs.clear();
		vcfs.addAll(res);

	}
	
	public IndelPosition(VcfRecord re){
		this(re, IndelUtils.getVariantType(re.getRef(), re.getAlt() ));		
	}
	
	public IndelPosition(List<VcfRecord> res ) throws Exception{
		this(res, IndelUtils.getVariantType(res.get(0).getRef(), res.get(0).getAlt() ) );
				
	}
	
	public VcfRecord getIndelVcf(int index){		
		return vcfs.get(index); 
	}
	
	public List<VcfRecord> getIndelVcfs( ){		
		return vcfs; 
	}
	
	public void addVcf(VcfRecord vcf) throws Exception{
		if(!IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt() ).equals(mutationType) )
			throw new Exception();
				
		vcfs.add(vcf);
//		motifs.add(vcf.getAlt().substring(1));		
	}
	
	public SVTYPE getIndelType(){
		return mutationType; 
	}

	public ChrPosition getChrPosition(){		
		return position; 
	}
	 
	public int getStart() {	
		return indelStart;
	}
	
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
	
	public boolean isComplex() {
		return mutationType.equals(SVTYPE.CTX);
	}
	
	public String getMotif(int index) {	
		
		if(mutationType.equals(SVTYPE.INS))
			return vcfs.get(index).getAlt().substring(1);			 
		else if(mutationType.equals(SVTYPE.DEL))
			return vcfs.get(index).getRef().substring(1);
		
		return null; 
	}
	
	
	public List<String> getMotifs( ) {	
		List<String> motifs = new ArrayList<String>();
		for(int i = 0; i < vcfs.size(); i ++)
			motifs.add(i, getMotif(i));
		
		return motifs; 
	}
	
	
	@Override
    public boolean equals(final Object o) {
	       
        if (!(o instanceof IndelPosition)) return false;
        
        final IndelPosition other = (IndelPosition) o;
             	
    	if (! this.mutationType .equals(other.mutationType))  
    		return false;
    	
    	if( ! this.position.equals(other.position))
    		return false;
    	
     
        return true; 
    }
	    
    @Override
    public int hashCode() { 
				
		return position.hashCode();
   }
    
	@Override
	public String toString() {
		return this.position.toString() + ":" + mutationType + ": variants number: " + vcfs.size();
	}

	public void setPileup(boolean isTumour, IndelPileup pileup){
		if(isTumour)
			 this.tumourPileup = pileup;
		else
			this.normalPileup = pileup;
	}
	
	public void setHomopolymer(Homopolymer polymer){
		this.polymer = polymer; 
	}
	
	public VcfRecord getPileupedVcf(int index){
		VcfRecord re = vcfs.get(index);
		
		//not interested int these indels since over coverage
		if(tumourPileup != null  && tumourPileup.getTotalCount() > 1000){
			re.setInfo( IndelUtils.INFO_HCOVT);
			return re; 
		}
		
		if(normalPileup != null  && normalPileup.getTotalCount() > 1000){
			re.setInfo( IndelUtils.INFO_HCOVN);
			return re; 
		}
		
		//decide somatic or not
		boolean somatic = true;
		if(polymer != null && !polymer.getType(index).equals(HOMOTYPE.NONE))  somatic = false; 
		else if(tumourPileup != null && tumourPileup.getNearbyIndelCount() > 0 ) somatic = false;
		else if(normalPileup != null){
			if( normalPileup.getnovelStartReadCount(index)  >= 3) somatic = false;
			else if(normalPileup.getInformativeCount() > 0){
				int scount =   normalPileup.getsuportReadCount(index);
				int icount =   normalPileup.getInformativeCount();
				if((scount * 100 / icount) >= 10 ) somatic = false; 
			}
		}
		
		if(somatic) 
			re.setFilter(VcfHeaderUtils.INFO_SOMATIC);
		 
		//String td = "TD=0:0:0:0:0:0:0";
		if(tumourPileup != null){ 		
			String td = String.format("ND=%d:%d:%d:%d[%d,%d]:%d:%d:%d", tumourPileup.getnovelStartReadCount(index),tumourPileup.getTotalCount(),tumourPileup.getInformativeCount(), 
					tumourPileup.getsuportReadCount(index),tumourPileup.getforwardsuportReadCount(index),tumourPileup.getbackwardsuportReadCount(index),
					tumourPileup.getparticalReadCount(index),tumourPileup.getNearbyIndelCount(),tumourPileup.getNearybySoftclipCount());
	
			re.appendInfo(td);
			if(!somatic && tumourPileup.getTotalCount() < 8)
				re.addFilter(IndelUtils.INFO_COVT);
			if(somatic && tumourPileup.getnovelStartReadCount(index) < 4 )
				re.addFilter(IndelUtils.INFO_NNS);
			if(tumourPileup.getparticalReadCount(index) > 3 &&
					(100 * tumourPileup.getparticalReadCount(index) / tumourPileup.getTotalCount()) > 10)
				re.addFilter(IndelUtils.INFO_TPART);
			if(somatic && tumourPileup.getsuportReadCount(index) >=3 && tumourPileup.hasStrandBias(index, 0.1, 0.9))
				re.addFilter(IndelUtils.INFO_TBIAS);
		}	
		
		//String nd = "ND=0:0:0:0:0:0:0";
		if(normalPileup != null){
			String nd = String.format("ND=%d:%d:%d:%d[%d,%d]:%d:%d:%d", normalPileup.getnovelStartReadCount(index),normalPileup.getTotalCount(),normalPileup.getInformativeCount(), 
					normalPileup.getsuportReadCount(index),normalPileup.getforwardsuportReadCount(index),normalPileup.getbackwardsuportReadCount(index),
					normalPileup.getparticalReadCount(index),normalPileup.getNearbyIndelCount(),normalPileup.getNearybySoftclipCount());
			re.appendInfo(nd);	
			
			if(somatic && normalPileup.getTotalCount() < 12)
				re.addFilter(IndelUtils.INFO_COVN12);
			if(!somatic && normalPileup.getTotalCount() < 8)
				re.addFilter(IndelUtils.INFO_COVN8);			
			if(somatic && normalPileup.getnovelStartReadCount(index) > 0)
				re.addFilter(IndelUtils.INFO_MIN);
			if(normalPileup.getparticalReadCount(index) > 3 &&
					(100 * normalPileup.getparticalReadCount(index) / normalPileup.getTotalCount()) > 5)
				re.addFilter(IndelUtils.INFO_NPART);
			if(somatic && normalPileup.getsuportReadCount(index) >=3 && normalPileup.hasStrandBias(index, 0.05, 0.95))
				re.addFilter(IndelUtils.INFO_TBIAS);			 
		}
				
		if(polymer != null && !polymer.getHOMOTYPE(index).equals(HOMOTYPE.NONE)){
			String po = String.format("%s=%d,%s", polymer.getType(index).name(), polymer.getBaseCount(index), polymer.getPolymerSequence(index) );
			re.appendInfo(po);
		}	 
		 
		


		return re; 	
	}
 


}
