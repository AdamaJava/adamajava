/**
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
*/
package au.edu.qimr.qannotate.modes;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.meta.QExec;
import org.qcmg.common.model.ChrPointPosition;
import org.qcmg.common.model.ChrPosition;
import org.qcmg.common.model.ChrPositionComparator;
import org.qcmg.common.model.ChrRangePosition;
import org.qcmg.common.util.ChrPositionUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.vcf.VcfRecord;
import org.qcmg.common.vcf.header.VcfHeader;
import org.qcmg.common.vcf.header.VcfHeaderRecord;
import org.qcmg.common.vcf.header.VcfHeaderUtils;
import org.qcmg.vcf.VCFFileReader;
import org.qcmg.vcf.VCFFileWriter;

import au.edu.qimr.qannotate.Main;

abstract class AbstractMode {
	public static final String DATE_FORMAT_STRING = "yyyyMMdd";
	
	final Map<ChrPosition,List<VcfRecord>> positionRecordMap = new HashMap<>();
	VcfHeader header = null;

	private final static QLogger logger = QLoggerFactory.getLogger(AbstractMode.class);	
	
	/**
	 * read variants from input into RAM hash map
	 * @param f is input vcf file 
	 * @throws IOException
	 */
	protected void loadVcfRecordsFromFile(File f, boolean isStringent) throws IOException {
		
        //read record into RAM, meanwhile wipe off the ID field value;
        try (VCFFileReader reader = new VCFFileReader(f)) {
        	header = reader.getHeader();
        	//no chr in front of position
			for (final VcfRecord vcf : reader) {
				ChrPosition pos = cloneIfLenient(vcf.getChrPosition(), isStringent) ;				
				//used converted  chr name as key but vcf is kept as original 
				positionRecordMap.computeIfAbsent(pos, function -> new ArrayList<VcfRecord>(2)).add(vcf);
			}
		}
        logger.info("loaded " + positionRecordMap.size() + " vcf entries from " + f.getAbsolutePath());        
	}

	abstract void addAnnotation(String dbfile) throws Exception;
	
	/**
	 * add annotate variants from RAM hash map intp the output file
	 * @param outputFile is output vcf file
	 * @throws IOException
	 */
	void writeVCF(File outputFile ) throws IOException {		 
		logger.info("creating VCF output...");	 		
		final List<ChrPosition> orderedList = new ArrayList<>(positionRecordMap.keySet());
		orderedList.sort( ChrPositionComparator.getCPComparatorForGRCh37());
		
		try(VCFFileWriter writer = new VCFFileWriter( outputFile)) {
			for(final VcfHeaderRecord record: header)  {
				writer.addHeader(record.toString());
			}
			long count = 0; 
			for (final ChrPosition position : orderedList)  
				for(  VcfRecord record : positionRecordMap.get(position) ){				
					writer.add( record );	
					count ++;
				}
			logger.info(String.format("outputed %d VCF record, happend on %d variants location.",  count , orderedList.size()));
		}  
	}
	
	 VcfHeader reheader(VcfHeader header, String cmd, String inputVcfName) {	
		 
		VcfHeader myHeader = header;  	
 		
		String version = Main.class.getPackage().getImplementationVersion();
		String pg = Main.class.getPackage().getImplementationTitle();
		final String fileDate = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
		final String uuid = QExec.createUUid();
		
		myHeader.addOrReplace(VcfHeaderUtils.CURRENT_FILE_FORMAT);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_FILE_DATE + "=" + fileDate);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_UUID_LINE + "=" + uuid);
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_SOURCE_LINE + "=" + pg+"-"+version);	
			
		String inputUuid = (myHeader.getUUID() == null)? null:  myHeader.getUUID().getMetaValue(); //new VcfHeaderUtils.SplitMetaRecord(myHeader.getUUID()).getValue();   
		myHeader.addOrReplace(VcfHeaderUtils.STANDARD_INPUT_LINE + "=" + inputUuid + ":"+ inputVcfName);
		
		if(version == null) version = Constants.NULL_STRING_UPPER_CASE;
	    if(pg == null ) pg = Constants.NULL_STRING_UPPER_CASE;
	    if(cmd == null) cmd = Constants.NULL_STRING_UPPER_CASE;
		VcfHeaderUtils.addQPGLineToHeader(myHeader, pg, version, cmd);
		
		return myHeader;			
	}
	

	/**
	 * 
	 * @param cmd is the command line string which will be added into vcf header
	 * @param inputVcfName: add input file name into vcf header
	 * @throws IOException
	 */
	void reheader(String cmd, String inputVcfName) throws IOException {	

		if(header == null)
	        try(VCFFileReader reader = new VCFFileReader(inputVcfName)) {
	        	header = reader.getHeader();	
	        	if(header == null)
	        		throw new IOException("can't receive header from vcf file, maybe wrong format: " + inputVcfName);
	        } 	
 
		header = reheader(header, cmd, inputVcfName);			
	}

	protected ChrPosition cloneIfLenient(ChrPosition cp, boolean isStringent ) {
		if( isStringent ) return cp; 
		
		String newChr = getFullChromosome(  cp.getChromosome() );
		if ( ! newChr.equals(cp.getChromosome())) {				
			if (cp instanceof ChrPointPosition) {
				return ChrPointPosition.valueOf(newChr, cp.getStartPosition());
			} else if  (cp instanceof ChrRangePosition) {
				return new ChrRangePosition(newChr, cp.getStartPosition(), cp.getEndPosition());
			} else {
				throw new UnsupportedOperationException("cloneWithNewName not yet implemented for any types other than ChrPointPosition and ChrRangePosition!!!");
			}							 
		}	
		
		return cp;		
	}
	
	
	/**
	 * Takes a string representation of a contig and updates it if necessary (ie. if ref is equal to X,Y,M,MT, or an integer less than 23 and greater than 0).
	 *  
	 *  It used to be the case that is the supplied ref was equal to "chrMT", the returned value would be "chrM", due to GRCh38 uses "chrM". 
	 *  This was useful when different versions of the human genome (ie. GRCh37/b37 and Hg19) had different values for the mitochondrial genome.
	 * 
	 * @param ref
	 * @return an updated ref name
	 */
	 protected String getFullChromosome(String ref) {
		if (ref == null ) return null; //stop exception
		
		String refName = ref.toLowerCase();
		
		if( refName.equals("m") || refName.equals("mt")|| refName.equals("chrm")|| refName.equals("chrmt")) {
			return Constants.CHR + "M";
		}
		
		if (refName.equals("x") || refName.equals("y") ) {
			return Constants.CHR + ref.toUpperCase();
		}
				
        if (refName.startsWith(Constants.CHR)) {
        	return Constants.CHR + refName.substring(3).toUpperCase();
        }
		
			
		/*
		 * If ref is an integer less than 23, slap "chr" in front of it
		 */
		if (Character.isDigit(ref.charAt(0))) {
			try {
				int refInt = Integer.parseInt(ref);
				if (refInt < 23 && refInt > 0) {
					return Constants.CHR + ref;
				}
			} catch (NumberFormatException nfe) {
				// don't do anything here - will return the original reference
			}
		}
		
		return ref;
	}

	
}
