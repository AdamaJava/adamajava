package QCMG::IO::Qbasepileup::ReportRecord;

###########################################################################
#
#  Module:   QCMG::IO::Qbasepileup::ReportRecord
#  Creator:  John V Pearson
#  Created:  2014-08-07
#
#  Data container for a qBasepileup file record.
#
#  $Id: ReportRecord.pm 4683 2014-08-07 23:24:18Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION $VALID_HEADERS $VALID_AUTOLOAD_METHODS );

use QCMG::Util::QLog;

( $REVISION ) = '$Revision: 4683 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: ReportRecord.pm 4683 2014-08-07 23:24:18Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global

BEGIN {
    ##qbasepileup version 1.0

    my @v_1 = qw( 
           ID Donor Bam SnpId Chromosome Start End
           RefBase TotalRef TotalNonRef
           Aplus Cplus Gplus Tplus Nplus TotalPlus
           Aminus Cminus Gminus Tminus Nminus TotalMinus );

    $VALID_HEADERS = {
       '1.0' => \@v_1,
    };

    # The version-specific headers are doubly important to use because
    # we are going to use them to create a hashref ($VALID_AUTOLOAD_METHODS)
    # that will hold the names of all the methods that we will try to handle
    # via AUTOLOAD.  We are using AUTOLOAD because with a little planning,
    # it lets us avoid defining and maintaining a lot of basically identical
    # accessor methods.

    $VALID_AUTOLOAD_METHODS = {
       '1.0' => {},
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

    die 'version ',$params{version}, ' is not supported'
        unless exists $VALID_HEADERS->{$params{version}};

    my $self = { _version  => $params{version},
                 verbose   => $params{verbose} || 0 };
    bless $self, $class;

    # Counts of data fields and headers must match
    my $dcount = scalar @{$params{data}};
    my @headers = @{ $VALID_HEADERS->{$params{version}} };
    my $hcount = scalar @headers;
    die "data [$dcount] and header [$hcount] counts do not match" 
        if ($dcount != $hcount);

    # Copy data into record hash
    foreach my $ctr (0..(scalar(@headers)-1)) {
        $self->{ $headers[$ctr] } = $params{data}->[$ctr];
    }

    return $self;
}


sub AUTOLOAD {
    my $self  = shift;
    my $value = shift || undef;

    my $type       = ref($self) or confess "$self is not an object";
    my $invocation = $AUTOLOAD;
    my $method     = undef;
    my $version    = $self->{_version};

    if ($invocation =~ m/^.*::([^:]*)$/) {
        $method = $1;
    }
    else {
        croak "AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    croak "can't access method [$method] via AUTOLOAD"
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
    my $ra_opts = shift;

    my @columns = @{ $VALID_HEADERS->{$self->{_version}} };
    my @values  = map { $self->{ $_ } } @columns;

    # Don't forget extra fields if any.  This is complicated because in
    # most cases we are outputting lots of MAF records to a file so if
    # the MAF records have different extra columns or they are in a
    # different order, we will output a chaotic mess.  The safest way to
    # handle this is to force the user to supply a list of any extra columns
    # they want output.

    if (defined $ra_opts) {
        foreach my $opt (@{ $ra_opts }) {
             # If this record does not have a value for the requested
             # extra column then we need to still output a blank
             # field so the spacing is preserved.
             my $val = ( exists $self->{ 'opt_'.$opt } and
                         defined $self->{ 'opt_'.$opt } ) ?
                       $self->{ 'opt_'.$opt } : '';
             push @values, $val;
        }
    }

    return join("\t",@values);
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub version {
    my $self = shift;
    return $self->{_version};
}


1;


__END__


=head1 NAME

QCMG::IO::Qbasepileup::ReportRecord - qBasepileup data container


=head1 SYNOPSIS

 use QCMG::IO::Qbasepileup::ReportRecord;


=head1 DESCRIPTION

This module provides a data container for a qBasepileup Record.

=head1 METHODS

=over

=item B<new()>

=item B<version()>

=item B<verbose()>

=back

=head1 AUTHORS

John Pearson L<mailto:john.pearson@qimrberghofer.edu.au>


=head1 VERSION

$Id: ReportRecord.pm 4683 2014-08-07 23:24:18Z j.pearson $


=head1 COPYRIGHT

This software is copyright 2014 by the QIMR Berghofer Medical Research
Institute.  All rights reserved.

=cut
