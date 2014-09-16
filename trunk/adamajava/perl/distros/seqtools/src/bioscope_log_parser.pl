#!/usr/bin/perl -w

##############################################################################
#
#  Program:  bioscope_log_parser.pl
#  Author:   John V Pearson
#  Created:  2010-06-02
#
#  This script reads through a Bioscope log directory and analyses the
#  main and parameter files for each phase.
#
#  $Id: bioscope_log_parser.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );
use File::Find;
use POSIX;

use QCMG::Bioscope::LogDirectory;
use QCMG::Bioscope::LogModule;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE $LOGFH );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: bioscope_log_parser.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $dir        = './';
    my $xmlfile    = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'd|dir=s'              => \$dir,           # -d
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

    # Append '/' to dir if not already present;
    $dir .= '/' unless ($dir =~ m/\!/$/);

    print( "\nbioscope_log_parser.pl  v$REVISION  [" . localtime() . "]\n",
           "   dir           $dir\n",
           "   xmlfile       $xmlfile\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $logs = QCMG::Bioscope::LogDirectory->new( directory => $dir, 
                                                  verbose   => $VERBOSE );

    if ($xmlfile) {
        my $xmlfh = IO::File->new( $xmlfile, 'w' );
        die "Cannot open XML file $xmlfile for writing: $!"
            unless defined $xmlfh;

        print $xmlfh '<?xml version="1.0" encoding="UTF-8" standalone="no"?>', "\n";
        print $xmlfh $logs->as_xml();
        $xmlfh->close;
    }
    else {
        print $logs->as_xml();
    }


}


__END__

=head1 NAME

bioscope_log_parser.pl - Perl script for parsing Bioscope log directories


=head1 SYNOPSIS

 bioscope_log_parser.pl [options]


=head1 ABSTRACT

This script will parse out the contents of key files from a Bioscope run
log directory.


=head1 OPTIONS

 -d | --dir           Bioscope log directory
 -x | --xmlfile       XML report file
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<bioscope_log_parser.pl> was designed to support the QCMG production
sequencing team. A Bioscope run is typically managed as a
series of Plugins that are run sequentially and where each Bioscope
Plugin launches it’s own jobs and writes it’s own log files.  There are
two files that every module produces - the main and the parameters
files.  This script parses those files to extract useful information and
writes out the results as an XML file.

By default an XML report is written to STDOUT but if the optional B<-x>
option is supplied, the XML report is written to the specified filename
instead.
In general, specifying the XML file output is recommended.

=head2 Commandline Options

=over

=item B<-d | --dir>

The B<-d> option is used to specify the Bioscoep log directory to be
processed.  This option is mandatory.

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
  
  bioscope_log_directory -d S0014_20090108_1_MC58/20100609/log

B<N.B.> The spaces between the options (B<-d> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=item QCMG::Bioscope::LogDirectory

=item QCMG::Bioscope::LogModule

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: bioscope_log_parser.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

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
