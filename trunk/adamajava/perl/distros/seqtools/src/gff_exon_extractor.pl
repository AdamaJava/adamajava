#!/usr/bin/perl -w

##############################################################################
#
#  Program:  gff_exon_extractor.pl
#  Author:   John V Pearson
#  Created:  2011-09-13
#
#  Take a GFF of gene models, pull out the exons, extend each splice
#  site by 2 bases and write out to a file.
#
#  Example files in GFF and GTF format:
#  /panfs/home/kkassahn/Ensembl_v55/Ensembl_v55_exons.gff
#  /panfs/share/genomes/WT.reference/Homo_sapiens.GRCh37.55.NCBI.gtf
#
#  $Id: gff_exon_extractor.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;
use Carp qw( carp croak );

use QCMG::IO::GffReader;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE %CHR_E2I %CHR_AG @AG_ORDER);

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: gff_exon_extractor.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

%CHR_E2I = (
             'HSCHR1_RANDOM_CTG5'   => 'GL000191.1',
             'HSCHR1_RANDOM_CTG12'  => 'GL000192.1',
             'HSCHR4_RANDOM_CTG2'   => 'GL000193.1',
             'HSCHR4_RANDOM_CTG3'   => 'GL000194.1',
             'HSCHR7_RANDOM_CTG1'   => 'GL000195.1',
             'HSCHR8_RANDOM_CTG1'   => 'GL000196.1',
             'HSCHR8_RANDOM_CTG4'   => 'GL000197.1',
             'HSCHR9_RANDOM_CTG1'   => 'GL000198.1',
             'HSCHR9_RANDOM_CTG2'   => 'GL000199.1',
             'HSCHR9_RANDOM_CTG4'   => 'GL000200.1',
             'HSCHR9_RANDOM_CTG5'   => 'GL000201.1',
             'HSCHR11_RANDOM_CTG2'  => 'GL000202.1',
             'HSCHR17_RANDOM_CTG1'  => 'GL000203.1',
             'HSCHR17_RANDOM_CTG2'  => 'GL000204.1',
             'HSCHR17_RANDOM_CTG3'  => 'GL000205.1',
             'HSCHR17_RANDOM_CTG4'  => 'GL000206.1',
             'HSCHR18_RANDOM_CTG1'  => 'GL000207.1',
             'HSCHR19_RANDOM_CTG1'  => 'GL000208.1',
             'HSCHR19_RANDOM_CTG2'  => 'GL000209.1',
             'HSCHR21_RANDOM_CTG9'  => 'GL000210.1',
             'HSCHRUN_RANDOM_CTG19' => 'GL000226.1',
             'HSCHRUN_RANDOM_CTG22' => 'GL000229.1',
             'HSCHRUN_RANDOM_CTG24' => 'GL000231.1',
             'HSCHRUN_RANDOM_CTG32' => 'GL000239.1',
             'HSCHRUN_RANDOM_CTG28' => 'GL000235.1',
             'HSCHRUN_RANDOM_CTG40' => 'GL000247.1',
             'HSCHRUN_RANDOM_CTG38' => 'GL000245.1',
             'HSCHRUN_RANDOM_CTG39' => 'GL000246.1',
             'HSCHRUN_RANDOM_CTG42' => 'GL000249.1',
             'HSCHRUN_RANDOM_CTG41' => 'GL000248.1',
             'HSCHRUN_RANDOM_CTG37' => 'GL000244.1',
             'HSCHRUN_RANDOM_CTG31' => 'GL000238.1',
             'HSCHRUN_RANDOM_CTG27' => 'GL000234.1',
             'HSCHRUN_RANDOM_CTG25' => 'GL000232.1',
             'HSCHRUN_RANDOM_CTG33' => 'GL000240.1',
             'HSCHRUN_RANDOM_CTG29' => 'GL000236.1',
             'HSCHRUN_RANDOM_CTG34' => 'GL000241.1',
             'HSCHRUN_RANDOM_CTG36' => 'GL000243.1',
             'HSCHRUN_RANDOM_CTG35' => 'GL000242.1',
             'HSCHRUN_RANDOM_CTG23' => 'GL000230.1',
             'HSCHRUN_RANDOM_CTG30' => 'GL000237.1',
             'HSCHRUN_RANDOM_CTG26' => 'GL000233.1',
             'HSCHRUN_RANDOM_CTG20' => 'GL000227.1',
             'HSCHRUN_RANDOM_CTG21' => 'GL000228.1',
             'HSCHRUN_RANDOM_CTG4'  => 'GL000214.1',
             'HSCHRUN_RANDOM_CTG13' => 'GL000221.1',
             'HSCHRUN_RANDOM_CTG9'  => 'GL000218.1',
             'HSCHRUN_RANDOM_CTG11' => 'GL000220.1',
             'HSCHRUN_RANDOM_CTG3'  => 'GL000213.1',
             'HSCHRUN_RANDOM_CTG1'  => 'GL000211.1',
             'HSCHRUN_RANDOM_CTG7'  => 'GL000217.1',
             'HSCHRUN_RANDOM_CTG6'  => 'GL000216.1',
             'HSCHRUN_RANDOM_CTG5'  => 'GL000215.1',
             'HSCHRUN_RANDOM_CTG10' => 'GL000219.1',
             'HSCHRUN_RANDOM_CTG16' => 'GL000224.1',
             'HSCHRUN_RANDOM_CTG15' => 'GL000223.1',
             'HSCHRUN_RANDOM_CTG2'  => 'GL000212.1',
             'HSCHRUN_RANDOM_CTG14' => 'GL000222.1',
             'HSCHRUN_RANDOM_CTG17' => 'GL000225.1',
             '1'                    => 'chr1',
             '2'                    => 'chr2',
             '3'                    => 'chr3',
             '4'                    => 'chr4',
             '5'                    => 'chr5',
             '6'                    => 'chr6',
             '7'                    => 'chr7',
             '8'                    => 'chr8',
             '9'                    => 'chr9',
             '10'                   => 'chr10',
             '11'                   => 'chr11',
             '12'                   => 'chr12',
             '13'                   => 'chr13',
             '14'                   => 'chr14',
             '15'                   => 'chr15',
             '16'                   => 'chr16',
             '17'                   => 'chr17',
             '18'                   => 'chr18',
             '19'                   => 'chr19',
             '20'                   => 'chr20',
             '21'                   => 'chr21',
             '22'                   => 'chr22',
             'MT'                   => 'chrMT',
             'X'                    => 'chrX',
             'Y'                    => 'chrY' );

%CHR_AG = (
             'HSCHR1_RANDOM_CTG5'   => 'chr1_gl000191_random',
             'HSCHR1_RANDOM_CTG12'  => 'chr1_gf000192_random',
             'HSCHR4_RANDOM_CTG2'   => 'chr4_gl000193_random',
             'HSCHR4_RANDOM_CTG3'   => 'chr4_gl000194_random',
             'HSCHR7_RANDOM_CTG1'   => 'chr7_gl000195_random',
             'HSCHR8_RANDOM_CTG1'   => 'chr8_gl000196_random',
             'HSCHR8_RANDOM_CTG4'   => 'chr8_gl000197_random',
             'HSCHR9_RANDOM_CTG1'   => 'chr9_gl000198_random',
             'HSCHR9_RANDOM_CTG2'   => 'chr9_gl000199_random',
             'HSCHR9_RANDOM_CTG4'   => 'chr9_gl000200_random',
             'HSCHR9_RANDOM_CTG5'   => 'chr9_gl000201_random',
             'HSCHR11_RANDOM_CTG2'  => 'chr11_gl000202_random',
             'HSCHR17_RANDOM_CTG1'  => 'chr17_gl000203_random',
             'HSCHR17_RANDOM_CTG2'  => 'chr17_gl000204_random',
             'HSCHR17_RANDOM_CTG3'  => 'chr17_gl000205_random',
             'HSCHR17_RANDOM_CTG4'  => 'chr17_gl000206_random',
             'HSCHR18_RANDOM_CTG1'  => 'chr18_gl000207_random',
             'HSCHR19_RANDOM_CTG1'  => 'chr19_gl000208_random',
             'HSCHR19_RANDOM_CTG2'  => 'chr19_gl000209_random',
             'HSCHR21_RANDOM_CTG9'  => 'chr20_gl000210_random',
             'HSCHRUN_RANDOM_CTG19' => 'chrUn_gl000226',
             'HSCHRUN_RANDOM_CTG22' => 'chrUn_gl000229',
             'HSCHRUN_RANDOM_CTG24' => 'chrUn_gl000231',
             'HSCHRUN_RANDOM_CTG32' => 'chrUn_gl000239',
             'HSCHRUN_RANDOM_CTG28' => 'chrUn_gl000235',
             'HSCHRUN_RANDOM_CTG40' => 'chrUn_gl000247',
             'HSCHRUN_RANDOM_CTG38' => 'chrUn_gl000245',
             'HSCHRUN_RANDOM_CTG39' => 'chrUn_gl000246',
             'HSCHRUN_RANDOM_CTG42' => 'chrUn_gl000249',
             'HSCHRUN_RANDOM_CTG41' => 'chrUn_gl000248',
             'HSCHRUN_RANDOM_CTG37' => 'chrUn_gl000244',
             'HSCHRUN_RANDOM_CTG31' => 'chrUn_gl000238',
             'HSCHRUN_RANDOM_CTG27' => 'chrUn_gl000234',
             'HSCHRUN_RANDOM_CTG25' => 'chrUn_gl000232',
             'HSCHRUN_RANDOM_CTG33' => 'chrUn_gl000240',
             'HSCHRUN_RANDOM_CTG29' => 'chrUn_gl000236',
             'HSCHRUN_RANDOM_CTG34' => 'chrUn_gl000241',
             'HSCHRUN_RANDOM_CTG36' => 'chrUn_gl000243',
             'HSCHRUN_RANDOM_CTG35' => 'chrUn_gl000242',
             'HSCHRUN_RANDOM_CTG23' => 'chrUn_gl000230',
             'HSCHRUN_RANDOM_CTG30' => 'chrUn_gl000237',
             'HSCHRUN_RANDOM_CTG26' => 'chrUn_gl000233',
             'HSCHRUN_RANDOM_CTG20' => 'chrUn_gl000227',
             'HSCHRUN_RANDOM_CTG21' => 'chrUn_gl000228',
             'HSCHRUN_RANDOM_CTG4'  => 'chrUn_gl000214',
             'HSCHRUN_RANDOM_CTG13' => 'chrUn_gl000221',
             'HSCHRUN_RANDOM_CTG9'  => 'chrUn_gl000218',
             'HSCHRUN_RANDOM_CTG11' => 'chrUn_gl000220',
             'HSCHRUN_RANDOM_CTG3'  => 'chrUn_gl000213',
             'HSCHRUN_RANDOM_CTG1'  => 'chrUn_gl000211',
             'HSCHRUN_RANDOM_CTG7'  => 'chrUn_gl000217',
             'HSCHRUN_RANDOM_CTG6'  => 'chrUn_gl000216',
             'HSCHRUN_RANDOM_CTG5'  => 'chrUn_gl000215',
             'HSCHRUN_RANDOM_CTG10' => 'chrUn_gl000219',
             'HSCHRUN_RANDOM_CTG16' => 'chrUn_gl000224',
             'HSCHRUN_RANDOM_CTG15' => 'chrUn_gl000223',
             'HSCHRUN_RANDOM_CTG2'  => 'chrUn_gl000212',
             'HSCHRUN_RANDOM_CTG14' => 'chrUn_gl000222',
             'HSCHRUN_RANDOM_CTG17' => 'chrUn_gl000225',
             '1'                    => 'chr1',
             '2'                    => 'chr2',
             '3'                    => 'chr3',
             '4'                    => 'chr4',
             '5'                    => 'chr5',
             '6'                    => 'chr6',
             '7'                    => 'chr7',
             '8'                    => 'chr8',
             '9'                    => 'chr9',
             '10'                   => 'chr10',
             '11'                   => 'chr11',
             '12'                   => 'chr12',
             '13'                   => 'chr13',
             '14'                   => 'chr14',
             '15'                   => 'chr15',
             '16'                   => 'chr16',
             '17'                   => 'chr17',
             '18'                   => 'chr18',
             '19'                   => 'chr19',
             '20'                   => 'chr20',
             '21'                   => 'chr21',
             '22'                   => 'chr22',
             'MT'                   => 'chrMT',
             'X'                    => 'chrX',
             'Y'                    => 'chrY' );

@AG_ORDER = ( qw{
chr1
chr1_gl000191_random
chr1_gl000192_random
chr2
chr3
chr4
chr4_gl000194_random
chr5
chr6
chr7
chr8
chr8_gl000196_random
chr8_gl000197_random
chr9
chr9_gl000201_random
chr10
chr11
chr12
chr13
chr14
chr15
chr16
chr17
chr17_gl000205_random
chr18
chr19
chr19_gl000209_random
chr20
chr21
chr22
chrX
chrY
chrUn_gl000238
chrUn_gl000231
chrUn_gl000237
chrUn_gl000236
chrUn_gl000211
chrUn_gl000213
chrUn_gl000212
chrUn_gl000215
chrUn_gl000214
chrUn_gl000217
chrUn_gl000216
chrUn_gl000219
chrUn_gl000218
chrUn_gl000246
chrUn_gl000228
chrUn_gl000229
chrUn_gl000224
chrUn_gl000227
chrUn_gl000220
chrUn_gl000221
chrUn_gl000223
chrUn_gl000249
chrUn_gl000222
chrUn_gl000244
chrUn_gl000242
chrUn_gl000243
chrUn_gl000240
chrUn_gl000241 } );



###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile     = '';
    my $outfile    = 'gff_exon_extractor_out.gff';
    my $extension  = 2;
    my $tmpdir     = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
           'i|infile=s'           => \$infile,        # -i
           'o|outfile=s'          => \$outfile,       # -o
           'x|extension=s'        => \$extension,     # -x
           't|tmpdir=s'           => \$tmpdir,        # -t
           'v|verbose+'           => \$VERBOSE,       # -v
           'version!'             => \$VERSION,       # --version
           'h|help|?'             => \$help,          # -?
           'man|m'                => \$man            # -m
           );

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$SVNID\n";
        exit;
    }

    print( "\ngff_exon_extractor.pl  v$REVISION  [" . localtime() . "]\n",
           "   infile        $infile\n",
           "   outfile       $outfile\n",
           "   extension     $extension\n",
           "   tmpdir        $tmpdir\n",
           "   verbose       $VERBOSE\n\n" ) if $VERBOSE;

    my $rh_exons = read_and_process_exons( $infile, $extension, $tmpdir );
    write_gff_file( $rh_exons, $outfile );

    print '['.localtime()."]  Finished.\n" if $VERBOSE;
}


sub read_and_process_exons {
    my $infile    = shift;
    my $extension = shift;
    my $tmpdir    = shift;

    # Create new GffReader object
    my $reader = QCMG::IO::GffReader->new( filename => $infile );
    
    print '['.localtime()."]  Opening file $infile\n" if $VERBOSE;

    my %gffs = ();

    my $ectr = 0;
    my $rctr = 0;
    while (my $gff = $reader->next_record) {
        $rctr++;
        # We only want exons
        next unless $gff->feature =~ /exon/i;
        # Rename Ensembl sequence names to GRCh37_ICGC_standard_v2 names
        my $seq = ensembl_to_agilent( $gff->seqname );
        next unless defined $seq;
        $gff->seqname( $seq );
        push @{ $gffs{ $gff->seqname } }, $gff;
        $ectr++;
    }
    print '['.localtime()."]    $rctr records read, $ectr exons\n"
        if $VERBOSE;

    # Sorting should be by gene_id (from attributes)
    # then within a gene, by exon start,
    # then collapse any exons that overlap
    # then resort the collection of genes by the start of the first exons
    # then output exons in order with 2bp extension on start and end of
    #   each exon except for start of first and end of last

    foreach my $seq (sort keys %gffs) {
        print '['.localtime()."]    sorting $seq ...\n" if $VERBOSE;
        my %genes = ();

        # Group the exons by gene_id
        foreach my $gff (@{ $gffs{$seq} }) {
            push @{ $genes{ $gff->attrib( 'gene_id' ) } }, $gff;
        }

        # Sort the exons within each gene
        foreach my $gene (keys %genes) {
            my @sorted_exons = map  { $_->[1] }
                               sort { $a->[0] <=> $b->[0] }
                               map  { [ $_->start, $_ ] }
                               @{ $genes{$gene} };
            $genes{$gene} = \@sorted_exons;
        }

        # Sort the genes by the start of the first exons
        my @unsorted_genes = values %genes;
        my @sorted_genes = map  { $_->[1] }
                           sort { $a->[0] <=> $b->[0] }
                           map  { [ $_->[0]->start, $_ ] }
                           @unsorted_genes;

        # At this point we now have a sorted list of genes where each
        # gene is a sorted list of exons.  For the sake of diagnostics,
        # we are now going to write out a temporary file for each $seq

        if ($tmpdir) {
            my $tmpfile = "${tmpdir}/tmp_gff_${seq}.gff";
            my $outfh = IO::File->new( $tmpfile, 'w' );
            die "Can't open output file $tmpfile for writing: $!"
                unless defined $outfh;
            foreach my $tmpg (@sorted_genes) {
                foreach my $gff (@{ $tmpg }) {
                    $outfh->print( $gff->as_gff3, "\n" );
                }
            } 
            $outfh->close;
        }
        

        # Process each gene, sorting the exons by start position and
        # then collapsing any that overlap

        my @collapsed_genes = ();
        foreach my $gene (@sorted_genes) {
            my @exons = @{ $gene };

            # Resolve any overlaps
            my @collapsed_exons = ();
            my $last_gff = shift @exons;
            while (scalar(@exons)) {
                my $this_gff = shift @exons;

                # Check whether we have an overlap
                if ($this_gff->start <= $last_gff->end) {

                    print 'Merging: '. $last_gff->seqname .':'.
                                       $last_gff->start .'-'.
                                       $last_gff->end .
                          ' with '. $this_gff->start .'-'.
                                    $this_gff->end ."\n"
                        if ($VERBOSE > 1);

                    # There are 2 scenarios for the overlap - the exons
                    # are identical but appear multiple times in the GFF
                    # because the exon appears once for each transcript
                    # it is part of; or there is a genuine overlap
                    # between non-identical exons


                    if ($last_gff->start == $this_gff->start and
                        $last_gff->end   == $this_gff->end ) {

                        merge_gffs( $this_gff, $last_gff );

                        # Add gfftool annotation to flag that we did a merge.
                        if (! defined $this_gff->attrib( 'gfftool' ) ) {
                            $this_gff->attrib( 'gfftool', '"identical"' );
                        }
                    }
                    else {

                        merge_gffs( $this_gff, $last_gff );

                        # Add gfftool annotation to flag that we did a merge.
                        if (! defined $this_gff->attrib( 'gfftool' ) or
                            $this_gff->attrib( 'gfftool' ) ne '"overlap"') {
                            $this_gff->attrib( 'gfftool', '"overlap"' );
                        }
                    }

                }
                else {
                    push @collapsed_exons, $last_gff;
                }
                $last_gff = $this_gff;
            }
            push @collapsed_exons, $last_gff;

            push @collapsed_genes, \@collapsed_exons;
        }

        # Now we extend all the exon boundaries (except the start of the
        # first and the end of the last).  We also check for any 
        # inter-exon gaps that would be closed by the extensions!

        my @extended_genes = ();
        foreach my $ra_gene (@collapsed_genes) {
            my @exons = @{$ra_gene};
            my @extended_exons = ();

            my $last_exon = shift @exons;
            while (my $this_exon = shift @exons) {
                # Check if there is room to do both extensions
                my $gap = $this_exon->start - $last_exon->end -1;
                if ($gap < 2*$extension) {
                    warn "WARN: gap [$gap] too small for extension: " .
                        $this_exon->seqname . ' ' .
                        $last_exon->end . ' -> ' .
                        $this_exon->start ."\n" if ($VERBOSE>1);

                    # Update $last_exon and DO NOT push onto @extended_exons
                    merge_gffs( $last_exon, $this_exon );

                    # Add gfftool annotation to flag that we did a merge.
                    if (! defined $last_exon->attrib( 'gfftool' ) or
                        $last_exon->attrib( 'gfftool' ) ne '"collapse"') {
                        $last_exon->attrib( 'gfftool', '"collapse"' );
                    }
                }
                else {
                    $last_exon->end( $last_exon->end + $extension );
                    $this_exon->start( $this_exon->start - $extension );
                    push @extended_exons, $last_exon;
                    $last_exon = $this_exon;
                }
            }

            # Catch the last $last_exon
            push @extended_exons, $last_exon;

            # Save our extended exons in a flattened list of extended genes
            push @extended_genes, @extended_exons;
        }

        # We need to do one final sort and collapse - at the moment exons
        # are sorted within genes (so we could do extension) and genes are
        # sorted by start site but that is not quite the same as having
        # all exons sorted by start site regardless of genes.

        my @sorted_exons = map  { $_->[1] }
                           sort { $a->[0] <=> $b->[0] }
                           map  { [ $_->start, $_ ] }
                           @extended_genes;

        my @final_exons = ();
        my $last_exon = shift @sorted_exons;
        while (my $this_exon = shift @sorted_exons) {
            # Check whether we have an overlap
            if ($this_exon->start <= $last_exon->end) {
                merge_gffs( $last_exon, $this_exon );
                # Add gfftool annotation to flag that we did a merge.
                if (! defined $last_exon->attrib( 'gfftool' ) or
                    $last_exon->attrib( 'gfftool' ) ne '"multigene"') {
                    $last_exon->attrib( 'gfftool', '"multigene"' );
                }
            }
            else {
                push @final_exons, $last_exon;
                $last_exon = $this_exon;
            }
        }

        # Catch the last $last_exon
        push @final_exons, $last_exon;

        # Put final list of sorted extended collapsed exons back into %gffs
        $gffs{ $seq } = \@final_exons;
    }

    print '['.localtime()."]    processing of $infile complete\n" if $VERBOSE;
    return \%gffs;
}


# Pass in 2 QCMG::IO::GffRecords and the merged info will be written
# over the top of the first GffRecord so he is the "keeper".

sub merge_gffs {
    my $keeper = shift;
    my $loser  = shift;

    # Set start to the lesser of the 2 gff starts
    if ($loser->start < $keeper->start) {
        $keeper->start( $loser->start );
    }

    # Set end to the greater of the 2 gff ends
    if ($loser->end > $keeper->end) {
        $keeper->end( $loser->end );
    }

    # Merge the exon_id and transcript_id attributes
    my $new_exon_id = $keeper->attrib('exon_id') . $loser->attrib('exon_id');
    $new_exon_id =~ s/""/\,/g;
    $keeper->attrib( 'exon_id', $new_exon_id ); 
    my $new_tran_id = $keeper->attrib('transcript_id') .
                      $loser->attrib('transcript_id');
    $new_tran_id =~ s/""/\,/g;
    $keeper->attrib( 'transcript_id', $new_tran_id ); 
}


sub write_gff_file {
    my $rh_seqs = shift;
    my $outfile  = shift;

    print '['.localtime()."]  Opening file $outfile\n" if $VERBOSE;

    # Get the detailed output file ready
    my $outfh = IO::File->new( $outfile, 'w' );
    die "Can't open output file $outfile for writing: $!"
        unless defined $outfh;

    # We need to write out the results in the same order as the Agilent
    # SureSelect GFF

    foreach my $seq (@AG_ORDER) {
        if (! exists $rh_seqs->{$seq}) {
            warn "No exons matcing SureSelect sequence $seq\n";
        }
        foreach my $gff (@{ $rh_seqs->{$seq} }) {
            $outfh->print( $gff->as_gff3, "\n" );
        }
    }

    $outfh->close;
}


sub ensembl_to_GRCh37 {
    my $ensembl = shift;
    if (exists $CHR_E2I{ $ensembl }) {
        return $CHR_E2I{ $ensembl };
    }
    warn "Cannot convert Ensembl sequence name $ensembl to GRCh37_ICGC\n";
    return undef;
}

sub ensembl_to_agilent {
    my $ensembl = shift;
    if (exists $CHR_AG{ $ensembl }) {
        return $CHR_AG{ $ensembl };
    }
    warn "Cannot convert Ensembl sequence name $ensembl to Agilent\n";
    return undef;
}


__END__

=head1 NAME

gff_exon_extractor.pl - Create GFF file of modified gene model exons


=head1 SYNOPSIS

 gff_exon_extactor.pl -i infile -o outfile [options]


=head1 ABSTRACT

This script will take a GFF of gene models, pull out the exons, extend
each splice site by 2 bases and write out to a new GFF file.  This is
the format requested by David Wheeler from Baylor for use in the
passenger/driver calculations.


=head1 OPTIONS

 -i | --infile        GFF file of gene models
 -o | --outfile       GFF file of extended exons
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: gff_exon_extractor.pl 4669 2014-07-24 10:48:22Z j.pearson $


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
