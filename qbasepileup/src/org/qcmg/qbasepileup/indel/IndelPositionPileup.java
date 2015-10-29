/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup.indel;

import java.util.Map;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.SAMRecord;

import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.qbasepileup.InputBAM;
import org.qcmg.qbasepileup.Options;
import org.qcmg.qbasepileup.QBasePileupException;



public class IndelPositionPileup {	
	
	private IndelPosition position;	
	private IndelPileup tumourPileup;
	private IndelPileup normalPileup;
	private String pileupFlags = new String();
	private Options options;
	private IndexedFastaSequenceFile indexedFasta;
	
	public IndelPositionPileup(InputBAM tumourBam, InputBAM normalBam, org.qcmg.qbasepileup.indel.IndelPosition position, Options options, IndexedFastaSequenceFile indexedFastaFile) throws QBasePileupException {
		this.position = position;
		this.options = options;
		this.indexedFasta = indexedFastaFile;
		tumourPileup = new IndelPileup(options, tumourBam, position, options.getReference(), options.getSoftClipWindow(), options.getNearbyHomopolymerWindow(), options.getNearbyIndelWindow(), true);
        normalPileup = new IndelPileup(options, normalBam,  position, options.getReference(), options.getSoftClipWindow(), options.getNearbyHomopolymerWindow(), options.getNearbyIndelWindow(), false);      
	}
	
//	public IndelPositionPileup(InputBAM tumourBam, InputBAM normalBam, org.qcmg.qbasepileup.indel.IndelPosition position, String reference) throws QBasePileupException {
//		this.position = position;
//		tumourPileup = new IndelPileup(options, tumourBam, position,  new File(reference), 13, 10, 3, true); 
//        normalPileup = new IndelPileup(options, normalBam,  position, new File(reference), 13, 10, 3, false);      
//	}

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
        
        //parsePindelInput();        
	}
	
//	private void parsePindelInput() {
//		if (options.getPindelMutations() != null) {
//        	Map<String, String[]> pindelMutations = options.getPindelMutations();
//        	String key = position.getChromosome() + ":" + position.getStart() + ":" + position.getEnd() + ":" + position.getMutationType();
//        	if (pindelMutations.containsKey(key)) {
//        		String[] vals = pindelMutations.get(key);
//        		String qcmgCount = tumourPileup.getSupportingReads() + ";" + normalPileup.getSupportingReads();
//        		String pindelCount = vals[0];
//        		
////        		if (!pindelCount.equals(qcmgCount)) {
////        			String[] tumour = vals[1].split(";");
////            		String[] normal = vals[2].split(";");            		
////            		            		
////            		//analyseReads(key, "Tumour", tumourPileup, tumour);
////            		//analyseReads(key, "Normal", normalPileup, normal);
////        		}
//        	}        	
//        }		
//	}

//	private void analyseReads(String key, String type, IndelPileup pileup, String[] pindelReads) {
//		for (Map.Entry<String, SAMRecord> entry: pileup.getRecords().entrySet()) {			
//			boolean found = false;
//			for (String s: pindelReads) {
//				if (s.equals(entry.getKey())) {
//					found = true;					
//				}
//			}
////			if (!found && entry.getValue().getMappingQuality() > 20) {
////				System.out.println("not found: " + "\t" + key + "\t" + type + "\t" + entry.getValue().getSAMString());				
////			}
//		}
//		
//		for (String s: pindelReads) {
////			if (!pileup.getRecords().containsKey(s)) {
////				System.out.println("Pindel" + "\t" + key + "\t" + type + "\t" + s);
////			}
//		}
//		
//	}

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
		String[] values = position.getInputString().split("\t");
		
		for (int i=0; i<values.length; i++) {
			if (i == position.getTdColumn()) {
				output.append(tumourPileup.toDCCString() + "\t");
			} else if (i == position.getNdColumn()) {
				output.append(normalPileup.toDCCString() + "\t");
			} else if (i == position.getQCMGFlagColumn()) {				
				String flags = values[i];
				
				if (!pileupFlags.equals("")) {
					//add semi colon if required
					if (flags != "" && !flags.endsWith(";")) {
						flags += ";";
					}
					flags += pileupFlags;
					output.append(flags + "\t");
				} else {
					if (flags.equals("PASS;")) {
						output.append("PASS" + "\t");
					} else {
						output.append(values[i] + "\t");
					}
				}				
			} else {
				output.append(values[i] + "\t");
			}
		}        		
		output.append("\n");
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
