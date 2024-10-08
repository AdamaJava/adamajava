package org.qcmg.qmule;

import htsjdk.samtools.*;
import htsjdk.samtools.fastq.FastqConstants;
import htsjdk.samtools.fastq.FastqReader;
import htsjdk.samtools.fastq.FastqRecord;
import htsjdk.samtools.util.*;
import org.broadinstitute.barclay.argparser.Argument;
import org.broadinstitute.barclay.argparser.CommandLineProgramProperties;
import org.broadinstitute.barclay.help.DocumentedFeature;
import picard.PicardException;
import picard.cmdline.CommandLineProgram;
import picard.cmdline.StandardOptionDefinitions;
import picard.cmdline.programgroups.ReadDataManipulationProgramGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static htsjdk.samtools.util.SequenceUtil.getSamReadNameFromFastqHeader;

/**
     * Converts a FASTQ file to an unaligned BAM or SAM file.
     * <p>
     *     Output read records will contain the original base calls and quality scores will be
     *     translated depending on the base quality score encoding: FastqSanger, FastqSolexa and FastqIllumina.
     * </p>
     * <p>
     *     There are also arguments to provide values for SAM header and read attributes that are not present in FASTQ
     *     (e.g see <code>RG</code> or <code>SM</code> below).
     * </p>
     * <h3>Inputs</h3>
     * <p>
     *     One FASTQ file name for single-end or two for pair-end sequencing input data.
     *     These files might be in gzip compressed format (when file name is ending with ".gz").
     * </p>
     * <p>
     *     Alternatively, for larger inputs you can provide a collection of FASTQ files indexed by their name (see <code>USE_SEQUENCIAL_FASTQ</code> for details below).
     * </p>
     * <p>
     *     By default, this tool will try to guess the base quality score encoding. However you can indicate it explicitly
     *     using the <code>QUALITY_FORMAT</code> argument.
     * </p>
     * <h3>Output</h3>
     * A single unaligned BAM or SAM file. By default, the records are sorted by query (read) name.
     * <h3>Usage examples</h3>
     *
     * <h4>Example 1:</h4>
     * <p>
     *     Single-end sequencing FASTQ file conversion. All reads are annotated
     *     as belonging to the "rg0013" read group that in turn is part of the sample "sample001".
     * </p>
     * <pre>
     * java -jar picard.jar FastqToSam \
     *      F1=input_reads.fastq \
     *      O=unaligned_reads.bam \
     *      SM=sample001 \
     *      RG=rg0013
     * </pre>
     * <h4>Example 2:</h4>
     * <p>
     *     Similar to example 1 above, but for paired-end sequencing.
     * </p>
     * <pre>
     * java -jar picard.jar FastqToSam \
     *      F1=forward_reads.fastq \
     *      F2=reverse_reads.fastq \
     *      O=unaligned_read_pairs.bam \
     *      SM=sample001 \
     *      RG=rg0013
     * </pre>
     */
@CommandLineProgramProperties(
        summary = "<p>" + org.qcmg.qmule.FastqToSamWithHeaders.USAGE_SUMMARY + ".</p>" + org.qcmg.qmule.FastqToSamWithHeaders.USAGE_DETAILS,
        oneLineSummary = org.qcmg.qmule.FastqToSamWithHeaders.USAGE_SUMMARY,
        programGroup = ReadDataManipulationProgramGroup.class)
@DocumentedFeature
public class FastqToSamWithHeaders extends CommandLineProgram {

    public static void main(final String[] argv) {


        int exitStatus = new FastqToSamWithHeaders().instanceMain(argv);

        System.exit(exitStatus);
    }
    static final String USAGE_SUMMARY =
            "Converts a FASTQ file to an unaligned BAM or SAM file";
    static final String USAGE_DETAILS =
            "<p>Output read records will contain the original base calls and quality scores will be " +
                        "translated depending on the base quality score encoding: FastqSanger, FastqSolexa and FastqIllumina.</p>" +
                        "<p>There are also arguments to provide values for SAM header and read attributes that are not present in FASTQ " +
                        "(e.g see RG or SM below).</p>" +
                        "<h3>Inputs</h3>" +
                        "<p>One FASTQ file name for single-end or two for pair-end sequencing input data. " +
                        "These files might be in gzip compressed format (when file name is ending with \".gz\").</p>" +
                        "<p>Alternatively, for larger inputs you can provide a collection of FASTQ files indexed by their name " +
                        "(see USE_SEQUENCIAL_FASTQ for details below).</p>" +
                        "<p>By default, this tool will try to guess the base quality score encoding. However you can indicate it explicitly " +
                        "using the QUALITY_FORMAT argument.</p>" +
                        "<h3>Output</h3>" +
                        "<p>A single unaligned BAM or SAM file. By default, the records are sorted by query (read) name.</p>" +
                        "<h3>Usage examples</h3>" +
                        "<h4>Example 1:</h4>" +
                        "<p>Single-end sequencing FASTQ file conversion. All reads are annotated " +
                        "as belonging to the \"rg0013\" read group that in turn is part of the sample \"sample001\".</p>" +
                        "<pre>java -jar picard.jar FastqToSam \\\n" +
                        "        F1=input_reads.fastq \\\n" +
                        "        O=unaligned_reads.bam \\\n" +
                        "        SM=sample001 \\\n" +
                        "        RG=rg0013</pre>" +
                        "<h4>Example 2:</h4>" +
                        "<p>Similar to example 1 above, but for paired-end sequencing.</p>" +
                        "<pre>java -jar picard.jar FastqToSam \\\n" +
                        "       F1=forward_reads.fastq \\\n" +
                        "       F2=reverse_reads.fastq \\\n" +
                        "       O=unaligned_read_pairs.bam \\\n" +
                        "       SM=sample001 \\\n" +
                        "       RG=rg0013</pre><hr />";

    private static final Log LOG = Log.getInstance(picard.sam.FastqToSam.class);

    @Argument(shortName="F1", doc="Input fastq file (optionally gzipped) for single end data, or first read in paired end data.")
    public File FASTQ;

    @Argument(shortName="F2", doc="Input fastq file (optionally gzipped) for the second read of paired end data.", optional=true)
    public File FASTQ2;

    @Argument(doc="Use sequential fastq files with the suffix <prefix>_###.fastq or <prefix>_###.fastq.gz." +
            "The files should be named:\n" +
            "    <prefix>_001.<extension>, <prefix>_002.<extension>, ..., <prefix>_XYZ.<extension>\n" +
            " The base files should be:\n" +
            "    <prefix>_001.<extension>\n" +
            " An example would be:\n" +
            "    RUNNAME_S8_L005_R1_001.fastq\n" +
            "    RUNNAME_S8_L005_R1_002.fastq\n" +
            "    RUNNAME_S8_L005_R1_003.fastq\n" +
            "    RUNNAME_S8_L005_R1_004.fastq\n" +
            "RUNNAME_S8_L005_R1_001.fastq should be provided as FASTQ.", optional=true)
    public boolean USE_SEQUENTIAL_FASTQS = false;

    @Argument(shortName="V", doc="A value describing how the quality values are encoded in the input FASTQ file.  " +
            "Either Solexa (phred scaling + 66), Illumina (phred scaling + 64) or Standard (phred scaling + 33).  " +
            "If this value is not specified, the quality format will be detected automatically.", optional = true)
    public FastqQualityFormat QUALITY_FORMAT;

    @Argument(doc="Output SAM/BAM file. ", shortName= StandardOptionDefinitions.OUTPUT_SHORT_NAME)
    public File OUTPUT ;

    @Argument(shortName="RG", doc="Read group name")
    public String READ_GROUP_NAME = "A";

    @Argument(shortName="SM", doc="Sample name to insert into the read group header")
    public String SAMPLE_NAME;

    @Argument(shortName="LB", doc="The library name to place into the LB attribute in the read group header", optional=true)
    public String LIBRARY_NAME;

    @Argument(shortName="PU", doc="The platform unit (often run_barcode.lane) to insert into the read group header", optional=true)
    public String PLATFORM_UNIT;

    @Argument(shortName="PL", doc="The platform type (e.g. ILLUMINA, SOLID) to insert into the read group header", optional=true)
    public String PLATFORM;

    @Argument(shortName="CN", doc="The sequencing center from which the data originated", optional=true)
    public String SEQUENCING_CENTER;

    @Argument(shortName = "PI", doc = "Predicted median insert size, to insert into the read group header", optional = true)
    public Integer PREDICTED_INSERT_SIZE;

    @Argument(shortName = "PG", doc = "Program group to insert into the read group header.", optional=true)
    public String PROGRAM_GROUP;

    @Argument(shortName = "PM", doc = "Platform model to insert into the group header (free-form text providing further details of the platform/technology used)", optional=true)
    public String PLATFORM_MODEL;

    @Argument(doc="Comment(s) to include in the merged output file's header.", optional=true, shortName="CO")
    public List<String> COMMENT = new ArrayList<>();

    @Argument(shortName = "DS", doc = "Inserted into the read group header", optional = true)
    public String DESCRIPTION;

    @Argument(shortName = "DT", doc = "Date the run was produced, to insert into the read group header", optional = true)
    public Iso8601Date RUN_DATE;

    @Argument(shortName="SO", doc="The sort order for the output sam/bam file.")
    public SAMFileHeader.SortOrder SORT_ORDER = SAMFileHeader.SortOrder.queryname;

    @Argument(doc="Minimum quality allowed in the input fastq.  An exception will be thrown if a quality is less than this value.")
    public int MIN_Q = 0;

    @Argument(doc="Maximum quality allowed in the input fastq.  An exception will be thrown if a quality is greater than this value.")
    public int MAX_Q = SAMUtils.MAX_PHRED_SCORE;

    @Deprecated
    @Argument(doc="Deprecated (No longer used). If true and this is an unpaired fastq any occurrence of '/1' or '/2' will be removed from the end of a read name.")
    public Boolean STRIP_UNPAIRED_MATE_NUMBER = false;

    @Argument(doc="Allow (and ignore) empty lines")
    public Boolean ALLOW_AND_IGNORE_EMPTY_LINES = false;

    public static final String ZT_ATTRIBUTE = "ZT";
    public static final String ZH_ATTRIBUTE = "ZH";

    public static final String TRIMMED_BASES = " TB:";

    private static final SolexaQualityConverter solexaQualityConverter = SolexaQualityConverter.getSingleton();


    /**
     * Get a list of FASTQs that are sequentially numbered based on the first (base) fastq.
     * The files should be named:
     *   <prefix>_001.<extension>, <prefix>_002.<extension>, ..., <prefix>_XYZ.<extension>
     * The base files should be:
     *   <prefix>_001.<extension>
     * An example would be:
     *   RUNNAME_S8_L005_R1_001.fastq
     *   RUNNAME_S8_L005_R1_002.fastq
     *   RUNNAME_S8_L005_R1_003.fastq
     *   RUNNAME_S8_L005_R1_004.fastq
     * where `baseFastq` is the first in that list.
     */
    protected static List<File> getSequentialFileList(final File baseFastq) {
        final List<File> files = new ArrayList<>();
        files.add(baseFastq);

        // Find the correct extension used in the base FASTQ
        FastqConstants.FastqExtensions fastqExtensions = null;
        String suffix = null; // store the suffix including the extension
        for (final FastqConstants.FastqExtensions ext : FastqConstants.FastqExtensions.values()) {
            suffix = "_001" + ext.getExtension();
            if (baseFastq.getAbsolutePath().endsWith(suffix)) {
                fastqExtensions = ext;
                break;
            }
        }
        if (null == fastqExtensions) {
            throw new PicardException(String.format("Could not parse the FASTQ extension (expected '_001' + '%s'): %s", Arrays.toString(FastqConstants.FastqExtensions.values()), baseFastq));
        }

        // Find all the files
        for (int idx = 2; true; idx++) {
            String fastq = baseFastq.getAbsolutePath();
            fastq = String.format("%s_%03d%s", fastq.substring(0, fastq.length() - suffix.length()), idx, fastqExtensions.getExtension());
            try {
                IOUtil.assertFileIsReadable(new File(fastq));
            } catch (final SAMException e) { // the file is not readable, so do not continue
                break;
            }
            files.add(new File(fastq));
        }

        return files;
    }

    /* Simply invokes the right method for unpaired or paired data. */
    protected int doWork() {
        IOUtil.assertFileIsReadable(FASTQ);
        if (FASTQ2 != null) {
            IOUtil.assertFileIsReadable(FASTQ2);
        }
        IOUtil.assertFileIsWritable(OUTPUT);

        final SAMFileHeader header = createSamFileHeader();
        final SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(header, false, OUTPUT);

        // Set the quality format
        QUALITY_FORMAT = picard.sam.FastqToSam.determineQualityFormat(fileToFastqReader(FASTQ),
                (FASTQ2 == null) ? null : fileToFastqReader(FASTQ2),
                QUALITY_FORMAT);

        // Lists for sequential files, but also used when not sequential
        final List<FastqReader> readers1 = new ArrayList<>();
        final List<FastqReader> readers2 = new ArrayList<>();

        if (USE_SEQUENTIAL_FASTQS) {
            // Get all the files
            for (final File fastq : getSequentialFileList(FASTQ)) {
                readers1.add(fileToFastqReader(fastq));
            }
            if (null != FASTQ2) {
                for (final File fastq : getSequentialFileList(FASTQ2)) {
                    readers2.add(fileToFastqReader(fastq));
                }
                if (readers1.size() != readers2.size()) {
                    throw new PicardException(String.format("Found %d files for FASTQ and %d files for FASTQ2.", readers1.size(), readers2.size()));
                }
            }
        }
        else {
            readers1.add(fileToFastqReader(FASTQ));
            if (FASTQ2 != null) {
                readers2.add(fileToFastqReader(FASTQ2));
            }
        }

        // Loop through the FASTQs
        for (int idx = 0; idx < readers1.size(); idx++) {
            makeItSo(readers1.get(idx),
                    (readers2.isEmpty()) ? null : readers2.get(idx),
                    writer);
        }

        // Close all the things
        for (final FastqReader reader : readers1) reader.close();
        for (final FastqReader reader : readers2) reader.close();
        writer.close();

        return 0;
    }

    /**
     * Handles the FastqToSam execution on the FastqReader(s).
     *
     * In some circumstances it might be useful to circumvent the command line based instantiation of this
     * class, however note that there is no handholding or guardrails to running in this manner.
     *
     * It is the caller's responsibility to close the reader(s)
     *
     * @param reader1 The FastqReader for the first fastq file
     * @param reader2 The second FastqReader if applicable. Pass in null if only using a single reader
     * @param writer The SAMFileWriter where the new SAM file is written
     *
     */
    public void makeItSo(final FastqReader reader1, final FastqReader reader2, final SAMFileWriter writer) {
        final int readCount = (reader2 == null) ?  doUnpaired(reader1, writer) : doPaired(reader1, reader2, writer);
        LOG.info("Processed " + readCount + " fastq reads");
    }

    /** Creates a simple SAM file from a single fastq file. */
    protected int doUnpaired(final FastqReader freader, final SAMFileWriter writer) {
        int readCount = 0;
        final ProgressLogger progress = new ProgressLogger(LOG);
        for ( ; freader.hasNext()  ; readCount++) {
            final FastqRecord frec = freader.next();
            final String frecName = getSamReadNameFromFastqHeader(frec.getReadName());
            final SAMRecord srec = createSamRecord(writer.getFileHeader(), frecName , frec, false) ;
            srec.setReadPairedFlag(false);
            writer.addAlignment(srec);
            progress.record(srec);
        }

        return readCount;
    }

    /** More complicated method that takes two fastq files and builds pairing information in the SAM. */
    protected int doPaired(final FastqReader freader1, final FastqReader freader2, final SAMFileWriter writer) {
        int readCount = 0;
        final ProgressLogger progress = new ProgressLogger(LOG);
        for ( ; freader1.hasNext() && freader2.hasNext() ; readCount++) {
            final FastqRecord frec1 = freader1.next();
            final FastqRecord frec2 = freader2.next();

            final String frec1Name = getSamReadNameFromFastqHeader(frec1.getReadName());
            final String frec2Name = getSamReadNameFromFastqHeader(frec2.getReadName());
            final String baseName = getBaseName(frec1Name, frec2Name, freader1, freader2);

            final SAMRecord srec1 = createSamRecord(writer.getFileHeader(), baseName, frec1, true) ;
            srec1.setFirstOfPairFlag(true);
            srec1.setSecondOfPairFlag(false);
            writer.addAlignment(srec1);
            progress.record(srec1);

            final SAMRecord srec2 = createSamRecord(writer.getFileHeader(), baseName, frec2, true) ;
            srec2.setFirstOfPairFlag(false);
            srec2.setSecondOfPairFlag(true);
            writer.addAlignment(srec2);
            progress.record(srec2);
        }

        if (freader1.hasNext() || freader2.hasNext()) {
            throw new PicardException("Input paired fastq files must be the same length");
        }

        return readCount;
    }

    private FastqReader fileToFastqReader(final File file) {
        return new FastqReader(file, ALLOW_AND_IGNORE_EMPTY_LINES);
    }


    public static SAMRecord createSamRecord(final SAMFileHeader header, final String baseName, final FastqRecord frec, final boolean paired, String readGroupName, FastqQualityFormat fqFormat, int minQ, int maxQ) {
        final SAMRecord srec = new SAMRecord(header);
        srec.setReadName(baseName);
        srec.setReadString(frec.getReadString());
        srec.setReadUnmappedFlag(true);
        srec.setAttribute(ReservedTagConstants.READ_GROUP_ID, readGroupName);
        // Optimize additional header handling
        String additionalHeader = frec.getReadName().substring(baseName.length());
        if ( ! additionalHeader.isEmpty()) {
            /*
            If this contains the trimmed bases flag (TB:) then put that in a separate tag
             */
            int tbIndex = additionalHeader.indexOf(TRIMMED_BASES);
            if (tbIndex == -1) {
                srec.setAttribute(ZH_ATTRIBUTE, additionalHeader);
            } else {
                srec.setAttribute(ZT_ATTRIBUTE, additionalHeader.substring(tbIndex + 4));
                if (tbIndex > 0) {
                    srec.setAttribute(ZH_ATTRIBUTE, additionalHeader.substring(0, tbIndex));
                }
            }
        }

        // Convert and validate quality scores
        final byte[] quals = StringUtil.stringToBytes(frec.getBaseQualityString());
        convertQuality(quals, fqFormat);

        if (fqFormat != FastqQualityFormat.Standard) {
            for (final byte qual : quals) {
                final int uQual = qual & 0xff;
                if (uQual < minQ || uQual > maxQ) {
                    throw new PicardException("Base quality " + uQual + " is not in the range " + minQ + ".." +
                            maxQ + " for read " + frec.getReadName());
                }
            }
        }
        srec.setBaseQualities(quals);

        if (paired) {
            srec.setReadPairedFlag(true);
            srec.setMateUnmappedFlag(true);
        }
        return srec;
    }
    private SAMRecord createSamRecord(final SAMFileHeader header, final String baseName, final FastqRecord frec, final boolean paired) {
        return FastqToSamWithHeaders.createSamRecord(header, baseName, frec, paired, READ_GROUP_NAME, QUALITY_FORMAT, MIN_Q, MAX_Q);
    }

    /** Creates a simple header with the values provided on the command line. */
    public SAMFileHeader createSamFileHeader() {
        final SAMReadGroupRecord rgroup = new SAMReadGroupRecord(this.READ_GROUP_NAME);
        rgroup.setSample(this.SAMPLE_NAME);
        if (this.LIBRARY_NAME != null) rgroup.setLibrary(this.LIBRARY_NAME);
        if (this.PLATFORM != null) rgroup.setPlatform(this.PLATFORM);
        if (this.PLATFORM_UNIT != null) rgroup.setPlatformUnit(this.PLATFORM_UNIT);
        if (this.SEQUENCING_CENTER != null) rgroup.setSequencingCenter(SEQUENCING_CENTER);
        if (this.PREDICTED_INSERT_SIZE != null) rgroup.setPredictedMedianInsertSize(PREDICTED_INSERT_SIZE);
        if (this.DESCRIPTION != null) rgroup.setDescription(this.DESCRIPTION);
        if (this.RUN_DATE != null) rgroup.setRunDate(this.RUN_DATE);
        if (this.PLATFORM_MODEL != null) rgroup.setPlatformModel(this.PLATFORM_MODEL);
        if (this.PROGRAM_GROUP != null) rgroup.setProgramGroup(this.PROGRAM_GROUP);

        final SAMFileHeader header = new SAMFileHeader();
        header.addReadGroup(rgroup);

        for (final String comment : COMMENT) {
            header.addComment(comment);
        }

        header.setSortOrder(this.SORT_ORDER);
        return header ;
    }

    /** Based on the type of quality scores coming in, converts them to a numeric byte[] in phred scale. */
     static void convertQuality(final byte[] quals, final FastqQualityFormat version) {
         switch (version) {
             case Standard -> SAMUtils.fastqToPhred(quals);
             case Solexa -> solexaQualityConverter.convertSolexaQualityCharsToPhredBinary(quals);
             case Illumina -> solexaQualityConverter.convertSolexa_1_3_QualityCharsToPhredBinary(quals);
         }
    }

    /**
     * Returns the read baseName and asserts correct pair read name format:
     * <ul>
     * <li> Paired reads must either have the exact same read names or they must contain at least one "/"
     * <li> and the First pair read name must end with "/1" and second pair read name ends with "/2"
     * <li> The baseName (read name part before the /) must be the same for both read names
     * <li> If the read names are exactly the same but end in "/2" or "/1" then an exception will be thrown
     * </ul>
     */
    String getBaseName(final String readName1, final String readName2, final FastqReader freader1, final FastqReader freader2) {

        /*
        readName1 and readName2 will not end in /1 or /2 as those have been trimmed by the time we are here.
        if they are the same, return, otherwise, do the check
         */
        if ( ! readName1.isEmpty() && readName1.equals(readName2)) {
            return readName1;
        }

        // Retrieve base name and pair number in one operation to avoid multiple traversals
        String[] toks1 = breakReadName(readName1, freader1);
        String[] toks2 = breakReadName(readName2, freader2);

        if (!toks1[0].equals(toks2[0])) {
            throw new PicardException(String.format("In paired mode, read name 1 (%s) does not match read name 2 (%s)", readName1, readName2));
        }

        // Check for the most common scenario and exit early if matched
        if ("1".equals(toks1[1]) && "2".equals(toks2[1])) {
            return toks1[0];
        }

        // Handle the case where either both are null or one is null and the other is not
        final String num1 = toks1[1];
        final String num2 = toks2[1];

        if ((num1 == null) != (num2 == null)) {
            if (num1 != null) {
                throw new PicardException(error(freader1, "Pair 1 number is missing (" + readName1 + "). Both pair numbers must be present or neither."));
            } else {
                throw new PicardException(error(freader2, "Pair 2 number is missing (" + readName2 + "). Both pair numbers must be present or neither."));
            }
        } else if (num1 != null) {
            if (!"1".equals(num1)) {
                throw new PicardException(error(freader1, "Pair 1 number must be 1 (" + readName1 + ")"));
            }
            throw new PicardException(error(freader2, "Pair 2 number must be 2 (" + readName2 + ")"));
        }

        return toks1[0];
    }

    /**
     * Breaks read name into baseName and number in one traversal.
     * Returns an array containing the baseName (index 0) and number (index 1).
     */
    private String[] breakReadName(final String readName, final FastqReader freader) {
        if (readName.isEmpty()) {
            throw new PicardException(error(freader, "Pair read name cannot be empty: " + readName));
        }
        final int idx = readName.lastIndexOf('/');
        final String[] result = { readName, null };
        if (idx != -1) {
            final String num = readName.substring(idx + 1);
            result[1] = ("1".equals(num) || "2".equals(num)) ? num : null;
            result[0] = result[1] != null ? readName.substring(0, idx) : readName;
        }
        return result;
    }

    /** Little utility to give error messages corresponding to line numbers in the input files. */
    private String error(final FastqReader freader, final String str) {
        return str + " at line " + freader.getLineNumber() + " in file " + freader.getFile().getAbsolutePath();
    }

    @Override
    protected String[] customCommandLineValidation() {
        if (MIN_Q < 0) return new String[]{"MIN_Q must be >= 0"};
        if (MAX_Q > SAMUtils.MAX_PHRED_SCORE) return new String[]{"MAX_Q must be <= " + SAMUtils.MAX_PHRED_SCORE};
        return null;
    }
}
