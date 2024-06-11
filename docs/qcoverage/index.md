# qcoverage

`qcoverage` calculates read depth 
statistics given a [BAM](http://samtools.github.io/hts-specs/SAMv1.pdf) 
file with mapped reads, and a 
[GFF3](http://www.sequenceontology.org/gff3.shtml) file with regions of
interest. 

qcoverage actually calculates read depth statistics not coverage so it
should really have been called qreaddepth but by the time the community
had settled on
the distinction between coverage and read depth, the `qcoverage` name had
stuck. C'est la vie.

It can be run in 2 different modes:

* sequence read depth
* physical read depth

Sequence read depth counts, for a given position, how many reads are
aligned so that they have bases that lie over the position. 

Physical read depth counts, for a given position, how many fragments
(not reads)
are aligned so that they have sequenced _or unsequenced_ bases that lie
over the position.

Physical read depth includes both (a) the reads themselves, plus (b) the 
bases that lie between a pair of matched reads. So physical read depth
includes all
bases on a fragment - those at either end that were sequenced and can be seen
in the reads _plus_ those bases that can be inferred to be part of the 
sequenced fragment but that were not themselves sequenced. Physical read
depth is only relevant for paired reads - for unpaired reads, sequence
read depth is identical to physical read depth.

[This link](http://www.nature.com/nrg/journal/v11/n10/fig_tab/nrg2841_F1.html#figure-title)
has more information on the differences between the two.

Note that this tool always uses at least 2 threads - one for monitoring
and one worker thread. You can change the number of worker threads but
there is always one thread in addition to the number specified with
`--threads`. 

## Installation

qcoverage requires java 21 and (ideally) a multi-core machine with at least 20GB of RAM.
* To do a build of qcoverage, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qcoverage and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qcoverage:build
  ~~~~
  This creates the qcoverage jar file along with dependent jars in the `qcoverage/build/flat` folder


## Usage

~~~~
usage: java -jar qcoverage.jar --type <type of coverage>  --input-bam <bam file> --input-gff3 <gff3 file> --output <output prefix> --log <log file> [options]
Option              Description
------              -----------
--help              Show usage and help.
--input-bai         Opt, a BAI index file for the BAM file. Def=<input-bam>.bai.
--input-bam         Req, a BAM input file containing the reads.
--input-gff3        Req, a GFF3 input file defining the features.
--log               Req, log file.
--loglevel          Opt, logging level [INFO,DEBUG], Def=INFO.
--output            Req, the output file path. Here, filename extension will
                      automatically added.
--output-format     Opt, specify output file format, multi values are allowed.
                      Possible values: [VCF, TXT, XML]. Def=TXT.
--per-feature       Opt, to run the per-feature coverage mode. Default is to run
                      standard coverage mode without this option.
--query             Opt, the query string for selecting reads for coverage.
--thread <Integer>  Opt, number of worker threads (yields n+1 total threads).
--type              Req, the type of coverage to perform. Possible Values: [sequence,
                      physical]. 
--validation        Opt, how strict to be when reading a SAM or BAM. Possible values:
                      [STRICT, LENIENT, SILENT].
--version           Show version number.
~~~~

## Output

### standard mode output

The default output is in tab delimited text format, which can be explicitly requested with the "--output-format TXT" option. Here file extension ".txt" is automatically appended to the output file. 

~~~~
java -jar qcoverage.java --type physical --output /path/report \
--input-gff3 /path/GRCh37_ICGC_standard_v2.gff3 --input-bam /path/input.bam --log /path/output.log 

less /path/report.txt

#coveragetype   featuretype     numberofbases   coverage
physical        chrom   2518007876      0x
physical        chrom   385426364       1x
...
physical        chrom   2       1539x
physical        chrom   6       1540x
~~~~

You can also specify the output to be XML format. Here file extension ".xml" is automatically append to the output file. 
~~~~
java -jar qcoverage.java --type physical \
--output-format XML --output /path/report \
--input-gff3 /path/GRCh37_ICGC_standard_v2.gff3 --input-bam /path/input.bam --log /path/report.log 

less /path/report.xml

<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<QCoverageStats>
    <coverageReport feature="chrom" type="PHYSICAL">
        <coverage bases="2518007876" at="0"/>
        <coverage bases="385426364" at="1"/>
...
~~~~


### per-feature mode output
It is possible to have output from the --per-feature mode in either TXT, XML or VCF format. The TXT and XML format are very extensive for each feature showing how many bases of the feature were covered at what levels. For long regions with variable coverage, this can run to hundreds of lines so an TXT or XML format output file containing hundreds of thousands of features can run to tens of millions of lines - it can be as big as the BAM itself. In most cases, the level of detail provided by the TXT or XML output is overkill and the VCF format is sufficient.

The VCF file contains a single line for each feature so it is almost
identical in length to the GFF3 file that defines the features. Each line 
gives details about the feature including start and stop positions as well 
as a high level summary of the coverage. This information is sufficient to 
provide average coverage for the feature and to allow for the assessment of 
capture bait performance.

The VCF output looks like:

~~~~{.text}
##fileformat=VCFv4.0
##bam_file=/ABCD_1234/bamfile.bam
##gff_file=/SureSelect_All_Exon_50mb_filtered_baits_1-200_20110524_shoulders.gff3
##FILTER=<ID=LowQual,Description="REQUIRED: QUAL < 50.0">
##INFO=<ID=B,Number=.,Type=String,Description="Bait name">
##INFO=<ID=BE,Number=.,Type=String,Description="Bait end position">
##INFO=<ID=ZC,Number=.,Type=String,Description="bases with Zero Coverage">
##INFO=<ID=NZC,Number=.,Type=String,Description="bases with Non Zero Coverage">
##INFO=<ID=TOT,Number=.,Type=String,Description="Total number of sequenced bases">
#CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO
chr1    1       .       .       .       .       .       B=fill;BE=14166;ZC=12240;NZC=1926;TOT=16840
chr1    14167   .       .       .       .       .       B=bait_3_100;BE=14266;ZC=100;NZC=0;TOT=0
chr1    14267   .       .       .       .       .       B=bait_2_100;BE=14366;ZC=100;NZC=0;TOT=0
chr1    14367   .       .       .       .       .       B=bait_1_100;BE=14466;ZC=100;NZC=0;TOT=0
chr1    14467   .       .       .       .       .       B=bait;BE=14587;ZC=64;NZC=57;TOT=445
chr1    14588   .       .       .       .       .       B=bait_1_100;BE=14638;ZC=0;NZC=51;TOT=1455
chr1    14639   .       .       .       .       .       B=bait;BE=14883;ZC=0;NZC=245;TOT=9763
~~~~

If we look at one output line in more detail we see:

~~~~{.text}
chr1    14467    .    .    .    .    .    B=bait;BE=14587;ZC=64;NZC=57;TOT=445
~~~~

which we interpret as:

* this region starts at position 14467 on chromosome 1 (chr1)
* the region is a bait (B=bait)
* the region ends at position (BE=14587)
* there were 64 bases with zero coverage (ZC=64) and 57 with non-zero coverage (NZC=57)
* the total number of sequenced bases in the region is 445 (TOT=445)

### multi formate output
The multi formate output is allowed but not recomended. Here the VCF format only work with per-feature mode. For example
~~~~{.text}
java -jar qcoverage.java --type physical --per-feature \
--output-format TXT --output-format VCF  --output-format XML \
--output /path/report  --input-gff3 /path/GRCh37_ICGC_standard_v2.gff3 --input-bam /path/input.bam --log /path/report.log

ls /path/report.*

/path/report.log  /path/report.txt /path/report.vcf /path/report.xml

~~~~


## Multithreading

To accelerate performance where hardware permits (e.g: multicore processors;
multi-processor machines), pass the --thread option to specify an appropriate number
of worker threads.  For example, applying --thread 15 to yield 15 worker threads plus 
1 supervising thread:

~~~~{.text}
java -Xmx20G -jar qcoverage.jar --thread 15 --type sequence\
    --input-bam test1.bam --input-gff3 GRCh37_primary_chr13.gff3 --output report --log logfile
~~~~

Adequate node resources must be allocated to ensure speedup when using this
technique on any cluster. It must also be noted that qcoverage uses more memory
when in multithreaded mode.

## Filtering

qcoverage reuses the query engine underpinning
[qbamfilter](../qbamfilter/), allowing the user to include only BAM reads 
satisfying a specified query expression. Like qbamfilter, this query
expression is supplied with the -q,--query option.  Identical query-language
semantics apply to qcoverage as for qbamfilter. A complete specification of
the qbamfilter query language can be found on the [qbamfilter](qBamfilter) page.

The default filter that is used by QCMG when running qcoverage is as follows:

###  sequence coverage

~~~~{.text}
java -jar qcoverage.jar --type sequence\
    --input-bam test1.bam --input-gff3 some.gff3 --output report --log logfile \
    --query "and( flag_ReadFailsVendorQuality==false, flag_DuplicateRead==false, flag_ReadUnmapped==false, flag_NotprimaryAlignment==false)"
~~~~

### physical coverage

~~~~{.text}
java -jar qcoverage.jar --type sequence\
    --input-bam test1.bam --input-gff3 some.gff3 --output report --log logfile
    --query "and( flag_ReadFailsVendorQuality==false, flag_DuplicateRead==false, flag_ReadUnmapped==false, flag_NotprimaryAlignment==false)"
~~~~

The `-q` string specifies that only quality-passed, non-duplicate, mapped,
primary alignment reads are included for the read depth calculations.

## Examples

Some other examples are:

Excluding reads above a defined ISIZE cutoff during physical coverage:

~~~~{.text}
java -jar qcoverage.jar --query "ISIZE < 200" --type physical \
    --input-bam test1.bam --input-gff3 some.gff3 --output report --log logfile
~~~~

Including only reads satisfying a ZP-quality of "AAA":

~~~~{.text}
java -jar qcoverage.jar --query "ZP==AAA" --type sequence\
    --input-bam test1.bam --input-gff3 some.gff3 --output report --log logfile
~~~~

Excluding unpaired reads from physical coverage:

~~~~{.text}
java -jar qcoverage.jar --query "flag_ReadPaired==true" --type physical \
    --input-bam test1.bam --input-gff3 some.gff3 --output report --log logfile
~~~~
