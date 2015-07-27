package QCMG::AlexaSeq::Bam; 
##############################################################################
#
#  Module:   QCMG::AlexaSeq::Bam.pm
#  Creators: Lynn Fink and David Wood
#  Created:  2012-06-26
#
#  This class contains methods for converting AlexaSeq output into a BAM file
#
#  $Id$
#
##############################################################################

=pod

=head1 NAME

QCMG::AlexaSeq::Bam -- Common functions for converting AlexaSeq output to a BAM

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::AlexaSeq::Bam->new();

=head1 DESCRIPTION

Contains methods for finding AlexaSeq output files for a slide, translating the
read records to alignment results, converting the results to fully-qualified SAM
lines, and writing a BAM

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'
 QCMG::Automation::Common

=cut

use strict;
# standard distro modules
use Data::Dumper;
use File::Spec;					# for parsing paths
use Cwd;
use IO::Zlib;
# in-house modules
use QCMG::Automation::Common;
use QCMG::AlexaSeq::Object;
use QCMG::AlexaSeq::Read;

our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods

our $ALIGNRESDIR	= "alignment_results/";
our $BLASTRESDIR	= "blast_results/";
our $READRECDIR		= "read_records/";

our @featuretypes	= (
	"exonBoundaries",
	"exonJunctions",
	"intergenics",
	"introns",
	"transcripts"
);

our %ftype2name		= (
	'exonBoundaries'	=> 'Boundaries',
	'exonJunctions'		=> 'Junctions',
	'intergenics'		=> 'Intergenics',
	'introns'		=> 'Introns',
	'transcripts'		=> 'ENST'
);

our %feature2labels	= (
	'exonBoundaries'	=> ['BOUNDARY_U', 'NOVEL_BOUNDARY_U'],
	'Boundaries'		=> ['BOUNDARY_U', 'NOVEL_BOUNDARY_U'],
	'exonJunctions'		=> ['NOVEL_JUNCTION_U'],
	'Junctions'		=> ['NOVEL_JUNCTION_U'],
	'intergenics'		=> ['INTERGENIC_U'],
	'Intergenics'		=> ['INTERGENIC_U'],
	'introns'		=> ['INTRON_U'],
	'Introns'		=> ['INTRON_U'],
	'transcripts'		=> ['ENST_U'],
	'ENST'			=> ['ENST_U']
);

our %feature2flag	= (
	'exonBoundaries'	=> 'EB',
#	'Boundaries'		=> 'EB',
	'exonJunctions'		=> 'NJ',
#	'Junctions'		=> 'NJ',
	'intergenics'		=> 'IG',
#	'Intergenics'		=> 'IG',
	'introns'		=> 'IN', 
#	'Introns'		=> 'IN', 
	'transcripts'		=> 'TX',
#	'ENST'			=> 'TX',
);

our %gene2transcriptIDs = ();
our %txExonStarts = ();
our %txExonLengths = ();

use vars qw( $SVNID $REVISION );

################################################################################
=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 ANALYSIS	=> analysis directory
 SEQDB		=> sequence database directory
 SEQDBNAME	=> sequence database name
 SEQDBVER	=> sequence database version
 LIB_ID		=> library id
 READLEN1	=> length of F3 read (optional)
 READLEN2	=> length of F5 read (optional)
 JUNCDIFF1	=> number of bases feature is truncated for long read (optional)
 JUNCDIFF2	=> number of bases feature is truncated for short read (optional)

 LOG_FILE   	=> path and filename of log file
 NO_EMAIL   	=> 1 (do not send emails), 0 (send emails)
 VERBOSE	=> 0 (no messages), 1 (print info messages to STDERR)
 ALIGNRES	=> alignment_results subdirectory
 READREC	=> read_records subdirectory

Returns:
 a new instance of this class.

Sets:
 $self->{'analysis'}
 $self->{'seqdb_dir'}

 $self->{'dbname'}
 $self->{'dbver'}
 $self->{'hostname'}
 $self->{'log_file'}
 $self->{'no_email'}
 $self->{'verbose'}

 $self->{'alignment_results'}
 $self->{'read_records'}

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

	# define LOG_FILE
	if($options->{'LOG_FILE'}) {
		$self->{'log_file'}		= $options->{'LOG_FILE'};
	}
	else {
		# default to current directory and generic name if no log file
		# provided
		my $cwd				= getcwd;
		$self->{'log_file'}		= $cwd."/alexa2bam.log";
	}
	$self->init_log_file();

	# get current hostname for logging information
	$self->{'hostname'}		= `hostname`;
	chomp $self->{'hostname'};

	### process parameters
	# set verboseness
	$self->{'verbose'}		= 0;	# default to no messages
	if($options->{'VERBOSE'} && $options->{'VERBOSE'} != 0) {
		$self->{'verbose'}	= 1;
	}

	# if NO_EMAIL => 1, don't send emails
	$self->{'no_email'}		= $options->{'NO_EMAIL'};

	$self->{'FIX_CHR_NAMES'} = 0;
	if (defined($options->{'FIX_CHR_NAMES'})) {
		$self->{'FIX_CHR_NAMES'} = $options->{'FIX_CHR_NAMES'};
	}

	# analysis directory path
	$self->{'analysis_dir'}		= $options->{'ANALYSIS'};
	print "Analysis dir:\t$self->{'analysis_dir'}\n" if($self->{'verbose'} > 0);

	# library ID
	$self->{'library_id'}		= $options->{'LIB_ID'};
	print "Library ID:\t$self->{'library_id'}\n" if($self->{'verbose'} > 0);

	# sequence database directory path
	$self->{'seqdb_dir'}		= $options->{'SEQDB'};
	print "Seq db dir:\t$self->{'seqdb_dir'}\n" if($self->{'verbose'} > 0);

	# sequence database name
	$self->{'seqdb_name'}		= $options->{'SEQDBNAME'};
	print "Seq db name:\t$self->{'seqdb_name'}\n" if($self->{'verbose'} > 0);

	# sequence database directory path
	$self->{'seqdb_ver'}		= $options->{'SEQDBVER'};
	print "Seq db version:\t$self->{'seqdb_ver'}\n" if($self->{'verbose'} > 0);

	# sequence database directory path
	$self->{'is_frag_run'}		= $options->{'IS_FRAG_RUN'};
	print "Is Frag Run:\t$self->{'is_frag_run'}\n" if($self->{'verbose'} > 0);

	# junction difference length:
	$self->{'junc_diff'}        = $options->{'JUNC_DIFF'};
	print "Junction Differences:\t$self->{'junc_diff'}\n" if($self->{'verbose'} > 0);
	($self->{'juncdiff1'}, $self->{'juncdiff2'}) = split(/\,/,$self->{'junc_diff'});

	# set any truncations for features
	$self->{'readlen1'}		= 50;	# DEFAULT
	$self->{'readlen2'}		= 35;	# DEFAULT
	$self->{'readlen1'}		= $options->{'READLEN1'} if($options->{'READLEN1'});
	$self->{'readlen2'}		= $options->{'READLEN2'} if($options->{'READLEN2'});

	# set defaults
	$self->{'read_records_name'}		= $READRECDIR;
	$self->{'alignment_results_name'}	= $ALIGNRESDIR;
	$self->{'blast_results_name'}	= $BLASTRESDIR;
	# override defaults if requested; change the preset names of the
	# subdirectories under analysis/
	$self->{'read_records_name'}		= $options->{'READREC'} if($options->{'READREC'});
	$self->{'alignment_results_name'}	= $options->{'ALIGNRES'} if($options->{'ALIGNRES'});
	$self->{'blast_results_name'}	= $options->{'BLASTRES'} if($options->{'BLASTRES'});

	# make sure everything is set
	unless ($self->{'analysis_dir'} &&
		$self->{'library_id'} &&
		$self->{'seqdb_dir'} &&
		$self->{'seqdb_name'} &&
		$self->{'seqdb_ver'} &&
		$self->{'read_records_name'} &&
		$self->{'blast_results_name'} &&
		defined($self->{'is_frag_run'}) &&
		$self->{'alignment_results_name'} ) {
		exit($self->EXIT_MISSING_PARAM());
	}

	$self->alignment_results();
	$self->blast_results();
	$self->read_records();

	return $self;
}

# Exit codes:
sub EXIT_NO_DIR {
	my $self	= shift @_;
	my $rv		= 1;
	print STDERR "FATAL: No valid analysis or sequence_database directory provided\n";
	$self->{'EXIT'} = $rv;
	return $rv;
}
sub EXIT_MISSING_PARAM {
	my $self	= shift @_;
	my $rv		= 2;
	print STDERR "FATAL: Missing required input parameter\n";
	$self->{'EXIT'} = $rv;
	return $rv;
}
sub EXIT_NO_ANNOTATION {
	my $self	= shift @_;
	my $rv		= 3;
	print STDERR "FATAL: Cannot find a feature annotation file\n";
	$self->{'EXIT'} = $rv;
	return $rv;
}
sub EXIT_NO_ALIGNMENTS {
	my $self	= shift @_;
	my $rv		= 4;
	print STDERR "FATAL: Found no alignments for a read\n";
	$self->{'EXIT'} = $rv;
	return $rv;
}

################################################################################
=pod

B<alignment_results()>

Set path to alignment_results

Sets:
 $self->{'alignment_results'}

Returns:
  scalar path

=cut
sub alignment_results {
	my $self	= shift @_;

	$self->{'alignment_results'}	= join "/", $self->{'analysis_dir'}, $self->{'alignment_results_name'};

	return $self->{'alignment_results'};
}


################################################################################
=pod

B<blast_results()>

Set path to blast_results

Sets:
 $self->{'blast_results'}

Returns:
  scalar path

=cut
sub blast_results {
	my $self	= shift @_;

	$self->{'blast_results'}	= join "/", $self->{'analysis_dir'}, $self->{'blast_results_name'};

	return $self->{'blast_results'};
}


################################################################################
=pod

B<read_records()>

Set path to read_records

Sets:
 $self->{'read_records'}

Returns:
  scalar path

=cut
sub read_records {
	my $self	= shift @_;

	$self->{'read_records'}	= join "/", $self->{'analysis_dir'}, $self->{'read_records_name'};

	return $self->{'read_records'};
}

################################################################################
=pod

B<summary_file()>

Set path and filename of summary of read records

Parameters:
 LANE	=> lane ID

Requires:
 $self->{'read_records'}
 $self->{'library_id'}

Sets:

Returns:
  scalar path

=cut
sub summary_file {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $lane	= $options->{'LANE'};

	my $file	= join "/", $self->{'read_records'}, $self->{'library_id'}, $self->{'library_id'}."_".$lane.".txt.gz";

	return $file;
}

################################################################################
=pod

B<lane_alignres_dir()>

Set path and filename of directory containing SAM files for a lane and feature

Parameters:
 LANE	=> lane ID
 TYPE	=> feature type

Requires:
 $self->{'alignment_results'}
 $self->{'library_id'}

Sets:

Returns:
  scalar path

=cut
sub lane_alignres_dir {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

=pod

|-- analysis
|   |-- alignment_results
|   |   `-- 000PBLIV
|   |       `-- 000PBLIV_Lane61
|   |           |-- exonBoundaries
|   |           |   |-- blast_1000.sam.gz
|   |           |   |-- blast_1001.sam.gz
|   |           |   |-- blast_2000.sam.gz
|   |           |   `-- blast_2001.sam.gz

=cut



	my $lane	= $options->{'LANE'};
	my $type	= $options->{'TYPE'};

	my $path	= join "/", $self->{'alignment_results'}, $self->{'library_id'}, $self->{'library_id'}."_".$lane, $type;

	return $path;
}


################################################################################
=pod

B<lane_blastres_dir()>

Set path and filename of directory containing parsed blast result files for a lane and feature

Parameters:
 LANE	=> lane ID
 TYPE	=> feature type

Requires:
 $self->{'blast_results'}
 $self->{'library_id'}

Sets:

Returns:
  scalar path

=cut
sub lane_blastres_dir {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

=cut
|-- analysis
|   |-- blast_results
|   |   `-- 000PBLIV
|   |       `-- 000PBLIV_Lane61
|   |           |-- exonBoundaries
|   |           |   |-- blast_1000.gz
|   |           |   |-- blast_1001.gz
|   |           |   |-- blast_2000.gz
|   |           |   `-- blast_2001.gz
=cut



	my $lane	= $options->{'LANE'};
	my $type	= $options->{'TYPE'};

	my $path	= join "/", $self->{'blast_results'}, $self->{'library_id'}, $self->{'library_id'}."_".$lane, $type;

	return $path;
}

################################################################################
=pod

B<alignment_results_name()>

Set path to alignment_results_name

Sets:
 $self->{'alignment_results_name'}

Returns:
  scalar path

=cut
sub alignment_results_name {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if($options->{'PATH'}) {
		$self->{'alignment_results_name'}	= $options->{'PATH'};
	}

	return $self->{'alignment_results_name'};
}

################################################################################
=pod

B<read_records_name()>

Set or get AlexaSEQ read_records directory; if PATH is set, this will be the new
path; if not, reurn the current path

Parameters:
 PATH	=> alexa_root/read_records/ (optional)

Sets/Requires:
 $self->{'read_records_name'}

Returns:
  scalar path

=cut
sub read_records_name {
	my $self	= shift @_;

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if($options->{'PATH'}) {
		$self->{'read_records_name'}	= $options->{'PATH'};
	}

	return $self->{'read_records_name'};
}

################################################################################
=pod

B<log_file()> 
 Return log filename

Parameters:

Returns:
  scalar: filename

=cut
sub log_file {
        my $self = shift @_;

	return($self->{'log_file'});
}


################################################################################
=pod

B<check_dir()>

Check that directory(s) provided by user exists, is a directory, and is readable

Parameters:

Requires:
 $self->{'analysis_dir'}
 $self->{'seqdbdir'}

Returns:

=cut
sub check_dir {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

	$self->writelog(BODY => "--- Checking ".$self->{'analysis_dir'}." ---");

	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if(	!    $self->{'analysis_dir'} ||
		! -e $self->{'analysis_dir'} || 
		! -d $self->{'analysis_dir'}    ) {

		$self->writelog(BODY => "No valid run folder provided\n");
		exit($self->EXIT_NO_DIR());
	}

	if(	! -r $self->{'analysis_dir'}) {
		my $msg = " is not a readable directory";
		$self->writelog(BODY => $self->{'analysis_dir'}.$msg);
		exit($self->EXIT_BAD_PERMS());
	}

	$self->writelog(BODY => "Analysis directory exists and is readble:");	
	$self->writelog(BODY => "\t".$self->{'analysis_dir'});	

	if(	!    $self->{'seqdb_dir'} ||
		! -e $self->{'seqdb_dir'} || 
		! -d $self->{'seqdb_dir'}    ) {

		$self->writelog(BODY => "No valid run folder provided\n");
		exit($self->EXIT_NO_DIR());
	}

	if(	! -r $self->{'seqdb_dir'}) {
		my $msg = " is not a readable directory";
		$self->writelog(BODY => $self->{'seqdb_dir'}.$msg);
		exit($self->EXIT_BAD_PERMS());
	}

	$self->writelog(BODY => "sequence_database directory exists and is readble:");	
	$self->writelog(BODY => "\t".$self->{'seqdb_dir'});	

	$self->writelog(BODY => "---");	

	return();
}

################################################################################
=pod

B<find_lanes()>

Find all lanes for the specified library by looking for their directory names in
the analysis directory

Parameters:

Requires:
 $self->{'analysis_dir'}

Sets:
 $self->{'lanes'} - ref to array of strings

Returns:
 $self->{'lanes'}

=cut
sub find_lanes {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

	$self->writelog(BODY => "--- Finding lanes for ".$self->{'library_id'}." ---");

	my @lanes	= ();

	# find /panfs/results/dwood/qalexa_bam_creation/analysis//alignment_results/ -type d -name "000PBLIV*"
	#my $cmd		= qq{find $self->{'alignment_results'} -type d -name $self->{'library_id'}*};
	opendir(LIB, "$self->{'alignment_results'}/$self->{'library_id'}") or die "Failed to read from $self->{'alignment_results'}/$self->{'library_id'}: $!\n";
	#$self->writelog(BODY => "$cmd");
	#my @dirs	= `$cmd`;
	my @dirs = grep /^[^\.]/, readdir(LIB);
	closedir LIB;

	# only keep directories that correspond to a lane
	# /panfs/results/dwood/qalexa_bam_creation/analysis//alignment_results/000PBLIV/000PBLIV_Lane61
	foreach (@dirs) {
		next unless(/lane/i);

		# 000PBLIV_Lane61 -> Lane61
		/$self->{'library_id'}\_(.+)/;
		push @lanes, $1;
	}

	my $lanelist		= join "\n", @lanes;
	$self->writelog(BODY => "Found lanes:\n$lanelist");	

	$self->{'lanes'}	= \@lanes;
  
	$self->writelog(BODY => "---");	

	return($self->{'lanes'});
}

################################################################################
=pod

B<find_reads()>

Find all read records for a given lane and a given feature type

Parameters:
 LANE	=> lane ID
 TYPE	=> feature type (exonBoundaries, intergenics, etc.)

Requires:
 $self->{'read_records'}
 %feature2labels

Returns:
 ref to hash of read objects

=cut
sub find_reads {
	my $self	= shift @_;

    #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }

	my $type	= $options->{'TYPE'};
	my $lane	= $options->{'LANE'};

	$self->writelog(BODY => "--- Checking ".$self->{'read_records'}." ---");

	$self->writelog(BODY => "Lane: $lane\nType: $type");

	my $summaryfile	= $self->summary_file(LANE => $lane);

	$self->writelog(BODY => "Reading summary file:\n\t$summaryfile");

	# get array of all feature labels that correspond to the specified
	# feature type; these are needed to pull out the appropriate reads from
	# the summary file
	my @labels	= @{$feature2labels{$type}};
	my $labellist	= join '|', @labels;
	$self->writelog(BODY => "Labels to find:\n$labellist");

	# get all read IDs that match the desired feature labels
	# ** these lines are not unique; some are duplicated
	#my @read_ids	= ();
	my %reads	= ();
    tie *FILE, 'IO::Zlib', $summaryfile, "rb";
    while (<FILE>) {
		# skip the undesired labels
		next unless(/$labellist/);

		chomp;
		my @info = split /\t/;
		
		# first capture the R1's
		foreach my $label (@labels) {
			if ($info[3] eq $label) {
				$reads{$info[1]} = QCMG::AlexaSeq::Read->new();
				$reads{$info[1]}->readid($info[1]);
				$reads{$info[1]}->alnfeaturetype($label);
			}
		}
		
		# next capture the R2's
		foreach my $label (@labels) {
			if ($info[4] eq $label) {
				$reads{$info[2]} = QCMG::AlexaSeq::Read->new();
				$reads{$info[2]}->readid($info[2]);
				$reads{$info[2]}->alnfeaturetype($label);
			}
		}
		if ($. % 10000 == 0) { $self->writelog(BODY => "Inspected $. Reads from Summary Read Record File (Loaded ".(keys %reads).")"); }
	}
	close FILE;

	my $count	= scalar(keys %reads);
	
	$self->writelog(BODY => "Found $count matching read IDs");	

	$self->writelog(BODY => "---");	

	return(\%reads);
}

################################################################################
=pod

B<filter_reads()>

Filter read records to find only those that are TopHits or Ambiguous

Parameters:
 LANE	=> lane ID
 TYPE	=> feature type (exonBoundaries, intergenics, etc.)
 READS	=> ref to hash of read objects
 FEATURES => ref to hash of features.

Requires:
 $self->{'read_records'}
 %feature2labels

Returns:
 ref to hash of read objects

=cut
sub filter_reads {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $type	= $options->{'TYPE'};
	my $lane	= $options->{'LANE'};
	my $reads	= $options->{'READS'};
	my $features	= $options->{'FEATURES'};

	# analysis/read_records/000PBLIV/Boundaries_v60/000PBLIV_Lane61_Boundaries_v60.txt.gz

	$self->writelog(BODY => "--- Filtering read IDs ---");

	$self->writelog(BODY => "Lane: $lane\nType: $type");

	my $path	= join '/', $self->{'read_records'}, $self->{'library_id'}, $ftype2name{$type}."_".$self->{'seqdb_ver'};
	my $file	= join '_', $self->{'library_id'}, $lane, $ftype2name{$type}, $self->{'seqdb_ver'}.".txt.gz";

	$file		= join '/', $path, $file;

	$self->writelog(BODY => "Reading detailed reads file:\n\t$file");

=cut
Read_ID HitType Boundary_ID     AlignmentLength PercentIdentity BitScore        Start   End     Strand
000PBLIV_6_396_1548_561_R2      Top_Hit 356463  35      100.00  69.7    12      46      -

Read_ID HitType Junction_ID     AlignmentLength PercentIdentity BitScore        Start   End     Strand
000PBLIV_6_396_1547_448_R1      Top_Hit 2974001 53      98.11   59.9    1       53      +

Read_ID DistanceBetweenReads    R1_ID   R1_HitType      R1_IntergenicName       R1_Strand       R1_AlignmentLength      R1_PercentIdentity      R1_BitScore     R1_Chr  R1_ChrStart  R1_ChrEnd        R2_ID   R2_HitType      R2_IntergenicName       R2_Strand       R2_AlignmentLength      R2_PercentIdentity      R2_BitScore     R2_Chr  R2_ChrStart     R2_ChrEnd
000PBLIV_6_396_1547_1065        NA      000PBLIV_6_396_1547_1065_R1     Top_Hit 13_8_36 +       50      92.00   67.7    13      53092289        53092338        000PBLIV_6_396_1547_1065_R2   None    NA      NA      NA      NA      NA      NA      NA      NA

Read_ID DistanceBetweenReads    R1_ID   R1_HitType      R1_IntronName   R1_Strand       R1_AlignmentLength      R1_PercentIdentity      R1_BitScore     R1_Chr  R1_ChrStart     R1_ChrEnd     R2_ID   R2_HitType      R2_IntronName   R2_Strand       R2_AlignmentLength      R2_PercentIdentity      R2_BitScore     R2_Chr  R2_ChrStart     R2_ChrEnd
000PBLIV_6_396_1546_1522        118     000PBLIV_6_396_1546_1522_R1     Top_Hit Y_1_148 +       50      96.00   83.7    Y       6844727 6844776 000PBLIV_6_396_1546_1522_R2     Top_Hit       Y_1_148 -       35      100.00  69.7    Y       6844811 6844845

Read_ID DistanceBetweenReads_Genomic    DistanceBetweenReads_Transcript R1_ID   R1_HitType      R1_GeneID       R1_GeneName     R1_AlignmentLength      R1_PercentIdentity      R1_BitScore   R1_TranscriptSize       R1_RelativePosition     R1_Chromosome   R1_Strand       R1_ChrStartCoords       R1_ChrEndCoords R2_ID   R2_HitType      R2_GeneID       R2_GeneName  R2_AlignmentLength       R2_PercentIdentity      R2_BitScore     R2_TranscriptSize       R2_RelativePosition     R2_Chromosome   R2_Strand       R2_ChrStartCoords       R2_ChrEndCoords
000PBLIV_6_396_1546_1226        110     110     000PBLIV_6_396_1546_1226_R1     Top_Hit 22672   MTRNR2L9        50      100.00  99.7    1559    3.0     chrMT   +       1692    1741 000PBLIV_6_396_1546_1226_R2      Top_Hit 22672   MTRNR2L9        35      100.00  69.7    1559    7.3     chrMT   -       1767    1801

our %ftype2header		= (
	'exonBoundaries'	=> 'Boundary',
	'exonJunctions'		=> 'Junction',
	'intergenics'		=> 'Intergenic',
	'introns'		=> 'Intron',
	'transcripts'		=> 'Transcript'
);

=cut

	#my @cols = ($readid_c, $hittype_c, $featid_c, $bitscore_c, $start_c)
	#COL INDEX NAMES = (READ_ID HIT_TYPE FEATURE_ID BIT_SCORE START_POS)
	$self->writelog(BODY => "Starting with ".(keys %{$reads})." prior to filtering");

	if (($type eq 'exonBoundaries') || ($type eq 'exonJunctions')) {
		my @cols = (0, 0, 1, 2, 5, 6); # here provide a spoof index for the 'distance between reads'.  Not elegant, but functional.
		$self->filter_feature_reads(TYPE => $type, READS => $reads, FILE => $file, R1INDICES => \@cols, R2INDICES => undef, FEATURES=>$features);
	}
	elsif (($type eq 'introns')||($type eq 'intergenics')) {
		my @r1cols = (2, 1, 3, 4, 8, 10);
		my @r2cols = (12, 1, 13, 14, 18, 20);
		$self->filter_feature_reads(TYPE => $type, READS => $reads, FILE => $file, R1INDICES => \@r1cols, R2INDICES => \@r2cols, FEATURES=>$features);
	}
	elsif ($type eq 'transcripts') {
		my @r1cols = (3, 2, 4, 5, 9, 14);
		my @r2cols = (16, 2, 17, 18, 22, 27);
		$self->filter_feature_reads(TYPE => $type, READS => $reads, FILE => $file, R1INDICES => \@r1cols, R2INDICES => \@r2cols, FEATURES=>$features);
	}
	else {
		die "unrecognized feature type: $type\n";
	}
	
	# now we want to specifically choose which transcript alignment is the Top Hit.  This is necessary as the fid in the read_records
	# summary file is actually the gene id, not the transcript id, and so will not match the rs id in the sam file.
	# unfortunately, in order to do this properly, we are required to pull in the entire blast_results directory.
	if ($type eq 'transcripts') {
		$self->assign_specific_transcript_id(READS => $reads, TYPE=>$type, LANE=>$lane, FEATURES=>$features);
	}
	
	# now check how many matching reads we have:
	my ($valid, $invalid) = (0,0);
	foreach my $readid (keys %{$reads}) {
		unless (defined($reads->{$readid}->alnfeatureid())) {
			delete($reads->{$readid});
			$invalid++;
		}
		else {
			$valid++;
		}
	}
	

	$self->writelog(BODY => "Found $valid matching read IDs and $invalid non-matching read IDs");	

	$self->writelog(BODY => "---");	

	return($reads);
}


################################################################################
=pod

B<filter_feature_reads()>

Filter summary reads for only those that are in the reads hash and are TopHits (or Ambiguous)

Parameters:
 TYPE	=> feature type (exonBoundaries, intergenics, etc.)
 READS	=> ref to hash of read objects
 R1INDICES => ref to array of read 1 cols to inspect in the read summary file
 R2INDICES => ref to array of read 2 cols to inspect in the read summary file (note, not available for junctions/boundaries)

Requires:
 

Returns:
 
=cut
sub filter_feature_reads {
	my $self	= shift @_;

    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
          my $this_sub = (caller(0))[0]."::".(caller(0))[3];
          $options->{uc($_[$i])} = $_[($i + 1)];
    }

	my $type	= $options->{'TYPE'};
	my $reads	= $options->{'READS'};
	my $file    = $options->{'FILE'};
	my $r1cols  = $options->{'R1INDICES'};
	my $r2cols  = $options->{'R2INDICES'};
	my $features = $options->{'FEATURES'};
	
	$self->writelog(BODY => "--- Filtering read IDs for $type ---");

	tie *FILE, 'IO::Zlib', $file, "rb";
	while (<FILE>) {
		next unless /Top_Hit/; # should also check for ambigious here, but skip for now.
		my @info = split /\t/;

		foreach my $cols ($r1cols, $r2cols) {
			next unless defined($cols);
		
			my ($readid, $distancebetweenpair, $hittype, $featid, $bitscore, $start) = map { $info[$_] } @{$cols};

			if ($hittype eq 'Top_Hit') {
				if (defined($reads->{$readid})) {
					if ($type eq 'transcripts') {
						$reads->{$readid}->alngeneid($featid);
						$reads->{$readid}->alnbitscore($bitscore);
						$reads->{$readid}->alnstart($start);
						$reads->{$readid}->distancetopair($distancebetweenpair);
					}
					else {
						if (defined($features->{$featid})) {
							$reads->{$readid}->alnfeatureid($featid);
							$reads->{$readid}->alnbitscore($bitscore);
							$reads->{$readid}->alnstart($start);
							if (($type eq 'introns')||($type eq 'intergenics')) { # 
								$reads->{$readid}->distancetopair($distancebetweenpair);
							}
							else {
								$reads->{$readid}->distancetopair('NA'); # this information is not available for junction/bonudary mapping.
							}
						}
						else {
							delete($reads->{$readid}); # this is on a feature that is not on the reference genome for this BAM.  So remove it.
						}
					}
				}
			}
		}
	}
	close FILE;
}
	


################################################################################
=pod

B<find_alignments()>

Find all alignments for a given lane and a given feature type and list of read IDs

Parameters:
 LANE	=> lane ID
 TYPE	=> feature type (exonBoundaries, intergenics, etc.)
 READS	=> ref to array of read IDs

Requires:
 $self->{'alignment_results'}
 %feature2labels

Returns:
 ref to array of SAM file lines

=cut
sub find_bfast_alignments {
	my $self	= shift @_;

    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }

	$self->writelog(BODY => "--- Finding alignments ---");

	my $type	= $options->{'TYPE'};
	my $lane	= $options->{'LANE'};
	my $reads	= $options->{'READS'};


	$self->writelog(BODY => "Lane: $lane\nType: $type");

	my $dir	= $self->lane_alignres_dir(LANE => $lane, TYPE => $type);
	$self->writelog(BODY => "Found dir:\n\t$dir");

	# 000PBLIV:6:396:1547:448#0/1     0       2973978 1       46      50M
	my @samlines	= ();


	# NOTE: Take advantage of the fact that the alignments are ordered by read ID.  If this is not the case
	# then the input SAM files will need to be sorted first.
	my @files = glob("$dir/blast*sam.gz");

	
	foreach my $blastfile (@files) {
		$self->writelog(BODY => "Parsing alignment file: $blastfile");
		
		my ($cid, $fid, $rid) = undef; # eg the current read id (cid), feature id (fid), and read id (rid).
		#$reads->{$rid}->alignmentlength($maxlen);
		my @tmp_samlines = ();
		my @tmp_bitscores = (); 
		my @tmp_alignlengths = ();
		
		tie *FILE, 'IO::Zlib', $blastfile, "rb" or die "Failed to read from $blastfile: $!\n";
		while (<FILE>) {
			next if /^\s*$/;
			
			if ($. % 1000000 == 0) { $self->writelog(BODY => "Inspected $. Alignments, Loaded: ".(scalar @samlines)); }
			my @alignment = split /\t/;
		
			$rid = $self->samid2readid(ID => $alignment[0]);
		
			next unless (defined($reads->{$rid}));
			$fid = $reads->{$rid}->alnfeatureid();
			next unless ($fid eq $alignment[2]); # eg this alignment is to a different feature to the one specified inthe read records file, so skip it..
		
			# all alignments that get to this stage are to the correct feature, but it's possible that there are multiple alignments to a
			# single feature, so now we need to select which alignment is the best quality.  This is a bit tedious.  
			# Ideally ALEXA-seq would keep this information,
			# but it doesn't, and unfortunately it currently cannot be reconstructed due to the data organisation and workflow
			# in the alexa-seq pipeline.  It would be possible with ALEXA-seq further modifications.  In the meantime, to
			# choose the 'best' alignment, calculate the blast bit score (as alexa does), and choose the alignment
			# with the highest score.
			if ((defined($cid))&&($cid ne $rid)) {
				# new read.   process the previous one if it exists:
				if (@tmp_samlines > 0) {
					my ($maxbs, $maxsam, $maxlen) = ($tmp_bitscores[0], $tmp_samlines[0], $tmp_alignlengths[0]);
					for (my $i = 1; $i < @tmp_samlines; $i++) {
						if ($tmp_bitscores[$i] > $maxbs) {
							($maxbs, $maxsam, $maxlen) = ($tmp_bitscores[$i], $tmp_samlines[$i], $tmp_alignlengths[$i]);
						}
					}
					push(@samlines, $maxsam);
					$reads->{$cid}->alnlength($maxlen);
					$reads->{$cid}->newbitscore($maxbs);
					@tmp_samlines = ();
					@tmp_bitscores = ();
					@tmp_alignlengths = ();
				
				}
			}
		
		
			if (/CM\:i\:(\d+)/) {
				my $mismatches = $1;
				unless (defined($mismatches)) { die "Cannot parse CM line from $_"; }
				my ($bitscore, $alignmentLength) = $self->calculate_bit_score(MISMATCHES => $mismatches, CIGAR => $alignment[5]);
				
				push(@tmp_samlines, $_);
				push(@tmp_bitscores, $bitscore);
				push(@tmp_alignlengths, $alignmentLength);
			}
			elsif (/NM\:i\:(\d+)/) {
				my $mismatches = $1;
				unless (defined($mismatches)) { die "Cannot parse NM line from $_"; }
				my ($bitscore, $alignmentLength) = $self->calculate_bit_score(MISMATCHES => $mismatches, CIGAR => $alignment[5]);
				
				push(@tmp_samlines, $_);
				push(@tmp_bitscores, $bitscore);
				push(@tmp_alignlengths, $alignmentLength);
			}
			else {
				die "Failed to parse number of mismatches from SAM line.  Neither CM:i:\\d+ nor NM:i:\\d+ has worked.\n";
			}
			
			
			$cid = $rid;
		}
		close FILE;
		
		# final read so it's needed to do this all again...
		if (@tmp_samlines > 0) {
			my ($maxbs, $maxsam, $maxlen) = ($tmp_bitscores[0], $tmp_samlines[0], $tmp_alignlengths[0]);
			for (my $i = 1; $i < @tmp_samlines; $i++) {
				if ($tmp_bitscores[$i] > $maxbs) {
					($maxbs, $maxsam, $maxlen) = ($tmp_bitscores[$i], $tmp_samlines[$i], $tmp_alignlengths[$i]);
				}
			}
			push(@samlines, $maxsam);
			$reads->{$cid}->alnlength($maxlen);
			$reads->{$cid}->newbitscore($maxbs);
		}
	}

	

	my $count	= scalar(@samlines);
	$self->writelog(BODY => "\nFound $count unique matching SAM lines");
	$self->writelog(BODY => "---");	

	return(\@samlines);
}

sub find_bwa_alignments {
	
	my $self	= shift @_;

    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }

	$self->writelog(BODY => "--- Finding BWA alignments ---");

	my $type	= $options->{'TYPE'};
	my $lane	= $options->{'LANE'};
	my $reads	= $options->{'READS'};


	$self->writelog(BODY => "Lane: $lane\nType: $type");

	my $dir	= $self->lane_alignres_dir(LANE => $lane, TYPE => $type);
	$self->writelog(BODY => "Found dir:\n\t$dir");

	my @samlines	= ();
	my @files = glob("$dir/blast*sam.gz");

	
	foreach my $blastfile (@files) {
		$self->writelog(BODY => "Parsing alignment file: $blastfile");
		
		my ($fid, $rid) = undef; # feature id (fid), and read id (rid).
	
		
		tie *FILE, 'IO::Zlib', $blastfile, "rb" or die "Failed to read from $blastfile: $!\n";
		while (<FILE>) {
			next if /^\s*$/;
			
			if ($. % 1000000 == 0) { $self->writelog(BODY => "Inspected $. Alignments, Loaded: ".(scalar @samlines)); }
			chomp;
			my @alignment = split /\t/;
		
			$rid = $self->samid2readid(ID => $alignment[0]);
		
			next unless (defined($reads->{$rid}));
			$fid = $reads->{$rid}->alnfeatureid();
			
				
			my $bestbs = 0;
			my $bestlength = undef;
			my $bestaln = undef;
			
			my $mainstrand = '+'; if ($alignment[1] & 0x10) { $mainstrand = '-'; }
			
			# for BWA alignments, we need to parse any possible multimappers from the XA tag and then check every one of these for the correct fid.
			if (defined($alignment[20])) {
				
				my @hits = (); # all alignments
				my @fidhits = (); # all alignments to this fid.
	
				# first add the actual alignment info from this line.
				my $mismatches = undef;
				if ($alignment[12] =~ /NM\:i\:(\d+)/) {
					$mismatches = $1;
				}
				else {
					die "Failed to parse NM tag: $_\n";
				}
				
				
				my $first_hit = ("$alignment[2],$mainstrand$alignment[3],$alignment[5],$mismatches");
				push (@hits, $first_hit);
				
				# next get all the multimappers.
				if ($alignment[20] =~ /XA\:Z\:(.*)/) {
			    	my $alt_align_string = $1;
			    	my @alt_hits = split(";", $alt_align_string);
			    	push(@hits, @alt_hits);
				}
				else {
					die "Failed to parse XA tag: $_\n";
				}
				my $chosenStart = 0;
				my $chosenCigar = 0;
				my $chosenStrand = 0;

			  	#Now go through each multimapper and check that it matches the fid, then if so check it's bit score and aln length 
			  	foreach my $align_string (@hits){

			    	#Basic info for each alignment
			    	#$subject_id, $strand, $subject_start, $cigar, $mismatches
			    	#e.g. "24875,+1695,51M,0"  e.g. "17471,-532,51M,0"
			    	my @vals = split(",", $align_string);
			
					next unless ($vals[0] eq $fid);
			
					# this alignment is to the correct fid, so now we need to check the alignment quality and choose the best one.
			    	my $subject_id = $vals[0];
			    	my $strand_pos = $vals[1];
			    	my $cigar = $vals[2];
			    	my $mismatches = $vals[3];
			    	my $strand;
			    	my $subject_start;
			    	if ($strand_pos =~ /([\-|\+])(\d+)/){
			      		$strand = $1;
			      		$subject_start = $2;
			    	}else{
			      		die "Alignment format not understood:\t$align_string";
			    	}

					my ($bitscore, $alignmentLength) = $self->calculate_bit_score(MISMATCHES => $mismatches, CIGAR => $cigar);
					
					if ($bitscore > $bestbs) {
						$bestbs = $bitscore;
						$bestlength = $alignmentLength;
						$bestaln = $align_string;
						$chosenStart = $subject_start;
						$chosenCigar = $cigar;
						$chosenStrand = $strand;
					}
				}
				
				# now we need to actually build the alignment string again.  Here, I'll also remove all the multimapping tags, as we have
				# chosen the alignment we want.
				$alignment[2] = $fid;
				$alignment[3] = $chosenStart;

				# now we'll need to set the reverse strand bit if the chosen alignment is on the reverse strand.
				if ($mainstrand ne $chosenStrand) {
					print "SWITCHING: $_\n";
					$alignment[9] = $self->revcomp(SEQ => $alignment[9]);
					$alignment[1] = $self->flip_flag_bit(FLAG => $alignment[1], FLIP => 5);
					$alignment[5] = $self->reverse_cigar(CIGAR => $chosenCigar);
					print "SWITCHING: ".join("\t",@alignment),"\n";
				}
				
				$bestaln = join("\t", @alignment);
			}
			else {
				next unless ($fid eq $alignment[2]);
				
				if ($alignment[12] =~ /NM\:i\:(\d+)/) {
					my ($bitscore, $alignmentLength) = $self->calculate_bit_score(MISMATCHES => $1, CIGAR => $alignment[5]);
					$bestbs = $bitscore;
					$bestlength = $alignmentLength;
					$bestaln = "$_\n";
				}
				else {
					die "Failed to parse mismatches from NM tag: $_\n";
				}
			}
		
			if ($bestbs == 0) {
				die "Failed to get good bit score: $_\n";
			}
			
			push(@samlines, $bestaln);
			$reads->{$rid}->alnlength($bestlength);
			$reads->{$rid}->newbitscore($bestbs);
		
		}
		close FILE;
	}


	my $count	= scalar(@samlines);
	$self->writelog(BODY => "\nFound $count unique matching SAM lines");
	$self->writelog(BODY => "---");	

	return(\@samlines);
}
	

################################################################################
=pod

B<assign_specific_transcript_id()>

Read the blast results files, and assign an actual transcript ID to each of the reads.
 
Parameters:
 READS: the reads to assign transcript IDs to.
 LANE: the lane ID
 TYPE: the feature type
Requires:


Returns:

=cut


sub assign_specific_transcript_id {
	my $self	= shift @_;

    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }

	my $reads = $options->{'READS'};
	my $type	= $options->{'TYPE'};
	my $lane	= $options->{'LANE'};
	my $features = $options->{'FEATURES'};
	
	$self->writelog(BODY => "Assigning transcript IDs to reads");

	my $dir		= $self->lane_blastres_dir(LANE => $lane, TYPE => $type);
	my @files = glob("$dir/blast*.gz");
	my $max_txid = 0;
	my $max_bitscore = 0;
	
	my %missingTranscripts = (); # these will be transcripts in ALEXA-seq but not in the BAM genome / GTF annotation file.
	
	my ($cid, $fid, $rid) = undef; # eg the current read id (cid), feature id (fid), and read id (rid).

	foreach my $blastfile (@files) {
		$self->writelog(BODY => "Parsing blast file: $blastfile");
		tie *FILE, 'IO::Zlib', $blastfile, "rb" or die "Failed to read from $blastfile: $!\n";
		while (<FILE>) {
			chomp;
			my @result = split /\t/;
			my $rid = $result[0];
			next unless (defined($reads->{$rid}));
			my $geneid = $reads->{$rid}->alngeneid();
			next unless $geneid; # eg this read is not assigned in this feature type.
			unless (defined($features->{$result[1]})) { $missingTranscripts{$result[1]}++; next; }
			unless (defined($txExonStarts{$result[1]})) { $self->writelog(BODY => "No exon start information for transcript: $result[1]"); next; }
			unless (defined($txExonLengths{$result[1]})) { $self->writelog(BODY => "No exon length information for transcript: $result[1]"); next; }
			
			next unless ($geneid eq $features->{$result[1]}->Gene_ID); # eg check that this parsed blast result is from a transcript in the correct gene.
			
			if ((defined($cid))&&($cid ne $rid)) {
				# new batch of reads.  process the previous one:
				$reads->{$cid}->alnfeatureid($max_txid);
				
				$max_txid = $result[1];
				$max_bitscore = $result[11];
			}
			if ($result[11] > $max_bitscore) { $max_bitscore = $result[11]; $max_txid = $result[1]; }
			$cid = $rid;
		}
		
		# AAAANNNND dont forget the last batch:
		if (defined($reads->{$cid})) {
			$reads->{$cid}->alnfeatureid($max_txid);
		}
		close FILE;
	}
	
	if (keys %missingTranscripts) > 0) {
		while (my ($txid, $count) = each %missingTranscripts) {
			$self->writelog(BODY => "No annotation information for transcript: $txid, hit with $count reads\n");
		}
	}
}


################################################################################
=pod

B<readid2samid()>

Convert read ID to SAM ID

Parameters:
 ID	=> read ID

Requires:

Returns:
 scalar SAM ID

=cut

sub readid2samid {
	my $self	= shift @_;

    #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
            my $this_sub = (caller(0))[0]."::".(caller(0))[3];
            $options->{uc($_[$i])} = $_[($i + 1)];
    }

	my $samid	= $options->{'ID'};

	# 000PBLIV_6_396_1556_1055_R1
	# 000PBLIV:6:396:1556:1055#0/1

	# this is a bit of a fark up.  The illumina reads in qalexa have the cofrrect formatting in the SAM file, 
	# while the SOLiD reads do not.  For now, I just check this before conversion, and if its ok then return the
	# original ID.
	if ($samid =~ /\#0\/[12]$/) { return $samid; }

	$samid		=~ s/\_/\:/g;
	$samid		=~ s/\:R(\d)/\#0\/$1/;

	$samid		=~ s/\n//g;

	return($samid);
}

################################################################################
=pod

B<samid2readid()>

Convert SAM ID to read ID

Parameters:
 ID	=> SAM ID

Requires:

Returns:
 scalar SAM ID

=cut
sub samid2readid {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $id	= $options->{'ID'};

	# 000PBLIV_6_396_1556_1055_R1
	# 000PBLIV:6:396:1556:1055#0/1

	# this is a bit of a fark up.  The illumina reads in qalexa have the cofrrect formatting in the SAM file, 
	# while the SOLiD reads do not.  For now, I just check this before conversion, and if its ok return the
	# original ID.
	# if the read id looks like this:000PBLIV:6:396:1547:448#0/1
	if ($id =~ /\_R[12]$/) { return $id; } 

	# now do the conversion.
	$id		=~ s/\#0\/(\d)$/\:R$1/;
	$id		=~ s/\:/\_/g;
	$id		=~ s/\:R(\d)/\#0\/$1/;
	

	return($id);
}

################################################################################
=pod

B<read_feature_file()>

Read a file containing feature definitions for a feature type

Parameters:
 TYPE	=> feature type

Requires:
 $self->{'seqdb_dir'}
 $self->{'seqdb_name'}

Returns:
 ref to hash of QCMG::AlexaSeq::Object objects

=cut
sub read_feature_file {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }


	$self->writelog(BODY => "--- Reading feature annotations ---");

	my $type	= $options->{'TYPE'};
	my $jlib    = $options->{'JLIB'}; # the size of the junction / boundary file.
	
	# build path to annotation files
	my $dir		= join '/', $self->{'seqdb_dir'}, $self->{'seqdb_name'}, $type;

	$self->writelog(BODY => "Feature type:\t$type\nPresumed path:\t$dir");
	# get annotation file for requested type; this assumes there is only one
	# file
	my $cmd	= qq{find $dir -maxdepth 1 -name "*gz"};
	if (($type eq 'exonJunctions')||($type eq 'exonBoundaries')) {
		$cmd = qq{find $dir -maxdepth 1 -name "*$jlib*gz"};
	}
	else {
		$cmd = "find $dir -maxdepth 1 -name \"".$type."_annotated.txt.gz\"";
	}
	$self->writelog(BODY => "Finding file:\t$cmd");
	my $file	= `$cmd`;
	chomp $file;

	# fail if an file is not found
	if(! -e $file && ! -r $file) {
		exit($self->EXIT_NO_ANNOTATION());
	}
	$self->writelog(BODY => "Found file:\n\t\t$file");


	# get all fields in reads files (different for each feature type)
	my $cmd			= qq{zcat $file | head -n 1};
	my $header		= `$cmd`;
	chomp $header;
	my @fields		= split /\s+/, $header;

    tie *FILE, 'IO::Zlib', $file, "rb";
	
	my %features	= ();
    while (<FILE>) {
		chomp;
		
		my @data	= split /\t/;

		# define all fields specified in header with a value from each
		# row in file
		# feature ID -> field            value
		# 4063       -> Boundary_ID      4063
		# 4063       -> Gene_ID          104
		# 4063       -> EnsEMBL_Gene_ID  ENSG00000005700
		$features{$data[0]} = QCMG::AlexaSeq::Object->new();
		for (my $f=0; $f<=$#fields; $f++) {
			my $fn	= $fields[$f];
			$features{$data[0]}->$fn($data[$f]);
		}

		# *for now* skip features that are on an LRG contig or have no 
		# chromosome name
		my $chr	= $features{$data[0]}->Chromosome();
		if ($self->{'FIX_CHR_NAMES'}) {
			if($chr =~ /^L/ || $chr !~ /\w/) {
				delete $features{$data[0]};
				next;
			}
		}

		$features{$data[0]}->Strand('+') if($features{$data[0]}->Strand() == 1);
		$features{$data[0]}->Strand('-') if($features{$data[0]}->Strand() == -1);

		# fix RS name (change this to support all RSs...)
		# convert "1" to "chr1"; do not change GLxxx names
		# convert 'M' to 'chrMT'
		# NOTE: only do this when requested, otherwise keep the chr names as found.
		if ($self->{'FIX_CHR_NAMES'}) {
			unless ($chr =~ /^G/) {
				my $rs	= 'chr'.$chr;
				$rs = 'chrMT' if($rs eq 'chrM');
				$features{$data[0]}->Chromosome($rs);
			}
		}
		
		if ($. % 10000 == 0) { $self->writelog(BODY => "Loaded $. features"); }
	}
	close FILE;


	$self->writelog(BODY => "Loaded ".(keys %features)." $type (features) in total");
	$self->writelog(BODY => "---");	

	return(\%features);
}

################################################################################
=pod

B<build_transcript_objects()>

Read the GFF file and build transcript objects.  Add these objects to each of the features alread created in teh $features hash.

Parameters:
 GFF	=> Transcript gene model GFF file.
 FEATURES => set of transcript features (from the ALEXA-seq features file)

Requires:

Returns:
 ref to hash of QCMG::AlexaSeq::Object of transcript features.

=cut

sub build_transcript_objects {
	my $self	= shift @_;

    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }

	$self->writelog(BODY => "--- Reading transcript annotations ---");
	#print "Loading exon information\n";

	my $features	= $options->{'FEATURES'};
	my $gfffile	= $options->{'GFF'};
	
	my %transcripts = ();

	# build a hash of the ensembl IDs (used in the GFF file), to the ALEXA-seq IDs (used in the $features object)
	my %ensembl2alexaIDs = ();
	foreach my $feature (keys %{$features}) {
		$ensembl2alexaIDs{$features->{$feature}->EnsEMBL_Trans_ID} = $features->{$feature}->Trans_ID;
	}
	
	# now add required information from the GFF file to the $features object.
	tie *FILE, 'IO::Zlib', $gfffile, "rb";
	
    while (<FILE>) {
		next unless /\texon\t/;
		chomp;
		my ($chr, $bt, $ft, $start, $end, $blah, $strand, $blah, $infoStr) = split /\t/;
		my $txid = undef;
		if ($infoStr =~ /transcript_id\s\"(.*?)\";\s+/) { $txid = $1; }
		
		push(@{$txExonStarts{$ensembl2alexaIDs{$txid}}},$start);
		push(@{$txExonLengths{$ensembl2alexaIDs{$txid}}}, ($end - $start + 1));
	}
	close FILE;
	
	# Do some organisation of the transcript data so that it is easier to use:
	foreach my $txid (keys %{$features}) {
	
		# reverse the exon information for antisense transcripts and calculate the transcript length 
		# to make downstream processing of CIGARs the same for forward and reverse strand transcripts.
		if ($features->{$txid}->Strand eq '-') {
			
			if (defined($txExonStarts{$txid})) { #otherwise where is this information??
				my @reverseExonStarts = reverse @{$txExonStarts{$txid}};
				my @reverseExonLengths = reverse @{$txExonLengths{$txid}};
				my $totalTranscriptLength = 0;
				foreach my $exonLength (@reverseExonLengths) {
					$totalTranscriptLength += $exonLength;
				}
				
				# for some reason, alexa-seq has decided to put 'NA' in the transcript Base_Count field.
				# So I'll tediously fill it in myself.
				$features->{$txid}->Base_Count($totalTranscriptLength);
				$txExonStarts{$txid} = \@reverseExonStarts;
				$txExonLengths{$txid} = \@reverseExonLengths;
			}
		}
	
		# record the transcript id(s) for this gene id.
		push(@{$gene2transcriptIDs{$features->{$txid}->Gene_ID}}, $txid);
	}
	
}



################################################################################
=pod

B<feature_aln_to_genomic_aln()>

Transform a SAM feature alignment line to a genomic alignment line.

Parameters:
 ALIGNMENTS	=> ref to array of SAM lines
 TYPE		=> feature type
 FEATURES	=> ref to hash of QCMG::AlexaSeq::Object objects
 READS		=> ref to hash of read objects

Requires:
 $self->{'juncdiff1'}
 $self->{'juncdiff2'}

Returns:
 ref to array of SAM lines (with fixed cigar)

=cut

sub feature_aln_to_genomic_aln {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
    for (my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }
	
	
	# different actions will be required depending on the feature type:
	$self->writelog(BODY => "--- Start Feature Alignment to Genomic Alignment ---");

	my $fixed_alignments = undef;

	my $alignments	= $options->{'ALIGNMENTS'};
	my $type	= $options->{'TYPE'};
	my $features	= $options->{'FEATURES'};
	my $reads	= $options->{'READS'};
	
	# all intergenic features are relative to the reference strand.  Only start pos transformation required
	if ($type eq 'intergenics') {
		$fixed_alignments = $self->fix_intergenic_alignments(ALIGNMENTS => $alignments,  TYPE => $type, FEATURES => $features, READS => $reads);
	}
	elsif (($type eq 'introns')||($type eq 'exonBoundaries')) {
		$fixed_alignments = $self->fix_nonspliced_stranded_alignments(ALIGNMENTS => $alignments,  TYPE => $type, FEATURES => $features, READS => $reads);
	}
	elsif ($type eq 'exonJunctions') {
		$fixed_alignments = $self->fix_junction_alignments(ALIGNMENTS => $alignments,  TYPE => $type, FEATURES => $features, READS => $reads);
	}
	elsif ($type eq 'transcripts') {
		$fixed_alignments = $self->fix_transcript_alignments(ALIGNMENTS => $alignments,  TYPE => $type, FEATURES => $features, READS => $reads);
	}
	else {
		die "feature type: [$type] not supported\n";
	}
	
	return $fixed_alignments;
}

################################################################################
=pod

B<fix_intergenic_alignments()>

Transform a SAM feature alignment line to a genomic alignment line.

Parameters:
 ALIGNMENTS	=> ref to array of SAM lines
 TYPE		=> feature type
 FEATURES	=> ref to hash of QCMG::AlexaSeq::Object objects
 READS		=> ref to hash of read objects

Requires:
 $self->{'juncdiff1'}
 $self->{'juncdiff2'}

Returns:
 ref to array of SAM lines (with fixed cigar)

=cut

sub fix_intergenic_alignments {
	my $self	= shift @_;

    #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }
	
	my $alignments	= $options->{'ALIGNMENTS'};
	my $features	= $options->{'FEATURES'};
	my $reads	= $options->{'READS'};
	my $type = $options->{'TYPE'};
	
	
	# different actions will be required depending on the feature type:
	$self->writelog(BODY => "--- Start Intergenic Alignment to Genomic Alignment ---");
	
	
	my @fixed_alignments	= ();
	
	foreach my $al (@{$alignments}) {
		my @data	= split /\s+/, $al;
		
		my $readid	= $self->samid2readid(ID => $data[0]);
		
		if (! $reads->{$readid}) {
			print STDERR "Can't find $readid in reads\n";
			next;
		}

		my $fid		= $reads->{$readid}->alnfeatureid;
		
		
		# TEMPORARY FIX!!! this shouldn't happen...
		if(! $features->{$fid}) {
			print STDERR "Can't find $fid in features\n";
			next;
		}

		my $newstart = $data[3] + $features->{$fid}->Unit1_start_chr();
		my $chr		= $features->{$fid}->Chromosome();
		
		$reads->{$readid}->alnchrstart($newstart);
		$reads->{$readid}->alnchr($chr);
		
		$data[4] = int($reads->{$readid}->alnbitscore()); # set the mapping quality to be the bit score.  
		$data[3]	= $newstart;
		$data[2]	= $chr;

		push @fixed_alignments, join "\t", @data;
	}

	$self->writelog(BODY => "--- Transformed ".(scalar @fixed_alignments)." $type alignments to genomic alignments ---");


	$self->writelog(BODY => "---");	

	return(\@fixed_alignments);
}


################################################################################
=pod

B<fix_nonspliced_stranded_alignments()>

Transform a nonspliced, stranded SAM feature alignment line to a genomic alignment line. This works for introns and boundaries.

Parameters:
 ALIGNMENTS	=> ref to array of SAM lines
 TYPE		=> feature type
 FEATURES	=> ref to hash of QCMG::AlexaSeq::Object objects
 READS		=> ref to hash of read objects

Requires:
 $self->{'juncdiff1'}
 $self->{'juncdiff2'}

Returns:
 ref to array of SAM lines (with fixed cigar)

=cut
sub fix_nonspliced_stranded_alignments {
	my $self	= shift @_;

    #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }
	
	my $alignments	= $options->{'ALIGNMENTS'};
	my $features	= $options->{'FEATURES'};
	my $reads	= $options->{'READS'};
	my $type = $options->{'TYPE'};
	
	
	# different actions will be required depending on the feature type:
	$self->writelog(BODY => "--- Start Stranded Nonspliced Alignment to Genomic Alignment ---");
	
	
	my @fixed_alignments = ();
	
	foreach my $al (@{$alignments}) {
		my @data	= split /\s+/, $al;
		my $readid	= $self->samid2readid(ID => $data[0]);
		my $juncdiff = $self->{'juncdiff1'};
		if ($readid =~ /R2$/) { 
			$juncdiff = $self->{'juncdiff2'}; 
			if ($type eq 'exonBoundaries') {
				next;# for now, skip R2 boundary alignments because they, out of all categories, seem to be munted.
			}
		}
		
		# TEMPORARY FIX!!! this shouldn't happen...
		#if(! $reads->{$readid}) {
		if (! $reads->{$readid}) {
			print STDERR "Can't find $readid in reads\n";
			next;
		}

		my $fid		= $reads->{$readid}->alnfeatureid;
		
		
		# TEMPORARY FIX!!! this shouldn't happen...
		if(! $features->{$fid}) {
			print STDERR "Can't find $fid in features\n";
			next;
		}

		my $flags	= $self->read_flag_bits(FLAG => $data[1]);
		my $is_reversed	= $self->is_reversed(FLAG => $data[1]);
	
		# get read sequence; may need to reverse complement it
		my $rseq	= $data[9];

		my $featstrand = $features->{$fid}->Strand;
		
		# we'll need to revcomp the alignment, adjust the start position, and flip the flag if this
		# feature is on the reverse strand:
		my ($newstart, $newseq, $newflag, $newcigar) = (undef, undef, undef, undef);
		if ($featstrand eq '-') {
				
			# grab the cigar, and calculate the alignment length.  This is needed for negative feature alignemnts
			my $cigar	= $data[5];
			my $alignmentLength = $self->calculate_alignment_length(CIGAR => $cigar);
			
			$newstart = $features->{$fid}->Unit1_end_chr - $data[3] - $alignmentLength + 1;
			if ($type eq 'exonBoundaries') { $newstart -= $juncdiff; }
			
			$newseq = $self->revcomp(SEQ => $rseq);
			$newflag = $self->flip_flag_bit(FLAG => $data[1], FLIP => 5);
			$newcigar = $self->reverse_cigar(CIGAR => $cigar);
		}
		elsif ($featstrand eq '+') {
			$newstart = $features->{$fid}->Unit1_start_chr + $data[3];
			if ($type eq 'exonBoundaries') { $newstart += $juncdiff; }
			
			$newseq = $rseq;
			$newflag = $data[1];
			$newcigar = $data[5];
		}
		else {
			die "Cannot identify feature strand: $featstrand\n";
		}

		
		my $chr		= $features->{$fid}->Chromosome();
		
		$reads->{$readid}->alnchrstart($newstart);
		$reads->{$readid}->alnchr($chr);
		
		$data[1] = $newflag;
		$data[2] = $chr;
		$data[3] = $newstart;
		$data[4] = int($reads->{$readid}->alnbitscore()); # set the mapping quality to be the bit score.  
		$data[5] = $newcigar;
		$data[9] = $newseq;
	
		push @fixed_alignments, join "\t", @data;
	}

	#print Dumper @fixed_alignments;
	
	$self->writelog(BODY => "--- Transformed ".(scalar @fixed_alignments)." $type alignments to genomic alignments ---");

	$self->writelog(BODY => "---");	

	return(\@fixed_alignments);
}



################################################################################
=pod

B<fix_junction_alignments()>

Transform junction alignments to genomic alignments.

Parameters:
 ALIGNMENTS	=> ref to array of SAM lines
 TYPE		=> feature type
 FEATURES	=> ref to hash of QCMG::AlexaSeq::Object objects
 READS		=> ref to hash of read objects

Requires:
 $self->{'juncdiff1'}
 $self->{'juncdiff2'}

Returns:
 ref to array of SAM lines (with fixed cigar)

=cut
sub fix_junction_alignments {
	my $self	= shift @_;

    #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }
	
	my $alignments	= $options->{'ALIGNMENTS'};
	my $features	= $options->{'FEATURES'};
	my $reads	= $options->{'READS'};
	my $type = $options->{'TYPE'};
	
	
	# different actions will be required depending on the feature type:
	$self->writelog(BODY => "--- Start Junction Alignment to Genomic Alignment ---");
	
	
	my @fixed_alignments = ();
	
	foreach my $al (@{$alignments}) {
		my @data	= split /\s+/, $al;
		my $readid	= $self->samid2readid(ID => $data[0]);
		my $juncdiff = $self->{'juncdiff1'};
		if ($readid =~ /R2$/) { $juncdiff = $self->{'juncdiff2'}; }
		
		# TEMPORARY FIX!!! this shouldn't happen...
		if (! $reads->{$readid}) {
			print STDERR "Can't find $readid in reads\n";
			die;
		}

		my $fid		= $reads->{$readid}->alnfeatureid;
		
		# TEMPORARY FIX!!! this shouldn't happen...
		if(! $features->{$fid}) {
			print STDERR "Can't find $fid in features\n";
			die;
		}

		my $flags = $self->read_flag_bits(FLAG => $data[1]);
		my $is_reversed	= $self->is_reversed(FLAG => $data[1]);
		my $rseq = $data[9];
		my $featstrand = $features->{$fid}->Strand;
		my $intronLength = $features->{$fid}->Unit2_start_chr - $features->{$fid}->Unit1_end_chr + 1;
		
		# we'll need to revcomp the alignment, adjust the start position, and flip the flag if this
		# feature is on the reverse strand:
		my ($newstart, $newseq, $newflag, $newcigar) = (undef, $rseq, $data[1], $data[5]);
		my $alignmentLength = undef;
		if ($featstrand eq '-') {
			$newcigar = $self->reverse_cigar(CIGAR => $data[5]);
			
			# next get the alignment length before the intron splicing, this is
			# so we can work out the actual start position for negative stranded features.
			$alignmentLength = $self->calculate_alignment_length(CIGAR => $newcigar);
			$newstart = $features->{$fid}->Unit2_end_chr - $juncdiff - $data[3] - $alignmentLength - $intronLength + 3;
			$newseq = $self->revcomp(SEQ => $rseq);
			$newflag = $self->flip_flag_bit(FLAG => $data[1], FLIP => 5); # the read is on the 'reverse' strand flag.
		}
		elsif ($featstrand eq '+') {
			# otherwise the feature is orientated in the reference sense strand, so just change the start position.
			$newstart = $features->{$fid}->Unit1_start_chr + $juncdiff + $data[3];
			$alignmentLength = $self->calculate_alignment_length(CIGAR => $newcigar);
		}
		else {
			die "Cannot identify feature strand: $featstrand\n";
		}
		
		
		# now add the intron into the cigar:
		my @intronStarts = ($features->{$fid}->Unit1_end_chr);
		my @intronLengths = ($intronLength);
		my $splicedCigar = $self->add_cigar_splicing(READID => $readid, ALIGN_START => $newstart, ALIGN_LENGTH => $alignmentLength, CIGAR => $newcigar, INTRON_STARTS => \@intronStarts, INTRON_LENGTHS => \@intronLengths);
		
		
		my $chr		= $features->{$fid}->Chromosome();
		
		$reads->{$readid}->alnchrstart($newstart);
		$reads->{$readid}->alnchr($chr);
		
		$data[1] = $newflag;
		$data[2] = $chr;
		$data[3] = $newstart;
		$data[4] = int($reads->{$readid}->alnbitscore()); # set the mapping quality to be the bit score.  
		$data[5] = $splicedCigar;
		$data[9] = $newseq;
	
		push @fixed_alignments, join "\t", @data;
	}

	$self->writelog(BODY => "--- Transformed ".(scalar @fixed_alignments)." $type alignments to genomic alignments ---");

	$self->writelog(BODY => "---");	

	return(\@fixed_alignments);
}


################################################################################
=pod

B<fix_transcript_alignments()>

Transform transcript alignments to genomic alignments.

Parameters:
 ALIGNMENTS	=> ref to array of SAM lines
 TYPE		=> feature type
 FEATURES	=> ref to hash of QCMG::AlexaSeq::Object objects
 READS		=> ref to hash of read objects

Requires:

Returns:
 ref to array of SAM lines (with fixed cigar, flag, start coords, chromosome).

=cut

sub fix_transcript_alignments {
	my $self	= shift @_;

    #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }
	
	my $alignments	= $options->{'ALIGNMENTS'};
	my $features	= $options->{'FEATURES'};
	my $reads	= $options->{'READS'};
	my $type = $options->{'TYPE'};
	
	
	# different actions will be required depending on the feature type:
	$self->writelog(BODY => "--- Start Transcript Alignment to Genomic Alignment ---");
	
	
	my @fixed_alignments = ();
	#print "transforming ".(scalar @{$alignments})." alignments\n";
	foreach my $al (@{$alignments}) {
		
		my @data	= split /\s+/, $al;	
		my $readid	= $self->samid2readid(ID => $data[0]);
		
		#if ($. % 100 == 0) { 
		#	$self->writelog(BODY => "$readid: NEWBITSCORE: ".
		#	$reads->{$readid}->newbitscore."\t OLDBITSCORE: ".
		#	$reads->{$readid}->alnbitscore); 
		#}
	
		# TEMPORARY FIX!!! this shouldn't happen...
		if (! $reads->{$readid}) {
			print STDERR "Can't find $readid in reads\n";
			die;
		}

		my $fid	= $reads->{$readid}->alnfeatureid;
		
		# TEMPORARY FIX!!! this shouldn't happen...
		if(! $features->{$fid}) {
			print STDERR "Can't find $fid in features\n";
			die;
		}

		my $flags = $self->read_flag_bits(FLAG => $data[1]);
		my $is_reversed	= $self->is_reversed(FLAG => $data[1]);
		my $rseq = $data[9];
		my $featstrand = $features->{$fid}->Strand;
		
		
		# we'll need to revcomp the alignment, adjust the start position, and flip the flag if this
		# feature is on the reverse strand:
		my ($newstart, $newseq, $newflag, $newcigar) = (undef, $rseq, $data[1], $data[5]);
		my $alignmentLength = undef;
		my $intronStartsRef = undef;
		my $intronLengthsRef = undef;
		my $alignmentLength = $self->calculate_alignment_length(CIGAR => $newcigar);
		my $transcriptStart = $data[3];

		if ($featstrand eq '-') {
			$newcigar = $self->reverse_cigar(CIGAR => $data[5]);
			
			# we just want to walk backwards along the transcript, so inverse all the coords, eg, make everything relative to
			# the end of the transcript, rather than the start.
			$transcriptStart = $features->{$fid}->Base_Count - ($data[3] + $alignmentLength) + 2;
			$newseq = $self->revcomp(SEQ => $rseq);
			$newflag = $self->flip_flag_bit(FLAG => $data[1], FLIP => 5); # the read is on the 'reverse' strand flag.
		}

		# calculate the genomic alignment start, as well as the coords and lengths of any introns this alignment spans.
		($newstart, $intronStartsRef, $intronLengthsRef) = 
		$self->calc_trans_align_splicing(READID => $readid, 
			ALIGN_START => $transcriptStart, 
			ALIGN_LENGTH => $alignmentLength,
			TX_ID => $fid,
			FEATURES => $features);		
		
		# now add the intron into the cigar:
		my $splicedCigar = $self->add_cigar_splicing(READID => $readid, 
			ALIGN_START => $newstart, 
			ALIGN_LENGTH => $alignmentLength, 
			CIGAR => $newcigar, 
			INTRON_STARTS => $intronStartsRef, 
			INTRON_LENGTHS => $intronLengthsRef);
		
		my $chr	= $features->{$fid}->Chromosome();
		$reads->{$readid}->alnchrstart($newstart);
		$reads->{$readid}->alnchr($chr);
		
		$data[1] = $newflag;
		$data[2] = $chr;
		$data[3] = $newstart;
		$data[4] = int($reads->{$readid}->alnbitscore()); # set the mapping quality to be the bit score.  
		$data[5] = $splicedCigar;
		$data[9] = $newseq;
	
		push @fixed_alignments, join "\t", @data;
	}

	$self->writelog(BODY => "--- Transformed ".(scalar @fixed_alignments)." $type alignments to genomic alignments ---");

	$self->writelog(BODY => "---");	

	return(\@fixed_alignments);
}


################################################################################
=pod

B<cal_trans_align_splicing()>

Determine the followig parameters about a transcript alignment:
1) It's genomic start position
2) The genomic coord(s) of any introns it includes
3) The length of any introns it includes.
Return this info back to the caller, so that the cigar string can then be calculated again.


Parameters:
	ALIGN_START => the start position of the alignment relative to the start of the transcript (starts at coord 1)
	ALIGN_LENGTH => the total number of reference bases covered by this alignment (excluding introns).
	TX_ID => the transcript alexaseq ID.
	FEATURES => the transcript features.
	
Requires:
 
Returns:
 Integer: the actual genomic start coord
 scalar (ref) to array of intron start coords
 scalar (ref) to array of intorn lengths

=cut
sub calc_trans_align_splicing {
	
	my $self	= shift @_;

    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $alignmentStart = $options->{'ALIGN_START'}; 
	my $alignmentLength = $options->{'ALIGN_LENGTH'};
	my $txid = $options->{'TX_ID'};
	my $features = $options->{'FEATURES'};
	my $readid = $options->{'READID'};
	
	

	
	my @exonStarts = @{$txExonStarts{$txid}};
	my @exonLengths = @{$txExonLengths{$txid}};
	
	#$self->writelog(BODY => "\n[$readid]\tSPLICE BEGIN\t$alignmentStart\t$alignmentLength\t$txid\t[".join(",",@exonStarts)."]\t[".join(",",@exonLengths)."]");
	
	# variables to return to caller.  
	my $genomicAlignStart = undef;
	my @intronStarts = ();
	my @intronLengths = ();

	# calculate alignment start position:
	my $runningTxPos = 0;
	my $alignExonIndex = 0;
	my $distanceFromPrevious3PrimeSpliceSite = 0;
	for (my $i = 0; $i < @exonLengths; $i++) {
		
		#$self->writelog(BODY => "[$readid]\t\tEXON[$i] RUNNING POS: $runningTxPos");
		
		$runningTxPos += $exonLengths[$i];
		if ($runningTxPos >= $alignmentStart) {

			$distanceFromPrevious3PrimeSpliceSite = $exonLengths[$i] - ($runningTxPos - $alignmentStart);
			$genomicAlignStart = $exonStarts[$i] + $distanceFromPrevious3PrimeSpliceSite - 1;
			$alignExonIndex = $i; # the index of the exon this alignment starts in, to be used below.
			#$self->writelog(BODY => "[$readid]\t\tFOUND CONTAINING EXON: 3'SPLICE DISTANCE: $distanceFromPrevious3PrimeSpliceSite, GENOMIC:$genomicAlignStart");
			last;
		}
		#else {
		#	$self->writelog(BODY => "[$readid]\t\tSKIPPING EXON");
		#}
	}

	#$self->writelog(BODY => "[$readid]\tFINDING INTRON INFO");
	# calculate the intron coords and lengths this alignment spans, if any.
	my $remainingAlignmentLength = $alignmentLength;
	my $count = 0;
	for (my $i = $alignExonIndex; $i < @exonStarts; $i++) {
		my $distanceToNextIntron5PrimeSpliceSite = $exonLengths[$i] - $distanceFromPrevious3PrimeSpliceSite;
		#$self->writelog(BODY => "[$readid]\t\tCHECK SPLICE SITE SPAN: REM_ALIGN_LENGTH: $remainingAlignmentLength, DISTANCE: $distanceToNextIntron5PrimeSpliceSite");
		if (($remainingAlignmentLength <= $distanceToNextIntron5PrimeSpliceSite)||($i == @exonStarts-1)) {
			#$self->writelog(BODY => "[$readid]\t\tNOT SPLICED");
			last; # the remainder of this alignment falls completely within this exon.
		}
		
		$remainingAlignmentLength -= $distanceToNextIntron5PrimeSpliceSite;
		$distanceFromPrevious3PrimeSpliceSite = 0;
		
		if ($i == @intronStarts - 1) { die "Transcript Alignment Calculation Error"; } # this should never happen....
		push(@intronStarts, $exonStarts[$i] + $exonLengths[$i] - 1 + $count);
		push(@intronLengths, $exonStarts[$i+1] - ($exonStarts[$i] + $exonLengths[$i]) + 2);
		$count++;
		#$self->writelog(BODY => "[$readid]\t\tSPLICED [START: ".($exonStarts[$i] + $exonLengths[$i] - 1)."] LENGTH: ".($exonStarts[$i+1] - ($exonStarts[$i] + $exonLengths[$i]) + 2)."]");
	}
	#$self->writelog(BODY => "[$readid]\t\t$txid\t$genomicAlignStart\t[".join(",",@intronStarts)."]\t[".join(",",@intronLengths)."]");	
	
	return ($genomicAlignStart, \@intronStarts, \@intronLengths);	
}

################################################################################
=pod

B<add_cigar_splicing()>

Insert one or more introns into a cigar string.  CIGARs are always left orientated.  
Any number of introns can be specified in the INTRON_STARTS and INTRON_LENGTHS arrays.
Introns are represented by the elements with the same indexes ine ach of the these arrays.

Parameters:
	ALIGN_START => the left based start position for this alignment.
	ALIGN_LENGTH => the total number of reference bases covered by this alignment (excluding introns).
	CIGAR => the original cigar string
	INTRON_STARTS => array of integers representing the chromsome intron start position
	INTRON_LENGTHS => array of integers representing the intron lengths
	
Requires:
 
Returns:
 new CIGAR string with intron(s) spliced in.

=cut
sub add_cigar_splicing {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }
		
	

	my $alignmentStart = $options->{'ALIGN_START'};
	my $alignmentLength = $options->{'ALIGN_LENGTH'};
	my $cigar = $options->{'CIGAR'};
	my @intronStarts = @{$options->{'INTRON_STARTS'}};
	my @intronLengths = @{$options->{'INTRON_LENGTHS'}};
	my $readid = $options->{'READID'};
	
	my @frags	= ($cigar =~ /(\d+[A-Z])/g);
	my @template	= ();
	my $currentGenomicPosition = $alignmentStart;
	my $newcigar = "";
	my @tmpis = @intronStarts;
	my @tmpil = @intronLengths;
	
	#$self->writelog(BODY => "\n[$readid]\tCIGAR BEGIN\t$cigar\t$alignmentStart\t$alignmentLength\t[".join(",",@intronStarts)."]\t[".join(",",@intronLengths)."]");
	
	for (my $i = 0; $i < @frags; $i++) {
		my $frag = $frags[$i];
		$frag =~ /(\d+)([A-Z])/;
		my $len	= $1;
		my $typ	= $2;

		#$self->writelog(BODY => "[$readid]\tCIGAR FRAG\t$frag\t$len\t$typ");

		# check if this CIGAR part extends into an intron, and if so calculate by how much, add this to the cigar
		# then calculate the overhang, add this back onto the @frags array, and increment array data.
		if ((@intronStarts >= 1)&&($currentGenomicPosition + $len > $intronStarts[0])) {
			
			
			my $overhangAmount = $currentGenomicPosition + $len - $intronStarts[0] - 1;
			my $leftAmount = $len - $overhangAmount;
			
			# check if the overhang is actually greater than zero, and there are more fragments, otherwise we need to remove the overhang CIGAR fragment (it's a 0M, 0I or 0D)
			if (($overhangAmount > 0)||($i < @frags - 1)) {
				$newcigar .= $leftAmount.$typ.($intronLengths[0] - 2).'N';
				$currentGenomicPosition = $intronStarts[0] + $intronLengths[0];

				#$self->writelog(BODY => "[$readid]\tCIGAR           INTRONIC [$leftAmount/$overhangAmount] now at $currentGenomicPosition, cigar=$newcigar");
			
				shift @intronStarts; 
				shift @intronLengths;
			  
				if ($overhangAmount > 0) {
					# We need to add this CIGAR subpart back onto the list for further testing as we don't yet know where the next intron is.
					$frags[$i] = $overhangAmount.$typ;
					$i--; # This would be better implemented recursively
				}
				next;
			}
			#else { # this frag ends bang on the 5' end of a splice site, so do not add the intron cigar fragment.
			#	$self->writelog(BODY => "[$readid]\tCIGAR           ENDS AT SPLICE SITE.");			
			#}
		}
		
		
		$newcigar .= $frag;
		if ($typ ne 'I') { # insertions in the query sequence are not relevant to reference alignments.
			$currentGenomicPosition += $len;
		}
		#$self->writelog(BODY => "[$readid]\tCIGAR           EXONIC, now at $currentGenomicPosition, cigar=$newcigar");
	}
	
	#$self->writelog(BODY => "[$readid]\tCIGAR RESULT: $newcigar\n");
	
	return $newcigar;

}	


################################################################################
=pod

B<calculate_bit_score()>

Calculate a blast like bit score for an alignment.  

Parameters:
	CIGAR => The cigar string
	MISMATCHES => The number of mismatches
	
Requires:

Returns:
 Double: the bit score.


=cut


sub calculate_bit_score {
	my $self	= shift @_;

    #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }

	#$self->writelog(BODY => "--- Fixing Calculating  ---");

	my $cigar	= $options->{'CIGAR'};
	my $mismatches	= $options->{'MISMATCHES'};
	my $aligner = $options->{'ALIGNER'};
	
	my $alignmentLength = $self->calculate_alignment_length(CIGAR => $cigar);
	
	my @frags	= ($cigar =~ /(\d+[A-Z])/g);
	
	my $deletion_count = 0;
    my $deletion_bases = 0;
    my @deletion_sizes = ();
    my $insertion_count = 0;
    my $insertion_bases = 0;
    my @insertion_sizes = ();

	for (my $i = 0; $i < @frags; $i++) {
		my $frag = $frags[$i];
		$frag =~ /(\d+)([A-Z])/;
		my $len	= $1;
		my $typ	= $2;
	
		if ($typ eq 'M') {
			#$alignmentLength += $len;
		}
		elsif ($typ eq 'D') {
			#$alignmentLength += $len;
			push(@deletion_sizes, $len);
			$deletion_count++;
			$deletion_bases += $len;
		}
		elsif ($typ eq 'I') {
			push(@insertion_sizes, $len);
			$insertion_count++;
			$insertion_bases += $len;
		}
		else {
			#$self->writelog("Unknown CIGAR code: $typ [length: $len]");
			#die;
		}
	}
	
	# adjust the mistmatches if the aligner is bwa:
	if ($aligner eq 'bwa') {
		my $tmp_mismatches = $mismatches - ($deletion_count + $insertion_count);
		if ($tmp_mismatches < 0) { $tmp_mismatches = 0; }
		$mismatches = $tmp_mismatches;
	}
	
	# this code stolen from ALEXA-seq ($ALEXAROOT/utilities/filterBfastStream.pl)
	my $del_penalty = 0;
    my $ins_penalty = 0;
    my $adjust = 0.3;
    foreach my $del_size (@deletion_sizes){$del_penalty += (13.9+(($del_size-1)*4.0));}
    foreach my $ins_size (@insertion_sizes){$ins_penalty += (15.9+(($ins_size-1)*6.0));}
    my $starting_score = ($alignmentLength * 2);
    my $mismatch_penalty = ($mismatches*8);
    my $bit_score = $starting_score - $mismatch_penalty - $del_penalty - $ins_penalty - $adjust;
    
	return ($bit_score, $alignmentLength);
}



################################################################################
=pod

B<fix_flags()>

Set the flags according to what information is available.  The following masks will be used:

First set PCR dup, read failure, not prim alignment to all be false
$FLAG AND 00011111111 0x255

Now set every read to be mapped.
$FLAG XOR 00000000100 0x4

For R1 reads:
	Set every read to be first in pair and read paired
	$FLAG OR 00001000001 0x65

For R2 reads:
	Set every read to be second in pair and read paired
	$FLAG OR 00010000001 0x129
	
Next determine if read is properly paired.  This comes from the 'feature alignment distance' in the read records details file.
if (alignment distace <= max allowable distance) 
	$FLAG OR 00000000010 0x2
	$FLAG AND 11111110111 2039
else 
	$FLAG XOR 00000001010 0x10

Parameters:
	READS => reference to the reads
	ALIGNMENTS => reference to the alignments
	
Requires:

Returns:
 reference to the alignments. 




sub fix_flags {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "--- Fixing flag field strings for alignments ---");

	my $alignments	= $options->{'ALIGNMENTS'};
	my $reads	= $options->{'READS'};
	
	my $numAlignments = scalar (@{$alignments});
	
	for (my $i = 0; $i < $numAlignments; $i++) {
		
		my $al = ${$alignments}[$i];
		my @data	= split /\s+/, $al;	
		my $readid	= $self->samid2readid(ID => $data[0]);
		my $templateid = undef;
		my $readnum = undef;
		my $readpairid = undef;
		
		if ($readid =~ /(.*)\_R([12])$/) {
			$templateid = $1;
			$readnum = $2;
		}
		else {
			die "Failed to parse template id from read id:[$readid]\n";
		}
		
		
		my $flags = $data[1];
		
		$flags = $flags & 0x255;
		$flags = $flags ^ 0x4;
		
		if ($readnum == 1) {
			$flags = $flags | 0x65;
			$readpairid = $templateid."_R2";
		}
		else {
			$flags = $flags | 0x129;
			$readpairid = $templateid."_R1";
		}
		
		if (defined($reads->{$readpairid})) {
			$flags = $flags & 0x2039;
			if ($reads->{$readid}->distanceBetweenFragments < $allowableDistance) {
				$flags = $flags | 0x2;
			}
			else {
				$flags = $flags ^ 0x2;
			}
		}
		else {
			$flags = $flags ^ 0x8;
		}
		$data[1] = $flags;
		${$alignments}[$i] = join("\t", @data);
	}
	
	return $alignments;
}
=cut

################################################################################
=pod

B<make_flags()>

Set the flags according to what information is available.  It's probably just easier to make the flag from scratch here.

Parameters:
	READS => reference to the reads
	ALIGNMENTS => reference to the alignments
	
Requires:

Returns:
 reference to the alignments. 

=cut


sub make_flags {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "--- Fixing flag field strings for alignments ---");

	my $alignments_ref	= $options->{'ALIGNMENTS'};
	my $maxdistance = $options->{'MAX_BELIEVABLE_FRAG_SIZE'};
	my $type	= $options->{'TYPE'};
	my $reads	= $options->{'READS'};
	
	my @alignments = @{$alignments_ref};
	
	my $numAlignments = scalar (@alignments);
	
	for (my $i = 0; $i < $numAlignments; $i++) {
		
		my $al = $alignments[$i];
		my @data	= split /\s+/, $al;	
		my $readid	= $self->samid2readid(ID => $data[0]);
		my $templateid = undef;
		my $readnum = undef;
		my $readpairid = undef;
		my $readpairsamid = $data[0];
		
		if ($readid =~ /(.*)\_R([12])$/) {
			$templateid = $1;
			$readnum = $2;
		}
		else {
			die "Failed to parse template id from read id:[$readid]\n";
		}
		
		my $isreversed = 0;
		if ($data[1] & 0x10) {
			$isreversed = 1;
		}
		
		my @flags;
		
		# for frag runs we just need to set the is_reversed flag.
		if ($self->{'is_frag_run'}) {
			@flags = (0,0,0,0,0,0,$isreversed,0,0,0,0);
		}
		
		# for paired runs we'll set the mate information as well (as much as possible here anyway).
		else {
			@flags = (0,0,0,0,0,0,$isreversed,0,0,0,1);
		
			if ($readnum == 1) {
				$flags[4] = 1; # set first in pair
				$readpairid = $templateid."_R2";
				$readpairsamid =~ s/\d$/2/g;
			}
			else {
				$flags[3] = 1; # set second in pair
				$readpairid = $templateid."_R1";
				$readpairsamid =~ s/\d$/1/g;
				if ($flags[6] == 0) { $flags[6] = 1;} else { $flags[6] = 0; } # correct the F5 read strand.
			}
		
			if (defined($reads->{$readpairid})) { # both reads mapped.
				$data[6] = $reads->{$readpairid}->alnchr; # set RNEXT
				$data[7] = $reads->{$readpairid}->alnchrstart; # set PNEXT
				if (($reads->{$readid}->distancetopair ne 'NA')&&($reads->{$readid}->distancetopair < $maxdistance)) {
					$flags[9] = 1; # set proper mapped pair true
				}
			}
			else {
				$flags[7] = 1; # mate is unmapped
			}
		}

		my $newflag = unpack("N", pack("B32", substr("0" x 32 . join("", @flags), -32)));
		$data[1] = $newflag;
		
		# now add the custom tags:
		my $fidtag	= 'ZA:Z:'.$reads->{$readid}->alnfeatureid;
		my $typetag = 'ZT:Z:'.$feature2flag{$type};
		my $rgtag = 'RG:Z:'.$self->{'library_id'};
		
		if ($type eq 'transcripts') {
			$fidtag	= 'ZA:Z:'.$reads->{$readid}->alngeneid;
		}

		#push(@data, ($typetag, $fidtag, $rgtag));
		push(@data, ($typetag, $fidtag, $rgtag));
		
		$alignments[$i] = join("\t", @data);
	}
	
	return \@alignments;
}



################################################################################
=pod

B<add_custom_tags()>

Add the custom tags to the SAM line.
These will be:
  ZA:     alexa feature ID (for transcripts use the geneid as we would have to guess for the txid for most alignments)
  ZT:     feature type (use codes: transcript:TX, intron:IN, intergenic:IG, Novel Junction: NJ, Boundary: EB)
  
Parameters:
 ALIGNMENTS	=> ref to array of SAM lines
 TYPE		=> feature type
 READS		=> ref to array of read IDs

Requires:

Returns:
 ref to array of SAM lines (with fixed cigar)



sub add_custom_tags {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "--- Fixing flag field strings for alignments ---");

	my $alignments_ref	= $options->{'ALIGNMENTS'};
	my $type	= $options->{'TYPE'};
	my $reads	= $options->{'READS'};
	
	my @alignments = @{$alignments_ref};
	my $numAlignments = scalar (@alignments);
	
	for (my $i = 0; $i < $numAlignments; $i++) {
		
		my $al = $alignments[$i];
		my @data	= split /\s+/, $al;	
		my $readid	= $self->samid2readid(ID => $data[0]);
	
		
        
		my $fidtag	= 'ZA:Z:'.$reads->{$readid}->alnfeatureid;
		my $typetag = 'ZT:Z:'.$feature2flag{$type};
		
		if ($type eq 'transcripts') {
			$fidtag	= 'ZA:Z:'.$reads->{$readid}->alngeneid;
		}

		push(@data, ($typetag, $fidtag));
		
		$alignments[$i] = join("\t", @data);
	}

	return(\@fixed_alignments);

}

################################################################################
=pod

B<fix_alignments_flags()>

Add flags to the SAM line to describe the feature

Parameters:
 ALIGNMENTS	=> ref to array of SAM lines
 TYPE		=> feature type
 READS		=> ref to array of read IDs

Requires:

Returns:
 ref to array of SAM lines (with fixed cigar)

=cut

=cut

sub fix_alignments_flags {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "--- Fixing flag field strings for alignments ---");

	my $alignments	= $options->{'ALIGNMENTS'};
	my $type	= $options->{'TYPE'};
	my $reads	= $options->{'READS'};

	# flag = $QCMG_AS_FLAG

=cut

=cut
    attach as custom SAM tags:
        alexa feature ID	ZA
        alexa sequence database ID??	ZS
        alignment bit score	ZZ
        feature type (use codes: transcript:TX, intron:I, intergenic:IG, Junction: J, Boundary: B) ZT
        alexa strand information. 	ZW
    make note of flag information:
        existing flag information (note: this is really bad when using bfast 0.6 at the moment and should not be trusted).
        for transcripts, introns, intergenics, are the reads paired properly?  ZR
        for boundaries and junctions - nothing for now?? 

Type  Regexp matching VALUE                                 Description 
-------------------------------------------------------------------------------
A     [!-~]                                                 Printable character 
i     [-+]?[0-9]+                                           Signed 32-bit integer 
f     [-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?                Single-precision ﬂoating number 
Z     [ !-~]+                                               Printable string, including space 
H     [0-9A-F]+                                             Byte array in the Hex format (i.e., a byte array <code>{0x1a,0xe3,0x1}</code> corresponds to a Hex string <code>1AE301</code>) 
B     [cCsSiIf](,[-+]?[0-9]*\.?[0-9]+([eE][-+]?[0-9]+)?)+   Integer or numeric array


	ZA:Z:000PBLIV:6:396:1553:159#0/2
	ZS:Z:hs_60_37e
	ZZ:i:23
	ZT:Z:B
	ZW:Z:-1
	ZR:i:1 (bit score? 1 = reads paired properly, 2 = reads not paired properly)
=cut

=cut
	## ADD CODE HERE...
	my @fixed_alignments	= ();
	foreach my $a (@{$alignments}) {
		$a		=~ /^(.+?)\s+/;
		my $readid	= $1;
		$readid		=~ /(\d)$/;
		my $readnum	= $1;
		$a		=~ /^(.+?)\#/;
		my $rootid	= $1;

		my $fnroot	= 'R'.$readnum;

        	# alexa feature ID	ZA
		my $fn		= $fnroot."_FeatureId";
		my $fid		= $reads->{$rootid}->$fn();
		my $fidflag	= 'ZA:Z:'.$fid;

        	# alexa sequence database ID??	ZS
		my $fn		= $fnroot."_FeatureId";
		my $fid		= $reads->{$rootid}->$fn();
		my $fidflag	= 'ZA:Z:'.$fid;

        	# alignment bit score	ZZ
		my $fn		= $fnroot."_BitScore";
		my $bitscore	= $reads->{$rootid}->$fn();
		my $scoreflag	= 'ZZ:f:'.$bitscore;

        	# feature type (use codes: transcript:TX, intron:I, intergenic:IG, Junction: J, Boundary: B) ZT
		my $typeflag	= 'ZT:Z:'.$feature2flag{$type};

        	# alexa strand information. 	ZW
		my $fn		= $fnroot."_Strand";
		my $strand	= $reads->{$rootid}->$fn();
		my $strandflag	= 'ZW:i:'.$strand;

        	# for transcripts, introns, intergenics, are the reads paired properly?  ZR
		my $are_paired	= 0;	# 1 = correct pairing; -1 = incorrect pairing; 0 = not set
		# assume that correct pairing means both reads belong to the
		# same feature type (?)
		#my $f1t		= $reads->{$rootid}->R1_FeatureType();
		#my $f2t		= $reads->{$rootid}->R2_FeatureType();
		#$are_paired	= -1 if($f2t ne $f1t);
		my $pairflag	= 'ZR:i:'.$are_paired;

		push @fixed_alignments, join "\t", $a, $fidflag, $scoreflag, $typeflag, $strandflag, $pairflag;
	}

	$self->writelog(BODY => "---");	

	return(\@fixed_alignments);
}
=cut


################################################################################
=pod

B<reverse_cigar()>

Reverse a CIGAR string

Parameters:
	CIGAR: the CIGAR string

Requires:

Returns:
	String: the reversed CIGAR

=cut

sub reverse_cigar {
	my $self	= shift @_;

    #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
    my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

    # parse params
	my $options = {};
	$self->{options} = $options;
    for(my $i=0; $i<=$#_; $i+=2) {
        my $this_sub = (caller(0))[0]."::".(caller(0))[3];
        $options->{uc($_[$i])} = $_[($i + 1)];
    }

	my $cigar = $options->{'CIGAR'};
	my $revcigar = "";
		
	my @frags	= ($cigar =~ /(\d+[A-Z])/g);

	my @revfrags = reverse(@frags);
	return join("", @revfrags);
}

################################################################################
=pod

B<revcomp()>

Reverse complement a sequence

Parameters:
	SEQ: the sequence

Requires:

Returns:
	String: the reverse complemented SEQ.

=cut
sub revcomp {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

		my $sequence = $options->{'SEQ'};

	my $revcompseq    = reverse($sequence);
	$revcompseq       =~ tr/CATGN/GTACN/;
	
	return $revcompseq;
	
}

################################################################################
=pod

B<convert_rs()>

Convert RS (Chromosome) labels to our standard  (hard-coded to ICGC hg19)

Parameters:

Requires:

Returns:
 ref to hash, key = ensembl name, value = QCMG name

=cut
sub convert_rs {
	my $self	= shift @_;

        #print STDERR (caller(0	))w0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $convfile = "/panfs/share/genomes/GRCh37_ICGC_standard_v2/chromosome_conversions.txt";
=cut
	#Ensemblv55     QCMG/NCBI       diBayes DCC_v0.4
	1       chr1    chr1    1
	10      chr10   chr10   10
	11      chr11   chr11   11
	12      chr12   chr12   12
=cut
	my %ens2qcmg;
	#my %ens2dcc;
	#my %dcc2qcmg;
	open(FH, $convfile) || die "$!: $convfile\n";
	while(<FH>) {
		next if(/^#/);
		chomp;
	
		my ($ens, $qcmg, $di, $dcc) = split /\t/;
	
		$ens2qcmg{$ens} = $qcmg;
		#$ens2dcc{$ens} = $dcc;
		#$dcc2qcmg{$dcc} = $qcmg;
		#print STDERR "$ens -> $qcmg\n";
	}
	close(FH);

	return(\%ens2qcmg);
}

################################################################################
=pod

B<is_reversed()>

Determine is read is reversed based on flag bits

Parameters:
 FLAG	=> integer flag bit from SAM/BAM line

Requires:

Returns:
 scalar 'y' if reversed; 'n' if not

=cut
sub is_reversed {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

        my $num = $options->{'FLAG'};

	my $is_reversed	= 'n';	# default

        if($num & 0x10) {
                # bit 5
                #5  0x0010   r     strand of the query (0 for forward; 1 for reverse strand) [0 = +; 1 = -] [REVERSED]
		$is_reversed	= 'y';
        }

	return($is_reversed);
}


################################################################################
=pod

B<calculate_alignment_length()>

Calculate the length of the query alignment using the CIGAR string.

Parameters:
  CIGAR => the CIGAR string

Requires:

Returns:
 Integer (length of the alignment relative to the subject)

=cut
sub calculate_alignment_length {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	 my $cigar = $options->{'CIGAR'};
	my $alignmentLength = 0;
	
	my @frags	= ($cigar =~ /(\d+[A-Z])/g);
	
	
	foreach my $frag (@frags) {
		$frag	=~ /(\d+)([A-Z])/;
		my $len	= $1;
		my $typ	= $2;
		if (($typ eq 'M')||($typ eq 'D')||($typ eq 'S')||($typ eq 'H')) {
			$alignmentLength += $len;
		}
		elsif ($typ eq 'I') {
			#$alignmentLength -= $len; Just do nothing.  This does not count towards the reference alignment length
		}
		else {
			$self->writelog(BODY => "Unknown CIGAR code: $typ [length: $len]");
			die;
		}
	}
	
	return $alignmentLength;
}

################################################################################
=pod

B<sam2bam()>

Convert SAM lines to BAM file

Parameters:
 SAM	=> filename of SAM file to convert
 BAM	=> filename of BAM output file
 GENOME	=> path to reference genome .fa file (for BAM header)

Requires:

Returns:
 scalar BAM file name

=cut
sub sam2bam {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "--- Converting SAM to BAM ---");

	my $sam		= $options->{'SAM'};
	my $genome	= $options->{'GENOME'};
	my $bam		= $options->{'BAM'};

	# should probably edit PG line...

	my $cmd		= qq{samtools view -bS -T $genome $sam > $bam};

	print STDERR "SAM2BAM: $cmd\n";

	system($cmd);

	$self->writelog(BODY => "---");

	return();
}


################################################################################
=pod

B<flip_flag_bit()>

Flip a SAM flag bit and return the decimal representation.

Parameters:
 FLAG	=> decimal flag bit from SAM/BAM line
 BIT_TO_FLIP => the bit to flip.  0 becomes 1, 1 becomes 0.

Requires:

Returns:
 integer: Decimal representation of all flags

=cut
sub flip_flag_bit {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

        my $flag = $options->{'FLAG'};
		my $flip = $options->{'FLIP'};
		
		
		my $flagLength = 11; # the number of SAM file flags/.

		# recalculate the bit to flip:
		my $bitToFlip = $flagLength - ($flagLength - $flip - 1);

		# convert to binary flag.
		my $str = unpack("B32", pack("N", $flag));
		$str =~ s/^0+(?=\d)//;
		my $leadingZeros = $flagLength - length($str);
		for (my $i = 0; $i < $leadingZeros; $i++) { $str = '0'.$str; }


		#print "$flag\t[$str]\n";

		my @flags = split (//, $str);
		if ($flags[$bitToFlip] == 0) { $flags[$bitToFlip] = 1; } else { $flags[$bitToFlip] = 0; }
		my $modflag = join("", @flags);
		my $dec = unpack("N", pack("B32", substr("0" x 32 . $modflag, -32)));

		return $dec;
		#print $dec."\t[".join("", @flags)."]\n";
}



################################################################################
=pod

B<read_flag_bits()>

Convert SAM/BAM flag bits to text 

&read_flag_bits(FLAG => $bits)

Parameters:
 FLAG	=> integer flag bit from SAM/BAM line

Requires:

Returns:
 scalar string of flag values as text

=cut
sub read_flag_bits {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

        my $num = $options->{'FLAG'};

        my @flags = ();

        if($num & 0x1) {
                # bit 1
                #1  0x0001   p     the read is paired in sequencing, no matter whether it is mapped in a pair [PAIRED]
                push @flags, 'PAIRED';
        }
        if($num & 0x2) {
                # bit 2
                #2  0x0002   P     the read is mapped in a proper pair (depends on the protocol, normally inferred during alignment) 1 [MAP_PAIR]
                push @flags, 'MAP_PAIR';
        }
        if($num & 0x4) {
                # bit 3
                #3  0x0004   u     the query sequence itself is unmapped [UNMAPPED]
                push @flags, 'UNMAPPED';
        }
        if($num & 0x8) {
                # bit 4
                #4  0x0008   U     the mate is unmapped 1 [M_UNMAPPED]
                push @flags, 'M_UNMAPPED';
        }
        if($num & 0x10) {
                # bit 5
                #5  0x0010   r     strand of the query (0 for forward; 1 for reverse strand) [0 = +; 1 = -] [REVERSED]
                push @flags, 'REVERSED';
        }
        if($num & 0x20) {
                # bit 6
                #6  0x0020   R     strand of the mate 1 [M_REVERSED]
                push @flags, 'M_REVERSED';
        }
        if($num & 0x40) {
                # bit 7
                #7  0x0040   1     the read is the first read in a pair 1,2 [FIRST_MATE]
                push @flags, 'FIRST_MATE';
        }
        if($num & 0x80) {
                # bit 8
                #8  0x0080   2     the read is the second read in a pair 1,2 [SECOND_MATE]
                push @flags, 'SECOND_MATE';
        }
        if($num & 0x100) {
                # bit 9
                #9  0x0100   s     the alignment is not primary (a read having split hits may have multiple primary alignment records) [NOT_PRIMARY]
                push @flags, 'NOT_PRIMARY';
        }
        if($num & 0x200) {
                # bit 10
                #10 0x0200   f     the read fails platform/vendor quality checks [QC_FAILED]
                push @flags, 'QC_FAILED';
        }
        if($num & 0x400) {
                # bit 11 
                #11 0x0400   d     the read is either a PCR duplicate or an optical duplicate [DUPLICATE]
                push @flags, 'DUPLICATE';
        }

        return join '|', @flags;
}

################################################################################
=pod

B<toollog()> 

Parameters:
 Write TOOLLOG names and values to log file and any extra EXECLOG lines

Returns:

=cut
sub toollog {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $date = $self->timestamp();
	
	my $msg		.= qq{TOOLLOG: LOG_FILE $self->{'log_file'}\n};
	$msg		.= qq{TOOLLOG: ANALYSIS_DIR $self->{'analysis_dir'}\n};
	$msg		.= qq{TOOLLOG: LIBRARY_ID $self->{'library_id'}\n};
	$msg		.= qq{TOOLLOG: SEQDB_DIR $self->{'seqdb_dir'}\n};
	$msg		.= qq{TOOLLOG: SEQDB_NAME $self->{'seqdb_name'}\n};
	$msg		.= qq{TOOLLOG: SEQDB_VER $self->{'seqdb_ver'}\n};
	$msg		.- qq{TOOLLOG: READLEN1: $self->{'readlen1'}\n};
	$msg		.- qq{TOOLLOG: READLEN2: $self->{'readlen2'}\n};
	$msg		.- qq{TOOLLOG: JUNCDIFF1: $self->{'juncdiff1'}\n};
	$msg		.- qq{TOOLLOG: JUNCDIFF2: $self->{'juncdiff2'}\n};
	$msg		.= qq{TOOLLOG: READRECDIR $self->{'read_records'}\n};
	$msg		.= qq{TOOLLOG: ALIGNRESDIR $self->{'alignment_results'}\n};

	$self->writelog(BODY => $msg);

	$self->writelog(BODY => "Beginning BAM creation process at $date\n");

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

	# override $? with $self->{'EXIT'}

	unless($self->{'EXIT'} > 0) {
		$self->writelog(BODY => "\nEXECLOG: errorMessage none");
	}

	my $eltime	= $self->elapsed_time();
	my $date	= $self->timestamp();

	$self->writelog(BODY => "EXECLOG: elapsedTime $eltime");
	$self->writelog(BODY => "EXECLOG: stopTime $date");
	$self->writelog(BODY => "EXECLOG: exitStatus $self->{'EXIT'}");

	close($self->{'log_fh'});

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
