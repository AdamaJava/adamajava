#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qBamMaker.pl
#  Author:   John V Pearson
#  Created:  2012-06-27
#
#  A perl program to query the QCMG LIMS and output reports and scripts
#  designed to create merged BAMs suitable for variant calling.
#
#  $Id: qBamMaker.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::FileDir::Finder;
use QCMG::Util::QLog;
use QCMG::Util::Util qw( qcmgschema_old_to_new qcmgschema_new_to_old );
use QCMG::QBamMaker::AmpliconMode;
use QCMG::QBamMaker::AutoMode;
use QCMG::QBamMaker::MapsetCollection;

use vars qw( $SVNID $REVISION $VERSION $CMDLINE );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qBamMaker.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( Project          => '',
                   Donor            => '',
                   Sample           => '',
                  'Primary Library' => '',
                  'Sample Code'     => '',
                   Material         => '',
                   Aligner          => '',
                   Mapset           => '',
                  'Capture Kit'     => '',
                  'Failed QC'       => '',
                  'Sequencing Platform' => '',
                   outfile          => '',
                   logfile          => '',
                   conorder         => 'pdrsebatcfq',
                   pbsdir           => '',
                   uptodate         => '',
                   help             => '',
                   autodonor        => '',
                   autoproject      => '',
                   amplicon         => '',
                   forcepbs         => 0,
                   automode         => 1,
                   verbose          => 0,
                   man              => 0 );
    $VERSION = 0;
    $CMDLINE = join(' ',@ARGV);

    my $results = GetOptions (
           'p|project=s'          => \$params{Project},           # -p
           'd|donor=s'            => \$params{Donor},             # -d
           'e|sample=s'           => \$params{Sample},            # -e
           'b|library=s'          => \$params{'Primary Library'}, # -b
           's|smcode=s'           => \$params{'Sample Code'},     # -s
           'r|material=s'         => \$params{Material},          # -r
           'a|aligner=s'          => \$params{Aligner},           # -a
           't|mapset=s'           => \$params{Mapset},            # -t
           'c|capture=s'          => \$params{'Capture Kit'},     # -c
           'f|platform=s'         => \$params{'Sequencing Platform'}, # -f
           'q|failedqc=s'         => \$params{'Failed QC'},       # -q
           'z|conorder=s'         => \$params{conorder},          # -z
           'o|outdir=s'           => \$params{outdir},            # -o
           'n|name=s'             => \$params{name},              # -n
           'l|logfile=s'          => \$params{logfile},           # -l
           'v|verbose+'           => \$params{verbose},           # -v
             'version!'           => \$VERSION,                   # --version
             'pbsdir=s'           => \$params{pbsdir},            # --pbsdir
             'autodonor=s'        => \$params{autodonor},         # --autodonor
             'autoproject=s'      => \$params{autoproject},       # --autoproject
             'amplicon=s'         => \$params{amplicon},          # --amplicon
             'automode=i'         => \$params{automode},          # --automode
             'uptodate=s'         => \$params{uptodate},          # --uptodate
             'forcepbs'           => \$params{forcepbs},          # --forcepbs
           'h|help|?'             => \$params{help},              # -?
           'man|m'                => \$params{man}                # -m
           );

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }
    pod2usage(1) if $params{help};
    pod2usage(-exitstatus => 0, -verbose => 2) if $params{man};

    if ($params{outdir}) {
       $params{outdir} .= '/' unless $params{outdir} =~ /\/$/;
    }
    
    # Every seq_final BAM PBS file will be written out regardless of
    # whether or not the underlying BAM file is uptodate or whether
    # the mapsets exist on disk.  Dangerous!
    if ($params{forcepbs}) {
        $QCMG::QBamMaker::MapsetCollection::FORCE_PBS = 1;
    }


    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    # At this point we split processing into "regular" and "auto" modes.

    if ($params{autodonor}) {
        qlogprint( "running in --autodonor mode\n" ) if $params{verbose};
        autodonor_mode( \%params );
    }
    elsif ($params{autoproject}) {
        qlogprint( "running in --autoproject mode\n" ) if $params{verbose};
        autoproject_mode( \%params );
    }
    elsif ($params{amplicon}) {
        qlogprint( "running in --amplicon mode\n" ) if $params{verbose};
        amplicon_mode( \%params );
    }
    else {
        regular_mode( \%params );
    }

    #warn "!!!\n";
    #warn 'qBamMaker is under active development by both QX and JP so '.
    #     'it is probably best to avoid using it if you can for the '.
    #     "next few weeks.  The new version will have many new features.\n";
    #warn "!!!\n";

    qlogend();
}


sub autoproject_mode {
    my $rh_params = shift;
    my %params = %{ $rh_params };

    # For each directory within autoproject that matches one of the
    # patterns, call autodonor (and don't forget to set automode).

    $params{automode} = 1;
    if ($params{autoproject} =~ /^(.*)\/([^\/]+)\/([^\/]+)\/{0,1}$/) {
        $params{RootDir} = $1;
        $params{Project} = $2;
        $params{Pattern} = $3;
    }
    else {
        die 'Cannot parse project out of [',
            $params{autoproject},"]\n";
    }

    qlogprint( 'parsed autoproject directory: ',
               '  root=',$params{RootDir},
               '  project=',$params{Project},
               '  pattern=',$params{Pattern}, "\n" ) if $params{verbose};

    my $projectdir = $params{RootDir} .'/'.
                     $params{Project};

    my $ff = QCMG::FileDir::Finder->new( verbose => $params{verbose} );
    qlogprint( "running find_directory on $projectdir with pattern ",
               $params{Pattern},"\n" );
    my @dirs = $ff->find_directory( $projectdir, $params{Pattern} );
    qlogprint( 'found ', scalar(@dirs),
               " directories that match the pattern\n" );

    foreach my $dir (@dirs) {
        # Use a pattern to make sure that our matching directory sits
        # directly below the Project directory.
        if ($dir =~ /^$projectdir\/([^\/]+)\/{0,1}$/) {
            $params{Donor} = $1;
            $params{autodonor} = $dir;
            qlogprint( 'calling --autodonor for ', $params{autodonor},"\n" )
                if $params{verbose};
            autodonor_mode( \%params );
        }
    }
}


sub autodonor_mode {
    my $rh_params = shift;
    my %params = %{ $rh_params };

    if ($params{autodonor} =~ /^(.*)\/([^\/]+)\/([^\/]+)\/{0,1}$/) {
        $params{RootDir} = $1;
        $params{Project} = $2;
        $params{Donor}   = $3;
    }
    else {
        die 'Cannot parse project and donor out of [',
            $params{autodonor},"]\n";
    }

    my $auto = QCMG::QBamMaker::AutoMode->new( parent_project => $params{Project},
                                               project        => $params{Donor},
                                               rootdir        => $params{RootDir},
                                               verbose        => $params{verbose} );
    my $rh_mapsets_as_groups = 
        $auto->apply_profile( $params{automode} );

    # Construct the seq_final directory where BAMs should go
    my $bam_dir = $params{autodonor};
    $bam_dir .= '/' unless ($bam_dir =~ /.*\/$/);
    $bam_dir .= 'seq_final/';

    # Construct the directory where the PBS files should go
    my $pbs_dir = $params{pbsdir};
    $pbs_dir = '.' unless $pbs_dir;  # here dir if no dir given
    
    if ($params{automode} == 1) {
        $auto->write_pbs_scripts( pbs_dir       => $pbs_dir,
                                  bam_dir       => $bam_dir,
                                  mapset_groups => $rh_mapsets_as_groups,
                                  cmdline       => $CMDLINE );
    }
}


sub amplicon_mode {
    my $rh_params = shift;
    my %params = %{ $rh_params };

    if ($params{amplicon} =~ /^(.*)\/([^\/]+)\/([^\/]+)\/{0,1}$/) {
        $params{RootDir} = $1;
        $params{Project} = $2;
        $params{Donor}   = $3;
    }
    else {
        die 'Cannot parse project and donor out of [',
            $params{amplicon},"]\n";
    }

    my $ampl = QCMG::QBamMaker::AmpliconMode->new( parent_project => $params{Project},
                                                   project        => $params{Donor},
                                                   rootdir        => $params{RootDir},
                                                   verbose        => $params{verbose} );

    # Add and classify any BAMs from seq_amplicon directory
    $ampl->add_BAMs_from_directory( $params{amplicon} .'/seq_amplicon' );

    my $rh_mapsets_as_groups = $ampl->process;

    # Construct the seq_final directory where BAMs should go
    my $bam_dir = $params{amplicon};
    $bam_dir .= '/' unless ($bam_dir =~ /.*\/$/);
    $bam_dir .= 'seq_final/';

    # Construct the directory where the PBS files should go
    my $pbs_dir = $params{pbsdir};
    $pbs_dir = '.' unless $pbs_dir;  # here dir if no dir given
    
    $ampl->write_pbs_scripts( pbs_dir       => $pbs_dir,
                              bam_dir       => $bam_dir,
                              mapset_groups => $rh_mapsets_as_groups,
                              cmdline       => $CMDLINE );
}


sub regular_mode {
    my $rh_params = shift;
    my %params = %{ $rh_params };

    die "--outdir and --name must be specified if --pbsdir is specified\n"
       if ($params{pbsdir} and (! $params{outdir} or ! $params{name}));

    my $msc = QCMG::QBamMaker::MapsetCollection->new(
                  verbose => $params{verbose} );
    $msc->cmdline( $CMDLINE );
    $msc->initialise_from_lims;

    # Apply any constraints

    $msc->set_constraint_order( $params{conorder} );
    foreach my $constraint ($msc->get_constraint_order) {
        # You can't check 'true' here because 'Failed QC'
        # needs to use '0' as a pattern so length() is the solution
        my $c_name = qcmgschema_new_to_old( $constraint );
        if (defined $params{$c_name} and length($params{$c_name})) {
            $msc->apply_constraint( $constraint, $params{$c_name} );
            # Cope with case that no mapsets passed the constraint
            last if ($msc->mapset_count == 0);
        }
    }

    # Write PBS script to file or report to screen

    if ( $msc->mapset_count > 0 ) {
        qlogprint( $msc->mapset_count, " mapsets passed all constraints\n" );
        if ($params{pbsdir}) {
            make_pbs_script( \%params, $msc );
        }
        elsif ($params{uptodate}) {
            # not implemented yet
        }
        else {
            print $msc->mapsets_to_string;
        }
    }
    else {
        warn "all processing halted as no records passed all constraints\n";
    }

}


sub make_pbs_script {
    my $rh_params = shift;
    my $msc       = shift;

    # Construct a name for our glorious seq_final BAM

    # Slam all donors (hopefully only one!) together for naming outputs
    my %donors = ();
    $donors{ $_->{'Donor'} }++ foreach $msc->mapsets;
    my $donor = join( '_', sort keys %donors);
    # Add the user-supplied name stem and the name of the creator
    my $final_stem = $donor .'.'. $rh_params->{name} .'.'. $ENV{USER};
    # Add the destination directory and an extension
    my $final_bam = $rh_params->{outdir} . $final_stem .'.bam';

    # Create name for PBS job based on donor ID
    my $pbs_job_name = 'qbm' . $donor;
    $pbs_job_name =~ s/[^A-Za-z0-9]//g;

    $msc->write_pbs_script( final_bam_pathname  => $final_bam,
                            final_bam_name_stem => $final_stem,
                            bam_dir             => $rh_params->{outdir},
                            pbs_job_name        => $pbs_job_name,
                            pbs_script_pathname => $rh_params->{pbsdir}
                          );

}


__END__

=head1 NAME

qBamMaker.pl - Perl script for creating merged BAMs


=head1 SYNOPSIS

 qBamMaker.pl [options]


=head1 ABSTRACT

As we move away from pre-made merged BAMs and towards just-in-time
merged BAMs that are only created immediately prior to variant calling,
we need a tool that can assemble collections of BAMs and intelligently
design merging (and deduping) commands ready for submission to the QCMG
cluster.

=head1 OPTIONS

 -p | --project       project pattern
 -d | --donor         donor pattern
 -e | --sample        sample pattern
 -b | --library       library pattern
 -s | --smcode        smcode pattern
 -r | --material      material pattern
 -a | --aligner       aligner pattern
 -t | --mapset        mapset pattern
 -c | --capture       capture kit pattern
 -f | --platform      sequencing platform
 -q | --failedqc      QC pattern

      --pbsdir        directory where PBS script(s) is to be written
      --uptodate      Check that specified BAM conains all report mapsets
 -z | --conorder      Order in which constraints will be applied
 -o | --outdir        Directory to place final BAM into (compulsory if --pbs)
 -n | --name          Stem to be used in naming output files
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message
 -m | --man           print full man page

      --autodonor     auto run for given donor directory
      --autoproject   auto run for given project directory

  # 'amplicon=s'         => \$params{amplicon},          # --amplicon
  # 'automode=i'         => \$params{automode},          # --automode
  # 'forcepbs'           => \$params{forcepbs},          # --forcepbs

The first group of 11 CLI parameters specify strings
that will be pattern-matched (case-insensitive) against mapsets pulled
from the Geneus LIMS.  These params are called constraints. By
specifying multiple constraints you can assemble a list of BAMs that you
might want to merge into a single BAM for analysis.  For somatic
variants, you will need to use this tool twice - once to create the
tumour BAM and once for the normal BAM.

You can also select for fields with empty values by using the string
NULL as the pattern you wish to match.  This is particularly useful for
capture kit where for whole genomes you wish to select mapsets that do
NOT have a capture kit.

N.B. You will B<almost always> want to specify '-q 0' to ensure that
BAMs that failed QC are ignored.

=head1 DESCRIPTION

=head2 Commandline Parameters

=over

=item B<--autoproject>

This option takes a string that represents a directory and pattern that
can be used to identify donor directories.  The directory should be
absolute and everything after the final '/' character is assumed to be
the pattern.  Examples might work best:

 ICGC pancreatic  /mnt/seq_results/icgc_pancreatic/APGI_....
 ICGC ovarian     /mnt/seq_results/icgc_ovarian/AOCS_...
 Brain met        /mnt/seq_results/smgres_brainmet/SLJS_Q...
 Endometrial      /mnt/seq_results/smgres_endometrial/PPPP_....

Also note that the pattern must match the entire donor directory name so
for APGI IDs that end with an underscore and 4 digits, the pattern must
be 'APGI_....' but Ovarian IDs only end with 3 digits so the pattern
'AOCS_...' works.

The --autoproject mode is really just a wrapper for
--autodonor mode.

=item B<--autodonor>

=item B<-p | --project>

Project.  Typical values include: icgc_pancreatic, icgc_ovarian.
smgres_special.

=item B<-d | --donor>

Donor.  Typical values include: COLO_829, APGI_2353, AOCS_001.

=item B<-r | --material>

Material.  Typical values include: 1:DNA, 2:RNA, 3:protein.

This field along with B<--smcode> replaces the old TD/ND/XD system and in
many cases you will need to specify both -m and -r to select the BAMs
you want.  Note that the field contains both the number and the text
description so you can select using either although case must be
exercised because a badly chosen pattern can easily match more than you
expected.
The QCMG wiki page 'Sample Coding' shows all available values
for this field.

=item B<-s | --smcode>

Sample Code.  Typical values include 1:Normal blood, 3:Normal control (adjacent), 
4:Normal control (other site), 7:Primary tumour, 8:Mouse xenograft
derived from tumour.

This field along with B<--material> replaces the old TD/ND/XD system and in
many cases you will need to specify both -m and -r to select the BAMs
you want.  Note that the field contains both the number and the text
description so you can select using either although case must be
exercised because a badly chosen pattern can easily match more than you
expected.  For example, the pattern '1' would match both '1:Normal
blood' and '12:Ascites' and the pattern 'normal' would match both
'3:Normal control (adjacent)' and '4:Normal control (other site)'. 
A regex pattern like '^12:' should work fine.  If
unsure, you should match the entire string as that's safest.
The QCMG wiki page 'Sample Coding' shows all available values
for this field.

=item B<-e | --sample>

Sample.  Typical values include: SMGres-PPPP-20110907-09-ND,
ICGC-MSSM-20110516-02-CD, ICGC-DBLG-20101027-22-TD.

You B<must not> use patterns like 'TD' or 'ND' against this field to try
to select normal DNA etc.  The sample names are assigned as samples
arive at QCMG and if subsequent investigations show that there has been
a sample mixup, the sample's annotation will be changed but the name
B<does not> so it is entirely possible that a sample named with a '-TD'
suffix is in fact a Xenograft or normal or ascites etc.  Please use
B<--smcode> and B<--material> to select DNA/RNA or tumour/normal/xeno
etc.

=item B<-b | --library>

Primary Library.  Typical values include: Library_BIA5298A2AR3,
Library_20120601_D, Library_20110916_A + Library_20110920_A. 

=item B<-a | --aligner>

Aligner.  Typical values include: bioscope, lifescope, tmap, bwa.

=item B<-t | --mapset>

Mapset.  Typical values include: T00002_20120517_196.nopd.IonXpress_012,
T00002_20110918_52.nopd.nobc,
S88006_20111215_2_FragPEBC.lane_4.bcB16_03,
S0449_20100603_2_Frag.nopd.nobc.

=item B<-c | --capture>

Capture Kit.  Typical values include: 29 Gene Cancer Panel (TargetSEQ),
Human All Exon 50Mb (SureSelect), Human Exome (TargetSEQ), Mouse Exome
(Nimblegen), QCMG Custom 109 Gene (TargetSEQ).

=item B<-q | --failedqc>

Failed QC.  Value must be one of 0 or 1 where 0 indicates that the BAM
passed QC and a 1 indicates a failure.  You almost certainly want to
specify a value for this field.

=item B<--pbs>

Name of PBS script to be written.
By default, qBamMaker outputs a report of mapsets that match the
supplied constraints but if B<--pbs> is specified, then a PBS script is
output in place of the query report and that PBS script contains all of
the commands to merge and dedup the specified mapsets into a single BAM.
This option takes primacy over B<--uptodate> so if both are specified,
the PBS script will be created.

=item B<--uptodate>

Name of BAM file to be checked for currency.
This mode is useful if you have an existing BAM and wish to work out
whether it is still current - i.e. it contains all of the mapsets that
are available based on the constraints you have specified.
This option is secondary to B<--pbs> so if both are specified,
the PBS script will be created.

=item B<--forcepbs>

Every seq_final BAM PBS file will be written out regardless of
whether or not the underlying BAM file is uptodate or whether
the mapsets exist on disk, etc.  This is very dangerous!  It will let
you launch jobs that write over the top of perfectly good BAM files
which is probably a big mistake.  This option is useful when you are
debugging qbammaker itself but shoula, in most cases, not be needed by
an end user.

=item B<-z | --conorder>

This string specifies the order in which any supplied constraints will
be applied.  It is also used to determine the output order for the
mapset report.  Each constraint is represented by a single letter which
corresponds to the single letter used as a CLI parameter, e.g. 'p' for
Project.  If supplied then the string MUST contain all of the
constraints even if you only plan on supplying a subset of the
constraints.  The default string is 'pdrsebatcqx' which specifies the
constraint order as being:

 1.  p - Project
 2.  d - Donor
 3.  r - Material
 4.  s - Sample Code
 5.  e - Sample
 6.  b - Primary Library
 7.  a - Aligner
 8.  t - Mapset
 9.  c - Capture Kit
 10. q - Failed QC

=item B<-o | --outdir>

Output directory name.  If the --pbs option has been specified to trigger
output of the PBS script then this option becomes compulsory.  The
script will almost certainly create one or more temporary files but
these will all be targetted to be placed on the execution node's
/scratch space.  The final BAM that is the end result of running the PBS
script should be directed to be placed on /panfs or /mnt or some other
directory that is not local to the execution host.

You B<must> use an absolute path (i.e. one that starts with "/") for
this parameter because you can't easily predict the directory that your
job will be running from so any relative pathname is fraught with
trouble - use an absolute pathname and you'll be set.  This parameter is
also used to work out where the log and metrics files should be copied -
they will go to the same directory as the outfile.

=item B<-n | --name>

Name stem.  This character string will be used to name the final BAM and
will also appear in any other log or output files created as part of the
run.  The final BAM will be named according to the pattern
donor.name.user where donor is the donor(s) associated with the mapset
BAMs being merged, name is the value of this parameter and user is the
username of the person who submits the PBS job.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back



=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qBamMaker.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2013

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
