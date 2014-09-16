package QCMG::IO::qPileupRecord;

###########################################################################
#
#  Module:   QCMG::IO::qPileupRecord
#  Creator:  John V Pearson
#  Created:  2010-03-08
#
#  Data container for a qPileup record.
#
#  $Id: qPileupRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( confess croak );
use Data::Dumper;
use vars qw( $SVNID $REVISION @QPILEUP_COLUMNS $VALID_AUTOLOAD_METHODS );

our $AUTOLOAD;  # it's a package global

BEGIN {
    @QPILEUP_COLUMNS = qw (
        Reference Position Ref_base
        A_for C_for G_for T_for N_for
        Aqual_for Cqual_for Gqual_for Tqual_for Nqual_for
        MapQual_for ReferenceNo_for NonreferenceNo_for
        HighNonreference_for LowReadCount_for
        StartAll_for StartNondup_for StopAll_for
        DupCount_for MateUnmapped_for
        CigarI_for CigarD_for CigarS_for CigarH_for CigarN_for
        A_rev C_rev G_rev T_rev N_rev
        Aqual_rev Cqual_rev Gqual_rev Tqual_rev Nqual_rev
        MapQual_rev ReferenceNo_rev NonreferenceNo_rev
        HighNonreference_rev LowReadCount_rev
        StartAll_rev StartNondup_rev StopAll_rev
        DupCount_rev MateUnmapped_rev
        CigarI_rev CigarD_rev CigarS_rev CigarH_rev CigarN_rev
        );

    # Construct hash to be used to lookup column contents in AUTOLOAD
    $VALID_AUTOLOAD_METHODS = {};
    foreach my $ctr (0..$#QPILEUP_COLUMNS) {
        $VALID_AUTOLOAD_METHODS->{ $QPILEUP_COLUMNS[$ctr] } = $ctr;
    }
};


( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qPileupRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class = shift;
    my $line  = shift;
    
    chomp $line;
    my @fields = split ",", $line;
    warn 'Saw ', scalar(@fields),
        " fields, should have been at least 54 [$line]\n"
        if (scalar(@fields) < 54);

    my $self = \@fields;
    bless $self, $class;
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
        croak "QCMG::IO::qPileupRecord AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    croak "QCMG::IO::qPileupRecord can't access method [$method] via AUTOLOAD"
        unless (exists $VALID_AUTOLOAD_METHODS->{$method});

    # If this is a setter call then do the set
    if (defined $value) {
        $self->[ $VALID_AUTOLOAD_METHODS->{$method} ] = $value;
    }
    # Return current value
    return $self->[ $VALID_AUTOLOAD_METHODS->{$method} ];

}  

1;

__END__


=head1 NAME

QCMG::IO::qPileupRecord - qPileup Record data container


=head1 SYNOPSIS

 use QCMG::IO::qPileupRecord;


=head1 DESCRIPTION

This module provides a data container for a qPileup Record.


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: qPileupRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $


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

