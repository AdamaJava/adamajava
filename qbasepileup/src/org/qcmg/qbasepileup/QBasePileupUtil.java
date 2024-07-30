/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
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

import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class QBasePileupUtil {
	
//	public static final Pattern SINGLE_DIGIT_PATTERN = Pattern.compile("\\d");
	public static final Pattern DOUBLE_DIGIT_PATTERN = Pattern.compile("\\d{1,2}");
	
	public static final char TAB = '\t';
	
	public static String getStackTrace(Exception e) {
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
	}
	
	public static String getFullChromosome(String ref) {
		
		
		// if ref starts with chr or GL, just return it
		if (ref.startsWith("chr") || ref.startsWith("GL")) {
			return ref;
		}
		
		if (addChromosomeReference(ref)) {
			return "chr" + ref;
		} else {
			return ref;
		}
	}
	
	public static boolean addChromosomeReference(String ref) {
		
		if (ref.startsWith("chr") || ref.startsWith("GL")) {
			return false;
		}
		
		if (ref.equals("X") || ref.equals("Y") || ref.equals("M") || ref.equals("MT")) {
			return true;
		}
		
		Matcher matcher = DOUBLE_DIGIT_PATTERN.matcher(ref);
		if (matcher.matches()) {
			return Integer.parseInt(ref) < 23;
		}
		return false;
	}
	
	public static FastaSequenceIndex getFastaIndex(File reference) {
		File indexFile = new File(reference.getAbsolutePath() + ".fai");	    		
		return new FastaSequenceIndex(indexFile);
	}
	
	public static IndexedFastaSequenceFile getIndexedFastaFile(File reference) {
		FastaSequenceIndex index = getFastaIndex(reference);

		return new IndexedFastaSequenceFile(reference, index);
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
					if (values[i].equalsIgnoreCase("nd")) {
						cols[1] = i;
					}
					if (values[i].equalsIgnoreCase("td")) {
						cols[2] = i;
					}
					if (values[i].equalsIgnoreCase("reference_genome_allele")) {
						cols[3] = i;
					}
					if (values[i].equalsIgnoreCase("tumour_genotype")) {
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

	/**
	 * Returns a String[] containing name, chr, start and end if format is dcc1
	 * 
	 * @param format
	 * @param values
	 * @param count
	 * @return
	 */
	public static String[] getSNPPositionColumns(String format, String[] values, long count) {

		return switch (format) {
			case "dcc1", "maf" -> new String[]{values[2], values[4], values[5], values[6]};
			case "dccq" -> new String[]{values[0], values[2], values[3], values[4]};
			case "vcf" -> new String[]{values[2], values[0], values[1], values[1]};
			case "tab", "columns" -> new String[]{values[0], values[1], values[2], values[3]};
			case "hdf" -> new String[]{Long.toString(count), values[0], values[1], values[2]};
			default -> null;
		};


	}

	public static byte[] getCompoundAltBases(String[] values, int index) throws QBasePileupException {
		try {
			return values[index].split(">")[1].getBytes();
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
		return "ID\tDonor\tBam\tSnpId\tChromosome\tStart\tEnd\tRefBase\tTotalRef\tTotalNonRef\tAplus\tCplus\tGplus\tTplus\tNplus\tTotalPlus\tAminus\tCminus\tGminus\tTminus\tNminus\tTotalMinus\n";
	}
	
	public static String getCompoundSnpHeader() {
		return "ID\tDonor\tBam\tSnpId\tChromosome\tStart\tEnd\tRefBase\tExpectedAltBase\tTotalPlus\tTotalRefPlus\tTotalExpectedAltPlus\tTotalOtherAltPlus\tTotalMinus\tTotalRefMinus\tTotalExpectedAltMinus\tTotalOtherAltMinus\n";
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
		return getHeaderLines(file, false);
	}
	
	public static List<String> getHeaderLines(File file, boolean includeMutation) throws IOException {
		List<String> headers = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line=reader.readLine()) != null) {
				if (line.startsWith("Hugo") || line.startsWith("#") || line.startsWith("analysis_id") || (includeMutation && line.startsWith("mutation"))) {
					headers.add(line);
				}
			}
		}
		return headers;
	}

	public static String getColumnsSnpHeader(List<InputBAM> inputBAMs) {
		StringBuilder sb = new StringBuilder();
		String TAAAAAB = "\t\t\t\t\t";
		
		sb.append(TAAAAAB);
		sb.append(TAB);
		for (InputBAM input : inputBAMs) {
			sb.append(input.getAbbreviatedBamFileName()).append(TAAAAAB);
		}
		sb.append("\n");
		sb.append("#gene\tchromosome\tstart\tend\tref\talt\t");
		sb.append("A\tC\tG\tT\tN\t".repeat(inputBAMs.size()));
		sb.append("\n");
		return sb.toString();
	}




}
