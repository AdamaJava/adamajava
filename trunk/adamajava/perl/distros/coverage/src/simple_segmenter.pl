#!/usr/bin/perl -w

##############################################################################
#
#  Program:  simple_segmenter.pl
#  Author:   John V Pearson
#  Created:  2011-05-11
#
#  Read in output file from tally_feature_coverage.pl and based on
#  user-specified min/max values for coverage_per_base_per_file (column
#  7), convert labels from bait to badbait for any features with
#  coverages outside the specified limits.
#
#  $Id: simple_segmenter.pl 4667 2014-07-24 10:09:43Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak );
use Data::Dumper;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use XML::Simple;

use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $VERSION $VERBOSE %BOUNDS $OUR_BOUNDS );

#use QCMG::Util::QLog;

( $REVISION ) = '$Revision: 4667 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: simple_segmenter.pl 4667 2014-07-24 10:09:43Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


BEGIN {

    # This data structure was created from the file
    # GRCh37_ICGC_standard_v2.properties which is stored on
    # qcmg-clustermk2 at: /panfs/share/genomes/GRCh37_ICGC_standard_v2

    my %GRCh37 = (  chr1  => [ 'chr1', 249250621, 0 ],
                    chr2  => [ 'chr2', 243199373, 252811351 ],
                    chr3  => [ 'chr3', 198022430, 499485007 ],
                    chr4  => [ 'chr4', 191154276, 700336335 ],
                    chr5  => [ 'chr5', 180915260, 894221393 ],
                    chr6  => [ 'chr6', 171115067, 1077721163 ],
                    chr7  => [ 'chr7', 159138663, 1251280737 ],
                    chr8  => [ 'chr8', 146364022, 1412692816 ],
                    chr9  => [ 'chr9', 141213431, 1561147759 ],
                    chr10 => [ 'chr10', 135534747, 1704378531 ],
                    chr11 => [ 'chr11', 135006516, 1841849496 ],
                    chr12 => [ 'chr12', 133851895, 1978784684 ],
                    chr13 => [ 'chr13', 115169878, 2114548756 ],
                    chr14 => [ 'chr14', 107349540, 2231363925 ],
                    chr15 => [ 'chr15', 102531392, 2340247037 ],
                    chr16 => [ 'chr16', 90354753, 2444243171 ],
                    chr17 => [ 'chr17', 81195210, 2535888714 ],
                    chr18 => [ 'chr18', 78077248, 2618243863 ],
                    chr19 => [ 'chr19', 59128983, 2697436508 ],
                    chr20 => [ 'chr20', 63025520, 2757410198 ],
                    chr21 => [ 'chr21', 48129895, 2821336090 ],
                    chr22 => [ 'chr22', 51304566, 2870153562 ],
                    chrX  => [ 'chrX', 155270560, 2922191058 ],
                    chrY  => [ 'chrY', 59373566, 3079679775 ],
                    'GL000191.1' => [ 'GL000191.1', 106433, 3139901541 ],
                    'GL000192.1' => [ 'GL000192.1', 547496, 3140009507 ],
                    'GL000193.1' => [ 'GL000193.1', 189789, 3140564837 ],
                    'GL000194.1' => [ 'GL000194.1', 191469, 3140757350 ],
                    'GL000195.1' => [ 'GL000195.1', 182896, 3140951567 ],
                    'GL000196.1' => [ 'GL000196.1', 38914, 3141137088 ],
                    'GL000197.1' => [ 'GL000197.1', 37175, 3141176570 ],
                    'GL000198.1' => [ 'GL000198.1', 90085, 3141214289 ],
                    'GL000199.1' => [ 'GL000199.1', 169874, 3141305673 ],
                    'GL000200.1' => [ 'GL000200.1', 187035, 3141477986 ],
                    'GL000201.1' => [ 'GL000201.1', 36148, 3141667705 ],
                    'GL000202.1' => [ 'GL000202.1', 40103, 3141704382 ],
                    'GL000203.1' => [ 'GL000203.1', 37498, 3141745070 ],
                    'GL000204.1' => [ 'GL000204.1', 81310, 3141783116 ],
                    'GL000205.1' => [ 'GL000205.1', 174588, 3141865600 ],
                    'GL000206.1' => [ 'GL000206.1', 41001, 3142042695 ],
                    'GL000207.1' => [ 'GL000207.1', 4262, 3142084294 ],
                    'GL000208.1' => [ 'GL000208.1', 92689, 3142088629 ],
                    'GL000209.1' => [ 'GL000209.1', 159169, 3142182655 ],
                    'GL000210.1' => [ 'GL000210.1', 27682, 3142344110 ],
                    'GL000211.1' => [ 'GL000211.1', 166566, 3142372200 ],
                    'GL000212.1' => [ 'GL000212.1', 186858, 3142541158 ],
                    'GL000213.1' => [ 'GL000213.1', 164239, 3142730698 ],
                    'GL000214.1' => [ 'GL000214.1', 137718, 3142897296 ],
                    'GL000215.1' => [ 'GL000215.1', 172545, 3143036994 ],
                    'GL000216.1' => [ 'GL000216.1', 172294, 3143212016 ],
                    'GL000217.1' => [ 'GL000217.1', 172149, 3143386784 ],
                    'GL000218.1' => [ 'GL000218.1', 161147, 3143561405 ],
                    'GL000219.1' => [ 'GL000219.1', 179198, 3143724867 ],
                    'GL000220.1' => [ 'GL000220.1', 161802, 3143906637 ],
                    'GL000221.1' => [ 'GL000221.1', 155397, 3144070763 ],
                    'GL000222.1' => [ 'GL000222.1', 186861, 3144228392 ],
                    'GL000223.1' => [ 'GL000223.1', 180455, 3144417935 ],
                    'GL000224.1' => [ 'GL000224.1', 179693, 3144600980 ],
                    'GL000225.1' => [ 'GL000225.1', 211173, 3144783253 ],
                    'GL000226.1' => [ 'GL000226.1', 15008, 3144997455 ],
                    'GL000227.1' => [ 'GL000227.1', 128374, 3145012690 ],
                    'GL000228.1' => [ 'GL000228.1', 129120, 3145142910 ],
                    'GL000229.1' => [ 'GL000229.1', 19913, 3145273887 ],
                    'GL000230.1' => [ 'GL000230.1', 43691, 3145294097 ],
                    'GL000231.1' => [ 'GL000231.1', 27386, 3145338425 ],
                    'GL000232.1' => [ 'GL000232.1', 40652, 3145366215 ],
                    'GL000233.1' => [ 'GL000233.1', 45941, 3145407460 ],
                    'GL000234.1' => [ 'GL000234.1', 40531, 3145454070 ],
                    'GL000235.1' => [ 'GL000235.1', 34474, 3145495193 ],
                    'GL000236.1' => [ 'GL000236.1', 41934, 3145530172 ],
                    'GL000237.1' => [ 'GL000237.1', 45867, 3145572718 ],
                    'GL000238.1' => [ 'GL000238.1', 39939, 3145619253 ],
                    'GL000239.1' => [ 'GL000239.1', 33824, 3145659775 ],
                    'GL000240.1' => [ 'GL000240.1', 41933, 3145694095 ],
                    'GL000241.1' => [ 'GL000241.1', 42152, 3145736640 ],
                    'GL000242.1' => [ 'GL000242.1', 43523, 3145779407 ],
                    'GL000243.1' => [ 'GL000243.1', 43341, 3145823564 ],
                    'GL000244.1' => [ 'GL000244.1', 39929, 3145867537 ],
                    'GL000245.1' => [ 'GL000245.1', 36651, 3145908049 ],
                    'GL000246.1' => [ 'GL000246.1', 38154, 3145945236 ],
                    'GL000247.1' => [ 'GL000247.1', 36422, 3145983948 ],
                    'GL000248.1' => [ 'GL000248.1', 39786, 3146020903 ],
                    'GL000249.1' => [ 'GL000249.1', 38502, 3146061270 ],
                    'chrM'	 => [ 'chrM',       16569, 3146100335 ],
                    'chrMT' 	 => [ 'chrMT',      16569, 3146100335 ] );

    # This data structure was created from the file
    # Mus_musculus.NCBIM37.64.ALL_validated.E.fa.fai which is stored on
    # qcmg-clustermk2 at: /panfs/share/genomes/lifescope/referenceData/internal/qcmg_mm64/reference

    my %mm64 =   (  chr1  => [ 'chr1', 197195432, 0 ],
                    chr2  => [ 'chr2', 181748087, 197195432 ],
                    chr3  => [ 'chr3', 159599783, 378943519 ],
                    chr4  => [ 'chr4', 155630120, 538543302 ],  
                    chr5  => [ 'chr5', 152537259, 694173422 ],
                    chr6  => [ 'chr6', 149517037, 846710681 ],
                    chr7  => [ 'chr7', 152524553, 996227718 ],
                    chr8  => [ 'chr8', 131738871, 1148752271 ],
                    chr9  => [ 'chr9', 124076172, 1280491142 ],
                    chr10 => [ 'chr10', 129993255, 1404567314 ],
                    chr11 => [ 'chr11', 121843856, 1534560569 ],
                    chr12 => [ 'chr12', 121257530, 1656404425 ],
                    chr13 => [ 'chr13', 120284312, 1777661955 ],
                    chr14 => [ 'chr14', 125194864, 1897946267 ],
                    chr15 => [ 'chr15', 103494974, 2023141131 ],
                    chr16 => [ 'chr16', 98319150, 2126636105 ],
                    chr17 => [ 'chr17', 95272651, 2224955255 ],
                    chr18 => [ 'chr18', 90772031, 2320227906 ],
                    chr19 => [ 'chr19', 61342430, 2410999937 ],
                    chrX  => [ 'chrX', 166650296, 2472342367 ],
                    chrY  => [ 'chrY', 15902555, 2638992663 ],
                    chrMT => [ 'chrMT', 16299, 2654895218 ] );

	# created from  /panfs/share/genomes/d_rerio_Zv9_split/*.fa
	my %drezv9	= (
		chr1  => ['chr1',  61210509, 0],
  		chr2  => ['chr2',  61210509, 61210509],
  		chr3  => ['chr3',  64172719, 122421018],
  		chr4  => ['chr4',  62981743, 185402761],
  		chr5  => ['chr5',  76763251, 262166012],
  		chr6  => ['chr6',  60795000, 322961012],
  		chr7  => ['chr7',  78380008, 401341020],
  		chr8  => ['chr8',  56987406, 458328426],
  		chr9  => ['chr9',  59064353, 517392779],
  		chr10 => ['chr10', 47256756, 564649535],
  		chr11 => ['chr11', 47327911, 611977446],
  		chr12 => ['chr12', 51421526, 663398972],
  		chr13 => ['chr13', 54866578, 718265550],
  		chr14 => ['chr14', 54501520, 772767070],
  		chr15 => ['chr15', 48120179, 820887249],
  		chr16 => ['chr16', 59620409, 880507658],
  		chr17 => ['chr17', 54755943, 935263601],
  		chr18 => ['chr18', 50590025, 985853626],
  		chr19 => ['chr19', 50972475, 1036826101],
  		chr20 => ['chr20', 56751458, 1093577559],
  		chr21 => ['chr21', 45180410, 1138757969],
  		chr22 => ['chr22', 42864730, 1181622699],
  		chr23 => ['chr23', 47049547, 1228672246],
  		chr24 => ['chr24', 44575404, 1273247650],
  		chr25 => ['chr25', 39049466, 1312297116],
  		chrMT => ['chrMT', 16835,    1312313951]);

    $BOUNDS{ GRCh37 } = \%GRCh37;
    $BOUNDS{ mm64   } = \%mm64;
    $BOUNDS{ drezv9 } = \%drezv9;

}


###########################################################################
#
# "Cry havoc, and let slip the dogs of war ..."
#

MAIN: {

    # Setup defaults for important variables.

    my $infile     = '';
    my $outfile    = '';
    my @features   = ();
    my $merge      = 0;
    my $fill       = '';
    my $bounds     = '';
       $VERBOSE    = 0;
       $VERSION    = 0;
    my $help       = 0;
    my $man        = 0;

    # Print usage message if no arguments supplied
    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $cmdline = join(' ',@ARGV);
    my $results = GetOptions (
           'i|infile=s'           => \$infile,        # -i
           'o|outfile=s'          => \$outfile,       # -o
           'f|feature=s'          => \@features,      # -f
           'g|merge!'             => \$merge,         # -g
           'l|fill!'              => \$fill,          # -l
           'b|bounds=s'           => \$bounds,        # -b
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

    # Set up logging
    qlogbegin;
    qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");
    qlogparams;

    if ($fill) {
        die "FATAL: if you specify --fill, you must also specify --bounds\n"
            unless ($bounds);
    }
    if ($bounds) {
        die "FATAL: [$bounds] is not a valid value for --bounds\n"
            unless (exists $BOUNDS{ $bounds });
        $OUR_BOUNDS = $BOUNDS{ $bounds };

    }

    my $rh_targets = process_features( @features );
    #print Dumper $rh_targets;
    my $ra_infeatures = read_gff( $infile, $rh_targets );
    
#    write_gff3( 'debug_G_after_initial_read.gff3', $ra_infeatures, 'n/a' );

    die "FATAL: there are no features of the types specified\n" unless
        (scalar(@{ $ra_infeatures }) > 0);

    my $ra_outfeatures = add_shoulders( $rh_targets, $ra_infeatures );

#    write_gff3( 'debug_K_after_add_shoulders.gff3', $ra_outfeatures, 'n/a' );

    if ($fill) {
        $ra_outfeatures = fill( $ra_outfeatures, $bounds );
#        write_gff3( 'debug_P_after_optional_fill.gff3', $ra_outfeatures, 'n/a' );
    }
    if ($merge) {
        $ra_outfeatures = mega_merge( $ra_outfeatures, 0,0,1 );
#        write_gff3( 'debug_T_after_optional_merge.gff3', $ra_outfeatures, 'n/a' );
    }

    write_gff3( $outfile, $ra_outfeatures, $cmdline );

    qlogend;
}


sub process_features {
    my @features = @_;

    my %targets = ();
    my $priority = 1;
    foreach my $seg (@features) {
        my @pre = ();
        my @post = ();
        my @fields = split /,/, $seg;
        my $feature = shift @fields;
        foreach my $shoulder (@fields) {
            if ($shoulder =~ /^\+(\d+)$/) {
                push @post, $1;
            }
            elsif ($shoulder =~ /^\-(\d+)$/) {
                push @pre, $1;
            }
            else {
                push @post, $shoulder;
                push @pre,  $shoulder;
            }
        }

        # Total length of pre and post shoulders
        my $before_bases = 0;
        $before_bases += $_ foreach @pre;
        my $after_bases = 0;
        $after_bases += $_ foreach @post;

        $targets{ $feature } = { feature      => $feature,
                                 before       => \@pre,
                                 before_bases => $before_bases,
                                 after        => \@post,
                                 after_bases  => $after_bases,
                                 priority     => $priority++ };
        if ($VERBOSE) {
           print 'Feature: ', join(',',reverse(@pre),$feature,@post),"\n";
        }
    }

    return \%targets;
}


sub read_gff {
    my $infile     = shift;
    my $rh_targets = shift;

    my $fh = IO::File->new( $infile, 'r' );
    die("Cannot open input file $infile for reading: $!\n")
        unless defined $fh;
 
    my @features = ();
    my $ctr = 0;
    while (my $line = $fh->getline) {
        chomp $line;
        next if ($line =~ /^#/);   # skip headers
        my @fields = split /\t/, $line;

        # If the region has a type in the requested list then save it
        if ( exists $rh_targets->{ $fields[2] } ) {
            push @features, \@fields;
            $rh_targets->{ $fields[2] }->{ count }++;
        }
    }

#    write_gff3( 'debug_B_in_read_gff_pre_sort.gff3', \@features, 'n/a' );

    my @sorted_features = @{ sort_features( \@features ) };

#    write_gff3( 'debug_E_in_read_gff_post_sort.gff3', \@sorted_features, 'n/a' );

    # Check for consecutive regions that are subfeatures or overlaps
    my $ra_merged_feats = mega_merge( \@sorted_features, 1,1,0 );
    @features = @{ $ra_merged_feats };

    return \@features;
}


sub sort_features {
    my $ra_infeats = shift;

    my @in_features  = @{ $ra_infeats };
    my @out_features = ();
    my %found_seqs   = ();

    # Sort into hash by sequence
    foreach my $ctr (0..$#in_features) {
        my $this = $in_features[$ctr];
        my $seq = $this->[0];
        $seq =~ s/^chr//;
        push @{ $found_seqs{$seq} }, $this;
    }

    # Now we sort the sequence names and sort the sequences by start
    # position with each sequence.  We will be sorting numerically so we
    # disable warnings to avoid bitching about X, Y, MT etc.

    no warnings;
    foreach my $seqname (sort { $a <=> $b } keys %found_seqs) {
        my %starts = ();
        # Sort by start position within this sequence
        foreach my $rec (@{ $found_seqs{$seqname} }) {
            # Allow for having multiple sequences with same start
            push @{ $starts{ $rec->[3] } }, $rec;
        }
        # Now unwind the sorted records into the output array
        foreach my $start (sort { $a <=> $b } keys %starts) {
            push @out_features, @{ $starts{ $start } };
        }
    }
    use warnings;

    # Return the sorted array
    return \@out_features;
}


sub add_shoulders {
    my $rh_targets = shift;
    my $ra_infeats = shift;

    # A GFF3 records looks like:
    # 0  sequence    chr1
    # 1  source      SureSelect_All_Exon_50mb_with_annotation.hg19.bed
    # 2  label       bait
    # 3  start       14467
    # 4  end         14587
    # 5  score       .
    # 6  strand      +
    # 7  phase       .
    # 8  attributes  ID=ens|ENST00000423562,ens|ENST00000438504 ...

    my @in_features  = @{ $ra_infeats };
    my @out_features = ();

    # We need to do the pre shoulders on the very first region
    if (scalar @in_features) {
        my $this = $in_features[0];
        push @out_features,
             pre_shoulders( $this, $rh_targets->{ $this->[2] }, 0 );
    }

    foreach my $ctr (0..($#in_features-1)) {
        my $this = $in_features[$ctr];
        my $next = $in_features[$ctr+1];
        print "[$ctr] Processing: ", as_key($this),',',$this->[2],
                              ' - ', as_key($next),',',$next->[2], "\n"
            if ($VERBOSE > 2);

        # If the baits are from different sequences, then we must be
        # finishing one and starting the next and this is a special case
        # where we set an arbitrarily large "next" location to force all
        # of the $this after shoulders to get allocated, and then set 0
        # as the min_pos for $next to allow for pre shoulders

        if ($this->[0] ne $next->[0]) {
            print '  End of sequence: ', $this->[0], "\n" if ($VERBOSE > 2);
            push @out_features, $this;
            push @out_features,
                 post_shoulders( $this, $rh_targets->{ $this->[2] },
                                 $this->[4]+1000000000, );
            push @out_features,
                 pre_shoulders( $next, $rh_targets->{ $next->[2] }, 0 );
            next;
        }

        # Work out whether the distance between the two features is
        # (a) anything at all and (b) enough for all of the shoulders.
        # If it is big enough the allocating is trivial.  If it's smaller
        # that the required shoulder gap then we will need to use the
        # feature priorities to decide who gets to put in their
        # shoulders first.  Remember that for feature priorities, smaller
        # numbers are "better" than larger ones and get to allocate their
        # shoulders first.

        my $shoulders = $rh_targets->{ $this->[2] }->{after_bases} +
                        $rh_targets->{ $next->[2] }->{before_bases};
        my $gap = $next->[3] - $this->[4] + 1;   # gap between 7 and 12 is 4

        # If there's no room to allocate shoulders then move on
        if ($gap <= 1) {
            push @out_features, $this;
            next;
        }

        if ($gap >= $shoulders) {
            # No problems - start assigning shoulders;
            print "  Plenty of room [gap:$gap,shoulders:$shoulders]\n"
                if ($VERBOSE > 2);
            push @out_features, $this;
            push @out_features, 
                 post_shoulders( $this, $rh_targets->{ $this->[2] },
                                 $next->[3]-1, );
            push @out_features,
                 pre_shoulders( $next, $rh_targets->{ $next->[2] },
                                $out_features[-1]->[4]+1 );
        }
        else {
            # Special case - use priorities to allocate shoulders
            print "  Gap too small [gap:$gap,shoulders:$shoulders]\n"
                if ($VERBOSE > 2);
            push @out_features, $this;

            # Work out which priority scenario we are looking at:
            # 1.  $this.priority == $next.priority
            # 2.  $this.priority >  $next.priority
            # 3.  $this.priority <  $next.priority

            if ( $rh_targets->{ $this->[2] }->{priority} ==
                 $rh_targets->{ $next->[2] }->{priority} ) {
                push @out_features,
                     alternate_shoulders( $this, $next,
                                          $rh_targets->{ $this->[2] } );
                print "    Alternate shoulders\n" if ($VERBOSE > 2);
            }
            elsif ( $rh_targets->{ $this->[2] }->{priority} <
                    $rh_targets->{ $next->[2] }->{priority} ) {
                # Do the $this "post" then $next "pre" shoulders
                push @out_features, 
                     post_shoulders( $this, $rh_targets->{ $this->[2] },
                                     $next->[3]-1, );
                push @out_features,
                     pre_shoulders( $next, $rh_targets->{ $next->[2] },
                                    $out_features[-1]->[4]+1 );
                print "    Post then Pre\n" if ($VERBOSE > 2);
            }
            elsif ( $rh_targets->{ $this->[2] }->{priority} >
                    $rh_targets->{ $next->[2] }->{priority} ) {
                # Pre-then-post is a weird case because even though you
                # calculate the pres first, you can't add them to
                # @out_featuers until after you have allocated the posts
                # so you have to remembers the pres.
                my @new_pres = 
                   pre_shoulders( $next, $rh_targets->{ $next->[2] },
                                  $this->[4]+1 );
                my $first_assigned = $next->[3]-1;   
                $first_assigned = $new_pres[0]->[3]-1 if (@new_pres);

                # Do the "post" shoulders for $this second
                push @out_features, 
                     post_shoulders( $this, $rh_targets->{ $this->[2] },
                                     $first_assigned );
                push @out_features, @new_pres;
                print "    Pre then Post\n" if ($VERBOSE > 2);
            }
        }
    }

    # Print out final feature and any post-shoulders
    my $next = $in_features[-1];
    push @out_features,
         $next,
         post_shoulders( $next, $rh_targets->{ $next->[2] },
                         $next->[4]+1000000000 );

    # Return all the shiny new shoulders
    return \@out_features;
}


# This routine will output post-shoulders up to the maximum base
# position given by $max_pos and returns an array of new features.

sub post_shoulders {
    my $feature     = shift;  # feature to have post shoulders added
    my $target      = shift;  # details of shoulder for this feature-type
    my $max_pos     = shift;  # biggest base we can allocate to

    my @new_features = ();
    my @lengths = @{ $target->{after} };

    # Check whether there's any space to allocate any shoulders
    return @new_features if ($max_pos <= $feature->[4]);

    if (scalar(@lengths) > 0) {
        my $start = $feature->[4] + 1;
        foreach my $post_ctr (0..$#lengths) {

            my $new_label = join('_', $feature->[2], ($post_ctr + 1),
                                      $lengths[$post_ctr] );
            my $end = $start + $lengths[$post_ctr] - 1;
            # Truncate $end if it would run past $max_pos
            $end = $max_pos if ($end > $max_pos);

            # Initialise new feature
            my @fields = ( $feature->[0],
                           "simple_segmenter.pl[v$REVISION]",
                           $new_label,
                           $start,
                           $end,
                           $feature->[5],
                           $feature->[6],
                           $feature->[7],
                           'record=' . $new_label );

            # Save the new feature
            push @new_features, \@fields;

            # We're out of here if we've hit our max allowable position
            last if ($end == $max_pos);

            $start = $start + $lengths[$post_ctr];
        }
    }

    return @new_features;
}


# This routine will output pre-shoulders up to the minimum base
# position given by $min_pos and returns an array of new features.

sub pre_shoulders {
    my $feature     = shift;  # feature to have pre shoulders added
    my $target      = shift;  # details of shoulder for this feature-type
    my $min_pos     = shift;  # smallest base we can allocate to

    my @new_features = ();
    my @lengths = @{ $target->{before} };
    
    # Check whether there's space to allocate any shoulders
    return @new_features if ($min_pos >= $feature->[3]);

    if (scalar(@lengths) > 0) {
        my $end = $feature->[3] - 1;
        foreach my $pre_ctr (0..$#lengths) {
            my $new_label = join('_', $feature->[2], ($pre_ctr + 1),
                                      $lengths[$pre_ctr] );
            my $start = $end - $lengths[$pre_ctr] + 1;
            # Truncate $start if it would run before $min_pos
            $start = $min_pos if ($start < $min_pos);

            # Initialise new feature
            my @fields = ( $feature->[0],
                           "simple_segmenter.pl[v$REVISION]",
                           $new_label,
                           $start,
                           $end,
                           $feature->[5],
                           $feature->[6],
                           $feature->[7],
                           'record=' . $new_label );

            # Push each new feature onto the start of the list
            unshift @new_features, \@fields;

            # We're out of here if we've hit our min allowable position
            last if ($start == $min_pos);

            $end = $end - $lengths[$pre_ctr];
        }
    }

    return @new_features;
}


# Special case only for two features of the same priority and that are
# too close to assign all the shoulders.  Bugger.

sub alternate_shoulders {
    my $this        = shift;
    my $next        = shift;
    my $target      = shift;

    my @posts        = @{ $target->{after} };   # lengths of afters
    my @pres         = @{ $target->{before} };  # lengths of befores
    my @new_posts    = ();  # new after features
    my @new_pres     = ();  # new before features
    my @new_features = ();  # all new features

    # I am going to attempt to somewhat balance the number of bases given to pre
    # and post by tracking the bases assigned and always giving the next
    # shoulder to the side with least bases.  Not sure this completely makes 
    # sense but I can't see any logical way to make a better choice.

    my $pre_ctr  = 1;
    my $post_ctr = 1;
    my $min_pos  = $this->[4] + 1;
    my $max_pos  = $next->[3] - 1;
    my $start    = $min_pos;
    my $end      = $max_pos;

    my $pre_base_ctr  = 0;
    my $post_base_ctr = 0;

    while ((scalar(@pres) + scalar(@posts)) > 0) {
        # If no more pres or if there are and there are posts and the
        # post is smaller, then allocate a post
        if (! scalar(@pres) or
            (scalar(@posts) and $post_base_ctr <= $pre_base_ctr)) {


            my $feature_length = shift @posts;
            my $new_label = join('_', $this->[2], $post_ctr++,
                                      $feature_length );
            $start = $min_pos;
            $end   = $start + $feature_length - 1;
            # Truncate $end if it would run past $max_pos
            $end = $max_pos if ($end > $max_pos);
            print "      assigning a post [$new_label,$start,$end]\n"
                if ($VERBOSE > 2);
            $post_base_ctr += ( $end-$start+1 );           

            # Initialise new post feature
            my @fields = ( $this->[0],
                           "simple_segmenter.pl[v$REVISION]",
                           $new_label,
                           $start,
                           $end,
                           $this->[5],
                           $this->[6],
                           $this->[7],
                           'record=' . $new_label );

            # Push the new post feature onto the end of the list
            push @new_posts, \@fields;
            # Set new minimum position
            $min_pos = $end + 1;

            # We're finished if we've hit our max allowable position
            last if ($end == $max_pos);
        }

        # If no more posts or if there are and there are pres and the
        # pre is smaller, then allocate a pre
        elsif (! scalar(@posts) or
               (scalar(@pres) and $pre_base_ctr <= $post_base_ctr)) {

            my $feature_length = shift @pres;
            my $new_label = join('_', $next->[2], $pre_ctr++,
                                      $feature_length );

            $end   = $max_pos;
            $start = $end - $feature_length + 1;
            # Truncate $start if it would run before $min_pos
            $start = $min_pos if ($start < $min_pos);
            print "      assigning a pre [$new_label,$start,$end]\n"
                if ($VERBOSE > 2);
            $pre_base_ctr += ( $end-$start+1 );           

            # Initialise new pre feature
            my @fields = ( $next->[0],
                           "simple_segmenter.pl[v$REVISION]",
                           $new_label,
                           $start,
                           $end,
                           $next->[5],
                           $next->[6],
                           $next->[7],
                           'record=' . $new_label );

            # Push the new pre feature onto the start of the list
            unshift @new_pres, \@fields;
            # Set new maximum position
            $max_pos = $start - 1;

            # We're out of here if we've hit our min allowable position
            last if ($start == $min_pos);
        }
        else {
            die "FATAL: Logic error - should not be able to get here!";
        }
    }

    # Save all the new features
    push @new_features, @new_posts, @new_pres;

    return @new_features;
}


sub mega_merge {
    my $ra_infeats    = shift;
    my $subfeat_flag  = shift || 0;
    my $overlap_flag  = shift || 0;
    my $adjacent_flag = shift || 0;

    my @in_features  = @{ $ra_infeats };
    my $last_in_feat = $#in_features;
    my @out_features = ();
    my $overlaps     = 0;
    my $subfeats     = 0;
    my $adjacents    = 0;

    # If 0 or 1 record, return immediately
    if (scalar(@in_features) < 2) {
        warn "No merging necessary - $last_in_feat records";
        return $ra_infeats;
    }

    my $ctr = 0;
    my $this = $in_features[0];

    foreach my $ctr (1..$last_in_feat) {

        my $next = $in_features[$ctr];

        # If different chroms or feature types then no merging is possible
        # so move on
        if (($this->[0] ne $next->[0]) or ($this->[2] ne $next->[2])) {
            push @out_features, $this;
            $this = $next;
            next;
        }

        my $probflag = 0;
        my $probmsg  = '';

        # We have 3 scenarios that we will potentially merge:
        # 1. Subfeature - one region is entirely within the next
        # 2. Overlap - the 2 regions overlap
        # 3. Adjacent - there are 0 bases between the regions

        # 1. Subfeature - one feature is entirely within the other

        if ( $subfeat_flag and
             ( ($this->[3] <= $next->[3]) and ($this->[4] >= $next->[4]) )
             or
             ( ($this->[3] >= $next->[3]) and ($this->[4] <= $next->[4]) )
           ) {
            $subfeats++;
            $probflag = 1;
            $probmsg  = 'subfeature';
        }

        # Overlap - merge if end of the first region is >= the start of next

        elsif ($this->[4] > ($next->[3]-1)) {
            $overlaps++;
            $probflag = 1;
            $probmsg  = 'overlap';
        }

        # Adjacent - end of the first region is immediately before start of next

        elsif ($this->[4] == ($next->[3]-1)) {
            $adjacents++;
            $probflag = 1;
            $probmsg  = 'adjacent';
        }

        if ($probflag) {
            print "Merging $probmsg: ", as_key($this),',',$this->[2], ' - ',
                                        as_key($next),',',$next->[2], "\n"
                if ($VERBOSE > 1);
            # Start point of $this becomes the smaller of the starts of
            # the 2 regions and the end becomes the larger of the 2
            # regions so just change $this based on $next.
            $this->[3] = $next->[3] if ($next->[3] < $this->[3]);
            $this->[4] = $next->[4] if ($next->[4] > $this->[4]);
        }
        else {
            # If regions were disjoint then save the first and repeat
            push @out_features, $this;
            $this = $next;
        }
    }

    # We always need to save the last record
    push @out_features, $this;

    print "Merged : $overlaps overlaps, $subfeats subfeatures and $adjacents adjacents",
          '  (inputs:', scalar(@in_features),',outputs:',
                        scalar(@out_features),")\n" if $VERBOSE;

    # Return the new sleeker merged array
    return \@out_features;
}



sub fill {
    my $ra_infeats = shift;
    my $bounds     = shift;

    my @in_features  = @{ $ra_infeats };
    my @out_features = ();
    my $fillctr      = 0;
    my %found_seqs   = ();

    # We need to do the pre-fill (if any) on the first region
    if (scalar @in_features) {
        my $this = $in_features[0];
        if ($this->[3] > 1) {
            # Initialise new post feature
            my @fields = ( $this->[0],
                           "simple_segmenter.pl[v$REVISION]",
                           'fill',
                           1,
                           $this->[3] - 1,
                           '.',
                           '.',
                           '.',
                           'record=fill' );
            push @out_features, \@fields;
            $fillctr++;
        }
        # Keep a track of the sequences we have seen
        $found_seqs{ $this->[0] } = 1; 
    }

    foreach my $ctr (0..($#in_features-1)) {
        my $this = $in_features[$ctr];
        my $next = $in_features[$ctr+1];
        # Keep a track of the sequences we have seen
        $found_seqs{ $this->[0] } = 1; 
        print "[$ctr] FIlling: ", as_key($this), ' - ', as_key($next), "\n"
            if ($VERBOSE > 2);

        # If the features are from different sequences, then we must be
        # finishing one sequence and starting the next so do 2 fills
        if ($this->[0] ne $next->[0]) {
            print '  End of sequence: ', $this->[0], "\n" if ($VERBOSE > 2);

            push @out_features, $this;
            # Create record to fill end of current sequence, but only if
            # the sequence appears in our list of fill contigs
            if (exists $OUR_BOUNDS->{ $this->[0] }) {
                if ($this->[4] < $OUR_BOUNDS->{ $this->[0] }->[1] ) {
                    my @fields = ( $this->[0],
                                   "simple_segmenter.pl[v$REVISION]",
                                   'fill',
                                   $this->[4] + 1,
                                   $OUR_BOUNDS->{ $this->[0] }->[1],
                                   '.',
                                   '.',
                                   '.',
                                   'record=fill' );
                    push @out_features, \@fields;
                }
            }
            else { 
                print 'No length found so cannot end-fill: ',$this->[0],"\n";
            }

            # Create record to fill start of next sequence
            if ($next->[3] > 1) {
                my @fields = ( $next->[0],
                               "simple_segmenter.pl[v$REVISION]",
                               'fill',
                               1,
                               $next->[3] - 1,
                               '.',
                               '.',
                               '.',
                               'record=fill' );
                push @out_features, \@fields;
            }

            $fillctr += 2;
            next;
        }
        elsif ($this->[4] != $next->[3]-1) {
            # Fill required
            push @out_features, $this;
            my @fields = ( $this->[0],
                           "simple_segmenter.pl[v$REVISION]",
                           'fill',
                           $this->[4] + 1,
                           $next->[3] - 1,
                           '.',
                           '.',
                           '.',
                           'record=fill' );
            push @out_features, \@fields;
            $fillctr++;
        }
        else {
            # No fill required so move on
            push @out_features, $this;
        }
    }

    # Now we need one extra pass - if there are no features from one of
    # the sequences in the hash then there will be no fill generated.
    # In this case we need to have a special "all" fill region added
    # which covers the entire sequence from base 1 to base n.

    foreach my $seqname (sort keys %{ $OUR_BOUNDS }) {
        if (! exists $found_seqs{ $seqname }) {
            my @fields = ( $seqname,
                           "simple_segmenter.pl[v$REVISION]",
                           'fill',
                           1,
                           $OUR_BOUNDS->{ $seqname }->[1],
                           '.',
                           '.',
                           '.',
                           'record=fill' );
            push @out_features, \@fields;
        }
    }

    print "Filled: $fillctr",
          '  (inputs:', scalar(@in_features),',outputs:',
                        scalar(@out_features),")\n" if $VERBOSE;

    # Return the new fatter filled array
    return \@out_features;
}


sub write_gff3 {
    my $outfile     = shift;
    my $ra_features = shift;
    my $commandline = shift;

    my $outfh = IO::File->new( $outfile, 'w' );
    die("Cannot open output file $outfile for writing: $!\n")
        unless defined $outfh;

    # Print a header
    $outfh->print( "##gff-version 3\n",
                   "# Created by: simple_segmenter.pl[v$REVISION]\n",
                   '# Created on: ' . localtime() . "\n",
                   "# Commandline: $commandline\n" );

    foreach my $feat (@{ $ra_features }) {
        # Do bounds checking if appropriate
        if (defined $OUR_BOUNDS) {
            if ($feat->[3] > $OUR_BOUNDS->{ $feat->[0] }->[1]) {
                warn 'dropping feature outside bounds: ',
                     join(',',@{$feat}[0,2,3,4]),"\n";
                next;
            }
            if ($feat->[4] > $OUR_BOUNDS->{ $feat->[0] }->[1]) {
                warn 'truncating feature outside bounds: ',
                     join(',',@{$feat}[0,2,3,4]),"\n";
                $feat->[4] = $OUR_BOUNDS->{ $feat->[0] }->[1];
            }
        }
        $outfh->print( join("\t",@{$feat}), "\n" );
#        warn 'end < start! ',join(',',@{$feat}),"\n"
#            if ($feat->[4] < $feat->[3]);
#        warn 'end == start! ',join(',',@{$feat}),"\n"
#            if ($feat->[4] == $feat->[3]);
    }

    $outfh->close;
}

sub as_key {
    my $feat = shift;
    return $feat->[0] .':'. $feat->[3] .'-'. $feat->[4];
}


__END__

=head1 NAME

simple_segmenter.pl - Add shoulders to GFF3 features


=head1 SYNOPSIS

 simple_segmenter.pl -i <infile> -o <outfile> [options]


=head1 ABSTRACT


=head1 OPTIONS

 -i | --infile        input GFF3 file of features
 -o | --outfile       output GFF3 file of features
 -f | --feature       feature description (name,shoulder,...)
 -v | --verbose       print progress and diagnostic messages
 -g | --merge         merge adjacent region with same type
 -l | --fill          fill all unassigned bases into off-target regions
 -b | --bounds        the name of a boundary set (reqd for --fill)
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


Values for B<-f> look like 'bait,100,100,100' which will pull 3 100 base
shoulders on either side of every bait region in the input GFF file.

The only 2 valid values for bounds are GRCh37 and mm64.  If you don't
specify a value for B<-b> with B<--fill> then there is no way for the
script to fill in the final fill region which extends to the end of the
chromosome.


=head1 AUTHOR

=over 2

=item John Pearson, L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: simple_segmenter.pl 4667 2014-07-24 10:09:43Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2014

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
