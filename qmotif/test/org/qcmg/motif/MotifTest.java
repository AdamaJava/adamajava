package org.qcmg.motif;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import htsjdk.samtools.SAMFileHeader.SortOrder;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMFileWriter;
import htsjdk.samtools.SAMFileWriterFactory;
import htsjdk.samtools.SAMRecord;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.commandline.Executor;
import org.qcmg.motif.util.MotifConstants;
import org.qcmg.picard.SAMFileReaderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MotifTest {
	
	@Rule
	public TemporaryFolder testFolder = new TemporaryFolder();
	
	File bamFile;
	File iniFile;
	static List<String> data = new ArrayList<>();
	
	@Before
	public void createFiles() throws IOException {
		if (bamFile == null) {
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			data.add("HWI-ST1240:136:D1JPJACXX:1:1110:13684:13489	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTGAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############BBBBBBB<BBBB7BBBB<BB<BBF<<FBFFIFFFFFBBBBIIFFBFFBFIFFFBFFFFBFIFFFFB7BFFFIFFFFBBBFFFFFFFBB<	ZC:i:9	RG:Z:20130123073922867");
			
			File sam = testFolder.newFile("sam");
			bamFile = testFolder.newFile("bam");
			iniFile = testFolder.newFile("ini");
			
			createSam(sam);
			createBam(sam, bamFile);
			createIni(iniFile);
		}
		
	}
	
	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.motif.Motif");
	}
	
	@Test
	public void qmotif() throws Exception {
		File logFile =  testFolder.newFile("log");
		File outputXmlFile =  testFolder.newFile("xml");
		File outputBamFile =  testFolder.newFile("bamoutput");
		ExpectedException.none();
		Executor exec = execute("--log " + logFile.getAbsolutePath() + " --bam " + bamFile.getAbsolutePath() + " -o " + outputXmlFile.getAbsolutePath() + " -o " + outputBamFile.getAbsolutePath() +  " -ini " + iniFile.getAbsolutePath());
		assertTrue(0 == exec.getErrCode());

		assertTrue(outputXmlFile.exists());
		assertTrue(outputBamFile.exists());
		
		// ok lets delve into the xml
		Document doc = createDocumentFromFile(outputXmlFile);
		
		NodeList iniNL = doc.getElementsByTagName("ini");
		
		for (int i = 0 ; i < iniNL.getLength() ; i++) {
			Element e = (Element) iniNL.item(i);
			Node s1m = e.getElementsByTagName("stage1_motif").item(0);
			Node s2m = e.getElementsByTagName("stage2_motif").item(0);
			Element io =(Element) e.getElementsByTagName("includes_only").item(0);
			assertEquals("false", io.getAttribute("value"));
			
			NodeList s1mNL = s1m.getChildNodes();
			NodeList s2mNL = s2m.getChildNodes();
			int stringCounter = 0;
			for (int j = 0 ; j < s1mNL.getLength() ; j++) {
				Node child =  s1mNL.item(j);
				if (child.getNodeName().equals("string")) {
					stringCounter++;
				}
			}
			assertEquals(2, stringCounter);
			
			int regexCounter = 0;
			for (int j = 0 ; j < s2mNL.getLength() ; j++) {
				Node child =  s2mNL.item(j);
				if (child.getNodeName().equals("regex")) {
					regexCounter++;
				}
			}
			assertEquals(1, regexCounter);
		}
		
		NodeList snl = doc.getElementsByTagName("counts");
		for (int i = 0 ; i < snl.getLength() ; i++) {
			Element e = (Element) snl.item(i);
			NodeList children = e.getChildNodes();
			for (int j = 0 ; j < children.getLength() ; j++) {
				Node childNode = children.item(j);
//				System.out.println("Node name: " + childNode.getClass());
				if ( ! childNode.getClass().toString().equals("class com.sun.org.apache.xerces.internal.dom.DeferredTextImpl")) {
					Element child =  (Element)children.item(j);
					if (child.getNodeName().startsWith("totalReadsInThisAnalysis")) {
						assertEquals(false, child.getAttribute("count").equals("0"));
					}
					if (child.getNodeName().startsWith(MotifConstants.BASES_CONTAINING_MOTIFS)) {
						System.out.println("bases containing motifs: " + child.getAttribute("count"));
						assertEquals(false, child.getAttribute("count").equals("0"));
					}
				}
			}
		}		
		
		
		NodeList nl = doc.getElementsByTagName("region");
		
		int count = 0;
		
		for (int i = 0 ; i < nl.getLength() ; i++) {
			Element e = (Element) nl.item(i);
			
			// get 
			NodeList children = e.getChildNodes();
			for (int j = 0 ; j < children.getLength() ; j++) {
				Node child =  children.item(j);
				NamedNodeMap attributes = child.getAttributes();
				if ( null != attributes && attributes.getLength() > 0) {
//					System.out.print(child.getNodeName());
					Node n = attributes.getNamedItem("number");
					
					count += Integer.parseInt(n.getNodeValue());
				}
			}
		}
		System.out.println("no of hits: " + count);
		assertEquals(10, count);
	}
	
	@Test
	public void qmotifIncludesOnly() throws Exception {
		File logFile =  testFolder.newFile("log");
		File outputXmlFile =  testFolder.newFile("xml");
		File outputBamFile =  testFolder.newFile("bamoutput");
		ExpectedException.none();
		
		File includesOnlyINi = testFolder.newFile();
				
		createIncludesInlyIni(includesOnlyINi);
		
		Executor exec = execute("--log " + logFile.getAbsolutePath() + " --bam " + bamFile.getAbsolutePath() + " -o " + outputXmlFile.getAbsolutePath() + " -o " + outputBamFile.getAbsolutePath() +  " -ini " + includesOnlyINi.getAbsolutePath());
		assertTrue(0 == exec.getErrCode());
		
		assertTrue(outputXmlFile.exists());
		assertTrue(outputBamFile.exists());
		
		// ok lets delve into the xml
		Document doc = createDocumentFromFile(outputXmlFile);
		
		NodeList iniNL = doc.getElementsByTagName("ini");
		
		for (int i = 0 ; i < iniNL.getLength() ; i++) {
			Element e = (Element) iniNL.item(i);
			Node s1m = e.getElementsByTagName("stage1_motif").item(0);
			Node s2m = e.getElementsByTagName("stage2_motif").item(0);
			Element io =(Element) e.getElementsByTagName(MotifConstants.INCLUDES_ONLY_MODE).item(0);
			assertEquals("true", io.getAttribute("value"));
			
			NodeList s1mNL = s1m.getChildNodes();
			NodeList s2mNL = s2m.getChildNodes();
			int stringCounter = 0;
			for (int j = 0 ; j < s1mNL.getLength() ; j++) {
				Node child =  s1mNL.item(j);
				if (child.getNodeName().equals("string")) {
					stringCounter++;
				}
			}
			assertEquals(2, stringCounter);
			
			int regexCounter = 0;
			for (int j = 0 ; j < s2mNL.getLength() ; j++) {
				Node child =  s2mNL.item(j);
				if (child.getNodeName().equals("regex")) {
					regexCounter++;
				}
			}
			assertEquals(1, regexCounter);
		}
		
		NodeList snl = doc.getElementsByTagName("counts");
		for (int i = 0 ; i < snl.getLength() ; i++) {
			Element e = (Element) snl.item(i);
			NodeList children = e.getChildNodes();
			for (int j = 0 ; j < children.getLength() ; j++) {
				Node childNode = children.item(j);
//				System.out.println("Node name: " + childNode.getClass());
				if ( ! childNode.getClass().toString().equals("class com.sun.org.apache.xerces.internal.dom.DeferredTextImpl")) {
					Element child =  (Element)children.item(j);
					if (child.getNodeName().startsWith("scaled")) {
						assertEquals("-1", child.getAttribute("count"));
					} else if (child.getNodeName().startsWith("totalReadsInThisAnalysis")) {
						assertEquals(true, child.getAttribute("count").equals("0"));
					} else if (child.getNodeName().startsWith(MotifConstants.BASES_CONTAINING_MOTIFS)) {
						System.out.println("bases containing motifs: " + child.getAttribute("count"));
						assertEquals(true, child.getAttribute("count").equals("0"));
					}
				}
			}
		}		
		
		
		
		NodeList nl = doc.getElementsByTagName("region");
		
		int count = 0;
		
		for (int i = 0 ; i < nl.getLength() ; i++) {
			Element e = (Element) nl.item(i);
			// get 
			NodeList children = e.getChildNodes();
			for (int j = 0 ; j < children.getLength() ; j++) {
				Node child =  children.item(j);
				NamedNodeMap attributes = child.getAttributes();
				if ( null != attributes && attributes.getLength() > 0) {
					Node n = attributes.getNamedItem("number");
					count += Integer.parseInt(n.getNodeValue());
				}
			}
		}
		assertEquals(0, count);
	}
	
	
	public static Document createDocumentFromFile(File absoluteFile)  {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		Document doc = null;
		try {
			builder = factory.newDocumentBuilder();
			doc = builder.parse(absoluteFile);
			doc.getDocumentElement().normalize();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return doc;
	}
	
	public static void createBam(File samFile, File bam) throws IOException {
		 SAMFileWriterFactory factory = new SAMFileWriterFactory().setCreateIndex(true);
		 
		  try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(samFile);
				SAMFileWriter writer = factory.makeBAMWriter(reader.getFileHeader(), false, bam);	 ) {			 		
			  for (SAMRecord r: reader)  
				writer.addAlignment(r);				 			 
		  }		  		  
	}
	
	public static void createSam(File samFile) throws IOException {
        // create sam header and records
        
       
        try (BufferedWriter out = new BufferedWriter(new FileWriter(samFile));) {
	        for (final String line : createSamHeader(SortOrder.coordinate)) {
	            out.write(line + "\n");
	        }
	        for (final String line : data) {
	        		out.write(line + "\n");
	        }
        }
 }
	public static void createIni(File iniFile) throws IOException {
		List<String> iniData = new ArrayList<>();
		iniData.add("[PARAMS]");
			iniData.add("stage1_motif_string=TTAGGGTTAGGGTTAGGG");
			iniData.add("stage2_motif_regex=(...GGG){2,}|(CCC...){2,}");
			iniData.add("stage1_string_rev_comp=true");
			iniData.add("window_size=10000");

			iniData.add("[INCLUDES]");
			iniData.add("; name, regions (sequence:start-stop)");
			iniData.add("chr1p	chr1:10001-12464");
			
			
		try (BufferedWriter out = new BufferedWriter(new FileWriter(iniFile));) {
			for (final String line : iniData) {
				out.write(line + "\n");
			}
		}
	}
	
	public static void createIncludesInlyIni(File iniFile) throws IOException {
		List<String> iniData = Arrays.asList("[PARAMS]",
				"stage1_motif_string=TTAGGGTTAGGGTTAGGG",
				"stage2_motif_regex=(...GGG){2,}|(CCC...){2,}",
				"stage1_string_rev_comp=true",
				"window_size=10000",
				MotifConstants.INCLUDES_ONLY_MODE + "=true",
				"[INCLUDES]",
				"; name, regions (sequence:start-stop)",
				"chr1p	chr1:10001-12464"
				);
		
		try (PrintWriter out = new PrintWriter(iniFile);) {
			iniData.stream().forEach(out::println);
		}
	}
	
	
	private static Collection<String> createSamHeader(SortOrder sort) {
		 final List<String> data = new ArrayList<String>();
	        data.add("@HD	VN:1.0	GO:none	SO:"+ sort.name());
	        data.add("@SQ	SN:chr1	LN:249250621	");
	        data.add("@SQ	SN:chr4	LN:191154276	");
	        data.add("@SQ	SN:chr7	LN:159138663	");
	        data.add("@SQ	SN:chrX	LN:155270560	");
	        data.add("@SQ	SN:chrY	LN:59373566	");
	        data.add("@SQ	SN:chr10	LN:135534747");	        
	        data.add("@SQ	SN:chr19	LN:59128983	");
	        data.add("@SQ	SN:GL000191.1	LN:106433	");
	        data.add("@SQ	SN:GL000211.1	LN:166566	");
	        data.add("@SQ	SN:chrMT	LN:16569	");
	        data.add("@RG	ID:20130123062807835	PL:ILLUMINA	PU:lane_3	LB:Library_20120511_C	SM:Colo-829");
	        data.add("@RG	ID:20130123070048613	PL:ILLUMINA	PU:lane_2	LB:Library_20120511_C	SM:Colo-829");
	        data.add("@RG	ID:20130123073922867	PL:ILLUMINA	PU:lane_1	LB:Library_20120511_C	SM:Colo-829");
	        return data;
	}

}
