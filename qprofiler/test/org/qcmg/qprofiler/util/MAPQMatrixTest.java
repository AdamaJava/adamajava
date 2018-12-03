package org.qcmg.qprofiler.util;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.Assert;

import org.junit.Test;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.qprofiler.util.MAPQMatrix;
import org.qcmg.qprofiler.util.SummaryByCycleUtils;
import org.qcmg.qprofiler.util.MAPQMatrix.MatrixType;

public class MAPQMatrixTest {
	
	@Test
	public void testAddToMatrix() {
		MAPQMatrix matrix = new MAPQMatrix();
		
		for (int i = 0 ; i < 100000 ; i++) {
			matrix.addToMatrix(i, MatrixType.CM);
		}
		for (int i = 0 ; i < 100000 ; i++) {
			matrix.addToMatrix(i, MatrixType.SM);
		}
		for (int i = 0 ; i < 100000 ; i++) {
			matrix.addToMatrix(i, MatrixType.LENGTH);
		}
		
		Assert.assertEquals(100000, matrix.getMatrixByType(MatrixType.CM).size());
		Assert.assertEquals(100000, matrix.getMatrixByType(MatrixType.SM).size());
		Assert.assertEquals(100000, matrix.getMatrixByType(MatrixType.LENGTH).size());
	}
	
	@Test
	public void testInCollection() {
		ConcurrentMap<MAPQMiniMatrix, AtomicLong> mapQCMMatrix = new ConcurrentSkipListMap<MAPQMiniMatrix, AtomicLong>();
		ConcurrentMap<MAPQMiniMatrix, AtomicLong> mapQSMMatrix = new ConcurrentSkipListMap<MAPQMiniMatrix, AtomicLong>();
		ConcurrentMap<MAPQMiniMatrix, AtomicLong> mapQLengthMatrix = new ConcurrentSkipListMap<MAPQMiniMatrix, AtomicLong>();
		
		ConcurrentMap<Integer, MAPQMatrix> mapQMatrix = new ConcurrentSkipListMap<Integer, MAPQMatrix>();
		ConcurrentMap<Integer, AtomicLong> counter = new ConcurrentSkipListMap<Integer, AtomicLong>();
		
		Random r = new Random();
		
		// reduce no of iterators to speed up the test
		for (int i = 0 ; i < 100000 ; i++) {
//			for (int i = 0 ; i < 10000000 ; i++) {
			
			MAPQMiniMatrix cmM = new MAPQMiniMatrix();
			MAPQMiniMatrix smM = new MAPQMiniMatrix();
			MAPQMiniMatrix lenM = new MAPQMiniMatrix();
			
			Integer mapQ = r.nextInt(100);
			SummaryByCycleUtils.incrementCount(counter, mapQ);
			
			cmM.setMapQ(mapQ);
			smM.setMapQ(mapQ);
			lenM.setMapQ(mapQ);
			
			MAPQMatrix matrix = mapQMatrix.get(mapQ);
			if (null == matrix) {
				MAPQMatrix newMatrix = new MAPQMatrix();
				matrix = mapQMatrix.putIfAbsent(mapQ, newMatrix);
				if (null == matrix)
					matrix = newMatrix;
				
//				synchronized (mapQMatrix) {
//					matrix = new MAPQMatrix();
//					MAPQMatrix existingMatrix= mapQMatrix.put(mapQ, matrix);
//					if (null != existingMatrix)
//						matrix = existingMatrix;
//				}
			}
			
			int cm = r.nextInt(12);
			int sm = r.nextInt(102);
			int length = r.nextInt(50);
			
			matrix.addToMatrix(cm, MatrixType.CM);
			matrix.addToMatrix(sm, MatrixType.SM);
			matrix.addToMatrix(length, MatrixType.LENGTH);
			
			smM.setValue(sm);
			cmM.setValue(cm);
			lenM.setValue(length);
			
			// mapqmatrix info
			SummaryByCycleUtils.incrementCount(mapQCMMatrix, cmM);
			SummaryByCycleUtils.incrementCount(mapQSMMatrix, smM);
			SummaryByCycleUtils.incrementCount(mapQLengthMatrix, lenM);
		}
		
		Assert.assertEquals(100, mapQMatrix.size());
		
		for (Integer integer : counter.keySet()) {
			Assert.assertEquals(counter.get(integer).get(), getTotalCountFromMap(mapQMatrix.get(integer).getMatrixByType(MatrixType.CM)));
			Assert.assertEquals(counter.get(integer).get(), getTotalCountFromMap(mapQMatrix.get(integer).getMatrixByType(MatrixType.SM)));
			Assert.assertEquals(counter.get(integer).get(), getTotalCountFromMap(mapQMatrix.get(integer).getMatrixByType(MatrixType.LENGTH)));
			
		}
		
		Map<MAPQMiniMatrix, AtomicLong> cmMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> smMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> lengthMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		
		for (Entry<Integer, MAPQMatrix> entry : mapQMatrix.entrySet()) {
			
			for (MatrixType type : MatrixType.values()) {
				Map<Integer, AtomicLong> map = entry.getValue().getMatrixByType(type);
				
				Map<MAPQMiniMatrix, AtomicLong> tempMap = new TreeMap<MAPQMiniMatrix, AtomicLong>();
				for (Entry<Integer, AtomicLong> mapEntry : map.entrySet()) {
					tempMap.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
				}
				
				switch (type) {
				case CM:
					cmMatrix.putAll(tempMap);
					break;
				case SM:
					smMatrix.putAll(tempMap);
					break;
				case LENGTH:
					lengthMatrix.putAll(tempMap);
					break;
				}
			}
		}
		
		Assert.assertEquals(mapQCMMatrix.size(), cmMatrix.size());
		for (Entry<MAPQMiniMatrix, AtomicLong> entry : mapQCMMatrix.entrySet()) {
			Assert.assertEquals(entry.getValue().get(), cmMatrix.get(entry.getKey()).get());
		}
		
		Assert.assertEquals(mapQSMMatrix.size(), smMatrix.size());
		for (Entry<MAPQMiniMatrix, AtomicLong> entry : mapQSMMatrix.entrySet()) {
			Assert.assertEquals(entry.getValue().get(), smMatrix.get(entry.getKey()).get());
		}
		
		Assert.assertEquals(mapQLengthMatrix.size(), lengthMatrix.size());
		for (Entry<MAPQMiniMatrix, AtomicLong> entry : mapQLengthMatrix.entrySet()) {
			Assert.assertEquals(entry.getValue().get(), lengthMatrix.get(entry.getKey()).get());
		}
		
		
	}
	
	
	
	private long getTotalCountFromMap(Map<Integer, AtomicLong> map) {
		long count = 0;
		for (Entry<Integer, AtomicLong> entry : map.entrySet())
			count +=entry.getValue().get();
		
		return count;
	}

}
