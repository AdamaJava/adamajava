package QCMG::DB::QcmgReader;

################################################################################
#
#  Module:   QCMG::DB::QcmgReader.pm
#  Creator:  Matthew J Anderson
#  Created:  2012-02-24
#
# It is suggested that you use QCMG::DB::Metadata as an interface this class.
#
# This class's primary purpose is for retiving metadata on sequencing runs
# and microarray assays from the qcmg database schema.
#  
#  $Id$:
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

This class contains methods for retrieving metadata from the qcmg schema which is a 
refined subset of the Genologic LIMS database "Geneus". The Geneus schema is super
flexable in being able to cutomize data storeage without changes to the database.
To do this the tables are overly abstracted and large queries are costly. 
The qcmg schema is structed simply with just the most common data needed for 
automation.

The metadata retrieved is intentionally used to populate the data 
structures of the QCMG::DB::Metadata class. In which if using this class for 
searching for metadata of a mapset will return metadata for all mapsets of the 
samples of the searched mapset. This class also allows you to retive all 
metadata from Geneus which is useful for processing directory stuctures. 

This module does not write to the database, nor should it in the future. 
To write to the Geneus database you must use the Genologic API. 
For qcmg schema tables (eg for merged bams, report metrics) use QCMG::DB::QcmgWriter.

=head1 REQUIREMENTS

 DBI
 Data::Dumper
 QCMG::Util::QLog
=cut


use strict;                     # Good practice
use warnings;                   # Good practice

use DBI;                        # Perl Database interface module
use JSON qw( decode_json );     # From CPAN
use Data::Dumper;               # Perl core module
use QCMG::Util::QLog;           # QCMG logging module


use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;




################################################################################
=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 scalar: debug (optional)  Set as 1 for debugging.

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
        passwd  => undef,
    };
    
  # Store the connection and cursor for the life of the class 
    my $self = {
        geneus_conn     => '',
        geneus_cursor   => '',
        debug           => ( $debug ?  1 :  0  ),
        all_resources   => 0
    };    
  
  # Create connection to Geneus Database
    my $connect_string = 'DBI:Pg:dbname='.$db_settings->{db}.';host='.$db_settings->{host};
    $self->{geneus_conn} = DBI->connect( $connect_string,
                                        $db_settings->{user},
                                        $db_settings->{passwd},
                                        { RaiseError         => 1,
                                          ShowErrorStatement => 0,
                                          AutoCommit         => 0 } ) ||
                          die "Can not connect to database server: $!\n";
    $self->{geneus_conn}->{'ChopBlanks'} = 1; # copes with padded spaces if type=char
  
  # Storing SQL statements.
  $self->{sampleIDs}                        = (); # List of sampleIDs of the searched Items
  $self->{parentprojects_query_stmt}        = {}; # constructed by _parentprojects_query_stmt()
  $self->{projects_of_parentprojects_query_stmt} = {}; # constructed by _projects_of_parentproject_query_stmts()
  $self->{sampleIDs_of_query_stmts}         = {}; # constructed by _sampleIDs_of_query_stmts()
  $self->{sample_metadata_query_stmt}       = {}; # constructed by _sample_metadata_query_stmt()        
  $self->{mapset_metadata_query_stmt}       = {}; # constructed by _mapset_metadata_query_stmt()
  $self->{microarray_metadata_query_stmt}   = {}; # constructed by _microarray_metadata_query_stmt()
  $self->{mergedbam_metadata_query_stmt}    = {}; # constructed by _mergedbam_metadata_query_stmt()
  $self->{mapsets_in_mergedbam_query_stmt}  = {}; # constructed by _mapsets_in_mergedbam_query_stmt()
  

  bless $self, $class;
  
  # Store query statements.
  $self->_parentprojects_query_stmt();
  $self->_projects_of_parentprojects_query_stmt();
  $self->_sampleIDs_of_query_stmts();
  $self->_sample_metadata_query_stmt();
  $self->_mapset_metadata_query_stmt();
  $self->_microarray_metadata_query_stmt();
  $self->_mergedbam_metadata_query_stmt();
  $self->_mapsets_in_mergedbam_query_stmt();
  
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
# #     PRIVATE METHODS
# 
#   These methods are only to be used by methods of this class
#
################################################################################
=cut _debug()

If debug has been set on class initiation the debug message will be printed.

Parameters:
 message: scalar string

Returns:
 None - Prints debug message if debug is set true on class creation.
=cut
sub _debug {
    my $self = shift;
    my $message = shift;
    
    if ($self->{debug}) {
        qlogprint( {l=>'DEBUG'}, "$message\n");
    }            
}



################################################################################
=cut _debug_array()

If debug has been set on class initiation the contents of the array and debug 
message will be printed.

Parameters:
 array: array

Returns:
 None -Prints debug message if debug is set true on class creation.
=cut
sub _debug_array {
    my $self = shift;
    my $array = shift;
      
    my $message = join ', ', @$array;
    if ($self->{debug}) {
        qlogprint( {l=>'DEBUG'}, "$message\n");
    }            
}

sub _dberror {
    my $self = shift;
    my $message = shift;
    
    if ($self->{debug}) {
        qlogprint( {l=>'DB_ERROR'}, "$message\n");
        qlogprint( {l=>'DB_ERROR'}, $self->{geneus_conn}->errstr."\n");
    }
}



################################################################################
=cut _parentprojects_query_stmt()

Sets class variable "parentprojects_query_stmt" with a SQL statements to list 
parent projects. 

Parameters:
 None

Returns:
 None
=cut
sub _parentprojects_query_stmt {
  my $self = shift;
  
  # Returns all parent projects and details
  $self->{parentprojects_query_stmt} = qq{ 
        SELECT 
          project_prefix_map.parent         AS "parent_project",
          project_prefix_map.prefix         AS "project_prefix",
          project_prefix_map.parent_path    AS "parent_project_path",
          project_prefix_map.label          AS "parent_project_label",
          project_prefix_map.array_samples  AS "array_samples"
        FROM qcmg.project_prefix_map
        };
}



################################################################################
=cut _projects_of_parentprojects_query_stmt()

Sets class variable "projects_of_parentprojects_query_stmt" with a template 
SQL statement for projects of parent projects. 

Parameters:
 None

Returns:
 None
=cut
sub _projects_of_parentprojects_query_stmt {
  my $self = shift;
  
  # Returns all projects for a parent project
  $self->{projects_of_parentprojects_query_stmt} = qq{ 
        SELECT
          project.parent_project                  AS "parent_project", 
          replace( replace( project.name, '-', '_'), '.', '_' )   
                                                  AS "project",
          project.project_open                    AS "project_open",
          project.study_group                     AS "study_group"
        FROM qcmg.project
        WHERE project.parent_project = ? 
        };
}



################################################################################
=cut _sampleIDs_of_query_stmts()

Sets class variable "sampleIDs_of_query_stmts" with a hash of required SQL 
statements needed to determine a sample name. Metadata is only returned for 
a given sample.

Parameters:
 None

Returns:
 None
=cut
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
      
      # Returns the Sample IDs of a given Project (donor)
      project => qq{
          SELECT DISTINCT sample.name
          FROM qcmg.sample
          INNER JOIN qcmg.project ON sample.projectid = project.projectid 
          WHERE  replace( replace( project.name, '-', '_'), '.', '_' ) = ?
        },
      
      # Returns the Sample IDs of a given mergedbam (final)
      mergedbam => qq{
          SELECT DISTINCT mergedbam.sample
          FROM qcmg.mergedbam
          WHERE mergedbam.name = ?
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
=cut _sampleIDs_of()

Returns an array of sample IDs (names) for a given resource.

Parameters:
 context:  scalar string   - Type of resource being searched. 
           
           Valid types are:
            "slide", "mapset", "primary_library", "project", "sample", 
            "microarray", "mergedbam"
 
 resource:  scalar string  - Name of the resource
 
Returns:
 sample_IDs: array  - list of sample IDs (names) found. Or 0 if not found.
=cut
sub _sampleIDs_of{
  my $self            = shift;
  my $context          = shift; 
  my $resource        = shift;
  my $query_stmt      = "";
  my $excute_status   = ''; 
    
  # Determining which query to run depending on resource type.
  if ( $context eq 'slide' ) {
    $query_stmt = $self->{sampleIDs_of_query_stmts}{slide};
    # Used to match mapset on just the slide name
    $resource = $resource.".%.%"; 
    
  } elsif ( $context eq 'mapset' ){
    $query_stmt = $self->{sampleIDs_of_query_stmts}{mapset};
    
  } elsif ( $context eq 'primary_library' ){
    $query_stmt = $self->{sampleIDs_of_query_stmts}{primary_library};
    
  } elsif ( $context eq 'project' ){
    $query_stmt = $self->{sampleIDs_of_query_stmts}{project};
    
  } elsif ( $context eq 'sample' ){
    $query_stmt = $self->{sampleIDs_of_query_stmts}{sample};
  
  }elsif ( $context eq 'microarray' ){
       $query_stmt = $self->{sampleIDs_of_query_stmts}{microarray};
  
  }elsif ( $context eq 'mergedbam' ){
      $query_stmt = $self->{sampleIDs_of_query_stmts}{mergedbam};   
      
  }else{
    qlogprint( {l=>'INFO'}, "Unknown context $context to find SampleID for.\n");
    return 0;
  }
  
  # Prepare SQL statement.  
  $self->{geneus_cursor} = $self->{geneus_conn}->prepare($query_stmt);
    if ( ! $self->{geneus_cursor} ) {
        $self->_dberror("Could not prepare statement for SampleIDs of $resource");
        return $excute_status;        
    }
        
    $self->{geneus_cursor}->bind_param(1, $resource);
    # Execute SQL statement and collect sample IDs if execute successful.
    $excute_status = $self->{geneus_cursor}->execute();
    if ( ! defined ($excute_status) ) {
        $self->_dberror("Could not execute statement for metadata");
    }else{
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
  qlogprint( {l=>'INFO'}, "No Samples found for $context $resource\n");
    return $excute_status;
  
}



################################################################################
=cut _sample_metadata_query_stmt()

Sets class variable "sample_metadata_query_stmt" with a template SQL statement 
for sample metadata of a set of Sample ID's.

Parameters:
 None
 
Returns:
 None
=cut
sub _sample_metadata_query_stmt {
  my $self   = shift;
  
  $self->{sample_metadata_query_stmt} = qq{
  SELECT  
    project.parent_project                AS "parent_project", 
    replace( replace( project.name, '-', '_'), '.', '_' )   
                                          AS "project",
    project.limsid                        AS "project_lims_id",
    project.study_group                   AS "study_group",
    project.project_open                  AS "project_open",
    project_prefix_map.prefix             AS "project_prefix",
    project_prefix_map.parent_path        AS "parent_project_path",
    project_prefix_map.label              AS "parent_project_label",
    sample.name                           AS "sample_name",
    sample.limsid                         AS "sample_lims id",
    sample.sample_code                    AS "sample_code",
    sample.material                       AS "material",
    sample.researcher_annotation          AS "researcher_annotation",
    sample.species_reference_genome       AS "species_reference_genome",
    genome.genome_file                    AS "reference_genome_file"
  FROM qcmg.project
  LEFT JOIN qcmg.project_prefix_map ON substring(project.name from E'^[A-Z0-9a-z]+') = project_prefix_map.prefix 
  -- LEFT JOIN project_prefix_map ON project.parent_project = project_prefix_map.parent
  INNER JOIN qcmg.sample on sample.projectid = project.projectid
  INNER JOIN qcmg.genome ON genome.species_reference_genome = sample.species_reference_genome
  LEFT JOIN qcmg.gff gffspecies ON sample.species_reference_genome = gffspecies.species_reference_genome    
  };
}



################################################################################
=cut _mapset_metadata_query_stmt()

Sets class variable "mapset_metadata_query_stmt" with a template SQL statement 
for mapset metadata of a set of Sample ID's and related mapsets.

Parameters:
 None
 
Returns:
 None
=cut
sub _mapset_metadata_query_stmt {
  my $self   = shift;
  
  $self->{mapset_metadata_query_stmt} = qq{
    SELECT  
      project.parent_project                  AS "parent_project", 
      replace( replace( project.name, '-', '_'), '.', '_' )   
                                              AS "project",      
      project.limsid                          AS "project_lims_id",
      project.study_group                     AS "study_group",
      project.project_open                    AS "project_open",
      project_prefix_map.prefix               AS "project_prefix",
      project_prefix_map.parent_path          AS "parent_project_path",
      project_prefix_map.label                AS "parent_project_label",
      sample.name                             AS "sample_name",
      sample.limsid                           AS "sample_lims id",
      sample.sample_code                      AS "sample_code",
      sample.material                         AS "material",
      sample.researcher_annotation            AS "researcher_annotation",
      mapset.name                             AS "mapset",
      mapset.limsid                           AS "mapset_lims_id",
      mapset.primary_library                  AS "primary_library", 
      mapset.library_protocol                 AS "library_protocol", 
      mapset.capture_kit                      AS "capture_kit",
      coalesce(gfflib.gff_file, gffcapture.gff_file, gffspecies.gff_file)       
                                              AS "gff_file",
      mapset.sequencing_platform              AS "sequencing_platform", 
      mapset.aligner                          AS "aligner",
      sample.alignment_required               AS "alignment_required",
      sample.species_reference_genome         AS "species_reference_genome",
      genome.genome_file                      AS "reference_genome_file",
      mapset.failed_qc                        AS "failed_qc",
      mapset.isize_min                        AS "isize_min", 
      mapset.isize_max                        AS "isize-max",
      mapset.isize_manually_reviewed          AS "isize_manually_reviewed"
    FROM qcmg.project
    LEFT JOIN qcmg.project_prefix_map ON substring(project.name from E'^[A-Z0-9a-z]+') = project_prefix_map.prefix
    --LEFT JOIN project_prefix_map ON project.parent_project = project_prefix_map.parent
    INNER JOIN qcmg.sample on sample.projectid = project.projectid
    INNER JOIN qcmg.genome ON genome.species_reference_genome = sample.species_reference_genome
    INNER JOIN qcmg.mapset on mapset.sampleid = sample.sampleid 
    LEFT JOIN qcmg.gff gfflib ON mapset.library_protocol ~ gfflib.library_protocol 
    LEFT JOIN qcmg.gff gffcapture ON mapset.capture_kit = gffcapture.capture_kit
    LEFT JOIN qcmg.gff gffspecies ON sample.species_reference_genome = gffspecies.species_reference_genome    
  };
}



################################################################################
=cut _microarray_metadata_query_stmt()

Sets class variable "microarray_metadata_query_stmt" with a template SQL statement 
for mircoarray metadata of a set of Sample ID's and related mircoarrays.

Parameters:
 None
 
Returns:
 None
=cut
sub _microarray_metadata_query_stmt {
  my $self   = shift;
  
  $self->{microarray_metadata_query_stmt} = qq{
    SELECT
      project.parent_project              AS "parent_project", 
      replace( replace( project.name, '-', '_'), '.', '_' )   AS "project",      
      project.limsid                      AS "project_lims_id",
      project.study_group                 AS "study_group",
      project.project_open                AS "project_open",
      project_prefix_map.prefix           AS "project_prefix",
      project_prefix_map.parent_path      AS "parent_project_path",
      project_prefix_map.label            AS "parent_project_label",
      sample.name                         AS "sample_name",
      sample.limsid                       AS "sample_lims id",
      sample.sample_code                  AS "sample_code",
      sample.material                     AS "material",
      sample.researcher_annotation        AS "researcher_annotation",
      arrayscan.name                      AS "array",
      arrayscan.array_type                AS "array_type",
      arrayscan.array_class               AS "array_class",
      arrayscan.array_protocol            AS "array_protocol",
      arrayscan.failed_qc                 AS "failed_qc"
    FROM qcmg.project
    LEFT JOIN qcmg.project_prefix_map ON substring(project.name from E'^[A-Z0-9a-z]+') = project_prefix_map.prefix
    --LEFT JOIN project_prefix_map ON project.parent_project = project_prefix_map.parent
    INNER JOIN qcmg.sample ON project.projectid = sample.projectid 
    INNER JOIN qcmg.arrayscan ON sample.sampleid = arrayscan.sampleid
  };
  
}



################################################################################
=cut _mergedbam_metadata_query_stmt()

Sets class variable "mergedbam_metadata_query_stmt" with a template SQL statement 
for mergedbam metadata of a set of Sample ID's and related mergedbams.

Parameters:
 None
 
Returns:
 None
=cut
sub _mergedbam_metadata_query_stmt {
  my $self   = shift;
  
  $self->{mergedbam_metadata_query_stmt} = qq{
    SELECT 
      project.parent_project                  AS "parent_project",
      replace( replace( project.name, '-', '_'), '.', '_' )   
                                              AS "project",      
      project.limsid                          AS "project_lims_id", 
      project.project_open                    AS "project_open",
      project_prefix_map.parent_path          AS "parent_project_path",
      mergedbam.name                          AS "mergedbam",
      mergedbam.sample_code                   AS "sample_code", 
      mergedbam.sample                        AS "sample_name",
      mergedbam.material                      AS "material", 
      mergedbam.library_protocol              AS "library_protocol", 
      mergedbam.capture_kit                   AS "capture_kit", 
      mergedbam.aligner                       AS "aligner", 
      mergedbam.sequencing_platform           AS "sequencing_platform"
    FROM qcmg.mergedbam
    INNER JOIN qcmg.sample  ON sample.name = mergedbam.sample
    INNER JOIN qcmg.project ON project.projectid = sample.projectid
    INNER JOIN qcmg.project_prefix_map ON substring(project.name from E'^[A-Z0-9a-z]+') = project_prefix_map.prefix
  };
}



################################################################################
=cut _mapsets_in_mergedbam_query_stmt()

Sets class variable "mapsets_in_mergedbam_query_stmt" with a template SQL statement 
for mapset of a mergedbam.

Parameters:
 None
 
Returns:
 None
=cut
sub _mapsets_in_mergedbam_query_stmt {
  my $self   = shift;
  
  $self->{mapsets_in_mergedbam_query_stmt} = qq{
    SELECT 
      mapset.name,
      mergedbam.name
    FROM qcmg.mergedbam
    INNER JOIN qcmg.mapset_mergedbam ON mergedbam.mergedbamid = mapset_mergedbam.mergedbamid
    LEFT JOIN qcmg.mapset ON mapset.limsid = mapset_mergedbam.mapset_limsid
    WHERE mergedbam.name = ?
  };
}



################################################################################
=cut _metadata_of_resources()

Executes SQL statement for metadata of a set of Sample ID's and related mapsets.

Parameters:
 query_stmt:    string      
 resources:     array string  Array of sample IDs.
 
Returns:
 Execution status: scalar interger.
 
Returns the execution status of the SQL statement 1 if succefull. Otherwise 0.
=cut
sub _metadata_of_resources{
  my $self            = shift;
  my $query_stmt      = shift; 
  my $resources       = shift @_;
  my $excute_status   = ''; 
  
  # Append where clause to statement if there are specific samples we want
  # metadata for. However if 'all_resources' is set, Skip.
  if ( ! $self->{all_resources} and $resources ) {
  # create a string of resources for sql statement.
    my $samples = join(', ', map { "'$_'" } @$resources);
          
    $query_stmt = $query_stmt." WHERE sample.name in ( $samples ) ";  
    $self->_debug ("Searching for metadata of these samples $samples");
  }
  # Reset all_resources.
  $self->{all_resources} = 0;
    
  # Prepare SQL statement for execution.
  $self->{geneus_cursor} = $self->{geneus_conn}->prepare($query_stmt);
  if ( ! $self->{geneus_cursor} ) {
      $self->_dberror("Could not prepare statement for metadata");
      return $excute_status;        
  }
  
  # Execute prepared QSL statement 
  $excute_status = $self->{geneus_cursor}->execute();
  if ( ! defined ($excute_status) ) {
      $self->_dberror("Could not execute statement for metadata");
  }
  
  # Returns execution status.
  return $excute_status;
}



################################################################################
=cut _parentprojects()

Executes SQL statement for parent projects and details

Parameters:
 None
 
Returns:
 Execution status: scalar interger.
 
Returns the execution status of the SQL statement 1 if succefull. Otherwise 0.
=cut
sub _parentprojects{
  my $self            = shift;
  my $query_stmt      = $self->{parentprojects_query_stmt}; 
  my $excute_status   = ''; 
  
  $self->_debug ("Searching for parentprojects");

  # Reset all_resources.
  $self->{all_resources} = 0;
  
  # Prepare SQL statement for execution.
  $self->{geneus_cursor} = $self->{geneus_conn}->prepare($query_stmt);
  if ( ! $self->{geneus_cursor} ) {
      $self->_dberror("Could not prepare statement for metadata");
      return $excute_status;        
  }
  
  # Execute prepared QSL statement 
  $excute_status = $self->{geneus_cursor}->execute();
  if ( ! defined ($excute_status) ) {
      $self->_dberror("Could not execute statement for metadata");
  }
  
  # Returns execution status.
  return $excute_status;
}



################################################################################
=cut _projects_of_parentproject()

Executes SQL statement for projects of a given parent project.

Parameters:
 None
 
Returns:
 Execution status: scalar interger.
 
Returns the execution status of the SQL statement 1 if succefull. Otherwise 0.
=cut
sub _projects_of_parentproject{
  my $self            = shift;
  my $parentproject   = shift;
  my $query_stmt      = $self->{projects_of_parentprojects_query_stmt}; 
  my $excute_status   = ''; 
    
  # Reset all_resources.
  $self->{all_resources} = 0;
  
  $self->_debug ("Searching for project of parentprojects");

  # Prepare SQL statement for execution.
  $self->{geneus_cursor} = $self->{geneus_conn}->prepare($query_stmt);
    
  if ( ! $self->{geneus_cursor} ) {
      $self->_dberror("Could not prepare statement for metadata");
      return $excute_status;        
  }
  $self->{geneus_cursor}->bind_param(1, $parentproject);
  
  # Execute prepared QSL statement 
  $excute_status = $self->{geneus_cursor}->execute();
  if ( ! defined ($excute_status) ) {
      $self->_dberror("Could not execute statement for metadata");
  }
  
  # Returns execution status.
  return $excute_status;
}



################################################################################
=cut _mapsets_in_mergedbam()

Executes SQL statement for mapsets in a given mergedbam.

Parameters:
 None
 
Returns:
 Execution status: scalar interger.
 
Returns the execution status of the SQL statement 1 if succefull. Otherwise 0.
=cut
sub _mapsets_in_mergedbam{
  my $self            = shift;
  my $mergedbam       = shift;
  my $query_stmt      = $self->{mapsets_in_mergedbam_query_stmt}; 
  my $excute_status   = ''; 
    
  # Reset all_resources.
  $self->{all_resources} = 0;
  
  $self->_debug ("Searching for mapsets in mergedbam");

  # Prepare SQL statement for execution.
  $self->{geneus_cursor} = $self->{geneus_conn}->prepare($query_stmt);
    
  if ( ! $self->{geneus_cursor} ) {
      $self->_dberror("Could not prepare statement for metadata");
      return $excute_status;        
  }
  $self->{geneus_cursor}->bind_param(1, $mergedbam);
  
  # Execute prepared QSL statement 
  $excute_status = $self->{geneus_cursor}->execute();
  if ( ! defined ($excute_status) ) {
      $self->_dberror("Could not execute statement for mapsets in mergedbam");
  }
  
  # Returns execution status.
  return $excute_status;
}



################################################################################
=cut _fetch_sample_metadata()

Returns sample metadata from a query on samples

Parameters:
 None
 
Returns:
 Array of hashes.   - Values are
    parent_project, project, project_limsid, study_group, project_open,
    project_prefix, parent_project_path, parent_project_label, 
    sample, sample_limsid, sample_code, material, researcher_annotation,
    species_reference_genome, reference_genome_file
=cut

sub _fetch_sample_metadata {
  my $self = shift; 
    
  # Assigning variables for binding returned metadata. 
  # Number of variables must matche the number of column in SQL query $self->{mapset_metadata_query_stmt}
  my (  $parent_project, $project, $project_limsid, $study_group, $project_open,
      $project_prefix, $parent_project_path, $parent_project_label,
      $sample, $sample_limsid, 
      $sample_code, $material, $researcher_annotation,
      $species_reference_genome, $reference_genome_file  
    );
  # binding must be to a refernce to the variable
  $self->{geneus_cursor}->bind_columns(
      \$parent_project, \$project, \$project_limsid, \$study_group, \$project_open,
      \$project_prefix, \$parent_project_path, \$parent_project_label,
      \$sample, \$sample_limsid, 
      \$sample_code, \$material, \$researcher_annotation,
      \$species_reference_genome, \$reference_genome_file  
    );

  my @metadata = ();
  # foreach row assign column values to a hash.
  while ( $self->{geneus_cursor}->fetchrow_arrayref() ) {      
    my $mapset_details = {
        parent_project            => $parent_project,
        project                   => $project,
        project_limsid            => $project_limsid,
        study_group               => $self->_studygroup_json_to_array($study_group),
        project_open              => $project_open,
        project_prefix            => $project_prefix, 
        parent_project_path       => $parent_project_path,
        parent_project_label      => $parent_project_label,
        sample                    => $sample,
        sample_limsid             => $sample_limsid,
        sample_code               => $sample_code,
        material                  => $material,
        researcher_annotation     => ($researcher_annotation ? $researcher_annotation : ""),
        species_reference_genome  => $species_reference_genome,
        reference_genome_file     => $reference_genome_file
    };
    push(@metadata, $mapset_details);
  } 
  # returns an array of hashes.   
  return \@metadata;
}



################################################################################
=cut _fetch_mapset_metadata()

Returns mapset metadata from a query on sample(s) mapsets

Parameters:
 None
 
Returns:
 Array of hashes.   - Values are
    parent_project, project, project_limsid, study_group, project_open,
    project_prefix, parent_project_path, parent_project_label, 
    sample, sample_limsid, sample_code, material, researcher_annotation,
    mapset, mapset_limsid, primary_library, library_protocol, capture_kit, 
    gff_file, sequencing_platform, aligner, alignment_required, 
    species_reference_genome, reference_genome_file, failed_qc, isize_min,
    isize_max, isize_manually_reviewed  
=cut
sub _fetch_mapset_metadata {
    my $self = shift; 
    
  # Assigning variables for binding returned metadata. 
  # Number of variables must matche the number of column in SQL query $self->{mapset_metadata_query_stmt}
    my (  $parent_project, $project, $project_limsid, $study_group, $project_open,
      $project_prefix, $parent_project_path, $parent_project_label,
      $sample, $sample_limsid, 
      $sample_code, $material, $researcher_annotation,
      $mapset, $mapset_limsid, $primary_library, $library_protocol,
      $capture_kit, $gff_file,  $sequencing_platform,      
      $aligner, $alignment_required, $species_reference_genome, $reference_genome_file,  
      $failed_qc, 
      $isize_min, $isize_max, $isize_manually_reviewed
    );
  # binding must be to a refernce to the variable
    $self->{geneus_cursor}->bind_columns(
      \$parent_project, \$project, \$project_limsid, \$study_group, \$project_open,
      \$project_prefix, \$parent_project_path, \$parent_project_label,
      \$sample, \$sample_limsid, 
      \$sample_code, \$material, \$researcher_annotation,
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
          parent_project            => $parent_project,
          project                   => $project,
          project_limsid            => $project_limsid,
          study_group               => $self->_studygroup_json_to_array($study_group),
          project_open              => $project_open,
          project_prefix            => $project_prefix, 
          parent_project_path       => $parent_project_path,
          parent_project_label      => $parent_project_label,
          sample                    => $sample,
          sample_limsid             => $sample_limsid,
          sample_code               => $sample_code,
          material                  => $material,
          researcher_annotation     => ($researcher_annotation ? $researcher_annotation : ""),
          mapset                    => $mapset,
          mapset_limsid             => $mapset_limsid,
          primary_library           => ( $primary_library ? $primary_library : "" ),
          library_protocol          => ( $library_protocol ? $library_protocol : "" ) ,   
          capture_kit               => $capture_kit,
          gff_file                  => $gff_file,
          sequencing_platform       => $sequencing_platform,
          aligner                   => $aligner,
          alignment_required        => ( $alignment_required ?   $alignment_required : 0),
          species_reference_genome  => $species_reference_genome,
          reference_genome_file     => $reference_genome_file,
          failed_qc                 => ( $failed_qc ?   $failed_qc : 0),
          isize_min                 => ( $isize_min ?   $isize_min : 0), 
          isize_max                 => ( $isize_max ?   $isize_max : 0),
          isize_manually_reviewed   => ( $isize_manually_reviewed ?   $isize_manually_reviewed: 0)      
      };
      push(@metadata, $mapset_details);
    } 
  # returns an array of hashes.   
  return \@metadata;
}



################################################################################
=cut _fetch_microarray_metadata()

Returns microarray metadata from a query on sample(s) microarrays

Parameters:
 None
 
Returns:
 Array of hashes.   - Values are
    parent_project, project, project_limsid, study_group, project_open,
    project_prefix, parent_project_path, parent_project_label, 
    sample, sample_limsid, sample_code,  material, researcher_annotation,
    array_name, array_type, array_class, array_protocol, failed_qc
=cut
sub _fetch_microarray_metadata {
    my $self = shift; 
    
  # Assigning variables for binding returned metadata. 
  # Number of variables must matche the number of column in SQL query $self->{mircoarray_metadata_query_stmt}
  my ($parent_project, $project, $project_limsid, $study_group, $project_open,
      $project_prefix, $parent_project_path, $parent_project_label, 
      $sample, $sample_limsid, 
      $sample_code, $material, $researcher_annotation,
      $array, $array_type, $array_class, $array_protocol, $failed_qc
    );

  # binding must be to a refernce to the variable
    $self->{geneus_cursor}->bind_columns(
      \$parent_project, \$project, \$project_limsid, \$study_group, \$project_open,
      \$project_prefix, \$parent_project_path, \$parent_project_label, 
      \$sample, \$sample_limsid, 
      \$sample_code, \$material, \$researcher_annotation,
      \$array, \$array_type, \$array_class, \$array_protocol, \$failed_qc
    );
    
    my @metadata = ();
  # foreach row assign column values to a hash.
    while ( $self->{geneus_cursor}->fetchrow_arrayref() ) {
      my $array_details = {
          parent_project        => $parent_project,
          project               => $project,
          project_limsid        => $project_limsid,
          study_group           => $self->_studygroup_json_to_array($study_group),
          project_open          => $project_open,
          project_prefix        => $project_prefix, 
          parent_project_path   => $parent_project_path,
          parent_project_label  => $parent_project_label,
          sample                => $sample,
          sample_limsid         => $sample_limsid,
          sample_code           => $sample_code,
          material              => $material,
          researcher_annotation => ($researcher_annotation ? $researcher_annotation : ""),
          array                 => $array,
          array_type            => $array_type,
          array_class           => $array_class,
          array_protocol        => $array_protocol,
          failed_qc             => ( $failed_qc ?   $failed_qc : 0)
      };
      push(@metadata, $array_details);
    } 
  # returns an array of hashes.   
    return \@metadata;
}



################################################################################
=cut _fetch_mergedbam_metadata()

Returns mergedbam metadata from a query on sample(s) mergedbams

Parameters:
 None
 
Returns:
 Array of hashes.   - Values are
    parent_project, project, project_limsid, project_open, parent_project_path, 
    mergedbam, sample, sample_code,  material
    library_protocol, capture_kit, aligner, sequencing_platform
=cut
sub _fetch_mergedbam_metadata {
  my $self = shift; 
    
  # Assigning variables for binding returned metadata. 
  # Number of variables must matche the number of column in SQL query $self->{mircoarray_metadata_query_stmt}
  my ($parent_project, $project, $project_limsid, $project_open,
      $parent_project_path, $mergedbam, 
      $sample_code, $sample, $material, 
      $library_protocol, $capture_kit, $aligner, $sequencing_platform
    );

  # binding must be to a refernce to the variable
    $self->{geneus_cursor}->bind_columns(
      \$parent_project, \$project, \$project_limsid, \$project_open,
      \$parent_project_path, \$mergedbam,
      \$sample_code, \$sample, \$material,
      \$library_protocol, \$capture_kit, \$aligner, \$sequencing_platform
    );
    
    my @metadata = ();
  # foreach row assign column values to a hash.
    while ( $self->{geneus_cursor}->fetchrow_arrayref() ) {
      my $array_details = {
          parent_project            => $parent_project,
          project                   => $project,
          project_limsid            => $project_limsid,
          project_open              => $project_open,
          parent_project_path       => $parent_project_path,
          mergedbam                 => $mergedbam,
          sample                    => $sample,
          sample_code               => $sample_code,
          material                  => $material,
          library_protocol          => ($library_protocol ? $library_protocol : "") ,   
          capture_kit               => $capture_kit,
          aligner                   => $aligner,
          sequencing_platform       => $sequencing_platform
      };
      push(@metadata, $array_details);
    } 
  # returns an array of hashes.   
    return \@metadata;
}



#################################################################################
=cut _fetch_mapsets_in_mergedbam()

Returns mapsets from a query on a mergedbam

Parameters:
 None
 
Returns:
 Array of hashes.   - Values are
    mapset, mergedbam
=cut
sub _fetch_mapsets_in_mergedbam {
  my $self = shift; 
   
  # Assigning variables for binding returned metadata. 
  # Number of variables must matche the number of column in 
  # SQL query $self->{_mapsets_in_mergedbam}
  my ($mapset, $mergedbam);
  
  # binding must be to a refernce to the variable
  $self->{geneus_cursor}->bind_columns( \$mapset, \$mergedbam );
    
  my @metadata = ();
  # foreach row assign column values to a hash.
  while ( $self->{geneus_cursor}->fetchrow_arrayref() ) {
    my $mapset_details = {
      mapset                    => $mapset,
      mergedbam                 => $mergedbam
    };
    push(@metadata, $mapset_details);
  } 
  # returns an array of hashes.   
  return \@metadata;
}



################################################################################
=cut _fetch_parentprojects()

Returns parent project metadata from a query on parentprojects

Parameters:
 None
 
Returns:
 Array of hashes.   - Values are
    project_project, project_prefix, parent_project_path, parent_project_label, 
    array_samples
=cut
sub _fetch_parentprojects {
  my $self = shift; 
  
  # Assigning variables for binding returned metadata. 
  # Number of variables must matche the number of column in SQL query $self->{}
  my ($parent_project, $project_prefix, $parent_project_path, 
      $parent_project_label, $array_samples
  );

  # binding must be to a refernce to the variable
  $self->{geneus_cursor}->bind_columns(
    \$parent_project, \$project_prefix, \$parent_project_path, 
    \$parent_project_label, \$array_samples
  );
  my @metadata = ();
  # foreach row assign column values to a hash.
  while ( $self->{geneus_cursor}->fetchrow_arrayref() ) {
    my $array_details = {
        parent_project        => $parent_project,
        project_prefix        => $project_prefix, 
        parent_project_path   => $parent_project_path,
        parent_project_label  => $parent_project_label,
        array_samples         => $array_samples
    };
    push(@metadata, $array_details);
  } 
  # returns an array of hashes.   
  return \@metadata;
}



################################################################################
=cut _fetch_parentprojects()

Returns projects and their parent projects projects

Parameters:
 None
 
Returns:
 Array of hashes.   - Values are
    project_project, project
=cut
sub _fetch_projects_of_parentprojects {
  my $self = shift; 
  
  # Assigning variables for binding returned metadata. 
  # Number of variables must matche the number of column in SQL query $self->{}
  my ( $parent_project, $project, $project_open, $study_group );

  # binding must be to a refernce to the variable
  $self->{geneus_cursor}->bind_columns( \$parent_project, \$project, \$project_open, \$study_group );
  my @metadata = ();
  # foreach row assign column values to a hash.
  while ( $self->{geneus_cursor}->fetchrow_arrayref() ) {
    my $array_details = {
        parent_project        => $parent_project,
        project               => $project,
        project_open          => $project_open,
        study_group           => $self->_studygroup_json_to_array($study_group)
    };
    push(@metadata, $array_details);
  } 
  # returns an array of hashes.   
  return \@metadata;
}
  



################################################################################
=cut _studygroup_json_to_array()

Removes json formating from arround a list of study groups.

Parameters:
 studygroups: scalar string   list of study groups in a json string from LIMS
 
Returns:
 Array: string   list of just the they study groups with out json formating
=cut
sub _studygroup_json_to_array {
  my $self        = shift;
  my $studygroups = shift;
  
  # It may not have a value.
  if ($studygroups) {
    my $decoded_json = decode_json( $studygroups );
    my @studygroup = keys %{$decoded_json};
    return \@studygroup;
  }
  
  return [];
}


#   End of PRIVATE METHODS


################################################################################
# # #
# #    Public Functions 
#
################################################################################

=pod

B<fetch_metadata()>

Fetches metadata for samples of the desired resource. The metadata returned 
depends on the 'type' requested. Which is 'mapset', 'microarray' or 'mergedbam'. 
If no type is given, the subroutine defaults to 'mapset'

Parameters:
 type: scalar string - Type of metadata to be returned. (Optional)
         valid values are 'mapset', 'microarray' or 'mergedbam'. 
        Defaults to 'mapset'.

Returns:
 Array of hashes.

 For mapset - keys are: 
    parent_project, project, project_limsid, study_group, project_open, 
    project_prefix, parent_project_path, parent_project_label, 
    sample, sample_limsid, sample_code, material, researcher_annotation,
    mapset, mapset_limsid, primary_library, library_protocol, capture_kit, 
    gff_file, sequencing_platform, aligner, alignment_required, 
    species_reference_genome, reference_genome_file, failed_qc, isize_min,
    isize_max, isize_manually_reviewed  

 For mircoarrays - values are:
    parent_project, project, project_limsid, study_group, project_open, 
    project_prefix, parent_project_path, parent_project_label, 
    sample, sample_limsid, sample_code,  material, researcher_annotation,
    array_name, array_type, array_class, array_protocol, failed_qc
 
For mergedbam - values are:
    parent_project, project, project_limsid, project_open, parent_project_path, 
    mergedbam, sample, sample_code,  material
    library_protocol, capture_kit, aligner, sequencing_platform
    
 
A request for returning metadata can only work after a sucessful return from 
either a particular resource request using B<resource_metadata()> or for all 
resources using B<all_resources_metadata()>
=cut
sub fetch_metadata {
  my $self = shift;
  my $type = shift;
  
  if (! $type) {
    $type = 'mapset';
  }
  
  # Return mapset metadata
  if ( $type eq 'mapset' ) {
    if ( $self->_metadata_of_resources($self->{mapset_metadata_query_stmt}, $self->{sampleIDs}) ){
      return $self->_fetch_mapset_metadata();
    }
  
  # Return microarray metadata  
  }elsif ( $type eq 'microarray' ){
    if ( $self->_metadata_of_resources($self->{microarray_metadata_query_stmt},  $self->{sampleIDs}) ){
      return $self->_fetch_microarray_metadata();
    }
  
  # Return microarray metadata  
  }elsif ( $type eq 'sample' ){
    if ( $self->_metadata_of_resources($self->{sample_metadata_query_stmt},  $self->{sampleIDs}) ){
      return $self->_fetch_sample_metadata();
    }
    
  # Return mergedbam metadata
  }elsif ( $type eq 'mergedbam' ){
    if ( $self->_metadata_of_resources($self->{mergedbam_metadata_query_stmt},  $self->{sampleIDs}) ){
      return $self->_fetch_mergedbam_metadata();
    }

    # Default to mapset metadata
  }else{
    if ( $self->_metadata_of_resources($self->{mapset_metadata_query_stmt}, $self->{sampleIDs}) ){
      return $self->_fetch_mapset_metadata();
    }
  }
  
  return 0;
}



################################################################################
=pod

B<all_resources_metadata()>

Requests for metadata of all metadata in Geneus.

Parameters:
 None

Returns:
 Execution status: scalar interger.
 
Returns 1 (successful). Otherwise 0.
=cut
sub all_resources_metadata{
    my $self = shift;

    return $self->{all_resources} = 1;
}



################################################################################
=pod

B<resource_metadata()>

Requests for metadata of for a particular resource.

Parameters:
 resource_type: scalar string - Type of resource being searched.
       valid values are "slide", "mapset", "primary_library", 
        "sample", "project", "microarray", "mergedbam"
 
 resource:  scalar string  - Name of the resource

Returns:
 Execution status: scalar interger.
 
Returns 1 (successful) if samples are found for the resource. Otherwise 0.

Metadata to be return will be for all All samples that the resource matches 
useing the method B<fetch_metadata()>.  
IE. A slide might be associcated with multiple samples.

If you intend on using this method, It is recommended that you use the module 
QCMG::DB::Metadata instead. It gives you a better interface for obtaining 
specific metadata or sets of metadata.
=cut
sub resource_metadata{
    my $self            = shift;
    my $resource_type   = shift;
    my $resource        = shift;
  
    $self->_debug("resource_metadata - searching for samples for $resource_type $resource");
    # Sample
    if ( $resource_type eq "sample") {
        $self->_debug("Sample");
    $self->{sampleIDs} = $self->_sampleIDs_of('sample', $resource);
      
  # Donor
    }elsif ( $resource_type eq "project") {
        $self->_debug("project");
        $self->{sampleIDs} = $self->_sampleIDs_of('project', $resource);
    
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
          
    # Mergedbam
    }elsif ($resource_type eq "mergedbam") {
        $self->_debug("Mergedbam");
        $self->{sampleIDs} = $self->_sampleIDs_of('mergedbam', $resource);
          
    # Everything Else
    }else{
        $self->_debug("Nothing");
        $self->{sampleIDs} = ();
    return 0;
    }
  
  # If Samples found return successful;
    if ( $self->{sampleIDs} != 0 ){
      #foreach my $sample ( @{$self->{sampleIDs}} ){
      #    #$sample = "'".$sample."'";
      #}
        my $samples = join ', ', @{$self->{sampleIDs}};
    $self->_debug("Found SampleIDs: $samples");
    #$self->_debug_array( $self->{sampleIDs} );
      return 1; # True
    }
      
    return 0;
}



################################################################################
# # #
# #       This is breaking from the original MODULE concepts 
#         but should tie in quite nicely.
#
################################################################################
=pod

B<parentprojects()>

Returns a complete list of parent projects.

Parameters:
 None

Returns:
  Array of hashes.
 
  Hash keys are: 
      parent_project, project_prefix, parent_project_path, 
      parent_project_label, array_samples

=cut
sub parentprojects {
  my $self      = shift;
  my $option    = shift;
  
  if ( $self->_parentprojects() ){
      return $self->_fetch_parentprojects();
  }
  return 0;
}



################################################################################
=pod

B<projects_of_parentproject()>

Returns a list of projects for a given parent project.

Parameters:
 parent_project: scalar string    name of the parent project

Returns:
  Array: hash     List of projects to parent projects.

  Hash keys are: 
      parent_project, project, project_open 
=cut
sub projects_of_parentproject {
  my $self              = shift;
  my $parent_project    = shift;
  
  if ( $parent_project and $self->_projects_of_parentproject($parent_project) ){
    return $self->_fetch_projects_of_parentprojects();
  }
  return 0
}



################################################################################
=pod

B<mapsets_in_mergedbam()>

Returns a list of mapsets in a mergedbam.

Parameters:
 mergedbam: scalar string       name of the mergedbam

Returns:
  Array: Hash     List of mapset to mergedbam.
 
  Hash keys are: 
      mapset, mergedbam 
=cut
sub mapsets_in_mergedbam {
  my $self          = shift;
  my $mergedbam     = shift;
  
  if ( $mergedbam and $self->_mapsets_in_mergedbam($mergedbam) ){
    return $self->_fetch_mapsets_in_mergedbam();
  }
  return 0
}



################################################################################
# # #
# #      SPECIAL CASE QUERIES 
#
#  These don't fit the data structure of QCMG::DB::Metadata module.
#  However they are required for legacy sequencing platforms. 
# 
#    Some of these functions require either different databases such as the
#   Geneus schema directly and hence have their own connection to the database 
#   and not the connection handle used by the class.
# 
################################################################################

=pod

B<solid_primaries()>

Returns details on the location of primary files from a given SOLiD slide.

Parameters:
 resource:  scalar string  - Name of a SOLiD slide.
 
Returns:
 Array of hashes or 0 if no results.
     values being: 'slide_name', 'bc_primary_reads', 
    'f3_primary_reads', 'f5_primary_reads', 'r3_primary_reads'
 

This method has is own connection to the Geneus schema as this information is
to specific to the SOLiD 4 and earlier to be included in the QCMG schema that 
the rest of this class uses. So there is a small performance hit every time 
this method is called to connect to the database.
=cut
sub solid_primaries {
    my $self        = shift; 
    my $resource    = shift;
    
    my @sections = split /\./, $resource;
    $resource = $sections[0];
    
  # SQL statement for finding SOLiD primary_reads.
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
         die "can't connect to database server: $!\n";
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
