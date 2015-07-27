package QCMG::Lifescope::TblReader;

###########################################################################
#
#  Module:   QCMG::Lifescope::TblReader.pm
#  Creator:  John V Pearson
#  Created:  2011-12-04
#
#  Parses the .tbl files created by LifeScope in the output/
#  directory tree.  This module uses the Moose OO framework.
#
#  $Id$
#
###########################################################################


use Moose;  # implicitly sets strict and warnings

use IO::File;
use Data::Dumper;
use QCMG::Lifescope::Tbl;
use Carp qw( carp croak cluck confess );
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

has 'filecount' => ( is => 'rw', isa => 'Int', default => 0 );
has 'verbose'   => ( is => 'rw', isa => 'Int', default => 0 );


sub parse_file {
    my $self = shift;
    my $file = shift;

    # The .tbl files are unix-style plain-text CSV so they are
    # pretty pleasant to parse.

    my $infh = IO::File->new( $file, 'r' );
    die "Cannot open tbl file $file for reading: $!" unless defined $infh;
    $self->filecount( $self->filecount + 1 );

    # Create a new Tbl data container
    my $tbl = QCMG::Lifescope::Tbl->new( pathname => $file );

    # Read headers and make sure it is a tbl type we can cope with
    my $column_headers = $self->_read_headers( $infh, $tbl );
    if ($tbl->title =~ /(BAMStats Summary)/) {
        $self->_parse_tbl_data( $infh, $column_headers, $tbl );
    }
    else {
        die 'Unrecognised .tbl title [', $tbl->title . ']';
    }

    return $tbl;
}


sub _read_headers {
    my $self = shift;
    my $infh = shift;
    my $tbl  = shift;  # QCMG::Lifescope::Tbl object

    # Read off headers
    while (1) {
        my $line = $infh->getline;
        $line =~ s/\s+$//;   # trim trailing spaces
        next unless $line;   # skip empty lines
        if ($line =~ /^#/) {
            # It's a comment/header
            $line =~ s/^#\s*//;   # trim # and any leading spaces
            my ($key, $value) = split /:\s?/, $line, 2;
            if ($key eq 'title') {
                $tbl->title( $value );
            }
            else { 
               die "Unrecognised key-value pair in header: $key, $value";
            }
        }
        else {
            # First (and only) non-comment line we read should be the headers
            return $line;
        }
    }
}


sub _parse_tbl_data {
    my $self = shift;
    my $infh = shift;
    my $headers = shift;
    my $tbl  = shift;  # QCMG::Lifescope::Tbl object

    push @{ $tbl->headers }, split( /\,/, $headers);

    # Read off data
    while (my $line = $infh->getline) {
        $line =~ s/\s+$//;   # trim trailing spaces
        next unless $line;   # skip empty lines
        my @values = split /\,/, $line;
        push @{ $tbl->data }, \@values;
    }
}


no Moose;

1;
__END__


=head1 NAME

QCMG::Lifescope::TblReader - Perl module for parsing Lifescope .tbl files


=head1 SYNOPSIS

 use QCMG::Lifescope::TblReader;

 my $reader = QCMG::Lifescope::TblReader->new();
 my $tbl = $reader->parse_file( 'Group_1-summary.tbl' );
 print $tbl->as_xml();

=head1 DESCRIPTION

This module provides an interface for parsing Lifescope .tbl files.  The
B<parse_file()> method emits QCMG::Lifescope::Tbl data container
objects.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $reader = QCMG::Lifescope::TblReader->new;
 my $reader = QCMG::Lifescope::TblReader->new( verbose => 2 );

Contructor has no mandatory arguments and only one optional argument -
verbose level as an integer.

=item B<parse_file()>

 my $tbl = $reader->parse_file( 'Group_1-summary.tbl' );

Parse a Bioscope .tbl chart file into a QCMG::Lifescope::Tbl object.

=item B<verbose()>

 $tbl->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


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
