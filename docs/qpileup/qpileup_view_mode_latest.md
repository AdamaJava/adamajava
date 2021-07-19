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
### `view` mode options
`view` mode is the only qpileup mode where there are command line options beyond `--ini`. 

`qpileup` offers a limited `view` mode option from the command line. Users may use this option to view the HDF header/metadata or qpileup output for a single reference genome range (maximum size is one chromosome)

Option | Description
----|----
--view | Req, Invoke view mode from the commandline.
--H | Show HDF header (metadata), view mode only.
--V | Show HDF version information, view mode only
--range | Req, the range to view. eg chr1 or chr1:1-1000, view mode only
--hdf | Req, path to the HDF5 file. 
--element | Opt, qpileup data element to view, view mode only. eg A, Aqula,CigarI. 
--group | Opt, Group of qpileup data elements to view, [forward, reverse, bases, quals, cigars, readStats].

Possible groups are:

* forward: all elements forward strand reads. 
* reverse: all elements: reverse strand reads
* bases: elements: A,C,G,T,N,ReferenceNo,NonreferenceNo,HighNonreference,LowReadCount
* quals: AQual,CQual,GQual,NQual,MapQual
* cigars: CigarI,CigarD,CigarD_start,CigarS,CigarS_start,CigarH,CigarH_start,CigarN,CigarN_start
* readStats: StartAll,StartNondup,StopAll,Dup,MateUnmapped

### INI file

If using the INI file version of view mode, the only option is the name of the INI file.
~~~~
export LD_LIBRARY_PATH=/software/hdf-java/hdf-java-2.8.0/lib/linux:${LD_LIBRARY_PATH};
java -Xmx20g -jar qpileup.jar --ini ./view.ini
~~~~

The INI file for `view` mode uses the `[general]` section to specify the name of the .h5 file to be read, logging details and thread
count. It also uses the `[view]` section for specific view options including the `range`(s) to be reported ...

The example below shows bootstrapping a qpileup for GRCh37 with non-default values for `low_read_count` and `nonref_percet`.


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

~~~~
export LD_LIBRARY_PATH=/software/hdf-java/hdf-java-2.8.0/lib/linux:${LD_LIBRARY_PATH};
#View version
java -Xmx20g -jar qpileup.jar  --view -V --hdf ./test/testhdf.h5

#View header
java -Xmx20g -jar qpileup.jar  --view -H --hdf ./test/testhdf.h5

#View pileup for region of chromosome
java -Xmx20g -jar qpileup.jar  --view --hdf test/testhdf.h5 --range chr1:950-1000
~~~~

