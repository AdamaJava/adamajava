# qbasepileup

## Introduction

`qbasepileup` performs base pileup on positions of interest in a BAM
file and produces base coverage information and various others
metrics on the reads at the positions of interest.

## Installation

qbasepileup requires java 7 and (ideally) a multi-core machine (5 threads 
are run concurrently) with at least 20GB of RAM.
Download the qbasepileup tar file
Untar the tar file into a directory of your choice
You should see jar files for qbasepileup and its dependencies:

~~~~{.text}
$ tar xjvf qbasepileup.tar.bz2
x antlr-3.2.jar
x ini4j-0.5.2-SNAPSHOT.jar
x jopt-simple-3.2.jar
x picard-1.110.jar
x qbamfilter-1.0pre.jar
x qcommon-0.1pre.jar
x qio-0.1pre.jar
x qpicard-0.1pre.jar
x qpileup-0.1pre.jar
x sam-1.110.jar
x jhdfobj.jar
x jhdf5obj.jar
x jhdf5.jar
x jhdf.jar
~~~~

## Usage

A general invocation of qbasepileup looks like:

~~~~{.text}
java -jar qbasepileup.jar [OPTIONS]
~~~~

A typical invocation might look like:

~~~~{.text}
java -jar qbasepileup.jar -m mode \
    -i file.bam -r ref.fa -s positions.txt -o pileup.txt --log file.log
~~~~

## Options

~~~~{.text}
--help, -h      Show help message.
--version       Print version.
-b              Path to tab delimited file with list of bams.
--bq            Minimum base quality score for accepting a read.
--dup           Include duplicates
-f              Format of SNPs file [ddc1,maf,db,dccq,vcf,torrent,RNA,DNA], Def=dcc1
--filter        Query string for qbamfilter
--gatk          Adjust insertion position to conform to GATK format
--hdf           HDF file to read list of bam files from
--hp            window around indel to check for homopolymers  [default:10]
-i              Path to bam file.
--ig            Path to somatic indel file
--in            Path to normal bam file
--ind           Include reads with indels. (y or n, default y).
--intron        Include reads mapping across introns. (y or n, default y).
--is            Path to somatic indel file
--it            Path to tumour bam file
--log           Req, Log file.
--loglevel      Logging level required, e.g. INFO, DEBUG. Default INFO.
-m              snp,indel,coverage or compoundsnp
--maxcov        Report reads that are less than the mininmum coverage option. Integer
--mincov        Report reads that are less than the
--mq            Minimum mapping quality score for accepting a read.
-n              Bases around indel to check for other indels., Def=3.
--novelstarts   Report novelstarts rather than read count [Y,N], Def=Y.
-o              Output file.
--of            Output file format [rows,columns].
--og            Output file for germline indels.
--os            Output file for somatic indels.
-p              Pileup profile type [ddc1,maf,db,dccq,vcf,torrent,RNA,DNA] Def=dcc1.
--pd            <pindel_deletions> Path to normal bam file
--pindel        adjust insertion position to conform to pindel format
-r              Path to reference genome fasta file.
-s              Path to tab delimited file containing snps. Formats: dcc1,dccq,vcf,maf,txt
--sc            <soft_clip_window> number of bases around indel to check for softclipped bases [default:13]
--strand        Separate coverage by strand. (y or n, default y)
--strelka       adjust insertion position to conform to strelka format (same as pindel)
-t              Thread number. Total = number supplied + 2.
~~~~


### `-n`

This integer is the number of bases around and indel to check for nearby
indel.  Default=3.

### `--pd`

<pindel_deletions> Path to normal bam file

## Modes

### [snp](qbasepileup_snp_mode)

Reads one or more BAM files, a reference genome, and a file containing
positions of SNPs. It finds the reference genome base at the SNP position 
as well as the bases found at that position in all reads aligned to that
region. Coverage per nucleotide is reported and the total coverage at that
position is reported. By default, duplicates and unmapped reads are excluded.

### [compoundsnp](qbasepileup_compound_snp_mode)

Reads one or more BAM files, a reference genome, and a file containing 
positions of compound SNPs (SNPs that sit next to each other). It finds the
reference genome base at the compound SNP positions as well as the bases 
found at that position in all reads aligned to that region. Coverage per 
nucleotide is reported and the total coverage at that position is reported. 

By default, the `--filter` qbamfilter query string is:

~~~~{.text}
and( Flag_DuplicateRead==false, CIGAR_M>34, MD_mismatch <= 3, option_SM > 10)
~~~~

For a more detailed description of qbamfilter and how it works to filter
reads in and out of a particular analysis, see
[qbamfilter](../qbamfilter/).

### [indel](qbasepileup_indel_mode)

Reads tumour and normal BAM files, a reference genome, and somatic and/or 
germline files containing positions of indels and pileups the reads around 
the indel to count a number of metrics. Metrics include:

* total reads 
* number of reads that span the indel
* number of reads with the indel
* number of novel starts with the indel
* number of reads with nearby soft clipping
* number of reads with nearby indels

### coverage

Reads one or more BAM files, and a file containing reference ranges and
piles up the reads around the indel to count the number of reads covering
each position in the range.

