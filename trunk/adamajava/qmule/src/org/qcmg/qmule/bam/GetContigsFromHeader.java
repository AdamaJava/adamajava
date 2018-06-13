package org.qcmg.qmule.bam;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

public class GetContigsFromHeader {
	
	private static QLogger logger;
	
	private int setup(String [] args) throws IOException {
		/*
		 * first arg should be the header,
		 * second arg (if present) should be how many times the genome should be diviied up
		 */
		
		SamReaderFactory factory = SamReaderFactory.make();
		SamReader reader = factory.open(new File(args[0]));
		SAMFileHeader header = reader.getFileHeader();
		
		SAMSequenceDictionary dict = header.getSequenceDictionary();
		Map<String, Integer> map = dict.getSequences().stream().collect(Collectors.groupingBy(SAMSequenceRecord::getSequenceName, Collectors.summingInt(SAMSequenceRecord::getSequenceLength)));
		
		
		
		if (args.length > 1 && null != args[1]) {
			int numberOfContigs = map.keySet().size();
			long length = map.values().stream().mapToLong(Integer::longValue).sum();
			int numberOfEntries = Integer.parseInt(args[1]) - 1;
			
			long noOFBasesPerEntry = length / numberOfEntries;
			
			System.out.println("genome length: " + length + ", numberOfEntries: " + numberOfEntries + ", noOFBasesPerEntry: " + noOFBasesPerEntry + ", numberOfContigs: " + numberOfContigs);
			
			
			Map<String,Integer> results = new HashMap<>();
			Set<String> contigs = new HashSet<>();
			
			List<String> sortedContigs =  map.entrySet().stream().sorted((e1, e2) -> e2.getValue() - e1.getValue()).map(e -> e.getKey()).collect(Collectors.toList());
			
			
			for (String contig : sortedContigs) {
				System.out.println("looking at contig: " + contig);
				Integer contigLength = map.get(contig);
				if ( ! contigs.contains(contig)) {
					if (contigLength >= noOFBasesPerEntry) {
						results.put(contig, contigLength);
						contigs.add(contig);
					} else {
						AtomicLong basesToMakeUp = new AtomicLong(noOFBasesPerEntry - contigLength);
//						long basesToMakeUp = noOFBasesPerEntry - e.getValue();
						StringBuilder key = new StringBuilder();
						key.append(contig);
						contigs.add(contig);
						while (basesToMakeUp.longValue() > 1000000) {
							Optional<Entry<String, Integer>> e1 = map.entrySet().stream().filter(en -> ! contigs.contains(en.getKey())).filter(en -> en.getValue() < basesToMakeUp.longValue()).max((en1, en2) -> en2.getValue() - en1.getValue());
							if (e1.isPresent()) {
								key.append(" -L ");
								key.append(e1.get().getKey());
								basesToMakeUp.addAndGet( - e1.get().getValue());
								contigs.add(e1.get().getKey());
							} else {
								break;
							}
						}
						results.put(key.toString(), (int)noOFBasesPerEntry - basesToMakeUp.intValue());
					}
				}
			}
			
			results.forEach((k,v) -> System.out.println("contigs: " + k + ", size: " + v));
			System.out.println("contigs.size(): " + contigs.size());
			
			/*
			 * write file
			 */
			if (args.length > 2 && null != args[2]) {
				try (Writer writer = new FileWriter(args[2]);) {
					
					/*
					 * sort according to number of bases 
					 */
					results.entrySet().stream().sorted((e1, e2) -> e2.getValue() - e1.getValue()).forEach(e -> {
						try {
							writer.write(e.getKey() + "\n");
						} catch (IOException e3) {
							// TODO Auto-generated catch block
							e3.printStackTrace();
						}
					});
				}
			}
		}
		
		return 0;
	}
	
	public static void main(String[] args) throws Exception {
		GetContigsFromHeader sp = new GetContigsFromHeader();
		int exitStatus = sp.setup(args);
		if (null != logger) {
			logger.logFinalExecutionStats(exitStatus);
		}
		
		System.exit(exitStatus);
	}

}

