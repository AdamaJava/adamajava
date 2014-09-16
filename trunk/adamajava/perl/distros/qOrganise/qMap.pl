#!/usr/bin/perl
################################################################################
#
#    qMap.pl
#
#    A simple tool to help map some existing slide data
#
#    Authors: Stephen Kazakoff
#
#    $Id: qMap.pl 4712 2014-09-15 05:03:20Z s.kazakoff $
#
################################################################################
# # #
# #       LOAD MODULES AND GET OPTIONS
#
use strict;
use warnings;
################################################################################
################################################################################
$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

BEGIN {
    select STDERR; $| = 1;
    select STDOUT; $| = 1;
}

umask 0007;

use Getopt::Long;
use Data::Dumper;

use Bio::SeqIO;
use Bio::DB::Sam;
use Capture::Tiny ':all';
use Cwd 'abs_path';
use DBI;
use File::Basename;
use File::chdir;
use File::Copy;
use File::Find::Rule;
use File::Spec;
use IO::Handle;
use IO::Tee;
use File::Path 'make_path';
use File::Slurp qw(edit_file read_file);
use File::Temp;
use Math::Round;
use POSIX;
use Sort::Key::Natural qw(natkeysort natsort);

#use lib qw(/panfs/home/skazakoff/devel/QCMGPerl/lib);

use QCMG::Organise::Slide;


################################################################################
# # #
# #       SET GLOBALS
#
################################################################################


our $cmd_line = join (" ", @ARGV);
our $version = "0.0.1.2";

our $LA_hiseq = "/QCMG_hiseq";
our $LA_qOrganise = "/QCMG_qOrganise";

our $seq_raw = "/mnt/seq_raw";
our $seq_mapped = "/mnt/seq_mapped";


my $options = check_params();

our $slide = $options->{'slide'};

our $seq_results = $options->{'type'} || 'seq_mapped';
our $mapping_mode = $options->{'mode'} || 'small';
our $sequencing_centre = $options->{'centre'} || 'QCMG';

our $date = (split ("_", $slide))[0];
our $email = $ENV{'QCMG_EMAIL'};


################################################################################
# # #
# #       MAIN
#
################################################################################


map_slide();


################################################################################
# # #
# #       CUSTOM SUBS
#
################################################################################
sub map_slide {
#
# Does all the hard work
#
    my $slidename = basename($slide);

    my $log_extn = "_map_FASTQs.log";

    my $os = QCMG::Organise::Slide->new(

        'log_extn' => $log_extn,
        'log_file' => "$slide/$slidename$log_extn",

        'slides' => { $slidename => 1 },
        'slide_folder' => $slide,
        'file_type' => "FASTQ_files",
    );

    $os->find_files();

    $os->get_parent_paths();

    $os->get_all_slide_data();

    $os->{'slide_data'} = $os->{'slides'}{$slidename};

    $os->create_BWA_mapping_jobs();

    $os->execute_PBS();

    return;
}

################################################################################
# # #
# #       PARSE OPTIONS
#
################################################################################
sub check_params {
#
# Checks all options at runtime
#
    my @standard_options = ( "help|h+", "version|v+" );
    my %options;

    GetOptions( \%options,

        @standard_options,

        "slide|s:s",

        "type|t:s",

        "mode|m:s",

        "centre|c:s",
    );

    print "Version: $version\n\n" if $options{'version'};
    exec "pod2usage $0" if keys %options == 0 || $options{'help'};

    if (
        !exists $options{'slide'}
    ) {

        print "\n";
        print "You must supply a slide name or path to slide.\n";
        print "Please use the '--slide' option.\n";
        print "\n";
        exec "pod2usage $0";
    }
    else {

        $options{'slide'} = $seq_raw . "/" . basename($options{'slide'});

        unless (-d $options{'slide'}) {

            print "\n";
            print "Could not find slide in: '/mnt/seq_raw'\n";
            print "Please check that the slide exists.\n";
            print "\n";
            exec "pod2usage $0";
        }
    }

    if (
        exists $options{'type'} &&
        $options{'type'} ne "seq_mapped" && 
        $options{'type'} ne "seq_verify"
    ) {

        print "\n";
        print "Unrecognized machine type selected.\n";
        print "Please select one of the following options:\n";
        print "    --type seq_mapped\n";
        print "    --type seq_verify\n";
        print "\n";
        exec "pod2usage $0";
    }

    if (
        exists $options{'mode'} &&
        $options{'mode'} ne "large" &&
        $options{'mode'} ne "small"
    ) {

        print "\n";
        print "Unrecognized mapping mode selected.\n";
        print "Please select one of the following options:\n";
        print "    --mode large\n";
        print "    --mode small\n";
        print "\n";
        exec "pod2usage $0";
    }

    return \%options;
}

################################################################################
__DATA__

=head1 NAME

    qMap.pl


=head1 DESCRIPTION

    ############################################################################

    Quickly maps a slide of interest. It assumes everything's in the LIMS and
    the slide is correctly named and that it exists in '/mnt/seq_raw'. Can
    output to either 'seq_mapped' (HiSeq) or 'seq_verify' (MiSeq).


    ############################################################################

    Developer's notes:

    This script cannot yet process RNASeq data. It will only map using the
    standard procedure (i.e. BWA-backtrack).


    ############################################################################

    For more details regarding this pipeline, please see the wiki:

        http://qcmg-wiki.imb.uq.edu.au/index.php/Analysing_External_Sequencing
        http://qcmg-wiki.imb.uq.edu.au/index.php/QOrganise


    ############################################################################

=head1 SYNOPSIS

    qMap.pl --slide|s STR [--type|t STR] [--mode|m STR] [--centre|c STR]
        [--help|h] [--version|v]



    --slide STR            Specify a slidename of interest.


    [--type STR]           Optional. Select either 'seq_mapped' or 'seq_verify'.
                           [Default: 'seq_mapped'].

    [--mode STR]           Optional. This option selects a mapping mode for BWA and
                           must be either: 'large' or 'small'
                           [default: 'small'].

    [--centre STR]         Optional. This option can be used to set the 'CN' tag
                           in the BAM header. Setting this field is not strictly
                           checked by any process here at QCMG, so please use
                           cautiously [default: 'QCMG'].


    [--help]               Optional. Displays this basic usage information.
    [--version]            Optional. Displays this scipt's version number.


    For more detailed help, please see the description.

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013,2014
Copyright (c) Stephen Kazakoff 2013

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

=cut
