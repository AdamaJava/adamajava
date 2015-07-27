package QCMG::IO::GffRecord;

###########################################################################
#
#  Module:   QCMG::IO::GffRecord
#  Creator:  John V Pearson
#  Created:  2010-02-12
#
#  Data container for a GFF v2 record.  Loosely based on an NHGRI
#  module.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my $line  = shift;
    
    chomp $line;
    my @fields = split "\t", $line;
    warn 'Saw ', scalar(@fields), " fields, should have been 9 [$line]\n"
        if (scalar(@fields) < 9);

    # Parse out attributes field because we are going to need this info
    my @attributes = split(';\s?',$fields[8]);
    my %attributes = ();

    # Try to spot GTF and GFF2 files which are both very similar to GFF3
    # but differ in how they handle field 9.
    # GFF3: field 9 is of the form [<name>=<value>][;<name>=<value>]*
    # GTF:  field 9 is of the form [<name> <value>][; <name> <value>]*
    # GFF2: field 9 is of the form [<name> <value>][; <name> <value>]*

    if ($attributes[0] =~ /=/) {
        foreach (@attributes) {
            my ($tag,$value) = split '=', $_;
            $attributes{ $tag } = $value; 
        }
    }
    else {
        foreach (@attributes) {
            my ($tag,$value) = split ' ', $_, 2;
            $attributes{ $tag } = $value; 
        }
    }

    my $self = { seqname  => $fields[0],
                 source   => $fields[1],
                 feature  => $fields[2],
                 start    => $fields[3],
                 end      => $fields[4],
                 score    => $fields[5],
                 strand   => $fields[6],
                 frame    => $fields[7],   
                 attribs  => \%attributes,
                 matepair => 0 };

    bless $self, $class;
}


sub seqname {
    my $self = shift;
    return $self->{seqname} = shift if @_;
    return $self->{seqname};
}

sub source {
    my $self = shift;
    return $self->{source} = shift if @_;
    return $self->{source};
}

sub feature {
    my $self = shift;
    return $self->{feature} = shift if @_;
    return $self->{feature};
}

sub start {
    my $self = shift;
    return $self->{start} = shift if @_;
    return $self->{start};
}

sub end {
    my $self = shift;
    return $self->{end} = shift if @_;
    return $self->{end};
}

sub score {
    my $self = shift;
    return $self->{score} = shift if @_;
    return $self->{score};
}

sub strand {
    my $self = shift;
    return $self->{strand} = shift if @_;
    return $self->{strand};
}

sub frame {
    my $self = shift;
    return $self->{frame} = shift if @_;
    return $self->{frame};
}

sub attrib {
    my $self = shift;
    my $type = shift;

    if (@_) {
        return $self->{attribs}->{$type} = shift;
    }
    else {
        # Check for existence;
        if (exists $self->{attribs}->{$type}) {
            return $self->{attribs}->{$type};
        }
        else {
            return undef;
        }
    }
}

sub matepair {
    my $self = shift;
    return $self->{matepair} = shift if @_;
    return $self->{matepair};
}


sub as_gff3 {
    my $self = shift;
    my $text = join("\t", $self->seqname,
                          $self->source,
                          $self->feature,
                          $self->start,
                          $self->end,
                          $self->score,
                          $self->strand,
                          $self->frame,
                          join('; ', map({ $_ .'='. $self->{attribs}->{$_} }
                                     keys %{$self->{attribs}} ) )
                          );

    return $text;
}




# This routine is not OO and it's being memoized for extra speed
sub _cb2b {
    my $base = uc( shift );
    my $col  = shift;
    my %transform = ( A => { 0 => 'A', 1 => 'C', 2 => 'G', 3 => 'T' },
                      C => { 1 => 'A', 0 => 'C', 3 => 'G', 2 => 'T' },
                      G => { 2 => 'A', 3 => 'C', 0 => 'G', 1 => 'T' },
                      T => { 3 => 'A', 2 => 'C', 1 => 'G', 0 => 'T' } );

    return $transform{$base}->{$col};
}
memoize('_cb2b');

# This routine is not OO and it's being memoized for extra speed
sub _bb2c {
    my $base1 = uc( shift );
    my $base2 = uc( shift );
    my %transform = ( A => { A => '0', C => '1', G => '2', T => '3' },
                      C => { A => '1', C => '0', G => '3', T => '2' },
                      G => { A => '2', C => '3', G => '0', T => '1' },
                      T => { A => '3', C => '2', G => '1', T => '0' } );

    return $transform{$base1}->{$base2};
}
memoize('_bb2c');


sub c2b_seq {
    my $self = shift;
    my $cstr = shift;

    return undef unless defined $cstr and $cstr;

    my @colors = split //, $cstr;
    my $base   = shift @colors;
    my $bases  = '';
    foreach my $color (@colors) {
        $bases .= $base;
        $base = _cb2b( $base, $color );
    }
    $bases .= $base;

    return $bases;
}


sub revcomp {
    my $self = shift;
    my $seq = shift;
    my $revc = join('', reverse( split(//,$seq) ));
    $revc =~ tr/ACGTacgt/TGCAtgca/;
    return $revc;
}


sub color_original {
    my $self = shift;
    return $self->attrib('g');
}


sub color_original_to_base {
    my $self = shift;
    return $self->c2b_seq( $self->color_original );
}


sub color_fixed {
    my $self = shift;

    return $self->attrib('g') unless $self->attrib('r');

    my @cols = split //, $self->attrib('g');
    my @fixes = split /\,/, $self->attrib('r');
    foreach my $fix (@fixes) {
        my ($loc,$col) = split /_/, $fix;
        $cols[$loc-1] = $col;
    }
    return join('',@cols);
}


sub color_fixed_to_base {
    my $self = shift;
    return $self->c2b_seq( $self->color_fixed );
}


sub _min {
    my @vals = @_;
    my $min = $vals[0];
    foreach my $val (@vals) {
        next unless defined $val;  # beyond limits of color quality array
        $min = $val if $val < $min; 
    }
    return $min;
}

sub _max {
    my @vals = @_;
    my $max = $vals[0];
    foreach my $val (@vals) {
        next unless defined $val;  # beyond limits of color quality array
        $max = $val if $val > $max; 
    }
    return $max;
}


sub color_quality_to_base_quality {
    my $self = shift;

    # This is a pain-in-the-ass.  We need to take the data from GFF
    # attibutes "g", "s" and "q" and apply the rules outlined in the
    # SOLiD_QC_to_BC.pdf document in order to calculate base qualities.

    my @colors = split //,   $self->attrib('g');
    my @qc     = split /\,/, $self->attrib('q');
    my @tmps   = ();
    if (defined $self->attrib('s')) {
        @tmps  = split /\,/, $self->attrib('s');
    }

    my @s      = map { ' ' } @colors;  # initialize to all blanks
    foreach my $tmp (@tmps) {
       my $type = substr( $tmp, 0 ,1 );
       my $locn = substr( $tmp, 1 );
       $s[ $locn ] = $type;
    }

    # First thing we need to do is recover the first color which
    # annoyingly is not supplied in the "g" attribute.  The primer base
    # and the first color are used to reduce the first color to a base
    # but luckily the full list of color qualities is given in "q" so we
    # just have to work out whether the read is F3 or R3 and based on
    # the primer base (T/G) we can work out what the color must have
    # been to give us the base shown at the start of "g".

    my $col1 = shift @colors;
    my $pcol = ( $self->seqname =~ /F3$/ ) ? 'T' : 'G';
    unshift @colors, _bb2c( $pcol, $col1 );

    # Apply the rules from the SOLiD_QC_to_BC.pdf document.

    # Default case: set base qualities assuming everything is normal.
    # Note that this formula requires the i+1 color quality for each
    # base quality which obviously does not exist for the final base so
    # to avoid warnings we need to treat the last base as a sepcial case.
    my @qtmp = ();
    foreach my $i (0..($#colors-1)) {
        $qtmp[ $i ] = $qc[ $i ] + $qc[ $i+1 ];
    }
    $qtmp[ $#colors ] = $qc[ $#colors ];

    my @qb = @qtmp;
    {
        # This next loop has almost limitless ways to reach beyond the
        # limits of the actual array of colors so in almost every case
        # it throws enough warnings to drown a fish.  I have no idea how
        # to modify the ABI rules in cases where the rules as stated
        # reach beyond the bounds of the actual colors so I will do the
        # dumbest but most pragmatic thing - disable warnings! 

        no warnings;

        foreach my $i (0..$#colors) {
       
            # Case 1: (grAy) isolated mismatch (sequencing error) 
            if ($s[$i] eq 'a') {
               $qb[ $i-1 ] = _min( $qc[ $i-1 ], $qc[ $i ] );
               $qb[ $i ]   = _min( $qc[ $i ], $qc[ $i+1 ] );
            }

            # Case 2: (g: Green) valid adjacent mismatch (one base change)
            elsif ($s[$i] eq 'g' and $s[$i+1] eq 'g') {
               $qb[ $i-1 ] = $qc[ $i-1 ] + $qc[ $i ];
               $qb[ $i ]   = $qc[ $i ]   + $qc[ $i+1 ];
               $qb[ $i+1 ] = $qc[ $i+1 ] + $qc[ $i+2 ];
            }

            # Case 3: (y: Yellow) call consistent with isolated two-base change
            elsif ($s[$i-1] eq 'y' and $s[$i] eq 'y' and $s[$i+1] eq 'y') {
               $qb[ $i-2 ] = _min( $qc[ $i-2 ], $qc[ $i-1 ], $qc[ $i ] );
               $qb[ $i-1 ] = $qb[ $i-2 ];
               $qb[ $i ]   = _min( $qc[ $i-1 ], $qc[ $i ], $qc[ $i+1 ] );
               $qb[ $i+1 ] = $qb[ $i ];
            }

            # Case 4: (r: Red)
            # color call consistent with isolated three-base change
            elsif ($s[$i-1] eq 'r' and $s[$i] eq 'r' and
                   $s[$i+1] eq 'r' and $s[$i+2] eq 'r' ) {
               $qb[ $i-1 ] = _min( $qc[ $i-2 ], $qc[ $i-1 ], $qc[ $i ],
                                   $qb[ $i+1 ], $qc[ $i+2 ], $qc[ $i+3 ] );
               $qb[ $i ]   = $qb[ $i-1 ];
               $qb[ $i+1 ] = $qb[ $i-1 ];
               $qb[ $i+2 ] = $qb[ $i-1 ];
               $qb[ $i+3 ] = $qb[ $i-1 ];
            }

            # Case 5: (b = Blue) invalid adjacent mismatch (other mismatches)
            elsif ($s[$i] eq 'b') {
               $qb[ $i-1 ] = 1;
               $qb[ $i ]   = 1;
            }
        }

    }  # warnings should be on again after this scope is closed out
    
    # Add 33 to all qualities and ASCII-ize (ceiling at 64)
    my @final_quals = map { ($_ > 30) ? '@' : chr(33+$_) } @qb;

#    print '  colors: ', join( "   ", @colors ), "\n",
#          '  qc:     ', join( "  ",  @qc ), "\n",
#          '  s:      ', join( "   ", @s ), "\n",
#          '  qtmp:   ', join( "  ",  @qtmp ), "\n",
#          '  qb:     ', join( "  ",  @qb ), "\n",
#          '  ascii:  ', join( "   ", @final_quals ), "\n\n";

    return join('',@final_quals);
}


sub debug {
    my $self = shift;

    my $output = $self->color_original .
                 "\toriginal color space\n" .
                 $self->color_original_to_base .
                 "\tbase space from original color\n" .
                 $self->attrib('b') .
                 "\tAB base space\n";
    if ($self->strand eq '-') {
        $output .= $self->revcomp($self->color_original_to_base) .
                   "\trevcomp base space from original color space\n";
        $output .= $self->revcomp($self->attrib('b')) .
                   "\trevcomp AB base space\n";
    }

    if ($self->attrib('r')) {
        $output .= $self->color_fixed .
                   "\tfixed color space [" . $self->attrib('r') . "]\n" .
                   $self->color_fixed_to_base .
                   "\tbase space from fixed color space\n";
        if ($self->strand eq '-') {
        $output .= $self->revcomp($self->color_fixed_to_base) .
                   "\trevcomp base space from fixed color space\n";
        }
    }

    return $self->seqname .
           "  [" . $self->attrib('i') .':'.
                   $self->start . $self->strand . "]\n" .
           $output . "\n";
}

1;

__END__


=head1 NAME

QCMG::IO::GffRecord - GFF v2 Record data container


=head1 SYNOPSIS

 use QCMG::IO::GffRecord;


=head1 DESCRIPTION

This module provides a data container for a GFF Record.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010-2014

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
