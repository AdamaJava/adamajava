
--help, -h      Show help message.
--version       Print version.
--log           Req, log file.
--loglevel     	Opt, logging level required, e.g. INFO, DEBUG. Default INFO.

-m              snp,indel,coverage or compoundsnp
-m <mode>	Opt, Mode [snp, compoundsnp, snpcheck, indel, coverage]. Def=snp.
-t              Thread number. Total = number supplied + 2.
-t <threads>	Opt, number of worker threads (yields n+2 total threads). Def=1.
--filter        Query string for qbamfilter
--filter <filter> Opt, a qbamfilter query to filter out BAM records. Def=null, or Def="and(Flag_DuplicateRead==false , CIGAR_M>34 , MD_mismatch <= 3 , option_SM > 10)" (compoundsnp mode).



##snp mode
-o              Output file.
-o <output> 	Req (coverage and snp related mode), the output file path.

-b              Path to tab delimited file with list of bams.
-b <bam list>   Opt (coverage and snp related mode),  path to tab delimited file with list of bams.

-i              Path to bam file.
-i <bam file> 	Opt (coverage and snp related mode), specify a single SAM/BAM file here
--hdf           HDF file to read list of bam files from
--hdf <hdf file> Opt (snp related mode),HDF file to read list of bam files from.          
-r              Path to reference genome fasta file.
-r <reference file>  Req (indel and snp related mode), path to reference genome fasta file.
-s              Path to tab delimited file containing snps. Formats: dcc1,dccq,vcf,maf,txt
-s <snp file> Req (coverage and snp related mode), path to tab delimited file containing snps. Formats: dcc1,dccq,vcf,maf,tab
-f <format>	Opt(snp related mode) snp file format: [dcc1, dccq, vcf, tab, maf].Def=dcc1. or
		Opt(coverage mode), snp file format: [dcc1, dccq, vcf, tab, maf, gff3, gtf]. Def=dcc1. 



-p <???>             Pileup profile type [ddc1,maf,db,dccq,vcf,torrent,RNA,DNA] Def=dcc1.
--bq            Minimum base quality score for accepting a read.
--bq <basequality>	Opt (snp related mode), Minimum base quality score for accepting a read. Def = null.
--mq            Minimum mapping quality score for accepting a read.
--mq <mapping quality>	Opt (snp related mode), Minimum mapping quality score for accepting a read. Def = null.
--ind           Include reads with indels. (y or n, default y).
--ind <y|n>	Opt (snp related mode),, include reads with indels [y,n]. Def=y.
--intron        Include reads mapping across introns. (y or n, default y).
--intron <y|n>  Opt (snp related mode),, include reads mapping across introns [y,n]. Def=y.
--strand        Separate coverage by strand. (y or n, default y)
--strand <y|n>  Opt (snp related mode),,separate coverage by strand [y,n]. Def=y.
--novelstarts   Report novelstarts rather than read count [Y,N], Def=y.
--novelstarts <y|n> Opt (snp related mode),, Report novelstarts rather than read count [Y,N], Def=y.
--dup           Include duplicates
--dup           (indel and snp related mode), a flag of including duplicates reads. 




--ig            Path to somatic indel file
--ig <dcc1>           Path to somatic indel file
--in            Path to normal bam file
--gatk          Adjust insertion position to conform to GATK format
--strelka       adjust insertion position to conform to strelka format (same as pindel)
--pindel        adjust insertion position to conform to pindel format
--hp            window around indel to check for homopolymers  [default:10]
--is            Path to somatic indel file
--it            Path to tumour bam file
--maxcov        Report reads that are less than the mininmum coverage option. Integer
--mincov        Report reads that are less than the
-n              Bases around indel to check for other indels., Def=3.
--of            Output file format [rows,columns].
--og            Output file for germline indels.
--os            Output file for somatic indels.
--pd            <pindel_deletions> Path to normal bam file
--sc            <soft_clip_window> number of bases around indel to check for softclipped bases [default:13]
