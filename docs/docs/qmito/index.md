# qmito

`qmito` produces mitochondrial sequencing summary reports from BAM files.

## Usage

~~~~{.text}
java -jar qmito.jar [options]
~~~~

## Options

~~~~{.text}
--help 
--version 
-m            Mode, [metric, stat]
--input, -i   Input BAM file
--output, -o  Output file
--log         Log file
--thread      Opt, number of execution threads (default=2)
~~~~

qmito only works on single BAMs so if you have tumour/normal pairs, or 
families or other collections of related BAM files that you may wish to 
analyse jointly, you should run qmito in `--mode metric` 
independently on each of the BAMs and then use `--mode stat` to compare
the `metric` files.

Design

* read in a single BAM
* use qbamfilter to only keep read pairs we want: `and( or(RNAME ==
'chrM', RNEXT == 'chrM'), flag_NotPrimaryAlignment==false,
flag_ReadFailsVendorQuality==false)`
* note that DUPs and unmapped reads are OK which means the filter should pull out pairs where one or both reads mapped to the mitochondrial sequence
* write a report with the same params as Felicity's `qpileup` (approx 40 params) but we want to write it to a TSV file, not the HDF5 format used by `qpileup`.

example `--filter` string:

~~~~{.text}
and( or(RNAME == 'chrM', RNEXT == 'chrM'),
    flag_NotPrimaryAlignment==false,
    flag_ReadFailsVendorQuality==false,
    flag_SupplementaryRead)
~~~~

~~~~{.text}
and( or(RNAME == 'chrM', RNEXT == 'chrM'),
     flag_NotPrimaryAlignment==false,
     flag_ReadFailsVendorQuality==false,
     flag_SupplementaryRead)
~~~~

### mode

#### metric

This mode reads multiple BAM files

Options:

~~~~{.text}
-h, --help
-q, --query
-i, --input
-o, --output
-r, --referenceFile
--log
--loglevel
--lowreadcount
--nonrefthreshold
~~~~

#### stat

Stat mode reads 2 metric mode report files and does a chi-square
of the A/C/G/T base counts of test vs control.  Forward and reverevse
strand counts are done separately.

~~~~{.text}
-h, --help
-t, --test
-c, --contronal
-o, --output
--log
--loglevel
~~~~

## Examples

~~~~{.text}
java -jar -Xms5g -Xmx5g  qmito-0.1pre.jar \
    -i $tumourBAM \
    -o ${tumourBAM}.qmito.tsv \
    --log ${tumourBAM}.qmito.log
~~~~

Output is a tab-separated plain-text file with unix line endings. The
format is that each position in the mitochondrial genome is a row and 
the columns are the various parameters collected at each position.

~~~~{.text}
#position  A_forward  C_forward  G_forward T_forward …
1          7          95         0         0
2          93         0          0         0
3          0          0          107       2
4          0          8          96        1
...
~~~~


From class PileupDataRecord it looks like these might be the items that
are collected and stored in metric mode for forward and reverse strands.

~~~~{.text}
        public static String headings;
        private final Integer position;
        private int referenceNo = 0;
        private int nonReferenceNo = 0;
        private final int highNonReference = 0;
        private int lowReadCount = 0;
        private int baseA = 0;
        private int baseC = 0;
        private int baseG = 0;
        private int baseT = 0;
        private int baseN = 0;
        private long aQual = 0;
        private long cQual = 0;
        private long gQual = 0;
        private long tQual = 0;
        private long nQual = 0;
        private long mapQual = 0;
        private int startAll = 0;
        private int startNondup = 0;
        private int stopAll = 0;
        private int dupCount = 0;
        private int mateUnmapped = 0;
        private int cigarI = 0;
        private int cigarD = 0;
        private int cigarDStart = 0;
        private int cigarS = 0;
        private int cigarSStart = 0;
        private int cigarH = 0;
        private int cigarHStart = 0;
        private int cigarN = 0;
        private int cigarNStart = 0;
        private String reference;
        boolean isReverse = false;
~~~~
