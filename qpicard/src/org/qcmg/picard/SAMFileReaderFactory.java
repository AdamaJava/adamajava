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

import htsjdk.samtools.BamFileIoUtils;
import htsjdk.samtools.SamFiles;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.SamReaderFactory.Option;
import htsjdk.samtools.seekablestream.SeekableStream;
import htsjdk.samtools.seekablestream.SeekableStreamFactory;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
//import org.qcmg.picard.aws.QSeekableStreamFactory;

import vcfstore.htsjdk.S3AwareSeekableStreamFactory;
import vcfstore.protocol.s3.S3AwareURLStreamHandlerFactory;

public class SAMFileReaderFactory {
//	EnumSet<Option> defultOption = SamReaderFactory.Option.DEFAULTS;
	static ValidationStringency DefaultStringency = ValidationStringency.SILENT;
	
	private static final QLogger logger = QLoggerFactory.getLogger(SAMFileReaderFactory.class);
	
	/**
	 * it read the bame with  SILENT validation stringency, without any SamReaderFactory.Option and with default index
	 * @param bamFile
	 * @return
	 */
	public static SamReader createSAMFileReader(final File bamFile) {
		return createSAMFileReader(bamFile,null, DefaultStringency, new Option[0]);
	}
	
	/**
	 * it read the bame with  SILENT validation stringency, without any SamReaderFactory.Option and with specified index
	 * @param bamFile
	 * @return
	 */
	public static SamReader createSAMFileReader(final File bamFile, final File indexFile) { 	
		return createSAMFileReader(bamFile, indexFile, DefaultStringency, new Option[0]);
	}
	
	public static SamReader createSAMFileReader(final File bamFile, final String stringency) {
				 
			if ("lenient".equalsIgnoreCase(stringency)) {
				return createSAMFileReader(bamFile,null, ValidationStringency.LENIENT, new Option[0]);
			} else if ("strict".equalsIgnoreCase(stringency)) {
				return createSAMFileReader(bamFile,null, ValidationStringency.STRICT, new Option[0]);
			} 
			
			return createSAMFileReader(bamFile,null, DefaultStringency, new Option[0]);
			 
	}
	
	
	/**
	 * Creates and returns a SamReader instance based on the supplied bam file, and the supplied Validation Stringency.
	 * If the stringency is null, calls {@link #createSAMFileReader(File)} which will examine the bam file header
	 * @param bamFile
	 * @param stringency
	 * @return SamReader reader for this bam file with this validation stringency
	 */
	public static SamReader createSAMFileReader(final File bamFile, final ValidationStringency stringency) {
		
		 return createSAMFileReader(bamFile, null, stringency, new Option[0]);
	}
	
	/**
	 * Creates and returns a SamReader instance based on the supplied bam file, and the supplied Validation Stringency.
	 * If the stringency is null, calls {@link #createSAMFileReader(File)} which will examine the bam file header
	 * @param bamFile
	 * @param stringency
	 * @return SamReader reader for this bam file with this validation stringency
	 */
	public static SamReader createSAMFileReader(final File bamFile, final File indexFile, final ValidationStringency stringency) {
		return createSAMFileReader(bamFile, indexFile, stringency, new Option[0]);//SamReaderFactory.Option.values());   //Option.DEFAULTS.toArray());
	}
	
	public static SamReader createSAMFileReader(final File bamFile, final SamReaderFactory.Option... options) {
		return createSAMFileReader(bamFile, null, DefaultStringency, options);
	}
	
	
	public static SamReader createSAMFileReader(final File bamFile, final File indexFile,  final ValidationStringency stringency, final Option...options ) {				

		SamReaderFactory samReaderFactory = SamReaderFactory.makeDefault();		
		
		if( options != null && options.length > 0)
			samReaderFactory = SamReaderFactory.makeDefault().enable(options);
		
		
		// only apply validationStringency(stringency) rather than static method setDefaultValidationStringency(stringency) for instance don't apply 
		//you can only call the static method before create the instanct 
		if (null != stringency) 
			samReaderFactory.validationStringency(stringency);
		else
			samReaderFactory.validationStringency(DefaultStringency);		 
		logger.debug("Setting Validation Stringency on SamReader to: " + samReaderFactory.validationStringency().name());	
			
		
		SamInputResource resource = SamInputResource.of(bamFile);
		if(indexFile != null)
			resource = SamInputResource.of(bamFile).index(indexFile);
		
		return samReaderFactory.open(resource);
	}
	
	public static SamReader createSAMFileReaderAsStream(final File bamFile) throws IOException {
		return createSAMFileReaderAsStream(bamFile, SamFiles.findIndex(bamFile), DefaultStringency);
	}
	public static SamReader createSAMFileReaderAsStream(final File bamFile, final ValidationStringency stringency) throws IOException {
		return createSAMFileReaderAsStream(bamFile, SamFiles.findIndex(bamFile), stringency);
	}
	public static SamReader createSAMFileReaderAsStream(final File bamFile, File indexFile) throws IOException {
		return createSAMFileReaderAsStream(bamFile, bamFile, DefaultStringency);
	}
	public static SamReader createSAMFileReaderAsStream(final File bamFile, File indexFile, final ValidationStringency stringency) throws IOException {				
		
		if (null == bamFile) {
			throw new IllegalArgumentException("Please provide a bam file");
		}
		SamReaderFactory factory = SamReaderFactory.makeDefault();		
		factory.validationStringency(null != stringency ? stringency : DefaultStringency);
		logger.debug("Setting Validation Stringency on SamReader to: " + factory.validationStringency().name());	
		/*
		 * check to see if we have a BAM or SAM file, which will determine which input stream is created
		 */
		SamInputResource resources;
		
		if (BamFileIoUtils.isBamFile(bamFile)) {
			SeekableStream is = SeekableStreamFactory.getInstance().getStreamFor(bamFile.getAbsolutePath());
			resources = SamInputResource.of(is);
			if (null != indexFile) {
				SeekableStream indexStream = SeekableStreamFactory.getInstance().getStreamFor(indexFile.getAbsolutePath());
				resources = resources.index(indexStream);
			}
		} else {
			InputStream is = new FileInputStream(bamFile);
			resources = SamInputResource.of(is);
		}
		
		SamReader reader =  factory.open(resources);
		return reader;
	}
	
	
	public static SamReader createSAMFileReaderAsStream(final String bamFile, String indexFile, final ValidationStringency stringency) throws IOException {
		if (StringUtils.isNullOrEmpty(bamFile)) {
			throw new IllegalArgumentException("Please provide a bam file");
		}
		
		// this factory makes a handler that provides openConnection(...) on s3: URLs
		URL.setURLStreamHandlerFactory(new S3AwareURLStreamHandlerFactory());
		
		SeekableStreamFactory.setInstance(new S3AwareSeekableStreamFactory());
		SeekableStream bamSource = SeekableStreamFactory.getInstance().getStreamFor(bamFile);
		
		SamInputResource resources = SamInputResource.of(bamSource);
		
		if (null != indexFile) {
			SeekableStream indexSource = SeekableStreamFactory.getInstance().getStreamFor(indexFile);
			resources = resources.index(indexSource);
			System.out.println("have setup index, index path: " + indexFile);
		}
		
		SamReaderFactory factory = SamReaderFactory.makeDefault();		
		factory.validationStringency(null != stringency ? stringency : DefaultStringency);
		SamReader reader =  factory.open(resources);
		
		System.out.println("reader.hasIndex(): " + reader.hasIndex());
		System.out.println("reader.type(): " + reader.type());
		
		return reader;
	}
	
//	public static SamReader createSAMFileReaderAsAWSStream(String bucket, String bamFile, String indexFile, ValidationStringency stringency) throws IOException {
//		
//		BasicAWSCredentials creds = new BasicAWSCredentials("YOUR_KEY", "YOUR_SECRET"); 
//		S3Client client = S3Client.builder().region(Region.AP_SOUTHEAST_2).build();
//		ResponseInputStream ris = client.getObject(GetObjectRequest.builder()
//                .bucket(bucket)
//                .key(bamFile)
//                .build(),
//                StreamingResponseHandler.toInputStream());
//		
//		SamInputResource resources;
//		resources = SamInputResource.of(ris);
//		
////		if (null == bamFile) {
////			throw new IllegalArgumentException("Please provide a bam file");
////		}
//		SamReaderFactory factory = SamReaderFactory.makeDefault();		
//		factory.validationStringency(null != stringency ? stringency : DefaultStringency);
//		logger.debug("Setting Validation Stringency on SamReader to: " + factory.validationStringency().name());	
//		
//		SamReader reader =  factory.open(resources);
//		System.out.println("reader.hasIndex(): " + reader.hasIndex());
//		System.out.println("reader.type(): " + reader.type());
//		System.out.println("reader.type(): " + reader.type());
//		return reader;
//	}
	
//	public static SamReader createSAMFileReaderThanksConrad(String bucket, String bamFile, String indexFile, ValidationStringency stringency) throws IOException {
//		
//		// this factory makes a handler that provides openConnection(...) on s3: URLs
//		URL.setURLStreamHandlerFactory(new S3AwareURLStreamHandlerFactory());
////		AWSCredentials credentials = AWSUtils.getAWSCredentials();
////		System.out.println("key: " + credentials.getAWSAccessKeyId());
////		System.out.println("secret key: " + credentials.getAWSSecretKey());
//		
//		SeekableStreamFactory.setInstance(new S3AwareSeekableStreamFactory());
//		String bamPath = "s3://" + bucket + "/" + bamFile;
//		SeekableStream bamSource = SeekableStreamFactory.getInstance().getStreamFor(bamPath);
//		
//		SamInputResource resources = SamInputResource.of(bamSource);
//		
//		if (null != indexFile) {
//			String indexPath = "s3://" + bucket + "/" + indexFile;
//			System.out.println("setting up index, index path: " + indexPath);
//			SeekableStream indexSource = SeekableStreamFactory.getInstance().getStreamFor(indexPath);
//			resources = resources.index(indexSource);
//			System.out.println("have setup index, index path: " + indexPath);
//		}
//		
//		SamReaderFactory factory = SamReaderFactory.makeDefault();		
//		factory.validationStringency(null != stringency ? stringency : DefaultStringency);
//		SamReader reader =  factory.open(resources);
//		
//		System.out.println("reader.hasIndex(): " + reader.hasIndex());
//		System.out.println("reader.type(): " + reader.type());
//		
//		return reader;
//	}
	
}
