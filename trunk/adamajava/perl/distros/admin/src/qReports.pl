#!/usr/bin/perl -w

##############################################################################
#
#  Script:   qReports.pl
#  Creator:  Matthew J Anderson
#  Created:  2013-02-26
#
# This script populates the tables in the QCMG schema. Which is in turn used 
# by qReports (The web interface) and other tools. These tables are:
#     qcmg.metric              (reports)
#     qcmg.mergedbam           (finals)
#     qcmg.mapset_mergedbam    (mapsets in mergedbam)
#
#  $Id: qReports.pl 4695 2014-08-29 04:35:25Z m.anderson $
#
##############################################################################

use strict;                     	    # Good practice
use warnings;                   	    # Good practice
use Data::Dumper;               	    # Perl core module
use File::Basename;                   # Perl core module
use File::stat;                       # Perl core module
use Getopt::Long;                     # 
use Pod::Usage;                       # 
                                      
use QCMG::QBamMaker::SeqFinalBam;     # QCMG merged bam module
use QCMG::DB::Metadata;					      # QCMG database module
use QCMG::DB::QcmgWriter;					    # QCMG database module
use QCMG::SeqResults::Config;			    # QCMG seq_results module
use QCMG::SeqResults::ReportMetrices	qw( coverage_report_metrics ); 	# QCMG seq results module
use QCMG::Util::QLog;                 # QCMG loging module


use vars qw( $SVNID $REVISION $VERSION $CMDLINE $VERBOSE $CONFIG $QCMGDB );

( $REVISION ) = '$Revision: 4695 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qReports.pl 4695 2014-08-29 04:35:25Z m.anderson $'
    =~ /\$Id:\s+(.*)\s+/;

##############################################################################
# # #
# #	    MAIN
# 
##############################################################################

MAIN: {
  # Print usage message if no arguments supplied
  pod2usage(1) unless (scalar @ARGV > 0);
  
  my %params = ( config   => '',
                 logfile  => ''
              );
  $VERBOSE = 0;
  $VERSION = 0;
  $CMDLINE = join(' ',@ARGV);
  
	my $results = GetOptions (
           'c|config=s'           => \$params{config},        # -c
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$VERBOSE,               # -v
             'version!'           => \$VERSION,               # --version
	);
  if ($VERSION) {
      print "$SVNID\n";
      exit;
  }
  
  pod2usage(1) if $params{help};
  pod2usage(-exitstatus => 0, -verbose => 2) if $params{man};
  
  
  # It is mandatory to supply an infile
  die "You must specify a JSON config file\n" unless $params{config};
  
  # Set up logging
  qlogfile($params{logfile}) if $params{logfile};
  qlogbegin();
  qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
  
	# Colleting config details from JSON file.
	$CONFIG = QCMG::SeqResults::Config->new( $params{config} );
	die "Unable to read config file\n" unless $CONFIG;
  # Contecting to QCMG schema (LIMS)
  $QCMGDB = QCMG::DB::QcmgWriter->new();
	die "Unable to connect to QCMG schema\n" unless $QCMGDB;
  
  # Re-populate the mergedbams and mapset_mergedbam tables;
  repopulate_mergedbams();
  # Re-populate the metric table;
  repopulate_reports();

  qlogprint( {l=>'DONE'}, "");
  qlogend();
}


##############################################################################
=cut file_timestamp()

??

Parameters:
  file_time   ??

Returns:
  None 
=cut
sub file_timestamp {
  my $file_time = shift;

  my ($sec, $min, $hour, $mday, $mon, $year, $wday, $yday, $isdst) = localtime($file_time);
  my $nice_timestamp = sprintf ( "%04d-%02d-%02d %02d:%02d:%02d",
                                   $year+1900, $mon+1, $mday, $hour, $min, $sec);
  
  return $nice_timestamp;
}


##############################################################################
=cut repopulate_mergedbams()

Repopulates the mergedbam and mapset_mergedbam tables with metadata and 
mapsets used by qbammaker. For merged bams found in seq_final of projects 
listed in the config file, the bam headers are parsed. 

Parameters:
  none   

Returns:
  None 
=cut
sub repopulate_mergedbams {
  
  # Find mergedbams
  qlogprint( {l=>'INFO'}, "Finding mergedbams\n") if $VERBOSE;
  my $seq_final_resources = collect_resources( ['seq_final'] );
  
  qlogprint( {l=>'INFO'}, sprintf ("Collected %d mergedbams\n", scalar @{$seq_final_resources}) ) if $VERBOSE;

  # Find Metadata and sub_resources (mapsets of mergedbam)
  qlogprint( {l=>'INFO'}, "Parsing mergedbams for metadata\n") if $VERBOSE;
    
  # Foreach merged bam parse sam header for metadata.
  foreach my $resource ( @{$seq_final_resources} ){        
    if ($resource->{resource_type} eq 'merged_bam' ) {
      # Hack to parse finals
      my $path = dirname($resource->{resource_path})."/".$resource->{resource_value}.".bam";
      my $final = QCMG::QBamMaker::SeqFinalBam->new(  filename => $path, #$resource->{resource_path},
                                                      verbose  => 0 );
      die 'module did not work' unless $final;
      
      my $merged_mapsets = $final->mapsets();
      if ( $merged_mapsets ) {
        $resource->{merged_mapsets} = [keys %{$merged_mapsets}];
        $resource->{merged_record}  = {};
        # The values we want.
        my $merged_metadata = {
          resource_value      => {$resource->{resource_value} =>''},
          project             => {}, #IcgcOvarian
          donor               => {}, # _AOCS001
          material            => {}, # _2RNA
          sample_code         => {}, # _7PrimaryTumour
          sample              => {}, # _ICGCDBLG2010022506TR
          library_protocol    => {}, # _NULL
          capture_kit         => {}, # _NoCapture
          aligner             => {}, # _BwaMapspliceRsem
          sequencing_platform => {}, # _HiSeq
          created             => { $resource->{resource_created} => $resource->{resource_created} }
        };
        # Extract metadata from mapsets.
    	  foreach my $mapset ( keys %{$merged_mapsets} ){
          foreach my $metadata ( keys %{$merged_mapsets->{$mapset}} ){
            my $metadata_value = $merged_mapsets->{$mapset}{$metadata};
            if ( exists $merged_metadata->{$metadata}) {
              # Keep unique metadata values
              if (! exists $merged_metadata->{$metadata}{$metadata_value} ) {
                $merged_metadata->{$metadata}{$metadata_value} = '';
              }
            }
          }
    	  }
      
        # rename metadata to match LIMS
        $merged_metadata->{parent_project} = delete $merged_metadata->{project};
        $merged_metadata->{project} = delete $merged_metadata->{donor};
      
        # Concatenate metadata values.
        foreach my $metadata ( keys %{$merged_metadata} ){
          $resource->{merged_record}{$metadata} = join (',', keys %{$merged_metadata->{$metadata}} );
        }
        
      } #end if $merged_mapsets
    }
  }
    
  # Empty mergedbam table ready for repopulation.
  qlogprint( {l=>'INFO'}, "Truncating tables\n") if $VERBOSE;
  if ( ! $QCMGDB->empty_mergedbam_table() ){
    qlogprint( {l=>'ERROR'}, "Unable to truncate the mergedbam table before population\n");
    die 'Not updating mergedbam table if table not truncated\n';
  }  
  
  # Iterating $seq_final_resources twice to keep table unpopulated for as short as posible.
  
  # Add resources to database
  qlogprint( {l=>'MERGEDBAMS'}, "Adding mergedbams to database\n") if $VERBOSE;
  my $reports_added_failed 	= 0;
  foreach my $resource ( @{$seq_final_resources} ){
    if ( exists $resource->{merged_record} ) {
      my $mergedbamid = $QCMGDB->add_mergedbam($resource->{merged_record});
      if ( $mergedbamid > 1 ) {
        foreach my $mapset ( @{$resource->{merged_mapsets}} ){
          $QCMGDB->add_mapset_mergedbam( $mergedbamid, $mapset );
        }
      }
      else{
        $reports_added_failed += 1;
        qlogprint( {l=>'WARN'}, sprintf("Failed entry for mergedbam %s\n", $resource->{merged_record}{resource_value}) );
      }
    }
  }
  
  # Print stats
  qlogprint ( {l=>'INFO'}, sprintf ("Collected %s mergedbams\n", scalar( @{$seq_final_resources} )) );
  if ( $reports_added_failed ) {
    qlogprint ( {l=>'WARN'}, sprintf ("Failed to add %s mergedbams\n", $reports_added_failed) );
  }
  
}



##############################################################################
=cut repopulate_reports()

Repopulates the metric table with reports and metric values for resources of 
projects listed in the config file. 

Parameters:
  none   

Returns:
  None 
=cut
sub repopulate_reports {
  # Find reports
  qlogprint( {l=>'REPORTS'}, "Finding reports\n") if $VERBOSE;
  my $reports = collect_reports();
    
  qlogprint( {l=>'REPORTS'}, "Truncating tables\n") if $VERBOSE;
  if ( ! $QCMGDB->empty_reports_table() ){
    qlogprint( {l=>'QCMG Schema'}, "Unable to truncate the reports table before population\n");
    die 'Not updating metric table if table not truncated\n';
  }
    
  # Add reports to database
  qlogprint( {l=>'REPORTS'}, "Adding reports to database\n") if $VERBOSE;
  my $reports_added_failed 	= 0;
  foreach my $report_record ( @{$reports} ){
    #print Dumper $report_record;

  	if ( ! $QCMGDB->add_report($report_record) ) {
  		# Adding report failed!
      $reports_added_failed += 1;
  		_print_report_record($report_record);
  	}
  }
  
  # Print stats
  qlogprint ( {l=>'INFO'},   sprintf ("Collected %s reports\n", scalar( @{$reports} )) );
  if ( $reports_added_failed ) {
    qlogprint ( {l=>'WARN'}, sprintf ("Failed to add %s reports\n", $reports_added_failed) );
  }
}



################################################################################
=cut  collect_resources()

Returns a list of resources found in the categories (optional) for all 
parent projects and projects. If a list of categories are given, then only 
these categories will be searched, otherwise all categores are searched.

Parameters:
  categories: array string (optional)    List of categories.
                                        (must be a subset of the config file)  

Returns:
  resources: array    list of resources found.
=cut
sub collect_resources {
  my $categories = shift @_;
  
	my $resources_list            = ();
	my $parent_project_categories = $CONFIG->parent_project_categories();
	my $project_categories        = $CONFIG->project_categories();
	
  # If a list of categories are given. Redefined parent_project and project
  # categories lists.
  if ($categories) {
    my $parent_project_sub_categories = ();
    my $project_sub_categories        = ();
    
    foreach my $category ( @{$categories} ) {
      foreach my $parent_project_category ( @{$parent_project_categories} ) {
        if ( $parent_project_category eq $category ) {
          push (@{$parent_project_sub_categories}, $category);
        }
      }
      foreach my $project_category ( @{$project_categories} ) {
        if ( $project_category eq $category ) {
          push (@{$project_sub_categories}, $category);
        }
      }
    }
    
    $parent_project_categories  = $parent_project_sub_categories;
    $project_categories         = $project_sub_categories;
  }
  
  
  # For each parent project directory
	foreach my $parent_project ( sort @{$CONFIG->parent_projects()} ){
    #next unless $parent_project eq "icgc_ovarian";
    
    qlogprint ( {l=>'INFO'}, "Parent Project: $parent_project\n" ) if $VERBOSE; 
		my $parent_project_dir = $CONFIG->parent_project_dir($parent_project);
		
		# Find Parent Project resources
		foreach my $category ( @{$parent_project_categories} ){
			my $root_path           = $parent_project_dir;
			my $category_resources  = find_category_resources($category, $root_path);
			foreach my $resource_record ( @{$category_resources} ){
				push ( @{$resources_list}, $resource_record);
			}
		}
		
		# Find Project (donor) resources
		my $projects = projects_of_parent_project($parent_project);
		foreach	my $project ( @{$projects} ) {
			foreach my $category ( @{$project_categories} ){
				my $root_path           = "$parent_project_dir/$project";
				my $category_resources  = find_category_resources($category, $root_path);
				foreach my $resource_record ( @{$category_resources} ){
					push ( @{$resources_list}, $resource_record);
				}
			}
		}
	}
	
	return $resources_list;
}



################################################################################
=cut  collect_reports()

Returns a list of reports found as defined it the config file.

Parameters:
  None

Returns:
  reports: array    list of reports found.
=cut
sub collect_reports {
	my $reports_list = ();
	
	my $parent_project_categories = $CONFIG->parent_project_categories();
	my $project_categories 			  = $CONFIG->project_categories();
  
	foreach my $parent_project ( sort @{$CONFIG->parent_projects()} ){
    #next unless $parent_project eq "icgc_ovarian";
        
    qlogprint ( {l=>'INFO'}, "Parent Project: $parent_project\n" ) if $VERBOSE; 
		my $parent_project_dir = $CONFIG->parent_project_dir($parent_project);
		
		# Find Parent Project reports
		foreach my $category ( @{$parent_project_categories} ){
			my $root_path                 = $parent_project_dir;
			my $category_resource_reports = find_category_resource_reports($category, $root_path);
			foreach my $report_record ( @{$category_resource_reports} ){
				push ( @{$reports_list}, $report_record);
			}
		}
		
		# Find Project (donor) reports
		my $projects = projects_of_parent_project($parent_project);
		foreach	my $project ( @{$projects} ) {
			foreach my $category ( @{$project_categories} ){
				my $root_path                 = "$parent_project_dir/$project";
				my $category_resource_reports = find_category_resource_reports($category, $root_path,);
        foreach my $report_record ( @{$category_resource_reports} ){
  				if ($category eq "variants_circos") {
            # extract project from file name, which is needed for the key.
            if ($report_record->{key_value} =~ /^([A-Z]+_\d+)_/ ) {
              $report_record->{key_value} = $1;
  				  }             
  				}
					push ( @{$reports_list}, $report_record);
				}
			}
		}
	}
	
  
	return $reports_list;
}



################################################################################
=cut  projects_of_parent_project()

Returns a list of project (donor) directories for a given parent project. 
Project directories listed as exempt in the config file are excluded. 

Parameters:
  parent_project: scalar string 		Parent project name.

Returns:
  projects: array   Array of project directory names.
=cut 
sub projects_of_parent_project {
	my $parent_project      = shift;
	my $parent_project_dir  = $CONFIG->parent_project_dir($parent_project);
	my $projects            = []; 
	
	opendir(DIR, $parent_project_dir);
	foreach my $file ( readdir(DIR) ){
		if ( -d "$parent_project_dir/$file" ) {
			push @$projects, $file
			        unless grep { $_ eq $file } @{$CONFIG->exempt_donorsdirs()};
		}
	}
	closedir(DIR);
	
	return $projects;
}



################################################################################
=cut find_category_resource_reports()

Find reports of resources for a given category.

Parameters:
  category: scalar	string	        Category name.
  root_path: scalar string		      Parent path directory of Category.
  debug: scalar string (optional)  Turn on debuging.

Returns:
  resource_rreports: array   Array of hashes {key_type, key_value, resource_value}
=cut 
sub find_category_resource_reports {
	my $category	= shift;
	my $root_path	= shift;
	my $debug		  = shift;
	
	my $category_reports_list     = ();
	my $category_dir              = "$root_path/".$CONFIG->category_dir($category);
	my $category_resources_types  = $CONFIG->category_resources($category);
	my $key_type                  = $CONFIG->category_to_resource_type($category);
	
  if (! $key_type ) {
    print "key_type '$key_type' unknown for category '$category'\n";
  }
  
	if ( @{$category_resources_types} ){
		foreach my $resource_type ( @{$category_resources_types} ){
			my $resource_filetype = $CONFIG->resource_file($resource_type);

			if ($resource_filetype){
				if ( -d $category_dir) {
          # Find resources in category 
					my $find_cmd = "find $category_dir -maxdepth 1 -name \"*$resource_filetype\"";
					my @resources =  split('\n', `$find_cmd`);
          
					foreach my $resource_path ( @resources ){
						my $resource_name = basename($resource_path, $resource_filetype);
            # Find reports for resource
						my $resource_reports = find_resource_reports($resource_type, $resource_path, $category);
            	
						foreach my $report_record ( @{$resource_reports} ){
							$report_record->{key_type}        = $key_type;
							$report_record->{key_value}       = $resource_name;
							$report_record->{resource_value}  = $resource_name;
							push ( @{$category_reports_list}, $report_record);
						}
					} # each resource
				}
			} # if files types
		} # each resource type
	
	} # if category resources
	return $category_reports_list
}



################################################################################
=cut  find_resource_reports()

Returns reports for a given resource.

Parameters:
  resource_type: scalar string		Type of resource file.
  resource_path: scalar string		Path to resource file.

Returns:
  resource_reports: array    list (of hashes) of reports for resource: 
  {key_type, key_value, resource_value, report_type, report_value, report_path}            

IMPORTANT: The values for "key_type", "key_value", "resource_value" need to be 
           defined by calling function
=cut
sub find_resource_reports {
	my $resource_type	= shift;
	my $resource_path	= shift;
	
	my $resource_reports_list = ();
	my $resource_filetype     = $CONFIG->resource_file($resource_type);
	my $resource_reports      = $CONFIG->resource_reports($resource_type);
	my $resource_name         = basename($resource_path, $resource_filetype);
	my $resource_dir          = dirname($resource_path, $resource_filetype);
	  
	foreach my $report ( @{$resource_reports} ){
		my $report_file           = $CONFIG->report_file($report);
		my $resource_report       = "$resource_name$report_file";
		my $resource_report_path  = "$resource_dir/$resource_report";
    my $file_stat             = stat($resource_report_path);
    
		if ( -e $resource_report_path ) {
			my $report_record = {
				key_type        => '',    # To be defined by caller of function
				key_value       => '',    # To be defined by caller of function
				resource_value  => '',    # To be defined by caller of function
				resource_type   => $resource_type,
				report_type     => $report,
				report_value    => report_value($report, $resource_report_path),
				report_path     => $resource_report_path,
        report_version  => '',
        created         => file_timestamp($file_stat->mtime)
			};
			push (@{$resource_reports_list}, $report_record);	
    }
	}
	
	return $resource_reports_list;
}


################################################################################
=cut  report_value()

Returns the key metric values of the report.
Currently only qcoverage reports have a metric value.

Parameters:
  scalar: report_type		Type of report file.
  scalar: report_path		Path to report file.

Returns:
  report_value: scalar string
=cut
sub report_value {
	my $report_type	= shift;
	my $report_path	= shift;
	my $report_value = "";
	#
	
	if ( $report_type eq "qcoverage_report" or $report_type eq "qcoverage_phys_report") {
		my $coverage_tags   = $CONFIG->coverage_tags();
		$report_value       = coverage_report_metrics($report_path, $coverage_tags);
	#}elsif ( $report_type eq "qsignature_donor_report") {
	#	print "Need to set metadata in html for qsignature_donor_report\n"; 
	}else{
		#print "$report_type \t";
	}
	
	return $report_value;
}



################################################################################
=cut  find_category_resources()

Returns resources for a given category.

Parameters:
  category: scalar string		          Category name.
  root_path: scalar string		        Parent path directory of Category.
  debug: scalar boolean (optional)    Turn on debuging.

Returns:
 category_resources: array    list of hashes of reports for resource. 
                      {resource_value, report_type, report_value, report_path}            
=cut
sub find_category_resources {
	my $category	    = shift;
	my $root_path	    = shift;
	my $resource_type	= shift;
	my $debug	        = shift;
  
  $root_path                    = '' unless $root_path;
  my $category_resources_list   = ();
	my $category_dir              = "$root_path/".$CONFIG->category_dir($category);
	my $category_resources_types  = $CONFIG->category_resources($category);
  my $key_type                  = $CONFIG->category_to_resource_type($category);
    
	if ( @{$category_resources_types} ){
		foreach my $resource_type ( @{$category_resources_types} ){
      my $resource_filetype = $CONFIG->resource_file($resource_type);
      # debug
      #if ($debug) { 
      #	printf ("\tCategory: %s\t - ", $category);
      #	printf ("\t %s\n", $resource_type);
      #	printf ("\t-- %s\n", $category_dir );
      #  print "\t\t$resource_type - file=$resource_filetype\n"; 
      #};
			if ($resource_filetype){
				if ( -d $category_dir) {
          # find resources
					my $find_cmd = "find $category_dir -maxdepth 1 -type f -name \"*$resource_filetype\" ";
					my @resources =  split('\n', `$find_cmd`);
					foreach my $resource_path ( @resources ){            
            my $resource_name = basename($resource_path, $resource_filetype);
            my $file_stat = stat($resource_path);
            # debug
            # print "\t\t\t$resource_name\n"; if ($debug);
            
      			my $resource_record = {
              resource_category   => $category,
      				resource_type       => $resource_type,
      				resource_value      => $resource_name,
              resource_path       => $resource_path,
              resource_created    => file_timestamp($file_stat->mtime)
      			};
            push ( @{$category_resources_list}, $resource_record);
          }
        }
      } # if resource_filetype
    } # each resource_type
  }
  
  return $category_resources_list
}



##############################################################################
#   Debugging Functions
##############################################################################

# filter_on can be: 
#		'key_type'
#		'key_value'
#		'resource_type'
#		'report_type'
#		'report_value'
#		'report_path'

# Filters records 
sub __filter_unique_records_for {
	my $records		= shift;
	my $filter_on	= shift;
	my $seen      = {};
  
	print "Looking for unique records of $filter_on\n";
	foreach my $report_record ( @{$records} ){
		my $value = $report_record->{$filter_on};
		if ( ! exists $seen->{$value} ) {
			$seen->{$value} = 0;
			print_report_record($report_record);
		}
		$seen->{$value} += 1;
	}	
	print Dumper $seen;
}

# Filters records 
sub __filter_records_for {
	my $records			      = shift;
	my $filter_key		    = shift;
	my $filter_value	    = shift;
	my $filtered_records  = ();
  
	print "Looking for records of $filter_key with the value $filter_value\n";
	foreach my $report_record ( @{$records} ){
		my $value = $report_record->{$filter_key};
		if ( $value eq $filter_value ) {
			push ( @{$filtered_records}, $report_record );
		}
	}	
	return $filtered_records;
}

# Print a report record
sub _print_report_record {
	my $report_record = shift;
	
	printf ( "#=> key type: '%s'	key value: '%s'	resource type: '%s'	resource value: '%s'	report type: '%s'	report value: '%s'	report path: '%s' \n", 
    $report_record->{key_type}, 
    $report_record->{key_value},
    $report_record->{resource_type},
    $report_record->{resource_value}, 
    $report_record->{report_type},
    $report_record->{report_value}, 
    $report_record->{report_path}
	);
}



__END__

=head1 NAME

 qReports.pl - Perl script for updating the qcmg schema with information on
 file found in seq_results.


=head1 SYNOPSIS

 qReports.pl [options]


=head1 ABSTRACT

 To improve qReports efficiency in page load times. qReports now relies on 
 database records of available reports for resources rather then looking 
 on the file system in real time. This could cause awful load times when 
 the system is under load. 

 This script populates a number of tables required by qReports. These 
 include listing merged bams (and metadata), the mapsets in a merged bam,
 reports found for resources (something that has a reports). 
 
 Reports might be circos plots, qProfiler, coverage, qSignature, timelord.

 Details of where to find resources and what reports it might have are 
 defined by the JSON config file. Which are specific to the structure of
 seq_results. 

=head1 OPTIONS

 -c | --config        JSON config file
 -l | --logfile       log file (optional)
 -v | --verbose       print progress and diagnostic messages
      --version       print version number and exit immediately
 -h | -? | --help     print help message
 -m | --man           print full man page


=head1 DESCRIPTION

=head2 Commandline Parameters

=over

=item B<--config>

Required JSON config file. This file describes what resources to look for, 
where to find them, what reports it might have.

=item B<-l | --logfile>

Optional log file name.  If this option is not specified then logging
goes to STDOUT.

=item B<-v | --verbose>

Print progress and diagnostic messages.  This option can be specified
multiple times on the commandline to enable higher levels of verbosity.

=item B<-h | --help>

Display help screen showing available commandline options.

=item B<-m | --man>

Display the full man page (this page).  This is equivalent to doing a
perldoc on the script.

=back


=head1 AUTHOR

=over 2

=item Matthew Anderson, L<mailto:m.anderson5@uq.edu.au>

=back


=head1 VERSION

$Id: qReports.pl 4695 2014-08-29 04:35:25Z m.anderson $


=head1 COPYRIGHT

This software is copyright 2011 by the Queensland Centre for Medical
Genomics. All rights reserved.  This License is limited to, and you
may use the Software solely for, your own internal and non-commercial
use for academic and research purposes. Without limiting the foregoing,
you may not use the Software as part of, or in any way in connection with 
the production, marketing, sale or support of any commercial product or
service or for any governmental purposes.  For commercial or governmental 
use, please contact licensing\@qcmg.org.

In any work or product derived from the use of this Software, proper 
attribution of the authors as the source of the software or data must be 
made.  The following URL should be cited:

  http://bioinformatics.qcmg.org/software/

=cut