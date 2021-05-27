### qsv
Tool that detects structural variants from matched tumour/normal BAM files

## Requirements
* Java 1.8

## Building qsv

* **To do a build of qsv, first clone the adamajava repository using "git clone":**
  ```
  git clone https://github.com/AdamaJava/adamajava
  ```

  Then move into the adamajava folder:
  ```
  cd adamajava
  ```
  Run gradle to build qsv and its dependent jar files:
  ```
  ./gradlew :qsv:build
  ```
  This creates the qsv jar file along with dependent jars in the `qsv/build/flat` folder

## Running qsv

* **To run qsv, you need to supply an ini file:**
  ```
  java -jar qsv.jar --ini <ini_file> --output-temporary <directory> [OPTIONS]
  ```
  Output is written to the directory specified in the ini file.
