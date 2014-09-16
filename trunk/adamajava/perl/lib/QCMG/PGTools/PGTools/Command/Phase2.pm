package PGTools::Command::Phase2;

use strict;
use PGTools::Util::Fasta;
use PGTools::Configuration;
use PGTools::Util;
use PGTools::Util::Path; 
use PGTools::Util::Collate;
use File::Slurp qw/ read_file /;
use File::Temp qw/tempfile/;
use File::Path qw/ make_path /;
use File::Spec::Functions;
use Data::Dumper;
use FindBin;
use Mojo::JSON;

use parent 'PGTools::Command';

# ignore warnings
no warnings;

=head1 NAME

PGTools::GenomeRun

=cut

sub run {
  my $class       = shift;
  my @files       = ();
  my $config      = PGTools::Configuration->new->config;

  # possible databases
  my @databases   = qw/
    utr   splice  noncode   pseudogene  6frame
  /;

  my @switches = qw/
    msearch fdr pepmerge collate summary
    extract group
  /;


  my @selected_databases;

  # get options
  my $options     = $class->get_options( [
    ( map { "$_" } @databases ),
    'all',
    @switches
  ]);

  my $no_options_given  = !grep { $options->{$_} } @switches;
  my $input_file  = shift @ARGV; 

  # selected databases
  if( $options->{all} || ( ! grep { $options->{$_} } @databases ) ) {
    @selected_databases = @databases;
  } else {
    @selected_databases = grep { $options->{$_} } @databases;
  }


  # get input file
  if( -f $input_file ) { 
    @files = ( $input_file );
  } 
  
  elsif( -d $input_file ) {
    @files = <$input_file/*.mgf>;
  }


  # in case there's no input file
  die "
    No files given, can't run
  " if @files == 0;

  # create hold path
  my $hold_path;
  if( @files > 0 ) {
    $hold_path = join( '_', 
      ( ( map { file_without_extension $_ } grep { $_ } ( sort { $a cmp $b } @files )[ 0 .. 3 ] ) )
    ); 
  } 


  # generate prefix to place all outputs
  # within scratch directory
  my $prefix      = catfile( 
    scratch_directory_path, 
    $hold_path
  );

  $ENV{ PGTOOLS_HOLD_PATH } = $hold_path;

  # Overall status
  $ENV{ PGTOOLS_RUN_STATUS_FILE } = catfile( $prefix, 'current_status.json' );

  create_unless_exists $prefix;

  # database names
  my %databases = (
    sixframeDB => qr/6FRAME/,
    PseudogeneID => qr/PG/,
    NonCodeDB => qr/ENS/
  );

  # colors to use
  my %colors = (
    red => qr/6FRAME/,
    green => qr/PG/,
    black => qr/ENS/
  );


  # compute_full_run
  my $compute_full_runs = @files > 1;
  my ( $six_frame, $hold_directory, $six_frame_merge_file, $six_frame_color );
  my %database_paths = %{ $config->{phase2_databases} };

  # config
  my $json        = Mojo::JSON->new;
  my $content     = strip_comments scalar( read_file( configuration_path ) );
  my $config      = $json->decode( $content ); 

  while( my ( $database_name, $database_path) = each( %database_paths ) ) {

    # database
    next unless $database_name ~~ @selected_databases;

    # verify existance
    defined_and_exists "Database $database_name :", $database_path;

    # prepare configuration
    $config->{msearch}{database} = $database_path;

    # prepare temporary config file
    my ( $fh, $filename ) = tempfile( 'configXXXXXX', SUFFIX => '.json' );

    # save file
    print $fh $json->encode( $config );

    # create new scratch directory for the output
    my $new_path = catfile( $prefix, file_without_extension( file( $database_path ) ) );
    create_unless_exists $new_path;

    # setup the environment variables for the rest of run
    local $ENV{ PGTOOLS_SCRATCH_PATH } = $new_path;
    local $ENV{ PGTOOLS_CONFIG_PATH } = $filename;

    # local status
    local $ENV{ PGTOOLS_RUN_STATUS_FILE } = catfile( $new_path, 'current_status.json' );




    # use symlinked database 
    # move merged files into common directory
    if( @files > 1 ) {

      $ENV{ PGTOOLS_USE_SYMLINKED_DATABASE } = 1;

      # In case of genome_run, this should be OK
      $hold_directory = $ENV{ PGTOOLS_CURRENT_RUN_DIRECTORY } = scratch_directory_path;

      make_path $hold_directory unless -d $hold_directory;
    }

    my $database = $config->{msearch}{database};
    my ( $name )= grep { $database =~ $databases{$_} } keys( %databases );
    my ( $color )= grep { $database =~ $colors{$_} } keys( %colors );

    my @to_run = ( );
    foreach my $file ( @files ) {

      my @runs = ();

      next unless $file;

      foreach my $command ( qw/msearch fdr pepmerge / ) {
        if( $no_options_given || $options->{$command} ) {
          push @to_run, sub {
            run_pgtool_command " $command $file ", label => $command;
          };
        }
      }

      my $bed = catfile( scratch_directory_path, file_without_extension( $file ), 'merged.BED' ); 
      my $merge = catfile( scratch_directory_path, file_without_extension( $file ), 'pepmerge.csv' ); 

      $name = $database_name; 
      $color  ||= 'red';

      push @to_run, sub {
        
        run_pgtool_command " merge2bed --merge=$merge --bed=$bed --database=$name --color=$color", label => 'merge'
          if $no_options_given || $options->{collate};

        # run extract only for 6frame dbs
        if( $database_name =~ /(6|six)frame/i ) {
          ( $six_frame, $six_frame_merge_file, $six_frame_color ) = ( 1, $bed, $color );

          if( ! $compute_full_runs && ( $no_options_given || $options->{extract} ) ) {
            run_pgtool_command " extract --novel-exon --novel-gene --overlapping-exon --overlapping-gene --outframe --bed=$bed";
          }
        }



        # temporarily
        1;

      };


    }

    # run everything
    run_serial @to_run;

    if( $compute_full_runs ) {

      # run full merge on 
      # merged files
      my @merge_files = map {
        catfile( scratch_directory_path, file_without_extension( $_ ), 'pepmerge.csv' );
      } grep { $_ } @files;

      my $output_file     = catfile( $hold_directory, 'genome_run.collate.csv' );
      my $bed_file = catfile( $hold_directory, 'genome_run.collate.BED' );

      my $full_run = PGTools::Util::Collate->new( [ @merge_files ], $output_file );

      # run
      $full_run->run if $no_options_given || $options->{collate};

      # run merge to bed
      run_pgtool_command " merge2bed --merge=$output_file --bed=$bed_file --database=$name --color=$six_frame_color";


      # run group
      # doesn't seem to work
      if( $database_name =~ /utr|noncode|pseudogene/ && ( $no_options_given || $options->{group} ) ) {
        run_pgtool_command " group $output_file";
      }


      # sixframe stuff
      if( $six_frame ) {
        if( $no_options_given || $options->{extract} ) {
          # run extract feature
          run_pgtool_command " extract --novel-exon --novel-gene --overlapping-exon --overlapping-gene --outframe --bed=$bed_file";
        }
      }

    }


    # save the old config file
    eval {

      # config file
      run_command " mv $filename $new_path/config.json";

      # obviously, needs the reset of the config path
      $ENV{ PGTOOLS_CONFIG_PATH } = "$new_path/config.json";

    };

    warn $@ if $@;


  }

  # generate summary
  $ENV{ PGTOOLS_SELECTED_DATABASES } = join( ':', @selected_databases );

  if( $no_options_given || $options->{summary} ) {
    run_pgtool_command " summary --phase=2 $input_file ";
  }


}



1;
__END__

