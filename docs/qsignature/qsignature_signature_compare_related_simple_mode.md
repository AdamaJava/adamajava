# qsignature SignatureCompareRelatedSimple mode

Once files are converted to qsignature VCF files, we calculate the
qsignature distances between any pair of files. The distance is 
calculated as the average Euclidean distance between every nucleotide
frequency observed at every SNV position:

~~~~{.text}
(∑_(s=1)^S▒∑_(j=1)^J▒√((x_(s,j)-y_(s,j) )^2 ))/S

where:
J = {"A,C,G,T"}
S = {"SNVs"}
~~~~

SNV positions with fewer than 10 observations are excluded from the distance.
Due to the coverage threshold and the position-specific filtering during the
pileups, the subset of SNVs used in any given pairwise comparison will be 
different. If one of the BAM files in the pairwise comparison is from a
capture kit or gene panel, the number of common positions used in the 
qsignature distance calculation can be as few as 200. However, the
qsignature distance is still a robust measure of similarity when all common 
SNVs are used (see Supplementary Data Figure 1). 

A single pairwise VCF comparison takes a few seconds to perform on a single core.

## Options

* -log REQUIRED full path to log file
* -dir REQUIRED full path to directory in which to search for and compare qsignatrue vcf files (*.qsig.vcf)
* -output OPTIONAL full path to output xml file which will contain the comparisons.
* -cutoff OPTIONAL double vlaue representing the cutoff value to use when identifying comparisons that do not match. Defaults to 0.2, which is what is used by QCMG
* -excludes OPTIONAL full path to a file containing a list of qsignature vcf files to exclude from the comparison

## Usage

~~~~{.text}
java -cp qsignature.jar org.qcmg.sig.SignatureCompareRelatedSimple \
        -o xmlFile \
        -log logFile \
        -exclude excludes.file \
        -d suspicious/donor/directory \
        -cutoff 0.175
~~~~

## Output

If the output option is supplied then an XML file containing the comparisons
performed is generated.  An example of the output is shown below:

~~~~{.xml}
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<qsignature>
<files>
<file coverage="652744" id="1" name="/donor_123/SNP_array_1.txt.qsig.vcf"/>
<file coverage="661122" id="2" name="/donor_123/SNP_array_2.txt.qsig.vcf"/>
<file coverage="1065490" id="3" name="/donor_123/bam_1.bam.qsig.vcf"/>
<file coverage="1031504" id="4" name="/donor_123/bam_2.bam.qsig.vcf"/>
<file coverage="1027473" id="5" name="/donor_123/bam_3.bam.qsig.vcf"/>
<file coverage="1037059" id="6" name="/donor_123/bam_4.bam.qsig.vcf"/>
</files>
<comparisons>
<comparison calcs="2579640" file1="1" file2="2" overlap="644910" score="0.016903420196461835"/>
<comparison calcs="2083996" file1="1" file2="3" overlap="520999" score="0.04893314141425636"/>
<comparison calcs="2013096" file1="1" file2="4" overlap="503274" score="0.04948555147871439"/>
<comparison calcs="2015928" file1="1" file2="5" overlap="503982" score="0.049278590204270174"/>
<comparison calcs="2025276" file1="1" file2="6" overlap="506319" score="0.049518496011118"/>
<comparison calcs="2108000" file1="2" file2="3" overlap="527000" score="0.054023471439236456"/>
<comparison calcs="2036068" file1="2" file2="4" overlap="509017" score="0.054682813857119546"/>
<comparison calcs="2039124" file1="2" file2="5" overlap="509781" score="0.05441990528323116"/>
<comparison calcs="2047812" file1="2" file2="6" overlap="511953" score="0.054694059267105716"/>
<comparison calcs="3406144" file1="3" file2="4" overlap="851536" score="0.04856505342038122"/>
<comparison calcs="3405124" file1="3" file2="5" overlap="851281" score="0.0484506294195163"/>
<comparison calcs="3425648" file1="3" file2="6" overlap="856412" score="0.04866983856433887"/>
<comparison calcs="3301288" file1="4" file2="5" overlap="825322" score="0.04876749248103125"/>
<comparison calcs="3321452" file1="4" file2="6" overlap="830363" score="0.04881754231098325"/>
<comparison calcs="3318228" file1="5" file2="6" overlap="829557" score="0.048823772147870516"/>
</comparisons>
</qsignature>
~~~~

The files section contains all the files that were used in the comparisons.
It also informs the user of the number of SNV positions for which data was
present (coverage).

The comparisons section lists the ids of the two files that are being 
compared, along with the comparison score (score), the number of SNV
positions that were used to derive the score (overlap), and the number of
calculations (calcs) that were used to derive the score.
