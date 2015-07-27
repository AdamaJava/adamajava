package PGTools::Util::GTF;

use strict;
use warnings;
use Text::CSV;
use IO::File;
use Mojo::Base -base;
use Data::Dumper;

has 'file';
has 'rows';

sub new {

  my ( $class, $file ) = @_;

  die "File: $file does not exist" 
    unless -e $file;

  warn "File does not have an extension .gff or .gtf"
    unless $file =~ /\.g(t|f)f$/;


  my $self = bless { 
    file  => $file,
    rows  => [ ]
  }, $class;

  $self->_read_up;

  $self;
}

sub columns {
  ( qw/
    seqname
    source
    feature
    start
    end
    score
    strand
    frame
    attribute
  /);
}

sub length { 
  scalar( @{ shift->rows } );
}

sub first { 
  my $self = shift;
  my $count = shift;

  die "Invalid count, requires an integer" 
    unless $count =~ /^[0-9]+$/;

  die "Only @{[ $self->length ]} available, invalid input"
    if $count > $self->length;


  @{ $self->rows }[ 0 .. ( $count - 1 ) ];

}

sub grep { 
  my $self = shift;
  my $code = shift;

  die "Each requires a subroutine reference"
    unless ref( $code ) eq 'CODE';

  my @rows = ();

  $self->each( sub {
    my $entry = shift;
    push @rows, $entry if $code->( $entry )
  } );

  @rows;

}

sub each {

  my $self = shift;
  my $code = shift;

  die "Each requires a subroutine reference"
    unless ref( $code ) eq 'CODE';

  for ( @{ $self->rows } ) {
    $code->( $_ );
  }

}


sub _parse_attributes {
  my $self    = shift;
  my $string  = shift;  
  my %attributes = ();


  my $trim = sub {
    my $value = shift;

    return '' unless $value;

    $value =~ s/^\s*|\s*$//g;

    $value;
  };

  my $remove_quotes = sub {
    my $value = shift;

    $value =~ s/["']//g;

    $value;
  };


  map {
    my $att = $trim->( $_ );

    $att =~ m/
      \s*
      (?'key' \S+ )
      \s*
      =?
      \s*
      (?'val' .+ )
    /x;

    $attributes{ $trim->( $+{ key } ) } = $remove_quotes->( 
      $trim->( $+{ val } )
    );

  } split /;/, $string;


  \%attributes;

}


sub _add {
  my $self = shift;
  my @fields = @_;
  my @columns = $self->columns;

  my %row = (
    map {
      $columns[ $_ ] => $fields[ $_ ]
    } ( 0 .. ( @columns - 1 ) )
  );

  $row{ attribute } = $self->_parse_attributes( $row{ attribute } );

  push @{ $self->rows }, \%row;

}


sub _read_up {

  my $self  = shift;
  my $file  = $self->file;
  my $fh    = IO::File->new( $file, 'r' );

  my $csv   = Text::CSV->new( {
    binary    => 1,
    sep_char  => "\t"
  } );


  while( my $row = <$fh> ) {

    # Ignore comments
    next if $row =~ /^\s*\#\#/;

    chomp $row;

    my @fields = split /\t/, $row, 9; 

    $self->_add( @fields );

  }

}




1;
__END__
