package QCMG::Bioscope::Ini::MappingFile;

###########################################################################
#
#  Module:   QCMG::Bioscope::Ini::MappingFile.pm
#  Creator:  John V Pearson
#  Created:  2010-08-27
#
#  Create mapping.ini file.
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

    my $map_type = $params{map_type};
    delete $params{map_type};  # remove map_type from %params

    my $template = '';
    my $filename = '';

    if ($map_type =~ /F3/) {
        $template = _set_template_F3();
        $filename = 'mappingF3.ini';
    }
    elsif ($map_type =~ /R3/) {
        $template = _set_template_R3();
        $filename = 'mappingR3.ini';
    }
    elsif ($map_type =~ /F5-P2/) {
        $template = _set_template_F5_P2();
        $filename = 'mappingF5.ini';
    }
    elsif ($map_type =~ /F5-BC/) {
        $template = _set_template_F5_BC();
        $filename = 'mappingF5.ini';
    }
    else {
        die 'QCMG::Bioscope::Ini::MappingFile - ',
            "[$map_type] is not a valid map_type";
    }


    my $self = $class->SUPER::new( %params,
                                   filename => $filename,
                                   template => $template,
                                   version  => $VERSION );

    return $self;
}


###########################################################################
#                          NON-OO METHODS                                 #
###########################################################################


sub _set_template_fragment {

    my $template = <<'_EO_TEMPLATE_';

import global.ini

# Run the mapping pipeline
mapping.run = 1
primer.set  = F3

# Library Name
mapping.library.name = ${qcmg.library}

# The pathname of the single csfasta file to be used as input
mapping.tagfiles = ${qcmg.base.dir}/${qcmg.f3.primary.dir}/${qcmg.f3.csfasta}

read.length      = ${qcmg.f3.read.length}

# Directory where the mapping pipeline output should be placed
mapping.output.dir = ${output.dir}/s_mapping
_EO_TEMPLATE_

    return $template;
}


sub _set_template_F3 {

    my $template = <<'_EO_TEMPLATE_';

import global.ini

# Run the mapping pipeline
mapping.run = 1

# Library Name
mapping.library.name = ${qcmg.library}

# Parameters for the F3 reads
primer.set           = F3
read.length          = ${qcmg.f3.read.length}
mapping.tagfiles     = ${qcmg.base.dir}/${qcmg.f3.primary.dir}/${qcmg.f3.csfasta}
mapping.output.dir   = ${output.dir}/F3/s_mapping
tmp.dir              = ${output.dir}/F3/tmp
intermediate.dir     = ${output.dir}/F3/intermediate
log.dir              = ${output.dir}/F3/log
scratch.dir          = ${qcmg.scratch.dir}/F3
_EO_TEMPLATE_

    return $template;
}


sub _set_template_R3 {

    my $template = <<'_EO_TEMPLATE_';

import global.ini

# Run the mapping pipeline
mapping.run = 1

# Library Name
mapping.library.name = ${qcmg.library}

# Parameters for the R3 reads
primer.set           = R3
read.length          = ${qcmg.r3.read.length}
mapping.tagfiles     = ${qcmg.base.dir}/${qcmg.r3.primary.dir}/${qcmg.r3.csfasta}
mapping.output.dir   = ${output.dir}/R3/s_mapping
tmp.dir              = ${output.dir}/R3/tmp
intermediate.dir     = ${output.dir}/R3/intermediate
log.dir              = ${output.dir}/R3/log
scratch.dir          = ${qcmg.scratch.dir}/R3
_EO_TEMPLATE_

    return $template;
}


sub _set_template_F5_P2 {

    my $template = <<'_EO_TEMPLATE_';

import global.ini

# Run the mapping pipeline
mapping.run = 1

# Library Name
mapping.library.name = ${qcmg.library}

# Parameters for the F5 reads
primer.set           = F5-P2
read.length          = ${qcmg.f5.read.length}
mapping.tagfiles     = ${qcmg.base.dir}/${qcmg.f5.primary.dir}/${qcmg.f5.csfasta}
mapping.output.dir   = ${output.dir}/F5/s_mapping
tmp.dir              = ${output.dir}/F5/tmp
intermediate.dir     = ${output.dir}/F5/intermediate
log.dir              = ${output.dir}/F5/log
scratch.dir          = ${qcmg.scratch.dir}/F5
_EO_TEMPLATE_

    return $template;
}


sub _set_template_F5_BC {

    my $template = <<'_EO_TEMPLATE_';

import global.ini

# Run the mapping pipeline
mapping.run = 1

# Library Name
mapping.library.name = ${qcmg.library}

# Parameters for the F5 reads
primer.set           = F5-BC
read.length          = ${qcmg.f5.read.length}
mapping.tagfiles     = ${qcmg.base.dir}/${qcmg.f5.primary.dir}/${qcmg.f5.csfasta}
mapping.output.dir   = ${output.dir}/F5/s_mapping
tmp.dir              = ${output.dir}/F5/tmp
intermediate.dir     = ${output.dir}/F5/intermediate
log.dir              = ${output.dir}/F5/log
scratch.dir          = ${qcmg.scratch.dir}/F5
_EO_TEMPLATE_

    return $template;
}

1;
__END__


=head1 NAME

QCMG::Bioscope::Ini::MappingFile - Create mapping.ini file


=head1 SYNOPSIS

 use QCMG::Bioscope::Ini::MappingFile;
 my $ini = QCMG::Bioscope::Ini::MappingFile->new( %params );
 $ini->write( '/panfs/imb/home/uqjpear1/tmp' );


=head1 DESCRIPTION

This module creates the mapping.ini file required to initiate a Bioscope
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
