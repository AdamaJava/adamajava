#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qtrim.pl
#  Author:   John V Pearson
#  Created:  2011-02-02
#
#  A pre-alignment tool to take FASTQ files written out by a PGM and
#  convert bases below a given quality to N.  The original purpose of
#  this script was to test whether convertin low quality bases to N
#  helped the tmap aligner produce alignments with fewer deletions and
#  insertions.  The thinking was that tmap might be inclined to open a
#  gap to accomodate a low quality base rather than calling it as a 
#  mismatch.
#
#  $Id: qtrim.pl 4668 2014-07-24 10:18:42Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::FileDir::Finder;
use QCMG::IO::SamReader;
use QCMG::IO::SamRecord;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4668 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qtrim.pl 4668 2014-07-24 10:18:42Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile     ='';
    my $length     =35;
    my @scores     = ();
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \$infile,        # -i
           'l|length=i'           => \$length,        # -l
           's|scores=s'           => \@scores,        # -s
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    # Allow for ,-separated lists of patterns
    @scores = map { split /\,/,$_ } @scores;

    print( "\nqtrim.pl  v$REVISION  [" . localtime() . "]\n",
           "   infile        $infile\n",
           "   length        $length\n",
           '   score(s)      ', join(',',@scores), "\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;


    # Do something here !!!

    foreach my $score (@scores) {
        apply_threshold_score( $infile, $length, $score );
    }
    print '[' . localtime() . "] Finished.\n";
}


sub apply_threshold_score {
    my $infile = shift;
    my $length = shift;
    my $tscore = shift;

    my $bam = QCMG::IO::SamReader->new( filename => $infile );
    while (my $rec = $bam->next_record_as_record) {

        print join("\t", $rec->qname,
                         $rec->flag_as_chars,
                         $rec->rname,
                         $rec->pos,
                         $rec->cigar,
                         $rec->tag('MD'),
                         length($rec->seq) ), "\n";

        my $tseq = ReadReferenceConcordance->new( $rec );
        if ($VERBOSE > 1) {
            print  "  read-reference concordance:\n", $tseq->to_text(4);
        }

        # Skip any reads that didn't map
        next if ($rec->flag_as_chars =~ /u/);
      
        # Skip any short reads
        next unless (length( $rec->seq ) >= $tscore);

        # Work out if this read mapped forward or reverse
        my $maps_forward = ($rec->flag_as_chars =~ /r/) ? 0 : 1;

    }
}


    # Base codes:
    # M = matched base, 
    # I = inserted base (in read but nor in ref),
    # D = deleted base (in ref but not in read),
    # V = variant base (show ref at this pos)
    # For each base we will store:
    # read_pos = bases offset with read - undef for D bases
    # read_base = base called in read - undef for D bases
    # read_qual = qual called in read - undef for D bases
    # ref_base = reference base at position where this base aligns to
    #     the reference - undef for I, matches read_base for M,
    #     different for V
    #
    # This structure means that to get the read sequence we concatenate
    # the read_base values ignoring undefs and to get the reference
    # sequence we concatenate the ref_base values ignoring undefs.

    # To make the reference sequence:
    # 1. Take the read sequence
    # 2. From CIGAR, remove all bases identified as I
    # 3. From MD, add all bases identified as D
    # 4. From MD, flip all bases identified as mismatch
    # Practically, to do 2 and 3 above, it is probably best to reformat
    # the sequence as a series of regions each of which has a sequence,
    # and a type (I,D,M,S) (insert,delete,match,snp).  When traversing
    # this list for (3), you should not count the "I" regions as they do
    # not exist from the perspective of MD.


package ReadReferenceConcordance;

sub new {
    my $class = shift;
    my $sam   = shift;  # QCMG::IO::SamRecord

    # Parse the sequence into individual bases
    my @seq_as_bases = ();
    foreach my $i (0..(length($sam->seq)-1)) {
        push @seq_as_bases,
            { read_pos  => $i,
              read_base => substr($sam->seq,$i,1),
              read_qual => substr($sam->qual,$i,1),
              base_type => '',
              ref_base  => '' };
    }

    # Label each base as M, I or D according to CIGAR
    if ($sam->cigar and $sam->cigar !~ /\*/) {
        # Divide CIGAR string into individual operations
        my @tmpops = ($sam->cigar =~ /(\d+[A-Z]{1})/g);
        my @cigops = map { /(\d+)([A-Z]{1})/; [ $1, $2 ] }
                     @tmpops;

        # Apply cigops to the sequence
        my $seqctr = 0;
        foreach my $ra_op (@cigops) {

            # A match/mismatch
            if ($ra_op->[1] eq 'M') {
                foreach (1..$ra_op->[0]) {
                    $seq_as_bases[$seqctr++]->{base_type} = 'M';
                }
            }

            # An insertion
            elsif ($ra_op->[1] eq 'I') {
                foreach (1..$ra_op->[0]) {
                    $seq_as_bases[$seqctr++]->{base_type} = 'I';
                }
            }

            # A deletion - deleted bases do not appear in the read
            # sequence so we will insert empty place holders for them.
            elsif ($ra_op->[1] eq 'D') {
                # Insert as many deleted bases as specified in the cigop
                foreach (1..$ra_op->[0]) {
                    splice(@seq_as_bases,$seqctr++,0,
                           { read_pos  => undef,
                             read_base => '',
                             read_qual => '!',
                             base_type => 'D',
                             ref_base  => '' } );
                }
            }

        }   
    }

    # Now add the deleted and mutated bases from MD
    if (defined $sam->tag('MD') and $sam->tag('MD')) {
        # Divide up MD tag into individual operations
        my @mdops = ($sam->tag('MD') =~ /(\d+|[A-Z]+|\^[A-Z]+)/g);
        print '  mdops: ',join(',',@mdops),"\n";

        # Now turn MD ops into base-level MD ops
        my @base_mdops = ();
        foreach my $mdop (@mdops) {
            if ($mdop =~ /\d+/) {
                push @base_mdops, 'M' foreach (1..$mdop);
            }
            elsif ($mdop =~ /^[A-Z]+/) {
                my @snps = split //,$mdop;
                foreach my $snp (@snps) {
                    push @base_mdops, 'V' . $snp;
                }
            }
            elsif ($mdop =~ /^\^[A-Z]+/) {
                my @dels = split //,$mdop;
                shift @dels;  # drop leading '^' char
                foreach my $del (@dels) {
                    push @base_mdops, 'D' . $del;
                }
            }
            else {
                die "Unknown MD operation: $mdop";
            }
        }
        print '  base_mdops: ',join(',',@base_mdops),"\n";

        # Apply the base-level MD ops
        my $seqctr = 0;
        foreach my $mdop (@base_mdops) {
            # Skip any 'I's
            while ($seq_as_bases[$seqctr]->{base_type} eq 'I') {
                $seqctr++;
            }
            if ($mdop =~ /^M/) {
                $seq_as_bases[$seqctr]->{ref_base} =
                    $seq_as_bases[$seqctr]->{read_base};
            }
            elsif ($mdop =~ /^V([A-Z]{1})/) {
                $seq_as_bases[$seqctr]->{ref_base}  = $1;
                $seq_as_bases[$seqctr]->{base_type} = 'V';
            }
            elsif ($mdop =~ /^D([A-Z]{1})/) {
                $seq_as_bases[$seqctr]->{ref_base} = $1;
            }
            else {
                die "Unknown MD operation: $mdop";
            }
            $seqctr++
        }
    }

    my $self = { bases => \@seq_as_bases };
    bless $self, $class;
}


sub to_text {
    my $self   = shift;
    my $indent = shift || 0;

    my $prefix = ' 'x$indent;

    my $text = "${prefix}position in read: ";
    $text .= join(' ', map { substr('   '.$_,-3) }
                       map { defined $_->{read_pos} ? $_->{read_pos} : ' ' }
                       @{ $self->{bases} } );
    $text .= "\n${prefix}base type:        ";
    $text .= join(' ', map { substr('   '.$_,-3) }
                       map { defined $_->{base_type} ? $_->{base_type} : ' ' }
                       @{ $self->{bases} } );
    $text .= "\n${prefix}read base:        ";
    $text .= join(' ', map { substr('   '.$_,-3) } 
                       map { defined $_->{read_base} ? $_->{read_base} : ' ' }
                       @{ $self->{bases} } );
    $text .= "\n${prefix}read quality:     ";
    $text .= join(' ', map { substr('   '.$_,-3) }
                       map { defined $_->{read_qual} ? ord($_->{read_qual})-33 : ' ' }
                       @{ $self->{bases} } );
    $text .= "\n${prefix}reference base:   ";
    $text .= join(' ', map { substr('   '.$_,-3) }
                       map { defined $_->{ref_base} ? $_->{ref_base} : ' ' }
                       @{ $self->{bases} } );
    $text .= "\n";

    return $text;
}



__END__

=head1 NAME

qtrim.pl - Perl script for manipulating Ion Torrent FASTQ files


=head1 SYNOPSIS

 qtrim.pl


=head1 ABSTRACT

A pre-alignment tool to take FASTQ files written out by an Ion Torrent
PGM and convert bases below a given quality to N.  The original purpose
of this script was to test whether converting low quality bases to N
helped the tmap aligner produce alignments with fewer deletions and
insertions.  The thinking was that tmap might be inclined to open a
gap to accomodate a low quality base rather than calling it as a 
mismatch.


=head1 OPTIONS

 -i | --infile        coordinate sorted SAM or BAM file to be processed
 -s | --score         scores to be used as trimming thresholds
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qtrim.pl 4668 2014-07-24 10:18:42Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
