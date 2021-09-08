
# Modes

## `bootstrap`

`bootstrap` mode creates a qpileup HDF5 file for a reference genome, storing position and reference base information for the genome. The output from
this mode is a complete HDF5 qpileup file but with no values in any of the summary metrics. For a given reference genome, a user would typically run
bootstrap mode once to create a "clean" initialised qpileup HSF5 and then use that bootstrap file as the basis for numerous qpileup BAM collections. 
Bootstrap only use one thread, it took about 5 minutes with 8G RAM. 

An example of INI to run bootstrap is given, here
  * the `hdf` file must not pre-exists, it is the output of qpileup bootstrap mode.
  * the `thread_no` is not used in bootstrap mode, only single thread is taken for bootstrap, plus other threads(2) for reading etc. 
  * the `bam_override`, `output_dir` and `range` are not used in bootstrap and merge mode.
  * In this example default `loglevel = INFO` is used.
  * A reference file (.fa) accompany with index file (.fai) must exists
  * `low_read_count=10` means that the number of BAMs that have less then 10 reads covering this position will be recorded on HDF5 Data Element LowReadCount. 
  * `nonref_percent=30` means the number of BAMs that had more than 30% non-ref bases on this position will be counted into HDF5 Data Element HighNonreference. 

~~~~{.text}
[general]
hdf=/path/to/output/GRCh37_ICGC_standard_v2.bootstrap.h5
log=/path/to/output/GRCh37_ICGC_standard_v2.bootstrap.h5.log
mode=bootstrap

[bootstrap]
reference=/path/to/reference/GRCh37_ICGC_standard_v2.fa
low_read_count=10
nonref_percent=30
~~~~

## `add`

`add` mode takes one or more BAM files as input and performs a pileup for the reads, adding the new data to an existing qpileup file. It is expected
that `add` and `view` will be the most often used qpileup modes as this mode allows for addition of new BAMs to existing qpileup files and "view" allows
for querying of locations in the qpileup file. 

  * there are two ways to add BAM file, here providing a file with bam files listed using `bamlist=<path to a file listing all BAMs>`.
  * the `hdf` must pre-exist. It is often directory copied from boostrap output.
  * the `output_dir` is not used in add mode.
  * In this example default `range=all` is used. 
  * In this example default `bam_override=false` is used. 
  * add mode allows multi theads, below table lists resource usage with different theads number on same dataset. 
  	* the more threads required, more mem usage taken, because each thead read certain amount genome range into RAM.
	* the more input bam records, more cpu time taken, because qpileup counts the read number mapped on each reference base. 
	* below table based on 9 small exon BAMs, total about 27G. 

 | resource/threads | 5 | 7 | 12 | 17 |
 | --- | --- | --- | --- |--- |
| resources_used.cpupercent | 494 | 656 | 1104 | 1495 |
| resources_used.mem | 16G | 22G | 31G | 31G |
| resources_used.cput | 12:46:55 | 09:20:24 | 11:18:00 | 11:34:48 |
| resources_used.walltime | 02:55:04 | 01:39:42 | 01:23:37 | 01:13:24 |
| exec_host | hpcnode040 | hpcnode065 | hpcnode040 | hpcnode040 |


~~~~
[general]
hdf=/path/test.qpileup.h5
log=/path/test.log
thread_no=12
mode=add

[add_remove]
bam_list=/qpileup/bam_list_20130202.txt
~~~~

Another INI file example:
  * there are two ways to add BAM file, here using `name=<path to BAM file>` to bam file.
  * `bam_override=true` means that same BAM (same name and path) allows to add multi times if list multi times. 
  * `Caution: adding BAMs to h5 with different range`. Below example add another BAMs within different range to the hdf from above run. Hence the output will be chaotic, this should rarely be necessary. Copy an output hdf file from bootstrap mode here, will be much relaible. 

~~~
[general]
hdf=/path/test.qpileup.h5
log=/path/test.log
bam_override=true
range=chr1:1000-20000
thread_no=12
mode=add

[add_remove]
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_001.sorted.bam
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_003.sorted.bam
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_003.sorted.bam
~~~~

## `remove`

`remove` mode is the opposite of `add` more. It takes one or more BAM files that are part of an existing qpileup HDF5 file and it removes the BAMs
from the collection by performing pileups and decrementing the values of all of the qpileup metrics so it was as though the BAM file(s) had not
been added to the HDF5. This should rarely be necessary but is invaluable in cases such as sample or tumour/normal swaps, or incorrect labelling of
BAM files.
Without this mode, any case where a BAM was incorrcetly added to an HDF5 file would require the HDF5 file to be regenerated from scratch.
  
  * the `range` and `output_dir` are not used in remove mode.
  * the hdf must pre-exists and will modified by qpileup.
  * In this example default `bam_override=false` is used. 
  * In this example default `loglevel = INFO` is used. 

~~~
[general]
hdf=/path/target_109.qpileup.h5
log=/path/test.log
thread_no=12
mode=remove

[add_remove]
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_001.sorted.bam
~~~~


## `view`

`view` mode reads metrics from a qpileup HDF5 file and writes to a CSV file. `view` can be used for a whole genome (but don't do that unless 
you have a lot of disk - it's a really big file) or for specific regions. `view` takes a list of ranges and will output a separate CSV for each chromosome/contig that is part of the list of ranges queried. Each file will contain metadata at the top including the HDF5 file the summary was extracted from and the regions extacted.

  * Unlike all of the other modes, there is a limited ability to use view mode via commandline options to qpileup. For more details on [page](index.md#usage2-view-mode-options).
  * the `output_dir` is requied in view and metrics mode.
  * the hdf must pre-exists and will be read by qpileup.
  * In this example default `loglevel = INFO` is used.
  * In this example default `range = all` is used. Hence the output will be large, not recommand, unless necessary.
  * multi `element` is allowed in view mode. The element is also called stand data element, details on [HDF5 file::strand summary table](hdf.md#strand-summary).
  * only single group is allowed in view mode. group is related to element, the possible groups are:
  	* forward: output all elements with forward strand.
  	* reverse: ouptut all elements with reverse strand.
  	* bases: output all base elements: A, C, G, T, N, ReferenceNo, NonreferenceNo, HighNonreference, LowReadCount.
  	* quals: output all qual elements: Aqual, Cqual, Gqual, Tqual, Nqual, Mapqual.
  	* cigars: output all cigar elements: CigarI, CigarD, CigarD_start, CigarS, CigarS_start, CigarH, CigarH_start, CigarN, CigarN_start.
  	* readStats: output all read start related elements: StartAll, StartNondup,StopAllGqual, MateUnmapped.

~~~~
[general]
hdf=/path/target_109.qpileup.h5
log=/path/test.log
output_dir=/path/
thread_no=12
mode=add

[view]
element = A 
element = T  
group = forward  
~~~~

## `merge`

`merge` mode will merge 2 or more HDF5 files together. The files must use the same reference genome, and the same values for `lowreadcount` and `percentnonref`.
the [boostrap] section is required, the merge will call boostrap mode to create a new hdf file. Merge use single thread, it took about 3 hours with 9G RAM for 3 H5 inputs, each about 20G. 

An example of INI to run merge mode is given, here
  * the `hdf` file must not pre-exists, it is the output of qpileup merge mode.
  * The [bootstrap] section should be same to the bootstrap mode, due to merge mode check and call bootstrap mode. 
  * the `thread_no` is not used in bootstrap and merge mode, only single thread is taken for merge, plus other threads(2) for reading etc. 
  * the `bam_override`, `output_dir` and `range` are not used in bootstrap and merge mode.
  * In this example default `loglevel = INFO` is used.

~~~~
[general]
hdf=/path/target_109.qpileup.h5
log=/path/test.log
mode=merge

[bootstrap]
reference=/path/to/reference/GRCh37_ICGC_standard_v2.fa
low_read_count=10
nonref_percent=30

[merge]
input_hdf==/path/testA.h5
input_hdf==/path/testB.h5
~~~~


## `metrics`

~~~~
[general]
;hdf is the input for metrics mode, must exists. 
hdf=/path/target_109.qpileup.h5
;output_dir, range is required in metric mode
output_dir=/path/
range=chr1
thread_no=12
log=/path/test.log
mode=add

[metrics]
min_bases=3
bigwig_path=/qpileup/wiggle
chrom_sizes=/qpileup/wiggle/grc37.chromosomes.txt

[metrics/clip]
position_value=2
window_count=10

[metrics/nonreference_base]
position_value=2
window_count=10

[metrics/indel]
position_value=0.2
window_count=3

[metrics/mapping_qual]
position_value=10
window_count=10

[metrics/high_coverage]
position_value=2000
window_count=10

[metrics/snp]
dbSNP=/dbSNP/135/00-All_chr.vcf
germlineDB=/qpileup/files/icgc_germline_qsnp.vcf
nonref_percent=20
nonref_count=100
high_nonref_count=1
position_value=1 (??not used)
window_count=2

[metrics/strand_bias]
min_percent_diff=30
min_nonreference_bases=100
~~~~

