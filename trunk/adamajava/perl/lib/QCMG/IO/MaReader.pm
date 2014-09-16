package QCMG::IO::MaReader;

###########################################################################
#
#  Module:   QCMG::IO::MaReader.pm
#  Creator:  John V Pearson
#  Created:  2010-11-03
#
#  Reads a .ma matches file as created by Bioscope. A .ma file is
#  basically a multi-FASTA file with some extra information on the
#  defline.  Note that this module assumes that every record has exactly
#  2 lines - a defline and a sequence line.
#
#  $Id: MaReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( confess );
use QCMG::IO::MaRecord;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: MaReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    confess "MaReader->new() requires a filename parameter" 
        unless (exists $params{filename} and defined $params{filename});

    my $self  = { filename     => $params{filename},
                  verbose      => ($params{verbose} ? $params{verbose} : 0),
                  headers      => [],
                  _defline     => '',
                  _recctr      => 0,
                  _linectr     => 0,
                  CreationTime => localtime().'',
                  Version      => $REVISION, 
                };

    bless $self, $class;
    $self->_initialise;
    return $self;
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub filename {
    my $self = shift;
    return $self->{filename};
}

sub sequence {
    my $self = shift;
    return @{ $self->{sequences} };
}

sub creation_time {
    my $self = shift;
    return $self->{'CreationTime'};
}

sub creator_module_version {
    my $self = shift;
    return $self->{'Version'};
}

sub linectr {
    my $self = shift;
    return $self->{_linectr};
}

sub recctr {
    my $self = shift;
    # We have already read the next record so current record is -1
    return $self->{_recctr}-1;
}

sub _getline {
    my $self = shift;
    my $fh = $self->{fh};
    my $line = $fh->getline;
    chomp $line;          # get rid of newline
    $line =~ s/\s+$//;    # trim trailing spaces
    $self->{_linectr}++;  # keep a count of lines read
    #print "read a line [$line]\n";
    return $line;
}

sub _initialise {
    my $self = shift;

    my $fh = IO::File->new( $self->filename, 'r' );
    confess 'Unable to open MA file [', $self->filename, "] for reading: $!"
        unless defined $fh;

    $self->{fh} = $fh;

    print 'processing MA file [', $self->filename, "]\n"
        if $self->verbose;

    my $line = '';
    my $seq  = '';
    
    # Read off any headers
    while ($line = $self->_getline) {
        next unless $line;                  # skip blank lines
        last unless ($line =~ /^#/);        # exit on first non-header line
        push @{ $self->{headers} }, $line;  # save header line
    }

    # To get here we must be past the headers so $line should contain
    # the first defline.  For safety's sake let's test this  

    if ($line =~ /^\>/) {
        $seq = $self->_getline;
        confess 'Invalid .ma file format at line ', $self->{_linectr},
                ": [$seq]\n" unless $seq;
        $self->{_recctr}++;  # keep a count of records read
    }
    else {
        confess 'Invalid .ma file format at line ', $self->{_linectr},
                ": [$line]\n";
    }

    #$self->{_current_record} = { defline => $line, seq => $seq };
    $self->{_current_record} = QCMG::IO::MaRecord->new( $line, $seq );
}

sub current_record {
    my $self = shift;
    return $self->{_current_record}
}

sub next_record {
    my $self = shift;

    # Return undef if we have already returned the last record
    return undef unless defined $self->{_current_record};

    # Save current record since this is what we will actually be returning
    my $crec = $self->current_record;

    # Read another record from the .ma file
    my $next_defline = $self->_getline;
    my $next_seq     = $self->_getline;
    if (defined $next_defline and defined $next_seq) {
        $self->{_recctr}++;  # keep a count of records read
        #$self->{_current_record} = { defline => $next_defline,
        #                             seq     => $next_seq };
        $self->{_current_record} = QCMG::IO::MaRecord->new( $next_defline,
                                                            $next_seq );
    }
    else {
        die 'What the F*!';
        $self->{_current_record} = undef;
    }

    # Return record
    return $crec;
}


1;

__END__


=head1 NAME

QCMG::IO::MaReader - Read .ma alignment files from Bioscope


=head1 SYNOPSIS

 use QCMG::IO::MaReader;


=head1 DESCRIPTION

This module provides an interface for reading .ma matches alignment
files as created by Bioscope A .ma file is basically a multi-FASTA file
with some extra information on the defline listing all of the alignments
found. Note that this module assumes that every record has exactly 2
lines - a defline and a sequence line.

 >2_52_1120_F3
 T0002001101010010000301203001030020300.010013130131
 >2_52_1143_F3,12_-27912272.2:(27.2.0):q10,5_-11653883.2:(24.2.0):q0
 T3002002000000200010100010022000021001.000011000201
 >2_52_1170_F3
 T3001300.03201220000100010102231011332.201122302223
 >2_52_1192_F3
 T1000001302010012000020002010112121330.320103000132
 >2_52_1196_F3,8_-14538482.2:(25.2.0):q2,11_-64039799.2:(24.2.0):q1,\
 15988598.2:(24.2.0):q1,17_-70021779.2:(24.2.0):q1
 T0000000001200012220000202201000220222.020023110031

=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: MaReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010-2014

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
