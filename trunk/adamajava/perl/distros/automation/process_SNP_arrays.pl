#!/usr/bin/perl -w
#############################################################################
#
#  Program:  process_SNP_arrays.pl
#  Author:   Scott Wood
#  Created:  2013-04-09
#
#  Read text files from Genome Studio and generate circos plots for them,
#  distributing them to the correct donor folders in /mnt/seq_results
#
#  $Id: process_SNP_arrays.pl 7095 2013-06-19 06:08:06Z scott.wood $Id$
#
##############################################################################

## TODO
## - manage and report txt files that are not matched in the db
## - ditch the stat and just use "if ( -f )" for .pgpass
## - clean up naming of sampleid samplename arrayname, etc
## - use arrayscan.name instead?
## - parse and pass addition notification addresses to make_circos()
## - log tissue type (priority, this is what Katia has asked for)
## - handle when the sample appears multiple times
## - put in a "rename and resditribue" mode
## - POD, LOGGING, AND ERROR HADNLING OVERHAUL

## ASKABOUT
## - shift() vs. $_ and @_ for sub args
## - reference misunderstanding
## - all for each or each for all
## - $PROGRAM_NAME, $0 scope
## - global for non zero exit status
## - classes / modules / size (think SNP, GEX, Meth)

use strict;
use warnings;

use Cwd;
use Data::Dumper;
use DBI;
use File::HomeDir;
use File::stat;
use File::Basename;
use Getopt::Long;
use IO::File;
use Pod::Usage;
use QCMG::Util::QLog;
use Cwd 'abs_path';
use vars qw( $SVNID $REVISION $DRYRUN $VERSION $VERBOSE $PROGRAM_NAME);

( $REVISION ) = '$Revision: 7095 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: process_SNP_arrays.pl 7095 2013-06-19 06:08:06Z scott.wood $'
	=~ /\$Id:\s+(.*)\s+/;
$PROGRAM_NAME = abs_path( $0 ); 

## Summary:
# This script will scan a folder of *FinalReport*.txt files and prepare them
# for building circos plots.  Once it has done so, it will kick off a pbs
# job for each.  Each of those pbs jobs will generate a circos plot and then
# copy the plot and all supporting files to the correct donor folder
#
# Alternatively, this script can be called to run against a single file
# Exit status:
# If all .txt files result in a successful qsub of a job to create a circos
# plot, then the exit status of this job will be 0.  Otherwise, it will be 1.

## Requirements:
# - module load QCMGPerl has been run

## Use:
#   Whole directory:
#   {this script} -d {a Genome Studio directory} [ -e {additional email addies}
#
#   Single file:
#   {this script} -f {a Genome Studio report} [ -e {additional email addies}


MAIN: {
	# Setup defaults for important variables.
	# The GetOpt stuff:
	my @files         = ();
	my @directories   = ();
	my @emails        = ();
	my $dryrun        = 0;
	my $check         = 0;
	my $logfile       = '';
	   $VERBOSE       = 0;
	   $VERSION       = 0;
	my $help          = 0;
	my $man           = 0;
	my $email         = '';
	# Some DB details:
	my $pgpass        = File::HomeDir->home . '/.pgpass';
	# itterative and placeholder
	my $file          = '';
	my $newname       = '';
	my @keys          = ();
	
	# Our Metadata for the list of files:
	my %files_samples = ();
	my %files_meta    = ();

	# Print usage message if no arguments supplied
	# Stolen directly from JP's scripts
	pod2usage(1) unless (scalar @ARGV > 0);
	
	# Print a copy of the command lina as supplied
	my $commandline = join(' ',@ARGV);

	# Use GetOptions module to parse commandline options
	my $results = GetOptions (
		'c|check'       => \$check,
		'd|directory=s'	=> \@directories,
		'e|email=s'	    => \@emails,
		'f|file=s'      => \@files,
		'l|logfile=s'   => \$logfile,
		'n|dryrun!'     => \$DRYRUN,
		'v|verbose+'    => \$VERBOSE,
		'version!'      => \$VERSION,
		'h|help|?'      => \$help,
		'man|m'         => \$man
	);

	pod2usage(1) if $help;
	pod2usage(-exitstatus => 0, -verbose => 2) if $man;
	
	if ($VERSION) {
		print "$SVNID\n";
		exit;
	}

	qlogfile($logfile) if $logfile;
	qlogbegin;
	qlogprint( {l=>'EXEC'}, "CommandLine $PROGRAM_NAME $commandline\n");

	# Check for ~/.pgpass with qcmg schema creds
	stat($pgpass) or die "No ~/.pgpass file";

	#########################################################################
	# Spoon! (The Tick's take on "Cry Havoc and let slip, the dogs of war") #
	#########################################################################

	#  Build a comma separated list of email addresses
	$email = join(',', @emails);

	#  Parse files and directories passed
	for my $directory ( @directories ) { 
		@files = ( @files, txts_in_folder($directory));
		#push @files, txts_in_folder($directory);
	}
	#push @files, txts_in_folder($_) foreach (@directories);

	#  Address relative paths
	#foreach my $i (0..$#files) {
	for (my $i = 0; $i <= $#files; $i++) {
		$files[$i] = abs_path( $files[$i] );
	}
	
	#  Everything from here should be refactored to go in one loop
	# Populate the %file_sample hash
	%files_samples = create_files_samples( @files );

	#my $rh_files = create_files_samples( @files );
    #$rh_files->{$key}
    #keys %{$rh_files}

	if( $VERBOSE ) { print Dumper(\%files_samples); }
	#print Dumper(\%files_samples) if $VERBOSE;
	
	# Get the $DONOR $PROJECT $SAMPLENAME $BARCODE $PANEL
	%files_meta = build_meta( %files_samples );
	if ( $VERBOSE ) { print Dumper(\%files_meta); }

	# Check mode renames nothing, generates nothing and stops here
	if ( ! $check ){
		# Rename the files
		foreach $file (keys(%files_meta) ){
			$newname = rename_snp( $file, $files_meta{$file} );
			qlogprint("Renaming $file to $newname\n" );
			if ( ! $DRYRUN ) {
				rename( "$file", "$newname" );
				$files_meta{$newname} = delete( $files_meta{$file} );
			}
		}

		# Make a SNP_run_circos.conf & link
		foreach $file (keys(%files_meta) ){	make_circos_conf( $file ) } 	

		## Make a circosBAF and circosLogR
		foreach $file (keys(%files_meta) ){	make_baf_logr( $file ) } 	

		# Make pbs files that rename, make circos, and distribute file
		foreach $file (keys(%files_meta) ){	make_circos( $file, $files_meta{$file}, $email ) }
		qlogend;
	}
}

## Required functions:

# Given a folder name, make a list of txt files in it
sub txts_in_folder {
	my $folder         = shift;
	my $folder_status  = '';
	my @txts_in_folder = ();

	if ( -d $folder ) {
		qlogprint("Searching $folder for txt files\n" );
		@txts_in_folder = glob( "$folder/*.txt" );
		if ( $VERBOSE ) {
			for my $txt ( @txts_in_folder ) {
				qlogprint("Found $txt in $folder\n" );
			}
		}
	} else {
		 warn "$folder is not a folder\n";
	}
	return @txts_in_folder;
}

# Given a list of paths to SNP array file, return a hash of their IDs
# %snp_types = (
#		 file1 => 'WG0227768_DNAD03_LP6005273_DNA_D03',
#		 file2 => '9020681036_R07C01',
#	 );
sub create_files_samples {
	my @files   = @_;
	my %snp_ids = ();
	my $type    = '';

	for my $file ( @files ) {
		if ( open TXT, "<", $file ) {
			$type = '';
			while ( <TXT> )  {
				chomp;
				my @data=split;
				$snp_ids{$file}='';
				# Get the 12'th line
				if ( $. == 12 ) {
					# Get the second element
					$snp_ids{$file} = $data[1];
						if ( $snp_ids{$file} =~ /^WG/ ) {
							$type='IGN';
						} elsif ( $snp_ids{$file} =~ /[!_]*_[!_]*/ ) {
							$type='QCMG';
						} else {
							$type='Unknown';
						}
					qlogprint("File: $file Sample ID:$snp_ids{$file} Type:$type\n" );
					close TXT;
					last;
				}
			}
		} else {
			warn "File $file not found.\n";
		}
	}
	return %snp_ids;
	#return \%snp_ids;
}


# Given a hash { array_txt_file => array_id }, return a hash:
# { array_txt_file => reference to a list of $DONOR, $PROJECT, $SAMPLENAME,
#   $BARCODE, $PANEL, $PROJECT_FOLDER }
sub build_meta {
	my %snp_ids    = @_;
	my $dbhost     = undef;
	my $database   = undef;
	my $username   = undef;
	my $samplename = '';
	my $donor      = '';
	my $project    = '';
	my $sampleid   = '';
	my %files_meta = ();

	my $dbh = DBI->connect( "dbi:Pg:dbname=$database;host=$dbhost",$username );
	foreach my $key (keys(%snp_ids)) {
		if ( $snp_ids{$key} =~ /^WG/ ) {
			my $plateloc      = '';
			my @id            = '';
			my @meta          = ();
			my $arrayscanname = ();
			my $barcode       = '';
			my $panel         = '';
			my @fields        = ();

			$snp_ids{$key} =~ s/-/_/g;
			@id = split( '_', $snp_ids{$key} );
			$plateloc = "$id[2]-$id[3]_$id[4]";
			my $sth = $dbh->prepare(
				"SELECT sample.name, 
				project.name, 
				project.parent_project, 
				sample.sampleid, 
				arrayscan.name
				FROM sample, project, arrayscan 
				WHERE sample.ign_plate_location = '$plateloc'
				AND arrayscan.sampleid = sample.sampleid
				AND sample.projectid = project.projectid" );
			$sth->execute;
			if ( $sth->rows == 0 ) {
				warn "Plate Location: $plateloc not found in DB. $key will be skipped.\n";
			} elsif ( $sth->rows == 1 ) {
				while ( ($samplename, $donor, $project, $sampleid, $arrayscanname ) = $sth->fetchrow_array() ) {
					@fields = split( '_', $arrayscanname );
					$panel = $fields[0];
					$barcode = $fields[1];
					@meta = ( $donor, $project, $samplename, $panel, $barcode );
					$files_meta{$key} = \@meta;
				}
			} else {
				warn "Plate Location: $plateloc returned more than one value from DB. $key will be skipped.\n";
			}

		} else {
			##Sample ID from the txt file will be in the format 5963797076_R03C01
			my @id      = '';
			my $barcode = '';
			my $panel   = '';
			my @meta    = ();

			$sampleid   = $snp_ids{$key};
			if ( $sampleid =~ /[!_]*_[!_]*/ ) {
				@id = split( '_', $snp_ids{$key} ); 
				$barcode = "$id[0]";
				$panel = "$id[1]";
				my $sth = $dbh->prepare(
					"SELECT project.name,
					project.parent_project,
					sample.name 
					FROM sample, project, arrayscan
					WHERE arrayscan.name = '$snp_ids{$key}'
					AND sample.sampleid = arrayscan.sampleid
					AND sample.projectid = project.projectid");
				$sth->execute;
				if ( $sth->rows == 0 ) {
					warn "Array Scan Name: $snp_ids{$key} not found in DB. $key will be skipped.\n";
				} elsif ( $sth->rows == 1 ) {
					while ( ($donor, $project, $sampleid) = $sth->fetchrow_array() ) {
						@meta = ( $donor, $project, $sampleid, $barcode, $panel );
						$files_meta{$key} = \@meta;
					}
				} else {
					warn "Array Scan Name: $snp_ids{$key} returned more than one value from DB. $key will be skipped.\n";
				}
			} 
		}
		# Find out where the project folder resides
		if ( $files_meta{$key} ) {
			my @prefix      = ();
			my $parent_path = '';

			@prefix = split( '_|-', $files_meta{$key}[0] );
			my $sth = $dbh->prepare(
				"SELECT parent_path
				FROM project_prefix_map
				WHERE prefix = '$prefix[0]'");
			$sth->execute;
			if ( $sth->rows == 0 ) {
				warn "No project prefix path found for donor $files_meta{$key}[0], Array Scan Name: $snp_ids{$key}.  $key will be skipped.\n";
				delete  $files_meta{$key};
			} else {
				while ( $parent_path = $sth->fetchrow_array() ) {
					$files_meta{$key}[5] = $parent_path;
				}
			}
		}
	}
	$dbh->disconnect;

	return %files_meta;
}

sub parent_project_folder {
	my $donor  = shift();
	my $folder = '';
	my @prefix = ();

	@prefix = split ( '_', $donor );
}

sub rename_snp{
	my $fullpath = shift();
	my @meta     = shift();
	my $arrayid  = '';
	my $barcode  = '';
	my $result   = '';
	
####### WHY? - Help with references, please ####### 
	$arrayid  = $meta[0][3];
	$barcode  = $meta[0][4];
	$result = dirname($fullpath)."/".$arrayid."_".$barcode.".txt";
	return $result;
}

# Given an array_txt_file, create a BAF and LogR file.
# Pretty fuch a straight rip from
# QCMGScripts/n.waddell/SNP_array_analysis/extract_BAF_logR_fromGSxOutput_forCircos.pl
sub make_baf_logr {
	my $filename    = shift();
	my $filecounter = '';
	my $line        = '';
	my $positionend = ''; 
	my @columns     = '';

	open(IN, $filename) or die "Can't open $filename because $!\n";

	qlogprint("Opening File: $filename\n" );
	open(BAF, ">$filename.circosBAF");
	open(LOGR, ">$filename.circosLogR");

	while($line=<IN>) {
	# removing the 11 title lines
		next if $. <=11;
		chomp($line);

		# splitting up when see a tab
		@columns=split(/\t/, $line);
		$positionend=abs$columns[17]+1;

		# If the element in 3 (Allele1 - Top is '-' therefore no call), then
		# leave out, otherwise print out elements in 0 and 20 (which is rs# and logR)
		if ($columns[3] eq '-') {} else {print BAF "hs$columns[16]\t$columns[17]\t$positionend\t$columns[30]\n"}
		if ($columns[3] eq '-') {} else {print LOGR "hs$columns[16]\t$columns[17]\t$positionend\t$columns[31]\n"}
	}
	close(IN);
	close(BAF);
	close(LOGR);
}



# Given an array_txt_file, create a _run_circos.conf
sub make_circos_conf {
	my $fullpath   = shift();
	my $email      = shift();
	my $directory  = '';
	my $circosfile = '';
	my $baffile    = '';
	my $logrfile   = '';
	my $conffile   = '';
	my $time       = '';

	$directory   = dirname($fullpath);
	$circosfile  = basename($fullpath, ".txt").".circos.png"; 
	$baffile     = $fullpath.".circosBAF";
	$logrfile    = $fullpath.".circosLogR";
	$conffile    = dirname($fullpath)."/".basename($fullpath, ".txt")."_SNP_run_circos.conf";
	$time        = localtime();
 	my $template =
"Generated by process_SNP_arrays.pl $REVISION on $time
<colors>
<<include etc/colors.conf>>
</colors>

<fonts>
<<include etc/fonts.conf>>
</fonts>

<ideogram>

<spacing>

default = 10u

</spacing>

# thickness (px) of chromosome ideogram
thickness        = 100p
stroke_thickness = 2
# ideogram border color
stroke_color     = black
fill             = yes
# the default chromosome color is set here and any value
# defined in the karyotype file overrides it
fill_color       = black

# fractional radius position of chromosome ideogram within image
radius         = 0.85r
show_label     = yes
label_font     = condensedbold
label_radius   = dims(ideogram,radius) + 0.05r
label_size     = 36

# cytogenetic bands
band_stroke_thickness = 2

# show_bands determines whether the outline of cytogenetic bands
# will be seen
show_bands            = yes
# in order to fill the bands with the color defined in the karyotype
# file you must set fill_bands
fill_bands            = yes

band_transparency     = 1

</ideogram>
show_ticks          = yes
show_tick_labels    = yes
chrticklabels       = yes
chrticklabelfont    = default

grid_start         = dims(ideogram,radius_inner)-0.5r
grid_end           = dims(ideogram,radius_outer)+100

<ticks>
skip_first_label     = no
skip_last_label      = no
radius               = dims(ideogram,radius_outer)
tick_separation      = 2p
min_label_distance_to_edge = 0p
label_separation = 5p
label_offset     = 2p
label_size = 8p
multiplier = 1e-6
color = black

<tick>
spacing        = 5u
size           = 5p
thickness      = 2p
color          = black
show_label     = no
label_size     = 8p
label_offset   = 0p
format         = %d
grid           = yes
grid_color     = grey
grid_thickness = 1p
</tick>
<tick>
spacing        = 10u
size           = 8p
thickness      = 2p
color          = black
show_label     = yes
label_size     = 12p
label_offset   = 0p
format         = %d
grid           = yes
grid_color     = dgrey
grid_thickness = 1p
</tick>
</ticks>


# specify the karyotype file here; try also
#  data/2/karyotype.dog.txt
#  data/2/karyotype.rat.txt
#  data/2/karyotype.mouse.txt
#  data/2/karyotype.all.txt (human+dog+rat+mouse)
# but reduce frequency of ticks when increasing the
# number of ideograms
karyotype = /share/software/circos-0.49/data/karyotype.human.txt    #confirm

<image>
dir = $directory
file  = $circosfile
24bit = yes
png = yes
#svg = yes
# radius of inscribed circle in image
radius         = 1500p
background     = white
# by default angle=0 is at 3 o\'clock position
angle_offset   = -90
#angle_orientation = counterclockwise

auto_alpha_colors = yes
auto_alpha_steps  = 5
</image>

chromosomes_units           = 1000000
chromosomes_display_default = yes
#chromosomes = hs1;hs2

<plots>
<plot>
show    = yes
type    = histogram
max_gap = 1u
file    = $logrfile
color = vdgrey
thickness = 1
min   = -4.0
max   = 2.0
r0    = 0.88r
r1    = 0.98r

background       = yes
background_color = vvlgrey
background_stroke_color = black
background_stroke_thickness = 2

fill_under = yes
fill_color = yes

axis           = yes
axis_color     = lgrey
axis_thickness = 2
axis_spacing   = 0.5

<rules>
<rule>
importance   = 100
condition    = _VALUE_ > 0.9
color = dred
fill_color = dred
</rule>
<rule>

importance   = 85
condition    = _VALUE_ < -0.9
color = dgreen
fill_color = dgreen
</rule>
</rules>

</plot>

<plot>

show  = yes
type  = scatter
file  = $baffile
glyph = circle
glyph_size = 2
fill_color = black
stroke_color = black
stroke_thickness = 1
min   = 0.000
max   = 1.000
r0    = 0.76r
r1    = 0.86r

background       = yes
background_color = vvlgrey
background_stroke_color = black
background_stroke_thickness = 1

axis           = yes
axis_color     = lgrey
axis_thickness = 2
axis_spacing   = 0.25

</plot>

<plot>
show    = no            #set to yes if wish to show
type    = histogram
max_gap = 1u
file    = /data         #change if plotting GEX
color = vdgrey
thickness = 1
min   = 100
max   = 10000
r0    = 0.64r
r1    = 0.74r

background       = yes
background_color = vvlgrey
background_stroke_color = black
background_stroke_thickness = 2

fill_under = yes
fill_color = yes

axis           = yes
axis_color     = lgrey
axis_thickness = 2
axis_spacing   = 0.2

<rules>
<rule>
importance   = 10
condition    = _VALUE_ > 500
color = dyellow
fill_color = dyellow
</rule>
<rule>
importance   = 20
condition    = _VALUE_ > 1000
color = orange
fill_color = orange
</rule>
<rule>
importance   = 100
condition    = _VALUE_ > 5000
color = red
fill_color = red
</rule>
</rules>
</plot>
</plots>



<links>

z      = 0
radius = 0.79r
crest  = 1

<link translocations>
show         = no       #set to yes if wish to show
color        = blue
thickness    = 3
file         = /data        #change if wish to show
radius = 0.66r
bezier_radius = 0.1r
bezier_radius_purity = 0.5
</link>


<link deletions>
show         = no       #set to yes if wish to show
color        = red
thickness    = 3
file         = /data        #change if wish to show
bezier_radius = 0.99r
bezier_radius_purity = 0.75
</link>


<link insertions>
show         = no       #set to yes if wish to show
color        = dorange
thickness    = 3
file         = /data        #change if wish to show
bezier_radius = 0.6r
bezier_radius_purity = 0.75
</link>

<rules>
## remap the color of the link to the first chromosome
<rule>
importance = 100
condition  = 1
color      = eval(\"chr\".substr(_CHR1_,2))
flow       = continue
</rule>
</rules>

</links>

anglestep       = 0.5
minslicestep    = 10
beziersamples   = 40
debug           = no
warnings        = no
imagemap        = no

# don\'t touch!
units_ok        = bupr
units_nounit    = n";
	
	open( CONFFILE, ">$conffile" );
	print( CONFFILE "$template" );
	close( CONFFILE );
}

# Make pbs files that makes circos plots, and distribute files
sub make_circos {
	my $fullpath       = shift();
	my @meta           = shift();
	my $email          = shift();
	my $donor          = '';
	my $projectdir     = '';
	my $donordir       = '';
	my $sampleid       = '';
	my $arrayid        = '';
	my $barcode        = '';
	# The metadata and files and dirs
	my $path           = '';
	my $base           = '';
	my $baffile        = '';
	my $logrfile       = '';
	my $conffile       = '';
	my $circosfile     = '';
	my $pbsfile        = '';
	# The new "real" locations for files
	my $rawarraydir    = '';
	my $newbase        = '';
	my $newsnpfile     = '';
	my $newbaffile     = '';
	my $newlogrfile    = '';
	my $newconffile    = '';
	my $newcircosfile  = '';
	# The new symbolic links
	my $snparraydir    = '';
	my $snplink        = '';
	my $linkbase       = '';
	my $baflink        = '';
	my $logrlink       = '';
	my $conflink       = '';
	my $circoslink     = '';
	# otherstuff
	my $time           = '';
	my $qsubcmd        = '';
	my $qsuboutput     = '';
	
	# Generate the file and link paths
	# Current files and paths
	$donor         = $meta[0][0];
	$donor        =~ s/-/_/g;
	$projectdir    = $meta[0][5];
	$donordir      = $projectdir."/".$donor;
	$sampleid      = $meta[0][2];
	$sampleid     =~ s/-/_/g;
	$arrayid       = $meta[0][3];
	$barcode       = $meta[0][4];
	$path          = dirname($fullpath);
	$base          = $path."/".basename($fullpath, ".txt");
	$baffile       = "$fullpath.circosBAF";
	$logrfile      = "$fullpath.circosLogR";
	$conffile      = "$base\_SNP_run_circos.conf";
	$circosfile    = "$base.circos.png";
	$pbsfile       = "$base.circos.pbs";
	# new real
	$rawarraydir    = "$projectdir/$donor/SNP_array/raw_array";
	$newbase        = "$rawarraydir/$arrayid\_$barcode";
	$newsnpfile     = "$newbase.txt";
	$newbaffile     = "$newsnpfile.circosBAF";
	$newlogrfile    = "$newsnpfile.circosLogR";
	$newconffile    = "$newbase\_SNP_run_circos.conf";
	$newcircosfile  = "$newbase.circos.png";
	# new links
	$snparraydir    = "$projectdir/$donor/SNP_array";
	$linkbase       = "$snparraydir/$sampleid\_$arrayid\_$barcode";
	$snplink        = "$linkbase.txt";
	$baflink        = "$snplink.circosBAF";
	$logrlink       = "$snplink.circosLogR";
	$conflink       = "$linkbase\_SNP_run_circos.conf";
	$circoslink     = "$linkbase.circos.png";
	# datestamp
	$time           = localtime();
	# clean up the email
	if ( ! defined $email || $email eq '' ) {
		$email      = ' ';
	} else {
		$email      = ",$email";
	}
	if ( $VERBOSE ) { 
		qlogprint("donor         $donor\n");
		qlogprint("projectdir    $projectdir\n");
		qlogprint("sampleid      $sampleid\n");
		qlogprint("arrayid       $arrayid\n");
		qlogprint("barcode       $barcode\n");
		qlogprint("path          $path\n");
		qlogprint("base          $base\n");
		qlogprint("baffile       $baffile\n");
		qlogprint("logrfile      $logrfile\n");
		qlogprint("conffile      $conffile\n");
		qlogprint("circosfile    $circosfile\n");
		qlogprint("pbsfile       $pbsfile\n");
		qlogprint("rawarraydir   $rawarraydir\n");
		qlogprint("newbase       $newbase\n");
		qlogprint("newsnpfile    $newsnpfile\n");
		qlogprint("newbaffile    $newbaffile\n");
		qlogprint("newlogrfile   $newlogrfile\n");
		qlogprint("newconffile   $newconffile\n");
		qlogprint("newcircosfile $newcircosfile\n");
		qlogprint("snparraydir   $snparraydir\n");
		qlogprint("linkbase      $linkbase\n");
		qlogprint("snplink       $snplink\n");
		qlogprint("baflink       $baflink\n");
		qlogprint("logrlink      $logrlink\n");
		qlogprint("conflink      $conflink\n");
		qlogprint("circoslink    $circoslink\n");
	}
	# The PBS script, itself
	my $template = 
"#Script $pbsfile
#Generated by process_SNP_arrays.pl $REVISION on $time

#PBS -N circos-$donor
#PBS -r n
#PBS -q batch
#PBS -l walltime=10:00:00
#PBS -m ae
#PBS -j oe
#PBS -o $pbsfile.log
#PBS -l mem=10gb
#PBS -W umask=0007
#PBS -M k.nones\@uq.edu.au,scott.wood\@imb.uq.edu.au$email

date
echo \"Running circos plot of $fullpath\"
# run the circos plot
/share/software/circos-0.49/bin/circos-0.49 -conf $conffile
EXIT=\$((\$EXIT+\$?))

if [[ -d $donordir && -d $snparraydir ]]; then
	if [[ ! -d $rawarraydir ]]; then
		mkdir -v $rawarraydir
		EXIT=\$((\$EXIT+\$?))
	fi
#	copy the lot of files to the DONOR_FOLDER/DONOR_ARRAY/raw_array folder
	cp -v $fullpath \\
		  $baffile \\
		  $logrfile \\
		  $conffile \\
		  $circosfile \\
		  $rawarraydir
	EXIT=\$((\$EXIT+\$?))
#   make the links in DONOR_FOLDER/DONOR_ARRAY/raw_array
	ln -sv raw_array/".basename($newsnpfile)." $snplink
	EXIT=\$((\$EXIT+\$?))
	ln -sv raw_array/".basename($newbaffile)." $baflink
	EXIT=\$((\$EXIT+\$?))
	ln -sv raw_array/".basename($newlogrfile)." $logrlink
	EXIT=\$((\$EXIT+\$?))
	ln -sv raw_array/".basename($newconffile)." $conflink
	EXIT=\$((\$EXIT+\$?))
	ln -sv raw_array/".basename($newcircosfile)." $circoslink
	EXIT=\$((\$EXIT+\$?))
else
	echo \"$snparraydir could not be found.\" 
	EXIT=\$((\$EXIT+\$?))
fi
date
exit \$EXIT
";
	

	# Generate the pbs script
	open( PBSFILE, ">$pbsfile" );
	print( PBSFILE "$template" );
	close( PBSFILE );

	# Run the pbs script ( if we're not in $DRYRUN )
	$qsubcmd = "qsub $pbsfile 2>&1";
	qlogprint("Running $qsubcmd\n");
	if ( ! $DRYRUN ) {
		$qsuboutput = `$qsubcmd`;
		if ( ! $? ) {
			qlogprint("Submitted JobID $qsuboutput" );
		} else {
			warn "Qsub Failed running $qsubcmd with: $qsuboutput";
		}
	}
}

__END__

=head1 NAME 

process_SNP_arrays.pl - Perl script for processing Genome Studio SNP output

=head1 SYNOPSIS

 process_SNP_arrays.pl [options]

=head1 ABSTRACT

This script will process a set of Genome Studio generated txt files, doing
the following tasks:

 - Rename the files to match the QCMG Naming conventions
 - Generate BAF and LogR files for each txt file
 - Generate config and pbs files to create circos plots for the txt files
 - Distribute the generated plots and files to the correct donor folders
 - Create symbolic links to the files matching our old naming conventions

The script requires that a ~/.pgpass file exists and contains connection
information for connecting to the correct QCMG database.
It also requires that the QCMGPerl tree is in your PERL5LIB.

This script should only be run on qcmg-clustermk2, or a compute node, as
it expects /mnt/seq_results to be present and distributes files there.

=head1 OPTIONS
 
 - c | --check        check txt files only
 - d | --directory    directory to process
 - e | --email        email address to send pbs output to
 - f | --file         file to process
 - l | --logfile      file to print output to
 - n | --dryrun       do not rename files or run pbs scripts
 - v | --verbose      generate more verbose output
 - h | --help         output help
 - ? |                output help
 - m | --man          output man page

=over 2

=item B<- c | --check>

Check.  A check of the txt files against the LIMS is run simply to establish
whether or not all files have the necessary information to be processed. In
this mode, and no files are renamed, not BAF, LogR or pbs files are generated.

=item B<- d | --directory>

Directory.  A directory that contains txt files that were generated by Genome
Studio.  If any txt files exist in the folder that are not SNP array output
from Genome Studio, or that can not be found in the database, the script will
skip these files and, upon completion, return an exit status of 1.

Multiple -d options may be provided, prefixed with individual -d parameters.

=item B<- e | --email> 

Email.  Email addresses to put in the list of recipients for the output of
the PBS jobs that are run.  k.nones@imb.uq.edu.au and scott.wood@imb.uq.edu.au
are automatically emailed. 

Multiple -e options may be provided, prefixed with individual -d parameters

=item B<- f | --file> 

File.  A specific Genome Studio generated txt file to be processed.  If the
file does not exist or can not be found in the database, the script will
skip the file and, upon completion, return an exit status of 1.

Multiple -f options may be provided, prefixed with individual -f parameters.

=item B<- l | --logfile> 

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<- n | --dryrun> 

Dry run.  Do not rename the txt files nor run the generated PBS scripts.
When this option is provided, the script still outputs BAF and LogR files.

=item B<- v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<- h | ? | --help> 

Display help screen showing available commandline options.

=item B<- m | --man> 

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back

=head1 EXAMPLES

=over 2

 > module load QCMGPerl 
 > ./process_SNP_arrays.pl \ 
 		-d /panfs/seq_analysis/array_raw_data/a_SNP_dir \ 
 		-f /panfs/home/user/a_SNP_file.txt \ 
 		-e bossman@uq.edu.au \ 
 		-e underling@uq.edu.au \ 
 		-l /panfs/home/production/logs/SNP/todays.log 
                          
The previous command would connect to qcmg-database and queried to identify
all txt files in /panfs/seq_analysis/array_raw_data/a_SNP_dir, and 
/panfs/home/user/a_SNP_file.txt.  For each ot them that it could identify,
it would rename them to match the fomat ##########_R##C##.txt.

It would then generate a BAF and LogR file for each of the txt files

Next, it would generate and run a pbs script for each of the txt files,
each of which would generate a circos plot, copy the files to the correct
/mnt/seq_results/{project}/{donor}/SNP_array/raw_array, and create symbolic
links to these files in the /mnt/seq_results/{project}/{donor}/SNP_array
folders.

A notification would then be emailed to scott.wood@imb.uq.edu.au,
k.nones@imb.uq.edu.au, bossman@uq.edu.au, and underling@uq.edu.au by
each of the submitted pbs jobs.

=back

=head1 KNOWN BUGS

- This script does not accept files or paths with spaces in them


=head1 AUTHOR

=over 2

=item Scott Wood, L<mailto:scott.wood@imb.uq.edu.au>

=back


=head1 VERSION

$Id: process_SNP_arrays.pl 7095 2013-06-19 06:08:06Z scott.wood $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

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
