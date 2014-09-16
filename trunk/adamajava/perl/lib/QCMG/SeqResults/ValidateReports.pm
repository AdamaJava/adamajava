package QCMG::SeqResults::ValidateReports;

###########################################################################
#
#  Module:   QCMG::SeqResults::ValidateReports.pm
#  Creator:  Matthew J Anderson
#  Created:  2012-11-20
#
# This Module is a collection of tools for checking if Reports have been
# run with the correct parameters. 
#
#  $Id: $
#
###########################################################################

use strict;
use warnings;

use File::Basename;
use Getopt::Long;
use Pod::Usage;
use Data::Dumper;
use Exporter qw( import );

use QCMG::DB::Metadata;
use QCMG::Util::QLog;
##use QCMG::SeqResults::Util qw( qmail is_valid_mapset_name
#                               bams_that_were_merged_into_this_bam
#                               seqmapped_bams seqlib_bams );
#


use vars qw( $SVNID $REVISION @EXPORT_OK );

( $REVISION ) = '$Revision: 2871 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Util.pm 2871 2012-08-28 03:35:45Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

@EXPORT_OK = qw( prefetch_metadata_by_parent_project check_seqmapped_coverage_report );

our $metadata = QCMG::DB::Metadata->new();
  #$metadata->prefetch_all_metadata();

sub prefetch_metadata_by_parent_project {
  my $parent_project = shift;
  $metadata->clear_found_metadata();
  
  foreach my $project ( @{$metadata->parent_project_projects($parent_project)} ){
    $metadata->find_metadata("project", $project);
  }
}
  

# This funciton is used to check if the coverage reports has been run correctly.
# Checking argument flags: --gff3,  --bam
sub check_seqmapped_coverage_report {
	my $coveage_log = shift;
	my $coveage_kind = shift;
	my $mapset = basename($coveage_log,  ".bam.$coveage_kind.log");
	
	my $cmd    = qq{grep "CommandLine" $coveage_log};
	my $res    = `$cmd`;
	
	my $gff = "";
	if ( $res =~ /--gff3\s+(.+?)\s/ ){
		$gff = $1;
	}elsif ($res =~ /--gff\s+(.+?)\s/ ){
		#print "gff match not gff3 - $coveage_log \n";
		$gff = $1;
	}

	$res =~ /--bam\s+(.+?)\s/;
	my $bam = $1;
	
	if ( $coveage_log ne "$bam.$coveage_kind.log") {
    qlogprint( {l=>'WARN'}, "bam $bam does not match log name $coveage_log\n");  
		#print "$coveage_log\n[LOG] --bam $bam does not match log name\n";
	}
		
	if ( $metadata->find_metadata("mapset", $mapset) ){	
		my $lims_gff = $metadata->gff_file( $mapset );
		if ( ! $lims_gff ){
      qlogprint( {l=>'WARN'}, "gff_file not found in LIMS for $mapset\n");  
		}
		elsif ( $gff ne $lims_gff ) {
			if ($gff ne "/panfs/share/qsegments/GRCh37_ICGC_standard_v2.properties.gff3") {
				print "$coveage_log\n";
				print "[LOG] 	--gff3 $gff\n";
				print "[LIMS] gff_file $lims_gff\n", 
			}
		}
	}else{
		print "Resource $mapset not found!\n";
	}
	 
}



1;

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
