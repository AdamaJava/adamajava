package QCMG::Bioscope::MatchFileRecord;

###########################################################################
#
#  Module:   QCMG::Bioscope:MatchFileRecord
#  Creator:  John V Pearson
#  Created:  2010-03-10
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;

sub new {
    my $class = shift;
    my %params = @_;

    my $self  = { defline  => $params{defline},
                  sequence => $params{sequence},
                  verbose  => ($params{verbose} ? $params{verbose} : 0),
                };

    bless $self, $class;
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub defline {
    my $self = shift;
    return $self->{defline};
}

sub sequence {
    my $self = shift;
    return $self->{sequence};
}

1;
__END__


=head1 NAME

QCMG::Bioscope::MatchFileRecord - Bioscope match file record


=head1 SYNOPSIS

 use QCMG::Bioscope::MatchFileRecord;

 my $rec = QCMG::Bioscope::MatchFileRecord->new(
               defline => '>seq1 my little sequence',
               seq     => 'ACGCATCAGCATCAGACTACGCGCATACAGC',
               verbose => 1 );


=head1 DESCRIPTION

This module is a data container for records from a Bioscope match (.ma)
file.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $mr = QCMG::Bioscope::MatchFileRecord->new(
                defline  => '12_134_186_F3',
                sequence => 'T01230001203202010202013',
                verbose  => 1 );  

The defline and sequence parameters must be supplied to this
constructor.

=item B<defline()>
 
 $mr->defline();

Returns the ID of the read loaded in this object.

=item B<sequence()>
 
 $mr->sequence();

Returns the sequence of the read loaded in this object.

=item B<verbose()>

 $ma->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: MatchFileRecord.pm 4660 2014-07-23 12:18:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
