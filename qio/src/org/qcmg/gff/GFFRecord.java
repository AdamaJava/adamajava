package org.qcmg.gff;

import java.util.HashMap;
import java.util.Map;

import org.qcmg.record.Record;

/**
 * @author jpearson
 * @version $Id: GffRecord.java,v 1.8 2009/08/17 20:17:35 jbeckstr Exp $
 * 
 *          Data container class for records from SOLiD GFF format sequence
 *          alignment files. GFF is a tab-separated text file with unix-style
 *          line endings and the following fields of which the last two are
 *          optional:
 * 
 *          Fieldname Example value 1. seqname 1231_644_1328_F3 2. source solid
 *          3. feature read 4. start 97 5. end 121 6. score 13.5 7. strand - 8.
 *          frame . 9. [attributes] b=TAGGGTTAGGGTTGGGTTAGGGTTA; c=AAA;
 *          g=T320010320010100103000103; i=1; p=1.000;
 *          q=23,28,27,20,17,12,24,16,20,8,13,26,28,2
 *          4,13,13,27,14,19,4,23,16,19,9,14; r=20_2; s=a20; u=0,1 10.
 *          [comments]
 */
public class GFFRecord implements Record {

	// private String originalLine; // original line
	private String seqname; // read ID
	private String source; // should always be "solid"
	private String feature; // should always be "read"
	private int start; // start position of mapping to reference
	private int end; // end position of mapping to reference
	private double score; // quality of mapping
	private String strand; // - or +
	private String frame; // 1,2,3,.
	private String attribStr; // this is the gold!
	private String comments; // comments (seldom present)
	private Map<String, String> attributes; // deconstruct attribStr

	/**
	 * Constructor 1
	 * 
	 * @param textRecord
	 *            text GFF Record typically read from GFF file
	 * @throws Exception
	 * @throws QProfilerException
	 */
	public GFFRecord(String[] fields) throws Exception {
		// public GffRecord( String textRecord, String delimiter ) {
		// this(); // call constructor 0
		// originalLine = textRecord;

		// String[] fields = textRecord.split( "\t" );
		seqname = fields[0];
		source = fields[1];
		feature = fields[2];
		start = Integer.parseInt(fields[3]);
		end = Integer.parseInt(fields[4]);
		score = Double.parseDouble(fields[5]);
		strand = fields[6];
		frame = fields[7];

		// Cope with the optional attribute field
		if (fields.length > 8) {
			attributes = new HashMap<String, String>();

			attribStr = fields[8];
			String[] tmpattribs = attribStr.split(";");
			for (int i = 0; i < tmpattribs.length; i++) {
				String[] attrFields = tmpattribs[i].split("=");
				if (attrFields.length < 2) {
					throw new Exception("Attribute [" + tmpattribs[i]
							+ "] is badly formed");
				}
				attributes.put(attrFields[0], attrFields[1]);
			}
		}

		// And comments is also optional
		if (fields.length > 9) {
			comments = fields[9];
		}
	}

	public String getSeqname() {
		return seqname;
	}

	public void setSeqname(String seqname) {
		this.seqname = seqname;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getFeature() {
		return feature;
	}

	public void setFeature(String feature) {
		this.feature = feature;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getStrand() {
		return strand;
	}

	public void setStrand(String strand) {
		this.strand = strand;
	}

	public String getFrame() {
		return frame;
	}

	public void setFrame(String frame) {
		this.frame = frame;
	}

	public String getAttribStr() {
		return attribStr;
	}

	public void setAttribStr(String attribStr) {
		this.attribStr = attribStr;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public void setAttribute(String key, String value) {
		this.attributes.put(key, value);
	}

	public String getAttribute(String key) {
		return null != attributes ? attributes.get(key) : null;
	}
}
