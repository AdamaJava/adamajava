/**
 * © Copyright The University of Queensland 2010-2014.
 * © Copyright QIMR Berghofer Medical Research Institute 2014-2016.
 *
 * This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.bammerge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import htsjdk.samtools.SAMTagUtil;
import htsjdk.samtools.SamFileHeaderMerger;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SAMProgramRecord;
import htsjdk.samtools.SAMReadGroupRecord;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.ValidationStringency;

import org.qcmg.common.log.QLogger;
import org.qcmg.common.log.QLoggerFactory;
import org.qcmg.common.string.StringUtils;
import org.qcmg.common.util.Constants;
import org.qcmg.common.util.FileUtils;
import org.qcmg.picard.HeaderUtils;
import org.qcmg.picard.MultiSAMFileIterator;
import org.qcmg.picard.MultiSAMFileReader;
import org.qcmg.picard.SAMOrBAMWriterFactory;
import org.qcmg.picard.util.SAMReadGroupRecordUtils;

/**
 * The <code>FileMerger</code> class represents the operation of SAM/BAM file
 * merging. Instantiation of this class performs a single merge upon a list of
 * SAM/BAM files to produce a single merged SAM/BAM file. A combination of SAM
 * and BAM files may be included in the input file list. <code>FileMerger</code>
 * determines the format of the input files according to the extension of each
 * filename (.bam for BAM files; .sam for SAM file). The output file will be
 * formatted according to the extension specified for the output file name.
 * <p>
 * Default behaviour prevents merging of SAM/BAM files that contain identical
 * read groups. A list of read group replacements can be specified to prevent
 * clashes; alternatively, the merge can be forced to ignore clashes.
 * <p>
 * The header of the output SAM/BAM will contain a PG line representing the
 * specific execution of the merge. To help correlate records with their
 * original SAM/BAM files (pre-merger), <code>FileMerger</code> adds PG:zc and
 * RG:zc annotations to the header and zc annotations to the records of the
 * output SAM/BAM. This approach preserves reversibility of the merge for
 * splitting the merged SAM/BAM into its constituent SAMs/BAMs.
 * <p>
 * Providing a non-negative value for the <code>numberRecords</code> constructor
 * argument limits the number of records merged into the output to the specified
 * value. Negative <code>numberRecords</code> values will cause the merging of
 * all records from the original SAM/BAM files. Overloaded
 * <code>FileMerger</code> constructors obviate the need to use negative
 * <code>numberRecords</code> value to request merging of all records.
 * <p>
 * Implementation Details: The class performs the merge during construction.
 * Successful construction indicates that the merge completed successfully. Any
 * exception thrown during construction indicates that the merge was
 * unsuccessful. Public methods exposed by the class--other than the
 * constructors themselves--provide access to information regarding the outcome
 * of the merging process, not to the contents of the data merged. The merging
 * process reads records one-by-one into memory from the input files, and
 * shuffles them directly into the output file. Picard implements any in-memory
 * caching of records during this operation.
 */
public final class FileMerger {
	
	/*
	 * Set up some short tags used by SAMREcord to retrive attributes
	 */
	public static final String ZC = "ZC";
	public static final String RG = "RG";
	public static final short ZC_TAG = SAMTagUtil.getSingleton().makeBinaryTag(ZC);
	public static final short RG_TAG = SAMTagUtil.getSingleton().RG;

	/** The naming prefix for temporary files during merging. */
	private static final String TEMP_FILE_PREFIX = "tmp";

	/**The regex pattern for parsing colon-delimited read group replacements.*/
	private static final Pattern colonDelimitedPattern = Pattern.compile("[:]+");

	/** The string to insert into the output header PG:CL annotation. */
	private final String commandLine;

	/** The list of group replacements used during the merge. */
	private GroupReplacements groupReplacements;

	/** The set of input files to be merged. */
	private final Set<File> inputFiles = new HashSet<File>();

	/** Flag to indicate that the output file will be included in the merge. */
	private final boolean includeOutputFile;

	/** Flag to indicate that read group clashes are to be ignored. */
	private final boolean ignoreReadGroupClashes;
	
	/** Flag to indicate that an index file should also be created. */
	private final boolean createIndex;

	/** The number of merged records in the output SAM/BAM. */
	private final int numberRecords;

	/** Counter for summing the total number of records merged. */
	private long mergeCount;

	/** The output file for the merge results. */
	private File outputFile;

	/** Reader for inputting records sourced from multiple input files. */
	private MultiSAMFileReader inputReader;

	/** Writer for outputting merged records into the output SAM/BAM. */
	//private SAMFileWriter outputWriter;

	private  SAMOrBAMWriterFactory outputWriterfactory;
	/**The merged header obtained from combining the input SAM/BAM file headers.*/
	private SAMFileHeader mergedHeader;
	
	private final String uuid;
	
	/**
	 * Used for the reverse lookup of group replacements based on the input file
	 * instance and old group value.
	 */
	private final Map<File, Map<String, GroupReplacement>> replacementMap = new HashMap<File, Map<String, GroupReplacement>>();

	/** The combined set of file names to be used in the merging process. */
	private String[] allInputFileNames;
	
	private final ValidationStringency validation ;
	
	private final String[] comments;
	
	private final File tmpdir;
	
	private final QLogger logger = QLoggerFactory.getLogger(FileMerger.class);
	

	/**
	 * Constructor that performs the merge.
	 * 
	 * @param outputFileName
	 *            the filename for the output SAM/BAM. Must have a .bam or .sam
	 *            extension.
	 * @param inputFileNames
	 *            the list of SAM/BAM filenames intended for merging. File names
	 *            must end in .sam or .bam file extensions.
	 * @param groupReplacements
	 *            the list of read group replacements.
	 * @param commandLine
	 *            the string inserted into the PG:CL annotation of the output
	 *            header's @PG line.
	 * @param numberRecords
	 *            the total number of records to merge (from start of file).
	 *            Specifying a -1 value indicates no limit, merging all the
	 *            records available in the source SAM/BAM files.
	 * @param includeOutputFile
	 *            if true, include the specified output file in the merge (if
	 *            the output file does not exist, an exception will be thrown).
	 * @param ignoreReadGroupClashes
	 *            if true, read group clashes are ignored.
	 * @throws BamMergeException
	 *             if merge exceptions are encountered.
	 * @throws IOException
	 *             if I/O exceptions are encountered during the merge.
	 * @throws Exception
	 *             if general exceptions are encountered during the merge.
	 */
	
	
	
	/**
	 * 
	 * @param uuid 
	 * @param outputFileName: the filename for the output SAM/BAM. Must have a .bam or .sam extension.
	 * @param inputFileNames: the list of SAM/BAM filenames intended for merging. File names must end in .sam or .bam file extensions.
	 * @param groupReplacements: the list of read group replacements.
	 * @param commandLine: the string inserted into the PG:CL annotation of the output header's @PG line.
	 * @param numberRecords: the total number of records to merge (from start of file). Specifying a -1 value indicates no limit, merging all the records available in the source SAM/BAM files.
	 * @param includeOutputFile: if true, include the specified output file in the merge (if the output file does not exist, an exception will be thrown).
	 * @param ignoreReadGroupClashes: if true, read group clashes are ignored.
	 * @param createIndex: if true, an index file will be created in case of sort by coordinate
	 * @param tmpdir: the directory in which all temporary files will be stored during processing.
	 * @param validation: How strict to read a SAM or BAM. Possible values: {STRICT, LENIENT, SILENT}. 
	 * @param comments: the String will be added into output header CO lines. 
	 * @throws BamMergeException
	 * @throws IOException
	 * @throws Exception
	 */
	public FileMerger(final String outputFileName, final String[] inputFileNames, final String[] groupReplacements,
			final String commandLine, final int numberRecords, final boolean includeOutputFile,
			final boolean ignoreReadGroupClashes, final boolean createIndex, final String tmpdir,
			final ValidationStringency validation, final String[] comments, String uuid) throws BamMergeException, IOException, Exception {
		this.includeOutputFile = includeOutputFile;
		this.ignoreReadGroupClashes = ignoreReadGroupClashes;
		this.createIndex = createIndex;
		this.numberRecords = numberRecords;
		this.commandLine = commandLine;
		this.mergeCount = 0;		
	    this.validation = validation;
	    this.comments = comments;
	    	this.tmpdir = tmpdir != null ? new File(tmpdir) : null;
	    	this.uuid = uuid;
		resolveFiles(outputFileName, inputFileNames, includeOutputFile);
		resolveReplacements(groupReplacements);
		performMerge();
		cleanUpTemporaries(outputFileName);
	}


	/**
	 * Constructor that performs the merge.
	 * 
	 * @param outputFileName
	 *            the filename for the output SAM/BAM. Must have a .bam or .sam
	 *            extension.
	 * @param inputFileNames
	 *            the list of SAM/BAM filenames intended for merging. File names
	 *            must end in .sam or .bam file extensions.
	 * @param groupReplacements
	 *            the list of read group replacements.
	 * @param commandLine
	 *            the string inserted into the PG:CL annotation of the output
	 *            header's @PG line.
	 * @param numberRecords
	 *            the total number of records to merge (from start of file).
	 *            Specifying a -1 value indicates no limit, merging all the
	 *            records available in the source SAM/BAM files.
	 * @param includeOutputFile
	 *            if true, include the specified output file in the merge (if
	 *            the output file does not exist, an exception will be thrown).
	 * @param ignoreReadGroupClashes
	 *            if true, read group clashes are ignored.
	 * @throws BamMergeException
	 *             if merge exceptions are encountered.
	 * @throws IOException
	 *             if I/O exceptions are encountered during the merge.
	 * @throws Exception
	 *             if general exceptions are encountered during the merge.
	 */
	public FileMerger(final String outputFileName, final String[] inputFileNames, final String commandLine,
			final int numberRecords, final boolean includeOutputFile, final boolean ignoreReadGroupClashes)
			throws BamMergeException, IOException, Exception {
		
		this(outputFileName, inputFileNames, null, commandLine, -1, false, ignoreReadGroupClashes, false,null,null,null, null);		
	}

	/**
	 * Constructor that performs the merge without having to specify the number
	 * of records to merge nor the flag to indicate inclusion of the output file
	 * in the merge.
	 * 
	 * @param outputFileName
	 *            the filename for the output SAM/BAM. Must have a .bam or .sam
	 *            extension.
	 * @param inputFileNames
	 *            the list of SAM/BAM filenames intended for merging. File names
	 *            must end in .sam or .bam file extensions.
	 * @param groupReplacements
	 *            the list of read group replacements.
	 * @param commandLine
	 *            the string inserted into the PG:CL annotation of the output
	 *            header's @PG line.
	 * @param ignoreReadGroupClashes
	 *            if true, read group clashes are ignored.
	 * @throws IOException
	 *             if I/O exceptions are encountered during the merge.
	 * @throws Exception
	 *             if general exceptions are encountered during the merge.
	 */
	public FileMerger(final String outputFileName, final String[] inputFileNames, final String[] replacements,
			final String commandLine, final boolean ignoreReadGroupClashes) throws IOException, Exception {
		this(outputFileName, inputFileNames, replacements, commandLine, -1, false, ignoreReadGroupClashes, false,null,null,null, null);
 
	}

	/**Library_20120510_D.o174055
	 * Constructor that performs the merge without having to specify the number
	 * of records to merge nor the flag to indicate inclusion of the output file
	 * in the merge.
	 * 
	 * @param outputFileName
	 *            the filename for the output SAM/BAM. Must have a .bam or .sam
	 *            extension.
	 * @param inputFileNames
	 *            the list of SAM/BAM filenames intended for merging. File names
	 *            must end in .sam or .bam file extensions.
	 * @param groupReplacements
	 *            the list of read group replacements.
	 * @param commandLine
	 *            the string inserted into the PG:CL annotation of the output
	 *            header's @PG line.
	 * @throws IOException
	 *             if I/O exceptions are encountered during the merge.
	 * @throws Exception
	 *             if general exceptions are encountered during the merge.
	 */
	public FileMerger(final String outputFileName, final String[] inputFileNames, final String[] groupReplacements,
			final String commandLine) throws IOException, Exception {
		this(outputFileName, inputFileNames, groupReplacements, commandLine, -1, false, false, false,null,null,null, null);
	}

	/**
	 * Constructor that performs the merge without having to specify the number
	 * of records to merge nor the flag to indicate inclusion of the output file
	 * in the merge.
	 * 
	 * @param outputFileName
	 *            the filename for the output SAM/BAM. Must have a .bam or .sam
	 *            extension.
	 * @param inputFileNames
	 *            the list of SAM/BAM filenames intended for merging. File names
	 *            must end in .sam or .bam file extensions.
	 * @param groupReplacements
	 *            the list of read group replacements.
	 * @throws IOException
	 *             if I/O exceptions are encountered during the merge.
	 * @throws Exception
	 *             if general exceptions are encountered during the merge.
	 */
	public FileMerger(final String outputFileName, final String[] inputFileNames, final String[] groupReplacements)
			throws IOException, Exception {
		this(outputFileName, inputFileNames, groupReplacements, "", -1, false, false, false,null,null,null, null);
	}

	/**
	 * Constructor that performs the merge without having to specify the number
	 * of records to merge nor the flag to indicate inclusion of the output file
	 * in the merge.
	 * 
	 * @param outputFileName
	 *            the filename for the output SAM/BAM. Must have a .bam or .sam
	 *            extension.
	 * @param inputFileNames
	 *            the list of SAM/BAM filenames intended for merging. File names
	 *            must end in .sam or .bam file extensions.
	 * @throws IOException
	 *             if I/O exceptions are encountered during the merge.
	 * @throws Exception
	 *             if general exceptions are encountered during the merge.
	 */
	public FileMerger(final String outputFileName, final String[] inputFileNames) throws IOException, Exception {
		this(outputFileName, inputFileNames, "", -1, false, false);
	}

	/**
	 * Constructor that performs the merge without having to specify the number
	 * of records to merge nor the flag to indicate inclusion of the output file
	 * in the merge.
	 * 
	 * @param outputFileName
	 *            the filename for the output SAM/BAM. Must have a .bam or .sam
	 *            extension.
	 * @param inputFileNames
	 *            the list of SAM/BAM filenames intended for merging. File names
	 *            must end in .sam or .bam file extensions.
	 * @param groupReplacements
	 *            the list of read group replacements.
	 * @param commandLine
	 *            the string inserted into the PG:CL annotation of the output
	 *            header's @PG line.
	 * @param ignoreReadGroupClashes
	 *            if true, read group clashes are ignored.
	 * @throws IOException
	 *             if I/O exceptions are encountered during the merge.
	 * @throws Exception
	 *             if general exceptions are encountered during the merge.
	 */
	public FileMerger(final String outputFileName, final String[] inputFileNames, final String commandLine,
			final boolean ignoreReadGroupClashes) throws IOException, Exception {
		this(outputFileName, inputFileNames, commandLine, -1, false, ignoreReadGroupClashes);
	}
 

	/**
	 * Assembles the true set of filenames to be used in the merging process,
	 * and creates the File instance to which the merging process will write.
	 * 
	 * @param outputFileName
	 * @param inputFileNames
	 * @param includeOutputFile
	 * @throws BamMergeException
	 * @throws IOException
	 */
	private void resolveFiles(final String outputFileName, final String[] inputFileNames,
			final boolean includeOutputFile) throws BamMergeException, IOException {
		if (includeOutputFile) {
			allInputFileNames = append(inputFileNames, outputFileName);
			outputFile = createTemporaryFile(outputFileName);
		} else {
			allInputFileNames = inputFileNames;
			outputFile = FileUtils.getCanonicalFile(outputFileName);
		}
		if (null == allInputFileNames || 1 > allInputFileNames.length) {
			throw new BamMergeException("INSUFFICIENT_INPUT_FILES");
		}
	}

	/**
	 * Assembles the set of group replacements and detects bad replacements in
	 * the process.
	 * 
	 * @param replacements
	 *            the list of intended group replacements.
	 * @throws BamMergeException
	 *             if the list of replacements is malformed.
	 */
	private void resolveReplacements(final String[] replacements) throws BamMergeException {
		if(replacements != null)
			groupReplacements = new GroupReplacements(replacements, allInputFileNames);
		else
			groupReplacements = new GroupReplacements(new String[0], allInputFileNames);
	}

	/**
	 * Creates the temporary File instance for use during merging for when the
	 * output file is to be included in the merge.
	 * 
	 * @param fileName
	 *            the name of the output file.
	 * @return the File object for the temporary file.
	 * @throws BamMergeException
	 *             if the specified file name has no extension.
	 * @throws IOException
	 *             if problems are encountered in opening the file.
	 */
	private File createTemporaryFile(final String fileName) throws BamMergeException, IOException {
		String outputDirectory = FileUtils.getParentDirectory(fileName);
		File outDir;
		if(tmpdir == null)
			outDir = new File(outputDirectory);
		else
			outDir = tmpdir;
		
		File file = new File(fileName);
		String ext = FileUtils.getExtension(file);
		if (null == ext) {
			throw new BamMergeException("UNSUITABLE_MERGE_FILE");
		}
		ext = "." + ext;
		File tempFile = File.createTempFile(TEMP_FILE_PREFIX, ext, outDir);
		return tempFile.getCanonicalFile();
	}

	/**
	 * Coordinates the overall merging process.
	 * 
	 * @throws BamMergeException
	 * @throws IOException
	 * @throws Exception
	 */
	private void performMerge() throws BamMergeException, IOException, Exception {
		try {
			openReader();
			mergeHeaders();
			openWriter();			
			mergeAlignments();

		} finally {
			close();
		}
	}

	/**
	 * Deletes temporary files (if any) created during the merging process.
	 * 
	 * @param mergeFileName
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws BamMergeException
	 */
	private void cleanUpTemporaries(final String mergeFileName) throws IOException, FileNotFoundException,
			BamMergeException {
		if (includeOutputFile) {
			File mergeFile = new File(mergeFileName);
			FileUtils.copyFile(outputFile, mergeFile);
			if (!outputFile.delete()) {
				throw new BamMergeException("CANNOT_DELETE_TEMPORARY_FILE");
			}
		}
	}

	/**
	 * Returns the new read group for replacing the specified old read group in
	 * the specified input SAM/BAM File.
	 * 
	 * @param file
	 *            the source SAM/BAM File instance containing the old group.
	 * @param oldGroup
	 *            the old group to be replaced.
	 * @return the new group to be used in the replacement.
	 */
	private String getReplacementGroup(File file, String oldGroup) {
		String result = null;
		Map<String, GroupReplacement> temp = replacementMap.get(file);
		if (null != temp) {
			GroupReplacement replacement = temp.get(oldGroup);
			if (null != replacement) {
				result = replacement.getNewGroup();
			}
		}
		return result;
	}

	/**
	 * Opens the source files for reading, while also detecting malformed
	 * scenarios such as duplicate input files, output file being used as an
	 * input file (does not apply to merging with the output), and read group
	 * clashes where applicable.
	 * 
	 * @throws IOException
	 * @throws BamMergeException
	 * @throws Exception
	 */
	private void openReader() throws IOException, BamMergeException, Exception {
		for (int i = 0; i < allInputFileNames.length; i++) {
			String fileName = allInputFileNames[i];
			File inputFile = FileUtils.getCanonicalFile(fileName);
			detectBadFile(fileName, inputFile);
			Map<String, GroupReplacement> groupMappings = groupReplacements.getGroupMappings(fileName);
			if ( null != groupMappings && ! groupMappings.isEmpty()) {
				replacementMap.put(inputFile, groupMappings);
			}
		}
		detectSameInputFiles();
		detectFileUsedAsInputAndOutput();
		if (!includeOutputFile) {
			detectBadOutputFile();
		}
		updateHeaderAttributes();
		if (!ignoreReadGroupClashes) {
			detectReadGroupOverlaps();
		}
	}

	/**
	 * Throws an exception if the output File instance has been used as an input
	 * file. The check is performed using canonical file instances, so that
	 * alternative pathings to the same file are detected.
	 * 
	 * @throws BamMergeException
	 */
	private void detectFileUsedAsInputAndOutput() throws BamMergeException {
		for (final File basisFile : inputFiles) {
			checkSameness(basisFile, outputFile);
//			for (final File file : inputFiles) {
//				if (outputFile.equals(file)) {
//					throw new BamMergeException("FILE_USED_BOTH_AS_INPUT_AND_OUTPUT", outputFile.getName());
//				}
//			}
		}
	}

	/**
	 * Throws an exception if an input file is supplied more than once in the
	 * input file list. The check is performed using canonical file instances,
	 * so that alternative pathings to the same file are detected.
	 * 
	 * @throws BamMergeException
	 */
	private void detectSameInputFiles() throws BamMergeException {
		for (final File basisFile : inputFiles) {
			for (final File file : inputFiles) {
				if (basisFile != file) {
					checkSameness(basisFile, file);
				}
			}
		}
	}

	/**
	 * Throws an exception if two File instances refer to the same file. The
	 * check is performed using canonical file instances, so that alternative
	 * pathings to the same file are detected.
	 * 
	 * @param fileA
	 * @param fileB
	 * @throws BamMergeException
	 */
	private static void checkSameness(final File fileA, final File fileB) throws BamMergeException {
		if (fileA.equals(fileB)) {
			Object[] params = { fileA.getName(), fileB.getName() };
			throw new BamMergeException("SAME_FILES", params);
		}
	}

	/**
	 * Updates the RG and PG header lines for the merge.
	 * 
	 * @throws Exception
	 */
	private void updateHeaderAttributes() throws Exception {
		
		//set lenient to Control validation of SAMRecords as they are read from file.
		if(validation != null)
			inputReader = new MultiSAMFileReader(inputFiles, validation);
		else 
			inputReader = new MultiSAMFileReader(inputFiles);
		
		for (SamReader reader : inputReader.getSAMFileReaders()) {
			
			SAMFileHeader header = reader.getFileHeader();
			for (SAMReadGroupRecord record : header.getReadGroups()) {
				String attribute = inputReader.getAttributeZc(record) ;					
				if (null == attribute) {
					File file = inputReader.getFile(reader);
					String name = file.getCanonicalPath();
					Integer zc = inputReader.getDefaultZc(reader);
					assert null != zc;
					if (null == zc) {
						throw new IllegalStateException(
								"Null zc default value not permitted. Report this bug to development.");
					}
					attribute = zc.toString() + ":" + name;
					 
				} else {
					 
						String[] params = colonDelimitedPattern.split(attribute);
						String zc = params[0];
						String fileName = params[1];
						Map<Integer, Integer> replacementZcs = inputReader.getReplacementZcs(reader);
						if (null != replacementZcs) {
							Integer replacement = replacementZcs.get(Integer.parseInt(zc));
							if (null != replacement) {
								zc = replacement.toString();
							}
						}
						attribute = zc + ":" + fileName;
						//delete old ZC tag
			            record.setAttribute("ZC", null);							
				}
				record.setAttribute("zc", attribute);
			}
			for (SAMProgramRecord record : header.getProgramRecords()) {
				Integer zc = inputReader.getAttributeZc(record);
				if (null == zc) {
					zc = inputReader.getDefaultZc(reader);
					if (null == zc) {
						throw new IllegalStateException(
								"Null ZC default value not permitted. Report this bug to development.");
					}
				} else {
						Map<Integer, Integer> replacementZcs = inputReader.getReplacementZcs(reader);
						if (null != replacementZcs) 
							zc = replacementZcs.get(zc);
						//delete old ZC tag
			            record.setAttribute("ZC", null);							 
				}
				record.setAttribute("zc", zc.toString());
			}
		}
	}

	/**
	 * @throws BamMergeException
	 *             if the output file already exists in the filesystem or if the
	 *             specified output file is actually a directory rather than a
	 *             file.
	 */
	private void detectBadOutputFile() throws BamMergeException {
		if (outputFile.exists()) {
			throw new BamMergeException("CANNOT_OVERWRITE_EXISTING_OUTPUT");
		}
		if (outputFile.isDirectory()) {
			throw new BamMergeException("FILE_NOT_DIRECTORY");
		}
	}

	/**
	 * @param fileName
	 * @param inputFile
	 * @throws BamMergeException
	 *             if the input File does not exist or if the input file has
	 *             already been added to the set of input files.
	 */
	private void detectBadFile(String fileName, File inputFile) throws BamMergeException {
		if (!inputFile.exists()) {
			throw new BamMergeException("NONEXISTENT_INPUT_FILE", fileName);
		} else if (!inputFiles.add(inputFile)) {
			throw new BamMergeException("SAME_INPUT_FILE", fileName);
		}
	}

	/**
	 * Throws an exception if any read group clashes arise in the input files,
	 * taking into account the specified read group replacements.
	 * 
	 * @throws BamMergeException
	 *             if any read group clashes arise in the input files, taking
	 *             into account the specified read group replacements.
	 */
	private void detectReadGroupOverlaps() throws BamMergeException {
		Vector<SamReader> readers = inputReader.getSAMFileReaders();
		for (final SamReader basisReader : readers) {
			for (final SamReader reader : readers) {
				if (basisReader != reader) {
					detectReadGroupOverlap(basisReader, reader);
				}
			}
		}
	}

	/**
	 * Throws an exception if any read group clashes are detected in the Files
	 * being read by two SAMFileReaders, taking into account the specified read
	 * group replacements.
	 * 
	 * @param readerA
	 *            the reader for the second SAM/BAM file to be checked.
	 * @param readerB
	 *            the reader for the first SAM/BAM file to be checked.
	 * @throws BamMergeException
	 *             if read group clashes are detected.
	 */
	private void detectReadGroupOverlap(final SamReader readerA, final SamReader readerB)
			throws BamMergeException {
		List<SAMReadGroupRecord> groupsA = readerA.getFileHeader().getReadGroups();
		List<SAMReadGroupRecord> groupsB = readerB.getFileHeader().getReadGroups();
		File fileA = inputReader.getFile(readerA);
		File fileB = inputReader.getFile(readerB);
		for (SAMReadGroupRecord groupA : groupsA) {
			String idA = groupA.getId().trim();
			String newIdA = getReplacementGroup(fileA, idA);
			for (SAMReadGroupRecord groupB : groupsB) {
				String idB = groupB.getId().trim();
				String newIdB = getReplacementGroup(fileB, idB);
				detectReadGroupOverlap(idA, idB, newIdA, newIdB, groupA, groupB);
			}
		}
	}

	/**
	 * Throws an exception if the specified old and new read group IDs clash.
	 * 
	 * @param idA
	 *            the old read group ID for the first input file.
	 * @param idB
	 *            the old read group ID for the second input file.
	 * @param newIdA
	 *            the new read group ID for the first input file.
	 * @param newIdB
	 *            the new read group ID for the second input file.
	 * @throws BamMergeException
	 *             if read group clashes are detected for the specified old and
	 *             new read group IDs.
	 */
	private void detectReadGroupOverlap(String idA, String idB, String newIdA, String newIdB, 
			SAMReadGroupRecord groupA, SAMReadGroupRecord groupB) throws BamMergeException {
		if (null != newIdA && null != newIdB) {
			if (newIdA.equals(newIdB)) {
				throw new BamMergeException("READ_GROUP_OVERLAP", "RG lines: " + SAMReadGroupRecordUtils.getRGString(groupA) , SAMReadGroupRecordUtils.getRGString(groupB));
			}
		} else if (null == newIdA && null != newIdB) {
			if (idA.equals(newIdB)) {
				throw new BamMergeException("READ_GROUP_OVERLAP", "RG lines: " + SAMReadGroupRecordUtils.getRGString(groupA) , SAMReadGroupRecordUtils.getRGString(groupB));
			}
		} else if (null != newIdA && null == newIdB) {
			if (newIdA.equals(idB)) {
				throw new BamMergeException("READ_GROUP_OVERLAP", "RG lines: " + SAMReadGroupRecordUtils.getRGString(groupA) , SAMReadGroupRecordUtils.getRGString(groupB));
			}
		} else if (idA.equals(idB)) {
			throw new BamMergeException("READ_GROUP_OVERLAP", "RG lines: " + SAMReadGroupRecordUtils.getRGString(groupA) , SAMReadGroupRecordUtils.getRGString(groupB));
		}
	}

	/**
	 * Merges the headers of the input files and performs the relevant read
	 * group replacements.
	 */
	private void mergeHeaders() {
		List<SAMReadGroupRecord> newGroups = new ArrayList<>();
		for (SamReader reader : inputReader.getSAMFileReaders()) {
			File file = inputReader.getFile(reader);
			List<SAMReadGroupRecord> oldGroups = reader.getFileHeader().getReadGroups();
			Map<String, GroupReplacement> mappings = replacementMap.get(file);
			if (null == mappings) {
				for (SAMReadGroupRecord oldGroup : oldGroups) {
					newGroups.add(oldGroup);
				}
			} else {
				for (SAMReadGroupRecord oldGroup : oldGroups) {
					GroupReplacement replacement = mappings.get(oldGroup.getId());
					if (null == replacement) {
						newGroups.add(oldGroup);
					} else {
						String newId = replacement.getNewGroup();
						SAMReadGroupRecord newGroup = new SAMReadGroupRecord(newId, oldGroup);
						newGroups.add(newGroup);
					}
				}
			}
		}
		SamFileHeaderMerger merger = new SamFileHeaderMerger( SAMFileHeader.SortOrder.coordinate, inputReader.getSAMFileHeaders(), true);		
		mergedHeader = merger.getMergedHeader();
		mergedHeader.setReadGroups(newGroups);
		addHeaderProgramGroup();
		addHeaderComments();
		replaceUUIDInHeader();
	}

	private void replaceUUIDInHeader() {
		if (uuid != null) {
			List<String> commentsToKeep = new ArrayList<>();
			for (String s : mergedHeader.getComments()) {
				if ( ! s.startsWith(Constants.COMMENT_Q3BAM_UUID_PREFIX)) {
					commentsToKeep.add(s);
				}
			}
			
			// add in the new uuid
			commentsToKeep.add(Constants.COMMENT_Q3BAM_UUID_PREFIX + ":" + uuid);
			mergedHeader.setComments(commentsToKeep);
		}
	}


	/**
	 * Creates the program group record for the merged header.
	 */
	private void addHeaderProgramGroup() {
		SAMProgramRecord record = HeaderUtils.addProgramRecord(mergedHeader, Messages.getProgramName(), Messages
				.getProgramVersion(), commandLine);
		Integer zc = inputReader.getNextAvailableZc();
		record.setAttribute("zc", zc.toString());
	}
	
	private void addHeaderComments() {
		if(comments == null)
			return;
		for(String co: comments){		
			mergedHeader.addComment(co);
		}
		
	}

	/**
	 * Merges the alignments of the source SAM/BAM files, performing the
	 * necessary group replacements and zc annotations in the process.
	 * 
	 * @throws BamMergeException
	 *             if any of the input records have zc annotation values that
	 *             are not part of the RG:zc annotations in the related input
	 *             SAM/BAM.
	 */
	private void mergeAlignments() throws BamMergeException {
		MultiSAMFileIterator iter = inputReader.getMultiSAMFileIterator();
		
		while (iter.hasNext() && !hasReachedNumberRecords()) {
			SAMRecord record = iter.next();
			
			if (null == record.getReadGroup()) {
				logger.warn(record.getSAMString());
				logger.warn(""+record.getAttribute(RG_TAG));
				logger.warn(""+record.getHeader());
				throw new BamMergeException("BAD_RECORD_RG");
			}
			
			SamReader fileReader = iter.getCurrentSAMFileReader();
			if ( ! replacementMap.isEmpty()) {
				String oldGroup = record.getReadGroup().getReadGroupId();
				File file = inputReader.getFile(fileReader);
				String newGroup = getReplacementGroup(file, oldGroup);
				if (null != newGroup) {
					record.setAttribute(RG, newGroup);
				}
			}
			Integer oldZc = record.getIntegerAttribute(ZC);
			if (null == oldZc) {
				Integer zc = inputReader.getDefaultZc(fileReader);
//				assert null != zc;
				record.setAttribute(ZC, zc);
			} else {
				Set<Integer> permissibleZcs = inputReader.getOldZcs(fileReader);
				if (!permissibleZcs.contains(oldZc)) {
					throw new BamMergeException("BAD_RECORD_ZC");
				}
				Map<Integer, Integer> replacementZcs = inputReader.getReplacementZcs(fileReader);
				if (null != replacementZcs) {
					Integer replacement = replacementZcs.get(oldZc);
					if (null != replacement) {
						record.setAttribute(ZC, replacement);
					}
				}
			}
//			queue.add(record);
			outputWriterfactory.getWriter().addAlignment(record);
			mergeCount++;
//			if (mergeCount % 1000000 == 0) {
//				int queueSize = queue.size();
//				while (queueSize > 200000) {
//					try {
//						Thread.sleep(100);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					queueSize = queue.size();
//				}
//			}
		}
//		readingLatch.countDown();
	}

	/**
	 * @return true if the maximum number of merged records has been reached.
	 */
	private boolean hasReachedNumberRecords() {
		return -1 < numberRecords && mergeCount == numberRecords;
	}

	/**
	 * Opens the SAM/BAM file writer for the output file.
	 */
	private void openWriter() {
		outputWriterfactory = new SAMOrBAMWriterFactory(mergedHeader,  true, outputFile,tmpdir, 0, createIndex, true, 500000);
//		outputWriterfactory = new SAMOrBAMWriterFactory(mergedHeader, true, outputFile, tmpdir, createIndex);
	}

	/**
	 * Closes all readers and writers used in the merging process.
	 * 
	 * @throws BamMergeException
	 *             if problems are encountered in closing the readers and
	 *             writers.
	 */
	private void close() throws BamMergeException {
		//close output write and rename index if needed 
		if (null != outputWriterfactory) {
			outputWriterfactory.closeWriter();
			String logMessage = outputWriterfactory.getLogMessage();
			if ( ! StringUtils.isNullOrEmpty(logMessage)) {
				logger.info(logMessage);
			}
		}
	 
		try {
			if (null != inputReader)
				inputReader.close();
		} catch (Exception e) {
			logger.error("Exception caught whilst closing input file", e);
			throw new BamMergeException("CANNOT_CLOSE_FILES");
		}
	 
	}

	/**
	 * Returns concatenated String array consisting of the original input String
	 * array and the specified input String value.
	 * 
	 * @param array
	 *            the original array of Strings.
	 * @param value
	 *            the String value to be added to the array.
	 * @return the full array of String values.
	 */
	private static String[] append(final String[] array, final String value) {
		List<String> list = new ArrayList<String>(Arrays.asList(array));
		list.add(value);
		Object[] objectArray = list.toArray();
		return Arrays.copyOf(objectArray, objectArray.length, String[].class);
	}
	
//	public class Writer implements Runnable {
//
//		@Override
//		public void run() {
//			try {
//				while (true) {
//					SAMRecord rec = queue.poll();
//					if (null != rec) {
//						outputWriter.addAlignment(rec);
//					} else {
//						if (readingLatch.getCount() == 0) {
//							break;
//						}
//						try {
//							Thread.sleep(10);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
//				}
//			} finally {
//				writingLatch.countDown();
//			}
//		}
//	}
}
