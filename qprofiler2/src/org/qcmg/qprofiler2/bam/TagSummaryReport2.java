package org.qcmg.qprofiler2.bam;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.Pair;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.util.CycleSummaryUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecord.SAMTagAndValue;
import htsjdk.samtools.SAMTagUtil;

//for sam record tag information
public class TagSummaryReport2 {

	public final static int additionTagMapLimit = 200;
	public final static int errReadLimit  = 10;	
	public final static String seperator = Constants.COLON_STRING;	

	private final static SAMTagUtil STU = SAMTagUtil.getSingleton();
	private final short MD = STU.MD;	
	
	// TAGS		
	@SuppressWarnings("unchecked")
	final CycleSummary<Character>[] tagMDMismatchByCycle = new CycleSummary[]{new CycleSummary<Character>(BamSummaryReport2.cc, 512), new CycleSummary<Character>(BamSummaryReport2.cc, 512), new CycleSummary<Character>(BamSummaryReport2.cc, 512)};	
	private final QCMGAtomicLongArray[] mdRefAltLengthsForward = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32)};	
	private final QCMGAtomicLongArray[] mdRefAltLengthsReverse = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32)};	
    final QCMGAtomicLongArray[] allReadsLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024)};

	private final ConcurrentMap<String, ConcurrentSkipListMap<String, AtomicLong>> additionalTags = new ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, AtomicLong>>();
	private final ConcurrentMap<String, QCMGAtomicLongArray> additionalIntegerTags = new ConcurrentSkipListMap<String, QCMGAtomicLongArray>();
	private final ConcurrentMap<String, ConcurrentSkipListMap<Character, AtomicLong>> additionalCharacterTags = new ConcurrentSkipListMap<String, ConcurrentSkipListMap<Character, AtomicLong>>();
	protected QLogger logger = QLoggerFactory.getLogger(getClass());	
	private long errMDReadNo = 0 ;

	public void parseTAGs(final SAMRecord record )  {
				
		for( SAMTagAndValue tag : record.getAttributes()) {
			if(tag.tag.equals("MD")) continue;
			
			// the type may be one of A (character), B (generalarray), f (real number), H (hexadecimal array), i (integer), or Z (string).
	        //  Note that H tag type is never written anymore, because B style is more compact.	         
			String type = ":B";			
			if(tag.value instanceof Integer) type = ":i";
			else if (tag.value instanceof Number ) type = ":f";
			else if (tag.value instanceof String) type = ":Z";
			else if (tag.value instanceof Character) type = ":A";

			String key = tag.tag + type;
			if(type.equals(":i")) {
				additionalIntegerTags.computeIfAbsent(key, k-> new QCMGAtomicLongArray(100)).increment(record.getIntegerAttribute(tag.tag));					
			}else {
				Map<String, AtomicLong> map = additionalTags.computeIfAbsent(key, k-> new ConcurrentSkipListMap<String, AtomicLong>());
				if(map.size() < additionTagMapLimit)
					map.computeIfAbsent(tag.value+"", k-> new AtomicLong()).incrementAndGet();
				else
					map.computeIfAbsent("others", k-> new AtomicLong()).incrementAndGet();
			} 			
		}
						
		//MD	 
		String value = (String) record.getAttribute(MD);
		if (null != value) {
			byte[] readBases = record.getReadBases();
			boolean reverseStrand =record.getReadNegativeStrandFlag();		

			//0: unpaired , 1: firstOfPair , 2: secondOfPair				
			int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;					
			String err = CycleSummaryUtils.tallyMDMismatches( value, record.getCigar(), tagMDMismatchByCycle[order], 
					readBases, reverseStrand, mdRefAltLengthsForward[order], mdRefAltLengthsReverse[order]);
			//limit err message on log file
			if ( err != null && (( errMDReadNo ++) < errReadLimit)) logger.warn(record.getReadName() + ": " + err);
			
			//this counts will be used to caculate % for MD
			for( int i = 1; i <= record.getReadLength(); i ++ )
				allReadsLineLengths[order].increment(i);				
		}		
	}
	
	public void toXml(Element parent){						
		//"tags:MD:Z" mismatchbycycle
		Element ele = XmlUtils.createMetricsNode(parent, "tags:MD:Z", null);	
		for(int order = 0; order < 3; order ++) { 
			tagMDMismatchByCycle[order].toXml( ele, BamSummaryReport2.sourceName[order] );
		}
				
		for(String strand : new String[]{ "ForwardStrand", "ReverseStrand" }){				
			for(int order = 0; order < 3; order ++) {				
				Map<String, AtomicLong> mdRefAltLengthsString = new HashMap<>();
				QCMGAtomicLongArray mdRefAltLengths = (strand.contains("Forward"))? mdRefAltLengthsForward[order] : mdRefAltLengthsReverse[order];				
				for (int m = 0 ; m < mdRefAltLengths.length() ; m++) {
					long l = mdRefAltLengths.get(m);
					if (l <= 0)  continue;
					mdRefAltLengthsString.put(CycleSummaryUtils.getStringFromInt(m), new AtomicLong(l));					 
				}
				String name = BamSummaryReport2.sourceName[order] + strand; 
				
				XmlUtils.outputTallyGroup(ele,  name, mdRefAltLengthsString, true);				
			}		
		}			
		
		// additional tags includes RG
		for (Entry<String,  ConcurrentSkipListMap<String, AtomicLong>> entry : additionalTags.entrySet())	
			outputTag(parent, entry.getKey(),  entry.getValue());
		
		// additional tagsInt
		for (Entry<String,  QCMGAtomicLongArray> entry : additionalIntegerTags.entrySet())
			outputTag(parent,  entry.getKey(),  entry.getValue().toMap());
		
		// additional tagsChar
		for (Entry<String,  ConcurrentSkipListMap<Character, AtomicLong>> entry : additionalCharacterTags.entrySet())
			outputTag(parent,  entry.getKey(),  entry.getValue());		
	}
	
	private <T> void outputTag(Element ele, String tag,  Map<T, AtomicLong> tallys) {
				
		int size = tallys.size();	
		ele = XmlUtils.createMetricsNode(ele, "tags:"+tag, new Pair(ReadGroupSummary.READ_COUNT, size));		
		AtomicInteger no = new AtomicInteger();		
		tallys.entrySet().removeIf( e-> no.incrementAndGet() > 100 );
		boolean hasPercent = (size >= 100)? false : true;
				
		String name = tag.substring(0, tag.indexOf(seperator));
		XmlUtils.outputTallyGroup(ele, name, tallys, hasPercent);	
		if( size > 100) 			 
			XmlUtils.addCommentChild(ele, "here only list top 100 tag values" );
					
	}
}
