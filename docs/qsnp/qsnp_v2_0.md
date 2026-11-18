# qsnp v2.0

`qsnp` performs heuristic (rules-based) calling of somatic and germline
single nucleotide variants (SNV) from next-generation sequencing BAM
files.

Key features include:

* joint-caller that considers tumour and normal BAMs together
* calls both somatic and germline variants
* fast - multi-threaded, so exomes typically run in under an hour, and a whole genome tumour/normal pair (~100x/40x) in under 2 hours
* written in java so runs on many machine architectures
* driven by a config file in the standard Windows-style INI format
* produces VCF output
* annotates variants with evidence codes that can be used to filter for high confidence variants
* includes a VCF mode that will compare a pair of VCFs from GATK or other tools that call tumour and normal separately
* can be run on a single (tumour) sample
* includes a BAM filter step that can be used to exclude undesirable reads
* annotates germline SNPs that exhibit possible LOH (homozygous for non-reference allele)

## Citing and Paper

If you are using qsnp, please cite this publication:

> Kassahn KS, Holmes O, Nones K, Patch A-M, Miller DK, et al. (2013)
> Somatic Point Mutation Calling in Low Cellularity Tumors.
> PLoS ONE 8(11): e74380. doi:10.1371/journal.pone.0074380

You can find the paper [here](http://www.plosone.org/article/info%3Adoi%2F10.1371%2Fjournal.pone.0074380)

## Installation

qsnp requires java 21 and (ideally) a multi-core machine with at least 20GB of RAM. Whole genome tumour/normal pairs need more - see [Memory and garbage collection](#memory-and-garbage-collection).

* To do a build of qsnp, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qsnp and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qsnp:build
  ~~~~
  This creates the qsnp jar file along with dependent jars in the `qsnp/build/flat` folder


## Usage

qsnp requires only two arguments in order to run - a config (.ini) file 
containing details of the analysis requested and the files to be used, and 
a pathname for a log file.  The config file is somewhat more complicated
although once you have established an analysis pattern, it is usually
sufficient for a new analysis to just copy an existing config file and 
change the names of the input and output files.

The contents of a config file are:

~~~~{.text}
[inputFiles]
ref = <REQUIRED - full path to the reference fasta file used to map the bam files>
controlBam = <OPTIONAL - full path to the control bam file. If not set, qsnp runs in single sample mode>
testBam = <REQUIRED - full path to the test bam file>

[parameters]
runMode = standard
filter = and (Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)

[ids]
donor = <donor_id>
controlSample = <control_sample_id>
testSample = <test_sample_id>
analysisId = <unique_id_for_this_analysis>

[outputFiles]
vcf = <REQUIRED - full path to output vcf file>

[rules]
control1=0,20,3
control2=21,50,4
control3=51,,10
test1=0,20,3
test2=21,50,4
test3=51,,5
~~~~


The [qsnp ini](qsnp_ini.md) page has further details on the ini format.


Run the following command to start execution.

~~~~{.text}
java -Xmx20g -jar qsnp/build/flat/qsnp.jar -i qsnp_demo.ini -log /path/to/qsnp/qsnpdemo.log
~~~~

### Memory and garbage collection

For whole genome tumour/normal pairs, the parallel garbage collector is recommended. On a ~100x/40x WGS pair it ran ~11% faster than the default (G1) collector and used about half the CPU, with identical output. Allow ample heap (60GB was used in testing):

~~~~{.text}
java -XX:+UseParallelGC -Xms60g -Xmx60g -jar qsnp/build/flat/qsnp.jar -i qsnp_demo.ini -log /path/to/qsnp/qsnpdemo.log
~~~~

The `adama/bin/qsnp` launcher script already uses the parallel garbage collector, but does not set the heap size, so java will use 25% of the memory it can see. Set `-Xmx` (or request enough memory from the scheduler) for large inputs.

When the parallel garbage collector pauses, it briefly uses all of the machine's cores. On shared nodes, `-XX:ParallelGCThreads=N` limits it to N threads.

This will log both to console and to the specified log file.
The log file looks like:

~~~~{.text}
...
13:21:38.256 [main] EXEC org.qcmg.snp.Main - Uuid 72490a55_78d4_4e87_8def_3cd292f5c907
13:21:38.259 [main] EXEC org.qcmg.snp.Main - StartTime 2013-09-05 13:21:38
13:21:38.260 [main] EXEC org.qcmg.snp.Main - OsName Linux
13:21:38.260 [main] EXEC org.qcmg.snp.Main - OsArch amd64
13:21:38.260 [main] EXEC org.qcmg.snp.Main - OsVersion 2.6.18-348.6.1.el5
13:21:38.261 [main] EXEC org.qcmg.snp.Main - RunBy oholmes
13:21:38.261 [main] EXEC org.qcmg.snp.Main - ToolName qsnp
13:21:38.262 [main] EXEC org.qcmg.snp.Main - ToolVersion 1.0 (6198)
13:21:38.262 [main] EXEC org.qcmg.snp.Main - CommandLine qsnp -i /panfs/home/oholmes/qSNP/demo/demo.ini -log /panfs/home/oholmes/qSNP/demo/qsdemo.log
13:21:38.262 [main] EXEC org.qcmg.snp.Main - JavaHome /share/software/jdk1.7.0_13/jre
13:21:38.263 [main] EXEC org.qcmg.snp.Main - JavaVendor Oracle Corporation
13:21:38.263 [main] EXEC org.qcmg.snp.Main - JavaVersion 1.7.0_13
13:21:38.263 [main] EXEC org.qcmg.snp.Main - host minion1
...
~~~~


## Modes

qsnp has a number of modes.  Modes are specified in the ini file using the `runMode=` value within the `[parameters]` section.  The available mode are:

Mode     | Description
-------- | ------------
[standard](#st_mode) | Main qsnp mode where BAM files for normal and tumour samples are compared and filtered using qsnp's heuristic engine.
[vcf](#vc_mode)      | Takes a pair of GATK VCF files for the normal and tumour samples, does the subtractive calling of somatic/germline variants, and applies the qsnp filtering rules

<a name="st_mode"></a>
### `standard` mode

This mode walks through both the normal and tumour bam files applying a filter (if specified) and collates base, quality, strand, novel start count, and unfiltered base (normal only) for each locus.

Multi-threaded - requires 5 threads (2 to read the 2 input bams, 2 to accumulate the information, and another to determine if there is sufficient evidence of a mutation)

An example ini file for standard mode can be found [here](qsnp_ini.md#standard-mode)

<a name="vc_mode"></a>
### `vcf` mode

In VCF mode, qsnp takes a pair (tumour, normal) of VCF files from GATK's HaplotypeCaller (or a similar caller), and uses those to create a single VCF file containing the somatic and germline mutations, along with any additional information which can be used to further filter the records. If no control VCF is given, the tumour VCF is processed on its own. If a test BAM is given, its header is used to sort the output, otherwise GRCh37 contig order is assumed.

An example ini file for vcf mode can be found [here](qsnp_ini.md#vcf-mode)

The `mutect` mode of earlier versions has been removed.

## I/O

### Input

Each of the qsnp modes requires that an ini file (`-i`) and log file (`-log`) be passed as
the only arguments to the program. `-loglevel DEBUG` gives more detailed logging.
The [INI](http://en.wikipedia.org/wiki/INI_file) file is in the classic 
format originally popularised by the Windows operating system and
subsequently deprecated in favour of the registry and/or XML config files.
The basic file layout is one or more sections, each of which starts with a
section title enclosed in square braces and sitting as the first and only 
element on a line. Each section has 1 or more properties, each of which is
a name/value pair with an "=" char used as separator.

Please [click here](qsnp_ini.md) for further information including example ini
files for each of the qsnp modes.

### Output

A single [VCF](http://samtools.github.io/hts-specs/VCFv4.1.pdf) file is the
output from qsnp.  There are some standard annotations added to the filter,
info and format fields as described below:


#### Filter field

The following annotations are used in the filter field. Where multiple
filters apply, the details of filters are provided as semicolon-separated
list.

Filter | Type | Description
----|----|----
PASS | somatic & germline | all filters have been passed
COVN12 | somatic | less than 12 reads coverage in normal
COVN8 | germline | less than 8 reads coverage in normal
SAN3 | germline | less than 3 reads of same allele in normal
COVT | germline | less than 8 reads coverage in tumour
SAT3 | germline | less than 3 reads of same allele in tumour
GERM | somatic | mutation is a germline variant in another patient
MIN | somatic | mutation also found in pileup of normal .BAM
MIUN | somatic | mutation also found in pileup of unfiltered normal .BAM
NNS | somatic | less than 4 novel starts not considering read pair
MR | somatic | less than 5 mutant reads or user-defined minimum
MER | somatic | mutation same as reference
SBIAS | somatic & germline | mutation only found on a single strand (strand bias)
5BP* | somatic & germline | less than 5 alt supporting reads with the alt in the middle of the read (ie. between read start + 5 and read end -5) * refers to number of reads with the alt within 5bp of the start and end of the read

#### Info field

The info field contains information regarding the mutant/variant call:


Abbreviation | Description
----|----
MR | Number mutant/variant reads
NNS | Number novel starts not considering read pair
FS | Flanking Sequence ( 5bp either side of position)

#### Format field

The format field contains more detailed information regarding the alleles
present in the tumour and normal samples:


Abbreviation | Description
----|----
GT | genotype: 0/0 homozygous reference; 0/1 heterozygous for alternate allele; 1/1 homozygous for alternate allele
GD | genotype details: specific alleles (A,G,T or C)
AC | allele count: lists number of reads on forward strand avg base quality, reverse strand avg base quality


## Dependencies#

### Internal

* [qbamfilter](../qbamfilter/index.md) - used when filtering bam files

### External

The external libraries (htsjdk, ini4j, jopt-simple etc.) are downloaded by
gradle when qsnp is built, and are copied to `qsnp/build/flat` along with the
qsnp jar. See `qsnp/build.gradle` for the versions used.
