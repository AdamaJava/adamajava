package QCMG::DB::Metadata;

##############################################################################
#
#  Module:   QCMG::DB::Metadata.pm
#  Creator:  Matthew J Anderson
#  Created:  2012-02-10
# 
# This class is a data structure for metadata on sequencing runs and 
# microarrays from the QCMG schema. 
# 
# It provides and interface to interact with
# metadata attributes and relations of Parent Projects, Projects, Samples,
# Sequencing Slides, Sequencing Mapsets, Mircoarray Assays and Mergedbams 
# (merged mapsets).
# 
#  $Id: Metadata.pm 4661 2014-07-23 12:26:01Z j.pearson $:
# 
##############################################################################


=pod

=head1 NAME

QCMG::DB::Metadata -- Common functions for obtaining metadata attributes and relations.

=head1 SYNOPSIS
 
 my $metadata = QCMG::DB::Metadata->new()
 my $resource = "ICGC-ABMP-20091203-10-ND";
 
 if ( $metadata->find_metadata("sample", $resource) ){
    foreach my $mapset ( @{$metadata->sample_mapsets()} ){
        print "\tMapset: ".$mapset."\n";
    }
 }

=head1 DESCRIPTION
 TODO


=head1 REQUIREMENTS
 
 Carp
 DBI
 Data::Dumper
 QCMG::DB::GeneusReader
 QCMG::Util::QLog 

=cut

use strict;                 # Good practice
use warnings;               # Good practice

use Data::Dumper;           # Perl core module
use Carp qw( carp croak );  # Perl core module

use QCMG::DB::QcmgReader;   # QCMG - "QCMG" schema datbase module 
use QCMG::Util::QLog;       # QCMG - logging module


use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4661 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: Metadata.pm 4661 2014-07-23 12:26:01Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 scalar: debug (optional)			Set as 1 for debugging.
 
Returns:
 a new instance of this class.

=cut

sub new {
    my $class           = shift;
    my $debug           = shift;
    
    my $self = {
        debug           => ( $debug ? $debug : 0 ),
        LIMS 			=> QCMG::DB::QcmgReader->new(),
        
        parent_projects => {},
        mergedbams  => {},  
        selected    => {},  # Keeping track of which metadata object has been requested
        metadata    => {},  # <sample> => {<metadata_object>}   - Metadata of project
                          # <metadata_object> = { 
                          #   parent_project, project, mergedbam, 
                          #   primary_library_mapsets => { <primary_library> => (<mapsets>, ) }, 
                          #   mapset_libaries => { <mapset> => <primary_library> }
                          # }
                  
        # Index to megedbam
        mergedbam_to_mapset_index         => {},
        sample_to_mergedbams_index        => {},
        project_to_mergedbam_index        => {},
                                
        # Index to Sample metadata
        project_to_sample_index         	=> {}, # <project>   	      => ( <sample>, ) - index of project to samples
        sample_index        	            => {}, # <sample>  		      => ( <sample>, ) - index of project to samples
        primaryLibrary_to_sample_index    => {}, # <primary_library>  => <sample> - index of primary_library to sample
        mapset_to_sample_index        	  => {}, # <mapset>  		      => <sample>  - index of mapset to sample
        slide_to_mapset_index 		        => {}, # <slide> 		        => (mapset, )
        microarray_to_sample_index        => {}  # <microarray> 	    => <sample>  - index of microarray to sample
    };
    
    bless $self, $class;
    
    $self->_fetch_parent_projects();
    
    return $self;
}

# class deconstruction 
sub DESTROY {
   my $self = shift;
}



#########################################################################
# # #
# #      Private Functions -- Creation of data structures
#
#########################################################################
 
=cut _reset_selected()

Resets selected sample

Parameters: None

Returns: None
=cut
sub _reset_selected {
    my $self = shift;
    
    $self->{selected} = {};
}


=cut _new_mapsets_details()

Builds a hash of mapset details for the module's data structure.

Parameters:
 properties: hash - mapset details

Returns:
 mapset_detail: hash - mapset details
=cut
sub _new_mapsets_details {
  my $self                        = shift;
  my $properties                  = shift;
    
  my $mapsets_details = {
    mapset                      => $properties->{mapset},                  
    lims_id                     => $properties->{mapset_limsid},           
    primary_library             => $properties->{primary_library},         
    library_protocol            => $properties->{library_protocol},        
    capture_kit                 => $properties->{capture_kit},             
    gff_file                    => $properties->{gff_file},                
    sequencing_platform         => $properties->{sequencing_platform},     
    aligner                     => $properties->{aligner},
    species_reference_genome    => $properties->{species_reference_genome},
    genome_file                 => $properties->{reference_genome_file},
    alignment_required          => $properties->{alignment_required},      
    failed_qc                   => $properties->{failed_qc},               
    isize_min                   => $properties->{isize_min},               
    isize_max                   => $properties->{isize_max},               
    isize_manually_reviewed     => $properties->{isize_manually_reviewed}  
  };
    
  return $mapsets_details;
}


=cut _new_array_details()

Builds a hash of microarray details for the module's data structure.

Parameters:
  properties: hash - microarray details

Returns:
 Hash of the details of a microarray.
=cut
sub _new_array_details {
  my $self				  = shift;
  my $properties    = shift;

	my $array_details = {
		array_name			=> $properties->{array}, 
		array_type			=> $properties->{array_type}, 
		array_class			=> $properties->{array_class}, 
		array_protocol	=> $properties->{array_protocol},	
		failed_qc			  => $properties->{failed_qc}
	};
	
	return $array_details;
}


=cut _new_mergedbam_details()

 Builds a hash of mergedbam details for the module's data structure.

Parameters:
 properties: hash - mergedbam details

Returns:
 mergedbam_details: hash - mergedbam details
=cut
sub _new_mergedbam_details {
  my $self                = shift;
  my $properties          = shift;
  my $mergedbam_mapsets   = shift;
  my $mapsets             = ();
  
  foreach my $item ( @{$mergedbam_mapsets} ){
    push @{$mapsets}, $item->{mapset};
  }

  my $mergedbam_details = {
    mergedbam             => $properties->{mergedbam},         
    material              => $properties->{material},          
    sample                => $properties->{sample},            
    sample_code           => $properties->{sample_code},       
    library_protocol      => $properties->{library_protocol},  
    capture_kit           => $properties->{capture_kit},       
    aligner               => $properties->{aligner},           
    sequencing_platform   => $properties->{sequencing_platform},
    mapsets               => $mapsets
  };
    
  return $mergedbam_details;
}


=cut _new_parent_projects()

 Builds a hash of parent project details for the module's data structure.

Parameters:
 properties: hash - parent project metadata

Returns:
 parent_project: hash - parent project details
=cut
sub _new_parent_projects {
  my $self        = shift;
  my $properties  = shift;
  
  my $parent_project = { 
        name			      =>  $properties->{parent_project},
        path			      =>  $properties->{parent_project_path},
        label			      =>  $properties->{parent_project_label},
        project_prefix	=>  $properties->{project_prefix},
        array_samples   =>  $properties->{array_samples}
  };
  
  return $parent_project;
}


=cut _new_meta_object()

Creates a new metadata object for a sample.

Parameters:
 properties: hash - parent_project, project and sample metadata

Returns:
 meta_object: hash - A data structure for a sample.
=cut
sub _new_meta_object {
  my $self          = shift;
  my $properties    = shift;
	
  my $meta_object = {
    # TODO: Remove Parent Project ?
    # FIXME This parent_project is now redundant 
    parent_project => { 
      name			      => $properties->{parent_project},
      path			      => $properties->{parent_project_path},
      label			      => $properties->{parent_project_label},
      project_prefix  => $properties->{project_prefix}
    },
    project => { 
      name      		=> $properties->{project},
      lims_id    	  => $properties->{project_limsid},
      study_groups  => $properties->{study_group},
      project_open	=> $properties->{project_open}
    },
    sample => { 
      name          => $properties->{sample},
      lims_id       => $properties->{sample_limsid},
      material      => $properties->{material},
      sample_code   => $properties->{sample_code},
      researcher_annotation	=> $properties->{researcher_annotation}
    },
    primary_library_mapsets => {},          # { "primary_library" => [mapsets, ] } 
    mapset_details 			=> {},  # { <mapset> => {
                                #     lims_id           => <lims_id>,
                                #     primrary_library  => <primrary_library>
                                #     library_type      => <library_type>,
                                #     capture_kit       => <capture_kit>,
                                #     sequencing_type   => <sequencing_type>,
                                #     aligner           => <aligner>,
                                #	  alignment_required => 0
                                #     failed_qc         => 0
                                #	  isize_min			=> 0			
                                #	  isize_max 		=> 0	
                                #	  isize_manually_reviewed => 0
                                #   }
                                # }
    microarray_details => {},	#$micro_arrays,  
          										# { expression_array 	=> <expression_array>,
          		  							#	  snp_array			=> <snp_array>,
          										#	  methylation_array	=> <methylation_array>
          										# }
	};
    
  return $meta_object;
}


=cut _build_parent_project_object()

Creates parent project objects for all parent projects 

Parameters:
 resources_metadata: hash - parent projects details

Returns: None
=cut
sub _build_parent_project_object{
  my $self                = shift;
  my $resources_metadata  = shift;
  
  foreach my $resource ( @$resources_metadata ){    
    # Create a new parent project data structure
    my $meta_object = $self->_new_parent_projects($resource);
    $self->{parent_projects}{ $resource->{parent_project} } = $meta_object;         
  }
}


=cut _build_sample_meta_object()

Creates metadata object for a sample (used internally) 

Parameters:
  resources_metadata: hash - mapset metadata

Returns: None
=cut
sub _build_sample_meta_object{
  my $self                = shift;
  my $resources_metadata  = shift;

  foreach my $resource ( @$resources_metadata ){    
    # Create a new metadata data structure based around a sample
    if ( ! exists $self->{metadata}{ $resource->{sample} }) {
      my $meta_object = $self->_new_meta_object($resource); 
      $self->{metadata}{ $resource->{sample} } = $meta_object;
      $self->_addto_project_to_sample_index( $resource->{project},  $resource->{sample} );
    }        
  }
}


=cut _build_mapset_meta_object()

Creates metadata object for a mapset (used internally) in realtions to a sample

Parameters:
 resources_metadata: hash - mapset metadata

Returns: None
=cut
sub _build_mapset_meta_object{
  my $self                = shift;
  my $resources_metadata  = shift;
    
  foreach my $resource ( @$resources_metadata ){    
      
    # Create a new metadata data structure based around a sample
    if ( ! exists $self->{metadata}{ $resource->{sample} }) {
      $self->_new_sample_meta_object($resource); 
    }
		my $meta_object = $self->{metadata}{ $resource->{sample} };
        
    # Add a mapset details to the sample metadata data structure 
    if ( ! exists $meta_object->{mapset_details}{ $resource->{mapset} } ){
      my $mapsets_details = $self->_new_mapsets_details($resource);
      $meta_object->{mapset_details}{ $resource->{mapset} } = $mapsets_details;        
    }
    
        
    # FIXME: is mapset_libaries acctually needed?
    if ( ! exists $meta_object->{mapset_libaries}{ $resource->{mapset} } ){
        $meta_object->{mapset_libaries}{ $resource->{mapset} } = $resource->{primary_library};
    }
        
        
		if ( ! $resource->{primary_library} ) {
      # skip indexing
    }
    elsif ( ! exists $meta_object->{primary_library_mapsets}{ $resource->{primary_library} } ) {
      my $mapsets = [ $resource->{mapset} ];
      $meta_object->{primary_library_mapsets}{ $resource->{primary_library} } = $mapsets;
    }
    else{
      my $mapsets = $meta_object->{primary_library_mapsets}{ $resource->{primary_library} };
      if( ! grep ( /$resource->{mapset}/, @$mapsets) ) {
        push(@$mapsets, $resource->{mapset} );
      } 
    }
        
    # Create indexes for faster retival of data. 
    # By which, removing the need to interate over the data structure to find relations.
    # $self->_addto_project_to_sample_index         ( $resource->{project},  $resource->{sample} );
    $self->_addto_slide_to_mapset_index( $resource->{mapset} );
    $self->_addto_primaryLibrary_to_sample_index( $resource->{primary_library}, $resource->{sample} );
    $self->_addto_mapset_to_sample_index( $resource->{mapset}, $resource->{sample} );
  }
}   


=cut _build_array_meta_object()

Creates metadata object for a microarray (used internally) in realtions to a sample

Parameters:
 resources_metadata: hash - microarray metadata

Returns: None
=cut
sub _build_array_meta_object{
  my $self                = shift;
  my $resources_metadata  = shift;
  
  foreach my $resource ( @$resources_metadata ){
        
    # Create a new metadata data structure based around a sample
    if ( ! exists $self->{metadata}{ $resource->{sample} }) {
      $self->_debug( " meta object does not exits\n" );
      $self->_new_sample_meta_object($resource); 
    }

    my $meta_object = $self->{metadata}{ $resource->{sample} };
    
    # Add a microarray details to the sample metadata data structure 
    if ( ! exists $meta_object->{microarray_details}{ $resource->{array} } ){
      my $microarray_details = $self->_new_array_details($resource);
      $meta_object->{microarray_details}{ $resource->{array} } = $microarray_details; 
    } 
            
    # Create indexes for faster retival of data. 
    # By which, removing the need to interate over the data structure to find relations.
    #$self->_addtoproject_to_sample_index    ( $resource->{project},  $resource->{sample} );
    $self->_addto_mircoarray_to_sample_index( $resource->{array},  $resource->{sample} );
  }
    
} 



=cut _build_mergedbam_meta_object()

Creates metadata object for a mergedbam (used internally) in realtions to a project

Parameters:
 resources_metadata: hash - mergedbam metadata

Returns: None
=cut
sub _build_mergedbam_meta_object{
  my $self                = shift;
  my $resources_metadata  = shift;
  
  foreach my $resource ( @$resources_metadata ){            
    if ( ! exists $self->{mergedbam}{ $resource->{mergedbam} }) {
      # collect mapsets of mergedbam
      my $mergedbam_mapsets = $self->{LIMS}->mapsets_in_mergedbam($resource->{mergedbam}); 
      $self->{mergedbam}{ $resource->{mergedbam} } = $self->_new_mergedbam_details($resource, $mergedbam_mapsets);
      # create index to mergedbam
      $self->_addto_sample_to_mergedbam_index($resource->{sample}, $resource->{mergedbam});
      $self->_addto_project_to_mergedbam_index($resource->{project}, $resource->{mergedbam});
    }
  }
}   



#########################################################################
# # #
# #      Private Functions -- Add indexes
#
#########################################################################

=cut _addto_parent_project_index()

TODO: Creates an index entry of a pproject to many samples

TODO: Parameters:
 project: scalar string - 
 sample: scalar string - 
TODO: Returns: None
=cut
sub _addto_parent_project_index {
  # TODO: _addto_parent_project_index()
    my $self    = shift;
    my $project = shift;
    my $sample  = shift;
    
    if ( ! exists $self->{project_index}{ $project } ) {
        $self->{project_index}{ $project } = [ $sample ];
        
    }else{
        my $project_samples = $self->{project_index}{ $project };
        if( ! grep ( /$sample/, @$project_samples) ) {
            push(@$project_samples, $sample );
        }
    }
}

=cut _addtoproject_to_sample_index()

Creates an index entry of a project to many samples

Parameters:
 project: scalar string - project name
 sample: scalar string - sample name

Returns: None
=cut
sub _addto_project_to_sample_index {
  my $self    = shift;
  my $project = shift;
  my $sample  = shift;
  
  if ( ! exists $self->{project_index}{ $project } ) {
      $self->{project_index}{ $project } = [ $sample ];
      
  }else{
      my $project_samples = $self->{project_index}{ $project };
      if( ! grep ( /$sample/, @$project_samples) ) {
          push(@$project_samples, $sample );
      }
  }
}


=cut _addto_slide_to_mapset_index()

Adds an index entry of a slide to many mapsets. 
Silde name is determined from mapset name.

Parameters:
 $mapset: scalar string - mapset name

Returns: None
=cut
sub _addto_slide_to_mapset_index {
  my $self    = shift;
  my $mapset  = shift;
  
  my @sections = split /\./, $mapset;
  my $slide = $sections[0];
  
  if ( ! exists $self->{slide_to_mapset_index}{ $slide } ) {
    $self->{slide_to_mapset_index}{ $slide } = [ $mapset ] ;
    $self->_debug("$mapset added to a new index for $slide \n");
  }
  else{
    my $slide_mapsets = $self->{slide_to_mapset_index}{ $slide };
    if( ! grep ( /^$mapset\$/, @$slide_mapsets) ) {
      push(@{$slide_mapsets}, $mapset );
      #$self->_debug("$mapset added to an existing index for $slide \n");
		}
    else{
      $self->_debug("$mapset NOT added to an existing index for $slide \n");
		}
  }
}


=pod

B<_addto_primaryLibrary_to_sample_index()>

Adds an index entry of a primary_library to a sample

Parameters:
 primary_library: scalar string - primary library name
 sample: scalar string - sample name

Returns: None
=cut
sub _addto_primaryLibrary_to_sample_index {
  my $self            = shift;
  my $primary_library = shift;
  my $sample          = shift;
    
  if ( ! exists $self->{primaryLibrary_to_sample_index}{ $primary_library } ) {
    $self->{primaryLibrary_to_sample_index}{ $primary_library } = $sample ;
  }
}


=cut _addto_mapset_to_sample_index()

Adds an index entry of a mapset to a sample

Parameters:
 mapset: scalar string - mapset name
 sample: scalar string - sample name

Returns: None
=cut
sub _addto_mapset_to_sample_index {
  my $self    = shift;
  my $mapset  = shift;
  my $sample  = shift;
  
  if ( ! exists $self->{mapset_to_sample_index}{ $mapset } ) {
    $self->{mapset_to_sample_index}{ $mapset } = $sample ;
  }
}


=cut _addto_mircoarray_to_sample_index()

Adds an index entry of a mircoarray to a sample

Parameters:
 mircoarray: scalar string - mircoarray name
 sample: scalar string - sample name

Returns: None
=cut
sub _addto_mircoarray_to_sample_index {
  my $self        = shift;
  my $mircoarray  = shift;
  my $sample      = shift;
  
  if ( ! exists $self->{microarray_to_sample_index}{ $mircoarray } ) {
      $self->{microarray_to_sample_index}{ $mircoarray } = $sample ;
  }
}



=cut TODO _addto_mergedbam_to_mapset_index()

Adds an index entry of a mergedbam to a sample

Parameters:
 mergedbam: scalar string - $mergedbam name
 mapsets: array string - sample name

Returns: None
=cut
sub _addto_mergedbam_to_mapset_index {
  my $self        = shift;
  my $mergedbam   = shift;
  my $mapsets     = shift;
  
  if ( ! exists $self->{mergedbam_to_mapset_index}{ $mergedbam } ) {
    foreach my $mapset ( $mapsets){
      print  "$mergedbam -> $mapset\n";
    } 
    # TODO $self->{mergedbam_to_mapset_index}{ $mergedbam } = ?
  }
}


=cut _addto_sample_to_mergedbam_index()

Adds an index entry of a sample to mergedbams

Parameters:
 sample: scalar string - sample name
 mergedbam: scalar string - mergedbam name

Returns: None
=cut
sub _addto_sample_to_mergedbam_index {
  my $self        = shift;
  my $sample      = shift;
  my $mergedbam   = shift;
  
  if ( ! exists $self->{sample_to_mergedbams_index}{ $sample } ) {
      $self->{sample_to_mergedbams_index}{ $sample } = [ $mergedbam ];
  }
  else{
    my $sample_mergedbams = $self->{sample_to_mergedbams_index}{ $sample };
    if( ! grep ( /$mergedbam/, $sample_mergedbams) ) {
        push(@{$sample_mergedbams}, $mergedbam );
    }
  }
}


=cut _addto_project_to_mergedbam_index()

Adds an index entry of a project to a mergedbam

Parameters:
 project: scalar string - project name
 mergedbam: scalar string - mergedbam name

Returns: None
=cut
sub _addto_project_to_mergedbam_index {
  my $self          = shift;
  my $project       = shift;
  my $mergedbam     = shift;
  
  if ( ! exists $self->{project_to_mergedbams_index}{ $project } ) {
    $self->{project_to_mergedbams_index}{ $project } = [ $mergedbam ];
  }
  else{
    my $project_mergedbams = $self->{project_to_mergedbams_index}{ $project };
    if( ! grep ( /$mergedbam/, $project_mergedbams) ) {
        push(@{$project_mergedbams}, $mergedbam );
    }
  }
}






#########################################################################
# # #
# #      Private Functions -- Populate data structures.
#
#########################################################################

=cut _addto_parent_project_object()

Adds a list of projects to a parent project object

Parameters:
 parent_project: scalar string - project name
 parent_project_projects: array string - list of project names

Returns: None
=cut
sub _addto_parent_project_object {
  my $self                      = shift;
  my $parent_project            = shift; 
  my $parent_project_projects   = shift; 

  if ( exists $self->{parent_projects}{$parent_project} ) {
    if ( scalar @{$parent_project_projects} < 1 ) {
      $self->{parent_projects}{$parent_project}{projects} = [];
    }
    else{
      $self->{parent_projects}{$parent_project}{projects} = ();
      foreach my $project ( @{$parent_project_projects} ){
        push @{$self->{parent_projects}{$parent_project}{projects}}, $project->{project};
      }
    }
  }
  else{
    qlogprint( {l=>'WARN'}, "parent_project $parent_project does not exist\n");
  }
  
}


=cut _fetch_parent_projects()

Fetches parent project details for population of data structures.

Parameters: None

Returns:
 Returns 1 (successful) if details are found. Otherwise 0.

=cut
sub _fetch_parent_projects {
  my $self            = shift; 
  my $details_found   = 0;
  
  $self->_debug("Searching for Parent Projects \n");
  $self->_debug("Fetching PARENT PROJECTS metadata ... ");
  if ( my $parent_projects = $self->{LIMS}->parentprojects() ){
    $self->_debug("Collected\n");
    $self->_build_parent_project_object($parent_projects);
    $details_found = 1;
  }
  else{
    $self->_debug("Failed\n");
  }
  
  return $details_found;
}


=cut _fetch_projects_of_parent_projects()

Fetches parent project details for population of data structures.

Parameters: None

Returns:
 Returns 1 (successful) if details are found. Otherwise 0.

=cut
sub _fetch_projects_of_parent_projects {
  my $self            = shift;
  my $parent_project  = shift;
  my $details_found   = 0;
  
  $self->_debug("Searching for Parent Projects \n");
  $self->_debug("Fetching PARENT PROJECTS projects ... $parent_project");
  if ( my $parent_project_projects = $self->{LIMS}->projects_of_parentproject($parent_project) ){
    $self->_debug("Collected\n");
    $self->_addto_parent_project_object($parent_project, $parent_project_projects);
    $details_found = 1;
  }
  else{
    $self->_addto_parent_project_object($parent_project, []);
    $self->_debug("Failed\n");
  }
  
  return $details_found;
}


=cut _fetch_metadata()

Fetches mapsets, microarray and mergedbam matadata for population of data structures.

Parameters:
 resource_type: scalar string - Options are: "sample", "mapset", "microarray" or "mergedbam"
 resource: scalar string - name of resources

Returns:
 Returns 1 (successful) if metadata is found. Otherwise 0.

=cut
sub _fetch_metadata {
  my $self            = shift; 
  my $resource_type   = shift;
  my $resource        = shift;
  
  $self->_debug("Searching for metadata for $resource_type:  $resource \n");
  
  my $metadata_found  = 0;
  
  if ( $self->{LIMS}->resource_metadata($resource_type, $resource) ){
	  
    $self->_debug("Fetching SAMPLE metadata for $resource... "); 
	  my $sample_metadata = $self->{LIMS}->fetch_metadata('sample');
	  $self->_debug("Collected\n"); 
    if ( $sample_metadata ){
      $self->_build_sample_meta_object($sample_metadata);
      $metadata_found = 1;
    }
      
	  $self->_debug("Fetching MAPSET metadata for $resource... "); 
	  my $mapset_metadata = $self->{LIMS}->fetch_metadata('mapset');
	  $self->_debug("Collected\n"); 
    if ( $mapset_metadata ){
      $self->_build_mapset_meta_object($mapset_metadata);
      $metadata_found = 1;
    }
	
	  $self->_debug("Fetching MICROARRAY for metadata $resource... "); 
    my $microarray_metadata = $self->{LIMS}->fetch_metadata('microarray');
    $self->_debug("Collected\n"); 
	  if ( $microarray_metadata ){
      $self->_build_array_meta_object($microarray_metadata);
      $metadata_found = 1;
    }
    
  	$self->_debug("Fetching MERGEDBAM metadata for $resource... "); 
  	my $merged_metadata = $self->{LIMS}->fetch_metadata('mergedbam');
  	$self->_debug("Collected\n"); 
    if ( $merged_metadata ){
      $self->_build_mergedbam_meta_object($merged_metadata);
      $metadata_found = 1;
    }
  }
  
  return $metadata_found;
}



=cut _first_slide_mapset_sample()

CAUTION: Avoid useing this function as this will just cause confusion!

Returns the first mapset in a list of mapsets for a slide. 

Parameters:
 slide: scalar string - slide name

Returns:
 Returns 1 (successful) if metadata is found. Otherwise 0.

=cut
sub _first_slide_mapset_sample{
    my $self    = shift;
    my $slide   = shift;
    
    qlogprint( {l=>'CAUTION'}, "Avoid useing _first_slide_mapset_sample() as this will just cause confusion!\n");
    my $mapsets = $self->slide_mapsets($slide);
    my $first_mapset = @$mapsets[0];
    return $self->mapset_sample($first_mapset);
}




#########################################################################
#########################################################################
# # # # #
# # # #
# # #      Public Functions
# #
# 
#########################################################################
#########################################################################


=pod

B<prefetch_all_metadata()>

Pre fetches all mapsets, microarray and mergedbam matadata for population
of data structures allows for faster processing of data. Rather than 
having to wait for each request for metadata.

This us most useful when when wanting to iterating over the metadata of 
many mapsets etc.

Parameters:
 None

Returns:
 Returns 1 (successful) if metadata is found. Otherwise 0.

=cut
sub prefetch_all_metadata{
  my $self  = shift;     
  my $metadata_found  = 0;
  
  if ( $self->{LIMS}->all_resources_metadata() ){
    my $sample_metadata = $self->{LIMS}->fetch_metadata('sample');
    if ( $sample_metadata ){
      $self->_build_sample_meta_object($sample_metadata);
      $metadata_found = 1;
    }
    
    my $mapset_metadata = $self->{LIMS}->fetch_metadata('mapset');
    if ( $mapset_metadata ){
      $self->_build_mapset_meta_object($mapset_metadata);
      $metadata_found = 1;
    }

    my $microarray_metadata = $self->{LIMS}->fetch_metadata('microarray');
    if ( $microarray_metadata ){
      $self->_build_array_meta_object($microarray_metadata);
      $metadata_found = 1;
    }

    my $mergedbam_metadata = $self->{LIMS}->fetch_metadata('mergedbam');
    if ( $mergedbam_metadata ){
      $self->_build_mergedbam_meta_object($mergedbam_metadata);
      $metadata_found = 1;
    }
  }
        
  return $metadata_found; 
}


#
#   Function does not return metadata. Use class functions.
#   
#   Populates class object with metadata from the LIMS of the 
#   requested resource.
# -------------------------------------------------------------------
=pod

B<find_metadata()>

# The method to call to get the metadata from the LIMS and creates
# the class's data objects.

Populates the data structure with metadata for the resource requested.
It does not return metadata, only if metadata was found for the resource or not. 

Use class functions to get the metadata you are after.

Parameters:
 resource_type: scalar string	- Options are: project, sample, library, 
                                mapset, slide, microarray, mergedbam
 resource: scalar string	- resource name

Returns:
 Returns 1 (successful) if metadata is found. Otherwise 0.

=cut
sub find_metadata{
  my $self            = shift;
  my $resource_type   = shift;
  my $resource        = shift;
  my $sample          = "";
    
  $self->_reset_selected();

	# Check (using indexes) if the resource's metadata has already been retived.
	# If metadata does not exist, search LIMS for metadata. 
	# Returns the existance of a sample for the desired resource.	
  
  # Project
  if ($resource_type eq "project"){
    # FIXME fix this - does not work for a project
    if ( exists $self->{metadata}{$resource} ){
      $sample = $resource;
    }else{
      if ( $self->_fetch_metadata( $resource_type, $resource ) ) {
        my $project_samples = $self->project_samples($resource);
        $sample = @$project_samples[0];
      }
    }
  # Sample
  }elsif ($resource_type eq "sample"){
    if ( exists $self->{metadata}{$resource} ){
      $sample = $resource;
    }else{    
      if ( $self->_fetch_metadata( $resource_type, $resource ) ){
        $sample = $resource;
      }
    }
  # Library        
  }elsif ($resource_type eq "library"){
    if ( exists $self->{primaryLibrary_to_sample_index}{$resource} ){
      $sample = $self->{primaryLibrary_to_sample_index}{$resource};
    }else{
      if ( $self->_fetch_metadata( $resource_type, $resource ) ){
        $sample = $self->{primaryLibrary_to_sample_index}{$resource};
      }
    }
  # Mapset
  }elsif ($resource_type eq "mapset"){
    if ( exists $self->{mapset_to_sample_index}{$resource} ){
      $sample = $self->{mapset_to_sample_index}{$resource};
    }else{
      if ( $self->_fetch_metadata( $resource_type, $resource ) ){
        $sample = $self->{mapset_to_sample_index}{$resource};
      }
    }
  # Slide         
	}elsif ($resource_type eq "slide"){
    if ( exists $self->{slide_index}{$resource} ){
      $sample = $self->_first_slide_mapset_sample( $resource );
    }else{
      if ( $self->_fetch_metadata( $resource_type, $resource ) ){
        $sample = $self->_first_slide_mapset_sample( $resource );
      }
    }
  # Microarray 
  }elsif ($resource_type eq "microarray"){
    if ( exists $self->{microarray_to_sample_index}{$resource} ){
      $sample = $self->{microarray_to_sample_index}{$resource};
       #//print "EXISTS - $resource_type: $resource - $sample\n";
    }else{
      #//print "Not yet found $resource!\n";
      if ( $self->_fetch_metadata( $resource_type, $resource ) ){
        #//print "FOUND - $resource_type: $resource - $sample\n";
        $sample = $self->{microarray_to_sample_index}{$resource};
      }
    }
   
  # Mergedbam 
  }elsif ($resource_type eq "mergedbam"){
    if ( exists $self->{mergedbam_index}{$resource} ){
      $sample = $self->{mergedbam_index}{$resource};
    }else{
      if ( $self->_fetch_metadata( $resource_type, $resource ) ){
        $sample = $self->{mergedbam_index}{$resource};
      }
    }
  }else{
    qlogprint( {l=>'WARN'}, "resource_type $resource_type is unknown\n");
  }
    
  # Now that the sample has been found. 
	# We will use it's meta_object to return metadata from.
  if ( $sample and exists $self->{metadata}{$sample} ){
    if ( $resource_type eq "slide" ) {
      $self-> _reset_selected();
    }else{
      $self->{selected} = $self->{metadata}{$sample};
    }
    return 1;
  }else{
    return 0;
  }
}


=pod

B<clear_found_metadata()>

Destroys fetched metadata found.

Parameters:
 None

Returns: None
=cut
sub clear_found_metadata{
  my $self = shift;
  
  # Do NOT clear parent project metadata! 
  # It will not be repopulated.
  
  $self->_reset_selected();
  # Sample metadata
  $self->{project_index}                    = {};
  $self->{sample_index}                     = {};
  $self->{primaryLibrary_to_sample_index}   = {};
  $self->{mapset_to_sample_index}           = {};
  $self->{microarray_to_sample_index}       = {};
  $self->{metadata}                         = {};
  
  # Mergedbam metadata
  $self->{mergedbam_to_mapset_index}        = {};
  $self->{sample_to_mergedbams_index}       = {};
  $self->{project_to_mergedbam_index}       = {};
  $self->{mergedbams}                       = {};                   
}


#########################################################################
# # #
# #      Public Functions -- Returning indivdual values.
#
#########################################################################


# !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !
#
#  find_metadata() must be called before these function should be used.
#
# !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !  !


# 	Parent Project 
# -------------------------------------------------------------------

=pod

B<parent_projects()>

 Returns a list of parent projects

Parameters:
 None

Returns:
 parent_projects: array string - List of parent projects

=cut
sub parent_projects {
  my $self            = shift;
  my @parent_projects = ();
  
  if ( $self->{parent_projects} ) {
      @parent_projects = keys %{$self->{parent_projects}};
  }
  
  return \@parent_projects;
}


=pod

B<parent_project()>

 Returns the parent project for a selected Sample, Library or Mapset

Parameters: None

Returns:
 parent_project: scalar string - Parent project identifier.

=cut
sub parent_project {
  my $self = shift;
  
  if ( $self->{selected} ) {
    # FIXME use parent_project structure
    return $self->{selected}{parent_project}{name};
  }
  
  return '';
}


=pod

B<parent_project_path()>

 Returns the directory path for a parent project.
 This function is overloaded; If the parent_project argument is supplied
 it will return the path for that parent project. If not supplied, the 
 path of the parent project for the selected sample is returned.

Parameters:
 parent_project: scalar sting - (Optional) parent project identifier

Returns:
 directory_path :scalar string - Parent project directory path.

=cut
sub parent_project_path {
  my $self            = shift;
  my $parent_project  = shift;
  
  # of supplied Parent Project  
  if ( $parent_project and exists $self->{parent_projects}{$parent_project} ) {
      return $self->{parent_projects}{$parent_project}{path};
  }
  # of selected Sample 
  elsif ( $self->{selected} ) {
      return $self->{selected}{parent_project}{path};
  }
  
  return '';
}


=pod

B<parent_project_label()>

 Returns the label for a parent project.
 This function is overloaded; If the parent_project argument is supplied
 it will return the label for that parent project. If not supplied, the 
 label of the parent project for the selected sample is returned.

Parameters:
 parent_project: scalar sting       (Optional) parent project identifier

Returns:
 label: scalar string               Parent project label

=cut
sub parent_project_label {
  my $self = shift;
  my $parent_project  = shift;
  
  # of supplied Parent Project  
  if ( $parent_project and exists $self->{parent_projects}{$parent_project} ) {
      return $self->{parent_projects}{$parent_project}{label};
  }
  # of selected Sample 
  elsif ( $self->{selected} ) {
      return $self->{selected}{parent_project}{label};
  }

  return '';
}


=pod

B<parent_project_project_prefix()>

 Returns the project prefix for a parent project.
 This function is overloaded; If the parent_project argument is supplied
 it will return the project prefix for that parent project. If not supplied, 
 the project prefix of the parent project for the selected sample is 
 returned.

Parameters:
 parent_project: scalar sting       (Optional) parent project identifier

Returns:
 project_prefix: scalar string      Pre fix for projects of the parent project.

=cut
sub parent_project_project_prefix {
  my $self = shift;
  my $parent_project  = shift;
  
  # of supplied Parent Project  
  if ( $parent_project and exists $self->{parent_projects}{$parent_project} ) {
      return $self->{parent_projects}{$parent_project}{project_prefix};
  }
  # of selected Sample 
  elsif ( $self->{selected} ) {
      return $self->{selected}{parent_project}{project_prefix};
  }

  return '';
}


=pod

B<parent_project_has_array_samples()>

 Returns the is a parent project has array samples.
 This function is overloaded; If the parent_project argument is supplied
 it will return if the parent project has array samples. If not supplied, 
 if the parent project has array samples for the selected sample is 
 returned.

Parameters:
 parent_project: scalar sting         (Optional) parent project identifier

Returns:
 has_array_samples: scalar boolean    Does the parent project have array samples.

=cut
sub parent_project_has_array_samples {
  my $self            = shift;
  my $parent_project  = shift;
  
  # of supplied Parent Project  
  if ( $parent_project and exists $self->{parent_projects}{$parent_project} ) {
      return $self->{parent_projects}{$parent_project}{array_samples};
  }
  # of selected Sample 
  elsif ( $self->{selected} ) {
      return $self->{selected}{parent_project}{array_samples};
  }

  return '';
}

=pod

B<parent_project_projects()>

 Returns a list of projects of a parent project.

Parameters:
 parent_project: scalar sting         parent project identifier

Returns:
 projects: array string               list of projects of parent project

=cut
# TODO parent_project_projects
sub parent_project_projects {
  my $self            = shift;
  my $parent_project  = shift;
  
#  # of supplied Parent Project  
  if ( $parent_project and exists $self->{parent_projects}{$parent_project} ) {
    if ( ! exists $self->{parent_projects}{$parent_project}{projects} ) {
      $self->_fetch_projects_of_parent_projects($parent_project);
    }
    return $self->{parent_projects}{$parent_project}{projects};
  }

  return [];
}

# 	Project 
# -------------------------------------------------------------------

#  Returns the project for a Final, Library or Mapset

=pod

B<project()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub project {
    my $self = shift;
    
    if ( $self->{selected} ) { 
        return $self->{selected}{project}{name};
    }
    return ''; 
}


=pod

B<project_limsid()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub project_limsid {
    my $self = shift;
    
    if ( $self->{selected} ) { 
        return $self->{selected}{project}{lims_id};
    }
    return ''; 
}


=pod

B<study_groups()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub study_groups {
    my $self = shift;
    
    if ( $self->{selected} ) {
        return $self->{selected}{project}{study_groups};
    }
    return '';
}


=pod

B<project_open()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub project_open {
    my $self = shift;
    
    if ( $self->{selected} ) {
        return $self->{selected}{project}{project_open};
    }
    return '';
}


# 	Sample
# -------------------------------------------------------------------

=pod

B<sample()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub sample {
    my $self = shift;
    
    if ( $self->{selected} ) { 
        return $self->{selected}{sample}{name};
    }
    return ''; 
}


=pod

B<sample_limsid()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub sample_limsid {
    my $self = shift;
    
    if ( $self->{selected} ) { 
        return $self->{selected}{sample}{lims_id};
    }
    return ''; 
}


=pod

B<material()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub material {
    my $self = shift;
    
    if ( $self->{selected} ) { 
        return $self->{selected}{sample}{material};
    }
    return ''; 
}


=pod

B<sample_code()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub sample_code {
    my $self = shift;
    
    if ( $self->{selected} ) {
        return $self->{selected}{sample}{sample_code};
    }
    return '';
}


=pod

B<researcher_annotation()>

TODO

Parameters:
 TODO

Returns:
 scalar string - TODO

=cut
sub researcher_annotation {
    my $self = shift;
    
    if ( $self->{selected} ) {
        return $self->{selected}{sample}{researcher_annotation};
    }
    return '';
}




# 	Mapset
# -------------------------------------------------------------------

=pod

B<mapset_limsid()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub mapset_limsid {
    my $self    = shift;
    my $mapset  = shift;
    
    if ( $self->{selected} ) { 
        return $self->{selected}{mapset_details}{$mapset}{lims_id};
    }
    return '';
}


=pod

B<primary_library()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub primary_library {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{primary_library};
    }
    return '';
}


=pod

B<library_protocol()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub library_protocol {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{library_protocol};
    }
    return '';
}


=pod

B<capture_kit()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub capture_kit {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{capture_kit};
    }
    return '';
}


=pod

B<gff_file()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO


=cut
sub gff_file {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{gff_file};
    }
    return '';
}


=pod

B<sequencing_platform()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub sequencing_platform {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{sequencing_platform};
    }
    return '';
}


=pod

B<aligner()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub aligner {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{aligner};
    }
    return '';
}


=pod

B<alignment_required()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO


=cut
sub alignment_required {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{alignment_required};
    }
    return '';
}


=pod

B<species_reference_genome()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO


=cut
sub species_reference_genome {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{species_reference_genome};
    }
    return '';
}


=pod

B<genome_file()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO


=cut
sub genome_file {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{genome_file};
    }
    return '';
}


=pod

B<failed_qc()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub failed_qc {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{failed_qc};
    }
    return '';
}


=pod

B<isize_min()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub isize_min {
	my $self = shift;
	my $mapset = shift;
	#my $resource_type   = "mapset";
	#my $resource        = $mapset;
	#
	#$self->_find_metadata($resource_type, $resource);
    
	if ( $self->{selected} ) {
	 return $self->{selected}{mapset_details}{$mapset}{isize_min};
	}
	return '';
}


=pod

B<isize_max()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub isize_max {
	my $self = shift;
	my $mapset = shift;
	#my $resource_type   = "mapset";
	#my $resource        = $mapset;
	#
	#$self->_find_metadata($resource_type, $resource);
    
	if ( $self->{selected} ) {
	 return $self->{selected}{mapset_details}{$mapset}{isize_max};
	}
	return '';
}


=pod

B<isize_manually_reviewed()>

TODO

Parameters:
 mapset:    scalar string - TODO

Returns:
 scalar string - TODO


=cut
sub isize_manually_reviewed {
	my $self = shift;
	my $mapset = shift;
	#my $resource_type   = "mapset";
	#my $resource        = $mapset;
	#
	#$self->_find_metadata($resource_type, $resource);
    
	if ( $self->{selected} ) {
        return $self->{selected}{mapset_details}{$mapset}{isize_manually_reviewed};
	}
	return '';
}








# 	Array
# -------------------------------------------------------------------

=pod

B<array_type()>

TODO

Parameters:
 microarray:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub array_type {
	my $self = shift;
	my $microarray = shift;
	#my $resource_type   = "mapset";
	#my $resource        = $mapset;
	#
	#$self->_find_metadata($resource_type, $resource);
    
	if ( $self->{selected} ) {
        return $self->{selected}{microarray_details}{$microarray}{array_type};
	}
	return '';
}


=pod

B<array_class()>

TODO

Parameters:
 microarray:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub array_class {
	my $self = shift;
	my $microarray = shift;
	#my $resource_type   = "mapset";
	#my $resource        = $mapset;
	#
	#$self->_find_metadata($resource_type, $resource);
    
	if ( $self->{selected} ) {
        return $self->{selected}{microarray_details}{$microarray}{array_class};
	}
	return '';
}


=pod

B<array_protocol()>

TODO

Parameters:
 microarray:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub array_protocol {
	my $self = shift;
	my $microarray = shift;
	#my $resource_type   = "mapset";
	#my $resource        = $mapset;
	#
	#$self->_find_metadata($resource_type, $resource);
    
	if ( $self->{selected} ) {
        return $self->{selected}{microarray_details}{$microarray}{array_protocol};
	}
	return '';
}


=pod

B<array_failed_qc()>

TODO

Parameters:
 microarray:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub array_failed_qc {
	my $self = shift;
	my $microarray = shift;
	#my $resource_type   = "mapset";
	#my $resource        = $mapset;
	#
	#$self->_find_metadata($resource_type, $resource);
    
	if ( $self->{selected} ) {
        return $self->{selected}{microarray_details}{$microarray}{failed_qc};
	}
	return '';
}



#sub array_limsid {
#    my $self    = shift;
#    my $mapset  = shift;
#    
#    if ( $self->{selected} ) { 
#        return $self->{selected}{mapset_details}{$mapset}{lims_id};
#    }
#    return '';
#}


# 	Mergedbam
# -------------------------------------------------------------------

=pod

B<mergedbam_material()>

Returns the material of the mapsets in the mergedbam

Parameters:
 mergedbam: scalar string - mergedbam name

Returns:
 material: scalar string - material of the mapsets in the mergedbam

=cut
sub mergedbam_material {
	my $self      = shift;
	my $mergedbam = shift;
  
  if ( exists $self->{mergedbam}{$mergedbam} ) {
    return $self->{mergedbam}{$mergedbam}{material};
  }
  return '';  
}


=pod

B<mergedbam_sample()>

Returns the sample of mapsets in mergedbam

Parameters:
 mergedbam: scalar string - mergedbam name

Returns:
 sample: scalar string - sample of the mapsets in the mergedbam

=cut
sub mergedbam_sample {
	my $self      = shift;
	my $mergedbam = shift;
  
  if ( exists $self->{mergedbam}{$mergedbam} ) {
    return $self->{mergedbam}{$mergedbam}{sample};
  }
  return '';  
}


=pod

B<mergedbam_sample_code()>

Returns the sample_code of mapsets in the mergedbam

Parameters:
 mergedbam: scalar string - mergedbam name

Returns:
 sample_code: scalar string - sample_code of mapsets in the mergedbam

=cut
sub mergedbam_sample_code{
	my $self      = shift;
	my $mergedbam = shift;
  
  if ( exists $self->{mergedbam}{$mergedbam} ) {
    return $self->{mergedbam}{$mergedbam}{sample_code};
  }
  return '';  
}


=pod

B<mergedbam_library_protocol()>

Returns the library protocol used by the mapsets in mergedbam

Parameters:
 mergedbam: scalar string - mergedbam name

Returns:
 library_protocol: scalar string - library protocol used by the mapsets in mergedbam

=cut
sub mergedbam_library_protocol {
	my $self      = shift;
	my $mergedbam = shift;
  
  if ( exists $self->{mergedbam}{$mergedbam} ) {
    return $self->{mergedbam}{$mergedbam}{library_protocol};
  }
  return '';  
}


=pod

B<mergedbam_capture_kit()>

Returns the capture kit used by the mapsets in the mergedbam

Parameters:
 mergedbam: scalar string - mergedbam name

Returns:
 capture_kit: scalar string - capture kit used by the mapsets in the mergedbam

=cut
sub mergedbam_capture_kit {
	my $self      = shift;
	my $mergedbam = shift;
  
  if ( exists $self->{mergedbam}{$mergedbam} ) {
    return $self->{mergedbam}{$mergedbam}{capture_kit};
  }
  return '';  
}



=pod

B<mergedbam_aligner()>

Returns the aligner used by the mapsets in the mergedbam

Parameters:
 mergedbam: scalar string - mergedbam name

Returns:
 aligner: scalar string - aligner used by the mapsets in the mergedbam

=cut
sub mergedbam_aligner {
	my $self      = shift;
	my $mergedbam = shift;
  
  if ( exists $self->{mergedbam}{$mergedbam} ) {
    return $self->{mergedbam}{$mergedbam}{aligner};
  }
  return '';  
}

=pod

B<mergedbam_sequencing_platform()>

Returns the sequencing platform used by the mapsets in the mergedbam

Parameters:
 mergedbam: scalar string - mergedbam name

Returns:
 sequencing_platform: scalar string - sequencing platform used by the mapsets in the mergedbam

=cut
sub mergedbam_sequencing_platform  {
	my $self      = shift;
	my $mergedbam = shift;
  
  if ( exists $self->{mergedbam}{$mergedbam} ) {
    return $self->{mergedbam}{$mergedbam}{sequencing_platform};
  }
  return '';  
}           

=pod

B<mergedbam_mapsets()>

Returns the mapsets in the mergedbam

Parameters:
 mergedbam: scalar string - mergedbam name

Returns:
 mapsets: scalar string - mapsets in the mergedbam

=cut
sub mergedbam_mapsets  {
	my $self      = shift;
	my $mergedbam = shift;
  
  if ( exists $self->{mergedbam}{$mergedbam} ) {
    return $self->{mergedbam}{$mergedbam}{mapsets};
  }
  return '';  
}  







#########################################################################
# # #
# #      Functions for return lists.
#
#########################################################################

=pod

B<sample_mapsets()>

Returns a list of mapsets for a selected sample.

Parameters: None

Returns:
 mapsets: Array, string - list of mapsets otherwise an empty string.

=cut
sub sample_mapsets {
  my $self = shift;
  
  if ( $self->{selected} ) {
    my $sample_mapsets = $self->{selected}{mapset_details};
    my $mapsets = ();
    foreach my $mapset (keys %{$sample_mapsets}){
        push( @{$mapsets}, $mapset );        
    }
    return $mapsets;
  }
  return '';
}


=pod

B<sample_microarrays()>

TODO

Parameters:
 microarray:    scalar string - TODO

Returns:
 scalar string - TODO

=cut
sub sample_microarrays {
  my $self = shift;
  
  if ( $self->{selected} ) {
    my $sample_microarrays = $self->{selected}->{microarray_details};
    my $microarrays = ();
    foreach my $microarray (keys %{$sample_microarrays} ){
        push( @{$microarrays}, $microarray );        
    }
    return $microarrays;
  }
  return '';
}

=pod

B<sample_mergedbams()>

Returns a list of mergedbams that contain mapsets of the selected sample.

Parameters: None

Returns:
 mergedbams: array string     list of mergedbams

=cut
sub sample_mergedbams {
  my $self = shift;
    
  if ( my $sample = $self->sample() ) {
    return $self->{sample_to_mergedbams_index}{$sample};
  }
  return '';
}




##
## TODO: TEST THESE METHODS
##

#  Returns the Mapset for a given Library
sub primary_library_mapsets {
    my $self            = shift;
    my $primary_library         = shift;
    #my $resource_type   = "primary_library";
    #my $resource        = $primary_library;
    #
    #$self->_find_metadata($resource_type, $resource);   
    if ( $self->{selected} ) {
        return $self->{selected}{primary_library_mapsets}{$primary_library};   
    }    
    return ''; 
}

sub slide_samples {
    my $self    = shift;
    my $slide   = shift;
    
    
}


sub slide_mapsets {
    my $self    = shift;
    my $slide   = shift;
    #my $resource_type   = "primary_library";
    #my $resource        = $primary_library;
    #
    #$self->_find_metadata($resource_type, $resource);   
    if ( $self->{selected} ) {
        return $self->{slide_to_mapset_index}{$slide};
    }    
    return ''; 
}

sub mapset_sample {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
         $self->{selected} = $self->{metadata}{ $self->{mapset_to_sample_index}{$mapset} };
        return $self->{mapset_to_sample_index}{$mapset} ;
    }elsif ( exists($self->{mapset_to_sample_index}{$mapset}) ){
        $self->{selected} = $self->{metadata}{ $self->{mapset_to_sample_index}{$mapset} };
        return $self->{mapset_to_sample_index}{$mapset} ;
    }
    
    return '';
}


sub project_samples {
    my $self = shift;
    my $project = shift; 
    
    if ( $self->{selected} ) {
        return $self->{project_index}{$project};
    }
    return '';
}

=pod

B<project_mergedbams()>

Returns a list of mergedbams that contain mapsets of the selected project.

Parameters: None

Returns:
 mergedbams: array string     list of mergedbams

=cut
sub project_mergedbams {
    my $self      = shift;

    if ( my $project = $self->project() ) {
        return $self->{project_to_mergedbams_index}{$project};
    }
    return '';
}

#########################################################################
# # #
# #      Functions for return structured data.
#
#########################################################################

# sample details

# Returns a data object for a given Mapset
sub mapset_metadata {
    my $self            = shift;
    my $mapset          = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset; 
    #
    #$self->_find_metadata($resource_type, $resource);
    if ( exists $self->{selected}{mapset_libaries}{$mapset} ) {
        my $mapset_metadata = {
            parent_project              => $self->parent_project(),
            parent_project_path         => $self->parent_project_path(),                # new
            project                     => $self->project(),
            sample                      => $self->sample(),
            sample_code                 => $self->sample_code(),
            material                    => $self->material(),
            mapset                      => $mapset,
            primary_library             => $self->primary_library($mapset),
			library_protocol			=> $self->library_protocol($mapset),
            capture_kit                 => $self->capture_kit($mapset),
            sequencing_platform         => $self->sequencing_platform($mapset),         # new
            species_reference_genome    => $self->species_reference_genome($mapset),
            genome_file					=> $self->genome_file($mapset),
            gff_file                    => $self->gff_file($mapset),                    # new
            aligner                     => $self->aligner($mapset),
			alignment_required			=> $self->alignment_required($mapset),
            failed_qc                   => $self->failed_qc($mapset),
            isize_min					=> $self->isize_min($mapset),
            isize_max                	=> $self->isize_max($mapset),
            isize_manually_reviewed		=> $self->isize_manually_reviewed($mapset)
        };
        
        return $mapset_metadata;
    }
    
    return '';
}

sub mapset_details {
    my $self    = shift;
    my $mapset  = shift;
    
    if ( $self->{selected} ) { 
        my $mapset_metadata = {
            mapset_limsid               => $self->mapset_limsid($mapset),           # new
            primary_library             => $self->primary_library($mapset),
			library_protocol			=> $self->library_protocol($mapset),
            capture_kit                 => $self->capture_kit($mapset),
            sequencing_platform         => $self->sequencing_platform($mapset),     # new
            gff_file                    => $self->gff_file($mapset),
            aligner                     => $self->aligner($mapset),
			alignment_required			=> $self->alignment_required($mapset),
            species_reference_genome    => $self->species_reference_genome($mapset),
            genome_file					=> $self->genome_file($mapset),
            failed_qc                   => $self->failed_qc($mapset),
            isize_min					=> $self->isize_min($mapset),
            isize_max                	=> $self->isize_max($mapset),
            isize_manually_reviewed		=> $self->isize_manually_reviewed($mapset)
        };
        
        return $mapset_metadata;
    }
    return '';
}

# new
sub array_details { 
    my $self    = shift;
    my $microarray  = shift;
    
    if ( $self->{selected} ) { 
        my $microarray_metadata = {
            array_type              => $self->array_type($microarray),           
            array_class             => $self->array_class($microarray),
			array_protocol          => $self->library_protocol($microarray),
            array_failed_qc         => $self->array_failed_qc($microarray)
        };
        
        return $microarray_metadata;
    }
    return '';
}

sub microarray_metadata {
    my $self            = shift;
    my $microarray          = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset; 
    #
    #$self->_find_metadata($resource_type, $resource);
    if ( exists $self->{selected}{microarray_details}{$microarray} ) {
        my $mapset_metadata = {
            parent_project              => $self->parent_project(),
            parent_project_path         => $self->parent_project_path(),                # new
            project                     => $self->project(),
            sample                      => $self->sample(),
            sample_code                 => $self->sample_code(),
            material                    => $self->material(),
            array_type                  => $self->array_type($microarray),           
            array_class                 => $self->array_class($microarray),
			array_protocol              => $self->array_protocol($microarray),
            array_failed_qc             => $self->array_failed_qc($microarray)
            
        };
        
        return $mapset_metadata;
    }
    
    return '';
}



sub reference_genome {
    my $self = shift;
    my $mapset = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
		my $reference_genome = {
			species_reference_genome 	=> $self->species_reference_genome($mapset),
			genome_file 				=> $self->genome_file($mapset)
		};
        return $reference_genome;
    }
    return '';
}

sub mapset_isizes {
    my $self    = shift;
    my $mapset  = shift;
    
    if ( $self->{selected} ) { 
        my $mapset_isizes = {
            isize_min					=> $self->isize_min($mapset),
            isize_max                	=> $self->isize_max($mapset),
            isize_manually_reviewed		=> $self->isize_manually_reviewed($mapset)
        };
        
        return $mapset_isizes;
    }
    return '';
}






#########################################################################
#      Other Useful Functions for return information about runs.
#########################################################################

# Matches on slide name
sub mapset_to_platform {
    my $self    = shift;
    my $mapset  = shift;
    
    my @sections = split /\./, $mapset;
    my $slide_name = $sections[0];
    
	return $self->slide_to_platform ( $slide_name );
	
}

sub slide_to_platform {
    my $self    	= shift;
    my $slide_name  = shift;
	
    my %PATTERNS;
    $PATTERNS{HISEQ_SLIDENAME} =    qr/^\d{6}_(SN)*\d{7}_\d{4}_[A-Z0-9]{10}/;
    $PATTERNS{MISEQ_SLIDENAME} =    qr/^\d{6}_M\d{5}_\d{4}(_A|_)(\d{9}-|)\w{5}/;
	
    $PATTERNS{TORRENT_SLIDENAME} =  qr/^[T]{1}\d{5}_20\d{6}_\d+/;
    $PATTERNS{SOLID4_SLIDENAME} =   qr/^S0\d{3}|[a-z]+_20\d{6}_[12]{1}/;
    $PATTERNS{XL5500_SLIDENAME} =   qr/^S[18]{1}\d{4}_20\d{6}_[12]{1}/;
    
    if ( $slide_name =~ /$PATTERNS{HISEQ_SLIDENAME}/ ) {
        return "HISEQ";
    
	}elsif ( $slide_name =~ /$PATTERNS{MISEQ_SLIDENAME}/ ) {
        return "MISEQ";
      
    }elsif ( $slide_name =~ /$PATTERNS{TORRENT_SLIDENAME}/ ) {
        return "TORRENT";
        
    }elsif ( $slide_name =~ /$PATTERNS{SOLID4_SLIDENAME}/ ) {
        return "SOLID4";
        
    }elsif ( $slide_name =~ /$PATTERNS{XL5500_SLIDENAME}/ ) {
        return "XL5500";
        
    }else{
        my $message = "Could not match Slide name: ".$slide_name." to a platform\n";
        qlogprint( {l=>'WARN'}, "$message\n");
        return "UNKNOWN";
    }
	
}


# 
# Extras For Lynn!
# 

sub solid4_shortname_mapping {
	my $self 	= shift;
	my $mapset	= shift;
	
	my @sections;
    @sections = split /\./, $mapset;
    my $slide_name = $sections[0];
	@sections = split /_/, $slide_name;
	my $machine_shortname = $sections[0];
	
	my $machine_shortname_mappings = {
	    S0014 => { ip => "10.160.72.10", 	resultsdir => "/data/results/solid0014",	nickname =>	"Pinchy"},
		S0039 => { ip => "10.160.72.9" , 	resultsdir => "/data/results/solid0039",	nickname =>	"Stampy"},
		S0411 => { ip => "10.160.72.4" , 	resultsdir => "/data/results/solid0411",	nickname =>	"Snowball"},
		S0413 => { ip => "10.160.72.5" , 	resultsdir => "/data/results/138000413",	nickname =>	"She's The Fastest"},
		S0414 => { ip => "10.160.72.6" , 	resultsdir => "/data/results/138000414",	nickname =>	"Santa's Little Helper"},
		S0416 => { ip => "10.160.72.7" , 	resultsdir => "/data/results/138000416",	nickname =>	"Blinky"},
		S0417 => { ip => "10.160.72.8" , 	resultsdir => "/data/results/138000417",	nickname =>	"Furious D"},
		S0428 => { ip => "10.160.72.14", 	resultsdir => "/data/results/solid0428",	nickname =>	"SpiderPig"},
		S0433 => { ip => "10.160.72.13", 	resultsdir => "/data/results/solid0433",	nickname =>	"JubJub"},
		S0436 => { ip => "10.160.72.12", 	resultsdir => "/data/results/solid0436",	nickname =>	"General Sherman"},
		S0449 => { ip => "10.160.72.11", 	resultsdir => "/data/results/solid0449",	nickname =>	"Mojo"}
	};
	
	if ( $machine_shortname_mappings->{$machine_shortname} ) {
		return $machine_shortname_mappings->{$machine_shortname};
	}
	return '';

}

sub solid_primary_resultsdir {
	my $self 	= shift;
	my $mapset  = shift;
	
	my @sections;
    @sections = split /\./, $mapset;
    my $run_name = $sections[0];
    my $barcode  = $sections[2];
	my $mappings = $self->solid4_shortname_mapping( $mapset );
	if ( $mappings ) {
		my $resultsdir = $mappings->{resultsdir};
		if ( $barcode eq "nobc" ){
			# For non-barcoded (Frag/FragPE/LMP) runs:	
			return "$resultsdir/$run_name/Sample1/results.F1B1/";
	    }elsif ( $barcode ne "" ) {
			# For barcoded (FragBC/FragPEBC) runs:
			return "$resultsdir/$run_name/bcSample1/results.F1B1/libraries/$barcode/";
		}
		
	}
	return '';
}

# Returns the primaries directories for a solid run.
sub solid_primaries {
    my $self    = shift;
    my $mapset  = shift;
    
    my @sections = split /\./, $mapset;
    my $slide_name = $sections[0];
    if ( $self->slide_to_platform($slide_name) eq "SOLID4" ){
        my $solid_primaries = { 
            primaries          => $self->{genologics_LIMS}->solid_primaries($slide_name),
            primary_resultsdir => $self->solid_primary_resultsdir($mapset)
        };
        
        return $solid_primaries;
    }
    return '';
}








#########################################################################
#
#       DEBUGGING Functions 
#
#########################################################################

#  Prints debuging 
sub _debug {
    my $self = shift;
    my $message = shift;

    if ( $self->{debug} ) {
       qlogprint( {l=>'DEBUG'}, "$message\n");
    }            
}

#  Prints the selected
sub _selected {
    my $self = shift;
    
    print "\n SELECTED \n";
    print Dumper $self->{selected};

}

#  Debugging of indexes
sub _indexes {
    my $self = shift;
    my $index = shift;
	
	if ( $index eq "projects") {
	    print "\n INDEXES - DONORS \n";
	    print Dumper $self->{project_index};
	}
	
    #print "\n INDEXES - SAMPLES \n";
    #print Dumper $self->{sample_index};    
	
	if ( $index eq "libraries") {
    	print "\n INDEXES - LIBRARIES \n";
    	print Dumper $self->{primaryLibrary_to_sample_index};
	}
	
	if ( $index eq "slides") {
		print "\n INDEXES - SLIDE \n";
    	print Dumper $self->{slide_to_mapset_index};
	}	
	
	if ( $index eq "mapsets") {
    	print "\n INDEXES - MAPSETS \n";
    	print Dumper $self->{mapset_to_sample_index};
	}
	
}

#  Debugging of metadata Object
sub _metadata {
    my $self = shift;
    
    print "\n METADATA \n";
    print Dumper $self->{metadata};
    
}

#  debuging method for printing metadata object with data returned from 
#  class functions
sub _data_object {
    my $self = shift;
    
    if ( $self->{selected} ) {
        #$self->_selected();
        
        
        # Printing basic metadata
        $self->_debug( "project LIMS ID  -\t"  . $self->project_limsid()  . "\n");
        $self->_debug( "sample LIMS ID  -\t" . $self->sample_limsid() . "\n");
        
        $self->_debug( "parent_project  -\t" . $self->parent_project() . "\n");
        $self->_debug( "project  -\t" .   $self->project()  . "\n");
        $self->_debug( "sample  -\t" .   $self->sample()  . "\n");
        $self->_debug( "material  -\t". $self->material()  . "\n");
        $self->_debug( "sample_code  -\t" .  $self->sample_code()  . "\n");
        
		
		
		
		
        # list of finals for a project
        $self->_debug("\nDonor Finals:" ."\n");
        my $project_finals = $self->project_finals( $self->project() );
        my $finals = join(', ', @$project_finals); 
        $self->_debug( "\t". $self->project()." - \t" . $finals . "\n");
        
        $self->_debug("\nFinal Libraries: \n");
        foreach my $final ( @$project_finals ){
            $self->find_metadata("final", $final);
            # list of final libraries 
            my $final_libraries = $self->sample_libraries();
            my $libraries = join(', ', @$final_libraries);
            
            $self->_debug("\t" . $final ." - ". $libraries . "\n");
        }
        
        # list of final libraries mapsets 
        $self->_debug("\nLibrary Mapsets:" . "\n");
        foreach my $final ( @$project_finals ){
            $self->find_metadata("final", $final);
            # list of final libraries 
            my $final_libraries = $self->final_libraries();
            foreach my $primary_library ( @$final_libraries ){
                my $mapsets =  $self->primary_library_mapsets($primary_library);
                my $primary_library_mapsets = join(', ', @$mapsets); 
                $self->_debug("\t" . $primary_library ." - ". $primary_library_mapsets . "\n");
            }
        }
        
        $self->_debug("\nFinal Mapsets:" . "\n");
        foreach my $final ( @$project_finals ){
            $self->find_metadata("final", $final);
            $self->_debug("\t" . $final ."\n");
            my $final_mapsets = $self->final_mapsets();
            foreach my $mapset ( @$final_mapsets) {
                $self->_debug("\t\t" . $mapset ."\n");
            }
        }
        
        $self->_debug("\nFinals:" . "\n");
        foreach my $final ( @$project_finals ){
            $self->find_metadata("final", $final);
            $self->_debug("\t" . $final ."\n");
            my $final_metadata = $self->final_metadata();
            if ( $self->{debug} ) {
                print Dumper $final_metadata;
            }
        }
        
        #$self->_debug("\nLibraries: \n");
        #foreach my $final ( @$project_finals ){
        #    $self->find_metadata("final", $final);
        #    # list of final libraries 
        #    my $final_libraries = $self->final_libraries();
        #    foreach my $primary_library ( @$final_libraries ){
        #        $self->_debug("\t" . $primary_library ."\n");
        #        my $primary_library_metadata =  $self->primary_library_metadata($primary_library);
        #        if ( $self->{debug} ) {
        #            print Dumper $primary_library_metadata;
        #        }
        #    }
        #}
        
        #$self->_debug("\nMapset Details:" . "\n");
        #foreach my $final ( @$project_finals ){
        #    $self->find_metadata("final", $final);            
        #    my $final_mapsets = $self->final_mapsets();
        #    my $mapsets = join(",\n\t\t\t", @$final_mapsets); 
        #    foreach my $mapset ( @$final_mapsets) {
        #        $self->_debug("\t" . $mapset ."\n");
        #        my $mapset_metadata =  $self->mapset_metadata($mapset);
        #        #my $mapset_metadata =  $self->mapset_details($mapset);
        #        if ( $self->{debug} ) {
        #            print Dumper $mapset_metadata;
        #        }
        #        
        #    }
        #}
        

        
    } else {
        print "No Final selected\n";
    }     
}

=cut


# Returns basic metadata for a selected Final shared between 
# Final, Library or Mapset
## TODO - Depricate
#sub basic_meta {
#    my $self            = shift;
#    
#    if ( $self->{selected} ) {
#        my $metadata = {
#            parent_project               => $self->parent_project(),
#            project                     => $self->project(),
#            sample                      => $self->sample(),
#            material                    => $self->material(),
#            sample_code                 => $self->sample_code()
#        };
#        return $metadata;
#    }
#    return '';
#}
#
# Returns a data object for a given Library
#sub primary_library_metadata {
#    my $self            = shift;
#    my $primary_library         = shift;
#    #my $resource_type   = "mapset";
#    #my $resource        = $mapset;
#    #
#    #$self->_find_metadata($resource_type, $resource);
#    if ( exists $self->{selected}{primary_library_mapsets}{$primary_library} ) {
#        my $primary_library_metadata = {
#            parent_project  => $self->parent_project(),
#            project         => $self->project(),
#            sample          => $self->sample(),
#            material        => $self->material(),
#            sample_code     => $self->sample_code(),
#            primary_library => $primary_library,
#            mapsets         => $self->primary_library_mapsets($primary_library)
#        };
#        
#        return $primary_library_metadata;
#    }
#    
#    return '';
#}




# Prints a count of finals in Metadata Structure
#sub _count_finals {
#    my $self = shift;
#    
#    my $finals = $self->{metadata};
#    my $count = scalar keys %$finals;
#    print $count."\n";
#}




#$self->_debug( "material_type  -\t". $self->material_type()  . "\n");
#$self->_debug( "source_type  -\t" .  $self->source_type()  . "\n");
#$self->_debug( "experiment  -\t".$self->experiment()  . "\n");

#  Returns the material for a Final, Library or Mapset
#sub material_type {
#    my $self = shift;
#    
#    if ( $self->{selected} ) { 
#        return $self->{selected}{material_type};
#    }
#    return ''; 
#} 

#  Returns the source for a Final, Library or Mapset
#sub source_type {
#    my $self = shift;
#    
#    if ( $self->{selected} ) {
#        return $self->{selected}{source_type};
#    }
#    return '';
#}

#  Returns the experiment for a Final, Library or Mapset
#sub experiment {
#    my $self = shift;
#    
#    if ( $self->{selected} ) {
#        return $self->{selected}{experiment};
#    }
#    return '';
#
#}


#  Returns the Library for a given Mapset
#sub mapset_library {
#    my $self            = shift;
#    my $mapset          = shift;
#    #my $resource_type   = "mapset";
#    #my $resource        = $mapset;
#    #
#    #$self->_find_metadata($resource_type, $resource);
#    
#    if ( $self->{selected} ) {
#        return $self->{selected}{mapset_libaries}{$mapset};
#    }
#    return '';
#    
#}

#



# Returns data Object for a Final
sub final_metadata {
    my $self = shift;
    
    #my $final = join ('.', $self->{project}, $self->{experiment} );
    
    if ( $self->{selected} ) {
        my $final_metadata = { 
            parent_project         => $self->parent_project(),
            project           => $self->project(),
            sample          => $self->sample(),
            material        => $self->material(),
            sample_code     => $self->sample_code(),
            libraries       => $self->final_libraries(),
            mapsets         => $self->final_mapsets()
        };
    
        return $final_metadata;
    }
    
    return '';
}


#  Returns the finals for a given Donor
sub project_finals {
    my $self = shift;
    my $project = shift; 
    #my $resource_type   = "project";
    #my $resource        = $project;
    # 
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{project_index}{$project};
    }
    return '';
}

#  Returns the finals for a given Donor
sub sample_finals {
    my $self = shift;
    my $sample = shift; 
    #my $resource_type   = "project";
    #my $resource        = $project;
    # 
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{sample_index}{$sample};
    }
    return '';
}


#  Returns the Libraries for a given Final
sub final_libraries {
    my $self = shift;
    
    if ( $self->{selected} ) {
        my $primary_library_mapsets = $self->{selected}{primary_library_mapsets};
        my @libraries = (); 
        foreach my $primary_library (keys %$primary_library_mapsets){
            push(@libraries, $primary_library);        
        }
     
        return \@libraries; 
    } 
    return '';
}

#  Returns the Mapsets for a given Final


#
sub primary_library_final {
    my $self = shift;
    my $primary_library = shift;
    #my $resource_type   = "mapset";
    #my $resource        = $mapset;
    #
    #$self->_find_metadata($resource_type, $resource);
    
    if ( $self->{selected} ) {
        return $self->{primaryLibrary_to_sample_index}{$primary_library};
    }
    return '';
}

=cut

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
