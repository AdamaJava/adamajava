package au.edu.qimr.tiledaligner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TiledAlignerLongMapTest {

	@Test
	public void convertStringToINtArray() {
		assertArrayEquals(new int[] {1,2,3}, TiledAlignerLongMap.convertStringToIntArray("1,2,3"));
		assertArrayEquals(new int[] {1,2,3,-1294967296}, TiledAlignerLongMap.convertStringToIntArray("1,2,3,3000000000"));
	}
	
	@Test
	public void unsignedIntToLong() {
		assertEquals("2304697863", Integer.toUnsignedString(-1990269433));
		assertEquals("3000000000", Integer.toUnsignedString(-1294967296));
	}
}
