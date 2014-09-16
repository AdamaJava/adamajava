#!/usr/bin/env perl

##############################################################################
#
#  Program:  solid_stats_report.pl
#  Author:   John V Pearson
#  Created:  2010-09-10
#
#  This script calls the various QCMG::Bioscope::*Stats.pm modules to parse
#  various statistics files output by the SOLiD sequencers and 
#  Bioscope/LifeScope.
#
#  $Id: solid_stats_report.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

my $HOSTKEY = `hostname`;
chomp $HOSTKEY;
if($HOSTKEY =~ /barrine/ || $HOSTKEY =~ /^[ab][0-9]*[ab][0-9]*/) {
	use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);
}
else {
	# set local Perl environment - this will include the necessary version of 
	#  XML::LibXML (to avoid using the system version which is too old)
	#my $source      = '. /usr/local/mediaflux/perlenv';
	#my $rv          = `$source`;

	use lib qw(/usr/local/mediaflux/perl5/lib/perl5/x86_64-linux-thread-multi/);
	use lib qw(/usr/local/mediaflux/perl5/lib/perl5/);
	use lib qw(/usr/local/mediaflux/QCMGPerl/lib/);
}

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

#use lib qw(/panfs/imb/qcmg/software/QCMGPerl/lib/);

use QCMG::Lifescope::Directory;
use QCMG::Bioscope::MappingStats;
use QCMG::Bioscope::PairingStats;
use QCMG::Bioscope::BarcodeStats;
use QCMG::Bioscope::LogDirectory;
use QCMG::Bioscope::FlowCellRunXML;
use QCMG::FileDir::DirectoryObject;
use QCMG::FileDir::DirectoryReport;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE $LOGFH );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: solid_stats_report.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $runid      = 0;
    my @mapstats   = ();
    my @pairstats  = ();
    my @bcstats    = ();
    my @bslogs     = ();
    my @seqdirs    = ();
    my @lfsdirs    = ();
    my @fcrxmls    = ();
    my $xmlfile    = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'r|runid=i'            => \$runid,         # -r
           'a|mapstat=s'          => \@mapstats,      # -a
           'p|pairstat=s'         => \@pairstats,     # -p
           'b|bcstat=s'           => \@bcstats,       # -b
           'l|bslog=s'            => \@bslogs,        # -l
           'd|seqdir=s'           => \@seqdirs,       # -d
           'c|lfsdir=s'           => \@lfsdirs,       # -c
           'f|fcrxml=s'           => \@fcrxmls,       # -f
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

    #die '-r (--runid) paremeter is compulsory' unless $runid;

    # Allow for ,-seperated lists of inputs
    @mapstats  = map { split /\,/,$_ } @mapstats;
    @pairstats = map { split /\,/,$_ } @pairstats;
    @bcstats   = map { split /\,/,$_ } @bcstats;
    @bslogs    = map { split /\,/,$_ } @bslogs;
    @seqdirs   = map { split /\,/,$_ } @seqdirs;
    @lfsdirs   = map { split /\,/,$_ } @lfsdirs;
    @fcrxmls   = map { split /\,/,$_ } @fcrxmls;

    # Append '/' to each @bslog entry if not already present;
    foreach my $i (0..$#bslogs) {
        $bslogs[$i] .= '/' unless ($bslogs[$i] =~ m/\/$/);
    }

    print( "\nsolid_stats_report.pl  v$REVISION  [" . localtime() . "]\n",
           "   runid          $runid\n",
           '   mapstat(s)     ', join("\n".' 'x18, @mapstats), "\n",
           '   pairstat(s)    ', join("\n".' 'x18, @pairstats), "\n",
           '   bcstat(s)      ', join("\n".' 'x18, @bcstats), "\n",
           '   bslog(s)       ', join("\n".' 'x18, @bslogs), "\n",
           '   seqdir(s)      ', join("\n".' 'x18, @seqdirs), "\n",
           '   lfsdir(s)      ', join("\n".' 'x18, @lfsdirs), "\n",
           '   fcrxml(s)      ', join("\n".' 'x18, @fcrxmls), "\n",
           "   xmlfile        $xmlfile\n",
           "   verbose        $VERBOSE\n\n" ) if $VERBOSE;

    my @objects = ();
    my @dirs    = ();

    foreach my $mapstat (@mapstats) {
        my $obj = QCMG::Bioscope::MappingStats->new( file    => $mapstat, 
                                                     verbose => $VERBOSE );
        push @objects, $obj;
    }
    foreach my $pairstat (@pairstats) {
        my $obj = QCMG::Bioscope::PairingStats->new( file    => $pairstat, 
                                                     verbose => $VERBOSE );
        push @objects, $obj;
    }
    foreach my $bcstat (@bcstats) {
        my $obj = QCMG::Bioscope::BarcodeStats->new( file    => $bcstat, 
                                                     verbose => $VERBOSE );
        push @objects, $obj;
    }
    foreach my $bslog (@bslogs) {
        my $obj = QCMG::Bioscope::LogDirectory->new( directory => $bslog, 
                                                     verbose   => $VERBOSE );
        push @objects, $obj;
    }
    foreach my $fcrxml (@fcrxmls) {
        my $obj = QCMG::Bioscope::FlowCellRunXML->new( file    => $fcrxml, 
                                                       verbose => $VERBOSE );
        push @objects, $obj;
    }
    foreach my $lfsdir (@lfsdirs) {
        my $obj = QCMG::Lifescope::Directory->new( directory => $lfsdir, 
                                                   verbose   => $VERBOSE );
        push @objects, $obj;
    }
    foreach my $seqdir (@seqdirs) {
        my $dirobj = QCMG::FileDir::DirectoryObject->new(
                           name    => $seqdir,
                           parent  => undef,
                           verbose => $VERBOSE );
        $dirobj->process();
        my $obj = QCMG::FileDir::DirectoryReport->new( dir     => $dirobj,
                                                       verbose => $VERBOSE );
        push @dirs, $obj;
    }

    if ($xmlfile) {
        my $xmlfh = IO::File->new( $xmlfile, 'w' );
        die "Cannot open XML file $xmlfile for writing: $!"
            unless defined $xmlfh;

        print $xmlfh '<?xml version="1.0" encoding="UTF-8" standalone="no"?>',
                     "\n<?xml-stylesheet href=\"/includes/xslt/solidstats.xsl\"",
                     " type=\"text/xsl\" ?>\n",
                     "<SolidStatsReport",
                     " runid=\"" . $runid . '"' ,
                     " code_version=\"" . $REVISION . '"' ,
                     " created_by=\"" . $ENV{USER} . '"' ,
                     " creation_date=\"" . localtime() . '"' ,
                     ">\n";
        print $xmlfh run_params_as_xml( $runid, \@mapstats, \@pairstats,
                                        \@bcstats, \@bslogs, \@seqdirs,
                                        \@fcrxmls, \@lfsdirs, $xmlfile );
        foreach my $object (@objects) {
            print $xmlfh $object->as_xml();
        }
        foreach my $dir (@dirs) {
            print $xmlfh $dir->as_xml( 0,
                         [ qw( csfasta qual ma bam bai fasta fai ) ] );
        }
        print $xmlfh "</SolidStatsReport>\n";
        $xmlfh->close;
    }
    else {
        foreach my $object (@objects) {
            print $object->as_xml();
        }
        foreach my $dir (@dirs) {
            print $dir->as_xml( 0,
                        [ qw( csfasta qual ma bam bai fasta fai ) ] );
        }
    }
}

sub run_params_as_xml {
    my $runid        = shift;
    my $ra_mapstats  = shift;
    my $ra_pairstats = shift;
    my $ra_bcstats   = shift;
    my $ra_bslogs    = shift;
    my $ra_seqdirs   = shift;
    my $ra_fcrxmls   = shift;
    my $ra_lsfdirs   = shift;
    my $xmlfile      = shift;

    my $xml = "<SolidStatsReportRunParameters>\n";
    $xml .= "<RunParameter name=\"runid\" value=\"$runid\"/>\n";
    foreach my $value (@{ $ra_mapstats }) {
        $xml .= "<RunParameter name=\"mapstat\" value=\"$value\"/>\n";
    }
    foreach my $value (@{ $ra_pairstats }) {
        $xml .= "<RunParameter name=\"pairstat\" value=\"$value\"/>\n";
    }
    foreach my $value (@{ $ra_bcstats }) {
        $xml .= "<RunParameter name=\"bcstat\" value=\"$value\"/>\n";
    }
    foreach my $value (@{ $ra_bslogs }) {
        $xml .= "<RunParameter name=\"bslog\" value=\"$value\"/>\n";
    }
    foreach my $value (@{ $ra_seqdirs }) {
        $xml .= "<RunParameter name=\"seqdir\" value=\"$value\"/>\n";
    }
    foreach my $value (@{ $ra_fcrxmls }) {
        $xml .= "<RunParameter name=\"fcrxml\" value=\"$value\"/>\n";
    }
    foreach my $value (@{ $ra_lsfdirs }) {
        $xml .= "<RunParameter name=\"lfsdir\" value=\"$value\"/>\n";
    }
    $xml .= "<RunParameter name=\"xmlfile\" value=\"$xmlfile\"/>\n";
    $xml .= "</SolidStatsReportRunParameters>\n";
    return $xml;
}




__END__

=head1 NAME

solid_stats_report.pl - Perl script for capturing key NGS file and directory attributes to XML


=head1 SYNOPSIS

 solid_stats_report.pl [options]


=head1 ABSTRACT

This script calls various perl modules, including 
QCMG::Bioscope::*Stats.pm and QCMG::FileDir::*.pm, to parse
various statistics files and directories output by the SOLiD sequencers
and Bioscope.  It is supposed to be used to collect all of the relevant
information about a single QCMG runid.


=head1 OPTIONS

 -r | --runid         QCMG runid being summarised
 -a | --mapstat       mapping-stats.txt input file
 -p | --pairstat      pairingStats.txt input file
 -b | --bcstat        BarcodeStatistics.*.txt input file
 -l | --bslog         Bioscope log/ directory
 -d | --seqdir        Directory of interest
 -c | --lfsdir        LifeScope output reports directory
 -f | --fcrxml        XML FlowCellRun file from seq_raw
 -x | --xmlfile       XML report file
 -v | --verbose       print diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

B<solid_stats_report.pl_> was designed to support the QCMG production
sequencing team.  There are multiple text report files created during 
SOLiD sequencing, some of them by the sequencers themselves and others
by Bioscope or other programs that are part of the analysis pipeline.
Each text report identified as containing useful information has had a
perl module created to parse it.  These parsing modules are all arranged
under the QCMG::Bioscope hierachy and most have a Stats.pm suffic to the
module name, for example MappingStats.pm and PairingStats.pm.  This
script can also create per-directory size reports.

This script is intended to be run once for each QCMG runid after the
runid has been fully processed through Bioscope to collect
all of the information relevant to that runid.  There is no way to check
that al of the inputs specified do relate to a single runid so this is
the responsibility of the user.

By default an XML report is written to STDOUT but if the optional B<-x>
option is supplied, the XML report is written to the specified filename
instead.
Unless debugging, specifying the XML file output is recommended.


=head2 Commandline Options

=over

=item B<-r | --runid>

The B<-r> option is used to specify a QCMG runid as found in the
icgc_runs log spreadsheet maintained on Google docs by Brooke Gardiner.
If you don't know what a QCMG runid is then it is quite unlikely that 
you have any business running this script.  You have been warned!

=item B<-a | --mapstat>

The B<-p> option is used to specify a Bioscope mapping-stats.txt file
to be processed.

=item B<-p | --pairstat>

The B<-p> option is used to specify a Bioscope pairingStats.txt file
to be processed.

=item B<-b | --bcstat>

The B<-p> option is used to specify a Bioscope BarcodeStatistics.*.txt
file to be processed.

=item B<-l | --bslog>

The B<-l> option is used to specify a Bioscope log directory as creaed
during one or more Bioscope runs.  The log directory is typicaly located
in the same directory as the INI files (because Bioscope won't put it
anywhere else) but it is usually copied to the seq_mapped directory.

=item B<-c | --lfsdir>

The B<-c> option is used to specify a LifeScope reports directory.  This
directory will be parsed for .cht and other files created by LifeScope.
An example of a LifeScope reports directory can be seen on the QCMG wiki
page I<LifeScope> in the section I<Results folder structure (example)>.

=item B<-d | --seqdir>

The B<-d> option is used to specify a directory that will be passed
through a size analysis including reporting on files with key extensions
including .qual, .csfasta, .bam, .bam, .fasta, .fai and .ma.

=item B<-f | --fcrxml>

The B<-f> option is used to specify an XML file produced by the
sequencer itself and which is usually captured in the root directory of
the raw data under seq_raw.  This file describes key attributes of the
run itself.

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

The following usage example will process a pairing report and two
mapping reports and will send the XML report to STDOUT.  Note the
unusual use of '0' as a runid.  This is because this collection of stats
relate to a run that was completed before the instigation of runid's so
it does not have a runid.  The use of 0 is preferable in this case as
runid must be an integer and any positive integer might eventually be
used as a runid although in practical terms, very large numbers 
(>1000000?) are probably also acceptable.
  
  solid_stats_report.pl \
  -r 0
  -p S0014_20090108_1_MC58/20100609/pairing/pairingStats.txt \
  -a S0014_20090108_1_MC58/20100609/F3/s_mapping/mapping-stats.txt \
  -a S0014_20090108_1_MC58/20100609/R3/s_mapping/mapping-stats.txt

B<N.B.> The spaces between the options (B<-p> etc) and the actual
values are compulsory.  If you do not use the spaces then the value will
be ignored.


=head1 SEE ALSO

=over 2

=item perl

=item QCMG::Bioscope::PairingStats

=item QCMG::Bioscope::MappingStats

=item QCMG::Bioscope::BarcodeStats

=item QCMG::Bioscope::LogDirectory

=item QCMG::FileDir::DirectoryObject

=item QCMG::FileDir::DirectoryReport

=item QCMG::Lifescope::Directory

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: solid_stats_report.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010-2012

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
