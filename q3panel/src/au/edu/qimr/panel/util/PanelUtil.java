package au.edu.qimr.panel.util;

import au.edu.qimr.panel.model.Contig;
import au.edu.qimr.panel.model.Fragment2;
import gnu.trove.map.hash.THashMap;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.VcfUtils;

public class PanelUtil {
	
	private static QLogger logger = QLoggerFactory.getLogger(PanelUtil.class);
	
	/**
	 * Loop through the supplied map of variants. For each one, get all fragments that overlap the position so that an OABS can be build up.
	 * Create VCfRecord based on that and add to list - simps!
	 * 
	 * @param variants
	 * @param contigFragmentMap
	 * @param vcfs
	 */
	public static void createVcfRecordFromVariantMap(Map<ChrPosition, Map<String, List<Fragment2>>> variants, 
			Map<Contig, List<Fragment2>> contigFragmentMap, List<VcfRecord> vcfs, int minAltPercentage) {
		
		for (Entry<ChrPosition, Map<String, List<Fragment2>>> entry : variants.entrySet()) {
			String ref = entry.getKey().getName();
			
			/*
			 * get list of fragment ids for this entry
			 */
			Set<Fragment2> altFragments = entry.getValue().values().stream().flatMap(List::stream).collect(Collectors.toSet());
			List<Fragment2> wildtypeFragments = new ArrayList<>();
			Map<String, int[]> basesAndCounts = new THashMap<>(4);
			
			
			/*
			 * loop through each contig to see if any overlap the ChrPosition in the entry key
			 */
			for (Contig c : contigFragmentMap.keySet()) {
				if (ChrPositionUtils.doChrPositionsOverlap(c.getPosition(), entry.getKey())) {
					List<Fragment2> fragments = contigFragmentMap.get(c);
					for (Fragment2 f : fragments) {
						if ( ! altFragments.contains(f)) {
							wildtypeFragments.add(f);
						}
					}
				}
			}
			
			/*
			 * wildtype first
			 */
			basesAndCounts.put(ref, FragmentUtil.getCountsFromFragments(wildtypeFragments));
			/*
			 * now the alts
			 */
			for (Entry<String, List<Fragment2>> entry2 : entry.getValue().entrySet()) {
				basesAndCounts.put(entry2.getKey(), FragmentUtil.getCountsFromFragments(entry2.getValue()));
			}
			
			String oabs = getOABS(basesAndCounts);
			
			String[] gtAdAlts = getGTADAlts(basesAndCounts, ref, minAltPercentage);
			
			/*
			 * create vcf record and add to list
			 * but only if we have a gt that is not "0/0"
			 */
			if ( ! gtAdAlts[0].equals("0/0")) {
				VcfRecord vcf = VcfUtils.createVcfRecord(entry.getKey(), Constants.MISSING_DATA_STRING, ref, gtAdAlts[2]);
				vcf.setFormatFields(Arrays.asList("GT:AD:DP:OABS", gtAdAlts[0] + Constants.COLON + gtAdAlts[1] + Constants.COLON 
						+ basesAndCounts.values().stream().mapToInt(a -> a[0] + a[1]).sum() + Constants.COLON + oabs));
				vcfs.add(vcf);
			}
		}
	}
	
	public static int getSumOfArray(int [] array) {
		if (null != array && array.length == 2) {
			return array[0] + array[1];
		}
		return 0;
	}
	
	public static String[] getGTADAlts(Map<String, int[]> basesAndCounts, String ref, int minPercentage) {
		String [] gtAdAlts = new String[]{".",".","."};
		if (null != basesAndCounts && ! basesAndCounts.isEmpty() && null != ref && ref.length() > 0) {
			List<String> twoLargestAlleles = getTwoLargestAlleles(basesAndCounts, minPercentage);
			String gt = "0/0";
			String ad = "" + getSumOfArray(basesAndCounts.get(ref));
			String alts = "";
			if (null != twoLargestAlleles && ! twoLargestAlleles.isEmpty()) {
				int nonRefCounter = 0;
				for (String s : twoLargestAlleles) {
					if ( ! s.equals(ref)) {
						ad += "," + getSumOfArray(basesAndCounts.get(s));
						nonRefCounter++;
						alts += nonRefCounter > 1 ? Constants.COMMA + s : s;
					}
				}
				gt = nonRefCounter == 2 ? "1/2" 
						: nonRefCounter == 1 && twoLargestAlleles.contains(ref) ? "0/1"
								: nonRefCounter == 0 ? "0/0" : "1/1";
			}
			if (alts.equals("")) {
				alts = Constants.MISSING_DATA_STRING;
			}
			
			gtAdAlts[0] = gt;
			gtAdAlts[1] = ad;
			gtAdAlts[2] = alts;
		}
		return gtAdAlts;
	}
	
	
	public static List<String> getTwoLargestAlleles(Map<String, int[]> basesAndCounts, int minPercentage) {
		List<String> alleles = new ArrayList<>(3);
		int totalCoverage = basesAndCounts.values().stream().mapToInt(a -> a[0] + a[1]).sum();
		alleles = basesAndCounts.entrySet().stream().filter(e -> 
			((double)(e.getValue()[0] + e.getValue()[1]) / totalCoverage) * 100 >= minPercentage).sorted((e1, e2) -> {
					int diff = (e1.getValue()[0] + e1.getValue()[1]) - (e2.getValue()[0] + e2.getValue()[1]);
					if (diff == 0) {
						diff = e1.getKey().compareTo(e2.getKey());
					}
					return diff;
					}).map(e -> e.getKey()).collect(Collectors.toList());
		
		while (alleles.size() > 2) {
			return Arrays.asList(alleles.get(0), alleles.get(1));
		}
		
		return alleles;
	}
		
	public static String getOABS(Map<String, int[]> basesAndCounts) {
		return basesAndCounts.entrySet().stream()
				.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
				.filter(e -> getSumOfArray(e.getValue()) > 0)
				.map(e -> e.getKey() + e.getValue()[0] + "[]" + e.getValue()[1] + "[]")
				.collect(Collectors.joining(Constants.SEMI_COLON_STRING));
		
	}

	public static String trimString(String orig, int trimLength) {
		if (trimLength <= 0) {
			return orig;
		}
		/*
		 * want to trim length from the start and end of the original string, so need to make sure that we have enough of orig to be able to do this
		 */
		int len = orig.length();
		if (2 * trimLength >= len) {
			/*
			 * there would be no string left if we did this, so just log and return the original string
			 */
			logger.warn("in trimString with original string: " + orig + ", and trimLength: " + trimLength);
			return orig;
		} else {
			return orig.substring(trimLength, len - trimLength);
		}
	}

	public static Optional<String> createBasicFragment(String s1, String s2, int minOverlap) {
		String s1End = s1.substring(s1.length() - minOverlap);
		int index = s2.indexOf(s1End);
		if (index > -1) {
			/*
			 * create overlap string
			 */
			return Optional.ofNullable(s1 + s2.substring(index + minOverlap));
		} else {
			/*
			 * try s2End
			 */
			String s2End = s2.substring(s2.length() - minOverlap);
			index = s1.indexOf(s2End);
			if (index > -1) {
				/*
				 * create overlap string
				 */
				return Optional.ofNullable(s2 + s1.substring(index + minOverlap));
			}
		}
		
		return Optional.empty();
	}

	public static int[] getAltAndTotalCoverage(VcfRecord vcf, List<int[]> fragmentsCarryingMutation, Map<String, List<Fragment2>> fragsByContig) {
		/*
		 * Get the total coverage at this position, along with the total alt coverage
		 * Update the DP and MR format fields of this vcf record with this info
		 */
		List<Fragment2> overlappingFragments = ClinVarUtil.getOverlappingFragments(vcf.getChrPosition(), fragsByContig);
		int totalCoverage = overlappingFragments.stream()
				.mapToInt(Fragment2::getRecordCount)
				.sum();
		int mutationCoverage = PanelUtil.getRecordCountFromIntPairs(fragmentsCarryingMutation);
		
		return new int[]{mutationCoverage, totalCoverage};
	}

	public static int getRecordCountFromIntPairs(List<int[]> list) {
		return list.stream()
			.mapToInt(i -> i[2])
			.sum();
	}

	public static void setActualCP(ChrPosition bufferedCP, int offset, Fragment2 f, int referenceLength) {
		final int startPosition =  bufferedCP.getStartPosition() + offset + 1;	// we are 1 based
		// location needs to reflect reference bases consumed rather sequence length
		ChrPosition actualCP = new ChrRangePosition(bufferedCP.getChromosome(), startPosition, startPosition + referenceLength -1);
		/*
		 * setting the actual (or final) position
		 */
		f.setPosition(actualCP, true);
	}
	
	/**
	   * Get a lazily loaded stream of lines from a gzipped file, similar to
	   * {@link Files#lines(java.nio.file.Path)}.
	   * 
	   * @param path
	   *          The path to the gzipped file.
	   * @return stream with lines.
	   */
	  public static Stream<String> lines(Path path) {
	    InputStream fileIs = null;
	    BufferedInputStream bufferedIs = null;
	    GZIPInputStream gzipIs = null;
	    try {
	      fileIs = Files.newInputStream(path);
	      // Even though GZIPInputStream has a buffer it reads individual bytes
	      // when processing the header, better add a buffer in-between
	      bufferedIs = new BufferedInputStream(fileIs, 65535);
	      gzipIs = new GZIPInputStream(bufferedIs);
	    } catch (IOException e) {
	      closeSafely(gzipIs);
	      closeSafely(bufferedIs);
	      closeSafely(fileIs);
	      throw new UncheckedIOException(e);
	    }
	    BufferedReader reader = new BufferedReader(new InputStreamReader(gzipIs));
	    return reader.lines().onClose(() -> closeSafely(reader));
	  }
	  
	  private static void closeSafely(Closeable closeable) {
		    if (closeable != null) {
		      try {
		        closeable.close();
		      } catch (IOException e) {
		        // Ignore
		      }
		    }
		  }

}
