#!/usr/bin/perl
################################################################################
#
#    qOrganise.pl
#
#    A tool to help handle sequence data sent in from external collaborators
#
#    Authors: Stephen Kazakoff
#
#    $Id: qOrganise.pl 4711 2014-09-15 05:01:40Z s.kazakoff $
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

use QCMG::Organise::Project;


################################################################################
# # #
# #       SET GLOBALS
#
################################################################################


our $cmd_line = join (" ", @ARGV);
our $version = "0.0.4.3";

our $LA_hiseq = "/QCMG_hiseq";
our $LA_qOrganise = "/QCMG_qOrganise";

our $seq_raw = "/mnt/seq_raw";
our $seq_mapped = "/mnt/seq_mapped";
our $seq_results = "seq_mapped";


my $options = check_params();

our $project = $options->{'project'};
our $csv = $options->{'csv'};

our $sequencing_centre = $options->{'centre'} || 'QCMG';
our $mapping_mode = $options->{'mapping_mode'} || 'small';
our $run_mode = $options->{'run_mode'};

our $date = (split ("_", $project))[0];
our $email = $ENV{'QCMG_EMAIL'};


################################################################################
# # #
# #       MAIN
#
################################################################################


if ($project) {

    run_qOrganise();
}


################################################################################
# # #
# #       CUSTOM SUBS
#
################################################################################
sub run_qOrganise {
#
# Does all the hard work
#
    my $log_extn = ".log";

    my $project_dir = join ("/",

        $seq_raw,
        "qOrganise",
        $project,
    );

    make_path($project_dir, { group => 'QCMG-data' });

    my $op = QCMG::Organise::Project->new(

        'log_extn' => $log_extn,
        'log_file' => "$project_dir/$project$log_extn",

        'project_dir' => $project_dir,
        'namespace' => "$LA_qOrganise/$project",
    );

    $op->get_external_data();

    $op->get_parent_paths();

    $op->check_input_files();

    $op->assign_unique_identifiers();

    $op->get_all_slide_data();

    unless ($run_mode && $run_mode eq "ingest") {

        $op->preprocess_project();

        $op->map_project();
    }

    return if ($run_mode && $run_mode eq "test");

    $op->assign_unique_identifiers();

    $op->calculate_checksums();

    $op->create_namespace();

    $op->ingest_datafiles();

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

        "project|p:s",

        "centre|c:s",

        "mapping_mode|m:s",

        "run_mode|r:s",
    );

    print "Version: $version\n\n" if $options{'version'};
    exec "pod2usage $0" if keys %options == 0 || $options{'help'};

    if (
        exists $options{'project'}
    ) {

        $options{'project'} = fileparse($options{'project'});

        $options{'csv'} = [

            grep {

                basename($_, '.csv') eq $options{'project'}
            }
            @{ QCMG::Organise::ScrapeSchema->get_project_list() }
        ];

        if (scalar @{ $options{'csv'} } != 1) {

            print "\n";
            print "Could not find project '$options{'project'}' in QCMG schema.\n";
            print "Please ensure that the metadata exists in the GenoLogics LIMS.\n";
            print "\n";
            exec "pod2usage $0";
        }

        $options{'csv'} = $options{'csv'}[0];
    }
    else {

        print "\n";
        print "You must supply a qOrganise project name before continuing.\n";
        print "Please use the '--project' option.\n";
        print "\n";
        exec "pod2usage $0";
    }

    if (
        exists $options{'mapping_mode'} &&
        $options{'mapping_mode'} ne "large" &&
        $options{'mapping_mode'} ne "small"
    ) {

        print "\n";
        print "Unrecognized mapping mode selected.\n";
        print "Please select one of the following options:\n";
        print "    --mapping_mode large\n";
        print "    --mapping_mode small\n";
        print "\n";
        exec "pod2usage $0";
    }

    if (
        exists $options{'run_mode'} &&
        $options{'run_mode'} ne "ingest" &&
        $options{'run_mode'} ne "test"
    ) {

        print "\n";
        print "Unrecognized run mode selected.\n";
        print "Please select one of the following options:\n";
        print "    --run_mode ingest\n";
        print "    --run_mode test\n";
        print "\n";
        exec "pod2usage $0";
    }

    return \%options;
}

################################################################################
__DATA__

=head1 NAME

    qOrganise.pl


=head1 DESCRIPTION

    ############################################################################

    We frequently receive sequencing data sent to us from external collaborators
    and this data can come in a variety of formats with varying amounts of
    metadata. We therefore require a process to rename and map these files. To
    standardise this process, an in-house Perl program, called qOrganise, was
    developed. qOrganise makes use of three tables in QCMG schema:

        external_data
        external_data_file
        external_flowcell_run

    For more details regarding these tables, please see the wiki:

        http://10.160.72.46/index.php/Qcmg_schema

    Therefore, one must first import the external data into these tables.


    ############################################################################

    Typical workflow:


    1. Create projects, samples and mapsets in GenoLogics LIMS.

        import_external_data.py --GLS-mode <csv>


    2. Wait at least 15 minutes for the LIMS to update.


    3. Run qOrganise

        qOrganise.pl -c <csv>


    ############################################################################

    Developer's notes regarding file types:


    1. Paired-end FASTQ files

    Currently, qOrganise only supports reading the following extensions:

        '.fastq.gz'

    This data format is the most typical and is processed using the methods
    described above.


    2. Merged BAM files

    Frequently, we recieve large BAM files that require processing. These
    files must be backed up first. They then need to be converted to FASTQ,
    split on 'lane', compressed and ingested. Mapping can then continue,
    along with the ingestion of the output. To save time, qOrganise utilizes
    an all-in-one tool called 'split_BAM_by_lane_into_FASTQ', which lives
    here:

        ~/devel/QCMGScripts/s.kazakoff/split_BAM_by_lane_into_FASTQ

    This tool expects QNAMEs in one of the following formats:

        HWUSI-EAS100R:6:73:941:1973
        EAS139:136:FC706VJ:2:2104:15343:197393


    ############################################################################

    Developer's notes regarding 'modes':


    1. Mapping modes

    'large'  forces BWA onto the 20 large batch nodes
    'small'  forces BWA onto the 16 small batch nodes


    2. Run modes

    The 'ingest' mode is useful for when resuming a qOrganise run that has
    failed during ingestion into LiveArc and the user does not wish to re-
    create and re-run any PBS output.

    The 'test' mode should be run for development and debugging purposes
    only. It will create the PBS output needed to map to the raw input. No
    files will be renamed or ingested using this option.


    ############################################################################

    For more details regarding this pipeline, please see the wiki:

        http://qcmg-wiki.imb.uq.edu.au/index.php/Analysing_External_Sequencing
        http://qcmg-wiki.imb.uq.edu.au/index.php/QOrganise


    ############################################################################

=head1 SYNOPSIS

    qOrganise.pl --project|p STR [--centre|c STR] [--mapping_mode|m STR]
        [--run_mode|r STR] [--help|h] [--version|v]



    --project STR           Specify a project name or recipe sheet (.csv).


    [--centre STR]          Optional. This option can be used to set the 'CN' tag
                            in the BAM header. Setting this field is not strictly
                            checked by any process here at QCMG, so please use
                            cautiously [default: 'QCMG'].

    [--mapping_mode STR]    Optional. This option selects a mapping mode for BWA
                            and must be either: 'large' or 'small'
                            [default: 'small'].

    [--run_mode STR]        Optional. This option selects a run mode for qOrganise
                            must be either 'test' or 'ingest' [default: none].


    [--help]                Optional. Displays this basic usage information.
    [--version]             Optional. Displays this scipt's version number.


    For more detailed help, please see the description.

=head1 COPYRIGHT

Copyright (C) 2013 Stephen Kazakoff
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
