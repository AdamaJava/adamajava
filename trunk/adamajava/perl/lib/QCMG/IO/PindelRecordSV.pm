package QCMG::IO::PindelRecordSV;

###########################################################################
#
#  Module:   QCMG::IO::PindelRecordSV
#  Creator:  John V Pearson
#  Created:  2012-08-08
#
#  Data container for a pindel report file record.
#
#  $Id: PindelRecordSV.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak confess );
use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION $VALID_AUTOLOAD_METHODS );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: PindelRecordSV.pm 4663 2014-07-24 06:39:00Z j.pearson $'
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
        Chrom                                 => 2,
        LeftBreakPoint                        => 3,
        LeftBreakPointSupportingReadsCount    => 4,
        RightBreakPoint                       => 5,
        RightBreakPointSupportingReadsCount   => 6,
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
        croak "QCMG::IO::PindelRecordSV AUTOLOAD problem with [$invocation]";
    }

    # We don't need to report on missing DESTROY methods.
    return undef if ($method eq 'DESTROY');

    croak "QCMG::IO::PindelRecordSV can't access method [$method] via AUTOLOAD"
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

    confess 'This record type is only applicable to pindel records of type '.
        'LI, not [', $fields[1], ']'
        unless ( $fields[1] eq 'LI' );
    $self->VarType( $fields[1] );

    if ($fields[2] =~ /ChrID (\w+)/) {
        $self->Chrom( $1 );
    }

    $self->LeftBreakPoint( $fields[3] );
    if ($fields[4] =~ /\+ (\d+)/) {
        $self->LeftBreakPointSupportingReadsCount( $1 );
    }
    else {
        confess "Unable to parse field LeftBreakPointSupportingReadsCount";
    }
    
    $self->RightBreakPoint( $fields[5] );
    if ($fields[6] =~ /\- (\d+)/) {
        $self->RightBreakPointSupportingReadsCount( $1 );
    }
    else {
        confess "Unable to parse field RightBreakPointSupportingReadsCount";
    }
    
    foreach my $fctr (7..$#fields) {
        $self->_parse_file_info( $fields[$fctr] );
    }
}


sub _parse_file_info {
    my $self = shift;
    my $text = shift;

    my @fields = split /\s+/, $text;

    push @{ $self->{file_info} },
         { SampleName      => $fields[0],
           LeftReadsCount  => $fields[2],
           RightReadsCount => $fields[4] };
}



1;


__END__


=head1 NAME

QCMG::IO::PindelRecordSV - pindel report record data container


=head1 SYNOPSIS

 use QCMG::IO::PindelRecordSV;


=head1 DESCRIPTION

This module provides a data container for a pindel report record for
files ending in _LI (large insertions).

The descriptions of the columns in a pindel report are taken from the
pindel manual and the accessor names were supplied by QCMG.  Note that
some columns are invariant strings (e.g. 3) so there is no accessor for
those columns.

 1.  Index
     The index of the indel/SV (57 means that 57 insertions precede this
     insertion in the file)
 2.  VarType
     The type of indel/SV: I for insertion, D for deletion, INV for
     inversion, TD for tandem duplication
 3.  ---
     String "ChrID"
 4.  Chrom
     The identifier of the chromosome the read was found on
 5.  LeftBreakPoint
     The left breakpoint 
 6.  ---
     String "+"
 7.  LeftBreakPointSupportingReadsCount
     Number of reads supporting the left breakpoint
 8.  RightBreakPoint
     The right breakpoint  - can be less than LeftBP if there is
     microhomology around the breakpoint
 9.  ---
     String "+"
 10. RightBreakPointSupportingReadsCount
     Number of reads supporting the right breakpoint

 11. File1SampleName
     Sample name
 12. File1UpstreamReadsCount
     supporting reads whose anchors are upstream
 13. File1UpstreamUniqueReadsCount
     unique supporting reads whose anchors are upstream
 14. File1DownstreamReadsCount
     supporting reads whose anchors are downstream
 15. File1DownstreamUniqueReadsCount
     unique supporting reads whose anchors are downstream
 
 16. File2SampleName
     Sample name
 17. File2UpstreamReadsCount
     supporting reads whose anchors are upstream
 18. File2UpstreamUniqueReadsCount
     unique supporting reads whose anchors are upstream
 19. File2DownstreamReadsCount
     supporting reads whose anchors are downstream
 20. File2DownstreamUniqueReadsCount
     unique supporting reads whose anchors are downstream
 
 
=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: PindelRecordSV.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
