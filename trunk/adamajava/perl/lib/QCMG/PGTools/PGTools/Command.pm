package PGTools::Command;

use strict;
use Getopt::Long;

sub get_options {
  my ( $class, $accepted_options ) = @_;

  my $options = { };

  GetOptions( $options, @$accepted_options );

  $options;

}

1;
__END__
