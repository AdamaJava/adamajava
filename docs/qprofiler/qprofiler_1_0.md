# qprofiler v1.0

_This document refers to an old version of qprofiler.  The current version
is [2.0](qprofiler_2_0.md)_

`qprofiler` provides quality control reporting for next-generation
BAM or FASTQ files and provides an XML file containing basic summary
statistics for each file. If no output file is specified (a supplied
file with a <code>.xml</code> extension is considered an output file),
then the process will write to a default file (currently 
<code>qprofiler.xml</code>).

While the XML file is useful for extracting values for further analysis,
a visual representation of the data is more useful in most cases so 
another tool [qvisualise](../qvisualise/) was created to parse
the qprofiler XML files and produce HTML output with embedded graphs
via the [Charts](https://developers.google.com/chart/) javascript library
developed by Google.  `qvisualise` exists as a standalone program but it
is also integrated into `qprofiler` so a HTML file will always be output 
by `qprofiler` unless the `--nohtml` tag is used. The HTML file name is 
based on the XML output filename with  the extension `.html` appended.

qprofiler uses the [picard](https://github.com/broadinstitute/picard)
library to access BAM files.

## System Requirements

* Java 1.7
* Multi-core machine (ideally) and 12GB of RAM

## Installation

* Download the [qprofiler tar file](http://sourceforge.net/projects/adamajava/files/qprofiler.tar.bz2/download)
* Untar the tar file into a directory of your choice

You should see something like this:

~~~~{.text}
$ tar xvf qprofiler.tar.bz2
jopt-simple-3.2.jar
picard-1.110.jar
qcommon-0.1pre.jar
qio-0.1pre.jar
qmule-0.1pre.jar
qpicard-0.1pre.jar
qprofiler-1.0.jar
qvisualise-0.1pre.jar
sam-1.110.jar
[oholmes@minion0 test_adama_tools_data]$
~~~~

## Usage

~~~~{.text}
java -jar qprofiler-1.0.jar
     -input <full_path_to_input_bam>
     -output <full_path_to_output_xml>
     -log <full_path_to_log_file>
     -ntC <number_of_consuming_threads> OPTIONAL
     -ntP <number_of_producing_threads> OPTIONAL
     -validation Possible values: {STRICT, LENIENT, SILENT}
~~~~

## Options

~~~~{.text}
--help           Show this help message
--version        Show version.

--input          Req, Input file in FASTQ, BAM or VCF format.
--include        Opt, SOLiD visualisations

--output         Opt, Output XML file, Def=qprofiler.xml
--maxRecords     Opt, Process a limited number of reads
--ntProducer     Opt, Producer thread count - BAM processing only
--ntConsumer     Opt, Consumer thread count - BAM processing only
--log            Opt, Log file.
--loglevel       Opt, Logging level [INFO,DEBUG,ALL], Def=INFO.
--validation     Opt, Validation stringency [STRICT,LENIENT,SILENT], Def=SILENT.

--include        Deprecated, Visualisation for SOLiD platform.
~~~~

### `--include`

This mode produces additional visualisations for the Life Technolgies
SOLiD platform. As this platform was abandoned in 2015, this option is
deprecated and the underlying code is not maintained.
It is not thread-safe and will not work correctly
in conjunction with the `-ntC` and `-ntP` options.
It will be removed in a subsequent release.

### `--maxRecords`

Specify how many records should be parsed by the qprofiler. Note that
qprofiler will always start at the beginning of a BAM file, meaning that
you will always get the first `maxRecords` records back. This option is
designed for testing or for when you want a quick look at a BAM and can't
wait for the full file to be processed.

### `--ntProducer`

Optional and only relevant to BAM files. Specifies how many threads
(integer) should be used to '''produce''' reads from the input file.

### `--ntConsumer`

Optional and only relevant to BAM files. Specifies how many threads
(integer) should be used to '''consume''' reads from the input file.

### `--tags`

Perform aggregations on user defined tags for BAM files. Example values
are `ZC`, `XY`, etc.  This option is considered legacy and may be 
deprecated in a future release. As the contents of BAM files has 
stabilised, custom reporting and visualisations have been created for 
the most common and useful tags.

### `--loglevel`

Level at which logging should be applied. Possible values in increasing
order of detail are INFO, DEBUG, ALL. At DEBUG level and above, the logging
is very granular so you should not use these levels unless you truly are 
debugging a qprofiler run. *optional, defaults to INFO*

### `--validation`

How strict to be when reading a SAM or BAM file. Possible values are STRICT,
LENIENT, SILENT and the default is SILENT.  This value is passed to the 
`Picard` library as the parameter `Validation Stringency`

## Examples

For example, to run on a 16 core node:

~~~~
java -jar qprofiler-1.0.jar \
     -input /sample_virus.BWA-backtrack.bam \
     -log /sample_virus.BWA-backtrack.bam.qp.log \
     -output /sample_virus.BWA-backtrack.bam.qp.xml \
     -ntP 4 -ntC 16
~~~~

NOTE that BWA mapped BAM files may need to be run with the optional
parameter `-validation SILENT` otherwise Picard will throw an exception.
