package QCMG::QInspect::Sam2Pdf;

##############################################################################
#
#  Module:   QCMG::QInspect::Sam2Pdf.pm
#  Creator:  John Pearson
#  Created:  2012-06-07
#
#  This perl module renders reads from an array of SamRecord objects as
#  a PDF content stream for inclusion in a PDF document Page.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;
use Data::Dumper;
use Carp qw( croak carp );

use QCMG::PDF::PdfPrimitives qw( ppRectanglePath ppStrokedRectangle
                                 ppClippedRectangle
                                 ppStrokedClippedRectangle );
use QCMG::QInspect::PdfPath;
use QCMG::QInspect::SamReferenceConcordance;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION %COLORS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

BEGIN {
    %COLORS = ( chr1   => '0.6 0.3 0.9', 
                chr2   => '0.3 0.5 0.8', 
                chr3   => '0.8 0.9 0.2', 
                chr4   => '0.7 0.0 0.8', 
                chr5   => '0.6 0.8 0.6', 
                chr6   => '0.1 0.8 0.8', 
                chr7   => '0.9 0.8 0.7', 
                chr8   => '0.2 0.7 0.5', 
                chr9   => '0.7 0.7 0.7', 
                chr10  => '0.6 0.3 0.6', 
                chr11  => '0.6 0.5 0.7', 
                chr12  => '0.7 0.6 0.5', 
                chr13  => '0.8 0.6 0.2', 
                chr14  => '0.3 0.8 0.8', 
                chr15  => '0.6 0.2 0.9', 
                chr16  => '0.9 0.9 0.4', 
                chr17  => '0.3 0.5 0.7', 
                chr18  => '0.1 0.7 0.6', 
                chr19  => '0.5 0.3 0.2', 
                chr20  => '0.9 0.4 0.8', 
                chr21  => '0.8 0.8 0.3', 
                chr22  => '0.4 0.5 0.9', 
                chrX   => '0.7 0.2 0.5', 
                chrY   => '0.3 0.7 0.4', 
                chrM   => '0.6 0.8 0.7', 
                chrMT  => '0.1 0.3 0.1', 
                other  => '1.0 0.0 0.0', ); 
}


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { sam_records          => ( $params{sam_records} ?
                                           $params{sam_records} : undef ),
                 show_dups            => 0,
                 verbose              => ( $params{verbose} ?
                                           $params{verbose} : 0 ),
               };
    bless $self, $class;
}


sub file {
    my $self = shift;
    return $self->{'file'} = shift if @_;
    return $self->{'file'};
}


sub show_dups {
    my $self = shift;
    return $self->{'show_dups'} = shift if @_;
    return $self->{'show_dups'};
}


sub verbose {
    my $self = shift;
    return $self->{'verbose'} = shift if @_;
    return $self->{'verbose'};
}


sub create_pdf_stream {
    my $self  = shift;
    my $doc   = shift;
    my $range = shift;  # must be in format chrA:B-C,D
    my $pdfre = shift;

    qlogprint( {l=>'INFO'}, "processing range: $range\n" );

    # Process SAM range
    my $rchr   = undef;   # range chromosome
    my $rstart = undef;   # range start
    my $rend   = undef;   # range end
    my $vstart = undef;   # variant start
    my $vend   = undef;   # variant end
    if ($range =~ /(chr\w+):(\d+)\-(\d+)(?:\,(\d+)\-(\d+)){0,1}$/) {
        $rchr   = $1;
        $rstart = $2;
        $rend   = $3;
        $vstart = $4;
        $vend   = $5;
        # Cope with case that range does not inlcude variant position
        if (! defined $vstart) {
            $vstart = $rstart + (($rend - $rstart)/2);
            $vend   = $vstart;
        }
        # Cope with SNPs where variant start/stop are the same
        if ($vstart == $vend) {
            $vend++;
        }
    }
    else {
        croak "invalid range param [$range]";
    }

    # Determine limits of plotting area - allows for outer stroked
    # rectangle plus inner clipped rectangle plus a couple of units
    my $plot_x_min = $pdfre->[0]+3;
    my $plot_y_min = $pdfre->[1]+3;
    my $plot_x_max = $pdfre->[2] + $plot_x_min -6;
    my $plot_y_max = $pdfre->[3] + $plot_y_min -6;

    my $spacer = 3; # number of bases to leave between adjacent reads

    # Tracks is a list of the next-free-position in each track so it
    # already accounts for the spacer
    my @tracks       = ( 0 );
    my @layout_reads = ();

    my $rec_ctr = 0;
    my $problem_no_md = 0;
    my $problem_unmapped = 0;
    my $problem_dups = 0;

    foreach my $sam (@{$self->{sam_records}}) {
        if (! $sam->tag('MD')) {
            $problem_no_md++;
            next;
        }
        if ($sam->flag_as_chars() =~ /u/) {
            $problem_unmapped++;
            next;
        }
        # Only keep dups if specifically requested
        if ($sam->flag_as_chars() =~ /d/ and ! $self->show_dups) {
            $problem_dups++;
            next;
        }

        # Extreme ends of possible read box
        my $start = $sam->start_with_clips;
        my $end   = $sam->end_with_clips;

        $rec_ctr++;

        # Work out y-coords
        my $found_track = undef;
        foreach my $tctr (0..$#tracks) {
            if ($tracks[$tctr] <= $start) {
                $found_track = $tctr;
                $tracks[$tctr] = $end + $spacer;
                last;  # stop search once we've found a track
            }
        }
        # if we didn't find a track with space to take this record then
        # append a new track at the end of the list
        if (! defined $found_track) {
            push @tracks, ($end + $spacer);
            $found_track = $#tracks;
        }

        push @layout_reads, [ $sam, $found_track ];
    }

    # Work out limits of data
    my $data_x_min = $rstart;
    my $data_x_max = $rend;
    my $data_y_min = 0;
    my $data_y_max = scalar(@tracks);

    # Now that we know the limits of our data and the limits of the
    # plot, we can make a decision on how we should scale the data to
    # fit it inside the plot. In order to ensure that all reads can be
    # plotted, we will calculate scaling factors for both X and Y axes
    # and use the smaller.  N.B. teh *2.5 factor in the y scaling is
    # because each red in the y dimension costs us 2.5 units - 2 for the
    # read itself and 0.5 as a spacer between reads.

    my $xscale = ($plot_x_max - $plot_x_min) /
                 ($data_x_max - $data_x_min);
    my $yscale = ($plot_y_max - $plot_y_min) /
                 ( ($data_y_max - $data_y_min) *2.5 );
    my $scaler = ($xscale <= $yscale) ? $xscale : $yscale;

    # Now that we have the scale, we want to center the reads plot in
    # the available window so we will very likely want to apply an
    # x-axis offset to shift the entire plot to the right: the distance
    # will be one half of the difference between the available x-axis
    # plot space and the required (scaled) x-axis data space.

    my $xoffset = 0.5 * ( ($plot_x_max - $plot_x_min) -
                          (($data_x_max - $data_x_min) * $scaler) );

    # Extra diagnostics in verbose mode
    if ($self->verbose()) {
        qlogprint( {l=>'INFO'}, "  $problem_no_md - ".
                   "records skipped because of missing MD tag\n" );
        qlogprint( {l=>'INFO'}, "  $problem_unmapped - ".
                   "records skipped because unmapped\n" );
        qlogprint( {l=>'INFO'}, "  $problem_dups - ".
                   "records skipped because duplicates\n" );
        qlogprint( {l=>'INFO'}, "  $rec_ctr - ".
                   "records processed into layout\n" );
        qlogprint( {l=>'INFO'}, "  data ranges: X[$data_x_min - $data_x_max]  ".
                   'Y[0 - ' . scalar(@tracks) ."]\n" );
        if ($self->verbose() > 1) {
            qlogprint( {l=>'INFO'},
                       "  plot ranges: X[$plot_x_min - $plot_x_max]   ".
                       "Y[$plot_y_min - $plot_y_max]\n" );
            qlogprint( {l=>'INFO'}, "  plot calcs: Xscale[$xscale]",
                                    "  Yscale[$yscale]  Xoffset[$xoffset]\n");
        }
    }

    # Create stream PDF object and draw a black border
    my $stream = QCMG::PDF::IndirectObject::Stream->new();
    $doc->add_object( $stream );
    $self->{stream} = $stream;

    # We are going to futz with a lot of stuff so let's establish a new
    # "graphics state" as the first thing we do (and end with a "Q")
    $stream->add_contents( "q\n" );

    # We need to set up a clipping path so any reads that we draw cannot
    # plot outside their box, even if we get the plotting math wrong
    # somehow. If we use a 2 user unit margin then there will be a little
    # clear space between the reads and any border box we might draw.
    my $path = ppClippedRectangle( @{ $pdfre }, 2 );
    $stream->add_contents( $path );

    # We will want to draw a rectangle along the axis of the variant
    # location but it will have to be defined in reference bases on the
    # x-axis and user units on the y-axis and then put through some of
    # the read transforms (all except y-scaling).

    my $var_path = QCMG::QInspect::PdfPath->new();
    $var_path->add_points( [ $vstart-1, $plot_y_min ],
                           [ $vstart-1, $plot_y_max ],
                           [ $vend+1,   $plot_y_max ],
                           [ $vend+1,   $plot_y_min ] );
    $var_path->pre_text( "1.0 0.6 1.0 rg\n" );
    $var_path->post_text( "f\n" );
    $var_path->apply_x_offset( 0-$data_x_min );
    $var_path->scale_x( $scaler );
    $var_path->apply_x_offset( $xoffset );
    $stream->add_contents( $var_path->pdf_text() );

    # Now add the PDF for the reads themselves

    foreach my $layout (@layout_reads) {
        my $pdf_text = $self->_define_a_read( $layout->[0],
                                              $layout->[1],
                                              $xoffset,
                                              $data_x_min,
                                              $plot_y_max,
                                              $scaler);
        $stream->add_contents( $pdf_text );
    }

    $stream->add_contents( "Q\n" );

    return $stream;
}


sub _define_a_read {
    my $self       = shift;
    my $rec        = shift;
    my $ytrack     = shift;
    my $xoffset    = shift;
    my $data_x_min = shift;
    my $plot_y_max = shift; 
    my $scaler     = shift;

    # The first thing to do is deconstruct the read into paths (sequences 
    # of points) in base units.  We will then offset and scale every point
    # in the path and create PDF for the path.  Computation is cheap and
    # paid for once up front so we should put some thought into
    # minimising the number of shapes the PDF has to draw.  For example,
    # if a read has no clipping and no deletions then there is no point
    # drawing the clipping rectangle because it will be entirely
    # occluded by the read itself.  Same for the deletion spine.
    #
    # We will draw one or more of the following for each read:
    # 1. light gray clipping rectangle
    # 2. thin black rectangle that will show through in deletions
    # 3. a box for each extent of MI from CIGAR - first and last boxes
    #    get "arrow and feathers"
    # 4. a colored box for any mismatches from MD
    # 5. some glyph to show insertions ("I"?).
    #
    # Every path will be created and then go through transforms:
    # 1. Substract $data_x_min from all x-axis positions - all boxes start
    #    with x coords that are in reference base positions.  This transform
    #    "zeros" the x-axis coords.
    # 2. Scale all x and y positions by $scaler.
    # 3. Add $xoffset to all x-axis positions - shifts plot so that it
    #    is roughly centred in the plot box.
    # 4. Subtract all y-axis positions from $plot_y_max - "flips" plot
    #    on the y axis.

    # Set useful flags that we will use in multiple places.
    my $flag_as_chars    = $rec->flag_as_chars();
    my $is_duplicate     = $flag_as_chars =~ /d/;
    my $is_unmapped      = $flag_as_chars =~ /u/;
    my $is_mate_unmapped = ( $flag_as_chars =~ /U/ or $rec->mrnm() eq '*' );
    my $is_reversed      = $flag_as_chars =~ /r/;
    my @cigops           = @{ $rec->cigops() };
    my $is_varied        = $rec->tag('MD') =~ /[ACGT]{1}/;
    my $is_clipped       = 0;
    my $is_gapped        = 0;
    my $is_inserted      = 0;

    foreach my $op (@cigops) {
        $is_clipped  = 1 if ($op->[1] eq 'H' or $op->[1] eq 'S');
        $is_gapped   = 1 if ($op->[1] eq 'D' or $op->[1] eq 'N');
        $is_inserted = 1 if ($op->[1] eq 'I');
    }

    # Unmapped reads should have been dropped during the preprocessing
    # but it costs almost nothing to check again
    next if $is_unmapped;

    # Pick color for this read based on mate's alignment.  Order matters
    # here and is_duplicate must come first because it is not exclusive
    # of the other checks.
    my $color = ($is_duplicate)                 ? '0.7 0.7 0.7' :
                ($rec->mrnm() eq $rec->rname()) ? '0.6 0.6 0.6' :
                ($rec->mrnm() eq '=')           ? '0.6 0.6 0.6' :
                ($rec->mrnm() eq '*')           ? '0.6 0.6 0.6' :
                (exists $COLORS{$rec->mrnm()})  ? $COLORS{$rec->mrnm()} :
                $COLORS{'other'};

    # Calc y position and set how many y user units should a read take up
    my $y_units = 2;
    my $ybot    = 0.5 + (($y_units+0.5) * $ytrack);
    my $ytop    = $ybot + $y_units;

    # To keep the logic from getting too florid, we will specify all of
    # the paths in "reference sequence" coordinates and store them away
    # in @paths.  At the end of the routine, we will use a single loop to
    # apply the desired transforms in the correct sequence to all of the
    # paths in a single pass.
    my @paths = ();

    # Draw light gray box for clip limits but only if the clip limits
    # are different from the expected limits or the read contains a
    # deletion, otherwise we are wastefully drawing a path that will
    # be completely occluded by the read itself.

    if ($is_clipped or $is_gapped) {
        my $clip_path = QCMG::QInspect::PdfPath->new();
        $clip_path->add_points( [ $rec->start_with_clips(), $ytop ],
                                [ $rec->end_with_clips(),   $ytop ],
                                [ $rec->end_with_clips(),   $ybot ],
                                [ $rec->start_with_clips(), $ybot ] );
        $clip_path->pre_text( "0.8 0.8 0.8 rg\n" );
        $clip_path->post_text( "f\n" );
        push @paths, $clip_path;
    }

    # Draw a black "spine" for deletions or introns
    if ($is_gapped) {
        my $gap_path = QCMG::QInspect::PdfPath->new();
        $gap_path->add_points( [ $rec->pos(), $ytop-0.4*$y_units ],
                               [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, $ytop-0.4*$y_units ],
                               [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, $ytop-0.6*$y_units ],
                               [ $rec->pos(), $ytop-0.6*$y_units ] );
        $gap_path->pre_text( "0.2 0.2 0.2 rg\n" );
        $gap_path->post_text( "f\n" );
        push @paths, $gap_path;
    }

    # The paths_from_sam() method doubles processing time for each SAM
    # record so we should avoid it where possible which is why we have
    # an "if" here that only runs the expensive method if the read
    # contains some sort of variant.  This leads to some minor 
    # functionality duplication but the run-time savings are worth it.

    if ($is_gapped or $is_inserted or $is_varied) {
        push @paths, $self->paths_from_sam( $rec, $is_reversed, $ytop, $ybot, $color );
    }
    else {
        # Draw chromosome-colored box for read limits including triangle
        # for direction
        my $chr_path = QCMG::QInspect::PdfPath->new();
        if ($is_reversed) {
            $chr_path->add_points(
                    [ $rec->pos() -1, ($ytop+$ybot)/2 ],
                    [ $rec->pos(), $ytop ],
                    [ $rec->pos()+$rec->length_on_ref_from_cigar(), $ytop ],
                    [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, ($ytop+$ybot)/2 ],
                    [ $rec->pos()+$rec->length_on_ref_from_cigar(), $ybot ],
                    [ $rec->pos(), $ybot ] );
        }
        else {
            $chr_path->add_points(
                    [ $rec->pos()+$rec->length_on_ref_from_cigar(), ($ytop+$ybot)/2 ],
                    [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, $ytop ],
                    [ $rec->pos()-1, $ytop ],
                    [ $rec->pos(), ($ytop+$ybot)/2 ],
                    [ $rec->pos()-1, $ybot ],
                    [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, $ybot ] );
        }
        $chr_path->pre_text( $color . " rg\n" );
        $chr_path->post_text( "f\n" );
        push @paths, $chr_path;
    }

    # If mate is unmapped, add gold paths for the arrow.
    # Logic to color feather is also there but currently commented out because 
    # it looked like overkill.
    if ($is_mate_unmapped) {

        my $a_path = QCMG::QInspect::PdfPath->new();

        if ($rec->flag_as_chars() =~ /r/) {
            $a_path->add_points(
                    [ $rec->pos(), $ytop ],
                    [ $rec->pos() -1, ($ytop+$ybot)/2 ],
                    [ $rec->pos(), $ybot ] );
#            $f_path->add_points(
#                    [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, $ybot ],
#                    [ $rec->pos()+$rec->length_on_ref_from_cigar(), $ybot ],
#                    [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, ($ytop+$ybot)/2 ],
#                    [ $rec->pos()+$rec->length_on_ref_from_cigar(), $ytop ],
#                    [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, $ytop ] );
        }
        else {
            $a_path->add_points(
                    [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, $ytop ],
                    [ $rec->pos()+$rec->length_on_ref_from_cigar(), ($ytop+$ybot)/2 ],
                    [ $rec->pos()+$rec->length_on_ref_from_cigar()-1, $ybot ] );
#            $f_path->add_points(
#                    [ $rec->pos(), $ybot ],
#                    [ $rec->pos()-1, $ybot ],
#                    [ $rec->pos(), ($ytop+$ybot)/2 ],
#                    [ $rec->pos()-1, $ytop ],
#                    [ $rec->pos(), $ytop ] );
        }

        $a_path->pre_text( "0.9 0.6 0 rg\n" );
#        $a_path->pre_text( "0.2 0.2 0.2 rg\n" );
        $a_path->post_text( "f\n" );
#        $f_path->pre_text( "0.2 0.2 0.2 rg\n" );
#        $f_path->post_text( "f\n" );

#        push @paths, $a_path, $f_path;
        push @paths, $a_path;
    }

    my $text = '';

    # Apply transforms
    foreach my $path (@paths) {
        $path->apply_x_offset( 0-$data_x_min );
        $path->scale( $scaler );
        $path->apply_x_offset( $xoffset );
        $path->offset_y_from( $plot_y_max );
        $text .= $path->pdf_text();
    }

    return $text;
}


sub paths_from_sam {
    my $self    = shift;
    my $rec     = shift;
    my $reverse = shift;
    my $ytop    = shift;
    my $ybot    = shift;
    my $color   = shift;

    my @epaths = ();
    my @vpaths = ();
    my @ipaths = ();

    my $src = QCMG::QInspect::SamReferenceConcordance->new( sam => $rec );
#    print $src->to_text(0);
#    my $ra_extents = $src->extents();
#    my $ra_variants = $src->variants();

    my @extents  = @{ $src->extents() };
    my @variants = @{ $src->variants() };
    my @inserts  = @{ $src->inserts() };
    
    foreach my $ctr (0..$#extents) {
        my $extent = $extents[$ctr];
        my $arrow = 0;
        my $feather = 0;
        if ($ctr == 0) {
            $arrow   = 1 if $reverse;
            $feather = 1 if ! $reverse;
        }
        if ($ctr == $#extents) {
            $arrow   = 1 if ! $reverse;
            $feather = 1 if $reverse;
        }
        my $bpath = $self->block( $rec->pos() + $extent->[0],
                                  $rec->pos() + $extent->[1],
                                  $ytop,
                                  $ybot,
                                  $reverse,
                                  $arrow,
                                  $feather,
                                  1 );
        $bpath->pre_text( $color . " rg\n" );
        $bpath->post_text( "f\n" );
        push @epaths, $bpath;
    }
    
    foreach my $variant (@variants) {
        my $bpath = $self->block( $rec->pos() + $variant->[0],
                                  $rec->pos() + $variant->[0]+1,
                                  $ytop,
                                  $ybot,
                                  0,
                                  0,
                                  0,
                                  1 );
        my $color = ($variant->[1] eq 'A') ? '0 0.8 0' :
                    ($variant->[1] eq 'C') ? '0 0 0' :
                    ($variant->[1] eq 'G') ? '0 0 0.9' :
                    ($variant->[1] eq 'T') ? '0.9 0 0' :
                    ($variant->[1] eq 'N') ? '0 0.9 0.9' : '0.9 0.9 0';
        $bpath->pre_text( $color . " rg\n" );
        $bpath->post_text( "f\n" );
        push @vpaths, $bpath;
    }
    
    foreach my $insert (@inserts) {
        my $bpath = $self->diamond( $rec->pos() + $insert->[0]-1,
                                    $rec->pos() + $insert->[0]+1,
                                    $ytop,
                                    $ybot );
        $bpath->pre_text( "0.8 0 0.8 rg\n" );
        $bpath->post_text( "f\n" );
        push @ipaths, $bpath;
    }

#    print Dumper \@variants if (scalar(@variants) > 0);

#    print Dumper \@variants if (scalar(@variants) > 0);
#    print Dumper $rec->pos(), scalar(@epaths), scalar(@vpaths),
#                 \@epaths, \@vpaths;

    return @epaths, @vpaths, @ipaths;
}


sub diamond {
    my $self  = shift;
    my $left  = shift;
    my $right = shift;
    my $top   = shift;
    my $bot   = shift;

    # We will always describe these paths as starting at the top
    # and moving clockwise.

    my $vmid = ($top+$bot)/2;
    my $hmid = ($left+$right)/2;
    my $path = QCMG::QInspect::PdfPath->new();

    $path->add_points( [$hmid,  $top],
                       [$right, $vmid],
                       [$hmid,  $bot],
                       [$left, $vmid] );
    return $path;
}


sub block {
    my $self  = shift;
    my $left  = shift;
    my $right = shift;
    my $top   = shift;
    my $bot   = shift;
    my $revrs = shift;  # 1=Rev, 0=For
    my $arrow = shift;  # 0=No, 1=Yes,
    my $feath = shift;  # 0=No, 1=Yes,
    my $shift = shift || 1;

    # We will always describe these paths as starting at the top left
    # corner and moving clockwise.

    my $mid = ($top+$bot)/2;
    my $path = QCMG::QInspect::PdfPath->new();

    # Top points
    if ($feath and ! $revrs) {
        $path->add_points( [$left-1,$top], [$right,$top] );
    }
    elsif ($feath) {
        $path->add_points( [$left,$top], [$right+1,$top] );
    }
    else {
        $path->add_points( [$left,$top], [$right,$top] );
    }

    # Middle on the right.
    if (! $revrs and $arrow) {
        $path->add_points( [$right+1,$mid] );
    }
    elsif ($revrs and $feath) {
        $path->add_points( [$right,$mid] );
    }

    # Bottom points
    if ($feath and $revrs) {
        $path->add_points( [$right+1,$bot], [$left,$bot] );
    }
    elsif ($feath) {
        $path->add_points( [$right,$bot], [$left-1,$bot] );
    }
    else {
        $path->add_points( [$right,$bot], [$left,$bot] );
    }

    # Middle on the left.
    if ($revrs and $arrow) {
        $path->add_points( [$left-1,$mid] );
    }
    elsif (! $revrs and $feath) {
        $path->add_points( [$left,$mid] );
    }

    return $path;
}


1;
__END__


=head1 NAME

QCMG::QInspect::Sam2Pdf - Perl module for plotting aligned reads as PDF


=head1 SYNOPSIS

 use QCMG::QInspect::Sam2Pdf;


=head1 DESCRIPTION

This module will take an array of QCMG::IO::SamRecord objects and some
display parameters and create a PDF Content Stream that would plot those
reads.


=head1 PUBLIC METHODS

=over

=item B<new()>

=item B<create_pdf_stream()>

=item B<verbose()>

 $ini->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


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
