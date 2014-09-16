package PGTools::SearchBase;

use strict;
use PGTools::Configuration;

use List::Util qw/first/;

use PGTools::Util;
use PGTools::Util::Path;
use Data::Dumper;

use PGTools::MSearch::OMSSA;
use PGTools::MSearch::XTandem;
use PGTools::MSearch::MSGF;

use PGTools::FDR::OMSSA;
use PGTools::FDR::XTandem;
use PGTools::FDR::MSGF;

=head1 NAME

PGTools::SearchBase - A abstract class that both PGTools::MSearch and PGTools::FDR inherit from 

=cut


=head1 METHODS

=over 12

=item C<config>

Returns reference to msearch parts of the config defined in config/config.json

=cut

sub config {
  PGTools::Configuration->new->config->{msearch};
} 

=item C<setup>

Returns input file path, dies if there is no iput file given 
Creates path with scratch if required

=cut 

sub setup {

  my $class = shift;
  my $msearch = shift;

  # Input file
  my $ifile   = shift @ARGV;

  # Just die if the input file doesn't exist
  # Nobody loves you
  exit_with_message "File doesn't seem to exist: $ifile" 
    unless defined_and_exists $ifile;

  my $input_directory = catfile( 
    scratch_directory_path, 
    file_without_extension( $ifile ) 
  );

  if( -e $input_directory && -t STDIN && $msearch ) {

    print STDERR "
      $input_directory already exists, you probably ran proteome_run / genome_run / msearch already.
      All the output files will be overwritten. If you already know this, you're all fine.
      type 'yes' to continue or 'no' to stop
    " . "\n";

    OPTION: {

      print STDERR "yes/no> ";
      my $option = <STDIN>;

      if( $option =~ /no?/i ) {
        exit 1;
      }

      elsif( $option !~ /y(es)?/i ) {
        print STDERR "
          Invalid Option, Try Again. (yes/no)
        ". "\n";

        redo OPTION;
      }

      else {
        exit 0;
      }

    }

  }

  # We are ready to start
  # create scratch directories
  create_path_within_scratch file_without_extension( $ifile );


  $ifile;

}


=item C<algorithms_to_be_processed>
=cut
sub algorithms_to_be_processed {

  my ( $class, $options ) = @_;

  # Algorithms to be processed 
  my @to_be_processed = ( );
  my $config = $class->config;

  ALGORITHMS_TO_BE_PROCESSED: {

    # Get the defaults
    my @algorithms  = @{ $config->{defaults} };

    # Get all availabel algorithms
    my @all_algorithms  = $class->all_algorithms( $config );

    for my $algorithm ( @all_algorithms ) {

      my $name = $algorithm->{name};

      my $is_in_default_list  = ( first { $name eq $_ } @algorithms );
      my $is_in_removal_list  = ( first { $name eq $_ } @{ $options->{remove} } );
      my $is_in_add_list      = ( first { $name eq $_ } @{ $options->{add} } );

      if( ( $is_in_default_list and not $is_in_removal_list ) or $is_in_add_list ) { 
        push @to_be_processed, $algorithm; 
      }

    }
  }

  @to_be_processed;

}


=item C<get_runnables_with_prefix>
=cut
sub get_runnables_with_prefix {
  my ( $class, $prefix, $ifile, $options ) = @_;

  my $config = $class->config;

  map {

     my $c = ( $prefix . '::' . $_->{class} )
      ->new( 
        ifile           => $ifile,
        config          => $_,
        scratch_path    => path_within_scratch( file_without_extension( $ifile ) ),
        msearch_config  => $config
      );

      unless( $options->{dont_cleanup} ) {
        $c->cleanup_path
          ->setup_path;
      }

      $c;

  } $class->algorithms_to_be_processed( $options );


}


=item C<all_algorithms>
=cut
sub all_algorithms {
  my ( $class, $config ) = @_;

  @{ $config->{algorithms} };
}

=back
=cut

1;
__END__
