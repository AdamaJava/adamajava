package org.qcmg.qsv.discordantpair;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qsv.util.TestUtil;

public class DiscordantPairClusterTest {

    private DiscordantPairCluster cluster = null;
    
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException, Exception {
        cluster = TestUtil.setupSolidCluster(PairGroup.AAC, "somatic", testFolder, "chr7", "chr7");
        assertEquals(cluster.getZp(), "AAC");
    }

    @After
    public void tearDown() {
        cluster = null;
    }

    @Test
    public void testSetters() {
        cluster.setLeftStart(1);
        assertEquals(1, cluster.getLeftStart());
        cluster.setLeftEnd(2);
        assertEquals(2, cluster.getLeftEnd());
        cluster.setRightStart(3);
        assertEquals(3, cluster.getRightStart());
        cluster.setRightEnd(4);
        assertEquals(4, cluster.getRightEnd());

        assertEquals("somatic", cluster.getType());
    }
    
    @Test
    public void testFindLeftStartOfCluster() {    	
    	cluster.findLeftStartOfCluster();
    	assertEquals(140188227, cluster.getLeftStart());
    }
    
    @Test
    public void testFindLeftEndOfCluster() {    	
    	cluster.findLeftEndOfCluster();
    	assertEquals(140189108, cluster.getLeftEnd());
    }
    
    @Test
    public void testFindRightStartOfCluster() {    	
    	cluster.findRightStartOfCluster();
    	assertEquals(140191044, cluster.getRightStart());
    }
    
    @Test
    public void testStrandOrientations() {    	
    	assertEquals("+/+", cluster.countStrandOrientations());
    	
    	assertEquals(2, cluster.getStrandOrientations().get("-/-").get());
    	assertEquals(4, cluster.getStrandOrientations().get("+/+").get());
    	
    }
    
    @Test
    public void testGetChrRegionFrom() {
    	assertEquals("chr7:140188227-140189108", cluster.getChrRegionFrom());
    }
    
    @Test
    public void testGetChrRegionTo() {
    	assertEquals("chr7:140191044-140191629", cluster.getChrRegionTo());
    }
    
    @Test
    public void getIndexOfPair() {
    	MatePair m = new MatePair("1887_329_319:20110221052813657,chr7,140188962,140189009,AAC,113,true,1887_329_319:20110221052813657,chr7,140191372,140191421,AAC,177,true,R1R2");
    	assertEquals(3, cluster.getIndexOfPair(m));    
    }
    
    @Test
    public void testCopyAndOrderCurrentClusterPairs() {
    	List<MatePair> mates = cluster.copyAndOrderCurrentClusterPairs();
    	assertEquals(6, mates.size());
    	assertEquals(140191044, mates.get(0).getRightMate().getStart());
    }
    
    @Test
    public void testGetBreakpointsCategory() throws Exception {
    	assertBreakpoints("AAC", "chr7", "chr7", "1", 140189108,140191044);
    	assertBreakpoints("AAB", "chr7", "chr7", "2", 140188227,140191629);
    	assertBreakpoints("AAB", "chr7", "chr7", "5", 140188227,140191629);
    	assertBreakpoints("BAA", "chr7", "chr7", "3", 140189108,140191629);
    	assertBreakpoints("BAA", "chr7", "chr7", "4", 140188227,140191044);
    }

	private void assertBreakpoints(String pair, String chr1,
			String chr2, String cat, int expectedLeft, int expectedRight) {
		QPrimerCategory c = new QPrimerCategory("AAC", "chr7", "chr7", "id", "pe");
    	c.setPrimaryCategoryNo(cat);
    	cluster.setqPrimerCateory(c);    	
    	assertEquals(expectedLeft, cluster.getLeftBreakPoint());
    	assertEquals(expectedRight, cluster.getRightBreakPoint());		
	}
	
	@Test
	public void testSetNormalRange() {
		cluster.setNormalRange(3000);
		assertEquals(140185667, cluster.getCompareLeftStart());
		assertEquals(140191667, cluster.getCompareLeftEnd());
		assertEquals(140188336, cluster.getCompareRightStart());
		assertEquals(140194336, cluster.getCompareRightEnd());
	}
	
	@Test
	public void testFinalizeAndGermlineRescue() throws Exception {		
		assertEquals("somatic", cluster.getType());
		assertEquals(1, cluster.getId());
		assertEquals("+/+", cluster.getStrandOrientation());
		assertEquals("1", cluster.getqPrimerCateory().getPrimaryCategoryNo());
		assertEquals(8, cluster.getLowConfidenceNormalMatePairs());
	}





}
