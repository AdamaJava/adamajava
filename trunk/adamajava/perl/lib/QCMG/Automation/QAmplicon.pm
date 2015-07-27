package QCMG::Automation::QAmplicon;

##############################################################################
#
#  Module:   QCMG::Automation::QAmplicon.pm
#  Creator:  Lynn Fink
#  Created:  2011-06-04
#
#  This class contains methods for optimizing primer design for Ion Torrent Sequencing
#
# $Id$
#
##############################################################################

=pod

=head1 NAME

QCMG::Automation::QAmplicon

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Automation::QAmplicon->new();

=head1 DESCRIPTION

=head1 REQUIREMENTS

=cut

use strict;

# standard distro modules
use Data::Dumper;
#use POSIX 'strftime';
use POSIX;
use List::Util qw( min max );

use vars qw( $SVNID $REVISION);
use vars qw($primer3in @tmpfiles $path_blast $path_primer3 $path_dbsnp);

# third-party executable paths or files
$path_primer3		= "/panfs/share/software/primer3-2.2.3/src/";
$path_blast		= "/panfs/share/software/blast-2.2.24/bin/";
#$path_blast		= "/panfs/share/software/blast-2.2.28+/bin/";
$path_dbsnp		= "/panfs/share/dbSNP/hg19.db130.single.SNP.all";

# nucleotide complements
my %BASECOMP		= ('A' => 'T', 'C' => 'G', 'G' => 'C', 'T' => 'A');

# template for primer3 input file; <VAR> fields are settable
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
PRIMER_PRODUCT_SIZE_RANGE=<SIZE_RANGE> 
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
 Required
 	POS		=> genomic position/genomic ranges + reverse complement info
 	GENOME		=> reference genome .fa file

 Optional
	P3LENGTH	=> length of genomic range to design primers in; default = 500
	P3TEMPLATE	=> alternative primer3 template file
	MODE		=> mode: normal, torrent, miseq
	BLASTGENOME	=> reference genome to BLAST against (default = GENOME)
 	DBSNP		=> data structure with dbSNP
 	EXHAUSTIVE	=> perform exhaustive search (try more primer pairs and ignore
			   low-complexity regions) [0 = no; 1 = do max num pairs; 
			   2 = skip first pairs (checked already)]
	GENOMEPROP	=> reference genome .properties file (default = genome.properties)
	LOG_LEVEL	=> DEBUG modes
	THREADS		=> number of threads for BLAST to use (default = 1)

Sets:
 $self->{'pos'}				- genomic position/genomic ranges + reverse complement info
 $self->{'rs'}				- reference sequence from genomic position
 $self->{'rs_left'}			- reference sequence from genomic position 1 if complex feature
 $self->{'rs_right'}			- reference sequence from genomic position 2 if complex feature
 $self->{'coord'}			- start base from genomic position
 $self->{'mode'}			- how to run script (default = 'normal', 'torrent','miseq','miseq_desperate')
 $self->{'p3template'}			- primer3 template file
 $self->{'alias'}			- genomic position, with non-word chars changed to _
 $self->{'p3target'}			- relative base in sequence window around position to start masking (primer3 setting)
 $self->{'p3targetlen'}			- length of mask (primer3 setting)
 $self->{'p3halflength_l'}		- half the size of the window around position
 $self->{'p3halflength_r'}		- half the size of the window around position for second range, if complex feature
 $self->{'leftmax'}			- start of window around position (absolute coords)
 $self->{'endmax'}			- end of window around position (absolute coords)
 $self->{'path_blast'}			- path to blast executables
 $self->{'path_primer3'}		- path to primer3 executable
 $self->{'path_dbsnp'}			- path to dbSNP flast file database
 $self->{'num_primer_pairs_to_get'}	- number of primer pairs to get from primer3 (default = 200)
 $self->{'allow_low_complexity'}	- check primer uniqueness in low complexity regions (blast setting)
 $self->{'filter_w_dbsnp'}		- check that a primer does not overlap a dbSNP position (user input)
 $self->{'threads'}			- number of threads for BLAST to use
 $self->{'blastgenome'}			- different reference genome to BLAST against
 $self->{'loglevel'}			- debug mode

Returns:
 a new instance of this class.

=cut

sub new {
        #my $that  = shift;
	my $class = shift @_;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $self = {};
	bless ($self, $class);

        #my $class = ref($that) || $that;
        #my $self = bless $that->SUPER::new(), $class;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->{'pos'}		= $options->{'POS'};
	$self->{'genome'} 	= $options->{'GENOME'};

	# allow BLAST to use a different genome, if desired; otherwise, use same
	# reference genome for all steps (does not need a .properties file)
	if($options->{'BLASTGENOME'}) {
		$self->{'blastgenome'} 	= $options->{'BLASTGENOME'};
	}
	else {
		$self->{'blastgenome'} 	= $options->{'GENOME'};
	}

	# check for existence of reference genome file
	unless(-e $self->{'genome'}) {
		print STDERR "Cannot find reference genome .fa file: $self->{'genome'} : $!\n";
		exit(1);
	}
	unless(-e $self->{'blastgenome'}) {
		print STDERR "Cannot find BLAST reference genome .fa file: $self->{'blastgenome'} : $!\n";
		exit(1);
	}
	unless( $self->{'genome'} =~ /\.fa$/ || $self->{'genome'} =~ /\.fasta$/) {
		print STDERR "Reference genome ".$self->{'genome'}." does not have a .fa or .fasta suffix\n";
		exit(1);
	}

	# read genome properties file and get association between reference
	# sequence name and reference sequence length; this attempts to use a
	# .properties file, then attempts a .dict file (same as the @SQ lines
	# from a BAM header), and if these both fail, it parses the .fa file
	# directly
	#
	# sets $self->{'rs2size'}
	print STDERR "Getting RS sizes...\n";
	$self->get_rs_sizes();
	print STDERR "done\n";

	#### SNP verification
	# define reference sequence for this position and the start base of the
	# specified position
	$self->{'complexfeature'}	= 'n';
	if($self->{'pos'} =~ /^.+?\:\d+$/) {
		$self->{'pos'}		=~ /(.+?)\:(\d+)/;
		$self->{'rs'}		= $1;
		$self->{'rs_left'}	= $1;
		$self->{'rs_right'}	= $1;
		$self->{'coord'}	= $2;
		$self->{'p3targetlen'}	= 5;
		$self->{'p3target'}	= $self->{'p3halflength_l'} - floor($self->{'p3targetlen'}/2);
		$self->{'leftmax'}	= $self->{'coord'} - $self->{'p3halflength_l'};
		$self->{'rightmax'}	= $self->{'coord'} + $self->{'p3halflength_l'};
		# define length of genomic sequence to retrieve and region in primer3
		# input file where SNP is (on either side; if 250, whole range = 500)
		$self->{'p3halflength_l'}		= 250;
		$self->{'p3halflength_r'}		= 250;
		if($options->{'P3LENGTH'} > 0 && $options->{'P3LENGTH'} != 500) {
			$self->{'p3halflength_l'}	= ceil($options->{'P3LENGTH'} / 2);
			$self->{'p3halflength_r'}	= ceil($options->{'P3LENGTH'} / 2);
		}
		elsif($self->{'mode'} =~ /miseq/i) {
			$self->{'p3halflength_l'}	= 1000 / 2;
			$self->{'p3halflength_r'}	= 1000 / 2;
		}
	}
	# if position is only ref sequence and start position, add end position
	# that is same as start
 	elsif($self->{'pos'} =~ /^.+?\:\d+\-\d+$/) {
		$self->{'pos'}		=~ /(.+?)\:(\d+)\-(\d+)/;
		$self->{'rs'}		= $1;
		$self->{'rs_left'}	= $1;
		$self->{'rs_right'}	= $1;
		$self->{'coord'}	= $2;
		$self->{'leftmax'}	= $self->{'coord'} - $self->{'p3halflength_l'};
		$self->{'rightmax'}	= $self->{'coord'} + $self->{'p3halflength_l'};
		# define length of genomic sequence to retrieve and region in primer3
		# input file where SNP is (on either side; if 250, whole range = 500)
		$self->{'p3halflength_l'}		= 250;
		$self->{'p3halflength_r'}		= 250;
		if($options->{'P3LENGTH'} > 0 && $options->{'P3LENGTH'} != 500) {
			$self->{'p3halflength_l'}	= ceil($options->{'P3LENGTH'} / 2);
			$self->{'p3halflength_r'}	= ceil($options->{'P3LENGTH'} / 2);
		}
		elsif($self->{'mode'} =~ /miseq/i) {
			$self->{'p3halflength_l'}	= 1000 / 2;
			$self->{'p3halflength_r'}	= 1000 / 2;
		}

		$self->{'p3targetlen'}	= 5;
		$self->{'p3target'}	= $self->{'p3halflength_l'} - floor($self->{'p3targetlen'}/2);

		# if range of more than one base, take midpoint; use length of
		# range +2 bases as primer3 mask (MMMMM)
		# chrA:x-y
		# ----------x=====y--------------
		#              .
		# ---------MMMMMMMMM-------------
		if($3 > $2) {
			$self->{'coord'}	= floor(($3 - $2) / 2)  + $2;
			$self->{'p3targetlen'}	= ($3 - $2) + 2;
			$self->{'p3target'}	= $self->{'p3halflength_l'} - floor($self->{'p3targetlen'}/2);
			$self->{'leftmax'}	= $self->{'coord'} - $self->{'p3halflength_l'};
			$self->{'rightmax'}	= $self->{'coord'} + $self->{'p3halflength_l'};
		}
	}
	##### COMPLEX FEATURE VERIFICATION (SV, ex/in boundary)
	elsif($self->{'pos'} =~ /^.+?\:\d+\-\d+\t.+?\:\d+\-\d+/) {
		$self->{'complexfeature'}	= 'y';
		print STDERR "Found complex feature\n";

		# get distance between breakpoints
		#                                     chr1   s1     e1     chr2   s2     e2     range
		#                                     1      2      3      4      5      6      7 
		#$self->{'pos'} 			=~ /^(.+?)\:(\d+)\-(\d+)\t(.+?)\:(\d+)\-(\d+)\t(\d+)/;
		# get distance between breakpoints
		#                                     chr1   s1     e1     chr2   s2     e2     
		#                                     1      2      3      4      5      6      
		$self->{'pos'} 			=~ /^(.+?)\:(\d+)\-(\d+)\t(.+?)\:(\d+)\-(\d+)/;
		$self->{'rs_left'}		= $1;
		$self->{'rs_right'}		= $4;

		# define maximum range where primers can fall, absolute genomic
		# coordinates, for both left and right breakpoints
		$self->{'leftmax_l'}		= $2;
		$self->{'rightmax_l'}		= $3;
		$self->{'leftmax_r'}		= $5;
		$self->{'rightmax_r'}		= $6;

		$self->{'p3halflength_l'}	= ($self->{'rightmax_l'} - $self->{'leftmax_l'});
		$self->{'p3halflength_r'}	= ($self->{'rightmax_r'} - $self->{'leftmax_r'});

		# LEFT: chrA:x-y		RIGHT: chrB:m-n
		# AAAAAAAxAAAAAAyAAAAAA      	BBBBBBmBBBBBBBBBBBnBBBBBB
		#
		#                  xAAAAAAymBBBBBBBBBBBn
		#                        MMMMM 
		$self->{'p3targetlen'}		= 5;
		$self->{'p3target'}		= $self->{'p3halflength_l'} - floor($self->{'p3targetlen'}/2);

	}
	else {
		print STDERR "Invalid position description: ".$self->{'pos'}."\n";
		exit(2);
	}

	# define genomic range around position to explore; can change with
	# set_mode()
	# default: 'normal', other options: 'torrent', 'miseq'
	$self->{'mode'}		= $options->{'MODE'};
	$self->{'mode'}		= 'normal' if(! $options->{'MODE'});
	print STDERR "Running in ".$self->{'mode'}." mode\n";

	# create an alias/unique ID for using in temporary filenames, etc.
	#  chr2:3234-3234 -> chr2_3234_3234
	$self->{'alias'}	= $self->{'pos'};
	$self->{'alias'}	=~ s/\W/\_/g;

	# if the user specifies their own primer3 template file, use it instead
	if($options->{'P3TEMPLATE'}) {
		$self->{'p3template'}	= $options->{'P3TEMPLATE'};
		$self->read_alt_p3template();
	}

	# define paths to executables and database
	$self->{'path_blast'}		= $path_blast;
	$self->{'path_primer3'}		= $path_primer3;
	$self->{'path_dbsnp'}		= $path_dbsnp;
	# allow user to define new paths
	$self->{'path_blast'}		= $options->{'PATH_BLAST'}   if($options->{'PATH_BLAST'});
	$self->{'path_primer3'}		= $options->{'PATH_PRIMER3'} if($options->{'PATH_PRIMER3'});
	$self->{'path_dbsnp'}		= $options->{'PATH_DBSNP'}   if($options->{'PATH_DBSNP'});

	# check that executables exist
	# check for presence of primer3
	unless(-e $self->{'path_primer3'}) {
		print STDERR "Cannot find primer3: $self->{'path_primer3'} : $!\n";
		exit(3);
	}
	# check for presence of BLAST
	unless(-e $self->{'path_blast'}) {
		print STDERR "Cannot find BLAST: $self->{'path_blast'} : $!\n";
		exit(4);
	}
	unless(-e $self->{'path_blast'}."fastacmd") {
		print STDERR "Cannot find BLAST tool fastacmd in ".$self->{'path_blast'}."\n";
		exit(1);
	}
	unless(-e $self->{'path_blast'}."bl2seq") {
		print STDERR "Cannot find BLAST tool bl2seq in ".$self->{'path_blast'}."\n";
		exit(1);
	}
	unless(-e $self->{'path_blast'}."blastall") {
		print STDERR "Cannot find BLAST tool blastall in ".$self->{'path_blast'}."\n";
		exit(1);
	}
=cut
	unless(-e $self->{'path_blast'}."blastdbcmd") {
		print STDERR "Cannot find BLAST tool blastdbcmd in ".$self->{'path_blast'}."\n";
		exit(1);
	}
	unless(-e $self->{'path_blast'}."blastn") {
		print STDERR "Cannot find BLAST tool blastn in ".$self->{'path_blast'}."\n";
		exit(1);
	}
=cut

	# for exhaustive search (if no good pairs can be found on the first run)
	$self->{'num_primer_pairs_to_get'}	= 200;
	$self->{'allow_low_complexity'}		= 1;	# no
	if($options->{'EXHAUSTIVE'} == 1) {
		$self->{'num_primer_pairs_to_get'}	= 5000;
		$self->{'allow_low_complexity'}		= 0;	# yes
	}
	elsif($options->{'EXHAUSTIVE'} == 2) {
		# skip primers that would have already been checked
		$self->{'num_primer_pairs_to_skip'}	= $self->{'num_primer_pairs_to_get'};
		$self->{'num_primer_pairs_to_get'}	= 5000;
		$self->{'allow_low_complexity'}		= 0;	# yes
	}

	# if dbSNP filtering is performed, get dbSNP positions that fall within
	# window around specified position
	if($options->{'DBSNP'}) {
		$self->{'filter_w_dbsnp'} = 1;
		unless(-e $self->{'path_dbsnp'}) {
			print STDERR "Cannot find dbSNP: $self->{'path_dbsnp'} : $!\n";
			exit(5);
		}
		$self->read_dbsnp(DBSNP => $options->{'DBSNP'});
	}

	# set debug level/mode
	$self->{'loglevel'}	= $options->{'LOG_LEVEL'} if($options->{'LOG_LEVEL'});

	# default to 1 thread unless user asks for more
	if($options->{'THREADS'} =~ /\d+/) {
		$self->{'threads'} = $options->{'THREADS'};
	}
	else {
		# default
		$self->{'threads'} = 1;
	}

	print STDERR "Using $self->{'threads'} threads for BLAST\n" if($self->{'loglevel'});

	return $self;
}

################################################################################
=pod

B<pos()> 
 Return requested position

Parameters:
 none

Returns:
 scalar - position to design primers around

=cut
sub pos {
        my $self = shift @_;

	return($self->{'pos'});
}

################################################################################
=pod

B<read_alt_p3template()> 
 Read an alternate primer3 template file from a user-specified file rather than
  use the default template in $primer3in

Parameters:
 none

Sets:
 $primer3in

Returns:
 none

=cut
sub read_alt_p3template {
        my $self = shift @_;

	if(! -e $self->{'p3template'}) {
		print STDERR "Could not find ".$self->{'p3template'}."\n";
		exit(10);
	}

	local($/)	= undef;
	open(FH, $self->{'p3template'});
	my $fc	= <FH>;
	close(FH);

	if($fc =~ /<NUM_PRIMERS>/ && $fc =~ /<SEQID>/ && $fc =~ /<SEQUENCE>/ && $fc =~ /<TARGET>/ && $fc =~ /<SIZE_RANGE>/) {
		$primer3in	= $fc;
	}
	else {
		print STDERR qq{primer3 template file must contain the following patterns:

SEQUENCE_ID=<SEQID>
SEQUENCE_TEMPLATE=<SEQUENCE>
SEQUENCE_TARGET=<TARGET>
PRIMER_NUM_RETURN=<NUM_PRIMERS>
PRIMER_PRODUCT_SIZE_RANGE=<SIZE_RANGE> 

(There is no command line option for setting the product size range, but the pattern must exist for internal parsing.)
		};
		exit(10);
	}

	return();
}

################################################################################
=pod

B<get_rs_sizes()> 
 Get reference sequence sizes - to make sure that SNP primer range falls on
  genome

Parameters:
 none

 Requires:
  $self->{'genome'} 
    and optional xxx.properties file or xxx.dict file 

Returns:
 void

 Sets:
  $self->{'rs2size'} - ref to hash of RS names and lengths

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
	my %rs2size	= ();

	# if the .properties file does not exist, try a .dict file
	if(! -e $gprop) {
		$gprop	= $self->{'genome'};
		$gprop	=~ s/\.fa/\.dict/;
	}
	else {
		# parse .properties
		local($/)	= undef;
		open(FH, $gprop) || die "Cannot read $gprop: $!\n";
		my $fc	= <FH>;
		close(FH);

		# ...
		# c.1.H=chr1
		# c.1.L=249250621
		# c.1.P=0
		# c.2.H=chr2
		# c.2.L=243199373
		# c.2.P=252811351
		# ...

		my @chr	= ($fc =~ /.+?H=.+?L=\d+.+?P=\d+/sg);

		foreach (@chr) {
			/H=(.+)/;
			my $chr	= $1;
		
			/L=(\d+)/;
			my $len	= $1;

			$rs2size{$chr}	= $len;
		}
	}

	# if the .dict file does not exist, just parse the fasta file
	if(! -e $gprop) {
		my $curchr	= '';
		open(FH, $self->{'genome'}) || die "Cannot read $self->{'genome'}: $!\n";
		while(<FH>) {
			chomp;

			# >chr1
			# NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN
			# NNNNNNNNNNNNNN
			# ...
			# >chr2
			# ...
		
			if(/^>/) {
				/^>(.+)/;
				$rs2size{$1}	= 0;
				#print STDERR "Found $_ => $1\n";
				$curchr		= $1;
			}
			else {
				my $size		= length($_);
				$rs2size{$curchr}	+= $size;
			}
		}
		close(FH);
	}
	else {
		# parse .dict
		local($/)	= undef;
		open(FH, $gprop) || die "Cannot read $gprop: $!\n";
		my $fc	= <FH>;
		close(FH);

		# @HD	VN:1.0	SO:unsorted
		# @SQ	SN:chr1	LN:249250621	UR:file:GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa	M5:1b22b98cdeb4a9304cb5d48026a85128
		# @SQ	SN:chr2	LN:243199373	UR:file:GRCh37_ICGC_standard_v2.fa	M5:a0d9851da00400dec1098a9255ac712e
		# ...

		my @chr	= ($fc =~ /\@SQ\s+SN\:(.+?)\s+LN\:(\d+)/sg);

		for (my $i=0;$i<$#chr;$i+=2) {
			$rs2size{$chr[$i]}	= $chr[$i+1];
		}
	}

	$self->{'rs2size'} = \%rs2size;

        return();
}

################################################################################
=pod

B<set_mode()> 
 Set mode - normal or torrent

Parameters:
 MODE	=> torrent,miseq (default = normal)

Returns:
 void

 Sets:
  $self->{'mode'}

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
	elsif($options->{'MODE'} =~ /miseq/i && $options->{'MODE'} =~ /desperate/i) {
		$self->{'mode'} = 'miseq_desperate';
	}
	elsif($options->{'MODE'} =~ /miseq/i) {
		$self->{'mode'} = 'miseq';
	}
	else {
		$self->{'mode'} = 'normal';
	}

	print STDERR "Setting mode: ".$self->{'mode'}."\n" if($self->{'loglevel'});

        return();
}

################################################################################
=pod

B<get_genomic_seq()> 
 Get sequence from reference genome bracketing the position

Parameters:
 none

 Requires
  $self->{'rs_left'}
  $self->{'rs_right'}
  $self->{'coord'}
  $self->{'genome'}
  $self->{'rs2size'}
  $self->{'p3halflength_l'}

Sets:
 $self->{'p3target'}	- only if $self->{'pos'} is very close to start or
                          end of reference sequence
 $self->{'sequence'}	- genomic sequence

Returns:
 scalar string of bases

=cut
sub get_genomic_seq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	### handle this differently if a ranged feature is used
	if($self->{'complexfeature'} eq 'y') {
		my $seq			= $self->build_genomic_seq();
		$self->{'sequence'}	= $seq;
		return($seq);
	}

	### get genomic sequence for single base positions
        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my @seqs	= ();
	my @targets	= ();

	my $start = $self->{'coord'} - $self->{'p3halflength_l'};
	my $end   = $self->{'coord'} + $self->{'p3halflength_l'};
	my $pos	  = $self->{'rs_left'}.":".$start."-".$end;

	# make sure that %rs2size keys match chromosome names in input file
	if(! defined $self->{'rs2size'}->{$self->{'rs_left'}}) {
		print STDERR "Chromosome name used in input file is not found in chromosome sizes: ".$self->{'rs_left'}."\n";
		exit(1);
	}

	print STDERR "Getting genomic sequence around ".$self->{'coord'}.": $pos\n"     if($self->{'loglevel'});
	print STDERR $self->{'rs_left'}." length: ".$self->{'rs2size'}->{$self->{'rs_left'}}."\n" if($self->{'loglevel'});

	# check that range is fully on RS; if not, shift window to start
	# or end at RS start or end, maintaining window length and mask start
	# position
	if($start < 1) {
		my $adjust		= 1 - $start;
		$start			= 1;
		$end			= $start + (2 * $self->{'p3halflength_l'});
		$self->{'p3target'}	= $self->{'p3halflength_l'} - $adjust;
		$pos	  		= $self->{'rs_left'}.":".$start."-".$end;
	
		print STDERR "Shifting window at start of RS: $start, $end, by $adjust\n" if($self->{'loglevel'});
		print STDERR "New primer3 range: ".$self->{'p3target'}."\n"               if($self->{'loglevel'});
	}
	if($end > $self->{'rs2size'}->{$self->{'rs_left'}}) {
		my $adjust		= $end - $self->{'rs2size'}->{$self->{'rs_left'}};
		$end			= $self->{'rs2size'}->{$self->{'rs_left'}};
		$start			= $end - (2 * $self->{'p3halflength_l'});
		$self->{'p3target'}	= $self->{'p3halflength_l'} + $adjust;
		$pos	  		= $self->{'rs_left'}.":".$start."-".$end;

		print STDERR "Shifting window at end of RS: $start, $end, by $adjust\n"  if($self->{'loglevel'});
		print STDERR "New primer3 range: ".$self->{'p3target'}."\n"              if($self->{'loglevel'});
	}

	my $seq		= $self->fetch_subsequence(GENOME => $self->{'genome'}, POS => $pos);
	$seq		=~ s/\n//sg;
	$self->{'sequence'}	= $seq;
	print STDERR "SEQ: $seq\n" if($self->{'loglevel'});

        return($seq);
}

################################################################################
=pod

B<build_genomic_seq()> 
 Build sequence from ranges and reverse complement rules

Parameters:
 none

 Requires
  $self->{'pos'}
  $self->{'genome'}
  $self->{'rs2size'}

Sets:
 $self->{'p3target'}	- only if $self->{'pos'} is very close to start or
                          end of reference sequence

Returns:
 scalar string of bases

=cut
sub build_genomic_seq {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	# these are not necessarily symmetric in size
	# chr1:77465522-77466122	chr6:82454589-82455189
	my ($range1, $range2, $rvc1, $rvc2)	= split /\t/, $self->{'pos'};

	#print STDERR "$range1, $range2, $rvc1, $rvc2\n";

	$range1		=~ /^(.+?)\:(\d+)\-(\d+)/;
	my $rs1		= $1;
	my $start1	= $2;
	my $end1	= $3;
	my $len1	= $end1 - $start1 + 1;
	#print STDERR "$rs1 : $start1 - $end1\n";
	$range2		=~ /^(.+?)\:(\d+)\-(\d+)/;
	my $rs2		= $1;
	my $start2	= $2;
	my $end2	= $3;
	my $len2	= $end2 - $start2 + 2;
	#print STDERR "$rs2 : $start2 - $end2\n";

	# make sure that %rs2size keys match chromosome names in input file
	if(! defined $self->{'rs2size'}->{$rs1}) {
		print STDERR "Chromosome name used in input file is not found in chromosome sizes: ".$rs1."\n";
		exit(1);
	}
	elsif(! defined $self->{'rs2size'}->{$rs2}) {
		print STDERR "Chromosome name used in input file is not found in chromosome sizes: ".$rs2."\n";
		exit(1);
	}

	# check that ranges are fully on chromosomes; if not, shift windows to start
	# or end at chromosome start or end, maintaining window length and mask start
	# position
	my $pos1			= $rs1.":".$start1."-".$end1;
	my $pos2			= $rs2.":".$start2."-".$end2;
	if($start1 < 1) {
		my $adjust		= 1 - $start1;
		$start1			= 1;
		$self->{'p3target'}	= $end1;
		$pos1	  		= $rs1.":".$start1."-".$end1;
	
		print STDERR "Truncating window at start of RS: $start1, $end1, by $adjust\n" if($self->{'loglevel'});
		print STDERR "New primer3 range: ".$self->{'p3target'}."\n"                   if($self->{'loglevel'});
	}
	if($end2 > $self->{'rs2size'}->{$rs2}) {
		my $adjust		= $end2 - $self->{'rs2size'}->{$rs2};
		$end2			= $self->{'rs2size'}->{$rs2};
		$self->{'p3target'}	= $len1;
		$pos2	  		= $rs2.":".$start2."-".$end2;

		print STDERR "Truncating window at end of RS: $start2, $end2, by $adjust\n"  if($self->{'loglevel'});
		#print STDERR "New primer3 range: ".$self->{'p3target'}."\n"                  if($self->{'loglevel'});
	}

	my $seq1	= $self->fetch_subsequence(GENOME => $self->{'genome'}, POS => $pos1);
	$seq1		=~ s/\n//sg;
	print STDERR "SEQ1: $seq1\n" if($self->{'loglevel'});
	my $seq2	= $self->fetch_subsequence(GENOME => $self->{'genome'}, POS => $pos2);
	$seq2		=~ s/\n//sg;
	print STDERR "SEQ2: $seq2\n" if($self->{'loglevel'});

	# reverse complement seqs if directed
	if($rvc1 eq 'y') {
		$seq1	= reverse($seq1);
		$seq1	=~ tr/CATG/GTAC/;
	}
	if($rvc2 eq 'y') {
		$seq2	= reverse($seq2);
		$seq2	=~ tr/CATG/GTAC/;
	}


	# combine sequences to create simulated rearrangement
	my $seq			= $seq1.$seq2;
	my $seqlen		= length($seq);
	# set coord to relative positions in simulated rearrangement
	$self->{'coord'}	= $rs1.",".$rs2.":"."1-".$seqlen;

        return($seq);
}

################################################################################
=pod

B<fetch_subsequence()> 
 Get a subsequence from a fasta file
   - the .fa reference genome file must have been formatted with -o T in
   order to index the reference sequence names

 NOTE: the .fa file MUST have been formatted using the BLAST tool formatdb like
       this

       formatdb -t Title -i genome.fa -l formatdb.log -p F -o T -s T

Parameters:
 GENOME	=> reference genome to query
 POS	=> position in genome to return sequence for

 Requires:
  $self->{'path_blast'} and fastacmd

Returns:
 scalar string of bases

=cut
sub fetch_subsequence {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $genome	= $options->{'GENOME'};
	my $pos		= $options->{'POS'};

	$pos		=~ /(.+)\:(\d+)\-(\d+)/;
	my $rs		= $1;
	my $s		= $2;
	my $e		= $3;

	print STDERR "FASTACMD: $pos => $rs : $s - $e\n";

	my $seq		= '';

	#print STDERR "Fetching sequence from $pos from $genome\n";

	# /panfs/share/software/blast-2.2.28+/bin//blastdbcmd -db GRCh37_ICGC_standard_v2.fa -dbtype nucl -entry chr12 -line_length 600 -range 25398030-25398530
	my $cmd		= $self->{'path_blast'}.qq{fastacmd -d $genome -p F -s $rs -l 2000 -L $s,$e};
	#my $cmd		= $self->{'path_blast'}.qq{blastdbcmd -db $genome -dbtype nucl -entry $rs  -line_length 600 -range }.$s.qq{-}.$e;
	print STDERR "$cmd\n";
	$seq		= `$cmd | grep -v ">"`;
	chomp $seq;
         
	return $seq;
}

################################################################################
=pod

B<create_primer3_input()> 
 Write primer3 input file

Parameters:
 SEQ	=> string of bases

 Requires
  $self->{'alias'}
  $self->{'p3target'}
  $self->{'num_primer_pairs_to_get'}

Returns:
 scalar - filename

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

	# join target start and length because they are input as a single
	# parameter in primer3
	my $target	= join ",", $self->{'p3target'}, $self->{'p3targetlen'};

	my $fc = $primer3in;
	$fc =~ s/<SEQID>/$self->{'alias'}/;
	$fc =~ s/<SEQUENCE>/$options->{'SEQ'}/;
	$fc =~ s/<TARGET>/$target/;
	$fc =~ s/<NUM_PRIMERS>/$self->{'num_primer_pairs_to_get'}/;
	if($self->{'mode'} =~ /miseq/i) {
		$fc =~ s/<SIZE_RANGE>/400-500/;
	}
	else {
		$fc =~ s/<SIZE_RANGE>/50-120/;
	}

	# write to file
	open(FH, ">".$tmpfile) || die "Cannot write to $tmpfile: $!\n";
	print FH $fc;
	close(FH);

	print STDERR "Writing primer3 input to $tmpfile\n" if($self->{'loglevel'});

	push @tmpfiles, $tmpfile;	# keep track of tmpfiles

        return($tmpfile);
}

################################################################################
=pod

B<run_primer3()> 
 Run primer3 with input file

Parameters:
 none

 Requires
  $self->{'num_primer_pairs_to_skip'}
  primer3

Returns:
 scalar - name of output file, or void if primer3 had a non-zero exit value

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

	my $seq		= $self->get_genomic_seq();
	my $infile	= $self->create_primer3_input(SEQ => $seq);

	my $errfile	= $infile.".err";
	my $outfile	= $infile.".p3.out";

	# -output=/path/to/output/file -error=/path/to/error/file primer3_input_file
	my $cmd = qq{$path_primer3/primer3_core -output=$outfile };
	$cmd .= qq{-error=$errfile $infile };

	print STDERR "Running primer3: $cmd\n" if($self->{'loglevel'});

	# exit = 0 if success; non-0 if failuer
	my $rv	= system($cmd);
	
	if($rv != 0) {
		my $error	= `cat $errfile`;
		chomp $error;
		print STDERR "primer3 error: $error\n" if($self->{'loglevel'});
		exit(10);
		# !!! should try to handle this more elegantly
	}

	push @tmpfiles, $errfile;
	push @tmpfiles, $outfile;

	local($/) = undef;
	open(FH, $outfile) || die "Cannot open $outfile : $!\n";
	my $fc = <FH>;
	close(FH);

	$fc =~ /PRIMER_PAIR_NUM_RETURNED=(\d+)/;
	my $num_returned = $1;

	if($num_returned == 0) {
		print STDERR "No primer pairs returned...?\n" if($self->{'loglevel'});
	}
	else {
		print STDERR "$num_returned pairs found\n" if($self->{'loglevel'});
	}

	# get genomic sequence used as template
	$fc =~ /SEQUENCE_TEMPLATE=(\w+)/;
	my $genseq = $1;

	# get primer pairs from primer3 output
	my @pairs = ($fc =~ /(PRIMER_PAIR_\d+_PENALTY.+?PRIMER_PAIR_\d+_T_OPT_A=[\d\.]+)/sg);

	# if the first batch of primers have already been checked and found to
	# be bad, just start with the new pairs
	if($self->{'num_primer_pairs_to_skip'}) {
		@pairs		= @pairs[$self->{'num_primer_pairs_to_skip'}..$#pairs];
		print STDERR "Evaluating additional ".scalar(@pairs)." pairs\n" if($self->{'loglevel'});
	}

        return(\@pairs);
}

################################################################################
=pod

B<evaluate_pairs()> 
 Evaluate all primer pairs and keep the ones that pass the basic criteria

Parameters:
 PAIRS	=> ref to array of primer3-formatted primer pairs

 Requires
  $self->{'filter_w_dbsnp'}
  $self->{'mode'}

Returns:
 ref to array of pairs (in primer3 output format)

 Sets:
  $self->{'pairs'}

=cut
sub evaluate_pairs {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
	print STDERR "Evaluating pairs\n" if($self->{'loglevel'});

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pairs	= $options->{'PAIRS'};

	my $status	= 0;

	# check pairs
	foreach (@{$pairs}) {
		/PRIMER_LEFT_(\d+)/;
		my $pid	= $1;

		# if either of the primer sequences or positions has been
		# evaluated already and found to be unsuitable, don't bother
		# evaluating them again
		#print STDERR "Checking if primers are known to be bad\n";
		$status		= $self->known_bad_primer(PAIR => $_);
		#print STDERR "Status: $status\n";
		if($status == 1) {
			print STDERR "Primer $pid was previously evaluated and failed\n" if($self->{'loglevel'});
			next;
		}

		# check primer pair Tm
		#print STDERR "Checking Tm of pair\n";
		my $status	= $self->check_primer_pair_tm(PAIR => $_);
		if($status	== 1) {
			print STDERR "Primer $pid failed dTm check\n" if($self->{'loglevel'});
			next;
		}

		# check that neither primer contains the position of interest
		#print STDERR "Check that SNP is not in primer sequences\n";
		$status		= $self->check_snp_in_primer(PAIR => $_);
		if($status	== 1) {
			print STDERR "Position falls inside primer $pid\n" if($self->{'loglevel'});
			next;
		}

		# check that neither primer overlaps a known SNP
		if($self->{'filter_w_dbsnp'} == 1) {
			#print STDERR "Check that a dbSNP position is not in primer sequences\n";
			$status		= $self->check_dbsnp(PAIR => $_);
			if($status	== 1) {
				print STDERR "Primer $pid contains a dnSNP position\n" if($self->{'loglevel'});
				next;
			}
		}

		# check if either primer is within 40 bp of position
		if($self->{'mode'} eq 'torrent') {
			#print STDERR "Checking torrent compatibility\n";
			$status	= $self->check_torrentable(PAIR => $_);
			if($status == 1) {
				print STDERR "Primer $pid not close enough to position for Ion Torrent sequencing\n" if($self->{'loglevel'});
				next;
			}
		}
		elsif($self->{'mode'} =~ /miseq/) {
			$status	= $self->check_miseqable(PAIR => $_);
			if($status == 1) {
				print STDERR "Primer $pid too close to primer ends for MiSeq sequencing\n" if($self->{'loglevel'});
				next;
			}
		}

		# if primer passes all tests, keep it for further testing
		push @{$self->{'pairs'}}, $_;

		print STDERR "Primer pair $pid passed all checks\n" if($self->{'loglevel'});
	}

	# keep track of number of viable pairs
	#$self->{'viable_pairs'}	= scalar(@{$self->{'pairs'}});

	return(\@{$self->{'pairs'}});
}

################################################################################
=pod

B<check_primer_pair_tm()> 
 Check that both primers in a pair have a Tm within 3C of each other

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

Returns:
 scalar - bool; 1 if pair is bad; 0 if pair is good

=cut
sub check_primer_pair_tm {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
	my $status	= 0;

	# check primer pair Tm
	$pair		=~ /PRIMER_LEFT_\d+_TM=(.+)/;
	my $pl_tm	= $1;

	$pair		=~ /PRIMER_RIGHT_\d+_TM=(.+)/;
	my $pr_tm	= $1;
	my $dtm		= abs($pl_tm - $pr_tm);
	$dtm		= sprintf("%3.3f", $dtm);

	if(abs($pl_tm - $pr_tm) > 3.1) {
		print STDERR "BAD dTM ($dtm), skipping\n" if($self->{'loglevel'});
		$status	= 1;
	}

	return($status);
}

################################################################################
=pod

B<get_primer_pair_coords()> 
 Get absolute genomic coordinates for each primer in a pair

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

 Requires:
  $self->{'p3target'}
  $self->{'coord'}
  $self->{'rs_left'}
  $self->{'rs_right'}

Returns:
 two scalars: left primer coordinate string, right primer coordinate string

=cut
sub get_primer_pair_coords {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
	my $status	= 0;

	# get primer coordinates
	#PRIMER_LEFT_0=60,20
	#PRIMER_RIGHT_0=538,20
	$pair		=~ /PRIMER_LEFT_(\d+)=(\d+),(\d+)/;
	my $pid		= $1;
	my $pl_loc	= $2;
	my $pl_rng	= $3;
	$pair		=~ /PRIMER_RIGHT_\d+=(\d+),(\d+)/;
	my $pr_loc	= $1;
	my $pr_rng	= $2;

	my ($pl_coord, $pr_coord);
	if($self->{'complexfeature'} eq 'y') {
		my $pl_l	= $self->{'leftmax_l'} + $pl_loc;
		my $pl_e	= $pl_l + $pl_rng;
		my $pr_l	= $self->{'leftmax_r'} + ($pr_loc - $self->{'p3halflength_l'});
		my $pr_e	= $pr_l + $pr_rng;

		print STDERR $self->{'leftmax_r'}." + $pr_loc - ".$self->{'p3halflength_l'}."\n";

		$pl_coord	= $self->{'rs_left'}.":".$pl_l."-".$pl_e;
		$pr_coord	= $self->{'rs_right'}.":".$pr_l."-".$pr_e;
	}
	else {
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
	
		$pl_coord	= $self->{'rs_left'}.":".$pl_l."-".$pl_e;
		$pr_coord	= $self->{'rs_right'}.":".$pr_l."-".$pr_e;
	}

	return($pl_coord, $pr_coord);
}

################################################################################
=pod

B<known_bad_primer()> 
 Check if primer sequence has been seen before ; translate into sequences and
  genomic coordinates and check both attributes

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

 Requires:
  $self->{'primer_blacklist'}

Returns:
 scalar - bool; 1 if primer is bad; 0 if primer is not bad

=cut
sub known_bad_primer {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
	my $status	= 0;

	# gather all coordinates and sequences
	my @attrs	= ();

	# get genomic coordindates of each primer
	my ($lco, $rco)	= $self->get_primer_pair_coords(PAIR => $pair);

	push @attrs, $lco, $rco;
	
	# get sequences of each primer
	$pair		=~ /PRIMER_LEFT_\d+_SEQUENCE=(.+)/;
	my $leftseq	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+_SEQUENCE=(.+)/;
	my $rightseq	= $1;

	push @attrs, $leftseq, $rightseq;

	#print STDERR "Checking if primer is bad\n";
	foreach (@attrs) {
		#print STDERR "$_ : ".$self->{'primer_blacklist'}->{$_}."\n";

		if($self->{'primer_blacklist'}->{$_} == 1) {
			$status	= 1;
			last;
		}
	}

	return($status);
}

################################################################################
=pod

B<set_bad_primer()> 
 If primer has been found to fail any criteria specific to sequence or genomic
  position mark it as a bad primer

Parameters:
 SEQ	=> a primer sequence
 POS	=> a primer's genomic coordinates

Requires:
 $self->{'primer_blacklist'}

Returns:
 void

=cut
sub set_bad_primer {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	# treat coordinates and sequences the same - this is just a hash so any
	# key should work
	$options->{'SEQ'}	= $options->{'POS'} if(defined $options->{'POS'});

	my $seq		= $options->{'SEQ'};

	$self->{'primer_blacklist'}->{$seq} = 1;

	#print STDERR "Setting bad primer $seq : $self->{'primer_blacklist'}->{$seq}\n";

	return();
}

################################################################################
=pod

B<check_dbsnp()> 
 Determine if a primer sequence overlaps a known dbSNP position

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

Requires:
 $self->{'snps'}

Returns:
 scalar - bool; 1 if primer is bad; 0 if primer is not bad

=cut
sub check_dbsnp {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
	my $status	= 0;

	my ($lco, $rco)	= $self->get_primer_pair_coords(PAIR => $pair);

	$lco		=~ /.+?\:(\d+)\-(\d+)/;
	my $ls		= $1;
	my $le		= $2;
	$rco		=~ /.+?\:(\d+)\-(\d+)/;
	my $rs		= $1;
	my $re		= $2;

	# if SNP falls in left primer
	foreach my $snp (@{$self->{'snps'}}) {
		if($snp >= $ls && $snp <= $le) {
			print STDERR "BAD: dbSNP SNP in left primer\n" if($self->{'loglevel'});
			$status	= 1;
			last;
		}
	}
	if($status == 1) {
		$self->set_bad_primer(POS => $lco);
	}

	# if SNP falls in right primer and left primer is ok
	unless($status == 1) {
		foreach my $snp (@{$self->{'snps'}}) {
			if($snp >= $rs && $snp <= $re) {
				print STDERR "BAD: dbSNP SNP in right primer\n" if($self->{'loglevel'});
				$status	= 1;
				last;
			}
		}
		if($status == 1) {
			$self->set_bad_primer(POS => $rco);
		}
	}

	return($status);
}

################################################################################
=pod

B<check_torrentable()> 
 Determine if desired SNP position is within 40 bp of primer sequence (which is
  optimal for Ion Torrent primers)

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

Requires:
 $self->{'p3target'}

Returns:
 scalar - bool; 1 if primer is bad; 0 if primer is not bad

=cut
sub check_torrentable {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
	my $status	= 0;

	# use relative coordinates for this check (not genomic coordinates)
	$pair		=~ /PRIMER_LEFT_\d+=(\d+),(\d+)/;
	my $pl_loc	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+=(\d+),(\d+)/;
	my $pr_loc	= $1;

	unless($self->{'p3target'} - $pl_loc < 40 || $pr_loc - $self->{'p3target'} < 40) {
		print STDERR "Primer is not within 40bp of SNP\n" if($self->{'loglevel'});
		$status	= 1;
	}

	return($status);
}

################################################################################
=pod

B<check_miseqable()> 
 Determine if desired SNP position is not within 70 bp of end of primers to
  avoid being removed by Nextera transposons

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

Requires:
 $self->{'p3target'}

Returns:
 scalar - bool; 1 if primer is bad; 0 if primer is not bad

=cut
sub check_miseqable {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
	my $status	= 0;

	# use relative coordinates for this check (not genomic coordinates)
	#PRIMER_LEFT_81_SEQUENCE=CTTCGGTGATGAGGCTGAG
	#PRIMER_RIGHT_81_SEQUENCE=CATTGTTGATGGAAGAATAGCC
	#PRIMER_LEFT_81=180,19
	#PRIMER_RIGHT_81=283,22
	$pair		=~ /PRIMER_LEFT_\d+=(\d+),\d+/;
	my $pl_loc	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+=(\d+),\d+/;
	my $pr_loc	= $1;

	if($self->{'p3target'} - $pl_loc < 100 || $pr_loc - $self->{'p3target'} < 100) {
		print STDERR "Primer is within 100bp of SNP\n" if($self->{'loglevel'});
		$status	= 1;
	}
	elsif($self->{'mode'} =~ /desperate/ && ($self->{'p3target'} - $pl_loc < 70 || $pr_loc - $self->{'p3target'} < 70)) {
		print STDERR "Primer is within 70bp of SNP\n" if($self->{'loglevel'});
		$status	= 1;
	}

	return($status);
}

################################################################################
=pod

B<check_snp_in_primer()> 
 Make sure that SNP position of interest does not fall inside a primer

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

Requires:
 $self->{'coord'} (SNP position of interest)

Returns:
 scalar - bool; 1 if primer is bad; 0 if primer is not bad

=cut
sub check_snp_in_primer {
        my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options	= {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $pair	= $options->{'PAIR'};
	my $status	= 0;

	my ($lco, $rco)	= $self->get_primer_pair_coords(PAIR => $pair);

	$lco		=~ /.+?\:(\d+)\-(\d+)/;
	my $ls		= $1;
	my $le		= $2;
	$rco		=~ /.+?\:(\d+)\-(\d+)/;
	my $rs		= $1;
	my $re		= $2;

	# check that position is not in right primer
	if($self->{'coord'} <= $re && $self->{'coord'} >= $rs) {
		print STDERR "SNP contained in right primer, skipping\n" if($self->{'loglevel'});
		$self->set_bad_primer(POS => $rco);
		$status	= 1;
	}
	# check that position is not in left primer (only if right primer is ok)
	unless($status == 1) {
		if($self->{'coord'} <= $le && $self->{'coord'} >= $ls) {
			print STDERR "SNP contained in left primer, skipping\n" if($self->{'loglevel'});
			$self->set_bad_primer(POS => $lco);
			$status	= 1;
		}
	}

	return($status);
}

################################################################################
=pod

B<create_blast_input()> 
 Create input file for BLAST

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

 Requires:
  $self->{'alias'}

Returns:
 two scalars - BLAST input filenames for left and right primers

 Sets:
  $self->{'left_blast_file'}->{<PRIMERPAIRID>}

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

	my $status	= 0;

	# before running primer pair through all check, see if either of
	# the pair is known to be a failed sequence
	#print STDERR "Checking if primers are known to be bad\n";
	$status		= $self->known_bad_primer(SEQ => $leftseq);
	next if($status == 1);
	$status		= $self->known_bad_primer(SEQ => $rightseq);
	next if($status == 1);

	print STDERR "Writing BLAST files for $leftid and $rightid\n"  if($self->{'loglevel'});

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

	# for use in subsequent subroutines (don't need right one)
	$self->{'left_blast_file'}->{$leftid} 		= $l_bl_file;
	#print STDERR "SETTING LEFT BLAST FILE: $l_bl_file\n";
	#print STDERR "SETTING LEFT BLAST FILE IDL $leftid ".$self->{'left_blast_file'}->{$leftid}."\n";

        return($l_bl_file, $r_bl_file);
}

################################################################################
=pod

B<blast_for_specificity()> 
 Run BLAST to check that primers are specific to one place in the genome

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

Returns:
 scalar - bool: 0 if primer is specific; 1 if primer fails specificity test

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

	my $pair		= $options->{'PAIR'};

	my ($lfile, $rfile)	= $self->create_blast_input(PAIR => $pair);

	# for recording bad primers
	my ($lco, $rco)		= $self->get_primer_pair_coords(PAIR => $pair);

	# BLAST left primer
	#$self->{'rs'}		= $self->{'rs_left'};
	my $lstatus		= $self->blast_primer(FILE => $lfile, PRIMERPOS => 'l');

	if($lstatus == 1) {
		$self->set_bad_primer(POS => $lco);
	}

	# BLAST right primer
	my $rstatus	= 1;
	#$self->{'rs'}		= $self->{'rs_right'};
	unless($lstatus == 1) {
		$rstatus	= $self->blast_primer(FILE => $rfile, , PRIMERPOS => 'r');

		if($rstatus == 1) {
			$self->set_bad_primer(POS => $rco);
		}
	}

	# if both primers are specific, return happy status
	my $status	= 1;
	if($lstatus == 0 && $rstatus == 0) {
		$status	= 0;
	}

	return($status);
}

################################################################################
=pod

B<blast_primer()> 
 Run BLAST to check that primers are specific to one place in the genome

Parameters:
 FILE		=> BLAST input file for a primer
 PRIMERPOS	=> specify which of the primer pair is being BLASTed ('l','r')

 Requires:
  $self->{'threads'}
  $self->{'blastgenome'}
  $self->{'allow_low_complexity'}

Returns:
 scalar - bool: 0 if primer is specific; 1 if primer fails specificity test

=cut
sub blast_primer {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $file	= $options->{'FILE'};
	my $primerpos	= $options->{'PRIMERPOS'};

	#print STDERR "Primer Pos = $primerpos\n"  if($self->{'loglevel'});

	print STDERR "Blasting $file\n" if($self->{'loglevel'});

	# set up template BLAST command
	my $basecmd	 = qq{$path_blast/blastall -a $self->{'threads'} };
	$basecmd	.= qq{ -i <FILE> -d $self->{'blastgenome'} -p blastn };
	$basecmd	.= qq{ -W 7 -G 3 -e 100 -v 1000 -m 7};
	# blastn -task blastn -db GRCh37_ICGC_standard_v2.fa -query /tmp/chr12_25398280_25398280_LEFT_0.fa -evalue 100 -gapopen 4 -num_threads 1 -word_size 7 -max_target_seqs 1000 -outfmt 6
	#my $basecmd	 = qq{$path_blast/blastn -task blastn -num_threads $self->{'threads'} };
	#$basecmd	.= qq{ -query <FILE> -db }.$self->{'blastgenome'};
	#$basecmd	.= qq{ -evalue 100 -gapopen 4 -word_size 7 -max_target_seqs 1000 -outfmt 6 };

	# BLAST left primer
	my $cmd	= $basecmd;
	$cmd		=~ s/<FILE>/$file/;
	print STDERR "$cmd\n" if($self->{'loglevel'});

	my $blastout	= `$cmd`;
	# get length of primer query sequence
	# <BlastOutput_query-len>21</BlastOutput_query-len>
  	$blastout	=~ /<BlastOutput\_query\-len>(\d+)<\/BlastOutput\_query\-len>/;
	my $qlen	= $1;
	# get all BLAST hits in output file
	my @hits	= ($blastout =~ /(<Hit>.+?<\/Hit>)/sg);
=cut
        <Hit>
          <Hit_num>1</Hit_num>
          <Hit_id>gnl|BL_ORD_ID|0</Hit_id>
          <Hit_def>chr1</Hit_def>
          <Hit_accession>0</Hit_accession>
          <Hit_len>249250621</Hit_len>
          <Hit_hsps>
            <Hsp>
              <Hsp_num>1</Hsp_num>
              <Hsp_bit-score>42.1223</Hsp_bit-score>
              <Hsp_score>21</Hsp_score>
              <Hsp_evalue>0.00259175</Hsp_evalue>
              <Hsp_query-from>1</Hsp_query-from>
              <Hsp_query-to>21</Hsp_query-to>
              <Hsp_hit-from>27106815</Hsp_hit-from>
              <Hsp_hit-to>27106835</Hsp_hit-to>
              <Hsp_query-frame>1</Hsp_query-frame>
              <Hsp_hit-frame>1</Hsp_hit-frame>
              <Hsp_identity>21</Hsp_identity>
              <Hsp_positive>21</Hsp_positive>
              <Hsp_align-len>21</Hsp_align-len>
              <Hsp_qseq>CCGCCTGGAGAAGTTGTATAG</Hsp_qseq>
              <Hsp_hseq>CCGCCTGGAGAAGTTGTATAG</Hsp_hseq>
              <Hsp_midline>|||||||||||||||||||||</Hsp_midline>
            </Hsp>
=cut

	# if no hits are found, try the search again allowing low complexity
	# regions
	if(scalar(@hits) == 0 && $self->{'allow_low_complexity'} == 0) {
		$cmd .= qq{ -F F };
		print STDERR "Trying low-complexity search: $cmd\n" if($self->{'loglevel'});
		$blastout	= `$cmd`;
  		$blastout	=~ /<BlastOutput\_query\-len>(\d+)<\/BlastOutput\_query\-len>/;
		$qlen	= $1;
		@hits	= ($blastout =~ /(<Hit>.+?<\/Hit>)/sg);
	}
	if(scalar(@hits) == 0) {
		return(1);
	}

	print STDERR scalar(@hits)." hits found, checking primer specificity\n" if($self->{'loglevel'});
	my $status	= $self->check_blast_hits(HITS => \@hits, QLEN => $qlen, PRIMERPOS => $primerpos);

	return(1) if($status != 0);
}

################################################################################
=pod

B<check_blast_hits()> 
 Check each BLAST hit

Parameters:
 HITS		=> BLAST hits, in array
 PRIMERPOS	=> specify which of the primer pair is being BLASTed ('l','r')

 Requires:
  $self->{'rs_left'}
  $self->{'rs_right'}
  $self->{'leftmax'}
  $self->{'rightmax'}

Returns:
 scalar - 0 if primer is specific, 1 if primer is not specific

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
	my $primerpos	= $options->{'PRIMERPOS'};

	my $rs		= $self->{'rs'};
	my $leftmax	= $self->{'leftmax'};
	my $rightmax	= $self->{'rightmax'};
	if($self->{'complexfeature'} eq 'y' && $primerpos eq 'l') {
		$rs		= $self->{'rs_left'};
		$leftmax	= $self->{'leftmax_l'};
		$rightmax	= $self->{'rightmax_l'};
	}
	elsif($self->{'complexfeature'} eq 'y' && $primerpos eq 'r') {
		$rs		= $self->{'rs_right'};
		$leftmax	= $self->{'leftmax_r'};
		$rightmax	= $self->{'rightmax_r'};
	}

	print STDERR "*****$primerpos -> $rs : $leftmax / $rightmax\n"  if($self->{'loglevel'});

=cut
          <Hit_id>lcl|chr1</Hit_id>
          <Hit_def>No definition line found</Hit_def>

          <Hit_id>gnl|BL_ORD_ID|0</Hit_id>
          <Hit_def>chr1</Hit_def>
=cut

	# go through all hits until a good hit on another genomic region is
	# found; if a hit is found, skip this primer pair - don't want primer to
	# anneal to any other place on genome except requested SNP
	my $status = 0;	# consider things good until a bad thing is found (1)
	foreach my $hit (@{$hits}) {
		$hit		=~ /<Hit_def>(.+?)<\/Hit_def>/;
		my $hitdef	= $1;

		my $curr_rs	= '';
		if($hitdef =~ /No definition line/) {
			$hit		=~ /<Hit_id>.+?\|(.+?)\W*<\/Hit_id>/;
			$curr_rs	= $1;
		
		}
		else {
			$hit		=~ /<Hit_def>(.+?)<\/Hit_def>/;
			$curr_rs	= $1;
		}
		$hit		=~ /<Hit_num>(.+?)<\/Hit_num>/;
		my $curr_hit	= $1;

		#print STDERR "Hit $curr_hit on $curr_rs (SNP on ".$self->{'rs'}.")\n";
		#print STDERR "Hit $curr_hit on $curr_rs (Variant position on ".$rs.")\n";
		#if($curr_rs ne $self->{'rs'} && $curr_hit == 1) {
		if($curr_rs ne $rs && $curr_hit == 1) {
			# BAD PAIR
			#print STDERR "BAD PRIMER: First hit not on same RS: $curr_rs not on ".$self->{'rs'}."\n" if($self->{'loglevel'});
			print STDERR "BAD PRIMER: First hit not on same RS: $curr_rs not on ".$rs."\n" if($self->{'loglevel'});
			$status = 1;
			return(1);
		}

		my @hsps	= ($hit =~ /(<Hsp>.+?<\/Hsp>)/sg);

		#print Dumper @hsps;

		foreach my $hsp (@hsps) {
			# first hit should be itself; if not primer is bad
			#  if hit is on same ref sequence but outside of primer
			#  range, primer is bad
			#print STDERR "CHECKING HSP:---\n";
			$hsp	=~ /<Hsp_hit-from>(\d+)<\/Hsp_hit-from>/;
			my $fr	= $1;
			$hsp	=~ /<Hsp_hit-to>(\d+)<\/Hsp_hit-to>/;
			my $to	= $1;

			#print STDERR "Checking hit range: $fr - $to\n" if($self->{'loglevel'});
			#print STDERR "Range: ".$leftmax." - ".$rightmax."\n" if($self->{'loglevel'});

			if($hsp =~ /<Hsp_num>1<\/Hsp_num>/ && $curr_hit == 1) {
				#unless($curr_rs eq $self->{'rs'} && ($fr >= $self->{'leftmax'} && $to <= $self->{'rightmax'})) {
				#unless($curr_rs eq $rs && ($fr >= $self->{'leftmax'} && $to <= $self->{'rightmax'})) {
				#print STDERR "CHECKING $curr_rs eq $rs && ($fr >= $leftmax && $to <= $rightmax)\n"  if($self->{'loglevel'});
				unless($curr_rs eq $rs && ($fr >= $leftmax && $to <= $rightmax)) {
					# BAD PAIR
					print STDERR "BAD PRIMER: First hit not in range\n" if($self->{'loglevel'}); 
					$status = 1;
					return(1);
				}
				#print STDERR "First hit in range and on same RS\n" if($self->{'loglevel'});
				next;
			}

			# if hit that isn't itself is good, primer is bad
              		$hsp	=~ /<Hsp_align-len>(\d+)<\/Hsp_align-len>/;
			my $tlen	= $1 / $qlen;
              		$hsp	=~ /<Hsp_midline>(.+?)<\/Hsp_midline>/;
			my $percid	= length($1) / $qlen;
			#print STDERR "Checking length and ID: $tlen >= 0.9 && $percid >= 0.85\n";
			if($tlen >= 0.9 && ($percid / $qlen) >= 0.85) {
				print STDERR "BAD PRIMER: BLAST hit has 90%+ length and 85%+ ID\n"   if($self->{'loglevel'});
				#print STDERR "     $tlen >= 0.9 && $percid >= 0.85\n";
				$status = 1;
				return(1);
			}
		}
	}

	print STDERR "Primer ok\n" if($self->{'loglevel'});

	return(0);
}


################################################################################
=pod

B<add_adapters()> 
 Add sequencing adapters to primers

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file
 LEFT	=> adapter sequence for left primer
 RIGHT	=> adapter sequene for right primer

Returns:
 scalar - primer3 pair, excerpted from primer3 output file

 Sets:
  $self->{'l_adapter'}
  $self->{'r_adapter'}

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

	# keep adapter sequences
	$self->{'l_adapter'}	= $la;
	$self->{'r_adapter'}	= $ra;

	if($self->{'mode'} =~ /miseq/) {
		$self->{'l_adapter'}	= "";
		$self->{'r_adapter'}	= "";
	}

	my ($leftid, $leftseq);
	# if barcode was added, retain it and prepend adapter
	if($pair =~ /PRIMER_LEFT_\d+_W_BARCODE/) {
		$pair		=~ /PRIMER_(LEFT_\d+)_W_BARCODE=(.+)/;
		$leftid		= $1;
		$leftseq	= $2;	# left primer sequence w barcode
	}
	# otherwise, just use left primer sequence
	else {
		$pair		=~ /PRIMER_(LEFT_\d+)_SEQUENCE=(.+)/;
		my $leftid	= $1;
		my $leftseq	= $2;
	}

	$pair		=~ /PRIMER_(RIGHT_\d+)_SEQUENCE=(.+)/;
	my $rightid	= $1;
	my $rightseq	= $2;

	$leftseq	= $la.$leftseq;
	$rightseq	= $ra.$rightseq;

	$pair		.= "\nPRIMER_".$leftid."_W_ADAPTER=".$leftseq."\n";
	$pair		.= "PRIMER_".$rightid."_W_ADAPTER=".$rightseq."\n";

        return($pair);
}

################################################################################
=pod

B<add_barcode()> 
 Add barcode to left primer sequence (assuming left adapter has not been added)

Parameters:
 PAIR		=> primer3 pair, excerpted from primer3 output file
 BARCODE	=> adapter sequence for left primer

Returns:
 scalar - primer3 pair, excerpted from primer3 output file

 Sets:
  $self->{'barcode'}

=cut
sub add_barcode {
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
	my $barcode	= $options->{'BARCODE'};

	$self->{'barcode'}	= $barcode;

	$pair		=~ /PRIMER_(LEFT_\d+)_SEQUENCE=(.+)/;
	my $leftid	= $1;
	my $leftseq	= $2;

	$leftseq	= $barcode.$leftseq;

	$pair		.= "\nPRIMER_".$leftid."_W_BARCODE=".$leftseq;

	print STDERR "Adding barcode $barcode to $leftseq\n" if($self->{'loglevel'});

        return($pair);
}

################################################################################
=pod

B<check_autodimer()> 
 Check that primers to not anneal to each other

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

 Requires:
  $self->{'left_blast_file'}
  $self->{'alias'}

Returns:
 scalar - bool; 1 if bad; 0 if not bad

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

	#print STDERR "$pair\n";

	# PRIMER__W_ADAPTER
	my ($leftid, $leftseq);
	if($pair =~ /PRIMER_LEFT_\d+_W_ADAPTER=/) {
		$pair		=~ /PRIMER_(LEFT_\d+)_W_ADAPTER=(.+)/;
		$leftid		= $1;
		$leftseq	= $2;
	}
	else {
		$pair		=~ /PRIMER__W_ADAPTER=(.+)/;
		$leftseq	= $1;
		$pair		=~ /PRIMER_(LEFT_\d+)/;
		$leftid		= $1;
	}
	#print STDERR "LEFT: $leftid $leftseq\n";
	# use existing BLAST input file for left primer
	my $l_bl_file	= $self->{'left_blast_file'}->{$leftid};

	$pair		=~ /PRIMER_(RIGHT_\d+)_W_ADAPTER=(.+)/;
	my $rightid	= $1."_revcomp";
	my $rightseq	= $2;
	# reverse right primer sequence and create the complement
	$rightseq	= reverse($rightseq);
	$rightseq	=~ tr/CATG/GTAC/;
	my $r_bl_file	= "/tmp/".$self->{'alias'}."_".$rightid.".fa";

	open(FH, ">".$r_bl_file) || die "Cannot open $r_bl_file : $!\n";
	print FH ">$rightid\n$rightseq\n";
	close(FH);
	push @tmpfiles, $r_bl_file;
	#print STDERR "TEMPFILE: $r_bl_file\n";

	# run BLAST
	# /panfs/share/software/blast-2.2.28+/bin//blastn -query /tmp/chr12_25398280_25398280_RIGHT_100.fa -subject /tmp/chr12_25398280_25398280_LEFT_9.fa -outfmt 7 -evalue 1 -xdrop_gap 200 -dust no
	my $cmd		= qq{$path_blast/bl2seq -i $l_bl_file -j $r_bl_file -p blastn -F F -e 1 -X 200 -D 1};
	#my $cmd		= qq{$path_blast/blastn -query $l_bl_file -subject $r_bl_file -outfmt 7 -evalue 1 -xdrop_gap 200 -dust no};
	print STDERR "$cmd\n" if($self->{'loglevel'});
	my $blout	= `$cmd`;

	#print STDERR "$blout\n";

	my @lines	= split /\n/, $blout;
	foreach (@lines) {
		unless(/^#/) {
			print STDERR "Auto-dimerization possible ($_)\n" if($self->{'loglevel'});
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
 PAIR	=> primer3 pair, excerpted from primer3 output file
 LEFT	=> BLAST input file for left primer

Returns:
 scalar - bool; 1 if bad; 0 if not bad

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
 Find palindrome in a sequence (cribbed from EMBOSS palindrome)

Parameters:
 SEQ	=> primer sequence

Returns:
 scalar - number of palindromes found in sequence

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
	
			# if found a potential start of a palindrome
			if($base1 eq $base2) {
				while($mismatches <= $maxmms && $ic < $ir) {
					# get bases to compare
					my $nextbase1 = $bases[$ic++];
					my $prevbase2 = $BASECOMP{$bases[$ir--]};
	
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
				#print STDERR "\t****PALINDROME FOUND\n";
				$numpals++;
			}
		}
	}
	
	print STDERR "Palindromes found: ", $numpals, "\n" if($self->{'loglevel'});

	return($numpals);	# 0 = no palindromes, >0 = palindromes
}

################################################################################
=pod

B<format_header()> 
 Format header for output file

Parameters:
 none

Returns:
 scalar - string of tab-delimited column headers

 Sets:
  $self->{'output_header'}

=cut
sub format_header {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my @header = (
		'position',
		'primer_left_seq',
		'primer_right_seq',
		'primer_left_seq_adapter',
		'primer_right_seq_adapter',
		'barcode',
		'amplicon_size',
		'amplicon_seq',
		'amplicon_range',
		'left_Tm',
		'right_Tm',
		'left_gc',
		'right_gc',
		'left_primer_to_from',
		'right_primer_to_from',
		'left_to_position',
		'right_to_position'
	);

	$self->{'output_header'} = \@header;

	my $line	= join "\t", @header;
	$line		.= "\n";

	return($line);
}

################################################################################
=pod

B<format_primer()> 
 Format primer for printing to output file

Parameters:
 PAIR	=> primer3 pair, excerpted from primer3 output file

 Requires:
  $self->{'genome'}
  $self->{'coord'}
  $self->{'pos'}
  $self->{'output_header'}
  $self->{'barcode'}
  $self->{'sequence'}

Returns:
 scalar - string of tab-delimited primer attributes (matches header)

=cut
sub format_primer {
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

	# amplicon size
	$pair		=~ /PRIMER_PAIR_(\d+)_PRODUCT_SIZE=(.+)/;
	#my $ampsize	= $1;
	my $ampsize	= $2;

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

	# primer sequences with adapters (will contain barcode if one was used)
	$pair		=~ /PRIMER_LEFT_\d+_W_ADAPTER=(.+)/;
	my $leftaseq	= $1;
	$pair		=~ /PRIMER_RIGHT_\d+_W_ADAPTER=(.+)/;
	my $rightaseq	= $1;

	if($self->{'mode'} =~ /miseq/) {
		$leftaseq	= "";
		$rightaseq	= "";
	}

	# primer genomic coordinates
	my ($leftcoord, $rightcoord)	= $self->get_primer_pair_coords(PAIR => $pair);



	### handle this differently if a ranged feature is used
	my $pos		= $self->{'pos'};
	my ($ampseq, $ampcoord, $ampsize, $lefttosnp, $righttosnp);
	if($self->{'complexfeature'} eq 'y') {
		my @data	= split /\t/, $self->{'pos'};
		$pos		= join ",", @data;

		$ampcoord	= join "|", $leftcoord, $rightcoord;

		$leftcoord	=~ /\-(\d+)$/;
		$lefttosnp	= $self->{'rightmax_l'} - $1;
		$rightcoord	=~ /\:(\d+)\-/;
		$righttosnp	= $1 - $self->{'leftmax_r'};

		$pair		=~ /PRIMER_LEFT_\d+=(\d+),(\d+)/;
		my $pl		= $1;
		my $pl_r	= $2;
		$pair		=~ /PRIMER_RIGHT_\d+=(\d+),(\d+)/;
		my $pr		= ($1 + 2) - $pl;
		my $pr_r	= $2;

		#print STDERR "SEQ ".$self->{'sequence'}."\n";
		#print STDERR "$pl $pr\n";

		# extract substring from sequence build from both genomic ranges
		$ampseq		= substr($self->{'sequence'}, $pl, $pr);
		$ampsize	= length($ampseq);

	}
	else {
		# amplicon genomic coordinate
		$leftcoord	=~ /(.+?\:\d+\-)/;
		my $ampcoord	= $1;
		$rightcoord	=~ /.+?\:\d+\-(\d+)/;
		$ampcoord	.= $1;

		# amplicon sequence
		#my $fai	= QCMG::SamTools::Sam::Fai->load($self->{'genome'});
		#my $ampseq	= $fai->fetch($ampcoord);
		$ampseq	= $self->fetch_subsequence(GENOME => $self->{'genome'}, POS => $ampcoord);

		# get distance from left primer to SNP
		$leftcoord	=~ /.+?\:(\d+)/;
		$lefttosnp	= $self->{'coord'} - $1;
		# get distance from right primer to SNP
		$rightcoord	=~ /.+?\:\d+\-(\d+)/;
		$righttosnp	= $1 - $self->{'coord'};
	}

	my %PRIMER = (
			'position'			=> $pos,
			'primer_left_seq'		=> $leftseq,
			'primer_right_seq'		=> $rightseq,
			'primer_left_seq_adapter'	=> $leftaseq,
			'primer_right_seq_adapter'	=> $rightaseq,
			'barcode'			=> $self->{'barcode'},
			'amplicon_size'			=> $ampsize,
			'amplicon_seq'			=> $ampseq,
			'amplicon_range'		=> $ampcoord,
			'left_Tm'			=> $lefttm,
			'right_Tm'			=> $righttm,
			'left_gc'			=> $leftgc,
			'right_gc'			=> $rightgc,
			'left_primer_to_from'		=> $leftcoord,
			'right_primer_to_from'		=> $rightcoord,
			'left_to_position'		=> $lefttosnp,
			'right_to_position'		=> $righttosnp
		);


	my $line;

	$self->format_header();

	foreach (@{$self->{'output_header'}}) {
		$line	.= $PRIMER{$_}."\t";
	}

	$line		=~ s/\t$//;
	$line		.= "\n";

	return($line);
}

################################################################################
=pod

B<read_dbsnp()> 
 Read dbSNP flat file

Parameters: 
 DBSNP

 Requires:
  $self->{'rs'}
  $self->{'leftmax'}
  $self->{'rightmax'}

  $self->{'rs_left'}
  $self->{'rs_right'}
  $self->{'leftmax_l'}
  $self->{'leftmax_r'}
  $self->{'rightmax_l'}
  $self->{'rightmax_r'}

Returns:
 ref to hash of dbSNP positions for the same reference sequence as position of
  interest

 Sets:
  $self->{'snps'} - ref to hash of dbSNP positions

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

	print STDERR "Extracting relevant SNPs from dbSNP\n" if($self->{'loglevel'});
	print "********************Extracting relevant SNPs from dbSNP\n";

	my @snps = ();

	if($self->{'complexfeature'} eq 'n') {
		foreach my $snp (@{$DBSNP->{$self->{'rs'}}}) {
			$snp	+= 1;	# adjust for 1-based coordinates in dbSNP
	
			unless($snp < $self->{'leftmax'} || $snp > $self->{'rightmax'}) {
				push @{$self->{'snps'}}, $snp;
			}
		}
	}
	else {
		foreach my $snp (@{$DBSNP->{$self->{'rs_left'}}}) {
			$snp	+= 1;	# adjust for 1-based coordinates in dbSNP

			unless($snp < $self->{'leftmax_l'} || $snp > $self->{'rightmax_l'}) {
				push @{$self->{'snps'}}, $snp;
			}
		}
		foreach my $snp (@{$DBSNP->{$self->{'rs_right'}}}) {
			$snp	+= 1;	# adjust for 1-based coordinates in dbSNP

			unless($snp < $self->{'leftmax_r'} || $snp > $self->{'rightmax_r'}) {
				push @{$self->{'snps'}}, $snp;
			}
		}
	}

	print Dumper $self->{'snps'};

        return($self->{'snps'});
}

################################################################################
=pod

B<delete_tmpfile()> 
 Delete temp files

Parameters:
 none

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
		print STDERR "Deleting $_\n" if($self->{'loglevel'});
		unlink($_);
	}

        return();
}


################################################################################
=pod

B<timestamp()> 
 Generate a timestamp in the format: 2003-02-14-16:37:46
                                     030214
                                     030214163746

Parameters:
 FORMAT 
   - ISO8601       - Default timestamp, ISO 8601 format
   - YYMMDD        - timestamp in YYYYMMDD format 
   - YYMMDDhhmmss  - timestamp in YYYYMMDDhhmmss format 

Returns:
 scalar - timestamp string

=cut
sub timestamp {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	#print STDERR "TIMESTAMP ".$options->{'FORMAT'}."\n";

	# should be in ISO 8601 format: [2011-02-19 23:59:99Z]
	my $stamp = lc strftime("[%Y-%m-%d %H:%M:%S]", localtime); 

	# return date in YYMMDD format
	if($options->{'FORMAT'} eq 'YYMMDD') {
		$stamp = uc strftime("%Y%m%d", localtime);
	}
	elsif($options->{'FORMAT'} =~ /yymmddhh/i) {
		$stamp = uc strftime("%Y%m%d%H%M%S", localtime);
	}

	return($stamp);
}


################################################################################
=pod

B<DESTROY()> 
 Destructor; write logging information to log file

Parameters:
 none

 Requires:
  $self->{'EXIT'}

Returns:
 void

=cut
sub DESTROY {
	my $self = shift;

	if($self->{'EXIT'} == 0) {
		$self->writelog(BODY => "EXECLOG: errorMessage none");
	}
	else {
		$self->writelog(BODY => "EXECLOG: errorMessage failed");
	}

	my $date	= $self->timestamp();

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
