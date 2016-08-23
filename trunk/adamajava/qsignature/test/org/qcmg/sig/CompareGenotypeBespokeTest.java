package org.qcmg.sig;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javafx.scene.shape.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;

public class CompareGenotypeBespokeTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void singleEmptyInputFile() throws Exception {
		File logF = testFolder.newFile();
		File inputF = testFolder.newFile("blah.qsig.vcf.gz");
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + inputF.getParent());
		assertTrue(0 == exec.getErrCode());		// all ok
	}
	
	@Test
	public void nonEmptyInputFiles() throws Exception {
		File logF = testFolder.newFile();
		File f1 = testFolder.newFile("blah.qsig.vcf");
		File f2 = testFolder.newFile("blah2.qsig.vcf");
		File o = testFolder.newFile();
		
		writeVcfFile(f1);
		writeVcfFile(f2);
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + f1.getParent() + " -o " + o.getAbsolutePath());
		assertTrue(0 == exec.getErrCode());		// all ok
		assertEquals(true, o.exists());
		assertEquals(10, Files.readAllLines(Paths.get(o.getAbsolutePath())).size());		// 10 lines means 1 comparison
	}
	
	@Test
	public void diffMd5InputFiles() throws Exception {
		File logF = testFolder.newFile();
		File f1 = testFolder.newFile("blah.qsig.vcf");
		File f2 = testFolder.newFile("blah2.qsig.vcf");
		File o = testFolder.newFile();
		
		writeVcfFile(f1, "##positions_md5sum=d18c99f481afbe04294d11deeb418890\n");
		writeVcfFile(f2, "##positions_md5sum=d18c99f481afbe04294d11deeb418890XXX\n");
		
		Executor exec = execute("--log " + logF.getAbsolutePath() + " -d " + f1.getParent() + " -o " + o.getAbsolutePath());
		assertTrue(0 == exec.getErrCode());		// all ok
		assertEquals(true, o.exists());
		assertEquals(8, Files.readAllLines(Paths.get(o.getAbsolutePath())).size());		// 8 lines means 0 comparison
		
	}
	
	
	private void writeVcfFile(File f) throws IOException {
		writeVcfFile(f, "##positions_md5sum=d18c99f481afbe04294d11deeb418890\n");
	}
	
	private void writeVcfFile(File f, String md5) throws IOException {
		try (FileWriter w = new FileWriter(f);){
			w.write("##fileformat=VCFv4.2\n");
			w.write("##datetime=2016-08-17T14:44:30.088\n");
			w.write("##program=SignatureGeneratorBespoke\n");
			w.write("##version=1.0 (1230)\n");
			w.write("##java_version=1.8.0_71\n");
			w.write("##run_by_os=Linux\n");
			w.write("##run_by_user=oliverH\n");
			w.write("##positions=/software/genomeinfo/configs/qsignature/qsignature_positions.txt\n");
			w.write(md5);
			w.write("##positions_count=1456203\n");
			w.write("##filter_base_quality=10\n");
			w.write("##filter_mapping_quality=10\n");
			w.write("##illumina_array_design=/software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt\n");
			w.write("##cmd_line=SignatureGeneratorBespoke -i /software/genomeinfo/configs/qsignature/qsignature_positions.txt -illumina /software/genomeinfo/configs/qsignature/Illumina_arrays_design.txt -i /mnt/lustre/working/genomeinfo/share/mapping/aws/argsBams/dd625894-d1e3-4938-8d92-3a9f57c23e08.bam -d /mnt/lustre/home/oliverH/qsignature/bespoke/ -log /mnt/lustre/home/oliverH/qsignature/bespoke/siggen.log\n");
			w.write("##INFO=<ID=QAF,Number=.,Type=String,Description=\"Lists the counts of As-Cs-Gs-Ts for each read group, along with the total\">\n");
			w.write("##input=/mnt/lustre/working/genomeinfo/share/mapping/aws/argsBams/dd625894-d1e3-4938-8d92-3a9f57c23e08.bam\n");
			w.write("##id:readgroup\n");
			w.write("##rg1:143b8c38-62cb-414a-aac3-ea3a940cc6bb\n");
			w.write("##rg2:65a79904-ee91-4f53-9a94-c02e23e071ef\n");
			w.write("#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO\n");
			w.write("chr1	47851\t.\tC\t.\t.\t.\tQAF=t:0-120-0-0,rg1:0-90-0-0,rg2:0-30-0-0\n");
			w.write("chr1	50251\t.\tT\t.\t.\t.\tQAF=t:0-0-0-110,rg1:0-0-0-90,rg2:0-0-0-20\n");
			w.write("chr1\t51938	.\tT\t.\t.\t.\tQAF=t:0-0-0-90,rg1:0-0-0-50,rg2:0-0-0-40\n");
			w.write("chr1\t52651	.\tT\t.\t.\t.\tQAF=t:0-0-0-30,rg1:0-0-0-10,rg2:0-0-0-20\n");
			w.write("chr1\t64251	.\tA\t.\t.\t.\tQAF=t:90-0-0-0,rg1:50-0-0-0,rg2:40-0-0-0\n");
			w.write("chr1\t98222	.\tC\t.\t.\t.\tQAF=t:0-120-0-0,rg1:0-50-0-0,rg2:0-70-0-0\n");
			w.write("chr1\t99236	.\tT\t.\t.\t.\tQAF=t:0-0-0-220,rg1:0-0-0-120,rg2:0-0-0-100\n");
			w.write("chr1\t101095	.\tT\t.\t.\t.\tQAF=t:0-0-0-100,rg1:0-0-0-50,rg2:0-0-0-50\n");
			w.write("chr1\t102954	.\tT\t.\t.\t.\tQAF=t:0-0-10-640,rg1:0-0-0-360,rg2:0-0-10-280\n");
			w.write("chr1\t104813	.\tG\t.\t.\t.\tQAF=t:0-10-170-0,rg1:0-10-100-0,rg2:0-0-70-0\n");
			w.write("chr1\t106222	.\tT\t.\t.\t.\tQAF=t:0-0-0-40,rg1:0-0-0-10,rg2:0-0-0-30\n");
		}
		
	}
	
	
	
	private Executor execute(final String command) throws IOException, InterruptedException {
		return new Executor(command, "org.qcmg.sig.CompareGenotypeBespoke");
	}

}
