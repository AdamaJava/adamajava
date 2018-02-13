package org.qcmg.picard.util;

import static org.junit.Assert.*;
import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class QueryIntervalUtilsTest {
	
	@Test
	public void getEmptyIntervals() {
		assertEquals(true, QueryIntervalUtils.getQueryIntervalsFromRegions(null, null).isEmpty());
		List<String> regions = Arrays.asList("");
		assertEquals(true, QueryIntervalUtils.getQueryIntervalsFromRegions(regions, null).isEmpty());
		regions = Arrays.asList("chr1");
		assertEquals(true, QueryIntervalUtils.getQueryIntervalsFromRegions(regions, null).isEmpty());
	}
	
	@Test
	public void getIntervalsWholeContigsOnly() {
		SAMSequenceDictionary dict = new SAMSequenceDictionary();
		dict.addSequence(new SAMSequenceRecord("seq_no_1", 123));
		List<String> regions = Arrays.asList("");
		assertEquals(true, QueryIntervalUtils.getQueryIntervalsFromRegions(regions, dict).isEmpty());
		regions = Arrays.asList("chr1");
		assertEquals(true, QueryIntervalUtils.getQueryIntervalsFromRegions(regions, dict).isEmpty());
		regions = Arrays.asList("seq_no_1");
		assertEquals(false, QueryIntervalUtils.getQueryIntervalsFromRegions(regions, dict).isEmpty());
		assertEquals(new QueryInterval(0,1,-1), QueryIntervalUtils.getQueryIntervalsFromRegions(regions, dict).get(0));
		
		// add another 
		dict.addSequence(new SAMSequenceRecord("seq_no_2", 456));
		assertEquals(new QueryInterval(0,1,-1), QueryIntervalUtils.getQueryIntervalsFromRegions(Arrays.asList("seq_no_1"), dict).get(0));
		assertEquals(new QueryInterval(1,1,-1), QueryIntervalUtils.getQueryIntervalsFromRegions(Arrays.asList("seq_no_2"), dict).get(0));
		assertEquals(new QueryInterval(1,1,2), QueryIntervalUtils.getQueryIntervalsFromRegions(Arrays.asList("seq_no_2:1-2"), dict).get(0));
		assertEquals(new QueryInterval(0,1,-1), QueryIntervalUtils.getQueryIntervalsFromRegions(Arrays.asList("seq_no_1:1--1"), dict).get(0));
		assertEquals(new QueryInterval(0,1,123), QueryIntervalUtils.getQueryIntervalsFromRegions(Arrays.asList("seq_no_1:1-123"), dict).get(0));
	}

}
