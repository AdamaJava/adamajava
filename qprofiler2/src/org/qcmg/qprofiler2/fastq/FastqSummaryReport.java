/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 * © Copyright QIMR Berghofer Medical Research Institute 2014.  This code is released under the terms outlined in the included LICENSE file.
 */
/**
 * All source code distributed as part of the AdamaJava project is released
 * under the GNU GENERAL PUBLIC LICENSE Version 3, a copy of which is
 * included in this distribution as gplv3.txt.
 */
package org.qcmg.qprofiler2.fastq;


import java.util.concurrent.atomic.AtomicLong;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.SAMUtils;
import org.qcmg.common.model.ProfileType;
import org.qcmg.common.model.QCMGAtomicLongArray;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.QprofilerXmlUtils;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.qcmg.qprofiler2.summarise.KmersSummary;
import org.qcmg.qprofiler2.summarise.ReadIDSummary;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class FastqSummaryReport extends SummaryReport {
	public final static String badBasePerRead = QprofilerXmlUtils.badBase + "PerRead";
	public final static String badBaseComment = "read with bad base(qual score < 10) distribution";
	
	private static final Character c = Character.MAX_VALUE;
	private static final Integer i = Integer.MAX_VALUE;
	
	private static final int ReportErrNumber = 100;
	AtomicLong errNumber = new AtomicLong();	
	
	//SEQ
	private final CycleSummary<Character> seqByCycle = new CycleSummary<Character>(c, 512);
//	private Map<Integer, AtomicLong> seqLineLengths = null;
	private final QCMGAtomicLongArray seqBadReadLineLengths = new QCMGAtomicLongArray(128);
	private final KmersSummary kmersSummary = new KmersSummary( KmersSummary.maxKmers ); //default use biggest mers length
	
	//QUAL
	private final CycleSummary<Integer> qualByCycleInteger = new CycleSummary<Integer>(i, 512);
	private final QCMGAtomicLongArray qualBadReadLineLengths = new QCMGAtomicLongArray(128);
		
	AtomicLong qualHeaderNotEqualToPlus = new AtomicLong();		
	private ReadIDSummary readHeaderSummary = new ReadIDSummary(); 
	
	public FastqSummaryReport() { super(); }
	public ReadIDSummary getReadIDSummary(){	return readHeaderSummary;	}
	@Override
	public void toXml(Element parent1) {		
		Element parent = init( parent1, ProfileType.FASTQ, null, null );
		parent = QprofilerXmlUtils.createSubElement( parent, ProfileType.FASTQ.getReportName() +  XmlUtils.metrics   );
		
		//header line:"analysis read name pattern for read group
		Element element =   QprofilerXmlUtils.createSubElement(parent, QprofilerXmlUtils.qname ) ;				
		readHeaderSummary.toXml(element );		

		//seq		
		final String seqBaseCycle = QprofilerXmlUtils.seqBase + QprofilerXmlUtils.cycle; 						 			
		element =   QprofilerXmlUtils.createSubElement(parent,QprofilerXmlUtils.seq  ) ;//QprofilerXmlUtils.createSubElement(parent, "SequenceData" );	 
		seqByCycle.toXml( element, QprofilerXmlUtils.seqBase, null, seqBaseCycle );	
		
		Element ele = XmlUtils.createMetricsNode( element, QprofilerXmlUtils.seqLength ,  null); 
		XmlUtils.outputTallyGroup( ele, QprofilerXmlUtils.seqLength, seqByCycle.getLengthMapFromCycle(), true );	
		
		ele = XmlUtils.createMetricsNode( element, QprofilerXmlUtils.badBase, null);
		XmlUtils.outputTallyGroup( ele, seqBaseCycle,   seqBadReadLineLengths.toMap(), true );	
		XmlUtils.addCommentChild(ele, "bad base(. or N) distribution" );
		
		//1mers is same to baseByCycle
		for( int i : new int[] { 2, 3, KmersSummary.maxKmers } )
			kmersSummary.toXml(element,i);	
				
		//QUAL
		final String qualBaseCycle = QprofilerXmlUtils.qualBase + QprofilerXmlUtils.cycle ; 	
		element =   QprofilerXmlUtils.createSubElement(parent, QprofilerXmlUtils.qual) ;
		qualByCycleInteger.toXml(element,QprofilerXmlUtils.qualBase ,null,   qualBaseCycle) ;
		ele = XmlUtils.createMetricsNode( element, QprofilerXmlUtils.qualLength,null) ;
		XmlUtils.outputTallyGroup( ele,  QprofilerXmlUtils.qualLength,  qualByCycleInteger.getLengthMapFromCycle(), true ) ;	
		ele = XmlUtils.createMetricsNode( element,  QprofilerXmlUtils.badBase, null) ;
		XmlUtils.outputTallyGroup( ele,  badBasePerRead ,  qualBadReadLineLengths.toMap(), false ) ;
		XmlUtils.addCommentChild(ele, badBaseComment );
		
		
 	}
	
	/**
	 * Reads a row from the text file and returns it as a string
	 * 
	 * @return next row in file
	 */
	public void parseRecord(FastqRecord record) {
		if( null == record ) return;
		 			
		updateRecordsParsed();
					 
		// QUAL   it also throw exception if fastq reads is invalid
		byte[] baseQualities = SAMUtils.fastqToPhred( record.getBaseQualityString() );
		
		//read are raw sequence ignore the strand
		qualByCycleInteger.parseByteData( baseQualities );
		SummaryReportUtils.tallyQualScores( baseQualities, qualBadReadLineLengths );
						
		// SEQ
		byte[] readBases = record.getReadString().getBytes();
		seqByCycle.parseByteData(readBases);
		SummaryReportUtils.tallyBadReadsAsString( readBases, seqBadReadLineLengths );
		//fastq base are all orignal forward, treat all as first of pair 
		kmersSummary.parseKmers( readBases, false, 0 ); 
		
		String qualHeader = record.getBaseQualityHeader();			
		// If header just contains "+" then FastqRecord has null for qual header
		if ( ! StringUtils.isNullOrEmpty( qualHeader ))  
			qualHeaderNotEqualToPlus.incrementAndGet();	
		
		String id = record.getReadName();//record.getReadHeader();		
		try { readHeaderSummary.parseReadId( id );
		} catch (Exception e) {
			if ( errNumber.incrementAndGet() < ReportErrNumber )
				logger.error( "Invalid read id: " + id );
		}		 						 			 	 
	}
}
