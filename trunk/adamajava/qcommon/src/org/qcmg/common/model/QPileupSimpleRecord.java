package org.qcmg.common.model;

import java.util.concurrent.atomic.AtomicInteger;

public class QPileupSimpleRecord {

	private char ref;
	private AtomicInteger countA;
	private AtomicInteger countT;
	private AtomicInteger countC;
	private AtomicInteger countG;
	private AtomicInteger countN;
	private AtomicInteger countDot;
	
	
	public char getRef() {
		return ref;
	}
	public void setRef(char ref) {
		this.ref = ref;
	}
	public AtomicInteger getCountA() {
		if (null == countA)
			countA = new AtomicInteger();
		return countA;
	}
	public void setCountA(AtomicInteger countA) {
		this.countA = countA;
	}
	public AtomicInteger getCountT() {
		if (null == countT)
			countT = new AtomicInteger();
		return countT;
	}
	public void setCountT(AtomicInteger countT) {
		this.countT = countT;
	}
	public AtomicInteger getCountC() {
		if (null == countC)
			countC = new AtomicInteger();
		return countC;
	}
	public void setCountC(AtomicInteger countC) {
		this.countC = countC;
	}
	public AtomicInteger getCountG() {
		if (null == countG)
			countG = new AtomicInteger();
		return countG;
	}
	public void setCountG(AtomicInteger countG) {
		this.countG = countG;
	}
	public AtomicInteger getCountN() {
		if (null == countN)
			countN = new AtomicInteger();
		return countN;
	}
	public void setCountN(AtomicInteger countN) {
		this.countN = countN;
	}
	public AtomicInteger getCountDot() {
		if (null == countDot)
			countDot = new AtomicInteger();
		return countDot;
	}
	public void setCountDot(AtomicInteger countDot) {
		this.countDot = countDot;
	}
	
	public void incrementBase(byte b) {
		switch ((char)b) {
		case 'A': getCountA().incrementAndGet(); break;
		case 'T': getCountT().incrementAndGet(); break;
		case 'C': getCountC().incrementAndGet(); break;
		case 'G': getCountG().incrementAndGet(); break;
		case 'N': getCountN().incrementAndGet(); break;
		case '.': getCountDot().incrementAndGet(); break;
		default:
		throw new IllegalArgumentException("Invalide base supplied: " + b);
		}
	}
	
	public String getFormattedString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getBaseCount(countA, 'A'));
		sb.append(getBaseCount(countT, 'T'));
		sb.append(getBaseCount(countC, 'C'));
		sb.append(getBaseCount(countG, 'G'));
		sb.append(getBaseCount(countN, 'N'));
		sb.append(getBaseCount(countDot, '.'));
		sb.append("\n");
		
		return sb.toString();
	}
	
	private String getBaseCount(AtomicInteger ai, char c) {
		if (null == ai) return "";
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0, len = ai.get() ; i < len ; i++) {
			sb.append(c);
		}
		return sb.toString();
	}
	
}
