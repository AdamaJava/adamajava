package QCMG::Organise::MapSlide;
################################################################################
#
#    MapSlide.pm
#
#    Common methods for mapping a QCMG-style slide
#
#    Authors: Stephen Kazakoff
#
#    $Id$
#
################################################################################
# # #
# #       SET DEFAULTS
#
use strict;
use warnings;
################################################################################
################################################################################
$ENV{'PERL5LIB'} = "/share/software/QCMGPerl/lib/";
(
    my $version = '$Id$'
)
    =~ s/.*Id: (?:\S+) (\d+).*/$1/;

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


################################################################################
# # #
# #       MAIN
#
################################################################################
sub create_BWA_mapping_jobs {
#
# Uses slide metadata to create the PBS to run BWA
#
    my $self = shift;

    $self->write_log("--- Creating BWA mapping jobs ---");

    unless (defined $self->{'slide_data'}) {

        $self->write_log("Could not find slide data.\n");

        $self->fail();
    }

    for (natkeysort { $_ } keys %{ $self->{'slide_data'} }) {

        my $ref = $self->{'slide_data'}{$_};

        $ref->{'genome_file'} ||= $self->{'default_genome'};

        (my $sample_name = $ref->{'sample_name'}) =~ s/-//g;

        unless ($ref->{'alignment_required'} eq '1') {

            $self->write_log("Alignment not required for: $_");

            next;
        }

        my $mapping_vars = {

            'genome' => $ref->{'genome_file'},

            'fastq1' => $self->{'slide_folder'} . "/$_.1.fastq.gz",
            'fastq2' => $self->{'slide_folder'} . "/$_.2.fastq.gz",

            'seqres' => join ("/",

                $self->{'parent_paths'}{ $ref->{'parent_prefix'} },
                $ref->{'name'},
                $main::seq_results,
            ),

            'id' => $self->unique_string(),
            'cn' => $main::sequencing_centre,
            'ds' => $ref->{'sample_name'},
            'lb' => $ref->{'primary_library'},
            'pl' => "ILLUMINA",
            'pu' => "lane_" . $ref->{'lane'} . "." . $ref->{'index'},
            'sm' => $ref->{'name'},
        };

        my $job_type;

        if ($ref->{'material'} eq "2:RNA") {

            push (@{ $self->{'RNA_jobs'}{$ref->{'name'} . "_$sample_name"} },

                $mapping_vars
            );

            $job_type = "RNA";
        }
        else {

            $job_type = "DNA";
        }

        push (@{ $self->{'PBS_jobs'} },

            $self->create_BWA_backtrack_PBS($job_type, $mapping_vars)
        );
    }

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub create_BWA_MEM_PBS {
#
# Defines the BWA-MEM PBS template
#
    my $self = shift;

    my $type = shift;
    my $vars = shift;

    my $bwa = $self->get_mapping_profile(

        $type eq "RNA" ? "small" : undef
    );

    my $queue = $bwa->{'queue'};
    my $nodes = $bwa->{'nodes'};
    my $ppn = $bwa->{'ppn'};
    my $threads = $bwa->{'threads'};
    my $memory = $bwa->{'memory'};
    my $max_mem_per_thread = $bwa->{'max_memory_per_thread'};
    my $walltime = $bwa->{'walltime'};

    my $genome = $vars->{'genome'};
    my $fastq1 = $vars->{'fastq1'};
    my $fastq2 = $vars->{'fastq2'};
    my $seqres = $vars->{'seqres'};

    my ($id, $cn, $ds, $lb, $pl, $pu, $sm) = @{ $vars }{

        qw(id cn ds lb pl pu sm)
    };

    my $slide = basename($self->{'slide'});

    my $seq_mapped = "$main::seq_mapped/$slide";

    my $seq_mapped_pbs = "$seq_mapped/pbs";
    my $seq_mapped_log = "$seq_mapped/log";

    make_path(

        $seq_mapped_pbs,
        $seq_mapped_log, {

            group => 'QCMG-data',
        }
    );

    my $script = "$seq_mapped_pbs/$slide.$pu.bwa.pbs";

    my $fh = IO::File->new("> $script");

    die "Could not open file \"$script\": $!\n" unless $fh;

    print $fh join ("\n",

        '#!/bin/bash',
        '#PBS -N BWA-MEM',
        "#PBS -q $queue",
        '#PBS -r n',
        "#PBS -l nodes=1:$nodes:ppn=$ppn,walltime=$walltime,mem=$memory",
        '#PBS -m ae',
        '#PBS -j oe',
        "#PBS -o $seq_mapped_log/$slide.$pu.bwa.log",
        '#PBS -W umask=0007',
        '#PBS -M s.kazakoff@imb.uq.edu.au',
        '################################################################################',
        '################################################################################',
        '',
        'set -e',
        '',
        "workdir=\"/scratch/$slide.$pu\"",
        '',
        'mkdir -p "$workdir"',
        'cd "$workdir"',
        "\n",
        'module load bwa/0.7.6a',
        'module load samtools/0.1.19',
        'module load picard/1.97',
        'module load java/1.7.45',
        "\n",
        "genome=\"$genome\"",
        '',
        "fastq1=\"$fastq1\"",
        "fastq2=\"$fastq2\"",
        '',
        "sample=\"$slide.$pu\"",
        '',
        "seq_mapped=\"$seq_mapped\"",
        "seq_results=\"$seqres\"",
        '',
        "id=\"$id\"",
        "cn=\"$cn\"",
        "ds=\"$ds\"",
        "lb=\"$lb\"",
        "pl=\"$pl\"",
        "pu=\"$pu\"",
        "sm=\"$sm\"",
        '',
        '################################################################################',
        '################################################################################',
        '',
        join (' ',
            "bwa mem -t $threads",
            "\\\n" . '    -R "@RG\tID:$id\tCN:$cn\tDS:$ds\tLB:$lb\tPL:$pl\tPU:$pu\tSM:$sm"',
            "\\\n" . '    "$genome"',
            "\\\n" . '    ' . ($fastq1 =~ /\.bz2$/ ? '<(bzip2 -dc "$fastq1")' : '"$fastq1"'),
            "\\\n" . '    ' . ($fastq2 =~ /\.bz2$/ ? '<(bzip2 -dc "$fastq2")' : '"$fastq2"'),
            "\\\n" . "    | samtools view -@ $threads -uS -",
            "\\\n" . "    | samtools sort -@ $threads -m $max_mem_per_thread - \"\$sample.mapped\""
        ),
        '',
        "echo 'BWA-MEM complete!'",
        '',
        join (' ',
            'java -Xmx8g -jar "$PICARD_HOME/MarkDuplicates.jar"',
            "\\\n" . '    I="$sample.mapped.bam"',
            "\\\n" . '    O="$sample.bam"',
            "\\\n" . '    M="$sample.metric"',
            "\\\n" . '    AS=true',
            "\\\n" . '    VALIDATION_STRINGENCY=LENIENT'
        ),
        '',
        "echo 'MarkDuplicates complete!'",
        '',
        'chmod 664 "$sample.bam"',
        '',
        'cp "$sample.bam" "$seq_mapped"',
        'cp "$sample.bam" "$seq_results"',
        '',
        'rm -rf "$workdir"',
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh->close;

    $self->write_log("Writing: $script");

    return $script;
}

################################################################################
sub create_BWA_backtrack_PBS {
#
# Defines the BWA-backtrack PBS template
#
    my $self = shift;

    my $type = shift;
    my $vars = shift;

    my $bwa = $self->get_mapping_profile(

        $type eq "RNA" ? "small" : undef
    );

    my $queue = $bwa->{'queue'};
    my $nodes = $bwa->{'nodes'};
    my $ppn = $bwa->{'ppn'};
    my $threads = $bwa->{'threads'};
    my $memory = $bwa->{'memory'};
    my $max_mem_per_thread = $bwa->{'max_memory_per_thread'};
    my $walltime = $bwa->{'walltime'};

    my $genome = $vars->{'genome'};
    my $fastq1 = $vars->{'fastq1'};
    my $fastq2 = $vars->{'fastq2'};
    my $seqres = $vars->{'seqres'};

    my ($id, $cn, $ds, $lb, $pl, $pu, $sm) = @{ $vars }{

        qw(id cn ds lb pl pu sm)
    };

    my $slide = basename($self->{'slide'});

    my $seq_mapped = "$main::seq_mapped/$slide";

    my $seq_mapped_pbs = "$seq_mapped/pbs";
    my $seq_mapped_log = "$seq_mapped/log";

    make_path(

        $seq_mapped_pbs,
        $seq_mapped_log, {

            group => 'QCMG-data',
        }
    );

    my $script = "$seq_mapped_pbs/$slide.$pu.bwa.pbs";

    my $fh = IO::File->new("> $script");

    die "Could not open file \"$script\": $!\n" unless $fh;

    print $fh join ("\n",

        '#!/bin/bash',
        '#PBS -N BWA-backtrack',
        "#PBS -q $queue",
        '#PBS -r n',
        "#PBS -l nodes=1:$nodes:ppn=$ppn,walltime=$walltime,mem=$memory",
        '#PBS -m ae',
        '#PBS -j oe',
        "#PBS -o $seq_mapped_log/$slide.$pu.bwa.log",
        '#PBS -W umask=0007',
        '#PBS -M s.kazakoff@imb.uq.edu.au',
        '################################################################################',
        '################################################################################',
        '',
        'set -e',
        '',
        "sample=\"$slide.$pu\"",
        'workdir="/scratch/$sample"',
        '',
        'mkdir -p "$workdir"',
        'cd "$workdir"',
        "\n",
        'module load bwa/0.6.2-mt',
        'module load samtools/0.1.19',
        'module load picard/1.97',
        'module load java/1.7.45',
        "\n",
        "genome=\"$genome\"",
        '',
        "fastq1=\"$fastq1\"",
        "fastq2=\"$fastq2\"",
        '',
        "seq_mapped=\"$seq_mapped\"",
        "seq_results=\"$seqres\"",
        '',
        "id=\"$id\"",
        "cn=\"$cn\"",
        "ds=\"$ds\"",
        "lb=\"$lb\"",
        "pl=\"$pl\"",
        "pu=\"$pu\"",
        "sm=\"$sm\"",
        '',
        '################################################################################',
        '################################################################################',
        '',
        join (' ',
            "bwa aln -t $threads",
            '"$genome"',
            ($fastq1 =~ /\.bz2$/ ? '<(bzip2 -dc "$fastq1")' : '"$fastq1"'),
            '>',
            '"$sample.1.sai"'
        ),
        '',
        join (' ',
            "bwa aln -t $threads",
            '"$genome"',
            ($fastq2 =~ /\.bz2$/ ? '<(bzip2 -dc "$fastq2")' : '"$fastq2"'),
            '>',
            '"$sample.2.sai"'
        ),
        '',
        join (' ',
            "bwa sampe -t $threads",
            "\\\n" . '    -r "@RG\tID:$id\tCN:$cn\tDS:$ds\tLB:$lb\tPL:$pl\tPU:$pu\tSM:$sm"',
            "\\\n" . '    "$genome"',
            "\\\n" . '    "$sample.1.sai"',
            "\\\n" . '    "$sample.2.sai"',
            "\\\n" . '    ' . ($fastq1 =~ /\.bz2$/ ? '<(bzip2 -dc "$fastq1")' : '"$fastq1"'),
            "\\\n" . '    ' . ($fastq2 =~ /\.bz2$/ ? '<(bzip2 -dc "$fastq2")' : '"$fastq2"'),
            "\\\n" . "    | samtools view -@ $threads -uS -",
            "\\\n" . "    | samtools sort -@ $threads -m $max_mem_per_thread - \"\$sample.mapped\""
        ),
        '',
        '################################################################################',
        '################################################################################',
        '',
        'qCleanup=/share/software/QCMGPerl/distros/qOrganise/qCleanup.pl',
        '',
        'host=$("$qCleanup" -s "$sample" -m "$seq_mapped" -r "$seq_results")',
        '',
        'scp -rpqv "$workdir" "$node:/scratch"',
        '',
        'rm -rf "$workdir"',
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh->close;

    $self->write_log("Writing: $script");

    return $script;
}

################################################################################
sub create_RNA_mapping_jobs {

    my $self = shift;

    return unless $self->{'RNA_jobs'};

    $self->write_log("--- Creating RNA mapping jobs ---");

    push (@{ $self->{'PBS_jobs'} },

        map {

            $self->{'RNA_dependencies'}{$_} = [

                map {

                    $self->{'slide'} . "." . $_->{'pu'}
                }
                @{ $self->{'RNA_jobs'}{$_} }
            ];

            $self->create_RNA_SeQC_PBS($_),
            $self->create_RSEM_PBS($_),
            $self->create_MapSplice_PBS($_)
        }
        natkeysort {

            $_
        }
        keys %{ $self->{'RNA_jobs'} }
    );

    $self->write_log("--- Done ---\n");

    return;
}   

################################################################################
sub create_RNA_SeQC_PBS {
#
# Defines the RNA-SeQC PBS template
#
    my $self = shift;

    my $code = shift;

    my $queue = 'mapping';
    my $ncpus = 8;
    my $memory = '15gb';
    my $walltime = '72:00:00';

    my $data = $self->{'RNA_jobs'}{ $code };

    my $slide = $self->{'slide'};
    my $sample_name = "$slide.nopd.$code";

    my $seq_mapped = "$main::seq_mapped/$slide";
    my $seq_results = dirname($data->[0]{'seqres'});

    my $seq_mapped_pbs = "$seq_mapped/pbs";
    my $seq_mapped_log = "$seq_mapped/log";

    my $script1 = "$seq_mapped_pbs/$sample_name.qc.txt";

    my $fh1 = IO::File->new("> $script1");

    die "Could not open file \"$script1\": $!\n" unless $fh1;

    print $fh1 join ("\t",

        "Sample ID",
        "Bam File",
        "Notes",
    ) . "\n";

    for (@$data) {

        print $fh1 join ("\t",

            $slide . "." . $_->{'pu'},
            $_->{'seqres'} . "/$slide" . "." . $_->{'pu'} . ".bam",
            $code,
        ) . "\n";
    }

    $fh1->close;

    $self->write_log("Writing: $script1");

    my $script2 = "$seq_mapped_pbs/$sample_name.qc.pbs";

    my $fh2 = IO::File->new("> $script2");

    die "Could not open file \"$script2\": $!\n" unless $fh2;

    print $fh2 join ("\n",

        '#!/bin/bash',
        '#PBS -N RNA_SeQC',
        "#PBS -q $queue",
        '#PBS -r n',
        "#PBS -l ncpus=$ncpus,walltime=$walltime,mem=$memory",
        '#PBS -m ae',
        '#PBS -j oe',
        "#PBS -o $seq_mapped_log/$sample_name.qc.log",
        '#PBS -W umask=0007',
        '#PBS -M s.kazakoff@imb.uq.edu.au',
        '################################################################################',
        '################################################################################',
        '',
        'set -e',
        "\n",
        'module load RNA-SeQC/1.1.7',
        "\n",
        "sample=\"$sample_name\"",
        '',
        "sample_file=\"$script1\"",
        'genome_file="' . $self->{'default_genome'} . '"',
        '',
        "seq_mapped=\"$seq_mapped/$slide\_rna-seqc/\$sample\"",
        "seq_results=\"$seq_results/rna_seq/rna-seqc\"",
        '',
        'GC_file="' . $self->{'default_GC'} . '"',
        'GTF_file="' . $self->{'default_GTF'} . '"',
        '',
        '################################################################################',
        '################################################################################',
        '',
        'mkdir -p "$seq_mapped"',
        'mkdir -p "$seq_results"',
        '',
        'array=(',
        join ("\n",

            map {

                my $bam = $_->{'seqres'} . "/$slide" . "." . $_->{'pu'} . ".bam";

                "    $bam",
            }
            @$data
        ),
        ')',
        '',
        'for i in "${array[@]}"; do',
        '',
        '    while [[ $(lsof -Fn "$i" | tail -n +2 | cut -c2-) || ! -f "$i.bai" ]]; do',
        '',
        '        sleep 5',
        '    done',
        'done',
        '',
        join (' ',
            'RNA-SeQC',
            "\\\n" . '    -n 1000',
            "\\\n" . '    -ttype 2',
            "\\\n" . '    -s "$sample_file"',
            "\\\n" . '    -t "$GTF_file"',
            "\\\n" . '    -r "$genome_file"',
            "\\\n" . '    -o "$seq_mapped"',
            "\\\n" . '    -strat gc',
            "\\\n" . '    -gc "$GC_file"'
        ),
        '',
        'cp -r "$seq_mapped" "$seq_results"',
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh2->close;

    $self->write_log("Writing: $script2");

    return $script2;
}

################################################################################
sub create_RSEM_PBS {
#
# Defines the RSEM PBS template
#
    my $self = shift;

    my $code = shift;

    my $run = $self->get_mapping_profile("large");

    my $queue = $run->{'queue'};
    my $nodes = $run->{'nodes'};
    my $ppn = $run->{'ppn'};
    my $threads = $run->{'threads'};
    my $memory = $run->{'memory'};
    my $walltime = $run->{'walltime'};

    my $data = $self->{'RNA_jobs'}{ $code };

    my (@fastq1, @fastq2);

    for (0 .. $#$data) {

        my $fq1 = $data->[$_]{'fastq1'};
        my $fq2 = $data->[$_]{'fastq2'};

        push (@fastq1, "fastq1_" . ($_ + 1) . "=" . $fq1);
        push (@fastq2, "fastq2_" . ($_ + 1) . "=" . $fq2);
    }

    my $slide = $self->{'slide'};
    my $sample_name = "$slide.nopd.$code";

    my $seq_mapped = "$main::seq_mapped/$slide/$slide\_rsem";
    my $seq_results = dirname($data->[0]{'seqres'}) . "/rna_seq/rsem";

    my $seq_mapped_pbs = "$main::seq_mapped/$slide/pbs";
    my $seq_mapped_log = "$main::seq_mapped/$slide/log";

    my $script = "$seq_mapped_pbs/$slide.nopd.$code.rsem.pbs";

    my $fh = IO::File->new("> $script");

    die "Could not open file \"$script\": $!\n" unless $fh;

    print $fh join ("\n",

        '#!/bin/bash',
        '#PBS -N RNA_RSEM',
        "#PBS -q $queue",
        '#PBS -r n',
        "#PBS -l nodes=1:$nodes:ppn=$ppn,walltime=$walltime,mem=$memory",
        '#PBS -m ae',
        '#PBS -j oe',
        "#PBS -o $seq_mapped_log/$sample_name.rsem.log",
        '#PBS -W umask=0007',
        '#PBS -M s.kazakoff@imb.uq.edu.au',
        '################################################################################',
        '################################################################################',
        '',
        'set -e',
        '',
        "workdir=\"/scratch/$sample_name\"",
        '',
        'mkdir -p "$workdir"',
        'cd "$workdir"',
        "\n",
        'module load bowtie/0.12.7',
        'module load rsem/1.2.3',
        "\n",
        "sorted_bam=\"$sample_name.genome.sorted.bam\"",
        "rsem_bam=\"$sample_name.rsem.bam\"",
        '',
        "genes=\"$sample_name.genes.results\"",
        "isoforms=\"$sample_name.isoforms.results\"",
        "stats=\"$sample_name.stat\"",
        '',
        "seq_mapped=\"$seq_mapped\"",
        "seq_results=\"$seq_results\"",
        '',
        'genome="' . $self->{'norRNA_genome'} . '"',
        "sample_name=\"$sample_name\"",
        '',
        join ("\n", @fastq1),
        '',
        join ("\n", @fastq2),
        '',
        '################################################################################',
        '################################################################################',
        '',
        join ("\n",

            map {

                my $stem = (split ("=", $_))[0];

                "zcat \"\$$stem\" | sed '1~4 { s/ .*/\\/1/ }' > ./$stem.fastq &",
                'job_pids+=($!)',
            }
            @fastq1
        ),
        '',
        join ("\n",

            map {

                my $stem = (split ("=", $_))[0];

                "zcat \"\$$stem\" | sed '1~4 { s/ .*/\\/2/ }' > ./$stem.fastq &",
                'job_pids+=($!)',
            }
            @fastq2
        ),
        '',
        'for i in "${job_pids[@]}"; do',
        '',
        '    while [[ $(ps -p "$pid" -o pid=) ]]; do',
        '',
        '        sleep 5',
        '    done',
        '',
        '    wait "$i"',
        '    status=$?',
        '',
        '    if [[ "$status" -ne 0 ]]; then',
        '',
        '        echo "Error. Job failed to decompress successfully."',
        '        echo',
        '        echo "Job failed with PID: $i"',
        '        echo "Exit status: $status"',
        '        echo',
        '        echo "Exiting."',
        '        exit 1',
        '    fi',
        'done',
        '',
        join (' ',
            'rsem-calculate-expression',
            "\\\n" . '    --paired-end',
            "\\\n" . '    --calc-ci',
            "\\\n" . '    --strand-specific',
            "\\\n" . '    --output-genome-bam',
            "\\\n" . "    -p $threads",
            "\\\n" . '    ' . join (",",
                map {

                    './' . (split ("=", $_))[0] . '.fastq'
                }
                @fastq1
            ),
            "\\\n" . '    ' . join (",",
                map {

                    './' . (split ("=", $_))[0] . '.fastq'
                }
                @fastq2
            ),
            "\\\n" . '    "$genome"',
            "\\\n" . '    "$sample_name"'
        ),
        '',
        'for i in "$seq_mapped" "$seq_results"; do',
        '',
        '    mkdir -p "$i"',
        '',
        '    cp "$sorted_bam" "$i/$rsem_bam"',
        '',
        '    cp "$genes" "$i"',
        '    cp "$isoforms" "$i"',
        '    cp -r "$stats" "$i"',
        'done',
        '',
        'rm -rf "$workdir"',
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh->close;

    $self->write_log("Writing: $script");

    return $script;
}

################################################################################
sub create_MapSplice_PBS {
#
# Defines the MapSplice PBS template
#
    my $self = shift;

    my $code = shift;

    my $run = $self->get_mapping_profile("large");

    my $queue = $run->{'queue'};
    my $nodes = $run->{'nodes'};
    my $ppn = $run->{'ppn'};
    my $threads = $run->{'threads'};
    my $memory = $run->{'memory'};
    my $walltime = $run->{'walltime'};

    my $data = $self->{'RNA_jobs'}{ $code };

    my (@fastq1, @fastq2);

    for (0 .. $#$data) {

        my $fq1 = $data->[$_]{'fastq1'};
        my $fq2 = $data->[$_]{'fastq2'};

        push (@fastq1, "fastq1_" . ($_ + 1) . "=" . $fq1);
        push (@fastq2, "fastq2_" . ($_ + 1) . "=" . $fq2);
    }

    my $lb = $data->[0]{'lb'};
    my $sm = $data->[0]{'sm'};
    my $cn = $data->[0]{'cn'};

    my $slide = $self->{'slide'};
    my $sample_name = "$slide.nopd.$code";

    my $seq_mapped = "$main::seq_mapped/$slide/$slide\_mapsplice";
    my $seq_results = dirname($data->[0]{'seqres'}) . "/rna_seq/mapsplice";

    my $seq_mapped_pbs = "$main::seq_mapped/$slide/pbs";
    my $seq_mapped_log = "$main::seq_mapped/$slide/log";

    my $script = "$seq_mapped_pbs/$slide.nopd.$code.mapsplice.pbs";

    my $fh = IO::File->new("> $script");

    die "Could not open file \"$script\": $!\n" unless $fh;

    print $fh join ("\n",

        '#!/bin/bash',
        '#PBS -N RNA_MapSplice',
        "#PBS -q $queue",
        '#PBS -r n',
        "#PBS -l nodes=1:$nodes:ppn=$ppn,walltime=$walltime,mem=$memory",
        '#PBS -m ae',
        '#PBS -j oe',
        "#PBS -o $seq_mapped_log/$sample_name.mapsplice.log",
        '#PBS -W umask=0007',
        '#PBS -M s.kazakoff@imb.uq.edu.au',
        '################################################################################',
        '################################################################################',
        '',
        'set -e',
        '',
        "workdir=\"/scratch/$sample_name\"",
        '',
        'mkdir -p "$workdir"',
        'cd "$workdir"',
        "\n",
        'module load adama/nightly',
        'module load bowtie/0.12.7',
        'module load mapsplice/2.1.3',
        "\n",
        "sample_name=\"$sample_name\"",
        "seq_mapped=\"$seq_mapped\"",
        "seq_results=\"$seq_results\"",
        '',
        "lb=\"$lb\"",
        "sm=\"$sm\"",
        "cn=\"$cn\"",
        '',
        'genome="' . $self->{'default_genome'} . '"',
        'chromosomes="' . $self->{'default_chromosomes'} . '"',
        '',
        join ("\n", @fastq1),
        '',
        join ("\n", @fastq2),
        '',
        '################################################################################',
        '################################################################################',
        '',
        join ("\n",

            map {

                my $stem = (split ("=", $_))[0];

                "zcat \"\$$stem\" | sed '1~4 { s/ .*/\\/1/ }' > ./$stem.fastq &",
                'job_pids+=($!)',
            }
            @fastq1
        ),
        '',
        join ("\n",

            map {

                my $stem = (split ("=", $_))[0];

                "zcat \"\$$stem\" | sed '1~4 { s/ .*/\\/2/ }' > ./$stem.fastq &",
                'job_pids+=($!)',
            }
            @fastq2
        ),
        '',
        'for i in "${job_pids[@]}"; do',
        '',
        '    while [[ $(ps -p "$pid" -o pid=) ]]; do',
        '',
        '        sleep 5',
        '    done',
        '',
        '    wait "$i"',
        '    status=$?',
        '',
        '    if [[ "$status" -ne 0 ]]; then',
        '',
        '        echo "Error. Job failed to decompress successfully."',
        '        echo',
        '        echo "Job failed with PID: $i"',
        '        echo "Exit status: $status"',
        '        echo',
        '        echo "Exiting."',
        '        exit 1',
        '    fi',
        'done',
        '',
        join (' ',
            'mapsplice',
            "\\\n" . "    -p $threads",
            "\\\n" . '    -c "$chromosomes"',
            "\\\n" . '    --fusion',
            "\\\n" . '    --qual-scale phred33',
            "\\\n" . '    -x "$genome"',
            "\\\n" . '    -1 ' . join (",",
                map {

                    './' . (split ("=", $_))[0] . '.fastq'
                }
                @fastq1
            ),
            "\\\n" . '    -2 ' . join (",",
                map {

                    './' . (split ("=", $_))[0] . '.fastq'
                }
                @fastq2
            ),
            "\\\n" . '    -o .'
        ),
        '',
        join (' ',
            'qbamfix',
            "\\\n" . '    --input ./alignments.sam',
            "\\\n" . '    --output ./alignments.bam',
            "\\\n" . "    --log ./qbamfix.log",
            "\\\n" . '    --validation SILENT',
            "\\\n" . '    --RGLB "$lb"',
            "\\\n" . '    --RGSM "$sm"',
            "\\\n" . '    --RGCN "$cn"',
            "\\\n" . '    --tmpdir "$TMPDIR"',
        ),
        '',
        'for i in "$seq_mapped" "$seq_results"; do',
        '',
        '    mkdir -p "$i"',
        '',
        '    cp ./alignments.bam "$i/$sample_name.mapsplice.bam"',
        '',
        '    for j in {insertions,deletions,fusions_{raw,candidates},junctions,stats}.txt; do',
        '',
        '        if [ -e "$j" ]; then',
        '',
        '            cp "$j" "$i/$sample_name.$j"',
        '        fi',
        '    done',
        '',
        '    if [ -d ./logs ]; then',
        '',
        '        cp -r ./logs "$i/${sample_name}.log"',
        '    fi',
        'done',
        '',
        'rm -rf "$workdir"',
        '',
        '################################################################################',
        '################################################################################',
    ) . "\n";

    $fh->close;

    $self->write_log("Writing: $script");

    return $script;
}

################################################################################
sub get_mapping_profile {
#
# Returns params to suit BWA when executing PBS
#   
    my $self = shift;

    my $mode = shift;

    $mode ||= $main::mapping_mode;

    my %params;

    if ($mode eq "large") {

        %params = (

            'queue' => 'batch',
            'nodes' => 'large',
            'ppn' => 12,
            'threads' => 24,
            'memory' => '90gb',
            'max_memory_per_thread' => '2G',
            'walltime' => '72:00:00', 
        )
    }

    if ($mode eq "small") {

        %params = (

            'queue' => 'batch',
            'nodes' => 'small',
            'ppn' => 8,
            'threads' => 16,
            'memory' => '45gb',
            'max_memory_per_thread' => '1G',
            'walltime' => '72:00:00', 
        )
    }

    return \%params;
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
