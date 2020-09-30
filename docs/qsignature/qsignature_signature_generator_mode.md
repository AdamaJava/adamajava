# qsignature SignatureGenerator mode

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
java -cp qsignature.jar org.qcmg.sig.SignatureGenerator \ 
                        -log $BAM.qsig.log \
                        -i qsignature_positions.txt \
                        -i $BAM \
                        -i Illumina_arrays_design.txt
~~~~

## Options

* -i REQUIRED - positions file - this is a hg19 based tab delimited text file that contains the positions at which qsignature will report upon. For bam files, a pileup is performed, and for snp array files, the logR ratio is used to determine the ref/alt split
* -i REQUIRED - data file - BAM or snp array txt file (Genome Studio)
* -i REQUIRED - Illumina arrays design text file - contains information on how to treat entries in the snp array files
* -minMappingQuality OPTIONAL - minimum mapping quality (defaults to 10)
* -minBaseQuality OPTIONAL - minimum base quality (defaults to 10)
* -validation OPTIONAL - validation stringency to use when reading BAM files (defaults to STRICT, unless mapped by bwa, in which case SILENT)

## Outputs

VCF file with coverage (either calculated or real) at the positions of
interest.

### Example

~~~~{.text}
##fileformat=VCFv4.0
##patient_id=ABCD_1234
##library=Library_EXT20140505_C
##bam=/bamFile.bam
##snp_file=/qsignature_positions.txt
##filter_q_score=10
##filter_match_qual=10
##FILTER=<ID=LowQual,Description="REQUIRED: QUAL < 50.0">
##INFO=<ID=FULLCOV,Number=.,Type=String,Description="all bases at position">
##INFO=<ID=NOVELCOV,Number=.,Type=String,Description="bases at position from reads with novel starts">
#CHROM  POS     ID      REF     ALT     QUAL    FILTER  INFO
chr1    89788   cnvi0159992     G               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    90900   cnvi0135911     G               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    91152   cnvi0111730     A               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    91467   cnvi0132916     G               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    91472   rs6680825       C               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    91538   cnvi0158801     T               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    91719   cnvi0131353     C               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    98222   cnvi0147298     C               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    99236   cnvi0131297     T               .       .       FULLCOV=A:0,C:0,G:0,T:2,N:0,TOTAL:2;NOVELCOV=A:0,C:0,G:0,T:2,N:0,TOTAL:2
chr1    100622  cnvi0147523     G               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    101095  cnvi0133071     T               .       .       FULLCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0;NOVELCOV=A:0,C:0,G:0,T:0,N:0,TOTAL:0
chr1    102954  cnvi0120648     T               .       .       FULLCOV=A:0,C:0,G:0,T:2,N:0,TOTAL:2;NOVELCOV=A:0,C:0,G:0,T:2,N:0,TOTAL:2
~~~~
