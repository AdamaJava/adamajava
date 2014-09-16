package PGTools::Command::Help;

use strict;
use parent 'PGTools::Command';
use PGTools;
use FindBin;
use Pod::Text;

=head1 NAME

PGTools::Help

=head1 SYNOPSIS

  ./pgtools
  ./pgtools help
  ./pgtools help <command>
  ./pgtools help help


=head1 DESCRIPTION

Uses Pod::Text to extract pods from modules and produce help text

=head2 METHODS

=over 12 

=item C<run>

Class method:

  PGTools::Help->run

Assumes @ARGV contains options and other data unchanged, 
after the first item is consumed by PGTools itself, tries
to use PGTools->module and FindBin to locate the file
and produce help text on STDOUT using Pod::Text

=back

=cut

sub run {
  my $class   = shift; 
  my $command = shift( @ARGV );
  my $pod     = Pod::Text->new( sentence => 0, width => 78 );

  $pod->output_fh( *STDOUT );

  unless( $command ) {
    # No command given, help file for PGTools
    $pod->parse_from_file( 
      $class->find_file( 'PGTools.pm' )
    );
  }

  else {
    $pod->parse_from_file( 
      $class->find_file( ( PGTools->module( $command ) )[1] )
    );
  }

}

sub find_file {
  my ( $class, $file ) = @_;

  for my $prefix ( @INC ) {
    return "$prefix/$file" if -e "$prefix/$file";
  }

}


1;
__END__
