/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.picard.reference.FastaSequenceIndex;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import au.edu.qimr.qlib.qpileup.*;
import au.edu.qimr.qlib.util.*;

public class MitoPileline {
	
	private QLogger logger = QLoggerFactory.getLogger(getClass());
	private int blockSize;
    private final int sleepUnit = 10;
    private int noOfThreads;	
	private String[] bamFiles;	
	private String outputFile;
	private String referenceFile;
//	private AtomicInteger exitStatus = new AtomicInteger(0);
	private ConcurrentHashMap<String, AtomicLong> totalSAMRecordMap = new ConcurrentHashMap<String, AtomicLong>();
	private ConcurrentHashMap<String, AtomicLong> filteredSAMRecordMap = new ConcurrentHashMap<String, AtomicLong>();
	private long startTime;
	private int bufferNumber;
	private String mode;
	private List<String> ranges;
	private boolean isRemove = false;
	private boolean isBamOverride = false;
	
	private Integer lowReadCount;
	private Integer nonrefThreshold;
	private SAMSequenceRecord referenceRecord;
	private byte[] referenceBases;
	private Options options;
	private QueryExecutor exec;
	
    NonReferenceRecord forwardNonRef = null;
    NonReferenceRecord reverseNonRef = null;	
    private StrandDS forward = null;
    private StrandDS reverse = null;

	public MitoPileline(Options options) throws Exception {
		 
		this.bamFiles = options.getInputFileNames();
		this.startTime = System.currentTimeMillis();
		exec = new QueryExecutor(options.getQuery());	
		lowReadCount = options.getLowReadCount();
		nonrefThreshold = options.getNonRefThreshold();
		referenceRecord = options.getReferenceRecord();
		referenceFile = options.getReferenceFile();
		outputFile = options.getOutputFileName();
		
		this.options = options;		
      	forward = new StrandDS( referenceRecord, false );
    	reverse = new StrandDS(referenceRecord, true );
       			
		//alalysis reads
		for (String bam : bamFiles) {       	 
        	readSAMRecords(bam) ;	
        	//add the stats that need to be done at the end to the datasets				
        	forward.finalizeMetrics(referenceRecord.getSequenceLength(), isRemove, forwardNonRef);
        	reverse.finalizeMetrics(referenceRecord.getSequenceLength(), isRemove, reverseNonRef); 
		}
		
	}
	
	

	
	/**
	 * it output all pileup datasets into tsv format file
	 * @param output: output file name with full path
	 * @throws IOException
	 */
 	public void report() throws IOException{
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false));
		
		//headlines
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String sb = "## DATE=" + dateFormat.format(new Date()) + "\n";
		sb += "## TOOL_NAME=" + options.getPGName() + ", version " + options.getVersion() + "\n";
		sb += "## REFERENCE=FILE: " + referenceFile + "\n";
		sb += "## REFERENCE=SEQUENCE: " + referenceRecord.getSequenceName() + ",LENGTH:" + referenceRecord.getSequenceLength() + "\n";
		sb += "## INFO=BAMS_ADDED:" + bamFiles.length + "\n";
		for (String bam : bamFiles)
			sb += "## INFO=BAMS_FILE:" + bam + "\n";
		sb += "## INFO=LOW_READ_COUNT:" + options.getLowReadCount() + "\n";
		sb += "## INFO=MIN_NONREF_PERCENT:" + options.getNonRefThreshold() + "\n";
		sb += "Reference\tPosition\tRef_base\t" + StrandEnum.getHeader() + "\n";
		writer.write(sb);			
		
		//all pileup dataset
    	IndexedFastaSequenceFile indexedFastaFile = Reference.getIndexedFastaFile( new File(referenceFile) );
       	referenceBases = indexedFastaFile.getSubsequenceAt(referenceRecord.getSequenceName(), 1,referenceRecord.getSequenceLength()).getBases();

       	PositionElement pos;
		for(int i = 0; i < referenceRecord.getSequenceLength(); i++){
			//Samtools report 1 on mapping position if mapped at start of reference. but java array start at 0
 			pos = new PositionElement(  referenceRecord.getSequenceName(), i+1, (char) referenceBases[i] );
 			
			QPileupRecord qRecord = new QPileupRecord(pos, 
					forward.getStrandElementMap(i), reverse.getStrandElementMap(i));			
			sb = qRecord.getPositionString() + qRecord.getStrandRecordString() + "\n" ;		
			writer.write(sb);		 		
 		}			

  	   	logger.info("outputed strand dataset of " + referenceRecord.getSequenceName() + ", pileup position " + referenceRecord.getSequenceLength());    	 			

		writer.close();			
	}
	/**
	 * main pileline to run pileup on a single BAM
	 * @param bamFile: input BAM file
	 * @param ref : reference file where BAM mapped on
	 * @param exec: qbamfilter query executor
	 * @throws Exception
	 */
	void readSAMRecords(String bamFile) throws Exception{							

    	try {            		
    		//set up overall stats for the bam 
        	forwardNonRef = new NonReferenceRecord(referenceRecord.getSequenceName(), referenceRecord.getSequenceLength(), false, lowReadCount, nonrefThreshold);
        	reverseNonRef = new NonReferenceRecord(referenceRecord.getSequenceName(), referenceRecord.getSequenceLength(), true, lowReadCount, nonrefThreshold);
			
        	int numReads = 0;
			SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile,ValidationStringency.SILENT);
			SAMRecordIterator ite = reader.query(referenceRecord.getSequenceName(),0, referenceRecord.getSequenceLength(), false);
			while(ite.hasNext()){
				SAMRecord record = ite.next();	 

/*				
//debug
if(record.getAlignmentStart() >= 16478){
System.out.println(String.format("record mapped on %d~%d,MD:%s", record.getAlignmentStart(), record.getAlignmentEnd(),record.getAttribute("MD")));
byte[] base = record.getReadBases();
System.out.print("read base:   "); 
for(byte b : base)
	System.out.print( (char) b);
System.out.println();
}*/
            	if(! exec.Execute(record)) continue;			
				PileupSAMRecord p = new PileupSAMRecord(record);   
				//pileup single read
            	p.pileup();    
            	//accumulate  base information into related reference base 
             	addToStrandDS(p);
            	p = null;		
            	numReads ++;
  			}  
			ite.close();
			reader.close();
 			logger.info("Added " + numReads + " reads mapped on "+ referenceRecord.getSequenceName()+" and met query from BAM: " + bamFile);					 			 
    	} catch (Exception e) {
    		logger.error("Exception happened during reading: " + bamFile);
            throw new Exception(ExceptionMessage.getStrackTrace(e) );
    	} 	
	}

	/**
	 * accumulate  base information into related reference base 
	 * @param p: a pileupSAMRecord stored pileup information on base of a single SAMRecord
	 * @throws Exception
	 */
	private void addToStrandDS(PileupSAMRecord p) throws Exception {		
	    	List<PileupDataRecord> records = p.getPileupDataRecords();			
			for (PileupDataRecord dataRecord : records) {
				//pileup will add extra pileupDataRecord for clips, it may byond reference edge
				if (dataRecord.getPosition() < 1 ||   dataRecord.getPosition() > referenceRecord.getSequenceLength()) 
					continue;
				
				int index = dataRecord.getPosition() - 1;  //?array start with 0, but reference start with 1
				if (dataRecord.isReverse()) {				
 					reverse.modifyStrandDS(dataRecord, index, false);
 					reverseNonRef.addNonReferenceMetrics(dataRecord, index);
				} else {
					forward.modifyStrandDS(dataRecord, index, false);
					forwardNonRef.addNonReferenceMetrics(dataRecord, index);
				}			 
 			}				
		}			

	public StrandDS GetForwardStrandDS(){	return forward; }
	
	public StrandDS GetReverseStrandDS(){	return reverse; }
}