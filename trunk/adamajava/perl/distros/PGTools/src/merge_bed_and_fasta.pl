#!/usr/bin/env perl

use strict;
use lib;
use FindBin;
use Getopt::Long;
use Data::Dumper;

BEGIN {

  my @paths = (
    "$FindBin::Bin/../lib",
    "/Perl/lib/pgtools"
  );
  # In the parent directory?
  for ( @paths ) {
    unshift @INC, $_ if -d $_; 
  }

}

use PGTools::Util::Fasta;

use PGTools::Util qw/
  normalize
/;

use PGTools::Translate;

use IO::File;

my $options = { };
GetOptions( 
  $options,
  'bed=s', 'input=s', 'output=s'
);

my ( $ifile, $ofile, $bed_file ) = @{ $options }{ qw/input output bed/ };

die "No BED file found"
  unless -e $bed_file; 

die "Not the right GTF file: $bed_file "
  unless $bed_file =~ /\.bed/i;

die "No input file given"
  unless -e $ifile;

die "No output file given" 
  unless $ofile;

sub read_bed {
  my $fh = IO::File->new( $bed_file, 'r' );
  my $rows = [ ];
  my @columns = qw/
    chrom
    chromStart
    chromEnd
    name
    score
    strand
    thickStart
    thickEnd
    itemRgb
    blockCount
    blockSizes
    blockStarts
  /;


  while( my $row = <$fh> ) {
    my @fields = split /\t/, $row, 12;
    push @$rows, {
      map {
        $columns[ $_ ] => $fields[ $_ ]
      } ( 0 .. $#columns ) 
    };
  }

  return $rows;

}

sub to_attributes {
  my $hash = shift;

  my $att = ' attributes:('
  . join( ';', 
      map {
        $_ . '=' . $hash->{$_}
      } keys( %$hash )
   )
  . ')';

  $att =~ s/\n//sg;

  $att;
}


my $fasta     = PGTools::Util::Fasta->new_with_file( $ifile );
my $ofh       = IO::File->new( $ofile, 'w' )      or die( "Cant open $ofile for writing" ); 
my $bed       = read_bed;
my $translate = PGTools::Util::Translate->new;

print "Starting ...\n";
while( $fasta->next ) {
  my $title     = $fasta->title;
  my ( $id )    = $title =~ /^(\w+)\b/;
  my ( $entry ) = grep {
    $_->{name} eq $id;
  } @$bed;


  unless( $entry ) {
    warn "Entry not found in the BED file: $id";
    next;
  }

  my @translates = ( $entry->{strand} =~ /\+/ ) ? ( 1, 2, 3 ) : ( -1, -2, -3 );
  my $attributes = to_attributes( $entry );

  $translate->set_sequence( $fasta->sequence );
  for ( @translates ) {

    print $ofh '>'
      . $fasta->title 
      . " frame: $_ "
      . $attributes
      . $fasta->eol;

    print $ofh normalize( $translate->translate( frame => $_ ) );

  }

}


$ofh->close;

