package org.qcmg.picard;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.picard.reference.FastaSequenceFile;
import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;
import net.sf.picard.reference.ReferenceSequenceFile;

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

}
