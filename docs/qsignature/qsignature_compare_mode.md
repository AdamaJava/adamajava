# qsignature Compare mode

Once files are converted to qsignature VCF files, we calculate the
qsignature score between any pair of files. 
The score is based on the number of times a similar genotype is observed at every SNV position, 
divided by the overlapping number of SNV positions.
The higher the score, the more similar the files are.

SNV positions with fewer than 10 observations are excluded from the distance.
Due to the coverage threshold and the position-specific filtering during the
pileups, the subset of SNVs used in any given pairwise comparison will be 
different. If one of the BAM files in the pairwise comparison is from a
capture kit or gene panel, the number of common positions used in the 
qsignature distance calculation can be as few as 200. However, the
qsignature distance is still a robust measure of similarity when all common 
SNVs are used. 

A single pairwise VCF comparison takes a few seconds to perform on a single core.

## Options

* -log REQUIRED full path to log file
* -dir REQUIRED full path to directory in which to search for and compare qsignatrue vcf files (*.qsig.vcf)
* -output OPTIONAL full path to output xml file which will contain the comparisons.
* -excludes OPTIONAL full path to a file containing a list of qsignature vcf files to exclude from the comparison

## Usage

~~~~{.text}
java -cp qsignature.jar org.qcmg.sig.Compare \
        -output xmlFile \
        -log logFile \
        -exclude excludes.file \
        -dir suspicious/donor/directory \
~~~~

## Output

If the output option is supplied then an XML file containing the comparisons
performed is generated.  An example of the output is shown below:

~~~~{.xml}
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<qsignature>
<cutoffs hom="0.9" lower_het="0.3" upper_het="0.7"/>
<files>
<file coverage="652744" id="1" name="/donor_123/SNP_array_1.txt.qsig.vcf"/>
<file coverage="661122" id="2" name="/donor_123/SNP_array_2.txt.qsig.vcf"/>
<file coverage="1065490" id="3" name="/donor_123/bam_1.bam.qsig.vcf"/>
<file coverage="1031504" id="4" name="/donor_123/bam_2.bam.qsig.vcf"/>
<file coverage="1027473" id="5" name="/donor_123/bam_3.bam.qsig.vcf"/>
<file coverage="1037059" id="6" name="/donor_123/bam_4.bam.qsig.vcf"/>
</files>
<comparisons>
<comparison file1="1" file2="2" overlap="644910" score="0.9903420196461835"/>
<comparison file1="1" file2="3" overlap="520999" score="0.993314141425636"/>
<comparison file1="1" file2="4" overlap="503274" score="0.948555147871439"/>
<comparison file1="1" file2="5" overlap="503982" score="0.9278590204270174"/>
<comparison file1="1" file2="6" overlap="506319" score="0.5518496011118"/>
<comparison file1="2" file2="3" overlap="527000" score="0.9023471439236456"/>
<comparison file1="2" file2="4" overlap="509017" score="0.9682813857119546"/>
<comparison file1="2" file2="5" overlap="509781" score="0.941990528323116"/>
<comparison file1="2" file2="6" overlap="511953" score="0.5694059267105716"/>
<comparison file1="3" file2="4" overlap="851536" score="0.956505342038122"/>
<comparison file1="3" file2="5" overlap="851281" score="0.94506294195163"/>
<comparison file1="3" file2="6" overlap="856412" score="0.566983856433887"/>
<comparison file1="4" file2="5" overlap="825322" score="0.976749248103125"/>
<comparison file1="4" file2="6" overlap="830363" score="0.581754231098325"/>
<comparison file1="5" file2="6" overlap="829557" score="0.5823772147870516"/>
</comparisons>
</qsignature>
~~~~

The files section contains all the files that were used in the comparisons.
It also informs the user of the number of SNV positions for which data was
present (coverage).

The comparisons section lists the ids of the two files that are being 
compared, along with the comparison score (score), and the number of SNV
positions that were used to derive the score (overlap).
