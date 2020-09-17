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
	
	public static Map<String, byte[]> referenceCache = new THashMap<>();
	
	public static String getRefFromChrStartStop(String refFile, String contig, int start, int stop) {
		
		byte[] ref = referenceCache.get(refFile + contig);
		if ( null == ref) {
			loadIntoReferenceCache(refFile, contig);
			ref = referenceCache.get(refFile + contig);
			if (null == ref) {
				//hmmm....
				System.err.println("Unable to load contig: " + contig + " into cache");
				System.out.println("Unable to load contig: " + contig + " into cache");
			}
		}
		if (start <= 0 || stop > ref.length) {
			System.out.println("ChrPosition goes beyond edge of contig: " + contig + ":" + start + "-" + stop + ", ref length: " + ref.length);
		}
		byte [] refPortion = Arrays.copyOfRange(referenceCache.get(contig), (start < 0 ? 0 : start), (stop >= ref.length ? ref.length - 1 : stop));
		String referenceSeq = new String(refPortion);
		
		return referenceSeq;
	}
	
	public static void loadIntoReferenceCache(String refFileName, String contig) {
//		String refFileName = "/reference/genomes/GRCh37_ICGC_standard_v2/indexes/BWAKIT_0.7.12/GRCh37_ICGC_standard_v2.fa";
		
		FastaSequenceIndex index = new FastaSequenceIndex(new File(refFileName + ".fai"));
		try (IndexedFastaSequenceFile refFile = new IndexedFastaSequenceFile(new File(refFileName), index);) {
//			if (null == dictionary) {
//				dictionary = refFile.getSequenceDictionary();
//			}
			ReferenceSequence refSeq = refFile.getSequence(contig);
			byte[] ref = refSeq.getBases();
			referenceCache.put(refFileName + contig, ref);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
