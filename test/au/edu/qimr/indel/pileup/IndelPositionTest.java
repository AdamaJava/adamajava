package au.edu.qimr.indel.pileup;


import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.picard.SAMFileReaderFactory;


public class IndelPositionTest {
	static final String inputIndel = "indel.vcf"; 
	static final String inputBam = "tumor.sam"; 
	QLogger logger = QLoggerFactory.getLogger(IndelPileupTest.class);
	
	@BeforeClass
	public static void createInput() {	
		IndelPileupTest.createVcf();
		IndelPileupTest.CreateSam();
	}	
	
	@Test
	public void getPileupedVcfTest() throws Exception{
//		ReadIndels read = new ReadIndels(logger);
//		read.LoadSingleIndels(new File(inputIndel));
//		Map<ChrPosition, IndelPosition> map = read.getIndelMap();
//				
//		//get pool		
//		SamReader inreader =  SAMFileReaderFactory.createSAMFileReader(new File(inputBam));
//		List<SAMRecord> pool = new ArrayList<SAMRecord>();
//        for(SAMRecord record : inreader){
//        	pool.add(record);
//        }
//        inreader.close();
//		
//		IndelPileup pileup = null; 
//		for(ChrPosition pos: map.keySet()){
//			pileup = new IndelPileup( map.get(pos), 13, 3); 		
//				pileup.pileup(pool);
//		}		
	}
 

}
