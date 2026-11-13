/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

public class ReferenceUtil {
	
	/*
	 * read without locking by many threads, so must be a concurrent map
	 * (a THashMap can return inconsistent results if read while another thread is adding to it)
	 */
	public static final Map<String, byte[]> referenceCache = new ConcurrentHashMap<>();
	
	public static String getRefFromChrStartStop(String refFile, String contig, int start, int stop) {
		String refContig = refFile + contig;
		byte[] ref = referenceCache.get(refContig);
		if ( null == ref) {
			synchronized (ReferenceUtil.class) {
				/*
				 * check again to see if the referenceCache does not have this data as this may have been added by a different thread
				 */
				ref = referenceCache.get(refContig);
				if ( null == ref) {
					loadIntoReferenceCache(refFile, contig);
					ref = referenceCache.get(refContig);
				}
			}
			if (null == ref) {
				//hmmm....
				System.err.println("Unable to load contig: " + contig + " into cache");
				System.out.println("Unable to load contig: " + contig + " into cache");
				throw new IllegalArgumentException("Unable to load contig: " + contig + " into cache");
			}
		}
		
		if (start <= 0 || stop > ref.length) {
			System.out.println("ChrPosition goes beyond edge of contig: " + contig + ":" + start + "-" + stop + ", ref length: " + ref.length);
		}
		/*
		 * same as new String(Arrays.copyOfRange(ref, from, to)), but without the intermediate copy (these can be up to 500kb)
		 */
		int from = start < 0 ? 0 : start;
		int to = stop >= ref.length ? ref.length - 1 : stop;
		if (from > to) {
			throw new IllegalArgumentException(from + " > " + to);		// as thrown by Arrays.copyOfRange
		}
		return new String(ref, from, to - from);
	}
	
	private static void loadIntoReferenceCache(String refFileName, String contig) {
		
		FastaSequenceIndex index = new FastaSequenceIndex(new File(refFileName + ".fai"));
		try (IndexedFastaSequenceFile refFile = new IndexedFastaSequenceFile(new File(refFileName), index);) {
			ReferenceSequence refSeq = refFile.getSequence(contig);
			byte[] ref = refSeq.getBases();
			referenceCache.put(refFileName + contig, ref);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
