package org.qcmg.primerinput;


public class PrimerInputRecord {

    protected String sequenceId;
    protected String sequenceTemplate;
    protected PrimerSequenceTarget sequenceTarget;
    protected int primerProductMinTm;
    protected int primerProductMaxTm;
    protected double primerDnaConc;
    protected double primerSaltConc;
    protected int primerMinTm;
    protected int primerOptTm;
    protected int primerMaxTm;
    protected int primerMinSize;
    protected int primerOptSize;
    protected int primerMaxSize;
    protected PrimerSizeRange primerProductSizeRange;
    protected boolean primerExplainFlag;
    protected int primerNumReturn;
    protected boolean primerNumNsAccepted;

    /**
     * Gets the value of the sequenceId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSequenceId() {
        return sequenceId;
    }

    /**
     * Sets the value of the sequenceId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSequenceId(String value) {
        this.sequenceId = value;
    }

    /**
     * Gets the value of the sequenceTemplate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getSequenceTemplate() {
        return sequenceTemplate;
    }

    /**
     * Sets the value of the sequenceTemplate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setSequenceTemplate(String value) {
        this.sequenceTemplate = value;
    }

    /**
     * Gets the value of the sequenceTarget property.
     * 
     * @return
     *     possible object is
     *     {@link PrimerSequenceTarget }
     *     
     */
    public PrimerSequenceTarget getSequenceTarget() {
        return sequenceTarget;
    }

    /**
     * Sets the value of the sequenceTarget property.
     * 
     * @param value
     *     allowed object is
     *     {@link PrimerSequenceTarget }
     *     
     */
    public void setSequenceTarget(PrimerSequenceTarget value) {
        this.sequenceTarget = value;
    }

    /**
     * Gets the value of the primerProductMinTm property.
     * 
     */
    public int getPrimerProductMinTm() {
        return primerProductMinTm;
    }

    /**
     * Sets the value of the primerProductMinTm property.
     * 
     */
    public void setPrimerProductMinTm(int value) {
        this.primerProductMinTm = value;
    }

    /**
     * Gets the value of the primerProductMaxTm property.
     * 
     */
    public int getPrimerProductMaxTm() {
        return primerProductMaxTm;
    }

    /**
     * Sets the value of the primerProductMaxTm property.
     * 
     */
    public void setPrimerProductMaxTm(int value) {
        this.primerProductMaxTm = value;
    }

    /**
     * Gets the value of the primerDnaConc property.
     * 
     */
    public double getPrimerDnaConc() {
        return primerDnaConc;
    }

    /**
     * Sets the value of the primerDnaConc property.
     * 
     */
    public void setPrimerDnaConc(double value) {
        this.primerDnaConc = value;
    }

    /**
     * Gets the value of the primerSaltConc property.
     * 
     */
    public double getPrimerSaltConc() {
        return primerSaltConc;
    }

    /**
     * Sets the value of the primerSaltConc property.
     * 
     */
    public void setPrimerSaltConc(double value) {
        this.primerSaltConc = value;
    }

    /**
     * Gets the value of the primerMinTm property.
     * 
     */
    public int getPrimerMinTm() {
        return primerMinTm;
    }

    /**
     * Sets the value of the primerMinTm property.
     * 
     */
    public void setPrimerMinTm(int value) {
        this.primerMinTm = value;
    }

    /**
     * Gets the value of the primerOptTm property.
     * 
     */
    public int getPrimerOptTm() {
        return primerOptTm;
    }

    /**
     * Sets the value of the primerOptTm property.
     * 
     */
    public void setPrimerOptTm(int value) {
        this.primerOptTm = value;
    }

    /**
     * Gets the value of the primerMaxTm property.
     * 
     */
    public int getPrimerMaxTm() {
        return primerMaxTm;
    }

    /**
     * Sets the value of the primerMaxTm property.
     * 
     */
    public void setPrimerMaxTm(int value) {
        this.primerMaxTm = value;
    }

    /**
     * Gets the value of the primerMinSize property.
     * 
     */
    public int getPrimerMinSize() {
        return primerMinSize;
    }

    /**
     * Sets the value of the primerMinSize property.
     * 
     */
    public void setPrimerMinSize(int value) {
        this.primerMinSize = value;
    }

    /**
     * Gets the value of the primerOptSize property.
     * 
     */
    public int getPrimerOptSize() {
        return primerOptSize;
    }

    /**
     * Sets the value of the primerOptSize property.
     * 
     */
    public void setPrimerOptSize(int value) {
        this.primerOptSize = value;
    }

    /**
     * Gets the value of the primerMaxSize property.
     * 
     */
    public int getPrimerMaxSize() {
        return primerMaxSize;
    }

    /**
     * Sets the value of the primerMaxSize property.
     * 
     */
    public void setPrimerMaxSize(int value) {
        this.primerMaxSize = value;
    }

    /**
     * Gets the value of the primerProductSizeRange property.
     * 
     * @return
     *     possible object is
     *     {@link PrimerSizeRange }
     *     
     */
    public PrimerSizeRange getPrimerProductSizeRange() {
        return primerProductSizeRange;
    }

    /**
     * Sets the value of the primerProductSizeRange property.
     * 
     * @param value
     *     allowed object is
     *     {@link PrimerSizeRange }
     *     
     */
    public void setPrimerProductSizeRange(PrimerSizeRange value) {
        this.primerProductSizeRange = value;
    }

    /**
     * Gets the value of the primerExplainFlag property.
     * 
     */
    public boolean isPrimerExplainFlag() {
        return primerExplainFlag;
    }

    /**
     * Sets the value of the primerExplainFlag property.
     * 
     */
    public void setPrimerExplainFlag(boolean value) {
        this.primerExplainFlag = value;
    }

    /**
     * Gets the value of the primerNumReturn property.
     * 
     */
    public int getPrimerNumReturn() {
        return primerNumReturn;
    }

    /**
     * Sets the value of the primerNumReturn property.
     * 
     */
    public void setPrimerNumReturn(int value) {
        this.primerNumReturn = value;
    }

    /**
     * Gets the value of the primerNumNsAccepted property.
     * 
     */
    public boolean isPrimerNumNsAccepted() {
        return primerNumNsAccepted;
    }

    /**
     * Sets the value of the primerNumNsAccepted property.
     * 
     */
    public void setPrimerNumNsAccepted(boolean value) {
        this.primerNumNsAccepted = value;
    }

}
