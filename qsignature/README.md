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
