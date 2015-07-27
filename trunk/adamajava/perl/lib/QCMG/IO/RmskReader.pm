package QCMG::IO::RmskReader;

###########################################################################
#
#  Module:   QCMG::IO::RmskReader
#  Creator:  Matthew J Anderson
#  Created:  2011-01-16
#
#  Reads a USCS dump of a list of Repeating Elements generaged by RepeatMasker.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use Carp qw( croak confess );
use QCMG::IO::RmskRecord;
use vars qw( $SVNID $REVISION );

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;
    
# Useage file name, verbose    
sub new {
    my $class = shift;
    my %params = @_;

    croak "RmskReader:new() requires a filename" 
        unless (exists $params{filename} and defined $params{filename});

      my $self = { filename     => $params{filename},
                   filehandle   => undef,
                   record_count => 0,
                   verbose      => ($params{verbose} ?
                                       $params{verbose} : 0),
                 };

      bless $self, $class;

      my $fh = IO::File->new( $params{filename}, 'r' );
      confess 'Unable to open ', $params{filename}, " for reading: $!"
          unless defined $fh;
      $self->filename( $params{filename} );
      $self->filehandle( $fh );

      return $self;
}

sub filename {
    my $self = shift;
    return $self->{filename} = shift if @_;
    return $self->{filename};
}


sub filehandle {
    my $self = shift;
    return $self->{filehandle} = shift if @_;
    return $self->{filehandle};
}

sub verbose {
    my $self = shift;
    return $self->{verbose};
}

sub record_count {
    my $self = shift;
    return $self->{record_count};
}

sub _increment_record_count {
    my $self = shift;
    return $self->{record_count}++;
}

sub next_record {
    my $self = shift;
    
    # Read lines, checking for and processing any headers
    # and only return once we have a record

    while (1) {
        my $line = $self->filehandle->getline();
        # Catch EOF
        return undef if (! defined $line);
        
        if ($self->verbose) {
            # Print progress messages for every 1M records
            $self->_increment_record_count;
            print( $self->record_count, ' Repeat Masker records processed: ',
                localtime().'', "\n" )
                if $self->record_count % 100000 == 0;
        }
        my $rmsk_line = QCMG::IO::RmskRecord->new( $line );
        print $rmsk_line->debug if ($self->verbose > 1);
        return $rmsk_line;
    }
}
1;

__END__

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

