package QCMG::Organise::Slide;
################################################################################
#
#    Slide.pm
#
#    Inherits the ability to process a QCMG-style slide
#
#    Authors: Stephen Kazakoff
#
#    $Id$
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

    QCMG::Organise::IngestSlide
    QCMG::Organise::MapSlide
    QCMG::Organise::ScrapeSchema
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

    $self->{'slide'} = basename($self->{'slide_folder'});

    return $self;
}

################################################################################
sub create_qIngest_jobs {
#
# Creates PBS to backup and delete the raw and mapped slides
#
    my $self = shift;

    my $slide = basename($self->{'slide'});
    my $project_dir = $self->{'project_dir'};

    my $script = "$project_dir/pbs/$slide.qIngest.pbs";
    my $pbslog = "$project_dir/log/$slide.qIngest.log";

    my $fh = IO::File->new("> $script");

    die "Could not open file \"$script\": $!\n" unless $fh;

    $self->write_log("--- Creating qIngest job ---");

    print $fh join ("\n",

        '#!/bin/bash',
        '#PBS -N qIngest',
        '#PBS -q mapping',
        '#PBS -r n',
        '#PBS -l ncpus=1,walltime=100:00:00,mem=1gb',
        '#PBS -m ae',
        '#PBS -j oe',
        '#PBS -o ' . $pbslog,
        '#PBS -W umask=0007',
        '#PBS -M s.kazakoff@imb.uq.edu.au',
        '################################################################################',
        '################################################################################',
        '',
        'set -e',
        '',
        'module load QCMGPerl/nightly-build',
        '',
        "slide=\"$slide\"",
        '',
        'geneus="/panfs/home/production/Devel/QCMGProduction/geneus"',
        'gls_client="$geneus/api/python/other_scripts/glsapi_version_1.0.1/gls_client.py"',
        '',
        'qIngest="/share/software/QCMGPerl/distros/qOrganise/qIngest.pl"',
        '',
        '################################################################################',
        '################################################################################',
        '',
        'if [[ ! -d "/mnt/seq_raw/$slide" && ! -d "/mnt/seq_mapped/$slide" ]]; then',
        '',
        '    echo "Could not find the following two directories:"',
        '    echo',
        '    echo "    /mnt/seq_raw/$slide"',
        '    echo "    /mnt/seq_mapped/$slide"',
        '    echo',
        '',
        '    exit 1',
        'fi',
        '',
        'if [ -d "/mnt/seq_mapped/$slide" ]; then',
        '',
        '    array=($(find "/mnt/seq_mapped/$slide" -maxdepth 1 -type d -name "$slide\_rsem" -o -name "$slide\_mapsplice"))',
        '',
        '    for i in $(find "/mnt/seq_mapped/$slide" -maxdepth 1 -type f -name \'*.bam\'); do',
        '',
        '        mapset=$(basename "${i/.bam/}")',
        '',
        '        if [[ "${#array[@]}" -eq 2 ]]; then',
        '',
        '            "$gls_client" -s qcmg-glprod.imb.uq.edu.au mapset_set "$mapset" "Aligner" "bwa;mapsplice;rsem"',
        '        else',
        '',
        '            "$gls_client" -s qcmg-glprod.imb.uq.edu.au mapset_set "$mapset" "Aligner" "bwa"',
        '        fi',
        '    done',
        '',
        '    "$qIngest" -s "$slide" --ingest_raw --ingest_mapped',
        'else',
        '',
        '    "$qIngest" -s "$slide" --ingest_raw',
        'fi',
        '',
        '#rm -rf "/mnt/seq_raw/$slide"',
        '#rm -rf "/mnt/seq_mapped/$slide"',
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh->close;

    push (@{ $self->{'PBS_jobs'} }, $script);

    $self->write_log("Writing: $script");
    $self->write_log("--- Done ---\n");

    return $script;
}

################################################################################
sub get_dependencies {
#
# Returns a job ID or list of dependencies for a given job
#
    my $self = shift;

    my $job = shift;

    my $dep;

    if (
        $self->{'splitBAM_job_dependencies'} &&
        !$self->{'qIngest_job_dependency'} &&
        !$self->{'RNA_dependencies'}
    ) {

        $dep = $self->{'splitBAM_job_dependencies'}{

            $self->{'slide_data'}{

                basename($job, '.bwa.pbs')
            }{
                'bam'
            }
        };
    }

    if (
        $self->{'splitBAM_job_dependencies'} &&
        !$self->{'qIngest_job_dependency'} &&
        $self->{'RNA_dependencies'}
    ) {

        for (keys %{ $self->{'RNA_dependencies'} }) {

            $dep = ($dep ? $dep . ":" : "") . join (":",

                map {

                    $self->{'splitBAM_job_dependencies'}{

                        $self->{'slide_data'}{

                            $_
                        }{
                            'bam'
                        }
                    }
                }
                @{ $self->{'RNA_dependencies'}{$_} }
            );
        }
    }

    if (
        $self->{'RNA_dependencies'} &&
        !$self->{'qIngest_job_dependency'} &&
        (fileparse($job, qr/\.[^\.]+\.pbs/))[2] eq ".qc.pbs"
    ) {

        for (keys %{ $self->{'RNA_dependencies'} }) {

            $dep = ($dep ? $dep . ":" : "") . join (":",

                map {

                    my $job_names = {

                        map {

                            basename($_) => $self->{'job_ids'}{$_};
                        }
                        keys %{ $self->{'job_ids'} }
                    };

                    $job_names->{"$_.bwa.pbs"}
                }
                @{ $self->{'RNA_dependencies'}{$_} }
            );
        }
    }

    $dep = $self->{'BCL2FQ_job_dependency'} if $self->{'BCL2FQ_job_dependency'};

    $dep = $self->{'AdptMsk_job_dependency'} if $self->{'AdptMsk_job_dependency'};

    $dep = $self->{'qIngest_job_dependency'} if $self->{'qIngest_job_dependency'};

    return $dep;
}

################################################################################
sub get_BCL2FQ_job_dependencies {
#
# Collects any BCL2FQ dependencies
#
    my $self = shift;

    my $jobs = $self->{'job_ids'};

    my $dep = join (":",

        map {

            $jobs->{$_}
        }
        keys %$jobs
    );

    $self->{'BCL2FQ_job_dependency'} = $dep;

    return;
}

################################################################################
sub get_AdptMsk_job_dependencies {
#
# Collects any AdptMsk dependencies
#
    my $self = shift;

    my $jobs = $self->{'job_ids'};

    my $dep = join (":",

        map {

            $jobs->{$_}
        }
        keys %$jobs
    );

    $self->{'AdptMsk_job_dependency'} = $dep;

    return;
}

################################################################################
sub get_splitBAM_job_dependencies {
#
# Collects any splitBAM dependencies
#
    my $self = shift;

    my ($splitBAM_jobs, $job_ids) = @_;

    return unless $splitBAM_jobs && $job_ids;

    $self->{'splitBAM_job_dependencies'} = {

        map {

            $splitBAM_jobs->{$_} => $job_ids->{$_}
        }
        keys %$splitBAM_jobs
    };

    return;
}

################################################################################
sub get_qIngest_job_dependencies {
#
# Collects any qIngest dependencies
#
    my $self = shift;

    my $jobs = $self->{'job_ids'};

    my $dep = join (":",

        map {

            $jobs->{$_}
        }
        keys %$jobs
    );

    $self->{'qIngest_job_dependency'} = $dep;

    return;
}

################################################################################
sub find_files {
#
# Finds FASTQ or BAM files
#
    my $self = shift;

    my $type = $self->{'file_type'};

    my ($name, $regex);

    if ($type eq "BAM_files") {

        $name = "BAM";
        $regex = "*.bam";
    }

    if ($type eq "FASTQ_files") {

        $name = "FASTQ";
        $regex = "*.fastq.gz";
    }

    $self->write_log("--- Searching for $name files ---");

    $self->{$type} = [

        map {

            $self->write_log("Found: $_"); $_
        }
        sort
        File::Find::Rule
            ->file
            ->name($regex)
            ->mindepth(1)
            ->maxdepth(1)
            ->in($self->{'slide_folder'})
    ];

    $self->write_log("--- Done ---\n");

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
