
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
-o <file> 	Req (coverage and snp related mode), the output file path.
--of <row|columns>    Output file format [rows,columns].

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

--maxcov <Integer>       Opt (unused), report reads that are less than the mininmum coverage option. 
--mincov <Integer>       Opt, report reads that are less than the mininmum coverage option. 


-p <String>     Opt, Pileup profile type [torrent,RNA,DNA, standard]. Def="standard".
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
--dup           Opt (indel and snp related mode), a flag of including duplicates reads. 

--ig            Path to somatic indel file
--ig <dcc1>     Opt, path to germline indel file, Def=null
--is            Path to somatic indel file
--is <dcc1>     Opt, Path to somatic indel file, Def=null

--it            Path to tumour bam file
--it <bam>      Req,Path to tumour bam file
--in            Path to normal bam file
--in <bam>      Req,Path to normal bam file

--pd            <pindel_deletions> Path to normal bam file
--pd <file>     Opt (deprecated), Path to pindel deletion file


--og <file>     Opt, Output file for germline indels. Def=null.
--os <file>     Opt, Output file for somatic indels. Def=null.

--gatk          Adjust insertion position to conform to GATK format
--strelka       adjust insertion position to conform to strelka format (same as pindel)
--pindel        adjust insertion position to conform to pindel format

--hp            window around indel to check for homopolymers  [default:10]
--hp <Integer>    Opt, base around indel to check for homopolymers. Def=10.
-n              Bases around indel to check for other indels., Def=3.
-n <Integer>      Opt, Bases around indel to check for other indels. Def=3.
--sc            <soft_clip_window> number of bases around indel to check for softclipped bases [default:13]
--sc <Integer>    Opt, bases around indel to check for softclip. Def=13.


