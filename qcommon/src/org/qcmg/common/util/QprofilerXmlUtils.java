package org.qcmg.common.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class QprofilerXmlUtils {
	
	public static final String EMPTY = "EMPTY";
	public static final String TOTAL = "Total";
	public static final String UNKNOWN_READGROUP = "unkown_readgroup_id";
	public static final String All_READGROUP = "overall";	
	public static final String COMMA = ","; 
	
	//xml tag name
	public static final String valueTally = "ValueTally";
	public static final String rangeTally = "RangeTally";
	public static final String cycleTally = "CycleTally";
//	public static final String lengthTally = "LengthTally";	
	
	public static final String tallyItem = "TallyItem";
	public static final String rangeTallyItem = "RangeTallyItem";

	
	//count	
	public static final String totalCount = "totalCount";
	public static final String totalBase = "totalBases";
	public static final String counts = "counts";
	public static final String count = "count";	
	public static final String value = "value";
	public static final String percent = "percent";
	public static final String possibles = "possibleValues";
	public static final String start = "start";
	public static final String end = "end";
	
	
	
		
	public static <T> String joinByComma( List<T> possibles){		 		
		StringBuilder sb = new StringBuilder();
		for(T mer :  possibles)
			if( mer instanceof  String || mer instanceof Number || mer instanceof Character)
			sb.append(mer).append(QprofilerXmlUtils.COMMA);			
		return (sb.length() > 0)?  sb.substring(0, sb.length()-COMMA.length()) : "";				
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
			if (!origFile.renameTo(newFile))  
				throw new RuntimeException("Unable to rename file from " + origFile.getName() + " to " + newFile.getName());
			 
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
			
			if(children.item(i).getNodeName().equals(tagName))
				elements.add((Element) children.item(i));
			
		}
		
		return elements; 		
	}
	
	public static  Element getChildElement(Element parent, String tagName, int itemNo){		
		List<Element> elements = getChildElementByTagName( parent,  tagName);
		
		
		return (itemNo >= elements.size()  )? null : elements.get(itemNo);		
	}
	
}
