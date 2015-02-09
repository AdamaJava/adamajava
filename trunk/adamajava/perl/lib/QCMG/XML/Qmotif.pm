package QCMG::XML::Qmotif;

##############################################################################
#
#  Module:   QCMG::XML:Qmotif.pm
#  Author:   John V Pearson
#  Created:  2014-01-20
#
#  Read qMotif XML report files.
#
#  $Id: Qmotif.pm 4666 2014-07-24 09:03:04Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use XML::LibXML;

use Grz::Util::Log;

use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4666 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Qmotif.pm 4666 2014-07-24 09:03:04Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    croak "You must supply one of file/xmltext/xmlnode to new()"
        unless (exists $params{file} or
                exists $params{xmltext} or
                exists $params{xmlnode});

    my $self = { file               => '',
                 xmltext            => '',
                 xmlnode            => undef,
                 keyvals            => undef,
                 motifs_by_category => undef,
                 reads_by_region    => undef,
                 percents           => undef,
                 verbose            => $params{verbose} || 0 };
    bless $self, $class;

    # Ultimately we need a XML::LibXML::Element but we could have been
    # passed an Element object, a filename or a text blob.  In the latter
    # two cases, we need to create an XML node from the file or text.

    if (exists $params{xmlnode}) {
        my $type = ref($params{xmlnode});
        croak 'xmlnode parameter must refer to a XML::LibXML::Element '.
              "object not [$type]" unless ($type eq 'XML::LibXML::Element');
        my $name = $params{xmlnode}->nodeName;
        croak "xmlnode parameter must be a qmotif element not [$name]\n"
            unless ($name eq 'qmotif');
        $self->{xmlnode} = $params{xmlnode};
    }
    elsif (exists $params{xmltext}) {
        my $xmlnode = undef;
        eval{ $xmlnode = XML::LibXML->load_xml( string => $params{xmltext} ); };
        croak $@ if $@;
        $self->{xmlnode} = $xmlnode;
    }
    elsif (exists $params{file}) {
        my $xmlnode = undef;
        eval{ $xmlnode = XML::LibXML->load_xml( location => $params{file} ); };
        croak $@ if $@;
        $self->{xmlnode} = $xmlnode;
        $self->{file}    = $params{file};
    }
    else {
        confess "Uh oh - should not be able to get here!"
    }

    # Process the XML 
    $self->_initialise;

    return $self;
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub xmlnode {
    my $self = shift;
    return $self->{xmlnode};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub get_value {
    my $self  = shift;
    my $param = shift;
    return exists $self->{keyvals}->{$param} ?
           $self->{keyvals}->{$param} : undef;
}


sub motif_percentages {
    my $self = shift;

    my %motifs_by_category = %{ $self->{motifs_by_category} };
    my @categories = qw( includes unmapped genomic);

    # Get totals by category so we can do percentages later
    my $unmapped_total = 0;
    $unmapped_total += $_ foreach values %{ $motifs_by_category{unmapped} };
    my $includes_total = 0;
    $includes_total += $_ foreach values %{ $motifs_by_category{includes} };
    my $genomic_total = 0;
    $genomic_total += $_ foreach values %{ $motifs_by_category{genomic} };

    my %all_motifs = ();
    my %totals     = ();
    foreach my $category (@categories) {
        foreach my $motif (keys %{ $motifs_by_category{ $category } }) {
            $totals{ $category } += $motifs_by_category{ $category }->{ $motif };
            $all_motifs{ $motif } = 1;
        }
        $totals{overall} += $totals{ $category };
    }

    # Report what percentage each motif represents for each category

    my %percents = ();
    foreach my $motif (keys %all_motifs) {

        # We have to cope with motifs being missing from some categories
        # which includes defending against divide-by-zero errors

        my $count         = 0;
        my $percent       = 0;
        my $overall_count = 0;
        foreach my $category (@categories) {
            $count = exists $motifs_by_category{$category}->{$motif} ?
                     $motifs_by_category{$category}->{$motif} : 0;
            $overall_count += $count;
            $percent = $totals{$category} ?
                       $count / $totals{$category} : 0;
            $percents{ $motif }->{ $category } = $percent;
        }

        # Should never get divide-by-zero for a motif total
        $percents{ $motif }->{ overall } = $overall_count / $totals{overall};
    }
    
    return \%percents;
}


sub log_top_motif_percentages {
    my $self = shift;

    my $rh_percents = $self->motif_percentages;

    # Sort the motifs by overall total
    my @sorted_totals = map { $_->[0] }
                        sort { $b->[1] <=> $a->[1] }
                        map { [ $_, $rh_percents->{$_}->{overall} ] }
                        keys %{ $rh_percents };

    # Print out the tops 10 motifs but cope gracefully if there are
    # fewer than 10 motifs.
    my $last_motif = $#sorted_totals < 9 ? $#sorted_totals : 9;
    for my $i (0..$last_motif) {
        my $motif = $sorted_totals[$i];

        # Some motifs may be missing so we have to set "0" for these or
        # the print statements will WARN which gets messy.
        my $pc_overall  = ( exists $rh_percents->{$motif}->{overall} ) ?
                            $rh_percents->{$motif}->{overall} : 0;
        my $pc_includes = ( exists $rh_percents->{$motif}->{includes} ) ?
                            $rh_percents->{$motif}->{includes} : 0;
        my $pc_unmapped = ( exists $rh_percents->{$motif}->{unmapped} ) ?
                            $rh_percents->{$motif}->{unmapped} : 0;
        my $pc_genomic  = ( exists $rh_percents->{$motif}->{genomic} ) ?
                            $rh_percents->{$motif}->{genomic} : 0;

        glogprint join("\t", $motif,
                             sprintf( "%.3f", $pc_overall ),
                             sprintf( "%.3f", $pc_includes ),
                             sprintf( "%.3f", $pc_unmapped ),
                             sprintf( "%.3f", $pc_genomic ),
                       ), "\n";
    }
}


##############################################################################
#                              Private Methods                               #
##############################################################################


sub _initialise {
    my $self = shift;

    my $file = $self->file;
    my $root = $self->xmlnode;

    glogprint( "parsing XML\n" ) if $self->verbose;

    # Key values from summary element
    my %values = ();
    my @fields = qw ( windowSize cutoff totalReadCount noOfMotifs
                      rawUnmapped rawIncludes rawGenomic
                      scaledUnmapped scaledIncludes scaledGenomic );
    for my $field (@fields) {
        my @nodes = $self->xmlnode->findnodes( "qmotif/summary/counts/$field" );
        my $value = scalar(@nodes) ? $nodes[0]->getAttribute('count') : '';
        $values{ $field } = $value;
    }

    $self->{keyvals} = \%values;

    # Submotifs within the Motifs
    my %submotifs = ();
    my @mnodes = $self->xmlnode->findnodes( "qmotif/motifs/motif" );
    my $ctr = 0;
    foreach my $node (@mnodes) {
        my $id = $node->getAttribute('id');
        my $seq = $node->getAttribute('motif');

        # Count the 60mer submotifs in this motif.  Note that forwards
        # we only look for /...GGG/ and on the reverse, we only look for
        # /CCC.../.  If a read showed a TTAGGG motif but the read had
        # mapped on the reverse strand then the motif should not be
        # counted (see diagram below) because the read in its native
        # form would NOT contain TTAGGG sequences.
        #
        # This is important because we look at the submotifs in both
        # directions on each motif and when we do the tallying, we
        # should only report submotifs as found in the original read so
        # we ignore TTAGGG that occur on the reverse strand and CCCTAA
        # that occur on the forward strand.
        #
        #    TTAGGGTTAGGGTTAGGG ->    Forward read with TTAGGGx3
        #    TTAGGGTTAGGGTTAGGG ->    Forward read as shown in BAM
        #
        # <- GGGATTGGGATTGGGATT       Reverse read with TTAGGGx3
        #    CCCTAACCCTAACCCTAA ->    Reverse read as shown in BAM

        $submotifs{ $id } = { F => {}, R => {} };
        while ($seq =~ /(...GGG)/g) {
            $submotifs{ $id }->{ F }->{ $1 }++;
        }
        while ($seq =~ /(CCC...)/g) {
            $submotifs{ $id }->{ R }->{ $1 }++;
        }
    }
    
    # Motifs in different region types
    my %regions = ();

    # How many reads are in each region
    my %reads_by_region = ();
    # How many of each motif are in each category
    my %motifs_by_category = ();

    my @rnodes = $self->xmlnode->findnodes( "qmotif/regions/region" );
    foreach my $node (@rnodes) {
        my $loc    = $node->getAttribute('chrPos');
        my $stage1 = $node->getAttribute('stage1Cov');
        my $stage2 = $node->getAttribute('stage2Cov');
        my $type   = $node->getAttribute('type');
        my @motifs = $node->findnodes( "motif" );

        $reads_by_region{ $loc } = { chrPos    => $loc,
                                     type      => $type,
                                     stage1Cov => $stage1,
                                     stage2Cov => $stage2 };

        # Do the overall motif tallies.  Depending on the strand and the
        # region category, we take all of the submotifs and tally them
        # up (remembering to multiply by $number);

        foreach my $motif (@motifs) {
            my $id     = $motif->getAttribute('motifRef');
            my $number = $motif->getAttribute('number');
            my $strand = $motif->getAttribute('strand');

            foreach my $motifseq (keys %{$submotifs{ $id }->{ $strand }}) {
                my $count = $submotifs{ $id }->{ $strand }->{ $motifseq } * $number;

                my $f_motif = $motifseq;
                if ($strand eq 'R') {
                    # We will revcomp the reverse strand motifs so all
                    # motif counts will be in the forward sense.
                    $f_motif = _revcomp( $motifseq );
                }

                $motifs_by_category{ $type }->{ $f_motif } +=
                      $submotifs{ $id }->{ $strand }->{ $motifseq } * $number;
                $motifs_by_category{ 'overall' }->{ $f_motif } +=
                      $submotifs{ $id }->{ $strand }->{ $motifseq } * $number;
            }
        }
    }

    $self->{motifs_by_category} = \%motifs_by_category;
    $self->{reads_by_region}    = \%reads_by_region;
}


sub _revcomp {
    my $seq = shift;
    my $revcomp = reverse($seq);
    $revcomp =~ tr/ACGT/TGCA/;
    return $revcomp;
}


1;

__END__


=head1 NAME

QCMG::XML::Qmotif - Perl module for parsing qMotif XML reports


=head1 SYNOPSIS

 use QCMG::XML::Qmotif;

 my $report = QCMG::XML::Qmotif->new( file => 'report.xml' );


=head1 DESCRIPTION


=head1 PUBLIC METHODS

=over

=item B<new()>

=item B<file()>

=item B<verbose()>

=item B<motif_percentages()>

Ruturns a hash keyed on motif showing what percentage of each category
each motif represents.  For example, the following hash shows that
TAAGGG motif is 1.2% of the unmapped category, 1.4% of the genomic and
0.3% of the includes and it is 0.4% of the overall motif count.

 {
     'TAAGGG' => {
                   'unmapped' => '0.0129195524773575',
                   'overall' => '0.00367416619326731',
                   'genomic' => '0.0138067061143984',
                   'includes' => '0.00250065327913046'
                 },
     'TATGGG' => {
                   'unmapped' => '0.00103889184869473',
                   'overall' => '0.000344957672959575',
                   'genomic' => '0.0019723865877712',
                   'includes' => '0.000254696167318843'
                 },
     ...
 }

=item B<log_top_motif_percentages()>

This method will print a summary of the top 10 motifs to the log (file
or STDOUT).  Five columns will be reported:

 1. The Motif
 2. Percent overall
 3. Percent within "Includes" category
 4. Percent within "Unmapped" category
 5. Percent within "Genomic" category

An example output (with leading text on each line elided):

 INFO QCMG::XML::Qmotif - TTAGGG     0.846   0.866   0.608   0.635
 INFO QCMG::XML::Qmotif - GTAGGG     0.044   0.041   0.077   0.074
 INFO QCMG::XML::Qmotif - ATCGGG     0.016   0.017   0.001   0.000
 INFO QCMG::XML::Qmotif - TTGGGG     0.012   0.010   0.038   0.024
 INFO QCMG::XML::Qmotif - ATAGGG     0.012   0.011   0.023   0.012
 INFO QCMG::XML::Qmotif - TAGGGG     0.009   0.008   0.024   0.039
 INFO QCMG::XML::Qmotif - CTAGGG     0.008   0.007   0.017   0.007
 INFO QCMG::XML::Qmotif - TGAGGG     0.007   0.005   0.029   0.014
 INFO QCMG::XML::Qmotif - GTTGGG     0.005   0.003   0.025   0.013
 INFO QCMG::XML::Qmotif - GTGGGG     0.004   0.003   0.021   0.009


=back 


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: Qmotif.pm 4666 2014-07-24 09:03:04Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2014

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
