/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.2-147 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.10.25 at 10:52:22 AM EST 
//


package org.qcmg.primeroutput;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for primerOutputRecord complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="primerOutputRecord">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="pairPenalty" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="leftPenalty" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="rightPenalty" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="leftSequence" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="rightSequence" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="left" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="right" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="leftTm" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="rightTm" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="leftGcPercent" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="rightGcPercent" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="leftSelfAny" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="rightSelfAny" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="leftSelfEnd" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="rightSelfEnd" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="leftEndStability" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="rightEndStability" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="pairComplAny" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="pairComplEnd" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="pairProductSize" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="pairProductTm" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="pairProductTmOligoTmDiff" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *         &lt;element name="pairTOptA" type="{http://www.w3.org/2001/XMLSchema}double"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "primerOutputRecord", propOrder = {
    "pairPenalty",
    "leftPenalty",
    "rightPenalty",
    "leftSequence",
    "rightSequence",
    "left",
    "right",
    "leftTm",
    "rightTm",
    "leftGcPercent",
    "rightGcPercent",
    "leftSelfAny",
    "rightSelfAny",
    "leftSelfEnd",
    "rightSelfEnd",
    "leftEndStability",
    "rightEndStability",
    "pairComplAny",
    "pairComplEnd",
    "pairProductSize",
    "pairProductTm",
    "pairProductTmOligoTmDiff",
    "pairTOptA"
})
public class PrimerOutputRecord {

    protected double pairPenalty;
    protected double leftPenalty;
    protected double rightPenalty;
    @XmlElement(required = true)
    protected String leftSequence;
    @XmlElement(required = true)
    protected String rightSequence;
    @XmlElement(required = true)
    protected String left;
    @XmlElement(required = true)
    protected String right;
    protected double leftTm;
    protected double rightTm;
    protected double leftGcPercent;
    protected double rightGcPercent;
    protected double leftSelfAny;
    protected double rightSelfAny;
    protected double leftSelfEnd;
    protected double rightSelfEnd;
    protected double leftEndStability;
    protected double rightEndStability;
    protected double pairComplAny;
    protected double pairComplEnd;
    protected int pairProductSize;
    protected double pairProductTm;
    protected double pairProductTmOligoTmDiff;
    protected double pairTOptA;

    /**
     * Gets the value of the pairPenalty property.
     * 
     */
    public double getPairPenalty() {
        return pairPenalty;
    }

    /**
     * Sets the value of the pairPenalty property.
     * 
     */
    public void setPairPenalty(double value) {
        this.pairPenalty = value;
    }

    /**
     * Gets the value of the leftPenalty property.
     * 
     */
    public double getLeftPenalty() {
        return leftPenalty;
    }

    /**
     * Sets the value of the leftPenalty property.
     * 
     */
    public void setLeftPenalty(double value) {
        this.leftPenalty = value;
    }

    /**
     * Gets the value of the rightPenalty property.
     * 
     */
    public double getRightPenalty() {
        return rightPenalty;
    }

    /**
     * Sets the value of the rightPenalty property.
     * 
     */
    public void setRightPenalty(double value) {
        this.rightPenalty = value;
    }

    /**
     * Gets the value of the leftSequence property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLeftSequence() {
        return leftSequence;
    }

    /**
     * Sets the value of the leftSequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLeftSequence(String value) {
        this.leftSequence = value;
    }

    /**
     * Gets the value of the rightSequence property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRightSequence() {
        return rightSequence;
    }

    /**
     * Sets the value of the rightSequence property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRightSequence(String value) {
        this.rightSequence = value;
    }

    /**
     * Gets the value of the left property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLeft() {
        return left;
    }

    /**
     * Sets the value of the left property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLeft(String value) {
        this.left = value;
    }

    /**
     * Gets the value of the right property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRight() {
        return right;
    }

    /**
     * Sets the value of the right property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRight(String value) {
        this.right = value;
    }

    /**
     * Gets the value of the leftTm property.
     * 
     */
    public double getLeftTm() {
        return leftTm;
    }

    /**
     * Sets the value of the leftTm property.
     * 
     */
    public void setLeftTm(double value) {
        this.leftTm = value;
    }

    /**
     * Gets the value of the rightTm property.
     * 
     */
    public double getRightTm() {
        return rightTm;
    }

    /**
     * Sets the value of the rightTm property.
     * 
     */
    public void setRightTm(double value) {
        this.rightTm = value;
    }

    /**
     * Gets the value of the leftGcPercent property.
     * 
     */
    public double getLeftGcPercent() {
        return leftGcPercent;
    }

    /**
     * Sets the value of the leftGcPercent property.
     * 
     */
    public void setLeftGcPercent(double value) {
        this.leftGcPercent = value;
    }

    /**
     * Gets the value of the rightGcPercent property.
     * 
     */
    public double getRightGcPercent() {
        return rightGcPercent;
    }

    /**
     * Sets the value of the rightGcPercent property.
     * 
     */
    public void setRightGcPercent(double value) {
        this.rightGcPercent = value;
    }

    /**
     * Gets the value of the leftSelfAny property.
     * 
     */
    public double getLeftSelfAny() {
        return leftSelfAny;
    }

    /**
     * Sets the value of the leftSelfAny property.
     * 
     */
    public void setLeftSelfAny(double value) {
        this.leftSelfAny = value;
    }

    /**
     * Gets the value of the rightSelfAny property.
     * 
     */
    public double getRightSelfAny() {
        return rightSelfAny;
    }

    /**
     * Sets the value of the rightSelfAny property.
     * 
     */
    public void setRightSelfAny(double value) {
        this.rightSelfAny = value;
    }

    /**
     * Gets the value of the leftSelfEnd property.
     * 
     */
    public double getLeftSelfEnd() {
        return leftSelfEnd;
    }

    /**
     * Sets the value of the leftSelfEnd property.
     * 
     */
    public void setLeftSelfEnd(double value) {
        this.leftSelfEnd = value;
    }

    /**
     * Gets the value of the rightSelfEnd property.
     * 
     */
    public double getRightSelfEnd() {
        return rightSelfEnd;
    }

    /**
     * Sets the value of the rightSelfEnd property.
     * 
     */
    public void setRightSelfEnd(double value) {
        this.rightSelfEnd = value;
    }

    /**
     * Gets the value of the leftEndStability property.
     * 
     */
    public double getLeftEndStability() {
        return leftEndStability;
    }

    /**
     * Sets the value of the leftEndStability property.
     * 
     */
    public void setLeftEndStability(double value) {
        this.leftEndStability = value;
    }

    /**
     * Gets the value of the rightEndStability property.
     * 
     */
    public double getRightEndStability() {
        return rightEndStability;
    }

    /**
     * Sets the value of the rightEndStability property.
     * 
     */
    public void setRightEndStability(double value) {
        this.rightEndStability = value;
    }

    /**
     * Gets the value of the pairComplAny property.
     * 
     */
    public double getPairComplAny() {
        return pairComplAny;
    }

    /**
     * Sets the value of the pairComplAny property.
     * 
     */
    public void setPairComplAny(double value) {
        this.pairComplAny = value;
    }

    /**
     * Gets the value of the pairComplEnd property.
     * 
     */
    public double getPairComplEnd() {
        return pairComplEnd;
    }

    /**
     * Sets the value of the pairComplEnd property.
     * 
     */
    public void setPairComplEnd(double value) {
        this.pairComplEnd = value;
    }

    /**
     * Gets the value of the pairProductSize property.
     * 
     */
    public int getPairProductSize() {
        return pairProductSize;
    }

    /**
     * Sets the value of the pairProductSize property.
     * 
     */
    public void setPairProductSize(int value) {
        this.pairProductSize = value;
    }

    /**
     * Gets the value of the pairProductTm property.
     * 
     */
    public double getPairProductTm() {
        return pairProductTm;
    }

    /**
     * Sets the value of the pairProductTm property.
     * 
     */
    public void setPairProductTm(double value) {
        this.pairProductTm = value;
    }

    /**
     * Gets the value of the pairProductTmOligoTmDiff property.
     * 
     */
    public double getPairProductTmOligoTmDiff() {
        return pairProductTmOligoTmDiff;
    }

    /**
     * Sets the value of the pairProductTmOligoTmDiff property.
     * 
     */
    public void setPairProductTmOligoTmDiff(double value) {
        this.pairProductTmOligoTmDiff = value;
    }

    /**
     * Gets the value of the pairTOptA property.
     * 
     */
    public double getPairTOptA() {
        return pairTOptA;
    }

    /**
     * Sets the value of the pairTOptA property.
     * 
     */
    public void setPairTOptA(double value) {
        this.pairTOptA = value;
    }

}