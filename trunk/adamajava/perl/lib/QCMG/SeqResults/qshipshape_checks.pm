package QCMG::SeqResults::qshipshape_checks;

##############################################################################
#
#  Module:   QCMG::SeqResults::qshipshape_checks.pm
#  Creator:  Matthew J Anderson
#  Created:  2012-07-10
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

use File::Basename;
use Getopt::Long;
use Data::Dumper;

use QCMG::DB::Metadata;

use vars qw( $SVNID $REVISION $DEBUG);

$REVISION = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
$SVNID 	= '$Id$'
    	=~ /\$Id:\s+(.*)\s+/;

$DEBUG = 1;

sub _dubug_requirements {
	my $message = shift;
	if ($DEBUG) {
		print "$message\n";
	}
	
}


sub mapset_metadata {
	my $self	= shift;
	my $mapset 	= shift;
	
	if ($mapset) {	
		my $metadata = QCMG::DB::Metadata->new();
		if ( $metadata->find_metadata("mapset", $mapset) ){
			return {
				library 	=> $metadata->primary_library($mapset),
				donor 		=> $metadata->donor(),
				final 		=> $metadata->mapset_final($mapset), #! should be sample
				project 	=> $metadata->project(),
				failed_qc 	=> $metadata->failed_qc($mapset),
				platform 	=> $metadata->mapset_to_platform($mapset)
			}
		}
	}
	return 0; # No metadata found.
}

sub mapset_exists {
    my $donor_directory     = shift;
    my $mapset              = shift;
    
     my @mapset_files = <$donor_directory/seq_mapped/$mapset*>;
     if (@mapset_files){
         print "The following files have been found with the mapset name: \n";
    #     qlogprint( {l=>'INFO'}, "The following files have been found with the mapset name:\n");
         foreach my $mapset_file (@mapset_files) {
          	print "$mapset_file \n";
    #    #  	qlogprint( {l=>'INFO'}, "$mapset_file\n");
         }
         return 1;
     } 
     return 0;
}

sub mapset_locked {
    my $donor_directory     = shift;
    my $mapset              = shift;
    
    # Check if mapset is locked
    die "[ Existing ] - Mapset $mapset has been locked for postmapping. Can not make changes to mapset." 
        unless ! -e "$donor_directory/seq_mapped/$mapset.bam.lck";
    
    return 0;
}


sub metadata_donor_directory{
	my $self 			= shift;
	my $projects_root	= shift;
	my $metadata		= shift;
	
	if ( ( exists $metadata->{project} and $metadata->{project} ) 
		and (exists $metadata->{donor} and $metadata->{donor} ) ) {
		return $projects_root.'/'.$metadata->{project}.'/'.$metadata->{donor};
	}
	
	return 0;
}


# Check options passed are valid
sub command_requirements_check {
    my $self	= shift;
	my $command = shift;
	
    # Set defaults for commandline options from programmer-supplied values
    my %opts	= ();
	my %flags	= ();
	#$opts{logfile}			=> 0;
	#$opts{mapset_dir_path}	=> 0;			
	
	$flags{rename}	= 0;
	$flags{verbose}	= 0;
	$flags{help}	= 0;
           
    my $results = GetOptions(
		"path=s"           	 => \$opts{mapset_path},        	# -path
		"mapped=s"           => \$opts{mapped_path},        	# -mapped
        "donor=s"            => \$opts{donor_directory},        # -donor
        "new=s"              => \$opts{new_donor_directory},    # -new
        "mapset=s"           => \$opts{original_mapset},        # -mapset
        "rename=s"           => \$opts{new_mapset},             # -rename
        "namespace=s"        => \$opts{LA_namespace},           # -namespace
        "asset=s"            => \$opts{LA_asset},               # -asset
        "existingDir=s"      => \$opts{existing_directory},     # -existingDir
        "existingMapset"     => \$opts{existingMapset},        # -existingMapset
        #"log=s"              => \$opts{log},                    # -log
        "v|verbose+"         => \$flags{verbose},               # -v
        "h|help|?"           => \$flags{help}                   # -help
    ); 
	
	# EXTRA 
	# $opts{ logfile, mapset_dir_path, }  
    
	#print Dumper \%opts;
	
    if ( $flags{help} ) {
        return 0;
    }
    
	if($opts{original_mapset}){
		_dubug_requirements("original_mapset");
		#print "mapset ".$opts{original_mapset}." \n";
		my ( $mapset, $mapset_dir_path, $suffix ) = fileparse( $opts{original_mapset}, qr/\.bam.*/ );
		#print "mapset ".$mapset." \n";
		#my $mapset_metadata = mapset_metadata ($mapset);
		#if ( $mapset_metadata ){
		#	print "METADATA failed_qc\t".$mapset_metadata->{failed_qc}."\n";
		#	print "METADATA project\t".$mapset_metadata->{project}."\n";
		#	print "METADATA donor \t".$mapset_metadata->{donor}."\n";
		#	
		#}
		
		
	}
	
	
	    
    # # # 
    # #   Standard requirements checks
    #
    
	# Mapset path
	if ( $opts{mapset_path} ) {
		_dubug_requirements("mapset_path");
		die "\nThe mapset ".$opts{mapset_path}." does not exist!" unless -f "$opts{mapset_path}";
		( $opts{original_mapset}, $opts{mapset_dir_path}, my $suffix ) = fileparse( $opts{mapset_path} );
	}
	# Mapped path
	elsif ( $opts{mapped_path} ) {
		_dubug_requirements("mapped_path");
		die "\nThe mapset ".$opts{mapped_path}." does not exist!" unless -f "$opts{mapped_path}";
		( $opts{original_mapset}, $opts{mapset_dir_path}, my $suffix ) = fileparse( $opts{mapset_path} );
		
	}
	# Donor directory + Original mapset
	else{
	    # Donor directory
		_dubug_requirements("donor_directory");
	    if ( $opts{donor_directory} ) {
			die "\nThe donor directory of the mapset ".$opts{donor_directory}." does not exist!" unless -d "$opts{donor_directory}";
	        # trimming trailing slash from directory paths.
	        $opts{donor_directory} =~ s[/$][];
	    }else{
	        print "\nYou must supply the donor directory of the mapset with a -donor <donor directory> \n\n";
	        
	        return 0;
			#&command_help($command);
	    }
    
	    # Original mapset
		_dubug_requirements("original_mapset");
	    if ( $opts{original_mapset} ) {
	        die "\nThe mapset ".$opts{original_mapset}." does not exist in ".$opts{donor_directory}."/seq_mapped !" 
	            unless mapset_exists($opts{donor_directory}, $opts{original_mapset});
        
	        &mapset_locked($opts{donor_directory}, $opts{original_mapset});
        
	        # TODO: Is this a valid mapset name
	        # TODO: Check it against Geneus
        
	    }else{
	        print "\nYou must supply a mapset with a -mapset <mapset name> \n\n";
	        return 0;
	       # &command_help($command);
	    }
    }
	
    # Log file directory.
	_dubug_requirements("logfile");
	my $log_file = $opts{original_mapset}.".qss.log";
	$opts{logfile} = $log_file;
	
    #if ( $opts{log} ) {   
    #    #my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);
    #    #my $date = sprintf("%04d%02d%02d", $year+1900, $mon+1, $mday);
    #    $opts{log} =~ s[/$][]; 
    #    #my $log_file = $opts{log}."/".$date."__".$opts{original_mapset}."__move.log";
	#	my $log_file = $opts{log}."/".$opts{original_mapset}.".qss.log";
    #    die "\nLog file $log_file already exists. Will not clobber existing log file!" unless ! -e $log_file;
    #    $opts{logfile} = $log_file;
	#}else{
	#	$opts{log} = 
	#}
	#
	#else{
    #    print "\nYou must supply a log directory with a -log <log directory> \n\n";
    #    &command_help($command);
    #}
    
    # # # 
    # #   Command spesific requirements checks
    #
    
    # Checking new donor directory exists, correctly structured and not the same as the current directory.
    if ( $opts{new_donor_directory} ) {
        _dubug_requirements("new_donor_directory");
		$opts{new_donor_directory} =~ s[/$][];
        
        # Checking new donor Direcotry is valid
        my $message = "\nThe new donor directory for the mapset ".$opts{new_donor_directory};
        die $message." does not exist" unless -d $opts{new_donor_directory};
        die $message."/seq_mapped does not exist"   unless -d $opts{new_donor_directory}."/seq_mapped";
        
		# No need to check seq_lib and seq_final any more.
		#	die $message."/seq_lib does not exist"      unless -d $opts{new_donor_directory}."/seq_lib";
        #	die $message."/seq_final does not exist"    unless -d $opts{new_donor_directory}."/seq_final";
        # TODO: Check against Geneus
        
        # Making sure the new mapset name is not the same as the old mapset name.
        die "\n[ Warning ] - New donor directory ".$opts{new_donor_directory}." is the same as the current donor directory ".$opts{donor_directory}." \n"
            unless ($opts{new_donor_directory} ne $opts{donor_directory});
    }   
    
    # New mapset name
    if ( $opts{new_mapset} ) {    
        _dubug_requirements("new_mapset");
		$flags{rename} = 1; 
        # TODO: Is this a valid mapset name
        # TODO: Check it against Geneus
        
        # Making sure that files do not exist with the new name.
        if ( $opts{new_donor_directory} ) {
            die "\n[ Warning ] - New mapset name ".$opts{new_mapset}." already exist in new directory ".$opts{new_donor_directory}."\n"
                   unless mapset_exists($opts{new_donor_directory}, $opts{new_mapset});
        }
        else{
            die "\n[ Warning ] - New mapset name ".$opts{new_mapset}." is the same as the original mapset name ".$opts{original_mapset}." \n"
                unless ($opts{new_mapset} ne $opts{original_mapset});
                
            die "\n[ Warning ] - New mapset name ".$opts{new_mapset}." already exist in current directory ".$opts{donor_directory}."\n"
                   unless mapset_exists($opts{donor_directory}, $opts{new_mapset});
        }
    } elsif ( $opts{new_donor_directory} ) {
        die "\n[ Warning ] - ".$opts{original_mapset}." already exist in new directory ".$opts{new_donor_directory}."\n"
               unless ! mapset_exists($opts{new_donor_directory}, $opts{original_mapset});
    }
    
    # LiveArc name space
    if ( $opts{LA_namespace} ){
        _dubug_requirements("LA_namespace");
		# TODO: Check namespace exists
        
        die "\n This option is not yet ready \n";
    }
    
    # LiveArc asset name
    if( $opts{LA_asset} ){
        _dubug_requirements("LA_asset");
		# TODO: Check asset exists
        die "\n This option is not yet ready \n";
    }

    # Existing directory
    if ( $opts{existing_directory} ){
        _dubug_requirements("existing_directory");
		
		my $message = "\nThe existing directory for the mapset ".$opts{existing_directory};
        die $message." does not exist" unless -d $opts{existing_directory};

        # Making sure the new mapset name is not the same as the old mapset name.
        die "\n[ Warning ] - New existing directory ".$opts{existing_directory}." is the same as the current donor directory ".$opts{donor_directory}." \n"
            unless ($opts{existing_directory} ne $opts{donor_directory});
    }
    
    #  Existing mapset
    if( $opts{existing_mapset} and $opts{existing_directory}){
        _dubug_requirements("existing_mapset + existing_directory");
		
		# Check if existing mapset exist  
        die "\nThe mapset ".$opts{existing_mapset}." does not exist in ".$opts{existing_directory}."/seq_mapped !" 
            unless ! &mapset_exists($opts{existing_directory}, $opts{existing_mapset});
        
        if ( $opts{existing_mapset} ne $opts{original_mapset}) {
            # we are renaming existing mapset
            #TODO: find if files exits
            if ( $flags{rename} ) {
                die "\nExiting. No files will be renamed.\n Mapset alreay exists ".$opts{donor_directory}."/",$opts{original_mapset}." \n"
                       unless ( $opts{new_mapset} ne $opts{original_mapset} );
                       
            }
            # If the existing mapset in not the same as the original, file will not be replaced.
            else{
                my $message = "\nYou are not allowed to replace $opts{original_mapset} with another name mapset!\n";
                $message .= "Existing mapset $opts{existing_mapset} does not match original mapset $opts{original_mapset}\n";
                $message .= "You must rename the existing mapset $opts{existing_mapset} to match original mapset to be replaced with a -rename\n";
                die $message ;
            }
        }

        
        # TODO: Is this a valid mapset name
        # TODO: Check it against Geneus
        
        die "\n This option is not yet ready \n";
    }

	# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	#
	#			COMMAND 	REQUIREMENTS 
	#
	# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # 
	
	# Auto
    if ( $command eq "auto" ) {
		_dubug_requirements( "Reqirement checks for command auto" );
	}
	
	# Check
	elsif ( $command eq "check" ) {
        _dubug_requirements( "Reqirement checks for command check" );
		if ( ! $opts{original_mapset} ) {
            print "\n You must supply a mapset with a -mapset \n\n";
	        return 0;
        }
		
        return  {opts=>\%opts, flags=>\%flags};
    }
	
	# Move
	elsif ( $command eq "move" ) {
        _dubug_requirements( "Reqirement checks for command move" );
		print "Checking move command options \n";
        if ( ! $opts{new_donor_directory}) {
            print "\n You must supply a new donor directory with a -new \n\n";
	        return 0;
            #&command_help($command);
        } 
        elsif ( $opts{donor_directory} and $opts{original_mapset} and $opts{new_donor_directory} ) {
            if ( $flags{rename} ) {
                die "\nExiting. No files will be renamed.\n Mapset alreay exists ".$opts{new_donor_directory}."/",$opts{original_mapset}." \n"
                       unless ! &mapset_exists($opts{new_donor_directory}, $opts{new_mapset});
                       
                print "Ready to move $opts{donor_directory} $opts{original_mapset} to $opts{new_donor_directory} as $opts{new_mapset}\n";
            }else{
                die "\nExiting. No files will be renamed.\n Mapset alreay exists ".$opts{new_donor_directory}."/",$opts{new_mapset}." \n"
                       unless ! &mapset_exists($opts{new_donor_directory}, $opts{original_mapset});
            
                print "\nReady to move $opts{donor_directory} $opts{original_mapset} to $opts{new_donor_directory} \n";
            }        
            return  {opts=>\%opts, flags=>\%flags};
        }
        else{
	        return 0;
            
			#&command_help($command);
        }
        
    }
	
	# Rename
    elsif ( $command eq "rename" ) {
        _dubug_requirements( "Reqirement checks for command rename" );
		if ( ! $flags{rename}) {
            print "\nYou must supply a new mapset name with a -rename \n\n";
	        return 0;
            #&command_help($command);
        }
        elsif ( $opts{donor_directory} and $opts{original_mapset} and $opts{new_mapset} ) {
             print "\nReady to rename $opts{donor_directory} $opts{original_mapset} as $opts{new_mapset}\n";
             return  {opts=>\%opts, flags=>\%flags};           
        }else{
	        return 0;
            #&command_help($command);
        }
        
    }
	
	# Replace
    elsif ( $command eq "replace" ) {
        _dubug_requirements( "Reqirement checks for command replace" );
		# TODO: requirements - replace
        if ( $opts{donor_directory} and $opts{original_mapset} ) {
            if ( $flags{rename} ) {
                die "\nExiting. No files will be renamed.\n Mapset alreay exists ".$opts{donor_directory}."/",$opts{new_mapset}." \n"
                    unless ! &mapset_exists($opts{donor_directory}, $opts{new_mapset});
            }
            if ( ($opts{LA_namespace} and $opts{LA_asset} ) and ! ( $opts{existing_directory} || $opts{existingMapset} )  ) {
                # TODO:  LiveArc Asset Check
                die "\nIt is not known if the Live Arc Asset exists yet."
            }elsif ( ( $opts{existing_directory} and $opts{existingMapset} ) and ! ($opts{LA_namespace} || $opts{LA_asset} ) ) {
                # TODO:  Copy from check.
                die "\nIt is not known if the Live Arc Asset exists yet.";
            }else{
                # TODO:  what to do to replace?
                die "\nIt looks like you don't know what you want.";
            }

        }else{
	       return 0;
           #&command_help($command);
        }
        
    }
	
	# Delete
    elsif ( $command eq "delete" ) {
        _dubug_requirements( "Reqirement checks for command delete" );
		if ( $opts{donor_directory} and $opts{original_mapset} ) {
            print "\nReady to delete mapset $opts{original_mapset} from $opts{donor_directory} \n";
            return  {opts=>\%opts, flags=>\%flags};
        }
        else{
	        return 0;
            #&command_help($command);    
        }        
	
	
	} 
	
	# Extract 
	elsif ( $command eq "extract" ) {
        _dubug_requirements( "Reqirement checks for command extract" );
		
		# TODO: requirements - extract
        if ( ! $flags{rename}) {
            print "\nYou must supply a new mapset name with a -rename \n\n";
	        return 0;
            #&command_help($command);
        }
        elsif ( ! $opts{LA_namespace} ){
            print "\nYou must supply a LiveArc namespace with a -namespace \n\n";
	        return 0;
            #&command_help($command);
        }
        elsif( ! $opts{LA_asset} ){
            print "\nYou must supply a LiveArc asset name with a -asset \n\n";
	        return 0;
            #&command_help($command);
        } 
        elsif ( $opts{donor_directory} and $opts{new_mapset} and $opts{LA_namespace} and $opts{LA_asset}  ) {
            print "\nReady to extract mappset asset $opts{LA_asset} from $opts{LA_namespace} to $opts{donor_directory} as $opts{new_mapset}\n";
            return  {opts=>\%opts, flags=>\%flags};
        
        } else{
	        return 0;
            #&command_help($command);
        }
    }
    
    return 0;
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
