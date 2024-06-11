# qprofiler 

`qprofiler` provides quality control reporting for next-generation sequencing(NGS). `qprofiler` takes FASTQ, BAM/SAM, FASTA, QUAL,GFF3, MA and VCF files as input and outputs an XML file containing summary statistics tailored to the input file type. If no output file is specified then the default output file name will be used: `qprofiler.xml`.

While the XML file is useful for extracting values for further analysis, a visual representation of the data is more useful in most cases so another tool qvisualise was created to parse the qprofiler XML files and produce HTML output with embedded graphs via the Charts javascript library developed by Google. qvisualise exists as a standalone program but it is also integrated into qprofiler so a HTML file will always be output by qprofiler unless the --nohtml tag is used. The HTML file name is based on the XML output filename with the extension .html appended.

## Installation
qprofiler requires java 21, a machine with multi cores and 5GB of RAM will be ideally.

To do a build of qprofiler, first clone the adamajava repository and move into the adamajava folder.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  cd adamajava
  git tag
  ~~~~

pick up a release, eg. "internal-7.a8ab31c.12803".
  ~~~~{.text}
  git checkout tags/internal-7.a8ab31c.12803
  ~~~~

Run gradle to build qprofiler and its dependent jar files.
  ~~~~{.text}
  ./gradlew :qprofiler:build
  ~~~~
This creates the qprofiler jar file along with dependent jars in the qprofiler/build/flat folder.

## Usage
If the release number is before 23.8546a7b, the main jar file name is "qprofiler-1.0.jar".
  ~~~~{.text}
  usage: java -jar qprofiler-1.0.jar [option...] -log logfile -loglevel INFO -output outputfile -input inputfile1 -input inputfile2 ... [-ntP 4 -ntC 16] [-exclude coverage,matrices,md,html] [-maxRecords 10000] [-tags XY,XZ,YZ]
  ~~~~

Release number is 23.8546a7b or after, the main jar file name is "qprofiler.jar".
  ~~~~{.text}
  usage: java -jar qprofiler.jar [option...] -log logfile -loglevel INFO -output outputfile -input inputfile1 -input inputfile2 ... [-ntP 4 -ntC 16] [-exclude coverage,matrices,md,html] [-maxRecords 10000] [-tags XY,XZ,YZ]
  ~~~~

The full option list is describe below but there are 3 options that you should probably specify every time you call qprofiler: `--input`, `--output`, and `--log`. If you have access to a multi-core machine (e.g. a compute node on a cluster) then you should also look at the thread-count parameters: `--ntProducer` and `--ntConsumer` if you are processing BAM files.

In general, we would recommend using as many consumer threads as you have cores available (so 16 consumers for a 16-core machine) and with approximately a 1:4 ratio between producer and consumer threads.  The producer threads are relatively lightweight and will not occupy a full core each.

For example, to run on a 16 core computer, we would suggest something like:

~~~~{.text}
java -jar qprofiler-1.0.jar \
     -input ~/sample_virus.BWA-backtrack.bam \
     -log ~/sample_virus.BWA-backtrack.bam.qp.log \
     -output ~/sample_virus.BWA-backtrack.bam.qp.xml \
     -ntP 4 -ntC 16
~~~~

The recommendations on counts of consumer and producer threads are empirical so if you are going to do lots of qprofiler work, you should probably do some testing of your own to see what thread counts and ratios work best on your servers or cluster nodes. This is especially important for cluster work where core count is critical - if you request 8 cores, you need to make sure that your threading parameters are dialled to keep qprofiler inside the number of cores you requested. It's also worth noting that hyperthreaded cores can cause the counts to be off - clusters may count each hyperthreaded core as two cores, i.e. capable of running 2 threads, but they will not be as efficient as 2 separate cores so again you will need some empirical testing to see what thread counts and producer/consumer ratios work best for you.

It is also worth noting that it is not unusual to find BAM files that contain headers or reads considered to be invalid by the Picard library which will throw exceptions and cause qprofiler to exit. This is why the default option for `--validation` is `SILENT` but this is _not_ an ideal situation.  If you are primarily a consumer of BAMs then it's probably OK to mostly operate in `SILENT` mode but if anything odd happens with your output, you should rerun with `STRICT` or `LENIENT` to see if there
are problems with the BAM.  If, on the other hand, you are a BAM producer, you should probably use `STRICT` and if any of your BAMs cause exceptions to be thrown, you should try to fix the underlying causes.

## Options

~~~~{.text}
Option                  Description
------                  -----------
--help                  Shows this help message.
--include               Include certain aggregations. Possible
                          values are "matrices", "coverage"
                          for BAM files.
--index                 File containing data to be profiled
                          (currently limited to BAM/SAM,
                          FASTQ, FASTA, QUAL, GFF3, MA)
--input                 File containing data to be profiled
--log                   File where log output will be directed
                          (must have write permissions)
--loglevel              Logging level required, e.g. INFO,
                          DEBUG. (Optional) If no parameter is
                          specified, will default to INFO
--maxRecords <Integer>  Only process the first {0} records in
                          the BAM file.
--nohtml                If this option is set, qvisualise will
                          NOT be called after qprofiler has
                          been run and so no html output will
                          be generated
--ntConsumer <Integer>  specify how many threads should be
                          used when processing the input file
                          (BAM files only)
--ntProducer <Integer>  specify how many threads should be
--output                File where the output of the qprofiler
                          should be written to (needs to be an
                          xml file)
--tags                  Perform aggregations on user defined
                          tags (Strings). Example values are
                          "ZC", "XY", etc.
--tagsChar              Perform aggregations on user defined
                          tags (chars). Example values are
--tagsInt               Perform aggregations on user defined
                          tags (ints). Example values are
--validation            How strict to be when reading a SAM or
                          BAM. Possible values: {STRICT,
                          LENIENT, SILENT}
--version               Print version info.

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

### `--format`

Group VCF records according to user-specified format fields.

### `--fullBamHeader`

By default, only `@HD` and `@SQ` lines from the BAM header are added to
the qprofiler2 XML report. The reason for this is that other header
lines may bleed information such as sample and library ids.
We'd like the XML report file to be something that can be freely shared
without risk of exposing sensitive information. By using this option,
the entire BAM header will be placed in the XML report so only use this
option if you have thought through the remifications.

### `--ntProducer`

Optional and only relevant to BAM files. Specifies how many threads
(integer) should be used to _produce_ reads from the input file.

### `--ntConsumer`

Optional and only relevant to BAM files. Specifies how many threads
(integer) should be used to _consume_ reads from the input file.

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

## Output

This example output shows XML from running `qrofiler` against a BAM file. 
This is a high level view and for readability, some of the ines have
been wrapped and most of the contents have been elided (`...`).

~~~~{.xml}
<qProfiler finish_time="2017-07-05 22:09:53" run_by_os="Linux" run_by_user="christiX"
           start_time="2017-07-05 17:36:42" version="2.0 (1954)">
  <BAMReport execution_finished="2017-07-05 22:09:33" execution_started="2017-07-05 17:36:42"
             file="/path/where/we/keep/bams/0f443106-e17d-4200-87ec-bd66fe91195f.bam">
    <HEADER>...</HEADER>
    <SUMMARY>...</SUMMARY>
    <SEQ>...</SEQ>
    <QUAL>...</QUAL>
    <TAG>...</TAG>
    <ISIZE>...</ISIZE>
    <RNEXT>...</RNEXT>
    <CIGAR>...</CIGAR>
    <MAPQ>...</MAPQ>
    <RNAME_POS>...</RNAME_POS>
    <FLAG>...</FLAG>
  </BAMReport>
</qProfiler>
~~~~

## Log file

This example log file is from running `qrofiler` against a BAM file. The 
majority of the log file has been elided (`...`) to save space.

~~~~{.text}
17:36:42.356 [main] EXEC org.qcmg.qprofiler.QProfiler - Uuid c637af74-2c8f-4682-944a-ccd42dd57967
17:36:42.357 [main] EXEC org.qcmg.qprofiler.QProfiler - StartTime 2017-07-05 17:36:42
17:36:42.358 [main] EXEC org.qcmg.qprofiler.QProfiler - OsName Linux
17:36:42.358 [main] EXEC org.qcmg.qprofiler.QProfiler - OsArch amd64
17:36:42.359 [main] EXEC org.qcmg.qprofiler.QProfiler - OsVersion 3.10.0-327.3.1.el7.x86_64
17:36:42.360 [main] EXEC org.qcmg.qprofiler.QProfiler - RunBy christiX
17:36:42.360 [main] EXEC org.qcmg.qprofiler.QProfiler - ToolName qprofiler
17:36:42.361 [main] EXEC org.qcmg.qprofiler.QProfiler - ToolVersion 2.0 (1954)
17:36:42.362 [main] EXEC org.qcmg.qprofiler.QProfiler - CommandLine qprofiler --log /mnt/lustre/home/christiX/qprofiler/colo_829.analysis/qprofiler2.0/output/0f443106-e17d-4200-87ec-bd66fe91195f.bam.qp.xml.log --loglevel INFO --output /mnt/lustre/home/christiX/qprofiler/colo_829.analysis/qprofiler2.0/output/0f443106-e17d-4200-87ec-bd66fe91195f.bam.qp.xml --input /mnt/lustre/working/genomeinfo/sample/c/9/c9a6be94-bdb7-4c0d-a89d-4addbf76e486/aligned_read_group_set/0f443106-e17d-4200-87ec-bd66fe91195f.bam -ntP 4 -ntC 20
17:36:42.363 [main] EXEC org.qcmg.qprofiler.QProfiler - JavaHome /software/java/jdk1.8.0_77/jre
17:36:42.363 [main] EXEC org.qcmg.qprofiler.QProfiler - JavaVendor Oracle Corporation
17:36:42.364 [main] EXEC org.qcmg.qprofiler.QProfiler - JavaVersion 1.8.0_77
17:36:42.365 [main] EXEC org.qcmg.qprofiler.QProfiler - host hpcnode040.adqimr.ad.lan
17:36:42.367 [main] TOOL org.qcmg.qprofiler.QProfiler - Running in multi-threaded mode (BAM files only). No of available processors: 56, no of requested consumer threads: 20, producer threads: 4
17:36:42.415 [main] INFO org.qcmg.qprofiler.QProfiler - processing file /mnt/lustre/working/genomeinfo/sample/c/9/c9a6be94-bdb7-4c0d-a89d-4addbf76e486/aligned_read_group_set/0f443106-e17d-4200-87ec-bd66fe91195f.bam
17:36:42.418 [pool-1-thread-1] INFO org.qcmg.qprofiler.QProfiler - running BamSummarizerMT
17:36:42.770 [pool-1-thread-1] INFO org.qcmg.qprofiler.bam.BamSummarizerMT - will create 20 consumer threads
17:36:42.777 [pool-1-thread-1] INFO org.qcmg.qprofiler.bam.BamSummarizerMT - waiting for Producer thread to finish (max wait will be 20 hours)
17:36:42.948 [pool-3-thread-2] INFO org.qcmg.qprofiler.bam.BamSummarizerMT$Producer - retrieving records for sequence: chr1
17:36:42.969 [pool-3-thread-1] INFO org.qcmg.qprofiler.bam.BamSummarizerMT$Producer - retrieving records for sequence: chr2
17:36:42.974 [pool-3-thread-4] INFO org.qcmg.qprofiler.bam.BamSummarizerMT$Producer - retrieving records for sequence: chr3
...
22:09:54.595 [main] EXEC org.qcmg.qprofiler.QProfiler - StopTime 2017-07-05 22:09:54
22:09:54.595 [main] EXEC org.qcmg.qprofiler.QProfiler - TimeTaken 04:33:12
22:09:54.595 [main] EXEC org.qcmg.qprofiler.QProfiler - ExitStatus 0
~~~~
