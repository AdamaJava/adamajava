# qpileup

`qpileup` gathers base-level metrics across a whole genome from one or more BAM files. It is conceptually similar to samtools or Picard pileup in that it parses BAM files to create a summary of information for each position in a reference sequence(s). It differs from existing pileup implementations in that it captures a much larger range of metrics and it makes use of the [HDF5](http://www.hdfgroup.org/HDF5/) data storage format to store the data, allowing for smaller files sizes (via compression) and much faster access (via indexing).

## Installation

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
usage1: java -jar qpileup.jar --ini <ini_file> 
usage2: java -jar qpileup.jar --view --hdf <hdf_file> [options]
Option         Description                                                                
------         -----------                                                                
--element      Opt (view mode), Select the elements date to view. e.g. A, AQual,etc.      
--group        Opt (view mode), Select the group data to view. e.g. forward, reverse, etc.
--hdf          Req (view mode), HDF File that will be written to or viewed.               
--hdf-header   Opt (view mode), View the header of the HDF file.                          
--hdf-version  Opt (view mode), View HDF file version information.                        
--help         Show usage and help.                                                       
--ini          Req, ini file with required options.                                       
--range        Opt (view mode), Range to view. Def=all.                                   
--version      Show version number.                                                       
--view         Req (view mode), Use this option to invoke view mode.    
~~~~

There are two type options to run qpileup. One is to specify all relevant qpileup options in a INI file, as show on `usage`, another is use `--view` command line option, as shown on `usage2`.  

### INI file

The INI file is divided into sections. All modes have a `[general]` section plus one or more sections specific to the mode.

~~~~{.text}

[general]
log = Req, Log file.
loglevel = Opt, Logging level [INFO,DEBUG], Def=INFO.
hdf = Req, HDF5 file ( must have .h5 extension). it is the output of bootstrap and merge mode, but input of view and metrics mode. ??not sure for add/remove
mode = Req, Mode [bootstrap,add,remove,merge,view].
thread_no = Opt, Number of threads [1-12]. Def=1
bam_override =  Opt (add and remove mode), If set, allows duplicate BAM files to be added, Def=FALSE
output_dir = Req (view and metrics mode), Directory for output pileup files.
range = Opt (view, metrics, add), Range to view. Def=all. 

[bootstrap] 
;the merge mode will call bootstrap, so this section is for both bootstrap and merge mode
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

#### `add`

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
hdf=/qpileup/runs/test/target_109.qpileup.h5

bam_override=true
thread_no=12
log=/qpileup/runs/test/test.log
mode=add

[add_remove]
bam_list=/qpileup/bam_list_20130202.txt
~~~~

`Caution: adding BAMs to h5 with different range`. eg add another bam within different range to hdf from above add mode run. Hence the output will be chaotic, this should rarely be necessary. Copy an outpuyt hdf file from bootstrap mode here, will be much relaible. 

~~~
[general]
hdf=/qpileup/runs/test/target_109.qpileup.h5
log=/qpileup/runs/test/test.log
range=chr1:1000-2000
thread_no=12
mode=add

[add_remove]
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_001.sorted.bam
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_003.sorted.bam
~~~~


#### `remove`

`remove` mode is the opposite of `add` more. It takes one or more BAM files that are part of an existing qpileup HDF5 file and it removes the BAMs
from the collection by performing pileups and decrementing the values of all of the qpileup metrics so it was as though the BAM file(s) had not
been added to the HDF5. This should rarely be necessary but is invaluable in cases such as sample or tumour/normal swaps, or incorrect labelling of
BAM files.
Without this mode, any case where a BAM was incorrcetly added to an HDF5 file would require the HDF5 file to be regenerated from scratch.

~~~
[general]
;range and output_dir are ignored in remove mode
;default value used for bam_override and loglevel  

hdf=/qpileup/runs/test/target_109.qpileup.h5
log=/qpileup/runs/test/test.log
thread_no=12
mode=remove

[add_remove]
name=/ABCD_1234/T02_20120318_153_FragBC.nopd.IonXpress_001.sorted.bam
~~~~


#### `view`

`view` mode reads metrics from a qpileup HDF5 file and writes to a CSV file. `view` can be used for a whole genome (but don't do that unless 
you have a lot of disk - it's a really big file) or for specific regions. `view` takes a list of ranges and will output a separate CSV for each chromosome/contig that is part of the list of ranges queried. Each file will contain metadata at the top including the HDF5 file the summary was extracted from and the regions extacted.

Unlike all of the other modes, there is a limited ability to use view mode via commandline options to qpileup. This mode can be to view metadata for the HDF file and the qpileup metrics for a small (up to one chromosome) region of the reference genome. For more details on `view` mode, see this [page](qpileup_view_mode.md).

~~~~
[general]
;default range=all, the output will be large, not recommand, unless necessary. 

log=/qpileup/runs/test/test.log
hdf=/qpileup/runs/test/target_109.qpileup.h5
mode=add
thread_no=12

;output_dir is required
output_dir=/qpileup/runs/test/

[view]
element = A 
element = T  
group = forward  
~~~~

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

#### `merge`

`merge` mode will merge 2 or more HDF5 files together. The files must use the same reference genome, and the same values for `lowreadcount` and `percentnonref`.
the [boostrap] section is required, the merge will call boostrap mode to create a new hdf file. 

~~~~
[general]
;bam_override, output_dir and range are ignored in merge mode. 

log=/qpileup/runs/test/test.log

;this hdf file must not exists
hdf=/qpileup/runs/test/target_109.qpileup.h5

threadNo=12
mode=merge

[bootstrap]
reference=/path/to/reference/GRCh37_ICGC_standard_v2.fa
low_read_count=10
nonref_percent=30

[merge]
input_hdf==/qpileup/runs/test/testA.h5
input_hdf==/qpileup/runs/test/testB.h5
~~~~


#### Metrics mode

~~~~
[general]

;hdf is the input for metrics mode, must exists. 
hdf=/qpileup/runs/test/target_109.qpileup.h5

;output_dir, range is required in metric mode
output_dir=/qpileup/runs/test/
range=chr1

thread_no=12
log=/qpileup/runs/test/test.log
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
position_value=1
window_count=2

[metrics/strand_bias]
min_percent_diff=30
min_nonreference_bases=100
~~~~

### `view` mode options (command line)
`qpileup` offers a limited `view` mode option from the command line. Users may use this option to view the HDF header/metadata or qpileup output for a single reference genome range (maximum size is one chromosome)

View version

~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -V --hdf /qpileup/runs/test/testhdf.h5
~~~~

View header

~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -H --hdf /qpileup/runs/test/testhdf.h5
~~~~

View pileup on region of chromosome

~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -H --hdf /qpileup/runs/test/testhdf.h5 --range chr1:1-1000
~~~~

View pileup on region of chromosome by group
~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -H --hdf /qpileup/runs/test/testhdf.h5 --range chr1:1-1000 --group reverse
~~~~

Here possible groups are:
+ reverse: all elements: reverse strand reads
* bases: elements: A,C,G,T,N,ReferenceNo,NonreferenceNo,HighNonreference,LowReadCount
* quals: AQual,CQual,GQual,NQual,MapQual
* cigars: CigarI,CigarD,CigarD_start,CigarS,CigarS_start,CigarH,CigarH_start,CigarN,CigarN_start
* readStats: StartAll,StartNondup,StopAll,Dup,MateUnmapped

View pileup on region of chromosome by element 
~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -H --hdf /qpileup/runs/test/testhdf.h5 --range chr1:1-1000 --element CigarI
~~~~

View pileup on region of chromosome by element and group
~~~~
java -Xmx20g -jar qpileup-0.1pre.jar  --view -H --hdf /qpileup/runs/test/testhdf.h5 --range chr1:1-1000 --element CigarI --group bases
~~~~
