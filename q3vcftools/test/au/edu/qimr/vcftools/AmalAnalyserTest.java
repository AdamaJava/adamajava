package au.edu.qimr.vcftools;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class AmalAnalyserTest {
	
	@Test
	public void getAllelicDistFromMap() {
		assertArrayEquals(new String[]{"GT:1","GT:2","GT:3","GT:4","GT:5","GT:6","GT:7","GT:8","GT:9"}, Arrays.copyOfRange(new String[]{"#chr","position","ref","alt","GT:1","GT:2","GT:3","GT:4","GT:5","GT:6","GT:7","GT:8","GT:9","AC:1","AC:2","AC:3","AC:4","AC:5","AC:6","AC:7","AC:8","AC:9","ALL-AC:1","ALL-AC:2","ALL-AC:3","ALL-AC:4","ALL-AC:5","Score"}, 4, 13));
	}
	
	@Test
	public void doesArrayOnlyContainEmptyGTs() {
		assertEquals(false, AmalAnalyser.doesArrayContainOnlyEmptyGT(new String[]{"GT:1","GT:2","GT:3","GT:4","GT:5","GT:6","GT:7","GT:8","GT:9"}));
		assertEquals(false, AmalAnalyser.doesArrayContainOnlyEmptyGT(new String[]{"GT:1","GT:2","GT:3","GT:4","./.","GT:6","GT:7","GT:8","./."}));
		assertEquals(false, AmalAnalyser.doesArrayContainOnlyEmptyGT(new String[]{"./.","GT:2","GT:3","GT:4","./.","GT:6","./.","./.","./."}));
		assertEquals(true, AmalAnalyser.doesArrayContainOnlyEmptyGT(new String[]{"./.","./.","./.","./.","./.","./.","./.","./.","./."}));
		assertEquals(true, AmalAnalyser.doesArrayContainOnlyEmptyGT(new String[]{"./."}));
	}
	
	@Test
	public void doesArrayContainEmptyGTs() {
		assertEquals(false, AmalAnalyser.doesArrayContainEmptyGT(new String[]{"GT:1","GT:2","GT:3","GT:4","GT:5","GT:6","GT:7","GT:8","GT:9"}));
		assertEquals(true, AmalAnalyser.doesArrayContainEmptyGT(new String[]{"GT:1","GT:2","GT:3","GT:4","./.","GT:6","GT:7","GT:8","./."}));
		assertEquals(true, AmalAnalyser.doesArrayContainEmptyGT(new String[]{"./.","GT:2","GT:3","GT:4","./.","GT:6","./.","./.","./."}));
		assertEquals(true, AmalAnalyser.doesArrayContainEmptyGT(new String[]{"./.","./.","./.","./.","./.","./.","./.","./.","./."}));
		assertEquals(false, AmalAnalyser.doesArrayContainEmptyGT(new String[]{"0/1","0/1","0/1","0/1","0/1","0/1","0/1","0/1","0/1"}));
		assertEquals(false, AmalAnalyser.doesArrayContainEmptyGT(new String[]{"0/1"}));
	}
	
	@Test
	public void whatGTsContainVariants() {
		assertEquals("1,2,3,4,5,6,7,8,9", AmalAnalyser.whatGTsContainVariant(new String[]{"0/1","0/1","0/1","0/1","0/1","0/1","0/1","0/1","0/1"}));
		assertEquals("1", AmalAnalyser.whatGTsContainVariant(new String[]{"0/1"}));
		assertEquals("", AmalAnalyser.whatGTsContainVariant(new String[]{"./."}));
		assertEquals("", AmalAnalyser.whatGTsContainVariant(new String[]{"."}));
		assertEquals("", AmalAnalyser.whatGTsContainVariant(new String[]{""}));
		assertEquals("", AmalAnalyser.whatGTsContainVariant(new String[]{null}));
		assertEquals("", AmalAnalyser.whatGTsContainVariant(null));
		assertEquals("1", AmalAnalyser.whatGTsContainVariant(new String[]{"0/1","./."}));
		assertEquals("1", AmalAnalyser.whatGTsContainVariant(new String[]{"0/1","./.","."}));
		assertEquals("1,4", AmalAnalyser.whatGTsContainVariant(new String[]{"0/1","./.",".","1/1"}));
	}

}
