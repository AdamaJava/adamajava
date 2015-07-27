#!/usr/bin/perl
################################################################################
#
#    QCMG::Ingest::Common
#
#    Common functions for ingesting raw sequencing files
#    
#    Author: Stephen Kazakoff
#
#    $Id$
#
################################################################################
# # #
# #       MODULE:
#
use strict;
use warnings;
################################################################################
################################################################################
sub create_log {
#
# Writes EXECLOG and TOOLLOG names and values
#
    my $self = shift;
    my $cmd = shift;

    my $date = $self->timestamp;

    my $perl = substr (((split " ", capturex("perl", "-v"))[3]), 1);
    my $java = substr ((split " ", (capture("java -version 2>&1"))[0])[-1], 1, -1);

    my $msg = join ("\n",
        "EXECLOG: startTime " . $date,
        "EXECLOG: host " . $self->HOSTKEY,
        "EXECLOG: runBy " . capturex("whoami") .
        "EXECLOG: osName " . capturex("uname", "-s") .
        "EXECLOG: osArch " . capturex("arch") .
        "EXECLOG: osVersion " . capturex("uname", "-v") .
        "EXECLOG: toolName $0",
        "EXECLOG: toolVersion ",
        "EXECLOG: cwd " . getcwd,
        "EXECLOG: commandLine $0 $cmd",
        "EXECLOG: perlVersion $perl",
        "EXECLOG: perlHome " . capturex("which", "perl") .
        "EXECLOG: javaVersion $java",
        "EXECLOG: javaHome " . capturex("which", "java") .
        "TOOLLOG: log_file $self->{ 'log_file' }",
        "TOOLLOG: slide_folder $self->{ 'slide_folder' }",
        "TOOLLOG: default_email " . $self->DEFAULT_EMAIL,
        "TOOLLOG: slide_name $self->{ 'slide_name' }"
    );

    $self->{'startTime'} = $date;

    $self->writelog(BODY => "$msg\n");
    $self->writelog(BODY => "Beginning processing at $date\n\n");

    return;
}

################################################################################
sub check_folder {
#
# Checks the slide older and permissions
#
    my $self = shift;

    my $folder = $self->{ "slide_folder" };

    $self->writelog(BODY => "---Checking " . $folder . "---");

    if (!$folder || !-e $folder || !-d $folder) {

        $self->writelog(BODY => "No valid run folder provided\n");
        exit $self->EXIT_NO_FOLDER;
    }

    if (!-r $folder) {

        $self->writelog(BODY => "$folder is not a readable directory");
        exit $self->EXIT_BAD_PERMS;
    }

    $self->writelog(BODY => "Slide folder exists and is readable.\n");

    return;
}

################################################################################
sub check_metafiles {
#
# Checks if some files and directories exist and are readable
#
    my $self = shift;
    my $type = shift;

    $self->find_metafiles(
        "data_type" => "directory",
        "file_path" => "/Data/Intensities/BaseCalls",
        "platform" => $type
    );

    if ($type eq "MiSeq") {

        $self->find_metafiles(
            "data_type" => "directory",
            "file_path" => "/InterOp",
            "platform" => $type
        );

        $self->find_metafiles(
            "data_type" => "file",
            "file_path" => "/SampleSheet.csv",
            "platform" => $type
        );

        $self->find_metafiles(
            "data_type" => "file",
            "file_path" => "/RunInfo.xml",
            "platform" => $type
        );
        $self->find_metafiles(
            "data_type" => "file",
            "file_path" => "/runParameters.xml",
            "platform" => $type
        );
    }

    return;
}

################################################################################
sub find_metafiles {
#
# Used only by the 'check_metafiles' subroutine
#
    my $self = shift;

    my $options = { @_ };

    my $data_type = $options->{'data_type'};
    my $file_path = $options->{'file_path'};
    my $platform = $options->{'platform'};

    (my $var_type = $file_path) =~ s/.*\///;

    my $name = join (" ", $var_type, $data_type);

    $self->writelog(BODY => "---Finding $name---");

    my $path =
        ($platform eq "HiSeq" ? $self->{'HiSeq_Folder'} :
        $self->{'slide_folder'}) . $file_path;

    if (-e $path && $options->{ "data_type" } eq "directory" ? -d $path : -r $path) {

        $self->{ $var_type } = $path;
        $self->writelog(BODY => "Found: $path\n");
    }
    else {

        $self->writelog(BODY => "No $name found; tried: $path\n");
        exit $self->EXIT_NO_METAFILES;
    }

    return;
}

################################################################################
sub get_slide_data_from_Geneus {
#
# Searches the LIMS for slide metadata
#
    my $self = shift;

    if (!-e "$ENV{HOME}/.pgpass") {

        $self->writelog(BODY => "Invalid ~/.pgpass file. Exiting.\n");
        exit $self->exit_no_pgpass;
    }

    my $connection = DBI->connect(
        'DBI:Pg:dbname=UNDEF;host=UNDEF',
        undef,
        undef,
        {
            RaiseError => 1,
            ShowErrorStatement => 0,
            AutoCommit => 0
        }
    ) or die DBI->errstr;

    my $query = qq{
        SELECT
        project.parent_project AS "Project",
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
        mapset.limsid AS "Mapset LIMS ID",
        mapset.primary_library AS "Primary Library",
        replace( mapset.primary_library, '_', '') AS "Filename Prefix",
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

    $cursor->bind_param(1, $self->{'slide_name'} . ".%.%");

    my ($field_names, $fetch_datas);

    if ($cursor->execute) {

        $field_names = $cursor->{'NAME'};
        $fetch_datas = $cursor->fetchall_arrayref;
    }
    else {
        $self->writelog(BODY => "Cannot execute query");
        exit $self->exit_no_slide;
    }

    $cursor->finish;
    $connection->disconnect;

    unless ($field_names || $fetch_datas) {
        $self->writelog(BODY => "Slide not in LIMS: $self->{ 'Slide data' }");
        exit $self->exit_no_slide;
    }
    else {
        $self->writelog(BODY => "---Finding mapset information---");
    }

    my @mapsets;

    $self->{'Slide data'} = {

        map {
            my %hash; 

            @hash{ @$field_names } = @$_;

            $hash{$_} ||= 0 for (
                'isize Max',
                'isize Min',
                'isize Manually Reviewed',
                'Failed QC',
                'Alignment Required'
            );

            $hash{$_} ||= "" for (
                'Library Protocol',
                'Aligner',
                'Researcher Annotation',
                'Capture Kit'
            );

            push @mapsets, "$hash{'Primary Library'} => $hash{'Mapset'}";

            my $prefix = $hash{'Filename Prefix'};

            delete $hash{'Filename Prefix'};

            $prefix => \%hash;
        }
        @$fetch_datas
    };

    $self->writelog(BODY => "Found: $_") for sort @mapsets;
    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub get_prefix_map_from_Geneus {
#
# Searches the LIMS for slide metadata
#
    my $self = shift;

    if (!-e "$ENV{HOME}/.pgpass") {

        $self->writelog(BODY => "Invalid ~/.pgpass file. Exiting.\n");
        exit $self->exit_no_pgpass;
    }

    my $connection = DBI->connect(
        'DBI:Pg:dbname=UNDEF;host=UNDEF',
        undef,
        undef,
        {
            RaiseError => 1,
            ShowErrorStatement => 0,
            AutoCommit => 0
        }
    ) or die DBI->errstr;

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
        $self->writelog(BODY => "Cannot execute query");
        exit $self->exit_no_slide;
    }

    $cursor->finish;
    $connection->disconnect;

    unless ($fetch_datas) {
        $self->writelog(BODY => "!!!!Slide not in LIMS: $self->{ 'Slide data' }");
        exit $self->exit_no_slide;
    }
    else {
        $self->writelog(BODY => "---Finding prefix and path associations---");
    }

    $self->{'Prefix map'} = { map { $_->[0] => $_->[1] } @$fetch_datas };

    $self->writelog(BODY => "Found: $_ => $self->{'Prefix map'}{$_}") for
        sort keys %{ $self->{'Prefix map'} };

    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub find_fastq_files {
#
# finds and checksums FASTQ files
#
    my $self = shift;
    my $type = shift;

    $self->writelog(BODY => "---Finding FASTQ files---");
    
    my $slide = $self->{'slide_name'};
    my $dir = $self->{'BaseCalls'};
    
    my %exts = (
        ".1.fastq.gz" => ".2.fastq.gz",
        ".1.fastq" => ".2.fastq",
        "_R1_001.fastq.gz" => "_R2_001.fastq.gz",
        "_R1_001.fastq" => "_R2_001.fastq",
    );
    
    my $files = [
        sort {
            ($a =~ /_S(\d+)[_\.]/)[0] <=> ($b =~ /_S(\d+)[_\.]/)[0] ||
            $a cmp $b
        }
        map { 
            my @type;

            for my $ext (keys %exts) {

                if (-e $_ . $ext && -e $_ . $exts{$ext}) {
                    @type = ($ext, $exts{$ext});
                }
            }

            $_ . ($type[0] ? $type[0] : "_Sample_unpaired"),
            $_ . ($type[1] ? $type[1] : "_Sample_unpaired")
        }
        keys %{
            {
                map { s/(?:\.\d|_R\d_001)?\.fastq(?:\.gz)?//; $_ => 1 }
#                grep { /\/$slide\// && !/Undetermined/ } 
                File::Find::Rule->file->name('*.fastq{,.gz}')->in($dir)
            }
        }
    ];

    unless ($files) {
        $self->writelog(BODY => "No FASTQ files in $dir");
        exit $self->EXIT_NO_FASTQ;
    }

    for (@$files) {
        unless ($_ =~ /_Sample_unpaired/) {
            $self->writelog(BODY => "Found: $_");
        }
        else {
            $self->writelog(BODY => "Bad sample: $_");
            $self->writelog(BODY => "$dir contains unpaired FASTQ files");
            exit $self->EXIT_NO_FASTQ;
        }
    }

    $self->{'FASTQ files'} = $files;

    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub find_mapping_files {
#
# finds and checksums BAM/VCF files
#
    my $self = shift;
    my $type = shift;

    $self->writelog(BODY => "---Finding BAM/VCF files---");

    my $slide = $self->{'slide_name'};
    my $dir = $self->{'BaseCalls'};

    my @dirs =
        sort { (stat $a)[9] <=> (stat $b)[9] }
        File::Find::Rule->directory->maxdepth(1)->name('Alignment*')->in($dir);

    chomp ($dir = shift @dirs);

    my @exts = (
        ".bam",
        ".vcf"
    );

    my $files = [
        sort {
            ($a =~ /_S\d+\./)[0] <=> ($b =~ /_S\d+\./)[0] ||
            $a cmp $b
        }
        map {
            my @type;

            for my $ext (@exts) {

                push @type, $ext if -e $_ . $ext;
            }

            $_ . ($type[0] ? $type[0] : "_Sample_missing"),
            $_ . ($type[1] ? $type[1] : "_Sample_missing")
        }
        keys %{
            {
                map { s/(?:\.vcf|\.bam)//; $_ => 1 }
#                grep { /\/$slide\// && !/Undetermined/ } 
                File::Find::Rule->file->name('*.bam','*.vcf')->in($dir)
            }
        }
    ];

    for (@$files) {
        unless ($_ =~ /_Sample_missing/) {
            $self->writelog(BODY => "Found: $_");
        }
        else {
            $self->writelog(BODY => "Bad sample: $_");
            $self->writelog(BODY => "$dir contains missing Mapping files");
            exit $self->EXIT_NO_FASTQ;
        }
    }

    $self->{'Mapping file types'} = scalar @exts;

    $self->{'Mapping files'} = $files;

    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub checksum_files {
#
# Calculates CRC and file sizes
#
    my $self = shift;
    my $type = shift;

    $self->writelog(BODY => "---Calculating checksums---");

    $self->{$type} = [
        map {
            my ($crc, $size) = $self->checksum(FILE => $_);

            (my $file = $_) =~ s/.*\///;
            (my $name = $_) =~ s/.*\/|_S\d+[\._].*//g;

            $self->writelog(BODY => "Stats: $file => $crc, $size");

            {
                "name" => $name,
                "file" => $_,
                "crc" => $crc,
                "size" => $size
            };
        }
        @{ $self->{$type} }
    ];

    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub check_datafiles {

    my $self = shift;

    $self->writelog(BODY => "---Checking FASTQ and Mapping file counts---");

    my $ref1 = scalar @{ $self->{'FASTQ files'} };
    my $ref2 = scalar @{ $self->{'Mapping files'} };
    my $ref3 = scalar keys %{ $self->{'Slide data'} };

    if ($ref1 - 2 == $ref2 && $ref2 == $ref3 * $self->{'Mapping file types'}) {

        $self->writelog(BODY => "Found: $ref1 FASTQ files");
        $self->writelog(BODY => "Found: $ref2 Mapping files");
        $self->writelog(BODY => "Found: $ref3 Mapsets");
        $self->writelog(BODY => "---Done---\n");
    }
    else {

        $self->writelog(BODY => "Slide missing datafiles and/or mapset information");
        exit $self->EXIT_NO_FASTQ;
    }

    return;
}

################################################################################
sub copy_mapping_files_to_donor_dirs {
#
# Checksums the datafiles
#
    my $self = shift;

    $self->writelog(BODY => "---Copying BAM/VCF to donor directories---");

    for (@{ $self->{'Mapping files'} }) {

        my $ref = $self->{'Slide data'}{ $_->{'name'} };

        (my $ext = $_->{'file'}) =~ s/[^\.]+\.//;

        my $new_path = join ("/",
            $self->{'Prefix map'}{ (split ("_", $ref->{'Donor'}, 2))[0] },
            $ref->{'Donor'},
            $ext eq "bam" ? 'seq_mapped' : 'variants/miseq_reporter');

        my $new_filename = $ref->{'Mapset'} . "." . $ext;

        if (!-d $new_path) {

            my $dir = eval { mkpath($new_path) };
            unless ($dir) {

                $self->writelog(BODY => "Failed to create $new_path");
                exit $self->EXIT_NO_FASTQ;
            }
        }

        my $new_file = $new_path . "/" . $new_filename;

        my $rv = copy($_->{'file'}, $new_file);

        $self->writelog(BODY => "cp $_->{'file'} $new_file");
        $self->writelog(BODY => uc $ext . " COPY FAILED") if $rv == 0;
    }

    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub DESTROY {
#
# Destructor; write logging information to log file
#
    my $self = shift;
    my $errmsg;

    unless ($self->{'EXIT'} > 0) {

        if ($self->{'import_attempted'} eq 'yes' && $self->{'import_succeeded'} eq 'yes') {

            $errmsg = "none";
            $self->{'EXIT'} = 0;

            my $subj = "Re: $self->{'slide_name'} RAW INGESTION -- SUCCEEDED";
            my $body = "RAW INGESTION of $self->{'slide_name'} successful\n";
            $body .= "See log file: $self->{'hostname'}:$self->{'log_file'}";

            $self->send_email(SUBJECT => $subj, BODY => $body) unless $self->{'no_email'} == 1;
        }

        elsif ($self->{'import_attempted'} eq 'no') {

            $errmsg = "import not attempted";
            $self->{'EXIT'} = 17;

            my $subj = "Re: $self->{'slide_name'} RAW INGESTION -- NOT ATTEMPTED";
            my $body = "RAW INGESTION of $self->{'slide_name'} not attempted\n";
            $body .= "See log file: $self->{'hostname'}:$self->{'log_file'}";

            $self->send_email(SUBJECT => $subj, BODY => $body) unless $self->{'no_email'} == 1;
        }
    }

    $self->writelog(BODY => "EXECLOG: errorMessage " . $errmsg);
#    $self->writelog(BODY => "EXECLOG: elapsedTime " . $self->elapsed_time);
    $self->writelog(BODY => "EXECLOG: stopTime " . $self->timestamp);
    $self->writelog(BODY => "EXECLOG: exitStatus " . $self->{'EXIT'});

    close $self->{'log_fh'};

    if ($self->{'EXIT'} == 0) {

        my $asset = $self->{'slide_name'} . "_ingest_log";

        my $assetid = $self->{'mf'}->asset_exists(NAMESPACE => $self->{'ns'}, ASSET => $asset);

        $assetid = $self->{'mf'}->create_asset(NAMESPACE => $self->{'ns'}, ASSET => $asset) unless $assetid;

        my $status = $self->{'mf'}->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});
    }

    return;
}

################################################################################
# # #
# #       EXIT CODES
#
################################################################################
sub EXIT_NO_FOLDER {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: No run folder: $self->{'slide_folder'}";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 1;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 1;
}
sub EXIT_BAD_PERMS {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: Bad permissions on $self->{'slide_folder'}";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 2;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 2;
}
sub EXIT_LIVEARC_ERROR {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: LiveArc error";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 9;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 9;
}

sub EXIT_ASSET_EXISTS {
        my $self        = shift @_;
        my $subj        = $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
        my $body        = "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
        $body           .= "\nFailed: LiveArc asset exists";
        $body           .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
        #$self->send_email(SUBJECT => $subj, BODY => $body);
        $self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

        $self->{'EXIT'} = 12;
        return 12;
}


sub EXIT_IMPORT_FAILED {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: LiveArc import failed";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 16;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 16;
}

sub EXIT_ASSET_NOT_EXISTS {
        my $self        = shift @_;
        my $subj        = $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
        my $body        = "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
        $body           .= "\nFailed: Requested LiveArc asset does not exist";
        $body           .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
        #$self->send_email(SUBJECT => $subj, BODY => $body);
        $self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

        $self->{'EXIT'} = 24;
        return 24;
}

sub EXIT_NO_BCL {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: No BCL files found";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 40;
    return 40;
}


sub EXIT_NO_METAFILES {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: Required metadata files not found";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 41;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 41;
}
sub EXIT_NO_FASTQ {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: FASTQ files were not found";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 42;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 42;
}

sub exit_no_pgpass {

    my $self = shift;

    my $subj = "Ingestion of $self->{'slide_folder'} failed";
    my $body = "Failed: Cannot read ~/.pgpass file\n";
    $body .= "See log file: $self->{'hostname'}:$self->{'log_file'}";

    $self->send_email(SUBJECT => $subj, BODY => $body) unless $self->{'no_email'} == 1;
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 48;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});

    return 48;
}

sub exit_no_slide {

    my $self = shift;

    my $subj = "Ingestion of $self->{'slide_folder'} failed";
    my $body = "Failed: Cannot find slide in LIMS\n";
    $body .= "See log file: $self->{'hostname'}:$self->{'log_file'}";

    $self->send_email(SUBJECT => $subj, BODY => $body) unless $self->{'no_email'} == 1;
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 49;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});

    return 49;
}


sub EXIT_NO_SAMPLESHEET {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: Cannot read sample sheet";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 49;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 49;
}
sub EXIT_BAD_SAMPLESHEET {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: Bad sample sheet";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 50;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 50;
}

sub EXIT_NO_PBS {
        my $self        = shift @_;
        my $subj        = $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
        my $body        = "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
        $body           .= "\nFailed: PBS scripts were not created";
        $body           .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
        #$self->send_email(SUBJECT => $subj, BODY => $body);
        $self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

        $self->{'EXIT'} = 51;
        return 51;
}

sub EXIT_NO_MAPPED {
    my $self    = shift;
    my $subj    = "Ingestion of $self->{'slide_folder'} failed";
    my $body    = "Failed: BAM/VCF files were expected and not found";
    $body               .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
    $self->send_email(SUBJECT => $subj, BODY => $body) unless($self->{'no_email'} == 1);
    $self->writelog(BODY => "EXECLOG: errorMessage $body");

    $self->{'EXIT'} = 52;
    $self->set_lims_flag(BODY => $body, EXIT => $self->{'EXIT'});
    return 52;
}

sub EXIT_NO_REFGENOME {
        my $self        = shift @_;
        my $subj        = $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
        my $body        = "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
        $body           .= "\nFailed: A reference genome could not be inferred";
        $body           .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
        #$self->send_email(SUBJECT => $subj, BODY => $body);
        $self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

        $self->{'EXIT'} = 52;
        return 52;
}

sub EXIT_NO_LOGDIR {
        my $self        = shift @_;
        my $subj        = $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
        my $body        = "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
        $body           .= "\nFailed: PBS log file directory could not be created";
        $body           .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
        #$self->send_email(SUBJECT => $subj, BODY => $body);
        $self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

        $self->{'EXIT'} = 57;
        return 57;
}

sub EXIT_NO_LIB {
        my $self        = shift @_;
        my $subj        = $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
        my $body        = "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
        $body           .= "\nFailed: Missing library";
        $body           .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
        #$self->send_email(SUBJECT => $subj, BODY => $body);
        $self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

        $self->{'EXIT'} = 57;
        return 57;
}

sub EXIT_NO_SEQRESDIR {
        my $self        = shift @_;
        my $subj        = $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
        my $body        = "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
        $body           .= "\nFailed: Could not find or create seq_results directory";
        $body           .= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
        #$self->send_email(SUBJECT => $subj, BODY => $body);
        $self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

        $self->{'EXIT'} = 61;
        return 61;
}

# SET exit(17) = import not attempted (but only used in DESTROY())


################################################################################
sub set_lims_flag {
#
#
#
    my $self = shift;

    my $options = { @_ };

    $self->{'gl'} = QCMG::DB::Geneus->new(
        server => 'http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/',
        verbose => 0
    );

    $self->{'gl'}->connect;

    return unless $self->{'glpid'};

    my $flag = $options->{'EXIT'} > 0 ? 1 : 0;

    my ($data, $slide) = $self->{'gl'}->torrent_run_by_process($self->{'glpid'});

    my $uri; $uri = $slide->{$_}->{'output_uri'} for keys %$slide;

    my $xml = $self->{'gl'}->set_raw_ingest_status($uri, $options->{'BODY'}, $flag);

    unless ($xml) {
        $self->{'gl'}->unset_raw_ingest_status($uri);
        $xml = $self->{'gl'}->set_raw_ingest_status($uri, $options->{'BODY'}, $flag);
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

