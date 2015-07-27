package PGTools::Util::ProteinAssembler;

use strict;
use PGTools::Util::Fasta;
use Text::CSV;
use IO::File;
use Data::Dumper;
use Storable;
use Text::CSV;

{
  package ProteinEntry;

  use strict;

  sub new {
    my ( $class, $protein, $accession ) = @_; 

    bless {
      protein     => $protein,
      accession   => $accession
    }, $class;
    
  }

  sub protein {
    shift->{protein};
  }

  sub accession {
    shift->{accession};
  }

  1;
}


sub new {
  my ( $class, %options ) = @_; 

  my $self = bless +{
    %options
  }, $class;

  $self->setup;

  $self;

}


sub setup {
  my $self = shift;

  $self->_db( 
    PGTools::Util::Fasta->new_with_file(
        $self->database
    )
  );
}




{

  no strict 'refs';

  my @methods = qw/
    input
    database
    _db
    peptide_column
    peptide_list
    snapshot
    output_file
    save_matrix
    matrix_file
  /;

  for my $method ( @methods ) {
    *$method = sub {
      my ( $self, $value ) = @_;

      if( $value ) {
        $self->{$method} = $value;
      }

      $self->{$method};

    };
  }

}

sub pep2prot {

  my $self = shift;

  # $self->peptide_list( retrieve 'peptide_list' );
  # return $self;


  my $fh   = IO::File->new( $self->input, 'r' );
  my $csv  = Text::CSV->new;

  SET_HEADERS: {
    $csv->column_names( 
      $csv->getline( $fh )
    );
  }

  my %peptide_list = ( );
  my $peptide_column = $self->peptide_column;
  while( my $row = $csv->getline_hr( $fh ) ) {
    my $peptide   = $row->{ $peptide_column } || $row->{ lc $peptide_column }; 

    warn "Peptide not found" unless $peptide;

    $peptide_list{ $peptide } = $self->get_proteins_for_peptide( $peptide );
  }

  $self->peptide_list( \%peptide_list );

  # store \%peptide_list, 'peptide_list';
  # exit;

  $self;

}


sub protein_list {
  my $self          = shift;
  my $peptide_list  = $self->peptide_list;

  my ( %proteins, %peptides, %protein_peptides, %shared_peptides, %proteins_description );


  while( my ( $peptide_key, $proteins_list ) = each( %$peptide_list ) ) {
    $peptides{ $peptide_key } = undef;

    for my $protein ( @$proteins_list ) {

      if( exists( $proteins{ $protein->protein } ) ) {
        $shared_peptides{ $protein->protein }++; 
      } else {
        $proteins_description{ $protein->protein } = $protein->accession;
      }

      # define
      $protein_peptides{ $protein->protein } ||= [ ];

      # keep track all all protein peptides
      push @{ $protein_peptides{ $protein->protein } }, $peptide_key;
      

      $proteins{ $protein->protein }++;

    }
  }


  # unset peptide list
  $self->peptide_list( 'NONE' );

  my %snapshot = ( 
    proteins             => \%proteins,
    peptides             => \%peptides,
    shared_peptides      => \%shared_peptides,
    proteins_description => \%proteins_description,
    protein_peptides     => \%protein_peptides
  );

  $self->snapshot( \%snapshot ); 

  $self;

}


# pending
sub consensus {
  my $self      = shift;
  my $snapshot  = $self->snapshot;
  my @matrix    = ( );

  my $fh = IO::File->new( $self->output_file, 'w' );
  my $csv = Text::CSV->new;

  $csv->print( $fh, [ qw/Protein Matches Group Representitive-Unassigned/ ] );
  $fh->write( "\n" );

  my @peptides = keys( %{ $snapshot->{peptides} } );
  my @proteins = keys( %{ $snapshot->{proteins} } );

  my $protein_count = @proteins;
  my $peptide_count = @peptides;

  # columns
  $matrix[ 0 ] = [ 'HEAD', @peptides, 'Peptide Count', 'Unique', 'Group' ];

  # rows
  for my $protein ( @proteins ) {
    push @matrix, [ $protein, ( undef ) x scalar( @peptides ), 0, 0, 0 ];
  }


  for( my $i=0; $i < scalar( @proteins ); $i++ ) {

    # protein
    my $protein = $proteins[ $i ];

    # unique
    my $unique = $snapshot->{proteins}{ $protein } - $snapshot->{shared_peptides}{ $protein };

    # set unique count in the matrix
    $matrix[ $i + 1 ][ $peptide_count + 2 ] = $unique;

    # get all the protein peptides 
    my %protein_peptides = ( );
    for my $peptide ( @{ $snapshot->{protein_peptides}{ $protein } } ) {
      $protein_peptides{ $peptide } = 1;
    }

    # set the total peptide count in the matrix too
    $matrix[ $i + 1 ][ $peptide_count + 1 ] = scalar( keys( %protein_peptides ) );

    # set the peptide matches
    for( my $j=0; $j < scalar( @peptides ); $j++ ) {
      my $peptide = $peptides[ $j ];

      if( exists( $protein_peptides{ $peptide } ) ) {
        $matrix[ $i + 1 ][ $j + 1 ] = $peptide;
      }
    }

  }


  # write the matrix file
  if( $self->save_matrix && $self->matrix_file ) {

    my $mfh = IO::File->new( $self->matrix_file, 'w' );
    
    for ( @matrix ) {
      $csv->print( $mfh, $_ );
      $mfh->write( "\n" );
    }

    $mfh->close;
  }



  my @unique = sort {
    $a->[ $peptide_count + 2 ] <=> $b->[ $peptide_count + 2 ]
  } grep {
    $_->[ $peptide_count + 2 ] > 0;
  } @matrix[ 1 .. $protein_count ];

  my @shared = sort {
    $a->[ $peptide_count + 1 ] <=> $b->[ $peptide_count + 1 ];
  } grep {
    $_->[ $peptide_count + 2 ] <= 0;
  } @matrix[ 1 .. $protein_count ];

  my @sorted = ( @unique, @shared );


  my $protein_count = 0;
  my $group_number  = 1;
  for( my $x=0; $x < scalar( @sorted ); $x++ ) {

    # already grouped
    next if $sorted[ $x ][ $peptide_count + 3 ] == 1;

    my %peptides_a = map {
      $_  => 1
    } grep {
      defined $_
    } @{ $sorted[ $x ] }[ 1 .. $peptide_count ];

    my $flag = 0;

    $protein_count++;

    for( my $y=$x+1; $y < scalar( @sorted ); $y++ ) { 

      # already grouped
      next if $sorted[ $y ][ $peptide_count + 3 ] == 1;

      my %peptides_b = map {
        $_ => 1
      } grep {
        defined $_
      } @{ $sorted[ $y ] }[ 1 .. $peptide_count ];


      my $count = 0;

      # count the number of matches
      map { $count++ if exists $peptides_a{ $_ } } keys %peptides_b;

      if( $count != 0 && $count == keys( %peptides_b ) ) {

        # mark
        for ( $x, $y ) {
          $sorted[ $_ ][ $peptide_count + 3 ] = 1; 
        }

        # flag
        $flag = 1;

        # print 
        $csv->print( $fh, [ $sorted[$y][0], scalar( keys( %peptides_b ) ), "Group $group_number", undef ] );
        $fh->write( "\n" );

      }

    }

    # group repr
    if( $flag == 1 && $sorted[ $x ][ $peptide_count + 2 ] > 0 ) {
      $csv->print( $fh, [ $sorted[$x][0], scalar( keys( %peptides_a ) ), "Group $group_number", "Group Representitive" ] );
      $group_number++;
      $fh->write( "\n" );
    }

    # unassigned: no unique peptides or hasn't been marked
    if( $flag == 0 && !$sorted[ $x ][ $peptide_count + 2 ] && !$sorted[ $x ][ $peptide_count + 3 ] ) {
      $csv->print( $fh, [ $sorted[$x][0], scalar( keys( %peptides_a ) ), "Unassigned", undef ] );
      $fh->write( "\n" );
    }

  }

  print "done\n";

}



sub get_proteins_for_peptide {

  my ( $self, $peptide ) = @_;

  my $fasta = $self->_db;
  my @proteins = ( );

  # reset the pointer in the fasta file
  $fasta->reset;

  while( $fasta->next ) { 
    if( $fasta->sequence =~ /$peptide/ ) {

      my $id = $fasta->accession;

      push @proteins, ProteinEntry->new( $fasta->title, $id );

    }
  }

  \@proteins;

}


1;
__END__
