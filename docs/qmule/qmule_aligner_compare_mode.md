# qmule AlignerCompare mode

This mode of the [qmule](index.md) tool will take 2 BAMs (sorted on `QNAME`)
and will compare BAM records.  It is intended for use in cases where we want
to compare 2 different aligners or the same aligner run with different
options.  The tool creates 3 output BAMs:

* all records that are identical
* records from BAM1 that don't match BAM2
* records from BAM2 that don't match BAM1

It is assumed that the 2 input BAM files were created from the same FASTQ 
files so all of the reads in BAM1 must also appear in BAM2 although there
may be more than one record per read which allows for secondary alignments
to be present.  The input BAMs are required to be sorted by `QNAME` not
`POS` so that all alignments for a given read are adjacent.  Only reads
that are primary alignments or unmapped will be compared so secondary
alignments are invisible to `AlignerCompare` unless you choose
`compareAll` as [option].

We treat two alignments as the same if these fields are identical:

* `QNAME` - query template name (read ID)
* `FLAG` - bitwise FLAG
* `RNAME` - reference sequence name (usually the chromosome or config)
* `POS` - 1-based leftmost alignment position
* alignment end position
* `MAPQ` - mapping quality
* `CIGAR` - CIGAR string
* MD field - optional field that provides a representation of the base-by-base alignment.

For the purposes of the optional `MD` field, the reads are considered to 
match if both align with the same MD string or both are missing the MD 
field so both or neither of the input BAMs should have the MD field.

## Usage

~~~~{.text}
qmule org.qcmg.qmule.AlignerCompare -i <bam1> -i <bam2> -o <output> [options]
~~~~

## Options

~~~~{.text}
--help, -h      Shows this help message.               
--version, -v   Print version.                 

--input, -i     Req, An input BAM - must appear twice.
--output, -o    Output file prefix.

--compareAll    Keep non-primary alignments
~~~~

Use option "compareAll" if you don't want to discard all non-primary 
alignments (secondary and supplementary alignments).  This tool will
split all input records into three output BAMs as shown below:

~~~~{.text}
java -cp qmule-0.1pre.jar org.qcmg.qmule.AlignerCompare \
    -i bwamem.bam -i bwaback.bam -o /output/prefix
~~~~

Outputs:

1. `/output/prefix.identical.bam` - reads with the same aligment in both input BAMs
2. `/output/prefix.different.bwamem.bam` - primary alignments from BAM1 which did not match BAM2
3. `/output/prefix.different.bwaback.bam` - primary alignments from BAM2 which did not match BAM1
4. `/output/prefix.log` - log file

With option `compareAll`, the non-primary alignments will appear in output2
or output3, otherwise these alignments will be discarded by default.

If the input BAMs have the same name but are from different directories
(please please please do not do this) the names of the two output BAMS
will be `/output/prefix.different.first.bam` and
`/output/prefix.different.second.bam`.

The log file will look something like:

~~~~{.text}
12:13:28.986 [main] INFO org.qcmg.qmule.AlignerCompare - input BAM1: /input/bwamem.bam
12:13:28.987 [main] INFO org.qcmg.qmule.AlignerCompare - input BAM2: /input/bwaback.bam
12:13:28.987 [main] INFO org.qcmg.qmule.AlignerCompare - discard secondary or supplementary alignments: true
12:13:29.031 [main] INFO org.qcmg.qmule.AlignerCompare - output of identical reads: /output/prefix.identical.bam
12:13:29.031 [main] INFO org.qcmg.qmule.AlignerCompare - output of unique reads from BAM1: /output/prefix.different.bwamem.bam
12:13:29.032 [main] INFO org.qcmg.qmule.AlignerCompare - output of unique reads from BAM2: /output/prefix.different.bwaback.bam
14:09:06.193 [main] INFO org.qcmg.qmule.AlignerCompare - There are 100411877 reads with 244684085 alignments from BAM1
14:09:06.219 [main] INFO org.qcmg.qmule.AlignerCompare - There are 100411877 reads with 200823754 alignments from BAM2
14:09:06.220 [main] INFO org.qcmg.qmule.AlignerCompare - There are 146425081 alignments are identical from both BAM
14:09:06.220 [main] INFO org.qcmg.qmule.AlignerCompare - Different alignments from BAM1 are 54398673, from BAM2 are 54398673
14:09:06.221 [main] INFO org.qcmg.qmule.AlignerCompare - discard 43426907 secondary alignments and 433424 supplementary alignments from BAM1
14:09:06.229 [main] INFO org.qcmg.qmule.AlignerCompare - discard 0 secondary alignments and 0 supplementary alignments from BAM2
14:09:07.231 [main] INFO org.qcmg.qmule.AlignerCompare - It took 1 hours, 55 seconds to perform the comparison
14:09:07.231 [main] EXEC org.qcmg.qmule.AlignerCompare - StopTime 2014-03-12 14:09:07
14:09:07.232 [main] EXEC org.qcmg.qmule.AlignerCompare - ExitStatus 0
~~~~
