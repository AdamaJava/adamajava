/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule.snppicker;

import java.text.DecimalFormat;

public class VariantRecord {
	
	private final static char DEFAULT_CHAR = '\u0000';
	private final static DecimalFormat df = new DecimalFormat("0.0000");

	private String dbSnpID;
	private char dbSnpStrand;
	private String dbSnpRef_Alt;
	private float illGCScore;
	private char illAllele1;
	private char illAllele2;
	private boolean illTypeHom;
	private String illuminaRef;
//	private String illuminaAlt;
	private String illuminaSNP;
	private char gffRef;
	private char gffGenotype;
	private String gffAlt;
	private char vcfRef;
	private char vcfAlt;
	private String vcfGenotype;
	private String pileup;
	private String positionMatch;
	private String genotypeMatch;
	
	public String getDbSnpID() {
		return dbSnpID;
	}
	public void setDbSnpID(String dbSnpID) {
		this.dbSnpID = dbSnpID;
	}
	public String getIlluminaRef() {
		return illuminaRef;
	}
	public void setIlluminaRef(String illuminaRef) {
		this.illuminaRef = illuminaRef;
	}
//	public String getIlluminaAlt() {
//		return illuminaAlt;
//	}
//	public void setIlluminaAlt(String illuminaAlt) {
//		this.illuminaAlt = illuminaAlt;
//	} 
	public char getGffRef() {
		return gffRef;
	}
	public void setGffRef(char gffRef) {
		this.gffRef = gffRef;
	}
	public char getGffGenotype() {
		return gffGenotype;
	}
	public void setGffGenotype(char gffGenotype) {
		this.gffGenotype = gffGenotype;
	}
	public String getGffAlt() {
		return gffAlt;
	}
	public void setGffAlt(String gffAlt) {
		this.gffAlt = gffAlt;
	}
	public char getVcfRef() {
		return vcfRef;
	}
	public void setVcfRef(char vcfRef) {
		this.vcfRef = vcfRef;
	}
	public char getVcfAlt() {
		return vcfAlt;
	}
	public void setVcfAlt(char vcfAlt) {
		this.vcfAlt = vcfAlt;
	}
	public String getVcfGenotype() {
		return vcfGenotype;
	}
	public void setVcfGenotype(String vcfGenotype) {
		this.vcfGenotype = vcfGenotype;
	}
	public void setIlluminaSNP(String illuminaSNP) {
		this.illuminaSNP = illuminaSNP;
	}
	public String getIlluminaSNP() {
		return illuminaSNP;
	}
	
	public String formattedRecord() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(null != dbSnpID ? dbSnpID : "");
		sb.append("\t");
		sb.append(DEFAULT_CHAR != dbSnpStrand ? dbSnpStrand : "");
		sb.append("\t");
		sb.append(null != dbSnpRef_Alt ? dbSnpRef_Alt : "");
		sb.append("\t");
		sb.append(illGCScore != 0.0f ? df.format(illGCScore) : "");
		sb.append("\t");
		sb.append(DEFAULT_CHAR != illAllele1 ? illAllele1 : "");
		sb.append("\t");
		sb.append(DEFAULT_CHAR != illAllele2 ? illAllele2 : "");
		sb.append("\t");
		sb.append(null != illuminaRef ? (illTypeHom ? "hom" : "het") : "");
		sb.append("\t");
		sb.append(null != illuminaRef ? illuminaRef : "");
		sb.append("\t");
//		sb.append(null != illuminaAlt ? illuminaAlt : "");
//		sb.append("\t");
//		sb.append(null != illuminaSNP ? illuminaSNP : "");
//		sb.append("\t");
		sb.append(DEFAULT_CHAR != gffRef ? gffRef : "");
		sb.append("\t");
		sb.append(null != gffAlt ? gffAlt : "");
		sb.append("\t");
		sb.append(DEFAULT_CHAR != gffGenotype ? gffGenotype : "");
		sb.append("\t");
		sb.append(DEFAULT_CHAR != vcfRef ? vcfRef : "");
		sb.append("\t");
		sb.append(DEFAULT_CHAR != vcfAlt ? vcfAlt: "");
		sb.append("\t");
		sb.append(null != vcfGenotype ? vcfGenotype: "");
		sb.append("\t");
		sb.append(null != pileup ? pileup: "");
		sb.append("\t");
		sb.append(null != positionMatch ? positionMatch: "");
		sb.append("\t");
		sb.append(null != genotypeMatch ? genotypeMatch: "");
		sb.append("\n");
		
		return sb.toString();
	}
	public float getIllGCScore() {
		return illGCScore;
	}
	public void setIllGCScore(float illGCScore) {
		this.illGCScore = illGCScore;
	}
	public char getIllAllele1() {
		return illAllele1;
	}
	public void setIllAllele1(char illAllele1) {
		this.illAllele1 = illAllele1;
	}
	public char getIllAllele2() {
		return illAllele2;
	}
	public void setIllAllele2(char illAllele2) {
		this.illAllele2 = illAllele2;
	}
	public boolean isIllTypeHom() {
		return illTypeHom;
	}
	public void setIllTypeHom(boolean illTypeHom) {
		this.illTypeHom = illTypeHom;
	}
	public char getDbSnpStrand() {
		return dbSnpStrand;
	}
	public void setDbSnpStrand(char dbSnpStrand) {
		this.dbSnpStrand = dbSnpStrand;
	}
	public String getDbSnpRef_Alt() {
		return dbSnpRef_Alt;
	}
	public void setDbSnpRef_Alt(String dbSnpRefAlt) {
		dbSnpRef_Alt = dbSnpRefAlt;
	}
	public void setPileup(String pileup) {
		this.pileup = pileup;
	}
	public String getPileup(String pileup) {
		return pileup;
	}
	public String getPositionMatch() {
		return positionMatch;
	}
	public void setPositionMatch(String positionMatch) {
		this.positionMatch = positionMatch;
	}
	public String getGenotypeMatch() {
		return genotypeMatch;
	}
	public void setGenotypeMatch(String genotypeMatch) {
		this.genotypeMatch = genotypeMatch;
	}
	
}
