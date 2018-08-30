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
	
	final ConcurrentMap<String, AtomicLong> tagRGLineLengths = new ConcurrentHashMap<String, AtomicLong>();
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

	//read name
	private final ConcurrentMap<String, ReadIDSummary> readIdSummary = new ConcurrentHashMap<String, ReadIDSummary>();
	private final ConcurrentMap<String, AtomicLong> inValidReadIds = new ConcurrentHashMap<String, AtomicLong>();
	
	public TagSummaryReport2( String [] tags, String [] tagsInt, String [] tagsChar) {
		this.tags = tags;
		this.tagsInt = tagsInt;
		this.tagsChar = tagsChar;	
		
		//init
		this.errMDReadNo = 0;
		this.torrentBam = false;
		this.includeMatrices = false;
		setupAdditionalTagMaps();		
	}
	
	/**
	 * default torrentBam is false, unless this method is called
	 */
	public void setTorrentBam(){ this.torrentBam = true; }
	public void setInclMatrices(){ this.includeMatrices = true; }
	
	public void parseTAGs(final SAMRecord record )  {
		byte[] readBases = record.getReadBases();
		boolean reverseStrand =record.getReadNegativeStrandFlag();		
		
		//RG
		String value = (String) record.getAttribute(RG);	
		if(value == null ) value = QprofilerXmlUtils.UNKNOWN_READGROUP;
		 
		SummaryByCycleUtils.incrementCount(tagRGLineLengths, value);						
		readIdSummary.computeIfAbsent( value, (k) -> new ReadIDSummary() );
		
		try {			
			readIdSummary.get(value).parseReadId( record.getReadName() );
		} catch (Exception e) {
			 SummaryByCycleUtils.incrementCount(inValidReadIds, value);
			 if ( (errIdReadNo ++) < errReadLimit )  logger.warn( "invalid read name: " + record.getReadName() );			 
		}
				
		//MD	 
		value = (String) record.getAttribute(MD);
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
				
		// if the record has a CQ tag, then it will likely have a CS tag
		// if a record does not have a CQ tag, then it will not have a CS tag
		value = (String) record.getAttribute(CQ);
		if (null != value) {
			// CQ
			byte[] valueB = SAMUtils.fastqToPhred(value);
			if (null != valueB) {
				tagCQByCycle.parseByteData(valueB);
				SummaryReportUtils.tallyQualScores(valueB, cqBadReadLineLengths);
			}
			// CS
			value = (String) record.getAttribute(CS);
			if (null != value) {
				tagCSByCycle.parseStringData(value, 1);
				SummaryReportUtils.tallyBadReadsAsString(value, csBadReadLineLengths);
			}
		}
	
		if ( ! torrentBam) {	
			//ZP - not for torrent bams
			value = (String) record.getAttribute(ZP);
			if (null != value)
				SummaryByCycleUtils.incrementCount(tagZPLineLengths, value);
			//ZF - not for torrent bams
			value = (String) record.getAttribute(ZF);
			if (StringUtils.isNumeric(value)) 	// only care about ints in this tag
				tagZFLineLengths.increment(Integer.parseInt(value));
	
		}

		MAPQMatrix matrix = null;			
		if (includeMatrices) {
			int mapQ = record.getMappingQuality();
			matrix = mapQMatrix.get(mapQ);
			if (null == matrix) {
				MAPQMatrix newMatrix = new MAPQMatrix(); 
				matrix = mapQMatrix.putIfAbsent(mapQ, newMatrix);
				if (null == matrix)
					matrix = newMatrix;
			}
			matrix.addToMatrix(record.getReadLength(), MatrixType.LENGTH);
		}
		
		//CM
		Integer iValue = (Integer) record.getAttribute(CM);
		if (null != iValue) {
			tagCMLineLengths.increment(iValue.intValue());
			//			SummaryByCycleUtils.incrementCount(tagCMLineLengths, iValue);
			if (matrix != null) 
				matrix.addToMatrix(iValue, MatrixType.CM);
		}

		//SM
		Integer sm = (Integer) record.getAttribute(SM);
		if (null != sm) {
			tagSMLineLengths.increment(sm.intValue());
			if (matrix != null) 
				matrix.addToMatrix(sm, MatrixType.SM);
		}
				
		//ZM - not for torrent bams
		if ( ! torrentBam) {	
			value = (String) record.getAttribute(ZM);
			if (null != value) {
				tagZMLineLengths.increment(Integer.parseInt(value));
				if (matrix != null) {
					Integer zm = Integer.valueOf(Integer.parseInt(value));
					if(zm != null)
						matrix.addToMatrix(zm, MatrixType.ZM);					 
					if ( null != zm && null != sm)
						zmSmMatrix.increment(zm, sm);	
				}
			}
		}				
		
		//NH
		iValue = (Integer) record.getAttribute(NH);
		if (null != iValue) {
			tagNHLineLengths.increment(iValue.intValue());
			if (matrix != null) 
				matrix.addToMatrix(iValue, MatrixType.NH);
		}

		//IH
		iValue = (Integer) record.getAttribute(IH);
		if (null != iValue) 
			tagIHLineLengths.increment(iValue.intValue());

		// additionalTags
		if (null != tags)
			for (String s : tags) {
				value = (String) record.getAttribute(s);
				if (null != value)
					SummaryByCycleUtils.incrementCount(additionalTags.get(s), value);
			}
		
		// additionalTagsInt
		if (null != tagsInt)
			for (String s : tagsInt) {
				iValue = (Integer) record.getAttribute(s);
				if (null != iValue)
					additionalIntegerTags.get(s).increment(iValue.intValue());
			}
		
		// additionalTagsChar
		if (null != tagsChar) {
			Character c = null;
			for (String s : tagsChar) {
				c = (Character) record.getAttribute(s);
				if (null != c)
					SummaryByCycleUtils.incrementCount(  additionalCharacterTags.get(s), c );
			}
		}


	}
	
	
	public void toXml(Element bamReportElement){
		Element tagElement = QprofilerXmlUtils.createSubElement(bamReportElement, "report");	//SEQ
		tagElement.setAttribute("Category", "TAG");
		
		
		//MD tag
		Element childElement = QprofilerXmlUtils.createSubElement(tagElement, "subField");	//SEQ
		childElement.setAttribute("Category", "TAG:MD");
		
		//mismatchbycycle
		for(int order = 0; order < 3; order ++)
			tagMDMismatchByCycle[order].toXml(childElement, "mismatch distribution per base cycle", BamSummaryReport2.sourceName[order], "read base cycle");
		
//		//"distribution of mismatches in the MD tag, and the reported base";
//		// TAG-MD-Mismatch
//		Element tagMDElement = QprofilerXmlUtils.createSubElement(tagElement, "MD");			
//		//Element misMatchE = QprofilerXmlUtils.createSubElement(tagMDElement, "MismatchByCycle" );
//		
//		for(int order = 0; order < 3; order ++){ 
//			// SummaryReportUtils.lengthMapToXml(tagMDElement, "AllReads", sourceName[order], allReadsLineLengths[order]);
//			CycleSummaryUtils.toXmlWithPercentage(tagMDMismatchByCycle[order], tagMDElement,"MismatchByCycle", BamSummaryReport2.sourceName[order], allReadsLineLengths[order]  ); 		
//			// TAG-MD ref>alt switch the ints back to Strings
//			for(String strand : new String[]{"MutationForward", "MutationReverse"}){
//				Map<String, AtomicLong> mdRefAltLengthsString = new HashMap<String, AtomicLong>();
//				QCMGAtomicLongArray mdRefAltLengths = (strand.equals("MutationForward"))? mdRefAltLengthsForward[order] : mdRefAltLengthsReverse[order];				
//				for (int m = 0 ; m < mdRefAltLengths.length() ; m++) {
//					long l = mdRefAltLengths.get(m);
//					if (l <= 0)  continue;
//					mdRefAltLengthsString.put(CycleSummaryUtils.getStringFromInt(m), new AtomicLong(l));					 
//				}
//				SummaryReportUtils.lengthMapToXml(tagMDElement, strand, BamSummaryReport2.sourceName[order], mdRefAltLengthsString);
//			}
//		}	
			
 		
		//TAG
 
		//TAG-CS
		childElement = QprofilerXmlUtils.createSubElement(tagElement, "subField");	//SEQ
		childElement.setAttribute("Category", "TAG:CS");		
		if( tagCSByCycle.cycles().size() > 0){
			tagCSByCycle.toXml(childElement, "ColourByCycle", null, "read base cycle");
			SummaryReportUtils.lengthMapToXml(childElement, "LengthTally", tagCSByCycle.getLengthMapFromCycle() );
		//	SummaryReportUtils.lengthMapToXml(tagElement, "BadColoursInReads", csBadReadLineLengths);
		}
		
		
		//TAG-CQ
		childElement = QprofilerXmlUtils.createSubElement(tagElement, "subField");	//SEQ
		childElement.setAttribute("Category", "TAG:CQ");

		if( tagCQByCycle.cycles().size() > 0){
			tagCQByCycle.toXml(childElement, "QualityByCycle", null, "read base cycle");
			//SummaryReportUtils.lengthMapToXml(tagElement, "LengthTally", tagCQByCycle.getLengthMapFromCycle());
			createElement4tag(childElement, "TAG:CQ", "LengthTally", tagCQByCycle.getLengthMapFromCycle());
			//SummaryReportUtils.lengthMapToXml(tagElement, "BadQualsInReads", cqBadReadLineLengths);
			createElement4tag(childElement, "TAG:CQ", "BadQualsInReads", cqBadReadLineLengths);
		}
		
		
		
				
		//TAG-ZM
		createElement4tag(tagElement, "ZM", null,tagZMLineLengths);
		// TAG-ZP
		createElement4tag(tagElement, "ZP", null,tagZPLineLengths);
		// TAG-ZF
		createElement4tag(tagElement, "ZF", null,tagZFLineLengths);
		// TAG-CM
		createElement4tag(tagElement, "CM", null,tagCMLineLengths);
		// TAG-SM
		createElement4tag(tagElement, "SM",null, tagSMLineLengths);
		// TAG-IH
		createElement4tag(tagElement, "IH",null, tagIHLineLengths);
		// TAG-NH
		createElement4tag(tagElement, "NH", null,tagNHLineLengths);
		
		if (includeMatrices)  // ConcurrentMap<Integer, MAPQMatrix> mapQMatrix
			createMatrix(tagElement );
		
		// additional tags
		for (Entry<String,  ConcurrentSkipListMap<String, AtomicLong>> entry : additionalTags.entrySet()) {
			createElement4tag(tagElement, entry.getKey(), null,entry.getValue());
		}
		// additional tagsInt
		for (Entry<String,  QCMGAtomicLongArray> entry : additionalIntegerTags.entrySet()) {
			createElement4tag(tagElement, entry.getKey(),null, entry.getValue());
		}
		// additional tagsChar
		for (Entry<String,  ConcurrentSkipListMap<Character, AtomicLong>> entry : additionalCharacterTags.entrySet()) {
			createElement4tag(tagElement, entry.getKey(),null, entry.getValue());
		}	
				 
		//TAG-RG
		createElement4tag(tagElement, "RG",null, tagRGLineLengths);
		

		/**
		 * 	private final ConcurrentMap<String, ReadIDSummary> readIdSummary = new ConcurrentHashMap<String, ReadIDSummary>();
	private final ConcurrentMap<String, AtomicLong> inValidReadIds = new ConcurrentHashMap<String, AtomicLong>();

		 */
	}
	
	private  <T> void createElement4tag(Element parent, String tagName, String source, Map<T, AtomicLong> map) {
		List<Element> reports = QprofilerXmlUtils.getChildElementByTagName(parent, "report");
		Element current = null;
		for(Element ele : reports)  
			if(ele.getAttribute("Category").equals("TAG:"+ tagName)) {
				current = ele;
				break;
			}
		 		
		if(current == null) {
			current = QprofilerXmlUtils.createSubElement(parent, "report");
			current.setAttribute("Category", "TAG:"+ tagName);
		}
			 
		
		SummaryReportUtils.lengthMapToXml(current, "RG",source, map, null);
	}
	
	private void createElement4tag(Element parent, String tagName,String source, QCMGAtomicLongArray array) {
	
		  				 
		Map<Integer, AtomicLong> map = new TreeMap<Integer, AtomicLong>();
		for (int i = 0, length = (int) array.length() ; i < length ; i++) {
			long count = array.get(i);					 			
			if (count > 0)	map.put( i, new AtomicLong(count) );
		}	
			
		createElement4tag(parent, tagName, source,  map);		
	}
	
	

	private void setupAdditionalTagMaps(){
		if (null != tags) {
			for (String tag : tags)
				additionalTags.put(tag, new ConcurrentSkipListMap<String, AtomicLong>());
		}
		if (null != tagsInt) {
			for (String tagInt : tagsInt)
				additionalIntegerTags.put(tagInt, new QCMGAtomicLongArray((100)));
		}
		if (null != tagsChar) {
			for (String tagChar : tagsChar)
				additionalCharacterTags.put(tagChar,  new ConcurrentSkipListMap<Character, AtomicLong>());
		}
	}	

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
		zmSmMatrix.toXml(parent, "ZmSmMatrix");
	//	XmlUtils.outputSet(parent,"not sure", "matrix is included", "ZmSmMatrix", "not sure", cmMatrix);
		
	}	
	
	public ConcurrentMap<String, ReadIDSummary>  getReadIDSummary(){	return readIdSummary;	}
	
}
