/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.indel;


import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.util.Constants;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupException;

public class IndelPositionPileup {	
	
	private IndelPosition position;	
	private IndelPileup tumourPileup;
	private IndelPileup normalPileup;
	private String pileupFlags = "";
	private final Options options;
	private final IndexedFastaSequenceFile indexedFasta;
	
	public IndelPositionPileup(InputBAM tumourBam, InputBAM normalBam, org.qcmg.qbasepileup.indel.IndelPosition position, Options options, IndexedFastaSequenceFile indexedFastaFile) throws QBasePileupException {
		this.position = position;
		this.options = options;
		this.indexedFasta = indexedFastaFile;
		tumourPileup = new IndelPileup(options, tumourBam, position, options.getReference(), options.getSoftClipWindow(), options.getNearbyHomopolymerWindow(), options.getNearbyIndelWindow(), true);
        normalPileup = new IndelPileup(options, normalBam,  position, options.getReference(), options.getSoftClipWindow(), options.getNearbyHomopolymerWindow(), options.getNearbyIndelWindow(), false);      
	}

	public IndelPosition getPosition() {
		return position;
	}

	public void setPosition(IndelPosition position) {
		this.position = position;
	}

	public IndelPileup getTumourPileup() {
		return tumourPileup;
	}

	public void setTumourPileup(IndelPileup tumourPileup) {
		this.tumourPileup = tumourPileup;
	}

	public IndelPileup getNormalPileup() {
		return normalPileup;
	}

	public void setNormalPileup(IndelPileup normalPileup) {
		this.normalPileup = normalPileup;
	}

	//pileup reads by query bam across indel positions
	public void pileupReads(QueryExecutor exec) throws Exception {		
	    tumourPileup.pileupReads(exec, indexedFasta);
        normalPileup.pileupReads(exec, indexedFasta);
        this.pileupFlags = calculatePileupFlags();
        
	}

	//pileup single reads only
	public void pileupRead(SAMRecord r, boolean isTumour) {
		
		if (isTumour) {
			if (!tumourPileup.isHighCoverage()) {
				tumourPileup.pileupRead(r);
			}
		} else {
			if (!normalPileup.isHighCoverage()) {
				normalPileup.pileupRead(r);
			}
		}		
	}

	public String calculatePileupFlags() {		
		StringBuilder flags = new StringBuilder();
		
		if (position.isSomatic() && tumourPileup.getNovelStartCount() < 4) {
			flags.append("NNS;");
		} 
		if (position.isGermline() && tumourPileup.getTotalReads() < 8) {
			flags.append("COVT;");
		}
		if (tumourPileup.isHighCoverage()) {
			flags.append("HCOVT;");
		}
		if (normalPileup.isHighCoverage()) {
			flags.append("HCOVN;");
		}
		if (position.isSomatic() && normalPileup.getNovelStartCount() > 0) {
			flags.append("MIN;");
		}
		if (position.isGermline() && normalPileup.getTotalReads() < 8) {
			flags.append("COVN8;");
		}
		if (position.isSomatic() && normalPileup.getTotalReads() < 12) {
			flags.append("COVN12;");
		}
		
		if (normalPileup.getPartialIndelCount() >= 3 && normalPileup.getPercentPartial() > 5) {
			flags.append("NPART;");
		}
		if (tumourPileup.getPartialIndelCount() >=3 && tumourPileup.getPercentPartial() > 10) {
			flags.append("TPART;");
		}
		
		if (position.isSomatic() && tumourPileup.hasStrandBias()) {
			flags.append("TBIAS;");
		}
		if (position.isGermline() && normalPileup.hasStrandBias()) {
			flags.append("NBIAS;");
		}
		
		
		//homopolymer type
		Homopolymer hp = tumourPileup.getHomopolymer();
		if (hp != null && !hp.getHomopolymerCount().equals("ne")) {
			String type = hp.getType();
			String count = hp.getHomopolymerCount();
			if (type.equals(Homopolymer.DISCONTIGUOUS)) {
				flags.append("HOMADJ_" + count);
			} else if (type.equals(Homopolymer.CONTIGUOUS)) {
				flags.append("HOMCON_" + count);
			} else if (type.equals(Homopolymer.EMBEDDED)) {
				flags.append("HOMEMB_" + count);
			}			
		}		
		
		String flagStr = flags.toString();
		if (flagStr.endsWith(";")) {
			return flagStr.substring(0, flagStr.lastIndexOf(";"));
		} else {
			return flagStr;
		}
	}
	
	public String toDCCString() {
		StringBuilder output = new StringBuilder();
        
        //get the original string
		String[] values = position.getInputString().split(Constants.TAB_STRING);
		
		for (int i=0; i<values.length; i++) {
			if (i == position.getTdColumn()) {
				output.append(tumourPileup.toDCCString()).append(Constants.TAB);
			} else if (i == position.getNdColumn()) {
				output.append(normalPileup.toDCCString()).append(Constants.TAB);
			} else if (i == position.getQCMGFlagColumn()) {				
				String flags = values[i];
				
				if ( ! pileupFlags.equals("")) {
					//add semi colon if required
					if ( ! "".equals(flags) && ! flags.endsWith(";")) {
						flags += ";";
					}
					flags += pileupFlags;
					output.append(flags).append(Constants.TAB);
				} else {
					if (flags.equals("PASS;")) {
						output.append("PASS\t");
					} else {
						output.append(values[i]).append(Constants.TAB);
					}
				}				
			} else {
				output.append(values[i]).append(Constants.TAB);
			}
		}        		
		output.append(Constants.NL);
        return output.toString();
	}

	public void finish() {
		tumourPileup.finish(indexedFasta);
		normalPileup.finish(indexedFasta);
		this.pileupFlags = calculatePileupFlags();	
	}
	
	public String getPileupFlags() {
		return pileupFlags;
	}

	public void setPileupFlags(String pileupFlags) {
		this.pileupFlags = pileupFlags;
	}

}
