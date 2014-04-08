/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import org.qcmg.common.util.LoadReferencedClasses;
import org.qcmg.picard.SAMFileReaderFactory;

public class TestJarUpdate {
	
	private SAMFileWriter writer;
	private SAMFileReader reader;
	
	private void doWork() throws Exception{
		try {
			
			LoadReferencedClasses.loadClasses(getClass());
			
//			URL className = getClass().getResource(TestJarUpdate.class.getName());
//			if (null != className) 
//				System.out.println("url: " + className.getFile());
//			else 
//				System.out.println("url: " + null);
//			
//			File jarFile = new File(TestJarUpdate.class.getProtectionDomain().getCodeSource().getLocation().toURI());
//			if (null != jarFile) 
//				System.out.println("jarFile: " + jarFile.getName());
//			else 
//				System.out.println("jarFile: " + null);
//			
//			System.out.println("is file type valid jar: " + FileUtils.isFileTypeValid(jarFile, "jar"));
//			
//			System.out.println("BEFORE: no of loaded packages: " + Package.getPackages().length);
//			
//			if (FileUtils.isFileTypeValid(jarFile, "jar")) {
//			
//				// got jar file - load and 
//				JarFile jf = new JarFile(jarFile);
//				Attributes att = jf.getManifest().getMainAttributes();
//				System.out.println("att.size" + att.size());
//				String classpath = att.getValue("Class-Path");
//				System.out.println("classpath: " + classpath);
//				
//				String [] jars = classpath.split(" ");
//				for (String jar : jars) {
//					JarFile internalJarFile = new JarFile(jar);
//					Enumeration<JarEntry> enums = internalJarFile.entries();
//					while (enums.hasMoreElements()) {
//						JarEntry je = enums.nextElement();
//						if (FileUtils.isFileTypeValid(je.getName(), "class")) {
//							String blah = je.getName().replace(".class", "");
//							blah = blah.replaceAll(System.getProperty("file.separator"), ".");
//							System.out.println("about to load class: " + blah);
//							this.getClass().getClassLoader().loadClass(blah);
//						}
//					}
//				}
//				
//			}
//			
//			System.out.println("AFTER: no of loaded packages: " + Package.getPackages().length);
			
			
		// write to bam file
		// sleep for a few mins to allow the sam jar file to be removed/replaced
		// close bam file
		// tinker with class loader
		File inputFile = File.createTempFile("testJarUpdateInput", ".sam");
		inputFile.deleteOnExit();
		File outputFile = File.createTempFile("testJarUpdateOutput", ".bam");
//		outputFile.deleteOnExit();
		
		createCoverageSam(inputFile);
		
		reader = SAMFileReaderFactory.createSAMFileReader(inputFile);
		
		SAMFileHeader header = reader.getFileHeader();
		List<SAMRecord> recs = new ArrayList<SAMRecord>();
		
		for( SAMRecord rec : reader) {
			recs.add(rec);
		}
		
		
		SAMFileWriterFactory factory = new SAMFileWriterFactory();
		
		writer = factory.makeSAMOrBAMWriter(header, true, outputFile);
		
//		for (int i = 0 ; i < 100 ; i++)
			for( SAMRecord rec : recs) {
				for (int i = 0 ; i < 100 ; i++)
					writer.addAlignment(rec);
			}
		
		System.out.println("About to sleep!");
		System.gc();
		Thread.sleep(60000);
		System.out.println("Am awake now");
		
		close();
		System.out.println("DONE!!!");
		} finally {
			System.out.println("about to run close quietly");
			closeQuietly();
			System.out.println("DONE!!! again");
		}
	}

	
	public static void main(String[] args) throws Exception {
		TestJarUpdate tju = new TestJarUpdate();
		tju.doWork();
	}
	
	
	private void close() throws Exception {
		try {
			writer.close();
			reader.close();
		} catch (Exception e) {
			System.out.println("Exception caught in close(): ");
//			e.printStackTrace();
			throw new Exception("CANNOT_CLOSE_FILES");
		}
	}
	
	private void closeQuietly() {
		try {
			close();
		} catch (Exception e) {
//			e.printStackTrace();
		}
	}
	
	public static final void createCoverageSam(final File fileName) throws Exception {

		OutputStream os = new FileOutputStream(fileName);
		PrintStream ps = new PrintStream(os);

		ps.println("@HD	VN:1.0	SO:coordinate");
		ps.println("@RG	ID:ZZ	SM:ES	DS:rl=50	");
		ps.println("@RG	ID:ZZZ	SM:ES	DS:rl=50	");
		ps.println("@PG	ID:SOLID-GffToSam	VN:1.4.3");
		ps.println("@SQ	SN:chr1	LN:249250621");
		ps.println("@SQ	SN:chr2	LN:243199373");
		ps.println("@SQ	SN:chr3	LN:198022430");
		ps.println("@SQ	SN:chr4	LN:191154276");
		ps.println("@SQ	SN:chr5	LN:180915260");
		ps.println("@SQ	SN:chr6	LN:171115067");
		ps.println("@SQ	SN:chr7	LN:159138663");
		ps.println("@SQ	SN:chr8	LN:146364022");
		ps.println("@SQ	SN:chr9	LN:141213431");
		ps.println("@SQ	SN:chr10	LN:135534747");
		ps.println("@SQ	SN:chr11	LN:135006516");
		ps.println("@SQ	SN:chr12	LN:133851895");
		ps.println("@SQ	SN:chr13	LN:115169878");
		ps.println("@SQ	SN:chr14	LN:107349540");
		ps.println("@SQ	SN:chr15	LN:102531392");
		ps.println("@SQ	SN:chr16	LN:90354753");
		ps.println("@SQ	SN:chr17	LN:81195210");
		ps.println("@SQ	SN:chr18	LN:78077248");
		ps.println("@SQ	SN:chr19	LN:59128983");
		ps.println("@SQ	SN:chr20	LN:63025520");
		ps.println("@SQ	SN:chr21	LN:48129895");
		ps.println("@SQ	SN:chr22	LN:51304566");
		ps.println("@SQ	SN:chrX	LN:155270560");
		ps.println("@SQ	SN:chrY	LN:59373566");
		ps.println("@SQ	SN:chrM	LN:16571");
		ps.println("1290_738_1025	0	chr1	54026	255	45M5H	*	0	0	AACATTCCAAAAGTCAACCATCCAAGTTTATTCTAAATAGATGTG	!DDDDDDDDDDDDDDDD''DDDDDD9DDDDDDDDD:<3B''DDD!	RG:Z:ZZ	CS:Z:T301130201000212101113201021003302230033233111	CQ:Z:BBB=B:@5?>B9A5?>B?'A49<475%@;6<+;9@'4)+8'1?:>");
		ps.println("2333_755_492	16	chr2	10103	255	10H40M	*	0	0	CACACCACACCCACACACCACACACCACACCCACACCCAC	!=DD?%+DD<)=DDD<@9)9C:DA.:DD>%%,<?('-,4!	RG:Z:ZZ	CS:Z:T0110001110211110111111111111100111001111	CQ:Z:%/&''(*6'&%+441*%=(31)<9(50=9%%8>?+%;<-1");
		ps.println("1879_282_595	0	chr3	60775	255	40M10H	*	0	0	TCTAAATTTGTTTGATCACATACTCCTTTTCTGGCTAACA	!DD,*@DDD''DD>5:DD>;DDDD=CDD8%%DA9-DDC0!	RG:Z:ZZ	CS:Z:T0223303001200123211133122020003210323011	CQ:Z:=><=,*7685'970/'437(4<:54*:84%%;/3''?;)(");
		ps.close();
		os.close();
	}
}
