# qpileup

`qpileup` gathers base-level metrics across a whole genome from one or more BAM files. It is conceptually similar to samtools or Picard pileup in that it parses BAM files to create a summary of information for each position in a reference sequence(s). It differs from existing pileup implementations in that it captures a much larger range of metrics and it makes use of the [HDF5](http://www.hdfgroup.org/HDF5/) data storage format to store the data, allowing for smaller files sizes (via compression) and much faster access (via indexing).

## Installation

cmd="module load adamajava/nightly;module load java/1.8.77; export LD_LIBRARY_PATH=/software/hdf-java/hdf-java-2.8.0/lib/linux:${LD_LIBRARY_PATH}; qpileup --ini $ini"
echo $cmd | qsub -l walltime=20:00:00,ncpus=15,mem=20g -j oe -m ae -N test12


qpileup requires java 8 and (ideally) a multi-core machine (5 threads are run concurrently) with at least 20GB of RAM.  

* To do a build of qpileup, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qsv and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qpileup:build
  ~~~~
  This creates the qpileup jar file along with dependent jars in the `qpileup/build/flat` folder


## Usage

Before run qpileup, the hdf-java has to be installed and exported, eg.
~~~~
export LD_LIBRARY_PATH=/software/hdf-java/hdf-java-2.8.0/lib/linux:${LD_LIBRARY_PATH};
java -Xmx20g -jar qpileup.jar --ini /path/to/ini/file/demo.ini
~~~~

### Options
~~~~
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


## Options

* `--ini`
Required. Use this option to specify the iniFile containing relevant qpileup
options. See the example below.

* **`--view`**
Invoke `view` mode from the commandline. This is somewhat equivalent to
`samtools view`.


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
