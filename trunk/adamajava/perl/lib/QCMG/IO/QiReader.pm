package QCMG::IO::QiReader;

###########################################################################
#
#  Module:   QCMG::IO::QiReader
#  Creator:  John V Pearson
#  Created:  2011-10-18
#
#  Reads qInspect structured annotated SAM files.
#
#  $Id: QiReader.pm 4663 2014-07-24 06:39:00Z j.pearson $
#
###########################################################################

use strict;
use warnings;

use Carp qw( croak );
use Data::Dumper;
use IO::File;
use IO::Zlib;

use QCMG::IO::QiRecord;
use QCMG::IO::SamRecord;

use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision: 4663 $ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id: QiReader.pm 4663 2014-07-24 06:39:00Z j.pearson $'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    croak "QCMG::IO::QiReader:new() requires filename or zipname parameter" 
        unless ( (exists $params{filename} and defined $params{filename}) or
                 (exists $params{zipname} and defined $params{zipname}) );

    my $self = { filename    => $params{filename},
                 records     => [],
                 verbose     => ($params{verbose} ?
                                 $params{verbose} : 0),
               };

    bless $self, $class;

    # If there a zipname, we use it in preference to filename.  We only
    # process one so if both are specified, the zipname wins.  The it
    # likely that Qi files will never get above a million or so lines
    # and will usually be much smaller so for simplicity's sake, we will
    # read and parse the entire file in one go in the new() routine.

    my $fh = undef;
    if ( $params{zipname} ) {
        $fh = IO::Zlib->new( $params{zipname}, 'r' );
        croak 'Unable to open ', $params{zipname}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{zipname} );
    }
    elsif ( $params{filename} ) {
        $fh = IO::File->new( $params{filename}, 'r' );
        croak 'Unable to open ', $params{filename}, " for reading: $!"
            unless defined $fh;
        $self->filename( $params{filename} );
    }

    my @lines = ();
    while (my $line = $fh->getline()) {
        chomp $line;
        next unless $line;  # skip blank lines
        next if ($line =~ /^#/ and $line !~ /^#QI/);
        push @lines, $line;
    };
    $self->{lines} = \@lines;

    $self->_parse_lines;

    return $self;
}


sub _parse_lines {
    my $self = shift;

    # These files should never get terribly big (some 100's of thousands
    # of lines) so the best idea is probably to slurp up front and parse
    # from the text clob.

    my @qi_recs   = ();
    my $qi        = undef;
    my $in_header = 0;
    my $in_body   = 0;

    # These lines have already been through the primary parse when they
    # were read from file so any "non-QI" comments have been stripped
    # out as have any blank lines

    my $recctr = 0;
    foreach my $line (@{ $self->{lines} }) {
        $recctr++;
        if ($line =~ /^#QI\s+(\w+)\s*\=\s*(.*)$/i) {
            my $key   = $1;
            my $value = $2;
            # If we were not already parsing a section header section then
            # we must have hit the start of a new section
            if (! $in_header) {
                push @qi_recs, $qi if (defined $qi);
                $qi = QCMG::IO::QiRecord->new( verbose => $self->verbose() );
            }
            $qi->set_annot( $key, $value );
            $in_header = 1;
            $in_body   = 0;
        }
        elsif ($line =~ /^[\w_\.\:\-]+\t(\d)+/) {
            # Must be in the body 
            $in_header = 0;
            $in_body   = 1;
            my $sam = QCMG::IO::SamRecord->new( sam => $line );
            $qi->add_sam_record( $sam );
        }
        else {
            die "QiReader could not pattern match line $recctr: [$line]";
        }
    }

    # Catch last qi record;
    push @qi_recs, $qi if (defined $qi);

    $self->{records} = \@qi_recs;
}


sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}


sub records {
    my $self = shift;
    return @{ $self->{records} };
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


1;

__END__


=head1 NAME

QCMG::IO::QiReader - qInspect file reader


=head1 SYNOPSIS

 use QCMG::IO::QiReader;


=head1 DESCRIPTION

This module provides an interface for reading the SAM-like qInspect text
files.


=head1 METHODS

=over

=item B<new()>

    my $qir = QCMG::IO::QiReader->new( filename => 'myqi.txt',
                                       verbose  => 1 );

=item B<records()>

=item B<filename()>

=item B<verbose()>

=back 




=head1 AUTHORS

John Pearson L<mailto:j.pearson@uq.edu.au>


=head1 VERSION

$Id: QiReader.pm 4663 2014-07-24 06:39:00Z j.pearson $


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
