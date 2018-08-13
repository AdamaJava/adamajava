package au.edu.qimr.qannotate.modes;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;
import org.qcmg.common.vcf.VcfFileMeta;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class CCMModeTest {
	
	public static final String REF = "0/0";
	public static final String DOT = "./.";
	
	
	
	
	@Test
	public void singleCallerMultiSample() {
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T",".",".","FLANK=ACACATACATA","GT:GD:AC:MR:NNS:FT","0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","0/0:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:."});
		 VcfFileMeta meta = new VcfFileMeta(createTwoSampleVcf());
		 assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
		 CCMMode m = new CCMMode();
		m.updateVcfRecordWithCCM(vcf1, meta.getCallerSamplePositions());
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(vcf1.getFormatFields());
		assertArrayEquals(new String[] {"22","22"}, ffMap.get(VcfHeaderUtils.FORMAT_CCM));
		assertArrayEquals(new String[] {"Germline","ReferenceNoVariant"}, ffMap.get(VcfHeaderUtils.FORMAT_CCC));
		
		vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T,G",".",".","FLANK=ACACATACATA","GT:GD:AC:MR:NNS:FT","1/2:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","1/2:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:."});
		m.updateVcfRecordWithCCM(vcf1, meta.getCallerSamplePositions());
		ffMap = VcfUtils.getFormatFieldsAsMap(vcf1.getFormatFields());
		assertArrayEquals(new String[] {"47","47"}, ffMap.get(VcfHeaderUtils.FORMAT_CCM));
		assertArrayEquals(new String[] {"DoubleGermline","Germline"}, ffMap.get(VcfHeaderUtils.FORMAT_CCC));
		
		vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T,G",".",".","FLANK=ACACATACATA","GT:GD:AC:MR:NNS:FT","0/0:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","1/2:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:."});
		m.updateVcfRecordWithCCM(vcf1, meta.getCallerSamplePositions());
		ffMap = VcfUtils.getFormatFieldsAsMap(vcf1.getFormatFields());
		assertArrayEquals(new String[] {"15","15"}, ffMap.get(VcfHeaderUtils.FORMAT_CCM));
		assertArrayEquals(new String[] {"Reference","DoubleSomatic"}, ffMap.get(VcfHeaderUtils.FORMAT_CCC));
		
		vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T,G",".",".","FLANK=ACACATACATA","GT:GD:AC:MR:NNS:FT","./.:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","1/2:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:."});
		m.updateVcfRecordWithCCM(vcf1, meta.getCallerSamplePositions());
		ffMap = VcfUtils.getFormatFieldsAsMap(vcf1.getFormatFields());
		assertArrayEquals(new String[] {"5","5"}, ffMap.get(VcfHeaderUtils.FORMAT_CCM));
		assertArrayEquals(new String[] {".","."}, ffMap.get(VcfHeaderUtils.FORMAT_CCC));
		
		vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T,G",".",".","FLANK=ACACATACATA","GT:GD:AC:MR:NNS:FT","1/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","1/2:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:."});
		m.updateVcfRecordWithCCM(vcf1, meta.getCallerSamplePositions());
		ffMap = VcfUtils.getFormatFieldsAsMap(vcf1.getFormatFields());
		assertArrayEquals(new String[] {"35","35"}, ffMap.get(VcfHeaderUtils.FORMAT_CCM));
		assertArrayEquals(new String[] {"Germline","Somatic"}, ffMap.get(VcfHeaderUtils.FORMAT_CCC));

	}
	
	@Test
	public void multiCallerMultiSample() {
		 VcfRecord vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T",".",".","FLANK=ACACATACATA","GT:GD:AC:MR:NNS:FT","0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","0/0:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:.","0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","0/0:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:."});
		 VcfFileMeta meta = new VcfFileMeta(createTwoSampleTwoCallerVcf());
		 assertArrayEquals(new short[]{1,2}, meta.getCallerSamplePositions().get("1"));
		 assertArrayEquals(new short[]{3,4}, meta.getCallerSamplePositions().get("2"));
		 CCMMode m = new CCMMode();
		m.updateVcfRecordWithCCM(vcf1, meta.getCallerSamplePositions());
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(vcf1.getFormatFields());
		assertArrayEquals(new String[] {"22","22","22","22"}, ffMap.get(VcfHeaderUtils.FORMAT_CCM));
		assertArrayEquals(new String[] {"Germline","ReferenceNoVariant","Germline","ReferenceNoVariant"}, ffMap.get(VcfHeaderUtils.FORMAT_CCC));
		
		vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T,G",".",".","FLANK=ACACATACATA","GT:GD:AC:MR:NNS:FT","1/2:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","1/2:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:.","0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","0/0:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:."});
		m.updateVcfRecordWithCCM(vcf1, meta.getCallerSamplePositions());
		ffMap = VcfUtils.getFormatFieldsAsMap(vcf1.getFormatFields());
		assertArrayEquals(new String[] {"47","47","22","22"}, ffMap.get(VcfHeaderUtils.FORMAT_CCM));
		assertArrayEquals(new String[] {"DoubleGermline","Germline","Germline","ReferenceNoVariant"}, ffMap.get(VcfHeaderUtils.FORMAT_CCC));
		
		vcf1 = new VcfRecord(new String[]{"chr8","12306635","rs28428895","C","T,G",".",".","FLANK=ACACATACATA","GT:GD:AC:MR:NNS:FT","0/1:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","1/2:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:.","1/2:C/T:C10[39],3[30],G1[11],0[0],T7[41.29],1[42]:8:8:.","0/2:C/C:C19[36.11],20[38.45],T1[42],0[0]:1:1:."});
		m.updateVcfRecordWithCCM(vcf1, meta.getCallerSamplePositions());
		ffMap = VcfUtils.getFormatFieldsAsMap(vcf1.getFormatFields());
		assertArrayEquals(new String[] {"25","25","44","44"}, ffMap.get(VcfHeaderUtils.FORMAT_CCM));
		assertArrayEquals(new String[] {"Germline","Somatic","DoubleGermline","GermlineReversionToReference"}, ffMap.get(VcfHeaderUtils.FORMAT_CCC));
	}
	
	@Test
	public void  controlIsMissing() {
		assertEquals(1, CCMMode.getCCM(DOT, "./."));
		assertEquals(2, CCMMode.getCCM(DOT, "0/0"));
		assertEquals(3, CCMMode.getCCM(DOT, "0/1"));
		assertEquals(3, CCMMode.getCCM(DOT, "0/2"));
		assertEquals(3, CCMMode.getCCM(DOT, "0/6"));
		assertEquals(3, CCMMode.getCCM(DOT, "0/200"));
		assertEquals(4, CCMMode.getCCM(DOT, "1/1"));
		assertEquals(4, CCMMode.getCCM(DOT, "2/2"));
		assertEquals(4, CCMMode.getCCM(DOT, "99/99"));
		assertEquals(5, CCMMode.getCCM(DOT, "1/2"));
		assertEquals(5, CCMMode.getCCM(DOT, "2/3"));
		assertEquals(5, CCMMode.getCCM(DOT, "6/1"));
	}
	
	@Test
	public void  testIsMissing() {
		assertEquals(1, CCMMode.getCCM("./.", DOT));
		assertEquals(11, CCMMode.getCCM("0/0", DOT));
		assertEquals(21, CCMMode.getCCM("0/1", DOT));
		assertEquals(21, CCMMode.getCCM("0/3", DOT));
		assertEquals(21, CCMMode.getCCM("0/5432", DOT));
		assertEquals(31, CCMMode.getCCM("3/3", DOT));
		assertEquals(31, CCMMode.getCCM("1/1", DOT));
		assertEquals(31, CCMMode.getCCM("2/2", DOT));
		assertEquals(41, CCMMode.getCCM("1/2", DOT));
		assertEquals(41, CCMMode.getCCM("2/1", DOT));
		assertEquals(41, CCMMode.getCCM("2/5", DOT));
		assertEquals(41, CCMMode.getCCM("6/5", DOT));
		assertEquals(41, CCMMode.getCCM("6/12", DOT));
	}
	
	@Test
	public void controlHasSingleZero() {
		assertEquals(22, CCMMode.getCCM("0/1", "0/0"));
		assertEquals(23, CCMMode.getCCM("0/1", "0/1"));
		assertEquals(23, CCMMode.getCCM("0/2", "0/2"));
		assertEquals(23, CCMMode.getCCM("0/22", "0/22"));
		
		assertEquals(24, CCMMode.getCCM("0/1", "1/1"));
		assertEquals(24, CCMMode.getCCM("0/2", "2/2"));
		assertEquals(24, CCMMode.getCCM("0/222", "222/222"));
		
		assertEquals(25, CCMMode.getCCM("0/1", "1/2"));
		assertEquals(25, CCMMode.getCCM("0/2", "1/2"));
		assertEquals(25, CCMMode.getCCM("0/2", "3/2"));
		assertEquals(25, CCMMode.getCCM("0/1", "3/1"));
		assertEquals(25, CCMMode.getCCM("0/1", "1/3"));
		
		assertEquals(26, CCMMode.getCCM("0/1", "0/2"));
		assertEquals(26, CCMMode.getCCM("0/2", "0/3"));
		assertEquals(26, CCMMode.getCCM("0/2", "0/1"));
		assertEquals(26, CCMMode.getCCM("0/1", "0/5"));
		
		assertEquals(27, CCMMode.getCCM("0/1", "2/2"));
		assertEquals(27, CCMMode.getCCM("0/2", "1/1"));
		assertEquals(27, CCMMode.getCCM("0/2", "10/10"));
		assertEquals(27, CCMMode.getCCM("0/1", "10/10"));
	}
	
	@Test
	public void controlIsHom() {
		assertEquals(31, CCMMode.getCCM("1/1", "./."));
		assertEquals(32, CCMMode.getCCM("1/1", "0/0"));
		
		assertEquals(33, CCMMode.getCCM("1/1", "0/1"));
		assertEquals(33, CCMMode.getCCM("1/1", "1/0"));
		assertEquals(33, CCMMode.getCCM("3/3", "3/0"));
		assertEquals(33, CCMMode.getCCM("3/3", "0/3"));
		
		assertEquals(34, CCMMode.getCCM("1/1", "1/1"));
		assertEquals(34, CCMMode.getCCM("101/101", "101/101"));
		
		assertEquals(35, CCMMode.getCCM("1/1", "1/2"));
		assertEquals(35, CCMMode.getCCM("1/1", "1/3"));
		assertEquals(35, CCMMode.getCCM("2/2", "1/2"));
		
		assertEquals(36, CCMMode.getCCM("1/1", "2/3"));
		assertEquals(36, CCMMode.getCCM("2/2", "1/3"));
		assertEquals(36, CCMMode.getCCM("2/2", "3/1"));
		assertEquals(36, CCMMode.getCCM("2/2", "3/5"));
		
		assertEquals(37, CCMMode.getCCM("1/1", "2/2"));
		assertEquals(37, CCMMode.getCCM("2/2", "1/1"));
	}
	
	@Test
	public void controlIsHet() {
		assertEquals(41, CCMMode.getCCM("1/2", "./."));
		assertEquals(42, CCMMode.getCCM("1/2", "0/0"));
		assertEquals(42, CCMMode.getCCM("3/2", "0/0"));
		
		assertEquals(43, CCMMode.getCCM("1/2", "0/1"));
		assertEquals(43, CCMMode.getCCM("1/2", "1/0"));
		
		assertEquals(44, CCMMode.getCCM("1/2", "2/0"));
		assertEquals(44, CCMMode.getCCM("1/2", "0/2"));
		
		assertEquals(50, CCMMode.getCCM("1/2", "3/3"));
		assertEquals(50, CCMMode.getCCM("1/2", "33/33"));
		
		assertEquals(48, CCMMode.getCCM("1/2", "4/3"));
		assertEquals(48, CCMMode.getCCM("1/2", "3/7"));
		/*
		 * should the following have their own unique code?
		 */
		assertEquals(49, CCMMode.getCCM("1/2", "0/3"));
		assertEquals(49, CCMMode.getCCM("1/2", "0/7"));
		
		assertEquals(50, CCMMode.getCCM("1/2", "3/3"));
		assertEquals(50, CCMMode.getCCM("1/2", "4/4"));
		
		assertEquals(51, CCMMode.getCCM("1/2", "1/3"));
		assertEquals(51, CCMMode.getCCM("1/2", "1/4"));
		assertEquals(51, CCMMode.getCCM("1/2", "1/5"));
	}
	
	@Test
	public void controlIsRef() {
		assertEquals(12, CCMMode.getCCM(REF, "0/0"));
		assertEquals(11, CCMMode.getCCM(REF, "./."));
		assertEquals(13, CCMMode.getCCM(REF, "0/1"));
		assertEquals(13, CCMMode.getCCM(REF, "0/2"));
		assertEquals(13, CCMMode.getCCM(REF, "0/23"));
		assertEquals(14, CCMMode.getCCM(REF, "1/1"));
		assertEquals(14, CCMMode.getCCM(REF, "2/2"));
		assertEquals(14, CCMMode.getCCM(REF, "23/23"));
		assertEquals(15, CCMMode.getCCM(REF, "1/2"));
		assertEquals(15, CCMMode.getCCM(REF, "2/1"));
		assertEquals(15, CCMMode.getCCM(REF, "2/12"));
	}
	@Test
	public void testIsRef() {
		assertEquals(12, CCMMode.getCCM("0/0", REF));
		assertEquals(22, CCMMode.getCCM("0/1", REF));
		assertEquals(22, CCMMode.getCCM("0/2", REF));
		assertEquals(22, CCMMode.getCCM("0/102", REF));
		assertEquals(32, CCMMode.getCCM("102/102", REF));
		assertEquals(32, CCMMode.getCCM("12/12", REF));
		assertEquals(32, CCMMode.getCCM("12/12", REF));
		assertEquals(42, CCMMode.getCCM("1/2", REF));
		assertEquals(42, CCMMode.getCCM("1/3", REF));
		assertEquals(42, CCMMode.getCCM("1/4", REF));
		assertEquals(42, CCMMode.getCCM("2/4", REF));
		assertEquals(42, CCMMode.getCCM("3/4", REF));
		assertEquals(42, CCMMode.getCCM("322/433", REF));
	}
	
	public static VcfHeader createTwoSampleVcf() {
		VcfHeader h = new VcfHeader();
		h.addOrReplace("##fileformat=VCFv4.0");
		h.addOrReplace("##qControlBamUUID=ABC");
		h.addOrReplace("##qTestBamUUID=DEF");
		h.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "ABC\tDEF");
		return h;
	}
	public static VcfHeader createTwoSampleTwoCallerVcf() {
		VcfHeader h = new VcfHeader();
		h.addOrReplace("##fileformat=VCFv4.0");
		h.addOrReplace("##1:qControlBamUUID=ABC");
		h.addOrReplace("##1:qTestBamUUID=DEF");
		h.addOrReplace("##2:qControlBamUUID=ABC");
		h.addOrReplace("##2:qTestBamUUID=DEF");
		h.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "ABC_1\tDEF_1\tABC_2\tDEF_2");
		return h;
	}
	public static VcfHeader createSingleSampleTwoCallerVcf() {
		VcfHeader h = new VcfHeader();
		h.addOrReplace("##fileformat=VCFv4.0");
		h.addOrReplace("##1:qTestBamUUID=DEF");
		h.addOrReplace("##2:qTestBamUUID=DEF");
		h.addOrReplace(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT + "DEF_1\tDEF_2");
		return h;
	}
	
}
