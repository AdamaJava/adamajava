package PGTools::Command::Collate;

use strict;
use PGTools::Util::Fasta;
use PGTools::Util;
use PGTools::Util::Path;
use PGTools::Util::Collate;
use File::Spec::Functions;
use IO::File;

use parent 'PGTools::Command';


=head1 NAME

PGTools::Command::Collate

=head1 SYNOPSIS

  ./pgtools collate input.mgf output_file

=head1 DESCRIPTION

Combines several pepmerge files into one single file. 
Within proteome_run workflow, collate gets activated only if multiple 
input files are submitted by the user. 

=cut



sub run {

  my $class   = shift; 

  my $input_file  = shift @ARGV; 
  my $output_file = shift @ARGV;

  die "Invalid input: $input_file" unless -d $input_file;
  must_be_defined "Output File", $output_file;

  my @files = <$input_file/*.mgf>;

  my @merge_files = map {
    catfile( scratch_directory_path, file_without_extension( $_ ), 'pepmerge.csv' );
  } grep { $_ } @files;

  print "Collating ... \n";

  my $full_run = PGTools::Util::Collate->new( [ @merge_files ], $output_file );
  $full_run->run;

  print "Done \n";

  exit 0;
}



1;
__END__

