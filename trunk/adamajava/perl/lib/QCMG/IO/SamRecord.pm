package QCMG::IO::SamRecord;

###########################################################################
#
#  Module:   QCMG::IO::SamRecord
#  Creator:  John V Pearson
#  Created:  2010-02-12
#
#  Data container for a SAM v0.1.2 record.
#
#  $Id: SamRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Memoize;
use POSIX;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: SamRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = '';

    # Try to make new record from gff or sam or else return an empty
    # record.  If a gff or sam parameter was passed in but was not
    # defined or was empty, then return undef.

    if (exists $params{gff}) {
        return undef unless defined $params{gff} and $params{gff};
        $self = _from_gff( $params{gff} );
    }
    elsif (exists $params{sam}) {
        return undef unless defined $params{sam} and $params{sam};
        $self = _from_sam( $params{sam} );
    }
    else {
        $self = _empty_record();
    }

    bless $self, $class;
}


sub _empty_record {
    return { qname   => '',
             flag    => 0,
             _flag_as_bits  => '',
             _flag_as_chars => '',
             rname   => '',
             pos     => 0,
             mapq    => 255,
             cigar   => '',
             mrnm    => '',
             mpos    => 0,
             isize   => 0,
             seq     => '',
             qual    => '',
             tags    => {} };


    # In some cases, you may want to work with flag in binary:
    # flag => unpack("S", pack("B*",'0'x16)),  # 16 bits zerod
}


sub qname {
    my $self = shift;
    return $self->{qname} = shift if @_;
    return $self->{qname};
}


sub flag {
    my $self = shift;
    if (@_) {
        $self->{flag} = shift;
        # On any change to FLAG we need to refresh the bits and chars
        my ($zb,$zf) = _int_to_bit_string( $self->{flag} ); 
        $self->{_flag_as_bits}  = $zb;
        $self->{_flag_as_chars} = $zf;
    }
    return $self->{flag};
}

sub flag_as_bits {
    my $self = shift;
    return $self->{_flag_as_bits};
}

sub flag_as_chars {
    my $self = shift;
    return $self->{_flag_as_chars};
}


sub rname {
    my $self = shift;
    return $self->{rname} = shift if @_;
    return $self->{rname};
}

sub pos {
    my $self = shift;
    return $self->{pos} = shift if @_;
    return $self->{pos};
}

sub mapq {
    my $self = shift;
    return $self->{mapq} = shift if @_;
    return $self->{mapq};
}

sub cigar {
    my $self = shift;
    return $self->{cigar} = shift if @_;
    return $self->{cigar};
}

sub mrnm {
    my $self = shift;
    return $self->{mrnm} = shift if @_;
    return $self->{mrnm};
}

sub mpos {
    my $self = shift;
    return $self->{mpos} = shift if @_;
    return $self->{mpos};
}

sub isize {
    my $self = shift;
    return $self->{isize} = shift if @_;
    return $self->{isize};
}

sub seq {
    my $self = shift;
    return $self->{seq} = shift if @_;
    return $self->{seq};
}

sub qual {
    my $self = shift;
    return $self->{qual} = shift if @_;
    return $self->{qual};
}

sub tag {
    my $self = shift;
    my $type = shift;
    if (@_) {
        return $self->{tags}->{$type} = shift;
    }
    else {
        if (exists  $self->{tags}->{$type} and
            defined $self->{tags}->{$type}) {
            return $self->{tags}->{$type};
        }
        else {
            return '';
        }
    }
}


sub tags {
    my $self = shift;
    return $self->{tags};
}


sub tags_as_array {
    my $self = shift;
    return values %{ $self->{tags} };
}


sub cigops {
    my $self = shift;
    # Breaks CIGAR down into an array of 2-item arrays where each 2-item
    # array is a length and an operator, e.g. [25,M], [8,H], [2,D] etc.
    if ($self->cigar and $self->cigar !~ /\*/) {
        # Divide CIGAR string into individual operations
        my @tmpops = ($self->cigar =~ /(\d+[A-Z]{1})/g);
        my @cigops = map { /(\d+)([A-Z]{1})/; [ $1, $2 ] }
                     @tmpops;
        return \@cigops;
    }
    return undef;
}


sub start_with_clips {
    my $self = shift;
    # This routine uses the CIGAR string to work out where the record
    # would have started if it wasn't clipped
    my $start = $self->pos;

    my $ra_cigops = $self->cigops();
    if (defined $ra_cigops) {
        # On torrent we often see both hard and soft clips on the same
        # read at the same end so we need to loop here
        foreach my $op (@{ $ra_cigops }) {
            if ($op->[1] eq 'S' or $op->[1] eq 'H') {
                $start -= $op->[0]
            }
            else {
                # As soon as we hit the first non-clip operator we exit
                last;
            }
        }
    }

    return $start;
}


sub end_with_clips {
    my $self = shift;
    # This routine uses the CIGAR string to work out where the record
    # would have ended (in ref coords) if it wasn't clipped
    my $end = $self->pos() + $self->length_on_ref_from_cigar() -1;

    my $ra_cigops = $self->cigops();
    if (defined $ra_cigops) {
        # On torrent we often see both hard and soft clips on the same
        # read at the same end so we need to loop here and we'll use
        # reverse so we are effectively reading back from the end.
        foreach my $op (reverse @{ $ra_cigops }) {
            if ($op->[1] eq 'S' or $op->[1] eq 'H') {
                $end += $op->[0]
            }
            else {
                # As soon as we hit the first non-clip operator we exit
                last;
            }
        }
    }

    return $end;
}


sub length_on_ref_from_cigar {
    my $self = shift;
    # This routine interprets the CIGAR string to work out how many
    # reference bases the read sits against.  This has to ignore
    # inserts, and clips but include deletes and mismatches
    my $len = 0;

    my $ra_cigops = $self->cigops();
    if (defined $ra_cigops) {
        # On torrent we often see both hard and soft clips on the same
        # read at the same end so we need to loop here
        foreach my $op (@{ $ra_cigops }) {
            if ($op->[1] eq 'S' or $op->[1] eq 'H' or $op->[1] eq 'I') {
                # do nothing
            }
            elsif ($op->[1] eq 'M' or $op->[1] eq 'D') {
                $len += $op->[0];
            }
            else {
                die "length_on_ref_from_cigar() cant cope with op ", $op->[1];
            }
        }
    }

    return $len;
}



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


sub _from_gff {
    my $gff  = shift;

    my $rec = _empty_record();

    # Process ID including removal of trailing _F3 / _R3 from IDs
    my $qname = $gff->seqname;
    $qname =~ s/_[FR]3$//;
    $rec->{qname} = $qname;

    # Set appropriate bits in flag
    my $flag = pack("S", $rec->{flag});
    vec( $flag, 0, 1 ) = 1 if ($gff->matepair);      # mate pair library
    vec( $flag, 4, 1 ) = 1 if ($gff->strand eq '-'); # match on - strand
    vec( $flag, 6, 1 ) = 1;                          # first read
    $rec->{flag} = unpack("S",$flag);

    # In the GFF sequences are only identified by number
    $rec->{rname} = 'seq_' . $gff->attrib('i');

    my $bquals = $gff->color_quality_to_base_quality;
    if ($gff->strand eq '-') {
       my @bquals = reverse split( //, $bquals );
       $bquals = join( '', @bquals );
    }
    $rec->{qual} = $bquals;

    $rec->{pos} = $gff->start;
    $rec->{cigar} = length( $gff->attrib('b') ) .'M';

    # Set sequence and work out MD descriptor as follows:
    # 1. $self->seq = AB "believed" sequence (GFF b= attrib), revcomp if
    #    match is on - strand
    # 2. To get reference, fix original color based on r= attrib,
    #    convert to base space and revcomp if match is on - strand
    # 3. Compare sequences from 1. and 2. and code delta in MD format

    if ($gff->strand eq '-') {
        $rec->{seq} = uc( $gff->revcomp( $gff->attrib('b') ) );
        my $refr_seq = $gff->revcomp( $gff->color_fixed_to_base );
        $rec->{tag}->{'MD'} = 'MD:Z:'. _MD( $rec->{seq}, $refr_seq );
    }
    else {
        $rec->{seq} = uc( $gff->attrib('b') );
        my $refr_seq = $gff->color_fixed_to_base;
        $rec->{tag}->{'MD'} = 'MD:Z:'. _MD( $rec->{seq}, $refr_seq );
    }

    # Add 33 to all qualities and ASCII-ize
    my @cquals = map { chr(33+$_) }
                 split(',',$gff->attrib('q'));

    $rec->{tag}->{'AS'} = 'AS:i:' . int($gff->score);
    $rec->{tag}->{'CQ'} = 'CQ:Z:' . join('',@cquals);

    # The color string needs some work because the SAM format wants the
    # primer base (T for F3, G for R3) plus the full 35 colors but the gff
    # "g" attribute is the first base of the sequence plus 34 colors so
    # we need to recover the first color before converting to SAM.

    my @colors = split //, $gff->attrib('g');
    my $base1 = ( $gff->seqname =~ /F3$/ ) ? 'T' : 'G';
    my $base2 = shift @colors;
    unshift @colors, $base1, _bb2c( $base1, $base2 );
    $rec->{tag}->{'CS'} = 'CS:Z:' . join('',@colors);

    return $rec;
}


sub _MD {
    my $seq  = shift;
    my $ref  = shift;

    my $md_str = '';
    my $ctr = 0;

    # Do a base-by-base compare on the read and reference sequences

    foreach my $i (0..(length($seq)-1)) {
        if (uc(substr( $seq, $i, 1 )) eq uc(substr( $ref, $i, 1 ))) {
            $ctr++;
        }
        else {
            if ($ctr > 0) {
                $md_str .= $ctr;
                $ctr = 0;
            }
            $md_str .= substr( $ref, $i, 1 );
        }
    }
    if ($ctr > 0) {
        $md_str .= $ctr;
    }

    return $md_str;
}


sub _from_sam {
    my $sam  = shift;

    my $rec = _empty_record();

    return $rec unless defined $sam and $sam;

    chomp $sam;  # Just in case there's a newline
    my @fields = split /\t/, $sam, 12;

    $rec->{qname} = $fields[0];
    $rec->{flag}  = $fields[1];
    $rec->{rname} = $fields[2];
    $rec->{pos}   = $fields[3];
    $rec->{mapq}  = $fields[4];
    $rec->{cigar} = $fields[5];
    $rec->{mrnm}  = $fields[6];
    $rec->{mpos}  = $fields[7];
    $rec->{isize} = $fields[8];
    $rec->{seq}   = $fields[9];
    $rec->{qual}  = $fields[10];

    # Turn the tags into a hash but only if there are any
    if ($fields[11]) {
        my @tags = split /\t/, $fields[11];
        foreach my $tag (@tags) {
            if ($tag =~ /(\w{2}):\w:(.*)/) {
                $rec->{tags}->{$1} = $2;
            }
            else {
                die "tag [$tag] in record $fields[0] can not be parsed";
            }
        }
    }

    # Setup alternate FLAG representations
    my ($zb,$zf) = _int_to_bit_string( $fields[1] ); 
    $rec->{_flag_as_bits}  = $zb;
    $rec->{_flag_as_chars} = $zf;
        
    return $rec; 
}


sub _int_to_bit_string {
    my $int = shift;

    # Special case
    return ( '0000000000000000', '' ) if ($int == 0);

    my $zb = '';
    foreach my $powerof2 (32768,16384,8192,4096,2048,1024,512,256,128,64,32,16,8,4,2,1) {
        my $this_bit = floor( $int / $powerof2 );
        if ($this_bit > 0) {
            $zb .= '1';
            $int -= $powerof2;
        }
        else {
            $zb .= '0';
        }
    }
    
    # Because of the way the bits are arranged in the FLAG we see:
    # substr($zb,0,1)  = unused
    # substr($zb,1,1)  = unused
    # substr($zb,2,1)  = unused
    # substr($zb,3,1)  = unused
    # substr($zb,4,1)  = unused
    # substr($zb,5,1)  = read is PCR or optical duplicate
    # substr($zb,6,1)  = read fails platform/vendor checks
    # substr($zb,7,1)  = alignment is not primary (???)
    # substr($zb,8,1)  = read is second in pair
    # substr($zb,9,1)  = read is first in pair
    # substr($zb,10,1) = mate strand
    # substr($zb,11,1) = read strand (0=forward)
    # substr($zb,12,1) = mate is unmapped
    # substr($zb,13,1) = read is unmapped
    # substr($zb,14,1) = read is in a proper pair
    # substr($zb,15,1) = paired in sequencing

    my $zf = '';
    my $zf_mask = '     dfs21RrUuPp';
    foreach (0..15) {
        if (substr($zb,15-$_,1) eq '1') {
            $zf .= substr($zf_mask,15-$_,1);
        }
    }
    return ($zb,$zf);
}


sub as_text {
    my $self = shift;

    return join( "\t", $self->qname,
                       $self->flag,
                       $self->rname,
                       $self->pos,
                       $self->mapq,
                       $self->cigar,
                       $self->mrnm,
                       $self->mpos,
                       $self->isize,
                       $self->seq,
                       $self->qual,
                       $self->tags_as_array )."\n";
}

1;
 
__END__


=head1 NAME

QCMG::IO::SamRecord - SAM record data container


=head1 SYNOPSIS

 use QCMG::IO::SamRecord;


=head1 DESCRIPTION

This module provides a data container for a SAM/BAM alignment record.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $srec = QCMG::IO::SamRecord->new( sam => $record );

This method creates a new SamRecord object from a text SAM record.

=item B<qname()>

 my $qname = $srec->qname;

QNAME field - query name or read ID.

=item B<flag()>

 my $flag = $srec->flag;

FLAG field as an integer.  This can be hard to understand so there are 2
alternative representations available as B<flag_as_bits()> and
B<flag_as_chars()>.

=item B<rname()>

=item B<pos()>

=item B<mapq()>

=item B<cigar()>

=item B<mrnm()>

=item B<mpos()>

=item B<isize()>

=item B<seq()>

=item B<qual()>


=item B<flag_as_bits()>

 my $bit_string = $srec->flag_as_bits;

Returns the FLAG field as a string of 16 characters where each is a 0 or
a 1 representing wherther the corresponding bit is set (1) or unset (0).
Note that the 0'th bit is the rightmost so when comparing this string
to the BAM spec, you should (counterintuitively) read the string 
from right-to-left.

=item B<flag_as_chars()>

 my $flag_chars = $srec->flag_as_chars;

Returns the FLAG field as a string of ASCII characters where each char
represent s a particular bit being set, for example B<p> means the read
is part of a paired run and a B<2> means the read is the first in a
pair.  For more details on this representation see the QCMG wiki page
for BAM.

=item B<length_on_ref_from_cigar()>

Just because a read is 50 bases, it doesn't mean that the read aligns
against 50 bases on the reference.  This is particularly important if
you are trying to draw the read so you will need to know how many
reference bases it aligns against to get the layout correct.
If the read contains an insert it will align to less than 50 bases
because inserts can't be seen and if the read spans a deletion, it will
align against more than 50 bases.

=item B<start_with_clips()>

This routine uses the CIGAR string to work out where the record would
have started if it wasn't clipped.  If the read is not clipped, this is
equivalent to B<pos()> but for clipped reads, it will be an earlier
position.

=item B<end_with_clips()>

Same idea as B<start_with_clips()> but at the other end of the read.  It
makes use of B<length_on_ref_from_cigar()> to work out where the read
would end on the reference without accounting for the clips and then it
adds back in the clipped bases.

=item B<cigops()>

This routines breaks the CIGAR string down into an array of 2-item 
arrays where each 2-item array is a length and an operator (M, H, I, D
etc).  For example, the cigar string 5H25M1D20M would be broken down to:

  [ [5,H], [25,M], [1,D], [20,M] ]

This is particularly useful when you are trying to work out how the
bases in a read map onto the reference for coverage or visualisation.

=back

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: SamRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
