#!/usr/bin/perl -w

##############################################################################
#
#  Program:  qsignature_xml_updater.pl
#  Author:   Matthew J Anderson
#  Created:  2013-12-05
#
# Takes a qsignature log file and slices it into per project (Donor) 
#
#  $Id: qsignature_xml_updater.pl 4669 2014-07-24 10:48:22Z j.pearson $
#
##############################################################################

use strict;       # Good practice
use warnings;     # Good practice

use Carp qw( carp croak verbose );
use Data::Dumper;   # Perl core module
use Getopt::Long;   
use IO::File;       # Perl core module
use File::Basename; # Perl core module
use Pod::Usage;     
use XML::LibXML;    # CPAN
use XML::Writer;		# CPAN

# QCMG modules
use QCMG::DB::Metadata;   # QCMG LIMS module
use QCMG::Util::QLog;   # QCMG logging module
use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );


use vars qw( $SVNID $REVISION $VERSION $VERBOSE );

( $REVISION ) = '$Revision: 4669 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: qsignature_xml_updater.pl 4669 2014-07-24 10:48:22Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;

#############################################################################
# # #
# #	    MAIN
# 
##############################################################################
MAIN: {
  
  # Setup defaults for important variables.
  my $mode						= '';
	my $qsignature_xml	= '';
  my $logfile					= '';
     $VERBOSE					= 0;
     $VERSION					= 0;
  my $help   					= 0;
  my $man    					= 0;
  
  # Print usage message if no arguments supplied
  pod2usage(1) unless (scalar @ARGV > 0);

  # Use GetOptions module to parse commandline options
  my $cmdline = join(' ',@ARGV);
	my $results = GetOptions (
					'g|mode=s'           	 => \$mode,  						# -g
          'i|infile=s'           => \$qsignature_xml, 	# -i
          'l|logfile=s'          => \$logfile,        	# -l
          'v|verbose+'           => \$VERBOSE,        	# -v
          'version!'             => \$VERSION,        	# --version
          'h|help|?'             => \$help,           	# -?
          'man|m'                => \$man             	# -m
	);
  
  if ($VERSION) {
      print "$SVNID\n";
      exit;
  }

  qlogfile($logfile) if $logfile;
  qlogbegin;
  qlogprint( {l=>'EXEC'}, "CommandLine $cmdline\n");
  pod2usage(1) if $help;
  pod2usage(-exitstatus => 0, -verbose => 2) if $man;
  
	
	die "Unable to read file $qsignature_xml\n" unless (-R $qsignature_xml);
	
	
	my $parser = XML::LibXML->new();
	my $doc = $parser->load_xml( location => $qsignature_xml );
	die "File $qsignature_xml does not contain qsignature XML\n" unless $doc->exists( '/qsignature' );

	add_file_metadata($doc);
	if ($qsignature_xml =~ /(.*)\.xml/) {
		$doc->toFile("$1.qvismeta.xml");
	}
	
	#print basename ($qsignature_xml, ".xml"), "\n"; 
	
	#my @qsig_files 				= $doc->findnodes( '/qsignature/files/file' );
	#my @qsig_comparisons 	= $doc->findnodes( '/qsignature/comparisons/*' );	
	#print "Found ". scalar @qsig_files ." files\n";
	#print "Found ". scalar @qsig_comparisons ." comparisons\n";
	
  #print "To Get Metadata\n";
  #collect_metadata(\@qsig_files);
		
	#my $comparison_collection = slice_comparisons( \@qsig_comparisons );
	
	#if ( $mode eq "donor") {
	#	my $donor_collection = slice_donor_files( \@qsig_files );
	#	#dice_donors($donor_collection, $comparison_collection);
	#}
	
}

sub add_file_metadata {
	my $doc = shift;
	
	my $metadata = QCMG::DB::Metadata->new();
	my @qsig_files = $doc->findnodes( '/qsignature/files/file' );
	
	foreach my $node ( @qsig_files ){
		if ($node->hasAttributes) {
        my @attributes = $node->attributes;
				
				my $file = {
					path 						=> dirname( $node->getAttribute( 'name' ) ),
					file 						=> basename( $node->getAttribute( 'name' ) ),
					type 						=> "Unknown",
					project_path 		=> '',
	        parent_project  => "Unknown",
	        project         => "Unknown",
	        sample          => "Unknown",
	        sample_code     => "Unknown",
	        material        => "Unknown",
	        failed_qc       => 0
				}; 
				
				# /root_path/<parent_project>/<project>/<sub_dir_name>
				if ( $file->{path} =~ /(.*\/(.+))\/(seq_mapped|SNP_array)/ ) {
					$file->{project_path}	= $1;
					$file->{project} 			= $2;
					$file->{type} 				= $3;
					if ( $file->{type} =~ /seq_mapped/ ) {
						$file->{type} = 'mapset';
					}
					elsif ( $file->{type} =~ /SNP_array/) {
						$file->{type} = 'microarray';
					}	
				}
				
				# Find Metadata for the file.
				my $resource_type = $file->{type};
	      my $resource = $file->{file};
				
				if ( $resource =~ /(.+)(\.txt|\.bam)\.qsig/ ) {
					$resource = $1;
          print "Microarray/Mapset name $resource\n";
				}
				if ( $file->{type} eq 'microarray'){
				  # Matches old Naming of sample and array. Just keep array ID.
          if ($resource =~ /\w+_(\w+_\w+)/) {
            $resource = $1;
            print "Microarray name $resource\n";
          }
        }
				
        $file->{name} = $resource;
				if ( $resource_type and $metadata->find_metadata($resource_type, $resource) ) {									
	        $file->{parent_project} = $metadata->parent_project();
	        $file->{project}        = $metadata->project();
	        $file->{sample}         = $metadata->sample();
	        $file->{sample_code}    = $metadata->sample_code();
	        $file->{material}       = $metadata->material();
										
	        if ( $resource_type eq "microarray" ) {
	          $file->{failed_qc} = $metadata->array_failed_qc($resource);
	        } 
					elsif ( $resource_type eq "mapset" ){
	          $file->{failed_qc} = $metadata->failed_qc($resource);
	        }
	      }else{
	        print "[Warning] Could not find Metadata for $resource_type $resource\n";
	      }
				
				foreach my $attribute ( keys %{$file} ){
					$node->setAttribute($attribute, $file->{$attribute});					 
				} 				
			}
				
#		print $node->toString, "\n\n";
		
	}
}



sub __slice_comparisons {
	my $qsig_comparisons				= shift;
	my $comparison_collection 	= {};
	
	foreach my $qsig_comparison ( @{$qsig_comparisons} ){
		my $id1 = undef;
		my $id2 = undef;
		
		if ( $qsig_comparison->nodeName =~ /id_(\d+)_vs_(\d+)/ ) {
			$id1 = $1;
			$id2 = $2;
			#print "comparison $id1 vs $id2\n";
			if ( ! exists $comparison_collection->{$id1} ) {
				$comparison_collection->{$id1} = ();
			}
			push (@{$comparison_collection->{$id1}}, $qsig_comparison);
		}
	}
	
	return $comparison_collection;
}


sub __slice_donor_files {
	my $qsig_files				= shift;
	my $donor_collection 	= {};
	
	foreach my $qsig_file ( @{$qsig_files} ){
		my $qsig_filename	= get_attr_by_name( $qsig_file, 'name' );
		my $path 	= dirname($qsig_filename);
		
		
		my $project = '';
		my $project_dir = $path;
		if ( $path =~ /(.*\/(.+))\/(seq_mapped|SNP_array)/ ) {
			$project_dir	= $1;
		}
		
		if ( ! exists $donor_collection->{$project_dir} ) {
			$donor_collection->{$project_dir} = { qsig_files => (), qsig_comparisons => () };
		}
		push ( @{$donor_collection->{$project_dir}{qsig_files}}, $qsig_file );
	}

	return $donor_collection;
}

sub __dice_donors {
	my $donor_collection 			= shift;
	my $comparison_collection = shift;
	
  my $metadata = QCMG::DB::Metadata->new(1);
  print "Prefetching Metadata...\t";
  $metadata->prefetch_all_metadata();
  print "Done\n";
  
	#my $stop_count = 0;
	my @donors = sort keys %{$donor_collection};
	foreach my $donor_dir ( @donors ) {
		
		my $files = ();
		my $comparisons = ();
				
		foreach my $qsig_file ( @{ $donor_collection->{$donor_dir}{qsig_files} } ){
			my $qsig_id 			= get_attr_by_name( $qsig_file, 'id' );
			my $qsig_coverage	= get_attr_by_name( $qsig_file, 'coverage' );
			my $qsig_filename	= get_attr_by_name( $qsig_file, 'name' );
			my $filename 			= basename($qsig_filename);
			my $resource = '';
      my $type = '';
      my $resource_metadata = {
        parent_project  => "Not Available",
        project         => "Not Available",
        sample          => "Not Available",
        sample_code     => "Not Available",
        material        => "Not Available",
        failed_qc       => 0
      };
      
			if ( $filename =~ /(.+)\.txt\.qsig/ ) {
				$resource = $1;
        $type = "microarray";
			}
			elsif ( $filename =~ /(.+)\.bam\.qsig/ ) {
				$resource = $1;
        $type = "mapset";
			}
      

      if ( $type and $metadata->find_metadata($type, $resource ) ) {
        #print "$type $resource\t\thas Metadata\n";
        $resource_metadata->{parent_project} = $metadata->parent_project();
        $resource_metadata->{project}        = $metadata->project();
        $resource_metadata->{sample}         = $metadata->sample();
        $resource_metadata->{sample_code}    = $metadata->sample_code();
        $resource_metadata->{material}       = $metadata->material();
        if ( $type eq "microarray" ) {
          $resource_metadata->{failed_qc} = $metadata->failed_qc($resource);
        } elsif ( $type eq "mapset" ){
          $resource_metadata->{failed_qc} = $metadata->array_failed_qc($resource);
        }  
      }else{
        print "Could not find Metadata for $type $resource\n";
      }
      
      
			my %file = (id 				      => $qsig_id, 
									coverage 	      => $qsig_coverage, 
									name 			      => $qsig_filename,
									type 			      => $type,
                  parent_project  => $resource_metadata->{parent_project},
                  project         => $resource_metadata->{project},
                  sample          => $resource_metadata->{sample},
                  sample_code     => $resource_metadata->{sample_code},
                  material        => $resource_metadata->{material},
                  failed_qc       => $resource_metadata->{failed_qc}
								);
			push (@{$files}, \%file);
			
			foreach my $qsig_comparison ( @{$comparison_collection->{$qsig_id}} ) {				
				if ( $qsig_comparison->nodeName =~ /id_(\d+)_vs_(\d+)/ ) {
					my $mapset = $1;
					my $snparray = $2;
					my $qsig_calcs 		= get_attr_by_name( $qsig_comparison, 'calcs' );
					my $qsig_overlap	= get_attr_by_name( $qsig_comparison, 'overlap' );
					my $qsig_score		= get_attr_by_name( $qsig_comparison, 'score' );
										
					my %comparison = (mapset 		=> $mapset, 
														snp_array => $snparray,
														calcs 		=> $qsig_calcs, 
														overlap		=> $qsig_overlap,
														score 		=> $qsig_score
													);
					push (@{$comparisons}, \%comparison);
				}
			} # end qsig_comparison
			
		} # end qsig_file
		
		create_donor_xml ($donor_dir , $files, $comparisons); 	
		
		#if ( $stop_count++ >= 15 ) {
		#	last;
		#}
	} # end donor
	
}

sub __create_donor_xml {
	my $donor_path	= shift;
	my $files 			= shift;
	my $comparisons = shift; 
	my $mode = 'donor';
	
	if ( $donor_path =~ /.*\/(.+)/ ) {
		my $project	= $1;
		print "$project";

		if ( scalar $files and scalar $comparisons ) {
			my $diced_qsig_xml = "$project.qsig.$mode.xml";
			my $output = IO::File->new(">$donor_path/$diced_qsig_xml");
			if ( ! $output ) {
				warn "Could not write to $diced_qsig_xml\n"; 
				return 0;
			}
			print "\twriting to to $donor_path/$diced_qsig_xml\n";
			my $writer = XML::Writer->new(OUTPUT => $output, DATA_MODE => 'true', DATA_INDENT => 2, ENCODING => 'utf-8');
			$writer->xmlDecl( 'UTF-8' );
			$writer->startTag( 'qsignature', 'version'=>"0.1");
			$writer->startTag( 'files' );
			foreach my $file ( @{$files} ){
				$writer->emptyTag('file', %{$file}
													#'id' 			 => $file->{id},
													#'coverage' => $file->{coverage},
													#'name'		 => $file->{name},
													#'type'		 => $file->{type}
													);
			}
			$writer->endTag(  );	# /files
			$writer->startTag( 'comparisons' );
			foreach my $comparison ( @{$comparisons} ){
				$writer->emptyTag('comparison', 
													'id'		 			 => $comparison->{mapset}."_vs_".$comparison->{snp_array},
													'mapset' 			 => $comparison->{mapset},
													'snp_array'		 => $comparison->{snp_array},
													'calcs' 		 	 => $comparison->{calcs},
													'overlap'			 => $comparison->{overlap},
													'score'				 => $comparison->{score}
													);						
			}
			$writer->endTag(  );	# /comparisons
			$writer->endTag(  );	# /qsignature
			$writer->end(  );
			
			return 1;
		}
		else{
			print "\n";
		}
	}

}




sub __parse_xml {
	my $mode			= shift;
  my $xml_file	= shift;
  
	my $doc = undef;
  if ( -R $xml_file ) {
		$doc = XML::LibXML->load_xml( location => $xml_file );
		
		if ($doc->exists( '/qsignature' )) {
			my $comparison_collection 	= {};
			my $current_project_dir 		= '';
			my $project_comparisons 		= {};
			my $diced_log_created 			= 0;
			
			my @qsig_files = $doc->findnodes( '/qsignature/files/file' );
			my @qsig_comparisons = $doc->findnodes( '/qsignature/comparisons/*' );

			my $stop_count = 0;
			foreach my $qsig_comparison ( @qsig_comparisons ){
				my $id1 = undef;
				my $id2 = undef;
				
				if ( $qsig_comparison->nodeName =~ /id_(\d+)_vs_(\d+)/ ) {
					$id1 = $1;
					$id2 = $2;
					#print "comparison $id1 vs $id2\n";
					if ( ! exists $comparison_collection->{$id1} ) {
						$comparison_collection->{$id1} = ();
					}
					push (@{$comparison_collection->{$id1}}, $qsig_comparison);
				}
			}
			#print Dumper $comparison_collection;
			
			$stop_count = 0;
			my $output	= undef;
			my $writer	= undef;
			
			foreach my $qsig_file ( @qsig_files ){
				my $qsig_id 			= get_attr_by_name( $qsig_file, 'id' );
				my $qsig_coverage	= get_attr_by_name( $qsig_file, 'coverage' );
				my $qsig_filename	= get_attr_by_name( $qsig_file, 'name' );
				my $file 	= basename($qsig_filename);
				my $path 	= dirname($qsig_filename);
				
				my $project = '';
				my $project_dir = $path;
				if ( $path =~ /(.*\/(.+))\/(seq_mapped|SNP_array)/ ) {
					$project_dir	= $1;
					$project			= $2
				}
				
				#if ( $current_project_dir ne $project_dir ) {
				#	#if ($diced_log_created) {
				#	#	$writer->endTag(  );	# /files
				#	#	# Add comparissons
				#	#	$writer->endTag(  );	# /qsignature
				#	#	$writer->end(  );
				#	#}
				#	$diced_log_created = 0;
        #
				#	$current_project_dir = $project_dir;
				#	print "$current_project_dir\n";
				#	$stop_count++
				#}

				my $comparisons = $comparison_collection->{$qsig_id};
				if ( scalar $comparisons ) {
						
						
						#if ( ! $diced_log_created ) {
						#	my $diced_qsig_xml = "$project.qsig.$mode.xml";
						#	$output = IO::File->new(">$diced_qsig_xml");
						#	die "Could not write to $diced_qsig_xml\n" unless $output;
						#	$writer = XML::Writer->new(OUTPUT => $output, DATA_MODE => 'true', DATA_INDENT => 2, ENCODING => 'utf-8');
						#	$writer->xmlDecl( 'UTF-8' );
						#	$writer->startTag( 'qsignature' );
						#	$writer->startTag( 'files' );
						#	print "Opened diced xml file $diced_qsig_xml\n";
						#	$diced_log_created++;
						#	
						#}
						#
						#$writer->emptyTag('file', 
						#									'id' => $qsig_id,
						#									'coverage' => $qsig_coverage,
						#									'name' => $qsig_filename
						#									);
						
						#print $qsig_file->toString() . "\n";
						print "\t$qsig_id\t$file\n";
						
						foreach my $id_comparison  ( @{$comparisons} ) {
							print $id_comparison->toString() . "\n";
						#	
						#
						#	#print "Write entry to file here!\n";
						#
						#	print "\t\tscore: ". get_attr_by_name( $id_comparison, 'score' ) . "\n";
						}
					}
					#}
				
				
				
				
				
				

				#print "$stop_count\n";
				if ( $stop_count >= 10 ) {
					last;
				}
				
				
				
				
			}
			
			


			
			
			#my $output = IO::File->new(">output.xml");
			#my $writer = XML::Writer->new(OUTPUT => $output, DATA_MODE => 'true', DATA_INDENT => 2, ENCODING => 'utf-8');
			#$writer->xmlDecl( 'UTF-8' );
			#$writer->startTag( 'qsignature' );
			#$writer->startTag( 'files' );
			#$writer->emptyTag( 'file' );
			#$writer->emptyTag( 'file' );
			#$writer->emptyTag( 'file' );
			#$writer->endTag(  );
			#$writer->startTag( 'comparisons' );
			#$writer->emptyTag( 'id_1_vs_9086' );
			#$writer->emptyTag( 'id_1_vs_9087 ' );
			#$writer->emptyTag( 'id_2_vs_9087 ' );
			#$writer->endTag(  );
			#$writer->endTag(  );
			
			# <?xml version="1.0" encoding="UTF-8" standalone="no"?>
			# <qsignature>
			# 	<files>
			# 		<file ... >
			# 	</files>
			# 	<comparisons>
			# 		<id_1_vs_9086 ...
			# 	</comparisons>
			# </qsignature>
			# 
			 
			exit;
		} 
		# If we get this far then we found no matches so die
		die "File $xml_file does not contain qsignature XML";
	}
	else {
		die "XML doc appears to have no child nodes ???";
	}
}



__END__

=head1 NAME

qsignature_logslicer.pl - Splits comparions scores into per-directory log files


=head1 SYNOPSIS

 qsignature_logslicer.pl -i qsignature_comparison.xml


=head1 ABSTRACT

Somthing Somthing.


=head1 OPTIONS

 -i | --infile       VCF allele frequency file(s)
 -l | --logfile       Log file; optional
 -v | --verbose       print progress and diagnostic messages
      --version       print version number
 -? | --help          display help
 -m | --man           display man page


=head1 DESCRIPTION

Somthing Somthing

=head2 Commandline options

=over2

=item B<-i | --infile>

XML file created by qSignature. Contains comparison scores.

=back


=head1 AUTHOR

=over 2

=item Matthew Anderson, L<mailto:m.anderson5@uq.edu.au>

=back


=head1 VERSION

$Id: qsignature_xml_updater.pl 4669 2014-07-24 10:48:22Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2014

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
