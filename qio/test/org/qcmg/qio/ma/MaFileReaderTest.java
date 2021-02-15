package org.qcmg.qio.ma;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.qcmg.qio.gff.GffReader;
import org.qcmg.qio.gff.GffReaderTest;
import org.qcmg.qio.ma.MaDefLine;
import org.qcmg.qio.ma.MaFileReader;
import org.qcmg.qio.ma.MaMapping;
import org.qcmg.qio.ma.MaRecord;

public class MaFileReaderTest {
	private static File EMPTY_FILE ;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
	@ClassRule
	public static TemporaryFolder testFolder = new TemporaryFolder();
	
	@BeforeClass
	public static void setup() throws IOException {
		EMPTY_FILE  = testFolder.newFile("empty.gff");
 		GffReaderTest.createTestFile(EMPTY_FILE.getAbsolutePath(), new ArrayList<String>());	
	}

    @Test
    public final void decodeDefLineNoMappings() {
    	try(MaFileReader reader = new MaFileReader(EMPTY_FILE);) {
	        String defLine = ">1_8_184_F3";
	        MaDefLine d = reader.parseDefLine(defLine);
	
	        assertTrue(d.getReadName().equals("1_8_184"));
	        assertFalse(d.hasMappings());
        } catch (IOException e) {
        	Assert.fail("IOException during unit test");		
		}
    }

    @Test
    public final void decodeDefLineWithMappings() throws IOException {
        ExpectedException.none();

        MaFileReader reader = new MaFileReader(EMPTY_FILE);
        String defLine = ">1_8_184_F3,8_-30078837.2:(31.3.0):q4,10_-9547536.2:(27.2.0):q1,18_-46572772.2:(26.2.0):q1,23_16023538.2:(24.2.0):q0";
        MaDefLine d = reader.parseDefLine(defLine);

        assertTrue(d.getReadName().equals("1_8_184"));
        assertTrue(d.hasMappings());

        Iterator<MaMapping> iter = d.iterator();

        assertTrue(4 == d.getNumberMappings());

        assertTrue(iter.hasNext());
        if (iter.hasNext()) {
            MaMapping mapping = iter.next();
            assertTrue(mapping.getChromosome().equals("8"));
            assertTrue(mapping.getLocation(), mapping.getLocation().equals("-30078837"));
            assertTrue(31 == mapping.getLength());
            assertTrue(3 == mapping.getPossibleMismatches());
            assertTrue(0 == mapping.getSeedStart());
            assertTrue(mapping.getQuality(), mapping.getQuality().equals("q4"));
        }

        assertTrue(iter.hasNext());
        if (iter.hasNext()) {
            MaMapping mapping = iter.next();
            assertTrue(mapping.getChromosome().equals("10"));
            assertTrue(mapping.getLocation(), mapping.getLocation().equals("-9547536"));
            assertTrue(27 == mapping.getLength());
            assertTrue(2 == mapping.getPossibleMismatches());
            assertTrue(0 == mapping.getSeedStart());
            assertTrue(mapping.getQuality(), mapping.getQuality().equals("q1"));
        }

        assertTrue(iter.hasNext());
        if (iter.hasNext()) {
            MaMapping mapping = iter.next();
            assertTrue(mapping.getChromosome().equals("18"));
            assertTrue(mapping.getLocation(), mapping.getLocation().equals("-46572772"));
            assertTrue(26 == mapping.getLength());
            assertTrue(2 == mapping.getPossibleMismatches());
            assertTrue(0 == mapping.getSeedStart());
            assertTrue(mapping.getQuality(), mapping.getQuality().equals("q1"));
        }

        assertTrue(iter.hasNext());
        if (iter.hasNext()) {
            MaMapping mapping = iter.next();
            assertTrue(mapping.getChromosome().equals("23"));
            assertTrue(mapping.getLocation(), mapping.getLocation().equals("16023538"));
            assertTrue(24 == mapping.getLength());
            assertTrue(2 == mapping.getPossibleMismatches());
            assertTrue(0 == mapping.getSeedStart());
            assertTrue(mapping.getQuality(), mapping.getQuality().equals("q0"));
        }

        assertFalse(iter.hasNext());
    }

    @Test
    public final void decodeRecord() throws IOException {
        ExpectedException.none();
        MaFileReader reader = new MaFileReader(EMPTY_FILE);
        String defLine = ">1_8_184_F3,8_-30078837.2:(31.3.0):q4,10_-9547536.2:(27.2.0):q1,18_-46572772.2:(26.2.0):q1,23_16023538.2:(24.2.0):q0";
        String sequence = "T1100011201110111121111111111211121.112211122111221";       
        new MaRecord(reader.parseDefLine(defLine), sequence);     
    }
    
    @Test
    public final void headerTest() throws IOException {
    	
    	//create file
        List<String> headerRecords = new ArrayList<>();
        headerRecords.add("#firstline");
        headerRecords.add("#secondline");
        headerRecords.add("#thirdline");
        File f  = testFolder.newFile();
        GffReaderTest.createTestFile(f.getAbsolutePath(), headerRecords);
        MaFileReader reader = new MaFileReader(f);
        
      //  MAHeader header = new MAHeader(headerRecords);

        Iterator<String> iter = reader.getHeader().iterator();

        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("#firstline"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("#secondline"));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals("#thirdline"));
        assertFalse(iter.hasNext());
    }

}
