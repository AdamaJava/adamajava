package QCMG::Bioscope::MappingStats;

##############################################################################
#
#  Module:   QCMG::Bioscope::MappingStats.pm
#  Creator:  John V Pearson
#  Created:  2010-08-11
#
#  Parses the mapping-stats.txt files created by Bioscope along with
#  each .ma file -  in the s_mapping directory for fragment runs and the
#  F3/s_mapping and R3/s_mapping directories for LMP runs.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use IO::File;
use Data::Dumper;
use Carp qw( carp croak cluck confess );
use File::Find;
use POSIX;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;
    
    die "QCMG::Bioscope::MappingStats->new() requires a file parameter",
        unless (exists $params{file} and $params{file});

    my $self = { file                  => $params{file},
                 info                  => {},
                 mapped_reads          => {},
                 uniquely_placed_reads => {},
                 verbose               => $params{verbose} || 0 };
    bless $self, $class;

    $self->_process_file();
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub mapped_reads {
    my $self = shift;
    return $self->{mapped_reads};
}


sub uniquely_placed_reads {
    my $self = shift;
    return $self->{uniquely_placed_reads};
}


sub info {
    my $self = shift;
    return $self->{info};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub status {
    my $self = shift;
    return $self->{status} = shift if @_;
    return $self->{status};
}

# The mapping-stats.txt file has a somewhat painful layout so the
# simplest parsing approach (although somewhat unsatisfying) is probably
# to read the whole thing into memory as a single string  and then just
# do regexes into the whole file to pull out bits and pieces.

sub _process_file {
    my $self = shift;

    my $infh = IO::File->new( $self->file, 'r' );
    die 'Cannot open mapping stats file ', $self->file, " for reading: $!"
        unless defined $infh;


    my $contents = '';
    while (my $line = $infh->getline) {
        $contents .= $line;
    }

    # 1. Parse out the 'Uniquely Placed Reads' matrix:
    if ($contents =~ /(^Uniquely Placed Reads.*?Megabases of coverage [\d\.]+$)/sm) {
        my ($ra_matrix, $coverage) = _parse_reads_matrix( $1 );
        $self->{uniquely_placed_reads} = { 
            headers => [ 'Read length', 'Mismatches',
                         'Read count', 'Read Percent',
                         'Cumulative Count', 'Cumulative Percent' ],
            data    => $ra_matrix };
        $self->{info}->{unique_reads_megabases} = $coverage;
        # Zero out the block
        $contents =~ s/^Uniquely Placed Reads.*?Megabases of coverage [\d\.]+$//sm;
    }
    else {
        die 'mapping-stats.txt file does not appear to have a ' .
            "'Uniquely Placed Reads' section.";
    }
    
    # 2. Parse out the 'Mapped Reads' matrix:
    if ($contents =~ /(^Mapped Reads.*?Megabases of coverage [\d\.]+$)/sm) {
        my ($ra_matrix, $coverage) = _parse_reads_matrix( $1 );
        $self->{mapped_reads} = { 
            headers => [ 'Read length', 'Mismatches',
                         'Read count', 'Read Percent',
                         'Cumulative Count', 'Cumulative Percent' ],
            data    => $ra_matrix };
        $self->{info}->{mapped_reads_megabases} = $coverage;
        # Zero out the block
        $contents =~ s/^Mapped Reads.*?Megabases of coverage [\d\.]+$//sm;
    }
    else {
        die 'mapping-stats.txt file does not appear to have a ' .
            "'Mapped Reads' section.";
    }
    
    # 3. Parse out the odds-and-ends.  This is not particulartly clever
    # but we'll just roll through each line throwing it against all the
    # regexes until one sticks and if none do then we complain about the
    # line.  This should give us some limited future-proofing in that if
    # a line changes, at least the line will fail the regex checks and
    # will be output so we know to edit this module.
    my @lines = split /\n/, $contents;
    foreach my $line (@lines) {
        $line =~ s/\s+$//;   # trim trailing spaces
        next unless $line;   # skip empty lines
        if ($line =~ /^#Bioscope version: (.*)$/i) {
            $self->{info}->{bioscope_version} = $1;
        }
        elsif ($line =~ /^input ma file:(.*)$/i) {
            $self->{info}->{input_ma_file} = $1;
        }
        elsif ($line =~ /^([\d,]+) total tags found$/i) {
            $self->{info}->{total_tags} = _cleanup_numeric_value($1);
        }
        elsif ($line =~ /^Reference sequence:(.*), ([\d,]+) bp$/i) {
            $self->{info}->{reference_sequence} = $1;
            $self->{info}->{reference_sequence_length} =
                 _cleanup_numeric_value($2);
        }
        elsif ($line =~ /^\s+mismatch penalty = ([-\d\.]+)$/i) {
            $self->{info}->{mismatch_penalty} = $1;
        }
        elsif ($line =~ /^\s+clearzone = ([-\d\.]+)$/i) {
            $self->{info}->{clearzone} = $1;
        }
        elsif ($line =~ /^Mapping Statistics -- (.*)$/i) {
            $self->{info}->{mapping_method} = $1;
        }
        elsif ($line =~ /^([\d,]+) Mapped reads \(([\d\.]+)%\).*$/i) {
            $self->{info}->{mapped_reads} = _cleanup_numeric_value($1);
            $self->{info}->{mapped_reads_percentage} = $2;
        }
        elsif ($line =~ /^\s+Number\ of\ Starting\ Points\ in\ 
                          Uniquely\ placed\ tag\ ([\d,]+)\ 
                          \(([\d\.]+)\%\ of\ reference\)$/ix) {
            $self->{info}->{start_points_from_unique_reads} =
                 _cleanup_numeric_value($1);
            $self->{info}->{start_points_from_unique_reads_ref_pcent} = $2;
        }
        elsif ($line =~ /^\s+Average\ Number(?:.*)Start\ Point\s+([\d\.]+)$/ix) {
            #$self->{info}->{average_unique_reads_per_start_point} = $1;
        }
        elsif ($line =~ /^\s+Estimate\ Number\ of\ starting\ point\  
                          for\ all\ mapped\ tags\ ([\d,]+)\ 
                          \(([\d\.]+)\%\ of\ reference\)$/ix) {
            $self->{info}->{start_points_from_mapped_reads} =
                 _cleanup_numeric_value($1);
            $self->{info}->{start_points_from_mapped_reads_ref_pcent} = $2;
        }
        elsif ($line =~ /^Starting Points within Placed Tags$/i) {
            # do nothing!
        }
        else {
            warn "QCMG::Bioscope::MappingStats - unparsed line: [$line]\n";
        }
    }

    return $self;
}


# The mapping-stats.txt file has 2 large matrices which are extremely
# similar in layout so this non-OO routine can be used to parse them
# both.

sub _parse_reads_matrix {
    my $text = shift;

    my @lines = split /\n/, $text;

    my $coverage = 0;
    my $read_length = 0;
    my @records = ();
    foreach my $line (@lines) {
        chomp $line;
        if ($line =~ /^Uniquely/) {
            # Do nothing
        }
        elsif ($line =~ /^Mapped/) {
            # Do nothing
        }
        elsif ($line =~ /^\s*Read Length\s+(\d+)$/) {
            $read_length = $1;
        }
        elsif ($line =~ /^\s*Megabases of coverage\s+([\d\.]+)\s*$/) {
            $coverage = $1;
        }
        else {
            # These lines are best torn apart by substr or regex rather
            # than split because they are vertically lined up using
            # spaces as padding.  There are either 2 or 3 sets of
            # count/percentage paired columns and they should be
            # treated differently - for the 3 set version, we are
            # ditching the middle column pair.
            if ($line =~ /^\s* (\d+) \s mismatches \s+
                          ([\d,]+) \s+ \(([\d\.\s]+)%\) \s*$/x ) {
                push @records,
                     _cleanup_numeric_values(
                            $read_length, $1, $2, $3, $2, $3 );
            }
            elsif ($line =~ /^\s* (\d+) \s mismatches \s+
                             ([\d,]+) \s+ \(([\d\.\s]+)%\) \s*
                             ([\d,]+) \s+ \(([\d\.\s]+)%\) \s*$/x ) {
                push @records,
                     _cleanup_numeric_values(
                            $read_length, $1, $2, $3, $4, $5 );
            }
            elsif ($line =~ /^\s* (\d+) \s mismatches \s+
                             ([\d,]+) \s+ \(([\d\.\s]+)%\) \s*
                             ([\d,]+) \s+ \(([\d\.\s]+)%\) \s*
                             ([\d,]+) \s+ \(([\d\.\s]+)%\) \s*$/x ) {
                push @records,
                     _cleanup_numeric_values(
                            $read_length, $1, $2, $3, $6, $7 );
             }
            else {
                warn "Couldn't pattern match [$line]";
            }
        }
    }

    return \@records, $coverage;
}


sub _cleanup_numeric_values {
    my @input = @_;

    my @output = ();
    foreach my $value (@input) {
        push @output, _cleanup_numeric_value($value);
    }

    return \@output;
}


sub _cleanup_numeric_value {
    my $input = shift;

    #print "value-in: [$input]\n";
    $input =~ s/[\s,]+//g;
    #print "value-out: [$input]\n";

    return $input;
}



sub _element {
    my $elem = shift;
    my $content = shift;
    return '<'.$elem.'>'.$content.'</'.$elem.">\n";
}


sub as_xml {
    my $self = shift;
    my $xml = '';

    $xml .= '<MappingStatsReport file="' . $self->file . '"' . ">\n";

    $xml .= "<MappingInfo>\n";
    $xml .= _element( 'BioscopeVersion',
                      $self->{info}->{'bioscope_version'} );
    $xml .= _element( 'MappingMethod',
                      $self->{info}->{'mapping_method'} );
    $xml .= _element( 'InputMaFile',
                      $self->{info}->{'input_ma_file'} );
    $xml .= _element( 'TotalTags',
                      $self->{info}->{'total_tags'} );
    $xml .= _element( 'MismatchPenalty',
                      $self->{info}->{'mismatch_penalty'} );
    $xml .= _element( 'Clearzone',
                      $self->{info}->{'clearzone'} );

    $xml .= _element( 'ReferenceSequence',
                      $self->{info}->{'reference_sequence'} );
    $xml .= _element( 'ReferenceSequenceLength',
                      $self->{info}->{'reference_sequence_length'} );

    $xml .= _element( 'MappedReads',
                      $self->{info}->{'mapped_reads'} );
    $xml .= _element( 'MappedReadsPercentage',
                      $self->{info}->{'mapped_reads_percentage'} );
    $xml .= _element( 'MappedReadsMegabases',
                      $self->{info}->{'mapped_reads_megabases'} );
    $xml .= _element( 'UniqueReadsMegabases',
                      $self->{info}->{'unique_reads_megabases'} );

    $xml .= _element( 'StartPointsFromMappedReads',
                      $self->{info}->{'start_points_from_mapped_reads'} );
    $xml .= _element( 'StartPointsFromMappedReadsReferencePercent',
                      $self->{info}->{'start_points_from_mapped_reads_ref_pcent'} );
    $xml .= _element( 'StartPointsFromUniqueReads',
                      $self->{info}->{'start_points_from_unique_reads'} );
    $xml .= _element( 'StartPointsFromUniqueReadsReferencePercent',
                      $self->{info}->{'start_points_from_unique_reads_ref_pcent'} );
    $xml .= "</MappingInfo>\n";

    $xml .= _matrix_as_xml( 'MappedReads',
                            $self->{mapped_reads} );
    
    $xml .= _matrix_as_xml( 'UniquelyPlacedReads',
                            $self->{uniquely_placed_reads} );
    
    $xml .= "</MappingStatsReport>\n";

    return $xml;
}


sub _matrix_as_xml {
    my $name      = shift;
    my $rh_matrix = shift;

    my $xml .= '<Table name="'. $name . "\">\n";
    my @headers = @{ $rh_matrix->{headers} };
    $xml .= '<Headers>';
    $xml .= "<Header>$_</Header>" foreach @headers;
    $xml .= "</Headers>\n";

    $xml .= "<Data>\n";
    my @rows = @{ $rh_matrix->{data} };
    foreach my $ra_data (@rows) {
        $xml .= "<Row>";
        $xml .= "<Cell>$_</Cell>" foreach @{ $ra_data };
        $xml .= "</Row>\n";
    }
    $xml .= "</Data>\n";
    $xml .= "</Table>\n";

    return $xml;
}


sub uniquely_placed_reads_as_text {
    my $self = shift;

    my $text = '';
    foreach my $ra_record (@{ $self->{uniquely_placed_reads} }) {
        $text .= $ra_record->[0] .
                 sprintf("%8u",     $ra_record->[1]) .
                 sprintf("%13u",    $ra_record->[2]) .
                 sprintf("%8.2f%%", $ra_record->[3]) .
                 sprintf("%13u",    $ra_record->[4]) .
                 sprintf("%8.2f%%", $ra_record->[5]) . "\n";
    }
    return $text;
}



1;
__END__


=head1 NAME

QCMG::Bioscope::MappingStats - Perl module for parsing Bioscope mapping-stats.txt files


=head1 SYNOPSIS

 use QCMG::Bioscope::MappingStats;


=head1 DESCRIPTION

This module provides an interface for parsing Bioscope mapping-stats.txt
log files.
This module will not usually be directly invoked by the user.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $ms = QCMG::Bioscope::MappingStats->new(
                file    =>
                'S00414_20100804_1_frag/F3/s_mapping/mapping-stats.txt',
                verbose => 0 );
 print $ms->as_xml();

=item B<as_xml()>

 $ps->as_xml();

Returns the contents of the MappingStats module as XML.  Note that this
does not include a document type line.

=item B<info()>

 $blm->info('unique_reads_megabases');

This method is a collective accessor for a large number of individual
pieces of information parsed out of the mapping-stats.txt file.  The
full list of accessor strings and the contents is shown below.

For matepair and paired-end runs, the assignment of duplicates cannot
be done without considering the mates so their are a number of items in
the list below (inlcuding unique_reads_megabases) that may not be
meaningfull if you are running this parsing module against
mapping-stats.txt files from paired-read F3, R3 or F5 files. 

=over

=item unique_reads_megabases

Megabases of coverage derived from uniquely placed reads.

=item mapped_reads_megabases

Megabases of coverage from mapped reads.  Note that this includes reads
that were uniquely mapped as well as duplicates.

=item bioscope_version

Version of bioscope that was used to create the mapping-stats.txt file.

=item start_points_from_unique_reads

Starting points in uniquely placed tags.

=item reference_start_points_from_unique_reads

The number of start points from uniquely placed tags expressed as a
percentage of the bases in the reference.

=item B<verbose()>

 $blm->verbose();

Returns the verbose status for this object where 0 sets verbose off 
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id$


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
