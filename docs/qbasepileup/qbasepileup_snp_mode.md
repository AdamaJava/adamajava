# qbasepileup snp mode

~~~~{.text}
java -jar qbasepileup.jar -m snp ...
~~~~

In this mode, qbasepileup reads one or more BAM files, a reference genome, 
and a file containing positions of SNPs. It finds the reference genome base
at the SNP position as well as the bases found at that position in all 
reads aligned to that region. Coverage per nucleotide is reported and the
total coverage at that position is reported. By default, duplicates and
unmapped reads are excluded.

## Options

~~~~{.text}
--help, -h     Shows usage and help text
--version, -v  Shows current version number.
--log          Required. Path to log file.
--loglevel     Optional. Logging level, e.g. INFO,DEBUG. Default INFO.
-i             Optional: snp or indel. Default snp.
-i             Required if no -b option. Path to BAM file.
-b             Required if no -i option. Path to tab delimited file with 
                   list of BAMs. File should contain 3 columns: Integer 
                   identifier, Donor, Path to BAM file.
-s             Required. Path to containing file of snp positions.
                   Formats: dcc1, maf, tab, dccq, vcf
-r             Required. Path to reference genome fasta file.
-o             Required. Output file name.
-of            Optional. Output file format: rows or columns (default rows). 
                   Results for the output file listing each bam by row or
                   column.
-f             Optional. Format of SNPs file. Default dcc1.
-p             Optional. Run a pileup profile. Default standard.
--filter       Optional. qbamfilter query to use
--t            Optional. Thread number. Total thread number = number
                   supplied + 2. Default 1 (total threads 4).
--strand       Optional. Separate coverage by strand. (y or n, default y).
--mq           Optional. Minimum mapping quality score for accepting a read.
                   Default is any mapping quality ie no filtering.
--bq           Optional. Minimum base quality score for accepting a read.
                   Default is any base quality ie no filtering.
--intron       Optional. Include reads with indels. (y or n, default y).
--ind          Optional. Include reads mapping across introns. (y or n,
                   default y).
--novelstarts  Optional. Report number of novel starts for each base type,
                   rather than number of reads. (y or n, default n).
~~~~

### `-f`

Format of the SNP file specified with `-s`. This option is optional and
the default is dcc1.  Current supported formats are:

* dcc1 - an ICGC data submission format
* maf - Mutation Annotation Format
* dccq - an extension of the dcc1 format but with extra fields
* vcf - Variant Call Format
* tab - Tab-delimited

If you don't have a VCF or MAF file, the tab format is often the easiest
to construct. It is a tab delimited plain text file with 4 columns and no
header. The columns are: id, chromosome, start_position, end_position

### `--profile`

This option provides predefined sets of values for 5 other
options that are frequently used together: `--strand`, `--indel`,
`--intron`, `-bq`, and `-mq`.
There are 4 currently defined progiles - `standard`, `dna`, `rna`,
`torrent`.

All profiles will ignore unmapped reads and reads where the DuplicateRead flag is true.

<table>
<tr>
<th rowspan=2>Profile</th><th colspan=2>Filter</th>
<th colspan=4>Metrics</th>
</tr>
<tr><th>Mapping quality</th><th>Base quality</th><th>Strand Specific</th>
<th>Include introns</th><th>Include indels</th><th>Novelstarts</th>
</tr>
<tr><td>standard</td><td>any</td><td>any</td><td>y</td><td>y</td><td>y</td><td>n</td></tr>
<tr><td>dna</td><td>10</td><td>10</td><td>y</td><td>n</td><td>y</td><td>n</td></tr>
<tr><td>rna</td><td>10</td><td>7</td><td>n</td><td>y</td><td>y</td><td>y</td</tr>
<tr><td>torrent</td><td>1</td><td>0</td><td>n</td><td>y</td><td>y</td><td>n</td></tr>
</table>

### `--strand`, `--indel`, `--intron`, `-bq`, `-mq`

### `--novelstarts`

novelstarts is a count of how many reads with different start sites
cover a particular position. For example, if 10 reads covered a position
but they all had alignments starting at the same position then
novelstarts=1.  If 10 reads covered a position and 4 started at the same
position and the outher 6 all started at different positions then
novelstarts=7. novelstarts helps spot cases where there
might be some question about whether the reads are duplicates, even if
they are paired ans the pairs seem to be different. 

## Examples

### Defaults with BAM file list

~~~~{.text}
qbasepileup -b bam_list.txt -s snps.dcc1 -r reference.fa \
    -o output.pileup.txt --log log_file.log
~~~~

* Default file format is dcc1
* Standard profile:
    * Print strand specific info
    * Include any base or mapping quality
    * Include reads in introns
    * Include reads with indels


### Defaults with single input BAM file

~~~~{.text}
qbasepileup -i input.bam -s snps.dcc1 -r reference.fa \
    -o output.pileup.txt --log log_file.log
~~~~

* Default file format is dcc1
* Standard profile:
    * Print strand specific info
    * Include any base or mapping quality
    * Include reads in introns
    * Include reads with indels


### Use of `--filter` qbamfilter query

~~~~{.text}
qbasepileup -b bam_list.txt -s snps.dcc1 -r reference.fa \
    -o output.pileup.txt --log log_file.log --filter "option_SM > 30"
~~~~

* Default file format is dcc1
* Standard profile.
* Qbamfilter query of option_SM > 30


### SNP file format is MAF

~~~~{.text}
qbasepileup -b bam_list.txt -s snps.maf -r reference.fa \
    -o output.pileup.txt --log log_file.log -f maf
~~~~

### Torrent profile

~~~~{.text}
qbasepileup -b bam_list.txt -s snps.maf -r reference.fa \
    -o output.pileup.txt --log log_file.log -f maf -p torrent
~~~~

* File format is MAF
* Torrent profile:
    * Do not print strand specific info
    * Min base quality: 0
    * Min mapping quality: 1
    * Include reads in introns
    * Include reads with indels


### Run with user defined filtering options

~~~~{.text}
qbasepileup -i input.bam -s snps.dcc1 -r reference.fa \
    -o output.pileup.txt --log log_file.log \
    --strand n --indel n --intron n -bq 5 -mq 10
~~~~

* No strand specific base coverage information
* Do not include reads with indels
* Do not include reads in introns
* Read must have a minimum mapping quality of 10
* Read must have a minimum base quality of 5.


### Novel starts

~~~~{.text}
qbasepileup -b bam_list.txt -s snps.dcc1 -r reference.fa \
    -o output.pileup.txt --log log_file.log --novelstarts
~~~~

* File format is dcc1
* Print strand specific info
* Include any base or mapping quality
* Include reads in introns
* Include reads with indels
* Count only novel starts
