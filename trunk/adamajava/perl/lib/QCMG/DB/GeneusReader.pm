package QCMG::DB::GeneusReader;

################################################################################
#
#  Module:   QCMG::DB::GeneusReader.pm
#  Creator:  Matthew J Anderson
#  Created:  2012-02-24
#
#  It is suggested that you use QCMG::DB::Metadata as an interface this class.
#
#  This class's primary purpose is for retiving metadata on sequencing runs
#  store in the Genologic LIMS database "Geneus". 
#  
#  $Id: GeneusReader.pm 4661 2014-07-23 12:26:01Z j.pearson $:
#
################################################################################


=pod

=head1 NAME

QCMG::DB::GeneusReader -- Common functions for querying the Geneus LIMS

=head1 SYNOPSIS

 my $geneus = QCMG::DB::GeneusReader->new()
 my $resource = "ICGC-ABMP-20091203-10-ND";
 if ( $geneus->resource_metadata("sample", $resource) ){
	print Dumper $geneus->fetch_metadata();
 }
 

=head1 DESCRIPTION

This class contains methods for retrieving metadata from Genologic LIMS database 
"Geneus". The metadata retrieved is intentionally used to populate the data 
structures of the QCMG::DB::Metadata class. In which if using this class for 
seraching for metadata of a mapset will return metadata for all mapsets of the 
samples of the searched mapset. This class also allows you to retive all 
metadata from Geneus which is useful for processing directory stuctures. 


This module directly interacts with Geneus with SQL statements rather then using 
the API for better performance in retrieving data. Not using the API also 
reduese the complexity in having to use the completely abstracted table 
structures used by geneus.

This module does not write to the database, nor should it in the future. 
To write to the database you must use the API. See QCMG::DB::QCMGWriter

=head1 REQUIREMENTS

 Data::Dumper
 Carp
 DBI

=cut


use strict;
use warnings;
use Data::Dumper;

use Carp qw( carp croak );
use DBI;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4661 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: GeneusReader.pm 4661 2014-07-23 12:26:01Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;




################################################################################

=head1 METHODS

=over 

=item B<new()>

Constructor - creates a new instance of this class.

Parameters:
 scalar: debug (optional)	Set as 1 for debugging.

Returns:
 a new instance of this class.

=cut

sub new {
	# Creates database connection and sets up query statements.
    my $class   = shift;
    my $debug   = shift;
    
	# Database credentials. 
	# TODO: Use a credential file in the future. 
    my $db_settings = {
        host    => undef,
        db      => undef,
        user    => undef,
        passwd  => undef
    };
    
	# Store the connection and cursor for the life of the class 
    my $self = {
        geneus_conn     => '',
        geneus_cursor   => '',
        debug           => ( $debug ?  1 :  0  )
    };    
	
	# Create connection to Geneus Database
    my $connect_string = 'DBI:Pg:dbname='.$db_settings->{db}.';host='.$db_settings->{host};
    $self->{geneus_conn} = DBI->connect( $connect_string,
                                        $db_settings->{user},
                                        $db_settings->{passwd},
                                        { RaiseError         => 1,
                                          ShowErrorStatement => 0,
                                          AutoCommit         => 0 } ) ||
        croak "can't connect to database server: $!\n";
    $self->{geneus_conn}->{'ChopBlanks'} = 1; # copes with padded spaces if type=char
	
	$self->{sampleIDs_of_query_stmts} 			= {}; # constructed by _sampleIDs_of_query_stmts()
	$self->{mapset_metadata_query_stmts}		= {}; # constructed by _mapset_metadata_query_stmts()
	$self->{microarray_metadata_query_stmts}	= {}; # constructed by _microarray_metadata_query_stmts()
	$self->{sampleIDs}							= (); # List of sampleIDs of the searched Items	

	bless $self, $class;
	
	# Store query statments.
	$self->_sampleIDs_of_query_stmts();
	$self->_mapset_metadata_query_stmts();
	$self->_microarray_metadata_query_stmts();

	return $self;
}


# Closes the database cursor still in use and connection handle.
sub DESTROY {
   my $self = shift;
   if ( $self->{geneus_conn} ) {
	   if ( $self->{geneus_cursor} ) {
		   $self->{geneus_cursor}->finish();
	   }
       $self->{geneus_conn}->disconnect();   
   }
}


################################################################################
# # #
# # 		PRIVATE METHODS
# 
# 	These methods are only to be used by methods of this class
#
################################################################################
# 
# _debug()
#
# If debug has been set on class initiation the debug message will be printed.
#
# Parameters:
#  message: scalar string
#
# Returns:
#  None - May print debug message.

sub _debug {
    my $self = shift;
    my $message = shift;
    
    if ($self->{debug}) {
       print "$message\n";
    }            
}


################################################################################
# 
# _debug_array()
# 
# If debug has been set on class initiation the contents of the array and debug message will be printed.
# 
# Parameters:
#  array: array
# 
# Returns:
#  None - May print debug message.

sub _debug_array {
    my $self = shift;
    my $array = shift;
    	
    my $message = join ', ', @$array;
    if ($self->{debug}) {
        print Dumper $array;
        print "$message\n";
    }            
}


################################################################################
# 
# _sampleIDs_of_query_stmts()
# 
# Sets class varibale "sampleIDs_of_query_stmts" with a hash of required SQL 
# statments needed to determine a the sample name. Metadata is only returned for 
# a given sample.
# 
# Parameters:
#  None
# 
# Returns:
#  None
# 
# TODO: Use sample.id vs sample.name which might a performance increase. 

sub _sampleIDs_of_query_stmts{
	my $self = shift;
		
	$self->{sampleIDs_of_query_stmts} = {
			
			# Returns the Sample IDs of a given microarray
			microarray=> qq{ 
					SELECT DISTINCT sample.name
					FROM qcmg.sample
					INNER JOIN qcmg.arrayscan ON arrayscan.sampleid = sample.sampleid 
					WHERE arrayscan.name = ?
				},
				
			# Returns the Sample IDs of a given Slide
			slide => qq{ 
					SELECT DISTINCT sample.name
					FROM qcmg.sample
					INNER JOIN qcmg.mapset ON mapset.sampleid = sample.sampleid 
					WHERE mapset.name like ?
				},
				
			# Returns the Sample IDs of a given Mapset
			mapset => qq{
					SELECT DISTINCT sample.name
					FROM qcmg.sample
					INNER JOIN qcmg.mapset ON mapset.sampleid = sample.sampleid 
					WHERE mapset.name = ?
				},
				
			# Returns the Sample IDs of a given Primary Library
			primary_library => qq{
					SELECT DISTINCT sample.name
					FROM qcmg.sample
					INNER JOIN qcmg.mapset ON mapset.sampleid = sample.sampleid 
					WHERE mapset.primary_library = ?
				},
			
			# Returns the Sample IDs of a given Donor
			donor => qq{
					SELECT DISTINCT sample.name
					FROM qcmg.sample
					INNER JOIN qcmg.project ON sample.projectid = project.projectid 
					WHERE  replace( replace( project.name, '-', '_'), '.', '_' ) = ?
					},
			
			# Returns the Sample ID for a Sample (Redundant I know)
			sample => qq{
					SELECT DISTINCT sample.name
					FROM qcmg.sample
					INNER JOIN qcmg.project ON sample.projectid = project.projectid 
					WHERE sample.name = ?
				}
			
	};
}


################################################################################
# 
# _sampleIDs_of()
# 
# Returns an array of sample IDs (names) for a given resource.
# 
# Parameters:
#  context:	scalar string 	- Type of resource being searched.
#  			valid types are "slide", "mapset", "primary_library", "donor", "sample", "microarray"
#  
#  resource:	scalar string	- Name of the resource
#  
# Returns:
#  sample_IDs: array	- list of sample IDs (names) found. Or 0 if not found.
# 

sub _sampleIDs_of{
    my $self        = shift;
	my $context	    = shift; 
    my $resource    = shift;
	my $query_stmt	= "";
	
	# Determining which query to run depending on resource type.
	if ( $context eq 'slide' ) {
		$query_stmt = $self->{sampleIDs_of_query_stmts}{slide};
		# Used to match mapset on just the slide name
		$resource = $resource.".%.%"; 
		
	} elsif ( $context eq 'mapset' ){
		$query_stmt = $self->{sampleIDs_of_query_stmts}{mapset};
		
	} elsif ( $context eq 'primary_library' ){
		$query_stmt = $self->{sampleIDs_of_query_stmts}{primary_library};
		
	} elsif ( $context eq 'donor' ){
		$query_stmt = $self->{sampleIDs_of_query_stmts}{donor};
		
	} elsif ( $context eq 'sample' ){
		$query_stmt = $self->{sampleIDs_of_query_stmts}{sample};
	
	}elsif ( $context eq 'microarray' ){
   		$query_stmt = $self->{sampleIDs_of_query_stmts}{microarray};
   	
	}else{
		$self->_debug ("Unknown context $context\n");
		return 0;
	}
	
	# Prepare SQL stament.	
	$self->{geneus_cursor} = $self->{geneus_conn}->prepare($query_stmt);
    $self->{geneus_cursor}->bind_param(1, $resource);
    # Execute SQL statement and collect sample IDs if execute successful.
    if ( $self->{geneus_cursor}->execute() ) {
        my $sampleID;
        $self->{geneus_cursor}->bind_columns(\$sampleID);
        my @sampleIDs_found = ();
        while ( $self->{geneus_cursor}->fetchrow_arrayref() ){
            push(@sampleIDs_found, $sampleID);
        }
		if (@sampleIDs_found) {
			# If sample IDs found, return list.
			return \@sampleIDs_found;
		}        
    }
	$self->_debug ("No Samples found for $context $resource\n");
    return 0;
	
}


################################################################################
# 
# _metadata_query_stmts()
# 
# Sets class varibale "mapset_metadata_query_stmts" with template SQL statment for 
# mapset metadata of a set of Sample ID's and related mapsets.
# 
# Parameters:
#  None
#  
# Returns:
#  None

sub _mapset_metadata_query_stmts {
	my $self 	= shift;
	
	$self->{mapset_metadata_query_stmts} = qq{
		SELECT  
			project.parent_project 									AS "Parent Project", 
	        replace( replace( project.name, '-', '_'), '.', '_' ) 	AS "Project",			
	        project.limsid 											AS "Project LIMS ID",
			project.study_group										AS "Study Group",
	        project.project_open									AS "Project Open",
			sample.name 											AS "Sample Name",
	        sample.limsid 											AS "Sample LIMS ID",
	        sample.sample_code 										AS "Sample Code",
	        sample.material 										AS "Material",
			sample.researcher_annotation							AS "Researcher Annotation",
	        mapset.name  											AS "Mapset",
	        mapset.limsid  											AS "Mapset LIMS ID",
	        mapset.primary_library 									AS "Primary Library", 
		    mapset.library_protocol									AS "Library Protocol", 
			mapset.capture_kit 										AS "Capture Kit",
			coalesce(gfflib.gff_file, gffcapture.gff_file, gffspecies.gff_file) 			AS "GFF File",
		    mapset.sequencing_platform								AS "Sequencing Platform", 
	        mapset.aligner 											AS "Aligner",
			sample.alignment_required  								AS "Alignment Required",
	        sample.species_reference_genome							AS "Species Reference Genome",
			genome.genome_file 										AS "Reference Genome File",
	        mapset.failed_qc 										AS "Failed QC",
			mapset.isize_min 										AS "isize Min", 
			mapset.isize_max 										AS "isize Max",
			mapset.isize_manually_reviewed 							AS "isize Manually Reviewed"
		FROM qcmg.project
		INNER JOIN qcmg.sample on sample.projectid = project.projectid
		INNER JOIN qcmg.mapset on mapset.sampleid = sample.sampleid
		LEFT JOIN qcmg.genome ON genome.species_reference_genome = sample.species_reference_genome 
		LEFT JOIN qcmg.gff gfflib ON mapset.library_protocol ~ gfflib.library_protocol 
		LEFT JOIN qcmg.gff gffcapture ON mapset.capture_kit = gffcapture.capture_kit
		LEFT JOIN qcmg.gff gffspecies ON sample.species_reference_genome = gffspecies.species_reference_genome		
	};
}




################################################################################
# 
# _mapset_metadata_query_stmts()
# 
# Sets class varibale "mapset_metadata_query_stmts" with template SQL statment for 
# mircoarray metadata of a set of Sample ID's and related mapsets.
# 
# Parameters:
#  None
#  
# Returns:
#  None

sub _microarray_metadata_query_stmts {
	my $self 	= shift;
	
	$self->{microarray_metadata_query_stmts} = qq{
		SELECT
		  project.parent_project, 
		  project.name,
		  project.project_limsid
		  project.study_group, 
		  project.project_open,
		  sample.name,
		  sample.sample_limsid 
		  sample.sample_code, 
		  sample.material,
		  sample.researcher_annotation,
		  arrayscan.name, 
		  arrayscan.array_type, 
		  arrayscan.array_class, 
		  arrayscan.array_protocol, 
		  arrayscan.failed_qc  
		FROM qcmg.project
		INNER JOIN qcmg.sample ON project.projectid = sample.projectid 
		INNER JOIN qcmg.arrayscan ON sample.sampleid = arrayscan.sampleid
	};
	
}

################################################################################
# 
# _metadata_query_stmts()
# 
# Executes SQL statment for for metadata for a set of Sample ID's and related mapsets.
# 
# Parameters:
#  resources: array string	Array of sample IDs.
#  
# Returns:
#  execution status: scalar interger.
#  
# Returns the execution status of the SQL statment 1 if succefull. Otherwise 0.

sub _metadata_of_resources{
    my $self 		= shift;
	my $query_stmt	= shift; 
    my $resources   = shift @_;
	
	if ( $resources ) {
	    # Not happy that I'm unable bind these.
	    foreach my $sample ( @$resources ){
	        $sample = "'".$sample."'";
	    }
		# create a string of resources for sql statment.
	    my $samples = join(', ', @$resources);
		
		$query_stmt = $query_stmt." WHERE sample.name in ( $samples ) ";	
	}
	
	# Prepare SQL statment for execution.
    $self->{geneus_cursor} = $self->{geneus_conn}->prepare($query_stmt);
    # Returns execution status.
	return $self->{geneus_cursor}->execute();
}


sub _fetch_mapset_metadata {
    my $self = shift; 
    
	# Assigning variables for binding returned metadata. 
	# Number of varibales must matche the number of column in SQL query $self->{metadata_query_stmts}
    my (	$parent_project, $project, $project_limsid, $study_group, $project_open,
			$sample, $sample_limsid, 
	        $source_code, $material, $researcher_annotation,
	        $mapset, $mapset_limsid, $primary_library, $library_protocol,
	        $capture_kit, $gff_file,  $sequencing_platform,			
			$aligner, $alignment_required, $species_reference_genome, $reference_genome_file,  
			$failed_qc, 
			$isize_min, $isize_max, $isize_manually_reviewed
		);
		# $libary_type,  $sequencing_type,
	# binding must be to a refernce to the varibale
    $self->{geneus_cursor}->bind_columns(
            \$parent_project, \$project, \$project_limsid, \$study_group, \$project_open,
			\$sample, \$sample_limsid, 
            \$source_code, \$material, \$researcher_annotation,
            \$mapset, \$mapset_limsid, \$primary_library, \$library_protocol,
            \$capture_kit, \$gff_file, \$sequencing_platform,
			\$aligner, \$alignment_required, \$species_reference_genome, \$reference_genome_file,  
			\$failed_qc,
			\$isize_min, \$isize_max, \$isize_manually_reviewed
		);
    
    my @metadata = ();
	# foreach row assign column values to a hash.
    while ( $self->{geneus_cursor}->fetchrow_arrayref() ) {
        my $mapset_details = {
            'Project'                   => $parent_project,
            'Donor'                     => $project,
            'Donor LIMS ID'             => $project_limsid,
			'Study Group'				=> $study_group,
			'Project Open'				=> $project_open,
            'Sample'                    => $sample,
            'Sample LIMS ID'            => $sample_limsid,
            'Sample Code'               => $source_code,
            'Material'                  => $material,
            'Researcher Annotation'		=> $researcher_annotation,
			'Mapset'                    => $mapset,
            'Mapset LIMS ID'            => $mapset_limsid,
            'Primary Library'           => ( $primary_library ? $primary_library : "" ),
			'Library Protocol'			=> ( $library_protocol ? $library_protocol : "" ) ,	 
            'Capture Kit'               => $capture_kit,
            'GFF File'                  => $gff_file,
			'Sequencing Platform'		=> $sequencing_platform,
            'Aligner'                   => $aligner,
			'Alignment Required' 		=> ( $alignment_required ?   $alignment_required : 0),
            'Species Reference Genome'  => $species_reference_genome,
			'Reference Genome File'		=> $reference_genome_file,
            'Failed QC'                 => ( $failed_qc ?   $failed_qc : 0),
		    'isize Min'					=> ( $isize_min ?   $isize_min : 0), 
		    'isize Max' 				=> ( $isize_max ?   $isize_max : 0),
		    'isize Manually Reviewed'	=> ( $isize_manually_reviewed ?   $isize_manually_reviewed: 0)			
        };
        push(@metadata, $mapset_details);
    } 
	# returns an array of hashes.   
    return \@metadata;
}




sub _fetch_microarray_metadata {
    my $self = shift; 
    
	# Assigning variables for binding returned metadata. 
	# Number of varibales must matche the number of column in SQL query $self->{metadata_query_stmts}
	my (	$parent_project, $project, $project_limsid, $study_group, $project_open,
			$sample, $sample_limsid, 
	        $source_code, $material, $researcher_annotation,
			$array_name, $array_type, $array_class, $array_protocol, $failed_qc
		);
		# $libary_type,  $sequencing_type,
	# binding must be to a refernce to the varibale
    $self->{geneus_cursor}->bind_columns(
            \$parent_project, \$project, \$project_limsid, \$study_group, \$project_open,
			\$sample, \$sample_limsid, 
            \$source_code, \$material, \$researcher_annotation,
            \$array_name, \$array_type, \$array_class, \$array_protocol, \$failed_qc
		);
    
    my @metadata = ();
	# foreach row assign column values to a hash.
    while ( $self->{geneus_cursor}->fetchrow_arrayref() ) {
        my $array_details = {
            'Parent Project'            => $parent_project,
            'Project'                   => $project,
            'Project LIMS ID'           => $project_limsid,
			'Study Group'				=> $study_group,
			'Project Open'				=> $project_open,
            'Sample'                    => $sample,
            'Sample LIMS ID'            => $sample_limsid,
            'Sample Code'               => $source_code,
            'Material'                  => $material,
            'Researcher Annotation'		=> $researcher_annotation,
			'Array'						=> $array_name,
			'Array Type'				=> $array_type,
			'Array Class'				=> $array_class,
			'Array Protocol'			=> $array_protocol,
			'Failed QC'					=> ( $failed_qc ?   $failed_qc : 0)
        };
        push(@metadata, $array_details);
    } 
	# returns an array of hashes.   
    return \@metadata;
}


# End of PRIVATE METHODS


################################################################################
# # #
# #  	Public Functions 
#
################################################################################

=item B<fetch_metadata()>

Fetches metadata for requested metadata.

Parameters:
 None

Returns:
 Array of hashes. 	- Values are
		'Project', 'Donor', 'Donor LIMS ID', 'Study Group', 
		'Sample', 'Sample LIMS ID', 'Sample Code', 
		'Material', 'Mapset', 'Mapset LIMS ID', 
		'Primary Library', 'Library Type', 'Library Protocol', 
		'Capture Kit', 'GFF File', 
		'Sequencing Type', 'Sequencing Platform', 'Aligner', 
		'Species Reference Genome', 'Reference Genome File',  
		'Alignment Required', 'Failed QC', 
		'isize Min', 'isize Max', 'isize Manually Reviewed',  
		'Expression Array', 'SNP Array', 'Methylation Array',
 
A request for returning metadata can only work after a sucessful return from 
either a particulate resourcer request using B<resource_metadata()> or for 
all resources using B<all_resources_metadata()>

=cut

sub fetch_metadata {
    my $self = shift;
	my $type = shift;
	
	if (! $type) {
		$type = 'mapset';
	}
	if ( $self->{sampleIDs} ) {
		if ( $type eq 'mapset' ) {
			$self->_metadata_of_resources( $self->{mapset_metadata_query_stmts}, $self->{sampleIDs} );
			return $self->_fetch_mapset_metadata();
		
		}elsif ( $type eq 'microarray' ){
			$self->_metadata_of_resources( $self->{microarray_metadata_query_stmts},  $self->{sampleIDs} );
			return $self->_fetch_microarray_metadata();
		
		}else{
			$self->_metadata_of_resources( $self->{mapset_metadata_query_stmts}, $self->{sampleIDs} );
			return $self->_fetch_mapset_metadata();
		}
	}else{
	
	}
}



################################################################################

=item B<all_resources_metadata()>

Requests for metadata of all metadata in Geneus.

Parameters:
 None

Returns:
 execution status: scalar interger.
 
Returns the execution status of the SQL statment 1 if succefull. Otherwise 0.

=cut

sub all_resources_metadata{
    my $self = shift;
    # Try to get all of the metadata and report success/failure
    if ( $self->_metadata_of_resources() ) {
        return 1; # True
    }
    else {
        return 0;
        # croak $sth->errstr;
    }
}


################################################################################

=item B<resource_metadata()>

Requests for metadata of for a particular resource.

Parameters:
 resource_type: scalar string - Type of resource being searched.
 			valid valuse are "slide", "mapset", "primary_library", "donor", "sample"
 
 resource:	scalar string	- Name of the resource

Returns:
 execution status: scalar interger.
 
Returns the execution status of the SQL statment 1 if succefull. Otherwise 0.
Metadata will returned will be for all All samples that the resource matches 
useing the method B<fetch_metadata()>.  
IE. A slide might be associcated with multiple samples. The results returned will
be all resoureces for all the samples. 

If you intend on using this method, It is recommended that you use the module 
QCMG::DB::Metadata instead. It gives you a better interface for obtaining 
specific metadata or sets of metadata.

=cut

sub resource_metadata{
    my $self            = shift;
    my $resource_type   = shift;
    my $resource        = shift;
	
    # Sample
    if ( $resource_type eq "sample") {
        $self->_debug("Sample");
		$self->{sampleIDs} = $self->_sampleIDs_of('sample', $resource);
    	
	# Donor
    }elsif ( $resource_type eq "donor") {
        $self->_debug("donor");
        $self->{sampleIDs} = $self->_sampleIDs_of('donor', $resource);
    
    # Library
    }elsif ($resource_type eq "primary_library") {
		$self->_debug("Library");
        $self->{sampleIDs} = $self->_sampleIDs_of('primary_library', $resource);

    # Mapset
    }elsif ($resource_type eq "mapset") {
		$self->_debug("Mapset");
        $self->{sampleIDs} = $self->_sampleIDs_of('mapset', $resource);
    
    # Slide
    }elsif ($resource_type eq "slide") {
        $self->_debug("Slide");
        $self->{sampleIDs} = $self->_sampleIDs_of('slide', $resource);
    
    # Microarray
    }elsif ($resource_type eq "microarray") {
        $self->_debug("Mircoarray");
        $self->{sampleIDs} = $self->_sampleIDs_of('microarray', $resource);
	        
    # Everything Else
    }else{
        $self->_debug("Nothing");
        $self->{sampleIDs} = ();
		return 0;
    }
	
	
    if ( $self->{sampleIDs} != 0 ){
		#print Dumper $sampleIDs;
	    $self->_debug_array( $self->{sampleIDs} );
	    #if ( $self->_metadata_of_resources( $self->{sampleIDs} ) ) {
	    	return 1; # True
			#}
    }
	    
    return 0;
}


##
## More specific queries (These are for you Lynn.)
## 
## But are needed 
## 


################################################################################
# # #
# #			SPECIAL CASE QUERIES 
#
#	These don't fit the data structure of QCMG::DB::Metadata module.
#	However they are required for legacy sequencing platforms. 
# 
#  	Some of these functions require either different databases such as the
#   Geneus schema directly and hence have their own connection to the database 
# 	and not the connection handle used by the class.
# 
################################################################################

=item B<solid_primaries()>

Returns details on the location of primary files from a given SOLiD slide.

Parameters:
 resource:	scalar string	- Name of a SOLiD slide.
 
Returns:
 Array of hashes or 0 if no results.
 		values being: 'slide_name', 'bc_primary_reads', 
		'f3_primary_reads', 'f5_primary_reads', 'r3_primary_reads'
 

This method has is own connection to the Geneus schema as this information is
to specific to the SOLiD 4 and earlier to be included in the QCMG schema that 
the rest of this class uses. So there is a small performance hit every time 
this method is called to connect to the database.

=back 

=cut

sub solid_primaries {
    my $self        = shift; 
    my $resource    = shift;
    
    my @sections = split /\./, $resource;
    $resource = $sections[0];
    
	# SQL statment for finding SOLiD primary_reads.
    my $query_stmt = qq{ 
    SELECT 
        a.artifactid, a.name AS "Slide Name", 
        max( CASE WHEN aus.rowindex = 0 THEN aus.text0 ELSE NULL::text END) AS "f3_primary_reads", 
        max( CASE WHEN aus.rowindex = 0 THEN aus.text1 ELSE NULL::text END) AS "f5_primary_reads", 
        max( CASE WHEN aus.rowindex = 0 THEN aus.text2 ELSE NULL::text END) AS "r3_primary_reads", 
        max( CASE WHEN aus.rowindex = 0 THEN aus.text3 ELSE NULL::text END) AS "bc_primary_reads"
    FROM artifact a
        LEFT JOIN artifactudfstorage aus ON a.artifactid = aus.artifactid
        LEFT JOIN artifact_sample_map asm ON a.artifactid = asm.artifactid
    WHERE a.processoutputtypeid IN (42)
        -- 42 is the processoutputtypeid for Results Directory (SOLiD Sequencing Run output)
        AND a.name = ?
    GROUP BY a.artifactid, a.name
    };
    
    
     # Custom database connection than used by class.
     my $db_settings = {
         host    => undef,
         db      => undef,
         user    => undef,
         passwd  => undef
     };
     
     my $geneus = { geneus_conn => '',  geneus_cursor   => '' };    
     my $connect_string = 'DBI:Pg:dbname='.$db_settings->{db}.';host='.$db_settings->{host};
     $geneus->{geneus_conn} = DBI->connect( $connect_string,
                                         $db_settings->{user},
                                         $db_settings->{passwd},
                                         { RaiseError         => 1,
                                           ShowErrorStatement => 0,
                                           AutoCommit         => 0 } ) ||
         croak "can't connect to database server: $!\n";
     $geneus->{geneus_conn}->{'ChopBlanks'}=1; # copes with padded spaces if type=char
    
	 # Prepare statement
     $geneus->{geneus_cursor} = $geneus->{geneus_conn}->prepare($query_stmt);
     $geneus->{geneus_cursor}->bind_param(1, $resource);
     
	  # Execute SQL statement and collect results if execute successful.
     if ( $geneus->{geneus_cursor}->execute() ){
         my ($artifactid, $slide_name, $f3_primary_reads, $f5_primary_reads, $r3_primary_reads, $bc_primary_reads);
         $geneus->{geneus_cursor}->bind_columns(\$artifactid, \$slide_name, \$f3_primary_reads, \$f5_primary_reads, \$r3_primary_reads, \$bc_primary_reads);
     
         my $solid_primaries = ();
         while ( $geneus->{geneus_cursor}->fetchrow_arrayref() ) {
             my $primary_details = {
                 'slide_name'           => ( $slide_name ?          $slide_name : '' ),
                 'f3_primary_reads'     => ( $f3_primary_reads ?    $f3_primary_reads : '' ),
                 'f5_primary_reads'     => ( $f5_primary_reads ?    $f5_primary_reads : '' ),
                 'r3_primary_reads'     => ( $r3_primary_reads ?    $r3_primary_reads : '' ),
                 'bc_primary_reads'     => ( $bc_primary_reads ?    $bc_primary_reads : '' )
             };
             push(@$solid_primaries, $primary_details);
         }
		 
	     if ( $geneus->{geneus_conn} ) {
	         $geneus->{geneus_conn}->disconnect();   
	     }
		 
		 # return array of hashes.
         return $solid_primaries;
     }

    return 0;
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
