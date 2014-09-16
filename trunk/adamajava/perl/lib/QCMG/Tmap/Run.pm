package QCMG::Tmap::Run;

##############################################################################
#
#  Module:   QCMG::Tmap::Run.pm
#  Creator:  Lynn Fink
#  Created:  2011-02-28
#
#  This class contains methods for automating the running of Tmap on raw
#  sequencing data files
#
# $Id: Run.pm 1287 2011-10-19 03:40:38Z l.fink $
#
##############################################################################


=pod

=head1 NAME

QCMG::Tmap::Run -- Common functions for running Tmap

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Tmap::Run->new();

=head1 DESCRIPTION

Contains methods for checking that raw sequencing data files are ready for
mapping, that Tmap is ready to be run, and to run Tmap

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'
 QCMG::Automation::LiveArc
 QCMG::Automation::Config
 QCMG::DB::Geneus

=cut

use strict;

# standard distro modules
use Data::Dumper;
use File::Spec;				# for parsing paths

# in-house modules
use QCMG::Automation::LiveArc;		# for accessing Mediaflux/LiveArc
use QCMG::DB::Geneus;

use QCMG::Automation::Common;
our @ISA = qw(QCMG::Automation::Common);	# inherit Common methods


use vars qw( $SVNID $REVISION $TMAP_ATTEMPTED $TMAP_SUCCEEDED $IMPORT_ATTEMPTED $IMPORT_SUCCEEDED );

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 SLIDE_FOLDER => directory
 LOG_FILE   => path and filename of log file

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
	$self->{'slide_folder'}		= $options->{'SLIDE_FOLDER'};

	$self->{'log_file'}		= $options->{'LOG_FILE'};

	#$self->{'hostname'}		= `hostname`;
	$self->{'hostname'}		= $self->HOSTKEY;
	chomp $self->{'hostname'};

	# RUN FOLDER NAME, should have / on the end
	if($self->{'slide_folder'} !~ /\/$/){
		$self->{'slide_folder'} .= '/';
	}

	$self->init_log_file();

	# must define log file before calling any another methods
	$self->slide_name();

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	# set global flags for checking in DESTROY()  (no/yes)
	$IMPORT_ATTEMPTED = 'no';
	$IMPORT_SUCCEEDED = 'no';
	$TMAP_ATTEMPTED = 'no';
	$TMAP_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: No run folder: $self->{'slide_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Bad permissions on $self->{'slide_folder'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_NO_SFF_FILE {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: No SFF file";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 88;
	return 88;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_ASSET_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: LiveArc asset exists";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 12;
	return 12;
}
sub EXIT_IMPORT_FAILED {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: LiveArc import failed";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 16;
	return 16;
}
sub EXIT_ASSET_NOT_EXISTS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: Requested LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 24;
	return 24;
}
sub EXIT_NO_PBS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: PBS scripts were not created";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 51;
	return 51;
}
sub EXIT_NO_REFGENOME {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- FAILED";
	my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'}";
	$body		.= "\nFailed: A reference genome could not be inferred";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	#$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: ERRORMESSAGE $body");

	$self->{'EXIT'} = 52;
	return 52;
}

# SET exit(17) = run not attempted (but only used in DESTROY())

################################################################################

=pod

B<slide_folder()>

Return run folder name, as set in new()

Parameters:

Returns:
  scalar run folder name

=cut
sub slide_folder {
	my $self	= shift @_;
	return $self->{'slide_folder'};
}

################################################################################
=pod

B<slide_name()> 
  Get slide name from run folder name (uses QCMG::FileDir::SeqRawParser)

Parameters:

Returns:
  scalar slide name 

=cut

sub slide_name {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
	# SLIDE_FOLDER - directory where runs are
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $rf			= $self->{'slide_folder'};
	$rf			=~ s/\/$//;
	my ($v, $d, $file)	= File::Spec->splitpath($rf);
	$self->{'slide_name'} 	= $file;

	print STDERR "Slide name:\t".$self->{'slide_name'}."\n";

        return $self->{'slide_name'};
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

B<check_folder()>

Check that directory provided by user exists, is a directory, and is readable

Parameters:

Returns:
 scalar: 0 if success; otherwise fill exit on fail

=cut
sub check_folder {
	my $self	= shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";
        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};

	my $status = 1;

	$self->writelog(BODY => "---Checking ".$self->{'slide_folder'}." ---");

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	if(	!    $self->{'slide_folder'} ||
		! -e $self->{'slide_folder'} || 
		! -d $self->{'slide_folder'}    ) {

		$self->writelog(BODY => "No valid run folder provided\n");
		#exit($self->EXIT_NO_FOLDER());
		exit($self->EXIT_NO_FOLDER());
	}

	if(	! -r $self->{'slide_folder'}) {
		my $msg = " is not a readable directory";
		$self->writelog(BODY => $self->{'slide_folder'}.$msg);
		exit($self->EXIT_BAD_PERMS());
	}

	if(	! -w $self->{'slide_folder'}) {
		my $msg = " is not a writable directory";
		$self->writelog(BODY => $self->{'slide_folder'}.$msg);
		#exit($self->EXIT_BAD_PERMS());
	}

	$self->writelog(BODY => "Folder exists and is read/writable.");	

	return($status);
}

################################################################################
=pod

B<add_email()> 
 Enter an email address to which a notification will be mailed upon completion of 
 the ingestion to let them know if it succeeded or if it failed ingestion.

Parameters:
 EMAIL

Returns:
 void

=cut

sub add_email {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	push @{$self->{'email'}}, $options->{'EMAIL'};

	my $emails = join ", ", @{$self->{'email'}};

	$self->writelog(BODY => "Setting additional email: $emails");

	return;
}

################################################################################
=pod

B<get_email()> 
 Get all email addresses that will receive notifications

Parameters:

Returns:
 scalar: ref to array of addresses

=cut
sub get_email {
        my $self = shift @_;

	return($self->{'email'});
}

################################################################################
=pod

B<runpbs2mapset()> 
 Extract the mapset name from a run.pbs file

Parameters:
 FILE - run.pbs file name

Returns:
 scalar mapset name

=cut

sub runpbs2mapset {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "Determining barcode status from LIMS...");
	# find out if this is a barcoded run or not; if not, use .sff file; else
	# use .barcode.sff.zip
	my $is_barcoded		= 'n';	# set with LIMS

	my $gl = QCMG::DB::Geneus->new;
	$gl->connect;

	my ($data, $slide)	= $gl->torrent_run_by_slide($self->{'slide_name'});

	#print Dumper $slide;

	my @barcodes;
	foreach my $field (keys %{$slide}) {
        	foreach my $key (keys %{$slide->{$field}}) {
                        #print STDERR "$key: ".$slide->{$field}->{$key}."\n";
			if($key eq 'barcode' && $slide->{$field}->{$key} =~ /\d/) {
				$is_barcoded	= 'y';
				push @barcodes, $slide->{$field}->{$key};
				print STDERR "BARCODE: ".$slide->{$field}->{$key}."\n";
			}
        	}
	}

	$self->{'barcodes'}	= \@barcodes;

	# seq_raw/S0449_20100603_1_Frag/_tmap_ini/S0449_20100603_1_Frag.nopd.nobc/20110512/run.pbs
	my ($v, $d, $f)	= File::Spec->splitpath($options->{'FILE'});
	# $d = seq_raw/S0449_20100603_1_Frag/_tmap_ini/S0449_20100603_1_Frag.nopd.nobc/20110512/
	$d =~ s/\/$//;
	# $d = seq_raw/S0449_20100603_1_Frag/_tmap_ini/S0449_20100603_1_Frag.nopd.nobc/20110512
	($v, $d, $f)	= File::Spec->splitpath($d);
	# $d = seq_raw/S0449_20100603_1_Frag/_tmap_ini/S0449_20100603_1_Frag.nopd.nobc/
	$d =~ s/\/$//;
	# $d = seq_raw/S0449_20100603_1_Frag/_tmap_ini/S0449_20100603_1_Frag.nopd.nobc
	($v, $d, $f)	= File::Spec->splitpath($d);
	# $f = S0449_20100603_1_Frag.nopd.nobc

	$self->writelog(BODY => "Mapset: $f");

	return($f);
}

################################################################################
=pod

B<create_pbs_scripts()> 
 Create PBS scripts that will run tmap, create indexes, and copy BAMs to the
  appropriate place

Parameters:

Returns:

=cut
sub create_pbs_scripts {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $status = 0;

	# from a Torrent Suite-mapped BAM header:
	# tmap mapall -n 12 -f REFGENOME -r SFFFILE -Y -v -R 
	# -n,--num-threads          INT            the number of threads (autodetect) [24]
	# -f,--fn-fasta             FILE           FASTA reference file name [not using]
	# -r,--fn-reads             FILE           the reads file name [stdin]
	# -Y,--sam-flowspace-tags                  include flow space specific SAM tags when available [false]
	# -v,--verbose                             print verbose progress information [false]
	# -R,--sam-read-group       STRING         the RG tags to add to the SAM header [not using]

	# @PG     ID:tmap VN:0.2.3        CL:mapall -n 12 -f /results/referenceLibrary/tmap-f2/ICGC_HG19/ICGC_HG19.fasta -r IonXpress_009_R_2012_03_02_23_28_29_user_T01-139_Analysis_T01-139.sff -Y -v -R LB:ICGC_HG19 -R CN:TorrentServer -R PU:PGM/318B -R ID:HMJSI -R SM:targetseq_109 -R DT:2012-03-02T23:28:29 -R PL:IONTORRENT stage1 map1 map2 map3

	$self->writelog(BODY => "---Creating PBS scripts for tmap---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	# get date of run for BAM header
	$self->{'slide_name'}	=~ /T\d+\_(\d{8})\_/;
	my $date		= $1;

	#### BUILD COMMAND TEMPLATES
	# whole script with bash if loops
	my $flow		= '
<MODLOAD>

# for qvisualise.pl
export PERL5LIB=/share/software/QCMGPerl/lib/:$PERL5LIB

<TMAP>

if [ $? -eq 0 ]
        then echo "Converting SAM to BAM"
	<SAM2BAM>
fi

if [ $? -eq 0 ]
        then echo "Sorting BAM"
	<SORTBAM>
fi

if [ $? -eq 0 ]
        then echo "Copying BAM to seq_results"
	<MVBAM>
fi

if [ $? -eq 0 ]
        then echo "Creating BAM index"
	<BAI>
fi

if [ $? -eq 0 ]
        then echo "Running qcoverage"
	<QCOV>
	<QVIS>
fi

if [ $? -eq 0 ]
        then echo "Ingesting BAM"
	<INGEST>
fi

if [ $? -eq 0 ]
        echo "tmap run successful"
fi

';
	my $seqmappeddir	= join "/", $self->SEQ_MAPPED_PATH, $self->{'slide_name'};
	my $rv			= mkdir $seqmappeddir;
	if($rv != 0) {
		$self->writelog(BODY => "Error creating $seqmappeddir");
		#exit($self->EXIT_BAD_PERMS());
	}

	# load module commands
	my $modloadcmd		= qq{module load tmap\n\n};
	$modloadcmd		.= qq{module load samtools\n\n};
	$modloadcmd		.= qq{module load adama};

	# tmap command
	my $tmapcmd		.= qq{/panfs/share/software/tmap/tmap-20120410/bin/tmap mapall };
	#my $tmapcmd		.= qq{tmap mapall };
	$tmapcmd		.= qq{ -n <THREADS> -f <REF> -r <SFF>  -Y -v -R LB:<LB> -R CN:<CN> -R PU:<PU> -R ID:<ID> -R SM:<SM> -R DT:<DT> -R PL:<PL>};
	$tmapcmd		.= qq{ stage1 map1 map2 map3   > <SAM>};

	# convert SAM to BAM command
	my $sam2bamcmd		= qq{samtools view -bS <SAM> > <UNSORTEDBAM>};

	# sort BAM command
	my $sortbamcmd		= qq{samtools sort <UNSORTEDBAM> <BAMPREFIX>};

	# BAM move command (move? copy?)
	my $mvbamcmd		= qq{cp <BAM> <NEWBAM>};

	# BAM index command
	my $baicmd		= qq{samtools index <NEWBAM>};

	# qcoverage command (if applicable)
	my $qcovcmd		= qq{qcoverage -t seq -n 13 --xml --query 'Flag_DuplicateRead==false' };
	$qcovcmd		.= qq{--gff <GFF> --bam <NEWBAM> --bai <NEWBAM>.bai };
	$qcovcmd		.= qq{--log <NEWBAM>.qcov.log --output <NEWBAM>.qcov.xml};

	# qvisualise command (if qcoverage is used
	my $viscmd		= qq{<BIN_SEQTOOLS>/qvisualise.pl qcoverage -i <XML> -o <XML>.html -log <XML>.qvis.log};

	# command to ingest mapped files
	my $ingestcmd		= qq{#<BIN_AUTO>/ingest_mapped_torrent.pl -i <MAPSET>};
	####

	#### FILL TEMPLATES
	my @pbsfiles	= ();
	foreach my $b (@{$self->{'barcodes'}}) {
		my $mapset		= $self->infer_mapset(BARCODE => $b);

		# get presumed SFF and anticipated SAM filenames
		my $sff			= $self->infer_sff(BARCODE => $b);
		my $sam			= $self->infer_sam(BARCODE => $b);
		my $ref			= $self->infer_reference(REFERENCE => $self->{'refgenome'});
		my $library		= $self->{'barcode2prilib'}->{$b};

		# anticipated seq_mapped BAM
		my $bam			= $self->infer_seqmapped_bam(BARCODE => $b);
		my $unsortedbam		= $bam;
		$unsortedbam		=~ s/\.bam/\.unsorted\.bam/;
		my $bamprefix		= $bam;
		$bamprefix		=~ s/\.bam//;
		# anticipated seq_results BAM
		my $newbam		= $self->infer_seqresults_bam(BARCODE => $b, PATIENT => $self->{'barcode2patient'}->{$b});

		# qcoverage parameters
		my $gff			= $self->infer_gff(PROJECT_TYPE => $self->{'project_type'});
		my $xml			= $newbam.".qcov.xml";

		my $binseqtools		= $self->{'BIN_SEQTOOLS'};
		my $binauto		= $self->{'BIN_AUTO'};

		### build commands
		$tmapcmd		=~ s/<THREADS>/8/g;
		$tmapcmd		=~ s/<REF>/$ref/g;
		$tmapcmd		=~ s/<SFF>/$sff/g;
		$tmapcmd		=~ s/<LB>/$library/g;
		$tmapcmd		=~ s/<ID>/$b/g;
		$tmapcmd		=~ s/<SM>/$self->{'project_type'}/g;
		$tmapcmd		=~ s/<CN>/\"QCMG TorrentServer\"/g;
		$tmapcmd		=~ s/<PU>/PGM\/$self->{'chip_type'}/g;
		$tmapcmd		=~ s/<DT>/$date/g;
		$tmapcmd		=~ s/<PL>/IONTORRENT/g;
		$tmapcmd		=~ s/<SAM>/$sam/g;

		$sam2bamcmd		=~ s/<SAM>/$sam/g;
		$sam2bamcmd		=~ s/<BAM>/$bam/g;

		$sortbamcmd		=~ s/<UNSORTEDBAM>/$unsortedbam/g;
		$sortbamcmd		=~ s/<BAMPREFIX>/$bamprefix/g;

		$mvbamcmd		=~ s/<BAM>/$bam/g;
		$mvbamcmd		=~ s/<NEWBAM>/$newbam/g;

		$baicmd			=~ s/<NEWBAM>/$newbam/g;

		# only add qcoverage/qvisualise commnands if appropriate
		if($gff) {
			$qcovcmd	=~ s/<GFF>/$gff/g;
			$qcovcmd	=~ s/<BAM>/$bam/g;
			$qcovcmd	=~ s/<NEWBAM>/$newbam/g;

			$viscmd		=~ s/<BIN_SEQTOOLS>/$binseqtools/g;
			$viscmd		=~ s/<XML>/$xml/g;
		}
		else {
			$qcovcmd	= '';
			$viscmd		= '';
		} 

		$ingestcmd		=~ s/<BIN_AUTO>/$binauto/g;
		$ingestcmd		=~ s/<MAPSET>/$mapset/g;

		# create flow for PBS script with all commands in place to allow
		# dependency on the previous command
		my $cmd			= $flow;
		$cmd			=~ s/<MODLOAD>/$modloadcmd/;
		$cmd			=~ s/<TMAP>/$tmapcmd/;
		$cmd			=~ s/<SAM2BAM>/$sam2bamcmd/;
		$cmd			=~ s/<MVBAM>/$mvbamcmd/;
		$cmd			=~ s/<SORTBAM>/$sortbamcmd/;
		$cmd			=~ s/<UNSORTEDBAM>/$unsortedbam/;
		$cmd			=~ s/<BAI>/$baicmd/;
		$cmd			=~ s/<QCOV>/$qcovcmd/;
		$cmd			=~ s/<QVIS>/$viscmd/;
		$cmd			=~ s/<INGEST>/$ingestcmd/;

		$self->writelog(BODY => "Writing PBS file for $mapset");
		my $filename		= $self->write_pbs_file(CMD => $cmd, MAPSET => $mapset);
		push @pbsfiles, $filename;
		$self->writelog(BODY => "\t$filename");
		last;
	}

	$self->{'pbsfiles'}	= \@pbsfiles;

	$self->writelog(BODY => "END: ".$self->timestamp());

	return($status);
}

################################################################################
=pod

B<write_pbs_file()> 
 Write PBS file for each barcode

Parameters:
 CMD	=> all commands to be run by PBS script
 MAPSET => mapset string

Returns:
 scalar string of filename with path

=cut
sub write_pbs_file {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $cmd		= $options->{'CMD'};
	my $mapset	= $options->{'MAPSET'};

	my $pbs		= qq{#!/bin/sh 
#PBS -N tmap
#PBS -q batch
#PBS -r n 
#PBS -l ncpus=8,walltime=10:00:00,mem=44gb
#PBS -m ae 
#PBS -M l.fink\@imb.uq.edu.au

};

	my $filename	= join "/", $self->SEQ_RAW_PATH, $self->{'slide_name'}, $mapset;
	$filename	.= ".tmap.pbs";

	# Now we are ready to write out the file
	open(FH, ">".$filename) || exit($self->EXIT_NO_PBS());
	print FH $pbs;
	print FH $cmd;
	close(FH);

	return($filename);
}

################################################################################
=pod

B<infer_mapset()> 
 Infer mapset from slidename and barcode

Parameters:
 BARCODE	=> a barcode

Returns:
 scalar string of mapset name

=cut
sub infer_mapset {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $barcode	= $options->{'BARCODE'};

	# T00001_20120419_162.nopd.IonXpress_009.sff
	my $mapset	= join ".", $self->{'slide_name'}, "nopd", $barcode;

	return ($mapset);
}

################################################################################
=pod

B<infer_sff()> 
 Infer SFF filename from slidename and barcode

Parameters:
 BARCODE	=> a barcode

Returns:
 scalar string of SFF file name, with path

=cut
sub infer_sff {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $barcode	= $options->{'BARCODE'};

	# T00001_20120419_162.nopd.IonXpress_009.sff
	my $mapset	= $self->infer_mapset(BARCODE => $barcode);
	my $sff		= join ".", $mapset, "sff";
	$sff		= join "/", $self->SEQ_RAW_PATH, $self->{'slide_name'}, $sff;

	return ($sff);
}

################################################################################
=pod

B<infer_sam()> 
 Infer SAM filename from slidename and barcode

Parameters:
 BARCODE	=> a barcode

Returns:
 scalar string of SAM file name, with path

=cut
sub infer_sam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $barcode	= $options->{'BARCODE'};

	# T00001_20120419_162.nopd.IonXpress_009.sam
	my $mapset	= $self->infer_mapset(BARCODE => $barcode);
	my $sam		= join ".", $mapset, "sam";
	$sam		= join "/", $self->SEQ_MAPPED_PATH, $self->{'slide_name'}, $sam;

	return ($sam);
}

################################################################################
=pod

B<infer_seqmapped_bam()> 
 Infer BAM filename from slidename and barcode (seq_mapped BAM)

Parameters:
 BARCODE	=> a barcode

Returns:
 scalar string of anticipated BAM file name, with path

=cut
sub infer_seqmapped_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $barcode	= $options->{'BARCODE'};

	# T00001_20120419_162.nopd.IonXpress_009.bam
	my $mapset	= $self->infer_mapset(BARCODE => $barcode);
	my $bam		= join ".", $mapset, "bam";
	$bam		= join "/", $self->SEQ_MAPPED_PATH, $self->{'slide_name'}, $bam;

	return ($bam);
}

################################################################################
=pod

B<infer_seqresults_bam()> 
 Infer BAM filename from slidename and barcode (seq_results BAM)

Parameters:
 BARCODE	=> a barcode
 PATIENT	=> a patient (eg, APGI_1992)

Returns:
 scalar string of anticipated BAM file name, with path

=cut
sub infer_seqresults_bam {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $barcode	= $options->{'BARCODE'};

	my $patient	= $options->{'PATIENT'};

	my $project	= $self->patient2project(PATIENT => $patient);

	# T00001_20120419_162.nopd.IonXpress_009.bam
	my $mapset	= $self->infer_mapset(BARCODE => $barcode);
	my $bam		= join ".", $mapset, "bam";
	my $subdir	= $self->projecttype2subdir(PROJECT_TYPE => $self->{'project_type'});
	# /mnt/seq_results/icgc_pancreatic/APGI_1992/[seq_mapped|seq_verify]/<BAM>
	$bam		= join "/", $self->SEQ_RESULTS_PATH, $project, $patient, $subdir, $bam;

	return ($bam);
}

################################################################################
=pod

B<patient2project()> 
 Infer project from patient ID

Parameters:
 PATIENT	=> patient ID

Returns:
 scalar string of project name (should match to dir in seq_results)

=cut
sub patient2project {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $patient_id	= $options->{'PATIENT'};

	# define parent project
	my $project;
	if($patient_id =~ /APGI/ || $patient_id =~ /CRL/) {
		$project	= 'icgc_pancreatic';
	}
	elsif($patient_id =~ /AOCS/) {
		$project	= 'icgc_ovarian';
	}
	elsif($patient_id =~ /COLO/) {
		$project	= 'smgres_special';
	}
	elsif($patient_id =~ /PPPP/) {
		$project	= 'smgres_endometrial';
	}
	elsif($patient_id =~ /^Q/ || $patient_id =~ /^B/) {
		$project	= 'smgres_brainmet';
	}
	elsif($patient_id =~ /^ANNC/) {
		$project	= 'smgres_andras';
	}

	return($project);
}

################################################################################
=pod

B<projecttype2subdir()> 
 Infer seq_results subdirectory from project type (seq_mapped or seq_amplicon)

Parameters:
 PROJECT_TYPE	=> project type (targetseq_109, etc) (defaults to seq_mapped)

Returns:
 scalar string of subdir name (either seq_mapped or seq_amplicon)

=cut
sub projecttype2subdir {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $type	= $options->{'PROJECT_TYPE'};

	my %ptype2subdir	= (
		'targetseq_109'		=> 'seq_mapped/',
		'targetseq_29'		=> 'seq_mapped/',
		'ampliseq_cancer'	=> 'seq_amplicon/',
		'ampliseq'		=> 'seq_amplicon/'
		);

	my $subdir	= $ptype2subdir{$type};
	$subdir		= 'seq_mapped' if(! $subdir);

	return($subdir);
}

################################################################################
=pod

B<infer_gff()> 
 Translate project type to segments file for qcoverage

Parameters:
 PROJECT_TYPE	=> project type (targetseq_109, etc) (defaults to seq_mapped)

Returns:
 scalar string of GFF file name (with path)

=cut
sub infer_gff {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $type	= $options->{'PROJECT_TYPE'};

	my %project2bed	= (
			'targetseq_109'		=> 'QCMG_109_design.gff3',
			'targetseq_29'		=> 'hg19_Cancer29_Tiled_regions.gff3',
			'ampliseq_cancer'	=> 'AmpliSeqCancerAmplicons.gff3',
			'ampliseq'		=> 'ASD270258.v1.gff3'
		);

	my $gff		= join "/", $self->SEGMENTS_DIR, $project2bed{$type};

	return($gff);
}

################################################################################
=pod

B<infer_reference()> 
 Translate reference genome label to FASTA file

Parameters:
 REFERENCE	=> reference genome label (eg, ICGC_HG19)

Returns:
 scalar string of FASTA file name (with path)

=cut
sub infer_reference {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                #defined($_[($i + 1)]) || warn "Odd number of params: $this_sub : $!";
                $options->{$_[$i]} = $_[($i + 1)];
        }

	my $label	= $options->{'REFERENCE'};

	# preferentially use the ref genomes on the minions; if it can't be
	# found, revert to the panfs version
	my $refpath	= '/usr/local/genomes/tmap-f3/';
	if(-e $refpath) {
		$refpath	= '/panfs/share/genomes/tmap-f3/';
	}

	my %label2ref	= (
			'ICGC_HG19'		=> $refpath.'/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa',
			'MM64'			=> ''
		);

	$self->writelog(BODY => "Using $label2ref{$label}");

	if(! $label2ref{$label}) {
		$self->writelog(BODY => "No reference genome file for label $label");
		exit($self->EXIT_NO_REFGENOME());
	}

	return($label2ref{$label});
}

################################################################################
=pod

B<run_tmap()> 
 Run tmap using PBS scripts

Parameters:

Requires:
 $self->{'pbsfiles'}

Returns:
 void

=cut
sub run_tmap {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	$self->writelog(BODY => "---Preparing for Tmap runs... ---");

	$TMAP_ATTEMPTED	= 'yes';

	# NS:    /test/S0449_20100603_1_Frag/
	# define LiveArc namespace
	my $ns			=  join "/", $self->LA_NAMESPACE, $self->{'slide_name'};

	foreach my $pbs (@{$self->{'pbsfiles'}}) {
=cut

		$self->writelog(BODY => "Ingesting PBS scripts to $ns");

		my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);

		# create  asset using run.pbs filename as template
		$self->writelog(BODY => "Creating asset from pbs script: $pbs");
		my ($v, $d, $asset)	= File::Spec->splitpath($pbs);
		my $assetid		= $la->create_asset(NAMESPACE => $ns, ASSET => $asset);
		my $status		= $la->update_asset_file(ID => $assetid, FILE => $pbs);

		if($status != 0) {
			$self->writelog(BODY => "Could not ingest $pbs as $ns / $asset");
			exit($self->EXIT_LIVEARC_ERROR());
		}
=cut

		# submit mapping job
		$self->writelog(BODY => "Submitting tmap run: $pbs");
		my $rv = `qsub $pbs`;
		$rv =~ /^(\d+)\./;
		my $jobid = $1;
		$self->writelog(BODY => "Submitted $pbs as job $jobid");

		my $subj	= $self->{'slide_name'}." TMAP.PBS SUBMITTED";
		my $body	= "TMAP.PBS SUBMITTED: ".$pbs."\n\nJob ID: ".$jobid;
		#$self->send_email(FROM => 'mediaflux@imq.uq.edu.au', TO => $self->DEFAULT_EMAIL, SUBJECT => $subj, BODY => $body);
	}

	$self->writelog(BODY => "Completed Tmap run job submissions");

	return();
}

################################################################################
=pod

B<query_lims()> 
 Query LIMS with slide name to get chip, barcode, and project info

Parameters:

Requires:
 $self->{'slide_name'}

Returns:

Set:
 $self->{'barcodes'} ref to array of barcode string
 $self->{'chip_type'}
 $self->{'project_type'}
 $self->{'refgenome'}
 $self->{'barcode2patient'} ref to hash, key = barcode, value = patient ID
 $self->{'barcode2prilib'} ref to hash, key = barcode, value = primary library

=cut
sub query_lims {
        my $self	= shift @_;

	my $gl = QCMG::DB::Geneus->new(server   => 'http://qcmg-glprod.imb.uq.edu.au:8080/api/v1/');
	$gl->connect;

	my $name	= $self->{'slide_name'};

	$self->writelog(BODY => "---Querying LIMS with $name ---");
	
	# get project type
	my $project_type	= $gl->torrent_slide2projecttype($name);
	#print STDERR "PROJECT TYPE: $project_type\n";
	$self->{'project_type'}	= $project_type;

	# get chip type
	my $chip_type	= $gl->torrent_slide2chiptype($name);
	#print STDERR "CHIP TYPE: $chip_type\n";
	$self->{'chip_type'}	= $chip_type;

	# get chip type
	my $refgenome	= $gl->torrent_slide2refgenome($name);
	$self->{'refgenome'}	= $refgenome;

	$self->writelog(BODY => "Project type:\t$project_type\nChip type:\t$chip_type\nReference:\t$refgenome\n");

	# get barcodes and patient/project info
	my ($data, $slide)	= $gl->torrent_run_by_slide($name);
	#print Dumper $slide;

	my @barcodes		= ();
	my %barcode2patient	= ();
	my %barcode2prilib	= ();
	foreach my $field (keys %{$slide}) {
		my $patient;
		my $barcode;
		my $prilib;
		foreach my $key (keys %{$slide->{$field}}) {
			$barcode	= $slide->{$field}->{$key} if($key eq 'barcode');
			$patient	= $slide->{$field}->{$key} if($key eq 'Patient ID');
			$patient	=~ s/[\-\.]+/\_/g;
			$prilib		= $slide->{$field}->{$key} if($key eq 'Primary Library'),
		}
		$self->writelog(BODY => "Barcode:\t$barcode\nPatient:\t$patient\nPrimary library: $prilib\n");
		$barcode2patient{ $barcode }	= $patient;
		$barcode2prilib{ $barcode }	= $prilib;
		push @barcodes, $barcode;
	}
	$self->{'barcodes'}		= \@barcodes;
	$self->{'barcode2patient'}	= \%barcode2patient;
	$self->{'barcode2prilib'}	= \%barcode2prilib;

	return();
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

	# java version for Mediaflux/LiveArc
	my $j		= $self->LA_JAVA;
	my $javaver	= `$j -version 2>&1 >/dev/null`;
	$javaver	=~ s/.+?version\s+\"(.+?)\".+/$1/s;

	my $date = $self->timestamp();
	
	my $msg		.= qq{EXECLOG: javaVersion $javaver\n};
	$msg		.= qq{EXECLOG: javaHome $j\n};

	$msg		.= qq{TOOLLOG: LOG_FILE $self->{'log_file'}\n};
	$msg		.= qq{TOOLLOG: SLIDE_FOLDER $self->{'slide_folder'}\n};
	$msg		.= qq{TOOLLOG: DEFAULT_EMAIL }.$self->DEFAULT_EMAIL."\n";
	$msg		.= qq{TOOLLOG: SLIDE_NAME}.$self->{'slide_name'}."\n";

	$self->writelog(BODY => $msg);

	$self->writelog(BODY => "Beginning ingestion process at $date");

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

	if(! $self->{'EXIT'}) {
		$self->{'EXIT'} = 0;
	}

	unless($self->{'EXIT'} > 0) {
		if($TMAP_ATTEMPTED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- SUCCEEDED";
			my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'} complete";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		# running tmap was not attempted; script ended for some other
		# reason
		elsif($TMAP_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage Tmap run not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RUN.PBS SUBMISSION -- NOT ATTEMPTED";
			my $body	= "SUBMISSION OF MAPPING JOB for $self->{'slide_folder'} not attempted";
			$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			#$self->send_email(SUBJECT => $subj, BODY => $body);
		}
	}

	# complete log file after successful run

	my $eltime	= $self->elapsed_time();
	my $date	= $self->timestamp();

	$self->writelog(BODY => "EXECLOG: elapsedTime $eltime");
	$self->writelog(BODY => "EXECLOG: stopTime $date");
	$self->writelog(BODY => "EXECLOG: exitStatus $self->{'EXIT'}");

	close($self->{'log_fh'});

	# if exit value is 0, ingest log file as a new asset in same namespace
	if($self->{'EXIT'} == 0) {
		# ingest this log file
		my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
		my $asset	= $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_RUNTMAP_LOG;

		my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
		my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
		my $status;
		if(! $assetid) {
			$assetid = $la->create_asset(NAMESPACE => $ns, ASSET => $asset);
		}
		$status = $la->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});
	}

	return;
}


1;

__END__


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

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
