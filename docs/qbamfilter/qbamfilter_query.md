# qbamfilter query

The `qbamfilter` library is used in almost all AdamaJava tools as a way 
of filtering out BAM file records that are not appropriate for the analysis
being conducted.
The heart of qbamfilter is its query language which determines which
records pass and are included in the analysis, and which records fail and
are discarded.

The general form of a qbamfilter query string is:

~~~~{.text}
operator( condition [, condition|query]* )
~~~~

The query string is comprised of one or more conditions and zero or more
queries joined by operators. Currently there are only two operators 
available - `and()` and  `or()`. Conditions take the form:

~~~~{.text}
key comparator value 
~~~~

There is no operator required if the query contains a single condition.

Conditions are evaluated left to right so standard
short-circuit thinking applies, i.e. in an `and()` operator, if the first
condition evaluates to `FALSE`, the rest of the conditions are not evaluated.
This means it is cheaper (and faster) to put the conditions that will 
reject the most records first, i.e. on the left.

## Example Queries

~~~~{.text}
RNAME =~ chr* 
~~~~

This first example has a single condition `RNAME =~ chr*`. Remembering 
that records are only kept if the qbamfilter string resolves to TRUE,
this query has the effect of rejecting any BAM record where the RNAME
field does not contain the string "chr".  This is useful in cases 
where your reference contains chromosomes plus additional non-chromosome
sequences, e.g. GL000191.1, and where you wish to ignore the 
non-chromosome matches for a particular analysis.

~~~~{.text}
and( RNAME =~ chr*, Cigar_M > 35 )
~~~~

This query is based on the previous query but with the addition of the
`and()` operator and a second condition that requires that more than 35 
of the bases in the CIGAR string are "M" indicating a match or mismatch.
The effect of this second condition is that records where most of the 
bases are clipped or inserted are discarded.

~~~~{.text}
and( RNAME =~ chr*, Cigar_M > 35, or( MAPQ > 20, option_ZM == 1 ) )
~~~~

This example shows the addition of a query to the `and()` operator so
now there are 2 conditions and a sub-query.  The or() clause shown has 
2 conditions and will return `TRUE` if either of the conditions are
`TRUE`.  The first condition requires that a record's MAPQ score is above
20 and the second condition requires that a user-supplied ZM field is 
present and has a valule of 1.

~~~~{.text}
and( RNAME =~ chr*, Cigar_M > 35, or(MAPQ > 50, option_ZM == 1), Flag_DuplicateRead == false )
~~~~

This query string adds another condition - that the `FLAG` field
indicates that the record is not a duplicate. Remember that all
conditions need to evaluate to true for the `and()` operator to evaluate
to `TRUE` and pass the record, so we have to be careful with each
condition to make sure it is doing what we want. In this case we don't want
any duplicate reads to pass and make it into our analysis so we need
`Flag_DuplicateRead == false`.

## BAM Fields

The tables below list available condition types for SAM/BAM fields.

### FLAG

FLAG is a bitmap of properties of the read including some properties of
the read's pair for paired-end or mate-pair sequencing.

Key | Comparator | Value 
----|------------|------
`flag_ReadPaired`<br> `flag_ProperPair`<br> `flag_ReadUnmapped`<br> `flag_Mateunmapped`<br> `flag_ReadNegativeStrand`<br> `flag_MateNegativeStrand`<br> `flag_FirstOfpair`<br> `flag_SecondOfpair`<br> `flag_NotprimaryAlignment`<br> `flag_ReadFailsVendorQuality`<br> `flag_DuplicateRead`<br> `flag_SupplementaryRead`<br> |`==`<br>`!=`<br> |`1`<br> `0`<br> `true`<br> `false`

Note that Values are case insensitive so `true`, `True` and `TRUE` are
all equivalent.  Also note that `1` and `true` are
equivalent and that `0` and `false` are equivalent.

_Example:_ match duplicate reads

~~~~{.text}
flag_DuplicateRead == true
flag_DuplicateRead != 0
~~~~

### CIGAR

Key | Comparator | Value
----|------------|------
`Cigar_M`<br> `Cigar_I`<br> `Cigar_D`<br> `Cigar_N`<br> `Cigar_S`<br> `Cigar_H`<br> `Cigar_P`<br>|`==`<br>`!=`<br>`>=`<br>`<=`<br>`>`<br>`<`|integer 

<br>
For an explanation of the various CIGAR values - M, I, D, etc - see the
[BAM specification](https://samtools.github.io/hts-specs/SAMv1.pdf).

_Example 1:_ match records with a count of matched/mismatched
bases greater than 15

~~~~{.text}
Cigar_M >= 16
Cigar_M > 15
~~~~

_Example 2:_ match records that have at least 1 base inserted or deleted

~~~~{.text}
or( Cigar_I > 0, Cigar_D > 0 )
~~~~

### MAPQ

Key | Comparator | Value
----|------------|------
MAPQ|`==`<br>`!=`<br>`>=`<br>`<=`<br>`>`<br>`<`|integer

<br>
_Example:_ match records with a mapping quality greater than 20:

~~~~{.text}
MAPQ > 20
~~~~

### SEQ

Key | Comparator | Value
----|------------|------
seq_numberN	|`==`<br>`!=`<br>`>=`<br>`<=`<br>`>`<br>`<`|integer

<br>
The only property that can be queried here is the count of "N" bases.

_Example:_ match records containing less than 5 N bases

~~~~{.text}
seq_numberN < 5
~~~~


### QUAL

Key | Comparator | Value 
----|------------|------
qual_average|`==`<br>`!=`<br>`>=`<br>`<=`<br>`>`<br>`<`|integer

The only property that can be queried here is the average base quality.

_Example:_ match records where average base quality is less than 20

~~~~{.text}
qual_average < 20
~~~~

### TLEN

Key | Comparator | Value
----|------------|------
TLEN|`==`<br>`!=`<br>`>=`<br>`<=`<br>`>`<br>`<`|integer

_Example:_ match records with template size arger than 1000

~~~~{.text}
TLEN > 1000
~~~~

### POS

Key | Comparator | Value
----|------------|------
pos|`==`<br>`!=`<br>`>=`<br>`<=`<br>`>`<br>`<`|integer

<br>
_Example:_ match records with start position between 1000 and 2000
inclusive

~~~~{.text}
and( pos >= 1000, pos <= 2000 )
~~~~

### RNAME, RNEXT

Key | Comparator | Value
----|------------|------
RNAME<br>RNEXT|`==`<br>`!=`|string (exact match)
|`=~`<br>`!~`|string (wildcard '*' at start or end of string)

_Example 1:_ match records that mapped to the X chromosome

~~~~{.text}
RNAME == chr1
~~~~

_Example 2:_ match records where the paired read maps to a different
sequence

~~~~{.text}
RNAME != RNEXT
~~~~

_Example 3:_ match records where the paired read maps to a different
sequence and one of the reads in the pair mapped to chromosome X

~~~~{.text}
and( RNAME != RNEXT, or( RNAME == chrX, RNEXT == X ) )
~~~~

### Optional Fields

BAM records can have an optional 12th field where users can set their
own fields of the form `TAG:TYPE:VALUE`. The `option_XX` condition
allows queries to run against optional fields where `XX` is replaced by
the name of the tag - see the examples below.  Three types of comparison
are currently supported: integer logic, exact string matching and string
pattern matching.

Key | Comparator | Value
----|------------|------
option_<tag\>|`==`<br>`!=`<br>`>=`<br>`<=`<br>`>`<br>`<`|integer
|`==`<br>`!=`|string (exact match)
|`=~`<br>`!~`|string (wildcard '*' at start or end of string)

_Example 1:_ match records where the tag "ZM" has a value less than 2

~~~~{.text}
option_ZM < 2
~~~~

_Example 2:_ match records with tag "RG" set to 'Tumor'

~~~~{.text}
option_RG == Tumor
~~~~

_Example 3:_ match records with tag "ZP" set to 'Z**'

~~~~{.text}
option_ZP == Z**
~~~~

_Example 4:_ match records where the tag "RG" does not contain the
substring "known"

~~~~{.text}
option_RG !~ known
~~~~

_Example 5:_ match records where the tag "ZP" does not start with 'null'

~~~~{.text}
option_ZP !~ null*
~~~~

_Example 6:_ match records where the tag "ZP" does not end with 'null'

~~~~{.text}
option_ZP !~ *null
~~~~

### Special conditions

There is a special query condition available for use with the Optional
Field MD. MD is almost always present in BAM files and in
conjunction with the CIGAR string, it can exactly specify the
differences between the read sequence and the reference sequence.

An extra condition using the key `MD_mismatch` can be used to operate 
against a count of the number of mismatched bases in a read as
determined from the MD Optional Field.

Key | Comparator | Value
----|------------|------
MD_mismatch|`==`<br>`!=`<br>`>=`<br>`<=`<br>`>`<br>`<`|integer

<br>
_Example:_ match records where there are less than 4 bases mismatched
against the reference

~~~~{.text}
MD_mismatch < 4
~~~~
