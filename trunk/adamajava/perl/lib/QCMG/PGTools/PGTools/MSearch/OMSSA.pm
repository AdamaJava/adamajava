package PGTools::MSearch::OMSSA;

use strict;
use parent 'PGTools::MSearch::Base';
use Data::Dumper;
use FindBin;

use PGTools::Util qw/
  extension
  file
  file_without_extension
  run_command
  run_pgtool_command
/;

use PGTools::Util::Path qw/
  create_path_within_scratch
  path_within_scratch
/;

use File::Basename qw/
  dirname
/;



use File::Spec::Functions;


sub get_runnable {

  my $self      = shift;

  my $ofile      = $self->ofile( '.csv' ); 

  my $extension = extension $self->ifile;

  my @runs = ( );

  my $ifile = $self->ifile;

  # Process
  unless( $self->is_decoy_concatenated ) {
    push @runs, 
      $self->_format_db( $self->database ),
      $self->_omssa( $self->database, $self->ofile( '.csv' ) );
  } 

  push @runs, 
    $self->_format_db( $self->decoy_database ),
    $self->_omssa( $self->decoy_database, $self->ofile( '-d.csv' ) );


  # Go Fetch
  sub {

    print "Processing OMSSA ...\n";

    $_->() for @runs;

    print "Done processing: OMSSA \n";
  };

}



sub _format_db {
  my ( $self, $database ) = @_;

  my $command = $self->config->{formatdb}; 

  die "
    $command: 

    formatdb CANNOT be found, can not proceed with running OMSSA,
    exiting!

  " unless -e $command;



  sub {
    print "Formatting DB... \n";
    
    my $cmd = sprintf( "%s -i %s -p T -o F",
      $command, 
      $database
    );

    run_command $cmd;
 };
}


sub _omssa {
  my ( $self, $database, $ofile ) = @_;
  sub {
      run_command sprintf( '%s -d %s -fm %s %s -oc %s',
        $self->command,
        $database,
        $self->ifile,
        $self->options,
        $ofile
      ), 'omssa';
  };
}



1;
__END__
