package QCMG::Bioscope::EnrichmentReport;

##############################################################################
#
#  Module:   QCMG::Bioscope::EnrichmentReport.pm
#  Creator:  John V Pearson
#  Created:  2010-11-25
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
    
    die "QCMG::Bioscope::PairingStats->new() requires a file parameter",
        unless (exists $params{file} and $params{file});

    my $self = { file                  => $params{file},
                 info                  => {},
                 pairing_classes       => {},
                 pairing_class_defs    => {},
                 pairing_stats         => {},
                 mismatch_report       => {},
                 verbose               => $params{verbose} || 0 };
    bless $self, $class;

    $self->_process_file();
}


sub file {
    my $self = shift;
    return $self->{file};
}


sub pairing_classes {
    my $self = shift;
    return $self->{pairing_classes};
}


sub pairing_class_defs {
    my $self = shift;
    return $self->{pairing_class_defs};
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

# Like the mapping-stats.txt file, the pairingStats.txt file has a 
# somewhat painful layout so the
# simplest parsing approach (although somewhat unsatisfying) is probably
# to read the whole thing into memory as a single string  and then just
# do regexes into the whole file to pull out bits and pieces.

sub _process_file {
    my $self = shift;

    my $infh = IO::File->new( $self->file, 'r' );
    die 'Cannot open pairing stats file ', $self->file, " for reading: $!"
        unless defined $infh;


    my $contents = '';
    while (my $line = $infh->getline) {
        $contents .= $line;
    }

    # 1. Parse out the 'Pairing classifications ... ' matrix:
    if ($contents =~ /^Pairing classifications among uniquely placed beads\n(.*?\n)\n/sm) {
        my @lines = split /\n/, $1;

        # Process column labels
        my $column_labels = shift @lines;
        chomp $column_labels;
        my @labels = split /\t/, $column_labels;
        $self->{pairing_classes}->{headers} = \@labels;
    
        # Process matrix
        foreach my $line (@lines) {
            chomp $line;
            my @fields = split /\t/, $line;
            push @{ $self->{pairing_classes}->{data} }, \@fields;
        }

        # Zero out the block
        $contents =~ s/^Pairing classifications among uniquely placed beads.*?\n\n//sm;
    }
    else {
        die 'pairingStats.txt file does not appear to have a ' .
            "'Pairing classifications' section.";
    }

    
    # 2. Parse out the 'Classification Definitions' matrix:
    if ($contents =~ /^Classification Definitions\n(.*?\n)\n/sm) {
        my @lines = split /\n/, $1;

        # Process column labels
        my $column_labels = shift @lines;
        chomp $column_labels;
        my @labels = split /\s+/, $column_labels;
        $self->{pairing_class_defs}->{headers} = \@labels;
    
        # Process matrix
        foreach my $line (@lines) {
            chomp $line;
            next if $line =~ /^\-/;  # skip --- lines
            my @fields = split /\s{2,}/, $line;
            push @{ $self->{pairing_class_defs}->{data} }, \@fields;
        }

        # Zero out the block
        $contents =~ s/^Classification Definitions.*?\n\n//sm;
    }
    else {
        die 'pairingStats.txt file does not appear to have a ' .
            "'Classification Definitions' section.";
    }

    # 3. Parse out the first 'Mismatch report' matrix:
    if ($contents =~ /^(Mismatch report for.*?\n)\n/sm) {
        $self->_parse_mismatch_report( $1 );
        $contents =~ s/Mismatch report for.*?\n\n//sm;
    }
    else {
        die 'pairingStats.txt file does not appear to have any ' .
            "'Mismatch report' sections.";
    }


    # 4. Parse out the second 'Mismatch report' matrix:
    if ($contents =~ /(^Mismatch report for.*?\n)\n/sm) {
        $self->_parse_mismatch_report( $1 );
        $contents =~ s/Mismatch report for.*?\n\n//sm;
    }
    else {
        die 'pairingStats.txt file does not appear to have a second ' .
            "'Mismatch report' sections.";
    }

    
    # 5. Parse out the 'uniqely placed beads' explanatory text block:
    if ($contents =~ /^(A bead is uniquely.*?than the best one.)$/sm) {
        $self->{info}->{placed_beads_explanation} = $1;
        $self->{info}->{placed_beads_explanation} =~ s/\n/ /g;
        # Zero out the block
        $contents =~ s/^A bead is uniquely.*?than the best one.$//sm;
    }
    
    
    # 6. Parse out the 'Pairing Statistics' matrix:
    if ($contents =~ /^Pairing Statistics\n\n(.*?\n)\n/sm) {
        my @lines = split /\n/, $1;

        # Parse headers
        my $headers = shift @lines;
        chomp $headers;
        my @headers = split /\s+/, $headers;
        $self->{pairing_stats}->{headers} = \@headers;

        # Parse data
        foreach my $line (@lines) {
           chomp $line;
           my @fields = split /\s+/, $line;
           push @{ $self->{pairing_stats}->{data} }, \@fields;
        }

        # Zero out the block
        $contents =~ s/^Pairing Statistics\n\n.*?\n\n//sm;
    }
    else {
        die 'pairingStats.txt file does not appear to have a ' .
            "'Pairing Statistics' section.";
    }

    
    # 7. Parse out the odds-and-ends.  This is not particulartly clever
    # but we'll just roll through each line throwing it against all the
    # regexes until one sticks and if none do then we complain about the
    # line.  This should give us some limited future-proofing in that if
    # a line changes, at least the line will fail the regex checks and
    # will be output so we know to edit this module.
    my @lines = split /\n/, $contents;
    foreach my $line (@lines) {
        $line =~ s/\s+$//;   # trim trailing spaces
        next unless $line;   # skip empty lines
        if ($line =~ /^#\s*Bioscope version: (.*)$/i) {
            $self->{info}->{bioscope_version} = $1;
        }
        elsif ($line =~ /^Insert range (\d+)-(\d+)$/i) {
            $self->{info}->{insert_size_min} = $1;
            $self->{info}->{insert_size_max} = $2;
        }
        elsif ($line =~ /^Total beads:\s+([\d,]+)$/i) {
            $self->{info}->{total_beads} = _cleanup_numeric_value($1);
        }
        elsif ($line =~ /^Total beads placed:\s+([\d,]+)$/i) {
            $self->{info}->{total_beads_placed} = _cleanup_numeric_value($1);
        }
        elsif ($line =~ /^Total normal \(AAA\) mates:\s+([\d,]+)$/i) {
            $self->{info}->{total_AAA_mates} = _cleanup_numeric_value($1);
        }
        elsif ($line =~ /^Total non-redundant mates:\s+([\d,]+)$/i) {
            $self->{info}->{total_non_redundant_mates} = _cleanup_numeric_value($1);
        }
        elsif ($line =~ /^Total normal \(AAA\) non-redundant mates:\s+([\d,]+)$/i) {
            $self->{info}->{total_AAA_non_redundant_mates} = _cleanup_numeric_value($1);
        }
        elsif ($line =~ /^Beads uniquely placed:\s+([\d,]+)$/i) {
            $self->{info}->{beads_uniquely_placed} = _cleanup_numeric_value($1);
        }
        elsif ($line =~ /^Total bases in paired reads:\s+([\d,]+)$/i) {
            $self->{info}->{total_bases_in_paired_reads} = _cleanup_numeric_value($1);
        }
        elsif ($line =~ /^Pairing Report$/i) {
            # Do nothing !
        }
        else {
            warn "QCMG::Bioscope::PairingStats - unparsed line: [$line]\n";
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


sub _parse_mismatch_report {
    my $self = shift;
    my $text = shift;

    my @lines = split /\n/, $text;
    my $label = '';

    # Process header line
    my $header = shift @lines;
    if ($header =~ /^Mismatch report for ([\w\d-]+)$/sm) {
        $label = $1;
    }
    else {
        die "Couldn't parse Mismatch report header line [$header]";
    }

    # Process column labels
    my $column_labels = shift @lines;
    chomp $column_labels;
    my @labels = split /\t/, $column_labels;
    $self->{mismatch_report}->{$label}->{headers} = \@labels;
    
    # Process matrix
    foreach my $line (@lines) {
       chomp $line;
       my @fields = split /\s+/, $line;
       $fields[2] = _cleanup_numeric_value( $fields[2] );
       $fields[4] = _cleanup_numeric_value( $fields[4] );
       push @{ $self->{mismatch_report}->{$label}->{data} }, \@fields;
    }
}


sub _element {
    my $elem = shift;
    my $content = shift;
    return '<'.$elem.'>'.$content.'</'.$elem.">\n";
}


sub as_xml {
    my $self = shift;
    my $xml = '';

    $xml .= '<PairingStatsReport file="' . $self->file . '"' . ">\n";

    $xml .= "<PairingInfo>\n";
    $xml .= _element( 'BioscopeVersion',
                      $self->{info}->{'bioscope_version'} );
    $xml .= _element( 'InsertSizeMinimum',
                      $self->{info}->{'insert_size_min'} );
    $xml .= _element( 'InsertSizeMaximum',
                      $self->{info}->{'insert_size_max'} );
    $xml .= _element( 'TotalBeads',
                      $self->{info}->{'total_beads'} );
    $xml .= _element( 'TotalBeadsPlaced',
                      $self->{info}->{'total_beads_placed'} );
    $xml .= _element( 'TotalAAAMates',
                      $self->{info}->{'total_AAA_mates'} );
    $xml .= _element( 'TotalNonRedundantMates',
                      $self->{info}->{'total_non_redundant_mates'} );
    $xml .= _element( 'TotalAAANonRedundantMates',
                      $self->{info}->{'total_AAA_non_redundant_mates'} );
    $xml .= _element( 'BeadsUniquelyPlaced',
                      $self->{info}->{'beads_uniquely_placed'} );
    $xml .= _element( 'TotalBasesInPairedReads',
                      $self->{info}->{'total_bases_in_paired_reads'} );
    $xml .= "</PairingInfo>\n";
    
    $xml .= _matrix_as_xml( 'PairingClassifications',
                            $self->{pairing_classes} );

    $xml .= _matrix_as_xml( 'ClassificationDefinitions',
                            $self->{pairing_class_defs} );

    $xml .= _matrix_as_xml( 'PairingStatistics', 
                            $self->{pairing_stats} );

    foreach my $label (keys %{ $self->{mismatch_report} }) {
        $xml .= _matrix_as_xml( 'MismatchReport' . $label,
                                $self->{mismatch_report}->{$label} );
    }

    $xml .= "</PairingStatsReport>\n";

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

QCMG::Bioscope::PairingStats - Perl module for parsing Bioscope pairingStats.txt files


=head1 SYNOPSIS

 use QCMG::Bioscope::PairingStats;


=head1 DESCRIPTION

This module provides an interface for parsing Bioscope pairingStats.txt
log files.
This module will not usually be directly invoked by the user.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $ps = QCMG::Bioscope::PairingStats->new(
                file    => 'S00414_20100804_1_frag/pairing/pairingStats.txt',
                verbose => 0 );
 print $ps->as_xml();

=item B<as_xml()>

 $ps->as_xml();

Returns the contents of the PairingStats module as XML.  Note that this
does not include a document type line.

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
