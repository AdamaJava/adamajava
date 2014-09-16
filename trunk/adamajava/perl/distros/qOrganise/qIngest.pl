#!/usr/bin/perl
################################################################################
#
#    qIngest.pl
#
#    A tool to ingest the raw and mapped slides created by qOrganise
#
#    Authors: Stephen Kazakoff
#
#    $Id: qIngest.pl 4703 2014-09-01 02:06:15Z s.kazakoff $
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
our $seq_results = "seq_mapped";


my $options = check_params();

our $email = $ENV{'QCMG_EMAIL'};


################################################################################
# # #
# #       MAIN
#
################################################################################


for my $slide ($options->{'raw_slide'}, $options->{'mapped_slide'}) {

    next unless $slide;

    ingest_slide($slide);

    for (qw(rsem mapsplice rna-seqc)) {

        ingest_RNA_sub_projects($slide, $_);
    }
}


################################################################################
# # #
# #       CUSTOM SUBS
#
################################################################################
sub ingest_slide {
#
# Can ingest a raw or mapped slide
#
    my $slide = shift;

    my $slidename = basename($slide);
    my $directory = dirname($slide);

    my $namespace = "$LA_hiseq/$slidename";

    my ($log_extn, $files, $ns);

    if ($directory eq $seq_raw) {

        $log_extn = "_raw-ingest.log";
        $files = "FASTQ_files";
        $ns = $namespace;
    }

    if ($directory eq $seq_mapped) {

        $log_extn = "_map-ingest.log";
        $files = "BAM_files";
        $ns = "$namespace/$slidename\_seq_mapped";
    }

    my $bam_log = "$slide/$slidename\_split-BAMs.log";
    my $map_log = "$slide/$slidename\_map_FASTQs.log";

    my $os = QCMG::Organise::Slide->new(

        'log_extn' => $log_extn,
        'log_file' => "$slide/$slidename$log_extn",

        'slide_folder' => $slide,
        'file_type' => $files,
        'namespace' => $ns,
    );

    $os->find_files();

    $os->calculate_checksums();

    $os->create_namespace();

    if ($files eq "BAM_files") {

        $os->ingest_folder("$slide/log");
        $os->ingest_folder("$slide/pbs");
    }

    if (-e $bam_log) {

        $os->ingest_file($bam_log);
    }

    if (-e $map_log) {

        $os->ingest_file($map_log);
    }

    $os->ingest_datafiles();

    return;
}

################################################################################
sub ingest_RNA_sub_projects {
#
# Ingests the RSEM, MapSplice and RNA-SeQC project folders under each slide
#
    my $slide = shift;
    my $project = shift;

    my $slidename = basename($slide);
    my $directory = dirname($slide);

    my $dir_name = "$slidename\_$project";
    my $slide_dir = "$slide/$dir_name";
    my $log_extn = "_$project-ingest.log";

    return unless -e $slide_dir;

    my $os = QCMG::Organise::Slide->new(

        'log_extn' => $log_extn,
        'log_file' => "$slide/$dir_name/$slidename$log_extn",

        'slide_folder' => "$slide/$dir_name",
        'file_type' => "BAM_files",
        'namespace' => "$LA_hiseq/$slidename/$dir_name",
    );

    $os->find_files();

    $os->calculate_checksums();

    $os->create_namespace();

    for (
        File::Find::Rule
            ->directory
            ->mindepth(1)
            ->maxdepth(1)
            ->in($slide_dir)
    ) {

        $os->ingest_folder($_);
    }

    for (
        File::Find::Rule
            ->file
            ->mindepth(1)
            ->maxdepth(1)
            ->not_name('*.bam', '*ingest.log')
            ->in($slide_dir)
    ) {

        $os->ingest_file($_);
    }

    $os->ingest_datafiles();

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

        "ingest_raw+",

        "ingest_mapped+",
    );

    print "Version: $version\n\n" if $options{'version'};
    exec "pod2usage $0" if keys %options == 0 || $options{'help'};

    if (
        !exists $options{'slide'}
    ) {

        print "\n";
        print "No slide name provided.\n";
        print "Please use the '--slide' option.\n";
        print "\n";
        exec "pod2usage $0";
    }
    else {

        $options{'slide'} = basename($options{'slide'});
    }

    if (
        !exists $options{'ingest_raw'} &&
        !exists $options{'ingest_mapped'}
    ) {

        print "No ingestion options provided.\n";
        print "Please select one or more of the following:\n\n";
        print "    --ingest_raw\n";
        print "    --ingest_mapped\n";
        print "\n";
        exec "pod2usage $0";
    }

    if (
        exists $options{'ingest_raw'}
    ) {
        $options{'raw_slide'} = $seq_raw . "/" . $options{'slide'};

        unless (-d $options{'raw_slide'}) {

            print "\n";
            print "Could not find slide in: '$seq_raw'\n";
            print "Please check that the slide exists in this directory.\n";
            print "\n";
            exec "pod2usage $0";
        }
    }

    if (
        exists $options{'ingest_mapped'}
    ) {
        $options{'mapped_slide'} = $seq_mapped . "/" . $options{'slide'};

        unless (-d $options{'mapped_slide'}) {

            print "\n";
            print "Could not find slide in: '$seq_mapped'\n";
            print "Please check that the slide exists in this directory.\n";
            print "\n";
            exec "pod2usage $0";
        }
    }

    delete $options{'slide'};

    return \%options;
}

################################################################################
__DATA__

=head1 NAME

    qIngest.pl


=head1 DESCRIPTION

    ############################################################################

    We frequently receive sequencing data sent to us from external collaborators
    and this data can come in a variety of formats with varying amounts of
    metadata. A program, called qOrganise, was developed to process this data.
    Whilst qOrganise can rename and map this data, another program, called
    qIngest, can be used to backup the files both pre- and post-mapping.


    ############################################################################

    Developer's notes:

    qOrganise will ingest each of the files in the recipe sheet. qIngest can
    ingest each of the FASTQ files that have been renamed and copied into
    '/mnt/seq_raw' using the '--ingest_raw' option. qIngest can also ingest each
    of the BAM files that have been mapped into '/mnt/seq_mapped' using the
    '--ingest_mapped' option. Note that qOrganise also copies these BAM files
    into '/mnt/seq_results' under the appropriate parent project.


    ############################################################################

    Important:

    If you have manipulated the raw data you've been sent on disk for your
    qOrganise project, you will need to manually back up these raw files!


    ############################################################################

    For more details regarding this pipeline, please see the wiki:

        http://qcmg-wiki.imb.uq.edu.au/index.php/Analysing_External_Sequencing
        http://qcmg-wiki.imb.uq.edu.au/index.php/QOrganise


    ############################################################################

=head1 SYNOPSIS

    qIngest.pl --slide|s STR [--ingest_raw] [--ingest_mapped] [--help|h]
        [--version|v]



    --slide|s STR           Specify the name or path to a slide of interest.


    [--ingest_raw]          Optional. Ingests the raw slide into LiveArc.

    [--ingest_mapped]       Optional. Ingests the mapped slide into LiveArc.


    [--help -h]             Optional. Displays this basic usage information.
    [--version -v]          Optional. Displays this scipt's version number.


    For more detailed help, please see the description.

=head1 COPYRIGHT

Copyright (c) 2013 Stephen Kazakoff
Copyright (c) The University of Queensland 2013,2014

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
