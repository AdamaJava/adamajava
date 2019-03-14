package org.qcmg.qprofiler2.util;


import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.qcmg.qprofiler2.summarise.CycleSummaryTest;
import org.qcmg.qprofiler2.util.CycleSummaryUtils;

public class CycleSummaryUtilsMDTest {
	
	@Test
	public void getBigMDCycleNoTest() throws IOException{
		String fname = "input.bam";
		CycleSummaryTest.createInputFile(fname);
		@SuppressWarnings("unchecked")
		final CycleSummary<Character>[] tagMDMismatchByCycle = new CycleSummary[]{ new CycleSummary<Character>(Character.MAX_VALUE, 512), new CycleSummary<Character>(Character.MAX_VALUE, 512)};	
		final QCMGAtomicLongArray[] allReadsLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024)};
		
		
		SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(fname));		
		int count = 0, firstNo = 0, secondNo = 0;
		for (SAMRecord record : reader) {
			String value = (String) record.getAttribute("MD");
			int order = (record.getFirstOfPairFlag())? 0 : 1;	//0: firstOfPair , 1: secondOfPair
			if(order == 0) firstNo ++;
			else secondNo ++;
			
			//this counts will be used to caculate % for MD
			for( int i = 1; i <= record.getReadLength(); i ++ ) allReadsLineLengths[order].increment(i);
			
			String err = CycleSummaryUtils.tallyMDMismatches(value, record.getCigar(), tagMDMismatchByCycle[order], record.getReadBases()  , record.getReadNegativeStrandFlag(), null, null);
			assertEquals(err , null);
			count ++;			
		}
		new File(fname).delete();
		assertEquals(firstNo , 2);
		assertEquals(secondNo , 2);
		assertEquals(count , 4);
		
		int bigMDno = CycleSummaryUtils.getBigMDCycleNo(tagMDMismatchByCycle, (float) 0.2, allReadsLineLengths);
		assertEquals(bigMDno, 4);
		bigMDno = CycleSummaryUtils.getBigMDCycleNo(tagMDMismatchByCycle, (float) 0.01, allReadsLineLengths);
		assertEquals(bigMDno, 4);		
		bigMDno = CycleSummaryUtils.getBigMDCycleNo(tagMDMismatchByCycle, (float) 0.3, allReadsLineLengths);
		assertEquals(bigMDno, 0);
	}
		

	/**
	 * Testing below tasks:
	 * 1. hardclip are ignored
	 * 2. softclip are added
	 * 3. insertion base will be skipped, won't count to mismatch
	 */
	@Test 	
	public void testTallyMDMismatches() {
		CycleSummary<Character> summary = new CycleSummary<Character>(Character.MAX_VALUE, 64);
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		
		//no mismatch  arrays should be empty
		String readBasesString = "ACAGGGATTTCGCCATGTTGGCCAGGTTGGAGATTTTATTTTTCTTAAGTCTCACTCTGTCCAGCTGGAGTGCAGCAGTGTGATCTGGGTGACTGTAGCC";
		byte[] readBases = readBasesString.getBytes();
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(3, CigarOperator.H));
		cigar.add(new CigarElement(3, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(96, CigarOperator.M));
		CycleSummaryUtils.tallyMDMismatches("99", cigar, summary, readBases, false, forwardArray, reverseArray);				
		for (int i = 0 ; i < forwardArray.length() ; i++)  
			assertEquals(0, forwardArray.get(i));	
				
		//forward with two mismatch and softclip, insertion		
		cigar = new Cigar(); //cigar 20H20S25M2I51M
		cigar.add(new CigarElement(20, CigarOperator.H));
		cigar.add(new CigarElement(20, CigarOperator.S));
		cigar.add(new CigarElement(25, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.I));
		cigar.add(new CigarElement(51, CigarOperator.M));
		//20S + 25M + 2I + 51M
		assertEquals(98,cigar.getReadLength());
		//base seq checked
		StringBuilder sb = new StringBuilder("ACCTA ACATC AGCAG TGCTT".replace(" ", "")); //20S
		sb.append("TCAAT TACGT TCCTT GAACA CTGTG".replace(" ", "")); //25M (25th mismatch C->G)
		sb.append("TC"); //2I
		sb.append("TTATG TCTTA TGTTA TGTCA TATAT TCATT ACATA TATAT ATTAC ATTACA".replace(" ", "")); //(51M) (31th mismatch C->A)
		readBases = sb.toString().getBytes();
		assertEquals(98,readBases.length);
		
		for(int i = 0; i < 10; i ++)
			CycleSummaryUtils.tallyMDMismatches("24C30C20", cigar, summary, readBases, false, forwardArray, reverseArray);		
		// would expect C>A on the forward as the mismatch happens before the insertion
		for (int i = 0 ; i < forwardArray.length() ; i++) 
			if (i == CycleSummaryUtils.getIntFromChars('C', 'G') || i == CycleSummaryUtils.getIntFromChars('C', 'A'))  
				assertEquals(10, forwardArray.get(i));
			else  
				assertEquals(0, forwardArray.get(i));
		 		
		for (int i = 0 ; i < reverseArray.length() ; i++)  assertEquals(0, reverseArray.get(i));
		//check MD cycle
		for( Integer cycle : summary.cycles() )
			for( Character value : summary.getPossibleValues() ){
				if ((cycle == 45 && value == 'G') || (cycle == 78 && value == 'A')) //cycle should includes soft and insertion
					assertEquals(10, summary.count(cycle, value));
				else
					assertEquals(0, summary.count(cycle, value));
			}

		//reverse string
		for(int i = 0; i < 5; i ++)
		CycleSummaryUtils.tallyMDMismatches("24C30C20", cigar, summary, readBases, true, forwardArray, reverseArray);
		for (int i = 0 ; i < reverseArray.length() ; i++)  
			if (i == CycleSummaryUtils.getIntFromChars('G', 'C') || i == CycleSummaryUtils.getIntFromChars('G', 'T')	) 
				assertEquals(5, reverseArray.get(i));
			else  
				assertEquals(0, reverseArray.get(i));
				
 		//check MD cycle
		for( Integer cycle : summary.cycles() )
			for( Character value : summary.getPossibleValues() ){
				if ((cycle == 45 && value == 'G') || (cycle == 78 && value == 'A')) //cycle should includes soft and insertion
					assertEquals(10, summary.count(cycle, value)); //forward
				else if((cycle == 21 && value == 'T') || (cycle == 54 && value == 'C')) //reversed base and position
					assertEquals(5, summary.count(cycle, value)) ; //reverse		
			}		
	}
	

	@Test
	public void TallyMDMismatchesInvalidMD() {		
		CycleSummary<Character> summary = new CycleSummary<Character>(Character.MAX_VALUE, 64);
		final String readBasesString = "AAAAAAAAAACCCCCCCCCCGGGGGGGGGGTTTTTTTTTTAAAAAAAAAAT";
		final byte[] readBases = readBasesString.getBytes();
		
		//invalid cigar
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(30, CigarOperator.M));					 
		String errMess = CycleSummaryUtils.tallyMDMismatches("52", cigar, summary, readBases, true, null, null);
		assertTrue(errMess != null);
		assertTrue(summary.cycles().size() == 0);
		assertTrue(summary.getPossibleValues().size() == 0);
		
		//valid cigar
		cigar = new Cigar();
		cigar.add(new CigarElement(51, CigarOperator.M));	
		errMess = CycleSummaryUtils.tallyMDMismatches("52", cigar, summary, readBases, true, null, null);
		assertTrue(errMess == null);	
		
		
		// extra long mds with big deletion  
		// cigar didn't contain deletion but md contains. At moment our qprofiler won't report error
		String mdString = "30^AGAAAATGTTTTTCATTTTCTTGATTTATTTCTGAATTCAGCTTGCTCTTCATTAGCGCTACATAGCTGMCTTATTATTCGTGGTC" +
		"CCCTATGACCCCCTGATCATTTTCCCTGAGGGTGCATATTTATTCACTAACTATGTTACAATCATGTGATCTGCTGGATTTTTTCTGATA" +
		"GTCTACTCTAGATTTGTTCTAAATTAATAAATCCCATTATTTTTGGCTTCTACTACTTCTATTTATTAAATTCATTCTGAATATGAAGTTTATTT" +
		"TCAAAGGAATTCATAATTCTTTACTCCRRGCTTGGTTCTAACAATGAATTTAATAAGAATTGTATTTAATCAATGTTTAAATATATTAAGGGC" +
		"AAATTTTGTAAAAATGTTAGTGTTCCAAGCTTTCCATTTCCCCACAAATTAATTTTTTTAGCCTTTCCCCTTAATCCACTTTCTT19G0";		
		errMess = CycleSummaryUtils.tallyMDMismatches(mdString, cigar, summary, readBases, false, null, null);
		assertTrue(errMess == null);		
		assertEquals(1, summary.count(50, 'A')); //forward
		
		
		//extra long md and invalid md (md baselength is bigger than read base)
		summary = new CycleSummary<Character>(Character.MAX_VALUE, 64);
		mdString = mdString.replace("19G", "29G");
		errMess = CycleSummaryUtils.tallyMDMismatches(mdString, cigar, summary, readBases, true, null, null);	
		System.out.println(errMess);
		assertTrue(errMess != null);
		assertTrue(summary.cycles().size() == 0);
		assertTrue(summary.getPossibleValues().size() == 0);
		
	}
	
	@Test
	public void tallyMDMismatchesDeletion() {
		//md: 92^AA2G2T1 , cigar: 92M2D1M1I6M, 
		//seq: GGATAGCTGTATACCCTTCAGGTCTTTTCCCCAAATACGATTGCCTAAAACAAAACATTATTAAAAGTTGTTCAAGGTCATGATCCTCCAACCTGTCTCT, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		CycleSummary<Character> summary = new CycleSummary<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(92, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.D));
		cigar.add(new CigarElement(1, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(6, CigarOperator.M));
		CycleSummaryUtils.tallyMDMismatches("92^AA2G2T1", cigar, summary, "GGATAGCTGTATACCCTTCAGGTCTTTTCCCCAAATACGATTGCCTAAAACAAAACATTATTAAAAGTTGTTCAAGGTCATGATCCTCCAACCTGTCTCT".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see 1 C>T and 1 G>T		
		assertEquals(1, forwardArray.get(CycleSummaryUtils.getIntFromChars('T', 'C')));
		assertEquals(1, forwardArray.get(CycleSummaryUtils.getIntFromChars('G', 'T')));
	}
	
	@Ignore
	public void tallyMDMismatchesInsertion() {
		//HWI-ST1445:86:C4CKMACXX:2:2308:11384:83325       163     chr1    450820  0       98I3M   =       450820  129     GTCTTTTTTTTTTTTTTTTTTTTTTTAAAAGGGGGGGGGCGGGGGGGCCCCCCCCTGTAACCCCAGCAATTTGGGGGACTGGGGGGGGGGGGTCTCTTGGG   BBBFFFFFFFFFFIIIIIFFFFFFB0<BBB#######################################################################   XA:i:2  MD:Z:0G0T0T2A1A1A0A0A0A0A0A0A0A0A0A0A0A1A0A0A0A0A0A0A3A0A0A0A0A0A0A0A0A0A0A1A1A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A23A1A0   NM:i:67 ZW:f:0.0
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		CycleSummary<Character> summary = new CycleSummary<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(98, CigarOperator.I));
		cigar.add(new CigarElement(3, CigarOperator.M));
		CycleSummaryUtils.tallyMDMismatches("0G0T0T2A1A1A0A0A0A0A0A0A0A0A0A0A0A1A0A0A0A0A0A0A3A0A0A0A0A0A0A0A0A0A0A1A1A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A0A23A1A0", cigar, summary, "GTCTTTTTTTTTTTTTTTTTTTTTTTAAAAGGGGGGGGGCGGGGGGGCCCCCCCCTGTAACCCCAGCAATTTGGGGGACTGGGGGGGGGGGGTCTCTTGGG".getBytes(), false, forwardArray, reverseArray);
		
		
	}
	
	@Test
	public void tallyMDMismatchesDeletion2() {
		//md: 94^AG2G1G0 , cigar: 94M2D1M1I4M, 
		//seq: TCTCACATGAGAGTAACTAGCATCTTTCTCTCAGATGATGAAGATGATGAAGAGGAAGATGAAGAGGAAGAAATCGACGTGGTCACTGTGGAGACTGTCT, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		CycleSummary<Character> summary = new CycleSummary<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(94, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.D));
		cigar.add(new CigarElement(1, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(4, CigarOperator.M));
		CycleSummaryUtils.tallyMDMismatches("94^AG2G1G0", cigar, summary, "TCTCACATGAGAGTAACTAGCATCTTTCTCTCAGATGATGAAGATGATGAAGAGGAAGATGAAGAGGAAGAAATCGACGTGGTCACTGTGGAGACTGTCT".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see 2 G>T		
		assertEquals(2, forwardArray.get(CycleSummaryUtils.getIntFromChars('G', 'T')));
	}
	
	@Test
	public void tallyMDMismatchesNastyCigar() {
		//md: 16A1T6A2^G5A6T1A0G2C4T0G2^A3^GA5T2T3T8 , cigar: 28M1D4M1I23M1D3M2D3M2I18M18S, 
		//seq: AGTCTAGAGT CCAAAAGGAA TTCTTCCTCC TG*C*CTTTTCAT CCCTTTTTTT CACATCTTTC A*CC*TCCGCCGGG CCAATTTCT>TCAGTTCT CGTTTTAAGC, reverse strand: false
		QCMGAtomicLongArray forwardArray = new QCMGAtomicLongArray(32);
		QCMGAtomicLongArray reverseArray = new QCMGAtomicLongArray(32);
		CycleSummary<Character> summary = new CycleSummary<Character>(Character.MAX_VALUE, 64);
		Cigar cigar = new Cigar();
		cigar.add(new CigarElement(28, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.D));
		cigar.add(new CigarElement(4, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.I));
		cigar.add(new CigarElement(23, CigarOperator.M));
		cigar.add(new CigarElement(1, CigarOperator.D));
		cigar.add(new CigarElement(3, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.D));
		cigar.add(new CigarElement(3, CigarOperator.M));
		cigar.add(new CigarElement(2, CigarOperator.I));
		cigar.add(new CigarElement(18, CigarOperator.M));
		cigar.add(new CigarElement(18, CigarOperator.S));
		
		CycleSummaryUtils.tallyMDMismatches("16A1T6A2^G5A6T1A0G2C4T0G2^A3^GA5T2T3T8", cigar, summary, "AGTCTAGAGTCCAAAAGGAATTCTTCCTCCTGCCTTTTCATCCCTTTTTTTCACATCTTTCACCTCCGCCGGGCCAATTTCTTCAGTTCTCGTTTTAAGC".getBytes(), false, forwardArray, reverseArray);
		
		//expecting to see A>G, T>A, 2x A>C,...... 	
		assertEquals(1, forwardArray.get(CycleSummaryUtils.getIntFromChars('A', 'G')));
		assertEquals(2, forwardArray.get(CycleSummaryUtils.getIntFromChars('A', 'C')));
		assertEquals(1, forwardArray.get(CycleSummaryUtils.getIntFromChars('A', 'T')));
		assertEquals(1, forwardArray.get(CycleSummaryUtils.getIntFromChars('C', 'T')));
		assertEquals(1, forwardArray.get(CycleSummaryUtils.getIntFromChars('G', 'T')));
		assertEquals(1, forwardArray.get(CycleSummaryUtils.getIntFromChars('G', 'C')));
		assertEquals(4, forwardArray.get(CycleSummaryUtils.getIntFromChars('T', 'C')));
		assertEquals(2, forwardArray.get(CycleSummaryUtils.getIntFromChars('T', 'A')));
	}
	

	
}
