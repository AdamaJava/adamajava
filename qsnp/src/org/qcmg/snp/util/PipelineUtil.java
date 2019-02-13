package org.qcmg.snp.util;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntCharMap;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.Accumulator;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.Classification;
import org.qcmg.common.model.Rule;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.AccumulatorUtils;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;
import org.qcmg.common.vcf.header.VcfHeaderUtils;

public class PipelineUtil {
	
	public static final String OPEN_CLOSE_BRACKETS = "[]";
	public static final String ZERO_ZERO_GT = "0/0";
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
			throw new IllegalArgumentException("Null or empty reference passed to PipelIneUtil.getAltStringAndGenotypes");
		}
		List<String>allels = new ArrayList<>(5);
		allels.add(reference);
		if (null != control) {
			allels.addAll(control.stream().distinct().filter(s -> isAltFreeOfRef(s, reference)).collect(Collectors.toList()));
		}
		if (null != test) {
			allels.addAll(test.stream().distinct().filter(s -> isAltFreeOfRef(s, reference)).collect(Collectors.toList()));
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
		String cgt = Arrays.binarySearch(controlGT, (short)-1) > -1 ? Constants.MISSING_GT : controlGT[0] + Constants.SLASH_STRING + controlGT[1];
		String tgt =  Arrays.binarySearch(testGT, (short)-1) > -1 ? Constants.MISSING_GT : testGT[0] + Constants.SLASH_STRING + testGT[1];
		return Arrays.asList(allels.isEmpty() ? Constants.MISSING_DATA_STRING : allels.stream().collect(Collectors.joining(Constants.COMMA_STRING)), cgt,tgt);
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
	 * Checks each character in the alt string against the corresponding character in the alt string. If any are the same, returns true. False otherwise
	 * @param alt
	 * @param ref
	 * @return
	 */
	public static boolean isAltFreeOfRef(String alt, String ref) {
		
		/*
		 * alt and ref must be not null, empty, missing data, and must be the same length  
		 */
		if ( ! StringUtils.isNullOrEmptyOrMissingData(alt) &&  ! StringUtils.isNullOrEmptyOrMissingData(ref)) {
			int len = alt.length();
			if (len == ref.length()) {
				for (int i = 0 ; i < len ; i++) {
					if (alt.charAt(i) == ref.charAt(i)) {
						return false;
					}
				}
			}
		}
		
		return true;
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
				.filter(e -> ! e.getKey().contains("_"))
				.filter(e -> e.getValue().length == 4)
				.filter(e -> (e.getValue()[0] + e.getValue()[2]) >= minimumCoverage )
				.filter(e -> e.getKey().equals(ref) || isAltFreeOfRef(e.getKey(), ref))
				.sorted(
						Comparator.comparing((Map.Entry<String,  short[]> e) -> e.getValue()[0] + e.getValue()[2], Comparator.reverseOrder())
						.thenComparing(e -> e.getValue()[0] > 0 && e.getValue()[2] > 0, Comparator.reverseOrder()))
				.map(e -> e.getKey())
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
	 * @param basesAndCounts
	 * @return
	 */
	public static Optional<String> getOABS(Map<String, short[]> basesAndCounts) {
		if (null != basesAndCounts) {
			String oabs = basesAndCounts.entrySet().stream()
					.filter(e -> e.getValue().length == 4)
					.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
					.map(e -> e.getKey() + e.getValue()[0] + OPEN_CLOSE_BRACKETS + e.getValue()[2] + OPEN_CLOSE_BRACKETS)
					.collect(Collectors.joining(Constants.SEMI_COLON_STRING));
			
			return Optional.ofNullable(oabs.length() > 0 ? oabs : null);
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
		TIntObjectMap<StringBuilder> moReadIdsAndBases = new TIntObjectHashMap<>();
		
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
			
			
			TIntCharMap accMap = acc.getReadIdBaseMap();
			/*
			 * convert entries with lower case chars (which represent negative strand reasds) into upper case ones with -ve readids
			 */
			accMap.forEachEntry((i,c) -> {
				if (Character.isLowerCase(c)) {
					accMap.put(-i, Character.toUpperCase(c));
					accMap.remove(i);
				}
				return true;
				});
			
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
						for (int k = 0 ; k < ai.get() ; k++) {
							sb.append('_');
						}
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
		
		/*
		 * now turn into a TMap<String,  Pair<TIntList, TIntList>>
		 */
		TMap<String, Pair<TIntList, TIntList>> basesAndReadIds = new THashMap<>(moReadIdsAndBases.size() * 2); 
		moReadIdsAndBases.forEachEntry((i,sb) -> {
			Pair<TIntList, TIntList> readIds = basesAndReadIds.get(sb.toString());
			if (null == readIds) {
				TIntList fsList = new TIntArrayList();
				TIntList rsList = new TIntArrayList();
				readIds = new Pair<>(fsList, rsList);
				basesAndReadIds.put(sb.toString(), readIds);
			}
			if (i < 0) {
				readIds.getRight().add(i);
			} else {
				readIds.getLeft().add( i);
			}
			return true;
		});
		
		
		/*
		 * Create map of readIdAndStartPos
		 */
		TIntIntMap combinedReadIdsStarPos = AccumulatorUtils.getReadIdStartPosMap(accs);
		
		
		Map<String, short[]> allelesCountsNNS = new THashMap<>();
		
		basesAndReadIds.forEachEntry((k,v) -> {
			
			short [] sa = new short[4];
			allelesCountsNNS.put(k, sa);
			
			/*
			 * first column in sa is fs count
			 */
			sa[0] = (short) v.getLeft().size();
			/*
			 * second column is fs nns count
			 * Need to get set of start positions from combinedReadIdsStarPos for our readIds 
			 */
			sa[1] = (short)getUniqueCount(combinedReadIdsStarPos, v.getLeft());
			/*
			 * third column in sa is rs count
			 */
			sa[2] = (short) v.getRight().size();
			/*
			 * fourth column is rs nns count
			 * Need to get set of start positions from combinedReadIdsStarPos for our readIds 
			 */
			sa[3] = (short)getUniqueCount(combinedReadIdsStarPos, v.getRight(), false);
			return true;
			});
		
		return allelesCountsNNS;
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
	 * Returns a VcfRecord with just the positional and ref and alt information provided. Does not contain filter, info, format etc.
	 * @param vcfs
	 * @return
	 */
	public static VcfRecord createSkeletonCompoundSnp(List<VcfRecord> vcfs) {
		/*
		 * sort list
		 */
		vcfs.sort(null);
		StringBuilder ref = new StringBuilder();
		StringBuilder alt = new StringBuilder();
		ChrPosition startPosition = vcfs.get(0).getChrPosition();
		
		for (VcfRecord v : vcfs) {
			ref.append(v.getRefChar());
			alt.append(v.getAlt());
		}
		return VcfUtils.createVcfRecord(startPosition, null, ref.toString(), alt.toString());
	}
	
	
	public static Pair<List<Accumulator>, List<Accumulator>> getAccs(Map<VcfRecord, Pair<Accumulator, Accumulator>> vcfs) {
		/*
		 * sort keys in map
		 */
		List<VcfRecord> l = new ArrayList<>(vcfs.keySet());
		l.sort(null);
		
		/*
		 * extract left for control, right for test
		 */
		List<Accumulator> cAccs = new ArrayList<>(vcfs.size() + 1);
		List<Accumulator> tAccs = new ArrayList<>(vcfs.size() + 1);
		
		for (VcfRecord v : l) {
			Pair<Accumulator, Accumulator> p = vcfs.get(v);
			if (null != p.getLeft()) {
				cAccs.add(p.getLeft());
			}
			if (null != p.getRight()) {
				tAccs.add(p.getRight());
			}
		}
		
		return new Pair<>(cAccs,tAccs);
	}
	
	public static Optional<String> getReference(Collection<VcfRecord> vcfs) {
		return Optional.ofNullable(vcfs.stream().sorted().map(VcfRecord::getRef).collect(Collectors.joining()));
	}
	
	/**
	 * REturns a count of either the novel starts
	 * 
	 * map contains bases as key, and short array contains 4 elements, which are (in this order):
	 * forward strand count
	 * forward strand novel starts count
	 * reverse strand count
	 * reverse strand novel starts count
	 * 
	 * Offset dictates whether you are getting novel starts (offset = 1), or counts (offset = 0)
	 * 
	 * 
	 * @param map
	 * @param key
	 * @param offset
	 * @return
	 */
	public static int getCount(Map<String,  short[]> map, String key, int offset) {
		short[] sa =map.get(key);
		return (null !=  sa) ? sa[0 + offset] + sa[2 + offset] : 0;
	}
	
	/**
	 * returns the novel starts counts for both strands for this base (key)
	 *  @see getCount(Map<String,  short[]> map, String key, int offset)
	 * @param map
	 * @param key
	 * @return
	 */
	public static int getNovelStartsCounts(Map<String,  short[]> map, String key) {
		return getCount(map, key, 1);
	}
	/**
	 * Returns the total count for both strands for this base
	 * @see getCount(Map<String,  short[]> map, String key, int offset)
	 * @param map
	 * @param key
	 * @return
	 */
	public static int getTotalCounts(Map<String,  short[]> map, String key) {
		return getCount(map, key, 0);
	}
	
	public static String[] getMR(Map<String, short[]> map, String[] aAlts, int firstG, int secondG) {
		if (null == aAlts) {
			throw new IllegalArgumentException("Null or empty alts passed to PipelIneUtil.getMR");
		}
		if (null == map) {
			throw new IllegalArgumentException("Null map passed to PipelIneUtil.getMR");
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
		if (mr.length() == 0) {
			mr.append(Constants.MISSING_DATA_STRING);
		}
		if (nns.length() == 0) {
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
	 * Classification (ie. SOMATIC) must be same for all snps - thats about the only rule...
	 * oh, and the genotypes need to be the same for all control samples and for all test samples
	 * eg. 0/0 ->0/1 for all snps in cs
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
				logger.warn("null dp array for rec: " + v.toString());
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
				
					VcfRecord firstRec = vcfs.get(0);
					VcfRecord v = VcfUtils.createVcfRecord(firstRec.getChrPosition(), null, csRef, csAlt);
					
					/*
					 * sort collections to get lowest value first - thats what we will use
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
						cSB = new StringBuilder(controlGTs.get(0));								//GT
						StringUtils.updateStringBuilder(cSB, controlADs.get(0), Constants.COLON);	//AD
						StringUtils.updateStringBuilder(cSB, controlDPs.get(0), Constants.COLON);		//DP
						StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//FT
						StringUtils.updateStringBuilder(cSB,controlGQs.get(0), Constants.COLON);		// GQ field
						StringUtils.updateStringBuilder(cSB,cINF, Constants.COLON);		// INF field
						StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//NNS
						StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//OABS
						StringUtils.updateStringBuilder(cSB, controlQLs.get(0), Constants.COLON);								//QL
					}
					
					StringBuilder tSB = new StringBuilder(testGTs.get(0));
					StringUtils.updateStringBuilder(tSB, testADs.get(0), Constants.COLON);
					StringUtils.updateStringBuilder(tSB, testDPs.get(0), Constants.COLON);
					StringUtils.updateStringBuilder(tSB, Constants.MISSING_DATA_STRING, Constants.COLON);
					StringUtils.updateStringBuilder(tSB, testGQs.get(0), Constants.COLON);	// GQ field
					StringUtils.updateStringBuilder(tSB, somCount > 0 ? "SOMATIC" : Constants.MISSING_DATA_STRING, Constants.COLON);	// INF field
					StringUtils.updateStringBuilder(tSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//NNS
					StringUtils.updateStringBuilder(tSB, Constants.MISSING_DATA_STRING, Constants.COLON);	//OABS
					StringUtils.updateStringBuilder(tSB, testQLs.get(0), Constants.COLON);									//QL
					
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
	
	public static Optional<VcfRecord> createCompoundSnp(Map<VcfRecord, Pair<Accumulator, Accumulator>> vcfs, List<Rule> controlRules, List<Rule> testRules, boolean runSBias, int sBiasCov, int sBiasAlt) {
		
		Pair<List<Accumulator>, List<Accumulator>> p = getAccs(vcfs);
		Optional<String> ref = getReference(vcfs.keySet());
		
		Map<String, short[]> cBasesCountsNNS =  getBasesFromAccumulators(p.getLeft());
		Map<String, short[]> tBasesCountsNNS =  getBasesFromAccumulators(p.getRight());
		
		int controlCov = getCoverage(cBasesCountsNNS);
		int testCov = getCoverage(tBasesCountsNNS);
		Rule cr = RulesUtil.getRule(controlRules, controlCov);
		Rule tr = RulesUtil.getRule(testRules, testCov);
		
		/*
		 * check to see if we need to use a percentage, if so, calculate the minCov based on totalCov
		 */
		int cMinCov = null != cr ? cr.getMaxCoverage() == Integer.MAX_VALUE ? (cr.getNoOfVariants() * controlCov) / 100 : cr.getNoOfVariants() : 2;
		int tMinCov = null != tr ? tr.getMaxCoverage() == Integer.MAX_VALUE ? (tr.getNoOfVariants() * testCov) / 100 : tr.getNoOfVariants() : 2;
		
		List<String> cGT = getBasesForGenotype (cBasesCountsNNS, cMinCov, ref.get());
		List<String> tGT = getBasesForGenotype (tBasesCountsNNS, tMinCov, ref.get());
		
		
		/*
		 * check to see if we have a mutation
		 * if getBasesForGenotype only returns an empty list OR a list just containing the reference for BOTH control, and test, then we don't have a mutation
		 */
		if ((cGT.isEmpty() || (cGT.size() == 1 && cGT.get(0).equals(ref.get()))) 
				&& (tGT.isEmpty() || (tGT.size() == 1 && tGT.get(0).equals(ref.get())))) {
			return Optional.empty();
		}
		
		/*
		 * this list should contain 3 elements, the first is a comma seperated list of alt alleles.
		 * The second and third elements are the control and test GT fields
		 */
		List<String> altsAndGTs = getAltStringAndGenotypes(cGT, tGT, ref.get());
		/*
		 * If alt string is '.' - no dice
		 */
		if (altsAndGTs.isEmpty() || StringUtils.isNullOrEmptyOrMissingData(altsAndGTs.get(0))) {
			return Optional.empty(); 
		}
		
		VcfRecord firstRec = vcfs.keySet().stream().sorted().findFirst().get();
		
		VcfRecord v = VcfUtils.createVcfRecord(firstRec.getChrPosition(), null, ref.get(), altsAndGTs.get(0));
		
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
		Classification c = GenotypeUtil.getClassification(cBasesCountsNNS.keySet().stream().collect(Collectors.joining(Constants.COMMA_STRING)), altsAndGTs.get(1), altsAndGTs.get(2), altsAndGTs.get(0));
		
		/*
		 * format fields - going for GT:AD:DP:FT:INF:NNS:OABS
		 */
		Optional<String> oOabs = getOABS(cBasesCountsNNS);
		String oabs = oOabs.isPresent() ? oOabs.get() : Constants.MISSING_DATA_STRING;
		
		StringBuilder cSB = new StringBuilder(altsAndGTs.get(1));
		StringUtils.updateStringBuilder(cSB, VcfUtils.getAD(ref.get(), altsAndGTs.get(0), oabs), Constants.COLON);
		StringUtils.updateStringBuilder(cSB, controlCov > 0 ? controlCov+"" : "0", Constants.COLON);
		/*
		 * filters are applied in qannotate now
		 */
		StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);
		String [] mrNNS =  getMR(cBasesCountsNNS, aAlts, controlFirstG, controlSecondG);
		StringUtils.updateStringBuilder(cSB, Constants.MISSING_DATA_STRING, Constants.COLON);	// INF field
		StringUtils.updateStringBuilder(cSB, mrNNS[1], Constants.COLON);
		StringUtils.updateStringBuilder(cSB, oabs, Constants.COLON);
		
		oOabs = getOABS(tBasesCountsNNS);
		oabs = oOabs.isPresent() ? oOabs.get() : Constants.MISSING_DATA_STRING;
		
		StringBuilder tSB = new StringBuilder(altsAndGTs.get(2));
		StringUtils.updateStringBuilder(tSB, VcfUtils.getAD(ref.get(), altsAndGTs.get(0), oabs), Constants.COLON);
		StringUtils.updateStringBuilder(tSB, testCov > 0 ? testCov +"" :  "0", Constants.COLON);
		/*
		 * filters are applied in qannotate now
		 */
		StringUtils.updateStringBuilder(tSB, Constants.MISSING_DATA_STRING, Constants.COLON);
		StringUtils.updateStringBuilder(tSB, (c == Classification.SOMATIC ? VcfHeaderUtils.INFO_SOMATIC : Constants.MISSING_DATA_STRING), Constants.COLON);	// INF field
		mrNNS =  getMR(tBasesCountsNNS,aAlts, testFirstG, testSecondG);
		StringUtils.updateStringBuilder(tSB, mrNNS[1], Constants.COLON);
		StringUtils.updateStringBuilder(tSB, oabs, Constants.COLON);
		
		v.setFormatFields(Arrays.asList("GT:AD:DP:FT:INF:NNS:OABS", cSB.toString(), tSB.toString()));
		
		return Optional.ofNullable(v);
	}

}
