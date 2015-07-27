package PGTools::Command::Visualize::Bar;

use strict;
use PGTools::Util;
use PGTools::Util::Path;
use PGTools::Configuration;
use Text::CSV;
use IO::File;
use Data::Dumper;
use File::Spec::Functions;
use File::Basename qw/dirname/;
use GD::Graph::bars;
use parent 'PGTools::Command';

sub run {
  my $class = shift;
  my $config = PGTools::Configuration->new->config;
  my $options = shift;
  my $input = $options->{ 'group-file'};

  must_have "Group File", $input;

  my $fh      = IO::File->new( $input, 'r'  );
  my $csv     = Text::CSV->new;
  my $output  = catfile( dirname( $input ), file_without_extension( file( $input ) ) . '.png' );
  my $bar     = GD::Graph::bars->new( 400, 400 );

  print "$input \n";

  $bar->set(
    x_label => 'Protein Groups',
    y_label => 'Proteins',
    title   => "Protein Group: $input"
  );


  my $data    = { };
  my @heading = @{ $csv->getline( $fh ) };

  # set column names
  $csv->column_names( @heading );

  while( my $row = $csv->getline_hr( $fh ) ) {
    my $group = $row->{Group};

    $data->{ $group } = 0 unless exists( $data->{ $group } );

    $data->{ $group }++;
  }

  my $count = 0;
  my @rows = ( [], [] );
  while( my ( $key, $value ) = each( %$data ) ) {
    push @{ $rows[0] }, ( $key =~ /(\d+)/ );
    push @{ $rows[1] }, $value;

    last if ++$count > 20;
  }

  my $gdh = $bar->plot( \@rows ) or die( $bar->error );

  open my $ofh, '>', $output or die( "Can't open file: $output for writing: $!");
  binmode( $ofh );
  print $ofh $gdh->png;
  close( $ofh );

  print "output placed at: $output \n";
  print "DONE \n"

}



1;
__END__
