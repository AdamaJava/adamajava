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

qmule requires java 8 and has different compute and memory requirements deepening on the mode.

* To do a build of qmule, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qsv and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qmule:build
  ~~~~
  This creates the qpileup jar file along with dependent jars in the `qmule/build/flat` folder

## Usage

~~~~{.text}
java -cp qmule.jar <mode class> [options]
~~~~

`qmule` modes are invoked by directly naming the class that is to be
executed as can be seen in the example below:

~~~~{.text}
java -cp qmule.jar org.qcmg.qmule.AlignerCompare -i file1.bam -i file2.bam -o output.txt
~~~~

## Modes

Mode             | Description
---------------- | ---------------------------
[AlignerCompare](qmule_aligner_compare_mode.md) | Compare 2 BAMs aligned from the same FASTQ and separate out reads that are different between the BAMs
[BamMismatchCounts](qmule_bam_mismatch_counts_mode.md) | For reads that mapped full-length, provide a tally of how many mismatches were in each read
[SubSample](qmule_subsample_mode.md) | Create a new BAM file with a subsample of reads from an existing BAM file 
[MafFilter](qmule_maf_filter_mode.md) | Search for QCMG-annotated MAF files within a specified directory and apply QCMG-specific filters to produce 2 MAF files - high and low confidence
[QSamToFASTQ](qmule_sam2fastq_mode.md) | Convert BAM/SAM file to FASTQ file



