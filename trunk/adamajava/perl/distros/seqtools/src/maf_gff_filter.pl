#!/usr/bin/perl -w

##############################################################################
#
#  Program:  maf_gff_filter.pl
#  Author:   John V Pearson
#  Created:  2011-11-01
#
#  Look for and records from the MAF that sit within any of the regions
#  defined in the GFF file.
#
#  $Id: maf_gff_filter.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX qw( floor );
use Storable qw(dclone);
use Statistics::Descriptive;

use QCMG::FileDir::Finder;
use QCMG::IO::DccSnpReader;
use QCMG::IO::GffReader;
use QCMG::IO::MafReader;
use QCMG::IO::VcfReader;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: maf_gff_filter.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

# Setup global data structures (if any)

BEGIN {
}


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $gfffile    = ();
    my $maffile    = ();
    my $vcffile    = ();
    my $dccfile    = ();
    my $outfile    = '';
    my $logfile    = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'g|gfffile=s'          => \$gfffile,       # -g
           'a|maffile=s'          => \$maffile,       # -a
           'd|dccfile=s'          => \$dccfile,       # -d
           'c|vcffile=s'          => \$vcffile,       # -c
           'o|outfile=s'          => \$outfile,       # -o
           'l|logfile=s'          => \$logfile,       # -l
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );
    
    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    qlogfile($logfile) if $logfile;
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");
    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    die "No GFF file specified" unless $gfffile;
    my $rh_regions = read_gff_file( $gfffile );

    # Now we need to decide which of the 3 modes has been invoked

    my $ra_variants = [];
    if ($maffile) {
       # Read in MAF records, filtering as we go   
       $ra_variants = read_maf_file( $maffile, $rh_regions );
       write_report( $outfile, $ra_variants );
    }
    elsif ($dccfile) {
       # Read in DCC records, filtering as we go   
       $ra_variants = read_dcc_file( $dccfile, $rh_regions );
       write_report( $outfile, $ra_variants );
    }
    elsif ($vcffile) {
       # Read in VCF records, filtering as we go   
       #my $ra_variants = read_vcf_file( $maffile, $rh_regions );
    }
    else {
        die "You must specify either a MAF (-a) or VCF (-c) file\n";
    }
    print $_->to_text,"\n" foreach @{$ra_variants};

    qlogend;
}


sub read_gff_file {
    my $gfffile = shift;

    my %regions = ();
    my $gff = QCMG::IO::GffReader->new( filename => $gfffile );
    while (my $region = $gff->next_record()) {
       # Store region details in hash keyed on sequence
       push @{ $regions{ $region->seqname } },
            [ $region->start, $region->end ];
    }

    # Sort regions by start position within each sequence
    foreach my $seq (keys %regions) {
        my @sorted_regions = map  { $_->[1] }
                             sort { $a->[0] <=> $b->[0] }
                             map  { [$_->[0], $_ ] }
                             @{ $regions{$seq} };
        $regions{ $seq } = \@sorted_regions;
    }

    return \%regions;
}


sub read_maf_file {
    my $maffile    = shift;
    my $rh_regions = shift;

    my @keepers = ();
    my $maf = QCMG::IO::MafReader->new( filename => $maffile );
    while (my $mrec = $maf->next_record()) {
        my $seq   = 'chr' . $mrec->Chromosome;
        my $start = $mrec->Start_Position;
        my $end   = $mrec->End_Position;
        #print join("\t", "testing:", $seq, $start, $end,
        #                 $mrec->Hugo_Symbol ),"\n";
        foreach my $region (@{ $rh_regions->{ $seq } }) {
            # If end of region is before variant then keep looking
            next if $region->[1] < $start;
            # We're done once we get past any possibility of matching
            last if $region->[0] > $end;
            if ($start >= $region->[0] and
                $end   <= $region->[1]) {
                push @keepers, $mrec;
            }
        }
    }
    
    # Sort the keepers by sequence and by position within the sequence
    my %keepers = ();
    my @sorted_keepers = ();
    foreach my $rec (@keepers) {
       push @{ $keepers{ $rec->Chromosome } }, $rec;
    }
    foreach my $seq (sort keys %keepers) {
        my @sorted_recs = map  { $_->[1] }
                          sort { $a->[0] <=> $b->[0] }
                          map  { [$_->Start_Position, $_ ] }
                          @{ $keepers{$seq} };
        push @sorted_keepers, @sorted_recs;
    }

    return \@sorted_keepers;
}


sub read_dcc_file {
    my $dccfile    = shift;
    my $rh_regions = shift;

    my @keepers = ();
    my $dcc = QCMG::IO::DccSnpReader->new( filename => $dccfile );
    while (my $drec = $dcc->next_record()) {
        my $seq   = 'chr' . $drec->chromosome;
        my $start = $drec->chromosome_start;
        my $end   = $drec->chromosome_end;
        #print join("\t", "testing:", $seq, $start, $end,
        #                 $drec->mutation_id ),"\n";
        foreach my $region (@{ $rh_regions->{ $seq } }) {
            # If end of region is before variant then keep looking
            next if $region->[1] < $start;
            # We're done once we get past any possibility of matching
            last if $region->[0] > $end;
            if ($start >= $region->[0] and
                $end   <= $region->[1]) {
                push @keepers, $drec;
            }
        }
    }

    # Sort the keepers by sequence and by position within the sequence
    my %keepers = ();
    my @sorted_keepers = ();
    foreach my $rec (@keepers) {
       push @{ $keepers{ $rec->chromosome } }, $rec;
    }
    foreach my $seq (sort {$a <=> $b} keys %keepers) {
        my @sorted_recs = map  { $_->[1] }
                          sort { $a->[0] <=> $b->[0] }
                          map  { [$_->chromosome_start, $_ ] }
                          @{ $keepers{$seq} };
        push @sorted_keepers, @sorted_recs;
        qlogprint($_->chromosome,' ',$_->chromosome_start,"\n") foreach @sorted_recs;
    }

    return \@sorted_keepers;
}


sub write_report {
    my $outfile    = shift;
    my $ra_records = shift;

    # Get the detailed output file ready
    my $outfh = IO::File->new( $file, 'w' );
    croak "Can't open output file $file for writing: $!"
        unless defined $outfh;

    foreach my $rec (@{ $ra_records }) {
       print $outfh $rec->to_text, "\n";
    }

    $outfh->close;
}



__END__

=head1 NAME

maf_gff_filter.pl - Extract variant fromMAF based on position


=head1 SYNOPSIS

 maf_gff_filter.pl -g gfffile -a maffile -o outfile [options]


=head1 ABSTRACT

This script takes a MAF (or VCF) file and selects just those variants
that lie within a set of regions defined in a GFF file.  Ths first use
for this script is to process the MAF for APGI_1992 to extract those
variants that are within regions targetted by the TargetSeq29 or
custom AmpliSeq enrichments that were run on the Ion Torrent.


=head1 OPTIONS

 -g | --gfffile       GFF file containing regions
 -a | --maffile       MAF file containing variants
 -c | --vcffile       VCF allele frequency file
 -o | --outfile       MAF output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

=head2 Commandline options

=over 2

=item B<-g | --gfffile>

GFF file contaning regions to be used as the selection criteria for the
MAF/VCF records.

=item B<-a | --maffile>

MAF file containing variants.

=item B<-c | --vcffile>

VCF file created by qSNP or other variant caller.

=item B<-o | --outfile>

MAF output file.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<--version>

Print the script version and exit immediately.

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

$Id: maf_gff_filter.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011,2012

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
