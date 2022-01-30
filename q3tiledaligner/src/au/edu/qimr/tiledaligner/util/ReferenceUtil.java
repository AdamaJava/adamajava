/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2020.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */

package au.edu.qimr.tiledaligner.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import gnu.trove.map.hash.THashMap;
import htsjdk.samtools.reference.FastaSequenceIndex;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;

public class ReferenceUtil {
	
	public static final Map<String, byte[]> referenceCache = new THashMap<>();
	
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
		byte [] refPortion = Arrays.copyOfRange(referenceCache.get(refContig), (start < 0 ? 0 : start), (stop >= ref.length ? ref.length - 1 : stop));
		
		return new String(refPortion);
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
