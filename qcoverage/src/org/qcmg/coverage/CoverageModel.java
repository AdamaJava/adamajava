/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.coverage;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.eclipse.persistence.oxm.annotations.XmlNameTransformer;

import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
//import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
//import javax.xml.bind.annotation.XmlType;

//@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Coverage")
@XmlNameTransformer(NameGenerator.class)
public class CoverageModel {
	
    public BigInteger getBases() {
		return bases;
	}
	public void setBases(BigInteger bases) {
		this.bases = bases;
	}
	public String getAt() {
		return at;
	}
	public void setAt(String at) {
		this.at = at;
	}
	
	@XmlAttribute(name = "bases", required = true)
//    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger bases;
    @XmlAttribute(name = "at", required = true)
    protected String at;

}
