package QCMG::Lifescope::ChtReader;

###########################################################################
#
#  Module:   QCMG::Lifescope::ChtReader.pm
#  Creator:  John V Pearson
#  Created:  2011-08-30
#
#  Parses the .cht and .csv files created by LifeScope in the output/
#  directory tree.  This module uses the Moose OO framework.
#
#  $Id: Cht.pm 1068 2011-09-08 11:29:36Z j.pearson $
#
###########################################################################


use Moose;  # implicitly sets strict and warnings

use IO::File;
use Data::Dumper;
use QCMG::Lifescope::Cht;
use Carp qw( carp croak cluck confess );
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 1068 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Cht.pm 1068 2011-09-08 11:29:36Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

has 'filecount' => ( is => 'rw', isa => 'Int', default => 0 );
has 'verbose'   => ( is => 'rw', isa => 'Int', default => 0 );


sub parse_file {
    my $self = shift;
    my $file = shift;

    # The .cht files are unix-style plain-text with a simple
    # standardised format so they are pretty pleasant to parse.

    my $infh = IO::File->new( $file, 'r' );
    die "Cannot open cht file $file for reading: $!" unless defined $infh;
    $self->filecount( $self->filecount + 1 );

    # Create a new Cht data container
    my $cht = QCMG::Lifescope::Cht->new( pathname => $file );

    # Read headers and make sure it is a cht type we can cope with
    my $column_headers = $self->_read_headers( $infh, $cht );
    if ($cht->type =~ /(line)|(vbar)|(pie)/) {
        $self->_parse_cht_data( $infh, $column_headers, $cht );
    }
    else {
        die 'Unrecognised .cht type [', $cht->type . ']';
    }

    # Check that axis and header counts agree
    my $axis_count   = scalar ( keys %{ $cht->axes } );
    my $header_count = scalar @{ $cht->headers };
    warn sprintf( "Column counts from axes (%s) and headers (%s) differ for file (%s)",
                 $axis_count, $header_count, $file) 
        unless ($axis_count == $header_count and $axis_count > 0);
    $cht->axis_num( $axis_count );

    return $cht;
}


sub _read_headers {
    my $self = shift;
    my $infh = shift;
    my $cht  = shift;  # QCMG::Lifescope::Cht object

    # Read off headers
    while (1) {
        my $line = $infh->getline;
        $line =~ s/\s+$//;   # trim trailing spaces
        next unless $line;   # skip empty lines
        if ($line =~ /^#/) {
            # It's a comment/header
            $line =~ s/^#\s+//;   # trim # and leading spaces
            $line =~ s/\s+$//;    # trim trailing spaces
            my ($key, $value) = split /:\s?/, $line, 2;
            if ($key eq 'name') {
                $cht->name( $value );
            }
            elsif ($key eq 'type') {
                $cht->type( $value );
            }
            elsif ($key eq 'title') {
                $cht->title( $value );
            }
            elsif ($key =~ /(.)axisname/) {
                $cht->axes->{ $1 }->{title} = $value;
            }
            elsif ($key =~ /(.)range/) {
                $cht->axes->{ $1 }->{range} = $value;
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


sub _parse_cht_data {
    my $self = shift;
    my $infh = shift;
    my $headers = shift;
    my $cht  = shift;  # QCMG::Lifescope::Cht object

    push @{ $cht->headers }, split( /\,/, $headers);

    # Read off data
    while (my $line = $infh->getline) {
        $line =~ s/\s+$//;   # trim trailing spaces
        next unless $line;   # skip empty lines
        my @values = split /\,/, $line;
        push @{ $cht->data }, \@values;
    }
}


no Moose;

1;
__END__


=head1 NAME

QCMG::Lifescope::ChtReader - Perl module for parsing Lifescope .cht files


=head1 SYNOPSIS

 use QCMG::Lifescope::ChtReader;

 my $reader = QCMG::Lifescope::ChtReader->new();
 my $cht = $reader->parse_file( 'Group_1.BaseQv.F3.cht' );
 $cht->trim_data_table(99);
 print $cht->as_xml();

=head1 DESCRIPTION

This module provides an interface for parsing Lifescope .cht files.  The
B<parse_file()> method emits QCMG::Lifescope::Cht data container
objects.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $reader = QCMG::Lifescope::ChtReader->new;
 my $reader = QCMG::Lifescope::ChtReader->new( verbose => 2 );

Contructor has no mandatory arguments and only one optional argument -
verbose level as an integer.

=item B<parse_file()>

 my $cht = $reader->parse_file( 'Group_1.BaseQv.F3.cht' );

Parse a Bioscope .cht chart file into a QCMG::Lifescope::Cht object.

=item B<verbose()>

 $cht->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Cht.pm 1068 2011-09-08 11:29:36Z j.pearson $


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
