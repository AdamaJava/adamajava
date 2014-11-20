/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.snp.util;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.sf.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.QSnpGATKRecord;
import org.qcmg.picard.QJumper;
import org.qcmg.pileup.QSnpRecord;

public class QJumperWorker<T> implements Runnable {
	
	public enum Mode {
		VCF,
		QSNP,
		QSNP_MUTATION_IN_NORMAL
	}
	
	private final CountDownLatch latch;
//	private  Map<ChrPosition, QSnpGATKRecord>  vcfs;
	private Deque<T> primaryQueue;
	private Deque<T> workStealingQueue;
	private String primaryBamFile;
	private String workStealingBamFile;
//	private  Map<ChrPosition, QSnpRecord>  snps;
	private final QJumper  qj = new QJumper();
	private final Mode mode;
	private int count, updatedPileup;
	
	private final QLogger logger = QLoggerFactory.getLogger(QJumperWorker.class);

//	public QJumperWorker(CountDownLatch latch, String bamFile, Map<ChrPosition, QSnpGATKRecord> vcfs) {
//		this.latch = latch;
////		this.vcfs = vcfs;
//		this.qj.setupReader(bamFile);
//		this.mode = Mode.VCF;
//	}
	
//	public QJumperWorker(CountDownLatch latch, String bamFile, Map<ChrPosition, QSnpRecord> snps, Mode mode) {
//		this.latch = latch;
////		this.snps = snps;
//		this.qj.setupReader(bamFile);
//		this.mode = mode;
//	}
	
	public QJumperWorker(CountDownLatch latch, String primaryBamFile, Deque<T> primaryQueue,
			String wsBamFile, Deque<T> wsQueue, Mode mode) {
		this.latch = latch;
		this.primaryBamFile = primaryBamFile;
		this.primaryQueue = primaryQueue;
		this.workStealingBamFile = wsBamFile;
		this.workStealingQueue = wsQueue;
		this.mode = mode;
	}
	
//	public QJumperWorker(CountDownLatch latch, String primaryBamFile, Deque<T> primaryQueue,
//			String wsBamFile, Deque<T> wsQueue) {
//		this(latch, primaryBamFile, primaryQueue, wsBamFile, wsQueue, Mode.VCF);
//	}
	
	public QJumperWorker(CountDownLatch latch, String primaryBamFile, Deque<T> primaryQueue, Mode mode) {
		this(latch, primaryBamFile, primaryQueue, null, null, mode);
	}
	
	@Override
	public void run() {
		logger.info("QJumperWorker thread started... mode: " + mode);
		try {
			
			// setup qj for primary bam file
			qj.setupReader(primaryBamFile);
			if (null != primaryQueue) {
			
				while (true) {
					
					T record = primaryQueue.pollFirst();
					if (null != record) {
						getPileup(record);
					} else {
						// work stealing mode
						if (null == workStealingQueue) break;
						
						record = workStealingQueue.pollLast();	// hit end of queue to minimise contention
						if (null == record) break;
						
						// close old reader
						qj.closeReader();
						
						logger.info("Switching to work steal mode");
						
						// setup qj for work stealing bam file
						qj.setupReader(workStealingBamFile);
						while (true) {
							getPileup(record);
							
							record = workStealingQueue.pollLast();
							if (null == record) break;
						}
					}
				}
			}
			
		} catch (Exception e) {
			logger.error("Exception caught in QJumperWorker thead", e);
			throw new RuntimeException("Exception caught in QJumperWorker thead", e);
		} finally {
			latch.countDown();
		}
		logger.info("QJumperWorker thread - DONE - pileup fetched for " + count + " positions");
	}
	
	private void getPileup(T record) throws Exception {
		switch (mode) {
		case VCF:
				getGatkPileup((QSnpGATKRecord) record) ;
				break;
		case QSNP:
		case QSNP_MUTATION_IN_NORMAL:
				getQsnpPileup((QSnpRecord) record) ;
				break;
		}
	}
	
	private void getGatkPileup(QSnpGATKRecord gatkRec) throws Exception {
		List<SAMRecord> samRecords = qj.getOverlappingRecordsAtPosition(gatkRec.getChromosome(), gatkRec.getPosition());
		BAMPileupUtil.examinePileupVCF(samRecords, gatkRec);
		if (++count % 1000 == 0) {
			logger.info("hit " + count + " vcf positions");
		}
		if (null != gatkRec.getPileup() && gatkRec.getPileup().size() > 0)
			++updatedPileup;
	}
	
	private void getQsnpPileup(QSnpRecord qsnp) throws Exception {
		List<SAMRecord> samRecords = qj.getOverlappingRecordsAtPosition(qsnp.getChromosome(), qsnp.getPosition(), qsnp.getPosition());
		BAMPileupUtil.examinePileupSNP(samRecords, qsnp, mode);
		
		if (++count % 1000 == 0) {
			logger.info("hit " + count + " qsnp positions");
		}
		if (null != qsnp.getNormalNucleotides() && null != qsnp.getTumourNucleotides())
			++updatedPileup;
	}
	

//	public void run() {
//		logger.info("QJumperWorker thread started... mode: " + mode);
//		int count = 0, updatedPileup = 0;
//		try {
//			if (Mode.VCF == mode) {
//			
//				Map<ChrPosition, QSnpGATKRecord> map = new TreeMap<ChrPosition, QSnpGATKRecord>(vcfs);
//				
//				for (QSnpGATKRecord gatkRec : map.values()) {
//					
//					List<SAMRecord> samRecords = qj.getOverlappingRecordsAtPosition(gatkRec.getChromosome(), gatkRec.getPosition());
//					BAMPileupUtil.examinePileupVCF(samRecords, gatkRec);
//					if (++count % 1000 == 0) {
//						logger.info("hit " + count + " vcf positions");
//					}
//					if (null != gatkRec.getPileup() && gatkRec.getPileup().size() > 0)
//						++updatedPileup;
//				}
//			} else {
//				Map<ChrPosition, QSnpRecord> map = new TreeMap<ChrPosition, QSnpRecord>(snps);
//				
//				for (QSnpRecord qsnp : map.values()) {
//					
//					List<SAMRecord> samRecords = qj.getOverlappingRecordsAtPosition(qsnp.getChromosome(), qsnp.getPosition(), qsnp.getPosition());
//					BAMPileupUtil.examinePileupSNP(samRecords, qsnp, mode);
//					
//					if (++count % 1000 == 0) {
//						logger.info("hit " + count + " qsnp positions");
//					}
//					if (null != qsnp.getNormalNucleotides() && null != qsnp.getTumourNucleotides())
//						++updatedPileup;
//				}
//				
//			}
//		} catch (Exception e) {
//			logger.error("Exception caught in QJumperWorker thead", e);
////			Thread.currentThread().interrupt();
//			throw new RuntimeException("Exception caught in QJumperWorker thead", e);
//		} finally {
//			latch.countDown();
//		}
//		logger.info("QJumperWorker thread - DONE - pileup fetched for " + count + " positions");
//	}

}
