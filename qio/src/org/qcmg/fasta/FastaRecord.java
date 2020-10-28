/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.fasta;

/**
 * this data set container class for records that have an id, and some data
 * <p>
 * eg. the .csfasta format from SOLiD sequence alignment files.  
 * Each record is split over two lines. The first line starts with '>' followed by the ID, 
 * the subsequent line contains the colour space sequence
 * 
 *  @author oholmes christina
 */
public class FastaRecord {

	private String id;
	private String data;
	
	public FastaRecord(String id, String data) {
		this.id = id;
		this.data = data;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}
	
	public void setData(String data) {
		this.data = data;
	}
	public String getData() {
		return data;
	}
}
