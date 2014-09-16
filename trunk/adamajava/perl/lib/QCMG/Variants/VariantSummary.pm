package QCMG::Variants::VariantSummary;

###########################################################################
#
#  Module:   QCMG::Variants::VariantSummary
#  Creator:  John V Pearson
#  Created:  2013-11-04
#
#  For use in qmaftools.pl and QCMG::IO::MafRecordCollection to keep
#  track of observed variants.  There can only be one variant per
#  patient/gene so setting a second value will overwrite the first.
#
#  $Id: VariantSummary.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use POSIX qw( floor );

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $MAF_VAR_PRIORITY );

( $REVISION ) = '$Revision: 4665 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: VariantSummary.pm 4665 2014-07-24 08:54:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global


###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    # We keep redundant summaries hashed by patient->gene and by
    # gene->patient.  This wastes memory but makes a lot of the summary
    # calculations a lot quicker.

    my $self = { By_Gene            => {},
                 By_Patient         => {},
                 By_Variant         => {},
                 By_Gene_Variant    => {},
                 By_Patient_Variant => {},
                 By_Patient_Gene    => {},
                 verbose => $params{verbose} || 0 };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub record_count {
    my $self = shift;
    return scalar( $self->records );
}


sub add_variant {
    my $self = shift;
    my %data = @_;

    # Make sure that we have all of the values we need
    confess "add_variant() must be passed a value for patient\n"
        unless (exists $data{patient} and defined $data{patient});
    confess "add_variant() must be passed a value for gene\n"
        unless (exists $data{gene} and defined $data{gene});
    confess "add_variant() must be passed a value for variant\n"
        unless (exists $data{variant} and defined $data{variant});

    my $patient = $data{patient};
    my $gene    = $data{gene};
    my $variant = $data{variant};

    # Check whether we already have a variant for this patient/gene
    my $old_var = undef;
    if (exists $self->{By_Patient_Gene}->{$patient}->{$gene}) {
        # Save existing variant and set to new value
        $old_var = $self->{By_Patient_Gene}->{$patient}->{$gene};
        $self->{'By_Patient_Gene'}->{$patient}->{$gene} = $variant;
        
        warn "duplicate variant found for $patient / $gene - is [$variant], was [$old_var]\n";

        # Decrement old counts
        $self->{'By_Variant'}->{ $old_var }--;
        $self->{'By_Gene_Variant'}->{ $gene }->{ $old_var }--;
        $self->{'By_Patient_Variant'}->{ $patient }->{ $old_var }--;

        # Increment new counts
        $self->{'By_Variant'}->{ $variant }++;
        $self->{'By_Gene_Variant'}->{ $gene }->{ $variant }++;
        $self->{'By_Patient_Variant'}->{ $patient }->{ $variant }++;
    }
    else {
        $self->{'By_Patient_Gene'}->{$patient}->{$gene} = $variant;
        # Increment counts
        $self->{'By_Gene'}->{ $gene }++;
        $self->{'By_Patient'}->{ $patient }++;
        $self->{'By_Variant'}->{ $variant }++;
        $self->{'By_Gene_Variant'}->{ $gene }->{ $variant }++;
        $self->{'By_Patient_Variant'}->{ $patient }->{ $variant }++;
    }

    return $old_var;
}


sub add_cnv_variant {
    my $self = shift;
    my %data = @_;

    # This is a special CNV-only version of the more generic
    # add_variant() method.  The problem with CNV is that we want to
    # keep the # most "extreme" copy number and that is going to require
    # some rules so this is the method with those rules.

    # Make sure that we have all of the values we need
    confess "add_variant() must be passed a value for patient\n"
        unless (exists $data{patient} and defined $data{patient});
    confess "add_variant() must be passed a value for gene\n"
        unless (exists $data{gene} and defined $data{gene});
    confess "add_variant() must be passed a value for variant\n"
        unless (exists $data{variant} and defined $data{variant});

    my $patient = $data{patient};
    my $gene    = $data{gene};
    my $new_var = $data{variant};

    # Check whether we already have a variant for this patient/gene
    my $old_var = undef;
    if (exists $self->{By_Patient_Gene}->{$patient}->{$gene}) {
        # Save existing variant
        $old_var = $self->{By_Patient_Gene}->{$patient}->{$gene};

        # Apply the rules to decide whether the new variant is more
        # "extreme" than the old variant.

        my $old_score = _cnv_extremeness_score( $old_var );
        my $new_score = _cnv_extremeness_score( $new_var );

        if ($new_score > $old_score) {
        
            # Only need to change counts etc if we decide to swap in the
            # new value over the old.

            $self->{'By_Patient_Gene'}->{$patient}->{$gene} = $new_var;

            # Decrement old counts
            $self->{'By_Variant'}->{ $old_var }--;
            $self->{'By_Gene_Variant'}->{ $gene }->{ $old_var }--;
            $self->{'By_Patient_Variant'}->{ $patient }->{ $old_var }--;

            # Increment new counts
            $self->{'By_Variant'}->{ $new_var }++;
            $self->{'By_Gene_Variant'}->{ $gene }->{ $new_var }++;
            $self->{'By_Patient_Variant'}->{ $patient }->{ $new_var }++;

            warn "duplicate CNV variant found for $patient / $gene - ".
                 "new [$new_var], old [$old_var], kept [$new_var]\n";
        }
        else {
            warn "duplicate CNV variant found for $patient / $gene - ".
                 "new [$new_var], old [$old_var], kept [$old_var]\n";
        }
    }
    else {
        $self->{'By_Patient_Gene'}->{$patient}->{$gene} = $new_var;
        # Increment counts
        $self->{'By_Gene'}->{ $gene }++;
        $self->{'By_Patient'}->{ $patient }++;
        $self->{'By_Variant'}->{ $new_var }++;
        $self->{'By_Gene_Variant'}->{ $gene }->{ $new_var }++;
        $self->{'By_Patient_Variant'}->{ $patient }->{ $new_var }++;
    }

    return $old_var;
}


sub _cnv_extremeness_score {
    my $cnv = shift;
    # Extremeness Score:
    # 0 : copy number 2
    # 1 : copy number 3/4/5
    # 2 : copy-neutral LOH
    # 3 : copy number 1
    # 4 : copy number > 5 (high-gain)
    # 5 : homozygous deletion (0)
    my $score = 0;
    if ($cnv =~ /copy-neutral LOH/i) {
        $score = 3;
    }
    elsif ($cnv > 2 and $cnv < 6) {
        $score = 1;
    }
    elsif ($cnv == 1) {
        $score = 2;
    }
    elsif ($cnv > 5) {
        $score = 4;
    }
    elsif ($cnv == 0) {
        $score = 5;
    }
    return $score;
}


sub patients {
    my $self = shift;
    return sort keys %{$self->{By_Patient}};
}


sub genes {
    my $self = shift;
    return sort keys %{$self->{By_Gene}};
}


sub variants {
    my $self = shift;
    return sort keys %{$self->{By_Variant}};
}


sub variant_by_patient_and_gene {
    my $self    = shift;
    my $patient = shift;
    my $gene    = shift;

    return $self->{By_Patient_Gene}->{$patient}->{$gene}
        if (exists $self->{By_Patient_Gene}->{$patient}->{$gene});
    # return undef if no value (0 and '' could both be legal in some
    # categorisation schemes).
    return undef;
}


sub summary_by_patient {
    my $self    = shift;
    my $patient = shift;

    if (exists $self->{By_Patient}->{$patient}) {
        return $self->{By_Patient}->{$patient};
    }
    else {
        # return 0 if no value
        #qlogprint( "summary_by_patient : no value found for patient $patient\n" );
        return 0;
    }
}


sub summary_by_gene {
    my $self = shift;
    my $gene = shift;

    if (exists $self->{By_Gene}->{$gene}) {
        return $self->{By_Gene}->{$gene};
    }
    else {
        # return 0 if no value
        #qlogprint( "summary_by_gene : no value found for gene $gene\n" );
        return 0;
    }
}


sub summary_by_variant {
    my $self    = shift;
    my $variant = shift;

    return $self->{By_Variant}->{$variant}
        if (exists $self->{By_Variant}->{$variant});
    # return 0 if no value
    return 0;
}


sub summary_by_patient_and_variant {
    my $self    = shift;
    my $patient = shift;
    my $variant = shift;

    return $self->{By_Patient_Variant}->{$patient}->{$variant}
        if (exists $self->{By_Patient_Variant}->{$patient}->{$variant});
    # return 0 if no value
    return 0;
}


sub summary_by_gene_and_variant {
    my $self    = shift;
    my $gene    = shift;
    my $variant = shift;

    return $self->{By_Gene_Variant}->{$gene}->{$variant}
        if (exists $self->{By_Gene_Variant}->{$gene}->{$variant});
    # return 0 if no value
    return 0;
}


sub gene_was_seen {
    my $self = shift;
    my $gene = shift;
    # Returns 1 if the gene was seen and 0 otherwise
    return (exists $self->{By_Gene}->{$gene}) ? 1 : 0;
}


sub patient_was_seen {
    my $self = shift;
    my $id   = shift;
    # Returns 1 if the patient was seen and 0 otherwise
    return (exists $self->{By_Patient}->{$id}) ? 1 : 0;
}


sub variant_was_seen {
    my $self    = shift;
    my $variant = shift;
    # Returns 1 if the variant was seen and 0 otherwise
    return (exists $self->{By_Variant}->{$variant}) ? 1 : 0;
}


1;

__END__


=head1 NAME

QCMG::Variants::VariantSummary - Variant counts


=head1 SYNOPSIS

 use QCMG::Variants::VariantSummary;

 my $vs = QCMG::Variants::VariantSummary->new();

 $vs->add_variant( gene    => 'TP53',
                   patient => 'ICGC_007',
                   variant => 'CpG- G -> A/C' );

 my $count = $vs->summary_by_gene( 'TP53' );


=head1 DESCRIPTION

This module provides operations on a collection of QCMG::IO::MafRecord object.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $vs = QCMG::variants::VariantSummary->new( verbose => 1 );

There are no compulsory options to new().  Verbose is set to 0 by
default and any non-zero integer will turn on additional logging.

=item B<add_variant()>

 $vs->add_variant( gene    => 'TP53',
                   patient => 'ICGC_007',
                   variant => 'CpG- G -> A/C' );

The caller must supply values for gene, patient and variant.  If the
patient/gene combination already has a variant set, setting it again
will overwrite the previous value.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: VariantSummary.pm 4665 2014-07-24 08:54:04Z j.pearson $


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
