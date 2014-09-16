#!/usr/bin/perl -w

##############################################################################
#
#  Program:  finder.pl
#  Author:   John V Pearson
#  Created:  2010-09-29
#
#  Find directories and files.
#
#  $Id: finder.pl 4670 2014-07-24 10:50:59Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::FileDir::Finder;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4670 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: finder.pl 4670 2014-07-24 10:50:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $dir        ='';
    my $type       ='d';
    my @patterns   = ();
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'd|dir=s'              => \$dir,           # -d
           't|type=s'             => \$type,          # -t
           'p|pattern=s'          => \@patterns,      # -p
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    # Allow for ,-separated lists of patterns
    @patterns = map { split /\,/,$_ } @patterns;

    print( "\nfinder.pl  v$REVISION  [" . localtime() . "]\n",
           "   dir           $dir\n",
           "   type          $type\n",
           '   pattern(s)    ', join("\n".' 'x17, @patterns), "\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $finder = QCMG::FileDir::Finder->new( verbose => $VERBOSE );

    if ($type =~ /^d/i) {
        foreach my $pattern (@patterns) {
            my @dirs = $finder->find_directory( $dir, $pattern );
            print "Pattern: $pattern\n  ",
                   join( "\n  ", @dirs ), "\n";
        }
    }
    elsif ($type =~ /^f/i) {
        foreach my $pattern (@patterns) {
            my @files = $finder->find_file( $dir, $pattern );
            print "Pattern: $pattern\n  ",
                   join( "\n  ", @files ), "\n";
        }
    }
    else {
        die "type must start with a 'd' or an 'f'"
    }
}



__END__

=head1 NAME

finder.pl - Perl script for testing new code


=head1 SYNOPSIS

 finder.pl


=head1 ABSTRACT

This script will find directories or files that match given patterns.


=head1 OPTIONS

 -d | --dir           directory to be searched
 -t | --type          search type; ('d' or 'f')
 -p | --pattern       search pattern (perl-ish regex)
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: finder.pl 4670 2014-07-24 10:50:59Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010

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
