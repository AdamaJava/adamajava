#!/usr/bin/perl -w

##############################################################################
#
#  Program:  directory_size_report.pl
#  Author:   John V Pearson
#  Created:  2010-06-02
#
#  This script summarizes disk usage by directory and (optionally) file.
#
#  $Id: directory_size_report.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use File::Find;
use POSIX;
use QCMG::FileDir::DirectoryReport;
use QCMG::FileDir::DirectoryObject;
use QCMG::FileDir::FileObject;
use Carp qw( carp croak );

use vars qw( $SVNID $REVISION $VERSION $VERBOSE $LOGFH );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: directory_size_report.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $dir        = './';
    my @extensions = ();
    my $xmlfile    = '';
    my $depth      = 0;
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'd|dir=s'              => \$dir,           # -d
           'e|extension=s'        => \@extensions,    # -e
           'x|xmlfile=s'          => \$xmlfile,       # -x
           'p|depth=i'            => \$depth,         # -p
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

    # Allow for ,-seperated lists of extensions
    @extensions = map { split /\,/,$_ } @extensions;

    # Append '/' to dir if not already present;
    #$dir .= '/' unless ($dir =~ m!/$!);

    print( "\ndirectory_size_report.pl  v$REVISION  [" . localtime() . "]\n",
           "   dir           $dir\n",
           "   depth         $depth\n",
           "   xmlfile       $xmlfile\n",
           '   extension(s)  ', join("\n".' 'x17, @extensions), "\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $dirobj = QCMG::FileDir::DirectoryObject->new(
                       name    => $dir, 
                       parent  => undef,
                       verbose => $VERBOSE );
    $dirobj->process();

    my $reporter = QCMG::FileDir::DirectoryReport->new(
                         dir     => $dirobj,
                         verbose => $VERBOSE );
    print $reporter->text_report_1( $depth, \@extensions );

    if ($xmlfile) {
       my $xmlfh = IO::File->new( $xmlfile, 'w' );
       die "Cannot open XML file $xmlfile for writing: $!"
           unless defined $xmlfh;
       $xmlfh->print('<?xml version="1.0" encoding="UTF-8" standalone="no"?>'."\n");
       $xmlfh->print( $reporter->as_xml( $depth, \@extensions ) );
       $xmlfh->close;
    }

}


__END__

=head1 NAME

directory_size_report.pl - Perl script for summarising directory sizes


=head1 SYNOPSIS

 directory_size_report.pl [options]


=head1 ABSTRACT

This script will report on the sizes of all directories in a tree rooted
at a nominated directory.


=head1 OPTIONS

 -d | --dir           root directory for report
 -p | --depth         maxiumu depth of directories to display
 -e | --extension     extensions of files to be reported
 -x | --xmlfile       XML report file
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<directory_size_report.pl> was designed to support the QCMG production
sequencing team.
It ignores .svn directories which causes it to slightly under-report
directory sizes but this vastly simplifies the output in cases where 
you wish to summarize directories under subversion control.

A text report is always written to STDOUT and an optional XML report is
written to a file if the B<-x> option is specified.  In general,
specifying the verbose flag (B<-v>) and the XML file output is
recommended.

=head2 Commandline Options

=over

=item B<-d | --dir>

If the B<-d> option is specified, it is for the root directory for the
report otherwise the current directory is assumed.  All subdirectories
from the root directory will be included in the report.

=item B<-p | --depth>

This option sets the maximum depth of directories that are displayed in
the report.  Note that it does not limit the directories that are
summarized, just those that are reported so if your directories are 10
deep but only 3 levels are requested for display, the displayed
directories will still show accurate statistics that include every file
and subdirectory below them.  Note that specifying B<-p> wll cause files
sitting in directories below the specified depth to not be reported even
though they have extensions that match those specified using B<-e>.

=item B<-e | --extension>

If specified, this option lists the file extensions that will trigger
reporting of a file.
Multiple extensions are allowed and may be specified 2 ways.  Firstly,
the B<-e> option may be specified multiple times, and secondly each
B<-e> may be followed by a comma-separated list (no spaces!) of
extensions.  You may also mix the 2 modes.
Note that this option can be affected by truncating the depth of the
directory reporting using the B<-p> option - see explanation in B<-p>
section.

=item B<-x | --xmlfile>

Name of XML report file to be written.  Optional but very highly
recommended.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT unless the B<-l> option is used to
specify a logfile.  At level 2, details of all file sizes are reported
in addition to the directories - use this carefully or the report can
very easily blow out in size to the point that it is unreadable.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will list the names and sizes of all
directories under /Users/j.pearson/ but only to 3 directories deep.
  
  directory_size_report.pl -d /Users/j.pearson -x 3

B<N.B.> The spaces between the options (B<-d> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: directory_size_report.pl 4669 2014-07-24 10:48:22Z j.pearson $


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
