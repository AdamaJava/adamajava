package QCMG::IO::QiRecord;

###########################################################################
#
#  Module:   QCMG::IO::QiRecord
#  Creator:  John V Pearson
#  Created:  2012-06-09
#
#  Data container for a qInspect record.
#
#  $Id: QiRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( carp croak );
use Data::Dumper;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: QiRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class  = shift;
    my %params = @_;

    my $self = { annotations  => {},
                 samrecs      => [],
                 verbose      => $params{verbose} || 0 };

    bless $self, $class;
}


sub verbose {
    my $self = shift;
    return $self->{verbose} = shift if @_;
    return $self->{verbose};
}


sub add_sam_record {
    my $self = shift;
    my $rec  = shift;
    die 'add_sam_record() must be passed QCMG::IO::SamRecord objects '.
        'but you passed ['. (ref($rec) ? ref($rec) : $rec) .']'
        unless ( ref($rec) eq 'QCMG::IO::SamRecord' );
    push @{ $self->{samrecs} }, $rec;
}


sub sam_records {
    my $self = shift;
    return $self->{samrecs};
}


sub annotations {
    my $self  = shift;
    return $self->{annotations};
}


sub set_annot {
    my $self  = shift;
    my $key   = shift;
    my $value = shift;
    $self->{annotations}->{ $key } = $value;
}


sub get_annot {
    my $self  = shift;
    my $key   = shift;
    return $self->{annotations}->{$key}
        if (exists $self->{annotations}->{$key});
    return undef;
}


1;
 
__END__


=head1 NAME

QCMG::IO::QiRecord - qInspect record data container


=head1 SYNOPSIS

 use QCMG::IO::QiRecord;


=head1 DESCRIPTION

This module provides a data container for a qInspect record which is a
set of annotations and a list of QCMG::IO::SamRecord objects.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $qi = QCMG::IO::QiRecord->new( verbose => 1 );

This method creates a new empty QiRecord object.

=item B<set_annot()>

 $qi->set_annot( 'Source' , 'APGI_1992' );

Set an annotation which is a key/value pair.

=item B<get_annot()>

 $qi->get_annot( 'Source');  # returns 'APGI_1992'

Gets an annotation which was previously set using B<set_annot()>.

=item B<add_sam_record()>
 
 my $samrec = QCMG::IO::SamRecord->new();
 ...
 $qi->add_sam_record( $samrec );

Add a new QCMG::IO::SamRecord.

=item B<sam_records()>

 my $ra_recs = $qi->sam_records();

Get an arrayref of SamRecord objects;

=item B<verbose()>

 $qi->verbose();

Returns the verbose status for this object where 0 sets verbose off
and any other value (traditionally 1) sets verbose mode on.

=back


=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: QiRecord.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
