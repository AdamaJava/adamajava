package org.qcmg.sig;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.qcmg.sig.model.BaseReadGroup;

public class SignatureGeneratorBespokeTest {
	
	@Test
	public void getEncodedDistString() {
		assertEquals(null, SignatureGeneratorBespoke.getEncodedDist(null));
		assertEquals(null, SignatureGeneratorBespoke.getEncodedDist(new ArrayList<>()));
		
		List<BaseReadGroup> list = new ArrayList<>();
		list.add(new BaseReadGroup('A',""));
		assertEquals("1-0-0-0", SignatureGeneratorBespoke.getEncodedDist(list));
		list.add(new BaseReadGroup('A',""));
		assertEquals("2-0-0-0", SignatureGeneratorBespoke.getEncodedDist(list));
		list.add(new BaseReadGroup('T',""));
		assertEquals("2-0-0-1", SignatureGeneratorBespoke.getEncodedDist(list));
		list.add(new BaseReadGroup('T',""));
		assertEquals("2-0-0-2", SignatureGeneratorBespoke.getEncodedDist(list));
		list.clear();
		list.add(new BaseReadGroup('C',""));
		assertEquals("0-1-0-0", SignatureGeneratorBespoke.getEncodedDist(list));
		list.clear();
		list.add(new BaseReadGroup('G',""));
		assertEquals("0-0-1-0", SignatureGeneratorBespoke.getEncodedDist(list));
		list.clear();
		list.add(new BaseReadGroup('T',""));
		assertEquals("0-0-0-1", SignatureGeneratorBespoke.getEncodedDist(list));
		
	}

}
