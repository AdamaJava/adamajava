package PGTools::Util::Fasta;

use strict;
use IO::File;
use Data::Dumper;
use PGTools::Util::AccessionHelper;

=head1 NAME

PGTools::Util::Fasta - A simple, quick but relatively inefficient ( For now ) way to deal with fasta files

=head1 SYNPOSIS

  $fasta = PGTools::Util::Fasta->new( 'fasta_file' )

  $fasta->title
  $fasta->accession
  $fasta->sequence
  $fasta->next

=head1 DESCRIPTION

PGTools::Util::Fasta is a simple class, that helps dealing with fasta files 

=head1 METHODS

=over 12

=item C<new_with_file>

  PGTools::Util::Fasta->new_with_file( $file_path );

Returns a PGTools::Util::Fasta object

=cut


# FIXME: Not very efficient at the moment
sub new_with_file {
  my ( $class, $file ) = @_;

  die "File not found: $file" 
    unless -e $file;

  my $self = bless {
    _file           => $file,
    _fh             => IO::File->new( $file, 'r' ),
    _current_entry  => undef
  }, $class;


  $self->eol_detect; 

  $self;

}

=item C<eol_detect>

Detects the eol character in the given fasta file

=cut


sub eol_detect { shift->eol; }

=item C<fh>

Returns the IO::File object of the current fasta file

=item C<file>

Returns the path of the current file

=item C<current_entry>

Returns the current entry that the cursor is set at for
the fasta file

=item C<title>

Returns the title for the current entry

=item C<accession> 

Returns the accession string for the current entry

=item C<description>

Returns the description for the current entry

=item C<sequence>

Returns the sequence in the current entry

=cut

{
  no strict 'refs';

  for my $met ( qw/fh file current_entry title accession description sequence/ ) {
    *$met = sub {
      shift->{ '_' . $met };
    };
  }
}


sub id {
  my $self = shift;
  PGTools::Util::AccessionHelper->id_for_accession( $self->accession ) || $self->title;
}

=item C<reset>

Reset the pointer to the first entry the fasta file

=cut
sub reset {
  seek shift->fh, 1, 0; 
}


=item C<close>

Close the file that is currently open

=cut
sub close {
  close( shift->fh );
}

=item C<eol>

Returns the end of line character of the current fasta file

=cut
sub eol {
  my $self  = shift;

  return $self->{_eol} 
    if $self->{_eol};

  $self->reset;

  my $fh    = $self->fh;

  local $/ = "\012>";

  EOL_DETECT: while( my $line = <$fh> ) {
    if( length $line > 1 ) {

      $self->{_eol} = ( $line =~ /\015\012/ ) 
        ? "\015\012"
        : "\012";

      last EOL_DETECT;
    }
  }

}


=item C<sequence_trimmed>

Returns the current sequence trimmed, remoing all newlines and all other 
characters except A-Z

=cut
sub sequence_trimmed {
  my $sequence = shift->sequence;

  $sequence =~ tr/a-zA-Z//cd;

  $sequence;
}


=item C<next>

Moves the pointer the next entry in the current fasta file

=cut
sub next {
  my $self  = shift;
  my $fh    = $self->fh;

  local $/ = $self->eol . '>'; 

  $self->{_current_entry} = scalar( <$fh> );

  $self->_setup_sequence;

  $self->current_entry;
}


sub _setup_sequence {
  my $self  = shift;
  my $entry = $self->current_entry;

  @{ $self }{ qw/_title _sequence/ } = split $self->eol, $entry, 2;

  $self->{_sequence} =~ tr/a-zA-Z_//cd;

  $self->_parse_title;
}


sub _parse_title {
  my $self = shift;
  
  @{ $self }{ qw/_accession _description/ } = split /\s+/, $self->title, 2;
}

=back
=cut



1;
__END__
