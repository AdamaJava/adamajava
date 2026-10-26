/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.picard;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import htsjdk.samtools.BAMFileReader;
import htsjdk.samtools.BamFileIoUtils;
import htsjdk.samtools.SamFiles;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SamReaderFactory.Option;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.util.BlockCompressedInputStream;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.protocol.s3.S3AwareURLStreamHandlerFactory;

public class SAMFileReaderFactory {
//	EnumSet<Option> defultOption = SamReaderFactory.Option.DEFAULTS;
	static ValidationStringency DefaultStringency = ValidationStringency.SILENT;
	static {
		// this factory makes a handler that provides openConnection(...) on s3: URLs
		// this can only be called once for each JVM, hence the static block
		URL.setURLStreamHandlerFactory(new S3AwareURLStreamHandlerFactory());
	}
	
	private static final QLogger logger = QLoggerFactory.getLogger(SAMFileReaderFactory.class);
	
	/**
	 * 
	 * @param bamFile is input file with CRAM, BAM or SAM format; it is compulsory. 
	 * @param indexFile is the index file of input, set to null if don't provide. 
	 * @param reference is a genome fastq file, it is compulsory when open CRAM input. 
	 * @param stringency is the strict level to read the inut file, the default level is SILENT. 
	 * @param options refer to Enum SamReaderFactory.Option
	 * @return File reader of CRAM, BAM or SAM. 
	 * @throws IOException
	 */
	public static SamReader createSAMFileReaderAsStream(final File bamFile, final File indexFile, final File reference, 
			final ValidationStringency stringency, final Option...options ) throws IOException {				
		return createSAMFileReaderAsStream(bamFile, indexFile, reference, stringency, true, options);
	}

	/**
	 * Opens a file for reading, optionally without an index.
	 * <p>
	 * Parsing a BAM index can be expensive for large files, so this method allows the index
	 * to be omitted when the caller has already parsed it, or does not need it at all.
	 *
	 * @param bamFile is input file with CRAM, BAM or SAM format; it is compulsory.
	 * @param indexFile is the index file of input, set to null if don't provide.
	 * @param reference is the genome fastq file, it is compulsory when open CRAM input.
	 * @param stringency is the strict level to read the inut file, the default level is SILENT.
	 * @param useIndex when false no index will be opened, even if one can be found for the input file.
	 * @param options refer to Enum SamReaderFactory.Option
	 * @return File reader of CRAM, BAM or SAM.
	 * @throws IOException
	 */
	private static SamReader createSAMFileReaderAsStream(final File bamFile, final File indexFile, final File reference,
			final ValidationStringency stringency, final boolean useIndex, final Option...options ) throws IOException {				
				
		if (null == bamFile) throw new IllegalArgumentException("Please provide a bam file");
				
		SamReaderFactory factory =   ( options != null && options.length > 0) ? 
				SamReaderFactory.makeDefault().enable(options) : SamReaderFactory.makeDefault();	
				
		factory.validationStringency(null != stringency ? stringency : DefaultStringency);
		logger.debug("Setting Validation Stringency on SamReader to: " + factory.validationStringency().name());			
		
		//check to see if we have a BAM/CRAM or SAM file, which will determine which input stream is created		 
		SamInputResource resources;
		//process BAM/CRAM
		if (BamFileIoUtils.isBamFile(bamFile) || SamReader.Type.CRAM_TYPE.hasValidFileExtension(bamFile.getName()) ) {
			SeekableStreamFactory.setInstance(new S3AwareSeekableStreamFactory());
			SeekableStream is = SeekableStreamFactory.getInstance().getStreamFor(bamFile.getAbsolutePath());	  
			resources = SamInputResource.of(is);
			
			File index = null == indexFile ? (useIndex ? SamFiles.findIndex(bamFile) : null) : indexFile;
			if (null != index) {
				SeekableStream indexStream = SeekableStreamFactory.getInstance().getStreamFor(index.getAbsolutePath());
				resources = resources.index(indexStream);
				logger.info("setup index: " + index.getAbsolutePath());
			}
			//set reference file for cram. default reference set by java option "-Dsamjdk.REFERENCE_FASTA" 
			if (reference != null && reference.exists()) {
				factory.referenceSequence(reference); 
				logger.info("setup reference: " + reference.getAbsolutePath());
			}			
		} else {
			InputStream is = new FileInputStream(bamFile);
			resources = SamInputResource.of(is);
		}
	
		return factory.open(resources);
	}	

	/**
	 * Opens a BAM file for reading without an index.
	 * <p>
	 * This is useful when the index has already been parsed by the caller, as parsing a
	 * BAM index can be expensive for large files.
	 *
	 * @param bamFile point to a BAM/SAM file
	 * @param validation the validation stringency to use
	 * @return SamReader
	 * @throws IOException
	 */
	public static SamReader createSAMFileReaderWithoutIndex(final File bamFile, final String validation) throws IOException {
		ValidationStringency stringency = DefaultStringency;
		if ("lenient".equalsIgnoreCase(validation)) {
			stringency = ValidationStringency.LENIENT;
		} else if ("strict".equalsIgnoreCase(validation)) {
			stringency = ValidationStringency.STRICT;
		}

		return createSAMFileReaderAsStream(bamFile, null, null, stringency, false);
	}
	
	/**
	 * Opens a BAM file for reading, without parsing its header.
	 * <p>
	 * The returned stream reads the contents of the BAM file and supports seeking to the
	 * virtual file pointers that are recorded in a BAM index. It is used by those tools
	 * that read the records of a BAM file themselves, rather than using htsjdk's
	 * BAMFileReader.
	 *
	 * @param bamFile point to a BAM file
	 * @return a stream that reads the contents of the BAM file
	 * @throws IOException
	 */
	public static BlockCompressedInputStream createBlockCompressedInputStream(final File bamFile) throws IOException {
		if (null == bamFile) throw new IllegalArgumentException("Please provide a bam file");

		SeekableStreamFactory.setInstance(new S3AwareSeekableStreamFactory());
		SeekableStream is = SeekableStreamFactory.getInstance().getStreamFor(bamFile.getAbsolutePath());
		return new BlockCompressedInputStream(is);
	}

	/**
	 * Returns the BAMFileReader that is used by the given reader, or null if the reader is
	 * not backed by a BAMFileReader.
	 * <p>
	 * The readers that are created by htsjdk's SamReaderFactory are wrappers around the
	 * underlying implementation, so the wrapper has to be unwrapped before the
	 * BAMFileReader specific methods can be used.
	 *
	 * @param reader a reader, typically created by this factory
	 * @return the underlying BAMFileReader, or null if there isn't one
	 */
	public static BAMFileReader getBAMFileReader(final SamReader reader) {
		if (reader instanceof BAMFileReader) {
			return (BAMFileReader) reader;
		}
		if (reader instanceof SamReader.PrimitiveSamReaderToSamReaderAdapter) {
			final SamReader.PrimitiveSamReader primitiveReader =
					((SamReader.PrimitiveSamReaderToSamReaderAdapter) reader).underlyingReader();
			if (primitiveReader instanceof BAMFileReader) {
				return (BAMFileReader) primitiveReader;
			}
		}
		return null;
	}
	
	/**
	 * It open a BAM file and 
	 * @param bamFile point to a BAM/SAM file
	 * @return SamReader
	 * @throws IOException
	 */
	public static SamReader createSAMFileReader(final File bamFile) throws IOException {
		return createSAMFileReaderAsStream(bamFile, null, null, null) ;
	}
	
	public static SamReader createSAMFileReader(final File bamFile, final File index) throws IOException {
		return createSAMFileReaderAsStream(bamFile, index, null, null) ;
	}

	public static SamReader createSAMFileReader(File bamFile, final File index, ValidationStringency validation) throws IOException {
		return createSAMFileReaderAsStream(bamFile, index, null, validation) ;
	}

	public static SamReader createSAMFileReader(File bamFile,  String validation) throws IOException {
		ValidationStringency stringency = DefaultStringency;
		if ("lenient".equalsIgnoreCase(validation)) {
			stringency = ValidationStringency.LENIENT;
		} else if ("strict".equalsIgnoreCase(validation)) {
			stringency = ValidationStringency.STRICT;
		} 	
		
		return createSAMFileReaderAsStream(bamFile, null, null, stringency);
	}

	public static SamReader createSAMFileReaderAsStream(String input, String index, ValidationStringency vs) throws IOException {
		File findex = (index == null)? null: new File(index);		 
		return createSAMFileReader(new File(input), findex, vs);
	}

	public static SamReader createSAMFileReader(final File bamFile, final SamReaderFactory.Option... options) throws IOException {
		return createSAMFileReaderAsStream(bamFile, null, null, null,  options);
	}
	
	
	//debug for qsignature only
	public static SamReader createSAMFileReaderSig(File bamFile) {
		return createSAMFileReaderSig(bamFile, null); 
	}

	public static SamReader createSAMFileReaderSig(File bamFile, String validationStringency) {
		
		ValidationStringency stringency = DefaultStringency;
		if ("lenient".equalsIgnoreCase(validationStringency)) {
			stringency = ValidationStringency.LENIENT;
		} else if ("strict".equalsIgnoreCase(validationStringency)) {
			stringency = ValidationStringency.STRICT;
		} 	

		SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault();		
		samReaderFactory.validationStringency(stringency);
		SamInputResource resource = SamInputResource.of(bamFile);			
		return samReaderFactory.open(resource);
	}	
	
}
