package QCMG::Bioscope::Ini::MaToBamFile;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::MaToBamFile.pm
#  Creator:  John V Pearson
#  Created:  2010-08-27
#
#  Create maToBam.ini file.
#
#  $Id: MaToBamFile.pm 4660 2014-07-23 12:18:43Z j.pearson $
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

( $VERSION ) = '$Revision: 4660 $ ' =~ /\$Revision:\s+([^\s]+)/;


###########################################################################
#                          PUBLIC METHODS                                 #
###########################################################################


sub new {
    my $class  = shift;
    my %params = @_;

    # Choose which sort of maToBam.ini is required
    my $template = '';
    if ($params{run_type} =~ /fragment/i) {
        $template = _template_fragment();
    }
    else {
        die 'QCMG::Bioscope::Ini::MaToBamFile - does not currently ',
            'understand runtype [', $params{run_type}, ']';
    }

    my $self = $class->SUPER::new( %params,
                                   filename => 'maToBam.ini',
                                   template => $template,
                                   version  => $VERSION );

    return $self;
}


sub _template_fragment {

    my $template = <<'_EO_TEMPLATE_';

import global.ini

# Run maToBam plugin. [1 - run, 0 - do not run]
ma.to.bam.run = 1

# Intermediate directory used by maToBam plugin
ma.to.bam.intermediate.dir = ${intermediate.dir}/maToBam

# Temp directory used by maToBam plugin
ma.to.bam.temp.dir = ${tmp.dir}/maToBam

# Library Name
ma.to.bam.library.name = ${qcmg.library}

# Slide name - used in the PU header of the BAM file
ma.to.bam.slide.name = ${qcmg.run.dir}

# Parameter specifies the Ma To Bam Read Group description
ma.to.bam.description = QCMG__${qcmg.run.dir}

# Parameter specifies the Read Group Sequencing center
ma.to.bam.sequencing.center = Queensland Center for Medical Genomics

# Directory where .ma files to be converted are located
mapping.output.dir = ${output.dir}/F3/s_mapping

# Qual file to be used for conversion
ma.to.bam.qual.file = ${qcmg.base.dir}/${qcmg.f3.primary.dir}/${qcmg.f3.qual}

# Parameter specifies the input pas file for maToBam
ma.to.bam.pas.file = ${output.dir}/smallIndelFrag/indel-evidence-list.pas

# Parameter specifies the full path to the reference file
ma.to.bam.reference = ${reference}

# Directory for output from maToBam conversion
ma.to.bam.output.dir = ${output.dir}/maToBam
_EO_TEMPLATE_

    return $template;
}



1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::MaToBamFile - Create maToBam.ini file


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::MaToBamFile;
 my $ini = QCMG::Bioscope::Ini::MaToBamFile->new( %params );
 $ini->write( '/panfs/imb/home/uqjpear1/tmp' );


=head1 DESCRIPTION

This module creates the maToBam.ini file required to initiate a Bioscope
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

$Id: MaToBamFile.pm 4660 2014-07-23 12:18:43Z j.pearson $


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
