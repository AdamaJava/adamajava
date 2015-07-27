package PGTools::Command::Group;

use strict;

use parent qw/
  PGTools::Command
  PGTools::SearchBase
/;

use PGTools::Util::ProteinAssembler;
use IO::File;
use File::Spec::Functions;
use File::Basename qw/dirname/;

use Data::Dumper;

=head1 NAME

PGTools::Group

=head1 SYNOPSIS

  ./pgtools group <input_file> 


Does protein-assembly of the merged output. Like several other commands, group depends on the 
fact that previous commands have successfully run and has placed output files in correct directories

Its expected that the commands are run in following exact order

  - msearch
  - fdr
  - merge
  - group

The actual protein assembly is implemented by PGTools::Util::ProteinAssembler. Post run, it places the output file
here

  <SCRATCH>/<INPUT_DIR>/group.csv

It also places the raw-matrix file here:

  <SCRATCH>/<INPUT_DIR>/matrix.csv

=cut

use PGTools::Util;
use PGTools::Util::Path;


sub run {
  my $class = shift;
  my $config  = $class->config; 
  my $ifile   = $class->setup;

  my ( $merged_file, $output_file, $matrix_file );

  if( $ifile =~ /\.mgf$/ ) {

    my $path = path_within_scratch( 
      file_without_extension( $ifile ) 
    );

    $merged_file = catfile( $path, 'pepmerge.csv' );
    $output_file = catfile( $path, 'group.csv' );
    $matrix_file = catfile( $path, 'matrix.csv' );

  }

  else {
    $merged_file = $ifile;
    $output_file = catfile( dirname( $ifile ), 'proteome_run.group.csv' ); 
    $matrix_file = catfile( dirname( $ifile ), 'proteome_run.matrix.csv' ); 
  }



  # FIXME:  Incorrect, databases should move out of
  #         algorithm specific into general
  my $database_path = $config->{database};


  my $assembler = PGTools::Util::ProteinAssembler->new(
    input           => $merged_file,
    database        => $database_path,
    peptide_column  => 'Peptide',
    output_file     => $output_file,
    save_matrix     => 1,
    matrix_file     => $matrix_file 
  );


  debug "About to group proteins ... ";

  $assembler
    ->pep2prot
    ->protein_list
    ->consensus;


  debug "Done grouping proteins";


}


1;
__END__
