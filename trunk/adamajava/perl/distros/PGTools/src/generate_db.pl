#!/usr/bin/env perl

use strict;
use DBI;
use Text::CSV;
use IO::File;
use File::Spec::Functions;
use Carp;

# use actual path
my $database    = "annotation.sqlite";
my $master_file = "master.csv";
my $id_mapping  = "id_mapping.txt";

# if the database exists
# remove it
if( -e $database ) {
  unlink( $database ) || die( "Error removing file: $database " . $! );
}

croak( "Unable to find master file" )       unless -e $master_file;
croak( "Unable to find id_mapping file" )   unless -e $id_mapping;

# database connection
my $dbh = DBI->connect( 'dbi:SQLite:dbname=' . $database );

my @master_columns = ( 
  "seq_id","hprd_id","geneSymbol","nucleotide_accession","protein_accession",
  "swissprot_id","entrezgene_id","disease_name","expression_term","architecture_name",
  "architecture_type","molecular_function_term","biological_process_term",
  "cellular_component_term","geneSymbol_2","hprd_id_2","protein_interactor_name",
  "site","residue","enzyme_name","enzyme_hprd_id","modification_type", "geneSymbol_x","nucleotide_accession_x",
  "protein_accession_x","swissprot_id_x","entrezgene_id_x","ensg_id"
);

my @id_mappings = ( 'id_data', 'hprd_id' );


create_table( 'master', @master_columns );
create_table( 'id_mapping', @id_mappings );

import_master();
import_id_mappings();

sub create_table {
  my $table_name = shift;
  my @cols  = @_;

  my $columns = join( ',', map {
    "\t $_ VARCHAR( 300 ) \n"
  } @cols );

  my $table = "
    CREATE TABLE $table_name (
      id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
      $columns
    );
  "; 

  $dbh->do( $table );
}

sub import_master {
  my $col_data = [ @master_columns ]; 
  my $table = 'master';

  print "Importing $table Data ...\n";

  my $statement = $dbh->prepare( sprintf( 
    'INSERT INTO %s ( %s ) VALUES ( %s )',
    $table,
    join( ',', @$col_data ),
    join( ',', map { '?' } @$col_data )
  ) );

  my $csv = Text::CSV->new( { binary => 1 } );
  my $fh = IO::File->new( $master_file, 'r' );
  my $cols = scalar( @$col_data );

  while( my $row = $csv->getline( $fh ) ) {
    my @data = ( @$row == $cols ) ? @$row : @{ $row }[ 0 .. ( $cols - 1 ) ];
    $statement->execute( @data );

  } 

  $fh->close;
}

sub import_id_mappings {
  print "Importing ID_MAPPING.txt Data ...\n";
  my $statement = $dbh->prepare( 'INSERT INTO ID_MAPPING ( id_data, hprd_id ) VALUES ( ?, ? )' );
  my $fh = IO::File->new( $id_mapping, 'r' ) or die( "File not found: $!" );

  while( my $line = <$fh> ) {
    chomp $line;

    next unless $line =~ /\b(ENSG[\S]+)\b/;

    my $hprd_id = get_hprd_id_for( $1 );

    next unless $hprd_id;

    $statement->execute( $line, get_hprd_id_for( $1 ) ) or die( "Error: $!" );
  }

  $fh->close;

  print "Done \n";

}

sub get_hprd_id_for {

  my $ensg_id = shift;
  my $where = "ensg_id LIKE '%$ensg_id%'";

  my $q = "SELECT * FROM master WHERE $where";

  print $q, "\n";

  my $st = $dbh->prepare( $q );


  $st->execute;

  my $row = $st->fetchrow_hashref;

  $row->{hprd_id} if $row;

}

sub update_hprd_ids {
  my $st    = $dbh->prepare( 'SELECT * FROM master' );
  my $id    = $dbh->prepare( 'SELECT * FROM id_mapping WHERE id_data LIKE ?' );
  my $up    = $dbh->prepare( 'UPDATE id_mapping SET hprd_id=? WHERE id=?' ); 

  $st->execute;

  my $count = 1;
  print "Updating HPRD IDs \n";
  while( my $row = $st->fetchrow_hashref ) { 

    $count++;

    print "$count Done \n" if $count % 100 == 0;

    $id->execute( '%' . $row->{protein_accession} . '%' );

    my $map = $id->fetchrow_hashref;

    $up->execute( $row->{hprd_id}, $map->{id} )
      if $map;

  }

  print "Done \n";

}
