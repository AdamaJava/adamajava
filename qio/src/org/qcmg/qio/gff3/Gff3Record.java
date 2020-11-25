/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qio.gff3;

import org.qcmg.common.util.Constants;
import org.qcmg.common.util.TabTokenizer;

public class Gff3Record {

    protected String seqId;
    protected String source;
    protected String type;
    protected int start;
    protected int end;
    protected String score;
    protected String strand;
    protected String phase;
    protected String attributes;
    protected String rawData;
    
    public Gff3Record() {}
	public Gff3Record(final String line)  {
		String[] params = TabTokenizer.tokenize(line);
		if (8 > params.length) {			 
			throw new IllegalArgumentException("Bad GFF3 format. Insufficient columns: '" + line + "'");
		}
		 
		setRawData(line);
		setSeqId(params[0]);
		setSource(params[1]);
		setType(params[2]);
		setStart(Integer.parseInt(params[3]));
		setEnd(Integer.parseInt(params[4]));
		setScore(params[5]);
		setStrand(params[6]);
		setPhase(params[7]);
		if (8 < params.length) {
			setAttributes(params[8]);
		}		 
	} 

    /**
     * Gets the value of the seqId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSeqId() {
        return seqId;
    }

    /**
     * Sets the value of the seqId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSeqId(String value) {
        this.seqId = value;
    }

    /**
     * Gets the value of the source property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSource() {
        return source;
    }

    /**
     * Sets the value of the source property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSource(String value) {
        this.source = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the start property.
     * 
     */
    public int getStart() {
        return start;
    }

    /**
     * Sets the value of the start property.
     * 
     */
    public void setStart(int value) {
        this.start = value;
    }

    /**
     * Gets the value of the end property.
     * 
     */
    public int getEnd() {
        return end;
    }

    /**
     * Sets the value of the end property.
     * 
     */
    public void setEnd(int value) {
        this.end = value;
    }

    /**
     * Gets the value of the score property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScore() {
        return score;
    }

    /**
     * Sets the value of the score property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScore(String value) {
        this.score = value;
    }

    /**
     * Gets the value of the strand property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrand() {
        return strand;
    }

    /**
     * Sets the value of the strand property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrand(String value) {
        this.strand = value;
    }

    /**
     * Gets the value of the phase property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPhase() {
        return phase;
    }

    /**
     * Sets the value of the phase property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPhase(String value) {
        this.phase = value;
    }

    /**
     * Gets the value of the attributes property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAttributes() {
        return attributes;
    }

    /**
     * Sets the value of the attributes property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAttributes(String value) {
        this.attributes = value;
    }

    /**
     * Gets the value of the rawData property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRawData() {
        return rawData;
    }

    /**
     * Sets the value of the rawData property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRawData(String value) {
        this.rawData = value;
    }
    
	public String toString() {
		StringBuilder result = new StringBuilder(getSeqId()).append(Constants.TAB);
		result.append(getSource()).append(Constants.TAB);
		result.append(getType()).append(Constants.TAB);
		result.append(getStart()).append(Constants.TAB);
		result.append(getEnd()).append(Constants.TAB);
		result.append(getScore()).append(Constants.TAB);
		result.append(getStrand()).append(Constants.TAB);
		result.append(getPhase()).append(Constants.TAB);
		if (null != getAttributes()) {
			result.append(getAttributes());
		}
		return result.toString();
	}

}
