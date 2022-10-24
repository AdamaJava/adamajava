/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package org.qcmg.picard;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.qcmg.common.string.StringUtils;

import htsjdk.samtools.reference.FastaSequenceFile;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.reference.ReferenceSequenceFile;

public class ReferenceUtils {
	
	private static ConcurrentMap<String, byte[]> REFERENCE;
	
	
	public static byte[] getReferenceBases(File referenceFile, String chr, int start, int end) {
		ConcurrentMap<String, byte[]> refMap = getReferenceMap(referenceFile);
		
		// arrays are zero based, and the copyOfRange specifies that the end is exclusive
		// so we leave end alone, and decrement start
		start--;
		
		return Arrays.copyOfRange(refMap.get(chr), start, end);
	}
	
	public synchronized static ConcurrentMap<String, byte[]> getReferenceMap(File referenceFile) {
		if (null == REFERENCE) {
			setupReferenceMap(referenceFile);
		}
		return REFERENCE;
	}
	
	/**
	 * Called when reference map is no longer required
	 * perhaps to try and free up memory..
	 * 
	 */
	public static void tearDownReferenceMap() {
		if (null != REFERENCE) {
			REFERENCE.clear();
			REFERENCE = null;
		}
	}
	
	private static synchronized void setupReferenceMap(File referenceFile)  {
		if (null == REFERENCE) {
			REFERENCE = new ConcurrentHashMap<>();
			ReferenceSequenceFile f = getReferenceFile(referenceFile);
			if (f == null) {
				f = new FastaSequenceFile(referenceFile, true);
			}
			ReferenceSequence nextSeq = f.nextSequence();
			
			while (nextSeq != null) {
				if (null != nextSeq.getName() && null != nextSeq.getBases()) {
					REFERENCE.put(nextSeq.getName(), nextSeq.getBases());
				}
				nextSeq = f.nextSequence();
			}
		}
	}
	
	/**
	 * Gets the reference file.
	 *
	 * @param referenceFile the reference file
	 * @return the reference file
	 * @throws QSVException the qSV exception
	 */
	private static IndexedFastaSequenceFile getReferenceFile(File referenceFile) {

		if (referenceFile.exists()) {
			File indexFile = new File(referenceFile.getPath() + ".fai");		

			if ( ! indexFile.exists()) {
				return null;
			}

			FastaSequenceIndex index = new FastaSequenceIndex(indexFile);
			IndexedFastaSequenceFile f = new IndexedFastaSequenceFile(referenceFile, index);
			return f;
		}
		return null;
	}
	
	
	/**
	 * Doesn't use any of the caches - looks up the reference file every time!
	 * @param file
	 * @param contig
	 * @param start
	 * @param end
	 * @return
	 */
	public static byte[] getRegionFromReferenceFile(String file, String contig, int start, int end) {
		
		if (null == file || ! new File(file).exists()) {
			throw new IllegalArgumentException("Null or invalid file supplied to ReferenceUtils.getRegionFromReferenceFile: " + file);
		}
		if (StringUtils.isNullOrEmpty(contig)) {
			throw new IllegalArgumentException("Null or invalid chromosome supplied to ReferenceUtils.getRegionFromReferenceFile: " + contig);
		}
		if ( ! new File(file + ".fai").exists()) {
			throw new IllegalArgumentException("No index file available: " + file + ".fai");
		}
		/*
		 * setup the index, check to see if the index contains the contig
		 */
		FastaSequenceIndex index = new FastaSequenceIndex(new File(file + ".fai"));
		if ( ! index.hasIndexEntry(contig)) {
			throw new IllegalArgumentException("Chromosome " + contig + " is not present in reference file: " + file);
		}
		
		try (IndexedFastaSequenceFile refFile = new IndexedFastaSequenceFile(new File(file), index);) {
			ReferenceSequence refSeq = refFile.getSubsequenceAt(contig, start, end);
			return refSeq.getBases();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
