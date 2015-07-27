package PGTools::Command::Annotate;

use strict;
use Getopt::Long;
use PGTools::Util::Annotate;
use PGTools::Configuration;
use PGTools::Util;
use File::Basename qw/dirname/;
use File::Spec::Functions;
use IO::File;
use Data::Dumper;

use parent qw/
  PGTools::Command
/;

=head1 NAME

PGTools::Command::Annotate

=head1 SYNOPSIS

  ./pgtools annotate [OPTIONS] <input_csv_file> <output_csv_file>

  [OPTIONS]
    -p    or    --protein_id
    Column in which protein id resides


    --csv  
    Input file is comma separated values

    --tsv  
    Input file is tab separated values


=head1 DESCRIPTION
The goal of annotate module is to annotate as many proteins as possible from group output 
using two comprehensive data sources for human proteins. The first resource is HPRD (Human 
Protein Reference Database), a manually annotated protein database and the second, The Human 
Protein Atlas (HPA) for which immunohistochemistry images are available for protein entries.
 
A backend SQLite master database (annotate.sqlite) is constructed using HPRD data and placed 
in pgtools_scratch. Links to The Human Protein Atlas are provided which provides information 
on protein expression profiles based on immunohistochemistry, subcellular localization and 
transcript expression levels. 

It is not feasible to store high-resolution images, hence a link is provided for HPA entries. 
Annotates a given list of protein ids, for each protein in the input following fields are 
added describing the function of protein.

=over 20

=item seq_id

=item hprd_id

=item geneSymbol

=item nucleotide_accession

=item protein_accession

=item disease_name

=item expression_term

=item architecture_name

=item architecture_type

=item molecular_function_term

=item biological_process_term

=item cellular_component_term

=item geneSymbol_2

=item hprd_id_2

=item protein_interactor_name

=item site

=item residue

=item enzyme_name

=item enzyme_hprd_id

=item modification_type

=item HPRD link 

=item Protein Atlas Link 


=back

The this command accepts either a CSV / TSV file or plain text file containing protein ids. Annotate tries its best to figure out protein ids used within the input files. 

This is possible because of specially prepared database which merges 9606 database containing all protein data with ??? database that maps all possible protein ids.

This database is pretty large, so its not shipped with PGTools itself, the command tries to download this database from URL within src/config.json, config.json already contains the default URLs for the database

The following configuration parameters define the URLs for annotate command

    "url": "http://caffainerush.delta18.com"
    "database": "annotation.sqlite"

which contain url and the database name respectively

=cut



sub run {

  my $class   = shift; 

  my $options = $class->get_options( [
    'protein_id|p=s', 'csv', 'tsv', 'no_header'
  ]);

  my $ifile  = shift @ARGV;
  my $ofile =  shift( @ARGV );

  must_have "Input file", $ifile;
  must_be_defined "Output file", $ofile;

  my $annotate = PGTools::Util::Annotate->new(
    input     => $ifile,
    output    => $ofile,
    error_log => catfile( dirname( $ofile ), 'annotate.error.log' ),
    column    => $options->{protein_id} || 0,
    type      => $options->{tsv} ? 'tsv' : 'csv',
    no_header => $options->{no_header}
  );


  $annotate->run;

  exit 0;
}



1;
__END__

