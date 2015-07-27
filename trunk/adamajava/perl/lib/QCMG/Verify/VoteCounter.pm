package QCMG::Verify::VoteCounter;

###########################################################################
#
#  Module:   QCMG::Verify::VoteCounter
#  Creator:  John V Pearson
#  Created:  2013-04-04
#
#  Tallies votes for/against verification based on inspection of 
#  qbasepileup records for final BAMs.  This object should be instantiated
#  for each donor/position and used for all matching qbasepileup records.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use QCMG::IO::QbpRecord;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %BAM_CATEGORY %LIMITS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {
    # Setup any global data structures

    # As at 2013-03-25 this is the list of valid 'Sample Code' values:
    # 1:Normal blood
    # 3:Normal control (adjacent)
    # 4:Normal control (other site)
    # 5:Control cell line derived from normal tissue
    # 6:Normal mouse host
    # 7:Primary tumour
    # 8:Mouse xenograft derived from tumour
    # 9:Cell line derived from tumour
    # 10:Cell line derived from xenograft
    # 11:<other>
    # 12:Ascites
    # 13:Serum
    # 14:Tumour local recurrence
    # 15:Tumour metastasis to local lymph node
    # 16:Tumour metastasis to distant location
    # 98:Control/Reference sample (not otherwise specified)
    # 99:Experimental sample (not otherwise specified)

    # This data categorises BAMs as to their utility for verification
    # based on the "Sample Code":
    # 0 - sample cannot be used for human verification,
    # 1 - a normal sample
    # 2 - a tumour sample
 
    %BAM_CATEGORY = (
            '1:Normal blood' => 1,
            '3:Normal control (adjacent)' => 1,
            '4:Normal control (other site)' => 1,
            '5:Control cell line derived from normal tissue' => 1,
            '6:Normal mouse host' => 0,
            '7:Primary tumour' => 2,
            '8:Mouse xenograft derived from tumour' => 2,
            '9:Cell line derived from tumour' => 2,
            '10:Cell line derived from xenograft' => 2,
            '11:<other>' => 0,
            '12:Ascites' => 2,
            '13:Serum' => 2,
            '14:Tumour local recurrence' => 2,
            '15:Tumour metastasis to local lymph node' => 2,
            '16:Tumour metastasis to distant location' => 2,
            '98:Control/Reference sample (not otherwise specified)' => 0,
            '99:Experimental sample (not otherwise specified)' => 0 );
    %LIMITS = (
            RNA_max_normal_pc  => 0.02,
            RNA_max_normal_ctr => 2,
            RNA_min_tumour_pc  => 0.05,
            RNA_min_tumour_ctr => 2,
            DNA_max_normal_pc  => 0.02,
            DNA_max_normal_ctr => 2,
            DNA_min_tumour_pc  => 0.05,
            DNA_min_tumour_ctr => 2 );
}

sub new {
    my $class = shift;
    my %params = @_;

    my $self = { votes           => { 'yes'       => 0,
                                      'no'        => 0,
                                      'undecided' => 0,
                                      'index'     => 0,
                                      'coverage'  => 0 },
                 index_bam_recs  => [],
                 vote_chars      => [],
                 verbose         => ($params{verbose} ?
                                     $params{verbose} : 0),
               };

    bless $self, $class;
}


sub index_bam_rec {
    my $self  = shift;
    my $index = shift;

    die 'index_bam_rec() must be passed QCMG::IO::BamListRecord objects '.
        'but you passed ['. (ref($index) ? ref($index) : $index) .']'
        unless ( ref($index) eq 'QCMG::IO::BamListRecord' );
    
    push @{ $self->{index_bam_recs} }, $index;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}
    

sub index_bam_recs {
    my $self = shift;
    return @{ $self->{index_bam_recs} };
}


sub compare_index_bams {
    my $self = shift;

    # If there are 2 index BAMs then check that they constitute an 
    # acceptable pair otherwise return immediately

    my @indices = $self->index_bam_recs;
    return 1 unless (scalar(@indices) == 2); 

    die 'Index BAM parent_project mismatch : ',
        $indices[0]->parent_project, ',', $indices[1]->parent_project, "\n"
        unless ($indices[0]->parent_project eq $indices[1]->parent_project);
    die 'Index BAM project mismatch : ',
        $indices[0]->project, ',', $indices[1]->project, "\n"
        unless ($indices[0]->project eq $indices[1]->project);
    die 'Index BAM material mismatch : ',
        $indices[0]->material, ',', $indices[1]->material, "\n"
        unless ($indices[0]->material eq $indices[1]->material);
    die 'Index BAM sample_code match : ',
        $indices[0]->sample_code, ',', $indices[1]->sample_code, "\n"
        if ($indices[0]->sample_code eq $indices[1]->sample_code);
    die 'Index BAM sequencing_platform mismatch : ',
        $indices[0]->sequencing_platform, ',',
        $indices[1]->sequencing_platform, "\n"
        unless ($indices[0]->sequencing_platform eq
                $indices[1]->sequencing_platform);
    die 'Index BAM aligner mismatch : ',
        $indices[0]->aligner, ',', $indices[1]->aligner, "\n"
        unless ($indices[0]->aligner eq $indices[1]->aligner);
    die 'Index BAM capture_kit mismatch : ',
        $indices[0]->capture_kit, ',', $indices[1]->capture_kit, "\n"
        unless ($indices[0]->capture_kit eq $indices[1]->capture_kit);

#    die 'Index BAM LibraryProtocol mismatch : ',
#        $indices[0]->LibraryProtocol, ',',
#        $indices[1]->LibraryProtocol, "\n"
#        unless ($indices[0]->LibraryProtocol eq
#                $indices[1]->LibraryProtocol);

    die 'Index BAM species_reference_genome mismatch : ',
        $indices[0]->species_reference_genome, ',',
        $indices[1]->species_reference_genome, "\n"
        unless ($indices[0]->species_reference_genome eq
                $indices[1]->species_reference_genome);
}


sub vote_by_percent_and_count {
    my $self      = shift;
    my $qbp_rec   = shift;
    my $rh_counts = shift;

    my @index_bams = $self->index_bam_recs;
    # Reclassify sequencing platform to collapse some platforms
    my $qbp_platform    = _recode_seq_platform( $qbp_rec->sequencing_platform );
    my $normal_platform = _recode_seq_platform( $index_bams[0] );
    
#    print Dumper $rh_counts
#      unless (defined $rh_counts and 
#              exists $rh_counts->{variant} and
#              exists $rh_counts->{normal} and 
#              exists $rh_counts->{other} and
#              defined $rh_counts->{variant} and
#              defined $rh_counts->{normal} and 
#              defined $rh_counts->{other});

    my $total_reads = $rh_counts->{variant} + $rh_counts->{normal} + $rh_counts->{other};
    my $variant_ctr = $rh_counts->{variant};
    # Prevent divide-by-zero problem
    my $variant_pc  = $total_reads ? $rh_counts->{variant} / $total_reads : 0;

    my $vote = '?'; 

    # Check for index BAM
    my $isIndex = 0;
    foreach my $bam_rec (@index_bams) {
        if ($qbp_rec->BamName eq $bam_rec->BamName) {
            $isIndex = 1;
        }
    }

    if ($isIndex) {
        $vote = 'I';
    }
    elsif ( $total_reads < 10) {
        # normal_variant coverage below 10 does not get to vote
        $vote = 'C';
    }
    elsif ($qbp_rec->material eq '2:RNA') {
        # RNA always votes even if the same platform
        if ( _is_normal( $qbp_rec ) ) {
            if ($variant_pc  >= $LIMITS{RNA_max_normal_pc} and
                $variant_ctr >= $LIMITS{RNA_max_normal_ctr}) {
                $vote = 'N';
            }
        }
        elsif ( _is_tumour( $qbp_rec ) ) {
            if ($variant_pc  >= $LIMITS{RNA_min_tumour_pc} and
                $variant_ctr >= $LIMITS{RNA_min_tumour_ctr}) {
                $vote = 'Y';
            }
        }
    }
    elsif ($qbp_rec->material eq '1:DNA') {
        if ( _is_normal( $qbp_rec ) ) {
            if ($variant_pc  >= $LIMITS{DNA_max_normal_pc} and
                $variant_ctr >= $LIMITS{DNA_max_normal_ctr}) {
                $vote = 'N';
            }
        }
        elsif ( _is_tumour( $qbp_rec ) ) {
            if ($variant_pc  >= $LIMITS{RNA_min_tumour_pc} and
                $variant_ctr >= $LIMITS{RNA_min_tumour_ctr}) {
                $vote = 'Y';
            }
        }
    }


    # We are not going to use $void, we're just using it for the side
    # effect of forcing evaluation of the chained trinary
    my $void = $vote eq '?' ? $self->{votes}->{undecided}++ :
               $vote eq 'I' ? $self->{votes}->{'index'}++ :
               $vote eq 'C' ? $self->{votes}->{coverage}++ :
               $vote eq 'Y' ? $self->{votes}->{yes}++ :
               $vote eq 'N' ? $self->{votes}->{no}++ : '';

    # Collect chars for output
    if ($vote eq 'Y' or $vote eq 'N') {
        my $vote_char = ($qbp_platform eq $normal_platform) ?
                        lc( $vote ) : $vote;
        push @{ $self->{vote_chars} }, $vote_char;
    }

    return $vote;
}


sub tally {
    my $self = shift;
    my $type = shift;

    return undef unless exists $self->{votes}->{$type};
    return $self->{votes}->{$type};
}


sub verif_status {
    my $self = shift;

    my $verif_status = '';
    if ($self->tally('yes') > 0 and $self->tally('no') > 0) {
        $verif_status = 'mixed';
    }   
    elsif ($self->tally('yes') > 0) {
        $verif_status = 'verified';
    }   
    elsif ($self->tally('no') > 0) {
        $verif_status = 'falsepos';
    }   
    else {
        $verif_status = 'untested';
    }

    return $verif_status;
}


sub vote_string {
    my $self = shift;
    return join('',reverse sort @{ $self->{vote_chars} });
}


sub _recode_seq_platform {
    my $platform = shift;
    return $platform =~ /HiSeq/ ?  'HiSeq' :
           $platform =~ /SOLiD/ ?  'SOLiD' :
           $platform;
}


sub _is_normal {
    my $rec = shift;
    return 0 if (! exists $BAM_CATEGORY{ $rec->sample_code });
    return 1 if ($BAM_CATEGORY{ $rec->sample_code } == 1);
    return 0;
}


sub _is_tumour {
    my $rec = shift;
    return 0 if (! exists $BAM_CATEGORY{ $rec->sample_code });
    return 1 if ($BAM_CATEGORY{ $rec->sample_code } == 2);
    return 0;
}



1;

__END__


=head1 NAME

QCMG::Verify::VoteCounter - tally verification votes


=head1 SYNOPSIS

 use QCMG::Verify::VoteCounter;


=head1 DESCRIPTION

This module ...


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

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
