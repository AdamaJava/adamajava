package PGTools::Command::Summary;

use strict;
use PGTools::Util;
use PGTools::Util::Path;
use PGTools::Util::Static;
use PGTools::Util::AccessionHelper;
use IO::File;
use Mojo::JSON;
use Text::CSV;
use Data::Dumper;

use parent qw/
  PGTools::Command
  PGTools::SearchBase
/;

use IO::File;
use PGTools::Util::SummaryHelper;

=head1 NAME

PGTools::Command::Summary

=head1 SYNOPSIS

  ./pgtools summary <input_fasta_file> 

=head1 DESCRIPTION

This utilitiy generates summary for current proteome_run of PGTools, It
also generates visualization for merge/collate and group outputs

=cut

my $hold_directory;

sub to_run {
  my $file = shift;
  __PACKAGE__->get_runnables_with_prefix( 
    'PGTools::FDR', 
    $file, 
    { dont_cleanup => 1 }
  );
}


sub file_for {
  my $for = shift;
  my $db = shift;
  my $phase2 = shift;

  my $prefix = $phase2 ? 'genome_run' : 'proteome_run';

  my $file;
  $file = ( -e catfile( $hold_directory, $for ) )
    ? catfile( $hold_directory, $for ) 
    : ( catfile( $hold_directory, $prefix . '.' . $for  ) )
      ? catfile( $hold_directory, $prefix . '.' . $for )
      : undef;

  if( ! -e $file and ! $phase2 ) {
    my $new_file = file_for( $for, $db, 1 );

    return $file unless -e $new_file;

    return $new_file;
  }

  return $file;

}


sub generate_proteome_run_sumary {
  my $class = shift;
  my $config  = $class->config; 
  my $ifile   = $class->setup;
  my @files   = get_files( $ifile );

  # lexical global
  $hold_directory = get_hold_directory( $ifile );

  my $data = { };
  my $handlers = {

    msearch     => sub {
      $data->{msearch} = msearch_data( \@files, \&to_run, 'default' );
    }, 

    full_merge  => sub {
      full_merge_data( $data, 'default', \&file_for )
    }, 

    group       => sub {
      if( -e file_for( 'group.csv') ) {
        my $file = ( file_for('group.csv') );
        my $cmd = qq{ perl -ne 'm/(Group (?:\\d+))/; print \$1 . "\\n"' $file | sort | uniq | wc -l};
        $data->{group} = `$cmd`; 


        $data->{group_file} = $file; 
        $data->{treemap} = $class->generate_treemap( $file );

      }
    }, 

    annotate    => sub {
      if( -e file_for( 'annotate.html' ) ) {
        my $file = file_for( 'annotate.html'); 
        $data->{annotate} = `grep '</tr>' $file | wc -l`;
        $data->{annotate_file} = $file;
      }
    }
  };

  # run summary collection
  $handlers->{$_}->() 
    for keys( %$handlers );

  my $msearch = "<table class='table table-stripped table-bordered'>";

  for my $file ( @files ) {

    $msearch .= "
      <tr>
        <th> Input File </th>
        <td> $file </td>
      </tr>
    ";

    for my $run ( to_run( $file ) ) {
      $msearch .= msearch_entry_for( $file, $run, $data->{msearch}{default}{$file} );
    }

  }

  $msearch .= "</table>";

  my $html = get_header( data => $data ) . sprintf( "
    <tr>
      <th> MSearch </th>
      <td> $msearch </td>
      <td> </td>
    </tr>

    <tr>
      <th> Merge / Collate </th>
      <td> 
        <span class='badge badge-info'> %d </span>  

        <br /> <br />
        <a href='file://@{[ $data->{ merge_file } ]}'>
          View File
        </a>
      </td>

      <td> <img src='file://%s' /></td>
    </tr>

    <tr>
      <th> Group </th>
      <td> 
        <span class='badge badge-info'> %d </span> 

        <br /> <br />
        <a href='file://@{[ $data->{ group_file } ]}'>
          View File
        </a>

      </td>
      <td> %s </td> 
    </tr>

    <tr>
      <th> Annotate </th>
      <td> 
        <span class='badge badge-info'> %d </span> 

        <br /> <br />
        <a href='file://@{[ $data->{ annotate_file } ]}'>
          View File
        </a>

      </td>
      <td> </td>
    </tr>
    </table>
    </body>
    </div>
    </html>
  ", @{ $data }{ qw/ merge merge_image group / }, $data->{treemap}{html}, $data->{annotate} );


  my $output = IO::File->new( catfile( $hold_directory, 'summary.html' ), 'w' ) or die( "Can't open file for writing: " . $! );
  $output->print( $html );
  $output->close;

  print "OK \n";

}

sub generate_genome_run_summary {
  my $class   = shift;
  my $config  = $class->config; 
  my $ifile   = $class->setup;
  my @files   = get_files( $ifile );
  my @databases = get_databases;

  my $css = PGTools::Util::Static->path_for( 
    css => 'bootstrap' 
  );

  my $js = PGTools::Util::Static->path_for(
    js => 'd3'
  );

  my $data = { };

  # msearch_data
  my $scratch_directory = scratch_directory_path;
  for my $database ( @databases ) {

    # set the scratch directory path
    my $path = catfile( $scratch_directory, $ENV{ PGTOOLS_HOLD_PATH }, file_for_database( $database ) );

    {

      local $ENV{ PGTOOLS_SCRATCH_PATH } = $path;

      $hold_directory = get_hold_directory( $ifile );

      for my $file ( @files ) {
        $data->{msearch}{ $database } = msearch_data( \@files, \&to_run, $database )->{ $database };
      }
  
      full_merge_data( $data, $database, \&file_for );

      # publish extract right here
      # the bed files must most certainly be where
      # merged.BED are
      if( $database =~ /sixframe/i || $database =~ /6frame/ ) {
        my $merged_file = file_for( 'merged.BED', $database );

        unless( -e $merged_file ) {
          $merged_file = file_for( 'collate.', $database  );
        }
    
        print "FILE: $merged_file \n";

        # get extract data
        my @features = qw/
          novel.exons
          novel.genes
          outframe
          overlapping.exons
          overlapping.genes
        /;

        COLLECT_FEATURES: for my $feature ( @features ) {
          my $key = join '_', split /\./, $feature;
          my $filename = catfile( dirname( $merged_file ), file_without_extension( $merged_file ) .  "." . $feature . '.BED' );
          @{ $data->{features} }{ ( $key, $key . '_file' ) } = ( count_lines( $filename ), $filename ); 
        
        }
      }
    }
  }

  my $output_dir = catfile( scratch_directory_path, $ENV{ PGTOOLS_HOLD_PATH } ); 

  # make the path
  make_path $output_dir unless -d $output_dir;

  if( circos_is_configured ) {

    # circos here 
    my $plot_count = 0;
    my $options = join " ", 
      map {
        $plot_count++;
        "--plot$plot_count=" . $_
      } 
      grep { -e $_ } 
      map {
        $data->{ $_ }{ bed_file }
      } @databases;

    # make the command
    my $command = " visualize --circos --output=$output_dir " . $options;

    print STDERR $command;

    # run circos
    eval {
      run_pgtool_command $command;
    };

    # generate circos
    $data->{circos_error} = $@ ? 'Error Generating Circos' : undef;
    $data->{circos_plot} = catfile( $output_dir, 'circos.png' );

  }

  

  # now start generating the html file
  my $html = get_header( phase => 'Phase II', no_treemap => 1 );
  my $msearch = "<table class='table table-stripped table-bordered'>";
  my $merge   = "<table class='table table-stripped table-bordered'>";
  my $extract = "";

  while( my ( $database, $value ) = each( %{ $data->{ msearch } }) ) {

    $msearch .= "
      <tr>
        <th colspan='2'> <h3> \U$database\E </h3> </th> 
      </tr>
    ";


    # msearch output here
    while( my ( $file, $file_data) = each( %$value ) ) {

      $msearch .= "
        <tr>
          <th> Input File</th>
          <td> $file </td>
        </tr>
      ";

      my $path = catfile( $scratch_directory, 'phase2', file_for_database( $database ) );
      {

        local $ENV{ PGTOOLS_SCRATCH_PATH } = $path;

        for my $run ( to_run( $file ) ) {
          $msearch .= msearch_entry_for( $file, $run, $file_data ); 
        }

      }
    }

    print "DB: $database \n";
    if( $database =~ /sixframe/i || $database =~ /6frame/ ) {

      while( my ( $key, $value ) = each( %{ $data->{features} } )) {

        next if $key =~ /file/;

        $extract .= "
          <tr> 
            <th> $key </th> 
            <td> $value </td> 
            <td> <a href='file://@{[ $data->{features}{ $key .'_file' }]}'> View File </a> </td>
          </tr>
        "; 
      } 

      $extract = '<table>' . $extract . '</table>';

    }
  }


  while( my ( $database, $value ) = each( %{ $data } ) ) {

    next if $database eq 'msearch';

    next unless ref $value eq 'HASH';

    $merge .= "
      <tr>
        <th colspan='2'> <h3> \U$database\E </h3> </th> 
      </tr>
    ";

    # merge output here
    $merge .= "
      <tr>
        <td> 
          <span class='badge badge-info'> @{[ $value->{merge} ]} </span>  

          <br /> <br />
          <a href='file://@{[ $value->{ merge_file } ]}'>
            View File
          </a>
        </td>
        <td> <img src='file://@{[ $value->{merge_image}]}' /></td>
     </tr>
    ";
  }



  $msearch .= '</table>';
  $merge .= '</table>';

  $html .= "

    <tr>
      <th> MSearch </th>
      <td> $msearch </td>
      <td> </td>
    </tr>

    <tr>
      <th> MSearch </th>
      <td colspan='2'> 
        $merge
      </td>
    </tr>

    <tr>
      <th> Features </th>
      <td>
        <table>
          $extract
        </table>
      </td>
    </tr>

    <tr>
      <th> Circos </th>
      <td colspan='2'>
        @{[
          $data->{circos_error} || qq( <img src='file://$data->{circos_plot}' /> ) 
        ]}
      </td>
    </tr>


    </table>
    </body>
    </div>
    </html>
  ";

  # circos output here

  my $summary_file = catfile( $output_dir, 'summary.html' );
  my $output = IO::File->new( $summary_file, 'w' ) or die( "Can't open file for writing: " . $! );
  $output->print( $html );
  $output->close;

  print $summary_file, "\n";
  print "OK \n";


}

sub run { 

  my $class = shift;

  my $options = $class->get_options( [ 'phase|p=s'] );
  my $label   = 'Summary: Phase ' . ( $options->{phase} || 1 );
  my $phase   = $options->{phase} || 1;


  if( $phase == 1 ) {
    $class->generate_proteome_run_sumary;
  }

  elsif( $phase == 2 ) {
    $class->generate_genome_run_summary;
  }

}



1;
