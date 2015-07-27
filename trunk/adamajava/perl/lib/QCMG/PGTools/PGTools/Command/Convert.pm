package PGTools::Command::Convert;

use strict;
use PGTools::Configuration;
use PGTools::Util;
use parent 'PGTools::Command';
use List::Util qw/first/;
use File::Copy qw/move/;
use File::Spec::Functions;
use Data::Dumper;

=head1 NAME

PGTools::Command::Convert

=head1 SYNOPSIS


  ./pgtools convert [OPTIONS] <input_file> <output_file> 

  [OPTIONS]
    -d    or    --dry-run

    Prints the commands that convert will run on invocation
    without actually running them


=head1 DESCRIPTION

This is a utility to convert into and from several formats. Specifically, this is used to convert input files
from mzml or mzxml to mgf format, which is common denominator amongst OMSSA, XTandem and MSGF+, But this command is
general enough for other uses.

Convert acts as a wrapper around FileConvert which is shipped with OpenMS, In order to use this command
following configuration in src/config.json needs to point to valid FileConvert executable

  "convert": { 
    "command": "/Applications/OpenMS-1.10.0/TOPP/FileConverter"
  }

No checks are conducted to verify that given path is a valid FileConvert binary, so, if you configure this with incorrect binary, the command might fail with strange error messages


=cut

sub to_command {
  my $c = shift;
  sprintf( "%s %s \n", $c->{command}, join( ' ', @{ $c->{arguments} } ) );
}

sub run {
  my $class = shift;

  my $options = $class->get_options( [ 
    'dry-run|d' 
  ] );


  my $ifile = shift @ARGV;
  my $ofile = shift @ARGV;

  must_have "Input file", $ifile;
  must_be_defined "Output file", $ofile;

  my ( $from, $to ) = ( extension( $ifile ), extension( $ofile ) );

  my %allowed_inputs  = $class->allowed_inputs;
  my %allowed_outputs = $class->allowed_outputs;
  my $config = PGTools::Configuration->new;

  # die "Does not support input type: $ifile" 
  #  unless exists( $allowed_inputs{ lc( $from ) } );

  # die "Does not support the output type: $ofile"
  #  unless exists( $allowed_outputs{ lc( $to ) } );

  die "FileConvert command is configured incorrectly"
    unless -e $config->config->{convert}{command};


  my $command = sprintf( "%s -in %s -out %s", 
    $config->config->{convert}{command},
    $ifile,
    $ofile
  );

  debug "Converting from $ifile to $ofile ";

  if( $options->{'dry-run'} ) {
    debug "$command ";
  }

  else {
    run_command $command, sub { print @_; };
  }

  debug "Done ";

}


sub allowed_inputs {
  map { 
    lc( $_ ) => $_ 
  } 
  ( 
    'mzData', 'mzXML', 'mzML', 'dta', 'dta2d', 
    'mgf', 'featureXML', 'consensusXML', 'ms2', 
    'fid', 'tsv', 'peplist', 'kroenik', 'edta' 
  );
}

sub allowed_outputs {
  map {
    lc( $_ ) => $_
  } ( 'mzData', 'mzXML', 'mzML', 'dta2d', 'mgf', 'featureXML', 'consensusXML', 'edta' );
}







1;
__END__
