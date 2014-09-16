package QCMG::HTML::BootstrapPage;

##############################################################################
#
#  Module:   QCMG::HTML:BootstrapPage.pm
#  Author:   Matthew J Anderson
#  Created:  2013-07-04
#
#  $Id: BootstrapPage.pm 4676 2014-08-07 02:13:29Z m.anderson $
#
##############################################################################

use strict;
use warnings;

use Data::Dumper;
use File::Basename;
use Pod::Usage;
use Carp qw( carp croak confess );
use JSON qw( encode_json );         # From CPAN

#use QCMG::IO::JsonReader;
use QCMG::Google::DataTable;
use QCMG::Util::QLog;

use vars qw( $SVNID $REVISION  );

( $REVISION ) = '$Revision: 4676 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: BootstrapPage.pm 4676 2014-08-07 02:13:29Z m.anderson $'
    =~ /\$Id:\s+(.*)\s+/;

sub new {
    my $class  = shift;
    my %params = @_;
    
    my $self = { file           => $params{file},
                 config         => $params{config},     # JSON conig input file.
                 
                 page_meta      => {},      # metadata for the page header. later parsed for qReports
                 summary        => {},
                 chart_data     => {},      # Generated json chart data from reports
                 tab_contents   => {},      # Content found for the tab.
                 
                 verbose        => $params{verbose} || 0 };
    bless $self, $class;
    
}

sub file {
    my $self = shift;
    return $self->{file};
}

#sub config {
#    my $self = shift;
#    return $self->{config};
#}

sub page_meta {
    my $self = shift;
    return $self->{page_meta};
}

sub summary {
    my $self = shift;
    return $self->{summary};
}

sub summary_tab {
    my $self = shift;
    
    if ( exists $self->{summary}->{tab} ) {
       return $self->{summary}->{tab};
    }
    return '';
}

sub summary_source {
    my $self = shift;
    
    if ( exists $self->{summary}->{source} ) {
       return $self->{summary}->{source};
    }
    return '';
}

sub chart_data {
    my $self = shift;
    return $self->{chart_data};
}

sub tab_contents  {
    my $self    = shift;
    my $tab     = shift if @_;
    
    if ($tab) {
        return $self->{title}->{$tab};
    }
    return $self->{tab_contents};
}

## ADD

sub add_page_meta {
    my $self    = shift;
    my $name    = shift;
    my $content = shift;

    $self->{page_meta}->{$name} = $content;
}

sub add_tab_table {
    my $self        = shift;
    my $tab         = shift;
    my $table_id    = shift;
        
    $self->{tab_contents}->{$tab}->{tables}->{$table_id} = { headers => [], rows => [] };
}

sub add_tab_table_row {
    my $self        = shift;
    my $tab         = shift;
    my $table_id    = shift;
    my $row         = shift;
    
    push @{$self->{tab_contents}->{$tab}->{tables}->{$table_id}->{rows}}, $row;
}


sub add_summary_tab(){
    my $self    = shift;
    my $tab     = shift;
    
    $self->{summary}->{tab} = $tab;
    
}

sub add_summary_source {
    my $self        = shift;
    my $entry_id    = shift;
    my $entry       = shift;
    
    #print "$entry_id\n";
    $self->{summary}->{source}->{$entry_id} = $entry;
}

sub add_tab_charts(){
    my $self        = shift;
    my $tab         = shift;
    my $chart_ids   = shift;
    
    
    if ($chart_ids) {
        #print "adding chart $tab\n";
        $self->{tab_contents}->{$tab}->{charts} = $chart_ids;
    }    
}

sub add_chart_data(){
    my $self        = shift;
    my $chart_id    = shift;
    my $chart_data  = shift;
    
    if ($chart_data) {
        #print Dumper $chart_data;
        $self->{chart_data}->{$chart_id} = $chart_data;
    }else{
      print "[ERROR] - No chart data to add!\n";
    }    
}


sub add_tab_text {
    my $self        = shift;
    my $tab         = shift;
    my $text        = shift;
    
    if ( ! exists $self->{tab_contents}->{$tab}->{text} ) {
        $self->{tab_contents}->{$tab}->{text} = [];
    }
    push @{$self->{tab_contents}->{$tab}->{text}}, $text;
}

#
# Build HTML markup
#


sub html {
    my $self = shift;
    
	return sprintf ('
<!DOCTYPE html>
<html lang="en">
	<!-- Head --> %s 	<!-- End Head -->
	
	<!-- Body --> %s 	<!-- End Body -->
</html>
',
	$self->_page_header(),
	$self->_page_body()
	);
}

sub _header_metadata {
	my $self            = shift;
    my $meta_headers    = "";

    my $metadata = $self->page_meta();    
    my @meta_keys = sort keys %{ $metadata };
    if (scalar @meta_keys) {
        foreach my $key (@meta_keys) {
            $meta_headers .= sprintf('<meta name="%s" content="%s">', 
                                    $key, $metadata->{$key});
        }
    }

    return $meta_headers;
}


sub _page_header {
	my $self    = shift;
  my $title   = $self->{config}->report("title") . ' - ' . $self->file() ;

	return sprintf ('
	<head>
		<title> %s </title>

    <!-- Meta --> %s <!-- End Meta -->

		<!-- CSS -->
        %s
    <!-- End CSS -->
	</head>
    ', 
	  $title, 
    $self->_header_metadata(),
    $self->_page_load_stylesheets()
  );
	
}

sub _page_body {
  my $self        = shift;
    
	my $program 	= $self->{config}->report("name"); 
  my $title	 	  = fileparse($self->file(), ".bam.qcov.xml" ); # FIXME ?
  
  my $html_nav_tabs       = "";
  my $html_tab_contents   = "";
  my $active              = 1; 
  
  # for each tab
  #   if tab content
  #       add tab
  #       add tab content
  
  my $nav_tabs = $self->{config}->navTabs();
      
  foreach my $tab ( @{$nav_tabs} ){
    #print "nav tab: $tab\n";
    $active = 0 if $html_tab_contents;
    if ( $self->_has_tab_content($tab) ) {
        $html_nav_tabs .= $self->_add_tab($tab, $active);
        $html_tab_contents .= $self->_add_tab_content($tab, $active);
    }
  }
  
  my $body_content = '';
  my $nav_location =  $self->{config}->layout("menu");
  print "nav_location $nav_location\n";
  #Top
  if ( $nav_location eq "top" ) {
    my $nav_tab = sprintf('
      <nav class="navbar navbar-default navbar-static-top" role="navigation">
        <div class="container-fluid">
          <!-- Brand and toggle get grouped for better mobile display -->
          <div class="navbar-header">
      <!-- <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
              <span class="sr-only">Toggle navigation</span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
              <span class="icon-bar"></span>
            </button>
      -->
            <span class="navbar-brand">%s</span>
          </div>
          <!-- Collect the nav links, forms, and other content for toggling -->
          <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
            <ul class="nav navbar-nav">
              %s
            </ul>
          </div>  <!-- /.navbar-collapse -->
        </div>  <!-- /.container-fluid -->
      </nav>
      ',
      $program,
      $html_nav_tabs
    ); # nav_tab
    
    $body_content = sprintf('
    	<div id="%s" class="" >
  			<!-- Top Nav--> %s
  			<!-- End Top Nav-->
      
    		<div class="report-title">
    			<span class="file">%s</span>
    		</div>
        <div class="report-content container-fluid">
          <div class="row">
      					<!-- Top Nav Content --> %s
      					<!-- End Top Nav Content -->	
    	      </div>
          </div>
        </div>
          
    	</div>
      ',
    	$program,
      $nav_tab,
    	$title,
    	$self->_wrap_tab_contents($html_tab_contents)
    ); # body_content
  }
  # Default for many tabs
  else{
    $body_content = sprintf('
    	<div id="%s" class="my-fluid-container" >

    		<div class="report-title">
    			<span class="file">%s</span>
    			<h3 class="report">%s</h3>
    		</div>
        <div class="report-content container-fluid">
          <div class="row">
          			<!-- Top Nav--> %s
          			<!-- End Top Nav-->
      					<!-- Top Nav Content --> %s
      					<!-- End Top Nav Content -->	
    	      </div>
          </div>
        </div>
          
    	</div>
    ',
  	$program,
  	$title,
  	$program,
  	$self->_wrap_tabs($html_nav_tabs),
  	$self->_wrap_tab_contents($html_tab_contents)
    );
  }


    
	my $body = sprintf ('
	<body>
		%s
    <!-- Javascript -->  
      <!-- Chart data --> %s <!-- End Chart data -->
      
      %s
    <!-- End Javascript -->
	</body>	
  ', 
	$body_content,
  $self->_page_load_javascript(),
  $self->_page_load_chartdata()
	);
  
  return $body;
}


sub _construct_javascript {
  my $self                = shift;
  my $javascript_libarary = shift;
  
  my $src   = $javascript_libarary->{source};
  
  if ( exists $javascript_libarary->{arguments} ) {
    my $json  = JSON->new->allow_nonref;
    $src .= $json->encode( $javascript_libarary->{arguments} );
    #print "$src\n";
  }  
  return sprintf ('<script type="%s" charset="%s"  src=\'%s\'></script>
    ',
    $javascript_libarary->{type},
    $javascript_libarary->{charset},
    $src
    );
}

sub _construct_stylesheets {
  my $self                = shift;
  my $stylesheet = shift;

  return sprintf ('<link rel="stylesheet" type="%s" charset="%s"  href=\'%s\'>
    ',
    $stylesheet->{type},
    $stylesheet->{charset},
    $stylesheet->{source}
    );
}

sub _page_load_javascript {
	my $self        = shift;
  my $javascript_libraries = '';

  foreach my $lib ( @{$self->{config}->javascript_libraries()} ){
    $javascript_libraries .= $self->_construct_javascript($lib);
  }
  #print $javascript_libraries,"\n";
  return $javascript_libraries;   
}

sub _page_load_stylesheets {
	my $self        = shift;
  my $stylesheets = '';

  foreach my $style ( @{$self->{config}->stylesheets()} ){
    $stylesheets .= $self->_construct_stylesheets($style);
  }
  return $stylesheets;   
}


#sub _page_load_javascript_UI_libraries {
#	my $self        = shift;
#    
#    return sprintf ('         
#    ');
#}
#
#sub _page_load_javascript_chart_libraries {
#	my $self        = shift;
#    
#    return sprintf ('
#    ');
#}


sub _page_load_chartdata {
    my $self        = shift;
    #my $json        = JSON->new->allow_nonref;
    #my $json        = JSON->new->utf8->
    
    #print "_page_load_chartdata        _page_load_chartdata        _page_load_chartdata\n";
    
    foreach my $chart_id ( keys %{$self->{chart_data}} ){
        #print "chart_id: $chart_id\n";
        my $chart_in_config = $self->{config}->chart($chart_id);
        
        if ( defined $chart_in_config ) {
            #print "exists\t chart_data for $chart_id\n";
            #print Dumper $self->{chart_data}->{$chart_id};
            $self->{config}->add_chartData($chart_id, $self->{chart_data}->{$chart_id}); 
            #$self->{config}{config}{chart_data}{$chart_id}{data} = $self->{chart_data}->{$chart_id};
        } 
        #print       
        #my $data_as_json = $json->pretty->encode( $self->{chart_data}->{$chart_id} );
        #     #print $json->encode( $chart_data->{$chart} );
        #$chart_data .= sprintf ("\nvar %s = %s;\n", $chart_id, $data_as_json); 
        
        
         
    } 
    
    #print Dumper $self->{config}{config}{chart};
    #foreach my $chart_id ( keys %{ $self->{config}{config}{chart} } ){
    #    print "Chart Data: $chart_id\n";
    #    print Dumper $self->{config}{config}{chart_data}{$chart_id};
    #}
     
        #$json_text = encode_json $perl_scalar
    #my $charts      = $json->encode( $self->{config}{config}{chart} );
    #my $chart_data  = $json->encode( $self->{config}{config}{chart_data} );  
    my $charts      = encode_json $self->{config}{config}{chart};
    my $chart_data  = encode_json $self->{config}{config}{chart_data};  
    
    
    
    $chart_data =~ s/\"(\d+|\d+\.\d+)\"/$1/g; # Make numbers not strings
    $chart_data =~ s/{\"v\":\"(\d+)\"/{\"v\":$1/g; 
    #print "$chart_data\n";
    return sprintf ('<script> 
        var charts = %s;
        var chart_data = %s;
        
        </script>', 
        $charts,
        $chart_data
     );
}


#
# Content
# 

sub _wrap_tabs {
  my $self        = shift;
  my $nav_tabs    = shift;
  
  if ($nav_tabs) {
      $nav_tabs = sprintf ('
 					<div class="navbar navbar-default" role="navigation">
 						<div class="navbar-header">
 							<ul class="nav navbar-nav">
                  %s
 							</ul>
 						</div>
 					</div>	
          ',
          $nav_tabs
      );
  }
  
  return $nav_tabs;
}

sub _wrap_sub_tabs {
  my $self        = shift;
  my $nav_tabs    = shift;
  my $layout      = shift;
  
  if ($nav_tabs) {
    my $classes = "nav nav-pills"; # nav nav-pills nav-tabs
    if ($layout and $layout eq "stacked") {
      $classes = "nav nav-pills nav-stacked";
    }    
    $nav_tabs = sprintf ('
				<ul class="%s">%s								
				</ul>
        ',
        $classes,
        $nav_tabs
    );
  }

  return $nav_tabs;
}


sub _wrap_tab_contents {
  my $self            = shift;
  my $tab_contents    = shift;
  
  if ($tab_contents){
    #print Dumper $tab_contents;
      $tab_contents = sprintf ('
  			<div class="tab-content">
  				%s
  			</div>',
  			$tab_contents
      );	
  }
  else{
    print "[ WARNING ] No tab content avalible\n";
  }
}


sub _has_tab_content{
  my $self        = shift;
  my $tab         = shift;    
  my $has_content = 0;
  
  #print "_has_tab_content($tab)\t";
  #print $self->{config}->tab_contentId($tab);
  #print Dumper $self->{config}->tabContent($self->{config}->tab_contentId($tab));
  #print "\n";
  
  foreach my $content_item ( @{ $self->{config}->tabContent_content_items($tab) } ){ 
    my $content_type = $self->{config}->tabContent_contentItem_type($content_item);

    # sub_tabs
    if ( $content_type eq "sub_tabs" ) {
      foreach my $sub_tab ( @{ $self->{config}->tabContent_contentItem_tabs($content_item) } ){
        $has_content += $self->_has_tab_content($sub_tab);
      }  
    }
    # chart_data, tables, etc
    else{
      $has_content += 1 if exists $self->{tab_contents}->{$tab};
    }
    
  }
  return $has_content;
}


# Add 
sub _add_tab_content {
    my $self        = shift;
    my $tab         = shift;
    my $active      = shift;
    
    my $active_pane = $active ? "in active" : "";
    my $pane_label  = "";
    my $display_content = "";
    
    #my $tab_content = $self->{config}->tabContent($tab);
    #print "\t_add_tab_content($tab)\n";
    #print Dumper $self->{config}->tabContent($tab);
    #print Dumper $self->{config}->tabContent_content_hasType($tab, "sub_tabs");
    
    if ( $self->{config}->tabContent_content_hasType($tab, "sub_tabs") ) {
        #print "\t=>content_type sub_tabs\n";
        $display_content .= $self->_add_sub_tabs( $tab );    
    }
    else{
        $pane_label = sprintf ("<h2>%s</h2>", $self->{config}->tabContent_paneLabel($tab) );
        foreach my $content_items ( @{ $self->{config}->tabContent_content_items($tab) } ){
            #print "TAB: $tab -- content_items with content_type ", $self->{config}->tabContent_contentItem_type($content_items), "\n";
            if ( exists $self->{tab_contents}->{$tab} ) {
                #print Dumper $self->{tab_contents}->{$tab};
                my $content = $self->{tab_contents}->{$tab};
            #    #print "tab content\n";
            #    #print Dumper $content;
                if ( exists $content->{charts} ) {
                    #print "\t_add_tab_content: _add_chart_placeholder\t";
                    foreach my $chart_id ( @{$content->{charts}} ) {
                        #print "\t$chart_id";
                        $display_content .= $self->_add_chart_placeholder($chart_id); 
                    }
                    #print "\n";
                }
                elsif ( exists $content->{tables} ) {
                   foreach my $table_id ( keys %{$content->{tables}} ) {
                        my $table_content = $content->{tables}->{$table_id};
                        $display_content .= $self->_add_table($table_id, $table_content);
                    } 
                    
                }
                elsif ( exists $content->{text} ) {
                    foreach my $text ( @{$content->{text}} ) {
                        #print "Adding Text for $tab\n";
                        $display_content .= $self->_add_text($text);
                    } 
                }
                else{
                    # Nothing yet.
                }     
            }
          }
          $display_content = sprintf('
            <div class="col-lg-12">
              %s<hr />
              %s
            </div>
          ',$pane_label ,$display_content);
    }
    
	return sprintf('
			<!-- %s -->
			<div class="tab-pane fade %s" id="%s">
				<div class="tab-content">
          %s
				</div>
			</div>
			',
			$pane_label,
			$active_pane, $tab,
			$display_content
	);
}


sub _add_tab {
  my $self        = shift;
  my $tab         = shift;
  my $active      = shift;
  
  my $active_tab = $active ? "active" : "";    
	my $tab_label = $tab;
	if ( $self->{config}->tab($tab) ){
        #print "Tab has details\n";
		$tab_label = $self->{config}->tab_label($tab);
	}
    #print "active_tab $active_tab\n";
    #print "tab $tab\n";
    #print "tab_label $tab_label\n";
    
	return sprintf('
    <li class="%s"> <a href="#%s" data-toggle="tab">%s</a> </li>
    ', $active_tab, $tab, $tab_label
  );    
}

sub _add_sub_tabs {
    my $self        = shift;
    my $tab         = shift;
    
    my $html_nav_tabs       = "";
    my $html_tab_contents   = "";
    my $active              = 1; 
    my $layout              = "";
    my $content             = "";
    
    my $content_items = $self->{config}->tabContent_content_items_ofType($tab, "sub_tabs");
    #print "\t\t_add_sub_tabs($tab) \n";
    ## only one array element should be returned.
    die "We can't have more then one set of sub_tabs for a tab." 
      unless scalar @{$content_items} == 1;
    
    
    #
    #$self->{config}->tabContent_contentItem_tabs($content_item);
    foreach my $content_item (@{$content_items}){
      $layout = $self->{config}->tabContent_contentItem_valueOf($content_item, "layout");
      #print Dumper $content_item;
      #print Dumper $self->{config}->tabContent_contentItem_tabs($content_item);
      foreach my $sub_tab ( @{ $self->{config}->tabContent_contentItem_tabs($content_item) } ){
        #print Dumper $tab;
          #print "\t\tSub tab $tab\n";
          $active = 0 if $html_tab_contents;
          if ( $self->_has_tab_content($sub_tab) ) {
              $html_nav_tabs .= $self->_add_tab($sub_tab, $active);
              #print "\t\t\t-- Sub tab content being added\n";
              $html_tab_contents .= $self->_add_tab_content($sub_tab, $active);
          }
          #print "\t\t^-End sub tab\n";
      } 
    }
    
    if ($layout eq "stacked") {
      $content = sprintf (
        '<div class="col-lg-2"> %s </div>
         <div class="col-lg-10"> %s </div>',    
    	   $self->_wrap_sub_tabs($html_nav_tabs, $layout),
    	   $self->_wrap_tab_contents($html_tab_contents)
      );
    }
    else{
      $content = sprintf (
        '<div class="col-lg-12"> %s </div>
         <div class="col-lg-12"> %s </div>',    
    	   $self->_wrap_sub_tabs($html_nav_tabs, $layout),
    	   $self->_wrap_tab_contents($html_tab_contents)
      );
    }
    
    return $content;

}

sub _add_chart_placeholder {
    my $self        = shift;
    my $chart_id    = shift;
    
    #print "\t_add_chart_placeholder($chart_id)\n";
    
    return sprintf('
    <figure id="%s" class="chart">%s</figure>', 
    $chart_id,
    '<div class="loading">
      <svg class="icon-loading loading-bubbles" xmlns="http://www.w3.org/2000/svg" width="72" height="24" fill="white" viewbox="0 0 0 0">
        <circle transform="translate(12 0)" cx="0" cy="12" r="0"> 
          <animate attributeName="r" values="0; 6; 0; 0" dur="1.2s" repeatCount="indefinite" begin="0" keyTimes="0;0.2;0.7;1" keySplines="0.2 0.2 0.4 0.8;0.2 0.6 0.4 0.8;0.2 0.6 0.4 0.8" calcMode="spline"></animate>
        </circle>
        <circle transform="translate(36 0)" cx="0" cy="12" r="0"> 
          <animate attributeName="r" values="0; 6; 0; 0" dur="1.2s" repeatCount="indefinite" begin="0.3" keyTimes="0;0.2;0.7;1" keySplines="0.2 0.2 0.4 0.8;0.2 0.6 0.4 0.8;0.2 0.6 0.4 0.8" calcMode="spline"></animate>
        </circle>
        <circle transform="translate(60 0)" cx="0" cy="12" r="0"> 
          <animate attributeName="r" values="0; 6; 0; 0" dur="1.2s" repeatCount="indefinite" begin="0.6" keyTimes="0;0.2;0.7;1" keySplines="0.2 0.2 0.4 0.8;0.2 0.6 0.4 0.8;0.2 0.6 0.4 0.8" calcMode="spline"></animate>
        </circle>
      </svg>
      <h2>Loading...</h2> 
    </div>'
    ) if $chart_id; 
    #      <img src="http://qcmg-clustermk2.imb.uq.edu.au/reports/qvisualise/svg/loading_bubbles.svg" alt="Loading icon" type="image/svg+xml"/>

    return '';
}

sub _add_table{
    my $self            = shift;
    my $table_id        = shift;
    my $table_content   = shift;
    
    #print "\t_add_table($table_id, content ...)\n"; 
    
    my $table = $self->{config}->table($table_id);
        
    my $table_headers   = "";
    my $table_rows      = "";
    my $column_count    = scalar @{$table->{table_columns}};
    my $column_format   = [];
    
    # Table headers and data column formats
    for (my $column = 0; $column < $column_count; $column++) {
      my $column_header = $table->{table_columns}->[$column];
      my $title = "";
      my $classes = "";
      
      if ( exists $column_header->{title} ) {
          $title = $column_header->{title};
      }
              
      if ( exists $column_header->{data_type} ) {
          if ($column_header->{data_type} eq "number"){
              $classes = "number";
          }elsif ($column_header->{data_type} eq "percentage"){
              $classes = "percentage";
          }
      }
      
      $table_headers .= sprintf ('<th>%s</th>', $title);
      $column_format->[$column] = {
         classes => $classes 
      }
    }
    $table_headers = sprintf ('<tr>%s</tr>', $table_headers); 
    
    foreach my $data_row ( @{$table_content->{rows}} ){ 
        my $table_row_column = "";

        for (my $column = 0; $column < $column_count; $column++) {            
            my $data_column = $data_row->[$column];
            
            if ( $column_format->[$column]->{classes} eq "percentage") {
              $data_column = commify(sprintf "%.0f", $data_column).'%';
            }
            elsif ( $column_format->[$column]->{classes} eq "number" ) {
              $data_column = commify($data_column);
            }
            
            $table_row_column .= sprintf (
                '<td class="%s">%s</td>', 
                $column_format->[$column]->{classes}, 
                $data_column
            );
        }
        $table_rows .= sprintf ('<tr>%s</tr>', $table_row_column);
    }
    
    if ($table_content) {
         return sprintf '
         <table id="%s" class="table table-hover">
             <thead>%s</thead>
             <tbody>%s</tbody>
         </table>', 
         $table_id, 
         $table_headers,
         $table_rows
    }
    
    return '';
   
}


sub _add_text {
    my $self    = shift;
    my $text    = shift;
    
    if ($text) {
        $text =~ s/\n/<br \/>/g;
        return sprintf('<div class="">%s</div>', $text); 
    }
    
    return '';
}

=cut

  Utils

=cut

sub commify {
    my $text = reverse $_[0];
    $text =~ s/(\d\d\d)(?=\d)(?!\d*\.)/$1,/g;
    return scalar reverse $text;
}



1;
__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2013-2014

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
