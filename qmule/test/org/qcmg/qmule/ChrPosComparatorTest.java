package org.qcmg.qmule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.gff3.Gff3Record;
import org.qcmg.gff3.GFF3RecordChromosomeAndPositionComparator;

public class ChrPosComparatorTest {
	
	@Test
	public void testGffComp() {
		Gff3Record r1 = new Gff3Record();
		r1.setSeqId("GL0000.191");
		r1.setStart(1);
		r1.setEnd(2);
		Gff3Record r2 = new Gff3Record();
		r2.setSeqId("chr1");
		r2.setStart(1);
		r2.setEnd(2);
		
		List<Gff3Record> list = new ArrayList<Gff3Record>();
		list.add(r1);
		list.add(r2);
		
		Collections.sort(list, new GFF3RecordChromosomeAndPositionComparator());
		
		Assert.assertEquals(r2, list.get(0));
	}

}
