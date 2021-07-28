


## INI file

The INI file is divided into sections. All modes have a `[general]`
section plus one or more sections specific to the mode.

### `[general]`

This section is required for all modes.

Option | Description
-------|----
`log` | Req, Log file.
`loglevel` | Opt, Log level [DEBUG,INFO], Def=INFO.
`hdf` | Req, HDF5 file - must have .h5 extension.
`mode` | Req, Mode [bootstrap,add,remove,merge,view].
`bam_override` | Opt, If set, allows duplicate BAM files to be added, Def=FALSE
`thread_no` | Req, Number of threads [1-12], Total threads will be number specified + 3. 
`output_dir` | Req, Directory for output pileup files, (required for view,metrics modes).
;e.g. `range=chrMT:1234-5678`, `range=chr12` or `range=all`. Here, "all" be used to write all positions in the HDF5 file.
`range` | Opt, Range to view. Def=all. 


Example:

~~~~{.text}
[general]
mode=bootstrap
hdf=my_first_GRCh37_qpileup.h5
log=my_first_GRCh37_qpileup.log
loglevel=INFO
thread_no=12
~~~~

### `[bootstrap]`

This section is only used for `bootstrap` mode.

Option | Description
-------|----
`reference` | Reference genome FASTA file. Can contain one or more sequences in FASTA file.

low_read_count=the number below which the LowReadCount element is defined. Also used to define the HighNonReference element. 
              If not defined, the default is 10
nonref_percent=Used for HighNonreference strand summary element. Minimum percent that non-reference bases 
to total bases must have in order to be counts as HighNonReference. If not defined the default is 20(%)

~~~~{.text}
[bootstrap]
reference=path to reference genome fasta file (required for bootstrap mode)
low_read_count=the number below which the LowReadCount element is defined. Also used to define the HighNonReference element. 
              If not defined, the default is 10
nonref_percent=Used for HighNonreference strand summary element. Minimum percent that non-reference bases 
to total bases must have in order to be counts as HighNonReference. If not defined the default is 20(%)
~~~~

### `[add]` or `[remove]`

~~~~{.text}
[add_remove]
bam_list=path of file with list of bams OR
name=path to bam files (Required for add, remove modes. More than one name allowed)
~~~~


[merge]
input_hdf=path to the hdf file/s that will be merged (required for merge mode)

[view]
group=Group of qpileup data elements to view. Possible groups are: forward, reverse, bases, quals, cigars, readStats. (Optional for view mode)
element=Qpileup data element to view. See strand summary table above: eg A, Aqula,CigarI etc. (Optional for view mode)

[metrics]
min_bases=minimum average coverage (base count) per reference position
bigwig_path=directory where wigToBigWig program is located
chrom_sizes=file listing chromosome lengths for wigToBigWig conversion

[metrics/clip]
position_value=minimum value as percent of total bases per position
window_count=minimum number of positions that pass the position_value in the window

[metrics/nonreference_base]
position_value=minimum value as percent of total bases per position
window_count=minimum number of positions that pass the position_value in the window

[metrics/indel]
position_value=minimum value as percent of total bases per position
window_count=minimum number of positions that pass the position_value in the window

[metrics/mapping_qual]
position_value=maximum value as percent of total bases per position
window_count=minimum number of positions that pass the position_value in the window

[metrics/high_coverage]
position_value=minimum value as percent of total bases per position
window_count=minimum number of positions that pass the position_value in the window

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


### `view` mode options

`qpileup` offers a limited `view` mode option from the command line. Users
may use this option to view the HDF header/metadata or qpileup output for a
single reference genome range (maximum size is one chromosome)

Option | Description
----|----
--view | Required
--H | View header only
--V | View version information
--range | The range to view. eg chr1 or chr1:1-1000
--hdf | Path to the HDF file to view. Required in view mode
--element | qpileup data element to view (see strand summary table above: eg A, Aqula,CigarI etc). Optional for view mode
--group | Group of qpileup data elements to view. Optional for view mode. 

Possible groups are:

* forward: all elements forward strand reads. 
+ reverse: all elements: reverse strand reads
* bases: elements: A,C,G,T,N,ReferenceNo,NonreferenceNo,HighNonreference,LowReadCount
* quals: AQual,CQual,GQual,NQual,MapQual
* cigars: CigarI,CigarD,CigarD_start,CigarS,CigarS_start,CigarH,CigarH_start,CigarN,CigarN_start
* readStats: StartAll,StartNondup,StopAll,Dup,MateUnmapped


## Examples

### Add mode options for ini file

Bam files can be listed in 2 ways:
within the ini file using name=path to bam file
provide a file with bam files listed using bamlist=path to bam list file

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

### Merge mode options for ini file

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

### View mode options for ini file

~~~~
[general]
log=/qpileup/runs/test/test.log
loglevel=DEBUG
hdf=/qpileup/runs/test/target_109.qpileup.h5
mode=add
bamoverride=true
thread_no=12
output_dir=/qpileup/runs/test/
range=all

[view]
; choose one of the following
range=all        ;View whole genome
range=chr1       ;View whole chromosome
range=chr1:1-1000    ;View part of a chromosome
range=chr1:1-1000 --element A    ;View part of a chromosome, A base element only
range=chr1:1-1000 --group forward    ;View part of a chromosome, forward group elements only
~~~~

### View mode (command line)

View version

~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -V --hdf /qpileup/runs/test/testhdf.h5
~~~~

View header

~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -H --hdf /qpileup/runs/test/testhdf.h5
~~~~

View pileup for region of chromosome

~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -H --hdf /qpileup/runs/test/testhdf.h5 --range chr1:1-1000
~~~~

### Metrics mode

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
