package QCMG::SamTools::PrimarySeq;

# $Id$

# POD documentation - main docs before the code

=head1 NAME

QCMG::SamTools::PrimarySeq - replacing Bioperl lightweight Sequence Object

=head1 SYNOPSIS


	$seqobj = QCMG::SamTools::PrimarySeq->new ( -seq => 'ATGGGGTGGGCGGTGGGTGGTTTG',
	                                 -id  => 'GeneFragment-12',
	                                 -accession_number => 'X78121',
	                                 -alphabet => 'dna',
	                                 -is_circular => 1 );
	print "Sequence ", $seqobj->id(), " with accession ",
	  $seqobj->accession_number, "\n";

	# to get out parts of the sequence.

	print "Sequence ", $seqobj->id(), " with accession ",
	  $seqobj->accession_number, " and desc ", $seqobj->desc, "\n";

	$string  = $seqobj->seq();
	$string2 = $seqobj->subseq(1,20);

=head1 DESCRIPTION

PrimarySeq is a lightweight Sequence object, storing the sequence, its
name, a computer-useful unique name, and other fundamental attributes.
It does not contain sequence features or other information.  To have a
sequence with sequence features you should use the Seq object which uses
this object.

...

The rest of the documentation details each of the object
methods. Internal methods are usually preceded with a _

=cut


use vars qw($MATCHPATTERN $GAP_SYMBOLS);
use strict;

$MATCHPATTERN = 'A-Za-z\-\.\*\?=~';
$GAP_SYMBOLS = '-~';

#
# setup the allowed values for alphabet()
#

my %valid_type = map {$_, 1} qw( dna rna protein );

=head2 new

 Title   : new
 Usage   : $seq    = QCMG::SamTools::PrimarySeq->new( -seq => 'ATGGGGGTGGTGGTACCCT',
	                                         -id  => 'human_id',
					   -accession_number => 'AL000012',
					   );

 Function: Returns a new primary seq object from
	         basic constructors, being a string for the sequence
	         and strings for id and accession_number.

	         Note that you can provide an empty sequence string. However, in
	         this case you MUST specify the type of sequence you wish to
	         initialize by the parameter -alphabet. See alphabet() for possible
	         values.
 Returns : a new QCMG::SamTools::PrimarySeq object
 Args    : -seq         => sequence string
	         -display_id  => display id of the sequence (locus name)
	         -accession_number => accession number
	         -primary_id  => primary id (Genbank id)
	         -version     => version number
	         -namespace   => the namespace for the accession
	         -authority   => the authority for the namespace
	         -description => description text
	         -desc        => alias for description
	         -alphabet    => sequence type (alphabet) (dna|rna|protein)
	         -id          => alias for display id
	         -is_circular => boolean field for whether or not sequence is circular
	         -direct      => boolean field for directly setting sequence (requires alphabet also set)
	         -ref_to_seq  => boolean field indicating the sequence is a reference (?!?)
	         -nowarnonempty => boolean field for whether or not to warn when sequence is empty

=cut


sub new {
	my $class = shift @_;
	#my @args = shift @_;

	#print STDERR (caller(0))[0]."::".(caller(0))[3], "\n";

	my $self = {};
	bless $self, ref($class) || $class;

	my %arg = @_;

	foreach (keys %arg) {
		print STDERR "$_	=> $arg{$_}\n";
	}

	$self->{name}		= $arg{-name} || $arg{-seqname} || $arg{-display_id} || $arg{-display_name} || $arg{-id};

	$self->{seq}		= $arg{-seq};
	$self->{acc}		= $arg{-accession_number};
	$self->{pid}		= $arg{-pid} || $arg{-primary_id};
	$self->{namespace}	= $arg{-namespace};
	$self->{authority}	= $arg{-authority};
	$self->{version}	= $arg{-version};
	$self->{oid}		= $arg{-oid};
	$self->{desc}		= $arg{-desc};
	$self->{description}	= $arg{-description};
	$self->{alphabet}	= $arg{-alphabet};
	#$self->{direct}		= $arg{-direct};
	#$self->{ref_to_seq}	= $arg{-ref_to_seq};

	print STDERR "Accession: ".$self->{acc}."\n";

	# -nowarnonempty => boolean field for whether or not to warn when sequence is empty
	# private var _nowarnonempty, need to be set before calling _guess_alphabet
	$self->{_nowarnonempty}	= $arg{-nowarnonempty};

	# -display_id  => display id of the sequence (locus name)
	# -id          => alias for display id
	my $id			= $arg{-id};
	my $given_id		= $arg{-given_id};
	if( defined $id && defined $given_id ) {
		if( $id ne $given_id ) {
			$self->throw("Provided both id and display_id constructor functions. [$id] [$given_id]");
		}
	}
	if( defined $given_id ) {
		$id = $given_id;
	}
	$self->{display_id} = $id;

	# -is_circular => boolean field for whether or not sequence is circular
	# is_circular is needed for Bio::PrimarySeqI compliance
	$self->{is_circular} = $arg{-is_circular} || 0;
	 
	# -alphabet    => sequence type (alphabet) (dna|rna|protein)
	# -direct      => boolean field for directly setting sequence (requires alphabet also set)
	# -ref_to_seq  => boolean field indicating the sequence is a reference (?!?)
	# if there is an alphabet, and direct is passed in, assume the alphabet
	# and sequence are ok
	#if($self->{direct} && $self->{ref_to_seq}) {
	#	$self->{seq} = $self->{ref_to_seq};
	#	if( ! $self->{alphabet} ) {
	#		$self->_guess_alphabet();
	#	} # else it has been set already above
	#}
	#else {
		# print STDERR "DEBUG: setting sequence to [$seq]\n";
		# note: the sequence string may be empty
	#	$self->{seq} if defined($arg{-seq});
	#}

	# let's set the length before the seq -- if there is one, this length is
	# going to be invalidated
	$self->{len} = 0;
	$self->{len} = length($self->{seq}) if($self->{seq});

	return $self;
}

sub _rearrange {
	  my $dummy = shift;
	  my $order = shift;
	  
	  return @_ unless (substr($_[0]||'',0,1) eq '-');
	  push @_,undef unless $#_ %2;
	  my %param;
	  while( @_ ) {
	      (my $key = shift) =~ tr/a-z\055/A-Z/d; #deletes all dashes!
	      $param{$key} = shift;
	  }
	  map { $_ = uc($_) } @$order; # for bug #1343, but is there perf hit here?
	  return @param{@$order};
}

sub throw{
	 my ($self,$string) = @_;

	 my $std = $self->stack_trace_dump();

	 my $out = "\n-------------------- EXCEPTION --------------------\n".
	     "MSG: ".$string."\n".$std."-------------------------------------------\n";
	 die $out;

}

sub stack_trace_dump{
	 my ($self) = @_;

	 my @stack = $self->stack_trace();

	 shift @stack;
	 shift @stack;
	 shift @stack;

	 my $out;
	 my ($module,$function,$file,$position);
	 

	 foreach my $stack ( @stack) {
	     ($module,$file,$position,$function) = @{$stack};
	     $out .= "STACK $function $file:$position\n";
	 }

	 return $out;
}


=head2 stack_trace

 Title   : stack_trace
 Usage   : @stack_array_ref= $self->stack_trace
 Function: gives an array to a reference of arrays with stack trace info
	         each coming from the caller(stack_number) call
 Returns : array containing a reference of arrays
 Args    : none


=cut

sub stack_trace{
	 my ($self) = @_;

	 my $i = 0;
	 my @out = ();
	 my $prev = [];
	 while( my @call = caller($i++)) {
	     # major annoyance that caller puts caller context as
	     # function name. Hence some monkeying around...
	     $prev->[3] = $call[3];
	     push(@out,$prev);
	     $prev = \@call;
	 }
	 $prev->[3] = 'toplevel';
	 push(@out,$prev);
	 return @out;
}



sub direct_seq_set {
	  my $obj = shift;
	  return $obj->{'seq'} = shift if @_;
	  return;
}


=head2 seq

 Title   : seq
 Usage   : $string    = $obj->seq()
 Function: Returns the sequence as a string of letters. The
	         case of the letters is left up to the implementer.
	         Suggested cases are upper case for proteins and lower case for
	         DNA sequence (IUPAC standard), but you should not rely on this.
 Returns : A scalar
 Args    : Optionally on set the new value (a string). An optional second
	         argument presets the alphabet (otherwise it will be guessed).

=cut

sub seq {
	 my ($obj,@args) = @_;

	 if( scalar(@args) == 0 ) {
	     return $obj->{'seq'};
	 }

	 my ($value,$alphabet) = @args;

	 if(@args) {
	     if(defined($value) && (! $obj->validate_seq($value))) {
	   $obj->throw("Attempting to set the sequence to [$value] ".
							"which does not look healthy");
		}
	     # if a sequence was already set we make sure that we re-adjust the
	     # alphabet, otherwise we skip guessing if alphabet is already set
	     # note: if the new seq is empty or undef, we don't consider that a
	     # change (we wouldn't have anything to guess on anyway)
		my $is_changed_seq =
		  exists($obj->{'seq'}) && (length($value || '') > 0);
		$obj->{'seq'} = $value;
	     # new alphabet overridden by arguments?
		if($alphabet) {
	   # yes, set it no matter what
			$obj->alphabet($alphabet);
		} elsif( # if we changed a previous sequence to a new one
				  $is_changed_seq ||
				  # or if there is no alphabet yet at all
				  (! defined($obj->alphabet()))) {
			# we need to guess the (possibly new) alphabet
			$obj->_guess_alphabet();
		} # else (seq not changed and alphabet was defined) do nothing
		# if the seq is changed, make sure we unset a possibly set length
		$obj->length(undef) if $is_changed_seq || $obj->{'seq'};
	 }
	 return $obj->{'seq'};
}

=head2 validate_seq

 Title   : validate_seq
 Usage   : if(! $seq->validate_seq($seq_str) ) {
	              print "sequence $seq_str is not valid for an object of
	              alphabet ",$seq->alphabet, "\n";
	   }
 Function: Validates a given sequence string. A validating sequence string
	         must be accepted by seq(). A string that does not validate will
	         lead to an exception if passed to seq().

	         The implementation provided here does not take alphabet() into
	         account. Allowed are all letters (A-Z) and '-','.','*','?','=',
	         and '~'.

 Example :
 Returns : 1 if the supplied sequence string is valid for the object, and
	         0 otherwise.
 Args    : The sequence string to be validated.


=cut

sub validate_seq {
	my ($self,$seqstr) = @_;
	if( ! defined $seqstr ){ $seqstr = $self->seq(); }
	return 0 unless( defined $seqstr);
	if((length($seqstr) > 0) &&
	   ($seqstr !~ /^([$MATCHPATTERN]+)$/)) {
	    $self->warn("seq doesn't validate, mismatch is " .
			join(",",($seqstr =~ /([^$MATCHPATTERN]+)/g)));
		return 0;
	}
	return 1;
}

=head2 subseq

 Title   : subseq
 Usage   : $substring = $obj->subseq(10,40);
 Function: returns the subseq from start to end, where the first sequence
	         character has coordinate 1 number is inclusive, ie 1-2 are the 
	         first two characters of the sequence
 Returns : a string
 Args    : integer for start position

=cut

sub subseq {
	my $self = shift;
	#my @args = @_;
	#my ($start,$end,$nogap,$replace) = $self->_rearrange([qw(START END NOGAP REPLACE_WITH)],@args);
	my $start	= shift @_;
	my $end		= shift @_;
	#my $nogap	= shift @_;
	#my $replace	= shift @_;

	my $subseq = "";

	if(! $start || ! $end || ! $self->{seq}) {
		print STDERR "Start: $start, End: $end, Sequence: ".$self->{seq}."\n";
		warn("Incorrect parameters to subseq - must be a sequence and two integers: start and end coordinates");
	}
	elsif($end > length($self->{seq})) {
	     warn("subseq(): End coordinate out of range");
	}
	else {
		$start -= 1;	# account for coordinates starting at 1, perl indexing at 0
		my $length = $end - $start;
		$subseq = substr($self->{seq}, $start, $length);
	}

	return($subseq);
}

=head2 length

 Title   : length
 Usage   : $len = $seq->length();
 Function: Get the length of the sequence in number of symbols (bases
	         or amino acids).

	         You can also set this attribute, even to a number that does
	         not match the length of the sequence string. This is useful
	         if you don''t want to set the sequence too, or if you want
	         to free up memory by unsetting the sequence. In the latter
	         case you could do e.g.

	             $seq->length($seq->length);
	             $seq->seq(undef);

	         Note that if you set the sequence to a value other than
	         undef at any time, the length attribute will be
	         invalidated, and the length of the sequence string will be
	         reported again. Also, we won''t let you lie about the length.

 Example :
 Returns : integer representing the length of the sequence.
 Args    : Optionally, the value on set

=cut

sub length {
	  my $self = shift;
	  my $len = length($self->seq() || '');

	  if(@_) {
		 my $val = shift;
		 if(defined($val) && $len && ($len != $val)) {
			 $self->throw("You're trying to lie about the length: ".
							  "is $len but you say ".$val);
		 }
		 $self->{'_seq_length'} = $val;
	  } elsif(defined($self->{'_seq_length'})) {
		 return $self->{'_seq_length'};
	  }
	  return $len;
}

=head2 display_id

 Title   : display_id or display_name
 Usage   : $id_string = $obj->display_id();
 Function: returns the display id, aka the common name of the Sequence object.

	         The semantics of this is that it is the most likely string to
	         be used as an identifier of the sequence, and likely to have
	         "human" readability.  The id is equivalent to the ID field of
	         the GenBank/EMBL databanks and the id field of the
	         Swissprot/sptrembl database. In fasta format, the >(\S+) is
	         presumed to be the id, though some people overload the id to
	         embed other information. Bioperl does not use any embedded
	         information in the ID field, and people are encouraged to use
	         other mechanisms (accession field for example, or extending
	         the sequence object) to solve this.

	         With the new Bio::DescribeableI interface, display_name aliases
	         to this method.

 Returns : A string
 Args    : None


=cut

sub display_id {
	 my ($obj,$value) = @_;
	 if( defined $value) {
	    $obj->{'display_id'} = $value;
	}
	return $obj->{'display_id'};
}

=head2 accession_number

 Title   : accession_number or object_id
 Usage   : $unique_key = $obj->accession_number;
 Function: Returns the unique biological id for a sequence, commonly
	         called the accession_number. For sequences from established
	         databases, the implementors should try to use the correct
	         accession number. Notice that primary_id() provides the
	         unique id for the implemetation, allowing multiple objects
	         to have the same accession number in a particular implementation.

	         For sequences with no accession number, this method should
	         return "unknown".

	         [Note this method name is likely to change in 1.3]

	         With the new Bio::IdentifiableI interface, this is aliased
	         to object_id

 Returns : A string
 Args    : A string (optional) for setting

=cut

sub accession_number {
	  #my( $obj, $acc ) = @_;

	  #if (defined $acc) {
		 #$obj->{'accession_number'} = $acc;
	  #} else {
		 #$acc = $obj->{'accession_number'};
		 #$acc = 'unknown' unless defined $acc;
	  #}
	  #return $acc;

	my $self = shift;

	return($self->{acc});
}

=head2 primary_id

 Title   : primary_id
 Usage   : $unique_key = $obj->primary_id;
 Function: Returns the unique id for this object in this
	         implementation. This allows implementations to manage their
	         own object ids in a way the implementaiton can control
	         clients can expect one id to map to one object.

	         For sequences with no natural primary id, this method
	         should return a stringified memory location.

 Returns : A string
 Args    : A string (optional, for setting)

=cut

sub primary_id {
	  my $obj = shift;

	  if(@_) {
		 $obj->{'primary_id'} = shift;
	  }
	  if( ! defined($obj->{'primary_id'}) ) {
		 return "$obj";
	  }
	  return $obj->{'primary_id'};
}


=head2 alphabet

 Title   : alphabet
 Usage   : if( $obj->alphabet eq 'dna' ) { /Do Something/ }
 Function: Get/Set the alphabet of sequence, one of
	         'dna', 'rna' or 'protein'. This is case sensitive.

	         This is not called <type> because this would cause
	         upgrade problems from the 0.5 and earlier Seq objects.

 Returns : a string either 'dna','rna','protein'. NB - the object must
	         make a call of the type - if there is no alphabet specified it
	         has to guess.
 Args    : optional string to set : 'dna' | 'rna' | 'protein'


=cut

sub alphabet {
	  my ($obj,$value) = @_;
	  if (defined $value) {
		 $value = lc $value;
		 unless ( $valid_type{$value} ) {
			 $obj->throw("Alphabet '$value' is not a valid alphabet (".
							 join(',', map "'$_'", sort keys %valid_type) .
							 ") lowercase");
		 }
		 $obj->{'alphabet'} = $value;
	  }
	  return $obj->{'alphabet'};
}

=head2 desc

 Title   : desc or description
 Usage   : $obj->desc($newval)
 Function: Get/set description of the sequence.

	         'description' is an alias for this for compliance with the
	         Bio::DescribeableI interface.

 Example :
 Returns : value of desc (a string)
 Args    : newvalue (a string or undef, optional)


=cut

sub desc{
	  my $self = shift;

	  return $self->{'desc'} = shift if @_;
	  return $self->{'desc'};
}

=head2 can_call_new

 Title   : can_call_new
 Usage   :
 Function:
 Example :
 Returns : true
 Args    :


=cut

sub can_call_new {
	 my ($self) = @_;

	 return 1;
}

=head2 id

 Title   : id
 Usage   : $id = $seq->id()
 Function: This is mapped on display_id
 Example :
 Returns :
 Args    :


=cut

sub  id {
	 return shift->display_id(@_);
}

=head2 is_circular

 Title   : is_circular
 Usage   : if( $obj->is_circular) { /Do Something/ }
 Function: Returns true if the molecule is circular
 Returns : Boolean value
 Args    : none

=cut

sub is_circular{
	  my $self = shift;

	  return $self->{'is_circular'} = shift if @_;
	  return $self->{'is_circular'};
}


=head1 Methods for Bio::IdentifiableI compliance

=cut

=head2 object_id

 Title   : object_id
 Usage   : $string    = $obj->object_id()
 Function: A string which represents the stable primary identifier
	         in this namespace of this object. For DNA sequences this
	         is its accession_number, similarly for protein sequences.

	         This is aliased to accession_number().
 Returns : A scalar


=cut

sub object_id {
	  return shift->accession_number(@_);
}

=head2 version

 Title   : version
 Usage   : $version    = $obj->version()
 Function: A number which differentiates between versions of
	         the same object. Higher numbers are considered to be
	         later and more relevant, but a single object described
	         the same identifier should represent the same concept.

 Returns : A number

=cut

sub version{
	  my ($self,$value) = @_;
	  if( defined $value) {
		 $self->{'_version'} = $value;
	  }
	  return $self->{'_version'};
}


=head2 authority

 Title   : authority
 Usage   : $authority    = $obj->authority()
 Function: A string which represents the organisation which
	         granted the namespace, written as the DNS name for
	         organisation (eg, wormbase.org).

 Returns : A scalar

=cut

sub authority {
	  my ($obj,$value) = @_;
	  if( defined $value) {
		 $obj->{'authority'} = $value;
	  }
	  return $obj->{'authority'};
}

=head2 namespace

 Title   : namespace
 Usage   : $string    = $obj->namespace()
 Function: A string representing the name space this identifier
	         is valid in, often the database name or the name
	         describing the collection.

 Returns : A scalar


=cut

sub namespace{
	  my ($self,$value) = @_;
	  if( defined $value) {
		 $self->{'namespace'} = $value;
	  }
	  return $self->{'namespace'} || "";
}

=head1 Methods for Bio::DescribableI compliance

This comprises of display_name and description.

=cut

=head2 display_name

 Title   : display_name
 Usage   : $string    = $obj->display_name()
 Function: A string which is what should be displayed to the user.
	         The string should have no spaces (ideally, though a cautious
	         user of this interface would not assumme this) and should be
	         less than thirty characters (though again, double checking
	         this is a good idea).

	         This is aliased to display_id().
 Returns : A scalar

=cut

sub display_name {
	  return shift->display_id(@_);
}

=head2 description

 Title   : description
 Usage   : $string    = $obj->description()
 Function: A text string suitable for displaying to the user a
	         description. This string is likely to have spaces, but
	         should not have any newlines or formatting - just plain
	         text. The string should not be greater than 255 characters
	         and clients can feel justified at truncating strings at 255
	         characters for the purposes of display.

	         This is aliased to desc().
 Returns : A scalar

=cut

sub description {
	  return shift->desc(@_);
}


=head1 Internal methods

These are internal methods to PrimarySeq

=cut

=head2 _guess_alphabet

 Title   : _guess_alphabet
 Usage   :
 Function: Determines (and sets) the type of sequence: dna, rna, protein
 Example :
 Returns : one of strings 'dna', 'rna' or 'protein'.
 Args    : none


=cut

sub _guess_alphabet {
	 my ($self) = @_;
	 my $type;

	#return if $self->alphabet;

	 my $str = $self->seq();
	# Remove chars that clearly denote ambiguity
	 $str =~ s/[-.?]//gi;

	 my $total = CORE::length($str);
	 if( $total == 0 ) {
	   if (!$self->{'_nowarnonempty'}) {
	     $self->warn("Got a sequence with no letters in it ".
	         "cannot guess alphabet");
	   }
	   return '';
	 }

	 my $u = ($str =~ tr/Uu//);
	# The assumption here is that most of sequences comprised of mainly
	 # ATGC, with some N, will be 'dna' despite the fact that N could
	# also be Asparagine
	 my $atgc = ($str =~ tr/ATGCNatgcn//);

	 if( ($atgc / $total) > 0.85 ) {
	     $type = 'dna';
	 } elsif( (($atgc + $u) / $total) > 0.85 ) {
	     $type = 'rna';
	 } else {
	     $type = 'protein';
	 }

	 $self->alphabet($type);
	 return $type;
}

1;

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
