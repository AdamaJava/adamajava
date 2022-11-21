/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

import org.qcmg.picard.SAMFileReaderFactory;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.util.FileExtensions;
import htsjdk.samtools.util.IOUtil;

public class BAMFileUtils {
	
	public static boolean bamFileIsLocked(File bamFile) {
		if (bamFile != null) {
			File bamlock = new File (bamFile.getAbsolutePath() + ".lck");
			
			if (bamlock.exists()) {
				return true;
			}
			
			File indexLock = new File (bamFile.getAbsolutePath() + ".bai.lck");
			
			if (indexLock.exists()) {
				return true;
			}
		}
		return false;
	}
	
	public static List<String> getContigsFromBamFile(File bamFile) throws IOException {
		try (SamReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile)) {
			return getContigsFromHeader(reader.getFileHeader());
		}
	}
	
	public static List<String> getContigsFromHeader(SAMFileHeader header) {
		return header.getSequenceDictionary().getSequences()
				.stream()
				.map(SAMSequenceRecord::getSequenceName)
				.collect(Collectors.toList());
	}

	/**
	 * picard index name created by replacing .bam to .bai. But we want to have index name end with .bam.bai
	 * @param bamFile
	 * @throws Exception 
	 */	
	public static void renameBamIndex(File input) throws IOException{
		
		if( input == null ) throw new IOException("we can't rename a NULL file!");
		
		String path = input.getPath();
		
		//do nothing if not BAM file
		if( !path.endsWith(FileExtensions.BAM)) return;
	
		final String indexFileBase = path.substring(0, path.lastIndexOf('.'));	
			
		//rename
	    final Path indexpicard = IOUtil.addExtension(Paths.get(indexFileBase), FileExtensions.BAI_INDEX);
	    final Path indexqcmg =  IOUtil.addExtension(Paths.get(path), FileExtensions.BAI_INDEX);
		 			
		//rename files
		Files.move(indexpicard, indexqcmg, StandardCopyOption.REPLACE_EXISTING);
		
	}
	/**
	 * picard index name created by adding .bai to .cram. But we want to have index name end with .cram.crai
	 * @param cramFile
	 * @throws Exception 
	 */	
	
	public static void renameCramIndex(File input) throws IOException{
		
		if( input == null ) throw new IOException("we can't rename a NULL file!");
		
		String path = input.getPath();
		//do nothing if not cram file
		if( !path.endsWith(FileExtensions.CRAM)) return;
		
		//rename
	    final Path indexpicard = IOUtil.addExtension(Paths.get(path), FileExtensions.BAI_INDEX);
	    final Path indexqcmg =  IOUtil.addExtension(Paths.get(path), FileExtensions.CRAM_INDEX);
		 			
		//rename files
		Files.move(indexpicard, indexqcmg, StandardCopyOption.REPLACE_EXISTING);				
	}
	
	
	
	

}
