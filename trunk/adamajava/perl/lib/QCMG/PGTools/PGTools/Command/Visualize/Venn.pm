package PGTools::Command::Visualize::Venn;

use strict;
use PGTools::Util;
use PGTools::Util::Path;
use PGTools::Configuration;
use Venn::Chart;
use Text::CSV;
use IO::File;
use File::Basename qw/dirname/;
use Data::Dumper;
use File::Spec::Functions;
use parent 'PGTools::Command';

sub run {

  my $class = shift;
  my $config = PGTools::Configuration->new->config;
  my $options = shift;
  my $input = $options->{ 'merge-file'};
  my $venn  = Venn::Chart->new( 800, 600 ) or die( "Error: $!");

  $venn->set_options( -title => "Merge: $input" );

  must_have "Merge File", $input;

  my $fh    = IO::File->new( $input, 'r'  );
  my $csv   = Text::CSV->new;
  my $data  = { };
  my $output = catfile( dirname( $input ), file_without_extension( file( $input ) ) . '.png' );

  my @heading = @{ $csv->getline( $fh ) };
  my @engines = ();

  die "Looks like the merge file: $input is empty" unless scalar( @heading );

  # setup data
  $data->{ $_ } = [ ], push( @engines, $_ ) for ( @heading[ 2 .. $#heading ] );

  # set legends
  $venn->set_legends( @engines );

  # set column names
  $csv->column_names( @heading );

  # prepare data
  while( my $row = $csv->getline_hr( $fh ) ) {
    for my $engine ( @engines ) {
      if( $row->{$engine} == 1 ) {
        push @{ $data->{ $engine } }, ( $row->{Peptide} || $row->{peptide} );
      }
    }
  }

  # plot
  my $gdh = $venn->plot( 
    map {
      $data->{ $_ }
    } @engines
  );


  open my $ofh, '>', $output or die( "Can't open file: $output for writing: $!");
  binmode( $ofh );
  print $ofh $gdh->png;
  close( $ofh );


  print "output placed at: $output \n";
  print "DONE \n"


}



1;
__END__
