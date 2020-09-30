# qpileup bootstrap mode

`bootstrap` mode creates a HDF5 file for a reference genome, storing
position and reference base information for the genome. The output from
this mode is a complete HDF5 qpileup file but with no values in any of the
summary metrics. For a given reference genome, a user would typically run
bootstrap mode once to create a "clean" initialised qpileup HSF5 and then
use that bootstrap file as the basis for numerous qpileup BAM collections.
Using 12 threads on a cluster node with 2 x 6-core CPUs, bootstrapping
the GRCh37 human genome takes approximately 12 minutes.

## Usage

In `bootstrap` mode, there are no commandline options other than the
location of the INI file.

~~~~{.text}
java -Xmx20g -jar qpileup.jar --ini my_bootstrap.ini
~~~~

## `ini`

The INI file for `bootstrap` mode uses the `[general]` section to
specify the name of the created .h5 file, logging details and thread
count. It also uses the `[bootstrap]` section for specific bootstrap
options including the FASTA reference file and any non-default values
for `low_read_count` and `nonref_percent`.

The example below shows bootstrapping a qpileup for GRCh37 with
non-default values for `low_read_count` and `nonref_percent`.

~~~~{.text}
[general]
mode=bootstrap
hdf=my_first_GRCh37_qpileup.h5
log=my_first_GRCh37_qpileup.log
loglevel=INFO
thread_no=12

[bootstrap]
reference=/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa
low_read_count=20
nonref_percent=30
~~~~
