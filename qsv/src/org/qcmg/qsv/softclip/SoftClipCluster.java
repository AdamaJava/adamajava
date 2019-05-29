/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.softclip;

import static org.qcmg.common.util.Constants.TAB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.assemble.QSVAssemble;
import org.qcmg.qsv.blat.BLAT;
import org.qcmg.qsv.splitread.UnmappedRead;
import org.qcmg.qsv.util.QSVConstants;
import org.qcmg.qsv.util.QSVUtil;

import gnu.trove.map.TIntObjectMap;

public class SoftClipCluster implements Comparable<SoftClipCluster> {
	
	
	final String name;
	Breakpoint leftBreakpointObject;
	Breakpoint rightBreakpointObject;
	private String mutationType;
	private String leftReference;
	private String rightReference;
	private char leftStrand;
	private char rightStrand;
	private Integer leftBreakpoint;
	private Integer rightBreakpoint;
	private boolean hasClusterMatch = false;
	private boolean hasMatchingBreakpoints;
	private char rightMateStrand;
	private char leftMateStrand;
	private boolean hasClipMatch;
	private boolean oneSide;
	private boolean rescuedClips;
	private boolean alreadyMatched;
	private String orientationCategory = "";


	public SoftClipCluster(Breakpoint leftBreakpoint) {
		this.name = leftBreakpoint.getName();
		this.leftBreakpointObject = leftBreakpoint;		
		setStartAndEnd();		
		this.rightBreakpointObject = null;	
		this.oneSide = true;
		this.mutationType = defineMutationType();
	}
	
	public SoftClipCluster(Breakpoint leftBreakpoint, Breakpoint rightBreakpoint) {
		this.name = (leftBreakpoint.getName().compareTo(rightBreakpoint.getName()) > 0) 
				? rightBreakpoint.getName() + ":" + leftBreakpoint.getName() : leftBreakpoint.getName() + ":" + rightBreakpoint.getName(); 
		this.leftBreakpointObject = leftBreakpoint;
		this.rightBreakpointObject = rightBreakpoint;
		this.hasMatchingBreakpoints = true;
		
		this.oneSide = leftBreakpoint.getClipsSize() == 0 || rightBreakpoint.getClipsSize() == 0;
		setStartAndEnd();
		this.mutationType = defineMutationType();
	}

	private void setStartAndEnd() {
		if (this.leftBreakpointObject == null) {
			this.leftBreakpoint = rightBreakpointObject.getMateBreakpoint();
			this.leftReference = rightBreakpointObject.getMateReference();
			this.leftStrand = rightBreakpointObject.getMateStrand();
			this.rightBreakpoint = rightBreakpointObject.getBreakpoint();			
			this.rightReference = rightBreakpointObject.getReference();
			this.rightStrand = rightBreakpointObject.getStrand();	
			this.rightMateStrand = rightBreakpointObject.getMateStrand();
			this.leftMateStrand = rightBreakpointObject.getStrand();
		} else {
			this.leftBreakpoint = leftBreakpointObject.getBreakpoint();
			this.leftReference = leftBreakpointObject.getReference();
			this.leftStrand = leftBreakpointObject.getStrand();
			this.leftMateStrand = leftBreakpointObject.getMateStrand();
			if (rightBreakpointObject == null) {
				this.rightBreakpoint = leftBreakpointObject.getMateBreakpoint();			
				this.rightReference = leftBreakpointObject.getMateReference();
				this.rightStrand = leftBreakpointObject.getMateStrand();
				this.rightMateStrand = leftBreakpointObject.getStrand();				
			} else {
				this.rightBreakpoint = rightBreakpointObject.getBreakpoint();		
				this.rightReference = rightBreakpointObject.getReference();
				this.rightStrand = rightBreakpointObject.getStrand();	
				this.rightMateStrand = rightBreakpointObject.getMateStrand();
			}
		}		
	}	

	public String defineMutationType() {
		
		if (oneSide) {
			if ( ! leftReference.equals(rightReference)) {
				checkOrder();
				return "CTX";
			} else {
				String mut = findSingleSideMutationType();
				orientationCategory = "";
				checkOrder();
				return mut;
			}
			
		} else {
			checkOrder();
			return findTwoSidedMutationType();	
			
		}
	}

	public String findTwoSidedMutationType() {
		if ( ! leftBreakpointObject.isLeft() && rightBreakpointObject.isLeft()) {
			
			if (leftBreakpointObject.getStrand() == leftBreakpointObject.getMateStrand() && rightBreakpointObject.getStrand() == rightBreakpointObject.getMateStrand()) {
				orientationCategory = "1";	
				
				return leftReference.equals(rightReference) ? "DEL/ITX" : "CTX";
			} 				
			
		} else if (leftBreakpointObject.isLeft() && !rightBreakpointObject.isLeft()) {
			if (leftBreakpointObject.getStrand() == leftBreakpointObject.getMateStrand() && rightBreakpointObject.getStrand() == rightBreakpointObject.getMateStrand()) {
				orientationCategory = QSVConstants.ORIENTATION_2;				
				return leftReference.equals(rightReference) ? "DUP/INS/ITX" : "CTX";
			}			
		} else if ((leftBreakpointObject.isLeft() && rightBreakpointObject.isLeft()) ||
				( ! leftBreakpointObject.isLeft() && !rightBreakpointObject.isLeft())) {
			if (leftBreakpointObject.getStrand() != leftBreakpointObject.getMateStrand() && rightBreakpointObject.getStrand() != rightBreakpointObject.getMateStrand()) {
				if (leftBreakpointObject.isLeft() && rightBreakpointObject.isLeft()) {
					orientationCategory = QSVConstants.ORIENTATION_4;
				}
				if ( ! leftBreakpointObject.isLeft() && ! rightBreakpointObject.isLeft()) {
					orientationCategory = QSVConstants.ORIENTATION_3;
				}
				return leftReference.equals(rightReference) ? "INV/ITX" : "CTX";
			}			
		}
		
		return "ITX";	
	}

	public String findSingleSideMutationType() {
		Breakpoint bpObject = getSingleBreakpoint();
		
		if ( ! bpObject.getMatchingStrands()) {
			return "ITX";
		}

		if (bpObject.isLeft()) {
			if (bpObject.getBreakpoint().intValue() < bpObject.getMateBreakpoint()) {
				return  "DUP/INS/ITX" ;
			} else {
				return "DEL/ITX" ;
			}
		} else {
			if (bpObject.getBreakpoint() < bpObject.getMateBreakpoint()) {
				return "DEL/ITX";
			} else {
				return "DUP/INS/ITX";
			}
		}
	}

	public void checkOrder() {
		if (leftReference.equals(rightReference)) {
			// wrong order: swap the records
            if (leftBreakpoint.intValue() > rightBreakpoint.intValue()) {
	            	swapBreakpoints();
            }

          // on different chromosomes
        } else {
            if ( QSVUtil.reorderByChromosomes(leftReference, rightReference)) {
	            	swapBreakpoints();
            }
        }
	}
	
	public void swapBreakpoints() {
		if (rightBreakpointObject != null) {
			Breakpoint temp = leftBreakpointObject;			
			leftBreakpointObject = rightBreakpointObject;
			rightBreakpointObject = temp;			
		} else {
			this.rightBreakpointObject = this.leftBreakpointObject;
			this.leftBreakpointObject = null;
		}
		setStartAndEnd();
	}

	@Override
	public int compareTo(SoftClipCluster o) {
		return this.name.compareTo(o.getName());
	}
	
	public String getName() {
		return name;
	}

	public boolean isRescuedClips() {
		return rescuedClips;
	}
	
	public boolean isOneSide() {
		return oneSide;
	}

	public Breakpoint getLeftBreakpointObject() {
		return leftBreakpointObject;
	}
	
	public Breakpoint getRightBreakpointObject() {
		return rightBreakpointObject;
	}

	public Integer getLeftBreakpoint() {
		return leftBreakpoint;
	}

	public Integer getRightBreakpoint() {
		return rightBreakpoint;
	}

	public String getRightReference() {
		return rightReference;
	}

	public String getLeftReference() {
		return leftReference;
	}

	public String getMutationType() {
		return this.mutationType;
	}
	
	public void setHasClusterMatch(boolean b) {
		this.hasClusterMatch = b;		
	}

	public boolean hasClusterMatch() {
		return hasClusterMatch;
	}

	public boolean hasMatchingBreakpoints() {
		return hasMatchingBreakpoints;
	}
	
	public boolean findMatchingBreakpoints() {
		return getLeftBreakpointObject() != null 
				&& getRightBreakpointObject() != null 
				&& getRightBreakpointObject().getClipsSize() > 0 
				&& getLeftBreakpointObject().getClipsSize() > 0;
	}

	public boolean hasClipMatch() {
		return this.hasClipMatch;
	}

	public void setHasClipMatch(boolean clipMatch) {
		this.hasClipMatch = clipMatch;	
	}	
	
	public String getOrientationCategory() {
		return orientationCategory;
	}

	public boolean findMatchingBreakpoints(SoftClipCluster compare) {
		
		Breakpoint left = getSingleBreakpoint();
		Breakpoint right = compare.getSingleBreakpoint();
		
		if (left != null && right != null) {
			if (left.getReference().equals(right.getMateReference()) && right.getReference().equals(left.getMateReference())) {
				
				Integer leftBPPosition = left.compare(right.getReference(), right.getBreakpoint());
				Integer rightBPPosition = right.compare(left.getReference(), left.getBreakpoint());			
	
				if (leftBPPosition != null && rightBPPosition != null) {
					this.hasMatchingBreakpoints = true;
					return true;
				}
			}
		}
		
		return false;
	}	
	
	public boolean isGermline() {
		return (leftBreakpointObject != null && leftBreakpointObject.isGermline())
				|| (rightBreakpointObject != null && rightBreakpointObject.isGermline());
		
	}

	public int getRealRightBreakpoint() {
		return this.rightBreakpointObject != null ?  rightBreakpointObject.getBreakpoint().intValue()
				: rightBreakpoint.intValue();
	}
	
	public int getRealLeftBreakpoint() {
		return this.leftBreakpointObject != null ? leftBreakpointObject.getBreakpoint().intValue() 
				: rightBreakpointObject.getMateBreakpoint();
	}

	public int getClipCount(boolean isTumour, boolean leftPos) {		
		
		if (leftBreakpointObject != null) {
			if (leftPos && !orientationCategory.equals(QSVConstants.ORIENTATION_2)) {
				return getLeftClipCount(isTumour);								
			} else if (!leftPos && orientationCategory.equals(QSVConstants.ORIENTATION_2)) {
				return getRightClipCount(isTumour);	
			}
		}
		if (rightBreakpointObject != null) {
			if (!leftPos && !orientationCategory.equals(QSVConstants.ORIENTATION_2)) {
				return getRightClipCount(isTumour);								
			} else {
				if (leftPos && orientationCategory.equals(QSVConstants.ORIENTATION_2)) {
					return getLeftClipCount(isTumour);		
				}
			}
		}
		return 0;
	}

	private int getLeftClipCount(boolean isTumour) {
		return isTumour ? leftBreakpointObject.getTumourClips().size() : leftBreakpointObject.getNormalClips().size();
	}
	
	private int getRightClipCount(boolean isTumour) {
		return isTumour ? rightBreakpointObject.getTumourClips().size() :  rightBreakpointObject.getNormalClips().size();
	}

	public String getStrand() {
		return this.leftStrand + "|" + this.rightStrand;
	}

	public Breakpoint getSingleBreakpoint() {
		if (leftBreakpointObject != null && rightBreakpointObject != null) {
			return null;
		} else if (leftBreakpointObject != null ) {
			return this.leftBreakpointObject;
		} else {
			return this.rightBreakpointObject;
		}
	}

	public String getClips(boolean isLeft, boolean isTumour) {
		if (isLeft) {
			if (leftBreakpointObject != null && ! leftBreakpointObject.isRescued()) {
				return isTumour ? leftBreakpointObject.getTumourClipString() : leftBreakpointObject.getNormalClipString();
			}
		} else {
			if (rightBreakpointObject != null && ! rightBreakpointObject.isRescued()) {
				return isTumour ? rightBreakpointObject.getTumourClipString() :  rightBreakpointObject.getNormalClipString();
			}
		}
		return "";
	}
	
	public String getLowConfClips(boolean isLeft, boolean isTumour) {
		if (isLeft) {
			if (leftBreakpointObject != null) {
				if (leftBreakpointObject.isRescued()) {
					if (isTumour) {
						return leftBreakpointObject.getTumourClipString();
					} else {
						return leftBreakpointObject.getNormalClipString();
					}
				}
			}
		} else {
			if (rightBreakpointObject != null) {
				if (rightBreakpointObject.isRescued()) {
					if (isTumour) {
						return rightBreakpointObject.getTumourClipString();
					} else {
						return rightBreakpointObject.getNormalClipString();
					}
				}
			}
		}
		return "";
	}
	
	public String getFullName() {
		return this.name + TAB + leftReference + TAB + leftBreakpoint + TAB + leftStrand + TAB + rightReference + TAB + rightBreakpoint + TAB + leftMateStrand + TAB;
	}

	public String getLeftInfo() {
		int size = 0;
		if (leftBreakpointObject != null) {
			size = leftBreakpointObject.getClipsSize(); 
		}
		return this.name + TAB + leftBreakpoint + TAB + leftStrand + TAB + rightBreakpoint + TAB + leftMateStrand + TAB + size;
	}
	
	public String getRightInfo() {
		int size = 0;
		if (rightBreakpointObject != null) {
			size = rightBreakpointObject.getClipsSize(); 
		}
		return this.name + TAB + rightBreakpoint + TAB + rightStrand + TAB + rightMateStrand + TAB + size; 
	}

	public void rescueClips(QSVParameters p, TIntObjectMap<int[]> cache, File tumourFile, File normalFile, String softclipDir, int consensusLength, int chrBuffer, Integer minInsertSize) throws Exception {
		
		Map<Integer, Breakpoint> leftMap = new HashMap<>();
		Map<Integer, Breakpoint> rightMap = new HashMap<>();
		TreeMap<Integer, List<UnmappedRead>> splitReads = new TreeMap<>();
		Integer bp = getOrphanBreakpoint();
		String ref = getOrphanReference();
			
		
		if (bp != null) {
			readRescuedClips(tumourFile, true, ref, bp, leftMap, rightMap, splitReads, consensusLength, chrBuffer, minInsertSize, p.getReadGroupIds(), p.getPairingType());
			readRescuedClips(normalFile, false, ref, bp, leftMap, rightMap, splitReads, consensusLength, chrBuffer, minInsertSize, p.getReadGroupIds(), p.getPairingType());
		}		
		findMaxRescueBreakpoint(p, cache, leftMap, rightMap, splitReads, softclipDir);
	}
//	public void rescueClips(QSVParameters p, BLAT blat, File tumourFile, File normalFile, String softclipDir, int consensusLength, int chrBuffer, Integer minInsertSize) throws Exception {
//		
//		Map<Integer, Breakpoint> leftMap = new HashMap<>();
//		Map<Integer, Breakpoint> rightMap = new HashMap<>();
//		TreeMap<Integer, List<UnmappedRead>> splitReads = new TreeMap<>();
//		Integer bp = getOrphanBreakpoint();
//		String ref = getOrphanReference();
//		
//		
//		if (bp != null) {
//			readRescuedClips(tumourFile, true, ref, bp, leftMap, rightMap, splitReads, consensusLength, chrBuffer, minInsertSize, p.getReadGroupIds(), p.getPairingType());
//			readRescuedClips(normalFile, false, ref, bp, leftMap, rightMap, splitReads, consensusLength, chrBuffer, minInsertSize, p.getReadGroupIds(), p.getPairingType());
//		}		
//		findMaxRescueBreakpoint(p, blat, leftMap, rightMap, splitReads, softclipDir);
//	}
	
	public void findMaxRescueBreakpoint(QSVParameters p, TIntObjectMap<int[]> cache,
			Map<Integer, Breakpoint> leftMap,
			Map<Integer, Breakpoint> rightMap, TreeMap<Integer, List<UnmappedRead>> splitReads, String softclipDir) throws Exception {

		//find maximum breakpoint
		Breakpoint maxLengthBp = null;
		int maxLength = 0;
		int buffer = p.getUpperInsertSize() + 100;		

		for (Breakpoint b : leftMap.values()) {
			
			if (b.getMaxBreakpoint(buffer, splitReads, maxLength)) {
				if (b.getBreakpoint().intValue() != this.getSingleBreakpoint().getBreakpoint().intValue()) {
					
					maxLengthBp = b;
					maxLength = b.getMateConsensus().length();
				}
			}
		}
		
		for (Breakpoint b: rightMap.values()) {
			
			if (b.getMaxBreakpoint(buffer, splitReads, maxLength)) {
				if (b.getBreakpoint().intValue() != this.getSingleBreakpoint().getBreakpoint().intValue()) {
					maxLengthBp = b;
					maxLength = b.getMateConsensus().length();
				}
			}
		}

		if (maxLengthBp != null) {
			
			if (maxLengthBp.getMateConsensus().length() > 20) {
				boolean match = maxLengthBp.findRescuedMateBreakpoint(cache, p, softclipDir);
				
				if (match) {
					if (this.getSingleBreakpoint() != null) {
						if (this.findMatchingBreakpoints(new SoftClipCluster(maxLengthBp))) {
							maxLengthBp.setRescued(false);
							
							if (leftBreakpointObject == null) {
								leftBreakpointObject = maxLengthBp;								
							} else {
								rightBreakpointObject = maxLengthBp;	
							}
							
							rescuedClips = true;
							oneSide = false;
							setStartAndEnd();
							this.mutationType = defineMutationType();
							hasMatchingBreakpoints = true;
						}
					}
				}
			} 
		}
	}	
//	public void findMaxRescueBreakpoint(QSVParameters p, BLAT blat,
//			Map<Integer, Breakpoint> leftMap,
//			Map<Integer, Breakpoint> rightMap, TreeMap<Integer, List<UnmappedRead>> splitReads, String softclipDir) throws Exception {
//		
//		//find maximum breakpoint
//		Breakpoint maxLengthBp = null;
//		int maxLength = 0;
//		int buffer = p.getUpperInsertSize() + 100;		
//		
//		for (Breakpoint b : leftMap.values()) {
//			
//			if (b.getMaxBreakpoint(buffer, splitReads, maxLength)) {
//				if (b.getBreakpoint().intValue() != this.getSingleBreakpoint().getBreakpoint().intValue()) {
//					
//					maxLengthBp = b;
//					maxLength = b.getMateConsensus().length();
//				}
//			}
//		}
//		
//		for (Breakpoint b: rightMap.values()) {
//			
//			if (b.getMaxBreakpoint(buffer, splitReads, maxLength)) {
//				if (b.getBreakpoint().intValue() != this.getSingleBreakpoint().getBreakpoint().intValue()) {
//					maxLengthBp = b;
//					maxLength = b.getMateConsensus().length();
//				}
//			}
//		}
//		
//		if (maxLengthBp != null) {
//			
//			if (maxLengthBp.getMateConsensus().length() > 20) {
//				boolean match = maxLengthBp.findRescuedMateBreakpoint(blat, p, softclipDir);
//				
//				if (match) {
//					if (this.getSingleBreakpoint() != null) {
//						if (this.findMatchingBreakpoints(new SoftClipCluster(maxLengthBp))) {
//							maxLengthBp.setRescued(false);
//							
//							if (leftBreakpointObject == null) {
//								leftBreakpointObject = maxLengthBp;								
//							} else {
//								rightBreakpointObject = maxLengthBp;	
//							}
//							
//							rescuedClips = true;
//							oneSide = false;
//							setStartAndEnd();
//							this.mutationType = defineMutationType();
//							hasMatchingBreakpoints = true;
//						}
//					}
//				}
//			} 
//		}
//	}	

	String getOrphanReference() {
		if (leftBreakpointObject == null) {
			//find potential matches for breakpoint
		    return leftReference;
		}    
	
		if (rightBreakpointObject == null) {
			return rightReference;
		}
		return null;
	}

	public static void readRescuedClips(File file, boolean isTumour, String reference, int bp, Map<Integer, Breakpoint> leftMap, Map<Integer, Breakpoint> rightMap, TreeMap<Integer, 
			List<UnmappedRead>> splitReads, int consensusLength, int chrBuffer, Integer minInsertSize, Collection<String> readGroupIds, String pairingType) throws IOException {
		int bpStart = bp - chrBuffer;
		int bpEnd = bp + chrBuffer;
		
		int referenceStart = bpStart - 120;
		int referenceEnd = bpEnd + 120;
		int count = 0;
		
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(file,ValidationStringency.SILENT)) {
        
    		SAMRecordIterator iter = reader.queryOverlapping(reference, referenceStart, referenceEnd);
	    
    		while (iter.hasNext()) {
	        	SAMRecord r = iter.next();	
		        	
        		if (readGroupIds.contains(r.getReadGroup().getId())) {
	        		if (r.getReadUnmappedFlag()) {
		        		count++;
		        		if (count > 5000) {
		    	        		break;
		    	        }
		        		UnmappedRead splitRead = new UnmappedRead(r, isTumour);
		        		List<UnmappedRead> reads = splitReads.get(splitRead.getBpPos());
		        		if (null == reads) {
		        			reads = new ArrayList<UnmappedRead>();
		        			splitReads.put(splitRead.getBpPos(), reads);
		        		}
		        		reads.add(splitRead);
		        		
		        	} else {
			        	if ( ! r.getDuplicateReadFlag() && r.getCigarString().contains("S")) {
			        		count++;
			        		if (count > 5000) {
			        			break;
			    	        }
			        		Clip c = SoftClipStaticMethods.createSoftClipRecord(r, bpStart, bpEnd, reference);
			        		if (c != null) {     			
		        				if (c.isLeft()) {
		        					Breakpoint b = leftMap.get(c.getBpPos());
		        					if (null == b) {
		        						b = new Breakpoint(c.getBpPos(), reference, true, consensusLength, minInsertSize);
		        						b.addTumourClip(c);
		        						leftMap.put(c.getBpPos(), b);
		        					} else {
		        					
			        					if (isTumour) {
			        						b.addTumourClip(c);
			        					} else {
			        						b.addNormalClip(c);
			        					}
		        					}
		        				} else {
		        					Breakpoint b = rightMap.get(c.getBpPos());
		        					if (null == b) {
		        						b = new Breakpoint(c.getBpPos(), reference, false, consensusLength, minInsertSize);
		        						b.addTumourClip(c);
		        						rightMap.put(c.getBpPos(), b);
		        					} else {
		        						if (isTumour) {
		        							b.addTumourClip(c);
		        						} else {
		        							b.addNormalClip(c);
		        						}
			        				}
		        				}
			        		}
			        	}
		        	}
        		}	
	        }
		}
	}

	public Integer getOrphanBreakpoint() throws Exception {
		if (leftBreakpointObject == null) {
			//find potential matches for breakpoint
		    return leftBreakpoint;
		} else if (rightBreakpointObject == null) {
			return rightBreakpoint;
		} else {
			throw new Exception("Null breakpoint");
		}
	}

	public boolean alreadyMatched() {
		return this.alreadyMatched;
	}
	
	public void setAlreadyMatched(boolean b) {
		this.alreadyMatched = b;
	}

	public String getSoftClipConsensusString(String svId) {
		String tab = "\t";
		return svId + tab + leftReference + tab + leftBreakpoint + tab + getLeftSoftClipConsensus() 
				+ tab + rightReference + tab + rightBreakpoint + tab + getRightSoftClipConsensus(); 
	}

	private String getLeftSoftClipConsensus() {
		if (leftBreakpointObject == null) {
			return "\t\t\t\t\t\t\t\t";
		} else {
			return leftBreakpointObject.getContigInfo();
		}
	}
	
	private String getRightSoftClipConsensus() {
		if (rightBreakpointObject == null) {
			return "\t\t\t\t\t\t\t\t";
		} else {
			return rightBreakpointObject.getContigInfo();
		}
	}
	
	public String getMicrohomology(String clusterCategory) {
		String nonTemp = getNonTemplateSequence(clusterCategory);
		if (nonTemp.equals(QSVConstants.UNTESTED)) {
			return QSVConstants.UNTESTED;
		} else {
			if (nonTemp.length() > 0 && !nonTemp.equals(QSVConstants.NOT_FOUND)) {
				return QSVConstants.NOT_FOUND;
			}
		}
		
		String cat = null;
		
		if (orientationCategory != null) {
			if (!orientationCategory.equals("")) {
				cat = orientationCategory;
			}
		} else {
			if (clusterCategory != null) {
				cat = clusterCategory;
			}
		}

		String leftReferenceSeq;
		String rightReferenceSeq;
//		Integer leftBp;
//		Integer rightBp;
		
		if (cat != null) {
			
		if (leftBreakpointObject == null) {
//			leftReferenceSeq = rightBreakpointObject.getMateConsensus();
//			rightReferenceSeq = rightBreakpointObject.getBreakpointConsenus();
//			leftBp = rightBreakpointObject.getMateBreakpoint();
//			rightBp = rightBreakpointObject.getBreakpoint();
			return "";
		} else if (rightBreakpointObject == null) {
//			leftReferenceSeq = leftBreakpointObject.getBreakpointConsenus();
//			rightReferenceSeq = leftBreakpointObject.getMateConsensus();
//			leftBp = leftBreakpointObject.getBreakpoint();
//			rightBp = leftBreakpointObject.getMateBreakpoint();
			return "";
		} else {
			leftReferenceSeq = leftBreakpointObject.getBreakpointConsenus();
			rightReferenceSeq = rightBreakpointObject.getBreakpointConsenus();
//			leftBp = leftBreakpointObject.getBreakpoint();
//			rightBp = rightBreakpointObject.getBreakpoint();
		}
	
			if (cat.equals(QSVConstants.ORIENTATION_2)) {
				String tmp = leftReferenceSeq;			
				leftReferenceSeq = rightReferenceSeq;
				rightReferenceSeq = tmp;		
//				Integer tmpInt = leftBp;
//				leftBp = rightBp;
//				rightBp = tmpInt;
			} else if (cat.equals(QSVConstants.ORIENTATION_3)) {
				rightReferenceSeq = QSVUtil.reverseComplement(rightReferenceSeq);
			} else if (cat.equals(QSVConstants.ORIENTATION_4)) {
				leftReferenceSeq = QSVUtil.reverseComplement(leftReferenceSeq);
			}
			
			String mh = "";
			
			
			for (int i = 1; i < rightReferenceSeq.length(); i++) {
				//startPosition
				String currentRight = rightReferenceSeq.substring(0, i);
				
				if (leftReferenceSeq.endsWith(currentRight)) {
					if (currentRight.length() > mh.length()) {
						mh = currentRight;
					}
				}			
			}
			
			return mh.equals("") ? QSVConstants.NOT_FOUND : mh;
		} else {
			return QSVConstants.UNTESTED;
		}		
	}
	
	public String getOverlappingContigSequence() throws Exception {		
		
		if (leftBreakpointObject == null) {
			return rightBreakpointObject.getCompleteConsensus();
		} else if (rightBreakpointObject == null) {
			return leftBreakpointObject.getCompleteConsensus();
		} else {
			QSVAssemble a = new QSVAssemble();		
			return a.getFinalClipContig(leftBreakpointObject.getCompleteConsensus(), rightBreakpointObject.getCompleteConsensus());	
		}
	}
	
	public int getConsensusSplits(boolean isLeft) {
		if (isLeft && leftBreakpointObject != null) {
			return leftBreakpointObject.getSplitConsensusReads();
		}
		if (!isLeft && rightBreakpointObject != null) {			
			return rightBreakpointObject.getSplitConsensusReads();
		}
		return 0;
	}
	
	public int getSplitReadTotal(boolean isLeft) {
		if (isLeft && leftBreakpointObject != null) {			
			return leftBreakpointObject.getSplitReadsSize();		
		}
		if (!isLeft && rightBreakpointObject != null) {
			return rightBreakpointObject.getSplitReadsSize();
		}
		return 0;
	}

	public int getConsensusClips(boolean isLeft) {
		if (isLeft && leftBreakpointObject != null) {
			return leftBreakpointObject.getClipConsensusReads();
		}
		if (!isLeft && rightBreakpointObject != null) {
			return rightBreakpointObject.getClipConsensusReads();
		}
		return 0;
	}
	
	public String getBreakpointConsensus(boolean isLeft) {
		if (isLeft) {
			if (leftBreakpointObject != null) {
				return leftBreakpointObject.getMateConsensus();
			}
		} else {
			if (rightBreakpointObject != null) {
				return rightBreakpointObject.getMateConsensus();
			}
		}
		return "";
	}

	public int getClipSize(boolean isLeft) {
		if (isLeft && leftBreakpointObject != null) {
			return leftBreakpointObject.getTumourClips().size() + leftBreakpointObject.getNormalClips().size();
		}
		if (!isLeft && rightBreakpointObject != null) {
			return rightBreakpointObject.getTumourClips().size() + rightBreakpointObject.getNormalClips().size();
		}
		return 0;
	}

	public String getNonTemplateSequence(String clusterCategory) {
		String nonTmp = QSVConstants.UNTESTED;
		if (hasMatchingBreakpoints) {
			
			String cat = null;
			
			if (orientationCategory != null) {
				if (!orientationCategory.equals("")) {
					cat = orientationCategory;
				}
			} else {
				if (clusterCategory != null) {
					cat = clusterCategory;
				}
			}
			
			if (cat != null) {
				
				String leftClipSeq = rightBreakpointObject.getMateConsensusPosStrand();			
				String rightClipSeq = leftBreakpointObject.getMateConsensusPosStrand();
				int leftNonTemp = rightBreakpointObject.getNonTempBases();
				int rightNonTemp = leftBreakpointObject.getNonTempBases();
				
				if (cat.equals(QSVConstants.ORIENTATION_2)) {
					String tmp = leftClipSeq;			
					leftClipSeq = rightClipSeq;
					rightClipSeq = tmp;		
				}
				
				if (leftNonTemp == rightNonTemp) {
					if (leftNonTemp == 0) {
						nonTmp = "";
					}
					String currentRight = rightClipSeq.substring(0, rightNonTemp);
					if (leftClipSeq.endsWith(currentRight)) {
						nonTmp = currentRight;
					}
				}
			}
		}
		return nonTmp;
	}
	
	public static String getNonTemplateSequence(String clusterCategory, boolean hasMatchingBreakpoints, String orientationCategory, 
			String rightBpMateConsensus, String leftBpMateConsensus, int rightBpNonTempBases, int leftBpNonTempBases) {
		String nonTmp = QSVConstants.UNTESTED;
		if (hasMatchingBreakpoints) {
			
			String cat = null;
			
			if (orientationCategory != null) {
				if (!orientationCategory.equals("")) {
					cat = orientationCategory;
				}
			} else {
				if (clusterCategory != null) {
					cat = clusterCategory;
				}
			}
			
			if (cat != null) {
				
				String leftClipSeq = rightBpMateConsensus;			
				String rightClipSeq = leftBpMateConsensus;
				int leftNonTemp = rightBpNonTempBases;
				int rightNonTemp = leftBpNonTempBases;
				
				if (cat.equals(QSVConstants.ORIENTATION_2)) {
					String tmp = leftClipSeq;			
					leftClipSeq = rightClipSeq;
					rightClipSeq = tmp;		
				}
				
				if (leftNonTemp == rightNonTemp) {
					if (leftNonTemp == 0) {
						nonTmp = "";
					}
					String currentRight = rightClipSeq.substring(0, rightNonTemp);
					if (leftClipSeq.endsWith(currentRight)) {
						nonTmp = currentRight;
					}
				}
			}
		}
		return nonTmp;
	}

	public boolean matchesSplitReadBreakpoints(String splitLeftReference,
			String splitRightReference, int splitLeftBreakpoint, int splitRightBreakpoint) {
		
		if (orientationCategory.equals(QSVConstants.ORIENTATION_2)) {
			if (leftReference.equals(splitRightReference) && rightReference.equals(splitLeftReference) &&
					leftBreakpoint == splitRightBreakpoint && rightBreakpoint == splitLeftBreakpoint) {
				return true;
			}				
				
		} else {
			if (leftReference.equals(splitLeftReference) && rightReference.equals(splitRightReference) &&
					leftBreakpoint == splitLeftBreakpoint && rightBreakpoint == splitRightBreakpoint) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SoftClipCluster other = (SoftClipCluster) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}

