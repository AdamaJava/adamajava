package QCMG::FileDir::q3QsnpDirRecord;

##############################################################################
#
#  Module:   QCMG::FileDir::q3QsnpDirRecord.pm
#  Creator:  John V Pearson
#  Created:  2015-02-07
#
#  This class is based on QSnpDirRecord but modified for the new q3 qSNP
#  which does not create DCC files and the new qannotate pipeline.
#
#  $Id$
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

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

# Establish class global list of filename search patterns

BEGIN {
    # Standard fields we report for a variant run.  These MUST exactly
    # match those assigned in method completion_report().
    my @variant_run = qw( qsnp_run_status
                          annotation_status
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
    my %patterns = ( ini          => '^([A-Z]+_\d+\.ini)$',
                     qsnp_cs_log  => '^(qsnp_cs.log)$',
                     main_vcf     => '^([A-Z]+_\d+\.vcf)$',
                     qanno_vcf    => '^([A-Z]+_\d+\.qanno.vcf)$',
                     mafall       => '^([A-Z]+_\d+\.qanno.maf)$',
                     mafall_log   => '^([A-Z]+_\d+\.qanno.maf.log)$'
                   );

    # This is our list of completion statuses.  In general, the higher
    # the number, the higher the completion.
    my %status  = ( 5  => 'qSNP files incomplete or badly named',
                    10 => 'qSNP running',
                    15 => 'qannotate files incomplete or badly named',
                    20 => 'qannotate running',
                    95 => 'complete'
                  );

    %CLASS_GLOBALS = ( patterns    => \%patterns,
                       statuses    => \%status,
                       variant_run => \@variant_run );
}


sub new {
    my $class  = shift;
    my %params = @_;

    die "You must supply a dir parameter to a new q3QsnpDirRecord"
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

    my $dir   = $self->dir;
    my $files = `find $dir -type f`;

    # Remove directory path from file name
    my @files = map { s/.*\///g; $_ } split /\n/, $files;
    qlogprint( 'found ',scalar(@files), " files in $dir\n" );


    # Look for key files using the globally-defined patterns

    my %patterns = %{ $CLASS_GLOBALS{patterns} };
    foreach my $key (keys %patterns) {
        my $pattern = $patterns{$key};

        my @matching_files = ();
        foreach my $file (@files) {
            # Find matching files
            if ($file =~ /$pattern/) {
                push @matching_files, $file;
            }

        }

        # We are expecting one and only one file to match each pattern.
        # If we got no matches then that could be because it's not
        # finished yet so only warn about that at high verbose levels
        # BUT more than one match is always a big problem so whinge
        # straight away and DO NOT choose between the files.

        my $file_count = scalar(@matching_files);
        if ($file_count > 1) {
            # More than one file matching pattern is a problem
            warn "more than one file found matching pattern for $key: ".
                 join(',',@matching_files). "\n";
        }
        elsif ($file_count == 0) {
            # No files file matching pattern *may* be a problem
            warn "no file found matching pattern for $key in dir ". $self->dir ."\n"
                if ($self->verbose > 1);
        }
        else {
            # There can be only one ...
            $self->_set_file( $key, $matching_files[0] );
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


sub completion_report {
    my $self = shift;
  
    my $qsnp_complete       = $self->qsnp_complete_status;
    my $annotation_complete = $self->annotation_complete_status;
    
    my $status = 95;  # assume complete until proven otherwise
    if ($qsnp_complete == 1) {
        $status = 5;  # qSNP files incomplete or badly named
    }
    elsif ($qsnp_complete == 2) {
        $status = 10;  # qSNP running
    }
    elsif ($annotation_complete == 1) {
        $status = 15;  # qannotate files incomplete or badly named
    }
    elsif ($annotation_complete == 2) {
        $status = 20;  # qannotate running
    }

    return $status;
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


sub qsnp_complete_status {
    my $self = shift;

    my @files = qw( ini
                    qsnp_cs_log
                    main_vcf );

    return $self->_is_complete( \@files, 3600 );
}


sub annotation_complete_status {
    my $self = shift;

    my @files = qw( qannon_vcf );

    return $self->_is_complete( \@files, 3600 );
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


sub variant_run_fields {
    my $self = shift;
    # Can be called OO or QCMG:FileDir::q3QsnpDirRecord->... style
    return @{ $CLASS_GLOBALS{variant_run} };
}



1;
__END__


=head1 NAME

QCMG::FileDir::q3QsnpDirRecord - data structure for q3 qSNP call directory


=head1 SYNOPSIS

 use QCMG::FileDir::q3QsnpDirParser;

 my $dir = '/mnt/seq_results/icgc_ovarian/AOCS_001/variants/qSNP/4NormalVs7Primary';
 my $fact = QCMG::FileDir::q3QsnpDirParser->new( verbose => 1 );
 my $ra_dirs = $fact->parse( $dir );


=head1 DESCRIPTION

This module is a data structure to hold key information about a qSNP variant 
calling directory.  You would not typically create one of these via the
new method, rather you would use the parse() method from the factory
module QCMG::FileDir::q3QsnpDirParser to parse a directory and get back an
array of objects of this class.

This class is closely related to QCMG::FileDir::GatkDirRecord since GATK
variant runs are usually post-processed by qSNP in GATK mode.


=head1 PUBLIC METHODS

=over

=item B<new()>
 
 my $obj = QCMG::FileDir::q3QsnpDirRecord( dir => /my/qsnp/dir',
                                           verbose => 1 );

iF you really must create objects of this class outside of
QCMG::FileDir::q3QsnpDirParse then this method takes one compulsory value
(dir) and one optional (verbose).

=item B<get_file()>

 $qsnp->get_file( 'ini' );
 $qsnp->get_file( 'main_vcf' );

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
 ini            '^([A-Z]+_\d+\.ini)$'
 qsnp_cs_log    '^([A-Z]+_\d+\.cs\.log)$'
 ...

=item B<verbose()>

 $qsnp->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:grendeloz@gmail.com>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014
Copyright (c) QIMR Berghofer Medical Research Institute 2015

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
