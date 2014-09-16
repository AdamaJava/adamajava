package QCMG::Extract::Torrent;

##############################################################################
#
#  Module:   QCMG::Extract::Torrent.pm
#  Creator:  Lynn Fink
#  Created:  2011-02-28
#
#  This class contains methods for automating the extractions of raw assets
#  from LiveArc to babyjesus
#
##############################################################################


=pod

=head1 NAME

QCMG::Extract::Torrent -- Common functions for extracting raw sequencing files 

=head1 SYNOPSIS

Most common use:
 my $qi = QCMG::Extract::Torrent->new();

=head1 DESCRIPTION

Contains methods for extracting raw sequencing from LiveArc to babyjesus

=head1 REQUIREMENTS

 Exporter
 File::Spec
 POSIX 'strftime'
 QCMG::Automation::LiveArc
 QCMG::Automation::Config

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

use vars qw( $SVNID $REVISION $EXTRACT_ATTEMPTED $EXTRACT_SUCCEEDED);

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 RUN_FOLDER => directory
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

	$self->{'assetid'}		= $options->{'ASSETID'};
	$self->{'extractto'}		= $options->{'EXTRACTTO'};
	$self->{'log_file'}		= $options->{'LOG_FILE'};

	$self->{'hostname'}		= $self->HOSTKEY;

	# set default seq_raw if it is not set by user
	#$self->{'extractto'}		= '/panfs/seq_raw/' if(! $self->{'extractto'});
	$self->{'extractto'}		= $self->SEQ_RAW_PATH if(! $self->{'extractto'});

	# fail if the user does not have LiveArc credentials
	if(! -e $self->LA_CRED_FILE) {
		print STDERR "WARNING: LiveArc credentials file not found!\n";
	}

	$self->init_log_file();

	$self->slide_name();
	$self->run_folder();
	$self->asset_name();

	push @{$self->{'email'}}, $self->DEFAULT_EMAIL;

	$self->{'job_name'}	= 'la_extract_raw';

	# set global flags for checking in DESTROY()  (no/yes)
	$EXTRACT_ATTEMPTED = 'no';
	$EXTRACT_SUCCEEDED = 'no';

	return $self;
}

# Exit codes:
sub EXIT_NO_FOLDER {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		= "\nFailed: No run folder: $self->{'extractto'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 1;
	return 1;
}
sub EXIT_BAD_PERMS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: Bad permissions on $self->{'extractto'}";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 2;
	return 2;
}
sub EXIT_LIVEARC_ERROR {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: LiveArc error";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 9;
	return 9;
}
sub EXIT_BAD_RAWFILES {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: Raw sequencing files are corrupt";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage  $body");

	$self->{'EXIT'} = 13;
	return 13;
}
sub EXIT_MISSING_METADATA {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: Cannot find critical metadata";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 14;
	return 14;
}
sub EXIT_EXTRACT_FAILED {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: LiveArc extraction";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 16;
	return 16;
}
# SET exit(17) = extract not attempted (but only used in DESTROY())
sub EXIT_NO_ASSET {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: LiveArc asset does not exist";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 19;
	return 19;
}
sub EXIT_NO_PBS {
	my $self	= shift @_;
	my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- FAILED";
	my $body	= "RAW EXTRACTION of $self->{'asset_name'} ($self->{'assetid'})";
	$body		.= "\nFailed: Cannot create tmap run.pbs script";
	$body		.= "\nSee log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	$self->send_email(SUBJECT => $subj, BODY => $body);
	$self->writelog(BODY => "EXECLOG: errorMessage $body");

	$self->{'EXIT'} = 99;
	return 99;
}


################################################################################

=pod

B<run_folder()>

Create run folder name from EXTRACTTO and slide_name

Parameters:

Returns:
  scalar run folder name

=cut
sub run_folder {
	my $self	= shift @_;

	# create RUN FOLDER name
	# namespace = /QCMG/runname/
	# assetname = runname_seq_raw
	# run_folder = /panfs/seq_raw/
	my $run_folder		= $self->{'extractto'}."/".$self->{'slide_name'};
	$run_folder 		=~ s/\/\//\//g;

	$self->{'run_folder'}	= $run_folder;

	return $self->{'run_folder'};
}

################################################################################
=pod

B<asset_name()>

Return asset name, as derived from run folder name

Parameters:

Returns:
  scalar asset name

=cut
sub asset_name {
	my $self	= shift @_;

	$self->{'asset_name'} = $self->{'slide_name'}.$self->LA_ASSET_SUFFIX_RAW;

	return $self->{'asset_name'};
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

B<slide_name()> 
  Get slide name from LiveArc asset name 

Parameters:

Returns:
  scalar slide name 

=cut

sub slide_name {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	# get asset information from ID of ingest_log
	# T02_20120318_153_FragBC_ingest_log
	# :namespace "/QCMG_torrent/T02_20120318_153_FragBC"
	# :path "/QCMG_torrent/T02_20120318_153_FragBC/:T02_20120318_153_FragBC_ingest_log"
	# :name "T02_20120318_153_FragBC_ingest_log"

	$self->writelog(BODY => "Getting slide name...");

	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 0);
	my $assetdata		= $la->get_asset(ID => $self->{'assetid'});
	$la 	= undef;

	# keep metadata for parsing later
	$self->{'metadata'}	= $assetdata;

	#$self->writelog(BODY => "Metadata:\n$assetdata");

	my $assetname		= $assetdata->{'NAME'};		# T02_20120318_153_FragBC_ingest_log
	my $namespace		= $assetdata->{'NAMESPACE'};	# /QCMG_torrent/T02_20120318_153_FragBC
	my $slide_name		= $assetname;

	$slide_name		=~ s/(.+)\_ingest\_log/$1/;	# T02_20120318_153_FragBC

	$self->{'slide_name'}	= $slide_name;
	$self->{'namespace'}	= $namespace;

	$self->writelog(BODY => "Slide name:\t".$self->{'slide_name'});
	$self->writelog(BODY => "Asset name:\t".$assetname);
	$self->writelog(BODY => "Namespace:\t".$namespace);

        return $self->{'slide_name'};
}

################################################################################
=pod

B<select_sff()> 
 Decide which sff/sff.zip file to extract (if barcoded run, extract
   barcode.sff.zip)

Parameters:

 Requires:
  $self->{'slide_name'}
  $self->{'namespace'}

Returns:
  ref to hash of asset name/asset ID

 Sets:
  $self->{'sff'} - ref to hash of asset name/asset ID

=cut

sub select_sff {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->writelog(BODY => "Determining barcode status from LIMS...");
	# find out if this is a barcoded run or not; if not, use .sff file; else
	# use .barcode.sff.zip
	$self->{'is_barcoded'}	= 'n';	# set with LIMS

	my $gl = QCMG::DB::Geneus->new;
	$gl->connect;

	my ($data, $slide)	= $gl->torrent_run_by_slide($self->{'slide_name'});

	print Dumper $slide;

	my @barcodes;
	foreach my $field (keys %{$slide}) {
        	foreach my $key (keys %{$slide->{$field}}) {
                        #print STDERR "$key: ".$slide->{$field}->{$key}."\n";
			if($key eq 'barcode' && $slide->{$field}->{$key} =~ /\d/) {

				$self->{'is_barcoded'}	= 'y';

				push @barcodes, $slide->{$field}->{$key};
				$self->writelog(BODY => "BARCODE:\t".$slide->{$field}->{$key});
			}
        	}
	}

	# nobc in barcode UDF field if no barcodes used
	$self->{'barcodes'}	= \@barcodes;

	# set pattern to search assets for; default to barcoded SFF zip file;
	# this pattern should be used like this /$pattern$/ (ends with $pattern)
	#my $pattern		= '.barcode.sff.zip';
	#if($self->{'is_barcoded'}	ne 'y') {
	#	$pattern	= '.sff';
	#}

	# get list of all assets in this namespace to look for desired one
        #/QCMG/S0413_20100831_1_FragBC/:S0413_20100831_1_FragBC_seq_raw  167089
        # key = namespace:asset; value = id
	my $la			= QCMG::Automation::LiveArc->new(VERBOSE => 0);
	my $assets		= $la->list_assets(NAMESPACE => $self->{'namespace'});
	$la 			= undef;

=cut
> ls
    :namespace -path "/QCMG_torrent/T00001_20120503_171"
        :asset -id "58457" -version "2" "T00001_20120503_171.nopd.nobc.wells"
        :asset -id "58460" -version "2" "T00001_20120503_171.nopd.nobc.sff"
        :asset -id "58461" -version "2" "T00001_20120503_171.nopd.IonXpress_007.sff"
        :asset -id "58462" -version "2" "T00001_20120503_171.nopd.IonXpress_008.sff"
        :asset -id "58464" -version "2" "T00001_20120503_171.nopd.IonXpress_010.sff"
        :asset -id "58465" -version "2" "T00001_20120503_171.nopd.IonXpress_012.sff"
        :asset -id "58466" -version "2" "T00001_20120503_171.nopd.nobc.fastq"
        :asset -id "58467" -version "2" "T00001_20120503_171.nopd.IonXpress_007.bam"
        :asset -id "58468" -version "2" "T00001_20120503_171.nopd.IonXpress_008.bam"
        :asset -id "58469" -version "2" "T00001_20120503_171.nopd.IonXpress_010.bam"
        :asset -id "58470" -version "2" "T00001_20120503_171.nopd.IonXpress_012.bam"
        :asset -id "58471" -version "2" "T00001_20120503_171.nopd.nobc.bam"
        :asset -id "58472" -version "1" "T00001_20120503_171.nopd.nobc_plugin_out"
        :asset -id "58473" -version "2" "T00001_20120503_171.nopd.nobc.analysis.pdf"
        :asset -id "58474" -version "1" "T00001_20120503_171_seq_results"
        :asset -id "58475" -version "2" "T00001_20120503_171_ingest_log"
=cut

	my %sff;
	foreach (keys %{$assets}) {
		#print Dumper $_;
		#if(/$pattern$/) {
			#print STDERR "Found asset with pattern $pattern\n";
			#my ($ns, $name)		= split /\:/;
			#$self->{'sff'}		= $name;
			#$self->{'sff_asset_id'}	= $assets->{$_};
			#print STDERR "Asset ID: ".$self->{'sff_asset_id'}."\n";
			#last;
		#}
		if(	(/IonX.+?sff/) || 
			(/nobc\.sff/   && $self->{'is_barcoded'} eq 'n')) {

			$sff{$_}	= $assets->{$_};
			$self->writelog(BODY => "SFF file:\t\t$_");
			$self->writelog(BODY => "SFF file asset ID:\t$assets->{$_}");
		}
	}


	if(scalar(keys %sff) < 1) {
		$self->writelog(BODY => "No SFF or SFF.ZIP asset found in LiveArc");
		exit($self->EXIT_NO_ASSET());
	}

	$self->{'sff'}	= \%sff;

        return (\%sff);
}


################################################################################
=pod

B<check_extract_checksums()> 
 Perform checksums on SFF/SFF.zip and compare them to LiveArc
  metadata
 
Parameters:

Returns:
 void

=cut

sub check_extract_checksums {
        my $self = shift @_;

        #print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

        # parse params
        my $options = {};

        # read params from @_ and put in %options
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{$_[$i]} = $_[($i + 1)];
        }

	$self->writelog(BODY => "---Performing checksums on raw files---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $meta	= $self->{'metadata'};
=cut
       :meta -stime "81609"
            :mf-revision-history -id "1"
                :user -id "47"
                    :domain "infoteam"
                    :name "lfink"
                :type "modify"
            :checksums -id "2"
                :source "orig"
                :checksum
                    :chksum "1487819556"
                    :filesize "7613"
                    :filename "Sample1/results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3_QV.qual"
                :checksum
                    :chksum "149038259"
                    :filesize "3562"
                    :filename "Sample1/results.F1B1/primary.20100610005836273/reads/S0449_20100603_1_Frag_Sample1_F3.csfasta"
=cut

	if($meta->{'META'} !~ /:checksums/) {
		$self->writelog(BODY => "Checksums not available for this asset, skipping check");
		return;
	}

	my @checksums = ($meta->{'META'} =~ /:checksum\n\s+:chksum\s+\"(\d+)\"\n\s+:filesize\s+\"(\d+)\"\n\s+:filename\s+\"(.+?)\"/sg);

	for (my $i=0;$i<=$#checksums;$i+=3) {
		my $cksum = $checksums[$i];
		my $fsize = $checksums[$i+1];
		my $fname = $checksums[$i+2];
	
		#print STDERR "$fname: checksum $cksum, size $fsize\n";
	
		my ($crc, $blocks, $fname) = $self->checksum(FILE => $self->{'run_folder'}."/".$fname);

		$self->writelog(BODY => "LiveArc $fname: checksum $cksum, size $fsize");

		$self->writelog(BODY => "Current $fname: checksum $crc, size $blocks");
	
		#print STDERR "$fname: checksum $crc, size $blocks\n";
	
		unless($cksum == $crc && $fsize == $blocks) {
			$self->writelog(BODY => "Checksum invalid");
			exit($self->EXIT_BAD_RAWFILES());
		}
	}

	$self->writelog(BODY => "Checksums complete");
	$self->writelog(BODY => "END: ".$self->timestamp());

	return;
}

################################################################################
=pod

B<extract()> 
 Generate LiveArc commands and extract the data from LiveArc

Parameters:

Returns:

=cut
sub extract {
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

	$EXTRACT_ATTEMPTED = 'yes';

	$self->writelog(BODY => "---Extracting LiveArc data---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 0);

	# get extract asset command for _seq_raw; 0 if success, 1 if failure
	my $path		= $self->{'extractto'}."/".$self->{'slide_name'};
	$self->writelog(BODY => "Creating output directory: mkdir $path");
	mkdir $path;

	foreach (keys %{$self->{'sff'}}) {

		my $asset	= $_;

		$asset		=~ s/(.+?\:)(.+)/$2/;

		my $file		= $path."/".$asset;
		my $cmd_seq_raw		= $la->extract_asset(ID => $self->{'sff'}->{$_}, PATH => $file, RAW => 1, CMD => 1);
		$self->writelog(BODY => "LiveArc command (extracting SFF): $cmd_seq_raw");
		my $status		= $la->extract_asset(ID => $self->{'sff'}->{$_}, PATH => $file, RAW => 1);
		$self->writelog(BODY => "\tRV: $status");

		# 1 = success
		unless($status == 1) {
			exit($self->EXIT_EXTRACT_FAILED());
		}
	}

	# unzip file if it is zipped
	#if($self->{'sff'} =~ /zip/) {
	#	my $cmd		= qq{unzip $self->{'run_folder'}/$self->{'sff'} -d $self->{'run_folder'}};
	#	$self->writelog(BODY => "Unzipping sff: $cmd");
	#	$status		= system($cmd);
	#
	#	$self->writelog(BODY => "\tRV: $status");
	#}

	#if($status != 0) {
	#	exit($self->EXIT_EXTRACT_FAILED());
	#}
	#else {
		$EXTRACT_SUCCEEDED = 'yes';
	#}
	$self->writelog(BODY => "Extract succeeded");

	$self->writelog(BODY => "Checking checkums...");
	$self->check_extract_checksums();
=cut
	$self->writelog(BODY => "Removing any unnecessary SFFs and renaming necessary ones...");
	# remove unnecessary SFFs (including .zip file) if a barcode.sff.zip is used
	if($self->{'is_barcoded'} eq 'y') {
		my @files	= `find $path -name "*sff"`;
		#print Dumper @files;

		foreach my $f (@files) {
			chomp $f;

			my $matches_barcode	= 'n';
			foreach my $b (@{$self->{'barcodes'}}) {
				my ($v, $d, $file)	= File::Spec->splitpath($f);

				if($file =~ /$b/) {
					$matches_barcode	= 'y';
					$self->writelog(BODY => "Found matching SFF file: $f");

					my $newname	= join '.', $self->{'slide_name'}, 'nopd', $b, "sff";
					$newname	= $d.'/'.$newname;

					my $cmd		= qq{mv $f $newname};
					$self->writelog(BODY => "Renaming barcoded SFF: $cmd");

					my $rv		= `$cmd`;
					$self->writelog(BODY => "RV: $rv");
				}
			}

			if($matches_barcode eq 'n') {
				my $cmd	= qq{mv $f /tmp/};
				$self->writelog(BODY => "Moving unneeded SFF: $cmd");
				system($cmd);
			}
		}
	}
	else {
		my ($v, $d, $f)	= File::Spec->splitpath($self->{'sff'});
		my $newname	= join '_', $self->{'slide_name'}, 'nopd', 'nobc', "sff";
		$newname	= $d.'/'.$newname;

		my $cmd		= qq{mv $f $newname};
		$self->writelog(BODY => "Renaming SFF: $cmd");
		my $rv		= `$cmd`;
		$self->writelog(BODY => "RV: $rv");

		$self->{'sff'}	= $newname;
	}
=cut

	$self->writelog(BODY => "END: ".$self->timestamp());

	return($status);
}

################################################################################
=pod

B<create_run_pbs()> 
 Create run.pbs files for each SFF and ingest them

Parameters:

Returns:

=cut
sub create_run_pbs {
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

	$self->writelog(BODY => "---Creating PBS scripts for tmap---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	my @sffs;
	if($self->{'is_barcoded'} eq 'y') {
		my $cmd		= "find ".$self->{'extractto'}."/".$self->{'slide_name'}."/ -name \"*.sff\"";
		$self->writelog(BODY => "Finding all SFF files: $cmd");
		my @files	= `$cmd`;
		foreach (@files) {
			chomp;
			push @sffs, $_;
		}
	}
	else {
		push @sffs, $self->{'extract_to'}."/".$self->{'slide_name'}."/".$self->{'sff'};
	}

# mapall -n 12 -f /results/referenceLibrary/tmap-f2/ICGC_HG19/ICGC_HG19.fasta -r IonXpress_005_R_2012_03_02_17_59_09_user_T01-138_Analysis_T01-138.sff -Y -v -R LB:ICGC_HG19 -R CN:TorrentServer -R PU:PGM/318B -R ID:H807E -R SM:TargetSeq_109_gene -R DT:2012-03-02T17:59:09 -R PL:IONTORRENT stage1 map1 map2 map3

	my $PBS	= "#!/bin/sh 
#PBS -N tmap
#PBS -q batch
#PBS -r n 
#PBS -l ncpus=8,walltime=10:00:00,mem=44gb
#PBS -m ae 
#PBS -M l.fink\@imb.uq.edu.au

module load tmap

/panfs/share/software/tmap/tmap-20120410/bin/tmap mapall -n 8 -f /panfs/share/genomes/GRCh37_ICGC_standard_v2/GRCh37_ICGC_standard_v2.fa -r <SFF> -Y -v stage1 map1 map2 map3

";

	my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 0);
	my $ns	= $self->LA_NAMESPACE."/".$self->{'slide_name'};

	my @pbs;
	foreach (@sffs) {
		my $pbs	= $PBS;
		$pbs	=~ s/<SFF>/$_/;

		my $pbsfile	= $_.".run.pbs";

		$self->writelog(BODY => "Writing $pbsfile");
		open(FH, ">".$pbsfile) || exit($self->EXIT_NO_PBS());
		print FH $pbs;
		close(FH);
		$self->writelog(BODY => "done");

		# ingest file
		$self->writelog(BODY => "Importing $pbsfile to LiveArc");
		my ($v, $d, $f)	= File::Spec->splitpath($pbsfile);
		my $assetid = $la->laimport_file(NAMESPACE => $ns, ASSET => $f, FILE => $pbsfile);
		$self->writelog(BODY => "asset ID: $assetid");

		push @pbs, $pbsfile;

		if($status !~ /\d+/) {
			$self->writelog(BODY => "Could not ingest run.pbs file $pbsfile");
			exit($self->EXIT_LIVEARC_ERROR());
		}
	}

	$self->{'pbsfiles'}	= \@pbs;
	
	$self->writelog(BODY => "END: ".$self->timestamp());

	return($status);
}

################################################################################
=pod

B<qsub_run_pbs()> 
 qsub run.pbs files for each SFF 

Parameters:

Requires:
 $self->{'pbsfiles'}

Returns:

=cut
sub qsub_run_pbs {
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

	$self->writelog(BODY => "---qsubbing PBS scripts---");
	$self->writelog(BODY => "START: ".$self->timestamp());

	foreach (@{$self->{'pbsfiles'}}) {
		$self->writelog(BODY => "qsubbing $_");
		my $cmd	= qq{qsub $_};
		my $rv	= `$cmd`;
		$self->writelog(BODY => "$rv");

		# HANDLE FAILURE!!!
	}
	
	$self->writelog(BODY => "END: ".$self->timestamp());

	return();
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
	$msg		.= qq{TOOLLOG: RUN_FOLDER $self->{'run_folder'}\n};
	$msg		.= qq{TOOLLOG: DEFAULT_EMAIL }.$self->DEFAULT_EMAIL."\n";

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

	#$self->writelog(BODY => "DESTROY()");

	# override $? with $self->{'EXIT'}

	unless($self->{'EXIT'} > 0) {
		if($EXTRACT_ATTEMPTED eq 'yes' && $EXTRACT_SUCCEEDED eq 'yes') {
			$self->writelog(BODY => "EXECLOG: errorMessage none");

			$self->{'EXIT'} = 0;

			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- SUCCEEDED";
			my $body	= "RAW EXTRACTION of $self->{'slide_name'} successful\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
		elsif($EXTRACT_ATTEMPTED eq 'no') {
			$self->writelog(BODY => "EXECLOG: errorMessage extract not attempted");
	
			$self->{'EXIT'} = 17;
	
			# send success email if no errors
			my $subj	= $self->{'slide_name'}." RAW EXTRACTION -- NOT ATTEMPTED";
			my $body	= "RAW EXTRACTION of $self->{'slide_name'} not attempted\n";
			$body		.= "See log file: ".$self->{'hostname'}.":".$self->{'log_file'};
	
			$self->send_email(SUBJECT => $subj, BODY => $body);
		}
	}

	my $eltime	= $self->elapsed_time();
	my $date	= $self->timestamp();

	$self->writelog(BODY => "EXECLOG: elapsedTime $eltime");
	$self->writelog(BODY => "EXECLOG: stopTime $date");
	$self->writelog(BODY => "EXECLOG: exitStatus $self->{'EXIT'}");

	close($self->{'log_fh'});

	# if exit value is 0, ingest log file as a new asset in same namespace
	if($self->{'EXIT'} == 0) {
		my $ns		= join "/", $self->LA_NAMESPACE, $self->{'slide_name'};
		my $asset	= $self->{'slide_name'}."_extract_log";
		print STDERR "Ingesting log file\n";
		my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 1);
		my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
		my $status;
		if(! $assetid) {
			print STDERR "Importing log file asset\n";
			$assetid = $la->laimport_file(NAMESPACE => $ns, ASSET => $asset, FILE => $self->{'log_file'});
		}
		print STDERR "Updating log file\n";
		$status = $la->update_asset_file(ID => $assetid, FILE => $self->{'log_file'});

		print STDERR "$status\n";
	}

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
