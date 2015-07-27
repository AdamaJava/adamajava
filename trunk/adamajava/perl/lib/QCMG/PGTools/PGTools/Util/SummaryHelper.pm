package PGTools::Util::SummaryHelper;

use strict;
use base 'Exporter';
use PGTools::Util;
use PGTools::Util::Path;
use File::Spec::Functions;
use File::Basename qw/dirname/;
use File::Path qw/make_path/;

our @EXPORT = qw/
  count_lines 
  get_files
  circos_is_configured
  get_header
  get_hold_directory
  msearch_data 
  full_merge_data
  file_for_database
  get_databases
  generate_json_from_group_file 
  generate_treemap 
  msearch_entry_for
/;

sub count_lines {
  my $file = shift;
  my ( $number ) = `wc -l $file` =~ /^\s*(\d+)/;

  $number;
}

sub get_files {
  my $ifile = shift;
  my @files;

  if( -d $ifile ) {
    @files = <$ifile/*.mgf>;
  }

  else {
    @files = ( $ifile );
  }

  @files;
}

sub circos_is_configured {
  my $config = PGTools::Configuration->new->config;

  if( -d $config->{circos_path} ) {
    return 1;
  }

  warn "No Circos configured, Skipping circos plot...\n";

  0;

}


sub get_hold_directory {
  my $ifile = shift;
   $ENV{ PGTOOLS_CURRENT_RUN_DIRECTORY } || catfile( 
    scratch_directory_path, 
    file_without_extension( $ifile )
  );
}

sub msearch_data {
  my $files = shift;
  my $to_run = shift;
  my $db = shift; 
  my $data = { };

  for my $file ( @$files ) {


    for my $run ( $to_run->( $file ) ) {

      my $target = $run->ofile( '-target.csv');
      my $decoy  = $run->ofile( '-decoy.csv' );
      my $fdr = $run->ofile( '-filtered.csv' );

      @{ $data->{$db}{$file}{ $run->name } }{ qw/target decoy fdr target_file decoy_file fdr_file/} = (
        count_lines( $target ),
        count_lines( $decoy ),
        count_lines( $fdr),
        $run->ofile( '-target.csv' ),
        $run->ofile( '-decoy.csv' ),
        $run->ofile( '-filtered.csv' )
      );

    }
  }

  $data;

}

sub full_merge_data {
  my (  $data, $db, $file_for ) = @_;
  my $file = $file_for->( 'pepmerge.csv', $db );

  unless( -e $file ) {
    $file = $file_for->( 'collate.csv', $db );
  }

  my $png_file = catfile( dirname( $file ), file_without_extension( file( $file ) ) . '.png' );

  my $d;
  if( $db eq 'default' ) {
    $d = $data;
  } else {
    $d = $data->{$db} = { };
  }

  print "File: $file \n";

  if( -e $file ) { 
    $d->{merge} = count_lines( $file ); 
    $d->{merge_file} = $file; 

    run_pgtool_command ' visualize --venn --merge-file=' . $file;

    $d->{merge_image} = $png_file;
  }

  # must be a phase2 run
  # populate with .BED file link
  if( $d ne 'default' ) {
    $d->{bed_file} = catfile( dirname( $file ), 'merged.BED' );
  }

}

sub file_for_database {
  my $config = PGTools::Configuration->new->config->{phase2_databases};
  my $database = shift;

  file_without_extension( file( $config->{ $database } ) );
}

sub get_databases {
  print Dumper \%ENV;
  if( $ENV{ PGTOOLS_SELECTED_DATABASES } ) {
    split /:/, $ENV{ PGTOOLS_SELECTED_DATABASES };
  }
}

sub get_header {
  my %options = @_;
  my $phase = $options{ phase } || ' Phase I ';
  my $data = $options{ data };

  my $css = PGTools::Util::Static->path_for( 
    css => 'bootstrap' 
  );

  my $js = PGTools::Util::Static->path_for(
    js => 'd3'
  );

  my $html = "
    <!DOCTYPE html>
    <html>
      <head>
      <link rel='stylesheet' type='text/css' href='file://$css' />

      <style>
        .row-fluid { width: 99%; margin: auto auto; } 
      </style>

      <script src='file://$js'> </script>
   ";

   unless( $options{no_treemap} ) {
     $html .= "
        <script>
          @{[ $data->{treemap}{json} ]}
        </script>

        <script>
          @{[ $data->{treemap}{script} ]}
        </script>
     ";
    }

    $html .= "
      <style>
        body {
          font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
          margin: auto;
          position: relative;
          width: 100%; 
        }

        form {
          position: absolute;
          right: 10px;
          top: 10px;
        }

        .node {
          border: solid 1px white;
          font: 10px sans-serif;
          line-height: 12px;
          overflow: hidden;
          position: absolute;
          text-indent: 2px;
        }
      
      </style>
      <body>
        <br /> <br />
        <div class='row-fluid'>
        <table class='table table-stripped table-bordered'>
          <tr>
            <th colspan='3'>
              $phase
            </th>
          </tr>
          <tr>
            <th> PGTools Module </th>
            <th> Results </th>
            <th> &nbsp; </th>
          </tr>
    ";


    $html;

}

sub generate_treemap {
  my $class = shift;
  my $group_file = shift;

  my $json = $class->generate_json_from_group_file( $group_file );

  my $script = '

window.onload = function() {
  

    var w = 800 - 80,
    h = 600 - 180,
    x = d3.scale.linear().range([0, w]),
    y = d3.scale.linear().range([0, h]),
    color = d3.scale.category20c(),
    node, root;

var treemap = d3.layout.treemap()
    .round(false)
    .size([w, h])
    .sticky(true)
    .value(function(d) { return d.size; });

    
var svg = d3.select("#treemap").append("div")
    .attr("class", "chart")
    .style("width", w + "px")
    .style("height", h + "px")
  .append("svg:svg")
    .attr("width", w)
    .attr("height", h)
  .append("svg:g")
    .attr("transform", "translate(.5,.5)");

  node = root = data; 

  var nodes = treemap.nodes(root)
      .filter(function(d) { return !d.children; });

  var cell = svg.selectAll("g")
      .data(nodes)
    .enter().append("svg:g")
      .attr("class", "cell")
      .attr("transform", function(d) { return "translate(" + d.x + "," + d.y + ")"; })
      .on("click", function(d) { return zoom(node == d.parent ? root : d.parent); });

  cell.append("svg:rect")
      .attr("width", function(d) { return d.dx - 1; })
      .attr("height", function(d) { return d.dy - 1; })
      .style("fill", function(d) { return color(d.parent.name); });

  cell.append("svg:text")
      .attr("x", function(d) { return d.dx / 2; })
      .attr("y", function(d) { return d.dy / 2; })
      .attr("dy", ".35em")
      .attr("text-anchor", "middle")
      .text(function(d) { return d.name; })
      .style("opacity", function(d) { d.w = this.getComputedTextLength(); return d.dx > d.w ? 1 : 0; });

  d3.select(window).on("click", function() { zoom(root); });

  d3.select("select").on("change", function() {
    treemap.value(this.value == "size" ? size : count).nodes(root);
    zoom(node);
  });

  function size(d) {
    return d.size;
  }

  function count(d) {
    return 1;
  }


function zoom(d) {
  var kx = w / d.dx, ky = h / d.dy;
  x.domain([d.x, d.x + d.dx]);
  y.domain([d.y, d.y + d.dy]);

  var t = svg.selectAll("g.cell").transition()
      .duration(d3.event.altKey ? 7500 : 750)
      .attr("transform", function(d) { return "translate(" + x(d.x) + "," + y(d.y) + ")"; });

  t.select("rect")
      .attr("width", function(d) { return kx * d.dx - 1; })
      .attr("height", function(d) { return ky * d.dy - 1; })

  t.select("text")
      .attr("x", function(d) { return kx * d.dx / 2; })
      .attr("y", function(d) { return ky * d.dy / 2; })
      .style("opacity", function(d) { return kx * d.dx > d.w ? 1 : 0; });

  node = d;
  d3.event.stopPropagation();
}

};

  ';

  my $html = '<div id="treemap"></div>';

  return {
    html => $html,
    json => $json,
    script => $script
  };

}

sub generate_json_from_group_file {
  my ( $class, $file ) = @_;


  my $fh = IO::File->new( $file, 'r' ) or die( "Cannot open file: $file for reading");
  my $csv = Text::CSV->new;
  my $headings = $csv->getline( $fh );
  my %data = ();

  $csv->column_names( @$headings );

  while( my $row = $csv->getline_hr( $fh ) ) {
    my $key = $row->{Group};
    my $rep = $row->{ 'Representitive-Unassigned' };

    unless( $data{ $key } ) {
      $data{ $key } = {
        size => 1,
        name => $rep ? PGTools::Util::AccessionHelper->id_from_accession( $row->{Protein} ) : ''
      };
    } else {
      $data{ $key }{ size }++;
      $data{ $key }{ name } = $rep ? PGTools::Util::AccessionHelper->id_for_accession( $row->{Protein} ) : '';
    }

  }

  'var data = ' . Mojo::JSON->new->encode( {
    name => 'Group',
    children => [
      map {
        +{
          name => $data{ $_ }{name},
          children => [
            { 
              name => $data{ $_ }{name},
              size => $data{ $_ }{size}
            }
          ]
        }
      } keys %data
    ]

  } ) . ';';
}

sub msearch_entry_for {
  my ( $file, $run, $data ) = @_;
  "
        <tr>
          <th colspan='2'>
            <span class='label label-success'> @{[ $run->name ]} </span>
          </th>
        </tr>
        <tr>
          <th rowspan='2'> Target </th>
          <td> <span class='badge badge-success'> @{[ $data->{ $run->name }{ target }]} </span></td>
        </tr>
        <tr>
          <td> 
            <a href='file://@{[ $data->{$run->name}{target_file}]}'>
              View File
            </a>
          </td>
        </tr>
        <tr>
          <th rowspan='2'> Decoy </th>
          <td> <span class='badge badge-success'> @{[ $data->{ $run->name }{ decoy }]} </span> </td>
        </tr>
        <tr>
          <td>
            <a href='file://@{[ $data->{$run->name}{decoy_file}]}'>
              View File
            </a>
          </td>
        </tr>
        <tr>
          <th rowspan='2'> FDR </th>
          <td><span class='badge badge-success'> @{[ $data->{ $run->name }{ fdr } ]}</span> </td>
        </tr>
        <tr>
          <td>
            <a href='file://@{[ $data->{$run->name}{fdr_file}]}'>
              View File
            </a>
          </td>
        </tr>
      ";
}

1;
__END__
