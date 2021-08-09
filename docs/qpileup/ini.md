
# Modes

## `bootstrap`

`bootstrap` mode creates a qpileup HDF5 file for a reference genome, storing position and reference base information for the genome. The output from
this mode is a complete HDF5 qpileup file but with no values in any of the summary metrics. For a given reference genome, a user would typically run
bootstrap mode once to create a "clean" initialised qpileup HSF5 and then use that bootstrap file as the basis for numerous qpileup BAM collections. 
Using 12 threads on a cluster node with 2 x 6-core CPUs, bootstrapping the GRCh37 human genome takes approximately 12 minutes.

~~~~{.text}
;example of ini file for bootstrap mode

[general]
;here loglevel use default value
;bam_override, output_dir and range are ignored in bootstrap mode. 

;this hdf file must not exists
hdf=/path/to/output/GRCh37_ICGC_standard_v2.bootstrap.h5

; tool total threads will be  15 = pileup threads based on chromosome (threadNo) + other threads (3)
threadNo=12

log=/path/to/output/GRCh37_ICGC_standard_v2.bootstrap.h5.log
mode=bootstrap

[bootstrap]
;index of reference file (.fai) must exists
reference=/path/to/reference/GRCh37_ICGC_standard_v2.fa

;the number of BAMs that have less then 10 reads covering this position will be recorded on HDF5 Data Element LowReadCount. 
low_read_count=10

;The number of BAMs that had more than 30% non-ref bases on this position will be counted into HDF5 Data Element HighNonreference. 
nonref_percent=30
~~~~

## `add`

`add` mode takes one or more BAM files as input and performs a pileup for the reads, adding the new data to an existing qpileup file. It is expected
that `add` and `view` will be the most often used qpileup modes as this mode allows for addition of new BAMs to existing qpileup files and "view" allows
for querying of locations in the qpileup file.

Bam files can be listed in 2 ways:
*within the ini file using name=path to bam file
*provide a file with bam files listed using bamlist=path to bam list file

~~~~
[general]
;here default range=all is used. 

;this hdf must already exists. often directory copied from boostrap output.
hdf=/path/target_109.qpileup.h5

bam_override=true
thread_no=12
log=/path/test.log
mode=add

[add_remove]
bam_list=/qpileup/bam_list_20130202.txt
~~~~

`Caution: adding BAMs to h5 with different range`. eg add another bam within different range to hdf from above add mode run. Hence the output will be chaotic, this should rarely be necessary. Copy an outpuyt hdf file from bootstrap mode here, will be much relaible. 

~~~
[general]
hdf=/path/target_109.qpileup.h5
log=/path/test.log
range=chr1:1000-2000
thread_no=12
mode=add

[add_remove]
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_001.sorted.bam
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

Unlike all of the other modes, there is a limited ability to use view mode via commandline options to qpileup. This mode can be to view metadata for the HDF file and the qpileup metrics for a small (up to one chromosome) region of the reference genome. For more details on `view` mode, see this [page](qpileup_view_mode.md).

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
  * forward: all elements: forward strand reads. 
  * reverse: all elements: reverse strand reads
  * bases: elements: A,C,G,T,N,ReferenceNo,NonreferenceNo,HighNonreference,LowReadCount
  * quals: elements: AQual,CQual,GQual,NQual,MapQual
  * cigars: elements: CigarI,CigarD,CigarD_start,CigarS,CigarS_start,CigarH,CigarH_start,CigarN,CigarN_start
  * readStats: elements: StartAll,StartNondup,StopAll,Dup,MateUnmapped

## `merge`

`merge` mode will merge 2 or more HDF5 files together. The files must use the same reference genome, and the same values for `lowreadcount` and `percentnonref`.
the [boostrap] section is required, the merge will call boostrap mode to create a new hdf file. 

~~~~
[general]
;bam_override, output_dir and range are ignored in merge mode. 

log=/path/test.log

;this hdf file must not exists
hdf=/path/target_109.qpileup.h5

threadNo=12
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

