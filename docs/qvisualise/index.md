# qvisualise

`qvisualise` creates HTML reprts from qprofiler XML reports. Only
XML reports from qprofiler, not qprofiler2, are currently supported.

## Installation

qvisualise requires java 21.

* To do a build of qvisualise, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qvisualise and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qvisualise:build
  ~~~~
  This creates the qvisualise jar file along with dependent jars in the `qvisualise/build/flat` folder

## Usage

~~~~{.text}
java -jar qvisualise.jar --input <qprofiler_XML> --output <qvisualise_HTML> [options]
~~~~

## Options

~~~~{.text}
Option      Description
------      -----------
--help      Shows this help message.
--version   Print version info.

--input     qprofiler XML file
--output    HTML report file
--log       Log file
--loglevel  Log level [INFO,DEBUG], Def=INFO
~~~~
