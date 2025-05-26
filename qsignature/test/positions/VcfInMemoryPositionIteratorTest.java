package positions;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.sig.SignatureGeneratorTest;
import org.qcmg.sig.positions.VcfInMemoryPositionIterator;

public class VcfInMemoryPositionIteratorTest {
	
	@Rule
	public  TemporaryFolder testFolder = new TemporaryFolder();
	
	@Test
	public void getRecords() throws IOException {
		final File positionsOfInterestFile = testFolder.newFile("getRecords.snps.txt");
	    SignatureGeneratorTest.writeSnpPositionsFile(positionsOfInterestFile);
	    
	    VcfInMemoryPositionIterator vspi = new VcfInMemoryPositionIterator(positionsOfInterestFile, 4);
	    List<ChrPosition> positions = new ArrayList<>();
	    
	    while (vspi.hasNext()) {
	    	positions.add(vspi.next());
	    }
	    
	    assertEquals(7, positions.size());
	    
	    vspi.close();
	    positions.forEach(cp -> System.out.println( cp.toString()));
	}
	@Test
	public void getRecordsVcf() throws IOException {
		final File positionsOfInterestFile = testFolder.newFile("getRecordsVcf.snps.vcf");
		SignatureGeneratorTest.writeSnpPositionsVcf(positionsOfInterestFile);
		
		VcfInMemoryPositionIterator vspi = new VcfInMemoryPositionIterator(positionsOfInterestFile);
		List<ChrPosition> positions = new ArrayList<>();
		
		while (vspi.hasNext()) {
			positions.add(vspi.next());
		}
		
		assertEquals(7, positions.size());
		
		vspi.close();
		positions.forEach(cp -> System.out.println( cp.toString()));
	}

}
