/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qsv.assemble;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Alignment {

	private final List<ReadMatch> matchedReads;
	private final Read seed;
	
	private int start;
	private int length;
	
	public Alignment(Read seed) {
		this.seed = seed;
		matchedReads = new ArrayList<ReadMatch>();
	}
	
	public Alignment(ConcurrentHashMap<Integer, ReadMatch> reads, Read seed)
	{
		this.seed = seed;
		matchedReads = new ArrayList<ReadMatch>(reads.values());
	}
	
	/*
	 * calculates the length of the potential contig. It does this by determining the smallest
	 * position in all the reads, this is used as the start. And by determining the longest length
	 * of all the reads. This is the stop.
	 */
	public int calculateLength ()
	{
		start = 0;
		length = seed.length();
		
		for (ReadMatch nextMatch : matchedReads) {
			
			length = Math.max(length, nextMatch.read().length() + nextMatch.matchedPos());
			start = Math.min(start, nextMatch.matchedPos());
			
		}
		return length - start;
	}
	public char[] getBasesOJH(int position)
	{
		char[] bases = new char[matchedReads.size()];
		for (int i = 0 , len = matchedReads.size() ; i < len ; i++) {
			ReadMatch nextMatch = matchedReads.get(i);
			Read read = nextMatch.read();
			int pos = nextMatch.matchedPos();
			if (read.positionWithin(position, pos)) {
				bases[i] = read.charAt(position, pos);
			}
		}
		return bases;
	}
	
	/*
	 * Determines the consensus base for a given position. It does this by first
	 * getting the bases for that given position in every read. After that it then
	 * counts how many of each base there is. It than determines which base is most
	 * frequent. It it can't determine which base is most frequent because there is a
	 * tie between two or more bases or there isn't at least MINIMUM_READ_EXTEND number
	 * of reads to validate that base then it tries to return the original contig base
	 * first and if that fails it return an N.
	 */
	private char getConsensusBase(int position)
	{	
		char[] bases = getBasesOJH(position);
		int a = 0;
		int t = 0;
		int c = 0;
		int g = 0;
		for (int i = 0 , len = bases.length ; i < len ; i++) {
			switch (bases[i]) {
			case 'A': 
				a++;
				break;
			case 'T':
				t++;
				break;
			case 'C':
				c++;
				break;
			case 'G':
				g++;
				break;
			}
		}
		
		if(a > t && a > c && a > g && a >= QSVAssemble.MINIMUM_READ_EXTEND) { //Is A the most common?
			return 'A';
		} else if (t > a && t > c && t > g && t >= QSVAssemble.MINIMUM_READ_EXTEND) { //Is T the most common?
			return 'T';
		} else if (c > a && c > t && c > g && c >= QSVAssemble.MINIMUM_READ_EXTEND) { //Is C the most common?
			return 'C';
		} else if (g > a && g > t && g > c && g >= QSVAssemble.MINIMUM_READ_EXTEND) { //Is G the most common?
			return 'G';
		} else { //None of the bases were the most common and was over Assembler.MINIMUM_READ_EXTEND. Ties go here as well	
			if (seed.positionWithin(position, 0)) { //Return the seed base if there is no consensus.
				return seed.charAt(position, 0);
			} else {
				return 'N';
			}
		}
	}
	
	/*
	 * Constructs the consensus sequence out of the matching reads. It does this by going from
	 * the beginning of the potential contig to the end and adding the consensus base at that position.
	 * If it can't determine the consensus base at that position than it stops extending the contig
	 * in that direction.
	 */
	public Read constructConsensusSequence () throws Exception
	{		
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < length; i++) {
			char cbase = getConsensusBase(i);
			if (cbase == 'N') {
				if (i >= seed.length()) { //Reached a base that the program couldn't decided which one it was.	Either there was less than Assembler.MINIMUM_READ_EXTEND reads or there was no base that had the majority			

					break;
				} else if (i < 0) {
					sb = new StringBuilder();
					continue;
				} else {
					//This only occurs if there is a N present in the original seed read AND there is no overlap with other reads at that position
				}
			}
			sb.append(cbase);
		}
		
		if (matchedReads.size() >= QSVAssemble.MINIMUM_READ_EXTEND) {
		
			Read newContig = new Read(">" + seed.getHeader(), sb.toString());
			newContig.setHeader(seed.getHeader());
			
			return newContig;
		} else {
			return null;
		}
	}
	
	public List<ReadMatch> getMatchingReads() {
		return matchedReads;
	}


	public Read getSeed() {
		return seed;
	}
	
}
