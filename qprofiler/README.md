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
  Run gradle to build qprofiler and its dependant jar files:
  ```
  ./gradlew :qprofiler:build
  ```
  This creates the qprofiler jar file along with dependant jars in the `qprofiler/build/flat` folder

## Running qprofiler

* To run qprofiler, you need to supply an input, an output and a log file:
  ```
  java -jar qprofiler/build/flat/qprofiler-1.0.jar -i <input.file> -o <output.xml> -l <logfile.log>
  ```
  Output is written to the supplied `output.xml` file
