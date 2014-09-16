package QCMG::Visualise::HtmlReport;

##############################################################################
#
#  Module:   QCMG::Visualise::HtmlReport.pm
#  Author:   Matthew J Anderson
#  Created:  2013-06-26
#
#  $Id: HtmlReport.pm 4692 2014-08-26 05:13:43Z m.anderson $
#
##############################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use File::Basename;
use Data::Dumper;
use XML::LibXML;
use Clone qw( clone );		 # From CPAN 
use JSON qw( encode_json );	 # From CPAN

#use QCMG::IO::JsonReader;
use QCMG::Google::DataTable;
use QCMG::Util::QLog;
use QCMG::Util::XML qw( get_attr_by_name get_node_by_name );
use QCMG::Visualise::Coverage;
use QCMG::Visualise::VisualiseConfig;
use QCMG::HTML::BootstrapPage;


use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4692 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: HtmlReport.pm 4692 2014-08-26 05:13:43Z m.anderson $'
	 =~ /\$Id:\s+(.*)\s+/;
	 

# TODO pod new	
sub new {
    my $class  = shift;
    my %params = @_;
    
    die "You must supply one of file/xmltext/xmlnode to new()"
        unless (exists $params{file} or
                exists $params{xmltext} or
                exists $params{xmlnode});

    my $self = {file	      => $params{file} || '',	 # Report source
                xmltext		  => '',
                xmlnode		  => undef,
                xmlroot		  => undef,
                xmlpath		  => [],
                config      => QCMG::Visualise::VisualiseConfig->new( json => $params{configfile} ),	# Parsed JSON config.
                page        => undef,
                verbose		  => $params{verbose} || 0 };
	 bless $self, $class;
	 
	 
	 $self->{page} = QCMG::HTML::BootstrapPage->new(
									 file	=> $params{file},	  
									 config  => $self->{config}   
								 );	
	 
	 # Ultimately we need a XML::LibXML::Element but we could have been
	 # passed an Element object, a filename or a text blob.  In the latter
	 # two cases, we need to create an XML node from the file or text.

	 if (exists $params{xmlnode}) {
		 #print "xmlnode\n";
		 my $type = ref($params{xmlnode});
		 die "xmlnode parameter must refer to a XML::LibXML::Element object not [$type]"
			 unless ($type eq 'XML::LibXML::Element');
		 $self->{xmlnode} = $params{xmlnode};
	 }
	 elsif (exists $params{xmltext}) {
		 #print "xmltext\n";
		 my $xmlnode = undef;
		 eval { $xmlnode = XML::LibXML->load_xml( string => $params{xmltext} ); };
		 die $@ if $@;
		 $self->{xmlnode} = $xmlnode;
	 }
	 elsif (exists $params{file}) {
		 #print "file\n";
		 my $xmlnode = undef;
		 eval { $xmlnode = XML::LibXML->load_xml( location => $params{file} ); };
		 die $@ if $@;
		 $self->{xmlnode} = $xmlnode;
	 }
	 else {
		 die "Uh oh - should not be able to get here!";
	 }
	 
 
	my $xml_root = $self->{config}->report("xml_root");
	my $xml_root_found = 0;
	#if ($self->{xmlnode}->hasChildNodes) {
	#	my @nodes = $self->{xmlnode}->childNodes;
	#	foreach my $node (@nodes) {
	#		printf "Looking for XML Root Node: %s \tfound: %s\n", $xml_root, $node->nodeName;
			if ($self->{xmlnode}->nodeName =~ /$xml_root/) {
                $self->xmlpath_add($self->{xmlnode}->nodeName);
				#$self->xmlroot($self->{xmlnode}->nodeName);
	            $xml_root_found += 1;
            }
	#	}
	  
	    #	# If we get this far then we found no matches so die
		die "File $params{infile} does not contain $xml_root XML"
			unless $xml_root_found; 
	#}
	#else {
	#	die "XML doc appears to have no child nodes ???";
	#}
	 
	 return $self;
}


# Class Data Object functions
# TODO pod file	
sub file {
	 my $self = shift;
	 return $self->{file};
}

# TODO pod xmlnode	
sub xmlnode {
	 my $self = shift;
	 return $self->{xmlnode};
}

# TODO pod verbose	
sub verbose {
	 my $self = shift;
	 return $self->{verbose};
}

# TODO pod process_tab	
sub html {
	 my $self = shift;
   print "\nG E N E R A T I N G    H T M L\n";
   #print "\nnav tabs\n";
   #my $nav_tabs = $self->{config}->nav_tabs();
   #
   #for my $nav_tab ( @{$nav_tabs} ){
   #    my $nav_tab_details = $self->{config}->tab_details($nav_tab);
   #    print "\tnav tab: $nav_tab\n";
   #    #print Dumper $nav_tab_details;
   #    if ($nav_tab_details) {
   #        printf "\t\ttab_label: %s\t\ttab_content: %s\n", 
   #                $nav_tab_details->{tab_label},  
   #                Dumper $nav_tab_details->{tab_content};
   #    }
   #}
   #print "\n STARTS NOW\n\n";

   #print "\nNav Tabs\n";
   #print Dumper $self->{config}->navTabs();
   
   print "\nTabs\n";
   foreach my $tab ( @{$self->{config}->tab_ids()} ){
     print "\t$tab -- ",$self->{config}->tab_contentId($tab)," \n";
     #print Dumper $self->{config}->tab($tab);
     #print Dumper $self->{config}->tabContent($self->{config}->tab_contentId($tab));
   }
   
   
   
   #foreach my $content ( @{$self->{config}{config}{tab_content}} ){
     #print "\t",$content->{content_id},"\n";
     #print Dumper $content->{content_id};
   #  #print Dumper $self->{config}->tab($tab);
     #print Dumper $self->{config}->tabContent($self->{config}->tab_contentId($tab));
     #}
   #print Dumper $self->{config}{config}{tab_content}; 
     
  #print "tab_contents:\n";
  #print Dumper $self->{page}->tab_contents();
  return $self->{page}->html();
}


# ADD new reports here!
# TODO pod process_tab	
sub process {
	 my $self = shift;
	 my $report_type = $self->{config}->report("type");
	 
   qlogprint {l=>'INFO'}, "Report Type: $report_type\n";
	 if ($report_type eq "qCoverage") {
		 $self->process_report_coverage();
	 }
   elsif ($report_type eq "qProfiler") {
		 $self->process_report_profiler();
	 }
   elsif ($report_type eq "qSignature") {
		 $self->process_report_signature();
	 }
   elsif ($report_type eq "qlibcompare") {
     $self->process_report_libcompare();
   }
   
   else{
     qlogprint {l=>'WARN'}, "Can not process unknown report type: $report_type\n";
   }
    
}

#
# Report Specific
#

# TODO pod process_report_coverage
sub process_report_coverage {
	my $self = shift;
		
	# Process non dynamic tabs first. So we don't reprocess the reports or the wrong elements. 	 
	foreach my $nav_tab ( @{ $self->{config}->navTabs() } ){
		if ( my $tab = $self->{config}->tab($nav_tab) ) {
			#print Dumper $tab;
      my $tab = $nav_tab;
			my $XMLsource = $self->{config}->tabContent_xmlSource($tab); 
		 
			# Add Tabs with XMLsource here
			if ( $XMLsource ){
				print "This was not yet expected. Tab $tab has XMLsource $XMLsource\n";
			} 

			# Add Tabs without XMLsource here
			else{
				if ( $tab eq "coverage_summary" ) {
					# Do nothing until all other contents are generated!
					printf "Setting summary tab %s\n", $tab;
					$self->{page}->add_summary_tab($tab);
        }
        #else{
      	#   print "?? nav_tab $nav_tab - $tab\n";
      	# }
			}	   
			    
			$self->_tab_content_logic($nav_tab);   
		 }
    
	 }
   	 
	 if ( $self->{config}->dynamicTabs() ){
		 print "Oooh dynamic_tabs\n";
		 $self->process_dynamic_tabs();
	 }	
	 
   # Debugging
   #print "Processed tabs:\n";
   #foreach my $tabid ( @{$self->{config}->tab_ids() } ){
   #  my $tab_contentId = $self->{config}->tab_contentId($tabid);
   #  printf " -- 'tab_id' => '%s',\t'tab_content_id' => '%s'\n", $tabid, $tab_contentId;
   #  print Dumper $self->{config}->tabContent_content_items($tab_contentId);
   #}
   #print "Don't create coverage summary process_report_coverage\n";
   #exit;
   
	 # Add Summary tabs Here
	 my $tab = $self->{page}->summary_tab();
	 if ( $tab eq "coverage_summary") {
		 print "Oooh coverage_summary\n";
		 $self->_coverage_summary($tab);
	 }
	 
}

# TODO pod process_report_profiler
sub process_report_profiler {
	 my $self = shift;
	 my $xml_root = $self->{xmlnode};
	 
	 if ( $xml_root->exists( 'BAMReport' ) ) {         
         $self->xmlpath_add('BAMReport');
	 }
	 print "Processing profiler\n";

	 # Process non dynamic tabs first. 
     # So we don't reprocess the reports or the wrong elements. 
	 foreach my $nav_tab ( @{ $self->{config}->navTabs() } ){
		 if ( $self->{config}->tab($nav_tab) ) {
			 $self->_tab_content_logic($nav_tab);   
		 }
	 }
	 
	 if ( $self->{config}->dynamicTabs() ){
		 $self->process_dynamic_tabs();
	 } 	 
	 
	 # Add Summary tabs Here
	 my $tab = $self->{page}->summary_tab();
	 
     #print "\nprocess_report_profiler -- tab contents dump\n";
     #print Dumper keys %{$self->{page}->tab_contents()};
     
}

# TODO pod process_report_signature
sub process_report_signature {
	 my $self = shift;
	 my $xml_root = $self->{xmlnode};
	 
	 #if ( $xml_root->exists( 'BAMReport' ) ) {         
   #      $self->xmlpath_add('BAMReport');
	 #}
	 
	 print "Processing signature\n";
	 
	 # Process non dynamic tabs first. 
   # So we don't reprocess the reports or the wrong elements. 
	 foreach my $nav_tab ( @{ $self->{config}->navTabs() } ){
		 if ( $self->{config}->tab($nav_tab) ) {
			 $self->_tab_content_logic($nav_tab);   
		 }
	 }
	 
	 #if ( $self->{config}->dynamic_tabs() ){
	 #	 $self->process_dynamic_tabs();
	 #} 	
	 
	 
}

# TODO pod process_report_libcompare
sub process_report_libcompare {
  my $self = shift;
  my $xml_root = $self->{xmlnode};
 
  #if ( $xml_root->exists( 'BAMReport' ) ) {         
  #  $self->xmlpath_add('BAMReport');
  #}
  print "Processing libcompare\n";
  # Process non dynamic tabs first. 
  # So we don't reprocess the reports or the wrong elements. 
  foreach my $nav_tab ( @{ $self->{config}->navTabs() } ){
    if ( $self->{config}->tab($nav_tab) ) {
      print "nav_tab $nav_tab\n";
      
      if ($nav_tab eq "metrix_summary") {
        # Do nothing until all other contents are generated!
        printf "Setting summary tab %s\n", $nav_tab;
        $self->{page}->add_summary_tab($nav_tab);
      }
      else{
        $self->_tab_content_logic($nav_tab);
      }
    }
  }
  
  if ( $self->{config}->dynamicTabs() ){
    print "Oooh dynamic_tabs\n";
    $self->process_dynamic_tabs();
  }
 
  # Add Summary tabs Here
  my $tab = $self->{page}->summary_tab();
  if ( $tab eq "metrix_summary") {
    print "Oooh metrix_summary\n";
    my $XMLsource = $self->{config}->tabContent_xmlSource($tab) ; 
    $self->_metrix_summary($XMLsource, $tab);
  }
 
}



# ADD new tabs here!
# TODO pod process_tab	
sub process_tab {
    my $self		      = shift;
    my $node_data     = shift; 
    my $tab_id	      = shift;
    my $template_id   = shift;
       
    my $content_info  = $self->{config}->tabContent($tab_id);
    my $action	      = $tab_id;
    my $chart_ids     = undef;
    
    		
    # Use Templates if created from dynamic tabs 
    if ( defined $template_id) {
        $content_info = $self->{config}->tabContentTemplate($template_id);
        $action	   = $template_id;
        
        #print "\t\tprocess_tab(node_data, $tab_id, $template_id)\n";
    }else{
      #print "\t\tprocess_tab(node_data, $tab_id)\n";
    }
		
    #print "\t\t--process_tab() with this tabContent:\n";
    #print Dumper $content_info;
    
		qlogprint {l=>'TAB'}, "Procesing $tab_id with action $action\n"
			if $self->{verbose};
		
    
    
    # Templates  
    my $tally_actions = [ 'cycle_tally',  'read_cycle_tally',
                          'length_tally', 'read_length_tally',
                          'value_tally',  'read_value_tally',
                          'range_tally',  'range_tally_isize0to5000', 'range_tally_isize0to50000'
                          ];
    my %tallies = map { $_ => 1 } @{$tally_actions};

    # Coverage
		if ( $action eq "coverageBinned" ) {
        $chart_ids = $self->_coverage($node_data, $tab_id);
    }
    elsif ( $action eq "cumulativeCoverage") {
        $chart_ids = $self->_cumulative_coverage($node_data, $tab_id);
    }
    
    # Signature
		elsif ( $action eq "signature_summary") {
        $chart_ids = $self->_signature_summary($node_data, $tab_id);
    }
    
    # Profiler
		elsif ( $action eq "bamheader") {
        # node_data is xml source, No returned value
        $self->_bam_header($node_data, $tab_id); 
    }
    elsif ( exists($tallies{$action}) ) { 
        $chart_ids = $self->_tally($node_data, $tab_id, $action);
    }
    
    # LibCompare
    elsif ( $action eq "metrix_summary") {
      $chart_ids = $self->_metrix_summary($node_data, $tab_id);
    }
    elsif ( $action eq "isize_compare_read_group") {
      $chart_ids = $self->_isize_compare($node_data, $tab_id);
    }
    elsif ( $action eq "isize_count") {
      $chart_ids = $self->_isize_count($node_data, $tab_id);
    }
    elsif ( $action eq "isize_percent") {
      $chart_ids = $self->_isize_percent($node_data, $tab_id);
    }
    
    else{
      print "Non expected tab for procesing $tab_id with action $action\n";
      
			 qlogprint {l=>'WARN'}, "Non expected tab for procesing $tab_id with action $action\n";
    }
    
    if ( $chart_ids and scalar @{$chart_ids} > 0) { 
        $self->{page}->add_tab_charts($tab_id, $chart_ids);
        #print Dumper 	   $chart_ids;
    }
    #print Dumper $self->{page}->tab_contents();
}

# ADD new dynamic tabs here!	
# TODO pod process_dynamic_tabs
sub process_dynamic_tabs {
	my $self = shift;
  
	my $xml_root = $self->xmlroot();
	
	print "\nprocess_dynamic_tabs\n########################################\n";
	foreach my $dynamic_tab ( @{ $self->{config}->dynamicTabs() } ){
    my $XMLsource = $self->{config}->dynamicTab_xmlSource($dynamic_tab);
		#print "XMLsource $XMLsource\n";
    if ( $XMLsource and $xml_root->exists( $XMLsource ) ) {
			my @nodes = $xml_root->findnodes( $XMLsource );
      foreach my $node ( @nodes ) {
        #print "Node Name: ".$node->nodeName()."\n";
        
        # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
	      #        Add Dynamic tabs Here
	      # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
        
	      # Coverage
				if ($dynamic_tab eq "coverageFeature") {
					my $cov = QCMG::Visualise::Coverage->new( node => $node, verbose => $self->verbose() );						
					my $feature = $cov->feature();          
					# Remove _ and make next letter uppercase
					$feature =~ s/_(\d)/-$1/g; 
					$feature =~ s/_(\w)/\U$1\E/g;
          
					#print "Coverage Feature --> \n";
					print "\tdynamic_tab --> $dynamic_tab feature --> $feature\n";         
					my $tab = $self->{config}->create_dynamicTab($dynamic_tab, $dynamic_tab, $feature, $feature);
          my $tab_template_id = $self->{config}->dynamicTab_tabTemplateId($dynamic_tab);
          my $tab_content_template_id = $self->{config}->tabTemplate_tabContentTemplateId($tab_template_id);
          
          # Use template details to add a tab.
          $self->_create_config_tab_content($cov, $tab_content_template_id, $tab);
				}
        
        # Lib Compare
				 elsif ($dynamic_tab eq "isize_compare"){
           my $id = get_attr_by_name($node, "id");
           
           print "isize_compare --> $id\n";
           #print "\tdynamic_tab --> $dynamic_tab ID --> $id\n";
           my $tab = $self->{config}->create_dynamicTab($dynamic_tab, $dynamic_tab, $id);
           #print "$tab\n";
           my $tab_template_id = $self->{config}->dynamicTab_tabTemplateId($dynamic_tab);
           my $tab_content_template_id = $self->{config}->tabTemplate_tabContentTemplateId($tab_template_id);
           # Use template details to add a tab.
           #$self->_create_config_tab_content($node, $tab_content_template_id, $tab);
				   #print "Would be proccessing dynamic tabs for isize_compare -- $file_id\n";
				 }  
				
				# Profiler
        # Others, etc
        #last; 
			}   
		} 
	 }
   
   print "\n// END process_dynamic_tabs\n\n\n";
}


sub dynamic_sub_tabs {
	my $self = shift;
  my $tab_template_id = shift;
  my $XMLsource   = shift;
  
  $XMLsource = $self->{config}->tabContentTemplate_xmlSource($tab_template_id);
  
  print "\t_dynamic_sub_tab($tab_template_id, $XMLsource)\n";
  print $self->xpath(), "\n";
  
  my $sub_tabs = ();
  
	my $xml_root = $self->xmlroot();
  
  print "XMLsource: $XMLsource\n";
  if ( $XMLsource and $xml_root->exists( $XMLsource ) ) {
		my @nodes = $xml_root->findnodes( $XMLsource );
    foreach my $node ( @nodes ) {
      print "Node Name: ".$node->nodeName()."\n";
      
      # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
      #        Add Dynamic tabs Here
      # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
      
      # Lib Compare
			if ($tab_template_id eq "isize_compare_read_group"){
        my $index = get_attr_by_name($node, "index");
        #print "isize_compare --> $index\n";
        print "\ttab_template_id --> $tab_template_id ID --> $index\n";
        #my $tab = $self->{config}->create_dynamicTab($dynamic_tab, $dynamic_tab, $id);
        my $tab = $self->{config}->create_tab_fromTemplate($tab_template_id, $tab_template_id, $index, $index);
        
        push @{$sub_tabs}, $tab;
        print "\tNew Tab: $tab\n";
        print Dumper $self->{config}->tab($tab);
      #  my $tab_template_id = $self->{config}->dynamicTab_tabTemplateId($dynamic_tab);
        my $tab_content_template_id = $self->{config}->tabTemplate_tabContentTemplateId($tab_template_id);
      #  # Use template details to add a tab.
        $self->_create_config_tab_content($node, $tab_content_template_id, $tab);
			#  #print "Would be proccessing dynamic tabs for isize_compare -- $file_id\n";
      #last;
      }  
			
      
    }
  }
  
  return $sub_tabs;
}



# # #
# #     Profiler
#

# TODO pod _bam_header
sub _bam_header{
	 my $self	    = shift;
	 my $XMLsource  = shift;
	 my $tab	    = shift;
	 my $xml_root   = $self->xmlroot();
     
	 my @nodes = $xml_root->findnodes( $self->xpath() );     
	 for my $node ( @nodes ) {
     # TODO Create has to build summery 
     #print $node->textContent(), "\n";
     $self->{page}->add_tab_text($tab, $node->textContent);
	 }
}

# TODO pod _tally
sub _tally {
  my $self       		= shift;
  my $xml_parent 		= shift;
  my $tab        		= shift;
	my $tally_type		= shift;
	my $filter_value	= shift;
	
  #my $xml_root	= $self->xmlroot();
  my $chart_ids       = ();
  my $charts          = ();
  my $xmlpath_added   = 0;
  
  my $tally_actions = 
    { cycle_tally =>  ['cycle_tally',  'read_cycle_tally'],
      length_tally => ['length_tally', 'read_length_tally'],
      value_tally =>  ['value_tally',  'read_value_tally'],
      range_tally =>  ['range_tally',  'range_tally_isize0to5000', 'range_tally_isize0to50000']
    };
  
  my $tally_action = undef;
  foreach my $action (keys %{$tally_actions}){
    foreach my $tally ( @{$tally_actions->{$action}} ){
      if ( $tally eq $tally_type ) {
        $tally_action = $action;
      }
    }
  }  
  
  print "\t\t::: tally ($xml_parent, $tab, $tally_type)\n";
  my $XMLsource = $self->{config}->tabContent_xmlSource($tab);
	
  #print "tabContent for $tab\n";
  #print Dumper $self->{config}->tabContent($tab); 
  foreach my $tab_content_item ( @{$self->{config}->tabContent_content_items($tab)} ){
    print "\t\t -- tab_content_item: ", $self->{config}->tabContent_contentItem_type($tab_content_item), "\n"; 
    if ( $self->{config}->tabContent_contentItem_isType($tab_content_item, "tab_content_template") ){
      #print "\t\t\tTemplated Tab\n\t\t====================================\n";
      
      my $templateId = $self->{config}->tabContent_ContentItem_templateId($tab_content_item);
        
      $XMLsource = $self->{config}->tabContentTemplate_xmlSource($templateId);
      $self->xmlpath_add($XMLsource);
      $xmlpath_added   += 1;
      
      foreach my $content_item ( @{$self->{config}->tabContentTemplate_content($templateId)} ){
        
        if ( $self->{config}->tabContent_contentItem_isType($content_item, "chart_templates") ) {
          my $chartTemplateId      = $self->{config}->tabContent_ContentItem_chartTemplateId($content_item);
          my $chartdataTemplateId  = $self->{config}->tabContent_ContentItem_chartdataTemplateId($content_item);
          my $chart_id      = "chart_".$tab;
          
          #print "To create chart '$chart_id' from chart template '$chartTemplateId' and data template '$chartdataTemplateId'\n";
          
          $self->{config}->create_chart_fromTemplate($chartTemplateId, $chartdataTemplateId, $chart_id);
          #
          push (@{$charts}, $chart_id);
          #    chart_data      => $chart_data,
          #    chart_config    => $chart_config,
          #}
          
        }
        elsif ( $self->{config}->tabContent_contentItem_isType($content_item, "chart") ) { 
          print $self->{config}->tabContent_contentItem_type($content_item);
          my $chart_id      = $self->{config}->tabContent_contentItem_chartId($content_item);
          push (@{$charts}, $chart_id);
        }
        
      }
      
    }  
    

  
  	#print "Tally Type $tally_type to have tally action $tally_action\n";
    
  	if ( $tally_action eq "cycle_tally" ) {
      $chart_ids = $self->_cycle_tally($tab, $XMLsource, $charts);
  	}
  	elsif ( $tally_action eq "length_tally" ) {
  		$chart_ids = $self->_length_tally($tab, $XMLsource, $charts);
  	}
  	elsif ( $tally_action eq "value_tally" ) {
  		$chart_ids = $self->_value_tally($tab, $XMLsource, $charts, $filter_value);
  	}
  	elsif ( $tally_action eq "range_tally"  ) {
  		$chart_ids = $self->_range_tally($tab, $XMLsource, $charts);
  	}
	  else{
	    print "unknown tally action $tally_action\n";
	  }
	
    if ($xmlpath_added) {
        $self->xmlpath_drop();
    }
    
  }
	
  # Check if tab uses a template
  #if ( $self->{config}->tabContent_content_hasType($tab, "tab_content_template") ) {
  #    
  #    
  #    my $tab_template    = $self->{config}->content_template($tab);
  #    my $chart_templates = $self->{config}->chart_template($tab);
  #    
	#		if (! $chart_templates ) {
	#			print "tab_template\n";
	#			print Dumper $tab_template;
	#			 
	#		}
	#		
	#		
  #    $XMLsource = $self->{config}->templated_tab_xml_source($tab_template);
  #    $self->xmlpath_add($XMLsource);
  #    $xmlpath_added   += 1;
  #    
  #    if ( scalar @{ $chart_templates }  == 1 ) {
  #        my $chart           = $chart_templates->[0];
  #        my $chart_config    = $self->{config}->templated_chart($chart->{config});
  #        my $chart_data      = $self->{config}->templated_chart_data($chart->{data});
  #        my $chart_id        = "chart_".$tab;
  #        
  #        $self->{config}->create_config_chart($tab_template, $tab_template, $chart_id);
  #        
  #        $charts->{$chart_id} = { 
  #            chart_data      => $chart_data,
  #            chart_config    => $chart_config,
  #        }
  #    }
  #    else{
	#			qlogprint {l=>'ERROR'}, "I'm not yet ready for more than 1 chart per page!\n";
	#			die;       
  #    }
  #} 
  #else {
	#		qlogprint {l=>'ERROR'}, "I don't believe we should get to a non templated tally items!\n";
  #    die;
  #}
	

  
	return $chart_ids;
}

# TODO pod _cycle_tally
sub _cycle_tally {
	my $self       	= shift;
	my $tab        	= shift;
	my $XMLsource		= shift;
	my $charts			= shift;
		
	my $xml_root		= $self->xmlroot();
	my $chart_ids		= [];
    
	#print "\t\t::: cycle_tally ($tab, $XMLsource)\n";

  if ( $xml_root->exists( $self->xpath() ) ) {   
		my @nodes_PossibleValues    = undef;
		my @possibleValues          = undef;
		my @cycles                  = undef;

		for my $root_node ( @{ $xml_root->find($self->xpath()) } ) {
			@nodes_PossibleValues = $root_node->findnodes('PossibleValues');
			@possibleValues =  split (',', get_attr_by_name( $nodes_PossibleValues[0], 'possibleValues' ) );
			@cycles = $root_node->findnodes('Cycle');
			#print "\t\tCycles: ".scalar @cycles."\n";
		}

    #print Dumper $charts;
		foreach my $chart_id ( @{$charts} ) {        
			my $chart = $self->{config}->chart_data($chart_id); #$charts->{$chart_id}{chart_data};
			my $td = undef;     # Table Data
			#print "CHART $chart_id\n";
			
			# Google Chart data table
			if ( $chart->{chart_data_format} eq "DataTable" ) {
				$td = QCMG::Google::DataTable->new( name => $chart_id );
              
				# # #
        # #     Add table columns 
        #   
				if ( scalar @{$chart->{columns}} > 0 ) {
					# Defined in config
					#print "Defined in config\n";
					foreach my $column_info ( @{$chart->{columns}} ){			 
						$td->add_col( @{ $column_info } );
					}
				}
				else{
          # Defined by data   
          #print "Defined by data\n";
          my $id      = "cycle";
          my $label   = "Cycle";
          my $type    = "string";
          $td->add_col( $id, $label, $type );
        
          $type = "number";
          foreach my $value ( @possibleValues ){			 
						$id     = $value;
						$label  = $value;
						$td->add_col( $id, $label, $type );
          }
      	}
              
        # # #
        # #     Add table rows
        #
        foreach my $cycle ( @cycles ) {
					my @row = ();
					push @row, get_attr_by_name( $cycle, 'value' );
            
					my $tallies = {};
					foreach my $tally_item ( @{$cycle->findnodes('TallyItem')} ){
						my $tally_count = get_attr_by_name( $tally_item, 'count' );
						my $tally_value = get_attr_by_name( $tally_item, 'value' );
						$tallies->{$tally_value} = $tally_count;
					}
            
					# Align tally values with correcponding column in DataTable
					foreach my $value ( @possibleValues ){			 
						if ( exists $tallies->{$value} ) {
							push @row, $tallies->{$value};
						}else{
							push @row, 0;
						}
					}
					$td->add_row( @row );
        } # End Table rows
              
          #print "Converting DataTable data into perl Object...\n" ;  
          $self->{page}->add_chart_data( $chart_id, $td->as_perl_object() );
          push ( @{$chart_ids}, $chart_id );
          
	      } # End Google Chart
	      
				else{
					# Nothing yet - Thinking D3js
	      }
      }
  }
  #print "\t\tEnd cycle_tally - returning chart_ids\n";
  
  return $chart_ids;
}

# TODO pod _length_tally
sub _length_tally {
	my $self       	= shift;
	my $tab        	= shift;
	my $XMLsource		= shift;
	my $charts			= shift;
		
	my $xml_root		= $self->xmlroot();
	my $chart_ids		= [];
    
	#print "\t\t::: length_tally ($tab, $XMLsource)\n";
    
  if ( $xml_root->exists( $self->xpath() ) ) {   
		my @tally_items = undef;
		for my $root_node ( @{ $xml_root->find($self->xpath()) } ) {
			#print "Node Name ! ? ".$root_node->nodeName()."\n";
			@tally_items = $root_node->findnodes('TallyItem');
			#print "TallyItems: ".scalar @tally_items."\n";
		}
      
		foreach my $chart_id ( @{$charts} ) {        
      my $chart = $self->{config}->chart_data($chart_id); #$charts->{$chart_id}{chart_data};
      my $td = undef;     # Table Data
      
      #print "CHART $chart_id\n";
      #print "chart_data_format: ".$chart->{chart_data_format}."\n";
      
      if ( $chart->{chart_data_format} eq "DataTable" ) {
      	$td = QCMG::Google::DataTable->new( name => $chart_id );
      	# # #
      	# #     Add table columns 
      	#   
      	if ( scalar @{$chart->{columns}} > 0 ) {
					# Defined in config
					#print "Defined in config\n";
      		foreach my $column_info ( @{$chart->{columns}} ){			 
						$td->add_col( @{ $column_info } );
      		}
      	}
      	else{
					# Defined by data   
					# Nothing Yet
      	}
      	
      	# # #
      	# #     Add table rows                  
      	# 
      	my $tallies = {};
      	foreach my $tally_item ( @tally_items ){
					my $tally_count = get_attr_by_name( $tally_item, 'count' );
					my $tally_value = get_attr_by_name( $tally_item, 'value' );
					my $tally_percent = get_attr_by_name( $tally_item, 'percent' );
					$tallies->{$tally_value} = $tally_count;
      	}
      	
      	my @values = keys %{$tallies};
      	@values = sort( @values);
      	#@values = sort( {$a <=> $b} @values);
      	foreach my $value ( @values ){	
					$td->add_row( $value, $tallies->{$value} );		 
      	}
      	
      	#print "Converting DataTable data into perl Object...\n" ;  
      	$self->{page}->add_chart_data( $chart_id, $td->as_perl_object() );
      	push ( @{$chart_ids}, $chart_id );
      	
      } # End Google Chart
      
			else{
				# Nothing yet - Thinking D3js
      }
		}
  }
 	return $chart_ids;
}

# TODO pod _value_tally
sub _value_tally {
	my $self       	= shift;
	my $tab        	= shift;
	my $XMLsource		= shift;
	my $charts			= shift;
		
	my $xml_root		= $self->xmlroot();
	my $chart_ids		= [];
    
	#print "\t\t::: value_tally ($tab, $XMLsource)\n";
    
	if ( $xml_root->exists( $self->xpath() ) ) {
		my @tally_items = undef;
		for my $root_node ( @{ $xml_root->find($self->xpath()) } ) {
			#print "Node Name ! ? ".$root_node->nodeName()."\n";
			@tally_items = $root_node->findnodes('TallyItem');
			#print "TallyItems: ".scalar @tally_items."\n";
		}
    
		foreach my $chart_id ( @{$charts} ) {        
	    my $chart 				= $self->{config}->chart_data($chart_id); #$charts->{$chart_id}{chart_data};
			my $chart_config 	= $self->{config}->chart($chart_id); #$charts->{$chart_id}{chart_config};
	    my $td = undef;     # Table Data
  
	    #print "CHART $chart_id\n";
	    #print "chart_data_format: ".$chart->{chart_data_format}."\n";
  
	    if ( $chart->{chart_data_format} eq "DataTable" ) {
        $td = QCMG::Google::DataTable->new( name => $chart_id );
        # # #
        # #     Add table columns 
        #
        if ( scalar @{$chart->{columns}} > 0 ) {
					# Defined in config
					#print "Defined in config\n";
        	foreach my $column_info ( @{$chart->{columns}} ){			 
						$td->add_col( @{ $column_info } );
        	}
        }
        else{
            # Defined by data   
            # Nothing Yet
        }

        # # #
        # #     Add table rows                  
        # 
        my $tallies = {};
        foreach my $tally_item ( @tally_items ){
					my $tally_count     = get_attr_by_name( $tally_item, 'count' );
          my $tally_value     = get_attr_by_name( $tally_item, 'value' );
          my $tally_percent   = get_attr_by_name( $tally_item, 'percent' );
          $tallies->{$tally_value} = $tally_count;
        }
    
				
				# my @values = sort {$a <=> $b} keys %{$tallies};
				#print "sort_type $sort_type\n";
        my @values = keys %{$tallies};
				
				if ( $chart_config->{sort}{on} eq 'value' ) {
					if ( $chart_config->{sort}{by} eq 'number' ) {
						#sort numerically ascending
						#print "SORTING\n\n";
	        	@values = sort {$a <=> $b} @values;
        	}
        }				
				
        foreach my $value ( @values ){	
					$td->add_row( $value, $tallies->{$value} );		 
        }

        #print "Converting DataTable data into perl Object...\n" ;  
        $self->{page}->add_chart_data( $chart_id, $td->as_perl_object() );
        push ( @{$chart_ids}, $chart_id );
      
	    }# End Google Chart
	    else{
	      # Nothing yet - Thinking D3js
	    }

    }
	}

	return $chart_ids;	 
}

# TODO pod _range_tally
sub _range_tally {
	my $self       	= shift;
	my $tab        	= shift;
	my $XMLsource		= shift;
	my $charts			= shift;
		
	my $xml_root		= $self->xmlroot();
	my $chart_ids		= [];
  
  print "\t\t::: range_tally ($tab, $XMLsource)\n";

  if ( $xml_root->exists( $self->xpath() ) ) {
      my @tally_items = undef;
			for my $root_node ( @{ $xml_root->find($self->xpath()) } ) {
				#print "Node Name ! ? ".$root_node->nodeName()."\n";
				@tally_items = $root_node->findnodes('RangeTallyItem');
				print "\t\tRangeTallyItem: ".scalar @tally_items."\n";
			}
			
			#my $root_node = ( @{ $xml_root->find($self->xpath()) } );
      #for my $root_node ( @{ $xml_root->find($self->xpath()) } ) {
      #    print "Node Name ! ? ".$root_node->nodeName()."\n";
      #    @tally_items = $root_node->findnodes('TallyItem');
      #    print "TallyItems: ".scalar @tally_items."\n";
      #}
      
      foreach my $chart_id ( @{$charts} ) {        
          my $chart = $self->{config}->chart_data($chart_id); #$charts->{$chart_id}{chart_data};
          my $td = undef;     # Table Data
          
          print "CHART $chart_id\n";
          #print "chart_data_format: ".$chart->{chart_data_format}."\n";
          
          if ( $chart->{chart_data_format} eq "DataTable" ) {
              $td = QCMG::Google::DataTable->new( name => $chart_id );
              # # #
              # #     Add table columns 
              # 
              if ( scalar @{$chart->{columns}} > 0 ) {
								# Defined in config
								print "Defined in config\n";
              	foreach my $column_info ( @{$chart->{columns}} ){			 
                      $td->add_col( @{ $column_info } );
              	}
              }
              else{
                  # Defined by data   
                  # Nothing Yet
              }
      
              # # #
              # #     Add table rows                  
              #
               
              #if (exists $chart->{filter}) {
              #  print "limit range start to ", $chart->{filter}{start}, " and end ".$chart->{filter}{end}."\n";
              #}
              #print Dumper $chart;
              my $filter_start  = exists $chart->{filter_for}{start}  ? $chart->{filter_for}{start} : undef;
              my $filter_end    = exists $chart->{filter_for}{end}    ? $chart->{filter_for}{end} :  undef;
              #print Dumper  $chart->{filter};
              print "limit range start to ", $filter_start, " and end ".$filter_end."\n" 
                if ( defined $filter_start and defined $filter_end );
              my $tallies = {};
			        foreach my $tally_item ( @tally_items ){
                  my $tally_count = get_attr_by_name( $tally_item, 'count' );
                  my $tally_start = get_attr_by_name( $tally_item, 'start' );
                  my $tally_end   = get_attr_by_name( $tally_item, 'end' );
                  
                  # 
                  if ( defined $filter_start and defined $filter_end ) {
                    #print "Woah! $tally_start < $filter_start --- $tally_end > $filter_end\n";
                    next if ($tally_start < $filter_start or $tally_end > $filter_end );
                  # {
                  #    
                  #  };
                  }
                  
                  #if () {
                    my $gap = ($tally_end - $tally_start)-1 ;
                    my $value = $tally_start + ($gap/2);
                    $tallies->{$value} = $tally_count;
                    #}
              }
              
              my @values = keys %{$tallies};
              #print Dumper @values;
              @values = sort( {$a <=> $b} @values );
              #@values = sort( @values );
              foreach my $value ( @values ){	
                 $td->add_row( $value, $tallies->{$value} );		 
              }
                           
              #print "Converting DataTable data into perl Object...\n" ;  
              $self->{page}->add_chart_data( $chart_id, $td->as_perl_object() );
              #print Dumper $td->as_perl_object();
              push ( @{$chart_ids}, $chart_id );
							
              #my $tallies = {};
              #foreach my $tally_item ( @tally_items ){
              #    my $tally_count     = get_attr_by_name( $tally_item, 'count' );
              #    my $tally_value     = get_attr_by_name( $tally_item, 'value' );
              #    my $tally_percent   = get_attr_by_name( $tally_item, 'percent' );
              #    $tallies->{$tally_value} = $tally_count;
              #}
              
							##sort numerically ascending
              #my @values = sort {$a <=> $b} keys %{$tallies};
              #foreach my $value ( @values ){	
              #   $td->add_row( $value, $tallies->{$value} );		 
              #}

              #print "Converting DataTable data into perl Object...\n" ;  
              #$self->{page}->add_chart_data( $chart_id, $td->as_perl_object() );
              #push ( @{$chart_ids}, $chart_id );
              
          }# End Google Chart
          else{
	          # Nothing yet - Thinking D3js
          }

      }
  }    

	return $chart_ids;
}

# # #
# #     Signature
# 
# TODO pod _signature_summary
sub _signature_summary {
	my $self							= shift;
	my $signature_object 	= shift;
	my $tab			 					= shift;

  my $xml_root			= $self->xmlroot();
  my $chart_ids			= [];
  my $charts				= {};
	my $files 				= ();
	my $comparisons 	= ();	
  
  my $tab_charts = $self->{config}->tab_charts($tab);
  
  # FIXME don't access data directly
  # my $content_info	= $self->{config}{tab_content}{$tab}{content};
  # = $self->{config}->tab_charts($tab);
	
  
  
	if ( scalar @{ $tab_charts }  == 1 ) {
		my $chart           = $tab_charts->[0];
    my $chart_id        = $chart->{chart_id};
    my $chart_data_id   = $chart->{chart_data_id};
	  
		$charts->{$chart_id} = { 
	      chart_data      => $self->{config}->chart_data($chart_data_id),
	      chart_config    => $self->{config}->chart($chart_id)
	  };
  }
	else{
		qlogprint {l=>'ERROR'}, "I'm not yet ready for more than 1 chart per page!\n";
  	die;
	} 
	
  # Fetch Files	
  foreach my $node ( @{$xml_root->findnodes( '/qsignature/files/file' )} ) {
    my $file = {};
    if ($node->hasAttributes) {
      my $id = get_attr_by_name( $node, 'id' );
      foreach my $attr ($node->attributes) {
        $file->{$attr->nodeName} = $attr->getValue;
      }
    }
    
    #my %file = (id 				=> get_attr_by_name( $node, 'id' ), 
		#						coverage 	=> get_attr_by_name( $node, 'coverage' ),
		#						name 			=> get_attr_by_name( $node, 'name' ),
		#						type 			=> get_attr_by_name( $node, 'type' )
		#					);
		push (@{$files}, $file);
	}
  # Fetch comparisons	
  foreach my $node ( @{$xml_root->findnodes( '/qsignature/comparisons/comparison' )} ) {
    my $comparison = {};
    if ($node->hasAttributes) {
      my $id = get_attr_by_name( $node, 'id' );
      foreach my $attr ($node->attributes) {
        $comparison->{$attr->nodeName} = $attr->getValue;
      }
			if ( exists $comparison->{file1} and exists $comparison->{file2} ) {
				$comparison->{source} = delete $comparison->{file1};
				$comparison->{target} = delete $comparison->{file2};
			}
			
    }
    
    #my %comparison = (mapset 		=> get_attr_by_name( $node, 'mapset' ),
		#									snp_array => get_attr_by_name( $node, 'snp_array' ),
		#									calcs 		=> get_attr_by_name( $node, 'calcs' ),
		#									overlap		=> get_attr_by_name( $node, 'overlap' ),
		#									score 		=> get_attr_by_name( $node, 'score' )
		#								);
		push (@{$comparisons}, $comparison)
	}
		
	foreach my $chart_id ( keys %{$charts} ) {    
		my $chart = $charts->{$chart_id}{chart_data};
	  #my $td = undef;     # Table Data
	  if ( $chart->{chart_data_format} eq "DataTable" ) {
	  	# Nothing -> Google Chart
		}
		elsif  ( $chart->{chart_data_format} eq "JSON" ) {
			#print "Oh yeah, D3 all the way!\n";
      # TODO Add metadata for sorting/clustering.
      my $chart_data = { 
				nodes => $files,
				links => $comparisons
			};
			
      $self->{page}->add_chart_data( $chart_id, $chart_data );
      push ( @{$chart_ids}, $chart_id );			
		}
	}	
	 
	return $chart_ids;
}




sub _content_charts {
  my $self			= shift;
  my $tab       = shift;
  
  my $charts				= ();
  foreach my $content_item ( @{$self->{config}->tabContent_content_items($tab)} ){
    #print Dumper $content_item;
    
    if ( $self->{config}->tabContent_contentItem_isType($content_item, "chart_templates") ){
      #print "\n\t\t\tTemplated Chart\n\t\t====================================\n";
      my $chart_templateId      = $self->{config}->tabContent_ContentItem_chartTemplateId($content_item);
      my $chartdata_templateId  = $self->{config}->tabContent_ContentItem_chartdataTemplateId($content_item);
      my $chart_id      = "chart_".$tab;  # This does not allow multiple charts per page.
      
      $self->{config}->create_chart_fromTemplate($chart_templateId, $chartdata_templateId, $chart_id);
      push (@{$charts}, $chart_id);
      
    }
    elsif ( $self->{config}->tabContent_contentItem_isType($content_item, "chart") ) { 
      my $chart_id      = $self->{config}->tabContent_contentItem_chartId($content_item);
      push (@{$charts}, $chart_id);
    }
  }
  return $charts;
}


# # #
# #     Coverage
# 
# TODO pod _coverage
sub _coverage {
  my $self			= shift;
  my $coverage_object = shift;
  #my $content_info	= shift;
  my $tab			 = shift;
  
  #print "\t\tstart _coverage()   tab_charts    $tab\n";
  
  my $chart_ids = [];
  my $content_info = $self->{config}->tabContent( $self->{config}->tab_contentId($tab) ); # {tab_content}{$tab}{content};
  my $cov = $coverage_object; #QCMG::Visualise::Coverage->new( node => $node, verbose => $self->verbose() );
    
  
    
  my $charts = $self->_content_charts($tab);
  #print "\t\t_content_charts($tab)", Dumper $charts;
  
  #print Dumper $self->{config}{config}{tab_content};
  #print "Returning empty after _content_charts in _coverage\n";
  #return $chart_ids;
   
   #my $tab_charts = $self->{config}->tab_charts($tab);
   #
   ## Get chart config information
   #if ( scalar @{ $tab_charts }  == 1 ) {
   #  my $chart           = $tab_charts->[0];
   #  my $chart_id        = $chart->{chart_id};
   #  my $chart_data_id   = $chart->{chart_data_id};
   #  
   #  $charts->{$chart_id} = { 
   #    chart_data      => $self->{config}->chart_data($chart_data_id),
   #    chart_config    => $self->{config}->chart($chart_id)
   #  };
   #}
   #else{
   #  qlogprint {l=>'ERROR'}, "I'm not yet ready for more than 1 chart per page!\n";
   #  die;
   #} 

  #print Dumper $charts;
  
  foreach my $chart_id ( @{$charts} ){
    #print "\t\tCHART ID $chart_id\n";
     
    #my $chart = $self->{config}{chart_data}{$chart_id};
    my $chart = $self->{config}->chart_data($chart_id);
    my $td = undef;
    if ( $chart->{chart_data_format} eq "DataTable" ) {
      $td = QCMG::Google::DataTable->new( name => $chart_id );
      
      # Use config to determine columns
      foreach my $column_info ( @{$chart->{columns}} ){			 
        $td->add_col( @{ $column_info } );
      }
    
      foreach my $row (@{$cov->data}) {
        $td->add_row( @{ $row } );
        #debugging: qlogprint( join("\t",@{$row}), "\n" );
      }
      $td->add_percentage(1);
      $td->trim(0.99,2);
      $td->bin;  # default 50 bins is what we want
      #$td->add_format(2,'%.4f');  # 4 decimal places
      $td->drop_col(2);
      
      $self->{page}->add_chart_data( $chart_id, $td->as_perl_object() );
      
      push ( @{$chart_ids}, $chart_id );
      
    }else{
      # Nothing yet - Thinking D3js
    }
     
  } 

	 #if ( exists $content_info->{chart_data} ) {
	 #  if ( scalar @{ $content_info->{chart_data} } == 1 ) {
	 #	   
	 #	   #my $chart_id = $content_info->{chart_data}[0];
	 #	   
	 #  }else{
	 #	  print "Too many chart_data requested for tab $tab\n"; 
	 #  }
     #
	 #}
	 
	 return $chart_ids;
}


# TODO pod _cumulative_coverage
sub _cumulative_coverage {
    my $self = shift;
    my $coverage_object = shift;
    my $tab  = shift;
    my $chart_ids = [];
    
    #print "\t\tstart _cumulative_coverage()   tab_charts    $tab\n";
    
    my $content_info = $self->{config}->tabContent( $self->{config}->tab_contentId($tab) ); # {tab_content}{$tab}{content};
    
    my $cov = $coverage_object; #QCMG::Visualise::Coverage->new( node => $node, verbose => $self->verbose() );

    my $charts = $self->_content_charts($tab);
    #print "\t\t_content_charts($tab) ", Dumper $charts;
  
    #print Dumper $self->{config}{config}{tab_content};
    #print "!!! Returning empty after _content_charts in _cumulative_coverage\n";
    #return $chart_ids;

    #my $content_info = $self->{config}{tab_content}{$tab}{content};
    #my $cov = QCMG::Visualise::Coverage->new( node => $node,
    #										  verbose => $self->verbose() );
    
    
    #print "\t\tstart _cumulative_coverage   tab_charts  $tab\n";   
    
    #for my $chart_id ( @{$self->{config}->tab_charts($tab)} ){
    foreach my $chart_id ( @{$charts} ){
      
        #print "\t\tCHART ID $chart_id\n";
        
        #my $chart_id = $content_info->{chart_data}[0];
        my $chart = $self->{config}->chart_data($chart_id);
        my $td = undef;
        
        if ( $chart->{chart_data_format} eq "DataTable" ) {
     	  $td = QCMG::Google::DataTable->new( name => $chart_id );
  
     	  # Use config to determine columns
     	  foreach my $column_info ( @{$chart->{columns}} ){			 
     		  $td->add_col( @{ $column_info } );
     	  }
  
     	  my $cumulative_cov = 0;
     	  my @cov_rows = ();  # We'll parse these again later
     	  foreach my $row (@{$cov->data}) {
     		  my $cov_at_this_or_more = 1 - ($cumulative_cov / $cov->region_length);
     		  $td->add_row( $row->[0], $cov_at_this_or_more, 0.8, $row->[1] );
     		  push @cov_rows, [ $row->[0], $cov_at_this_or_more, $row->[1] ];
     		  $cumulative_cov += $row->[1];
     	  }
     	  # We're going to add percentage but only for trimming - we'll pull it
     	  # off for the final plotting
     	  $td->add_percentage(3);
     	  $td->trim(0.95, 4, 'cumul_max', '_..++');
     	  $td->drop_col(4);
     	  $td->drop_col(3);
  
     	  $self->{page}->add_chart_data($chart_id, $td->as_perl_object() );
  
     	  push ( @{$chart_ids}, $chart_id );
			
     	   # Parse @cov_rows looking for key coverage criteria and bury them in
     	   # the HTML document as <meta> tags in the page <head>.
     	   my @key_covs = ();
     	   foreach my $ctr (0..($#cov_rows-1)) {
     		   if ($cov_rows[$ctr]->[1] >= 0.9 and $cov_rows[$ctr+1]->[1] < 0.9) {
     			   push @key_covs, '0.90@'. $cov_rows[$ctr]->[0];
     		   }
     		   if ($cov_rows[$ctr]->[1] >= 0.8 and $cov_rows[$ctr+1]->[1] < 0.8) {
     			   push @key_covs, '0.80@'. $cov_rows[$ctr]->[0];
     		   }
     		   if (($cov_rows[$ctr]->[0] eq '10') or ($cov_rows[$ctr]->[0] eq '20')) {
     			   push @key_covs, sprintf('%.2f',$cov_rows[$ctr]->[1]).
     							   '@'. $cov_rows[$ctr]->[0];
     		   }
     	   }		  

     	   # Add average coverage
     	   push @key_covs, 'avg@'.sprintf('%.2f', $cov->average_coverage() );

     	   # Push key coverages into a META element in page header
     	   $self->{page}->add_page_meta( 'coverage_'. $cov->feature, join(',',@key_covs) );
   
   
     	   # If we are to build the summary table for the front page then we
     	   # are going to need to keep summary stats for each feature ...
     	   ## TODO $self->add_feature( $cov );
     	   $self->{page}->add_summary_source($cov->feature, $cov); 
   
        }else{
     	   # Nothing yet - Thinking D3js
        }
        
    } 
    
    #print Dumper keys %{$self->{page}{summary}{source}{chrom}};
    #print "end _cumulative_coverage     tab_charts  $tab\n";
    
   #if ( $self->{config}->has_tab_content_type($tab, "charts") ) {
   #    if ( scalar @{ $self->{config}->tab_content($tab, "charts") } == 1 ) {
   #
   #
   #
   #
   #
   #    }else{
   #      print "Too many chart_data requested for tab $tab\n"; 
   #    }
   #
   #}
   #print "Chart IDs\n";
   #print Dumper $chart_ids;
    return $chart_ids
}


# TODO pod _coverage_summary
sub _coverage_summary {
	 my $self		   = shift;
	 my $tab			= shift;
	 my $cov_features   = $self->{page}->summary_source();
	 
   #print "_coverage_summary()\n";
   #print Dumper keys %{$cov_features};
   #print Dumper $cov_features->{chrom};
   
	 my $content_info = $self->{config}->tabContent_content_items_ofType($tab, "tables");
   # $self->{config}{tab_content}{$tab}{content};
   
   #print Dumper $self->{config}->tabContent_content_items_ofType($tab, "tables");
   
   
	 if ( $cov_features and $content_info) {
	   if ( scalar @{ $content_info } == 1 ) {
		   my $table_id = $content_info->[0]{table_id};
		   
		   # Add table for tab.
		   $self->{page}->add_tab_table($tab, $table_id);
		   
		   # Do totals calcs first so they can be used as denominators
		   my $total_bases_in_bam  = 0;
		   my $total_bases_in_span = 0;
		   foreach my $id (sort keys %{ $cov_features }) {
			   #print "$id\n";
         #print Dumper keys %{};
         #if ( $cov_features->{$id} ) {
           my $feature = $cov_features->{ $id };
  			   $total_bases_in_bam  += $feature->bases_of_sequence; 
  			   $total_bases_in_span += $feature->region_length;
           #}
		   }
		   
	 
		   my $row = [];
		   # Do per-feature calcs using totals calcs
		   foreach my $id (sort keys %{ $cov_features }) {
			   #if ( $cov_features->{$id} ) {
           my $feature = $cov_features->{ $id };
           
          my $cumulative_cov = 0;
          my @cov_rows = ();  # We'll parse these again later
          foreach my $row (@{$feature->data}) {
            my $cov_at_this_or_more = 1 - ($cumulative_cov / $feature->region_length);
            push @cov_rows, [ $row->[0], $cov_at_this_or_more, $row->[1] ];
            $cumulative_cov += $row->[1];
          }
          my $key_covs = {};
          foreach my $ctr (0..($#cov_rows-1)) {
            #if ($cov_rows[$ctr]->[1] >= 0.9 and $cov_rows[$ctr+1]->[1] < 0.9) {
            #  push @key_covs, '0.90@'. $cov_rows[$ctr]->[0];
            #}
            if ($cov_rows[$ctr]->[1] >= 0.8 and $cov_rows[$ctr+1]->[1] < 0.8) {
              $key_covs->{'0.80@'} = $cov_rows[$ctr]->[0];
            }
            #if (($cov_rows[$ctr]->[0] eq '10') or ($cov_rows[$ctr]->[0] eq '20')) {
            #  push @key_covs, sprintf('%.2f',$cov_rows[$ctr]->[1]).'@'. $cov_rows[$ctr]->[0];
            #}
      	  } 
           
           #print Dumper $key_covs;
           #exit;

  			   # Use trinary op to check that denominators are not zero
  			   my $span_as_pc_of_total = $total_bases_in_span ?
  					 ($feature->region_length * 100 / $total_bases_in_span) : 0;
  			   my $coverage_as_pc_of_total = $total_bases_in_bam ?
  					 ($feature->bases_of_sequence * 100 / $total_bases_in_bam) : 0;
		 
  			   $row = [
  					   $feature->feature,
               $key_covs->{'0.80@'}.'x',
  					   $feature->region_length,
  					   sprintf( "%.2f", $span_as_pc_of_total ),
  					   $feature->bases_of_sequence,
  					   sprintf( "%.2f", $coverage_as_pc_of_total ),
  					   sprintf( "%.2f", $feature->average_coverage )
  			   ];
		 
  			   # Add row data for table
  			   $self->{page}->add_tab_table_row($tab, $table_id, $row);
           #}
		   }
	 
		   # Add summary row data for table.
		   $row = [
			   'Totals',
         '',
			   $total_bases_in_span,
			   '100.00',
			   $total_bases_in_bam,
			   '100.00',
			   sprintf( "%.2f", $total_bases_in_bam / $total_bases_in_span)
		   ];
		   $self->{page}->add_tab_table_row($tab, $table_id, $row);
	 
	   }else{
		  print "Too many chart_data requested for tab $tab\n"; 
	   }

	 }
	 
   
	 


}

# # #
# #     qlibcompare
#
sub _metrix_summary {
  my $self	            = shift;
  my $XMLsource         = shift;
  my $tab	              = shift;
  
  my $chart_ids			= [];
  my $charts				= {};
  my $files 				= ();
  my $read_groups   = ();
  my $comparisons 	= ();
  
  my $xml_root      = $self->xmlroot();
  my $tab_charts = $self->{config}->tab_charts($tab);
  my $summary_scores   = $self->{page}->summary_source();
  
  print "_metrix_summary($tab)\n";
  #print Dumper $summary_scores;
  
  # Capture file Metadata
  if ( $XMLsource and $xml_root->exists( $XMLsource ) ) {
    foreach my $node ( $xml_root->findnodes( $XMLsource ) ) {
      if ($node->hasAttributes) {
        my $index = get_attr_by_name( $node, 'index' );
        $files->[$index] = {};
        foreach my $attr ($node->attributes) {
          $files->[$index]{$attr->nodeName} = $attr->getValue;
        }
      }
    }
  }
  
  my $other_XMLsource = 'read_groups/RG';
  if ( $xml_root->exists( $other_XMLsource ) ) {
    foreach my $node ( @{$xml_root->findnodes( $other_XMLsource )} ) {
      if ($node->hasAttributes) {
        my $index = get_attr_by_name( $node, 'index' );
        $read_groups->[$index] = {};
        foreach my $attr ($node->attributes) {
          $read_groups->[$index]{$attr->nodeName} = $attr->getValue;
        }
      }
    }
  }
  
  #foreach my $file ( @{$files} ){
  #  my $rgs = $file;
  #  print Dumper $file;
  #}
  #
  #foreach my $rg ( @{$read_groups} ){
  #  #my $rgs = $file;
  #  print Dumper $rg;
  #}
  
  ##foreach my $node ( @{$xml_root->findnodes( $other_XMLsource )} ) {
  ##  my $RG = {};
  ##  foreach my $attr ($node->attributes) {
  ##    $RG->{$attr->nodeName} = $attr->getValue;
  ##  }
  ##  
  ##  if ($RG->{index} ne $RG_a) {
  ##    #push @{$array_heading}, basename ($file->{path}, ".bam.qp.xml");
  ##    push @{$array_heading}, "_".$RG->{ID};
  ##  }else{
  ##    # set pane_label
  ##    #my $new_label = $self->{config}->tabContent_paneLabel($tab_contentId, basename ($file->{path}, ".bam.qp.xml"));
  ##    my $new_label = $self->{config}->tabContent_paneLabel($tab_contentId, "Read Group ID: ".$RG->{ID} );
  ##    #print "\t O.O Mapset is $new_label \n";
  ##    #print "was it set for $tab?", $self->{config}->tab_label($tab, $new_label);
  ##    if ( $RG->{no_inserts} ) {
  ##      print $RG->{ID}." Has No inserts!\n";
  ##    }
  ##    
  ##  }
  ##  
	##	push (@{$read_groups}, $RG);
  ##  
	##}
  
  
  # Get chart config information
	if ( scalar @{ $tab_charts }  == 1 ) {
		my $chart           = $tab_charts->[0];
    my $chart_id        = $chart->{chart_id};
    my $chart_data_id   = $chart->{chart_data_id};
	  
		$charts->{$chart_id} = { 
	      chart_data      => $self->{config}->chart_data($chart_data_id),
	      chart_config    => $self->{config}->chart($chart_id)
	  };
  }
	else{
		qlogprint {l=>'ERROR'}, "I'm not yet ready for more than 1 chart per page!\n";
  	die;
	} 
  
  
  if ($summary_scores) {
    # Keeping data output tidy
    my @ids = sort( keys %{$summary_scores} );
    foreach my $i ( @ids ){
      foreach my $j ( $summary_scores->{$i} ){
        foreach my $link ( @{$j} ){
          push @{$comparisons}, $link;
        }
      }
    } 
  
  	foreach my $chart_id ( keys %{$charts} ) {    
  		my $chart = $charts->{$chart_id}{chart_data};
  	  #print "$chart_id chart_data_format\t", $chart->{chart_data_format}, "\n";

  	  if ( $chart->{chart_data_format} eq "DataTable" ) {
  	  	# Nothing -> Google Chart
  		}
  		elsif  ( $chart->{chart_data_format} eq "JSON" ) {
  			#print "Oh yeah, D3 all the way!\n";
  			my $chart_data = { 
  			  #nodes  => $files,
  				nodes => $read_groups,
          links => $comparisons,
          extra => $files
  			};
			  
        #print Dumper $chart_data;
        
        $self->{page}->add_chart_data( $chart_id, $chart_data );
        push ( @{$chart_ids}, $chart_id );			
  		}
  	}	
  
    $self->{page}->add_tab_charts($tab, $chart_ids);
  }else{
    print "NO SUMMARY SCORES !!!\n";
  }
  
  
  
  return $chart_ids;
}

sub _isize_compare {
  my $self	      = shift;
  my $XMLsource   = shift;
  my $tab	        = shift;
  
  my $files           = {};
  my $read_groups     = [];
  my $chart_ids			  = [];
  my $summary_scores  = ();
  my @binned_compares = ();
  my $xml_root        = $self->xmlroot();
  my $array_heading   = ["bin", ];
  #my $file_a = get_attr_by_name($XMLsource, "id");
  my $RG_index_a = get_attr_by_name($XMLsource, "index");  
  
  print "_isize_compare($tab) $RG_index_a\n";
  
  my $tab_contentId = $self->{config}->tab_contentId($tab);
  

  # TODO : Whats happing here?
#  foreach my $node ( @{$xml_root->findnodes( 'files/file' )} ) {
#    my $file = {index 				  => get_attr_by_name( $node, 'id' ),
#                path        => get_attr_by_name( $node, 'path' ), 
#                #sum_count   => get_attr_by_name( $node, 'sum_count' ),
#                failed_qc   => get_attr_by_name( $node, 'failed_qc' ),
#                #no_inserts  => get_attr_by_name( $node, 'no_inserts' )
#              };
#    
#    #keep index of files
#    $files->{$file->{index}} = $file;
#    
#    # No need for listing data on comparing a file against itself!
#    if ($file->{index} ne $file_a) {
#      push @{$array_heading}, basename ($file->{path}, ".bam.qp.xml");
#    }else{
#      # set pane_label
#      my $new_label = $self->{config}->tabContent_paneLabel($tab_contentId, basename ($file->{path}, ".bam.qp.xml"));
#      #print "\t O.O Mapset is $new_label \n";
#      #print "was it set for $tab?", $self->{config}->tab_label($tab, $new_label);
#      if ( $file->{no_inserts} ) {
#        print basename ($files->{$file_a}{path}, ".bam.qp.xml")." Has No inserts!\n";
#      }
#      
#    }
#	}
  
  foreach my $node ( @{$xml_root->findnodes( 'read_groups/RG' )} ) {
    my $RG = {};
    foreach my $attr ($node->attributes) {
      $RG->{$attr->nodeName} = $attr->getValue;
    }
    
    #print Dumper $RG;
        
    if ($RG->{index} ne $RG_index_a) {
      #push @{$array_heading}, basename ($file->{path}, ".bam.qp.xml");
      push @{$array_heading}, "_".$RG->{ID};
    }else{
      # set pane_label
      #my $new_label = $self->{config}->tabContent_paneLabel($tab_contentId, basename ($file->{path}, ".bam.qp.xml"));
      my $new_label = $self->{config}->tabContent_paneLabel($tab_contentId, "Read Group ID: ".$RG->{ID} );
      #print "\t O.O Mapset is $new_label \n";
      #print "was it set for $tab?", $self->{config}->tab_label($tab, $new_label);
      if ( $RG->{no_inserts} ) {
        print $RG->{ID}." Has No inserts!\n";
      }
      
    }
    
		push (@{$read_groups}, $RG);
    
	}
  
  push @binned_compares, $array_heading;  
  
  
#  # TODO : Whats happing here?
  my $test_structure = {};
	my @nodes = $XMLsource->findnodes( "COMPARED" );
  foreach my $node ( @nodes ) {
    my $RG_index_b = get_attr_by_name($node, "index");
    my $score = 0;
    
    # No need for listing data on comparing a file against itself!
    if ( $read_groups->[$RG_index_a]{no_inserts} and $read_groups->[$RG_index_b]{no_inserts} ) {
      print "Not comparing ".$read_groups->{$RG_index_a}{ID}." No inserts!\n";
      $score = 2;
    }
    elsif ($RG_index_a != $RG_index_b) {
      my $range_tally_items = $node->findnodes( "RangeTallyItem" );
      foreach my $tally_item ( @{$range_tally_items} ){
        my $start = get_attr_by_name($tally_item, "start"); 
        my $end   = get_attr_by_name($tally_item, "end");
        my $percentage_difference = get_attr_by_name($tally_item, "percentage_difference");
        $score += $percentage_difference;
        
        push @{$test_structure->{$start}}, sprintf "%.10f", $percentage_difference;
      } 
    }
    # No need for listing data on comparing a file against itself!
    #if ( $files->{$file_a}{no_inserts} and $files->{$file_b}{no_inserts} ) {
    ##  #print "Not comparing ".basename ($files->{$file_a}{path}, ".bam.qp.xml")." No inserts!\n";
    #  $score = 2;
    #}
    #elsif ($file_a != $file_b) {
    #  my $range_tally_items = $node->findnodes( "RangeTallyItem" );
    #  foreach my $tally_item ( @{$range_tally_items} ){
    #    my $start = get_attr_by_name($tally_item, "start"); 
    #    my $end   = get_attr_by_name($tally_item, "end");
    #    my $percentage_difference = get_attr_by_name($tally_item, "percentage_difference");
    #    $score += $percentage_difference;
    #    
    #    push @{$test_structure->{$start}}, sprintf "%.10f", $percentage_difference; 
    #  }
    #}
    my $link = {
      source => $RG_index_a,
      target => $RG_index_b,
      value  => $score
    };
    push @{$summary_scores}, $link;
    
    print "$RG_index_a VS $RG_index_b = $score\n";
  }
  
  #print "### ### ### summary_scores\n";
  #print Dumper $summary_scores;
  
  # Build Array for Google Charts
  my @starts = sort {$a <=> $b} keys %{$test_structure};
  foreach my $bin ( @starts ){
    my @row = ($bin, @{$test_structure->{$bin}});
    push @binned_compares, \@row;
  }
  
  #print Dumper @binned_compares;
  
  # # # foreach my $content_item ( @{$self->{config}->tabContentTemplate_content($templateId)} ){
  # # #   
  # # #   if ( $self->{config}->tabContent_contentItem_isType($content_item, "chart_templates") ) {
  # # #     my $chartTemplateId      = $self->{config}->tabContent_ContentItem_chartTemplateId($content_item);
  # # #     my $chartdataTemplateId  = $self->{config}->tabContent_ContentItem_chartdataTemplateId($content_item);
  # # #     my $chart_id      = "chart_".$tab;
  # # #     
  # # #     #print "To create chart '$chart_id' from chart template '$chartTemplateId' and data template '$chartdataTemplateId'\n";
  # # #     
  # # #     $self->{config}->create_chart_fromTemplate($chartTemplateId, $chartdataTemplateId, $chart_id);
  # # #     #
  # # #     push (@{$charts}, $chart_id);
  # # #     #    chart_data      => $chart_data,
  # # #     #    chart_config    => $chart_config,
  # # #     #}
  # # #     
  # # #   }
  # # #   elsif ( $self->{config}->tabContent_contentItem_isType($content_item, "chart") ) { 
  # # #     print $self->{config}->tabContent_contentItem_type($content_item);
  # # #     my $chart_id      = $self->{config}->tabContent_contentItem_chartId($content_item);
  # # #     push (@{$charts}, $chart_id);
  # # #   }
  # # #   
  # # # }
  
  my $charts = $self->_content_charts($tab);
  
  #print Dumper $charts;
  foreach my $chart_id ( @{$charts}){
    print "Chart ID is $chart_id\n";
    
    #print Dumper $self->{config}->chart($chart_id);
    #
    #my chart_data = $self->{config}->chart_data($chart_id)
    
    # Google Chart
	  if ( $self->{config}->chart_data_isFormat($chart_id, "DataTable") ) {
	  	# Nothing
		}
    elsif ( $self->{config}->chart_data_isFormat($chart_id, "Array") ){
      $self->{page}->add_chart_data( $chart_id, \@binned_compares );
      push ( @{$chart_ids}, $chart_id );
    }
    # D3js
		elsif  ( $self->{config}->chart_data_isFormat($chart_id, "JSON") ) {
      # Nothing
    }

    #print Dumper $self->{config}->chart_data($chart_id);
  }
  
  #exit;
  
  #print "tabContent($tab)\n";
  #print Dumper $self->{config}->tabContent($tab);
  
  #$self->{config}->
  
  
  ##foreach my $content_item ( @{$self->{config}->tabContent_content_items_ofType($tab, "chart_templates")} ){
  ##  my $chartTemplateId      = $self->{config}->tabContent_ContentItem_chartTemplateId($content_item);
  ##  my $chartdataTemplateId  = $self->{config}->tabContent_ContentItem_chartdataTemplateId($content_item);
  ##  my $chart_id      = "chart_".$tab;
  ##  
  ##  $self->{config}->create_chart_fromTemplate($chartTemplateId, $chartdataTemplateId, $chart_id);
  ##  
  ##  
  ##  #my $chart_id      = $self->{config}->tabContent_contentItem_chartId($content_item);
  ##  #my $chart_data_id = $self->{config}->tabContent_contentItem_chartDataId($content_item);
  ##  #my $chart         = $self->{config}->chart($chart_id);
  ##  #my $chart_data    = $self->{config}->chart_data($chart_id);
  ##  
  ##  print "chartTemplateId: $chartTemplateId\t\t  chartdataTemplateId: $chartdataTemplateId\t\t chart_id: $chart_id\n";
  ##  
  ##  print "ADD DATA to chart!\n";
  ##  
  ##  ## Google Chart
  ##  #if ( $chart_data->{chart_data_format} eq "DataTable" ) {
  ##  #	# Nothing
  ##	#}
  ##  #elsif ( $chart_data->{chart_data_format} eq "Array" ){
  ##  #  $self->{page}->add_chart_data( $chart_id, \@binned_compares );
  ##  #  push ( @{$chart_ids}, $chart_id );
  ##  #}
  ##  ## D3js
  ##	#elsif  ( $chart_data->{chart_data_format} eq "JSON" ) {
  ##  #  # Nothing
  ##  #}
  ##}
  
  
  # Add score to summery page.
  print  " ID ?? $RG_index_a\n";
  $self->{page}->add_summary_source($RG_index_a, $summary_scores);
  
	return $chart_ids;
}

# TODO
sub __isize_count {
  my $self	      = shift;
  my $XMLsource   = shift;
  my $tab	        = shift;
  
  my $xml_root        = $self->xmlroot();
  my $chart_ids			  = [];
  #my @binned_percents = ();
  my @binned_counts   = ();
  
  
  print "_isize_count($tab)\n";
  
  
  # Sneeky
  # Fetch Files	
  my $files = ();
  my $array_heading = ["bin", ];
  print "Node Name =".$xml_root->nodeName()."\n";
  foreach my $node ( @{$xml_root->findnodes( 'files/file' )} ) {
		my $file = {id 				=> get_attr_by_name( $node, 'id' ),
                path      => get_attr_by_name( $node, 'path' ), 
								sum_count => get_attr_by_name( $node, 'sum_count' ),
                failed_qc => get_attr_by_name( $node, 'failed_qc' )
                #file parent_project="Not Available" 
                #lims_identifier="CNAG_-_FASTQ_A3MCW_-_513F-B_GAGTGG_L001_BWA-backtrack" 
                #project="Not Available" 
                #primary_library="Not Available" 
                #capture_kit="Not Available" 
                #sequencing_platform="Not Available" 
                #isize_min="Not Available" 
                #sample="Not Available" 
                #id="0" 
                #isize_manually_reviewed="Not Available" 
                #path="/mnt/seq_results/smgres_special/icgc_benchmark_2013/seq_mapped/CNAG_-_FASTQ_A3MCW_-_513F-B_GAGTGG_L001_BWA-backtrack.bam.qp.xml" 
                #library_protocol="Not Available" 
                #aligner="Not Available" 
                #sum_count="19395889" 
                #species_reference_genome="Not Available" 
                #type="mapset" 
                #sample_code="Not Available" 
                #isize_max="Not Available" 
                #material="Not Available" />
							};
		push (@{$files}, $file);
    push @{$array_heading}, basename ($file->{path}, ".bam.qp.xml");
	}
  push @binned_counts, $array_heading;
  
  
  if ( $xml_root->exists( $self->xpath() ) ) {
    my @range_tally_items = undef;
		foreach my $root_node ( @{ $xml_root->find($self->xpath()) } ) {
			#print "Node Name ! ? ".$root_node->nodeName()."\n";
			@range_tally_items = $root_node->findnodes('RangeTallyItem');
			print "\t\tRangeTallyItem: ".scalar @range_tally_items."\n";
		}

    foreach my $range_tally_item (@range_tally_items){
      #my @tally_percents = ();
      my @tally_counts = ();
      #printf "[%d", get_attr_by_name( $range_tally_item, 'start' );
      #push @tally_percents, get_attr_by_name( $range_tally_item, 'start' );
      push @tally_counts, get_attr_by_name( $range_tally_item, 'start' );
      my @tally_items = $range_tally_item->findnodes('TallyItem');
      foreach my $tally_item  (@tally_items){
        #printf ", %d", get_attr_by_name( $tally_item, 'count' );
        #printf ", %.7f", get_attr_by_name( $tally_item, 'percent' );
        #push @tally_percents, get_attr_by_name( $tally_item, 'percent' );
        push @tally_counts, get_attr_by_name( $tally_item, 'count' );
      }
      #printf "],\n";
      #push @binned_percents, \@tally_percents;
      push @binned_counts, \@tally_counts;
    }
    
    
    foreach my $content_item ( @{$self->{config}->tabContent_content_items_ofType($tab, "chart")} ){
      my $chart_id      = $self->{config}->tabContent_contentItem_chartId($content_item);
      my $chart_data_id = $self->{config}->tabContent_contentItem_chartDataId($content_item);
      my $chart         = $self->{config}->chart($chart_id);
      my $chart_data    = $self->{config}->chart_data($chart_id);
      
      # Google Chart
  	  if ( $chart_data->{chart_data_format} eq "DataTable" ) {
  	  	# Nothing
  		}
      elsif ( $chart_data->{chart_data_format} eq "Array" ){
        $self->{page}->add_chart_data( $chart_id, \@binned_counts );
        push ( @{$chart_ids}, $chart_id );
      }
      # D3js
  		elsif  ( $chart_data->{chart_data_format} eq "JSON" ) {
        # Nothing
      }
    }
   
  }
  
	return $chart_ids;
}
 
sub _isize_percent {
  my $self	      = shift;
  my $XMLsource   = shift;
  my $tab	        = shift;
  
  my $xml_root        = $self->xmlroot();
  my $chart_ids			  = [];
  my @binned_percents = ();
  
  my $files = ();
  my $read_groups = ();
  my $array_heading = ["bin", ];
  print "Node Name = ".$xml_root->nodeName()."\n";
  foreach my $node ( @{$xml_root->findnodes( 'files/file' )} ) {
		my $file = {index 		  => get_attr_by_name( $node, 'index' )+0,
                path        => get_attr_by_name( $node, 'path' ), 
								#sum_count   => get_attr_by_name( $node, 'sum_count' )+0,
                failed_qc   => get_attr_by_name( $node, 'failed_qc' )+0
                #no_inserts  => get_attr_by_name( $node, 'no_inserts' )+0
                #file parent_project="Not Available" 
                #lims_identifier="CNAG_-_FASTQ_A3MCW_-_513F-B_GAGTGG_L001_BWA-backtrack" 
                #project="Not Available" 
                #primary_library="Not Available" 
                #capture_kit="Not Available" 
                #sequencing_platform="Not Available" 
                #isize_min="Not Available" 
                #sample="Not Available" 
                #id="0" 
                #isize_manually_reviewed="Not Available" 
                #path="/mnt/seq_results/smgres_special/icgc_benchmark_2013/seq_mapped/CNAG_-_FASTQ_A3MCW_-_513F-B_GAGTGG_L001_BWA-backtrack.bam.qp.xml" 
                #library_protocol="Not Available" 
                #aligner="Not Available" 
                #sum_count="19395889" 
                #species_reference_genome="Not Available" 
                #type="mapset" 
                #sample_code="Not Available" 
                #isize_max="Not Available" 
                #material="Not Available" />
							};
    push (@{$files}, $file);
    #push @{$array_heading}, basename ($file->{path}, ".bam.qp.xml");
    #print Dumper $file;
	}
  
  foreach my $node ( @{$xml_root->findnodes( 'read_groups/RG' )} ) {
    my $RG = {};
    foreach my $attr ($node->attributes) {
      $RG->{$attr->nodeName} = $attr->getValue;
    }
		#my $RG = {index 		  => get_attr_by_name( $node, 'index' )+0,
    #          file_index  => get_attr_by_name( $node, 'file_index' )+0,
    #          ID          => get_attr_by_name( $node, 'ID' )
		#					};
    #print Dumper $RG;
    
		push (@{$read_groups}, $RG);
    push @{$array_heading}, "_".$RG->{ID};
	}
  
  push @binned_percents, $array_heading;
  
  
  if ( $xml_root->exists( $self->xpath() ) ) {
    my @range_tally_items = undef;
    foreach my $root_node ( @{ $xml_root->find($self->xpath()) } ) {
      @range_tally_items = $root_node->findnodes('RangeTallyItem');
      print "\t\tRangeTallyItem: ".scalar @range_tally_items."\n";
    }
  
  
    foreach my $range_tally_item (@range_tally_items){
      my @tally_percents = ();
      push @tally_percents, get_attr_by_name( $range_tally_item, 'start' );
      my @tally_items = $range_tally_item->findnodes('TallyItem');
      foreach my $tally_item  (@tally_items){
        push @tally_percents, get_attr_by_name( $tally_item, 'percent' );
      }
      push @binned_percents, \@tally_percents;
    }  
    
    #print Dumper $binned_percents[2];
    
    foreach my $content_item ( @{$self->{config}->tabContent_content_items_ofType($tab, "chart")} ){
      my $chart_id      = $self->{config}->tabContent_contentItem_chartId($content_item);
      my $chart_data_id = $self->{config}->tabContent_contentItem_chartDataId($content_item);
      my $chart         = $self->{config}->chart($chart_id);
      my $chart_data    = $self->{config}->chart_data($chart_id);
      
      # Google Chart
  	  if ( $chart_data->{chart_data_format} eq "DataTable" ) {
  	  	# Nothing
  		}
      elsif ( $chart_data->{chart_data_format} eq "Array" ){
        $self->{page}->add_chart_data( $chart_id, \@binned_percents );
        push ( @{$chart_ids}, $chart_id );
      }
      # D3js
  		elsif  ( $chart_data->{chart_data_format} eq "JSON" ) {
        # Nothing
      }
    }
   
  }
  
	return $chart_ids;
}

#
# LOGIC
#

# TODO pod xmlroot
sub xmlroot {
    my $self	= shift;
    if ( ! $self->{xmlroot} ) {
        $self->{xmlroot} = $self->{xmlnode};
    }
    my $xmlroot  = $self->{xmlroot};

    return $xmlroot;
}

# TODO pod xmlpath
sub xmlpath {
    my $self = shift;
    return $self->{xmlpath};
}

# TODO pod xmlpath_add
sub xmlpath_add {
    my $self = shift;
    my $path = shift;
     
    if ($path) {
        push @{$self->{xmlpath}}, $path;
    }#else{
    #   $path = "XX O.o XX"; 
    #}
    #printf "\tPath after adding %s - %s\n", $path, $self->xpath();
}

# TODO pod xmlpath_drop
sub xmlpath_drop {
    my $self = shift;
	 
    pop @{$self->{xmlpath}};
    #printf "\tPath after dropping - %s\n", $self->xpath();
}

# TODO pod xpath
sub xpath {
    my $self = shift;
    my $xpath = "";
	 
    foreach my $node ( @{ $self->xmlpath() } ){
        $xpath .= sprintf "/%s", $node;
    }
    return $xpath;
}

# TODO pod _tab_content_logic
sub _tab_content_logic {
  my $self		= shift;
  my $tab		    = shift;
  my $xml_parent  = shift;
  
  #print "\n-->START tab_content_logic - $tab\n";
  my $XMLsource = $self->{config}->tabContent_xmlSource($tab) ; 
  $self->xmlpath_add($XMLsource);
  
  foreach my $tab_content_item ( @{$self->{config}->tabContent_content_items($tab)} ){
    my $tab_content_type = $self->{config}->tabContent_contentItem_type($tab_content_item);  
	  #print "\tTab content  - $tab_content_type\n";
    # sub_tabs
    if ($tab_content_type eq 'sub_tabs') {
      foreach my $sub_tab ( @{ $self->{config}->tabContent_contentItem_tabs($tab_content_item) } ) {
        #print "\t\tLogic Sub Tab: $sub_tab from $tab using XMLsource -> <$XMLsource>\n";
			  # NOTE: Recursive call
			  $self->_tab_content_logic($sub_tab, $XMLsource);
			  #$self->process_tab($XMLsource, $tab, $template);
		   } 
	   }
     # tab_content_template
		 elsif ($tab_content_type eq 'tab_content_template') {
			 my $template = $self->{config}->tabContent_ContentItem_templateId($tab_content_item);   
			 #print "\t\tTemplate: $template\n";
			 $self->process_tab($XMLsource, $tab, $template);
		 }
     # dynamic_sub_tabs	
		 elsif ($tab_content_type eq 'dynamic_sub_tabs') {
			 #print "! ! ! ! ! ! ! ! ! ! This step ($tab_content_type) is yet to be completed\n";
       my $tab_template_id = $self->{config}->tabContent_ContentItem_dynamicTabId($tab_content_item);
       my $sub_tabs = $self->dynamic_sub_tabs( $tab_template_id, $XMLsource );
       #print "Created sub tabs: ", join (', ', @{$sub_tabs}), "\n";
       $self->{config}->set_tabContent_subTabs($tab, $sub_tabs);       
     }    
       
		 else{
			 $self->process_tab($XMLsource, $tab);
		 }
	 }
	 
	 if ($XMLsource) {
		 $self->xmlpath_drop();
	 }
	 #print "<--END tab_content_logic -- $tab\n\n\n";
}


# TODO pod _create_config_tab_content
# _create_tab_from_template

sub _create_config_tab_content  {
	my $self				= shift;
	my $node_data   = shift;
	my $template_id = shift;
	my $tab_id  		= shift;
    
  #print "\t_create_config_tab_content() -- Tab Content: $tab_id - template: $template_id\n";
  my $tab_content_config = {};
	my $tab_content_template = $self->{config}->tabContentTemplate($template_id);
	
	foreach my $config_item ( keys %{$tab_content_template} ){
		#print "\t\tCCTC -- $tab_id config_item $config_item \n";
		# pane_label
		if ( $config_item eq "pane_label" and ! $tab_content_template->{$config_item} ) {
      #print "\tCCTC -- TODO: SET pane_label\n";
      $tab_content_config->{$config_item} = "";  # ?? TODO ??;
    }
		if ( $config_item eq "pane_label" ) {
      #print "\tCCTC -- TODO: SET pane_label ",$tab_content_template->{$config_item},"\n";
      $tab_content_config->{$config_item} = $tab_content_template->{$config_item};
    }
    
		# content 
		elsif ( $config_item eq "content" ){	
			#next;
      #print "\tCCTC --> content items of $tab_id\n";	 
		  foreach my $contentItem ( @{$tab_content_template->{$config_item}} ){	
        my $content_type = $self->{config}->tabContent_contentItem_type($contentItem);
        #print "\t\tcontent_type: $content_type\n";
        
        # add sub_tabs
        if ($content_type eq "sub_tabs") {
          print "\tCCTC -- sub tabs for template_id $template_id -- NOTHING DONE\n";
        }
        # add sub_tab_templates
        if ($content_type eq "sub_tab_templates") {
          #$tab_content_config->{sub_tabs} = 
          #print "\tCCTC -- sub tab templates for template_id $template_id\n";
          #my $sub_tabs = $tab_content_config->{$content_type};
					my $subtab_count = 0;
          my $sub_tabs = ();
          foreach my $sub_tab ( @{$self->{config}->tabContent_contentItem_templateTabs($contentItem)} ){
            #print "\t\t -- sub tab template? $sub_tab\n";
            my $sub_tab_id = $self->{config}->create_tab_fromTemplate($sub_tab, $tab_id, $sub_tab);
            #my $tab_id = $self->{config}->create_tab($sub_tab, $tab_id, $sub_tab);
            if ($sub_tab_id) {
              #print "\t## Added sub tab ".$subtab_count++." $sub_tab_id to $sub_tab\n";
              #print Dumper $self->{config}->tab($sub_tab_id);
              push @{$sub_tabs}, $sub_tab_id;
              
              #_tab_content_logic($tab, $xml_parent);
              my $template =$self->{config}->tabTemplate_tabContentTemplateId($sub_tab);
              #print "\t\t\ttemplate: $template - Recursive call next...\n\n";
              #print "Press 'RETURN' to continue\n"; 
              #my $wait = <STDIN>;
              
              
              $self->_create_config_tab_content($node_data, $template, $sub_tab_id);
              
              
              #print Dumper $self->{config}->tabContent($self->{config}->tab_contentId($tab_id));
              
              #print "Exiting after _create_config_tab_content\n";
              #exit;
            }
          }
          
          
          # FIXME:  ????
          if ( ! exists $tab_content_config->{$config_item} ) {
            $tab_content_config->{$config_item} = ();
          }
          push @{$tab_content_config->{$config_item}}, {type=>"sub_tabs",  tabs=>$sub_tabs};
          
          #$tab_content_config->{$config_item}{type} = "sub_tabs";
          #$tab_content_config->{$config_item}{tabs} = $sub_tabs;
          #delete $tab_content_config->{template_tabs};
          #print "Tabs\t", join ', ', @{$sub_tabs}, "\n";
          
          #print "tab_content_config\n", Dumper $tab_content_config;
        }
        
        # add contents - charts
        #elsif ($content_type eq "charts"){
        #  #print "\tCCTC -- Charts for template_id $template_id\n";
        #}
        
        # add contents - chart_templates
        elsif ($content_type eq "chart_templates") {
          #print "\tCCTC -- Chart Templates for template_id $template_id\n";
          $tab_content_config->{$config_item} = $tab_content_template->{$config_item};
        }
        
				else{
					# Nothing yet!
          print "\t\tOops -".$content_type."\n";
				}  
      }
      #print "\t//End content\n\n";
	 	}
	      
		# xml_source
    elsif ( $config_item eq "xml_source" ){
      $tab_content_config->{$config_item} = $tab_content_template->{$config_item};
    }
    
    elsif ( $config_item eq "tab_content_template_id" ){
      # Do Nothing? 
    #  print "What Now? - ", $tab_content_template->{$config_item}, "\n";
    #  #print Dumper $self->{config}->tab($tab_id);
    }
    
		else{
			print "\tCCTC -- OHH OH $config_item\n";
		}
        
	}
	
  
	#print "\nDone; Next -> add_tab_content for $tab_id\n";
  #print "\tTab from Config\n";
  #print Dumper $self->{config}->tab($tab_id);
  #print "\tGathered Config\n";
  #print Dumper $tab_content_config;
  
  #if ( ! $self->{config}->add_tabContent($tab_id, $tab_content_config) ) {
   
  if ( $self->{config}->add_tabContent($tab_id, $tab_content_config) ) {
  	#print $self->{config}->tab_contentId($tab_id), "\n";
    #print Dumper $self->{config}->tabContent( $self->{config}->tab_contentId($tab_id) );
    #print "\nPress 'RETURN' to continue to process_tab(node_data, $tab_id, $template_id)\n"; 
    #my $wait = <STDIN>;
    # Process data for tab
  	$self->process_tab($node_data, $tab_id, $template_id);
  } else {
    print "Failed to add tab content for $tab_id\n";
  }
  #print "\t// END _create_config_tab_content()\n";
}

1;
__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013,2014

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
