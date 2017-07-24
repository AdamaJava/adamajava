/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package au.edu.qimr.qmito;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.KeyValue;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.meta.QLimsMeta;
import org.qcmg.common.meta.QToolMeta;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.util.QLimsMetaFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import au.edu.qimr.qmito.lib.*;

//import au.edu.qimr.qlib.qpileup.*;
//import au.edu.qimr.qlib.util.*;

public class MetricPileline {
	
	private QLogger logger = QLoggerFactory.getLogger(getClass());
	private String[] bamFiles;	
	private String outputFile;
	private String referenceFile;

	private Integer lowReadCount;
	private Integer nonrefThreshold;
	private SAMSequenceRecord referenceRecord;
	private byte[] referenceBases;
	private String query;
	private QExec qexec;
	
    NonReferenceRecord forwardNonRef = null;
    NonReferenceRecord reverseNonRef = null;	
    private StrandDS forward = null;
    private StrandDS reverse = null;
    
    private long[] Fmismatch = new long[100];
    private long[] Rmismatch = new long[100];
    private long Ftotal = 0;
    private long Rtotal = 0;
    
	public MetricPileline(MetricOptions options) throws Exception {
		 
		this.bamFiles = options.getInputFileNames();
		this.query = options.getQuery();
		this.qexec = options.getQExec();
		
		lowReadCount = options.getLowReadCount();
		nonrefThreshold = options.getNonRefThreshold();
		referenceRecord = options.getReferenceRecord();
		referenceFile = options.getReferenceFile();
		outputFile = options.getOutputFileName();
		
	//	this.options = options;		
      	forward = new StrandDS( referenceRecord, false );
    	reverse = new StrandDS(referenceRecord, true );
       			
		//alalysis reads
    	QueryExecutor exec = new QueryExecutor(options.getQuery());	
		for (String bam : bamFiles)  readSAMRecords(bam,exec) ;	
		
    	//add the stats that need to be done at the end to the datasets				
    	forward.finalizeMetrics(referenceRecord.getSequenceLength(), false, forwardNonRef);
    	reverse.finalizeMetrics(referenceRecord.getSequenceLength(), false, reverseNonRef); 
    	
	}
	
	private void createHeader(BufferedWriter writer) throws Exception{
 		//output tool execution information	
		writer.write(qexec.getExecMetaDataToString());

		//output input files information		
		for (String bam : bamFiles) {       	 
			SAMFileHeader header = SAMFileReaderFactory.createSAMFileReader(new File(bam)).getFileHeader(); 
			QLimsMeta limsMeta = QLimsMetaFactory.getLimsMeta("input", bam, header);		 
			writer.write(limsMeta.getLimsMetaDataToString() );
		}
		
		//output tool parameters
		KeyValue[] array = new KeyValue[5];
		array[0] = new KeyValue("ReferenceFile", referenceFile); 
		array[1] = new KeyValue("ReferenceName", referenceRecord.getSequenceName() + ",LENGTH:" + referenceRecord.getSequenceLength());
		array[2] = new KeyValue("QueryForQbamfilter", query );
		array[3] = new KeyValue("LOW_READ_COUNT", lowReadCount.toString() );
		array[4] = new KeyValue("MIN_NONREF_PERCENT",  nonrefThreshold.toString() );		
		
		QToolMeta meta = new QToolMeta("qMito", array);
		writer.write(meta.getToolMetaDataToString());	
		
		//output column names
		writer.write("Reference\tPosition\tRef_base\t" + StrandEnum.getHeader() + "\n");
		
		
	}

	/**
	 * it output all pileup datasets into tsv format file
	 * @param output: output file name with full path
	 * @throws Exception 
	 */
 	public void report() throws Exception{
 		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, false));
		
		//create header lines
		createHeader(writer);
		
		//all pileup dataset
    	IndexedFastaSequenceFile indexedFastaFile = Reference.getIndexedFastaFile( new File(referenceFile) );
       	referenceBases = indexedFastaFile.getSubsequenceAt(referenceRecord.getSequenceName(), 1,referenceRecord.getSequenceLength()).getBases();

       	PositionElement pos;
		for(int i = 0; i < referenceRecord.getSequenceLength(); i++){
			//Samtools report 1 on mapping position if mapped at start of reference. but java array start at 0
 			pos = new PositionElement(  referenceRecord.getSequenceName(), i+1, (char) referenceBases[i] );
 			
			QPileupRecord qRecord = new QPileupRecord(pos, 
					forward.getStrandElementMap(i), reverse.getStrandElementMap(i));			
			String sb = qRecord.getPositionString() + qRecord.getStrandRecordString() + "\n" ;		
			writer.write(sb);		 		
 		}			
		writer.close();	
		
  	   	logger.info("outputed strand dataset of " + referenceRecord.getSequenceName() + ", pileup position " + referenceRecord.getSequenceLength());    	 			
    	//report mismatch stats
  	   	long total = Ftotal + Rtotal;
    	for(int i = 0; i < 100; i ++) 
    		if(Fmismatch[i] > 0 || Rmismatch[i] > 0) 
    			logger.info(String.format("There %d (%.2f) forward records  and %d (%.2f) reverse reacords contains %d base mismatch", Fmismatch[i], (double) Fmismatch[i] / total ,Rmismatch[i], (double) Rmismatch[i] / total, i));
		logger.info(String.format("There are total %d (%.2f) forward records and %d (%.2f) reverse records pileup", Ftotal,(double) Ftotal / total , Rtotal, (double) Rtotal / total));

	}
	/**
	 * main pileline to run pileup on a single BAM
	 * @param bamFile: input BAM file
	 * @param ref : reference file where BAM mapped on
	 * @param exec: qbamfilter query executor
	 * @throws Exception
	 */
	void readSAMRecords(String bamFile, QueryExecutor exec) throws Exception{							

    	try {            		
    		//set up overall stats for the bam 
        	forwardNonRef = new NonReferenceRecord(referenceRecord.getSequenceName(), referenceRecord.getSequenceLength(), false, lowReadCount, nonrefThreshold);
        	reverseNonRef = new NonReferenceRecord(referenceRecord.getSequenceName(), referenceRecord.getSequenceLength(), true, lowReadCount, nonrefThreshold);
			
        	int numReads = 0, total = 0;
			SamReader reader = SAMFileReaderFactory.createSAMFileReader(new File(bamFile),ValidationStringency.SILENT);
			
			SAMRecordIterator ite = reader.query(referenceRecord.getSequenceName(),0, referenceRecord.getSequenceLength(), false);
			while(ite.hasNext()){
				total ++;
				SAMRecord record = ite.next();	 
            	if(! exec.Execute(record)) continue;	
            	if(record.getAttribute("MD") == null) continue;
            	
				//add little stats here
				add2Stat(record);            	
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
 			logger.info("Total read " + total + " reads from input: " + bamFile);					 			 
 			logger.info("Added " + numReads + " reads mapped on "+ referenceRecord.getSequenceName()+" and met query from BAM: " + bamFile);					 			 
    	} catch (Exception e) {
    		logger.error("Exception happened during reading: " + bamFile);
            throw new Exception(MitoException.getStrackTrace(e) );
    	} 	
	}
	
	private void add2Stat(SAMRecord record){
		String attribute = (String)record.getAttribute("MD");	
	 
			int count = 0;
			for (int i = 0, size = attribute.length() ; i < size ; ) {
				char c = attribute.charAt(i);
				if (c == 'A' || c == 'C' || c == 'G' || c == 'T' || c == 'N') {
					count++;
					i++;
				} else if ( c == '^') {
					//skip the insertion base
					while (++i < size && Character.isLetter(attribute.charAt(i))) {}
				} else i++;	// need to increment this or could end up with infinite loop...
			}
			if(record.getReadNegativeStrandFlag()){	
				Rmismatch[count] ++;
				Rtotal ++;
			}else{
				Fmismatch[count]++;
				Ftotal ++;
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
