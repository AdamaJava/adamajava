### qsignature
is a simple and highly effective system for detecting potential
sample mix-ups using distance measurements between SNV alleles that are
common to pairs of samples of interest

## Requirements
* Java 1.8

## Building qsignature

* **To do a build of qsignature, first clone the adamajava repository using "git clone":**
  ```
  git clone https://github.com/AdamaJava/adamajava
  ```

  Then move into the adamajava folder:
  ```
  cd adamajava
  ```
  Run gradle to build qsignature and its dependent jar files:
  ```
  ./gradlew :qsignature:build
  ```
  This creates the qsignature jar file along with dependent jars in the `qsignature/build/flat` folder

## Running qsignature

* **To run qsignature in the mode that generates the qsignature vcf file:**
  ```
  java -cp qsignature.jar org.qcmg.sig.SignatureGeneratorBespoke -snpPositions <file containing positions of interest> -input <input BAM file> -output <output directory where qsig.vcf files will be generated> -log <log file>
  ```
  Output is written to the directory specified by the ouptut option
  
* **To run qsignature in the mode that compares the qsignature vcf file:**
  ```
  java -cp qsignature.jar org.qcmg.sig.Compare -log <log file> -dir <directory in which to search for .qsig.vcf files> -output <output xml file>
  ```
  
  Output is written to the directory specified by the ouptut option.

### Comparing files on shared storage

Searching a large project on Lustre can take substantial time because recursive
discovery reads metadata for every entry, including unrelated files. `Compare`
uses `-noOfThreads` for directory discovery as well as loading and comparison:

```sh
java -cp qsignature.jar org.qcmg.sig.Compare -log compare.log \
  -dir /path/to/project -noOfThreads 8 -output comparisons.xml
```

Independent directories are scanned concurrently; symbolic links are followed
with ancestor-cycle detection. The default is one thread. Parallel discovery
overlaps metadata requests, but still visits the tree and offers little benefit
when most entries are in a single directory. Logs include discovery time per root.
Using a narrower signature directory also reduces the work.

To bypass recursive discovery, supply a file list from the generating pipeline:

```sh
java -cp qsignature.jar org.qcmg.sig.Compare -log compare.log \
  --input-list signatures.txt -noOfThreads 8 -output comparisons.xml
```

The UTF-8 list contains one `.qsig.vcf` or `.qsig.vcf.gz` path per line. Blank
lines are ignored; relative paths are resolved against the list's directory.
Listed files must exist and be readable. Only listed files are selected, so
refresh the list when project inputs change. A list can also be generated once
and reused for subsequent comparisons:

```sh
find -L /path/to/project \( -name '*.qsig.vcf' -o -name '*.qsig.vcf.gz' \) \
  -type f -print > signatures.txt
```

Generating the list still requires an initial scan. Use absolute project paths
in that command so the generated paths can be read from any list location.
Individual files can also be supplied with repeated `-input` arguments.
Directories, lists, and individual files can be combined; duplicate paths are
removed and the usual exclusion and search-string filters still apply.
