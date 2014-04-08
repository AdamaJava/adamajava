/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.discordantpair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.qsv.QSVParameters;
import org.qcmg.qsv.report.SVCountReport;
import org.qcmg.qsv.util.QSVUtil;

public class FindDiscordantPairClustersMT implements Callable <Map<String, List<DiscordantPairCluster>>>  {

    private static QLogger logger = QLoggerFactory.getLogger(FindDiscordantPairClustersMT.class);

    private CountDownLatch countDownLatch;
    private MatePairsReader findReader;
    private MatePairsReader compareReader;
    private QSVParameters findParameters;
    private PairGroup zp;
    private Map<String, List<DiscordantPairCluster>> clusters; // Key: mutation type(somatic, germline, normal-germline, artifact) | Value: List of clusters
    private DiscordantPairCluster currentCluster;
    private int windowSize;
    private int clusterSize;
    private QSVParameters compareParameters;
    private int somaticCount = 0;
    private int germlineCount = 0;
    private int normalgermlineCount = 0;
	private SVCountReport clusterCounts;
	private String query;
	private boolean isLargeCluster = false;
	private boolean isQCMG;
	

    public FindDiscordantPairClustersMT(PairGroup zp, CountDownLatch countDownLatch, 
            MatePairsReader findReader, MatePairsReader compareReader, 
            QSVParameters findParameters, QSVParameters compareParameters, SVCountReport clusterCounts, String query, boolean isQCMG) {
        this.zp = zp;
        this.countDownLatch = countDownLatch;
        this.findParameters = findParameters;
        this.compareParameters = compareParameters;
        this.clusters = new HashMap<String, List<DiscordantPairCluster>>();
        this.findReader = findReader;
        this.compareReader = compareReader;
        this.currentCluster = null;
        this.clusterSize = findParameters.getClusterSize();
        this.windowSize = findParameters.getUpperInsertSize();
        this.clusterCounts = clusterCounts;
        this.query = query;
        this.isQCMG = isQCMG;
        
    }

    public String getTotalClusters() {
        String clusterCount = "";
        
        if (findParameters.isTumor()) {
        	clusterCount += " somatic = " + somaticCount + " | germline = " + germlineCount;
        } else {
        	clusterCount += " normal-germline = " + normalgermlineCount;
        }
        return clusterCount;
    } 

    /**
     * Starting point for the thread
     */
    @Override
    public Map<String, List<DiscordantPairCluster>>  call() throws Exception {
        try {
            
            if (findParameters.isTumor() || (!findParameters.isTumor() && isQCMG)) {
            	
                processByMatePairList();        
                if (isQCMG) {
                	logger.info("Number of " + findReader.getZp() + " mates for " + findReader.getType() + " " + zp.getPairGroup() + ": " + findReader.getMateCount());
                	logger.info("Number of clusters for " + findParameters.getFindType() + " " + zp.getPairGroup() + ": " + getTotalClusters());
                } else {
                  	logger.info("Number of mates for " + findReader.getType() + " " + QSVUtil.getMutationByPairGroup(zp.getPairGroup()) + ": " + findReader.getMateCount());
                	logger.info("Number of clusters for " + findParameters.getFindType() + " " + QSVUtil.getMutationByPairGroup(zp.getPairGroup()) + ": " + getTotalClusters());   	
                }
            }
        } catch (Exception e) {
            logger.error("Error while parsing SAMRecords", e);            
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            // decrement the countdown by one
            countDownLatch.countDown();
        }        

        clusterCounts.addCountsToMap(zp, somaticCount, germlineCount, normalgermlineCount);
        
        return this.clusters;
    }

    private void processByMatePairList() throws Exception {    
    	
        for (Entry<String, List<File>> entry : findReader.getFilesToRead().entrySet()) {        	
        	
        	//hasn't been noted before
        	if (!isLargeCluster) {
        		if (somaticCount > 10000) {
        			isLargeCluster = true;
        			logger.warn("Greater than 10000 somatic clusters have been found for SV type " + this.zp + ". qSV analysis may not complete or may have a long run time. Please check insert isize range/s supplied in ini file");
        		} else {
        			if (germlineCount > 50000) {
        				isLargeCluster = true;
        				logger.warn("Greater than 50000 germline clusters have been found for SV type " + this.zp + ". qSV analysis may not complete or may have a long run time. Please check insert isize range/s supplied in ini file");
        			}
        		}        		
        	}

                List<File> list = entry.getValue();            
                
                List<MatePair> findMatePairsList = findReader.getMatePairsListByFiles(list, true);        
                List<DiscordantPairCluster> tempClusters = findClusters(findMatePairsList);   
                
                findMatePairsList = null;
                List<MatePair> compareMatePairsList = new ArrayList<MatePair>();
                if (compareReader != null) {
	                if (compareReader.getFilesToRead().containsKey(entry.getKey()) && findParameters.isTumor()) {
	                    List<File> compareList = compareReader.getFilesToRead().get(entry.getKey());
	                    compareMatePairsList = compareReader.getMatePairsListByFiles(compareList, false);                    
	                }
                }
                
                String version = "type";                           
                
                if (findParameters.isRunSoftClipAnalysis()) {
                	version = "reference";
                }
                classifyClusters(tempClusters, compareMatePairsList, version);
                
                compareMatePairsList = null;
        	}              
             
    }
 
    /**
     * Find clusters in the read (by chromosome)
     * 
     * @param findMatePairs list of mate pairs to find 
     * @return
     * @throws IOException 
     */
    public List<DiscordantPairCluster> findClusters(List<MatePair> findMatePairs) throws IOException {
        
        return findNormalClusters(findMatePairs);
    }

    private List<DiscordantPairCluster> findNormalClusters(List<MatePair> findMatePairs) {
        int startPos = -1;
        int range = -1;
        int finalRead = findMatePairs.size() - 1;
        List<DiscordantPairCluster> tempClusters = new ArrayList<DiscordantPairCluster>();
        //iterate through currentPairs and start defining clusters
        for (int i = 0; i < findMatePairs.size();i++) {
            //start the new cluster;
            MatePair startPair = findMatePairs.get(i); 
            

            startPos = startPair.getLeftMate().getStart();
            range = startPos + windowSize;
            currentCluster = new DiscordantPairCluster(startPair.getLeftMate().getReferenceName(), startPair.getRightMate().getReferenceName(), zp.getPairGroup(), findParameters, isQCMG);
            currentCluster.getClusterMatePairs().add(startPair);

            //start adding mate pairs to the cluster
            for (int j=i+1; j< findMatePairs.size(); j++) {
                MatePair currentPair = findMatePairs.get(j);
                
                currentCluster.findLeftEndOfCluster();
              
                /*  firstreadxxxxxx-------------------xxxxxxxxx
                 *  windowsizeeeeeeeeeeeeeeeeee
                 *  |                         |       
                 *  start                    range
                 */ 
                //begin by setting the left range, otherwise carry on
    
                int currentPairStart = currentPair.getLeftMate().getStart();
                
                
                // check if the left mate of the read pair is in the left range
                // defined and if the mate is on the same chromosome
                if (currentPairStart >= startPos && currentPairStart <= range) {
                	
                    currentCluster.getClusterMatePairs().add(currentPair);    
                }                
                      
                // if the read starts outside the current cluster window, or is the
                // final read, check the right window and restart the next cluster
                // or check to see if the current right start is less than current left end
                if (currentPairStart > range || j == finalRead) {  
                    // check cluster size for normal bam is greater than 1.                   
                    //if potential cluster has minimum cluster size, attempt to resolve the right reads, otherwise will seed
                    //cluster with the next pair
                	
                    if (currentCluster.getClusterMatePairs().size() >= clusterSize) {
                        
                            //find the best cluster
                            int nextIndex = currentCluster.findBestCluster(i, j, clusterSize);
                            
                            if (currentCluster.getClusterMatePairs().size() != 0) {                                
                                startPos = currentCluster.getClusterMatePairs().get(0).getLeftMate().getStart();
                                range = startPos + windowSize;

                                if (currentCluster.getClusterMatePairs().size() >= findParameters.getClusterSize()) {
                                    if (currentPairStart > range || j == finalRead) {
                                        tempClusters.add(currentCluster);
                                        currentCluster = null;
                                        i = j - nextIndex - 1;                                       
                                        break;
                                    }                                    
                                }                        
                                if (currentPairStart >= startPos && currentPairStart <= range) {
                                    currentCluster.getClusterMatePairs().add(currentPair);
                                } else if (currentPairStart > range || j == finalRead) {
                                    break;
                                }
                            } else {
                                //no members of cluster, so break
                                break;
                            }
                    } else {
                        break;
                    }
                }
            }
            if (i == finalRead) {
                break;
            }
        }
        return tempClusters;
    }
 
    /**
     * Filter clusters by the parameters for comparison to determine the
     * mutation type (somatic, germline)
     * 
     * @param tempClusters the unfiltered list of clusters
     * @param compareMatePairsList 
     * @param compareMatePairs map of read pairs to compare with
     * @return
     * @throws QPileupException 
     */    
    void classifyClusters(List<DiscordantPairCluster> tempClusters, List<MatePair> compareMatePairsList, String version) throws Exception {
    	
        for (DiscordantPairCluster cluster : tempClusters) {
            
            //set the ends of the cluster, check for any overlap
            cluster.setClusterEnds();                
            
           //if finding clusters in tumor, need to filter through normal reads 
           if (findParameters.isTumor()) {
               
               //filterCluster(cluster);
        	   if (compareParameters != null) {
        		   filterCluster(compareMatePairsList, cluster);
        	   }
        	   
               if (cluster.getMatchedReadPairs().size() >= findParameters.getCompareClusterSize()) {    
            	   germlineCount++; 
                   cluster.finalize(findParameters, compareParameters, "germline", germlineCount, query, findParameters.getPairingType(), isQCMG);                            
                     
                   
               // otherwise is a somatic mutation
               } else {      
            	   somaticCount++;
                   cluster.finalize(findParameters, compareParameters, "somatic", somaticCount, query, findParameters.getPairingType(), isQCMG);           
                   
                   //do pileup for somatic
                                    
                   if (tempClusters.size() < 3000) {           	   
                        // cluster.findPileup(findParameters, compareParameters);
                   }
               }
           } else {         
        	   normalgermlineCount++;
               cluster.finalize(findParameters, compareParameters, "normal-germline", normalgermlineCount, query, findParameters.getPairingType(), isQCMG);              
               
           } 
           addToClustersMap(cluster, version);
        }
    }
    
    private DiscordantPairCluster filterCluster(List<MatePair> compareReadPairs, DiscordantPairCluster cluster) {  
    	//set normal window range
    	
    	cluster.setNormalRange(compareParameters.getUpperInsertSize());
    	
    	int leftStart = cluster.getCompareLeftStart();
        int leftEnd = cluster.getCompareLeftEnd();
        int rightStart = cluster.getCompareRightStart();
        int rightEnd = cluster.getCompareRightEnd();

        // if there are reads on this chromosome
        if (compareReadPairs != null) {
            // check if the reads in the comparison reads map are in the
            // window

            for (MatePair pairToCompare : compareReadPairs) {
 //           	System.out.println(pairToCompare.toString());
                //left start must be in the left window and right start must be in the right window
                //  leftStart       leftEnd             rightStart          rightEnd
                //      |               |                   |                   |
                //      wwwwwwwwwwwwwwwww                   wwwwwwwwwwwwwwwwwwwww   window from foundCluster
                //              xxxxxx--------------------------------xxxxxxxxxxxx  this compare read
                //              |                                     |
                //              read pair start (left read)           read pair start (right read)
                //
                //check the left start or end is within the left range
                
        		if (pairToCompare.getLeftMate().getStart() > leftEnd) {
        			break;
        		} else {
        			if (isGermlineMatchNormal(leftStart, leftEnd, rightStart, rightEnd, pairToCompare)) {                    	
                        cluster.getMatchedReadPairs().add(pairToCompare);
                    }
        		}      			                    
            }
        }	
        
        return cluster;
    }

    
    private boolean isGermlineMatchNormal(int leftStart, int leftEnd,
            int rightStart, int rightEnd, MatePair pairToCompare) {
        boolean isMatch = false;
        if ((pairToCompare.getLeftMate().getStart() >= leftStart 
                && pairToCompare.getLeftMate().getStart() <= leftEnd)                        
                || (pairToCompare.getLeftMate().getEnd() >= leftStart 
                        && pairToCompare.getLeftMate().getEnd() <= leftEnd)) {
           
            //check the right start or end is within the left range
           if ((pairToCompare.getRightMate().getStart() >= rightStart 
                    && pairToCompare.getRightMate().getStart() <= rightEnd)                            
                    || (pairToCompare.getRightMate().getEnd() >= rightStart 
                            && pairToCompare.getRightMate().getEnd() <= rightEnd)                              
                   ) {
               isMatch = true;
            }
        }
        
        return isMatch;
    }
    
	private void addToClustersMap(DiscordantPairCluster cluster, String version) {
		if (version.equals("reference")) {
			if (clusters.containsKey(cluster.getReferenceKey())) {
				clusters.get(cluster.getReferenceKey()).add(cluster);
			} else {
				List<DiscordantPairCluster> clusterList = new ArrayList<DiscordantPairCluster>();
				clusterList.add(cluster);
				clusters.put(cluster.getReferenceKey(), clusterList);
			}
		} else {
			if (clusters.containsKey(cluster.getType())) {
	            clusters.get(cluster.getType()).add(cluster);
	        } else {
	            List<DiscordantPairCluster> clusterList = new ArrayList<DiscordantPairCluster>();
	            clusterList.add(cluster);
	            clusters.put(cluster.getType(), clusterList);
	        }
		}
	}
    
    public Map<String, List<DiscordantPairCluster>> getClustersMap() {
    	return this.clusters;
    }
}