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

	public static final String CRAM_INDEX_SUFFIX = FileExtensions.CRAM + FileExtensions.CRAM_INDEX;
	public static final String BAM_INDEX_SUFFIX = FileExtensions.BAM + FileExtensions.BAI_INDEX;

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
	@Deprecated
	public static void renameIndex(File input) throws IOException {
		if( input == null ) throw new IllegalArgumentException("we can't rename a NULL file!");

		String path = input.getPath();
		Path pPath = input.toPath();
		final String indexFileBase = path.substring(0, path.lastIndexOf('.'));
		Path indexPicard = null;
		Path indexQcmg = null;

		//do nothing if not BAM file
		if (path.endsWith(FileExtensions.BAM)) {
			//rename
			indexPicard = IOUtil.addExtension(Paths.get(indexFileBase), FileExtensions.BAI_INDEX);
			indexQcmg =  IOUtil.addExtension(pPath, FileExtensions.BAI_INDEX);
		} else if (path.endsWith(FileExtensions.CRAM)) {
			indexPicard = IOUtil.addExtension(pPath, FileExtensions.BAI_INDEX);
			indexQcmg =  IOUtil.addExtension(pPath, FileExtensions.CRAM_INDEX);
		}

		//rename files
		if (indexPicard != null && indexQcmg != null) {
			Files.move(indexPicard, indexQcmg, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Will rename the originalIndex with an updated suffix should the original index not end in .bam.bai or .cram.crai
	 *
	 * Not sure this method will do much work anymore as the version of htsjdk currently being used (4.0.2) does this out of the box
	 *
	 * @param bamOrCramFile
	 * @param originalIndex
	 * @throws IOException
	 */
	public static void renameIndex(File bamOrCramFile, File originalIndex) throws IOException {
		if (bamOrCramFile == null  || ! bamOrCramFile.exists()) {
			throw new IllegalArgumentException("BAM or CRAM file corresponding to index to be renamed must not be null or empty");
		}

		if (originalIndex != null  && originalIndex.exists()) {

			String bamOrCramFileName = bamOrCramFile.getName();
			String originalIndexName = originalIndex.getName();
			String toReplace = null;

			if (bamOrCramFileName.endsWith(FileExtensions.BAM)) {
				if (!originalIndexName.endsWith(BAM_INDEX_SUFFIX)) {
					toReplace = BAM_INDEX_SUFFIX;
				}
			} else if (bamOrCramFileName.endsWith(FileExtensions.CRAM)) {
				if (!originalIndexName.endsWith(CRAM_INDEX_SUFFIX)) {
					toReplace = CRAM_INDEX_SUFFIX;
				}
			}

			if (null != toReplace) {
				final int index = originalIndexName.lastIndexOf('.');
				String originalIndexNameMinusSuffix = (index > 0 && index > originalIndexName.lastIndexOf(File.separator)) ? originalIndexName.substring(0, index) : originalIndexName;
			/*
			check to see if we end with either .cram or .bam - if so, change "toReplace" to just include the index part
			 */
				if (originalIndexNameMinusSuffix.endsWith(FileExtensions.CRAM)) {
					toReplace = FileExtensions.CRAM_INDEX;
				} else if (originalIndexNameMinusSuffix.endsWith(FileExtensions.BAM)) {
					toReplace = FileExtensions.BAI_INDEX;
				}
				Path updatedIndex = IOUtil.addExtension(Paths.get(originalIndex.getParentFile().getPath(), originalIndexNameMinusSuffix), toReplace);

				Files.move(originalIndex.toPath(), updatedIndex, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
