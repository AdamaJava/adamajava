# qcnv

`qcnv` is designed to act as a pre-processor for copy number analysis
tools. It reads one or more BAM files and outputs the read counts within
a given window-size to a VCF-like plain text file.  

## Installation

qcnv requires java 8 and (ideally) a multi-core machine. qcnv is threaded
for increased execution speed and is relatively memory efficient at 
approximately 2GB for a single thread although the memory use scales
linearly with thread count so using large numbers of threads requires
large amounts of memory. To install:

* Download the [qcnv tar file](https://sourceforge.net/projects/adamajava/files/qcnv-0.3.tar.bz2/download)
* Untar the tar file into a directory of your choice
 
You should see jar files for qcnv and its dependencies:

~~~~{.text}
> tar xjvf qcnv-0.3.tar.bz2
x antlr-3.2.jar
x commons-cli-1.2.jar
x htsjdk-1.140.jar
x jopt-simple-4.6.jar
x picard-lib.jar
x qbamfilter-1.2.jar
x qcnv-0.3pre.jar
x qcommon-0.3.jar
x qpicard-1.1.jar
x trove-3.1a1.jar
~~~~

## Usage

~~~~{.text}
java -jar qcnv.jar [options]
~~~~

## Options

~~~~{.text}
--help              Shows this help message.
--version           Print version.

--input, -i         Input BAM file with full path.
--id                Sample id, will be used as column name for output.
--output, -o        Output file.
--log               Log file.

--thread            Opt, Thread number, Def=2.
--window_size, -w   Opt, Window size, Def=10000.
--query, -q         Opt, Query string for selecting reads for cnv counts. 
~~~~

## Output Example

Multiple `-i` BAM files can be specified and each one can be followed
by an `--id` option to give the file a unique name in the output file.
This example shows 3 BAM files from a single cancer patient - tumour,
normal, metastasis - being processed through qcnv together.

Java is being told via `-Xms40g -Xmx40g` that the initial and maximum
heap size if 40 gigabytes. The window size is 10,000 bases (`-w
10000`), 16 threads can be used (`-thread 16`) and a
[qbamfilter](../qbamfilter/index.md) query string is being used to
only process reads that aligned against sequences wth names starting 
with 'chr' and tht have at least 100 bases with a CIGAR char of 'M
' indicating a match or mismatch (
`-q "and(RNAME =~ chr*, cigar_M >= 100)"`).

~~~~{.text}
 java -jar -Xms40g -Xmx40g  qcnv-0.3pre.jar -w 10000 --thread 16 \
   -i P1tumourBAM --id tumour \
   -i P1normalBAM --id normal \
   -i P1metastasisBAM --id metastasis \
   -o P1qcnv.txt --log p1qcnv.log \
   -q "and( RNAME =~ chr*, cigar_M >= 100 )"
~~~~

The output shows the number of reads in each window with columns for the
3 BAM files input with labels "tumour", "normal" and "metastasis" as per
the `--id` options.

~~~~{.text}
#CHROM  ID      START   END     FORMAT  tumour  normal  metastasis
chr1    1_10000 1       10000   DP      0       0       0
chr1    2_10000 10001   20000   DP      412     0       2
chr1    3_10000 20001   30000   DP      300     0       1
chr1    4_10000 30001   40000   DP      200     159     109
...
~~~~
