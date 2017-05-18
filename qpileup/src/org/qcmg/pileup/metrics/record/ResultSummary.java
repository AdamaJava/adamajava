/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup.metrics.record;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.qcmg.pileup.PileupConstants;

public class ResultSummary {
	
	
	private final String chromosome;
	private final int start;
	private int end;
	final Map<String, ResultRecord> resultRecords;
	private final long totalBases;
	boolean isBaseline;
	boolean isGreaterThanZero = false;	
	int misMapperCount = 0;
	double regularityScore = 0;
	boolean lowQualityMapper = false;
	private boolean hasClips = false;
	int positionCount;
	private final static double MIN_POSITION_COUNT  = 3;
	private final static double MIN_ADJUSTED_REGULARITY_SCORE  = 5;
	private final double HIGH_ADJUSTED_REGULARITY_SCORE = 20;
	private final double MIN_REGULARITY_SCORE = 100;
	private boolean highCoverageMapper = false;
	
	public ResultSummary(String chromosome, int start, int end, long totalBases, boolean isBaseline) {
		this.isBaseline = isBaseline;
		this.chromosome = chromosome;
		this.start = start;
		this.end = end;
		this.totalBases = totalBases;
		this.resultRecords = new HashMap<>();
	}
	
	public ResultSummary(String[] headerList, String line) {
		String[] values = line.split("\t");
		this.resultRecords = new HashMap<>();
		this.chromosome = values[0];
		this.start = Integer.parseInt(values[1]);
		this.end = Integer.parseInt(values[2]);
		
		int index = 3;
		for (int i=0; i<headerList.length; i++) {
			ResultRecord r = new ResultRecord(headerList[i], Long.parseLong(values[index]), Long.parseLong(values[index+1]), Double.parseDouble(values[index+2]));
			addRecord(r.getName(), r);
			if (!r.getName().equals(PileupConstants.METRIC_STRAND_BIAS)) {
				positionCount += r.getNumberPositions();
			}
			index +=3;			
		}
		this.totalBases = Integer.parseInt(values[values.length-1]);
		
	}
	
	public ResultSummary(ResultSummary rs, long count) {
		this.chromosome = rs.chromosome;
		this.start = rs.start;
		this.end = rs.end;
		this.totalBases = rs.totalBases;
		this.resultRecords = new HashMap<>(rs.resultRecords);		
		this.isGreaterThanZero = rs.isGreaterThanZero;
		this.misMapperCount = rs.misMapperCount;
		this.regularityScore = rs.regularityScore;
		this.isBaseline = rs.isBaseline;
		this.hasClips = rs.hasClips;
		this.lowQualityMapper = rs.lowQualityMapper;
		this.highCoverageMapper = rs.highCoverageMapper;
		this.positionCount = rs.getPositionCount();				
	}

	public int getMisMapperCount() {
		return misMapperCount;
	}
	
	public String getChromosome() {
		return chromosome;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public Map<String, ResultRecord> getResultRecords() {
		return resultRecords;
	}

	public boolean isBaseline() {
		return isBaseline;
	}

	public boolean isGreaterThanZero() {
		return isGreaterThanZero;
	}
	
	public long getTotalBases() {
		return totalBases;
	}

	public void setRegularityScore(int i) {
		this.regularityScore = i;		
	}
	
	public ResultRecord getRecord(String key) {
		return resultRecords.get(key);
	}

	public boolean isHasClips() {
		return hasClips;
	}

	public boolean isLowQualityMapper() {
		return this.lowQualityMapper;		
	}	

	public double getRegularityScore() {
		return this.regularityScore;
	}
	
	private int getPositionCount() {
		return this.positionCount;
	}
	
	public boolean isErrorRegion() {
		return isMisMapper() || isLowQualityMapper() || isHighCoverageMapper(); 
	}
	
	public boolean isWritableErrorRegion() {
		return isLowQualityMapper() || isWritableMismapper() || isHighCoverageMapper(); 
	}

	boolean highPositionCount() {
		return isMisMapper() && getPositionCountScore() > MIN_POSITION_COUNT;
	}

	public boolean isMisMapper() {
		return misMapperCount >=2; 
	}
	
	public boolean isRegularMapper() {
		return isMisMapper() && getAdjustedRegularityScore() > MIN_ADJUSTED_REGULARITY_SCORE && getFinalRegularityScore() > MIN_REGULARITY_SCORE ; 
	}
	
	public void addRecord(String key, ResultRecord record) {
		//add a record. Check to make see if the record has a count greater than 0
		if (!isGreaterThanZero && record != null) {
			if (record.getNumberPositions() > 0) {
				isGreaterThanZero = true;
			}
		}
		if (record != null) {
			if (record.getNumberPositions() > 0) {
				if (key.equals(PileupConstants.METRIC_CLIP)) {
					hasClips = true;
				}
				//increment scores
				if (!key.equals(PileupConstants.METRIC_STRAND_BIAS)) {
					regularityScore += record.getTotalRegularityScore();
					positionCount += record.getNumberPositions();
				}
				//decode on type;
				if (key.equals(PileupConstants.METRIC_MAPPING)) {
					lowQualityMapper = true;				
				} else if (key.equals(PileupConstants.METRIC_HCOV)) {
					highCoverageMapper = true;				
				} else if (!key.equals(PileupConstants.METRIC_UNMAPPED_MATE) && !key.equals(PileupConstants.METRIC_STRAND_BIAS)
						&& !key.equals(PileupConstants.METRIC_SNP)
						){
					if (!resultRecords.containsKey(key)) {
						misMapperCount++;				
					}
				}
			}
		}
		if (resultRecords.containsKey(key)) {
			resultRecords.get(key).mergeRecords(record);
		} else {
			resultRecords.put(key, record);
		}
		
		
	}
	
	public void addSummaryRecord(ResultSummary rs) {
		//extend the end
		if (rs.getEnd() > end) {
			setEnd(rs.getEnd());	
		}
		for (Entry<String, ResultRecord> e: rs.getResultRecords().entrySet()) {
			addRecord(e.getKey(), e.getValue());
		}		
	}
	
	public double getPositionCountScore() {
		return positionCount / ( (double)getEnd() - (double) getStart() + 1);
	}
	
	public double getFinalRegularityScore() {
		return regularityScore / ( (double)getEnd() - (double) getStart() + 1);		
	}
	
	public double getRegularityPositionCountScore() {
		int count = 0;
		//count only those metrics that count for a regular event
		for (Entry<String, ResultRecord> entry: resultRecords.entrySet()) {
			if (entry.getKey().equals(PileupConstants.METRIC_INDEL) || entry.getKey().equals(PileupConstants.METRIC_NONREFBASE)
					|| entry.getKey().equals(PileupConstants.METRIC_CLIP)) {
				count += entry.getValue().getNumberPositions();
			}
		}
		return count;
	}

	public double getAdjustedRegularityScore() {
		double regScore = regularityScore;
		double regularityCount = getRegularityPositionCountScore();
		double totalPositions = ( (double)getEnd() - (double) getStart() + 1);
		if (regScore > 0 && regularityCount > 0 && totalPositions > 0) {
			return (regScore/regularityCount)/totalPositions;
		} else {
			return 0;
		}		
	}	
	
	private String getScoreString(double score) {
		DecimalFormat df = new DecimalFormat("#.##");
		return df.format(score);
	}	

	public String getName() {
		if (isLowQualityMapper() && isWritableMismapper() && isHighCoverageMapper() || (isLowQualityMapper() && isWritableMismapper()) || (isHighCoverageMapper() && isWritableMismapper()) ||
				(isLowQualityMapper() && isHighCoverageMapper())) {
			return PileupConstants.MIXED;
		} else if ((isLowQualityMapper())){
			return PileupConstants.LOW_MAPPING_QUAL;
		} else if ((isHighCoverageMapper())){
			return PileupConstants.HCOV;
		}else {
			if (isWritableMismapper()) {
				return getMisMapperType();				
			}			
		}
		return "";
	}	
	
	private boolean isWritableMismapper() {
		return null != getMisMapperType(); 
	}

	public boolean isHighCoverageMapper() {
		return this.highCoverageMapper;
	}

	public String getMisMapperType() {
		if (highPositionCount()) {
			if (isRegularMapper()) {
				if (getAdjustedRegularityScore() > HIGH_ADJUSTED_REGULARITY_SCORE ) {
					return PileupConstants.REGULAR;
				} else {
					return PileupConstants.MIXED;
				}
			} else {						
				return PileupConstants.IRREGULAR;
			}
		} else {
			if (isRegularMapper()) {
				return PileupConstants.REGULAR;
			}
		}		
		
		return null;
	}
	
	public String getMixedCategories() {
		String types = "";
		if (isLowQualityMapper()) {
			types += PileupConstants.LOW_MAPPING_QUAL + ",";
		}
		if (isHighCoverageMapper()) {			
			types += PileupConstants.HCOV + ",";
		}
		if (isMisMapper()) {
			String mm =  getMisMapperType();
			
			if (mm != null) {
				if (mm.equals(PileupConstants.MIXED)) {
					types += PileupConstants.REGULAR + "," + PileupConstants.IRREGULAR; 
				} else {
					types += mm;
				}
			}
		}
		if (types.endsWith(",")) {
			return types.substring(0, types.length()-1);
		} else {
			return types;
		}
		
	}
	
	public String getColour(String name) {
		if (name.equals(PileupConstants.MIXED)) {
			return "#800080";//green
		} else if (name.equals(PileupConstants.LOW_MAPPING_QUAL)){
			return "#FF0000";//red
		} else if (name.equals(PileupConstants.HCOV)){
			return "#00FFFF";//aqua
		} else if (name.equals(PileupConstants.REGULAR)) {
			return "#0000EE";//blue
		} else {
			return "#ff00ff";//pink IRREGULAR
		}
	}	

	public String getAttributes() {
		String name = getName();
		String attrs = "Name=" + name + ";color=" + getColour(name) + ";PositionCountScore=" + getScoreString(getPositionCountScore()) + ";AdjustedRegularityScore=" + getScoreString(getAdjustedRegularityScore()) + ";RegularityScore=" + getScoreString(getRegularityScore());
		
		String mixed = new String();
		if (name.equals(PileupConstants.MIXED)) {
			mixed = getMixedCategories();
			attrs += ";Categories="+mixed;
		}
		if (name.equals(PileupConstants.HCOV) || mixed.contains(PileupConstants.HCOV)) {
			ResultRecord r = resultRecords.get(PileupConstants.METRIC_HCOV);
			DecimalFormat df = new DecimalFormat("##.##");
			double percent = r.getAverageScore();
			attrs += ";HCov=" + df.format(percent);
		}
		return attrs;
	}
	
	public String checkTmpRecords(String key) {
		ResultRecord rr =  resultRecords.get(key);
		return null != rr ? rr.toTmpString() : "0\t0\t0\t";
	}

	public String checkRecords(String key) {
		ResultRecord rr =  resultRecords.get(key);
		return null != rr ? rr.toString() : "0\t0\t";
	}
	
	public String toGFFString() {
		String result = getChromosome() + "\t";
		result += "qpileup" + "\t";
		result += "." + "\t";
		result += getStart() + "\t";
		result += getEnd() + "\t";
		result += getScoreString(getPositionCountScore()) + "\t";
		result += "." + "\t";
		result += "." + "\t";
		if (null != getAttributes()) {
			result += getAttributes();
		}
		return result;
	}
	
	public String toTmpString(String[] headerList) {
		StringBuilder sb = new StringBuilder();
		sb.append(chromosome).append("\t").append(start).append("\t").append(end).append("\t");
		
		for (String metric: headerList) {			
			if (resultRecords.containsKey(metric)) {
				sb.append(checkTmpRecords(metric));
			} 
		}

		sb.append(totalBases).append("\n");		
		return sb.toString();
	}
	
	public String toString(String[] headerList) {
		StringBuilder sb = new StringBuilder();
		sb.append(chromosome).append("\t").append(start).append("\t").append(end).append("\t");
		
		for (String metric: headerList) {
			if (resultRecords.containsKey(metric)) {
				sb.append(checkRecords(metric));
			} else {
				sb.append("\t0\t0");
			}
		}
		sb.append(totalBases).append("\n");
		return sb.toString();
	}

}

	
