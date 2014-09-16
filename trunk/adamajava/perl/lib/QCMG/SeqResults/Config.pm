package QCMG::SeqResults::Config;

##############################################################################
#
# Script:   Config.pm
# Creator:  Matthew J Anderson
# Created:  2013-02-26
#
# Requires:
# 	QCMG::IO::JsonReader		- QCMG Module
#
##############################################################################

use strict;                     # Good practice
use warnings;                   # Good practice
use Data::Dumper;               # Perl core module
use QCMG::IO::JsonReader;		    # QCMG IO module
use QCMG::Util::QLog;           # QCMG Log module

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 3390 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: GeneusReader.pm 3390 2013-02-19 01:33:02Z m.anderson $'
    =~ /\$Id:\s+(.*)\s+/;


################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 scalar: json_config (optional)		A JSON config file.
 scalar: debug (optional)			Set as 1 for debugging.

Returns:
 a new instance of this class.

=cut

sub new {
	my $class   	  = shift;
	my $json_config	= shift;
	my $debug   	  = shift;
	
  return undef unless -e $json_config;
	#if (  ) {
	#	$json_config = $ENV{'QCMG_SVN'}."/QCMGProduction/automation/qcmg-clustermk2/seq_results.config.json";
	#}
		
	#print "Config file is $json_config\n" if $debug;
		
	my $json_reader     = QCMG::IO::JsonReader->new($debug);
	my $config_details  = $json_reader->decode_json_file( $json_config );
		
	my $self = {
		parent_project_groups			=> $config_details->{parent_project_groups},
		parent_project_list				=> $config_details->{parent_project_list},
		categories_list						=> $config_details->{categories_list},
		categories_resources			=> $config_details->{categories_resources},
		resource_reports					=> $config_details->{resource_reports},
		category_metadata					=> $config_details->{category_metadata},
		metadata									=> $config_details->{metadata},
		category_to_resource_type	=> $config_details->{category_to_resource_type},
		resource_type_to_category	=> $config_details->{resource_type_to_category},
		coverage_tag_list					=> $config_details->{coverage_tag_list},
			
		parent_project_categories	=> $config_details->{parent_project_categories},
		project_categories				=> $config_details->{project_categories},
		searchable_categories			=> $config_details->{searchable_categories},		
		exempt_donorsdirs					=> $config_details->{exempt_donorsdirs},
			
    debug           => ( $debug ?  1 :  0  )
    };    
		
	#print Dumper $self->{categories_list};
		
	bless $self, $class;
	return $self;
}


################################################################################

=pod

=head1 METHODS

B<decode_json_file()>

Decodes the contents of JSON file format into a Perl data structure. 

Parameters:
 scalar: json_file 	File path to the JSON file.

Returns:
 a Perl data structure. Its up to the user to know what to do with it.

=cut



sub parent_project_categories {
	my $self	= shift;
	return $self->{parent_project_categories};
}


sub project_categories {
	my $self	= shift;
	return $self->{project_categories};
}

sub exempt_donorsdirs {
	my $self	= shift;
	return $self->{exempt_donorsdirs};
}



sub parent_projects {
	my $self	= shift;
	
	my $parent_project_list = $self->{parent_project_list};
	my @parent_projects = keys %$parent_project_list;
	return \@parent_projects;
}

sub parent_project_dir {
	my $self			= shift;
	my $parent_project 	= shift;
	
	if ( exists $self->{parent_project_list}{$parent_project} ) {
		return $self->{parent_project_list}{$parent_project}{results_dir};
	}
	return 0;
} 


sub projects_of_parent_project {
	my $self			= shift;
	my $parent_project 	= shift;
	
	my $parent_project_dir = $self->parent_project_dir($parent_project);
	my $projects = []; 
	
	opendir(DIR, $parent_project_dir);
	foreach my $file ( readdir(DIR) ){
		if ( -d "$parent_project_dir/$file" ) {
			push @$projects, $file
			        unless grep { $_ eq $file } $self->{exempt_donorsdirs};
		}
	}
	closedir(DIR);
	
	return $projects;
}

sub categories {
	my $self				= shift;
	my $category_selection 	= shift;
	
	my $categories;
	if ( $category_selection and $category_selection eq "parent_project" ) {
		$categories = $self->{parent_project_categories};
		
	} elsif ( $category_selection and $category_selection eq "project" ){
		$categories = $self->{project_categories};
	
	}else { 
		my $categories_list = $self->{categories_list};
		my @all_categories = keys %$categories_list;
		$categories = \@all_categories;
  	}	
	return $categories;
}

sub category_dir {
	my $self		= shift;
	my $category 	= shift;
	
	if (  exists $self->{categories_list}{$category} ) {
		return $self->{categories_list}{$category}{results_dir};
	}
	return '';
}



sub category_resources {
	my $self		= shift;
	my $category 	= shift;
	
	if ( exists $self->{categories_list}{$category} ) {
		return $self->{categories_list}{$category}{resources};
	}
	return '';
}

sub resource_file { 
	my $self		= shift;
	my $resources 	= shift;

	if ( exists $self->{categories_resources}{$resources} ) {
		return $self->{categories_resources}{$resources}{file};
	}
	return '';
}

sub resource_reports {
	my $self		= shift;
	my $resources 	= shift;
	
	if ( exists $self->{categories_resources}{$resources} ) {
		return $self->{categories_resources}{$resources}{reports};
	}
	return '';
}

sub report_file { 
	my $self		= shift;
	my $report 		= shift;

	if ( exists $self->{resource_reports}{$report} ) {
		return $self->{resource_reports}{$report}{report};
	}
	return '';
}

sub report_log {
	my $self		= shift;
	my $report 		= shift;

	if ( exists $self->{resource_reports}{$report} ) {
		return $self->{resource_reports}{$report}{log};
	}
	return '';
}

sub category_to_resource_type {
	my $self		= shift;
	my $category 		= shift;
	
	if ( exists $self->{categories_list}{$category} and exists $self->{category_to_resource_type}{$category} ){
		return $self->{category_to_resource_type}{$category};
	}
	return '';
}


sub coverage_tags {
	my $self = shift;
	
	my $coverage_tag_list = $self->{coverage_tag_list};
	my @coverage_tags = keys %$coverage_tag_list;
	return \@coverage_tags;
}


1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013

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
