package QCMG::Ingest::Slide;
################################################################################
#
#    Slide.pm
#
#    Common methods to ingest QCMG-style slides into LiveArc
#
#    Authors: Stephen Kazakoff
#
#    $Id: Slide.pm 4663 2014-07-24 06:39:00Z j.pearson $
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
(
    my $version = '$Id: Slide.pm 4663 2014-07-24 06:39:00Z j.pearson $'
)
    =~ s/.*Id: (?:\S+) (\d+).*/$1/;

BEGIN {
    select STDERR; $| = 1;
    select STDOUT; $| = 1;
}

umask 0002;

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
sub new {
#
# Set some defaults
#
    my ($class, %options) = @_;

    my $self = {};

    bless $self, $class;


    $self->{'version'} = $version;
    $self->{'options'} = \%options;

    $self->{'slide_data'} = delete $self->{'options'}{'slide_data'};
    $self->{'slide_folder'} = delete $self->{'options'}{'slide_folder'};
    $self->{'slide'} = basename($self->{'slide_folder'});

    $self->{'log_file'} = delete $self->{'options'}{'log_file'};
    $self->{'log_extn'} = delete $self->{'options'}{'log_extn'};

    $self->{'seq_raw'} = delete $self->{'options'}{'seq_raw'};
    $self->{'seq_mapped'} = delete $self->{'options'}{'seq_mapped'};

    $self->{'default_email'} = 'QCMG-InfoTeam@imb.uq.edu.au';

    $self->{'default_genome'} = join ("/",

        "/panfs/share/genomes/GRCh37_ICGC_standard_v2",
        "GRCh37_ICGC_standard_v2.fa",
    );


    my $LA_java = "/usr/bin/java";
    my $LA_cred = $ENV{'HOME'} . "/.mediaflux/mf_credentials.cfg";
    my $LA_term = "/panfs/share/software/mediaflux/aterm.jar";
    my $LA_base = "$LA_java -Dmf.result=shell -Dmf.cfg=$LA_cred -Xmx1g -jar $LA_term ";

    $self->{'LA_execute'} = "$LA_base --app exec ";

    $self->create_log(

        $self->{'options'}{'cmdline'},
        $self->{'log_file'},
        $self->{'log_extn'},
    );

    return $self;
}

################################################################################
sub ingest_datafiles {
#
# Ingests a hash of checked files
#
    my $self = shift;

    my $list = $self->{'filelist'};

    for (natkeysort { $_ } keys %$list) {

        $self->ingest_file(

            $_, [

                $list->{$_}{'crc'},
                $list->{$_}{'size'},
            ],
        );
    }

    return;
}

################################################################################
sub ingest_folder {
#
# Performs directory ingestion into LiveArc
#
    my $self = shift;

    my $folder = shift;

    my $base = basename($folder);

    $self->write_log("--- Ingesting $folder ---");

    my $ns = $self->{'options'}{'namespace'};

    my $asset_import = join (" ",

        $self->{'LA_execute'} .
        "import",
        "-namespace $ns",
        "-archive 1",
        "-name " . $self->{'slide'} . "_bwa_$base",
        $folder,
    );

    my $res = $self->exec_LA_command($asset_import);

    $res =~ s/[^ ]* *//;

    $self->write_log("Successfully $res into LiveArc.");

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub ingest_file {
#
# Performs file ingestion into LiveArc
#
    my $self = shift;

    my $file = shift;
    my $meta = shift;

    my $base = basename($file);

    $self->write_log("--- Ingesting $base ---");

    my $ns = $self->{'options'}{'namespace'};

    my $asset_exists = $self->{'LA_execute'} . "asset.exists :id path=$ns/$base";
    my $asset_get = $self->{'LA_execute'} . "asset.get :id path=$ns/$base";
    my $asset_create = $self->{'LA_execute'} . "asset.create :namespace $ns :name $base";

    my ($id, $vs);

    my $res1 = $self->exec_LA_command($asset_exists);

    if ((split ('"', $res1))[-1] eq "true") {

        $self->write_log("Found: $ns/$base");

        my $res2 = $self->exec_LA_command($asset_get);

        my %meta = map {

            s/[^:]+://;

            my @fields = split;

            shift @fields => join (" ", @fields)
        }
        split ("\n", $res2);

        $id = (split ('"', $meta{'asset'}))[1];
        $vs = (split ('"', $meta{'asset'}))[3];

        $self->write_log("Found an asset with id: $id, and version: $vs");
    }
    else {

        $self->write_log("Creating asset: $ns/$base");

        my $res3 = $self->exec_LA_command($asset_create);

        $id = (split ('"', $res3))[-1];

        $self->write_log("Asset with id: $id, is now at version: 1");
    }

    my $metadata = $meta ? join (" ",
        "",
        ":meta \\<",
            ":checksums \\<",
                ":source orig",
                ":checksum \\<",
                    ":chksum " . $meta->[0],
                    ":filesize " . $meta->[1],
                    ":filename $base",
                "\\>",
            "\\>",
        "\\>",
    ) : "";

    my $res4 = $self->exec_LA_command(

        $self->{'LA_execute'} . join (" ",
            "asset.set",
            ":id $id",
            ":in file:$file",
        ) . $metadata
    );

    $res4 = (split ('"', $res4))[-1];

    $self->write_log("Asset with id: $id, is now at version: $res4");

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub exec_LA_command {
#
# Runs a LiveArc command, re-attempt every 60 seconds if necessary
#
    my $self = shift;

    my $cmd = shift;

    my @results = (undef, undef, 1);
    my $counter = 1;

    my $sleep_time = 60;
    my $time = 0;

    do {

        @results = capture { system ($cmd) };

        if ($results[2] != 0) {

            $self->write_log("Failed to execute ATERM command:\n\n$cmd\n");
            $self->write_log("Advanced automatic retry feature enabled.");
            $self->write_log("Attempting to re-execute previous ATERM command.");
            $self->write_log("Attempt number: " . $counter++);
            $self->write_log("Elapsed time: $time seconds\n\n");
    
            sleep $sleep_time;
        }

        $time += $sleep_time;

    } until $results[2] == 0 || $time >= 360;

    if ($results[2] != 0) {

        $self->write_log("Error: Could not execute ATERM command:\n$cmd\n");
        $self->write_log("Failed after $counter retries (" . $time / 60 . " minutes)");

        die;

    } else {

        (my $log = $cmd) =~ s/.*--app //;

        $log =~ s/ :(meta .*)/\n$1/;

        $self->write_log($log);
    }

    chomp $results[0];

    return $results[0];
}

################################################################################
sub create_namespace {
#
# Make sure LiveArc is ready to accept ingestion
#
    my $self = shift;

    my $ns = $self->{'options'}{'namespace'};

    $self->write_log("--- Creating Namespace $ns ---");

    my @dirs = File::Spec->splitdir($ns);

    for (1 .. $#dirs) {

        my $dir = File::Spec->catdir(@dirs[1 .. $_]);

        my $dir_exists = $self->{'LA_execute'} . "asset.namespace.exists :namespace $dir";
        my $dir_create = $self->{'LA_execute'} . "asset.namespace.create :namespace $dir";

        my (@res1) = capture { system ($dir_exists) };

        (my $log1 = $dir_exists) =~ s/.*--app //;

        $self->write_log($log1);

        if ($res1[2] != 0) {

            $self->write_log("Error: Could not execute ->\n$dir_exists");

            die;
        }

        chomp $res1[0];

        if ((split ('"', $res1[0]))[-1] eq "true") {
    
            $self->write_log("Found: $dir");
        }
        else {

            $self->write_log("Creating namespace: $dir");

            my (@res2) = capture { system ($dir_create) };

            (my $log2 = $dir_create) =~ s/.*--app //;

            $self->write_log($log2);

            if ($res2[2] != 0) {

                $self->write_log("Error: Could not execute ->\n$dir_create");

                die;
            }

            $self->write_log("Created namespace: $dir");
        }
    }

    $self->{'ingest_prepared'} = 1;

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub checksum_files {
#
# Calculates CRC and file sizes
#
    my $self = shift;

    $self->write_log("--- Calculating checksums ---");

    my @filelist;

    if ($self->{'FASTQ_files'}) {

        push (@filelist, @{ $self->{'FASTQ_files'} });
        delete $self->{'FASTQ_files'};
    };

    if ($self->{'BAM_files'}) {

        push (@filelist, @{ $self->{'BAM_files'} });
        delete $self->{'BAM_files'};
    }

    my %checksums = map {

        my $name = basename($_);

        my ($crc, $size) = map { split } capture { system ("cksum", $_) };

        $self->write_log("Stats: $name => $crc, $size");

        $_ => {
            'crc' => $crc,
            'size' => $size,
        };
    }
    @filelist;

    $self->{'filelist'} = \%checksums;

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub make_BWA_backtrack_PBS {
#
# Creates BWA-backtrack PBS using geneus metadata
#
    my $self = shift;

    my $fastq = $self->{'FASTQ_files'};
    my $slide = $self->{'slide'};

    $self->write_log("--- Creating BWA-backtrack PBS for $slide ---");

    my @fastq1 = @$fastq[ grep { $_ % 2 == 0 } 0 .. $#$fastq ];
    my @fastq2 = @$fastq[ grep { $_ % 2 == 1 } 0 .. $#$fastq ];

    my @mapset_stems;

    my $ref;

    if (defined $self->{'geneus_data'}) {

        @mapset_stems = sort keys %{ $self->{'geneus_data'} };
    }
    elsif (defined $self->{'slide_data'}) {

        @mapset_stems = $self->{'slide_data'};
    }
    else {

        $self->write_log("Error: Could not find data input");
        $self->write_log("Please confirm that `\$self->{'geneus_data'}` or");
        $self->write_log("`\$self->{'slide_data'}` is begin populated correctly");

        die;
    }

    for (0 .. $#mapset_stems) {

        if (defined $self->{'geneus_data'}{ $mapset_stems[$_] }) {

            $ref = $self->{'geneus_data'}{ $mapset_stems[$_] };
        }
        elsif (defined $self->{'slide_data'}[$_]) {

            $ref = $self->{'slide_data'}[$_];
        }
        else {

            $self->write_log("Error: Could not find data input");
            $self->write_log("Please confirm that `\$self->{'geneus_data'}` or");
            $self->write_log("`\$self->{'slide_data'}` is begin populated correctly");

            die;
        }

	$ref->{'Reference Genome File'} ||= $self->{'default_genome'};

        my $mapping_vars = {

            'slide' => $slide,

            'genome' => $ref->{'Reference Genome File'},

            'fastq1' => $fastq1[$_],

            'fastq2' => $fastq2[$_],

            'seq_results' => join ("/",

                $self->{'Parent_prefixes'}{ $ref->{'Parent Prefix'} },
                $ref->{'Donor'},
                "seq_verify",
            ),

            'id' => strftime(

                "%Y%m%d%H%M%S", localtime

            ) . join ("",

                map {

                    int ( rand (9) )

                } 1 .. 3
            ),

            'cn' => "QCMG",
            'ds' => $ref->{'Sample Name'},
            'lb' => $ref->{'Primary Library'},
            'pl' => "ILLUMINA",
            'pu' => "lane_" . $ref->{'Lane'} . "." . $ref->{'Index'},
            'sm' => $ref->{'Donor'},
        };

        push (@{ $self->{'PBS_jobs'} },

            $self->use_BWA_backtrack_PBS_template($mapping_vars)
        );
    }

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
#sub run_BWA_backtrack_from_CSV {
##
## Creates PBS using a qOrganise CSV to run BWA-backtrack
##
#    my $self = shift;
#
#    my $flag = shift;
#
#    my $fastq = $self->{'FASTQ_files'};
#    my $slide = $self->{'slide'};
#
#    $self->write_log("--- Creating BWA-backtrack PBS for $slide ---");
#
#    my @pbs_filelist;
#
#    my @fastq1 = @$fastq[ grep { $_ % 2 == 0 } 0 .. $#$fastq ];
#    my @fastq2 = @$fastq[ grep { $_ % 2 == 1 } 0 .. $#$fastq ];
#
#
#    for (0 .. $#{ $self->{'slide_data'} }) {
#
#        my $ref = $self->{'slide_data'}[$_];
#
#        my $fastq1 = $fastq1[$_];
#        my $fastq2 = $fastq2[$_];
#
#        my $genome = "/panfs/share/genomes/GRCm38.p2/GRCm38.p2.fa";
#        my $seqres = join ("/",
#
#            "/mnt/seq_results/smgres_gemm",
#            $ref->{'QCMG_donor_id'},
#            "seq_mapped",
#        );
#
#        # define values for the BAM's @RG tag
#        my $id = strftime(
#
#            "%Y%m%d%H%M%S", localtime
#
#        ) . join ("",
#
#            map {
#                int ( rand (9) )
#            } 1 .. 3
#        );
#        my $cn = "Ashok Laboratory";
#        my $ds = $ref->{'QCMG_sample_id'};
#        my $lb = $ref->{'QCMG_library_id'};
#        my $pl = "ILLUMINA";
#        my $bc = $ref->{'Barcode'} eq "-" ? "nobc" : $ref->{'Barcode'};
#        my $pu = "lane_" . $ref->{'Lane'} . ".$bc";
#        my $sm = $ref->{'QCMG_donor_id'};
#    }
#}
#
################################################################################
sub use_BWA_backtrack_PBS_template {
#
# Creates PBS to run BWA-backtrack
#
    my $self = shift;

    my $vars = shift;

    my $bwa = $self->{'options'}{'bwa'};

    my $threads = $bwa->{'number of threads'};
    my $mem_request = $bwa->{'memory request'};
    my $walltime = $bwa->{'walltime'};
    my $max_mem_per_thread = $bwa->{'max memory per thread'};
    my $queue = $bwa->{'queue name'};
    my $threading = $bwa->{'hyperthreading'};
    my $ncpus = $threading eq 'Y' ? int ($threads / 2) : $threads;

    my (
        $slide,
        $genome,
        $fastq1,
        $fastq2,
        $seq_results,

        $id, $cn, $ds, $lb, $pl, $pu, $sm,
    )
    = @{ $vars }{
        qw(
            slide
            genome
            fastq1
            fastq2
            seq_results

            id cn ds lb pl pu sm
        )
    };

    my $seq_mapped = $self->{'seq_mapped'} . "/$slide";

    my $seq_mapped_pbs = "$seq_mapped/pbs";
    my $seq_mapped_log = "$seq_mapped/log";

    make_path(

        $seq_mapped_pbs,
        $seq_mapped_log
    );

    my $script = "$seq_mapped_pbs/$slide.$pu.pbs";

    my $fh = IO::File->new("> $script");

    die "Could not open file \"$script\": $!\n" unless $fh;

    print $fh join ("\n",

        '#!/bin/bash',
        '#PBS -N BWA-backtrack',
        "#PBS -q $queue",
        '#PBS -r n',
        "#PBS -l ncpus=$ncpus,walltime=$walltime,mem=$mem_request",
        '#PBS -m ae',
        '#PBS -j oe',
        "#PBS -o $seq_mapped_log/$slide.$pu.log",
        '#PBS -W umask=0022',
        '#PBS -M ' . $ENV{'QCMG_EMAIL'},
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
        'module load bwa/0.6.2-mt',
        'module load samtools/0.1.19',
        'module load picard/1.97',
        'module load java/1.7.13',
        "\n",
        "genome=\"$genome\"",
        '',
        "fastq1=\"$fastq1\"",
        "fastq2=\"$fastq2\"",
        '',
        "sample=\"$slide.$pu\"",
        '',
        "seq_mapped=\"$seq_mapped\"",
        "seq_results=\"$seq_results\"",
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
        'echo "BWA-backtrack complete!"',
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
        'echo "MarkDuplicates complete!"',
        '',
        'chmod 664 "$sample.bam"',
        'chmod 664 "$sample.metric"',
        '',
        'cp "$sample.bam" "$seq_mapped"',
        'cp "$sample.metric" "$seq_mapped"',
        '',
        'cp "$sample.bam" "$seq_results"',
        'cp "$sample.metric" "$seq_results"',
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
sub execute_PBS {
#
# Executes a list of PBS scripts
#
    my $self = shift;

    my $slide = $self->{'slide'};

    $self->write_log("--- Executing PBS for $slide ---");

    map {

        my ($stdout, $stderr, $exit) = capture {

            system ("qsub", $_)
        };

        chomp $stdout;

        $self->write_log("Job executed: $stdout");

        if ($exit != 0) {

            $self->write_log($stderr);

            die "Could not `qsub` file \"$_\": $!\n";
        }

    } @{ $self->{'PBS_jobs'} };

    $self->write_log("--- Done ---\n");
}

################################################################################
sub create_connection_to_geneus {
#
# Checks for config files and opens up a connection instance to geneus
#
    my $self = shift;

    my $home = $ENV{'HOME'};

    $ENV{'PGSYSCONFDIR'} = "$home/.geneus";

    if (!-e "$home/.pgpass") {

        $self->write_log("Invalid `.pgpass` file. Exiting.\n");
        die;
    }

    if (!-e "$home/.geneus/pg_service.conf") {

        $self->write_log("Invalid `pg_service.conf` file. Exiting.\n");
        die;
    }

    my $dbh = DBI->connect(

        'DBI:Pg:service=QCMG',
        undef,
        undef,
        {
            AutoCommit => 0,
            RaiseError => 1,
            PrintError => 0,
        }

    ) or die DBI->errstr;

    return $dbh;
}

################################################################################
sub get_slide_data_from_geneus {
#
# Searches geneus for slide metadata
#
    my $self = shift;

    my $connection = create_connection_to_geneus();

    my $query = qq{
        SELECT
        project.parent_project AS "Project",
        regexp_replace( project.name, '-.*', '' ) AS "Parent Prefix",
        replace( replace( project.name, '-', '_'), '.', '_' ) AS "Donor",
        project.limsid AS "Project LIMS ID",
        project.study_group AS "Study Group",
        project.project_open AS "Project Open",
        sample.name AS "Sample Name",
        sample.limsid AS "Sample LIMS ID",
        sample.sample_code AS "Sample Code",
        sample.material AS "Material",
        sample.researcher_annotation AS "Researcher Annotation",
        mapset.name AS "Mapset",
        regexp_replace( mapset.name, E'.*\\\\.', '') AS "Index",
        regexp_replace( mapset.name, '.*_([1-8]).*', E'\\\\1') AS "Lane",
        mapset.limsid AS "Mapset LIMS ID",
        mapset.primary_library AS "Primary Library",
        mapset.library_protocol AS "Library Protocol",
        mapset.capture_kit AS "Capture Kit",
        coalesce(gfflib.gff_file, gffcapture.gff_file, gffspecies.gff_file) AS "GFF File",
        mapset.sequencing_platform AS "Sequencing Platform",
        mapset.aligner AS "Aligner",
        sample.alignment_required AS "Alignment Required",
        sample.species_reference_genome AS "Species Reference Genome",
        genome.genome_file AS "Reference Genome File",
        mapset.failed_qc AS "Failed QC",
        mapset.isize_min AS "isize Min",
        mapset.isize_max AS "isize Max",
        mapset.isize_manually_reviewed AS "isize Manually Reviewed"
        FROM qcmg.project
        INNER JOIN qcmg.sample on sample.projectid = project.projectid
        INNER JOIN qcmg.mapset on mapset.sampleid = sample.sampleid
        LEFT JOIN qcmg.genome ON genome.species_reference_genome = sample.species_reference_genome
        LEFT JOIN qcmg.gff gfflib ON mapset.library_protocol ~ gfflib.library_protocol
        LEFT JOIN qcmg.gff gffcapture ON mapset.capture_kit = gffcapture.capture_kit
        LEFT JOIN qcmg.gff gffspecies ON sample.species_reference_genome = gffspecies.species_reference_genome
        WHERE mapset.name like ?
    };

    my $cursor = $connection->prepare($query);

    $cursor->bind_param(1, $self->{'slide'} . ".%.%");

    my ($field_names, $fetch_datas);

    if ($cursor->execute) {

        $field_names = $cursor->{'NAME'};
        $fetch_datas = $cursor->fetchall_arrayref;
    }
    else {

        $self->write_log("Cannot execute query. Exiting.\n");
        die;
    }

    $cursor->finish;
    $connection->disconnect;

    unless ($field_names || $fetch_datas) {

        $self->write_log("Slide not in LIMS: $self->{'Slide data'}");
        die;
    }
    else {

        $self->write_log("---Finding mapset information---");
    }

    my @mapsets = map {

        my %hash;

        @hash{ @$field_names } = @$_;

        map {

            $hash{$_} = ""
        }
        grep {

            !$hash{$_}
        }
        keys %hash;

        my $mapset = delete $hash{'Mapset'};

        $self->{'geneus_data'}{$mapset} = \%hash;

        join (" ",
            $hash{'Sample Name'},
            "=>",
            $mapset,
        )
    }
    @$fetch_datas;

    $self->write_log("Found: $_") for sort @mapsets;
    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub get_parent_paths_from_geneus {
#
# Collects parent and path associations from geneus
#
    my $self = shift;

    my $connection = create_connection_to_geneus();

    my $query = qq{
        SELECT
        prefix,
        parent_path
        FROM project_prefix_map
    };

    my $cursor = $connection->prepare($query);

    my $fetch_datas;

    if ($cursor->execute) {

        $fetch_datas = $cursor->fetchall_arrayref
    }
    else {

        $self->write_log("Cannot execute query");
        die;
    }

    $cursor->finish;
    $connection->disconnect;

    unless ($fetch_datas) {

        $self->write_log("Slide not in LIMS: $self->{'Slide data'}");
        die;
    }
    else {

        $self->write_log("---Finding prefix and path associations---");
    }

    $self->{'Parent_prefixes'} = {

        map {

            my ($parent, $path) = (
                $_->[0],
                $_->[1],
            );

            $self->write_log("Found: $parent => $path");

            $parent => $path
        }
        sort {

            $a->[0] cmp $b->[0]
        }
        @$fetch_datas
    };

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub find_files {
#
# Finds FASTQ or BAM files
#
    my $self = shift;

    my $type = shift;

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

            $self->write_log("Found: $_");
            $_
        }
        sort
        File::Find::Rule
            ->file
            ->readable
            ->name($regex)
            ->in($self->{'slide_folder'})
    ];

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub rename_and_copy_FASTQ_files {
#
# Reads a qOrganise CSV
#
    my $self = shift;

    my $flag = shift;

    $self->write_log("--- Copying FASTQ files to /mnt/seq_raw ---");

    map {

        my $mapset = $_;

        push (@{ $self->{'FASTQ_files'} },

            map {

                my $fastq = $self->{'slide_folder'} . "/" . join (".",

                    $self->{'slide'},
                    "lane_" . $mapset->{'Lane'},
                    $mapset->{'Barcode'} eq "-" ? "nobc" : $mapset->{'Barcode'},
                    $_,
                    "fastq.gz",
                );

                if ($flag == 1) {

                    $self->write_log("Copying: " . $mapset->{"FASTQ$_"});
                    $self->write_log("To:      " . $fastq);

                    copy(

                        $mapset->{"FASTQ$_"},
                        $fastq

                    ) or die join ("\n",

                        "Cannot move file:",
                        "FROM: " . $mapset->{"FASTQ$_"},
                        "TO: " . $fastq,
                    );
                }
                else {

                    $self->write_log("File exists: $fastq");
                }

                $fastq

            } 1 .. 2
        );

    } @{ $self->{'slide_data'} };

    $self->write_log("--- Done ---\n");

    return;
}

################################################################################
sub create_log {
#
# Renames an existing logfile (if present), creates a log and writes a header
#
    my $self = shift;

    my ($cmdline, $logfile, $logextn) = @_;

    my $date = date_spec();

    my ($name, $dir, $suffix) = fileparse($logfile, $logextn);

    if (
        File::Find::Rule->file->name($name . $suffix)->in($dir)
    ) {
        my @ext = split (/\./, $suffix);

        my $type = pop @ext;

        my $newname = $name . join (".", @ext, "") . $date->[3] . ".$type";

        my $rv = rename ($logfile, $dir . $newname);

        if ($rv != 1) {

            die "Error: Could not rename the file ->\n$logfile\n" .
                "Please check file permissions and try again.\n";
        }
    }

    $self->{'log_fh'} = IO::File->new($logfile, "w");
    $self->{'log_io'} = IO::Handle->new();

    $self->{'log_io'}->autoflush(1);

    my %sysinfo = (

       'nodename' => capture { system ("uname", "-n") },
       'userid' => capture { system ("whoami") },
       'kernel_name' => capture { system ("uname", "-s") },
       'architecture' => capture { system ("arch") },
       'kernel_version' => capture { system ("uname", "-r") },
       'perl' => capture { system ("which", "perl") },
       'java' => capture { system ("which", "java") },
       'java_version' => (split ('"', capture { system ("java -version 2>&1") }))[1],
    );
  
    map { chomp; $_ } values %sysinfo;

    my $msg = join ("\n",
        "EXECLOG: Start-time " . $date->[1],
        "EXECLOG: Host " . $sysinfo{'nodename'},
        "EXECLOG: Run-by " . $sysinfo{'userid'},
        "EXECLOG: OS-name " . $sysinfo{'kernel_name'},
        "EXECLOG: OS-arch " . $sysinfo{'architecture'},
        "EXECLOG: OS-version " . $sysinfo{'kernel_version'},
        "EXECLOG: Tool-name " . $0,
        "EXECLOG: Tool-version " . $self->{'version'},
        "EXECLOG: PWD " . getcwd,
        "EXECLOG: Command-line $0 $cmdline",
        "EXECLOG: Perl-version " . $],
        "EXECLOG: Perl-home " . $sysinfo{'perl'},
        "EXECLOG: Java-version " . $sysinfo{'java_version'},
        "EXECLOG: Java-home " . $sysinfo{'java'},
        "TOOLLOG: Log_file " . $logfile,
        "TOOLLOG: Slide_folder " . abs_path($dir),
        "TOOLLOG: Default_email " . $self->{'default_email'},
        "TOOLLOG: Slide_name " . $name
    );

    $self->{'start_time'} = $date;

    $self->write_log("$msg\n\n");

    return;
}

################################################################################
sub date_spec {
#
# Returns a hash of three common date specs
#
    my $self = shift;

    my $time = time;
    my @localtime = localtime ($time);

    my @timestamps = (

        $time,
        strftime("[%Y-%m-%d %H:%M:%S]", @localtime),
        strftime("%Y%m%d", @localtime),
        strftime("%Y%m%d%H%M%S", @localtime),
    );

    return \@timestamps;
}

################################################################################
sub write_log {
#
# Writes logging info
#
    my $self = shift;

    my $content = shift;

    my $fh = $self->{'log_fh'};
    my $io = $self->{'log_io'};

    if ($io->fdopen($fh, "w") && !$self->{'fh_closed'}) {

        $io->print("$content\n");
    }

    return;
}

################################################################################
sub DESTROY {
#
# A destroyer of objects
#
    my $self = shift;

    my $date = date_spec();

    my $elapsed_time = $date->[0] - $self->{'start_time'}[0];

    my $time = sprintf ("%d days, %d hours, %d minutes and %d seconds",

        int ($elapsed_time / (24 * 60 * 60)),
        ($elapsed_time / (60 * 60)) % 24,
        ($elapsed_time / 60) % 60,
        ($elapsed_time % 60),
    );

    $self->write_log("EXECLOG: Elapsed_time " . $time);
    $self->write_log("EXECLOG: Stop_time " . $date->[1]);
    $self->write_log("EXECLOG: Exit_status $?");

    $self->{'fh_closed'}++;

    $self->{'log_io'}->close;

    if ($? == 0 && $self->{'ingest_prepared'}) {

        $self->ingest_file($self->{'log_file'});
    }

    return;
}

################################################################################
1;

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013-2014

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
