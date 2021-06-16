# qsv (Latest)

`qsv` is a tool that detects structural variants from matched tumour/normal BAM files. 

## Key feature

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
  --output            Opt, specify qsv output directory here, the base name must be uuid
                        string. All existing output will be override.  Without this option a
                        new output folder will be created according to ini file.
  --output-temporary  Req, directory to write temporary files to.
  --version           Show version number.
~~~~

## Ini file

The template and descrition of ini file follows:

~~~~{.text}
[general]
log = Opt, base name of log file. Def={sample>.log
loglevel = Opt, Logging level [INFO,DEBUG], Def=INFO.
sample = Opt, donor or patient id.
analysis_mode = (*) Opt, Type of sv analysis [pair, clip, both]. Def=both. (*)
output = Req, output directory. A result folder for the analysis will be automatically created in uuid format. (-) 
reference = Req, path to fasta reference file
platform = Req, sequence machine platform [solid, illumina,bgi].
min_insert_size = Opt, minimum size of SV insert. Def=50.
range = Opt,specify one or more chromosomes or inter for translocations. Def={all chromosome>
repeat_cutoff = Opt, specified number of clipped reads to define a potential repeat region, Def=1000
tiled_aligner = Req, Tiled aligner file with full path, created by our in-home aligner - q3tiledaligner.  
qcmg = Opt, set to true if plan to create more output for the user named qcmg. Def=false. 

[pair]
pairing_type = (**) Opt, type of reads [pe,lmp,imp]. Def=pe. (**)
mapper = Opt,mapping tool name [bioscope, lifescope, bwa, bwa-mem, novoalign]. Def=bws
pair_query = (+) Opt, qbamfilter query string for discordant pair. Def="and (Cigar_M > 34, MD_mismatch < 3, option_SM > 10, flag_DuplicateRead == false)" for Paired End reads(pe).
cluster_size = Opt, number of discordant reads required to define a cluster. Def=3.
filter_size = Opt, number of control reads in a cluster to classify it germline. Def=1.

[clip]
clip_query = (+) Opt, Filtering query for clips. Def="and (Cigar_M > 34, MD_mismatch < 3, MapQ > 0, flag_DuplicateRead == false)".
clip_size = Opt,number of reads required to proceed with soft clip SV signature detection. Def=3.
consensus_length = Opt, minimum length of soft clip consensus sequence. Def=20.
single_side_clip = Opt, set to true if SV signatures with soft clip evidence at one breakpoint should be included. Def=false.

[test]
name = Req, test sample name which may indicate sample id, name or type.  
sample_id = Opt, test sample id. Def=<test::name>
input_file = Req, the test/disease bam with full path. Must be co-ordinate sorted.

[test/size_1]
rgid = Req, Read Group ID.
lower = Req, lower insert size
upper = Req, upper insert size

[control]
name = Req, control sample name which may indicate sample id, name or type.  
sample_id = Opt, control sample id. Def=<control::name>.
input_file = Req, the control/normal bam with full path. Must be co-ordinate sorted.

[control/size_1]
rgid = Req, Read Group ID.
lower = Req, lower insert size
upper = Req, upper insert size
~~~~

### `output` option under [general] section
Each qsv run will create an new folder named in a random uuid string, hence all qsv results will be output to that folder (\<output>/\<uuid>/). the output under [general] should be full path otherwise it will create under execution directory.
 * eg. "output=qsv" will result to: "./qsv/\<uuid>/"; <br />
 * eg. "output=/user/home/qsv" will result to "/user/home/qsv/\<uuid>". <br />

This ini option will be ignored if the "output" option is specified on command line. 

### `tiled aligner` option under [general] section
a file containing 13-mer motifs, and their locations in a genome, generated by our in house tool, q3TiledAligner.

### `analysis mode` option under [general] section 

qsv has two analysis modes: pair (Discordant pair)  and clip (Soft clipping). They can run separately or together. it has been tested with:
 * SOLiD long-mate paired and paired-end sequencing data mapped using bioscope or lifescope (discordant pair mode only).
 * Illumina paired-end sequencing data mapped using BWA (discordant pair and soft clipping modes).
 * Illumina long mate paired sequencing data mapped using BWA (discordant pair and soft clipping modes).

### `pairing type` option under [pair] section

the pair mode support type: pe (paired End), lmp (solid Long Mate Pair) and imp (illumina mate-pair).

### `query` option under [pair] or [clip] section
qsv uses the [qbamfilter](../qbamfilter/index.md) library to filter reads so only high-quality reads are used for analysis.

### `input` option under [test] and [control] section
To determine somatic and germline events, qsv requires 2 coordinate-sorted BAM files:
  * a test data file (i.e. disease/tumour)
  * a control file (i.e. normal)

qsv can call structural variants in a single BAM file in which case the input is a single coordinate-sorted BAM file: eg. a test data file

### `insert size` option under [test/size] or [control/size] section

Discordant pair mode requires the provision of a normal range of expected insert sizes for paired sequencing reads (lower and upper isize). This can be obtained:
1.  using Picard's [CollectInsertSizeMetrics](http://broadinstitute.github.io/picard/command-line-overview.html#CollectInsertSizeMetrics) to give you a TLEN distribution
2.  using [qprofiler](../qprofiler/index.md)

An upper and lower insert size must be provided for each readgroup in the input BAM files. 
(?? should this [test/size] or [control/size] section be removed if we run clip mode only?)

## outputs
qsv will generate a number of output files to \<output>/\<uuid>/:
 * \<uuid>.qsv.log: a log file records of activities for the qSV analysis.
 * \<uuid>.qsv.summary.txt:  a summary file summarizes parameters used in the qSV analysis.
 * \<uuid>.\<germline|normal-germline|somatic>.sv.txt: summary of structural variants identified in a tab-delimited text file for germline and somatic events.
 * \<uuid>.\<germline|normal-germline|somatic>.dcc: DCC files for QCMG center. 
 * \<uuid>_no_blat_alignment.txt: An unaligned soft clips file lists soft clipps with high evidence that did not lign to the reference genome and are potentially somatic. 
 * \<uuid>.somatic.qprimer: a file lists of regions to design primers 
 * \<uuid>.sv_counts.txt: a file lists counts of the numbers of discordant pair SVs
 * \<uuid>.somatic.softlcip.txt: contigs from soft clips 
 * \<uuid>.<test::name|control::name>.pairing_stats.xml: ???
 * \<uuid>.\<chr>.\<germlines|omatic>.records: files gives information about the discordant pair reads and/or soft clipped reads that support a particular structural variant. Each structural variant is described by:
   *  a header specifying the sv_id, and mutation type call 
   *  a list of the discordant reads (where appropriate) supporting the SV in the format: 
   *  comma separated list of mate 1 read name:read group id, mate 1 reference, mate 1 position start, mate 1 position end, mate 1 flags, mate 1 strand, mate 2 read name:read group id, mate 2 reference, mate 2 position start, mate 2 position end, mate 2 flags, mate 2 strand, pair orientation (eg. F1R2), mutation type eg DEL/ITX based on expected read strand, orientation and distance. 
   *  list of soft clipped reads (where appropriate) supporting the SV in the format: 
   *  comma separated list of read name: read group id, reference, position, strand, left/right clip type, clipping sequence 

