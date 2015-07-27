package QCMG::Visualise::VisualiseConfig;

##############################################################################
#
#  Module:   QCMG::Visualise::HtmlReport.pm
#  Author:   Matthew J Anderson
#  Created:  2013-06-26
#
#  $Id$
#
##############################################################################

use strict;
use warnings;

#use Carp qw( carp croak confess );
use Data::Dumper;
use JSON qw( encode_json ); # From CPAN
#use XML::LibXML;
use Clone qw( clone );		  # From CPAN 
# 
use QCMG::IO::JsonReader;   # QCMG Module for reading a JSON file
use QCMG::Util::QLog;       # QCMG Module for Logging


use vars qw( $SVNID $REVISION $INDEXES );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
	 =~ /\$Id:\s+(.*)\s+/;
( $INDEXES ) = {
     dynamic_tab => 'dynamic_tab_id',
     tab => 'tab_id',
     tab_content => 'content_id',
     tab_template => 'tab_template_id',
     tab_content_template => 'tab_content_template_id',
     table => 'table_id',
     chart => 'chart_id',
     chart_data => 'chart_data_id',
     chart_template => 'chart_template_id',
     chart_data_template => 'chart_data_template_id'
   };
   

sub new {
    my $class  = shift;
    my %params = @_;
    
    my $self = {config      => undef,
                indexes     => {},
                verbose     => $params{verbose} || 0 
            };
    
    bless $self, $class;
    
    # We expect at least these high level objects in the JSON config file.
    my @required_objects = qw( report nav_tabs tab tab_content chart chart_data);
    
    if ( defined $params{json} ) {
        # Parse JSON config file. This tells us what to do!
        my $json_reader = QCMG::IO::JsonReader->new();		
        $self->{config} = $json_reader->decode_json_file( $params{json} );
        $self->_check_config( \@required_objects );
        
        die("Cannot open json config file $params{json} for reading: $!\n")
            unless defined $self->{config} ;
    }
    
    elsif ( defined $params{xml} ) {
        die("Not ready to use xml as a config file\n");
    }
    
    else{
        die("No config file given\n");
    }
    

    foreach my $index ( keys %{$INDEXES} ){
      $self->_set_index($index, $INDEXES->{$index});
    }
    #print Dumper $self->{indexes};
    
	 return $self;
}

# TODO pod _check_config() - Check config has required elements
sub _check_config {
	 my $self   = shift;
	 my $required_objects = shift;
	 my $configfile_errors = 0;
	      
	 foreach my $object ( @{$required_objects} ){
		 if ( ! defined $self->{config}{$object} ) {
			 print "Expected object \"$object\" was not found in config file.\n";
			 $configfile_errors += 1;
		 }
	 }
	 
	 if ( ! defined $self->{config}{report}{xml_root}) {
		 print "Expected object \"report\" does not have \"xml_root\" defined \n";
		 $configfile_errors += 1;
		 #print Dumper $self->{config}{report};
	 }
	 
	 if ($configfile_errors) {
		 my $message = sprintf ("Config file %s is not valid\n", $self->{configfile});
		 die $message;
	 }
	 
	 return 1;
}



=cut _by_index()

Creates and index for a top level category in the config file for a given key.

Parameters:
 root_element: scalar string      This is the category in the config file.
 identifier_key: scalar string    Key for which shall be the identifier 

Returns:
 item: hash   if item
=cut
sub _set_index {
  my $self          = shift;
  my $root_element  = shift;
  my $identifier_key    = shift;

  $self->{indexes}{$root_element} = {};
  
  if ( exists $self->{config}{$root_element} ) {
    foreach my $details ( @{$self->{config}{$root_element}} ){
      if ( exists $details->{$identifier_key}) {
        $self->{indexes}{$root_element}{$details->{$identifier_key}} = $details;
      }else{
        qlogprint( {l=>'ERROR'}, "The identifier key $identifier_key does not exist in the Config file for the category $root_element.\n");
      }
    }
  }else{
    qlogprint( {l=>'ERROR'}, "The category $root_element does not exist in the Config file. An index will not be created for $root_element.\n");
  }

}

=cut _by_index()

Returns an item by its given index for a top level category in the config file.

Parameters:
 root_element: scalar string      This is the category in the config file.
 identifier: scalar string        ID for item 

Returns:
 item: hash   if item
=cut
sub _by_index {
  my $self          = shift;
  my $root_element  = shift;
  my $identifier    = shift;
  
  if ( exists $self->{indexes}{$root_element}{$identifier} ) {
    return $self->{indexes}{$root_element}{$identifier};
  }
  return '';
}


=cut _tab_by_index()

Returns details on a tab using an index rather then
interating over the array of tabs

Parameters:
 identifier: scalar string    ID for tab

Returns:
 tab: hash                    tab item
=cut
sub _tab_by_index {
 my $self       = shift;
 my $identifier = shift;
 
 return $self->_by_index("tab", $identifier);
}


=cut _dynamic_tab_by_index()

Returns details on a dynamic tab using an index rather then
interating over the array of dynamic tabs

Parameters:
 identifier: scalar string   ID for dynamic tab
 
Returns:
 dynamic_tab: hash           Dynamic tab item
=cut
sub _dynamic_tab_by_index {
  my $self        = shift;
  my $identifier  = shift;
  
  return $self->_by_index("dynamic_tab", $identifier);
}

=cut _dynamic_tab_by_index()

Returns details on a dynamic tab using an index rather then
interating over the array of dynamic tabs

Parameters:
 identifier: scalar string   ID for dynamic tab
 
Returns:
 dynamic_tab: hash           Dynamic tab item
=cut
sub _tab_template_by_index {
  my $self        = shift;
  my $identifier  = shift;
  
  return $self->_by_index("tab_template", $identifier);

}

=cut _tab_content_by_index()

Returns details on a tab using an index rather then
interating over the array of tab contents

Parameters:
 identifier: scalar string    ID for content

Returns:
 tab_content: hash            Tab content item
=cut
sub _tab_content_by_index {
  my $self        = shift;
  my $identifier  = shift;
 
 return $self->_by_index("tab_content", $identifier);

}

=cut _tab_content_by_index()
# TODO 
Returns details on a tab using an index rather then
interating over the array of tab contents

Parameters:
 identifier: scalar string    ID for content

Returns:
 tab_content: hash            Tab content item
=cut
sub _tab_content_template_by_index {
  my $self        = shift;
  my $identifier  = shift;
 
 return $self->_by_index("tab_content_template", $identifier);

}

=cut _chart_by_index()

Returns details on a chart using an index rather then
interating over the array of charts

Parameters:
 identifier: scalar string    ID for chart
 
Returns:
 chart: hash                  Chart item
=cut
sub _chart_by_index {
  my $self        = shift;
  my $identifier  = shift;
  
  return $self->_by_index("chart", $identifier);

}

=cut _chart_template_by_index()

# TODO Returns details on a chart using an index rather then
interating over the array of charts

Parameters:
 identifier: scalar string    ID for chart
 
Returns:
 chart: hash                  Chart item
=cut
sub _chart_template_by_index {
  my $self        = shift;
  my $identifier  = shift;
  
  return $self->_by_index("chart_template", $identifier);

}


=cut _chart_data_by_index()

# TODO Returns details on a chart data using an index rather then
interating over the array of chart data

Parameters:
 identifier: scalar string    ID for chart data
 
Returns:
 chart_data: hash             Chart data item
=cut
sub _chart_data_template_by_index {
  my $self        = shift;
  my $identifier  = shift;
  
  return $self->_by_index("chart_data_template", $identifier);

}

=cut _chart_data_by_index()

Returns details on a chart data using an index rather then
interating over the array of chart data

Parameters:
 identifier: scalar string    ID for chart data
 
Returns:
 chart_data: hash             Chart data item
=cut
sub _chart_data_by_index {
  my $self        = shift;
  my $identifier  = shift;
  
  return $self->_by_index("chart_data", $identifier);

}



=cut _table_by_index()

Returns details on a table using an index rather then
interating over the array of tables

Parameters:
 identifier: scalar string    ID for table
 
Returns:
 table_data: hash             Table item
=cut
sub _table_by_index {
  my $self        = shift;
  my $identifier  = shift;
  
  return $self->_by_index("table", $identifier);

}


##############################################################################
# # #
# #     Config Updates - Makes updates to the config setting. 
#                        Eg. Adding tabs that can only be defined dynamically.
#
##############################################################################

# TODO add_tab() - add to config, add to index
sub add_tab {
    my $self	          = shift;
    my $tab_id          = shift;
    my $tab_label       = shift;
    my $tab_content_id  = shift;
    
    if ( ! $self->_tab_by_index($tab_id) ) {
      push @{$self->{config}->{tab}}, 
        { tab_id          => $tab_id, 
          tab_label       => $tab_label, 
          tab_content_id  => $tab_content_id
        };
      # rebuild indexes
      $self->_set_index("tab", $INDEXES->{"tab"});
      
      #print Dumper $self->_tab_by_index($tab_id);
      
      return 1;
    }
    
    return 0; 
}


################################################################################
=pod 

B<add_navTab()>

Adds a tab to the list of top level navigation tabs.

Parameters:
 tad_id: scalar string    tab id
 
Returns:  
  1 if succesful else 0

=cut
sub add_navTab {
  my $self	    = shift;
  my $tab_id    = shift;
  
  if ( $tab_id ) {
    foreach my $tab ($self->{config}{nav_tabs}){
      if ( $tab eq $tab_id) {
        qlogprint( {l=>'WARN'}, "Navigation tab already exists for $tab_id! Tab not added.\n");
        return 0;
      }
    }
    push ( @{$self->{config}{nav_tabs}}, $tab_id );
    return 1;
  }
  
  return 0;
}

# For dynamic tabs


################################################################################
=pod 

B<create_dynamicTab()>

creates a tab where content defines tabs.

Parameters:
 dynamic_tab: scalar string     tab id
 original_id: scalar string     tab id
 add_id: scalar string          (Optional) appended value to original_id

Returns:  
  1 if succesful else 0

=cut
sub create_dynamicTab {
  my $self		        = shift;
  my $dynamic_tab_id  = shift;
	my $original_id     = shift;
	my $add_id	        = shift;
  my $new_label       = shift;
  
  #print "\tcreate_dynamicTab($dynamic_tab_id, $original_id, $add_id)  \n";
    
  my $tab_template_id         = $self->dynamicTab_tabTemplateId($dynamic_tab_id);
  my $tab_label               = $self->tabTemplate_tabLabel($tab_template_id);
  if ($new_label) {
    $tab_label = $new_label;
  }
  
  #my $tab_content_template_id = $self->tabTemplate_tabContentTemplateId($tab_template_id);
  
  my $new_tab_id = $self->_new_tab_id($original_id, $add_id);
  #if ( $self->add_tab($new_tab_id, $tab_label, $tab_content_template_id )) {
  if ( $self->add_tab($new_tab_id, $tab_label, $new_tab_id )) {
    $self->add_navTab($new_tab_id);
    
    #print Dumper $self->tab($new_tab_id);
    return $new_tab_id;
  }
  
  return 0;
}


sub _new_tab_id {
  my $self          = shift;
	my $original_id   = shift;
	my $postfix	      = shift;

	my $tab_id	  = $postfix;
	$tab_id =~ s/_(\d)/-$1/g;
	$tab_id =~ s/_(\w)/\U$1\E/g;
	if (defined $tab_id){
    return sprintf ("%s_%s", $original_id, $tab_id);
	}
  return $original_id;
}

sub _new_tab_label {
  my $self      = shift;
	my $original_id   = shift;
	my $postfix	      = shift;
  
	my $tab_label = sprintf ("%s %s", $original_id, $postfix);
	$tab_label =~ s/(_|-)(\d)/ $2/g;
	$tab_label =~ s/([A-Z])/ $1/g;
	$tab_label =~ s/((^|\s)\w)/\U$1\E/g;
	
  return $tab_label;
}


# TODO create_tab() - Not sure what this does yet
sub create_tab_fromTemplate {
  my $self		      = shift;
	my $tab_template_id   = shift;
	my $original_id   = shift;
	my $postfix	      = shift;
  my $tab_label     = shift;
  
  my $new_tab_id = $self->_new_tab_id($original_id, $postfix);
  
  #print "\tCREATE TAB::\t";
  #printf "template:'%s'\t Orig:'%s'\t Adding:'%s'\t", $tab_template_id, $original_id, $postfix;
  #print "NEW TAB -> tab_id: '$new_tab_id'\n";
  #
	#my $tab_config = undef;
	#my $tab_template_id = $self->tabTemplate($tab_template_id);
  
  #print "tab_template_config - $tab_template_id\n";
  
  #my 
  if (! $tab_label) {
    
    $tab_label = $self->tabTemplate_tabLabel($tab_template_id);
    if (! $tab_label) {
      $tab_label = $self->_new_tab_label($original_id, $postfix);
    }
  }
  
  my $tab_content_template_id =  $self->tabTemplate_tabContentTemplateId($tab_template_id);
	  
  #
  #print Dumper $tab_template_config;
  #return 0;
  
  #if ( $self->add_tab($new_tab_id, $tab_label, $tab_content_template_id )) {
  if ( $self->add_tab($new_tab_id, $tab_label, $new_tab_id)) {
    #$self->add_navTab($new_tab_id);
    return $new_tab_id;
  }
  
  return 0
	
  #my $tab_id = $self->_new_tab_id($original_id, $add_id);
	#my $tab_id	  = $add_id;
	#$tab_id =~ s/_(\d)/-$1/g;
	#$tab_id =~ s/_(\w)/\U$1\E/g;
	#if ($tab_id){
  #      $tab_id = sprintf ("%s_%s", $original_id, $tab_id);
	#}else{
  #      $tab_id = $original_id;
	#}
	#my $tab_label = sprintf ("%s %s", $original_id, $add_id);
	#$tab_label =~ s/_|-(\d)/ $1/g;
	#$tab_label =~ s/([A-Z])/ $1/g;
	#$tab_label =~ s/((^)\w)/\U$1\E/g;
 #
 #print "CREATE TAB::\t";
 #printf "template:'%s'\t Orig:'%s'\t Adding:'%s'\t", $template_id, $original_id, $add_id;
 #print "NEW TAB -> tab_id: '$tab_id'\n";
 #	
 #
 #print "tab_label \t$tab_label\n";
  #foreach my $config_item ( keys %{$tab_template_config} ){
  #      if ( $config_item eq "tab_label" and ! $tab_template_config->{$config_item} ) {
  #          $tab_config->{$config_item} = $tab_label;
  #      }
  #      elsif ( $config_item eq "tab_content" ){
  #          $tab_config->{$config_item} = [$tab_id];
  #      }
  #      else{
  #          $tab_config->{$config_item} = $tab_template_config->{$config_item};
  #      }
  #  }
	# 
  #  $self->add_tab($tab_id, $tab_config);
  #  
  #  #foreach my $tab_content_id ( @{$self->{config}{dynamic_tab_template}{$template_id}{tab_content}} ){
  #  #    print "\ttab_content_id: $tab_content_id\n";
  #  #    $self->_create_config_tab_content($node_data, $tab_content_id, $tab_id);
	##}
	# 
  #  if ( $self->tab_details($tab_id) ) {
  #       #print "Created tab: $tab_id\n";
  #       return $tab_id;
	#}
	#
  #;
  
  #return '';
}


################################################################################
=pod 

B<add_tabContent()>

# TODO Adds a tab to the list of top level navigation tabs.

Parameters:
 content_id: scalar string    tab content id
 content_config: hash         tab content details
 
Returns:
  1 if succesful else 0

=cut
sub add_tabContent {
  my $self	            = shift;
  my $content_id        = shift;
  my $content_config    = shift;
    
  if ( $content_id ) {
    
    #print "add_tabContent($content_id, content_config)\n";
    #print Dumper $content_config; #, $self->_tab_content_by_index($content_id);
    
    if ( ! $self->_tab_content_by_index($content_id) ) {
      #print "_tab_content_by_index($content_id)\n";
      push (@{$self->{config}{tab_content}}, {'content_id' => $content_id, %{$content_config}} );
      $self->_set_index("tab_content", $INDEXES->{"tab_content"});
      #print "Going to return 1\n";
      return 1;
    }
    else{
      qlogprint( {l=>'WARN'}, "Tab content already exists for $content_id! Tab content not added.\n");
    }
  }
  print "DID NOT RETURN 1\n";
  return '';
}

# TODO add_chart_data()
sub add_chartData {
    my $self	      = shift;
    my $data_id     = shift;
    my $chart_data  = shift;
    
    #print "add_chartData($data_id)\n";    
    if ( my $chart = $self->_chart_data_by_index($data_id) ) {
        $chart->{data} = $chart_data;
    }
}


=pod
B<add_tabContent()>

# TODO Adds a tab to the list of top level navigation tabs.

Parameters:
 content_id: scalar string    tab content id
 content_config: hash         tab content details
 
Returns:
  1 if succesful else 0

=cut
sub set_tabContent_subTabs {
  my $self	            = shift;
  my $content_id        = shift;
  my $sub_tabs          = shift;
    
  if ( $content_id and $sub_tabs) {
    if ( my $content = $self->_tab_content_by_index($content_id) ) {
      foreach my $item ( @{ $content->{content} } ){
        if ( $self->tabContent_contentItem_isType($item, "sub_tabs" ) ){
          $item->{tabs} = $sub_tabs;
          return 1;
        };
      }
    }
    else{
      qlogprint( {l=>'WARN'}, "Tab content does not exists for $content_id! Could not set sub tabs.\n");
    }
  }
  return 0;
}


# TODO create_config_chart()
################################################################################
=pod 

B<create_chart_fromTemplate()>

Adds a tab to the list of top level navigation tabs.

Parameters:
 config_template_id: scalar string    tab content id
 data_template_id: hash               tab content details
 id: scalar string                    the id to be used by the chart
 
Returns:
  1 if succesful else 0

=cut
sub create_chart_fromTemplate {
    my $self		            = shift;
    my $config_template_id  = shift;
    my $data_template_id    = shift;
    my $id		              = shift;
	 
    #print "Adding Chart $id - using template $config_template_id & $data_template_id\n";
    
    my $chart = clone( $self->chartTemplate($config_template_id) );
    $chart->{$INDEXES->{"chart"}} = $id;
    #delete $chart->{$INDEXES->{"chart_template"}};
    push ( @{$self->{config}{chart}}, $chart );
    $self->_set_index("chart", $INDEXES->{"chart"});
    
    my $chart_data = clone( $self->chartDataTemplate($data_template_id) ); 
    $chart_data->{$INDEXES->{"chart_data"}} = $id;
    #delete $chart->{$INDEXES->{"chart_data_template"}};
    push ( @{$self->{config}{chart_data}}, $chart_data );
    $self->_set_index("chart_data", $INDEXES->{"chart_data"});
    
    #print Dumper $chart;
    #print Dumper $chart_data;
    
    #my $tab_content_chart_templates = $self->tab_template_content($config_template_id, 	"chart_templates");
		#my $chart_templates         = $tab_content_chart_templates->[0]{config};
    #my $chart_data_templates    = $tab_content_chart_templates->[0]{data};
    #$self->{config}{chart}{$id}	        = clone( $self->templated_chart($chart_templates) ); 
    #$self->{config}{chart_data}{$id}    = clone( $self->templated_chart_data($chart_data_templates) );
}

##############################################################################
# # #
# #     Report
#
##############################################################################
=pod 

B<report()>

Returns details on the report type

Parameters:
 attribute: scalar sting    (Optional) report attibute
 
Returns:
  report details: hash    details about the report

=cut
sub report {
  my $self	    = shift;
  my $attribute   = shift;
  
  if ( $attribute ){
    if ( exists $self->{config}{report}{$attribute} ) {
      return $self->{config}{report}{$attribute};
    }
    qlogprint( {l=>'WARN'}, "$attribute was not found in report settings!\n");
  }
  return $self->{config}{report};
}

##############################################################################
# # #
# #     Layout
#
##############################################################################
=pod 

B<layout()>

Returns details on the layout type

Parameters:
 attribute: scalar sting    (Optional) layout attibute
 
Returns:
  report details: hash    details about the layout

=cut
sub layout {
  my $self	    = shift;
  my $attribute   = shift;
  
  if (exists $self->{config}{layout}) {
    if ( $attribute ){
      if ( exists $self->{config}{layout}{$attribute} ) {
        return $self->{config}{layout}{$attribute};
      }
      qlogprint( {l=>'WARN'}, "$attribute was not found in report settings!\n");
    }
    return $self->{config}{layout};
  }
  return '';
  
}



##############################################################################
# # #
# #     Javascript Libraries
#
##############################################################################
=pod 

B<javascript_libraries()>

Returns a list of javascript_libraries of a given category.

Parameters:
 category: scalar sting    (Optional) javascript library category
 
Returns:
  javascript_libraries: array hashes    details about the javascript library.

=cut
sub javascript_libraries {
  my $self	    = shift;
  my $category  = shift;
  my $javascript_libraries = ();
  
  if ( $category ){
    foreach my $library ( @{$self->{config}{javascript_libraries}} ){
      if ( $library->{category} eq $category) {
        push @{$javascript_libraries}, $library;
      }
    }
    return $javascript_libraries;
  }
  return $self->{config}{javascript_libraries};
}

##############################################################################
# # #
# #     Stylesheets
#
##############################################################################
=pod 

B<stylesheets()>

Returns a list of javascript_libraries of a given category.

Parameters:
 category: scalar sting    (Optional) stylesheets category
 
Returns:
  stylesheet: array hashes    details about the stylesheet.

=cut
sub stylesheets {
  my $self	    = shift;
  my $category  = shift;
  my $stylesheets = ();
  
  if ( $category ){
    foreach my $style ( @{$self->{config}{stylesheets}} ){
      if ( $style->{category} eq $category) {
        push @{$stylesheets}, $style;
      }
    }
    return $stylesheets;
  }
  return $self->{config}{stylesheets};
}


##############################################################################
# # #
# #     Tabs
#
##############################################################################

################################################################################
=pod 

B<navTabs()>

Returns a list of navigation tabs

Parameters: None
 
Returns:
 Array: string   list of tabs
=cut
sub navTabs {
  my $self	= shift;
  
  return $self->{config}{nav_tabs};
}


################################################################################
=pod 

B<tab_ids()>

Returns a list of tab ids

Parameters:
 None
 
Returns:
 Array: string   list of tab ids
=cut
sub tab_ids {
  my $self	= shift;
  
  # We want to preserve the ordeing of tabs defined in the config. 
  my @tabs  = ();
  foreach my $tab ( @{$self->{config}{tab}} ){
    push (@tabs, $tab->{tab_id});
  }
  return \@tabs;
}


################################################################################
=pod 

B<tab()>

Returns details of a tab

Parameters:
 tab_id: scalar string    tab id
 
Returns:
 tab : object hash        Details on the tab
=cut
sub tab {
  my $self	    = shift;
  my $tab_id  = shift;
  
  #print "tab($tab_name)\n";
  #print Dumper $self->_tab_by_index($tab_name);
  if ( my $tab = $self->_tab_by_index($tab_id) ) {
    return $tab;  
  }
  return '';
}


# TODO __tab_details()  - Delete
sub __tab_details {
    my $self	  = shift;
    my $tab_id 	= shift;
    
    print "USE tab() instead\n";
    
    return '';
}


################################################################################
=pod 

B<tab_label()>

Returns the label of a tab

Parameters:
 tab_id: scalar string    tab id
 
Returns:
 label: scalar string     Label of a tab

=cut
sub tab_label {
  my $self		= shift;
  my $tab_id 	= shift;
  my $new_label = shift;
  
  if ( my $tab = $self->_tab_by_index($tab_id) ) {
    if ($new_label) {
      print "to set new label as: $new_label\n";
      $tab->{tab_label} = $new_label;
    }
    return $tab->{tab_label};  
  }    
  return '';
}


################################################################################
=pod 

B<tab_contentId()>

Returns a list of content ids of a tab

Parameters:
 tab_id: scalar string    tab id
 
Returns:
 content_ids: array string     Label of a tab

=cut
sub tab_contentId {
  my $self		= shift;
  my $tab_id 	= shift;
  
  #print $tab_id, "\n";
  if ( my $tab = $self->tab($tab_id) ) {
    return $tab->{tab_content_id};  
  }    
  return '';
}



################################################################################
=pod 

B<tab_content()>

Returns the label of a tab

Parameters:
 tab_id: scalar string            tab id

Returns:
 tab content: object hash     Content details

=cut    
sub tabContent {
  my $self	          = shift;
  my $content_id      = shift;
    
  if ( my $tab_content = $self->_tab_content_by_index($content_id) ) {
    return $tab_content;  
  }

  return ''; 
}




################################################################################
=pod 

B<tabContent_pane_label()>

Returns the label of a tab's content

Parameters:
 tab_id: scalar string    tab id
 
Returns:
 label: scalar string     Label of a tab

=cut
sub tabContent_paneLabel {
  my $self		    = shift;
  my $content_id 	= shift;
  my $new_label = shift;
  
  if ( my $tab_content = $self->tabContent($content_id) ) {
    if ($new_label) {
      $tab_content->{pane_label} = $new_label;
    }
    return $tab_content->{pane_label};  
  }    
  return '';
}


################################################################################
=pod 

B<tabContent_xml_source()>

Returns the xml_source of a tab's content

Parameters:
 content_id: scalar string    tab content id
 
Returns:
 xml_source: scalar string     xml_source of a tab

=cut
sub tabContent_xmlSource {
  my $self		    = shift;
  my $content_id 	= shift;
  
  if ( my $tab_content = $self->tabContent($content_id) ) {
    return $tab_content->{xml_source};  
  }    
  return '';
}


################################################################################
=pod 

# TODO B<tabContent_content_hasType()>

Returns the if a tab_content has a type of content

Parameters:
 content_id: scalar string       tab content id
 content_type: scalar string     type of content
 
Returns:
 scalar Boolean       if tab content has type

=cut
sub tabContent_content_hasType {
  my $self	        = shift;
  my $tab_id        = shift;
  my $content_type  = shift;
  
  foreach my $item ( @{$self->tabContent_content_items($tab_id)} ){
    return 1 if $self->tabContent_contentItem_isType($item, $content_type);
  }

  #return '';
}


################################################################################
=pod 

B<tabContent_content_items()>

Returns a list of contents for a tab_content

Parameters:
 content_id: scalar string       tab content id
 
Returns:
 types: array hashes       tab contents items

=cut
sub tabContent_content_items {
    my $self		    = shift;
    my $content_id 	= shift;
    
    #print "tabContent_content_items - $content_id\n";
    if ( my $tab_content = $self->tabContent($content_id) ) {
      return $tab_content->{content};
    }
    return [];
}

################################################################################
=pod 

# TODO B<tabContent_content_ofType()>

Returns the type of content; chart, sub tabs, etc

Parameters:
 contentItem: hash       content details
 
Returns:
 types: scalar string       tab content type 

=cut
sub tabContent_content_items_ofType {
  my $self		      = shift;
  my $tab_id 	      = shift;
  my $content_type  = shift;
  my $content       = ();
  
  foreach my $item ( @{$self->tabContent_content_items($tab_id)} ){
    #print Dumper $item;
    push (@{$content}, $item) if $self->tabContent_contentItem_isType($item, $content_type);
  }
  
  #print Dumper $content;
  
  return $content if defined $content;
  return [];
}





################################################################################
=cut _tabContent_contentItem_value()

Returns the value for  for a content item

Parameters:
 contentItem: hash        content item details
 key: scalar string       the key for the value
 
Returns:
 value: ?       
=cut
sub _tabContent_contentItem_value {
  my $self		      = shift;
  my $contentItem 	= shift;
  my $key           = shift;
  
  
  #
  return $contentItem->{$key} if exists $contentItem->{$key};
  #print "_tabContent_contentItem_value(contentItem, $key)\n";
  #print Dumper $contentItem->[0];
  return "";
}

sub tabContent_contentItem_valueOf {
  my $self		      = shift;
  my $contentItem 	= shift;
  my $key           = shift;
  
  return $self->_tabContent_contentItem_value($contentItem, $key);
}



################################################################################
=pod 

B<tabContent_contentItem_type()>

Returns the type of content; chart, sub tabs, etc

Parameters:
 contentItem: hash       content details
 
Returns:
 types: scalar string       tab content type 

=cut
sub tabContent_contentItem_type {
  my $self		      = shift;
  my $contentItem 	= shift;
  
  my $value = $self->_tabContent_contentItem_value($contentItem, "type");
  return $value if defined $value;
  return '';
}



################################################################################
=pod 

# TODO B<tabContent_content_isType()>

Returns the if a tab_content item is a type of content

Parameters:
 content_id: scalar string       tab content id
 content_type: scalar string     type of content
 
Returns:
 scalar Boolean       if tab content is the type

=cut
sub tabContent_contentItem_isType {
    my $self	        = shift;
    my $content_item  = shift;
    my $content_type  = shift;
    
    return 1 if $content_type eq $self->tabContent_contentItem_type($content_item); 
    return 0;
}




################################################################################
=pod 

B<tabContent_contentItem_chartId()>

Returns the chart id for a content item with type chart

Parameters:
 contentItem: hash       content item details
 
Returns:
 chart_id: scalar string       chart  id

=cut
sub tabContent_contentItem_chartId {
  my $self		      = shift;
  my $contentItem 	= shift;  
  
  my $value = $self->_tabContent_contentItem_value($contentItem, "chart_id");
  return $value if defined $value;
  
  $self->_error_tabContent_contentItemType($contentItem, "chart");
  return '';
}

################################################################################
=pod 

B<tabContent_contentItem_chartDataId()>

Returns the chart id for a content item with type chart

Parameters:
 contentItem: hash       content item details
 
Returns:
 chart_data_id: scalar string       chart data id

=cut
sub tabContent_contentItem_chartDataId {
  my $self		      = shift;
  my $contentItem 	= shift;
    
  my $value = $self->_tabContent_contentItem_value($contentItem, "chart_data_id");
  return $value if defined $value;

  $self->_error_tabContent_contentItemType($contentItem, "chart");
  return '';
}


################################################################################
=pod 

B<tabContent_contentItem_tableId()>

Returns the table id for a content item with type table

Parameters:
 contentItem: hash       content item details
 
Returns:
 table_id: scalar string       table id

=cut
sub tabContent_contentItem_tableId {
  my $self		      = shift;
  my $contentItem   = shift;
    
  my $value = $self->_tabContent_contentItem_value($contentItem, "table_id");
  return $value if defined $value;

  $self->_error_tabContent_contentItemType($contentItem, "table");
  return '';
}


################################################################################
=pod 

# TODO B<tabContent_contentItem_tabs()>

Returns the tabs for a content item with type sub_tabs

Parameters:
 contentItem: hash       content item details
 
Returns:
 tabs: array string       list of tab ids

=cut
sub tabContent_contentItem_tabs {
  my $self		      = shift;
  my $contentItem 	= shift;
  
  #print Dumper $contentItem;
  my $value = $self->_tabContent_contentItem_value($contentItem, "tabs");
  #print Dumper $value;
  return $value if defined $value;
  
  $self->_error_tabContent_contentItemType($contentItem, "sub_tabs");
  return '';
    
  #return $contentItem->{tabs} if exists $contentItem->{tabs};
  #return [];
}


################################################################################
=pod 

# TODO B<tabContent_contentItem_tabs()>

Returns the tabs for a content item with type sub_tabs

Parameters:
 contentItem: hash       content item details
 
Returns:
 tabs: array string       list of tab ids

=cut
sub tabContent_contentItem_templateTabs {
  my $self		      = shift;
  my $contentItem 	= shift;
    
  return $contentItem->{template_tabs} if exists $contentItem->{template_tabs};
  return [];
}




#################################################################################
=pod 

 B<tabContent_templateId()>

Returns the tab content template id content item

Parameters:
 contentItem: hash       content item details
 
Returns:
 tab_content_template_id: scalar string       tab content template id

=cut
sub tabContent_ContentItem_templateId {
  my $self		      = shift;
  my $contentItem   = shift;
  
  my $value = $self->_tabContent_contentItem_value($contentItem, "tab_content_template_id");
  return $value if defined $value;

  $self->_error_tabContent_contentItemType($contentItem, "tab_content_template");
  return '';
}


# TODO tab_content_template_id
# TODO chart_template_id
# TODO chart_template_data_id
# TODO dynamic_tabs

#################################################################################
=pod 

 B<tabContent_ContentItem_chartTemplateId()>

Returns the chart template id for the content item

Parameters:
 contentItem: hash       content item details
 
Returns:
 chart_template_id: scalar string       chart template id

=cut
sub tabContent_ContentItem_chartTemplateId {
  my $self		      = shift;
  my $contentItem   = shift;
  
  my $value = $self->_tabContent_contentItem_value($contentItem, "chart_template_id");
  return $value if defined $value;

  $self->_error_tabContent_contentItemType($contentItem, "chart_templates");
  return '';
}


#################################################################################
=pod 

 B<tabContent_ContentItem_chartdataTemplateId()>

Returns the chart data template id for the content item

Parameters:
 contentItem: hash       content item details
 
Returns:
 chart_data_template_id: scalar string       chart data template id

=cut
sub tabContent_ContentItem_chartdataTemplateId {
  my $self		      = shift;
  my $contentItem   = shift;
  
  my $value = $self->_tabContent_contentItem_value($contentItem, "chart_data_template_id");
  return $value if defined $value;

  $self->_error_tabContent_contentItemType($contentItem, "chart_templates");
  return '';
}


#################################################################################
=pod 

 B<tabContent_ContentItem_dynamicTabId()>

Returns the dynamic tab id for the content item

Parameters:
 contentItem: hash       content item details
 
Returns:
 chart_data_template_id: scalar string       chart data template id

=cut
sub tabContent_ContentItem_dynamicTabId {
  my $self		      = shift;
  my $contentItem   = shift;
  
  my $value = $self->_tabContent_contentItem_value($contentItem, "dynamic_tab_id");
  return $value if defined $value;

  $self->_error_tabContent_contentItemType($contentItem, "dynamic_sub_tabs");
  return '';
}


=cut _error_tabContent_contentItemType()
 
 Reports if the the content item has a given content type.
 If the content type does not exist a warning message is logged. 

Parameters:
 contentItem: hash               content item details
 content_type: scalar string     type of content
 
Returns:
 boolean: scalar number          returns 1 if content type does not exist.

=cut 
sub _error_tabContent_contentItemType {
  my $self		      = shift;
  my $contentItem 	= shift;
  my $content_type  = shift;
  
  if (! $self->tabContent_content_hasType($contentItem, $content_type)){
    qlogprint( {l=>'WARN'}, "Content Item is not of type $content_type but of type".$self->tabContent_contentItem_type($contentItem)."\n");  
    return 1;
  }
}






sub tabContentTemplate {
  my $self	          = shift;
  my $template_id      = shift;
    
  if ( my $tab_content_template = $self->_tab_content_template_by_index($template_id) ) {
    return $tab_content_template;  
  }

  return ''; 
}

################################################################################
=pod 
# TODO 
B<tabContent_pane_label()>

Returns the label of a tab's content

Parameters:
 tab_id: scalar string    tab id
 
Returns:
 label: scalar string     Label of a tab

=cut
sub tabContentTemplate_paneLabel {
  my $self		    = shift;
  my $template_id 	= shift;
  
  if ( my $tab_content_template = $self->tabContentTemplate($template_id) ) {
    return $tab_content_template->{pane_label};  
  }    
  return '';
}


################################################################################
=pod 
# TODO 
B<tabContent_xml_source()>

Returns the xml_source of a tab's content

Parameters:
 content_id: scalar string    tab content id
 
Returns:
 xml_source: scalar string     xml_source of a tab

=cut
sub tabContentTemplate_xmlSource {
  my $self		    = shift;
  my $template_id 	= shift;
  
  if ( my $tab_content_template = $self->tabContentTemplate($template_id) ) {
    return $tab_content_template->{xml_source};  
  }    
  return '';
}

################################################################################
=pod 
# TODO 
B<tabContent_xml_source()>

Returns the xml_source of a tab's content

Parameters:
 content_id: scalar string    tab content id
 
Returns:
 xml_source: scalar string     xml_source of a tab

=cut
sub tabContentTemplate_content {
  my $self		    = shift;
  my $template_id 	= shift;
  
  if ( my $tab_content_template = $self->tabContentTemplate($template_id) ) {
    return $tab_content_template->{content};  
  }    
  return '';
}




# TODO contentTemplate()
sub contentTemplate {
    my $self	= shift;
    my $content_id     = shift;
    
    if ( $self->has_tabContent_type($content_id, "tab_content_template") ) {
         return $self->tabContent_content_type($content_id, "tab_content_template");
    }
    return ''; 
}


# TODO chartTemplate()
sub __chartTemplate {
    my $self	= shift;
    my $tab_id     = shift;
    
    if ( $self->has_tabContent_type($tab_id, "chart_templates") ) {
         return $self->{config}{tab_content}{$tab_id}{content}{"chart_templates"};
    }
    
    return ''; 
}

##############################################################################
# # #
# #      Content
#
##############################################################################
sub __content_subTabs {
  my $self	= shift;
  my $content_id     = shift;
  
  if ( my $tabContent = $self->tabContent_contentByType($content_id, "sub_tabs") ) {
    # body...
  }
  return '';
}

sub __content_dynamicSubTabs {
  my $self	= shift;
  my $content_id     = shift;

  if ( my $tabContent = $self->tabContent_contentByType($content_id, "dynamic_sub_tabs") ) {
    # body...
  }
  return '';
}

# TODO pod content_tables
sub content_table {
  my $self	      = shift;
  my $table_id  = shift;
  
  #foreach my $table (){
  #  
  #}
  #if ( my $tabContent = $self->tabContent_contentByType($content_id, "tables") ) {
  #  # body...
  #}
  #return '';
}

sub content_charts {
  my $self	= shift;
  my $content_id     = shift;

  if ( my $tabContent = $self->tabContent_contentByType($content_id, "charts") ) {
    # body...
  }
  return '';
}

sub content_tabTemplate {
  my $self	= shift;
  my $content_id     = shift;

  if ( my $tabContent = $self->tabContent_contentByType($content_id, "tab_content_template") ) {
    # body...
  }
  return '';
}

sub content_chartTemplates {
  my $self	= shift;
  my $content_id     = shift;

  if ( my $tabContent = $self->tabContent_contentByType($content_id, "chart_templates") ) {
    # body...
  }
  return '';
}

##############################################################################
# # #
# #      Charts
#
##############################################################################

################################################################################
=pod 

B<chart()>

Returns details on a chart

Parameters:
 chart_id: scalar string       chart id
 
Returns:
 chart: hash object           chart details

=cut
sub chart {
    my $self	    = shift;
    my $chart_id  = shift;
    
    if ( my $chart = $self->_chart_by_index($chart_id)) {
        return $chart;
    }
    
    return undef;
}

################################################################################
=pod 

B<chart_data()>

Returns details on a chart

Parameters:
 data_id: scalar string       chart data id
 
Returns:
 chart_data: hash object           chart data

=cut
sub chart_data{
    my $self	    = shift;
    my $data_id   = shift;
    
    if ( my $chart_data = $self->_chart_data_by_index($data_id)) {
        return $chart_data;
    }

    return undef;
}


sub chart_data_isFormat{
    my $self	    = shift;
    my $data_id   = shift;
    my $format    = shift;
    
    if ( my $chart_data = $self->_chart_data_by_index($data_id) ) {
      return 1 if $format eq $chart_data->{chart_data_format}; 
    }
    return 0;
}


# TODO tab_charts()
sub tab_charts {
  my $self	        	= shift;
  my $tab_id          = shift;
  my $tab_template    = shift;
  my $charts          = ();
  
  #print Dumper $self->{config}{tab_content};
  #print Dumper $self->{config}{tab_content_template};
  #print "tab_id-$tab_id, tab_template-"; 
  #print $tab_template if defined $tab_template;
  #print "\n";
  
      
  if ( $tab_template ) {
    #print "\tTC == tab_charts $tab - template $tab_template\n";
    #print Dumper $self->{config}{tab_content_template}{$tab_template};
    if ( exists $self->{config}{tab_content_template}{$tab_template}{content}{"chart_templates"} ) {
      $charts = $self->tab_template_content($tab_template, "chart_templates");
      #return $tab_template_charts
      #foreach my $chart ( @{$tab_template_charts} ){
      #    #print "\tTC == Chart Templates\n";
      #    print Dumper $charts;
      #    
      #    push ( @{$charts}, { 
      #        config  => $self->templated_chart($chart->{config}),
      #        data    =>$self->templated_chart_data($chart->{data})
      #        });
      #}           
    }
  }
  #elsif( $self->tabContent_content_hasType($tab_id, "chart_templates") ){
  #    print "\tTC == Charts - template\n";
  #    $charts = $self->tabContent_content_items_ofType($tab_id, "chart_templates");
  #    
  #}
  #elsif( $self->has_tabContent_content_type($tab_id, "charts") ){
  #    print "\tTC == Charts \n";
  #    
  #    $charts = $self->tab_content($tab_id, "charts");
  #    
  #}
  else{
    #print "TC == $tab_id - Chart\n";
    foreach my $item ( @{$self->tabContent_content_items($tab_id)} ){
      push (@{$charts}, $item) if $self->tabContent_contentItem_isType($item, "chart");
    }
  }
  
  return $charts;
}

################################################################################
=pod 

B<table()>

Returns details on a table

Parameters:
 table_id: scalar string       table id
 
Returns:
 table: hash object           table details

=cut
sub table {
    my $self	    = shift;
    my $table_id  = shift;
    
    if ( my $table = $self->_table_by_index($table_id)) {
        return $table;
    }
    
    return undef;
}


##############################################################################
# # #
# #      Templated and Dynmaic tabs
#
##############################################################################
# TODO tab_template_details()
sub __tabTemplate_details {
    my $self	= shift;
    my $tab_id     = shift;

    if ( my $details = $self->_tab_template_by_index($tab_id) ) {
        return $details;  
    }

    return ''; 
}


################################################################################
=pod 

B<is_navTab()>

Returns if a dynamic tab should be added as a navigation tab.

Parameters:
 tab_id: scalar string       tab id
 
Returns:
 Boolean: scalar number           chart data

=cut
sub is_navTab {
  my $self	  = shift;
  my $tab_id  = shift;
    
  if ( my $dynamic_tab = $self->_dynamic_tab_by_index($tab_id) ) {
    return $dynamic_tab->{nav_tab};
  }
  
  return 0;
}

# TODO tab_template_content()
sub tabTemplate_content {
    my $self		= shift;
    my $tab_id 	= shift;
    my $content_type    = shift;
        
    if ( exists $self->{config}{tab_content_template}{$tab_id} ) {
        if ( $content_type ) {
            if ( exists $self->{config}{tab_content_template}{$tab_id}{content}{$content_type} ) {
               return $self->{config}{tab_content_template}{$tab_id}{content}{$content_type};
            }
        }else{
            return $self->{config}{tab_content_template}{$tab_id}{content};  
        }
    }
    
    return ''; 
}


################################################################################
=pod 

B<dynamic_tabs()>

Returns the a list of dynamic tab

Parameters:
 None
 
Returns:
 dynamic_tabs: array string     list of dynamic tabs

=cut
sub dynamicTabs {
  my $self	= shift;
  
  if ( defined $self->{config}{dynamic_tab} ) {
    my @dynamic_tabs = ();
    foreach my $dynamic_tab ( @{$self->{config}{dynamic_tab}} ){
      push (@dynamic_tabs, $dynamic_tab->{dynamic_tab_id});
    }
    return \@dynamic_tabs;
  }
  return 0;
}


################################################################################
=pod 

B<dynamicTab_xmlSource()>

Returns the xml source for a dynamic tab

Parameters:
 tab_id: scalar string       tab id
 
Returns:
 xml_source: scalar string     xml source

=cut
sub dynamicTab_xmlSource {
    my $self		= shift;
    my $tab_id 	= shift;
    
    if (  my $dynamic_tab = $self->_dynamic_tab_by_index($tab_id) ) {
        return $dynamic_tab->{xml_source};
    }
    return 0;
}

################################################################################
=pod 

B<dynamicTab_tabTemplateId()>

Returns the tab template id for a dynamic tab

Parameters:
 tab_id: scalar string       tab id
 
Returns:
 tab_template_id: scalar string     tab template id

=cut
sub dynamicTab_tabTemplateId {
    my $self		= shift;
    my $tab_id 	= shift;
    
    if (  my $dynamic_tab = $self->_dynamic_tab_by_index($tab_id) ) {
        return $dynamic_tab->{tab_template_id};
    }
    return 0;
}



# TODO templated_tab_xml_source()
sub templated_tab_xml_source {
    my $self		= shift;
    my $tab_id 	= shift;
    
    if ( exists $self->{config}{tab_content_template}{$tab_id} ){
        return $self->{config}{tab_content_template}{$tab_id}{xml_source};  
    
    }elsif ( exists $self->{config}{tab_content}{$tab_id} ){
        return $self->{config}{tab_content}{$tab_id}{xml_source};
        
    }
    return '';
}


sub tabTemplate {
  my $self		= shift;
  my $tab_id 	= shift;
  
  if (  my $tab_template = $self->_tab_template_by_index($tab_id) ) { 
      return $tab_template;
  }
  return '';
}

# TODO templated_tab_details()
sub tabTemplate_tabLabel {
  my $self		= shift;
  my $tab_id 	= shift;
  
  if (  my $tab_template = $self->_tab_template_by_index($tab_id) ) { 
      return $tab_template->{tab_label};
  }
  elsif ( my $tab_id = $self->tab($tab_id) ){
    print "Wait! whats this? tabTemplate_tabLabel\n";
    #return $self->{config}{tab}{$tab_id};
  }
  print "No templated_tab_details $tab_id\n";
  #print Dumper $self->{config};
  return '';
}

# TODO templated_tab_details()
sub tabTemplate_tabContentTemplateId {
  my $self		= shift;
  my $tab_id 	= shift;
  
  if (  my $tab_template = $self->_tab_template_by_index($tab_id) ) {
      return $tab_template->{tab_content_template_id};
  }
  elsif ( my $tab_id = $self->tab($tab_id) ){
    print "Wait! whats this? tabTemplate_tabContentTemplateId\n";
    #return $self->{config}{tab}{$tab_id};
  }
  print "No templated_tab_details $tab_id\n";
  #print Dumper $self->{config};
  return '';
}


#sub templated_tab_charts {
#    my $self	= shift;
#    my $tab 	= shift;
#    
#    if ( exists $self->{config}{tab_content_template}{$tab} ) {
#        return $self->{config}{tab_content_template}{$tab}{xml_source};  
#    }
#    return '';
#}

# TODO chartTemplate()
sub chartTemplate {
  my $self	        = shift;
  my $template_id 	= shift;
  
  if ( my $chart_content = $self->_chart_template_by_index($template_id) ) {
    return $chart_content;  
  }
  return '';
}

# TODO chartDataTemplate()
sub chartDataTemplate {
  my $self	= shift;
  my $template_id 	= shift;
  
  if ( my $tab_content = $self->_chart_data_template_by_index($template_id) ) {
    return $tab_content;  
  }
  return '';
}





 

sub ___config_tabContent {
    my $self            = shift;
    my $tab_id             = shift;
    my $content_info    = undef;
    
    if ( exists $self->{config}{tab_content}{$tab_id} ) {
        $content_info = $self->{config}{tab_content}{$tab_id}{content};
        # if content uses a template
        if ( exists $content_info->{tab_content_template} ) {
            my $tab_template = $content_info->{tab_content_template};
            $content_info = $self->{config}{tab_content_template}{$tab_template};
        }
    }
    
    return $content_info;
}

sub ___config_chart_data {
    my $self       = shift;
    my $tab_id        = shift;
    
    my $content_info = $self->config_tabContent($tab_id);
    if ( exists $content_info->{content}{chart_data} ) {
        
    }
}

sub ___config_chart_id {
    # body...
}

# TODO nodes()
sub ___nodes {
    my $self        = shift;
    my $tab_id         = shift;
    my $XMLsource   = $self->config_tabContent()->{xml_source};
    my $xml_root    = $self->xmlroot();
    
    $self->xmlpath_add($XMLsource);
    
    if ( $xml_root->exists( $self->xpath() ) ) {        
        my $root_node = $xml_root->find( $self->xpath() ); 
            
    }
    
    
    if ($XMLsource) {
        $self->xmlpath_drop();
    }
    
    return '';
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
