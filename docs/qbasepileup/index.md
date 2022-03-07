# qbasepileup
`This tool may deprecate soon, due to some input format is no longer supported!`

## Introduction

`qbasepileup` performs base pileup on positions of interest in a BAM
file and produces base coverage information and various others
metrics on the reads at the positions of interest.

## Installation

qbasepileup requires java 8 and (ideally) a multi-core machine (5 threads 
are run concurrently) with at least 20GB of RAM.

* **To do a build of qbasepileup, first clone the adamajava repository using "git clone":**
  ```
  git clone https://github.com/AdamaJava/adamajava
  ```

  Then move into the adamajava folder:
  ```
  cd adamajava
  ```
  Run gradle to build qbasepileup and its dependent jar files:
  ```
  ./gradlew :qbasepileup:build
  ```
  This creates the qbasepileup jar file along with dependent jars in the `qbasepileup/build/flat` folder


## Usage

A general invocation of qbasepileup looks like:

~~~~{.text}
java -jar qbasepileup.jar -m mode \
    -i file.bam -r ref.fa -s positions.txt -o pileup.txt --log file.log
~~~~

## Options

~~~~{.text}
Option                   Description                                                                             
------                   -----------                                                                             
-V, -v, --version        Print version info.                                                                     
-b <txt file>            Opt (coverage and snp mode),  path to tab delimited file with list of bams.             
--bq <Integer>           Opt (snp related mode), minimum base quality score for accepting a read. Def = null.    
--dup                    Opt (indel and snp mode), a flag to include duplicates reads.                           
-f [format]              Opt(snp mode) snp file format: [dcc1, dccq, vcf, tab, maf].Def=dcc1. or                 
                         Opt(coverage mode), snp file format: [dcc1, dccq, vcf, tab, maf, gff3, gtf]. Def=dcc1.  
--filter <query>         Opt, a qbamfilter query to filter out BAM records. Def=null.                            
--gatk                   Opt (indel mode), a flag to conform gatk format, but do nothing.                        
-h, --help               Shows this help message.                                                                
--hdf <hdf file>         Opt (snp mode), path to hdf file which header contains a list of bams.                  
--hp <Integer>           Opt (indel mode), base around indel to check for homopolymers. Def=10.                  
-i <bam file>            Opt (coverage and snp mode), specify a single SAM/BAM file here.                        
--ig <dcc1>              Req (indel mode), path to germline indel file in dcc1 format.                           
--in <bam file>          Req (indel mode), path to normal bam file                                               
--ind <y|n>              Opt (snp related mode), include reads with indels [y,n]. Def=y.                         
--intron <y|n>           Opt (snp related mode), include reads mapping across introns [y,n]. Def=y.              
--is <dcc1>              Req (indel mode), path to somatic indel file in dcc1 format.                            
--it <bam file>          Req (indel mode), path to tumour bam file                                               
--log                    Req, log file.                                                                          
--loglevel               Opt, logging level required, e.g. INFO, DEBUG. Default INFO.                            
-m <mode>                Opt, Mode [snp, compoundsnp, snpcheck, indel, coverage]. Def=snp.                       
--mincov <Integer>       Opt, report reads that are less than the mininmum coverage option.                      
--mq <Integer>           Opt (snp related mode), minimum mapping quality score for accepting a read. Def = null. 
-n <Integer>             Opt (indel mode), bases around indel to check for other indels. Def=3.                  
--novelstarts <y|n>      Opt (snp related mode), report novelstarts rather than read count [Y,N], Def=y.         
-o <txt file>            Req (coverage and snp related mode), the output file path.                              
--of <format>            Opt (snp mode only), output file format [columns]. this option only works with input snp
                           file format is tab, otherwise it will ignored.                                        
--og <dcc1>              Req (indel mode), output file for germline indels.                                      
--os <dcc1>              Req (indel mode), output file for somatic indels.                                       
-p [profile]             Opt (snp mode), pileup profile type [torrent,RNA,DNA, standard]. Def="standard".        
--pd <pindel_deletions>  Req (indel mode), path to normal bam file                                               
--pindel                 Opt (indel mode), a flag to conform pindel format, but do nothing.                      
-r <fasta file>          Req (indel and snp mode), path to reference genome fasta file.                          
-s <txt file>            Req (coverage and snp mode), path to tab delimited file containing snps.                
--sc <Integer>           Opt (indel mode), bases around indel to check for softclip. Def=13.                     
--strand <y|n>           Opt (snp related mode), separate coverage by strand [y,n]. Def=y.                       
--strelka                Opt (indel mode), a flag to conform strelka format, but do nothing.                     
-t [Integer]             Opt, number of worker threads (yields n+2 total threads). Def=1.         
~~~~

## Modes
### coverage

Reads one or more BAM files, and a file containing reference ranges and
piles up the reads around the indel to count the number of reads covering
each position in the range.


### [snp](qbasepileup_snp_mode)

Reads one or more BAM files, a reference genome, and a file containing
positions of SNPs. It finds the reference genome base at the SNP position 
as well as the bases found at that position in all reads aligned to that
region. Coverage per nucleotide is reported and the total coverage at that
position is reported. By default, duplicates and unmapped reads are excluded.

### [compoundsnp](qbasepileup_compound_snp_mode)

```This mode is deprecated due to the input snp position file format (dcc1) is no longer available!```

In this mode, qbasepileup reads one or more BAM files, a reference genome, and a file containing positions of compound SNPs (SNPs that sit next to each other). It finds the reference genome base at the compound SNP positions as well as the bases found at that position in all reads aligned to that region. Coverage per nucleotide is reported and the total coverage at that position is reported. By default, the filter is:

~~~~{.text}
and( Flag_DuplicateRead==false, CIGAR_M>34, MD_mismatch <= 3, option_SM > 10)
~~~~

For a more detailed description of qbamfilter and how it works to filter
reads in and out of a particular analysis, see[qbamfilter](../qbamfilter/).


This mode is for compound SNPs, is very similar to [snp mode](qbasepileup_snp_mode.md) except:

* Only dcc1 format (`-f`) is currently accepted
* Default filter is: `and(Flag_DuplicateRead==false, CIGAR_M>34, MD_mismatch<=3, option_SM>10)`


### [indel](qbasepileup_indel_mode)
`This mode is deprecated due to the indle file format (dcc1) is no longer available!`

Reads tumour and normal BAM files, a reference genome, and somatic and/or 
germline files containing positions of indels and pileups the reads around 
the indel to count a number of metrics. Metrics include:

* total reads 
* number of reads that span the indel
* number of reads with the indel
* number of novel starts with the indel
* number of reads with nearby soft clipping
* number of reads with nearby indels

#### Examples

* Somatic and Germline input files in pindel format
~~~~{.text}
qbasepileup -t 2 -m indel -r reference.fa --pindel \
    --it tumour.bam --in normal.bam \
    --is somatic.input.dcc1 --ig germline.input.dcc1 \
    --os somatic.output.dcc1 --og germline.output.dcc1 \
    --log basepileup.log
~~~~

* Somatic input file in gatk format
~~~~{.text}
qbasepileup -t 2 -m indel --it tumour.bam --in normal.bam \
    --is somatic.input.dcc1 --os somatic.output.dcc1 
    --log basepileup.log -r reference.fa --gatk
~~~~

* Germline input file in pindel format
~~~~{.text}
qbasepileup -t 2 -m indel --it tumour.bam --in normal.bam \
    --ig germline.input.dcc1 --og germline.output.dcc1
    --log basepileup.log -r reference.fa --pindel
~~~~
