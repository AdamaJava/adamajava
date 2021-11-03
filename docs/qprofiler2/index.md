# qprofiler2

`qprofiler2` is a standalone Java application that produces summary metrics for common file types used in next-generation sequencing. It can process BAM, FASTQ, VCF files and the output in all cases is an XML file containing basic summary statistics. It is a newer version of qprofiler but with many more features including the VCF mode and a vastly expanded BAM mode.


## Installation

qprofiler2 requires java 8 and Multi-core machine (ideally) and 5G of RAM

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
java -jar qprofiler2.jar --threads-consumer 12 -threads-producer 2 --bam-index  $somedir/${bam}.bai --input  $somedir/$bam --output $somedir/${bam}.qp2.xml --log $somedir/${bam}.qp2.log
~~~~

## Output

### Xml Validation

qprofiler2 provide a schema file which help you to validate the xml output. This xsd file can download from https://purl.org/adamajava/xsd/qprofiler2/v2/qprofiler2.xsd

~~~~{.text}
xmllint --noout --schema ~/PATH/qprofier2.xsd qprofiler2.xml
or
java -jar xsd11-validator.jar -sf ~/PATH/qprofier2.xsd  -if qprofiler2.xml
~~~~

### FASTQ Mode output
it output three main section information: read name analysis, sequence analysis and base quality analysis. An example as below:

~~~~{.text}
<qProfiler finish_time=2021-09-10 16:59:32 run_by_os=Linux run_by_user=christiX start_time=2021-09-10 16:51:25 version=78-43982c9>
  <fastqReport execution_finished=2021-09-10 16:59:32 execution_started=2021-09-10 16:51:25 file=/mnt/Illumina_gdc_realn_1.fastq.gz records_parsed=86,324,947>
   <ReadNameAnalysis>
     <INSTRUMENTS>...</INSTRUMENTS>
     <RUN_IDS>...</RUN_IDS>
     <FLOW_CELL_IDS/>
     <FLOW_CELL_LANES>...</FLOW_CELL_LANES>
     <TILE_NUMBERS>...</TILE_NUMBERS>
     <PAIR_INFO>...</PAIR_INFO>
     <FILTER_INFO>...</FILTER_INFO>
     <INDEXES/>
     <QUAL_HEADERS>...</QUAL_HEADERS>
   </ReadNameAnalysis>
   <SEQ>
     <BaseByCycle>...</BaseByCycle>
     <LengthTally>...</LengthTally>
     <BadBasesInReads>...</BadBasesInReads>
     <mers6>...</mers6>
     <mers1>...</mers1>
     <mers2>...</mers2>
     <mers3>...</mers3>
   </SEQ>
   <QUAL>
     <QualityByCycle>...</QualityByCycle>
     <LengthTally>...</LengthTally>
     <BadQualsInReads>
     <ValueTally>...</ValueTally>
     </BadQualsInReads>
   </QUAL>
</fastqReport>
</qProfiler>

~~~~

### BAM Mode output
BAM record contains both fastq record information and algnment information. qProfiler2 outputs based on BAM record element order, but also output BAM header and summary information. An example as below:

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

### BAM summary output
The <bamSummary> section contains three top level information: read group summary, overall summary and base lost summary. It provide the read counts for bases, reads, pairs, trims, clips, overlaps etc. A example of screen shot for the <bamSummary> is below.

~~~~{.text}
<bamSummary>
 <readGroups>
  <readGroup name=69f81d0d-c430-4a6f-9ccd-05ea88b22c1d>...</readGroup>
  <readGroup name=cd90dd75-8a1f-4fd0-a352-0364d8dd5300>
   <sequenceMetrics name=basesLost>
    <variableGroup name=duplicateReads>...</variableGroup>
    <variableGroup name=unmappedReads>...</variableGroup>
    <variableGroup name=notProperPairs>
     <value name=readCount>134461</value>
     <value name=basesLostCount>20303611</value>
     <value name=basesLostPercent>4.34</value>
    </variableGroup>
    <variableGroup name=trimmedBases>...</variableGroup>
    <variableGroup name=softClippedBases>...</variableGroup>
    <variableGroup name=hardClippedBases>...</variableGroup>
    <variableGroup name=overlappedBases>...</variableGroup>
   </sequenceMetrics>
   <sequenceMetrics name=reads readCount=3158317>...</sequenceMetrics>
   <sequenceMetrics name=properPairs pairCount=1255372>...</sequenceMetrics>
   <sequenceMetrics name=notProperPairs pairCount=68377>
    <variableGroup name=F3F5>...</variableGroup>
    <variableGroup name=F5F3>...</variableGroup>
    <variableGroup name=Inward>...</variableGroup>
    <variableGroup name=Outward>
     <value name=firstOfPairs>6230</value>
     <value name=secondOfPairs>6230</value>
     <value name=mateUnmappedPair>0</value>
     <value name=mateDifferentReferencePair>0</value>
     <value name=tlenZeroPairs>1</value>
     <value name=tlenUnder1500Pairs>1058</value>
     <value name=tlenOver10000Pairs>3313</value>
     <value name=tlenBetween1500And10000Pairs>1858</value>
     <value name=overlappedPairs>0</value>
     <value name=pairCountUnderTlen5000>2101</value>
    </variableGroup>
    <variableGroup name=Others>...</variableGroup>
   </sequenceMetrics>
  </readGroup>
 </readGroups>
 <sequenceMetrics name=Overall>
     <value name=Number of cycles with greater than 1% mismatches>103</value>
     <value name=Average length of first-of-pair reads>150</value>
     <value name=Average length of second-of-pair reads>150</value>
     <value name=Discarded reads (FailedVendorQuality, secondary, supplementary)>807377</value>
     <value name=Total reads including discarded reads>39966613</value>
 </sequenceMetrics>
 <sequenceMetrics name=OverallBasesLost>
     <value name=readCount>39159236</value>
     <value name=basesCount>5913044636</value>
     <value name=basesLostCount_basesLostPercent>20.00</value>
     <value name=duplicateReads_basesLostPercent>13.23</value>
     <value name=unmappedReads_basesLostPercent>0.19</value>
     <value name=notProperPairs_basesLostPercent>4.45</value>
     <value name=trimmedBases_basesLostPercent>0.13</value>
     <value name=softClippedBases_basesLostPercent>1.11</value>
     <value name=hardClippedBases_basesLostPercent>0.00</value>
     <value name=overlappedBases_basesLostPercent>0.89</value>
 </sequenceMetrics>
</bamSummary>
~~~~
