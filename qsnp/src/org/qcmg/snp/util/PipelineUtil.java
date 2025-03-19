package org.qcmg.snp.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import gnu.trove.set.hash.TLongHashSet;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.Classification;
import org.qcmg.common.model.Rule;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.AccumulatorUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongCharMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class PipelineUtil {
	
	public static final String OPEN_CLOSE_BRACKETS = "[]";
	private static final QLogger logger = QLoggerFactory.getLogger(PipelineUtil.class);
	
	public static List<List<VcfRecord>> listOfListOfAdjacentVcfs(List<VcfRecord> snps) {
		if (null == snps) {
			return null;
		}
		/*
		 * sort initial list
		 */
		snps.sort(null);
		
		List<List<VcfRecord>> loloRecs = new ArrayList<>();
		
		for (int i = 0, size = snps.size() ; i < size - 1 ; ) {
			
			VcfRecord thisPosition = snps.get(i++);
			VcfRecord nextPosition = snps.get(i);
			
			if (ChrPositionUtils.areAdjacent(thisPosition.getChrPosition(), nextPosition.getChrPosition())) {
				
				if (VcfUtils.isRecordSomatic(thisPosition) != VcfUtils.isRecordSomatic(nextPosition) ) {
					continue;
				}
				
				/*
				 * create list
				 */
				List<VcfRecord> list = new ArrayList<>(3);
				loloRecs.add(list);
				list.add(thisPosition);
				list.add(nextPosition);
				
				while (i < size-1) {
					thisPosition = nextPosition;
					nextPosition = snps.get(++i);		// end point of compound snp
					if ( ! ChrPositionUtils.areAdjacent(thisPosition.getChrPosition(), nextPosition.getChrPosition())) {
						break;
					}
					// check to see if they have the same classification
					
					if (VcfUtils.isRecordSomatic(thisPosition) != VcfUtils.isRecordSomatic(nextPosition) ) {
						break;
					}
					list.add(nextPosition);
				}
			}
		}
		return loloRecs;
	}
	
	/**
	 * Given the top 2 bases from both control and test, along with the reference, return the alt string, and the control and test genotype strings.
	 * control and test could be null, in which case the missing data '.' char will be placed in their respective list positions.
	 * 
	 * @param control
	 * @param test
	 * @param reference
	 * @return
	 */
	public static List<String> getAltStringAndGenotypes(List<String> control, List<String> test, String reference) {
		if (StringUtils.isNullOrEmpty(reference)) {
			throw new IllegalArgumentException("Null or empty reference passed to PipelineUtil.getAltStringAndGenotypes");
		}
		List<String>allels = new ArrayList<>(5);
		allels.add(reference);
		if (null != control) {
			allels.addAll(control.stream().distinct().filter(s -> isAltFreeOfRef(s, reference)).toList());
		}
		if (null != test) {
			allels.addAll(test.stream().distinct().filter(s -> isAltFreeOfRef(s, reference)).toList());
		}
		
		allels = allels.stream().distinct().collect(Collectors.toList());
		
		short[] controlGT = getGenotypeArray(control, allels);
		short[] testGT = getGenotypeArray(test, allels);
		
		/*
		 * remove ref from alleles list
		 */
		allels.remove(reference);
		
		/*
		 * sort the arrays
		 */
		Arrays.sort(controlGT);
		Arrays.sort(testGT);
		String cgt = Arrays.binarySearch(controlGT, (short) -1) > -1 ? Constants.MISSING_GT : controlGT[0] + Constants.SLASH_STRING + controlGT[1];
		String tgt =  Arrays.binarySearch(testGT, (short) -1) > -1 ? Constants.MISSING_GT : testGT[0] + Constants.SLASH_STRING + testGT[1];
		return Arrays.asList(allels.isEmpty() ? Constants.MISSING_DATA_STRING : String.join(Constants.COMMA_STRING, allels), cgt, tgt);
	}
	
	/**
	 * Returns a short array of the genotype eg [0,1] will translate to GT "0/1"
	 *  
	 * @param alleles
	 * @param map
	 * @param currentCounter
	 * @return
	 */
	public static short[] getGenotypeArray(List<String> sampleAlleles, List<String> allAlleles) {
		short[] gt = new short[]{-1,-1};
		
		if (null != sampleAlleles) {
			int i = 0;
			for (String a : sampleAlleles) {
				if (i < 2) {
					/*
					 * only do this twice at most
					 */
					int pos = allAlleles.indexOf(a);
					if (pos == -1) {
						//hmmmmm.....
					}
					gt[i++] = (short)pos;
				}
				if (sampleAlleles.size() == 1) {
					gt[i] = gt[0];
				}
			}
		}
		return gt;
	}
	
	/**
	 * Checks if the provided alternate string ('alt') is completely free of any reference bases ('ref').
	 * The method ensures that all characters in 'alt' differ from their corresponding characters in 'ref'.
	 * If either 'alt' or 'ref' is null, empty, or contains '.', or if their lengths do not match,
	 * the method returns true, assuming 'alt' is free of any reference comparison.
	 *
	 * @param alt The alternate string to be checked. Must not be null, empty, or contain missing data (".").
	 * @param ref The reference string to compare against. Must not be null, empty, or contain missing data (".").
	 * @return true if 'alt' is free of any characters from 'ref' or the inputs are invalid; otherwise, false.
	 */
	public static boolean isAltFreeOfRef(String alt, String ref) {
		
		/*
		 * alt and ref must be not null, empty, missing data, and must be the same length  
		 */
		if (alt == null || ref == null || alt.isEmpty() || ref.isEmpty() || ".".equals(alt) || ".".equals(ref) || alt.length() != ref.length()) {
			return true;
		}

		int len = alt.length();
		for (int i = 0 ; i < len ; i++) {
			if (alt.charAt(i) == ref.charAt(i)) {
				return false;
			}
		}

		return true; // No matches found, alt is free of ref.
	}
	
	/**
	 * Gets a list of bases from the map that make up the genotype. This will be the 2 sets of bases with the largest coverage (based on counts and then strand).
	 * 
	 * @param basesAndCounts
	 * @param minimumCoverage
	 * @return
	 */
	public static List<String> getBasesForGenotype(Map<String, short[]> basesAndCounts, int minimumCoverage, String ref) {
		if (null != basesAndCounts) {
			
			List<String> genotypeBases = basesAndCounts.entrySet().stream()
				.filter(e -> e.getValue().length == 4 && (e.getValue()[0] + e.getValue()[2]) >= minimumCoverage && (e.getKey().equals(ref) || isAltFreeOfRef(e.getKey(), ref)) && ! e.getKey().contains("_"))
				.sorted(
						Comparator.comparing((Map.Entry<String,  short[]> e) -> e.getValue()[0] + e.getValue()[2], Comparator.reverseOrder())
						.thenComparing(e -> e.getValue()[0] > 0 && e.getValue()[2] > 0, Comparator.reverseOrder()))
				.map(Map.Entry::getKey)
				.collect(Collectors.toList());
			
			if (genotypeBases.size() > 2) {
				return Arrays.asList(genotypeBases.get(0), genotypeBases.get(1));
			}
			
			return genotypeBases;
		}
		return Collections.emptyList();
	}
	
	/**
	 * Returns the Observed Alleles By Strand for this map of bases and counts.
	 *
	 * @param basesAndCounts a map where the key is a string and the value is an array of
	 *                       short integers; used to generate a formatted string if certain
	 *                       conditions are met
	 * @return an Optional containing the formatted string if the input map is not null and
	 *         contains matching entries, otherwise an empty Optional
	 */
	public static Optional<String> getOABS(Map<String, short[]> basesAndCounts) {
		if (null != basesAndCounts) {
			String oabs = basesAndCounts.entrySet().stream()
					.filter(e -> e.getValue().length == 4)
					.sorted(Map.Entry.comparingByKey())
					.map(e -> e.getKey() + e.getValue()[0] + OPEN_CLOSE_BRACKETS + e.getValue()[2] + OPEN_CLOSE_BRACKETS)
					.collect(Collectors.joining(Constants.SEMI_COLON_STRING));
			
			return Optional.ofNullable(!oabs.isEmpty() ? oabs : null);
		}
		return Optional.empty();
	}
	
	/**
	 * It is assumed that the list of accumulators are ordered and adjacent - will throw an IllegalArgumentException should this not be the case
	 * 
	 * returned map contains bases as key, and short array contains 4 elements, which are (in this order):
	 * forward strand count
	 * forward strand novel starts count
	 * reverse strand count
	 * reverse strand novel starts count
	 * 
	 * 
	 * @param accs
	 * @return
	 */
	public static Map<String, short[]> getBasesFromAccumulators(List<Accumulator> accs) {
		if (null == accs) {
			return Collections.emptyMap();
		}
		/*
		 * create map of readIds and bases, then flip to bases and counts
		 */
		TLongObjectMap<StringBuilder> moReadIdsAndBases = new TLongObjectHashMap<>();
		
		int pos = -1;
		AtomicInteger ai = new AtomicInteger();
		for (Accumulator acc : accs) {
			if (null == acc) continue;
			/*
			 * check that accumulators are adjacent
			 */
			if (pos != -1) {
				if (acc.getPosition() != pos + 1) {
					
					logger.warn("Accumulator objects are not in sequence!:");
					accs.forEach(a -> logger.warn(a.toString()));
					throw new IllegalArgumentException("List of Accumulator objects are not in sequence!");
				}
			}
			pos = acc.getPosition();
			
			
			TLongCharMap accMap = AccumulatorUtils.getReadNameHashBaseMap(acc);
			if (null == accMap) continue;
			
			if (ai.get() == 0) {
				
				accMap.forEachEntry((j,c) -> {
					moReadIdsAndBases.put(j, new StringBuilder(c + Constants.EMPTY_STRING) ); 
					return true;
					} );
			} else {
			/*
			 *	update existing StringBuilders should they exist. If they don't exist, add new StringBuilder with i*_ depicting the missing bases. 
			 */
				accMap.forEachEntry((j,c) -> {
					StringBuilder sb = moReadIdsAndBases.get(j);
					if (null == sb) {
						sb = new StringBuilder();
                        sb.append("_".repeat(Math.max(0, ai.get())));
						moReadIdsAndBases.put(j, sb );
					}
					sb.append(c);
					return true;
					});
				
				/*
				 * Now need to ensure than any existing entries are updated with an '_'
				 */
				moReadIdsAndBases.forEachEntry((j,sb) -> {
					if ( ! accMap.containsKey(j)) {
						sb.append('_');
					}
					return true;
					});
			}
			ai.incrementAndGet();
		}
		
		TMap<String, Pair<TLongList, TLongList>> basesAndReadIds = invertMap(moReadIdsAndBases);
		
		
		/*
		 * Create map of readIdAndStartPos
		 * The start positions of reads on the reverse strand are indicated by the sign of the start position (value in the map).
		 * If it is -ve, then the read is on the reverse strand, if +ve, its on the forward strand
		 */
		TLongIntMap combinedReadIdsStarPos = AccumulatorUtils.getReadIdStartPosMap(accs);
		
		
		Map<String, short[]> allelesCountsNNS = new THashMap<>();
		
		basesAndReadIds.forEachEntry((k,v) -> {
			
			short [] sa = new short[4];
			allelesCountsNNS.put(k, sa);
			
			/*
			 * first column in sa is fs count
			 */
			sa[0] = (short) v.left().size();
			/*
			 * second column is fs nns count
			 * Need to get set of start positions from combinedReadIdsStarPos for our readIds 
			 */
			sa[1] = (short)getUniqueCount(combinedReadIdsStarPos, v.left(), true);
			/*
			 * third column in sa is rs count
			 */
			sa[2] = (short) v.right().size();
			/*
			 * fourth column is rs nns count
			 * Need to get set of start positions from combinedReadIdsStarPos for our readIds 
			 */
			sa[3] = (short)getUniqueCount(combinedReadIdsStarPos, v.right(), false);
			return true;
			});
		
		return allelesCountsNNS;
	}

	private static TMap<String, Pair<TLongList, TLongList>> invertMap(TLongObjectMap<StringBuilder> moReadIdsAndBases) {
		/*
		 * now turn into a TMap<String,  Pair<TIntList, TIntList>>
		 */
		TMap<String, Pair<TLongList, TLongList>> basesAndReadIds = new THashMap<>(moReadIdsAndBases.size() * 2); 
		moReadIdsAndBases.forEachEntry((i,sb) -> {
			
			/*
			 * if the string builder contains lower case chars, then it is reverse strand
			 * so need to upper case it
			 */
			boolean reverseStrand = isStringLowerCase(sb.toString());
			
			String bases = reverseStrand ? sb.toString().toUpperCase() : sb.toString();
			
			Pair<TLongList, TLongList> readIds = basesAndReadIds.get(bases);
			if (null == readIds) {
				TLongList fsList = new TLongArrayList();
				TLongList rsList = new TLongArrayList();
				readIds = new Pair<>(fsList, rsList);
				basesAndReadIds.put(bases, readIds);
			}
			if (reverseStrand) {
				readIds.right().add(i);
			} else {
				readIds.left().add( i);
			}
			return true;
		});
		return basesAndReadIds;
	}
	
	public static int getUniqueCount(TIntIntMap map, TIntList list) {
		return getUniqueCount(map, list, true);
	}
	/**
	 * for each element in the list, get the corresponding value in the map, and return the unique count of these values
	 * @param map
	 * @param list
	 * @return
	 */
	public static int getUniqueCount(TIntIntMap map, TIntList list, boolean fs) {
		TIntSet set = new TIntHashSet();
		list.forEach(i -> {
			if ((fs && i > 0) || ( ! fs && i < 0)) {
				int v = map.get(i);
				if (v > 0) {
					set.add(v);
				}
			}
			return true;
		});
		return set.size();
	}
	
	/**
	 * Determines if the given string contains at least one lowercase character.
	 *
	 * @param s the string to be checked for lowercase characters
	 * @return true if the string contains at least one lowercase character, false otherwise
	 */
	public static boolean isStringLowerCase(String s) {
		if (null != s) {
			for (char c : s.toCharArray()) {
				if (Character.isLowerCase(c)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Computes the count of unique integers based on the given map and list.
	 * The method evaluates the elements in the list, retrieves their corresponding values
	 * from the map, and filters them based on the specified boolean flag.
	 * Only absolute values of the integers matching the condition are considered, and duplicates
	 * are ignored.
	 *
	 * @param map a mapping of long keys to integer values that is used to determine the start positions
	 * @param list a list of long values to be evaluated
	 * @param fs a flag that determines the criteria for filtering the map values; true for positive
	 *           values and false for negative values
	 * @return the count of unique absolute values matching the specified criteria
	 */
	public static int getUniqueCount(TLongIntMap map, TLongList list, boolean fs) {
		TIntSet set = new TIntHashSet();
		list.forEach(i -> {
			int startPosition = map.get(i);
			if ((fs && startPosition > 0) || ( ! fs && startPosition < 0)) {
				set.add(Math.abs(startPosition));
			}
			return true;
		});
		return set.size();
	}
	
	/**
	 * Processes a map of VcfRecord to a pair of Accumulator objects and separates them into two lists:
	 * one for control accumulators and another for test accumulators. Entries with non-null accumulators
	 * are added to their respective lists.
	 *
	 * @param vcfs A map where keys are VcfRecord objects and values are pairs of Accumulator objects.
	 *             The left value in the pair corresponds to the control accumulator, and the right value
	 *             corresponds to the test accumulator.
	 * @return A pair of lists, where the first list contains the control accumulators and the second
	 *         list contains the test accumulators.
	 */
	public static Pair<List<Accumulator>, List<Accumulator>> getAccs(Map<VcfRecord, Pair<Accumulator, Accumulator>> vcfs) {
		// Preallocate lists for control and test accumulators based on map size
		List<Accumulator> cAccs = new ArrayList<>(vcfs.size() + 1);
		List<Accumulator> tAccs = new ArrayList<>(vcfs.size() + 1);

		// Stream and process the map entries directly
		vcfs.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey()) // Optional: sorting based on keys
				.forEach(entry -> {
					Pair<Accumulator, Accumulator> p = entry.getValue();
					if (p.left() != null) {
						cAccs.add(p.left());
					}
					if (p.right() != null) {
						tAccs.add(p.right());
					}
				});

		// Return the final pair
		return new Pair<>(cAccs, tAccs);

	}
	
	public static Optional<String> getReference(Collection<VcfRecord> vcfs) {
		return Optional.of(vcfs.stream().sorted().map(VcfRecord::getRef).collect(Collectors.joining()));
	}
	
	/**
	 * Retrieves a specific count from the short array associated with the given key in the map.
	 * The short array contains coverage data, and the count is determined by the offset and an adjacent index.
	 * If the key is not found or the short array is null, the method returns 0.
	 *
	 * @param map A map where keys are strings representing specific identifiers, and values are short arrays
	 *            containing coverage data.
	 * @param key The key for which the count is to be retrieved.
	 * @param offset The offset within the short array to determine the count.
	 * @return The sum of the value at the offset index and the value at the adjacent index (offset + 2)
	 *         in the short array corresponding to the specified key. If the key is not found or the associated
	 *         array is null, 0 is returned.
	 */
	public static int getCount(Map<String,  short[]> map, String key, int offset) {
		short[] sa =map.get(key);
		return (null !=  sa) ? sa[offset] + sa[2 + offset] : 0;
	}
	
	/**
	 * Retrieves the count of novel starts for the specified key from the given map.
	 * The map contains bases as keys, and each key corresponds to a short array
	 * that holds count information for various metrics. The novel starts count is
	 * calculated by summing the forward strand novel starts count and the reverse strand
	 * novel starts count.
	 *
	 * @param map A map where keys are strings representing bases, and values are
	 *            short arrays containing count data. The short array has
	 *            4 elements in the following order:
	 *            - Forward strand count
	 *            - Forward strand novel starts count
	 *            - Reverse strand count
	 *            - Reverse strand novel starts count
	 * @param key The specific base key for which to retrieve the novel starts count.
	 * @return The sum of forward strand and reverse strand novel starts counts for
	 *         the specified base key. If the key is not found in the map, 0 is returned.
	 */
	public static int getNovelStartsCounts(Map<String,  short[]> map, String key) {
		return getCount(map, key, 1);
	}
	/**
	 * Retrieves the total counts for the specified key from the given map.
	 * The map contains bases as keys, and each key corresponds to a short array
	 * that holds count information for various metrics. The total count is
	 * calculated by summing the counts from both forward and reverse strands.
	 *
	 * @param map A map where keys are strings representing bases, and values
	 *            are short arrays containing count data. The short array has
	 *            4 elements in the following order:
	 *            - Forward strand count
	 *            - Forward strand novel starts count
	 *            - Reverse strand count
	 *            - Reverse strand novel starts count
	 * @param key The specific base key for which to retrieve the total counts.
	 * @return The total counts (sum of forward and reverse strand counts) for
	 *         the specified base key. If the key is not found in the map, 0 is returned.
	 */
	public static int getTotalCounts(Map<String,  short[]> map, String key) {
		return getCount(map, key, 0);
	}
	
	public static String[] getMR(Map<String, short[]> map, String[] aAlts, int firstG, int secondG) {
		if (null == aAlts) {
			throw new IllegalArgumentException("Null or empty alts passed to PipeLineUtil.getMR");
		}
		if (null == map) {
			throw new IllegalArgumentException("Null map passed to PipeLineUtil.getMR");
		}
		
		/*
		 * If gt is missing, return missing String
		 */
		if ( -1 == firstG && -1 == secondG) {
			return new String[]{Constants.MISSING_DATA_STRING,Constants.MISSING_DATA_STRING};
		}
		
		StringBuilder mr = new StringBuilder();
		StringBuilder nns = new StringBuilder();
		
		/*
		 *Only get data if we have a non-zero value
		 */
		if (firstG > 0) {
			int c = getTotalCounts(map, aAlts[firstG -1]);
			StringUtils.updateStringBuilder(mr, c > 0 ? c + "" : Constants.MISSING_DATA_STRING, Constants.COMMA);
			c = getNovelStartsCounts(map, aAlts[firstG - 1]);
			StringUtils.updateStringBuilder(nns, c > 0 ? c + "" : Constants.MISSING_DATA_STRING, Constants.COMMA);
		}
		if (secondG != firstG && secondG > 0) {
			int c = getTotalCounts(map, aAlts[secondG -1]);
			StringUtils.updateStringBuilder(mr, c > 0 ? c + "" : Constants.MISSING_DATA_STRING, Constants.COMMA);
			c = getNovelStartsCounts(map, aAlts[secondG - 1]);
			StringUtils.updateStringBuilder(nns, c > 0 ? c + "" : Constants.MISSING_DATA_STRING, Constants.COMMA);
		}
		
		/*
		 * if string builder are empty, put missing data in there
		 */
		if (mr.isEmpty()) {
			mr.append(Constants.MISSING_DATA_STRING);
		}
		if (nns.isEmpty()) {
			nns.append(Constants.MISSING_DATA_STRING);
		}
		
		return new String[]{mr.toString(), nns.toString()};
	}
	
	public static int getCoverage(Map<String, short[]> map) {
		/*
		 * Only care about strings without underscores (which denote missing bases)
		 */
		return map.entrySet().stream()
			.filter(e -> ! e.getKey().contains("_"))
			.mapToInt(e -> e.getValue()[0] + e.getValue()[2])
			.sum();
	}
	
	/**
	 * Create compound snp based purely on GATK vcf information.
	 * Classification (i.e. SOMATIC) must be same for all snps - that's about the only rule...
	 * oh, and the genotypes need to be the same for all control samples and for all test samples
	 * e.g. 0/0 ->0/1 for all snps in cs
	 * 
	 * @param vcfs
	 * @return
	 */
	public static Optional<VcfRecord> createCompoundSnpGATK(List<VcfRecord> vcfs) {
		return createCompoundSnpGATK(vcfs, false);
	}
	public static Optional<VcfRecord> createCompoundSnpGATK(List<VcfRecord> vcfs, boolean singleSampleMode) {
		short somCount = 0;
		String csRef = "";
		String csAlt = "";
		List<String> controlGTs = singleSampleMode ? null : new ArrayList<>(vcfs.size() + 1);
		List<String> controlDPs = singleSampleMode ? null : new ArrayList<>(vcfs.size() + 1);
		List<String> controlADs = singleSampleMode ? null : new ArrayList<>(vcfs.size() + 1);
		List<String> controlGQs = singleSampleMode ? null : new ArrayList<>(vcfs.size() + 1);
		List<String> controlQLs = singleSampleMode ? null : new ArrayList<>(vcfs.size() + 1);
		List<String> controlINFs = singleSampleMode ? null : new ArrayList<>(vcfs.size() + 1);
		List<String> testDPs = new ArrayList<>(vcfs.size() + 1);
		List<String> testGTs = new ArrayList<>(vcfs.size() + 1);
		List<String> testADs = new ArrayList<>(vcfs.size() + 1);
		List<String> testGQs = new ArrayList<>(vcfs.size() + 1);
		List<String> testQLs = new ArrayList<>(vcfs.size() + 1);
		List<String> testINFs = new ArrayList<>(vcfs.size() + 1);
		for (VcfRecord v : vcfs) {
			if (VcfUtils.isRecordSomatic(v)) {
				somCount++;
			}
			csRef += v.getRef();
			csAlt += v.getAlt();
			Map<String, String[]> ffMap = VcfUtils.getFormatFieldsAsMap(v.getFormatFields());
			String [] gtArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE);
			String [] dpArr = ffMap.get(VcfHeaderUtils.FORMAT_READ_DEPTH);
			if (null == dpArr) {
				logger.warn("null dp array for rec: " + v);
			}
			String [] adArr = ffMap.get(VcfHeaderUtils.FORMAT_ALLELIC_DEPTHS);
			String [] gqArr = ffMap.get(VcfHeaderUtils.FORMAT_GENOTYPE_QUALITY);
			String [] qlArr = ffMap.get(VcfHeaderUtils.FORMAT_QL);
			String [] infArr = ffMap.get(VcfHeaderUtils.FORMAT_INFO);
			
			if (singleSampleMode) {
				
				testGTs.add(gtArr[0]);
				testDPs.add(dpArr[0]);
				testADs.add(adArr[0]);
				testGQs.add(gqArr[0]);
				testQLs.add(qlArr[0]);
				testINFs.add(infArr[0]);
			} else {
				
				controlGTs.add(gtArr[0]);
				testGTs.add(gtArr[1]);
				controlDPs.add(dpArr[0]);
				testDPs.add(dpArr[1]);
				controlADs.add(adArr[0]);
				testADs.add(adArr[1]);
				controlGQs.add(gqArr[0]);
				testGQs.add(gqArr[1]);
				controlQLs.add(qlArr[0]);
				testQLs.add(qlArr[1]);
				controlINFs.add(infArr[0]);
			}
		}
		
		/*
		 * if we have a comma in the alt field, don't proceed
		 */
		if ( ! csAlt.contains(Constants.COMMA_STRING)) {
			if (somCount == 0 || somCount == vcfs.size()) {
			// if gts are the same, alls well
				if (singleSampleMode || (controlGTs.stream().distinct().count() == 1 && testGTs.stream().distinct().count() == 1 )) {
				
					VcfRecord firstRec = vcfs.getFirst();
					VcfRecord v = VcfUtils.createVcfRecord(firstRec.getChrPosition(), null, csRef, csAlt);
					
					/*
					 * sort collections to get lowest value first - that's what we will use
					 */
					if ( ! singleSampleMode) {
						controlDPs.sort(null);
						controlADs.sort(null);
						controlGQs.sort(null);
						controlQLs.sort(null);
					}
					testDPs.sort(null);
					testADs.sort(null);
					testGQs.sort(null);
					testQLs.sort(null);
					
					/*
					 * unique INF lists
					 */
					List<String> infoList = singleSampleMode ? testINFs : controlINFs;
					String cINF = infoList.stream().distinct().collect(Collectors.joining(Constants.SEMI_COLON_STRING));
					
					
					/*
					 * format fields - going for GT:AD:DP:FT:GQ:INF:NNS:OABS:QL
					 * of which FT,NNS, and OABS will be missing data string
					 */
					StringBuilder cSB = null;
					if ( ! singleSampleMode) {
						cSB = new StringBuilder(controlGTs.getFirst());								//GT
						StringUtils.updateStringBuilder(cSB, controlADs.getFirst(), Constants.COLON);	//AD
						StringUtils.updateStringBuilder(cSB, controlDPs.getFirst(), Constants.COLON);		//DP
						StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//FT
						StringUtils.updateStringBuilder(cSB,controlGQs.getFirst(), Constants.COLON);		// GQ field
						StringUtils.updateStringBuilder(cSB,cINF, Constants.COLON);		// INF field
						StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//NNS
						StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//OABS
						StringUtils.updateStringBuilder(cSB, controlQLs.getFirst(), Constants.COLON);								//QL
					}
					
					StringBuilder tSB = new StringBuilder(testGTs.getFirst());
					StringUtils.updateStringBuilder(tSB, testADs.getFirst(), Constants.COLON);
					StringUtils.updateStringBuilder(tSB, testDPs.getFirst(), Constants.COLON);
					StringUtils.updateStringBuilder(tSB, Constants.MISSING_DATA_STRING, Constants.COLON);
					StringUtils.updateStringBuilder(tSB, testGQs.getFirst(), Constants.COLON);	// GQ field
					StringUtils.updateStringBuilder(tSB, somCount > 0 ? "SOMATIC" : Constants.MISSING_DATA_STRING, Constants.COLON);	// INF field
					StringUtils.updateStringBuilder(tSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//NNS
					StringUtils.updateStringBuilder(tSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//OABS
					StringUtils.updateStringBuilder(tSB, testQLs.getFirst(), Constants.COLON);									//QL
					
					if ( singleSampleMode) {
						v.setFormatFields(Arrays.asList("GT:AD:DP:FT:GQ:INF:NNS:OABS:QL", tSB.toString()));
					} else {
						v.setFormatFields(Arrays.asList("GT:AD:DP:FT:GQ:INF:NNS:OABS:QL", cSB.toString(), tSB.toString()));
					}
					
					return Optional.of(v);
				}
			}
		}
		
		return Optional.empty();
	}

	public static String formatCBasesCountsNNS(Map<String, short[]> cBasesCountsNNS) {
		if (cBasesCountsNNS == null || cBasesCountsNNS.isEmpty()) {
			return "cBasesCountsNNS is empty or null";
		}

		// Prepare StringBuilder to format content
		StringBuilder sb = new StringBuilder("{");
		cBasesCountsNNS.forEach((key, counts) -> {
			sb.append(key)
					.append(": [")
					.append(Arrays.toString(counts)) // Format short[] as a string
					.append("], ");
		});

		// Remove trailing ", " if it exists and close the string
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 2); // Remove last ", "
		}
		sb.append("}");

		return sb.toString();
	}


	public static Optional<VcfRecord> createCompoundSnp(Map<VcfRecord, Pair<Accumulator, Accumulator>> vcfs, List<Rule> controlRules, List<Rule> testRules, boolean runSBias, int sBiasCov, int sBiasAlt) {
		
		Pair<List<Accumulator>, List<Accumulator>> p = getAccs(vcfs);
		Optional<String> refO = getReference(vcfs.keySet());
		String ref = refO.orElse(null);
		
		Map<String, short[]> cBasesCountsNNS =  getBasesFromAccumulators(p.left());
		Map<String, short[]> tBasesCountsNNS =  getBasesFromAccumulators(p.right());
		
		int controlCov = getCoverage(cBasesCountsNNS);
		int testCov = getCoverage(tBasesCountsNNS);
		Rule cr = RulesUtil.getRule(controlRules, controlCov);
		Rule tr = RulesUtil.getRule(testRules, testCov);

		/*
		 * check to see if we need to use a percentage, if so, calculate the minCov based on totalCov
		 */
		int cMinCov = null != cr ? cr.maxCoverage() == Integer.MAX_VALUE ? (cr.noOfVariants() * controlCov) / 100 : cr.noOfVariants() : 2;
		int tMinCov = null != tr ? tr.maxCoverage() == Integer.MAX_VALUE ? (tr.noOfVariants() * testCov) / 100 : tr.noOfVariants() : 2;
		
		List<String> cGT = getBasesForGenotype (cBasesCountsNNS, cMinCov, ref);
		List<String> tGT = getBasesForGenotype (tBasesCountsNNS, tMinCov, ref);
		
		
		/*
		 * check to see if we have a mutation
		 * if getBasesForGenotype only returns an empty list OR a list just containing the reference for BOTH control, and test, then we don't have a mutation
		 */
		if ((cGT.isEmpty() || (cGT.size() == 1 && cGT.getFirst().equals(ref)))
				&& (tGT.isEmpty() || (tGT.size() == 1 && tGT.getFirst().equals(ref)))) {
			return Optional.empty();
		}
		
		/*
		 * this list should contain 3 elements, the first is a comma seperated list of alt alleles.
		 * The second and third elements are the control and test GT fields
		 */
		List<String> altsAndGTs = getAltStringAndGenotypes(cGT, tGT, ref);
		/*
		 * If alt string is '.' - no dice
		 */
		if (altsAndGTs.isEmpty() || StringUtils.isNullOrEmptyOrMissingData(altsAndGTs.getFirst())) {
			return Optional.empty(); 
		}
		
		VcfRecord firstRec = vcfs.keySet().stream().sorted().findFirst().get();

//		ChrPosition cpToDebug = ChrPositionUtils.getChrPosition("chr1", 25002122, 25002122);
//		if (firstRec.getChrPosition().equals(cpToDebug)) {
//			logger.info("cBasesCountsNNS: " + formatCBasesCountsNNS(cBasesCountsNNS));
//			logger.info("tBasesCountsNNS: " + formatCBasesCountsNNS(tBasesCountsNNS));
//			logger.info("p.left():");
//			int x = 0;
//			for (Accumulator acc : p.left()) {
//				if ( null != acc.getFailedFilterACount()) {
//					for (int i = 0 ; i < acc.getFailedFilterACount().size() ; i++) {
//						logger.info("failedFilterACount: " + acc.getFailedFilterACount().get(i));
//					}
//				}
//				if ( null != acc.getFailedFilterCCount()) {
//					for (int i = 0 ; i < acc.getFailedFilterCCount().size() ; i++) {
//						logger.info("failedFilterCCount: " + acc.getFailedFilterCCount().get(i));
//					}
//				}
//				if ( null != acc.getFailedFilterGCount()) {
//					for (int i = 0 ; i < acc.getFailedFilterGCount().size() ; i++) {
//						logger.info("failedFilterGCount: " + acc.getFailedFilterGCount().get(i));
//					}
//				}
//				if ( null != acc.getFailedFilterTCount()) {
//					for (int i = 0 ; i < acc.getFailedFilterTCount().size() ; i++) {
//						logger.info("failedFilterTCount: " + acc.getFailedFilterTCount().get(i));
//					}
//				}
//				logger.info("that was acc: " + x++);
//			}
//			logger.info("p.right():");
//			x = 0;
//			for (Accumulator acc : p.right()) {
//				if ( null != acc.getFailedFilterACount()) {
//					for (int i = 0 ; i < acc.getFailedFilterACount().size() ; i++) {
//						logger.info("failedFilterACount: " + acc.getFailedFilterACount().get(i));
//					}
//				}
//				if ( null != acc.getFailedFilterCCount()) {
//					for (int i = 0 ; i < acc.getFailedFilterCCount().size() ; i++) {
//						logger.info("failedFilterCCount: " + acc.getFailedFilterCCount().get(i));
//					}
//				}
//				if ( null != acc.getFailedFilterGCount()) {
//					for (int i = 0 ; i < acc.getFailedFilterGCount().size() ; i++) {
//						logger.info("failedFilterGCount: " + acc.getFailedFilterGCount().get(i));
//					}
//				}
//				if ( null != acc.getFailedFilterTCount()) {
//					for (int i = 0 ; i < acc.getFailedFilterTCount().size() ; i++) {
//						logger.info("failedFilterTCount: " + acc.getFailedFilterTCount().get(i));
//					}
//				}
//				logger.info("that was acc: " + x++);
//			}
//		}

		
		VcfRecord v = VcfUtils.createVcfRecord(firstRec.getChrPosition(), null, ref, altsAndGTs.getFirst());
		
		/*
		 * get some data that will be used frequently in the filter finding
		 */
		String [] aAlts = altsAndGTs.get(0).split(Constants.COMMA_STRING);
		int controlFirstG = -1, controlSecondG = -1, testFirstG = -1, testSecondG = -1;
		if ( ! StringUtils.isNullOrEmpty(altsAndGTs.get(1)) && ! Constants.MISSING_GT.equals(altsAndGTs.get(1))) {
			
			 controlFirstG = Integer.parseInt(altsAndGTs.get(1).charAt(0) + "");
			 controlSecondG = Integer.parseInt(altsAndGTs.get(1).charAt(2) + "");
		}
		if ( ! StringUtils.isNullOrEmptyOrMissingData(altsAndGTs.get(2)) && ! Constants.MISSING_GT.equals(altsAndGTs.get(2))) {
			testFirstG = Integer.parseInt(altsAndGTs.get(2).charAt(0) + "");
			testSecondG = Integer.parseInt(altsAndGTs.get(2).charAt(2) + "");
		}
		
		/*
		 * get classification
		 */
		Classification c = GenotypeUtil.getClassification(String.join(Constants.COMMA_STRING, cBasesCountsNNS.keySet()), altsAndGTs.get(1), altsAndGTs.get(2), altsAndGTs.get(0));
		
		/*
		 * format fields - going for GT:AD:DP:FF:FT:INF:NNS:OABS
		 */
		Optional<String> oOabs = getOABS(cBasesCountsNNS);
		String oabs = oOabs.orElse(Constants.MISSING_DATA_STRING);

		String failedFilter = getFailedFilterCS(p.left());

		StringBuilder cSB = new StringBuilder(altsAndGTs.get(1));
		StringUtils.updateStringBuilder(cSB, VcfUtils.getAD(ref, altsAndGTs.get(0), oabs), Constants.COLON);//GT and
		StringUtils.updateStringBuilder(cSB, controlCov > 0 ? controlCov + "" : "0", Constants.COLON);		//DP
		StringUtils.updateStringBuilder(cSB, failedFilter, Constants.COLON);								// FF (failed filter)
		/*
		 * filters are applied in qannotate now
		 */
		StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);				// FT field
		String [] mrNNS =  getMR(cBasesCountsNNS, aAlts, controlFirstG, controlSecondG);
		StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);				// INF field
		StringUtils.updateStringBuilder(cSB, mrNNS[1], Constants.COLON);									// NNS field
		StringUtils.updateStringBuilder(cSB, oabs, Constants.COLON);										// OABS field
		
		oOabs = getOABS(tBasesCountsNNS);
		oabs = oOabs.orElse(Constants.MISSING_DATA_STRING);
		
		StringBuilder tSB = new StringBuilder(altsAndGTs.get(2));
		StringUtils.updateStringBuilder(tSB, VcfUtils.getAD(ref, altsAndGTs.get(0), oabs), Constants.COLON);//GT and
		StringUtils.updateStringBuilder(tSB, testCov > 0 ? testCov + "" :  "0", Constants.COLON);			//DP
		failedFilter = getFailedFilterCS(p.right());
		StringUtils.updateStringBuilder(tSB, failedFilter, Constants.COLON);								// FF (failed filter)
		/*
		 * filters are applied in qannotate now
		 */
		StringUtils.updateStringBuilder(tSB, Constants.MISSING_DATA_STRING, Constants.COLON);				// FT field
		StringUtils.updateStringBuilder(tSB, (c == Classification.SOMATIC ? VcfHeaderUtils.INFO_SOMATIC : Constants.MISSING_DATA_STRING), Constants.COLON);	// INF field
		mrNNS =  getMR(tBasesCountsNNS,aAlts, testFirstG, testSecondG);
		StringUtils.updateStringBuilder(tSB, mrNNS[1], Constants.COLON);									// NNS field
		StringUtils.updateStringBuilder(tSB, oabs, Constants.COLON);										// OABS field
		
		v.setFormatFields(Arrays.asList("GT:AD:DP:FF:FT:INF:NNS:OABS", cSB.toString(), tSB.toString()));
		
		return Optional.of(v);
	}

	public static String getFailedFilterCS(List<Accumulator> accumulators) {
		if (null == accumulators || accumulators.isEmpty()) {
			return Constants.MISSING_DATA_STRING;
		}
		Map<Long, StringBuilder> failedFilters = new THashMap<>();
		int x = 0;
		int runningTally = 0;
		for (Accumulator acc : accumulators) {
			if (null != acc) {
				String padding = "_".repeat(x);
				TLongList count = acc.getFailedFilterACount();
				if (null != count && ! count.isEmpty()) {
					TLongHashSet failedFilterSet = new TLongHashSet(count);
					long[] array = failedFilterSet.toArray();
					for (long l : array) {
						failedFilters.computeIfAbsent(l, k -> new StringBuilder(padding)).append("A");
					}
				}
				count = acc.getFailedFilterCCount();
				if (null != count && ! count.isEmpty()) {
					TLongHashSet failedFilterSet = new TLongHashSet(count);
					long[] array = failedFilterSet.toArray();
					for (long l : array) {
						failedFilters.computeIfAbsent(l, k -> new StringBuilder(padding)).append("C");
					}
				}
				count = acc.getFailedFilterGCount();
				if (null != count && ! count.isEmpty()) {
					TLongHashSet failedFilterSet = new TLongHashSet(count);
					long[] array = failedFilterSet.toArray();
					for (long l : array) {
						failedFilters.computeIfAbsent(l, k -> new StringBuilder(padding)).append("G");
					}
				}
				count = acc.getFailedFilterTCount();
				if (null != count && ! count.isEmpty()) {
					TLongHashSet failedFilterSet = new TLongHashSet(count);
					long[] array = failedFilterSet.toArray();
					for (long l : array) {
						failedFilters.computeIfAbsent(l, k -> new StringBuilder(padding)).append("T");
					}
				}
			}
			if (x >= 1) {
				/*
				add padding to any entry in the map that has length less than x
				 */
				for (Map.Entry<Long, StringBuilder> e : failedFilters.entrySet()) {
					if (e.getValue().length() < (x + 1)) {
						e.getValue().append("_".repeat((x + 1) - e.getValue().length()));
					}
				}
			}
			x++;
//			logger.info("runningTally: " + runningTally + ", failedFilters.size(): " + failedFilters.size());

			/*
			purge any entries in the map that have a value greater than length (x + 1)
			 */
			Iterator<Map.Entry<Long, StringBuilder>> iter = failedFilters.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Long, StringBuilder> entry = iter.next();
				if (entry.getValue().length() >= (x + 1)) {
					iter.remove();
				}
			}

		}
		for (Map.Entry<Long, StringBuilder> e : failedFilters.entrySet()) {
			if (e.getValue().length() < x) {
				e.getValue().append("_".repeat(x - e.getValue().length()));
			}
		}

		/*
		 * now we have a map of readIds and failed filters, we need to turn this into a string
		 */
		Map<String, AtomicInteger> failedFilterCounts = new HashMap<>();
		failedFilters.forEach((k,v) -> failedFilterCounts.computeIfAbsent(v.toString(), k1 -> new AtomicInteger()).incrementAndGet());

		StringBuilder outputSB = new StringBuilder();
		failedFilterCounts.entrySet().stream() .sorted(Map.Entry.comparingByKey())
				.forEach(e -> StringUtils.updateStringBuilder(outputSB, e.getKey() + e.getValue().get(), ';'));

		return outputSB.isEmpty() ? Constants.MISSING_DATA_STRING : outputSB.toString();
	}

}
