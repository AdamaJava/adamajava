# qsignature SignatureGeneratorBespoke mode

In order to define a set of SNVs that would be common across all major
sequencing and aray platform, we selected all single-base dbSNP-derived
SNPs included on the (2015) OMNI-1Mquad genotyping array
(~1.4 million SNPs). These SNVs are common to other members of the Illumina
OMNI array family as well as whole genome data and some regions of exome
data and targeted gene panels.
The qsignature test determines the nucleotide frequencies at each of the
SNV positions using spot intensities for genotyping microarrays and base
pileups for BAM files.

Genotyping array intensities are transformed into relative nucleotide
counts using the following formula:

~~~~{.text}
T = ⌊C⋅e^LRR ⌋ 
A = ⌊BAF⋅T⌋ 
R = T-A 

T = total counts
A = alternate allele count
R = reference allele count
C = pseudocount,20
LRR = logR ratio
BAF = B-allele frequency
~~~~

To calculate nucleotide frequencies from BAM reads, we perform a pileup at
each of the selected SNV positions and report the total count of each 
nucleotide from reads that have a mapping quality of at least 10; a base
quality of at least 10; have passed the vendor check; are the primary
alignment; and are not a duplicate read.

VCF generation takes about 20 minutes on a single core to report nucleotide
counts from 500 million reads and less than a minute to estimate counts from
a genotype array. This step needs to be performed only once per file.

## Usage

~~~~{.text}
java -cp qsignature.jar org.qcmg.sig.SignatureGeneratorBespoke \ 
                        -log $BAM.qsig.log \
                        -snpPositions qsignature_positions.txt \
                        -input $BAM \
                        -illuminaArraysDesign Illumina_arrays_design.txt
~~~~

## Options

* -snpPositions REQUIRED - positions file - this is a hg19 based tab delimited text file that contains the positions at which qsignature will report upon. For bam files, a pileup is performed, and for snp array files, the logR ratio is used to determine the ref/alt split
* -input REQUIRED - data file - BAM or snp array txt file (Genome Studio)
* -log REQUIRED - output file containing logging information
* -illuminaArraysDesign OPTIONAL (REQUIRED if running against snp array txt file)- Illumina arrays design text file - contains information on how to treat entries in the snp array files
* -minMappingQuality OPTIONAL - minimum mapping quality (defaults to 10)
* -minBaseQuality OPTIONAL - minimum base quality (defaults to 10)
* -validation OPTIONAL - validation stringency to use when reading BAM files (defaults to STRICT, unless mapped by bwa, in which case SILENT)

## Outputs

VCF file with coverage (either calculated or real) at the positions of
interest.

### Example

~~~~{.text}
##fileformat=VCFv4.2
##datetime=2021-03-09T10:52:34.073
##program=SignatureGeneratorBespoke
##version=58-1f6355ea
##java_version=1.8.0_152
##run_by_os=Linux
##run_by_user=cromwelltst
##snp_positions=qsignature_positions.txt
##gene_positions=null
##reference=null
##positions_md5sum=d18c99f481afbe04294d11deeb418890
##positions_count=1456203
##filter_base_quality=10
##filter_mapping_quality=10
##illumina_array_design=null
##cmd_line=SignatureGeneratorBespoke --snpPositions qsignature_positions.txt -input 55c7fbcf-439d-4058-9f55-6bd2d55127f5.bam -log 55c7fbcf-439d-4058-9f55-6bd2d55127f5.bam.qsig.vcf.log --output 55c7fbcf-439d-4058-9f55-6bd2d55127f5.bam.qsig.vcf.gz --validation SILENT
##INFO=<ID=QAF,Number=.,Type=String,Description="Lists the counts of As-Cs-Gs-Ts for each read group, along with the total">
##input=/working/genomeinfo/cromwell-test/cromwell-executions/somaticDnaFastqToMaf/84361977-6637-4bae-b5b7-b49f00473273/call-controlQsigGen/inputs/-51614194/55c7fbcf-439d-4058-9f55-6bd2d55127f5.bam
##rg0=null
##rg1=13fbd3ce-9667-49b4-8936-1b7a02648bf0
##rg2=cb006bf1-0636-4c3d-9877-6319db04fa3f
#CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO
chr1    696838  .       A       .       .       .       QAF=t:8-0-0-0,rg1:6-0-0-0,rg2:2-0-0-0
chr1    725060  .       A       .       .       .       QAF=t:5-0-0-0,rg1:4-0-0-0,rg2:1-0-0-0
chr1    725737  .       T       .       .       .       QAF=t:0-0-0-2,rg1:0-0-0-1,rg2:0-0-0-1
chr1    725908  .       A       .       .       .       QAF=t:5-0-0-0,rg1:1-0-0-0,rg2:4-0-0-0
chr1    726060  .       T       .       .       .       QAF=t:0-0-0-1,rg2:0-0-0-1
chr1    726224  .       A       .       .       .       QAF=t:3-0-0-0,rg1:2-0-0-0,rg2:1-0-0-0
chr1    727037  .       A       .       .       .       QAF=t:9-0-0-0,rg1:7-0-0-0,rg2:2-0-0-0
chr1    823451  .       T       .       .       .       QAF=t:1-0-0-91,rg1:1-0-0-44,rg2:0-0-0-47
chr1    882803  .       A       .       .       .       QAF=t:0-0-31-0,rg1:0-0-14-0,rg2:0-0-17-0
chr1    883899  .       T       .       .       .       QAF=t:0-0-2-80,rg1:0-0-1-44,rg2:0-0-1-36
chr1    1223621 .       G       .       .       .       QAF=t:0-3-62-0,rg1:0-2-33-0,rg2:0-1-29-0
chr1    1223728 .       C       .       .       .       QAF=t:0-30-0-0,rg1:0-14-0-0,rg2:0-16-0-0
chr1    1223837 .       C       .       .       .       QAF=t:0-39-0-0,rg1:0-19-0-0,rg2:0-20-0-0
chr1    1223956 .       T       .       .       .       QAF=t:0-0-1-42,rg1:0-0-0-25,rg2:0-0-1-17
~~~~
