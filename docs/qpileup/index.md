# qpileup

`qpileup` gathers base-level metrics across a whole genome from one or more BAM files. It is conceptually similar to samtools or Picard pileup in that it parses BAM files to create a summary of information for each position in a reference sequence(s). It differs from existing pileup implementations in that it captures a much larger range of metrics and it makes use of the [HDF5](http://www.hdfgroup.org/HDF5/) data storage format to store the data, allowing for smaller files sizes (via compression) and much faster access (via indexing).

## Installation

qpileup requires java 21 and (ideally) a multi-core machine. eg. 5 threads with at least 20GB of RAM; 12 threads with at least 30GB and 25 threads with 40GB etc.

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

Before you run qpileup, the hdf-java library has to be installed and exported, eg.
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
--element      Opt (view mode), Select the elements date to view. e.g. A, Aqual,etc.      
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

### usage1: `ini` file option

The INI file is divided into sections. All modes have a `[general]` section plus one or more sections specific to the mode.

~~~~{.text}

[general]
log = Req, Log file.
loglevel = Opt, Logging level [INFO,DEBUG], Def=INFO.
hdf = Req, path to HDF5 file. it is the output from bootstrap and merge mode, but input for view and metrics mode, and modified by add and remove mode. 
mode = Req, Mode [bootstrap, add, remove, merge, view, metrics].
thread_no = Opt (add,remove, view, metrics), Number of threads. Def=1
bam_override =  Opt (add,remove,merge ), If set, allows duplicate BAM files to be added, Def=false
output_dir = Req (view, metrics), Directory for output pileup files.
range = Opt (add, remove, view, metrics), Range to view. Def=all. 

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
;multi element is allowed, but only one group will be taken.
element = Opt, qpileup data element to view. see [strand summary table](ndex_latest.md#strand-summary).
group = Opt, Possible groups [forward, reverse, bases, quals, cigars, readStats]. 
graph = (Deprecated) Opt, set to true if a html should be created . Def=false.
stranded = (Deprecated) Opt, it accompany with gragh option. Def=false. 
graph_hdf = (Deprecated) Opt, hdf to view. Def = null. 

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
dbSNP = Opt,path to dbSNP VCF file, . Def=null
germlineDB = Opt,path to qSNP germlineDB file. Def=null
nonref_percent = Opt,Minimum percentage that non-reference bases that must be in order to be included as a SNP. Def=20
nonref_count = Opt, Minimum count of non reference bases in order to be included. Def=10
high_nonref_count = Opt,Minimum number of patients that had a high non_reference count. Def=0.
window_count = Req, minimum number of positions that pass the position_value in the window
snp_file = Opt, path to ???. Def=null
snp_file_format = Opt, file formate, eg. vcf. Def=null (only vcf format will be processed)
snp_file_annotation = Opt, annotation string. Def=null (not sure why & how)

[metrics/strand_bias]
min_percent_diff = Req, minimum percent difference in % nonreference bases between strands
min_nonreference_bases = Req, minimum number of non-reference bases per reference position
~~~~

#### [HDF5 file](hdf.md)
HDF5 is a binary file format that allows for high-speed random access to very large datasets. The data stored in the qpileup HDF5 file conceptually 
fits into 3 categories: [position](hdf.md), [strand summary](hdf.md) and [metadata](hdf.md). Here HDF5 file must have .h5 extension.

#### [Modes](ini.md)
Mode | Description
-----| -----------
[`bootstrap`](ini.md##bootstrap) | mode creates a qpileup HDF5 file for a reference genome.
[`add`](ini.md#add) | add BAM files pileup counts to existing HDF5 file. 
[`remove`](ini.md#remove) | remove BAM files pileup counts to existing HDF5 file. 
[`view`](ini.md#view) | metrics from a qpileup HDF5 file and writes to a CSV file.
[`merge`](ini.md#merge) | merge 2 or more HDF5 files together.
[`metrics`](ini.md#metrics) | create metrics file from a qpileup HDF5 file, similar to `view`.

### Usage2: `view` mode options 
`qpileup` offers a limited `view` mode option from the command line. Users may use this option to view the HDF header/metadata or qpileup output for a single reference genome range (maximum size is one chromosome). Some example usage are 

~~~~
#view version
java -jar qpileup.jar  --view --hdf-vesion --hdf /path/testhdf.h5

#View header
java  -jar qpileup.jar  --view -hdf-header --hdf /path/testhdf.h5

#View pileup on region of chromosome
java -Xmx20g -jar qpileup.jar  --view --hdf /path/testhdf.h5 --range chr1:1-1000

#View pileup on region of chromosome by group
java -Xmx20g -jar qpileup.jar  --view --hdf /path/testhdf.h5 --range chr1:1-1000 --group reverse

#View pileup on a chromosome by element 
java -Xmx20g -jar qpileup.jar  --view --hdf /path/testhdf.h5 --range chrX --element CigarI

#View pileup on a chromosome by element and group
java -Xmx20g -jar qpileup.jar  --view --hdf /path/testhdf.h5 --range chrMT --element CigarI --group bases
~~~~
