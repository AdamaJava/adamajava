#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qclean.pl
#  Author:   Matthew J Anderson
#  Created:  2012-03-03
#
# This script reports on the status of whether raw data (slide) and its mapped 
# data (mapset), that have been mapped by the production automated pipline, 
#  are ready to be deleted.
#
#  $Id: qclean.pl 4668 2014-07-24 10:18:42Z j.pearson $
#
##############################################################################

use strict;
use warnings;

use IO::File;
use File::Find;
use File::stat;
use Getopt::Long;
use Data::Dumper;
use Pod::Usage;

use Time::HiRes qw( gettimeofday );
use POSIX qw( strftime );

use QCMG::Automation::LiveArc;
use QCMG::DB::Metadata;
use QCMG::FileDir::Finder;

#
# Setup defaults for important variables.
#
use vars qw( $CVSID $REVISION $VERSION $VERBOSE $LOGFH );
	( $REVISION ) = '$Revision: 4668 $ ' =~ /\$Revision:\s+([^\s]+)/;
	( $CVSID ) = '$Id: qclean.pl 4668 2014-07-24 10:18:42Z j.pearson $'
	    	=~ /\$Id:\s+(.*)\s+/;

use vars qw( $METADATA $LIVEARC );
	( $LIVEARC )	= QCMG::Automation::LiveArc->new();
	( $METADATA ) 	= QCMG::DB::Metadata->new();


use vars qw( $DIR_AGE $PANFS $SEQ_RAW $SEQ_MAPPED $PANFS_SEQ_RAW $PANFS_SEQ_MAPPED $MNT_HISEQ $MNT_HISEQNEW $USER_LIST %USERIDS );
	( $DIR_AGE )       		= 7;
	( $PANFS )				= $ENV{'QCMG_PANFS'};
	( $SEQ_RAW )      		= "/mnt/seq_raw";
	( $SEQ_MAPPED )     	= "/mnt/seq_mapped";
	( $PANFS_SEQ_RAW )      = "$PANFS/seq_raw";
	( $PANFS_SEQ_MAPPED )   = "$PANFS/seq_mapped";
	( $MNT_HISEQ )			= "/mnt/HiSeq/runs";
	( $MNT_HISEQNEW )		= "/mnt/HiSeqNew/runs";
	( $USER_LIST )     		= "";


# Configs for checking aligner specific mapped files and LiveArc assets.
our $ALIGNERS = {
	BWA 		=> { mapset_assets => {
							loctation => [ $SEQ_MAPPED, $PANFS_SEQ_MAPPED ], 					
							LA_assests => {},
							local_assets => {}
						}
				},
				
	BioScope 	=> { mapset_assets => { 
							loctations => [ $SEQ_MAPPED, $PANFS_SEQ_MAPPED ],
							LA_assests => {
								bam				=> { LA_prefix => "mapset",	asset => ".notfixed.bam",		asset_type => "file",	LA_namespace => "_mapped" },
								unmapped_bam	=> { LA_prefix => "mapset",	asset => ".unmapped.bam", 		asset_type => "file",	LA_namespace => "_mapped" },
								solidstats		=> { LA_prefix => "mapset",	asset => ".solidstats.xml", 	asset_type => "file",	LA_namespace => "_mapped" },
								bioscope_log	=> { LA_prefix => "mapset",	asset => "_bioscope_log", 		asset_type => "file",	LA_namespace => "_mapped" },
								ingest_log		=> { LA_prefix => "mapset",	asset => "_ingestmapped_log", 	asset_type => "file",	LA_namespace => "_mapped" },
								extract_log		=> { LA_prefix => "mapset",	asset => "_extractmapped_log", 	asset_type => "file",	LA_namespace => "_mapped" }
								},
							local_assets => {
								bam				=> { prefix => "mapset",	asset => ".bam",				asset_type => "file",	assets_location => { Fragment => "/pairing" } },
								unmapped_bam	=> { prefix => "mapset",	asset => ".unmapped.bam", 		asset_type => "file",	assets_location => { Fragment => "/pairing" } },
								solidstats		=> { prefix => "mapset",	asset => ".solidstats.xml", 	asset_type => "file",	assets_location => { Fragment => "" } },
								bioscope_log	=> { prefix => "mapset",	asset => "_bioscope_log", 		asset_type => "file",	assets_location => { Fragment => "" } },
								ingest_log		=> { prefix => "mapset",	asset => "_ingestmapped_log", 	asset_type => "file",	assets_location => { Fragment => "" } },
								extract_log		=> { prefix => "mapset",	asset => "_extractmapped_log", 	asset_type => "file",	assets_location => { Fragment => "" } }
							}
						}
				},
				
	LifeScope	=> { mapset_assets => { 
			 				loctations => [ "$PANFS/lifescope_results/projects" ], 
							LA_assests => {},
							local_assets => {}
						}
				}
};

# Configs for checking platform specific raw data files and LiveArc assets.
our $PLATFORMS  = {
	
	HISEQ	=> { raw_assets	=> {		
						locations	=> [ $MNT_HISEQ  ],
						LA_assests  => { 
							interop			=> { prefix => "slide",		asset => "_interop", 			asset_type => "dir",		LA_namespace => "" },
							SampleSheet		=> { prefix => "slide",		asset => "_SampleSheet.csv", 	asset_type => "file",		LA_namespace => "" },
							RunInfo 		=> { prefix => "slide",		asset => "_RunInfo.xml", 		asset_type => "file",		LA_namespace => "" },
							runParameters	=> { prefix => "slide",		asset => "_runParameters.xml", 	asset_type => "file",		LA_namespace => "" },
							},
						local_assets => {
							interop			=> { prefix => "",			asset => "InterOp", 			asset_type => "dir",	 asset_location => "" },
							SampleSheet		=> { prefix => "",			asset => "SampleSheet.csv", 	asset_type => "file",	 asset_location => "Data/Intensities/BaseCalls" },
							RunInfo			=> { prefix => "",			asset => "RunInfo.xml", 		asset_type => "file",	 asset_location => "" },
							runParameters	=> { prefix => "",			asset => "runParameters.xml",	asset_type => "file",	 asset_location => "" },
						} 
					},
				
				slide_assets =>{
						locations	=> [ $SEQ_RAW, $PANFS_SEQ_RAW ],	
						LA_assests  => { 
							casava 			=> { prefix => "slide",		asset => "_casava", 			asset_type => "dir",		LA_namespace => "" },
							fastq_1 		=> { prefix => "mapset", 	asset => ".1.fastq.gz", 		asset_type => "file",		LA_namespace => "" },
							fastq_2			=> { prefix => "mapset", 	asset => ".2.fastq.gz", 		asset_type => "file",		LA_namespace => "" }
						},
						local_assets => {
							casava 			=> { prefix => "",			asset => "Unaligned", 			asset_type => "dir",	 asset_location => "Unaligned" },
							fastq_1			=> { prefix => "mapset", 	asset => ".1.fastq.gz", 		asset_type => "file",	 asset_location => "" },						
							fastq_2			=> { prefix => "mapset", 	asset => ".2.fastq.gz", 		asset_type => "file",	 asset_location => "" }
						} 
					},	
						
				},
				
	#MISEQ	=> { raw_assets	=> {		
	#					locations	=> [ $MNT_HISEQ, "/mnt/HiSeqNew" ],	
	#					LA_assests  => { 
	#						interopt		=> { prefix => "slide",		asset => "_interopt", 			asset_type => "dir",		LA_namespace => "" },
	#						SampleSheet		=> { prefix => "slide",		asset => "_SampleSheet.csv", 	asset_type => "file",		LA_namespace => "" },
	#						RunInfo 		=> { prefix => "slide",		asset => "_RunInfo.xml", 		asset_type => "file",		LA_namespace => "" },
	#						runParameters	=> { prefix => "slide",		asset => "_runParameters.xml", 	asset_type => "file",		LA_namespace => "" },
	#						casava 			=> { prefix => "slide",		asset => "_casava", 			asset_type => "dir",		LA_namespace => "" },
	#						},
	#					local_assets => {
	#						interopt		=> { prefix => "",			asset => "InterOp", 			asset_type => "dir",	 asset_location => "" },
	#						SampleSheet		=> { prefix => "",			asset => "SampleSheet.csv", 	asset_type => "file",	 asset_location => "Data/Intensities/BaseCalls" },
	#						RunInfo			=> { prefix => "",			asset => "RunInfo.xml", 		asset_type => "file",	 asset_location => "" },
	#						runParameters	=> { prefix => "",			asset => "runParameters.xml",	asset_type => "file",	 asset_location => "" },
	#						casava 			=> { prefix => "slide",		asset => "_casava", 			asset_type => "dir",	 asset_location => "" },
	#					} 
	#				},
	#			
	#			slide_assets =>{
	#					locations	=> [ $SEQ_RAW, $PANFS_SEQ_RAW ],	
	#					LA_assests  => { 
	#						fastq_1 		=> { prefix => "mapset", 	asset => ".1.fastq.gz", 		asset_type => "file",		LA_namespace => "" },
	#						fastq_2			=> { prefix => "mapset", 	asset => ".2.fastq.gz", 		asset_type => "file",		LA_namespace => "" }
	#					},
	#					local_assets => {
	#						fastq_1			=> { prefix => "mapset", 	asset => ".1.fastq.gz", 		asset_type => "file",	 asset_location => "" },						
	#						fastq_2			=> { prefix => "mapset", 	asset => ".2.fastq.gz", 		asset_type => "file",	 asset_location => "" }						
	#					} 
	#				},	
	#					
	#			},
							
	XL5500	=> { slide_assets 	=> {
						locations	=> [ $SEQ_RAW, $PANFS_SEQ_RAW ],	
						LA_assests 	=> {
							xsq	=> { prefix => "slide_phydiv_nobc", 	asset => ".xsq",	asset_type => "file",		LA_namespace => "" }
						
						},
						local_assets => {
							xsq => { prefix => "slide_phydiv_nobc", 	asset => ".xsq",	asset_type => "file",		asset_location => "" }
						}
					}
					
				},
				
	SOLID4	=> { slide_assets 	=> {	
						locations	=> [ $SEQ_RAW, $PANFS_SEQ_RAW ],
						LA_assests => {
							solidstats			=> { prefix => "slide", 	asset => ".solidstats.xml",	asset_type => "file",		LA_namespace => "" },
						},
						local_assets => {
							solidstats			=> { prefix => "slide",  	asset => ".solidstats.xml",	asset_type => "file",	 asset_location => "" },
						}
					}
					
				}
	#TORRENT		=> { raw_location	=> "", 		 	assests => {}}
};



# These are the directories and files we wish to keep when cleaning up HiSeq raw data
our $CLEANUP_KEEP = {
	HISEQ	=> { raw_assets	=>	{
						dir_cleaned => [
									"Data/",
									"InterOp/",
									"First_Base_Report.htm",
									"RunInfo.xml",
									"runParameters.xml"
									######### Extras to check on #########
									#"Recipe/",
									#"Logs/",
									#"SampleSheet.csv",
									#"Config/",
									],
						dir_keeps 	=> [	
									"Data/Status_Files/",
									"Data/reports/",
									"Data/Status.htm",
									"InterOp/",
									"First_Base_Report.htm",
									"RunInfo.xml",
									"runParameters.xml",
									"analysisError.txt" 
									]
								}
				}
};








MAIN: {
    my $logdir     	= $ENV{'QCMG_HOME'}."/job_cleanup/qClean_logs";
    
	# Options
	my @users       = ("lfink", "production");
    my $mapped_dir  = "";
    my $raw_dir     = "";
	my @slides		= ();
	my @mapsets		= ();
	my $aligner		= ""; 
	
	   $VERBOSE     = 0; 
       $VERSION     = 0; 
    my $help        = 0; 
    my $man         = 0; 
                                
    # Print usage message if no arguments supplied
	#DEBUG    pod2usage(1) unless (scalar @ARGV > 0);

    # Use GetOptions module to parse commandline options

    my $results = GetOptions (
            'd|dirage=i'			=> \$DIR_AGE,       #-a
            'u|user=s'  			=> \@users,			# -u
            'l|logdir=s'			=> \$logdir,       	# -l
			's|slide=s'				=> \@slides,		# -s
			'm|mapset=s'			=> \@mapsets,		# -m
			'a|aligner'				=> \$aligner,		# -a
			
            'v|verbose+'           	=> \$VERBOSE,       # -v
            'version!'             	=> \$VERSION,       # --version
            'h|help|?'             	=> \$help,          # -?
            'man'                	=> \$man            # -m
	);

    pod2usage(1) if $help;
    pod2usage(-exitstatus => 0, -verbose => 2) if $man;

    if ($VERSION) {
        print "$CVSID\n";
        exit;
    }
    
	# Build user list for finds
    foreach (@users){
        if ( $USER_LIST eq "" ){ $USER_LIST .= "-user $_";
        }else{ $USER_LIST .= " -o -user $_";
        }
    }
    
    if ( -d $logdir ) {
	    my ($seconds, $microseconds) = gettimeofday;
	    my $datatime = strftime( q/%Y%m%d_%H%M%S/, localtime($seconds)) .
	              sprintf("%03d", $microseconds/1000); 
   	 	my $logfile = "$logdir/qClean_".$datatime.".log";
   		$LOGFH = IO::File->new( $logfile, 'w' );
    	$LOGFH->autoflush();
       	logdie( "Cannot open log file $logfile for writing: $!" )
        	unless (defined $LOGFH);
			
		print "Report in $logfile\n\n";
   	}

	logprint( 1, "\nqclean.pl  v$REVISION  [" . localtime() . "]\n",
             "   verbose       $VERBOSE\n\n" );

	
	print "Looking for Raw directories...\n";
	# Checking raw data
	#my $platform_rawdata_location = platform_rawdata_location();
	my $platform_rawdata_location = platform_data_loction ( 'raw_assets' );
	my $directory_raws = find_raws($platform_rawdata_location);
	
	print "Finding Slide info...\n";
	find_slide_info( $directory_raws );
	print "Checking Raw assests...\n";
	check_raw_assets( $directory_raws );
	#check_slide_assets ( $directory_raws );
	print "Comparing Raw assests...\n";
	compare_assets ( $directory_raws );
	
	#print Dumper \$directory_raws;
	
	print_ready_directories($directory_raws);
	
	#foreach my $path (keys %$directory_raws) {
	#	my $slide_list = $directory_raws->{$path};
	#	foreach my $slide ( keys %$slide_list ) { 
	#		print "$path/$slide/\t";
	#		print $slide_list->{$slide}{status};
	#		print "\n";
	#	}
	#}
	
	# Checking slide data 
	#my $platform_slidedata_location = platform_slidedata_location();
#	my $platform_slidedata_location = platform_data_loction ( 'slide_assets' );
	#print Dumper \$platform_slidedata_location;
	
#	my $directory_slides = find_production_mapped_slides( $platform_slidedata_location );
	#print Dumper \$directory_slides;
	
#	find_slide_info( $directory_slides );
	#print Dumper \$directory_slides;
		
	#check_slide_assets ( $directory_slides );
	#print Dumper \$directory_slides;
	
	#checking mapped data
#	my $aligner_mappeddata_location = aligner_mappeddata_location();
#	my $directory_mapasets = find_production_mapped_mapsets( $aligner_mappeddata_location );
#	$directory_mapasets = find_mapset_info( $directory_mapasets );
	#print Dumper $directory_mapasets;
	
	# TODO Check assest of slides
	# TODO check if dirs exist for mapsets.
	# TODO check asset of mapsets of aligners
	
	#my $checked_slides = check_seqslide( \@slides );
	#print Dumper $checked_slides;
	#check_seqmapped( $checked_slides, $aligner );
	
	
	
}



# If the current level of $VERBOSE is >= the verbosity level (vlvl) for
# this message then it should be output.  The output will be written to
# to the logfile if one was opened otherwise it will go to STDOUT.  This
# routine should probably replace all uses of warn.

sub logprint {
    my $vlvl = shift;  # verbose level
    if ($VERBOSE >= $vlvl) {
        if (defined $LOGFH) {
            print $LOGFH $_ foreach @_;
        }
        else {
            print STDOUT $_ foreach @_;
        }
    }
}


# This routine handles die level events including standardizing the
# output format, printing it to the logfile and then actually die'ing.
sub logdie {
    if ($LOGFH) {
        print $LOGFH "\nFATAL ERROR: ";
        print $LOGFH $_ foreach @_;
        die "FATAL ERROR: ", @_,
            "Unable to continue processing until error is fixed.\n";
    }
    else {
        print STDOUT "\nERROR: ";
        print STDOUT $_ foreach @_;
        die "Unable to continue processing until error is fixed.\n";
    }
}

sub platform_to_LiveArc_Namespace {
	my $platform = shift;
	
		if ( $platform eq "HISEQ") { 	return "QCMG_hiseq";
	}elsif ( $platform eq "MISEQ"){		return "QCMG_miseq";
	}elsif ( $platform eq "TORRENT"){	return "QCMG_torrent";
	}elsif ( $platform eq "SOLID4"){	return "QCMG_solid";
	}elsif ( $platform eq "XL5500"){	return "QCMG_5500";
	}
	return "";	
}



sub platform_data_loction {
	my $assets_for  = shift;	# "raw_assets" or "slide_assets"
	my $platform	= shift;	# Optional
	
	my $directories = {};
		
	if ( $platform and exists $PLATFORMS->{$platform} and exists $PLATFORMS->{$platform}{$assets_for} ) {
		my $asset_location = $PLATFORMS->{$platform}{$assets_for}{locations};
		foreach my $location (@$asset_location){
			$directories->{$location} = [ $platform ];
		}
		
	}else{
		foreach $platform (keys %$PLATFORMS) {
			if ( exists $PLATFORMS->{$platform}{$assets_for}) {
				my $asset_location = $PLATFORMS->{$platform}{$assets_for}{locations};
				foreach my $location ( @$asset_location ){		
					if ( exists $directories->{ $location } ) {
						my $platforms = $directories->{$location};
						push ( @$platforms, $platform );
						$directories->{$location} = $platforms;
					}else{
						$directories->{$location} = [ $platform ];
					}
				} 
			}
		}			
	}
	
	return $directories;
}







sub aligner_mappeddata_location {
	my $aligner		= shift;
	
	my $mapped_directories = {};
	
	if ( $aligner and exists $ALIGNERS->{$aligner}) {
		my $output_loctation = $ALIGNERS->{$aligner}{output_loctations};
		foreach my $location (@$output_loctation){
			$mapped_directories->{$output_loctation} = [ $aligner ];
		}
	}else{
		foreach $aligner (keys %$ALIGNERS) {
			my $output_loctation = $ALIGNERS->{$aligner}{output_loctations};
			foreach my $location (@$output_loctation){
				if ( exists $mapped_directories->{$location} ) {
					my $aligners = $mapped_directories->{$location};
					push ( @$aligners, $aligner );
					$mapped_directories->{$location} = $aligners;
				}else{
					$mapped_directories->{$location} = [ $aligner ];
				}
			}
		 } 
	}
	
	return $mapped_directories;
}




sub raw_dir_cleaned {
	my $platform	= shift;	# 'HISEQ'
	my $assets		= shift; 	# 'raw_assets'
	my $root_dir	= shift;
	my $raw_dir		= shift;
	
	my $keepers = $CLEANUP_KEEP->{$platform}{$assets}{dir_cleaned};
	
	my $some_dir = "$root_dir/$raw_dir";	
	my @dir_listing = <$some_dir/*>;
	
	my %dir_contents = map { $_ => '' } @dir_listing;
	foreach my $key ( keys %dir_contents ){
		%dir_contents->{$key.'/'} = delete %dir_contents->{$key} if -d $key;
	}
	
	foreach my $pattern ( @$keepers ){
		foreach my $key ( keys %dir_contents ){
			if ($key =~ /$pattern/) {
				#print "$pattern = $key\n"; # if $key =~ /$pattern/;
				%dir_contents->{$key} = $pattern if ! %dir_contents->{$key};
			}			
		}
	}
	#print Dumper \%dir_contents;
	
	# if there are paths that have not match a kept pattern,
	# then the directory has not been clened.
	#print "$raw_dir\n";
	foreach my $path (keys %dir_contents) {
    	#print "$path => ".%dir_contents->{$path}." \n"
		print "$path\n" if ! %dir_contents->{$path};
		#return 0 if ! %dir_contents->{$path};
	}
	foreach my $path (keys %dir_contents) {
		return 0 if ! %dir_contents->{$path};
	}
	
	
	return 1;	
}


sub list_directory_contents {
    my $dir = shift;

    my $finder = QCMG::FileDir::Finder->new( verbose => 0 );
    my @directories = $finder->find_directory( $dir, '.');
    my @files   = $finder->find_file( $dir, '.' );
    #combining the list of directories and files into 1 list.
    push( @directories, @files );
    #my @contents = sort @directories;
    my %contents = map { $_ => "" } sort @directories;
	    
    return \%contents;
}




sub find_raws {
    my	$platform_rawdirs 	= shift;
	my	$directory_raws 		= {};
	our ( $DIR_AGE, $USER_LIST );
	
	foreach my $raw_dir (keys %$platform_rawdirs ) {
		print "Finding slides in $raw_dir for users where directory modification time is greater then $DIR_AGE days.\n";	
		#my $cmd = qq( find $raw_dir -maxdepth 1 -type d -mtime +$DIR_AGE -exec basename {} \\; );
		my $cmd = qq( find $raw_dir -maxdepth 1 -type d -mtime +$DIR_AGE ! -name "*_M*" -exec basename {} \\; );
		print $cmd."\n";
		my @cmd_results = `$cmd`; 
		chomp ( @cmd_results );	
		
		my @directories = sort @cmd_results;
		$directory_raws->{$raw_dir} = \@directories;
	}
	
	foreach my $dir ( keys %$directory_raws ) {
		my $dir_raws = $directory_raws->{$dir};
		my @dir_dirty_raws = ();
		my $dir_count = scalar(@$dir_raws);
		for (my $i = 0; $i < $dir_count; $i++) {
			print "$i - ".@$dir_raws[$i]."\n";
			if ( raw_dir_cleaned( 'HISEQ', 'raw_assets', $dir, @$dir_raws[$i] ) ){
				print "  - Cleaned \n\n";
			}
			else{
				push ( @dir_dirty_raws,  @$dir_raws[$i] );
				print "  - DIRTY \n\n";
			}
		}
		my $dir_dirty_count = scalar(@dir_dirty_raws);
		print "$dir has $dir_count slides. of which $dir_dirty_count have not been cleaned\n";		
		
		$directory_raws->{$dir} = \@dir_dirty_raws;
	}
	
	#foreach my $dir ( keys %$directory_raws ) {
	#	my $dir_raws = $directory_raws->{$dir};
	#	foreach my $raws ( @$dir_raws) {
	#		print "$dir/$raws  - Not Cleaned \n";
	#	}	
	#}
		
	return \%$directory_raws;
}

 
# Find list of slides owned by production users and has a _bioscope_ini dir to identify that this has been proceesed
# by the Automation pipline. Slide directories must be greater then X day old to give time for mapping of mapsets.
sub find_production_mapped_slides {
    my	$platform_slidedirs 	= shift;
	my	$directory_slides 		= {};
	our ( $DIR_AGE, $USER_LIST );
	
	foreach my $slide_dir (keys %$platform_slidedirs ) {
		print "Finding slides in $slide_dir for users $USER_LIST where directory modification time is greater then $DIR_AGE days.\n";	
		my $cmd = qq( find $slide_dir -maxdepth 1 -type d \\( $USER_LIST \\) -mtime +$DIR_AGE -exec basename {} \\; );
		#print $cmd;
		my @cmd_results = `$cmd`; 
		chomp ( @cmd_results );	
		
		$directory_slides->{$slide_dir} = \@cmd_results;
	}

	return \%$directory_slides;
}

sub find_production_mapped_mapsets {
    my	$platform_mapsetdirs 	= shift;
	my	$directory_mapsets 		= {};
	our ( $DIR_AGE, $USER_LIST );
	
	foreach my $mapset_dir (keys %$platform_mapsetdirs ) {
		print "Finding slides in $mapset_dir for users $USER_LIST where directory modification time is greater then $DIR_AGE days.\n";	
		my $cmd = qq( find $mapset_dir -maxdepth 1 -type d \\( $USER_LIST \\) -mtime +$DIR_AGE -exec basename {} \\; );
		#print $cmd;
		my @cmd_results = `$cmd`; 
		chomp ( @cmd_results );	
		
		$directory_mapsets->{$mapset_dir} = \@cmd_results;
	}

	return $directory_mapsets;
}




sub find_slide_info {
    my	$directory_slides 	= shift;
	
	foreach my $directory (keys %$directory_slides) {
		my $slides = $directory_slides->{$directory};
		my $hashed_directory_slides  = {};
		foreach my $slidename (@$slides){
			$hashed_directory_slides->{$slidename} = {};
			
			$hashed_directory_slides->{$slidename}{platform} = $METADATA->slide_to_platform( $slidename );
			# slide_to_platform() can be called without calling find_metadata as the database is not used. - Sneaky hey!
			
			if ( $METADATA->find_metadata("slide", $slidename) ){	
				$hashed_directory_slides->{$slidename}{mapsets} = $METADATA->slide_mapsets( $slidename );
			}else{
				print "No information found for $slidename !\n";
			}
		}
		$directory_slides->{$directory} = $hashed_directory_slides;
	}
}



sub find_mapset_info {
    my	$directory_mapsets 	= shift;
	
	foreach my $directory (keys %$directory_mapsets) {
		my $mapsets = $directory_mapsets->{$directory};
		my $hashed_directory_mapsets  = {};
		foreach my $mapsetname (@$mapsets){
			$hashed_directory_mapsets->{$mapsetname} = {};
			if ( $METADATA->find_metadata("mapset", $mapsetname) ){	
				$hashed_directory_mapsets->{$mapsetname}{platform} 				= $METADATA->mapset_to_platform( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{library_type} 			= $METADATA->library_type( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{capture_kit} 			= $METADATA->capture_kit( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{gff_file} 				= $METADATA->gff_file( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{sequencing_type} 		= $METADATA->sequencing_type( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{aligner} 				= $METADATA->aligner( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{species_reference_genome} = $METADATA->species_reference_genome( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{genome_file} 			= $METADATA->genome_file( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{alignment_required} 	= $METADATA->alignment_required( $mapsetname );
				$hashed_directory_mapsets->{$mapsetname}{failed_qc} 			= $METADATA->failed_qc( $mapsetname );
			}else{
				print "No information found for $mapsetname !\n";
			}
		}
		$directory_mapsets->{$directory} = $hashed_directory_mapsets;
	}
	return $directory_mapsets;
}


# Create "<slide>_<phydiv>_nobc"
sub mapsets_to_slide_phydiv_nobcs {
	my $mapsets = shift;	
	my $slide_phydiv_nobc = [];
	
	foreach my $mapset ( @$mapsets ) {
	    my @sections = split /\./, $mapset;
	    my $slide_id = $sections[0];
		my $physical_div = $sections[1];
		if( ! grep ( /^$slide_id\.$physical_div\.nobc/, @$slide_phydiv_nobc) ) {
			push( @$slide_phydiv_nobc, "$slide_id.$physical_div.nobc" );
		}
	}
	
	return $slide_phydiv_nobc;
}


sub print_ready_directories {
	my $directories = shift;  

	foreach my $path (keys %$directories) {
		my $directory_list = $directories->{$path};
		foreach my $directory ( keys %$directory_list ) { 
			if ( $directory_list->{$directory}{status} eq "passed" ){
				print "$path/$directory/\t";
				print $directory_list->{$directory}{status};
				print "\n";
			}
		}
	}
	
}




sub local_asset_checks {
	my $info 			= shift;			# $directory_raws->{$directory}{$raw}{platform};
	my $asset_category 	= shift; 	# "raw_asset", "slide_assets", "mapped_asset"
	my $directory		= shift;
	my $slidemapset		= shift;
	my $asset 			= shift;
	
	my $platform 		= $info->{platform};
	my $prefix 			= $PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{prefix};
	my $asset_type 		= $PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset_type};
	my $asset_location	= $PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset_location};
	my $assets 			= {};				
	my $assets_info		= {};	
	
	## Building list of assets to check.			
	if ( $prefix eq "" ) {					
		#push ( @$assets, $PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset} );
		$assets->{$asset} = $PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset};
		
	} elsif ( $prefix eq "slide" ) {
    	my @sections = split /\./, $slidemapset;			
		my $slide = $sections[0];		
		#push ( @$assets, $slide.$PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset});
		$assets->{$asset} = $slide.$PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset};
			
	} elsif	( $prefix eq "mapset"){			
		my $mapsets	= $info->{mapsets};
		foreach my $mapset ( @$mapsets ) {	
			$assets->{$mapset.'_'.$asset} = $mapset.$PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset};
			#ush ( @$assets, $mapset.$PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset} ); 
		}
		
	} elsif	( $prefix eq "slide_phydiv_nobc"){	
		my $mapsets	= $info->{mapsets};
		my $slide_phydiv_nobc = mapsets_to_slide_phydiv_nobcs( \@$mapsets); 
		foreach my $slide_phydiv ( @$slide_phydiv_nobc) {
			$assets->{$slide_phydiv.'_'.$asset} = $slide_phydiv.$PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset};	
			#push ( @$assets, $slide_phydiv.$PLATFORMS->{$platform}{$asset_category}{local_assets}{$asset}{asset} );
		}
	}
				
	foreach my $asset_group (keys  %$assets) {				
		my $asset_name = $assets->{$asset_group};
		#my $raw_location = "$directory/$raw";
		my $local_location = "$directory/$slidemapset/$asset_location";
	    # trimming trailing slash from directory paths.
	    $local_location =~ s[/$][];
					
		my $local_asset_path = "$local_location/$asset_name";
		my $local_size = -1;
		my $local_mtime	= -1;
		
		# ASSET is a File
		if ($asset_type eq "file") {
			if ( -e $local_asset_path ) {
				my $filestats = stat($local_asset_path);
				$local_size = $filestats->size;
                $local_mtime = $filestats->mtime;
			}else{
				print "[ WARNING ] - file $local_asset_path does not exist !\n";
			}
																	
		# ASSET is a Directory
		} elsif ($asset_type eq "dir"){
			if ( -d $local_asset_path ) {
				my $filestats = stat($local_asset_path);
				$local_size = $filestats->size;
                $local_mtime = $filestats->mtime;
			}else{
				print "[ WARNING ] - directory $local_asset_path does not exist !\n";
			}
		}			
		
		$assets_info->{$asset_group}{local_location} = $local_asset_path;
		$assets_info->{$asset_group}{local_size} 	= $local_size;
		$assets_info->{$asset_group}{local_mtime} 	= $local_mtime;
	}
	
	return \%$assets_info; 	
	
}


sub LiveArc_asset_checks {
	my $info 			= shift;			# $directory_raws->{$directory}{$raw}{platform};
	my $asset_category 	= shift; 	# "raw_asset", "slide_assets", "mapped_asset"
	my $directory		= shift;
	my $slidemapset		= shift;
	my $asset 			= shift;
	
	my $platform 		= $info->{platform};
	my $LA_root_namespace = platform_to_LiveArc_Namespace($platform);
	my $LA_namespace 	= $PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{LA_namespace};
	my $prefix 			= $PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{prefix};
	my $asset_type 		= $PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset_type};
	my $asset_location	= $PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset_location};
	my $assets 			= {};				
	my $assets_info		= {};	
	
	## Building list of assets to check.			
	if ( $prefix eq "" ) {					
		$assets->{$asset} = $PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset};
		#push ( @$assets, $PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset} );
			
	} elsif ( $prefix eq "slide" ) {
    	my @sections = split /\./, $slidemapset;			
		my $slide = $sections[0];
		$assets->{$asset} = $slide.$PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset};
		#push ( @$assets, $slide.$PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset});
			
	} elsif	( $prefix eq "mapset"){			
		my $mapsets	= $info->{mapsets};
		foreach my $mapset ( @$mapsets ) {	
			$assets->{$mapset.'_'.$asset} = $mapset.$PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset};
			#push ( @$assets, $mapset.$PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset} ); 
		}
		
	} elsif	( $prefix eq "slide_phydiv_nobc"){	
		my $mapsets	= $info->{mapsets};
		my $slide_phydiv_nobc = mapsets_to_slide_phydiv_nobcs( \@$mapsets); 
		foreach my $slide_phydiv ( @$slide_phydiv_nobc) {	
			$assets->{$slide_phydiv.'_'.$asset} = $slide_phydiv.$PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset};
			#push ( @$assets, $slide_phydiv.$PLATFORMS->{$platform}{$asset_category}{LA_assests}{$asset}{asset} );
		}
	}
	
	foreach my $asset_group (keys  %$assets) {				
		my $asset_name = $assets->{$asset_group};
	
		#print "Asset $asset - Name: $asset_name\n";
		my $LA_location = "/$LA_root_namespace/$slidemapset/$LA_namespace";
		$LA_location =~ s[/$][];
		my $LA_asset_path = "$LA_location/$asset_name";
		
		my $assetid = -1;
		my $LA_asset_size = -1;
		my $LA_asset_mtime = -1;
		my $LA_asset_source = "";
		
		# ASSET is a File
		if ( $asset_type eq "file") {
			$assetid = $LIVEARC->asset_exists(NAMESPACE => $LA_location, ASSET => $asset_name );
			if ($assetid) {
				my $LA_asset_details = $LIVEARC->get_asset(ID => $assetid);
				my $LA_asset_meta = $LA_asset_details->{META};
					
				$LA_asset_meta	=~ /:filesize\s+\"(\d+)\"/;
				$LA_asset_size = $1;
				if ($LA_asset_size) {
					#print "$asset_name - Size: $LA_asset_size ";
				}else{
					my $LA_asset_content = $LA_asset_details->{CONTENT};
					$LA_asset_content	=~ /:size -h \".+\" \"(\d+)\"/;
					$LA_asset_size = $1;
					if ($LA_asset_size) {
						#print "$asset_name - Size: $LA_asset_size ";
					}else{
						#print "$asset_name - No Size";
					}
				}				
					
				$LA_asset_meta	=~ /:filename\s+\"(.+)\"/;
				$LA_asset_source = $1;						
				if ($LA_asset_source) {
					#print " - Path: $LA_asset_path\n";
				}else{
					$LA_asset_meta =~ /:source \"file:(.+)\"/;
					$LA_asset_source = $1;
					if ($LA_asset_source) {
						#print " - Path: $LA_asset_path\n";
					}else{
						my $LA_asset_content = $LA_asset_details->{CONTENT};
						$LA_asset_content =~ /:source \"file:(.+)\"/;
						$LA_asset_source = $1;						
						if ($LA_asset_source) {
							#print " - Path: $LA_asset_path\n";
						}else{
							#print " - No Path \n";
						}
					}
				}
					}else{
				print "[ WARNING ] - asset $LA_asset_path does not exist !\n";
				$LA_asset_path = "";
			}
			
		# ASSET is a Directory
		} elsif ($asset_type eq "dir"){
			$assetid = $LIVEARC->asset_exists(NAMESPACE => $LA_location, ASSET => $asset_name );
			if ($assetid) {
				my $LA_asset_details = $LIVEARC->get_asset(ID => $assetid);
				my $LA_asset_meta = $LA_asset_details->{MTIME};
				#print Dumper $LA_asset_meta;
							
				#:mtime -millisec "1347543191553" "13-Sep-2012 23:33:11"
				$LA_asset_meta	=~ /-millisec\s+\"(\d+)\d{3}\"/;
				$LA_asset_mtime = $1;
		
				if (! $LA_asset_mtime) {
					$LA_asset_mtime = -1;
					#print "$LA_asset_mtime\n";
				}
							
							
			}else{
				print "[ WARNING ] - directory assest $LA_asset_path does not exist !\n";
				$LA_asset_path = "";
			}
		
		}
		
		
		
		$assets_info->{$asset_group}{LA_location}	= $LA_asset_path;
		$assets_info->{$asset_group}{LA_size}		= $LA_asset_size;
		$assets_info->{$asset_group}{LA_mtime}		= $LA_asset_mtime;
		$assets_info->{$asset_group}{LA_source}		= $LA_asset_source;
		$assets_info->{$asset_group}{LA_asset_id}	= $assetid;	

	}
	
	return \%$assets_info;
#	
#	
#	my $LA_prefix 		= $PLATFORMS->{$platform}{raw_assets}{LA_assests}{$asset}{prefix};
#	my $LA_asset		= $PLATFORMS->{$platform}{raw_assets}{LA_assests}{$asset}{asset};
#	my $LA_asset_type 	= $PLATFORMS->{$platform}{raw_assets}{LA_assests}{$asset}{asset_type};
#	my $LA_namespace 	= $PLATFORMS->{$platform}{raw_assets}{LA_assests}{$asset}{LA_namespace};
#	my $LA_assets 		= [];
#				
#	#print "Asset $asset - Name: $LA_asset - Prefix: $LA_prefix \n";				
#				
#	if ( $LA_prefix eq "" ) {					
#			push ( @$LA_assets, $PLATFORMS->{$platform}{raw_assets}{LA_assests}{$asset}{asset} );
#	} elsif ( $LA_prefix eq "slide" ) {		
#			push ( @$LA_assets, $raw.$PLATFORMS->{$platform}{raw_assets}{LA_assests}{$asset}{asset});
#	} elsif	( $LA_prefix eq "mapset"){			
#		foreach my $mapset ( @$mapsets ) {	
#			push ( @$LA_assets, $mapset.$PLATFORMS->{$platform}{raw_assets}{LA_assests}{$asset}{asset} ); 
#		}
#	} elsif	( $LA_prefix eq "slide_phydiv_nobc"){	
#		foreach my $slide_phydiv ( @$slide_phydiv_nobc) {	
#			push ( @$LA_assets, $slide_phydiv.$PLATFORMS->{$platform}{raw_assets}{LA_assests}{$asset}{asset} );
#		}
#	}
#				
#				
#				
#				
#	foreach my $asset_name ( @$LA_assets ) {
#		#print "Asset $asset - Name: $asset_name\n";
#		my $assetid = -1;
#		my $LA_asset_size = -1;
#		my $LA_asset_mtime = -1;
#		my $LA_asset_source = "";
#					
#		my $LA_location = "/$LA_root_namespace/$raw/$LA_namespace";
#		$LA_location =~ s[/$][];
#		my $LA_asset_path = "$LA_location/$asset_name";
#		#print "LA_asset_path: $LA_asset_path\n";
#
#		# ASSET is a File
#		#
#		if ( $LA_asset_type eq "file") {
#			$assetid = $LIVEARC->asset_exists(NAMESPACE => $LA_location, ASSET => $asset_name );
#			if ($assetid) {
#				my $LA_asset_details = $LIVEARC->get_asset(ID => $assetid);
#				my $LA_asset_meta = $LA_asset_details->{META};
#					
#				$LA_asset_meta	=~ /:filesize\s+\"(\d+)\"/;
#				$LA_asset_size = $1;
#				if ($LA_asset_size) {
#					#print "$asset_name - Size: $LA_asset_size ";
#				}else{
#					my $LA_asset_content = $LA_asset_details->{CONTENT};
#					$LA_asset_content	=~ /:size -h \".+\" \"(\d+)\"/;
#					$LA_asset_size = $1;
#					if ($LA_asset_size) {
#						#print "$asset_name - Size: $LA_asset_size ";
#					}else{
#						#print "$asset_name - No Size";
#					}
#				}				
#					
#				$LA_asset_meta	=~ /:filename\s+\"(.+)\"/;
#				$LA_asset_source = $1;						
#				if ($LA_asset_source) {
#					#print " - Path: $LA_asset_path\n";
#				}else{
#					$LA_asset_meta =~ /:source \"file:(.+)\"/;
#					$LA_asset_source = $1;
#					if ($LA_asset_source) {
#						#print " - Path: $LA_asset_path\n";
#					}else{
#						my $LA_asset_content = $LA_asset_details->{CONTENT};
#						$LA_asset_content =~ /:source \"file:(.+)\"/;
#						$LA_asset_source = $1;						
#						if ($LA_asset_source) {
#							#print " - Path: $LA_asset_path\n";
#						}else{
#							#print " - No Path \n";
#						}
#					}
#				}
#	
#			}else{
#				print "[ WARNING ] - asset $LA_asset_path does not exist !\n";
#				$LA_asset_path = "";
#			}
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_location}	= $LA_asset_path;
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_size}		= $LA_asset_size;
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_mtime}		= $LA_asset_mtime;
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_source}		= $LA_asset_source;
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_asset_id}	= $assetid;	
#																	
#		# ASSET is a Directory
#		#
#		} elsif ($asset_type eq "dir"){
#			$assetid = $LIVEARC->asset_exists(NAMESPACE => $LA_location, ASSET => $asset_name );
#			if ($assetid) {
#				my $LA_asset_details = $LIVEARC->get_asset(ID => $assetid);
#				my $LA_asset_meta = $LA_asset_details->{MTIME};
#				#print Dumper $LA_asset_meta;
#							
#				#:mtime -millisec "1347543191553" "13-Sep-2012 23:33:11"
#				$LA_asset_meta	=~ /-millisec\s+\"(\d+)\d{3}\"/;
#				$LA_asset_mtime = $1;
#
#				if (! $LA_asset_mtime) {
#					$LA_asset_mtime = -1;
#					#print "$LA_asset_mtime\n";
#				}
#							
#							
#			}else{
#				print "[ WARNING ] - directory assest $LA_asset_path does not exist !\n";
#				$LA_asset_path = "";
#			}
#						
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_location}	= $LA_asset_path;
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_size}		= $LA_asset_size;
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_mtime}		= $LA_asset_mtime;
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_source}		= $LA_asset_source;
#			$directory_slides->{$directory}{$raw}{assets}{$asset}{LA_asset_id}	= $assetid;	
#		}			
#					
#	
#	}
#	
}


sub check_raw_assets {
	my	$directory_slides 		= shift;
	
	foreach my $directory (keys %$directory_slides) {
		my $raw_slides = $directory_slides->{$directory};
		
		foreach my $raw_slide (keys %$raw_slides) {
			print "$raw_slide\n";
			$directory_slides->{$directory}{$raw_slide}{assets} = {};
			my $info = $directory_slides->{$directory}{$raw_slide};
			# Identifing assests to check for.
			my $platform = $info->{platform};
			my $rawslides_local_assets = $PLATFORMS->{$platform}{raw_assets}{local_assets};			
			
			foreach my $asset (keys %$rawslides_local_assets ) {
				my $assets = {};
				$assets = local_asset_checks($info, 'raw_assets', $directory, $raw_slide, $asset);
				foreach my $asset_name ( keys %$assets ) {
					if (! exists $directory_slides->{$directory}{$raw_slide}{assets}{$asset_name} ) {
						$directory_slides->{$directory}{$raw_slide}{assets}{$asset_name} = {};
					}
					my $asset_stats = $assets->{$asset_name};
					foreach my $stat ( keys %$asset_stats ){
						$directory_slides->{$directory}{$raw_slide}{assets}{$asset_name}{$stat} = $assets->{$asset_name}{$stat};
					}
					
				}
				
				$assets = LiveArc_asset_checks($info, 'raw_assets', $directory, $raw_slide, $asset);
				foreach my $asset_name ( keys %$assets ) {
					if (! exists $directory_slides->{$directory}{$raw_slide}{assets}{$asset_name} ) {
						$directory_slides->{$directory}{$raw_slide}{assets}{$asset_name} = {};
					}
					my $asset_stats = $assets->{$asset_name};
					foreach my $stat ( keys %$asset_stats ){
						$directory_slides->{$directory}{$raw_slide}{assets}{$asset_name}{$stat} = $assets->{$asset_name}{$stat};
					}					
				}
				
				#$directory_raws->{$directory}{$raw}{assets}{$asset}{local_location} = $local_asset_path;
				#$directory_raws->{$directory}{$raw}{assets}{$asset}{local_size} 	= $local_size;
				#$directory_raws->{$directory}{$raw}{assets}{$asset}{local_mtime} 	= $local_mtime;
				
			}
			
			my $slides_LA_assets = $PLATFORMS->{$platform}{slide_assets}{LA_assests};			
			foreach my $asset (keys %$slides_LA_assets ) {
				my $assets = LiveArc_asset_checks($info, 'slide_assets', $directory, $raw_slide, $asset);
				foreach my $asset_name ( keys %$assets ) {
					if (! exists $directory_slides->{$directory}{$raw_slide}{assets}{$asset_name} ) {
						$directory_slides->{$directory}{$raw_slide}{assets}{$asset_name} = {};
					}
					my $asset_stats = $assets->{$asset_name};
					foreach my $stat ( keys %$asset_stats ){
						$directory_slides->{$directory}{$raw_slide}{assets}{$asset_name}{$stat} = $assets->{$asset_name}{$stat};
					}					
				}
				
			}
			
		}
	}
}


sub check_slide_assets {
	my	$directory_slides 		= shift;
	
	foreach my $directory (keys %$directory_slides) {
		my $slides = $directory_slides->{$directory};
		for my $slide (keys %$slides) {
			my $mapsets = $directory_slides->{$directory}{$slide}{mapsets};
			my $slide_phydiv_nobc = mapsets_to_slide_phydiv_nobcs( \@$mapsets); 
			
			#
			# Identifing assests to check for.
			#
			my $platform = $directory_slides->{$directory}{$slide}{platform};
			my $LA_root_namespace = platform_to_LiveArc_Namespace($platform);
			my $local_assets = $PLATFORMS->{$platform}{slide_assets}{local_assets};
			#print Dumper $local_assets;
			foreach my $asset (keys %$local_assets ) {
				my $prefix 			= $PLATFORMS->{$platform}{slide_assets}{local_assets}{$asset}{prefix};
				my $asset_type 		= $PLATFORMS->{$platform}{slide_assets}{local_assets}{$asset}{asset_type};
				my $asset_location	= $PLATFORMS->{$platform}{slide_assets}{local_assets}{$asset}{asset_location};
				my $assets 	= [];
				
				my $LA_prefix 		= $PLATFORMS->{$platform}{slide_assets}{LA_assests}{$asset}{prefix};
				my $LA_asset		= $PLATFORMS->{$platform}{slide_assets}{LA_assests}{$asset}{asset};
				my $LA_asset_type 	= $PLATFORMS->{$platform}{slide_assets}{LA_assests}{$asset}{asset_type};
				my $LA_namespace 	= $PLATFORMS->{$platform}{slide_assets}{LA_assests}{$asset}{LA_namespace};
				#my $LA_assets 	= [];
					
				if ( $prefix eq "slide" ) {
					push ( @$assets, $slide);
					
				} elsif	( $prefix eq "mapset"){
					foreach my $mapset ( @$mapsets ) {
						push ( @$assets, $mapset );
					}
						
				} elsif	( $prefix eq "slide_phydiv_nobc"){
					foreach my $slide_phydiv ( @$slide_phydiv_nobc) {
						push ( @$assets, $slide_phydiv );
					}
				}
								
				$directory_slides->{$directory}{$slide}{assets} = {};
				foreach my $asset_item ( @$assets) {
					my $asset_name = $asset_item.$PLATFORMS->{$platform}{slide_assets}{local_assets}{$asset}{asset};
					#my $slide_location = $PLATFORMS->{$platform}{slide_assets}{location};
					my $slide_location = "$directory/$slide";
					my $local_location = "$slide_location/$slide/$asset_location";
				    # trimming trailing slash from directory paths.
				    $local_location =~ s[/$][];
					my $local_size = -1;
					my $LA_asset_size = -1;
					my $LA_asset_source = "";
					
					my $asset_file = "$local_location/$asset_name";
					
					if ( -e $asset_file ) {
						my $filestats = stat($asset_file);
						$local_size = $filestats->size;
					}else{
						print "[ WARNING ] - file $asset_file does not exist !\n";
					}
					
					my $LA_location = "/$LA_root_namespace/$slide/$LA_namespace";
				    $LA_location =~ s[/$][];
					my $LA_asset_file = "$LA_location/$asset_name";
					
					my $assetid = $LIVEARC->asset_exists(NAMESPACE => $LA_location, ASSET => $asset_name );
					
					if ($assetid) {
						my $LA_asset_details = $LIVEARC->get_asset(ID => $assetid);
						my $LA_asset_meta = $LA_asset_details->{META};
						
						$LA_asset_meta	=~ /:filesize\s+\"(\d+)\"/;
						$LA_asset_size = $1;
						if ($LA_asset_size) {
							#print "$asset_name - Size: $LA_asset_size ";
						}else{
							my $LA_asset_content = $LA_asset_details->{CONTENT};
							$LA_asset_content	=~ /:size -h \".+\" \"(\d+)\"/;
							$LA_asset_size = $1;
							if ($LA_asset_size) {
								#print "$asset_name - Size: $LA_asset_size ";
							}else{
								#print "$asset_name - No Size";
							}
						}
						
						
						
						$LA_asset_meta	=~ /:filename\s+\"(.+)\"/;
						$LA_asset_source = $1;						
						if ($LA_asset_source) {
							#print " - Path: $LA_asset_path\n";
						}else{
							$LA_asset_meta =~ /:source \"file:(.+)\"/;
							$LA_asset_source = $1;
							if ($LA_asset_source) {
								#print " - Path: $LA_asset_path\n";
							}else{
								my $LA_asset_content = $LA_asset_details->{CONTENT};
								$LA_asset_content =~ /:source \"file:(.+)\"/;
								$LA_asset_source = $1;						
								if ($LA_asset_source) {
									#print " - Path: $LA_asset_path\n";
								}else{
									#print " - No Path \n";
								}
							}
						}
						
						
						#print Dumper $asset_meta->{CONTENT};
					}else{
						print "[ WARNING ] - asset $LA_location does not exist !\n";
					}
					
					
																
					$directory_slides->{$directory}{$slide}{assets}{$asset_name} = { 
							local_location 	=> $asset_file, 
							local_size 		=> $local_size,
							LA_location		=> $LA_asset_file,
							LA_size			=> $LA_asset_size,
							LA_source		=> $LA_asset_source,
							LA_asset_id		=> $assetid
						};
				}
									
			}			
		}
	}
}

sub compare_assets {
	my	$directory_assets = shift;
	
	foreach my $directory (keys %$directory_assets) {
		my $slides = $directory_assets->{$directory};
		for my $slide (keys %$slides) {
			print "$slide\n";
			$slides->{$slide}{status} = "unknown";
			#$slides->{$slide}{assets};
			#$slides->{$slide}{platform};
			
			#print Dumper $slides->{$slide};
			my $asset_list = $slides->{$slide}{assets};
			foreach my $asset ( keys %$asset_list ){
				my $asset_details = $asset_list->{$asset};
				#print Dumper $asset_list->{$asset};

				if ( ! exists $asset_details->{local_size} ) {
					# Just checking if an assets exists
					
					if ( $asset_details->{LA_size} > 1 ) {
						$asset_details->{compare} = "passed"; 
						#print " GOOD. Looks like LiveArc Asset exists but there is no local asset";
					}
					elsif ( $asset_details->{LA_mtime} > 1){
						$asset_details->{compare} = "passed"; 
						#print " GOOD. Looks like LiveArc Asset exists but there is no local asset";
					}
					else{
						$slides->{$slide}{status} = 'failed';
						print "[ Warning ] - There is an issue with the LiveArc Asset: $asset\n";
					}
				}
				
				
				else{
					# Checking if local asset is backed up in LiveArc
					
					if ( $asset_details->{LA_size} == -1 and $asset_details->{LA_mtime} == -1 ) {
						$asset_details->{compare} = "failed"; 						
						print "[ Warning ] - Asset does not exist: $asset\n";
					}
					elsif ( $asset_details->{LA_size} == $asset_details->{local_size} ) {
						$asset_details->{compare} = "passed"; 
						#print " GOOD. Asset are the same size!";
					}
					elsif ( $asset_details->{LA_mtime} > $asset_details->{local_mtime} ) {
						$asset_details->{compare} = "passed"; 
						#print " GOOD. LiveArc Asset is newer then Local assest!";
					}
					else{
						$asset_details->{compare} = "failed"; 
						print "[ Warning ] - There is an issue with the Local Asset: $asset\n";
					}
				}
				
				
				if ( $asset_details->{compare} eq "failed" ) {
					$slides->{$slide}{status} = 'failed';
					foreach my $key ( keys %$asset_details ){
						print "\t$key => ".$asset_details->{$key}."\n";
					}
				}else{
					if ( $slides->{$slide}{status} ne "failed") {
						$slides->{$slide}{status} = 'passed';
					}
				}
				
				
			}
			
		}
	}
	
}





sub check_local_asset_file {
	# body...
}


sub check_LiveArc_asset_file {

}

sub check_local_asset_directory {
	# body...
}


sub check_LiveArc_asset_directory {

}










sub check_seqraw{
    my $slides = shift;
	my $slide_assets_checked = {};
	
	if ( ! @$slides ) {
		$slides = find_production_mapped_slides(); 
	}
	
	if ( @$slides ) {
		our $METADATA; 
		foreach	my $slide_name ( @$slides ){
			my $platform 	= "";
			my $mapsets		= "";
			my $raw_check 	= "Unknown Status";
			#check Assests vs LiveArc
			
			if ( $METADATA->find_metadata("slide", $slide_name) ) {
				$mapsets  =	$METADATA->slide_mapsets($slide_name);
			}
			
			# No call to find_metadata is need for this function.
			$platform =	$METADATA->slide_to_platform($slide_name);
			if ( $platform ) {
				$raw_check = check_seqraw_assets($slide_name, $platform);
			}
						
			$slide_assets_checked->{$slide_name} = {
				platform 		=> $platform,
				mapsets			=> $mapsets,
				raw_check		=> $raw_check,
				mapped_check	=> "Unknown Status"
			};
		} 
		
	}
	
	return \%$slide_assets_checked;
}











################################################################################
#        
#            Y   E
#            
#            O   L   D     
#            
#            C   O   D   E
#            
#            D   U   M   P   P   I   N   G
#            
#            G   R   O   U   N   D   E
#        
################################################################################


#sub platform_rawdata_location {
#	my $platform	= shift;
#		
#	my $raw_directories = {};
#		
#	if ( $platform and exists $PLATFORMS->{$platform} and exists $PLATFORMS->{$platform}{raw_assets} ) {
#		my $raw_location = $PLATFORMS->{$platform}{raw_assets}{locations};
#		foreach my $location (@$raw_location){
#			$raw_directories->{$location} = [ $platform ];
#		}
#		
#	}else{
#		foreach $platform (keys %$PLATFORMS) {
#			if ( exists $PLATFORMS->{$platform}{raw_assets}) {
#				my $raw_location = $PLATFORMS->{$platform}{raw_assets}{locations};
#				foreach my $location ( @$raw_location ){		
#					if ( exists $raw_directories->{ $location } ) {
#						my $platforms = $raw_directories->{$location};
#						push ( @$platforms, $platform );
#						$raw_directories->{$location} = $platforms;
#					}else{
#						$raw_directories->{$location} = [ $platform ];
#					}
#				} 
#			}
#		}			
#	}
#	
#	return $raw_directories;
#}


#sub platform_slidedata_location {
#	my $platform	= shift;
#	
#	my $slide_directories = {};
#	
#	if ( $platform and exists $PLATFORMS->{$platform}) {
#		my $slide_location = $PLATFORMS->{$platform}{slide_assets}{locations};
#		foreach my $location (@$slide_location){
#			$slide_directories->{$location} = [ $platform ];
#		}
#	}else{
#		foreach $platform (keys %$PLATFORMS) {
#			my $slide_location = $PLATFORMS->{$platform}{slide_assets}{locations};
#			foreach my $location (@$slide_location){
#				if ( exists $slide_directories->{ $location } ) {
#					my $platforms = $slide_directories->{$location};
#					push ( @$platforms, $platform );
#					$slide_directories->{$location} = $platforms;
#				}else{
#					$slide_directories->{$location} = [ $platform ];
#				}
#			}
#		 } 
#	}
#	
#	return $slide_directories;
#}


#
## If directory and owner is not in user list.
#sub non_production_slides {
#    my $non_production_slides = `find $SEQ_RAW -maxdepth 1 -type d ! \\( $USER_LIST \\) -exec basename {} \\;` ;
#    print "$non_production_slides \n";
#    #for slide in `find $SEQ_RAW -maxdepth 1 -type d ! \( $PRODUCTION_USER_LIST \) -exec basename {} \;`; do
#    #    non_production_slides+=($slide);
#    #done
#}
#
## Find list of mapsets not owned by production users
#sub non_production_mapsets {
#    #for mapset in `find $SEQ_MAPPED -maxdepth 1 -type d ! \( $PRODUCTION_USER_LIST \) -exec basename {} \;`; do
#    #    non_production_mapsets+=($mapset);
#    #done
#}
#
## Find list of slides owned by production users and has a _bioscope_ini dir to identify that this has been proceesed
## by the Automation pipline. Slide directories must be greater then X day old to give time for mapping of mapsets.
#sub find_production_mapped_slides {
#    #for slide in `find $SEQ_RAW -maxdepth 1 -type d \( $PRODUCTION_USER_LIST \) -mtime +$DIR_AGE -exec basename {} \;`; do 
#    #    if [[ -d $SEQ_RAW/$slide/_bioscope_ini ]]; then 
#    #        production_slides+=($slide);
#    #    
#    #    elif [[ `find $SEQ_RAW/$slide/$slide.*.ls -exec basename {} \;` ]]; then 
#    #        production_slides+=($slide);
#    #    
#    #    else
#    #        non_production_slides+=($slide);
#    #    fi
#    #done
#}
#
#
#archived_logs
## Find list of mapsets owned by production users. 
## Mapset directories must be greater then X day old to give time for mapping of mapsets.
#
#sub find_production_mapped_mapsets {
#    #for mapset in `find $SEQ_MAPPED -maxdepth 1 -type d \( $PRODUCTION_USER_LIST \) -mtime +$DIR_AGE -exec basename {} \;`; do 
#    #    production_mapsets+=($mapset);
#    #done
#}


__END__


=over 2

=item perl

=back


=head1 AUTHOR

=over 2

=item Matthew Anderson <mailto:m.anderson@imb.uq.edu.au>

=back


=head1 VERSION

$Id: qclean.pl 4668 2014-07-24 10:18:42Z j.pearson $


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
