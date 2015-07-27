package QCMG::AlexaSeq::Read;

use strict;
use vars qw(
		@ISA @EXPORT @EXPORT_OK %EXPORT_TAGS 
	);
use Exporter;

=pod

=head1 VARIABLES AND TAGS

 Module version
  $VERSION
 
=cut

@ISA = qw(Exporter);
@EXPORT = qw( 
				%EXPORT_TAGS
		);
@EXPORT_OK = qw();
%EXPORT_TAGS =  (
				);

################################################################################

=pod

=head1 METHODS

B<new()> (constructor)

my $t = Read->new();

Create a new instance of this class.

Parameters:
 None.

Returns:
 a new instance of this class.

=cut
sub new {
	my $class = shift @_;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $self = {
			_readid		=> undef,
			_readnum    => undef,
			_alnstart   => undef,
			_alnchrstart => undef,
			_alnchr => undef,
			#_rs         => undef,
			_alnfeaturetype => undef,
			_alnfeatureid  => undef,
			_alnbitscore => undef,
			_aligngeneid => undef,
			_newbitscore => undef,
			_distancetopair => undef,
			_alnlength => undef
			#_r1_readid	=> undef,
			#_r2_readid	=> undef,
			#_r1_start	=> undef,
			#_r2_start	=> undef,
			#_r1_end		=> undef,
			#_r2_end		=> undef,
			#_r1_rs		=> undef,
			#_r2_rs		=> undef,
			#_r1_featureid	=> undef,
			#_r2_featureid	=> undef,
			#_r1_strand	=> undef,
			#_r2_strand	=> undef,
			#_r1_bitscore	=> undef,
			#_r2_bitscore	=> undef
	};

	bless ($self, $class);

        # parse params
	my $options = {};
	$self->{options} = $options;
        for(my $i=0; $i<=$#_; $i+=2) {
                my $this_sub = (caller(0))[0]."::".(caller(0))[3];
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	return $self;
}

sub readid {
	my ( $self, $readid ) = @_;
	$self->{_readid} = $readid if defined($readid);
	return $self->{_readid};
}
sub alnstart {
	my ( $self, $alnstart ) = @_;
	$self->{_alnstart} = $alnstart if defined($alnstart);
	return $self->{_alnstart};
}
sub alnchr {
	my ( $self, $alnchr ) = @_;
	$self->{_alnchr} = $alnchr if defined($alnchr);
	return $self->{_alnchr};
}
sub alnchrstart {
	my ( $self, $alnchrstart ) = @_;
	$self->{_alnchrstart} = $alnchrstart if defined($alnchrstart);
	return $self->{_alnchrstart};
}
sub distancetopair {
	my ( $self, $distancetopair ) = @_;
	$self->{_distancetopair} = $distancetopair if defined($distancetopair);
	return $self->{_distancetopair};
}

#sub rs {
#	my ( $self, $rs ) = @_;
#	$self->{_rs} = $rs if defined($rs);
#	return $self->{_rs};
#}

sub alnfeatureid {
	my ( $self, $alnfeatureid ) = @_;
	$self->{_alnfeatureid} = $alnfeatureid if defined($alnfeatureid);
	return $self->{_alnfeatureid};
}
sub alnfeaturetype {
	my ( $self, $alnfeaturetype ) = @_;
	$self->{_alnfeaturetype} = $alnfeaturetype if defined($alnfeaturetype);
	return $self->{_alnfeaturetype};
}
sub alnbitscore {
	my ( $self, $alnbitscore ) = @_;
	$self->{_alnbitscore} = $alnbitscore if defined($alnbitscore);
	return $self->{_alnbitscore};
}
sub newbitscore {
	my ( $self, $newbitscore ) = @_;
	$self->{_newbitscore} = $newbitscore if defined($newbitscore);
	return $self->{_newbitscore};
}
sub alngeneid {
	my( $self, $alngeneid ) = @_;
	$self->{_alngeneid} = $alngeneid if defined($alngeneid);
	return $self->{_alngeneid};
}
sub alnlength {
	my ( $self, $alnlength ) = @_;
	$self->{_alnlength} = $alnlength if defined($alnlength);
	return $self->{_alnlength};
}
=cut
sub r1_readid {
	my ( $self, $r1_readid ) = @_;
	$self->{_r1_readid} = $r1_readid if defined($r1_readid);
	return $self->{_r1_readid};
}
sub r1_start {
	my ( $self, $r1_start ) = @_;
	$self->{_r1_start} = $r1_start if defined($r1_start);
	return $self->{_r1_start};
}
sub r1_end {
	my ( $self, $r1_end ) = @_;
	$self->{_r1_end} = $r1_end if defined($r1_end);
	return $self->{_r1_end};
}
sub r1_rs {
	my ( $self, $r1_rs ) = @_;
	$self->{_r1_rs} = $r1_rs if defined($r1_rs);
	return $self->{_r1_rs};
}
sub r1_featureid {
	my ( $self, $r1_featureid ) = @_;
	$self->{_r1_featureid} = $r1_featureid if defined($r1_featureid);
	return $self->{_r1_featureid};
}
sub r1_strand {
	my ( $self, $r1_strand ) = @_;
	$self->{_r1_strand} = $r1_strand if defined($r1_strand);
	return $self->{_r1_strand};
}
sub r1_bitscore {
	my ( $self, $r1_bitscore ) = @_;
	$self->{_r1_bitscore} = $r1_bitscore if defined($r1_bitscore);
	return $self->{_r1_bitscore};
}

sub r2_readid {
	my ( $self, $r2_readid ) = @_;
	$self->{_r2_readid} = $r2_readid if defined($r2_readid);
	return $self->{_r2_readid};
}
sub r2_start {
	my ( $self, $r2_start ) = @_;
	$self->{_r2_start} = $r2_start if defined($r2_start);
	return $self->{_r2_start};
}
sub r2_end {
	my ( $self, $r2_end ) = @_;
	$self->{_r2_end} = $r2_end if defined($r2_end);
	return $self->{_r2_end};
}
sub r2_rs {
	my ( $self, $r2_rs ) = @_;
	$self->{_r2_rs} = $r2_rs if defined($r2_rs);
	return $self->{_r2_rs};
}
sub r2_featureid {
	my ( $self, $r2_featureid ) = @_;
	$self->{_r2_featureid} = $r2_featureid if defined($r2_featureid);
	return $self->{_r2_featureid};
}
sub r2_strand {
	my ( $self, $r2_strand ) = @_;
	$self->{_r2_strand} = $r2_strand if defined($r2_strand);
	return $self->{_r2_strand};
}
sub r2_bitscore {
	my ( $self, $r2_bitscore ) = @_;
	$self->{_r2_bitscore} = $r2_bitscore if defined($r2_bitscore);
	return $self->{_r2_bitscore};
}

=cut

1;

__END__

=head1 COPYRIGHT

Copyright (c) The University of Queensland 2009-2014

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
