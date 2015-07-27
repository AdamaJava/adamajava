package PGTools::MSearch::Base;

use strict;

use File::Spec::Functions;

use File::Path qw/ 
  make_path 
  remove_tree
/;

use Data::Dumper;
use Cwd 'abs_path';


use PGTools::Util::Path qw/
  cp
  mv
/;

use PGTools::Util qw/
  extension
  file
  file_without_extension
  create_unless_exists
  run_pgtool_command
  run_parallel
  run_command
  directory
/;

use File::Spec::Functions;

sub new {
  my ( $class, %options ) = @_;

  my $self = bless {
    (
      map {
        $_ => $options{ $_ }
      } grep {
        exists( $options{ $_ } ) && $options{ $_ }
      } qw{ config ifile scratch_path msearch_config}
    )
  }, $class;


  $self;

}


sub my_scratch_path {
  my $self = shift;

  # Path for us to use 
  catfile( $self->{scratch_path},  $self->{config}->{scratch_path} || $self->{config}->{class} );
}


sub scratch_database_path {
  my $self = shift;

  $self->path_for( '../database' );
}

sub prepare_database_path {

  my $self = shift;

  create_unless_exists $self->scratch_database_path;

  $self;

}



sub setup_path {
  my $self = shift;

  create_unless_exists $self->$_ for qw/
    my_scratch_path 
    scratch_database_path
  /; 

  $self;
}




sub cleanup_path {
  my $self = shift;

  print "**WARNING**: Cleaning up the path \n";

  for ( qw/my_scratch_path scratch_database_path/ ) {
    remove_tree $self->$_ if -d $self->$_;
  }

  $self;


}

BEGIN {

  {
    no strict 'refs';

    for my $item ( qw/config scratch_path msearch_config/ ) {
      *$item = sub {
        shift->{$item};
      };
    }

    for my $config_accessor ( qw/command options/ ) {
      *$config_accessor = sub {
        shift->config->{$config_accessor};
      };
    }

  }
}

sub database {
  shift->msearch_config->{database};
}


sub ofile {
  my ( $self, $extension, $suffix ) = @_;

  $suffix ||= '';

  catfile( 
    $self->my_scratch_path, 
    file_without_extension( $self->ifile ) . $suffix . $extension 
  );
}


sub filtered_ofile {
  shift->ofile( '-filtered.csv' );
}


sub _postprocess {
  my $self = shift;
  sub {
    run_command sprintf( "awk -f %s %s > %s", $self->awk_script, $self->ofile( '.csv' ), $self->ofile( '.csv', '-mayu' ) );
  };
}

sub name {
  shift->config->{name};
}

sub awk_script {
  catfile( directory( abs_path( $0 ) ), 'scripts', lc( shift->config->{class} ) . '.awk' );
}


sub _mayu { 
  my $self = shift;
  sub {
    run_command sprintf( "perl -I%s %s -B %s -C %s -E 'rev_'", $self->_mayu_lib, $self->_mayu_script, $self->ofile( '.csv', '-mayu' ), $self->decoy_database );
  };
}

sub _mayu_script {
  catfile( shift->_mayu_path, 'Mayu.pl' );
}

sub _mayu_lib {
  catfile( shift->_mayu_path, 'lib' );
}

sub _mayu_path {
  catfile( directory( abs_path( $0 ) ), '..', 'vendor', 'Mayu' );
}

sub ifile {
  shift->{ifile};
}


sub config {
  shift->{config};
}

sub should_prepare_decoy {
  shift->msearch_config->{decoy}{prepare};
}

sub is_decoy_concatenated {
  my $self = shift;
  $self->should_prepare_decoy and $self->msearch_config->{decoy}{concat};
}


sub use_fdr_score {
  shift->msearch_config->{use_fdr_score};
}

sub get_fdr_class {
  ( shift->use_fdr_score ) ? 'PGTools::Util::FDRScore' : 'PGTools::Util::FDR';
}


sub prepare_database {

  my $self = shift;
  my $sconfig = $self->msearch_config;
  my @commands = ( );

  my $r = ( lc( $self->msearch_config->{decoy}{type} ) eq 'random' ) 
    ? '-r' 
    : '';


  # Looks like we need to prepare the decoy
  if( $self->should_prepare_decoy ) { 

    # If its concatenated decoy 
    # then it probably makes sense to move
    # the database file somewhere into scratch
    # then create a decoy

    if( $self->is_decoy_concatenated ) { 
      my $old = $self->database;
      my $new = $self->decoy_database; 

      die "

        Can not find the database file: $old
        Please check the path and alter the configuration

      " unless -e $old;

      unless( -e $new ) {

        cp $old, $new unless -e $new; 

        # Run Decoy
        print "Running concat decoy ... \n";
        push @commands, sub {
          run_pgtool_command sprintf( "decoy $r -a %s", $self->decoy_database );
        };

      }

    }

    else { 
      print "Running non-concating decoy ... \n";

      unless( -e $self->decoy_database ) {
        push @commands, sub {
          run_pgtool_command sprintf( "decoy $r %s %s", $self->database, $self->decoy_database );
        };
      }

    }

  }

  run_parallel @commands;

}


sub decoy_database {
  my $self = shift;
  $self->database_path_for( file( $self->database ) );
}


sub path_for {
  my ( $self, $file ) = @_;

  catfile( 
    $self->my_scratch_path, 
    $file
  );
}


sub database_path_for {
  my ( $self, $file ) = @_;

  my $parent;

  if( $ENV{ PGTOOLS_USE_SYMLINKED_DATABASE } && $ENV{ PGTOOLS_CURRENT_RUN_DIRECTORY  } && -d $ENV{ PGTOOLS_CURRENT_RUN_DIRECTORY } ) {
    $parent = $ENV{ PGTOOLS_CURRENT_RUN_DIRECTORY };
  }

  else {
    $parent = $self->scratch_database_path;
  }

  catfile( 
    $parent, 
    $file 
  );

}



1;
__END__

