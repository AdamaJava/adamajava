# qsv

`qsv` is a sensitive multi-method structural-variant detection tool that 
has been developed for whole genome paired-end or mate-pair cancer
sequencing.
This implementation of qsv integrates independent findings from soft clipping
and discordant mapped pair analyses and increases detection accuracy of
breakpoint, microhomology and non-template sequence by the incorporation
of a localized de novo assembly of abnormal reads and split contig alignment.

## Key features

* joint-caller that considers tumour and normal BAMs together
* fast - typically runs in 6 hours on an 8-core machine for a WGS tumour/normal BAM pair (60x/30x).
* uses evidence from one or both of soft-clipping and discordant mapped pair analyses
* aligns the breakpoint contig to the genome to detect microhomology and non-template sequence at the breakpoint
* open-source java code so you can see exactly how it works

### New Feature
qsv used to uses blat to determine where consensus sequences that it builds best sit. There are a couple of disadvantages to using blat:

  * a blat service needs to be running somewhere (hpcapp01:8000) that qsv can connect to
  * if multiple qsv instances are on the go, multiple blat services also need to be setup as a single blat instance can get saturated leading to slow qsv runtimes
  * blat is free for research, but you need to buy a license if using commercially...

In this version we was decided to replacing blat with an in-house product, q3TiledAligner. There are two part process involved:
  * the tiled aligner part, which for each sequence will generate a list of potential genomic positions. 
  * the Smith-Waterman part, which will attempt to align the sequence to the reference at the positions that the tiled aligner has identified. The Smith-Waterman results are scored and returned, highest score first (similar to blat).

## Installation

qsv requires java 8, a machine with 8 cores (hyperthreaded) and at least 40GB of RAM.

* To do a build of qsv, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qsv and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qsv:build
  ~~~~
  This creates the qsv jar file along with dependent jars in the `qsv/build/flat` folder

### Usage

 * qsv requires 2 arguments in order to run:
   * a config (.ini) file containing details of the parameters to be used
   * a path to a directory where temporary files will be written

~~~~{.text}
  usage: java -jar qsv.jar --ini <ini_file> --output-temporary <directory> [OPTIONS]

  Option              Description
  ------              -----------
  --help              Show usage and help.
  --ini               Req, ini file with required options.
  --uuid              Opt, a sub folder under output directory will be named by this customized
                      UUID. By default, a random UUID will be created.
  --output-temporary  Req, directory to write temporary files to.
  --version           Show version number.
~~~~

## Ini file

The template and description of the ini file is shown below:

~~~~{.text}
[general]
log = Opt, base name of log file. Def=<sample>.log
loglevel = Opt, logging level [INFO,DEBUG], Def=INFO.
sample = Opt, donor or patient id.
sv_analysis = (*) Opt, type of sv analysis [pair, clip, both]. Def=both.
output = (*) Req, output directory. A result sub folder for the analysis will be automatically created in uuid format.
reference = Req, path to fasta reference file
platform = Req, sequence machine platform [solid, illumina,bgi].
min_insert_size = Opt, minimum size of SV insert. Def=50.
range = Opt, specify one or more chromosomes or inter for translocations. Def={all chromosome>
repeat_cutoff = Opt, specified number of clipped reads to define a potential repeat region, Def=1000
tiled_aligner = (*) Req, tiled aligner file with full path, created by our in-home aligner - q3tiledaligner.  
qcmg = Opt, set to true if plan to create more output for the user named qcmg. Def=false. 

[pair]
pairing_type = (*) Opt, type of reads [pe,lmp,imp]. Def=pe.
mapper = Opt, mapping tool name [bioscope, lifescope, bwa, bwa-mem, novoalign]. Def=bws
pair_query = (*) Opt, qbamfilter query string for discordant pair. Def="and (Cigar_M > 34, MD_mismatch < 3, option_SM > 10, flag_DuplicateRead == false)" for Paired End reads(pe).
cluster_size = Opt, number of discordant reads required to define a cluster. Def=3.
filter_size = Opt, number of control reads in a cluster to classify it germline. Def=1.

[clip]
clip_query = (*) Opt, filtering query for clips. Def="and (Cigar_M > 34, MD_mismatch < 3, MapQ > 0, flag_DuplicateRead == false)".
clip_size = Opt, number of reads required to proceed with soft clip SV signature detection. Def=3.
consensus_length = Opt, minimum length of soft clip consensus sequence. Def=20.
single_side_clip = Opt, set to true if SV signatures with soft clip evidence at one breakpoint should be included. Def=false.

[test]
name = Req, test sample name which may indicate sample id, name or type.  
sample_id = Opt, test sample id. Def=<test::name>
input_file = (*) Req, the test/disease bam with full path. Must be co-ordinate sorted.

[test/size_1]
rgid = Req, read Group ID.
lower = (*) Req, lower insert size.
upper = (*) Req, upper insert size.
name = Opt, sample name which may indicate read group id, name or type. Def=<empty string>.

[control]
name = Req, control sample name which may indicate sample id, name or type.  
sample_id = Opt, control sample id. Def=<control::name>.
input_file = (*) Req, the control/normal bam with full path. Must be co-ordinate sorted.

[control/size_1]
rgid = Req, read Group ID.
lower = (*) Req, lower insert size.
upper = (*) Req, upper insert size.
name = Opt, sample name which may indicate read group id, name or type. Def=<empty string>.
~~~~

### `output` option under [general] section
Each time the qsv is run, a new folder under the output directory will be created, containing all the outputs. This new folder is named as a random uuid: \<output>/\<random uuid>/. The output directory has to be specified,  with the full path to the `output` option under [general] section, otherwise it will output to the execution directory.
 * eg. if "output=qsv", then "./qsv/\<random uuid>/" will be created; <br />
 * eg. if "output=.", then "./\<random uuid>/" will be created; <br />
 * eg. if "output=/user/home/qsv", then "/user/home/qsv/\<random uuid>" will be created. <br />

Unless command line option "--uuid" is used, then the new output folder will be named as user specified UUID. In above example, a sub folder named as user specified UUID 
 * eg. if "output=qsv", then "./qsv/\<user specified uuid>/" will be created; <br />
 * eg. if "output=/user/home/qsv", then "/user/home/qsv/\<user specified uuid>/" will be created. <br />

### `tiled_aligner` option under [general] section
a file containing 13-mer motifs, and their locations in a genome, generated by our in house tool, q3TiledAligner.

### `sv_analysis` option under [general] section 

qsv has two analysis modes: pair (Discordant pair)  and clip (Soft clipping). They can run separately or together. It has been tested with:
 * SOLiD long-mate paired and paired-end sequencing data mapped using bioscope or lifescope (discordant pair mode only).
 * Illumina paired-end sequencing data mapped using BWA (discordant pair and soft clipping modes).
 * Illumina long mate paired sequencing data mapped using BWA (discordant pair and soft clipping modes).

### `pairing_type` option under [pair] section

the pair mode support type: pe (paired End), lmp (solid Long Mate Pair) and imp (illumina mate-pair).

### `query` option under [pair] or [clip] section
qsv uses the [qbamfilter](../qbamfilter/index.md) library to filter reads so only high-quality reads are used for analysis.

### `input_file` option under [test] and [control] section
To determine somatic and germline events, qsv requires 2 coordinate-sorted BAM files:
  * a test data file (i.e. disease/tumour)
  * a control file (i.e. normal)

qsv can call structural variants in a single BAM file in which case the input is a single coordinate-sorted BAM file: 
  * a test data file

For discordant pair mode, each BAM record must contain several tags in the optional field. These tags are described in the SAMtags document.

  * MD string for mismatching positions
  * SM template-independent mapping quality (if using default filtering query)
  * NH number of reported alignments that contains the query in the current record. If the reads have been mapped using BWA, the NH flag is not necessary if the records contain the BWA specific fields: X0, XA

### `upper` and `lower` option under [test/size] or [control/size] section

Discordant pair mode requires the provision of a normal range of expected insert sizes for paired sequencing reads (lower and upper isize). This can be obtained:
1.  using Picard's [CollectInsertSizeMetrics](http://broadinstitute.github.io/picard/command-line-overview.html#CollectInsertSizeMetrics) to give you a TLEN distribution
2.  using [qprofiler](../qprofiler/index.md)

An upper and lower insert size must be provided for each readgroup in the input BAM files. This section is not required if sv_analysis=clip. 

## outputs
qsv will generate a number of output files to \<output>/\<uuid>/:
 * \<sample>.qsv.log: a log file records of activities for the qSV analysis.
 * \<sample>.qsv.summary.txt: (*) a summary file summarizes parameters used in the qSV analysis.
 * \<sample>.\<germline|normal-germline|somatic>.sv.txt: summary of structural variants identified in a tab-delimited text file for germline and somatic events.
 * \<sample>.\<germline|normal-germline|somatic>.dcc: DCC files for QCMG center. 
 * \<sample>.no_blat_alignment.txt: An unaligned soft clips file lists soft clips with high evidence that did not align to the reference genome and are potentially somatic. 
 * \<sample>.somatic.qprimer: a file that contains lists of regions to design primers.
 * \<sample>.sv_counts.txt: a file that lists counts of the numbers of discordant pair SVs.
 * \<sample>.somatic.softlcip.txt: contigs from soft clips.
 * \<sample>.\<test::name|control::name>.pairing_stats.xml: a file listing discordant pair stats (counts of each type of discordant pair category).
 * \<sample>.\<chr>.\<germlines|omatic>.records: files gives information about the discordant pair reads and/or soft clipped reads that support a particular structural variant. Each structural variant is described by:
   *  a header specifying the sv_id, and mutation type call.
   *  a list of the discordant reads (where appropriate) supporting the SV in the format.
   *  comma separated list of mate 1 read name:read group id, mate 1 reference, mate 1 position start, mate 1 position end, mate 1 flags, mate 1 strand, mate 2 read name:read group id, mate 2 reference, mate 2 position start, mate 2 position end, mate 2 flags, mate 2 strand, pair orientation (eg. F1R2), mutation type eg DEL/ITX based on expected read strand, orientation and distance. 
   *  list of soft clipped reads (where appropriate) supporting the SV in the format.
   *  comma separated list of read name: read group id, reference, position, strand, left/right clip type, clipping sequence.

### Structural variants file (*.sv.txt)
column | Header | Description
----|----|----
1 | analysis_id |  random uuid created by qsv or user specified one through command line
2 | sample_id | test or control sample id
3 | sv_id | id of the structural variant
4 | sv_type | DEL/ITX – deletion/other intrachromosomal
&nbsp;|| CTX – interchromosomal translocation
&nbsp;|| DUP/INS/ITX – duplication/insertion/other intrachromosomal
&nbsp;|| INV/ITX – inversion/other intrachromosomal
5 | chr1 | chromosome 1 of SV
6 | pos1 | position 1 of SV
7 | strand1
8 | chr2 | chromosome 2 of SV
9 | pos2 | position 2 of SV
10| strand2
11 | test_discordant_pairs_count | number of discordant pair reads which pass the filter from test bam that support the current SV
12 | control_discordant_pairs_count | number of discordant pair reads which pass the filter from control bam that support the current SV
13 | control_low_qual_reads_count | number of low quality discordant pair reads from control bam for the current SV. These reads are lower quality reads from the controlbam that were excluded by the original filtering parameters. Presence of a large number of the reads may indicate the event is germline rather than somatic
14 | test_clips_count_pos1 | number of high quality soft clipped reads at position1 from test/disease bam that support the current SV
15 | test_clips_count_pos2 | number of high quality soft clipped reads at position2 from test/disease bam that support the current SV
16 | control_clips_count_pos1 | number of high quality soft clipped reads at position1 from control bam that support the current SV
17 | control_clips_count_pos2 | number of high quality soft clipped reads at position2 from control bam that support the current SV
18 | Category | Evidence for the SV (1-6)
&nbsp;|| 1. High level of evidence: eg discordant pair evidence, clipping at both SV breakpoints, local split read contig evidence observed
&nbsp;|| 2. Medium level of evidence: eg discordant pair signature (both breakpoints) and soft clipping signature
&nbsp;|| 3. Lower level of evidence eg. discordant pair signature alone
&nbsp;|| 4. Possible germline due to the presence of low quality control reads or evidence in the control bam from local split read alignment
&nbsp;|| 5. Possible repeat region. Greater than 1000 clips identified in the region of the SV breakpoints
&nbsp;|| 6. Low level evidence - Soft clipping signature for one breakpoint
19| microhomology | bases of microhomology found. If microhomology was tested, and no microhomology found, the result will be “not found”. If microhomology was not tested this column will list: “not tested”
20| non-template | bases of non-template sequence found. If non-template was tested, and no non-template was identified, the result will be “not found”. If no non-template was not tested result will be: “not tested”
21| test_split_read_bp |
22| control_split_read_bp|
23| event_notes |
24| contig|


