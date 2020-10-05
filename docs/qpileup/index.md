# qpileup

`qpileup` gathers base-level metrics across a whole genome from one or
more BAM files. It is conceptually similar to samtools or Picard pileup
in that it parses BAM files to create a summary of information for each
position in a reference sequence(s). It differs from existing pileup
implementations in that it captures a much larger range of metrics and
it makes use of the [HDF5](http://www.hdfgroup.org/HDF5/) data storage
format to store the data, allowing for smaller files sizes (via 
compression) and much faster access (via indexing).

## Installation

qpileup requires java 7 and (ideally) a multi-core machine (5 threads
are run concurrently) with at least 20GB of RAM.  Download the 
qpileup tar file and untar into a directory of your choice.
You should see jar files for qpileup and its dependencies:

~~~~{.text}
$ tar xvf qpileup.tar.bz2
x antlr-3.2.jar
x ini4j-0.5.2-SNAPSHOT.jar
x jopt-simple-3.2.jar
x picard-1.110.jar
x qbamfilter-1.0pre.jar
x qcommon-0.1pre.jar
x qio-0.1pre.jar
x qpicard-0.1pre.jar
x qpileup-0.1pre.jar
x sam-1.110.jar
x jhdfobj.jar
x jhdf5obj.jar
x jhdf5.jar
x jhdf.jar
~~~~

## Usage

~~~~
java -Xmx20g -jar qpileup.jar --ini /path/to/ini/file/demo.ini
~~~~

## Modes

qpileup has several modes:

### `bootstrap`

`bootstrap` mode creates a qpileup HDF5 file for a reference genome, storing
position and reference base information for the genome. The output from
this mode is a complete HDF5 qpileup file but with no values in any of the
summary metrics. For a given reference genome, a user would typically run
bootstrap mode once to create a "clean" initialised qpileup HSF5 and then
use that bootstrap file as the basis for numerous qpileup BAM collections.
Using 12 threads on a cluster node with 2 x 6-core CPUs, bootstrapping
the GRCh37 human genome takes approximately 12 minutes.

For more details on `bootstrap` mode, see this
[page](qpileup_bootstrap_mode.md).

### `add`

`add` mode takes one or more BAM files as input and performs a pileup for
the reads, adding the new data to an existing qpileup file. It is expected
that `add` and `view` will be the most often used qpileup modes as this mode
allows for addition of new BAMs to existing qpileup files and "view" allows
for querying of locations in the qpileup file.

### `remove`

`remove` mode is the opposite of `add` more. It takes one or more BAM files
that are part of an existing qpileup HDF5 file and it removes the BAMs
from the collection by performing pileups and decrementing the values of
all of the qpileup metrics so it was as though the BAM file(s) had not
been added to the HDF5. This should rarely be necessary but is invaluable
in cases such as sample or tumour/normal swaps, or incorrect labelling of
BAM files.
Without this mode, any case where a BAM was incorrcetly added to an HDF5
file would require the HDF5 file to be regenerated from scratch.

### `view`

`view` mode reads metrics from a qpileup HDF5 file and writes to a CSV
file. `view` can be used for a whole genome (but don't do that unless 
you have a lot of disk - it's a really big file) or for specific
regions.
`view` takes a list of ranges and will output a separate CSV for each
chromosome/contig that is part of the list of ranges queried. Each file
will contain metadata at the top including the HDF5 file the summary was
extracted from and the regions extacted.

Unlike all of the other modes, there is a limited ability to use view
mode via commandline options to qpileup.
This mode can be to view metadata for the HDF file and the qpileup metrics
for a small (up to one chromosome) region of the reference genome.

For more details on `view` mode, see this
[page](qpileup_view_mode.md).

### `merge`

`merge` mode will merge 2 or more HDF5 files together. The files must use the
same reference genome, and the same values for `lowreadcount` and 
`percentnonref`.

### `metrics`

_Information required for custom metrics modes that have been
implemented._

## HDF5 file

HDF5 is a binary file format that allows for high-speed random access to 
very large datasets, and includes support for user-defined composite data
structures and compression. It is version 5 of HDF. 
The first version was created in 1987 and it 
has been used by hundreds of organisations worldwide including NASA, 
Deutsche Bank, Baylor College of Medicine and ANSTO.

HDF is maintained by [The HDF
Group](https://portal.hdfgroup.org/display/support) and the current
development source code
is maintained in a public git repository on their [BitBucket
server](https://bitbucket.hdfgroup.org/projects/HDFFV/repos/hdf5/browse).
HDF5 version 1.12 is the latest released version as at 2020-05-03 -
[`hdf5_1_12`](https://bitbucket.hdfgroup.org/projects/HDFFV/repos/hdf5/browse?at=refs%2Fheads%2Fhdf5_1_12)

Source packages for current and previous releases are located at:
[https://portal.hdfgroup.org/display/support/Downloads](https://portal.hdfgroup.org/display/support/Downloads).

The data stored in the qpileup HDF5 file conceptually fits into 3 categories:

* position - which relates to the reference genome, 
* strand summary - which holds the per-base metrics derived from the reads in the the BAMs added to the HDF5 file,
* metadata - which is a log of the bootstrap/add/remove operations that have been applied to the HDF5.

### position

Position is stored in the HDF as 1D Scalar datasets - Integer for postion, char (as byte) for base.

Data Elements are chunked (size=10000), with compression level of 1.

Data Element | Type | Description
----|----|----
Position | Integer | Offset of this base within the sequence. Should be 1-based so the first base is numbered 1.
Reference | Char | Reference base at this position

### strand summary

Each of the following data elements is compiled independently for each
strand so these elements will exist in the HDF5 file in `_for` (forward)
and `_rev` (reverse) versions, for example: `AQual_for` and `AQual_rev`.

Strand Data Elements are created individually as 1D Scalar Datasets. This
structure is used due to speed considerations - use of compound datasets
or 2D datasets results in much slower run times due to inefficiency of data
structure compatabilities with Java/C.

Data Elements are chunked (size=10000), with compression level of 1.

Data Element | Type | Description
----|----|----
A | Integer | Count of all the A bases observed
C | Integer | Count of all the C bases observed
G | Integer | Count of all the G bases observed
T | Integer | Count of all the T bases observed
N | Integer | Count of all the N bases observed
AQual | Long | Sum of the qualities of all the A bases at this position
CQual | Long | Sum of the qualities of all the C bases at this position
GQual | Long | Sum of the qualities of all the G bases at this position
TQual | Long | Sum of the qualities of all the T bases at this position
NQual | Long | Sum of the qualities of all the N bases at this position
MapQual | Long | Sum of the mapping qualities of all reads that provide bases at this position
StartAll | Integer | Count of all reads where alignment starts at this base (obeys clipping)
StartNondup | Integer | As for StartAll except that we only count non-duplicate reads (obeys clipping)
StopAll | Integer | Count of all reads where alignment stops at this base (obeys clipping)
Dup | Integer | Count of reads that were flagged as duplicate and have a base at this position
MateUnmapped | Integer | Count of reads at this position that have an unmapped mate
CigarI | Integer | Count of reads that have an "I" in the CIGAR string at this position. Only is counted at the first position at which the insertion occurs. Defined as where there is an insertion between this reference position and the next reference position.
CigarD | Integer | Count of reads that have an "D" in the CIGAR string at this position
CigarS | Integer | Count of reads that have an "S" in the CIGAR string at this position
CigarH | Integer | Count of reads that have an "H" in the CIGAR string at this position
CigarN | Integer | Count of reads that have an "N" in the CIGAR string at this position (only valid for RNA alignments)
CigarD_start | Integer | Count of reads that have an "D" in the CIGAR string that starts at this position
CigarS_start | Integer | Count of reads that have an "S" in the CIGAR string that starts at this position
CigarH_start | Integer | Count of reads that have an "H" in the CIGAR string that starts at this position
LowReadCount | Integer | Count of the number of BAMs that have a low number of reads covering this position. By default LowReadCOunt is set to 10 but the `lowreadcount` option in thr INI file can be used to set this in `bootstrap` mode. If a BAM has a lowreadcount at a position, it is not used when calculating HighNonreference base count.
ReferenceNo | Integer | Count of the number of bases at this position which are the same as the reference base
NonreferenceNo | Integer | Count of the number of bases at this position which are not the same as the reference base
HighNonreference | Integer | Count of the number of BAMs that have a high number of non-reference bases at this position. By default this is defined as non-reference bases accounting for at least 20% of the total number of bases for this BAM at this position. The minimum number of bases can be defined in bootstrap mode using the lowreadcount inifile option. The non-reference base percentage minimum threshold can be defined using the `percentnonref` inifile option during `bootstrap`.

### Metadata

Stored as chunked (size=1) 1D Scalar DS, with compression level of 1.
Strings are stored as bytes
Two types of metadata:

#### Record metadata

A comma-separated string with the following elements:

Data Element | Description
----|----
Mode | Mode carried out (bootstrap/add/remove)
Date | Date that the Mode was performed
Run time | Run time for the analysis
Bam | Path of BAM file added/removed
Record Count | Number of records in the BAM file

The following attributes are also associated with the metadata and are added
during bootstrap mode and potentially modified in other modes:

* `bams_added`: a count of the bams that have been added in add mode. Is modified during add mode.
* `low_read_count`: The `low_read_count` default or as set by the option from the `bootstrap` mode INI file. Cannot be modified after bootstrapping and will return an error if this option is different than the one added during bootstrap.
* `non_reference_threshold`: The percentnonref iniFile option. Cannot be modified after bootstrapping and will return an error if this option is different than the one added during bootstrap.

#### Reference metadata

The reference metadata contains length and name information for the
reference genome. The information is added during bootstrap and cannot be 
modified after this point. It is a comma separated string with the following
elements:

Data Element | Description
----|----
Sequence | Name of the reference sequence (ie chromosome or contig)
Length | Number of base pairs in the sequence Options#

## Options

~~~~{.text}
--version, -v, -V   Print version info.
--help, -h          Shows this help message.
--ini               INI file with required options.

--view              Use this option to view the HDF file header.
--hdf               HDF File that will be viewed. 
-H                  View the header of the HDF file.
--range             The range to be viewed - sequence:start-end.
--element           Which data elements to view.
--group             Which group of Select the group of qpileup data elements to view: 
                         forward, reverse, bases, quals, cigars, readStats      
~~~~

* `--ini`
Required. Use this option to specify the iniFile containing relevant qpileup
options. See the example below.

* **`--view`**
Invoke `view` mode from the commandline. This is somewhat equivalent to
`samtools view`.



### `--ini`

Required. Use this option to specify the iniFile containing relevant qpileup
options. See the example below.

### `--view`

Invoke `view` mode from the commandline. This is somewhat equivalent to
`samtools view`.

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

The INI file is divided into sections. All modes have a `[general]`
section plus one or more sections specific to the mode.

### `[general]`

This section is required for all modes.

Option | Description
-------|----
`log` | Opt, Log file.
`loglevel` | Opt, Log level [DEBUG,INFO], Def=INFO.
`hdf` | Req, HDF5 file - must have .h5 extension.
`mode` | Req, Mode [bootstrap,add,remove,merge,view].
`bam_override` | Opt, If set, allows duplicate BAM files to be added, Def=FALSE
`thread_no` | Req, Number of threads [1-12], Total threads will be number specified + 3. 
`output_dir` | Directory for output pileup files, (required for view,metrics modes).
`range` | Range to view. The format of a range is _seq:start-end_, e.g. `range=chrMT:1234-5678`. Alternatively, `range=all` can be used to write all positions in the HDF5 file, or the name of a sequence from the reference will write all positions from that sequence, e.g. `range=chr12`. Multiple ranges may be specified in which case each range will be written to a separate file.

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
