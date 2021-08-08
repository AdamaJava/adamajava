
## INI file

The INI file is divided into sections. All modes have a `[general]` section plus one or more sections specific to the mode.

~~~~{.text}

[general]
log = Req, Log file.
loglevel = Opt, Logging level [INFO,DEBUG], Def=INFO.
hdf = Req, HDF5 file ( must have .h5 extension). 
mode = Req, Mode [bootstrap,add,remove,merge,view].
bam_override =  Opt, If set, allows duplicate BAM files to be added, Def=FALSE
thread_no = Opt, Number of threads [1-12]. Def=1
output_dir = Req, Directory for output pileup files, (required for view and metrics modes).
;multi-value is allowed, each range in seperate line. 
range = Opt, Range to view (required for view, metircs and add mode). Def=all. 

[bootstrap] 
;the merge mode will call bootstrap, so this section is for both bootstrap and merge mode
; low_read_count and nonref_percent  never reach default value but throw exception. 
reference = Req, path to reference genome fasta file.
low_read_count = Opt, the number below which the LowReadCount element is defined. Also used to define the HighNonReference element. Def=10.
nonref_percent = Opt, Used for HighNonreference strand summary element. Minimum percent that non-reference bases to total bases must have in order to be counts as HighNonReference. Def=20(%)

[add_remove]
; this section for both add and remove mode.
bam_list = Req, path of file with list of bams. Or
name = Req, path to bam files (Required for add, remove modes). More than one name allowed.
filter = Opt, a qbamfilter query to filter out BAM records. Def=null.

[merge]
;multi-value is allowed, each hdf file in seperate line. 
input_hdf = Req, path to the hdf file/s that will be merged.

[view]
;multi-value for element and group are allowed
element = Opt, qpileup data element to view. see [strand summary table](ndex_latest.md#strand-summary).
group = Opt, Possible groups [forward, reverse, bases, quals, cigars, readStats]. 
graph = Opt, ???create html file. Def=false.
stranded = Opt, it accompany with gragh option. Def=false. 
graph_hdf = Opt, hdf to view. Def = null. 

[metrics]
min_bases = Req, minimum average coverage (base count) per reference position.
temporary_dir = Req, directory that temporary files will be written to.
bigwig_path = Opt, directory where wigToBigWig program is located. Def=null.
chrom_sizes = Opt, file listing chromosome lengths for wigToBigWig conversion. Def=null.

[metrics/clip]
position_value = Req, minimum value as percent of total bases per position
window_count = Req, minimum number of positions that pass the position_value in the window

[metrics/nonreference_base]
position_value = Req, minimum value as percent of total bases per position
window_count = Req, minimum number of positions that pass the position_value in the window

[metrics/indel]
position_value = Req, minimum value as percent of total bases per position
window_count = Req, minimum number of positions that pass the position_value in the window

[metrics/mapping_qual]
position_value = Req, minimum value as percent of total bases per position
window_count = Req, minimum number of positions that pass the position_value in the window

[metrics/high_coverage]
position_value = Req, minimum value as percent of total bases per position
window_count = Req, minimum number of positions that pass the position_value in the window

[metrics/snp]
dbSNP=path to dbSNP VCF file
germlineDB=path to qSNP germlineDB file
nonref_percent=Minimum percentage that non-reference bases that must be in order to be included as a SNP. Default 10 (ie 10% non reference)
nonref_count=Minimum count of non reference bases in order to be included. Default 10. 
high_nonref_count=Minimum number of patients that had a high non_reference count. Default 0.
position_value=minimum value as percent of total bases per position
window_count=minimum number of positions that pass the position_value in the window

[metrics/strand_bias]
min_percent_diff=minimum percent difference in % nonreference bases between strands
min_nonreference_bases=minimum number of non-reference bases per reference position
~~~~


### Modes

#### `bootstrap`

`bootstrap` mode creates a qpileup HDF5 file for a reference genome, storing position and reference base information for the genome. The output from
this mode is a complete HDF5 qpileup file but with no values in any of the summary metrics. For a given reference genome, a user would typically run
bootstrap mode once to create a "clean" initialised qpileup HSF5 and then use that bootstrap file as the basis for numerous qpileup BAM collections. 
Using 12 threads on a cluster node with 2 x 6-core CPUs, bootstrapping the GRCh37 human genome takes approximately 12 minutes. For more details on `bootstrap` mode, see this [page](qpileup_bootstrap_mode.md).

#### `add`

`add` mode takes one or more BAM files as input and performs a pileup for the reads, adding the new data to an existing qpileup file. It is expected
that `add` and `view` will be the most often used qpileup modes as this mode allows for addition of new BAMs to existing qpileup files and "view" allows
for querying of locations in the qpileup file.

#### `remove`

`remove` mode is the opposite of `add` more. It takes one or more BAM files that are part of an existing qpileup HDF5 file and it removes the BAMs
from the collection by performing pileups and decrementing the values of all of the qpileup metrics so it was as though the BAM file(s) had not
been added to the HDF5. This should rarely be necessary but is invaluable in cases such as sample or tumour/normal swaps, or incorrect labelling of
BAM files.
Without this mode, any case where a BAM was incorrcetly added to an HDF5 file would require the HDF5 file to be regenerated from scratch.

#### `view`

`view` mode reads metrics from a qpileup HDF5 file and writes to a CSV file. `view` can be used for a whole genome (but don't do that unless 
you have a lot of disk - it's a really big file) or for specific regions. `view` takes a list of ranges and will output a separate CSV for each chromosome/contig that is part of the list of ranges queried. Each file will contain metadata at the top including the HDF5 file the summary was extracted from and the regions extacted.

Unlike all of the other modes, there is a limited ability to use view mode via commandline options to qpileup. This mode can be to view metadata for the HDF file and the qpileup metrics for a small (up to one chromosome) region of the reference genome. For more details on `view` mode, see this [page](qpileup_view_mode.md).

#### `merge`

`merge` mode will merge 2 or more HDF5 files together. The files must use the same reference genome, and the same values for `lowreadcount` and `percentnonref`.


### [general]
This section is required for all modes.

* range
e.g. `range=chrMT:1234-5678`, `range=chr12` or `range=all`. Here, "all" be used to write all positions in the HDF5 file.

* threads number
tool total threads will be number specified + 3.

* element
see [strand summary table](ndex_latest.md#strand-summary).

* group
Possible groups are:
** forward: all elements forward strand reads. 
** reverse: all elements: reverse strand reads
** bases: elements: A,C,G,T,N,ReferenceNo,NonreferenceNo,HighNonreference,LowReadCount
** quals: AQual,CQual,GQual,NQual,MapQual
** cigars: CigarI,CigarD,CigarD_start,CigarS,CigarS_start,CigarH,CigarH_start,CigarN,CigarN_start
** readStats: StartAll,StartNondup,StopAll,Dup,MateUnmapped

### Examples
#### Add mode

Bam files can be listed in 2 ways:
*within the ini file using name=path to bam file
*provide a file with bam files listed using bamlist=path to bam list file

~~~~
[general]
log=/qpileup/runs/test/test.log
loglevel=DEBUG
hdf=/qpileup/runs/test/target_109.qpileup.h5
mode=add
bam_override=true
thread_no=12

[add_remove]
bam_list=/qpileup/bam_list_20130202.txt OR
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_001.sorted.bam
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_003.sorted.bam
~~~~

#### Merge mode

~~~~
[general]
log=/qpileup/runs/test/test.log
loglevel=DEBUG
hdf=/qpileup/runs/test/target_109.qpileup.h5
mode=merge
bam_override=true
threadNo=12

[merge]
input_hdf==/qpileup/runs/test/testA.h5
input_hdf==/qpileup/runs/test/testB.h5
~~~~

#### View mode options for ini file

~~~~
[general]
log=/qpileup/runs/test/test.log
loglevel=DEBUG
hdf=/qpileup/runs/test/target_109.qpileup.h5
mode=add
bamoverride=true
thread_no=12
output_dir=/qpileup/runs/test/
range=chr1

[view]
; choose one of the following
element = A 
element = T  
group = forward  
~~~~


#### Metrics mode

~~~~
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
position_value=1
window_count=2

[metrics/strand_bias]
min_percent_diff=30
min_nonreference_bases=100
~~~~
