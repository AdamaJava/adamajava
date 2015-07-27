package PGTools::MSearch::MSGFDB;

use strict;
use parent 'PGTools::MSearch::Base';
use Data::Dumper;

use PGTools::Util qw/
  run_command
  run_parallel
  file_without_extension
/;

use PGTools::Util::Path qw/
  create_path_within_scratch
  path_within_scratch
/;

use File::Basename qw/
  dirname
/;

use File::Spec::Functions;


sub generate_ofile {
  my ( $self, $suffix ) = @_;

  $suffix ||= ''; 

  catfile( 
    $self->my_scratch_path,
    file_without_extension( $self->ifile ) . $suffix . '.csv'
  );

}


sub get_runnable {
  my $self      = shift;

  my $ofile      = $self->generate_ofile;

  my @commands = (); 

  # Decoy has been prepared already
  # We must run two searches 
  # or if its concatanated database
  # a single search
  if( $self->should_prepare_decoy ) {

    if( $self->is_decoy_concatenated ) {
      push @commands, $self->make_command( $self->decoy_database, $self->generate_ofile( '-combined' ) ); 
    }

    else {
      push @commands, $self->make_command( $self->database, $ofile );
      push @commands, $self->make_command( $self->decoy_database, $self->generate_ofile( '-decoy' ) ); 
    }
  } 
  
  else {
    push @commands, $self->make_command( $self->database, $ofile ); 
  }

  sub {
    run_command $_ for @commands;
  };

}

sub make_command {
  my ( $self, $database, $ofile ) = @_;

  sprintf( 
    "java -Xmx2000M -jar %s %s -s %s -d %s -o %s",
    $self->command,
    $self->options,
    $self->ifile,
    $database,
    $ofile
  );

}





1;
__END__
