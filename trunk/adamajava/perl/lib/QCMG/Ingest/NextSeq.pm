package QCMG::Ingest::NextSeq;
################################################################################
#
#    QCMG::Ingest::NextSeq
#
#    Authors: Stephen Kazakoff
#
#    $Id$
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

use Cwd 'realpath';
use File::Basename;
use File::Spec;
use File::Path 'make_path';
use File::Copy;
use File::Find::Rule;
use File::Slurp qw(edit_file read_file);
use Text::ParseWords;
use Data::Dumper;
use DBI;
use IO::Zlib;
use Parallel::ForkManager;
use POSIX 'strftime';
use XML::LibXML;

use parent qw(

    QCMG::Organise::Common
    QCMG::Organise::ScrapeSchema
    QCMG::Organise::Slide
);

################################################################################
sub new {

    my ($class, %options) = @_;

    my $self = QCMG::Organise::Common->new(%options);

    bless $self, $class;

    return $self;
}

################################################################################
sub perform_file_check {

    my $self = shift;

    $self->write_log("--- Performing basic file check ---");

    my @files = (

        '',
        '/Data/Intensities/BaseCalls',
        '/InterOp',
        '/SampleSheet.csv',
        '/RunInfo.xml',
        '/RunParameters.xml',
    );

    for (@files) {

        my $f = $self->{'slide'} . $_;

        if (-e $f && -r $f) {

            $self->write_log("Found: $f");
        }
        else {

            $self->log_die("Could NOT find: $f\n");
        }
    }

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub read_samplesheet {

    my $self = shift;

    $self->write_log("--- Reading sample sheet ---");

    my @lines = map {

        s/,*$//g; $_
    }
    grep {
        !/^,*$/
    }
    split (/[\r\n]/,

        read_file($self->{'slide'} . '/SampleSheet.csv')
    );

    my (%sample_sheet, @data, $header);

    for (@lines) {

        if ($_ =~ s/[][]//g) {

            $header = $_;

            next;
        }

        push (@{ $sample_sheet{$header} }, $_);
    }

    $self->{'Reads'} = scalar @{ $sample_sheet{'Reads'} };

    $sample_sheet{'Settings'} = {

        map {

            split ","
        }
        @{ $sample_sheet{'Settings'} }
    };

    my @data_header = map {

        quotewords(',', 0, $_)
    }
    shift @{ $sample_sheet{'Data'} };

    for (@{ $sample_sheet{'Data'} }) {

        my %hash;

        @hash{ @data_header } = quotewords(',', 0, $_);

        push (@data, { %hash });
    }

    $sample_sheet{'Data'} = \@data;

    my $ref = $sample_sheet{'Data'};

    unshift (@$ref, { "Sample_Name" => "Undetermined" });

    for my $i (1 .. $#{ $ref }) {

        my $int = $ref->[$i];

        my ($idx1, $idx2) = (delete $int->{'index'}, delete $int->{'index2'});

        $int->{'Index'} ||=
            $idx1 && $idx2 ? join ("-", $idx1, $idx2) :
            $idx1 && !$idx2 ? $idx1 :
            "nobc";

        $int->{'Sample_Name'} ||= $int->{'Sample_ID'} || $int->{'SampleID'};

        %$int = map {

            $_ => $int->{ $_ }
        }
        keys %$int;

        $self->{'sample_indices'}{$int->{'Sample_Name'}} = $i;

        $self->write_log(
            sprintf('Row: %03d; Sample: %s; Index: %s',
                $i,
                $int->{'Sample_Name'},
                $int->{'Index'}
            )
        );
    }

    $self->{'sample_sheet'} = \%sample_sheet;

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub get_metadata {

    my $self = shift;

    $self->write_log("--- Finding relevant metadata ---");

    my $slide = basename($self->{'slide'});

    $self->get_parent_paths;

    $self->{'slides'} = { $slide => 1 };

    $self->get_all_slide_data;

    $self->{'slide_data'} = delete $self->{'slides'}{$slide};

    delete $self->{'slides'};

    $self->{'slide_folder'} = $self->{'project_dir'};

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub create_bcl_to_fastq_job {

    my $self = shift;

    $self->write_log("--- Creating BCL2FQ2 PBS ---");

    my $slide = $self->{'slide'};
    my $slidename = basename($self->{'slide'});
    my $project_dir = $self->{'project_dir'};

    make_path("$project_dir/pbs", "$project_dir/log");

    my $script = "$project_dir/pbs/$slidename.bcl2fastq.pbs";
    my $pbslog = "$project_dir/log/$slidename.bcl2fastq.log";

    my $fh = IO::File->new("> $script");

    $self->log_die("Could not open file \"$script\": $!\n") unless $fh;

    $self->write_log("Writing: $script");

    print $fh join ("\n",

        '#!/bin/bash',
        '#PBS -N BCL2FQ2',
        '#PBS -q mapping',
        '#PBS -r n',
        '#PBS -l ncpus=8,walltime=24:00:00,mem=15gb',
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
        'module load bcltofastq2/2.1.4',
        '',
        join (' ',
            'bcl2fastq',
            "\\\n" . '    --demultiplexing-threads 4',
            "\\\n" . '    --processing-threads 8',
            "\\\n" . '    --runfolder-dir ' . $slide,
            "\\\n" . '    --output-dir ' . $project_dir,
            "\\\n" . '    --interop-dir',
            "\\\n" . '    $TMPDIR',
        ),
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh->close;

    push (@{ $self->{'PBS_jobs'} }, $script);

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub create_FASTQ_basenames {

    my $self = shift;

    $self->write_log("--- Renaming FASTQ files ---");

    my $slide_data = $self->{'slide_data'};

    for my $mapset (sort {

        $slide_data->{$a}{'sample_name'} cmp $slide_data->{$b}{'sample_name'} ||

        $slide_data->{$a}{'lane'} <=> $slide_data->{$b}{'lane'} ||

        $slide_data->{$a}{'index'} cmp $slide_data->{$b}{'index'}
    }
    keys %$slide_data) {

        my $ref = $slide_data->{$mapset};

        my $sample = $ref->{'sample_name'};

        (my $biospecimen_id = $ref->{'biospecimen_id'}) =~ s/ /_/g;

        my $idx = 'S' . $self->{'sample_indices'}{$sample};
        my $lane = 'L00' . $ref->{'lane'};

        for (1 .. $self->{'Reads'}) {

            my $old = "$sample\_$idx\_$lane\_R$_\_001.fastq.gz";
            my $new = ($ref->{'alignment_required'} eq '1' ?

                "$mapset.$_.fastq.gz" :
                "$biospecimen_id\_$idx\_$lane\_R$_\_001.fastq.gz"
            );

            $self->write_log("$old => $new");

            $self->{'FASTQ_files'}{$old} = $new;
        }
    }

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub create_AdptMsk_jobs() {

    my $self = shift;

    $self->write_log("--- Creating AdptMsk PBS ---");

    my @FASTQs = sort keys %{ $self->{'FASTQ_files'} };

    for (0 .. $#FASTQs) {

        my $f;

        my $fastq = $FASTQs[$_];

        $f=1 if $fastq =~ /_R1_001\.fastq\.gz$/;
        $f=2 if $fastq =~ /_R2_001\.fastq\.gz$/;

        die "Could not get read number from filename\n" unless $f;

        my $rl = int($self->{'sample_sheet'}{'Reads'}[$f - 1]);

        $self->_create_AdptMsk_PBS(
            $_ + 1,
            $fastq,
            $self->{'FASTQ_files'}{$fastq},
            $rl,
        );
    }
    
    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub _create_AdptMsk_PBS() {

    my $self = shift;

    my ($job, $ifq, $ofq, $rl) = @_;

    my $slide = $self->{'slide'};
    my $slidename = basename($self->{'slide'});
    my $project_dir = $self->{'project_dir'};

    make_path("$project_dir/pbs", "$project_dir/log");

    $job = sprintf ("%03d", $job);

    my $script = "$project_dir/pbs/$slidename.adptmsk$job.pbs";
    my $pbslog = "$project_dir/log/$slidename.adptmsk$job.log";

    my $fh = IO::File->new("> $script");

    $self->log_die("Could not open file \"$script\": $!\n") unless $fh;

    $self->write_log("Writing: $script");

    print $fh join ("\n",

        '#!/usr/bin/perl',
        '#PBS -N AdptMsk',
        '#PBS -q mapping',
        '#PBS -r n',
        '#PBS -l ncpus=1,walltime=24:00:00,mem=1gb',
        '#PBS -m ae',
        '#PBS -j oe',
        '#PBS -o ' . $pbslog,
        '#PBS -W umask=0007',
        '#PBS -M s.kazakoff@imb.uq.edu.au',
        '################################################################################',
        '################################################################################',
        '',
        'use strict;',
        'use warnings;',
        '',
        'use File::Copy;',
        'use IO::Zlib;',
        '',
        "chdir \"$project_dir\";",
        '',
        "mask(\"$ifq\", $rl);",
        '',
        join ("\n",
            'move(',
            '    "' . $ifq . '",',
            '    "' . $ofq . '",',
            ');',
        ),
        '',
        'sleep (int(rand(241)) + 60);',
        '',
        '################################################################################',
        '################################################################################',
        'sub mask {',
        '',
        '    my ($file, $rl) = @_;',
        '',
        '    tie *INFILE, "IO::Zlib", $file, "rb";',
        '    tie *OUTPUT, "IO::Zlib", "$file.tmp", "wb9";',
        '',
        '    while (my $seq_header = <INFILE>) {',
        '',
        '        my $seq = <INFILE>;',
        '        my $qual_header = <INFILE>;',
        '        my $qual = <INFILE>;',
        '',
        '        chomp $seq_header;',
        '        chomp $seq;',
        '        chomp $qual_header;',
        '        chomp $qual;',
        '',
        '        $seq .= "N" x ($rl - length ($seq));',
        '        $qual .= "#" x ($rl - length ($qual));',
        '',
        '        print OUTPUT "$seq_header\n$seq\n$qual_header\n$qual\n";',
        '    };',
        '',
        '    close INFILE;',
        '    close OUTPUT;',
        '',
        '    move("$file.tmp", $file);',
        '}',
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh->close;

    push (@{ $self->{'PBS_jobs'} }, $script);

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

