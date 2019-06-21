The AdamaJava project holds code for variant callers and pipeline tools related to next-generation sequencing (NGS).  The code was created by members of the Queensland Centre for Medical Genomics ([QCMG](http://www.qcmg.org)) as part of their participation in the International Cancer Genome Consortium ([ICGC](http://www.icgc.org)).  The code is now being developed and maintained by the [Genome Informatics Group](http://www.qimrberghofer.edu.au/lab/genome-informatics/) at the [QIMR Berghofer Medical Research Institute](http://www.qimrberghofer.edu.au).

While the repository contains code for dozens of utilities, the list below shows tools that are considered robust enough to be released for general use.  Most are written in Java and require version 1.7 although some now require version 1.8 or above.  Some tools and helper scripts may be written in other languages, primarily perl and groovy.  Some tools have dependencies on other software.

* [qAmplicon](/p/adamajava/wiki/qAmplicon/) - design primer pairs for variant verification
* [qCnv](/p/adamajava/wiki/qCnv/) - a pre-processor for copy number variant (CNV) calling
* [qMule](/p/adamajava/wiki/qMule/) - collection of utility routines, mostly for operating on BAM files
* [qProfiler](/p/adamajava/wiki/qProfiler/) - summary statistics on BAM files for use in QC assessment
* [qSnp](/p/adamajava/wiki/qSnp/) - heuristics-based tumour/normal single nucleotide variant (SNV) caller 
* [qSv](/p/adamajava/wiki/qsv/) - tumour/normal structural variant (SV) caller 
* [qPileup](/p/adamajava/wiki/qPileup/) - gathers base-level metrics across a whole genome from a group of BAMs
* [qBamfilter](/p/adamajava/wiki/qBamfilter/) - filters records from a BAM file
* [qCoverage](/p/adamajava/wiki/qCoverage/) - calculates coverage statistics given a BAM file with mapped reads
* [qSignature](/p/adamajava/wiki/qSignature/) - a method for detecting potential sample mix-ups using distance measurements between SNP alleles that are common to pairs of samples of interest
* [qMotif](/p/adamajava/wiki/qMotif/) - search for motifs in a BAM file 

Some QCMG tools have been released as separate SourceForge Projects

* [qPure](https://sourceforge.net/projects/qpure/?source=directory)

The wiki uses [Markdown](/p/adamajava/wiki/markdown_syntax/) syntax.
