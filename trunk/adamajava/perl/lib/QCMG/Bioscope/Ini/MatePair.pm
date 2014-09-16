package QCMG::Bioscope::Ini::MatePair;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::MatePair.pm
#  Creator:  John V Pearson
#  Created:  2010-08-23
#
#  Create bioscope .ini files for a matepair run.
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;
use vars qw( $VERSION @ISA );

use QCMG::Bioscope::Ini::RunType;
use QCMG::Bioscope::Ini::RunPbsFile;
use QCMG::Bioscope::Ini::BioscopePlanFile;
use QCMG::Bioscope::Ini::GlobalIniFile;
use QCMG::Bioscope::Ini::MappingFile;
use QCMG::Bioscope::Ini::PairingFile;
use QCMG::Bioscope::Ini::PosErrorsFile;

@ISA = qw( QCMG::Bioscope::Ini::RunType );

sub new {
    my $class = shift;
    my %params = @_;

    my $self = $class->SUPER::new( %params );

    $self->{verbose} = ($params{verbose} ? $params{verbose} : 0);

    my $file = QCMG::Bioscope::Ini::RunPbsFile->new( %params );
    $file->write( $self->directory );
    $file = QCMG::Bioscope::Ini::BioscopePlanFile->new( %params );
    $file->write( $self->directory );
    $file = QCMG::Bioscope::Ini::GlobalIniFile->new( %params );
    $file->write( $self->directory );
    $file = QCMG::Bioscope::Ini::MappingFile->new( %params, map_type => 'F3' );
    $file->write( $self->directory );
    $file = QCMG::Bioscope::Ini::MappingFile->new( %params, map_type => 'R3' );
    $file->write( $self->directory );
    $file = QCMG::Bioscope::Ini::PairingFile->new( %params );
    $file->write( $self->directory );
    $file = QCMG::Bioscope::Ini::PosErrorsFile->new( %params );
    $file->write( $self->directory );

    return $self;
}


sub verbose {
    my $self = shift;
    return $self->{verbose};
}


1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::MatePair - Create Bioscope ini files for a matepair run


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::MatePair;
 my $ini = QCMG::Bioscope::Ini::MatePair->new( %params );


=head1 DESCRIPTION

This module creates the ini files required to initiate a Bioscope
matepair run.  It requires multiple values to be supplied by the user.
All the user-supplied values must be specified in the call to B<new()>.


=head1 PUBLIC METHODS

=over

=item B<new()>

 my $ini = QCMG::Bioscope::Ini::MatePair->new(
                execution_server => 'barrine',
                execution_dir    => '/panfs/imb/home/uqjpear1/bioscope/',
                run_name         => 'S0436_20100517_2_Frag',
                run_date         => '20100804',
                );

=back


=head1 SEE ALSO

=over

=item QCMG::Bioscope::Ini::RunType

=item QCMG::Bioscope::Ini::RunPbsFile

=item QCMG::Bioscope::Ini::BioscopePlanFile

=item QCMG::Bioscope::Ini::GlobalIniFile

=item QCMG::Bioscope::Ini::MappingFile

=item QCMG::Bioscope::Ini::PairingFile

=item QCMG::Bioscope::Ini::PosErrors



=back


=head1 AUTHORS

=over

=item John Pearson L<mailto:j.pearson@uq.edu.au>

=back


=head1 VERSION

$Id: MatePair.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
