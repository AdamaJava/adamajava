package org.qcmg.qprofiler2.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import org.qcmg.qprofiler2.util.FlagUtil;

public class FlagUtilTest {
	
	@Test
	public void getValidFlags() {
		assertEquals("000000000000", FlagUtil.getFlagString(0));
		assertEquals("000000000001, p", FlagUtil.getFlagString(1));
		assertEquals("000000000010", FlagUtil.getFlagString(2));			// INVALID
		assertEquals("000000000011, pP", FlagUtil.getFlagString(3));		// paired and in proper pair
		assertEquals("000000000100, u", FlagUtil.getFlagString(4));		//UNMAPPED
		assertEquals("000000000101, pu", FlagUtil.getFlagString(5));		//paired and UNMAPPED
		assertEquals("000000000110, u", FlagUtil.getFlagString(6));			//UNMAPPED
		assertEquals("000000000111, pPu", FlagUtil.getFlagString(7));			//UNMAPPED
		assertEquals("000000001000", FlagUtil.getFlagString(8));			//INVALID
	}
	
	
	/*
	 * Bit Description
0x1 template having multiple segments in sequencing
0x2 each segment properly aligned according to the aligner
0x4 segment unmapped
0x8 next segment in the template unmapped
0x10 SEQ being reverse complemented
0x20 SEQ of the next segment in the template being reversed
0x40 the first segment in the template
0x80 the last segment in the template
0x100 secondary alignment
0x200 not passing quality controls
0x400 PCR or optical duplicate
0x800 supplementary alignment
	 */
	
	
	@Test
	public void readPairedFlag() {
		
		for (int i = 1 ; i < 5000 ; i++) {
			
//			System.out.println("i: " + i + " : " + (i & 0x1) + " : " + (i & 0x2));
			
			if ((i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("1, p"));
			}
			if ((i & 0x2) == 2 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("1, pP"));
			}
			if ((i & 0x4) == 4) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("u"));
			}
			if ((i & 0x8) == 8 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("U"));
			}
			if ((i & 0x10) == 16) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("r"));
			}
			if ((i & 0x20) == 32 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("R"));
			}
			if ((i & 0x40) == 64 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).matches(".*1, p.*1.*"));
			}
			if ((i & 0x80) == 128 && (i & 0x1) == 1) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("2"));
			}
			if ((i & 0x100) == 256) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("s"));
			}
			if ((i & 0x200) == 512) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("f"));
			}
			if ((i & 0x400) == 1024) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("d"));
			}
			if ((i & 0x800) == 2048) {
				assertEquals(true, FlagUtil.getFlagString(i).contains("S"));
			}
			
		}
	}
	
//	public enum TRANS{
//		AG(0), GA(1), CT(2), TC(3),			
//		AC(4), CA(5),AT(6), TA(7),CG(8), GC(9), GT(10), TG(11), 
//		NA(12),AN(13), NG(14), GN(15),NT(16),TN(17),NC(18),CN(19), Others(20); 
//		public final int order;
//		 
//		TRANS(int od){
//			this.order = od;  			 
//		}
//		
//		public int toCharCode(){
//			
//			int ascRef =  (int) name().charAt(0) ;
//			int ascii = ascRef *2 + (int) name().charAt(1);	
//			
//			System.out.println(  "case " + ascii + " : return " + name() + ";");
//			
//			return ascii;
//		}
//	}		
	
	
			
//	@Test
//	public void xuTest() {
//		
//		int[] inArray = new int[21];;
//		for(TRANS tr :  TRANS.values())
//			inArray[tr.order] =  tr.toCharCode();		
//		
//		Arrays.sort(inArray);
//		for (int i = 0; i < inArray.length; i ++) {
//			 System.out.println(i + "th = " + inArray[i]);
//		}
//	}
	

}
