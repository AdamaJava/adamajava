The AdamaJava project holds code for variant callers and pipeline tools related to next-generation sequencing (NGS).
The code was created by members of the Queensland Centre for Medical
Genomics at The University of Queensland as part of their participation in the International Cancer Genome Consortium ([ICGC](http://www.icgc.org)).

The code is now being developed and maintained by the [Genome Informatics](https://www.qimrberghofer.edu.au/our-research/cancer/genome-informatics/) and [Medical Genomics](https://www.qimrberghofer.edu.au/our-research/cancer/medical-genomics/) groups at the [QIMR Berghofer Medical Research Institute](https://www.qimrberghofer.edu.au).
Documentation for the project is maintained in Markdown format and committed in the docs directory. To view the documentation, see the [AdamaJava project](https://adamajava.readthedocs.io/en/latest) at [Read the Docs](https://readthedocs.org/).

While the repository contains code for dozens of utilities, the list below shows tools that are considered robust enough to be released for general use.
Most are written in Java and require version 21.
Some tools have dependencies on other software applications and libraries.

* qAmplicon - design primer pairs for variant verification
* qCnv - a pre-processor for copy number variant (CNV) calling
* qMule - collection of utility routines, mostly for operating on BAM files
* qProfiler - summary statistics on BAM files for use in QC assessment
* qSnp - heuristics-based tumour/normal single nucleotide variant (SNV) caller 
* qSv - tumour/normal structural variant (SV) caller 
* qPileup - gathers base-level metrics across a whole genome from a group of BAMs
* qBamfilter - filters records from a BAM file
* qCoverage - calculates coverage statistics given a BAM file with mapped reads
* qSignature - a method for detecting potential sample mix-ups using distance measurements between SNP alleles that are common to pairs of samples of interest
* qMotif - search for motifs in a BAM file 

Some tools developed at QCMG have been released as separate projects:

* [qPure](https://sourceforge.net/projects/qpure)

Some related tools may be written in other languages and exist in other repositories including:

* [adamaperl](https://github.com/AdamaJava/adamaperl)
* [adamar](https://github.com/AdamaJava/adamar)
