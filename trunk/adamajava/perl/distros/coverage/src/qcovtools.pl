#!env perl

##############################################################################
#
#  Program:  qcovtools.pl
#  Author:   John V Pearson
#  Created:  2015-03-15
#
#  Coverage-related modes.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX qw( floor );
use Storable qw(dclone);
use XML::Simple;

use Grz::Util::Util qw( current_user new_timestamp );

use QCMG::DB::Metadata;
use QCMG::FileDir::Finder;
use QCMG::IO::CnvReader;
use QCMG::IO::DccSnpReader;
use QCMG::IO::FastaReader;
use QCMG::IO::GffReader;
use QCMG::IO::INIFile;
use QCMG::IO::LogFileReader;
use QCMG::IO::MafReader;
use QCMG::IO::MafWriter;
use QCMG::IO::PropertiesReader;
use QCMG::IO::qSnpGermlineReader;
use QCMG::IO::TelomereReport;
use QCMG::IO::TsvReader;
use QCMG::IO::VcfReader;
use QCMG::IO::VerificationReader;
use QCMG::Util::Util qw( ranges_overlap split_final_bam_name );
use QCMG::XML::Qmotif;

use Grz::Util::Log;

use vars qw( $SVNID $REVISION $CMDLINE $VERSION $VERBOSE );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures


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
    
    my @valid_modes = qw( help man version 
                          search_bam_headers
                          props_2_gff
                          illumina_panel_manifest
                          );

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
    elsif ($mode =~ /^$valid_modes[4]/i) {
        props_2_gff();
    }
    elsif ($mode =~ /^$valid_modes[3]/i) {
        illumina_panel_manifest();
    }
    else {
        die "qcovtools mode [$mode] is unrecognised; valid modes are:\n   ".
            join("\n   ",@valid_modes) ."\n";
    }
}


sub props_2_gff {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/props_2_gff' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '/panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.properties',
                   outfile     => 'GRCh37_ICGC_standard_v2.properties.gff',
                   source      => 'GRCh37_ICGC_standard_v2.properties',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           's|source=s'           => \$params{source},        # -s
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin();
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    my $props = QCMG::IO::PropertiesReader->new(
                     filename => $params{infile},
                     verbose  => $params{verbose} );

    glogprint( {l=>'INFO'}, 'found ', $props->record_count,
                            ' sequences in properties file ',
                            $params{infile},"\n" );

    my $rh_seqs = $props->records();
    my @keys = sort { $a <=> $b } keys %{ $rh_seqs };

    my $outfh =IO::File->new( $params{outfile}, 'w' )
        or croak 'Can\'t open GFF3 file [', $params{outfile},
                 "] for writing: $!";

    $outfh->print( "##gff-version\t3\n" );
    $outfh->print( '#', join("\t", qw{ seqid source type start end score strand
                                       phase attributes } ), "\n" );

    foreach my $key (@keys) {
        $outfh->print( join("\t", $rh_seqs->{ $key }->{H},
                                  $params{source},
                                  'chrom',
                                  1,
                                  $rh_seqs->{ $key }->{L},
                                  '.',
                                  '.',
                                  '.',
                                  'FASTA_byte_offset='.$rh_seqs->{ $key }->{P} ), "\n" );
    }
    $outfh->close;

    glogend();
}


sub illumina_panel_manifest {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMAND DETAILS/illumina_panel_manifest' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infile      => '',
                   outstem     => '',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outstem=s'          => \$params{outstem},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply input and outstem parameters
    die "You must specify an input file\n" unless $params{infile};
    die "You must specify a stem for output file names\n" unless $params{outstem};

    # Set up logging
    glogfile($params{logfile}) if $params{logfile};
    glogbegin;
    glogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );
    glogparams( \%params );

    my $probe_bed_file     = $params{outstem} . 'probes.bed';
    my $target_bed_file    = $params{outstem} . 'targets.bed';
    my $probe_gff_file     = $params{outstem} . 'probes.gff';
    my $target_gff_file    = $params{outstem} . 'targets.gff';

    my $ini = QCMG::IO::INIFile->new( file    => $params{infile},
                                      verbose => $params{verbose},
                                      unprocessed_rules => 1 );
    die "unable to open file $params{infile} for reading: $!" unless
        defined $ini;

    #foreach my $section ($ini->section_names) {
    #    my @rules = @{ $ini->unprocessed_section( $section ) };
    #    print Dumper $section, scalar( @rules );
    #}

    # Process [Probes]

    my @probes = @{ $ini->unprocessed_section( 'Probes' ) };

    # The first line should be the column headers so use it to check
    # that the critical columns are where we expect them to be.
    my @headers = split /\t/, shift(@probes);
    my %probe_columns = ( 5 => 'Chromosome',
                          6 => 'Start Position',
                          7 => 'End Position' );
    foreach my $pos (sort keys %probe_columns) {
        die "Column $pos should be [$probe_columns{$pos}}] but is [$headers[$pos]]\n"
            unless $headers[$pos] eq $probe_columns{$pos}; 
    }

    my $pbfh = _open_file_and_write_header( $probe_bed_file );
    foreach my $probe (@probes) {
        my @fields = split /\t/, $probe;
        $pbfh->print( join(' ', $fields[5], $fields[6], $fields[7]), "\n" );
    }

    # Process [Targets]

    my @targets = @{ $ini->unprocessed_section( 'Targets' ) };

    # The first line should be the column headers so use it to check
    # that the critical columns are where we expect them to be.
    @headers = split /\t/, shift(@targets);
    my %target_columns = ( 3 => 'Chromosome',
                           4 => 'Start Position',
                           5 => 'End Position' );
    foreach my $pos (sort keys %target_columns) {
        die "Column $pos should be [$target_columns{$pos}}] but is [$headers[$pos]]\n"
            unless $headers[$pos] eq $target_columns{$pos}; 
    }

    my $tbfh = _open_file_and_write_header( $target_bed_file );
    foreach my $target (@targets) {
        my @fields = split /\t/, $target;
        $tbfh->print( join(' ', $fields[3], $fields[4], $fields[5]), "\n" );
    }

    glogend;
}


sub _open_file_and_write_header {
    my $filename = shift;

    my $ofh = IO::File->new( $filename, 'w' );
    die "unable to open file $filename for writing: $!" unless
        defined $ofh;

    glogprint( "opened $filename for writing\n" );

    $ofh->print ( "# Filename: $filename\n" .
                  "# Creator:  ". current_user() ."\n" .
                  "# Software: $0 version $REVISION\n" .
                  "# DateTime: ". new_timestamp(). "\n" );

    return $ofh;
}


__END__

=head1 NAME

qcovtools.pl - coverage-related modes.


=head1 SYNOPSIS

 qcovtools.pl command [options]


=head1 ABSTRACT

This script is a collection of snippets of perl code that do various
small but useful tasks.  In the past I would have made each of these as
a separate script but that way leads to madness as more and more scripts
get written and placed in more and more directories.  At least this way
there is only one script and POD block to grep when you are looking for
that routine you know you wrote but can't remember where it is.

=head1 COMMANDS

 props_2_gff        - create GFF3 from bioscope properties file
 illumina_panel_manifest - make GFF and BED files from Manifest files
 version            - print version number and exit immediately
 help               - display usage summary
 man                - display full man page

=head1 COMMAND DETAILS

=head2 props_2_gff

 -i | --infile        input properties file
 -o | --outfile       output GFF3 file
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

We need to be able to do "whole of genome" coverage calcs using
qcoverage so we need a GFF3 that lists all of the chromosomes in the
genome.  We could do this by hand or just generate it from the
properties files created by Bioscope.  This mode takes a bioscope
properties file as input and outputs a GFF3 ready for qcoverage.

=head3 Commandline options

=over

=item B<-i | --infile>

Bioscope properties file.  This file is used by bioscope to list the
names, lengths and byte offset in the FASTA file of the sequences that
appear in the FASTA file (in order) that is used to align against.
The file should look like this:

 #Created by ReferenceProperies
 #Sun Jul 04 08:25:38 EST 2010
 version=4.0.0
 reference.length=3101804739
 number.of.contigs=84
 c.1.H=chr1
 c.1.L=249250621
 c.1.P=0
 c.2.H=chr2
 c.2.L=243199373
 c.2.P=252811351
 c.3.H=chr3
 ...

=item B<-o | --outfile>

GFF3 file with one record for each sequence described in the input Bioscope properties file.
The file will look like this:


 ##gff-version   3
 #seqid  source  type    start   end score   strand  phase   attributes
 chr1    GRCh37_ICGC_standard_v2  chrom 1 249250621 . . OFFSET=0
 chr2    GRCh37_ICGC_standard_v2  chrom 1 243199373 . . OFFSET=252811351
 ...

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=back

=head2 illumina_panel_manifest

 -i | --input      Illumina manifest file
 -o | --outstem    Stem for names of 3 output files
 -l | --logfile    log file (optional)
 -v | --verbose    print progress and diagnostic messages

Takes the manifest file from an Illumina capture panel platform
(standard or custom) and creates 4 files - two GFF3 files for use with
qcoverage and two BED files for IGV use.  In both cases, one file is for
the Probes and one for the Targets.

The Illumina manifest file is somewhat like a multi-section INI file but
all of the lines (including those in the [Header] are tab-separated and
are right padded with the correct number of tabs:

 [Header]
 Customer Name       "ILLUMINA, INC."
 Product Type        15032433
 Date Manufactured   25/10/2012
 Lot                 35105
 DesignStudio ID     NA
 Target Plexity      212

 [Probes]
 Target Region Name   Target Region ID   Target ID   Species ...
 MPL1_2   MPL1_2.chr1.43815008.43815009 ...
 ...

 [Targets]
 TargetA   TargetB   Target Number   Chromosome   Start Position ...
 MPL1_2.chr1.43815008.43815009_tile_1 ...
 ...


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:grendeloz@gmail.com>

=back


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright 2012-2014  The University of Queensland
Copyright 2012-2014  John V Pearson
Copyright 2014-      QIMR Berghofer Medical Research Institute

All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.

=cut
