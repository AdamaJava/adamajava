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

qcoverage requires java 8 and (ideally) a multi-core machine with at least 20GB of RAM.



Download the [qcoverage tar file](http://sourceforge.net/projects/adamajava/files/qcoverage.tar.bz2/download)
Untar the tar file into a directory of your choice

You should see jar files for qcoverage and its dependencies:

~~~~{.text}
[oholmes@minion0 qcoverage]$ tar xjvf qcoverage.tar.bz2
x antlr-3.2.jar
x jopt-simple-3.2.jar
x picard-1.110.jar
x qbamfilter-1.0pre.jar
x qcommon-0.1pre.jar
x qpicard-0.1pre.jar
X qio-0.1pre.jar
x qcoverage-0.7pre.jar
x sam-1.110.jar
[oholmes@minion0 qcoverage]$
~~~~

## Usage

For sequence read depth:

~~~~{.text}
java -jar qcoverage.jar -t seq \
    --bam test1.bam --gff3 GRCh37.gff3 -o report.vcf --log logfile
~~~~

For physical read depth:

~~~~{.text}
java -jar qcoverage.jar -t phys \
    --bam test1.bam --gff3 GRCh37.gff3 -o report.vcf --log logfile
~~~~



## Options

~~~~{.text}
--help, -h      Shows this help message.               
--version, -v   Print version.                    
--bam           Input BAM file.
--bai           Index file for --bam.
--gff3          The GFF3 file defining the features.   
--log           File where log output will be directed (must have write permissions).       
--loglevel      Logging level [INFO, DEBUG], Def=INFO. 
-n              Opt, Number of worker threads (yields n+1 total threads), Def=1.
--output, -o    Req, Output file.
--per-feature   Opt, Perform per-feature coverage.
--query, -q     Opt, The query string for selecting reads for coverage.
--type, -t      The type of coverage to perform [seq, sequence, phys, physical].
--xml           Opt, Output report in XML format.
--vcf           Opt, Output report in VCF format. Needs the per-feature flag to also be set.
~~~~

## VCF output

It is possible to have output from the --per-feature mode in either XML or
VCF format. The XML format is very extensive with an XML table for each
feature showing a full breakdown of how many bases of the feature were 
covered at what levels. For long regions with variable coverage, this table
can run to hundreds of lines so an XML-format output file containing 
hundreds of thousands of features can run to tens of millions of lines - it
can be as big as the BAM itself. In most cases, the level of detail provided
by the XML output is overkill and the VCF format is sufficient.

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

## Multithreading

To accelerate performance where hardware permits (e.g: multicore processors;
multi-processor machines), pass the -n option to specify an appropriate number
of worker threads.  For example, applying -n 15 to yield 15 worker threads plus 
1 supervising thread:

~~~~{.text}
java -Xmx20G -jar qcoverage.jar -n 15 -t seq \
    --bam test1.bam --gff3 GRCh37_primary_chr13.gff3 -o report.txt --log logfile
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
java -jar qcoverage.jar -t seq \
    -bam test1.bam --gff3 some.gff3 -o report.txt --log logfile \
    -q "and( flag_ReadFailsVendorQuality==false, flag_DuplicateRead==false, flag_ReadUnmapped==false, flag_NotprimaryAlignment==false)"
~~~~

### physical coverage

~~~~{.text}
java -jar qcoverage.jar -t seq \
    -bam test1.bam --gff3 some.gff3 -o report.txt --log logfile
    -q "and( flag_ReadFailsVendorQuality==false, flag_DuplicateRead==false, flag_ReadUnmapped==false, flag_NotprimaryAlignment==false)"
~~~~

The `-q` string specifies that only quality-passed, non-duplicate, mapped,
primary alignment reads are included for the read depth calculations.

## Examples

Some other examples are:

Excluding reads above a defined ISIZE cutoff during physical coverage:

~~~~{.text}
java -jar qcoverage.jar -q "ISIZE < 200" -t phys \
    --bam test1.bam --gff3 some.gff3 -o report.txt --log logfile
~~~~

Including only reads satisfying a ZP-quality of "AAA":

~~~~{.text}
java -jar qcoverage.jar --query "ZP==AAA" -t seq \
    --bam test1.bam --gff3 some.gff3 -o report.txt --log logfile
~~~~

Excluding unpaired reads from physical coverage:

~~~~{.text}
java -jar qcoverage.jar --query "flag_ReadPaired==true" -t phys \
    --bam test1.bam --gff3 some.gff3 -o report.txt --log logfile
~~~~
