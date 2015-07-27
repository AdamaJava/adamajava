package QCMG::DB::GeneusResult;

##############################################################################
#
#  Module:   QCMG::DB::GeneusResult.pm
#  Creator:  John V Pearson
#  Created:  2011-06-21
#
#  This class is a data container to hold information about the results
#  of a query run against the QCMG LIMS using Geneus.pm.
#
#  $Id$
#
##############################################################################

=pod

=head1 NAME

QCMG::DB::GeneusReport -- Result from a Geneus query

=head1 SYNOPSIS

 my $gl = QCMG::DB::Geneus->new();
 $gl->connect();
 my $gs = $gl->query( 'processtypes/' );
 print Dumper $gs;

=head1 DESCRIPTION

This class is a data container for the results of a query run against
the QCMG Geneus LIMS using the QCMG::DB::Geneus.pm perl module.  As a
user, you should never need to instantiate an object of this class
yourself but you will receive one back from every Geneus query.

If the query was successful then calling data() will give you a data
structure parsed from the XML.  You can call status() and status_msg()
to get integer and string repesentations of the status of the query
where a non-zero status() indicates failure.  If status == 99 then the
failure was not one recognised by GeneusResults in which case you can
use reply() to see the full text of the reply from the server.  The
reply is usually in XML for successful queries and HTML for failures.

=head1 REQUIREMENTS

 Carp
 Data::Dumper

=cut

use strict;
use warnings;
use Data::Dumper;
use Carp qw( carp croak );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

################################################################################


sub new {
    my $class  = shift;
    my %params = @_;
    
    my $self = { };

    # Set values from input %params or from defaults.
    my %defaults = ( url         => undef,
                     reply       => undef,
                     msg         => undef,
                     data        => undef,
                     query_count => undef,
                     uri_list    => undef,
                     verbose => 0 );

    # This block will set values from parameters then defaults. Note
    # that only keys from %defaults are even checked so this is how we
    # control what can be specified in @params - if it's not in 
    # %defaults, it can't be set!

    foreach my $key (keys %defaults) {
        if (exists $params{$key} and defined $params{$key} and $params{$key}) {
            $self->{ $key } = $params{ $key };
        }
        else {
            $self->{ $key } = $defaults{ $key };
        }
    }

    # Parse HTTP msg and set status and status_msg accordingly.  Status
    # 99 should always be last as is set to match anything so all queries
    # will be marked as 99 if they don't match an earlier status.
    my @errors = (
        [ 0, qr/OK/, 'Query appears to have executed successfully' ],
        [ 1, qr/Unauthorized/, 'Unauthorised - name/password missing or incorrect?' ],
        [ 2, qr/Not Found/, 'No values found in LIMS or could be a malformed query string' ],
        [ 99, qr/.*/, 'Indeterminate status - could not parse server reply.' ],
        );
    if (defined $self->{msg}) {
        foreach my $error (@errors) {
            if ($self->{msg} =~ $error->[1]) {
                $self->{status}     = $error->[0];
                $self->{status_msg} = $error->[2];
                last;  # On match we must stop looking
            }
        }
    }
    else {
        # Assumes 'indeterminate' is last item in list
        $self->{status}     = $errors[-1]->[0];
        $self->{status_msg} = $errors[-1]->[2];
    }

    bless $self, $class;
}


################################################################################

=head1 METHODS

B<url()>

Get URL query submitted

Parameters:
 none

Returns:
 scalar: URL

=cut
sub server {
    my $self = shift;
    return $self->{url};
}


################################################################################

=pod

B<reply()>

Get text returned by Geneus server (HTML or XML)

Parameters:
 none

Returns:
 scalar: HTML or XML string

=cut
sub reply {
    my $self = shift;
    return $self->{reply};
}


################################################################################

=pod

B<data()>

Get data structure parsed from XML reply to query

Parameters:
 none

Returns:
 data structure

=cut
sub data {
    my $self = shift;
    return $self->{data};
}


################################################################################

=pod

B<query_count()>

Number of queries required for multiple-page queries

Parameters:
 none

Returns:
 integer

Some LIMS queries (e.g. B<samples()>) require multiple queries
under-the-hood so this object contains the most recent query plus 2
additional data items that summarise the process - the number of
subqueries required (this method) plus the list of all URI's returned
across all of the subqueries (see B<uri_list()>).  This method returns
undef if the underlying query was not of the special multi-query type.

=cut
sub query_count {
    my $self = shift;
    return $self->{query_count};
}


################################################################################

=pod

B<uri_list()>

List of all URIs for multiple-page queries

Parameters:
 none

Returns:
 arrayref

Some LIMS queries (e.g. B<samples()>) require multiple queries
under-the-hood so this object contains the most recent query plus 2
additional data items that summarise the process - the number of
subqueries required (see B<query_count()) plus the list of all URI's
returned across all of the subqueries (this method).  This method returns
undef if the underlying query was not of the special multi-query type.

=cut
sub uri_list {
    my $self = shift;
    return $self->{uri_list};
}


################################################################################

=pod

B<verbose()>

Get/set verbosity level

Parameters:
 scalar: integer defining level of verbose messages

Returns:
 scalar: integer defining level of verbose messages

=cut
sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


################################################################################

=pod

B<status()>

Get status of query (0 = success, >0 = failure)

Parameters:
 none

Returns:
 scalar: integer

=cut
sub status {
    my $self = shift;
    return $self->{status};
}


################################################################################

=pod

B<status_msg()>

String message associated with non-zero value for B<status()>

Parameters:
 none

Returns:
 scalar: string

=cut
sub status_msg {
    my $self = shift;
    return $self->{status_msg};
}


1;

__END__

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
