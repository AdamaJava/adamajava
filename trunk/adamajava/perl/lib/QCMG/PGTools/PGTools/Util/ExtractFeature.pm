package PGTools::Util::ExtractFeature;

use strict;
use IO::File;
use PGTools::Util::GTF;
use Data::Dumper;
use Mojo::Base '-base';
use IO::File;
use File::Spec::Functions;
use PGTools::Configuration;
use PGTools::Util;
use PGTools::Util::Path;
use File::Basename qw/dirname/;
use DBI;

no warnings 'redefine';

# CONS:
# - Does brute force
# - Reads up the entire thing

has 'bed_file';
has 'gtf';
has 'bed';

sub new {
  my ( $class, %options ) = @_;

  my $config = PGTools::Configuration
    ->new
    ->config;

  my $db_file = catfile( 
    original_scratch_directory_path, 
    'feature.sqlite' 
  );

  if( ! -e $db_file ) {
    my $url = join( '/', @{ $config->{feature_extract} }{ qw/ url database/ } );
    download $url, $db_file;
  }

  my $dbh = DBI->connect( 'dbi:SQLite:dbname=' . $db_file ); 

  bless { 
    %options,
    dbh => $dbh 
  }, $class;

}

sub dbh { shift->{dbh}; }

sub prepare {
  my $self = shift;

  $self->bed( 
    $self->_read_bed
  );

  $self;
}

sub _read_bed {
  read_bed_file( shift->bed_file );
}

sub bed_entries {
  my ( $self, $chromosome, $strand ) = @_;

  grep {
    $chromosome && $_->{chromosome} && $strand && $_->{strand} &&
    lc( $chromosome ) eq lc( $_->{chromosome} ) && $strand eq $_->{strand}
  } @{ $self->bed }

}

sub completely_contained {
  "
    strand=? AND
    chromosome=? AND
    ? >= start AND ? =< end
  ";
}

# absolutely overlaps
sub overlaps {
  "
  strand=? AND
  chromosome=? AND
  (
    ( ? < start AND ? >= end  )
    OR
    ( ? < start AND ? >= start )
    OR
    ( ? > start AND ? > end AND ? < end )
    OR
    ( ? = start AND  ? > end )
  )

  "
}

sub overlaps_params {
  my ( $self, $bed ) = @_;
  (
    map { lc }
    @{ $bed }{
      qw/
        strand chromosome
        start end
        start end 
        start end start
        start end
      /
    }
  )
}


# overlaps or is contained
sub boundry_condition {
  "
      strand=? AND
      chromosome=? AND
      ( 
        ( ? <= start AND ? >= end  )
        OR
        ( ? >= start AND ? <= end )
        OR
        (  start >= ? AND start <= ?  )
        OR 
        ( ? >= start AND ? <= end )
      )
  ";

}

sub boundry_params {
  my ( $self, $bed ) = @_;
  (
    $bed->{strand}, lc( $bed->{chromosome} ),
    $bed->{start}, $bed->{end},
    $bed->{start}, $bed->{end},
    $bed->{start}, $bed->{end},
    $bed->{start}, $bed->{start}
  );
}


sub write_unique {
  my ( $self, $file, $entries ) = @_;
  my %seen = ();
  my $fh = IO::File->new( $file, 'w' ) or die( "Can't open file for writing" );
  my @keys = qw/
    chromosome
    start
    end
    type
    score
    strand
  /;


  for my $entry ( @$entries ) {
    my $key = join '', %$entry;

    next if $seen{ $key };

    $fh->print( join( "\t", @{ $entry }{ @keys } ) . "\n" );

    $seen{ $key } = 1;

  }

  $fh->close;

}


sub write_file {
  my ( $self, $file, $entries ) = @_;

  my $filename = catfile( dirname( $self->bed_file ), file_without_extension( file( $self->bed_file ) ) . $file );

  print $filename, "\n";

  $self->write_unique( 
    $filename,
    $entries
  );
}

sub chromosomes {
  map { 'chr' . $_ } ( ( 1 .. 22 ), 'x', 'y' );
}

sub strands {
  ( '+', '-' );
}


sub filter {
  my ( $self, $coderef ) = @_;
 
  my @chromosomes = $self->chromosomes;
  my @strands = $self->strands; 

  my @entries;
 
  for my $chromosome ( @chromosomes ) {
    for my $strand ( @strands ) {
      print "For Chromosome: $chromosome, Strand: $strand \n";

      my @bed_entries = $self->bed_entries( $chromosome, $strand );

      for my $bed ( @bed_entries ) {
        if( $coderef->( $bed ) ) {
          push @entries, $bed;
        }
      }

    }

  }
 
  \@entries;
}


sub extract_novel_gene {

  my $self = shift;

  my $clause = "
    FROM grch37 
    WHERE 
      feature='gene' AND
      @{[ $self->boundry_condition ]}
  ";

  my $query = "
    SELECT COUNT(*) cnt 
    $clause
  ";
  
  my $debug = "
    SELECT * 
    $clause
  ";

  my $st = $self->dbh->prepare( $query );
  my $db = $self->dbh->prepare( $debug );

  my $entries = $self->filter( sub {
    my $bed = shift;
    my @bind_params = $self->boundry_params( $bed );

    $self->debug( "RECEIVED BED" );
    $self->debug( Dumper( $bed ) );

    $st->execute( @bind_params );

    if( $self->is_debug ) {
      $db->execute( @bind_params );
      $self->debug( Dumper( $db->fetchall_hashref( 'id' ) ) );
    }

    $st->fetchrow_hashref->{cnt} == 0;

  });

  $self->write_file( '.novel.genes.BED', $entries );

}

sub extract_gene_overlap {
  my $self = shift;

  my $clause = "
    FROM grch37 
      WHERE feature='gene' AND 
      @{[ $self->overlaps ]}
  ";

  my $query = "
    SELECT COUNT(*) cnt
    $clause
  ";

  my $debug = "
    SELECT * 
    $clause
  ";

  my $st = $self->dbh->prepare( $query );
  my $db = $self->dbh->prepare( $debug );

  my $entries = $self->filter( sub {
    my $bed = shift;

    $self->debug( "BED ENTRY" );
    $self->debug( Dumper( $bed ) );

    my @bind_params = $self->overlaps_params( $bed );

    $st->execute( @bind_params );
    $db->execute( @bind_params );

    $self->debug( "OVERLAPPING GENES" );
    $self->debug( Dumper( $db->fetchall_hashref( 'id' ) ) );

    my $result = $st->fetchrow_hashref;

    $result->{cnt} > 0;

  } );

  $self->write_file( '.overlapping.genes.BED', $entries );
}


sub extract_novel_exon {
  my $self = shift;

  my $entries_within_gene = $self->dbh->prepare( "
    SELECT * FROM grch37 
    WHERE feature='gene' AND 
    @{[ $self->boundry_condition ]}
    
  " );


  my $exons_within_gene = $self->dbh->prepare( "
    SELECT * FROM grch37 
    WHERE feature='exon' AND 
    @{ [ $self->boundry_condition ]}
    ORDER BY start
  " );


  # get all entries within a gene
  my $entries = $self->filter( sub {
    my ( $bed ) = @_; 

    $self->debug( "RECIEVED BED FILE " );
    $self->debug( Dumper( $bed ) );

    $entries_within_gene->execute( 
      $self->boundry_params( $bed )
    );


    my $entries = $entries_within_gene->fetchall_hashref( 'id' );

    $self->debug( "GENE ENTRIES" );
    $self->debug( Dumper( $entries ) );

    # gene
    my ( $gene ) = values %{ $entries }; 

    $self->debug( "NO GENE FOUND, EXITING " ) unless $gene;

    # no gene, no point looking for an exon
    return 0 unless $gene;

    $exons_within_gene->execute(
      $self->boundry_params( $bed )
    );

    my $exon_entries = $exons_within_gene->fetchall_hashref( 'id' );

    $self->debug( "EXON ENTRIES FOUND" );
    $self->debug( Dumper( $exon_entries ) );

    # all exon rows
    my @rows = values( %{ $exon_entries } ); 

    return 0 if @rows > 0;

    return 1;

  });

  $self->write_file( '.novel.exons.BED', $entries );

}

sub extract_outframe {
  my $self = shift;

  my $query = "
    SELECT COUNT(*) cnt  FROM grch37
    WHERE
      feature='CDS' AND strand=? AND chromosome=? AND
      ? >= start and ? < end AND
      ( ( ( ? - CAST( start AS int ) ) % 3 ) - CAST( frame AS int ) ) != 0 
  ";

  # execute
  my $st = $self->dbh->prepare( $query );

  # extract stuff
  my $entries = $self->filter( sub {
      my ( $bed ) = @_;

      $st->execute( 
        $bed->{strand},
        $bed->{chromosome},
        $bed->{start},
        $bed->{start},
        $bed->{start}, 
      ) or die( $self->dbh->error );

      $st->fetchrow_hashref->{cnt} > 0;

  });

  $self->write_file( '.outframe.BED', $entries );

}

sub extract_exon_overlap {

  my $self = shift;

  my $filter_sql = "
    FROM grch37 
    WHERE  
      feature='exon' AND 
      @{[ $self->overlaps ]}
  ";

  my $query = "
    SELECT COUNT(*) cnt
    $filter_sql
  ";

  my $debug = "
    SELECT * 
    $filter_sql
  ";

  my $st = $self->dbh->prepare( $query );
  my $db = $self->dbh->prepare( $debug );

  my $entries = $self->filter( sub {
    my ( $bed ) = @_;

    $self->debug( "RECIEVED BED ENTRY:");
    $self->debug( Dumper( $bed ) );

    my @bind_params = $self->overlaps_params( $bed );

    $st->execute( @bind_params );

    if( $self->is_debug ) {
      $db->execute( @bind_params );
      $self->debug( "OVERLAPPING EXONS: ");
      $self->debug( Dumper( $db->fetchall_hashref( 'id' ) ) );
    }

    $st->fetchrow_hashref->{cnt} > 0;

  });

  $self->write_file( '.overlapping.exons.BED', $entries );

}

sub is_debug {
  $ENV{PGTOOLS_DEBUG};
}

# make sure everything you pass is a string
# write to STDERR
sub debug {
  my $self = shift;
  if( $self->is_debug ) {
    print STDERR "$_ \n" for @_;
  }
}


1;
__END__
