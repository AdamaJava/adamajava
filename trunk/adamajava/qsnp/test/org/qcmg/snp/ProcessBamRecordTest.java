package org.qcmg.snp;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMRecord;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.model.Accumulator;
import org.qcmg.picard.SAMRecordFilterWrapper;

public class ProcessBamRecordTest {
	
	private final  ConcurrentMap<Integer, Accumulator> map = new ConcurrentHashMap<Integer, Accumulator>();
	
	@Test
	public void testRealLifeData() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(750778);
		sam.setCigarString("101M");
		sam.setReadString("ACTCGGGAGGCTGAGGGAGGAGAATTGCTTGAACCCGGGAGGTGGAGGTTGCAGTGAGCCGAGATGGCGCCACTGCACTCCAGCCTGGGCGATAGAGCGAG");
		sam.setBaseQualityString("8BB@DDDDDDCDDDDDDDDDEDDDDDDDECB?9DDDDDBBEFHHGHIHD>HHH@JIJJJJIJJJJJIGGGGGHIJHGIIIIJHEHGJJGHHHHFFFFFCCC");
		processSAMRecord(getSamWrapper(sam));
		assertEquals(101, map.size());
		assertEquals('A', map.get(750778).getBase());
		assertEquals('G', map.get(750878).getBase());
		
		map.clear();
		
		sam = new SAMRecord(null);
		sam.setAlignmentStart(750973);
		sam.setCigarString("12M1D11M1D13M3I62M");
		String readBases = "TGCCCTCCAGCCGGGGGAAGGAGGAGAGGCCCCCTCAAAAAAAAAAAAAAAAAAAAAGCAGGCTGTAAGGGAAGTCTCTTCTAACACTGCCCTGGAGCTAG";
		sam.setReadString(readBases);
		sam.setBaseQualityString("#######################################BBDBBDDDFHHHIJHHCHBDGHGGGGFEHFHGIGHGBGEGGIDIHEGGEF??DADFFFF@@@");
		
		processSAMRecord(getSamWrapper(sam));
		final Cigar cigar = sam.getCigar();
		assertEquals(readBases.length(), cigar.getReadLength());	//101
		assertEquals(100, cigar.getReferenceLength());	// we have a 3base insertion & 2x 1 base deletions
		assertEquals(98, map.size());					// 101 - 3 base insertion
		// 12M
		for (int i = 750973 ; i < 750973 + 12 ; i++) {
			Assert.assertNotNull(map.get(i));
		}
		// 1D
		Assert.assertNull(map.get(750973 + 12));
		// 11M
		for (int i = 750973 + 12 + 1 ; i < 750973 + 12 +1 + 11; i++) {
			Assert.assertNotNull(map.get(i));
		}
		// 1D
		Assert.assertNull(map.get(750973 +  12 +1 + 11));
		// 13M
		for (int i = 750973 + 12 + 1 + 11 + 1 ; i < 750973 + 12 +1 + 11 + 1 + 13; i++) {
			Assert.assertNotNull(map.get(i));
		}
		//3I
		// can't really check this
		// 62M
		for (int i = 750973 + 12 + 1 + 11 + 1 + 13; i < 750973 + 12 +1 + 11 + 1 + 13 + 62; i++) {
			Assert.assertNotNull(map.get(i));
		}
		// null for entries above this point
		Assert.assertNull(map.get(750973 + 12 +1 + 11 + 1 + 13 + 62));
		
	}
	
	private static SAMRecordFilterWrapper getSamWrapper(SAMRecord record) {
		SAMRecordFilterWrapper wrapper = new SAMRecordFilterWrapper(record, 1);
		wrapper.setPassesFilter(true);
		return wrapper;
	}
	
	@Test
	public void testRealLifeData2() {
		SAMRecord sam = new SAMRecord(null);
		sam.setAlignmentStart(750985);
		sam.setCigarString("44M1D16M1D12M1I24M4S");
		String readBases = "TGGGCGATAGAGCGAGACTCCGTCTCAAAAAAAAAAAAAAAAAAAAGGCTGTAAGGGAAGCTTTTTTTAAACTTGCCCTGGAGCTGGGCCTTTTCCTCACC";
		sam.setReadString(readBases);
		sam.setBaseQualityString("@@7?DDDDHDFH?E@@?@BHEIDEHHJDCEGEGGEEFDD@BD###########################################################");
		
		map.clear();
		processSAMRecord(getSamWrapper(sam));
		assertEquals(readBases.length() - 1 -4, map.size());	// 1I + 4S
		
		for (int i = 0 ; i < readBases.length() - 1 ; i++) {
			if (i < 44) {
				Assert.assertNotNull(map.get(i + 750985));
				if (i > 26)
					Assert.assertEquals('A', map.get(i + 750985).getBase());
			}
			if (i == 44)
				Assert.assertNull(map.get(i + 750985));
			if (i > 44 && i < 61)
				Assert.assertNotNull(map.get(i + 750985));
			if (i == 61)
				Assert.assertNull(map.get(i + 750985));
			if (i > 62 && i < 74)
				Assert.assertNotNull(map.get(i + 750985));
			if (i > 73 && i < 98)
				Assert.assertNotNull(map.get(i + 750985));
		}
		// start
		assertEquals('T', map.get(750985).getBase());
		assertEquals('G', map.get(750986).getBase());
		assertEquals('G', map.get(750987).getBase());
		assertEquals('G', map.get(750988).getBase());
		// end
		assertEquals('C', map.get(750985 + 95).getBase());
		assertEquals('C', map.get(750985 + 96).getBase());
		assertEquals('T', map.get(750985 + 97).getBase());
		Assert.assertNull(map.get(750985 + 98));
	}
	
	private void processSAMRecord(final SAMRecordFilterWrapper record) {
		final SAMRecord sam = record.getRecord();
		final int startPosition = sam.getAlignmentStart();
		final int endPosition = sam.getAlignmentEnd();
		final byte[] bases = sam.getReadBases();
		final byte[] qualities = sam.getBaseQualities();
		final Cigar cigar = sam.getCigar();
		final boolean forwardStrand = ! sam.getReadNegativeStrandFlag();
		
		int referenceOffset = 0, offset = 0;
		
		for (CigarElement ce : cigar.getCigarElements()) {
			CigarOperator co = ce.getOperator();
			int length = ce.getLength();

			if (co.consumesReferenceBases() && co.consumesReadBases()) {
				// we have a number (length) of bases that can be advanced.
//				if (debugOn) {
//				}
//				if ( 169798 == startPosition || 169809 == startPosition) {
//					logger.info("about to call updateListWithAccums(startPosition: " + startPosition +", bases: " + bases + 
//						", qualities: " + qualities + ", forwardStrand: " + forwardStrand + ", offset: "
//						+ offset + ",cigar: " + cigar.toString() + ", length: " + length + ", referenceOffset: " + referenceOffset + ", isNormal: " + isNormal + ")");
//				}
				updateMapWithAccums(startPosition, bases,
						qualities, forwardStrand, offset, length, referenceOffset, 
						record.getPassesFilter(), endPosition);
				// advance offsets
				referenceOffset += length;
				offset += length;
			} else if (co.consumesReferenceBases()) {
				// DELETION
				referenceOffset += length;
			} else if (co.consumesReadBases()){
				// INSERTION, SOFT CLIPPING
				offset += length;
			}
		}
	}
	
	public void updateMapWithAccums(int startPosition, final byte[] bases, final byte[] qualities,
			boolean forwardStrand, int offset, int length, int referenceOffset, final boolean passesFilter, int endPosition) {
		
		for (int i = 0 ; i < length ; i++) {
//			for (int i = offset ; i < length + offset; i++) {
//			if ((startPosition + i + referenceOffset) > 169874)
//				logger.warn("gone past reference index, startPosition: " + startPosition + ", referenceOffset: " + referenceOffset + ", i: " + i);
			Accumulator acc = map.get(startPosition + i + referenceOffset);
			if (null == acc) {
				acc = new Accumulator(startPosition + i + referenceOffset);
				map.put(startPosition + i + referenceOffset, acc);
//				Accumulator oldAcc = array.putIfAbsent(startPosition + i + referenceOffset, acc);
//				if (null != oldAcc) acc = oldAcc;
			}
			if (passesFilter)
				acc.addBase(bases[i + offset], qualities[i + offset], forwardStrand, startPosition, startPosition + i + referenceOffset, endPosition);
			else
				acc.addUnfilteredBase(bases[i + offset]);
		}
	}

}
