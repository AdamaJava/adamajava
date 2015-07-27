package QCMG::Annotate::Qsnp2;

###########################################################################
#
#  Module:   QCMG::Annotate::Qsnp2.pm
#  Creator:  John V Pearson
#  Created:  2012-11-28
#
#  This module is intended to eventually replace Qsnp.pm.  This new
#  version uses the new EnsemblConsequences module which gives us a much
#  better level of abstraction on annotation.
#
#  $Id$
#
###########################################################################

use strict;
#use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use Pod::Find qw(pod_where);

use QCMG::Annotate::EnsemblConsequences;
use QCMG::Annotate::EnsemblConsequenceRecord;
use QCMG::Annotate::Util qw( load_ensembl_API_modules
                             initialise_dcc2_file
                             transcript_to_domain_lookup 
                             transcript_to_geneid_and_symbol_lookups
                             transcript_lookups 
                             dcc2rec_from_dcc1rec
                             dccqrec_from_dcc1rec 
                             annotation_defaults
                             read_and_filter_dcc1_records
                             mutation_snp );
use QCMG::IO::DccSnpReader;
use QCMG::IO::DccSnpWriter;
use QCMG::IO::EnsemblDomainsReader;
use QCMG::IO::EnsemblTranscriptMapReader;
use QCMG::Util::QLog;
use QCMG::Util::Util qw( qexec_header );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class    = shift;
    my %defaults = @_;   # Allow for global defaults

    # Print usage message if no arguments supplied
    pod2usage( -input    => pod_where({-inc => 1, verbose => 1}, __PACKAGE__),
               -exitval  => 0,
               -verbose  => 99,
               -sections => 'DESCRIPTION/Commandline parameters' )
        unless (scalar @ARGV);

    # Setup defaults for CLI params
    my %params = ( infile   => '',
                   dcc2out  => '',
                   dccqout  => '',
                   logfile  => '',
                   ensver   => $defaults{ensver},
                   organism => $defaults{organism},
                   release  => $defaults{release},
                   verbose  => $defaults{verbose},
                   domains  => '',
                   symbols  => '' );

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'i|infile=s'         => \$params{infile},            # -i
           'o|dcc2out=s'        => \$params{dcc2out},           # -o
           'q|dccqout=s'        => \$params{dccqout},           # -q
           'l|logfile=s'        => \$params{logfile},           # -l
           'e|ensembl=s'        => \$params{ensver},            # -e
           'g|organism=s'       => \$params{organism},          # -g
           'r|release=s'        => \$params{release},           # -r
           'd|domains=s'        => \$params{domains},           # -d
           'y|symbols=s'        => \$params{symbols},           # -y
           'v|verbose+'         => \$params{verbose},           # -v
           );

    die "--infile, --dcc2out and --dccqout are required\n"
       unless ($params{infile} and $params{dcc2out} and $params{dccqout});

    # Pick default Ensembl files if not specified by user
    $params{domains} = annotation_defaults( 'ensembl_domains', \%params )
        unless ($params{domains});
    $params{symbols} = annotation_defaults( 'ensembl_symbols', \%params )
        unless ($params{symbols});
    $params{release} = '14'
        unless ($params{release});


    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n" );
    qlogprint( "Run parameters:\n" );
    foreach my $key (sort keys %params) {
        qlogprint( "  $key : ", $params{$key}, "\n" );
    }

    # Get those pesky ensembl API modules into the search path
    load_ensembl_API_modules( $params{ensver} );

    # Create the object
    my $self = { %params };

    bless $self, $class;
}


sub _execute_old {
    my $self = shift;

    # This is the version of execute() retired in 2013-06.  It serially
    # reads a DCC1 file, annotates the records and then writes out DCC2
    # and DCCQ files.  It does each step in turn so it keeps all of the
    # records in memory.  For somatic this is oK but for germline SNV
    # files it ended up needing >40Gb of RAM which became limiting.

    # Because there are so many types of DCC1 file, we are going to let
    # the DccSnpReader guess the version rather than trying to specify it

    my $dcc1 = QCMG::IO::DccSnpReader->new(
                   filename => $self->{infile},
                   verbose  => $self->{verbose} );
    
    # Die if the file is not a DCC1 variant or if we can't determine 
    # somatic/germline from file type

    die "input file must be a DCC1 variant but detected file type is [",
        $dcc1->version, "]\n" unless ($dcc1->dcc_type eq 'dcc1');
    die "cannot parse type (somatic/germline) from DCC1 file type [",
        $dcc1->version, "]\n" unless (defined $dcc1->variant_type);

    # Read in the SNP positions from the DCC1 and return lists of
    # those to be annotated and those to be skipped

    my ($ra_targets, $ra_skips) = read_and_filter_dcc1_records( $dcc1 );

    my @ens_recs = ();
    qlogprint( "Creating QCMG::Annotate::EnsemblConsequenceRecord objects\n" );
    foreach my $rec (@{ $ra_targets }) {

        # Cope with different column names in DCC somatic and germline files
        my $variation_name = '';
        my $mutation       = '';
        if ($dcc1->variant_type eq 'somatic') {
            $variation_name = $rec->mutation_id;
            $mutation = mutation_snp( $dcc1->variant_type,
                                      $rec->control_genotype,
                                      $rec->mutation,
                                      $rec->reference_genome_allele );
        }
        elsif ($dcc1->variant_type eq 'germline') {
            # Cope with different names for different DCC releases
            if ($dcc1->dcc_release eq '14') {
                $variation_name = $rec->variant_id;
            }
            else {
                $variation_name = $rec->variation_id;
            }
            $mutation = mutation_snp( $dcc1->variant_type,
                                      $rec->control_genotype,
                                      $rec->expressed_allele,
                                      $rec->reference_genome_allele );
        }

        # Create an annotatable record for the DCC1 record
        push @ens_recs,
             QCMG::Annotate::EnsemblConsequenceRecord->new(
                 chrom       => $rec->chromosome,
                 chrom_start => $rec->chromosome_start,
                 chrom_end   => $rec->chromosome_end,
                 mutation    => $mutation,
                 var_name    => $variation_name,
                 dcc1        => $rec );
    }

    # Release memory since we are now using @ens_recs
    $ra_targets = undef;

    # Load Ensembl lookups for domains, geneids and symbols
    my ( $tr2domain, $tr2geneid, $tr2symbol ) =
        transcript_lookups( $self->{domains}, $self->{symbols}, $self->{verbose} );

    # Lock-n-load an annotator ...
    my $ens = QCMG::Annotate::EnsemblConsequences->new(
                  ensver   => $self->ensver,
                  organism => $self->organism,
                  verbose  => $self->verbose );

    # In the interests of speed, we'll just pull a handful of record for
    # actual annotation during testing
    #my @recs = @ens_recs[30..45];
    #my @recs = @ens_recs[0..45];

    # Annotate !!!
    $ens->get_consequences( $tr2domain,
                            $tr2geneid,
                            $tr2symbol,
                            \@ens_recs );

    # Release memory since we are finished with these data structures
    $tr2domain = undef;
    $tr2geneid = undef;
    $tr2symbol = undef;

    # Construct QCMG metadata string that should be inserted at the top
    # of both the DCC2 and DCCQ files
    my $new_meta = qexec_header() . $dcc1->qcmgmeta_string;

    # Open DCC2 that is of the same "flavour" as the source DCC1 file and
    # copy out "skipped" records, skipping any non-PFAM domains

    my $dcc2_version = $dcc1->version;
    $dcc2_version =~ s/dcc1/dcc2/;
    my $dcc2 = initialise_dcc2_file( $self->{dcc2out},
                                     $dcc2_version,
                                     $self->{verbose},
                                     $self->{ensver},
                                     $ra_skips,
                                     $new_meta );

    foreach my $rec (@ens_recs) {
        my $ra_dcc2recs = $rec->as_dcc2_records;

        # There is a dangerous and subtle bug introduced here because
        # the DCC currently only accepts PFAM domain annotations.  If
        # the variant was smack in the middle of an exon and affected
        # multiple transcripts BUT all of the annotated domains were
        # non-PFAM, then ditching them would effectively drop all 
        # annotation for the variant.
        #
        # What we have to do instead is, if a record contains a
        # non-PFAM domain then we want to keep the record because we
        # want to know the affected transcript BUT we will need to zero
        # out the protein_domain_affected field.  We need to do this
        # check on every record (PFAM records match /^PF\d+$/).

        foreach my $dcc2rec (@{ $ra_dcc2recs }) {
            if ($dcc2rec->protein_domain_affected ne '-888' and
                $dcc2rec->protein_domain_affected !~ /^PF\d+$/) {
                warn "zeroing non-PFAM domain [",
                     $dcc2rec->protein_domain_affected,
                     '] for ',
                     $dcc2rec->mutation_id, ' ',
                     $dcc2rec->transcript_affected, "\n"
                    if ($self->verbose > 1);
                $dcc2rec->protein_domain_affected( '-888' );
            }
            $dcc2->write_record( $dcc2rec );
        }
    }


    # Create corresponding DCCQ file type from DCC1 file type
    my $dccq_version = $dcc2_version;
    $dccq_version =~ s/dcc2/dccq/;

    $new_meta = qexec_header() . $dcc1->qcmgmeta_string;

    my $dccq = QCMG::IO::DccSnpWriter->new(
                   filename => $self->{dccqout},
                   version  => $dccq_version,
                   meta     => $new_meta,
                   verbose  => $self->verbose );

    foreach my $rec (@ens_recs) {
        my $dccqrec = $rec->as_dccq_record;
        $dccq->write_record( $dccqrec );
    }


#    my @annot_chrs = sort keys %counts;
#    qlogprint( "Annotated SNPs on chromosomes:\n" );
#    foreach my $key (@annot_chrs) {
#        qlogprint( "  $key : ", $counts{$key}, "\n" );
#    }
#    my @conseq_chrs = sort keys %consequences;
#    qlogprint( "Found consequences on chromosomes:\n" );
#    foreach my $key (@conseq_chrs) {
#        qlogprint( "  $key : ", $consequences{$key}, "\n" );
#    }

    qlogend();
}


sub execute {
    my $self = shift;

	## before annotating SNPs, we should collapse same-codon adjacent SNPs
	## into a single mutation
	# copy original DCC1 file to avoid clobbering it
	my $dcc1orig	= $self->infile.".orig";
	my $cmd		= qq{cp }.$self->infile.qq{ $dcc1orig };
	qlogprint("Copying original DCC1 file: $cmd\n");
	my $rv		= system($cmd);
	qlogprint("RV: $rv\n");
	unless($rv == 0) {
		die "Cannot write working file $dcc1orig: $!\n";
	}

    # This is the 2013-06 rewrite to attempt to limit memory use,
    # particularly for germline DCC1's.  To do this, we need to unroll
    # loops - rather than read all of the DCC1 records into memory and
    # annotate them all at once, we will need to read them one at a 
    # time and annotate them one at a time.

    # Load Ensembl lookups for domains, geneids and symbols
    my ( $tr2domain, $tr2geneid, $tr2symbol ) = $self->_transcript_lookup;

    # Because there are so many types of DCC1 file, we are going to let
    # the DccSnpReader guess the version rather than trying to specify it

    my $dcc1 = QCMG::IO::DccSnpReader->new(
                   filename => $self->infile,
                   verbose  => $self->verbose );
    
    # Die if the file is not a DCC1 variant or if we can't determine 
    # somatic/germline from file type

    die "input file must be a DCC1 variant but detected file type is [",
        $dcc1->version, "]\n" unless ($dcc1->dcc_type eq 'dcc1');
    die "cannot parse type (somatic/germline) from DCC1 file type [",
        $dcc1->version, "]\n" unless (defined $dcc1->variant_type);

    # Note that the metadata strings inserted at the top of both the DCC2
    # and DCCQ files contain the metadata from DCC1 plus a qexec. 
    my $dcc1_qcmgmeta_string = defined $dcc1->qcmgmeta_string ?
                               $dcc1->qcmgmeta_string : '';

    # Open DCC2 that is of the same "flavour" as the source DCC1 file
    my $dcc2_version = $dcc1->version;
    $dcc2_version =~ s/dcc1/dcc2/;
    my $dcc2 = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dcc2out,
                   version  => $dcc2_version,
                   meta     => qexec_header() . $dcc1_qcmgmeta_string,
                   verbose  => $self->verbose );

    # Open DCCQ that is of the same "flavour" as the source DCC1 file
    my $dccq_version = $dcc1->version;
    $dccq_version =~ s/dcc1/dccq/;
    my $dccq = QCMG::IO::DccSnpWriter->new(
                   filename => $self->dccqout,
                   version  => $dccq_version,
                   meta     => qexec_header() . $dcc1_qcmgmeta_string,
                   verbose  => $self->verbose );

    # Lock-n-load an annotator ...
    my $ens = QCMG::Annotate::EnsemblConsequences->new(
                  ensver   => $self->ensver,
                  organism => $self->organism,
                  verbose  => $self->verbose );

    # Keep some counts of stuff
    my %tallies = ( keeps          => 0,
                    skips          => 0,
                    dcc2count      => 0,
                    dccqcount      => 0,
                    problem_chroms => {} );

    # Let the fun begin ...
    while (my $rec = $dcc1->next_record) {

       # We are going to skip annotating SNPs that are on sequences that we
       # know cannot be annotated, e.g. HSCHRUN_RANDOM_CTG22. We still
       # write these records to the DCC2 outfile but we do NOT try to
       # annotate them we skip to the next record.

        if (($rec->chromosome =~ /^H/) or
            ($rec->chromosome =~ /^GL/) or
            ($rec->chromosome =~ /n/)) {
            $tallies{problem_chroms}->{$rec->chromosome}++;

            my $rec2 = dcc2rec_from_dcc1rec( $rec, $dcc2->version );
            $rec2->gene_build_version( $self->ensver );
            $dcc2->write_record( $rec2 );
            $tallies{skips}++;
            next;
        }
        else {
            $tallies{keeps}++;
        }

        # Cope with different column names in DCC somatic and germline files
        my $variation_name = '';
        my $mutation       = '';
        if ($dcc1->variant_type eq 'somatic') {
            $variation_name = $rec->mutation_id;
            $mutation = mutation_snp( $dcc1->variant_type,
                                      $rec->control_genotype,
                                      $rec->mutation,
                                      $rec->reference_genome_allele );
        }
        elsif ($dcc1->variant_type eq 'germline') {
            # Cope with different names for different DCC releases
            if ($dcc1->dcc_release eq '14') {
                $variation_name = $rec->variant_id;
            }
            else {
                $variation_name = $rec->variation_id;
            }
            $mutation = mutation_snp( $dcc1->variant_type,
                                      $rec->control_genotype,
                                      $rec->expressed_allele,
                                      $rec->reference_genome_allele );
        }

        # Create an annotatable record for the DCC1 record
        my $ensrec = QCMG::Annotate::EnsemblConsequenceRecord->new(
                         chrom       => $rec->chromosome,
                         chrom_start => $rec->chromosome_start,
                         chrom_end   => $rec->chromosome_end,
                         mutation    => $mutation,
                         var_name    => $variation_name,
                         dcc1        => $rec );

        # Annotate this record
        $ens->get_consequences( $tr2domain,
                                $tr2geneid,
                                $tr2symbol,
                                [ $ensrec ] );

        # Create DCC2 records for each of the annotations 
        my $ra_dcc2recs = $ensrec->as_dcc2_records;
  
        #print Dumper $ensrec, $ra_dcc2recs;
        #die;

        # There is a dangerous and subtle bug introduced here because
        # the DCC currently only accepts PFAM domain annotations.  If
        # the variant was smack in the middle of an exon and affected
        # multiple transcripts BUT all of the annotated domains were
        # non-PFAM, then ditching them would effectively drop all 
        # annotation for the variant.
        #
        # What we have to do instead is, if a record contains a
        # non-PFAM domain then we want to keep the record because we
        # want to know the affected transcript BUT we will need to zero
        # out the protein_domain_affected field.  We need to do this
        # check on every record (PFAM records match /^PF\d+$/).

        foreach my $dcc2rec (@{ $ra_dcc2recs }) {
            if ($dcc2rec->protein_domain_affected ne '-888' and
                $dcc2rec->protein_domain_affected !~ /^PF\d+$/) {
                warn "zeroing non-PFAM domain [",
                     $dcc2rec->protein_domain_affected,
                     '] for ',
                     $dcc2rec->mutation_id, ' ',
                     $dcc2rec->transcript_affected, "\n"
                    if ($self->verbose > 1);
                $dcc2rec->protein_domain_affected( '-888' );
            }
            $dcc2->write_record( $dcc2rec );
            $tallies{dcc2count}++;
        }

        # Write out the DCCQ record
        my $dccqrec = $ensrec->as_dccq_record;
        $dccq->write_record( $dccqrec );
        $tallies{dccqcount}++;
    }

    qlogprint( 'Summary of DCC1 records from ', $dcc1->filename, "\n" );
    qlogprint( '  keeping ', $tallies{keeps}, " records\n" );
    qlogprint( '  dropping ', $tallies{skips}, " records:\n" );
    if ($tallies{skips}) {
        my @skipped_chrs = sort keys %{$tallies{problem_chroms}};
        foreach my $key (@skipped_chrs) {
            qlogprint( "    $key : ", $tallies{problem_chroms}->{$key}, "\n" );
        }
    }

    $ens->report;

    qlogprint( 'Wrote ', $tallies{dcc2count}, ' DCC2 records to ',
               $dcc2->filename, "\n" );
    qlogprint( 'Wrote ', $tallies{dccqcount}, ' DCCQ records to ',
               $dccq->filename, "\n" );

    qlogend();
}


sub _transcript_lookup {
    my $self = shift;

    # Load Ensembl lookups for domains, geneids and symbols
    my ( $tr2domain, $tr2geneid, $tr2symbol ) =
        transcript_lookups( $self->{domains}, $self->{symbols}, $self->{verbose} );

    return $tr2domain, $tr2geneid, $tr2symbol;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub ensver {
    my $self = shift;
    return $self->{ensver};
}

sub organism {
    my $self = shift;
    return $self->{organism};
}

sub infile {
    my $self = shift;
    return $self->{infile};
}

sub dcc2out {
    my $self = shift;
    return $self->{dcc2out};
}

sub dccqout {
    my $self = shift;
    return $self->{dccqout};
}


1;

__END__

=head1 NAME

QCMG::Annotate::Qsnp2 - Annotate qSNP output files


=head1 SYNOPSIS

 use QCMG::Annotate::Qsnp2;

my %defaults = ( ensver   => '70',
                 organism => 'human',
                 release  => '11',
                 verbose  => 0 );
 my $qsnp = QCMG::Annotate::Qsnp2->new( %defaults );
 $qsnp->execute;


=head1 DESCRIPTION

This module is based on a series of SNP annotation scripts originally
created by Karin Kassahn and JP and elaborated by Karin to adapt to new
organisms (Mouse etc) and new versions for the EnsEMBL API.  This
version has been rewritten by JP for better diagnostics and to support
the annotation of SNPs from different organisms and EnsEMBL versions
within a single executable.

=head2 Commandline parameters

 -i | --infile        Input file in DCC1 format
 -o | --dcc2out       Output file in DCC2 format
 -q | --dccqout       Output file in DCCQ format
 -l | --logfile       log file (optional)
 -g | --organism      Organism; default=human
 -e | --ensver        EnsEMBL version; default=70
 -r | --release       DCC release; default=11
 -d | --domains       Ensembl domains file
 -y | --symbols       Ensembl gene ids and symbols file
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message

Detailed documentation of the commandline parameters appears on the QCMG
wiki in the qsnp mode section of the qannotate.pl page.

=head1 AUTHORS

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=item Karin Kassahn, L<mailto:k.kassahn@uq.edu.au>

=back


=head1 VERSION

$Id$


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
