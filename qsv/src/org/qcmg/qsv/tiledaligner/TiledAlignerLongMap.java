package org.qcmg.qsv.tiledaligner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;

import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.NumberUtils;
import org.qcmg.common.util.TabTokenizer;
import org.qcmg.string.StringFileReader;
import org.qcmg.tab.TabbedFileReader;
import org.qcmg.tab.TabbedRecord;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;

public class TiledAlignerLongMap {
	
	
//	private static TLongObjectMap<int[]> map = new TLongObjectHashMap<>(64 * 1024 * 1024, 1f);
	private static TIntObjectMap<int[]> map = new TIntObjectHashMap<>(68 * 1024 * 1024, 1f);
	
	public static void getTiledDataInMap(String tiledAlignerFile, int bufferSize) throws IOException {
		getTiledDataInMap(tiledAlignerFile, bufferSize, 1);
	}
	
	public static void getTiledDataInMap(String tiledAlignerFile, int bufferSize, int threadPoolSize) throws IOException {
		
//		try (FileInputStream is = new FileInputStream(tiledAlignerFile) ;
//				GZIPInputStream gzin = new GZIPInputStream(is);
//			final ReadableByteChannel channel = Channels.newChannel(gzin);) {
////			final FileChannel channel = is.getChannel();) {
//			ByteBuffer buffer = ByteBuffer.allocate(65536);
//			int newLineCount = 0;
//			long start = System.currentTimeMillis();
//			  while (channel.read(buffer) != -1) {
////				  System.out.println("read in buffer, buffer.remaining(): " + buffer.remaining());
//				  buffer.flip();
////				  System.out.println("buffer flipped, buffer.remaining(): " + buffer.remaining());
//				  while (buffer.hasRemaining()) {
//					  byte b = buffer.get();
//					  if (b == '\n') {
//						  newLineCount++;
//					  }
////					    System.out.print((char) b);
//					}
////			      System.out.println("about to clear buffer, buffer.remaining(): " + buffer.remaining());
//			      buffer.clear();
//			      channel.
//			    }
//			  System.out.println("number of new lines: " + newLineCount);
//			  System.out.println("time taken: " + (System.currentTimeMillis() - start));
////			System.out.println("Channel size: " + channel.size());
////			long size = Math.min(channel.size(), Integer.MAX_VALUE);
////			MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
////			System.out.println("got buffer! " + buffer.capacity());
////			
////			while (buffer.hasRemaining()) {
////			    System.out.print((char) buffer.get());
////			}
//			// when finished
//			channel.close();
//		}
//		
		
		AbstractQueue<String> queue = new LinkedBlockingQueue<>();
		AtomicBoolean hasReaderFinished = new AtomicBoolean(false);
//		int threadPoolSize = 1;
		ExecutorService e = Executors.newFixedThreadPool(threadPoolSize);
//		e.execute(new Reader(queue, tiledAlignerFile, bufferSize, hasReaderFinished));
		for (int i = 0 ; i < threadPoolSize ; i++) {
			e.execute(new Worker(queue, hasReaderFinished));
		}
		
		try (StringFileReader reader = new StringFileReader(new File(tiledAlignerFile), bufferSize * 1024)) {
			
			int i = 0;
			long start = System.currentTimeMillis();
			for (String rec : reader) {
				queue.add(rec);
				if (++i % 1000000 == 0) {
					System.out.println("hit " + (i / 1000000) + "M records free mem: " + Runtime.getRuntime().freeMemory() + ", map size: " + map.size() + " queue size: " + queue.size());
				}
			}
			System.out.println("Time taken with buffer size: " + bufferSize + "kb is: " + (System.currentTimeMillis() - start));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			System.out.println("finished reading from file - setting hasReaderFinished to true");
			hasReaderFinished.set(true);
		}
	
		e.shutdown();
		try {
			e.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
//		System.exit(1);
		
//		
//		try (StringFileReader reader = new StringFileReader(new File(tiledAlignerFile))) {
//			
//			int i = 0;
//			int invalidCount = 0;
//			for (String rec : reader) {
//				if (++i % 1000000 == 0) {
//					System.out.println("hit " + (i / 1000000) + "M records free mem: " + Runtime.getRuntime().freeMemory() + ", map size: " + map.size());
//				}
//				int tabindex = rec.indexOf(Constants.TAB);
//				String tile = rec.substring(0, tabindex);
//				int tileLong = TiledAlignerUtil.convertTileToInt(tile);
//				if (tileLong == -1) {
////				if (tile.indexOf('N') > -1 || tile.indexOf('M') > -1) {
//					invalidCount++;
//				} else {
//	//				long tileLong = TiledAlignerUtil.convertTileToLong(tile);
////					if (tileLong == 63887556) {
////						System.out.println("tile: " + tile);
////					}
//					int [] existingArray = map.put(tileLong, convertStringToIntArray(rec.substring(tabindex + 1)));
//	//				String s = map.put(tileLong, data.substring(tabindex + 1));
//					if (null != existingArray) {
//						System.out.println("already have an entry for tile: " + tile);
//					}
//				}
//				
//			}
//			
//			System.out.println("map size: " + map.size() + ", number of tiles with invalid chars: " + invalidCount);
//				
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
//	public static void getTiledDataInMap(String tiledAlignerFile) {
//		try (TabbedFileReader reader = new TabbedFileReader(new File(tiledAlignerFile))) {
//			
//			int i = 0;
//			int invalidCount = 0;
//			for (TabbedRecord rec : reader) {
//				if (++i % 1000000 == 0) {
//					System.out.println("hit " + (i / 1000000) + "M records free mem: " + Runtime.getRuntime().freeMemory() + ", map size: " + map.size());
//				}
//				String data = rec.getData();
//				int tabindex = data.indexOf(Constants.TAB);
//				String tile = data.substring(0, tabindex);
//				int tileLong = TiledAlignerUtil.convertTileToInt(tile);
//				if (tileLong == -1) {
////				if (tile.indexOf('N') > -1 || tile.indexOf('M') > -1) {
//					invalidCount++;
//				} else {
//					//				long tileLong = TiledAlignerUtil.convertTileToLong(tile);
////					if (tileLong == 63887556) {
////						System.out.println("tile: " + tile);
////					}
//					int [] existingArray = map.put(tileLong, convertStringToIntArray(data.substring(tabindex + 1)));
//					//				String s = map.put(tileLong, data.substring(tabindex + 1));
//					if (null != existingArray) {
//						System.out.println("already have an entry for tile: " + tile);
//					}
//				}
//				
//			}
//			
//			System.out.println("map size: " + map.size() + ", number of tiles with invalid chars: " + invalidCount);
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
	
	static class Worker implements Runnable {
		private AbstractQueue<String> queue;
		AtomicBoolean hasReaderFinished;
		public Worker(AbstractQueue<String> queue, AtomicBoolean hasReaderFinished) {
			this.queue = queue;
			this.hasReaderFinished = hasReaderFinished;
		}
		@Override
		public void run() {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Worker started...");
			int count = 0;
			String s = null;
			int invalidCount = 0;
			while ( ! ((s = queue.poll()) == null && hasReaderFinished.get())) {
//				s = queue.poll();
				if (null != s) {
//					if (count++ % 1000000 == 0) {
//						System.out.println("hit " + count + " records from Worker thread");
//					}
					int tabindex = s.indexOf(Constants.TAB);
					String tile = s.substring(0, tabindex);
					int tileLong = NumberUtils.convertTileToInt(tile);
					if (tileLong == -1) {
						invalidCount++;
					} else {
		//				long tileLong = TiledAlignerUtil.convertTileToLong(tile);
	//					if (tileLong == 63887556) {
	//						System.out.println("tile: " + tile);
	//					}
						int [] existingArray = map.put(tileLong, convertStringToIntArray(s.substring(tabindex + 1)));
		//				String s = map.put(tileLong, data.substring(tabindex + 1));
						if (null != existingArray) {
							System.out.println("already have an entry for tile: " + tile);
						}
					}
				}
			}
			System.out.println("Worker has finished! invalidCount: " + invalidCount + ", map size: " + map.size());
		}
	}
	
//	static class Reader implements Runnable {
//		private AbstractQueue<String> queue;
//		private String tiledAlignerFile;
//		private int bufferSize;
//		private AtomicBoolean hasReaderFinished;
//		public Reader(AbstractQueue<String> queue, String tiledAlignerFile, int bufferSize, AtomicBoolean hasReaderFinished) {
//			this.queue = queue;
//			this.tiledAlignerFile = tiledAlignerFile;
//			this.bufferSize = bufferSize;
//			this.hasReaderFinished = hasReaderFinished;
//		}
//		@Override
//		public void run() {
//			try (StringFileReader reader = new StringFileReader(new File(tiledAlignerFile), bufferSize * 1024)) {
//				
//				int i = 0;
//				long start = System.currentTimeMillis();
//				for (String rec : reader) {
//					queue.add(rec);
//					if (++i % 1000000 == 0) {
//						System.out.println("hit " + (i / 1000000) + "M records free mem: " + Runtime.getRuntime().freeMemory() + ", map size: " + map.size());
//					}
//				}
//				System.out.println("Time taken with buffer size: " + bufferSize + "kb is: " + (System.currentTimeMillis() - start));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} finally {
//				hasReaderFinished.set(true);
//			}
//		}
//	}
	
	public static TIntObjectMap<int[]> getCache(String tiledAlignerFile, int bufferSize) throws IOException {
		return getCache(tiledAlignerFile, bufferSize, 1);
	}
	public static TIntObjectMap<int[]> getCache(String tiledAlignerFile, int bufferSize, int threadCount) throws IOException {
		if (map.isEmpty()) {
			getTiledDataInMap(tiledAlignerFile, bufferSize, threadCount);
		}
		return map;
	}
	
	/**
	 * Converts a String consisting of a list of longs into an int array.
	 * If the String starts with a 'C' this corresponds to 'Count' and is followed by the
	 *  number of times in the genome this particular tile was found
	 *   In this instance, we will return an empty array
	 * @param s
	 * @return
	 */
	public static int[] convertStringToIntArray(String s) {
		if ( ! StringUtils.isNullOrEmpty(s)) {
			
			if (s.charAt(0) != 'C') {
				int commaIndex = s.indexOf(Constants.COMMA);
				int oldCommaIndex = 0;
				int sizeOfArray = org.apache.commons.lang3.StringUtils.countMatches(s, Constants.COMMA);
				int [] positionsArray = new int[sizeOfArray + 1];
//				TIntList list = new TIntArrayList(); 
				int i = 0;
				while (commaIndex > -1) {
					positionsArray[i++] = ((int)Long.parseLong(s.substring(oldCommaIndex, commaIndex)));
					oldCommaIndex = commaIndex + 1;
					commaIndex = s.indexOf(Constants.COMMA, oldCommaIndex);
				}
				/*
				 * add last entry
				 */
				positionsArray[i] = ((int)Long.parseLong(s.substring(oldCommaIndex)));
				return positionsArray;
			}
		}
		return new int[] {};
	}
//	public static int[] convertStringToIntArray(String s) {
//		if ( ! StringUtils.isNullOrEmpty(s)) {
//			
//			if (s.charAt(0) != 'C') {
//				int commaIndex = s.indexOf(Constants.COMMA);
//				int oldCommaIndex = 0;
//				TIntList list = new TIntArrayList(); 
//				while (commaIndex > -1) {
//					list.add((int)Long.parseLong(s.substring(oldCommaIndex, commaIndex)));
//					oldCommaIndex = commaIndex + 1;
//					commaIndex = s.indexOf(Constants.COMMA, oldCommaIndex);
//				}
//				/*
//				 * add last entry
//				 */
//				list.add((int)Long.parseLong(s.substring(oldCommaIndex)));
//				return list.toArray();
//			}
//		}
//		return new int[] {};
//	}
//	public static int[] convertStringToIntArray(String s) {
//		if ( ! StringUtils.isNullOrEmpty(s)) {
//			
//			/*
//			 * If the string starts with a 'C', this corresponds to 'Count' and is followed by the
//			 *  number of times in the genome this particular tile was found
//			 * In this instance, we will return a list with a single element containing the count, 
//			 * and the MSB set to 1, which will allow us to identify this entry as a count rather than a position
//			 */
//			if (s.charAt(0) != 'C') {
//				String [] array = TabTokenizer.tokenize(s, Constants.COMMA);
//				int len = array.length;
//				int [] positions = new int [len];
//				for (int i = 0 ; i < len ; i++) {
//					positions[i] = (int)(Long.parseLong(array[i]));
//				}
//				return positions;
//			}
//		}
//		return new int[] {};
//	}
	
	
	
	public static void main(String[] args) throws IOException {
		getTiledDataInMap(args[0], Integer.parseInt(args[1]));
	}

}
