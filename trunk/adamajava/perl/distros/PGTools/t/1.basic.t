use strict;
use Test::More; 

my @classes = qw/
  PGTools
  PGTools::Util
  PGTools::Convert
  PGTools::Help
  PGTools::Decoy
  PGTools::Translate
  PGTools::MSearch
  PGTools::Util::Fasta
  PGTools::Util::Translate
  PGTools::Configuration
/;

use_ok $_ for @classes;

done_testing;
