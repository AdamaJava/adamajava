package PGTools::Command::FullRun;

use strict;
use PGTools::Util::Fasta;
use PGTools::Util;
use PGTools::Util::Path;


use File::Path qw/
  make_path
/;

use PGTools::Util::Collate;

use File::Temp;
use Data::Dumper;
use FindBin;
use Mojo::JSON;
use File::Slurp qw/ read_file /;
use File::Spec::Functions;

use parent 'PGTools::Command';

=head1 NAME

PGTools::FullRun

=head1 SYNOPSIS

  ./pgtools proteome_run <input_fasta_file>|<directory_containing_mgf_files>

=head1 DESCRIPTION

Takes a given input mgf file or a directory containing mgf files and runs follwing commands on the input files
automatically managing commands that need to run serially or in parallel. Generates a summary file at the end of the run.

If the input is file is a single mgf file, following commands are run:

  pgtools msearch input.mgf
  pgtools fdr input.mgf
  pgtools pepmerge input.mgf
  pgtools group input.mgf
  pgtools annotate input.mgf
  pgtools summary input.mgf

If the input is a directory containing multiple mgf files. Following commands are run on each mgf file: 

  pgtools msearch input.mgf
  pgtools fdr input.mgf
  pgtools merge input.mgf

Using the resultant merge file, another merge is run

  pgtools full_merge input_dir

Then following commands are run on the full merge file

  pgtools group full_merge_file
  pgtools annotate full_merge_file

Finally the full summary is generated

=cut

sub run {
  my $class       = shift;
  my @files       = ();

  # run only specific parts
  my @switches =  qw/
      msearch
      pepmerge
      fdr
      collate
      group
      annotate
      summary
  /;

  my $options           = $class->get_options( [ @switches ] );
  my $no_options_given  = !grep { $options->{$_} } @switches;
  my $input_file        = shift @ARGV; 

  if( -f $input_file ) { 
    @files = ( $input_file );
  } 

  elsif( -d $input_file ) {
    @files = <$input_file/*.mgf>;
  }

  die "
    No files given, can't run
  " if @files == 0;


  my $compute_full_runs = @files > 1;
  my $hold_directory;
  my $current_status_file = 'current_status.json';

  # use symlinked database 
  # move merged files into common directory
  if( @files > 1 ) {

    $ENV{ PGTOOLS_USE_SYMLINKED_DATABASE } = 1;

    $hold_directory = $ENV{ PGTOOLS_CURRENT_RUN_DIRECTORY } = path_within_scratch( 
      join( '_', 
        ( ( map { file_without_extension $_ } ( sort { $a cmp $b } @files )[ 0 .. 3 ] ) ) 
      ) 
    );

    make_path $hold_directory unless -d $hold_directory;

    # the current status file
    $ENV{ PGTOOLS_RUN_STATUS_FILE } = catfile( 
      $hold_directory, 
      $current_status_file 
    );

  } 

  else {

    my $dir = make_path catfile( scratch_directory_path, file_without_extension( $files[ 0 ] )  );

    # create the directory if you need to
    make_path $dir unless -d $dir;

    $ENV{ PGTOOLS_RUN_STATUS_FILE } = catfile( 
      scratch_directory_path, 
      file_without_extension( $files[ 0 ] ), 
      $current_status_file 
    );

  }

  # remove existing file
  unlink $ENV{ PGTOOLS_RUN_STATUS_FILE } if -e $ENV{ PGTOOLS_RUN_STATUS_FILE }; 
  unlink "$ENV{ PGTOOLS_RUN_STATUS_FILE }.lock" if -e "$ENV{ PGTOOLS_RUN_STATUS_FILE }.lock"; 


  print "Running Phase-I \n";

  my @to_run = ( );

  foreach my $file ( @files ) {
    my @runs = ();

    next unless $file;

    foreach my $command ( qw/msearch fdr pepmerge / ) {
      if( $no_options_given || $options->{ $command } ) {
        push @runs, sub {
          run_pgtool_command " $command $file ", label => $command;
        };
      }
    }

    unless( $compute_full_runs ) {

      my $output_file = catfile( scratch_directory_path, file_without_extension( $file ), 'pepmerge.csv' );
      my $annotate_output = catfile( scratch_directory_path, file_without_extension( $file ), 'annotate.csv' );
      my $html_output = catfile( scratch_directory_path, file_without_extension( $file ), 'annotate.html' );

      # Group
      if( $no_options_given || $options->{group} ) {
        push @runs, sub { run_pgtool_command " group $file", label => 'group'; };
      }

      # Annotate
      if( $no_options_given || $options->{annotate} ) {
        push @runs, sub { run_pgtool_command " annotate --csv --protein_id=1 $output_file $annotate_output ", label => 'annotate'; };
        push @runs, sub { run_pgtool_command " csv_to_html $annotate_output $html_output ", label => 'csv_to_html'; };
      }

    }

    @to_run = ( @to_run, @runs );

  }

  if( $compute_full_runs ) {

    # run full merge on 
    # merged files
    my @merge_files = map {
      catfile( scratch_directory_path, file_without_extension( $_ ), 'pepmerge.csv' );
    } grep { $_ } @files;

    my $output_file     = catfile( $hold_directory, 'proteome_run.collate.csv' );
    my $annotate_output = catfile( $hold_directory, 'proteome_run.annotate.csv' );
    my $html_output     = catfile( $hold_directory, 'proteome_run.annotate.html' );


    # run
    push @to_run, sub {
      run_pgtool_command " collate $input_file $output_file";
    } if $no_options_given || $options->{collate};

    # group
    push @to_run, sub {
      run_pgtool_command " group $output_file --phase=1 $input_file", label => 'group';
    } if $no_options_given || $options->{group};

    if( $no_options_given || $options->{annotate} ) {

      # annotate
      push @to_run, sub {
        run_pgtool_command " annotate --csv --protein_id=1 $output_file $annotate_output ", label => 'annotate';
      };

      # generate html output
      push @to_run, sub {
        run_pgtool_command " csv_to_html $annotate_output $html_output ", label => 'csv_to_html';
      };

    }


  }

  # summary stuff
  if( $no_options_given || $options->{summary} ) {
    # summary 
    push @to_run, sub {
      run_pgtool_command " summary $input_file ", label => 'summary';
    };
  }


  run_serial @to_run;

}



1;
__END__

