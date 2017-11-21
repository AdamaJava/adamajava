/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.pileup;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qcmg.pileup.model.Chromosome;


public class PileupUtil {
	
	private static final Pattern pattern = Pattern.compile("\\d{1,2}");
	
    public static String getCurrentTime(String seperator) {
    	//http://www.mkyong.com/java/java-how-to-get-current-date-time-date-and-calender/
    	DateFormat dateFormat = new SimpleDateFormat("HH" + seperator + "mm" + seperator + "ss");
    	Calendar cal = Calendar.getInstance();    	
 	    return dateFormat.format(cal.getTime());
    }
    
	public static String getRunTime(long start, long end) {
		
		long runTimeSec = (end - start) / 1000;
		
		int seconds = (int) (runTimeSec % 60);
        int minutes = (int) ((runTimeSec / 60) % 60);
        int hours = (int) ((runTimeSec / 3600));
        String secondsStr = (seconds < 10 ? "0" : "") + seconds;
        String minutesStr = (minutes < 10 ? "0" : "") + minutes;
        String hoursStr = (hours < 10 ? "0" : "") + hours;
        String time =  hoursStr + ":" + minutesStr + ":" + secondsStr;
        return time;
	}
	
	public static String getCurrentDate() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");		
		return formatter.format(new Date());
	}
	
	public static String getCurrentDateTime() {			
		return getCurrentDate() + "_" +  getCurrentTime("_");
	}
	
	public static String getStrackTrace(Exception e) {
		StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
	}

	public static int getBlockSize(String mode, int noOfThreads) {
		if (mode.equals("merge")) {
			if (noOfThreads > 5) {
				return 1000000;
			} else {
				return 2000000;
			}
		} else {
			if (noOfThreads >= 5) {
				return 2000000;
			} else {
				return 5000000;
			}
		}
	}

	public static final String[] byteToString(byte[] bytes, int length) {
        if (bytes == null) {
            return null;
        }

        int n = bytes.length / length;
        String[] strArray = new String[n];
        String str = null;
        int idx = 0;
        for (int i = 0; i < n; i++) {
            str = new String(bytes, i * length, length);
            idx = str.indexOf('\0');
            if (idx > 0) {
                str = str.substring(0, idx);
            }

            // trim only the end
            int end = str.length();
            while (end > 0 && str.charAt(end - 1) <= '\u0020')
                end--;

            strArray[i] = (end <= 0) ? "" : str.substring(0, end);
        }

        return strArray;
    }

	 public static final byte[] stringToByte(String[] strings, int length) {
        if (strings == null) {
            return null;
        }

        int size = strings.length;
        byte[] bytes = new byte[size * length];

        StringBuilder strBuff = new StringBuilder(length);
//        StringBuffer strBuff = new StringBuffer(length);
        for (int i = 0; i < size; i++) {
            // initialize the string with spaces
            strBuff.replace(0, length, " ");

            if (strings[i] != null) {
                if (strings[i].length() > length) {
                    strings[i] = strings[i].substring(0, length);
                }
                strBuff.replace(0, length, strings[i]);
            }

            strBuff.setLength(length);
            System.arraycopy(strBuff.toString().getBytes(), 0, bytes, length
                    * i, length);
        }

        return bytes;
    }
	
	public static Map<String, List<Chromosome>> getChromosomeRangeMap(List<String> readRanges, List<Chromosome> chromosomes) throws QPileupException {
		Map<String, List<Chromosome>> queueMap = new TreeMap<>();
		
		if (readRanges.size() == 1 && readRanges.get(0).equals("all")) {
			for (Chromosome chr: chromosomes)  {
				addToMap(chr, queueMap);
			}
		} else {		
			for (String readRange: readRanges) {			
				if (readRange.contains(":")) {
					String [] params = readRange.split(":"); 
					String chrName = params[0];
					String pos = params[1];
					String [] posParams = pos.split("-");
					int start = Integer.parseInt(posParams[0]);
					int end = Integer.parseInt(posParams[1]);
					Chromosome chr = new Chromosome(chrName, getChromosomeByName(chrName, chromosomes).getTotalLength(), start, end);				
					addToMap(chr, queueMap);	        					
				} else {
					String chrName = readRange;
					Chromosome chr = getChromosomeByName(chrName, chromosomes);
					if (chr != null) {
						addToMap(chr, queueMap);
					} else {
						throw new QPileupException("BAD_CHROMOSOME", chrName);
					}
				}
			}
		}
		return queueMap;
	}
	
	private static void addToMap(Chromosome chr, Map<String, List<Chromosome>> queueMap) {
		
		List<Chromosome> chrs = queueMap.get(chr.getName());
		if (null == chrs) {
			queueMap.put(chr.getName(), Arrays.asList(chr));
		} else {
			/*
			 * I hope there are not too many entries in the list here.....
			 */
			if ( ! chrs.contains(chr)) {
				chrs.add(chr);
			}
		}
	}
	
	private static Chromosome getChromosomeByName(String chrName, List<Chromosome> chromosomes) {
		for (Chromosome c: chromosomes) {
			if (c.getName().equals(chrName)) {
				return c;
			}
		}
		return null;
	}
	
	public static boolean isRegularityType(String type) {
		return type.equals(PileupConstants.METRIC_INDEL) || type.equals(PileupConstants.METRIC_NONREFBASE) || type.equals(PileupConstants.METRIC_CLIP);
	}
	
	public static String getFullChromosome(String ref) {
		if (addChromosomeReference(ref)) {			
			return "chr" + ref;
		} else {			
			return ref;
		}
	}
	
	public static boolean addChromosomeReference(String ref) {
		
		if (ref.startsWith("chr")) {
			return false;
		} else {
			
		 	if (ref.equals("X") || ref.equals("Y") || ref.equals("M") || ref.equals("MT")) {
		  		return true;
		  	} 
			
		    Matcher matcher = pattern.matcher(ref);
		    if (matcher.matches()) {			    	
			    	if (Integer.parseInt(ref) < 23) {
			    		return true;
			    	}
		    }
		}
		return false;
	}
	
	public static void createIndexFile(String htmlDir, String links) throws IOException {
		
		if (new File(htmlDir).listFiles().length > 0) {
			
			String name = htmlDir + PileupConstants.FILE_SEPARATOR + "index.html";
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(name)))) {
			
				writer.write("<html>");
				writer.write("<body>");
				
				writer.write(links);
			
				writer.write("</div>\n");
				writer.write("<body>\n");
				writer.write("</body>\n");
				writer.write("</html>\n");
			}
			
		} else {
			new File(htmlDir).delete();
		}		
	}
}
