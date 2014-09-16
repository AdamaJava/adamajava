#!/usr/bin/env perl


use strict;
use warnings;

use ExtUtils::MakeMaker;

WriteMakefile(
  NAME         => 'PGTools',
  VERSION_FROM => 'lib/PGTools.pm',
  ABSTRACT     => 'analysis and visualization of proteogenomic data',
  AUTHOR       => 'Shiv Nagaraj',

  EXE_FILES    => [ 'src/pgtools' ],

  PREREQ_PM => {
    'Data::Dumper'      => 0,
    'Getopt::Long'      => 0,
    'IO::File'          => 0,
    'FindBin'           => 0,
    'Pod::Text'         => 0,
    'List::Util'        => 0,
    'Config::General'   => 0,
    'Font::TTF::Font'   => 0,
    'Math::Bezier'      => 0,
    'Math::Round'       => 0,
    'Regexp::Common'    => 0,
    'Readonly'          => 0,
    'Params::Validate'  => 0,
    'GD::Polyline'      => 0,
    'Clone'             => 0,
    'Set::IntSpan'      => 0,
    'List::MoreUtils'   => 0,
    'Text::Format'      => 0,
    'Math::VecStat'     => 0,
    'Exporter'          => 0,
    'File::Slurp'       => 0,
    'parent'            => 0,
    'Text::CSV'         => 0,
    'XML::Twig'         => 0,
    'DBI'               => 0,
    'DBD::SQLite'       => 0,
    'Venn::Chart'
  }
);
