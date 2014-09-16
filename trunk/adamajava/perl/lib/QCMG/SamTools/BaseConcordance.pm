package QCMG::SamTools::BaseConcordance;

##############################################################################
#
#  Module:   QCMG::SamTools::BaseConcordance
#  Creator:  Lynn Fink
#  Created:  2011-04-29
#
# This class contains methods for determining attributes of reads, including
# deconvoluting their CIGAR string to return a base-for-base position
# concordance with the reference genome.
#
#  $Id: BaseConcordance.pm 4665 2014-07-24 08:54:04Z j.pearson $
#
##############################################################################

=pod

=head1 NAME

QCMG::SamTools::BaseConcordance -- Methods for determining mapped read attributes

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::SamTools::BaseConcordance->new();

=head1 DESCRIPTION

=head1 REQUIREMENTS

 Exporter

=cut

use strict;

# standard distro modules
use Data::Dumper;

use vars qw( $SVNID $REVISION $VERBOSE );

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 none

Returns:
 a new instance of this class.

=cut

sub new {
	my $class = shift @_;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $self = {};
	bless ($self, $class);

        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$VERBOSE = 1 if($options->{'VERBOSE'});

	if($options->{'READ'}) {
		my $a			= $options->{'READ'};

	    	$self->{'name'}		= $a->name;		# scalar (string)
		$self->{'primary_tag'}	= $a->primary_tag;	# scalar (string): match|read_pair|coverage|region/chromosome
		$self->{'seq_id'}	= $a->seq_id;		# scalar (string)
		$self->{'start'}	= $a->start;		# scalar (int)
		$self->{'end'}		= $a->end;		# scalar (int)
		$self->{'length'}	= $a->length;		# scalar (int)
		$self->{'strand'}	= $a->strand;		# scalar (bool): 1 or -1
		$self->{'mstrand'}	= $a->mstrand;		# scalar (bool): 1 or -1
		$self->{'qname'}	= $a->query->name;	# scalar (string)
		$self->{'qstart'}	= $a->query->start;	# scalar (int)
		$self->{'qend'}		= $a->query->end;	# scalar (int)
		$self->{'qlength'}	= $a->query->length;	# scalar (int)
		$self->{'qdna'}		= $a->query->dna;	# scalar (string)
		$self->{'qscore'}	= $a->qscore;		# array, each element is the quality score for each position (int)
		$self->{'qseq'}		= $a->qseq;		# scalar (string)
		$self->{'tags'}		= $a->get_all_tags;	# array
		$self->{'cigar'}	= $a->cigar_str;	# scalar (string)
		$self->{'match_qual'}	= $a->qual;		# scalar (int)

		#print Dumper $self;

		my $tag;
		#print Dumper $self->{'tags'};

                #foreach $tag (@{ $self->{'tags'} }) {
		#	print STDERR "Tag; $tag\n";
			#$self->{$tag} =  $a->get_tag_values($tag);
                #}

		undef $a;
	}
	else {
		# user is supplying attributes here
		$self->{'name'}		= $options->{'name'};
		$self->{'primary_tag'}	= $options->{'primary_tag'};
		$self->{'seq_id'}	= $options->{'seq_id'};
		$self->{'start'}	= $options->{'start'};
		$self->{'end'}		= $options->{'end'};
		$self->{'length'}	= $options->{'length'};
		$self->{'strand'}	= $options->{'strand'};
		$self->{'mstrand'}	= $options->{'mstrand'};
		$self->{'qname'}	= $options->{'qname'};
		$self->{'qstart'}	= $options->{'qstart'};
		$self->{'qend'}		= $options->{'qend'};
		$self->{'qlength'}	= $options->{'qlength'};
		$self->{'qdna'}		= $options->{'qdna'};
		$self->{'qscore'}	= $options->{'qscore'};
		$self->{'qseq'}		= $options->{'qseq'};
		#$self->{'tags'}		= $options->{'tags'};
		$self->{'cigar'}	= $options->{'cigar'};
		$self->{'match_qual'}	= $options->{'match_qual'};

		$self->{'PAIRED'}	= $options->{'PAIRED'};
		$self->{'REVERSED'}	= $options->{'REVERSED'};
		$self->{'FIRST_MATE'}	= $options->{'FIRST_MATE'};
		$self->{'SECOND_MATE'}	= $options->{'SECOND_MATE'};
		$self->{'M_UNMAPPED'}	= $options->{'M_UNMAPPED'};
		$self->{'M_REVERSED'}	= $options->{'M_REVERSED'};
		$self->{'NOT_PRIMARY'}	= $options->{'NOT_PRIMARY'};
		$self->{'UNMAPPED'}	= $options->{'UNMAPPED'};
		$self->{'MAP_PAIR'}	= $options->{'MAP_PAIR'};
		$self->{'DUPLICATE'}	= $options->{'DUPLICATE'};
		$self->{'QC_FAILED'}	= $options->{'QC_FAILED'};
	}

	return $self;
}

################################################################################
=pod

B<softclip_read()>

 removes the bases from the read sequence that are tagged as soft-clipped; 
  removes the soft-clip field from the CIGAR string to indicate that the bases 
  are gone

Parameters:
 none

Requires:
 'cigar' is defined
 'qseq'  is defined

Returns:
 soft-clipped 'qseq'
 adjusted CIGAR string

Sets:
 'cigar_sclipped' -> CIGAR string trimmed of S fields
 'qseq_sclipped'  -> qseq trimmed of S bases
 'qseq'           -> redefines qseq
 'cigar'	  -> redefines CIGAR string

=cut
sub softclip_read {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my @subc = ($self->{'cigar'} =~ /(\d+[M|I|D|N|S|H|P|=|x]{1})/g);

        # hard-clipping does not need to be adjusted - it should be ignored when
        # creating read-reference base concordance; it will matter, however, for
        # novel starts

	my $qseq	= $self->{'qseq'};
	my $cigar;

        if($subc[0] =~ /S$/) {
		#print STDERR "Clipping from left: $subc[0]\n";
                my $c = shift @subc;
                $c =~ /(\d+)(\w)/;
                my $nb  = $1;
                my $tag = $2;

		#print STDERR "Num bases, tag: $nb, $tag\n";

                # remove soft clipped sequence
                $qseq =~ s/(.{$nb})(.+)/$2/;
                $cigar = join "", @subc;
        }

        if($self->{'cigar'} =~ /S$/) {
                my $c = pop @subc;
		#print STDERR "Clipping from right: $c\n";
                $c =~ /(\d+)(\w)/;
                my $nb  = $1;
                my $tag = $2;

		#print STDERR "Num bases, tag: $nb, $tag\n";

                # remove soft clipped sequence
                $qseq =~ s/(.+)(.{$nb})/$1/;
                $cigar = join "", @subc;
        }

	#print STDERR "New cigar: $cigar\n";

	$self->{'qseq_sclipped'}	= $qseq;
	$self->{'qseq'}			= $qseq;
	$self->{'cigar_sclipped'}	= $cigar;
	$self->{'cigar'}		= $cigar;

        return($qseq, $cigar);
}

################################################################################
=pod

B<mask_softclip_read()>

 masks the bases from the read sequence that are tagged as soft-clipped; 
  replaces the soft-clipped reads with S in the qseq 
Parameters:
 none

Requires:
 'cigar' is defined
 'qseq'  is defined

Returns:
 soft-clip masked 'qseq'

Sets:
 'qseq_sclip_mask'  -> qseq mask with S bases

=cut
sub mask_softclip_read {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my @subc = ($self->{'cigar'} =~ /(\d+[M|I|D|N|S|H|P|=|x]{1})/g);

        # hard-clipping does not need to be adjusted - it should be ignored when
        # creating read-reference base concordance; it will matter, however, for
        # novel starts

	my $qseq	= $self->{'qseq'};
	my $cigar;

        if($subc[0] =~ /S$/) {
		#print STDERR "Clipping from left: $subc[0]\n";
                my $c = shift @subc;
                $c =~ /(\d+)(\w)/;
                my $nb  = $1;
                my $tag = $2;

		#print STDERR "Num bases, tag: $nb, $tag\n";

		my $mask	= "S" x $nb;

                # remove soft clipped sequence
                $qseq =~ s/(.{$nb})(.+)/$mask$2/;
                $cigar = join "", @subc;
        }

        if($self->{'cigar'} =~ /S$/) {
                my $c = pop @subc;
		#print STDERR "Clipping from right: $c\n";
                $c =~ /(\d+)(\w)/;
                my $nb  = $1;
                my $tag = $2;

		#print STDERR "Num bases, tag: $nb, $tag\n";

		my $mask	= "S" x $nb;

                # remove soft clipped sequence
                $qseq =~ s/(.+)(.{$nb})/$1$mask/;
                $cigar = join "", @subc;
        }

	#print STDERR "New cigar: $cigar\n";

	$self->{'qseq_sclip_mask'}	= $qseq;

        return($qseq, $cigar);
}

################################################################################
=pod

B<true_read_start()> 

 determines the genomic coordinate of the start of the read considering strand,
  and any clipping

Parameters:
 none

Requires:
 'cigar'  is defined
 'strand' is defined
 'start'  is defined
 'end'    is defined

Returns:
 true start coordinate

Sets:
 'start_true' -> genomic coordinate of true read start

=cut
sub true_read_start {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my @subc = ($self->{'cigar'} =~ /(\d+[M|I|D|N|S|H|P|=|x]{1})/g);

	my $start;
        if($self->{'strand'} < 1) {
		# - strand
                $start = $self->{'end'};

                my $c = pop @subc;
                $c =~ /(\d+)(\w)/;
                my $tag = $2;

                #if($tag eq 'H') {
                if($tag =~ /[S|H]/) {
			$start += $1;
                }
        }
        else {
		# + strand
		$start	= $self->{'start'};

                my $c = shift @subc;
                $c =~ /(\d+)(\w)/;
                my $tag = $2;

                #if($tag eq 'H') {
                if($tag =~ /[S|H]/) {
			$start -= $1;
                }
        }

	$self->{'start_true'}	= $start;

        return($start);
}

################################################################################
=pod

B<deconvolute_cigar()> 

 generates a base-to-base concordance between the read coordinates and the
  reference genome coordinates, accounting for insertions, deletions, and
  introns

Parameters:
 none

Requires:
 'cigar'  is defined
 'pos'    is defined

Returns:
 hash of coordinate concordances

Sets:
 'base_cc' -> hash of coordinate concordances

=cut
sub deconvolute_cigar {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my @subc = ($self->{'cigar'} =~ /(\d+[M|I|D|N|S|H|P|=|x]{1})/g);

	if($self->{'cigar'} =~ /[S]/) {
		$self->softclip_read();
	}

        my $end         = $1;
        my $start       = 1;

        my $j           = 1;    		# read pos
        my $i           = $self->{'start'};	# adjust so coords match to ref genome
        my @refpos      = ();
        my @readpos     = ();

	#$VERBOSE = 1;

        #print STDERR "$self->{'cigar'}\n$start, $end, $i, $j\n" if($VERBOSE);

        foreach (@subc) {
                my ($numbases, $type)           = /(\d+)(\w)/;

                $end    =  $start + ($numbases - 1);

		#print "Looping from $start to $end\n";
		#print "Refseq index starts at $i\n";
		#print "Read   index starts at $j\n";
                for my $index ($start..$end) {
			# direct mapping of positions to reference if matches or
			# mismatches
			if($type =~ /[M|=|X]/i) {
			        $refpos[$index]         = $i++;
			        $readpos[$index]        = $j++;
			        #print "$refpos[$index], $readpos[$index]\n" if($VERBOSE);
			}
			# add offset to account for insertions
			elsif($type =~ /[I]/i) {
				$refpos[$index]		= '';
				$readpos[$index]	= $j++;
				print STDOUT "$refpos[$index], $readpos[$index]\n" if($VERBOSE);
			}
			# add offset to account for introns
			elsif($type =~ /[N]/i) {
				$refpos[$index]		= $i++;
				$readpos[$index]	= '';
				print STDOUT "$refpos[$index], $readpos[$index]\n" if($VERBOSE);
			}
			# add offset to account for deletions
			elsif($type =~ /[D]/i) {
			        $refpos[$index]         = $i++;
			        $readpos[$index]        = '';
			        #print "$refpos[$index], $readpos[$index]\n" if($VERBOSE);
			}
			#elsif($type =~ /[P]i/) {
			        # ???
			#       print STDERR "Padded reference sequence
			#       encountered: $cigar\n";
			#}
                }

                $start  += $numbases;
        }

        my %coordmap;
        for my $index (1..$#refpos) {
		#next if(! $refpos[$index] && ! $readpos[$index]);
                $coordmap{$refpos[$index]} = $readpos[$index];
		#push @{$arraymap[$refpos[$index]]}, $readpos[$index];
        }

	$self->{'base_cc'}	= \%coordmap;

        return (\%coordmap, \@refpos, \@readpos);
}

################################################################################
=pod

B<get_read_base()> 

 Get base at a specific genomic coordinate in a mapped read, after the read
  positions have been adjusted by CIGAR string
 
Parameters:
 POS - single base coordinate (int) 

Requires:
 'base_cc' is defined (deconvolute_cigar() has been called)
 'qseq' is defined

Returns:
 single base (A|C|G|T, scalar char)

Sets:

=cut
sub get_read_base {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
	my $sub		= (caller(0))[3];

        # read params from @_ and put in %options
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }
	my $pos		= $options->{'POS'};

	# if no position is specified, complain and return void
	if(! $pos) {
		#print STDERR "$sub: No position specified\n";
		return();
	}

	# if deconvolute_cigar() has not been run yet, run it to get the
	# base-to-base mapping
	if(! $self->{'base_cc'}) {
		$self->deconvolute_cigar();
	}

=pod

chr1:9770588-9770670
REFBASE: GCTGCCCACAGGGGTCTACCTGAACTTCCCTGTGTCCCGCAATGCCAACCTCAGCACCATCAAGCAGGTATGGCCTCCATCCG
S0FUE:11:529;0;9770588;9770670;1        (chr1 : 9770588 - 9770670, +):  83M
83M
1, 83M, , 1
9770588 -> 1 (0): G G
9770589 -> 2 (1): C C
9770590 -> 3 (2): T T
9770591 -> 4 (3): G G
9770592 -> 5 (4): C C
9770593 -> 6 (5): C C
...
base_cc key    base_cc value  qpos (base_cc value - 1)  reference genome base  read base
9770593     -> 6             (5):                       C                      C

=cut

	# get base using base-to-base coordinates
	my $qpos;
	if($self->{'base_cc'}->{$pos}) {
	        # subtract 1 to account for Perl's 0-based system
	        $qpos = $self->{'base_cc'}->{$pos} - 1;
	        #print STDERR "New pos: $qpos (from $pos)\n" if($VERBOSE);
	}
	else {
	        # read does not map to this genomic coordinate, only
	        # maps across it
	        #print STDERR "no mapping to this base\n" if($VERBOSE);
		return();
	}

	my $base	= substr($self->{'qseq'}, $qpos, 1);
	my $qscore	= $self->{'qscore'}->[$qpos];

	#print "BASE: $base, QSCORE: $qscore\n";

	#print join "\t", split //, $self->{'qseq'}, "\n";
	#print join "\t", @{$self->{'qscore'}}, "\n";

	return($base, $qscore);
	#return($base);
}

################################################################################
=pod

B<get_masked_read_base()> 

 Get base at a specific genomic coordinate in a mapped read, after the read
  positions have been masked with soft-clips
 
Parameters:
 POS - single base coordinate (int) 

Requires:
 'base_cc' is defined (deconvolute_cigar() has been called)
 'qseq_sclip_mask' is defined

Returns:
 single base (A|C|G|T, scalar char)

Sets:

=cut
sub get_masked_read_base {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
	my $sub		= (caller(0))[3];

        # read params from @_ and put in %options
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }
	my $pos		= $options->{'POS'};

	# if no position is specified, complain and return void
	if(! $pos) {
		#print STDERR "$sub: No position specified\n";
		return();
	}

	# if deconvolute_cigar() has not been run yet, run it to get the
	# base-to-base mapping
	if(! $self->{'base_cc'}) {
		$self->deconvolute_cigar();
	}

=pod

chr1:9770588-9770670
REFBASE: GCTGCCCACAGGGGTCTACCTGAACTTCCCTGTGTCCCGCAATGCCAACCTCAGCACCATCAAGCAGGTATGGCCTCCATCCG
S0FUE:11:529;0;9770588;9770670;1        (chr1 : 9770588 - 9770670, +):  83M
83M
1, 83M, , 1
9770588 -> 1 (0): G G
9770589 -> 2 (1): C C
9770590 -> 3 (2): T T
9770591 -> 4 (3): G G
9770592 -> 5 (4): C C
9770593 -> 6 (5): C C
...
base_cc key    base_cc value  qpos (base_cc value - 1)  reference genome base  read base
9770593     -> 6             (5):                       C                      C

=cut

	# get base using base-to-base coordinates
	my $qpos;
	if($self->{'base_cc'}->{$pos}) {
	        # subtract 1 to account for Perl's 0-based system
	        $qpos = $self->{'base_cc'}->{$pos} - 1;
	        #print STDERR "New pos: $qpos (from $pos)\n" if($VERBOSE);
	}
	else {
	        # read does not map to this genomic coordinate, only
	        # maps across it
	        #print STDERR "no mapping to this base\n" if($VERBOSE);
		return();
	}

	my $base	= substr($self->{'qseq_sclip_mask'}, $qpos, 1);
	my $qscore	= $self->{'qscore'}->[$qpos];

	#print "BASE: $base, QSCORE: $qscore\n";

	#print join "\t", split //, $self->{'qseq'}, "\n";
	#print join "\t", @{$self->{'qscore'}}, "\n";

	return($base, $qscore);
	#return($base);
}

1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011-2013

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
