package PGTools;

use strict;
use warnings;
use Data::Dumper;

use PGTools::Util::Path qw/
  create_scratch_directory 
/;

our $VERSION = '0.0.1';

# FIXME: 
# PGTools::Util::Path, PGTools::Util and File::Path are being used all over
# the place and seems to have overlapping functionality, It would make sense
# to move everything into a single class, sometime soon


=head1 NAME 
  PGTools

=head1 SYNOPSIS

  ./pgtools <command> <options>
  ./pgtools help <command>
  ./pgtools help

=head1 DESCRIPTION 
PGTools stands for proteogenomic tools. It’s a software suite which comprises of a set of utilities, 
libraries, helpers and customized databases for the analysis and visualization of proteogenomic data. 
PGTools is very flexible. The modules within PGTools can be run both independently or as a pipeline. 
The modules can be run in a command line or using a user friendly graphical user interface (GUI). 
PGTools also allows visualization of data wherever possible, including simple Venn diagrams to complex
tools Circos plots.


=head2 METHODS

=over 12

=item C<all_commands>

A Class method:

  PGTools->all_commands

Returns all the commands that pgtools can run as a hash, keys consist of commands
that can be called from the console, values consist of module names, assumed to be in 
namespace PGTools

=item C<module>

A Class method

  PGTools->module( $command )

Given a command, it returns the file path and fully qualified module name

=item C<run>

A Class method:

  PGToos->run;

The main method, does not return anything all it does is, detect what submodule is
responsible for the given console input and attempts to call run method on that module

=back

=cut

my %commands = (
  (
    map {
      $_ => ucfirst $_
    } qw/
      decoy 
      help 
      translate
      convert
      collate
      group
      annotate
      extract
      summary
      visualize
    /
  ),

  pepmerge           => 'PepMerge',

  proteome_run    => 'FullRun',

  # temporary     
  genome_run      => 'Phase2',

  # temporary / partial
  merge2bed       => 'MergeToBED',

  msearch         => 'MSearch',
  fdr             => 'FDR',
  tandem_process  => 'TandemProcess',

  # same command
  csv_to_html     => 'CSVToHTML',
  csv2html        => 'CSVToHTML'
);

sub all_commands {
  %commands;
}

sub module {
  my ( $class, $command ) = @_;

  ( 
    'PGTools::Command::' . $commands{ $command }, 
    'PGTools/Command/' . $commands{ $command } . '.pm' 
  ); 
}


sub run {

  my $class   = shift;
  
  # Should be run the first thing
  # We will always need it
  create_scratch_directory;


  my $command = lc shift( @ARGV ) || 'help';

  # Die
  if( ! $command || ! $commands{ $command } ) { 
    print "\n\nCommand does not exist: $command \n\n";
    $command = 'help';
  }

  my ( $module, $file ) = $class->module( $command );

  eval {
    require $file; 
  };

  die $@ if $@;

  $module->run;

}



1;
__END__
