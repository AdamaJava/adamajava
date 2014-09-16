#!/usr/bin/perl -w

##############################################################################
#
#  Program:  test_mule.pl
#  Author:   John V Pearson
#  Created:  2010-09-29
#
#  Script to act as mule for testing code during module development.
#
#  $Id: test_mule.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::Bioscope::Ini::Parameter;
use QCMG::Bioscope::Ini::ParameterCollection;
use QCMG::DB::Torrent;
use QCMG::FileDir::Finder;
use QCMG::IO::MaReader;
use QCMG::IO::MafReader;
use QCMG::Picard::MarkDupMetrics;
use QCMG::Lifescope::Cht;
use QCMG::Package::PerlCodeFile;
use QCMG::Google::Charts;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: test_mule.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
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

    print( "\ntest_mule.pl  v$REVISION  [" . localtime() . "]\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    #picard_markdup_metrics()
    #mareader_module_test();
    #ini_parameters();
    #filedir_finder();
    #perl_bitwise_ops();
    #test_lifescope_cht_parsing();
    #test_packaging();
    #test_charts();
    #test_qlog();
    #mafreader_module_test();
    torrent_server_api();
}


sub filedir_finder {
    my $finder = QCMG::FileDir::Finder->new( verbose => 1 );
    my @dirs = $finder->find_directory(
    '/panfs/seq_mapped/S0014_20090108_1_MC58', '20' );
    print Dumper $finder;
}


sub ini_parameters {
    my $pc = QCMG::Bioscope::Ini::ParameterCollection->new(
                  name => 'qcmg.F3.params',
                  lines_post => 1,
                  comment => 'Parameters for the R3 reads' );
    $pc->add_parameter( name  => 'primer.set',
                        value => 'R3' );
    $pc->add_parameter( name  => 'read.length',
                        value => '${qcmg.r3.read.length}' );
    $pc->add_parameter( name  => 'mapping.tagfiles',
                        value => '${qcmg.base.dir}/${qcmg.r3.primary.dir}/${qcmg.r3.csfasta}' );
    $pc->add_parameter( name  => 'mapping.output.dir',
                        value => '${output.dir}/R3/s_mapping' );
    $pc->add_parameter( name  => 'tmp.dir',
                        value => '${output.dir}/R3/tmp' );
    $pc->add_parameter( name  => 'intermediate.dir',
                        value => '${output.dir}/R3/intermediate' );
    $pc->add_parameter( name  => 'log.dir',
                        value => '${output.dir}/R3/log' );
    $pc->add_parameter( name  => 'scratch.dir',
                        value => '${qcmg.scratch.dir}/R3' );

    my $pc2 = QCMG::Bioscope::Ini::ParameterCollection->new(
                   name => 'qcmg.global',
                   lines_post => 1,
                   comment => 'The QCMG boss params' );
    $pc2->add_parameter( name  => 'run.name',
                         value => 'S0416_20100819_1_PEBC' );
    $pc2->add_parameter( name  => 'qcmg.date',
                         value => '20100819' );
    $pc2->add_parameter( name  => 'qcmg.panfs.dir',
                         value => '/panfs/imb' );
    $pc2->add_parameter( name  => 'qcmg.hohoho.very.long.parameter.name',
                         value => 'bioscope' );

    $pc->add_parameter_object( $pc2 );
    print Dumper $pc;
    print "\n\nA.\n\n", $pc->as_text();
    print "\n\nB.\n\n", $pc->as_text($pc->name_length);
    print "\n\nC.\n\n", $pc->as_text($pc->name_length,20);
    print "\n\nD.\n\n", $pc->as_text($pc->name_length,75,'      ');
}


sub mareader_module_test {
    my $file = '/panfs/seq_mapped/S0428_20100525_1_LMP/20100704/F3/s_mapping/'.
               'S0428_20100525_1_LMP_Sample1_F3.csfasta.ma';

    print Dumper $file;
    my $mr = QCMG::IO::MaReader->new( file    => $file,
                                      verbose => 1 );

    #print Dumper $mr;

    my $ctr = 0;
    while (my $rec = $mr->next_record) {
       print join( "\t", $mr->recctr,
                         $rec->id, 
                         $rec->id_notag, 
                         $rec->match_count, 
                         join(',',$rec->matches) ), "\n";
       exit if $mr->recctr > 1000;
    }
}


sub picard_markdup_metrics {
    my $file = '/panfs/seq_analysis/APGI_2039/reports/' .
               'APGI_2039_ND_exome.20100625_B.dedup.metrics';

    my $md = QCMG::Picard::MarkDupMetrics->new( file    => $file,
                                                verbose => 1 );

    print $md->as_xml;
}


sub perl_bitwise_ops {
    printf( "%d & %d = %d\n", 255, 0, 255 & 0 );
    printf( "%d & %d = %d\n", 255, 1, 255 & 1 );
    printf( "%d & %d = %d\n", 255, 2, 255 & 2 );
    printf( "%d & %d = %d\n", 255, 3, 255 & 3 );
    printf( "%d & %d = %d\n", 255, 8, 255 & 8 );
    printf( "%d & %d = %d\n", 255, 255, 255 & 255 );
    printf( "%d & %d = %d\n", 131, 1024, 131 & 1024 );
}


sub test_lifescope_cht_parsing {
    my $ROOT = '/panfs/lifescope_results/projects/eglazov/';
    my $RUN  = '1992/S0414_20110326_1_FragPEBC_L20110316_T_ExomeQCMG/';
    my $PATH = $ROOT . $RUN . 'outputs/bamstats/Group_1/';
    #my $file = $PATH . 'Group_1.Coverage.by.Chromosome.GL000249.1.cht';
    my $file = $PATH . 'Group_1.Pairing.Stats.cht';

    my $cht = QCMG::Lifescope::Cht->new( filename => $file );
    print $cht->as_xml;
}


sub test_packaging {
    my $file = $ENV{QCMG_SVN}.'/QCMGPerl/lib/QCMG/Package/PerlCodeFile.pm';

    my $pf = QCMG::Package::PerlCodeFile->new(
                 file    => $file,
                 class   => 'QCMG::Package::PerlCodeFile',
                 verbose => 1 );
    #print Dumper $pf;
    #my $stuff = $pf->code;
    #print $stuff;
    print $pf->code(strip=>1);
    #print $pf->contents;
}


sub test_charts {
    #my $gc = QCMG::Google::Charts->_new( name => 'jp1' );
    my $gc = QCMG::Google::Chart::Line->new( name => 'jp1' );
    print Dumper $gc;
    $gc->title('HoHo');
    print Dumper $gc;
    #print $gc->title."\n";
}


sub test_qlog {
    use QCMG::Util::QLog;
    {
        no warnings;
        print( (defined $MAIN::QLOG) ? "Found :)\n" : "Not found :(\n" );
    }
    print STDERR "Via STDERR\n";
    print STDOUT "Via STDOUT\n";
    print MAIN::QLOG "Via QLOG ($MAIN::QLOG)\n";
    qlogfile('mylog.log');
    print STDERR "Via STDERR 2\n";
    print MAIN::QLOG "Via QLOG 2 ($MAIN::QLOG)\n";
    qlogprint {l=>'INFO'}, "This is an INFO message in mylog.log\n";
    qlogfile('mylog.log2');
    qlogbegin;
    qlogthread( 'thread1' );
    warn "This is a warning\n";
    qlogprint {l=>'INFO'}, "This is an INFO message with hash in mylog.log2\n";
    qlogprint "This is an INFO message without hash in mylog.log2\n";
    print STDERR "Via STDERR 3\n";
    print MAIN::QLOG "Via QLOG 3 ($MAIN::QLOG)\n";
    print Dumper \@QCMG::Util::QLog::LEVELS;
    qlogaddlevel( 'HOHO' , '<DEBUG' );
    qlogaddlevel( 'HOHO2' , '>DEBUG' );
    qlogaddlevel( 'HOHO3' , '<INFO' );
    qlogaddlevel( 'TOOL' , '>INFO' );
    qlogaddlevel( 'EXEC' , '>TOOL' );
    print Dumper \@QCMG::Util::QLog::LEVELS;
    qlogend;
    die "This is a die\n"
}


sub mafreader_module_test {
    my $dir  = '/Users/j.pearson/Desktop/MAFs/';
    my $file = $dir . 'MAF_QCMG_20111005_70samples_highConfidence.txt';
    #my $file = $dir . 'MAF_Baylor_20111005.txt';
    #my $file = $dir . 'MAF_OICR_20111007_strictKK.txt';

    print Dumper $file;
    my $mr = QCMG::IO::MafReader->new( filename => $file,
                                       verbose  => 1 );

    print Dumper $mr;

    my $ctr = 0;
    while (my $rec = $mr->next_record) {
       print join( "\t", $mr->record_ctr,
                         $rec->Hugo_Symbol, 
                         $rec->Center, 
                         $rec->Start_Position,
                         $rec->optional_col('QCMG_flag') ), "\n";
       exit if $mr->record_ctr > 1000;
    }
}

sub torrent_server_api {
    my $ts = QCMG::DB::Torrent->new();
    $ts->connect;

    #print Dumper $ts->query( '' );

    #print Dumper $ts->query( 'experiment/schema/' );
    #print Dumper $ts->query( 'experiment/' );
    #print Dumper $ts->query( 'experiment?format=json&limit=0' );
    # shows 'project' => 'targetseq'

    #print Dumper $ts->query( 'qualitymetrics/schema/' );
    #print Dumper $ts->query( 'qualitymetrics/' );

    #print Dumper $ts->query( 'analysismetrics/schema/' );
    #print Dumper $ts->query( 'analysismetrics/' );
    #print Dumper $ts->query( 'analysismetrics?format=json&limit=0' );

    # This one ties together experiments, qualitymetrics, analysismetrics
    # libmetrics, tfmetrics and fastq and BAM locations
    #print Dumper $ts->query( 'results/schema/' );
    #print Dumper $ts->query( 'results/' );
    #print Dumper $ts->query( 'results/327' );

    #print Dumper $ts->query( 'libmetrics/schema/' );
    #print Dumper $ts->query( 'libmetrics/' );
    print Dumper $ts->query( 'libmetrics/249' );

    # analysismetrics:
    # bead - tf = libLive (and lib_pass_cafie)
    # bead - dud = live
    # live - tf = lib
    # bead + empty + ignored + pinned = (approx) total wells on chip
    #
    # tf = tfLive
    # tfFinal = tfKp
    # libLive = lib_pass_cafie

}

__END__

=head1 NAME

test_mule.pl - Perl script for testing new code


=head1 SYNOPSIS

 test_mule.pl


=head1 ABSTRACT

This script holds test routines for use while developing new modules.


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: test_mule.pl 4669 2014-07-24 10:48:22Z j.pearson $


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
