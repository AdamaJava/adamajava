#!/usr/bin/env perl
#
##############################################################################
#
#  Program:  qverify.pl
#  Author:   John V Pearson
#  Created:  2012-06-27
#
#  A perl program to query the QCMG LIMS and output reports and scripts
#  designed to create merged BAMs suitable for variant calling.
#
#  $Id: qverify.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::FileDir::Finder;
use QCMG::FileDir::QSnpDirRecord;
use QCMG::IO::BamListReader;
use QCMG::IO::MafReader;
use QCMG::IO::MafWriter;
use QCMG::IO::QbpReader;
use QCMG::IO::VcfReader;
use QCMG::QBamMaker::SeqFinalDirectory;
use QCMG::Run::Qbasepileup;
use QCMG::Util::QLog;
use QCMG::Verify::AutoQbasepileup;
use QCMG::Verify::AutoQverify;
use QCMG::Verify::QVerify;
use QCMG::Verify::VoteCounter;

use vars qw( $SVNID $REVISION $VERSION $CMDLINE );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qverify.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMANDS' )
        unless (scalar @ARGV > 0);

    $CMDLINE = join(' ',@ARGV);
    my $mode = shift @ARGV;

    # Each of the modes invokes a subroutine, and these subroutines 
    # are often almost identical.  While this looks like wasteful code 
    # duplication, it is necessary so that each mode has complete 
    # independence in terms of processing input parameters and taking
    # action based on the parameters.

    my @valid_modes = qw( help man version verify autoqbasepileup
                          autoqverify );

    if ($mode =~ /^$valid_modes[0]$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );

    }
    elsif ($mode =~ /^$valid_modes[1]$/i) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }
    elsif ($mode =~ /^$valid_modes[2]$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^$valid_modes[3]/i) {
        verify();
    }
    elsif ($mode =~ /^$valid_modes[4]/i) {
        autoqbasepileup();
    }
    elsif ($mode =~ /^$valid_modes[5]/i) {
        autoqverify();
    }
    else {
        die "qverify mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub verify {

    # Print help for this command if no further params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/VERIFY' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( maf              => '',
                   vcf              => '',
                   indexbams        => [],
                   qbasepileup      => '',
                   bamlist          => '',
                   outfile          => '',
                   mafout           => '',
                   logfile          => '',
                   verbose          => 0 );

    my $results = GetOptions (
           'f|maf=s'              => \$params{maf},               # -f
             'vcf=s'              => \$params{vcf},               # --vcf
           'i|indexbam=s'         =>  $params{indexbams},         # -i
           'q|qbasepileup=s'      => \$params{qbasepileup},       # -q
           'b|bamlist=s'          => \$params{bamlist},           # -b
           'o|outfile=s'          => \$params{outfile},           # -o
             'mafout=s'           => \$params{mafout},            # --mafout
           'l|logfile=s'          => \$params{logfile},           # -l
           'v|verbose+'           => \$params{verbose},           # -v
           );


    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # Print out run params if running in verbose mode
    qlogparams( \%params ) if ($params{verbose});

    my $vf = QCMG::Verify::QVerify->new( %params );
    $vf->write_verified_maf( $params{mafout} );

    qlogend();
}


sub autoqbasepileup {

    # Print help for this command if no further params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/AUTOQBASEPILEUP' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( maf              => '',
                   logfile          => '',
                   verbose          => 0 );

    my $results = GetOptions (
           'f|maf=s'              => \$params{maf},               # -f
           'l|logfile=s'          => \$params{logfile},           # -l
           'v|verbose+'           => \$params{verbose},           # -v
           );

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # Print out run params if running in verbose mode
    qlogparams( \%params ) if ($params{verbose});

    # Print out ENV variable if running under PBS
    qlogenv;

    # It is mandatory to supply a MAF absolute pathname
    die "You must specify a MAF file (-f)\n" unless ( $params{maf} );
    die "The -f parameter must be an absolute pathname\n"
        unless ( $params{maf} =~ /^\// );

    # We are going to "unset" the logfile because the user-supplied
    # logfile is for the output from qverify and we will let the
    # AutoQbasepileup choose a suitable default logfile name for the
    # qbasepileup jave app.
    $params{logfile} = '';

    my $fact = QCMG::Verify::AutoQbasepileup->new( verbose => $params{verbose} );
    $fact->run( $params{maf} );

    qlogend();
}


sub autoqverify {

    # Print help for this command if no further params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/AUTOQVERIFY' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( maf              => '',
                   logfile          => '',
                   verbose          => 0 );

    my $results = GetOptions (
           'f|maf=s'              => \$params{maf},               # -f
           'l|logfile=s'          => \$params{logfile},           # -l
           'v|verbose+'           => \$params{verbose},           # -v
           );

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # Print out run params if running in verbose mode
    qlogparams( \%params ) if ($params{verbose});

    # It is mandatory to supply a MAF absolute pathname
    die "You must specify a MAF file (-f)\n" unless ( $params{maf} );
    die "The -f parameter must be an absolute pathname\n"
        unless ( $params{maf} =~ /^\// );

    my $fact = QCMG::Verify::AutoQverify->new( verbose => $params{verbose} );
    $fact->run( $params{maf} );

    qlogend();
}




__END__

=head1 NAME

qverify.pl - Perl routines for verifying variant calls


=head1 SYNOPSIS

 qverify.pl command [options]


=head1 ABSTRACT

This script outputs reports on QCMG variant calling. It uses a number of
external executables so, depending on which mode you are using, you will
need to run some or all of the following commands in your shell script
or PBS file before calling qverify.pl

 module load samtools
 module load adama/nightly
 module load java/1.7.13


=head1 COMMANDS

 verify          - verify variant calls from MAF
 autoqbasepileup - run qbasepileup given MAF name
 autoqverify     - run qverify given MAF name
 version         - print version number and exit immediately
 help            - display usage summary
 man             - display full man page


=head1 COMMAND DETAILS

=head2 VERIFY

Verify variant calls from a MAF file

 -f | --maf           MAF file containing variant positions to be tested
 -i | --indexbam      BAM used to generate calls in --maf file
 -q | --qbasepileup   qbasepileup file
 -b | --bamlist       list of BAM files - should match those used for -q
 -o | --outfile       file to place output in
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message
 -m | --man           print full man page

=head2 AUTOQBASEPILEUP

Automatically run qbasepileup on a MAF

 -f | --maf           full pathname to MAF file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

=head2 AUTOQVERIFY

Automatically run qverify on a MAF

 -f | --maf           full pathname to MAF file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages


=head1 DESCRIPTION


=head2 verify Mode

 qverify.pl verify -v
    --maf         /dir/panc/APGI_1/variants/qSNP/123456/mymaf.maf
    --qbasepileup /dir/panc/APGI_1/variants/qSNP/123456/mymaf.maf.qbasepileup.txt
    --bamlist     /dir/panc/APGI_1/variants/qSNP/123456/APGI_1.bamlist.txt
    --indexbam    /dir/panc/APGI_1/seq_final/my_normal.bam
    --indexbam    /dir/panc/APGI_1/seq_final/my_tumour.bam
    --mafout      /dir/panc/APGI_1/variants/qSNP/123456/mymaf.qverified.maf
    --outfile     /dir/panc/APGI_1/variants/qSNP/123456/mymaf.qverified.maf.report
    --logfile     /dir/panc/APGI_1/variants/qSNP/123456/mymaf.qverified.maf.log

In order to save on sequencing costs for verification of our variants,
we are trying to do as much in-silico verification as possible.  This
involves doing pileups at variant positions across all BAMs available
for a donor and then trying to use non-Illumina data to corroborate the
HiSeq calls.

This mode assumes that a file listing all of the seq_final
BAMs for this donor is available, and that the java qbasepileup tool has
been run against the MAF file and the bam list.
If these assumptions have not been met, you should probably consider
using the B<autoqbasepileup> mode first and unless you really want to
select different input files, you might consider use the B<autoqverify>
mode rather than this mode.

=head3 Commandline Parameters

=over

=item B<-f | --maf>

MAF file containing the variant positions to be tested.  This file
shoudl have come from variant calling on the pair of BAMs specified by
--normalBam and --tumourBAM.  This file should also have been used to
generate the list of pileups in the --qbasepileup file.

=item B<-i | --indexbam>

This should normally be used twice - once to identify the normal BAM and
once to identify the tumour BAM.

=item B<-q | --qbasepileup>

File output by running the qbasepileup java tool with the positions
in the --maf file against the BAM files in --bamlist.

=item B<-b | --bamlist>

List of BAM files with the properties of the BAMs.

=item B<-o | --outfile>

Output file name.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head2 autoqbasepileup Mode

In order to save on sequencing costs for verification of our variants,
we are trying to do as much in-silico verification as possible.  This
involves doing pileups at variant positions across all BAMs available
for a donor and then trying to use non-Illumina data to corroborate the
HiSeq calls.

=head2 autoqverify Mode

=head3 Commandline Parameters

=over

=item B<-f | --maf>

This must contain an absolutel pathname to a MAF file.  The full
pathname is required because it will be parsed to determine key values,
including project, that are required to correctly sutoset other
parameters such as the list of bams.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qverify.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012,2013

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
