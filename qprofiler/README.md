### qprofiler
qprofiler collects summary statistics from a number of NGS related files including BAM and VCF.

## Requirements

* Java 1.8

## Building qprofiler

* **To do a build of qprofiler, first clone the adamajava repository using "git clone":**
  ```
  git clone https://github.com/AdamaJava/adamajava
  ```

  Then move into the adamajava folder:
  ```
  cd adamajava
  ```
  Run gradle to build qprofiler and its dependent jar files:
  ```
  ./gradlew :qprofiler:build
  ```
  This creates the qprofiler jar file along with dependent jars in the `qprofiler/build/flat` folder

## Running qprofiler

* To run qprofiler, you need to supply an output, a log file and one or more inputs:
  ```
  usage: java -jar qprofiler.jar --input <input file>  --output <output xml file> --log <log file> [options]
  ```
