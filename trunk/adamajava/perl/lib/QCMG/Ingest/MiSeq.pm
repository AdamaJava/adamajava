package QCMG::Ingest::MiSeq;

##############################################################################
#
#  Module:   QCMG::Ingest::MiSeq.pm
#  Creator:  Lynn Fink
#  Created:  2012-10-11
#
#  This class contains methods for automating the ingest into LiveArc
#  of raw  Illumina MiSeq sequencing data
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

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

use QCMG::Automation::LiveArc;
use QCMG::Automation::Common;
use QCMG::DB::Geneus;

our @ISA = qw(QCMG::Automation::Common);

use vars qw( $SVNID $REVISION $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED);


##############################################################################
sub new {

    my $that  = shift;

    my $class = ref $that || $that;
    my $self = bless $that->SUPER::new, $class;

        my $options = {};
        $self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
            my $this_sub = (caller(0))[0]."::".(caller(0))[3];
            $options->{uc($_[$i])} = $_[($i + 1)];
        }

        # if NO_EMAIL => 1, don't send emails
        $self->{'no_email'} = $options->{'NO_EMAIL'};

        # set update mode if requested
        $self->{'update'} = 1 if($options->{'UPDATE'});

        $self->{'slide_folder'} = $options->{'SLIDE_FOLDER'};
        $self->{'slide_folder'} = Cwd::realpath($self->{'slide_folder'})."/";

        $self->{'hostname'} = `hostname`;
        chomp $self->{'hostname'};

        # set LIMS process ID (if supplied)
        $self->{'glpid'} = $options->{'GLPID'};
        $self->{'glpid'} = '' if($self->{'glpid'} == 0);

        my $slide_name = $self->slide_name();

        # define LOG_FILE
        if($options->{'LOG_FILE'}) {
            $self->{'log_file'} = $options->{'LOG_FILE'};
        }
        else {
            # DEFAULT: /run/folder/runfolder_ingest.log
            $self->{'log_file'} = $self->AUTOMATION_LOG_DIR.$slide_name."_ingest.log";
        }

        # fail if the user does not have LiveArc credentials
        if(! -e $self->LA_CRED_FILE) {

            print STDERR "LiveArc credentials file not found!\n";
            exit($self->EXIT_LIVEARC_ERROR());
        }

        $self->init_log_file();

        push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

        # set global flags for checking in DESTROY()  (no/yes)
        $IMPORT_ATTEMPTED = 'no';
        $IMPORT_SUCCEEDED = 'no';

        return $self;
}

################################################################################
################################################################################
=pod

B<slide_name()> 
  Get slide name from slide folder name

Parameters:

Requires:
 $self->{'slide_folder'}

Returns:
  scalar slide name 

=cut
sub slide_name {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

        # /scratch/platinumgenomes/primary/110614_SN103_0846_BD03UMACXX.images/
        # -> 110614_SN103_0846_BD03UMACXX

        my $rf                  = $self->{'slide_folder'};
        $rf                     =~ s/\/$//;
        my ($v, $d, $file)      = File::Spec->splitpath($rf);
        $file                   =~ s/\.images//;
        $self->{'slide_name'}   = $file;

        return $self->{'slide_name'};
}


################################################################################
sub toollog {
#
# Write TOOLLOG names and values to log file and any extra EXECLOG lines
#
        my $self = shift @_;

        # java version for Mediaflux/LiveArc
        my $j           = $self->LA_JAVA;
        my $javaver     = `$j -version 2>&1 >/dev/null`;
        $javaver        =~ s/.+?version\s+\"(.+?)\".+/$1/s;

        my $date = $self->timestamp();

        my $msg         .= qq{EXECLOG: javaVersion $javaver\n};
        $msg            .= qq{EXECLOG: javaHome $j\n};

        $msg            .= qq{TOOLLOG: LOG_FILE $self->{'log_file'}\n};
        $msg            .= qq{TOOLLOG: SLIDE_FOLDER $self->{'slide_folder'}\n};
        $msg            .= qq{TOOLLOG: DEFAULT_EMAIL }.$self->DEFAULT_EMAIL."\n";

        $self->writelog(BODY => $msg);

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

    $self->_find_metafiles("filepath" => "/Data/Intensities/BaseCalls");
    $self->_find_metafiles("filepath" => "/InterOp");
    $self->_find_metafiles("filepath" => "/SampleSheet.csv");
    $self->_find_metafiles("filepath" => "/RunInfo.xml");
    $self->_find_metafiles("filepath" => "/runParameters.xml");
    $self->_find_metafiles("filepath" => "/RunParameters.xml");

    return;
}

################################################################################
sub _find_metafiles {
#
# Used only by the 'check_metafiles' subroutine
#
    my $self = shift;

    my $options = { @_ };

    my $filepath = $options->{'filepath'};

    (my $name = $filepath) =~ s/.*\///;

    $self->writelog(BODY => "---$name---");

    my $path = $self->{'slide_folder'} . $filepath;

    if (-e $path && -r $path) {

        $self->{ $name } = $path;
        $self->writelog(BODY => "Found: $path\n");
    }
    else {

        $self->writelog(BODY => "No $name found.\nTried: $path\n");
    }

    return;
}

################################################################################
sub read_samplesheet {
#
# Hash up the samplesheet
#
    my $self = shift;

    $self->writelog(BODY => "---Reading SampleSheet.csv---");

    my @lines = split (/[\r\n]/, read_file($self->{'SampleSheet.csv'}));

    my @headers = map {

        s/,*$//g; $_
    }
    grep {

        /^\[|\],*$/
    }
    @lines;

    my %sample_sheet;
    my @data;
    my $header;

    for my $line (@lines) {

        next if $line eq "";

        if (grep $_ eq $line, @headers) {

            $line =~ s/[][]//g;
            $header = $line;
            next;
        }

        push (@{ $sample_sheet{$header} }, $line);
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

        next if $_ =~ /^#/;

        my %hash;

        @hash{ @data_header } = quotewords(',', 0, $_);

        push (@data, { %hash });
    }

    $sample_sheet{'Data'} = \@data;

    my $ref = $sample_sheet{'Data'};

    unshift (@$ref, { "Sample_Name" => "Undetermined" });

    for my $i (1 .. $#{ $ref }) {

        my $int = $ref->[ $i ];

        my ($idx1, $idx2) = ($int->{'index'}, $int->{'index2'});

        $int->{'Index'} ||=
            $idx1 && $idx2 ? join ("-", $idx1, $idx2) :
            $idx1 && !$idx2 ? $idx1 :
            "nobc";

        $int->{'Sample_Name'} ||= $int->{'Sample_ID'} || $int->{'SampleID'};

        %$int =
            map { $_ => $int->{ $_ } }
            grep { $_ eq "Sample_Name" || $_ eq "Index" }
            keys %$int;

        $self->writelog(
            BODY => join (" ",
                $int->{'Sample_Name'}, "is on row $i with index", $int->{'Index'}
            )
        );
    }

    $self->writelog(BODY => "---done---\n");

    $self->{'Sample Sheet'} = \%sample_sheet;

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
        sample.biospecimen_id AS "BioSpecimen ID",
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
        $self->writelog(BODY => "Slide not in LIMS: $self->{'Slide data'}");
        exit $self->exit_no_slide;
    }
    else {
        $self->writelog(BODY => "---Finding mapset information---");
    }

    my @mapsets;

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

        push @mapsets, "$hash{'Sample Name'} => $hash{'Mapset'}";

        $self->
            {'Slide data'}
            { delete $hash{'Lane'} }
            { delete $hash{'Index'} }
            = \%hash;

    }
    @$fetch_datas;

    $self->writelog(BODY => "Found: $_") for sort @mapsets;
    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub get_parent_path_prefixes {
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
        $self->writelog(BODY => "!!!!Slide not in LIMS: $self->{'Slide data'}");
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
# finds FASTQ files
#
    my $self = shift;

    $self->writelog(BODY => "---Finding FASTQ files---");

    my $slide = $self->{'slide_name'};
    my $dir = $self->{'BaseCalls'};

    my %data;

    my %FASTQs = map {

        s/_R[1-2]_(.*?)$/_<KEY>_$1/;

        $_ => 1
    }
    grep {

        !/Undetermined/
    }
    File::Find::Rule->maxdepth(1)->file->name('*.fastq.gz')->in($dir);

    for (keys %FASTQs) {

        (my $f1 = $_) =~ s/_<KEY>_/_R1_/;
        (my $f2 = $_) =~ s/_<KEY>_/_R2_/;

        my @reads = $f1;

        push (@reads, $f2) if $self->{'Reads'} == 2;

        for (@reads) {

            die "Could not find file: $_\n" unless -f $_;

            $self->writelog(BODY => "Found: $_");

            push (@{ $self->{'FASTQs'} }, $_);
        };
    }

    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub checksum_files {
#
# Calculates CRC and file sizes
#
    my $self = shift;

    $self->writelog(BODY => "---Calculating checksums---");

    $self->{'FASTQs'} = [

        map {

            my ($crc, $size) = $self->checksum(FILE => $_);

            (my $file = $_) =~ s/.*\///;
            (my $idx = $_) =~ s/.*_S(\d+)[\._].*/$1/g;

            $self->writelog(BODY => "Stats: $file => $crc, $size");

            {
                "idx" => $idx,
                "file" => $_,
                "crc" => $crc,
                "size" => $size
            };
        }
        @{ $self->{'FASTQs'} }
    ];

    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub copy_FASTQ_to_seq_raw {
#
# Copies and renames the FASTQs to /mnt/seq_raw/<slide>
#
    my $self = shift;

    $self->writelog(BODY => "---Copying and renaming FASTQs---");

    my $slide_path = join ('/', $self->{'SEQ_RAW_PATH'}, $self->{'slide_name'});

    make_path($slide_path);

    my $data = $self->{'Sample Sheet'}{'Data'};

    my %samples = map { $_ => $data->[$_]{'Index'} } 1 .. $#{ @$data };

    for (@{ $self->{'FASTQs'} }) {

        my $ref = $self->{'Slide data'}{'1'}{ $samples{ $_->{'idx'} } };

        my ($suffix) = (basename($_->{'file'}) =~ /(_S\d+_L001_R[1-2]_001\.fastq\.gz)/);

        (my $stem = $ref->{'BioSpecimen ID'}) =~ s/ /_/g;

        $self->writelog(BODY => "Copying: " . $_->{'file'});
        $self->writelog(BODY => "To: " . "$slide_path/$stem$suffix");

        copy($_->{'file'}, "$slide_path/$stem$suffix") or die "Copy failed: $!";
    }

    $self->writelog(BODY => "---Done---\n");

    return;
}

################################################################################
sub apply_adapter_masking {
#
# Adds N's to reads that have been trimmed during conversion to FASTQ
#
    my $self = shift;

    return unless exists $self->{'Sample Sheet'}{'Settings'}{'Adapter'};

    $self->writelog(BODY => "---Applying N-Mask---");

    my $slide_path = join ('/', $self->{'SEQ_RAW_PATH'}, $self->{'slide_name'});

    my @files = File::Find::Rule->maxdepth(1)->file->name('*.fastq.gz')->in($slide_path);

    my $pm = new Parallel::ForkManager(8);

    for my $file (@files) {

        $self->writelog(BODY => "Masking: $file");

        my $pid = $pm->start and next;

        my $f;

        $f=1 if $file =~ /_R1_001\.fastq\.gz$/;
        $f=2 if $file =~ /_R2_001\.fastq\.gz$/;

        die "Could not get read number from filename\n" unless $f;

        my $rl = int($self->{'Sample Sheet'}{'Reads'}[$f - 1]);

        tie *INFILE, 'IO::Zlib', $file, "rb";
        tie *OUTPUT, 'IO::Zlib', "$file.tmp", "wb9";

        while (my $seq_header = <INFILE>) {

            my $seq = <INFILE>;
            my $qual_header = <INFILE>;
            my $qual = <INFILE>;

            chomp $seq_header;
            chomp $seq;
            chomp $qual_header;
            chomp $qual;

            $seq .= 'N' x ($rl - length ($seq));
            $qual .= '#' x ($rl - length ($qual));

            print OUTPUT "$seq_header\n$seq\n$qual_header\n$qual\n";
        };

        close INFILE;
        close OUTPUT;

        move("$file.tmp", $file);

        $pm->finish;
    }

    $pm->wait_all_children;

    $self->writelog(BODY => "---Done---\n");

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

    my $self = shift;

    my $options = {};

    $self->{'gl'} = QCMG::DB::Geneus->new(
        server => 'http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/',
        verbose => 0
    );

    $self->{'gl'}->connect;

    # don't try to set the flag if a LIMS entry doesn't exist
    return unless $self->{'glpid'};

    my $flag        = 0;
    $flag           = 1 if($options->{'EXIT'} > 0);

    my ($data, $slide)      = $self->{'gl'}->torrent_run_by_process($self->{'glpid'}); ## ???

        #print Dumper $slide;
        my $uri;
        foreach (keys %{$slide}) {
                $uri    = $slide->{$_}->{'output_uri'};
        }

        #                                               uri, status, is_error (0=not
        #                                               error, 1= error)
        my ($xml)       = $self->{'gl'}->set_raw_ingest_status($uri, $options->{'BODY'}, $flag);
        if(! $xml) {
                $self->{'gl'}->unset_raw_ingest_status($uri);
                $xml    = $self->{'gl'}->set_raw_ingest_status($uri, $options->{'BODY'}, $flag);
        }

        return;
}

################################################################################
1;

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2014

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
