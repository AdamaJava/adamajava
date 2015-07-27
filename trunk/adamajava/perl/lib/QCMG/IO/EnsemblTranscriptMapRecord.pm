package QCMG::IO::EnsemblTranscriptMapRecord;

###########################################################################
#
#  Module:   QCMG::IO::EnsemblTranscriptMapRecord
#  Creator:  John V Pearson
#  Created:  2012-11-26
#
#  Data container for a record from the Ensembl transcript map text file.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION $VALID_HEADERS $VALID_AUTOLOAD_METHODS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global

BEGIN {

    my @v_20121126 =
        qw( gene_id transcript_id protein_id
            gene_symbol gene_symbol_description );
    my @v_20130110 =
        qw( gene_id transcript_id protein_id
            gene_symbol gene_symbol_description entrez_id );

    $VALID_HEADERS = {
        'v_20121125' => \@v_20121126,
        'v_20130110' => \@v_20130110,
        };

    # Create AUTOLOAD methods hash from $VALID_HEADERS
    $VALID_AUTOLOAD_METHODS = {};
    foreach my $version (keys %{$VALID_HEADERS}) {
        $VALID_AUTOLOAD_METHODS->{ $version } = {};
        foreach my $method (@{ $VALID_HEADERS->{ $version } }) {
            $VALID_AUTOLOAD_METHODS->{$version}->{$method} = 1;
        }
    }
}

###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    croak "no version parameter to new()" unless
        (exists $params{version} and defined $params{version});

    # Based on requested version, we will set the valid headers and
    # autoload methods. Default is @v_1.
    my @valid_headers = ();
    if (exists $VALID_HEADERS->{ $params{version} }) {
        @valid_headers = @{ $VALID_HEADERS->{ $params{version} } };
    }
    else {
        croak 'the version parameter [', $params{version},
              '] passed to EnsemblTranscripMapRecord::new() is unknown';
    }

    my $self = { valid_headers          => $VALID_HEADERS->{ $params{version} },
                 valid_autoload_methods => $VALID_AUTOLOAD_METHODS->{ $params{version} },
                 version                => $params{version},
                 verbose                => $params{verbose} || 0 };
    bless $self, $class;

    # This is for the use case where the user passes in a
    # array of data to be used to populate the record.  The data must
    # have exactly as many fields as the header count and it's up to the
    # user to make sure the order matches.  This is tres dangerous but
    # also tres useful!

    if ($params{data} and defined $params{data}) {
        my $dcount = scalar @{$params{data}};
        my $hcount = scalar @valid_headers;
        croak "data [$dcount] and header [$hcount] counts do not match" 
            if ($dcount != $hcount and $self->verbose);

        foreach my $ctr (0..($dcount-1)) {
            if (! defined $valid_headers[$ctr]) {
                carp "Empty header value in column [$ctr] - ignoring data"
                    if $self->verbose;
                next;
            }
            $self->{ $valid_headers[$ctr] } = $params{data}->[$ctr];
        }
    }

    return $self;
}


sub AUTOLOAD {
    my $self  = shift;
    my $value = shift || undef;

    my $type       = ref($self) or confess "$self is not an object";
    my $invocation = $AUTOLOAD;
    my $method     = undef;

    if ($invocation =~ m/^.*::([^:]*)$/) {
        $method = $1;
    }
    else {
        croak "QCMG::IO::EnsemblTranscriptMapRecord AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    croak "QCMG::IO::EnsemblTranscriptMapRecord can't access method [$method] via AUTOLOAD"
        unless (exists $self->{valid_autoload_methods}->{$method});

    # If this is a setter call then do the set
    if (defined $value) {
        $self->{$method} = $value;
    }

    # Return current value
    return $self->{$method};
}


sub to_text {
    my $self = shift;
    my @columns = @{ $self->{valid_headers} };
    my @values = map { exists $self->{ $_ } ? $self->{ $_ } : '' } @columns;
    return join("\t",@values);
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub version {
    my $self = shift;
    if (@_) {
        my $new_version = shift;
        # Check that requested version actually exists otherwise die
        if (exists $VALID_HEADERS->{ $new_version }) {
            $self->{valid_headers}           = $VALID_HEADERS->{ $new_version };
            $self->{valid_autoload_methods}  = $VALID_AUTOLOAD_METHODS->{ $new_version };
            $self->{version}                 = $new_version;
        }
        else {
            croak 'the version parameter [', $new_version,
                  '] passed to EnsemblTranscriptMapRecord::version() is unknown';
        }
    }
    return $self->{version};
}


1;


__END__


=head1 NAME

QCMG::IO::EnsemblTranscriptMapRecord - Ensembl id-to-id map record


=head1 SYNOPSIS

 use QCMG::IO::EnsemblTranscripMapRecord;


=head1 DESCRIPTION

This module provides a data container for a record parsed from the
Ensembl transcript-to-other-ids map text file created by querying a
locally installed instance of the Ensembl database using the script
QCMGScripts/j.pearson/annotation/ensembl_domains_from_mysql.sh.

=head1 METHODS

=over

=item B<new()>

 $snp = QCMG::IO::EnsemblTranscriptMapRecord->new(
            version => 'v_20121125',
            data    => \@fields );

In many cases you will not need to directly invoke the B<new()> method
as you will be getting the records from the B<new_record()> method from
the B<QCMG::IO::EnsemblTranscriptMapReader> class.

In the event that you do call this method directly, it is mandatory that
you supply a valid string for the version parameter.  You can populate
the record either by passing an array of values (in the correct order)
to the new() method, or by calling the set accessor methods.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2014

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
