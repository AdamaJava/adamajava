# qsnp .ini file

## .ini file sections

The qsnp ini files are split into a number of sections

### ids

Property name | Description
------- | -------
donor | Patient for which qsnp is being run
normalSample | sample id pertaining to the control sample
tumourSample | sample id pertaining to the test sample
analysisId | unique id for this analysis (usually a type 4 uuid)

### parameters

Property name | Required | Description
------- | ------- | -----
runMode | yes | indicates the mode in which qsnp should run. Possible values are [standard, vcf, mutect]
annotateMode | no | if set to dcc, will output partially completed dcc files. If left blank, will output VCF file only
filter | no | only used by standard mode when it filters reads from the incoming bam files. If this section is blank, no filtering is performed other than to remove duplicates
noOfRecordsFailingFilter | no | only used by standard mode. If this number of records is reached without a single one passinf the filter, fail. Defaults to 1000000.
numberNovelStarts | no | Specifies the minimum number of novel starts required so that the NNS flag is not applied. Defaults to 4.
numberMutantReads | no | Specifies the minimum number of mutant reads required so that the MR flag is not applied. Defaults to 5.
validation | no | Specifies the validation stringency to be used when parsing the bam files. Possible values are SILENT, STRICT, LENIENT. Defaults to STRICT, unless there is an entry in the header file indicating that bwa was used to map the bam, in which case defaults to SILENT.

### rules

The rules that stipulate whether a position has enough variants to be considered a position of interest are defined in this section.
There are separate rules for the normal (control) bam and for the tumour (test) bam
The rules are in the format: minimum coverage, maximum coverage,number of variants needed.

Example:

~~~~{.text}
normal1=0,20,3
normal2=21,50,4
normal3=51,,10
tumour1=0,20,3
tumour2=21,50,4
tumour3=51,,5
~~~~

Here we are specifying that for the normal bam, if we have coverage between
0 and 20, then we need at least 3 alts. If we have coverage between 21 and
50, we need 4 alts, and if we have 51 and over, we need 10% of reads to 
contain the alt.
The same rules apply for reads from the tumour bam apart from the case where
the coverage is 51 or over, in which case we are after 5% of reads containing
the alt before the position will be considered.

### rules rules###

* As many rules as is appropriate may be specified for each of normal and tumour, as long as each rule begins with either "normal" or "tumour" and is unique.
* If more than 1 rule applies to a position with a certain coverage, then qsnp will exit with an error message
* If some coverage values do not have a valid rule defined, then qsnp will emit a warning, and ignore all positions that have that coverage range.
* If the maximum coverage value is not specified, then it is assumed that it is the Integer.MAX_VALUE, and the number of variants number is then a percentage rather than the actual number of variants.

## inputFiles

Property name | Mode | Required | Description
---|---|---|---
dbSNP | all | no | This file is available in compressed format as 00-All.vcf.gz from the [dbSNP FTP site](ftp://ftp.ncbi.nih.gov/snp/organisms/human_9606/VCF/) You will need to download it and uncompress it to a directory where qSNP can see it
germlineDB | all | no | This is a QCMG-specific VCF file which contains germline SNPs called in other samples. This is used to look for evidence that a somatic SNP appears as a germline SNP in another patient
chrConv | all | yes (if annotateMode is dcc) | This is another QCMG-specific file used to resolve BAM files that have different names for the same sequences. For example the Ensemblv55 sequence "HSCHR1_RANDOM_CTG5" is called "GL000191.1" in the QCMG GRCh37 standard genome, "chr25" by diBayes, and "93" in the ICGC DCC_v0.4. This concordance file is primarily used during creation of the DCC file where an integer identifier is used rather than the versioned RefSeq identifiers used in the QCMG reference genome and BAMs.
ref | standard | yes | This is the reference used to align the bam files
normalBam | all | yes | Control bam file
tumourBam | all | yes | Test bam file
vcfNormal | vcf & mutect | yes | GATK UnifiedGenotyper/HaplotypeCaller output from control bam
vcfTumour | vcf & mutect | yes | GATK UnifiedGenotyper/HaplotypeCaller output from test bam
illuminaNormal | all | no | SNP microarray data from Illumina's GenomeStudio software in text format (TSV). The header from this file is shown in the example immediately below this table so you can check that your SNP data is in an appropriate format. All potential SNPs are checked against this table to look for concordance and if a qSNP called variant is also found in the SNP array, then a code (48?) is added to the validation column of the DCC output file. Note that the Illumina genotypes may not match the qSNP genotypes because of the Illumina TOP/BOT convention so just be aware that a mismatch between BAM and array does not necessarily mean that the genotypes are different. (<patient>/SNP_array/<sample_id><number>.txt)
illuminaTumour | all | no | As for illuminaNormal but for the test sample SNP microarray.

## outputFiles

qsnp outputs a single VCF file containing both somatic and germline mutations.
It can also optionally output files (1 for somatic and another for germline)
that are in a partial dcc format (used by the ICGC).

Property name | Mode | Required | Description
---|---|---|---
vcf | all | yes | full path to the output vcf file
dccSomatic | all | no | full path to the output somatic dcc file
dccGermline | all | no | full path to the output germline dcc file

## Example ini files

### Standard mode

~~~~{.text}
[inputFiles]
dbSNP = /dbSNP/135/00-All.vcf
germlineDB = /qsnp/icgc_germline_qsnp.vcf
chrConv = /qsnp/chromosome_conversions.txt
ref = /genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa
normalBam = /ABCD_1234/control.bam
tumourBam = /ABCD_1234/test.bam
illuminaNormal = /ABCD_1234/SNP_array/control_snp_array.txt
illuminaTumour = /ABCD_1234/SNP_array/test_snp_array.txt

[parameters]
runMode = standard
filter = and (Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)
annotateMode = dcc

[ids]
donor = ABCD_1234
normalSample = QWERTY-XXYY-20130816-028
tumourSample = QWERTY-XXYY-20131107-114
analysisId = c57c66e4_dfad_47ea_a71b_ea37a004e042

[outputFiles]
vcf = /ABCD_1234/variants/qSNP/c57c66e4_dfad_47ea_a71b_ea37a004e042/ABCD_1234.vcf
dccSomatic = /ABCD_1234/variants/qSNP/c57c66e4_dfad_47ea_a71b_ea37a004e042/ABCD_1234.SomaticSNV.dcc1
dccGermline = /ABCD_1234/variants/qSNP/c57c66e4_dfad_47ea_a71b_ea37a004e042/ABCD_1234.GermlineSNV.dcc1

[rules]
normal1=0,20,3
normal2=21,50,4
normal3=51,,10
tumour1=0,20,3
tumour2=21,50,4
tumour3=51,,5
~~~~

### VCF mode

~~~~{.text}
[inputFiles]
vcfNormal = /ABCD_1234/variants/GATK/9ab5efed_a5eb_4158_9b89_e156913450fc/ABCD_1234.Control.vcf
vcfTumour = /ABCD_1234/variants/GATK/9ab5efed_a5eb_4158_9b89_e156913450fc/ABCD_1234.Test.vcf
dbSNP = /dbSNP/135/00-All_chr.vcf
germlineDB = /qsnp/icgc_germline_qsnp.vcf
chrConv = /qsnp/chromosome_conversions.txt
ref = /genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa
normalBam = /ABCD_1234/control.bam
tumourBam = /ABCD_1234/test.bam
illuminaNormal = /ABCD_1234/SNP_array/control_snp_array.txt
illuminaTumour = /ABCD_1234/SNP_array/test_snp_array.txt

[parameters]
runMode = vcf
annotateMode = dcc
minimumBaseQuality=10
pileupOrder=NT

[ids]
donor = ABCD_1234
normalSample = QWERTY-XXYY-20130816-028
tumourSample = QWERTY-XXYY-20131107-114
analysisId = 9ab5efed_a5eb_4158_9b89_e156913450fc

[outputFiles]
vcf = /ABCD_1234/variants/GATK/9ab5efed_a5eb_4158_9b89_e156913450fc/APGI_3507.vcf
dccSomatic = /ABCD_1234/variants/GATK/9ab5efed_a5eb_4158_9b89_e156913450fc/APGI_3507.SomaticSNV.dcc1
dccGermline = /ABCD_1234/variants/GATK/9ab5efed_a5eb_4158_9b89_e156913450fc/APGI_3507.GermlineSNV.dcc1

[rules]
normal1=0,20,3
normal2=21,50,4
normal3=51,,10
tumour1=0,20,3
tumour2=21,50,4
tumour3=51,,5
~~~~
