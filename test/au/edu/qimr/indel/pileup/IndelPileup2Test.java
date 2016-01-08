package au.edu.qimr.indel.pileup;

import java.io.File;

import org.junit.Before;

import au.edu.qimr.indel.IniFileTest;

public class IndelPileup2Test {
	
	@Before
	public void before() {
		 
		IndelMTTest.createBam(IndelMTTest. TEST_BAM_NAME);
  
 		
 		IndelMTTest.createGatkVcf(IndelMTTest.inputVcf);
 
		
 
	 	
		
	

	}
	

}
