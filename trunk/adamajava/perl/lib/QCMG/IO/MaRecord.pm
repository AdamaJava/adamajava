package QCMG::IO::MaRecord;

###########################################################################
#
#  Module:   QCMG::IO::MaRecord
#  Creator:  John V Pearson
#  Created:  2010-02-12
#
#  Data container for a Bioscope .ma alignment file record.
#
#  $Id: MaRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: MaRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class   = shift;
    my $defline = shift;
    my $seq     = shift;
    
    chomp $defline;
    chomp $seq;

    my @fields = split /\,/, $defline;

    my $id = shift @fields;
    $id =~ s/^\>//;  # Discard leading '>'

    my $self = { id       => $id,
                 matches  => \@fields,
                 seq      => $seq };

    bless $self, $class;
}


sub id {
    my $self = shift;
    return $self->{id};
}

sub id_notag {
    my $self = shift;
    my $id = $self->id;
    # Try for an explicit pattern to drop only F3, R3, F5-BC, F5-P2
    $id =~ s/_[FRBCP235-]*$//; 
    return $id;
}

sub seq {
    my $self = shift;
    return $self->{seq};
}

sub matches {
    my $self = shift;
    return @{ $self->{matches} };
}

sub match_count {
    my $self = shift;
    return scalar $self->matches;
}


1;

__END__


=head1 NAME

QCMG::IO::MaRecord - Bioscope .ma alignment record data container


=head1 SYNOPSIS

 use QCMG::IO::MaRecord;


=head1 DESCRIPTION

This module provides a data container for a Bioscope .ma alignment file
record.  A .ma record is basicaly a FASTA record with all of the
alignments appended to the defline separated by commas.  A example block
of 3 records is shown below:

 >2_52_1120_F3
 T0002001101010010000301203001030020300.010013130131
 >2_52_1143_F3,12_-27912272.2:(27.2.0):q10,5_-11653883.2:(24.2.0):q0
 T3002002000000200010100010022000021001.000011000201
 >2_52_1196_F3,8_-14538482.2:(25.2.0):q2,11_-64039799.2:(24.2.0):q1,\
 15988598.2:(24.2.0):q1,17_-70021779.2:(24.2.0):q1
 T0000000001200012220000202201000220222.020023110031

Note that the first record has no alignments, the second has 2 and the
third has 4.

=head1 PUBLIC METHODS

=over

=item B<new()>

 my $rec = QCMG::IO::MaRecord->new( $defline, $seq );

=item B<id()>

 my $id = $rec->id();

Returns the ID field.  Note that because this is a .ma record, the IDs
still have the tag suffix - _F3, _R3, _F5-BC etc.  If you wish to get
the ID without the tag suffix, use method B<id_notag()> instead.

=item B<id_notag()>

 my $id = $rec->id_notag();

Returns the ID field but with the tag suffix removed.  This is useful
whan matching against a BAM or other downstream file where the tag
suffixes have been dropped.

=item B<matches()>

 my @matches = $rec->matches();

Returns the alignments as an array.

=item B<match_count()>

 my $count = $rec->match_count();

Returns the number of alignments.

=item B<seq()>

 my $seq = $rec->seq();

Returns the sequence.  This is almost certainly color-space, not
base-space.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: MaRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
