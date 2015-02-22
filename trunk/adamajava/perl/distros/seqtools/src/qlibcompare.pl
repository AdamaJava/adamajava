#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qlibcomapre.pl
#  Author:   Matthew J Anderson
#  Created:  2014-02-26
#
#  Read multiple qProfiler XML report files to compare libraries using iSize
#
#  $Id: qlibcompare.pl 4770 2014-12-02 02:29:46Z m.anderson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak verbose );
use Data::Dumper;
use File::Basename;   # Perl core module
use Getopt::Long;
use IO::File;
use XML::LibXML;
use XML::Writer;		  # CPAN
use Pod::Usage;

# QCMG Modules
use QCMG::DB::Metadata;   # QCMG LIMS module
use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION $CMDLINE );

( $REVISION ) = '$Revision: 4770 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qlibcompare.pl 4770 2014-12-02 02:29:46Z m.anderson $'
    =~ /\$Id:\s+(.*)\s+/;
    
MAIN: {

    # Print usage message if no arguments supplied
    pod2usage( -exitval  => 0,
               -verbose  => 99,
               -sections => 'SYNOPSIS|COMMANDS' )
        unless (scalar @ARGV > 0);

    $CMDLINE = join(' ',@ARGV);

    my %params = ( infiles  => [],
                   outfile  => '',
                   start    => 0,
                   end      => 5000,
                   binsize  => 1,
                   logfile  => '',
                   verbose  => 0 );

    my $results = GetOptions (
           'i|infile=s@'          => \$params{infiles},        # -i
           'o|outfile=s'          => \$params{outfile},       # -o
           's|start=i'            => \$params{start},         # -s
           'e|end=i'              => \$params{end},           # -e
           'b|binsize=i'          => \$params{binsize},       # -b
           'l|logfile=s'          => \$params{logfile},       # -l
           'v|verbose+'           => \$params{verbose},       # -v
           );
    
    
    # Set up logging
    qlogfile($params{logfile}) if $params{logfile};
    qlogbegin();
    qlogprint( {l=>'EXEC'}, "CommandLine $CMDLINE\n");
    
    
    # Validate files
    qlogprint( "Validating files\n");
    my $xml_files = validate_files($params{infiles}, "qProfiler");
    
    # Find read groups in file headers
    qlogprint( "Collecting Read Groups\n" );
    my $read_groups = collect_header_readgroups($xml_files);
    
    print Dumper scalar @{$params{infiles}}, $read_groups;
    die "You must specify at least 2 input files or an imput file with at least 2 read groups\n" 
      unless scalar @{$params{infiles}} >= 2 or scalar @{$read_groups} >= 2;
    
    #print Dumper $read_groups;
    
    # Collect Metadata from LIMS where avalibale
    qlogprint( "Obtaining Metadata\n");
    collect_metadata($xml_files);
    
    #print Dumper $xml_files;
    #my $file_count = scalar keys %{$xml_files};
    
    
    # Build a Refernce of all positions
    my $posistitions = {};
    for (my $i = $params{start}; $i < $params{end}; $i += $params{binsize}) {
      $posistitions->{$i} = [];
    }    
    
    # Collect counts at postions to build a master collection of counts to compare
    qlogprint( "Collecting posistion counts\n");
    collect_positions ($posistitions, $xml_files, $read_groups, $params{start}, $params{end}, $params{binsize} );
    
    # Create an index of positions.
    # This is to prevent looping through the positon list as not all positions have count and may not exist
    # in the collected positions.
    my $indexed_posistitions = []; #sort {$a <=> $b} keys %{$comp};
    $indexed_posistitions->[$_] = $posistitions->{$_} for keys %{$posistitions};
    
    # Compare positions
    qlogprint( "Comparing positions\n");
    my $compared_readgroups = compare_positions( $indexed_posistitions, $xml_files, $read_groups, $params{start}, $params{binsize} );
    
    qlogprint( "Writing XML file\n" );
    write_xml($params{outfile}, 
              $xml_files, 
              $read_groups, 
              $indexed_posistitions, 
              $compared_readgroups, 
              $params{start}, 
              $params{binsize} 
    );
    
    qlogend();
}

sub validate_files {
  my $infiles = shift;
  my $root_node_name = shift;
  
  my $xml_files = [];
  my $file_id = 0;
  
  my $parser = XML::LibXML->new();
  $parser->load_ext_dtd(0);   # Don't load DTD - Minons can't access the URL
  $parser->no_network(0);
  $parser->recover(2);        # Suppressing error mesages.
  
  # Foreach input file.
  foreach my $file ( @{$infiles} ){
    qlogprint( "Checking file $file_id - $file\n");
    
    my $doc = undef;
    eval { $doc = $parser->load_xml( location => $file ); };
      #die $@ if $@; # Nobody is dieing today
      if (ref($@)) {
        print "Handle a structured error (XML::LibXML::Error object)\n";
      } 
      elsif ($@) {
        print  "Error, but not an XML::LibXML::Error object\n";
      }
      else{
        if ($doc->hasChildNodes) {
           foreach my $node ($doc->childNodes) {
             if ($node->nodeName =~ /$root_node_name/ and ref($node) =~ 'XML::LibXML::Element') {
               $xml_files->[$file_id] = {
                 index            => $file_id, 
                 path             => $file, 
                 lims_identifier  => basename($file, '.bam.qp.xml'),
                 type             => '',  # could be a final
                 xmlnode          => $node,
                 read_groups      => []
               };
                              
             }
           }
           # If we get this far, then we have NOT found any matches, so die!
           die "File $file did not appear to contain qProfiler XML"
             unless exists $xml_files->[$file_id];
        }
        else {
          die "XML doc [$file] appears to have no child nodes ???";
        }
    
        $file_id +=1;
    }
  }
  
  return $xml_files;
}

sub collect_header_readgroups {
  my $xml_files     = shift;
  
  my $xml_path = "/qProfiler/BAMReport/HEADER";
  my $file_count = scalar @{$xml_files};
  
  my $read_groups = [];
  my $RG_index = 0;
  
  
  for (my $file_index = 0; $file_index < $file_count; $file_index++) {
    my $file = $xml_files->[$file_index];
    my $root_node = $file->{xmlnode};
    
    # collect read group items as a hash from header
		foreach my $node ( @{ $root_node->find($xml_path) } ) {
			my $BAMheader = $node->textContent();
      while ($BAMheader =~ /(\@RG.*\n)/g) {
        my $readgroup_line = $1;
        
        $read_groups->[$RG_index] = {
          index  => $RG_index,
          file_index   => $file_index,
          type         => "read_group" 
        };
        
        while ($readgroup_line =~ /([A-Z]+):(\w+)/g) {
          $read_groups->[$RG_index]{$1} = $2;
        }
                
        # keep copy with file.
        push @{$file->{read_groups}}, $read_groups->[$RG_index];
        $RG_index += 1;        
      }
		}
  }
  
  return $read_groups;
}


sub collect_positions {
  my $positions     = shift;
  my $xml_files     = shift;
  my $read_groups   = shift;
  my $start         = shift;
  my $end           = shift;
  my $binsize       = shift;
 
  my $xml_path = "/qProfiler/BAMReport/ISIZE"; #
  my $file_count = scalar @{$xml_files};
  
  for (my $file_index = 0; $file_index < $file_count; $file_index++) {
    my $file = $xml_files->[$file_index];
    
    # TODO BY Read Groups
    my $results = collect_counts($file->{xmlnode}, $xml_path, $start, $end);
    
    foreach my $file_read_group ( @{$file->{read_groups}} ){
      my $RG_id = $file_read_group->{ID}; 
      my $RG_index = $file_read_group->{index};
      
      if ( ! exists $results->{$RG_id} ) {
        qlogprint( {l=>'WARN'}, "No positions found for \@RG $RG_id \n");
        $results->{$RG_id}{tallies} = [];
        $results->{$RG_id}{sum_count} = 0;
        #next();
      }
      
      my $read_group_positions = $results->{$RG_id};
      #$read_group_positions->{no_inserts} = scalar @{$read_group_positions->{tallies}} > 1 ? 0 : 1;
      $read_groups->[$RG_index]{no_inserts} = scalar @{$read_group_positions->{tallies}} > 1 ? 0 : 1;
      
      # set empty values for all bin positions for the file.
      for (my $i = $start; $i < $end; $i += $binsize) {
        $positions->{$i}[$RG_index] = {};
      }
      # set values for positions for read group
      foreach my $tally ( @{$read_group_positions->{tallies}} ) {
        $positions->{ $tally->{start} }[$RG_index] = $tally;
      }
      
      qlogprint( "File:$file_index  start:$start  end:$end  \@RG:$RG_id  count:",
                 $read_group_positions->{sum_count}, "\n");
    }
  }
  
}

sub collect_counts {
  my $root_node     = shift;
  my $xml_path      = shift;
  my $filter_start  = shift;
  my $filter_end    = shift;
  
  my $read_group_tallies = ();
  
  if ( $root_node->exists($xml_path) ) {
    foreach my $node ( @{ $root_node->find($xml_path) } ) {

      my @RG_items = $node->findnodes('RG');        
      foreach my $RG_item ( @RG_items ){
        my $RG_index = get_attr_by_name( $RG_item, 'value' );
        my @tally_items = $RG_item->findnodes('RangeTally/RangeTallyItem');
        
        $read_group_tallies->{$RG_index} = count_tallies(\@tally_items, $filter_start, $filter_end);  
      }
    }
    return $read_group_tallies;
  }
  else{
    qlogprint( {l=>'WARN'}, "xml_path $xml_path not found\n");
  }
}


sub count_tallies {
  my $tally_items   = shift;
  my $filter_start  = shift;
  my $filter_end    = shift;
  
  my $sum_of_counts = 0;
  my $tallies = ();
  foreach my $tally_item ( @{$tally_items} ){
      my $tally_count = get_attr_by_name( $tally_item, 'count' );
      my $tally_start = get_attr_by_name( $tally_item, 'start' );
      my $tally_end   = get_attr_by_name( $tally_item, 'end' );
      
      # Filter out those outside the range
      if ( defined $filter_start and defined $filter_end ) {
        next if ($tally_start < $filter_start or $tally_end > $filter_end );
      }
      
      $sum_of_counts += $tally_count;
      #my $gap = ($tally_end - $tally_start) ;
      #my $value = $tally_start + ($gap/2);
      
      push @{$tallies}, 
      {
        start   => $tally_start,
        end     => $tally_end,
        count   => $tally_count,
        percent =>  0
      }
  }
    
  # Calculate count at possition as percentage of total counts 
  foreach my $tally ( @{$tallies} ){
    $tally->{percent} = $tally->{count} / $sum_of_counts;
  }
  
  return { tallies => $tallies, sum_count => $sum_of_counts };
}




sub compare_positions {
  my $indexed_posistitions  = shift;
  my $xml_files     = shift;
  my $read_groups   = shift;
  my $start         = shift;
  my $binsize       = shift;
  
  my $compared_positions = [];
  my $file_count = scalar @{$xml_files};
  my $read_group_count = scalar @{$read_groups};
  my $bin_count = scalar @{$indexed_posistitions};
    
  for (my $index_a = 0; $index_a < $read_group_count; $index_a++) {
    my $RG_a =  $read_groups->[$index_a];   
    for (my $index_b = 0; $index_b < $read_group_count; $index_b++) { 
      my $RG_b =  $read_groups->[$index_b];
      
      # Calculate and sum differences between these 2 read groups for
      # each bin position.
      my $total_difference = 0;
      for (my $bin = $start; $bin < $bin_count; $bin +=$binsize) {
        my $difference = 0;
        # If file are not the same and both files have inserts sizes
        if ( $index_a != $index_b and ! $RG_a->{no_inserts} and ! $RG_b->{no_inserts} ) {
          my $percent_a = exists $indexed_posistitions->[$bin][$index_a]->{percent} ? $indexed_posistitions->[$bin][$index_a]->{percent} : 0;
          my $percent_b = exists $indexed_posistitions->[$bin][$index_b]->{percent} ? $indexed_posistitions->[$bin][$index_b]->{percent} : 0;
          $difference = abs ($percent_a - $percent_b);
          $total_difference += $difference;
        }
        $compared_positions->[$index_a][$index_b][$bin] = $difference;
      }
      qlogprint( "$index_a (", $RG_a->{ID}, ") vs $index_b (",$RG_b->{ID},") = $total_difference\n" ); 
    }
  }
  return $compared_positions;
}


sub collect_metadata {
  my $xml_files   = shift;
  my $file_count  = scalar @{$xml_files};
  
  #my $metadata = QCMG::DB::Metadata->new();
  my $metadata = {};
  
  my @metadata_elements = (
    "aligner",
    "capture_kit",
    "failed_qc",
    "isize_manually_reviewed",
    "isize_max",
    "isize_min",
    "library_protocol",
    "material",
    "parent_project",
    "primary_library",
    "project",
    "sample",
    "sample_code",
    "sequencing_platform",
    "species_reference_genome"
  );
  
  for (my $index = 0; $index < $file_count; $index++) {    
    my $file = $xml_files->[$index];
    $file->{metadata} = {};
    
    printf ("#%s\t %s -> %s\n", $index, $file->{lims_identifier}, $file->{path});
    my $resource = $file->{lims_identifier};
    #my $resource_type = $file->{type};
    
    my $resource_metadata = undef;
#    foreach my $resource_type ( ("mapset", "mergedbam") ) {
#      if ( $metadata->find_metadata($resource_type, $resource) ) {
#        $resource_metadata = $metadata->mapset_metadata($resource);
#        $file->{type} = $resource_type;
#        last();
#      }  
#    }
    
    foreach my $element ( @metadata_elements ){
      my $value = "Not Available";
      
      if ( exists $resource_metadata->{$element} ) {
        $value = $resource_metadata->{$element};
        if ( ! defined $value ) {
          $value = "";
        }
      }
      
      $file->{metadata}{$element} = $value;
    }
    
  }
}


sub write_xml {
  my $outfile       = shift;
  my $files         = shift;
  my $read_groups   = shift;
  my $bined_values  = shift;
  my $compared      = shift;
  my $bin_start     = shift;
  my $bin_size      = shift; 
  
  my $output = IO::File->new(">$outfile");
	if ( ! $output ) {
		warn "Could not write to $output\n"; 
		return 0;
	}
  
  my $file_count = scalar @{$files};
  my $read_group_count = scalar @{$read_groups};
  my $compared_count = scalar @{$compared};
  my $bin_count = scalar @{$bined_values};
  
  qlogprint( {l=>'INFO'}, "Writing to $outfile\n");
  my $writer = XML::Writer->new(OUTPUT => $output, DATA_MODE => 'true', DATA_INDENT => 2, ENCODING => 'utf-8');
	$writer->xmlDecl( 'UTF-8' );
	$writer->startTag( 'qLibCompare', 'version'=>"0.1");
  
  # FILES
  print "Files\n";
  $writer->startTag( 'files' );
    for (my $file_index = 0; $file_index < $file_count; $file_index++) {    
      my $file = $files->[$file_index];
      my $attributes = $file->{metadata};
      # Add extra details
      $attributes->{index}      = $file_index;
      $attributes->{lims_identifier} = $file->{lims_identifier};
      $attributes->{path}            = $file->{path};
      # TODO $attributes->{sum_count}       = $file->{sum_count};
      $attributes->{type}            = $file->{type};
      #foreach my $rg ( @{$file->{read_groups}} ){
      #  print $rg->{index}, "";
      #}
      #$attributes->{read_groups}            = $file->{read_groups};
      # TODO $attributes->{no_inserts}      = $file->{no_inserts};
      #print Dumper $attributes;
      $writer->emptyTag('file', %{$attributes});
    }
  $writer->endTag(  );	# /files
  
  # Read Groups
  print "Read Groups\n";
  $writer->startTag( 'read_groups' );
    for (my $RG_index = 0; $RG_index < $read_group_count; $RG_index++) {    
      my $RG = $read_groups->[$RG_index];
      #print Dumper $RG;
      #   #my $attributes = $file->{metadata};
      #   # Add extra details
      #   $attributes->{id}              = $file_id;
      #   $attributes->{lims_identifier} = $file->{lims_identifier};
      #   $attributes->{path}            = $file->{path};
      #   # TODO $attributes->{sum_count}       = $file->{sum_count};
      #   $attributes->{type}            = $file->{type};
      #   # TODO $attributes->{no_inserts}      = $file->{no_inserts};
      #   #print Dumper $attributes;
      $writer->emptyTag('RG', %{$RG});
    }
  $writer->endTag(  );	# /files
  
  
  $writer->startTag( 'BAMReport' );
  # ISIZE
    $writer->startTag( 'ISIZE' );
      print "RangeTally\n";
      $writer->startTag( 'RangeTally' );
      for (my $bin = $bin_start; $bin < $bin_count; $bin +=$bin_size) {
        my $start = $bin;
        my $end = $bin + $bin_size-1;
        $writer->startTag( 'RangeTallyItem', 
                           start  => $start+0, 
                           end  =>  $end+0 
                           );
        for (my $id = 0; $id < $file_count; $id++) {
          my $file_bin = $bined_values->[$bin][$id];
          my $count = exists $file_bin->{count} ? $file_bin->{count} : 0;
          my $percent = exists $file_bin->{percent} ? $file_bin->{percent} : 0;
          $writer->emptyTag('TallyItem', 
                              'RG_index'      => $id+0,
                              'count'   => $count+0,
                              'percent' => $percent+0
                            );
          #$writer->dataElement('percent',  $percent+0);
          #$writer->endTag(  );	# /TallyItem
        }
        $writer->endTag(  );	# /RangeTallyItem
        #last();
      }
      $writer->endTag(  );	# /RangeTally
    $writer->endTag(  );	# /ISIZE
  $writer->endTag(  );	# /BAMReport
  
  # COMPARES
  print "Compares\n";
  $writer->startTag( 'Compares' );
  for (my $index_a = 0; $index_a < $compared_count; $index_a++) {
    $writer->startTag( 'RG', 'index'=> $index_a+0);    
    for (my $index_b = 0; $index_b < $compared_count; $index_b++) {
      $writer->startTag( 'COMPARED', 'index'=> $index_b+0);
       for (my $bin = $bin_start; $bin < $bin_count; $bin +=$bin_size) {
         my $value = $compared->[$index_a][$index_b][$bin];
         my $start = $bin;
         my $end = $bin + $bin_size-1;
         $writer->emptyTag( 'RangeTallyItem', 
           'start'  => $start+0, 
           'end'    => $end+0, 
           'percentage_difference'  => $value+0 );
      }  
      $writer->endTag(  );	# /COMPARED
    }
    $writer->endTag(  );	# /RG
  }  
  $writer->endTag(  );	# /COMPARES
	$writer->endTag(  );	# /qLibCompare
	$writer->end(  );
  
  
  
  return 1;
  
}
