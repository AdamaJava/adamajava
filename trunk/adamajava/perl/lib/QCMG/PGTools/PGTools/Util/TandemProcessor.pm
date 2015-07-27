package PGTools::Util::TandemProcessor;

use strict;
use XML::Twig;
use IO::File;
use Text::CSV;
use Data::Dumper;


sub new {
  my ( $class, %options ) = @_;

  bless {
    %options
  }, $class;
}

sub ofile { shift->{ofile} }
sub ifile { shift->{ifile} }


# Copied most of stuff from 
# PP1
sub process {
  my $self = shift;

  my $ofh = IO::File->new( $self->ofile, 'w' ) || die( "Couldn't open file: " . $self->ofile );
  my $csv = Text::CSV->new;

  $csv->print( $ofh, [ split( ',', "ScanID,MH+,Peptide,Score,Expect,Modification,Protein ID,Pep start,Pep End" ) ] );
  $ofh->write( "\n" );

  my $t = XML::Twig->new(
    twig_roots  => {
      protein => sub {
        my ( $twig, $element ) = @_;

        my $r = $element;

        # Values required for each line of CSV
        my ( $score, $mh, $missed_cleavage, @proteins, $protein_id );
        my ( $scan_id, $peptide, $expect, $pep_start, $pep_end);

        my $protein_flag = 0;
        my $peptide_flag = 0;
        my $scan_id_flag = 0;
        my $mod = '';

        my $protein;
        if( defined $r->id ) {
          while( $r = $r->next_elt ) {

            $scan_id = $element->{att}{uid};

            $protein = $r->text
              if $r->gi eq 'note' && $r->{att}{label} eq 'description';

            if( $r->gi eq 'domain' && $peptide_flag == 0 ) {
              $peptide = $r->{att}{seq};

              ( $mh, $pep_start, $pep_end, $score, $missed_cleavage, $expect ) = @{ $r->{att} }{ 
                qw/mh start end hyperscore missed_clevages expect/ 
              };

              $peptide_flag = 1;
            }


            $mod .= join( ':', @{ $r->{att} }{ qw/ type at modified / } ) . ';' 
              if $r->gi eq 'aa';

          }

          $element->purge;

          # Get the protein id
          $protein_id = $protein; 

          # Remove new lines from scan id
          $scan_id =~ s/\n//g;			

          # Remove leading / trailing space from scan_id
          $scan_id =~ s/^\s*|\s*$//g;

          # There you have it now
          $csv->print( $ofh, [
              ( $scan_id,$mh,$peptide,$score,$expect,$mod,$protein_id,$pep_start,$pep_end )
            ] );

          $ofh->write( "\n" );


        }

      }
    }
  );

  $t->parsefile( $self->ifile );

  $ofh->close;

  print "Done\n";

}


1;
__END__




