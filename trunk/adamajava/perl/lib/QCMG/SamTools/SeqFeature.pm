package QCMG::SamTools::SeqFeature;

# $Id$

=pod

=head1 NAME

QCMG::SamTools::SeqFeature -- Replacement for Bio::SeqFeature::Lite

=head1 SYNOPSIS

 my $sf = QCMG::SamTools::SeqFeature->new(
                );

=head1 DESCRIPTION

This class contains methods that mimic those in Bio::SeqFeature::Lite, but avoid
the significant overhead of Bio::Perl itself. Possibly everything may not be
completely compatible with other Bio::Perl classes.


=cut

use strict;

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 None.

Returns:
 a new instance of this class.



 $feature = Bio::SeqFeature::Lite->new(@args);

This method creates a new feature object. You can create a simple feature that contains no subfeatures, or a hierarchically nested object.

Arguments are as follows:

  -seq_id      the reference sequence
  -start       the start position of the feature
  -end         the stop position of the feature
  -stop        an alias for end
  -name        the feature name (returned by seqname())
  -type        the feature type (returned by primary_tag())
  -primary_tag the same as -type
  -source      the source tag
  -score       the feature score (for GFF compatibility)
  -desc        a description of the feature
  -segments    a list of subfeatures (see below)
  -subtype     the type to use when creating subfeatures
  -strand      the strand of the feature (one of -1, 0 or +1)
  -phase       the phase of the feature (0..2)
  -seq         a dna or protein sequence string to attach to feature
  -id          an alias for -name
  -seqname     an alias for -name
  -display_id  an alias for -name
  -display_name an alias for -name  (do you get the idea the API has changed?)
  -primary_id  unique database ID
  -url         a URL to link to when rendered with Bio::Graphics
  -attributes  a hashref of tag value attributes, in which the key is the tag and the value is an array reference of values
  -factory     a reference to a feature factory, used for compatibility with more obscure parts of Bio::DB::GFF
=cut


# usage:
# Bio::SeqFeature::Lite->new(
#                         -start => 1,
#                         -end   => 100,
#                         -name  => 'fred feature',
#                         -strand => +1);
#
# Alternatively, use -segments => [ [start,stop],[start,stop]...]
# to create a multisegmented feature.

sub new {
=cut
  my $class= shift;
  $class = ref($class) if ref $class;
  my %arg = @_;

  my $self = bless {},$class;
=cut
	my $class = shift @_;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $self = {};
	bless $self, ref($class) || $class;


  my %arg = @_;

  $arg{-strand} ||= 0;
  if ($arg{-strand} =~ /^[\+\-\.]$/){
	$arg{-strand} = "+" && $self->{strand} ='1';
	$arg{-strand} = "-" && $self->{strand} = '-1';
	$arg{-strand} = "." && $self->{strand} = '0';
  } else {
	  $self->{strand}  = $arg{-strand} ? ($arg{-strand} >= 0 ? +1 : -1) : 0;
  }
  $self->{name}    = $arg{-name}   || $arg{-seqname} || $arg{-display_id} 
    || $arg{-display_name} || $arg{-id};
  $self->{type}    = $arg{-type}   || $arg{-primary_tag} || 'feature';
  $self->{subtype} = $arg{-subtype} if exists $arg{-subtype};
  $self->{source}  = $arg{-source} || $arg{-source_tag} || '';
  $self->{score}   = $arg{-score}   if exists $arg{-score};
  $self->{start}   = $arg{-start};
  $self->{stop}    = exists $arg{-end} ? $arg{-end} : $arg{-stop};
  $self->{ref}     = $arg{-seq_id} || $arg{-ref};
  for my $option (qw(class url seq phase desc attributes primary_id)) {
    $self->{$option} = $arg{"-$option"} if exists $arg{"-$option"};
  }

  # is_circular is needed for Bio::PrimarySeqI compliance
  $self->{is_circular} = $arg{-is_circular} || 0;

  # fix start, stop
  if (defined $self->{stop} && defined $self->{start}
      && $self->{stop} < $self->{start}) {
    @{$self}{'start','stop'} = @{$self}{'stop','start'};
    $self->{strand} *= -1;
  }

  my @segments;
  if (my $s = $arg{-segments}) {
    # NB: when $self ISA Bio::DB::SeqFeature the following invokes
    # Bio::DB::SeqFeature::add_segment and not
    # Bio::DB::SeqFeature::add_segment (as might be expected?)
    $self->add_segment(@$s);
  }

  $self;
}

################################################################################

=pod

Most subroutines were directly ripped out Bio::Perl code and modified as
necessary...

=cut

sub segments {
  my $self = shift;
  my $s = $self->{segments} or return wantarray ? () : 0;
  @$s;
}
sub score    {
  my $self = shift;
  my $d = $self->{score};
  $self->{score} = shift if @_;
  $d;
}
sub primary_tag     { 
    my $self = shift;
    my $d    = $self->{type};
    $self->{type} = shift if @_;
    $d;
}
sub name            {
  my $self = shift;
  my $d    = $self->{name};
  $self->{name} = shift if @_;
  $d;
}
sub seq_id          { shift->ref(@_)         }
sub ref {
  my $self = shift;
  my $d = $self->{ref};
  $self->{ref} = shift if @_;
  $d;
}
sub start    {
  my $self = shift;
  my $d = $self->{start};
  $self->{start} = shift if @_;
  if (my $rs = $self->{refseq}) {
    my $strand = $rs->strand || 1;
    return $strand >= 0 ? ($d - $rs->start + 1) : ($rs->end - $d + 1);
  } else {
    return $d;
  }
}
sub end    {
  my $self = shift;
  my $d = $self->{stop};
  $self->{stop} = shift if @_;
  if (my $rs = $self->{refseq}) {
    my $strand = $rs->strand || 1;
    return $strand >= 0 ? ($d - $rs->start + 1) : ($rs->end - $d + 1);
  }
  $d;
}
sub strand {
  my $self = shift;
  my $d = $self->{strand};
  $self->{strand} = shift if @_;
  if (my $rs = $self->{refseq}) {
    my $rstrand = $rs->strand;
    return  0 unless $d;
    return  1 if $rstrand == $d;
    return -1 if $rstrand != $d;
  }
  $d;
}

# this does nothing, but it is here for compatibility reasons
sub absolute {
  my $self = shift;
  my $d = $self->{absolute};
  $self->{absolute} = shift if @_;
  $d;
}

sub abs_start {
  my $self = shift;
  local $self->{refseq} = undef;
  $self->start(@_);
}
sub abs_end {
  my $self = shift;
  local $self->{refseq} = undef;
  $self->end(@_);
}
sub abs_strand {
  my $self = shift;
  local $self->{refseq} = undef;
  $self->strand(@_);
}

sub length {
  my $self = shift;
  return $self->end - $self->start + 1;
}

#is_circular is needed for Bio::PrimarySeqI
sub is_circular {
  my $self = shift;
  my $d = $self->{is_circular};
  $self->{is_circular} = shift if @_;
  $d;
}


sub seq {
  my $self = shift;
  my $seq =  exists $self->{seq} ? $self->{seq} : '';
  return $seq;
}

sub dna {
  my $seq = shift->seq;
  $seq    = $seq->seq if CORE::ref($seq);
  return $seq;
}

# added so this will run...
sub class {
  my $self        = shift;
  return 'QCMG::SamTools::SeqFeature';
}
# added so this will run...
sub source_tag {
  my $self        = shift;
  return;
}


sub add_segment {
  my $self        = shift;
  my $type = $self->{subtype} || $self->{type};
  $self->{segments} ||= [];
  my $ref   = $self->seq_id;
  my $name  = $self->name;
  my $class = $self->class;
  my $source_tag = $self->source_tag;

  my $min_start = $self->start ||  999_999_999_999;
  my $max_stop  = $self->end   || -999_999_999_999;

  my @segments = @{$self->{segments}};

  for my $seg (@_) {
    if (CORE::ref($seg) eq 'ARRAY') {
      my ($start,$stop) = @{$seg};
      next unless defined $start && defined $stop;  # fixes an obscure bug somewhere above us
      my $strand = $self->{strand};

      if ($start > $stop) {
	($start,$stop) = ($stop,$start);
	$strand = -1;
      }

      push @segments,$self->new(-start  => $start,
				-stop   => $stop,
				-strand => $strand,
				-ref    => $ref,
				-type   => $type,
			        -name   => $name,
			        -class  => $class,
				-phase  => $self->{phase},
				-score  => $self->{score},
				-source_tag  => $source_tag,
				-attributes  => $self->{attributes},
			       );
      $min_start = $start if $start < $min_start;
      $max_stop  = $stop  if $stop  > $max_stop;

    } elsif (CORE::ref $seg) {
      push @segments,$seg;

      $min_start = $seg->start if ($seg->start && $seg->start < $min_start);
      $max_stop  = $seg->end   if ($seg->end && $seg->end > $max_stop);
    }
  }
  if (@segments) {
    local $^W = 0;  # some warning of an uninitialized variable...
    $self->{segments} = \@segments;
    $self->{ref}    ||= $self->{segments}[0]->seq_id;
    $self->{start}    = $min_start;
    $self->{stop}     = $max_stop;
  }
}





1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2011

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
