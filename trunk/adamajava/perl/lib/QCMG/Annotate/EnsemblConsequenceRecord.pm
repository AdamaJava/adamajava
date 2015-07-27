package QCMG::Annotate::EnsemblConsequenceRecord;

###########################################################################
#
#  Module:   QCMG::Annotate::EnsemblConsequenceRecord.pm
#  Creator:  John V Pearson
#  Created:  2012-11-28
#
#  Data container for Ensembl annotated DCC1 records.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Clone qw( clone );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;

use QCMG::Annotate::Util qw( dcc2rec_from_dcc1rec
                             dccqrec_from_dcc1rec
                             load_ensembl_API_modules );
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class = shift;
    my %opts  = @_;

    croak "dcc1 is a mandatory option to EnsemblConsequenceRecord::new()\n"
        unless (exists $opts{dcc1} and defined $opts{dcc1});

    my $self = { chrom       => $opts{chrom}       || '',
                 chrom_start => $opts{chrom_start} || 0,
                 chrom_end   => $opts{chrom_end}   || 0,
                 mutation    => $opts{mutation}    || '',
                 var_name    => $opts{var_name}    || '',
                 ensver      => $opts{ensver}      || undef,
                 dcc1        => $opts{dcc1},
                 verbose     => $opts{verbose}     || 0,
               };

    bless $self, $class;
}


sub as_dcc2_records {
    my $self = shift;

    my $version = $self->dcc1->version;
    $version =~ s/dcc1/dcc2/;

    my @records = ();

    foreach my $conseq (@{ $self->{consequences} }) {

        # Even if there are no domains there is always a single record
        # output so create a suitable record.  If there are domains then
        # we are going to just clone this record over and over to create
        # a record for each domain

        my $dcc2rec = dcc2rec_from_dcc1rec( $self->dcc1, $version );
        $dcc2rec->consequence_type( $conseq->{string} )
            if ($conseq->{string});
        $dcc2rec->gene_affected( $conseq->{gene_id} )
            if ($conseq->{gene_id});
        $dcc2rec->transcript_affected( $conseq->{transcript} )
            if ($conseq->{transcript});
        $dcc2rec->gene_build_version( $self->ensver )
            if ($self->ensver);

        if ($self->dcc1->is_somatic) {
            $dcc2rec->Mutation( $self->mutation ) if $self->mutation;
        }
        elsif ($self->dcc1->is_germline) {
            $dcc2rec->Variant( $self->mutation ) if $self->mutation;
        }
        else {
           croak 'Cannot determine if DCC1 record is somatic or germline';
        }

        if (scalar @{$conseq->{domains}}) {
            # If there are domains then we need one record per domain
            foreach my $domain (@{ $conseq->{domains} }) {
                my $domrec = clone( $dcc2rec );

                if ($self->dcc1->is_somatic) {
                    $domrec->aa_mutation( $conseq->{aa_change} )
                        if ($conseq->{aa_change});
                    $domrec->cds_mutation( $conseq->{cds_change} )
                        if ($conseq->{cds_change});
                }
                elsif ($self->dcc1->is_germline) {
                    # Release 14 changed some germline field names
                    if ($version =~ /_r14$/) {
                        $domrec->aa_variant( $conseq->{aa_change} )
                            if ($conseq->{aa_change});
                        $domrec->cds_variant( $conseq->{cds_change} )
                            if ($conseq->{cds_change});
                    }
                    else {
                        $domrec->aa_variation( $conseq->{aa_change} )
                            if ($conseq->{aa_change});
                        $domrec->cds_variation( $conseq->{cds_change} )
                            if ($conseq->{cds_change});
                    }
                }

                $domrec->protein_domain_affected( $domain->{name} )
                    if ($domain->{name});
                push @records, $domrec;
            }
        }
        else {

            if ($self->dcc1->is_somatic) {
                $dcc2rec->aa_mutation( $conseq->{aa_change} )
                    if ($conseq->{aa_change});
                $dcc2rec->cds_mutation( $conseq->{cds_change} )
                    if ($conseq->{cds_change});
            }
            elsif ($self->dcc1->is_germline) {
                # Release 14 changed some germline field names
                if ($version =~ /_r14$/) {
                    $dcc2rec->aa_variant( $conseq->{aa_change} )
                        if ($conseq->{aa_change});
                    $dcc2rec->cds_variant( $conseq->{cds_change} )
                        if ($conseq->{cds_change});
                }
                else {
                    $dcc2rec->aa_variation( $conseq->{aa_change} )
                        if ($conseq->{aa_change});
                    $dcc2rec->cds_variation( $conseq->{cds_change} )
                        if ($conseq->{cds_change});
                }
            }

            push @records, $dcc2rec;
        }
    }

    return \@records;
}


sub as_dccq_record {
    my $self = shift;

    my $version = $self->dcc1->version;
    $version =~ s/dcc1/dccq/;
    my $dccqrec = dccqrec_from_dcc1rec( $self->dcc1, $version );
    #$dccqrec->Mutation( $self->mutation );
    $dccqrec->gene_build_version( $self->ensver );

    # DCCQ format is a single record with a heap of composite fields so
    # for each composite field we'll need an array that we can push onto
    # as we examine the various consequences and domains.

    my @consequence_type = ();
    my @aa_mutvar = ();
    my @cds_mutvar = ();
    my @protein_domain_affected = ();
    my @gene_affected = ();
    my @gene_symbol = ();
    my @transcript_affected = ();
    my @gene_build_version = ();
    my @All_domains = ();
    my @All_domains_type = ();
    my @All_domains_description = ();

    foreach my $conseq (@{ $self->{consequences} }) {

        # Even if there are no domains there is always a single record
        # output so create a suitable record.  If there ARE domains then
        # we are going to just clone this record over and over to create
        # a record for each domain.

        push @consequence_type,    $conseq->{string} || '';
        push @gene_affected,       $conseq->{gene_id} || '';
        push @transcript_affected, $conseq->{transcript} || '';
        push @gene_symbol,         $conseq->{gene_symbol} || '';

        if (scalar @{$conseq->{domains}}) {

            my @dom_aa_mutvar               = ();
            my @dom_cds_mutvar              = ();
            my @dom_protein_domain_affected = ();
            my @dom_domains                 = ();
            my @dom_domains_type            = ();
            my @dom_domains_desc            = ();

            foreach my $domain (@{ $conseq->{domains} }) {
                push @dom_aa_mutvar,    ($conseq->{aa_change}  ? $conseq->{aa_change}  : '-888');
                push @dom_cds_mutvar,   ($conseq->{cds_change} ? $conseq->{cds_change} : '-888');
                push @dom_protein_domain_affected, $domain->{name};
                push @dom_domains,      $domain->{name};
                push @dom_domains_type, $domain->{type};
                push @dom_domains_desc, $domain->{desc};
            }

            push @aa_mutvar,               join(';',@dom_aa_mutvar);
            push @cds_mutvar,              join(';',@dom_cds_mutvar);
            push @protein_domain_affected, join(';',@dom_protein_domain_affected);
            push @All_domains,             join(';',@dom_domains);
            push @All_domains_type,        join(';',@dom_domains_type);
            push @All_domains_description, join(';',@dom_domains_desc);

        }
        else {
            push @aa_mutvar,  ($conseq->{aa_change}  ? $conseq->{aa_change}  : '-888');
            push @cds_mutvar, ($conseq->{cds_change} ? $conseq->{cds_change} : '-888');
        }

    }

    $dccqrec->consequence_type( join(',',@consequence_type) );
    $dccqrec->protein_domain_affected( join(',',@protein_domain_affected) );
    $dccqrec->gene_affected( join(',',@gene_affected) );
    $dccqrec->gene_symbol( join(',',@gene_symbol) );
    $dccqrec->transcript_affected( join(',',@transcript_affected) );
    $dccqrec->gene_build_version( join(',',@gene_build_version) );
    $dccqrec->All_domains( join(',',@All_domains) );
    $dccqrec->All_domains_type( join(',',@All_domains_type) );
    $dccqrec->All_domains_description( join(',',@All_domains_description) );

    if ($self->dcc1->is_somatic) {
        $dccqrec->aa_mutation( join(',',@aa_mutvar) );
        $dccqrec->cds_mutation( join(',',@cds_mutvar) );
    }
    elsif ($self->dcc1->is_germline) {
        # Release 14 changed some field names
        if ($version =~ /_r14$/) {
            $dccqrec->aa_variant('-888');
            $dccqrec->cds_variant('-888');
        }
        else {
            $dccqrec->aa_variation('-888');
            $dccqrec->cds_variation('-888');
        }
    }
    else {
       croak 'Cannot determine if DCC1 record is somatic or germline';
    }

    return $dccqrec;
}


sub chrom {
    my $self = shift;
    return $self->{chrom};
}

sub chrom_start {
    my $self = shift;
    return $self->{chrom_start};
}

sub chrom_end {
    my $self = shift;
    return $self->{chrom_end};
}

sub mutation {
    my $self = shift;
    return $self->{mutation};
}

sub var_name {
    my $self = shift;
    return $self->{var_name};
}

sub ensver {
    my $self = shift;
    return $self->{ensver} = shift if @_;
    return $self->{ensver};
}

sub dcc1 {
    my $self = shift;
    return $self->{dcc1};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}


1;

__END__

=head1 NAME

QCMG::Annotate::EnsemblConsequenceRecord - Data container


=head1 SYNOPSIS

 use QCMG::Annotate::EnsemblConsequenceRecord;

 my $ecr = QCMG::Annotate::EnsemblConsequences->new( 
                chrom       => 1
                chrom_start => 1000
                chrom_end   => 1000
                mutation    => 'A/C',
                var_name    => 'GEMM_1001_SNP_17',
                dcc1        => $dcc1rec,
                verbose     => 1 );
 my @dcc2recs = $ecr->as_dcc2_records;


=head1 DESCRIPTION

This module provides a convenience data structure to facilitate Ensembl
API annotation of DCC1 records.  It also handle post-annotation creation
of suitable DCCQ and DCC2 records.


=head1 METHODS


=over

=item B<new()>

=item B<as_dcc2_records()>

=back


=head1 AUTHORS

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=item Karin Kassahn, L<mailto:k.kassahn.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


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
