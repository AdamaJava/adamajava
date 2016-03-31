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

}
