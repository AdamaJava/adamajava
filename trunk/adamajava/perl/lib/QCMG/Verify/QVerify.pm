package QCMG::Verify::QVerify;

###########################################################################
#
#  Module:   QCMG::Verify::QVerify
#  Creator:  John V Pearson
#  Created:  2013-07-19
#
#  Contains logic previously in qverify.pl.
#
#  $Id: QVerify.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak carp confess );
use Data::Dumper;
use IO::File;

use QCMG::FileDir::Finder;
use QCMG::IO::BamListReader;
use QCMG::IO::MafReader;
use QCMG::IO::MafWriter;
use QCMG::IO::QbpReader;
use QCMG::IO::VcfReader;
use QCMG::Util::QLog;
use QCMG::Util::Util qw( qexec_header );
use QCMG::Verify::VoteCounter;

use vars qw( $SVNID $REVISION %BAM_CATEGORY %LIMITS );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: QVerify.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    die "new() requires option indexbams\n" unless scalar(@{ $params{indexbams} });
    die "new() requires option qbasepileup\n" unless $params{qbasepileup};
    die "new() requires option bamlist\n" unless $params{bamlist};
    die "new() requires option outfile\n" unless $params{outfile};
    die "new() requires option maf or option vcf\n"
        unless ( $params{maf} or $params{vcf} );

    my $self = { maf             => ($params{maf} ? $params{maf} : ''),
                 vcf             => ($params{vcf} ? $params{vcf} : ''),
                 indexbams       => $params{indexbams},
                 qbasepileup     => $params{qbasepileup},
                 bamlist         => $params{bamlist},
                 outfile         => $params{outfile},
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;

    #print Dumper \%params, $self;

    $self->_verify();

    return $self;
}


sub _verify {
    my $self = shift;

    my $maf_version       = '2.2';
    my @maf_recs          = ();
    my @maf_extra_fields = ();
   
    if ( $self->{maf} ) {

        my $maf = QCMG::IO::MafReader->new( filename => $self->{maf},
                                            verbose  => $self->{verbose} );
        while (my $maf_rec = $maf->next_record) {
            push @maf_recs, $maf_rec;
        }
        $maf_version = $maf->maf_version;
        @maf_extra_fields = $maf->extra_fields;

        # Keep the MafReader because we are going to want to reuse the
        # header later
        $self->{mafreader} = $maf;
    }
    elsif ( $self->{vcf} ) {

        # If we have a MuTect VCF then the easiest (laziest) way to handle
        # this is to make a faux MAF record for each VCF record and then
        # feed it through the MAF-oriented pipeline that exists.

        my @maf_headers = @{ $QCMG::IO::MafRecord::VALID_HEADERS->{$maf_version} };

        my $vcf = QCMG::IO::VcfReader->new( filename => $self->{vcf},
                                            verbose  => $self->{verbose} );
        while (my $vcf_rec = $vcf->next_record) {
            my @maf_data = ( '','','QCMG','',$vcf_rec->chrom,
                             $vcf_rec->position,$vcf_rec->position,'','','SNP',
                             $vcf_rec->ref_allele,$vcf_rec->alt_allele,$vcf_rec->alt_allele,
                             '','','','',
                             $vcf_rec->ref_allele,$vcf_rec->ref_allele,
                             '','','','','','','','','','','','','' );
            my $maf_rec = QCMG::IO::MafRecord->new(
                               version => $maf_version,
                               headers => \@maf_headers,
                               data    => \@maf_data );
            push @maf_recs, $maf_rec;
        }
    }
    else {
        die "a MAF or VCF file must be supplied as input\n";
    }

    # Save away all of the MAF info
    $self->{maf_version}       = $maf_version;
    $self->{maf_recs}          = \@maf_recs;
    $self->{maf_extra_fields}  = \@maf_extra_fields;

    my $qbp = QCMG::IO::QbpReader->new( filename => $self->{qbasepileup},
                                        verbose  => $self->{verbose} );
    my $bml = QCMG::IO::BamListReader->new( filename => $self->{bamlist},
                                            verbose  => $self->{verbose} );
    my $vote = QCMG::Verify::VoteCounter->new( verbose  => $self->{verbose} );

    my @qbp_recs = ();
    while (my $qbp_rec = $qbp->next_record) {
        push @qbp_recs, $qbp_rec;
    }
    my @bam_recs = ();
    while (my $bam_rec = $bml->next_record) {
        push @bam_recs, $bam_rec;
    }
    my $donor = $bam_recs[0]->project;

    # Look through the BAM records to make sure we can find the BAMs
    # that have been nominated as Tumour and Normal.
    my %indexbams = ();
    foreach my $index (@{ $self->{indexbams} }) {

        $indexbams{ $index } = undef;
        foreach my $bam_rec (@bam_recs) {
            if ($bam_rec->BamName =~ /$index/i) {
                die "Multiple BAMs match the index BAM : $index\n"
                    if (defined $indexbams{ $index });
                $indexbams{ $index } = $bam_rec;
                qlogprint 'BAM ',$bam_rec->ID, " matches one of the indexbams\n";
            }
        }

        # If we didn't find a match then we are outta here ...
        die " No final BAM matched the index BAM : $index\n"
            unless (defined $indexbams{ $index });
    }

    # If we got to here then we must have found one and only one match
    # for each of the index bams.

    # Build a hash of the qbp records for quick lookup
    my %qbp_recs_hash = ();
    foreach my $qbp_rec (@qbp_recs) {
        my $key = $qbp_rec->Chromosome .':'.
                  $qbp_rec->Start .'-'.
                  $qbp_rec->End;
        push @{ $qbp_recs_hash{ $key } }, $qbp_rec;
    }

    my $outfh = IO::File->new( $self->{outfile}, 'w' );
    die 'Unable to open file ',$self->{outfile}," for writing; $!\n"
        unless defined $outfh;

    # For each MAF record, examine the qbasepileup records to pick those
    # that are acceptable for use in verification.

    my %verified = ( verified => 0, falsepos => 0, mixed => 0, untested => 0 );
    my $non_snp_ctr = 0;

    foreach my $maf_rec (@maf_recs) {

        # The pileup-based approach only really works for SNPs so we
        # should probably ignore non-SNP variants in the MAF
        if ($maf_rec->Variant_Type !~ /SNP/i) {
            $non_snp_ctr++;
            next;
        }

        # Collect all of the qbp records that match this MAF record
        my $key = $maf_rec->Chromosome .':'.
                  $maf_rec->Start_Position .'-'.
                  $maf_rec->End_Position;
        my @relevant_qbp_recs = @{ $qbp_recs_hash{$key} };

        # We need to do some painful logic here to determine the normal
        # and variant bases.  It's tempting (but not OK) to simply say that
        # the reference base is the normal base because that is not always
        # true - the reference base is not always the most common in the
        # population. We'll simply have to nut it out for ourselves but it's
        # not too awful because there are really only 2 common scenarios:
        #   (1) normal hom -> tumour het
        #   (2) normal het -> tumour hom
        # and two very uncommon scenarios:
        #   (3) normal hom -> tumour different hom
        #   (4) normal het -> tumour different het

        my $normal_base = '';
        my $variant_base = '';

        if ($maf_rec->Match_Norm_Seq_Allele1 eq
            $maf_rec->Match_Norm_Seq_Allele2) {
            # Normal is hom so expect tumour het
            $normal_base = $maf_rec->Match_Norm_Seq_Allele1;
            # Watch out for case -/- which we only usually see in the
            # LowConfidence qSNP MAF
            if ($normal_base eq '-') {
                $normal_base = $maf_rec->Reference_Allele;
            }
            if ($maf_rec->Tumor_Seq_Allele1 ne $normal_base) {
                $variant_base = $maf_rec->Tumor_Seq_Allele1;
            }
            else {
                $variant_base = $maf_rec->Tumor_Seq_Allele2;
            }
        }
        elsif ($maf_rec->Tumor_Seq_Allele1 eq
               $maf_rec->Tumor_Seq_Allele2) {
            # Normal is het so expect tumour hom
            $variant_base = $maf_rec->Tumor_Seq_Allele1;
            if ($maf_rec->Match_Norm_Seq_Allele1 ne $variant_base) {
                $normal_base = $maf_rec->Match_Norm_Seq_Allele1;
            }
            else {
                $normal_base = $maf_rec->Match_Norm_Seq_Allele2;
            }
        }
        else {
            if ($maf_rec->Match_Norm_Seq_Allele1 eq
                $maf_rec->Tumor_Seq_Allele1) {
                $normal_base = $maf_rec->Match_Norm_Seq_Allele2;
                $variant_base = $maf_rec->Tumor_Seq_Allele2;
            }
            elsif ($maf_rec->Match_Norm_Seq_Allele2 eq
                   $maf_rec->Tumor_Seq_Allele2) {
                $normal_base = $maf_rec->Match_Norm_Seq_Allele1;
                $variant_base = $maf_rec->Tumor_Seq_Allele1;
            }
            elsif ($maf_rec->Match_Norm_Seq_Allele1 eq
                   $maf_rec->Tumor_Seq_Allele2) {
                $normal_base = $maf_rec->Match_Norm_Seq_Allele2;
                $variant_base = $maf_rec->Tumor_Seq_Allele1;
            }
            elsif ($maf_rec->Match_Norm_Seq_Allele2 eq
                   $maf_rec->Tumor_Seq_Allele1) {
                $normal_base = $maf_rec->Match_Norm_Seq_Allele1;
                $variant_base = $maf_rec->Tumor_Seq_Allele2;
            }
            else {
                 die "Logic error - should not be able to get to this code!";
            }
            warn( 'Unusual variant - normal het to tumour het: ',
                  $maf_rec->Match_Norm_Seq_Allele1 .'/'.
                  $maf_rec->Match_Norm_Seq_Allele2 ,
                  ' -> ',
                  $maf_rec->Tumor_Seq_Allele1 .'/'.
                  $maf_rec->Tumor_Seq_Allele2, 
                  " ($normal_base\>$variant_base)\n" );
        }

        # We are going to let each BAM from a different platform vote
        # for/against the variant:
        # Normal BAM votes 'yes' if it has no variant bases
        # Normal BAM votes 'no' if it has variant bases
        # Tumour BAM votes 'yes' if it has variant bases
        # Tumour BAM votes 'no' if it has no variant bases
        # A BAM votes 'undecided' if it has no coverage
        # A BAM votes 'forbidden' if it is from the same platform
        # RNA BAMs always vote regardless of platform
       
        # Set up voting object for this donor/position
        my $vote = QCMG::Verify::VoteCounter->new( verbose  => $self->{verbose} );
        $vote->index_bam_rec( $_ ) foreach (values %indexbams);
        $vote->compare_index_bams;

        my $SnpId = '';

        print $outfh '[', $maf_rec->Chromosome,':',
                          $maf_rec->Start_Position,'-',
                          $maf_rec->End_Position,'  ', 
                          $maf_rec->Reference_Allele,'  ',
                          $maf_rec->Match_Norm_Seq_Allele1,'/',
                          $maf_rec->Match_Norm_Seq_Allele2,' > ',
                          $maf_rec->Tumor_Seq_Allele1,'/',
                          $maf_rec->Tumor_Seq_Allele2, "]\n";
        print $outfh join("\t", qw( ID Donor SnpID NormalBase VarBase
                                    NormalCount VarCount OtherCount Vote Bam )),"\n";
        foreach my $qbp_rec (@relevant_qbp_recs) {
            my $rh_counts = score_qbp( $qbp_rec, $normal_base, $variant_base );

            # The first thing we have to do is work out the properties of
            # the BAM that this qbp relates to.

            my $qbp_bam = find_my_bam( $qbp_rec, \@bam_recs );

            my $char = $vote->vote_by_percent_and_count( $qbp_bam,
                                                         $rh_counts );

            print $outfh join( "\t", $qbp_rec->ID,
                                     $qbp_rec->Donor,
                                     $qbp_rec->SnpId,
                                     $normal_base,
                                     $variant_base,
                                     $rh_counts->{normal},
                                     $rh_counts->{variant},
                                     $rh_counts->{other},
                                     $char,
                                     $qbp_rec->Bam ),"\n";
            $SnpId = $qbp_rec->SnpId;
        }

        # Make the big decision!
        my $verif_status = $vote->verif_status;

        # Keep some stats across all of the MAF records 
        $verified{ $verif_status }++;

        print $outfh "[Verification status - $verif_status (", $vote->vote_string,');  ',
                     $SnpId,'  ',
                     $maf_rec->Chromosome,':',
                     $maf_rec->Start_Position,'-',
                     $maf_rec->End_Position,'  ', 
                     'votes - yes:',$vote->tally('yes'),
                             ' no:',$vote->tally('no'),
                      ' undecided:',$vote->tally('undecided'),
                       ' coverage:',$vote->tally('coverage'),
                          ' index:',$vote->tally('index'),"]\n\n";

        # Fill in verification status in MafRecord
        $maf_rec->Validation_Status( $verif_status );
        $maf_rec->Validation_Method( 'qverify' );
    }

    # Print summary to output file
    print $outfh "[[Verification totals for $donor - ",
                 '  verified:', $verified{verified},
                 '  falsepos:', $verified{falsepos},
                 '  mixed:',    $verified{mixed},
                 '  untested:', $verified{untested}, "]]\n";

    $outfh->close;

    qlogprint "Skipped $non_snp_ctr non-SNP variants in MAF file\n";

    qlogprint "Verification totals:\n";
    qlogprint '    verified  - ', $verified{verified}, "\n";
    qlogprint '    falsepos  - ', $verified{falsepos}, "\n";
    qlogprint '    mixed     - ', $verified{mixed}, "\n";
    qlogprint '    untested  - ', $verified{untested}, "\n";
}


sub write_verified_maf {
    my $self     = shift;
    my $filename = shift;

    # Copy over the headers from the original MAF file but swap in our
    # own Q_EXEC header lines.
    my $comments = qexec_header();
    if (defined $self->{mafreader}) {
        my @comments = grep { $_ !~ /^#Q(_)?EXEC\s/ }
                           $self->{mafreader}->comments;
        $comments .= join("\n",@comments)."\n";
    }

    # Initialise MAF file to hold verified results
    my $maf_out = QCMG::IO::MafWriter->new( filename      => $filename,
                                            comments      => $comments,
                                            version       => $self->{maf_version},
                                            extra_fields  => $self->{maf_extra_fields},
                                            verbose       => $self->{verbose} );

    # The MAF records have already been verified as part of new() so all
    # we have to do here is write them all out to the new file.  
    foreach my $rec (@{ $self->{maf_recs} }) {
        $maf_out->write( $rec );
    }
}


sub score_qbp {
    my $qbp_rec      = shift;
    my $normal_base  = shift;
    my $variant_base = shift;

    warn "No normal base\n" unless $normal_base;
    warn "No variant_base\n" unless $variant_base;

    my %totals = ( normal  => 0,
                   variant => 0,
                   other   => 0 );

    # The easiest way to manipulate the counts is to throw them straight
    # into a hash and then operate on the hash.

    my %counts = ( A => $qbp_rec->Aplus + $qbp_rec->Aminus,
                   C => $qbp_rec->Cplus + $qbp_rec->Cminus,
                   G => $qbp_rec->Gplus + $qbp_rec->Gminus,
                   T => $qbp_rec->Tplus + $qbp_rec->Tminus );

    #print Dumper $normal_base, $variant_base, \%counts;

    # Tally normal/variant/other
    $totals{normal}  = $counts{ $normal_base };
    delete $counts{ $normal_base };
    $totals{variant} = $counts{ $variant_base };
    delete $counts{ $variant_base };
    my @others = values %counts;
    $totals{other} = $others[0] + $others[1];

    return \%totals;
}


sub find_my_bam {
    my $qbp_rec = shift;
    my $ra_bams = shift;

    # The qbp record is a pileup from a BAM and that BAM should in
    # theory be in our list of BAMs so all we have to do is go search
    # for it.  Of course it could all turn into a cluster-fsck.

    my $qbp_bam = undef;
    foreach my $bam_rec (@{ $ra_bams }) {
        if ($bam_rec->BamName eq $qbp_rec->Bam) {
            die "Multiple BAMs match the qbp BAM : $qbp_rec->Bam\n"
                if (defined $qbp_bam);
            $qbp_bam = $bam_rec;
        }
    }
    die ' No BAM matched QBP record: ',$qbp_rec->Bam,"\n"
        unless (defined $qbp_bam);

    return $qbp_bam;
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}
    

1;

__END__

=head1 NAME

QCMG::Verify::QVerify - run qverify


=head1 SYNOPSIS

 use QCMG::Verify::QVerify;


=head1 ABSTRACT

In order to save on sequencing costs for verification of our variants,
we are trying to do as much in-silico verification as possible.  This
involves doing pileups at variant positions across all BAMs available
for a donor and then trying to use non-Illumina data to corroborate the
HiSeq calls.


=head1 DESCRIPTION

=head2 Public Methods

=over

=item  B<new()>

 my $vf = QCMG::Verify::QVerify->new(
              bamlist     => /path/to/my_bam_list.txt,
              indexbams   => [ '/path/to/normal.bam',
                               '/path/to/tumour.bam' ],
              maf         => /path/to/my_variants.maf,
              qbasepileup => /path/to/my_variants.maf.qbp.txt,
              outfile     => /my/home/dir/qverify_report.txt,
              verbose     => 1 );
            
The new method


=over

=item B<maf>

MAF file containing the variant positions to be tested.  This file
should have come from variant calling on the pair of BAMs specified by
B<indexbams>.  This file should also have been used to
generate the list of pileups in the B<qbasepileup> file.

=item B<indexbams>

An array of the full pathnames of the BAMs used to generate the variant
calls in the MAF file - there should usually be 2, a normal BAM and a
tumour BAM.  Note that both of these BAM files should still appear in
the B<bamlist> file.

=item B<qbasepileup>

File output by running the qbasepileup java tool with the positions
in the B<maf> file against the BAM files in B<bamlist>.

=item B<bamlist>

File that contans a list of BAM files with their properties.

=item B<outfile>

Output file name.

=item B<verbose>

Print progress and diagnostic messages.  Higher numbers enable 
higher levels of verbosity.

=back

=item  B<write_verified_maf()>

=back



=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: QVerify.pm 4665 2014-07-24 08:54:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013,2014

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
