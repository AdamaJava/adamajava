# qannotate

`qannotate` adds annotations to a [VCF](https://en.wikipedia.org/wiki/Variant_Call_Format) file.

There are a variety of different modes in which it can be run, 
each one adding a different annotation to the VCF file.

It can also create a [TSV](https://en.wikipedia.org/wiki/Tab-separated_values) file 
from a VCF file, adding annotations from multiple sources as defined in a 
[JSON](https://en.wikipedia.org/wiki/JSON) file.

The modes available to qannotate (the value passed to `--mode`, case-insensitive) are:
* `cadd`
* `ccm`
* `confidence`
* `dbsnp`
* `germline`
* `hom` (homopolymer)
* `indelconfidence`
* `make_valid` - takes an existing invalid VCF file and makes it valid by splitting joined format columns into separate columns (removing the `&` operator)
* `overlap`
* `snpeff`
* `trf` (tandem repeat)
* `vcf2maf`
* `vcf2maftmp` - accepted by the option parser, but is not currently dispatched to an annotation mode, so running it results in an error


## Installation

`qannotate` requires java 21 and (ideally) a multi-core machine with at least 20GB of RAM.
* To build `qannotate`, first clone the `adamajava` repository.
  ~~~~{.text}
  git clone https://github.com/AdamaJava/adamajava
  ~~~~

*  Then move into the `adamajava` folder:
  ~~~~{.text}
  cd adamajava
  ~~~~

*  Run gradle to build `qannotate` and its dependent jar files:
  ~~~~{.text}
  ./gradlew :qannotate:build
  ~~~~
  This creates the `qannotate` jar file along with dependent jars in the `qannotate/build/flat` folder


## Usage 

~~~~
usage: java -Xmx20G -jar qannotate.jar --mode <annotation mode> --input <vcf file> -d <database file> --output <output vcf> --log <log file> [options]
Option              Description
------              -----------
-h, --help          Show usage and help.
-i, --input         Req, a VCF input file containing the records to be annotated.
--mode              Req, the annotation mode (see list above).
--log               Req, log file.
--loglevel          Opt, logging level [INFO,DEBUG], Def=INFO.
-o, --output        Req, the output VCF file path.
-v, --version       Show version number.
-d, --database      Database file used by the annotation mode. Required by dbsnp, germline,
                    hom, cadd, snpeff and trf modes. May be specified more than once
                    (eg. cadd, overlap). Not available in confidence and vcf2maf modes,
                    which take --test/--control instead.
~~~~

Mode-specific options:

~~~~
confidence and vcf2maf
--test              Opt, test sample column name. Defaults to the value of ##qTestSample in the VCF header.
--control           Opt, control sample column name. Defaults to the value of ##qControlSample in the VCF header.

confidence
--nnsCounts         Opt, number of novel starts (NNS) required to be High Confidence.
--mrCounts          Opt, number of mutant reads (MR) required to be High Confidence.
--mrPercentage      Opt, number of mutant reads (MR) required to be High Confidence as a percentage.
--minCutoff         Opt, minimum number of reads containing the alt that will result in a MIN (Mutation In Normal)
                    in the filter field. Used in conjunction with --minPercentage.
--minPercentage     Opt, percentage of reads containing the alt that will result in a MIN (Mutation In Normal)
                    in the filter field. Used in conjunction with --minCutoff.
--controlCoverageCutoff
                    Opt, minimum coverage (DP format field) value to gain a PASS for control samples.
--controlCoverageCutoffForSomatic
                    Opt, minimum coverage (DP format field) value to gain a PASS for control samples
                    when the call is a SOMATIC one.
--testCoverageCutoff
                    Opt, minimum coverage (DP format field) value to gain a PASS for test samples.
--miunCutoff        Opt, number of failed filter reads (FF) containing the alt that will result in a MIUN
                    (Mutation In Unfiltered Normal). Accepted, but not currently used by confidence mode.

confidence and indelconfidence
--homCutoff         Opt, cutoff value for number of homopolymer bases. Def=6.

hom
--homWindow         Opt, check for homopolymers inside this window size on both sides of variants. Def=100.
--homReportWindow   Opt, number of homopolymer bases on either side of the variant to report. Def=10.

snpeff
--config            Opt, full path to snpEff config file. Defaults to snpEff.config in the database file directory.
--summaryFile       Opt, full path to output stats file. Defaults to <output file name>.snpEff_summary.html.

trf
--buffer            Opt, check TRF region on both sides of indel within this nominated size. Def=0.

cadd
--gap               Opt, adjacent variants size. Def=1000.

vcf2maf
--outdir            Opt, produce all output MAF files under this directory (created if it does not exist).
                    Either --outdir or --output must be supplied.
--donor             Opt, donor id, used in output file names. Defaults to ##qDonorId in the VCF header.
--center            Opt, genome sequencing center.
--sequencer         Opt, sequencer platform, eg. Illumina HiSeq, Illumina MiSeq, Ion Torrent PGM.
~~~~

OR

~~~~
usage: java -Xmx20G -cp qannotate.jar au.edu.qimr.qannotate.nanno.Annotate  --input <vcf file> --config <json configuration file> --output <output TSV> --log <log file>
Option              Description
------              -----------
-h, --help          Show usage and help.
-i, --input         Req, a VCF input file containing the records to be annotated.
--config            Req, a JSON configuration file containing the annotation sources (see Configuration below).
--log               Req, log file.
--loglevel          Opt, logging level [INFO,DEBUG], Def=INFO.
-o, --output        Req, the output TSV file path.
-v, --version       Show version number.
~~~~

## Output

### VCF output

Each of the different annotation modes will append a different annotation to the VCF file.

### TSV output

TSV output will contain the fields that have been defined in the configuration file.
The following fields will be present as a bare minimum:

~~~
chr position  ref alt GATK_AD
~~~

## Configuration

This section will describe the JSON configuration file that is used when creating a TSV file from a VCF file (ie. not when `qannotate` is run using one of the `modes`)
  
### Example JSON configuration file

~~~
{
  "outputFieldOrder": "aaref,aaalt,refcodon,gene_name,feature_id,feature_type,effect,cdna_position,cds_position,protein_position,putative_impact,SIFT_pred,SIFT_score,hgvs.c,hgvs.p,gnomAD_genomes_POPMAX_AF,CADD_raw,CADD_raw_rankscore,CADD_phred,DANN_score,DANN_rankscore,PROVEAN_pred,REVEL_score,CLNSIG,CLNREVSTAT,ALLELEID,CLNHGVS,MC,CLNVI,DBVARID,CLNDN,gnomAD_genomes_AF,gnomAD_genomes_AFR_AF,gnomAD_genomes_AMR_AF,gnomAD_genomes_ASJ_AF,gnomAD_genomes_EAS_AF,gnomAD_genomes_FIN_AF,gnomAD_genomes_NFE_AF,gnomAD_genomes_AMI_AF,gnomAD_genomes_SAS_AF,FATHMM_pred,MutationAssessor_pred,MutationTaster_converted_rankscore,MutationTaster_pred,Polyphen2_HDIV_pred,Polyphen2_HVAR_pred,Polyphen2_HDIV_score,Polyphen2_HDIV_rankscore,Polyphen2_HVAR_score,Polyphen2_HVAR_rankscore,BayesDel_addAF_score,BayesDel_addAF_rankscore,BayesDel_addAF_pred,BayesDel_noAF_score,BayesDel_noAF_rankscore,BayesDel_noAF_pred,clinvar_OMIM_id,clinvar_id",
  "additionalEmptyFields": "test1,test2,test3",
  "includeSearchTerm": true,
  "annotationSourceThreadCount": 3,
  "inputs": [{
    "file": "/reference/data/dbNSFP/4.1a/dbNSFPv4.1a_sorted.gz",
    "chrIndex": 1,
    "positionIndex": 2,
    "refIndex": 3,
    "altIndex": 4,
    "fields": "aaref,aaalt,refcodon,BayesDel_addAF_score,BayesDel_addAF_rankscore,BayesDel_addAF_pred,BayesDel_noAF_score,BayesDel_noAF_rankscore,BayesDel_noAF_pred,DANN_score,DANN_rankscore,SIFT_score,CADD_phred,CADD_raw,CADD_raw_rankscore,PROVEAN_pred,REVEL_score,SIFT_pred,gnomAD_genomes_AF,gnomAD_genomes_POPMAX_AF,gnomAD_genomes_AFR_AF,gnomAD_genomes_AMR_AF,gnomAD_genomes_ASJ_AF,gnomAD_genomes_EAS_AF,gnomAD_genomes_FIN_AF,gnomAD_genomes_NFE_AF,gnomAD_genomes_AMI_AF,gnomAD_genomes_SAS_AF,FATHMM_pred,MutationAssessor_pred,MutationTaster_converted_rankscore,MutationTaster_pred,Polyphen2_HDIV_pred,Polyphen2_HVAR_pred,Polyphen2_HDIV_score,Polyphen2_HDIV_rankscore,Polyphen2_HVAR_score,Polyphen2_HVAR_rankscore,clinvar_OMIM_id,clinvar_id"
  },
  {
    "file": "NA12878_ERR194147.genotype.hard_filtered.snpEff.GRCh38.105.eff.vcf",
    "chrIndex": 1,
    "positionIndex": 2,
    "refIndex": 4,
    "altIndex": 5,
    "snpEffVcf": true,
    "fields": "gene_name,feature_id,feature_type,effect,cdna_position,cds_position,protein_position,putative_impact,hgvs.c,hgvs.p"
  },
  {
    "file": "/reference/data/clinvar/vcf_GRCh38/clinvar.vcf.gz",
    "chrIndex": 1,
    "positionIndex": 2,
    "refIndex": 4,
    "altIndex": 5,
    "fields": "CLNSIG,CLNDN,CLNREVSTAT,CLNVI,DBVARID,MC,ALLELEID,CLNHGVS"
  }]
}
~~~


### Field description

* `outputFieldOrder` Ordered list of fields from the various input sources. An exception will be thrown if this list contains a field that has not been defined in one of the inputs, or if the this field does not contain a field defined in one of the inputs.
* `additionalEmptyFields` Ordered list of fields (comma separated) that will be appended to the output. Useful when needing to add additional information not available in the inputs
* `includeSearchTerm` true/false value to indicate if a field should be appended to each record that uses the HGVS.c and HGVS.p fields (assuming that they have been specified in one of the inputs) to build a search term for use in PubMed.
* `annotationSourceThreadCount` number of threads that should be made available to retrieve data from the inputs. Should be equal to the number of inputs for optimal performance. Defaults to 1.
* `inputs` List of annotation sources, each of which must provide the following information:
  * `file` location of annotation source file
  * `chrIndex` position (1 based) of chromosome in annotation source file
  * `positionIndex` position (1 based) of the variant position column in annotation source file
  * `refIndex` position (1 based) of the reference allele column in annotation source file
  * `altIndex` position (1 based) of the alternate allele column in annotation source file
  * `fields` list of required fields (comma separated) from this annotation source
  * `snpEffVcf` true/false value indicating if this annotation source file is a snpEff VCF file
  * `chrStartsWithChr` optional boolean, defaults to `false`. Set to `true` if the chromosome values in this annotation source file start with `chr` (eg. `chr1`), so that the prefix is stripped when matching against the input records.
  * `byteBuffered` optional boolean, defaults to `false`. For large UTF-8 TSV sources with `chrIndex=1` and `positionIndex=2`, set this to `true` to skip unwanted rows as bytes without constructing their full strings. Supports plain text and gzip files. Other source types or column layouts use the standard reader and log a warning. All traversed gzip data is still decompressed.

For example, enable the byte-buffered reader for a gnomAD TSV source with:

```json
{
  "file": "/reference/data/gnomad.genomes.v3.1.2.sites.all.tsv.gz",
  "chrIndex": 1,
  "positionIndex": 2,
  "refIndex": 4,
  "altIndex": 5,
  "chrStartsWithChr": true,
  "byteBuffered": true,
  "fields": "AF,AN,AC,vep"
}
```

