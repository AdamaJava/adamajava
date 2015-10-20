package org.qcmg.motif;

import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import htsjdk.samtools.SAMFileHeader;
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
//		data.add("HWI-ST1240:136:D1JPJACXX:1:1207:5755:74707	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	<BB<B<<BFBBBBBFFFBBBFFFBBFFFFBFFFFFFFFFFFIFFFFFFFFFIFIIIIIIFIFIFFFIIIIIIFFIIIFFFFFFIIIFFFFFFFFFFFFBBB	ZC:i:9	RG:Z:20130123073922867");
//		data.add("HWI-ST1240:136:D1JPJACXX:1:2211:15379:94526	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	BFFFFFFFFFFBFFFFFFFFFFFBFFFFFFFFFFFFFFFFFIFIIIIIIFFIIIIIIIIIFIIIIIIIIIIIIIIIIIIIIIIIIIIIFFFFFFFFFFBBB	ZC:i:9	RG:Z:20130123073922867");
//		data.add("HWI-ST1240:136:D1JPJACXX:1:2306:11099:6077	117	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTCAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	0BBBB<7B<BB7<<7'7BBB<BBB<770BBB7'<<<<<77<BB7'<FFBB<770'<FBBB7'0B<FFFFFFFFBBFFFFBFFF<BFBFBFBF<FFFFFBBB	ZC:i:9	RG:Z:20130123073922867");
//		data.add("HWI-ST1240:136:D1JPJACXX:2:1105:12886:84131	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	BFFFFFFFFFBFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFIIIIIFIIIIIFFIIIFFIIIIIIIIIIIIIIIIIIIIIIIIIIFIIFFFFFFFFFFBBB	ZC:i:7	RG:Z:20130123070048613");
//		data.add("HWI-ST1240:136:D1JPJACXX:2:2204:10254:59272	117	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTGAGGGTTAGGGTTAGGG	0BB<BBBBBBBBB77BBBBBBBB<B<B<B<BBBBB<BFFFBBB0IIFFBIIIIIFIFFFFFBBFFIFIFFFBFIIIIIIIFFFB<BFIFFFFFFFFFFBBB	ZC:i:7	RG:Z:20130123070048613");
//		data.add("HWI-ST1240:136:D1JPJACXX:2:2311:13903:33588	117	chr1	9992	0	*	=	9992	0	GCTCTTCCAATCTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTATGGTTAGGGTTAGGGTTAGGGTTCGGGTTAGGGTTAGGGTTAGGGTTAGGG	###BB<00''<700BBB<''70B<B777<'0B<B<'<00B7'BFB<<'07'<<70FBFIIFFFFFIFBFFB000BBBB<FBBFB<FBFFB<FFFB<<FBBB	ZC:i:7	RG:Z:20130123070048613");
//		data.add("HWI-ST1240:136:D1JPJACXX:3:1104:13086:45358	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	BFFFBFBB<FFFBFBBBFFFBBFFBBBBBFFFFFFFFFFB<FIFFFFFBFIFFFIIIFFFIIIFFFFFFFFIFIIIIIIIIIIIFIIFFFFFFFFFFFBBB	ZC:i:11	RG:Z:20130123062807835");
//		data.add("HWI-ST1240:136:D1JPJACXX:3:1206:2437:61531	117	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGATTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	############B<BB<7077B<B<<BBBBB<B7'<<0'FBB77FB70'B<BBBFFBBFB7<<FB<<B7F<7FFBFFFBBB<B<<FBFB<FFFFFFFFB7B	ZC:i:11	RG:Z:20130123062807835");
//		data.add("HWI-ST1240:136:D1JPJACXX:3:1208:17115:46538	181	chr1	9992	0	*	=	9992	0	GCTCTTCCGATCTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGGTTAGGG	<FBFBBFBBFBFBFFBBBFFFFBBFFFFFFBFFFFFFFFFBFIIFFIFFFIIIIFIIFFFFFIIIIIFIIIIIIIIIIIIIIIIFFIIFFFFFFFFFFBBB	ZC:i:11	RG:Z:20130123062807835");
		
		File sam = testFolder.newFile("sam");
		bamFile = testFolder.newFile("bam");
		iniFile = testFolder.newFile("ini");
		
		createSam(sam);
		createBam(sam, bamFile);
		createIni(iniFile);
		
	}
	
	private Executor execute(final String command) throws Exception {
		return new Executor(command, "org.qcmg.motif.Motif");
	}
	
	@Test
	public void qmotif() throws Exception {
		File logFile =  testFolder.newFile("log");
		File outputXmlFile =  testFolder.newFile("xml");
		File outputBamFile =  testFolder.newFile("bamoutput");
//		File outputXmlFile =  testFolder.newFile("xml");
		ExpectedException.none();
		Executor exec = execute("--log " + logFile.getAbsolutePath() + " --bam " + bamFile.getAbsolutePath() + " -o " + outputXmlFile.getAbsolutePath() + " -o " + outputBamFile.getAbsolutePath() +  " -ini " + iniFile.getAbsolutePath());
		assertTrue(0 == exec.getErrCode());

		assertTrue(outputXmlFile.exists());
		assertTrue(outputBamFile.exists());
		
		// ok lets delve into the xml
		Document doc = createDocumentFromFile(outputXmlFile);
		NodeList nl = doc.getElementsByTagName("region");
		
		int count = 0;
		
		for (int i = 0 ; i < nl.getLength() ; i++) {
			Element e = (Element) nl.item(i);
			System.out.println("e.getNodeName: " + e.getNodeName());
			System.out.println("e.chrPos: " + e.getAttribute("chrPos"));
			System.out.println("e.name: " + e.getAttribute("name"));
			System.out.println("e.type: " + e.getAttribute("type"));
			System.out.println("e.stage1Cov: " + e.getAttribute("stage1Cov"));
			System.out.println("e.stage2Cov: " + e.getAttribute("stage2Cov"));
			
			// get 
			NodeList children = e.getChildNodes();
			for (int j = 0 ; j < children.getLength() ; j++) {
				Node child =  children.item(j);
				NamedNodeMap attributes = child.getAttributes();
				if ( null != attributes && attributes.getLength() > 0) {
					System.out.print(child.getNodeName());
					Node mr = attributes.getNamedItem("motifRef");
					Node n = attributes.getNamedItem("number");
					Node s = attributes.getNamedItem("strand");
					
					count += Integer.parseInt(n.getNodeValue());
					
					System.out.print("\tmr: " + mr.getNodeValue());
					System.out.print("\tn: " + n.getNodeValue());
					System.out.println("\ts: " + s.getNodeValue());
				}
			}
		}
		System.out.println("no of hits: " + count);
	}
	
	private int getPatternMatchCount(String pattern, String data) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(data);
		int count = 0;
		while (m.find()) {
			count++;
		}
		return count;
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
