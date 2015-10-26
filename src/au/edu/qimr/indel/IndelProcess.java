package au.edu.qimr.indel;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;
import htsjdk.samtools.reference.ReferenceSequence;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SAMRecord;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.qbamfilter.query.QueryExecutor;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.indel.pileup.Homopolymer;
import au.edu.qimr.indel.pileup.IndelPileup;
import au.edu.qimr.indel.pileup.IndelPosition;
import au.edu.qimr.indel.pileup.ReadIndels;


public class IndelProcess {
	
	Options options; 
	QLogger logger; 
	ReadIndels indelload;
	
	private final List<SAMSequenceRecord> sortedContigs = new ArrayList<SAMSequenceRecord>();
	private Map<ChrPosition, IndelPosition> positionRecordMap ;
	
	public IndelProcess(File inputVcf, Options options, QLogger logger) throws Exception {		
		this.options = options;	
		this.logger = logger; 
		
		SamReader TBreader =  SAMFileReaderFactory.createSAMFileReader(options.getTestBam() );
		for (final SAMSequenceRecord contig : TBreader.getFileHeader().getSequenceDictionary().getSequences())  
			sortedContigs.add(contig);
		
		this.indelload = new ReadIndels(logger);
		indelload.LoadSingleIndels(inputVcf);	
	}

	public IndelProcess(File inputTumourVcf, File inputNormalVcf, Options options, QLogger logger) throws Exception {
		this(inputTumourVcf, options, logger); 
		indelload.appendIndels(inputNormalVcf); 		
	}
	
	public int process() throws Exception{
		
		positionRecordMap = indelload.getIndelMap();
		for(SAMSequenceRecord contig : sortedContigs ){
						
			List<ChrPosition> list = getIndelList(contig);
 			if(list.size() == 0) continue;			
 
 			Map<ChrPosition,IndelPileup> tumourPileup = contigPileup(contig, list, options.getTestBam(),null );			
 			Map<ChrPosition,IndelPileup> normalPileup = contigPileup(contig, list, options.getControlBam(),null ); 			
				
			//annotateVcf with td, nd and homopolymers	 						
			for (final ChrPosition pos : list) {
				IndelPosition indel = positionRecordMap.get(pos);
				indel.setPileup(true, tumourPileup.get(pos));
				indel.setPileup(false, normalPileup.get(pos));
//				indel.setHomopolymer(new Homopolymer(indel, options.getReference() , options.nearbyHomopolymer)); 				
			}
			
			logger.info(list.size() + " indels are pileuping from " + contig.getSequenceName());
			
		}
		//output annotated vcf
		writeVCF( options.getOutput() );			
		
		return 0; 
	}
	
	public void writeVCF( File outputFile ) throws IOException {		 
		logger.info("Writing VCF output");	 		
		final List<ChrPosition> orderedList = getIndelList(null);
		
		try(VCFFileWriter writer = new VCFFileWriter( outputFile)) {						 
			long count = 0; 
			for (final ChrPosition pos : orderedList) {
				IndelPosition indel = positionRecordMap.get(pos);
				for(int i = 0; i < indel.getMotifs().size(); i++){					
					VcfRecord re = indel.getPileupedVcf(i); 
					writer.add( re );	
				}
				count ++;
			}
			logger.info("outputed VCF record:  " + count);
		}  
	}
	
	/**
	 * it swap SAMRecord between currentPool and nextPool. After then, the currentPool will contain all SAMRecord overlapping topPos position, 
	 * the nextPool will contain all SAMRecord start after topPos position.  All SAMRecord end before topPos position will be remvoved from both pool. 
	 * @param topPos:   pileup position
	 * @param currentPool: a list of SAMRecord overlapped previous pileup Position
	 * @param nextPool: a list of SAMRecord behind previous pileup Position
	 */
	private void resetPool( ChrPosition topPos, List<SAMRecord> currentPool, List<SAMRecord> nextPool){
			List<SAMRecord> tmp_current_pool = new ArrayList<SAMRecord>();			
			
			List<SAMRecord> tmp_pool = new ArrayList<SAMRecord>();	
			tmp_pool.addAll(nextPool);
			
			//check read record behind on current position			
			for( SAMRecord  re : tmp_pool ){
				//aligned position before indel
				if(re.getAlignmentEnd() < topPos.getPosition())
					nextPool.remove(re);
				//aligned position cross indel
				else if(re.getAlignmentStart() <= topPos.getEndPosition()) 	 					 
					tmp_current_pool.add(re);	 				 
			}	 

			
			tmp_pool.clear();
			tmp_pool.addAll(currentPool);
			//check already read record  for previous pileup
			for( SAMRecord  re1 : tmp_pool ){
				//aligned position before indel
				if(re1.getAlignmentEnd() < topPos.getPosition())
					currentPool.remove(re1);
				//aligned position after indel
				else if(re1.getAlignmentStart() > topPos.getEndPosition()){
					nextPool.add(re1);
					currentPool.remove(re1);
				}
			}
			
			//merge samrecord
			currentPool.addAll(tmp_current_pool);
	}
	
	
//	private Map<ChrPosition,IndelPileup> contigPileup(List<ChrPosition> list, SAMRecordIterator ite, QueryExecutor exec) throws Exception{
	private Map<ChrPosition,IndelPileup> contigPileup(SAMSequenceRecord contig, List<ChrPosition> list, File bam, QueryExecutor exec) throws Exception{	 
		
		SamReader TBreader =  SAMFileReaderFactory.createSAMFileReader( bam );
					
		SAMRecordIterator ite = TBreader.query(contig.getSequenceName(), 0, contig.getSequenceLength(),false);		
 
		if(list.isEmpty()) return null; 
			
		List<ChrPosition> indellist = new ArrayList<ChrPosition>(list);
		Map<ChrPosition,IndelPileup> pileupMap = new ConcurrentHashMap<ChrPosition,IndelPileup>();
	 	 	
	 	List<SAMRecord> current_pool = new ArrayList<SAMRecord>();
	 	List<SAMRecord> next_pool = new ArrayList<SAMRecord>(); 		 	
	 	ChrPosition topPos = indellist.remove(0);	
	 	
	 	while (ite.hasNext()) {	
	 		SAMRecord re = ite.next();
//	 		boolean isNext = false;  
	 		//bam file already sorted, skip non-indel region record
	 		if(re.getAlignmentEnd() < topPos.getPosition()) continue; 
	 		
	 		//only interested pass filter record
	 		boolean passFilter; 	
	 		if(exec != null )
				passFilter = exec.Execute(re);
			else
				passFilter = !re.getReadUnmappedFlag() && (!re.getDuplicateReadFlag() || options.includeDuplicates());
	 		if(! passFilter ) continue; 
	 			 		
	 		//whether in current indel region
	 		if(re.getAlignmentStart() <= topPos.getEndPosition())
	 			current_pool.add(re);
	 		else{
	 			next_pool.add(re); 
	 			//pileup
	 			IndelPileup pileup= new IndelPileup(positionRecordMap.get(topPos), options.getSoftClipWindow(), options.getNearbyIndelWindow());
	 			pileup.pileup(current_pool);
	 			pileupMap.put(topPos, pileup  );
	 			
	 			//prepare for next indel position
	 			if(indellist.isEmpty()) return pileupMap;
	 			topPos = indellist.remove(0);	
	 			resetPool(topPos,  current_pool, next_pool); 	 				 			 
	 		}
	 	}	 	
	 	
	 	//after loop check all pool
	 	do{			
 			IndelPileup pileup= new IndelPileup(positionRecordMap.get(topPos), options.getSoftClipWindow(), options.getNearbyIndelWindow());
 			pileup.pileup(current_pool);
			pileupMap.put(topPos, pileup);
			
			if(indellist.isEmpty()) break;
			topPos = indellist.remove(0);
			resetPool(topPos,  current_pool, next_pool); 							
			//topPos = indellist.get(0);
	 	}while( true ) ;	 	
		
	 	return pileupMap;		
	}
		
	/*
	 * 
	 * return a sorted list for specified contig, which contains all indel position: chr, start, end
	 */
	/**
	 * 
	 * @param contig: contig name or null for whole reference
	 * @return list of chrPosition on this contig; return all chrPostion on whole reference if contig is null
	 */
	private  List<ChrPosition> getIndelList( SAMSequenceRecord contig ){
	  List<ChrPosition>  list = new ArrayList<ChrPosition> ();		
	  if (positionRecordMap == null || positionRecordMap.size() == 0)
		  return list; 
	  
	  if(contig == null){ //get whole reference
		  list.addAll(positionRecordMap.keySet());	
	  }else{	  //get all chrPosition on specified contig	
		  for(ChrPosition pos : positionRecordMap.keySet())
			  if(pos.getChromosome().equals(contig.getSequenceName()))
				  list.add(pos);	 
	  }
	  
 	 final Comparator<String> chrComparator = new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					return sortedContigs.indexOf(o1) - sortedContigs.indexOf(o2);
				}
			};
		Collections.sort(list, new Comparator<ChrPosition>() {
			@Override
			public int compare(ChrPosition o1, ChrPosition o2) {
				final int diff = chrComparator.compare(o1.getChromosome(), o2.getChromosome());
				if (diff != 0) return diff;
				return o1.getPosition() - o2.getPosition();
			}
		});
	  
	  return list;
  }
	 
		
}
