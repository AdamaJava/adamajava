package QCMG::FileDir::QSnpDirRecord;

##############################################################################
#
#  Module:   QCMG::FileDir::QSnpDirRecord.pm
#  Creator:  John V Pearson
#  Created:  2010-09-16
#
#  This class is a container for information about a qSNP variants
#  directory.  It was used as the basis for GatkDirRecord so if you make
#  substantial changes here, please sync with GatkDirRecord.
#
#  $Id: QSnpDirRecord.pm 4662 2014-07-23 12:39:59Z j.pearson $
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;

use QCMG::FileDir::DirectoryObject;
use QCMG::FileDir::FileObject;
use QCMG::FileDir::QLogFile;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %CLASS_GLOBALS );

( $REVISION ) = '$Revision: 4662 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: QSnpDirRecord.pm 4662 2014-07-23 12:39:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Establish class global list of filename search patterns

BEGIN {
    # Standard fields we report for a variant run.  These MUST exactly
    # match those assigned in method completion_report().
    my @variant_run = qw( qsnp_run_status
                          somatic_annotation_status
                          somatic_maf_status
                          germline_annotation_status
                          germline_maf_status
                          overall_status
                          qsnplog_toolversion
                          qsnplog_starttime
                          qsnplog_project
                          qsnplog_analysis_uuid
                          analysis_dir
                          qsnplog_normal_bam
                          qsnplog_normal_bam_epoch
                          qsnplog_tumour_bam
                          qsnplog_tumour_bam_epoch
                          oldest_non_ini_file_name
                          oldest_non_ini_file_epoch );

    # Patterns used to match filename in qSNP directory
    my %patterns = ( ini          => '^(\w+_\d+\.ini)$',
                     qsnp_log     => '^(.*qsnp.log)$',
                     somat_dcc1   => '^(.*\.SomaticSNV.dcc1)$',
                     somat_dcc2   => '^(.*\.SomaticSNV.dcc2)$',
                     somat_dccq   => '^(.*\.SomaticSNV.dccq)$',
                     somat_annot  => '^(.*\.SomaticSNV.dccq.annotation.log)$',
                     somat_mafall => '^(.*\.Somatic.ALL.snv.maf)$',
                     somat_mafhc  => '^(.*\.Somatic.HighConfidence.snv.maf)$',
                     somat_mafhcc => '^(.*\.Somatic.HighConfidenceConsequence.snv.maf)$',
                     somat_maflog => '^(.*mafPipelineSomatic.*\.log)$',
                     germl_dcc1   => '^(.*\.GermlineSNV.dcc1)$',
                     germl_dcc2   => '^(.*\.GermlineSNV.dcc2)$',
                     germl_dccq   => '^(.*\.GermlineSNV.dccq)$',
                     germl_annot  => '^(.*\.GermlineSNV.dccq.annotation.log)$',
                     germl_mafall => '^(.*\.Germline.ALL.snv.maf)$',
                     germl_mafhc  => '^(.*\.Germline.HighConfidence.snv.maf)$',
                     germl_mafhcc => '^(.*\.Germline.HighConfidenceConsequence.snv.maf)$',
                     germl_maflog => '^(.*mafPipelineGermline.*\.log)$'
                   );

    # This is our list of completion statuses.  In general, the higher
    # the number, the higher the completion.
    my %status  = ( 5  => 'qSNP files incomplete or badly named',
                    10 => 'qSNP running',
                    15 => 'qannotate somatic files incomplete or badly named',
                    20 => 'qannotate somatic running',
                    25 => 'qmaftools somatic files incomplete or badly named',
                    30 => 'qmaftools somatic running',
                    35 => 'qannotate somatic failed',
                    40 => 'qannotate germline files incomplete or badly named',
                    45 => 'qannotate germline running',
                    50 => 'qmaftools germline files incomplete or badly named',
                    55 => 'qmaftools germline running',
                    95 => 'complete'
                  );

    %CLASS_GLOBALS = ( patterns    => \%patterns,
                       statuses    => \%status,
                       variant_run => \@variant_run );
}


sub new {
    my $class  = shift;
    my %params = @_;

    die "You must supply a dir parameter to a new QSnpDirRecord"
       unless (exists $params{dir} and $params{dir});

    my $self = { dir                 => $params{dir},
                 dirobj              => undef,
                 files               => {},
                 size                => 0,
                 qsnp_log_obj        => undef,
                 oldest_non_ini_file => undef,  # QCMG::FileDir::FileObject
                 verbose             => $params{verbose} || 0 };
    bless $self, $class;

    # Split into thisdir and parentdir
    if ($params{dir} =~ /^(.*)\/([^\/]{1,})\/{0,1}$/) {
        $self->{parentdir} = $1;
        $self->{thisdir} = $2;
        
    }
    else {
        die "Could not pattern match parent out of $params{dir}\n";
    }

    # DirectoryObject only works properly if it has a tree rooted at "/" so
    # we need a parent ($pdir) that starts at root.  We will almost
    # certainly never use it but it needs to exist.  Just saying.
 
    my $pdir = QCMG::FileDir::DirectoryObject->new( name    => $self->{parentdir}, 
                                                    parent  => undef,
                                                    verbose => $self->verbose );
    my $tdir = QCMG::FileDir::DirectoryObject->new( name    => $self->{thisdir}, 
                                                    parent  => $pdir,
                                                    verbose => $self->verbose );
    $self->{dirobj} = $tdir;

    # Parse directory looking for files that match the expected patterns
    # and store QCMG::FileDir::FileObject's for each in $self->{files}

    $self->_search_for_expected_files;
    $self->_parse_qsnp_log_file;

    return $self;
}



sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub dir {
    my $self = shift;
    return $self->{dir};
}


sub size {
    my $self = shift;
    return $self->{size} = shift if @_;
    return $self->{size};
}


sub oldest_non_ini_file {
    my $self = shift;
    return $self->{oldest_non_ini_file};
}


sub qsnp_log {
    my $self = shift;
    return $self->{qsnp_log_obj};
}


sub _search_for_expected_files {
    my $self = shift;

    my $finder = QCMG::FileDir::Finder->new( verbose => $self->verbose );
    
    my %patterns = %{ $CLASS_GLOBALS{patterns} };

    # Look for our signature files using the patterns above
    foreach my $key (keys %patterns) {
        my @files = $finder->find_file( $self->dir, $patterns{$key} );

        # We are expecting one and only one file to match each pattern.
        # If we got no matches then that could be because it's not
        # finished yet so only warn about that at high verbose levels
        # BUT more than one match is always a big problem so whinge
        # straight away and DO NOT choose between the files.

        my $file_count = scalar(@files);
        if ($file_count > 1) {
            # More than one file matching pattern is a big problem
            warn "more than one file found matching pattern for $key: ".
                 join(',',@files). "\n";
        }
        elsif ($file_count == 0) {
            # No files file matching pattern may be a problem
            warn "no file found matching pattern for $key in dir ". $self->dir ."\n"
                if ($self->verbose > 1);
        }
        else {
            # There can be only one ...
            $self->_set_file( $key, $files[0] );
        }
    }
}


sub _set_file {
    my $self = shift;
    my $type = shift;
    my $file = shift;

    # If $file includes a path, we have to pull it off before creating a
    # FileObject.
    if ($file =~ /^.*\/([^\/]{1,})\/{0,1}$/) {
        $file = $1;
    }

    my $fo = QCMG::FileDir::FileObject->new( name    => $file,
                                             parent  => $self->{dirobj},
                                             verbose => $self->verbose );

    return $self->{files}->{$type} = $fo;
}


sub get_file {
    my $self = shift;
    my $type = shift;

    return undef if ! exists $self->{files}->{$type};
    # Returns QCMG::FileDir::FileObject
    return $self->{files}->{$type};
}


sub _parse_qsnp_log_file {
    my $self = shift;

    # If qsnp_log file does not exist then ditch
    my $fileobj = $self->get_file( 'qsnp_log' );
    return undef unless defined $fileobj;
    my $name = $fileobj->full_pathname;

    qlogprint "parsing log file: $name\n" if $self->verbose;

    my $qsnplog = QCMG::FileDir::QLogFile->new( file    => $name,
                                                verbose => $self->verbose );

    $self->{qsnp_log_obj} = $qsnplog;
}


sub completion_report {
    my $self = shift;
  
    my $qsnp_complete                = $self->qsnp_complete_status;
    my $somatic_annotation_complete  = $self->somatic_annotation_complete_status;
    my $somatic_maf_complete         = $self->somatic_maf_complete_status;
    my $germline_annotation_complete = $self->germline_annotation_complete_status;
    my $germline_maf_complete        = $self->germline_maf_complete_status;
    
    my $status = 95;  # assume complete until proven otherwise
    if ($qsnp_complete == 1) {
        $status = 5;  # qSNP files incomplete or badly named
    }
    elsif ($qsnp_complete == 2) {
        $status = 10;  # qSNP running
    }
    elsif ($somatic_annotation_complete == 1) {
        $status = 15;  # qannotate somatic files incomplete or badly named
    }
    elsif ($somatic_annotation_complete == 2) {
        $status = 20;  # qannotate somatic running
    }
    elsif ($somatic_maf_complete == 1) {
        $status = 25;  # qmaftools somatic files incomplete or badly named
    }
    elsif ($somatic_maf_complete == 2) {
        $status = 30;  # qmaftools somatic running
    }
    elsif ($somatic_maf_complete == 4) {
        $status = 35;  # qannotate somatic failed
    }
    elsif ($germline_annotation_complete == 1) {
        $status = 40;  # qannotate germline files incomplete or badly named
    }
    elsif ($germline_annotation_complete == 2) {
        $status = 45;  # qannotate germline running
    }
    elsif ($germline_maf_complete == 1) {
        $status = 50;  # qmaftools germline files incomplete or badly named
    }
    elsif ($germline_maf_complete == 2) {
        $status = 55;  # qmaftools germline running
    }

    # Get info from qsnp.log file if it exists:
    my $rh_qsnplog_exec_attributes = {};
    my $rh_qsnplog_tool_attributes = {};
    my $qsnp_log = $self->qsnp_log;
    if (defined $qsnp_log) {
        $rh_qsnplog_exec_attributes = $qsnp_log->attributes_from_exec_lines;
        $rh_qsnplog_tool_attributes = $self->_parse_qsnp_log_tool_attributes;
    }
    
    # Get info from somatic annotation log file if it exists:
    # Get info from somatic maf creation log file if it exists:
    # Get info from germline annotation log file if it exists:
    # Get info from germline maf creation log file if it exists:

    # To get dates of BAMs, we will parse them.
    #my $nbam = QCMG::QBamMaker::SeqFinalBam->new( filename =>
    #               $rh_qsnplog_tool_attributes->{normal_bam} );
    #my $tbam = QCMG::QBamMaker::SeqFinalBam->new( filename =>
    #               $rh_qsnplog_tool_attributes->{tumour_bam} );
    #
    #               qsnplog_normal_bam         => $nbam->BamName,
    #               qsnplog_normal_bam_epoch   => $nbam->BamMtimeEpoch,
    #               qsnplog_tumour_bam         => $tbam->BamName,
    #               qsnplog_tumour_bam_epoch   => $tbam->BamMtimeEpoch,

    my $nbam_name  = $rh_qsnplog_tool_attributes->{normal_bam};
    my $nbam_epoch = ( stat( $nbam_name ) )[9];
    my $tbam_name  = $rh_qsnplog_tool_attributes->{tumour_bam};
    my $tbam_epoch = ( stat( $tbam_name ) )[9];

    # If you add or change any fields here, you need to make the same
    # changes (including order) to the @variant_run array in CLASS_GLOBALS.

    my %report = ( qsnp_run_status            => $qsnp_complete,
                   somatic_annotation_status  => $somatic_annotation_complete,
                   somatic_maf_status         => $somatic_maf_complete,
                   germline_annotation_status => $germline_annotation_complete,
                   germline_maf_status        => $germline_maf_complete,
                   overall_status             => $status.' - '.$CLASS_GLOBALS{statuses}->{$status},
                   qsnplog_toolversion        => $rh_qsnplog_exec_attributes->{ToolVersion},
                   qsnplog_starttime          => $rh_qsnplog_exec_attributes->{StartTime},
                   qsnplog_project            => $rh_qsnplog_tool_attributes->{project},
                   qsnplog_analysis_uuid      => $rh_qsnplog_tool_attributes->{analysis_uuid},
                   analysis_dir               => $self->dir,
                   qsnplog_normal_bam         => $nbam_name,
                   qsnplog_normal_bam_epoch   => $nbam_epoch,
                   qsnplog_tumour_bam         => $tbam_name,
                   qsnplog_tumour_bam_epoch   => $tbam_epoch,
                   oldest_non_ini_file_name   => $self->oldest_non_ini_file->name,
                   oldest_non_ini_file_epoch  => $self->oldest_non_ini_file->filestat(9),
                   );

    return \%report;
}


sub completion_report_text {
    my $self = shift;
    
    my $rh_data = $self->completion_report();
    my $text = '';
    my @fields = map { (exists $rh_data->{$_} and defined $rh_data->{$_}) ? $rh_data->{$_} : '' }
                 @{ $CLASS_GLOBALS{variant_run} };
                     
    return join( "\t", @fields ) ."\n";
}


sub completion_report_header {
    my $self = shift;
    
    my @fields = @{ $CLASS_GLOBALS{variant_run} };
    return join( "\t", @fields ) ."\n";
}


sub _parse_qsnp_log_tool_attributes {
    my $self = shift;

    # Extract info from the TOOL lines
    my $qsnplog = $self->{qsnp_log_obj};
    my %attribs = ();
    foreach my $rh_line (@{ $qsnplog->lines_by_loglevel( 'TOOL' ) }) {
        if ($rh_line->{message} =~ /patient ID: (\w*)/) {
            $attribs{project} = $1;
        }
        elsif ($rh_line->{message} =~ /analysisId: (\w*)/) {
            $attribs{analysis_uuid} = $1;
        }
        elsif ($rh_line->{message} =~ /normalSampleId:\s(.*)$/) {
            $attribs{normal_sample_id} = $1;
        }
        elsif ($rh_line->{message} =~ /tumourSampleId:\s(.*)$/) {
            $attribs{tumour_sample_id} = $1;
        }
        elsif ($rh_line->{message} =~ /normalBam: \[(.*)\]/) {
            $attribs{normal_bam} = $1;
        }
        elsif ($rh_line->{message} =~ /tumourBam: \[(.*)\]/) {
            $attribs{tumour_bam} = $1;
        }
    }

    return \%attribs;
}


sub get_log_file_attribute {
    my $self = shift;
    my $file = shift;
    my $type = shift;
    my $name = shift;

    return '' unless exists $self->{ $file .'_attrs' }->{ $type }->{ $name };
    return $self->{ $file .'_attrs' }->{ $type }->{ $name };
}


sub qsnp_complete_status {
    my $self = shift;

    my @files = qw( ini
                    qsnp_log
                    somat_dcc1
                    germl_dcc1 );

    return $self->_is_complete( \@files, 3600 );
}


sub somatic_annotation_complete_status {
    my $self = shift;

    my @files = qw( somat_dcc1
                    somat_dcc2
                    somat_dccq
                    somat_annot );

    return $self->_is_complete( \@files, 3600 );
}


sub germline_annotation_complete_status {
    my $self = shift;

    my @files = qw( germl_dcc1
                    germl_dcc2
                    germl_dccq
                    germl_annot );

    return $self->_is_complete( \@files, 3600 );
}


sub somatic_maf_complete_status {
    my $self = shift;

    my @files = qw( somat_mafall
                    somat_mafhc
                    somat_maflog );

    my $status = $self->_is_complete( \@files, 3600 );

    # If the files are all present and times are OK then we need to
    # check in the log file that the job as ExistStatus==0
    if ($status == 0) {
        my $filename = $self->get_file( 'somat_maflog' )->full_pathname;
        my $log = QCMG::FileDir::QLogFile->new( file => $filename,
                                                verbose => $self->verbose );
        my $rh_attribs = $log->attributes_from_exec_lines;
        # Return 4 if no ExistStatus or it is non-zero
        $status = 4 unless ( exists $rh_attribs->{ExitStatus} and
                             $rh_attribs->{ExitStatus} == 0 );
    }

    return $status;
}


sub germline_maf_complete_status {
    my $self = shift;

    my @files = qw( germl_mafall
                    germl_mafhc
                    germl_maflog );

    my $status = $self->_is_complete( \@files, 3600 );

    # If the files are all present and times are OK then we need to
    # check in the log file that the job as ExistStatus==0
    if ($status == 0) {
        my $filename = $self->get_file( 'germl_maflog' )->full_pathname;
        my $log = QCMG::FileDir::QLogFile->new( file => $filename,
                                                verbose => $self->verbose );
        my $rh_attribs = $log->attributes_from_exec_lines;
        # Return 4 if no ExistStatus or it is non-zero
        $status = 4 unless ( exists $rh_attribs->{ExitStatus} and
                             $rh_attribs->{ExitStatus} == 0 );
    }

    return $status;
}


sub _is_complete {
    my $self      = shift;
    my $ra_files  = shift;
    my $time_secs = shift;

    # If the files exist and all of them are at least $time_secs old then
    # we will consider this to be complete.
    #
    # Return codes:
    # 0 = all files present and more than $time_secs old
    # 1 = at least 1 file is incomplete
    # 2 = all files present but some less than $time_secs old
    #
    # We will also store the name and epoch time of the oldest non-ini file
    # found during this process.  We ignore the INI file because it is
    # often made early and never changes even if it gets reused.

    # Time in secs since unix epoch
    my $now = time();

    # Make sure the files exist and are at least $time_secs old
    my $status = 0;
    foreach my $filetype (@{ $ra_files }) {
        # Get QCMG::FileDir::FileObject
        my $file = $self->get_file( $filetype );
        # File-not-found is worse than file-too-new so status=1 is
        # always set even if status=2 has already been set
        if (! defined $file) {
            $status = 1;
        }
        else {
            # If file exists, test age
            if (($now - $file->filestat( 9 )) < $time_secs) {
                $status = 2 if $status == 0;
            }
            # Check and update "oldest non-ini file" status
            if (($filetype ne 'ini') and
                (! defined $self->oldest_non_ini_file or 
                 $file->filestat( 9 ) < $self->oldest_non_ini_file->filestat( 9 ))) {
                $self->{oldest_non_ini_file} = $file;
            }
        }
    }

    return $status;
}


sub count_records {
    my $self = shift;

    # This is the list of files we need to parse.  It could well be that
    # it makes more sense to get this info by parsing the log files so
    # this method may never end up getting implemented.

    my @files = qw( somat_dcc1
                    somat_dcc2
                    somat_dccq
                    somat_mafall
                    somat_mafhc
                    somat_mafhcc
                    germl_dcc1
                    germl_dcc2
                    germl_dccq
                    germl_mafall
                    germl_mafhc
                    germl_mafhcc );
}


sub variant_run_fields {
    my $self = shift;
    # Can be called OO or QCMG:FileDir::QSnpDirRecord->... style
    return @{ $CLASS_GLOBALS{variant_run} };
}



1;
__END__


=head1 NAME

QCMG::FileDir::QSnpDirRecord - data structure for qSNP call directory


=head1 SYNOPSIS

 use QCMG::FileDir::QSnpDirParser;

 my $dir = '/mnt/seq_results/icgc_ovarian/AOCS_001/variants/qSNP/4NormalVs7Primary';
 my $fact = QCMG::FileDir::QSnpDirParser->new( verbose => 1 );
 my $ra_dirs = $fact->parse( $dir );


=head1 DESCRIPTION

This module is a data structure to hold key information about a qSNP variant 
calling directory.  You would not typically create one of these via the
new method, rather you would use the parse() method from the factory
module QCMG::FileDir::QSnpDirParser to parse a directory and get back an
array of objects of this class.

This class is closely related to QCMG::FileDir::GatkDirRecord since GATK
variant runs are usually post-processed by qSNP in GATK mode.


=head1 PUBLIC METHODS

=over

=item B<new()>
 
 my $obj = QCMG::FileDir::QSnpDirRecord( dir => /my/qsnp/dir',
                                         verbose => 1 );

iF you really must create objects of this class outside of
QCMG::FileDir::QSnpDirParse then this method takes one compulsory value
(dir) and one optional (verbose).

=item B<get_file()>

 $qsnp->get_file( 'ini' );
 $qsnp->get_file( 'somat_dccq' );

This method takes a string representing the "type" of file that is
wanted and it returns a QCMG::FileDir::FileObject instance.  The text
and table below show the types of files that can be requested with this
method and the string (first column) that should be passed in to thsi
method.

A qSNP run directory is expected to have a number of files with fixed
naming patterns and the presence or absence of these files can be used
to determine whether the run is complete.
The contents of these files, particularly the log files, can be parsed
to provide information about the run.
This table shows the patterns we will look for and the name we give to
that file/pattern.
If more than one file in the qSNP run directory matches a given pattern
then that is considered an error and the entire directory will be marked
as incomplete.

 Type           Pattern
 ---------------------------------------------------------
 ini            '^(\w+_\d+\.ini)$'
 qsnp_log       '^(qsnp.log)$'
 somat_dcc1     '^(.*\.SomaticSNV.dcc1)$'
 somat_dcc2     '^(.*\.SomaticSNV.dcc2)$'
 somat_dccq     '^(.*\.SomaticSNV.dccq)$'
 somat_annot    '^(.*\.SomaticSNV.dccq.annotation.log)$'
 somat_mafall   '^(.*\.Somatic.ALL.snv.maf)$'
 somat_mafhc    '^(.*\.Somatic.HighConfidence.snv.maf)$'
 somat_mafhcc   '^(.*\.Somatic.HighConfidenceConsequence.snv.maf)$'
 germl_dcc1     '^(.*\.GermlineSNV.dcc1)$'
 germl_dcc2     '^(.*\.GermlineSNV.dcc2)$'
 germl_dccq     '^(.*\.GermlineSNV.dccq)$'
 germl_annot    '^(.*\.GermlineSNV.dccq.annotation.log)$'
 germl_mafall   '^(.*\.Germline.ALL.snv.maf)$'
 germl_mafhc    '^(.*\.Germline.HighConfidence.snv.maf)$'
 germl_mafhcc   '^(.*\.Germline.HighConfidenceConsequence.snv.maf)$'

=item B<completion_report()>

 print $qsnp->completion_report;

Returns a hash containing the values in the following table.
This can be used to create reports including database tables.
The values returned are:

 qsnp_complete                : see below
 somatic_annotation_complete  : see below
 somatic_maf_complete         : see below
 germline_annotation_complete : see below
 germline_maf_complete        : see below
 status                       : numeric summary of fields 1-5
 tool_version                 : qSNP version
 start_time                   : qSNP run start time
 project                      : project (donor)
 analysis_uuid                : UUID for this analysis
 run_directory                : run directory
 normal_bam                   : full pathname of normal BAM
 tumour_bam                   : full pathname of tumour BAM

The first 5 fields in this report are integers taken directly from
methods:
B<qsnp_complete_status()>
B<somatic_annotation_complete_status()>
B<somatic_maf_complete_status()>
B<germline_annotation_complete_status()>
B<germline_maf_complete_status()>.
See below for more details about the values returned from these methods

=item B<completion_report_text()>

 print $qsnp->completion_report_text;

This is a text equivalent of the B<completion_report()> routine and
contains the same information in a tab-separated line of text with a
terminating newline.  This is intended to be used to create reports
including database tables and can be used with
completion_report_header() which will give you a matching header line
for the report.

=item B<completion_report_header()>

This returns a single newline-terminated line of text containing the
tab-separated names of the fields output by B<completion_report_text()>
and in the same order.  It is useful when writing text reports.

=item B<qsnp_complete_status()>

=item B<somatic_annotation_complete_status()>

=item B<somatic_maf_complete_status()>

=item B<germline_annotation_complete_status()>

=item B<germline_maf_complete_status()>

All of these methods return integer values denoting the completion
status of the named qSNP process.  The 5 processes are qSNP itself, the
somatic and germline annotations of variants and generation of somatic
and germline MAF files.  In all cases, a "0" value means that all of the
files are present and more than an hour old so we assume that that piece
of the process is complete.  If a file is missing, which includes cases
where the file exists but is not named in a way that matches the expected
pattern, then a "1" is returned.  If all of the files are present but
one or more have been modified in the past hour then we assume they may
still be being written to so a "2" is returned.

=item B<verbose()>

 $qsnp->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: QSnpDirRecord.pm 4662 2014-07-23 12:39:59Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
