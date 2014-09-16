package PGTools::Command::Visualize;

use strict;
use PGTools::Util;
use parent 'PGTools::Command';
use Data::Dumper;

use PGTools::Command::Visualize::Circos;
use PGTools::Command::Visualize::Venn;
use PGTools::Command::Visualize::Bar;
=head1 NAME

PGTools::Command::Visualize

=head1 SYNOPSIS

  ./pgtools visualize [OPTIONS]

  [OPTIONS]
    --venn          Generate venn diagram for the input file, The input needs to be merge output
                    and must be supplied. Required --merge-file option to be set to a valid merge file

    --merge-file    Merge file, this is the input used by PGTools::Visualize::Venn to generate venn charts


    --circos        Generate circos visualization ( http://circos.ca/ ) for several BED files. Its a thin wrapper
                    with very little ability to customize the kind of plots that can be generated. The only purpose
                    is to simplify and automate generating some of the more common plots

    --plot1 --plot2 --plot3   Must be either text file containing circos data or BED files, which are automatically converted to 
                              correct format before running circos


=head1 DESCRIPTION

General command to help visualize some output from PGTools, Current generates circos plots and venn diagrams. TreeMap visualization
is built right into summary files.

=cut


sub run  {
  my $class = shift;

  my @options = ( 
    'circos', 
     ( map { "plot$_" . '|' . "p$_" . '=s'} ( 1 .. 8 ) ),
    'output=s',

    'venn', 
    'merge-file=s',

    'bar',
    'group-file=s'


  );

  my $options = $class->get_options( 
    [ @options ]
  );

  # run
  for my $type ( qw/ circos venn bar/ ) {
    if( $options->{ $type } ) {
      _run( $type, $options );
      return;
    }
  }

}

sub _run {
  my ( $class, $options ) = @_;

  eval {
    ( 'PGTools::Command::Visualize::' . ucfirst( $class ) )->run( $options );
  };

  warn $@ if $@;

}


1;
__DATA__
