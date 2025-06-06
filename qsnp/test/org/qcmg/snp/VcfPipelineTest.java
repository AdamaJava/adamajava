package org.qcmg.snp;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ini4j.Ini;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qio.vcf.VcfFileReader;

import static org.junit.Assert.*;

public class VcfPipelineTest {
	
	@org.junit.Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	@org.junit.Rule
    public ExpectedException thrown= ExpectedException.none();

	private void createFile( File vcfFile, List<String> data ) throws IOException {
		try (final PrintWriter pw = new PrintWriter(vcfFile)){
			data.forEach(pw::println);
		}
	}
	
	@Test
	public void readsEndAfter1Snp() throws Exception {
		final File iniFile = testFolder.newFile("qsnp_vcf.ini");	
		final File testInputVcf = testFolder.newFile("test.vcf");
		final File testInputBam = testFolder.newFile("test.sam");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		List<String> vcfs = Arrays.asList(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1",
				"chrX	2710840	rs311168	C	T	.	.	AC=2;AF=1.00;AN=2;DP=31;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=33.08;SOR=1.688;IN=1,2;DB;VLD;VAF=0.6125	GT:GD	1/1:T/T");
		createFile(testInputVcf, vcfs);
		
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "testVcf = " + testInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "testBam = " + testInputBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = vcf\nfilter = and (MAPQ > 10, CIGAR_M>34, MD_mismatch<=3)", true);	// append to file
		
		new VcfPipeline(new Ini(iniFile), new QExec("stackOverflow2", "test", null), true);
		assertEquals(vcfs.size()-1, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
	@Test
	public void updateGTField() {
		VcfRecord v = new VcfRecord(new String[]{"chr1","881627","rs2272757","G","A","164.77",".","AC=1;AF=0.500;AN=2;BaseQRankSum=-2.799;ClippingRankSum=1.555;DB;DP=18;FS=7.375;MLEAC=1;MLEAF=0.500;MQ=59.94;MQ0=0;MQRankSum=0.400;QD=9.15;ReadPosRankSum=0.755;SOR=0.044","GT:AD:DP:GQ:PL","0/1:10,8:18:99:193,0,370",".:.:.:.:."});
		List<String> ff = v.getFormatFields();
		assertEquals(3, ff.size());
        assertTrue(ff.get(1).contains("0/1"));
		assertEquals(".:.:.:.:.", ff.get(2));
		VcfPipeline.updateGTFieldIFNoCall(v, 10, false);
		ff = v.getFormatFields();
		assertEquals(3, ff.size());
        assertTrue(ff.get(1).contains("0/1"));
		assertEquals("./.:.:10:.:.", ff.get(2));
	}
	@Test
	public void updateGTFieldAgain() {
		VcfRecord v = new VcfRecord(new String[]{"chr1","31029","rs372996257","G","A",".",".","FLANK=TCACCAGGAAC;BaseQRankSum=1.632;ClippingRankSum=0.457;DP=15;FS=24.362;MQ=23.95;MQRankSum=-0.326;QD=2.38;ReadPosRankSum=2.285;SOR=3.220;IN=1,2;DB","GT:AD:CCC:CCM:DP:FT:GQ:INF:MR:NNS:OABS","./.:.:.:3:.:.:.:.:.:.:G14[37.5]0[0]","0/1:11,4:.:3:15:SBIASALT:64:SOMATIC:4:4:A0[0]4[41];G10[39]2[41]"});
		List<String> ff = v.getFormatFields();
		assertEquals(3, ff.size());
        assertTrue(ff.get(1).contains("./."));
		VcfPipeline.updateGTFieldIFNoCall(v, 14, true);
		ff = v.getFormatFields();
		assertEquals(3, ff.size());
        assertTrue(ff.get(1).contains("./."));
	}
	
	@Test
	public void classify() throws Exception {
		final File iniFile = testFolder.newFile("qsnp_vcf.ini");	
		final File testInputVcf = testFolder.newFile("test.vcf");
		final File testInputBam = testFolder.newFile("test.sam");
		final File vcfOutput = testFolder.newFile("output.vcf");
		final File controlInputVcf = testFolder.newFile("control.vcf");
		final File controlInputBam = testFolder.newFile("control.sam");
		
		List<String> testVcfs = Arrays.asList(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT,"chrX	2710840	rs311168	C	T	.	.	AC=2;AF=1.00;AN=2;DP=31;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=33.08;SOR=1.688;IN=1,2;DB;VLD;VAF=0.6125	GT:GD	1/1:T/T",
				"chrX	2710895	rs311169	C	G	.	. 	AC=2;AF=1.00;AN=2;DP=22;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=37.63;SOR=1.828;IN=1,2;DB;VAF=0.6161	GT:GD	1/1:G/G");
		List<String> controlVcfs = Arrays.asList(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT,"chrY	2710840	rs311168	C	T	.	.	AC=2;AF=1.00;AN=2;DP=31;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=33.08;SOR=1.688;IN=1,2;DB;VLD;VAF=0.6125	GT:GD	1/1:T/T",
				"chrY	2710895	rs311169	C	G	.	. 	AC=2;AF=1.00;AN=2;DP=22;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=37.63;SOR=1.828;IN=1,2;DB;VAF=0.6161	GT:GD	1/1:G/G");
		createFile(testInputVcf, testVcfs);
		createFile(controlInputVcf, controlVcfs);
		
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "testVcf = " + testInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "testBam = " + testInputBam.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "controlVcf = " + controlInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "controlBam = " + controlInputBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = vcf\nfilter = and (MAPQ > 10, CIGAR_M>34, MD_mismatch<=3)", true);	// append to file
		
		new VcfPipeline(new Ini(iniFile), new QExec("stackOverflow2", "test", null), false);
		// check the vcf output file
		assertEquals(testVcfs.size() + controlVcfs.size() -2, noOfLinesInVCFOutputFile(vcfOutput));		// -2 removes the 2 header files
		try (VcfFileReader reader = new VcfFileReader(vcfOutput)){
			for (VcfRecord vcf : reader) {
				
				/*
				 * I expect to see NCIG in all vcfs
				 */
				Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(vcf.getFormatFields());
				String[] infArray = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
				if (vcf.getChromosome().equals("chrX")) {
					// missing control so should be first column.
					assertEquals("NCIG", infArray[0]);
				} else if (vcf.getChromosome().equals("chrY")) {
					assertEquals("NCIG", infArray[1]);
				}
			}
		}
	}
	
	@Test
	public void classifySingleSample() throws Exception {
		final File iniFile = testFolder.newFile("qsnp_vcf.ini");	
		final File testInputVcf = testFolder.newFile("test.vcf");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		List<String> testVcfs = Arrays.asList(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT," chr1	11081	rs10218495	G	T	62.74	.	AC=2;AF=1.00;AN=2;DB;DP=2;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=24.51;MQ0=0;QD=31.37;SOR=2.303	GT:AD:DP:GQ:PL	1/1:0,2:2:6:90,6,0",
				"chr1	12783	.	G	A	460.77	.	AC=2;AF=1.00;AN=2;DP=19;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=25.27;MQ0=0;QD=24.25;SOR=5.994	GT:AD:DP:GQ:PL	1/1:0,19:19:55:489,55,0");
		createFile(testInputVcf, testVcfs);
		
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "testVcf = " + testInputVcf.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = vcf\nfilter = and (MAPQ > 10, CIGAR_M>34, MD_mismatch<=3)", true);	// append to file
		
		new VcfPipeline(new Ini(iniFile), new QExec("classifySingleSample", "test", null), true);
		// check the vcf output file
		assertEquals(testVcfs.size() - 1, noOfLinesInVCFOutputFile(vcfOutput));		// -1 removes the header file
		try (VcfFileReader reader = new VcfFileReader(vcfOutput)){
			for (VcfRecord vcf : reader) {
				
				/*
				 * should have added the FT and INF fields to the format fields
				 */
				Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(vcf.getFormatFields());
				assertEquals(7, ffMap.size());
				
				String[] infArray = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
				assertEquals(1, infArray.length);												// only dealing with single sample data here
				assertEquals(Constants.MISSING_DATA_STRING, infArray[0]);		// only dealing with single sample data here
				String[] ftArray = ffMap.get(VcfHeaderUtils.FORMAT_FILTER);
				assertEquals(1, ftArray.length);													// only dealing with single sample data here
				assertEquals(Constants.MISSING_DATA_STRING, ftArray[0]);		// only dealing with single sample data here
			}
		}
	}
	@Test
	public void readsEndAfter2Snps() throws Exception {
		final File iniFile = testFolder.newFile("qsnp_vcf.ini");	
		final File testInputVcf = testFolder.newFile("test.vcf");
		final File testInputBam = testFolder.newFile("test.sam");
		final File vcfOutput = testFolder.newFile("output.vcf");
		final File controlInputVcf = testFolder.newFile("control.vcf");
		final File controlInputBam = testFolder.newFile("control.sam");
		
		List<String> vcfs = Arrays.asList(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1",
				"chrX	2710840	rs311168	C	T	.	.	AC=2;AF=1.00;AN=2;DP=31;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=33.08;SOR=1.688;IN=1,2;DB;VLD;VAF=0.6125	GT:GD	1/1:T/T",
				"chrX	2710895	rs311169	C	G	.	. 	AC=2;AF=1.00;AN=2;DP=22;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=37.63;SOR=1.828;IN=1,2;DB;VAF=0.6161	GT:GD	1/1:G/G");
		createFile(testInputVcf, vcfs);
		
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "testVcf = " + testInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "testBam = " + testInputBam.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "controlVcf = " + controlInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "controlBam = " + controlInputBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = vcf\nfilter = and (MAPQ > 10, CIGAR_M>34, MD_mismatch<=3)", true);	// append to file
		
		new VcfPipeline(new Ini(iniFile), new QExec("stackOverflow2", "test", null), true);
		// check the vcf output file, minus CHROM line
		assertEquals(vcfs.size()-1, noOfLinesInVCFOutputFile(vcfOutput));
	}
	@Test
	public void readsEndBefore3Snps() throws Exception {
		final File iniFile = testFolder.newFile("qsnp_vcf.ini");	
		final File testInputVcf = testFolder.newFile("test.vcf");
		final File testInputBam = testFolder.newFile("test.sam");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		List<String> vcfs = Arrays.asList(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1",
				"chrX	2710840	rs311168	C	T	.	.	AC=2;AF=1.00;AN=2;DP=31;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=33.08;SOR=1.688;IN=1,2;DB;VLD;VAF=0.6125	GT:GD	1/1:T/T",
				"chrX	2710895	rs311169	C	G	.	. 	AC=2;AF=1.00;AN=2;DP=22;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=37.63;SOR=1.828;IN=1,2;DB;VAF=0.6161	GT:GD	1/1:G/G",
	"chrX	2711895	rs311169	C	G	.	. 	AC=2;AF=1.00;AN=2;DP=22;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=37.63;SOR=1.828;IN=1,2;DB;VAF=0.6161	GT:GD	1/1:G/G");
		createFile(testInputVcf, vcfs);
		
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "testVcf = " + testInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "testBam = " + testInputBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = vcf\nfilter = and (MAPQ > 10, CIGAR_M>34, MD_mismatch<=3)", true);	// append to file
		
		new VcfPipeline(new Ini(iniFile), new QExec("stackOverflow2", "test", null), true);
		// check the vcf output file, minus CHROM line
		assertEquals(vcfs.size()-1, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
	@Test
	public void readsEndBefore4Snps() throws Exception {
		final File iniFile = testFolder.newFile("qsnp_vcf.ini");	
		final File testInputVcf = testFolder.newFile("test.vcf");
		final File testInputBam = testFolder.newFile("test.sam");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		List<String> vcfs = Arrays.asList(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1",
				"chrX	2710840	rs311168	C	T	.	.	AC=2;AF=1.00;AN=2;DP=31;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=33.08;SOR=1.688;IN=1,2;DB;VLD;VAF=0.6125	GT:GD	1/1:T/T",
				"chrX	2710895	rs311169	C	G	.	. 	AC=2;AF=1.00;AN=2;DP=22;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=37.63;SOR=1.828;IN=1,2;DB;VAF=0.6161	GT:GD	1/1:G/G",
				"chrX	2711895	rs311169	C	G	.	. 	AC=2;AF=1.00;AN=2;DP=22;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=37.63;SOR=1.828;IN=1,2;DB;VAF=0.6161	GT:GD	1/1:G/G",
				"chrY	1000	rs311169	C	G	.	. 	AC=2;AF=1.00;AN=2;DP=22;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=37.63;SOR=1.828;IN=1,2;DB;VAF=0.6161	GT:GD	1/1:G/G");
		createFile(testInputVcf, vcfs);
		
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "testVcf = " + testInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "testBam = " + testInputBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = vcf\nfilter = and (MAPQ > 10, CIGAR_M>34, MD_mismatch<=3)", true);	// append to file
		
		new VcfPipeline(new Ini(iniFile), new QExec("stackOverflow2", "test", null), true);
		// check the vcf output file, minus CHROM line
		assertEquals(vcfs.size() - 1, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
	@Test
	public void compoundSnps() throws Exception {
		final File iniFile = testFolder.newFile("qsnp_vcf.ini");	
		final File testInputVcf = testFolder.newFile("test.vcf");
		final File testInputBam = testFolder.newFile("test.sam");
		final File vcfOutput = testFolder.newFile("output.vcf");
		
		List<String> vcfs = Arrays.asList(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1",
				"chrX	2710840	rs311168	C	T	.	.	AC=2;AF=1.00;AN=2;DP=31;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=33.08;SOR=1.688;IN=1,2;DB;VLD;VAF=0.6125	GT:GD:DP:AD:GQ	1/1:T/T:20:1,19:50",
				"chrX	2710841	rs311169	C	T	.	.	AC=2;AF=1.00;AN=2;DP=31;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=33.08;SOR=1.688;IN=1,2;DB;VLD;VAF=0.6125	GT:GD:DP:AD:GQ	1/1:T/T:20:2,18:60");
		createFile(testInputVcf, vcfs);
		
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "testVcf = " + testInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "testBam = " + testInputBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = vcf\nfilter = and (MAPQ > 10, CIGAR_M>34, MD_mismatch<=3)", true);	// append to file
		
		new VcfPipeline(new Ini(iniFile), new QExec("compoundSnp", "test", null), true);
		// check the vcf output file, minus CHROM line
		assertEquals(2, noOfLinesInVCFOutputFile(vcfOutput));
	}
	
	@Test
	public void sorting() throws IOException {
		
		Comparator<String> comp = null;
		List<String> sortedContigs = new ArrayList<>();
		final File testInputBam = testFolder.newFile("test.sam");
		createFile(testInputBam, getBamFile());
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(testInputBam)) {
			final SAMFileHeader header = reader.getFileHeader();
			
			for (final SAMSequenceRecord contig : header.getSequenceDictionary().getSequences()) {
				sortedContigs.add(contig.getSequenceName());
			}
		}
		
		comp = ChrPositionComparator.getChrNameComparator(sortedContigs);
		Comparator<ChrPosition> c = ChrPositionComparator.getComparator(comp);
		
		List<ChrPosition> chrPos = sortedContigs.stream().map(s -> ChrPositionUtils.getChrPosition(s, 1, 1)).sorted(VcfPipeline.CHR_COMPARATOR).collect(Collectors.toList());

        assertEquals(24, chrPos.indexOf(ChrPositionUtils.getChrPosition("chrMT", 1, 1)));
		chrPos.sort(c);
		assertEquals(83, chrPos.indexOf(ChrPositionUtils.getChrPosition("chrMT", 1, 1)));

        assertTrue(c.compare(ChrPositionUtils.getChrPosition("chr1", 1, 1), ChrPositionUtils.getChrPosition("chr1", 2, 2)) < 0);
        assertEquals(0, c.compare(ChrPositionUtils.getChrPosition("chr1", 2, 2), ChrPositionUtils.getChrPosition("chr1", 2, 2)));
        assertTrue(c.compare(ChrPositionUtils.getChrPosition("chr1", 2, 2), ChrPositionUtils.getChrPosition("chr1", 1, 1)) > 0);
        assertTrue(c.compare(ChrPositionUtils.getChrPosition("chr1", 2, 2), ChrPositionUtils.getChrPosition("chr2", 1, 1)) < 0);
        assertTrue(c.compare(ChrPositionUtils.getChrPosition("chr2", 2, 20), ChrPositionUtils.getChrPosition("chr2", 2, 2)) > 0);
	}
	
	
	@Test
	public void getListComparator() throws IOException {
		List<String> list = Arrays.asList("chr1", "chr2");
		Comparator<String> comp = ChrPositionComparator.getChrNameComparator(list);

        assertTrue(comp.compare("chr1", "chr2") < 0);
        assertTrue(comp.compare("chr2", "chr1") > 0);
		
		final File testInputBam = testFolder.newFile("test.sam");
		createFile(testInputBam, getBamFile());
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(testInputBam)) {
			final SAMFileHeader header = reader.getFileHeader();
			
			final List<String> sortedContigs = new ArrayList<>();
			for (final SAMSequenceRecord contig : header.getSequenceDictionary().getSequences()) {
				sortedContigs.add(contig.getSequenceName());
			}
			comp = ChrPositionComparator.getChrNameComparator(sortedContigs);
		}

        assertTrue(comp.compare("chr1", "chr2") < 0);
        assertTrue(comp.compare("chr2", "chr1") > 0);
        assertTrue(comp.compare("chrMT", "chr1") > 0);
        assertTrue(comp.compare("chrMT", "GL000249.1") > 0);
		
		List<String> toSort = Arrays.asList("chrMT", "chrX","chr1", "chrY", "GL000217.1");
		toSort.sort(comp);
		assertEquals(0, toSort.indexOf("chr1"));
		assertEquals(1, toSort.indexOf("chrX"));
		assertEquals(2, toSort.indexOf("chrY"));
		assertEquals(3, toSort.indexOf("GL000217.1"));
		assertEquals(4, toSort.indexOf("chrMT"));
	}
	
	@Ignore
	public void testVcfOmissionBasedOnLackOfData() {
		VcfRecord vcf = new VcfRecord(new String[]{"chrX","84428775",".","C","CT","368.74",".","AC=1;AF=0.500;AN=2;BaseQRankSum=1.883;ClippingRankSum=0.000;DP=18;ExcessHet=3.0103;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQRankSum=0.000;QD=20.49;ReadPosRankSum=0.774;SOR=0.941","GT","./."});
        assertEquals(VcfPipeline.FF_NOT_ENOUGH_INFO, vcf.getFormatFieldStrings());
		vcf = new VcfRecord(new String[]{"chrX","84428775",".","C","CT","368.74",".","AC=1;AF=0.500;AN=2;BaseQRankSum=1.883;ClippingRankSum=0.000;DP=18;ExcessHet=3.0103;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQRankSum=0.000;QD=20.49;ReadPosRankSum=0.774;SOR=0.941","GT","C/CT"});
        assertNotEquals(VcfPipeline.FF_NOT_ENOUGH_INFO, vcf.getFormatFieldStrings());
		vcf = new VcfRecord(new String[]{"chrX","84428775",".","C","CT","368.74",".","AC=1;AF=0.500;AN=2;BaseQRankSum=1.883;ClippingRankSum=0.000;DP=18;ExcessHet=3.0103;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=60.00;MQRankSum=0.000;QD=20.49;ReadPosRankSum=0.774;SOR=0.941","GT:AD","./.:0"});
        assertNotEquals(VcfPipeline.FF_NOT_ENOUGH_INFO, vcf.getFormatFieldStrings());
	}
	
	@Test
	public void stackOverflow() throws Exception {
		/*
		 * I think that this is due to the comparator placing chrMT  before the GL's whereas in the bam headers and vcfs, chrMT is last.
		 * Not the case - see getLIstComparator test
		 * Would love to see the SO.....
		 */
		final File iniFile = testFolder.newFile("qsnp_vcf.ini");	
		final File testInputVcf = testFolder.newFile("test.vcf");
		final File testInputBam = testFolder.newFile("test.sam");
		final File vcfOutput = testFolder.newFile("output.vcf");
	
		List<String> vcfs = new ArrayList<>();
				
		/*
		 * 10000 is about enough to get the SO on my machine (runs with about 4gb mem)
		 */
		vcfs.add(  VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE_INCLUDING_FORMAT+"s1");
		for (int i = 0 ;i < 10000 ; i++) {
			vcfs.add("chr1	" + (33161 + (i * 2)) + "	.	A	C	217.77	.	AC=1;AF=0.500;AN=2;BaseQRankSum=-0.724;ClippingRankSum=-1.102;DP=23;FS=9.901;MLEAC=1;MLEAF=0.500;MQ=47.64;MQ0=0;MQRankSum=-2.677;QD=9.47;ReadPosRankSum=0.724;SOR=1.185	GT:AD:DP:GQ:PL	0/1:14,9:23:99:246,0,496");
		}
//		createFile(testInputBam, getBamFilechrX());
		createFile(testInputVcf, vcfs);
		
		IniFileGenerator.createRulesOnlyIni(iniFile);
		IniFileGenerator.addInputFiles(iniFile, false, "testVcf = " + testInputVcf.getAbsolutePath());
		IniFileGenerator.addInputFiles(iniFile, false, "testBam = " + testInputBam.getAbsolutePath());
		IniFileGenerator.addOutputFiles(iniFile, false, "vcf = " + vcfOutput.getAbsolutePath());
		
		// add runType to ini file
		IniFileGenerator.addStringToIniFile(iniFile, "[parameters]\nrunMode = vcf", true);	// append to file
		
		new VcfPipeline(new Ini(iniFile), new QExec("stackOverflow2", "test", null), true);
	}
	
	private List<String> getBamHeader() {
		return Arrays.asList("@HD	VN:1.4	SO:coordinate",
				"@SQ	SN:chr1	LN:249250621",
				"@SQ	SN:chr2	LN:243199373",
				"@SQ	SN:chr3	LN:198022430",
				"@SQ	SN:chr4	LN:191154276",
				"@SQ	SN:chr5	LN:180915260",
				"@SQ	SN:chr6	LN:171115067",
				"@SQ	SN:chr7	LN:159138663",
				"@SQ	SN:chr8	LN:146364022",
				"@SQ	SN:chr9	LN:141213431",
				"@SQ	SN:chr10	LN:135534747",
				"@SQ	SN:chr11	LN:135006516",
				"@SQ	SN:chr12	LN:133851895",
				"@SQ	SN:chr13	LN:115169878",
				"@SQ	SN:chr14	LN:107349540",
				"@SQ	SN:chr15	LN:102531392",
				"@SQ	SN:chr16	LN:90354753",
				"@SQ	SN:chr17	LN:81195210",
				"@SQ	SN:chr18	LN:78077248",
				"@SQ	SN:chr19	LN:59128983",
				"@SQ	SN:chr20	LN:63025520",
				"@SQ	SN:chr21	LN:48129895",
				"@SQ	SN:chr22	LN:51304566",
				"@SQ	SN:chrX	LN:155270560",
				"@SQ	SN:chrY	LN:59373566",
				"@SQ	SN:GL000191.1	LN:106433",
				"@SQ	SN:GL000192.1	LN:547496",
				"@SQ	SN:GL000193.1	LN:189789",
				"@SQ	SN:GL000194.1	LN:191469",
				"@SQ	SN:GL000195.1	LN:182896",
				"@SQ	SN:GL000196.1	LN:38914",
				"@SQ	SN:GL000197.1	LN:37175",
				"@SQ	SN:GL000198.1	LN:90085",
				"@SQ	SN:GL000199.1	LN:169874",
				"@SQ	SN:GL000200.1	LN:187035",
				"@SQ	SN:GL000201.1	LN:36148",
				"@SQ	SN:GL000202.1	LN:40103",
				"@SQ	SN:GL000203.1	LN:37498",
				"@SQ	SN:GL000204.1	LN:81310",
				"@SQ	SN:GL000205.1	LN:174588",
				"@SQ	SN:GL000206.1	LN:41001",
				"@SQ	SN:GL000207.1	LN:4262",
				"@SQ	SN:GL000208.1	LN:92689",
				"@SQ	SN:GL000209.1	LN:159169",
				"@SQ	SN:GL000210.1	LN:27682",
				"@SQ	SN:GL000211.1	LN:166566",
				"@SQ	SN:GL000212.1	LN:186858",
				"@SQ	SN:GL000213.1	LN:164239",
				"@SQ	SN:GL000214.1	LN:137718",
				"@SQ	SN:GL000215.1	LN:172545",
				"@SQ	SN:GL000216.1	LN:172294",
				"@SQ	SN:GL000217.1	LN:172149",
				"@SQ	SN:GL000218.1	LN:161147",
				"@SQ	SN:GL000219.1	LN:179198",
				"@SQ	SN:GL000220.1	LN:161802",
				"@SQ	SN:GL000221.1	LN:155397",
				"@SQ	SN:GL000222.1	LN:186861",
				"@SQ	SN:GL000223.1	LN:180455",
				"@SQ	SN:GL000224.1	LN:179693",
				"@SQ	SN:GL000225.1	LN:211173",
				"@SQ	SN:GL000226.1	LN:15008",
				"@SQ	SN:GL000227.1	LN:128374",
				"@SQ	SN:GL000228.1	LN:129120",
				"@SQ	SN:GL000229.1	LN:19913",
				"@SQ	SN:GL000230.1	LN:43691",
				"@SQ	SN:GL000231.1	LN:27386",
				"@SQ	SN:GL000232.1	LN:40652",
				"@SQ	SN:GL000233.1	LN:45941",
				"@SQ	SN:GL000234.1	LN:40531",
				"@SQ	SN:GL000235.1	LN:34474",
				"@SQ	SN:GL000236.1	LN:41934",
				"@SQ	SN:GL000237.1	LN:45867",
				"@SQ	SN:GL000238.1	LN:39939",
				"@SQ	SN:GL000239.1	LN:33824",
				"@SQ	SN:GL000240.1	LN:41933",
				"@SQ	SN:GL000241.1	LN:42152",
				"@SQ	SN:GL000242.1	LN:43523",
				"@SQ	SN:GL000243.1	LN:43341",
				"@SQ	SN:GL000244.1	LN:39929",
				"@SQ	SN:GL000245.1	LN:36651",
				"@SQ	SN:GL000246.1	LN:38154",
				"@SQ	SN:GL000247.1	LN:36422",
				"@SQ	SN:GL000248.1	LN:39786",
				"@SQ	SN:GL000249.1	LN:38502",
				"@SQ	SN:chrMT	LN:16569",
				"@RG	ID:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	SM:http://purl.org/net/grafli/collectedsample#6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a",
				"@PG	ID:bwa	VN:0.7.12-r1039	CL:bwa mem -p -t16 -R@RG\tID:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8\tSM:http://purl.org/net/grafli/collectedsample#6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a -H@CO\tq3BamUUID:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8 /opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/indexes/BWAKIT_0.7.12/GRCh37_ICGC_standard_v2.fa -	PN:bwa",
				//"@PG	ID:MarkDuplicates	VN:1.129(b508b2885562a4e932d3a3a60b8ea283b7ec78e2_1424706677)	CL:picard.sam.markduplicates.MarkDuplicates INPUT=[/mnt/genomeinfo_projects/data/20150624_Garvan/R_150409_KATNON_FGS_M001/FR07887046/inputFastq/2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8.aln.bam] OUTPUT=/mnt/genomeinfo_projects/sample/6/9/6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a/aligned_read_group_set/ecc3c778-a77e-49b7-a4f5-fe828bcf9b88.bam METRICS_FILE=/mnt/genomeinfo_projects/sample/6/9/6946f2e6-24aa-4cb8-a2fb-35fc3486bb9a/aligned_read_group_set/ecc3c778-a77e-49b7-a4f5-fe828bcf9b88.bam.dedup_metrics READ_NAME_REGEX=^[a-zA-Z0-9\-]+:[0-9]+:[a-zA-Z0-9]+:[0-9]:([0-9]+):([0-9]+):([0-9]+) OPTICAL_DUPLICATE_PIXEL_DISTANCE=100 VALIDATION_STRINGENCY=SILENT COMPRESSION_LEVEL=5    MAX_SEQUENCES_FOR_DISK_READ_ENDS_MAP=50000 MAX_FILE_HANDLES_FOR_READ_ENDS_MAP=8000 SORTING_COLLECTION_SIZE_RATIO=0.25 PROGRAM_RECORD_ID=MarkDuplicates PROGRAM_GROUP_NAME=MarkDuplicates REMOVE_DUPLICATES=false ASSUME_SORTED=false DUPLICATE_SCORING_STRATEGY=SUM_OF_BASE_QUALITIES VERBOSITY=INFO QUIET=false MAX_RECORDS_IN_RAM=500000 CREATE_INDEX=false CREATE_MD5_FILE=false	PN:MarkDuplicates",
				"@CO	q3BamUUID:ecc3c778-a77e-49b7-a4f5-fe828bcf9b88");
	}
	
	private List<String> getBamFile() {
		List<String> l = new ArrayList<>(getBamHeader());
		l.addAll(Arrays.asList(
"ST-E00180:52:H5LNMCCXX:3:2107:30506:52995	163	GL000249.1	38255	0	150M	=	38348	243	TATGATGAAACAGACATAACTAATTGTGGAGATCTGAGATATAATGAGGGCTTCTTTATAATAATAACAGAATAAATATTCGCCCTACAGGAAAAGTAAACACCAAGGAAATGTTTCAACATTCCTAGGTAGTAACAACCGTGTGCATTT	AAFFFKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKFAFKFKKKKKAFFAKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKAKKKKKKFK<FFFKKKKFFAFKFKFAFKFKFKKKKKK<AFFKK<A<	XA:Z:chr22,+28255366,150M,0;	MD:Z:150	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:0	AS:i:150	XS:i:150",
"ST-E00180:52:H5LNMCCXX:3:1213:28019:25798	83	GL000249.1	38291	0	150M	=	37888	-553	AGATATAATGAGGGCTTCTTTATAATAATAACAGAATAAATATTCGCCCTACAGGAAAAGTAAACACCAAGGAAATGTTTCAACATTCCTAGGTAGTAACAACCGTGTGCATTTATAAAATTCCAATTCTCCCCTCCTCCTAGATCTAAC	KKKFKKKKFKKKFKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFAKKKKKKKKKKKKKKKFFFAA	XA:Z:chr22,-28255402,150M,0;	MD:Z:150	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:0	AS:i:150	XS:i:150",
"ST-E00180:52:H5LNMCCXX:3:1201:9637:4825	83	GL000249.1	38311	0	149M	=	38035	-425	TATAATAATAACAGAATAAATATTCGCCCTACAGGAAAAGTAAACACCAAGGAAATGTTTCAACATTCCTAGGTAGTAACAACCGTGTGCATTTATAATATTCCAATTCTCCCCTCCTCCTAGATCTAACTGCCCATGTTACTTTCACT	,,,AKFFF7FA<FFFKAAAA,AFFKKKKKKFA,A,KKKFKF,KKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKFAKKKKKKKKKKKKKKKF,AF,KKKKKKKKKFFKKKKKKKFKKKKKKKKKKKFKKKKKKKKKK<KKF<A<AA	XA:Z:chr22,-28255422,149M,1;	MD:Z:98A50	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:1	AS:i:144	XS:i:144",
"ST-E00180:52:H5LNMCCXX:3:1216:1152:36997	1097	GL000249.1	38340	0	148M	=	38340	0	TACAGGAAAAGTAAACACCAAGGAAATGTTTCAACATTCCTAGGTAGTAACAACCGTGTGCATTTATAAAATTCCAATTCTCCCCTCCTCCTAGATCTAACTGCCCATGTTACTTTCACTTTAATAAAGAAGACATAAAAAAGAAACA	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKAKKKKKKKKKKAAFKKKKKKKKKKKKFKAKKKFFKKFKKKKKKKKFKFKFFFKKKAF7KKAKFK7FKKKKAAAKKKKKKFKK77FKA<,77AFKKKK7FAFFK	XA:Z:chr22,+28255451,148M,0;	MD:Z:148	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:0	AS:i:148	XS:i:148",
"ST-E00180:52:H5LNMCCXX:3:1216:1152:36997	133	GL000249.1	38340	0	*	=	38340	0	TTCGCCGAACTCCGCTCCCTCGCTATCAGTCCAAAAGAAATGTGTCCAGGGGCACTTAACTAGTTTGTCCCTCCAGCATGAGTCGTGGACCTGGCTCGGAAGAACACCTACGCACACAGGGACTAGCCGTCATGCCCCCAGTGTTCGG	,,<,,,,,,,,,,,(,(((,,((7,,7,,,,,7,,,,,,7,77,,,,,,,,((((,,7,,,,,,,,,7,,,,7(7,,7,,,F,,,,,,,,,,,,,,,,(,7,,,,,,,,,,,,,,,,,,,,,,,,,,<77(,,,,<,((,(,,,,,,,	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	AS:i:0	XS:i:0",
"ST-E00180:52:H5LNMCCXX:3:2121:28445:35116	73	GL000249.1	38340	0	148M	=	38340	0	TACAGGAAAAGTAAACACCAAGGAAATGTTTCAACATTCCTAGGTAGTAACAACCGTGTGCATTTATAAAATTCCAATTCTCCCCTCCTCCTAGATCTAACTGCCCATGTTACTTTCACTTTAATAAAGAAGACATAAAAAAGAAACA	A,FFFKKKKKKKKKKFKKKKKKKKKKKKKFKKKKKKKKKKKK7FFKAKKKKKKKKKAFKKKKAKKKKKKKKKKKKFKKFKKKKKKKKKKKKKKKAKFFKKKKKKKKFKFKKKKKKFFKKFKFF77AKFFKKKKKK<FFAFAKKKKF7F	XA:Z:chr22,+28255451,148M,0;	MD:Z:148	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:0	AS:i:148	XS:i:148",
"ST-E00180:52:H5LNMCCXX:3:2121:28445:35116	133	GL000249.1	38340	0	*	=	38340	0	AGTCGGGTTTATATCTGCCATGGAGGCACGTCTTTCGTCACGAGGTGTTGGTGAGATACATGGTAGTTGACAAGGCGAAGTTGTCTAGACGGAGGTATGAGCGTGGATCTGGGCGAGTACTGGGATAGTGTATACTGTGATACGGAGTC	,,,,<A<F,<,,7,,,7,,,,77,7,(,,(7,,,,,7,77,7,,7,,,,7,,7,,,,7,,,,7,,,,,A,,,,,,,,,,,,,,,,,,,,,,,,,,77,,,,,,,,,,,,,,<,((,(,,,,,<,,,,,,,,,,,,,,,,,,,,,,,,,,	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	AS:i:0	XS:i:0",
"ST-E00180:52:H5LNMCCXX:3:2112:21878:57073	147	GL000249.1	38341	0	148M	=	38026	-463	ACAGGAAAAGTAAACACCAAGGAAATGTTTCAACATTCCTAGGTAGTAACAACCGTGTGCATTTATAAAATTCCAATTCTCCCCTCCTCCTAGATCTAACTGCCCATGTTACTTTCACTTTAATAAAGAAGACATAAAAAAGAAACAA	A,<,AKKAAAKFKFKFKF<KAFKKAAKKF<KKFAFFAFKFAA<<KFKKKKKKKKKKKKKKKKFKKKKKKKKKFAKFKKA<FKKA7FKFKKKKKKKKKKKKKKAKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	XA:Z:chr22,-28255452,148M,0;	MD:Z:148	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:0	AS:i:148	XS:i:148",
"ST-E00180:52:H5LNMCCXX:3:2107:30506:52995	83	GL000249.1	38348	0	150M	=	38255	-243	AAGTAAACACCAAGGAAATGTTTCAACATTCCTAGGTAGTAACAACCGTGTGCATTTATAAAATTCCAATTCTCCCCTCCTCCTAGATCTAACTGCCCATGTTACTTTCACTTTAATAAAGAAGACATAAAAAAGAAACAAGTAGAATCT	FKF<KFKKFFKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFF<AAKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKFFFAA	XA:Z:chr22,-28255459,150M,0;	MD:Z:150	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:0	AS:i:150	XS:i:150",
	"ST-E00180:52:H5LNMCCXX:3:1212:12297:36856	1187	chrMT	18	60	134M16S	=	334	464	CCTATTAACCACTCACGGGAGCTCTCCCTGCATTGGGTATTTTCGTCTGGGGGGCATGCACGCGATAGCATTGCGAGAGGCTGGAGCCGGAGCACCCTAGGTCGCAGTATCTGTCTTTGACTCCAGCCTCATCCCAACATCTATCACCCC	<<<F,,AFKKKF,AAKK<A<7AAK,77,,7AFK<,<,FAFFK,,77A,F7,FFF,<K7A<<,(77,7F,<A,A,,,,7,7<,,AF<F,,<<<(7A<77F,<,,<7FKFK,,,,7AF,,AA,,AA,A<<,A,,,,,<,,,,,,A<<,,,,,	MD:Z:27A6T19T23C20T20T3T9	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:7	AS:i:99	XS:i:0",
	"ST-E00180:52:H5LNMCCXX:3:1212:12307:36874	163	chrMT	18	60	150M	=	334	464	CCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGGGTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTCCTGCCTCATCCCATTATTTATCGCACC	AAFFFKKKKKKKKKFKKKKKKKFKKKKKKKKKKKKKKKKAKKKKKKKKKKKAFFF,FFKKKKKKFKKKKKFKKFKFKKKKKKKKKKKFFFFKKFKFKFKKKKFAF7FAKAF<FAKKFFKFAFFAFKKFFFAFFKK7<A<FFK<AKAA7A<	MD:Z:134T15	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:1	AS:i:145	XS:i:81",
	"ST-E00180:52:H5LNMCCXX:3:1217:15555:27591	163	chrMT	18	60	150M	=	338	470	CCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGGGTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTCCTGCCCCATCCCATTATTTATCGCGCC	AFFFFKKKKKKKKKKKKKKKFFFKKFFKKKKFKKKKKKKKKK,FKKAKF77<AFFKKAKKKFKK<FAKKKKKFAAAA7K<FKAA<AAFFF(<7AA,AAAA7FKK7AFKFK7FFKKFKKF<7A<FAAAA((,,A<A<AF7,,A,,<AA,7<	MD:Z:128T5T12A2	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:3	AS:i:137	XS:i:86",
	"ST-E00180:52:H5LNMCCXX:3:2117:6684:18274	99	chrMT	18	60	150M	=	197	328	CCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGGGCATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTCCTGCCTCATCCCATTATTTATCGCACC	AAFAFFKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKAKKKKKKKKKKKKKKKKKKKKKKKKKKFFKKKKKKKKKKKKKKKKKKKKKKK<A<FKKKKKFKKKKKKKFAAKKKKKFKKKKKFKKKFFFKKKAAFKFK,FFKFAFKAKKKA	MD:Z:54T79T15	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:2	AS:i:140	XS:i:76",
	"ST-E00180:52:H5LNMCCXX:3:2211:13647:42077	99	chrMT	18	60	150M	=	260	391	CCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGGGTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTCCTGCCTCATCCCATTATTTATCGCACC	AAFFFFFKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKK<KKFKKKKKKKAFKKKKKKK<KK	MD:Z:134T15	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:1	AS:i:145	XS:i:81",
	"ST-E00180:52:H5LNMCCXX:3:2211:13515:42622	1123	chrMT	18	60	150M	=	260	391	CCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGGGTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTCCTGCCTCATCCCATTATTGATCGCACC	AAFFFKKFKKKKKKKKKKKKKK<F7FKK,FKKKKKKKKKKKKKKKAFFKKKFFK7FFKKKKKKKKKKKKKK,FKAKKKKKKKKKFFKKKFFKKKKKKKFAFKKKKFKKAFK,FKKKKKKKKKKKKKKKKKKKKKKKKKKKF,,FFFKFKK	MD:Z:134T6T8	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:2	AS:i:140	XS:i:85",
	"ST-E00180:52:H5LNMCCXX:3:2211:13738:42728	1123	chrMT	18	60	150M	=	260	391	CCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGGGTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTCCTGCCTCATCCCATTATTTATCGCACC	A<FFFKFKFKKKKKKKKKK<FFFFKKKKFFKKKKKKKKKAKKKKKKKKKFKFKKFKKKKKKKFKKKKKKKAKKKKKKFKKKKKK<KKKKKFKKKKKKKFFKFKKKAKFKAKKKAFKKKKFKFKKKKKKKFFA<AF,AFFFKFKFF<<,,<	MD:Z:134T15	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:1	AS:i:145	XS:i:81",
	"ST-E00180:52:H5LNMCCXX:3:2211:13697:42763^C	1123	chrMT	18	60	150M	=	260	391	CCTATTAACCACTCACGGGAGCTCTCCATGCATTTGGTATTTTCGTCTGGGGGGTATGCACGCGATAGCATTGCGAGACGCTGGAGCCGGAGCACCCTATGTCGCAGTATCTGTCTTTGATTCCTGCCTCATCCCATTATTTATCGCACC	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKAKKKKKKKFKAKFKK	MD:Z:134T15	PG:Z:MarkDuplicates	RG:Z:2ef8ccaf-2a09-4a9b-a5b3-8510b14dc8e8	NM:i:1	AS:i:145	XS:i:81"));
		return l;
	}
	private List<String> getBamFilechrX() {
		List<String> l = new ArrayList<>(getBamHeader());
		l.addAll(Arrays.asList(
		"ST-E00185:51:H5LNNCCXX:8:2214:22325:3383	83	chrX	2710692	60	149M	=	2710518	-323	TGACCTCTCAAAGTGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCGCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGT	,<,KKKKFF<7<A<7F,F<,FK<KKFFAF<KFFKKKKKKFK<KKKKKKFKFKFAKKKKFKKKKKKFFFKKKFKFAKKFFF<,KKKKKFA,KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKFKKKKKKKKKKKKKKKKFKKKFF<AA	ZC:i:1	MD:Z:48C99C0	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:2	AS:i:14	XS:i:41",
"ST-E00185:51:H5LNNCCXX:8:1215:25796:31477	1187	chrX	2710693	60	150M	=	2710955	411	GACCTCTCAAAGTGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGG	AA<FFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKFKKKKKKKK,FFFAF7<F<AKFAFKKKKAKFFKKKFAFKFFKKKFF<AFAFKKK<FFF,AFKAKK77F,,AA77<FKKF,,AFKAFK<777<<7,AFFKK7FK	ZC:i:1	MD:Z:147C2	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:32",
"ST-E00185:51:H5LNNCCXX:8:1215:25461:34272	1187	chrX	2710693	60	150M	=	2710955	411	GACCTCTCAAAGTGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCACAGCCGCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGG	AAFFFKKKKKKKKKKKKKKKKKKKFKKFKKKAKKKKKF7FKKKAAFKK<K7FKKK7AKKFFAAF7,<F7<FAA7A7AFKKKKFFKAA,,,F7FFK,(,,,,(7,,,,A,<,,,,<A,<,,7A,<<A<,7<<,,,,,<,A<,A7,<7<<,7	ZC:i:1	MD:Z:96G4T45C2	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:3	AS:i:13	XS:i:33",
"ST-E00185:51:H5LNNCCXX:8:1215:25481:34272	163	chrX	2710693	60	150M	=	2710955	411	GACCTCTCAAAGTGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGG	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKFKKKKKKKKKKKKKKKKFKKKKFKKKKKFFKKKKKKFFKKKKKKKKKKKKFAKK<KKFKFKKFKFKKKK<<<7<FKKKKKAKAA<	ZC:i:1	MD:Z:147C2	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:32",
"ST-E00185:51:H5LNNCCXX:8:2111:31582:58410	147	chrX	2710693	60	150M	=	2710533	-310	GACCTCTCAAAGTGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGG	77FA7A7A,,,,,7A<7FA77,,A7KKKKKKFF<,,KAFKFKFA<<KKFFFKKFF7FFFKKKKKFF,AFKKKAKAFFF7KKKKKKKKA,FKKKKKKA<KKKKKKAKKKKKKF7FFKKKKKKAKKFKKKKKKKKKKKKKKKKFKKKFFFAA	ZC:i:1	MD:Z:147C2	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:32",
"ST-E00106:156:H52NFCCXX:6:1207:32414:39001	83	chrX	2710700	60	148M	=	2710558	-290	CAAAGTGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCA	AFFFKKAFFAKFKKAKKKA<AFA<FFKKKKF<A7KKKKF<,KFFFKKAKFFFKAFKKKKKKKKKKFFKKKKFFFKKF7KFFAKKKKKKFFKKKKKKKKKKKKKFKKKKKKKKKKKKKKAFAFKFKKKAKKKKKKKKKKKKKKKFFFAA	ZC:i:0	MD:Z:140C7	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:27",
"ST-E00106:156:H52NFCCXX:6:1124:1964:22722	83	chrX	2710704	60	150M	=	2710544	-310	GTGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAG	FA<KKAA,AF7FFKKFKKFKF7KKFKKFKKFFKKKKKKFKKKKAKKKKKKKKKKKFKKKKKKKKKKFFKFKKKKA<AKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKAKKKKKKKKKKKKFKKKKFKKKKKFFFAA	ZC:i:0	MD:Z:136C13	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:24",
"ST-E00106:156:H52NFCCXX:6:1201:11891:73247	83	chrX	2710704	60	150M	=	2710369	-485	GTGCTGGGATTACGGATGTGAGCCACTGAGCCCCGCGCTAGGAAACACCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAG	7,,,,,<,,,,,,,,,,7,7,,KFAF,,(77A,(<,,<<,7,,,7A7,,7AFA,,,FKFAA,7<,7,,<,F7KK7A,,<F<AAAA,7,KF7<FA7A<,<,KFAA77<<<F7,AKKF7FF,7AFK,KAAKAKFFFFAF,AFF<A,<FF,<,	ZC:i:0	MD:Z:13A19A2C10G88C13	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:5	AS:i:125	XS:i:31",
"ST-E00106:156:H52NFCCXX:6:2211:7902:34026	1107	chrX	2710704	60	150M	=	2710544	-310	GTGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAG	,KFA<FKK<KKKKKKKKKKFAFKKFKKA7AKFKKFKKKKKFKKKKKKKKKKKKKKFKKKKKKKFFKKK<7KKKKFKFFKKKKKKKKFKKKKKKKKKKKKKKKKKKFKKKFKFAFFKKKKFKFAKKKFAKAKKKKKFKKKAKKKKKFFFAA	ZC:i:0	MD:Z:136C13	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1110:13251:31125	99	chrX	2710705	60	150M	=	2711097	542	TGCTGGGATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGG	AAFFFKF<AFKKKKKKFFKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKF7FKFAFKKFFAFKKKFKKAKKFKFKKFKKKKKKKKKKFKFKKKKKKKKKAAAKFKFKFFKKKKKKFA<FA,7AKFAFKKKFFKFKKF,<F7AA<,AFF	ZC:i:0	MD:Z:135C14	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:36",
"ST-E00185:51:H5LNNCCXX:8:2109:15118:26308	99	chrX	2710711	60	150M	=	2710932	371	GATTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACT	A<FFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFKKKKKKKKKKKKFKKKFKKKKKKKKKKK7KKKKKKFFKKFKK,AKKKKKKKKKKKKKKKKFKKKKKKK,<AAKAFF7AFF7F,AFFKF,7<	ZC:i:1	MD:Z:129C20	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:29",
"ST-E00106:156:H52NFCCXX:6:2124:16225:19223	1187	chrX	2710713	60	149M	=	2710989	425	TTACAGATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTC	AAAFFF<FFKKKKKKKKKKKKKAFF(A,<FKKKKKKKKKAFKKKKKFKFFKKKKK7KFFFFA7FKKKKKAFA<F<F<AFFAAAAAFKKFKKKAK<FKA<7<7A,,<AA<F<<,<,AA,<<AK77FFKKFKK77A7<,,<A7A,A77(,,	ZC:i:0	MD:Z:127C21	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:27",
"ST-E00106:156:H52NFCCXX:6:2124:15880:19258	163	chrX	2710713	60	149M	=	2710989	425	TTACAGATGTGAGCCACTGAGCCCAACCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCCCTC	AAFFFFKKKKKKKKKKKKKKKKKKK,FFKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKFKKKFFKKKKKKKFKKKKKKKKKKKFKKKKKKKKFKKKKFKK<AKKFF7AKAFFKKKKFKKFKAFAFAA,A<FFFKK<AF<K<(,AF	ZC:i:0	MD:Z:25G101C17A3	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:3	AS:i:135	XS:i:26",
"ST-E00185:51:H5LNNCCXX:7:1118:21584:56282	99	chrX	2710719	60	150M	=	2710946	376	ATGTGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACC	AAFFFKKKKKKKFKKKKKKKKFKKFKKKKKKKKKKKKKKKKKKKKKKKFFAKKKKKAFFKFFFKKKKKKKFKKKKKKKKFKKKKFKKKKKKKKKKFKKKKKKKFKKKKKKAAFK,,AFFFKKKKKFF<AKFKKKKKKKK<7FFKKFFFFK	ZC:i:2	MD:Z:121C28	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:1	AS:i:14	XS:i:22",
"ST-E00185:51:H5LNNCCXX:8:2208:18397:62646	83	chrX	2710722	60	150M	=	2710587	-285	CGAGCCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGC	(AKKFKKKKKKAKKKFKFFKKKKKKKKKF7KFKKKKKKKKKKKKKKKKKKKFKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:1	MD:Z:0T117C31	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:2	AS:i:14	XS:i:20",
"ST-E00185:51:H5LNNCCXX:8:2213:6268:55526	163	chrX	2710726	60	150M	=	2711040	464	CCACTGAGCCCAGCCCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTC	AA<FFKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKFKKKKKKKKFKKKKKFKKKKKFKKKKKFFKKKFAFKKKKFKAA,AAK7AAKAK7<<<AA,77,7AKAKKFKKKF<77F<	ZC:i:1	MD:Z:114C35	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:20",
"ST-E00185:51:H5LNNCCXX:7:2103:4887:49514	121	chrX	2710734	60	150M	=	2710734	0	CCCAGCTCTCGGAAACAGCCTGTTAAGCCAAGTGCATTCCGTCTGGACCTCCCCAGAGCCTCTGTGCATGCTGCCTCTACTTCAGCCTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCT	7,7,,,,,,,,,,<AAFF,A77,,,<A,,,<<,A7,,,,,,,,,7,7(,<7,7KA,AFA7,,,FAFF,7,,A,F7A,,,7,KA,7,,<<F7,KFA7,7FKKKFF7KFAFAKF<FKFKKAKFAA<,KKKFA7F<<<KKKKKAKKKKAFFA<	ZC:i:2	MD:Z:6C2A11C14A1G0A0C3T2A24A4G8T19C43	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:13	AS:i:85	XS:i:20",
"ST-E00185:51:H5LNNCCXX:8:1216:2715:12912	99	chrX	2710740	60	148M	=	2710901	311	CCTAGGAAACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGG	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK7FFKKKKKKKKKKKKKKKKKKKKFFKKKFKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKFKFKFKFKKKKFKAAFKKKKKKKKKKKA<FKAFKFFFKA7FKKF	ZC:i:1	MD:Z:100C47	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:2213:32292:4192	147	chrX	2710744	60	150M	=	2710439	-455	CGAAACAGGCTATTAAGCCAAGTGCCATGACTCTTGAACTCCCCAGCGCCTCTGTGCATTCGACCTCGACGTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTC	,,,<,,,,,,,,<A<,,<<FAAA,A7A,,,AA7<A7AAKAKAAFA7(KKKF7A7KFFA<,7AKKKKKF<A,KKKFF<<7K7KKFKKFKKKKFAKFKKKFKKKFFKFF7<<FKKKFAKKKKKKKKKKFKKKKFKKKKKKKKKKKFKFFFAA	ZC:i:1	MD:Z:0G7C2C13A20A12G1T8T25C53	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:9	AS:i:109	XS:i:20",
"ST-E00185:51:H5LNNCCXX:7:2207:3811:58621	1187	chrX	2710748	60	150M	=	2710958	359	ACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACGGGCAGTCCCAAAGCCCTGGAGAGTCTGTG	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKK<FKKAKFKKKKFKK<FAK<KKK,<FFKKKKKKKKKKKKKFAFKKFFFKKK7F<7AF,7<FAK<7F<<<FFKKF<,<,,(,AF,,,,AF,,,,77A<,<,,,,,A	ZC:i:2	MD:Z:92C27C14T14	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:3	AS:i:135	XS:i:19",
"ST-E00185:51:H5LNNCCXX:7:2207:4745:58902	163	chrX	2710748	60	150M	=	2710958	359	ACAGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTG	AAAFFKKKKKKKKKKKKKKKKKKKKF,FKKKKFKKKKKKKKFFKKKKKKKKKKKKKKKAKKKKKFFFKKKKKFKFFKKKKKKKKKKKKKKKKKKAFAAFKFF,A<AKKKKAKKF<AFFAKKFKFFFFFFKKKKKA,AAF<AA7A,,,AFF	ZC:i:2	MD:Z:92C57	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:2123:27187:4034	83	chrX	2710750	60	150M	=	2710390	-510	AGCCTCTTAAGCCAAGTGCAATGCCTCTTGACCTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCT	KKKFFFAA7FKKFF<A77A<,FA,FAAKFKA<(<KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKFFFAA	ZC:i:1	MD:Z:23A7A58C59	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:3	AS:i:13	XS:i:20",
"ST-E00185:51:H5LNNCCXX:8:2212:26679:30404	147	chrX	2710750	60	150M	=	2710550	-350	AGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCT	KKKKFFFAFKA7KKKAFAF<<,<FKKKKA7A,KKKKKFKKKKKKKKKKKKFAFFKFFKKKKKKKKKKKKKKKKAFKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:1	MD:Z:90C59	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:2218:12002:5088	1171	chrX	2710750	60	150M	=	2710550	-350	AGCCTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCT	KKKKKKKKFKKKKKKKKKKKKKA7KKKKKFF<KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:1	MD:Z:90C59	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1120:31703:68149	99	chrX	2710753	60	148M	=	2711164	559	CTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTG	AAFFA<KKFKFKKKKKKKAKKKKKKKKKKKKAFFFKKKKKKFFKFKKKFKKKKKKKKKKKKKKKKKKFKKKKKKKKFA7,AAFKKFF7FFFAKKKKKAAFKKKKKA<<KFFKAFKKKKKFKFFK7A,FKK7AAFFKK,<<7FKKKF,7	ZC:i:0	MD:Z:87C60	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:1113:20173:72755	163	chrX	2710753	60	148M	=	2710855	252	CTCTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGGCTGTGCTG	AFFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKFKKKKKKKKKKKKKFKKKKKKKKAFFAFKKKKKKKKKKKKKFKKAAFKKKKKKKKKKKKKKKFFKKFKKFF7A,,7,A,,AAFFFAK	ZC:i:1	MD:Z:87C51T8	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:2	AS:i:13	XS:i:20",
"ST-E00106:156:H52NFCCXX:6:2116:8389:67288	83	chrX	2710755	60	148M	=	2710574	-329	CTTAAGCCAAGTGCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAG	F77KKKKKKKKKKKFFKF7KKKKKKKAKKKKKKKKFKKKAKKKKKKKFKKKAKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:0	MD:Z:85C62	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2122:7800:26783	83	chrX	2710767	60	150M	=	2710572	-345	GCAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGT	KKFAAA,KKFKKA<7A,KKKKFFAFKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:0	MD:Z:73C76	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2220:2735:56124	83	chrX	2710768	60	150M	=	2710613	-305	CAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGGAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTT	KKFA,,A<KKKF<,FAKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:0	MD:Z:72C30C46	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:2	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2220:2197:56810	1107	chrX	2710768	60	150M	=	2710633	-285	CAATGACTCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTT	KFFFFFF<KKKF<7KFKFFKFKFKKKKKKKKKKKAKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:0	MD:Z:72C77	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2122:24538:9361	99	chrX	2710775	60	149M	=	2710958	333	TCTTGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCA	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKAKKFKAKKKKFKFKKKKKKKKKKKF7AAFFFKKKKFKKKKKKKK	ZC:i:0	MD:Z:65C83	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:1116:13312:17377	163	chrX	2710778	60	150M	=	2711149	520	TGAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGCGAGTCTGTGCTGAGGGAATGGTCCCCGTTACAGCAAAGC	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKFKFFAKKKKKKKKKKKKKKKKK7FKFAKAFKKKKFFKKFAKKKFKKK<FAAFKKKAFKAK7FK,7,,,A<FF77,A,AA,,,,AAFKF,(((,A,,,,,,,<,	ZC:i:1	MD:Z:62C47A24A0T4A8	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:5	AS:i:125	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1101:5912:59974	147	chrX	2710778	60	149M	=	2710587	-340	TGAACTCCCCAGAGCCTCTTGTCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAG	AA,,7,A<,,,,,<<,,,,,7,KKKFAA,FAF7A,FKAA7FKKKA,7,,,,<,7AAFKKKKFKKF<<FFKKKA7AA<KKKKKKKKFFKKKKKKKKKKKKKKKKKKFKKKKFKKKKFKKKKKKKKKKKKKKKKFKAKKKKKKKKKFFFAA	ZC:i:0	MD:Z:19G0T0G40C86	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:4	AS:i:129	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2212:24284:24216	147	chrX	2710779	60	149M	=	2710571	-357	GAACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGC	,,,,,7A,,,A,AFA7A,,KFA<,,FKAKKKKKFKAFKKFKKKFFKF<F<AKKKKKKKKKKKKKKKKKKKKFFKFFFKKKKKKAKKKKKFAKKKKKKKKKKKKKKKKKKKKKKKKKKAKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:0	MD:Z:61C87	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2223:24213:2328	1107	chrX	2710780	60	150M	=	2710546	-384	AACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAG	<AKFKKKKKKFKKKKKFKF<KKFFKFFKKKKKKKKKKKKKAKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFAKKKKKKKKKFKKKKKKKKKKKKKKKKFKKKKKFFKKKKKKKKKKKFFKKKKKKFKKKKKKKKKKFAFFKKFAFA<	ZC:i:0	MD:Z:60C89	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:7:1214:16844:57073	83	chrX	2710780	60	150M	=	2710546	-384	AACTCCCCAGAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAG	<,A<FKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:2	MD:Z:60C89	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1215:30587:72614	99	chrX	2710789	60	150M	=	2711036	374	GAGCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGCATGGTCCATGTTAAAGCAAAGCAGAACAACCAC	AA<FFKKKKKKKKFKKKKKKKFK7KFKKKKKKKKFKKKKKKKKKFKKKKKKFKKKKKKKKFAAKKKKKAFFFKKKKKKKKKKK<FKKKKKK,FKKKKKFFK,77FAKAFKKK7AKF(<,AA,AAFK7F<A<AAKKKKKKKKKA,<FAAAA	ZC:i:0	MD:Z:51C64A33	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:2	AS:i:14	XS:i:22",
"ST-E00185:51:H5LNNCCXX:8:2115:22315:72386	81	chrX	2710791	60	150M	chr7	57700768	0	GCCTCTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTC	,,7AFAAAA,<AFKF7KFAFA,KKKKF<<,,FAKAFAKKKKAFAFKKFKAA,KFAKKKKKFKKFFA7,F7KAKKKKKKKKKKKKKKKFKKKKKKKKKKKKFAFKKKKK7FKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKA<FAA	ZC:i:1	MD:Z:49C100	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:145	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1217:21482:60677	83	chrX	2710795	60	148M	=	2710560	-383	CTGTGCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTG	KKKKFKKKKKKKAA7KKKKFFKKKKKKKKKFAKKFFFKKKKKKKKFKKKKKFKKKF7AKKKKKKKFKFKFKKAKKKKKKKKKKKKFFKKKFKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKFKFFKKKKKKKKKKKKFKFKKFFFAA	ZC:i:0	MD:Z:45C102	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1117:9059:44152	147	chrX	2710799	60	149M	=	2710546	-402	GCATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTT	AF<KKKA<FAFF<<FAAAFFF<KKA<<KKFF<KKKKKKKKKKKF<<KKKKKKKKFAKKKFKKKFFFFFKFKKKKKFKKKKKKKKKKFFKKKKKKKKKKKKKKKKKFKKKKKKKFKKKKKKKAKKAKKKKKKKKKKKKKFKKKKKFFFAA	ZC:i:0	MD:Z:41C107	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:2208:4034:64492	83	chrX	2710799	60	150M	=	2710395	-554	GCATGGTACCTCGACTTCAGCTTCACAGATGTATGCAAATGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTT	7,,<,,7KKKKAA7<,,<,,AAFA,<FA<,7AA7<<<,KKKKFKFFF7KKKKA,,FAA7KKFFAKKKFFKKKKF,AF<F7KKKFKKKKFKKKKKKKKFKKKKKKKKKKKKKKKFKKKFFKKKKKKKKFKKKKKKKFFF<KKFKKFFFFAA	ZC:i:1	MD:Z:5C32C2C108	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:3	AS:i:13	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1101:5922:59781	1171	chrX	2710800	60	22S127M	=	2710587	-340	TGACTCCCCCGGGGCCTCTTTTCATGCTACCTCGTCTTCAGCTTCACAGTGGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAG	,,,((,,((,(((,<,,,7<,,,7FAA,,<,7,,,7<,,,<,,,777,,,,<A,KAA<A<,,7,,,KF7,FKF<A,7,7<F7<,FAKKFFAFKA7A<7FA,<,,,AKKKKF,,F7,FKKKFKKKFFFAKFF7KFAKK77,FF7F7,7,,	ZC:i:0	MD:Z:12A14A0T11C86	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:4	AS:i:107	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2116:19655:71683	99	chrX	2710801	60	150M	=	2710953	301	ATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGT	AAAFFKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFKKFFAFFKKKFFKKKAFKKKFKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKAFKKKKKKKKKKKKKKKKKKFKFKKKKKFKKKAAAFFAFFFFFKKK	ZC:i:0	MD:Z:39C110	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2215:23705:72544	1107	chrX	2710801	60	150M	=	2710590	-361	ATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGT	7F7,<<FF7AA,FA,,,FFKFA<<,<<<A7AKKKKFK<A,,<F7KKFAKAA,<7,F<A,,FFFFAFKFKKKFFKKKKKFAA7<7FKKKKKKFKKFKKKFKAFFKKKAFF<7AKAF<<F,AKKKKKFFFFKKKKKKF<KKKKKKKKFAF<A	ZC:i:0	MD:Z:39C110	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2215:23472:72632	83	chrX	2710801	60	150M	=	2710590	-361	ATGCTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGT	K<FA<AKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKF<<KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:0	MD:Z:39C110	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:7:2120:7678:65881	147	chrX	2710802	60	3S147M	=	2710538	-411	GACTGCTACCTCTACTGCAGCGTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTT	,,7AAA,,,7,,,,<,(7,,,,,,,<7FKKAF<A<,,AF7KAA7A,,<,,F<,(AFFFF<KKKKA7K<AAKFKFFKKKF7FKFKKKKKKKKKKKKAKKKKKKKFFKKKKKFKKKKKKKKKKKKKKKKKKFFKKKKKKKFKKKKKKFFFAA	ZC:i:2	MD:Z:9G3T4T19C108	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:4	AS:i:127	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2107:13251:52960	99	chrX	2710804	60	149M	=	2711018	363	CTACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGTGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTAT	AAF<FKFKFKKKKKKKA<7FAFKKFKFAFKKKAKKKKKKKFKKKFKKKKKKKKKKF7FKKFFKKKKKKFFFKKKKKAAKFKKKKKA,A7AKFKKKKKFKFKFKKF7<FA7FFKFFKKKKKA<FFFK<FKKKKKFK7AAFFFFKFKFA,A	ZC:i:0	MD:Z:36C49A62	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:2	AS:i:13	XS:i:20",
"ST-E00185:51:H5LNNCCXX:8:2123:22335:58955	163	chrX	2710805	60	150M	=	2710964	308	TACCTCGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTT	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKFKKKKKKKF<FKFKKKKKKKKKKK<FKKKKFFAFKKKKFKKF7<AFKFKKAA<FKKK,<,AF,AFK<F<A,AAF	ZC:i:1	MD:Z:35C114	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2215:7120:36083	147	chrX	2710806	60	148M	=	2710532	-422	ACCTCCACTTCAGTTTCCCAGATGTATTCAACTGTGGGATCAGGTTAGGGGCACTCACTTCCCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCCCTCTGCCTTTTGTATT	<<,<<,,,,,,,,,,7,,,,,,F7<7,,7,,A,,,F<FA7A7A,,A(F<A<<AA,<,FA,(FFA,FA,7F<,,<,,F777FFFA,,AA<,,,7<A77FFF<<7,<F,7FKKKKKAKKKFFFAKKKFKA7F,,FKKAKKKKKF<<FAAA	ZC:i:0	MD:Z:5G7C3A9G6C25A70A16	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:7	AS:i:113	XS:i:21",
"ST-E00106:156:H52NFCCXX:6:1217:24588:11558	163	chrX	2710810	60	150M	=	2710989	329	CGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATAC	AAFFFKFKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKFKKKKKKKKKFKKKKKKKKKKKKKKFKAKKKKKKKKKAFKKK<FAFKKKKKKKKFKK7A<AKF<7FFAFKKFFF<<7A<77<AKKFK<A,,A<7AF<A7AKKK7<F<A<F<A	ZC:i:0	MD:Z:30C119	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2221:21188:14529	147	chrX	2710810	60	150M	=	2710542	-418	CGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGATTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATAC	,A,AA,,,7KA<<7A,,,7<,,F7A,7<KFF<,,<A,,,,A7(,,,<F<KKF<KKF7AA7<KKKFFAFFFKKFFAFKKFAAFKA<,<,FKF<A<FAFKFKFFFAFFKFKAAA<7<AAKAAF,A,,<A,FKFF<<7KKKKFKFKKKFAFAA	ZC:i:0	MD:Z:30C8G110	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:2	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:7:2124:7506:66321	147	chrX	2710810	60	149M	=	2710577	-382	CGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATA	AF77FAA<AKK7F<,,KKFKKFKKKAFFFA,AFFFA7AA7,AAAA77AAKKKKF<A,KFK<KAKKKKKFAKKFAKKKF7FAKKKKKKFFKKKKKFFKF7FAKKKKKKKKKKKKKKKKKKKKKKKKFAF<AFKKKFKKFKFKFKFAFFA,	ZC:i:2	MD:Z:30C118	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:7:2207:9729:9413	1171	chrX	2710810	60	149M	=	2710577	-382	CGACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATA	,7A7<K<<FFFAF<FAA<<AA<AA7,AA,FKKF<FFKFF<FA7<(<A77A,KKKFFAKAA,AFKKFAFKFKAFAAFKAKFFFAKAKKFKFA,7AKKKF,,7<AFAF<7FFAKFKKAFKKFKKFKA<AA7AA<<77KA,FK77AAFAF<A	ZC:i:2	MD:Z:30C118	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:1	AS:i:144	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1202:26030:6108	99	chrX	2710811	60	150M	=	2711107	446	GACTTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACT	AAAFFKKKK,FKKKKKAKKKKKKKKKKKKKKKKKAAKKKKKFKKKKKKKKKKKKKKKFKKKFKKKKKKFFKKAKFFFFKAKFFKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKF<77A7AFFFKAFAA<,AA<7	ZC:i:0	MD:Z:29C120	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:2215:7820:71753	99	chrX	2710814	60	150M	=	2710967	302	TTCAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGT	AAFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKFKKKKKKKKKKAKKKKKKKKKKKKKKKKKA7FF7FKKKF<FKFFKFKKKKF<	ZC:i:1	MD:Z:26C123	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1119:21807:2451	99	chrX	2710816	60	150M	=	2711122	455	CAGCTTCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTT	AAFFFKKKKKKKKKKKKKKKKKKKKKKKFKKFFKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKFFFKFFK<AKFKKKKKKK,AFK77FKKKKKKAKKKFKKKKFF,AFKKFKKKKKKAKFKAFKKKKKKKKKKFKKAF7<AFKKKK	ZC:i:0	MD:Z:24C125	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:2115:26324:34008	83	chrX	2710821	60	148M	=	2710477	-492	TCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTT	KFFKKKKFKFKKKKKKKKKKKKKKFKKKKKKFKKKKKKKFKKKKKKKKKKKKKKKKKKKFFKKKKKKKKKKFFKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:0	MD:Z:19C128	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:7:2119:14266:24022	83	chrX	2710821	60	148M	=	2710587	-382	TCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTT	FA7AF<<FKA<AFF<,,7F7FFKKKKKF<A7FAA,A<AFFKKF<AAKKKKKKKFKKKKKKKKKKKKKKKKKFKFA,KKFKKKKFKKKKKKKKKKKKKKKKKKKKKKFKKKKK7FKFFKKKKKKKFKKKKKKKKKKKFKKKKKKFFFA<	ZC:i:2	MD:Z:19C128	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:1111:19107:15531	147	chrX	2710821	60	148M	=	2710619	-350	TCACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTT	AKFFKKKKF<<KAKFAAFKFKAKAF<KKKKFFKKKKKAKAA<<AF7FFKKKFFKKKKKKKFKKKKKKKKKKKFFKKKKKKKKKKKKKKKKKFKKKKKKKKKKFFKKKKKKKKKKKFAKKKKKKKFKKKKFFKKKKKKKKKKKKFFFAA	ZC:i:1	MD:Z:19C128	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:19",
"ST-E00106:156:H52NFCCXX:6:1218:15778:17746	163	chrX	2710822	60	150M	=	2711107	435	CACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTTTTT	AAFFFFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFKKKKKKKKKKKKKKKKKK7FAKFAKKKFKFKKKKKKKKKKKKKKKKFFF7FFKFFKKKKKKKK7AAAFA7FKFKFF7<7<,,<FKKKKKAAAAKAAAKKKKAA<KKK<F<F	ZC:i:0	MD:Z:18C131	PG:Z:MarkDuplicates	RG:Z:6cb07878-a642-4290-bb26-0589eafb426d	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:2210:2735:23073	83	chrX	2710822	60	149M	=	2710576	-395	CACAGATGTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTTTT	F<FF<FA77<AF7A,77F<F<KFAKFKKFA,KKKFFFKKKKKKFA<FKKKKK<KKFFKKKFFKKKKKKAKKKFKKF7AKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKAKFKKKKKKKKKKKKKKKKKKKKKKKKKKFFFA<	ZC:i:1	MD:Z:18C130	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:1	AS:i:14	XS:i:19",
"ST-E00185:51:H5LNNCCXX:7:1117:21462:26923	147	chrX	2710829	60	150M	=	2710620	-359	GTATGCAACTGTGGGATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTTTTTTGAAACA	FKKFKKKFF,A<<KAKKKKKKKKKKKKKKKKFKKKKKKKKK<KKKKKKKKKKFKKKKKFKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKFFAAA	ZC:i:2	MD:Z:11C138	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:1	AS:i:14	XS:i:20",
"ST-E00185:51:H5LNNCCXX:7:2211:32069:70048	147	chrX	2710842	60	18S132M	=	2710558	-416	CATATTTATTCAACTTGTGGATCAGTTTAGGGGGCCTCACTTACCGGCAGTCCCAAAGCCCTGGAGAGTCTGTGCTGAGGCAATGTTCCATTTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTTTTTTT	,,,,7,,,,,A7,,,,,,77,,7,,,,,,((,(,,,,,7FA,7KAA,,,,AKF<KFA,7,A<A,,AFA,A,7F7A,7F7,,AA,<,,A<,A,KKF7F,KKKAAA7KKKKF7F,,<,KKFKKKKKKKKKKKKKK7KKKK7KKKKKKFFAAA	ZC:i:2	MD:Z:7G7C0A24T20G4G5G57G0	PG:Z:MarkDuplicates	RG:Z:0d86f75a-d723-4018-8e56-9f351f4af76d	NM:i:8	AS:i:96	XS:i:19",
"ST-E00185:51:H5LNNCCXX:8:2116:26172:68518	83	chrX	2710843	60	150M	=	2710602	-391	GATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTTTTTTGAAACAGGGTCTCTCTGTGT	<KKKFAA,,,F<7AKFKFKKK<FKKFKKKK7AKKF7KKKKFKFKKKKKFFKKKAAKFKKKKKKFAKF7KKFFKKFKKKKKKKKKKKKKKKKAKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKFKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:1	MD:Z:150	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:0	AS:i:15	XS:i:27",
"ST-E00185:51:H5LNNCCXX:8:2116:26172:68519	83	chrX	2710993	60	150M	=	2710602	-391	GATCAGGTTAGGGGCACTCACTTACCGGCAGTCCCAAAGCTCTGGAGAGTCTGTGCTGAGGGAATGGTCCATGTTAAAGCAAAGCAGAACAACCACTCTGCCTTTTGTATTTTATACTTGTTTTTTTTTTGAAACAGGGTCTCTCTGTGT	<KKKFAA,,,F<7AKFKFKKK<FKKFKKKK7AKKF7KKKKFKFKKKKKFFKKKAAKFKKKKKKFAKF7KKFFKKFKKKKKKKKKKKKKKKKAKKKKKKKKKKKKKKKKKKKKKKKFKKKKKKKKKFKKKKKKKKKKKKKKKKKKKFFFAA	ZC:i:1	MD:Z:150	PG:Z:MarkDuplicates	RG:Z:32454611-7c1e-495f-9e8e-21fef832390a	NM:i:0	AS:i:15	XS:i:27"));
				
		return l;
	}
	
	@Test
	public void getExistingVcfHeader() throws Exception {
		// create an actual GATK vcf header
		List<String> header = new ArrayList<>();		
		  header.add("##fileformat=VCFv4.1");
		  header.add("##qUUID=5072595e-8aa2-42df-b5da-dc90a5f2e057");
		  header.add("##FILTER=<ID=LowQual,Description=\"Low quality\">");
		  header.add("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		  header.add("##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth (reads with MQ=255 or with bad mates are filtered)\">");
		  header.add("##FORMAT=<ID=GQ,Number=1,Type=Integer,Description=\"Genotype Quality\">");
		  header.add("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");
		  header.add("##FORMAT=<ID=PL,Number=G,Type=Integer,Description=\"Normalized, Phred-scaled likelihoods for genotypes as defined in the VCF specification\">");
		  header.add("##GATKCommandLine=<ID=HaplotypeCaller,Version=3.3-0-g37228af,Date=\"Tue Feb 03 06:10:18 EST 2015\",Epoch=1422907818839,CommandLineOptions=\"analysis_type=HaplotypeCaller input_file=[/mnt/genomeinfo_projects/data/oesophageal/OESO_2804/seq_final/SmgresOesophageal_OESO2804_1DNA_7PrimaryTumour_OESOABNW2012061454TD_IlluminaIGNOutsourcing_NoCapture_Bwa_HiSeq.jpearson.bam] showFullBamList=false read_buffer_size=null phone_home=AWS gatk_key=null tag=NA read_filter=[] intervals=[/mnt/genomeinfo_projects/data/oesophageal/OESO_2804/seq_final/SmgresOesophageal_OESO2804_1DNA_7PrimaryTumour_OESOABNW2012061454TD_IlluminaIGNOutsourcing_NoCapture_Bwa_HiSeq.jpearson.bam.queue/HC_C_OESO_2804-1-sg/temp_01_of_25/scatter.intervals] excludeIntervals=null interval_set_rule=UNION interval_merging=ALL interval_padding=0 reference_sequence=/opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa nonDeterministicRandomSeed=false disableDithering=false maxRuntime=-1 maxRuntimeUnits=MINUTES downsampling_type=BY_SAMPLE downsample_to_fraction=null downsample_to_coverage=250 baq=OFF baqGapOpenPenalty=40.0 refactor_NDN_cigar_string=false fix_misencoded_quality_scores=false allow_potentially_misencoded_quality_scores=false useOriginalQualities=false defaultBaseQualities=-1 performanceLog=null BQSR=null quantize_quals=0 disable_indel_quals=false emit_original_quals=false preserve_qscores_less_than=6 globalQScorePrior=-1.0 validation_strictness=SILENT remove_program_records=false keep_program_records=false sample_rename_mapping_file=null unsafe=null disable_auto_index_creation_and_locking_when_reading_rods=false no_cmdline_in_header=false sites_only=false never_trim_vcf_format_field=false bcf=false bam_compression=null simplifyBAM=false disable_bam_indexing=false generate_md5=false num_threads=1 num_cpu_threads_per_data_thread=1 num_io_threads=0 monitorThreadEfficiency=false num_bam_file_handles=null read_group_black_list=null pedigree=[] pedigreeString=[] pedigreeValidationType=STRICT allow_intervals_with_unindexed_bam=false generateShadowBCF=false variant_index_type=DYNAMIC_SEEK variant_index_parameter=-1 logging_level=INFO log_to_file=null help=false version=false out=org.broadinstitute.gatk.engine.io.stubs.VariantContextWriterStub likelihoodCalculationEngine=PairHMM heterogeneousKmerSizeResolution=COMBO_MIN graphOutput=null bamOutput=null bamWriterType=CALLED_HAPLOTYPES disableOptimizations=false dbsnp=(RodBinding name=dbsnp source=/opt/local/genomeinfo/dbsnp/135/00-All_chr.vcf) dontTrimActiveRegions=false maxDiscARExtension=25 maxGGAARExtension=300 paddingAroundIndels=150 paddingAroundSNPs=20 comp=[] annotation=[ClippingRankSumTest, DepthPerSampleHC] excludeAnnotation=[SpanningDeletions, TandemRepeatAnnotator] debug=false useFilteredReadsForAnnotations=false emitRefConfidence=NONE annotateNDA=false heterozygosity=0.001 indel_heterozygosity=1.25E-4 standard_min_confidence_threshold_for_calling=30.0 standard_min_confidence_threshold_for_emitting=30.0 max_alternate_alleles=6 input_prior=[] sample_ploidy=2 genotyping_mode=DISCOVERY alleles=(RodBinding name= source=UNBOUND) contamination_fraction_to_filter=0.0 contamination_fraction_per_sample_file=null p_nonref_model=null exactcallslog=null output_mode=EMIT_VARIANTS_ONLY allSitePLs=false sample_name=null kmerSize=[10, 25] dontIncreaseKmerSizesForCycles=false allowNonUniqueKmersInRef=false numPruningSamples=1 recoverDanglingHeads=false doNotRecoverDanglingBranches=false minDanglingBranchLength=4 consensus=false GVCFGQBands=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 70, 80, 90, 99] indelSizeToEliminateInRefModel=10 min_base_quality_score=10 minPruning=2 gcpHMM=10 includeUmappedReads=false useAllelesTrigger=false phredScaledGlobalReadMismappingRate=45 maxNumHaplotypesInPopulation=128 mergeVariantsViaLD=false doNotRunPhysicalPhasing=true pair_hmm_implementation=VECTOR_LOGLESS_CACHING keepRG=null justDetermineActiveRegions=false dontGenotype=false errorCorrectKmers=false debugGraphTransformations=false dontUseSoftClippedBases=false captureAssemblyFailureBAM=false allowCyclesInKmerGraphToGeneratePaths=false noFpga=false errorCorrectReads=false kmerLengthForReadErrorCorrection=25 minObservationsForKmerToBeSolid=20 pcr_indel_model=CONSERVATIVE maxReadsInRegionPerSample=1000 minReadsPerAlignmentStart=5 activityProfileOut=null activeRegionOut=null activeRegionIn=null activeRegionExtension=null forceActive=false activeRegionMaxSize=null bandPassSigma=null maxProbPropagationDistance=50 activeProbabilityThreshold=0.002 min_mapping_quality_score=20 filter_reads_with_N_cigar=false filter_mismatching_base_and_quals=false filter_bases_not_stored=false\">");
		  header.add("##INFO=<ID=AC,Number=A,Type=Integer,Description=\"Allele count in genotypes, for each ALT allele, in the same order as listed\">");
		  header.add("##INFO=<ID=AF,Number=A,Type=Float,Description=\"Allele Frequency, for each ALT allele, in the same order as listed\">");
		  header.add("##INFO=<ID=AN,Number=1,Type=Integer,Description=\"Total number of alleles in called genotypes\">");
		  header.add("##INFO=<ID=BaseQRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt Vs. Ref base qualities\">");
		  header.add("##INFO=<ID=ClippingRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref number of hard clipped bases\">");
		  header.add("##INFO=<ID=DB,Number=0,Type=Flag,Description=\"dbSNP Membership\">");
		  header.add("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Approximate read depth; some reads may have been filtered\">");
		  header.add("##INFO=<ID=DS,Number=0,Type=Flag,Description=\"Were any of the samples downsampled?\">");
		  header.add("##INFO=<ID=FS,Number=1,Type=Float,Description=\"Phred-scaled p-value using Fisher's exact test to detect strand bias\">");
		  header.add("##INFO=<ID=HaplotypeScore,Number=1,Type=Float,Description=\"Consistency of the site with at most two segregating haplotypes\">");
		  header.add("##INFO=<ID=InbreedingCoeff,Number=1,Type=Float,Description=\"Inbreeding coefficient as estimated from the genotype likelihoods per-sample when compared against the Hardy-Weinberg expectation\">");
		  header.add("##INFO=<ID=MLEAC,Number=A,Type=Integer,Description=\"Maximum likelihood expectation (MLE) for the allele counts (not necessarily the same as the AC), for each ALT allele, in the same order as listed\">");
		  header.add("##INFO=<ID=MLEAF,Number=A,Type=Float,Description=\"Maximum likelihood expectation (MLE) for the allele frequency (not necessarily the same as the AF), for each ALT allele, in the same order as listed\">");
		  header.add("##INFO=<ID=MQ,Number=1,Type=Float,Description=\"RMS Mapping Quality\">");
		  header.add("##INFO=<ID=MQ0,Number=1,Type=Integer,Description=\"Total Mapping Quality Zero Reads\">");
		  header.add("##INFO=<ID=MQRankSum,Number=1,Type=Float,Description=\"Z-score From Wilcoxon rank sum test of Alt vs. Ref read mapping qualities\">");
		  header.add("##INFO=<ID=QD,Number=1,Type=Float,Description=\"Variant Confidence/Quality by Depth\">");
		  header.add("##INFO=<ID=ReadPosRankSum,Number=1,Type=Float,Description=\"Z-score from Wilcoxon rank sum test of Alt vs. Ref read position bias\">");
		  header.add("##INFO=<ID=SOR,Number=1,Type=Float,Description=\"Symmetric Odds Ratio of 2x2 contingency table to detect strand bias\">");
		  header.add("##contig=<ID=chr1,length=249250621>");
		  header.add("##contig=<ID=chr2,length=243199373>");
		  header.add("##contig=<ID=chr3,length=198022430>");
		  header.add("##contig=<ID=chr4,length=191154276>");
		  header.add("##contig=<ID=chr5,length=180915260>");
		  header.add("##contig=<ID=chr6,length=171115067>");
		  header.add("##contig=<ID=chr7,length=159138663>");
		  header.add("##contig=<ID=chr8,length=146364022>");
		  header.add("##contig=<ID=chr9,length=141213431>");
		  header.add("##contig=<ID=chr10,length=135534747>");
		  header.add("##contig=<ID=chr11,length=135006516>");
		  header.add("##contig=<ID=chr12,length=133851895>");
		  header.add("##contig=<ID=chr13,length=115169878>");
		  header.add("##contig=<ID=chr14,length=107349540>");
		  header.add("##contig=<ID=chr15,length=102531392>");
		  header.add("##contig=<ID=chr16,length=90354753>");
		  header.add("##contig=<ID=chr17,length=81195210>");
		  header.add("##contig=<ID=chr18,length=78077248>");
		  header.add("##contig=<ID=chr19,length=59128983>");
		  header.add("##contig=<ID=chr20,length=63025520>");
		  header.add("##reference=file:///opt/local/genomeinfo/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa");
		  header.add("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	OESO_2804");
		  
		  // create tmp file and shove this stuff in
		  File vcfFile = testFolder.newFile();
		  createFile(vcfFile, header);
		  TestVcfPipeline vp = new TestVcfPipeline(true);
		  vp.testVcfFile = vcfFile.getAbsolutePath();
		  VcfHeader headerFromFile = null;
		  try (VcfFileReader reader = new VcfFileReader(vcfFile)) {
			  headerFromFile = reader.getVcfHeader();
		  }
		  vp.setTestVcfHeader(headerFromFile);
		  
		  // check that header order is ok
		  int i = 0;
		  for (VcfHeaderRecord rec : headerFromFile) {
			  i++;
			  if (i == 1) {
				  assertEquals("##fileformat=VCFv4.1", rec.toString());
			  } else if (i == 2) {
                  assertTrue(rec.toString().startsWith(VcfHeaderUtils.STANDARD_UUID_LINE));
			  } else if (i == 3) {
                  assertTrue(rec.toString().startsWith("##reference"));
			  } else if (i == 4) {
                  assertTrue(rec.toString().startsWith(VcfHeaderUtils.HEADER_LINE_FILTER));
			  } else if (i > 4 && i < 24) {
                  assertTrue(rec.toString().startsWith(VcfHeaderUtils.HEADER_LINE_INFO));
			  }else if (i > 23 && i < 29) {
                  assertTrue(rec.toString().startsWith(VcfHeaderUtils.HEADER_LINE_FORMAT));
			  } else if (i > 29 && i < 50) {
                  assertTrue(rec.toString().startsWith("##contig"));
			  }  else if (i == 29) {
                  assertTrue(rec.toString().startsWith("##GATKCommandLine"));
			  } else {
                  assertTrue(rec.toString().startsWith("#CHROM"));
			  }
		  }
		  assertEquals(50, i);	// no additional header added at this stage
		  
		  VcfHeader existingHeader = vp.getExistingVCFHeaderDetails();
		  
		  // this should only contain entries that are marked for inclusion
		  for (VcfHeaderRecord rec : existingHeader) {
			  if(rec.toString().startsWith(VcfHeaderUtils.STANDARD_FILE_FORMAT) 
					  || rec.toString().startsWith(VcfHeaderUtils.STANDARD_FINAL_HEADER_LINE))
				  continue;
              assertTrue(rec.getId() != null     //???
                      || rec.toString().startsWith(VcfHeaderUtils.STANDARD_CONTROL_VCF)
                      || rec.toString().startsWith(VcfHeaderUtils.STANDARD_CONTROL_VCF_UUID)
                      || rec.toString().startsWith(VcfHeaderUtils.STANDARD_CONTROL_VCF_GATK_VER)
                      || rec.toString().startsWith(VcfHeaderUtils.STANDARD_TEST_VCF)
                      || rec.toString().startsWith(VcfHeaderUtils.STANDARD_TEST_VCF_GATK_VER)
                      || rec.toString().startsWith(VcfHeaderUtils.STANDARD_TEST_VCF_UUID));
		  }
		  
		  // now check that when calling writeToVcf that we get this along with standard qsnp vcf header		  
		  File qsnpVcfFile = testFolder.newFile();
		  vp.writeVCF(qsnpVcfFile.getAbsolutePath());
		  
		  VcfHeader finalHeader = null;
		  try (VcfFileReader reader2 = new VcfFileReader(qsnpVcfFile)) {
		  		finalHeader = reader2.getVcfHeader();
		  }
		  
		  List<VcfHeaderRecord> finalHeaderRecords = new ArrayList<>();
		  for (VcfHeaderRecord rec : finalHeader) finalHeaderRecords.add(rec);
		  
		  List<VcfHeaderRecord> existingHeaderRecords = new ArrayList<>();
		  for (VcfHeaderRecord rec : existingHeader) existingHeaderRecords.add(rec);
				  
		  //iterator will add ##FileFormat and #CHROM... line
		  finalHeaderRecords.add(existingHeader.getFileFormat());
		  finalHeaderRecords.add(existingHeader.getChrom());
		  
		  //existingHeaderRecords contains version and Chrom		  
		  for (VcfHeaderRecord rec : existingHeaderRecords)  		  
			  if ( ! finalHeaderRecords.contains(rec)) {
				  System.out.println("rec not contained in final: " + rec);
				  if (rec.toString().contains("BaseQRankSum")) {					  
					  System.out.println("rec BaseQRankSum hashCode: " + rec.hashCode());
					  // loop through finals
					  for (VcfHeaderRecord finalRec : finalHeaderRecords)  
						  if (finalRec.toString().contains("BaseQRankSum")) {
							  System.out.println("finalRec: " + finalRec);
							  assertEquals(rec, finalRec);							  
							  String line1 = rec.toString();
							  String line2 = finalRec.toString();
                              assertEquals(line1, line2);
							  System.out.println("finalRec: BaseQRankSum hashCode: " + finalRec.hashCode());
						  }					   
				  }
			  }

        assertTrue(finalHeaderRecords.containsAll(existingHeaderRecords));
	}
	
	@Test
	public void merge() {
		Map<ChrPosition, VcfRecord> controlMap = new HashMap<>();
		Map<ChrPosition, VcfRecord> testMap = new HashMap<>();
		List<VcfRecord> mergedVcfs = new ArrayList<>();
		
		/*
		 * setup records
		 */
		String [] params1 = "chr1	142671569	.	C	T	757.77	.	AC=1;AF=0.500;AN=2;BaseQRankSum=-1.971;ClippingRankSum=0.000;DP=125;ExcessHet=3.0103;FS=1.825;MLEAC=1;MLEAF=0.500;MQ=50.74;MQRankSum=-4.031;QD=7.65;ReadPosRankSum=-1.984;SOR=0.922	GT:AD:DP:GQ:PL	0/1:71,28:99:99:786,0,2594".split("\t");
		VcfRecord vcf1 = new VcfRecord(params1);
		controlMap.put(vcf1.getChrPosition(), vcf1);
		
		String [] params2 = "chr1	142671569	.	C	G	1491.77	.	AC=1;AF=0.500;AN=2;BaseQRankSum=-2.275;ClippingRankSum=0.000;DP=242;ExcessHet=3.0103;FS=104.768;MLEAC=1;MLEAF=0.500;MQ=49.87;MQRankSum=-10.385;QD=7.46;ReadPosRankSum=-0.318;SOR=5.812	GT:AD:DP:GQ:PL	0/1:141,59:200:99:1520,0,5093".split("\t");
		VcfRecord vcf2 = new VcfRecord(params2);
		testMap.put(vcf2.getChrPosition(), vcf2);
		
		/*
		 * run merge
		 */
		VcfPipeline.mergeVcfRecords(controlMap, testMap, mergedVcfs);
		assertEquals(1, mergedVcfs.size());
		VcfRecord mergedRec = mergedVcfs.getFirst();
		Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(mergedRec.getFormatFields());
		assertEquals("T,G", mergedRec.getAlt());
		assertEquals("0/1", ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE)[0]);
		assertEquals("0/2", ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE)[1]);
		assertEquals("71,28", ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS)[0]);
		assertEquals("141,0,59", ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS)[1]);
	}
	
	private int noOfLinesInVCFOutputFile(File vcfOutput) throws IOException {
		int noOfLines = 0;
		try (VcfFileReader reader = new VcfFileReader(vcfOutput)){
			for (VcfRecord vcf : reader) noOfLines++;
		}
		return noOfLines;
	}
	
}
