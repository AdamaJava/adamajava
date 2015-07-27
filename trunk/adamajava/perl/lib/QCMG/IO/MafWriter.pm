package QCMG::IO::MafWriter;

###########################################################################
#
#  Module:   QCMG::IO::MafWriter
#  Creator:  John V Pearson
#  Created:  2012-04-02
#
#  Creates v4.1 MAF file (1000 Genomes) to hold variant information.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use Data::Dumper;
use vars qw( $SVNID $REVISION );

use QCMG::IO::MafRecord;
use QCMG::Util::QLog;

( $REVISION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;
( $SVNID ) = '$Id$'
    =~ /\$Id:\s+(.*)\s+/;


sub new {
    my $class = shift;
    my %params = @_;

    die "MafWriter:new() requires a filename parameter" 
        unless (exists $params{filename} and defined $params{filename});

    my $self = { filename      => $params{filename},
                 filehandle    => undef,
                 record_ctr    => 0,
                 extra_fields  => ($params{extra_fields} ?
                                   $params{extra_fields} : []),
                 version       => ($params{version} ?
                                   $params{version} : '2.2'),
                 verbose       => ($params{verbose} ?
                                   $params{verbose} : 0),
               };

    bless $self, $class;

    # Log that we are writing extra fields
    my @extra_fields = @{ $self->{extra_fields} };
    if (scalar(@extra_fields)) {
        qlogprint 'writing extra fields to MAF file: ',
                  join(',',@extra_fields),"\n";
    }

    # Open file and make sure it is writable
    my $fh = IO::File->new( $params{filename}, 'w' );
    die 'Unable to open ', $params{filename}, "for writing: $!"
        unless defined $fh;

    # If a text comment block is passed in then it is written out
    # verbatim.  This is how we carry over comment headers from one MAF
    # to another.
    print $fh $params{comments} if $params{comments};

    die 'MAF version ['. $self->{version} . "] is unknown\n" unless 
        (exists $QCMG::IO::MafRecord::VALID_HEADERS->{$self->version});

    # Make sure we have output the correct column list
    my @valid_headers = ( @{ $QCMG::IO::MafRecord::VALID_HEADERS->{$self->version} },
                          @{ $self->{extra_fields} } );

    print $fh '#version '. $self->{version} . "\n",
          join("\t",@valid_headers), "\n";

    $self->{filehandle} = $fh;
    return $self;
}


sub filename {
    my $self = shift;
    return $self->{filename};
}

sub filehandle {
    my $self = shift;
    return $self->{filehandle};
}

sub version {
    my $self = shift;
    return $self->{version};
}

sub record_ctr {
    my $self = shift;
    return $self->{record_ctr};
}

sub extra_fields {
    my $self = shift;
    return @{ $self->{extra_fields} };
}

sub write {
    my $self = shift;
    my $rec  = shift;

    die "write() must be passed an object of type QCMG::IO::MafRecord\n"
        unless ref($rec) eq 'QCMG::IO::MafRecord';

    $self->filehandle->print( $rec->to_text( [ $self->extra_fields ] ) ."\n" );
    $self->{record_ctr}++;
}

1;

__END__


=head1 NAME

QCMG::IO::MafWriter - MAF file IO


=head1 SYNOPSIS

 use QCMG::IO::MafWriter;


=head1 DESCRIPTION

This module provides an interface for writing MAF files.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $mwriter = QCMG::IO::MafWriter->new( filename => 'sample1.maf',
                                         comments => $my_comments,
                                         version  => 2.2 );

Filename is compulsory but version is optional and is set to 2.2 by
default.  If supplied the version string must be in the form X.X and the
only valid values currently are 1.0, 2.0, 2.1, 2.2.

If a text block is passed in via the 'comments' parameter then it is 
written out verbatim.  This is how we carry over comment headers from
one MAF to another, e.g. in QCMG::Verify::QVerify where we read a MAF,
modify it slightly and write it out again but ideally with all of the
headers from the original MAF copied over to the new MAF.

=item B<write()>

Takes a QCMG::IO::MafRecord object and writes out the record data to the
MAF file.

=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


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
