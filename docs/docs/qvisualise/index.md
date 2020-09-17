# qvisualise

`qvisualise` creates HTML reprts from qprofiler XML reports. Only
XML reports from qprofiler, not qprofiler2, are currently supported.

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
