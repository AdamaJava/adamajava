package org.qcmg.picard.util;

import static org.junit.Assert.assertEquals;
import net.sf.samtools.SAMFileHeader;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.meta.QDccMeta;
import org.qcmg.common.meta.QExec;

public class QDccMetaFactoryTest {
	
	@Test
	public void testGetCommentLine() {
		assertEquals(null, QLimsMetaFactory.getQCMGCommentLine(new SAMFileHeader()));
		SAMFileHeader header = getHeader("");
		assertEquals(null, QLimsMetaFactory.getQCMGCommentLine(header));
		
		String commentLine = "@CO	CN:QCMG	QN:qlimsmeta	Project=uProject	Donor=AAAA_1111	Material=1:DNA	Sample Code=4:Normal control (other site)	Sample=SAMPLE-20110923-56-ND	Aligner=bwa	Capture Kit=Human Rapid Exome (Nextera)	Sequencing Platform=Form1";
		header = getHeader(commentLine);
		assertEquals(commentLine, QLimsMetaFactory.getQCMGCommentLine(header));
		
		commentLine = "@CO	CN:QCMG	QN:BLAHqlimsmeta	Project=uProject	Donor=AAAA_1111	Material=1:DNA	Sample Code=4:Normal control (other site)	Sample=SAMPLE-20110923-56-ND	Aligner=bwa	Capture Kit=Human Rapid Exome (Nextera)	Sequencing Platform=Form1";
		header = getHeader(commentLine);
		assertEquals(null, QLimsMetaFactory.getQCMGCommentLine(header));
	}
	
	@Test
	public void testGetDccMeta() throws Exception {
		String controlCommentLine = "@CO	CN:QCMG	QN:qlimsmeta	Project=uProject	Donor=AAAA_1111	Material=1:DNA	Sample Code=4:Normal control (other site)	Sample=SAMPLE-20110923-56-ND	Aligner=bwa	Capture Kit=Human Rapid Exome (Nextera)	Sequencing Platform=Form1";
		SAMFileHeader controlHeader = getHeader(controlCommentLine);
		String analysisCommentLine = "@CO	CN:QCMG	QN:qlimsmeta	Project=uProject	Donor=AAAA_1111	Material=1:DNA	Sample Code=7:Primary Tumour	Sample=SAMPLE-20110923-56-TD	Aligner=bwa	Capture Kit=Human Rapid Exome (Nextera)	Sequencing Platform=Form1";
		SAMFileHeader analysisHeader = getHeader(analysisCommentLine);
		
		QExec qexec = new QExec("QDccMetaFactoryTest", "0.1-beta", null);
		
		QDccMeta dccMeta = QDccMetaFactory.getDccMeta(qexec, controlHeader, analysisHeader, "testGetDccMeta");
		
		assertEquals(true, null != dccMeta);
		assertEquals("AAAA_1111", dccMeta.getDonorId().getValue());
		assertEquals("Form1", dccMeta.getPlatform().getValue());
		assertEquals(qexec.getUuid().getValue(), dccMeta.getAnalysisId().getValue());
		assertEquals("SAMPLE-20110923-56-TD", dccMeta.getAnalyzedSampleId().getValue());
		assertEquals("SAMPLE-20110923-56-ND", dccMeta.getMatchedSampleId().getValue());
		assertEquals("Human Rapid Exome (Nextera)", dccMeta.getExperimentalProtocol().getValue());
		assertEquals("bwa", dccMeta.getAlignmentAlgorithm().getValue());
	}
	
	@Test
	public void testGetDccMetaDataSOLiD() throws Exception {
		String controlCommentLine = "CO	CN:QCMG	QN:qlimsmeta	Aligner=bioscope	Capture Kit=Kit1 (SureSelect)	Donor=AAAA_3333	Failed QC=0	Library Protocol=P1 v4 Multiplexed SpriTE	Material=1:DNA	Project=uProject	Reference Genome File=/path/ref.fa	Sample=SAMPLE-20110807-01-ND	Sample Code=4:Normal control (other site)	Sequencing Platform=P14	Species Reference Genome=Homo sapiens (REF_v2)";
		SAMFileHeader controlHeader = getHeader(controlCommentLine);
		String analysisCommentLine = "@CO	CN:QCMG	QN:qlimsmeta	Aligner=bioscope	Capture Kit=Kit1 (SureSelect)	Donor=AAAA_3333	Failed QC=0	Library Protocol=P1 v4 Multiplexed SpriTE	Material=1:DNA	Project=uProject	Reference Genome File=/path/ref.fa	Sample=SAMPLE-20110807-02-TD	Sample Code=7:Primary tumour	Sequencing Platform=P14	Species Reference Genome=Homo sapiens (REF_v2)";
		SAMFileHeader analysisHeader = getHeader(analysisCommentLine);
		
		QExec qexec = new QExec("QDccMetaFactoryTest", "0.1-beta", null);
		
		QDccMeta dccMeta = QDccMetaFactory.getDccMeta(qexec, controlHeader, analysisHeader, "testGetDccMeta");
		
		assertEquals(true, null != dccMeta);
		assertEquals("AAAA_3333", dccMeta.getDonorId().getValue());
		assertEquals("P14", dccMeta.getPlatform().getValue());
		assertEquals(qexec.getUuid().getValue(), dccMeta.getAnalysisId().getValue());
		assertEquals("SAMPLE-20110807-02-TD", dccMeta.getAnalyzedSampleId().getValue());
		assertEquals("SAMPLE-20110807-01-ND", dccMeta.getMatchedSampleId().getValue());
		assertEquals("P1 v4 Multiplexed SpriTE Kit1 (SureSelect)", dccMeta.getExperimentalProtocol().getValue());
		assertEquals("bioscope", dccMeta.getAlignmentAlgorithm().getValue());
	}
	
	@Test
	public void testGetDccMetaMismatchingDonors() throws Exception {
		String controlCommentLine = "@CO	CN:QCMG	QN:qlimsmeta	Project=uProject	Donor=AAAA_1111	Material=1:DNA	Sample Code=4:Normal control (other site)	Sample=SAMPLE-20110923-56-ND	Aligner=bwa	Capture Kit=Human Rapid Exome (Nextera)	Sequencing Platform=Form1";
		SAMFileHeader controlHeader = getHeader(controlCommentLine);
		String analysisCommentLine = "@CO	CN:QCMG	QN:qlimsmeta	Project=uProject	Donor=AAAA_1993	Material=1:DNA	Sample Code=7:Primary Tumour	Sample=SAMPLE-20110923-56-TD	Aligner=bwa	Capture Kit=Human Rapid Exome (Nextera)	Sequencing Platform=Form1";
		SAMFileHeader analysisHeader = getHeader(analysisCommentLine);
		
		QExec qexec = new QExec("QDccMetaFactoryTest", "0.1-beta", null);
		try {
			QDccMeta dccMeta = QDccMetaFactory.getDccMeta(qexec, controlHeader, analysisHeader, "testGetDccMeta");
			Assert.fail("Should have thrown an exception");
		} catch (Exception e) {}
	}
	
	
	private SAMFileHeader getHeader(String comment) {
		SAMFileHeader header = new SAMFileHeader();
		header.addComment(comment);
		return header;
	}

}
