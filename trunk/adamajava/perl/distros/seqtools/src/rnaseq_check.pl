#!/usr/bin/perl -w

##############################################################################
#
#  Program:  rnaseq_check.pl
#  Author:   Matthew J Anderson
#  Created:  2014-03-13
#
#  $Id: rnaseq_check.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;       # Good practice
use warnings;     # Good practice

use Carp qw( carp croak verbose );
use Data::Dumper;   # Perl core module
use Getopt::Long;   
use IO::File;       # Perl core module
use File::Basename; # Perl core module
use File::stat;     # Perl core module
use Pod::Usage;
use POSIX 'strftime';		# for printing timestamp     

# QCMG modules
use QCMG::Util::QLog;   # QCMG logging module
use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );

use vars qw( $SVNID $REVISION $VERSION $CMDLINE $VERBOSE $ARCHIVED_STATUS);

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: rnaseq_check.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


##############################################################################
# # #
# #	    MAIN
# 
##############################################################################

MAIN: {
  # Print usage message if no arguments supplied
  #pod2usage(1) unless (scalar @ARGV > 0);
  
  my %params = ( logfile        => '',
                 archivefile    => "/var/www/html/RNA_seQC/RNA_seQC.archive.csv",
                 html           => 0,
                 RNA_seQC_dir   => "/var/www/html/RNA_seQC",
                 project_dir    => undef,
                 
              );
  
  my $thresholds = [      
    {column => 'Intragenic Rate', condition=>'<',       value=>'0.65',       format=>'float1' },     # SOME FAIL
    {column => 'rRNA rate',       condition=>'>',       value=>'1',          format=>'float1' },        # ALL PASS
    {column => 'End 2 % Sense',   condition=>'<',       value=>'90',         format=>'Percent' },       # A FEW FAIL
    {column => 'Mapped',          condition=>'total <', value=>'90000000',   format=>'whole' }       # MOST FAIL (6 Pass)
    ];
                
  
  $VERBOSE = 0;
  $VERSION = 0;
  $CMDLINE = join(' ',@ARGV);
  
	my $results = GetOptions (
           'l|logfile=s'          => \$params{logfile},       # -l
           'q|rnaseqcdir=s'       => \$params{RNA_seQC_dir},  # -q
           'p|projectdir=s'       => \$params{project_dir},   # -p
           'a|archivefile=s'      => \$params{archivefile},   # -a
           'html+'                => \$params{html},          # -html
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
  if ( ! $params{html} ) {
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
  }
  
  # Obtaine list of previously checked files.  
  open(ARCHIVE, "+<", $params{archivefile})      # open for update
          or die "Can't open '".$params{archivefile}."' for update: $!";
  
  # Parse Archive file.
  $ARCHIVED_STATUS = {};
  my $line = <ARCHIVE>;
  chomp $line;
  my @headings = split ',', $line; # Not Used yet
  
  while ($line = <ARCHIVE>) {
    chomp $line;    
    my @details = split ',', $line;
    $ARCHIVED_STATUS->{$details[3]} = \@details;
  }
  
  # look for metrics.tsv files
  my $metrics_tsvs = undef;
  if (defined $params{project_dir} ) {
    push (@{$metrics_tsvs}, "$params{project_dir}/metrics.tsv") if -e "$params{project_dir}/metrics.tsv";
  }
  else{
    $metrics_tsvs = find_metrics( $params{RNA_seQC_dir} );
  }
  
  # Proccess metrics.tsv files
  my $count = 0;
  my $failed = 0;
  foreach my $tsv ( @{$metrics_tsvs} ){
    my $details = check_thresholds($tsv, $thresholds, ['Sample']);
    
    if ( $params{html} ) { 
      outHTML($thresholds, $tsv, $details);
    }  
    elsif ( scalar %{$details->{failed_on}} ) {
      print "$tsv\t[FAILED ON]: ", join (', ', keys %{$details->{failed_on}}),"\n";
      $failed++;
    }
    $count++;
  }
  
  
  if ( ! $params{html} ) {
    print "$failed Failed out of $count\n";  
    qlogend();  
  }
}

sub outHTML {
  my $thresholds = shift;
  my $tsv = shift;
  my $details = shift;
  
  
  $tsv =~ /(\w+)\/(\w+)\/metrics.tsv/g;
  my $row = {
    Slide => $1,
    Project => $2,
    Mapsets => scalar @{$details->{mapset}},
    Status => {}
  };
  foreach my $threshold ( @{$thresholds} ){
    my $value = 0;
    if (exists $details->{failed_on}{$threshold->{column}} ) {
      $value = $details->{failed_on}{$threshold->{column}};
      $value = sprintf('%.2f%s', $value, "%") if $threshold->{format} eq 'Percent';
      $value = sprintf('%.3f', $value) if $threshold->{format} eq 'float1';
      $value =~ s/(\d)(?=(\d{3})+(\D|$))/$1\,/g if $threshold->{format} eq 'whole';
    }
    $row->{Status}{$threshold->{column}} = $value;
  }
  
  #Slide 	Project 	Status 
  my $td_status = "";
  foreach my $column ( keys %{$row->{Status}} ){
    my $value = ! $row->{Status}{$column} ? "" : $row->{Status}{$column};
    $td_status = sprintf '%s<td class="number">%s</td>', $td_status, $value;
  } 
  
  my $tr_class = scalar %{$details->{failed_on}} ? 'danger' : '';
  my $row_listing = sprintf '<tr class="%s"> <td>%s</td> <td><a target="_blank" href="%s/%s">%s</a></td> %s <td class="count">%s</td> </tr>', 
      $tr_class, $row->{Slide}, $row->{Slide}, $row->{Project}, $row->{Project}, $td_status, $row->{Mapsets};
  print "\t\t$row_listing\n\n";

}



sub check_thresholds {
  my $tsv_file      = shift;
  my $thresholds    = shift;
  my $return_values = shift;
  
  
  $tsv_file =~ /(\w+)\/(\w+)\/metrics.tsv/g;
  my $slide = $1;
  my $project = $2;
  my $sb = stat($tsv_file);
  my $file_time_stamp = lc strftime("%Y-%m-%d %H:%M:%S", localtime($sb->mtime));
  
  my $metrics = read_tsv( $tsv_file );
    
  my $column    = $metrics->{columns};
  my $row       = $metrics->{rows};
  my $row_count = scalar @{$row};
    
  my $suspect_rows = {};
  my $failed_on = {};
  my $total = {};
  my $mapsets = [];
  my $t = 0;  
  
  for (my $i = $row_count-1; $i >= 0; $i--) {
    $mapsets->[$i] = $row->[$i][$column->{"Sample"}];
    my $failed_conditions = ();
    my $threshold_metrics = ();
    
    foreach my $threshold (@{$thresholds}) {
      my $c = $column->{$threshold->{column}};
      my $value = $row->[$i][$c];
      my $threshold_failed = 0;
      
      if ($threshold->{format} eq 'whole' ) {
        push @{$threshold_metrics}, sprintf ('"%s":%d', $threshold->{column}, $value);
      }else{
        push @{$threshold_metrics}, sprintf ('"%s":%f', $threshold->{column}, $value);
      }  
      
      if ( $threshold->{condition} eq '<' ) {
        if ( $value <= $threshold->{value} ) {
          $suspect_rows->{$i} = 1;
          if ( exists $failed_on->{$threshold->{column}} ) {
             $value = $failed_on->{$threshold->{column}} if $value > $failed_on->{$threshold->{column}};
          }
          $threshold_failed += 1;
        }
      }
      elsif ( $threshold->{condition} eq '>' ){
        if ( $value >= $threshold->{value} ) {
          $suspect_rows->{$i} = 1;
          if ( exists $failed_on->{$threshold->{column}} ) {
             $value = $failed_on->{$threshold->{column}} if $value < $failed_on->{$threshold->{column}};
          }
          $threshold_failed += 1;
        }
      }
      elsif ( $threshold->{condition} eq 'total <' ){
        $t += $value;
        if (! exists $total->{$threshold->{column}} ) {
          $total->{$threshold->{column}} = 0;
        }
        $total->{$threshold->{column}} = $total->{$threshold->{column}}+$value;
      }
      
      if ( $threshold_failed ) {
        $failed_on->{$threshold->{column}} = $value;
        my $message = sprintf '"%s" %s %s', $threshold->{column}, $threshold->{condition}, $threshold->{value};
        push @{$failed_conditions}, $message;
      }
      
    }
    
    # Archive status
    $failed_conditions = $failed_conditions ? join ";", @{$failed_conditions} : "";
    $threshold_metrics = join (";", @{$threshold_metrics}) ;
    if (! exists $ARCHIVED_STATUS->{$mapsets->[$i]}) {
      print ARCHIVE join (",", $file_time_stamp, $slide, $project, $mapsets->[$i], $failed_conditions, $threshold_metrics), "\n";
    }
  
  }
  
  foreach my $condition ( keys %{$total}){
    foreach my $threshold ( @{$thresholds}) {
      if ($threshold->{column} eq $condition) {
        if ( $threshold->{condition} eq 'total <' ){
          if ( $total->{$condition} < $threshold->{value} ) {
             $failed_on->{$threshold->{column}} = $total->{$condition};
             #print $total->{$condition}, " ", $threshold->{value}, "\n"; 
             for (my $i = $row_count-1; $i >= 0; $i--) {
               $suspect_rows->{$i} = 1;
             }
             
          }
        }
    }
      
    }
  }

  return { mapset=>$mapsets, suspect_rows=>$suspect_rows, failed_on=>$failed_on };
}


# Looks for metrics.tsv files. 
sub find_metrics {
  my $dir = shift;  
  my $metrics = ();
  
  # Check $dir for slide directories (Which are symbolic links)
  opendir(my $slide_dh, $dir) || die;
  while (my $slide = readdir($slide_dh)) {
    next unless (-l "$dir/$slide");
    next unless (-d "$dir/$slide"); # Some links point to directories that no longer exist!

    opendir(my $slide_project_dh, "$dir/$slide") || die;
    while (my $project = readdir($slide_project_dh)) {
      next if ($project =~ m/^\./); # ignore dot files
      next unless (-d "$dir/$slide/$project");
      
      # Find the metrics.tsv file. We'll parse this later.
      my $metrics_tsv = "$dir/$slide/$project/metrics.tsv";
      push (@{$metrics}, $metrics_tsv) if ( -f $metrics_tsv);
    }
    closedir($slide_project_dh);
  }
  closedir($slide_dh);
  
  return $metrics;
}

# Parse the tab seperated file into an array of arrays.
# And create an index to each column by name.
sub read_tsv{
  my $tsv_file = shift;
  my $column = {};
  my $row = ();
    
  open (TSV, $tsv_file) || die "Couldn't open $tsv_file: $!";
    #Create an index to columns by name.
    my @headers = split ("\t", <TSV>);
    chomp(@headers);
    for (my $i = (scalar @headers) -1; $i >= 0; $i--) {
      $column->{$headers[$i]} = $i;
    }
    
    #Grab the rows/lins of the file.
    while (my $line = <TSV>) {
      chomp($line);
      my @headers = split ("\t", $line);
      push (@{$row}, \@headers);
      last if ($line eq '');
    }
  close TSV;
  
  return  {columns => $column, rows => $row};
}

__END__
