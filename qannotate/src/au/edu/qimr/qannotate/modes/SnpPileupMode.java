package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.util.IndelUtils;

import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;

import org.qcmg.common.util.Constants;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.ContigPileup;
import au.edu.qimr.qannotate.utils.SnpPileup;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import scala.actors.threadpool.Arrays;

public class SnpPileupMode extends AbstractMode{


	private final static int threadNo = 5; 
	
	private final static QLogger logger = QLoggerFactory.getLogger(SnpPileupMode.class);
	private QueryExecutor query =  new QueryExecutor( "and (flag_ReadUnmapped == false,flag_NotprimaryAlignment == false,"
			+ " flag_ReadFailsVendorQuality == false, flag_SupplementaryRead == false, Flag_DuplicateRead == false, CIGAR_M > 34, MAPQ >10, MD_mismatch <= 3 )");
	
	public SnpPileupMode(Options options) throws Exception{	

		logger.tool(	"input: " + options.getInputFileName()	);
        logger.tool(	"bam file: " + Arrays.toString( options.getDatabaseFiles()) );
        logger.tool(	"output for annotated vcf records: " + options.getOutputFileName()	);
        logger.tool(	"logger file " + options.getLogFileName()	);
        logger.tool(	"logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel())	);
              
        //read vcf records to Map<ChrPosition,List<VcfRecord>> positionRecordMap
        inputRecord(new File(options.getInputFileName()));   
                
        Map<ChrPosition, String[]> acsnpMap = new HashMap<>(); 
        for(int i = 0; i < options.getDatabaseFiles().length; i ++ ) 
        	for(SnpPileup pileup: addAnnotation( options.getDatabaseFiles()[i]  , i ) ){
        		String[] anno = acsnpMap.computeIfAbsent( pileup.getPosition(), k-> new String[ options.getDatabaseFiles().length] );
        		anno[i] = pileup.getAnnotation();       		
        	}
        
        String[] acsnp;//options.getDatabaseFiles().length
        for(ChrPosition pos: positionRecordMap.keySet()) 
        	if( ( acsnp = acsnpMap.get(pos)) != null ){
        		for(VcfRecord vcf: positionRecordMap.get(pos)){
        			if( IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt()).getOrder() >= 5 )
        				continue;	//skip non SNP,MNP variants
        			
        			List<String> field = vcf.getFormatFields();
        			for(int f = 1; f < field.size(); f ++){
        				VcfFormatFieldRecord forR = vcf.getSampleFormatRecord(f);
        				String value = ( acsnp.length < f || acsnp[f-1] == null )? Constants.MISSING_DATA_STRING : acsnp[f-1] ;       						
        				forR.setField("ACSNP", value);  
        				field.set(f, forR.getSampleColumnString());
        				if(f == 1) //only set once
        					field.set(0, forR.getFormatColumnString());
        			}
        			vcf.setFormatFields(  field ); 
        		}
        	}        
          
		reheader(options.getCommandLine(),options.getInputFileName());
		writeVCF( new File(options.getOutputFileName()));	        
	}
	
	@Override
	void addAnnotation(String bamfile) throws Exception { }

	List< SnpPileup > addAnnotation( String bamfile, final int columnNo ) throws Exception { 
		
		//get sequence from bam header
		final List<SAMSequenceRecord> contigs = new ArrayList<SAMSequenceRecord>();
		SamReader reader =  SAMFileReaderFactory.createSAMFileReader( new File(bamfile) ) ;				 
		for (final SAMSequenceRecord contig : reader.getFileHeader().getSequenceDictionary().getSequences())  
			contigs.add(contig);
		reader.close(); 
				
		//load snp records
        final CountDownLatch pileupLatch = new CountDownLatch(contigs.size() * 2); // filtering thread                       
      
        final AbstractQueue< SnpPileup > queue = new ConcurrentLinkedQueue<>();
        // set up executor services
        ExecutorService pileupThreads = Executors.newFixedThreadPool( threadNo );        
     	//each time only throw threadNo thread, the loop finish until the last threadNo     
           
    	for (SAMSequenceRecord contig : contigs )        		   		 
    		pileupThreads.execute(new ContigPileup(contig, getVcfList(contig, columnNo), new File(bamfile), query, queue, Thread.currentThread(), pileupLatch ));    		
    	 
    	pileupThreads.shutdown();
    	
		// wait for threads to complete
		try {
			logger.info("waiting for  threads to finish (max wait will be 20 hours)");
			pileupThreads.awaitTermination(20, TimeUnit.HOURS);
			logger.info("All threads finished");			
		} catch (Exception e) {
			logger.error("Exception caught whilst waiting for threads to finish: " + e.getMessage(), e);
			throw e;
		} finally { pileupThreads.shutdownNow(); }
		
		return new ArrayList( queue );		
	}
	
	 /**
	  * 
	  * @param contig: contig name or null for whole reference
	  * @param filter: only return indel vcf records with specified filter value. Put null here if ignor record filter column value
	  * @return a sorted list of IndelPotion on this contig; return whole reference indels if contig is null
	  */
	 private  AbstractQueue<ChrPosition>  getVcfList( SAMSequenceRecord contig, final int sampleColumnNo ){	  
		if (positionRecordMap == null || positionRecordMap.size() == 0)
			return new ConcurrentLinkedQueue<ChrPosition>(); 	
				  
		List<ChrPosition> list = new ArrayList<> ();	
		for(ChrPosition pos : positionRecordMap.keySet()){
			//get variants from same contig
			if(contig != null && !pos.getChromosome().equals(contig.getSequenceName())  )
				continue; 
			//get variants of snp or mnp which are SNP(1),DNP(2),TNP(3), ONP(4)
			for(VcfRecord vcf : positionRecordMap.get(pos)) 			 
				if( IndelUtils.getVariantType(vcf.getRef(), vcf.getAlt()).getOrder() == 1 ){  
					list.add(vcf.getChrPosition());	
					break;
				}	 
		}

		// lambda expression to replace abstract method			 				 
	//	list.sort( (ACSNP o1, ACSNP o2) ->  new ChrPositionComparator().compare( o1.chrP, o2.chrP ) );
		list.sort( new ChrPositionComparator());
		 
		return new ConcurrentLinkedQueue< ChrPosition >( list ); 
	 }
		



}
