#!/usr/bin/perl -w

##############################################################################
#
#  Program:  timelord.pl
#  Author:   John V Pearson
#  Created:  2011-04-10
#
#  Find directories and files.
#
#  $Id: timelord.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use File::Find;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use XML::LibXML;

use QCMG::SeqResults::ActionLibraryMerge;
use QCMG::SeqResults::ReportCheckBam;
use QCMG::SeqResults::ReportInspectBam;
use QCMG::SeqResults::ReportMissing;
use QCMG::SeqResults::Util qw( qmail );
use QCMG::Util::QLog;

our( $SVNID, $REVISION );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: timelord.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {
    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMANDS' ) unless (scalar @ARGV > 0);

    #my $default_dir   = $ENV{'QCMG_PANFS'} . '/seq_results';
    my $default_dir   = '/mnt/seq_results';
    my $default_email = $ENV{'QCMG_EMAIL'};

    my $cmdline = join(' ',@ARGV);
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");

    # Transfer control to package for requested command
    my $command = shift @ARGV;

    if ($command =~ /checkbam/i) {
        my $reporter = QCMG::SeqResults::ReportCheckBam->new();
        $reporter->execute();
        $reporter->output_report();
    }
    elsif ($command =~ /missing/i) {
        my $reporter = QCMG::SeqResults::ReportMissing->new();
        $reporter->execute();
        $reporter->output_report();
    }
    elsif ($command =~ /inspectbams/i) {
        my $reporter = QCMG::SeqResults::ReportInspectBam->new();
        $reporter->execute();
        $reporter->output_report();
    }
    elsif ($command =~ /pertwee/i) {
        my $text = '';

        my $cb = QCMG::SeqResults::ReportCheckBam->new();
        $cb->execute();
        $text .= "\nCHECKBAM:\n\n" .
                 $cb->report_bams_with_invalid_names;
            
        my $ib = QCMG::SeqResults::ReportInspectBam->new();
        $ib->execute();
        $text .= "\nINSPECTBAM:\n\n" .
                 $ib->report_bams_not_aligned_to_reference_v2 . "\n" .
                 $ib->report_bams_without_MD_tags;
        
        my $ms = QCMG::SeqResults::ReportMissing->new();
        $ms->execute();
        $text .= "\nMISSING:\n\n" .
                 $ms->bams_on_disk_but_not_in_lims . "\n" .
                 $ms->bams_in_lims_but_not_on_disk;

        open OUT, '>metebelis3.txt' ||
            die "Unable to open metebelis3.txt for writing $!";
        print OUT $text;
        close OUT;
    }
    elsif ($command =~ /baker/i) {
        baker();
    }
    elsif ($command =~ /librarymerge/i) {
        my $actor = QCMG::SeqResults::ActionLibraryMerge->new();
        $actor->execute();
        #print $actor->{mapsetobj}->mapsets_report;
        #print Dumper $actor;
    }
    elsif ($command =~ /version/i) {
        print "$SVNID\n";
        exit;
    }
    elsif ($command =~ /help/i) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );
    }
    elsif ($command =~ /man/i) {
        pod2usage(-exitstatus => 0, -verbose => 2);
    }
    else {
        warn "command $command is unrecognised\n\n";
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );
    }

    qlogend();
}


sub baker {

    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/BAKER' )
        unless (scalar @ARGV > 0);

    # Set defaults for commandline options from user-supplied values
    my %opts = ( dir     => '/mnt/seq_results',
                 outfile => 'metebelis3.xml',
                 verbose => 0 );

    # Use GetOptions module to parse commandline options
    my $results = GetOptions (
            'd|dir=s'         => \$opts{dir},           # -d
            'o|outfile=s'     => \$opts{outfile},       # -o
            'v|verbose+'      => \$opts{verbose},       # -v
            );

    my $xmldoc  = XML::LibXML::Document->new('1.0');
    my $xmlroot = $xmldoc->createElement('TimelordReport');
    $xmldoc->setDocumentElement( $xmlroot );
    $xmlroot->setAttribute( 'code_version', $REVISION ); 
    $xmlroot->setAttribute( 'creation_date', localtime().'' );
    $xmlroot->setAttribute( 'created_by', $ENV{USER} );

    my $cb = QCMG::SeqResults::ReportCheckBam->new(
                 dir     => $opts{dir},
                 verbose => $opts{verbose} );
    $cb->execute();
    $xmlroot->appendChild( $cb->report_xml_object );
 
    my $ib = QCMG::SeqResults::ReportInspectBam->new(
                 dir     => $opts{dir},
                 verbose => $opts{verbose} );
    $ib->execute();
    $xmlroot->appendChild( $ib->report_xml_object );
 
    my $ms = QCMG::SeqResults::ReportMissing->new(
                 dir     => $opts{dir},
                 verbose => $opts{verbose} );
    $ms->execute();
    $xmlroot->appendChild( $ms->report_xml_object );

    my $outfh = IO::File->new( $opts{outfile}, 'w' );
    die "Unable to open $opts{outfile} for writing $!"
        unless defined $outfh;
    $outfh->print( $xmldoc->toString(1) );
    $outfh->close;
}


__END__

=head1 NAME

timelord.pl - Perl script for processing seq_results directory


=head1 SYNOPSIS

 timelord.pl <command> [options]


=head1 ABSTRACT

This script has multiple commands, each of which is an operation against a
QCMG-style seq_results directory.  Each command has its own distinct set of
commandline options.
This man page is only being maintained to list
available commands and all other documentation including examples and
detailed descriptions of the options has been moved to the QCMG wiki at

 http://qcmg-wiki.imb.uq.edu.au/index.php/timelord.pl


=head1 COMMAND DETAILS

 help         print usage message showing available commands
 man          print full man page
 version      print version number
 checkbam     check that BAM names match QCMG naming conventions
 missing      check for BAMs that are in LIMS but not on disk
 inspectbams  look for BAMS without MD tags or aligned against old genomes


=head2 MISSING

Look for BAMs that are in the LIMS but not on disk and vice versa.  Also
looks for BAM files with @RG lines and checks that the SM and LB fields
match the sample and library IDs found in the LIMS.

 -d | --dir      directory to process
 -o | --outfile  output filename
 -e | --email    send output to specified email address
 -v | --verbose  output progress and diagnostic messages
 -h | --help     print usage message showing available commands


=head2 BAKER

Run modes checkbam, missing and inspectbams and output to an XML report
file ready for visualisation with qvisualise.pl

 -d | --dir      directory to process; default=/mnt/seq_results
 -o | --outfile  output filename; default=metebelis3.xml
 -v | --verbose  output progress and diagnostic messages


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: timelord.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2013

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
