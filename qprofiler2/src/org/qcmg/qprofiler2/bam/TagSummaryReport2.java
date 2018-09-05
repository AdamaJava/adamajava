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

	public final static int additionTagMapLimit = 200;
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

	
	/**
	 * default torrentBam is false, unless this method is called
	 */
	public void setTorrentBam(){   }
	public void setInclMatrices(){  }
	
	public void parseTAGs(final SAMRecord record )  {
		byte[] readBases = record.getReadBases();
		boolean reverseStrand =record.getReadNegativeStrandFlag();		
				
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
					if(map.size() < additionTagMapLimit)
						map.computeIfAbsent(record.getCharacterAttribute(tag.tag), k-> new AtomicLong()).incrementAndGet();
					else
						map.computeIfAbsent((char)-128, k-> new AtomicLong()).incrementAndGet();
				}else if(record.getAttribute(tag.tag) instanceof String) {					
					Map<String, AtomicLong> map = additionalTags.computeIfAbsent(tag.tag, k-> new ConcurrentSkipListMap<String, AtomicLong>());
					if(map.size() < additionTagMapLimit)
						map.computeIfAbsent(record.getStringAttribute(tag.tag), k-> new AtomicLong()).incrementAndGet();
					else
						map.computeIfAbsent("others", k-> new AtomicLong()).incrementAndGet();
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
				XmlUtils.outputMap(childElement, "base counts", String.format("%s base distribution per mutation value on %s", strand, BamSummaryReport2.sourceName[order]), 
						strand+BamSummaryReport2.sourceName[order], "mutation value", mdRefAltLengthsString);
			}		
		}
		// additional tags includes RG
		for (Entry<String,  ConcurrentSkipListMap<String, AtomicLong>> entry : additionalTags.entrySet()) {
			XmlUtils.outputMap(parent,"read counts","read distribution",  "tags:" + entry.getKey(), "tag value",entry.getValue());
		}
		// additional tagsInt
		for (Entry<String,  QCMGAtomicLongArray> entry : additionalIntegerTags.entrySet()) {
			XmlUtils.outputMap(parent,"counts","read distribution",  "tagInt:" + entry.getKey(),"tag value", entry.getValue());
		}
		// additional tagsChar
		for (Entry<String,  ConcurrentSkipListMap<Character, AtomicLong>> entry : additionalCharacterTags.entrySet()) {
			XmlUtils.outputMap(parent,"counts","read distribution",  "tagChar:" + entry.getKey(),"tag value", entry.getValue());
		}			
	}
	
	private void createMatrix(Element parent ){
		
		Map<MAPQMiniMatrix, AtomicLong> cmMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> smMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> lengthMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> nhMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();
		Map<MAPQMiniMatrix, AtomicLong> zmMatrix = new TreeMap<MAPQMiniMatrix, AtomicLong>();

		logger.debug("cmMatrix(): " + cmMatrix.size());
		logger.debug("smMatrix(): " + smMatrix.size());
		logger.debug("lengthMatrix(): " + lengthMatrix.size());
		logger.debug("nhMatrix(): " + nhMatrix.size());
		logger.debug("zmMatrix(): " + zmMatrix.size());
		
		XmlUtils.outputMap(parent,"not sure", "matrix is included", "MAPQMatrixCM", "not sure", cmMatrix);
		XmlUtils.outputMap(parent,"not sure", "matrix is included", "MAPQMatrixSM", "not sure", cmMatrix);
		XmlUtils.outputMap(parent,"not sure", "matrix is included", "MAPQMatrixLength", "not sure", cmMatrix);
		XmlUtils.outputMap(parent,"not sure", "matrix is included", "MAPQMatrixCM", "not sure", cmMatrix);
		XmlUtils.outputMap(parent,"not sure", "matrix is included", "MAPQMatrixZM", "not sure", cmMatrix);
		
		parent = QprofilerXmlUtils.createSubElement( parent, "ZmSmMatrix" );
		XmlUtils.outputMap(parent,"not sure", "matrix is included", "MAPQMatrixZM", "not sure", zmMatrix);		
	}	
	
//	public ConcurrentMap<String, ReadIDSummary>  getReadIDSummary(){	return readIdSummary;	}
	
}
