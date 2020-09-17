# qbasepileup compound snp mode

~~~~{.text}
java -jar qbasepileup.jar -m compoundsnp ...
~~~~

In this mode, qbasepileup reads one or more BAM files, a reference 
genome, and a file containing positions of compound SNPs (SNPs that 
sit next to each other). It finds the reference genome base at the
compound SNP positions as well as the bases found at that position in 
all reads aligned to that region. Coverage per nucleotide is reported 
and the total coverage at that position is reported. By default, the 
filter is:
`and( Flag_DuplicateRead==false, CIGAR_M>34, MD_mismatch<=3, option_SM>10 )`

## Options

This mode is for compound SNPs, i.e. SNPs that are next to each other. It
is very similar to [snp mode](qbasepileup_snp_mode.md) except:

* Only dcc1 format (`-f`) is currently accepted
* Default filter is: `and(Flag_DuplicateRead==false, CIGAR_M>34, MD_mismatch<=3, option_SM>10)`
