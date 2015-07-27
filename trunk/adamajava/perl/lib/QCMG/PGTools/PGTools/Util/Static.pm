package PGTools::Util::Static;

use strict;
use IO::File;
use File::Slurp;
use PGTools::Util;
use File::Spec::Functions;
use File::Basename qw/dirname/;
use Carp qw/confess/;
use Data::Dumper;

my %static = (

  css => {
    bootstrap => 'bootstrap.css' 
  },

  js => {
    jquery => 'jquery.js',
    d3     => 'd3.js'
  },

  circos => {
  }

);

sub path_for {
  my ( $class, $type, $file ) = @_;

  my $path = catfile( dirname( __FILE__), '..', '..', 'static', $type, $static{ $type }{ $file } );

  confess "$path doesn't exist" unless -f $path;

  $path;
}

sub get {
  scalar( read_file, path_for( @_ ) );
}



1;
__END__
