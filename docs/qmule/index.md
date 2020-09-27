# qmule

## Introduction

`qmule` is a collection of small but useful routines for operating on files
associated with next-generation sequencing.  It is a stand-alone java app 
that is a intentionally structured after the [samtools](http://samtools.sourceforge.net) 
model of a single executable where the first commandline parameter picks
which mode to execute, and each mode has it's own collection of commandline
options.  The `qmule` modes are each described on separate wiki pages linked
from the table below.

## Installation

qmule requires java 7 and has different compute and memory requirements deepening on the mode.

To install qmule:

* Download the [qmule tar file](http://sourceforge.net/projects/adamajava/files/qmule.tar.bz2/download)
* Untar the tar file into a directory of your choice

You should see jar files for qmule and its dependencies:

~~~~{.text}
$ tar xjvf qmule.tar.bz2
jopt-simple-3.2.jar
picard-1.110.jar
qcommon-0.1pre.jar
qio-0.1pre.jar
qmule-0.1pre.jar
qpicard-0.1pre.jar
sam-1.110.jar
~~~~


## Usage

~~~~{.text}
qmule class [options]
~~~~

`qmule` modes are invoked by directly naming the class that is to be
executed as can be seen in the example below:

~~~~{.text}
qmule org.qcmg.qmule.AlignerCompare -i file1.bam -i file2.bam -o output.txt
~~~~

## Modes

Mode             | Description
---------------- | ---------------------------
[AlignerCompare](qmule_aligner_compare_mode) | Compare 2 BAMs aligned from the same FASTQ and separate out reads that are different between the BAMs
[BamMismatchCounts](qmule_bam_mismatch_counts_mode) | For reads that mapped full-length, provide a tally of how many mismatches were in each read
[SubSample](qmule_subsample_mode) | Create a new BAM file with a subsample of reads from an existing BAM file 
[MafFilter](qmule_maf_filter_mode) | Search for QCMG-annotated MAF files within a specified directory and apply QCMG-specific filters to produce 2 MAF files - high and low confidence
