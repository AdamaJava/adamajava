/**
 * Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.qmule;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import net.sf.samtools.SAMFileHeader.SortOrder;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.picard.SAMFileReaderFactory;
import org.qcmg.picard.SAMOrBAMWriterFactory;

public class AlignerCompare {
		static QLogger logger = QLoggerFactory.getLogger(AlignerCompare.class);
		boolean discardNonPrimary;	
		SAMFileReader firReader;
		SAMFileReader secReader;
		
		SAMOrBAMWriterFactory sameWriter;
		SAMOrBAMWriterFactory diffWriter_first;
		SAMOrBAMWriterFactory diffWriter_second;
		
		SAMOrBAMWriterFactory unsureWriter_first;
		SAMOrBAMWriterFactory unsureWriter_second;
		
		
		long total_bam1 = 0;
		long total_bam2 = 0;
		long total_same = 0;
		long noDiff_bam1 = 0;
		long noDiff_bam2 = 0;
		long noSecondary_bam1 = 0;
		long nosupplementary_bam1 = 0;
		long noSecondary_bam2 = 0;
		long nosupplementary_bam2 = 0;
		long nounsureAlignment = 0;


		AlignerCompare(File firBam, File secBam, String prefix, boolean flag) throws Exception{
			//check inputs: sort by query name
			firReader = SAMFileReaderFactory.createSAMFileReader(firBam, SAMFileReader.ValidationStringency.SILENT);
			secReader = SAMFileReaderFactory.createSAMFileReader(secBam, SAMFileReader.ValidationStringency.SILENT);
			discardNonPrimary = flag;
			
			if(! firReader.getFileHeader().getSortOrder().equals(SortOrder.queryname))
				throw new Exception("Please sort the input BAM by queryname: " + firBam.getAbsolutePath());
			
			if(! secReader.getFileHeader().getSortOrder().equals(SortOrder.queryname))
				throw new Exception("Please sort the input BAM by queryname: " + secBam.getAbsolutePath());
			
	 		
	 		logger.info("input BAM1: " + firBam.getAbsolutePath());
	 		logger.info("input BAM2: " + secBam.getAbsolutePath());	 	
	 		logger.info("discard secondary or supplementary alignments: " + String.valueOf(discardNonPrimary));

			//create outputs
			File outsame = new File(prefix + ".identical.bam" );			
			File outdiff_first =  new File(prefix + ".different.first.bam"  );
			File outdiff_second = new File(prefix + ".different.second.bam" );
				
			if(! firBam.getName().equals(secBam.getName())){	
				outdiff_first = new File( prefix + ".different." + firBam.getName() );
				outdiff_second = new File( prefix + ".different." + secBam.getName() );
 			}
			
 			sameWriter = new SAMOrBAMWriterFactory(firReader.getFileHeader(), true, outsame);
			diffWriter_first = new SAMOrBAMWriterFactory(firReader.getFileHeader(), true, outdiff_first );
			diffWriter_second = new SAMOrBAMWriterFactory(secReader.getFileHeader(), true, outdiff_second );
 
			logger.info("output of identical alignments: " + outsame.getAbsolutePath());
	 		logger.info("output of different alignments from BAM1: " + outdiff_first.getAbsolutePath());
	 		logger.info("output of different alignments from BAM2: " + outdiff_second.getAbsolutePath());
	 		
	 		//execute comparison
	 		compareExecutor();
 

			//close IOs
			firReader.close();
			secReader.close();			
			sameWriter.closeWriter();
			diffWriter_first.closeWriter();
			diffWriter_second.closeWriter();
 				
		}

		void compareExecutor() throws Exception{
			ArrayList<SAMRecord> from1 = new ArrayList<SAMRecord> ();
			ArrayList<SAMRecord> from2 = new ArrayList<SAMRecord> ();
			SAMRecordIterator it1 = firReader.iterator();
			SAMRecordIterator it2 = secReader.iterator();	
			//stats
			long noRead = 0;			 
			long noAlign1 = 1;
			long noAlign2 = 1;
			long noSame = 0;
			
			//initialize
			SAMRecord record1 = it1.next();
			SAMRecord record2 = it2.next();
			String Id = record1.getReadName();
			from1.add(record1);
			from2.add(record2);
 			
			//get all aligner from same read
			while( it1.hasNext() || it2.hasNext()){				
				while(it1.hasNext()){
					noAlign1 ++;
					record1 = it1.next() ;
 					if(record1.getReadName().equals(Id)){											 
							from1.add(record1);				
					}else //if not equals(Id)
						break;					
 				} //end while
					
 				while( it2.hasNext() ){
					noAlign2 ++;
					record2 = it2.next();
					if(record2.getReadName().equals(Id)){						 						 
							from2.add(record2);
					}else
						break;  //exit while, record2 is read for next loop					 
				}
 			   //compare alignment in arraylist which filtered out secondary or supplenmentary alignments     
 					noSame += classifyReads( AlignerFilter(from1, unsureWriter_first)  , AlignerFilter(from2, unsureWriter_second) );
 												 
				//clear arraylist and store current reads into arraylist for next loop
				noRead ++;
				from1.clear();
				from2.clear();
				from1.add(record1);
				from2.add(record2);
				Id = record1.getReadName();				 
			}
			
			logger.info(String.format("There are %d reads with %d alignments from BAM1", noRead, noAlign1));
			logger.info(String.format("There are %d reads with %d alignments from BAM2", noRead, noAlign2));
			logger.info(String.format("There are %d alignments are identical from both BAM", noSame));
			logger.info(String.format("Different alignments from BAM1 are %d, from BAM2 are %d", noDiff_bam1, noDiff_bam2));
	 		logger.info( String.format("discard %d secondary alignments and %d supplementary alignments from BAM1",noSecondary_bam1,nosupplementary_bam1));
	 		logger.info(String.format("discard %d secondary alignments and %d supplementary alignments from BAM2",noSecondary_bam2,nosupplementary_bam2));
			
			
		}
		
		/**
		 * 
		 * @param from: an input alignments with same read id
		 * @return ArrayList<SAMRecord> : cleaned alignments excluding secondary and supplementary alignments
		 */
		ArrayList<SAMRecord> AlignerFilter(ArrayList<SAMRecord> from, SAMOrBAMWriterFactory factory) throws Exception{
			ArrayList<SAMRecord> cleaned = new ArrayList<SAMRecord>();
			
			for(SAMRecord record : from)
				if( discardNonPrimary && record.isSecondaryOrSupplementary()){		 
					if( record.getNotPrimaryAlignmentFlag()) 
							noSecondary_bam1 ++;									 
					else if( record.getSupplementaryAlignmentFlag()) 
						 	nosupplementary_bam1 ++;		
					else
						throw new  Exception(record.getReadName() + " record flag error: record.isSecondaryOrSupplementary but not (secondary or supplementary) : " + record.getFlags());
				}else
					cleaned.add(record);
			
/*			//record these multi alignments for further investigation
			if(cleaned.size() != 2){
				for(SAMRecord record : cleaned){
					factory.getWriter().addAlignment(record);	
					nounsureAlignment ++;
					
				}
			}
*/			
			return cleaned;
		}
		
	 
		int classifyReads(ArrayList<SAMRecord> from1, ArrayList<SAMRecord> from2) throws Exception{
			ArrayList<SAMRecord> toremove1 = new ArrayList<SAMRecord>();
			ArrayList<SAMRecord> toremove2 = new ArrayList<SAMRecord>();
						
			for(SAMRecord record1 : from1){
				for(SAMRecord record2: from2){
					if(!record1.getReadName().equals(record2.getReadName()))
						throw new Exception("error during process: reads with different name are store in arrayList for comparison: "
								+ record1.getReadName() + " != " + record2.getReadName() ) ;
					if (record1.getFlags() == record2.getFlags() &&
							record1.getReferenceName().equals(record2.getReferenceName()) &&
							record1.getAlignmentStart() == record2.getAlignmentStart() &&
							record1.getAlignmentEnd() == record2.getAlignmentEnd() &&
							record1.getMappingQuality() == record2.getMappingQuality() &&
							record1.getCigarString().equals(record2.getCigarString())  &&
							Objects.equals(record1.getAttribute("MD") , record2.getAttribute("MD"))){
						sameWriter.getWriter().addAlignment(record1);
						toremove1.add(record1);
						toremove2.add(record2);
 					}
				}				
			}
			
			//record the left differnt aligner
			from1.removeAll(toremove1);
			for(SAMRecord record1 : from1) 
				diffWriter_first.getWriter().addAlignment(record1);
			
			from2.removeAll(toremove2);
			for(SAMRecord record2: from2)
				diffWriter_second.getWriter().addAlignment(record2);	
			
			//count unique alignment number
			noDiff_bam1 += from1.size();
			noDiff_bam2 += from2.size();
			
			return toremove1.size();			
		}
			
		public static void main(String[] args) throws Exception{
					
			Options op = new Options(AlignerCompare.class, args);    
		    if(op.hasHelpOption()){
		    	System.out.println(Messages.getMessage("USAGE_AlignerCompare"));
		    	op.displayHelp();
		    	System.exit(0);		
		    }
	    
		    if( op.getInputFileNames().length != 2 
		    		|| op.getOutputFileNames().length != 1 ){
		    	System.err.println("improper parameters passed to command line, please refer to");
		    	System.out.println(Messages.getMessage("USAGE_AlignerCompare"));
		    	op.displayHelp();
		    	System.exit(1);	
		    }
		    
		    File f1 = new File(op.getInputFileNames()[0]);
		    File f2 = new File(op.getInputFileNames()[1]);	
			if(!  f1.exists()  || ! f2.exists()) 
				throw new Exception("input not exists: " + args[0] + " or " + args[1]);
			
			//assign to true if no "compareAll" option
			boolean flag = ! op.hasCompareAllOption();
			
			if(op.hasLogOption())
				logger = QLoggerFactory.getLogger(AlignerCompare.class,op.getLogFile(), op.getLogLevel());
			else
				logger = QLoggerFactory.getLogger(AlignerCompare.class,op.getOutputFileNames()[0] + ".log", op.getLogLevel());	

			String version = org.qcmg.qmule.Main.class.getPackage().getImplementationVersion();	
  			logger.logInitialExecutionStats( "qmule " + AlignerCompare.class.getName(), version,args);

 			long startTime = System.currentTimeMillis();
			AlignerCompare compare = new AlignerCompare( f1, f2, op.getOutputFileNames()[0], flag );				

			logger.info( String.format("It took %d hours, %d minutes to perform the comparison",
					 (int) (System.currentTimeMillis() - startTime) / (1000*60*60), 
					 (int) ( (System.currentTimeMillis() - startTime) / (1000*60) ) % 60)  );
			logger.logFinalExecutionStats(0);
			
		}
 
	  
}
