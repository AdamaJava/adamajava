package au.edu.qimr.vcftools;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class AmalgamatorTest {
	
	@Test
	public void getAllelicDistFromMap() {
		Map<String, Integer> m = new HashMap<>();
		m.put("A",123);
		assertEquals("123,0,0", Amalgamator.getAllelicDistFromMap(m, "A", "C"));
		m.put("C",321);
		assertEquals("123,321,0", Amalgamator.getAllelicDistFromMap(m, "A", "C"));
		m.put("G",222);
		assertEquals("123,321,222", Amalgamator.getAllelicDistFromMap(m, "A", "C"));
		m.put("T",111);
		assertEquals("123,321,333", Amalgamator.getAllelicDistFromMap(m, "A", "C"));
		assertEquals("123,222,432", Amalgamator.getAllelicDistFromMap(m, "A", "G"));
		assertEquals("123,111,543", Amalgamator.getAllelicDistFromMap(m, "A", "T"));
	}
	
	@Test
	public void getStringFromArray() {
		String[] array = new String[]{"hello"};
		assertEquals("hello", Amalgamator.getStringFromArray(array, 0));
		try {
			Amalgamator.getStringFromArray(array, 1);
			Assert.fail("should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			Amalgamator.getStringFromArray(array, 10);
			Assert.fail("should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		try {
			Amalgamator.getStringFromArray(array, -1);
			Assert.fail("should have thrown an exception");
		} catch (IllegalArgumentException iae) {}
		
		array = new String[]{"hello","there","world"};
		assertEquals("hello", Amalgamator.getStringFromArray(array, 0));
		assertEquals("there", Amalgamator.getStringFromArray(array, 1));
		assertEquals("world", Amalgamator.getStringFromArray(array, 2));
	}
	@Test
	public void getStringFromArrayWithDelim() {
		String[] array = new String[]{"hello&there&world"};
		assertEquals("hello", Amalgamator.getStringFromArray(array, 0));
		array = new String[]{"why&hello","up&there","dear&world"};
		assertEquals("why", Amalgamator.getStringFromArray(array, 0));
		assertEquals("up", Amalgamator.getStringFromArray(array, 1));
		assertEquals("dear", Amalgamator.getStringFromArray(array, 2));
	}
	
	@Test
	public void getPositionFromHeader() {
		String[] array = new String[]{"hello"};
		assertEquals(0, Amalgamator.getPositionFromHeader(array, "hello"));
		assertEquals(-1, Amalgamator.getPositionFromHeader(array, "there"));
		assertEquals(-1, Amalgamator.getPositionFromHeader(array, "world"));
		array = new String[]{"why&hello","there","world"};
		assertEquals(-1, Amalgamator.getPositionFromHeader(array, "hello"));
		assertEquals(0, Amalgamator.getPositionFromHeader(array, "why&hello"));
		assertEquals(1, Amalgamator.getPositionFromHeader(array, "there"));
		assertEquals(2, Amalgamator.getPositionFromHeader(array, "world"));
	}

}
