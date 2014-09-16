package PGTools::Command::PepMerge;

use strict;
use Getopt::Long;
use Data::Dumper;
use Text::CSV;
use File::Spec::Functions;
use IO::File;
use List::Util qw/sum/;

use parent qw/
  PGTools::SearchBase
  PGTools::Command
/;

use PGTools::Util;
use PGTools::Util::Path;


=head1 NAME

PGTools::Merge

=head1 SYNOPSIS

  ./pgtools merge <input_file> 

  [OPTIONS]
    -u    or    --union
    Do a set union of all search outputs

    -i    or    --intersection
    Accepts a number as input, allows you to say that given hit needs to intersection of atleast
    'x' number of search outputs. For example saying -i 2 would mean, given peptide must be part 
    ( OMSSA, XTandtem ) or ( XTandem, MSGF ) or ( MSGF, OMSSA )

Merges output of all search algorithms into a single file

=cut

sub run {
  my $class = shift;

  my $options = $class->get_options( [
     'union|u', 'intersection|i=i'
   ]);

  my $config  = $class->config; 
  my $ifile   = $class->setup;

  # Dont' cleanup
  $options->{dont_cleanup} = 1;

  debug "About to merge ...";

  # If nothing is set, set union
  if( ! $options->{union} && ! $options->{intersection} ) {
    $options->{union} = 1;
  }

  # Absolutely any runnable will do
  my @to_run  = $class->get_runnables_with_prefix( 
    'PGTools::FDR', 
    $ifile, 
    $options  
  );

  my $fh = IO::File->new( catfile( $to_run[ 0 ]->{scratch_path}, 'pepmerge.csv' ), 'w' );
  my $csv = Text::CSV->new;

  my %peptide_data = ( );
  for my $runnable ( @to_run ) {
    $peptide_data{ $runnable->name } = $class->extract_peptides( $runnable );
  }

  print Dumper \%peptide_data;

  my @all_runnables = keys %peptide_data;
  my @items = ();

  $csv->print( $fh, [ "Peptide", "Protein", @all_runnables ] );
  $fh->write( "\n" );

  for( my $i=0; $i < @all_runnables; $i++ ) {

    my $name = $all_runnables[ $i ];

    print "NAME: $name \n";

    my $peptides = $peptide_data{ $name };

    print "COUNT: " . scalar( keys %$peptides ) . " \n";


    for my $peptide ( keys %$peptides ) {

      my @data = ( $peptide, $peptides->{ $peptide }[ 0 ], ( map { 0 } @all_runnables ) ); 

      # current search engine
      $data[ 2 + $i ] = 1;

      for( my $j=$i+1; $j<@all_runnables; $j++ ) {
        my $other_name = $all_runnables[ $j ];

        if( $peptide_data{ $other_name }{ $peptide } ) {
          $data[ 2 + $j ] = 1;
          delete $peptide_data{ $other_name }{ $peptide };
        }

        else {
          $data[ $j + 2 ] = 0;
        }
      }

      push @items, [ @data ];

    }

  }

  # print Dumper( @items );

  my $sum = $options->{union} ? 1 : $options->{intersection};
  
  for ( 
    grep {
      sum( @{ $_ }[ 1 .. scalar( @$_ ) - 1 ] ) >= $sum;
    } @items
  ) {
    $csv->print( $fh, $_ );
    $fh->write( "\n" );
  }
  
  debug "OK ";
}

sub extract_peptides {
  my $self = shift;
  my $runnable = shift;

  # assume an empty file, if the file doesn't exist
  return { } unless -e $runnable->filtered_ofile; 

  my $csv   = Text::CSV->new( {
    sep_char => ( ( $runnable->name eq 'msgfdb' ) ? "\t" : "," )
  } );

  my $fh    = IO::File->new( $runnable->filtered_ofile, 'r' ) || die( "Can not file the filterd output file" );

  my $columns = $csv->getline( $fh );

  return { } unless $columns;

  $csv->column_names(  
    [ map { trim $_ } @$columns ]
  );

  my %peptides = ();

  while( ( my $row = $csv->getline_hr( $fh ) ) || ( print( $csv->error_diag ) && 0 ) ) {
    $peptides{ $runnable->peptide( $row ) } = [ $runnable->protein( $row ) ]; 
  }
  
  \%peptides;

}


1;
__END__
