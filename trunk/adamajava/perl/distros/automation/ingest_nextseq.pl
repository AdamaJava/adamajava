#!/usr/bin/env perl
################################################################################
#
#    ingest_nextseq.pl
#
#    Authors: Stephen Kazakoff
#
#    $Id: $
#
#    This software is copyright 2010 by the Queensland Centre for Medical
#    Genomics. All rights reserved. This License is limited to, and you may
#    use the Software solely for, your own internal and non-commercial use for
#    academic and research purposes. Without limiting the foregoing, you may
#    not use the Software as part of, or in any way in connection with the
#    production, marketing, sale or support of any commercial product or
#    service or for any governmental purposes. For commercial or governmental
#    use, please contact licensing@qcmg.org.
#
#    In any work or product derived from the use of this Software, proper
#    attribution of the authors as the source of the software or data must be
#    made. The following URL should be cited:
#
#    http://bioinformatics.qcmg.org/software/
#
################################################################################

use strict;
use warnings;

$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";

BEGIN {

    select STDERR; $| = 1;
    select STDOUT; $| = 1;
}

umask 0007;

################################################################################

#use lib qw(/panfs/home/skazakoff/devel/QCMGPerl/lib/);

use QCMG::Ingest::NextSeq;

our $cmd_line = join (' ', @ARGV);
our $version = "0.0.1.2";

our $LA_ISF = "/ISF";
our $path_to_slide = "/mnt/HiSeqNew/runs";

our $seq_raw = "/mnt/seq_raw";
our $seq_mapped = "/mnt/seq_mapped";

################################################################################

my $options = check_params();

our $seq_results = $options->{'type'} || 'seq_mapped';
our $mapping_mode = $options->{'mode'} || 'super';
our $sequencing_centre = $options->{'centre'} || 'ISF';

################################################################################

use Data::Dumper;
use File::Basename;
use File::Copy;
use File::Path 'make_path';
use Getopt::Long;

sleep 1800;

my $slidename = basename($options->{'slide'});

my $project_dir = join ("/", $seq_raw, $slidename);

if (-d $project_dir) {

    my $existing_project = "$project_dir\_todelete";

    die "Please remove or rename: $existing_project" if -d $existing_project;

    move($project_dir, $existing_project);
}

make_path($project_dir);

my $ns = QCMG::Ingest::NextSeq->new(

    'log_extn' => ".log",
    'log_file' => "$project_dir/$slidename.log",
    'slide' => $options->{'slide'},

    'project_dir' => $project_dir,
    'namespace' => "$LA_ISF/$slidename",
);

$ns->perform_file_check();
$ns->read_samplesheet();

$ns->get_metadata();
$ns->create_FASTQ_basenames();

$ns->create_bcl_to_fastq_job();
$ns->execute_PBS();

$ns->create_AdptMsk_jobs();
$ns->get_BCL2FQ_job_dependencies();
$ns->execute_PBS();

$ns->create_BWA_mapping_jobs();
$ns->get_AdptMsk_job_dependencies();
$ns->execute_PBS();

$ns->create_RNA_mapping_jobs();
$ns->execute_PBS();

$ns->create_qIngest_jobs();
$ns->get_qIngest_job_dependencies();
$ns->execute_PBS();

exit;

##############################################################################
sub check_params {

    my @standard_options = ("help|h+", "version|v+");

    my %options;

    GetOptions( \%options,

        @standard_options,

        "slide|i:s",

        "noemail|n+",
    );

    print "Version: $version\n\n" if $options{'version'};

    exec "pod2usage $0" if keys %options == 0 || $options{'help'};

    unless (exists $options{'slide'}) {

        print "You must specify a slidename to ingest.\n";
        print "Please use the '--slide' option.\n";

        exec "pod2usage $0";
    }

    my $slide_folder = join ('/', $path_to_slide, $options{'slide'});

    if (-d $slide_folder) {

        $options{'slide'} = $slide_folder;
    }
    else {

        print "Could not find the slide folder under: $path_to_slide\n";
        print "Please check that the slidename is correct.\n";

        exec "pod2usage $0";
    }

    return \%options;
}

##############################################################################
__DATA__

=head1 NAME

    ingest_nextseq.pl

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2014

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

=head1 SYNOPSIS

    ingest_nextseq.pl --slide|i STR [--noemail|n] [--help|h] [--version|v]


    -i STR, --slide STR     Specify a slidename to ingest.

    [-n, --noemail]         Optional. Turns off email notifications.


    [--help]                Optional. Displays this basic usage information.
    [--version]             Optional. Displays this scipt's version number.


    For more detailed help, please see the description.

=cut
