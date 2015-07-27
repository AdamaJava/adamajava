package PGTools::Command::Decoy;

use strict;
use PGTools::Util::Fasta;
use PGTools::Util;
use IO::File;

use parent 'PGTools::Command';


=head1 NAME

PGTools::Command::Decoy

=head1 SYNOPSIS

  ./pgtools decoy [OPTIONS] <input_fasta_file> [<output_fasta_file>]

  [OPTIONS]
    -r    or    --random

    Randomizes the sequences in the output fasta file. 
    The default behaviour is to reverse the sequence


    -a    or    --append
    If this option is set an output file need not be specified. The output is appended
    automatically to the input fasta file

  

    -k    or    --keep_accessions
    If this option is set, the accession strings are not mangled, 
    by default, rand_ or rev_ is prepended to the original accession string
    to produce new accession strings

=head1 DESCRIPTION

Decoy input databanks with either 'reverse' or 'random' method


=cut



sub run {

  my $class   = shift; 

  my $options = $class->get_options( [
    'random|r', 'append|a', 'keep_accessions|k'
  ]);

  # input fasta file
  my $ifile  = shift @ARGV;

  # ouput file is either given on command line
  # or we create a temporary file and write into it
  # then append the result back into original file
  my $ofile = $options->{append} 
    ? ( $ifile . '.tmp' ) 
    : shift( @ARGV );

  # error checks
  must_have "Input file", $ifile;
  must_be_defined "Output file", $ofile;

  my $out   = IO::File->new( $ofile, 'w+' );
  my $fasta = PGTools::Util::Fasta->new_with_file( $ifile );

  # compute the prefix for each fasta entry
  my $addent = sub {
    $options->{random} 
      ? 'rand_'
      : 'rev_';
  };

  # compute optional description line for
  # each fasta entry
  my $desc = sub {
    $options->{random}
      ? ' Random Sequence, '
      : ' Reverse Sequence, ';
  };

  # reset the seek position
  $fasta->reset;

  my $count = 0;

  debug "Preparing decoy ...";

  # Iterate over each entry
  while( $fasta->next ) {

    debug "Working on Sequence: " . $count 
      if ( ++$count % 1000 ) == 0;

    print $out ">" 
      . ( ( $options->{keep_accessions} ) ? '' : $addent->() ) 
      . $fasta->accession
      . $desc->() 
      . $fasta->description
      . $fasta->eol;

     my $seq = normalize( 
      (
        ( $options->{random} )
          ? reverse( $fasta->sequence_trimmed )
          : randomize( $fasta->sequence_trimmed ) 
      ), $fasta->eol
    );

    print $out $seq . $fasta->eol;

  }

  $fasta->close;

  if( $options->{append} ) {

    debug "Appending sequences to the original file ";
    eval {
      my $in = IO::File->new( $ifile, '>>' ) || die( "Can not open file: $ifile for appending" );

      $out->seek( 0, 0 );

      while( my $line = $out->getline ) {
        print $in $line;
      }

      close $out;
      close $in;

      unlink $ofile;


    };

    print $@ if $@;

  }

  debug "Done preparing decoy";

  exit 0;
}



1;
__END__

