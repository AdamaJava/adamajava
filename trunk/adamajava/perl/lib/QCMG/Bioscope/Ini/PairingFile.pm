package QCMG::Bioscope::Ini::PairingFile;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::PairingFile.pm
#  Creator:  John V Pearson
#  Created:  2010-08-27
#
#  Create pairing.ini file.
#
#  $Id$
#
###########################################################################

use strict;
use warnings;

use IO::File;
use IO::Zlib;
use Data::Dumper;
use vars qw( $VERSION @ISA );

use QCMG::Bioscope::Ini::IniFile;

@ISA = qw( QCMG::Bioscope::Ini::IniFile );

( $VERSION ) = '$Revision$ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    # Choose appropriate template based on run_type
    my $template = '';
    if ($params{run_type} =~ /matepair/i) {
        $template = _template_matepair();
    }
    elsif ($params{run_type} =~ /pairedendBC/i) {
        $template = _template_pairedendBC();
    }
    else {
        die 'QCMG::Bioscope::Ini::PairingFile - does not currently ',
            'understand runtype [', $params{run_type}, ']';
    }

    my $self = $class->SUPER::new( %params,
                                   filename => 'pairing.ini',
                                   template => $template,
                                   version  => $VERSION );

    return $self;
}


sub _template_matepair {

    my $template = <<'_EO_TEMPLATE_';

import global.ini

# Run the pairing pipeline
pairing.run             = 1

# Library Name
pairing.library.name = ${qcmg.library}

# Mapping output directories for F3 & R3
mate.pairs.tagfile.dirs = ${output.dir}/F3/s_mapping,${output.dir}/R3/s_mapping

# Output directory for paired files
pairing.output.dir      = ${output.dir}/pairing

# Set the QCMG pattern for BAM file names
bam.file.name           = ${qcmg.mapset}.bam
unmapped.bam.file.name   = ${qcmg.mapset}.unmapped.bam

# Use 4 of the 5 possible indel selectors
indel.preset.parameters = 1,3,4,5

# Color quality file paths
pairing.color.qual.file.path.1 = ${qcmg.base.dir}/${qcmg.f3.primary.dir}/${qcmg.f3.qual}
pairing.color.qual.file.path.2 = ${qcmg.base.dir}/${qcmg.r3.primary.dir}/${qcmg.r3.qual}
_EO_TEMPLATE_

    return $template;
}


sub _template_pairedendBC {

    my $template = <<'_EO_TEMPLATE_';

import global.ini

# Run the pairing pipeline
paired-end-pairing.run  = 1

# Library Name
pairing.library.name = ${qcmg.library}

# Mapping output directories for F3 & F5
mate.pairs.tagfile.dirs = ${output.dir}/F3/s_mapping,${output.dir}/F5/s_mapping

# Output directory for paired files
pairing.output.dir      = ${output.dir}/pairing

# Set the QCMG pattern for BAM file names
bam.file.name            = ${qcmg.mapset}.bam
unmapped.bam.file.name   = ${qcmg.mapset}.unmapped.bam

# Look for indels
indel.preset.parameters = 1

primer.set              = F3,F5-BC
pairing.mark.duplicates = true

# Color quality file paths
pairing.color.qual.file.path.1 = ${qcmg.base.dir}/${qcmg.f3.primary.dir}/${qcmg.f3.qual}
pairing.color.qual.file.path.2 = ${qcmg.base.dir}/${qcmg.f5.primary.dir}/${qcmg.f5.qual}
_EO_TEMPLATE_

    return $template;
}

1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::PosErrorsFile - Create posErrors.ini file


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::PosErrorsFile;
 my $ini = QCMG::Bioscope::Ini::PosErrorsBamFile->new( %params );
 $ini->write( '/panfs/imb/home/uqjpear1/tmp' );


=head1 DESCRIPTION

This module creates the posErrors.ini file required to initiate a Bioscope
frgament run.  It requires multiple values to be supplied by the user.
All the user-supplied values can be specified in the call to B<new()> or
they can be supplied by calling the set accessor methods.


=head1 PUBLIC METHODS

See documentation for the superclass QCMG::Bioscope::Ini::IniFile.


=head1 SEE ALSO

=over

=item QCMG::Bioscope::Ini::IniFile

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
