package PGTools::MSearch::MSGF;

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
  my ( $self, $suffix, $extension ) = @_;

  $suffix ||= ''; 

  catfile( 
    $self->my_scratch_path,
    file_without_extension( $self->ifile ) . $suffix . ( $extension || '.csv' )
  );

}


sub get_runnable {
  my $self      = shift;

  my @commands = (); 
  my @runs = ();

  unless( $self->is_decoy_concatenated ) {
    push @commands, $self->make_command( 
      $self->database, 
      ''
    ); 

    push @commands, $self->make_convert_command( '' );


  } 

  push @commands, $self->make_command( 
    $self->decoy_database, 
    '-d'
  ); 

  push @commands, $self->make_convert_command( '-d' );

  sub {
    print "About to process MSGF ... \n";
    run_command $_ for @commands;
    $_->() for @runs;
    print "Done processing: MSGF \n";
  };

}

sub make_command {
  my ( $self, $database, $suffix ) = @_;

  my $ofile = $self->generate_ofile( $suffix, '.mzid' );

  sprintf( 
    "java -Xmx2000M -jar %s %s -s %s -d %s -o %s",
    $self->command,
    $self->options,
    $self->ifile,
    $database,
    $ofile
  );

}


sub make_convert_command {
  my ( $self, $suffix ) = @_;

  my @cmd = ( 
    sprintf( 
      "java -Xmx2000M -cp %s edu.ucsd.msjava.ui.MzIDToTsv -i %s -o %s", 
      $self->command,
      $self->generate_ofile( $suffix, '.mzid' ),
      $self->generate_ofile( $suffix, '.tsv' )
    ),
    sprintf( 
      "cat %s | sed 's/\"//g' > %s",
      $self->generate_ofile( $suffix, '.tsv' ),
      $self->generate_ofile( $suffix, '.csv' )
    )
  );

  @cmd;

}



1;
__END__
