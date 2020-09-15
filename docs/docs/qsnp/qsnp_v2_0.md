# qsnp v2.0

`qsnp` performs heuristic (rules-based) calling of somatic and germline
single nucleotide variants (SNV) from next-generation sequencing BAM
files.

Key features include:

* joint-caller that considers tumour and normal BAMs together
* calls both somatic and germline variants
* fast - multi-threaded so typically exomes run in under an hour and genomes in 3-4 hours
* written in java so runs on many machine architectures
* driven by a config file in the standard Windows-style INI format
* produces VCF output as well as a format specific to the ICGC project
* annotates variants with evidence codes that can be used to filter for high confidence variants
* includes a VCF mode that will compare a pair of VCFs from GATK or other tools that call tumour and normal separately
* includes a BAM filter step that can be used to exclude undesirable reads
* annotates germline SNPs that exhibit possible LOH (homozygous for non-reference allele)

## Citing and Paper

If you are using qsnp, please cite this publication:

> Kassahn KS, Holmes O, Nones K, Patch A-M, Miller DK, et al. (2013)
> Somatic Point Mutation Calling in Low Cellularity Tumors.
> PLoS ONE 8(11): e74380. doi:10.1371/journal.pone.0074380

You can find the paper [here](http://www.plosone.org/article/info%3Adoi%2F10.1371%2Fjournal.pone.0074380)

## Installation

qsnp requires java 8 and (ideally) a multi-core machine (5 threads are run
concurrently) with at least 20GB of RAM.

* Download the [qsnp tar file](http://sourceforge.net/projects/adamajava/files/qsnp.tar.bz2/download)
* Untar the tar file into a directory of your choice

You should see jar files for qsnp and its dependencies:

~~~~{.text}
$ tar xvf qsnp.tar.bz2
x antlr-3.2.jar
x commons-math3-3.1.1.jar
x ini4j-0.5.2-SNAPSHOT.jar
x jopt-simple-3.2.jar
x picard-1.110.jar
x qbamfilter-1.0pre.jar
x qcommon-0.1pre.jar
x qio-0.1pre.jar
x qmaths-0.1pre.jar
x qpicard-0.1pre.jar
x qsnp-1.0.jar
x sam-1.110.jar
[oholmes@minion0 qsnp]$
~~~~


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
dbSNP = <OPTIONAL - fullpath to dbSNP vcf file, or leave blank if no dbSNP file>
germlineDB = <OPTIONAL - fullpath to germline database vcf file, or leave blank if no file>
chrConv = <REQUIRED if annotateMode is set to dcc, OPTIONAL otherwise - fullpath to chromosome conversion file, or leave blank if no file>
ref = <REQUIRED - ful path to reference fasta file used to map the bam files>
normalBam = <REQUIRED - full path to control bam file>
tumourBam = <REQUIRED - full path to test bam file>
illuminaNormal = <OPTIONAL - fullpath to control Illumina snp chip file, or leave blank if no file>
illuminaTumour = <OPTIONAL - fullpath to test Illumina snp chip file, or leave blank if no file>

[parameters]
runMode            = standard
filter             = and (Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)
annotateMode       = vcf

[ids]
donor = <donor_id>
normalSample = <control_sample_id>
tumourSample = <test_sample_id
analysisId = <unique_id_for_this_analysis>

[outputFiles]
vcf = <REQUIRED - full path to output vcf file>
dccSomatic = <REQUIRED if annotateMode is set to dcc, OPTIONAL otherwise>
dccGermline =<REQUIRED if annotateMode is set to dcc, OPTIONAL otherwise>

[rules]
normal1=0,20,3
normal2=21,50,4
normal3=51,,10
tumour1=0,20,3
tumour2=21,50,4
tumour3=51,,5
~~~~


The [qsnp ini](qsnp_ini.md) page has further details on the ini format.


Run the following command to start execution.

~~~~{.text}
java -Xmx20g -jar qsnp-1.0.jar -i qsnp_demo.ini -log /path/to/qsnp/qsnpdemo.log
~~~~

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
[vcf](#vc_mode)      | Takes a pair of GATK VCF files for normal and
tumour BAMs along with the BAMs and does the subtractive calling of somatic/germline variants plus applies qsnp filtering rules and outputs in ICGC DCC format
[mutect](#mu_mode)   | Takes Mutect output files, applies qsnp filtering rules and outputs in ICGC DCC format

<a name="st_mode"></a>
### `standard` mode

This mode walks through both the normal and tumour bam files applying a filter (if specified) and collates base, quality, strand, novel start count, and unfiltered base (normal only) for each locus.

Multi-threaded - requires 5 threads (2 to read the 2 input bams, 2 to accumulate the information, and another to determine if there is sufficient evidence of a mutation)

An example ini file for standard mode can be found [here](qsnp_ini#standard-mode)

<a name="vc_mode"></a>
## `vcf` mode ##

In VCF mode, qsnp takes a pair (tumour, normal) of VCF files from GATK's UnifiedGenotyer/HaplotypeCaller VCF files, and uses those to create a single vcf file containing somatic and germline mutations, along with any additional information (eg. novel starts counts) which can be used to further filter snp records. The original test and control bams are also required as input parameters in the ini file.

An example ini file for vcf mode can be found [here](qsnp_ini#vcf-mode)

<a name="mu_mode"></a>
### `mutect` mode

In this mode, qsnp takes the output from [MuTect](http://www.broadinstitute.org/cancer/cga/mutect_run) as input. MuTect is a somatic caller and so all calls are set as such. This mode also requires the tumour bam so that it can go back to positions of interest to get coverage, strand and novel start information, as this is missing from the MuTect output.

## I/O

### Input

Each of the qsnp modes requires that an ini file and log file be passed as
the only arguments to the program.
The [INI](http://en.wikipedia.org/wiki/INI_file) file is in the classic 
format originally popularised by the Windows operating system and
subsequently deprecated in favour of the registry and/or XML config files.
The basic file layout is one or more sections, each of which starts with a
section title enclosed in square braces and sitting as the first and only 
element on a line. Each section has 1 or more properties, each of which is
a name/value pair with an "=" char used as separator.

Please [click here](qsnp_ini) for further information including example ini
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

* [picard](https://sourceforge.net/projects/picard/?source=navbar) - version 1.110
* [ini-4j](http://ini4j.sourceforge.net/) - version 0.5.2-SNAPSHOT
* [jopt-simple](http://pholser.github.io/jopt-simple/) - version 3.2
* [commons-math3](http://commons.apache.org/proper/commons-math/) - version 3.1.1
