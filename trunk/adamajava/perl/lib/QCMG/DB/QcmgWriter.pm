package QCMG::DB::QcmgWriter;

################################################################################
#
#  Module:   QCMG::DB::QcmgWriter.pm
#  Creator:  Matthew J Anderson
#  Created:  2013-02-26
#
# This class's primary purpose is for populating tables in the "qcmg" schema
# and not the Geneus schema.
# 
#  
#  $Id
#
################################################################################


=pod

=head1 NAME

QCMG::DB::QcmgWriter -- Common functions for querying the Geneus LIMS

=head1 SYNOPSIS

 my $geneus = QCMG::DB::QcmgWriter->new()
 ...
 

=head1 DESCRIPTION



=head1 REQUIREMENTS

 Data::Dumper
 Carp
 DBI

=cut


use strict;					          # Good practice
use warnings;				          # Good practice
use Data::Dumper;			        # 
use Carp qw( carp croak );	  # 
use DBI;					# 

use QCMG::Util::QLog;                 # QCMG loging module

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4694 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: QcmgWriter.pm 4694 2014-08-29 04:30:06Z m.anderson $'
    =~ /\$Id:\s+(.*)\s+/;




################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 debug: scalar boolean (optional)	  Set as 1 for debugging.

Returns:
 a new instance of this class.

=cut

sub new {
  my $class   		= shift;
  my $debug  			= shift;
	my $autoCommit_off	= shift;
    
	# Database credentials. 
	# TODO: Use a credential file in the future. 
  my $db_settings = {
      host    	=> undef,
      db      	=> undef,
      user    	=> undef,
      passwd  	=> undef,
	autocommit	=> ( $autoCommit_off ?  0 :  1  ) 	# Default is ON. 
  };
    
	# Store the connection, cursor and prepared statements for the life of the class 
  my $self = {
      geneus_conn     => '',
      geneus_cursor   => '',
      debug           => ( $debug ?  1 :  0  ),
      prepared_stmt   => {}
  };    

  # Create connection to qcmg schema
  my $connect_string = 'DBI:Pg:dbname='.$db_settings->{db}.';host='.$db_settings->{host};
  $self->{geneus_conn} = DBI->connect( $connect_string,
                                      $db_settings->{user},
                                      $db_settings->{passwd},
                                      { RaiseError         => 1,
                                        ShowErrorStatement => 0,
                                        AutoCommit         => $db_settings->{autocommit}
									} ) ||
      croak "can't connect to database server: $!\n";
  $self->{geneus_conn}->{'ChopBlanks'} = 1; # copes with padded spaces if type=char

	bless $self, $class;
	
  
	##$self->{sampleIDs_of_query_stmts} 	= {}; # constructed by _sampleIDs_of_query_stmts()
	##$self->{metadata_query_stmts}		= {}; # constructed by _metadata_query_stmts()
  
	# Store query statments.
	##$self->_sampleIDs_of_query_stmts();
	##$self->_metadata_query_stmts();
	
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

=cut _debug()

If debug has been set on class initiation the debug message will be printed.

Parameters:
 message: scalar string

Returns:
 None - May print debug message.

=cut

sub _debug {
    my $self = shift;
    my $message = shift;
    
    if ($self->{debug}) {
       print "$message\n";
    }            
}


################################################################################

=cut _debug_array()

If debug has been set on class initiation the contents of the array and debug message will be printed.

Parameters:
 array: array

Returns:
 None - May print debug message.

=cut

sub _debug_array {
    my $self = shift;
    my $array = shift;
    	
    my $message = join ', ', @$array;
    if ($self->{debug}) {
        print Dumper $array;
        print "$message\n";
    }            
}



=cut
sub _prepare_insert_stmts {
   my $self = shift;
   
   
   $self->{geneus_cursor} = $self->{geneus_conn}->prepare($insert_stmt);
   
   parent_project
   project
   sample
   mapset
   mergedbam
   arrayscan
}

if ($merged_record->{project}) {
  $key_value_stmt = qq{	
  			SELECT project.limsid FROM qcmg.project
  			WHERE replace( replace( project.name, '-', '_'), '.', '_' ) = ? 
  		};
}

if ( $key_value_stmt ) {		
	# Build insert query 
	my $insert_stmt = qq{ 
    INSERT INTO  qcmg.mergedbam
    (project_limsid, name, material, sample_code, sample, library_protocol, capture_kit,  aligner, sequencing_platform) 
		VALUES ( ($key_value_stmt), ?, ?, ?, ?, ?, ?, ?, ?)	
	};

sub _report_insert_stmts {
   my $self = shift;
   my $key_type = shift;
   
   # Parent Project reports
   if ( $key_type eq 'parent_project') {
   	$key_value_stmt = qq{	
   		SELECT distinct 0
   		FROM qcmg.project
   		WHERE replace( replace( project.name, '-', '_'), '.', '_' )  =  ?
   	};
   }
   # Project reports	
   elsif ( $key_type eq 'project') {
   	$key_value_stmt = qq{	
   		SELECT project.limsid FROM qcmg.project
   		WHERE replace( replace( project.name, '-', '_'), '.', '_' ) = ? 
   	};
   }
   # Sample reports	
   elsif ( $key_type eq 'sample') {
   	# Nothing to do yet
   	# print "[ Warning ] - Not yet expecting key type 'sample' \n";
   	$key_value_stmt = qq{
   		SELECT sample.limsid FROM qcmg.sample
   		WHERE replace( sample.name, '-', '_') || '_' || sample.snp_array_barcode || '_' || sample.snp_array_location = ? 
   	};
   }
   # Mapset reports	
   elsif ( $key_type eq 'mapset') {
   	$key_value_stmt = qq{
   		SELECT mapset.limsid FROM qcmg.mapset
   		WHERE mapset.name = ? 
   	};
   }
   # Merged Bam (final) reports
   elsif ( $key_type eq 'mergedbam') {
   	$key_value_stmt = qq{
   		SELECT mergedbam.mergedbamid::text FROM qcmg.mergedbam
   		WHERE mergedbam.name = ? 
   	};
   }
   # Array reports	
   elsif ( $key_type eq 'arrayscan') {
   	$key_value_stmt = qq{
   		SELECT arrayscan.limsid
   		FROM qcmg.arrayscan
   		INNER JOIN qcmg.sample ON sample.sampleid = arrayscan.sampleid
   		WHERE replace( sample.name, '-', '_') || '_' || arrayscan.name = ? 
   	};
   }
   ## Variant
   #elsif ( $key_type eq 'variant' ){	
   #  
   #
   #}
   # Unknown report	
   else{
   	print "[ Warning ] - Unrecognized key type: $key_type\n";
   	return ''; 
   }

   if ( $key_value_stmt ) {		
   	# Build insert query 
   	my $report_insert_stmt = qq{ 
   		INSERT INTO  qcmg.metric 
   			(key_type, key_value, 
   			resource_type, resource_value, 
   			report_type, report_value, report_path) 
   		VALUES (?, ($key_value_stmt), ?, ?, ?, ?, ?)	
   	};
    
    return $report_insert_stmt;
  }
  return '';
}



;


sub _prepared_stmt {
  my $self = shift;
  # body...
}

=cut


################################################################################
# # #
# #  	Public Functions 
#
################################################################################

=pod
B<empty_reports_table()>

Truncates the table qcmg.metric and resets the primary ID to 0.

Parameters:
 None

Returns:
 1 if successful else 0.
=cut
sub empty_reports_table {
	my $self 	= shift;
	
	my $truncate_stmt  = qq{ TRUNCATE qcmg.metric RESTART IDENTITY };
	$self->{geneus_cursor} = $self->{geneus_conn}->prepare($truncate_stmt);
	if ( $self->{geneus_cursor}->execute() ) {
		return 1;
	}
	return 0;
}



=pod
B<empty_mergedbam_table()>

Truncates the table qcmg.mergedbam and resets the primary ID to 0.

Parameters:
 None

Returns:
 1 if successful else 0.
=cut
sub empty_mergedbam_table {
	my $self 	= shift;
	
	my $truncate_stmt  = qq{ TRUNCATE qcmg.mergedbam RESTART IDENTITY CASCADE };
	$self->{geneus_cursor} = $self->{geneus_conn}->prepare($truncate_stmt);
	if ( $self->{geneus_cursor}->execute() ) {
		return 1;
	}
	return 0;
}



=pod
B<add_mergedbam()>

Fetches metadata for requested metadata.

Parameters:
 None

Returns:
   ID of inserted mergedbam or 1 if successful else 0.
=cut
sub add_mergedbam {
	my $self 		      = shift;
  my $merged_record	= shift;
  
  my $key_value_stmt = "";
  
  if ($merged_record->{project}) {
    $key_value_stmt = qq{	
    			SELECT project.limsid FROM qcmg.project
    			WHERE replace( replace( project.name, '-', '_'), '.', '_' ) = ? 
    		};
  }
  
	if ( $key_value_stmt ) {		
		# Build insert query 
		my $insert_stmt = qq{ 
      INSERT INTO  qcmg.mergedbam
      (project_limsid, name, material, sample_code, sample, library_protocol, capture_kit,  aligner, sequencing_platform, created) 
			VALUES ( ($key_value_stmt), ?, ?, ?, ?, ?, ?, ?, ?, ?)	
		};
  
  	# Set values to insert.
  	my @insert_values = (
      $merged_record->{project},   
      $merged_record->{resource_value},
  		$merged_record->{material}, 
  		$merged_record->{sample_code},      
      $merged_record->{sample},             
      $merged_record->{library_protocol}, 
      $merged_record->{capture_kit}, 
      $merged_record->{aligner}, 
      $merged_record->{sequencing_platform},
      $merged_record->{created}
  	);
    
		$self->{geneus_cursor} = $self->{geneus_conn}->prepare_cached($insert_stmt);
		if ( $self->{geneus_cursor}->execute(@insert_values) ) {
			my $last_insert_id = $self->{geneus_conn}->last_insert_id('systemdbgeneus', 'qcmg', 'mergedbam', 'mergedbamid');
			if ( $last_insert_id ) {
				# Return the value of inserted report
				return $last_insert_id;
			}
			return 1;
		}
	}
	return 0; 
}



=pod
B<add_mapset_mergedbam()>

Add a relation of mapset to mergedbam.

Parameters:
 None

Returns:
  Boolean: 1 if successful else 0.
=cut
sub add_mapset_mergedbam {
	my $self 		        = shift;
  my $mergedbamid     = shift;
  my $mapset          = shift;
  my $mapset_id_stmt  = "";
  
  
  if ($mapset) {
    $mapset_id_stmt = qq{	
        SELECT mapset.limsid FROM qcmg.mapset
        WHERE mapset.name = ? 
    };
  }
  
	if ( $mapset_id_stmt ) {		
		# Build insert query 
		my $insert_stmt = qq{ 
      INSERT INTO  qcmg.mapset_mergedbam
      (mapset_limsid, mergedbamid) 
			VALUES ( ($mapset_id_stmt), ? )	
		};
  
  	# Set values to insert.
  	my @insert_values = (
      $mapset,   
      $mergedbamid
  	);
    
		$self->{geneus_cursor} = $self->{geneus_conn}->prepare_cached($insert_stmt);
		if ( $self->{geneus_cursor}->execute(@insert_values) ) {
			my $last_insert_id = $self->{geneus_conn}->last_insert_id('systemdbgeneus', 'qcmg', 'mapset_mergedbam', 'mapset_limsid');
			if ( $last_insert_id ) {
			  print "$mapset -> $last_insert_id\n";
      #	# Return the value of inserted report
			#	return $last_insert_id;
      }
			return 1;
		}
	}
	return 0; 
}

sub prepare_reports {
  # body...
}


=pod
B<add_report()>

Fetches metadata for requested metadata.

Parameters:
 report_record	

Returns:
 ID of inserted report or 1 if successful else 0.
=cut
sub add_report {
	my $self 			= shift;
	my $report_record	= shift;
	
	my $key_type = $report_record->{key_type};
	my $key_value_stmt = "";  
	
	# Depending on the report key_type
	  
	# Parent Project reports
	if ( $key_type eq 'parent_project') {
		$key_value_stmt = qq{	
			SELECT distinct 0
			FROM qcmg.project
			WHERE replace( replace( project.name, '-', '_'), '.', '_' )  =  ?
		};
	}
  # Project reports	
  elsif ( $key_type eq 'project') {
		$key_value_stmt = qq{	
			SELECT project.limsid FROM qcmg.project
			WHERE replace( replace( project.name, '-', '_'), '.', '_' ) = ? 
		};
	}
  # Sample reports	
  elsif ( $key_type eq 'sample') {
		# Nothing to do yet
		# print "[ Warning ] - Not yet expecting key type 'sample' \n";
		$key_value_stmt = qq{
			SELECT sample.limsid FROM qcmg.sample
			WHERE replace( sample.name, '-', '_') || '_' || sample.snp_array_barcode || '_' || sample.snp_array_location = ? 
		};
	}
  # Mapset reports	
  elsif ( $key_type eq 'mapset') {
		$key_value_stmt = qq{
			SELECT mapset.limsid FROM qcmg.mapset
			WHERE mapset.name = ? 
		};
	}
  # Merged Bam (final) reports
  elsif ( $key_type eq 'mergedbam') {
		$key_value_stmt = qq{
			SELECT mergedbam.mergedbamid::text FROM qcmg.mergedbam
			WHERE mergedbam.name = ? 
		};
	}
  # Array reports	
  elsif ( $key_type eq 'arrayscan') {
		$key_value_stmt = qq{
			SELECT arrayscan.limsid
			FROM qcmg.arrayscan
			INNER JOIN qcmg.sample ON sample.sampleid = arrayscan.sampleid
			WHERE replace( sample.name, '-', '_') || '_' || arrayscan.name = ? 
		};
	}
  ## Variant
  #elsif ( $key_type eq 'variant' ){	
	#  
  #
	#}
  # Unknown report	
  else{
		print "[ Warning ] - Unrecognized key type: $key_type\n";
		return ''; 
	}
	  
	if ( $key_value_stmt ) {		
		# Build insert query 
		my $insert_stmt = qq{ 
			INSERT INTO  qcmg.metric (
				key_type, key_value, 
				resource_type, resource_value, 
				report_type, report_value, report_path,
        created, report_version
        ) 
			VALUES (?, ($key_value_stmt), ?, ?, ?, ?, ?, ?, ?)	
		};
		
		# Set values to insert.
		my @insert_values = (
			$report_record->{key_type}, 
			$report_record->{key_value}, 
			$report_record->{resource_type}, 
			$report_record->{resource_value}, 
			$report_record->{report_type}, 
			$report_record->{report_value}, 
			$report_record->{report_path},
      $report_record->{created},
      $report_record->{report_version}
		);
		
    #qlogprint( {l=>'RECORD DATA'}, join (', ', @insert_values)."\n" );
    
		$self->{geneus_cursor} = $self->{geneus_conn}->prepare_cached($insert_stmt);
		if ( $self->{geneus_cursor}->execute(@insert_values) ) {
			my $last_insert_id = $self->{geneus_conn}->last_insert_id('systemdbgeneus', 'qcmg', 'metric', 'metricid');
			if ( $last_insert_id ) {
				# Return the value of inserted report
				return $last_insert_id;
			}
      #print Dumper @insert_values;
			return 1;
    }else{
      print "OPPS execute failed for values", Dumper @insert_values;
    }
	}
	return 0;
}





#INSERT INTO  metric (key_type, key_value, resource_type, resource_value, report_type, report_value, report_path) 
#VALUES ('mapset', 
#	(SELECT mapset.mapsetid 
#	FROM 	qcmg.mapset
#	WHERE mapset.name = 'S0413_20100512_1_LMP.nopd.nobc'),
#'bam', 
#'',
#'qcoverage_report',
#'',
#'/mnt/seq_results/icgc_pancreatic/APGI_1992/seq_mapped/S0413_20100512_1_LMP.nopd.nobc.bam.qcov.xml.html'
#)


##=> key type: parent_project	key value: smgres_endometrial	resource type: timelord	report type: timelord	report value: 	report path: /mnt/seq_results/smgres_endometrial/smgres_endometrial.timelord.html
#SELECT 	project.parent_project
#FROM 	qcmg.project
#WHERE 	project.parent_project = 'smgres_endometrial'
#
#	#=> key type: project	key value: APGI_1992	resource type: qsig	report type: qsignature_compare_report	report value: 	report path: /mnt/seq_results/icgc_pancreatic/APGI_1992/APGI_1992.qsig.comp.html
#SELECT 	project.projectid
#FROM	qcmg.project
#WHERE replace( replace( project.name, '-', '_'), '.', '_' ) = 'APGI_1992'
#
##=> key type: final	key value: APGI_1992.genome.TD	resource type: bam	report type: qcoverage_report	report value: 	report path: /mnt/seq_results/icgc_pancreatic/APGI_1992/seq_final/APGI_1992.genome.TD.bam.qcov.xml.html
#SELECT	mergedbam.mergedbamid 
#FROM 	qcmg.mergedbam
#WHERE mergedbam.name = 'APGI_1992.genome.TD'
#
##=> key type: mapset	key value: S0413_20100512_1_LMP.nopd.nobc	resource type: bam	report type: qcoverage_report	report value: 	report path: /mnt/seq_results/icgc_pancreatic/APGI_1992/seq_mapped/S0413_20100512_1_LMP.nopd.nobc.bam.qcov.xml.html
#SELECT	mapset.mapsetid 
#FROM 	qcmg.mapset
#WHERE mapset.name = 'S0413_20100512_1_LMP.nopd.nobc'
#
#
##=> key type: array	key value: ICGC_ABMP_20091203_06_TD_4802094023_R02C01	resource type: circos	report type: circos_plot	report value: 	report path: /mnt/seq_results/icgc_pancreatic/APGI_1992/SNP_array/ICGC_ABMP_20091203_06_TD_4802094023_R02C01.circos.thumbnail.gif
#SELECT	sample.sampleid, sample.name 
#FROM 	qcmg.sample
#WHERE replace( sample.name, '-', '_') || '_' || sample.snp_array_barcode || '_' || sample.snp_array_location = 'ICGC_ABMP_20091203_06_TD_4802094023_R02C01'













################################################################################

=cut _metadata_query_stmts()

Sets class varibale "metadata_query_stmts" with template SQL statment for metadata
of a set of Sample ID's and related mapsets.

Parameters:
 None
 
Returns:
 None

=cut

#sub _metadata_query_stmts {
#	my $self 	= shift;
#	
#	$self->{metadata_query_stmts} = qq{
#		SELECT  
#			project.parent_project 									AS "Project", 
#	        replace( replace( project.name, '-', '_'), '.', '_' ) 	AS "Donor",			
#	        project.limsid 											AS "Donor LIMS ID",
#			project.study_group										AS "Study Group",
#	        sample.name 											AS "Sample Name",
#	        sample.limsid 											AS "Sample LIMS ID",
#	        sample.sample_code 										AS "Sample Code",
#	        sample.material 										AS "Material",
#			sample.researcher_annotation							AS "Researcher Annotation",
#	        mapset.name  											AS "Mapset",
#	        mapset.limsid  											AS "Mapset LIMS ID",
#	        mapset.primary_library 									AS "Primary Library", 
#	        mapset.library_type 									AS "Library Type",
#		    mapset.library_protocol									AS "Library Protocol", 
#	        mapset.capture_kit 										AS "Capture Kit",
#			coalesce(gfflib.gff_file, gffcapture.gff_file) 			AS "GFF File",
#	        mapset.sequencing_type 									AS "Sequencing Type",
#		    mapset.sequencing_platform								AS "Sequencing Platform", 
#	        mapset.aligner 											AS "Aligner",
#			sample.alignment_required  								AS "Alignment Required",
#	        sample.species_reference_genome							AS "Species Reference Genome",
#			genome.genome_file 										AS "Reference Genome File",
#	        mapset.failed_qc 										AS "Failed QC",
#			mapset.isize_min 										AS "isize Min", 
#			mapset.isize_max 										AS "isize Max",
#			mapset.isize_manually_reviewed 							AS "isize Manually Reviewed",
#		    sample.exp_array_barcode || sample.exp_array_location 	AS "Expression Array", 
#		    sample.snp_array_barcode || sample.snp_array_location 	AS "SNP Array",
#		    sample.mth_array_barcode || sample.mth_array_location 	AS "Methylation Array"
#		FROM qcmg.project
#		INNER JOIN qcmg.sample on sample.projectid = project.projectid
#		INNER JOIN qcmg.genome ON genome.species_reference_genome = sample.species_reference_genome
#		INNER JOIN qcmg.mapset on mapset.sampleid = sample.sampleid 
#		LEFT JOIN qcmg.gff gfflib ON mapset.library_type = gfflib.library_type 
#		LEFT JOIN qcmg.gff gffcapture ON mapset.capture_kit = gffcapture.capture_kit
#	};
#}


################################################################################

=cut _metadata_query_stmts()

Executes SQL statment for for metadata for a set of Sample ID's and related mapsets.

Parameters:
 resources: array string	Array of sample IDs.
 
Returns:
 execution status: scalar interger.
 
Returns the execution status of the SQL statment 1 if succefull. Otherwise 0.

=cut

#sub _metadata_of_resources{
#    my $self 		= shift; 
#    my $resources   = shift @_;
#    my $query_stmt  = $self->{metadata_query_stmts};
#	
#	if ( $resources ) {
#	    # Not happy that I'm unable bind these.
#	    foreach my $sample ( @$resources ){
#	        $sample = "'".$sample."'";
#	    }
#		# create a string of resources for sql statment.
#	    my $samples = join(', ', @$resources);
#		
#		$query_stmt = $query_stmt." WHERE sample.name in ( $samples ) ";	
#	}
#	
#	# Prepare SQL statment for execution.
#    $self->{geneus_cursor} = $self->{geneus_conn}->prepare($query_stmt);
#    # Returns execution status.
#	return $self->{geneus_cursor}->execute();
#}


# End of PRIVATE METHODS





1;

__END__
