# qsv (V2)

Here, the individual tool version are removed, an universal version is applied to the adamajava package. To retrive version information just by applying "--version" option to qsv.jar. The function and description of qsv remains same as the [qsv (V1)](qsv.archived.7.md).  

## Installation

qsv requires java 8, a machine with 8 cores (hyperthreaded) and at least 40GB of RAM.

* To do a build of qsv, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qsv and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qsv:build
  ~~~~
  This creates the qsv jar file along with dependent jars in the `qsv/build/flat` folder

### Usage

 * qsv requires 2 arguments in order to run:
   * a config (.ini) file containing details of the parameters to be used
   * a path to a directory where temporary files will be written

~~~~{.text}
  usage: java -jar qsv.jar --ini <ini_file> --tmp <directory> [OPTIONS]

  Option              Description
  ------              -----------
  -V -v --version     Print version info.
  -h --help           Shows this help message.
  --ini <ini>         ini file with required options
  --overrideOutput [overrideOutput]  name of directory to write qSV output
  --tmp <tmp>         directory to write temporary files to.
  --range [range]     Chromosome to run. 
  --log               Name of log file. Will be written to the output directory.              
  --loglevel          Logging level required, e.g. INFO, DEBUG.                             
~~~~

