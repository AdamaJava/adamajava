package QCMG::Coverage;

=pod

=head1 NAME

QCMG::Coverage -- Common functions for parsing coverage stats 

=head1 SYNOPSIS

Most common use:
 my $qc = QCMG::Coverage->new();

To query a different database:
 my $qc = QCMG::Coverage->new();

=head1 DESCRIPTION



=head1 REQUIREMENTS


=cut

use strict;
use vars qw(	@ISA @EXPORT @EXPORT_OK %EXPORT_TAGS $VERSION 
	);
use Data::Dumper;

#use lib qw(/Users/l.fink/0_projects/qcmg/devel/QCMGPerl/lib);

=pod

=head1 VARIABLES AND TAGS

 Module version
  $VERSION

=cut

$VERSION		= 0.01;


@ISA = qw(Exporter);
@EXPORT = qw( 
		%EXPORT_TAGS $VERSION 
	);
@EXPORT_OK = qw(
		);

%EXPORT_TAGS = ( all => [ @EXPORT_OK ] );


################################################################################
=pod

=head1 METHODS

B<new()> (constructor)

Create a new instance of this class.

Parameters:
 FILE	  -> file with all coverage stats
 TYPE     -> type of feature to extract (bait, exon, tier1, etc.)
 COVERAGE -> type of coverage to extract (sequence, physical)

 (all required)

Returns:
 a new instance of this class.

=cut

sub new {
	my $class = shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

	my $self = {};
	bless ($self, $class);

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	$self->{'FILE'}		= $options->{'FILE'};
	$self->{'TYPE'}		= $options->{'TYPE'};
	$self->{'COVERAGE'}	= $options->{'COVERAGE'};
	$self->{'VERBOSE'}	= $options->{'VERBOSE'};

	print STDERR "Coverage file:\t$self->{'FILE'}\n"	if($self->{'VERBOSE'} > 0);
	print STDERR "Coverage type:\t$self->{'COVERAGE'}\n"	if($self->{'VERBOSE'} > 0);
	print STDERR "Feature type:\t$self->{'TYPE'}\n"		if($self->{'VERBOSE'} > 0);

	$self->get_file_type();	# pfx, afx, aft

	return($self);
}

################################################################################
=pod

B<get_file_type()>

 Determine whether file is a per-feature XML file; all-features XML file; or an
 all-features text file

Parameters:

Returns:
 scalar: file type (pfx, afx, aft) (per-feature XML, all-feature XML,
					all-feature text)

=cut

sub get_file_type {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }


	local($/) = undef;
	open(FH, $self->{'FILE'});
	my $fc = <FH>;
	close(FH);

	# per-feature XML
	if($fc =~ /<coverageReport/ && $fc =~ /feature=\".+?\s+\.+?\s+.+?\s+\.+?/) {
		$self->{'ftype'}	= 'pfx';
	}
	elsif($fc =~ /<coverageReport/ && $fc =~ /feature=\"[\w\_]+\"/) {
		$self->{'ftype'}	= 'afx';
	}
	elsif($fc =~ /#coveragetype/) {
		$self->{'ftype'}	= 'aft';
	}
	else {
		warn "Unknown coverage file type; defaulting to all-feature text file\n";
		$self->{'ftype'}	= 'aft';
	}

	print STDERR "File type:\t$self->{'ftype'}\n" if($self->{'VERBOSE'} > 1);

	return($self->{'ftype'});
}

################################################################################
=pod

B<_read_xml_per_feature()>

 Read a per-feature XML file and extract the coverages for each feature

Parameters:

Returns:
 reference to a hash of references to a hash: 

   key = feature name, value = level of coverage, value = number of bases w
         level of coverage
   $hash->{feature name}->{level of coverage} = number of bases

=cut

sub _read_xml_per_feature {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $features;

	local($/) = undef;
	open(FH, $self->{'FILE'});
	my $fc = <FH>;
	close(FH);

=cut
    <coverageReport type="physical" feature="chr10	Agilent	bait 102775360	102775600	.	+	.">
        <coverage at="0" bases="478"/>
    </coverageReport>
=cut

	# get all entries for feature with the requested type of coverage
	my @features = ($fc =~ /(<coverageReport\s+type="$self->{'COVERAGE'}".+?coverageReport>)/sg);

	foreach (@features) {
		# skip unwanted feature types
		next unless(/feature=\".+?\s+.+?\s+$self->{'TYPE'}\s+\d+/);

		# get feature name string
		/feature=\"(.+?)\"/;
		my $fid = $1;

		#print STDERR "$fid\n";

		# get all levels of coverage and number of bases in each
		my @cov = (/(<coverage at="\d+" bases="\d+"\/>)/sg);
	
		foreach my $c (@cov) {
			$c =~ /<coverage at=\"(\d+)\" bases=\"(\d+)\"/;
			my $level = $1;
			my $bases = $2;
			#print STDERR "$level -> $bases\n";

			$self->{'features'}->{$fid}->{$level} = $bases;
		}
	}

	return($self->{'features'});
}

################################################################################
=pod

B<_read_xml_all_features()>

 Read an XML file and extract coverage stats for the requested feature type

Parameters:

Returns:
 reference to a hash of references to a hash: 

   key = level of coverage, value = number of bases w
         level of coverage
   $hash->{level of coverage} = number of bases

=cut

sub _read_xml_all_features {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $features;

	local($/) = undef;
	open(FH, $self->{'FILE'});
	my $fc = <FH>;
	close(FH);
=cut
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:QCoverageStats xmlns:ns2="http://www.qcmg.org">
    <coverageReport type="physical" feature="gene">
        <coverage at="0" bases="32884805"/>
    </coverageReport>
    <coverageReport type="physical" feature="gene">
        <coverage at="2" bases="499"/>
    </coverageReport>
    <coverageReport type="physical" feature="CDS">
        <coverage at="0" bases="849970"/>
    </coverageReport>
    <coverageReport type="physical" feature="stop_codon">
        <coverage at="0" bases="496"/>
    </coverageReport>
    <coverageReport type="physical" feature="exon">
        <coverage at="0" bases="1726564"/>
    </coverageReport>
</ns2:QCoverageStats>
=cut

	# get all entries for feature with the requested type of coverage
	my @features = ($fc =~ /(<coverageReport\s+type="$self->{'COVERAGE'}".+?coverageReport>)/sg);

	foreach (@features) {
		# skip unwanted feature types
		next unless(/feature=\"$self->{'TYPE'}\"/);

		# get all levels of coverage and number of bases in each
		my @cov = (/(<coverage at="\d+" bases="\d+"\/>)/sg);
	
		foreach my $c (@cov) {
			$c =~ /<coverage at=\"(\d+)\" bases=\"(\d+)\"/;
			my $level = $1;
			my $bases = $2;
			#print STDERR "$self->{'TYPE'} : $level -> $bases\n";

			$self->{'coverage'}->{ $self->{'TYPE'} }->{$level} = $bases;
		}
	}

	return($self->{'coverage'});
}

################################################################################
=pod

B<_read_text_all_features()>

 Read a text file and extract coverage stats for the requested feature type

Parameters:

Returns:
 reference to a hash of references to a hash: 

   key = level of coverage, value = number of bases w
         level of coverage
   $hash->{level of coverage} = number of bases

=cut

sub _read_text_all_features {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $features;

=cut
#coveragetype   featuretype     numberofbases   coverage
sequence        tier1   97654483        0x
sequence        tier1   962691  1x
sequence        tier1   284142  2x
sequence        tier1   184546  3x
sequence        tier1   141347  4x
sequence        tier1   114039  5x
sequence        tier1   98039   6x
...
sequence        exon    45167093        0x
sequence        exon    821368  1x
sequence        exon    302918  2x
sequence        exon    203726  3x
sequence        exon    162206  4x
sequence        exon    133174  5x
sequence        exon    118222  6x
...
=cut

	open(FH, $self->{'FILE'});
	while(<FH>) {
		# skip header
		next if(/^#/);

		# skip unrequested coverage type and feature type
		next unless(/$self->{'COVERAGE'}\s+$self->{'TYPE'}/);

		/$self->{'COVERAGE'}\s+$self->{'TYPE'}\s+(\d+)\s+(\d+)x/;
		my $level = $2;
		my $bases = $1;
		#print STDERR "$self->{'TYPE'} : $level -> $bases\n";

		$self->{'coverage'}->{ $self->{'TYPE'} }->{$level} = $bases;
	}
	close(FH);

	return($self->{'coverage'});
}

################################################################################
=pod

B<get_coverage()>

 Get all coverage stats

Parameters:

Returns:

=cut

sub get_coverage {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        #my $options	= {};
        #for(my $i=0; $i<=$#_; $i+=2) {
        #        defined($_[($i + 1)]) || 
        #                die print STDERR "Odd number of params: $this_sub : $!";
        #        $options->{uc($_[$i])} = $_[($i + 1)];
        #}

	if($self->{'ftype'} eq 'pfx') {
		$self->{'features'} = $self->_read_xml_per_feature();

		return($self->{'features'});

	}
	elsif($self->{'ftype'} eq 'afx') {
		$self->{'features'} = $self->_read_xml_all_features();

		return($self->{'features'});
	}
	else {
		$self->{'features'} = $self->_read_text_all_features();

		return($self->{'features'});
	}
}

################################################################################
=pod

B<min_feature_coverage()>

 Find the lowest level of coverage for a given feature or file

Parameters:
 FEATURE  -> hash with coverage stats for a single feature
             (key = coverage level, value = number of bases)

Returns:

=cut

sub min_feature_coverage {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my @srt = sort {$a <=> $b} keys %{$options->{'FEATURE'}};

	return($srt[0]);
}

################################################################################
=pod

B<min_coverage()>

 Find the lowest level of coverage for a given feature type

Parameters:
 COVERAGE  -> hash with coverage stats for a single feature
             (key = coverage level, value = number of bases)

Returns:

=cut

sub min_coverage {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my @srt = sort {$a <=> $b} keys %{$options->{'COVERAGE'}};

	return($srt[0]);
}

################################################################################
=pod

B<max_feature_coverage()>

 Find the highest level of coverage for a given feature or file

Parameters:
 FEATURE  -> hash with coverage stats for a single feature
             (key = coverage level, value = number of bases)

Returns:

=cut

sub max_feature_coverage {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my @srt = sort {$b <=> $a} keys %{$options->{'FEATURE'}};

	return($srt[0]);
}

################################################################################
=pod

B<max_coverage()>

 Find the highest level of coverage for a given feature type 

Parameters:
 COVERAGE  -> hash with coverage stats for a single feature
             (key = coverage level, value = number of bases)

Returns:

=cut

sub max_coverage {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my @srt = sort {$b <=> $a} keys %{$options->{'COVERAGE'}};

	return($srt[0]);
}

################################################################################
=pod

B<ave_feature_coverage()>

 Find the "average" level of coverage for a given feature or file

Parameters:
 FEATURE  -> hash with coverage stats for a single feature
             (key = coverage level, value = number of bases)

Returns:
 scalar int -> "average" coverage

=cut

sub ave_feature_coverage {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $sum		= 0;
	my $count	= 0;

	# coverage levels are sorted by increasing coverage

	# check for zero coverage cases
        if(	scalar(keys %{$options->{'FEATURE'}} ) == 1 &&
		$options->{'FEATURE'}->{$_}->{0} == 0) {

		return(0);
	}
	

	# $_				=> coverage level
	# $options->{'FEATURE'}->{$_}	=> number of bases
	foreach (keys %{$options->{'FEATURE'}}) {
		$sum	+= $_ * $options->{'FEATURE'}->{$_};
		$count	+= $options->{'FEATURE'}->{$_};
	}

	my $ave		= $sum / $count;
	$ave		= int($ave + 0.5);

	return($ave);
}

################################################################################
=pod

B<ave_coverage()>

 Find the "average" level of coverage for all features of a type

Parameters:
 COVERAGE  -> hash with coverage stats for a single feature
             (key = coverage level, value = number of bases)

Returns:
 scalar int -> "average" coverage

=cut

sub ave_coverage {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $sum		= 0;
	my $count	= 0;

	# $_				=> coverage level
	# $options->{'COVERAGE'}->{$_}	=> number of bases
	foreach (keys %{$options->{'COVERAGE'}}) {
		# skip non-covered areas?
		next if($_ == 0);

		$sum	+= $_ * $options->{'COVERAGE'}->{$_};
		$count	+= $options->{'COVERAGE'}->{$_};
	}

	my $ave		= $sum / $count;
	$ave		= int($ave + 0.5);

	return($ave);
}

################################################################################
=pod

B<find_bad_baits()>

 Find baits with no coverage

Parameters:

 FEATURES - ref to hash of hash with all features and their coverage levels
 
Returns:

=cut

sub find_bad_baits {
	my $self	= shift @_;

        my $this_sub	= (caller(0))[0]."::".(caller(0))[3];

        # parse params
        my $options	= {};
        for(my $i=0; $i<=$#_; $i+=2) {
                defined($_[($i + 1)]) || 
                        die print STDERR "Odd number of params: $this_sub : $!";
                $options->{uc($_[$i])} = $_[($i + 1)];
        }

	my $features = $options->{'FEATURES'};

	#print Dumper $features;

	# foreach feature
	foreach my $t (keys %{$features}) {

        	unless(	scalar(keys %{$features->{$t}} ) == 1 &&
			$features->{$t}->{0}) {
				delete($features->{$t});
		}
	}

	return($features);
}

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
