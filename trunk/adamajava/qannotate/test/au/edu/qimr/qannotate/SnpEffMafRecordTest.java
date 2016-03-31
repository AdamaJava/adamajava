package au.edu.qimr.qannotate;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.common.util.Constants;

import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class SnpEffMafRecordTest {
	
	@Test
	public void toMafString() {
		SnpEffMafRecord rec = new SnpEffMafRecord();
		rec.setDefaultValue();
		
//		System.out.println("rec: " + rec.getMafLine());
		assertEquals(SnpEffMafRecord.column , rec.getMafLine().split(Constants.TAB + "").length);
	}
	
	@Test
	public void getMafColumn() {
		SnpEffMafRecord rec = new SnpEffMafRecord();
		rec.setDefaultValue();
		
		for (int i = -100 ; i < 1000000 ; i++) {
			
			if (i < 1 || i > SnpEffMafRecord.column) {
				try {
					rec.getColumnValue(i);
					fail("Should have thrown an IllegalArgumentException");
				} catch (IllegalArgumentException iae) {}
			} else  {
//				assertNotNull(rec.getColumnValue(i));
			}
		}
	}

}
