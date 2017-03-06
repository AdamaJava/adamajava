/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.Constants;

public class Segmenter {	

	private final File inputFile;
	private final File outputFile;
	private final boolean runMerge;
	private final boolean runFill;
	private final Feature[] features;
	private Map<String, List<Segment>> segments = new HashMap<>();	
	private final QLogger logger = QLoggerFactory.getLogger(getClass());
	private final Map<String, Integer> bounds;
	private final List<String> chromosomeOrder = new ArrayList<String>();
	private final String cmdLine;
	private final File boundsFile;
	 
	public Segmenter(Options options, String cmdLine) throws IOException {
		this.inputFile = options.getInputSegmentFile();
		this.outputFile = options.getOutputSegmentFile();
		this.runMerge = options.hasMergeOption();
		this.runFill = options.hasFillOption();
		this.features = processFeatures(options.getFeatures());
		this.cmdLine = cmdLine; 
		this.boundsFile = options.getBoundsOption();
		this.bounds = readInBounds();
		run();
	}

	private void run() throws IOException {
		logOptions();
		readGFF();		
		
		addShoulders();
		
		if (runFill) {
			fill();
		}
		
		if (runMerge) {			
			merge(false);
		}
		
		writeGFF3();
	}
	
	private void logOptions() {
		logger.info("Input GFF: " + inputFile.getAbsolutePath());
		logger.info("Output GFF: " + outputFile.getAbsolutePath());
		for (Feature f: features) {
			logger.info("Feature: " + f.toString());
		}
		logger.info("Run merge: "+ runMerge);
		logger.info("Run fill: "+ runFill);
		logger.info("Bounds: "+ boundsFile.getAbsolutePath());	
	}

	private Map<String, Integer> readInBounds() throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(boundsFile));) {
			String line;		
			while((line = reader.readLine()) != null) {			
				String[] vals = line.split(Constants.TAB_STRING);
				bounds.put(vals[0], Integer.valueOf(vals[1]));
				chromosomeOrder.add(vals[0]);
			}
		}
		return bounds;
	}

	private void readGFF() throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));) {
			String line;
			int recordCount = 0;
			while((line = reader.readLine()) != null) {
				//skip headers
				if (line.startsWith(Constants.HASH_STRING)) {
					continue;
				}
				recordCount++;
				String[] fields = line.split(Constants.TAB_STRING);
				
				String chr = fields[0];
				
				for (Feature f: features) {
					if (f.getName().equals(fields[2])) {
						segments.computeIfAbsent(chr, k -> new ArrayList<Segment>()).add(new Segment(fields, f, recordCount));
					}
				} 			
			}
		merge(true);
		}
	}	

	private void merge(boolean includeSubFeature) {
		HashMap<String, List<Segment>> newSegments = new HashMap<String, List<Segment>>();
		int subFeatures = 0;
		int overlapFeatures = 0;
		int adjacentFeatures = 0;
		
		int inRecords = 0;
		int outRecords = 0;
		for (Entry<String, List<Segment>> entry: segments.entrySet()) {			
			String chr = entry.getKey();
			List<Segment> list = entry.getValue();
			Collections.sort(list);
			
			if (!newSegments.containsKey(chr)) {
				List<Segment> emptyList = new ArrayList<Segment>();
				newSegments.put(chr, emptyList);
			}
			inRecords += list.size();
			Segment currentSeg = list.get(0);
			
			for (int i=1; i<list.size(); i++) {				
				Segment nextSeg = list.get(i);
				
				//If different chroms or feature types then no merging is possible
			    //so move on
				if (!currentSeg.getFeature().getName().equals(nextSeg.getFeature().getName())) {
					newSegments.get(chr).add(currentSeg);
					currentSeg = nextSeg;
					continue;
				}
				
				boolean problemFlag = false;
				String probMsg = "";
				
				//We have 3 scenarios that we will potentially merge:
				//1. Subfeature - one region is entirely within the next
				//2. Overlap - the 2 regions overlap
				//3. Adjacent - there are 0 bases between the regions
				//
				//1. Subfeature - one feature is entirely within the other	
				
				if (includeSubFeature && ((currentSeg.getPositionStart() <= nextSeg.getPositionStart() && currentSeg.getPositionEnd() >= nextSeg.getPositionEnd())
				|| (currentSeg.getPositionStart() >= nextSeg.getPositionStart() && currentSeg.getPositionEnd() <= nextSeg.getPositionEnd()))) {
					subFeatures++;
					problemFlag = true;
					probMsg = "subfeature";
				} else if (currentSeg.getPositionEnd() > nextSeg.getPositionStart()-1) {// Overlap - merge if end of the first region is >= the start of next
					overlapFeatures++;
					problemFlag = true;	
					probMsg = "overlap";
				} else if (currentSeg.getPositionEnd() == nextSeg.getPositionStart()-1) {// Adjacent - end of the first region is immediately before start of next
					adjacentFeatures++;
					problemFlag = true;	
					probMsg = "adjacent";
				}
				
				if (problemFlag) {
					logger.info("Merging " + probMsg  + " " + currentSeg.getPositionString() + "," + nextSeg.getPositionString());
					if (nextSeg.getPositionStart() < currentSeg.getPositionStart()) {
//						currentSeg.setPositionStart(nextSeg.getPositionStart());
						// create new Segment object with updated start position
						ChrRangePosition newChrPos = new ChrRangePosition(currentSeg.getPosition().getChromosome(), nextSeg.getPositionStart(), currentSeg.getPosition().getEndPosition());
						currentSeg = new Segment(currentSeg.getFields(), currentSeg.getFeature(), newChrPos, currentSeg.getRecordCount());
					}
					if (nextSeg.getPositionEnd() > currentSeg.getPositionStart()) {
//						currentSeg.setPositionEnd(nextSeg.getPositionEnd());
						// create new Segment object with updated end position
						ChrRangePosition newChrPos = new ChrRangePosition(currentSeg.getPosition().getChromosome(), currentSeg.getPosition().getStartPosition(), nextSeg.getPositionEnd());
						currentSeg = new Segment(currentSeg.getFields(), currentSeg.getFeature(), newChrPos, currentSeg.getRecordCount());
					}					
				} else {
					newSegments.get(chr).add(currentSeg);
					currentSeg = nextSeg;
				}
			}	
			
			newSegments.get(chr).add(currentSeg);
			outRecords+=newSegments.get(chr).size();
			int count = 0;
			for (Segment s: newSegments.get(chr)) {
				logger.info(s.getPositionString() + " " + count);
				count++;
			}
			
		}
		//don't need the input any more
		
		//System.out.println("end merge" + newSegments.get("chr1").size());
		logger.info("Merged: " + overlapFeatures + " overlaps " + subFeatures + " subfeatures and " + adjacentFeatures + " adjacents " +				
				"(inputs " + inRecords + " outputs " + outRecords + ")" );
		
		segments.clear();
		
		segments = new HashMap<String, List<Segment>>(newSegments);
	}

	private Feature[] processFeatures(String[] featureList) {
		Feature[] features = new Feature[featureList.length]; 
		for (int i=0; i<featureList.length; i++) {
			features[i] = new Feature(featureList[i], i+1);
		}
		return features;
	}
	
	private Segment createNewSegment(Segment currentSeg, String label, int start,
			int end) {
		
		Feature feature = new Feature(label, currentSeg.getFeature().getPriority());
		String[] oldValues = currentSeg.getFields();
		String[] values = new String[oldValues.length];
		values[0] = oldValues[0];//chr
		values[1] = "qcoverage[v" + Main.class.getPackage().getImplementationVersion() + "]";
		values[2] = label; 
		values[3] = Integer.toString(start);
		values[4] = Integer.toString(end);
		values[5] = oldValues[5];
		values[6] = oldValues[6];
		values[7] = oldValues[7];
		values[8] = "record=" + label;
		return new Segment(values, feature, 1);
	}
	
	private Segment createNewFillSegment(String chr, int start,
			int end) {
		logger.info("Creating new fill " + chr + " " + start + " " + end);
		Feature feature = new Feature("fill", 1);
		String[] values = new String[9];
		values[0] = chr;//chr
		values[1] = "qcoverage[v" + Main.class.getPackage().getImplementationVersion() + "]";
		values[2] = "fill"; 
		values[3] = Integer.toString(start);
		values[4] = Integer.toString(end);
		values[5] = ".";
		values[6] = ".";
		values[7] = ".";
		values[8] = "record=fill";
		return new Segment(values, feature, 1);
	}


	private void addShoulders() {
//	    # A GFF3 records looks like:
//	        # 0  sequence    chr1
//	        # 1  source      SureSelect_All_Exon_50mb_with_annotation.hg19.bed
//	        # 2  label       bait
//	        # 3  start       14467
//	        # 4  end         14587
//	        # 5  score       .
//	        # 6  strand      +
//	        # 7  phase       .
//	        # 8  attributes  ID=ens|ENST00000423562,ens|ENST00000438504 ...
		Map<String, List<Segment>> newSegments = new HashMap<>();
		
		for (Entry<String, List<Segment>> entry : segments.entrySet()) {
			logger.info("Adding shoulder for chromosome: " + entry.getKey());
			//add keyvalue pair to new map
			List<Segment> newList = new ArrayList<Segment>();			
			
			List<Segment> segments = entry.getValue();
			
			//Pre shoulders on the first region
			newList.addAll(preShoulders(segments.get(0), 0));
			
			for (int i=0; i<segments.size()-1; i++) {
		        // Work out whether the distance between the two features is
		        // (a) anything at all and (b) enough for all of the shoulders.
		        // If it is big enough the allocating is trivial.  If it's smaller
		        // that the required shoulder gap then we will need to use the
		        // feature priorities to decide who gets to put in their
		        // shoulders first.  Remember that for feature priorities, smaller
		        // numbers are "better" than larger ones and get to allocate their
		        // shoulders first.
				Segment currentSeg = segments.get(i);
				Segment nextSeg = segments.get(i+1);
				logger.info("Processing " + currentSeg.getPositionString() + " " + nextSeg.getPositionString());
				logger.info("Size" + newList.size());
				int shoulderLength = currentSeg.getFeature().getAfterBases() + nextSeg.getFeature().getBeforeBases();
				int gap = nextSeg.getPositionStart() - currentSeg.getPositionEnd() + 1;
				
				//no room to allocate shoulders
				if (gap <=1) {
					newList.add(currentSeg);
					continue;
				}
				//can allocate shoulders
				if (gap >= shoulderLength) {
					logger.info("  Plenty of room [gap:"+gap+ " ;shoulders:" + shoulderLength + "]");
					newList.add(currentSeg);
					newList.addAll(postShoulders(currentSeg, nextSeg.getPositionStart()-1));
					newList.addAll(preShoulders(nextSeg, newList.get(newList.size()-1).getPositionEnd()+1));
				} else {
					//special case - use priorities to assign shoulders
					int currentPriority = currentSeg.getFeature().getPriority();
					int nextPriority = nextSeg.getFeature().getPriority();
					newList.add(currentSeg);
					logger.info("   Gap too small [gap:"+gap+ " ;shoulders:" + shoulderLength+ "]");
					if (currentPriority == nextPriority) {
						newList.addAll(alternateShoulders(currentSeg, nextSeg));
						logger.info("Alternate shoulders");
					} else if (currentPriority < nextPriority) {
						newList.addAll(postShoulders(currentSeg, nextSeg.getPositionStart()-1));
						newList.addAll(preShoulders(nextSeg, newList.get(newList.size()-1).getPositionEnd()+1));
						logger.info("Post then Pre");
					} else if (currentPriority > nextPriority) {
		                //Pre-then-post is a weird case because even though you
		                //calculate the pres first, you can't add them to
		                //newList until after you have allocated the posts
		                //so you have to remembers the pres.
						List<Segment> newPres = preShoulders(nextSeg, currentSeg.getPositionEnd()+1);
						int firstAssigned = nextSeg.getPositionStart()-1;
						if (newPres.size() > 0) {
							firstAssigned = newPres.get(0).getPositionStart()-1;
						}
						newList.addAll(postShoulders(currentSeg, firstAssigned));
						newList.addAll(newPres);
						logger.info("Pre then Post");
					}
					
				}
			}
			
			//final feature and any post-shoulders
			Segment lastSeg = segments.get(segments.size()-1);
			newList.add(lastSeg);
			logger.info("last");
			newList.addAll(postShoulders(lastSeg, lastSeg.getPositionEnd() + 1000000000));
			
			newSegments.put(entry.getKey(), newList);
		}		
		//logger.info("Filled: " + fillCount + "(inputs " + inRecords + " outputs " + outRecords + ")" );
		
		segments.clear();
		int size = 0;
		for (Entry<String, List<Segment>> entry: newSegments.entrySet()) {
			size += entry.getValue().size();
		}
		logger.info("Added shoulders [output : " + size + "]");
		segments = new HashMap<String, List<Segment>>(newSegments);		
		
	}

	private List<Segment> preShoulders(Segment currentSeg, int minPos) {
		List<Segment> newSegments = new ArrayList<>();
		
		List<Integer> preLengths = currentSeg.getFeature().getPreList();
		
		//Check whether there's space to allocate any shoulders
		if (minPos >= currentSeg.getPositionStart()) {
			return newSegments;
		}
		
		int end = currentSeg.getPositionStart()-1;
		for (int i=0; i<preLengths.size(); i++) {
			int labelLength = i+1;
			String label = currentSeg.getFeature().getName() + "_" + labelLength + "_" + preLengths.get(i);
			int start = end - preLengths.get(i) + 1;
			//truncate start if it be run before minPos
			if (start < minPos) {
				start = minPos;
			}			
			Segment newSegment =  createNewSegment(currentSeg, label, start, end);
			//add to start of list
			logger.info("pre" + start + " " + end + " " + minPos + " " + preLengths.get(i));
			newSegments.add(0, newSegment);
			//break if we've hit our min allowable position
			if (start == minPos) {
				break;
			}
			end = end - preLengths.get(i);
		}		
		
		return newSegments;
	}
	
	private List<Segment> postShoulders(Segment currentSeg, int maxPos) {
		List<Segment> newSegments = new ArrayList<>();
		
		List<Integer> postLengths = currentSeg.getFeature().getPostList();
		
		//Check whether there's space to allocate any shoulders
		if (maxPos <= currentSeg.getPositionEnd()) {
			return newSegments;
		}
		
		int start = currentSeg.getPositionEnd() + 1; 
		for (int i=0; i<postLengths.size(); i++) {
			int labelLength = i+1;
			String label = currentSeg.getFeature().getName() + "_" + labelLength + "_" + postLengths.get(i);
			
			int end = start + postLengths.get(i) -1;
			if (end > maxPos) {
				end = maxPos;
			}			
			logger.info("post" + start + " " + end + " " + maxPos + " " + postLengths.get(i));
			Segment newSegment = createNewSegment(currentSeg, label, start, end);
			//add to start of list
			newSegments.add(newSegment);
			//break if we've hit our min allowable position
			if (start == maxPos) {
				break;
			}
			start = start + postLengths.get(i);
		}		
		
		return newSegments;
	}
	
	//# Special case only for two features of the same priority and that are
	//too close to assign all the shoulders. 
	private List<Segment> alternateShoulders(Segment currentSeg, Segment nextSeg) {
		List<Segment> newSegments = new ArrayList<Segment>();
		ArrayList<Integer> preLengths = new ArrayList<Integer>(currentSeg.getFeature().getPreList());
		ArrayList<Integer> postLengths = new ArrayList<Integer>(currentSeg.getFeature().getPostList());
		
		List<Segment> newPres = new ArrayList<Segment>();
		List<Segment> newPosts = new ArrayList<Segment>();
		
		int minPos = currentSeg.getPositionEnd()+1;
		int maxPos = nextSeg.getPositionStart()-1;
		int start = minPos;
		int end = maxPos;
		int preBaseCount = 1;
		int postBaseCount = 1;
		int preBaseIndex = 0;
		int postBaseIndex = 0;
		
		//logger.info(minPos + "|" + maxPos);
		while (preLengths.size() + postLengths.size() > 0) {
			if (preLengths.size() == 0 || postLengths.size() > 0 && postBaseIndex <= preBaseIndex) {
				int length = postLengths.get(0);
				postLengths.remove(0);
				String label = currentSeg.getFeature().getName() + "_" + postBaseCount++ + "_" + length;
				start = minPos;
				end = start + length  -1;
				//Truncate end if it would run past max_pos
				if (end > maxPos) {
					end = maxPos;
				}
				postBaseIndex += (end - start + 1);
				logger.info("assigning a post" + label + "  " + start + " " + end);
				Segment newSegment = createNewSegment(nextSeg, label, start, end);
				//add to start of list
				newPosts.add(newSegment);
				minPos = end + 1;
				if (end == maxPos) {
					break;
				}
			} else if (postLengths.size() == 0 || preLengths.size() > 0 && preBaseIndex <= postBaseIndex){
				int length = preLengths.get(0);
				preLengths.remove(0);
				String label = nextSeg.getFeature().getName() + "_" + preBaseCount++ + "_" + length;
				end = maxPos;
				start = (end - length + 1);
				
				if (start < minPos) {
					start = minPos; 
				}
				
				preBaseIndex += (end - start + 1);
				logger.info("assigning a pre" + label + "  " + start + " " + end);
				Segment newSegment = createNewSegment(nextSeg, label, start, end);
				
				//add to start of list
				newPres.add(0, newSegment);
				maxPos = start - 1;
				if (start == minPos) {
					break;
				}
			}			
		}
		
		newSegments.addAll(newPres);
		newSegments.addAll(newPosts);
		Collections.sort(newSegments);
		return newSegments;
	}

	private void fill() throws IOException {
		HashMap<String, List<Segment>> newSegments = new HashMap<String, List<Segment>>();		
		
		int inRecords = 0;
		int outRecords = 0;
		int fillCount = 0;
		for (Entry<String, List<Segment>> entry: segments.entrySet()) {
			String chromosome = entry.getKey();
			List<Segment> segmentList = entry.getValue();			
			List<Segment> newSegmentList = new ArrayList<Segment>();
			//prefill on first region
			Segment firstSeg = segmentList.get(0);
			
			inRecords += segmentList.size();
			if (firstSeg.getPositionStart() > 1) {
				Segment newSegment = createNewFillSegment(chromosome, 1, firstSeg.getPositionStart()-1);
				newSegmentList.add(newSegment);
				fillCount++;
			}
			
			for (int i=0; i<segmentList.size(); i++) {
				Segment currentSeg = segmentList.get(i);
				
				//final segment
				if (i == segmentList.size()-1) {	
					//last segment - end of sequence					
					logger.info("end filling" + currentSeg.getPositionString());
					newSegmentList.add(currentSeg);
					//Create record to fill end of current sequence, but only if
		            //the sequence appears in our list of fill contigs
					if (bounds.containsKey(chromosome)) {
						if (currentSeg.getPositionEnd() < bounds.get(chromosome)) {
							Segment newSegment = createNewFillSegment(chromosome, currentSeg.getPositionEnd()+1, bounds.get(chromosome));
							newSegmentList.add(newSegment);
							fillCount++;
						}
					}					
				} else {
					Segment nextSeg = segmentList.get(i+1);
					logger.info("Filling " + currentSeg.getPositionString() + " " + nextSeg.getPositionString());
					if (currentSeg.getPositionEnd() != (nextSeg.getPositionStart()-1)) {//Create record to fill start of next sequence
						newSegmentList.add(currentSeg);
						Segment newSegment = createNewFillSegment(chromosome, currentSeg.getPositionEnd()+1, nextSeg.getPositionStart()-1);
						newSegmentList.add(newSegment);
						fillCount++;
					} else {
						newSegmentList.add(currentSeg);
					}					
				}			
			}			
			
			outRecords += newSegmentList.size();
			
			for (Segment s : newSegmentList) {
				logger.info("fill list " + s.getPositionString());
			}
			newSegments.put(chromosome, newSegmentList);			
		}
		
//		Now we need one extra pass - if there are no features from one of
//	    the sequences in the hash then there will be no fill generated.
//	    In this case we need to have a special "all" fill region added
//	    which covers the entire sequence from base 1 to base n.
		
		for (Entry<String, Integer> entry: bounds.entrySet()) {
			if (!segments.containsKey(entry.getKey())) {
				List<Segment> segmentList = new ArrayList<Segment>();
				segmentList.add(createNewFillSegment(entry.getKey(), 1, entry.getValue()));
				newSegments.put(entry.getKey(), segmentList);
			}
		}
		
		logger.info("Filled: " + fillCount + " (inputs " + inRecords + " outputs " + outRecords + ")" );
		
		segments.clear();
		segments = new HashMap<String, List<Segment>>(newSegments);		
	}
	
	private void writeGFF3() throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));) {
		
			writer.write("##gff-version 3\n");
	        writer.write("#Created by: qcoverage[v" + Main.class.getPackage().getImplementationVersion() + "]\n");
			writer.write("# Created on: " +System.currentTimeMillis()+ "\n");
	        writer.write("# Commandline: "+ cmdLine + "\n" );
	        
	       
	        for (String chromosome : chromosomeOrder) {
	        	List<Segment> segmentList = segments.get(chromosome);
	        	if (segmentList != null) {
	        		int chrEnd = bounds.get(chromosome);
	            	logger.info(chromosome + " " + segmentList.size());
	            	for (Segment segment : segmentList) {
	            		if (segment.getPositionStart() > chrEnd) {
	            			//feature outside bounds so drop it
	            			continue;
	            		} else if (segment.getPositionEnd() > chrEnd) {
	            			//feature outside bounds so truncating feature
	            			logger.info("truncating " + segment.getPositionString());
	            			
	            			
	            			// create new Segment object with updated end position
							ChrRangePosition newChrPos = new ChrRangePosition(segment.getPosition().getChromosome(), segment.getPosition().getStartPosition(), chrEnd);
							segment = new Segment(segment.getFields(), segment.getFeature(), newChrPos, segment.getRecordCount());
	//             			segment.setPositionEnd(chrEnd);        			
	            			writer.write(segment.toString());
	            		} else {
	            			//logger.info(segment.toString());
	            			writer.write(segment.toString());
	            		}
	            	} 
	        	}
	        	       	
	        }
		}
	}


}
