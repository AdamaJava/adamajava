package QCMG::Util::Util;

###########################################################################
#
#  Module:   QCMG::Util::Util.pm
#  Creator:  John V Pearson
#  Created:  2012-11-22
#
#  Non-OO utility functions for key QCMG operations.
#
#  $Id$
#
###########################################################################

use strict;
#use warnings;

use Carp qw( confess );
use Data::Dumper;
use Exporter;
use File::Find;
use Mail::Sendmail;
use POSIX 'strftime';		# for printing timestamp
use UUID::Tiny;

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %QCMG_DEFAULTS %QCMG_MAPS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

our @ISA = qw( Exporter );
our @EXPORT_OK = qw( load_ensembl_API_modules
                     send_email 
                     timestamp
                     fetch_subsequence
                     exec_info
                     qexec_header
                     qcmg_default
                     qcmgschema_new_to_old
                     qcmgschema_old_to_new
                     ranges_overlap
                     split_final_bam_name
                     db_credentials
                    );

BEGIN {
    # This is a one-stop shop for defaults that we should be enforcing
    # across all of our modules and scripts.
    %QCMG_DEFAULTS = (
       hs_ref_fa => '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
       ensembl_version => 70,
    );

    %QCMG_MAPS = (
         qcmgschema_old_to_new => {
             'Donor'                    => 'project',
             'Project'                  => 'parent_project',
             'Material'                 => 'material',
             'Sample Code'              => 'sample_code',
             'Sequencing Platform'      => 'sequencing_platform',
             'Aligner'                  => 'aligner',
             'Capture Kit'              => 'capture_kit',
             'Library Protocol'         => 'library_protocol',
             'Sample'                   => 'sample',
             'Species Reference Genome' => 'species_reference_genome',
             'Reference Genome File'    => 'reference_genome_file',
             'Failed QC'                => 'failed_qc',
             'Primary Library'          => 'primary_library',
             'Mapset'                   => 'mapset',
             },
         qcmgschema_new_to_old => {
             'project'                  => 'Donor',
             'parent_project'           => 'Project',
             'material'                 => 'Material',
             'sample_code'              => 'Sample Code',
             'sequencing_platform'      => 'Sequencing Platform',
             'aligner'                  => 'Aligner',
             'capture_kit'              => 'Capture Kit',
             'library_protocol'         => 'Library Protocol',
             'sample'                   => 'Sample',
             'species_reference_genome' => 'Species Reference Genome',
             'reference_genome_file'    => 'Reference Genome File',
             'failed_qc'                => 'Failed QC',
             'primary_library'          => 'Primary Library',
             'mapset'                   => 'Mapset',
             },
    );

}

=head1 NAME

QCMG::Util::Util - Perl non-OO library of utility functions


=head1 SYNOPSIS

 use QCMG::Util::Util qw( qexec_header qcmg_default );


=head1 DESCRIPTION

This module is not an OO class, rather it contains a collection of
static methods that return QCMG-specific data structures.
To use any of the functions described below, you will need to name the
function as part of the "use" statement for this module, as shown above;


=head1 FUNCTIONS

=pod

B<qcmg_default()> 
 
 qcmg_default( 'hs_ref_fa' );
 qcmg_default( 'ensembl_version' );

This function takes the name of a parameter and returns the default
value that should be used for that parameter within QCMG.  The intent is
to centralise key parameters rather than having them hard-coded into each
of our QCMG modules/scripts.  If the supplied parameter does not exist
in our hash of defaults then undef is returned.
 
Parameters:
 parameter

Returns:
 value

=cut

sub qcmg_default {
    my $key = shift;
    return undef unless exists $QCMG_DEFAULTS{$key};
    return $QCMG_DEFAULTS{$key};
}
 

=pod

B<qcmgschema_old_to_new()> 

Convert old field names from the qcmgchema LIMS tables to the new
format.  For example old 'Project' is new 'parent_project'.  If the
field passed in does not exist, undef is returned.  The match is
case-sensitive.  See also qcmgschema_new_to_old().
 
Parameters:
 old field name

Returns:
 new field name

=cut

sub qcmgschema_old_to_new {
    my $old_name = shift;
    if (exists $QCMG_MAPS{qcmgschema_old_to_new}->{$old_name}) {
         return $QCMG_MAPS{qcmgschema_old_to_new}->{$old_name};
    }
    return undef;
}


=pod

B<qcmgschema_new_to_old()> 

Convert new field names from the qcmgchema LIMS tables to the old
format.  For example new 'parent_project' is old 'Project'.  If the
field passed in does not exist, undef is returned.  The match is
case-sensitive.  See also qcmgschema_old_to_new().
 
Parameters:
 new field name

Returns:
 old field name

=cut

sub qcmgschema_new_to_old {
    my $new_name = shift;
    if (exists $QCMG_MAPS{qcmgschema_new_to_old}->{$new_name}) {
         return $QCMG_MAPS{qcmgschema_new_to_old}->{$new_name};
    }
    return undef;
}


=pod

B<load_ensembl_API_modules()> 

Load Ensembl API modules.  Should be passed a version string that is
the integer version of the Ensembl API that you wish to use - 58, 66,
70 etc.  There is no default so a version string must be supplied and
if the requested API version is not found or not readable, a die will
be thrown.
 
Parameters:
 version

Returns:
 void

=cut

sub load_ensembl_API_modules {
    my $version  = shift;

    # The default location for the Ensembl Perl API used to be Karin
    # Kassahn's home directory but is now in /panfs/share/software
    my $module_dir='/panfs/share/software/EnsemblPerlAPI_v' . $version;

    die "Ensembl API directory for version $version ($module_dir) does not exist\n"
        unless (-d $module_dir);
    die "Ensembl API directory for version $version ($module_dir) exists but is not readable\n"
        unless (-r $module_dir);
             
    # Directly manipulate the @INC module search path list
    unshift @INC, "$module_dir/ensembl/modules";
    unshift @INC, "$module_dir/ensembl-variation/modules";
    unshift @INC, "$module_dir/ensembl-functgenomics/modules";
    unshift @INC, "/panfs/share/software/bioperl-1.2.3";

    # Using 'require' rather than 'use' so we can delay execution of
    # these statements until after option processing.
    require Bio::EnsEMBL::Registry;
    require Bio::EnsEMBL::Variation::VariationFeature;
}


################################################################################
=pod

B<send_email()> 
 Send email to notify of ingestion.
 
Parameters:
 SUBJECT
 BODY
 TO (a single email address)

Returns:
 void

=cut
sub send_email {
        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	shift @_ if($_[0] ne 'T0' || $_[0] ne 'BODY' || $_[0] ne 'SUBJECT');

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	#print Dumper $options;

	#my $email = join ", ", $options->{'TO'};
	my $email = $options->{'TO'};

	# requires editing /etc/mail/sendmail.mc:
	# dnl define(`SMART_HOST',`smtp.your.provider')dnl -> 
	#     define(`SMART_HOST',`smtp.imb.uq.edu.au')
	# then recompiling sendmail, allegedly

	my $fromemail	= 'mediaflux@qcmg-clustermk2.imb.uq.edu.au';
	my $sendmail	= '/usr/sbin/sendmail';

	# echo "To: l.fink@imb.uq.edu.au Subject: BWA MAPPING -- COMPLETED\nMAPPING OF 120523_SN7001240_0047_BD12NAACXX.lane_1.nobc has ended. See log file for status: /panfs/seq_raw//120523_SN7001240_0047_BD12NAACXX/log/120523_SN7001240_0047_BD12NAACXX.lane_3.nobc_sam2bam_out.log" |/usr/sbin/sendmail -v -fmediaflux@qcmg-clustermk2.imb.uq.ed.au l.fink@imb.uq.edu.au

	my $to		= "To: ".$email;
	my $subj	= "Subject: ".$options->{'SUBJECT'};
	my $cmd		= qq{echo "$to\n$subj\n$options->{'BODY'}" | /usr/sbin/sendmail -v -f$fromemail $email}; #"
	`$cmd`;

	#print STDERR qq{$to $subj $fromemail $email\n};
	#print STDERR "$cmd\n";

	return;
}

################################################################################
=pod

B<timestamp()> 
 Generate a timestamp in the format: 2003-02-14-16:37:46
                                     030214
                                     030214163746

Parameters:
 FORMAT 
   - ISO8601       - Default timestamp, ISO 8601 format
   - YYMMDD        - timestamp in YYYYMMDD format 
   - YYMMDDhhmmss  - timestamp in YYYYMMDDhhmmss format 

Returns:
 scalar - timestamp string

=cut
sub timestamp {
        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	#print STDERR "TIMESTAMP ".$options->{'FORMAT'}."\n";

	# should be in ISO 8601 format: [2011-02-19 23:59:99Z]
	my $stamp = lc strftime("[%Y-%m-%d %H:%M:%S]", localtime); 

	# return date in YYMMDD format
	if($options->{'FORMAT'} eq 'YYMMDD') {
		$stamp = uc strftime("%Y%m%d", localtime);
	}
	elsif($options->{'FORMAT'} =~ /yymmddhh/i) {
		$stamp = uc strftime("%Y%m%d%H%M%S", localtime);
	}

	return($stamp);
}

################################################################################
=pod

B<fetch_subsequence()> 

 Return a subsequence from a reference genome given the genomic coordinates

Parameters:
 GENOME		=> path to reference genome
 COORD		=> genomic coordinate (e.g., chr1:101-105)
 COMP		=> return complemented sequence ("n"/"y")
 REVCOMP	=> return reverse complemented sequence ("n"/"y")

QCMG::Util::Util::fetch_subsequence(GENOME => $params{'reference'}, COORD => $chr.":".$start."-".$end, REVCOMP => $revcomp)

Returns:
 scalar - timestamp string

=cut
sub fetch_subsequence {
	require QCMG::SamTools::Sam;

        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $genome	= $options->{'GENOME'};
	my $coord	= $options->{'COORD'};

	my $comp	= 'n';	# default to NO, do not complement
	$comp		= $options->{'COMP'};

	my $revcomp	= 'n';	# default to NO, do not complement
	$revcomp	= $options->{'REVCOMP'};

	#print STDERR "Using genome: $genome\n";

	# extract sequence from FASTA file
	my $fai         = QCMG::SamTools::Sam::Fai->load($genome);
	my $seq         = $fai->fetch($coord);

	if($comp ne 'n') {
		$seq	=~ tr/CATG/GTAC/;
	}
	if($revcomp eq 'y') {
		$seq	= reverse($seq);
		$seq	=~ tr/CATG/GTAC/;
	}

	return($seq);
}


################################################################################

=pod

B<exec_info)> 

Returns information about the running process including PID, UID, etc.

Parameters:
 none

 my $rh_info = exec_info();

Returns:
 hashref - information which may include:
    StartTime
    ProcessID
    RealUID
    EffectiveUID
    OsName
    PerlVersion
    PerlExecutable
    Host
    RunBy
    OsArch
    OsVersion
    ToolName
    ToolVersion
   
=cut

sub exec_info {
    my %info = ();

    $info{StartTime}      = localtime();
    $info{ProcessID}      = $$;
    $info{RealUID}        = $<;
    $info{EffectiveUID}   = $>;
    $info{OsName}         = $^O;
    $info{PerlVersion}    = sprintf("%vd",$^V);
    $info{PerlExecutable} = $^X;

    my $host = `uname -n`;
    $host =~ s/\n//;
    $info{Host} = $host;

    my $user = `id -un`;
    $user =~ s/\n//;
    $info{RunBy} = $user;

    my $osarch = `uname -p`;
    $osarch =~ s/\n//;
    $info{OsArch} = $osarch;

    my $osver = `uname -r`;
    $osver =~ s/\n//;
    $info{OsVersion} = $osver;

    $0 =~ /([^\/]+)$/;
    $info{ToolName} = $1;

    if (defined $::REVISION) {
        $info{ToolVersion} = $::REVISION;
    }

    return \%info;
}


=pod

B<qexec_header> 

Returns a text string that includes information about the running process
including PID, UID, etc.
The string is intended for use as a header that would be included at the top
of an output file so every line has #QEXEC as a prefix.
Each line has a name (e.g. OsName) and a value (e.g. linux) separated by a tab.
If you want this information but as a hash rather than a single string,
see method l<exec_info()> instead.

Parameters:
 none

 my $header = qexec_header();

Returns:
 string - of the format:
 
 #Q_EXEC Uuid    77cb5327-81a0-4aaa-98df-131ba7b68d64
 #Q_EXEC StartTime       Sat May  4 03:16:38 2013
 #Q_EXEC OsName  linux
 #Q_EXEC OsArch  x86_64
 #Q_EXEC OsVersion       2.6.18-164.11.1.el5
 #Q_EXEC ProcessID       25389
 #Q_EXEC RealUID 13263
 #Q_EXEC EffectiveUID    13263
 #Q_EXEC RunBy   nickwaddell
 #Q_EXEC ToolName        qannotate.pl
 #Q_EXEC ToolVersion     3739
 #Q_EXEC PerlVersion     5.8.8
 #Q_EXEC PerlExecutable  /usr/bin/perl
 #Q_EXEC Host    minion36
 
=cut


sub qexec_header {
    my $rh_info = QCMG::Util::Util::exec_info();

    my $text = '';
    $text .= "#Q_EXEC\tUuid\t"           . create_UUID_as_string(UUID_V4) . "\n";
    $text .= "#Q_EXEC\tStartTime\t"      . $rh_info->{StartTime} . "\n";
    $text .= "#Q_EXEC\tOsName\t"         . $rh_info->{OsName} . "\n";
    $text .= "#Q_EXEC\tOsArch\t"         . $rh_info->{OsArch} . "\n";
    $text .= "#Q_EXEC\tOsVersion\t"      . $rh_info->{OsVersion} . "\n";
    $text .= "#Q_EXEC\tProcessID\t"      . $rh_info->{ProcessID} . "\n";
    $text .= "#Q_EXEC\tRealUID\t"        . $rh_info->{RealUID} . "\n";
    $text .= "#Q_EXEC\tEffectiveUID\t"   . $rh_info->{EffectiveUID} . "\n";
    $text .= "#Q_EXEC\tRunBy\t"          . $rh_info->{RunBy} . "\n";
    $text .= "#Q_EXEC\tToolName\t"       . $rh_info->{ToolName} . "\n";
    $text .= "#Q_EXEC\tToolVersion\t"    . $rh_info->{ToolVersion} . "\n";
    $text .= "#Q_EXEC\tPerlVersion\t"    . $rh_info->{PerlVersion} . "\n";
    $text .= "#Q_EXEC\tPerlExecutable\t" . $rh_info->{PerlExecutable} . "\n";
    $text .= "#Q_EXEC\tHost\t"           . $rh_info->{Host} . "\n";

    return $text;
}


=pod

B<ranges_overlap> 

Takes 2 ranges and returns an integer representing if and how the
ranges overlap.  A return value of 0 indicates that the ranges are
disjoint and a positive integer indicates an overlap of some sort and
the integer gives the type of overlap.

Parameters:
 range1_start - positive integer
 range1_end   - positive integer
 range2_start - positive integer
 range2_end   - positive integer

Returns:
 value - integer:
    0 = disjoint
    1 = identical
    2 = first range is within second
    3 = second range is within first
    4 = overlap

=cut


sub ranges_overlap {
    my $start1 = shift;
    my $end1   = shift;
    my $start2 = shift;
    my $end2   = shift;

    # Status:
    # 0 = disjoint
    # 1 = identical
    # 2 = first is within second
    # 3 = second is within first
    # 4 = overlap

    if ($end1 < $start2 or $end2 < $start1) {
        return 0;
    }
    elsif ($start1 == $start2 and $end1 == $end2) {
        return 1;
    }
    elsif ($start2 <= $start1 and $end2 >= $end1) {
        return 2;
    }
    elsif ($start1 <= $start2 and $end1 >= $end2) {
        return 3;
    }
    else {
        return 4;
    }
}


=pod

B<split_final_bam_name> 

Extracts information from a seq_final BAM name.
This routine assumes that the BAM is a final BAM so the format of
the BAM is predictable.
If the BAM name does not match the regex pattern, you will get a 
warning and an undef return.
This is *not* the best way to learn about a BAM - it would be safer to
parse the BAM header and look for the "QN:qlimsmeta" @CO line.
It's also worth noting that the values you get from the filename have
had filename-unfriendly characters removed including spaces, underscores
and colons so the values are often not directly matchable against the
same information pulled from the LIMS or BAM headers.
Having said all that, this can be a simple quick-n-dirth way of 
getting at donor IDs etc.

Parameters:
 string - BAM name

Returns:
 undef - BAM name did not match expected pattern
 hash  - values parsed from BAM name in the form:
         { parent_project   => ...,
           project          => ...,
           material         => ...,
           sample_code      => ...,
           sample           => ...,
           library_protocol => ...,
           capture_kit      => ...,
           aligner          => ...,
           platform         => ... }

=cut


sub split_final_bam_name {
    my $file = shift;

    # Strip off any leading path elements
    $file =~ s/.*\///g;

    # Match
    my $pattern = '([^_]+)_([^_]+)_([^_]+)_([^_]+)_([^_]+)_([^_]+)_([^_]+)_([^_]+)_([^_]+)\.([^\.]+)\.bam';
    if ($file =~ /$pattern/) {
        return { parent_project   => $1,
                 project          => $2,
                 material         => $3,
                 sample_code      => $4,
                 sample           => $5,
                 library_protocol => $6,
                 capture_kit      => $7,
                 aligner          => $8,
                 platform         => $9,
                 user             => $10 };
    }
    else {
        warn "unable to parse filename for  BAM $file\n";
        return undef;
    }
}


=pod

B<db_credentials> 

Splits a string into database connection parameters.

Parameters:
 string - database parameters

Returns:
 hash  - values parsed from database string:
         { host     => ...,
           database => ...,
           username => ...,
           password => ... }
=cut


sub db_credentials {
    my $string = shift;

    my @fields = split /:::/, $string;

    confess 'there should be 4 elements, not ', scalar(@fields),
            " in [$string]\n" unless (scalar(@fields) == 4);

    return { host     => $fields[0],
             database => $fields[1],
             username => $fields[2],
             password => $fields[3] }
}


1;


=pod

=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=item Lynn Fink L<mailto:l.fink@uq.edu.au>

=back


=head1 VERSION

$Id$


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
