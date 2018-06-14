package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;

import org.qcmg.common.vcf.VcfFormatFieldRecord;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;

import au.edu.qimr.qannotate.Options;
import au.edu.qimr.qannotate.utils.ContigPileup;
import au.edu.qimr.qannotate.utils.VariantPileup;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import scala.actors.threadpool.Arrays;

public class OverlapMode extends AbstractMode{


	private final static int threadNo = 8; 
	
	private final static QLogger logger = QLoggerFactory.getLogger(OverlapMode.class);
//	private QueryExecutor query =  new QueryExecutor( "and (flag_ReadUnmapped == false,flag_NotprimaryAlignment == false,"
//			+ " flag_ReadFailsVendorQuality == false, flag_SupplementaryRead == false, Flag_DuplicateRead == false, CIGAR_M > 34, MAPQ >10, MD_mismatch <= 3 )");

	
	private final QueryExecutor query =  new QueryExecutor( "and (Flag_DuplicateRead == false, CIGAR_M > 34, MAPQ >10, MD_mismatch <= 3 )");
	
	public OverlapMode(Options options) throws Exception{	
		logger.tool(	"input: " + options.getInputFileName()	);
        logger.tool(	"bam file: " + Arrays.toString( options.getDatabaseFiles()) );
        logger.tool(	"output for annotated vcf records: " + options.getOutputFileName()	);
        logger.tool(	"logger file " + options.getLogFileName()	);
        logger.tool(	"logger level " + (options.getLogLevel() == null ? QLoggerFactory.DEFAULT_LEVEL.getName() :  options.getLogLevel())	);
              
        //read vcf records to Map<ChrPosition,List<VcfRecord>> positionRecordMap
        loadVcfRecordsFromFile(new File( options.getInputFileName())   );
              
        Map<VcfRecord, String[]> annotationMap = annotation( options.getDatabaseFiles() );        
	    String[] acOverlap;//options.getDatabaseFiles().length
	    for(List<VcfRecord> vcfs: positionRecordMap.values()) 
	    	for(VcfRecord vcf :  vcfs){ 
	    		acOverlap = annotationMap.get(vcf);
	    		if(acOverlap == null) continue;        		
				List<String> field = vcf.getFormatFields();
				for(int f = 1; f < field.size(); f ++){
					VcfFormatFieldRecord forR = vcf.getSampleFormatRecord(f);
					String value = ( acOverlap.length < f || acOverlap[f-1] == null )? Constants.MISSING_DATA_STRING : acOverlap[f-1] ;       						
					forR.setField(VcfHeaderUtils.FORMAT_ACLAP, value);  
					field.set(f, forR.getSampleColumnString());
					if(f == 1) //only set once
						field.set(0, forR.getFormatColumnString());
				}
				vcf.setFormatFields(  field ); 
			}
        	                 
		reheader(options.getCommandLine(),options.getInputFileName());
		header.addFormat(VcfHeaderUtils.FORMAT_ACLAP, ".", "String", VcfHeaderUtils.FORMAT_ACLAP_DESC);
		writeVCF( new File(options.getOutputFileName()));	        
	}
	
	@Override
	void addAnnotation(String bamfile) throws Exception { }

	private Map<VcfRecord, String[]> annotation( String[] bamfiles ) throws Exception { 
		
		//get sequence from bam header, start from biggest contig
		final TreeSet<SAMSequenceRecord> contigs = new TreeSet<>( 
				(SAMSequenceRecord s1, SAMSequenceRecord s2) -> s2.getSequenceLength() -  s1.getSequenceLength() );
		for(String bamfile : bamfiles){
			try (SamReader reader =  SAMFileReaderFactory.createSAMFileReader( new File(bamfile))) {				 
				for (final SAMSequenceRecord contig : reader.getFileHeader().getSequenceDictionary().getSequences())  	{			
					contigs.add(contig);
				}
			}
		}
								
		//load snp records
        final CountDownLatch pileupLatch = new CountDownLatch(contigs.size() * bamfiles.length); // filtering thread                           
        final AbstractQueue< VariantPileup > queue = new ConcurrentLinkedQueue<>();
        // set up executor services
        ExecutorService pileupThreads = Executors.newFixedThreadPool( threadNo );        
     	//each time only throw threadNo thread, the loop finish until the last threadNo     
                  
	   for (SAMSequenceRecord contig : contigs ) 
		   for(int i = 0 ; i< bamfiles.length; i ++ )      		   
			pileupThreads.execute(new ContigPileup(contig, getVcfList(contig), new File(bamfiles[i]), query, queue, i+1, Thread.currentThread(), pileupLatch ));    		    	 
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
		
		//put queue to order by sample column
		Map<VcfRecord, String[]> pileuped = new HashMap<>();
		for(VariantPileup pileup : queue){ 
			String[] anno = pileuped.computeIfAbsent( pileup.getVcf(), k-> new String[bamfiles.length] );
			anno[pileup.getSampleColumnNo()-1] = pileup.getAnnotation();	
			
//			//debug
//			if(pileup.getVcf().getPosition() == 4511341){			
//				try (SamReader Breader =  SAMFileReaderFactory.createSAMFileReader(new File(bamfiles[0]));){
//					SAMOrBAMWriterFactory fact = new SAMOrBAMWriterFactory(Breader.getFileHeader(), false, new File("output.33.bam"));				 
//					for(SAMRecord re: pileup.getReads33())
//						fact.getWriter().addAlignment(re);
//					fact.closeWriter();					 
//				}
//			}
			
		}
				
		return pileuped;		
	}
	
	 /**
	  * 
	  * @param contig: contig name or null for whole reference
	  * @param filter: only return indel vcf records with specified filter value. Put null here if ignor record filter column value
	  * @return a sorted list of IndelPotion on this contig; return whole reference indels if contig is null
	  */
	 private  AbstractQueue<VcfRecord>  getVcfList( SAMSequenceRecord contig ){
		if (positionRecordMap == null || positionRecordMap.size() == 0)
			return new ConcurrentLinkedQueue<VcfRecord>(); 	
				  
		List<VcfRecord> list = new ArrayList<> ();	
		for(ChrPosition pos : positionRecordMap.keySet()){
			//get variants from same contig
			if(contig != null && !pos.getChromosome().equals(contig.getSequenceName())  )
				continue; 
			list.addAll(positionRecordMap.get(pos));
		}

		// lambda expression to replace abstract method			 				 
		list.sort( (VcfRecord o1,VcfRecord o2) ->  new ChrPositionComparator().compare( o1.getChrPosition(), o2.getChrPosition() ) );
		 
		return new ConcurrentLinkedQueue< VcfRecord>( list ); 
	 }
}
