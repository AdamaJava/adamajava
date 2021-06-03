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
 * Output is written to the directory specified in the ini file.

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
  --range             Opt, Specify reference sequence name to run. Otherwise qsv will run on
                        reference listed on ini file. Def=<input_BAM_reference>
  --version           Show version number.
~~~~

## Ini file

* (*) qsv has two analysis modes: pair (Discordant pair)  and clip (Soft clipping). They can run separately or together. 
* (+) the pair mode support type: pe (paired End), lmp (solid Long Mate Pair) and imp (illumina mate-pair).
* (-) the output under [general] should be full path otherwise it will create under execution directory. eg. 
 	"output=qsv" will result to: "./qsv/beb19f3f-6221-406f-b206-898f52b6ed8c/"; <br />
	"output=/user/home/qsv" will result to "/user/home/qsv/05785d0b-c6b4-465b-8843-49d0e94c0de1". <br />
	Here, each qsv run creates a new output folder in uuid format. This option will ignored if the "output" option is specified on command line. 

The template and descrition of ini file follows:

~~~~{.text}
[general]
log = Opt, base name of log file. Def=<sample>.log
loglevel = Opt, Logging level [INFO,DEBUG], Def=INFO.
sample = Opt, donor or patient id.
sv_analysis = Opt, Type of sv analysis [pair, clip, both]. Def=both. (*)
output = Req, output directory. A result folder for the analysis will be automatically created in uuid format. (-) 
reference = Req, path to fasta reference file
platform = Req, sequence machine platform [solid, illumina,bgi].
min_insert_size = Opt, minimum size of SV insert. Def=50.
?isize_records=number of records per read group used to calculate isize (seem not used)
range = Opt,specify one or more chromosomes or inter for translocations. Def=<all chromosome>
repeat_cutoff = Opt, specified number of clipped reads to define a potential repeat region, Def=1000
tiled_aligner = Req, Tiled aligner file with full path, created by our in-home aligner - q3tiledaligner.  
qcmg = Opt, set to true if plan to create more output for the user named qcmg. Def=false. 

[pair]
pairing_type = Opt, type of reads [pe,lmp,imp]. Def=pe. (+)
mapper = Opt,mapping tool name [bioscope, lifescope, bwa, bwa-mem, novoalign]. Def=bws
pair_query = Opt, qbamfilter query string for discordant pair. Def="and (Cigar_M > 34, MD_mismatch < 3, option_SM > 10, flag_DuplicateRead == false)" for Paired End reads(pe).
cluster_size = Opt, number of discordant reads required to define a cluster. Def=3.
filter_size = Opt, number of control reads in a cluster to classify it germline. Def=1.
?primer_size = 3 (seem not used) 

[clip]
clip_query = Opt, Filtering query for clips. Def="and (Cigar_M > 34, MD_mismatch < 3, MapQ > 0, flag_DuplicateRead == false)".
clip_size = Opt,number of reads required to proceed with soft clip SV signature detection. Def=3.
consensus_length = Opt, minimum length of soft clip consensus sequence. Def=20.
single_side_clip = Opt, set to true if SV signatures with soft clip evidence at one breakpoint should be included. Def=false.
?blatpath = path to blat executable /home/Software/BLAT (seem not used)
?blatserver = name of blat server eg:localhost (seem not used)
?blatport = port for blat server (seem not used)

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

