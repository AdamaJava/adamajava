package QCMG::SeqResults::qshipshape_actions;

##############################################################################
#
#  Program:  qshipshape_actions.pm
#  Author:   Matthew J Anderson
#  Created:  2012-08-30
#
#  This script is a template framework for a command-line perl script.
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use IO::File;

# -- Move Mapsets
# _mapset_exists
# _mapset_files
# _library_of_mapset_bam
# _find_libraries
# _find_finals
# _move_mapset_files
# _delete_mapset_files
# _delete_Library_Final_files
# _delete_overview_files
# -- Rename Mapsets
# _rename_mapset_files
# -- 
# _get_mapset_LA_namespace
# _pre_checks
# _extract_mapset

sub _check_mapsetbam_lock {
	my $directory = shift;
	my $mapset = shift;
	
	my $lockfile = "$directory/$mapset.bam.lck";
	
	return $lockfile if -e $lockfile ;
	#else 
	return 0;
}


sub _set_mapsetbam_lock {
	my $directory = shift;
	my $mapset = shift;
	
	# If mapset bam does not exist, lock file won't be created.
	if ( ! -e "$directory/$mapset.bam" ) {
		print "[ Warning ] - Unable to set lock! - Mapset bam $directory/$mapset.bam does not exist.\n";
		return 0;
	}
	
	my $lockfile = _check_lock($directory, $mapset);
	if ( ! $lockfile ) {
		if ( open(my $lock, ">", "$directory/$mapset.bam.lck") ) {
			close( $lock ) ;
			return 1;
		}else{
			print "[ Warning ] - Unable to set lock! - Unable to create lock file $directory/$mapset.bam.lck .\n";
			return 0;
		}
	}
	print "[ Warning ] - Unable to set lock! - lock file already exist for $directory/$mapset.bam.lck .\n";
	return 0;
}

sub _remove_mapasetbam_lock{
	my $directory = shift;
	my $mapset = shift;
	
	my $lockfile = _check_lock($directory, $mapset);
	if ( $lockfile ) {
		if ( unlink $lockfile ) {
			return 1;
		}
		print "[ Warning ] - Unable to remove lock! - lock file $directory/$mapset.bam.lck does not exist.\n";
		return 0;
	}
	print "[ Warning ] - Unable to remove lock! - lock file $directory/$mapset.bam.lck does not exist.\n";
	return 0;
}


##
## ACTIONS
## 

sub auto {
	# body...
}

sub move {
	
	
	_check_mapsetbam_lock($directory, $mapset);
	#my $locked = _set_lock();
	#$directory = ?;
	#$mapset = ?;
	
	#_check_lock();
	
	#_remove_lock( $locked );
}

sub rename {
	# body...
}
sub replace {
	# body...
}

sub delete {
	# body...
}

sub extract {
	# body...
}

sub copy {
	# body...
}

sub quarantine {
	# body...
}



sub mapset_locked {
    my $donor_directory     = shift;
    my $mapset              = shift;
    
    # Check if mapset is locked
    die "[ Existing ] - Mapset $mapset has been locked for postmapping. Can not make changes to mapset." 
        unless ! -e "$donor_directory/seq_mapped/$mapset.bam.lck";
    
    return 0;
}

sub mapset_exists {
    my $donor_directory     = shift;
    my $mapset              = shift;
    
     my @mapset_files = <$donor_directory/seq_mapped/$mapset*>;
     if (@mapset_files){
         #print "The following files have been found with the mapset name: \n";
    #     qlogprint( {l=>'INFO'}, "The following files have been found with the mapset name:\n");
         #foreach my $mapset_file (@mapset_files) {
         # 	print "$mapset_file \n";
    #    #  	qlogprint( {l=>'INFO'}, "$mapset_file\n");
         #}
         return 0;
     } 
     return 1;
}

sub mapset_metadata {
	my $mapset = shift;
	my $metadata = QCMG::DB::Metadata->new();
	if ( $metadata->find_metadata("mapset", $mapset) ){
		return {
			library 	=> $metadata->primary_library($mapset),
			donor 		=> $metadata->donor(),
			final 		=> $metadata->mapset_final($mapset),
			project 	=> $metadata->project(),
			failed_qc 	=> $metadata->failed_qc($mapset),
			platform 	=> $metadata->mapset_to_platform($mapset)
		}
	}
	return 1; # No metadata found.
}

sub check_original_mapset_valid {
	my $original_mapset = shift;
	my $donor_directory	= shift;
	my $mapset_metadata = mapset_metadata( $original_mapset );
	
	print "original_mapset $original_mapset\n";
	print "donor_directory $donor_directory\n";
	if ( $mapset_metadata ) {
		print "METADATA failed_qc\t".$mapset_metadata->{failed_qc}."\n";
		print "METADATA project\t".$mapset_metadata->{project}."\n";
		print "METADATA donor \t".$mapset_metadata->{donor}."\n";
		
	}

}

sub check_new_mapset_valid {
	my $new_mapset 			= shift;
	my $new_donor_directory = shift;
	my $mapset_metadata = mapset_metadata( $new_mapset );
	
	print "new_mapset $new_mapset\n";
	print "new_donor_directory $new_donor_directory\n";
	if ( $mapset_metadata ) {
		print "METADATA failed_qc\t".$mapset_metadata->{failed_qc}."\n";
		print "METADATA project\t".$mapset_metadata->{project}."\n";
		print "METADATA donor\t".$mapset_metadata->{donor}."\n";
	}
	
}






1;

__END__

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
