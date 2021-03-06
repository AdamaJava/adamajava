package au.edu.qimr.qannotate;

import static org.junit.Assert.*;

import org.junit.Test;
import org.qcmg.common.util.Constants;

import au.edu.qimr.qannotate.utils.MafElement;
import au.edu.qimr.qannotate.utils.SnpEffMafRecord;

public class SnpEffMafRecordTest {
	
	@Test
	public void toMafString() {
		SnpEffMafRecord rec = new SnpEffMafRecord();
		
		assertEquals(MafElement.values().length , rec.getMafLine(true).split(Constants.TAB_STRING).length);
	}
	
	@Test
	public void getMafColumn() {
		SnpEffMafRecord rec = new SnpEffMafRecord();
		
		
		for (int i = -10 ; i < 1000 ; i++) {
			
			if (i < 1 || i > MafElement.values().length) {
				try {
					rec.getColumnValue(i);
					fail("Should have thrown an IllegalArgumentException");
				} catch (ArrayIndexOutOfBoundsException iae) {
					// intentionally left blank
				}
			} else  {
				assertEquals(rec.getColumnValue(i), MafElement.getByColumnNo(i).getDefaultValue())  ;
			}
		}
	}

}
