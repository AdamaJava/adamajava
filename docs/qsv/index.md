# qsv

`qsv` is a sensitive multi-method structural-variant detection tool that 
has been developed for whole genome paired-end or mate-pair cancer
sequencing.
This implementation of qsv integrates independent findings from soft clipping
and discordant mapped pair analyses and increases detection accuracy of
breakpoint, microhomology and non-template sequence by the incorporation
of a localized de novo assembly of abnormal reads and split contig alignment.

Key features include:

* joint-caller that considers tumour and normal BAMs together
* fast - typically runs in 6 hours on an 8-core machine for a WGS tumour/normal BAM pair (60x/30x).
* uses evidence from one or both of soft-clipping and discordant mapped pair analyses
* performs localized de-novo assembly of abnormal reads and split alignments to create a putative contig that spans the breakpoint
* aligns the breakpoint contig to the genome to detect microhomology and non-template sequence at the breakpoint
* open-source java code so you can see exactly how it works

## Installation

qsv requires java 7, a machine with 8 cores (hyperthreaded) and at least 40GB of RAM.

To install qsv:

* Download the [qsv tar file](http://sourceforge.net/projects/adamajava/files/qsv.tar.bz2/download)
* Untar the tar file into a directory of your choice

You should see jar files for qsv and its dependencies:

~~~~
$ tar xjvf qsv-0.3.tar.bz2
x antlr-3.2.jar
x htsjdk-1.140.jar
x ini4j-0.5.2-SNAPSHOT.jar
x jopt-simple-4.6.jar
x picard-lib.jar
x qbamannotate-0.3pre.jar
x qbamfilter-1.2.jar
x qcommon-0.3.jar
x qio-0.1pre.jar
x qpicard-1.1.jar
x qsv-0.3.jar
x trove-3.1a1.jar
~~~~

### Dependencies

qsv uses BLAT to align assembled contigs for breakpoint categorisation so 
BLAT will need to be downloaded and installed prior to using qsv.

* BLAT is free for academic, non-profit or personal use (http://users.soe.ucsc.edu/~kent/)
* A license can be arranged for [commercial use](http://www.kentinformatics.com/).
* A BLAT server (gfServer) should be set up to serve out the human genome to qsv - see [instructions](http://genome.ucsc.edu/goldenPath/help/blatSpec.html).

_We are currently working on removing the BLAT dependency by replacing
it with an in-house aligner - [q3tiledaligner](../q3tiledaligner)._

## Running qsv

qsv requires 2 arguments in order to run:

* a config (.ini) file containing details of the parameters to be used
* a path to a directory where temporary files will be written

A qsv invocation might look like:

~~~~{.text}
java -Xmx40g -jar qsv-1.0.jar -ini qsv.demo.ini -tmp /path/to/tmp/directory
~~~~

## Ini file

qsv has two modes which can be run separately or together:

* Discordant pair mode (pair)
* Soft clipping mode (clip)

An example ini file follows:

~~~~{.text}
[general]
log = name of log file
loglevel = INFO or DEBUG
sample = donor or patient id
sv_analysis = Type of sv analysis: pair, clip, both
output = output directory
reference = path to fasta reference file
platform=solid or illumina
min_insert_size = minimum size of SV insert. Default 50.
isize_records=number of records per read group used to calculate isize
range=specify one or more chromosomes or inter for translocations
repeat_cutoff=specified number of clipped reads to define a potential repeat region

[pair]
pairing_type = type of reads: lmp (solid Long Mate Pair), pe (Paired End) or imp (illumina mate-pair)
mapper = mapping eg bioscope, lifescope, bwa,bwa-mem, novoalign
pair_query = qbamfilter query string for discordant pairs eg. and(Cigar_M> 34, option_SM>10, MD_mismatch < 3, Flag_DuplicateRead == false)
cluster_size = number of discordant reads required to define a cluster
filter_size = number of control reads in a cluster to classify it germline


[clip]
clip_query = Filtering query for clips: eg and(Cigar_M> 34, MD_mismatch < 3, MAPQ >0,Flag_DuplicateRead == false)
clip_size = number of reads required to proceed with soft clip SV signature detection
consensus_length = minimum length of soft clip consensus sequence
blatpath = path to blat executable /home/Software/BLAT
blatserver = name of blat server eg:localhost
blatport = port for blat server
single_side_clip = If SV signatures with soft clip evidence at one breakpoint should be included

[test]
name = id for the sample
input_file = location of the test/disease bam. Must be co-ordinate sorted

[test/size_1]
rgid = Read Group ID
lower = lower insert size
upper = upper insert size

[control]
name = id for the control sample
input_file = location of the control sample

[control/size_1]
rgid = Read Group ID
lower = lower insert size
upper =  upper insert size
~~~~

A more detailed description of the ini file options is listed in the table below:

Section | Option | Required/Optional | Description \[Default value\]
----|----|----|----
general | log | optional | Name of log file \[sample_name.log\]
| loglevel | optional | Logging level required, e.g. INFO,DEBUG. \[INFO\]
| sample | required | Donor/sample id eg PatientA
| sv_analysis | optional | Use this option to specify what type of sv_analysis will be carried out in qsv. - pair: discordant pair SV detection - clip: soft clipping SV detection - both: SV detection using both discordant pairs and soft clips \[both\]
|output | required | Output directory. A results folder for the analysis will be automatically created based on sample and date. Eg. output directory is /home/test/qsv. Results will be written to: /home/test/qsv/qsv_patientA_20121025_1111
|reference | required | Path to the reference genome file. Must also have a .fai index file. This can be generated using samtools ‘faidx’ program [1]
| platform | required | Platform used for sequencing: solid or illumina \[illumina\]
| min_insert_size | optional | Minimum size of insert for potential SVs. \[50\]
| range | optional | Specify one or more chromosomes. Specify inter for translocations.
| repeat_cutoff | optional | The number of clipped reads that will define define a potential repeat region (see SV category 5) \[1000\]
pair | pairing_type | Required for discordant pair | Specify the type of read pairing used: - lmp =solid long mate pair - imp (illumina mate pair) - pe = paired end \[pe\]
| mapper | Required for discordant pair | Mapping tool used to map reads: - for lmp: bioscope or lifescope - for pe: bwa \[bwa\]
| pair_query | optional | [qbamfilter](../qbamfilter/index.md) query string to filter the discordant pair reads. (See FILTER OPTIONS) Default is: - If minimal/no filtering is required, use: Flag_DuplicateRead == false  \[- for lmp: and(Cigar_M> 35, option_SM> 14, MD_mismatch< 3, Flag_DuplicateRead == false) - for pe or imp: and(Cigar_M> 35, option_SM> 10, MD_mismatch< 3, Flag_DuplicateRead == false)\]
| cluster_size | optional | Number of reads required to define a cluster. \[3\]
| filter_size | optional | Number of control reads in a cluster required to call the cluster germline. \[1\]
clip | clip_query | optional | [qbamfilter](../qbamfilter/index.md) query string to filter the soft clipped reads. (See FILTER OPTIONS) \[and(Cigar_M> 34,MD_mismatch < 3,MAPQ >0,Flag_DuplicateRead == false)\]
| clip_size | optional | Number of clipped reads required to proceed with soft clip SV signature detection. \[3\]
| consensus_length | optional | Minimum length of soft clip consensus sequence. \[20\]
| blatpath | Required for soft clipping | Path to blat executable /home/Software/BLAT
| blatserver | Required for soft clipping | Name of blat server eg: localhost
| blatport | Required for soft clipping or local split read contig | Port for blat server: eg 8000
| single_side_clip | optional | Set to true if SV signatures with soft clip evidence at one breakpoint should be identified
test | name | required | Name of the test sample eg tumour
| input_file | required | Path to the test bam. Must be co-ordinate sorted
test/size : Use nomenclature test/size_numbe eg test/size_1, test/size_2 etc) | rgid | required | Read Group ID. Found in the header of the bam
| lower | required | Lower insert size
| upper | required | Upper insert size
control | name | required | Name of the control sample eg. normal
| input_file | required | Path to the control bam. Must be co-ordinate sorted |
control/size : Use nomenclature control/size_number e.g. control/size_1, control/size_2 etc) | rgid |required | Read Group ID. Found in the header of the bam
| lower | required | Lower insert size
| upper | required | Upper insert size



## Filter Options

qsv uses the [qbamfilter](../qbamfilter/index.md) library to
filter reads so only high-quality reads are used for analysis.

## Insert Size Options

Discordant pair mode requires the provision of a normal range of expected
insert sizes for paired sequencing reads (lower and upper isize). This can
be obtained:
1.  using Picard's
[CollectInsertSizeMetrics](http://broadinstitute.github.io/picard/command-line-overview.html#CollectInsertSizeMetrics)
to give you a TLEN distribution
2.  using [qprofiler](../qprofiler/index.md)

Once the user has calculated the expected insert size ranges, they should
be added in the ini file (see ini file options test/size section and 
control/size section).
An upper and lower insert size must be provided for each readgroup in the
input BAM files.

## Input Files

qsv takes mapped next-generation sequencing data as input. It has been 
tested with:

* SOLiD long-mate paired and paired-end sequencing data mapped using bioscope or lifescope (discordant pair mode only).
* Illumina paired-end sequencing data mapped using BWA (discordant pair and soft clipping modes).
* Illumina long mate paired sequencing data mapped using BWA (discordant pair and soft clipping modes).

To determine somatic and germline events, qsv requires 2
coordinate-sorted BAM files:

* a test data file (i.e. disease/tumour)
* a control file (i.e. normal) 

qsv can call structural variants in a single BAM file in which case the
input is a single coordinate-sorted BAM file:

* a test data file

For discordant pair mode, each BAM record must contain several tags in
the optional field. These tags are described in the
[SAMtags](https://samtools.github.io/hts-specs/SAMtags.pdf) document.

* `MD` string for mismatching positions
* `SM` template-independent mapping quality (if using default filtering query)
* `NH` number of reported alignments that contains the query in the
current record. If the reads have been mapped using BWA, the NH flag is
not necessary if the records contain the BWA specific fields: `X0`, `XA`

For soft-clipping mode, we recommend the reads are mapped by BWA. (Other
mapping algorithms can be added).

## Output Files

qsv generates a number of output files.

### Log file

* eg. PatientA_test_control.log
* record of activities for the qsv analysis

### Summary file

* eg. PatientA.qsv.summary.txt
* summarizes parameters used in the qsv analysis

### Structural variants file

* eg. PatientA.somatic.sv.txt; PatientA.germline.sv.txt
* Files generated for germline and somatic events
* tab-delimited text file with the columns:

Header | Description
----|----
analysis_id | in format of qsv_sample_date_time
sv_id | id of the structural variant
| sm = somatic
| gm = germline
sv_type | DEL/ITX – deletion/other intrachromosomal
| CTX – interchromosomal translocation
| DUP/INS/ITX – duplication/insertion/other intrachromosomal
| INV/ITX – inversion/other intrachromosomal
chr1 | chromosome 1 of SV
pos1 | position 1 of SV
strand1
chr2 | chromosome 2 of SV
pos2 | position 2 of SV
strand2
test_discordant_pairs_count | number of discordant pair reads which pass the filter from test bam that support the current SV
control_discordant_pairs_count | number of discordant pair reads which pass the filter from control bam that support the current SV
control_low_qual_reads_count | number of low quality discordant pair reads from control bam for the current SV. These reads are lower quality reads from the controlbam that were excluded by the original filtering parameters. Presence of a large number of the reads may indicate the event is germline rather than somatic.
test_clips_count_pos1 | number of high quality soft clipped reads at position1 from test/disease bam that support the current SV
test_clips_count_pos2 | number of high quality soft clipped reads at position2 from test/disease bam that support the current SV
control_clips_count_pos1 | number of high quality soft clipped reads at position1 from control bam that support the current SV
control_clips_count_pos2 | number of high quality soft clipped reads at position2 from control bam that support the current SV
microhomology | bases of microhomology found. If microhomology was tested, and no microhomology found, the result will be “not found”. If microhomology was not tested this column will list: “not tested”
non-template | bases of non-template sequence found. If non-template was tested, and no non-template was identified, the result will be “not found”. If no non-template was not tested result will be: “not tested”
Category | Evidence for the SV (1-6)
| 1. High level of evidence: eg discordant pair evidence, clipping at both SV breakpoints, local split read contig evidence observed.
| 2. Medium level of evidence: eg discordant pair signature (both breakpoints) and soft clipping signature
| 3. Lower level of evidence eg. discordant pair signature alone
| 4. Possible germline due to the presence of low quality control reads or evidence in the control bam from local split read alignment
| 5. Possible repeat region. Greater than 1000 clips identified in the region of the SV breakpoint/s.
| 6. Low level evidence - Soft clipping signature for one breakpoint

### Unaligned soft clips

* Eg test_no_blat_alignment.txt
* Reference positions that have >10 soft clipped reads that do not align to the reference genome and are potentially somatic
* Tab delimited text file format with columns:
    * reference
    * position
    * mutation_type (somatic)
    * clip_type
    * left: clips on the left end of read
    * right: clips on the right end of the read
    * pos_clips
    * number of clipped reads that align to the positive strand of the reference
    * neg_clips
    * number of clipped reads that align to the negative strand of the reference
    * consensus
    * consensus sequence for clipped reads

### Verbose output

* Eg. test.chr10.somatic.records
* Text file that gives information about the discordant pair reads and/or soft clipped reads that support a particular structural variant
* Each structural variant is described by:
    * a header specifying the sv_id, and mutation type call
* A list of the discordant reads (where appropriate) supporting the SV in the format:
    * Comma separated list of mate 1 read name:read group id, mate 1 reference, mate 1 position start, mate 1 position end, mate 1 flags, mate 1 strand, mate 2 read name:read group id, mate 2 reference, mate 2 position start, mate 2 position end, mate 2 flags, mate 2 strand, pair orientation (eg. F1R2), mutation type eg DEL/ITX based on expected read strand, orientation and distance.
* A list of soft clipped reads (where appropriate) supporting the SV in the format:
    * Comma separated list of read name: read group id, reference, position, strand, left/right clip type, clipping sequence

## Examples

Sample example files are provided here:  <TODO>Image Qsv Example Files</TODO>
Edit the following ini file options in example.ini:

* output: location where the results will be written
* input_file: (test section): location of example.test.bam file
* input_file (control section): location of example.control.bam
* blatserver: name of blat server eg localhost
* blatpath: path to blat software
* blatport: port of the blat server

Make sure the BLAT server dependency has been installed and is running.
Run qsv using the following command:

~~~~{.text}
java -jar qsv-0.3.jar -ini example.ini -tmp [path/to/tmp/directory]
~~~~

Results will be written to the specified output directory and can be found
under the directory:

* qsv_sample name_analysis date_analysis time
* eg qsv_PatientA_20130202_1514
