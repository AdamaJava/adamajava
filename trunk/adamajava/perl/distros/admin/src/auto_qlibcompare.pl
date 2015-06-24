#!/usr/bin/env perl

##############################################################################
#
#  Script:   auto_qlibcompare.pl
#  Creator:  Matthew J Anderson
#  Created:  2014-03-24
#
#  Automates the submission of required qlibcompare reports.
#
#  Checks if qlibcompare are missing or out of date. If new reports are 
#  required jobs are automatically submitted to the jobs pbs server. Requires
#  quering QCMG Schema to find required projects to run reports against.
#
#  $Id: qReports.pl 4506 2014-02-14 09:36:54Z m.anderson $
#
##############################################################################

use strict;                     	    # Good practice
use warnings;                   	    # Good practice
use Data::Dumper;               	    # Perl core module
use File::Basename;                   # Perl core module
use File::stat;                       # Perl core module
use Getopt::Long;                     # 
use Pod::Usage;                       # 
                                      
#use QCMG::QBamMaker::SeqFinalBam;     # QCMG merged bam module
use QCMG::DB::Metadata;					    # QCMG database module
#use QCMG::SeqResults::Config;			    # QCMG seq_results module
#use QCMG::SeqResults::ReportMetrices	qw( coverage_report_metrics ); 	# QCMG seq results module
use QCMG::Util::QLog;                 # QCMG loging module


use vars qw( $SVNID $REVISION $VERSION $CMDLINE $VERBOSE $METADATA);

( $REVISION ) = '$Revision: 4506 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qReports.pl 4506 2014-02-14 09:36:54Z m.anderson $'
    =~ /\$Id:\s+(.*)\s+/;

##############################################################################
# # #
# #	    MAIN
# 
##############################################################################

MAIN: {
  # Print usage message if no arguments supplied
  #pod2usage(1) unless (scalar @ARGV > 0);
  
  my %params = ( config   => '',
                 logfile  => ''
              );
  $VERBOSE = 0;
  $VERSION = 0;
  $CMDLINE = join(' ',@ARGV);
  
	my $results = GetOptions (
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
    
  # Set up logging
  qlogfile($params{logfile}) if $params{logfile};
  qlogbegin();
  qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
  
  qlogprint( {l=>'INFO'}, "Collecting Projects\n");
  $METADATA = QCMG::DB::Metadata->new();
  
  qlogprint( {l=>'INFO'}, "function_name\n");
  submit_jobs();
  
  print "Done\n";
}


sub submit_jobs {
  my $parent_projects = $METADATA->parent_projects();
  
  # For each parent project directory
	foreach my $parent_project ( sort @{$parent_projects} ){
    qlogprint ( {l=>'INFO'}, "Parent Project: $parent_project\n" ) if $VERBOSE; 
		
		# Find Project (donor)
    my $parent_project_path = $METADATA->parent_project_path($parent_project);
		my $projects = projects_of_parent_project($parent_project);
		foreach	my $project ( @{$projects} ) {
				my $root_path           = "$parent_project_path/$project";
        
        my $newest_qlibcompare_time = 0;
        my $newest_profiler_time = 0;
        my $processing_cost = 0;
        my $build_report = 0;
        
        my $qlibcompare_reports = find_qlibcompare_reports($root_path, $project);
        my $qprofiler_reports = find_qprofiler_reports($root_path, $project);
        
        # Find qlibcompare reports
			  foreach my $report (@{$qlibcompare_reports}){
			    my $sb = stat($report);
          $newest_qlibcompare_time = $sb->mtime if $sb->mtime > $newest_qlibcompare_time ;
			  }
        
        # Find qprofiler reports
			  foreach my $report (@{$qprofiler_reports}){
			    my $sb = stat($report);
          $newest_profiler_time = $sb->mtime if $sb->mtime > $newest_profiler_time;
			  }
        
        #
        # Check to see if we need to build/rebuild the qlibcompare report     
        #
        # EXISTING REPORT
        #   - If a qlibcompare report exists and at least 2 qprofiler reports
        #     - Rebuild if any qprofiler reports are newer than the qlibcompare report
        if ( scalar @{$qlibcompare_reports} and scalar @{$qprofiler_reports} > 1 ){
          if( $newest_qlibcompare_time lt $newest_profiler_time ) {
            $build_report ++;
            print "We need to rebulid the report!\n";
          }
        }
        # NEW REPORT
        #   - Must have at least 2 qprofiler reports for comparison.
        elsif ( scalar @{$qprofiler_reports} > 1 ){
          $build_report ++;
        }
        
        if ($build_report) {
          $processing_cost = scalar @{$qprofiler_reports} * scalar @{$qprofiler_reports};
          print "$project -- processing_cost = $processing_cost\n";
          
          my $infiles = sprintf " --infile %s", join (" --infile ", @{$qprofiler_reports});
          
          #print "\t$infiles\n";
          
          my $cmd = "qsub -v PARENT_PROJECT_PATH=$parent_project_path,PROJECT=$project,QLIBCOMPARE_INFILES='$infiles' \$QCMG_SVN/QCMGProduction/automation/qcmg-clustermk2/autoqlibcompare.pbs";
          #print "$cmd\n";
          my $qsub_job_id = `$cmd`;
          print "Submitted Job $qsub_job_id\n" if $qsub_job_id;
        }
		}
	}
}


################################################################################
=cut  find_qlibcompare_reports()

Returns a list of directory paths of qlibcompare report for a given project path. 

Parameters:
  path: scalar string 		Path to project directory.

Returns:
  projects: array   Array of file paths.
=cut 
sub find_qlibcompare_reports {
  my $path = shift;
  my $reports = ();
  
	opendir(DIR, $path);
	foreach my $file ( readdir(DIR) ){
    push @{$reports}, "$path/$file" if ( $file =~ /$project.qp.libcomp.xml$/ );
  }
	closedir(DIR);
  
  return $reports;
}

################################################################################
=cut  find_qprofiler_reports()

Returns a list of directory paths of qprofiler report for a given project path. 

Parameters:
  path: scalar string 		Path to project directory.
  
Returns:
  report: array   Array of file paths.
=cut 
sub find_qprofiler_reports {
  my $path = shift;
  my $reports = ();
  
	opendir(DIR, "$path/seq_mapped");
	foreach my $file ( readdir(DIR) ){
    push @{$reports}, "$path/seq_mapped/$file" if ( $file =~ /\.bam\.qp\.xml$/ );
    
    #print "$file\n" if ( $file =~ /.qp.xml$/ );
  }
	closedir(DIR);
  
  return $reports;
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
	#my $parent_project_dir  = $CONFIG->parent_project_dir($parent_project);
  my $parent_project_path = $METADATA->parent_project_path($parent_project);
  my $parent_project_project_prefix = $METADATA->parent_project_project_prefix($parent_project);
	my $projects            = []; 
	
  #print $parent_project_path;
	opendir(DIR, $parent_project_path);
	foreach my $file ( readdir(DIR) ){
		if ( -d "$parent_project_path/$file" ) {
			push @$projects, $file if $file =~ /$parent_project_project_prefix/;
			        #unless grep { $_ eq $file } @{$CONFIG->exempt_donorsdirs()};
		}
	}
	closedir(DIR);
	
	return $projects;
}
