package QCMG::Picard::MarkDupMetrics;

##############################################################################
#
#  Module:   QCMG::Picard::MarkDupMetrics.pm
#  Creator:  John V Pearson
#  Created:  2011-02-25
#
#  Parses the metrics file (METRICS=) created by Picard MarkDuplicates.
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
    
    die "QCMG::Picard::MarkDupMetrics->new() requires a file parameter",
        unless (exists $params{file} and $params{file});

    my $self = { file                  => $params{file},
                 cmdline               => {},
                 start_time            => '',
                 metrics               => {},
                 histogram             => [],
                 verbose               => $params{verbose} || 0 };
    bless $self, $class;

    $self->_process_file();
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub cmdline {
    my $self = shift;
    return $self->{cmdline};
}


sub cmdline_string {
    my $self = shift;
    return $self->{cmdline}->{string};
}


sub cmdline_params {
    my $self = shift;
    return $self->{cmdline}->{params};
}


sub start {
    my $self = shift;
    return $self->{start};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


sub metrics {
    my $self = shift;
    return $self->{metrics};
}

# The MarkDuplicates metrics file has a somewhat painful layout so the
# simplest parsing approach (although somewhat unsatisfying) is probably
# to read the whole thing into memory as a single string  and then just
# do regexes into the whole file to pull out bits and pieces.

sub _process_file {
    my $self = shift;

    my $infh = IO::File->new( $self->file, 'r' );
    die 'Cannot open MarkDup metrics file ', $self->file, " for reading: $!"
        unless defined $infh;

    my $pattern = '';
    my $contents = '';
    while (my $line = $infh->getline) {
        $contents .= $line;
    }

    # 1. Parse out the 'HISTOGRAM' matrix:
    $pattern = '^## HISTOGRAM.*?\n\n$';
    if ($contents =~ /($pattern)/sm) {
        $self->{histogram} = _parse_histogram( $1 );
        # Zero out the block
        $contents =~ s/$pattern//sm;
    }
    else {
        die 'Picard MarkDup metrics file does not appear to have a ' .
            "'HISTOGRAM' section.";
    }

    # 2. Parse out the 'METRICS CLASS' line:
    $pattern = '^## METRICS CLASS.*?\n\n$';
    if ($contents =~ /($pattern)/sm) {
        $self->{metrics} = _parse_metrics_class( $1 );
        # Zero out the block
        $contents =~ s/$pattern//sm;
    }
    else {
        die 'Picard MarkDup metrics file does not appear to have a ' .
            "'HISTOGRAM' section.";
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
        if ($line =~ /^# net.sf.picard.sam.MarkDuplicates (.*)$/i) {
            $self->{cmdline}->{string} = $1;
            $self->{cmdline}->{params} =
                _parse_cmdline( $self->{cmdline}->{string} );
        }
        elsif ($line =~ /^# Started on: (.*)$/i) {
            $self->{start_time} = $1;
        }
        elsif ($line =~ /^## net.sf.picard.metrics.StringHeader$/i) {
            # Do nothing;
        }
        else {
            warn "QCMG::Picard::MarkDupMetrics - unparsed line: [$line]\n";
        }
    }

    return $self;
}


sub _parse_histogram {
    my $text = shift;

    my @lines = split /\n/, $text;

    my $coverage = 0;
    my $read_length = 0;
    my @records = ();
    my @headers = ();
    foreach my $line (@lines) {
        chomp $line;
        if ($line =~ /^##/) {
            # Do nothing
        }
        elsif ($line =~ /^BIN/) {
            push @headers, split(/\s+/,$line);
        }
        elsif ($line =~ /^[\d\s\.]+/) {
            my ($bin, $value) = split /\s+/, $line, 2;
            push @records, [ $bin, $value ];
        }
        else {
            die "Couldn't pattern match from HISTOGRAM [$line]";
        }
    }

    return { 'headers' => \@headers,
             'values'  => \@records };
}


sub _parse_metrics_class {
    my $text = shift;

    my @lines = split /\n/, $text;

    my %metrics = ();
    if ($lines[0] =~ /^## METRICS CLASS\s+([\w\.]+)/) {
        $metrics{class} = $1;
    }
    my @keys = split /\t/, $lines[1];
    my @vals = split /\t/, $lines[2];

    die "The key and value counts don't match when parsing METRICS CLASS"
        unless ($#keys == $#vals);

    foreach my $i (0..$#keys) {
        #print join("\t", $i, $keys[$i], $vals[$i]) ,"\n";
        $metrics{ $keys[$i] } = $vals[$i];
    }

    return \%metrics;
}


sub _parse_cmdline {
    my $text = shift;

    my @fields = split /\s+/, $text;

    my %params = map { /(\w+)=(.*)/; $1 => $2 }
                 @fields;

    return \%params;
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

    $xml .= '<MarkDupMetrics file="' . $self->file . '"' . ">\n";
    $xml .= _element( 'startTime', $self->{'start_time'} );

    $xml .= "<cmdLine>\n";
    $xml .= _element( 'string', $self->{cmdline}->{string} );
    $xml .= "<params>\n";
    foreach my $key (keys %{ $self->{cmdline}->{params} }) {
        $xml .= _element( $key, $self->{cmdline}->{params}->{$key} );
    }
    $xml .= "</params>\n";
    $xml .= "</cmdLine>\n";

    $xml .= "<metrics>\n";
    $xml .= _element( 'class',
                      $self->{metrics}->{'class'} );
    $xml .= _element( 'library',
                      $self->{metrics}->{'LIBRARY'} );
    $xml .= _element( 'unmappedReads',
                      $self->{metrics}->{'UNMAPPED_READS'} );
    $xml .= _element( 'readPairsExamined',
                      $self->{metrics}->{'READ_PAIRS_EXAMINED'} );
    $xml .= _element( 'unpairedReadsExamined',
                      $self->{metrics}->{'UNPAIRED_READS_EXAMINED'} );
    $xml .= _element( 'unpairedReadDuplicates',
                      $self->{metrics}->{'UNPAIRED_READ_DUPLICATES'} );
    $xml .= _element( 'readPairDuplicates',
                      $self->{metrics}->{'READ_PAIR_DUPLICATES'} );
    $xml .= _element( 'readPairOpticalDuplicates',
                      $self->{metrics}->{'READ_PAIR_OPTICAL_DUPLICATES'} );
    $xml .= _element( 'percentDuplication',
                      $self->{metrics}->{'PERCENT_DUPLICATION'} );
    $xml .= _element( 'estimatedLibrarySize',
                      $self->{metrics}->{'ESTIMATED_LIBRARY_SIZE'} );
    $xml .= "</metrics>\n";

    $xml .= _matrix_as_xml( 'histogram',
                            $self->{histogram} );
    
    $xml .= "</MarkDupMetrics>\n";

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
    my @rows = @{ $rh_matrix->{values} };
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

QCMG::Picard::MarkDupMetrics - Perl module for parsing Picard MarkDuplicates metrics file


=head1 SYNOPSIS

 use QCMG::Picard::MarkDupMetrics;


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
