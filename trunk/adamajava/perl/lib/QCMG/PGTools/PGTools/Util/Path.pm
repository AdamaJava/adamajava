package PGTools::Util::Path;

use strict;
use base 'Exporter';
use PGTools::Configuration;
use Cwd 'abs_path';
use FindBin;
use Data::Dumper;
use PGTools::Util;
use FindBin;
use File::Spec::Functions;

use File::Copy qw/
  copy
  move
/;

use File::Path qw/
  make_path
  remove_tree
/;

use File::Basename qw/
  basename
  dirname
/;



use File::Spec::Functions;

our @EXPORT = qw{
  abs_path
  dirname
  copy
  move
  make_path
  remove_tree
  dirname
  basename
  catfile
  scratch_directory_path
  original_scratch_directory_path
  create_scratch_directory
  create_path_within_scratch
  path_within_scratch
  configuration_path
  cp 
  mv
};


my $configuration;


sub _configuration {
  ( $configuration && $configuration->config ) || ( ( $configuration = PGTools::Configuration->new ) && _configuration() );
}


sub scratch_directory_path {
  abs_path( $ENV{ PGTOOLS_SCRATCH_PATH } || _configuration->{scratch_directory} || original_scratch_directory_path()  );
}

sub original_scratch_directory_path {
  "$FindBin::Bin/../pgtools_scratch";
}


sub create_scratch_directory {
  make_path scratch_directory_path 
    unless -d scratch_directory_path;
}


sub path_within_scratch {
  catfile scratch_directory_path, shift;
}

sub configuration_path {
  PGTools::Configuration::_config_path;
}


sub create_path_within_scratch {

  my $path = path_within_scratch shift; 

  if( -d $path ) {
    # XXX: Removed this warning, should be added later on
    # after some introspection
    # warn "$path already exists";
  }

  else {
    make_path $path;
  }

}

sub mv {
  my ( $from, $to ) = @_;

  move $from, $to;

}

sub cp {
  my ( $from, $to ) = @_;

  copy $from, $to;
}




1;
__END__

