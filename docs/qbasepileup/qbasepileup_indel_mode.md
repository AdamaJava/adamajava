# qbasepileup indel mode

~~~~{.text}
java -jar qbasepileup.jar -m indel ...
~~~~

In this mode, qbasepileup reads tumour and normal BAM files, a reference
genome, and somatic and/or germline files containing positions of
indels. It creates pileups of the reads around the indel to count a number
of metrics. Metrics include:

* total reads
* number of reads that span the indel
* number of reads with the indel
* number of novel starts with the indel
* number of reads with nearby soft clipping
* number of reads with nearby indels.

## Options


Option order is arbitrary.

~~~~{.text}
--help      Show help message.
--version   Show version.

--log       Required. Path to log file.
--mode      Required. Specify "indel"
--it        Required. Path to tumour bam file
--in        Required. Path to normal bam file.
--r         Required. Path to reference genome
--is        Required unless --ig option is present. Path to somatic dcc1 input file.
--ig        Required unless --is options is present. Path to germline dcc1 input file.
--os        Required if --is option is present. Path to somatic dcc1 outut file.
--og        Required if --ig option is present. Path to germline dcc1 output file.

--loglevel  Optional. Logging level, e.g. INFO,DEBUG. Default INFO.
--t         Optional. Thread number. Total thread number = number supplied + 2. Default 1 (total threads 3).
--filter    Optional. Qbamfilter query to use
--sc        Optional. Window of reference bases either side of indel to look for soft clipping. Default 13
--hp        Optional. Window of reference bases either side of indel to look for homopolymer. Default 10
--n         Optional. Window of reference bases either side of indel to look for other indels. Default 3
--strelka   Optional. Strelka file type
--pindel    Optional. Pindel file type. Is default.
--gatk      Optional. GATK file type.
~~~~


## Examples

### Somatic and Germline input files

~~~~{.text}
qbasepileup -t 2 -m indel -r reference.fa --pindel \
    --it tumour.bam --in normal.bam \
    --is somatic.input.dcc1 --ig germline.input.dcc1 \
    --os somatic.output.dcc1 --og germline.output.dcc1 \
    --log basepileup.log
~~~~

* Pindel file
* Default soft clip window (13)
* Default nearby indel window (3)
* default homopolymer length (10)

### Somatic input file

~~~~{.text}
qbasepileup -t 2 -m indel --it tumour.bam --in normal.bam \
    --is somatic.input.dcc1 --os somatic.output.dcc1 
    --log basepileup.log -r reference.fa --gatk
~~~~

* GATK file
* Default soft clip window (13)
* Default nearby indel window (3)
* default homopolymer length (10)

### Germline input file

~~~~{.text}
qbasepileup -t 2 -m indel --it tumour.bam --in normal.bam \
    --ig germline.input.dcc1 --og germline.output.dcc1
    --log basepileup.log -r reference.fa --pindel
~~~~

* Pindel file
* Default soft clip window (13)
* Default nearby indel window (3)
* default homopolymer length (10)
