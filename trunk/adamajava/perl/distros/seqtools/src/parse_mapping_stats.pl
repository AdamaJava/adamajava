#!/usr/bin/perl -w

##############################################################################
#
#  Program:  parse_mapping_stats.pl
#  Author:   John V Pearson
#  Created:  2010-08-12
#
#  This script calls the QCMG::Bioscope::MappingStats.pm module to parse
#  a mapping-stats.txt file.
#
#  $Id: parse_mapping_stats.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::Bioscope::MappingStats;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE $LOGFH );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: parse_mapping_stats.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile     = '';
    my $xmlfile    = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \$infile,        # -i
           'x|xmlfile=s'          => \$xmlfile,       # -x
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

    print( "\nparse_mapping_stats.pl  v$REVISION  [" . localtime() . "]\n",
           "   infile        $infile\n",
           "   xmlfile       $xmlfile\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $ms = QCMG::Bioscope::MappingStats->new( file    => $infile, 
                                                verbose => $VERBOSE );


    if ($xmlfile) {
        my $xmlfh = IO::File->new( $xmlfile, 'w' );
        die "Cannot open XML file $xmlfile for writing: $!"
            unless defined $xmlfh;

        print $xmlfh '<?xml version="1.0" encoding="UTF-8" standalone="no"?>', "\n";
        print $xmlfh $ms->as_xml();
        $xmlfh->close;
    }
    else {
        print $ms->as_xml();
    }
}


__END__

=head1 NAME

parse_mapping_stats.pl - Perl script for parsing Bioscope mapping-stats.txt file


=head1 SYNOPSIS

 parse_mapping_stats.pl [options]


=head1 ABSTRACT

This script will parse out the contents of the mapping-stats.txt file
create bu Bioscope with each .ma alignment file.


=head1 OPTIONS

 -i | --infile        mapping-stats.txt input file
 -x | --xmlfile       XML report file
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<parse_mapping_stats.pl> was designed to support the QCMG production
sequencing team. Along with each .ma file created by Bioscope is a
mapping-stats.txt file which summarizes the reads that aligned along wit
their error profile.

By default an XML report is written to STDOUT but if the optional B<-x>
option is supplied, the XML report is written to the specified filename
instead.
In general, specifying the XML file output is recommended.

=head2 Commandline Options

=over

=item B<-i | --infile>

The B<-i> option is used to specify the Bioscope mapping-stats.txt file
to be processed.  This option is mandatory.

=item B<-x | --xmlfile>

Name of XML report file to be written.  Optional but very highly
recommended.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will process a log directory located under
the current pwd and will send the XML report to STDOUT.
  
  parse_mapping_stats.pl \
  -i S0014_20090108_1_MC58/20100609/s_mapping/mapping-stats.txt

B<N.B.> The spaces between the options (B<-i> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=item QCMG::Bioscope::MappingStats

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: parse_mapping_stats.pl 4669 2014-07-24 10:48:22Z j.pearson $


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
