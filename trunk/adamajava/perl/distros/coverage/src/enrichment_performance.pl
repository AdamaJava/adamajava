#!/usr/bin/perl -w

##############################################################################
#
#  Program:  enrichment_performance.pl
#  Author:   John V Pearson
#  Created:  2013-07-21
#
#  Tasks associated with assessing performace for enrichment platforms
#  including capture and amplicon panels.
#
#  $Id: pbstools.pl 2705 2012-08-20 03:35:59Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use Storable qw( dclone );
use XML::LibXML;

use QCMG::FileDir::Finder;
use QCMG::QBamMaker::MapsetCollection;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 2705 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: pbstools.pl 2705 2012-08-20 03:35:59Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMANDS' )
        unless (scalar @ARGV > 0);

    $CMDLINE = join(' ',@ARGV);
    my $mode = shift @ARGV;

    # Each of the modes invokes a subroutine, and these subroutines 
    # are often almost identical.  While this looks like wasteful code 
    # duplication, it is necessary so that each mode has complete 
    # independence in terms of processing input parameters and taking
    # action based on the parameters.

    my @valid_modes = qw( help man version mapsets );

    if ($mode =~ /^$valid_modes[0]$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );

    }
    elsif ($mode =~ /^$valid_modes[1]$/i) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }
    elsif ($mode =~ /^$valid_modes[2]$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^$valid_modes[3]/i) {
        mapsets();
    }
    else {
        die "enrichment_performance mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub mapsets {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/MAPSETS' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|outdir=s'           => \$params{outdir},        # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams if $params{verbose};

    die "No outfile specified\n" unless $params{outfile};
    die "No outdir specified\n" unless $params{outdir};

    my $msc = QCMG::QBamMaker::MapsetCollection->new( verbose => $params{verbose} );
    $msc->initialise_from_lims;

    qlogprint $msc->mapset_count, " mapsets found in lims\n";
    $msc->apply_constraint( 'capture_kit', '.+' );
    qlogprint $msc->mapset_count, " mapsets found with non-empty capture_kit value\n";
    $msc->apply_constraint( 'failed_qc', '0' );
    qlogprint $msc->mapset_count, " mapsets found that passed QC\n";
    $msc->apply_constraint( 'material', '1:DNA' );
    qlogprint $msc->mapset_count, " mapsets found with material = 1:DNA\n";

    # Report on mapsets so far
    my @mapsets = $msc->mapsets;

    log_by_capture_kit( @mapsets );
    log_by_capture_kit_by_library( @mapsets );

    # Now we will clone off 3 copies of the MapsetCollection so we can
    # use constraints to pull of the 1:Normal, 3:Normal, 4:Normal.
    
    qlogprint "Select out only mapsets for normal samples:\n";
    my $msc1 = dclone( $msc );
    $msc1->apply_constraint( 'sample_code', '1:Normal blood' );
    qlogprint "\t", $msc1->mapset_count, "\tmapsets found of type 1:Normal blood\n";

    my $msc3 = dclone( $msc );
    $msc3->apply_constraint( 'sample_code', '3:Normal' );
    qlogprint "\t", $msc3->mapset_count, "\tmapsets found of type 3:Normal control (adjacent)\n";

    my $msc4 = dclone( $msc );
    $msc4->apply_constraint( 'sample_code', '4:Normal' );
    qlogprint "\t", $msc4->mapset_count, "\tmapsets found of type 4:Normal control (other site)\n";

    # Collect our normals back together and lets get cooking
    my @normals = ( $msc1->mapsets, $msc3->mapsets, $msc4->mapsets );

    log_by_capture_kit( @normals );
    log_by_capture_kit_by_library( @normals );

    # We only want one mapset from each library to avoid overrepresenting
    # data from runs where the samples were multiplexed prior to capture.

    my %by_lib = ();
    foreach my $mapset (@normals) {
        push @{ $by_lib{ $mapset->attribute( 'capture_kit' ) }->{ $mapset->attribute( 'primary_library' ) } },
             $mapset;
    }
    my @selected = ();
    foreach my $capture_kit (sort keys %by_lib) {
        foreach my $library (sort keys %{ $by_lib{$capture_kit} }) {
            # We will check each mapset until we find one on disk and if
            # we don't find any then we just move on
            my @mapsets = @{ $by_lib{ $capture_kit }->{ $library } };
            foreach my $rec (@mapsets) {
                my $pathname = $rec->attribute( 'parent_project_path' ) .'/'.
                               $rec->attribute( 'project' ) .'/seq_mapped/'.
                               $rec->attribute( 'mapset' ) .'.bam';
                if (-r $pathname) {
                    # If we find the file then save it and exit search loop
                    push @selected, $rec;
                    last;
                }
            }
        }
    }
    qlogprint scalar(@selected), " mapsets selected for use in enrichment analysis\n";
    #print Dumper $selected[0];

    write_to_file( $params{ outfile }, \@selected );
    write_pbs_scripts( $params{ outdir }, \@selected );

    qlogend;
}


sub log_by_capture_kit {
    my @mapsets = @_;

    my %by_kit = ();
    foreach my $mapset (@mapsets) {
        $by_kit{ $mapset->attribute( 'capture_kit' ) }++;
    }
    qlogprint "Summary by capture_kit:\n";
    foreach my $capture_kit (sort keys %by_kit) {
        qlogprint "\t", $by_kit{$capture_kit}, "\t", $capture_kit, "\n";
    }
}


sub log_by_capture_kit_by_library {
    my @mapsets = @_;

    my %by_lib = ();
    foreach my $mapset (@mapsets) {
        $by_lib{ $mapset->attribute( 'capture_kit' ) }->{ $mapset->attribute( 'primary_library' ) }++;
    }
    my %captures = ();
    foreach my $capture_kit (sort keys %by_lib) {
        foreach my $library (sort keys %{ $by_lib{$capture_kit} }) {
            $captures{$capture_kit}++;
        }
    }
    qlogprint "Summary of captures collapsed by library:\n";
    foreach my $kit (sort keys %captures) {
        qlogprint "\t", $captures{$kit}, "\t", $kit, "\n";
    }
}


sub write_to_file {
    my $file    = shift;
    my $ra_recs = shift;

    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open output file $file for writing: $!"
        unless defined $outfh;

    my @fields = qw( capture_kit parent_project project mapset );

    $outfh->print( join( "\t", @fields, 'pathname' ), "\n" );

    my $id = 1;
    foreach my $rec (@{ $ra_recs }) {

        # Because we check earlier, we can assume that all mapsets
        # that make it this far are on disk and readable.
        my @values = map { $rec->attribute( $_ ) } @fields;
        my $pathname = $rec->attribute( 'parent_project_path' ) .'/'.
                       $rec->attribute( 'project' ) .'/seq_mapped/'.
                       $rec->attribute( 'mapset' ) .'.bam';
        $outfh->print( join( "\t", $id++, @values, $pathname ), "\n" );
    }

    $outfh->close;
}


sub bamname_from_mapset {
    my $rec = shift;
    return $rec->attribute( 'parent_project_path' ) .'/'.
           $rec->attribute( 'project' ) .'/seq_mapped/'.
           $rec->attribute( 'mapset' ) .'.bam';
}


sub covpfstem_from_mapset {
    my $rec = shift;
    my $capture = $rec->attribute( 'capture_kit' );
    $capture =~ s/[^\w\d_]//g;
    $capture =~ s/\s\b/\u/g;
    return $rec->attribute( 'project' ) .'.'.
           $rec->attribute( 'mapset' ) .'.'.
           $capture;
}


sub write_pbs_scripts {
    my $dir        = shift;
    my $ra_mapsets = shift;

    foreach my $mapset (@{ $ra_mapsets }) {
        write_pbs_script( $dir, $mapset ); 
    }
}


sub write_pbs_script {
    my $dir    = shift;
    my $mapset = shift;

    my %gffs = (
        '29 Gene Cancer Panel (TargetSEQ)' =>
            '/panfs/share/qsegments/hg19_TargetSeq29_20111003.gff3',
        'Human All Exon 50Mb (SureSelect)' =>
            '/panfs/share/qsegments/SureSelect_All_Exon_50mb_with_annotation.hg19.gff3',
        'Human Exome (TargetSEQ)' =>
            '/panfs/share/qsegments/TargetSeq_exome_probe_regions_hg19.gff3',
        'Human Rapid Exome (Nextera)' =>
            '/panfs/share/qsegments/CEX_Manifest_01242013.gff3',
        'Mouse Exome (Nimblegen)' =>
            '/panfs/share/qsegments/110624_MM9_exome_L2R_D02_EZ_HX1.gff3',
        'QCMG Custom 109 Gene (TargetSEQ)' =>
            '/panfs/share/qsegments/QCMG_109_design.gff3',
        'SeqCap EZ Human Exome Library v3.0 (Nimbelgen)' =>
            '/panfs/share/qsegments/SeqCap_EZ_Exome_v3.gff'
        );

#    qlogprint "processing for $pbs_script_pathname\n" if $self->verbose;

    my $script = <<'EOHEADER';
#!/bin/bash

##########################################################################
#
#  Generator: enrichment_preformance.pl
#  Creator:   ~~USER~~
#  Created:   ~~DATETIME~~
#
#  This is a PBS submission job for calculating per-feature coverage
#
##########################################################################
#
#PBS -N ~~PBSJOBNAME~~
#PBS -S /bin/bash
#PBS -q mapping
#PBS -r n
#PBS -l walltime=10:00:00,ncpus=8,mem=15gb
#PBS -m ae
#PBS -M ~~EMAIL~~

# Cause shell to exit immediately if any command exits with non-zero status
set -e

module load samtools
module load java/1.7.13
module load picard
module load adama/nightly

EOHEADER

    my $bam = bamname_from_mapset( $mapset );
    my $bai = $bam . '.bai';
    my $gff = $gffs{ $mapset->attribute( 'capture_kit' ) };
    my $out = $dir .'/'. covpfstem_from_mapset( $mapset ) .'.qcovpf.xml';
    my $log = $dir .'/'. covpfstem_from_mapset( $mapset ) .'.qcovpf.log';
    my $pbs = $dir .'/'. covpfstem_from_mapset( $mapset ) .'.qcovpf.pbs';
    my $lck = $pbs .'.lck';

    # For ease of tracking down the matching PBS .o and .e files, push the
    # PBS JOBID and JOBNAME into the lockfile.
    $script .= "# Create lockfile\n" .
               "touch $lck\n" .
               "echo PBS_JOBID: \$PBS_JOBID >> $lck\n" .
               "echo PBS_JOBNAME: \$PBS_JOBNAME >> $lck\n\n";

    # call qcoverage
    $script .= "# Call qcoverage\n".
               "qcoverage -t seq -n 7 --per-feature --xml \\\n".
               "          --gff3   $gff \\\n".
               "          --bam    $bam \\\n".
               "          --bai    $bai \\\n".
               "          --output $out \\\n".
               "          --log    $log \n\n";

    # Cleanup lock file
    $script .= "# Remove lockfile\n" .
               "rm -f $lck\n";

    # Do final substitutions
    my $user     = $ENV{USER};
    my $email    = $ENV{QCMG_EMAIL};
    my $datetime = localtime() .'';
    my $pbsjob   = $mapset->attribute( 'project' ) .'_qcovpf';

    $script =~ s/~~USER~~/$user/smg;
    $script =~ s/~~EMAIL~~/$email/smg;
    $script =~ s/~~DATETIME~~/$datetime/smg;
    $script =~ s/~~PBSJOBNAME~~/$pbsjob/smg;

    # Write out script file
    my $outfh = IO::File->new( $pbs, 'w' );
    die "Unable to open file $pbs for writing: $!" 
        unless defined $outfh;
    $outfh->print( $script );
    $outfh->close;

}


__END__

=head1 NAME

enrichment_preformance.pl - assesss various enrichment platforms


=head1 SYNOPSIS

 enrichment_performance.pl command [options]


=head1 ABSTRACT

This script is a collection of modes that help assess the performance of
enrichment platforms used by QCMG.


=head1 COMMANDS

 mapsets        - outpur details of all LIMS mapsets with capture platform
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 MAPSETS

Query LIMS for all mapsets that have a value for capture platform

 -o | --outfile       output file in tap-separated text format
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages


=head1 DESCRIPTION



=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: pbstools.pl 2705 2012-08-20 03:35:59Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013
Copyright (c) John Pearson 2013

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
