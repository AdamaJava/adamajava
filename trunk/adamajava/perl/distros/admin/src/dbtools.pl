#!/usr/bin/perl -w

##############################################################################
#
#  Program:  dbtools.pl
#  Author:   John V Pearson
#  Created:  2013-10-16
#
#  Tasks associated with databases.
#
#  $Id: dbtools.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX;

use QCMG::FileDir::Finder;
use QCMG::FileDir::QLogFile;
use QCMG::FileDir::QSnpDirParser;
use QCMG::FileDir::GatkDirParser;
use QCMG::IO::EnsemblDomainsReader;
use QCMG::IO::FastaReader;
use QCMG::IO::SamHeader;
use QCMG::IO::SamReader;
use QCMG::PDF::Document;
use QCMG::QInspect::Sam2Pdf;
use QCMG::Util::FileManipulator;
use QCMG::Util::QLog;
use QCMG::Verify::AutoNames;
use QCMG::QBamMaker::SeqFinalDirectory;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: dbtools.pl 4667 2014-07-24 10:09:43Z j.pearson $'
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

    my @valid_modes = qw( help man version qsnpdir finalbampairs
                          aligner_from_mapset_bam );

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
        qsnpdir();
    }
    elsif ($mode =~ /^$valid_modes[4]/i) {
        finalbampairs();
    }
    elsif ($mode =~ /^$valid_modes[5]/i) {
        aligner_from_mapset_bam()
    }
    else {
        die "dbtools mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub qsnpdir {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QSNPDIR' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dirs     => [],
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              =>  $params{dirs},          # -d
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply an infile or directory
    die "You must specify a directory (-d)\n"
        unless ( scalar( @{ $params{dirs} } ) );
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params );

    warn "No output file specified\n" unless $params{outfile};

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;

    my $fact = QCMG::FileDir::QSnpDirParser->new( verbose => 0 );
    
    # Find all variants/qSNP directories
    my $header_written = 0;
    foreach my $dir (@{ $params{dirs} }) {
        my $ra_qsnps = $fact->parse( $dir );
        foreach my $qsnp (@{ $ra_qsnps }) {
            if (! $header_written) {
                $outfh->print( '#', $qsnp->completion_report_header );
                $header_written = 1;
            }
            $outfh->print( $qsnp->completion_report_text );
        }
    }

    qlogend();
}


sub finalbampairs {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/FINALBAMPAIRS' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( dirs     => [],
                   aligner  => '',
                   outfile  => '',
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'd|dir=s'              =>  $params{dirs},          # -d
           'a|aligner=s'          => \$params{aligner},       # -a
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # It is mandatory to supply a directory and output file
    die "You must specify a directory (-d)\n" unless scalar( @{ $params{dirs} } );
    die "You must specify an output file (-o)\n" unless $params{outfile};

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    qlogparams( \%params );

    warn "No output file specified\n" unless $params{outfile};

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;
    $outfh->print( join("\t", qw( parent_project project ctr bam1 bam2 )), "\n" );

    my $fact = QCMG::QBamMaker::SeqFinalDirectory->new( verbose => $params{verbose} );

    foreach my $dir (@{ $params{dirs} }) {

        # Create QCMG::QBamMaker::SeqFinalBamCollection
        my $coll = $fact->process_directory( $dir );
        my $rh_collections = $coll->new_collections_sorted_by_project_and_donor;

        foreach my $project (sort keys %{ $rh_collections }) {
            foreach my $donor (sort keys %{ $rh_collections->{$project} }) {
                my $this_coll = $rh_collections->{$project}->{$donor};
                qlogprint join( "\t", $project, $donor, 
                                  $this_coll->record_count ), "\n";
                $outfh->print( '[',$project,':',$donor,"]\n" ); 

                # Look for Bams paired for variant calling.  Data 
                # structure is pairs of QCMG::QBamMaker::SeqFinalBam objects
                my $ra_bam_pairs = $this_coll->bams_paired_for_variant_calling;
                my $ctr = 1;
                foreach my $ra_bam_pair (@{ $this_coll->bams_paired_for_variant_calling }) {
                    if ($params{aligner}) {
                       next unless ($ra_bam_pair->[0]->aligner eq $params{aligner});
                    }
                    $outfh->print( join("\t", $project,
                                              $donor,
                                              $ctr++,
                                              $ra_bam_pair->[0]->BamName,
                                              $ra_bam_pair->[1]->BamName), "\n" );
                }
            }
        }

    }

    qlogend();
}


sub testqlogfile {
    qlogbegin();

    my $dir = '/mnt/seq_results/icgc_pancreatic/APGI_1992/variants/qSNP/6bcf8d4d_0394_47df_b9ac_4e488b542e49';

    my $file = QCMG::FileDir::QLogFile->new( file    => "$dir/qsnp.log",
                                             verbose => 0 );


    #print Dumper $file->lines_by_loglevel( 'EXEC' );
    foreach my $rh_line (@{ $file->lines_by_loglevel( 'EXEC' ) }) {
        print '   ',
              join( ' ', $rh_line->{'timestamp'},
                         $rh_line->{'thread'},
                         $rh_line->{'loglevel'},
                         $rh_line->{'class'},
                         $rh_line->{'message'} ),
              "\n";
    }

    print Dumper $file->{unparsed};
    print Dumper $file->attributes_from_exec_lines;

    qlogend();
}


sub autonames {
    qlogbegin();

    my $dir = '/mnt/seq_results/icgc_pancreatic/APGI_1992/variants/';
    my $find = QCMG::FileDir::Finder->new( verbose => 0 );
    my $verf = QCMG::Verify::AutoNames->new( verbose => 0 );

    my @mafs = $find->find_file( $dir, '\.Somatic\.HighConfidence\.snv\.maf$' );

    print Dumper \@mafs;

    qlogprint 'Mafs found ',scalar(@mafs),"\n";

    foreach my $maf (@mafs) {
        print Dumper $maf, $verf->params_from_maf($maf);
    }

    qlogend();
}



sub aligner_from_mapset_bam {
    qlogbegin();

    #my $dir = '/mnt/seq_results/smgres_oesophageal/OESO_0384';
    #my $dir = '/mnt/seq_results/smgres_oesophageal/';
    my $dir = '/mnt/seq_results/';
    my %tally = ();

    my $find = QCMG::FileDir::Finder->new( verbose => 0 );

    my @bam_files = $find->find_file( $dir, '\.bam$' );

    foreach my $bam_file (@bam_files) {
        # We only want mapset-level BAMs
        next unless ($bam_file =~ /\/seq_mapped\//);

        my $bam = QCMG::IO::SamReader->new( filename => $bam_file );
        my $head = QCMG::IO::SamHeader->new( header => $bam->headers_text );

        my $bwa_found = 0;

        my @pgs = @{ $head->PG };
        foreach my $pg (@pgs) {
            my @fields = split /\t/, $pg;
            if ($pg =~ /:bwa/) {
                $bwa_found = 1;
                if ($pg =~ /(^.*\sCL:.{10})/) {
                    $pg = $1;
                    push @{ $tally{ $pg } }, $bam_file;
                }
                else {
                    push @{ $tally{ $pg } }, $bam_file;
                }
            }
        }

        # Catch any mapsets where we couldn't get the aligner.
        if (! $bwa_found) {
            push @{ $tally{ 'unknown' } }, $bam_file;
        }
    }
    
    #print Dumper \%tally;
    foreach my $aligner (sort keys %tally) {
        my @found_bams = @{ $tally{ $aligner } };
        qlogprint( scalar(@found_bams), " - $aligner\n" );
        print "[$aligner]\n";
        print "\t$_\n" foreach @found_bams;
    }

    qlogend();
}



sub gatk_variantdir {
    qlogbegin();

    # /mnt/seq_results/icgc_pancreatic/APGI_2185/variants/GATK/d3cba824_6b2f_41e9_bf8b_95027315f298
    my $dir = '/mnt/seq_results/icgc_pancreatic/APGI_2185';
    #my $dir = '/mnt/seq_results/icgc_pancreatic/APGI_3205';
    #my $dir = '/mnt/seq_results/icgc_pancreatic';
    my $fact = QCMG::FileDir::GatkDirParser->new( verbose => 0 );
    my $ra_dirs = $fact->parse( $dir );

    $fact->completion_report( 'icgc_pancreatic_gatk_20130801.txt' );

    #print Dumper $gatk;

    foreach my $dir (@{ $ra_dirs }) {
        print Dumper $dir->maf_details;
    }

    qlogend();
}


__END__

=head1 NAME

dbtools.pl - tool to create tables for QCMGschema


=head1 SYNOPSIS

 dbtools.pl command [options]


=head1 ABSTRACT

This script is a collection of modes that write text file reports
suitable for importing into QCMGschema.


=head1 COMMANDS

 qsnpdir        - progress of qSNP calling
 finalbampairs  - list of callable pairs of BAMs
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 QSNPDIR

Process calling directories for qSNP

 -d | --dir           root directory under which to search
 -o | --outfile       output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

This mode starts at the specified directory and recursively parses every 
subdirectory looking for directories called 'qSNP' assuming that for
each donor, qSNP calls are all in directories under 'variants/qSNP'.
All direct
subdirectories of qSNP are parsed as though they were qSNP call
directories and a line is added to the report for each one.

Example 1.  Report on all qSNP calls in icgc_pancreatic:

 dbtools qsnpdir -d /mnt/seq_results/icgc_pancreatic \
                 -o qsnp_completion_report.txt -v

=head2 FINALBAMPAIRS

Identify callable pairs of BAMs

 -d | --dir           root directory under which to search
 -o | --outfile       output file
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages

This mode starts at the specified directory and recursively parses every 
subdirectory looking for seq_final BAMs.  It sorts the BAMs by
parent_project and project and tries to identify "callable pairs" of
BAMs.


=head1 DESCRIPTION


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: dbtools.pl 4667 2014-07-24 10:09:43Z j.pearson $


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
