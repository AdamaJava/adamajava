# qmito

`qmito` produces mitochondrial sequencing summary reports from BAM files.

## Installation

qmito requires java 8 and Multi-core machine (ideally) and 5G of RAM

* To do a build of qmito, first clone the adamajava repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the adamajava folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build qmito and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qmito:build
  ~~~~
  This creates the qmito jar file along with dependent jars in the `qmito/build/flat` folder

## Usage 

* metic mode

~~~~{.text}
usage: java -cp qmito.jar au.edu.qimr.qmito.Metric --output <output> --input <bam file> --reference <reference file> --log <logfile> [options]
Option                        Description                                                            
------                        -----------                                                            
--help                        Show usage and help.                                                   
--input                       Req, a SAM/BAM file with full path                                     
--log                         Req, Log file with full path.                                          
--loglevel                    (Opt, Logging level [INFO,DEBUG], Def = INFO.                          
--lowread-count <Integer>     (Optional) Specify an integer here. It will report true if the coverage
                                on that base is lower than this integer.                             
--mito-name                   Opt, specify mitochondrial sequence name listed on reference file,     
                                Def=chrMT.                                                           
--nonref-threshold <Integer>  (Optional) Specify an integer here. It will report true if the non-    
                                reference ratio on that base is higher than this proportion .        
--output                      Req, a full path TSV format file                                       
--query                       Opt,A string with double quotation, following qbamfilter query rule.   
                                Default query is "and (flag_NotprimaryAlignment == false,            
                                flag_ReadUnmapped == false)"                                         
--reference                   Req, a reference file with fa format where input BAM files are mapped. 
--version                     Show version number.                        
~~~~

* stat mode:

~~~~{.text}
usage: java -cp qmito.jar au.edu.qimr.qmito.Stat --output <output> --input-control <control metric input> --input-test <test metric input> --log <logfile> [options]
Option           Description                                                           
------           -----------                                                           
--help           Show usage and help.                                                  
--input-control  Req, a metric file of control sample with tsv format, created by      
                   qmito/qpileup                                                       
--input-test     a metric file of test sample with tsv format, created by qmito/qpileup
--log            Req, Log file with full path.                                         
--loglevel       (Opt, Logging level [INFO,DEBUG], Def = INFO.                         
--output         Req, a full path TSV format file                                      
--version        Show version number.                
~~~~




## Modes 

### metric mode

* read in a single BAM
* use qbamfilter to only keep read pairs we want: `and( or(RNAME =='chrM', RNEXT == 'chrM'), flag_NotPrimaryAlignment==false, flag_ReadFailsVendorQuality==false, flag_SupplementaryRead)`
* note that DUPs and unmapped reads are OK which means the filter should pull out pairs where one or both reads mapped to the mitochondrial sequence
* write a report with the same params as Felicity's `qpileup` (approx 40 params) but we want to write it to a TSV file, not the HDF5 format used by `qpileup`.


Output Example

~~~~{.text}
java -Xmx5g -cp qmito.jar au.edu.qimr.qmito.Metric \
    --input $tumourBAM \
    --reference $refFile \
    --output ${tumourBAM}.qmito.tsv \
    --log ${tumourBAM}.qmito.log
~~~~

Output is a tab-separated plain-text file with unix line endings. The format is that each position in the mitochondrial genome is a row and 
the columns are the various parameters collected at each position.

~~~~{.text}
Reference       Position        Ref_base        A_for   C_for   G_for   T_for   N_for   Aqual_for       Cqual_for       Gqual_for       Tqual_for       Nqual_for       MapQual_for     ReferenceNo_for NonreferenceNo_for      HighNonreference_for    LowReadCount_for        StartAll_for    StartNondup_for StopAll_for     DupCount_for    MateUnmapped_for        CigarI_for      CigarD_for      CigarD_start_for        CigarS_for      CigarS_start_for        CigarH_for      CigarH_start_for        CigarN_for      CigarN_start_for        A_rev   C_rev   G_rev   T_rev   N_rev   Aqual_rev       Cqual_rev       Gqual_rev       Tqual_rev       Nqual_rev       MapQual_rev     ReferenceNo_rev NonreferenceNo_rev      HighNonreference_rev    LowReadCount_rev        StartAll_rev    StartNondup_rev StopAll_rev     DupCount_rev    MateUnmapped_rev        CigarI_rev      CigarD_rev      CigarD_start_rev        CigarS_rev      CigarS_start_rev        CigarH_rev      CigarH_start_rev        CigarN_rev      CigarN_start_rev
chrMT   1       G       0       0       14      0       0       0       0       533     0       0       542     14      0       0       0       14      14      0       0       0       0       0       0       0       0       0       0       0       0       0       0       2       0       0       0       0       73      0       0       76      2       0       0       1       2       2       0       0       0       0       0       0       1       0       0       0       0       0
...
~~~~


### stat mode

Metric mode only works on single BAMs so if you have tumour/normal pairs, or families or other collections of related BAM files that you may wish to analyse jointly, you should run qmito  metric independently on each of the BAMs and then use  stat mode to compare the `metric` files.

Stat mode reads 2 metric mode report files and does a chi-square of the A/C/G/T base counts of test vs control.  Forward and reverevse strand counts are done separately. 

Output Example: 
~~~~{.text}
java -Xmx5g -cp qmito.jar au.edu.qimr.qmito.Stat \
    --input-control $controlMetric \
    --input-test $testMetric \
    --output ${pair}.qmito.tsv \
    --log ${pair}.qmito.log
~~~~

~~~~{.text}
Reference       Position        Ref_base        con_A_for       con_C_for       con_G_for       con_T_for       con_A_rev       con_C_rev       con_G_rev       con_T_rev       con_chi_forVSrev        test_A_for      test_C_for      test_G_for      test_T_for      test_A_rev      test_C_rev      test_G_rev      test_T_rev      test_chi_forVSrev       total_chi_conVStest
chrMT   1       G       0       0       14      0       0       2       0       0       0.021622837607703338    0       0       14      0       0       2       0       0       0.021622837607703338    1.0
...
~~~~

