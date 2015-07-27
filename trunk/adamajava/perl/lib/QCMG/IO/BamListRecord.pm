package QCMG::IO::BamListRecord;

###########################################################################
#
#  Module:   QCMG::IO::BamListRecord
#  Creator:  John V Pearson
#  Created:  2013-03-18
#
#  Data container for a qbasepileup record.
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
    # Our qbasepileup file must start with a header lines that matches one
    # of the following lists (dependent on version) exactly including case.
    # N.B. the mixed case that can be observed in the field names
    # follows the rules:
    # 1. Fields that comes from qcmg_scheme database are in all small
    #    letters with underscores separating words
    # 2. Fields that originate in this module are in CamelCase with no
    #    spaces between words.

    my @v_1_0 = (
           'ID', 'Donor', 'BamName', 'BamHeaderCRC32', 'BamCtimeEpoch',
           'Project', 'Material', 'Sample Code',
           'Sequencing Platform', 'Aligner',
           'Capture Kit', 'Library Protocol', 'Sample',
           'Species Reference Genome', 'Reference Genome File', 'Failed QC' );
    my @v_1_1 = ( @v_1_0, 'FromQLimsMeta' );
    my @v_1_2 = (
           'ID', 'project', 'BamName', 'BamHeaderCRC32', 'BamCtimeEpoch',
           'parent_project', 'material', 'sample_code',
           'sequencing_platform', 'aligner',
           'capture_kit', 'library_protocol', 'sample',
           'species_reference_genome', 'reference_genome_file', 'failed_qc',
           'FromQLimsMeta' );
    my @v_1_3 = (
           'ID', 'project', 'BamName', 'BamHeaderCRC32', 'BamMtimeEpoch',
           'parent_project', 'material', 'sample_code',
           'sequencing_platform', 'aligner',
           'capture_kit', 'library_protocol', 'sample',
           'species_reference_genome', 'reference_genome_file', 'failed_qc',
           'FromQLimsMeta' );


    $VALID_HEADERS = {
       version_1_0 => \@v_1_0,
       version_1_1 => \@v_1_1,
       version_1_2 => \@v_1_2,
       version_1_3 => \@v_1_3,
    };

    # The version-specific headers are doubly important to use because
    # we are going to use them to create a hashref ($VALID_AUTOLOAD_METHODS)
    # that will hold the names of all the methods that we will try to handle
    # via AUTOLOAD.  We are using AUTOLOAD because with a little planning,
    # it lets us avoid defining and maintaining a lot of basically identical
    # accessor methods.

    $VALID_AUTOLOAD_METHODS = {
       version_1_0 => {},
       version_1_1 => {},
       version_1_2 => {},
    };

    foreach my $version (keys %{$VALID_HEADERS}) {
        foreach my $method (@{$VALID_HEADERS->{$version}}) {
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
    croak "no data parameter to new()" unless
        (exists $params{data} and defined $params{data});
    croak "no headers parameter to new()" unless
        (exists $params{headers} and defined $params{headers});

    my $self = { qbp_version => $params{version},
                 verbose     => $params{verbose} || 0 };
    bless $self, $class;

    my $dcount = scalar @{$params{data}};
    my $hcount = scalar @{$params{headers}};
    carp "data [$dcount] and header [$hcount] counts do not match" 
        if ($dcount != $hcount and $self->verbose);

    # These are the expected headers
    my @headers = @{ $VALID_HEADERS->{$params{version}} };

    # Copy the expected fields across first
    foreach my $ctr (0..(scalar(@headers)-1)) {
        if (! defined $params{headers}->[$ctr]) {
            carp "Empty header value in column [$ctr] - ignoring data"
                if $self->verbose;
            next;
        }
        $self->{ $params{headers}->[$ctr] } = $params{data}->[$ctr];
    }

    # We have to keep a track of any extra fields by name and we are
    # going to copy them into the hash with an 'opt_' prefix to prevent
    # any collisions with "real" columns (already seen this!)
    if ($hcount > scalar(@headers)) {
        foreach my $ctr (scalar(@headers)..($hcount-1)) {
            if (! defined $params{headers}->[$ctr]) {
                carp "Empty header value in column [$ctr] - ignoring data"
                    if $self->verbose;
                next;
            }
            $self->{ 'opt_'.$params{headers}->[$ctr] } = $params{data}->[$ctr];
            push @{ $self->{extra_fields} }, $params{headers}->[$ctr];
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
    my $version    = $self->{qbp_version};

    if ($invocation =~ m/^.*::([^:]*)$/) {
        $method = $1;
    }
    else {
        croak "QCMG::IO::BamListRecord AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    croak "QCMG::IO::BamListRecord can't access method [$method] via AUTOLOAD"
        unless (exists $VALID_AUTOLOAD_METHODS->{$version}->{$method});

    # If this is a setter call then do the set
    if (defined $value) {
        $self->{$method} = $value;
    }
    # Return current value
    return $self->{$method};

}  


sub to_text {
    my $self    = shift;

    my @columns = @{ $VALID_HEADERS->{$self->{qbp_version}} };
    my @values = map { $self->{ $_ } } @columns;
    # Don't forget extra fields if any
    my @extras = map { $self->{ $_ } } @{ $self->{extra_fields} };
    return join("\t",@values,@extras);
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub optional_col {
    my $self = shift;
    my $type = shift;

    if (@_) {
        return $self->{'opt_'.$type} = shift;
    }
    else {
        # Check for existence;
        if (exists $self->{'opt_'.$type}) {
            return $self->{'opt_'.$type};
        }
        else {
            return undef;
        }
    }
}

1;


__END__


=head1 NAME

QCMG::IO::BamListRecord - Information about a BAM file


=head1 SYNOPSIS

 use QCMG::IO::BamListRecord;


=head1 DESCRIPTION

This module provides a data container for information about a seq_final
BAM file.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


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
