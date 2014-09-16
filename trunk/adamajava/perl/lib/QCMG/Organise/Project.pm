package QCMG::Organise::Project;
################################################################################
#
#    Project.pm
#
#    Contains methods to handle a qOrganise project sheet
#
#    Authors: Stephen Kazakoff
#
#    $Id: Project.pm 4703 2014-09-01 02:06:15Z s.kazakoff $
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

use parent qw(

    QCMG::Organise::Common
    QCMG::Organise::Slide
);


################################################################################
# # #
# #       MAIN
#
################################################################################
sub new {
#
# Define a constructor
#
    my ($class, %options) = @_;

    my $self = QCMG::Organise::Common->new(

        %options,
    );

    bless $self, $class;

    return $self;
}

################################################################################
sub check_input_files {
#
# Checks the readability of the input and determines 'file_type'
#
    my $self = shift;

    my $data = $self->{'external_data'};

    my @BAMs = grep { $_ } map {

        $data->{$_}{'bam'},
        $data->{$_}{'bam_id'}
    }
    keys %$data;

    my @FASTQs = grep { $_ } map {

        $data->{$_}{'fastq1'},
        $data->{$_}{'fastq2'}
    }
    keys %$data;

    my $sample_count = scalar keys %$data;
    my $BAM_count = scalar @BAMs;
    my $FASTQ_count = scalar @FASTQs;

    if ($sample_count == 0) {

        $self->write_log("No samples found.");
        $self->write_log("Please import all external data before continuing.\n");

        $self->fail();
    }

    if (!$BAM_count && !$FASTQ_count) {

        $self->write_log("No FASTQ or BAM files found.");
        $self->write_log("Please import all external data before continuing.\n");

        $self->fail();
    }

    if ($BAM_count && $FASTQ_count) {

        $self->write_log("Cannot process multiple file types simultaneously.");
        $self->write_log("Please evaluate the 'external_data' table before continuing.\n");

        $self->fail();
    }

    if (
        $sample_count * 2 != $BAM_count &&
        $sample_count * 2 != $FASTQ_count
    ) {
        $self->write_log("Odd number of files found.");
        $self->write_log("Please evaluate the 'external_data' table before continuing.\n");

        $self->fail();
    }

    @BAMs = grep { $_ } map { $data->{$_}{'bam'} } keys %$data;

    my $count = scalar @BAMs + scalar @FASTQs;

    if (@BAMs) {

        $self->{'file_type'} = 'BAM_files';

        $self->{ $self->{'file_type'} } = [

            keys %{
                {
                    map {

                        $_ => 1
                    }
                    @BAMs
                }
            }
        ];
    }

    if (@FASTQs) {

        $self->{'file_type'} = 'FASTQ_files';

        $self->{ $self->{'file_type'} } = \@FASTQs;
    }

    for (sort @{ $self->{ $self->{'file_type'} } }) {

        chomp;

        $self->write_log("Checking: $_");

        unless (-r $_) {

            $self->write_log("\nCould not read file:\n$_");
            $self->write_log("Please check the permissions of all files in this project.\n");

            $self->fail();
        }
    }

    $self->write_log("\nChecked: $count file(s).\n");

    return;
}

################################################################################
sub assign_unique_identifiers {
#
# Give each file a universal identifer
#
    my $self = shift;

    my $file_type = $self->{'file_type'};

    my $prefixes = (split ("_", $file_type))[0] . "_prefixes";

    return if $self->{$prefixes};

    $self->write_log("--- Producing unique identifiers ---");

    $self->{$prefixes} = {

        map {

            my $uniq_str = $self->unique_string();

            $self->write_log("Assigning: $_ => $uniq_str");

            $_ => $uniq_str
        }
        @{ $self->{$file_type} }
    };

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub preprocess_project {
#
# Decides how to process the input
#
    my $self = shift;

    my $slides = $self->{'slides'};

    my $file_count = 0;

    make_path(

        $self->{'project_dir'} . "/pbs",
        $self->{'project_dir'} . "/log", {

            group => 'QCMG-data',
        }
    );

    for my $slide (sort keys %$slides) {

        make_path(
            "$main::seq_raw/$slide", {

                group => 'QCMG-data',
            }
        );

        for my $mapset (sort keys %{ $slides->{$slide} }) {

            my $data = $slides->{$slide}{$mapset};

            map {

                $data->{$_} = $self->{'external_data'}{$mapset}{$_}
            }
            grep {

                $self->{'external_data'}{$mapset}{$_}
            }
            qw(bam bam_id fastq1 fastq2);

            if ($data->{'bam'} && $data->{'bam_id'}) {

                $self->{'BAM_array'}
                    ->{ $data->{'bam'} }
                    ->{ $data->{'bam_id'} }
                    ->{ $data->{'lane'} }
                = {
                    'mapset' => $mapset,
                    'slide_path' => "$main::seq_raw/$slide",
                };
            }

            next unless $self->{'file_type'} eq 'FASTQ_files';

            for (qw{fastq1 fastq2}) {

                (my $n = $_) =~ s/fastq//;

                my $x1 = $data->{$_};

                my $x2 = "$main::seq_raw/$slide/$mapset.$n.fastq.gz";

                $self->write_log("Creating: $x2");

                next if ($main::run_mode || -e $x2);

                copy($x1, $x2) or die "Copy failed: $!";

                $file_count++;
            }
        }
    }

    if ($self->{'file_type'} eq 'BAM_files') {

        $self->{'splitBAM_jobs'} = {

            map {

                $file_count++;

                $self->split_BAM_to_FASTQ($_) => $_
            }
            keys %{ $self->{'BAM_array'} }
        };
    }

    $self->write_log("\nCreated: $file_count file(s).\n");

    $self->execute_PBS();

    delete $self->{'BAM_array'};
    delete $self->{'external_data'};

    return;
}

################################################################################
sub split_BAM_to_FASTQ {
#
# Creates PBS to run split_BAM_by_lane_into_FASTQ
#
    my $self = shift;

    my $BAM_file = shift;

    my $project_dir = $self->{'project_dir'};
    my $mapset_data = $self->{'BAM_array'};

    my $mapsets = $mapset_data->{$BAM_file};

    my $prefix = $self->{'BAM_prefixes'}{$BAM_file};

    my $script = "$project_dir/pbs/$prefix.pbs";
    my $pbslog = "$project_dir/log/$prefix.log";

    my $fh = IO::File->new("> $script");

    die "Could not open file \"$script\": $!\n" unless $fh;

    print $fh join ("\n",

        '#!/bin/bash',
        '#PBS -N splitbam2fastqs',
        "#PBS -q mapping",
        '#PBS -r n',
        "#PBS -l ncpus=4,walltime=100:00:00,mem=1gb",
        '#PBS -m ae',
        '#PBS -j oe',
        "#PBS -o $pbslog",
        '#PBS -W umask=0007',
        '#PBS -M s.kazakoff@imb.uq.edu.au',
        '################################################################################',
        '################################################################################',
        '',
        'set -e',
        '',
        'cd "$TMPDIR"',
        "\n",
        'module load splitbam2fastqs/nightly',
        "\n",
        "project=\"$project_dir\"",
        "bamfile=\"$BAM_file\"",
        '',
        '################################################################################',
        '################################################################################',
        'sleep 5000',
        "split_BAM_by_lane_into_FASTQ \"\$bamfile\"",
        '',
        join ("\n",

            map {

                my $qname = $_;

                map {

                    my $lane = $_;

                    my $ref = $mapsets->{$qname}{$lane};

                    my $slide = $ref->{'slide_path'};
                    my $mapset = $ref->{'mapset'};

                    map {

                        join (" ",

                            "mv",
                            "./$qname.lane_$lane.nobc.$_.fastq.gz",
                            "$slide/$mapset.$_.fastq.gz",
                        )
                    } 1 .. 2;
                }
                sort {

                    $a <=> $b
                }
                keys %{ $mapsets->{$qname} }
            }
            natkeysort {

                $_
            }
            keys %$mapsets
        ),
        '',
        'array=($(find . -type f -name \'*.fastq.gz\'))',
        '',
        'if [[ "${array[@]}" ]]; then',
        '',
        '    echo "${#array[@]} extra sample(s) detected:"',
        '',
        '    for i in "${array[@]}"; do',
        '',
        '        mv -v "$i" "$project"',
        '    done',
        '',
        '    echo "These have been moved into: $project"',
        '',
        '    exit 1',
        'fi',
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh->close;

    push (@{ $self->{'PBS_jobs'} }, $script);

    $self->write_log("Writing: $script");

    return $script;
}

################################################################################
sub map_project {
#
# Creates and executes mapping PBS for each slide
#
    my $self = shift;

    for (sort keys %{ $self->{'slides'} }) {

        my $slide_folder = "$main::seq_raw/$_";

        my $log_extn;

        $log_extn = "_map_FASTQs.log" if $self->{'file_type'} eq 'FASTQ_files';
        $log_extn = "_split-BAMs.log" if $self->{'file_type'} eq 'BAM_files';

        my $log_file = "$slide_folder/$_$log_extn";

        my $os = QCMG::Organise::Slide->new(

            'log_extn' => $log_extn,
            'log_file' => $log_file,

            'slide_data' => $self->{'slides'}{$_},
            'slide_folder' => $slide_folder,

            'project_dir' => $self->{'project_dir'},
            'parent_paths' => $self->{'parent_paths'},

            'namespace' => "$main::LA_hiseq/$_",
        );

        $self->write_log("Executing jobs for slide: $_\n");


        ###################################################
        # # #
        # #    Begin mapping using BWA
        #
        ###################################################


        $os->create_BWA_mapping_jobs();

        $os->get_splitBAM_job_dependencies(

            $self->{'splitBAM_jobs'},
            $self->{'job_ids'},
        );

        $os->execute_PBS();


        ###################################################
        # # #
        # #    Execute any RNA mapping jobs
        #
        ###################################################


        $os->create_RNA_mapping_jobs();

        $os->execute_PBS();


        ###################################################
        # # #
        # #    Ingest the 'raw' and 'mapped' output
        #
        ###################################################


        $os->create_qIngest_jobs();

        $os->get_qIngest_job_dependencies();

        $os->execute_PBS();
    }

    return;
}

################################################################################
1;

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
