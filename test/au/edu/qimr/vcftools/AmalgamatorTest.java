package au.edu.qimr.vcftools;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

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

}
