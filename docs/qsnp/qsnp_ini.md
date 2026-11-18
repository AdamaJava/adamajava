# qsnp .ini file

qsnp is configured with an [INI](http://en.wikipedia.org/wiki/INI_file) file,
which is split into the sections described below. Property names are case
sensitive. Properties that qsnp does not recognise are ignored, so a misspelt
property name is not reported as an error. Check the start of the log file,
which lists the values that qsnp has read.

## ids

Property name | Required | Description
------- | ------- | -------
donor | no | the patient (donor) for which qsnp is being run
controlSample | no | the id of the control (normal) sample
testSample | no | the id of the test (tumour) sample
analysisId | no | a unique id for this analysis (usually a type 4 uuid). Used as the analysis id in the log file and as the prefix of the mutation ids. A uuid is generated if it is not set

## parameters

Property name | Required | Description
------- | ------- | -----
runMode | yes | the mode in which qsnp runs: `standard` or `vcf`. See [modes](qsnp_v2_0.md#modes)
filter | no | standard mode only. A [qbamfilter](../qbamfilter/index.md) query that reads must pass to be used. If blank, no filtering is done other than removing duplicate, unmapped, secondary and supplementary reads
includeDuplicates | no | `true` to use reads that are flagged as duplicates. Defaults to `false`
noOfRecordsFailingFilter | no | standard mode only. If this many records are read without one passing the filter, qsnp fails, as the filter is probably wrong. Defaults to 1000000
numberNovelStarts | no | the minimum number of novel starts required for the NNS filter not to be applied. Defaults to 4
numberMutantReads | no | the minimum number of mutant reads required for the MR filter not to be applied. Defaults to 5
minimumBaseQuality | no | bases with a lower base quality are not used. Defaults to 10
validation | no | the validation stringency used when reading the BAM files: `SILENT`, `LENIENT` or `STRICT`. Defaults to `SILENT`
skipAnnotation | no | set to `SBIASALT` to skip the strand bias annotation
sBiasAltPercentage | no | the percentage used by the alt strand bias (SBIASALT) annotation. Defaults to 5
sBiasCovPercentage | no | the percentage used by the coverage strand bias (SBIASCOV) annotation. Defaults to 5

## rules

The rules decide whether a position has enough evidence of a variant to be a
position of interest. There are separate rules for the control (normal) and
test (tumour) samples. Each rule is in the format:
minimum coverage, maximum coverage, number of variant reads needed.

Example:

~~~~{.text}
control1=0,20,3
control2=21,50,4
control3=51,,10
test1=0,20,3
test2=21,50,4
test3=51,,5
~~~~

For the control sample, at a coverage of 0 to 20 at least 3 variant reads are
needed, at 21 to 50 at least 4, and at 51 and over 10% of the reads must
contain the variant.
The test sample rules are the same, apart from a coverage of 51 or over, where
5% of the reads must contain the variant.

### Rules for rules

* Each rule name must begin with `control` or `test`, and be unique. Rules with any other name (including the `normal` and `tumour` names used by older versions of qsnp) are ignored.
* As many rules as are needed can be given for each of control and test.
* If more than one rule applies to a coverage, qsnp exits with an error.
* If no rule covers a coverage, qsnp logs a warning and ignores positions with that coverage.
* If the maximum coverage is left blank, it is unlimited, and the number of variant reads becomes a percentage of the coverage.

## inputFiles

Property name | Mode | Required | Description
---|---|---|---
ref | standard | yes | the reference FASTA file that the BAM files were aligned to
testBam | standard | yes | the test (tumour) BAM. May be given more than once, in which case the BAMs are read as a single sample
controlBam | standard | no | the control (normal) BAM. May be given more than once. If there is no control BAM, qsnp runs in single sample mode
testVcf | vcf | yes | the test (tumour) VCF from GATK HaplotypeCaller (or a similar caller)
controlVcf | vcf | no | the control (normal) VCF. If there is no control VCF, qsnp runs in single sample mode
testBam | vcf | no | if given, the contig order in its header is used to sort the output. Otherwise GRCh37 order is assumed

## outputFiles

qsnp writes a single VCF file containing both the somatic and germline variants.

Property name | Mode | Required | Description
---|---|---|---
vcf | all | yes | full path to the output VCF file

## Example ini files

### Standard mode

~~~~{.text}
[inputFiles]
ref = /genomes/GRCh38/GRCh38.fa
controlBam = /ABCD_1234/control.bam
testBam = /ABCD_1234/test.bam

[parameters]
runMode = standard
filter = and (Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)

[ids]
donor = ABCD_1234
controlSample = QWERTY-XXYY-20130816-028
testSample = QWERTY-XXYY-20131107-114
analysisId = c57c66e4-dfad-47ea-a71b-ea37a004e042

[outputFiles]
vcf = /ABCD_1234/variants/qSNP/c57c66e4-dfad-47ea-a71b-ea37a004e042/ABCD_1234.vcf

[rules]
control1=0,20,3
control2=21,50,4
control3=51,,10
test1=0,20,3
test2=21,50,4
test3=51,,5
~~~~

### VCF mode

~~~~{.text}
[inputFiles]
controlVcf = /ABCD_1234/variants/GATK/9ab5efed-a5eb-4158-9b89-e156913450fc/ABCD_1234.control.vcf
testVcf = /ABCD_1234/variants/GATK/9ab5efed-a5eb-4158-9b89-e156913450fc/ABCD_1234.test.vcf
testBam = /ABCD_1234/test.bam

[parameters]
runMode = vcf

[ids]
donor = ABCD_1234
controlSample = QWERTY-XXYY-20130816-028
testSample = QWERTY-XXYY-20131107-114
analysisId = 9ab5efed-a5eb-4158-9b89-e156913450fc

[outputFiles]
vcf = /ABCD_1234/variants/GATK/9ab5efed-a5eb-4158-9b89-e156913450fc/ABCD_1234.vcf

[rules]
control1=0,20,3
control2=21,50,4
control3=51,,10
test1=0,20,3
test2=21,50,4
test3=51,,5
~~~~
