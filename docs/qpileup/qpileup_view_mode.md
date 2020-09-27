# qpileup view mode

`view` mode reads metrics from a qpileup HDF5 file and writes to a CSV
file. `view` can be used for a whole genome (but don't do that unless 
you have a lot of disk - it's a _really_ big file) or for specific
regions.
`view` takes a list of ranges and will output a separate CSV for each
chromosome/contig that is part of the list of ranges queried. Each file
will contain metadata at the top including the HDF5 file the summary was
extracted from and the regions extacted.

Unlike all of the other qprofiler modes, there is a limited ability to 
use view mode via commandline options.
This mode can be to view metadata for the HDF file and the qpileup metrics
for a small (up to one chromosome) region of the reference genome.

## Usage

`view` mode is the only qpileup mode where there are command line options
beyond `--ini`.

~~~~{.text}
java -Xmx20g -jar qpileup.jar --ini my_view.ini
~~~~

## Options

### INI file

If using the INI file version of view mode, the only option is the name
of the INI file:

~~~~{.text}
--ini        INI file.
~~~~

### `--ini`

Required. Use this option to specify the iniFile containing relevant qpileup
options. See the example below.

The INI file for `view` mode uses the `[general]` section to
specify the name of the .h5 file to be read, logging details and thread
count. It also uses the `[view]` section for specific view
options including the `range`(s) to be reported ...

The example below shows bootstrapping a qpileup for GRCh37 with
non-default values for `low_read_count` and `nonref_percent`.



### command line

The following command line options are only active when option `--view` is
used. 

To use view mode from the commandline, you must specify the `--view`
option, the HDF5 file to be viewed (`--hdf`) and at least one range
to be reported (`--range`). Additionally, you can use `--element` and
`--group` to specify that you only want a subset of the available data
elements to be reported. 

~~~~{.text}
--view       Use this option to view the HDF file header.
--hdf        HDF File that will be viewed. 
-H           View the header of the HDF file.
--range      The range to be viewed - sequence:start-end.
--element    Which data elements to view.
--group      Which group of Select the group of qpileup data elements to view: 
                 forward, reverse, bases, quals, cigars, readStats      
--tmp
~~~~

### `--view`

Invoke `view` mode from the commandline. `qprofiler --view` is
somewhat analagous to `samtools view`.

### `--hdf`

In `--view`, this specifies the HDF5 file to be viewed.

### `-H`

In `--view`, shows the header (metadata) for the `--hdf` file.

### `--range`

In `--view`, specifies the range of the viewing to be undertaken, e.g.
`chr1:1-1000`, `chrX:1000000-1500000`.

### `--group`

Which group of data elements to view: `forward`, `reverse`, `bases`,
`quals`, `cigars`, `readStats`.      

## INI file

~~~~{.text}
[general]
log=path to log file (required)
loglevel=logging level - DEBUG or INFO. (optional)
hdf=path to hdf file (required. Must end in .h5)
mode=bootstrap, add, remove, merge or view (required)
bam_override=whether duplicate bam files can be added. (optional. False by default)
thread_no=number of threads to use for multithreading. Must be between 1 and 12. (required). 
               Total threads will be the number specified + 3. 
output_dir=the place that pileup files are written to (required for view,metrics modes)
range=the range to view eg all, chr1, chr:1-1000. (Required for view, metrics modes. More than one range option allowed.)

[bootstrap]
reference=path to reference genome fasta file (required for bootstrap mode)
low_read_count=the number below which the LowReadCount element is defined. Also used to define the HighNonReference element. 
              If not defined, the default is 10
nonref_percent=Used for HighNonreference strand summary element. Minimum percent that non-reference bases 
to total bases must have in order to be counts as HighNonReference. If not defined the default is 20(%)

[add_remove]
bam_list=path of file with list of bams OR
name=path to bam files (Required for add, remove modes. More than one name allowed)

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
