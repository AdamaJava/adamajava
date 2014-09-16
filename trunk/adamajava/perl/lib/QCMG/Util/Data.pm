package QCMG::Util::Data;

##############################################################################
#
#  Module:   QCMG::Util::Data.pm
#  Creator:  John Pearson
#  Created:  2010-12-15
#
#  This non-OO perl module contains static methods for returning various
#  data structures relevant to QCMG.
#
#  $Id: Data.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;
use XML::Simple;
use IO::File;
use Carp qw( croak carp );
use vars qw( $REVISION $SVNID @ISA @EXPORT @EXPORT_OK );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID )    = '$Id: Data.pm 4665 2014-07-24 08:54:04Z j.pearson $'
                 =~ /\$Id:\s+(.*)\s+/;

BEGIN {
    use Exporter ();
    @ISA = qw(Exporter);
    # Optionally exported functions
    @EXPORT_OK = qw( sequencers );
}

sub sequencers {
    return { S0411 => 'snowball',
             S0413 => 'shesthefastest',
             S0414 => 'santaslittlehelper',
             S0416 => 'blinky',
             S0417 => 'furiousd',
             S0039 => 'stampy',
             S0014 => 'pinchy',
             S0449 => 'mojo',
             S0436 => 'generalsherman',
             S0433 => 'jubjub',
             S0428 => 'spiderpig',
             S8006 => 'nibbler',
             S88006 => 'nibbler',
             S16004 => 'guenter',
             S17001 => 'hypnotoad',
             S17002 => 'spiceweasel',
             S17009 => 'mushu' };
}


1;
__END__


=head1 NAME

QCMG::Util::Data - Perl module containing QCMG-specific data structures


=head1 SYNOPSIS

 use QCMG::Util::Data;


=head1 DESCRIPTION

This module is not an OO class, rather it contains a collection of
static methods that return QCMG-specific data structures such as a hash
mapping sequencer names to ID numbers.  To use any of
the functions described below, you will need to use the 'import'
notations shown above to make the functions visible in your program's
MAIN namespace.


=head1 FUNCTIONS

=over

=item B<sequencers()>

 my $rh_seqs = sequencers;

Returns a hash where the keys are the numeric IDs of QCMG sequencers
(S0014, S17009 etc) and the values are the QCMG-assigned sequencer names
(pinchy, nibbler etc).

=back 


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Data.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010-2013

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
