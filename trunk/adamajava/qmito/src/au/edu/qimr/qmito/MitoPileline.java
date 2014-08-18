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
	QueryExecutor exec;
	
    NonReferenceRecord forwardNonRef = null;
    NonReferenceRecord reverseNonRef = null;	
    StrandTXTDS forward = null;
    StrandTXTDS reverse = null;

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
       
       	forward = new StrandTXTDS( referenceRecord, false );
    	reverse = new StrandTXTDS(referenceRecord, true );
       			
		//alalysis reads
		for (String bam : bamFiles)        	 
        	readSAMRecords(bam, referenceRecord, exec) ;	
		
		//output 
  	   	//read reference base into array
		BufferedWriter writer = new BufferedWriter(new FileWriter(options.getOutputFileName(), false));
		writeHeader(writer);
		writeDataset(writer );
		writer.close();		
	}

	private void writeDataset(BufferedWriter writer) throws IOException{
     	IndexedFastaSequenceFile indexedFastaFile = Reference.getIndexedFastaFile( new File(referenceFile) );
       	FastaSequenceIndex index = Reference.getFastaIndex(new File(referenceFile));  	   	
       	referenceBases = indexedFastaFile.getSubsequenceAt(referenceRecord.getSequenceName(), 1,referenceRecord.getSequenceLength()).getBases();

       	PositionElement pos;
		for(int i = 0; i < referenceRecord.getSequenceLength(); i++){
			pos = new PositionElement(  referenceRecord.getSequenceName(), i, (char) referenceBases[i] );
			QPileupRecord qRecord = new QPileupRecord(pos, 
					forward.getStrandElementMap(i), reverse.getStrandElementMap(i));			
			String sb = qRecord.getPositionString() + qRecord.getStrandRecordString() + "\n" ;		  				
			writer.write(sb);		 
 		}			
  	   	logger.info("outputed strand dataset of " + referenceRecord.getSequenceName() + ", pileup position " + referenceRecord.getSequenceLength());    	 			
	}	
	
	private void writeHeader( BufferedWriter writer ) throws IOException{
		
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
		sb += "## Reference\tPosition\tRef_base\t" + StrandEnum.getHeader() + "\n";
		writer.write(sb);	
	}
	
	private void readSAMRecords(String bamFile, SAMSequenceRecord ref,QueryExecutor exec) throws Exception{							

    	try {            		
    		//set up overall stats for the bam 
        	forwardNonRef = new NonReferenceRecord(ref.getSequenceName(), ref.getSequenceLength(), false, lowReadCount, nonrefThreshold);
        	reverseNonRef = new NonReferenceRecord(ref.getSequenceName(), ref.getSequenceLength(), true, lowReadCount, nonrefThreshold);
			
        	int numReads = 0;
			SAMFileReader reader = SAMFileReaderFactory.createSAMFileReader(bamFile,ValidationStringency.SILENT);
			SAMRecordIterator ite = reader.query(ref.getSequenceName(),0, ref.getSequenceLength(), false);
			while(ite.hasNext()){
				SAMRecord record = ite.next();	
	
           	//debug
            //	if(record.getReadName().equals( "HWI-ST1240:115:H03J8ADXX:2:1216:5509:54570")){
//            		System.out.println( record.getCigarString() + " (cigar)," + record.getAlignmentStart() + "--" + record.getAlignmentEnd());
            				
            	if(! exec.Execute(record)) continue;			
				PileupSAMRecord p = new PileupSAMRecord(record);    	      	            	
            	p.pileup();      	            	
            	addToStrandDS(p);
            	p = null;		
            	numReads ++;
   	
  			}  
			ite.close();
			reader.close();
			logger.info("Added " + numReads + " reads mapped on "+ ref.getSequenceName()+" and met query from BAM: " + bamFile);					 			 
    	} catch (Exception e) {
    		logger.error("Exception happened during reading: " + bamFile);
            throw new Exception(ExceptionMessage.getStrackTrace(e) );
    	} 	
	}

	private void addToStrandDS(PileupSAMRecord p) throws Exception {
	    	List<PileupDataRecord> records = p.getPileupDataRecords();			
			for (PileupDataRecord dataRecord : records) {
				//pileup will add extra pileupDataRecord for clips, it may byond reference edge
				if (dataRecord.getPosition() < 0 ||   dataRecord.getPosition() >= referenceRecord.getSequenceLength()) 
					continue;

				int index = dataRecord.getPosition();  //?array start with 0, but reference start with 1
				if (dataRecord.isReverse()) {
/*
if(p.getSAMRecord().getReadName().equals( "HWI-ST1240:115:H03J8ADXX:2:1216:5509:54570")){
//	System.out.println(p.getSAMRecord().getReadName() );
	System.out.println(dataRecord.getPosition() + " ,index: " + index + " ,pileupDataRecord size: " + records.size() + 
			", reference length: " +  referenceRecord.getSequenceLength());

}*/
 					reverse.modifyStrandDS(dataRecord, index, false);
 					reverseNonRef.addNonReferenceMetrics(dataRecord, index);
				} else {
					forward.modifyStrandDS(dataRecord, index, false);
					forwardNonRef.addNonReferenceMetrics(dataRecord, index);
				}			 
 			}
					
		}			
	

}