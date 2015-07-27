package QCMG::SamTools::Bam::Target;
use strict;

use base 'QCMG::SamTools::Bam::Query';

=head1 NAME

QCMG::SamTools::Bam::Target -- Object representing the query portion of a BAM/SAM alignment in NATIVE alignment

=head1 SYNOPSIS

This is identical to QCMG::SamTools::Bam::Query, except that the dna, qscores
and start and end positions are all given in the orientation in which
the read was sequenced, not in the oreintation in which it was
aligned.

=cut


sub dna {
    my $self = shift;
    return $$self->strand > 0 ? $$self->qseq : reversec($$self->qseq);
}

sub qscore {
    my $self = shift;
    my @qscore = $$self->qscore;
    @qscore    = reverse @qscore if $$self->strand < 0;
    return wantarray ? @qscore : \@qscore;
}


sub start {
    my $self = shift;
    return $self->strand > 0 ? $self->low : $self->high;
}

sub end {
    my $self = shift;
    return $self->strand > 0 ? $self->high : $self->low;
}

# sub strand { 1 }

sub reversec {
    my $dna = shift;
    $dna =~ tr/gatcGATC/ctagCTAG/;
    return scalar reverse $dna;
}

1;

