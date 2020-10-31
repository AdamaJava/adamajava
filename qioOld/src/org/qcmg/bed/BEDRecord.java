/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.bed;



public class BEDRecord {
	
	private final static char T = '\t';

	String chrom;
	int chromStart;
	int chromEnd;
	String name;
	int score;
	String strand;
	int thickStart;
	int thickEnd;
	String itemRGB;
	int blockCount;
	int blockSizes;
	int blockStarts;
	
	public String getChrom() {
		return chrom;
	}
	public void setChrom(String chrom) {
		this.chrom = chrom;
	}
	public int getChromStart() {
		return chromStart;
	}
	public void setChromStart(int chromStart) {
		this.chromStart = chromStart;
	}
	public int getChromEnd() {
		return chromEnd;
	}
	public void setChromEnd(int chromEnd) {
		this.chromEnd = chromEnd;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public String getStrand() {
		return strand;
	}
	public void setStrand(String strand) {
		this.strand = strand;
	}
	public int getThickStart() {
		return thickStart;
	}
	public void setThickStart(int thickStart) {
		this.thickStart = thickStart;
	}
	public int getThickEnd() {
		return thickEnd;
	}
	public void setThickEnd(int thickEnd) {
		this.thickEnd = thickEnd;
	}
	public String getItemRGB() {
		return itemRGB;
	}
	public void setItemRGB(String itemRGB) {
		this.itemRGB = itemRGB;
	}
	public int getBlockCount() {
		return blockCount;
	}
	public void setBlockCount(int blockCount) {
		this.blockCount = blockCount;
	}
	public int getBlockSizes() {
		return blockSizes;
	}
	public void setBlockSizes(int blockSizes) {
		this.blockSizes = blockSizes;
	}
	public int getBlockStarts() {
		return blockStarts;
	}
	public void setBlockStarts(int blockStarts) {
		this.blockStarts = blockStarts;
	}
}
