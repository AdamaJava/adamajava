#!/usr/bin/perl

##############################################################################
#
#  Program:  extract_torrent.pl
#  Author:   Lynn Fink
#  Created:  2011-02-03
#
# Mapped file extraction script for Torrent; copies files from LiveArc to babyjesus
#
# $Id: extract_mapped.pl 1527 2012-01-30 23:33:24Z l.fink $
#
##############################################################################

use strict;
use Getopt::Long;
#use lib qw(/share/software/QCMGPerl/lib/);	# not good, but testing...
#use lib qw(/panfs/home/lfink/devel/QCMGPerl/lib/);
use QCMG::Automation::LiveArc;
use QCMG::Automation::Common;
use QCMG::DB::Metadata;
use File::Spec;
use Data::Dumper;

use vars qw($SVNID $REVISION $VERSION);
use vars qw($SLIDENAME $EXTRACTTO $LOG_FILE $LOG_LEVEL $ADD_EMAIL $UPDATE);
use vars qw($USAGE);

# set command line, for logging
my $cline	= join " ", @ARGV;

&GetOptions(
		"i=s"		=> \$SLIDENAME,	# root name
		"o=s"		=> \$EXTRACTTO,
		"log=s"		=> \$LOG_FILE,
		"loglevel=s"	=> \$LOG_LEVEL,
		"e=s"		=> \$ADD_EMAIL, 
		"V!"		=> \$VERSION,
		"h!"		=> \$USAGE
	);

$EXTRACTTO	= "/mnt/seq_results/";	# default
unless($LOG_FILE) {
	$LOG_FILE	= "/panfs/automation_log/".$SLIDENAME."_extracttorrent.log";
}

# help message
#	-u	  only extract assets that have not yet been extracted
if($USAGE || (! $SLIDENAME) ) {
        my $message = <<USAGE;

        USAGE: $0

	Extract mapped Torrent sequencing run files from LiveArc 

        $0 -i slidename  -log logfile.log

	Required:
	-i        <name> slidename 
	-log      <file> log file to write execution params and info to

	Optional:
	-V        (version information)
        -h        (print this message)

USAGE

        print $message;
        exit(0);
}

umask 0002;

=cut
    :namespace -path "/QCMG_torrent/T01_20120112_104_Frag"
        :asset -id "47568" -version "3" "T01_20120112_104_Frag.nopd.nobc.wells"
        :asset -id "47569" -version "3" "T01_20120112_104_Frag.nopd.nobc.fastq"
        :asset -id "47570" -version "3" "T01_20120112_104_Frag.nopd.nobc.bam"
=cut

my $la		= QCMG::Automation::LiveArc->new(VERBOSE => 1);

# write EXECLOG parameters
# pass command line args for logging
$la->cmdline(LINE => $cline);
# write start of log file
$LOG_FILE =~ s/ //g;
$la->init_log_file(LOG_FILE => $LOG_FILE);
$la->execlog();

# write TOOLLOG parameters, if any
my $toollog	 = qq{TOOLLOG: LOG_FILE $LOG_FILE\n};
$toollog	.= qq{TOOLLOG: SLIDENAME $SLIDENAME\n};
$toollog	.= qq{TOOLLOG: EXTRACTTO $EXTRACTTO\n};
$la->writelog(BODY => $toollog);

$la->LA_NAMESPACE("/QCMG_torrent/");
my $bin_seqtools	= $la->BIN_SEQTOOLS;

system("module load samtools");
system('module load adama');

# key = namespace:asset; value = id
my %to_extract	= ();
my $assets	= $la->list_assets(NAMESPACE => '/QCMG_torrent/'.$SLIDENAME);
#print Dumper $assets;

print STDERR "Querying LIMS for $SLIDENAME\n";

my $md	= QCMG::DB::Metadata->new();

my $project_type;
my @barcodes	= ();
if($md->find_metadata("slide", $SLIDENAME)) {
	my $slide_mapsets	=  $md->slide_mapsets($SLIDENAME);

	foreach my $mapset (@$slide_mapsets) {
		$md->find_metadata("mapset", $mapset);

		my $gff 			= $md->gff_file($mapset);

        	my $d				= $md->mapset_metadata($mapset);

		my $libtype			= $d->{'library_type'};
		my $barcode			= $d->{'barcode'};
		my $mapset			= $d->{'mapset'};
		my $donor			= $d->{'donor'};
		my $project			= $d->{'project'};
		my $failed_qc			= $d->{'failed_qc'};

		# don't attempt to extract anything if this slide failed QC
		if($failed_qc != 0) {
			$la->writelog(BODY => "$SLIDENAME extraction not attempted; failed_qc flag set");
	
			my $date	= $la->timestamp();
	
			$la->writelog(BODY => "EXECLOG: ERRORMESSAGE extraction not attempted");
			$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
			$la->writelog(BODY => "EXECLOG: EXITSTATUS 0");
		
			my $subj	= $SLIDENAME." MAPPED EXTRACTION -- NOT ATTEMPTED";
			my $body	= "MAPPED EXTRACTION of $SLIDENAME not attempted";
			$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;
			$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
		
			exit(0);
		}

		my $subdir			= 'seq_mapped/';

		if($libtype =~ /ampliseq/i) {
			$subdir			= 'seq_amplicon/';
		}



		# T00001_20120628_218.nopd.IonXpress_016
		my ($slide, $pd, $barcode)	= split /\./, $mapset;

		#print STDERR "BARCODE: $barcode\n";
		$la->writelog(BODY => "Mapset: $mapset\nBarcode: $barcode\nGFF: $gff\nSUBDIR: $subdir");

		push @barcodes, $barcode;


		my $asset		= $la->LA_NAMESPACE."/$SLIDENAME/:".$mapset.".bam";
		$asset			=~ s/\/\//\//g;
		my $id			= $assets->{$asset};

		print STDERR "$asset -> $id\n";

		$la->writelog(BODY => "Project ID: $project");
		$la->writelog(BODY => "Donor     : $donor");

		my $extract_path	= "/mnt/seq_results/$project/$donor/$subdir/$mapset".".bam";
		print STDERR "EXTRACT PATH: $extract_path\n";


		$la->writelog(BODY => "Extracting $asset ($id) to $extract_path");

		my $cmd = $la->extract_asset(ID => $id, PATH => $extract_path, RAW => 1, CMD => 1);
		$la->writelog(BODY => "$cmd");
		$la->writelog(BODY => "LiveArc command: $cmd");

		$la->writelog(BODY => "Executing $cmd");	# 0 = success
		my $rv	= system($cmd);
		$la->writelog(BODY => "RV: $rv");	# 0 = success

		if($rv != 0) {
			$la->writelog(BODY => "$asset extraction failed");
	
			my $date	= $la->timestamp();
	
			$la->writelog(BODY => "EXECLOG: ERRORMESSAGE extraction failed");
			$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
			$la->writelog(BODY => "EXECLOG: EXITSTATUS $rv");
		
			my $subj	= $SLIDENAME." MAPPED EXTRACTION -- FAILED";
			my $body	= "MAPPED EXTRACTION of $SLIDENAME failed";
			$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;
			$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
		
			exit(1);
		}

		$la->writelog(BODY => "--Performing checksum on BAM--");
		$la->writelog(BODY => "START: ".$la->timestamp());
		my $assetdata		= $la->get_asset(ID => $id);
		if($assetdata->{'META'} !~ /:checksums/) {
			$la->writelog(BODY => "Checksums not available for this asset, skipping check");
			next;
		}

		my ($cksum, $fsize, $fname) = ($assetdata->{'META'} =~ /:checksum\n\s+:chksum\s+\"(\d+)\"\n\s+:filesize\s+\"(\d+)\"\n\s+:filename\s+\"(.+?)\"/sg);
		my ($crc, $blocks, $fname) = $la->checksum(FILE => $extract_path);
	
		$la->writelog(BODY => "LiveArc $fname: checksum $cksum, size $fsize");
		$la->writelog(BODY => "Current $fname: checksum $crc, size $blocks");
	
		#print STDERR "$fname: checksum $crc, size $blocks\n";
	
		unless($cksum == $crc && $fsize == $blocks) {
			$la->writelog(BODY => "Checksum invalid");

			my $date	= $la->timestamp();
	
			$la->writelog(BODY => "EXECLOG: ERRORMESSAGE BAM checksum mismatch");
			$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
			$la->writelog(BODY => "EXECLOG: EXITSTATUS 13");
	
			my $subj	= $SLIDENAME." MAPPED EXTRACTION -- FAILED";
			my $body	= "MAPPED EXTRACTION of $SLIDENAME failed";
			$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;
			$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
	
			exit(13);
		}

		# extract plugin_out folder to variants directory
		my $plugin_assetname	= $SLIDENAME.".nopd.nobc_plugin_out";
		my $id			= $la->asset_exists(ASSET => $plugin_assetname, NAMESPACE => $la->LA_NAMESPACE."/$SLIDENAME");
		print STDERR "Checking for variant calls in asset $plugin_assetname: $id\n";
		$la->writelog(BODY => "Extracting variant calls from $plugin_assetname (id: $id)");
		if($id =~ /\d/) {
			# create variants/torrent/ directory if it doesn't exist already
			my $variantsdir	= "/mnt/seq_results/$project/$donor/variants/";
			print STDERR "Making $variantsdir ...\n";
			mkdir $variantsdir if(! -e $variantsdir);
			$variantsdir	= "/mnt/seq_results/$project/$donor/variants/torrent/";
			print STDERR "Making $variantsdir ...\n";
			mkdir $variantsdir if(! -e $variantsdir);

			# get contents
			print STDERR "Checking for VCF file archive...\n";
			my $files		= $la->list_contents(ID => $id);
			foreach (keys %{$files}) {
				chomp;

				#print STDERR "FILE: $_ *\n";

				next unless(/TSVC\_variants\.vcf$/);

				print STDERR "CHECKING FILE: $_ BARCODE: $barcode\n";

				#print STDERR "FOUND TSVC FILE: $_ (current barcode = $barcode)\n";
				# will need to fix when a non-barcoded run is
				# seen; not sure what nonbarcoded variant calls
				# look like.... FIX!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
				# plugin_out/variantCaller_out/TSVC_variants.vcf -> non-barcoded

				my $vcf = $_;
				# plugin_out/variantCaller_out/IonXpress_002/TSVC_variants.vcf
				if(defined $barcode && $vcf =~ /$barcode/) {
					print STDERR "Found: $vcf (barcode = $barcode)\n";
				}
				elsif(! $barcode || $barcode eq 'nobc') {
					print STDERR "Found: $vcf (barcode = $barcode (none))\n";
				}
				else {
					#print STDERR "Skipping $vcf (barcode = $barcode)\n";
					next;
				}

				# plugin_out/variantCaller_out/IonXpress_011/TSVC_variants.vcf -> 7
				my $barcode	= 'nobc';
				if(/variantCaller_out\/(.+?)\/TSVC/) {
					$barcode	= $1;
				}
				my ($v, $d, $f)	= File::Spec->splitpath($vcf);
				#print STDERR "$_ -> $files->{$_} -> $f\n";

				my $vcfname	= join ".", $SLIDENAME, "nopd", $barcode, $f;
				#print STDERR "VCF: $vcfname\n";

				# extract asset to that directory
				#my $rv		= $la->extract_content(ID => $id, IDX => $files->{$_}, PATH => $variantsdir."/".$vcfname);
				my $tmpdir	= "/tmp/".$vcfname;
				mkdir $tmpdir;
				my $rv		= $la->extract_content(ID => $id, IDX => $files->{$_}, PATH => $tmpdir);
				#print STDERR "RV: $rv, extracting variant calls asset $id to $variantsdir\n";
				$la->writelog(BODY => "RV: $rv extracted variant calls asset $id to $variantsdir");

				my $cmd		= qq{mv $tmpdir/$vcf $variantsdir/$vcfname};
				print STDERR "$cmd\n";
				my $rv		= system($cmd);
				$la->writelog(BODY => "RV: $rv copied variant calls from $tmpdir/$vcf to $variantsdir/$vcfname");
				# remove temporary extraction directory
				unlink $tmpdir;

				last;
			}
		}



		# make BAM index
		my $idxcmd	= qq{samtools index $extract_path};
		my $rv		= system($idxcmd);

		$la->writelog(BODY => "RV for $idxcmd : $rv");	# 0 = success

		if($rv != 0) {
			$la->writelog(BODY => "$extract_path BAM index creation failed");
		
			#my $date	= $la->timestamp();
	
			#$la->writelog(BODY => "EXECLOG: ERRORMESSAGE index creation failed");
			#$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
			#$la->writelog(BODY => "EXECLOG: EXITSTATUS 31");
		
			#my $subj	= $SLIDENAME." MAPPED EXTRACTION -- FAILED";
			#my $body	= "MAPPED EXTRACTION of $SLIDENAME failed";LP6005273-DNA_F07 - bam2fastq 294465 R
			#$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;
			#$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
		
			#exit(2);
		}

=cut
		# only run coverage if a valud GFF file exists
		if(-e $gff) {
			my $covcmd		= qq{qcoverage -t seq -n 13 --xml --query 'Flag_DuplicateRead==false' --gff $gff --bam $extract_path --bai $extract_path.bai --log $extract_path.qcov.log --output $extract_path.qcov.xml};
	
			print STDERR "$covcmd\n";
	
			my $xml			= $extract_path.".qcov.xml";
			my $viscmd		= qq{$bin_seqtools/qvisualise.pl qcoverage -i $xml -o $xml.html -log $xml.qvis.log};
	
			my $rv		= system($covcmd);
			$la->writelog(BODY => "RV for $cmd : $rv");	# 0 = success
	
			my $rv		= system($viscmd);
			$la->writelog(BODY => "RV for $cmd : $rv");	# 0 = success
	
			if($rv != 0) {
				$la->writelog(BODY => "$_ coverage/visualization failed");
		
				my $date	= $la->timestamp();
		
				$la->writelog(BODY => "EXECLOG: ERRORMESSAGE coverage failed");
				$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
				$la->writelog(BODY => "EXECLOG: EXITSTATUS 31");
		
				my $subj	= $SLIDENAME." MAPPED EXTRACTION -- FAILED";
				my $body	= "MAPPED EXTRACTION of $SLIDENAME failed";
				$body		.= "\nSee log file: ".$la->HOSTKEY.":".$LOG_FILE;
				$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);
		
				exit(32);
			}
		}
=cut
	}
}

# finish EXECLOG
my $date = $la->timestamp();
$la->writelog(BODY => "EXECLOG: ERRORMESSAGE none");
$la->writelog(BODY => "EXECLOG: FINISHTIME $date");
$la->writelog(BODY => "EXECLOG: EXITSTATUS 0");

# send success email if no errors
my $subj	= $SLIDENAME." MAPPED EXTRACTION -- SUCCEEDED";
my $body	= "MAPPED EXTRACTION of $SLIDENAME successful. See log file: ".$la->HOSTKEY.":".$LOG_FILE;
$la->send_email(EMAIL => 'QCMG-InfoTeam@imb.uq.edu.au', SUBJECT => $subj, BODY => $body);


my $ns		= join "/", $la->LA_NAMESPACE, $SLIDENAME;

my $asset		= $SLIDENAME."_extractmapped_log";

my $la	= QCMG::Automation::LiveArc->new(VERBOSE => 0);
my $assetid = $la->asset_exists(NAMESPACE => $ns, ASSET => $asset);
my $status;

if(! $assetid) {
	#print STDERR "Creating log file asset\n";
	$assetid = $la->create_asset(NAMESPACE => $ns, ASSET => $asset);
}
#print STDERR "Updating log file\n";
$status = $la->update_asset_file(ID => $assetid, FILE => $LOG_FILE);


exit(0);

