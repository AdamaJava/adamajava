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
import org.qcmg.common.util.Pair;
import org.qcmg.common.util.XmlElementUtils;
import org.qcmg.qprofiler2.report.SummaryReport;
import org.qcmg.qprofiler2.summarise.CycleSummary;
import org.qcmg.qprofiler2.summarise.KmersSummary;
import org.qcmg.qprofiler2.summarise.ReadGroupSummary;
import org.qcmg.qprofiler2.summarise.ReadIDSummary;
import org.qcmg.qprofiler2.util.SummaryReportUtils;
import org.qcmg.qprofiler2.util.XmlUtils;
import org.w3c.dom.Element;

public class FastqSummaryReport extends SummaryReport {
	public final static String badBaseComment = " the number of reads containing a given number of bad bases (. or N) ";	
	public final static String badQualComment = "the number of reads containing a given number of bases with bad quality scores (<10)";
	
	private static final Character c = Character.MAX_VALUE;
	private static final Integer i = Integer.MAX_VALUE;	
	private static final int ReportErrNumber = 100;
	AtomicLong errNumber = new AtomicLong();	
	
	//SEQ
	private final CycleSummary<Character> seqByCycle = new CycleSummary<Character>(c, 512);
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
		parent = XmlElementUtils.createSubElement( parent, ProfileType.FASTQ.getReportName() +  XmlUtils.METRICS   );
		
		//header line:"analysis read name pattern for read group
		Element element =   XmlElementUtils.createSubElement(parent, XmlUtils.QNAME ) ;							
		readHeaderSummary.toXml(element );		
				

		//seq								 			
		element =   XmlElementUtils.createSubElement(parent,XmlUtils.SEQ  ) ;
		long counts = 0;
		for(int order = 0; order < 3; order++) {
			counts += seqByCycle.getInputCounts();
		}
		Pair rcPair = new Pair<String, Number>(ReadGroupSummary.READ_COUNT, counts);
		Element ele = XmlUtils.createMetricsNode( element, XmlUtils.SEQ_BASE , rcPair); 
		seqByCycle.toXml( ele, XmlUtils.SEQ_BASE);	
		
		ele = XmlUtils.createMetricsNode( element, XmlUtils.SEQ_LENGTH , rcPair); 
		XmlUtils.outputTallyGroup( ele, XmlUtils.SEQ_LENGTH, seqByCycle.getLengthMapFromCycle(), true, true );	
		
		
		counts = 0;
		for(int order = 0; order < 3; order++) {
			counts += seqBadReadLineLengths.getSum();	
		}
		rcPair = new Pair<String, Number>(ReadGroupSummary.READ_COUNT, counts);		
		ele = XmlUtils.createMetricsNode( element, XmlUtils.BAD_READ, rcPair);
		XmlUtils.outputTallyGroup( ele, XmlUtils.BAD_READ,   seqBadReadLineLengths.toMap(), true, true );	
		XmlUtils.addCommentChild(ele, FastqSummaryReport.badBaseComment );
		
		//1mers is same to baseByCycle
		for( int i : new int[] { 2, 3, KmersSummary.maxKmers } ) {
			kmersSummary.toXml( element,i );	
		}
		
		//QUAL
		element = XmlElementUtils.createSubElement(parent, XmlUtils.QUAL) ;
		ele = XmlUtils.createMetricsNode( element, XmlUtils.QUAL_BASE , null); 
		qualByCycleInteger.toXml(element,XmlUtils.QUAL_BASE) ;
		
		ele = XmlUtils.createMetricsNode( element, XmlUtils.QUAL_LENGTH, null) ;
		XmlUtils.outputTallyGroup( ele,  XmlUtils.QUAL_LENGTH,  qualByCycleInteger.getLengthMapFromCycle(), true, true ) ;	
		
		ele = XmlUtils.createMetricsNode( element,  XmlUtils.BAD_READ, null) ;
		XmlUtils.outputTallyGroup( ele,  XmlUtils.BAD_READ ,  qualBadReadLineLengths.toMap(), false, true ) ;
		XmlUtils.addCommentChild(ele, FastqSummaryReport.badQualComment );
			
 	}
	
	/**
	 * Reads a row from the text file and returns it as a string
	 * 
	 * @return next row in file
	 */
	public void parseRecord(FastqRecord record) {
		if( null == record ) return;
		 			
		updateRecordsInputed();
					 
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
