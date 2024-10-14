# qmotif v1.2

`qmotif` searches BAM sequencing records for user-specified nucleotide 
motifs. It 
produces a detailed XML output file outlining the types and counts of 
matching motifs found and directs all matching reads to an output BAM file 
to allow for more detailed analysis.  In regular mode it tests every read 
in the input BAM, but it also has a mode where it only looks in selected 
genomic regions.  In cases where the search space can be constrained, this
"fast" mode can cut run-times for whole genome files from hours to seconds.

qmotif was created to search for reads that contained canonical
and degenerate forms of the telomeric TTAGGG repeat.

## Installation

qmotif requires java 21 and (ideally) a multicore machine with at least 
20GB of RAM.  To install:

* Download the [qmotif tar file](https://sourceforge.net/projects/adamajava/files/qmotif-1.2.tar.bz2/download).
* Untar the tar file into a directory of your choice.
* Invoke java with the qmotif jar file

When you uncompress the qmotif distribution file, you should see java jar files for qmotif and its dependencies:

~~~~{.text}
$ tar -xjvf qmotif-1.2.tar.bz2
x antlr-3.2.jar
x htsjdk-1.140.jar
x ini4j-0.5.2-SNAPSHOT.jar
x jopt-simple-4.6.jar
x picard-lib.jar
x qbamfilter-1.2.jar
x qcommon-0.2.jar
x qio-0.1pre.jar
x qmotif-1.2.jar
x qpicard-1.1.jar
x trove-3.1a1.jar
~~~~

The `qmotif-1.2.jar` jar file contains the qmotif code and the other jar
files contain libraries that are used by qmotif.

## Usage

~~~~{.text}
java -Xmx20g -jar qmotif-1.2.jar \
    -n 8 \
    --bam /mydata/bamFile.bam \
    --bai /mydata/bamFile.bam.bai \
    --log /mydata/bamFile.bam.qmotif.log \
    --loglevel DEBUG \
    -ini /mydata/qmotif.ini \
    -o /mydata/bamFile.bam.qmotif.xml \
    -o /mydata/bamFile.telomere.bam
~~~~

## Options

The following command line options are supported:

| Option   | Required | Description                                                                                                                                                                                                                                                                                                                   |
|----------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| i        | Yes      | Configuration file in INI format.  This file is designed to contain parameters that are likely to be fixed for a given type of analysis.  So the general idea is that you test parameters in the INI file until you are happy and then you use that single INI file with all of the BAM files that are part of this analysis. |
| bam      | Yes      | BAM file on which to operate.                                                                                                                                                                                                                                                                                                 |
| bai      | Yes      | Index file for the BAM file being processed.                                                                                                                                                                                                                                                                                  |
| log      | Yes      | Log file.                                                                                                                                                                                                                                                                                                                     |
| loglevel | No       | Level of verbosity of logging.  The default level is INFO and valid values are INFO, DEBUG.                                                                                                                                                                                                                                   |
| n        | No       | Number of threads to use when running qmotif. Defaults to 1.                                                                                                                                                                                                                                                                  |
| o        | Yes      | Output XML file containing the motifs found and the regions they were found in.                                                                                                                                                                                                                                               |
| o        | Yes      | Output BAM file containing the reads that matched the stage1 search regex/string                                                                                                                                                                                                                                              |

## Config file

The qmotif config file is in the standard INI-file format and has 3 sections,
PARAMS, INCLUDES, and EXCLUDES.  The PARAMS sections lists the search 
criteria along with other customisable parameters, the INCLUDES section 
contains a list of genomic regions that are to be targeted for analysis, and
the EXCLUDES section contains a list of genomic regions that are to be 
excluded from analysis.

Lines can be commented out by prepending a `;` character as can be seen in 
the example below where a number of different stage1 and stage2 motif
parameters appear but only one of each type is ''not'' commented out.

An example ini file follows:

~~~~{.text}
[PARAMS]
stage1_motif_string=TTAGGGTTAGGGTTAGGG
;stage1_motif_string=TTAGGGTTAGGGTTAGGGTTAGGG
stage2_motif_regex=(...GGG){2,}|(CCC...){2,}
stage1_string_rev_comp=true
window_size=10000

[INCLUDES]
; name    regions (sequence:start-stop)
chr1p   chr1:10001-12464
chr1q   chr1:249237907-249240620
chr2p   chr2:10001-12592
chr2q   chr2:243187373-243189372
chr2xA  chr2:243150480-243154648
chr3p   chr3:60001-62000
chr3q   chr3:197960430-197962429
chr3xB  chr3:197897576-197903397
chr4p   chr4:10001-12193
chr4q   chr4:191041613-191044275
chr5p   chr5:10001-13806
chr5q   chr5:180903260-180905259
chr6p   chr6:60001-62000
chr6q   chr6:171053067-171055066

[EXCLUDES]
; regions (sequence:start-stop)
;chr1:143274114-143274336
~~~~

This table shows the parameters that can be specified in `[PARAMS]` :

| Parameter              | Example                          | Description                                                                                                                                                                                                                                                    |
|------------------------|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| stage1_motif_string    | TTAGGGTTAGGG                     | String that reads are matched against in the stage 1 match.  More than one string can be specified separated by commas.                                                                                                                                        |
| stage1_motif_regex     | (...GGG){2,}&verbar;(CCC...){2,} | Regular expression that reads are matched against in the stage 1 match.                                                                                                                                                                                        |
| stage2_motif_string    | TTAGGGTTAGGG                     | As per `stage1_motif_string` but for the second stage matching.                                                                                                                                                                                                |
| stage2_motif_regex     | (...GGG){2,}&vert;(CCC...){2,}   | As per `stage1_motif_regex` but for the second stage matching.                                                                                                                                                                                                 |
| stage1_string_rev_comp | true                             | If this parameter is set to true then the reverse complement is also matched for any motifs specified in `stage1_motif_string`.  It has no effect if the `_regex` parameters are used so any regex should include the reverse complement if you care about it. |
| stage2_string_rev_comp | true                             | If this parameter is set to true then the reverse complement is also matched for any motifs specified in `stage2_motif_string`.  It has no effect if the `_regex` parameters are used so any regex should include the reverse complement if you care about it. |
| window_size            | 10000                            | For GENOMIC regions (see below), this is the size of the windows that are considered for reporting.  If window_size is not specified, the default value is 10000.                                                                                              |

The `_string` and `_regex` parameters are mutually exclusive within a stage,
i.e. you can set either `stage1_motif_string` or `stage1_motif_regex` but not both.

## How it works

Every single read in the BAM is put through the stage1 match (string or
regex). If the read passes the stage1 match, we decide which region of 
the BAM the read falls within and depending on the type of region (see 
table below for a discussion of region types), the read may or may not 
go on to stage2 matching.  If the read passes the stage1 match, the 
`stage1Cov` tally for the region is incremented.

Stage2 involves another match against the read sequence. Again, the match
could be against a string or a regex but this time, the actual matches are
retrieved and a tally is kept for each region of how many of which motifs
were seen in reads from that region. If the read passes the stage2 match,
the `stage2Cov` tally for the region is incremented.

The 2-stage matching system was specifically designed to help us with speed
so in practice, we always use quick string matching for stage1 and only 
reads that pass stage1 (potentially) go on to the much slower regex match
in stage2.  There is no reason why you _must_ do it this way but this is
how we designed the system.  This is worth understanding because if you 
decide to use a stage1 regex on a large BAM, your runtimes could blow out
significantly.  And if you use string matching in stage2, your tally of 
motifs will of course be very simple.

The reference genome is split into regions of 4 different types: INCLUDES,
EXCLUDES, UNMAPPED, and GENOMIC.  Depending on the region, mapped reads 
or unmapped reads or both types may be considered for stage2 matching.  
All reads are always put through stage1 so region type only becomes a 
factor for stage2 matching.

| Region Type | Reads passed to stage2 match | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
|-------------|------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| INCLUDES    | mapped, unmapped             | These regions are specified in the ini file and are the regions of particular interest. In the case of telomere analysis, these are regions that are defined as telomeric.  All reads from pairs that fall within these regions are analysed, i.e. mapped reads and unmapped reads where the paired read maps within the region.                                                                                                                                                                                                                                                                                                                                                                                                                        |
| EXCLUDES    | neither                      | These regions are specified in the ini file but in this case these are regions that we specifically want to exclude from the analysis.  For example, in the case of telomeres these might include centromeric regions or non-telomeric places in the genome that are known to contain the telomeric motif and so should be actively ignored.  EXCLUDE regions are still analysed and reported (only unmapped reads are included) but this gives us a mechanism to ensure that the region will not appear in any GENOMIC windows.                                                                                                                                                                                                                        |
| GENOMIC     | unmapped                     | All other regions in the genome not covered by INCLUDES and EXCLUDES. Only unmapped reads are included in analysis of these regions. The size of each GENOMIC region is determined by the window_size entry in the ini file. The length of the chromosome is divided by the window size to give the number of genomic regions.  Note that INCLUDES and EXCLUDES are overlaid on top of GENOMIC regions so if one of then occurs in the middle of a GENOMIC window, that window is split into two smaller windows on either side of the INCLUDE/EXCLUDE.  This means that you need to pay attention to the `chrPos` attribute of `region` elements in the XML to work out how big the region is - you can't assume they are all the `window_size` bases. |
| UNMAPPED    | unmapped                     | Single region for all pairs where both reads are unmapped.  Obviously only unmapped reads are included in this analysis.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |

A matching example - a read with the following sequence:

~~~~{.text}
ACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCT
~~~~

will pass the stage1 check (assuming a filter of TTAGGGTTAGGGTTAGGG with revcomp set to true)
and will add the following string to the list of matches for the region:

~~~~{.text}
CCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAACCCTAA
~~~~

which is identical to the read sequence apart from the first character and the last 4 characters.

This information is gathered for all reads in all regions and summarised in
the output xml file.  An output bam file is also produced which contains
only the reads that passed both the stage1 and stage2 filters.

## Output

There are two output files - an XML report and a BAM containing all reads
that matched the motif search - and both are detailed here.

### XML

Sample XML output from qmotif:

~~~~
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<qmotif version="157-06eae489">
  <ini file="qmotif_GRCh38.ini">
    <stage1_motif>
        <string value="TTAGGGTTAGGGTTAGGG"/>
        <string value="CCCTAACCCTAACCCTAA"/>
    </stage1_motif>
    <stage2_motif>
        <regex value="(...GGG){2,}|(CCC...){2,}"/>
    </stage2_motif>
    <window_size value="10000"/>
    <includes_only value="false"/>
    <includes>
      <region chrPos="chr1:10001-12464" name="chr1p"/>
      <region chrPos="chr1:248943708-248946421" name="chr1q"/>
      <region chrPos="chr2:10001-12592" name="chr2p"/>
      <region chrPos="chr2:242146750-242148749" name="chr2q"/>
      <region chrPos="chr2:242181358-242183529" name="chr2xA"/>
      <region chrPos="chr3:18323-20322" name="chr3p"/>
      <region chrPos="chr3:198170705-198176526" name="chr3xB"/>
      <region chrPos="chr3:198233559-198235558" name="chr3q"/>
      <region chrPos="chr4:10001-12193" name="chr4p"/>
      <region chrPos="chr4:190120458-190123120" name="chr4q"/>
      <region chrPos="chr5:10001-13806" name="chr5p"/>
      <region chrPos="chr5:181476259-181478258" name="chr5q"/>
      <region chrPos="chr6:60001-62000" name="chr6p"/>
      <region chrPos="chr6:170743979-170745978" name="chr6q"/>
      <region chrPos="chr7:10001-12238" name="chr7p"/>
      <region chrPos="chr7:159333868-159335972" name="chr7q"/>
      <region chrPos="chr8:60001-62000" name="chr8p"/>
      <region chrPos="chr8:145076636-145078635" name="chr8q"/>
      <region chrPos="chr9:10001-12359" name="chr9p"/>
      <region chrPos="chr9:138260981-138262980" name="chr9q"/>
      <region chrPos="chr10:14061-16061" name="chr10p"/>
      <region chrPos="chr10:133785144-133787421" name="chr10q"/>
      <region chrPos="chr11:60001-62000" name="chr11p"/>
      <region chrPos="chr11:135074564-135076621" name="chr11q"/>
      <region chrPos="chr12:10001-12582" name="chr12xC"/>
      <region chrPos="chr12:43740-45739" name="chr12p"/>
      <region chrPos="chr12:133262872-133265308" name="chr12q"/>
      <region chrPos="chr13:18445861-18447860" name="chr13p"/>
      <region chrPos="chr13:114342403-114344402" name="chr13q"/>
      <region chrPos="chr14:18243524-18245523" name="chr14p"/>
      <region chrPos="chr14:106879333-106881349" name="chr14q"/>
      <region chrPos="chr15:19794748-19796747" name="chr15p"/>
      <region chrPos="chr15:101978766-101981188" name="chr15q"/>
      <region chrPos="chr16:10001-12033" name="chr16p"/>
      <region chrPos="chr16:90226345-90228344" name="chr16q"/>
      <region chrPos="chr17:150208-152207" name="chr17p"/>
      <region chrPos="chr17:83245442-83247441" name="chr17q"/>
      <region chrPos="chr18:10001-12621" name="chr18p"/>
      <region chrPos="chr18:80256343-80259271" name="chr18q"/>
      <region chrPos="chr19:60001-62000" name="chr19p"/>
      <region chrPos="chr19:58605455-58607615" name="chr19q"/>
      <region chrPos="chr20:79360-81359" name="chr20p"/>
      <region chrPos="chr20:64332167-64334166" name="chr20q"/>
      <region chrPos="chr21:8522361-8524360" name="chr21p"/>
      <region chrPos="chr21:46697876-46699982" name="chr21q"/>
      <region chrPos="chr22:15926017-15927980" name="chr22p"/>
      <region chrPos="chr22:50804138-50806137" name="chr22q"/>
      <region chrPos="chrX:10001-12033" name="chrXp"/>
      <region chrPos="chrX:156028068-156030894" name="chrXq"/>
      <region chrPos="chrY:10001-12033" name="chrYp"/>
      <region chrPos="chrY:57214588-57217414" name="chrYq"/>
    </includes>
  </ini>
  <summary bam="/path/to/my/bamfile.bam">
    <counts>
      <totalReadsInThisAnalysis count="821999607"/>
      <noOfMotifs count="48522"/>
      <rawUnmapped count="74"/>
      <rawIncludes count="54774"/>
      <rawGenomic count="12"/>
      <scaledUnmapped count="90"/>
      <scaledIncludes count="66635"/>
      <scaledGenomic count="14"/>
      <bases_containing_motifs count="7399368"/>
    </counts>
  </summary>
  <motifs>
    <motif id="1" motif="AAAGGGAGAGGGGGAGGGGGAGGG" noOfHits="1"/>
    <motif id="2" motif="AAAGGGATAGGG" noOfHits="2"/>
    <motif id="3" motif="AAAGGGATTGGGCTAGGGATAGGGTTAGGGGTAGGG" noOfHits="1"/>
    <motif id="4" motif="AAAGGGCTAGGG" noOfHits="1"/>
    <motif id="5" motif="AAAGGGGAAGGG" noOfHits="1"/>
    ...
  </motifs>
  <regions>
    <region chrPos="chr1:0-9999" stage1Cov="30" stage2Cov="30" type="genomic">
      <motif id="16087" number="3" strand="R"/>
      <motif id="28504" number="1" strand="R"/>
      <motif id="16053" number="1" strand="R"/>
      <motif id="30640" number="1" strand="R"/>
      <motif id="22632" number="2" strand="R"/>
      ...
    </region>
    <region chrPos="chr1:10000-10000" stage1Cov="6" stage2Cov="6" type="genomic">
      <motif id="25994" number="6" strand="F"/>
    </region>
    <region chrPos="chr1:10001-12464" stage1Cov="1805" stage2Cov="1805" type="includes">
      <motif id="397" number="1" strand="F"/>
      <motif id="26591" number="1" strand="F"/>
      <motif id="7782" number="1" strand="F"/>
      <motif id="18961" number="1" strand="F"/>
      <motif id="3065" number="1" strand="F"/>
      ...
    </region>
    ...
    <region chrPos="unmapped:0-9999" stage1Cov="15452" stage2Cov="15452" type="unmapped">
      <motif id="5017" number="3" strand="F"/>
      <motif id="19855" number="1" strand="F"/>
      <motif id="20772" number="5" strand="F"/>
      <motif id="20256" number="1" strand="F"/>
      <motif id="18776" number="1" strand="F"/>
      ...
    </region>
  </regions>
</qmotif>
~~~~

The output file has 4 sections:

* `<ini>` - the parameters that were used to run qmotif
* `<summary>` - the input parameters plus any parameters that were not specified but that have defaults
  * `<totalReadsInThisAnalysis count="821999607"/>` - the number of reads in the input BAM that were analysed (note that this might not necessarily be the total number of reads in the BAM if you are using the `includes_only` option)
  * `<noOfMotifs count="48522"/>` - the total number of motifs found in the input BAM
  * `<rawUnmapped count="74"/>` - the number of unmapped reads that passed the stage1 and stage2 filters
  * `<rawIncludes count="54774"/>` - the number of reads in INCLUDES regions that passed the stage1 and stage2 filters
  * `<rawGenomic count="12"/>` - the number of reads in GENOMIC regions that passed the stage1 and stage2 filters
  * `<scaledUnmapped count="90"/>` - the number of unmapped reads that passed the stage1 and stage2 filters after scaling
  * `<scaledIncludes count="66635"/>` - the number of reads in INCLUDES regions that passed the stage1 and stage2 filters after scaling
  * `<scaledGenomic count="14"/>` - the number of reads in GENOMIC regions that passed the stage1 and stage2 filters after scaling
  * `<bases_containing_motifs count="7399368"/>` - sum of motif length * number of occurrences for all motifs
* `<motifs>` - an entry for each stage2_motif found along with a count of the total number of times the motif was seen
* `<regions>` - counts of the different motifs seen in each of the regions (INCLUDES, EXCLUDES, GENOMIC, UNMAPPED) where each motif ID relates to one of the motif elements in "motifs"

#### Scaling
The scaled counts are calculated by scaling the totalReadCount to 1 billion.  This is done to allow for easy comparison of the number of motifs found in BAM files with different number of reads.
So if your BAM has 0.5B reads, all the scaled scores will be double the raw counts and if your BAM has 2B reads, the scaled scores would be half of the raw numbers. 
We don’t take any account of unmapped reads, secondary alignments etc. when scaling, we just count every read. 
We take this simple approach because when you are talking about tumours, the correct approach is non-obvious - for example, if we have 3 chromosomes with whole-arm amplifications, how should we take account of that? 
Clever/correct scaling is left as an exercise for the user as they know their data best. 
With all of those caveats, qMotif scaled scores correlate very well with wet-lab techniques as we showed in the qMotif paper so we think the simple scaling approach probably works well enough in the majority of cases.

### BAM

Any reads that pass the stage 1 filter will make it into the output bam file,
as long as the region that the read is in allows that type of read. For 
example, if the read falls within an INCLUDES region, passes the stage 1 
filter, and is mapped or unmapped, then it will make it into the BAM. If
the read falls within an GENOMIC, or UNMAPPED region, passes the stage 1
filter, and is mapped it will NOT make it into the BAM. If it is unmapped
and passes the filter, then it will make it into the BAM. If the read is in
an EXCLUDES region, then it will not make it into the BAM, regardless of 
filters and mapped status.

Output bams are coordinate sorted by default.

## Quantifying telomeres using qmotif

The major use case for the development of qmotif was quantification of 
telomeres in cancer whole genome sequencing by counting reads that contain
the 6-base telomeric motif (TTAGGG).  The configuration file shown below is
the one [we](http://www.qimrberghofer.edu.au/lab/bioinformatics/) use for
telomere quantification.

~~~~{.text}
; File:     qmotif configuration file for hg19 telomere quantification
; Created:  2016-01-25
;
; This qmotif configuration file is designed to search for reads that
; contain telomeric motifs.  The [INCLUDES] section defines the human
; hg19 genomic regions that have been pre-determined to "capture" 
; telomeric reads during (bwa) alignment and should not be edited
; without careful thought.  The stage 1 string match is 3 consecutive
; repeats of the canonical telomere motif (TTAGGG) and the stage 2
; regular expression match is for any 2 adjacent repeats of the motif
; with variation allowed in the first 3 positions.  Note that for the
; regex match, you need to account for the reverse complement case
; yourself whereas for a string match, you can tell qmotif to also
; search the reverse complement of the given string.

[PARAMS]
stage1_motif_string=TTAGGGTTAGGGTTAGGG
stage1_string_rev_comp=true
stage2_motif_regex=(...GGG){2,}|(CCC...){2,}
window_size=10000
includes_only=true

[INCLUDES]
; name, regions (sequence:start-stop)
chr1p   chr1:10001-12464
chr1q   chr1:249237907-249240620
chr2p   chr2:10001-12592
chr2q   chr2:243187373-243189372
chr2xA  chr2:243150480-243154648
chr3p   chr3:60001-62000
chr3q   chr3:197960430-197962429
chr3xB  chr3:197897576-197903397
chr4p   chr4:10001-12193
chr4q   chr4:191041613-191044275
chr5p   chr5:10001-13806
chr5q   chr5:180903260-180905259
chr6p   chr6:60001-62000
chr6q   chr6:171053067-171055066
chr7p   chr7:10001-12238
chr7q   chr7:159126558-159128662
chr8p   chr8:10001-12000
chr8q   chr8:146302022-146304021
chr9p   chr9:10001-12359
chr9q   chr9:141151431-141153430
chr10p  chr10:60001-62000
chr10q  chr10:135522469-135524746
chr11p  chr11:60001-62000
chr11q  chr11:134944458-134946515
chr12p  chr12:60001-62000
chr12q  chr12:133839458-133841894
chr12xC chr12:93158-97735
chr13p  chr13:19020001-19022000
chr13q  chr13:115107878-115109877
chr14p  chr14:19020001-19022000
chr14q  chr14:107287540-107289539
chr15p  chr15:20000001-20002000
chr15q  chr15:102518969-102521391
chr16p  chr16:60001-62033
chr16q  chr16:90292753-90294752
chr17p  chr17:1-2000
chr17q  chr17:81193211-81195210
chr18p  chr18:10001-12621
chr18q  chr18:78014226-78017247
chr19p  chr19:60001-62000
chr19q  chr19:59116822-59118982
chr20p  chr20:60001-62000
chr20q  chr20:62963520-62965519
chr21p  chr21:9411194-9413193
chr21q  chr21:48117788-48119894
chr22p  chr22:16050001-16052000
chr22q  chr22:51242566-51244565
chrXp   chrX:60001-62033
chrXq   chrX:155257733-155260559
chrYp   chrY:10001-12033
chrYq   chrY:59360739-59363565
~~~~

