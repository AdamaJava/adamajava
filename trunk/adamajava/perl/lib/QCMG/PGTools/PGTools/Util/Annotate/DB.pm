package PGTools::Util::Annotate::DB;

use strict;

use PGTools::Util::Path;
use PGTools::Util;

use File::Spec::Functions;
use PGTools::Configuration;
use LWP::UserAgent;
use IO::File;
use Data::Dumper;
use Text::CSV;
use File::Basename qw/basename/;
use DBI;


sub new { 

  my $config = PGTools::Configuration->new->config;


  my $self = bless {
    config    => $config, 
    database  => catfile( original_scratch_directory_path, $config->{annotate}{database} ) 
  }, __PACKAGE__;

  $self;

}

sub column_data {
  {

    'MASTER.csv' => [
      "seq_id", "hprd_id", "geneSymbol", "nucleotide_accession", "protein_accession", "disease_name", "expression_term",
      "architecture_name", "architecture_type", "molecular_function_term", "biological_process_term", 
      "cellular_component_term", "geneSymbol_2", "hprd_id_2", "protein_interactor_name", "site", 
      "residue", "enzyme_name", "enzyme_hprd_id", "modification_type"
    ],

    'ID_MAPPING.txt' => [
      'id_data', 'hprd_id'
    ]


  };
}

sub header_data {
  {

    'MASTER.csv' => [
      "PGTools_seq_id", "HPRD_ID", "GeneSymbol", "Nucleotide_Accession", "Protein_Accession", "Disease_Association", "Protein_Expression",
      "Protein_Architecture_Name", "Protein_Architecture_Type", "Molecular_Function_Term", "Biological_Process_Term", 
      "Cellular_Component_Term", "Protein_Protein_Interactions(Interactor_Name)", "PPI_HPRD_ID", "Non_Protein_Interactions", "Post_Translational_Modifications(Site)", 
      "PTM_Residue", "PTM_Enzyme_Name", "PTM_Enzyme_HPRD_ID", "Modification Type"
    ],

    'ID_MAPPING.txt' => [
      'id_data', 'hprd_id'
    ]


  }
}


sub is_database_setup { 
  my $self = shift;
  -e $self->{database} and ! -z $self->{database};
}


sub download_data { 

  my $self      = shift;
  my $url       = $self->{config}{annotate}{url};
  my $save_path = scratch_directory_path;
  my $file =  'annotation.sqlite';

  download "$url/$file" , catfile( $save_path, $file );

}


sub database {
  shift->{database};
}

sub dbh {
  my $self = shift;  
  $self->connect && $self->{dbh};
}

sub connect {
  my $self = shift;

  if( ! $self->{dbh} ) {
    $self->{dbh} = DBI->connect( 'dbi:SQLite:dbname=' . $self->database )
  }

  $self->{dbh};
}

sub get_data_for {
  my ( $self, $protein_id ) = @_;

  if( my ( $hprd_id, $ensg_id ) = $self->get_hprd_id_and_ensg_id( $protein_id ) ) {

    if( $hprd_id ) {

      my $st = $self->dbh->prepare( 'SELECT * FROM MASTER WHERE hprd_id=?' );

      $st->execute( $hprd_id );

      my $data = $st->fetchrow_hashref;

      $data->{ensg_id} = $ensg_id;

      $data;

    }
  }
}

sub get_hprd_id_from_protein {
  my ( $self, $protein_id ) = @_;

  my $row = $self->get_id_mapping_for_protein( $protein_id );

  if( $row ) {
    return $row->{hprd_id};
  }
}

sub get_hprd_id_and_ensg_id {
  my ( $self, $protein_id ) = @_;

  my $row = $self->get_id_mapping_for_protein( $protein_id );

  if( $row ) {
    my ( $ensg_id ) = $row->{id_data} =~ /\b(ENSG\d+)\b/;
    return ( $row->{hprd_id}, $ensg_id );
  }

}

sub get_id_mapping_for_protein {
  my ( $self, $protein_id ) = @_;

  my $st = $self->dbh->prepare( 'SELECT * FROM ID_MAPPING WHERE id_data LIKE ?' );

  $st->execute( '%' . $protein_id . '%' );

  my $row = $st->fetchrow_hashref;

  $row;

}


sub each_line {

  my ( $self, $file, $cols, $callback ) = @_;

  die( "Invalid callback" ) 
    unless ref( $callback ) eq 'CODE';

  die( "Cannot be zero columns" )
    if $cols == 0;

  if( $file =~ /\.csv$/ ) {
    my $csv = Text::CSV->new( { binary => 1 } );
    my $fh = IO::File->new( $file, 'r' );

    while( my $row = $csv->getline( $fh ) ) {

      my @data = ( @$row == $cols ) ? @$row : @{ $row }[ 0 .. ( $cols - 1 ) ];

      $callback->( @data );

    } 
  }
}


1;
__END__
