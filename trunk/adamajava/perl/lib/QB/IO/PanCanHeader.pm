package QB::IO::PanCanHeader;

###########################################################################
#
#  Module:   QB::IO::PanCanHeader
#  Creator:  John V Pearson
#  Created:  2015-05-09
#
#  Data container for all of the information needed about a BAM in order
#  to create na ICGC PanCancer-compliant file for reheading the BAM.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use vars qw( $SVNID $REVISION $VALID_HEADERS $VALID_AUTOLOAD_METHODS );

use Grz::Util::Log;

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global

BEGIN {

    # These are the data items we need for ICGC PanCancer Train 3 in
    # May 2015.  These may remain the same for future efforts in which
    # case there is no need for this extra work or it could simplify our
    # lives if the required information fields change.
    #
    # Fields starting with 'rg_' should all be concatenated onto the @RG
    # line, probably in the order shown although order *should* be
    # irrelevant.
    #
    # Fields starting with 'co_' should each be placed on a separate @CO
    # line in the format '@CO field_name:value' where field_name is the
    # field name but with the leading 'co_' removed.

    my @v_3 = qw( rg_ID rg_CN rg_DT rg_LB
                  rg_PI rg_PL rg_PM rg_PU rg_SM
                  co_dcc_project_code
                  co_submitter_donor_id
                  co_submitter_specimen_id
                  co_submitter_sample_id
                  co_dcc_specimen_type
                  co_use_cntl
                );

    $VALID_HEADERS = {
       '3.0' => \@v_3,
    };

    # The version-specific headers are doubly important to use because
    # we are going to use them to create a hashref ($VALID_AUTOLOAD_METHODS)
    # that will hold the names of all the methods that we will try to handle
    # via AUTOLOAD.  We are using AUTOLOAD because with a little planning,
    # it lets us avoid defining and maintaining a lot of basically identical
    # accessor methods.

    $VALID_AUTOLOAD_METHODS = {
       '3.0' => {},
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

    # Default version is 3.0 unless a version was specified in the call
    # to new() and it is valid, i.e. it is in the global $VALID_HEADERS.
    
    my $version = '3.0';
    if (exists  $params{version} and 
        defined $params{version} and
        exists  $VALID_HEADERS->{ $params{version} }) {
        $version = $params{version};
    }
    
    my $self = { version    => $version,
                 fields     => {},
                 attributes => {},
                 verbose    => (exists $params{verbose} ?
                                       $params{verbose} : 0)
               };

    bless $self, $class;
}


sub version {
    my $self = shift;
    # We intentionally do not provide a setter for this value - it has
    # to be set during the call to new() or not at all.  Trying to
    # change version would include changing the valid fields after some
    # of the might have already been set and that is asking for trouble.
    return $self->{version};
}


sub AUTOLOAD {
    my $self  = shift;
    my $value = shift || undef;

    my $type       = ref($self) or confess "$self is not an object";
    my $invocation = $AUTOLOAD;
    my $method     = undef;
    my $version    = $self->version;

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
        $self->{fields}->{$method} = $value;
    }
    # Return current value
    return $self->{fields}->{$method};
}  


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub attribute {
    my $self = shift;
    my $type = shift;

    # We will need to implement set and get

    if (@_) {
        my $val = shift;
        $self->{attributes}->{$type} = $val;
        return $val;
    }
    else {
        # Check for existence;
        if (exists $self->{attributes}->{$type}) {
            return $self->{attributes}->{$type};
        }
        else {
            return undef;
        }
    }
}


sub attributes {
    my $self = shift;
    # Return a list of all attributes as [name,value]
    return map { [ $_, $self->{attributes}->{$_} ] }
           sort keys %{ $self->{attributes} };
}


sub to_text {
    my $self = shift;

    my $text = "\@HD\tVN:1.4\n";

    # This code should preserve the order in which the fields were 
    # specified in the BEGIN{} block at the top of the package.

    my @fields = @{ $VALID_HEADERS->{$self->version} };
    my @rg_fields = grep {/^rg_/} @fields;
    my @co_fields = grep {/^co_/} @fields;

    my @rg_values = map { $_ .':'. $self->{fields}->{$_}
                          if defined $self->{fields}->{$_} }
                    @rg_fields;

    # Strip off leading rg_ strings
    ($rg_values[$_] =~ s/^rg_//) foreach (0..$#rg_values);

    $text .= "\@RG\t" . join( "\t", @rg_values ) ."\n";

    my @co_values = map  { $_ .':'. $self->{fields}->{$_} }
                    grep { defined $self->{fields}->{$_} }
                    @co_fields;

    # Strip off leading co_ strings
    ($co_values[$_] =~ s/^co_//) foreach (0..$#co_values);

    foreach my $value (@co_values) {
        $text .= "\@CO\t$value\n";
    }
    
    return $text;
}

1;


__END__


=head1 NAME

QB::IO::PanCanHeader - ICGC PanCancer BAM header data container


=head1 SYNOPSIS

 use QB::IO::PanCanHeader;


=head1 DESCRIPTION

This module provides a data container for an ICGC PanCancer BAM header
record.

=head1 METHODS

Many of the methods in this module depend n which version PanCan header
you are using.

=over

=item B<attribute()>

  $rec->attribute( 'Status', 'qSNP-only' );
  my $status = $rec->attribute( 'Status' );

Setter and getter for non-standard attributes.  These will not be output
when the record is serialised to file but are available for programming
against.

=back

=head1 AUTHORS

John Pearson L<mailto:john.pearson@qimrberghofer.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) QIMR Berghofer Medical Research Institute 2015

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
