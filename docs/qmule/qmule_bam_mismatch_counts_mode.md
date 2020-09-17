# qmule BamMismatchCounts mode


This mode of the [qmule](index.md) tool provides, for reads that mapped 
full-length, a breakdown of how many mismatches are in each read.  It 
also logs a breakdown of reads that were in the BAM but not included 
in the mismatch tally so they were unmapped or had a CIGAR string
indicating insertion (I), deletion (D), padding (P), skipping (N), or 
clipping (S,H).

## Usage

~~~~{.text}
qmule org.qcmg.qmule.BamMismatchCounts <bam/sam filename> <output filename>
~~~~

## Outputs

The mismatch tally output looks like this:

~~~~{.text}
mismatch	reads_number	ratio_to_(fullmapped,total)
0	805075	(83%,80%)
1	109567	(11%,10%)
2	22495	(2%,2%)
3	8918	(0%,0%)
4	5763	(0%,0%)
5	4773	(0%,0%)
6	776	(0%,0%)
7	622	(0%,0%)
8	518	(0%,0%)
9	367	(0%,0%)
10	301	(0%,0%)
11	256	(0%,0%)
12	179	(0%,0%)
13	156	(0%,0%)
14	127	(0%,0%)
...
~~~~

It automatically creates a log file named <output filename>.log. It extracts below shows a example breakdown of the reads in the BAM.  Of the 999912 reads, 960361 were mapped full-length and so were used to create the mismatch tally.

~~~~{.text}
20:04:42.378 [main] INFO org.qcmg.qmule.BamMismatchCounts - total records in file: 999912
20:04:42.378 [main] INFO org.qcmg.qmule.BamMismatchCounts - unmapped records: 6874
20:04:42.379 [main] INFO org.qcmg.qmule.BamMismatchCounts - records with clipping (CIGAR S,H): 13150
20:04:42.379 [main] INFO org.qcmg.qmule.BamMismatchCounts - records with indel (CIGAR I,D): 19527
20:04:42.379 [main] INFO org.qcmg.qmule.BamMismatchCounts - records with skipping or padding (CIGAR N,P): 0
20:04:42.379 [main] INFO org.qcmg.qmule.BamMismatchCounts - records mapped full-length: 960361
20:04:42.380 [main] INFO org.qcmg.qmule.BamMismatchCounts - records mapped full-length but missing MD field: 0
~~~~
