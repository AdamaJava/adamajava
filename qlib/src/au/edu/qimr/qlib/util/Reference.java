package au.edu.qimr.qlib.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.IndexedFastaSequenceFile;
public class Reference {
	public static final Pattern DOUBLE_DIGIT_PATTERN = Pattern.compile("\\d{1,2}");
	
	public static String getFullChromosome(String ref) {
		if (ref.equals("chrM") || ref.equals("M")) {
			return "chrMT";
		} else	if (addChromosomeReference(ref)) {			
			return "chr" + ref;
		} else {			
			return ref;
		}
	}
	
	public static boolean addChromosomeReference(String ref) {
		
		if (ref.equals("X") || ref.equals("Y") || ref.equals("M") || ref.equals("MT")) {
			return true;
		} else if ( ! ref.contains("chr")) {
			
			Matcher matcher = DOUBLE_DIGIT_PATTERN.matcher(ref);
			if (matcher.matches()) {		    	
				if (Integer.parseInt(ref) < 23) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static FastaSequenceIndex getFastaIndex(File reference) {
		File indexFile = new File(reference.getAbsolutePath() + ".fai");	    		
		return new FastaSequenceIndex(indexFile);
	}
		
	public static IndexedFastaSequenceFile getIndexedFastaFile(File reference) {
		FastaSequenceIndex index = getFastaIndex(reference);		
		IndexedFastaSequenceFile indexedFasta = new IndexedFastaSequenceFile(reference, index);
		
		return indexedFasta;
	}	

}
