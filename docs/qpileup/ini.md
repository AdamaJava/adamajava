
# Modes

## `bootstrap`

`bootstrap` mode creates a qpileup HDF5 file for a reference genome, storing position and reference base information for the genome. The output from
this mode is a complete HDF5 qpileup file but with no values in any of the summary metrics. For a given reference genome, a user would typically run
bootstrap mode once to create a "clean" initialised qpileup HSF5 and then use that bootstrap file as the basis for numerous qpileup BAM collections. 
Bootstrap only use one thread, it took about 5 minutes with 8G RAM. 

An example of INI to run bootstrap is given, here
* the default log level is used.
* `bam_override`, `output_dir` and `range` are ignored in bootstrap mode.
* the `hdf` file must not pre-exists, it is the output of qpileup bootstrap mode.
* tool total threads will be 15 which is the sum of pileup threads based on chromosome (threadNo) and other threads(3).
* index of reference file (.fai) must exists
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

INI file example:
  * there are two ways to add BAM file, here providing a file with bam files listed using bamlist=path to bam list file.
  * `output_dir` is ignored in add mode.
  * this example default `range=all` is used. 
  * this example default `bam_override=false` is used. 
  * this `hdf` must already exists. It is often directory copied from boostrap output, and renamed.
  * The resource requirement in below INI example, it takes:
	 * 15(=12+3) threads with 35G memory, 
 	 * qpileup takes around 8 hour to pileup 3 BAMs (each BAM size is around 90G with 900M reads);
 	 * or 13 hours to pileup 5 BAMs with simiar size.  

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
  * there are two ways to add BAM file, here using `name=path` to bam file.
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

~~~
[general]
;range and output_dir are ignored in remove mode
;default value used for bam_override and loglevel  

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

Unlike all of the other modes, there is a limited ability to use view mode via commandline options to qpileup. For more details on [page](index.md#usage2-view-mode-options).

~~~~
[general]
;default range=all, the output will be large, not recommand, unless necessary. 

log=/path/test.log
hdf=/path/target_109.qpileup.h5
mode=add
thread_no=12

;output_dir is required
output_dir=/path/

[view]
element = A 
element = T  
group = forward  
~~~~

* element is also called stand data element, details on [HDF5 file::strand summary table](hdf.md#strand-summary).

* group is related to element, the possible groups are: 
  * forward: output all elements with forward strand. 
  * reverse: ouptut all elements with reverse strand.
  * bases: output all base elements: A, C, G, T, N, ReferenceNo, NonreferenceNo, HighNonreference, LowReadCount.
  * quals: output all qual elements: Aqual, Cqual, Gqual, Tqual, Nqual, Mapqual.
  * cigars: output all cigar elements: CigarI, CigarD, CigarD_start, CigarS, CigarS_start, CigarH, CigarH_start, CigarN, CigarN_start.
  * readStats: output all read start related elements: StartAll, StartNondup,StopAllGqual, MateUnmapped.

## `merge`

`merge` mode will merge 2 or more HDF5 files together. The files must use the same reference genome, and the same values for `lowreadcount` and `percentnonref`.
the [boostrap] section is required, the merge will call boostrap mode to create a new hdf file. 

~~~~
[general]
;bam_override, output_dir and range are ignored in merge mode. 
;this hdf file must not exists
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

