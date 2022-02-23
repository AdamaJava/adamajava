
## HDF5 file

HDF5 is a binary file format that allows for high-speed random access to 
very large datasets, and includes support for user-defined composite data
structures and compression. It is version 5 of HDF. 
The first version was created in 1987 and it 
has been used by hundreds of organisations worldwide including NASA, 
Deutsche Bank, Baylor College of Medicine and ANSTO.

HDF is maintained by [The HDF Group](https://portal.hdfgroup.org/display/support) and the current
development source code is maintained in a public git repository on their [BitBucket
server](https://bitbucket.hdfgroup.org/projects/HDFFV/repos/hdf5/browse).
HDF5 version 1.12 is the latest released version as at [`2020-05-03-hdf5_1_12`](https://bitbucket.hdfgroup.org/projects/HDFFV/repos/hdf5/browse?at=refs%2Fheads%2Fhdf5_1_12).

Source packages for current and previous releases are located at [here](https://portal.hdfgroup.org/display/support/Downloads).

The data stored in the qpileup HDF5 file conceptually fits into 3 categories:
* [position](#position)  - which relates to the reference genome, 
* [strand summary](#strand-summary) - which holds the per-base metrics derived from the reads in the the BAMs added to the HDF5 file,
* [metadata](#metadata) - which is a log of the bootstrap/add/remove operations that have been applied to the HDF5.

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
and `_rev` (reverse) versions, for example: `Aqual_for` and `Aqual_rev`.

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
Aqual | Long | Sum of the qualities of all the A bases at this position
Cqual | Long | Sum of the qualities of all the C bases at this position
Gqual | Long | Sum of the qualities of all the G bases at this position
Tqual | Long | Sum of the qualities of all the T bases at this position
Nqual | Long | Sum of the qualities of all the N bases at this position
MapQual | Long | Sum of the mapping qualities of all reads that provide bases at this position
StartAll | Integer | Count of all reads where alignment starts at this base (obeys clipping)
StartNondup | Integer | As for StartAll except that we only count non-duplicate reads (obeys clipping)
StopAll | Integer | Count of all reads where alignment stops at this base (obeys clipping)
DupCount | Integer | Count of reads that were flagged as duplicate and have a base at this position
MateUnmapped | Integer | Count of reads at this position that have an unmapped mate
CigarI | Integer | Count of reads that have an "I" in the CIGAR string at this position. Only is counted at the first position at which the insertion occurs. Defined as where there is an insertion between this reference position and the next reference position.
CigarD | Integer | Count of reads that have an "D" in the CIGAR string at this position
CigarS | Integer | Count of reads that have an "S" in the CIGAR string at this position
CigarH | Integer | Count of reads that have an "H" in the CIGAR string at this position
CigarN | Integer | Count of reads that have an "N" in the CIGAR string at this position (only valid for RNA alignments)
CigarD_start | Integer | Count of reads that have an "D" in the CIGAR string that starts at this position
CigarS_start | Integer | Count of reads that have an "S" in the CIGAR string that starts at this position
CigarH_start | Integer | Count of reads that have an "H" in the CIGAR string that starts at this position
CigarN_start | Integer | Count of reads that have an "N" in the CIGAR string that starts at this position
LowReadCount | Integer | Count of the number of BAMs that have a low number of reads covering this position. By default LowReadCOunt is set to 10 but the `lowreadcount` option in thr INI file can be used to set this in `bootstrap` mode. If a BAM has a lowreadcount at a position, it is not used when calculating HighNonreference base count.
ReferenceNo | Integer | Count of the number of bases at this position which are the same as the reference base
NonreferenceNo | Integer | Count of the number of bases at this position which are not the same as the reference base
HighNonreference | Integer | Count of the number of BAMs that have a high number of non-reference bases at this position. By default this is defined as non-reference bases accounting for at least 20% of the total number of bases for this BAM at this position. The minimum number of bases can be defined in bootstrap mode using the lowreadcount inifile option. The non-reference base percentage minimum threshold can be defined using the `percentnonref` inifile option during `bootstrap`.

### metadata

Stored as chunked (size=1) 1D Scalar DS, with compression level of 1.
Strings are stored as bytes. Two types of metadata:

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

