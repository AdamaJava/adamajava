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

qsignature requires java 8.

* **To do a build of qsignature, first clone the adamajava repository using "git clone":**
  ```
  git clone https://github.com/AdamaJava/adamajava
  ```

  Then move into the adamajava folder:
  ```
  cd adamajava
  ```
  Run gradle to build qsignature and its dependent jar files:
  ```
  ./gradlew :qsignature:build
  ```
  This creates the qsignature jar file along with dependent jars in the `qsignature/build/flat` folder


## Usage

qsignature modes are invoked by directly naming the class that is to be executed as can be seen in the example below:

~~~~{.text}
java -cp qsignature.jar org.qcmg.sig.Generate -snpPositions <file containing positions of interest> -input <input BAM file> -output <output directory where qsig.vcf files will be generated> -log <log file>

~~~~

## Modes

Mode | Description
-----| -----------
[`Generate`](qsignature_signature_generator_bespoke_mode.md) | Generates a compressed VCF file (.qsig.vcf.gz) for a BAM or snp chip file
[`Compare`](qsignature_compare_mode.md) | Compares all of the qsig.vcf.gz files in a specified folder to ascertain whether there is a potential mixup
