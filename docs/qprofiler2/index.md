# qprofiler2

`qprofiler2` is a standalone Java application that produces summary metrics for common file types used in next-generation sequencing. It can process BAM, FASTQ, VCF files and the output in all cases is an XML file containing basic summary statistics. It is a newer version of qprofiler but with many more features including the VCF mode and a vastly expanded BAM mode.


## Installation

qprofiler2 requires java 8 and Multi-core machine (ideally) and little of RAM

* To do a build of qprofiler2, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qsv and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qprofiler2:build
  ~~~~
  This creates the qprofiler2 jar file along with dependent jars in the `qprofiler2/build/flat` folder

## Usage

~~~~{.text}
java -jar qprofiler2.jar -h
usage: java -jar qprofiler2.jar --input <input file> --output <output xml file> --log <log file> [options]

Option                        Description                                                               
------                        -----------                                                               
--bam-full-header             Opt, this option indicate qprofiler2 output entire BAM header ; otherwise 
                                only output HD and SQ.                                                  
--bam-index                   Opt, a BAI index file. The option of "threads-producer" will be ignored   
                                if no index file provided.                                              
--bam-records <Integer>       Opt, only process the specified number of records from the beginning of   
                                the BAM file. Default to process entire BAM file.                       
--bam-validation              Opt, BAM record validation stringency [STRICT,LENIENT,SILENT].            
                                Def=LENIENT.                                                            
--help                        Show usage and help.                                                      
--input                       Req, input file in FASTQ, BAM/SAM or VCF format, also support FA, MA,     
                                QUAL, FASTA, GFF and GFF3 format.                                       
--log                         Req, Log file.                                                            
--loglevel                    Opt, Logging level [INFO,DEBUG], Def=INFO.                                
--output                      Opt, XML output which containing basic summary statistics. Def="qprofiler.
                                xml".                                                                   
--threads-consumer <Integer>  Opt, number of threads to process inputed records, BAM and FASTQ mode     
                                only. Def=0 (not multi-thread mode).                                    
--threads-producer <Integer>  Opt, number of threads to read the indexed BAM file, BAM mode only. This  
                                option will be ignored without "threads-consumer" option.  Def=1.       
--vcf-format-field            Opt, group VCF records according to user specified format fields. eg.     
                                "GD", "CCM","FT=PASS". Default to ignore VCF format field.              
--version                     Show version number.     
~~~~


### multi thread
When running multi-threaded, we suggest more consumer than producer threads with a recommended ratio of 6:1. However, it is up to your machine system. for example, only one thread to read the input file but 12 threads are specified to process reads.

Please specify the BAM index file if multiple producer threads are going to be used, eg. 

~~~~{.text}
java -jar qprofiler2.jar -ntC 12 -ntP 2 --index  $somedir/${bam}.bai --input  $somedir/$bam --output $somedir/${bam}.qp2.xml --log $somedir/${bam}.qp2.log
~~~~

## Output

### Xml Validation

qprofiler2 provide a schema file which help you to validate the xml output. This xsd file is published on github repository: https://github.com/AdamaJava/adamajava/blob/master/qprofiler2/src/org/qcmg/qprofiler2/qprofiler2.xsd

~~~~{.text}
xmllint --noout --schema ~/PATH/Schema.xsd file.xml
or
java -jar xsd11-validator.jar -sf my.xsd -if my.xml
~~~~


Xmllint does not validate xsd 1.1. But you can try https://www.dropbox.com/s/939jv39ihnluem0/xsd11-validator.jar



### BAM Mode

~~~~{.text}
<qProfiler finishTime="2019-05-22 16:31:47" operatingSystem="Linux" startTime="2019-05-22 12:16:12" user="me" validationSchema="qprofiler_2_0.xsd" version="2.0 (b3a23f83)">
 <bamReport file="/myDir/tumour.normal.2rg.bam" finishTime="2019-05-22 16:31:41"  md5sum="D98A32C19DF282228E7BC61DC8543FEC" startTime="2019-05-22 12:16:13" uuid="babbb684-38bd-43d5-ba24-281b38d5e662">
   <bamHeader>
     <headerRecords TAG="HD" description="The header line">...</headerRecords>
     <headerRecords TAG="SQ" description="Reference sequence dictionary">...</headerRecords>
   </bamHeader>
   <bamSummary>
     <readGroups>
        <readGroup name="8e523d07-e989-4fdc-900a-d5b9e857bbf7">...</readGroup>
        <readGroup name="87b8c254-7fa2-43d5-9463-f07c13378502">...</readGroup>
        <readGroup name="b7e7c4c1-3a2e-46a7-9377-691c016517b6">...</readGroup>
     </readGroups>
     <sequenceMetrics name="Overall">...</sequenceMetrics>
     <sequenceMetrics name="OverallBaseLost">...</sequenceMetrics>
   </bamSummary>
   <bamMetrics>
     <QNAME>...</QNAME>
     <FLAG>...</FLAG>
     <RNAME>...</RNAME>
     <POS>...</POS>
     <MAPQ>...</MAPQ>
     <CIGAR>...</CIGAR>
     <TLEN>...</TLEN>
     <SEQ>...</SEQ>
     <QUAL>...</QUAL>
     <TAG>...</TAG>
   </bamMetrics>
  </bamReport>
</qProfiler>
~~~~
