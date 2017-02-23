package QCMG::IO::VcfRecord;

###########################################################################
#
#  Module:   QCMG::IO::VcfRecord
#  Creator:  John V Pearson
#  Created:  2010-03-08
#
#  Data container for a VCF 4.1 record.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Memoize;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my $line  = shift;
    
    # If undefined or empty line passed in then we are done and we pass
    # an undef straight back.
    return undef if (! defined $line or ! $line);

    chomp $line;
    my @fields = split "\t", $line;
    if (scalar(@fields) < 8) {
        warn 'Saw ', scalar(@fields),
            " fields, should have been at least 8 [$line]\n";
        return undef;
    }

    my $self = { chrom       => $fields[0],
                 position    => $fields[1],
                 id          => $fields[2],
                 ref_allele  => $fields[3],
                 alt_allele  => $fields[4],
                 qual        => $fields[5],
                 filter      => $fields[6],
                 info        => $fields[7],
                 format      => $fields[8],
                 calls       => [] };

    # Capture the calls and scores (if present)

    if (exists $fields[8]) {
       $self->{format} = $fields[8];
    }
    if (scalar(@fields) > 9) {
        $self->{calls} = [ @fields[9..$#fields] ];  # slice
    }


    bless $self, $class;
}


sub chrom {
    my $self = shift;
    return $self->{chrom};
}

sub position {
    my $self = shift;
    return $self->{position};
}

sub id {
    my $self = shift;
    return $self->{id};
}

sub ref_allele {
    my $self = shift;
    return $self->{ref_allele};
}

sub alt_allele {
    my $self = shift;
    return $self->{alt_allele};
}

sub qual {
    my $self = shift;
    return $self->{qual};
}

sub filter {
    my $self = shift;
    return $self->{filter};
}

sub info {
    my $self = shift;
    return $self->{info};
}

sub format {
    my $self = shift;
    return $self->{format};
}

sub calls {
    my $self = shift;
    return $self->{calls};
}

1;

__END__


=head1 NAME

QCMG::IO::VcfRecord - VCF Record data container


=head1 SYNOPSIS

 use QCMG::IO::VcfRecord;


=head1 DESCRIPTION

This module provides a data container for a VCF 4.1 Record.  It is ver
lightweight and only implements methods that return the contents of the
columns in the VCF.  It does not do any processing of composite columns
such as Filter or Info.


=head1 METHODS

=over 2

=item B<chrom()>

=item B<position()>

=item B<id()>

=item B<ref_allele()>

=item B<alt_allele()>

=item B<qual()>

=item B<filter()>

=item B<info()>

=item B<format()>

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id$


=head1 COPYRIGHT

Copyright (c) The University of Queensland 2010-2014
Copyright (c) QIMR Berghofer Medical Research Institute 2017

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
