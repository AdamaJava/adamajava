package QCMG::IO::QbpReader;

###########################################################################
#
#  Module:   QCMG::IO::QbpReader
#  Creator:  John V Pearson
#  Created:  2012-11-25
#
#  Reads qbasepileup files.
#
#  $Id: QbpReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;
use QCMG::IO::TsvReader;
use QCMG::IO::QbpRecord;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION @ISA );

@ISA = qw( QCMG::IO::TsvReader );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: QbpReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    # If no version parameter supplied, assume version v_1_0
    $params{version} = 'version_1_0'
        unless (exists $params{version} and $params{version});
    # Check that version is valid
    die 'QCMG::IO::QbpReader version [', $params{version}, "] is not valid\n"
        unless (exists $QCMG::IO::QbpRecord::VALID_HEADERS->{ $params{version} });
    $params{headers} =
        $QCMG::IO::QbpRecord::VALID_HEADERS->{ $params{version} };

    qlogprint( 'reading from file: ', $params{filename}. "\n" )
        if $params{verbose};

    my $self = bless $class->SUPER::new( %params ), $class;
    $self->version( $params{version} );

    return $self;
}


sub version {
    my $self = shift;
    return $self->{version} = shift if @_;
    return $self->{version};
}


sub next_record {
    my $self = shift;

    my $line = $self->SUPER::next_record;

    # On last record, output counter if verbose
    if (! defined $line) {
        qlogprint( 'read ',$self->record_ctr, " records\n" )
            if $self->verbose;
        return undef;
    }

    my @fields = split /\t/, $line;
    my $rec = QCMG::IO::QbpRecord->new(
                  data    => \@fields,
                  version => $self->version,
                  headers => $self->{headers} );
    return $rec;
}


1;

__END__


=head1 NAME

QCMG::IO::QbpReader - qbasepileup file reader


=head1 SYNOPSIS

 use QCMG::IO::QbpReader;


=head1 DESCRIPTION

This module provides an interface for reading files created by the
qbasepileup tool.


=head1 METHODS

This class inherits from L<QCMG::IO::TsvReader> so all of the methods
from that class are also available here in addition to those outlined
here.

=over

=item B<new()>

my $qbp = QCMG::IO::QbpReader->new(
                filename => 'qbasepileup_APGI2353.txt',
                verbose  => 1 );

Returns a reader object that can be used to retrieve records.

=item B<next_record()>

 my $rec = $qbp->next_record();

Returns a QCMG::IO::QbpRecord object.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: QbpReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013-2014

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
