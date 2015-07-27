package PGTools::Command::FDR;

use strict;
use PGTools::Util;
use parent qw/
  PGTools::SearchBase
  PGTools::Command
/;

use Data::Dumper;

=head1 NAME

PGTools::FDR

=head1 SYNOPSIS

  ./pgtools fdr <input_file> 

  [OPTIONS]
    -v    or    --verbose

    Run verbosely, output as much information as possible


Its is *required* to run msearch before running FDR, fdr uses same commandline api as msearch,
although, strictly speaking there's not need for specifying input file, and the command actually
doesn't do anything with the input file, it is still required to so PGTools can figure out where
msearch data has been placed.


FDR command actually uses PGTools::Util::FDR to compute FDR, which in turn depends on PGTools::FDR::*
to provide and parse details from each search output.

FDR can behaviour can be tweaked with following configuration directives in src/config.json

    "cutoff":10, 

Sets the FDR to 10%.

    "use_fdr_score": 0

Lets you choose between standard FDR and FDRScore. FDR Score however is implemented only partially, best to leave this out at 0

   "decoy": {
      "concat": true 
    }

Choose concatenated decoy instead of separate target and decoy searches, separate search is default.

=cut

sub run {
  my $class = shift;

  my $options = $class->get_options( [
    'verbose|v', 'add|a=s@', 'remove|r=s@'
  ]);

  my $config  = $class->config; 
  my $ifile   = $class->setup;

  # Dont' cleanup
  $options->{dont_cleanup} = 1;

  my @to_run  = $class->get_runnables_with_prefix( 
    'PGTools::FDR', 
    $ifile, 
    $options  
  );

  run_parallel map { $_->get_runnable } @to_run;

}


1;
__END__

