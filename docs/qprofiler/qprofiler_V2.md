# qprofiler (Latest)

`qprofiler` provides quality control reporting for next-generation sequencing(NGS). `qprofiler` takes FASTQ, BAM/SAM, FASTA, QUAL,GFF3, MA and VCF files as input and outputs an XML file containing summary statistics tailored to the input file type. If no output file is specified then the default output file name will be used: `qprofiler.xml`.

While the XML file is useful for extracting values for further analysis, a visual representation of the data is more useful in most cases so another tool qvisualise was created to parse the qprofiler XML files and produce HTML output with embedded graphs via the Charts javascript library developed by Google. qvisualise exists as a standalone program but it is also integrated into qprofiler so a HTML file will always be output by qprofiler unless the --nohtml tag is used. The HTML file name is based on the XML output filename with the extension .html appended.

## Installation
qprofiler requires java 8, multi-core machine (ideally) and 5GB of RAM

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

~~~~{.text}
usage: java -jar qprofiler.jar --input <input file> --output <output xml file> --log <log file> [options]
Option                              Description                                               
------                              -----------                                               
Option                        Description                                               
------                        -----------                                               
-h, --help                    Show usage and help.                                      
--include                     Deprecated, Visualisation for SOLiD platform.             
--index                       Opt, A bai index file. Def=null.                          
--input                       Req, Input file in FASTQ, BAM/SAM or VCF format, also     
                                support FA, MA, QUAL, FASTA, GFF and GFF3 format.       
--log                         Req, Log file.                                            
--loglevel                    Opt, Logging level [INFO,DEBUG], Def=INFO.                
--nohtml                      Opt, No html output will be generated if this option is   
                                set.                                                    
--output                      Opt, XML output which containing basic summary statistics.
                                Def="qprofiler.xml".                                    
--records <Integer>           Opt, Only process the specified number of records from the
                                beginning of the BAM file                               
--tags                        Opt, Specify user defined tags in SAM/BAM file, which     
                                support printable string type, eg.["ZC", "XY"].         
--tagsChar                    Opt, Specify user defined tags in SAM/BAM file,  which    
                                support Printable character type, eg.["ZC", "XY"].      
--tagsInt                     Opt, Specify user defined tags in SAM/BAM file,  which    
                                support Signed integer type, eg.["ZC", "XY"].           
--threads-consumer <Integer>  Opt, Number of threads to process inputed BAM records.    
                                Def=0 (run in single thread mode).                      
--threads-producer <Integer>  Opt, Number of threads to read the indexed BAM file. This 
                                option will ignored without positive value of "threads- 
                                consumer".  Def=1.                                      
--validation                  Opt, BAM record validation stringency [STRICT,LENIENT,    
                                SILENT]. Def=LENIENT.                                   
--version                     Show version number.

~~~~


