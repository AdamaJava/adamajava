package PGTools::Command::Translate;

use strict;
use PGTools::Util::Fasta;
use PGTools::Util::Translate;
use Getopt::Long;
use PGTools::Util;
use parent 'PGTools::Command';
use IO::File;
use Data::Dumper;

=head1 NAME

PGTools::Translate

=head1 SYNOPSIS

  ./pgtools translate [OPTIONS] <input_fasta_file> [<output_fasta_file>]

  [OPTIONS]
    -f    or    --frames

  Provide a list of frames to translate, for example -f 1,-1,2,3 by default 
  all frames are translated


=cut

sub run {
  my $class   = shift; 
  my $options = $class->get_options( [
    'frames|f=s', 'minimum|m=i'
  ]);

  my ( $ifile, $ofile ) = @ARGV;


  must_have "Input file", $ifile;
  must_be_defined "Output file", $ofile;

  my $fasta     = PGTools::Util::Fasta->new_with_file( $ifile );
  my $out       = IO::File->new( $ofile, 'w' ); 
  my $translate = PGTools::Util::Translate->new;

  my $frames  = ( $options->{frames} ) 
    ? [ map { int } map { /(-?\d)/ } split /,/, $options->{frames} ]
    : [ 1, 2, 3, -1, -2, -3 ];

  my $options->{minimum} ||= 30;

  # reset
  $fasta->reset;

  debug "About to translate ...";

  my $sequence = 0;
  while( $fasta->next ) {


    debug "Translated $sequence sequences " if ( ++$sequence % 1000 ) == 0;

    $translate->set_sequence( $fasta->sequence_trimmed );

    for my $frame ( @{ $frames } ) {

      my $translated = $translate->translate( frame => $frame );

      if( length( $translated ) >= $options->{minimum} ) {

        # Header
        print $out '>'                      .
          $fasta->accession                 .
          $translate->frame_label( $frame ) .
          $fasta->description               .
          $fasta->eol;

        print $out normalize( 
          $translated,
          "\n" 
        );

      }
    }
  }

  debug "Done translating";


  exit 0;


}

1;
__END__

