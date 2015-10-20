package org.qcmg.split;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;

import org.junit.Assert;
import org.junit.Test;
import org.qcmg.common.util.TabTokenizer;

public class OneLevelSplit {
	
	private final HashMap<Integer, String> zcToFileNameMap = new HashMap<Integer, String>();
	private final HashMap<String, Integer> fileNameToZcMap = new HashMap<String, Integer>();
	private final HashMap<Integer, SAMFileHeader> zcToOutputHeaderMap = new HashMap<Integer, SAMFileHeader>();
	
	
	
	
	
	@Test
	public void testExtractHeaderDetails() throws Exception {
		SAMFileHeader header = setupHeader();
		extractHeaderReadGroupDetails(header);
		prepareOutputHeaders(header);
		
		System.out.println("zcToFileNameMap: ");
		for (Entry<Integer, String> entry : zcToFileNameMap.entrySet()) {
			System.out.println("entry: " + entry.getKey() + ":" + entry.getValue());
		}
		System.out.println("fileNameToZcMap: ");
		for (Entry<String, Integer> entry : fileNameToZcMap.entrySet()) {
			System.out.println("entry: " + entry.getKey() + ":" + entry.getValue());
		}
		System.out.println("zcToOutputHeaderMap: ");
		for (Entry<Integer, SAMFileHeader> entry : zcToOutputHeaderMap.entrySet()) {
			System.out.println("entry: " + entry.getKey() + ":" + entry.getValue().getTextHeader());
		}
		
		
		Assert.assertEquals(true, doMultipleMergesExist(header.getProgramRecords()));
		singleLevelSplit(header);
	}
	
	private void singleLevelSplit(SAMFileHeader header) throws IOException {
		
		List<SAMProgramRecord> pgs = header.getProgramRecords();
		SAMProgramRecord latestMerge = getLatestMerge(pgs);
		
		
		System.out.println("latestMerge: " + latestMerge.getCommandLine());
		System.out.println("group id: " + latestMerge.getProgramGroupId());
		System.out.println("zc: " + latestMerge.getAttribute("zc"));

		Map<Integer, String> singleSplitMap = new HashMap<Integer, String>();
		List<String> inputs = getProgramRecordInputs(latestMerge);
		determineSingleSplitOutputs(inputs, singleSplitMap, pgs, null);
		
		System.out.println("Should now have a single split map!!");
		
		for (Entry<Integer, String> entry : singleSplitMap.entrySet() ) {
			System.out.println("entry: " + entry.getKey() + ":" + entry.getValue());
		}
		
	}
	
	private void determineSingleSplitOutputs(List<String> inputs, Map<Integer, String> singleSplitMap, List<SAMProgramRecord> pgs, String parentInput) throws IOException {
		for (String s : inputs) {
			boolean matchFound = false;
			System.out.println("input: " + s);
			
			// if the input is in the zcToFileMap, we are done - add to singleSplitMap and move on
			for (Entry<String,Integer> entry : fileNameToZcMap.entrySet()) {
				if (entry.getKey().equals(s) || entry.getKey().equals(getLinkedFilename(s))) {
					// we've got a match
					if (null != parentInput) {
						singleSplitMap.put(entry.getValue(), parentInput);
					} else {
						singleSplitMap.put(entry.getValue(), s);
					}
					matchFound = true;
					break;
				}
			}
			
			if (matchFound) continue;
			
			// need to find program record for input
			
			SAMProgramRecord linkedMerge = getPGforInput(s, pgs);
			if (null != linkedMerge) {
				System.out.println("linkedMerge: " + ((null != linkedMerge) ? linkedMerge.getCommandLine() : "null"));
				determineSingleSplitOutputs(getProgramRecordInputs(linkedMerge), singleSplitMap, pgs, s);
			}
		}
	}
	
	private String getLinkedFilename(String filename) throws IOException {
		Path path = Paths.get(filename, "");
		if (Files.isSymbolicLink(path)) {
			return Files.readSymbolicLink(path).toString();
		} else return filename;
	}
	
	private SAMProgramRecord getPGforInput(String input, List<SAMProgramRecord> pgs) {
		for (SAMProgramRecord pg : pgs) {
			if ("qbammerge".equals(pg.getProgramName())) {
				String output = getProgramRecordOutputs(pg);
				if (output.equals(input)) {
					return pg;
				}
			}
		}
		
		return null;
	}
	
	private List<String> getProgramRecordInputs(SAMProgramRecord pg) {
		List<String> inputs = new ArrayList<String>();
		
		String cl = pg.getCommandLine();
		String [] params = TabTokenizer.tokenize(cl, ' ');
		
		// loop through array - when we get a "-i", the next param is an input
		boolean get = false;
		for (String p : params) {
			if (get) inputs.add(p);
			get = "-i".equals(p);
		}
		
		return inputs;
	}
	private String getProgramRecordOutputs(SAMProgramRecord pg) {
		String output = null;
		
		String cl = pg.getCommandLine();
		String [] params = TabTokenizer.tokenize(cl, ' ');
		
		// loop through array - when we get a "-i", the next param is an input
		boolean get = false;
		for (String p : params) {
			if (get) output = p;
			get = "-o".equals(p);
		}
		
		return output;
	}
	
	private SAMProgramRecord getLatestMerge(List<SAMProgramRecord> pgs) {
		// we are looking for the qbammerge program record that has the highest zc attribute
		SAMProgramRecord latestMerge = null;
		
		for (SAMProgramRecord pg : pgs) {
			if ("qbammerge".equals(pg.getProgramName())) {
				if (null == latestMerge) {
					latestMerge = pg;
				} else if (Integer.parseInt(pg.getAttribute("zc")) > Integer.parseInt(latestMerge.getAttribute("zc"))) {
					latestMerge = pg;
				}
			}
		}
		
		return latestMerge;
	}
	
	private boolean doMultipleMergesExist(List<SAMProgramRecord> pgs) {
		int counter = 0;
		
		for (SAMProgramRecord pg : pgs)
			if ("qbammerge".equalsIgnoreCase(pg.getProgramName()))
				counter++;
		
		return counter > 1;
	}
	
	
	
	
	
	private void prepareOutputHeaders(SAMFileHeader header) throws Exception {
		Split s = new Split();
		for (final SAMReadGroupRecord record : header.getReadGroups()) {
			String zc = Split.getAttributeZc(record);
			String[] params = Split.colonDelimitedPattern.split(zc);
			Integer zcInt = Integer.parseInt(params[0]);
			SAMFileHeader outputHeader = header.clone();
			s.preserveZCReadGroup(outputHeader, zcInt);
//			Split.conserveProgramRecords(outputHeader, zcInt);
			zcToOutputHeaderMap.put(zcInt, outputHeader);
		}
//		stripZCsFromOutputHeader();
	}

	private void stripZCsFromOutputHeader() {
		for (SAMFileHeader outputHeader : zcToOutputHeaderMap.values()) {
			for (final SAMReadGroupRecord record : outputHeader.getReadGroups()) {
				record.setAttribute("zc", null);
			}
			for (final SAMProgramRecord record : outputHeader
					.getProgramRecords()) {
				record.setAttribute("zc", null);
			}
		}
	}
	
	
	
	private void extractHeaderReadGroupDetails(SAMFileHeader header) throws Exception {
		for (SAMReadGroupRecord record : header.getReadGroups()) {
			String zc = Split.getAttributeZc(record);
			if (null == zc) {
				throw new Exception(
						"Input file header has RG fields lacking zx attributes");
			}
		
			String[] params = Split.colonDelimitedPattern.split(zc);
			Integer zcInt = Integer.parseInt(params[0]);
			String fileName = params[1];
			String previous = zcToFileNameMap.put(zcInt, fileName);
			if (null != previous && !previous.equals(fileName)) {
				throw new Exception(
						"Input file header contains conflicting output file details for ZC value "
								+ zcInt);
			}
			Integer previousZc = fileNameToZcMap.put(fileName, zcInt);
			if (null != previousZc && previousZc != zcInt) {
				throw new Exception(
						"Malformed merged BAM file. Multiple ZC-to-originating-file mappings.");
			}
		}
	}
	
	
	
	public SAMFileHeader setupHeader() {
		SAMFileHeader header = new SAMFileHeader();
		
		/*
		 * 
@RG	ID:20100905231122897	PL:SOLiD	PU:bioscope-pairing	LB:Library_20100816_D	zc:5:/mapped/S0416_20100819_1_FragPEBC.nopd.S16_04.bam	PI:190	DS:RUNTYPE{50x25RRBC}	DT:2010-09-06T09:11:22+1000	SM:S1
@RG	ID:20100907043607557	PL:SOLiD	PU:bioscope-pairing	LB:Library_20100816_D	zc:6:/mapped/S0416_20100819_2_FragPEBC.nopd.S16_04.bam	PI:190	DS:RUNTYPE{50x25RRBC}	DT:2010-09-07T14:36:07+1000	SM:S1
@RG	ID:20110414154541403	PL:SOLiD	PU:bioscope-pairing	LB:Library_20110316_T	zc:4:/mapped/S0414_20110326_1_FragPEBC.nopd.bcB16_03.bam	PI:176	DS:RUNTYPE{50x35RRBC}	DT:2011-04-15T01:45:41+1000	SM:S1
@PG	ID:ea20fbcf-2bc7-4ddd-8734-7706c7fb8eeb	PN:qbammerge	zc:7	VN:0.6pre (5204)	CL:qbammerge --log /path/lib/Library_20100816_D.bam.qmerg.log -o /path/lib/Library_20100816_D.bam -i /mapped/S0416_20100819_1_FragPEBC.nopd.S16_04.bam -i /mapped/S0416_20100819_2_FragPEBC.nopd.S16_04.bam
@PG	ID:82f4cc98-83ba-470b-860c-5872b9900f65	PN:qbammerge	zc:8	VN:0.6pre (5230)	CL:qbammerge --log /path/final/AAAA_1111.exome.ND.bam.qmerg.log -o /path/final/AAAA_1111.exome.ND.bam -i /path/lib/Library_20110316_T.bam -i /path/lib/Library_20100816_D.bam
		 */
		SAMReadGroupRecord rg1 = new SAMReadGroupRecord("20100905231122897");
		rg1.setLibrary("Library_20100816_D");
		rg1.setPlatform("SOLiD");
		rg1.setPlatformUnit("bioscope-pairing");
		rg1.setAttribute("zc", "5:/mapped/S0416_20100819_1_FragPEBC.nopd.S16_04.bam");
		
		SAMReadGroupRecord rg2 = new SAMReadGroupRecord("20100907043607557");
		rg2.setLibrary("Library_20100816_D");
		rg2.setPlatform("SOLiD");
		rg2.setPlatformUnit("bioscope-pairing");
		rg2.setAttribute("zc", "6:/mapped/S0416_20100819_2_FragPEBC.nopd.S16_04.bam");
		
		SAMReadGroupRecord rg3 = new SAMReadGroupRecord("20110414154541403");
		rg3.setLibrary("Library_20110316_T");
		rg3.setPlatform("SOLiD");
		rg3.setPlatformUnit("bioscope-pairing");
		rg3.setAttribute("zc", "4:/mapped/S0414_20110326_1_FragPEBC.nopd.bcB16_03.bam");
		
		header.addReadGroup(rg1);
		header.addReadGroup(rg2);
		header.addReadGroup(rg3);
		
		SAMProgramRecord pg1 = new SAMProgramRecord("ea20fbcf-2bc7-4ddd-8734-7706c7fb8eeb");
		pg1.setProgramName("qbammerge");
		pg1.setCommandLine("qbammerge --log /path/lib/Library_20100816_D.bam.qmerg.log -o /path/lib/Library_20100816_D.bam -i /mapped/S0416_20100819_1_FragPEBC.nopd.S16_04.bam -i /mapped/S0416_20100819_2_FragPEBC.nopd.S16_04.bam");
		pg1.setProgramVersion("0.6pre (5204)");
		pg1.setAttribute("zc","7");
		
		SAMProgramRecord pg2 = new SAMProgramRecord("82f4cc98-83ba-470b-860c-5872b9900f65");
		pg2.setProgramName("qbammerge");
		pg2.setCommandLine("qbammerge --log /path/final/AAAA_1111.exome.ND.bam.qmerg.log -o /path/final/AAAA_1111.exome.ND.bam -i /path/lib/Library_20110316_T.bam -i /path/lib/Library_20100816_D.bam");
		pg2.setProgramVersion("0.6pre (5230)");
		pg2.setAttribute("zc","8");
		
		header.addProgramRecord(pg1);
		header.addProgramRecord(pg2);
		
		
		return header;
	}

}
