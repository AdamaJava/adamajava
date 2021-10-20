# qmule QSamToFastq mode

## Usage

~~~~{.text}
java -cp $qmule org.qcmg.qmule.bam.QSamToFastq I=<input.bam> FASTQ=<r1.fastq> SECOND_END_FASTQ=<r2.fastq> [options]
~~~~

## Options

Options:
~~~~{.text}

--help
-h                            Displays options specific to this tool.

--stdhelp
-H                            Displays options specific to this tool AND options common to all Picard command line
                              tools.

--version                     Displays program version.

INPUT=File
I=File                        Input SAM/BAM file to extract reads from  Required.

FASTQ=File
F=File                        Output fastq file (single-end fastq or, if paired, first end of the pair fastq).
                              Required.  Cannot be used in conjuction with option(s) OUTPUT_PER_RG (OPRG)

SECOND_END_FASTQ=File
F2=File                       Output fastq file (if paired, second end of the pair fastq).  Default value: null.  Cannot
                              be used in conjuction with option(s) OUTPUT_PER_RG (OPRG)

OUTPUT_PER_RG=Boolean
OPRG=Boolean                  Output a fastq file per read group (two fastq files per read group if the group is
                              paired).  Default value: false. This option can be set to 'null' to clear the default
                              value. Possible values: {true, false}  Cannot be used in conjuction with option(s)
                              SECOND_END_FASTQ (F2) FASTQ (F)

OUTPUT_DIR=File
ODIR=File                     Directory in which to output the fastq file(s).  Used only when OUTPUT_PER_RG is true.
                              Default value: null.

RE_REVERSE=Boolean
RC=Boolean                    Re-reverse bases and qualities of reads with negative strand flag set before writing them
                              to fastq  Default value: true. This option can be set to 'null' to clear the default
                              value. Possible values: {true, false}

INCLUDE_NON_PF_READS=Boolean
NON_PF=Boolean                If true, include non-PF reads that don't pass quality controls in the output, otherwise
                              this read will be discarded.  Default value: false. This option can be set to 'null' to
                              clear the default value. Possible values: {true, false}

INCLUDE_NON_PRIMARY_ALIGNMENTS=Boolean
                              If true, include non-primary alignments in the output, otherwise this read will be
                              discarded.  Default value: false. This option can be set to 'null' to clear the default
                              value. Possible values: {true, false}

INCLUDE_SUPPLEMENTARY_READS=Boolean
                              If true, include supplementary alignments in the output, otherwise this read will be
                              discarded.  Default value: false. This option can be set to 'null' to clear the default
                              value. Possible values: {true, false}

CLIPPING_ATTRIBUTE=String
CLIP_ATTR=String              The attribute that stores the position at which the SAM record should be clipped  Default
                              value: null.

CLIPPING_ACTION=String
CLIP_ACT=String               The action that should be taken with clipped reads: 'X' means the reads and qualities
                              should be trimmed at the clipped position; 'N' means the bases should be changed to Ns in
                              the clipped region; and any integer means that the base qualities should be set to that
                              value in the clipped region.  Default value: null.

READ1_TRIM=Integer
R1_TRIM=Integer               The number of bases to trim from the beginning of read 1.  Default value: 0. This option
                              can be set to 'null' to clear the default value.

READ1_MAX_BASES_TO_WRITE=Integer
R1_MAX_BASES=Integer          The maximum number of bases to write from read 1 after trimming. If there are fewer than
                              this many bases left after trimming, all will be written.  If this value is null then all
                              bases left after trimming will be written.  Default value: null.

READ2_TRIM=Integer
R2_TRIM=Integer               The number of bases to trim from the beginning of read 2.  Default value: 0. This option
                              can be set to 'null' to clear the default value.

READ2_MAX_BASES_TO_WRITE=Integer
R2_MAX_BASES=Integer          The maximum number of bases to write from read 2 after trimming. If there are fewer than
                              this many bases left after trimming, all will be written.  If this value is null then all
                              bases left after trimming will be written.  Default value: null.

MARK_MATE=Boolean             If true, read id will be appended with /1 for first of pair and /2 for second of pair. If
                              false, read id will be as same as BAM record id.  Default value: false. This option can be
                              set to 'null' to clear the default value. Possible values: {true, false}

BASE_NULL_TO_N=Boolean        If true, set 'N' to fastq record base if SAM record missing base sequence; and then set
                              '!' to base quality. If false, read base will be same as BAM record base, often is '*'.
                              Default value: true. This option can be set to 'null' to clear the default value. Possible
                              values: {true, false}

MISS_MATE_RESCUE=Boolean      If true, output a pair of fastq records, set base sequence 'N' and base quality '!' to the
                              missing mate record . If false, output one fastq record (only if the input SAM record
                              missing mate).  Default value: true. This option can be set to 'null' to clear the default
                              value. Possible values: {true, false}

LOG_FILE=String
LOG=String                    output a log file.  Default value: qsamtofastq.log. This option can be set to 'null' to
                              clear the default value.
~~~~

## Examples
  * run in default option value
  ~~~~{.text}
java -cp qmule.jar org.qcmg.qmule.bam.QSamToFastq I=test.bam FASTQ=r1.fastq SECOND_END_FASTQ=r2.fastq
  ~~~~

  this command line is same as below
  ~~~~{.text}
java -cp qmule.jar org.qcmg.qmule.bam.QSamToFastq INPUT=test.bam FASTQ=r1.fastq SECOND_END_FASTQ=r2.fastq  OUTPUT_PER_RG=false RE_REVERSE=true INCLUDE_NON_PF_READS=false INCLUDE_NON_PRIMARY_ALIGNMENTS=false INCLUDE_SUPPLEMENTARY_READS=false READ1_TRIM=0 READ2_TRIM=0 MARK_MATE=false BASE_NULL_TO_N=true MISS_MATE_RESCUE=true LOG_FILE=qsamtofastq.log
  ~~~~

  Here we use a reversed SAM record as example, it miss first of pair. 
  ~~~~{.text}
ST-E00119:628:HFMTKALXX:7:1116:25652:22616	147	chr1	20514631	60	151M	=	20514362	-420	TCTATCAAAAGAAAGTTTCAAGTCTGTGAGTTGAATTGCACACATC	JJ<JFJ<JJJJJJJJFJ-7<FJFJJFJJJJFJJAAJJJJJJJJJJJ
  ~~~~
  
  output is

~~~~{.text}
# output in r1.fastq
@ST-E00119:628:HFMTKALXX:7:1116:25652:22616
N
+
!

# output in r2.fastq
@ST-E00119:628:HFMTKALXX:7:1116:25652:22616
TATACGGACTAATTATTCATCTGACTGGCTTCTTATAGATGTTGAAAGGAGAAATAAGTGCACTTAGGCAGTACCTTGACAAATGTCTCTTACTCCTTGACCTGATTATTCAGTTTCTTATGTGTGCGGTTTTATTAGTGTTGAAGCCAAT
+
A<AAFJJJJFJJJJJJJJJJJJJJJFAJFAAJAFJJJJJFJJJJJJJJJJJAJJJFJJJJJJJJJJJJJFFFJAFAFJJJJJJJFJJFJJJF7AFJJJJJJJJF7A-7AJJJJF7AJ<AAJJJJF<JJF)<7JAAF<-<-A-AA--AFAAF

~~~~

  * set "RE_REVERSE=false", "MISS_MATE_RESCUE=false", "BASE_NULL_TO_N=false" and "MARK_MATE=true" in above example. It didn't output R1 record due to missing mate, only R2 record as below:

~~~~{.text}

@ST-E00119:628:HFMTKALXX:7:1116:25652:22616/2
ATTGGCTTCAACACTAATAAAACCGCACACATAAGAAACTGAATAATCAGGTCAAGGAGTAAGAGACATTTGTCAAGGTACTGCCTAAGTGCACTTATTTCTCCTTTCAACATCTATAAGAAGCCAGTCAGATGAATAATTAGTCCGTATA
+
FAAFA--AA-A-<-<FAAJ7<)FJJ<FJJJJAA<JA7FJJJJA7-A7FJJJJJJJJFA7FJJJFJJFJJJJJJJFAFAJFFFJJJJJJJJJJJJJFJJJAJJJJJJJJJJJFJJJJJFAJAAFJAFJJJJJJJJJJJJJJJFJJJJFAA<A
~~~~

