#!/usr/bin/env perl

use strict;
use lib;
use FindBin;
use Getopt::Long;

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
use PGTools::Util::GTF;
use PGTools::Util::Translate;
use PGTools::Util qw/
  normalize
/;

use IO::File;


my $options = { };
GetOptions( 
  $options,
  'gtf=s', 'input=s', 'output=s', 'no-merge'
);

my ( $ifile, $ofile, $gtf_file, $no_merge ) = @{ $options }{ qw/input output gtf no-merge/ };

unless( $no_merge ) {

  die "No GTF file found"
    unless -e $gtf_file; 

  die "Not the right GTF file: $gtf_file "
    unless $gtf_file =~ /\.g(t|f)f$/;

}

die "No input file given"
  unless -e $ifile;

die "No output file given" 
  unless $ofile;


my $fasta     = PGTools::Util::Fasta->new_with_file( $ifile );
my $gtf       = ( $no_merge ) ? undef : PGTools::Util::GTF->new( $gtf_file );
my $translate = PGTools::Util::Translate->new;
my $ofh       = IO::File->new( $ofile, 'w' )          
  or die( "Unable to open file: $ofile for writing" );


sub to_attributes {
  my $item    = shift;
  my $string  = '';

  $string .= 'details:(' 
    . join( ';', 
        map   { $_ . '=' . $item->{$_} } 
        grep  { not m/attribute/ } 
        keys( %$item ) 
      ) 
  . ') ';

  $string .= 'attributes:(' 
    . join( ';', 
        map { $_ . '=' . $item->{attribute}{ $_ }; } 
        keys( %{ $item->{attribute} } ) 
      ) 
    . ')';

  $string;

}


$fasta->reset;

print "Starting ...\n";
while( $fasta->next ) {
  my $title     = $fasta->title;
  my ( $id )    = $title =~ /\b(PGOHUM\d+)\b/;
  my ( $entry, $attributes, @translates );

  next if $fasta->sequence =~ /unavailable/i;

  unless( $no_merge ) {
    ( $entry ) = $gtf->grep( sub { shift->{attribute}{gene_id} eq $id; } );

    if( $entry ) {
      $attributes = to_attributes( $entry ); 
      @translates = ( $entry->{strand} eq '-' ) ? ( -1, -2, -3 ) : ( 1, 2, 3 );
    }

    else {
      warn "No GTF Entry found for: $id, Skipping. \n";
      next;
    }

  }

  else {
    $attributes = '';
    @translates = ( $title !~ /\-/ ) ? ( 1, 2, 3 ) : ( -1, -2, -3 );
  }

  $translate->set_sequence( $fasta->sequence );
  for ( @translates ) {

    print $ofh '>'
      . $fasta->title 
      . " frame: $_ "
      . $attributes
      . $fasta->eol;

    print $ofh normalize( $translate->translate( frame => $_ ) ) . $fasta->eol;

  }
}

$ofh->close;

print "Done \n";





