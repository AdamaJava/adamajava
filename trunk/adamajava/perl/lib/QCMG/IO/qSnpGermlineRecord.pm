package QCMG::IO::qSnpGermlineRecord;

###########################################################################
#
#  Module:   QCMG::IO::qSnpGermlineRecord
#  Creator:  John V Pearson
#  Created:  2010-06-20
#
#  Data container for a qSNP germline SNP record.
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
    $VALID_HEADERS = [ qw(
            analysis_id control_sample_id variation_id variation_type 
            chromosome chromosome_start chromosome_end chromosome_strand
            refsnp_allele refsnp_strand reference_genome_allele
            control_genotype tumour_genotype quality_score probability
            read_count is_annotated validation_status validation_platform
            xref_ensembl_var_id note QCMGflag ND TD ) ];

    foreach my $method (@{$VALID_HEADERS}) {
        $VALID_AUTOLOAD_METHODS->{$method} = 1;
    }

}

###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    croak "no data parameter to new()" unless
        (exists $params{data} and defined $params{data});
    croak "no headers parameter to new()" unless
        (exists $params{headers} and defined $params{headers});

    my $self = { verbose     => $params{verbose} || 0 };
    bless $self, $class;

    my $dcount = scalar @{$params{data}};
    my $hcount = scalar @{$params{headers}};
    carp "data [$dcount] and header [$hcount] counts do not match" 
        if ($dcount != $hcount and $self->verbose);

    # These are the expected headers
    my @headers = @{ $VALID_HEADERS };

    foreach my $ctr (0..$hcount-1) {
        if (! defined $params{headers}->[$ctr]) {
            carp "Empty header value in column [$ctr] - ignoring data"
                if $self->verbose;
            next;
        }
        $self->{ $params{headers}->[$ctr] } = $params{data}->[$ctr];
    }

    # We have to keep a track of any extra fields by name
    if ($hcount > scalar(@headers)) {
        foreach my $ctr (scalar(@headers)..($hcount-1)) {
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

    if ($invocation =~ m/^.*::([^:]*)$/) {
        $method = $1;
    }
    else {
        croak "QCMG::IO::qSnpGermlineRecord AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    croak "QCMG::IO::qSnpGermlineRecord can't access method [$method] via AUTOLOAD"
        unless (exists $VALID_AUTOLOAD_METHODS->{$method});

    # If this is a setter call then do the set
    if (defined $value) {
        $self->{$method} = $value;
    }
    # Return current value
    return $self->{$method};

}  


sub to_text {
    my $self    = shift;

    my @columns = @{ $VALID_HEADERS };
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
        return $self->{$type} = shift;
    }
    else {
        # Check for existence;
        if (exists $self->{$type}) {
            return $self->{$type};
        }
        else {
            return undef;
        }
    }
}

1;


__END__


=head1 NAME

QCMG::IO::qSnpGermlineRecord - qSNP germline record data container


=head1 SYNOPSIS

 use QCMG::IO::qSnpGermlineRecord;


=head1 DESCRIPTION

This module provides a data container for a qSNP germline SNP Record.


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
