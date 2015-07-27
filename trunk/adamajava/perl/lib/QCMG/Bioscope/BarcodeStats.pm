package QCMG::Bioscope::BarcodeStats;

##############################################################################
#
#  Module:   QCMG::Bioscope::BarcodeStats.pm
#  Creator:  John V Pearson
#  Created:  2010-09-09
#
#  Parses the BarcodeStatistisc.*.txt files created by Bioscope in the
#  libraries directory for each barcode run.
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
    
    die "QCMG::Bioscope::BarcodeStats->new() requires a file parameter",
        unless (exists $params{file} and $params{file});

    my $self = { file                  => $params{file},
                 info                  => {},
                 barcode_counts        => {},
                 barcode_totals        => {},
                 verbose               => $params{verbose} || 0 };
    bless $self, $class;

    $self->_process_file();
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub barcode_counts {
    my $self = shift;
    return $self->{barcode_counts};
}


sub info {
    my $self = shift;
    return $self->{info};
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


# The BarcodeStats.txt file has a very simple structure. 

sub _process_file {
    my $self = shift;

    my $infh = IO::File->new( $self->file, 'r' );
    die 'Cannot open main log file ', $self->file, " for reading: $!"
        unless defined $infh;


    my $contents = '';
    while (my $line = $infh->getline) {
        $contents .= $line;
    }

    # 1. Parse out the 'Library ...' matrix:
    if ($contents =~ /^##(Library.*?\n^All Beads.*?)$/sm) {
        my $report = $1;
        my @lines = split /\n/, $report;

        # Process column labels
        my $column_labels = shift @lines;
        chomp $column_labels;
        my @labels = split /\t/, $column_labels;
        $self->{barcode_counts}->{headers} = \@labels;
        $self->{barcode_totals}->{headers} = \@labels;
    
        # Process matrix
        foreach my $line (@lines) {
            chomp $line;
            my @fields = split /\t/, $line;
            # Subtotals go to a different place
            if ($fields[1] =~ /Subtotals/i) {
                push @{ $self->{barcode_totals}->{data} }, \@fields;
            }
            elsif ($fields[0] =~ /All Beads/i) {
                push @{ $self->{barcode_totals}->{data} }, \@fields;
            }
            else {
                push @{ $self->{barcode_counts}->{data} }, \@fields;
            }
        }

        # Zero out the block
        $contents =~ s/##$report//sm;
    }
    else {
        die 'BarcodeStatisticss.txt file does not appear to have a ' .
            "'Library' section.";
    }

    
    # 2. Parse out the odds-and-ends.  This is not particularly clever
    # but we'll just roll through each line throwing it against all the
    # regexes until one sticks and if none do then we complain about the
    # line.  This should give us some limited future-proofing in that if
    # a line changes, at least the line will fail the regex checks and
    # will be output so we know to edit this module.
    my @lines = split /\n/, $contents;
    foreach my $line (@lines) {
        $line =~ s/\s+$//;   # trim trailing spaces
        next unless $line;   # skip empty lines
        if ($line =~ /^#\?\s*missing-barcode-reads=(.*)$/i) {
            $self->{info}->{missing_barcode_reads} = $1;
        }
        elsif ($line =~ /^#\?\s*missing-F3-reads=(.*)$/i) {
            $self->{info}->{missing_F3_reads} = $1;
        }
        else {
            warn "QCMG::Bioscope::BarcodeStats - unparsed line: [$line]\n";
        }
    }

    return $self;
}


# The mapping-stats.txt file has 2 large matrices which are extremely
# similar in layout so this non-OO routine can be used to parse them
# both.


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

    $xml .= '<BarcodeStatsReport file="' . $self->file . '"' . ">\n";

    $xml .= "<BarcodeInfo>\n";
    $xml .= _element( 'MissingBarcodeReads',
                      $self->{info}->{'missing_barcode_reads'} );
    $xml .= _element( 'MissingF3Reads',
                      $self->{info}->{'missing_F3_reads'} );
    $xml .= "</BarcodeInfo>\n";
    
    $xml .= _matrix_as_xml( 'BarcodeCounts',
                            $self->{barcode_counts} );

    $xml .= _matrix_as_xml( 'BarcodeTotals',
                            $self->{barcode_totals} );

    $xml .= "</BarcodeStatsReport>\n";

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



1;
__END__


=head1 NAME

QCMG::Bioscope::BarcodeStats - Perl module for parsing BarcodeStatistics.*.txt files


=head1 SYNOPSIS

 use QCMG::Bioscope::BarcodeStats;


=head1 DESCRIPTION

This module provides an interface for parsing Bioscope 
BarcodeStatistics.*.txt log files.
This module will not usually be directly invoked by the user.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $bc = QCMG::Bioscope::BarcodeStats->new(
                file    => 'BarcodeStatistics.20100820223516376.txt',
                verbose => 0 );
 print $bc->as_xml();

=item B<as_xml()>

 $ps->as_xml();

Returns the contents of the parsed BarcodeStatistics.*.txt file as XML.
Note that this does not include a document type line.

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
