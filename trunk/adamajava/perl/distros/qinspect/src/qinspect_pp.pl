#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qinspect_pp.pl
#  Author:   John V Pearson
#  Created:  2012-06-14
#
#  This script is a "pre-processor" for qinspect.pl - it creates the SAM-like
#  "qi" files that qinspect.pl visualises.
#
#  $Id: qinspect_pp.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::IO::QiCoordsReader;
use QCMG::IO::MafReader;
use QCMG::IO::MafWriter;
use QCMG::IO::MafRecordCollection;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $CMDLINE $VERSION $VERBOSE );


( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qinspect_pp.pl 4669 2014-07-24 10:48:22Z j.pearson $'
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
    
    my @valid_modes = qw( help man version 
                          qcmgmaf coordlist );

    if ($mode =~ /^help$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );
    }
    elsif ($mode =~ /^man$/i) {
        pod2usage(-exitstatus => 0, -verbose => 2)
    }
    elsif ($mode =~ /^version$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^qcmgmaf/i) {
        qcmgmaf();
    }
    elsif ($mode =~ /^coordlist/i) {
        coordlist();
    }
    else {
        die "qinspect_pp [$mode] is unrecognised; valid modes are: ".
            join(' ',@valid_modes) ."\n";
    }
}



sub coordlist {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/COORDLIST' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => 'qi.txt',
                   surrounds   => 100,
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           's|surrounds=i'        => \$params{surrounds},     # -s
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply at least one infile
    die "You must specify an input file\n" unless scalar(@{$params{infiles}});

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    # Read Input files
    my $ctr = 0;
    foreach my $file (@{$params{infiles}}) {
         my $qic = QCMG::IO::QiCoordsReader->new( filename => $file );

         my $id = $qic->header('ID');

         foreach my $coord ($qic->coords) {
             $ctr++;
             my $range = $coord->{chrom} .':'.
                         ($coord->{start} - $params{surrounds}) .'-'.
                         ($coord->{end}   + $params{surrounds});
             my $vloc  = $coord->{start} .'-'. $coord->{end};

             foreach my $bam (@{$qic->header('BAMs')}) {
                  my $bam_name = $bam->{name};
                  my $cmd  = "samtools view $bam_name $range";
                  warn $cmd;
                  my $recs = `$cmd`;
                  if ($recs) {
                      $outfh->print( "#QI SOURCE = $id\n",
                                     "#QI BAM = $bam_name\n",
                                     "#QI RANGE = $range,$vloc\n",
                                     $recs ); 
                  }
             }
         }
    }

    $outfh->close;

    qlogend;
}


sub qcmgmaf {
   
    # Print help message if no CLI params
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QCMGMAF' )
        unless (scalar @ARGV > 0);

    # Setup defaults for CLI params
    my %params = ( infiles     => [],
                   outfile     => 'qi.txt',
                   surrounds   => 100,
                   project     => 'icgc_pancreatic',
                   bamtype     => 'exome',
                   logfile     => '',
                   verbose     => 0 );

    my $results = GetOptions (
           'i|infile=s'           =>  $params{infiles},       # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           's|surrounds=i'        => \$params{surrounds},     # -s
           'p|project=s'          => \$params{project},       # -t
           'b|bamtype=s'          => \$params{bamtype},       # -b
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply infile
    die "You must specify an input file\n" unless scalar(@{$params{infiles}});

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n" );

    my $pattern = '';
    my $patstr  = '';
    if ($params{project} =~ /icgc_pancreatic/) {
        $pattern = qr/APGI[_-]\d{4}/i;
        $patstr  = 'APGI[_-]\d{4}';
    }
    elsif ($params{project} =~ /icgc_ovarian/) {
        $pattern = qr/AOCS[_-]\d{4}/i;
        $patstr  = 'AOCS[_-]\d{4}';
    }
    else {
        die 'Invalid --project value [',$params{project},"]\n";
    }

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    die "unable to open file $params{outfile} for writing: $!" unless
        defined $outfh;

    # Read MAF file(s) and filter by pattern
    my $mfc = parse_mafs_new( @{$params{infiles}} );
    my $ctr     = 0;
    my $matches = 0;
    my $id      = '';
    my %t = ();
    my %g = ();
    foreach my $rec ($mfc->records()) {
         $id = '';
         $ctr++;
         next unless $rec->Hugo_Symbol;
         if ($rec->Tumor_Sample_Barcode =~ /($pattern)/) {
             $id = $1;
             $matches++;
             $t{$id}++;
             $g{$rec->Hugo_Symbol}++;
         }
         else {
             next;
         }

         my $range = 'chr' . $rec->Chromosome .':'.
                     ($rec->Start_Position - $params{surrounds}) .'-'.
                     ($rec->End_Position + $params{surrounds});
         my $vloc  = $rec->Start_Position .'-'. $rec->End_Position;
         my $stem = '/mnt/seq_results/'. $params{project} .
                    "/$id/seq_final/$id" .'.'. $params{bamtype};
         my $tumour_bam = $stem . '.TD.bam';
         my $normal_bam = $stem . '.ND.bam';

         my $cmd1  = "samtools view $tumour_bam $range";
         warn $cmd1;
         my $recs1 = `$cmd1`;
         if ($recs1) {
             $outfh->print( "#QI SOURCE = $id\n",
                            "#QI BAM = $tumour_bam\n",
                            "#QI GENE = ", $rec->Hugo_Symbol, "\n",
                            "#QI RANGE = $range,$vloc\n",
                            $recs1 ); 
         }

         my $cmd2  = "samtools view $normal_bam $range";
         warn $cmd2;
         my $recs2 = `$cmd2`;
         if ($recs2) {
         $outfh->print( "#QI SOURCE = $id\n",
                        "#QI BAM = $normal_bam\n",
                        "#QI GENE = ", $rec->Hugo_Symbol, "\n",
                        "#QI RANGE = $range,$vloc\n",
                        $recs2 ); 
         }
    }
    #print Dumper \%t, \%g, $ctr, $matches;

    $outfh->close;

    qlogend;
}


sub parse_mafs_new {
    my @files = @_;

    my $mfc = QCMG::IO::MafRecordCollection->new( verbose  => $VERBOSE );
    foreach my $file (@files) {
        $mfc->add_records( [ parse_maf_new( $file )->records ] );
    }

    return $mfc;
}


sub parse_maf_new {
    my $file = shift;

    my @mafs = ();
    my $mr = QCMG::IO::MafReader->new( filename => $file,
                                       verbose  => $VERBOSE );
    my $mc = QCMG::IO::MafRecordCollection->new( verbose  => $VERBOSE );
    while (my $rec = $mr->next_record) {
        $mc->add_record( $rec );
    }
    qlogprint 'read '. $mc->record_count . " MAF records from $file\n";

    return $mc;
}


__END__

=head1 NAME

qinspect_pp.pl - Pre-processor for qinspect.pl


=head1 SYNOPSIS

 qinspect_pp.pl command [options]


=head1 ABSTRACT

This script creates the text "qi" files visualised into PDF by
qinspect.pl.  There are a number of different modes depending on the
input file formats.


=head1 COMMANDS

 qcmgmaf        - MAF file containing QCMG IDs
 coordlist      - per-patient file containing coordinates of variants
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 QCMGMAF

 -i | --infile        MAF input files
 -o | --outfile       QI text file
 -s | --surrounds     number of bases to include either side of variant
 -p | --project       either icgc_pancreatic or icgc_ovarian
 -b | --bamtype       either exome or genome
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode is designed to be run on QCMG's cluster against MAF files
created for QCMG patients.  The script will pull the donor IDs, variant
positions and gene names out of the MAF file and then it will use that
information plus the --project and --bamtype values to "guess" the
relevant BAM names and go pull out the SAM records from the normal (TD)
and tumour (TD) BAMs.  If the MAF contains records for non-QCMG patients
or you just want the records for a subset of patients, just cut down the
MAF file yourself.  Note that you MUST have the MAF file header intact
or this routine will not run.  The header is 2 lines - a version line 
plus a line of column headers:

 #version 2.2
 Hugo_Symbol     Entrez_Gene_Id  Center  NCBI_Build  ...

=head3 Example 

 qinspect_pp.pl qcmgmaf -s 100 -p icgc_pancreatic -b exome \
                        -i QCMG_20111026.maf \
                        -o QCMG_APGI_MAF.qi.txt \
                        -l QCMG_APGI_MAF.qi.log

=head2 COORDLIST

 -i | --infile        Coordinate list files
 -o | --outfile       QI text file
 -s | --surrounds     number of bases to include either side of variant
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages

This mode is designed for use outside QCMG.  It expects to be fed
plain-text space or tab-sepaarted files where the first column contains
variant coordinates in the "chrom:start-end" format.  The file must also
contain 3 special "QIPP" header that identify the donor and the BAMs
that are to be processed.  An example file might look like:

 #QIPP ID=donor1
 #QIPP BAMTUMOUR=/home/jpearson/bams/donor1.hiseq_genome_normal.bam
 #QIPP BAMNORMAL=/home/jpearson/bams/donor1.hiseq_genome_tumour.bam
 chr1:144873963-144873963    line17103   frameshiftdeletion
 chr1:144917828-144917828    line17117   frameshiftdeletion
 chr1:153005007-153005007    line18154   frameshiftinsertion
 chr1:240255569-240255571    line31430   nonframeshiftdeletion

The headers that identify the BAM file are of the general form "QIPP BAM<anything>="
wherw the "<anything>" can be an empty string (BAM=) or a number (BAM1=)
or a string (BAMTUMOUR=).

=head3 Example 

 qinspect_pp.pl coordlist -s 100 \
                        -i APGI_1.txt \
                        -i APGI_2.txt \
                        -i APGI_3.txt \
                        -o my_apgi_variants.qi.txt \
                        -l my_apgi_variants.qi.log


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: qinspect_pp.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012
Copyright (c) John Pearson 2012

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
