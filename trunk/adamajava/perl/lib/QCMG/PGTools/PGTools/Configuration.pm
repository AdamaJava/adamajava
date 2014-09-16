package PGTools::Configuration;

use strict;
use FindBin;
use File::Basename qw/dirname/;
use File::Slurp qw/read_file/;
use Mojo::JSON;
use Data::Dumper;
use PGTools::Util;

=head1 NAME

PGTools::Configuration

=head1 SYNOPSIS

  my $configuration = PGTools::Configuration->new;

  say $configuration->{msearch}{defaults}

A Convinience object to access configuration parameters within "src/config.json", 
The parameters and their significance should be described else where

=cut


sub new {
  my ( $class, $file ) = @_;

  $file = _config_path() if !$file; 

  my $mojo = Mojo::JSON->new;

  my $config_text = strip_comments( 
    scalar( read_file( $file ) 
  ) );

  my $json = $mojo->decode( 
    $config_text
  );

  print $mojo->error
    if $mojo->error;


  bless {
    config  => $json
  }, $class;

}


# A Quick accessor
sub config { shift->{config}; }

sub _config_path {
  $ENV{PGTOOLS_CONFIG_PATH} || ( $FindBin::Bin . '/config.json' ); 
}




1;
__END__
