# qsignature

`qsignature` is a simple and highly effective system for detecting potential
sample mix-ups using distance measurements between SNV alleles that are
common to pairs of samples of interest. qsignature can use both genotyping
array and high-throughput sequencing data to detect potential mix-ups and
can be applied across multiple sequencing platforms and experiment types.

qsignature functions in two modes:

* mode 1 processes a genotyping array file or BAM file to create a summary file in a format based on the Variant Call Format [VCF](http://samtools.github.io/hts-specs/VCFv4.1.pdf). This file summarizes the intensity- or read-based SNP data for a given sample and only needs to be run once per genotype or BAM file;
* mode 2 performs a pairwise comparison between two or more qsignature mode 1 VCF files to calculate qsignature distance metrics between the samples.

## Installation

qsignature requires java 7.

* Download the [qsignature tar file](http://sourceforge.net/projects/adamajava/files/qsignature.tar.bz2/download)
* Untar the tar file into a directory of your choice

You should see jar files for qsignature and its dependencies:

~~~~{.text}
$ tar xvf qsignature.tar.bz2
x commons-math3-3.1.1.jar
x jopt-simple-3.2.jar
x picard-1.110.jar
x qcommon-0.1pre.jar
x qio-0.1pre.jar
x qpicard-0.1pre.jar
x sam-1.110.jar
x Illumina_arrays_design.txt
x qsignature_positions.txt
~~~~

## Usage

qsignature modes are invoked by directly naming the class that is to be executed as can be seen in the example below:

~~~~{.text}
java -cp qsignature-0.1pre.jar org.qcmg.sig.SignatureGenerator -i <input> -o <output>
~~~~

## Modes

Mode | Description
-----| -----------
[`SignatureGenerator`](qsignature_signature_generator_mode) | Generates a compressed VCF file (.qsig.vcf.gz) for a BAM or snp chip file
[`SignatureCompareRelatedSimple`](qsignature_signature_compare_related_simple_mode) | Compares all of the qsig.vcf.gz files in a specified folder to ascertain whether there is a potential mixup
