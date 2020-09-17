# qmule MafFilter mode


This [qmule](index.md) mode searches the supplied directory for MAF files
(.maf extension) and applies QCMG-specific filters to sort the records 
into 2 MAFs - one containing high confidence calls (more stringent 
filtering) and the other containing lower confidence calls.  This mode 
only works if your MAF has been generated at QCMG and has the 
QCMG-specific annotations.

## Usage

~~~~{.text}
qmule org.qcmg.qmule.MafFilter -i $HOME/maf \
                               -o $HOME/maf/highConfidence.maf \
                               -o $HOME/maf/lowerConfidence.maf \
                               -log $HOME/maf/maf_filter.log
~~~~

## Options

~~~~{.text}
-i, --input <directory>      Directory containing maf files to be filtered
-o, --output <output_file>   High confidence filtered maf file
-o, --output <output_file>   Low confidence filtered maf file
    --log <log_file>         Mandatory log file
    --loglevel <DEBUG>       Optional logging level (defaults to INFO)
-h, --help                   Shows this help message.
-v, --version                Print version info.
~~~~

The options can appear in any order except for the 2 output files where
the high confidence file must be listed first.
