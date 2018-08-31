package org.qcmg.qprofiler2.bam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS;
import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.model.MAPQMatrix;
import org.qcmg.common.model.MAPQMiniMatrix;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.model.SummaryByCycle;
import org.qcmg.common.model.MAPQMatrix.MatrixType;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.common.util.SummaryByCycleUtils;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.qcmg.qprofiler2.summarise.ReadIDSummary;
import org.qcmg.qprofiler2.util.CycleSummaryUtils;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecord.SAMTagAndValue;
import htsjdk.samtools.SAMTagUtil;
import htsjdk.samtools.SAMUtils;

//for sam record tag information
public class TagSummaryReport2 {

	public final static int errReadLimit  = 100;	
	private final static SAMTagUtil STU = SAMTagUtil.getSingleton();
	private final short CS = STU.CS;
	private final short CQ = STU.CQ;
	private final short RG = STU.RG;
	private final short CM = STU.CM;
	private final short SM = STU.SM;
	private final short NH = STU.NH;
	private final short MD = STU.MD;
	private final short IH = STU.IH;
	// custom tags
	private final short ZM = STU.makeBinaryTag("ZM");
	private final short ZP = STU.makeBinaryTag("ZP");
	private final short ZF = STU.makeBinaryTag("ZF");
	
	// TAGS
	final CycleSummary<Character> tagCSByCycle = new CycleSummary<Character>(BamSummaryReport2.cc, 512);
	private final QCMGAtomicLongArray csBadReadLineLengths = new QCMGAtomicLongArray(128);
	final CycleSummary<Integer> tagCQByCycle = new CycleSummary<Integer>(BamSummaryReport2.ii, 512);
	private final QCMGAtomicLongArray cqBadReadLineLengths = new QCMGAtomicLongArray(128);
	private final SummaryByCycle<Integer> zmSmMatrix = new SummaryByCycle<Integer>(128);
	
//	final ConcurrentMap<String, AtomicLong> tagRGLineLengths = new ConcurrentHashMap<String, AtomicLong>();
	final QCMGAtomicLongArray tagZMLineLengths = new QCMGAtomicLongArray(2048);
	private final QCMGAtomicLongArray tagCMLineLengths = new QCMGAtomicLongArray(128);
	private final QCMGAtomicLongArray tagSMLineLengths = new QCMGAtomicLongArray(256);
	private final QCMGAtomicLongArray tagNHLineLengths = new QCMGAtomicLongArray(1024);
	private final QCMGAtomicLongArray tagIHLineLengths = new QCMGAtomicLongArray(1024);
	
	// mapqMatrix
	final ConcurrentMap<Integer, MAPQMatrix> mapQMatrix = new ConcurrentSkipListMap<Integer, MAPQMatrix>();
	
	@SuppressWarnings("unchecked")
	final CycleSummary<Character>[] tagMDMismatchByCycle = new CycleSummary[]{new CycleSummary<Character>(BamSummaryReport2.cc, 512), new CycleSummary<Character>(BamSummaryReport2.cc, 512), new CycleSummary<Character>(BamSummaryReport2.cc, 512)};	
	private final QCMGAtomicLongArray[] mdRefAltLengthsForward = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32)};	
	private final QCMGAtomicLongArray[] mdRefAltLengthsReverse = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32), new QCMGAtomicLongArray(32)};	
    final QCMGAtomicLongArray[] allReadsLineLengths = new QCMGAtomicLongArray[]{new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024), new QCMGAtomicLongArray(1024)};
	
	// new tags for JP
	private final ConcurrentMap<String, AtomicLong> tagZPLineLengths = new ConcurrentHashMap<String, AtomicLong>();
	private final QCMGAtomicLongArray tagZFLineLengths = new QCMGAtomicLongArray(128);		
	private final ConcurrentMap<String, ConcurrentSkipListMap<String, AtomicLong>> additionalTags = new ConcurrentSkipListMap<String, ConcurrentSkipListMap<String, AtomicLong>>();
	private final ConcurrentMap<String, QCMGAtomicLongArray> additionalIntegerTags = new ConcurrentSkipListMap<String, QCMGAtomicLongArray>();
	private final ConcurrentMap<String, ConcurrentSkipListMap<Character, AtomicLong>> additionalCharacterTags = new ConcurrentSkipListMap<String, ConcurrentSkipListMap<Character, AtomicLong>>();
	protected QLogger logger = QLoggerFactory.getLogger(getClass());
	
	private String [] tags;
	private String [] tagsInt;
	private String [] tagsChar;
	private boolean torrentBam;	
	private boolean includeMatrices;
	private long errMDReadNo ;
	private long errIdReadNo ;

//	//read name
//	private final ConcurrentMap<String, ReadIDSummary> readIdSummary = new ConcurrentHashMap<String, ReadIDSummary>();
//	private final ConcurrentMap<String, AtomicLong> inValidReadIds = new ConcurrentHashMap<String, AtomicLong>();
	
//	public TagSummaryReport2( String [] tags, String [] tagsInt, String [] tagsChar) {
	public TagSummaryReport2() {
//		this.tags = tags;
//		this.tagsInt = tagsInt;
//		this.tagsChar = tagsChar;	
		
		//init
		this.errMDReadNo = 0;
		this.torrentBam = false;
		this.includeMatrices = false;
	//	setupAdditionalTagMaps();		
	}
 
	
	/**
	 * default torrentBam is false, unless this method is called
	 */
	public void setTorrentBam(){ this.torrentBam = true; }
	public void setInclMatrices(){ this.includeMatrices = true; }
	
	public void parseTAGs(final SAMRecord record )  {
		byte[] readBases = record.getReadBases();
		boolean reverseStrand =record.getReadNegativeStrandFlag();		
		
//		//RG
//		String value = (String) record.getAttribute(RG);	
//		if(value == null ) value = QprofilerXmlUtils.UNKNOWN_READGROUP;
//		 
//		SummaryByCycleUtils.incrementCount(tagRGLineLengths, value);						
//		readIdSummary.computeIfAbsent( value, (k) -> new ReadIDSummary() );
//		
//		try {			
//			readIdSummary.get(value).parseReadId( record.getReadName() );
//		} catch (Exception e) {
//			 SummaryByCycleUtils.incrementCount(inValidReadIds, value);
//			 if ( (errIdReadNo ++) < errReadLimit )  logger.warn( "invalid read name: " + record.getReadName() );			 
//		}
				
		//MD	 
		String value = (String) record.getAttribute(MD);
		if (null != value) {
			//0: unpaired , 1: firstOfPair , 2: secondOfPair				
			int order = (!record.getReadPairedFlag())? 0: (record.getFirstOfPairFlag())? 1 : 2;					
			String err = CycleSummaryUtils.tallyMDMismatches(value, record.getCigar(), tagMDMismatchByCycle[order], readBases, reverseStrand, mdRefAltLengthsForward[order], mdRefAltLengthsReverse[order]);
			//limit err message on log file
			if ( err != null && (( errMDReadNo ++) < errReadLimit)) logger.warn(record.getReadName() + ": " + err);
			
			//this counts will be used to caculate % for MD
			for( int i = 1; i <= record.getReadLength(); i ++ )
				allReadsLineLengths[order].increment(i);
				
		}
		// additionalTags	 
		for( SAMTagAndValue tag : record.getAttributes()) {
			if(tag.tag.equals("MD")) continue;
			try {
				if( record.getAttribute(tag.tag) instanceof Integer ) 
					additionalIntegerTags.computeIfAbsent(tag.tag, k-> new QCMGAtomicLongArray(100))
					.increment(record.getIntegerAttribute(tag.tag));
				else if(record.getAttribute(tag.tag) instanceof Character ) { 
					Map<Character, AtomicLong> map = additionalCharacterTags.computeIfAbsent(tag.tag, k-> new ConcurrentSkipListMap<Character, AtomicLong>());					
					map.computeIfAbsent(record.getCharacterAttribute(tag.tag), k-> new AtomicLong()).incrementAndGet();
				}else if(record.getAttribute(tag.tag) instanceof String) {					
					Map<String, AtomicLong> map = additionalTags.computeIfAbsent(tag.tag, k-> new ConcurrentSkipListMap<String, AtomicLong>());
					long v = map.computeIfAbsent(record.getStringAttribute(tag.tag), k-> new AtomicLong()).incrementAndGet();	
					//debug
					//System.out.println(tag.tag + " => " + record.getStringAttribute(tag.tag) + " = " + v);
				}
			}catch(Exception e) {
				Map<String, AtomicLong> map = additionalTags.computeIfAbsent("Others", k-> new ConcurrentSkipListMap<String, AtomicLong>());
				map.computeIfAbsent(tag.tag, k-> new AtomicLong()).incrementAndGet();					
			}
		}			

	}
	
	
	public void toXml(Element parent){
				
		//MD tag
		Element childElement = QprofilerXmlUtils.createSubElement(parent, "subField");	//SEQ
		childElement.setAttribute("Category", "TAG:MD");
		
		//mismatchbycycle
		for(int order = 0; order < 3; order ++) 
			tagMDMismatchByCycle[order].toXml(childElement, "mismatch distribution per base cycle",
					BamSummaryReport2.sourceName[order],"read base cycle");
			
		for(int order = 0; order < 3; order ++) {	
			for(String strand : new String[]{"MutationForward", "MutationReverse"}){				
				Map<String, AtomicLong> mdRefAltLengthsString = new HashMap<String, AtomicLong>();
				QCMGAtomicLongArray mdRefAltLengths = (strand.equals("MutationForward"))? mdRefAltLengthsForward[order] : mdRefAltLengthsReverse[order];				
				for (int m = 0 ; m < mdRefAltLengths.length() ; m++) {
					long l = mdRefAltLengths.get(m);
					if (l <= 0)  continue;
					mdRefAltLengthsString.put(CycleSummaryUtils.getStringFromInt(m), new AtomicLong(l));					 
				}
				XmlUtils.outputSet(childElement, "base counts", String.format("%s base distribution per mutation value on %s", strand, BamSummaryReport2.sourceName[order]), 
						strand+BamSummaryReport2.sourceName[order], "mutation value", mdRefAltLengthsString);
			}		
		}
 

		
		// additional tags includes RG
		for (Entry<String,  ConcurrentSkipListMap<String, AtomicLong>> entry : additionalTags.entrySet()) {
			XmlUtils.outputSet(parent,"read counts","read distribution",  "tags:" + entry.getKey(), "tag value",entry.getValue());
		}
		// additional tagsInt
		for (Entry<String,  QCMGAtomicLongArray> entry : additionalIntegerTags.entrySet()) {
			XmlUtils.outputSet(parent,"counts","read distribution",  "tagInt:" + entry.getKey(),"tag value", entry.getValue());
		}
		// additional tagsChar
		for (Entry<String,  ConcurrentSkipListMap<Character, AtomicLong>> entry : additionalCharacterTags.entrySet()) {
			XmlUtils.outputSet(parent,"counts","read distribution",  "tagChar:" + entry.getKey(),"tag value", entry.getValue());
		}	
	}

//	private void setupAdditionalTagMaps(){
//		if (null != tags) {
//			for (String tag : tags)
//				additionalTags.put(tag, new ConcurrentSkipListMap<String, AtomicLong>());
//		}
//		if (null != tagsInt) {
//			for (String tagInt : tagsInt)
//				additionalIntegerTags.put(tagInt, new QCMGAtomicLongArray((100)));
//		}
//		if (null != tagsChar) {
//			for (String tagChar : tagsChar)
//				additionalCharacterTags.put(tagChar,  new ConcurrentSkipListMap<Character, AtomicLong>());
//		}
//	}	

	void generateMAPQSubMaps(Map<MAPQMiniMatrix, AtomicLong> cmMatrix,
			Map<MAPQMiniMatrix, AtomicLong> smMatrix,
			Map<MAPQMiniMatrix, AtomicLong> lengthMatrix,
			Map<MAPQMiniMatrix, AtomicLong> nhMatrix,
			Map<MAPQMiniMatrix, AtomicLong> zmMatrix) {

		logger.debug("mapQMatrix.size(): " + mapQMatrix.size());

		Map<Integer, AtomicLong> map = null;

		for (Entry<Integer, MAPQMatrix> entry : mapQMatrix.entrySet()) {
			logger.debug( entry.getKey() + ", entry.getValue().toString(): " + entry.getValue().toString());

			for (MatrixType type : MatrixType.values()) {
				map = entry.getValue().getMatrixByType(type);

				for (Entry<Integer, AtomicLong> mapEntry : map.entrySet()) {

					switch (type) {
					case CM:
						cmMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
						break;
					case SM:
						smMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
						break;
					case LENGTH:
						lengthMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
						break;
					case NH:
						nhMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
						break;
					case ZM:
						zmMatrix.put(new MAPQMiniMatrix(entry.getKey(), mapEntry.getKey()), mapEntry.getValue());
						break;
					}
				}
			}
			// removing due to memory limitations
			mapQMatrix.remove(entry.getKey());
		}
	}	
	private void createMatrix(Element parent ){
		
		Map<MAPQMiniMatrix, AtomicLong> cmMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> smMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> lengthMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> nhMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> zmMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();

		generateMAPQSubMaps(cmMatrix, smMatrix, lengthMatrix, nhMatrix, zmMatrix);

		logger.debug("cmMatrix(): " + cmMatrix.size());
		logger.debug("smMatrix(): " + smMatrix.size());
		logger.debug("lengthMatrix(): " + lengthMatrix.size());
		logger.debug("nhMatrix(): " + nhMatrix.size());
		logger.debug("zmMatrix(): " + zmMatrix.size());
		


//		SummaryReportUtils.lengthMapToXml(parent, "MAPQMatrixCM",null, cmMatrix, null);
		XmlUtils.outputSet(parent,"not sure", "matrix is included", "MAPQMatrixCM", "not sure", cmMatrix);
		
//		SummaryReportUtils.lengthMapToXml(parent, "MAPQMatrixSM",null, smMatrix,null);
		XmlUtils.outputSet(parent,"not sure", "matrix is included", "MAPQMatrixSM", "not sure", cmMatrix);
		
//		SummaryReportUtils.lengthMapToXml(parent, "MAPQMatrixLength",null, lengthMatrix,null);
		XmlUtils.outputSet(parent,"not sure", "matrix is included", "MAPQMatrixLength", "not sure", cmMatrix);
		
//		SummaryReportUtils.lengthMapToXml(parent, "MAPQMatrixNH",null, nhMatrix,null);
		XmlUtils.outputSet(parent,"not sure", "matrix is included", "MAPQMatrixCM", "not sure", cmMatrix);
		
//		SummaryReportUtils.lengthMapToXml(parent, "MAPQMatrixZM",null, zmMatrix,null);
		XmlUtils.outputSet(parent,"not sure", "matrix is included", "MAPQMatrixZM", "not sure", cmMatrix);
		
		parent = QprofilerXmlUtils.createSubElement( parent, "ZmSmMatrix" );
		XmlUtils.outputSet(parent,"not sure", "matrix is included", "MAPQMatrixZM", "not sure", zmMatrix);
		zmSmMatrix.toXml(parent, "ZmSmMatrix");
	//	XmlUtils.outputSet(parent,"not sure", "matrix is included", "ZmSmMatrix", "not sure", cmMatrix);
		
	}	
	
//	public ConcurrentMap<String, ReadIDSummary>  getReadIDSummary(){	return readIdSummary;	}
	
}
