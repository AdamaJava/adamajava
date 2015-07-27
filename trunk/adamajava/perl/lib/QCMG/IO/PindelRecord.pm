package QCMG::IO::PindelRecord;

###########################################################################
#
#  Module:   QCMG::IO::PindelRecord
#  Creator:  John V Pearson
#  Created:  2012-08-08
#
#  Data container for a pindel report file record.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION $VALID_AUTOLOAD_METHODS );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;

our $AUTOLOAD;  # it's a package global

BEGIN {

    # We are using AUTOLOAD because with a little planning, it lets us avoid
    # defining and maintaining a lot of basically identical accessor methods.
    # Parsing the values for all of these items out of a pindel record
    # is another painful story - see _parse_headers() for the details.

    $VALID_AUTOLOAD_METHODS = {
        Index                                 => 0,
        VarType                               => 1,
        VarLength                             => 2,
        NonTemplateLength                     => 3,
        NonTemplateSequence                   => 4,
        Chrom                                 => 5,
        VarStart                              => 6,
        VarEnd                                => 7,
        VarRangeStart                         => 8,
        VarRangeEnd                           => 9,
        SupportingReadsCount                  => 10,
        SupportingUniqueReadsCount            => 11,
        UpstreamSupportingReadsCount          => 12,
        UpstreamSupportingUniqueReadsCount    => 13,
        DownstreamSupportingReadsCount        => 14,
        DownstreamSupportingUniqueReadsCount  => 15,
        SimpleScore                           => 16,
        SumMappingScoresAnchorReads           => 17,
        SampleCount                           => 18,
        SamplesSupportingVar                  => 19,
        SamplesUniqueSupportingVar            => 20,
    };

}

###########################################################################
#                             PUBLIC METHODS                              #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    croak "no headers parameter to new()" unless
        (exists $params{headers} and defined $params{headers});

    my $self = { headers    => [],
                 orig_head  => '',
                 reads_text => '',
                 verbose    => $params{verbose} || 0 };
    bless $self, $class;

    $self->{orig_head} = $params{headers};
    $self->_parse_headers;

    return $self;
}


sub AUTOLOAD {
    my $self  = shift;
    my $value = shift;

    my $type       = ref($self) or confess "$self is not an object";
    my $invocation = $AUTOLOAD;
    my $method     = undef;
    my $version    = $self->{maf_version};

    if ($invocation =~ m/^.*::([^:]*)$/) {
        $method = $1;
    }
    else {
        croak "QCMG::IO::PindelRecord AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    croak "QCMG::IO::PindelRecord can't access method [$method] via AUTOLOAD"
        unless (exists $VALID_AUTOLOAD_METHODS->{$method});

    # If this is a setter call then do the set based on array offset
    # stored in VALID_AUTOLOAD_METHODS hash
    if (defined $value) {
        $self->{headers}->[ $VALID_AUTOLOAD_METHODS->{$method} ] = $value;
    }
    # Return current value
    return $self->{headers}->[ $VALID_AUTOLOAD_METHODS->{$method} ];
}  


sub reads_text {
    my $self    = shift;
    return $self->{reads_text} = shift if @_;
    return $self->{reads_text};
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub files {
    my $self = shift;
    return @{ $self->{file_info} };
}


sub file {
    my $self = shift;
    my $id   = shift;
    return undef unless exists $self->{file_info}->[$id];
    return $self->{file_info}->[$id];
}


sub to_text {
    my $self = shift;

    return '#'x100 ."\n". 
           $self->{orig_head} ."\n".
           $self->{reads_text};
}


sub debug_text {
    my $self = shift;

    my $text = '';
    foreach my $column (keys %{$VALID_AUTOLOAD_METHODS}) {
        $text .= substr($column . " "x40, 0, 40 ) .
                 $self->{headers}->[ $VALID_AUTOLOAD_METHODS->{$column} ] .
                 "\n";
    }
    $text .= $self->{reads_text};
    return $text;
}


###########################################################################
#                            PRIVATE METHODS                              #
###########################################################################


sub _parse_headers {
    my $self = shift;
    my $text = $self->{orig_head};

    my @fields = split /\t/, $text;

    $self->Index( $fields[0] );

    my ($val1, $val2) = split /\s+/, $fields[1];
    $self->VarType( $val1 );
    $self->VarLength( $val2 );

    if ($fields[2] =~ /NT (\d+) \"(\w*)\"/) {
        $self->NonTemplateLength( $1 );
        $self->NonTemplateSequence( $2 );
    }

    if ($fields[3] =~ /ChrID (\w+)/) {
        $self->Chrom( $1 );
    }

    if ($fields[4] =~ /BP (\d+)/) {
        $self->VarStart( $1 );
    }

    $self->VarEnd( $fields[5] );

    if ($fields[6] =~ /BP_range (\d+)/) {
        $self->VarRangeStart( $1 );
    }
    
    $self->VarRangeEnd( $fields[7] );

    if ($fields[8] =~ /Supports (\d+)/) {
        $self->SupportingReadsCount( $1 );
    }
    
    $self->SupportingUniqueReadsCount( $fields[9] );

    if ($fields[10] =~ /\+ (\d+)/) {
        $self->UpstreamSupportingReadsCount( $1 );
    }
    
    $self->UpstreamSupportingUniqueReadsCount( $fields[11] );

    if ($fields[12] =~ /\- (\d+)/) {
        $self->DownstreamSupportingReadsCount( $1 );
    }
    
    $self->DownstreamSupportingUniqueReadsCount( $fields[13] );

    if ($fields[14] =~ /S1 (\d+)/) {
        $self->SimpleScore( $1 );
    }
 
    if ($fields[15] =~ /SUM_MS (\d+)/) {
        $self->SumMappingScoresAnchorReads( $1 );
    }

    $self->SampleCount( $fields[16] );

    if ($fields[17] =~ /NumSupSamples (\d+)/) {
        $self->SamplesSupportingVar( $1 );
    }

    $self->SamplesUniqueSupportingVar( $fields[18] );

    foreach my $fctr (0..($self->SampleCount-1)) {
        $self->_parse_file_info( $fields[19+$fctr] );
    }
}

sub _parse_file_info {
    my $self = shift;
    my $text = shift;

    my @fields = split /\s+/, $text;

    push @{ $self->{file_info} },
         { SampleName                 => $fields[0],
           UpstreamReadsCount         => $fields[1],
           UpstreamUniqueReadsCount   => $fields[2],
           DownstreamReadsCount       => $fields[3],
           DownstreamUniqueReadsCount => $fields[4] };
}



1;


__END__


=head1 NAME

QCMG::IO::PindelRecord - pindel report record data container


=head1 SYNOPSIS

 use QCMG::IO::PindelRecord;


=head1 DESCRIPTION

This module provides a data container for a pindel report record.

The descriptions of the columns in a pindel report are taken from the
pindel manual and the accessor names were supplied by QCMG.  Note that
some columns are invariant strings (e.g. 4) so there is no accessor for
those columns.

 1.  Index
     The index of the indel/SV (57 means that 57 insertions precede this
     insertion in the file)
 2.  VarType
     The type of indel/SV: I for insertion, D for deletion, INV for
     inversion, TD for tandem duplication
 3.  VarLength
     The length of the SV
 4.  ---
     String "NT"
 5.  NonTemplateLength
     The length(s) of the non-template (NT) fragment(s).
     Insertions are fully covered by the NT-fields, deletions can have NT
     bases if the deletion is not 'pure', meaning that while bases have
     been deleted, some bases have been inserted between the breakpoints.
 6.  NonTemplateSequence
     The sequence(s) of the NT fragment(s)
 7.  ---
     String "ChrID"
 8.  Chrom
     The identifier of the chromosome the read was found on
 9.  ---
     String "BP"
 10. VarStart
     The start positions of the SVs
 11. VarEnd
     The end positions of the SVs
 12. ---
     String "BP_range"
 13. VarRangeStart
     If the exact position of the SV is unclear since bases at the edge 
     of one read-half could equally well be appended to the other 
     read-half. In the deletion example, ACA could be on any side of
     the gap, so the original deletion could have been between 1337143 and
     1338815, between 1337144 and 1338816, or between 1337145 and 133817, or
     between 1337146 and 133818. BP-range is used to indicate this range.
 14. VarRangeEnd
     End of the range
 15. ---
     String "Supports"
 16. SupportingReadsCount
     The number of reads supporting the SV
 17. SupportingUniqueReadsCount
     The number of unique reads supporting the SV (so not counting
     duplicate reads)
 18. ---
     String "+"
 19. UpstreamSupportingReadsCount
     Number of supporting reads whose anchors are upstream of the SV
 20. UpstreamSupportingUniqueReadsCount
     Number of unique supporting reads whose anchors are upstream of the SV
 21. ---
     String "-"
 22. DownstreamSupportingReadsCount
     Number of supporting reads whose anchors are downstream of the SV
 23. DownstreamSupportingUniqueReadsCount
     Number of unique supporting reads whose anchors are downstream of the SV

 24-25) S1: a simple score, (“# +” + 1)* (“# -” + 1) ;
 26-27) SUM_MS: sum of mapping qualities of anchor reads;

 28. SampleCount
     The number of different samples scanned
 29. ---
     String "NumSupSamples"
 30. SamplesSupportingVar
     The number of samples supporting the SV
 31. SamplesUniqueSupportingVar
     The the number of samples having unique reads supporting the SV (in
     practice, this must be the same as column 30, SampleSupportingVar

 32. File1SampleName
     Sample name
 33. File1UpstreamReadsCount
     supporting reads whose anchors are upstream
 34. File1UpstreamUniqueReadsCount
     unique supporting reads whose anchors are upstream
 35. File1DownstreamReadsCount
     supporting reads whose anchors are downstream
 36. File1DownstreamUniqueReadsCount
     unique supporting reads whose anchors are downstream
 
 37. File2SampleName
     Sample name
 38. File2UpstreamReadsCount
     supporting reads whose anchors are upstream
 39. File2UpstreamUniqueReadsCount
     unique supporting reads whose anchors are upstream
 40. File2DownstreamReadsCount
     supporting reads whose anchors are downstream
 42. File2DownstreamUniqueReadsCount
     unique supporting reads whose anchors are downstream
 
 
=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2012-2014

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
