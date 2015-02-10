package QCMG::IO::MafRecordCollection;

###########################################################################
#
#  Module:   QCMG::IO::MafRecordCollection
#  Creator:  John V Pearson
#  Created:  2012-04-13
#
#  Operations on a collection of QCMG::IO::MafRecord objects.
#
#  $Id: MafRecordCollection.pm 4663 2014-07-24 06:39:00Z j.pearson $
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
use Storable qw(dclone);

use QCMG::Util::QLog;
use QCMG::Variants::VariantSummary;

use vars qw( $SVNID $REVISION $MAF_VAR_PRIORITY );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: MafRecordCollection.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global

BEGIN {
    # This hash holds the Variation_Classification values from most
    # damaging (1) to least (9).  Where a patient has multiple mutations
    # in a single gene then the most damaging is chosen based on this table.

    $MAF_VAR_PRIORITY = { Frame_Shift_Del   => 1,
                          Frame_Shift_Ins   => 2,
                          Nonsense_Mutation => 3,
                          Nonstop_Mutation  => 4,
                          In_Frame_Del      => 5,
                          In_Frame_Ins      => 6,
                          Missense_Mutation => 7,
                          Splice_Site       => 8,
                          Silent            => 99 };
}


###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { records => [],
                 verbose => $params{verbose} || 0 };
    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub records {
    my $self = shift;
    return @{ $self->{records} };
}


sub record_count {
    my $self = shift;
    return scalar( $self->records );
}


sub add_record {
    my $self = shift;
    my $rec  = shift;

    if (ref($rec) ne 'QCMG::IO::MafRecord') {
        confess 'MafRecordCollection::add_record() can only accept objects '.
                'of class QCMG::IO::MafRecord and you passed an object of '.
                'type '. ref($rec);
    }

    push @{ $self->{records} }, $rec;
}


sub add_records {
    my $self    = shift;
    my $ra_recs = shift;

    foreach my $rec (@{ $ra_recs }) {
        $self->add_record( $rec );
    }
}


sub filter_by_genes {
    my $self     = shift;
    my $ra_genes = shift;

    qlogprint( 'filtering MAF records based on list of ',
               scalar(@{ $ra_genes }), " genes\n" );

    # If a gene list was supplied then filter records
    qlogprint( "record count before gene filter: ",
               $self->record_count, "\n" );

    my @keep_records = ();
    if (scalar(@{$ra_genes})) {
        my %genes = ();
        $genes{$_} = 1 foreach @{ $ra_genes };
        foreach my $rec ($self->records) {
            next unless exists $genes{ $rec->Hugo_Symbol };
            push @keep_records, $rec;
        }
    }

    my $new_coll = QCMG::IO::MafRecordCollection->new();
    $new_coll->add_records( \@keep_records );
    qlogprint( "record count after gene filter: ",
               $new_coll->record_count, "\n" );

    return $new_coll;
}


sub filter_by_tumour_ids {
    my $self   = shift;
    my $ra_ids = shift;
    $self->_filter_by_ids( $ra_ids, 1 );
}


sub filter_by_normal_ids {
    my $self   = shift;
    my $ra_ids = shift;
    $self->_filter_by_ids( $ra_ids, 2 );
}


sub _filter_by_ids {
    my $self   = shift;
    my $ra_ids = shift;
    my $mode   = shift;

    my $type = ($mode == 1) ? 'Tumour' : 'Normal';
    qlogprint( 'filtering MAF records based on list of ',
               scalar(@{ $ra_ids }), " $type ID(s)\n" );

    # If a patient list was supplied then filter records
    qlogprint( "record count before patient filter: ",
               $self->record_count, "\n" );

    my @keep_records = ();
    if (scalar(@{$ra_ids})) {
        my %ids = ();
        $ids{$_} = 1 foreach @{ $ra_ids };
        foreach my $rec ($self->records) {
            # mode decides whether we are filtering on normal or tumour id
            my $id = $mode == 1 ? $rec->Tumor_Sample_Barcode :
                     $mode == 2 ? $rec->Matched_Norm_Sample_Barcode :
                     undef;
            next unless exists $ids{ $id };
            push @keep_records, $rec;
        }
    }

    my $new_coll = QCMG::IO::MafRecordCollection->new();
    $new_coll->add_records( \@keep_records );
    qlogprint( "record count after patient filter: ",
               $new_coll->record_count, "\n" );

    return $new_coll;
}


sub filter_by_worst_consequence {
    my $self = shift;

    $self->split_multi_gene_records;

    my %mafs_to_keep = ();
    my %problems = ();
    foreach my $mfr ($self->records) {

        my $donor = $mfr->Tumor_Sample_Barcode;
        my $gene  = $mfr->Hugo_Symbol;
        my $var   = $mfr->Variant_Classification;

        # Keep only the worst consequence

        if (exists $mafs_to_keep{$donor}->{$gene}) {
            my $old_mfr = $mafs_to_keep{$donor}->{$gene};
            my $old_var = $old_mfr->Variant_Classification;

            my $old_consequence = maf_var_priority( $old_var );
            my $new_consequence = maf_var_priority( $var );

            # Keep a tally of problems
            if ($new_consequence == 98) {
               $problems{ $var }++;
            }

            if ($new_consequence == $old_consequence) {
                # do nothing if they are the same priority
            }
            elsif ($new_consequence < $old_consequence) {
                # if new consequence is higher (lower number)
                # priority then replace old with new
                qlogprint( "replacing variant $old_var".
                           " with $var for $donor/$gene\n" )
                    if $self->verbose > 1;
                $mafs_to_keep{$donor}->{$gene} = $mfr;
            }
            else {
                qlogprint( "existing variant $old_var".
                           " is worse than $var for $donor/$gene\n" )
                    if $self->verbose > 1;
            }
        }
        else {
            $mafs_to_keep{$donor}->{$gene} = $mfr;
        }
    }

    # Report any variant types that we could not categorise
    foreach my $var (sort keys %problems) {
        my $count = $problems{$var};
        warn "MAF variant consequence $var was seen $count times but is not part of worst-consequence logic\n";
    }

    # Now that we have a single MAF for each gene/donor, turn the hash
    # back into a list of MafRecords.
    my @keep_records = ();
    foreach my $donor (keys %mafs_to_keep) {
        foreach my $gene (keys %{ $mafs_to_keep{$donor} }) {
            push @keep_records, $mafs_to_keep{$donor}->{$gene};
        }
    }

    my $new_coll = QCMG::IO::MafRecordCollection->new();
    $new_coll->add_records( \@keep_records );
    qlogprint( "record count after consequence filter: ",
               $new_coll->record_count, "\n" );

    return $new_coll;
}


sub split_multi_gene_records {
    my $self = shift;

    my @keep_records = ();
    foreach my $mfr ($self->records) {

        # If there are multiple values in Hugo_Symbol then there should
        # be matching values in Variation_Classification
        my @genes = split /;/, $mfr->Hugo_Symbol;
        my @vars  = split /;/, $mfr->Variant_Classification;
        croak "Hugo_Symbol / Variant_Classification count mismatch\n"
            unless (scalar(@genes) == scalar(@vars));

        foreach my $ctr (0..$#genes) {
            my $var  = $vars[$ctr];
            my $gene = $genes[$ctr];

            my $mfr_clone = dclone($mfr);
            $mfr_clone->Hugo_Symbol( $gene );
            $mfr_clone->Variant_Classification( $var );
            push @keep_records, $mfr_clone;
        }
    }

    $self->{records} = \@keep_records;
}


sub write_maf_file {
    my $self   = shift;
    my %params = @_;

    croak "file parameter must be supplied to write_maf_file()\n"
        unless (exists $params{file});
    my $version = (exists $params{version}) ? $params{version} : 2.2;

    # Use the first record to look for extra fields
    my @records = $self->records;
    my @extra_fields = $records[0]->extra_fields;

    # Get the detailed output file ready
    my $mafout = QCMG::IO::MafWriter->new(
                     filename     => $params{file},
                     extra_fields => \@extra_fields,
                     version      => $params{version} );

    my $rctr = 0;
    if ($params{sort_by_genes}) {
        my @genes = @{ $params{sort_by_genes} };
        foreach my $gene (@genes) {
            foreach my $ctr (0..$#records) {
                # We are using delete not splice so need undef check
                next unless defined $records[$ctr];
                if ($gene == $records[$ctr]->Hugo_Symbol) {
                     $mafout->write( $records[$ctr] );
                     $rctr++;
                     delete $records[$ctr]
                }
            }
        }

        # Now write out any records that are left, i.e. any records that
        # did not match any of the sort_by genes
        foreach my $ctr (0..$#records) {
            next unless defined $records[$ctr];
            $mafout->write( $records[$ctr] );
        }
    }
    else {
        foreach my $mfr (@records) {
            $mafout->write( $mfr );
            $rctr++;
        }
    }

    qlogprint( "wrote $rctr MAF records to ", $params{file}, "\n" );

    $mafout->filehandle->close;
}


sub categorise_variants {
    my $self = shift;
    qlogprint "categorising variants - nocode\n";
    return $self->_categorise_variants( 0 );
}


sub categorise_variants_kassahn {
    my $self = shift;
    qlogprint "categorising variants - kassahn\n";
    return $self->_categorise_variants( 1 );
}


sub categorise_variants_stransky {
    my $self = shift;
    qlogprint "categorising variants - stransky\n";
    return $self->_categorise_variants( 2 );
}


sub categorise_variants_jones {
    my $self = shift;
    qlogprint "categorising variants - jones\n";
    return $self->_categorise_variants( 3 );
}


sub categorise_variants_quiddell {
    my $self = shift;
    qlogprint "categorising variants - quiddell\n";
    return $self->_categorise_variants( 4 );
}


sub categorise_variants_synonymous {
    my $self = shift;
    qlogprint "categorising variants - synonymous\n";
    return $self->_categorise_variants( 5 );
}


sub _categorise_variants {
    my $self = shift;
    my $mode = shift;

    # Filter and deconvolute MAFs so there is one variant per gene per
    # patient and it is the variant with the worst consequence
    my $new_mfc = $self->filter_by_worst_consequence;

    my $rh_summaries = {};
    my $vs = QCMG::Variants::VariantSummary->new(
                 verbose => $self->verbose );

    foreach my $mfr ($new_mfc->records) {
        my $patient = $mfr->Tumor_Sample_Barcode;
        my $variant = $mfr->Variant_Type;
        my $gene    = $mfr->Hugo_Symbol;

        #print "< $mode $patient $gene $variant\n";

        if ($mode == 0) {
            # mode 0 is no recoding so do nothing
        }
        elsif ($mode == 1) {
            $variant = $mfr->categorise_kassahn;
        }
        elsif ($mode == 2) {
            $variant = $mfr->categorise_stransky;
        }
        elsif ($mode == 3) {
            $variant = $mfr->categorise_jones;
        }
        elsif ($mode == 4) {
            $variant = $mfr->categorise_quiddell;
        }
        elsif ($mode == 5) {
            $variant = $mfr->categorise_synonymous;
        }
        else {
            confess "unknown mode [$mode]";
        }

        #print "> $mode $patient $gene $variant\n";

        # Cope with variants that cannot be uncategorised under the
        # requested scheme
        next unless defined $variant;

        # This block is not needed now that we are returning a
        # QCMG::Variants::VariantSummary object.
        #$rh_summaries->{'By_Gene'}->{ $gene }++;
        #$rh_summaries->{'By_Gene_Variant'}->{ $gene }->{ $variant }++;
        #$rh_summaries->{'By_Variant'}->{ $variant }++;
        #$rh_summaries->{'By_Patient_Variant'}->{ $patient }->{ $variant }++;
        #$rh_summaries->{'By_Patient'}->{ $patient }++;
        #$rh_summaries->{'By_Patient_Gene'}->{$patient}->{$gene} = $variant;
        
        $vs->add_variant( patient => $patient,
                          gene    => $gene,
                          variant => $variant );
    }

    #return $rh_summaries;
    return $vs;
}


# New or external MAFs might contain classifications
# that are not in our scheme.  If so, warn about it and
# do nothing, i.e. assign a priority of 98 to the
# unknown classification so it is effectively ignored.

sub maf_var_priority {
    my $var = shift;
    if (exists $MAF_VAR_PRIORITY->{$var}) {
        return $MAF_VAR_PRIORITY->{$var};
    }
    else {
        return 98;
    }
}


1;

__END__


=head1 NAME

QCMG::IO::MafRecordCollection - Collection of MafRecord objects


=head1 SYNOPSIS

 use QCMG::IO::MafRecordCollection;


=head1 DESCRIPTION

This module provides operations on a collection of QCMG::IO::MafRecord object.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $mrc = QCMG::IO::MafRecordCollection->new( verbose => 1 );

There are no compulsory options to new().  Verbose is set to 0 by
default and any non-zero integer will turn on additional logging.

=item B<add_record()>

Add an object of type QCMG::IO::MafRecord to the collection.

=item B<add_records()>

Takes an arrayref of objects of type QCMG::IO::MafRecord to be addede to
the collection.

=item B<records()>

Returns an array containing the QCMG::IO::MafRecord objects contained in
the collection.

=item B<record_count()>

Number of MafRecords in the collection.

=item B<split_multi_gene_records()>

A single MAF record can relate to multiple transcripts from the same
gene (possibly with different consequences) or to multiple genes if
there are gene on both strands that cover the variant position.  It can
simplify some analyses if these multi-gene MAF records are split into
multiple MAF records and this function does that.

=back

=head2 FILTER METHODS

There are a number of filter methods and they all have the same basic
operation - they return a new QCMG::IO::MafRecordCollection object with
some records added/deleted/modified with respect to the original
collection.  It is very important to note that for any MafRecords shared
between the original and new collections, those objects are shared, i.e.
there is no cloning of the underlying objects.  This is critical to
remember because if you subsequently modify any of the shared objects
they will change in both of the collections and in any other filtered
collections derived from either collection.

=over

=item B<filter_by_genes()>

 $mrc->filter_by_genes( qw( BRCA1 BRCA2 TP52 KRAS ) );

Pass in an arrayref of gene names and any MAF records that do not match
one of the supplied genes are discarded. There is no wildcarding, case
insensitivity or pattern matching - this filter is based on perfect
matching of the MAF Hugo_Symbol field to the supplied strings (genes).

=item B<filter_by_normal_ids()>

 $mrc->filter_by_normal_ids( qw( APGI_2057 APGI_1992 ) );

This function works as filter_by_genes() does but it matches against
the MAF Matched_Norm_Sample_Barcode field.

=item B<filter_by_tumour_ids()>

 $mrc->filter_by_tumour_ids( qw( APGI_2057 APGI_1992 ) );

This function works as filter_by_normal_id() does but it matches
against the MAF Tumor_Sample_Barcode field.

=item B<filter_by_worst_consequence()>

 $mrc->filter_by_worst_consequence()

There could be more than one variant called for a given gene and
patient because of multiple transcripts at the same position or
multiple variants at different positions.  To simplify some analyses, it
may be convenient to only keep, for each gene, 
the variant with the worst consequence.  Regardless of 
whether we are going to use the Stransky categorisation or not,
we will choose "worst consequence" based on the $MAF_VAR_PRIORITY scale.

This subroutine has a very big side-effect - it cannot cope with
multi-gene variant records so it calls split_multi_gene_records().

=item B<create_variant_matrix()>

 $mrc->create_variant_matrix()

This routine calls B<filter_by_worst_consequence()> which has some
serious side-effects so the MafRecordCollection may not be useful for
other work once this routine has been called since some records may 
have been split and others may have been dropped.

=item B<categorise_variants_kassahn()>

=item B<categorise_variants_stransky()>

=item B<categorise_variants_jones()>

=item B<categorise_variants_synonymous()>

=item B<categorise_variants_quiddell()>

These routines all categorise variants according to different schemes and
return an instance of the QCMG::Variants::VariantSummary class.
In all cases they collapse the
variants so there is only one per gene per patient so if a patient has
multiple variants in a single gene the "worst" is selected using the
B<filter_by_worst_consequence()> method.

Details of the recoding schemes are documented in the script
QCMGPerl/distros/seqtools/src/qmaftools.pl.

B<categorise_variants_quiddell()> is a special case because it is not an
implementation of the full quiddell categorisation scheme which also
requires CNV (and SV) data.  The kassahn, stransky and jones schemes are
all complete here because they only require SNP and indel information
which should all be in the same MAF.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: MafRecordCollection.pm 4663 2014-07-24 06:39:00Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2014

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
