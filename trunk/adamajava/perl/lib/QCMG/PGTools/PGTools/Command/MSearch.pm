package PGTools::Command::MSearch;

use strict;

use parent qw/
  PGTools::Command
  PGTools::SearchBase
/;

use Getopt::Long;
use Data::Dumper;
use PGTools;
use File::Spec::Functions;
use Cwd qw/abs_path/;
use PGTools::Util;
use PGTools::Util::Path; 

use constant FIRST => 0;



=head1 NAME

PGTools::MSearch

=head1 SYNOPSIS

  ./pgtools msearch [file.mgf|file.mzxml]

"msearch" and "fdr" commands together form two step process to take a given input and the database file,
run searchs against different algorithms, compute FDR and produce a subset of actual search.

This command barely takes in any parameters, this is extremely configurable, but most of the configuration is
done via 

  config.json

Currently OMSSA, XTandem and MSGFDB are supported algorithms. More may be added later.

The configuration terms are described here:

  defaults  - An array containing default algorithms that must be run for every search

  decoy     -     
    prepare - Boolean - Default: true - controls whether decoy files are created or not
    concat  - Boolean - Default: false - If true, a concatenated decoy database is created else, target and decoy are search separately

  algorithms - An array containing array of algorithms, each algorithm is a hash containing following properties
    name      - Name of the algorithm 
    command   - Path to the program
    database  - Path to the database
    options   - Additional options to pass to the program
    class     - Class that handles running this program ( internal )

To implement your own class implementing another search program, Following needs to be taken into consideration

  * The class must be in the namespace PGTools::MSearch
  * The class must inherit from PGTools::MSearch::Base
  * The class must implement one mandatory method "get_runnable" that returns an anonoymous subroutine which be run by the scheduler

Please take a look at documentation for PGTools::MSearch::Base for properties, methods and configuration items that you have available

=cut


sub run {
  my $class = shift;

  # We'll worry about options later
  my $options = $class->get_options( [
    'verbose|v', 'add|a=s@', 'remove|r=s@',
    'check'
  ]);


  # Remove overwrite checks completely
  my $config  = $class->config; 
  my $ifile   = $class->setup; 

  # If we don't have the right file, convert it to right format
  {
    no warnings;

    my $scratch_path  = scratch_directory_path;
    my $input_dir     = file_without_extension $ifile;
    my $extension     = extension( $ifile );
    my $new_ifile     = catfile( $scratch_path, $input_dir, file_without_extension( $ifile ) . '.mgf' );

    print "$scratch_path \n";

    my @possible_extensions = qw/
      mzData
      mzXML
      mzML
      dta
      dta2d
      ms2
      fid
      peplist
      edta
    /;

    if( $extension ne 'mgf' && grep { /$extension/i } @possible_extensions ) { 
      run_pgtool_command " convert $ifile $new_ifile ";
      $ifile = $new_ifile;
    }

    exit_with_message "MSearch requires a mgf or mzXML file"
      unless $ifile =~ /\.(mgf)$/i;

  }


  my @to_run  = $class->get_runnables_with_prefix( 
    'PGTools::MSearch', 
    $ifile, 
    $options  
  );


  # Prepare database just once
  $to_run[ FIRST ]->prepare_database;

  debug "Running: " . join( ',', map { $_->name } @to_run ) . " in parallel \n"
    if  @to_run > 1;
    
  run_parallel map { $_->get_runnable } @to_run;


}





1;
__END__
