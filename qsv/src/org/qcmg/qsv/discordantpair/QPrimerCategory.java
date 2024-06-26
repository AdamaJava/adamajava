/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package org.qcmg.qsv.discordantpair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.qcmg.common.util.Constants;
import org.qcmg.qsv.util.QSVConstants;

public class QPrimerCategory {

	private Map<String, AtomicInteger> map;
	private String primaryCategoryNo;
	private String mixedCategories;
	private int startLeft;
	private int endLeft;
	private int startRight;
	private int endRight;
	private String reverseFlag;
	private final String zpType;
	private String leftChr;
	private String rightChr;
	private final String pairType;
	
	public QPrimerCategory(String zpType, String leftChr, String rightChr, String id, String pairType) {
		this( zpType,  leftChr,  rightChr,  pairType);
	}

	public QPrimerCategory(String zpType, String leftChr, String rightChr, String pairType) {
		this.map = new HashMap<>();
		this.zpType = zpType;
		this.leftChr = leftChr;
		this.rightChr = rightChr;
		this.mixedCategories = "";
		this.pairType = pairType;
	}

	public Map<String, AtomicInteger> getMap() {
		return map;
	}

	public void setMap(Map<String, AtomicInteger> map) {
		this.map = map;
	}

	public String getPrimaryCategoryNo() {
		return primaryCategoryNo;
	}

	public void setPrimaryCategoryNo(String primaryCategoryNo) {
		this.primaryCategoryNo = primaryCategoryNo;
	}

	public String getMixedCategories() {
		return mixedCategories;
	}

	public int getStartLeft() {
		return startLeft;
	}

	public int getEndLeft() {
		return endLeft;
	}

	public int getStartRight() {
		return startRight;
	}

	public int getEndRight() {
		return endRight;
	}

	public String getReverseFlag() {
		return reverseFlag;
	}

	public String getZpType() {
		return zpType;
	}

	public String getLeftChr() {
		return leftChr;
	}

	public String getRightChr() {
		return rightChr;
	}

	public void findClusterCategory(List<MatePair> clusterMatePairs, int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) throws Exception {
		//find the categories of each read pair		
		countCategories(clusterMatePairs);
		
		if (map.isEmpty()) {
			primaryCategoryNo = "unknown";
		} else {
			findCategoryNo();
			findQPrimerSites(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
		}
	}

	void countCategories(List<MatePair> clusterMatePairs) {
		String cat = null;
        switch (pairType) {
            case "lmp" -> {
                for (MatePair p : clusterMatePairs) {
                    cat = p.getSVCategoryForLMP();
                }
            }
            case "pe" -> {
                for (MatePair p : clusterMatePairs) {
                    cat = p.getSVCategoryForPE();
                }
            }
            case "imp" -> {
                for (MatePair p : clusterMatePairs) {
                    cat = p.getSVCategoryForIMP();
                }
            }
        }
		
		if (cat != null) {
			addToCategoryMap(cat);
		}
	}
	

	public void findQPrimerSites(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
        switch (primaryCategoryNo) {
            case QSVConstants.ORIENTATION_1, QSVConstants.ORIENTATION_3 -> {

                if (pairType.equals("lmp")) {
                    setStandardEnds(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
                } else {
                    if (primaryCategoryNo.equals(QSVConstants.ORIENTATION_1)) {
                        setCat1PEEnds(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
                    } else {
                        setCat3PEEnds(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
                    }
                }

                if (primaryCategoryNo.equals(QSVConstants.ORIENTATION_3)) {
                    reverseFlag = "true";
                } else if (primaryCategoryNo.equals(QSVConstants.ORIENTATION_4)) {

                } else {
                    reverseFlag = "false";
                }
            }
            case QSVConstants.ORIENTATION_2 -> {
                if (pairType.equals("lmp")) {
                    setSwappedEnds(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
                } else {
                    setCat2SwappedEnds(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
                }

                reverseFlag = "false";
            }
            case QSVConstants.ORIENTATION_4 -> {
                if (pairType.equals("lmp")) {
                    setCat4Ends(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
                } else {
                    setCat4PEEnds(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
                }

                reverseFlag = "lefttrue";
            }
            case QSVConstants.ORIENTATION_5 -> {
                setCat5SwappedEnds(clusterLeftStart, clusterLeftEnd, clusterRightStart, clusterRightEnd);
                reverseFlag = "false";
            }
        }
	}

	private void setCat1PEEnds(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
		startLeft = clusterLeftStart;
		endLeft = clusterLeftEnd - 50;
		startRight = clusterRightStart + 50;
		endRight = clusterRightEnd;		
	}
	
	private void setCat3PEEnds(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
		startLeft = clusterLeftStart;
		endLeft = clusterLeftEnd - 50;
		startRight = clusterRightStart;
		endRight = clusterRightEnd - 50;	
		String chr = leftChr;
		leftChr = rightChr;
		rightChr = chr;
	}
	
	private void setCat4PEEnds(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
		startLeft = clusterLeftStart+50;
		endLeft = clusterLeftEnd;
		startRight = clusterRightStart+50;
		endRight = clusterRightEnd;	
	}
	
	private void setCat2SwappedEnds(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
		startLeft = clusterRightStart;
		endLeft = clusterRightEnd - 50;		
		startRight = clusterLeftStart + 50;
		endRight = clusterLeftEnd;
		String chr = leftChr;
		leftChr = rightChr;
		rightChr = chr;
	}

	private void setCat5SwappedEnds(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
		int leftPrimerStart = clusterLeftStart + 50;
		int rightPrimerEnd = clusterRightEnd - 50;
		
		int leftPrimerEnd = (int)Math.ceil(((double)clusterLeftStart + clusterRightEnd)/ 2);
		int rightPrimerStart = leftPrimerEnd;
		
		//swap!
		startLeft = rightPrimerStart;
		endLeft = rightPrimerEnd;
		startRight = leftPrimerStart;
		endRight = leftPrimerEnd;
		
		String chr = leftChr;
		leftChr = rightChr;
		rightChr = chr;
	}

	private void setStandardEnds(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
		
		endLeft = (int) Math.ceil(((double)clusterLeftEnd + clusterLeftStart) / 2);	
		startLeft = endLeft-499;
		if (startLeft < clusterLeftStart) {
			startLeft = clusterLeftStart;
		}
		
		startRight = (int) Math.ceil(((double)clusterRightEnd + clusterRightStart) / 2);
		
		endRight = startRight + 499;
		if (endRight > clusterRightEnd) {
			endRight = clusterRightEnd;
		}		
	}
	
	private void setCat4Ends(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
		
		startLeft = (int) Math.ceil(((double)clusterLeftEnd + clusterLeftStart) / 2);
		
		endLeft = startLeft + 499;
		if (endLeft > clusterLeftEnd) {
			endLeft = clusterLeftEnd;
		}
		
		endRight = (int) Math.ceil(((double)clusterRightEnd + clusterRightStart) / 2);	
		
		startRight = endRight - 499;
		if (startRight < clusterRightStart) {
			startRight = clusterRightStart;
		}		
	}
	
	private void setSwappedEnds(int clusterLeftStart, int clusterLeftEnd, int clusterRightStart, int clusterRightEnd) {
		//swap the left and right chr
		String tempRight = leftChr;
		String tempLeft = rightChr;
		
		leftChr = tempLeft;
		rightChr = tempRight;
		endLeft = (int) Math.ceil(((double)clusterRightEnd + clusterRightStart) / 2);	
		
		startLeft = endLeft - 499;
		if (startLeft < clusterRightStart) {
			startLeft = clusterRightStart;
		}
		
		startRight = (int) Math.ceil(((double)clusterLeftEnd + clusterLeftStart) / 2);

		endRight = startRight + 499;
		if (endRight > clusterLeftEnd) {
			endRight = clusterLeftEnd;
		}
	}

	private void addToCategoryMap(String key) {
		map.computeIfAbsent(key, f -> new AtomicInteger()).incrementAndGet();
	}
	
	public void findCategoryNo() throws Exception {
		this.primaryCategoryNo = "";
			if (map.isEmpty()) {
				throw new Exception ("Pairs could not be assigned to qPrimer category");
			} else {
				if (map.size() == 1) {
					for (Entry<String, AtomicInteger> entry: map.entrySet()) {
						primaryCategoryNo = entry.getKey();
					}    			
				} else {
					List<String> categoryNos = new ArrayList<>();
					int max = -1;
					
					// sort keys so that tests pass....
					List<String> keys = new ArrayList<>(map.keySet());
					Collections.sort(keys);
					Collections.reverse(keys);
					
					for (String key : keys) {
						AtomicInteger value = map.get(key);
						mixedCategories += "Cat" + key + "(" + value.get() + "),";
						if (value.intValue() > max) {
							max = value.intValue();
						}
					}
					
					//see if it happens more than once
					for (Entry<String, AtomicInteger> entry: map.entrySet()) {
						if (entry.getValue().intValue() == max) {
							categoryNos.add(entry.getKey());
						}
					}			
					primaryCategoryNo = categoryNos.getFirst();
				}
		}
	}
	
	public String toString(String svId) {

        return svId + Constants.TAB +
                //left
                leftChr + Constants.COLON +
                startLeft + '-' +
                endLeft + Constants.TAB +
                //right
                rightChr + Constants.COLON +
                startRight + '-' +
                endRight + Constants.TAB +
                reverseFlag + Constants.TAB +
                primaryCategoryNo + Constants.TAB +
                mixedCategories;
	}
}
