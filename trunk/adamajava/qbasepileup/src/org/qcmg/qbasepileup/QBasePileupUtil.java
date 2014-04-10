/**
 * �� Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qbasepileup;

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

public class QBasePileupUtil {
	
//	public static final Pattern SINGLE_DIGIT_PATTERN = Pattern.compile("\\d");
	public static final Pattern DOUBLE_DIGIT_PATTERN = Pattern.compile("\\d{1,2}");
	
	public static String getStrackTrace(Exception e) {
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
	}
	
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
	
	public static String printStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
	}

	public static int[] parseDCCHeader(List<String> headers) throws QBasePileupException {
		int[] cols = {-1, -1, -1, -1, -1};
		for (String header: headers) {
			
			String[] values = header.split("\t");
			if (values.length == 28) {
				//check dcc header
				for (int i=0; i<values.length; i++) {
					if (values[i].toLowerCase().contains("qcmgflag")) {
						cols[0] = i;
					}
					if (values[i].toLowerCase().equals("nd")) {
						cols[1] = i;
					}
					if (values[i].toLowerCase().equals("td")) {
						cols[2] = i;
					}
					if (values[i].toLowerCase().equals("reference_genome_allele")) {
						cols[3] = i;
					}
					if (values[i].toLowerCase().equals("tumour_genotype")) {
						cols[4] = i;
					}
				}
			}
		}
		for (int i=0; i<cols.length; i++) {
			
			if (cols[i] == -1) {
				String type = "QCMGFlag";
				if (i == 1) {
					type = "ND";
				}
				if (i == 2) {
					type = "TD";
				}
				if (i == 3) {
					type = "reference_genome_allele";
				}
				if (i == 4) {
					type = "tumour_genotype";
				}
				throw new QBasePileupException("DCC_PARSE_ERROR", type);
			}
		}
		return cols;
	}
	
	public static String getSequenceString(byte[] sequence) {
		StringBuilder sb = new StringBuilder();
		for (byte b: sequence) {
			sb.append((char) b);
		}
		return sb.toString();
	}

	public static String[] getSNPPositionColumns(String format, String[] values, long count) {
		
		if (format.equals("dcc1") || format.equals("maf")) {
			String[] columns = {values[2], values[4], values[5], values[6]};
			return columns;			
    	} else if (format.equals("dccq")) {
    		String[] columns = {values[0], values[2], values[3], values[4]};
			return columns;	    		
    	} else if (format.equals("vcf")) {
    		String[] columns = {values[2], values[0], values[1], values[1]};
			return columns;	  
    	}  else if (format.equals("tab") || format.equals("columns")) {  
    		String[] columns = {values[0], values[1], values[2], values[3]};
			return columns;
    	} else if (format.equals("hdf")) {
    		String[] columns = {Long.toString(count), values[0], values[1], values[2]};
			return columns;
    	}  	
		return null;
	}

	public static byte[] getCompoundAltBases(String[] values, int index) throws QBasePileupException {
		try {
			byte[] bases = values[index].split(">")[1].getBytes();
			return bases;
		} catch (Exception e) {
			throw new QBasePileupException("NO_MUTATION");
		}
	}
	
	public static int[] getMafColumns(String headers) throws QBasePileupException {
		String[] values = headers.split("\t");
		
			int refAllele = -1;
			int tumourAllele1 = -1;
			int tumourAllele2 = -1;
			for (int i=0; i<values.length; i++) {
				String s = values[i];
				if (s.equalsIgnoreCase("Reference_Allele")) {
					refAllele = i;
				}
				if (values[i].equalsIgnoreCase("Tumor_Seq_Allele1")) {
					tumourAllele1 = i;			
				}
				if (s.equalsIgnoreCase("Tumor_Seq_Allele2")) {
					tumourAllele2 = i;
				}
			}
			if (refAllele == -1 || tumourAllele1== -1 || tumourAllele2== -1) {
				throw new QBasePileupException("NO_MAF_ALT_BASES" + headers);
			}
			
		int[] arr = new int[3];
		arr[0] = refAllele;
		arr[1] = tumourAllele1;
		arr[2] = tumourAllele2;
		return arr;
	}
	



	public static String getStandardSnpHeader() {
		String TAB = "\t";
		return "ID" +TAB + "Donor" +TAB + "Bam" +TAB + "SnpId" +TAB + "Chromosome" +TAB + "Start" +TAB + "" +
				"End" +TAB + "RefBase" +TAB + "TotalRef" +TAB + "TotalNonRef" +TAB + "Aplus" +TAB + "Cplus" +TAB + "Gplus" +TAB + 
				"Tplus" +TAB + "Nplus" +TAB + "TotalPlus" +TAB + 
				"Aminus" +TAB + "Cminus" +TAB + "Gminus" +TAB + "Tminus" +TAB + "Nminus" +TAB + "TotalMinus\n";
	}
	
	public static String getCompoundSnpHeader() {
		String TAB = "\t";
		return "ID" +TAB + "Donor" +TAB + "Bam" +TAB + "SnpId" +TAB + "Chromosome" +TAB + "Start" +TAB + "" +
		"End" +TAB + "RefBase" +TAB + "ExpectedAltBase" +TAB + "TotalPlus" +TAB + "TotalRefPlus" +TAB + "TotalExpectedAltPlus" +TAB + "TotalOtherAltPlus" +TAB + "TotalMinus" +TAB + "TotalRefMinus" +TAB + 
		"TotalExpectedAltMinus" +TAB + "TotalOtherAltMinus\n";
	}

	public static int getMutationColumn(String line) {
		String[] values = line.split("\t");
		int col = -1;
		for (int i=0; i<values.length; i++) {
			if (values[i].equalsIgnoreCase("mutation")) {
				col = i;
			}
		}
		return col;
	}

	public static List<String> getHeaderLines(File file) throws IOException {
		List<String> headers = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String line;
		while ((line=reader.readLine()) != null) {
			if ((line.startsWith("Hugo") || line.startsWith("#") || line.startsWith("analysis_id"))) {
					headers.add(line);
			}
		}
		reader.close();
		
		return headers;
	}

	public static String getColumnsSnpHeader(List<InputBAM> inputBAMs) {
		StringBuilder sb = new StringBuilder();
		String TAB = "\t";
		
		sb.append(TAB + TAB + TAB + TAB + TAB + TAB);
		for (InputBAM input : inputBAMs) {
			sb.append(input.getAbbreviatedBamFileName() + TAB + TAB + TAB + TAB + TAB);
		}
		sb.append("\n");
		sb.append("#gene\tchromosome\tstart\tend\tref\talt\t");
		for (int i=0; i<inputBAMs.size(); i++) {
			sb.append("A\tC\tG\tT\tN\t");
		}
		sb.append("\n");
		return sb.toString();
	}




}
