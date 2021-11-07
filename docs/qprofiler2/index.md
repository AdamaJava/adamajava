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
FASTQ mode outputs three sections information: read name analysis, sequence analysis and base quality analysis. An example as below:

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
  <readGroup name="69f81d0d-c430-4a6f-9ccd-05ea88b22c1d">...</readGroup>
  <readGroup name="cd90dd75-8a1f-4fd0-a352-0364d8dd5300">
   <sequenceMetrics name="basesLost">...</sequenceMetrics>
   <sequenceMetrics name="reads" readCount="3158317">
    <variableGroup name="discardedReads">
     <value name="supplementaryAlignmentCount">62243</value>
     <value name="secondaryAlignmentCount">0</value>
     <value name="failedVendorQualityCount">0</value>
    </variableGroup>
    <variableGroup name="readLength">
     <value name="readCount">3096074</value>
     <value name="max">151</value>
     <value name="mean">150</value>
     <value name="mode">151</value>
     <value name="median">151</value>
    </variableGroup>
    <variableGroup name="tLen">...</variableGroup>
    <variableGroup name="countedReads">...</variableGroup>
   </sequenceMetrics>
   <sequenceMetrics name="properPairs" pairCount="1255372">...</sequenceMetrics>
   <sequenceMetrics name="notProperPairs" pairCount="68377">...</sequenceMetrics>
  </readGroup>
 </readGroups>
 <sequenceMetrics name="Overall">
    <value name="Number of cycles with greater than 1% mismatches">103</value>
    <value name="Average length of first-of-pair reads">150</value>
    <value name="Average length of second-of-pair reads">150</value>
    <value name="Discarded reads (FailedVendorQuality, secondary, supplementary)">807377</value>
    <value name="Total reads including discarded reads">39966613</value>
 </sequenceMetrics>
 <sequenceMetrics name="OverallBasesLost">
    <value name="readCount">39159236</value>
    <value name="basesCount">5913044636</value>
    <value name="basesLostCount_basesLostPercent">20.00</value>
    <value name="duplicateReads_basesLostPercent">13.23</value>
    <value name="unmappedReads_basesLostPercent">0.19</value>
    <value name="notProperPairs_basesLostPercent">4.45</value>
    <value name="trimmedBases_basesLostPercent">0.13</value>
    <value name="softClippedBases_basesLostPercent">1.11</value>
    <value name="hardClippedBases_basesLostPercent">0.00</value>
    <value name="overlappedBases_basesLostPercent">0.89</value>
 </sequenceMetrics>
</bamSummary>
~~~~

#### \<sequenceMetrics name="basesLost">
There are four <sequenceMetrics> under each <bamSummary>/<readGroups>/<readGroup>, one of them named "basesLost". Due to sample, sequence or mapping tool limits, there are alway some base information is not usable, we call them base lost. The qprofiler2 walk through whole BAM file, summarize all losted base in this section. 

~~~~{.text}
<readGroup name="cd90dd75-8a1f-4fd0-a352-0364d8dd5300">
 <sequenceMetrics name="basesLost">
  <variableGroup name="duplicateReads">
    <value name="readCount">445721</value>
    <value name="basesLostCount">67303871</value>
    <value name="basesLostPercent">14.40</value>
  </variableGroup>
  <variableGroup name="unmappedReads">...</variableGroup>
  <variableGroup name="notProperPairs">...</variableGroup>
  <variableGroup name="trimmedBases">...</variableGroup>
  <variableGroup name="softClippedBases">...</variableGroup>
  <variableGroup name="hardClippedBases">...</variableGroup>   
  <variableGroup name="overlappedBases">...</variableGroup>
 </sequenceMetrics>
 ...
</readGroup>
~~~~

It has 7 children elements, the description as below. 

 variableGroup element   | value element | Description 
---------------- | ---------------------- | ----------------
name="duplicateReads" | \<value name="readCount"> |  reads marked as duplicated but excluds discarded reads (failed, secondary and supplementary). 
  |\<value name="basesLostCount"> |     readCount *  readLength.max
name="unmappedReads"  | \<value name="readCount"> |  reads marked as unmapped but excluds duplicate and  discarded reads  
  | \<value name="basesLostCount"> |     readCount * readLength.max
name="notProperPairs" | \<value name="readCount"> |  reads marked as notProperPair but excluds unmapped, duplicate and  discarded reads. see further detils on [[qprofiler2/bamSummary#notPorperPair | notProperPaireadLength.max] 
   |       \<value name="basesLostCount"> |     readCount *  readLength.max
name="trimmedBases"   | \<value name="readCount"> |  reads with short base length, that is readlenght + hardclip  \< max read length; but excludes notProperPair, unmapped, duplicated and discarded reads 
   | \<value name="basesLostCount"> |     Cumulative count of the total differences between Max Read Length and individual read lengths (calculated as per column Average Read Length). This should capture any read trimming that happened before the FASTQ file was passed through an aligner 
name="softClippedBases"       | \<value name="readCount"> |  reads contains soft clipped base but excluds notProperPair, unmapped, duplicated and discarded reads 
  | \<value name="basesLostCount"> |     sum of soft clipped base of each read 
name="hardClippedBases"       | \<value name="readCount"> |  reads contains hard clipped base but excluds notProperPair, unmapped, duplicated and discarded reads 
  | \<value name="basesLostCount"> |     Bases that are hard clipped based on analysis of the CIGAR string. 
name="overlappedBases"        | \<value name="readCount"> |  overlapped reads with positive tLen or firstOfpair with zero tLen; but excluds notProperPair, unmapped, duplicated and discarded reads 
  | \<value name="basesLostCount"> |     Excludes all bases covered by hard and soft clips and only counts overlapped bases on one strand. So if a 2 x 100bp read pair overlapped by 50 bases, the assumption is that the sequenced fragment was 150 bases so 50 of the 200 sequenced bases were wasted because they were sequenced on both reads. 
  all above elements    | \<value name="basesLostPercent"> |     this. basesLostCount / OverallBasesLost.basesCount


Here the value of "readLength.max" used on above tabel, is the value of attribute "max" from "sequenceMetrics" named "reads". In this example, the value is 151. 
~~~~{.text}
 <sequenceMetrics name="reads" readCount="3158317">
    <variableGroup name="readLength">
     <value name="max">151</value>
~~~~

#### \<sequenceMetrics name="properPairs/notProperPairs" pairCount="...">
The sequence metircs named "properPairs" or "notProperPairssection" lists statistic data of pairs count for each readGroup. Here, the The attribute "pairCount" is the sum of all reads marked as "firstOfpair" and "notProperPair"/"properPairs" but excludes unmapped, duplicate and discarded reads. Both metrics lists possible pair type: "F3F5", "F5F3", "Inward", "Outward" and "Others". 

~~~~{.text}
<sequenceMetrics name="properPairs" pairCount="...">...</sequenceMetrics>
<sequenceMetrics name="notProperPairs" pairCount="...">
 <variableGroup name="F3F5">
  <value name="firstOfPairs">...</value>
  <value name="secondOfPairs">...</value>
  <value name="mateUnmappedPair">...</value>
  <value name="mateDifferentReferencePair">...</value>
  <value name="tlenZeroPairs">...</value>
  <value name="tlenUnder1500Pairs">...</value>
  <value name="tlenOver10000Pairs">...</value>
  <value name="tlenBetween1500And10000Pairs">...</value>
  <value name="overlappedPairs">...</value>
  <value name="pairCountUnderTlen5000">...</value>
 </variableGroup>
 <variableGroup name="F5F3">...</variableGroup>
 <variableGroup name="Inward">...</variableGroup>
 <variableGroup name="Outward">...</variableGroup>
 <variableGroup name="Others">...</variableGroup>
</sequenceMetrics>
~~~~

Each pair type contains below 9 children elements.

  value element | Description
---------------------- | ----------------
\<value name="firstOfPairs"> | all reads marked as first of pair and belong variableGroup and sequenceMetrics specified category
\<value name="secondOfPairs"> | all reads marked as second of pair and belong variableGroup and sequenceMetrics specified category
\<value name="mateUnmappedPair"> | read is mapped, but mate belongs to unmappedReads
\<value name="mateDifferentReferencePair"> | read and mate mapped to different reference, here we only count the firstOfPair
\<value name="overlappedPairs"> | overlapped reads with tLen >= 0, excludes reads with zero tLen value but not firstOf Pair
\<value name="tlenUnder1500Pairs"> | reads with tLen < 1500 and tLen > 0; exclues overlapped reads
\<value name="tlenOver10000Pairs"> | read with tLen > 10000
\<value name="tlenBetween1500And10000Pairs"> | read with tLen > 1500 and tLen < 10000
\<value name="pairCountUnderTlen5000"> | reads with positive tLen and tLen < 5000; or firsrOfPair with zero tLen:wq

#### \<sequenceMetrics name="OverallBasesLost" readCount="...">
In above bam summary output example, this metrics lists the summed counts from all read groups. The attribute "readCount" in the top element stores the count of total inputted reads including discarded reads. It has 10 children elements, the description as below:

  children element | Description 
------------------------ | ---------------------------
\<value name="readCount">  | total reads but excludeing discarded reads  
\<value name="basesCount"> |the sum of baseCount from all read group 
\<value name="basesLostCount_basesLostPercent"> | (the sum of basesLostCount from all read group)/ this.basesCount 
\<value name="basesLostCount_basesLost"> | (the sum of basesLostCount from all read group)/ this.basesCount 
\<value name="duplicateReads_basesLostPercent"> | (the sum of duplicateReads.basesLostCount from all read group)/ this.basesCount 
\<value name="unmappedReads_basesLostPercent"> | (the sum of unmappedReads.basesLostCount from all read group)/ this.basesCount 
\<value name="notProperPairs_basesLostPercent"> | (the sum of notProperPairs.basesLostCount from all read group)/ this.basesCount 
\<value name="trimmedBases_basesLostPercent"> | (the sum of trimmedBases.basesLostCount from all read group)/ this.basesCount 
\<value name="softClippedBases_basesLostPercent" > | (the sum of softClippedBases.basesLostCount from all read group)/ this.basesCount 
\<value name="hardClippedBases_basesLostPercent" > | (the sum of hardClippedBases.basesLostCount from all read group)/ this.basesCount 
\<value name"=""overlappedBases_basesLostPercent"> | (the sum of overlappedBases.basesLostCount from all read group)/ this.basesCount 


### BAM metrics output
The summary of BAM record fileds is outputed to "bamMetrics" section. There are too much information, here we only lists "readCount" description under each "sequneceMetrics" node.

 parent node | sequenceMetrics node  | include discarded reads | include notPorpperPair | include unmapped | include duplicated | <div style="width:290px"> readCount descritpion </div> 
 ---------- | ---------- | ----------- | ----------- | ------------ | ----------- | ----------------
\<QNAME>\<readGroups> |  \<sequenceMetrics name="qnameInfo" <br> readCount="653091922">  |  no |  no |  no |  no | total reads but excludes notProperpair, unmapped, duplicate and  discarded reads
\<FLAG> |  \<sequenceMetrics readCount="822289947"> | yes | yes | yes | yes | Total reads including discarded reads
\<RNAME> |  \<sequenceMetrics readCount="653091922"> |  no |  no |  no |  no | total reads but  excludes notProperpair, unmapped, duplicate and  discarded reads
\<POS>\<readGroups> |  \<sequenceMetrics readCount="653091922"> |  no |  no |  no |  no | total reads but  excludes notProperpair, unmapped, duplicate and  discarded reads
\<MAPQ> |  \<sequenceMetrics readCount="822289947"> | yes | yes | yes | yes | Total reads including discarded reads
\<CIGAR>\<readGroups> |  \<sequenceMetrics readCount="814173022"> |  no | yes | yes | yes | reads including duplicateReads, nonCanonicalPairs and unmappedReads but excluding discardedReads (failed, secondary and supplementary).
\<TLEN>\<readGroups> |  \<sequenceMetrics name="tLenInNotProperPair" <br>pairCount="564214">  |   no | yes |  no |  no | not properPaired reads which 0 \< tLen \< 5000 or firstOfPair with tLen == 0,    excludes discarded, duplicate, unmapped and ProperPaired reads
\<TLEN>\<readGroups> |  \<sequenceMetrics name="overlapBaseInNotProperPair" <br>pairCount="22428"> |   no |  yes  |  no |  no | not properPaired, overlapped reads which 0 \< tLen \< 5000 or firstOfPair with tLen == 0,    excludes discarded, duplicate, unmapped and ProperPaired reads
\<TLEN>\<readGroups> |  \<sequenceMetrics name="tLenInProperPair" <br>pairCount="326545961"> |   no |  no |  no |  no | properPaired reads which 0 \< tLen \< 5000 or firstOfPair with tLen == 0,    excludes discarded, duplicate, unmapped and notProperPaired reads
\<TLEN>\<readGroups> |  \<sequenceMetrics name="overlapBaseInProperPair" <br>pairCount="293773743">  |  no |  no |  no |  no | properPaired, overlapped reads which 0 \< tLen \< 5000 or firstOfPair with tLen == 0,    excludes discarded, duplicate, unmapped and notProperPaired reads
\<SEQ> |  \<sequenceMetrics name="seqBase" readCount="653091922"> \<sequenceMetrics name="seqLength" readCount="653091922"> \<sequenceMetrics name="badBase" readCount="653091922"> \<sequenceMetrics name="2mers" readCount="653091922"> \<sequenceMetrics name="3mers" readCount="653091922"> \<sequenceMetrics name="6mers" readCount="653091922"> | no |  no |  no |  no | total reads but  excludes notProperpair, unmapped, duplicate and  discarded reads
\<QUAL> |  \<sequenceMetrics name="qualBase" readCount="653091922"> \<sequenceMetrics name="qualLength" readCount="653091922"> \<sequenceMetrics name="badBase" readCount="653091922"> |  no |  no |  no |  no | total reads but  excludes notProperpair, unmapped, duplicate and  discarded reads
\<TAG> |  \<sequenceMetrics name="tags:MD:Z" readCount="797657701"> \<sequenceMetrics name="tags:PG:Z" readCount="822289947"> \<sequenceMetrics name="tags:NM:i" readCount="797657701"> \<sequenceMetrics name="tags:RG:Z" readCount="822289947"> ... |  no | yes | yes | yes | Total reads contains specified tag excludes discarded reads

