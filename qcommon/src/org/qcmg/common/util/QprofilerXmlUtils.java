package org.qcmg.common.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


public class QprofilerXmlUtils {
	
	public static final String EMPTY = "EMPTY";
	public static final String TOTAL = "Total";
	public static final String UNKNOWN_READGROUP = "unkown_readgroup_id";
	public static final String All_READGROUP = "overall";	
	
	//summary
	public static final String readGroup = "readGroup";
	public static final String summary = "bamSummary";
	public static final String reads = "reads"; 
	public static final String readPairs= "readPairs"; 
	public static final String FirstOfPair = "firstReadInPair"; 
	public static final String SecondOfPair = "secondReadInPair";
	public static final String mdCycle = "mdMismatchCycles";	
	public static final String discardReads = "discardReads";
	public static final String fileReads = "fileReads";	
	public static final String duplicateReads = "duplicateReads";
	public static final String unmappedReads = 	"unmappedReads";
	public static final String nonCanonicalPair = "nonCanonicalPair";
	public static final String trimmedBase = "trimmedBase";
	public static final String softClippedBases = "softClippedBases";
	public static final String hardClippedBases = "hardClippedBases";
	public static final String overlapBases = "overlapBases";
	
	//debug will removed for new qprofiler2
	public static final String filteredReads = "filteredReads";
	public static final String basePercent = "basePercent";
	
 
	// tag name for old xml
	public static final String valueTally = "ValueTally";
	public static final String rangeTally = "RangeTally";
	public static final String cycleTally = "CycleTally";
	public static final String tallyItem = "TallyItem";
	public static final String rangeTallyItem = "RangeTallyItem";
	
	//count	
	public static final String source = "source";
	public static final String totalCount = "totalCount";
	public static final String totalBase = "totalBases";
	public static final String counts = "counts";
	public static final String count = "count";	
	public static final String readCount = "readCount";
	public static final String value = "value";
	public static final String percent = "percent";
	public static final String lostPercent = "basePercent";
	public static final String possibles = "possibleValues";
	public static final String start = "start";
	public static final String end = "end";	

	//commly used on fastq bam
	public static final String qname = "QNAME";
	public static final String flag = "FLAG";
	public static final String rname = "RNAME";
	public static final String pos = "POS";
	public static final String mapq = "MAPQ";
	public static final String cigar = "CIGAR";
	public static final String tlen = "TLEN";
	public static final String seq = "SEQ"; 
	public static final String tag = "TAG";
	public static final String cycle = "Cycle";	
	public static final String seqBase = "sequenceBase";
	public static final String seqLength = "sequenceLength";
	public static final String badBase = "badBase";
	public static final String qual = "QUAL";
	public static final String qualBase = "qualBase";
	public static final String qualLength = "qualLength";

		
			
	public static <T> String joinByComma( List<T> possibles){		 		
		StringBuilder sb = new StringBuilder();
		for(T mer :  possibles) {
			if( mer instanceof  String || mer instanceof Number || mer instanceof Character) {
				sb.append(mer).append(Constants.COMMA);	
			} 	
		}	
		return (sb.length() > 0)?  sb.substring(0, sb.length()-1) : "";		
	}

	/**
	 * Backup a file by renaming it where the renaming is based on appending (or
	 * incrementing) a version number extension. This could turn into a
	 * recursive process if the new names we come up with already exist in which
	 * case those files need to be renamed etc etc. To do the renaming we will
	 * add a numeric version number to the file and increment as needed.
	 * 
	 * @param filename name of file to be renamed with version number
	 */
	public static void backupFileByRenaming(String filename) throws Exception {
		
		Pattern fileVersionPattern = Pattern.compile("^(.*)\\.(\\d+)$");

		File origFile = new File(filename);
		
		// check that directory exists and is writable
		// this will throw an IOExcpetion if the file path is incorrect
		// if it returns true, do nowt, otherwise - rename existing file
		if (origFile.createNewFile()) {
			
			// delete the file straight away - don't want empty files lying around
			origFile.delete();
			
		} else {

//		 if file already exists, backup by renaming
			Matcher matcher = fileVersionPattern.matcher(origFile.getCanonicalPath());
			boolean matchFound = matcher.find();

			// Determine the name we will use to rename the current file
			String fileStem = null;
			Integer fileVersion = 0;
			if (!matchFound) {
				// Original filename has no version so create new filename by
				// appending ".1"
				fileStem = origFile.getCanonicalPath();
				fileVersion = 1;
			} else {
				// Original filename has version so create new filename by
				// incrementing version
				fileStem = matcher.group(1);
				fileVersion = Integer.parseInt(matcher.group(2)) + 1;
			}

			// If new filename already exists then we need to rename that file
			// also so let's use some recursion
			File newFile = new File(fileStem + "." + fileVersion);
			if (newFile.canRead())  
				backupFileByRenaming(newFile.getCanonicalPath());

			// Finally we get the rename origFile to newFile!
			if (!origFile.renameTo(newFile)) { 
				throw new RuntimeException("Unable to rename file from " + origFile.getName() + " to " + newFile.getName());
			}
		}
	}
	
	/**
	 * 
	 * @param parent
	 * @param tagName: The name of the tag to match on
	 * @return a list of Element of first generation child Elements with a given tag name and order
	 */
	public static List<Element> getChildElementByTagName(Element parent, String tagName){
		List<Element> elements = new ArrayList<Element>(); 
		if(parent == null ) return elements;
		
		NodeList children = parent.getChildNodes();
		for(int i = 0; i < children.getLength(); i ++){
			if( ! (children.item(i) instanceof Element))
				continue;
			
			if(children.item(i).getNodeName().equals(tagName)) {
				elements.add((Element) children.item(i));	
			}		
		}
		
		return elements; 		
	}
	/**
	 * 
	 * @param parent : parent element
	 * @param tagName: first generation children element name
	 * @param itemNo: the order of first generation children
	 * @return element if exists, otherwise return null
	 */
	public static  Element getChildElement(Element parent, String tagName, int itemNo){		
		List<Element> elements = getChildElementByTagName( parent,  tagName);
		return (itemNo >= elements.size()  )? null : elements.get(itemNo);		
	}
	
	/**
	 * 
	 * @param parent
	 * @param tagName: The name of the tag to match on
	 * @return a list of Element of all generation offspring Elements with a given tag name and order
	 */
	public static List<Element> getOffspringElementByTagName( Element parent, String tagName ){
				
		List<Element> elements = new ArrayList<Element>(); 
		if( parent == null ) return elements;
				
		NodeList offspring = parent.getElementsByTagName(tagName);
		for( int i = 0; i < offspring.getLength(); i ++ ){
			if( ! (offspring.item(i) instanceof Element) ) 	continue;			
			if(offspring.item(i).getNodeName().equals(tagName)) {
				elements.add((Element) offspring.item(i));	
				
			}		
		}
		
		return elements; 		
	}
	
	/**
	 * 
	 * @param parent: root element node
	 * @param filename : output element and offsprings to text file
	 */	
	public static void asXmlText( Element parent, String filename )  {		
	
		try{        
			DOMImplementationLS	impl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("XML 3.0 LS 3.0");	
	        LSSerializer serializer = impl.createLSSerializer();
	        LSOutput output = impl.createLSOutput();
	        output.setCharacterStream(new OutputStreamWriter(new FileOutputStream(filename)));
	        serializer.write(parent.getOwnerDocument(), output);
		}catch( ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException | IOException ex){
			ex.printStackTrace();
		}
		
	}
	

	/**
	 * create an top level element without appending to any URI, and it's child element if specified
	 * @param parentName: name for top level element 
	 * @param childName: The child name of the top element to be created or null.
	 * @return the top level element
	 * @throws ParserConfigurationException
	 */
	public static Element createRootElement(String parentName, String childName) throws ParserConfigurationException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();		
		Document doc = domImpl.createDocument(null, parentName, null);
		Element root = doc.getDocumentElement();
		
		if(childName == null) return root;
		
		return QprofilerXmlUtils.createSubElement(root, childName);	 
	}	

	public static Element createSubElement(Element parent, String name) {
		Element element = parent.getOwnerDocument().createElement(name);
		parent.appendChild(element);
		return element;
	}
	
}
