package QCMG::Annotate::EnsemblConsequences;

###########################################################################
#
#  Module:   QCMG::Annotate::EnsemblConsequences.pm
#  Creator:  John V Pearson
#  Created:  2012-11-28
#
#  Module for generic annotation using local Ensembl API and database
#
#  $Id: EnsemblConsequences.pm 4660 2014-07-23 12:18:43Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::Annotate::Util qw( load_ensembl_API_modules );
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4660 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: EnsemblConsequences.pm 4660 2014-07-23 12:18:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class = shift;
    my %opts  = @_;

    my $self = { ensver    => $opts{ensver}   || '70',
                 organism  => $opts{organism} || 'human',
                 db_host   => $opts{db_host}  || '10.160.72.25',
                 db_user   => $opts{db_user}  || 'ensembl-user',
                 verbose   => $opts{verbose}  || 0,
                _slice     => undef,
                _slice_chr => '',
                _count     => 0,
                _chroms    => {},
                _conseqs   => {},
               };
    bless $self, $class;

    # Get those pesky ensembl API modules into the search path
    load_ensembl_API_modules( $self->ensver );

    # Bio::EnsEMBL::Registry::load_registry_from_db() expects the latin
    # species name so we'll set it here.
    my %species = ();
    if ($self->organism =~ /human/i) {
        $species{latin} = 'homo sapiens';
        $species{adaptor} = 'human';
    }
    elsif ($self->organism =~ /mouse/i) {
        $species{latin} = 'mus musculus';
        $species{adaptor} = 'mouse';
    }
    else {
        die 'Organism [', $self->organism, "] is not valid\n";
    }

    my %reg_params = ( -host       => $self->db_host,
                       -user       => $self->db_user,
                       -verbose    => $self->verbose,
                       -species    => $species{latin},
                       -db_version => $self->ensver );

    qlogprint( "Initialising Bio::EnsEMBL::Registry from database using params:\n" );
    foreach my $key (sort keys %reg_params) {
        qlogprint( "  $key : ", $reg_params{$key}, "\n" );
    }

    # Ensembl API magic:
    # 1. Load a registry via a class method from Bio::EnsEMBL::Registry
    # 2. Get a Bio::EnsEMBL::Variation::DBSQL::VariationFeatureAdaptor
    # 3. Get a Bio::EnsEMBL::DBSQL::SliceAdaptor

    my $reg = 'Bio::EnsEMBL::Registry';
    $reg->load_registry_from_db( %reg_params );
    my $vfa = $reg->get_adaptor($species{adaptor}, 'variation', 'variationfeature');
    my $sa = $reg->get_adaptor($species{adaptor}, 'core', 'slice');

    $self->{ens_vfa} = $vfa;
    $self->{ens_sa}  = $sa;
    
    return $self;
}


sub get_consequences {
    my $self      = shift;
    my $tr2domain = shift;  # from Util::transcript_to_domain_lookup()
    my $tr2geneid = shift;  # from Util::transcript_to_geneid_and_symbol_lookups()
    my $tr2symbol = shift;  # from Util::transcript_to_geneid_and_symbol_lookups()
    my $ra_recs   = shift;  # EnsemblConsequenceRecord(s);

    # We expect to be passed an array of records where each record is a
    # hash of the form shown below AND where the array is sorted by
    # chromosome.  If it's not sorted the annotation should still work
    # but will be *MUCH* slower as each chromosome change will force
    # retrieval of a new "slice".
    #
    # $rec = { chrom       => 'chr1',
    #          chrom_start => 1234567
    #          chrom_end   => 1234567
    #          mutation    => 'A/B'
    #          var_name    => 'GEMM_0101_indel_3' }
    #
    # Notes:
    # 1. $rec->{mutation} : the first allele should be the reference
    # 2. $rec->{var_name} : for somatic records this would normally be
    #                       the mutation_id and for germline it would be
    #                       the variation_id

    # And now do the annotation and output
 
    #my $count        = 0;
    #my %counts       = ();
    #my %consequences = ();

    # We need to save chr and slice across invocations of this method
    # because getting a new Ensembl slice is expensive so if this method
    # is called for each SNV in a large file, we can't afford all of the
    # slice retrieval ops.  This is less of a problem is large arrays of
    # SNvs are annotated at once but this costs lots of memory to store
    # all of the SNVs so that can also be unworkable.

    my $slice        = $self->_slice;
    my $chr          = $self->_slice_chr;

    foreach my $rec (@{$ra_recs}) {

        # Set ensembl version for annotated record based on the version
        # of this annotator
        $rec->ensver( $self->ensver );

        # fetch_by_region() can't seem to cope with chromosome names
        # that include 'chr' so we will strip them out (particularly
        # required for mouse where our reference includes the 'chr').
        my $chrom = $rec->chrom;
        $chrom =~ s/^chr//;
        
        # From the 2013 ICGC benchmarking exercise, it looks like the
        # ensembl API may be barfing if the sequence name is chrM (as
        # used in the benchmark BAMs but not if it looks like chrMT (as
        # it does in our BAMs.  So ...
        $chrom = 'MT' if $chrom eq 'M';

        $self->{_count}++;
        $self->{_chroms}->{$chrom}++;
        qlogprint( 'Annotating record ',$self->{_count},"\n")
            if ($self->{_count} % 1000 == 0);

        # Every chromosome needs a "slice" so watch for a change in chromosome
        if ($chrom ne $chr) {
            $chr = $chrom;
            $self->_slice_chr( $chr );
            # Get a slice for the new feature to be attached to
            qlogprint( "Fetching new EnsEMBL 'slice' for chromosome $chr\n" );
            $slice = $self->{ens_sa}->fetch_by_region('chromosome',$chr);
            $self->_slice( $slice );
        }

        my %var_feat_params = (
            -verbose        => $self->verbose,
            -start          => $rec->chrom_start,
            -end            => $rec->chrom_end,
            -slice          => $slice,           # the variation must be attached to a slice
            -allele_string  => $rec->mutation,   # the first allele should be the reference
            -strand         => 1,
            -map_weight     => 1,
            -adaptor        => $self->{ens_vfa},
            -variation_name => $rec->var_name );

        my $new_vf = Bio::EnsEMBL::Variation::VariationFeature->new( %var_feat_params );
           
        # We need to eval this block because it has a tendency to barf under
        # all sorts of ill-defined conditions and we would much rather just
        # note that the SNP in question is "not annotatable" and soldier on.

        eval {
            my $ra_consequences = $new_vf->get_all_TranscriptVariations();

            while (my $con = shift @{$ra_consequences}) {
                my $ra_strings= $con->consequence_type;
                while (my $string = shift @{$ra_strings}) {

                    $self->{_conseqs}->{ $chr }++;

                    #get amino acid change in DCC format
                    my $aa_ch     = $con->pep_allele_string;
                    my $aa_pos    = '';
                    my $aa_change = '';
#{
#    no warnings;
#    print $rec->var_name,
#          " $string:  aa_change: $aa_change  aa_pos: $aa_pos aa_ch: $aa_ch\n"; 
#}
                    if (defined $aa_ch and $aa_ch=~/\//) {
                        $aa_pos    = $con->translation_start;
                        my @fields = split(/\//,$aa_ch);
                        $aa_change = $fields[0].$aa_pos.$fields[1];
                    }
                    elsif (defined $aa_ch and $aa_ch=~/[A-Z]/) {
                        $aa_pos    = $con->translation_start;
                        $aa_change = $aa_ch.$aa_pos.$aa_ch;
                    }
                    else {
                        # presumably $aa_ch is undefined
                        $aa_ch = '';
                    }
#{
#    no warnings;
#    print $rec->var_name,
#          " $string:  aa_change: $aa_change  aa_pos: $aa_pos aa_ch: $aa_ch\n"; 
#}

                    #get cds change in DCC format
                    my $cds_pos    = $con->cdna_start;
                    my $cds_change = '';
                    my $transcript = '';
#{
#    no warnings;
#    print $rec->var_name,
#          " $string:  cds_change: $cds_change  cds_pos: $cds_pos  transcript: $transcript\n"; 
#}
                    if (defined $cds_pos and $cds_pos=~/\d/) {
                        $cds_change = $cds_pos . $rec->mutation;
                        #$cds_change =~ s/\//>/;  # change X/Y to X>Y (this line causes problems with the Ensembl annotation. character must be a "/" not ">"
                        $transcript = $con->transcript->stable_id;
                    }
                    elsif ($string eq 'INTERGENIC') {
                        # do nothing
                    }
                    else {
                        $transcript = $con->transcript->stable_id;
                    }
#{
#    no warnings;
#    print $rec->var_name,
#          " $string:  cds_change: $cds_change  cds_pos: $cds_pos  transcript: $transcript\n"; 
#}

                    # DCC currently only reports PFAM domains but it
                    # makes sense here for us to capture all affected
                    # domains and we'll leave the filtering to the caller

                    my @domains = ();
                    if ($aa_pos) {
                        if (exists $tr2domain->{ $transcript } ) {
                            my $ra_domains = $tr2domain->{ $transcript };
                            foreach my $drec (@{ $ra_domains }) {
                                if ( $drec->protein_seq_start <= $aa_pos and
                                     $drec->protein_seq_end   >= $aa_pos ) {
                                    push @domains, { name => $drec->protein_hit_name,
                                                     type => $drec->domain,
                                                     desc => $drec->interpro_label };
                                }
                            }
                        }
                    }

                    my $gene_symbol = '';
                    if (exists $tr2symbol->{ $transcript } ) {
                        $gene_symbol = $tr2symbol->{ $transcript };
                    }

                    my $gene_id = '';
                    if (exists $tr2geneid->{ $transcript } ) {
                        $gene_id = $tr2geneid->{ $transcript };
                    }

                    # Now save this all out
                    my $new_rec = {
                        string        => $string,
                        aa_ch         => $aa_ch,
                        aa_pos        => $aa_pos,
                        aa_change     => $aa_change,
                        cds_change    => $cds_change,
                        transcript    => $transcript,
                        domains       => \@domains,
                        gene_symbol   => $gene_symbol,
                        gene_id       => $gene_id };

                    push @{ $rec->{consequences} }, $new_rec;
                }
            }
        };
        if ($@) {
            # If there was a problem, set consequences to undef
            $rec->{ consequences } = undef;
        }
    }
}


sub report {
    my $self = shift;

    my @annot_chrs = sort keys %{$self->{_chroms}};
    qlogprint( "Annotated SNPs on chromosomes:\n" );
    foreach my $key (@annot_chrs) {
        qlogprint( "  $key : ", $self->{_chroms}->{$key}, "\n" );
    }
    my @conseq_chrs = sort keys %{$self->{_conseqs}};
    qlogprint( "Found consequences on chromosomes:\n" );
    foreach my $key (@conseq_chrs) {
        qlogprint( "  $key : ", $self->{_conseqs}->{$key}, "\n" );
    }
}


sub ensver {
    my $self = shift;
    return $self->{ensver};
}

sub organism {
    my $self = shift;
    return $self->{organism};
}

sub db_host {
    my $self = shift;
    return $self->{db_host};
}

sub db_user {
    my $self = shift;
    return $self->{db_user};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub _slice {
    my $self = shift;
    return $self->{_slice} = shift if @_;
    return $self->{_slice};
}

sub _slice_chr {
    my $self = shift;
    return $self->{_slice_chr} = shift if @_;
    return $self->{_slice_chr};
}


1;

__END__

=head1 NAME

QCMG::Annotate::EnsemblConsequences - Uses local Ensembl API


=head1 SYNOPSIS

 use QCMG::Annotate::EnsemblConsequences;

 my %defaults = ( ensver   => '70',
                  organism => 'human',
                  db_host  => '10.160.72.25',
                  db_user  => 'ensembl-user',
                  verbose  => 0 );
 my $ens = QCMG::Annotate::EnsemblConsequences->new( %defaults );

 $ens->get_consequences();

=head1 DESCRIPTION

This module provides functionality for using the local Ensembl API for
annotation.

This module is based on a series of SNP annotation scripts originally
created by Karin Kassahn and JP and elaborated by Karin to adapt to new
organisms (Mouse etc) and new versions for the EnsEMBL API.  This
version has been rewritten by JP for better diagnostics and to support
the annotation of differnt variant types from different organisms and
different Ensembl versions.

=head1 METHODS


=over

=item B<new()>

=item B<get_consequences()>

=back


=head1 AUTHORS

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=item Karin Kassahn, L<mailto:k.kassahn.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: EnsemblConsequences.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
