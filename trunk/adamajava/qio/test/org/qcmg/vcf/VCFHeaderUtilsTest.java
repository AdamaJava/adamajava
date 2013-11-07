package org.qcmg.vcf;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Vector;

import org.junit.Test;

public class VCFHeaderUtilsTest {
	
	@Test
	public void testGetMapFromInfoHeader() {
		
		Map<String, Integer> mapOfFiles = VCFHeaderUtils.getMapFromInfoHeader(buildVcfHeader());
		assertEquals(false, mapOfFiles.isEmpty());
		assertEquals(23, mapOfFiles.size());
		for (int i = 1 ; i < 24 ; i++) assertEquals(true, mapOfFiles.containsValue(i));
		
		assertEquals(true, mapOfFiles.containsKey("/path/AAAA_1127/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1127.GermlineSNV.dcc1"));
		assertEquals(true, mapOfFiles.containsKey("/path/AAAA_1971/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_sureselect_exome/AAAA_1971.GermlineSNV.dcc1"));
		
	}
	
	@Test
	public void testGetIdsForPatient() {
		
		Map<String, Integer> mapOfFiles = VCFHeaderUtils.getMapFromInfoHeader(buildVcfHeader());
		
		assertEquals(1, VCFHeaderUtils.getIdsForPatient(mapOfFiles, "AAAA_1127")[0]);
		assertEquals(null, VCFHeaderUtils.getIdsForPatient(mapOfFiles, "AAAA_1992"));
		
		assertEquals(2, VCFHeaderUtils.getIdsForPatient(mapOfFiles, "AAAA_1971").length);
		for (int i : VCFHeaderUtils.getIdsForPatient(mapOfFiles, "AAAA_1971")) {
			assertEquals(true, i == 22 || i == 23);
		}
		
		assertEquals(3, VCFHeaderUtils.getIdsForPatient(mapOfFiles, "AAAA_1955").length);
		for (int i : VCFHeaderUtils.getIdsForPatient(mapOfFiles, "AAAA_1955")) {
			assertEquals(true, i == 10 || i == 11 || i == 12);
		}
		
	}
	
	private VCFHeader buildVcfHeader() {
		Vector<String> headerData = new Vector<String>();
		
		headerData.add("##fileformat=VCFv4.0");
		headerData.add("##search_string=GermlineSNV.dcc1");
		headerData.add("##search_directory=/path");
		headerData.add("##additionalsearch_directory=[qSNP]");
		headerData.add("##INFO=<ID=1,Number=0,Type=Flag,Description=\"/path/AAAA_1127/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1127.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=2,Number=0,Type=Flag,Description=\"/path/AAAA_1151/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1151.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=3,Number=0,Type=Flag,Description=\"/path/AAAA_1699/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1699.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=4,Number=0,Type=Flag,Description=\"/path/AAAA_1830/variants/qSNP/3NormalAdjacent_vs_7PrimaryTumour_solid_exome/AAAA_1830.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=5,Number=0,Type=Flag,Description=\"/path/AAAA_1839/variants/qSNP/1NormalBlood_vs_10XenograftCellLine/AAAA_1839.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=6,Number=0,Type=Flag,Description=\"/path/AAAA_1840/variants/qSNP/4NormalOther_vs_10XenograftCellLine/AAAA_1840.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=7,Number=0,Type=Flag,Description=\"/path/AAAA_1840/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1840.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=8,Number=0,Type=Flag,Description=\"/path/AAAA_1953/variants/qSNP/4NormalOther_vs_7PrimaryTumour/AAAA_1953.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=9,Number=0,Type=Flag,Description=\"/path/AAAA_1953/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1953.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=10,Number=0,Type=Flag,Description=\"/path/AAAA_1955/variants/qSNP/4NormalOther_vs_7PrimaryTumour/AAAA_1955.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=11,Number=0,Type=Flag,Description=\"/path/AAAA_1955/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1955.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=12,Number=0,Type=Flag,Description=\"/path/AAAA_1955/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_genome/AAAA_1955.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=13,Number=0,Type=Flag,Description=\"/path/AAAA_1956/variants/qSNP/4NormalOther_vs_10XenograftCellLine/AAAA_1956.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=14,Number=0,Type=Flag,Description=\"/path/AAAA_1956/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1956.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=15,Number=0,Type=Flag,Description=\"/path/AAAA_1956/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_genome/AAAA_1956.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=16,Number=0,Type=Flag,Description=\"/path/AAAA_1959/variants/qSNP/4NormalOther_vs_7PrimaryTumour/AAAA_1959.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=17,Number=0,Type=Flag,Description=\"/path/AAAA_1959/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1959.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=18,Number=0,Type=Flag,Description=\"/path/AAAA_1959/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_genome/AAAA_1959.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=19,Number=0,Type=Flag,Description=\"/path/AAAA_1962/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1962.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=20,Number=0,Type=Flag,Description=\"/path/AAAA_1965/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1965.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=21,Number=0,Type=Flag,Description=\"/path/AAAA_1966/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_exome/AAAA_1966.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=22,Number=0,Type=Flag,Description=\"/path/AAAA_1971/variants/qSNP/4NormalOther_vs_7PrimaryTumour/AAAA_1971.GermlineSNV.dcc1\">");
		headerData.add("##INFO=<ID=23,Number=0,Type=Flag,Description=\"/path/AAAA_1971/variants/qSNP/4NormalOther_vs_7PrimaryTumour_solid_sureselect_exome/AAAA_1971.GermlineSNV.dcc1\">");
		
		VCFHeader header = new VCFHeader(headerData);
		return header;
	}

}
