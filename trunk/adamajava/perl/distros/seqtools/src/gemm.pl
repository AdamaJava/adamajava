#!/usr/bin/perl -w

##############################################################################
#
#  Program:  gemm.pl
#  Author:   John V Pearson
#  Created:  2012-10-23
#
#  Tasks associated with running and post-processing data from the
#  GEMM project.
#
#  $Id: gemm.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use XML::LibXML;

use QCMG::FileDir::Finder;
use QCMG::IO::DccSnpReader;
use QCMG::Run::Annovar;
use QCMG::Util::QLog;
use QCMG::Util::FileManipulator;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: gemm.pl 4669 2014-07-24 10:48:22Z j.pearson $'
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

    my @valid_modes = qw( help version qsnp smallindeltool );

    if ($mode =~ /^$valid_modes[0]$/i or $mode =~ /\?/) {
        pod2usage( -exitval  => 0,
                   -verbose  => 99,
                   -sections => 'SYNOPSIS|COMMANDS' );

    }
    elsif ($mode =~ /^$valid_modes[1]$/i) {
        print "$SVNID\n";
    }
    elsif ($mode =~ /^$valid_modes[2]/i) {
        qsnp();
    }
    elsif ($mode =~ /^$valid_modes[3]/i) {
        smallindeltool();
    }
    else {
        die "gemm mode [$mode] is unrecognised; valid modes are: " .
            join(' ',@valid_modes) ."\n";
    }
}


sub qsnp {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/QSNP' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile    => '',
                   outfile   => '',
                   logfile   => '',
                   mutreads  => 5,
                   novstarts => 4,
                   verbose   => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'r|mutreads=s'         => \$params{mutreads},      # -r
           'n|novstarts=s'        => \$params{novstarts},     # -n
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # It is mandatory to supply an infile
    die "You must specify an input file (-i)\n" unless $params{infile};

    my $dcc = QCMG::IO::DccSnpReader->new(
                    filename => $params{infile},
                    version  => 'dcc1a',
                    verbose  => $params{verbose} );

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;

    # There is no point putting headers on the output file because
    # annovar can't cope with them BUT we WILL want to put headers back
    # on at a later stage so let's REMEMBER the headers including the
    # 5 annovar-specific columns we are going to prepend to each record.

    my @headers = ( qw(annovar_chrom
                       annovar_start
                       annovar_end
                       annovar_ref_allele
                       annovar_alt_allele ), $dcc->headers);

    # Read and filter records from input and write to output

    my %rec_ctrs = ();
    while (my $rec = $dcc->next_record) {

        if ($rec->QCMGflag ne '--') {
            $rec_ctrs{'Failed QCMGflag'}++;
            next;
        }
        if ($rec->NNS < $params{novstarts}) {
            $rec_ctrs{'Failed novel starts'}++;
            next;
        }
        if (fails_mutreads_check( $rec->mutation, $rec->TD, $params{mutreads} )) {
            $rec_ctrs{'Failed mutant reads'}++;
            next;
        }

        # Warn if the control genotype is not 2 copies of the reference
        # genome allele:
        check_control_alleles( $rec->mutation_id,
                               $rec->control_genotype,
                               $rec->reference_genome_allele );

        # If we got this far then the record's a keeper so we write it
        # out. This is also the appropriate time to create the annoying
        # but mandatory 5 fields that annovar needs to see as the first
        # 5 fields in the annotation input file.

        $rec_ctrs{'Passed'}++;
        my ($ref_allele, $alt_allele) = parse_mutation( $rec->mutation );
        $outfh->print( join("\t", $rec->chromosome,
                                  $rec->chromosome_start,
                                  $rec->chromosome_end,
                                  $ref_allele,
                                  $alt_allele,
                                  $rec->to_text ),"\n" );
    }
    $outfh->close;

    # Log where all the records went

    qlogprint( 'Records parsed : ', $dcc->record_ctr, "\n" );
    if ($params{verbose}) {
        qlogprint( "Records breakdown :\n" );
        foreach my $type (sort keys %rec_ctrs) {
            qlogprint( "  $type : ", $rec_ctrs{$type}, "\n" );
        }

    }

    # We will annotate with both ensembl and refgenes so this will take
    # 2 calls to annotate_variation with some column shifting in between

    my $fm  = QCMG::Util::FileManipulator->new( verbose => $params{verbose} );
    my $anv = QCMG::Run::Annovar->new( verbose => $params{verbose} );
    my $exonfile = $params{outfile} .'.exonic_variant_function';
    #my $exonfile = $params{outfile} .'.variant_function';

    # Annotate with refgene
    $anv->annotate_variation( infile   => $params{outfile},
                              mode     => 'refgene',
                              buildver => 'mm9' );

    # Move 3 refgene columns to end of file (.exonic_variant_function)
    # Move 2 refgene columns to end of file (.variant_function)
    $fm->move_columns_from_front_to_back( infile  => $exonfile,
                                          outfile => $exonfile .'.tmp1',
                                          count   => 3 );

    rename( $exonfile .'.tmp1', $params{outfile} );
    
    # Annotated with ensgene
    $anv->annotate_variation( infile   => $params{outfile},
                              mode     => 'ensgene',
                              buildver => 'mm9' );

    # Move 3 ensgene columns to end of file
    $fm->move_columns_from_front_to_back( infile  => $exonfile,
                                          outfile => $exonfile .'.tmp1',
                                          count   => 3 );

    # Now we want to put headers back on the file so this will require
    # the header list we saved away earlier PLUS the extra columns
    # that the refgene and ensgene annovar runs have added.

    my $header_line = join("\t", @headers,
                                 qw( anv_refgene_lineno
                                     anv_refgene_mutntype
                                     anv_refgene_consequences ),
                                 qw( anv_ensgene_lineno
                                     anv_ensgene_mutntype
                                     anv_ensgene_consequences ) ) .
                      "\n";

    $fm->add_lines_at_top( infile  => $exonfile .'.tmp1',
                           outfile => $exonfile .'.tmp2',
                           lines   => [ $header_line ] );

    # Ditch the annoying annovar columns off the front
    $fm->drop_columns( infile  => $exonfile .'.tmp2',
                       outfile => $exonfile .'.tmp3',
                       columns => [ 0,1,2,3,4 ] );

    # Rename final output back to originally requested output filename
    rename( $exonfile .'.tmp3', $params{outfile} );

    qlogend();
}


sub smallindeltool {
    my $class = shift;

    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'COMMAND DETAILS/SMALLINDELTOOL' )
        unless (scalar @ARGV > 0);

    # Setup defaults for important variables.

    my %params = ( infile    => '',
                   outfile   => '',
                   logfile   => '',
                   mutreads  => 5,
                   novstarts => 4,
                   verbose   => 0 );

    my $results = GetOptions (
           'i|infile=s'           => \$params{infile},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           'l|logfile=s'          => \$params{logfile},       # -l
           'r|mutreads=s'         => \$params{mutreads},      # -r
           'n|novstarts=s'        => \$params{novstarts},     # -n
           'v|verbose+'           => \$params{verbose},       # -v
           );

    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");

    # It is mandatory to supply an infile
    die "You must specify an input file (-i)\n" unless $params{infile};

    my $dcc = QCMG::IO::DccSnpReader->new(
                    filename => $params{infile},
                    version  => 'dcc1b',
                    verbose  => $params{verbose} );

    my $outfh = IO::File->new( $params{outfile}, 'w' );
    croak 'Unable to open ', $params{outfile}, " for writing: $!"
        unless defined $outfh;

    # There is no point putting headers on the output file because
    # annovar can't cope with them BUT we will want to put headers back
    # on at a later stage so we will REMEMBER the headers.
    my @headers = ( qw(annovar_chrom
                       annovar_start
                       annovar_end
                       annovar_ref_allele
                       annovar_alt_allele ), $dcc->headers);

    # Read and filter all records from input
    my %rec_ctrs = ();
    while (my $rec = $dcc->next_record) {

        if ($rec->QCMGflag ne '--') {
            $rec_ctrs{'Failed QCMGflag'}++;
            next;
        }

        # Warn if the control genotype is not 2 copies of the reference
        # genome allele:
        check_control_alleles( $rec->mutation_id,
                               $rec->control_genotype,
                               $rec->reference_genome_allele );

        # For the dcc1b format file output by the Bioscope small indel
        # tools, we will need to filter on "tumourcount" which is a composite
        # field of the form mutreads[novstarts]/totreads[mutread%]

        my ($mutreads,
            $novstarts,
            $total_reads,
            $mutread_percent) = parse_indel_counts( $rec->tumourcount );
        my ($ref_allele, $alt_allele) = parse_mutation( $rec->mutation );

        if ($novstarts < $params{novstarts}) {
            $rec_ctrs{'Failed novel starts'}++;
            next;
        }
        if ($mutreads < $params{mutreads}) {
            $rec_ctrs{'Failed mutant reads'}++;
            next;
        }

#        print join( "\t", $rec->analysis_id,
#                          $rec->tumour_sample_id,
#                          $rec->mutation,
#                          $rec->QCMGflag,
#                          $rec->NNS,
#                          $rec->ND,
#                          $rec->TD ), "\n";

        # If we got this far then we are a keeper so we get written out.
        # This is also the appropriate time to create the annoying but
        # mandatory 5 fields that annovar needs to see as the first 5
        # fields in the annotation input file.

        $rec_ctrs{'Passed'}++;
        $outfh->print( join("\t", $rec->chromosome,
                                  $rec->chromosome_start,
                                  $rec->chromosome_end,
                                  $rec->reference_genome_allele,
                                  $alt_allele,
                                  $rec->to_text ),"\n" );
    }
    $outfh->close;

    qlogprint( 'Records parsed : ', $dcc->record_ctr, "\n" );
    if ($params{verbose}) {
        qlogprint( "Records breakdown :\n" );
        foreach my $type (sort keys %rec_ctrs) {
            qlogprint( "  $type : ", $rec_ctrs{$type}, "\n" );
        }

    }

    # We will annotate with both ensembl and refgenes so this will take
    # 2 calls to annotate_variation with some column shifting in between

    my $fm  = QCMG::Util::FileManipulator->new( verbose => $params{verbose} );
    my $anv = QCMG::Run::Annovar->new( verbose => $params{verbose} );
    my $exonfile = $params{outfile} .'.exonic_variant_function';

    # Annotate with refgene
    $anv->annotate_variation( infile   => $params{outfile},
                              mode     => 'refgene',
                              buildver => 'mm9' );

    # Move 3 refgene columns to end of file
    $fm->move_columns_from_front_to_back( infile  => $exonfile,
                                          outfile => $exonfile .'.tmp1',
                                          count   => 3 );

    rename( $exonfile .'.tmp1', $params{outfile}.'.b' );
    $exonfile = $params{outfile} .'.b.exonic_variant_function';
    
    # Annotated with ensgene
    $anv->annotate_variation( infile   => $params{outfile} .'.b',
                              mode     => 'ensgene',
                              buildver => 'mm9' );

    # Move 3 ensgene columns to end of file
    $fm->move_columns_from_front_to_back( infile  => $exonfile,
                                          outfile => $exonfile .'.tmp1',
                                          count   => 3 );

    # Now we want to put headers back on the file so this will require
    # the header list we saved away earlier PLUS the extra columns
    # that the refgene and ensgene annovar runs have added.

    my $header_line = join("\t", @headers,
                                 qw( anv_refgene_lineno
                                     anv_refgene_mutntype
                                     anv_refgene_consequences ),
                                 qw( anv_ensgene_lineno
                                     anv_ensgene_mutntype
                                     anv_ensgene_consequences ) ) .
                      "\n";

    $fm->add_lines_at_top( infile  => $exonfile .'.tmp1',
                           outfile => $exonfile .'.tmp2',
                           lines   => [ $header_line ] );

    # Ditch the annoying annovar columns off the front
    $fm->drop_columns( infile  => $exonfile .'.tmp2',
                       outfile => $exonfile .'.tmp3',
                       columns => [ 0,1,2,3,4 ] );

    # Rename final output back to originally requested output filename
    rename( $exonfile .'.tmp3', $params{outfile} );

    qlogend();
    # end of &smallindeltool
} 


sub check_control_alleles {
    my $mutation_id      = shift;
    my $control_genotype = shift;
    my $reference_allele = shift;

    my ($allele1, $allele2) = parse_alleles( $control_genotype );
    if ($allele1 ne $reference_allele or
        $allele2 ne $reference_allele) {
        warn "$mutation_id : control genotype [$control_genotype]",
             " does not match reference allele [$reference_allele]\n";
    }
}


sub parse_indel_counts {
    my $counts = shift;

    # 2012-10-26
    # My understanding of the (undocumented) count fields ar based on my
    # reading of A-M's perl script
    # $SVN/QCMGScripts/a.patch/smallindel/parseCheckPileup.pl
    # From this it would *appear* that a field like:
    #   36[13]/48[0.75]
    # means:
    #   36 - reads showing muntant allele
    #   13 - novel starts mutant reads
    #   48 - total reads
    #   0.75 - percent mutant reads (i.e. 36/48)

    my $mutreads        = 0;
    my $novstarts       = 0;
    my $total_reads     = 0;
    my $mutread_percent = 0;

    if ($counts =~ /^(\d+)\[(\d+)\]\/(\d+)\[([.\d)]+)\]$/) {
       $mutreads        = $1;
       $novstarts       = $2;
       $total_reads     = $3;
       $mutread_percent = $4;
    }
    else {
        die "Could not parse counts [$counts] using indel patterns";
    }
    return $mutreads,
           $novstarts,
           $total_reads,
           $mutread_percent;
}


sub parse_mutation {
    my $mutation = shift;

    my $ref_allele = '';
    my $alt_allele = '';

    if ($mutation =~ /^([ACGT-]+)\>([ACGT-]+)$/) {
        $ref_allele = uc($1);
        $alt_allele = uc($2);
    }
    else {
        die "Could not parse mutation [$mutation] using SNP or indel patterns";
    }
    return $ref_allele, $alt_allele;
}


sub parse_alleles {
    my $genotype = shift;

    my $allele1 = '';
    my $allele2 = '';

    if ($genotype =~ /^([ACGT-]+)\/([ACGT-]+)$/) {
        $allele1 = uc($1);
        $allele2 = uc($2);
    }
    else {
        die "Could not parse genotype [$genotype] into 2 alleles";
    }
    return $allele1, $allele2;
}



sub fails_mutreads_check {
    my $mutation = shift;
    my $TD       = shift;
    my $mutreads = shift;

    my ($ref_allele, $alt_allele) = parse_mutation( $mutation );

    my %alleles = ();
    while ($TD =~ /(\w):(\d+)\[([\d\.]+)\],(\d+)\[([\d\.]+)\]/g) {
        #print 'Muts: ', join("\,",$1,$2,$3,$4,$5), "\n";
        $alleles{ $1 } = $2+$4;
    }

    # Return true (1) if the record FAILS to have enough reads
    return ($alleles{ $alt_allele } < $mutreads) ? 1 : 0
}


sub _do_annovar_ensgene {
     my $infile          = shift;
     my $binary          = shift;
     my $verbose         = shift;

     my $cmdline = "$binary " .
                   "-geneanno -dbtype refgene ".
                   "$infile " .
                   "-buildver mm9 /share/software/annovar/mm9";

     qlogprint( "annotate_variation cmdline: $cmdline\n" ) if $verbose;

     if (system($cmdline) != 0) {
         # You can check all the failure possibilities by inspecting $? like this:
         if ($? == -1) {
             qlogprint "failed to execute annotate_variation: $!\n";
         }
         elsif ($? & 127) {
             qlogprint( sprintf( "annotate_variation died with signal %d, %s coredump\n",
                                ($? & 127), ($? & 128) ? 'with' : 'without' ));
         }
         else {
             qlogprint( sprintf "annotate_variation exited with value %d\n", $? >> 8 );
         }
     }
}


__END__

=head1 NAME

gemm.pl - GEMM project utility belt script


=head1 SYNOPSIS

 gemm.pl command [options]


=head1 ABSTRACT

This script is a collection of modes that relate to the GEMM project.


=head1 COMMANDS

 qsnp           - post-process mouse qSNP calls
 smallindeltool - post-process Bioscope small indel tool calls
 version        - print version number and exit immediately
 help           - display usage summary
 man            - display full man page


=head1 COMMAND DETAILS

=head2 QSNP

Filter records out of qSNP DDC1 output file.

 -i | --infile        qSNP .dcc1 file
 -o | --outfile       output file 
 -l | --logfile       Log file; optional
 -s | --mutreads      minimum number of mutant reads; default=5
 -r | --novstarts     minimum number of novel starts; default=4
 -v | --verbose       print progress and diagnostic messages

=head2 SMALLINDELTOOL

Filter records out of small indel tool DDC1 output file.

 -i | --infile        samll indel tool .dcc1 file
 -o | --outfile       output file 
 -l | --logfile       Log file; optional
 -s | --mutreads      minimum number of mutant reads; default=5
 -r | --novstarts     minimum number of novel starts; default=4
 -v | --verbose       print progress and diagnostic messages



=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: gemm.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012

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
