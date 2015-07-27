package QCMG::QInspect::SamReferenceConcordance;

###########################################################################
#
#  Module:   QCMG::QInspect::SamReferenceConcordance.pm
#  Creator:  John V Pearson
#  Created:  2012-06-10
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak confess );
use Data::Dumper;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    croak "new() must be passed a sam parameter"
        unless (exists $params{sam} and defined $params{sam});

    croak 'new() must be passed a single QCMG::IO::SamRecord object '.
          'and you passed a ['. ref($params{sam}) .']'
        unless (ref($params{sam}) eq 'QCMG::IO::SamRecord');

    my $self = { sam     => $params{sam},
                 verbose => $params{verbose} || 0 };
    bless $self, $class;

    $self->_initialise();

    return $self;
}


sub sam {
    my $self = shift;
    return $self->{sam};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
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

sub _initialise {
    my $self = shift;

    my $sam = $self->sam();  # QCMG::IO::SamRecord

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
        my @cigops = @{ $sam->cigops() };

        # Useful for debugging!
        $self->{cigops} = \@cigops;

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

            # Soft clipped bases - hard clips don't even exist in the
            # read so we should be able to ignore these
            elsif ($ra_op->[1] eq 'S') {
                foreach (1..$ra_op->[0]) {
                    $seq_as_bases[$seqctr++]->{base_type} = 'S';
                }
            }

        }   
    }

    # Now add the deleted and mutated bases from MD
    if (defined $sam->tag('MD') and $sam->tag('MD')) {
        # Divide up MD tag into individual operations
        my @mdops = ($sam->tag('MD') =~ /(\d+|[A-Z]+|\^[A-Z]+)/g);
        #print '  mdops: ',join(',',@mdops),'   MD: ',$sam->tag('MD'),
        #      '   CIGAR: ', $sam->cigar, "\n";

        # Useful for debugging!
        $self->{mdops} = \@mdops;

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
        #print '  base_mdops: ',join(',',@base_mdops),"\n";

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

    $self->{bases} = \@seq_as_bases;
}


sub extents {
    my $self = shift;

    my $ctr = -1;
    my $in_extent = 0;
    my @extents = ();
    my $extent = undef;
    foreach my $rec (@{$self->{bases}}) {
        $ctr++;
        if ($rec->{base_type} =~ /[VMS]{1}/) {
            next if $in_extent;
            $extent = [ $ctr, undef ];
            $in_extent = 1;
        }
        elsif ($rec->{base_type} =~ /I/) {
            $ctr--;  # I's dont exist from a reference POV
        }
        elsif ($rec->{base_type} =~ /D/) {
            if ($in_extent) {
                $extent->[1] = $ctr-1;
                push @extents, $extent;
                $in_extent = 0;
            }
        }
        else {
            confess 'Unexpected base_type value: ',$rec->{base_type};
        }
    }
    if (defined $extent) {
        $extent->[1] = $ctr;
        push @extents, $extent;
    }

    return \@extents;
}


sub variants {
    my $self = shift;

    my $ctr = -1;
    my @variants = ();
    foreach my $rec (@{$self->{bases}}) {
        $ctr++;
        if ($rec->{base_type} =~ /V/) {
            push @variants, [ $ctr, $rec->{read_base} ];
        }
        elsif ($rec->{base_type} =~ /I/) {
            $ctr--;  # I's dont exist from a reference POV
        }
    }
    return \@variants;
}


sub inserts {
    my $self = shift;
    
    # We only want to return the location of the first inserted base so
    # we need some "in insert" logic.  We want to draw at "ctr" because
    # that is the coordinate offset to cope with deletions.

    my $ctr = -1;
    my @inserts = ();
    my $in_insert = 0;
    foreach my $rec (@{$self->{bases}}) {
        $ctr++;
        if ($rec->{base_type} =~ /I/) {
            if (! $in_insert) {
                $ctr--;  # I's dont exist from a reference POV
                push @inserts, [ $ctr, $rec->{read_pos} ];
                $in_insert = 1;
            }
        }
        else {
            $in_insert = 0;
        }
    }

    return \@inserts;
}


sub to_text {
    my $self   = shift;
    my $indent = shift || 0;

    my $prefix = ' 'x$indent;

    my $text = "${prefix}values from SAM:   ".
               'MD: '. $self->sam->tag('MD') .
               '  CIGAR: '.  $self->sam->cigar ."\n";
    $text .= "${prefix}position in read: ";
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


1;
 
__END__


=head1 NAME

QCMG::QInspect::SamReferenceConcordance - SAM record aligned to reference


=head1 SYNOPSIS

 use QCMG::QInspect::SamReferenceConcordance;


=head1 DESCRIPTION

This module analyses the sequence, CIGAR and MD tags from a SAM record
to provide a detailed picture of how the read aligns against the
reference sequence.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $path = QCMG::QInspect::SamReferenceConcordance->new( sam => $sam );

This method creates a new SamReferenceConcordance object and initialises
the analysis of CIGAR and MD tags.

=item B<to_text()>

Takes a single integer parameter which gives the indent space count.

=item B<extents()>

Analyses a read based on CIGAR and MD and returns an array of
[start,stop] arrays that denoted the blocks that would need to be
drawn against the reference to show this read.  The coords are 0-based
where the 0 is at $sam->pos.  For example:

 MD: 32  CIGAR: 28M3I4M                => [ [0,31 ] ];
 MD: 3^GCACTTAA1A0C44  CIGAR: 3M8D47M  => [ [0,2], [11,57] ];
 MD: 0T4^CT4C40  CIGAR: 5M2D45M        => [ [0,4], [7,51] ];
 MD: 39C0A6T0  CIGAR: 40M2I8M          => [ [0,47] ]; 

Returns an arrayref.

=item B<variants()>

Analyses a read based on CIGAR and MD and returns an array of
[pos,base] arrays that denoted the positions where there is avariant
and the varant base. as with extents(), the coords are 0-based
where the 0 is at $sam->pos.  For example:

 MD: 39C0A6T0  CIGAR: 40M2I8M          =>  [ [39,'A'], [40,'C'], [47,'N'] ]
 MD: 4^CAC46  CIGAR: 4M3D46M           =>  [ ]
 MD: 31C0  CIGAR: 25M3I7M              =>  [ [31,'N'] ]
 MD: 3^GCACTTAA1A0C44  CIGAR: 3M8D47M  =>  [ [12,'T'], [13,'G'] ];

Returns an arrayref.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
