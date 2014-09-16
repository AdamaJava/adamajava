package PGTools::Util::Collate;


use strict;
use Text::CSV;
use IO::File;
use File::Basename qw/dirname/;
use File::Spec::Functions;

sub new {
  my ( $class, $merge_input_files, $output_file ) = @_;


  my @actual_input_files;
  for ( @$merge_input_files ) {

    unless( -e $_ ) {

      my $merged_file = catfile( dirname( $_ ), 'merged.csv' );

      if( -e $merged_file ) {
        push @actual_input_files, $merged_file;
      } else {
        warn "Unable to find file: $_";
      }

      next;
    }

    push @actual_input_files, $_;

  }

  my $self = bless {
    input_files => \@actual_input_files,
    output_file => $output_file
  }, __PACKAGE__;


  $self;

}

sub input_files { shift->{input_files}; }


sub run {
  my $self    = shift;
  my %output  = ();
  my $csv     = Text::CSV->new;
  my @cols    = ();

  print "Merging the all files\n";

  for my $file ( @{ $self->input_files } ) {

    print "Working on: $file \n";

    my $fh = IO::File->new( $file, 'r' );

    # set column names
    $csv->column_names( @cols = map { lc } @{ $csv->getline( $fh ) } );

    while( my $row = $csv->getline_hr( $fh ) ) {
      unless( exists( $output{ $row->{peptide} } ) ) {
        $output{ $row->{peptide} } = $row;
      }
    }

    $fh->close;

  }

  print "Writing output file \n";
  my $ofh = IO::File->new( $self->{output_file}, 'w' ) 
    or die( "Unable to open: @{[ $self->{output_file} ]} for writing \n" );

  $csv->print( $ofh, [ @cols ] );
  $ofh->print( "\n" );

  while( my ( $peptide, $data ) = each( %output ) ) {
    $csv->print( $ofh, [ 
      map {
        $data->{ $_ }
      } @cols
    ] );

    $ofh->print( "\n" );

  }

  $ofh->close;

  print "Done \n";


}




1;
__END__
