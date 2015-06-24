#!/usr/bin/env perl

##############################################################################
#
#  Program:  seqfinal_bam_properties.pl
#  Author:   John V Pearson
#  Created:  2010-06-24
#
#  This script takes a root directory and finds the name of all BAMs
#  under that directory that sit in a seq_final directory.  For each
#  found BAM, it tries to guess the properties of that BAM, firstly by
#  looking inside the BAM header for the '@CO qlimsmeta
#
#  $Id: seqfinal_bam_properties.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Digest::CRC;
use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;

use QCMG::FileDir::Finder;
use QCMG::QBamMaker::SeqFinalDirectory;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %PARAMS );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: seqfinal_bam_properties.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.  We put these is a single
    # hash so we can pass them around easily if we need to.

    %PARAMS = ( dir        => '',
                outfile    => 'seqfinal_bam_report.txt',
                logfile    => '',
                verbose    => 0,
                version    => 0,
                help       => '',
                man        => 0 );

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'd|dir=s'              => \$PARAMS{dir},           # -d
           'l|logfile=s'          => \$PARAMS{logfile},       # -l
           'o|outfile=s'          => \$PARAMS{outfile},       # -o
           'v|verbose+'           => \$PARAMS{verbose},       # -v
           'version!'             => \$PARAMS{version},       # --version
           'h|help|?'             => \$PARAMS{help},          # -?
           'man|m'                => \$PARAMS{man}            # -m
           );

    if ($PARAMS{version}) {
        print "$SVNID\n";
        exit;
    }
    pod2usage(1) if $PARAMS{help};
    pod2usage(-exitstatus => 0, -verbose => 2) if $PARAMS{man};

    qlogfile($PARAMS{logfile}) if $PARAMS{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");

    die( "a directory to be processed must be specified\n" )
        unless ($PARAMS{dir});

    my $fact = QCMG::QBamMaker::SeqFinalDirectory->new( verbose => 1 );
    my $coll = $fact->process_directory( $PARAMS{dir} );

    my $rh_collections = $coll->new_collections_sorted_by_donor;

    my $coll_ctr = 0;
    foreach my $donor (sort keys %{ $rh_collections }) {
        die "more than one donor found ($donor)\n" if ($coll_ctr > 0);
        my $this_coll = $rh_collections->{$donor};
        qlogprint join( "\t", $donor, $this_coll->record_count ), "\n";
        $this_coll->write_bamlist_file( $PARAMS{outfile} );
        $coll_ctr++;
    }

    qlogend;  # log program exit
}


__END__

=head1 NAME

seq_final_properties.pl - Perl script to list properties of seq_final BAMs


=head1 SYNOPSIS

 seqfinal_bam_properties.pl [options]


=head1 ABSTRACT

This script takes the name of a directory, searches for all BAM files in
the tree below the directory, applies a filter to only keep those files
that lie under a seq_final directory, and then tries to determine the
properties of each BAM file.


=head1 OPTIONS

 -d | --dir           name of directory; Required!
 -o | --outfile       output filename; default=seqfinal_bam_report.txt
 -l | --logfile       logfile name
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<seqfinal_bam_properties.pl> was designed to support creation of BAM
file lists for verification of the PDAC cell lines.  The intent more
generically was to make it easy to parse the properties for each
seq_final BAM out of the new qlimsmeta @CO comment line and to create
a database of all final BAMs.

=head2 Commandline Options

=over

=item B<-d | --dir>

This option must be specified and gives the root directory to be
searched for BAM files.  It must be an absolute path starting from root.
This is mandatory so that the full BAM pathname can be used elsewhere
for processing.  This directory need not be a donor directory although
it can be.  All BAM files below the specified directory are examined but
they only make the final list if the sit within a seq_final directory.
You can specify the root of a study and the list will contain all seq_final
BAMs for the study across all donors.

=item B<-l | --logfile>

By default, error and diagnostic messages are output to STDOUT but if
a logfile is specified, the messages will be written to the logfile 
instead.  This can be particularly useful if the script is intended to
be made available via a web wrapper.

=item B<-o | --outfile>

The output file is a tab-separated plain text with unix-style line
endings.  If no name is supplied for the output file then the default
(seqfinal_bam_report.txt) is used.  The output file contains 7 columns of data:
    
 1.  ID
 2.  Donor
 3.  BamName
 4.  BamHeaderCRC32
 5.  BamCtimeEpoch
 6.  Project
 7.  Material
 8.  Sample Code
 9.  Sequencing Platform
 10. Aligner
 11. Capture Kit
 12. Library Protocol
 13. Sample
 14. Species Reference Genome
 15. Reference Genome File
 16. Failed QC

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.
The default level is 0 so no diagnostic messages are output.  At level
1, a small number of progress-related messages are written.  These
messages will be printed to STDOUT unless the B<-l> option is used to
specify a logfile.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 Example

The following usage example will do blah

  template -i blah.fa -c blah -s 15736064

B<N.B.> The spaces between the options (B<-i>, B<-c> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:jpearson@tgen.org>

=back


=head1 VERSION

$Id: seqfinal_bam_properties.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

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
