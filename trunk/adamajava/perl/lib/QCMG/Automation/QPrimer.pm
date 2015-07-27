package QCMG::Automation::QPrimer;

##############################################################################
#
#  Module:   QCMG::Automation::QPrimer.pm
#             (formerly QCMG::Automation::TorrentP.pm)
#  Creator:  Lynn Fink
#  Created:  2011-06-04
#
#  This class contains methods for optimizing primer for Torrent Sequencing
#
# $Id$
#
##############################################################################

=pod

=head1 NAME

QCMG::Automation::QPrimer

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Automation::QPrimer->new();

=head1 DESCRIPTION

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'

=cut

use strict;

# standard distro modules
use Data::Dumper;

# in-house modules
use lib qw(/panfs/home/lfink/devel/QCMGPerl/lib/);
use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods
use QCMG::DB::Geneus;

use vars qw( $SVNID $REVISION);
use vars qw($primer3in @tmpfiles $path_blast $path_primer3 $path_dbsnp);

$path_blast		= "/panfs/share/software/blast-2.2.24/bin";
$path_primer3		= "/panfs/share/software/primer3-2.2.3/src/";
$path_dbsnp		= "/panfs/share/dbSNP/hg19.db130.single.SNP.all";

my %BASECOMP		= ('A' => 'T', 'C' => 'G', 'G' => 'C', 'T' => 'A');

# template for primer3 input file
$primer3in = qq{SEQUENCE_ID=<SEQID>
SEQUENCE_TEMPLATE=<SEQUENCE>
SEQUENCE_TARGET=<TARGET>
PRIMER_PRODUCT_MIN_TM=65
PRIMER_PRODUCT_MAX_TM=85 
PRIMER_DNA_CONC=120.0
PRIMER_SALT_CONC=50.0  
PRIMER_MIN_TM=55
PRIMER_OPT_TM=60
PRIMER_MAX_TM=65
PRIMER_MIN_SIZE=18 
PRIMER_OPT_SIZE=20
PRIMER_MAX_SIZE=25 
PRIMER_PRODUCT_SIZE_RANGE=50-120 
PRIMER_EXPLAIN_FLAG=1
PRIMER_NUM_RETURN=<NUM_PRIMERS>
PRIMER_NUM_NS_ACCEPTED=1
=
};

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 POS	=> genomic position
 GENOME	=> reference genome .fa file
 DBSNP	=> data structure with dbSNP
 EXHAUSTIVE	=> perform exhaustive search (try more primer pairs and ignore
			low-complexity regions)

Returns:
 a new instance of this class.

=cut

sub new {
        my $that  = shift;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my $class = ref($that) || $that;
        my $self = bless $that->SUPER::new(), $class;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	# check that executables can be used
	# check for presence of primer3
	unless(-e $path_primer3) {
		print STDERR "Cannot find primer3: $path_primer3 : $!\n";
		exit(2);
	}
	# check for presence of BLAST
	unless(-e $path_blast) {
		print STDERR "Cannot find BLAST: $path_blast : $!\n";
		exit(3);
	}
	# check for presence of fetch_subsequence.pl
	#unless(-e $path_fetch) {
	#	print STDERR "Cannot find BLAST: $path_fetch : $!\n";
	#	exit(4);
	#}

	$self->{'hostname'}	= $self->HOSTKEY;

	$self->{'pos'}		= $options->{'POS'};
	$self->{'genome'} 	= $options->{'GENOME'};
	$self->{'blastgenome'} 	= $options->{'BLASTGENOME'};

	$self->get_rs_sizes();

	# define reference sequence for this position
	$self->{'pos'}		=~ /(.+?)\:(\d+)/;
	$self->{'rs'}		= $1;
	$self->{'coord'}	= $2;

	# if position is only ref sequence and start position, add end position
	# that is same as start
	unless($self->{'pos'} =~ /.+?\:\d+\-\d+/) {
		$self->{'pos'} .= "-".$self->{'coord'};
	}

	# define genomic range around position to explore; can change with
	# set_mode()
	$self->{'mode'}		= 'normal';

	# create an alias/unique ID for using in filenames, etc.
	#  chr2:3234-3234 -> chr2_3234_3234
	$self->{'alias'}	= $self->{'pos'};
	$self->{'alias'}	=~ s/\W/\_/g;

	# define length of genomic sequence to retrieve and region in primer3
	# input file where SNP is (on either side; if 250, whole range = 500)
	#$self->{'p3target'}	= 251;
	$self->{'p3target'}	= 248;
	$self->{'p3targetlen'}	= 5;
	$self->{'p3halflength'}	= 250;

	# define aximum range where primers can fall
	$self->{'leftmax'}	= $self->{'coord'} - $self->{'p3halflength'};
	$self->{'rightmax'}	= $self->{'coord'} + $self->{'p3halflength'};

	#print STDERR "PRIMER RANGE: ".$self->{'leftmax'}." - ".$self->{'rightmax'}."\n";

	# for exhaustive search (if no good pairs can be found on the first run)
	$self->{'num_primer_pairs_to_get'}	= 200;
	$self->{'allow_low_complexity'}	= 1;
	if($options->{'EXHAUSTIVE'} == 1) {
		$self->{'num_primer_pairs_to_get'}	= 5000;
		$self->{'allow_low_complexity'}	= 0;
	}

	if($options->{'DBSNP'}) {
		#print Dumper $options->{'DBSNP'};
		$self->read_dbsnp(DBSNP => $options->{'DBSNP'});
		$self->{'filter_w_dbsnp'} = 1;
	}

	return $self;
}

sub pos {
        my $self = shift @_;
	return($self->{'pos'});
}
sub rs {
        my $self = shift @_;
	return($self->{'rs'});
}
sub coord {
        my $self = shift @_;
	return($self->{'coord'});
}
sub p3target {
        my $self = shift @_;
	return($self->{'p3target'});
}
sub p3targetlen {
        my $self = shift @_;
	return($self->{'p3targetlen'});
}
sub p3halflength {
        my $self = shift @_;
	return($self->{'p3halflength'});
}
sub num_primer_pairs_to_get {
        my $self = shift @_;
	return($self->{'num_primer_pairs_to_get'});
}
sub path_blast {
        my $self = shift @_;
	return($path_blast);
}
sub path_primer3 {
        my $self = shift @_;
	return($path_primer3);
}
sub path_dbsnp {
        my $self = shift @_;
	return($path_dbsnp);
}

################################################################################
=pod

B<get_rs_sizes()> 
 Get reference sequence sizes - to make sure that SNP primer range falls on
  genome; expecting a file that is the same filename as the genome, but ends
  with .properties instead of .fa

Parameters:
 requires 'genome' and corresponding .properties file

Returns:
 sets 'rs2size' - ref to hash of RS names and lengths

=cut
sub get_rs_sizes {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $gprop	= $self->{'genome'};
	$gprop		=~ s/\.fa/\.properties/;

	#c.1.H=chr1
	#c.1.L=249250621
	#c.1.P=0

	local($/) = undef;
	open(FH, $gprop) || die "Cannot open reference genome properties in $gprop : $!\n";
	my $fc = <FH>;
	close(FH);

	my @rs = ($fc =~ /c\.\d+\.H=(.+?)\nc\.\d+\.L=(\d+)/sg);

	my %RS2SIZE;
	for (my $i=0; $i<=$#rs; $i+=2) {
		$RS2SIZE{$rs[$i]} = $rs[$i+1];
	}

	$self->{'rs2size'} = \%RS2SIZE;

        return();
}
################################################################################
=pod

B<set_mode()> 
 Set mode - normal or torrent

Parameters:
 MODE	=> normal|torrent (default = normal)

Returns:
 void

=cut
sub set_mode {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if($options->{'MODE'} =~ /torrent/i) {
		$self->{'mode'} = 'torrent';
	}
	else {
		$self->{'mode'} = 'normal';
	}

	#print STDERR "Setting mode: ".$self->{'mode'}."\n";

        return();
}

################################################################################
=pod

B<set_barcode()> 
 Define a barcode to add to the primer pair

Parameters:
 BARCODE => barcode sequence (e.g., ACTGATAAGT)

Returns:
 void

=cut
sub set_barcode {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if(! $options->{'BARCODE'}) {
		print STDERR "WARNING: no barcode specified\n";
	}
	else {
		$self->{'barcode'} = $options->{'BARCODE'};
		#print STDERR "Setting barcode: ".$self->{'barcode'}."\n";
	}

        return();
}

################################################################################
=pod

B<get_genomic_seq()> 
 Get sequence from reference genome bracketing the position

Parameters:
 requires
	'mode'
	'pos'
	'genome'
 OR override with START and END

Returns:
 scalar string of bases

=cut
sub get_genomic_seq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my @seqs	= ();
	my @targets	= ();

	# build complete genomic coordinates
	if($options->{'START'} && $options->{'END'}) {
		my $pos		= $self->{'rs'}.":".$options->{'START'}."-".$options->{'END'};
		# extract sequence from FASTA file
		my $fai         = QCMG::SamTools::Sam::Fai->load($self->{'genome'});
		my $seq         = $fai->fetch($pos);
		#print STDERR "SEQ: $seq\n";

		push @seqs, $seq;
	}
	else {
		# FIX!!! do not request areas of genome that do not
		# exist; must check with ref genome first
		my $start = $self->{'coord'} - $self->{'p3halflength'};
		my $end   = $self->{'coord'} + $self->{'p3halflength'};
		my $pos	  = $self->{'rs'}.":".$start."-".$end;

		#print STDERR "Getting genomic bracket for SNP at ".$self->{'coord'}.": $pos\n";

		#print STDERR "RS SIZE: ".$self->{'rs2size'}->{$self->{'rs'}}."\n";

		# check that range is fully on RS; if not, shift window to start
		# or end at RS start or end, maintaining window length
		if($start < 1) {
			my $adjust		= 1 - $start;
			$start			= 1;
			$end			= $start + (2 * $self->{'p3halflength'});
			print STDERR "Shifting window at start of RS: $start, $end, by $adjust\n";
			$self->{'p3target'}	= $self->{'p3halflength'} - $adjust;
			$pos	  = $self->{'rs'}.":".$start."-".$end;
			#print STDERR "New primer3 range: ".$self->{'p3target'}."\n";
		}
		if($end > $self->{'rs2size'}->{$self->{'rs'}}) {
			my $adjust		= $end - $self->{'rs2size'}->{$self->{'rs'}};
			$end = $self->{'rs2size'}->{$self->{'rs'}};
			$start = $end - (2 * $self->{'p3halflength'});
			print STDERR "Shifting window at end of RS: $start, $end, by $adjust\n";
			$self->{'p3target'}	= $self->{'p3halflength'} + $adjust;
			$pos	  = $self->{'rs'}.":".$start."-".$end;
			#print STDERR "New primer3 range: ".$self->{'p3target'}."\n";
		}

		#print STDERR "Getting genomic bracket: $pos\n";
		# one range to get and process
		my $fai         = QCMG::SamTools::Sam::Fai->load($self->{'genome'});
		my $seq         = $fai->fetch($pos);

		#print STDERR "SEQ: $seq\n";
		push @seqs, $seq;
		push @targets, $self->{'p3target'}.",".$self->{'p3targetlen'};
	}

	#print Dumper @seqs;
	#print Dumper @targets;

        return(\@seqs, \@targets);
}

################################################################################
=pod

B<create_primer3_input()> 
 Write primer3 input file

Parameters:
 SEQ	=> string of bases

Returns:
 filename

=cut
sub create_primer3_input {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	# create unique filename for each file
	srand;
	my $rand = int(rand(1000));
	my $tmpfile = "/tmp/".$self->{'alias'}."_p3.in";
	if(-e $tmpfile) {
		until(! -e $tmpfile) {
			$tmpfile .= "_".$rand;
		}
	}

	my $fc = $primer3in;

	$fc =~ s/<SEQID>/$self->{'alias'}/;
	$fc =~ s/<SEQUENCE>/$options->{'SEQ'}/;
	$fc =~ s/<TARGET>/$options->{'TARGET'}/;
	$fc =~ s/<NUM_PRIMERS>/$self->{'num_primer_pairs_to_get'}/;

	# write to file
	open(FH, ">".$tmpfile) || die "Cannot write to $tmpfile: $!\n";
	print FH $fc;
	close(FH);
	push @tmpfiles, $tmpfile;	# keep track of tmpfiles
	#print STDERR "TEMPFILE: Writing $tmpfile\n";

        return($tmpfile);
}
################################################################################
=pod

B<run_primer3()> 
 Run primer3 with input file

Parameters:
 INFILE		=> input file with sequence and range

Returns:
 scalar name of output file

=cut
sub run_primer3 {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $errfile	= $options->{'INFILE'}.".err";
	my $outfile	= $options->{'INFILE'}.".p3.out";

	# -output=/path/to/output/file -error=/path/to/error/file primer3_input_file
	my $cmd = qq{$path_primer3/primer3_core -output=$outfile };
	$cmd .= qq{-error=$errfile $options->{'INFILE'} };

	print STDERR "Running primer3: $cmd\n";

	system($cmd);

	push @tmpfiles, $errfile;
	push @tmpfiles, $outfile;

        return($outfile);
}
################################################################################
=pod

B<evaluate_pairs()> 
 Evaluate all pairs and keep the ones that pass the basic criteria

Parameters:
 OUTFILE	=> output file name

Returns:
 ref to array of pairs (in primer3 output format)

=cut
sub evaluate_pairs {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	local($/) = undef;
	open(FH, $options->{'OUTFILE'}) || die "Cannot open $options->{'OUTFILE'} : $!\n";
	my $fc = <FH>;
	close(FH);
	push @tmpfiles, $options->{'OUTFILE'};
	#print STDERR "TEMPFILE: ".$options->{'OUTFILE'}."\n";

	$fc =~ /PRIMER_PAIR_NUM_RETURNED=(\d+)/;
	my $num_returned = $1;

	if($num_returned == 0) {
		print STDERR "No primer pairs returned...?\n";
	}
	else {
		print STDERR "$num_returned pairs found\n";
	}

	# get genomic sequence used as template
	$fc =~ /SEQUENCE_TEMPLATE=(\w+)/;
	my $genseq = $1;

	# get primer pairs
	my @pairs = ($fc =~ /(PRIMER_PAIR_\d+_PENALTY.+?PRIMER_PAIR_\d+_T_OPT_A=[\d\.]+)/sg);

	# check pairs
	foreach (@pairs) {
		#print STDERR "PAIR:--------------\n";

		# check primer pair Tm
		/PRIMER_LEFT_(\d+)_TM=(.+)/;
		my $pid		= $1;	# primer ID number
		my $pl_tm	= $2;
		/PRIMER_RIGHT_\d+_TM=(.+)/;
		my $pr_tm	= $1;
		my $dtm		= abs($pl_tm - $pr_tm);
		if(abs($pl_tm - $pr_tm) > 3.1) {
			print STDERR "PAIR $pid: BAD dTM ($dtm), skipping\n";
			#print STDERR "$pl_tm - $pr_tm = $dtm\n";
			next;
		}

		# get primer coordinates
		/PRIMER_LEFT_\d+=(\d+),(\d+)/;
		my $pl_loc	= $1;
		my $pl_rng	= $2;
		/PRIMER_RIGHT_\d+=(\d+),(\d+)/;
		my $pr_loc	= $1;
		my $pr_rng	= $2;

		# get genomic coordinate of left primer start and end
		#   30          =   251               -  221
		my $pl_d	= $self->{'p3target'} - $pl_loc;
		# 8445525       = 8445555          -  30
		my $pl_l	= $self->{'coord'} - $pl_d;
		# 8445545       = 8445525 + 20
		my $pl_e	= $pl_l + $pl_rng;
		# get genomic coordinate of right primer start and end
		my $pr_d	= $pr_loc - $self->{'p3target'};
		my $pr_l	= $self->{'coord'} + $pr_d;
		my $pr_e	= $pr_l + $pr_rng;

		my $pl_coord	= $self->{'rs'}.":".$pl_l."-".$pl_e;
		my $pr_coord	= $self->{'rs'}.":".$pr_l."-".$pr_e;

		#PRIMER_LEFT_1_COORD=chr17:8445494-8445514
		#PRIMER_RIGHT_1_COORD=chr17:8445591-8445616
		$_		.= "\nPRIMER_LEFT_".$pid."_COORD=".$pl_coord."\n";
		$_		.= "PRIMER_RIGHT_".$pid."_COORD=".$pr_coord."\n";

		#print STDERR "Primer regions:\n\t$pl_coord\n\t$pr_coord\n";

		# check if pair overlaps a SNP, if requested
		if($self->{'filter_w_dbsnp'} == 1) {
			my $status = 0;
			#print STDERR "Filtering by dbSNP\n";

			#print Dumper $self->{'snps'};

			foreach my $snp (@{$self->{'snps'}}) {
				#print STDERR "Checking: $snp >= $pl_l && $snp <= $pl_e || $snp >= $pr_l && $snp <= $pr_e\n";
				# if SNP falls in a primer
				if($snp >= $pl_l && $snp <= $pl_e || $snp >= $pr_l && $snp <= $pr_e) {
					print STDERR "BAD: dbSNP SNP in primer\n";
					$status = 1;
					last;
				}
			}

			if($status == 1) {
				next;
			}
		}

		# check if either primer is within 40 bp of position
		if($self->{'mode'} eq 'torrent') {
			#print STDERR "Checking: ".$self->{'p3target'}." - $pl_loc < 40 || $pr_loc - ".$self->{'p3target'}." < 40\n";
			if($self->{'p3target'} - $pl_loc < 40 || $pr_loc - $self->{'p3target'} < 40) {
				#print STDERR "Primer within 40bp of SNP\n";
			}
			else {
				next;
			}
		}

		# make sure that primer does not contains SNP (shouldn't happen,
		# but check just in case)
		#print STDERR "PRIMER LEFT END: $pl_e\nPRIMER RIGHT START: $pr_l\n";
		#print STDERR "SNP: $self->{'coord'}\n";
		#print STDERR "$pl_e >= $self->{'coord'} || $pr_l <= $self->{'coord'}\n";
		if($pl_e >= $self->{'coord'} || $pr_l <= $self->{'coord'}) {
			print STDERR "SNP contained in primer, skipping\n";
			next;
		}

		push @{$self->{'pairs'}}, $_;
	}

	return(\@{$self->{'pairs'}});
}
################################################################################
=pod

B<create_blast_input()> 
 Create input file for BLAST

Parameters:
 PAIR	=> primer3 formatted pair data

Returns:
 BLAST input filenames for left and right primers

=cut
sub create_blast_input {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair = $options->{'PAIR'};

	$pair		=~ /PRIMER_(LEFT_\d+)_SEQUENCE=(.+)/;
	my $leftid	= $1;
	my $leftseq	= $2;
	$pair		=~ /PRIMER_(RIGHT_\d+)_SEQUENCE=(.+)/;
	my $rightid	= $1;
	my $rightseq	= $2;

	print STDERR "Writing BLAST files for $leftid and $rightid\n";

	my $l_bl_file	= "/tmp/".$self->{'alias'}."_".$leftid.".fa";
	my $r_bl_file	= "/tmp/".$self->{'alias'}."_".$rightid.".fa";

	open(FH, ">".$l_bl_file) || die "Cannot open $l_bl_file : $!\n";
	print FH ">$leftid\n$leftseq\n";
	close(FH);
	push @tmpfiles, $l_bl_file;
	#print STDERR "TEMPFILE: $l_bl_file\n";

	open(FH, ">".$r_bl_file) || die "Cannot open $r_bl_file : $!\n";
	print FH ">$rightid\n$rightseq\n";
	close(FH);
	push @tmpfiles, $r_bl_file;
	#print STDERR "TEMPFILE: $r_bl_file\n";

        return($l_bl_file, $r_bl_file);
}
################################################################################
=pod

B<blast_for_specificity()> 
 Run BLAST to check that primers are specific to one place in the genome

Parameters:
 LEFT	=> BLAST input file for LEFT primer
 RIGHT	=> BLAST input file for RIGHT primer

Returns:

=cut
sub blast_for_specificity {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $lfile = $options->{'LEFT'};
	my $rfile = $options->{'RIGHT'};

	# set up template BLAST command
	my $cmd = qq{$path_blast/blastall -i <FILE> -d $self->{'blastgenome'} -p blastn -W 7 -G 3 -e 100 -v 1000 -m 7};
	print STDERR "Using ".$self->{'blastgenome'}." for BLASTing\n";

	# BLAST left primer
	my $lcmd	= $cmd;
	$lcmd		=~ s/<FILE>/$lfile/;
	print STDERR "$lcmd\n";
	my $lblastout	= `$lcmd`;
	# get length of primer query sequence
  	$lblastout	=~ /<BlastOutput\_query\-len>(\d+)<\/BlastOutput\_query\-len>/;
	my $qlen	= $1;
	# get all BLAST hits in output file
	my @hits	= ($lblastout =~ /(<Hit>.+?<\/Hit>)/sg);

	if(scalar(@hits) == 0 && $self->{'allow_low_complexity'} == 0) {
		$lcmd .= qq{ -F F };
		print STDERR "Trying low-complexity search: $lcmd\n";
		$lblastout	= `$lcmd`;
  		$lblastout	=~ /<BlastOutput\_query\-len>(\d+)<\/BlastOutput\_query\-len>/;
		$qlen	= $1;
		@hits	= ($lblastout =~ /(<Hit>.+?<\/Hit>)/sg);
	}
	if(scalar(@hits) == 0) {
		return(1);
	}

	print STDERR scalar(@hits)." hits found (left), checking primer specificity\n";
	my $lstatus	= $self->check_blast_hits(HITS => \@hits, QLEN => $qlen);

	return(1) if($lstatus != 0);

	# BLAST right primer
	my $rcmd	= $cmd;
	$rcmd		=~ s/<FILE>/$rfile/;
	print STDERR "$rcmd\n";
	my $rblastout = `$rcmd`;
	# get length of primer query sequence
  	$rblastout	=~ /<BlastOutput\_query\-len>(\d+)<\/BlastOutput\_query\-len>/;
	my $qlen	= $1;
	# get all BLAST hits in output file
	my @hits	= ($rblastout =~ /(<Hit>.+?<\/Hit>)/sg);

	if(scalar(@hits) == 0 && $self->{'allow_low_complexity'} == 0) {
		$rcmd .= qq{ -F F };
		print STDERR "Trying low-complexity search: $rcmd\n";
		$rblastout	= `$rcmd`;
  		$rblastout	=~ /<BlastOutput\_query\-len>(\d+)<\/BlastOutput\_query\-len>/;
		$qlen	= $1;
		@hits	= ($rblastout =~ /(<Hit>.+?<\/Hit>)/sg);
	}
	if(scalar(@hits) == 0) {
		return(1);
	}

	print STDERR scalar(@hits)." hits found (right), checking primer specificty\n";
	my $rstatus	= $self->check_blast_hits(HITS => \@hits, QLEN => $qlen);

	# if both primers are specific, return happy status
	if($lstatus == 0 && $rstatus == 0) {
		return(0);
	}
	else {
		return(1);
	}
}

################################################################################
=pod

B<check_blast_hits()> 
 Check each BLAST hit

Parameters:
 HITS	=> BLAST hits, in array

Returns:
 scalar status: 0 if primer is specific, 1 if primer is not specific

=cut
sub check_blast_hits {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $hits	= $options->{'HITS'};
	my $qlen	= $options->{'QLEN'};

	# go through all hits until a good hit on another genomic region is
	# found; if a hit is found, skip this primer pair - don't want primer to
	# anneal to any other place on genome except requested SNP
	my $status = 0;	# consider things good until a bad thing is found (1)
	foreach my $hit (@{$hits}) {
		$hit		=~ /<Hit_def>(.+?)<\/Hit_def>/;
		my $curr_rs	= $1;
		$hit		=~ /<Hit_num>(.+?)<\/Hit_num>/;
		my $curr_hit	= $1;

		#print STDERR "Hit $curr_hit on $curr_rs (SNP on ".$self->{'rs'}.")\n";
		if($curr_rs ne $self->{'rs'} && $curr_hit == 1) {
			# BAD PAIR
			print STDERR "BAD: First hit not on same RS: $curr_rs not on ".$self->{'rs'}."\n";
			$status = 1;
			return(1);
		}

		my @hsps	= ($hit =~ /(<Hsp>.+?<\/Hsp>)/sg);

		#print Dumper @hsps;

		foreach my $hsp (@hsps) {
			# first hit should be itself, it not primer is bad
			# if hit is on same ref sequence but outside of primer
			# range, primer is bad
			#print STDERR "CHECKING HSP:---\n";
			$hsp	=~ /<Hsp_hit-from>(\d+)<\/Hsp_hit-from>/;
			my $fr	= $1;
			$hsp	=~ /<Hsp_hit-to>(\d+)<\/Hsp_hit-to>/;
			my $to	= $1;

			#print STDERR "Checking hit range: $fr - $to\n"; 
			#print STDERR "Range: ".$self->{'leftmax'}." - ".$self->{'rightmax'}."\n";

			if($hsp =~ /<Hsp_num>1<\/Hsp_num>/ && $curr_hit == 1) {
				unless($curr_rs eq $self->{'rs'} && ($fr >= $self->{'leftmax'} && $to <= $self->{'rightmax'})) {
					# BAD PAIR
					print STDERR "BAD: First hit not in range\n"; 
					$status = 1;
					return(1);
				}
				print STDERR "First hit in range and on same RS\n";
				next;
			}

			# if hit that isn't itself is good, primer is bad
              		$hsp	=~ /<Hsp_align-len>(\d+)<\/Hsp_align-len>/;
			my $tlen	= $1 / $qlen;
              		$hsp	=~ /<Hsp_midline>(.+?)<\/Hsp_midline>/;
			my $percid	= length($1) / $qlen;
			#print STDERR "Checking length and ID: $tlen >= 0.9 && $percid >= 0.85\n";
			if($tlen >= 0.9 && ($percid / $qlen) >= 0.85) {
				print STDERR "BAD: BLAST hit has 90%+ length and 85%+ ID\n";
				print STDERR "     $tlen >= 0.9 && $percid >= 0.85\n";
				$status = 1;
				return(1);
			}
		}
	}

	print STDERR "Primer ok\n";

	return(0);
}
################################################################################
=pod

B<add_adapters()> 
 Add sequencing adapters to primers and barcode if specified in set_barcode()

Parameters:

Returns:
 void

=cut
sub add_adapters {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
	my $la		= $options->{'LEFT'};
	my $ra		= $options->{'RIGHT'};

	$pair		=~ /PRIMER_(LEFT_\d+)_SEQUENCE=(.+)/;
	my $leftid	= $1;
	my $leftseq	= $2;
	$pair		=~ /PRIMER_(RIGHT_\d+)_SEQUENCE=(.+)/;
	my $rightid	= $1;
	my $rightseq	= $2;

	#### generate complete primer sequences, left and right
	## LEFT
	if($self->{'barcode'} =~ /\w+/) {
		# if barcode is being used, insert it between the left adapter
		# and left primer sequence (only sequence in one direction so
		# only need it on left primer)
		$leftseq	= $la.$self->{'barcode'}.$leftseq;
	}
	else {
		$leftseq	= $la.$leftseq;
	}
	## RIGHT
	$rightseq	= $ra.$rightseq;

	$pair		.= "PRIMER_".$leftid."_W_ADAPTER=".$leftseq."\n";
	$pair		.= "PRIMER_".$rightid."_W_ADAPTER=".$rightseq."\n";

	#print Dumper $pair;

        return($pair);
}

################################################################################
=pod

B<check_autodimer()> 
 Check that primers to not anneal to each other

Parameters:
 PAIR	=> primer3 formatted pair
 LEFT	=> BLAST input file for LEFT primer

Returns:
 void

=cut
sub check_autodimer {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $status	= 0;

	my $pair	= $options->{'PAIR'};
	my $l_bl_file	= $options->{'LEFT'};

	$pair		=~ /PRIMER_(LEFT_\d+)_W_ADAPTER=(.+)/;
	my $leftid	= $1;
	my $leftseq	= $2;
	$pair		=~ /PRIMER_(RIGHT_\d+)_W_ADAPTER=(.+)/;
	my $rightid	= $1."_revcomp";
	my $rightseq	= $2;

	# reverse right primer sequence and create the reverse complement
	$rightseq	= reverse($rightseq);
	$rightseq	=~ tr/CATG/GTAC/;

	print STDERR "Writing BLAST files for reverse complement $rightid\n";

	my $r_bl_file	= "/tmp/".$self->{'alias'}."_".$rightid.".fa";

	open(FH, ">".$r_bl_file) || die "Cannot open $r_bl_file : $!\n";
	print FH ">$rightid\n$rightseq\n";
	close(FH);
	push @tmpfiles, $r_bl_file;
	#print STDERR "TEMPFILE: $r_bl_file\n";

	# run BLAST
	my $cmd		= qq{$path_blast/bl2seq -i $l_bl_file -j $r_bl_file -p blastn -F F -e 1 -X 200 -D 1};
	print STDERR "$cmd\n";
	my $blout	= `$cmd`;

	#print STDERR "$blout\n";

	my @lines	= split /\n/, $blout;
	foreach (@lines) {
		unless(/^#/) {
			print STDERR "Auto-dimerization possible ($_)\n";
			$status = 1;
			last;
		}
	}

        return($status);	# 0 = no autodimer, 1 = autodimer (bad)
}
################################################################################
=pod

B<check_palindrome()> 
 Check that primers to not form a secondary structure

Parameters:
 PAIR	=> primer3 formatted pair

Returns:
 void

=cut
sub check_palindrome {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $status	= 0;

	my $pair	= $options->{'PAIR'};
	my $l_bl_file	= $options->{'LEFT'};

	$pair		=~ /PRIMER_(LEFT_\d+)_W_ADAPTER=(.+)/;
	my $leftid	= $1;
	my $leftseq	= $2;
	$pair		=~ /PRIMER_(RIGHT_\d+)_W_ADAPTER=(.+)/;
	my $rightid	= $1."_revcomp";
	my $rightseq	= $2;

	$status		= $self->palindrome(SEQ => $leftseq);
	if($status == 0) {
		$status	= $self->palindrome(SEQ => $rightseq);
	}

	return($status);	# 0 = no palidromes, 1 = palindromes
}

################################################################################
=pod

B<palindrome()> 
 Find palindrome in a sequence

Parameters:
 SEQ	=> sequence

Returns:
 void

=cut
sub palindrome {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $seq		= $options->{'SEQ'};

	my $maxmms	= 0;	# max mismatches allowed
	my $maxGap	= 10;
	my $minLen	= 5;
	my $maxLen	= length($seq) / 2;
	
	my @bases	= split //, $seq;
	
	my $end		= length($seq) - 1;
	my $begin	= 0;
	
	my $count	= 0;
	my $gap		= 0;
	my $rev		= 0;
	my $numpals	= 0;

	print STDERR "Checking $seq\n";
	
	#/* loop to look for inverted repeats */
	my $current;
	for ($current = $begin; $current < $end; $current++) {
	
		my $iend = $current + 2*($maxLen) + $maxGap;
	
		if($iend > $end) {
			$iend = $end;
		}
	
		my $istart = $current + $minLen;
	
		for (my $rev = $iend; $rev > $istart; $rev--) {
			my $count	= 0;
			my $mismatches	= 0;
			my $mismatchAtEnd = 0;
			my $ic		= $current;
			my $ir		= $rev;
	
			my $base1 = $bases[$ic];
			my $base2 = $BASECOMP{$bases[$ir]};
	
			#print STDERR "$base1, $base2\n";
	
			# if found a potential start of 
			if($base1 eq $base2) {
				while($mismatches <= $maxmms && $ic < $ir) {
					# get bases to compare
					my $nextbase1 = $bases[$ic++];
					my $prevbase2 = $BASECOMP{$bases[$ir--]};
	
					#print STDERR "\t$ic: $nextbase1 <=> $ir: $prevbase2\n";
	
					# check for mismatch at end
					if($nextbase1 eq $prevbase2) {
						$mismatchAtEnd = 0;
					}
					else {
						$mismatches++;
						$mismatchAtEnd++;
					}
					$count++;
				}
	
				$count	-= $mismatchAtEnd;
				$gap	= $rev - $current - $count - $count + 1;
			}
	
	
			if($count >= $minLen && $gap <= $maxGap) {
				#print STDERR "\tlength of inverted repeat > minlength: $count >= $minLen\n";
				#print STDERR "\tnumber of gaps <= max gaps: $gap <= $maxGap\n";
				#print STDERR "\t****PALINDROME FOUND\n";
				$numpals++;
			}
		}
	}
	
	print STDERR "Palindromes found: ", $numpals, "\n";

	return($numpals);	# 0 = no palindromes, >0 = palindromes
}
################################################################################
=pod

B<format_primers()> 
 Format primers for printing to output file

Parameters:
 PAIR	=> primer3 formatted primer pair

Returns:
 ref to hash of all primer attributes

=cut
sub format_primers {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
=cut
PRIMER_PAIR_10_PENALTY=6.153505
PRIMER_LEFT_10_PENALTY=0.623972
PRIMER_RIGHT_10_PENALTY=5.529533
PRIMER_LEFT_10_SEQUENCE=CCTTCGCGCTGGTATTCCTC
PRIMER_RIGHT_10_SEQUENCE=TCCTTTGAACAACTTTGCATCAACT
PRIMER_LEFT_10=187,20
PRIMER_RIGHT_10=287,25
PRIMER_LEFT_10_TM=65.624
PRIMER_RIGHT_10_TM=64.470
PRIMER_LEFT_10_GC_PERCENT=60.000
PRIMER_RIGHT_10_GC_PERCENT=36.000
PRIMER_LEFT_10_SELF_ANY=4.00
PRIMER_RIGHT_10_SELF_ANY=6.00
PRIMER_LEFT_10_SELF_END=0.00
PRIMER_RIGHT_10_SELF_END=2.00
PRIMER_LEFT_10_END_STABILITY=7.9000
PRIMER_RIGHT_10_END_STABILITY=6.7000
PRIMER_PAIR_10_COMPL_ANY=3.00
PRIMER_PAIR_10_COMPL_END=0.00
PRIMER_PAIR_10_PRODUCT_SIZE=101
PRIMER_PAIR_10_PRODUCT_TM=72.6356
PRIMER_PAIR_10_PRODUCT_TM_OLIGO_TM_DIFF=8.1651
PRIMER_PAIR_10_T_OPT_A=55.2860
PRIMER_LEFT_10_COORD=chr17:8445491-8445511
PRIMER_RIGHT_10_COORD=chr17:8445591-8445616
PRIMER_LEFT_10_W_ADAPTER=CCATCTCATCCCTGCGTGTCTCCGACTCAGCCTTCGCGCTGGTATTCCTC
PRIMER_RIGHT_10_W_ADAPTER=CCTCTCTATGGGCAGTCGGTGATTCCTTTGAACAACTTTGCATCAACT
=cut

	# amplicon size
	$pair		=~ /PRIMER_PAIR_\d+_PRODUCT_SIZE=(.+)/;
	my $ampsize	= $1;

	# melting temperatures
	$pair		=~ /PRIMER_LEFT_\d+_TM=(.+)/;
	my $lefttm	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+_TM=(.+)/;
	my $righttm	= $1;

	# GC%
	$pair		=~ /PRIMER_LEFT_\d+_GC_PERCENT=(.+)/;
	my $leftgc	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+_GC_PERCENT=(.+)/;
	my $rightgc	= $1;

	# primer sequences
	$pair		=~ /PRIMER_LEFT_\d+_SEQUENCE=(.+)/;
	my $leftseq	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+_SEQUENCE=(.+)/;
	my $rightseq	= $1;

	# primer sequences with adapters
	$pair		=~ /PRIMER_LEFT_\d+_W_ADAPTER=(.+)/;
	my $leftaseq	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+_W_ADAPTER=(.+)/;
	my $rightaseq	= $1;

	# primer genomic coordinates
	$pair		=~ /PRIMER_LEFT_\d+_COORD=(.+)/;
	my $leftcoord	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+_COORD=(.+)/;
	my $rightcoord	= $1;

	# amplicon genomic coordinate
	$leftcoord	=~ /(.+?\:\d+\-)/;
	my $ampcoord	= $1;
	$rightcoord	=~ /.+?\:\d+\-(\d+)/;
	$ampcoord	.= $1;

	# amplicon sequence
	my $fai		= QCMG::SamTools::Sam::Fai->load($self->{'genome'});
	my $ampseq	= $fai->fetch($ampcoord);

	# get distance from left primer to SNP
	$leftcoord	=~ /.+?\:(\d+)/;
	my $lefttosnp	= $self->{'coord'} - $1;
	# get distance from right primer to SNP
	$rightcoord	=~ /.+?\:\d+\-(\d+)/;
	my $righttosnp	= $1 - $self->{'coord'};

	#print STDERR "$self->{'coord'} : $1

	my %PRIMER = (
			'SNP_position'		=> $self->{'pos'},
			'primer_left_seq'	=> $leftseq,
			'primer_right_seq'	=> $rightseq,
			'primer_left_seq_adapter'	=> $leftaseq,
			'primer_right_seq_adapter'	=> $rightaseq,
			'amplicon_size'		=> $ampsize,
			'amplicon_seq'		=> $ampseq,
			'amplicon_range'	=> $ampcoord,
			'left_Tm'		=> $lefttm,
			'right_Tm'		=> $righttm,
			'left_gc'		=> $leftgc,
			'right_gc'		=> $rightgc,
			'left_primer_to_from'	=> $leftcoord,
			'right_primer_to_from'	=> $rightcoord,
			'left_to_SNP'		=> $lefttosnp,
			'right_to_SNP'		=> $righttosnp,
			'barcode'		=> $self->{'barcode'}
		);

        return(\%PRIMER);
}
################################################################################
=pod

B<read_dbsnp()> 
 Read dbSNP flat file

Parameters: 
 DBSNP

Returns:
 void

=cut
sub read_dbsnp {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $DBSNP	= $options->{'DBSNP'};

	print STDERR "Extracting relevant SNPs from dbSNP\n";

	my @snps = ();
	foreach my $snp (@{$DBSNP->{$self->{'rs'}}}) {
		unless($snp < $self->{'leftmax'} || $snp > $self->{'rightmax'}) {
			push @{$self->{'snps'}}, $snp;
		}
	}

        return($self->{'snps'});
}

################################################################################
=pod

B<patient_barcode()> 
 Retrieve a patient's Ion Torrent barcode (assigned in LIMS)

Parameters: 
 ID => 'APGI_1992' or 'APGI-1992'

Returns:
 scalar: barcode sequence (e.g., ATCAGACACG)
 scalar: barcode ID       (e.g., MID-5)

=cut
sub patient_barcode {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $id	= $options->{'ID'};

	print STDERR "Retrieving patient Torrent barcode sequence from LIMS\n";
	my $gl = QCMG::DB::Geneus->new;
	$gl->connect;
	my ($data, $slide)	= $gl->project_by_patient($id);
	my $barcode		= $slide->{'Ion Torrent Verification Index Sequence'};
	my $barcode_id		= $slide->{'Ion Torrent Verification Index Name'};

	$self->{$id}->{'barcode'}	= $barcode;
	$self->{$id}->{'barcode_id'}	= $barcode_id;

        return($self->{$id}->{'barcode'}, $self->{$id}->{'barcode_id'});
}

################################################################################
=pod

B<delete_tmpfile()> 
 Delete temp files

Parameters:

Returns:
 void

=cut
sub delete_tempfiles {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	foreach (@tmpfiles) {
		print STDERR "Deleting $_\n";
		unlink($_);
	}

        return();
}
################################################################################
=pod

B<DESTROY()> 

 Destructor; write logging information to log file

Parameters:

Returns:
 void

=cut
sub DESTROY {
	my $self = shift;

	#$self->delete_tempfiles();

	if($self->{'EXIT'} == 0) {
		$self->writelog(BODY => "EXECLOG: errorMessage none");
	}
	else {
		$self->writelog(BODY => "EXECLOG: errorMessage failed");
	}

	my $eltime	= $self->elapsed_time();
	my $date	= $self->timestamp();

	$self->writelog(BODY => "EXECLOG: elapsedTime $eltime");
	$self->writelog(BODY => "EXECLOG: stopTime $date");
	$self->writelog(BODY => "EXECLOG: exitStatus $self->{'EXIT'}");

	return;
}


1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
