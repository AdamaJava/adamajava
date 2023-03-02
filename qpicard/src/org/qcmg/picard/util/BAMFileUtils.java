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
	 * picard BAM index name created by replacing .bam to .bai. But we want to have index name end with .bam.bai;
	 * picard CRAM index name created by adding .bai to .cram. But we want to have index name end with .cram.crai
	 * @param input is a BAM or CRAM file
	 * @throws IOException
	 */
	public static void renameIndex(File input) throws IOException {
		if( input == null ) throw new IllegalArgumentException("we can't rename a NULL file!");
		
		String path = input.getPath();
		final String indexFileBase = path.substring(0, path.lastIndexOf('.'));			
		Path indexpicard = null;
		Path indexqcmg = null;
		
		//do nothing if not BAM file
		if( path.endsWith(FileExtensions.BAM)) {
			//rename
		    indexpicard = IOUtil.addExtension(Paths.get(indexFileBase), FileExtensions.BAI_INDEX);
		    indexqcmg =  IOUtil.addExtension(Paths.get(path), FileExtensions.BAI_INDEX);			
		} else if( path.endsWith(FileExtensions.CRAM)) {
		    indexpicard = IOUtil.addExtension(Paths.get(path), FileExtensions.BAI_INDEX);
		    indexqcmg =  IOUtil.addExtension(Paths.get(path), FileExtensions.CRAM_INDEX);						
		}  			
		 			
		//rename files
		if(indexpicard != null && indexqcmg != null) {
			Files.move(indexpicard, indexqcmg, StandardCopyOption.REPLACE_EXISTING);
		}		
	}
}
