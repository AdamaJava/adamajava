# qbamfilter

## Introduction

`qbamfilter` select reads from BAM files based on a user-supplied query.
qbamfilter is available as a standalone application and is incorporated 
into the majority of AdamaJava tools as a library to provide filtering 
of BAM records.  For the standalone application, reads that match the query 
are written to a new BAM file and reads that do not are dropped or 
optionally written to a different BAM file.  For the library use-case, 
only BAM records that pass the query string are accepted for further 
processing by the AdamaJava tool.
There is a separate page with a more detailed explanation of the
[qbamfilter query language](qbamfilter_query).

## Installation

qbamfilter requires java 21 and (ideally) a multi-core machine, although 
it operates in single-threaded mode by default.  You can tune the amount
of memory used by qbamfilter by specifying the number of records to store 
in memory (`--maxRecordNumber`).  You can also opt to sort the output BAM 
and the BAM will be automatically indexed if the sort-by-coordinate option
is specified.

* **To do a build of qbamfilter, first clone the adamajava repository using "git clone":**
  ```
  git clone https://github.com/AdamaJava/adamajava
  ```
  
  Then move into the adamajava folder:
  ```
  cd adamajava
  ```
  Run gradle to build qbamfilter and its dependent jar files:
  ```
  ./gradlew :qbamfilter:build
  ```
  This creates the qbamfilter jar file along with dependent jars in the `qbamfilter/build/flat` folder
 

## Usage

~~~~{.text}
java -jar qbamfilter.jar -q "<query>" -i <input> -o <output> --log <logfile> [options]
~~~~

### `example`
~~~~{.text}
java -jar qbamfilter.jar -q "or( MAPQ > 50, option_ZM == 1 )" -i /path/input.bam -o /path/output.bam --log /path/output.log -t 3
~~~~

## Options

~~~~{.text}
--help, -h              Show help message.
--version, -v           Print version.

--input, -i             Req, Input BAM file.
--output, -o            Req, Output BAM file.
--query, -q             Req, Query string.
--log                   Req, Log file.

--loglevel              Opt, Logging level [INFO,DEBUG], Def=INFO.
--filterOut, -f         Opt, BAM file for records that failed --query.
--maxRecordNumber, -m   Opt, BAM record queue size in 1000's, Def=100.
--sort                  Opt, Sort order [queryname,coordinate,unsorted], Def=unsorted.
--threadNumber, -t      Opt, Filtering thread count, Def=1.
--tmpdir                Opt, Location of temporary BAM files.
--validation            Opt, BAM record validation stringency [STRICT,LENIENT,SILENT] Def=LENIENT.
~~~~


### `--input`

The name of the SAM or BAM file to be filtered.

### `--output`

The name of the BAM file where records that match `--query` will be
written.

### `--maxRecordNumber`

BAM record queue size during reading and writing. The unit is 1000's
or records so `--maxRecordNumber=100` allows for a queue of 100,000 records.

### `--tmpdir`

During processing, temporary BAM files will be created. This behaviour
is a consequence of using the `picard` libraryy.

### `--query`

This string defines the criteria to be used to sort the BAM records 
into matching and not matching with the matching records written to
`--output` and (optionally) the non-matching records written to
`--filterOut`.

The general form of a query string is:

~~~~{.text}
operator( condition [, condition|query]* )
~~~~

i.e., it lists one or more conditions and zero or more queries joined by 
operators. Currently there are only two operators available - `and()` and 
`or()`.  A more complicated example is shown here (formatted for
readability):

~~~~{.text}
and( Cigar_M > 35,
     RNAME =~ chr*,
     or( MAPQ > 50, option_ZM == 1 ),
     Flag_DuplicateRead == false )
~~~~

This query string shows an `and()` operator with 3 conditions and a
query using the `or()` operator with 2 conditions.
This query has the effect of matching BAM records where all of these
conditions are met:

 1. there are more than 35 bases with an "M" CIGAR designation `and`
 2. the name of the sequence that the read aligned against starts with
 the string 'chr' `and`
 3. either the mapping quality is greater than 50 `or` the ZM option is
 set to 1 `and`
 4. the read is not a duplicate according to the FLAG field.

It is important to remember that the query must evaluate to 'true" for the 
read to be passed by qbamfilter and be written to the `--output` BAM file.

There is a separate page with a more detailed explanation of the
[qbamfilter query language](qbamfilter_query).


