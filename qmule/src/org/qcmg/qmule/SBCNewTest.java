/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.util.AbstractQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import htsjdk.samtools.SAMRecord;

import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.model.SummaryByCycleNew2;
import org.qcmg.common.util.SummaryByCycleUtils;

public class SBCNewTest {
	
	private final static Character C = 'c';
	private final static Integer I = 1;
	private final static int NO_OF_PROCESORS = Runtime.getRuntime().availableProcessors();
	
	/**
	 * SAMRecord taken from Torrent run: 
	 * /mnt/seq_results/smgres_special/targetseq29/seq_mapped/R_2011_09_29_14_12_44_user_T01-55_Analysis_T01-55.sorted.bam
	 * 
	 * 
	JV5US:1593:2430	16	chr1	11166612	44	15H134M	*	0	0	CATTTATTGGCACAAAAATTATTCTGATACAACATGGTGTCTAGACATGGCTACACTTTATACTTTGTGCATTTAGTTGAGTATTTGTTCTGCTCATAATTTCCAATATGTACCAGACCTTCCCTGTGTTCAGC	CC6CCB:>
	7=?AC/EEEA9A@8C<==6866:A=AA;>?>CCEEECAA?7?<;:<<4.<5386;2AA>?:<:5CDC=5@=3164-?;?=C@@@CCCD=:?:EE?D;@AAA=+.*A532.,+*'222?>@<CCCC;	RG:Z:JV5US	PG:Z:tmap	MD:Z:134	NM:i:0	AS:i:134	FZ:B:S,110,0,87,8,0,88,4,187,0,1
	00,116,93,214,0,109,98,0,107,0,107,8,11,345,238,0,209,112,0,84,0,5,15,91,5,0,216,145,98,112,0,0,0,1,0,94,0,0,0,103,0,0,118,207,0,199,305,227,0,6,105,0,0,0,0,103,0,0,107,2,85,1,98,6,88,0,4,79,104,3,199,0,110,7,323,66,0,14,119,11,0,0,
	4,64,10,0,5,86,0,96,0,0,217,95,0,104,1,9,14,306,0,11,0,94,0,72,15,0,73,4,97,12,0,0,0,94,319,100,0,84,136,0,0,106,329,6,73,92,0,1,80,13,9,4,9,91,0,5,119,0,16,88,0,15,0,6,10,216,101,0,18,107,14,2,88,98,0,95,11,97,9,11,0,104,95,4,109,0
	,103,4,124,0,203,14,82,107,80,210,0,0,0,91,15,67,138,0,0,89,4,115,1,0,8,17,0,85,94,0,192,84,0,0,182,502,2,88,16,102,104,12,3,177,203,0,12,98,288,5,0,98,0,0,111,41,0,1,0,101,12,88,132,114,98,0,93,42,9,107,214,100,0,0,0,101,102,10,190
	,2,16,0,87	XA:Z:map3-1	XS:i:18	XT:i:37	ZS:i:11166562	ZE:i:11166795
	 */
	
	
	
	private  SAMRecord sam;
	
	private final AbstractQueue<SAMRecord> samQueue = new ConcurrentLinkedQueue<SAMRecord>();
	
	private final SummaryByCycle<Character> oldBases = new SummaryByCycle<Character>();
	private final SummaryByCycle<Integer> oldQuals = new SummaryByCycle<Integer>();
	
	private final SummaryByCycleNew2<Character> newBases = new SummaryByCycleNew2<Character>(C, 512);
	private final SummaryByCycleNew2<Integer> newQuals = new SummaryByCycleNew2<Integer>(I, 512);
	
//	private final SummaryByCycleNew<Character> intermediateBases = new SummaryByCycleNew<Character>(C, 512);
//	private final SummaryByCycleNew<Integer> intermediateQuals = new SummaryByCycleNew<Integer>(I, 512);
	
	private enum Method {
		OLD,
		INTERMEDIATE,
		NEW;
	}
	

	private void setupSamRecord() {
		sam = new SAMRecord(null);
		sam.setReadName("JV5US:1593:2430");
		sam.setReferenceName("chr1");
		sam.setFlags(16);
		sam.setAlignmentStart(11166612);
		sam.setMappingQuality(44);
		sam.setCigarString("15H134M");
		sam.setMateReferenceName("*");
		sam.setMateAlignmentStart(0);
		
		// read info
		sam.setReadString("CATTTATTGGCACAAAAATTATTCTGATACAACATGGTGTCTAGACATGGCTACACTTTATACTTTGTGCATTTAGTTGAGTATTTGTTCTGCTCATAATTTCCAATATGTACCAGACCTTCCCTGTGTTCAGC");
		sam.setBaseQualityString("CC6CCB:>7=?AC/EEEA9A@8C<==6866:A=AA;>?>CCEEECAA?7?<;:<<4.<5386;2AA>?:<:5CDC=5@=3164-?;?=C@@@CCCD=:?:EE?D;@AAA=+.*A532.,+*'222?>@<CCCC;");
		
		if (sam.getReadLength() != 134) {
			System.out.println("read length in sam record does not equal 134!!");
		}
	}
	
	private void setupBamFile(int noOfRecords) {
		if (null == sam) throw new IllegalStateException("SAM Record has not ye been initialised");
		
//		File tmpBam = tempFolder.newFile("tmpBam");
//		SAMFileWriterFactory factory = new SAMFileWriterFactory();
//		SAMFileWriter writer = factory.makeBAMWriter(null, true, tmpBam);
		
//		try {
			for (int i = 0 ; i < noOfRecords ; i++) {
				samQueue.add(sam);
//				writer.addAlignment(sam);
			}
//		} finally {
//			writer.close();
//		}
	}
	
	private void setup() {
		// setup sam record
		setupSamRecord();
		
		int noOfRecords = 10000000;
		// write to bam file
		setupBamFile(noOfRecords);
	}
	
	
	private void test(final  Method method) {;
		ExecutorService executor = Executors.newFixedThreadPool(NO_OF_PROCESORS);
		long start = System.currentTimeMillis();
		for (int i = 0 ; i < NO_OF_PROCESORS ; i++) {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					// get SAMRecord
					while (true) {
						SAMRecord sam = samQueue.poll();
						if (null == sam) break;
						
						if (Method.NEW == method) {
							// SEQ
							SummaryByCycleUtils.parseCharacterSummary(newBases , sam.getReadString());
							// QUAL
							SummaryByCycleUtils.parseIntegerSummary(newQuals, sam.getBaseQualities());
						} else if (Method.INTERMEDIATE == method) {
							// SEQ
//							SummaryByCycleUtils.parseCharacterSummary(intermediateBases , sam.getReadString());
//							// QUAL
//							SummaryByCycleUtils.parseIntegerSummary(intermediateQuals, sam.getBaseQualities());
						} else if (Method.OLD == method) {
							// SEQ
							SummaryByCycleUtils.parseCharacterSummary(oldBases , sam.getReadString());
							// QUAL
							SummaryByCycleUtils.parseIntegerSummary(oldQuals, sam.getBaseQualities());
						}
					}
				}
			});
		}
		executor.shutdown();
		try {
			executor.awaitTermination(10, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		executor.shutdownNow();
		
		System.out.println("method: " + method + " : " + (System.currentTimeMillis() - start));
		
	}
	
	private void testResults() {
		
		if ( ! oldBases.getPossibleValuesAsString().equals(newBases.getPossibleValuesAsString())) {
			System.out.println("getPossibleValuesAsString() do not match!!");
			System.out.println(oldBases.getPossibleValuesAsString());
			System.out.println(newBases.getPossibleValuesAsString());
		}
		if ( ! oldQuals.getPossibleValuesAsString().equals(newQuals.getPossibleValuesAsString())) {
			System.out.println("getPossibleValuesAsString() do not match!!");
			System.out.println(oldQuals.getPossibleValuesAsString());
			System.out.println(newQuals.getPossibleValuesAsString());
		}
//		if ( ! oldQuals.getPossibleValuesAsString().equals(intermediateQuals.getPossibleValuesAsString())) {
//			System.out.println("getPossibleValuesAsString() do not match!!");
//			System.out.println(oldQuals.getPossibleValuesAsString());
//			System.out.println(intermediateQuals.getPossibleValuesAsString());
//		}
		
		if ( ! oldQuals.cycles().equals(newQuals.cycles())) {
			System.out.println("cycles() do not match!!");
			System.out.println(oldQuals.cycles());
			System.out.println(newQuals.cycles());
		}
		if ( ! oldBases.cycles().equals(newBases.cycles())) {
			System.out.println("cycles() do not match!!");
			System.out.println(oldBases.cycles());
			System.out.println(newBases.cycles());
		}
//		if ( ! oldBases.cycles().equals(intermediateBases.cycles())) {
//			System.out.println("cycles() do not match!!");
//			System.out.println(oldBases.cycles());
//			System.out.println(intermediateBases.cycles());
//		}
		
	}
	
	
	public static void main(String[] args) {
		SBCNewTest test = new SBCNewTest();
		test.setup();
		test.test(Method.NEW);
		test.setup();
		test.test(Method.INTERMEDIATE);
		test.setup();
		test.test(Method.OLD);
		test.testResults();
	}

}
